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
require_contains "scripts/v1-state.sh" "AUTHORIZATION_STATUS:"
require_contains "scripts/v1-state.sh" "REQUESTED_PACKAGE:"
require_contains "scripts/v1-state.sh" "ACTIVE_CONFLICTING_OPEN_PRS"
require_contains "scripts/v1-state.sh" "CLOSED_TECHNICAL_DEBT_STATUS"
require_contains "scripts/v1-state.sh" "CLOSED_TECHNICAL_DEBT_EFFECTIVE"
require_contains "scripts/v1-state.sh" "CLOSED_TECHNICAL_DEBT_BLOCKS_AUDIT"
require_contains "scripts/v1-state.sh" "RESOLVED_FROM_STATE"
require_contains "scripts/v1-state.sh" "RESOLVED_PACKAGE"
require_contains "scripts/v1-state.sh" "RESOLVED_MODE"
require_contains "scripts/v1-state.sh" "RESOLVED_EDIT_PERMISSION"
require_contains "scripts/v1-state.sh" "CURRENT_PACKAGE_ACTION_ALLOWED"
require_contains "scripts/v1-state.sh" "MACHINE_AUTHORIZED_PACKAGE"
require_contains "scripts/v1-state.sh" "MACHINE_AUTHORIZED_BRANCH"
require_contains "scripts/v1-state.sh" "MACHINE_AUTHORIZED_STARTING_FULL_SHA"
require_contains "scripts/v1-state.sh" "CURRENT_PACKAGE_MATCH"
require_contains "scripts/v1-state.sh" "CURRENT_BRANCH_MATCH"
require_contains "scripts/v1-state.sh" "CURRENT_STARTING_SHA_MATCH"
require_contains "scripts/v1-state.sh" "--self-test-exact-machine-gate"
require_contains "scripts/v1-state.sh" "CURRENT_PACKAGE_BLOCK_REASON"
require_contains "scripts/v1-state.sh" "OPEN_PR_EVIDENCE_SOURCE"
require_contains "scripts/v1-state.sh" "OPEN_PR_NONE_CONFIRMED"
require_contains "scripts/v1-state.sh" "ACTIVE_CONFLICTING_PRS"
require_contains "scripts/v1-state.sh" "REQUEST_CLASS"
require_contains "scripts/v1-state.sh" "CURRENT_PACKAGE:"
require_contains "scripts/v1-state.sh" "AUTHORIZED_NEXT_PACKAGE:"
require_contains "scripts/v1-state.sh" "REPOSITORY_EDITS_ALLOWED:"
require_contains "scripts/v1-state.sh" "IMPLEMENTATION_ALLOWED:"
require_contains "scripts/v1-state.sh" "PR_CREATION_ALLOWED:"
require_contains "scripts/v1-state.sh" "BLOCKED_UNKNOWN_RESOLVED_STATE"
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
require_contains "docs/CODEX_NEXT_TASK.yml" "p1b_authorization_status: \"EFFECTIVE_MERGED_MAIN\""
require_contains "docs/CODEX_NEXT_TASK.yml" "current_package_phase:"
require_contains "docs/CODEX_NEXT_TASK.yml" "current_package_mode:"
require_contains "docs/CODEX_NEXT_TASK.yml" "authorized_next_package_phase:"
require_contains "docs/CODEX_NEXT_TASK.yml" "authorized_next_package_mode:"
require_contains "docs/CODEX_NEXT_TASK.yml" "current_package_starting_full_sha:"
require_contains "docs/CODEX_NEXT_TASK.yml" "authorized_next_package_starting_full_sha:"
require_contains "docs/CODEX_NEXT_TASK.yml" "current_package_allowed_paths:"
require_contains "docs/CODEX_NEXT_TASK.yml" "blocked_package_phase:"
require_contains "scripts/codex-next-task.sh" "RESOLVED_FROM_STATE"
require_contains "scripts/codex-next-task.sh" "RESOLVED_SCOPE_PROFILE"
require_contains "scripts/codex-next-task.sh" "CURRENT_PACKAGE_ACTION_ALLOWED"
require_contains "scripts/codex-next-task.sh" "OPEN_PR_EVIDENCE_SOURCE"
require_contains "scripts/v1-operator.sh" "OPERATOR_MODE: READ_ONLY_AUDIT"
require_contains "scripts/v1-operator.sh" "OPERATOR_MODE: CURRENT_PACKAGE_CONTINUATION"
require_contains "scripts/v1-operator.sh" "OPERATOR_MODE: IMPLEMENTATION"
require_contains "scripts/v1-operator.sh" "REPOSITORY_MUTATION: DISABLED"
require_contains "scripts/v1-operator.sh" "PR_CREATION: DISABLED"
require_contains "scripts/v1-operator.sh" "OPERATOR_RESULT_STATUS: PASS"
require_contains "scripts/v1-operator.sh" "BLOCKED_UNKNOWN_RESOLVED_STATE"
require_contains "scripts/v1-codex-run-next.sh" "scripts/v1-operator.sh"
require_contains "scripts/v1-codex-run-next.sh" "OUTER_LAUNCHER_STATUS: PASS"
require_contains "scripts/v1-codex-run-next.sh" "--request-package"
require_contains "scripts/v1-codex-run-next.sh" "PRODUCT_SOURCE_GATE_STATUS"
require_not_contains "scripts/v1-codex-run-next.sh" "scripts/v1-auto.sh next"
require_not_contains "scripts/v1-codex-run-next.sh" "当前分支不是 main"
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
require_file "docs/P1A_HOME_ALIGNMENT_AUDIT.md"
require_file "docs/P1B_AUTHORIZATION_SCOPE.md"
require_file "docs/P1B_HOME_CORE_DATA_AUTHORIZATION.md"
require_file "docs/P2_POSITION_MONITORING_BACKEND_AUTHORIZATION.md"
require_file "docs/P2_POSITION_MONITORING_AUTHORIZATION_VALIDATION.md"
require_file "docs/FUNDAMENTAL_AI_V4_1_DECISION_CHAIN_AUTHORIZATION.md"
require_file "docs/FUNDAMENTAL_AI_V4_1_OBJECT_OWNERSHIP_MAP.md"
require_file "docs/FUNDAMENTAL_AI_V4_1_FINAL_INTERACTION_PAGE_AND_RUNTIME_AUTHORIZATION.md"
require_file "docs/FUNDAMENTAL_AI_V4_1_FINAL_INTERACTION_OBJECT_OWNERSHIP_MAP.md"
require_file "docs/FUNDAMENTAL_AI_V4_1_PAGE_ROUTE_COMPONENT_MATRIX.md"
require_file "docs/FUNDAMENTAL_AI_V4_1_PR1179_REUSE_AND_SUPERSESSION_MAP.md"
require_file "docs/FUNDAMENTAL_AI_V4_1_FINAL_INTERACTION_AUTHORIZATION_VALIDATION.md"
require_file "docs/FUNDAMENTAL_AI_V4_1_VISUAL_DENSITY_AND_PROPORTION_CONTRACT.md"
require_file "docs/FUNDAMENTAL_AI_V4_1_CANONICAL_FIGMA_AUTHORIZATION_SCOPE_RECONCILIATION.md"
require_file "docs/FUNDAMENTAL_AI_V4_1_TARGET_RUNTIME_BLOCKER_SOURCE_MAPPING.md"
require_file "docs/FUNDAMENTAL_AI_V4_1_TARGET_RUNTIME_BLOCKER_OWNERSHIP_MAP.md"
require_file "docs/FUNDAMENTAL_AI_V4_1_TARGET_RUNTIME_BLOCKER_REMEDIATION_AUTHORIZATION.md"
require_file "docs/FUNDAMENTAL_AI_V4_1_TARGET_RUNTIME_BLOCKER_REMEDIATION_AUTHORIZATION_VALIDATION.md"
require_file "docs/FUNDAMENTAL_AI_V4_1_TELEGRAM_SOURCE_MAPPING.md"
require_file "docs/FUNDAMENTAL_AI_V4_1_TELEGRAM_OWNERSHIP_MAP.md"
require_file "docs/FUNDAMENTAL_AI_V4_1_TELEGRAM_HIGH_VALUE_ALERT_AUTHORIZATION.md"
require_file "docs/FUNDAMENTAL_AI_V4_1_TELEGRAM_AUTHORIZATION_VALIDATION.md"
require_file "docs/FUNDAMENTAL_AI_LOCAL_REAL_SOURCE_MAPPING.md"
require_file "docs/FUNDAMENTAL_AI_LOCAL_REAL_OWNERSHIP_MAP.md"
require_file "docs/FUNDAMENTAL_AI_LOCAL_REAL_READINESS_AUTHORIZATION.md"
require_file "docs/FUNDAMENTAL_AI_LOCAL_REAL_AUTHORIZATION_VALIDATION.md"
require_file "docs/FUNDAMENTAL_AI_FRONTEND_INTERACTION_RUNTIME_CLOSURE_SOURCE_MAPPING.md"
require_file "docs/FUNDAMENTAL_AI_FRONTEND_INTERACTION_RUNTIME_CLOSURE_OWNERSHIP_MAP.md"
require_file "docs/FUNDAMENTAL_AI_FRONTEND_INTERACTION_RUNTIME_CLOSURE_AUTHORIZATION.md"
require_file "docs/FUNDAMENTAL_AI_FRONTEND_INTERACTION_RUNTIME_CLOSURE_AUTHORIZATION_VALIDATION.md"
require_file "docs/TRINE_LOGIC_MULTI_USER_ACCOUNT_REGISTRATION_SOURCE_MAPPING.md"
require_file "docs/TRINE_LOGIC_MULTI_USER_ACCOUNT_REGISTRATION_OWNERSHIP_MAP.md"
require_file "docs/TRINE_LOGIC_MULTI_USER_ACCOUNT_REGISTRATION_AUTHORIZATION.md"
require_file "docs/TRINE_LOGIC_MULTI_USER_ACCOUNT_REGISTRATION_AUTHORIZATION_VALIDATION.md"
require_file "docs/TRINE_LOGIC_TELEGRAM_TWO_CATEGORY_REMEDIATION_SOURCE_MAPPING.md"
require_file "docs/TRINE_LOGIC_TELEGRAM_TWO_CATEGORY_REMEDIATION_OWNERSHIP_MAP.md"
require_file "docs/TRINE_LOGIC_TELEGRAM_TWO_CATEGORY_REMEDIATION_AUTHORIZATION.md"
require_file "docs/TRINE_LOGIC_TELEGRAM_TWO_CATEGORY_REMEDIATION_AUTHORIZATION_VALIDATION.md"
require_file "docs/TRINE_LOGIC_CORE_PRODUCTION_LOOP_AUTOMATION_SOURCE_MAPPING.md"
require_file "docs/TRINE_LOGIC_CORE_PRODUCTION_LOOP_AUTOMATION_OWNERSHIP_MAP.md"
require_file "docs/TRINE_LOGIC_CORE_PRODUCTION_LOOP_AUTOMATION_AUTHORIZATION.md"
require_file "docs/TRINE_LOGIC_CORE_PRODUCTION_LOOP_AUTOMATION_AUTHORIZATION_VALIDATION.md"
require_file "docs/product-sources/FUNDAMENTAL_AI_V4_1_DECISION_CHAIN.md"
require_contains "docs/P1A_HOME_ALIGNMENT_AUDIT.md" "P1A_COMPLETION_STATUS: COMPLETED"
require_contains "docs/P1B_AUTHORIZATION_SCOPE.md" "HOME_READ_PROJECTION_ONLY"
require_contains "docs/P1B_HOME_CORE_DATA_AUTHORIZATION.md" "P1B_HOME_CORE_DATA_COMPLETION"
require_contains "docs/P1B_HOME_CORE_DATA_AUTHORIZATION.md" "Seven-Field Primary Card Body"
require_contains "docs/P1B_HOME_CORE_DATA_AUTHORIZATION.md" "Four-Field Secondary Status Strip"
require_contains "docs/P2_POSITION_MONITORING_BACKEND_AUTHORIZATION.md" "P2_POSITION_MONITORING_BACKEND_IMPLEMENTATION"
require_contains "docs/P2_POSITION_MONITORING_BACKEND_AUTHORIZATION.md" "automatic open, close"
require_contains "scripts/v1-state.sh" "P2_POSITION_MONITORING_AUTHORIZATION_STATUS"
require_contains "docs/FUNDAMENTAL_AI_V4_1_DECISION_CHAIN_AUTHORIZATION.md" "HISTORICAL_REFERENCE_ONLY / SUPERSEDED"
require_contains "docs/FUNDAMENTAL_AI_V4_1_FINAL_INTERACTION_PAGE_AND_RUNTIME_AUTHORIZATION.md" "FUNDAMENTAL_AI_V4_1_FINAL_INTERACTION_PAGE_AND_RUNTIME_IMPLEMENTATION"
require_contains "docs/FUNDAMENTAL_AI_V4_1_FINAL_INTERACTION_PAGE_AND_RUNTIME_AUTHORIZATION.md" "automatically open, close"
require_contains "docs/FUNDAMENTAL_AI_V4_1_FINAL_INTERACTION_PAGE_AND_RUNTIME_AUTHORIZATION.md" "rdMYmsAvZYkXHJX8hdl7UN"
require_contains "docs/FUNDAMENTAL_AI_V4_1_VISUAL_DENSITY_AND_PROPORTION_CONTRACT.md" '70:30'
require_contains "docs/FUNDAMENTAL_AI_V4_1_VISUAL_DENSITY_AND_PROPORTION_CONTRACT.md" '`SUPERSEDED`'
require_contains "docs/FUNDAMENTAL_AI_V4_1_VISUAL_DENSITY_AND_PROPORTION_CONTRACT.md" '| Position Monitoring | 60% | 58-62% |'
require_contains "docs/FUNDAMENTAL_AI_V4_1_FINAL_INTERACTION_OBJECT_OWNERSHIP_MAP.md" "Duplicate Skeleton Gate"
require_contains "scripts/v1-state.sh" "V4_1_FINAL_INTERACTION_AUTHORIZATION_STATUS"
require_contains "scripts/v1-state.sh" "V4_1_TARGET_RUNTIME_REMEDIATION_AUTHORIZATION_STATUS"
require_contains "docs/FUNDAMENTAL_AI_V4_1_TARGET_RUNTIME_BLOCKER_REMEDIATION_AUTHORIZATION.md" "FUNDAMENTAL_AI_V4_1_TARGET_RUNTIME_BLOCKER_REMEDIATION"
require_contains "docs/FUNDAMENTAL_AI_V4_1_TARGET_RUNTIME_BLOCKER_REMEDIATION_AUTHORIZATION.md" "COINGLASS_SECRET_REPOSITORY_WRITE_ALLOWED=false"
require_contains "docs/FUNDAMENTAL_AI_V4_1_TARGET_RUNTIME_BLOCKER_OWNERSHIP_MAP.md" "Duplicate Skeleton Gate"
require_contains "scripts/v1-state.sh" "V4_1_TELEGRAM_AUTHORIZATION_STATUS"
require_contains "docs/FUNDAMENTAL_AI_V4_1_TELEGRAM_HIGH_VALUE_ALERT_AUTHORIZATION.md" "FUNDAMENTAL_AI_V4_1_TELEGRAM_HIGH_VALUE_ALERT_CHANNEL_INTEGRATION"
require_contains "docs/FUNDAMENTAL_AI_V4_1_TELEGRAM_HIGH_VALUE_ALERT_AUTHORIZATION.md" "TELEGRAM_SECRET_FILE_READ_ALLOWED=false"
require_contains "docs/FUNDAMENTAL_AI_V4_1_TELEGRAM_OWNERSHIP_MAP.md" "Duplicate Skeleton Gate"
require_contains "scripts/v1-state.sh" "LOCAL_REAL_AUTHORIZATION_STATUS"
require_contains "docs/FUNDAMENTAL_AI_LOCAL_REAL_READINESS_AUTHORIZATION.md" "LOCAL_REAL_READINESS_SYNC_AND_REAL_ANALYSIS_ENABLEMENT"
require_contains "docs/FUNDAMENTAL_AI_LOCAL_REAL_OWNERSHIP_MAP.md" "Duplicate Skeleton Gate"
require_contains "scripts/v1-state.sh" "FRONTEND_INTERACTION_AUTHORIZATION_STATUS"
require_contains "docs/FUNDAMENTAL_AI_FRONTEND_INTERACTION_RUNTIME_CLOSURE_AUTHORIZATION.md" "FRONTEND_INTERACTION_RUNTIME_CLOSURE"
require_contains "docs/FUNDAMENTAL_AI_FRONTEND_INTERACTION_RUNTIME_CLOSURE_OWNERSHIP_MAP.md" "Duplicate Skeleton Gate"
require_contains "scripts/v1-state.sh" "MULTI_USER_AUTHORIZATION_STATUS"
require_contains "docs/TRINE_LOGIC_MULTI_USER_ACCOUNT_REGISTRATION_AUTHORIZATION.md" "MULTI_USER_ACCOUNT_REGISTRATION_CLOSURE"
require_contains "docs/TRINE_LOGIC_MULTI_USER_ACCOUNT_REGISTRATION_OWNERSHIP_MAP.md" "Duplicate Skeleton Gate"
require_contains "docs/TRINE_LOGIC_MULTI_USER_ACCOUNT_REGISTRATION_OWNERSHIP_MAP.md" "USER_OWNED"
require_contains "scripts/v1-state.sh" "V4_1_TELEGRAM_REMEDIATION_AUTHORIZATION_STATUS"
require_contains "docs/TRINE_LOGIC_TELEGRAM_TWO_CATEGORY_REMEDIATION_AUTHORIZATION.md" "FUNDAMENTAL_AI_V4_1_TELEGRAM_HIGH_VALUE_ALERT_CHANNEL_INTEGRATION"
require_contains "docs/TRINE_LOGIC_TELEGRAM_TWO_CATEGORY_REMEDIATION_AUTHORIZATION.md" "three in-application Message categories remain available"
require_contains "docs/TRINE_LOGIC_TELEGRAM_TWO_CATEGORY_REMEDIATION_AUTHORIZATION.md" "Only these two categories may be connected"
require_contains "docs/TRINE_LOGIC_TELEGRAM_TWO_CATEGORY_REMEDIATION_OWNERSHIP_MAP.md" "Duplicate Skeleton Gate"
require_contains "scripts/v1-state.sh" "V4_1_CORE_PRODUCTION_LOOP_AUTHORIZATION_STATUS"
require_contains "docs/TRINE_LOGIC_CORE_PRODUCTION_LOOP_AUTOMATION_AUTHORIZATION.md" "FUNDAMENTAL_AI_V4_1_CORE_PRODUCTION_LOOP_AUTOMATION"
require_contains "docs/TRINE_LOGIC_CORE_PRODUCTION_LOOP_AUTOMATION_AUTHORIZATION.md" "observing"
require_contains "docs/TRINE_LOGIC_CORE_PRODUCTION_LOOP_AUTOMATION_AUTHORIZATION.md" "OPEN"
require_contains "docs/TRINE_LOGIC_CORE_PRODUCTION_LOOP_AUTOMATION_AUTHORIZATION.md" 'Automatic trading capability count remains `0`'
require_contains "docs/TRINE_LOGIC_CORE_PRODUCTION_LOOP_AUTOMATION_OWNERSHIP_MAP.md" "Duplicate Skeleton Gate"
require_contains "scripts/v1-auto.sh" "complete-pr"
require_contains "scripts/v1-pr-complete.sh" "GH_NOT_AVAILABLE_FOR_PR_MERGE"
require_contains "scripts/v1-pr-complete.sh" "A_RISK_SCOPE_OK"
require_contains "scripts/v1-merge-sync.sh" "PR_1004_PROTECTED"

