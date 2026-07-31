#!/usr/bin/env python3
"""Scoped semantic guards for the FE-04E governance contract.

This helper intentionally reads only the frozen FE-04E governance surfaces.
It complements the shell contract checker with contradiction detection and
case-insensitive authorization-state checks for Markdown and YAML-shaped text.
"""

from __future__ import annotations

import argparse
import hashlib
import importlib.util
import inspect
import json
import os
import re
import sys
import unicodedata
from dataclasses import dataclass
from enum import Enum
from pathlib import Path
from typing import Callable, Dict, Iterable, List, Mapping, MutableMapping, Optional, Sequence, Tuple


SEMANTIC = "docs/design/FE04_SEMANTIC_CONTRACT_V2.md"
INTERACTION = "docs/INTERACTION_CONTRACT_V3.md"
STATE = "docs/PROJECT_CURRENT_STATE.md"
MATRIX = "docs/DELIVERY_PROGRESS_MATRIX.md"
ACTIVE = "docs/ACTIVE_MAINLINE_STATUS.yml"
NEXT_TASK = "docs/CODEX_NEXT_TASK.yml"
CHANGE_LOG = "docs/CONTRACT_CHANGE_LOG.md"
DELIVERY = "docs/PROJECT_DELIVERY_CONTRACT.md"
CAPABILITY = "docs/V1_CAPABILITY_MATRIX.md"

REQUIRED_FILES = (
    SEMANTIC,
    INTERACTION,
    STATE,
    MATRIX,
    ACTIVE,
    NEXT_TASK,
    CHANGE_LOG,
    DELIVERY,
    CAPABILITY,
)

CONTRADICTION_GUARDS = (
    "private Recheck contradiction guard",
    "private Push contradiction guard",
    "private execution contradiction guard",
    "UserPosition/private risk contradiction guard",
    "OPPORTUNITY weakening exception guard",
)

AUTHORIZATION_GUARDS = (
    "Telegram semantic authorization guard",
    "system notification semantic authorization guard",
    "external notification semantic authorization guard",
    "automatic notification semantic authorization guard",
    "trading semantic authorization guard",
    "trading capability movement guard",
    "PushRecheck trade authorization guard",
    "next-task capability authorization guard",
)

CROSS_FILE_GUARDS = (
    "OPPORTUNITY cross-file consistency guard",
    "notification cross-file consistency guard",
    "trading cross-file consistency guard",
)

SCOPE_GUARDS = ("required governed scope discovery guard",)

SEMANTIC_GUARD_NAMES = (
    CONTRADICTION_GUARDS + AUTHORIZATION_GUARDS + CROSS_FILE_GUARDS + SCOPE_GUARDS
)
# Backward-compatible name used by the existing runner output loop.
STATIC_GUARD_NAMES = SEMANTIC_GUARD_NAMES


@dataclass(frozen=True)
class Scope:
    document: str
    name: str
    text: str


@dataclass(frozen=True)
class MarkdownSection:
    level: int
    title: str
    normalized_title: str
    heading_start: int
    body_start: int
    end: int
    body: str


@dataclass(frozen=True)
class ScopeRequirement:
    name: str
    document: str
    aliases: Tuple[str, ...]
    body_pattern: Optional[re.Pattern[str]] = None


PRODUCTION_SCOPE_ALIASES: Mapping[str, Tuple[str, ...]] = {
    # Telegram is the capability identity. Heading qualifiers are optional and
    # must never be required for the production scope to be discoverable.
    "telegram": ("telegram",),
    "system_notification": ("old new contract difference", "contract difference"),
    "fe04e_privacy_state": ("fe 04e privacy state", "fe 04e privacy"),
}


class StatusClass(str, Enum):
    SAFE = "SAFE"
    DANGEROUS = "DANGEROUS"
    UNKNOWN = "UNKNOWN"


@dataclass(frozen=True)
class StatusDeclaration:
    capability: str
    classification: StatusClass
    value: str
    document: str
    scope: str
    excerpt: str
    line: int = 0
    original_value: str = ""
    normalized_value: str = ""
    matched_token: str = ""
    key_path: str = ""
    source_kind: str = "PROSE"
    token_order: int = 0
    clause: str = ""
    clause_start: int = 0
    clause_end: int = 0
    clause_order: int = 0


@dataclass(frozen=True)
class MatchedStatusToken:
    classification: StatusClass
    original: str
    normalized: str
    start: int
    end: int
    clause: str = ""
    clause_start: int = 0
    clause_end: int = 0
    clause_order: int = 0


@dataclass(frozen=True)
class LogicalClause:
    text: str
    start: int
    end: int
    order: int


@dataclass(frozen=True)
class CoverageRecord:
    id: str
    category: str
    layer: str
    entrypoint: str
    target_guard: str
    expected_result: str
    assertion_objective: str
    fixture_fingerprint: str
    source: str

    @property
    def qualifying_key(self) -> str:
        return ":".join(
            (
                self.layer,
                self.entrypoint,
                self.target_guard,
                self.assertion_objective,
                self.fixture_fingerprint,
            )
        )


@dataclass(frozen=True)
class Violation:
    guard: str
    document: str
    scope: str
    category: str
    excerpt: str
    expected: str


@dataclass(frozen=True)
class Probe:
    name: str
    document: str
    mutation: Callable[[MutableMapping[str, str]], None]
    expected_guard: str
    expected_category: str = ""
    expected_tokens: Tuple[str, ...] = ()
    validator: Optional[Callable[[Mapping[str, str]], Optional[str]]] = None
    coverage_layer: str = "L2_HELPER_INTEGRATION"
    coverage_entrypoint: str = "semantic_evaluate"
    coverage_objective: str = ""


def coverage_case(
    *,
    layer: str,
    entrypoint: str,
    target_guard: str,
    expected_result: str,
    assertion_objective: str,
    fixture: str,
) -> Callable[[Callable[..., object]], Callable[..., object]]:
    """Attach auditable coverage metadata to a unittest method."""

    def decorate(function: Callable[..., object]) -> Callable[..., object]:
        setattr(
            function,
            "__fe04e_coverage__",
            {
                "layer": layer,
                "entrypoint": entrypoint,
                "target_guard": target_guard,
                "expected_result": expected_result,
                "assertion_objective": assertion_objective,
                "fixture": fixture,
            },
        )
        return function

    return decorate


def normalized(text: str) -> str:
    value = unicodedata.normalize("NFKC", text)
    value = value.replace("`", " ").replace("—", " - ").replace("–", " - ")
    value = re.sub(r"[_/\\-]+", " ", value)
    value = re.sub(r"[*#]+", " ", value)
    return re.sub(r"\s+", " ", value).strip().lower()


HEADING_RE = re.compile(r"(?m)^(#{1,6})[ \t]+(.+?)[ \t]*#*[ \t]*$")
HEADING_NUMBER_PREFIX = re.compile(
    r"^(?:(?:part|step)[ \t]+[a-z0-9一二三四五六七八九十]+|"
    r"\d+(?:\.\d+)*)(?:[ \t]*(?:[.、:：)）\-—–]|[ \t]))*[ \t]*",
    re.I,
)


def normalize_heading(title: str) -> str:
    """Normalize harmless heading decoration without erasing capability words."""

    value = unicodedata.normalize("NFKC", title).strip().strip("#").strip()
    previous = None
    while value != previous:
        previous = value
        value = HEADING_NUMBER_PREFIX.sub("", value, count=1).strip()
    value = re.sub(r"[()\[\]{}<>（）【】《》:：,，;；/\\|+]+", " ", value)
    value = value.replace("—", " ").replace("–", " ").replace("-", " ")
    return re.sub(r"\s+", " ", value).strip().lower()


def parse_markdown_sections(text: str) -> List[MarkdownSection]:
    """Parse heading-delimited sections using Markdown hierarchy (levels 1-6)."""

    matches = list(HEADING_RE.finditer(text))
    sections: List[MarkdownSection] = []
    for index, match in enumerate(matches):
        level = len(match.group(1))
        end = len(text)
        for candidate in matches[index + 1 :]:
            if len(candidate.group(1)) <= level:
                end = candidate.start()
                break
        body_start = match.end()
        sections.append(
            MarkdownSection(
                level=level,
                title=match.group(2).strip(),
                normalized_title=normalize_heading(match.group(2)),
                heading_start=match.start(),
                body_start=body_start,
                end=end,
                body=text[body_start:end].strip(),
            )
        )
    return sections


def heading_matches_alias(title: str, alias: str) -> bool:
    title_tokens = set(normalize_heading(title).split())
    alias_tokens = set(normalize_heading(alias).split())
    return bool(alias_tokens) and alias_tokens.issubset(title_tokens)


def find_markdown_sections(text: str, aliases: Sequence[str]) -> List[MarkdownSection]:
    """Return every section whose normalized heading matches any accepted alias."""

    return [
        section
        for section in parse_markdown_sections(text)
        if any(heading_matches_alias(section.title, alias) for alias in aliases)
    ]


def production_scope_sections(text: str, scope_name: str) -> List[MarkdownSection]:
    """Discover a governed Markdown scope using production aliases only."""

    aliases = PRODUCTION_SCOPE_ALIASES.get(scope_name)
    if aliases is None:
        raise KeyError(f"unknown production scope: {scope_name}")
    return find_markdown_sections(text, aliases)


def excerpt(text: str, limit: int = 220) -> str:
    value = re.sub(r"\s+", " ", text).strip()
    return value if len(value) <= limit else value[: limit - 3] + "..."


def extract_between(text: str, start: str, end: str) -> str:
    """Compatibility helper for marker-delimited, non-heading governance facts."""

    start_match = re.search(rf"(?m)^{re.escape(start)}\s*$", text)
    if not start_match:
        return ""
    end_match = re.search(rf"(?m)^{re.escape(end)}\s*$", text[start_match.end() :])
    if not end_match:
        return ""
    return text[start_match.start() : start_match.end() + end_match.start()]


def extract_marker_window(text: str, start: str, end: str) -> str:
    start_index = text.find(start)
    if start_index < 0:
        return ""
    end_index = text.find(end, start_index + len(start))
    if end_index < 0:
        return ""
    return text[start_index:end_index]


def logical_units(text: str) -> List[str]:
    units: List[str] = []
    current: List[str] = []

    def flush() -> None:
        if current:
            units.append(" ".join(part.strip() for part in current if part.strip()))
            current.clear()

    for raw_line in text.splitlines():
        line = raw_line.strip()
        if not line:
            flush()
            continue
        if re.match(r"^#{1,6}\s", line):
            flush()
            continue
        if line.startswith("|"):
            flush()
            units.append(line)
            continue
        if re.match(r"^(?:[-*+]|\d+\.)\s+", line):
            flush()
            current.append(line)
            continue
        current.append(line)
    flush()
    return units


def statement_units(text: str) -> List[str]:
    """Split prose declarations without separating Markdown table labels/values."""

    statements: List[str] = []
    for unit in logical_units(text):
        if unit.lstrip().startswith("|"):
            statements.append(unit)
            continue
        for part in re.split(r"(?<=[.!?。！？;；])\s+|[;；](?=\s*\w)", unit):
            value = part.strip()
            if value:
                statements.append(value)
    return statements


def markdown_alias_scopes(
    documents: Mapping[str, str],
    document: str,
    name: str,
    aliases: Sequence[str],
) -> List[Scope]:
    return [
        Scope(document, f"{name} [{section.title}]", section.body)
        for section in find_markdown_sections(documents[document], aliases)
    ]


POSITION_RISK_BOUNDARY = re.compile(
    r"(?im)^(?:#{1,6}\s*)?(?:(?:[-*]\s*)?Owner[\s-]*scoped\s+[`']?POSITION_RISK[`']?|"
    r"(?:[-*]\s*)?[`']?POSITION_RISK[`']?\s+is\s+[`']?OWNER_SCOPED)",
)


def public_opportunity_segment(text: str) -> str:
    """Keep only the public OPPORTUNITY domain inside a mixed Message section."""

    start = re.search(
        r"(?im)^(?:#{1,6}\s*)?(?:Push Detail remains|Authenticated shared.*OPPORTUNITY|"
        r"Public [`']?OPPORTUNITY)",
        text,
    )
    if not start:
        return text
    end = POSITION_RISK_BOUNDARY.search(text[start.start() :])
    end_index = len(text) if not end else start.start() + end.start()
    return text[start.start() : end_index].strip()


def position_risk_segment(text: str) -> str:
    """Return the owner-scoped POSITION_RISK domain from a mixed Message section."""

    start = POSITION_RISK_BOUNDARY.search(text)
    return "" if not start else text[start.start() :].strip()


def telegram_domain_scopes(
    documents: Mapping[str, str],
) -> Tuple[List[Scope], List[Scope]]:
    """Parse Telegram sections once and expose their public/private domains."""

    public_scopes: List[Scope] = []
    private_scopes: List[Scope] = []
    for section in production_scope_sections(documents[SEMANTIC], "telegram"):
        name = f"Message/Telegram [{section.title}]"
        public_scopes.append(
            Scope(
                SEMANTIC,
                f"{name} public OPPORTUNITY",
                public_opportunity_segment(section.body),
            )
        )
        private_text = position_risk_segment(section.body)
        if private_text:
            private_scopes.append(
                Scope(SEMANTIC, f"{name} owner-scoped POSITION_RISK", private_text)
            )
    return public_scopes, private_scopes


def opportunity_scopes(documents: Mapping[str, str]) -> List[Scope]:
    scopes, _ = telegram_domain_scopes(documents)
    scopes.extend(
        markdown_alias_scopes(documents, INTERACTION, "Overview Dashboard public OPPORTUNITY", ("overview dashboard",))
    )
    scopes.extend(
        markdown_alias_scopes(documents, INTERACTION, "Mobile Push Detail public OPPORTUNITY", ("mobile push detail", "push detail"))
    )
    scopes.extend(
        markdown_alias_scopes(documents, INTERACTION, "Mobile Message Center public OPPORTUNITY", ("mobile message center", "message center"))
    )
    scopes.extend(
        markdown_alias_scopes(
            documents,
            STATE,
            "FE-04E public OPPORTUNITY boundary",
            PRODUCTION_SCOPE_ALIASES["fe04e_privacy_state"],
        )
    )
    scopes.extend(
        markdown_alias_scopes(
            documents,
            CHANGE_LOG,
            "FE-04E merged-main public OPPORTUNITY record",
            ("fe04e privacy state foundation effective merged main",),
        )
    )
    return scopes


