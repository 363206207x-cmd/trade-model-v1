#!/usr/bin/env bash

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

CONTRACT_FILE="docs/PROJECT_DELIVERY_CONTRACT.md"
CURRENT_STATE_FILE="docs/PROJECT_CURRENT_STATE.md"
MATRIX_FILE="docs/DELIVERY_PROGRESS_MATRIX.md"
TEMPLATE_FILE="docs/CODEX_TASK_TEMPLATE.md"
TASK_FILE="docs/CODEX_NEXT_TASK.yml"
STATE_ARGS=()

usage() {
  cat <<'EOF'
usage: bash scripts/codex-next-task.sh [--validate] [--open-pr-none-confirmed] [--request-package PACKAGE]

Reads the Project Delivery Contract, Delivery Progress Matrix, Project Current State,
Codex Task Template, and the derived CODEX_NEXT_TASK.yml handoff.
It does not modify files, stage, commit, push, call gh, or run business commands.
EOF
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

state_value() {
  local state_text="$1"
  local key="$2"
  printf '%s\n' "$state_text" | awk -F': ' -v key="$key" '$1 == key {print substr($0, length(key) + 3); exit}'
}

matrix_field() {
  local phase="$1"
  local field_index="$2"
  [[ -f "$MATRIX_FILE" ]] || return 0
  awk -F'|' -v phase="$phase" -v field_index="$field_index" '
    $2 ~ "^[[:space:]]*" phase "[[:space:]]*$" {
      value=$field_index
      gsub(/^[[:space:]]+|[[:space:]]+$/, "", value)
      print value
      exit
    }
  ' "$MATRIX_FILE"
}

current_state_value() {
  local key="$1"
  [[ -f "$CURRENT_STATE_FILE" ]] || return 0
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
  ' "$CURRENT_STATE_FILE"
}

validate_contract_task() {
  local failed=0
  for f in "$CONTRACT_FILE" "$CURRENT_STATE_FILE" "$MATRIX_FILE" "$TEMPLATE_FILE" "$TASK_FILE"; do
    if [[ ! -f "$f" ]]; then
      echo "TASK_VALIDATION_FAILED missing file: $f" >&2
      failed=1
    fi
  done
  [[ "$failed" -eq 0 ]] || return 1

  local matrix_phase="P0-0"
  local matrix_status task_phase task_allowed current_phase current_status effective compat state_text
  local current_package_phase current_package_mode current_package_status authorized_next_phase authorized_next_mode
  local authorized_next_edits authorized_next_implementation authorized_next_pr blocked_package blocked_status
  local p1b_1_status p1b_authorization_status home_core_data_status home_core_data_implementation_status p1b_scope
  local product_p1b_status product_p2_status p2_authorization_status p2_implementation_status
  local product_v4_1_authorization v4_1_design_status v4_1_authorization_status v4_1_implementation_status
  matrix_status="$(matrix_field P0-0 4)"
  task_phase="$(yaml_value "$TASK_FILE" current_phase)"
  task_allowed="$(yaml_value "$TASK_FILE" next_business_phase_allowed)"
  current_phase="$(current_state_value "Current Phase")"
  current_status="$(current_state_value "Current Phase Status")"
  state_text="$(bash scripts/v1-state.sh ${STATE_ARGS[@]+"${STATE_ARGS[@]}"})"
  effective="$(printf '%s\n' "$state_text" | awk -F': ' '$1 == "COMPLETION_EFFECTIVE_STATE" {print $2; exit}')"
  compat="$(yaml_value "$TASK_FILE" compatibility_status)"
  current_package_phase="$(yaml_value "$TASK_FILE" current_package_phase)"
  current_package_mode="$(yaml_value "$TASK_FILE" current_package_mode)"
  current_package_status="$(yaml_value "$TASK_FILE" current_package_status)"
  authorized_next_phase="$(yaml_value "$TASK_FILE" authorized_next_package_phase)"
  authorized_next_mode="$(yaml_value "$TASK_FILE" authorized_next_package_mode)"
  authorized_next_edits="$(yaml_value "$TASK_FILE" authorized_next_package_repository_edits_allowed)"
  authorized_next_implementation="$(yaml_value "$TASK_FILE" authorized_next_package_implementation_allowed)"
  authorized_next_pr="$(yaml_value "$TASK_FILE" authorized_next_package_implementation_pr_allowed)"
  authorized_next_canonical_figma="$(yaml_value "$TASK_FILE" authorized_next_package_canonical_figma_desktop_implementation_allowed)"
  authorized_next_mobile="$(yaml_value "$TASK_FILE" authorized_next_package_mobile_implementation_allowed)"
  authorized_next_canonical_figma_key="$(yaml_value "$TASK_FILE" authorized_next_package_canonical_figma_file_key)"
  blocked_package="$(yaml_value "$TASK_FILE" blocked_package_phase)"
  blocked_status="$(yaml_value "$TASK_FILE" blocked_package_status)"
  p1b_1_status="$(yaml_value "$TASK_FILE" p1b_1_status)"
  p1b_authorization_status="$(yaml_value "$TASK_FILE" p1b_authorization_status)"
  home_core_data_status="$(yaml_value "$TASK_FILE" p1b_home_core_data_authorization_status)"
  home_core_data_implementation_status="$(yaml_value "$TASK_FILE" p1b_home_core_data_implementation_status)"
  product_p1b_status="$(yaml_value "$TASK_FILE" product_p1b_status)"
  product_p2_status="$(matrix_field "Product P2" 5)"
  p2_authorization_status="$(yaml_value "$TASK_FILE" p2_position_monitoring_authorization_status)"
  p2_implementation_status="$(yaml_value "$TASK_FILE" p2_position_monitoring_implementation_status)"
  product_v4_1_authorization="$(matrix_field "Product v4.1" 5)"
  v4_1_design_status="$(yaml_value "$TASK_FILE" v4_1_final_interaction_design_status)"
  v4_1_authorization_status="$(yaml_value "$TASK_FILE" v4_1_final_interaction_authorization_status)"
  v4_1_implementation_status="$(yaml_value "$TASK_FILE" v4_1_final_interaction_implementation_status)"
  p1b_scope="$(yaml_value "$TASK_FILE" scope)"

  [[ "$compat" == "DERIVED_ONLY" ]] || { echo "TASK_VALIDATION_FAILED CODEX_NEXT_TASK must be DERIVED_ONLY" >&2; failed=1; }
  [[ "$task_phase" == "$matrix_phase" ]] || { echo "TASK_VALIDATION_FAILED task current_phase mismatch: $task_phase" >&2; failed=1; }
  [[ "$current_phase" == P0-0* ]] || { echo "TASK_VALIDATION_FAILED current state phase mismatch: $current_phase" >&2; failed=1; }
  [[ "$current_status" == "$matrix_status" ]] || { echo "TASK_VALIDATION_FAILED current state status mismatch: $current_status != $matrix_status" >&2; failed=1; }
  [[ -n "$current_package_phase" && -n "$current_package_mode" ]] || { echo "TASK_VALIDATION_FAILED current package declaration is incomplete" >&2; failed=1; }
  [[ "$current_package_phase" == "FUNDAMENTAL_AI_V4_1_FINAL_INTERACTION_AUTHORIZATION" && "$current_package_status" == "COMPLETED" ]] || { echo "TASK_VALIDATION_FAILED v4.1 Final Interaction authorization declaration mismatch" >&2; failed=1; }
  [[ "$current_package_mode" == "BOUNDED_PRODUCT_DECISION_AND_AUTHORIZATION" ]] || { echo "TASK_VALIDATION_FAILED current authorization mode mismatch" >&2; failed=1; }
  [[ -n "$authorized_next_phase" && "$authorized_next_phase" != "$current_package_phase" ]] || { echo "TASK_VALIDATION_FAILED authorized next package must be distinct" >&2; failed=1; }
  [[ "$authorized_next_phase" == "FUNDAMENTAL_AI_V4_1_FINAL_INTERACTION_PAGE_AND_RUNTIME_IMPLEMENTATION" && "$authorized_next_mode" == "IMPLEMENTATION" ]] || { echo "TASK_VALIDATION_FAILED authorized v4.1 final interaction package mismatch" >&2; failed=1; }
  [[ "$authorized_next_mode" != "$current_package_mode" ]] || { echo "TASK_VALIDATION_FAILED current and authorized next modes must be distinct" >&2; failed=1; }
  [[ "$authorized_next_edits" == "true" && "$authorized_next_implementation" == "true" && "$authorized_next_pr" == "true" && "$authorized_next_canonical_figma" == "true" && "$authorized_next_mobile" == "false" && "$authorized_next_canonical_figma_key" == "rdMYmsAvZYkXHJX8hdl7UN" ]] || { echo "TASK_VALIDATION_FAILED bounded v4.1 permissions are incomplete" >&2; failed=1; }
  [[ -n "$blocked_package" && "$blocked_package" != "$current_package_phase" && "$blocked_package" != "$authorized_next_phase" && "$blocked_status" == BLOCKED_* ]] || { echo "TASK_VALIDATION_FAILED blocked successor declaration mismatch" >&2; failed=1; }
  [[ "$p1b_1_status" == "EFFECTIVE_MERGED_MAIN" ]] || { echo "TASK_VALIDATION_FAILED P1B-1 predecessor is not effective" >&2; failed=1; }
  [[ "$p1b_authorization_status" == "EFFECTIVE_MERGED_MAIN" && "$home_core_data_status" == "EFFECTIVE_MERGED_MAIN" && "$home_core_data_implementation_status" == "COMPLETE" ]] || { echo "TASK_VALIDATION_FAILED Product P1B predecessor boundary mismatch" >&2; failed=1; }
  [[ "$product_p1b_status" == "COMPLETE" && "$product_p2_status" == "AUTHORIZED_TO_IMPLEMENT" ]] || { echo "TASK_VALIDATION_FAILED historical Product P1B/P2 evidence mismatch" >&2; failed=1; }
  [[ "$p2_authorization_status" == "EFFECTIVE_MERGED_MAIN" && "$p2_implementation_status" == "COMPLETE" ]] || { echo "TASK_VALIDATION_FAILED merged P2 compatibility evidence mismatch" >&2; failed=1; }
  [[ "$product_v4_1_authorization" == "AUTHORIZED_TO_IMPLEMENT" && "$v4_1_design_status" == "FROZEN" ]] || { echo "TASK_VALIDATION_FAILED v4.1 Product Source freeze or matrix authorization mismatch" >&2; failed=1; }
  [[ "$v4_1_authorization_status" == "AUTHORIZED_PENDING_MERGED_MAIN" && "$v4_1_implementation_status" == "NOT_STARTED" && "$p1b_scope" == "V4_1_FINAL_INTERACTION_PAGE_AND_RUNTIME_ONLY" ]] || { echo "TASK_VALIDATION_FAILED v4.1 Final Interaction authorization boundary mismatch" >&2; failed=1; }
  [[ -f docs/FUNDAMENTAL_AI_V4_1_FINAL_INTERACTION_PAGE_AND_RUNTIME_AUTHORIZATION.md && -f docs/FUNDAMENTAL_AI_V4_1_FINAL_INTERACTION_OBJECT_OWNERSHIP_MAP.md && -f docs/FUNDAMENTAL_AI_V4_1_PAGE_ROUTE_COMPONENT_MATRIX.md && -f docs/FUNDAMENTAL_AI_V4_1_PR1179_REUSE_AND_SUPERSESSION_MAP.md && -f docs/FUNDAMENTAL_AI_V4_1_VISUAL_DENSITY_AND_PROPORTION_CONTRACT.md && -f docs/FUNDAMENTAL_AI_V4_1_CANONICAL_FIGMA_AUTHORIZATION_SCOPE_RECONCILIATION.md && -f docs/product-sources/FUNDAMENTAL_AI_V4_1_DECISION_CHAIN.md ]] || { echo "TASK_VALIDATION_FAILED v4.1 final interaction artifacts are missing" >&2; failed=1; }
  if [[ "$matrix_status" != "DONE" || "$effective" != "EFFECTIVE_MERGED_MAIN" ]]; then
    [[ "$task_allowed" == "false" || "$task_allowed" == "NO" ]] || { echo "TASK_VALIDATION_FAILED next business phase must be blocked while current phase is not effective" >&2; failed=1; }
    local task_active_block task_module task_next_action
    task_active_block="$(yaml_value "$TASK_FILE" active_block)"
    task_module="$(yaml_value "$TASK_FILE" module)"
    task_next_action="$(yaml_value "$TASK_FILE" next_allowed_action)"
    if printf '%s\n%s\n%s\n' "$task_active_block" "$task_module" "$task_next_action" | grep -Eiq 'P0-1[[:space:]]+UserPosition[[:space:]]+implementation|UserPosition implementation|active_block:[[:space:]]*P0-1|module:[[:space:]]*UserPosition'; then
      echo "TASK_VALIDATION_FAILED task must not generate P0-1 implementation while P0-0 is not effective" >&2
      failed=1
    fi
  fi
  [[ "$failed" -eq 0 ]] || return 1
  echo "CODEX_TASK_VALIDATION_OK"
}

validate_only="NO"
while [[ "$#" -gt 0 ]]; do
  case "$1" in
    --validate)
      validate_only="YES"
      shift
      ;;
    --open-pr-none-confirmed)
      STATE_ARGS+=("--open-pr-none-confirmed")
      shift
      ;;
    --request-package)
      [[ -n "${2:-}" ]] || { echo "--request-package requires a package identifier" >&2; exit 2; }
      STATE_ARGS+=("--request-package" "$2")
      shift 2
      ;;
    --help|-h)
      usage
      exit 0
      ;;
    *)
      usage >&2
      exit 2
      ;;
  esac
