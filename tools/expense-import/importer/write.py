"""Write orchestration: run the gates, aggregate, reconcile, apply.

This is where the three gates actually stop a write and where their escape
hatches release it. The gates themselves only produce verdicts; without this
module they are advice nobody has to take.

Only ``--allow-unknown`` needs to transform anything (it moves unmatched outflows
into the uncategorised bucket). The other two flags simply mean "proceed despite
the verdict", so they are checked, reported, and stepped over here.
"""

from __future__ import annotations

from collections import defaultdict
from dataclasses import dataclass, field
from decimal import Decimal
from typing import Callable

from .apiclient import FinanceApiClient
from .gates import (
    FoldSummary,
    check_funding_gaps,
    check_missing_sources,
    check_unknown,
    fold_unknown_into_uncategorised,
)
from .models import Action, Classified
from .reconcile import ReconcilePlan, RemoteRecord
from .sources import Source


class WriteBlocked(RuntimeError):
    """A gate refused the write. The message names what to fix or which flag to pass."""


@dataclass(frozen=True)
class PreparedWrite:
    """Everything the gates approved, ready to reconcile against the remote period."""

    #: minor_category_id -> net amount, refunds already netted out
    totals: dict[int, Decimal] = field(default_factory=dict)
    rows: list[Classified] = field(default_factory=list)
    fold_summary: FoldSummary = field(default_factory=FoldSummary)
    manual_reminders: list[str] = field(default_factory=list)
    pending_notices: list[str] = field(default_factory=list)
    waived_gaps: dict[str, Decimal] = field(default_factory=dict)
    waived_sources: list[str] = field(default_factory=list)


@dataclass(frozen=True)
class WriteResult:
    posted_categories: list[int] = field(default_factory=list)
    deleted_ids: list[int] = field(default_factory=list)
    preserved: list[RemoteRecord] = field(default_factory=list)


def aggregate_totals(rows: list[Classified]) -> dict[int, Decimal]:
    """Sum expenses per minor category, netting refunds within each one."""
    totals: dict[int, Decimal] = defaultdict(Decimal)
    for row in rows:
        if row.action is not Action.EXPENSE or row.minor_category_id is None:
            continue
        totals[row.minor_category_id] += -row.txn.amount
    return dict(totals)


def plan_write(
    rows: list[Classified],
    declared_sources: list[Source],
    present_source_ids: set[str],
    *,
    allow_unknown: bool = False,
    allow_gaps: bool = False,
    allow_missing_sources: bool = False,
) -> PreparedWrite:
    """Run all three gates, then aggregate. Raises WriteBlocked on any refusal."""
    fold_summary = FoldSummary()

    unknown = check_unknown(rows)
    if unknown.blocked:
        if not allow_unknown:
            listing = "\n".join(
                f"  {c.txn.txn_date}  {c.txn.amount:>10}  {c.txn.description[:52]}"
                for c in unknown.offenders
            )
            raise WriteBlocked(
                f"拒绝写入: {len(unknown.offenders)} 笔未分类，共 ${unknown.total:,.2f}\n"
                f"{listing}\n"
                f"先在 rules.toml 补规则，或加 --allow-unknown 归入「其他/未分类」"
            )
        rows, fold_summary = fold_unknown_into_uncategorised(rows)

    funding = check_funding_gaps(rows, present_source_ids)
    if funding.blocked and not allow_gaps:
        listing = "\n".join(
            f"  {target}: ${funding.missing_totals[target]:,.2f}"
            for target in funding.missing_targets
        )
        raise WriteBlocked(
            f"拒绝写入: 有充值但缺对应账单，这部分开支会漏计\n{listing}\n"
            f"导入对应 CSV，或加 --allow-gaps 接受漏计"
        )

    sources = check_missing_sources(declared_sources, present_source_ids)
    if sources.blocked and not allow_missing_sources:
        raise WriteBlocked(
            f"拒绝写入: 缺少来源 {', '.join(sources.missing)}\n"
            f"月度总额会静默偏小。导入对应 CSV，或加 --allow-missing-sources"
        )

    return PreparedWrite(
        totals=aggregate_totals(rows),
        rows=rows,
        fold_summary=fold_summary,
        manual_reminders=sources.manual_reminders,
        pending_notices=sources.pending_notices,
        waived_gaps=funding.missing_totals if funding.blocked else {},
        waived_sources=sources.missing if sources.blocked else [],
    )


def apply_plan(
    client: FinanceApiClient,
    period: str,
    plan: ReconcilePlan,
    expense_type_for: Callable[[int], str],
) -> WriteResult:
    """Apply a reconcile plan. Deletions first, so a category can change identity.

    ``expense_type_for`` maps a minor category id to FIXED_DAILY or
    LARGE_IRREGULAR. The backend requires the field and validates it against
    exactly those two values, and the answer belongs to the category itself
    (``expense_categories_minor.expense_type``) rather than to any rule — so it is
    injected rather than guessed here.
    """
    client.login()

    for record in plan.to_delete:
        client.delete_record(record.id)

    if plan.to_post:
        client.batch_save(
            period,
            [
                {
                    "minorCategoryId": cid,
                    "amount": float(amount),
                    "currency": "USD",
                    "expenseType": expense_type_for(cid),
                    "description": f"自动导入 {period}",
                }
                for cid, amount in sorted(plan.to_post.items())
            ],
        )

    return WriteResult(
        posted_categories=sorted(plan.to_post),
        deleted_ids=[r.id for r in plan.to_delete],
        preserved=plan.untouched,
    )
