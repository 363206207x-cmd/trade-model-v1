#!/usr/bin/env python3
"""Validate and canonicalize sanitized P3-H LAB evidence contracts."""

from __future__ import annotations

import argparse
import os
import re
import stat
import sys
import tempfile
from collections import defaultdict
from pathlib import Path
from typing import Callable


MAX_INPUT_BYTES = 1024 * 1024
MAX_LINE_BYTES = 512
SAFE_VALUE = re.compile(r"^[A-Za-z0-9._,+-]+$")
SAFE_TOKEN = re.compile(r"^[A-Z0-9_]+$")
HEX_40 = re.compile(r"^[0-9a-f]{40}$")
HEX_64 = re.compile(r"^[0-9a-f]{64}$")
VERSION_TOKEN = re.compile(r"^[A-Za-z0-9][A-Za-z0-9._,+-]*$")


class ContractFailure(Exception):
    """A sanitized evidence-contract failure."""

    def __init__(self, category: str) -> None:
        super().__init__(category)
        self.category = category


def fixed(expected: str) -> Callable[[str], bool]:
    return lambda value: value == expected


def token(value: str) -> bool:
    return bool(VERSION_TOKEN.fullmatch(value))


def positive_integer(value: str) -> bool:
    return value.isdigit() and int(value) > 0


def nonnegative_integer(value: str) -> bool:
    return value.isdigit()


PREFLIGHT_RULES: list[tuple[str, Callable[[str], bool]]] = [
    ("REMOTE_PREFLIGHT", fixed("PASS")),
    ("LINUX_DISTRIBUTION", token),
    ("KERNEL_RELEASE", token),
    ("CPU_ARCHITECTURE", token),
    ("SYSTEMD_VERSION", token),
    ("DOCKER_ENGINE_VERSION", token),
    ("DOCKER_COMPOSE_VERSION", token),
    ("OPENSSL_VERSION", token),
    ("TIMEZONE", fixed("UTC")),
    ("TIME_SYNCHRONIZED", fixed("YES")),
    ("SECRET_BACKEND_CLASS", fixed("SYSTEMD_CREDENTIALS")),
    ("SECRET_MOUNT", fixed("RUNTIME_ONLY")),
    ("SECRET_MOUNT_RUNTIME_VERIFICATION", fixed("PASS_BACKEND_BOUND")),
    ("SECRET_MOUNT_FILESYSTEM", lambda value: value in {"tmpfs", "ramfs"}),
    ("SECRET_FILE_CONTRACT", fixed("PASS_NAMES_OWNERS_PERMISSIONS")),
    ("SUDO_NONINTERACTIVE", fixed("AVAILABLE")),
    ("AVAILABLE_DISK_KB", positive_integer),
    ("AVAILABLE_MEMORY_KB", positive_integer),
]


