#!/usr/bin/env python3
"""Minimal local-only NTP responder for the disposable P3-H Lima lab."""

from __future__ import annotations

import argparse
import ipaddress
import signal
import socket
import struct
import time


NTP_EPOCH_OFFSET = 2_208_988_800
RUNNING = True
ALLOWED_CLIENT_NETWORKS = (
    ipaddress.ip_network("127.0.0.0/8"),
    ipaddress.ip_network("192.168.5.0/24"),
)


def ntp_timestamp(value: float) -> bytes:
    seconds = int(value) + NTP_EPOCH_OFFSET
    fraction = int((value - int(value)) * (1 << 32))
    return struct.pack("!II", seconds, fraction)


def stop_server(_signum: int, _frame: object) -> None:
    global RUNNING
    RUNNING = False


def build_response(request: bytes, received_at: float) -> bytes | None:
    if len(request) < 48:
        return None
    version = (request[0] >> 3) & 0x07
    mode = request[0] & 0x07
    if version not in (3, 4) or mode != 3:
        return None

    response = bytearray(48)
    response[0] = (version << 3) | 4
    response[1] = 2
    response[2] = request[2]
    response[3] = 0xEC  # precision: 2^-20 seconds
    struct.pack_into("!I", response, 4, 0)
    struct.pack_into("!I", response, 8, 1 << 16)
    response[12:16] = b"LAB1"
    response[16:24] = ntp_timestamp(received_at - 1.0)
    response[24:32] = request[40:48]
    response[32:40] = ntp_timestamp(received_at)
    response[40:48] = ntp_timestamp(time.time())
    return bytes(response)


def main() -> int:
    parser = argparse.ArgumentParser(add_help=False)
    parser.add_argument("--bind", default="127.0.0.1")
    parser.add_argument("--port", type=int, default=123)
    parser.add_argument("--owner-token", required=True)
    args = parser.parse_args()
    if args.owner_token != "P3H-LAB1-USER-AUTH-20260717":
        return 2

    signal.signal(signal.SIGTERM, stop_server)
    signal.signal(signal.SIGINT, stop_server)
    with socket.socket(socket.AF_INET, socket.SOCK_DGRAM) as server:
        server.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
        server.bind((args.bind, args.port))
        server.settimeout(1.0)
        print("P3H_LAB_LOCAL_NTP: READY", flush=True)
        while RUNNING:
            try:
                request, address = server.recvfrom(512)
            except socket.timeout:
                continue
            client_address = ipaddress.ip_address(address[0])
            if not any(client_address in network for network in ALLOWED_CLIENT_NETWORKS):
                continue
            response = build_response(request, time.time())
            if response is not None:
                server.sendto(response, address)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