PRIVATE_ENTITY_PATTERNS = {
    "private Recheck": re.compile(r"\b(?:private\s+)?(?:push\s*recheck|recheck)\b", re.I),
    "private Push": re.compile(
        r"\b(?:private|internal)[\s_-]+push\b|"
        r"\bpush[\s_-]+status\b|\bpush[\s_-]*id\b",
        re.I,
    ),
    "private execution": re.compile(
        r"\bexecution\b|\bexecution[\s_-]+status\b|\bprivate[\s_-]+execution\b|"
        r"\bexecution[\s_-]+(?:result|completion|completed|failed|pending)\b|"
        r"\b(?:completed|failed)\b.{0,100}\b(?:public|ready|error|visibility|lifecycle)\b|"
        r"\bexecuted\b|"
        r"\bcompletion[\s_-]+state\b",
        re.I,
    ),
    "UserPosition/private risk": re.compile(
        r"\buserposition\b|\bcurrent\s+user(?:'s)?\s+position\b|"
        r"\buser\s+scoped\s+context\b|\baccount\s+risk\b|"
        r"\bposition\s+risk\b|\bprivate\s+risk\b|\brisk\s+context\b|"
        r"\b(?:long|short|flat|no[\s-]*position)\b|"
        r"\bposition\s+direction\b|\bholding(?:\s+(?:state|direction))?\b|"
        r"\bcaller\s+(?:position|holding|exposure)\b|\buser\s+exposure\b|"
        r"多仓|空仓|无仓|持仓方向|用户持仓|持仓风险|持仓|仓位|多空方向|"
        r"风险上下文|账户风险|私有风险",
        re.I,
    ),
}

UNSAFE_RELATION = re.compile(
    r"\bmay\s+(?:be\s+)?(?:use|consult|supplement|influence|refine|participate)|"
    r"\bcan\s+(?:be\s+)?(?:use|consult|supplement|influence|refine|participate)|"
    r"\b(?:used|consulted|consumed)\s+as\b|\bauxiliary\b|\boptional\b|"
    r"\bfallback\b|\bwhen\s+available\b|\bmay\s+determine\b|"
    r"\b(?:influences?|determines?|refines?|supplements?|participates?|alters?|changes?|"
    r"affects?|gates?|produces?|makes?|keeps?|controls?)\b|"
    r"\bmaps?\b.{0,60}\bto\b|\bwaits?\s+for\b|\brel(?:y|ies)\s+on\b|"
    r"\bdepends?\s+on\b|\b(?:is|are)\s+dependent\s+on\b|"
    r"\brequir(?:e|es|ing)\b|\brequired\s+(?:for|before)\b|"
    r"\b(?:is|are)\s+required\s+(?:for|before)\b|"
    r"\binput\s+(?:to|for)\b|\bfor\s+accuracy\b|"
    r"\bused\s+(?:internally|in|to|as\s+fallback)\b|\bhidden\s+but\s+used\b|"
    r"\bbest\s+effort\b|改变|影响|决定|控制|参与|修正|用于|参与计算",
    re.I,
)

WEAKENING_RELATION = re.compile(
    r"\bunless\b|\bexcept\b|\bhowever\b|\bauxiliary\b|\boptional\b|"
    r"\bfallback\b|\bwhen\s+available\b|\bmay\s+consult\b|"
    r"\bmay\s+supplement\b|\bmay\s+be\s+used\b|\bcan\s+be\s+used\b|"
    r"\binternal\s+only\b|\bnot\s+(?:exposed|returned|serialized)\s+but\b|"
    r"\bhidden\s+but\b|\bfor\s+accuracy\b|\bbest\s+effort\b|"
    r"虽不展示但|隐藏但|不展示但",
    re.I,
)

SAFE_PROHIBITION = re.compile(
    r"\bmust\s+not\b|\bnever\b|\bcannot\b|\b(?:does|do|may|can)\s+not\b|"
    r"\bnot\s+(?:used|consulted|read|consumed|required|allowed|permitted|authorized)\b|"
    r"\b(?:forbidden|prohibited|excluded|neither)\b|"
    r"\bno\s+(?:private|internal|userposition|account\s+risk|position\s+risk)\b|"
    r"不得|不能|禁止|不应|不会",
    re.I,
)

PUBLIC_STATE_CONTEXT = re.compile(
    r"\bpublic\b|\bopportunity\b|\breadiness\b|\blifecycle\b|"
    r"\bevaluation\b|\bresult\b|\bvisibility\b|\bstate\b|\bstatus\b|"
    r"\bready\b|\bpartial\b|\berror\b|公共状态|公开状态|公共生命周期|"
    r"公开生命周期|公共就绪状态|公开就绪状态|公开评估|公共评估|公共结果|"
    r"公开结果|公共输出|公开输出",
    re.I,
)


def contradiction_units(scopes: Iterable[Scope]) -> Iterable[Tuple[Scope, str]]:
    for scope in scopes:
        if not scope.text:
            continue
        for unit in statement_units(scope.text):
            yield scope, unit


def is_unsafe_private_statement(unit: str, entity_pattern: re.Pattern[str]) -> bool:
    if not entity_pattern.search(unit):
        return False
    if not UNSAFE_RELATION.search(unit):
        return False
    if WEAKENING_RELATION.search(unit):
        return True
    return not SAFE_PROHIBITION.search(unit)


def private_semantic_category(base_category: str, unit: str) -> str:
    normalized_unit = normalized(unit)
    if base_category == "private execution":
        if "fallback" in normalized_unit:
            return "PRIVATE_EXECUTION_FALLBACK"
        if re.search(r"\bpending\b|\bpartial\b", normalized_unit):
            return "PRIVATE_EXECUTION_PENDING_PARTIAL"
        if re.search(r"\b(?:fail|failed|failure)\b|\berror\b", normalized_unit):
            return "PRIVATE_EXECUTION_FAILURE_ERROR"
        if re.search(r"\bcompleted?\b|\bcompletion\b|\bready\b", normalized_unit):
            return "PRIVATE_EXECUTION_COMPLETION_GATE"
        return "PRIVATE_EXECUTION_DEPENDENCY"
    if base_category == "UserPosition/private risk":
        if re.search(r"risk|风险", normalized_unit):
            return "PRIVATE_RISK_PUBLIC_STATE_DEPENDENCY"
        return "POSITION_DIRECTION_PUBLIC_STATE_DEPENDENCY"
    if base_category == "private Recheck":
        return "PRIVATE_RECHECK_PUBLIC_STATE_DEPENDENCY"
    if base_category == "private Push":
        return "PRIVATE_PUSH_PUBLIC_STATE_DEPENDENCY"
    return base_category


def contradiction_violations(
    documents: Mapping[str, str],
    guard: str,
    category: str,
    entity_pattern: re.Pattern[str],
) -> List[Violation]:
    violations: List[Violation] = []
    scopes = opportunity_scopes(documents)
    for scope in scopes:
        if not scope.text:
            violations.append(
                Violation(
                    guard,
                    scope.document,
                    scope.name,
                    "missing governed scope",
                    scope.name,
                    "the scoped public OPPORTUNITY contract must remain readable",
                )
            )
            continue
        for _, unit in contradiction_units((scope,)):
            if is_unsafe_private_statement(unit, entity_pattern):
                violations.append(
                    Violation(
                        guard,
                        scope.document,
                        scope.name,
                        private_semantic_category(category, unit),
                        excerpt(unit),
                        "public OPPORTUNITY must use public inputs only",
                    )
                )
    return violations


def weakening_violations(documents: Mapping[str, str]) -> List[Violation]:
    guard = "OPPORTUNITY weakening exception guard"
    violations: List[Violation] = []
    combined_private = re.compile(
        "|".join(f"(?:{pattern.pattern})" for pattern in PRIVATE_ENTITY_PATTERNS.values())
        + r"|\bprivate\s+(?:state|data|payload|context)\b",
        re.I,
    )
    for scope, unit in contradiction_units(opportunity_scopes(documents)):
        if (
            combined_private.search(unit)
            and PUBLIC_STATE_CONTEXT.search(unit)
            and WEAKENING_RELATION.search(unit)
        ):
            violations.append(
                Violation(
                    guard,
                    scope.document,
                    scope.name,
                    "exception or weakening semantics",
                    excerpt(unit),
                    "no private-state exception, fallback, or auxiliary input is permitted",
                )
            )
    return violations


CAPABILITY_PATTERNS = {
    "telegram": re.compile(r"\btelegram\b", re.I),
    "system notification": re.compile(r"\bsystem[\s_-]*notifications?\b", re.I),
    "external notification": re.compile(
        r"\bexternal[\s_-]*(?:notifications?|send|delivery)\b|\bwebhook\b", re.I
    ),
    "automatic notification": re.compile(
        r"\bautomatic[\s_-]*notifications?\b|"
        r"\bautomatic[\s_-]*delivery\b|"
        r"\bauto[\s_-]*(?:notify|notification|send|delivery)\b",
        re.I,
    ),
    "trading": re.compile(
        r"\btrading\b|\btrade[\s_-]*(?:authorization|authorized|capability)\b|"
        r"\bauto[\s_-]*trade\b|\border[\s_-]*(?:placement|execution|authorization)\b|"
        r"\b(?:close|reverse)[\s_-]*authorization\b",
        re.I,
    ),
}

POSITIVE_STATUS = re.compile(
    r"\b(?:authorized|enabled|implemented|active|allowed|available|supported|"
    r"ready|true|yes|on|granted|permitted)\b",
    re.I,
)

SAFE_STATUS = re.compile(
    r"\bnot\s+(?:authorized|enabled|implemented|active|allowed|available|supported|ready)\b|"
    r"\bno\b.{0,220}\b(?:authorized|enabled|implemented|active|allowed|available|supported)\b|"
    r"\b(?:disabled|blocked|prohibited|forbidden)\b|"
    r"\brequires?\s+separate\s+(?:future\s+)?authorization\b|"
    r"\bfuture\s+(?:delivery\s+outlet|extension)\s+only\b|\bpending\s+implementation\b|"
    r"\bpermitted\s+future\b.{0,80}\bcategories?\b|"
    r"\bnot\s+connected\b|\bwaiting\s+sync\b|\bno\s+(?:delivery|send|trading)\b|"
    r"\bnot\s+started\b|\breadiness\s+gate\s+required\b|"
    r"\bmovement\s*(?::|=|\||-)\s*none\b|\bread[\s_-]*only\b|"
    r"\bmust\s+not\b|\bnever\b",
    re.I,
)

SAFE_STATUS_TOKEN = re.compile(
    r"(?<![A-Za-z0-9])(?:"
    r"no\b.{0,320}?\bis\s+allowed\b|"
    r"not[\s_-]+(?:authorized|enabled|implemented|active|allowed|available|supported|ready)|"
    r"(?:no|must[\s_-]+not|never)\b.{0,120}?\b"
    r"(?:authorized|enabled|implemented|active|allowed|available|supported|ready)|"
    r"disabled|blocked|prohibited|forbidden|"
    r"not[\s_-]+connected|waiting[\s_-]+sync|not[\s_-]+started|"
    r"pending[\s_-]+implementation|read[\s_-]*only|none|false|off|"
    r"extension|"
    r"permitted[\s_-]+future\b.{0,100}\bcategories?\b(?:\s+are\s+limited)?|"
    r"未授权|未启用|未实现|已禁用|禁用|阻塞|禁止|不允许"
    r")(?![A-Za-z0-9])",
    re.I,
)

DANGEROUS_STATUS_TOKEN = re.compile(
    r"(?<![A-Za-z0-9])(?:authorized|enabled|implemented|active|allowed|available|supported|"
    r"ready|connected|true|yes|granted|permitted|limited|expanded|partial|"
    r"已授权|已启用|已实现|启用|授权|激活|允许|可用|支持|就绪)(?![A-Za-z0-9])",
    re.I,
)

COMPOUND_STATUS_SEPARATOR = re.compile(
    r"\s*(?:[;,+/|\n、，；]|\b(?:and|but|however|yet|although|then|while|whereas)\b|"
    r"并且|但是|然而|不过|同时|随后|但)\s*",
    re.I,
)

LOGICAL_CLAUSE_BOUNDARY = re.compile(
    r"(?:[.;:\n。；]+|"
    r"\s*(?:,\s*)?\b(?:but|however|yet|although|then|while|whereas)\b\s*|"
    r"\s*(?:，\s*)?(?:但是|然而|不过|同时|随后|但)\s*)",
    re.I,
)

RESIDUE_WORD = re.compile(r"[A-Za-z0-9_]+|[\u3400-\u4dbf\u4e00-\u9fff]+")

ALLOWED_RESIDUE_WORDS = frozenset(
    {
        "a",
        "an",
        "the",
        "is",
        "are",
        "be",
        "remains",
        "remain",
        "currently",
        "explicitly",
        "still",
        "status",
        "state",
        "capability",
        "authorization",
        "implementation",
        "integration",
        "delivery",
        "send",
        "movement",
        "notification",
        "notifications",
        "system",
        "external",
        "automatic",
        "auto",
        "telegram",
        "trading",
        "trade",
        "order",
        "placement",
        "public",
        "private",
        "future",
        "extension",
        "only",
        "outlet",
        "pass",
        "boundary",
        "and",
        "or",
        "plus",
        "to",
        "of",
        "for",
        "this",
        "that",
        "it",
        "until",
        "verified",
        "source",
        "exists",
        "0",
        "no",
        "not",
        "must",
        "never",
        "状态",
        "能力",
        "授权",
        "实现",
        "集成",
        "通知",
        "推送",
        "发送",
        "交易",
        "未来",
        "扩展",
        "仅",
    }
)

ASSIGNMENT_CONNECTOR = re.compile(
    r"(?:\bstatus\b|\bauthori[sz]ation\b|\bcapability\b|\bstate\b|"
    r"\bimplementation\b|\bdelivery\b|\bsend\b)?"
    r"\s*(?::|=|\||—|-|\bis\b|\bare\b|\bremains?\b)\s*",
    re.I,
)


def logical_clauses(value: str) -> List[LogicalClause]:
    """Split authorization prose before classification while retaining offsets."""

    source = unicodedata.normalize("NFKC", value)
    clauses: List[LogicalClause] = []
    cursor = 0
    for boundary in LOGICAL_CLAUSE_BOUNDARY.finditer(source):
        raw = source[cursor : boundary.start()]
        leading = len(raw) - len(raw.lstrip())
        trailing = len(raw.rstrip())
        start = cursor + leading
        end = cursor + trailing
        if start < end:
            clauses.append(LogicalClause(source[start:end], start, end, len(clauses) + 1))
        cursor = boundary.end()
    raw = source[cursor:]
    leading = len(raw) - len(raw.lstrip())
    trailing = len(raw.rstrip())
    start = cursor + leading
    end = cursor + trailing
    if start < end:
        clauses.append(LogicalClause(source[start:end], start, end, len(clauses) + 1))
    if not clauses and source.strip():
        start = len(source) - len(source.lstrip())
        end = len(source.rstrip())
        clauses.append(LogicalClause(source[start:end], start, end, 1))
    return clauses


def status_fragments(value: str) -> List[Tuple[int, int, LogicalClause]]:
    source = unicodedata.normalize("NFKC", value)
    fragments: List[Tuple[int, int, LogicalClause]] = []
    for clause in logical_clauses(source):
        cursor = 0
        for separator in COMPOUND_STATUS_SEPARATOR.finditer(clause.text):
            raw = clause.text[cursor : separator.start()]
            leading = len(raw) - len(raw.lstrip())
            trailing = len(raw.rstrip())
            start = clause.start + cursor + leading
            end = clause.start + cursor + trailing
            if start < end:
                fragments.append((start, end, clause))
            cursor = separator.end()
        raw = clause.text[cursor:]
        leading = len(raw) - len(raw.lstrip())
        trailing = len(raw.rstrip())
        start = clause.start + cursor + leading
        end = clause.start + cursor + trailing
        if start < end:
            fragments.append((start, end, clause))
    return fragments


