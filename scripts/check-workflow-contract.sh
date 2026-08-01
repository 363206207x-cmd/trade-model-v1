#!/usr/bin/env bash

set -euo pipefail

if ! bash scripts/product-source-gate.sh; then
  echo "WORKFLOW_STATUS:"
  echo "BLOCKED_BY_PRODUCT_SOURCE_GATE"
  exit 1
fi

failed=0

fail() {
  echo "$*"
  failed=1
}

require_file() {
  local file="$1"
  if [[ ! -f "$file" ]]; then
    fail "missing file: $file"
  fi
}

require_contains() {
  local file="$1"
  local text="$2"
  if [[ ! -f "$file" ]] || ! grep -Fq -- "$text" "$file"; then
    fail "missing required text in $file: $text"
  fi
}

require_not_contains() {
  local file="$1"
  local text="$2"
  if [[ -f "$file" ]] && grep -Fq -- "$text" "$file"; then
    fail "forbidden stale text in $file: $text"
  fi
}

require_executable() {
  local file="$1"
  if [[ ! -x "$file" ]]; then
    fail "missing executable: $file"
  fi
}

yaml_value() {
  local file="$1"
  local key="$2"
  [[ -f "$file" ]] || return 0
  awk -v key="$key" '
    $0 ~ "^" key ":" {
      value=$0
      sub("^[^:]*:[[:space:]]*", "", value)
      gsub(/^\"/, "", value)
      gsub(/\"$/, "", value)
      print value
      exit
    }
  ' "$file"
}

yaml_list() {
  local file="$1"
  local key="$2"
  [[ -f "$file" ]] || return 0
  awk -v key="$key" '
    $0 ~ "^" key ":[[:space:]]*$" {
      capture=1
      next
    }
    capture && $0 ~ "^[^[:space:]]" {
      exit
    }
    capture && $0 ~ "^[[:space:]]+-[[:space:]]+" {
      value=$0
      sub("^[[:space:]]+-[[:space:]]+", "", value)
      gsub(/^\"/, "", value)
      gsub(/\"$/, "", value)
      print value
    }
  ' "$file"
}

matrix_field() {
  local phase="$1"
  local field_index="$2"
  [[ -f docs/DELIVERY_PROGRESS_MATRIX.md ]] || return 0
  awk -F'|' -v phase="$phase" -v field_index="$field_index" '
    $2 ~ "^[[:space:]]*" phase "[[:space:]]*$" {
      value=$field_index
      gsub(/^[[:space:]]+|[[:space:]]+$/, "", value)
      print value
      exit
    }
  ' docs/DELIVERY_PROGRESS_MATRIX.md
}

current_state_value() {
  local key="$1"
  [[ -f docs/PROJECT_CURRENT_STATE.md ]] || return 0
  awk -F':' -v key="$key" '
    tolower($1) == tolower(key) {
      value=$0
      sub("^[^:]*:[[:space:]]*", "", value)
      gsub(/^`|`$/, "", value)
      gsub(/^[[:space:]]+|[[:space:]]+$/, "", value)
      sub(/[.]$/, "", value)
      result=value
    }
    END { if (result != "") print result }
  ' docs/PROJECT_CURRENT_STATE.md
}

# Legacy workflow files retained.
require_file "docs/ACTIVE_MAINLINE_STATUS.yml"
require_file "docs/SESSION_BOOTSTRAP.md"
require_file "docs/ANSWER_FORMAT_CONTRACT.md"
require_file "docs/V1_CAPABILITY_MATRIX.md"
require_file "docs/V1_PROGRESS_SOURCE_OF_TRUTH.md"
require_file "docs/WORKFLOW_COMMAND_AUTOMATION.md"
require_file "AGENTS.md"
require_file ".github/pull_request_template.md"
require_file ".github/workflows/workflow-contract.yml"
require_file "scripts/v1-auto.sh"
require_file "scripts/v1-merge-current.sh"
require_file "scripts/v1.sh"
require_file "scripts/v1-status.sh"
require_file "scripts/v1-pr-review-input.sh"
require_file "scripts/v1-merge-sync.sh"
require_file "scripts/v1-safe-check.sh"
require_file "scripts/v1-session-bootstrap.sh"
require_file "scripts/v1-next-pack-context.sh"
require_executable "scripts/v1-auto.sh"
require_executable "scripts/v1-merge-current.sh"
require_executable "scripts/v1.sh"

# New delivery-contract source of truth.
require_file "docs/PROJECT_DELIVERY_CONTRACT.md"
require_file "docs/PROJECT_CURRENT_STATE.md"
require_file "docs/DELIVERY_PROGRESS_MATRIX.md"
require_file "docs/CODEX_TASK_TEMPLATE.md"
require_file "docs/CONTRACT_CHANGE_LOG.md"
require_file "docs/DEAD_CODE_CANDIDATES.md"
require_file "docs/PROJECT_GLOBAL_AUDIT.md"
require_file "docs/PAUSED_TECHNICAL_DEBT_REGISTER.md"

require_contains "AGENTS.md" "docs/PROJECT_DELIVERY_CONTRACT.md"
require_contains "AGENTS.md" "docs/PROJECT_CURRENT_STATE.md"
require_contains "AGENTS.md" "docs/DELIVERY_PROGRESS_MATRIX.md"
require_contains "AGENTS.md" "docs/CODEX_TASK_TEMPLATE.md"
require_contains "AGENTS.md" "task_mode=READ_ONLY_PRODUCT_AUDIT"
require_contains "docs/PROJECT_DELIVERY_CONTRACT.md" "Contract Status: ACTIVE"
require_contains "docs/PROJECT_DELIVERY_CONTRACT.md" "docs-only"
require_contains "docs/PROJECT_DELIVERY_CONTRACT.md" "DTO-only"
require_contains "docs/PROJECT_DELIVERY_CONTRACT.md" "review-only"
require_contains "docs/PROJECT_DELIVERY_CONTRACT.md" "preview-only"
require_contains "docs/PROJECT_DELIVERY_CONTRACT.md" "dashboard-only"
require_contains "docs/DELIVERY_PROGRESS_MATRIX.md" "P0-0"
require_contains "docs/DELIVERY_PROGRESS_MATRIX.md" "Phase Status"
require_contains "docs/DELIVERY_PROGRESS_MATRIX.md" "Existing Module Maturity"
require_contains "docs/PROJECT_CURRENT_STATE.md" "Current Phase: P0-0"
require_contains "docs/PROJECT_CURRENT_STATE.md" "Current Phase Status:"
require_contains "docs/ACTIVE_MAINLINE_STATUS.yml" "compatibility_status: \"DERIVED_ONLY\""
require_contains "docs/CODEX_NEXT_TASK.yml" "compatibility_status: \"DERIVED_ONLY\""
require_contains "docs/WORKFLOW_GITHUB_AUTH_AND_HANDOFF_RULE.md" "A-risk Auto Merge Rule"
require_contains "docs/WORKFLOW_COMMAND_AUTOMATION.md" "A-risk Auto Merge Rule"
require_contains "docs/PROJECT_DELIVERY_CONTRACT.md" "A-risk Auto Merge Rule"
require_contains "docs/ANSWER_FORMAT_CONTRACT.md" "WHAT_THIS_STEP_DOES"
require_contains "docs/ANSWER_FORMAT_CONTRACT.md" "CURRENT_PROGRESS"
require_contains "docs/ANSWER_FORMAT_CONTRACT.md" "NEXT_ALLOWED_ACTION"
require_contains "docs/ANSWER_FORMAT_CONTRACT.md" "NEXT_BLOCKED_ACTION"
require_contains "docs/ANSWER_FORMAT_CONTRACT.md" "OVERREACH_STATUS"
require_contains "docs/ANSWER_FORMAT_CONTRACT.md" "这一步在做什么"
require_contains "docs/ANSWER_FORMAT_CONTRACT.md" "当前进度"
require_contains "docs/ANSWER_FORMAT_CONTRACT.md" "下一允许动作"
require_contains "docs/ANSWER_FORMAT_CONTRACT.md" "下一禁止动作"
require_contains "docs/ANSWER_FORMAT_CONTRACT.md" "越界状态"
require_contains "docs/CODEX_TASK_TEMPLATE.md" "WHAT_THIS_STEP_DOES"
require_contains "docs/CODEX_TASK_TEMPLATE.md" "CURRENT_PROGRESS"
require_contains "docs/CODEX_TASK_TEMPLATE.md" "NEXT_ALLOWED_ACTION"
require_contains "docs/CODEX_TASK_TEMPLATE.md" "NEXT_BLOCKED_ACTION"
require_contains "docs/CODEX_TASK_TEMPLATE.md" "OVERREACH_STATUS"
require_contains "scripts/v1-auto.sh" "WHAT_THIS_STEP_DOES"
require_contains "scripts/codex-next-task.sh" "NEXT_BLOCKED_ACTION"
require_contains "docs/PROJECT_CURRENT_STATE.md" "CURRENT_PACKAGE_PR"
require_contains "docs/PROJECT_CURRENT_STATE.md" "UNRELATED_OPEN_PRS"
require_contains "docs/ACTIVE_MAINLINE_STATUS.yml" "pr_1004_boundary"
require_contains "docs/CODEX_NEXT_TASK.yml" "a_risk_auto_merge_rule"
require_contains "scripts/v1-state.sh" "CURRENT_PACKAGE_PR"
require_contains "scripts/v1-state.sh" "UNRELATED_OPEN_PRS"
require_contains "scripts/v1-state.sh" "BLOCK_NEXT_BUSINESS_PHASE_ONLY"
require_contains "scripts/v1-state.sh" "OPEN_PR_CHECK_SOURCE"
require_contains "scripts/v1-state.sh" "OPEN_PR_COUNT"
require_contains "scripts/v1-state.sh" "OPEN_PR_STATUS"
require_contains "scripts/v1-state.sh" "CLEAN_SYNCED_MAIN"
require_contains "scripts/v1-state.sh" "PRODUCT_AUDIT_ALLOWED"
require_contains "scripts/v1-state.sh" "READ_ONLY_PRODUCT_AUDIT_STATUS"
require_contains "scripts/v1-state.sh" "CURRENT_TASK_MODE"
require_contains "scripts/v1-state.sh" "AUTHORIZED_NEXT_TASK_MODE"
require_contains "scripts/v1-state.sh" "NEXT_TASK_AUTHORIZATION_STATUS"
require_contains "scripts/v1-state.sh" "P1A_TRANSITION_ALLOWED"
require_contains "scripts/v1-state.sh" "P1B_AUTHORIZATION_RUNTIME_STATUS"
require_contains "scripts/v1-state.sh" "ACTIVE_CONFLICTING_OPEN_PRS"
require_contains "scripts/v1-state.sh" "CLOSED_TECHNICAL_DEBT_STATUS"
require_contains "scripts/v1-state.sh" "CLOSED_TECHNICAL_DEBT_EFFECTIVE"
require_contains "scripts/v1-state.sh" "CLOSED_TECHNICAL_DEBT_BLOCKS_AUDIT"
require_contains "scripts/v1-state.sh" "RESOLVED_FROM_STATE"
require_contains "scripts/v1-state.sh" "RESOLVED_PACKAGE"
require_contains "scripts/v1-state.sh" "RESOLVED_MODE"
require_contains "scripts/v1-state.sh" "RESOLVED_EDIT_PERMISSION"
require_not_contains "scripts/v1-state.sh" "classify_paused_pr_scope"
require_not_contains "scripts/v1-state.sh" "PAUSED_PR_SCOPE_RELATION"
require_not_contains "scripts/v1-state.sh" "ALLOWED_WITH_PAUSED_UNRELATED_PR"
require_contains "scripts/v1-state.sh" "--self-test-product-audit-policy"
require_contains "docs/CODEX_NEXT_TASK.yml" "current_task_mode:"
require_contains "docs/CODEX_NEXT_TASK.yml" "authorized_next_task_mode:"
require_contains "docs/CODEX_NEXT_TASK.yml" "current_effective_status:"
require_contains "docs/CODEX_NEXT_TASK.yml" "authorized_next_product_phase:"
require_contains "docs/CODEX_NEXT_TASK.yml" "next_task_authorization_conditions:"
require_contains "docs/CODEX_NEXT_TASK.yml" "p1a_repository_edits_allowed: false"
require_contains "docs/CODEX_NEXT_TASK.yml" "p1a_implementation_allowed: false"
require_contains "docs/CODEX_NEXT_TASK.yml" "p1a_implementation_pr_allowed: false"
require_contains "docs/CODEX_NEXT_TASK.yml" "p1b_authorization_status: \"BLOCKED_"
require_contains "docs/CODEX_NEXT_TASK.yml" "current_package_phase:"
require_contains "docs/CODEX_NEXT_TASK.yml" "current_package_mode:"
require_contains "docs/CODEX_NEXT_TASK.yml" "authorized_next_package_phase:"
require_contains "docs/CODEX_NEXT_TASK.yml" "authorized_next_package_mode:"
require_contains "docs/CODEX_NEXT_TASK.yml" "blocked_package_phase:"
require_contains "scripts/codex-next-task.sh" "RESOLVED_FROM_STATE"
require_contains "scripts/codex-next-task.sh" "RESOLVED_SCOPE_PROFILE"
require_contains "scripts/v1-operator.sh" "OPERATOR_MODE: READ_ONLY_AUDIT"
require_contains "scripts/v1-operator.sh" "REPOSITORY_MUTATION: DISABLED"
require_contains "scripts/v1-operator.sh" "PR_CREATION: DISABLED"
require_contains "docs/CODEX_NEXT_TASK.yml" "audit_scope_modules:"
require_contains "docs/CODEX_NEXT_TASK.yml" "audit_scope_paths:"
require_contains "docs/CODEX_NEXT_TASK.yml" "audit_scope_source_domains:"
require_contains "docs/CODEX_NEXT_TASK.yml" "read_only_product_audit_scope_contract: \"NO_CODE_NO_TEST_NO_BUSINESS_PR_NO_CLOSED_DEBT_CHANGES\""
require_contains "docs/CODEX_NEXT_TASK.yml" "paused_governance_base: \"2552dd24b1b756d5eb517e640baa772e1c5bcab6\""
require_contains "docs/CODEX_NEXT_TASK.yml" "paused_governance_head: \"75d04e95bc7aa5eb761299b0192dfbc2caec3792\""
require_contains "docs/CODEX_NEXT_TASK.yml" "paused_governance_status: \"CLOSED_PAUSED_TECHNICAL_DEBT\""
require_contains "docs/CODEX_NEXT_TASK.yml" "paused_governance_merged_status: \"NOT_MERGED\""
require_contains "docs/CODEX_NEXT_TASK.yml" "paused_governance_effective: false"
require_contains "docs/SESSION_BOOTSTRAP.md" "Closed unmerged technical debt does not block"
require_contains "docs/WORKFLOW_COMMAND_AUTOMATION.md" "Closed unmerged technical debt is not an active blocker"
require_contains "docs/PAUSED_TECHNICAL_DEBT_REGISTER.md" "FE04E-GOVERNANCE-PARSER-PR1156"
require_contains "docs/PAUSED_TECHNICAL_DEBT_REGISTER.md" "CLOSED_PAUSED_TECHNICAL_DEBT"
require_contains "docs/PAUSED_TECHNICAL_DEBT_REGISTER.md" "8_UNRESOLVED_PRESERVED"
require_contains "docs/PAUSED_TECHNICAL_DEBT_REGISTER.md" "b168819d38e46f4fb90131ca92294cb45b5abbf6"
require_contains "docs/PAUSED_TECHNICAL_DEBT_REGISTER.md" "440a2a7f038bb4d2086bb7b0fabba1ca02cc81632a58c99b39283de38550bc0a"
require_contains "docs/PRODUCT_FIELD_SOURCE.md" "REAL_DATA_STATUS=PARTIAL/FALLBACK_PRESENT"
require_contains "docs/PRODUCT_FIELD_SOURCE.md" "fixed base/default values and light-rule adjustments"
require_contains "docs/PRODUCT_COMPLETION_MATRIX.md" "| Eight Scores | PARTIAL"
require_contains "docs/PRODUCT_COMPLETION_MATRIX.md" "| iPhone | FUNCTIONAL_UNVALIDATED"
require_contains "docs/PRODUCT_COMPLETION_MATRIX.md" "simulator install/launch"
require_contains "docs/PRODUCT_GAP_ANALYSIS.md" "The remaining gap is real-device and production validation"
require_contains "docs/PRODUCT_ROADMAP_V2.md" "P1A — Home Alignment Readiness and Gap Audit"
require_contains "docs/PRODUCT_ROADMAP_V2.md" "P1B — Home Alignment First Implementation"
require_contains "scripts/v1-auto.sh" "complete-pr"
require_contains "scripts/v1-pr-complete.sh" "GH_NOT_AVAILABLE_FOR_PR_MERGE"
require_contains "scripts/v1-pr-complete.sh" "A_RISK_SCOPE_OK"
require_contains "scripts/v1-merge-sync.sh" "PR_1004_PROTECTED"

matrix_phase="P0-0"
matrix_status="$(matrix_field P0-0 4)"
current_phase="$(current_state_value "Current Phase")"
current_status="$(current_state_value "Current Phase Status")"
active_phase="$(yaml_value docs/ACTIVE_MAINLINE_STATUS.yml current_phase)"
active_status="$(yaml_value docs/ACTIVE_MAINLINE_STATUS.yml current_phase_status)"
task_phase="$(yaml_value docs/CODEX_NEXT_TASK.yml current_phase)"
task_allowed="$(yaml_value docs/CODEX_NEXT_TASK.yml next_business_phase_allowed)"
active_allowed="$(yaml_value docs/ACTIVE_MAINLINE_STATUS.yml next_business_phase_allowed)"
current_task_mode="$(yaml_value docs/CODEX_NEXT_TASK.yml current_task_mode)"
authorized_next_task_mode="$(yaml_value docs/CODEX_NEXT_TASK.yml authorized_next_task_mode)"
p1a_repository_edits_allowed="$(yaml_value docs/CODEX_NEXT_TASK.yml p1a_repository_edits_allowed)"
p1a_implementation_allowed="$(yaml_value docs/CODEX_NEXT_TASK.yml p1a_implementation_allowed)"
p1a_implementation_pr_allowed="$(yaml_value docs/CODEX_NEXT_TASK.yml p1a_implementation_pr_allowed)"
p1b_authorization_status="$(yaml_value docs/CODEX_NEXT_TASK.yml p1b_authorization_status)"
current_package_phase="$(yaml_value docs/CODEX_NEXT_TASK.yml current_package_phase)"
current_package_mode="$(yaml_value docs/CODEX_NEXT_TASK.yml current_package_mode)"
current_package_branch="$(yaml_value docs/CODEX_NEXT_TASK.yml current_package_branch)"
authorized_next_package_phase="$(yaml_value docs/CODEX_NEXT_TASK.yml authorized_next_package_phase)"
authorized_next_package_mode="$(yaml_value docs/CODEX_NEXT_TASK.yml authorized_next_package_mode)"
authorized_next_package_edits="$(yaml_value docs/CODEX_NEXT_TASK.yml authorized_next_package_repository_edits_allowed)"
authorized_next_package_implementation="$(yaml_value docs/CODEX_NEXT_TASK.yml authorized_next_package_implementation_allowed)"
authorized_next_package_pr="$(yaml_value docs/CODEX_NEXT_TASK.yml authorized_next_package_implementation_pr_allowed)"
blocked_package_phase="$(yaml_value docs/CODEX_NEXT_TASK.yml blocked_package_phase)"
blocked_package_status="$(yaml_value docs/CODEX_NEXT_TASK.yml blocked_package_status)"
current_package_allowed_scope="$(yaml_list docs/CODEX_NEXT_TASK.yml current_package_allowed_scope)"
current_package_blocked_scope="$(yaml_list docs/CODEX_NEXT_TASK.yml current_package_blocked_scope)"
transition_conditions="$(yaml_list docs/CODEX_NEXT_TASK.yml next_task_authorization_conditions)"
audit_scope_modules="$(yaml_list docs/CODEX_NEXT_TASK.yml audit_scope_modules)"
audit_scope_paths="$(yaml_list docs/CODEX_NEXT_TASK.yml audit_scope_paths)"
audit_scope_domains="$(yaml_list docs/CODEX_NEXT_TASK.yml audit_scope_source_domains)"
p1a_allowed_changes="$(yaml_list docs/CODEX_NEXT_TASK.yml p1a_allowed_changes)"

[[ -n "$current_task_mode" ]] || fail "current_task_mode must be declared"
[[ -n "$authorized_next_task_mode" ]] || fail "authorized_next_task_mode must be declared"
[[ "$current_task_mode" != "$authorized_next_task_mode" ]] || fail "current and authorized next task modes must remain distinct"
[[ "$authorized_next_task_mode" == "READ_ONLY_PRODUCT_AUDIT" ]] || fail "authorized next task mode must be READ_ONLY_PRODUCT_AUDIT"
[[ "$p1a_repository_edits_allowed" == "false" ]] || fail "P1A repository edits must remain false"
[[ "$p1a_implementation_allowed" == "false" ]] || fail "P1A implementation must remain false"
[[ "$p1a_implementation_pr_allowed" == "false" ]] || fail "P1A implementation PR creation must remain false"
[[ "$p1b_authorization_status" == BLOCKED_* ]] || fail "P1B must remain blocked"
[[ -n "$current_package_phase" && -n "$current_package_mode" && -n "$current_package_branch" ]] || fail "current package declaration must be complete"
[[ -n "$authorized_next_package_phase" && "$authorized_next_package_phase" != "$current_package_phase" ]] || fail "authorized next package must be distinct"
[[ "$authorized_next_package_mode" == "READ_ONLY_PRODUCT_AUDIT" ]] || fail "authorized next package mode mismatch"
[[ "$authorized_next_package_mode" != "$current_package_mode" ]] || fail "current and authorized next package modes must be distinct"
[[ "$authorized_next_package_edits" == "false" ]] || fail "authorized P1A repository edits must remain false"
[[ "$authorized_next_package_implementation" == "false" ]] || fail "authorized P1A implementation must remain false"
[[ "$authorized_next_package_pr" == "false" ]] || fail "authorized P1A PR creation must remain false"
[[ -n "$blocked_package_phase" && "$blocked_package_phase" != "$current_package_phase" && "$blocked_package_phase" != "$authorized_next_package_phase" && "$blocked_package_status" == BLOCKED_* ]] || fail "blocked successor package declaration mismatch"
[[ -n "$current_package_allowed_scope" && -n "$current_package_blocked_scope" ]] || fail "current package runtime scope must be explicit"
[[ "$p1a_allowed_changes" == "NONE" ]] || fail "P1A allowed changes must be NONE"
[[ -n "$audit_scope_modules" && -n "$audit_scope_paths" && -n "$audit_scope_domains" ]] || fail "machine-readable P1A audit scope must be complete"
for transition_condition in \
  P0_EFFECTIVE_MERGED_MAIN \
  LOCAL_ORIGIN_MAIN_MATCH \
  P0_MERGED_MAIN_VALIDATION_PASS \
  PRODUCT_SOURCE_GATE_PASS \
  CLEAN_WORKTREE \
  NO_ACTIVE_CONFLICTING_PR; do
  printf '%s\n' "$transition_conditions" | grep -Fxq "$transition_condition" \
    || fail "missing P0 to P1A transition condition: $transition_condition"
done

run_handoff_scenario() {
  local scenario="$1"
  V1_WORKFLOW_SELF_TEST=1 V1_HANDOFF_SELF_TEST_SCENARIO="$scenario" bash scripts/codex-next-task.sh
}

assert_handoff_blocked() {
  local scenario="$1"
  local expected_reason="$2"
  local output status
  set +e
  output="$(run_handoff_scenario "$scenario" 2>&1)"
  status=$?
  set -e
  [[ "$status" -ne 0 ]] || fail "handoff scenario must be blocked: $scenario"
  printf '%s\n' "$output" | grep -Fq "RESOLUTION_STATUS: BLOCKED" \
    || fail "blocked handoff scenario omitted blocked status: $scenario"
  printf '%s\n' "$output" | grep -Fq "NEXT_PACKAGE_BLOCK_REASON: $expected_reason" \
    || fail "blocked handoff scenario reason mismatch: $scenario"
  printf '%s\n' "$output" | grep -Fq "GENERATED_TASK: BLOCKED" \
    || fail "blocked handoff scenario generated a task: $scenario"
}

p0_open_handoff="$(run_handoff_scenario p0_open)" || fail "P0 open handoff failed"
printf '%s\n' "$p0_open_handoff" | grep -Fq "RESOLVED_PACKAGE: $current_package_phase" \
  || fail "P0 open handoff did not resolve current P0 package"
printf '%s\n' "$p0_open_handoff" | grep -Fq "RESOLVED_HANDOFF_STAGE: P0_REMEDIATION_REVIEW" \
  || fail "P0 open handoff stage mismatch"
printf '%s\n' "$p0_open_handoff" | grep -Fq "NEXT_PACKAGE_ALLOWED: NO" \
  || fail "P0 open handoff must keep P1A blocked"

p0_ready_handoff="$(run_handoff_scenario p0_ready_unmerged)" || fail "P0 ready handoff failed"
printf '%s\n' "$p0_ready_handoff" | grep -Fq "RESOLVED_PACKAGE: $current_package_phase" \
  || fail "P0 ready handoff did not resolve current P0 package"
printf '%s\n' "$p0_ready_handoff" | grep -Fq "RESOLVED_HANDOFF_STAGE: P0_FINAL_MERGE_PATH" \
  || fail "P0 ready handoff did not resolve final merge path"
printf '%s\n' "$p0_ready_handoff" | grep -Fq "NEXT_PACKAGE_ALLOWED: NO" \
  || fail "P0 ready handoff must keep P1A blocked"

assert_handoff_blocked p0_merged_unsynced BLOCKED_PENDING_LOCAL_ORIGIN_MAIN_MATCH

p1a_handoff="$(run_handoff_scenario p0_merged_validated)" || fail "P0 merged validated handoff failed"
for p1a_expected in \
  "RESOLVED_PACKAGE: $authorized_next_package_phase" \
  "RESOLVED_MODE: READ_ONLY_PRODUCT_AUDIT" \
  "RESOLVED_EDIT_PERMISSION: false" \
  "RESOLVED_IMPLEMENTATION_PERMISSION: false" \
  "RESOLVED_PR_CREATION_PERMISSION: false" \
  "GENERATED_PACKAGE: $authorized_next_package_phase" \
  "GENERATED_BRANCH: NONE_READ_ONLY_AUDIT"; do
  printf '%s\n' "$p1a_handoff" | grep -Fq "$p1a_expected" \
    || fail "P1A handoff omitted: $p1a_expected"
done
if printf '%s\n' "$p1a_handoff" | grep -Fq "GENERATED_PACKAGE: $current_package_phase" \
  || printf '%s\n' "$p1a_handoff" | grep -Fq "GENERATED_BRANCH: $current_package_branch"; then
  fail "post-merge handoff fell back to editable P0"
fi

assert_handoff_blocked p1b_request BLOCKED_REQUESTED_PACKAGE_NOT_AUTHORIZED
assert_handoff_blocked conflicting_pr BLOCKED_ACTIVE_CONFLICTING_PR
assert_handoff_blocked dirty_worktree BLOCKED_WORKTREE_DIRTY
assert_handoff_blocked product_source_failure BLOCKED_PRODUCT_SOURCE_GATE
assert_handoff_blocked unknown_state BLOCKED_UNKNOWN_CURRENT_PACKAGE_PR_STATE

closed_debt_handoff="$(run_handoff_scenario closed_pr_1156)" || fail "closed debt handoff failed"
printf '%s\n' "$closed_debt_handoff" | grep -Fq "RESOLVED_PACKAGE: $authorized_next_package_phase" \
  || fail "closed PR #1156 incorrectly blocked P1A"
if printf '%s\n' "$closed_debt_handoff" | grep -Eq '^GENERATED_(TASK|PACKAGE|BRANCH): .*1156'; then
  fail "closed PR #1156 became generated task context"
fi

operator_branch_before="$(git branch --show-current)"
operator_head_before="$(git rev-parse HEAD)"
operator_status_before="$(git status --porcelain=v1)"
operator_output="$(V1_WORKFLOW_SELF_TEST=1 V1_HANDOFF_SELF_TEST_SCENARIO=p1a_operator bash scripts/v1-operator.sh)" \
  || fail "P1A operator invocation failed"
operator_branch_after="$(git branch --show-current)"
operator_head_after="$(git rev-parse HEAD)"
operator_status_after="$(git status --porcelain=v1)"
for operator_expected in \
  "OPERATOR_MODE: READ_ONLY_AUDIT" \
  "REPOSITORY_MUTATION: DISABLED" \
  "PR_CREATION: DISABLED" \
  "IMPLEMENTATION: DISABLED"; do
  printf '%s\n' "$operator_output" | grep -Fq "$operator_expected" \
    || fail "P1A operator omitted: $operator_expected"
done
[[ "$operator_branch_before" == "$operator_branch_after" ]] || fail "P1A operator changed branch"
[[ "$operator_head_before" == "$operator_head_after" ]] || fail "P1A operator changed HEAD"
[[ "$operator_status_before" == "$operator_status_after" ]] || fail "P1A operator changed worktree or index"

audit_policy_text="$(bash scripts/v1-state.sh --self-test-product-audit-policy)" || fail "product audit policy self-test failed"
printf '%s\n' "$audit_policy_text" | grep -Fq "PRODUCT_AUDIT_POLICY_TESTS: PASS" \
  || fail "product audit policy self-test did not report PASS"
for expected_case in \
  TRANSITION_TEST_P0_OPEN \
  TRANSITION_TEST_P0_READY_UNMERGED \
  TRANSITION_TEST_P0_MERGED_UNSYNCED \
  TRANSITION_TEST_P0_MERGED_VALIDATED \
  TRANSITION_TEST_P1B_REMAINS_BLOCKED \
  AUDIT_POLICY_TEST_CLOSED_UNMERGED_TECHNICAL_DEBT \
  AUDIT_POLICY_TEST_P0_NOT_MERGED \
  AUDIT_POLICY_TEST_CURRENT_PACKAGE_PR \
  AUDIT_POLICY_TEST_ACTIVE_CONFLICTING_PR \
  AUDIT_POLICY_TEST_DIRTY_WORKTREE \
  AUDIT_POLICY_TEST_PRODUCT_SOURCE_GATE_FAILED \
  AUDIT_POLICY_TEST_IMPLEMENTATION_ATTEMPT \
  AUDIT_POLICY_TEST_ATTEMPTED_REPOSITORY_EDIT; do
  printf '%s\n' "$audit_policy_text" | grep -Fq "$expected_case: PASS" \
    || fail "missing product audit policy case: $expected_case"
done

if grep -F '| AI Detail | Scores | eightScores' docs/PRODUCT_FIELD_SOURCE.md | grep -Fq 'functional/partial'; then
  fail "eight-score field source must not retain the false functional/partial claim"
fi
if grep -Fq 'no complete Xcode' docs/PRODUCT_COMPLETION_MATRIX.md docs/PRODUCT_GAP_ANALYSIS.md docs/PRODUCT_BASELINE_FREEZE_REPORT.md docs/PRODUCT_ROADMAP_V2.md; then
  fail "iPhone baseline must not claim that the merged Xcode foundation is absent"
fi

state_text="$(bash scripts/v1-state.sh)"
runtime_completion_effective_state="$(printf '%s\n' "$state_text" | awk -F': ' '$1 == "COMPLETION_EFFECTIVE_STATE" {print $2; exit}')"

[[ "$current_phase" == P0-0* ]] || fail "PROJECT_CURRENT_STATE current phase mismatch: $current_phase"
[[ "$current_status" == "$matrix_status" ]] || fail "PROJECT_CURRENT_STATE current status mismatch: $current_status != $matrix_status"
[[ "$active_phase" == "$matrix_phase" ]] || fail "ACTIVE_MAINLINE_STATUS current_phase mismatch: $active_phase != $matrix_phase"
[[ "$active_status" == "$matrix_status" ]] || fail "ACTIVE_MAINLINE_STATUS current_phase_status mismatch: $active_status != $matrix_status"
[[ "$task_phase" == "$matrix_phase" ]] || fail "CODEX_NEXT_TASK current_phase mismatch: $task_phase != $matrix_phase"

if [[ "$matrix_status" != "DONE" || "$runtime_completion_effective_state" != "EFFECTIVE_MERGED_MAIN" ]]; then
  [[ "$active_allowed" == "false" || "$active_allowed" == "NO" ]] || fail "ACTIVE_MAINLINE_STATUS must block next business phase while P0-0 is not effective"
  [[ "$task_allowed" == "false" || "$task_allowed" == "NO" ]] || fail "CODEX_NEXT_TASK must block next business phase while P0-0 is not effective"
  task_active_block="$(yaml_value docs/CODEX_NEXT_TASK.yml active_block)"
  task_module="$(yaml_value docs/CODEX_NEXT_TASK.yml module)"
  task_next_action="$(yaml_value docs/CODEX_NEXT_TASK.yml next_allowed_action)"
  if printf '%s\n%s\n%s\n' "$task_active_block" "$task_module" "$task_next_action" | grep -Eiq 'P0-1[[:space:]]+UserPosition[[:space:]]+implementation|UserPosition implementation|active_block:[[:space:]]*P0-1|module:[[:space:]]*UserPosition'; then
    fail "CODEX_NEXT_TASK must not point to P0-1/UserPosition implementation while P0-0 is not effective"
  fi
fi

if grep -Eiq 'review-only slice count.*(complete|progress|next)|completed review-only.*next business' scripts/v1-auto.sh scripts/v1-state.sh scripts/codex-next-task.sh docs/CODEX_NEXT_TASK.yml docs/ACTIVE_MAINLINE_STATUS.yml 2>/dev/null; then
  fail "workflow must not use completed review-only slice count as next-task or delivery-completion basis"
fi

changed_files="$({ git diff --name-only 2>/dev/null || true; git diff --cached --name-only 2>/dev/null || true; git diff --name-only origin/main...HEAD 2>/dev/null || true; git diff --name-only HEAD~1..HEAD 2>/dev/null || true; } | sort -u)"
if echo "$changed_files" | grep -Eq 'src/main/java|src/test/java'; then
  require_contains ".github/pull_request_template.md" "Capability Level Before"
  require_contains ".github/pull_request_template.md" "Capability Level After"
fi

if [[ "$failed" -eq 0 ]]; then
  echo "WORKFLOW_CONTRACT_OK"
  exit 0
fi

echo "WORKFLOW_CONTRACT_FAILED"
exit 1