product_first_stop_rule_files=(
  AGENTS.md
  docs/PRODUCT_SOURCE_OF_TRUTH.md
  docs/PRODUCT_ROADMAP_V2.md
  docs/PRODUCT_ACCEPTANCE_STANDARD.md
  docs/ANSWER_FORMAT_CONTRACT.md
  docs/SESSION_BOOTSTRAP.md
)
for stop_rule_file in "${product_first_stop_rule_files[@]}"; do
  require_contains "$stop_rule_file" "PRODUCT_FIRST_STOP_RULE"
  require_contains "$stop_rule_file" "BLOCKER_CLASS:"
  require_contains "$stop_rule_file" "PRODUCT_SEMANTIC_BLOCKER"
  require_contains "$stop_rule_file" "SECURITY_OR_PRIVACY_BLOCKER"
  require_contains "$stop_rule_file" "REAL_DATA_INTEGRITY_BLOCKER"
  require_contains "$stop_rule_file" "NEXT_PRODUCT_STAGE_BLOCKER"
  require_contains "$stop_rule_file" "BUILD_OR_RUNTIME_BLOCKER"
  require_contains "$stop_rule_file" "NON_BLOCKING_TECHNICAL_DEBT"
  require_contains "$stop_rule_file" "BLOCKS_CURRENT_STAGE: NO"
  require_contains "$stop_rule_file" "PRODUCT_WORK_RATIO:"
  require_contains "$stop_rule_file" "NON_PRODUCT_WORK_RATIO:"
  require_contains "$stop_rule_file" "STOP_RULE_TRIGGERED: YES / NO"