def status_tokens(value: str) -> List[MatchedStatusToken]:
    """Collect clause-bounded SAFE/DANGEROUS tokens with original offsets."""

    source = unicodedata.normalize("NFKC", value)
    tokens: List[MatchedStatusToken] = []
    for clause in logical_clauses(source):
        safe_matches = list(SAFE_STATUS_TOKEN.finditer(clause.text))
        safe_spans = [(match.start(), match.end()) for match in safe_matches]
        for match in safe_matches:
            tokens.append(
                MatchedStatusToken(
                    StatusClass.SAFE,
                    match.group(0),
                    normalized(match.group(0)),
                    clause.start + match.start(),
                    clause.start + match.end(),
                    clause.text,
                    clause.start,
                    clause.end,
                    clause.order,
                )
            )
        for match in DANGEROUS_STATUS_TOKEN.finditer(clause.text):
            if any(match.start() < end and match.end() > start for start, end in safe_spans):
                continue
            tokens.append(
                MatchedStatusToken(
                    StatusClass.DANGEROUS,
                    match.group(0),
                    normalized(match.group(0)),
                    clause.start + match.start(),
                    clause.start + match.end(),
                    clause.text,
                    clause.start,
                    clause.end,
                    clause.order,
                )
            )
    if not tokens and normalized(source) in {"0", "no"}:
        clause = logical_clauses(source)[0]
        tokens.append(
            MatchedStatusToken(
                StatusClass.SAFE,
                clause.text,
                normalized(clause.text),
                clause.start,
                clause.end,
                clause.text,
                clause.start,
                clause.end,
                clause.order,
            )
        )
    elif not tokens and normalized(source) == "on":
        clause = logical_clauses(source)[0]
        tokens.append(
            MatchedStatusToken(
                StatusClass.DANGEROUS,
                clause.text,
                normalized(clause.text),
                clause.start,
                clause.end,
                clause.text,
                clause.start,
                clause.end,
                clause.order,
            )
        )
    return sorted(tokens, key=lambda item: (item.start, item.end, item.classification.value))


def residue_tokens(
    source: str,
    known_tokens: Sequence[MatchedStatusToken],
) -> List[MatchedStatusToken]:
    """Return every non-structural word not consumed by a known status span."""

    unknown: List[MatchedStatusToken] = []
    for fragment_start, fragment_end, clause in status_fragments(source):
        covered = sorted(
            (
                max(fragment_start, token.start),
                min(fragment_end, token.end),
            )
            for token in known_tokens
            if token.start < fragment_end and token.end > fragment_start
        )
        merged: List[Tuple[int, int]] = []
        for start, end in covered:
            if merged and start <= merged[-1][1]:
                merged[-1] = (merged[-1][0], max(merged[-1][1], end))
            else:
                merged.append((start, end))
        gaps: List[Tuple[int, int]] = []
        cursor = fragment_start
        for start, end in merged:
            if cursor < start:
                gaps.append((cursor, start))
            cursor = max(cursor, end)
        if cursor < fragment_end:
            gaps.append((cursor, fragment_end))
        for gap_start, gap_end in gaps:
            for match in RESIDUE_WORD.finditer(source[gap_start:gap_end]):
                raw = match.group(0)
                components = normalized(raw).split()
                if not components or all(word in ALLOWED_RESIDUE_WORDS for word in components):
                    continue
                start = gap_start + match.start()
                end = gap_start + match.end()
                unknown.append(
                    MatchedStatusToken(
                        StatusClass.UNKNOWN,
                        raw,
                        normalized(raw) or "<unknown>",
                        start,
                        end,
                        clause.text,
                        clause.start,
                        clause.end,
                        clause.order,
                    )
                )
    return unknown


def status_value_tokens(
    value: str, *, include_unknown: bool = False
) -> List[MatchedStatusToken]:
    """Collect known states and fail closed on unclassified compound fragments."""

    source = unicodedata.normalize("NFKC", value)
    tokens = status_tokens(source)
    if not include_unknown:
        return tokens

    tokens.extend(residue_tokens(source, tokens))
    return sorted(tokens, key=lambda item: (item.start, item.end))


def declaration_from_token(
    capability: str,
    token: MatchedStatusToken,
    original_value: str,
    document: str,
    scope: str,
    source_kind: str,
    line: int = 0,
    token_order: int = 0,
) -> StatusDeclaration:
    return StatusDeclaration(
        capability=capability,
        classification=token.classification,
        value=token.normalized,
        document=document,
        scope=scope,
        excerpt=excerpt(original_value),
        line=line + original_value.count("\n", 0, token.start),
        original_value=original_value,
        normalized_value=token.normalized,
        matched_token=token.original,
        key_path=scope,
        source_kind=source_kind,
        token_order=token_order,
        clause=token.clause,
        clause_start=token.clause_start,
        clause_end=token.clause_end,
        clause_order=token.clause_order,
    )


def token_applies_to_capability(
    capability: str,
    text: str,
    token: MatchedStatusToken,
    source_kind: str,
) -> bool:
    """Bind a prose/table token to the capability instead of nearby subjects."""

    if source_kind == "TABLE":
        return True
    source = unicodedata.normalize("NFKC", text)
    capability_matches = list(CAPABILITY_PATTERNS[capability].finditer(source))
    if not capability_matches:
        return False
    nearest = min(
        capability_matches,
        key=lambda item: min(abs(token.start - item.end()), abs(item.start() - token.end)),
    )
    if token.classification == StatusClass.UNKNOWN and token.end <= nearest.start():
        return False
    if token.start >= nearest.end():
        distance = token.start - nearest.end()
        relationship = source[nearest.end() : token.start]
    else:
        distance = nearest.start() - token.end
        relationship = source[token.end : nearest.start()]
    if token.classification == StatusClass.SAFE:
        return distance <= 360
    if distance > 180:
        return False
    normalized_relationship = normalized(relationship)
    return bool(
        ASSIGNMENT_CONNECTOR.search(relationship)
        or len(normalized_relationship.split()) <= 6
        or re.search(
            r"[+/,|，、；;]|\b(?:and|but|then)\b|并且|但|同时",
            relationship,
            re.I,
        )
    )


def explicit_capability_assignment(capability: str, text: str) -> bool:
    """Identify a real capability value assignment, not a prose list mention."""

    source = unicodedata.normalize("NFKC", text)
    for match in CAPABILITY_PATTERNS[capability].finditer(source):
        suffix = source[match.end() : match.end() + 90]
        if re.match(
            r"\s*(?:(?:status|authori[sz]ation|capability|movement|"
            r"implementation|state)\s*)?"
            r"(?::|=|\||\bis\b|\bare\b|\bremains?\b|\bbecomes?\b)",
            suffix,
            re.I,
        ):
            return True
    return False


def table_status_tokens(capability: str, text: str) -> List[MatchedStatusToken]:
    """Read status tokens only from cells adjacent to a capability-label cell."""

    cells = list(re.finditer(r"\|([^|]*)", text))
    tokens: List[MatchedStatusToken] = []
    for index, cell in enumerate(cells[:-1]):
        label = cell.group(1).strip()
        if not capability_in_text(capability, label):
            continue
        if len(normalized(label).split()) > 6:
            continue
        value_cell = cells[index + 1]
        value = value_cell.group(1)
        value_start = value_cell.start(1)
        for token in status_value_tokens(value, include_unknown=True):
            tokens.append(
                MatchedStatusToken(
                    token.classification,
                    token.original,
                    token.normalized,
                    value_start + token.start,
                    value_start + token.end,
                    token.clause,
                    value_start + token.clause_start,
                    value_start + token.clause_end,
                    token.clause_order,
                )
            )
    return sorted(tokens, key=lambda item: (item.start, item.end))


def selected_markdown_scopes(documents: Mapping[str, str]) -> List[Scope]:
    matrix_row = "\n".join(
        line for line in documents[MATRIX].splitlines() if line.startswith("| FE-04 |")
    )
    capability_rows = "\n".join(
        line
        for line in documents[CAPABILITY].splitlines()
        if re.match(
            r"^\|\s*(?:External Channel|order / execution / auto-trading|Dashboard)\s*\|",
            line,
            re.I,
        )
    )
    scopes: List[Scope] = []
    specs = (
        (SEMANTIC, "Message And Telegram V2", PRODUCTION_SCOPE_ALIASES["telegram"]),
        (INTERACTION, "Product Identity and Safety Contract", ("product identity safety contract", "safety contract")),
        (INTERACTION, "Mobile Push Detail", ("mobile push detail", "push detail")),
        (INTERACTION, "Mobile Message Center", ("mobile message center", "message center")),
        (INTERACTION, "Prohibited prototype behavior", ("prohibited prototype behavior",)),
        (
            STATE,
            "FE-04E Privacy/State Foundation",
            PRODUCTION_SCOPE_ALIASES["fe04e_privacy_state"],
        ),
        (DELIVERY, "Permanent Safety Rules", ("permanent safety rules", "永久安全规则")),
    )
    for document, name, aliases in specs:
        scopes.extend(markdown_alias_scopes(documents, document, name, aliases))
    scopes.extend(
        (
            Scope(MATRIX, "FE-04 delivery matrix row", matrix_row),
            Scope(CAPABILITY, "capability matrix frozen rows", capability_rows),
        )
    )
    return scopes


def yaml_entries(text: str) -> List[Tuple[str, str, str]]:
    entries: List[Tuple[str, str, str]] = []
    stack: List[Tuple[int, str]] = []
    for raw_line in text.splitlines():
        if not raw_line.strip() or raw_line.lstrip().startswith("#"):
            continue
        key_match = re.match(r"^(\s*)([A-Za-z0-9_-]+)\s*:\s*(.*)$", raw_line)
        if key_match:
            indent = len(key_match.group(1).replace("\t", "  "))
            key = key_match.group(2)
            value = key_match.group(3).strip().strip("\"'")
            while stack and stack[-1][0] >= indent:
                stack.pop()
            path = ".".join([item[1] for item in stack] + [key])
            entries.append((path, value, raw_line.strip()))
            if not value:
                stack.append((indent, key))
            continue
        item_match = re.match(r"^(\s*)-\s*(.+)$", raw_line)
        if item_match:
            indent = len(item_match.group(1).replace("\t", "  "))
            while stack and stack[-1][0] >= indent:
                stack.pop()
            path = ".".join(item[1] for item in stack)
            entries.append((path, item_match.group(2).strip().strip("\"'"), raw_line.strip()))
    return entries


REQUIRED_SCOPE_REQUIREMENTS = (
    ScopeRequirement(
        "Telegram",
        SEMANTIC,
        PRODUCTION_SCOPE_ALIASES["telegram"],
        re.compile(r"\btelegram\b", re.I),
    ),
    ScopeRequirement(
        "system notification",
        SEMANTIC,
        PRODUCTION_SCOPE_ALIASES["system_notification"],
        re.compile(r"\bsystem[\s-]*notifications?\b", re.I),
    ),
    ScopeRequirement(
        "external notification",
        STATE,
        PRODUCTION_SCOPE_ALIASES["fe04e_privacy_state"],
        re.compile(r"\bexternal[\s-]*(?:notification|send|delivery)", re.I),
    ),
    ScopeRequirement(
        "automatic notification",
        STATE,
        PRODUCTION_SCOPE_ALIASES["fe04e_privacy_state"],
        re.compile(r"\bautomatic[\s-]*notification", re.I),
    ),
    ScopeRequirement(
        "trading capability",
        STATE,
        PRODUCTION_SCOPE_ALIASES["fe04e_privacy_state"],
        re.compile(r"\btrading\s+capability\b", re.I),
    ),
    ScopeRequirement(
        "Public OPPORTUNITY contract",
        SEMANTIC,
        PRODUCTION_SCOPE_ALIASES["telegram"],
        re.compile(r"\bOPPORTUNITY\b", re.I),
    ),
    ScopeRequirement(
        "POSITION_RISK contract",
        SEMANTIC,
        PRODUCTION_SCOPE_ALIASES["telegram"],
        re.compile(r"\bPOSITION_RISK\b", re.I),
    ),
)


def required_scope_violations(documents: Mapping[str, str]) -> List[Violation]:
    guard = "required governed scope discovery guard"
    violations: List[Violation] = []
    for requirement in REQUIRED_SCOPE_REQUIREMENTS:
        sections = find_markdown_sections(
            documents[requirement.document], requirement.aliases
        )
        aliases = ", ".join(requirement.aliases)
        if not sections:
            violations.append(
                Violation(
                    guard,
                    requirement.document,
                    requirement.name,
                    "FAIL_CLOSED_MISSING_SCOPE",
                    f"parser result=0 sections; searched aliases=[{aliases}]",
                    "restore a non-empty governed capability section and rerun the semantic gate",
                )
            )
            continue
        for section in sections:
            if not section.body.strip():
                violations.append(
                    Violation(
                        guard,
                        requirement.document,
                        f"{requirement.name} [{section.title}]",
                        "FAIL_CLOSED_EMPTY_SCOPE",
                        f"parser result=empty section; searched aliases=[{aliases}]",
                        "add explicit governed status text; an empty capability scope cannot pass",
                    )
                )
        if requirement.body_pattern and not any(
            requirement.body_pattern.search(section.body) for section in sections
        ):
            violations.append(
                Violation(
                    guard,
                    requirement.document,
                    requirement.name,
                    "FAIL_CLOSED_SCOPE_CONTENT_MISSING",
                    f"parser result={len(sections)} section(s) but required capability text missing; aliases=[{aliases}]",
                    "restore the explicit capability contract inside the discovered scope",
                )
            )

    yaml_requirements = (
        (NEXT_TASK, "module"),
        (NEXT_TASK, "next_allowed_action"),
        (ACTIVE, "next_required_action"),
    )
    for document, required_leaf in yaml_requirements:
        matches = [
            (path, value)
            for path, value, _ in yaml_entries(documents[document])
            if path.split(".")[-1] == required_leaf
        ]
        if not matches or any(not value.strip() for _, value in matches):
            violations.append(
                Violation(
                    guard,
                    document,
                    "next task",
                    "FAIL_CLOSED_MISSING_OR_EMPTY_NEXT_TASK_SCOPE",
                    f"expected non-empty YAML key={required_leaf}; parser result={matches or 'missing'}",
                    "restore an explicit read-only readiness/governance next-task declaration",
                )
            )
    return violations


def capability_in_text(capability: str, text: str) -> bool:
    return bool(CAPABILITY_PATTERNS[capability].search(normalized(text)))


def is_negated(text: str, start: int) -> bool:
    prefix = text[max(0, start - 280) : start]
    if re.match(r"^\s*no\b", text[:start], re.I):
        return True
    return bool(
        re.search(
            r"(?:\bnot\b|\bno\b|\bnever\b|\bwithout\b|\bmust\s+not\b|"
            r"\b(?:blocked|disabled|prohibited|forbidden)\b).{0,250}$",
            prefix,
            re.I,
        )
    )


