"""Offline expense importer: bank CSV -> classified transactions -> review output.

Phase A is read-only by design: it never writes to the database.
"""

from .classify import Rule, RuleError, classify, load_rules
from .models import Action, Classified, Txn
from .parsers import parser_for
from .report import Categories, aggregate, print_summary, write_batch_preview, write_review

__all__ = [
    "Action", "Categories", "Classified", "Rule", "RuleError", "Txn",
    "aggregate", "classify", "load_rules", "parser_for",
    "print_summary", "write_batch_preview", "write_review",
]
