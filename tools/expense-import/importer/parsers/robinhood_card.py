"""Robinhood credit card export (Settings → Documents → Export CSV to Email).

Header: Date,Time,Cardholder,Amount,Points,Balance,Status,Type,Merchant,Description

Two things here differ from every other source:

**Purchases are POSITIVE** and must be flipped. Chase's card reports them
negative; there is no "credit cards are positive" rule, only per-institution
facts.

**Declined attempts appear in the file.** A declined retry carries the same
amount and date as the charge that then succeeded, so keeping it silently
doubles the purchase. Only ``Posted`` rows survive.
"""

from __future__ import annotations

from datetime import datetime

from ..models import Txn
from .base import Parser, register, to_decimal

#: Only settled rows are real. "Declined" duplicates a successful retry.
_SETTLED = "posted"


@register
class RobinhoodCardParser(Parser):
    source = "robinhood_card"
    header_signature = ("Date", "Cardholder", "Amount", "Points", "Status", "Merchant")

    def parse_row(self, row: dict[str, str], account: str) -> Txn | None:
        if (row.get("Status") or "").strip().lower() != _SETTLED:
            return None
        raw_date = row.get("Date")
        if not raw_date:
            return None

        # Positive means money out here, so flip to the shared convention.
        amount = -to_decimal(row["Amount"])

        # Description carries the full merchant string; Merchant alone is a
        # normalised brand name and loses the detail rules key off.
        detail = " ".join((row.get("Description") or "").split())
        merchant = " ".join((row.get("Merchant") or "").split())

        return Txn(
            source=self.source,
            account=account,
            txn_date=datetime.strptime(raw_date, "%Y-%m-%d").date(),
            description=detail or merchant,
            amount=amount,
            raw_type=row.get("Type") or "",
        )
