"""Expected-sources config — the input to gate 3.

Gates 1 and 2 catch what the data reveals: an unmatched merchant, a PayPal
top-up with no matching statement. Neither can see a credit-card statement that
was never exported — it leaves no trace anywhere, and the month's total is
simply short. Declaring up front which institutions a complete month contains is
the only way to notice.

A source is one of three explicit states — never a silent blank:

- **parser-backed** — its CSV is parsed by a registered parser
- **``manual = true``** — no CSV export exists; the user enters it by hand
- **``pending = true``** — declared, but its parser is not built yet

``pending`` exists so the list can describe a *complete* month rather than
current coverage. Without it, declaring a not-yet-built source is
indistinguishable from a typo, so the list could only ever name what is already
implemented — which makes gate 3 vacuous during exactly the period it matters
most. Each flag is checked against the registry, so neither a typo nor a stale
``pending`` can hide.
"""

from __future__ import annotations

import tomllib
from dataclasses import dataclass
from pathlib import Path

from .parsers.base import registered_sources


class SourceConfigError(ValueError):
    """Raised when sources.toml is malformed — fail loudly, never guess."""


@dataclass(frozen=True)
class Source:
    id: str
    parser: str | None = None
    manual: bool = False
    pending: bool = False
    label: str = ""
    note: str = ""

    @property
    def is_expected_in_directory(self) -> bool:
        """Whether gate 3 should require this source's CSV to be present."""
        return not self.manual and not self.pending


def load_sources(path: Path) -> list[Source]:
    with path.open("rb") as fh:
        data = tomllib.load(fh)

    known_parsers = registered_sources()
    seen: set[str] = set()
    sources: list[Source] = []

    for index, entry in enumerate(data.get("source", []), start=1):
        where = f"source #{index} ({entry.get('id', 'unnamed')})"

        source_id = entry.get("id")
        if not source_id:
            raise SourceConfigError(f"{where}: 缺少 'id'")
        if source_id in seen:
            raise SourceConfigError(f"{where}: id {source_id!r} 重复声明")
        seen.add(source_id)

        manual = bool(entry.get("manual", False))
        pending = bool(entry.get("pending", False))
        parser = entry.get("parser")

        if manual and pending:
            raise SourceConfigError(
                f"{where}: 'manual' 与 'pending' 互斥 —— "
                f"manual 表示永远没有 CSV，pending 表示解析器还没写"
            )
        if manual and parser:
            raise SourceConfigError(
                f"{where}: manual 来源不得声明 'parser' —— "
                f"同时声明会掩盖该来源究竟是否已被覆盖"
            )
        if not manual and not parser:
            raise SourceConfigError(
                f"{where}: 非 manual 来源必须声明 'parser'（或标记 manual = true）。"
                f"pending 来源同样要写 'parser' —— 那个 id 就是将来实现要满足的契约"
            )

        if parser and parser not in known_parsers and not pending:
            raise SourceConfigError(
                f"{where}: parser {parser!r} 未注册；已注册: {', '.join(sorted(known_parsers))}。"
                f"若该解析器尚未实现，显式标记 pending = true"
            )
        if pending and parser in known_parsers:
            raise SourceConfigError(
                f"{where}: parser {parser!r} 已注册，请移除 'pending' 标记 —— "
                f"过期的 pending 会把一个可用来源挡在闸门之外"
            )

        sources.append(
            Source(
                id=source_id,
                parser=parser,
                manual=manual,
                pending=pending,
                label=entry.get("label", ""),
                note=entry.get("note", ""),
            )
        )

    if not sources:
        raise SourceConfigError(
            f"{path} 未声明任何来源 —— 空清单会让来源缺失闸门形同虚设"
        )
    return sources
