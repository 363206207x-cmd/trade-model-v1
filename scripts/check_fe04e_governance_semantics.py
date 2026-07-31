#!/usr/bin/env python3
"""Scoped semantic guards for the FE-04E governance contract.

This helper intentionally reads only the frozen FE-04E governance surfaces.
It complements the shell contract checker with contradiction detection and
case-insensitive authorization-state checks for Markdown and YAML-shaped text.
"""

from __future__ import annotations

import argparse
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


def public_opportunity_segment(text: str) -> str:
    """Keep only the public OPPORTUNITY domain inside a mixed Message section."""

    start = re.search(
        r"(?im)^(?:Push Detail remains|Authenticated shared.*OPPORTUNITY|"
        r"Public [`']?OPPORTUNITY)",
        text,
    )
    if not start:
        return text
    end = re.search(
        r"(?im)^(?:(?:[-*]\s*)?Owner[\s-]*scoped\s+[`']?POSITION_RISK[`']?|"
        r"(?:[-*]\s*)?[`']?POSITION_RISK[`']?\s+is\s+[`']?OWNER_SCOPED)",
        text[start.start() :],
    )
    end_index = len(text) if not end else start.start() + end.start()
    return text[start.start() : end_index].strip()


def opportunity_scopes(documents: Mapping[str, str]) -> List[Scope]:
    scopes: List[Scope] = []
    for scope in markdown_alias_scopes(
        documents,
        SEMANTIC,
        "Message/Telegram public OPPORTUNITY",
        ("message telegram", "telegram authorization", "telegram status", "通知 telegram"),
    ):
        scopes.append(Scope(scope.document, scope.name, public_opportunity_segment(scope.text)))
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
        markdown_alias_scopes(documents, STATE, "FE-04E public OPPORTUNITY boundary", ("fe 04e privacy state", "fe 04e privacy"))
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
        r"\bexecution[\s_-]+status\b|\bprivate[\s_-]+execution\b|"
        r"\bexecution[\s_-]+(?:result|completion|completed|failed|pending)\b|"
        r"\b(?:completed|failed|pending)\b|\bexecuted\b|"
        r"\bcompletion[\s_-]+state\b",
        re.I,
    ),
    "UserPosition/private risk": re.compile(
        r"\buserposition\b|\bcurrent\s+user(?:'s)?\s+position\b|"
        r"\buser\s+scoped\s+context\b|\baccount\s+risk\b|"
        r"\bposition\s+risk\b|\bprivate\s+risk\b|"
        r"\b(?:long|short|flat|no[\s-]*position)\b|"
        r"\bposition\s+direction\b|\bholding\s+state\b|"
        r"\bcaller\s+position\b|\buser\s+exposure\b|"
        r"多仓|空仓|无仓",
        re.I,
    ),
}

UNSAFE_RELATION = re.compile(
    r"\bmay\s+(?:be\s+)?(?:use|consult|supplement|influence|refine|participate)|"
    r"\bcan\s+(?:be\s+)?(?:use|consult|supplement|influence|refine|participate)|"
    r"\b(?:used|consulted|consumed)\s+as\b|\bauxiliary\b|\boptional\b|"
    r"\bfallback\b|\bwhen\s+available\b|\bmay\s+determine\b|"
    r"\b(?:influences?|determines?|refines?|supplements?|participates?|alters?|changes?|"
    r"affects?|gates?|produces?)\b|"
    r"\bmaps?\b.{0,60}\bto\b|\bwaits?\s+for\b|"
    r"\brequired\s+for\b|\binput\s+(?:to|for)\b|\bfor\s+accuracy\b|"
    r"\bused\s+(?:internally|in|to)\b|\bbest\s+effort\b",
    re.I,
)