done

if [[ "$validate_only" == "YES" ]]; then
  validate_contract_task
  exit 0
fi

validate_contract_task >/dev/null

if ! state_text="$(bash scripts/v1-state.sh ${STATE_ARGS[@]+"${STATE_ARGS[@]}"} 2>&1)"; then
  echo "STOP: authoritative state resolver failed." >&2
  printf '%s\n' "$state_text" >&2
  exit 1
fi

branch="$(state_value "$state_text" BRANCH)"
worktree="$(state_value "$state_text" WORKTREE_CLEAN)"
main_sync="$(state_value "$state_text" MAIN_SYNC)"
open_prs="$(state_value "$state_text" OPEN_PRS)"
completion_effective_state="$(state_value "$state_text" COMPLETION_EFFECTIVE_STATE)"
next_package_allowed="$(state_value "$state_text" NEXT_PACKAGE_ALLOWED)"
next_package_block_reason="$(state_value "$state_text" NEXT_PACKAGE_BLOCK_REASON)"
current_package_action_allowed="$(state_value "$state_text" CURRENT_PACKAGE_ACTION_ALLOWED)"
current_package_block_reason="$(state_value "$state_text" CURRENT_PACKAGE_BLOCK_REASON)"
open_pr_evidence_source="$(state_value "$state_text" OPEN_PR_EVIDENCE_SOURCE)"
open_pr_none_confirmed="$(state_value "$state_text" OPEN_PR_NONE_CONFIRMED)"
active_conflicting_prs="$(state_value "$state_text" ACTIVE_CONFLICTING_PRS)"
request_class="$(state_value "$state_text" REQUEST_CLASS)"
current_package="$(state_value "$state_text" CURRENT_PACKAGE)"
requested_package_output="$(state_value "$state_text" REQUESTED_PACKAGE)"
authorization_status="$(state_value "$state_text" AUTHORIZATION_STATUS)"
p1a_completion_status="$(state_value "$state_text" P1A_COMPLETION_STATUS)"
p1b_authorization_runtime_status="$(state_value "$state_text" P1B_AUTHORIZATION_RUNTIME_STATUS)"
p1b_1_runtime_status="$(state_value "$state_text" P1B_1_STATUS)"
home_core_data_authorization_runtime_status="$(state_value "$state_text" P1B_HOME_CORE_DATA_AUTHORIZATION_STATUS)"
home_core_data_completion_status="$(state_value "$state_text" P1B_HOME_CORE_DATA_COMPLETION_STATUS)"
p2_authorization_runtime_status="$(state_value "$state_text" P2_POSITION_MONITORING_AUTHORIZATION_STATUS)"
p2_implementation_runtime_status="$(state_value "$state_text" P2_POSITION_MONITORING_IMPLEMENTATION_STATUS)"
v4_1_design_runtime_status="$(state_value "$state_text" V4_1_DECISION_CHAIN_DESIGN_STATUS)"
v4_1_authorization_runtime_status="$(state_value "$state_text" V4_1_DECISION_CHAIN_AUTHORIZATION_STATUS)"
v4_1_implementation_runtime_status="$(state_value "$state_text" V4_1_DECISION_CHAIN_IMPLEMENTATION_STATUS)"
resolved_from_state="$(state_value "$state_text" RESOLVED_FROM_STATE)"
resolution_status="$(state_value "$state_text" RESOLUTION_STATUS)"
resolution_block_reason="$(state_value "$state_text" RESOLUTION_BLOCK_REASON)"
resolved_package="$(state_value "$state_text" RESOLVED_PACKAGE)"
resolved_mode="$(state_value "$state_text" RESOLVED_MODE)"
resolved_branch="$(state_value "$state_text" RESOLVED_BRANCH)"
resolved_active_block="$(state_value "$state_text" RESOLVED_ACTIVE_BLOCK)"
resolved_risk="$(state_value "$state_text" RESOLVED_RISK)"
resolved_scope_profile="$(state_value "$state_text" RESOLVED_SCOPE_PROFILE)"
resolved_handoff_stage="$(state_value "$state_text" RESOLVED_HANDOFF_STAGE)"
resolved_edit_permission="$(state_value "$state_text" RESOLVED_EDIT_PERMISSION)"
resolved_implementation_permission="$(state_value "$state_text" RESOLVED_IMPLEMENTATION_PERMISSION)"
resolved_pr_creation_permission="$(state_value "$state_text" RESOLVED_PR_CREATION_PERMISSION)"
canonical_figma_desktop_implementation_allowed="$(state_value "$state_text" CANONICAL_FIGMA_DESKTOP_IMPLEMENTATION_ALLOWED)"
mobile_implementation_allowed="$(state_value "$state_text" MOBILE_IMPLEMENTATION_ALLOWED)"
canonical_figma_file_key="$(state_value "$state_text" CANONICAL_FIGMA_FILE_KEY)"
resolved_next_action="$(state_value "$state_text" RESOLVED_NEXT_ACTION)"

