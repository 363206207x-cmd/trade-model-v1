#!/usr/bin/env bash

set -euo pipefail

failed=0

require_file() {
  local file="$1"
  if [[ ! -f "$file" ]]; then
    echo "missing file: $file"
    failed=1
  fi
}

require_contains() {
  local file="$1"
  local text="$2"
  if [[ ! -f "$file" ]] || ! grep -Fq "$text" "$file"; then
    echo "missing required text in $file: $text"
    failed=1
  fi
}

require_file "docs/ACTIVE_MAINLINE_STATUS.yml"
require_file "docs/SESSION_BOOTSTRAP.md"
require_file "docs/ANSWER_FORMAT_CONTRACT.md"
require_file "docs/V1_CAPABILITY_MATRIX.md"
require_file "docs/V1_PROGRESS_SOURCE_OF_TRUTH.md"
require_file "docs/WORKFLOW_COMMAND_AUTOMATION.md"
require_file ".github/pull_request_template.md"
require_file "scripts/v1-status.sh"
require_file "scripts/v1-pr-review-input.sh"
require_file "scripts/v1-merge-sync.sh"
require_file "scripts/v1-safe-check.sh"
require_file "scripts/v1-session-bootstrap.sh"
require_file "scripts/v1-next-pack-context.sh"

require_contains "docs/ACTIVE_MAINLINE_STATUS.yml" "active_mainline"
require_contains "docs/ANSWER_FORMAT_CONTRACT.md" "Current Mainline（当前主线）"
require_contains "docs/SESSION_BOOTSTRAP.md" "Open PR / branch / Issue does not count as done"
require_contains ".github/pull_request_template.md" "Capability Level Before"

changed_files="$(
  {
    git diff --name-only 2>/dev/null || true
    git diff --cached --name-only 2>/dev/null || true
    git diff --name-only origin/main...HEAD 2>/dev/null || true
    git diff --name-only HEAD~1..HEAD 2>/dev/null || true
  } | sort -u
)"

if echo "$changed_files" | grep -Eq 'src/main/java|src/test/java'; then
  require_contains ".github/pull_request_template.md" "Capability Level Before"
  require_contains ".github/pull_request_template.md" "Capability Level After"
  require_contains "docs/ACTIVE_MAINLINE_STATUS.yml" "current_level"
fi

if [[ "$failed" -eq 0 ]]; then
  echo "WORKFLOW_CONTRACT_OK"
  exit 0
fi

echo "WORKFLOW_CONTRACT_FAILED"
exit 1
