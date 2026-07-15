#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
CONTEXT_DIR="${1:-}"

if [ -z "${CONTEXT_DIR}" ] || [ ! -d "${CONTEXT_DIR}" ]; then
  echo "DOCKER_CONTEXT_SAFETY: BLOCKED_CONTEXT_REQUIRED"
  exit 2
fi

case "${CONTEXT_DIR}" in
  "${ROOT_DIR}"|"${ROOT_DIR}/"*)
    echo "DOCKER_CONTEXT_SAFETY: BLOCKED_WORKTREE_CONTEXT"
    exit 2
    ;;
esac

if [ ! -f "${CONTEXT_DIR}/Dockerfile" ]; then
  echo "DOCKER_CONTEXT_SAFETY: BLOCKED_DOCKERFILE_MISSING"
  exit 2
fi

unsafe_path="$(find "${CONTEXT_DIR}" \
  \( -type d -name .runtime -o -type d -name backups \
     -o -type f -name '*.dump' -o -type f -name '*.attestation' \
     -o -type f -name '.env' -o -type f -name '.env.*' \
     -o -type f -name '*.env' \) \
  ! -name '.env.example' -print -quit)"
if [ -n "${unsafe_path}" ]; then
  echo "DOCKER_CONTEXT_SAFETY: BLOCKED_FORBIDDEN_PATH"
  exit 2
fi

if find "${CONTEXT_DIR}" -type l -print -quit | grep -q .; then
  echo "DOCKER_CONTEXT_SAFETY: BLOCKED_SYMLINK"
  exit 2
fi

echo "DOCKER_CONTEXT_SAFETY: PASS_EXACT_ARCHIVE_CONTEXT"
echo "RUNTIME_EVIDENCE_IN_CONTEXT: NO"
echo "DUMP_IN_CONTEXT: NO"
echo "ATTESTATION_IN_CONTEXT: NO"
echo "SECRET_ENV_IN_CONTEXT: NO"