def dangerous_assignment(capability: str, text: str) -> bool:
    """Detect every positive capability status without safe-prefix masking."""

    if not capability_in_text(capability, text):
        return False
    return any(
        token.classification == StatusClass.DANGEROUS for token in status_tokens(text)
    )


def status_declarations_for_text(
    capability: str,
    text: str,
    document: str = "<memory>",
    scope: str = "<statement>",
    line: int = 0,
    source_kind: str = "PROSE",
) -> List[StatusDeclaration]:
    """Classify every explicit capability status in one prose/table statement."""

    if not capability_in_text(capability, text):
        return []
    kind = "TABLE" if text.lstrip().startswith("|") else source_kind
    explicit_status = explicit_capability_assignment(capability, text)
    matched_tokens = (
        table_status_tokens(capability, text)
        if kind == "TABLE"
        else [
            token
            for token in status_value_tokens(text, include_unknown=explicit_status)
            if token_applies_to_capability(capability, text, token, kind)
        ]
    )
    declarations = [
        declaration_from_token(
            capability,
            token,
            text,
            document,
            scope,
            kind,
            line,
            order,
        )
        for order, token in enumerate(matched_tokens, start=1)
    ]
    if kind == "TABLE" and not declarations:
        return []
    if explicit_status and not declarations:
        declarations.append(
            StatusDeclaration(
                capability,
                StatusClass.UNKNOWN,
                normalized(text) or "<unknown>",
                document,
                scope,
                excerpt(text),
                line,
                text,
                normalized(text),
                "<unknown>",
                scope,
                kind,
                1,
            )
        )
    return declarations


def positive_assignment(capability: str, text: str) -> bool:
    return any(
        item.classification == StatusClass.DANGEROUS
        for item in status_declarations_for_text(capability, text)
    )


def relevant_yaml_path(capability: str, path: str, value: str) -> bool:
    path_text = normalized(path)
    value_text = normalized(value)
    return capability_in_text(capability, path_text) or capability_in_text(
        capability, value_text
    )


def classify_yaml_value(path: str, value: str) -> Optional[StatusClass]:
    """Compatibility summary; declarations retain every matched status token."""

    value_normal = normalized(value)
    path_normal = normalized(path)
    if not value_normal:
        return None
    tokens = status_tokens(value)
    if any(token.classification == StatusClass.DANGEROUS for token in tokens):
        return StatusClass.DANGEROUS
    if tokens and all(token.classification == StatusClass.SAFE for token in tokens):
        return StatusClass.SAFE
    if value_normal in {"0", "no"}:
        return StatusClass.SAFE
    if re.search(
        r"\b(?:status|authori[sz]ation|enabled|active|implemented|send|delivery|movement)\b",
        path_normal,
    ):
        return StatusClass.UNKNOWN
    return None


def yaml_status_declarations(
    capability: str, document: str, text: str
) -> List[StatusDeclaration]:
    declarations: List[StatusDeclaration] = []
    for path, value, raw in yaml_entries(text):
        if not relevant_yaml_path(capability, path, value):
            continue
        path_has_capability = capability_in_text(capability, path)
        tokens = status_value_tokens(value, include_unknown=path_has_capability)
        if path_has_capability and tokens:
            declarations.extend(
                declaration_from_token(
                    capability,
                    token,
                    value,
                    document,
                    path or "<list item>",
                    "YAML",
                    0,
                    order,
                )
                for order, token in enumerate(tokens, start=1)
            )
            continue
        prose_units = statement_units(value) or [value]
        before = len(declarations)
        for prose_unit in prose_units:
            statement = f"{capability} {prose_unit}" if path_has_capability else prose_unit
            declarations.extend(
                status_declarations_for_text(
                    capability,
                    statement,
                    document,
                    path or "<list item>",
                    source_kind="YAML",
                )
            )
        if path_has_capability and len(declarations) == before and re.search(
            r"\b(?:status|authori[sz]ation|enabled|active|implemented|send|delivery|movement)\b",
            normalized(path),
        ):
            declarations.append(
                StatusDeclaration(
                    capability,
                    StatusClass.UNKNOWN,
                    normalized(value) or "<empty>",
                    document,
                    path or "<list item>",
                    excerpt(raw),
                    0,
                    value,
                    normalized(value),
                    "<unknown>",
                    path or "<list item>",
                    "YAML",
                    1,
                )
            )
    return declarations


def yaml_authorized(capability: str, path: str, value: str) -> bool:
    if not relevant_yaml_path(capability, path, value):
        return False
    classification = classify_yaml_value(path, value)
    if classification == StatusClass.DANGEROUS:
        return True
    return dangerous_assignment(capability, f"{path}: {value}")


def aggregate_authorization_violation(
    capability: str,
    guard: str,
    declarations: Sequence[StatusDeclaration],
) -> List[Violation]:
    unique: List[StatusDeclaration] = []
    seen = set()
    for declaration in declarations:
        key = (
            declaration.classification,
            declaration.value,
            declaration.original_value,
            declaration.matched_token,
            declaration.document,
            declaration.scope,
            declaration.excerpt,
            declaration.clause,
            declaration.clause_start,
            declaration.clause_end,
        )
        if key not in seen:
            seen.add(key)
            unique.append(declaration)
    dangerous = [item for item in unique if item.classification == StatusClass.DANGEROUS]
    unknown = [item for item in unique if item.classification == StatusClass.UNKNOWN]
    safe = [item for item in unique if item.classification == StatusClass.SAFE]
    if not dangerous and not unknown:
        return []
    if dangerous and safe:
        category = "CONTRADICTORY_AUTHORIZATION"
    elif unknown:
        category = "UNKNOWN_AUTHORIZATION_STATE"
    else:
        category = f"{capability} authorization"
    blocking = dangerous + unknown
    blocking_originals = {
        item.original_value or item.excerpt for item in blocking
    }
    first = (dangerous or unknown)[0]
    same_statement = [
        item
        for item in unique
        if (item.original_value or item.excerpt) in blocking_originals
    ]
    if dangerous and safe and not any(
        item.classification == StatusClass.SAFE for item in same_statement
    ):
        first_index = unique.index(first)

        def contextual_distance(item: StatusDeclaration) -> Tuple[int, int, int, int]:
            same_document = item.document == first.document
            same_scope = same_document and item.scope == first.scope
            item_root = item.scope.split(".", 1)[0]
            first_root = first.scope.split(".", 1)[0]
            same_scope_root = same_document and item_root == first_root
            return (
                0 if same_scope else 1,
                0 if same_scope_root else 1,
                0 if same_document else 1,
                abs(unique.index(item) - first_index),
            )

        diagnostic_items = [min(safe, key=contextual_distance), *same_statement]
    else:
        diagnostic_items = same_statement
    original_values = list(
        dict.fromkeys(item.original_value or item.excerpt for item in diagnostic_items)
    )
    values = "; ".join(
        f"{item.matched_token or item.value} [{item.classification.value}] "
        f"normalized={item.normalized_value or item.value} "
        f"source={item.document}/{item.key_path or item.scope} "
        f"line={item.line} kind={item.source_kind}"
        for item in diagnostic_items
    )
    clause_values = []
    clause_seen = set()
    for item in sorted(
        diagnostic_items, key=lambda value: (value.clause_order, value.token_order)
    ):
        clause_key = (item.clause_order, item.clause_start, item.clause_end, item.clause)
        if not item.clause or clause_key in clause_seen:
            continue
        clause_seen.add(clause_key)
        clause_values.append(
            f"{item.clause_order}. {item.clause} "
            f"CLASSIFICATION={item.classification.value} "
            f"OFFSET={item.clause_start}:{item.clause_end}"
        )
    unconsumed = [
        item.matched_token or item.value
        for item in diagnostic_items
        if item.classification == StatusClass.UNKNOWN
    ]
    return [
        Violation(
            guard,
            first.document,
            first.scope,
            category,
            excerpt(
                f"CAPABILITY={capability}; ORIGINAL_VALUE={original_values}; "
                f"CLAUSES=[{'; '.join(clause_values)}]; DETECTED=[{values}]; "
                f"UNCONSUMED={unconsumed}",
                1200,
            ),
            f"{capability} expects only explicit compatible SAFE values; any DANGEROUS or UNKNOWN value blocks",
        )
    ]


def authorization_violations(
    documents: Mapping[str, str], capability: str, guard: str
) -> List[Violation]:
    declarations: List[StatusDeclaration] = []
    for document in (ACTIVE, NEXT_TASK):
        declarations.extend(yaml_status_declarations(capability, document, documents[document]))
    for scope in selected_markdown_scopes(documents):
        for unit in statement_units(scope.text):
            declarations.extend(
                status_declarations_for_text(
                    capability,
                    unit,
                    scope.document,
                    scope.name,
                )
            )
    return aggregate_authorization_violation(capability, guard, declarations)


TRADING_MOVEMENT_PATTERN = re.compile(
    r"(?<![A-Za-z0-9_])(?:trading[\s_-]*)?capability[\s_-]*movement\b"
    r"\s*(?::|=|\||—|-|\bis\b)\s*"
    r"(?P<value>\"[^\"\n]*\"|'[^'\n]*'|[^\n#]+)",
    re.I,
)


def trading_movement_values(
    text: str, document: str = "<memory>", scope: str = "trading movement"
) -> List[StatusDeclaration]:
    declarations: List[StatusDeclaration] = []
    for match in TRADING_MOVEMENT_PATTERN.finditer(text):
        original_value = match.group("value").strip().strip("\"'")
        line = text.count("\n", 0, match.start()) + 1
        tokens = status_tokens(original_value)
        if not tokens:
            declarations.append(
                StatusDeclaration(
                    "trading capability movement",
                    StatusClass.UNKNOWN,
                    normalized(original_value) or "<empty>",
                    document,
                    scope,
                    excerpt(match.group(0)),
                    line,
                    original_value,
                    normalized(original_value),
                    "<unknown>",
                    scope,
                    "YAML",
                    1,
                )
            )
            continue
        for order, token in enumerate(tokens, start=1):
            classification = (
                StatusClass.SAFE
                if token.classification == StatusClass.SAFE
                and token.normalized == "none"
                else StatusClass.DANGEROUS
            )
            declarations.append(
                declaration_from_token(
                    "trading capability movement",
                    MatchedStatusToken(
                        classification,
                        token.original,
                        token.normalized,
                        token.start,
                        token.end,
                    ),
                    original_value,
                    document,
                    scope,
                    "YAML",
                    line,
                    order,
                )
            )
    return declarations


def trading_movement_violations(documents: Mapping[str, str]) -> List[Violation]:
    guard = "trading capability movement guard"
    declarations: List[StatusDeclaration] = []
    for document, text in documents.items():
        declarations.extend(trading_movement_values(text, document))
    return aggregate_authorization_violation(
        "trading capability movement", guard, declarations
    )


def pushrecheck_trade_violations(documents: Mapping[str, str]) -> List[Violation]:
    guard = "PushRecheck trade authorization guard"
    violations: List[Violation] = []
    unsafe = re.compile(
        r"\bpush\s*recheck\b.{0,100}\b(?:may|can|is\s+allowed\s+to|"
        r"authori[sz](?:e|es|ed)|permits?|allows?)\b.{0,100}"
        r"\b(?:trade|order|execution|open|close|reverse)\b",
        re.I,
    )
    safe = re.compile(
        r"\bpush\s*recheck\b.{0,80}\b(?:never|not|cannot|must\s+not)\b|"
        r"\bnot\s+(?:a\s+)?trade\s+authorization\b",
        re.I,
    )
    for scope in selected_markdown_scopes(documents):
        for unit in logical_units(scope.text):
            if unsafe.search(unit) and not safe.search(unit):
                violations.append(
                    Violation(
                        guard,
                        scope.document,
                        scope.name,
                        "PushRecheck trading authorization",
                        excerpt(unit),
                        "PushRecheck must never authorize trade or order execution",
                    )
                )
    for document in (ACTIVE, NEXT_TASK):
        for path, value, raw in yaml_entries(documents[document]):
            combined = f"{path}: {value}"
            if (
                re.search(r"\bpush[\s_-]*recheck\b", combined, re.I)
                and re.search(r"\b(?:trade|order|execution)[\s_-]*authorization\b", combined, re.I)
                and yaml_authorized("trading", path, value)
            ):
                violations.append(
                    Violation(
                        guard,
                        document,
                        path,
                        "PushRecheck trading authorization",
                        excerpt(raw),
                        "PushRecheck must never authorize trade or order execution",
                    )
                )
    return violations


NEXT_TASK_ALLOWED_ACTION = re.compile(
    r"\b(?:review|re[\s-]*evaluat\w*|audit|verify|inspect|confirm|compare|"
    r"assess\w*|validate\w*|read[\s-]*only\s+gate|output)\b|"
    r"\b(?:readiness|governance)\s+(?:assessment|validation|review|gate)\b",
    re.I,
)
NEXT_TASK_FORBIDDEN_TARGET = re.compile(
    r"\b(?:message\s*(?:center|push)?\s*ui|push\s*detail\s*ui|telegram|"
    r"system\s*notifications?|external\s*notifications?|automatic\s*notifications?|"
    r"automatic\s+delivery|auto\s+delivery|push\s+notifications?|mutation|"
    r"write\s+action|unread\s+count|fake\s+count|order|trade|trading|"
    r"close|reverse|execution|delivery|notification)\b",
    re.I,
)
NEXT_TASK_FORBIDDEN_ACTION = re.compile(
    r"\b(?:implement(?:ation)?|build|add|create|modify|mutate|mutation|write|"
    r"send|deliver|delivery|enable|activate|trigger|execute|dispatch)\w*\b",
    re.I,
)


def classify_next_task_statement(text: str) -> StatusClass:
    value = normalized(text)
    dangerous_actions = [
        match
        for match in NEXT_TASK_FORBIDDEN_ACTION.finditer(value)
        if not is_negated(value, match.start())
        and not re.search(
            r"\b(?:review|verify|audit)\b.{0,45}\b(?:prohibition|boundary|absence)\b",
            value[max(0, match.start() - 55) : match.end() + 55],
            re.I,
        )
    ]
    if dangerous_actions and NEXT_TASK_FORBIDDEN_TARGET.search(value):
        return StatusClass.DANGEROUS
    if NEXT_TASK_ALLOWED_ACTION.search(value):
        return StatusClass.SAFE
    if NEXT_TASK_FORBIDDEN_TARGET.search(value) and SAFE_STATUS.search(value):
        return StatusClass.SAFE
    return StatusClass.UNKNOWN


def next_task_violations(documents: Mapping[str, str]) -> List[Violation]:
    guard = "next-task capability authorization guard"
    target_paths = {
        "module",
        "next_business_phase",
        "next_allowed_action",
        "next_required_action",
        "allowed_scope",
    }
    declarations: List[StatusDeclaration] = []
    for document in (NEXT_TASK, ACTIVE):
        for path, value, raw in yaml_entries(documents[document]):
            leaf = path.split(".")[-1]
            if leaf not in target_paths or not value.strip():
                continue
            declarations.append(
                StatusDeclaration(
                    "next task",
                    classify_next_task_statement(value),
                    normalized(value),
                    document,
                    path,
                    excerpt(raw),
                )
            )
    return aggregate_authorization_violation("next task", guard, declarations)


