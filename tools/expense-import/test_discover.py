"""Tests for directory-driven source discovery and period attribution.

Run: python -m unittest discover -s tools/expense-import -t tools/expense-import -p "test_discover.py"
"""

from __future__ import annotations

import tempfile
import unittest
from pathlib import Path

import os
from unittest import mock

from importer.discover import (
    DEFAULT_DATA_ROOT_ENV,
    DiscoveryError,
    data_root,
    discover,
    period_dir,
    period_from_dirname,
)

CHASE_CHECKING_CSV = """Details,Posting Date,Description,Amount,Type,Balance,Check or Slip #
DEBIT,08/26/2026,"PGANDE WEB ONLINE",-259.24,ACH_DEBIT,6030.78,,
DEBIT,08/10/2026,"PAYPAL           INST XFER  XFWANG",-24.00,ACH_DEBIT,7412.67,,
"""

UNKNOWN_HEADER_CSV = """Foo,Bar,Baz
1,2,3
"""


def write(dirpath: Path, name: str, content: str) -> Path:
    path = dirpath / name
    path.write_text(content, encoding="utf-8")
    return path


class TestPeriodFromDirname(unittest.TestCase):
    def test_valid_period(self) -> None:
        self.assertEqual(period_from_dirname("2026-08"), "2026-08")

    def test_december_boundary(self) -> None:
        self.assertEqual(period_from_dirname("2026-12"), "2026-12")

    def test_non_period_name_is_rejected(self) -> None:
        with self.assertRaises(DiscoveryError) as ctx:
            period_from_dirname("august")
        self.assertIn("YYYY-MM", str(ctx.exception))

    def test_impossible_month_is_rejected(self) -> None:
        with self.assertRaises(DiscoveryError):
            period_from_dirname("2026-13")


class TestDiscover(unittest.TestCase):
    def test_finds_sources_and_counts_transactions(self) -> None:
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp) / "2026-08"
            root.mkdir()
            write(root, "Chase8798_Activity.csv", CHASE_CHECKING_CSV)

            result = discover(root)

            self.assertEqual(result.period, "2026-08")
            self.assertEqual([s.source for s in result.sources], ["chase_checking"])
            self.assertEqual(result.sources[0].count, 2)
            self.assertEqual(len(result.transactions), 2)

    def test_ignores_non_csv_files(self) -> None:
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp) / "2026-08"
            root.mkdir()
            write(root, "Chase8798_Activity.csv", CHASE_CHECKING_CSV)
            write(root, "notes.txt", "not a statement")

            result = discover(root)

            self.assertEqual(len(result.sources), 1)

    def test_missing_directory_is_rejected(self) -> None:
        with tempfile.TemporaryDirectory() as tmp:
            missing = Path(tmp) / "2026-08"
            with self.assertRaises(DiscoveryError) as ctx:
                discover(missing)
            self.assertIn(str(missing), str(ctx.exception))
            self.assertFalse(missing.exists(), "discover must not create the directory")

    def test_empty_directory_is_rejected(self) -> None:
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp) / "2026-08"
            root.mkdir()
            with self.assertRaises(DiscoveryError) as ctx:
                discover(root)
            self.assertIn("CSV", str(ctx.exception))

    def test_unrecognised_header_is_rejected_with_diagnostics(self) -> None:
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp) / "2026-08"
            root.mkdir()
            write(root, "mystery.csv", UNKNOWN_HEADER_CSV)

            with self.assertRaises(DiscoveryError) as ctx:
                discover(root)

            message = str(ctx.exception)
            self.assertIn("mystery.csv", message)
            self.assertIn("Foo", message, "must print the offending header")
            self.assertIn("chase_checking", message, "must list registered parsers")


class TestCrossPeriodFiltering(unittest.TestCase):
    """Statements routinely contain transactions outside the folder's period."""

    CROSS_PERIOD_CSV = """Details,Posting Date,Description,Amount,Type,Balance,Check or Slip #
DEBIT,08/26/2026,"IN PERIOD ONE",-10.00,ACH_DEBIT,0,,
DEBIT,09/04/2026,"NEXT MONTH ONE",-20.00,ACH_DEBIT,0,,
DEBIT,09/05/2026,"NEXT MONTH TWO",-30.00,ACH_DEBIT,0,,
DEBIT,07/31/2026,"PRIOR MONTH ONE",-40.00,ACH_DEBIT,0,,
"""

    def test_out_of_period_transactions_are_excluded(self) -> None:
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp) / "2026-08"
            root.mkdir()
            write(root, "Chase8798.csv", self.CROSS_PERIOD_CSV)

            result = discover(root)

            self.assertEqual(len(result.transactions), 1)
            self.assertEqual(result.transactions[0].description, "IN PERIOD ONE")

    def test_filtered_count_is_reported_not_silent(self) -> None:
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp) / "2026-08"
            root.mkdir()
            write(root, "Chase8798.csv", self.CROSS_PERIOD_CSV)

            result = discover(root)

            self.assertEqual(result.filtered_out, 3)

    def test_source_count_reflects_kept_transactions(self) -> None:
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp) / "2026-08"
            root.mkdir()
            write(root, "Chase8798.csv", self.CROSS_PERIOD_CSV)

            result = discover(root)

            self.assertEqual(result.sources[0].count, 1)
            self.assertEqual(result.sources[0].filtered_out, 3)


class TestDataRoot(unittest.TestCase):
    """CSV files carry account numbers, balances and merchant names.

    They live outside the repo so that no .gitignore mistake, forced add, or
    backup tool sweep can put them under version control.
    """

    def test_default_root_is_outside_the_repo(self) -> None:
        with mock.patch.dict(os.environ, {}, clear=True):
            root = data_root()

        self.assertEqual(root, Path.home() / "finance-data")
        repo = Path(__file__).resolve().parents[2]
        self.assertNotIn(repo, root.resolve().parents)
        self.assertNotEqual(root.resolve(), repo)

    def test_env_var_overrides_default(self) -> None:
        with mock.patch.dict(os.environ, {DEFAULT_DATA_ROOT_ENV: "/tmp/elsewhere"}, clear=True):
            self.assertEqual(data_root(), Path("/tmp/elsewhere"))

    def test_explicit_override_beats_env_var(self) -> None:
        with mock.patch.dict(os.environ, {DEFAULT_DATA_ROOT_ENV: "/tmp/from-env"}, clear=True):
            self.assertEqual(data_root("/tmp/from-cli"), Path("/tmp/from-cli"))

    def test_period_dir_joins_root_and_period(self) -> None:
        self.assertEqual(
            period_dir("2026-08", root="/tmp/data"),
            Path("/tmp/data") / "2026-08",
        )

    def test_period_dir_validates_the_period(self) -> None:
        with self.assertRaises(DiscoveryError):
            period_dir("august", root="/tmp/data")


if __name__ == "__main__":
    unittest.main()
