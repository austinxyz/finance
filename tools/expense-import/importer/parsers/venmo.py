"""Venmo account statement export.

Two preamble lines carry the account name, so the real header sits on row 3 and
begins with an empty column. ``header_search_rows`` finds it.

Amounts are text with an explicit sign: ``+ $45.00`` / ``- $32.00``.

In practice this account is not a spending channel but a reimbursement one: you
front a court booking or a group dinner and people pay you back. Inbound rows
are therefore kept as positive amounts so that classifying them into the same
category nets them against that month's spending, rather than being counted as
income and leaving the category overstated.

The ``Note`` becomes the description because it is the only field that says what
the money was for. It is free text, so new phrasings land in UNKNOWN each month
— Venmo is the main source of ongoing rule additions.
"""

from __future__ import annotations

from datetime import datetime
from decimal import Decimal

from ..models import Txn
from .base import Parser, register, to_decimal

#: Preamble is 2 lines; scan a little past in case the export gains a row.
_HEADER_SEARCH_ROWS = 8

_SETTLED = "complete"


@register
class VenmoParser(Parser):
    source = "venmo"
    header_signature = ("ID", "Datetime", "Type", "Status", "Note", "From", "To", "Amount (total)")
    header_search_rows = _HEADER_SEARCH_ROWS

    def parse_row(self, row: dict[str, str], account: str) -> Txn | None:
        # Balance-only rows carry no ID; they are statement scaffolding.
        if not (row.get("ID") or "").strip():
            return None
        if (row.get("Status") or "").strip().lower() != _SETTLED:
            return None

        raw_amount = (row.get("Amount (total)") or "").strip()
        raw_datetime = (row.get("Datetime") or "").strip()
        if not raw_amount or not raw_datetime:
            return None

        amount = _signed_amount(raw_amount)
        note = " ".join((row.get("Note") or "").split())
        # On an inbound row the user is the To; on an outbound row they are the
        # From. Picking by direction keeps the fallback describing the other
        # party rather than the account holder.
        other_side = row.get("From") if amount > 0 else row.get("To")
        counterparty = " ".join((other_side or "").split())

        return Txn(
            source=self.source,
            account=account,
            txn_date=datetime.fromisoformat(raw_datetime).date(),
            description=note or counterparty,
            amount=amount,
            raw_type=row.get("Type") or "",
        )


def _signed_amount(raw: str) -> Decimal:
    """Parse Venmo's ``+ $45.00`` / ``- $32.00`` into a signed Decimal."""
    text = raw.replace(" ", "")
    if text.startswith("+"):
        return to_decimal(text[1:])
    if text.startswith("-"):
        return -to_decimal(text[1:])
    return to_decimal(text)
