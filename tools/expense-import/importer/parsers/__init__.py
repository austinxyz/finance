"""Institution parsers. Importing a module here registers it."""

from . import chase_checking  # noqa: F401  (registers ChaseCheckingParser)
from .base import Parser, parser_for, register, to_decimal

__all__ = ["Parser", "parser_for", "register", "to_decimal"]
