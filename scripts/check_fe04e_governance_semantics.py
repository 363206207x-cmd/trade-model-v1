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
from pathlib import Path
from typing import Callable, Dict, Iterable, List, Mapping, MutableMapping, Sequence, Tuple


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

STATIC_GUARD_NAMES = CONTRADICTION_GUARDS + AUTHORIZATION_GUARDS + CROSS_FILE_GUARDS


@dataclass(frozen=True)
class Scope:
    document: str
    name: str
    text: str


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


def excerpt(text: str, limit: int = 220) -> str:
    value = re.sub(r"\s+", " ", text).strip()
    return value if len(value) <= limit else value[: limit - 3] + "..."


def extract_between(text: str, start: str, end: str) -> str:
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


def markdown_section_scope(
    documents: Mapping[str, str],
    document: str,
    name: str,
    start: str,
    end: str,
) -> Scope:
    return Scope(document, name, extract_between(documents[document], start, end))


def opportunity_scopes(documents: Mapping[str, str]) -> List[Scope]:
    semantic_window = extract_marker_window(
        documents[SEMANTIC],
        "Push Detail remains review-only and source-specific.",
        "Owner-scoped `POSITION_RISK` detail remains a separate",
    )
    return [
        Scope(SEMANTIC, "Message And Telegram V2 / public OPPORTUNITY", semantic_window),
        markdown_section_scope(
            documents,
            INTERACTION,
            "Overview Dashboard",
            "### 3.1 Overview Dashboard",
            "### 3.2 Evidence & Scoring",
        ),
        markdown_section_scope(
            documents,
            INTERACTION,
            "Mobile Push Detail",
            "### 3.10 Mobile Push Detail",
            "### 3.11 Mobile Profile & Settings",
        ),
        markdown_section_scope(
            documents,
            INTERACTION,
            "Mobile Message Center",
            "### 3.12 Mobile Message Center",
            "### 3.13 AI Analysis And Asset Search",
        ),
        markdown_section_scope(
            documents,
            STATE,
            "FE-04E Privacy/State Foundation And UI Readiness Boundary",
            "### FE-04E Privacy/State Foundation And UI Readiness Boundary",
            "## P3-U2 iPhone Private Test App Foundation",
        ),
        Scope(
            CHANGE_LOG,
            "v1.0-fe04e-privacy-state-foundation-effective-merged-main",
            extract_between(
                documents[CHANGE_LOG],
                "## v1.0-fe04e-privacy-state-foundation-effective-merged-main",
                "__END_OF_FILE__",
            )
            or documents[CHANGE_LOG].split(
                "## v1.0-fe04e-privacy-state-foundation-effective-merged-main", 1
            )[-1],
        ),
    ]


PRIVATE_ENTITY_PATTERNS = {
    "private Recheck": re.compile(r"\b(?:private\s+)?(?:push\s*recheck|recheck)\b", re.I),
    "private Push": re.compile(
        r"\b(?:private|internal)[\s_-]+push\b|"
        r"\bpush[\s_-]+status\b|\bpush[\s_-]*id\b",
        re.I,
    ),
    "private execution": re.compile(
        r"\bexecution[\s_-]+status\b|\bprivate[\s_-]+execution\b|"
        r"\bexecution[\s_-]+(?:completed|failed)\b",
        re.I,
    ),
    "UserPosition/private risk": re.compile(
        r"\buserposition\b|\bcurrent\s+user(?:'s)?\s+position\b|"
        r"\buser\s+scoped\s+context\b|\baccount\s+risk\b|"
        r"\bposition\s+risk\b|\bprivate\s+risk\b",
        re.I,
    ),
}

UNSAFE_RELATION = re.compile(
    r"\bmay\s+(?:be\s+)?(?:use|consult|supplement|influence|refine|participate)|"
    r"\bcan\s+(?:be\s+)?(?:use|consult|supplement|influence|refine|participate)|"
    r"\b(?:used|consulted|consumed)\s+as\b|\bauxiliary\b|\boptional\b|"
    r"\bfallback\b|\bwhen\s+available\b|\bmay\s+determine\b|"
    r"\b(?:influences?|determines?|refines?|supplements?|participates?|alters?|changes?)\b|"
    r"\brequired\s+for\b|\binput\s+(?:to|for)\b|\bfor\s+accuracy\b|"
    r"\bbest\s+effort\b",
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
    r"\bevaluation\b|\bstate\b|\bstatus\b|\bready\b|\berror\b",
    re.I,
)


