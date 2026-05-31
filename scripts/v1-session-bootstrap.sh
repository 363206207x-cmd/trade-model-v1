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

for file in \
  docs/SESSION_BOOTSTRAP.md \
  docs/ACTIVE_MAINLINE_STATUS.yml \
  docs/ANSWER_FORMAT_CONTRACT.md \
  docs/V1_CAPABILITY_MATRIX.md \
  docs/V1_PROGRESS_SOURCE_OF_TRUTH.md
do
  require_file "$file"
done

echo "Current Mainline（当前主线）: $(yaml_value active_mainline) / $(yaml_value active_mainline_cn)"
echo "Current Block（当前模块）: $(yaml_value active_block) / $(yaml_value active_block_cn)"
echo "Current Level（当前层级）: $(yaml_value current_level)"
echo "Current PR（当前 PR）: $(yaml_value current_pr)"
echo "Next Step（下一步）: $(yaml_value next_required_action)"
echo "Do Not Do（禁止事项）: do not continue to $(yaml_value do_not_continue_to); no order / execution / auto-trading"

echo
echo "git log --oneline -5:"
git log --oneline -5

echo
echo "git status --short:"
git status --short

echo
echo "ACTIVE_MAINLINE_STATUS.yml:"
cat docs/ACTIVE_MAINLINE_STATUS.yml
