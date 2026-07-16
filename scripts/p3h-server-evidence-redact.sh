#!/usr/bin/env bash
set -euo pipefail

input_file="${1:-}"
output_file="${2:-}"
if [ -z "${input_file}" ] || [ -z "${output_file}" ] \
    || [ ! -f "${input_file}" ] || [ -L "${input_file}" ]; then
  echo "P3H_EVIDENCE_REDACTION: BLOCKED_INPUT" >&2
  exit 2
fi

umask 077
mkdir -p "$(dirname "${output_file}")"
awk -F ': ' '
  BEGIN {
    allowed["REMOTE_PREFLIGHT"] = 1
    allowed["LINUX_DISTRIBUTION"] = 1
    allowed["KERNEL_RELEASE"] = 1
    allowed["CPU_ARCHITECTURE"] = 1
    allowed["SYSTEMD_VERSION"] = 1
    allowed["DOCKER_ENGINE_VERSION"] = 1
    allowed["DOCKER_COMPOSE_VERSION"] = 1
    allowed["OPENSSL_VERSION"] = 1
    allowed["TIMEZONE"] = 1
    allowed["TIME_SYNCHRONIZED"] = 1
    allowed["SECRET_BACKEND_CLASS"] = 1
    allowed["SECRET_MOUNT"] = 1
    allowed["SECRET_MOUNT_RUNTIME_VERIFICATION"] = 1
    allowed["SECRET_MOUNT_FILESYSTEM"] = 1
    allowed["SECRET_FILE_CONTRACT"] = 1
    allowed["SUDO_NONINTERACTIVE"] = 1
    allowed["AVAILABLE_DISK_KB"] = 1
    allowed["AVAILABLE_MEMORY_KB"] = 1
  }
  NF == 2 && allowed[$1] && $2 ~ /^[A-Za-z0-9._+-]+$/ { print $1 ": " $2 }
' "${input_file}" >"${output_file}"

if ! grep -q '^REMOTE_PREFLIGHT: PASS$' "${output_file}"; then
  echo "P3H_EVIDENCE_REDACTION: BLOCKED_UNSAFE_OR_INCOMPLETE" >&2
  exit 2
fi
echo "P3H_EVIDENCE_REDACTION: PASS"