def contradiction_units(scopes: Iterable[Scope]) -> Iterable[Tuple[Scope, str]]:
    for scope in scopes:
        if not scope.text:
            continue
        for unit in logical_units(scope.text):
            unit_normal = normalized(unit)
            if scope.document != SEMANTIC and not (
                "public" in unit_normal or "opportunity" in unit_normal
            ):
                continue
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
        r"\bauto[\s_-]*(?:notify|notification|send)\b",
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
    r"\bfuture\s+extension\s+only\b|\bpending\s+implementation\b|"
    r"\bpermitted\s+future\b.{0,80}\bcategories?\b|"
    r"\bnot\s+connected\b|\bno\s+delivery\b|\breadiness\s+gate\s+required\b|"
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
    return [
        markdown_section_scope(
            documents,
            SEMANTIC,
            "Message And Telegram V2",
            "## 7. Message And Telegram V2",
            "## 8. Search Asset V2",
        ),
        markdown_section_scope(
            documents,
            INTERACTION,
            "Product Identity and Safety Contract",
            "## 1. Product Identity and Safety Contract",
            "## 2. Page Navigation Structure",
        ),
        markdown_section_scope(
            documents,
            INTERACTION,
            "Mobile Push Detail",
            "### 3.10 Mobile Push Detail",
            "### 3.11 Mobile Profile & Settings",
        ),
        markdown_section_scope(
            documents,
            INTERACTION,
            "Mobile Message Center",
            "### 3.12 Mobile Message Center",
            "### 3.13 AI Analysis And Asset Search",
        ),
        markdown_section_scope(
            documents,
            INTERACTION,
            "Prohibited prototype behavior",
            "### 8.2 Prohibited prototype behavior",
            "## 9. Implementation Readiness",
        ),
        markdown_section_scope(
            documents,
            STATE,
            "FE-04E Privacy/State Foundation And UI Readiness Boundary",
            "### FE-04E Privacy/State Foundation And UI Readiness Boundary",
            "## P3-U2 iPhone Private Test App Foundation",
        ),
        Scope(MATRIX, "FE-04 delivery matrix row", matrix_row),
        Scope(CAPABILITY, "capability matrix frozen rows", capability_rows),
        markdown_section_scope(
            documents,
            DELIVERY,
            "Permanent Safety Rules",
            "## 4. Permanent Safety Rules / 永久安全规则",
            "## 5. Development Order Gate / 开发顺序总门禁",
        ),
    ]


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


def capability_in_text(capability: str, text: str) -> bool:
    return bool(CAPABILITY_PATTERNS[capability].search(normalized(text)))


def positive_assignment(capability: str, text: str) -> bool:
    capability_pattern = CAPABILITY_PATTERNS[capability]
    search_text = normalized(text)
    for match in capability_pattern.finditer(search_text):
        window_start = max(0, match.start() - 45)
        window_end = min(len(search_text), match.end() + 120)
        window = search_text[window_start:window_end]
        positive = POSITIVE_STATUS.search(search_text, match.end(), window_end)
        if not positive:
            continue
        relationship = search_text[match.end() : positive.end()]
        if not (
            ASSIGNMENT_CONNECTOR.search(relationship)
            or re.search(
                r"\b(?:authori[sz]e|enable|implement|activate|allow|support|permit)\w*\b",
                relationship,
                re.I,
            )
        ):
            continue
        polarity_window = window
        if SAFE_STATUS.search(polarity_window) and not re.search(
            r"\b(?:but|however|except|unless)\b", polarity_window, re.I
        ):
            continue
        return True
    return False


def relevant_yaml_path(capability: str, path: str, value: str) -> bool:
    path_text = normalized(path)
    value_text = normalized(value)
    return capability_in_text(capability, path_text) or capability_in_text(
        capability, value_text
    )


def yaml_authorized(capability: str, path: str, value: str) -> bool:
    if not relevant_yaml_path(capability, path, value):
        return False
    combined = f"{path}: {value}"
    value_normal = normalized(value)
    path_normal = normalized(path)
    if SAFE_STATUS.search(combined):
        return False
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
    }:
        return True
    if POSITIVE_STATUS.search(value) and positive_assignment(capability, combined):
        return True
    if value_normal in {"true", "yes", "on"} and re.search(
        r"\b(?:authorized|enabled|active|allowed|implemented|send|delivery)\b",
        path_normal,
    ):
        return True
    return False


