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

echo "Current branch:"
git branch --show-current

echo
echo "git status --short:"
git status --short

echo
echo "git log --oneline -5:"
git log --oneline -5

echo
echo "current HEAD:"
git rev-parse --short HEAD

echo
echo "ACTIVE_MAINLINE_STATUS.yml summary:"
require_file "docs/ACTIVE_MAINLINE_STATUS.yml"
echo "active_mainline: $(yaml_value active_mainline)"
echo "active_block: $(yaml_value active_block)"
echo "current_level: $(yaml_value current_level)"
echo "current_pr: $(yaml_value current_pr)"
echo "current_branch: $(yaml_value current_branch)"
echo "next_required_action: $(yaml_value next_required_action)"
echo "do_not_continue_to: $(yaml_value do_not_continue_to)"

echo
echo "open PR list:"
gh pr list --state open --limit 20

echo
echo "recent open Issue list:"
gh issue list --state open --limit 20

echo
echo "source-of-truth files existence:"
for file in \
  docs/SESSION_BOOTSTRAP.md \
  docs/ACTIVE_MAINLINE_STATUS.yml \
  docs/V1_PROGRESS_SOURCE_OF_TRUTH.md \
  docs/V1_CAPABILITY_MATRIX.md \
  docs/V1_CURRENT_STATE.md \
  docs/PROJECT_PROGRESS_INDEX.md \
  docs/ANSWER_FORMAT_CONTRACT.md
do
  require_file "$file"
  echo "OK $file"
done
