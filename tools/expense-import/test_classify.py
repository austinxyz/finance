"""Tests for the classification rules engine.

These guard the failure mode that matters: a transfer or a top-up silently
becoming an expense, which inflates a category without any visible error.

Run: python -m unittest discover -s tools/expense-import
"""

from __future__ import annotations

import tempfile
import unittest
from datetime import date
from decimal import Decimal
from pathlib import Path

from importer.classify import RuleError, classify, load_rules
from importer.models import Action, Txn
from importer.report import aggregate

CATEGORIES = {67, 68, 81}


def txn(desc: str, amount: str, raw_type: str = "", day: int = 15) -> Txn:
    return Txn(
        source="chase_checking",
        account="Chase8798",
        txn_date=date(2026, 8, day),
        description=desc,
        amount=Decimal(amount),
        raw_type=raw_type,
    )


def rules_from(toml_text: str):
    with tempfile.NamedTemporaryFile("w", suffix=".toml", delete=False, encoding="utf-8") as fh:
        fh.write(toml_text)
        path = Path(fh.name)
    try:
        return load_rules(path, valid_categories=CATEGORIES)
    finally:
        path.unlink(missing_ok=True)


class TestExclusions(unittest.TestCase):
    """Transfers and top-ups must never land in a category."""

    def setUp(self) -> None:
        self.rules = rules_from("""
[[rule]]
name = "card-payment"
type_is = "LOAN_PMT"
action = "TRANSFER"

[[rule]]
name = "paypal-funding"
desc = '(?i)PAYPAL\\s+INST XFER'
action = "FUNDING"

[[rule]]
name = "utility"
desc = '(?i)^PGANDE'
action = "EXPENSE"
category = 68
""")

    def test_card_payment_is_transfer_not_expense(self) -> None:
        [result] = classify([txn("Payment to Chase card ending in 5198", "-4028.11", "LOAN_PMT")], self.rules)
        self.assertIs(result.action, Action.TRANSFER)
        self.assertIsNone(result.minor_category_id)
        self.assertEqual(result.counted_amount, Decimal("0"))

    def test_paypal_topup_is_funding_not_expense(self) -> None:
        [result] = classify([txn("PAYPAL INST XFER CROCOXU", "-43.00", "ACH_DEBIT")], self.rules)
        self.assertIs(result.action, Action.FUNDING)
        self.assertEqual(result.counted_amount, Decimal("0"))

    def test_real_expense_is_counted_as_positive(self) -> None:
        [result] = classify([txn("PGANDE WEB ONLINE", "-259.24", "ACH_DEBIT")], self.rules)
        self.assertIs(result.action, Action.EXPENSE)
        self.assertEqual(result.minor_category_id, 68)
        self.assertEqual(result.counted_amount, Decimal("259.24"))

    def test_unmatched_stays_unknown(self) -> None:
        [result] = classify([txn("SOME NEW MERCHANT", "-12.34")], self.rules)
        self.assertIs(result.action, Action.UNKNOWN)
        self.assertIsNone(result.minor_category_id)

    def test_excluded_rows_never_reach_the_totals(self) -> None:
        rows = classify(
            [
                txn("Payment to Chase card ending in 5198", "-4028.11", "LOAN_PMT"),
                txn("PAYPAL INST XFER CROCOXU", "-43.00"),
                txn("PGANDE WEB ONLINE", "-259.24"),
                txn("SOME NEW MERCHANT", "-12.34"),
            ],
            self.rules,
        )
        self.assertEqual(aggregate(rows), {"2026-08": {68: Decimal("259.24")}})


class TestOrderingAndRefunds(unittest.TestCase):
    def test_first_matching_rule_wins(self) -> None:
        rules = rules_from("""
[[rule]]
name = "uber-eats-food"
desc = '(?i)uber ?eats'
action = "EXPENSE"
category = 67

[[rule]]
name = "uber-ride"
desc = '(?i)uber'
action = "EXPENSE"
category = 81
""")
        [result] = classify([txn("UBER EATS SF", "-30.00")], rules)
        self.assertEqual(result.rule, "uber-eats-food")
        self.assertEqual(result.minor_category_id, 67)

    def test_refund_nets_out_within_category(self) -> None:
        rules = rules_from("""
[[rule]]
name = "grocery"
desc = '(?i)safeway'
action = "EXPENSE"
category = 67
""")
        rows = classify(
            [txn("SAFEWAY #123", "-100.00", day=3), txn("SAFEWAY #123 REFUND", "25.00", day=9)],
            rules,
        )
        self.assertEqual(aggregate(rows), {"2026-08": {67: Decimal("75.00")}})


class TestRuleValidation(unittest.TestCase):
    """Malformed rules must fail loudly at load time, never silently."""

    def test_expense_without_category_is_rejected(self) -> None:
        with self.assertRaises(RuleError):
            rules_from('[[rule]]\nname = "x"\ndesc = "a"\naction = "EXPENSE"\n')

    def test_unknown_category_id_is_rejected(self) -> None:
        with self.assertRaises(RuleError):
            rules_from('[[rule]]\nname = "x"\ndesc = "a"\naction = "EXPENSE"\ncategory = 9999\n')

    def test_category_on_non_expense_is_rejected(self) -> None:
        with self.assertRaises(RuleError):
            rules_from('[[rule]]\nname = "x"\ndesc = "a"\naction = "TRANSFER"\ncategory = 67\n')

    def test_bad_regex_is_rejected(self) -> None:
        with self.assertRaises(RuleError):
            rules_from('[[rule]]\nname = "x"\ndesc = "(unclosed"\naction = "TRANSFER"\n')


class TestShippedRules(unittest.TestCase):
    """The real rules.toml must load cleanly against the real category list."""

    def test_rules_file_is_valid(self) -> None:
        from importer.report import Categories

        here = Path(__file__).parent
        cats = Categories.load(here / "categories.toml")
        rules = load_rules(here / "rules.toml", valid_categories=cats.ids)
        self.assertGreater(len(rules), 10)

    def test_structural_rules_precede_merchant_rules(self) -> None:
        """A card payment must be caught before any merchant pattern can claim it."""
        from importer.report import Categories

        here = Path(__file__).parent
        cats = Categories.load(here / "categories.toml")
        rules = load_rules(here / "rules.toml", valid_categories=cats.ids)
        [result] = classify([txn("Payment to Chase card ending in 5198", "-3180.90", "LOAN_PMT")], rules)
        self.assertIs(result.action, Action.TRANSFER)


if __name__ == "__main__":
    unittest.main()