ACTION_RULES: dict[str, list[tuple[str, Callable[[str], bool]]]] = {
    "BUILD_APPLICATION_IMAGE": [
        ("P3H_REMOTE_STAGE", fixed("APPLICATION_IMAGE_BUILD_PASS")),
        ("P3H_IMAGE_BUILD_ATTEMPTS", fixed("1")),
        ("P3H_IMAGE_BUILD_RETRY_COUNT", fixed("0")),
        ("APP_IMAGE_REVISION", lambda value: bool(HEX_40.fullmatch(value))),
    ],
    "PULL_RUNTIME_IMAGES": [
        ("P3H_REMOTE_STAGE", fixed("RUNTIME_IMAGE_PULL_PASS")),
        ("P3H_RUNTIME_IMAGE_PREFETCH", fixed("PASS_3_OF_3")),
    ],
    "INITIAL_DEPLOY": [
        ("P3H_REMOTE_STAGE", fixed("INITIAL_DEPLOY_PASS")),
        ("STAGING_FLYWAY", fixed("PASS_V1_TO_V7")),
        ("FLYWAY_REPEAT", fixed("ZERO_MIGRATIONS")),
        ("APPLICATION_DATABASE_ROLE", fixed("READ_ONLY")),
        ("READ_ONLY_WRITE_PROBE", fixed("DENIED")),
        ("TLS_1_2", fixed("PASS")),
        ("TLS_1_3", lambda value: value in {"PASS", "CLIENT_UNSUPPORTED_WITH_EVIDENCE"}),
        ("HTTP_TO_HTTPS_REDIRECT", fixed("PASS")),
        ("UNKNOWN_HOST", fixed("REJECTED")),
        ("UNAUTHENTICATED_API", fixed("DENIED")),
        ("AUTHENTICATED_DASHBOARD", fixed("PASS")),
        ("EMPTY_DASHBOARD_FAIL_CLOSED", fixed("PASS")),
        ("RATE_LIMIT", fixed("PASS_429")),
    ],
    "BACKUP_RESTORE": [
        ("P3H_REMOTE_STAGE", fixed("BACKUP_RESTORE_PASS")),
        ("PROD_BACKUP_SCRIPT", fixed("PASS")),
        ("PROD_RESTORE_SCRIPT", fixed("PASS")),
        ("RESTORE_SCHEMA", fixed("MATCH")),
        ("RESTORE_CONTENT", fixed("MATCH")),
    ],
    "ROTATE": [
        ("P3H_REMOTE_STAGE", fixed("ROTATION_PASS")),
        ("ADMIN_SECRET_ROTATION", fixed("PASS_V2_ACTIVE_V1_DENIED")),
        ("DATABASE_SECRET_ROTATION", fixed("PASS_V2_ACTIVE_V1_DENIED")),
        ("TLS_ROTATION", fixed("PASS")),
        ("SERVICE_RESTART", fixed("PASS")),
    ],
    "POST_REBOOT_VERIFY": [
        ("P3H_REMOTE_STAGE", fixed("POST_REBOOT_PASS")),
        ("VM_REBOOT_STATUS", fixed("PASS_ACTUAL_LINUX_VM_REBOOT")),
        ("V2_DATABASE_AFTER_REBOOT", fixed("PASS")),
        ("V1_DATABASE_AFTER_REBOOT", fixed("DENIED")),
        ("V2_ADMIN_AFTER_REBOOT", fixed("PASS")),
        ("V1_ADMIN_AFTER_REBOOT", fixed("DENIED")),
        ("POST_REBOOT_CONTENT_FINGERPRINT", fixed("MATCH")),
        ("SECRET_LEAK_CANDIDATE_COUNT", fixed("0")),
        ("PROVIDER_EXTERNAL_CALLS", fixed("DISABLED")),
        ("AI_EXTERNAL_CALLS", fixed("DISABLED")),
        ("SCHEDULERS", fixed("DISABLED")),
        ("TRADING", fixed("DISABLED")),
        ("P3H_REMOTE_EXECUTION_IMPLEMENTATION", fixed("PASS_LOCAL_VM")),
        ("REAL_EXTERNAL_STAGING_STATUS", fixed("NOT_RUN")),
        ("P3H_RESULT", fixed("PARTIAL_LOCAL_VM_EVIDENCE")),
        ("P4_ALLOWED", fixed("NO")),
        ("PRODUCTION_READINESS", fixed("BLOCKED")),
    ],
    "CLEANUP": [
        ("P3H_REMOTE_STAGE", fixed("CLEANUP_PASS")),
    ],
}


ACTION_OPTIONAL_RULES: dict[str, Callable[[str], bool]] = {
    "P3H_LAB_STAGE": lambda value: bool(SAFE_TOKEN.fullmatch(value)),
    "STAGE_ELAPSED_SECONDS": nonnegative_integer,
    "GLOBAL_ELAPSED_SECONDS": nonnegative_integer,
    "PROCESS_STATE": lambda value: bool(SAFE_TOKEN.fullmatch(value)),
    "DOCKER_OPERATION_CLASS": lambda value: bool(SAFE_TOKEN.fullmatch(value)),
    "P3H_PROGRESS_PROBE_STATUS": lambda value: bool(SAFE_TOKEN.fullmatch(value)),
    "VM_AVAILABLE_MEMORY_MB": positive_integer,
    "VM_AVAILABLE_DISK_GB": positive_integer,
    "DOCKER_DAEMON": fixed("ACTIVE"),
    "DNS_RESOLUTION": fixed("PASS"),
    "REQUIRED_REGISTRY_CONNECTIVITY": fixed("PASS_BOUNDED"),
    "MAVEN_REPOSITORY_CONNECTIVITY": fixed("PASS_BOUNDED"),
}


REPEATABLE_ACTION_KEYS = {
    "P3H_LAB_STAGE",
    "STAGE_ELAPSED_SECONDS",
    "GLOBAL_ELAPSED_SECONDS",
    "PROCESS_STATE",
    "DOCKER_OPERATION_CLASS",
    "P3H_PROGRESS_PROBE_STATUS",
}


FINAL_UNIQUE_RULES: list[tuple[str, Callable[[str], bool]]] = (
    PREFLIGHT_RULES
    + [
        ("SOURCE_ARCHIVE_SHA256", lambda value: bool(HEX_64.fullmatch(value))),
        ("SOURCE_ARCHIVE_REMOTE_SHA256", lambda value: bool(HEX_64.fullmatch(value))),
    ]
    + [rule for action in ACTION_RULES.values() for rule in action if rule[0] != "P3H_REMOTE_STAGE"]
    + [("RESOURCE_CLEANUP", fixed("PASS"))]
)


