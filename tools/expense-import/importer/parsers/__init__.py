"""Institution parsers. Importing a module here registers it."""

from . import boa_checking  # noqa: F401  (registers BoaCheckingParser)
from . import chase_card  # noqa: F401  (registers ChaseCardParser)
from . import chase_checking  # noqa: F401  (registers ChaseCheckingParser)
from . import paypal  # noqa: F401  (registers PayPalParser)
from . import robinhood_card  # noqa: F401  (registers RobinhoodCardParser)
from . import venmo  # noqa: F401  (registers VenmoParser)
from .base import Parser, parser_for, register, to_decimal

__all__ = ["Parser", "parser_for", "register", "to_decimal"]
