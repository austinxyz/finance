"""Tests that exercise run.py's own output.

The unit tests cover what the write path decides; these cover what it says.
Twice now a reporting change looked applied but was not, and the failure only
surfaced as an AttributeError in front of the user after a real write had
already gone through — the exact half-applied state the reporting exists to
describe.

Run: python -m unittest discover -s tools/expense-import -t tools/expense-import -p "test_cli.py"
"""

from __future__ import annotations

import io
import unittest
from contextlib import redirect_stdout
from decimal import Decimal
from pathlib import Path

import run
from importer.reconcile import RemoteRecord, reconcile
from importer.report import Categories
from importer.write import WriteResult

HERE = Path(__file__).parent


class FakeArgs:
    post = True


class StubClient:
    def __init__(self) -> None:
        self.posted: list = []

    def login(self) -> None: ...
    def load_family_id(self) -> int:
        return 1

    def get_records(self, period: str):
        return [RemoteRecord(id=9, minor_category_id=93, amount=Decimal("28.00"), currency="USD")]

    def batch_save(self, period: str, records: list[dict]) -> dict:
        self.posted.append(records)
        return {"success": True}


class PreparedStub:
    def __init__(self, totals: dict[int, Decimal]) -> None:
        self.totals = totals


class TestPostReporting(unittest.TestCase):
    def setUp(self) -> None:
        self.cats = Categories.load(HERE / "categories.toml")

    def _run_post(self, totals: dict[int, Decimal]) -> str:
        client = StubClient()
        plan = reconcile(local=totals, remote=client.get_records("2026-08"))
        result = WriteResult(
            posted_categories=sorted(plan.to_post),
            stale=list(plan.to_delete),
            preserved=plan.untouched,
        )
        buffer = io.StringIO()
        with redirect_stdout(buffer):
            run._report_post(result, plan, "2026-08", self.cats)
        return buffer.getvalue()

    def test_written_categories_are_named(self) -> None:
        output = self._run_post({94: Decimal("28.00")})
        self.assertIn("数码产品", output)
        self.assertIn("28.00", output)

    def test_superseded_category_is_reported_loudly(self) -> None:
        """A row the corrected rules no longer produce must be impossible to miss."""
        output = self._run_post({94: Decimal("28.00")})

        self.assertIn("健身美容", output)
        self.assertIn("id=9", output)
        self.assertIn("!!!", output, "the leftover block must stand out")

    def test_report_explains_why_it_was_not_deleted(self) -> None:
        output = self._run_post({94: Decimal("28.00")})
        self.assertIn("is_protected", output)

    def test_no_leftover_block_when_nothing_is_superseded(self) -> None:
        output = self._run_post({93: Decimal("28.00")})
        self.assertNotIn("!!!", output)

    def test_unchanged_period_says_so(self) -> None:
        output = self._run_post({93: Decimal("28.00")})
        self.assertIn("无变化", output)

    def test_reporting_touches_no_removed_attribute(self) -> None:
        """Guards the AttributeError that reached the user twice."""
        for totals in ({94: Decimal("28.00")}, {93: Decimal("28.00")}, {}):
            with self.subTest(totals=totals):
                self._run_post(totals)  # must not raise


if __name__ == "__main__":
    unittest.main()