FINAL_STAGE_VALUES = [
    "APPLICATION_IMAGE_BUILD_PASS",
    "RUNTIME_IMAGE_PULL_PASS",
    "INITIAL_DEPLOY_PASS",
    "BACKUP_RESTORE_PASS",
    "ROTATION_PASS",
    "POST_REBOOT_PASS",
    "CLEANUP_PASS",
]


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(add_help=False)
    parser.add_argument("--contract", choices=("preflight", "action", "final"), required=True)
    parser.add_argument("--action", choices=tuple(ACTION_RULES))
    parser.add_argument("--source-head")
    parser.add_argument("--input-file", required=True)
    parser.add_argument("--output-file", required=True)
    parser.add_argument("--status-key", required=True)
    args = parser.parse_args()
    if args.contract == "action" and not args.action:
        parser.error("action contract requires an action")
    if args.contract in {"action", "final"}:
        if not args.source_head or not HEX_40.fullmatch(args.source_head):
            parser.error("action and final contracts require a source head")
    return args


def read_lines(path: Path) -> list[tuple[str, str]]:
    try:
        metadata = path.lstat()
    except OSError as exc:
        raise ContractFailure("BLOCKED_INVALID_VALUE") from exc
    if stat.S_ISLNK(metadata.st_mode) or not stat.S_ISREG(metadata.st_mode):
        raise ContractFailure("BLOCKED_INVALID_VALUE")
    if metadata.st_size > MAX_INPUT_BYTES:
        raise ContractFailure("BLOCKED_OVERSIZED_VALUE")
    try:
        payload = path.read_bytes()
    except OSError as exc:
        raise ContractFailure("BLOCKED_INVALID_VALUE") from exc
    if len(payload) > MAX_INPUT_BYTES:
        raise ContractFailure("BLOCKED_OVERSIZED_VALUE")
    if any(byte < 32 and byte not in (10,) for byte in payload) or 127 in payload:
        raise ContractFailure("BLOCKED_CONTROL_CHARACTER")
    try:
        text = payload.decode("ascii")
    except UnicodeDecodeError as exc:
        raise ContractFailure("BLOCKED_INVALID_VALUE") from exc

    parsed: list[tuple[str, str]] = []
    for raw_line in text.split("\n"):
        if not raw_line:
            continue
        if len(raw_line.encode("ascii")) > MAX_LINE_BYTES:
            raise ContractFailure("BLOCKED_OVERSIZED_VALUE")
        if raw_line.count(": ") != 1:
            raise ContractFailure("BLOCKED_INVALID_VALUE")
        key, value = raw_line.split(": ", 1)
        if not SAFE_TOKEN.fullmatch(key) or not value or not SAFE_VALUE.fullmatch(value):
            raise ContractFailure("BLOCKED_INVALID_VALUE")
        parsed.append((key, value))
    return parsed


def group_lines(lines: list[tuple[str, str]]) -> dict[str, list[str]]:
    grouped: dict[str, list[str]] = defaultdict(list)
    for key, value in lines:
        grouped[key].append(value)
    return grouped


def require_unique(
        grouped: dict[str, list[str]],
        rules: list[tuple[str, Callable[[str], bool]]],
) -> list[tuple[str, str]]:
    canonical: list[tuple[str, str]] = []
    for key, validator in rules:
        values = grouped.get(key, [])
        if not values:
            raise ContractFailure("BLOCKED_MISSING_REQUIRED_KEY")
        if len(values) != 1:
            raise ContractFailure("BLOCKED_DUPLICATE_KEY")
        if not validator(values[0]):
            raise ContractFailure("BLOCKED_INVALID_VALUE")
        canonical.append((key, values[0]))
    return canonical


def validate_preflight(lines: list[tuple[str, str]]) -> list[tuple[str, str]]:
    grouped = group_lines(lines)
    allowed = {key for key, _validator in PREFLIGHT_RULES}
    if set(grouped) - allowed:
        raise ContractFailure("BLOCKED_UNKNOWN_KEY")
    return require_unique(grouped, PREFLIGHT_RULES)


