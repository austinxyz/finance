"""Rules engine: map normalized transactions onto an Action + category.

Rules live in ``rules.toml`` and are evaluated in file order — first match wins,
so put specific patterns above general ones. Every verdict carries the rule name
that produced it, so nothing is classified silently.
"""

from __future__ import annotations

import re
import tomllib
from dataclasses import dataclass
from decimal import Decimal
from pathlib import Path

from .models import Action, Classified, Txn


@dataclass(frozen=True)
class Rule:
    name: str
    action: Action
    desc: re.Pattern[str] | None = None
    type_is: str | None = None
    source_is: str | None = None
    amount_min: Decimal | None = None   # inclusive, compares on magnitude
    amount_max: Decimal | None = None   # inclusive, compares on magnitude
    minor_category_id: int | None = None
    funding_target: str | None = None
    note: str = ""

    def matches(self, txn: Txn) -> bool:
        if self.desc is not None and not self.desc.search(txn.description):
            return False
        if self.type_is is not None and txn.raw_type != self.type_is:
            return False
        if self.source_is is not None and txn.source != self.source_is:
            return False
        magnitude = abs(txn.amount)
        if self.amount_min is not None and magnitude < self.amount_min:
            return False
        if self.amount_max is not None and magnitude > self.amount_max:
            return False
        return True


class RuleError(ValueError):
    """Raised when rules.toml is malformed — fail loudly, never guess."""


def load_rules(path: Path, valid_categories: set[int] | None = None) -> list[Rule]:
    with path.open("rb") as fh:
        data = tomllib.load(fh)

    rules: list[Rule] = []
    for index, entry in enumerate(data.get("rule", []), start=1):
        where = f"rule #{index} ({entry.get('name', 'unnamed')})"

        try:
            action = Action(entry["action"])
        except KeyError as exc:
            raise RuleError(f"{where}: missing 'action'") from exc
        except ValueError as exc:
            raise RuleError(
                f"{where}: unknown action {entry['action']!r}; "
                f"valid: {', '.join(a.value for a in Action)}"
            ) from exc

        category = entry.get("category")
        if action is Action.EXPENSE and category is None:
            raise RuleError(f"{where}: EXPENSE rules need a 'category' (minor category id)")
        if category is not None and action is not Action.EXPENSE:
            raise RuleError(f"{where}: only EXPENSE rules may set 'category'")
        if category is not None and valid_categories is not None and category not in valid_categories:
            raise RuleError(f"{where}: category {category} is not an active minor category")

        funding_target = entry.get("funding_target")
        if action is Action.FUNDING and not funding_target:
            raise RuleError(
                f"{where}: FUNDING 规则必须声明 'funding_target'（如 \"paypal\"）—— "
                f"缺了它，充值缺口闸门无从判断缺的是哪家来源，等于被静默关闭"
            )
        if funding_target is not None and action is not Action.FUNDING:
            raise RuleError(f"{where}: 只有 FUNDING 规则可以设置 'funding_target'")

        pattern = entry.get("desc")
        try:
            compiled = re.compile(pattern) if pattern else None
        except re.error as exc:
            raise RuleError(f"{where}: bad regex {pattern!r}: {exc}") from exc

        rules.append(
            Rule(
                name=entry.get("name") or f"rule-{index}",
                action=action,
                desc=compiled,
                type_is=entry.get("type_is"),
                source_is=entry.get("source_is"),
                amount_min=_opt_decimal(entry.get("amount_min"), where, "amount_min"),
                amount_max=_opt_decimal(entry.get("amount_max"), where, "amount_max"),
                minor_category_id=category,
                funding_target=funding_target,
                note=entry.get("note", ""),
            )
        )

    if not rules:
        raise RuleError(f"{path} defines no rules")
    return rules


def _opt_decimal(value: object, where: str, field: str) -> Decimal | None:
    if value is None:
        return None
    try:
        return Decimal(str(value))
    except Exception as exc:  # noqa: BLE001 - surface any malformed number the same way
        raise RuleError(f"{where}: {field} must be a number, got {value!r}") from exc


def classify(txns: list[Txn], rules: list[Rule]) -> list[Classified]:
    """Apply rules to every transaction; unmatched ones become UNKNOWN."""
    out: list[Classified] = []
    for txn in txns:
        verdict = next((r for r in rules if r.matches(txn)), None)
        if verdict is None:
            out.append(Classified(txn=txn, action=Action.UNKNOWN, rule="", note="需要新规则"))
        else:
            out.append(
                Classified(
                    txn=txn,
                    action=verdict.action,
                    minor_category_id=verdict.minor_category_id,
                    funding_target=verdict.funding_target,
                    rule=verdict.name,
                    note=verdict.note,
                )
            )
    return out
