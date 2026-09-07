"""Write gates — fail-closed checks that run before anything reaches the database.

Each gate stops one way the monthly total can be wrong while everything *looks*
fine. They are layered because they see different things:

- **Unknown** — a merchant no rule matches. Visible in the data.
- **Funding gap** — a PayPal/Venmo top-up whose platform statement is absent.
  Visible only because the checking account recorded the top-up.
- **Missing source** — a statement that was never exported at all. Visible in
  **no** CSV; the other two gates cannot see it, and the month is simply short.
  This is the only mechanism that catches it.

Every function here is pure: classified rows and config in, a verdict out. No
network, no filesystem, no printing — so the decision to block is testable
independently of how it is reported.
"""

from __future__ import annotations

from dataclasses import dataclass, field
from decimal import Decimal

from .models import Action, Classified
from .sources import Source

#: 「其他/未分类」 — where --allow-unknown parks transactions no rule matched.
UNCATEGORISED_MINOR_ID = 80


@dataclass(frozen=True)
class UnknownVerdict:
    offenders: list[Classified] = field(default_factory=list)

    @property
    def blocked(self) -> bool:
        return bool(self.offenders)

    @property
    def total(self) -> Decimal:
        return sum((abs(c.txn.amount) for c in self.offenders), Decimal("0"))


@dataclass(frozen=True)
class FoldSummary:
    """What --allow-unknown let through — always printed, never silent."""

    count: int = 0
    total: Decimal = Decimal("0")


@dataclass(frozen=True)
class FundingVerdict:
    missing_targets: list[str] = field(default_factory=list)
    missing_totals: dict[str, Decimal] = field(default_factory=dict)

    @property
    def blocked(self) -> bool:
        return bool(self.missing_targets)


@dataclass(frozen=True)
class SourceVerdict:
    missing: list[str] = field(default_factory=list)
    manual_reminders: list[str] = field(default_factory=list)
    pending_notices: list[str] = field(default_factory=list)

    @property
    def blocked(self) -> bool:
        return bool(self.missing)


def check_unknown(rows: list[Classified]) -> UnknownVerdict:
    """Block while any transaction is unmatched by every rule."""
    return UnknownVerdict(offenders=[r for r in rows if r.action is Action.UNKNOWN])


def fold_unknown_into_uncategorised(
    rows: list[Classified],
) -> tuple[list[Classified], FoldSummary]:
    """--allow-unknown: park unmatched *outflows* in 「其他/未分类」.

    Inflows are left as UNKNOWN. An unexplained credit is not an expense, and
    folding one in would subtract from the month's total — understating spending
    is exactly the failure this gate exists to prevent.
    """
    out: list[Classified] = []
    count = 0
    total = Decimal("0")

    for row in rows:
        if row.action is not Action.UNKNOWN or not row.txn.is_outflow:
            out.append(row)
            continue
        count += 1
        total += abs(row.txn.amount)
        out.append(
            Classified(
                txn=row.txn,
                action=Action.EXPENSE,
                minor_category_id=UNCATEGORISED_MINOR_ID,
                rule="--allow-unknown",
                note="未匹配任何规则，经 --allow-unknown 归入 其他/未分类",
            )
        )

    return out, FoldSummary(count=count, total=total)


def check_funding_gaps(
    rows: list[Classified], present_source_ids: set[str]
) -> FundingVerdict:
    """Block while a top-up has no matching platform statement in the directory.

    The money left the checking account and was spent; without that platform's
    CSV the spending has no categories and the month is short by exactly this
    amount.

    Only outflows count. A refund arriving back from the platform matches the
    same rule as a top-up, and counting it here would inflate the gap for money
    that came back rather than going out — an inflow-only month has no missing
    spend to report.
    """
    totals: dict[str, Decimal] = {}
    for row in rows:
        if row.action is not Action.FUNDING or not row.funding_target:
            continue
        if not row.txn.is_outflow:
            continue
        if row.funding_target in present_source_ids:
            continue
        totals[row.funding_target] = totals.get(row.funding_target, Decimal("0")) + abs(
            row.txn.amount
        )

    return FundingVerdict(missing_targets=sorted(totals), missing_totals=totals)


def check_missing_sources(
    declared: list[Source], present_source_ids: set[str]
) -> SourceVerdict:
    """Block while a declared, parser-backed source has no CSV in the directory.

    ``manual`` and ``pending`` sources are surfaced rather than blocked — the
    first has no CSV to export, the second has no parser yet — but neither is
    ever treated as covered. Silence about them is the failure mode.
    """
    missing: list[str] = []
    manual_reminders: list[str] = []
    pending_notices: list[str] = []

    for source in declared:
        if source.manual:
            manual_reminders.append(source.id)
        elif source.pending:
            pending_notices.append(source.id)
        elif source.id not in present_source_ids:
            missing.append(source.id)

    return SourceVerdict(
        missing=missing,
        manual_reminders=manual_reminders,
        pending_notices=pending_notices,
    )
