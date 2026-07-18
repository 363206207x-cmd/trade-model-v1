#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

exec python3 "${ROOT_DIR}/scripts/p3h-evidence-contract.py" \
  --contract action \
  --action "${1:-}" \
  --source-head "${2:-}" \
  --input-file "${3:-}" \
  --output-file "${4:-}" \
  --status-key P3H_REMOTE_ACTION_EVIDENCE_CONTRACT
