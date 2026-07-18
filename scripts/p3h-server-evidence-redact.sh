#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

exec python3 "${ROOT_DIR}/scripts/p3h-evidence-contract.py" \
  --contract preflight \
  --input-file "${1:-}" \
  --output-file "${2:-}" \
  --status-key P3H_EVIDENCE_REDACTION
