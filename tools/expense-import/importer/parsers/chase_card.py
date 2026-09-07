"""Chase credit card activity export.

Header: Transaction Date,Post Date,Description,Category,Type,Amount,Memo

**Purchases are negative here**, the same convention as Chase's checking export.
There is no general "credit cards report purchases as positive" rule — Robinhood
does, Chase does not — so no sign flip is applied.

The card is where the real spending lives; the matching payment in the checking
account is a transfer, and a rule marks it as such. Both sides being excluded on
the checking side and counted on the card side is what keeps a purchase from
being counted twice.
"""

from __future__ import annotations

from datetime import datetime

from ..models import Txn
from .base import Parser, register, to_decimal


@register
class ChaseCardParser(Parser):
    source = "chase_card"
    header_signature = ("Transaction Date", "Post Date", "Description", "Category", "Amount")

    def parse_row(self, row: dict[str, str], account: str) -> Txn | None:
        # Transaction Date, not Post Date: a purchase late in the month can post
        # in the next one, and posting date would misattribute the period.
        raw_date = row.get("Transaction Date")
        if not raw_date:
            return None
        return Txn(
            source=self.source,
            account=account,
            txn_date=datetime.strptime(raw_date, "%m/%d/%Y").date(),
            description=" ".join((row.get("Description") or "").split()),
            amount=to_decimal(row["Amount"]),
            # Chase's own category ("Gas", "Food & Drink") is a useful rule signal.
            raw_type=row.get("Category") or "",
        )