if [[ "$resolved_from_state" != "YES" || "$resolution_status" != "ALLOWED" ]]; then
  cat <<EOF
RESOLVED_FROM_STATE: ${resolved_from_state:-NO}
RESOLUTION_STATUS: ${resolution_status:-BLOCKED}
RESOLVED_PACKAGE: ${resolved_package:-BLOCKED}
RESOLVED_MODE: ${resolved_mode:-BLOCKED}
RESOLVED_EDIT_PERMISSION: ${resolved_edit_permission:-false}
NEXT_PACKAGE_ALLOWED: ${next_package_allowed:-NO}
NEXT_PACKAGE_BLOCK_REASON: ${next_package_block_reason:-BLOCKED_UNKNOWN_STATE}
RESOLUTION_BLOCK_REASON: ${resolution_block_reason:-BLOCKED_UNKNOWN_STATE}
GENERATED_TASK: BLOCKED
EOF
  exit 1
fi

case "$resolved_scope_profile" in
  CURRENT_PACKAGE)
    [[ "$request_class" == "CURRENT_PACKAGE_CONTINUATION" && "$current_package_action_allowed" == "YES" ]] \
      || { echo "STOP: current package was not authorized by the authoritative resolver." >&2; exit 1; }
    generated_allowed_scope="$(yaml_list "$TASK_FILE" current_package_allowed_scope)"
    generated_blocked_scope="$(yaml_list "$TASK_FILE" current_package_blocked_scope)"
    ;;
  AUTHORIZED_NEXT_PACKAGE)
    [[ "$request_class" == "SUCCESSOR_PACKAGE" && "$next_package_allowed" == "YES" ]] \
      || { echo "STOP: successor package was not authorized by the authoritative resolver." >&2; exit 1; }
    generated_allowed_scope="$(yaml_list "$TASK_FILE" authorized_next_allowed_scope)"
    generated_blocked_scope="$(yaml_list "$TASK_FILE" authorized_next_blocked_scope)"
    case "$resolved_mode" in
      READ_ONLY_PRODUCT_AUDIT)
        [[ "$resolved_edit_permission" == "false" ]] || { echo "STOP: read-only audit resolved with repository edits enabled." >&2; exit 1; }
        [[ "$resolved_implementation_permission" == "false" ]] || { echo "STOP: read-only audit resolved with implementation enabled." >&2; exit 1; }
        [[ "$resolved_pr_creation_permission" == "false" ]] || { echo "STOP: read-only audit resolved with PR creation enabled." >&2; exit 1; }
        ;;
      IMPLEMENTATION)
        [[ "$resolved_edit_permission" == "true" ]] || { echo "STOP: implementation resolved without repository edit permission." >&2; exit 1; }
        [[ "$resolved_implementation_permission" == "true" ]] || { echo "STOP: implementation resolved without implementation permission." >&2; exit 1; }
        [[ "$resolved_pr_creation_permission" == "true" ]] || { echo "STOP: implementation resolved without PR creation permission." >&2; exit 1; }
        if [[ "$resolved_package" == "FUNDAMENTAL_AI_V4_1_FINAL_INTERACTION_PAGE_AND_RUNTIME_IMPLEMENTATION" ]]; then
          [[ "$canonical_figma_desktop_implementation_allowed" == "true" ]] || { echo "STOP: exact v4.1 package resolved without Canonical Figma Desktop permission." >&2; exit 1; }
          [[ "$mobile_implementation_allowed" == "false" ]] || { echo "STOP: exact v4.1 package resolved with forbidden Mobile permission." >&2; exit 1; }
          [[ "$canonical_figma_file_key" == "rdMYmsAvZYkXHJX8hdl7UN" ]] || { echo "STOP: exact v4.1 package resolved the wrong Canonical Figma file." >&2; exit 1; }
        fi
        ;;
      *)
        echo "STOP: unsupported authorized successor mode (${resolved_mode:-UNKNOWN})." >&2
        exit 1
        ;;
    esac
    ;;
  *)
    echo "STOP: unknown resolved scope profile (${resolved_scope_profile:-UNKNOWN})." >&2
    exit 1
    ;;