done
require_contains "docs/PRODUCT_ROADMAP_V2.md" "at most an estimated 10% of a product stage"
require_contains "docs/PRODUCT_ACCEPTANCE_STANDARD.md" "at most an estimated 10% of a product stage"
require_contains "docs/PRODUCT_SOURCE_OF_TRUTH.md" "naming preference -> \`NON_BLOCKING_TECHNICAL_DEBT\` -> \`BLOCKS_CURRENT_STAGE: NO\`"
require_contains "docs/PRODUCT_SOURCE_OF_TRUTH.md" "reproducible cross-user data leak -> \`SECURITY_OR_PRIVACY_BLOCKER\` -> \`BLOCKS_CURRENT_STAGE: YES\`"
require_contains "docs/PRODUCT_SOURCE_OF_TRUTH.md" "reproducible post-merge P1A deadlock -> \`NEXT_PRODUCT_STAGE_BLOCKER\` -> \`BLOCKS_CURRENT_STAGE: YES\`"

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
p1b_1_status="$(yaml_value docs/CODEX_NEXT_TASK.yml p1b_1_status)"
home_core_data_authorization_status="$(yaml_value docs/CODEX_NEXT_TASK.yml p1b_home_core_data_authorization_status)"
home_core_data_implementation_status="$(yaml_value docs/CODEX_NEXT_TASK.yml p1b_home_core_data_implementation_status)"
product_p1b_status="$(yaml_value docs/CODEX_NEXT_TASK.yml product_p1b_status)"
p2_matrix_status="$(matrix_field "Product P2" 5)"
p2_authorization_status="$(yaml_value docs/CODEX_NEXT_TASK.yml p2_position_monitoring_authorization_status)"
p2_implementation_status="$(yaml_value docs/CODEX_NEXT_TASK.yml p2_position_monitoring_implementation_status)"
v4_1_matrix_authorization="$(matrix_field "Product v4.1" 5)"
v4_1_design_status="$(yaml_value docs/CODEX_NEXT_TASK.yml v4_1_final_interaction_design_status)"
v4_1_authorization_status="$(yaml_value docs/CODEX_NEXT_TASK.yml v4_1_final_interaction_authorization_status)"
v4_1_implementation_status="$(yaml_value docs/CODEX_NEXT_TASK.yml v4_1_final_interaction_implementation_status)"
v4_1_target_authorization_status="$(yaml_value docs/CODEX_NEXT_TASK.yml v4_1_target_runtime_remediation_authorization_status)"
v4_1_target_implementation_status="$(yaml_value docs/CODEX_NEXT_TASK.yml v4_1_target_runtime_remediation_implementation_status)"
v4_1_target_status="$(yaml_value docs/CODEX_NEXT_TASK.yml v4_1_target_runtime_status)"
v4_1_telegram_authorization_status="$(yaml_value docs/CODEX_NEXT_TASK.yml v4_1_telegram_authorization_status)"
v4_1_telegram_implementation_status="$(yaml_value docs/CODEX_NEXT_TASK.yml v4_1_telegram_implementation_status)"
v4_1_telegram_live_acceptance_status="$(yaml_value docs/CODEX_NEXT_TASK.yml v4_1_telegram_live_acceptance_status)"
v4_1_telegram_remediation_authorization_status="$(yaml_value docs/CODEX_NEXT_TASK.yml v4_1_telegram_remediation_authorization_status)"
v4_1_telegram_remediation_implementation_status="$(yaml_value docs/CODEX_NEXT_TASK.yml v4_1_telegram_remediation_implementation_status)"
v4_1_core_production_loop_authorization_status="$(yaml_value docs/CODEX_NEXT_TASK.yml v4_1_core_production_loop_authorization_status)"
v4_1_core_production_loop_implementation_status="$(yaml_value docs/CODEX_NEXT_TASK.yml v4_1_core_production_loop_implementation_status)"
v4_1_machine_gate_owner_amendment_status="$(yaml_value docs/CODEX_NEXT_TASK.yml v4_1_machine_gate_owner_amendment_status)"
real_data_home_blocker_closure_authorization_status="$(yaml_value docs/CODEX_NEXT_TASK.yml real_data_home_blocker_closure_authorization_status)"
real_data_home_blocker_closure_implementation_status="$(yaml_value docs/CODEX_NEXT_TASK.yml real_data_home_blocker_closure_implementation_status)"
local_real_authorization_status="$(yaml_value docs/CODEX_NEXT_TASK.yml local_real_authorization_status)"
local_real_implementation_status="$(yaml_value docs/CODEX_NEXT_TASK.yml local_real_implementation_status)"
frontend_interaction_authorization_status="$(yaml_value docs/CODEX_NEXT_TASK.yml frontend_interaction_authorization_status)"
frontend_interaction_implementation_status="$(yaml_value docs/CODEX_NEXT_TASK.yml frontend_interaction_implementation_status)"
multi_user_authorization_status="$(yaml_value docs/CODEX_NEXT_TASK.yml multi_user_authorization_status)"
multi_user_implementation_status="$(yaml_value docs/CODEX_NEXT_TASK.yml multi_user_implementation_status)"
current_package_phase="$(yaml_value docs/CODEX_NEXT_TASK.yml current_package_phase)"
current_package_mode="$(yaml_value docs/CODEX_NEXT_TASK.yml current_package_mode)"
current_package_status="$(yaml_value docs/CODEX_NEXT_TASK.yml current_package_status)"
current_package_branch="$(yaml_value docs/CODEX_NEXT_TASK.yml current_package_branch)"
current_package_starting_full_sha="$(yaml_value docs/CODEX_NEXT_TASK.yml current_package_starting_full_sha)"
authorized_next_package_phase="$(yaml_value docs/CODEX_NEXT_TASK.yml authorized_next_package_phase)"
authorized_next_package_mode="$(yaml_value docs/CODEX_NEXT_TASK.yml authorized_next_package_mode)"
authorized_next_package_branch="$(yaml_value docs/CODEX_NEXT_TASK.yml authorized_next_package_branch)"
authorized_next_package_starting_full_sha="$(yaml_value docs/CODEX_NEXT_TASK.yml authorized_next_package_starting_full_sha)"
authorized_next_package_edits="$(yaml_value docs/CODEX_NEXT_TASK.yml authorized_next_package_repository_edits_allowed)"
authorized_next_package_implementation="$(yaml_value docs/CODEX_NEXT_TASK.yml authorized_next_package_implementation_allowed)"
authorized_next_package_pr="$(yaml_value docs/CODEX_NEXT_TASK.yml authorized_next_package_implementation_pr_allowed)"
authorized_next_package_canonical_figma="$(yaml_value docs/CODEX_NEXT_TASK.yml authorized_next_package_canonical_figma_desktop_implementation_allowed)"
authorized_next_package_mobile="$(yaml_value docs/CODEX_NEXT_TASK.yml authorized_next_package_mobile_implementation_allowed)"
authorized_next_package_canonical_figma_key="$(yaml_value docs/CODEX_NEXT_TASK.yml authorized_next_package_canonical_figma_file_key)"
blocked_package_phase="$(yaml_value docs/CODEX_NEXT_TASK.yml blocked_package_phase)"
blocked_package_status="$(yaml_value docs/CODEX_NEXT_TASK.yml blocked_package_status)"
p1b_scope="$(yaml_value docs/CODEX_NEXT_TASK.yml scope)"
current_package_allowed_scope="$(yaml_list docs/CODEX_NEXT_TASK.yml current_package_allowed_scope)"
current_package_allowed_paths="$(yaml_list docs/CODEX_NEXT_TASK.yml current_package_allowed_paths)"
current_package_blocked_scope="$(yaml_list docs/CODEX_NEXT_TASK.yml current_package_blocked_scope)"
transition_conditions="$(yaml_list docs/CODEX_NEXT_TASK.yml next_task_authorization_conditions)"
audit_scope_modules="$(yaml_list docs/CODEX_NEXT_TASK.yml audit_scope_modules)"
audit_scope_paths="$(yaml_list docs/CODEX_NEXT_TASK.yml audit_scope_paths)"
audit_scope_domains="$(yaml_list docs/CODEX_NEXT_TASK.yml audit_scope_source_domains)"
p1a_allowed_changes="$(yaml_list docs/CODEX_NEXT_TASK.yml p1a_allowed_changes)"

