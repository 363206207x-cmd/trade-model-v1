#!/usr/bin/env python3
"""Run one command in an isolated process group with sanitized time bounds."""

from __future__ import annotations

import argparse
import os
import signal
import subprocess
import sys
import time


EXIT_STAGE_TIMEOUT = 124
EXIT_GLOBAL_TIMEOUT = 125
EXIT_SIGNALLED = 143


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
    ):
        if value <= 0:
            parser.error("time values must be positive")
    if args.poll_seconds > 15:
        parser.error("poll interval must not exceed 15 seconds")
    return args


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


def terminate_group(process: subprocess.Popen[bytes], grace_seconds: int) -> None:
    process_group_id = process.pid
    if not process_group_exists(process_group_id):
        return
    try:
        os.killpg(process_group_id, signal.SIGTERM)
    except (ProcessLookupError, PermissionError):
        return
    deadline = time.monotonic() + grace_seconds
    while process_group_exists(process_group_id) and time.monotonic() < deadline:
        process.poll()
        time.sleep(min(1, max(0.0, deadline - time.monotonic())))
    if process_group_exists(process_group_id):
        try:
            os.killpg(process_group_id, signal.SIGKILL)
        except (ProcessLookupError, PermissionError):
            pass
    try:
        process.wait(timeout=grace_seconds)
    except subprocess.TimeoutExpired:
        pass


def main() -> int:
    args = parse_args()
    started_monotonic = time.monotonic()
    process = subprocess.Popen(args.command, start_new_session=True)
    interrupted = False

    def handle_signal(_signum: int, _frame: object) -> None:
        nonlocal interrupted
        interrupted = True
        terminate_group(process, args.term_grace_seconds)

    signal.signal(signal.SIGTERM, handle_signal)
    signal.signal(signal.SIGINT, handle_signal)

    next_heartbeat = args.heartbeat_seconds
    while process.poll() is None:
        stage_elapsed = int(time.monotonic() - started_monotonic)
        global_elapsed = max(0, int(time.time()) - args.global_start_epoch)
        if interrupted:
            return EXIT_SIGNALLED
        if global_elapsed >= args.global_timeout_seconds:
            emit_heartbeat(args, stage_elapsed, "GLOBAL_TIMEOUT")
            terminate_group(process, args.term_grace_seconds)
            return EXIT_GLOBAL_TIMEOUT
        if stage_elapsed >= args.timeout_seconds:
            emit_heartbeat(args, stage_elapsed, "STAGE_TIMEOUT")
            terminate_group(process, args.term_grace_seconds)
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
        return EXIT_SIGNALLED
    return process.returncode


if __name__ == "__main__":
    raise SystemExit(main())
