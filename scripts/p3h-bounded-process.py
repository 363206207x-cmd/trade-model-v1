#!/usr/bin/env python3
"""Run one command in an isolated process group with sanitized time bounds."""

from __future__ import annotations

import argparse
import os
import signal
import stat
import subprocess
import sys
import time
from typing import BinaryIO
from pathlib import Path


EXIT_STAGE_TIMEOUT = 124
EXIT_GLOBAL_TIMEOUT = 125
EXIT_SIGNALLED = 143
EXIT_INPUT_CONTRACT = 2
MAX_STDIN_BYTES = 1024 * 1024


class StdinContractError(Exception):
    """A sanitized stdin-file contract failure."""

    def __init__(self, category: str) -> None:
        super().__init__(category)
        self.category = category


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(add_help=False)
    parser.add_argument("--timeout-seconds", type=int, required=True)
    parser.add_argument("--global-start-epoch", type=int, required=True)
    parser.add_argument("--global-timeout-seconds", type=int, required=True)
    parser.add_argument("--stage", required=True)
    parser.add_argument("--operation-class", required=True)
    parser.add_argument("--poll-seconds", type=int, default=15)
    parser.add_argument("--heartbeat-seconds", type=int, default=60)
    parser.add_argument("--term-grace-seconds", type=int, default=15)
    parser.add_argument("--cleanup-script")
    parser.add_argument("--cleanup-timeout-seconds", type=int, default=300)
    parser.add_argument("--timeout-marker")
    parser.add_argument("--stdin-file")
    parser.add_argument("command", nargs=argparse.REMAINDER)
    args = parser.parse_args()
    if args.command and args.command[0] == "--":
        args.command = args.command[1:]
    if not args.command:
        parser.error("a command is required")
    for value in (
        args.timeout_seconds,
        args.global_timeout_seconds,
        args.poll_seconds,
        args.heartbeat_seconds,
        args.term_grace_seconds,
        args.cleanup_timeout_seconds,
    ):
        if value <= 0:
            parser.error("time values must be positive")
    if args.poll_seconds > 15:
        parser.error("poll interval must not exceed 15 seconds")
    return args


def open_stdin_file(path_value: str) -> BinaryIO:
    path = Path(path_value)
    try:
        before = path.lstat()
    except OSError as exc:
        raise StdinContractError("BLOCKED_MISSING") from exc
    if stat.S_ISLNK(before.st_mode):
        raise StdinContractError("BLOCKED_SYMLINK")
    if not stat.S_ISREG(before.st_mode):
        raise StdinContractError("BLOCKED_NOT_REGULAR")
    if before.st_mode & 0o022:
        raise StdinContractError("BLOCKED_UNSAFE_PERMISSIONS")
    if before.st_size > MAX_STDIN_BYTES:
        raise StdinContractError("BLOCKED_OVERSIZED")

    flags = os.O_RDONLY | getattr(os, "O_CLOEXEC", 0) | getattr(os, "O_NOFOLLOW", 0)
    try:
        descriptor = os.open(path, flags)
    except OSError as exc:
        raise StdinContractError("BLOCKED_UNREADABLE") from exc
    try:
        after = os.fstat(descriptor)
        if not stat.S_ISREG(after.st_mode):
            raise StdinContractError("BLOCKED_NOT_REGULAR")
        if (before.st_dev, before.st_ino) != (after.st_dev, after.st_ino):
            raise StdinContractError("BLOCKED_RACE")
        if after.st_mode & 0o022:
            raise StdinContractError("BLOCKED_UNSAFE_PERMISSIONS")
        if after.st_size > MAX_STDIN_BYTES:
            raise StdinContractError("BLOCKED_OVERSIZED")
        return os.fdopen(descriptor, "rb", closefd=True)
    except Exception:
        os.close(descriptor)
        raise


def emit_heartbeat(args: argparse.Namespace, stage_elapsed: int, state: str) -> None:
    global_elapsed = max(0, int(time.time()) - args.global_start_epoch)
    print(f"P3H_LAB_STAGE: {args.stage}", file=sys.stderr, flush=True)
    print(f"STAGE_ELAPSED_SECONDS: {stage_elapsed}", file=sys.stderr, flush=True)
    print(f"GLOBAL_ELAPSED_SECONDS: {global_elapsed}", file=sys.stderr, flush=True)
    print(f"PROCESS_STATE: {state}", file=sys.stderr, flush=True)
    print(
        f"DOCKER_OPERATION_CLASS: {args.operation_class}",
        file=sys.stderr,
        flush=True,
    )


def process_group_exists(process_group_id: int) -> bool:
    try:
        os.killpg(process_group_id, 0)
    except ProcessLookupError:
        return False
    except PermissionError:
        return True
    return True