def authorization_violations(
    documents: Mapping[str, str], capability: str, guard: str
) -> List[Violation]:
    violations: List[Violation] = []
    for document in (ACTIVE, NEXT_TASK):
        for path, value, raw in yaml_entries(documents[document]):
            if (
                capability != "trading"
                and capability_in_text(capability, path)
                and "capability movement" in normalized(path)
                and normalized(value) != "none"
            ):
                violations.append(
                    Violation(
                        guard,
                        document,
                        path,
                        f"{capability} capability movement",
                        excerpt(raw),
                        f"{capability} capability movement must remain NONE",
                    )
                )
                continue
            if yaml_authorized(capability, path, value):
                violations.append(
                    Violation(
                        guard,
                        document,
                        path or "<list item>",
                        f"{capability} authorization",
                        excerpt(raw),
                        f"{capability} must remain not implemented and not authorized",
                    )
                )
    for scope in selected_markdown_scopes(documents):
        if not scope.text:
            continue
        for unit in logical_units(scope.text):
            movement = re.search(
                r"capability[\s_-]+movement"
                r"\s*(?::|=|\||—|-|\bis\b)\s*([A-Za-z0-9_-]+)",
                unit,
                re.I,
            )
            if (
                capability != "trading"
                and capability_in_text(capability, unit)
                and movement
                and normalized(movement.group(1)) != "none"
            ):
                violations.append(
                    Violation(
                        guard,
                        scope.document,
                        scope.name,
                        f"{capability} capability movement",
                        excerpt(unit),
                        f"{capability} capability movement must remain NONE",
                    )
                )
                continue
            if positive_assignment(capability, unit):
                violations.append(
                    Violation(
                        guard,
                        scope.document,
                        scope.name,
                        f"{capability} authorization",
                        excerpt(unit),
                        f"{capability} must remain not implemented and not authorized",
                    )
                )
    return violations


def trading_movement_violations(documents: Mapping[str, str]) -> List[Violation]:
    guard = "trading capability movement guard"
    violations: List[Violation] = []
    movement_key = re.compile(r"\btrading[\s_-]*capability[\s_-]*movement\b", re.I)
    for document in (ACTIVE, NEXT_TASK):
        for path, value, raw in yaml_entries(documents[document]):
            if not movement_key.search(path):
                continue
            if normalized(value) != "none":
                violations.append(
                    Violation(
                        guard,
                        document,
                        path,
                        "trading capability movement",
                        excerpt(raw),
                        "TRADING_CAPABILITY_MOVEMENT must remain NONE",
                    )
                )
    for scope in selected_markdown_scopes(documents):
        for unit in logical_units(scope.text):
            if not movement_key.search(unit):
                continue
            match = re.search(
                r"trading[\s_-]*capability[\s_-]*movement"
                r"\s*(?::|=|\||—|-|\bis\b)\s*([A-Za-z0-9_-]+)",
                unit,
                re.I,
            )
            if match and normalized(match.group(1)) != "none":
                violations.append(
                    Violation(
                        guard,
                        scope.document,
                        scope.name,
                        "trading capability movement",
                        excerpt(unit),
                        "TRADING_CAPABILITY_MOVEMENT must remain NONE",
                    )
                )
    return violations


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


def next_task_violations(documents: Mapping[str, str]) -> List[Violation]:
    guard = "next-task capability authorization guard"
    violations: List[Violation] = []
    target_paths = {
        "module",
        "next_business_phase",
        "next_allowed_action",
        "next_required_action",
        "allowed_scope",
    }
    forbidden_target = re.compile(
        r"\b(?:message\s*(?:center|push)\s*ui|push\s*detail\s*ui|telegram|"
        r"system\s*notification|external\s*notification|automatic\s*notification|"
        r"trading|order\s*execution)\b",
        re.I,
    )
    delivery_action = re.compile(
        r"\b(?:implementation|implement|delivery|send|enable|activate|execute)\b", re.I
    )
    for path, value, raw in yaml_entries(documents[NEXT_TASK]):
        leaf = path.split(".")[-1]
        if leaf not in target_paths:
            continue
        if forbidden_target.search(value) and delivery_action.search(value):
            if re.search(r"\b(?:readiness|governance|inspect|re-evaluat)\b", value, re.I):
                continue
            if SAFE_STATUS.search(value):
                continue
            violations.append(
                Violation(
                    guard,
                    NEXT_TASK,
                    path,
                    "next-task capability authorization",
                    excerpt(raw),
                    "next task must remain read-only readiness/governance re-evaluation",
                )
            )
    return violations


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


def mutation_before(document: str, marker: str, text: str) -> Callable[[MutableMapping[str, str]], None]:
    return lambda documents: insert_before(documents, document, marker, text)


def mutation_append(document: str, text: str) -> Callable[[MutableMapping[str, str]], None]:
    return lambda documents: append_text(documents, document, text)


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

    print(f"FE04E_SEMANTIC_STATIC_ASSERTIONS: {len(STATIC_GUARD_NAMES)}")
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
