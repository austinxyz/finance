"""Parser registry: pick an institution parser by sniffing the CSV header.

Adding an institution means writing a module with a ``Parser`` subclass and
importing it in ``__init__``; nothing else in the pipeline changes.

Not every export puts its header on line 1. BOA opens with a five-line balance
summary; Venmo with two lines of account name. Parsers declare how far in to
look via ``header_search_rows``, and the located row is used both for matching
and for reading the data.
"""

from __future__ import annotations

import csv
from abc import ABC, abstractmethod
from decimal import Decimal, InvalidOperation
from pathlib import Path

from ..models import Txn


class Parser(ABC):
    """Base class for institution CSV parsers."""

    #: Parser id, also written to the review file.
    source: str = ""

    #: Column names that must all be present in the header for this parser to claim a file.
    header_signature: tuple[str, ...] = ()

    #: How many leading rows to scan for the header. 1 means "line 1 only".
    #: Raise it for exports that begin with a summary or title block.
    header_search_rows: int = 1

    @classmethod
    def matches(cls, header: list[str]) -> bool:
        normalized = {h.strip().lower() for h in header}
        return all(col.lower() in normalized for col in cls.header_signature)

    @classmethod
    def locate_header(cls, path: Path) -> int | None:
        """Index of the row holding this parser's header, or None if absent."""
        with path.open(newline="", encoding="utf-8-sig") as fh:
            for index, row in enumerate(csv.reader(fh)):
                if index >= cls.header_search_rows:
                    return None
                if cls.matches(row):
                    return index
        return None

    @abstractmethod
    def parse_row(self, row: dict[str, str], account: str) -> Txn | None:
        """Convert one CSV row into a Txn, or None to skip it."""

    def parse(self, path: Path) -> list[Txn]:
        account = self.account_label(path)
        header_row = self.locate_header(path)
        if header_row is None:
            raise ValueError(f"{path.name}: 找不到 {self.source} 的表头")
        out: list[Txn] = []
        with path.open(newline="", encoding="utf-8-sig") as fh:
            for _ in range(header_row):
                fh.readline()
            reader = csv.DictReader(fh, restkey="_extra")
            for row in reader:
                clean = {
                    (k.strip() if k else k): (v.strip() if isinstance(v, str) else v)
                    for k, v in row.items()
                }
                txn = self.parse_row(clean, account)
                if txn is not None:
                    out.append(txn)
        return out

    @staticmethod
    def account_label(path: Path) -> str:
        """Derive a short account label from the filename, e.g. Chase8798."""
        return path.stem.split("_")[0]


_REGISTRY: list[type[Parser]] = []


def register(cls: type[Parser]) -> type[Parser]:
    _REGISTRY.append(cls)
    return cls


def registered_sources() -> set[str]:
    """Parser ids currently registered — used to validate sources.toml."""
    return {cls.source for cls in _REGISTRY}


def parser_for(path: Path) -> Parser:
    """Return the parser that claims this file, or raise with a clear message."""
    for cls in _REGISTRY:
        if cls.locate_header(path) is not None:
            return cls()

    with path.open(newline="", encoding="utf-8-sig") as fh:
        header = next(csv.reader(fh), [])
    known = ", ".join(c.source for c in _REGISTRY) or "(none registered)"
    raise ValueError(
        f"No parser matches {path.name}.\n"
        f"  header: {header}\n"
        f"  known parsers: {known}\n"
        f"Add one under importer/parsers/ and register it."
    )


def to_decimal(raw: str) -> Decimal:
    """Parse a money field, tolerating $ signs, commas and parenthesised negatives."""
    if raw is None:
        raise ValueError("missing amount")
    text = str(raw).strip().replace("$", "").replace(",", "")
    if not text:
        raise ValueError("empty amount")
    negative = text.startswith("(") and text.endswith(")")
    if negative:
        text = text[1:-1]
    try:
        value = Decimal(text)
    except InvalidOperation as exc:
        raise ValueError(f"unparseable amount: {raw!r}") from exc
    return -value if negative else value