WEAKENING_RELATION = re.compile(
    r"\bunless\b|\bexcept\b|\bhowever\b|\bauxiliary\b|\boptional\b|"
    r"\bfallback\b|\bwhen\s+available\b|\bmay\s+consult\b|"
    r"\bmay\s+supplement\b|\bmay\s+be\s+used\b|\bcan\s+be\s+used\b|"
    r"\binternal\s+only\b|\bnot\s+(?:exposed|returned|serialized)\s+but\b|"
    r"\bhidden\s+but\b|\bfor\s+accuracy\b|\bbest\s+effort\b",
    re.I,
)

SAFE_PROHIBITION = re.compile(
    r"\bmust\s+not\b|\bnever\b|\bcannot\b|\b(?:does|do|may|can)\s+not\b|"
    r"\bnot\s+(?:used|consulted|read|consumed|required|allowed|permitted|authorized)\b|"
    r"\b(?:forbidden|prohibited|excluded|neither)\b|"
    r"\bno\s+(?:private|internal|userposition|account\s+risk|position\s+risk)\b",
    re.I,
)

PUBLIC_STATE_CONTEXT = re.compile(
    r"\bpublic\b|\bopportunity\b|\breadiness\b|\blifecycle\b|"
    r"\bevaluation\b|\bresult\b|\bvisibility\b|\bstate\b|\bstatus\b|"
    r"\bready\b|\bpartial\b|\berror\b",
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
    if not PUBLIC_STATE_CONTEXT.search(unit):
        return False
    if not UNSAFE_RELATION.search(unit):
        return False
    if WEAKENING_RELATION.search(unit):
        return True
    return not SAFE_PROHIBITION.search(unit)


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
                        category,
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

ASSIGNMENT_CONNECTOR = re.compile(
    r"(?:\bstatus\b|\bauthori[sz]ation\b|\bcapability\b|\bstate\b|"
    r"\bimplementation\b|\bdelivery\b|\bsend\b)?"
    r"\s*(?::|=|\||—|-|\bis\b|\bare\b|\bremains?\b)\s*",
    re.I,
)


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
        (SEMANTIC, "Message And Telegram V2", ("message telegram", "telegram authorization", "telegram status", "通知 telegram")),
        (INTERACTION, "Product Identity and Safety Contract", ("product identity safety contract", "safety contract")),
        (INTERACTION, "Mobile Push Detail", ("mobile push detail", "push detail")),
        (INTERACTION, "Mobile Message Center", ("mobile message center", "message center")),
        (INTERACTION, "Prohibited prototype behavior", ("prohibited prototype behavior",)),
        (STATE, "FE-04E Privacy/State Foundation", ("fe 04e privacy state", "fe 04e privacy")),
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
        ("message telegram", "telegram authorization", "telegram status", "通知 telegram"),
        re.compile(r"\btelegram\b", re.I),
    ),
    ScopeRequirement(
        "system notification",
        SEMANTIC,
        ("old new contract difference", "contract difference"),
        re.compile(r"\bsystem[\s-]*notifications?\b", re.I),
    ),
    ScopeRequirement(
        "external notification",
        STATE,
        ("fe 04e privacy state", "fe 04e privacy"),
        re.compile(r"\bexternal[\s-]*(?:notification|send|delivery)", re.I),
    ),
    ScopeRequirement(
        "automatic notification",
        STATE,
        ("fe 04e privacy state", "fe 04e privacy"),
        re.compile(r"\bautomatic[\s-]*notification", re.I),
    ),
    ScopeRequirement(
        "trading capability",
        STATE,
        ("fe 04e privacy state", "fe 04e privacy"),
        re.compile(r"\btrading\s+capability\b", re.I),
    ),
    ScopeRequirement(
        "Public OPPORTUNITY contract",
        SEMANTIC,
        ("message telegram", "telegram authorization", "telegram status", "通知 telegram"),
        re.compile(r"\bOPPORTUNITY\b", re.I),
    ),
    ScopeRequirement(
        "POSITION_RISK contract",
        SEMANTIC,
        ("message telegram", "telegram authorization", "telegram status", "通知 telegram"),
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
    """Detect positive capability authorization without allowing safe masking."""

    capability_pattern = CAPABILITY_PATTERNS[capability]
    search_text = normalized(text)
    for match in capability_pattern.finditer(search_text):
        window_end = min(len(search_text), match.end() + 160)
        for positive in POSITIVE_STATUS.finditer(search_text, match.end(), window_end):
            if is_negated(search_text, positive.start()):
                continue
            relationship = search_text[match.end() : positive.start()]
            if (
                ASSIGNMENT_CONNECTOR.search(relationship)
                or len(relationship.split()) <= 4
                or re.search(
                    r"\b(?:authori[sz]e|enable|implement|activate|allow|support|permit)\w*\b",
                    relationship,
                    re.I,
                )
            ):
                return True
        prefix = search_text[max(0, match.start() - 90) : match.start()]
        command = re.search(
            r"\b(?:authori[sz]e|enable|implement|activate|allow|support)\w*\b"
            r"\s+(?:the\s+)?$",
            prefix,
            re.I,
        )
        if command and not is_negated(prefix, command.start()):
            return True
    return False


def status_declarations_for_text(
    capability: str,
    text: str,
    document: str = "<memory>",
    scope: str = "<statement>",
    line: int = 0,
) -> List[StatusDeclaration]:
    """Classify every explicit capability status in one prose/table statement."""

    if not capability_in_text(capability, text):
        return []
    declarations: List[StatusDeclaration] = []
    if SAFE_STATUS.search(normalized(text)):
        declarations.append(
            StatusDeclaration(
                capability,
                StatusClass.SAFE,
                "SAFE",
                document,
                scope,
                excerpt(text),
                line,
            )
        )
    if dangerous_assignment(capability, text):
        declarations.append(
            StatusDeclaration(
                capability,
                StatusClass.DANGEROUS,
                "DANGEROUS",
                document,
                scope,
                excerpt(text),
                line,
            )
        )
    explicit_status = re.search(
        r"\b(?:status|authori[sz]ation|capability|movement|implementation|"
        r"enabled|active|auto[\s_-]*send)\b\s*(?::|=|\||\bis\b|\bare\b)",
        normalized(text),
        re.I,
    )
    if explicit_status and not declarations:
        declarations.append(
            StatusDeclaration(
                capability,
                StatusClass.UNKNOWN,
                "UNKNOWN",
                document,
                scope,
                excerpt(text),
                line,
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
    value_normal = normalized(value)
    path_normal = normalized(path)
    if not value_normal:
        return None
    if value_normal in {"0", "false", "no", "off", "none"}:
        return StatusClass.SAFE
    if SAFE_STATUS.search(normalized(f"{path}: {value}")):
        return StatusClass.SAFE
    if value_normal in {
        "authorized",
        "enabled",
        "implemented",
        "active",
        "allowed",
        "available",
        "supported",
        "ready",
        "true",
        "yes",
        "on",
        "granted",
        "permitted",
        "limited",
        "expanded",
        "partial",
        "read only plus",
    }:
        return StatusClass.DANGEROUS
    if value_normal in {"true", "yes", "on"} and re.search(
        r"\b(?:authorized|enabled|active|allowed|implemented|send|delivery)\b",
        path_normal,
    ):
        return StatusClass.DANGEROUS
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
        pieces = [part.strip() for part in re.split(r"[;；]", value) if part.strip()]
        if not pieces and value:
            pieces = [value]
        for piece in pieces:
            classification = classify_yaml_value(path, piece) if path_has_capability else None
            if classification is not None:
                declarations.append(
                    StatusDeclaration(
                        capability,
                        classification,
                        normalized(piece) or "<empty>",
                        document,
                        path or "<list item>",
                        excerpt(raw),
                    )
                )
                continue
            prose_units = statement_units(piece) or [piece]
            for prose_unit in prose_units:
                statement = (
                    f"{capability} {prose_unit}" if path_has_capability else prose_unit
                )
                declarations.extend(
                    status_declarations_for_text(
                        capability,
                        statement,
                        document,
                        path or "<list item>",
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
            declaration.document,
            declaration.scope,
            declaration.excerpt,
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
        category = "UNKNOWN_OR_AMBIGUOUS_AUTHORIZATION"
    else:
        category = f"{capability} authorization"
    values = "; ".join(
        f"{item.classification.value}:{item.value}@{item.document}:{item.scope}"
        for item in unique
    )
    first = (dangerous or unknown)[0]
    return [
        Violation(
            guard,
            first.document,
            first.scope,
            category,
            excerpt(f"capability={capability}; all detected values=[{values}]", 700),
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
    r"\s*(?::|=|\||—|-|\bis\b)\s*([A-Za-z0-9_-]+)",
    re.I,
)


def trading_movement_values(
    text: str, document: str = "<memory>", scope: str = "trading movement"
) -> List[StatusDeclaration]:
    declarations: List[StatusDeclaration] = []
    for match in TRADING_MOVEMENT_PATTERN.finditer(text):
        value = normalized(match.group(1))
        line = text.count("\n", 0, match.start()) + 1
        declarations.append(
            StatusDeclaration(
                "trading capability movement",
                StatusClass.SAFE if value == "none" else StatusClass.DANGEROUS,
                value,
                document,
                scope,
                excerpt(match.group(0)),
                line,
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


def mutation_before(document: str, marker: str, text: str) -> Callable[[MutableMapping[str, str]], None]:
    return lambda documents: insert_before(documents, document, marker, text)


def mutation_append(document: str, text: str) -> Callable[[MutableMapping[str, str]], None]:
    return lambda documents: append_text(documents, document, text)


def mutation_replace(
    document: str, old: str, new: str
) -> Callable[[MutableMapping[str, str]], None]:
    return lambda documents: replace_once(documents, document, old, new)


def adversarial_probes() -> List[Probe]:
    semantic_end = "Owner-scoped `POSITION_RISK` detail remains a separate"
    return [
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
        # Heading discovery must inspect every alias-matched section and fail closed.
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
            "owner-scoped POSITION_RISK private Recheck control",
            SEMANTIC,
            mutation_before(
                SEMANTIC,
                "## 8. Search Asset V2",
                "Private Recheck is permitted only inside owner-scoped POSITION_RISK state resolution.",
            ),
            "",
        ),
    ]


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
        invalid_diagnostic = re.search(
            r"\b(?:syntax error|missing governance source|traceback)\b",
            output,
            re.I,
        )
        if (
            exit_code == 1
            and target
            and target_marker in output
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
                f"observed guards=[{observed_text}]"
            )

    for probe in legal_control_probes():
        documents = dict(base_documents)
        try:
            probe.mutation(documents)
            exit_code, results, _ = contract_outcome(documents)
        except Exception as exc:
            errors += 1
            messages.append(f"ERROR: legal control probe [{probe.name}] crashed: {exc}")
            continue
        violations = flatten(results)
        if exit_code != 0 or violations:
            failures += 1
            guards = ", ".join(sorted({item.guard for item in violations}))
            messages.append(
                f"FAIL: legal control probe [{probe.name}] was rejected by [{guards}]"
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
    print(f"FE04E_SEMANTIC_FAILURES: {len(violations) + probe_failures}")
    print(f"FE04E_SEMANTIC_ERRORS: {probe_errors}")

    if violations or probe_failures or probe_errors:
        print("FE04E_SEMANTIC_GOVERNANCE_FAILED")
        return 1
    print("FE04E_SEMANTIC_GOVERNANCE_OK")
    return 0


if __name__ == "__main__":
    sys.exit(main())