[[ -n "$current_task_mode" ]] || fail "current_task_mode must be declared"
[[ -n "$authorized_next_task_mode" ]] || fail "authorized_next_task_mode must be declared"
[[ "$current_task_mode" != "$authorized_next_task_mode" ]] || fail "current and authorized next task modes must remain distinct"
[[ "$current_task_mode" == "DOCS_GATE_OWNER_AMENDMENT" ]] || fail "current task mode must be the explicit Owner gate amendment"
[[ "$authorized_next_task_mode" == "IMPLEMENTATION" ]] || fail "authorized next task mode must be IMPLEMENTATION"
[[ "$p1a_repository_edits_allowed" == "false" ]] || fail "P1A repository edits must remain false"
[[ "$p1a_implementation_allowed" == "false" ]] || fail "P1A implementation must remain false"
[[ "$p1a_implementation_pr_allowed" == "false" ]] || fail "P1A implementation PR creation must remain false"
[[ "$p1b_authorization_status" == "EFFECTIVE_MERGED_MAIN" ]] || fail "P1B-1 authorization must be effective on merged main"
[[ "$p1b_1_status" == "EFFECTIVE_MERGED_MAIN" ]] || fail "P1B-1 predecessor must be effective on merged main"
[[ "$home_core_data_authorization_status" == "EFFECTIVE_MERGED_MAIN" ]] || fail "Home Core Data authorization must remain effective on merged main"
[[ "$home_core_data_implementation_status" == "COMPLETE" ]] || fail "Home Core Data implementation must remain complete"
[[ "$product_p1b_status" == "COMPLETE" ]] || fail "Product P1B must remain complete"
[[ "$p2_matrix_status" == "AUTHORIZED_TO_IMPLEMENT" ]] || fail "Product P2 matrix authorization is missing"
[[ "$p2_authorization_status" == "EFFECTIVE_MERGED_MAIN" ]] || fail "P2 authorization compatibility evidence must remain effective"
[[ "$p2_implementation_status" == "COMPLETE" ]] || fail "P2 backend implementation compatibility evidence must remain complete"
[[ "$v4_1_matrix_authorization" == "AUTHORIZED_TO_IMPLEMENT" ]] || fail "v4.1 matrix authorization is missing"
[[ "$v4_1_design_status" == "FROZEN" ]] || fail "v4.1 Product Design must remain frozen"
[[ "$v4_1_authorization_status" == "EFFECTIVE_MERGED_MAIN" ]] || fail "v4.1 Final Interaction authorization must remain effective"
[[ "$v4_1_implementation_status" == "COMPLETE" ]] || fail "v4.1 Final Interaction implementation must remain complete"
[[ "$v4_1_target_authorization_status" == "EFFECTIVE_MERGED_MAIN" ]] || fail "v4.1 target-runtime authorization must remain effective"
[[ "$v4_1_target_implementation_status" == "COMPLETE" ]] || fail "v4.1 target-runtime remediation must remain complete"
[[ "$v4_1_target_status" == "PENDING_PRIVATE_CONFIGURATION_AND_ACCEPTANCE" ]] || fail "v4.1 target runtime status mismatch"
[[ "$v4_1_telegram_authorization_status" == "EFFECTIVE_MERGED_MAIN" ]] || fail "v4.1 Telegram authorization must remain effective"
[[ "$v4_1_telegram_implementation_status" == "COMPLETE" ]] || fail "v4.1 Telegram integration must remain complete"
[[ "$v4_1_telegram_remediation_authorization_status" == "EFFECTIVE_MERGED_MAIN" ]] || fail "v4.1 Telegram remediation authorization must remain effective on merged main"
[[ "$v4_1_telegram_remediation_implementation_status" == "NOT_STARTED" ]] || fail "v4.1 Telegram remediation implementation must remain not started"
[[ "$v4_1_core_production_loop_authorization_status" == "EFFECTIVE_MERGED_MAIN" ]] || fail "v4.1 core production-loop authorization must remain effective on merged main"
[[ "$v4_1_core_production_loop_implementation_status" == "NOT_STARTED" ]] || fail "v4.1 core production-loop implementation must remain not started"
[[ "$v4_1_machine_gate_owner_amendment_status" == "AUTHORIZED_PENDING_MERGED_MAIN" ]] || fail "machine-gate owner amendment must remain pending merged-main effectivity"
[[ "$real_data_home_blocker_closure_authorization_status" == "PENDING_MACHINE_GATE_OWNER_AMENDMENT_MERGED_MAIN" ]] || fail "B01-B04 successor authorization must remain pending the gate amendment merge"
[[ "$real_data_home_blocker_closure_implementation_status" == "NOT_STARTED" ]] || fail "B01-B04 successor must remain not started"
[[ "$local_real_authorization_status" == "EFFECTIVE_MERGED_MAIN" ]] || fail "local-real authorization must remain effective on merged main"
[[ "$local_real_implementation_status" == "COMPLETE" ]] || fail "local-real implementation must remain complete"
[[ "$frontend_interaction_authorization_status" == "EFFECTIVE_MERGED_MAIN" ]] || fail "frontend interaction authorization must be effective on merged main"
[[ "$frontend_interaction_implementation_status" == "COMPLETE" ]] || fail "frontend interaction implementation must remain complete"
[[ "$multi_user_authorization_status" == "EFFECTIVE_MERGED_MAIN" ]] || fail "multi-user authorization must remain effective on merged main"
[[ "$multi_user_implementation_status" == "NOT_STARTED" ]] || fail "multi-user implementation must remain not started"
[[ -n "$current_package_phase" && -n "$current_package_mode" && -n "$current_package_branch" ]] || fail "current package declaration must be complete"
[[ "$current_package_phase" == "TRINE_LOGIC_V4_1_MACHINE_GATE_OWNER_AMENDMENT" && "$current_package_status" == "COMPLETED" ]] || fail "machine-gate owner amendment declaration mismatch"
[[ "$current_package_branch" == "codex/v4-1-machine-gate-owner-amendment" ]] || fail "machine-gate owner amendment branch mismatch"
[[ "$current_package_starting_full_sha" == "ba40c8caf4bb8d752f8833f3a089a6f01a86fa2e" ]] || fail "machine-gate owner amendment starting SHA mismatch"
[[ "$current_package_starting_full_sha" =~ ^[0-9a-fA-F]{40}$ ]] || fail "machine-gate owner amendment SHA must be full length"
[[ -n "$authorized_next_package_phase" && "$authorized_next_package_phase" != "$current_package_phase" ]] || fail "authorized next package must be distinct"
[[ "$authorized_next_package_phase" == "REAL_DATA_HOME_BLOCKER_CLOSURE" ]] || fail "authorized next package phase mismatch"
[[ "$authorized_next_package_branch" == "codex/v4-1-real-data-home-blocker-closure" ]] || fail "authorized next package branch mismatch"
[[ "$authorized_next_package_starting_full_sha" == "a60eff8d83c0e1d04371bd425267f1e8d0e4f95c" ]] || fail "authorized next package starting SHA mismatch"
[[ "$authorized_next_package_starting_full_sha" =~ ^[0-9a-fA-F]{40}$ ]] || fail "authorized next package SHA must be full length"
[[ "$authorized_next_package_mode" == "IMPLEMENTATION" ]] || fail "authorized next package mode mismatch"
[[ "$authorized_next_package_mode" != "$current_package_mode" ]] || fail "current and authorized next package modes must be distinct"
[[ "$authorized_next_package_edits" == "true" ]] || fail "authorized v4.1 repository edits must be true"
[[ "$authorized_next_package_implementation" == "true" ]] || fail "authorized v4.1 implementation must be true"
[[ "$authorized_next_package_pr" == "false" ]] || fail "authorized B01-B04 PR creation must remain false"
[[ "$authorized_next_package_canonical_figma" == "false" ]] || fail "frontend interaction Canonical Figma Desktop permission must remain false"
[[ "$authorized_next_package_mobile" == "false" ]] || fail "authorized v4.1 Mobile permission must remain false"
[[ "$authorized_next_package_canonical_figma_key" == "NONE" ]] || fail "frontend interaction package must not resolve a Figma key"
[[ -n "$blocked_package_phase" && "$blocked_package_phase" != "$current_package_phase" && "$blocked_package_phase" != "$authorized_next_package_phase" && "$blocked_package_status" == BLOCKED_* ]] || fail "blocked successor package declaration mismatch"
[[ "$p1b_scope" == "REAL_DATA_HOME_BLOCKER_CLOSURE_ONLY" ]] || fail "scope must remain REAL_DATA_HOME_BLOCKER_CLOSURE_ONLY"
[[ -n "$current_package_allowed_scope" && -n "$current_package_allowed_paths" && -n "$current_package_blocked_scope" ]] || fail "current package gate-owner scope must be explicit"
expected_owner_paths="$(printf '%s\n' \
  docs/CODEX_NEXT_TASK.yml \
  docs/PRODUCT_SOURCE_OF_TRUTH.md \
  docs/PROJECT_CURRENT_STATE.md \
  docs/DELIVERY_PROGRESS_MATRIX.md \
  docs/ACTIVE_MAINLINE_STATUS.yml \
  scripts/v1-state.sh \
  scripts/codex-next-task.sh \
  scripts/check-workflow-contract.sh)"
