"""Reconciling diff — make the period's remote state equal the local aggregate.

Posting alone is not enough. The batch endpoint upserts, so a category written
under a rule that later turns out to be wrong stays in the database forever: the
corrected run simply stops mentioning it. Reconciling adds the other half — what
is no longer in the local aggregate is removed.

**Currency isolation is the critical invariant here.** ``expense_records`` is keyed
by (family, period, minor category, currency), so a category can hold one row per
currency. Records entered by hand through the app's currency selector sit in the
same period as ours and are, structurally, "remote rows this run has no local
counterpart for" — exactly the shape this module deletes. Filtering to the
currency this tool writes is the only thing standing between a reconcile and
silently destroying hand-entered data that cannot be recovered from here.

Pure functions only: aggregates and records in, a plan out. No network.
"""

from __future__ import annotations

from dataclasses import dataclass, field
from decimal import Decimal

#: The only currency this tool writes — and therefore the only one it may remove.
MANAGED_CURRENCY = "USD"


@dataclass(frozen=True)
class RemoteRecord:
    """One existing ``expense_records`` row, as returned by the API."""

    id: int
    minor_category_id: int
    amount: Decimal
    currency: str


@dataclass(frozen=True)
class ReconcilePlan:
    #: minor_category_id -> amount to write (only categories that changed)
    to_post: dict[int, Decimal] = field(default_factory=dict)
    #: rows to remove — always MANAGED_CURRENCY, never anything else
    to_delete: list[RemoteRecord] = field(default_factory=list)
    #: rows deliberately left alone, kept for reporting so the skip is visible
    untouched: list[RemoteRecord] = field(default_factory=list)

    @property
    def is_empty(self) -> bool:
        return not self.to_post and not self.to_delete


def reconcile(
    local: dict[int, Decimal],
    remote: list[RemoteRecord],
    currency: str = MANAGED_CURRENCY,
) -> ReconcilePlan:
    """Diff the local aggregate against the remote period.

    ``local`` maps minor category id to the period's net amount. Categories whose
    net is zero or negative cannot be posted (the backend enforces ``> 0``) and
    are treated as absent — which means an existing row for them is deleted
    rather than left standing at a stale figure.
    """
    managed = [r for r in remote if r.currency == currency]
    untouched = [r for r in remote if r.currency != currency]

    by_category = {r.minor_category_id: r for r in managed}
    postable = {cid: amount for cid, amount in local.items() if amount > 0}

    to_post = {
        cid: amount
        for cid, amount in postable.items()
        if cid not in by_category or by_category[cid].amount != amount
    }
    to_delete = [r for cid, r in by_category.items() if cid not in postable]

    return ReconcilePlan(
        to_post=to_post,
        to_delete=sorted(to_delete, key=lambda r: r.id),
        untouched=untouched,
    )
