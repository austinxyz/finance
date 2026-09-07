"""Tests for the expected-sources config.

This config is what gate 3 checks against. A credit-card statement that never
got exported leaves no trace in any other CSV — the funding gate cannot see it,
and the monthly total is silently short. Declaring the expected sources up front
is the only mechanism that catches it, so a malformed config must fail loudly.

Run: python -m unittest discover -s tools/expense-import -t tools/expense-import -p "test_sources.py"
"""

from __future__ import annotations

import tempfile
import unittest
from pathlib import Path

from importer.sources import SourceConfigError, load_sources


def sources_from(toml_text: str):
    with tempfile.NamedTemporaryFile("w", suffix=".toml", delete=False, encoding="utf-8") as fh:
        fh.write(toml_text)
        path = Path(fh.name)
    try:
        return load_sources(path)
    finally:
        path.unlink(missing_ok=True)


class TestLoading(unittest.TestCase):
    def test_loads_parser_backed_source(self) -> None:
        sources = sources_from("""
[[source]]
id = "chase_checking"
parser = "chase_checking"
label = "Chase 支票"
""")
        self.assertEqual(len(sources), 1)
        self.assertEqual(sources[0].id, "chase_checking")
        self.assertFalse(sources[0].manual)

    def test_loads_manual_source_without_parser(self) -> None:
        """A source with no CSV export is declared manual, not omitted."""
        sources = sources_from("""
[[source]]
id = "robinhood_card"
manual = true
label = "Robinhood 信用卡"
note = "只有 PDF 账单"
""")
        self.assertTrue(sources[0].manual)
        self.assertIsNone(sources[0].parser)

    def test_expected_parser_ids_excludes_manual(self) -> None:
        sources = sources_from("""
[[source]]
id = "chase_checking"
parser = "chase_checking"

[[source]]
id = "robinhood_card"
manual = true
""")
        self.assertEqual({s.id for s in sources if not s.manual}, {"chase_checking"})

    def test_loads_pending_source_whose_parser_is_not_built_yet(self) -> None:
        """The list describes a complete month, not what is implemented today.

        Without this state, declaring a not-yet-built source is indistinguishable
        from a typo, so the list could only ever describe current coverage — which
        makes gate 3 vacuous during the very period it is most needed.
        """
        sources = sources_from("""
[[source]]
id = "paypal"
parser = "paypal"
pending = true
""")
        self.assertTrue(sources[0].pending)
        self.assertFalse(sources[0].manual)


class TestValidation(unittest.TestCase):
    """Malformed config must fail at load time, never at write time."""

    def test_missing_id_is_rejected(self) -> None:
        with self.assertRaises(SourceConfigError):
            sources_from('[[source]]\nparser = "chase_checking"\n')

    def test_unregistered_parser_is_rejected(self) -> None:
        """Catches typos — the reason pending must be explicit rather than inferred."""
        with self.assertRaises(SourceConfigError) as ctx:
            sources_from('[[source]]\nid = "x"\nparser = "no_such_parser"\n')
        self.assertIn("no_such_parser", str(ctx.exception))
        self.assertIn("pending", str(ctx.exception), "must point at the pending escape")

    def test_pending_source_must_still_name_a_parser(self) -> None:
        """The parser id is the contract the future implementation must satisfy."""
        with self.assertRaises(SourceConfigError):
            sources_from('[[source]]\nid = "x"\npending = true\n')

    def test_manual_and_pending_are_mutually_exclusive(self) -> None:
        with self.assertRaises(SourceConfigError):
            sources_from('[[source]]\nid = "x"\nparser = "chase_card"\nmanual = true\npending = true\n')

    def test_pending_flag_is_rejected_once_the_parser_exists(self) -> None:
        """Stale pending flags would keep a working source out of the gate."""
        with self.assertRaises(SourceConfigError) as ctx:
            sources_from('[[source]]\nid = "chase_checking"\nparser = "chase_checking"\npending = true\n')
        self.assertIn("pending", str(ctx.exception))

    def test_non_manual_source_needs_a_parser(self) -> None:
        with self.assertRaises(SourceConfigError) as ctx:
            sources_from('[[source]]\nid = "x"\n')
        self.assertIn("parser", str(ctx.exception))

    def test_manual_source_must_not_declare_a_parser(self) -> None:
        """Declaring both hides whether the source is actually covered."""
        with self.assertRaises(SourceConfigError):
            sources_from('[[source]]\nid = "x"\nparser = "chase_checking"\nmanual = true\n')

    def test_duplicate_ids_are_rejected(self) -> None:
        with self.assertRaises(SourceConfigError) as ctx:
            sources_from("""
[[source]]
id = "chase_checking"
parser = "chase_checking"

[[source]]
id = "chase_checking"
parser = "chase_checking"
""")
        self.assertIn("chase_checking", str(ctx.exception))

    def test_empty_config_is_rejected(self) -> None:
        """An empty list would make gate 3 vacuously pass."""
        with self.assertRaises(SourceConfigError):
            sources_from("# nothing here\n")


class TestShippedConfig(unittest.TestCase):
    def test_repo_sources_toml_loads(self) -> None:
        sources = load_sources(Path(__file__).parent / "sources.toml")
        self.assertEqual(len(sources), 6, "all six institutions must be declared")

    def test_every_institution_is_covered_or_flagged(self) -> None:
        """No silent blanks: each source has a parser or is explicitly manual."""
        for source in load_sources(Path(__file__).parent / "sources.toml"):
            with self.subTest(source=source.id):
                self.assertTrue(
                    source.manual or source.parser,
                    f"{source.id} is neither parser-backed nor flagged manual",
                )


if __name__ == "__main__":
    unittest.main()