[[ "$(printf '%s\n' "$current_package_allowed_paths" | sort)" == "$(printf '%s\n' "$expected_owner_paths" | sort)" ]] \
  || fail "gate-owner allowlist must contain exactly the eight authorized paths"
if printf '%s\n' "$current_package_allowed_paths" | grep -Eq '[*?]|(^|/)(src|docs|scripts)/?$'; then
  fail "gate-owner allowlist must not contain wildcards or directory-level grants"
fi
[[ "$p1a_allowed_changes" == "NONE" ]] || fail "P1A allowed changes must be NONE"
[[ -n "$audit_scope_modules" && -n "$audit_scope_paths" && -n "$audit_scope_domains" ]] || fail "machine-readable P1A audit scope must be complete"
for transition_condition in \
  OWNER_EXPLICIT_DOCS_GATE_OWNER_AMENDMENT_AUTHORIZATION \
  GATE_AMENDMENT_STARTED_FROM_CLEAN_EXACT_ORIGIN_MAIN \
  EXACT_PACKAGE_MATCH \
  EXACT_BRANCH_MATCH \
  EXACT_40_CHARACTER_STARTING_SHA_MATCH \
  MACHINE_GATE_OWNER_AMENDMENT_EFFECTIVE_MERGED_MAIN \
  PRODUCT_SOURCE_GATE_PASS \
  WORKFLOW_CONTRACT_PASS \
  CLEAN_WORKTREE \
  AUTHORIZED_FILE_SCOPE_ONLY \
  NO_ACTIVE_CONFLICTING_PR; do
  printf '%s\n' "$transition_conditions" | grep -Fxq "$transition_condition" \
    || fail "missing v4.1 authorization transition condition: $transition_condition"
done

run_handoff_scenario() {
  local scenario="$1"
  shift
  V1_WORKFLOW_SELF_TEST=1 V1_HANDOFF_SELF_TEST_SCENARIO="$scenario" bash scripts/codex-next-task.sh "$@"
}