def required_markers_missing(
    documents: Mapping[str, str],
    guard: str,
    markers: Sequence[Tuple[str, str, str]],
    expected: str,
) -> List[Violation]:
    violations: List[Violation] = []
    for document, scope, marker in markers:
        if marker not in documents[document]:
            violations.append(
                Violation(
                    guard,
                    document,
                    scope,
                    "cross-file required contract missing",
                    marker,
                    expected,
                )
            )
    return violations


def evaluate(documents: Mapping[str, str]) -> Dict[str, List[Violation]]:
    results: Dict[str, List[Violation]] = {name: [] for name in STATIC_GUARD_NAMES}

    results["required governed scope discovery guard"].extend(
        required_scope_violations(documents)
    )

    for guard, category in zip(CONTRADICTION_GUARDS[:4], PRIVATE_ENTITY_PATTERNS):
        results[guard].extend(
            contradiction_violations(
                documents,
                guard,
                category,
                PRIVATE_ENTITY_PATTERNS[category],
            )
        )
    results["OPPORTUNITY weakening exception guard"].extend(
        weakening_violations(documents)
    )

    capability_guards = (
        ("telegram", "Telegram semantic authorization guard"),
        ("system notification", "system notification semantic authorization guard"),
        ("external notification", "external notification semantic authorization guard"),
        ("automatic notification", "automatic notification semantic authorization guard"),
        ("trading", "trading semantic authorization guard"),
    )
    for capability, guard in capability_guards:
        results[guard].extend(authorization_violations(documents, capability, guard))

    results["trading capability movement guard"].extend(
        trading_movement_violations(documents)
    )
    results["PushRecheck trade authorization guard"].extend(
        pushrecheck_trade_violations(documents)
    )
    results["next-task capability authorization guard"].extend(
        next_task_violations(documents)
    )

    opportunity_markers = (
        (SEMANTIC, "Message And Telegram V2", "This is a strict no-private-state-oracle rule."),
        (
            INTERACTION,
            "Overview Dashboard",
            "any `OPPORTUNITY` preview uses the shared public projection and public state",
        ),
        (
            INTERACTION,
            "Mobile Push Detail",
            "This public/private split is shared by Dashboard opportunity previews,",
        ),
        (
            INTERACTION,
            "Mobile Message Center",
            "authenticated shared public `OPPORTUNITY`",
        ),
    )
    results["OPPORTUNITY cross-file consistency guard"].extend(
        required_markers_missing(
            documents,
            "OPPORTUNITY cross-file consistency guard",
            opportunity_markers,
            "all four surfaces must share the public-only no-private-state-oracle contract",
        )
    )
    for guard in CONTRADICTION_GUARDS:
        if results[guard]:
            results["OPPORTUNITY cross-file consistency guard"].append(
                Violation(
                    "OPPORTUNITY cross-file consistency guard",
                    results[guard][0].document,
                    results[guard][0].scope,
                    "cross-file public/private contradiction",
                    results[guard][0].excerpt,
                    "no governed surface may add a private-state exception",
                )
            )
            break

    notification_markers = (
        (
            SEMANTIC,
            "Message And Telegram V2",
            "Telegram is an `EXTENSION / PENDING_IMPLEMENTATION` notification outlet.",
        ),
        (
            INTERACTION,
            "Mobile Message Center",
            "No system-notification, AI-generated-message, delivery, or third message source",
        ),
        (
            ACTIVE,
            "fe_04e",
            'fe_04e_telegram_boundary_status: "PASS_EXTENSION_NOT_CONNECTED"',
        ),
        (
            NEXT_TASK,
            "compatibility boundary",
            "system notifications, Telegram, external send, automatic notification",
        ),
    )
    results["notification cross-file consistency guard"].extend(
        required_markers_missing(
            documents,
            "notification cross-file consistency guard",
            notification_markers,
            "notification channels must remain blocked across all governing sources",
        )
    )
    for guard in AUTHORIZATION_GUARDS[:4]:
        if results[guard]:
            results["notification cross-file consistency guard"].append(
                Violation(
                    "notification cross-file consistency guard",
                    results[guard][0].document,
                    results[guard][0].scope,
                    "cross-file notification authorization conflict",
                    results[guard][0].excerpt,
                    "all notification facts must remain not authorized",
                )
            )
            break

    trading_markers = (
        (
            DELIVERY,
            "Permanent Safety Rules",
            "Treat PushRecheck as trading authorization.",
        ),
        (SEMANTIC, "Message And Telegram V2", "PushRecheck never authorizes a trade."),
        (INTERACTION, "Product Identity", "Push recheck is not trading authorization."),
        (
            CAPABILITY,
            "External Channel",
            "| order / execution / auto-trading | 0 NOT_STARTED |",
        ),
        (
            NEXT_TASK,
            "checks",
            "No schema, Figma, Telegram, external-send, automatic-notification, AI, or trading capability movement occurs",
        ),
    )
    results["trading cross-file consistency guard"].extend(
        required_markers_missing(
            documents,
            "trading cross-file consistency guard",
            trading_markers,
            "PushRecheck is review-only and trading capability movement remains NONE",
        )
    )
    for guard in (
        "trading semantic authorization guard",
        "trading capability movement guard",
        "PushRecheck trade authorization guard",
        "next-task capability authorization guard",
    ):
        if results[guard]:
            results["trading cross-file consistency guard"].append(
                Violation(
                    "trading cross-file consistency guard",
                    results[guard][0].document,
                    results[guard][0].scope,
                    "cross-file trading authorization conflict",
                    results[guard][0].excerpt,
                    "trading remains not authorized and capability movement remains NONE",
                )
            )
            break
    return results


def insert_before(documents: MutableMapping[str, str], document: str, marker: str, text: str) -> None:
    source = documents[document]
    index = source.find(marker)
    if index < 0:
        raise ValueError(f"probe marker missing in {document}: {marker}")
    documents[document] = source[:index] + text.rstrip() + "\n\n" + source[index:]


def insert_after(documents: MutableMapping[str, str], document: str, marker: str, text: str) -> None:
    source = documents[document]
    index = source.find(marker)
    if index < 0:
        raise ValueError(f"probe marker missing in {document}: {marker}")
    insert_at = index + len(marker)
    documents[document] = (
        source[:insert_at] + "\n\n" + text.rstrip() + source[insert_at:]
    )


def append_text(documents: MutableMapping[str, str], document: str, text: str) -> None:
    documents[document] = documents[document].rstrip() + "\n" + text.rstrip() + "\n"


def replace_once(
    documents: MutableMapping[str, str], document: str, old: str, new: str
) -> None:
    source = documents[document]
    if source.count(old) != 1:
        raise ValueError(
            f"probe replacement expected exactly one match in {document}: {old}"
        )
    documents[document] = source.replace(old, new, 1)


def tagged_mutation(
    mutation: Callable[[MutableMapping[str, str]], None],
    fixtures: Sequence[str],
) -> Callable[[MutableMapping[str, str]], None]:
    setattr(mutation, "__fe04e_fixtures__", tuple(fixtures))
    return mutation


def mutation_before(document: str, marker: str, text: str) -> Callable[[MutableMapping[str, str]], None]:
    return tagged_mutation(
        lambda documents: insert_before(documents, document, marker, text), (text,)
    )


def mutation_after(document: str, marker: str, text: str) -> Callable[[MutableMapping[str, str]], None]:
    return tagged_mutation(
        lambda documents: insert_after(documents, document, marker, text), (text,)
    )


def mutation_append(document: str, text: str) -> Callable[[MutableMapping[str, str]], None]:
    return tagged_mutation(
        lambda documents: append_text(documents, document, text), (text,)
    )


def mutation_replace(
    document: str, old: str, new: str
) -> Callable[[MutableMapping[str, str]], None]:
    return tagged_mutation(
        lambda documents: replace_once(documents, document, old, new), (new,)
    )


def mutation_sequence(
    *mutations: Callable[[MutableMapping[str, str]], None],
) -> Callable[[MutableMapping[str, str]], None]:
    def apply(documents: MutableMapping[str, str]) -> None:
        for mutation in mutations:
            mutation(documents)

    fixtures = tuple(
        fixture
        for mutation in mutations
        for fixture in getattr(mutation, "__fe04e_fixtures__", ())
    )
    return tagged_mutation(apply, fixtures)


POSITION_RISK_PRIVATE_RECHECK_CONTROL = (
    "Authoritative private Recheck may be used for private state resolution."
)


def validate_position_risk_private_recheck_control(
    documents: Mapping[str, str],
) -> Optional[str]:
    public_scopes, private_scopes = telegram_domain_scopes(documents)
    if not public_scopes:
        return "production Telegram scope discovery returned no public domain"
    if not private_scopes:
        return "production Telegram scope discovery returned no POSITION_RISK domain"
    if any(POSITION_RISK_PRIVATE_RECHECK_CONTROL in scope.text for scope in public_scopes):
        return "legal private Recheck control leaked into public OPPORTUNITY scope"
    if not any(
        POSITION_RISK_PRIVATE_RECHECK_CONTROL in scope.text for scope in private_scopes
    ):
        return "legal private Recheck control was not parsed inside POSITION_RISK scope"
    if not is_unsafe_private_statement(
        POSITION_RISK_PRIVATE_RECHECK_CONTROL,
        PRIVATE_ENTITY_PATTERNS["private Recheck"],
    ):
        return "private Recheck sentence does not traverse the target semantic matcher"
    return None


