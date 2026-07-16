#!/usr/bin/env bash
set -euo pipefail

secret_dir="${1:-}"
shift || true
if [ -z "${secret_dir}" ] || [ ! -d "${secret_dir}" ] || [ -L "${secret_dir}" ] \
    || [ "$#" -eq 0 ]; then
  echo "P3H_SECRET_LEAK_SCAN: BLOCKED_INPUT"
  exit 2
fi

python3 - "${secret_dir}" "$@" <<'PY'
from pathlib import Path
import sys

secret_dir = Path(sys.argv[1])
scan_roots = [Path(value) for value in sys.argv[2:]]
secrets = []
for path in secret_dir.iterdir():
    if path.is_symlink() or not path.is_file():
        continue
    value = path.read_bytes().strip()
    if len(value) >= 8:
        secrets.append(value)

count = 0
for root in scan_roots:
    candidates = [root] if root.is_file() else list(root.rglob("*")) if root.is_dir() else []
    for path in candidates:
        if path.is_symlink() or not path.is_file():
            continue
        try:
            content = path.read_bytes()
        except OSError:
            continue
        if any(secret in content for secret in secrets):
            count += 1

print(f"SECRET_LEAK_CANDIDATE_COUNT: {count}")
raise SystemExit(0 if count == 0 else 2)
PY
