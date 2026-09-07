"""PayPal activity export (Activity → Statements → Custom → Download → CSV).

Header begins: Date,Time,TimeZone,Name,Type,Status,Currency,Amount,...

Amounts are signed the same way as a bank export, so no flip.

The rows typed ``Bank Deposit to PP Account`` are the **mirror** of the
``PAYPAL INST XFER`` debits in checking: the same money, seen from the other
side. Both sides have to be excluded, or the top-up is counted once as a
checking outflow and again as a PayPal inflow. The type is preserved in
``raw_type`` so a rule can mark it TRANSFER.

Pending rows are dropped: a pending charge can still reverse, and it reappears
as Completed in a later export once it settles.
"""

from __future__ import annotations

from datetime import datetime

from ..models import Txn
from .base import Parser, register, to_decimal

_SETTLED = "completed"


@register
class PayPalParser(Parser):
    source = "paypal"
    header_signature = ("Date", "Name", "Type", "Status", "Currency", "Amount", "Transaction ID")

    def parse_row(self, row: dict[str, str], account: str) -> Txn | None:
        if (row.get("Status") or "").strip().lower() != _SETTLED:
            return None
        raw_date = row.get("Date")
        raw_amount = row.get("Amount")
        if not raw_date or not raw_amount:
            return None

        # Rules match on description, and for PayPal the counterparty name is
        # the only merchant signal; the type disambiguates deposits from spend.
        name = " ".join((row.get("Name") or "").split())
        kind = " ".join((row.get("Type") or "").split())
        description = f"{name} {kind}".strip() if name else kind

        return Txn(
            source=self.source,
            account=account,
            txn_date=datetime.strptime(raw_date, "%m/%d/%Y").date(),
            description=description,
            amount=to_decimal(raw_amount),
            raw_type=kind,
        )
