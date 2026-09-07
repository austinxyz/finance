"""Chase checking account activity export.

Header: Details,Posting Date,Description,Amount,Type,Balance,Check or Slip #
Rows carry trailing commas, so DictReader collects the overflow under _extra.
"""

from __future__ import annotations

from datetime import datetime

from ..models import Txn
from .base import Parser, register, to_decimal


@register
class ChaseCheckingParser(Parser):
    source = "chase_checking"
    header_signature = ("Details", "Posting Date", "Description", "Amount", "Type")

    def parse_row(self, row: dict[str, str], account: str) -> Txn | None:
        raw_date = row.get("Posting Date")
        if not raw_date:
            return None
        return Txn(
            source=self.source,
            account=account,
            txn_date=datetime.strptime(raw_date, "%m/%d/%Y").date(),
            description=" ".join((row.get("Description") or "").split()),
            amount=to_decimal(row["Amount"]),
            raw_type=row.get("Type") or "",
        )
