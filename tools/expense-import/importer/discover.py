"""Directory-driven source discovery.

One directory per accounting period: ``~/finance-data/2026-08/``. Drop each
institution's CSV in and the period is read off the directory name — no file
list, no ``--period`` flag.

Statements routinely spill past the period they were exported for (a file pulled
on Sep 6 carries early-September rows). Those transactions are attributed by
their own date, filtered out here, and **counted** — a silently dropped
transaction looks identical to one that was never exported.
"""

from __future__ import annotations

import os
import re
from dataclasses import dataclass, field
from pathlib import Path

from .models import Txn
from .parsers import parser_for

#: Directory names are the accounting period itself.
_PERIOD_RE = re.compile(r"^(\d{4})-(\d{2})$")

#: Environment variable overriding where the statement directories live.
DEFAULT_DATA_ROOT_ENV = "FINANCE_DATA_ROOT"

#: Default location — deliberately outside the repository. Statements carry
#: account numbers, balances and merchant names; keeping them out of the working
#: tree means no .gitignore mistake or forced add can commit them.
DEFAULT_DATA_ROOT = Path.home() / "finance-data"


class DiscoveryError(Exception):
    """Raised when a directory cannot be turned into a clean set of transactions."""


@dataclass(frozen=True)
class SourceResult:
    """One CSV file's contribution to the period."""

    source: str          # parser id, e.g. "chase_checking"
    path: Path
    count: int           # transactions kept (in period)
    filtered_out: int    # transactions dropped (out of period)


@dataclass(frozen=True)
class DiscoveryResult:
    period: str
    sources: list[SourceResult] = field(default_factory=list)
    transactions: list[Txn] = field(default_factory=list)
    filtered_out: int = 0

    @property
    def source_ids(self) -> set[str]:
        """Parser ids present in this directory — what the source gate checks against."""
        return {s.source for s in self.sources}


def data_root(override: str | Path | None = None) -> Path:
    """Where the per-period statement directories live.

    Precedence: explicit override (CLI) → ``FINANCE_DATA_ROOT`` → the default
    outside the repo.
    """
    if override is not None:
        return Path(override)
    from_env = os.environ.get(DEFAULT_DATA_ROOT_ENV)
    if from_env:
        return Path(from_env)
    return DEFAULT_DATA_ROOT


def period_dir(period: str, root: str | Path | None = None) -> Path:
    """Directory holding ``period``'s statements, e.g. ``~/finance-data/2026-08``."""
    return data_root(root) / period_from_dirname(period)


def period_from_dirname(name: str) -> str:
    """Validate and return a ``YYYY-MM`` period read from a directory name."""
    match = _PERIOD_RE.match(name)
    if not match:
        raise DiscoveryError(
            f"目录名 {name!r} 不是有效期间，期望 YYYY-MM 格式（如 2026-08）"
        )
    month = int(match.group(2))
    if not 1 <= month <= 12:
        raise DiscoveryError(
            f"目录名 {name!r} 的月份 {month:02d} 无效，期望 YYYY-MM 格式，月份为 01-12"
        )
    return name


def discover(directory: Path) -> DiscoveryResult:
    """Parse every CSV in ``directory``, keeping only transactions in its period.

    Raises DiscoveryError — never creates the directory, never guesses a parser.
    """
    directory = Path(directory)
    if not directory.is_dir():
        raise DiscoveryError(
            f"目录不存在: {directory}\n"
            f"请先创建该目录并放入当月各家 CSV（本工具不会自动创建）"
        )

    period = period_from_dirname(directory.name)

    csv_paths = sorted(p for p in directory.iterdir() if p.suffix.lower() == ".csv")
    if not csv_paths:
        raise DiscoveryError(f"目录 {directory} 下没有 CSV 文件")

    sources: list[SourceResult] = []
    kept: list[Txn] = []
    for path in csv_paths:
        try:
            parser = parser_for(path)
        except ValueError as exc:
            # parser_for's message already names the file, its header, and the
            # registered parsers — exactly the diagnostics needed to add one.
            raise DiscoveryError(str(exc)) from exc

        parsed = parser.parse(path)
        in_period = [t for t in parsed if t.period == period]
        sources.append(
            SourceResult(
                source=parser.source,
                path=path,
                count=len(in_period),
                filtered_out=len(parsed) - len(in_period),
            )
        )
        kept.extend(in_period)

    return DiscoveryResult(
        period=period,
        sources=sources,
        transactions=kept,
        filtered_out=sum(s.filtered_out for s in sources),
    )