esac

cat "$TEMPLATE_FILE"
cat <<EOF

---

# Authoritative Resolved Task Handoff

RESOLVED_FROM_STATE: YES
RESOLUTION_STATUS: ALLOWED
CURRENT_PACKAGE: $current_package
REQUESTED_PACKAGE: $requested_package_output
P1A_COMPLETION_STATUS: $p1a_completion_status
AUTHORIZATION_STATUS: $authorization_status
P1B_AUTHORIZATION_RUNTIME_STATUS: $p1b_authorization_runtime_status
P1B_1_STATUS: $p1b_1_runtime_status
P1B_HOME_CORE_DATA_AUTHORIZATION_STATUS: $home_core_data_authorization_runtime_status
P1B_HOME_CORE_DATA_COMPLETION_STATUS: $home_core_data_completion_status
P2_POSITION_MONITORING_AUTHORIZATION_STATUS: $p2_authorization_runtime_status
P2_POSITION_MONITORING_IMPLEMENTATION_STATUS: $p2_implementation_runtime_status
V4_1_DECISION_CHAIN_DESIGN_STATUS: $v4_1_design_runtime_status
V4_1_DECISION_CHAIN_AUTHORIZATION_STATUS: $v4_1_authorization_runtime_status
V4_1_DECISION_CHAIN_IMPLEMENTATION_STATUS: $v4_1_implementation_runtime_status
V4_1_FINAL_INTERACTION_DESIGN_STATUS: $v4_1_design_runtime_status
V4_1_FINAL_INTERACTION_AUTHORIZATION_STATUS: $v4_1_authorization_runtime_status
V4_1_FINAL_INTERACTION_IMPLEMENTATION_STATUS: $v4_1_implementation_runtime_status
RESOLVED_PACKAGE: $resolved_package
RESOLVED_MODE: $resolved_mode
RESOLVED_EDIT_PERMISSION: $resolved_edit_permission
RESOLVED_IMPLEMENTATION_PERMISSION: $resolved_implementation_permission
RESOLVED_PR_CREATION_PERMISSION: $resolved_pr_creation_permission
CANONICAL_FIGMA_DESKTOP_IMPLEMENTATION_ALLOWED: $canonical_figma_desktop_implementation_allowed
MOBILE_IMPLEMENTATION_ALLOWED: $mobile_implementation_allowed
CANONICAL_FIGMA_FILE_KEY: $canonical_figma_file_key
RESOLVED_BRANCH: $resolved_branch
RESOLVED_HANDOFF_STAGE: $resolved_handoff_stage
REQUEST_CLASS: $request_class
CURRENT_PACKAGE_ACTION_ALLOWED: $current_package_action_allowed
CURRENT_PACKAGE_BLOCK_REASON: $current_package_block_reason
NEXT_PACKAGE_ALLOWED: $next_package_allowed
NEXT_PACKAGE_BLOCK_REASON: $next_package_block_reason
OPEN_PR_EVIDENCE_SOURCE: $open_pr_evidence_source
OPEN_PR_NONE_CONFIRMED: $open_pr_none_confirmed
ACTIVE_CONFLICTING_PRS: $active_conflicting_prs

