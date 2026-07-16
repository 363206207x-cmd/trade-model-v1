#!/usr/bin/env bash
set -euo pipefail

if [ "${P3H_SECRET_BACKEND:-}" != "SYSTEMD_CREDENTIALS" ]; then
  echo "P3H_SYSTEMD_CREDENTIALS: BLOCKED_BACKEND_NOT_IMPLEMENTED"
  exit 2
fi
for required_tool in findmnt realpath; do
  command -v "${required_tool}" >/dev/null 2>&1 \
    || { echo "P3H_SYSTEMD_CREDENTIALS: BLOCKED_REQUIRED_TOOL"; exit 2; }
done
if [ -z "${CREDENTIALS_DIRECTORY:-}" ] || [ ! -d "${CREDENTIALS_DIRECTORY}" ] \
    || [ -L "${CREDENTIALS_DIRECTORY}" ]; then
  echo "P3H_SYSTEMD_CREDENTIALS: BLOCKED_CREDENTIALS_DIRECTORY"
  exit 2
fi

credentials_realpath="$(realpath "${CREDENTIALS_DIRECTORY}")"
case "${credentials_realpath}" in
  /run/credentials/*) ;;
  *) echo "P3H_SYSTEMD_CREDENTIALS: BLOCKED_CREDENTIALS_DIRECTORY"; exit 2 ;;
esac

runtime_fstype="$(findmnt -n -T /run -o FSTYPE 2>/dev/null || true)"
case "${runtime_fstype}" in
  tmpfs|ramfs) ;;
  *) echo "P3H_SYSTEMD_CREDENTIALS: BLOCKED_RUNTIME_FILESYSTEM"; exit 2 ;;
esac

export P3H_SECRET_MOUNT_DIR="${credentials_realpath}"

exec "$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)/p3h-compose-start.sh"
