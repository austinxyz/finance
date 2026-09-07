#!/usr/bin/env python3
"""Classify a month of bank statements into expense categories.

    python run.py 2026-08              # classify only — no network, no credentials
    python run.py 2026-08 --post       # reconcile the period into the database

Reads every CSV in ``~/finance-data/<period>/``, identifies each institution by
its header, classifies transactions against ``rules.toml``, and aggregates by
minor category. Writing is opt-in and guarded by three gates; without ``--post``
nothing leaves the machine.
"""

from __future__ import annotations

import argparse
import sys
from pathlib import Path

from importer import aggregate, classify, load_rules, print_summary, write_review
from importer.apiclient import ApiError, CredentialsError, FinanceApiClient
from importer.discover import DiscoveryError, data_root, discover, period_dir
from importer.reconcile import reconcile
from importer.report import Categories, write_batch_preview
from importer.sources import SourceConfigError, load_sources
from importer.write import WriteBlocked, apply_plan, plan_write

HERE = Path(__file__).parent


def _force_utf8_stdout() -> None:
    """Windows consoles default to a legacy codepage that mangles Chinese output."""
    for stream in (sys.stdout, sys.stderr):
        if hasattr(stream, "reconfigure"):
            stream.reconfigure(encoding="utf-8", errors="replace")


def build_parser() -> argparse.ArgumentParser:
    ap = argparse.ArgumentParser(
        description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter
    )
    ap.add_argument("period", help="记账期间 YYYY-MM，同时也是目录名")
    ap.add_argument("--root", help=f"CSV 根目录，默认 {data_root()}")
    ap.add_argument("--outdir", type=Path, default=HERE / "out", help="产出目录")
    ap.add_argument("--rules", type=Path, default=HERE / "rules.toml")
    ap.add_argument("--categories", type=Path, default=HERE / "categories.toml")
    ap.add_argument("--sources", type=Path, default=HERE / "sources.toml")

    ap.add_argument(
        "--post", action="store_true", help="写入数据库（默认只分类不写）"
    )
    gates = ap.add_argument_group(
        "闸门逃生舱",
        "默认三道闸门全部拒写。每个开关都会在控制台打印它放行了什么。",
    )
    gates.add_argument(
        "--allow-unknown", action="store_true", help="未分类交易归入「其他/未分类」"
    )
    gates.add_argument(
        "--allow-gaps", action="store_true", help="接受充值缺口带来的漏计"
    )
    gates.add_argument(
        "--allow-missing-sources", action="store_true", help="接受来源缺失"
    )
    return ap


def main(argv: list[str] | None = None) -> int:
    _force_utf8_stdout()
    args = build_parser().parse_args(argv)

    try:
        cats = Categories.load(args.categories)
        rules = load_rules(args.rules, valid_categories=cats.ids)
        declared = load_sources(args.sources)
        found = discover(period_dir(args.period, root=args.root))
    except (DiscoveryError, SourceConfigError, ValueError, FileNotFoundError) as exc:
        print(f"{exc}", file=sys.stderr)
        return 2

    for source in found.sources:
        suffix = f"，已过滤 {source.filtered_out} 笔非本期" if source.filtered_out else ""
        print(f"读取 {source.path.name}: {source.count} 笔  [{source.source}]{suffix}")
    if found.filtered_out:
        print(f"共过滤 {found.filtered_out} 笔非 {found.period} 交易（将在对应月份目录处理）")

    rows = classify(found.transactions, rules)

    args.outdir.mkdir(parents=True, exist_ok=True)
    review_path = args.outdir / f"review-{found.period}.csv"
    write_review(rows, cats, review_path)
    print_summary(rows, aggregate(rows), cats)
    print(f"\n明细核对: {review_path}")

    try:
        prepared = plan_write(
            rows=rows,
            declared_sources=declared,
            present_source_ids=found.source_ids,
            allow_unknown=args.allow_unknown,
            allow_gaps=args.allow_gaps,
            allow_missing_sources=args.allow_missing_sources,
        )
    except WriteBlocked as exc:
        print(f"\n{exc}", file=sys.stderr)
        return 1

    _report_waivers(prepared)

    preview_path = args.outdir / f"batch-preview-{found.period}.json"
    write_batch_preview({found.period: prepared.totals}, cats, preview_path)
    print(f"导入预览: {preview_path}")

    if not args.post:
        print("\n（未加 --post，未写入数据库）")
        return 0

    return _post(args, found.period, prepared, cats)


def _report_waivers(prepared) -> None:
    """Every escape hatch that fired says so — silence would defeat the gate."""
    if prepared.fold_summary.count:
        print(
            f"\n--allow-unknown: {prepared.fold_summary.count} 笔未分类共 "
            f"${prepared.fold_summary.total:,.2f} 已归入「其他/未分类」"
        )
    for target, amount in prepared.waived_gaps.items():
        print(f"--allow-gaps: {target} 充值 ${amount:,.2f} 无对应账单，这部分漏计")
    if prepared.waived_sources:
        print(f"--allow-missing-sources: 缺少 {', '.join(prepared.waived_sources)}，总额偏小")
    for source_id in prepared.manual_reminders:
        print(f"提示: {source_id} 无 CSV 导出，需在 ExpenseBatchUpdate 页手工补录")
    for source_id in prepared.pending_notices:
        print(f"提示: {source_id} 的解析器尚未实现，本月未覆盖")


def _post(args, period: str, prepared, cats: Categories) -> int:
    try:
        client = FinanceApiClient.from_env()
        client.login()  # apply_plan logs in again; both are idempotent
        plan = reconcile(local=prepared.totals, remote=client.get_records(period))
        result = apply_plan(client, period, plan, expense_type_for=cats.expense_type)
    except (CredentialsError, ApiError) as exc:
        print(f"\n写入失败: {exc}", file=sys.stderr)
        return 1

    print(f"\n已写入 {period}:")
    if result.posted_categories:
        for cid in result.posted_categories:
            print(f"  写入  {cats.major(cid)}/{cats.minor(cid)}  ${plan.to_post[cid]:,.2f}")
    if result.deleted_ids:
        for record in plan.to_delete:
            cid = record.minor_category_id
            print(f"  清除  {cats.major(cid)}/{cats.minor(cid)}  (原 ${record.amount:,.2f})")
    if not result.posted_categories and not result.deleted_ids:
        print("  无变化（库中状态已与本地聚合一致）")
    if result.preserved:
        print(f"  保留  {len(result.preserved)} 条非 USD 记录未触碰")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
