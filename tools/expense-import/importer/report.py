"""Review outputs: a per-transaction CSV, a console summary, and a batch preview.

Nothing here touches the database. The batch preview mirrors the shape of
``POST /api/expenses/records/batch`` so phase B is a straight hand-off.
"""

from __future__ import annotations

import csv
import json
import tomllib
from collections import defaultdict
from decimal import Decimal
from pathlib import Path

from .models import Action, Classified

#: UNKNOWN first (needs a rule), then real expenses, then the excluded noise.
_ACTION_ORDER = {
    Action.UNKNOWN: 0,
    Action.EXPENSE: 1,
    Action.FUNDING: 2,
    Action.TRANSFER: 3,
    Action.INCOME: 4,
    Action.IGNORE: 5,
}

REVIEW_COLUMNS = [
    "状态", "日期", "账户", "描述", "金额",
    "机构类型", "命中规则", "大类", "小类", "小类ID", "备注",
]


class Categories:
    """Lookup for minor category id -> names and expense type."""

    def __init__(self, entries: dict[int, dict[str, str]]) -> None:
        self._entries = entries

    @classmethod
    def load(cls, path: Path) -> Categories:
        with path.open("rb") as fh:
            data = tomllib.load(fh)
        entries = {int(c["id"]): c for c in data.get("category", [])}
        if not entries:
            raise ValueError(f"{path} contains no categories")
        return cls(entries)

    @property
    def ids(self) -> set[int]:
        return set(self._entries)

    def major(self, cid: int | None) -> str:
        return self._entries.get(cid, {}).get("major", "") if cid else ""

    def minor(self, cid: int | None) -> str:
        return self._entries.get(cid, {}).get("minor", "") if cid else ""

    def expense_type(self, cid: int) -> str:
        return self._entries.get(cid, {}).get("expense_type", "FIXED_DAILY")


def write_review(rows: list[Classified], cats: Categories, out: Path) -> None:
    """Write every transaction with its verdict, most-actionable first."""
    ordered = sorted(
        rows,
        key=lambda c: (_ACTION_ORDER.get(c.action, 9), c.txn.txn_date, c.txn.description),
    )
    # utf-8-sig so Excel on Windows renders the Chinese headers correctly.
    with out.open("w", newline="", encoding="utf-8-sig") as fh:
        writer = csv.writer(fh)
        writer.writerow(REVIEW_COLUMNS)
        for item in ordered:
            txn = item.txn
            writer.writerow([
                item.action.value,
                txn.txn_date.isoformat(),
                txn.account,
                txn.description,
                f"{txn.amount:.2f}",
                txn.raw_type,
                item.rule,
                cats.major(item.minor_category_id),
                cats.minor(item.minor_category_id),
                item.minor_category_id or "",
                item.note,
            ])


def aggregate(rows: list[Classified]) -> dict[str, dict[int, Decimal]]:
    """Sum expenses into {period: {minor_category_id: amount}}.

    Refunds inside a category net out here rather than being dropped.
    """
    totals: dict[str, dict[int, Decimal]] = defaultdict(lambda: defaultdict(Decimal))
    for item in rows:
        if item.action is not Action.EXPENSE or item.minor_category_id is None:
            continue
        totals[item.txn.period][item.minor_category_id] += item.counted_amount
    return {period: dict(by_cat) for period, by_cat in totals.items()}


def write_batch_preview(
    totals: dict[str, dict[int, Decimal]],
    cats: Categories,
    out: Path,
    family_id: int = 1,
) -> None:
    """Emit one BatchExpenseRecordRequest-shaped payload per period."""
    payloads = []
    for period in sorted(totals):
        records = [
            {
                "minorCategoryId": cid,
                "amount": float(amount),
                "currency": "USD",
                "expenseType": cats.expense_type(cid),
                "description": f"自动导入 {period}",
            }
            for cid, amount in sorted(totals[period].items())
            if amount > 0  # backend rejects amount <= 0
        ]
        payloads.append({"familyId": family_id, "expensePeriod": period, "records": records})
    out.write_text(json.dumps(payloads, ensure_ascii=False, indent=2), encoding="utf-8")


def print_summary(rows: list[Classified], totals: dict[str, dict[int, Decimal]], cats: Categories) -> None:
    counts: dict[Action, int] = defaultdict(int)
    sums: dict[Action, Decimal] = defaultdict(Decimal)
    for item in rows:
        counts[item.action] += 1
        sums[item.action] += abs(item.txn.amount)

    print(f"\n{'=' * 62}\n交易分类统计  共 {len(rows)} 笔\n{'=' * 62}")
    for action in sorted(counts, key=lambda a: _ACTION_ORDER.get(a, 9)):
        print(f"  {action.value:<9} {counts[action]:>4} 笔   ${sums[action]:>12,.2f}")

    for period in sorted(totals):
        by_cat = totals[period]
        print(f"\n{'-' * 62}\n{period} 归类开支（这就是会 POST 的内容）\n{'-' * 62}")
        for cid, amount in sorted(by_cat.items(), key=lambda kv: -kv[1]):
            label = f"{cats.major(cid)}/{cats.minor(cid)}"
            print(f"  {label:<22} ${amount:>10,.2f}   (id={cid})")
        print(f"  {'合计':<22} ${sum(by_cat.values()):>10,.2f}")

    unknown = [c for c in rows if c.action is Action.UNKNOWN]
    if unknown:
        print(f"\n{'!' * 62}\n未匹配 {len(unknown)} 笔 — 需要在 rules.toml 加规则\n{'!' * 62}")
        for item in unknown:
            print(f"  {item.txn.txn_date}  ${item.txn.amount:>10,.2f}  {item.txn.description[:52]}")

    funding = [c for c in rows if c.action is Action.FUNDING]
    if funding:
        total = sum(abs(c.txn.amount) for c in funding)
        print(
            f"\n注意: {len(funding)} 笔 PayPal/Venmo 充值共 ${total:,.2f} 未计入。"
            f"\n     真实开支在对应账单里，导入 PayPal/Venmo CSV 后才完整。"
        )
