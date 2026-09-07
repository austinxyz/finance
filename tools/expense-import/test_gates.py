"""Tests for the three write gates.

Every gate exists to stop one way the monthly total can be wrong without
anything looking wrong. They are fail-closed by construction: the default is to
refuse the write, and each escape hatch is an explicit command-line flag that
prints what it let through.

Run: python -m unittest discover -s tools/expense-import -t tools/expense-import -p "test_gates.py"
"""

from __future__ import annotations

import unittest
from datetime import date
from decimal import Decimal

from importer.gates import (
    UNCATEGORISED_MINOR_ID,
    check_funding_gaps,
    check_missing_sources,
    check_unknown,
    fold_unknown_into_uncategorised,
)
from importer.models import Action, Classified, Txn
from importer.sources import Source


def txn(desc: str, amount: str, day: int = 15) -> Txn:
    return Txn(
        source="chase_checking",
        account="Chase8798",
        txn_date=date(2026, 8, day),
        description=desc,
        amount=Decimal(amount),
    )


def unknown(desc: str, amount: str, day: int = 15) -> Classified:
    return Classified(txn=txn(desc, amount, day), action=Action.UNKNOWN)


def expense(desc: str, amount: str, cid: int = 68) -> Classified:
    return Classified(
        txn=txn(desc, amount), action=Action.EXPENSE, minor_category_id=cid, rule="r"
    )


def funding(desc: str, amount: str, target: str) -> Classified:
    return Classified(
        txn=txn(desc, amount), action=Action.FUNDING, funding_target=target, rule="f"
    )


class TestUnknownGate(unittest.TestCase):
    def test_blocks_when_unmatched_transactions_exist(self) -> None:
        verdict = check_unknown([expense("PGANDE", "-259.24"), unknown("MYSTERY CO", "-45.20")])

        self.assertTrue(verdict.blocked)
        self.assertEqual(len(verdict.offenders), 1)

    def test_reports_each_offender_with_date_amount_description(self) -> None:
        verdict = check_unknown([unknown("SQ *BLUE BOTTLE", "-45.20", day=3)])

        [row] = verdict.offenders
        self.assertEqual(row.txn.txn_date, date(2026, 8, 3))
        self.assertEqual(row.txn.amount, Decimal("-45.20"))
        self.assertIn("BLUE BOTTLE", row.txn.description)

    def test_passes_when_everything_is_classified(self) -> None:
        self.assertFalse(check_unknown([expense("PGANDE", "-259.24")]).blocked)

    def test_passes_on_empty_input(self) -> None:
        self.assertFalse(check_unknown([]).blocked)


class TestUnknownEscapeHatch(unittest.TestCase):
    """--allow-unknown must be loud: it changes the numbers."""

    def test_folds_unknown_into_uncategorised(self) -> None:
        rows, summary = fold_unknown_into_uncategorised(
            [expense("PGANDE", "-259.24"), unknown("MYSTERY", "-45.20")]
        )

        folded = [r for r in rows if r.minor_category_id == UNCATEGORISED_MINOR_ID]
        self.assertEqual(len(folded), 1)
        self.assertIs(folded[0].action, Action.EXPENSE)

    def test_reports_count_and_total_for_the_console(self) -> None:
        _, summary = fold_unknown_into_uncategorised(
            [unknown("A", "-45.20"), unknown("B", "-10.00")]
        )

        self.assertEqual(summary.count, 2)
        self.assertEqual(summary.total, Decimal("55.20"))

    def test_leaves_classified_rows_untouched(self) -> None:
        original = expense("PGANDE", "-259.24")
        rows, _ = fold_unknown_into_uncategorised([original, unknown("MYSTERY", "-45.20")])

        self.assertIn(original, rows)

    def test_inflow_unknowns_are_not_folded_into_an_expense(self) -> None:
        """An unexplained credit is not an expense; folding it would understate."""
        rows, summary = fold_unknown_into_uncategorised([unknown("REFUND?", "80.00")])

        self.assertTrue(all(r.action is not Action.EXPENSE for r in rows))
        self.assertEqual(summary.count, 0)


