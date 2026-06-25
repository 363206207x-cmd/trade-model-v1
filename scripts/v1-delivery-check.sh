#!/usr/bin/env bash

set -euo pipefail

EXPECTED_ROOT="/Users/xuchao/Documents/trade-model-v1"
ROOT="$(pwd -P)"

if [[ "$ROOT" != "$EXPECTED_ROOT" ]]; then
  echo "DELIVERY_CHECK_STATUS: FAIL"
  echo "REASON: WRONG_PROJECT_PATH"
  echo "PWD: $ROOT"
  echo "EXPECTED_PWD: $EXPECTED_ROOT"
  exit 2
fi

tmp_dir="$(mktemp -d)"
trap 'rm -rf "$tmp_dir"' EXIT

test_log="$tmp_dir/maven-test.log"
state_log="$tmp_dir/v1-state.log"
gh_log="$tmp_dir/gh-status.log"
changed_log="$tmp_dir/changed-files.log"

field_from_state() {
  local key="$1"
  awk -F': ' -v key="$key" '$1 == key { print substr($0, length(key) + 3); exit }' "$state_log"
}

collect_changed_files() {
  {
    git diff --name-only
    git diff --cached --name-only
    git ls-files --others --exclude-standard
  } | sort -u >"$changed_log"
}

branch="$(git branch --show-current 2>/dev/null || true)"
collect_changed_files

if command -v gh >/dev/null 2>&1; then
  if gh auth status >"$gh_log" 2>&1; then
    gh_status="OK"
  else
    gh_status="UNAVAILABLE_OR_UNAUTHENTICATED"
  fi
else
  gh_status="NOT_INSTALLED"
  echo "gh not found" >"$gh_log"
fi

if ./mvnw test -q >"$test_log" 2>&1; then
  test_result="PASS"
else
  test_result="FAIL"
fi

if bash scripts/v1-state.sh >"$state_log" 2>&1; then
  state_result="PASS"
else
  state_result="FAIL"
fi

if [[ "$test_result" == "PASS" && "$state_result" == "PASS" ]]; then
  delivery_status="PASS"
else
  delivery_status="FAIL"
fi

echo "DELIVERY_CHECK_STATUS: $delivery_status"
echo "PWD: $ROOT"
echo "BRANCH: ${branch:-UNKNOWN}"
echo "CHANGED_FILES_COUNT: $(wc -l <"$changed_log" | tr -d ' ')"
if [[ -s "$changed_log" ]]; then
  sed 's/^/CHANGED_FILE: /' "$changed_log"
else
  echo "CHANGED_FILES: none"
fi
echo "GH_STATUS: $gh_status"
echo "TEST_RESULT: $test_result"
echo "V1_STATE_RESULT: $state_result"
echo "WORKTREE_CLEAN: $(field_from_state WORKTREE_CLEAN || true)"
echo "MAIN_SYNC: $(field_from_state MAIN_SYNC || true)"
echo "OPEN_PR_STATUS: $(field_from_state OPEN_PR_STATUS || true)"
echo "NEXT_BUSINESS_PHASE_ALLOWED: $(field_from_state NEXT_BUSINESS_PHASE_ALLOWED || true)"
echo "CAN_CONTINUE_NEXT_PACKAGE: $(field_from_state CAN_CONTINUE_NEXT_PACKAGE || true)"
echo "BLOCKERS: $(field_from_state BLOCKERS || true)"

if [[ "$test_result" != "PASS" ]]; then
  echo "TEST_LOG_TAIL:"
  tail -n 80 "$test_log"
fi

if [[ "$state_result" != "PASS" ]]; then
  echo "V1_STATE_LOG_TAIL:"
  tail -n 80 "$state_log"
fi

if [[ "$delivery_status" != "PASS" ]]; then
  exit 1
fi