GENERATED_TASK: $resolved_active_block
GENERATED_PACKAGE: $resolved_package
GENERATED_TASK_MODE: $resolved_mode
GENERATED_BRANCH: $resolved_branch
GENERATED_RISK: $resolved_risk
GENERATED_EDIT_PERMISSION: $resolved_edit_permission
GENERATED_IMPLEMENTATION_PERMISSION: $resolved_implementation_permission
GENERATED_PR_CREATION_PERMISSION: $resolved_pr_creation_permission
GENERATED_NEXT_ALLOWED_ACTION: $resolved_next_action

CURRENT_RUNTIME_BRANCH: ${branch:-UNKNOWN}
CURRENT_RUNTIME_WORKTREE_CLEAN: ${worktree:-UNKNOWN}
CURRENT_RUNTIME_MAIN_SYNC: ${main_sync:-UNKNOWN}
CURRENT_RUNTIME_OPEN_PRS: ${open_prs:-UNKNOWN}
CURRENT_EFFECTIVE_STATUS: ${completion_effective_state:-UNKNOWN}

GENERATED_ALLOWED_SCOPE:
EOF
while IFS= read -r scope_item; do
  [[ -n "$scope_item" ]] && printf -- '- %s\n' "$scope_item"
done <<<"$generated_allowed_scope"

echo "GENERATED_BLOCKED_SCOPE:"
while IFS= read -r scope_item; do
  [[ -n "$scope_item" ]] && printf -- '- %s\n' "$scope_item"
done <<<"$generated_blocked_scope"

cat <<'EOF'

NEXT_BLOCKED_ACTION（下一禁止动作）: Do not start an unresolved or blocked package, do not infer authorization from static compatibility fields, and do not bypass the resolved permissions above.
EOF