def adversarial_probes() -> List[Probe]:
    semantic_end = "Owner-scoped `POSITION_RISK` detail remains a separate"
    private_marker = (
        "Owner-scoped `POSITION_RISK` detail remains a separate\n"
        "`OWNER_SCOPED_PRIVATE_PROJECTION`."
    )
    return [
        Probe(
            "public OPPORTUNITY private Recheck control pair",
            SEMANTIC,
            mutation_before(
                SEMANTIC,
                private_marker,
                POSITION_RISK_PRIVATE_RECHECK_CONTROL,
            ),
            "private Recheck contradiction guard",
            "PRIVATE_RECHECK_PUBLIC_STATE_DEPENDENCY",
        ),
        Probe(
            "private Recheck auxiliary readiness",
            SEMANTIC,
            mutation_before(
                SEMANTIC,
                semantic_end,
                "Private Recheck may be used as an auxiliary readiness input.",
            ),
            "private Recheck contradiction guard",
        ),
        Probe(
            "private execution fallback",
            SEMANTIC,
            mutation_before(
                SEMANTIC,
                semantic_end,
                "`execution_status` may be consulted as a fallback for public READY.",
            ),
            "private execution contradiction guard",
        ),
        Probe(
            "hidden UserPosition evaluation",
            SEMANTIC,
            mutation_before(
                SEMANTIC,
                semantic_end,
                "UserPosition is not exposed but may refine public evaluation.",
            ),
            "UserPosition/private risk contradiction guard",
        ),
        Probe(
            "private Push lifecycle supplement",
            SEMANTIC,
            mutation_before(
                SEMANTIC,
                semantic_end,
                "Private Push status may supplement public lifecycle.",
            ),
            "private Push contradiction guard",
        ),
        Probe(
            "table-form hidden private state exception",
            INTERACTION,
            mutation_before(
                INTERACTION,
                "**Click entries and targets**",
                "| Public evaluation | Private state is not returned but may participate when available |",
            ),
            "OPPORTUNITY weakening exception guard",
        ),
        Probe(
            "Telegram lowercase authorized",
            ACTIVE,
            mutation_append(ACTIVE, "telegram: authorized"),
            "Telegram semantic authorization guard",
        ),
        Probe(
            "Telegram enabled boolean",
            ACTIVE,
            mutation_append(ACTIVE, "telegram_enabled: true"),
            "Telegram semantic authorization guard",
        ),
        Probe(
            "Telegram nested implemented",
            ACTIVE,
            mutation_append(ACTIVE, "telegram:\n  status: implemented"),
            "Telegram semantic authorization guard",
        ),
        Probe(
            "Telegram Markdown enabled",
            SEMANTIC,
            mutation_before(
                SEMANTIC,
                "Push Detail remains review-only and source-specific.",
                "Telegram — Enabled",
            ),
            "Telegram semantic authorization guard",
        ),
        Probe(
            "Telegram capability movement limited",
            ACTIVE,
            mutation_append(ACTIVE, "telegram_capability_movement: LIMITED"),
            "Telegram semantic authorization guard",
        ),
        Probe(
            "system notification enabled",
            ACTIVE,
            mutation_append(ACTIVE, "system_notification: enabled"),
            "system notification semantic authorization guard",
        ),
        Probe(
            "external notification nested authorization",
            ACTIVE,
            mutation_append(
                ACTIVE,
                "external_notification:\n  authorization: enabled",
            ),
            "external notification semantic authorization guard",
        ),
        Probe(
            "automatic notification authorized yes",
            NEXT_TASK,
            mutation_append(NEXT_TASK, "automatic_notification_authorized: yes"),
            "automatic notification semantic authorization guard",
        ),
        Probe(
            "external notification Markdown table",
            STATE,
            mutation_before(
                STATE,
                "## P3-U2 iPhone Private Test App Foundation",
                "| External Notification | Active |",
            ),
            "external notification semantic authorization guard",
        ),
        Probe(
            "automatic notification nested auto-send",
            NEXT_TASK,
            mutation_append(
                NEXT_TASK,
                "automatic_notification:\n  auto_send: true",
            ),
            "automatic notification semantic authorization guard",
        ),
        Probe(
            "trading authorized boolean",
            NEXT_TASK,
            mutation_append(NEXT_TASK, "trading_authorized: true"),
            "trading semantic authorization guard",
        ),
        Probe(
            "trade capability enabled",
            NEXT_TASK,
            mutation_append(NEXT_TASK, "trade_capability: enabled"),
            "trading semantic authorization guard",
        ),
        Probe(
            "trading nested active",
            ACTIVE,
            mutation_append(ACTIVE, "trading:\n  status: active"),
            "trading semantic authorization guard",
        ),
        Probe(
            "PushRecheck order authorization",
            SEMANTIC,
            mutation_before(
                SEMANTIC,
                "## 8. Search Asset V2",
                "PushRecheck may authorize order execution.",
            ),
            "PushRecheck trade authorization guard",
        ),
        Probe(
            "trading capability movement limited",
            NEXT_TASK,
            mutation_append(NEXT_TASK, "TRADING_CAPABILITY_MOVEMENT: LIMITED"),
            "trading capability movement guard",
        ),
        Probe(
            "trading Markdown table",
            STATE,
            mutation_before(
                STATE,
                "## P3-U2 iPhone Private Test App Foundation",
                "| Trading | Allowed |",
            ),
            "trading semantic authorization guard",
        ),
        # Conflict aggregation: every declaration is retained; safe never masks danger.
        Probe(
            "Telegram prohibited then enabled conflict",
            SEMANTIC,
            mutation_before(
                SEMANTIC,
                "Push Detail remains review-only and source-specific.",
                "Telegram remains prohibited. Telegram is enabled.",
            ),
            "Telegram semantic authorization guard",
            "CONTRADICTORY_AUTHORIZATION",
            ("prohibited", "enabled"),
        ),
        Probe(
            "Telegram NOT_AUTHORIZED plus AUTHORIZED",
            ACTIVE,
            mutation_append(ACTIVE, "telegram: NOT_AUTHORIZED\ntelegram: AUTHORIZED"),
            "Telegram semantic authorization guard",
        ),
        Probe(
            "Telegram DISABLED plus ACTIVE",
            ACTIVE,
            mutation_append(ACTIVE, "telegram: DISABLED\ntelegram: ACTIVE"),
            "Telegram semantic authorization guard",
        ),
        Probe(
            "system notification safe plus enabled",
            ACTIVE,
            mutation_append(
                ACTIVE,
                "system_notification: NOT_AUTHORIZED\nsystem_notification: ENABLED",
            ),
            "system notification semantic authorization guard",
        ),
        Probe(
            "external notification safe plus implemented",
            ACTIVE,
            mutation_append(
                ACTIVE,
                "external_notification: DISABLED\nexternal_notification: IMPLEMENTED",
            ),
            "external notification semantic authorization guard",
        ),
        Probe(
            "automatic notification safe plus auto send",
            NEXT_TASK,
            mutation_append(
                NEXT_TASK,
                "automatic_notification: DISABLED\nautomatic_notification_auto_send: true",
            ),
            "automatic notification semantic authorization guard",
        ),
        Probe(
            "trading movement NONE plus LIMITED",
            NEXT_TASK,
            mutation_append(
                NEXT_TASK,
                "TRADING_CAPABILITY_MOVEMENT: NONE; TRADING_CAPABILITY_MOVEMENT: LIMITED",
            ),
            "trading capability movement guard",
        ),
        # All-match validation, including order independence and cross-file conflicts.
        Probe(
            "first safe second dangerous",
            SEMANTIC,
            mutation_before(
                SEMANTIC,
                "Push Detail remains review-only and source-specific.",
                "Telegram is disabled. Telegram is authorized.",
            ),
            "Telegram semantic authorization guard",
        ),
        Probe(
            "first dangerous second safe",
            SEMANTIC,
            mutation_before(
                SEMANTIC,
                "Push Detail remains review-only and source-specific.",
                "Telegram is authorized. Telegram is disabled.",
            ),
            "Telegram semantic authorization guard",
        ),
        Probe(
            "multiple dangerous Telegram values",
            ACTIVE,
            mutation_append(ACTIVE, "telegram: ENABLED\ntelegram: ACTIVE"),
            "Telegram semantic authorization guard",
        ),
        Probe(
            "cross-file Telegram status conflict",
            STATE,
            mutation_before(
                STATE,
                "## P3-U2 iPhone Private Test App Foundation",
                "Telegram is implemented.",
            ),
            "Telegram semantic authorization guard",
        ),
        # Compound declarations must preserve and validate every atomic state.
        Probe(
            "Telegram compound plus conflict",
            ACTIVE,
            mutation_append(ACTIVE, 'telegram: "NOT_AUTHORIZED + AUTHORIZED"'),
            "Telegram semantic authorization guard",
            "CONTRADICTORY_AUTHORIZATION",
            ("NOT_AUTHORIZED", "AUTHORIZED"),
        ),
        Probe(
            "Telegram compound comma conflict",
            ACTIVE,
            mutation_append(ACTIVE, 'telegram: "DISABLED, ENABLED"'),
            "Telegram semantic authorization guard",
            "CONTRADICTORY_AUTHORIZATION",
            ("DISABLED", "ENABLED"),
        ),
        Probe(
            "Telegram compound slash conflict",
            ACTIVE,
            mutation_append(ACTIVE, 'telegram: "BLOCKED / ACTIVE"'),
            "Telegram semantic authorization guard",
            "CONTRADICTORY_AUTHORIZATION",
            ("BLOCKED", "ACTIVE"),
        ),
        Probe(
            "Telegram compound conjunction conflict",
            ACTIVE,
            mutation_append(ACTIVE, 'telegram: "NOT_IMPLEMENTED but AUTHORIZED"'),
            "Telegram semantic authorization guard",
            "CONTRADICTORY_AUTHORIZATION",
            ("NOT_IMPLEMENTED", "AUTHORIZED"),
        ),
        Probe(
            "system notification compound conflict",
            ACTIVE,
            mutation_append(
                ACTIVE, 'system_notification: "NOT_AUTHORIZED + ENABLED"'
            ),
            "system notification semantic authorization guard",
            "CONTRADICTORY_AUTHORIZATION",
            ("NOT_AUTHORIZED", "ENABLED"),
        ),
        Probe(
            "external notification compound pipe conflict",
            ACTIVE,
            mutation_append(
                ACTIVE, 'external_notification: "DISABLED | IMPLEMENTED"'
            ),
            "external notification semantic authorization guard",
            "CONTRADICTORY_AUTHORIZATION",
            ("DISABLED", "IMPLEMENTED"),
        ),
        Probe(
            "automatic notification Chinese punctuation conflict",
            NEXT_TASK,
            mutation_append(NEXT_TASK, 'automatic_notification: "BLOCKED，ACTIVE"'),
            "automatic notification semantic authorization guard",
            "CONTRADICTORY_AUTHORIZATION",
            ("BLOCKED", "ACTIVE"),
        ),
        Probe(
            "trading status compound conflict",
            NEXT_TASK,
            mutation_append(NEXT_TASK, 'trading_status: "NOT_AUTHORIZED; ALLOWED"'),
            "trading semantic authorization guard",
            "CONTRADICTORY_AUTHORIZATION",
            ("NOT_AUTHORIZED", "ALLOWED"),
        ),
        Probe(
            "trading movement compound conflict",
            NEXT_TASK,
            mutation_append(
                NEXT_TASK, 'TRADING_CAPABILITY_MOVEMENT: "NONE + LIMITED"'
            ),
            "trading capability movement guard",
            "CONTRADICTORY_AUTHORIZATION",
            ("NONE", "LIMITED"),
        ),
        Probe(
            "Telegram compound Markdown table conflict",
            SEMANTIC,
            mutation_before(
                SEMANTIC,
                "Push Detail remains review-only and source-specific.",
                "| Telegram | NOT_AUTHORIZED + ACTIVE |",
            ),
            "Telegram semantic authorization guard",
            "CONTRADICTORY_AUTHORIZATION",
            ("NOT_AUTHORIZED", "ACTIVE"),
        ),
        Probe(
            "Telegram nested safe and dangerous conflict",
            ACTIVE,
            mutation_append(
                ACTIVE,
                "telegram:\n  status: NOT_AUTHORIZED\n  enabled: true",
            ),
            "Telegram semantic authorization guard",
            "CONTRADICTORY_AUTHORIZATION",
            ("NOT_AUTHORIZED", "true"),
        ),
        # Heading discovery must inspect every alias-matched section and fail closed.
        Probe(
            "bare Telegram heading dangerous status",
            SEMANTIC,
            mutation_sequence(
                mutation_replace(
                    SEMANTIC, "## 7. Message And Telegram V2", "## Telegram"
                ),
                mutation_before(
                    SEMANTIC,
                    "Push Detail remains review-only and source-specific.",
                    "Telegram is enabled.",
                ),
            ),
            "Telegram semantic authorization guard",
            expected_tokens=("enabled",),
        ),
        Probe(
            "Chinese Telegram status heading dangerous status",
            SEMANTIC,
            mutation_sequence(
                mutation_replace(
                    SEMANTIC,
                    "## 7. Message And Telegram V2",
                    "### Telegram 状态",
                ),
                mutation_before(
                    SEMANTIC,
                    "Push Detail remains review-only and source-specific.",
                    "Telegram is enabled.",
                ),
            ),
            "Telegram semantic authorization guard",
            expected_tokens=("enabled",),
        ),
        Probe(
            "future-extension Telegram heading dangerous status",
            SEMANTIC,
            mutation_sequence(
                mutation_replace(
                    SEMANTIC,
                    "## 7. Message And Telegram V2",
                    "### Telegram（未来扩展）",
                ),
                mutation_before(
                    SEMANTIC,
                    "Push Detail remains review-only and source-specific.",
                    "Telegram is implemented.",
                ),
            ),
            "Telegram semantic authorization guard",
            expected_tokens=("implemented",),
        ),
        Probe(
            "numbered Telegram heading dangerous status",
            SEMANTIC,
            mutation_sequence(
                mutation_replace(
                    SEMANTIC,
                    "## 7. Message And Telegram V2",
                    "#### 3. Telegram Authorization",
                ),
                mutation_before(
                    SEMANTIC,
                    "Push Detail remains review-only and source-specific.",
                    "Telegram is active.",
                ),
            ),
            "Telegram semantic authorization guard",
            expected_tokens=("active",),
        ),
        Probe(
            "empty Telegram capability section",
            SEMANTIC,
            mutation_before(SEMANTIC, "## 8. Search Asset V2", "### Telegram Status"),
            "required governed scope discovery guard",
        ),
        Probe(
            "dangerous Telegram status in second section",
            SEMANTIC,
            mutation_before(
                SEMANTIC,
                "## 8. Search Asset V2",
                "### Telegram Status\n\nTelegram is enabled.",
            ),
            "Telegram semantic authorization guard",
        ),
        # Equivalent private-state language remains forbidden in public OPPORTUNITY.
        Probe(
            "long short no position changes public lifecycle",
            SEMANTIC,
            mutation_before(
                SEMANTIC,
                semantic_end,
                "Long, short, or no position changes public lifecycle.",
            ),
            "UserPosition/private risk contradiction guard",
        ),
        Probe(
            "position direction influences public readiness",
            SEMANTIC,
            mutation_before(
                SEMANTIC,
                semantic_end,
                "Current position direction influences public readiness.",
            ),
            "UserPosition/private risk contradiction guard",
        ),
        Probe(
            "COMPLETED required for public READY",
            SEMANTIC,
            mutation_before(
                SEMANTIC,
                semantic_end,
                "COMPLETED is required for public READY.",
            ),
            "private execution contradiction guard",
            "PRIVATE_EXECUTION_COMPLETION_GATE",
        ),
        Probe(
            "FAILED produces public ERROR",
            SEMANTIC,
            mutation_before(
                SEMANTIC,
                semantic_end,
                "FAILED produces public ERROR.",
            ),
            "private execution contradiction guard",
            "PRIVATE_EXECUTION_FAILURE_ERROR",
        ),
        Probe(
            "execution completion gates public visibility",
            SEMANTIC,
            mutation_before(
                SEMANTIC,
                semantic_end,
                "Execution completion gates public visibility.",
            ),
            "private execution contradiction guard",
        ),
        Probe(
            "account risk supplements public status",
            SEMANTIC,
            mutation_before(
                SEMANTIC,
                semantic_end,
                "Account risk supplements public status.",
            ),
            "UserPosition/private risk contradiction guard",
        ),
        Probe(
            "position risk refines public evaluation",
            SEMANTIC,
            mutation_before(
                SEMANTIC,
                semantic_end,
                "Position risk refines public evaluation.",
            ),
            "UserPosition/private risk contradiction guard",
        ),
        Probe(
            "bare execution fallback",
            SEMANTIC,
            mutation_before(
                SEMANTIC,
                semantic_end,
                "Execution is used as fallback.",
            ),
            "private execution contradiction guard",
            "PRIVATE_EXECUTION_FALLBACK",
        ),
        Probe(
            "pending execution makes public partial",
            SEMANTIC,
            mutation_before(
                SEMANTIC,
                semantic_end,
                "Pending execution makes public PARTIAL.",
            ),
            "private execution contradiction guard",
            "PRIVATE_EXECUTION_PENDING_PARTIAL",
        ),
        Probe(
            "risk context determines public readiness",
            SEMANTIC,
            mutation_before(
                SEMANTIC,
                semantic_end,
                "Risk context determines public readiness.",
            ),
            "UserPosition/private risk contradiction guard",
            "PRIVATE_RISK_PUBLIC_STATE_DEPENDENCY",
        ),
        Probe(
            "hidden private risk is used",
            SEMANTIC,
            mutation_before(
                SEMANTIC,
                semantic_end,
                "Private risk is hidden but used.",
            ),
            "UserPosition/private risk contradiction guard",
            "PRIVATE_RISK_PUBLIC_STATE_DEPENDENCY",
        ),
        Probe(
            "long position changes public lifecycle",
            SEMANTIC,
            mutation_before(
                SEMANTIC,
                semantic_end,
                "Long position changes public lifecycle.",
            ),
            "UserPosition/private risk contradiction guard",
            "POSITION_DIRECTION_PUBLIC_STATE_DEPENDENCY",
        ),
        Probe(
            "short position changes public status",
            SEMANTIC,
            mutation_before(
                SEMANTIC,
                semantic_end,
                "Short position changes public status.",
            ),
            "UserPosition/private risk contradiction guard",
            "POSITION_DIRECTION_PUBLIC_STATE_DEPENDENCY",
        ),
        Probe(
            "no position changes public readiness",
            SEMANTIC,
            mutation_before(
                SEMANTIC,
                semantic_end,
                "No position changes public readiness.",
            ),
            "UserPosition/private risk contradiction guard",
            "POSITION_DIRECTION_PUBLIC_STATE_DEPENDENCY",
        ),
        Probe(
            "Chinese long position changes public state",
            SEMANTIC,
            mutation_before(SEMANTIC, semantic_end, "多仓改变公共状态。"),
            "UserPosition/private risk contradiction guard",
            "POSITION_DIRECTION_PUBLIC_STATE_DEPENDENCY",
        ),
        Probe(
            "Chinese short position affects public lifecycle",
            SEMANTIC,
            mutation_before(SEMANTIC, semantic_end, "空仓影响公开生命周期。"),
            "UserPosition/private risk contradiction guard",
            "POSITION_DIRECTION_PUBLIC_STATE_DEPENDENCY",
        ),
        Probe(
            "Chinese no position determines public readiness",
            SEMANTIC,
            mutation_before(SEMANTIC, semantic_end, "无仓决定公开就绪状态。"),
            "UserPosition/private risk contradiction guard",
            "POSITION_DIRECTION_PUBLIC_STATE_DEPENDENCY",
        ),
        Probe(
            "Chinese position direction participates public evaluation",
            SEMANTIC,
            mutation_before(SEMANTIC, semantic_end, "持仓方向参与公开评估。"),
            "UserPosition/private risk contradiction guard",
            "POSITION_DIRECTION_PUBLIC_STATE_DEPENDENCY",
        ),
        Probe(
            "Chinese hidden private risk participates calculation",
            SEMANTIC,
            mutation_before(SEMANTIC, semantic_end, "私有风险虽不展示但参与计算。"),
            "UserPosition/private risk contradiction guard",
            "PRIVATE_RISK_PUBLIC_STATE_DEPENDENCY",
        ),
        # Next task is an audit/readiness gate, never an implementation action.
        Probe(
            "next task adds mutation and automatic delivery",
            NEXT_TASK,
            mutation_append(
                NEXT_TASK,
                'module: "Message/Push UI add mutation and automatic delivery"',
            ),
            "next-task capability authorization guard",
        ),
        Probe(
            "next task implements Message UI",
            NEXT_TASK,
            mutation_append(NEXT_TASK, 'module: "Implement Message UI"'),
            "next-task capability authorization guard",
        ),
        Probe(
            "next task adds Telegram integration",
            NEXT_TASK,
            mutation_append(NEXT_TASK, 'module: "Add Telegram integration"'),
            "next-task capability authorization guard",
        ),
        Probe(
            "next task enables external notification",
            NEXT_TASK,
            mutation_append(NEXT_TASK, 'module: "Enable external notification"'),
            "next-task capability authorization guard",
        ),
        Probe(
            "next task adds trading action",
            NEXT_TASK,
            mutation_append(NEXT_TASK, 'module: "Add trading action"'),
            "next-task capability authorization guard",
        ),
        # Clause isolation: later authorization must survive an earlier SAFE clause.
        Probe(
            "Telegram safe clause then allowed delivery",
            SEMANTIC,
            mutation_before(
                SEMANTIC,
                "Push Detail remains review-only and source-specific.",
                "No Telegram capability is authorized, but Telegram delivery is allowed.",
            ),
            "Telegram semantic authorization guard",
            "CONTRADICTORY_AUTHORIZATION",
            ("No Telegram capability is authorized", "allowed"),
        ),
        Probe(
            "Telegram not enabled then active delivery",
            SEMANTIC,
            mutation_before(
                SEMANTIC,
                "Push Detail remains review-only and source-specific.",
                "Telegram is not enabled; Telegram delivery is active.",
            ),
            "Telegram semantic authorization guard",
            "CONTRADICTORY_AUTHORIZATION",
            ("not enabled", "active"),
        ),
        Probe(
            "Telegram prohibited then supported integration",
            SEMANTIC,
            mutation_before(
                SEMANTIC,
                "Push Detail remains review-only and source-specific.",
                "Telegram remains prohibited. Telegram integration is supported.",
            ),
            "Telegram semantic authorization guard",
            "CONTRADICTORY_AUTHORIZATION",
            ("prohibited", "supported"),
        ),
        Probe(
            "system notification disabled however automatic delivery enabled",
            SEMANTIC,
            mutation_before(
                SEMANTIC,
                "Push Detail remains review-only and source-specific.",
                "System notification is disabled, however automatic delivery is enabled.",
            ),
            "system notification semantic authorization guard",
            "CONTRADICTORY_AUTHORIZATION",
            ("disabled", "enabled"),
        ),
        Probe(
            "trading unauthorized yet order placement allowed",
            STATE,
            mutation_before(
                STATE,
                "## P3-U2 iPhone Private Test App Foundation",
                "Trading is not authorized, yet order placement is allowed.",
            ),
            "trading semantic authorization guard",
            "CONTRADICTORY_AUTHORIZATION",
            ("not authorized", "allowed"),
        ),
        Probe(
            "Chinese Telegram unauthorized then enabled",
            SEMANTIC,
            mutation_before(
                SEMANTIC,
                "Push Detail remains review-only and source-specific.",
                "未授权 Telegram，但 Telegram 推送已启用。",
            ),
            "Telegram semantic authorization guard",
            "CONTRADICTORY_AUTHORIZATION",
            ("未授权", "已启用"),
        ),
        # Unknown residue must be preserved after every recognized SAFE span.
        Probe(
            "Telegram NOT_AUTHORIZED MAYBE residue",
            ACTIVE,
            mutation_append(ACTIVE, "telegram: NOT_AUTHORIZED MAYBE"),
            "Telegram semantic authorization guard",
            "UNKNOWN_AUTHORIZATION_STATE",
            ("MAYBE",),
        ),
        Probe(
            "Telegram NOT_AUTHORIZED AND_MAYBE residue",
            ACTIVE,
            mutation_append(ACTIVE, "telegram: NOT_AUTHORIZED AND_MAYBE"),
            "Telegram semantic authorization guard",
            "UNKNOWN_AUTHORIZATION_STATE",
            ("AND_MAYBE",),
        ),
        Probe(
            "Telegram NOT_AUTHORIZED CONDITIONAL residue",
            ACTIVE,
            mutation_append(ACTIVE, "telegram: NOT_AUTHORIZED CONDITIONAL"),
            "Telegram semantic authorization guard",
            "UNKNOWN_AUTHORIZATION_STATE",
            ("CONDITIONAL",),
        ),
        Probe(
            "Telegram DISABLED OPTIONAL residue",
            ACTIVE,
            mutation_append(ACTIVE, "telegram: DISABLED OPTIONAL"),
            "Telegram semantic authorization guard",
            "UNKNOWN_AUTHORIZATION_STATE",
            ("OPTIONAL",),
        ),
        Probe(
            "system notification NOT_IMPLEMENTED TBD residue",
            ACTIVE,
            mutation_append(ACTIVE, "system_notification: NOT_IMPLEMENTED TBD"),
            "system notification semantic authorization guard",
            "UNKNOWN_AUTHORIZATION_STATE",
            ("TBD",),
        ),
        Probe(
            "external notification BLOCKED EXPERIMENTAL residue",
            ACTIVE,
            mutation_append(ACTIVE, "external_notification: BLOCKED EXPERIMENTAL"),
            "external notification semantic authorization guard",
            "UNKNOWN_AUTHORIZATION_STATE",
            ("EXPERIMENTAL",),
        ),
        Probe(
            "automatic notification DISABLED BETA residue",
            NEXT_TASK,
            mutation_append(NEXT_TASK, "automatic_notification: DISABLED BETA"),
            "automatic notification semantic authorization guard",
            "UNKNOWN_AUTHORIZATION_STATE",
            ("BETA",),
        ),
        Probe(
            "trading NOT_AUTHORIZED PREVIEW residue",
            NEXT_TASK,
            mutation_append(NEXT_TASK, "trading: NOT_AUTHORIZED PREVIEW"),
            "trading semantic authorization guard",
            "UNKNOWN_AUTHORIZATION_STATE",
            ("PREVIEW",),
        ),
        Probe(
            "trading movement NONE MAYBE residue",
            NEXT_TASK,
            mutation_append(NEXT_TASK, "TRADING_CAPABILITY_MOVEMENT: NONE MAYBE"),
            "trading semantic authorization guard",
            "UNKNOWN_AUTHORIZATION_STATE",
            ("MAYBE",),
        ),
        Probe(
            "Telegram prohibited possibly residue prose",
            SEMANTIC,
            mutation_before(
                SEMANTIC,
                "Push Detail remains review-only and source-specific.",
                "Telegram remains prohibited, possibly.",
            ),
            "Telegram semantic authorization guard",
            "UNKNOWN_AUTHORIZATION_STATE",
            ("possibly",),
        ),
        Probe(
            "Telegram NOT_AUTHORIZED MAYBE residue table",
            SEMANTIC,
            mutation_before(
                SEMANTIC,
                "Push Detail remains review-only and source-specific.",
                "| Telegram | NOT_AUTHORIZED MAYBE |",
            ),
            "Telegram semantic authorization guard",
            "UNKNOWN_AUTHORIZATION_STATE",
            ("MAYBE",),
            coverage_objective="table_contract_rejection",
        ),
        Probe(
            "Telegram nested qualifier MAYBE residue",
            ACTIVE,
            mutation_append(
                ACTIVE,
                "telegram:\n  status: NOT_AUTHORIZED\n  qualifier: MAYBE",
            ),
            "Telegram semantic authorization guard",
            "UNKNOWN_AUTHORIZATION_STATE",
            ("MAYBE",),
        ),
        Probe(
            "Public READY requires execution completion",
            SEMANTIC,
            mutation_before(
                SEMANTIC,
                semantic_end,
                "Public READY requires execution completion.",
            ),
            "private execution contradiction guard",
            "PRIVATE_EXECUTION_COMPLETION_GATE",
            ("Public READY requires execution completion.",),
        ),
    ]