run_operator_scenario() {
  local scenario="$1"
  shift
  V1_WORKFLOW_SELF_TEST=1 V1_HANDOFF_SELF_TEST_SCENARIO="$scenario" bash scripts/v1-operator.sh "$@"
}

run_outer_scenario() {
  local scenario="$1"
  shift
  V1_WORKFLOW_SELF_TEST=1 V1_HANDOFF_SELF_TEST_SCENARIO="$scenario" bash scripts/v1-codex-run-next.sh "$@"
}

assert_handoff_blocked() {
  local scenario="$1"
  local expected_reason="$2"
  local output status
  shift 2
  set +e
  output="$(run_handoff_scenario "$scenario" "$@" 2>&1)"
  status=$?
  set -e
  [[ "$status" -ne 0 ]] || fail "handoff scenario must be blocked: $scenario"
  printf '%s\n' "$output" | grep -Fq "RESOLUTION_STATUS: BLOCKED" \
    || fail "blocked handoff scenario omitted blocked status: $scenario"
  printf '%s\n' "$output" | grep -Fq "RESOLUTION_BLOCK_REASON: $expected_reason" \
    || fail "blocked handoff scenario reason mismatch: $scenario"
  printf '%s\n' "$output" | grep -Fq "GENERATED_TASK: BLOCKED" \
    || fail "blocked handoff scenario generated a task: $scenario"
}

authorization_handoff="$(run_handoff_scenario authorization_pending)" || fail "authorization handoff failed"
printf '%s\n' "$authorization_handoff" | grep -Fq "RESOLVED_PACKAGE: $current_package_phase" \
  || fail "authorization handoff did not resolve the current v4.1 authorization package"
printf '%s\n' "$authorization_handoff" | grep -Fq "RESOLVED_HANDOFF_STAGE: V4_1_MACHINE_GATE_OWNER_AMENDMENT_REVIEW" \
  || fail "machine-gate owner amendment review stage mismatch"
for machine_identity_field in \
  "MACHINE_AUTHORIZED_PACKAGE: $authorized_next_package_phase" \
  "MACHINE_AUTHORIZED_BRANCH: $authorized_next_package_branch" \
  "MACHINE_AUTHORIZED_STARTING_FULL_SHA: $authorized_next_package_starting_full_sha" \
  "CURRENT_PACKAGE_MATCH: YES" \
  "CURRENT_BRANCH_MATCH: YES" \
  "CURRENT_STARTING_SHA_MATCH: YES"; do
  printf '%s\n' "$authorization_handoff" | grep -Fq "$machine_identity_field" \
    || fail "machine-gate handoff omitted: $machine_identity_field"
done
printf '%s\n' "$authorization_handoff" | grep -Fq "NEXT_PACKAGE_ALLOWED: NO" \
  || fail "unmerged authorization must keep v4.1 implementation blocked"

authorization_ready_handoff="$(run_handoff_scenario authorization_ready_unmerged)" || fail "ready authorization handoff failed"
printf '%s\n' "$authorization_ready_handoff" | grep -Fq "RESOLVED_HANDOFF_STAGE: V4_1_MACHINE_GATE_OWNER_AMENDMENT_FINAL_MERGE_PATH" \
  || fail "ready authorization did not resolve final merge path"

assert_handoff_blocked authorization_pending_request_v4_1 BLOCKED_PENDING_MACHINE_GATE_OWNER_AMENDMENT_MERGED_MAIN
assert_handoff_blocked authorization_merged_unsynced BLOCKED_PENDING_MACHINE_GATE_OWNER_AMENDMENT_MERGED_MAIN

v4_1_handoff="$(run_handoff_scenario authorization_merged_validated --request-package "$authorized_next_package_phase")" \
  || fail "v4.1 merged-main handoff failed"
for v4_1_expected in \
  "CURRENT_PACKAGE: $current_package_phase" \
  "REQUESTED_PACKAGE: $authorized_next_package_phase" \
  "AUTHORIZATION_STATUS: AUTHORIZED" \
  "V4_1_FINAL_INTERACTION_DESIGN_STATUS: FROZEN" \
  "V4_1_FINAL_INTERACTION_AUTHORIZATION_STATUS: EFFECTIVE_MERGED_MAIN" \
  "V4_1_FINAL_INTERACTION_IMPLEMENTATION_STATUS: COMPLETE" \
  "V4_1_TARGET_RUNTIME_REMEDIATION_AUTHORIZATION_STATUS: EFFECTIVE_MERGED_MAIN" \
  "V4_1_TARGET_RUNTIME_REMEDIATION_IMPLEMENTATION_STATUS: COMPLETE" \
  "V4_1_TELEGRAM_AUTHORIZATION_STATUS: EFFECTIVE_MERGED_MAIN" \
  "V4_1_TELEGRAM_IMPLEMENTATION_STATUS: COMPLETE" \
  "V4_1_TELEGRAM_REMEDIATION_AUTHORIZATION_STATUS: EFFECTIVE_MERGED_MAIN" \
  "V4_1_TELEGRAM_REMEDIATION_IMPLEMENTATION_STATUS: NOT_STARTED" \
  "V4_1_CORE_PRODUCTION_LOOP_AUTHORIZATION_STATUS: EFFECTIVE_MERGED_MAIN" \
  "V4_1_CORE_PRODUCTION_LOOP_IMPLEMENTATION_STATUS: NOT_STARTED" \
  "V4_1_MACHINE_GATE_OWNER_AMENDMENT_STATUS: EFFECTIVE_MERGED_MAIN" \
  "REAL_DATA_HOME_BLOCKER_CLOSURE_AUTHORIZATION_STATUS: AUTHORIZED" \
  "REAL_DATA_HOME_BLOCKER_CLOSURE_IMPLEMENTATION_STATUS: NOT_STARTED" \
  "LOCAL_REAL_AUTHORIZATION_STATUS: EFFECTIVE_MERGED_MAIN" \
  "LOCAL_REAL_IMPLEMENTATION_STATUS: COMPLETE" \
  "FRONTEND_INTERACTION_AUTHORIZATION_STATUS: EFFECTIVE_MERGED_MAIN" \
  "FRONTEND_INTERACTION_IMPLEMENTATION_STATUS: COMPLETE" \
  "REQUEST_CLASS: AUTHORIZED_IMPLEMENTATION_PACKAGE" \
  "RESOLVED_PACKAGE: $authorized_next_package_phase" \
  "RESOLVED_MODE: IMPLEMENTATION" \
  "RESOLVED_EDIT_PERMISSION: true" \
  "RESOLVED_IMPLEMENTATION_PERMISSION: true" \
  "RESOLVED_PR_CREATION_PERMISSION: false" \
  "CANONICAL_FIGMA_DESKTOP_IMPLEMENTATION_ALLOWED: false" \
  "MOBILE_IMPLEMENTATION_ALLOWED: false" \
  "CANONICAL_FIGMA_FILE_KEY: NONE" \
  "GENERATED_PACKAGE: $authorized_next_package_phase"; do
  printf '%s\n' "$v4_1_handoff" | grep -Fq "$v4_1_expected" \
    || fail "v4.1 handoff omitted: $v4_1_expected"
done

assert_handoff_blocked v4_1_unauthorized BLOCKED_REAL_DATA_HOME_SCOPE_NOT_AUTHORIZED
assert_handoff_blocked v4_1_permission_missing BLOCKED_REAL_DATA_HOME_PERMISSIONS_INCOMPLETE
assert_handoff_blocked authorization_merged_validated BLOCKED_UNKNOWN_RESOLVED_STATE \
  --request-package REAL_DATA_HOME_BLOCKER_CLOSUR
assert_handoff_blocked authorization_merged_validated BLOCKED_UNKNOWN_RESOLVED_STATE \
  --request-package FUNDAMENTAL_AI_V4_1_AUTO_TRADING
assert_handoff_blocked authorization_merged_validated BLOCKED_UNKNOWN_RESOLVED_STATE \
  --request-package FUNDAMENTAL_AI_V4_1_MOBILE
assert_handoff_blocked authorization_merged_validated BLOCKED_UNKNOWN_RESOLVED_STATE \
  --request-package FUNDAMENTAL_AI_V4_1_FIGMA