def terminate_group(process: subprocess.Popen[bytes], grace_seconds: int) -> bool:
    """Terminate one exact process group and report whether KILL was required."""
    process_group_id = process.pid
    if not process_group_exists(process_group_id):
        return False
    try:
        os.killpg(process_group_id, signal.SIGTERM)
    except (ProcessLookupError, PermissionError):
        return False
    deadline = time.monotonic() + grace_seconds
    while process_group_exists(process_group_id) and time.monotonic() < deadline:
        process.poll()
        time.sleep(min(1, max(0.0, deadline - time.monotonic())))
    escalated = process_group_exists(process_group_id)
    if escalated:
        try:
            os.killpg(process_group_id, signal.SIGKILL)
        except (ProcessLookupError, PermissionError):
            pass
    try:
        process.wait(timeout=grace_seconds)
    except subprocess.TimeoutExpired:
        pass
    return escalated


def run_cleanup(args: argparse.Namespace) -> bool:
    """Run the exact caller-provided cleanup script in its own bounded group."""
    if not args.cleanup_script:
        return True
    cleanup = subprocess.Popen(
        ["bash", args.cleanup_script],
        start_new_session=True,
    )
    try:
        cleanup.wait(timeout=args.cleanup_timeout_seconds)
    except subprocess.TimeoutExpired:
        escalated = terminate_group(cleanup, args.term_grace_seconds)
        print("SUPERVISOR_CLEANUP_STATUS: FAIL", file=sys.stderr, flush=True)
        print("SUPERVISOR_CLEANUP_TIMEOUT: YES", file=sys.stderr, flush=True)
        if escalated:
            print("CLEANUP_TERM_ESCALATED_TO_KILL: YES", file=sys.stderr, flush=True)
        return False
    if cleanup.returncode != 0:
        print("SUPERVISOR_CLEANUP_STATUS: FAIL", file=sys.stderr, flush=True)
        return False
    print("SUPERVISOR_CLEANUP_STATUS: PASS", file=sys.stderr, flush=True)
    return True


def write_timeout_marker(path_value: str | None) -> None:
    if not path_value:
        return
    path = Path(path_value)
    try:
        descriptor = os.open(path, os.O_WRONLY | os.O_CREAT | os.O_EXCL, 0o600)
    except FileExistsError:
        return
    else:
        os.close(descriptor)


def main() -> int:
    args = parse_args()
    started_monotonic = time.monotonic()
    stdin_handle: BinaryIO | None = None
    if args.stdin_file:
        try:
            stdin_handle = open_stdin_file(args.stdin_file)
        except StdinContractError as failure:
            print(f"STDIN_FILE_STATUS: {failure.category}", file=sys.stderr, flush=True)
            return EXIT_INPUT_CONTRACT
    try:
        process = subprocess.Popen(
            args.command,
            stdin=stdin_handle,
            start_new_session=True,
        )
    except Exception:
        if stdin_handle is not None:
            stdin_handle.close()
        raise
    interrupted = False

    def handle_signal(_signum: int, _frame: object) -> None:
        nonlocal interrupted
        interrupted = True
        terminate_group(process, args.term_grace_seconds)

    signal.signal(signal.SIGTERM, handle_signal)
    signal.signal(signal.SIGINT, handle_signal)

    try:
        next_heartbeat = args.heartbeat_seconds
        while process.poll() is None:
            stage_elapsed = int(time.monotonic() - started_monotonic)
            global_elapsed = max(0, int(time.time()) - args.global_start_epoch)
            if interrupted:
                return EXIT_SIGNALLED
            if global_elapsed >= args.global_timeout_seconds:
                emit_heartbeat(args, stage_elapsed, "GLOBAL_TIMEOUT")
                write_timeout_marker(args.timeout_marker)
                escalated = terminate_group(process, args.term_grace_seconds)
                print("GLOBAL_TIMEOUT_TRIGGERED: YES", file=sys.stderr, flush=True)
                if escalated:
                    print("TERM_ESCALATED_TO_KILL: YES", file=sys.stderr, flush=True)
                run_cleanup(args)
                return EXIT_GLOBAL_TIMEOUT
            if stage_elapsed >= args.timeout_seconds:
                emit_heartbeat(args, stage_elapsed, "STAGE_TIMEOUT")
                escalated = terminate_group(process, args.term_grace_seconds)
                print("STAGE_TIMEOUT_TRIGGERED: YES", file=sys.stderr, flush=True)
                if escalated:
                    print("TERM_ESCALATED_TO_KILL: YES", file=sys.stderr, flush=True)
                run_cleanup(args)
                return EXIT_STAGE_TIMEOUT
            if stage_elapsed >= next_heartbeat:
                emit_heartbeat(args, stage_elapsed, "RUNNING")
                next_heartbeat += args.heartbeat_seconds
            remaining = min(
                args.poll_seconds,
                args.timeout_seconds - stage_elapsed,
                args.global_timeout_seconds - global_elapsed,
            )
            time.sleep(max(0.1, remaining))

        if interrupted:
            run_cleanup(args)
            return EXIT_SIGNALLED
        if process.returncode != 0:
            run_cleanup(args)
        return process.returncode
    finally:
        if stdin_handle is not None:
            stdin_handle.close()


if __name__ == "__main__":
    raise SystemExit(main())