def validate_action(
        lines: list[tuple[str, str]], action: str, source_head: str
) -> list[tuple[str, str]]:
    grouped = group_lines(lines)
    required_rules = ACTION_RULES[action]
    allowed = {key for key, _validator in required_rules} | set(ACTION_OPTIONAL_RULES)
    if set(grouped) - allowed:
        raise ContractFailure("BLOCKED_UNKNOWN_KEY")
    canonical = require_unique(grouped, required_rules)
    for key, values in grouped.items():
        if key not in ACTION_OPTIONAL_RULES:
            continue
        if key not in REPEATABLE_ACTION_KEYS and len(values) != 1:
            raise ContractFailure("BLOCKED_DUPLICATE_KEY")
        if any(not ACTION_OPTIONAL_RULES[key](value) for value in values):
            raise ContractFailure("BLOCKED_INVALID_VALUE")
    if action == "BUILD_APPLICATION_IMAGE":
        revision = grouped["APP_IMAGE_REVISION"][0]
        if revision != source_head:
            raise ContractFailure("BLOCKED_INVALID_VALUE")
    return canonical


def validate_final(
        lines: list[tuple[str, str]], source_head: str
) -> list[tuple[str, str]]:
    grouped = group_lines(lines)
    allowed = {key for key, _validator in FINAL_UNIQUE_RULES} | {"P3H_REMOTE_STAGE"}
    if set(grouped) - allowed:
        raise ContractFailure("BLOCKED_UNKNOWN_KEY")
    canonical = require_unique(grouped, FINAL_UNIQUE_RULES)

    stage_values = grouped.get("P3H_REMOTE_STAGE", [])
    for expected in FINAL_STAGE_VALUES:
        count = stage_values.count(expected)
        if count == 0:
            raise ContractFailure("BLOCKED_MISSING_REQUIRED_KEY")
        if count > 1:
            raise ContractFailure("BLOCKED_DUPLICATE_KEY")
    if len(stage_values) != len(FINAL_STAGE_VALUES):
        raise ContractFailure("BLOCKED_INVALID_VALUE")

    values = dict(canonical)
    if values["SOURCE_ARCHIVE_SHA256"] != values["SOURCE_ARCHIVE_REMOTE_SHA256"]:
        raise ContractFailure("BLOCKED_INVALID_VALUE")
    if values["APP_IMAGE_REVISION"] != source_head:
        raise ContractFailure("BLOCKED_INVALID_VALUE")

    canonical_by_key = dict(canonical)
    ordered: list[tuple[str, str]] = []
    ordered.extend((key, canonical_by_key[key]) for key, _validator in PREFLIGHT_RULES)
    ordered.extend([
        ("SOURCE_ARCHIVE_SHA256", canonical_by_key["SOURCE_ARCHIVE_SHA256"]),
        ("SOURCE_ARCHIVE_REMOTE_SHA256", canonical_by_key["SOURCE_ARCHIVE_REMOTE_SHA256"]),
    ])
    for action in ACTION_RULES:
        stage_expected = ACTION_RULES[action][0][1]
        stage_value = next(value for value in FINAL_STAGE_VALUES if stage_expected(value))
        ordered.append(("P3H_REMOTE_STAGE", stage_value))
        ordered.extend(
            (key, canonical_by_key[key])
            for key, _validator in ACTION_RULES[action]
            if key != "P3H_REMOTE_STAGE"
        )
    ordered.append(("RESOURCE_CLEANUP", canonical_by_key["RESOURCE_CLEANUP"]))
    return ordered


def write_output(path: Path, lines: list[tuple[str, str]]) -> None:
    parent = path.parent
    parent.mkdir(parents=True, exist_ok=True)
    if path.is_symlink() or (path.exists() and not path.is_file()):
        raise ContractFailure("BLOCKED_INVALID_VALUE")
    descriptor, temporary_name = tempfile.mkstemp(prefix=".p3h-evidence-", dir=parent)
    temporary = Path(temporary_name)
    try:
        os.fchmod(descriptor, 0o600)
        with os.fdopen(descriptor, "w", encoding="ascii", newline="\n") as handle:
            for key, value in lines:
                handle.write(f"{key}: {value}\n")
        os.replace(temporary, path)
    except Exception:
        try:
            temporary.unlink(missing_ok=True)
        except OSError:
            pass
        raise


def main() -> int:
    args = parse_args()
    try:
        lines = read_lines(Path(args.input_file))
        if args.contract == "preflight":
            canonical = validate_preflight(lines)
        elif args.contract == "action":
            canonical = validate_action(lines, args.action, args.source_head)
        else:
            canonical = validate_final(lines, args.source_head)
        write_output(Path(args.output_file), canonical)
    except ContractFailure as failure:
        print(f"{args.status_key}: {failure.category}", file=sys.stderr, flush=True)
        return 2
    print(f"{args.status_key}: PASS_EXACT", flush=True)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