assert_handoff_blocked conflicting_pr BLOCKED_ACTIVE_CONFLICTING_PR
assert_handoff_blocked dirty_worktree BLOCKED_WORKTREE_DIRTY
assert_handoff_blocked product_source_failure BLOCKED_PRODUCT_SOURCE_GATE
assert_handoff_blocked unknown_state BLOCKED_UNKNOWN_RESOLVED_STATE

closed_debt_handoff="$(run_handoff_scenario closed_pr_1156)" || fail "closed debt handoff failed"
printf '%s\n' "$closed_debt_handoff" | grep -Fq "RESOLVED_PACKAGE: $authorized_next_package_phase" \
  || fail "closed PR #1156 incorrectly blocked v4.1"
if printf '%s\n' "$closed_debt_handoff" | grep -Eq '^GENERATED_(TASK|PACKAGE|BRANCH): .*1156'; then
  fail "closed PR #1156 became generated task context"
fi

operator_branch_before="$(git branch --show-current)"
operator_head_before="$(git rev-parse HEAD)"
operator_status_before="$(git status --porcelain=v1)"
operator_output="$(V1_WORKFLOW_SELF_TEST=1 V1_HANDOFF_SELF_TEST_SCENARIO=authorization_pending \
  bash scripts/v1-operator.sh)" \
  || fail "gate-owner amendment operator invocation failed"
operator_branch_after="$(git branch --show-current)"
operator_head_after="$(git rev-parse HEAD)"
operator_status_after="$(git status --porcelain=v1)"
for operator_expected in \
  "OPERATOR_MODE: CURRENT_PACKAGE_CONTINUATION" \
  "CURRENT_PACKAGE_ACTION: ALLOWED" \
  "CURRENT_PACKAGE_BRANCH: ACCEPTED"; do
  printf '%s\n' "$operator_output" | grep -Fq "$operator_expected" \
    || fail "gate-owner amendment operator omitted: $operator_expected"
done
[[ "$operator_branch_before" == "$operator_branch_after" ]] || fail "v4.1 operator self-test changed branch"
[[ "$operator_head_before" == "$operator_head_after" ]] || fail "v4.1 operator self-test changed HEAD"
[[ "$operator_status_before" == "$operator_status_after" ]] || fail "v4.1 operator self-test changed worktree or index"

assert_chain_allowed() {
  local name="$1" scenario="$2" expected_class="$3" expected_operator_mode="$4" expected_evidence="$5"
  local handoff_output operator_output
  shift 5
  handoff_output="$(run_handoff_scenario "$scenario" "$@")" || { fail "$name handoff was blocked"; return; }
  printf '%s\n' "$handoff_output" | grep -Fq "RESOLUTION_STATUS: ALLOWED" \
    || fail "$name handoff omitted ALLOWED"
  printf '%s\n' "$handoff_output" | grep -Fq "REQUEST_CLASS: $expected_class" \
    || fail "$name request classification mismatch"
  printf '%s\n' "$handoff_output" | grep -Fq "OPEN_PR_EVIDENCE_SOURCE: $expected_evidence" \
    || fail "$name evidence source mismatch"
  if [[ "$expected_operator_mode" == "LOCAL_IMPLEMENTATION_NO_PR" ]]; then
    printf '%s\n' "$handoff_output" | grep -Fq "RESOLVED_MODE: IMPLEMENTATION" \
      || fail "$name local implementation mode mismatch"
    printf '%s\n' "$handoff_output" | grep -Fq "RESOLVED_PR_CREATION_PERMISSION: false" \
      || fail "$name local implementation unexpectedly received PR permission"
  else
    operator_output="$(run_operator_scenario "$scenario" "$@")" || { fail "$name operator was blocked"; return; }
    printf '%s\n' "$operator_output" | grep -Fq "OPERATOR_MODE: $expected_operator_mode" \
      || fail "$name operator mode mismatch"
  fi
  echo "WORKFLOW_CHAIN_$name: PASS"
}

assert_chain_blocked() {
  local name="$1" scenario="$2" expected_reason="$3"
  local handoff_output handoff_status operator_output operator_status
  shift 3
  set +e
  handoff_output="$(run_handoff_scenario "$scenario" "$@" 2>&1)"
  handoff_status=$?
  operator_output="$(run_operator_scenario "$scenario" "$@" 2>&1)"
  operator_status=$?
  set -e
  [[ "$handoff_status" -ne 0 ]] || fail "$name handoff unexpectedly passed"
  [[ "$operator_status" -ne 0 ]] || fail "$name operator unexpectedly passed"
  printf '%s\n' "$handoff_output" | grep -Fq "RESOLUTION_BLOCK_REASON: $expected_reason" \
    || fail "$name handoff block reason mismatch"
  printf '%s\n' "$operator_output" | grep -Fq "RESOLUTION_BLOCK_REASON: $expected_reason" \
    || fail "$name operator block reason mismatch"
  echo "WORKFLOW_CHAIN_$name: PASS"
}

# Full handoff chain: CODEX_NEXT_TASK.yml -> v1-state -> codex-next-task -> v1-operator.
assert_chain_allowed CURRENT_AUTHORIZATION_REMEDIATION current_authorization_remediation \
  CURRENT_PACKAGE_CONTINUATION CURRENT_PACKAGE_CONTINUATION GH_QUERY
assert_chain_allowed CURRENT_AUTHORIZATION_FINAL_GATE current_authorization_final_gate \
  CURRENT_PACKAGE_CONTINUATION CURRENT_PACKAGE_CONTINUATION GH_QUERY
final_gate_handoff="$(run_handoff_scenario current_authorization_final_gate)" || fail "authorization final gate handoff failed"
printf '%s\n' "$final_gate_handoff" | grep -Fq "RESOLVED_HANDOFF_STAGE: V4_1_MACHINE_GATE_OWNER_AMENDMENT_FINAL_MERGE_PATH" \
  || fail "machine-gate owner amendment final gate stage mismatch"
assert_chain_allowed MERGED_VALIDATED_WITH_GH merged_gh_no_pr \
  AUTHORIZED_IMPLEMENTATION_PACKAGE LOCAL_IMPLEMENTATION_NO_PR GH_QUERY --request-package "$authorized_next_package_phase"
assert_chain_blocked MERGED_WITHOUT_GH_OR_EVIDENCE merged_gh_unavailable_no_evidence \
  BLOCKED_UNKNOWN_CURRENT_PACKAGE_PR_STATE --request-package "$authorized_next_package_phase"
assert_chain_allowed MERGED_WITH_EXPLICIT_EVIDENCE merged_gh_unavailable \
  AUTHORIZED_IMPLEMENTATION_PACKAGE LOCAL_IMPLEMENTATION_NO_PR EXPLICIT_CONFIRMED --open-pr-none-confirmed \
  --request-package "$authorized_next_package_phase"
assert_chain_blocked EXPLICIT_EVIDENCE_WITH_CONFLICT explicit_with_conflict \
  BLOCKED_ACTIVE_CONFLICTING_PR --open-pr-none-confirmed --request-package "$authorized_next_package_phase"
assert_chain_allowed CURRENT_PR_SELF_CONFLICT current_pr_self_conflict \
  CURRENT_PACKAGE_CONTINUATION CURRENT_PACKAGE_CONTINUATION GH_QUERY
assert_chain_blocked SEPARATE_CONFLICTING_PR separate_conflicting_pr_successor \
  BLOCKED_ACTIVE_CONFLICTING_PR --request-package "$authorized_next_package_phase"
assert_chain_allowed SEPARATE_CONFLICT_CURRENT_AUTHORIZATION separate_conflicting_pr_current \
  CURRENT_PACKAGE_CONTINUATION CURRENT_PACKAGE_CONTINUATION GH_QUERY
assert_chain_blocked V4_1_PERMISSION_MISSING v4_1_permission_missing BLOCKED_REAL_DATA_HOME_PERMISSIONS_INCOMPLETE \
  --request-package "$authorized_next_package_phase"
assert_chain_blocked V4_1_BEFORE_AUTHORIZATION authorization_pending_request_v4_1 BLOCKED_PENDING_MACHINE_GATE_OWNER_AMENDMENT_MERGED_MAIN \
  --request-package "$authorized_next_package_phase"
assert_chain_blocked V4_1_UNAUTHORIZED v4_1_unauthorized BLOCKED_REAL_DATA_HOME_SCOPE_NOT_AUTHORIZED \
  --request-package "$authorized_next_package_phase"
