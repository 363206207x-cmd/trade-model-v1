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
  if [[ ! -f "$file" ]] || ! grep -Fq "$text" "$file"; then
    fail "missing required text in $file: $text"
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

require_contains "AGENTS.md" "docs/PROJECT_DELIVERY_CONTRACT.md"
require_contains "AGENTS.md" "docs/PROJECT_CURRENT_STATE.md"
require_contains "AGENTS.md" "docs/DELIVERY_PROGRESS_MATRIX.md"
require_contains "AGENTS.md" "docs/CODEX_TASK_TEMPLATE.md"
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