class TestFundingGapGate(unittest.TestCase):
    def test_blocks_when_topup_has_no_matching_statement(self) -> None:
        verdict = check_funding_gaps(
            [funding("PAYPAL INST XFER", "-161.59", "paypal")],
            present_source_ids={"chase_checking"},
        )

        self.assertTrue(verdict.blocked)
        self.assertIn("paypal", verdict.missing_targets)

    def test_reports_the_missing_amount(self) -> None:
        verdict = check_funding_gaps(
            [
                funding("PAYPAL A", "-100.00", "paypal"),
                funding("PAYPAL B", "-61.59", "paypal"),
            ],
            present_source_ids={"chase_checking"},
        )

        self.assertEqual(verdict.missing_totals["paypal"], Decimal("161.59"))

    def test_passes_when_the_statement_is_present(self) -> None:
        verdict = check_funding_gaps(
            [funding("PAYPAL INST XFER", "-161.59", "paypal")],
            present_source_ids={"chase_checking", "paypal"},
        )

        self.assertFalse(verdict.blocked)

    def test_only_the_missing_platform_is_flagged(self) -> None:
        verdict = check_funding_gaps(
            [
                funding("PAYPAL", "-100.00", "paypal"),
                funding("VENMO", "-50.00", "venmo"),
            ],
            present_source_ids={"paypal"},
        )

        self.assertEqual(set(verdict.missing_targets), {"venmo"})

    def test_refund_inflow_is_not_counted_as_a_gap(self) -> None:
        """A platform refund matches the same rule as a top-up.

        Counting it as a gap inflates the missing amount for money that came
        back rather than going out.
        """
        verdict = check_funding_gaps(
            [
                funding("PAYPAL INST XFER", "-100.00", "paypal"),
                funding("PAYPAL INST XFER REVERSAL", "40.00", "paypal"),
            ],
            present_source_ids={"chase_checking"},
        )

        self.assertEqual(verdict.missing_totals["paypal"], Decimal("100.00"))

    def test_inflow_only_funding_does_not_block(self) -> None:
        """Money came back and nothing went out — there is no missing spend."""
        verdict = check_funding_gaps(
            [funding("PAYPAL REFUND", "75.00", "paypal")],
            present_source_ids={"chase_checking"},
        )

        self.assertFalse(verdict.blocked)
        self.assertEqual(verdict.missing_targets, [])

    def test_passes_when_there_is_no_funding_at_all(self) -> None:
        self.assertFalse(
            check_funding_gaps([expense("PGANDE", "-259.24")], present_source_ids=set()).blocked
        )


class TestMissingSourceGate(unittest.TestCase):
    """The only gate that can see a statement which was never exported."""

    def test_blocks_when_a_declared_source_is_absent(self) -> None:
        verdict = check_missing_sources(
            declared=[
                Source(id="chase_checking", parser="chase_checking"),
                Source(id="chase_card", parser="chase_card"),
            ],
            present_source_ids={"chase_checking"},
        )

        self.assertTrue(verdict.blocked)
        self.assertEqual(verdict.missing, ["chase_card"])

    def test_passes_when_every_declared_source_is_present(self) -> None:
        verdict = check_missing_sources(
            declared=[Source(id="chase_checking", parser="chase_checking")],
            present_source_ids={"chase_checking"},
        )

        self.assertFalse(verdict.blocked)

    def test_manual_source_does_not_block_but_is_surfaced(self) -> None:
        verdict = check_missing_sources(
            declared=[
                Source(id="chase_checking", parser="chase_checking"),
                Source(id="robinhood_card", manual=True, label="Robinhood 信用卡"),
            ],
            present_source_ids={"chase_checking"},
        )

        self.assertFalse(verdict.blocked)
        self.assertEqual(verdict.manual_reminders, ["robinhood_card"])

    def test_pending_source_does_not_block_but_is_surfaced(self) -> None:
        """Its parser is not built yet — blocking would make the tool unusable."""
        verdict = check_missing_sources(
            declared=[
                Source(id="chase_checking", parser="chase_checking"),
                Source(id="paypal", parser="paypal", pending=True),
            ],
            present_source_ids={"chase_checking"},
        )

        self.assertFalse(verdict.blocked)
        self.assertEqual(verdict.pending_notices, ["paypal"])

    def test_manual_source_is_never_counted_as_covered(self) -> None:
        """Silence about a manual source is the failure this gate prevents."""
        verdict = check_missing_sources(
            declared=[Source(id="robinhood_card", manual=True)],
            present_source_ids=set(),
        )

        self.assertEqual(verdict.manual_reminders, ["robinhood_card"])


class TestNoCatchAllRule(unittest.TestCase):
    """A catch-all would silently absorb every new merchant, disarming gate 1."""

    def test_random_merchant_stays_unknown_under_shipped_rules(self) -> None:
        from pathlib import Path

        from importer.classify import classify, load_rules
        from importer.report import Categories

        here = Path(__file__).parent
        cats = Categories.load(here / "categories.toml")
        rules = load_rules(here / "rules.toml", valid_categories=cats.ids)

        [result] = classify([txn("ZZQX UNLIKELY MERCHANT 9174", "-12.34")], rules)

        self.assertIs(result.action, Action.UNKNOWN)
        self.assertEqual(result.rule, "")


if __name__ == "__main__":
    unittest.main()