assert_chain_blocked DIRTY_WORKTREE dirty_worktree BLOCKED_WORKTREE_DIRTY \
  --request-package "$authorized_next_package_phase"
assert_chain_blocked PRODUCT_SOURCE_FAILURE product_source_failure BLOCKED_PRODUCT_SOURCE_GATE \
  --request-package "$authorized_next_package_phase"
assert_chain_blocked UNKNOWN_RESOLVER unknown_state BLOCKED_UNKNOWN_RESOLVED_STATE

assert_outer_allowed() {
  local name="$1" scenario="$2" expected_mode="$3"
  local output
  shift 3
  output="$(run_outer_scenario "$scenario" "$@")" || { fail "$name outer launcher was blocked"; return; }
  grep -Fq "OPERATOR_MODE: $expected_mode" <<<"$output" \
    || fail "$name outer launcher mode mismatch"
  grep -Fq "OPERATOR_RESULT_STATUS: PASS" <<<"$output" \
    || fail "$name outer launcher omitted operator PASS"
  grep -Fq "OUTER_LAUNCHER_STATUS: PASS" <<<"$output" \
    || fail "$name outer launcher omitted launcher PASS"
  echo "OUTER_LAUNCHER_$name: PASS"
}

assert_outer_blocked() {
  local name="$1" scenario="$2" expected_reason="$3"
  local output status
  shift 3
  set +e
  output="$(run_outer_scenario "$scenario" "$@" 2>&1)"
  status=$?
  set -e
  [[ "$status" -ne 0 ]] || fail "$name outer launcher unexpectedly passed"
  printf '%s\n' "$output" | grep -Fq "$expected_reason" \
    || fail "$name outer launcher reason mismatch"
  echo "OUTER_LAUNCHER_$name: PASS"
}

assert_outer_allowed CURRENT_AUTHORIZATION_LAUNCH current_authorization_remediation CURRENT_PACKAGE_CONTINUATION
assert_outer_allowed CURRENT_AUTHORIZATION_FINAL_GATE current_authorization_final_gate CURRENT_PACKAGE_CONTINUATION
assert_outer_blocked V4_1_PERMISSION_MISSING v4_1_permission_missing BLOCKED_REAL_DATA_HOME_PERMISSIONS_INCOMPLETE \
  --request-package "$authorized_next_package_phase"
assert_outer_blocked V4_1_PENDING_MERGE authorization_pending_request_v4_1 BLOCKED_PENDING_MACHINE_GATE_OWNER_AMENDMENT_MERGED_MAIN \
  --request-package "$authorized_next_package_phase"
assert_outer_blocked V4_1_UNAUTHORIZED v4_1_unauthorized BLOCKED_REAL_DATA_HOME_SCOPE_NOT_AUTHORIZED \
  --request-package "$authorized_next_package_phase"
assert_outer_blocked UNKNOWN_STATE unknown_state BLOCKED_UNKNOWN_RESOLVED_STATE
assert_outer_blocked ACTIVE_CONFLICTING_PR conflicting_pr BLOCKED_ACTIVE_CONFLICTING_PR \
  --request-package "$authorized_next_package_phase"
assert_outer_blocked PRODUCT_SOURCE_FAILURE product_source_failure BLOCKED_PRODUCT_SOURCE_GATE \
  --request-package "$authorized_next_package_phase"

set +e
invalid_evidence_output="$(V1_OPEN_PR_NONE_CONFIRMED=INVALID V1_WORKFLOW_SELF_TEST=1 \
  V1_HANDOFF_SELF_TEST_SCENARIO=merged_gh_no_pr bash scripts/codex-next-task.sh 2>&1)"
invalid_evidence_status=$?
set -e
[[ "$invalid_evidence_status" -ne 0 ]] || fail "invalid Open PR evidence unexpectedly passed"
printf '%s\n' "$invalid_evidence_output" | grep -Fq "RESOLUTION_BLOCK_REASON: BLOCKED_INVALID_OPEN_PR_EVIDENCE" \
  || fail "invalid Open PR evidence did not fail closed"

run_next_output="$(V1_WORKFLOW_SELF_TEST=1 \
  V1_HANDOFF_SELF_TEST_SCENARIO=merged_gh_unavailable bash scripts/codex-next-task.sh \
  --open-pr-none-confirmed --request-package "$authorized_next_package_phase")" \
  || fail "codex-next-task did not propagate explicit Open PR evidence"
printf '%s\n' "$run_next_output" | grep -Fq "OPEN_PR_EVIDENCE_SOURCE: EXPLICIT_CONFIRMED" \
  || fail "codex-next-task omitted explicit evidence at the authoritative resolver"
printf '%s\n' "$run_next_output" | grep -Fq "PR_CREATION_PERMISSION: false" \
  || fail "bounded target handoff unexpectedly enabled PR creation"

exact_gate_text="$(bash scripts/v1-state.sh --self-test-exact-machine-gate)" \
  || fail "exact machine-gate self-test failed"
for exact_gate_case in \
  EXACT_GATE_01_CORRECT_TRIPLE \
  EXACT_GATE_02_WRONG_SHA \
  EXACT_GATE_03_SHORT_SHA \
  EXACT_GATE_04_MISSING_SHA \
  EXACT_GATE_05_WRONG_BRANCH \
  EXACT_GATE_06_WRONG_PACKAGE \
  EXACT_GATE_07_OUT_OF_SCOPE_FILE \
  EXACT_GATE_08_JAVA_SQL_CSS_MIX \
  EXACT_GATE_09_ORDINARY_PACKAGE_GATE_OWNER_MUTATION \
  EXACT_GATE_10_OWNER_AMENDMENT_NON_MAIN_START \
  EXACT_GATE_11_OWNER_PUSH_MERGE_PERMISSION \
  EXACT_GATE_12_BLOCKED_PACKAGE_REGRESSION; do
  printf '%s\n' "$exact_gate_text" | grep -Fq "$exact_gate_case: PASS" \
    || fail "missing exact machine-gate case: $exact_gate_case"
done
printf '%s\n' "$exact_gate_text" | grep -Fq "EXACT_MACHINE_GATE_TESTS: PASS" \
  || fail "exact machine-gate suite did not report PASS"

audit_policy_text="$(bash scripts/v1-state.sh --self-test-product-audit-policy)" || fail "product audit policy self-test failed"
printf '%s\n' "$audit_policy_text" | grep -Fq "PRODUCT_AUDIT_POLICY_TESTS: PASS" \
  || fail "product audit policy self-test did not report PASS"
for expected_case in \
  TRANSITION_TEST_P0_OPEN \
  TRANSITION_TEST_P0_READY_UNMERGED \
  TRANSITION_TEST_P0_MERGED_UNSYNCED \
  TRANSITION_TEST_P0_MERGED_VALIDATED \
  TRANSITION_TEST_P1B_REMAINS_BLOCKED \
  TRANSITION_TEST_P1A_INCOMPLETE \
  TRANSITION_TEST_P1B_AUTHORIZATION_UNMERGED \
  TRANSITION_TEST_P1B_AUTHORIZATION_EFFECTIVE \
  TRANSITION_TEST_P1B_UNAUTHORIZED \
  TRANSITION_TEST_P1B_SOURCE_GATE_FAILURE \
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

bash scripts/validate-frontend-interaction-runtime-closure-authorization.sh >/dev/null \
  || fail "frontend interaction authorization validation failed"

bash scripts/validate-multi-user-account-registration-authorization.sh >/dev/null \
  || fail "multi-user authorization validation failed"

for core_authorization_artifact in \
  docs/TRINE_LOGIC_CORE_PRODUCTION_LOOP_AUTOMATION_SOURCE_MAPPING.md \
  docs/TRINE_LOGIC_CORE_PRODUCTION_LOOP_AUTOMATION_OWNERSHIP_MAP.md \
  docs/TRINE_LOGIC_CORE_PRODUCTION_LOOP_AUTOMATION_AUTHORIZATION.md \
  docs/TRINE_LOGIC_CORE_PRODUCTION_LOOP_AUTOMATION_AUTHORIZATION_VALIDATION.md; do
  git diff --quiet origin/main -- "$core_authorization_artifact" \
    || fail "merged core production-loop authorization artifact changed: $core_authorization_artifact"
done

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