def legal_control_probes() -> List[Probe]:
    return [
        Probe(
            "Telegram NOT_AUTHORIZED control",
            ACTIVE,
            mutation_append(ACTIVE, "telegram: NOT_AUTHORIZED"),
            "",
        ),
        Probe(
            "Telegram prohibited prose control",
            SEMANTIC,
            mutation_before(
                SEMANTIC,
                "Push Detail remains review-only and source-specific.",
                "Telegram remains prohibited.",
            ),
            "",
        ),
        Probe(
            "Telegram movement NONE control",
            ACTIVE,
            mutation_append(ACTIVE, "telegram_capability_movement: NONE"),
            "",
        ),
        Probe(
            "external notification NOT_IMPLEMENTED control",
            ACTIVE,
            mutation_append(ACTIVE, "external_notification: NOT_IMPLEMENTED"),
            "",
        ),
        Probe(
            "trading movement NONE control",
            NEXT_TASK,
            mutation_append(NEXT_TASK, "TRADING_CAPABILITY_MOVEMENT: NONE"),
            "",
        ),
        Probe(
            "PushRecheck not trade authorization control",
            SEMANTIC,
            mutation_before(
                SEMANTIC,
                "## 8. Search Asset V2",
                "PushRecheck is not a trade authorization.",
            ),
            "",
        ),
        Probe(
            "unrelated ready true control",
            NEXT_TASK,
            mutation_append(NEXT_TASK, "unrelated_feature:\n  ready: true"),
            "",
        ),
        Probe(
            "duplicate safe Telegram values control",
            ACTIVE,
            mutation_append(ACTIVE, "telegram: DISABLED\ntelegram: NOT_AUTHORIZED"),
            "",
        ),
        Probe(
            "Telegram heading level change control",
            SEMANTIC,
            mutation_replace(
                SEMANTIC,
                "## 7. Message And Telegram V2",
                "### 7. Message And Telegram V2",
            ),
            "",
        ),
        Probe(
            "Telegram numbered heading alias control",
            SEMANTIC,
            mutation_replace(
                SEMANTIC,
                "## 7. Message And Telegram V2",
                "#### Part A - Telegram Authorization",
            ),
            "",
        ),
        Probe(
            "Telegram bilingual heading alias control",
            SEMANTIC,
            mutation_replace(
                SEMANTIC,
                "## 7. Message And Telegram V2",
                "### 通知 / Telegram（未来扩展）",
            ),
            "",
        ),
        Probe(
            "multiple safe Telegram sections control",
            SEMANTIC,
            mutation_before(
                SEMANTIC,
                "## 8. Search Asset V2",
                "### Telegram Status\n\nTelegram remains prohibited.",
            ),
            "",
        ),
        Probe(
            "review mutation prohibition control",
            NEXT_TASK,
            mutation_replace(
                NEXT_TASK,
                'module: "FE-04E Message/Push UI Readiness and Governance Re-evaluation"',
                'module: "Review mutation prohibition for Message/Push readiness"',
            ),
            "",
        ),
        Probe(
            "verify no automatic delivery control",
            NEXT_TASK,
            mutation_replace(
                NEXT_TASK,
                'module: "FE-04E Message/Push UI Readiness and Governance Re-evaluation"',
                'module: "Verify no automatic delivery in the read-only gate"',
            ),
            "",
        ),
        Probe(
            "audit Telegram prohibition control",
            NEXT_TASK,
            mutation_replace(
                NEXT_TASK,
                'module: "FE-04E Message/Push UI Readiness and Governance Re-evaluation"',
                'module: "Audit Telegram prohibition in the read-only gate"',
            ),
            "",
        ),
        Probe(
            "automatic notification DISABLED control",
            NEXT_TASK,
            mutation_append(NEXT_TASK, "automatic_notification: DISABLED"),
            "",
        ),
        Probe(
            "compound safe Telegram plus control",
            ACTIVE,
            mutation_append(ACTIVE, 'telegram: "NOT_AUTHORIZED + BLOCKED"'),
            "",
        ),
        Probe(
            "compound safe Telegram slash control",
            ACTIVE,
            mutation_append(ACTIVE, 'telegram: "NOT_IMPLEMENTED / DISABLED"'),
            "",
        ),
        Probe(
            "safe Telegram values across files control",
            ACTIVE,
            mutation_sequence(
                mutation_append(ACTIVE, "telegram: DISABLED"),
                mutation_append(NEXT_TASK, "telegram: NOT_AUTHORIZED"),
            ),
            "",
            coverage_objective="cross_file_contract_acceptance",
        ),
        Probe(
            "owner-scoped POSITION_RISK private Recheck control",
            SEMANTIC,
            mutation_after(
                SEMANTIC,
                "Owner-scoped `POSITION_RISK` detail remains a separate\n"
                "`OWNER_SCOPED_PRIVATE_PROJECTION`.",
                POSITION_RISK_PRIVATE_RECHECK_CONTROL,
            ),
            "",
            validator=validate_position_risk_private_recheck_control,
        ),
        Probe(
            "Telegram explicit NOT_AUTHORIZED grammar control",
            SEMANTIC,
            mutation_before(
                SEMANTIC,
                "Push Detail remains review-only and source-specific.",
                "Telegram is explicitly NOT_AUTHORIZED.",
            ),
            "",
        ),
        Probe(
            "Telegram capability remains NOT_AUTHORIZED grammar control",
            SEMANTIC,
            mutation_before(
                SEMANTIC,
                "Push Detail remains review-only and source-specific.",
                "Telegram capability remains NOT_AUTHORIZED.",
            ),
            "",
        ),
        Probe(
            "Public READY explicit execution non-use control",
            SEMANTIC,
            mutation_before(
                SEMANTIC,
                "Owner-scoped `POSITION_RISK` detail remains a separate",
                "Public READY is determined only by the shared public OPPORTUNITY "
                "projection; execution completion is not used.",
            ),
            "",
        ),
    ]


def coverage_slug(value: str) -> str:
    slug = re.sub(r"[^a-z0-9]+", "-", normalized(value)).strip("-")
    return slug or "unnamed"


def coverage_fingerprint(value: str) -> str:
    canonical = re.sub(
        r"[^a-z0-9\u3400-\u4dbf\u4e00-\u9fff]+", " ", normalized(value)
    ).strip()
    return hashlib.sha256(canonical.encode("utf-8")).hexdigest()[:16]


def helper_unit_coverage_records(test_path: Path) -> List[CoverageRecord]:
    """Load unittest IDs and attached metadata without executing the suite."""

    module_name = "fe04e_governance_helper_inventory"
    spec = importlib.util.spec_from_file_location(module_name, test_path)
    if spec is None or spec.loader is None:
        raise RuntimeError(f"cannot load helper test inventory from {test_path}")
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    test_class = getattr(module, "GovernanceSemanticHelperTest")
    method_names = sorted(
        name for name in dir(test_class) if name.startswith("test_") and callable(getattr(test_class, name))
    )
    records: List[CoverageRecord] = []
    for name in method_names:
        method = getattr(test_class, name)
        metadata = dict(getattr(method, "__fe04e_coverage__", {}))
        source = inspect.getsource(method)
        fixture = metadata.get("fixture", source)
        records.append(
            CoverageRecord(
                id=f"HELPER:{name}",
                category="HELPER_UNIT_TEST",
                layer=metadata.get("layer", "L1_UNIT"),
                entrypoint=metadata.get("entrypoint", name),
                target_guard=metadata.get("target_guard", name),
                expected_result=metadata.get("expected_result", "PASS"),
                assertion_objective=metadata.get("assertion_objective", name),
                fixture_fingerprint=coverage_fingerprint(fixture),
                source=f"{test_path.name}:{name}",
            )
        )
    return records


