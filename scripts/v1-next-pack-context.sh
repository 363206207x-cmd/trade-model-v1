#!/usr/bin/env bash

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

require_file() {
  local file="$1"
  if [[ ! -f "$file" ]]; then
    echo "missing file: $file" >&2
    exit 1
  fi
}

yaml_value() {
  local key="$1"
  awk -F': ' -v key="$key" '$1 == key { gsub(/^"|"$/, "", $2); print $2 }' docs/ACTIVE_MAINLINE_STATUS.yml
}

require_file "docs/ACTIVE_MAINLINE_STATUS.yml"

echo "current HEAD:"
git rev-parse --short HEAD

echo
echo "active mainline: $(yaml_value active_mainline)"
echo "active block: $(yaml_value active_block)"
echo "current level: $(yaml_value current_level)"
echo "next required action: $(yaml_value next_required_action)"
echo "do not continue target: $(yaml_value do_not_continue_to)"

echo
echo "source-of-truth files to read:"
awk '
  /^source_of_truth_files:/ { in_list = 1; next }
  in_list && /^  - / { sub(/^  - /, ""); print; next }
  in_list && !/^  - / { in_list = 0 }
' docs/ACTIVE_MAINLINE_STATUS.yml

echo
echo "open PR list:"
gh pr list --state open --limit 20

echo
echo "open Issue list:"
gh issue list --state open --limit 20
