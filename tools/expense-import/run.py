#!/usr/bin/env python3
"""Classify bank CSV exports into expense categories — offline, read-only.

Usage:
    python run.py <csv> [<csv> ...] [--period 2026-08] [--outdir out]

Writes ``review.csv`` (per-transaction verdicts) and ``batch_preview.json``
(what a future import would POST), and prints a summary. Touches no database.
"""

from __future__ import annotations

import argparse
import sys
from pathlib import Path

from importer import (
    RuleError,
    aggregate,
    classify,
    load_rules,
    parser_for,
    print_summary,
    write_batch_preview,
    write_review,
)
from importer.report import Categories

HERE = Path(__file__).parent


def _force_utf8_stdout() -> None:
    """Windows consoles default to a legacy codepage that mangles Chinese output."""
    for stream in (sys.stdout, sys.stderr):
        if hasattr(stream, "reconfigure"):
            stream.reconfigure(encoding="utf-8", errors="replace")


def main(argv: list[str] | None = None) -> int:
    _force_utf8_stdout()
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("csvs", nargs="+", type=Path, help="bank CSV export(s)")
    ap.add_argument("--period", help="only keep this YYYY-MM period")
    ap.add_argument("--outdir", type=Path, default=HERE / "out", help="output directory (default: ./out)")
    ap.add_argument("--rules", type=Path, default=HERE / "rules.toml")
    ap.add_argument("--categories", type=Path, default=HERE / "categories.toml")
    ap.add_argument("--family-id", type=int, default=1, help="familyId for the batch preview")
    args = ap.parse_args(argv)

    try:
        cats = Categories.load(args.categories)
        rules = load_rules(args.rules, valid_categories=cats.ids)
    except (RuleError, ValueError, FileNotFoundError) as exc:
        print(f"配置错误: {exc}", file=sys.stderr)
        return 2

    txns = []
    for path in args.csvs:
        if not path.exists():
            print(f"文件不存在: {path}", file=sys.stderr)
            return 2
        try:
            parser = parser_for(path)
        except ValueError as exc:
            print(f"{exc}", file=sys.stderr)
            return 2
        parsed = parser.parse(path)
        print(f"读取 {path.name}: {len(parsed)} 笔  [{parser.source}]")
        txns.extend(parsed)

    if args.period:
        before = len(txns)
        txns = [t for t in txns if t.period == args.period]
        print(f"按期间 {args.period} 过滤: {before} -> {len(txns)} 笔")

    if not txns:
        print("没有可处理的交易。", file=sys.stderr)
        return 1

    rows = classify(txns, rules)
    totals = aggregate(rows)

    args.outdir.mkdir(parents=True, exist_ok=True)
    review_path = args.outdir / "review.csv"
    preview_path = args.outdir / "batch_preview.json"
    write_review(rows, cats, review_path)
    write_batch_preview(totals, cats, preview_path, family_id=args.family_id)

    print_summary(rows, totals, cats)
    print(f"\n明细核对: {review_path}")
    print(f"导入预览: {preview_path}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
