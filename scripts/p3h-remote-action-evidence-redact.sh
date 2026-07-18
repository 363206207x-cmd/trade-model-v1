#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
action="${1:-}"

if [ "${action}" = BUILD_APPLICATION_IMAGE ]; then
  exec python3 "${ROOT_DIR}/scripts/p3h-evidence-contract.py" \
    --contract action \
    --action "${action}" \
    --source-head "${2:-}" \
    --input-file "${3:-}" \
    --output-file "${4:-}" \
    --app-jar-sha256 "${5:-}" \
    --app-artifact-archive-sha256 "${6:-}" \
    --status-key P3H_REMOTE_ACTION_EVIDENCE_CONTRACT
fi

exec python3 "${ROOT_DIR}/scripts/p3h-evidence-contract.py" \
  --contract action \
  --action "${action}" \
  --source-head "${2:-}" \
  --input-file "${3:-}" \
  --output-file "${4:-}" \
  --status-key P3H_REMOTE_ACTION_EVIDENCE_CONTRACT