def probe_coverage_record(probe: Probe, *, legal: bool = False) -> CoverageRecord:
    fixtures = getattr(probe.mutation, "__fe04e_fixtures__", ()) or (probe.name,)
    expected = "ACCEPT" if legal else "REJECT"
    target_guard = "LEGAL_CONTROL" if legal else probe.expected_guard
    objective = probe.coverage_objective or (
        "contract_acceptance" if legal else "contract_rejection"
    )
    prefix = "LEGAL" if legal else "NEGATIVE"
    return CoverageRecord(
        id=f"{prefix}:{coverage_slug(probe.name)}",
        category="LEGAL_CONTROL" if legal else "NEGATIVE_PROBE",
        layer=probe.coverage_layer,
        entrypoint=probe.coverage_entrypoint,
        target_guard=target_guard,
        expected_result=expected,
        assertion_objective=objective,
        fixture_fingerprint=coverage_fingerprint("\n".join(fixtures)),
        source=probe.name,
    )


def deduplicate_coverage_records(
    records: Sequence[CoverageRecord],
) -> Tuple[List[str], Dict[str, str]]:
    first_by_key: Dict[str, CoverageRecord] = {}
    duplicate_of: Dict[str, str] = {}
    qualifying_ids: List[str] = []
    for record in records:
        primary = first_by_key.get(record.qualifying_key)
        if primary is None:
            first_by_key[record.qualifying_key] = record
            qualifying_ids.append(record.id)
        else:
            duplicate_of[record.id] = primary.id
    return qualifying_ids, duplicate_of


def build_coverage_inventory(
    *,
    static_assertions: int,
    legacy_negative_probes: int,
    helper_test_path: Path,
    include_helper_unit_tests: bool = True,
    include_semantic_probes: bool = True,
) -> Dict[str, object]:
    records: List[CoverageRecord] = []
    records.extend(
        CoverageRecord(
            id=f"STATIC:{index:03d}",
            category="STATIC_ASSERTION",
            layer="L3_CONTRACT_RUNNER",
            entrypoint="check-fe04e-governance-contract.sh",
            target_guard=f"static-assertion-{index:03d}",
            expected_result="PASS",
            assertion_objective=f"static-contract-{index:03d}",
            fixture_fingerprint=coverage_fingerprint(f"static-{index:03d}"),
            source="scripts/check-fe04e-governance-contract.sh",
        )
        for index in range(1, static_assertions + 1)
    )
    records.extend(
        CoverageRecord(
            id=f"SEMANTIC:{coverage_slug(name)}",
            category="SEMANTIC_GUARD",
            layer="L2_HELPER_INTEGRATION",
            entrypoint="semantic_evaluate",
            target_guard=name,
            expected_result="PASS",
            assertion_objective="baseline_guard_acceptance",
            fixture_fingerprint=coverage_fingerprint(name),
            source="SEMANTIC_GUARD_NAMES",
        )
        for name in SEMANTIC_GUARD_NAMES
    )
    if include_helper_unit_tests:
        records.extend(helper_unit_coverage_records(helper_test_path))
    records.extend(
        CoverageRecord(
            id=f"NEGATIVE:legacy-shell-{index:03d}",
            category="NEGATIVE_PROBE",
            layer="L3_CONTRACT_PROBE",
            entrypoint="check-fe04e-governance-contract.sh",
            target_guard=f"legacy-shell-probe-{index:03d}",
            expected_result="REJECT",
            assertion_objective="runner_rejection_and_restore",
            fixture_fingerprint=coverage_fingerprint(f"legacy-shell-{index:03d}"),
            source="run_negative_probe",
        )
        for index in range(1, legacy_negative_probes + 1)
    )
    if include_semantic_probes:
        records.extend(probe_coverage_record(probe) for probe in adversarial_probes())
        records.extend(
            probe_coverage_record(probe, legal=True) for probe in legal_control_probes()
        )

    qualifying_ids, duplicate_of = deduplicate_coverage_records(records)

    categories: Dict[str, Dict[str, object]] = {}
    for category in (
        "STATIC_ASSERTION",
        "SEMANTIC_GUARD",
        "HELPER_UNIT_TEST",
        "NEGATIVE_PROBE",
        "LEGAL_CONTROL",
    ):
        category_records = [record for record in records if record.category == category]
        category_ids = [record.id for record in category_records]
        category_qualifying = [item for item in category_ids if item not in duplicate_of]
        category_duplicates = [item for item in category_ids if item in duplicate_of]
        categories[category] = {
            "raw": len(category_ids),
            "qualifying": len(category_qualifying),
            "ids": category_ids,
            "qualifying_ids": category_qualifying,
            "duplicate_ids": category_duplicates,
        }

    digest_source = json.dumps(
        {
            "records": [
                {
                    "id": record.id,
                    "key": record.qualifying_key,
                    "source": record.source,
                }
                for record in records
            ],
            "duplicates": duplicate_of,
        },
        ensure_ascii=True,
        sort_keys=True,
        separators=(",", ":"),
    )
    return {
        "records": records,
        "categories": categories,
        "duplicate_of": duplicate_of,
        "raw_total": len(records),
        "duplicate_total": len(duplicate_of),
        "qualifying_total": len(qualifying_ids),
        "digest": hashlib.sha256(digest_source.encode("utf-8")).hexdigest(),
    }


def print_coverage_inventory(inventory: Mapping[str, object]) -> None:
    categories = inventory["categories"]
    assert isinstance(categories, dict)
    key_prefixes = {
        "STATIC_ASSERTION": "STATIC_ASSERTIONS",
        "SEMANTIC_GUARD": "SEMANTIC_GUARDS",
        "HELPER_UNIT_TEST": "HELPER_UNIT_TESTS",
        "NEGATIVE_PROBE": "NEGATIVE_PROBES",
        "LEGAL_CONTROL": "LEGAL_CONTROLS",
    }
    for category, prefix in key_prefixes.items():
        details = categories[category]
        assert isinstance(details, dict)
        print(f"FE04E_RAW_{prefix}: {details['raw']}")
        print(f"FE04E_QUALIFYING_{prefix}: {details['qualifying']}")
        print(f"FE04E_{prefix}_IDS: {','.join(details['ids'])}")
        print(
            f"FE04E_{prefix}_DUPLICATE_IDS: "
            f"{','.join(details['duplicate_ids']) or 'NONE'}"
        )
    duplicate_of = inventory["duplicate_of"]
    assert isinstance(duplicate_of, dict)
    records = inventory["records"]
    assert isinstance(records, list)
    records_by_id = {record.id: record for record in records}
    for duplicate_id, primary_id in sorted(duplicate_of.items()):
        duplicate = records_by_id[duplicate_id]
        print(
            f"FE04E_DUPLICATE_EXECUTION: {duplicate_id} -> {primary_id}; "
            f"layer={duplicate.layer}; entrypoint={duplicate.entrypoint}; "
            f"guard={duplicate.target_guard}; fixture={duplicate.fixture_fingerprint}; "
            "reason=same qualifying key, counted once"
        )
    print(f"FE04E_DUPLICATE_EXECUTION_COUNT: {inventory['duplicate_total']}")
    print(f"FE04E_RAW_EXECUTION_TOTAL: {inventory['raw_total']}")
    print(f"FE04E_QUALIFYING_UNIQUE_TOTAL: {inventory['qualifying_total']}")
    print(f"FE04E_QUALIFYING_INVENTORY_SHA256: {inventory['digest']}")


def flatten(results: Mapping[str, Sequence[Violation]]) -> List[Violation]:
    return [violation for violations in results.values() for violation in violations]


def violation_message(violation: Violation) -> str:
    return (
        f'FAIL: [{violation.guard}] document={violation.document} '
        f'section="{violation.scope}" category="{violation.category}" '
        f'matched="{violation.excerpt}" expected="{violation.expected}"'
    )


def contract_outcome(
    documents: Mapping[str, str],
) -> Tuple[int, Dict[str, List[Violation]], str]:
    results = evaluate(documents)
    violations = flatten(results)
    output = "\n".join(violation_message(item) for item in violations)
    return (1 if violations else 0), results, output


def run_probes(
    base_documents: Mapping[str, str],
) -> Tuple[int, int, int, int, List[str]]:
    negative_passed = 0
    control_passed = 0
    failures = 0
    errors = 0
    messages: List[str] = []

    for probe in adversarial_probes():
        documents = dict(base_documents)
        try:
            probe.mutation(documents)
            exit_code, results, output = contract_outcome(documents)
        except Exception as exc:  # fail closed with a diagnostic, never count a crash as capture
            errors += 1
            messages.append(f"ERROR: adversarial probe [{probe.name}] crashed: {exc}")
            continue
        target = results.get(probe.expected_guard, ())
        target_marker = f"[{probe.expected_guard}]"
        category_matches = not probe.expected_category or any(
            item.category == probe.expected_category for item in target
        )
        tokens_match = all(token in output for token in probe.expected_tokens)
        invalid_diagnostic = re.search(
            r"\b(?:syntax error|missing governance source|traceback)\b",
            output,
            re.I,
        )
        if (
            exit_code == 1
            and target
            and target_marker in output
            and category_matches
            and tokens_match
            and not invalid_diagnostic
        ):
            negative_passed += 1
            messages.append(
                f"PASS: adversarial probe [{probe.name}] rejected by "
                f"[{probe.expected_guard}]"
            )
        else:
            failures += 1
            observed = sorted(name for name, items in results.items() if items)
            observed_text = ", ".join(observed) if observed else "none"
            messages.append(
                f"FAIL: adversarial probe [{probe.name}] did not produce the expected "
                f"contract failure from [{probe.expected_guard}]; exit={exit_code}; "
                f"expected category={probe.expected_category or 'any'}; "
                f"expected tokens={list(probe.expected_tokens)}; "
                f"observed guards=[{observed_text}]"
            )

    for probe in legal_control_probes():
        documents = dict(base_documents)
        try:
            probe.mutation(documents)
            exit_code, results, _ = contract_outcome(documents)
            validation_error = probe.validator(documents) if probe.validator else None
        except Exception as exc:
            errors += 1
            messages.append(f"ERROR: legal control probe [{probe.name}] crashed: {exc}")
            continue
        violations = flatten(results)
        if exit_code != 0 or violations or validation_error:
            failures += 1
            guards = ", ".join(sorted({item.guard for item in violations}))
            messages.append(
                f"FAIL: legal control probe [{probe.name}] was rejected by [{guards}]"
                f" validator={validation_error or 'pass'}"
            )
        else:
            control_passed += 1
            messages.append(f"PASS: legal control probe [{probe.name}] accepted")

    return negative_passed, control_passed, failures, errors, messages


def load_documents(root: Path) -> Dict[str, str]:
    documents: Dict[str, str] = {}
    for relative in REQUIRED_FILES:
        path = root / relative
        if not path.is_file():
            raise FileNotFoundError(f"missing governance source: {relative}")
        documents[relative] = path.read_text(encoding="utf-8")
    return documents


def print_violation(violation: Violation) -> None:
    print(violation_message(violation))


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--root", default=".")
    parser.add_argument(
        "--skip-probes",
        action="store_true",
        help="run scoped semantic guards without adversarial/control probes",
    )
    parser.add_argument("--static-assertions", type=int, default=0)
    parser.add_argument("--legacy-negative-probes", type=int, default=0)
    parser.add_argument("--skip-helper-unit-tests", action="store_true")
    parser.add_argument(
        "--helper-test-path",
        default=str(Path(__file__).with_name("test_check_fe04e_governance_semantics.py")),
    )
    args = parser.parse_args()

    try:
        documents = load_documents(Path(args.root).resolve())
    except (OSError, UnicodeError) as exc:
        print(f"ERROR: FE-04E semantic governance sources unavailable: {exc}")
        return 2

    results = evaluate(documents)
    violations = flatten(results)
    for guard in STATIC_GUARD_NAMES:
        if not results[guard]:
            print(f"PASS: semantic guard [{guard}]")
    for violation in violations:
        print_violation(violation)

    negative_total = 0
    negative_passed = 0
    control_total = 0
    control_passed = 0
    probe_failures = 0
    probe_errors = 0
    if not args.skip_probes:
        negative_total = len(adversarial_probes())
        control_total = len(legal_control_probes())
        (
            negative_passed,
            control_passed,
            probe_failures,
            probe_errors,
            probe_messages,
        ) = run_probes(documents)
        for message in probe_messages:
            print(message)

    print(f"FE04E_SEMANTIC_GUARDS: {len(SEMANTIC_GUARD_NAMES)}")
    # Compatibility key for callers pinned to the previous helper protocol.
    print(f"FE04E_SEMANTIC_STATIC_ASSERTIONS: {len(SEMANTIC_GUARD_NAMES)}")
    print(f"FE04E_CONTRADICTION_GUARDS: {len(CONTRADICTION_GUARDS)}")
    print(f"FE04E_AUTHORIZATION_SEMANTIC_GUARDS: {len(AUTHORIZATION_GUARDS)}")
    print(f"FE04E_CROSS_FILE_GUARDS: {len(CROSS_FILE_GUARDS)}")
    print(f"FE04E_ADVERSARIAL_PROBES: {negative_total}")
    print(f"FE04E_ADVERSARIAL_PROBES_PASSED: {negative_passed}")
    print(f"FE04E_LEGAL_CONTROL_PROBES: {control_total}")
    print(f"FE04E_LEGAL_CONTROL_PROBES_PASSED: {control_passed}")
    try:
        inventory = build_coverage_inventory(
            static_assertions=args.static_assertions,
            legacy_negative_probes=args.legacy_negative_probes,
            helper_test_path=Path(args.helper_test_path).resolve(),
            include_helper_unit_tests=not args.skip_helper_unit_tests,
            include_semantic_probes=not args.skip_probes,
        )
    except (ImportError, OSError, RuntimeError, AttributeError) as exc:
        print(f"ERROR: FE-04E qualifying coverage inventory unavailable: {exc}")
        probe_errors += 1
    else:
        print_coverage_inventory(inventory)
    print(f"FE04E_SEMANTIC_FAILURES: {len(violations) + probe_failures}")
    print(f"FE04E_SEMANTIC_ERRORS: {probe_errors}")

    if violations or probe_failures or probe_errors:
        print("FE04E_SEMANTIC_GOVERNANCE_FAILED")
        return 1
    print("FE04E_SEMANTIC_GOVERNANCE_OK")
    return 0


if __name__ == "__main__":
    sys.exit(main())
