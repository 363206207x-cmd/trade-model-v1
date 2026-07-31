#!/usr/bin/env bash

set -uo pipefail

tests=0
failures=0
errors=0
skipped=0

fail() {
  echo "FAIL: $*"
  failures=$((failures + 1))
}

assert_contains() {
  local file="$1"
  local text="$2"
  tests=$((tests + 1))
  if [[ ! -f "$file" ]]; then
    fail "missing file: $file"
  elif ! grep -Fq -- "$text" "$file"; then
    fail "$file is missing: $text"
  fi
}

assert_not_contains() {
  local file="$1"
  local text="$2"
  tests=$((tests + 1))
  if [[ ! -f "$file" ]]; then
    fail "missing file: $file"
  elif grep -Fq -- "$text" "$file"; then
    fail "$file still contains forbidden text: $text"
  fi
}

semantic="docs/design/FE04_SEMANTIC_CONTRACT_V2.md"
interaction="docs/INTERACTION_CONTRACT_V3.md"
freeze="docs/FE04_POSITION_MONITORING_IMPLEMENTATION_FREEZE.md"
state="docs/PROJECT_CURRENT_STATE.md"
matrix="docs/DELIVERY_PROGRESS_MATRIX.md"
active="docs/ACTIVE_MAINLINE_STATUS.yml"
next_task="docs/CODEX_NEXT_TASK.yml"
change_log="docs/CONTRACT_CHANGE_LOG.md"
frontend_audit="docs/FRONTEND_IMPLEMENTATION_CONTRACT_AUDIT_V2.md"

# Public OPPORTUNITY contract: public inputs only and no private state oracle.
assert_contains "$semantic" 'AUTHENTICATED_SHARED_PUBLIC_PROJECTION'
assert_contains "$semantic" 'Its public state inputs are limited to public'
assert_contains "$semantic" 'MUST NOT read or depend on private/internal `pushId`'
assert_contains "$semantic" 'This is a strict no-private-state-oracle rule.'
assert_contains "$semantic" 'same lifecycle, status, and readiness for all authenticated users'
assert_not_contains "$semantic" 'A separate server-side readiness projection may validate'
assert_not_contains "$semantic" 'required Recheck is complete'

assert_contains "$interaction" 'public `OPPORTUNITY` never exposes or pivots through private/internal'
assert_contains "$interaction" 'never change public'
assert_contains "$interaction" 'private pivot or state oracle.'
assert_contains "$interaction" 'No system-notification, AI-generated-message, delivery, or third message source'
assert_not_contains "$interaction" 'complete legal matching Push/Recheck data with completed execution'
assert_not_contains "$interaction" 'System notices may be shown as a separate informational category'

# Private POSITION_RISK contract: one resolver and the authoritative latest monitor.
assert_contains "$freeze" 'list and detail use one shared resolver'
assert_contains "$freeze" 'authoritative latest monitor through the exact owner/message/position'
assert_contains "$freeze" 'selected historical monitor cannot substitute'
assert_contains "$freeze" 'Monitor-row existence alone must never imply `READY`.'
assert_contains "$freeze" 'Account-risk level and monitor composite-risk level are distinct dimensions.'
assert_contains "$freeze" 'inequality between the two levels is valid and is not itself `ERROR`'
assert_contains "$freeze" 'FE04E_POSITION_RISK_STATE_RESOLVER: LIST_DETAIL_SHARED_AUTHORITATIVE_LATEST_MONITOR'
assert_contains "$freeze" 'FE04E_POSITION_RISK_ROW_EXISTENCE_RULE: ROW_EXISTENCE_NEVER_IMPLIES_READY'
assert_contains "$freeze" 'FE04E_RISK_LEVEL_DIMENSIONS: ACCOUNT_AND_MONITOR_COMPOSITE_INDEPENDENT_MISMATCH_NOT_ERROR'

# Exact merged-main evidence and runtime/governance separation.
assert_contains "$state" 'FE-04E Source Implementation PR: #1155 / MERGED'
assert_contains "$state" 'FE-04E Source Authorized Head: 269ec97c11efa30fe58d99a4d78d09387e6fd277'
assert_contains "$state" 'FE-04E Source Merge Commit: 2552dd24b1b756d5eb517e640baa772e1c5bcab6'
assert_contains "$state" 'FE-04E Source Merged At: 2026-07-31T03:55:03Z'
assert_contains "$state" 'FE-04E Source Merge Method: SQUASH'
assert_contains "$state" 'FE-04E Runtime Status: EFFECTIVE_MERGED_MAIN'
assert_contains "$state" 'FE-04E Governance Alignment: PR #1156 / PENDING_MERGED_MAIN'

assert_contains "$matrix" '| FE-04 | Position Monitoring Frontend | IN_PROGRESS | PARTIAL |'
assert_contains "$matrix" 'PR #1155 authorized Head `269ec97c11efa30fe58d99a4d78d09387e6fd277` was squash-merged at `2026-07-31T03:55:03Z`'
assert_contains "$matrix" 'FE-04E Message/Push UI and FE-04F Profile remain unimplemented'
assert_contains "$matrix" 'mismatch alone is not ERROR'

assert_contains "$active" 'fe_04e_source_pr_status: "MERGED"'
assert_contains "$active" 'fe_04e_source_merge_method: "SQUASH"'
assert_contains "$active" 'fe_04e_governance_alignment_status: "PENDING_MERGED_MAIN"'
assert_contains "$active" 'fe_04e_position_risk_state_resolver: "LIST_DETAIL_SHARED_AUTHORITATIVE_LATEST_MONITOR"'
assert_contains "$active" 'fe_04e_risk_level_dimensions: "ACCOUNT_AND_MONITOR_COMPOSITE_INDEPENDENT_MISMATCH_NOT_ERROR"'

assert_contains "$next_task" 'module: "FE-04E Message/Push UI Readiness and Governance Re-evaluation"'
assert_contains "$next_task" 'fe_04e_ui_status: "NOT_STARTED_PENDING_READINESS_AND_GOVERNANCE_REEVALUATION"'
assert_contains "$next_task" 'fe_04e_source_authorized_head: "269ec97c11efa30fe58d99a4d78d09387e6fd277"'
assert_contains "$next_task" 'fe_04e_source_merged_at: "2026-07-31T03:55:03Z"'
assert_contains "$next_task" 'fe_04e_authorization: "READ_ONLY_READINESS_REEVALUATION_ONLY"'

assert_contains "$change_log" '## v1.0-fe04e-privacy-state-foundation-effective-merged-main'
assert_not_contains "$change_log" '## v1.0-fe04e-opportunity-public-projection-candidate'
assert_contains "$change_log" '`269ec97c11efa30fe58d99a4d78d09387e6fd277` was squash-merged at'
assert_contains "$change_log" '`2026-07-31T03:55:03Z` as current main'
assert_contains "$change_log" 'Message/Push UI remains'
assert_contains "$change_log" '`NOT_IMPLEMENTED`'

assert_contains "$frontend_audit" 'public `OPPORTUNITY` readiness uses public opportunity inputs only'
assert_contains "$frontend_audit" 'Private `POSITION_RISK` states use the same authoritative'

echo "FE04E_GOVERNANCE_TESTS: $tests"
echo "FAILURES: $failures"
echo "ERRORS: $errors"
echo "SKIPPED: $skipped"

if [[ "$failures" -eq 0 && "$errors" -eq 0 ]]; then
  echo "FE04E_GOVERNANCE_CONTRACT_OK"
  exit 0
fi

echo "FE04E_GOVERNANCE_CONTRACT_FAILED"
exit 1
