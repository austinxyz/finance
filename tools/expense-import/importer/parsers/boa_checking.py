"""Bank of America checking activity export.

The file opens with a five-line balance summary under its own
``Description,,Summary Amt.`` header, then a blank line, then the real header:

    Date,Description,Amount,Running Bal.

``header_search_rows`` lets the registry find that row instead of reading line 1
and concluding no parser matches.

Debits are negative, credits positive — same convention as Chase's exports.
Amounts carry thousands separators and are quoted; ``to_decimal`` handles both.
"""

from __future__ import annotations

from datetime import datetime

from ..models import Txn
from .base import Parser, register, to_decimal

#: The summary block is 5 lines plus a blank; scan a little past that.
_HEADER_SEARCH_ROWS = 12


@register
class BoaCheckingParser(Parser):
    source = "boa_checking"
    header_signature = ("Date", "Description", "Amount", "Running Bal.")
    header_search_rows = _HEADER_SEARCH_ROWS

    def parse_row(self, row: dict[str, str], account: str) -> Txn | None:
        raw_date = row.get("Date")
        raw_amount = row.get("Amount")
        # The opening-balance row carries a running balance but no amount; it is
        # a statement artefact, not a transaction.
        if not raw_date or not raw_amount:
            return None
        return Txn(
            source=self.source,
            account=account,
            txn_date=datetime.strptime(raw_date, "%m/%d/%Y").date(),
            description=" ".join((row.get("Description") or "").split()),
            amount=to_decimal(raw_amount),
        )
