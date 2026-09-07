"""Normalized transaction model shared by all institution parsers."""

from dataclasses import dataclass
from datetime import date
from decimal import Decimal
from enum import Enum


class Action(str, Enum):
    """What a transaction means for expense accounting."""

    EXPENSE = "EXPENSE"      # 真实开支，计入统计
    INCOME = "INCOME"        # 收入，不计入开支
    TRANSFER = "TRANSFER"    # 账户间转账（含信用卡还款），必须排除
    FUNDING = "FUNDING"      # 给 PayPal/Venmo 充值，真实开支在对方账单里
    IGNORE = "IGNORE"        # 明确忽略（利息、手续费冲抵等）
    UNKNOWN = "UNKNOWN"      # 无规则匹配，需人工定规则


#: Actions that contribute to the monthly category totals.
COUNTED = frozenset({Action.EXPENSE})

#: Actions that must never silently become expenses.
EXCLUDED = frozenset({Action.INCOME, Action.TRANSFER, Action.FUNDING, Action.IGNORE})


@dataclass(frozen=True)
class Txn:
    """One transaction, normalized across institutions.

    ``amount`` keeps the institution's sign convention: negative is money out.
    """

    source: str          # parser id, e.g. "chase_checking"
    account: str         # account label, e.g. "Chase8798"
    txn_date: date
    description: str
    amount: Decimal
    raw_type: str = ""   # institution's own type code, e.g. "LOAN_PMT"

    @property
    def period(self) -> str:
        """Accounting period in YYYY-MM, matching the backend's expensePeriod."""
        return f"{self.txn_date:%Y-%m}"

    @property
    def is_outflow(self) -> bool:
        return self.amount < 0


@dataclass(frozen=True)
class Classified:
    """A transaction plus the verdict of the rules engine."""

    txn: Txn
    action: Action
    minor_category_id: int | None = None
    rule: str = ""
    note: str = ""

    @property
    def counted_amount(self) -> Decimal:
        """Positive magnitude for expenses; zero for everything else.

        The backend requires ``amount > 0``, and refunds inside a category must
        net out rather than be dropped, so credits stay signed here.
        """
        if self.action not in COUNTED:
            return Decimal("0")
        return -self.txn.amount
