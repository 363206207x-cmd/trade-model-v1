#!/usr/bin/env bash

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

CONTRACT_FILE="docs/PROJECT_DELIVERY_CONTRACT.md"
CURRENT_STATE_FILE="docs/PROJECT_CURRENT_STATE.md"
MATRIX_FILE="docs/DELIVERY_PROGRESS_MATRIX.md"
TASK_FILE="docs/CODEX_NEXT_TASK.yml"
ACTIVE_FILE="docs/ACTIVE_MAINLINE_STATUS.yml"

blockers=()

readonly_audit_scope_contract="NO_CODE_NO_TEST_NO_BUSINESS_PR_NO_CLOSED_DEBT_CHANGES"

open_pr_none_confirmed="NO"
open_pr_evidence_input_valid="YES"
case "${V1_OPEN_PR_NONE_CONFIRMED:-NO}" in
  YES|yes|true|1)
    open_pr_none_confirmed="YES"
    ;;
  NO|no|false|0|"")
    open_pr_none_confirmed="NO"
    ;;
  *)
    open_pr_evidence_input_valid="NO"
    ;;
esac
requested_package="${V1_REQUESTED_PACKAGE:-}"

trim() { sed -E 's/^[[:space:]]+|[[:space:]]+$//g'; }

is_false_flag() {
  [[ "$1" == "false" || "$1" == "NO" ]]
}

is_true_flag() {
  [[ "$1" == "true" || "$1" == "YES" ]]
}

is_full_git_sha() {
  [[ "${1:-}" =~ ^[0-9a-fA-F]{40}$ ]]
}

path_is_in_list() {
  local wanted="$1"
  local candidate
  while IFS= read -r candidate; do
    [[ -n "$candidate" && "$candidate" == "$wanted" ]] && return 0
  done <<<"${2:-}"
  return 1
}

changed_paths_from_starting_sha() {
  local starting_sha="$1"
  {
    git diff --name-only "$starting_sha"..HEAD 2>/dev/null || true
    git diff --name-only 2>/dev/null || true
    git diff --cached --name-only 2>/dev/null || true
    git ls-files --others --exclude-standard 2>/dev/null || true
  } | awk 'NF && !seen[$0]++'
}

changed_paths_from_origin_main() {
  {
    git diff --name-only origin/main...HEAD 2>/dev/null || true
    git diff --name-only 2>/dev/null || true
    git diff --cached --name-only 2>/dev/null || true
    git ls-files --others --exclude-standard 2>/dev/null || true
  } | awk 'NF && !seen[$0]++'
}

gate_owner_paths() {
  printf '%s\n' \
    docs/CODEX_NEXT_TASK.yml \
    docs/PRODUCT_SOURCE_OF_TRUTH.md \
    docs/PROJECT_CURRENT_STATE.md \
    docs/DELIVERY_PROGRESS_MATRIX.md \
    docs/ACTIVE_MAINLINE_STATUS.yml \
    scripts/v1-state.sh \
    scripts/codex-next-task.sh \
    scripts/check-workflow-contract.sh
}

owner_amendment_scope_matches() {
  local starting_sha="$1"
  local allowed_paths="$2"
  local changed_path
  is_full_git_sha "$starting_sha" || return 1
  while IFS= read -r changed_path; do
    [[ -z "$changed_path" ]] && continue
    path_is_in_list "$changed_path" "$allowed_paths" || return 1
  done < <(changed_paths_from_starting_sha "$starting_sha")
}

ordinary_package_preserves_gate_owners() {
  local gate_path
  git rev-parse --verify origin/main >/dev/null 2>&1 || return 1
  while IFS= read -r gate_path; do
    [[ -z "$gate_path" ]] && continue
    git diff --quiet origin/main -- "$gate_path" || return 1
  done < <(gate_owner_paths)
}

normalization_commit_matches() {
  local source_parent="$1"
  local allowed_paths="$2"
  local expected_extra_count="$3"
  local candidate="" parent_line="" changed_paths="" changed_count="0"
  local expected_count="0" path candidate_blob main_blob

  normalized_base_full_sha="UNAVAILABLE"
  normalization_match="NO"
  is_full_git_sha "$source_parent" || return 1
  [[ "$expected_extra_count" == "0" ]] || return 1
  git cat-file -e "$source_parent^{commit}" >/dev/null 2>&1 || return 1
  git merge-base --is-ancestor "$source_parent" HEAD >/dev/null 2>&1 || return 1
  candidate="$(git rev-list --reverse --ancestry-path "$source_parent"..HEAD 2>/dev/null | head -n 1)"
  is_full_git_sha "$candidate" || return 1
  parent_line="$(git rev-list --parents -n 1 "$candidate")"
  [[ "$(awk '{print NF}' <<<"$parent_line")" == "2" ]] || return 1
  [[ "$(awk '{print $2}' <<<"$parent_line")" == "$source_parent" ]] || return 1

  changed_paths="$(git diff-tree --no-commit-id --name-only -r "$candidate" | awk 'NF' | sort)"
  changed_count="$(printf '%s\n' "$changed_paths" | awk 'NF {count++} END {print count+0}')"
  expected_count="$(printf '%s\n' "$allowed_paths" | awk 'NF {count++} END {print count+0}')"
  [[ "$changed_count" == "$expected_count" ]] || return 1
  [[ "$expected_count" == "4" ]] || return 1
  [[ "$changed_paths" == "$(printf '%s\n' "$allowed_paths" | awk 'NF' | sort)" ]] || return 1

  while IFS= read -r path; do
    [[ -n "$path" ]] || continue
    candidate_blob="$(git rev-parse "$candidate:$path" 2>/dev/null || true)"
    main_blob="$(git rev-parse "origin/main:$path" 2>/dev/null || true)"
    [[ -n "$candidate_blob" && "$candidate_blob" == "$main_blob" ]] || return 1
  done <<<"$allowed_paths"

  normalized_base_full_sha="$candidate"
  normalization_match="YES"
  return 0
}

emit_resolved_task_state() {
  printf 'MACHINE_AUTHORIZED_PACKAGE: %s\n' "${authorized_next_package_phase:-UNDECLARED}"
  printf 'MACHINE_AUTHORIZED_BRANCH: %s\n' "${authorized_next_package_branch:-UNDECLARED}"
  printf 'MACHINE_AUTHORIZED_STARTING_FULL_SHA: %s\n' "${authorized_next_package_starting_full_sha:-UNDECLARED}"
  printf 'NORMALIZATION_SOURCE_PARENT_SHA: %s\n' "${authorized_next_normalization_source_parent_sha:-UNDECLARED}"
  printf 'NORMALIZED_BASE_FULL_SHA: %s\n' "${normalized_base_full_sha:-UNAVAILABLE}"
  printf 'SOURCE_STARTING_SHA_MATCH: %s\n' "${current_starting_sha_match:-NO}"
  printf 'NORMALIZATION_MATCH: %s\n' "${normalization_match:-NO}"
  printf 'CURRENT_PACKAGE_MATCH: %s\n' "${current_package_match:-NO}"
  printf 'CURRENT_BRANCH_MATCH: %s\n' "${current_branch_match:-NO}"
  printf 'CURRENT_STARTING_SHA_MATCH: %s\n' "${current_starting_sha_match:-NO}"
  printf 'CURRENT_PACKAGE: %s\n' "${current_package_phase:-UNDECLARED}"
  printf 'REQUESTED_PACKAGE: %s\n' "${requested_package:-AUTO}"
  printf 'CURRENT_PACKAGE_ACTION_ALLOWED: %s\n' "${current_package_action_allowed:-NO}"
  printf 'CURRENT_PACKAGE_BLOCK_REASON: %s\n' "${current_package_block_reason:-BLOCKED_UNKNOWN_STATE}"
  printf 'CURRENT_EFFECTIVE_STATUS: %s\n' "${completion_effective_state:-UNKNOWN}"
  printf 'AUTHORIZED_NEXT_PACKAGE: %s\n' "${authorized_next_package_phase:-UNDECLARED}"
  printf 'AUTHORIZED_PACKAGE: %s\n' "${authorized_next_package_phase:-UNDECLARED}"
  printf 'AUTHORIZED_NEXT_TASK_MODE: %s\n' "${authorized_next_package_mode:-UNDECLARED}"
  printf 'NEXT_PACKAGE_ALLOWED: %s\n' "${next_package_allowed:-NO}"
  printf 'NEXT_PACKAGE_BLOCK_REASON: %s\n' "${next_package_block_reason:-BLOCKED_UNKNOWN_STATE}"
  printf 'OPEN_PR_EVIDENCE_SOURCE: %s\n' "${open_pr_evidence_source:-UNAVAILABLE}"
  printf 'OPEN_PR_NONE_CONFIRMED: %s\n' "${open_pr_none_confirmed:-NO}"
  printf 'ACTIVE_CONFLICTING_PRS: %s\n' "${active_conflicting_pr_count:-UNKNOWN}"
  printf 'AUTHORIZED_SUCCESSOR_PRS: %s\n' "${authorized_successor_pr_count:-UNKNOWN}"
  printf 'REQUEST_CLASS: %s\n' "${request_class:-UNKNOWN}"
  printf 'REPOSITORY_EDITS_ALLOWED: %s\n' "${resolved_edit_permission:-false}"
  printf 'IMPLEMENTATION_ALLOWED: %s\n' "${resolved_implementation_permission:-false}"
  printf 'PR_CREATION_ALLOWED: %s\n' "${resolved_pr_creation_permission:-false}"
  printf 'CANONICAL_FIGMA_DESKTOP_IMPLEMENTATION_ALLOWED: %s\n' "${resolved_canonical_figma_desktop_permission:-false}"
  printf 'MOBILE_IMPLEMENTATION_ALLOWED: %s\n' "${resolved_mobile_implementation_permission:-false}"
  printf 'CANONICAL_FIGMA_FILE_KEY: %s\n' "${resolved_canonical_figma_file_key:-NONE}"
  # Compatibility alias for older consumers. New launchers consume PR_CREATION_ALLOWED.
  printf 'IMPLEMENTATION_PR_ALLOWED: %s\n' "${resolved_pr_creation_permission:-false}"
  printf 'P1B_AUTHORIZATION_STATUS: %s\n' "${p1b_authorization_declared_status:-UNDECLARED}"
  printf 'P1B_AUTHORIZATION_RUNTIME_STATUS: %s\n' "${p1b_authorization_runtime_status:-BLOCKED}"
  printf 'P1B_1_STATUS: %s\n' "${p1b_1_declared_status:-UNDECLARED}"
  printf 'P1B_HOME_CORE_DATA_AUTHORIZATION_STATUS: %s\n' "${home_core_data_authorization_runtime_status:-BLOCKED}"
  printf 'P1B_HOME_CORE_DATA_COMPLETION_STATUS: %s\n' "${home_core_data_implementation_status:-UNDECLARED}"
  printf 'PRODUCT_P1B_STATUS: %s\n' "${product_p1b_declared_status:-UNDECLARED}"
  printf 'P2_POSITION_MONITORING_AUTHORIZATION_STATUS: %s\n' "${p2_authorization_runtime_status:-BLOCKED}"
  printf 'P2_POSITION_MONITORING_IMPLEMENTATION_STATUS: %s\n' "${p2_implementation_status:-UNDECLARED}"
  printf 'V4_1_DECISION_CHAIN_DESIGN_STATUS: %s\n' "${v4_1_design_status:-UNDECLARED}"
  printf 'V4_1_DECISION_CHAIN_AUTHORIZATION_STATUS: %s\n' "${v4_1_authorization_declared_status:-UNDECLARED}"
  printf 'V4_1_DECISION_CHAIN_IMPLEMENTATION_STATUS: %s\n' "${v4_1_implementation_status:-UNDECLARED}"
  printf 'V4_1_FINAL_INTERACTION_DESIGN_STATUS: %s\n' "${v4_1_design_status:-UNDECLARED}"
  printf 'V4_1_FINAL_INTERACTION_AUTHORIZATION_STATUS: %s\n' "${v4_1_authorization_declared_status:-UNDECLARED}"
  printf 'V4_1_FINAL_INTERACTION_IMPLEMENTATION_STATUS: %s\n' "${v4_1_implementation_status:-UNDECLARED}"
  printf 'V4_1_TARGET_RUNTIME_REMEDIATION_AUTHORIZATION_STATUS: %s\n' "${v4_1_target_runtime_authorization_runtime_status:-BLOCKED}"
  printf 'V4_1_TARGET_RUNTIME_REMEDIATION_IMPLEMENTATION_STATUS: %s\n' "${v4_1_target_runtime_implementation_status:-UNDECLARED}"
  printf 'V4_1_TARGET_RUNTIME_STATUS: %s\n' "${v4_1_target_runtime_status:-UNDECLARED}"
  printf 'V4_1_TELEGRAM_AUTHORIZATION_STATUS: %s\n' "${v4_1_telegram_authorization_runtime_status:-BLOCKED}"
  printf 'V4_1_TELEGRAM_IMPLEMENTATION_STATUS: %s\n' "${v4_1_telegram_implementation_status:-UNDECLARED}"
  printf 'V4_1_TELEGRAM_LIVE_ACCEPTANCE_STATUS: %s\n' "${v4_1_telegram_live_acceptance_status:-UNDECLARED}"
  printf 'V4_1_TELEGRAM_REMEDIATION_AUTHORIZATION_STATUS: %s\n' "${v4_1_telegram_remediation_authorization_runtime_status:-BLOCKED}"
  printf 'V4_1_TELEGRAM_REMEDIATION_IMPLEMENTATION_STATUS: %s\n' "${v4_1_telegram_remediation_implementation_status:-UNDECLARED}"
  printf 'V4_1_CORE_PRODUCTION_LOOP_AUTHORIZATION_STATUS: %s\n' "${v4_1_core_production_loop_authorization_runtime_status:-BLOCKED}"
  printf 'V4_1_CORE_PRODUCTION_LOOP_IMPLEMENTATION_STATUS: %s\n' "${v4_1_core_production_loop_implementation_status:-UNDECLARED}"
  printf 'V4_1_MACHINE_GATE_OWNER_AMENDMENT_STATUS: %s\n' "${v4_1_machine_gate_owner_amendment_runtime_status:-BLOCKED}"
  printf 'V4_1_BASELINE_RECONCILIATION_GATE_STATUS: %s\n' "${v4_1_baseline_reconciliation_gate_runtime_status:-BLOCKED}"
  printf 'REAL_DATA_HOME_BLOCKER_CLOSURE_AUTHORIZATION_STATUS: %s\n' "${real_data_home_blocker_closure_authorization_runtime_status:-BLOCKED}"
  printf 'REAL_DATA_HOME_BLOCKER_CLOSURE_IMPLEMENTATION_STATUS: %s\n' "${real_data_home_blocker_closure_implementation_status:-UNDECLARED}"
  printf 'ANALYSIS_RUN_IDEMPOTENCY_TX_FIX_AUTHORIZATION_STATUS: %s\n' "${analysis_run_idempotency_tx_fix_authorization_runtime_status:-BLOCKED}"
  printf 'ANALYSIS_RUN_IDEMPOTENCY_TX_FIX_IMPLEMENTATION_STATUS: %s\n' "${analysis_run_idempotency_tx_fix_implementation_status:-UNDECLARED}"
  printf 'V4_1_FINAL_RUNTIME_HOME_ACCESS_IDEMPOTENCY_CLOSURE_AUTHORIZATION_STATUS: %s\n' "${final_runtime_home_closure_authorization_runtime_status:-BLOCKED}"
  printf 'V4_1_FINAL_RUNTIME_HOME_ACCESS_IDEMPOTENCY_CLOSURE_IMPLEMENTATION_STATUS: %s\n' "${final_runtime_home_closure_implementation_status:-UNDECLARED}"
  printf 'LOCAL_REAL_AUTHORIZATION_STATUS: %s\n' "${local_real_authorization_runtime_status:-BLOCKED}"
  printf 'LOCAL_REAL_IMPLEMENTATION_STATUS: %s\n' "${local_real_implementation_status:-UNDECLARED}"
  printf 'FRONTEND_INTERACTION_AUTHORIZATION_STATUS: %s\n' "${frontend_interaction_authorization_runtime_status:-BLOCKED}"
  printf 'FRONTEND_INTERACTION_IMPLEMENTATION_STATUS: %s\n' "${frontend_interaction_implementation_status:-UNDECLARED}"
  printf 'MULTI_USER_AUTHORIZATION_STATUS: %s\n' "${multi_user_authorization_runtime_status:-BLOCKED}"
  printf 'MULTI_USER_IMPLEMENTATION_STATUS: %s\n' "${multi_user_implementation_status:-UNDECLARED}"
  printf 'P1A_COMPLETION_STATUS: %s\n' "${p1a_completion_status:-BLOCKED}"
  printf 'AUTHORIZATION_STATUS: %s\n' "${authorization_status:-BLOCKED}"
  printf 'RESOLVED_FROM_STATE: YES\n'
  printf 'RESOLUTION_STATUS: %s\n' "${resolution_status:-BLOCKED}"
  printf 'RESOLUTION_BLOCK_REASON: %s\n' "${resolution_block_reason:-BLOCKED_UNKNOWN_STATE}"
  printf 'RESOLVED_PACKAGE: %s\n' "${resolved_package:-BLOCKED}"
  printf 'RESOLVED_MODE: %s\n' "${resolved_mode:-BLOCKED}"
  printf 'RESOLVED_BRANCH: %s\n' "${resolved_branch:-NONE}"
  printf 'RESOLVED_ACTIVE_BLOCK: %s\n' "${resolved_active_block:-BLOCKED}"
  printf 'RESOLVED_RISK: %s\n' "${resolved_risk:-A}"
  printf 'RESOLVED_SCOPE_PROFILE: %s\n' "${resolved_scope_profile:-NONE}"
  printf 'RESOLVED_HANDOFF_STAGE: %s\n' "${resolved_handoff_stage:-BLOCKED}"
  printf 'RESOLVED_EDIT_PERMISSION: %s\n' "${resolved_edit_permission:-false}"
  printf 'RESOLVED_IMPLEMENTATION_PERMISSION: %s\n' "${resolved_implementation_permission:-false}"
  printf 'RESOLVED_PR_CREATION_PERMISSION: %s\n' "${resolved_pr_creation_permission:-false}"
  printf 'RESOLVED_NEXT_ACTION: %s\n' "${resolved_next_action:-No task is authorized}"
}

classify_package_request() {
  request_class="UNKNOWN"
  if [[ -n "${requested_package:-}" ]]; then
    if [[ "$requested_package" == "${current_package_phase:-UNDECLARED}" ]]; then
      request_class="CURRENT_PACKAGE_CONTINUATION"
    elif [[ "$requested_package" == "${authorized_next_package_phase:-UNDECLARED}" ]]; then
      request_class="AUTHORIZED_IMPLEMENTATION_PACKAGE"
    elif [[ "$requested_package" == "${blocked_package_phase:-UNDECLARED}" ]]; then
      request_class="SUCCESSOR_PACKAGE"
    fi
    return 0
  fi

  case "${completion_effective_state:-UNKNOWN}" in
    PENDING_MERGED_MAIN)
      request_class="CURRENT_PACKAGE_CONTINUATION"
      ;;
    EFFECTIVE_MERGED_MAIN)
      request_class="AUTHORIZED_IMPLEMENTATION_PACKAGE"
      ;;
  esac
}

resolve_task_handoff() {
  resolution_status="BLOCKED"
  resolution_block_reason="BLOCKED_UNKNOWN_RESOLVED_STATE"
  current_package_action_allowed="NO"
  current_package_block_reason="BLOCKED_UNKNOWN_CURRENT_PACKAGE_STATE"
  next_package_allowed="NO"
  next_package_block_reason="${next_task_authorization_status:-BLOCKED_UNKNOWN_STATE}"
  resolved_package="BLOCKED"
  resolved_mode="BLOCKED"
  resolved_branch="NONE"
  resolved_active_block="BLOCKED"
  resolved_risk="A"
  resolved_scope_profile="NONE"
  resolved_handoff_stage="BLOCKED"
  resolved_edit_permission="false"
  resolved_implementation_permission="false"
  resolved_pr_creation_permission="false"
  resolved_canonical_figma_desktop_permission="false"
  resolved_mobile_implementation_permission="false"
  resolved_canonical_figma_file_key="NONE"
  resolved_next_action="No task is authorized until runtime state resolution succeeds"

  classify_package_request

  if [[ "$open_pr_evidence_input_valid" != "YES" ]]; then
    current_package_block_reason="BLOCKED_INVALID_OPEN_PR_EVIDENCE"
    next_package_block_reason="BLOCKED_INVALID_OPEN_PR_EVIDENCE"
  elif [[ "${product_source_gate_status:-BLOCKED}" != "PASS" ]]; then
    current_package_block_reason="BLOCKED_PRODUCT_SOURCE_GATE"
    next_package_block_reason="BLOCKED_PRODUCT_SOURCE_GATE"
  elif [[ "${worktree_clean:-No}" != "Yes" ]]; then
    current_package_block_reason="BLOCKED_WORKTREE_DIRTY"
    next_package_block_reason="BLOCKED_WORKTREE_DIRTY"
  else
    if [[ "${completion_effective_state:-UNKNOWN}" != "PENDING_MERGED_MAIN" ]]; then
      current_package_block_reason="BLOCKED_CURRENT_PACKAGE_NOT_PENDING"
    elif [[ "${branch:-UNKNOWN}" != "${current_package_branch:-UNDECLARED}" ]]; then
      current_package_block_reason="BLOCKED_CURRENT_PACKAGE_BRANCH_MISMATCH"
    elif { [[ "$current_package_phase" == "TRINE_LOGIC_V4_1_BASELINE_RECONCILIATION_GATE" \
      || "$current_package_phase" == "TRINE_LOGIC_V4_1_ANALYSIS_RUN_IDEMPOTENCY_TX_FIX_AUTHORIZATION" \
      || "$current_package_phase" == "TRINE_LOGIC_V4_1_FINAL_RUNTIME_HOME_ACCESS_IDEMPOTENCY_CLOSURE_AUTHORIZATION" ]] \
      && [[ "${machine_identity_allowed:-NO}" != "YES" ]]; }; then
      current_package_block_reason="${machine_identity_block_reason:-BLOCKED_EXACT_MACHINE_IDENTITY}"
    elif [[ "${current_package_pr_count:-UNKNOWN}" != "0" \
      && "${current_package_pr_count:-UNKNOWN}" != "1" ]]; then
      current_package_block_reason="BLOCKED_UNKNOWN_CURRENT_PACKAGE_PR_STATE"
    else
      current_package_action_allowed="YES"
      current_package_block_reason="NONE"
    fi

    if [[ "$current_package_phase" == "TRINE_LOGIC_V4_1_BASELINE_RECONCILIATION_GATE" \
      || "$current_package_phase" == "TRINE_LOGIC_V4_1_ANALYSIS_RUN_IDEMPOTENCY_TX_FIX_AUTHORIZATION" \
      || "$current_package_phase" == "TRINE_LOGIC_V4_1_FINAL_RUNTIME_HOME_ACCESS_IDEMPOTENCY_CLOSURE_AUTHORIZATION" ]]; then
      if [[ "${machine_gate_effective_on_origin_main:-NO}" != "YES" ]]; then
        next_package_block_reason="${next_task_authorization_status:-BLOCKED_PENDING_AUTHORIZATION_MERGED_MAIN}"
      elif [[ "${machine_identity_allowed:-NO}" != "YES" ]]; then
        next_package_block_reason="${machine_identity_block_reason:-BLOCKED_EXACT_MACHINE_IDENTITY}"
      elif [[ "${current_package_pr_count:-UNKNOWN}" == "UNKNOWN" ]]; then
        next_package_block_reason="BLOCKED_UNKNOWN_CURRENT_PACKAGE_PR_STATE"
      elif [[ "${current_package_pr_count:-0}" != "0" ]]; then
        next_package_block_reason="BLOCKED_CURRENT_PACKAGE_PR_PRESENT_OR_UNKNOWN"
      elif [[ "${active_conflicting_pr_count:-UNKNOWN}" == "UNKNOWN" ]]; then
        next_package_block_reason="BLOCKED_UNKNOWN_ACTIVE_PR_STATE"
      elif [[ "${active_conflicting_pr_count:-0}" != "0" ]]; then
        next_package_block_reason="BLOCKED_ACTIVE_CONFLICTING_PR"
      elif [[ "${next_transition_allowed:-NO}" != "YES" ]]; then
        next_package_block_reason="${next_task_authorization_status:-BLOCKED_TRANSITION_NOT_AUTHORIZED}"
      else
        next_package_allowed="YES"
        next_package_block_reason="NONE"
      fi
    elif [[ "${completion_effective_state:-UNKNOWN}" != "EFFECTIVE_MERGED_MAIN" ]]; then
      next_package_block_reason="${next_task_authorization_status:-BLOCKED_PENDING_P0_MERGED_MAIN}"
    elif [[ "${current_package_pr_count:-UNKNOWN}" == "UNKNOWN" ]]; then
      next_package_block_reason="BLOCKED_UNKNOWN_CURRENT_PACKAGE_PR_STATE"
    elif [[ "${current_package_pr_count:-0}" != "0" ]]; then
      next_package_block_reason="BLOCKED_CURRENT_PACKAGE_PR_PRESENT_OR_UNKNOWN"
    elif [[ "${active_conflicting_pr_count:-UNKNOWN}" == "UNKNOWN" ]]; then
      next_package_block_reason="BLOCKED_UNKNOWN_ACTIVE_PR_STATE"
    elif [[ "${active_conflicting_pr_count:-0}" != "0" ]]; then
      next_package_block_reason="BLOCKED_ACTIVE_CONFLICTING_PR"
    elif [[ "${next_transition_allowed:-NO}" != "YES" ]]; then
      next_package_block_reason="${next_task_authorization_status:-BLOCKED_TRANSITION_NOT_AUTHORIZED}"
    elif [[ "${authorized_next_package_mode:-UNDECLARED}" == "READ_ONLY_PRODUCT_AUDIT" \
      && "${product_audit_allowed:-NO}" != "YES" ]]; then
      next_package_block_reason="${product_audit_blocker:-BLOCKED_PRODUCT_AUDIT_POLICY}"
    else
      next_package_allowed="YES"
      next_package_block_reason="NONE"
    fi
  fi

  if [[ "$request_class" == "CURRENT_PACKAGE_CONTINUATION" ]]; then
    if [[ "$current_package_action_allowed" != "YES" ]]; then
      resolution_block_reason="$current_package_block_reason"
      return 0
    fi

    resolution_status="ALLOWED"
    resolution_block_reason="NONE"
    resolved_package="$current_package_phase"
    resolved_mode="$current_package_mode"
    resolved_branch="$current_package_branch"
    resolved_active_block="$current_package_active_block"
    resolved_risk="$current_package_risk"
    resolved_scope_profile="CURRENT_PACKAGE"
    resolved_handoff_stage="CURRENT_PACKAGE_REMEDIATION_REVIEW"
    if [[ "$current_package_phase" == "P0_PRODUCT_FOUNDATION" ]]; then
      resolved_handoff_stage="P0_REMEDIATION_REVIEW"
    elif [[ "$current_package_phase" == "P1A_HOME_ALIGNMENT_READINESS_AND_GAP_AUDIT" ]]; then
      resolved_handoff_stage="P1B_AUTHORIZATION_REMEDIATION_REVIEW"
    elif [[ "$current_package_phase" == "P1B_HOME_CORE_DATA_AUTHORIZATION" ]]; then
      resolved_handoff_stage="P1B_HOME_CORE_DATA_AUTHORIZATION_REVIEW"
    elif [[ "$current_package_phase" == "P2_POSITION_MONITORING_AUTHORIZATION" ]]; then
      resolved_handoff_stage="P2_POSITION_MONITORING_AUTHORIZATION_REVIEW"
    elif [[ "$current_package_phase" == "FUNDAMENTAL_AI_V4_1_FINAL_INTERACTION_AUTHORIZATION" ]]; then
      resolved_handoff_stage="V4_1_FINAL_INTERACTION_AUTHORIZATION_REVIEW"
    elif [[ "$current_package_phase" == "FUNDAMENTAL_AI_V4_1_TARGET_RUNTIME_BLOCKER_REMEDIATION_AUTHORIZATION" ]]; then
      resolved_handoff_stage="V4_1_TARGET_RUNTIME_REMEDIATION_AUTHORIZATION_REVIEW"
    elif [[ "$current_package_phase" == "FUNDAMENTAL_AI_V4_1_TELEGRAM_HIGH_VALUE_ALERT_CHANNEL_AUTHORIZATION" ]]; then
      resolved_handoff_stage="V4_1_TELEGRAM_AUTHORIZATION_REVIEW"
    elif [[ "$current_package_phase" == "FUNDAMENTAL_AI_V4_1_TELEGRAM_HIGH_VALUE_ALERT_CHANNEL_REMEDIATION_AUTHORIZATION" ]]; then
      resolved_handoff_stage="V4_1_TELEGRAM_REMEDIATION_AUTHORIZATION_REVIEW"
    elif [[ "$current_package_phase" == "FUNDAMENTAL_AI_V4_1_CORE_PRODUCTION_LOOP_AUTOMATION_AUTHORIZATION" ]]; then
      resolved_handoff_stage="V4_1_CORE_PRODUCTION_LOOP_AUTHORIZATION_REVIEW"
    elif [[ "$current_package_phase" == "TRINE_LOGIC_V4_1_BASELINE_RECONCILIATION_GATE" ]]; then
      resolved_handoff_stage="V4_1_BASELINE_RECONCILIATION_REVIEW"
    elif [[ "$current_package_phase" == "TRINE_LOGIC_V4_1_ANALYSIS_RUN_IDEMPOTENCY_TX_FIX_AUTHORIZATION" ]]; then
      resolved_handoff_stage="ANALYSIS_RUN_IDEMPOTENCY_TX_FIX_AUTHORIZATION_REVIEW"
    elif [[ "$current_package_phase" == "TRINE_LOGIC_V4_1_FINAL_RUNTIME_HOME_ACCESS_IDEMPOTENCY_CLOSURE_AUTHORIZATION" ]]; then
      resolved_handoff_stage="V41_FINAL_RUNTIME_HOME_ACCESS_IDEMPOTENCY_CLOSURE_AUTHORIZATION_REVIEW"
    elif [[ "$current_package_phase" == "LOCAL_REAL_READINESS_SYNC_AND_REAL_ANALYSIS_ENABLEMENT_AUTHORIZATION" ]]; then
      resolved_handoff_stage="LOCAL_REAL_AUTHORIZATION_REVIEW"
    elif [[ "$current_package_phase" == "FRONTEND_INTERACTION_RUNTIME_CLOSURE_AUTHORIZATION" ]]; then
      resolved_handoff_stage="FRONTEND_INTERACTION_AUTHORIZATION_REVIEW"
    elif [[ "$current_package_phase" == "MULTI_USER_ACCOUNT_REGISTRATION_CLOSURE_AUTHORIZATION" ]]; then
      resolved_handoff_stage="MULTI_USER_AUTHORIZATION_REVIEW"
    fi
    if [[ "${current_package_pr_count:-0}" == "1" && "${current_package_pr_draft:-UNKNOWN}" == "false" ]]; then
      resolved_handoff_stage="CURRENT_PACKAGE_FINAL_MERGE_PATH"
      if [[ "$current_package_phase" == "P0_PRODUCT_FOUNDATION" ]]; then
        resolved_handoff_stage="P0_FINAL_MERGE_PATH"
      elif [[ "$current_package_phase" == "P1A_HOME_ALIGNMENT_READINESS_AND_GAP_AUDIT" ]]; then
        resolved_handoff_stage="P1B_AUTHORIZATION_FINAL_MERGE_PATH"
      elif [[ "$current_package_phase" == "P1B_HOME_CORE_DATA_AUTHORIZATION" ]]; then
        resolved_handoff_stage="P1B_HOME_CORE_DATA_AUTHORIZATION_FINAL_MERGE_PATH"
      elif [[ "$current_package_phase" == "P2_POSITION_MONITORING_AUTHORIZATION" ]]; then
        resolved_handoff_stage="P2_POSITION_MONITORING_AUTHORIZATION_FINAL_MERGE_PATH"
      elif [[ "$current_package_phase" == "FUNDAMENTAL_AI_V4_1_FINAL_INTERACTION_AUTHORIZATION" ]]; then
        resolved_handoff_stage="V4_1_FINAL_INTERACTION_AUTHORIZATION_FINAL_MERGE_PATH"
      elif [[ "$current_package_phase" == "FUNDAMENTAL_AI_V4_1_TARGET_RUNTIME_BLOCKER_REMEDIATION_AUTHORIZATION" ]]; then
        resolved_handoff_stage="V4_1_TARGET_RUNTIME_REMEDIATION_AUTHORIZATION_FINAL_MERGE_PATH"
      elif [[ "$current_package_phase" == "FUNDAMENTAL_AI_V4_1_TELEGRAM_HIGH_VALUE_ALERT_CHANNEL_AUTHORIZATION" ]]; then
        resolved_handoff_stage="V4_1_TELEGRAM_AUTHORIZATION_FINAL_MERGE_PATH"
      elif [[ "$current_package_phase" == "FUNDAMENTAL_AI_V4_1_TELEGRAM_HIGH_VALUE_ALERT_CHANNEL_REMEDIATION_AUTHORIZATION" ]]; then
        resolved_handoff_stage="V4_1_TELEGRAM_REMEDIATION_AUTHORIZATION_FINAL_MERGE_PATH"
      elif [[ "$current_package_phase" == "FUNDAMENTAL_AI_V4_1_CORE_PRODUCTION_LOOP_AUTOMATION_AUTHORIZATION" ]]; then
        resolved_handoff_stage="V4_1_CORE_PRODUCTION_LOOP_AUTHORIZATION_FINAL_MERGE_PATH"
      elif [[ "$current_package_phase" == "TRINE_LOGIC_V4_1_BASELINE_RECONCILIATION_GATE" ]]; then
        resolved_handoff_stage="V4_1_BASELINE_RECONCILIATION_FINAL_MERGE_PATH"
      elif [[ "$current_package_phase" == "TRINE_LOGIC_V4_1_ANALYSIS_RUN_IDEMPOTENCY_TX_FIX_AUTHORIZATION" ]]; then
        resolved_handoff_stage="ANALYSIS_RUN_IDEMPOTENCY_TX_FIX_AUTHORIZATION_FINAL_MERGE_PATH"
      elif [[ "$current_package_phase" == "TRINE_LOGIC_V4_1_FINAL_RUNTIME_HOME_ACCESS_IDEMPOTENCY_CLOSURE_AUTHORIZATION" ]]; then
        resolved_handoff_stage="V41_FINAL_RUNTIME_HOME_ACCESS_IDEMPOTENCY_CLOSURE_AUTHORIZATION_FINAL_MERGE_PATH"
      elif [[ "$current_package_phase" == "LOCAL_REAL_READINESS_SYNC_AND_REAL_ANALYSIS_ENABLEMENT_AUTHORIZATION" ]]; then
        resolved_handoff_stage="LOCAL_REAL_AUTHORIZATION_FINAL_MERGE_PATH"
      elif [[ "$current_package_phase" == "FRONTEND_INTERACTION_RUNTIME_CLOSURE_AUTHORIZATION" ]]; then
        resolved_handoff_stage="FRONTEND_INTERACTION_AUTHORIZATION_FINAL_MERGE_PATH"
      elif [[ "$current_package_phase" == "MULTI_USER_ACCOUNT_REGISTRATION_CLOSURE_AUTHORIZATION" ]]; then
        resolved_handoff_stage="MULTI_USER_AUTHORIZATION_FINAL_MERGE_PATH"
      fi
    fi
    resolved_edit_permission="$current_package_repository_edits_allowed"
    resolved_implementation_permission="$current_package_implementation_allowed"
    resolved_pr_creation_permission="$current_package_implementation_pr_allowed"
    resolved_next_action="$current_package_next_action"
    return 0
  fi

  if [[ "$request_class" != "AUTHORIZED_IMPLEMENTATION_PACKAGE" \
    && "$request_class" != "SUCCESSOR_PACKAGE" ]]; then
    resolution_block_reason="BLOCKED_UNKNOWN_RESOLVED_STATE"
    return 0
  fi

  if [[ -n "${requested_package:-}" && "$requested_package" == "${blocked_package_phase:-UNDECLARED}" ]]; then
    resolution_block_reason="BLOCKED_REQUESTED_PACKAGE_NOT_AUTHORIZED"
    return 0
  fi

  if [[ -n "${requested_package:-}" && "$requested_package" != "${authorized_next_package_phase:-UNDECLARED}" ]]; then
    resolution_block_reason="BLOCKED_UNKNOWN_REQUESTED_PACKAGE"
    return 0
  fi

  if [[ "$next_package_allowed" != "YES" ]]; then
    resolution_block_reason="$next_package_block_reason"
    return 0
  fi

  resolution_status="ALLOWED"
  resolution_block_reason="NONE"
  current_package_action_allowed="YES"
  current_package_block_reason="NONE"
  resolved_package="$authorized_next_package_phase"
  resolved_mode="$authorized_next_package_mode"
  resolved_branch="$authorized_next_package_branch"
  resolved_active_block="$authorized_next_package_active_block"
  resolved_risk="$authorized_next_package_risk"
  resolved_scope_profile="AUTHORIZED_NEXT_PACKAGE"
  resolved_handoff_stage="AUTHORIZED_SUCCESSOR"
  if [[ "$authorized_next_package_mode" == "READ_ONLY_PRODUCT_AUDIT" ]]; then
    resolved_handoff_stage="P1A_READ_ONLY_AUDIT"
  elif [[ "$authorized_next_package_phase" == "P1B_HOME_ALIGNMENT_FIRST_IMPLEMENTATION" ]]; then
    resolved_handoff_stage="P1B_HOME_IMPLEMENTATION"
  elif [[ "$authorized_next_package_phase" == "P1B_HOME_CORE_DATA_COMPLETION" ]]; then
    resolved_handoff_stage="P1B_HOME_CORE_DATA_IMPLEMENTATION"
  elif [[ "$authorized_next_package_phase" == "P2_POSITION_MONITORING_BACKEND_IMPLEMENTATION" ]]; then
    resolved_handoff_stage="P2_POSITION_MONITORING_BACKEND_IMPLEMENTATION"
  elif [[ "$authorized_next_package_phase" == "FUNDAMENTAL_AI_V4_1_FINAL_INTERACTION_PAGE_AND_RUNTIME_IMPLEMENTATION" ]]; then
    resolved_handoff_stage="FUNDAMENTAL_AI_V4_1_FINAL_INTERACTION_PAGE_AND_RUNTIME_IMPLEMENTATION"
  elif [[ "$authorized_next_package_phase" == "FUNDAMENTAL_AI_V4_1_TARGET_RUNTIME_BLOCKER_REMEDIATION" ]]; then
    resolved_handoff_stage="FUNDAMENTAL_AI_V4_1_TARGET_RUNTIME_BLOCKER_REMEDIATION"
  elif [[ "$authorized_next_package_phase" == "FUNDAMENTAL_AI_V4_1_TELEGRAM_HIGH_VALUE_ALERT_CHANNEL_INTEGRATION" ]]; then
    resolved_handoff_stage="FUNDAMENTAL_AI_V4_1_TELEGRAM_HIGH_VALUE_ALERT_CHANNEL_INTEGRATION"
  elif [[ "$authorized_next_package_phase" == "FUNDAMENTAL_AI_V4_1_CORE_PRODUCTION_LOOP_AUTOMATION" ]]; then
    resolved_handoff_stage="FUNDAMENTAL_AI_V4_1_CORE_PRODUCTION_LOOP_AUTOMATION"
  elif [[ "$authorized_next_package_phase" == "REAL_DATA_HOME_BLOCKER_CLOSURE" ]]; then
    resolved_handoff_stage="REAL_DATA_HOME_BLOCKER_CLOSURE"
  elif [[ "$authorized_next_package_phase" == "ANALYSIS_RUN_IDEMPOTENCY_TRANSACTION_BOUNDARY_FIX" ]]; then
    resolved_handoff_stage="ANALYSIS_RUN_IDEMPOTENCY_TRANSACTION_BOUNDARY_FIX"
  elif [[ "$authorized_next_package_phase" == "V41_FINAL_RUNTIME_HOME_ACCESS_IDEMPOTENCY_CLOSURE" ]]; then
    resolved_handoff_stage="V41_FINAL_RUNTIME_HOME_ACCESS_IDEMPOTENCY_CLOSURE"
  elif [[ "$authorized_next_package_phase" == "LOCAL_REAL_READINESS_SYNC_AND_REAL_ANALYSIS_ENABLEMENT" ]]; then
    resolved_handoff_stage="LOCAL_REAL_READINESS_SYNC_AND_REAL_ANALYSIS_ENABLEMENT"
  elif [[ "$authorized_next_package_phase" == "FRONTEND_INTERACTION_RUNTIME_CLOSURE" ]]; then
    resolved_handoff_stage="FRONTEND_INTERACTION_RUNTIME_CLOSURE"
  elif [[ "$authorized_next_package_phase" == "MULTI_USER_ACCOUNT_REGISTRATION_CLOSURE" ]]; then
    resolved_handoff_stage="MULTI_USER_ACCOUNT_REGISTRATION_CLOSURE"
  fi
  resolved_edit_permission="$authorized_next_repository_edits_allowed"
  resolved_implementation_permission="$authorized_next_implementation_allowed"
  resolved_pr_creation_permission="$authorized_next_implementation_pr_allowed"
  resolved_canonical_figma_desktop_permission="$authorized_next_canonical_figma_desktop_implementation_allowed"
  resolved_mobile_implementation_permission="$authorized_next_mobile_implementation_allowed"
  resolved_canonical_figma_file_key="$authorized_next_canonical_figma_file_key"
  resolved_next_action="$authorized_next_package_next_action"
}

resolve_task_handoff_legacy_reference() {
  if [[ "${product_source_gate_status:-BLOCKED}" != "PASS" ]]; then
    next_package_block_reason="BLOCKED_PRODUCT_SOURCE_GATE"
    return 0
  fi
  if [[ "${worktree_clean:-No}" != "Yes" ]]; then
    next_package_block_reason="BLOCKED_WORKTREE_DIRTY"
    return 0
  fi
  if [[ "${current_package_pr_count:-UNKNOWN}" != "0" && "${current_package_pr_count:-UNKNOWN}" != "1" ]]; then
    next_package_block_reason="BLOCKED_UNKNOWN_CURRENT_PACKAGE_PR_STATE"
    return 0
  fi
  if [[ "${active_conflicting_pr_count:-UNKNOWN}" == "UNKNOWN" ]]; then
    next_package_block_reason="BLOCKED_UNKNOWN_ACTIVE_PR_STATE"
    return 0
  fi
  if [[ "${active_conflicting_pr_count:-0}" != "0" ]]; then
    next_package_block_reason="BLOCKED_ACTIVE_CONFLICTING_PR"
    return 0
  fi
  if [[ -n "${requested_package:-}" && "${requested_package}" == "${blocked_package_phase:-UNDECLARED}" ]]; then
    next_package_block_reason="BLOCKED_REQUESTED_PACKAGE_NOT_AUTHORIZED"
    return 0
  fi

  if [[ "${completion_effective_state:-UNKNOWN}" == "PENDING_MERGED_MAIN" ]]; then
    resolution_status="ALLOWED"
    resolved_package="$current_package_phase"
    resolved_mode="$current_package_mode"
    resolved_branch="$current_package_branch"
    resolved_active_block="$current_package_active_block"
    resolved_risk="$current_package_risk"
    resolved_scope_profile="CURRENT_PACKAGE"
    resolved_handoff_stage="P0_REMEDIATION_REVIEW"
    if [[ "${current_package_pr_count:-0}" == "1" && "${current_package_pr_draft:-UNKNOWN}" == "false" ]]; then
      resolved_handoff_stage="P0_FINAL_MERGE_PATH"
    fi
    resolved_edit_permission="$current_package_repository_edits_allowed"
    resolved_implementation_permission="$current_package_implementation_allowed"
    resolved_pr_creation_permission="$current_package_implementation_pr_allowed"
    resolved_next_action="$current_package_next_action"
    return 0
  fi

  if [[ "${completion_effective_state:-UNKNOWN}" != "EFFECTIVE_MERGED_MAIN" ]]; then
    next_package_block_reason="BLOCKED_UNKNOWN_OR_INEFFECTIVE_P0_STATE"
    return 0
  fi
  if [[ "${p1a_transition_allowed:-NO}" != "YES" ]]; then
    next_package_block_reason="${next_task_authorization_status:-BLOCKED_P1A_TRANSITION}"
    return 0
  fi
  if [[ "${product_audit_allowed:-NO}" != "YES" ]]; then
    next_package_block_reason="${product_audit_blocker:-BLOCKED_PRODUCT_AUDIT_POLICY}"
    return 0
  fi

  resolution_status="ALLOWED"
  next_package_allowed="YES"
  next_package_block_reason="NONE"
  resolved_package="$authorized_next_package_phase"
  resolved_mode="$authorized_next_package_mode"
  resolved_branch="$authorized_next_package_branch"
  resolved_active_block="$authorized_next_package_active_block"
  resolved_risk="$authorized_next_package_risk"
  resolved_scope_profile="AUTHORIZED_NEXT_PACKAGE"
  resolved_handoff_stage="P1A_READ_ONLY_AUDIT"
  resolved_edit_permission="$authorized_next_repository_edits_allowed"
  resolved_implementation_permission="$authorized_next_implementation_allowed"
  resolved_pr_creation_permission="$authorized_next_implementation_pr_allowed"
  resolved_next_action="$authorized_next_package_next_action"
}

evaluate_p0_to_p1a_transition() {
  local current_mode="$1" next_mode="$2" completion_state="$3" synced_main_status="$4"
  local source_gate_status="$5" merged_main_validation_status="$6" repository_edits_allowed="$7"
  local implementation_allowed="$8" implementation_pr_allowed="$9"

  effective_task_mode="$current_mode"
  p1a_transition_allowed="NO"
  p1a_completion_status="NOT_STARTED"
  next_transition_allowed="NO"
  authorization_status="BLOCKED"
  next_task_authorization_status="BLOCKED_INVALID_TRANSITION_CONTRACT"
  p1b_authorization_runtime_status="BLOCKED_AS_EXPECTED"

  [[ "$current_mode" == "PRODUCT_FOUNDATION_REMEDIATION" ]] || return 0
  [[ "$next_mode" == "READ_ONLY_PRODUCT_AUDIT" ]] || return 0
  if ! is_false_flag "$repository_edits_allowed" || ! is_false_flag "$implementation_allowed" || ! is_false_flag "$implementation_pr_allowed"; then
    next_task_authorization_status="BLOCKED_P1A_NOT_READ_ONLY"
    return 0
  fi
  if [[ "$completion_state" != "EFFECTIVE_MERGED_MAIN" ]]; then
    next_task_authorization_status="BLOCKED_PENDING_P0_MERGED_MAIN"
    return 0
  fi
  if [[ "$synced_main_status" != "YES" ]]; then
    next_task_authorization_status="BLOCKED_PENDING_LOCAL_ORIGIN_MAIN_MATCH"
    return 0
  fi
  if [[ "$source_gate_status" != "PASS" ]]; then
    next_task_authorization_status="BLOCKED_PRODUCT_SOURCE_GATE"
    return 0
  fi
  if [[ "$merged_main_validation_status" != "PASS" ]]; then
    next_task_authorization_status="BLOCKED_P0_MERGED_MAIN_VALIDATION"
    return 0
  fi

  effective_task_mode="$next_mode"
  p1a_transition_allowed="YES"
  next_transition_allowed="YES"
  authorization_status="APPROVED"
  next_task_authorization_status="ALLOWED"
}

evaluate_p1a_to_p1b_transition() {
  local current_phase="$1" current_status="$2" current_mode="$3" next_phase="$4" next_mode="$5"
  local completion_state="$6" synced_main_status="$7" source_gate_status="$8"
  local merged_main_validation_status="$9" repository_edits_allowed="${10}"
  local implementation_allowed="${11}" implementation_pr_allowed="${12}"
  local declared_authorization_status="${13}"

  effective_task_mode="$current_mode"
  p1a_transition_allowed="NO"
  p1a_completion_status="BLOCKED"
  next_transition_allowed="NO"
  authorization_status="BLOCKED"
  next_task_authorization_status="BLOCKED_INVALID_TRANSITION_CONTRACT"
  p1b_authorization_runtime_status="BLOCKED"

  [[ "$current_phase" == "P1A_HOME_ALIGNMENT_READINESS_AND_GAP_AUDIT" ]] || return 0
  [[ "$next_phase" == "P1B_HOME_ALIGNMENT_FIRST_IMPLEMENTATION" ]] || return 0
  [[ "$next_mode" == "IMPLEMENTATION" ]] || return 0

  if [[ "$current_status" != "COMPLETED" ]]; then
    next_task_authorization_status="BLOCKED_P1A_AUDIT_INCOMPLETE"
    return 0
  fi
  p1a_transition_allowed="YES"
  p1a_completion_status="PASS"

  if [[ "$declared_authorization_status" != "EFFECTIVE_PENDING_MERGED_MAIN" ]]; then
    next_task_authorization_status="BLOCKED_P1B_SCOPE_NOT_AUTHORIZED"
    return 0
  fi
  if ! is_true_flag "$repository_edits_allowed" \
    || ! is_true_flag "$implementation_allowed" \
    || ! is_true_flag "$implementation_pr_allowed"; then
    next_task_authorization_status="BLOCKED_P1B_IMPLEMENTATION_PERMISSIONS_INCOMPLETE"
    return 0
  fi
  if [[ "$completion_state" != "EFFECTIVE_MERGED_MAIN" ]]; then
    next_task_authorization_status="BLOCKED_PENDING_P1B_AUTHORIZATION_MERGED_MAIN"
    p1b_authorization_runtime_status="PENDING_MERGED_MAIN"
    return 0
  fi
  if [[ "$synced_main_status" != "YES" ]]; then
    next_task_authorization_status="BLOCKED_PENDING_LOCAL_ORIGIN_MAIN_MATCH"
    return 0
  fi
  if [[ "$source_gate_status" != "PASS" ]]; then
    next_task_authorization_status="BLOCKED_PRODUCT_SOURCE_GATE"
    return 0
  fi
  if [[ "$merged_main_validation_status" != "PASS" ]]; then
    next_task_authorization_status="BLOCKED_P1B_AUTHORIZATION_MERGED_MAIN_VALIDATION"
    return 0
  fi

  effective_task_mode="$next_mode"
  next_transition_allowed="YES"
  authorization_status="APPROVED"
  next_task_authorization_status="ALLOWED"
  p1b_authorization_runtime_status="EFFECTIVE_MERGED_MAIN"
}

evaluate_home_core_data_transition() {
  local current_phase="$1" current_status="$2" current_mode="$3" next_phase="$4" next_mode="$5"
  local completion_state="$6" synced_main_status="$7" source_gate_status="$8"
  local merged_main_validation_status="$9" repository_edits_allowed="${10}"
  local implementation_allowed="${11}" implementation_pr_allowed="${12}"
  local declared_authorization_status="${13}" predecessor_status="${14}"

  effective_task_mode="$current_mode"
  p1a_transition_allowed="YES"
  p1a_completion_status="PASS"
  p1b_1_completion_status="BLOCKED"
  next_transition_allowed="NO"
  authorization_status="BLOCKED"
  next_task_authorization_status="BLOCKED_INVALID_TRANSITION_CONTRACT"
  home_core_data_authorization_runtime_status="BLOCKED"
  p1b_authorization_runtime_status="BLOCKED"

  [[ "$current_phase" == "P1B_HOME_CORE_DATA_AUTHORIZATION" ]] || return 0
  [[ "$current_mode" == "BOUNDED_PRODUCT_DECISION_AND_AUTHORIZATION" ]] || return 0
  [[ "$next_phase" == "P1B_HOME_CORE_DATA_COMPLETION" ]] || return 0
  [[ "$next_mode" == "IMPLEMENTATION" ]] || return 0

  if [[ "$predecessor_status" != "EFFECTIVE_MERGED_MAIN" ]]; then
    next_task_authorization_status="BLOCKED_P1B_1_NOT_EFFECTIVE_MERGED_MAIN"
    return 0
  fi
  p1b_1_completion_status="PASS"
  p1b_authorization_runtime_status="EFFECTIVE_MERGED_MAIN"

  if [[ "$current_status" != "COMPLETED" ]]; then
    next_task_authorization_status="BLOCKED_HOME_CORE_DATA_AUTHORIZATION_INCOMPLETE"
    return 0
  fi
  if [[ "$declared_authorization_status" != "AUTHORIZED_PENDING_MERGED_MAIN" ]]; then
    next_task_authorization_status="BLOCKED_HOME_CORE_DATA_SCOPE_NOT_AUTHORIZED"
    return 0
  fi
  if ! is_true_flag "$repository_edits_allowed" \
    || ! is_true_flag "$implementation_allowed" \
    || ! is_true_flag "$implementation_pr_allowed"; then
    next_task_authorization_status="BLOCKED_HOME_CORE_DATA_IMPLEMENTATION_PERMISSIONS_INCOMPLETE"
    return 0
  fi
  if [[ "$completion_state" != "EFFECTIVE_MERGED_MAIN" ]]; then
    next_task_authorization_status="BLOCKED_PENDING_AUTHORIZATION_MERGED_MAIN"
    home_core_data_authorization_runtime_status="PENDING_MERGED_MAIN"
    return 0
  fi
  if [[ "$synced_main_status" != "YES" ]]; then
    next_task_authorization_status="BLOCKED_PENDING_LOCAL_ORIGIN_MAIN_MATCH"
    return 0
  fi
  if [[ "$source_gate_status" != "PASS" ]]; then
    next_task_authorization_status="BLOCKED_PRODUCT_SOURCE_GATE"
    return 0
  fi
  if [[ "$merged_main_validation_status" != "PASS" ]]; then
    next_task_authorization_status="BLOCKED_HOME_CORE_DATA_AUTHORIZATION_MERGED_MAIN_VALIDATION"
    return 0
  fi

  effective_task_mode="$next_mode"
  next_transition_allowed="YES"
  authorization_status="APPROVED"
  next_task_authorization_status="ALLOWED"
  home_core_data_authorization_runtime_status="EFFECTIVE_MERGED_MAIN"
  p1b_authorization_runtime_status="EFFECTIVE_MERGED_MAIN"
}

evaluate_p2_position_monitoring_transition() {
  local current_phase="$1" current_status="$2" current_mode="$3" next_phase="$4" next_mode="$5"
  local completion_state="$6" synced_main_status="$7" source_gate_status="$8"
  local merged_main_validation_status="$9" repository_edits_allowed="${10}"
  local implementation_allowed="${11}" implementation_pr_allowed="${12}"
  local declared_authorization_status="${13}" predecessor_status="${14}"

  effective_task_mode="$current_mode"
  p1a_transition_allowed="YES"
  p1a_completion_status="PASS"
  next_transition_allowed="NO"
  authorization_status="BLOCKED"
  next_task_authorization_status="BLOCKED_INVALID_TRANSITION_CONTRACT"
  p2_authorization_runtime_status="BLOCKED"

  [[ "$current_phase" == "P2_POSITION_MONITORING_AUTHORIZATION" ]] || return 0
  [[ "$current_mode" == "BOUNDED_PRODUCT_DECISION_AND_AUTHORIZATION" ]] || return 0
  [[ "$next_phase" == "P2_POSITION_MONITORING_BACKEND_IMPLEMENTATION" ]] || return 0
  [[ "$next_mode" == "IMPLEMENTATION" ]] || return 0

  if [[ "$predecessor_status" != "COMPLETE" ]]; then
    next_task_authorization_status="BLOCKED_PRODUCT_P1B_NOT_COMPLETE"
    return 0
  fi
  if [[ "$current_status" != "COMPLETED" ]]; then
    next_task_authorization_status="BLOCKED_P2_AUTHORIZATION_INCOMPLETE"
    return 0
  fi
  if [[ "$declared_authorization_status" != "AUTHORIZED_PENDING_MERGED_MAIN" ]]; then
    next_task_authorization_status="BLOCKED_P2_SCOPE_NOT_AUTHORIZED"
    return 0
  fi
  if ! is_true_flag "$repository_edits_allowed" \
    || ! is_true_flag "$implementation_allowed" \
    || ! is_true_flag "$implementation_pr_allowed"; then
    next_task_authorization_status="BLOCKED_P2_IMPLEMENTATION_PERMISSIONS_INCOMPLETE"
    return 0
  fi
  if [[ "$completion_state" != "EFFECTIVE_MERGED_MAIN" ]]; then
    next_task_authorization_status="BLOCKED_PENDING_P2_AUTHORIZATION_MERGED_MAIN"
    p2_authorization_runtime_status="PENDING_MERGED_MAIN"
    return 0
  fi
  if [[ "$synced_main_status" != "YES" ]]; then
    next_task_authorization_status="BLOCKED_PENDING_LOCAL_ORIGIN_MAIN_MATCH"
    return 0
  fi
  if [[ "$source_gate_status" != "PASS" ]]; then
    next_task_authorization_status="BLOCKED_PRODUCT_SOURCE_GATE"
    return 0
  fi
  if [[ "$merged_main_validation_status" != "PASS" ]]; then
    next_task_authorization_status="BLOCKED_P2_AUTHORIZATION_MERGED_MAIN_VALIDATION"
    return 0
  fi

  effective_task_mode="$next_mode"
  next_transition_allowed="YES"
  authorization_status="APPROVED"
  next_task_authorization_status="ALLOWED"
  p2_authorization_runtime_status="EFFECTIVE_MERGED_MAIN"
}

evaluate_v4_1_target_runtime_transition() {
  local current_phase="$1" current_status="$2" current_mode="$3" next_phase="$4" next_mode="$5"
  local completion_state="$6" synced_main_status="$7" source_gate_status="$8"
  local merged_main_validation_status="$9" repository_edits_allowed="${10}"
  local implementation_allowed="${11}" implementation_pr_allowed="${12}"
  local declared_authorization_status="${13}" predecessor_status="${14}"

  effective_task_mode="$current_mode"
  p1a_transition_allowed="YES"
  p1a_completion_status="PASS"
  next_transition_allowed="NO"
  authorization_status="BLOCKED"
  next_task_authorization_status="BLOCKED_INVALID_TRANSITION_CONTRACT"
  v4_1_target_runtime_authorization_runtime_status="BLOCKED"

  [[ "$current_phase" == "FUNDAMENTAL_AI_V4_1_TARGET_RUNTIME_BLOCKER_REMEDIATION_AUTHORIZATION" ]] || return 0
  [[ "$current_mode" == "BOUNDED_PRODUCT_DECISION_AND_AUTHORIZATION" ]] || return 0
  [[ "$next_phase" == "FUNDAMENTAL_AI_V4_1_TARGET_RUNTIME_BLOCKER_REMEDIATION" ]] || return 0
  [[ "$next_mode" == "IMPLEMENTATION" ]] || return 0

  if [[ "$predecessor_status" != "COMPLETE" ]]; then
    next_task_authorization_status="BLOCKED_V4_1_FINAL_INTERACTION_NOT_COMPLETE"
    return 0
  fi
  if [[ "$current_status" != "COMPLETED" ]]; then
    next_task_authorization_status="BLOCKED_V4_1_TARGET_RUNTIME_AUTHORIZATION_INCOMPLETE"
    return 0
  fi
  if [[ "$declared_authorization_status" != "AUTHORIZED_PENDING_MERGED_MAIN" ]]; then
    next_task_authorization_status="BLOCKED_V4_1_TARGET_RUNTIME_SCOPE_NOT_AUTHORIZED"
    return 0
  fi
  if ! is_true_flag "$repository_edits_allowed" \
    || ! is_true_flag "$implementation_allowed" \
    || ! is_true_flag "$implementation_pr_allowed" \
    || ! is_false_flag "$authorized_next_canonical_figma_desktop_implementation_allowed" \
    || ! is_false_flag "$authorized_next_mobile_implementation_allowed" \
    || [[ "$authorized_next_canonical_figma_file_key" != "NONE" ]]; then
    next_task_authorization_status="BLOCKED_V4_1_TARGET_RUNTIME_PERMISSIONS_INCOMPLETE"
    return 0
  fi
  if [[ "$completion_state" != "EFFECTIVE_MERGED_MAIN" ]]; then
    next_task_authorization_status="BLOCKED_PENDING_V4_1_TARGET_RUNTIME_AUTHORIZATION_MERGED_MAIN"
    v4_1_target_runtime_authorization_runtime_status="PENDING_MERGED_MAIN"
    return 0
  fi
  if [[ "$synced_main_status" != "YES" ]]; then
    next_task_authorization_status="BLOCKED_PENDING_LOCAL_ORIGIN_MAIN_MATCH"
    return 0
  fi
  if [[ "$source_gate_status" != "PASS" ]]; then
    next_task_authorization_status="BLOCKED_PRODUCT_SOURCE_GATE"
    return 0
  fi
  if [[ "$merged_main_validation_status" != "PASS" ]]; then
    next_task_authorization_status="BLOCKED_V4_1_TARGET_RUNTIME_AUTHORIZATION_MERGED_MAIN_VALIDATION"
    return 0
  fi

  effective_task_mode="$next_mode"
  next_transition_allowed="YES"
  authorization_status="APPROVED"
  next_task_authorization_status="ALLOWED"
  v4_1_target_runtime_authorization_runtime_status="EFFECTIVE_MERGED_MAIN"
}

evaluate_v4_1_telegram_transition() {
  local current_phase="$1" current_status="$2" current_mode="$3" next_phase="$4" next_mode="$5"
  local completion_state="$6" synced_main_status="$7" source_gate_status="$8"
  local merged_main_validation_status="$9" repository_edits_allowed="${10}"
  local implementation_allowed="${11}" implementation_pr_allowed="${12}"
  local declared_authorization_status="${13}" predecessor_status="${14}"

  effective_task_mode="$current_mode"
  p1a_transition_allowed="YES"
  p1a_completion_status="PASS"
  next_transition_allowed="NO"
  authorization_status="BLOCKED"
  next_task_authorization_status="BLOCKED_INVALID_TRANSITION_CONTRACT"
  v4_1_target_runtime_authorization_runtime_status="EFFECTIVE_MERGED_MAIN"
  v4_1_telegram_authorization_runtime_status="BLOCKED"

  [[ "$current_phase" == "FUNDAMENTAL_AI_V4_1_TELEGRAM_HIGH_VALUE_ALERT_CHANNEL_AUTHORIZATION" ]] || return 0
  [[ "$current_mode" == "BOUNDED_PRODUCT_DECISION_AND_AUTHORIZATION" ]] || return 0
  [[ "$next_phase" == "FUNDAMENTAL_AI_V4_1_TELEGRAM_HIGH_VALUE_ALERT_CHANNEL_INTEGRATION" ]] || return 0
  [[ "$next_mode" == "IMPLEMENTATION" ]] || return 0

  if [[ "$predecessor_status" != "COMPLETE" ]]; then
    next_task_authorization_status="BLOCKED_V4_1_TARGET_RUNTIME_REMEDIATION_NOT_COMPLETE"
    return 0
  fi
  if [[ "$current_status" != "COMPLETED" ]]; then
    next_task_authorization_status="BLOCKED_V4_1_TELEGRAM_AUTHORIZATION_INCOMPLETE"
    return 0
  fi
  if [[ "$declared_authorization_status" != "AUTHORIZED_PENDING_MERGED_MAIN" ]]; then
    next_task_authorization_status="BLOCKED_V4_1_TELEGRAM_SCOPE_NOT_AUTHORIZED"
    return 0
  fi
  if ! is_true_flag "$repository_edits_allowed" \
    || ! is_true_flag "$implementation_allowed" \
    || ! is_true_flag "$implementation_pr_allowed" \
    || ! is_false_flag "$authorized_next_canonical_figma_desktop_implementation_allowed" \
    || ! is_false_flag "$authorized_next_mobile_implementation_allowed" \
    || [[ "$authorized_next_canonical_figma_file_key" != "NONE" ]]; then
    next_task_authorization_status="BLOCKED_V4_1_TELEGRAM_PERMISSIONS_INCOMPLETE"
    return 0
  fi
  if [[ "$completion_state" != "EFFECTIVE_MERGED_MAIN" ]]; then
    next_task_authorization_status="BLOCKED_PENDING_V4_1_TELEGRAM_AUTHORIZATION_MERGED_MAIN"
    v4_1_telegram_authorization_runtime_status="PENDING_MERGED_MAIN"
    return 0
  fi
  if [[ "$synced_main_status" != "YES" ]]; then
    next_task_authorization_status="BLOCKED_PENDING_LOCAL_ORIGIN_MAIN_MATCH"
    return 0
  fi
  if [[ "$source_gate_status" != "PASS" ]]; then
    next_task_authorization_status="BLOCKED_PRODUCT_SOURCE_GATE"
    return 0
  fi
  if [[ "$merged_main_validation_status" != "PASS" ]]; then
    next_task_authorization_status="BLOCKED_V4_1_TELEGRAM_AUTHORIZATION_MERGED_MAIN_VALIDATION"
    return 0
  fi

  effective_task_mode="$next_mode"
  next_transition_allowed="YES"
  authorization_status="APPROVED"
  next_task_authorization_status="ALLOWED"
  v4_1_telegram_authorization_runtime_status="EFFECTIVE_MERGED_MAIN"
}

evaluate_local_real_transition() {
  local current_phase="$1" current_status="$2" current_mode="$3" next_phase="$4" next_mode="$5"
  local completion_state="$6" synced_main_status="$7" source_gate_status="$8"
  local merged_main_validation_status="$9" repository_edits_allowed="${10}"
  local implementation_allowed="${11}" implementation_pr_allowed="${12}"
  local declared_authorization_status="${13}" predecessor_status="${14}"

  effective_task_mode="$current_mode"
  p1a_transition_allowed="YES"
  p1a_completion_status="PASS"
  next_transition_allowed="NO"
  authorization_status="BLOCKED"
  next_task_authorization_status="BLOCKED_INVALID_TRANSITION_CONTRACT"
  v4_1_target_runtime_authorization_runtime_status="EFFECTIVE_MERGED_MAIN"
  v4_1_telegram_authorization_runtime_status="EFFECTIVE_MERGED_MAIN"
  local_real_authorization_runtime_status="BLOCKED"

  [[ "$current_phase" == "LOCAL_REAL_READINESS_SYNC_AND_REAL_ANALYSIS_ENABLEMENT_AUTHORIZATION" ]] || return 0
  [[ "$current_mode" == "BOUNDED_PRODUCT_DECISION_AND_AUTHORIZATION" ]] || return 0
  [[ "$next_phase" == "LOCAL_REAL_READINESS_SYNC_AND_REAL_ANALYSIS_ENABLEMENT" ]] || return 0
  [[ "$next_mode" == "IMPLEMENTATION" ]] || return 0

  if [[ "$predecessor_status" != "COMPLETE" ]]; then
    next_task_authorization_status="BLOCKED_V4_1_TELEGRAM_IMPLEMENTATION_NOT_COMPLETE"
    return 0
  fi
  if [[ "$current_status" != "COMPLETED" ]]; then
    next_task_authorization_status="BLOCKED_LOCAL_REAL_AUTHORIZATION_INCOMPLETE"
    return 0
  fi
  if [[ "$declared_authorization_status" != "AUTHORIZED_PENDING_MERGED_MAIN" ]]; then
    next_task_authorization_status="BLOCKED_LOCAL_REAL_SCOPE_NOT_AUTHORIZED"
    return 0
  fi
  if ! is_true_flag "$repository_edits_allowed" \
    || ! is_true_flag "$implementation_allowed" \
    || ! is_true_flag "$implementation_pr_allowed" \
    || ! is_false_flag "$authorized_next_canonical_figma_desktop_implementation_allowed" \
    || ! is_false_flag "$authorized_next_mobile_implementation_allowed" \
    || [[ "$authorized_next_canonical_figma_file_key" != "NONE" ]]; then
    next_task_authorization_status="BLOCKED_LOCAL_REAL_PERMISSIONS_INCOMPLETE"
    return 0
  fi
  if [[ "$completion_state" != "EFFECTIVE_MERGED_MAIN" ]]; then
    next_task_authorization_status="BLOCKED_PENDING_LOCAL_REAL_AUTHORIZATION_MERGED_MAIN"
    local_real_authorization_runtime_status="PENDING_MERGED_MAIN"
    return 0
  fi
  if [[ "$synced_main_status" != "YES" ]]; then
    next_task_authorization_status="BLOCKED_PENDING_LOCAL_ORIGIN_MAIN_MATCH"
    return 0
  fi
  if [[ "$source_gate_status" != "PASS" ]]; then
    next_task_authorization_status="BLOCKED_PRODUCT_SOURCE_GATE"
    return 0
  fi
  if [[ "$merged_main_validation_status" != "PASS" ]]; then
    next_task_authorization_status="BLOCKED_LOCAL_REAL_AUTHORIZATION_MERGED_MAIN_VALIDATION"
    return 0
  fi

  effective_task_mode="$next_mode"
  next_transition_allowed="YES"
  authorization_status="APPROVED"
  next_task_authorization_status="ALLOWED"
  local_real_authorization_runtime_status="EFFECTIVE_MERGED_MAIN"
}

evaluate_frontend_interaction_transition() {
  local current_phase="$1" current_status="$2" current_mode="$3" next_phase="$4" next_mode="$5"
  local completion_state="$6" synced_main_status="$7" source_gate_status="$8"
  local merged_main_validation_status="$9" repository_edits_allowed="${10}"
  local implementation_allowed="${11}" implementation_pr_allowed="${12}"
  local declared_authorization_status="${13}" predecessor_status="${14}"

  effective_task_mode="$current_mode"
  p1a_transition_allowed="YES"
  p1a_completion_status="PASS"
  next_transition_allowed="NO"
  authorization_status="BLOCKED"
  next_task_authorization_status="BLOCKED_INVALID_TRANSITION_CONTRACT"
  v4_1_target_runtime_authorization_runtime_status="EFFECTIVE_MERGED_MAIN"
  v4_1_telegram_authorization_runtime_status="EFFECTIVE_MERGED_MAIN"
  local_real_authorization_runtime_status="EFFECTIVE_MERGED_MAIN"
  frontend_interaction_authorization_runtime_status="BLOCKED"

  [[ "$current_phase" == "FRONTEND_INTERACTION_RUNTIME_CLOSURE_AUTHORIZATION" ]] || return 0
  [[ "$current_mode" == "BOUNDED_PRODUCT_DECISION_AND_AUTHORIZATION" ]] || return 0
  [[ "$next_phase" == "FRONTEND_INTERACTION_RUNTIME_CLOSURE" ]] || return 0
  [[ "$next_mode" == "IMPLEMENTATION" ]] || return 0

  if [[ "$predecessor_status" != "COMPLETE" ]]; then
    next_task_authorization_status="BLOCKED_LOCAL_REAL_IMPLEMENTATION_NOT_COMPLETE"
    return 0
  fi
  if [[ "$current_status" != "COMPLETED" ]]; then
    next_task_authorization_status="BLOCKED_FRONTEND_INTERACTION_AUTHORIZATION_INCOMPLETE"
    return 0
  fi
  if [[ "$declared_authorization_status" != "AUTHORIZED_PENDING_MERGED_MAIN" ]]; then
    next_task_authorization_status="BLOCKED_FRONTEND_INTERACTION_SCOPE_NOT_AUTHORIZED"
    return 0
  fi
  if ! is_true_flag "$repository_edits_allowed" \
    || ! is_true_flag "$implementation_allowed" \
    || ! is_true_flag "$implementation_pr_allowed" \
    || ! is_false_flag "$authorized_next_canonical_figma_desktop_implementation_allowed" \
    || ! is_false_flag "$authorized_next_mobile_implementation_allowed" \
    || [[ "$authorized_next_canonical_figma_file_key" != "NONE" ]]; then
    next_task_authorization_status="BLOCKED_FRONTEND_INTERACTION_PERMISSIONS_INCOMPLETE"
    return 0
  fi
  if [[ "$completion_state" != "EFFECTIVE_MERGED_MAIN" ]]; then
    next_task_authorization_status="BLOCKED_PENDING_FRONTEND_INTERACTION_AUTHORIZATION_MERGED_MAIN"
    frontend_interaction_authorization_runtime_status="PENDING_MERGED_MAIN"
    return 0
  fi
  if [[ "$synced_main_status" != "YES" ]]; then
    next_task_authorization_status="BLOCKED_PENDING_LOCAL_ORIGIN_MAIN_MATCH"
    return 0
  fi
  if [[ "$source_gate_status" != "PASS" ]]; then
    next_task_authorization_status="BLOCKED_PRODUCT_SOURCE_GATE"
    return 0
  fi
  if [[ "$merged_main_validation_status" != "PASS" ]]; then
    next_task_authorization_status="BLOCKED_FRONTEND_INTERACTION_AUTHORIZATION_MERGED_MAIN_VALIDATION"
    return 0
  fi

  effective_task_mode="$next_mode"
  next_transition_allowed="YES"
  authorization_status="APPROVED"
  next_task_authorization_status="ALLOWED"
  frontend_interaction_authorization_runtime_status="EFFECTIVE_MERGED_MAIN"
}

evaluate_multi_user_transition() {
  local current_phase="$1" current_status="$2" current_mode="$3" next_phase="$4" next_mode="$5"
  local completion_state="$6" synced_main_status="$7" source_gate_status="$8"
  local merged_main_validation_status="$9" repository_edits_allowed="${10}"
  local implementation_allowed="${11}" implementation_pr_allowed="${12}"
  local declared_authorization_status="${13}" predecessor_status="${14}"

  effective_task_mode="$current_mode"
  p1a_transition_allowed="YES"
  p1a_completion_status="PASS"
  next_transition_allowed="NO"
  authorization_status="BLOCKED"
  next_task_authorization_status="BLOCKED_INVALID_TRANSITION_CONTRACT"
  v4_1_target_runtime_authorization_runtime_status="EFFECTIVE_MERGED_MAIN"
  v4_1_telegram_authorization_runtime_status="EFFECTIVE_MERGED_MAIN"
  local_real_authorization_runtime_status="EFFECTIVE_MERGED_MAIN"
  frontend_interaction_authorization_runtime_status="EFFECTIVE_MERGED_MAIN"
  multi_user_authorization_runtime_status="BLOCKED"

  [[ "$current_phase" == "MULTI_USER_ACCOUNT_REGISTRATION_CLOSURE_AUTHORIZATION" ]] || return 0
  [[ "$current_mode" == "BOUNDED_PRODUCT_DECISION_AND_AUTHORIZATION" ]] || return 0
  [[ "$next_phase" == "MULTI_USER_ACCOUNT_REGISTRATION_CLOSURE" ]] || return 0
  [[ "$next_mode" == "IMPLEMENTATION" ]] || return 0

  if [[ "$predecessor_status" != "COMPLETE" ]]; then
    next_task_authorization_status="BLOCKED_FRONTEND_INTERACTION_IMPLEMENTATION_NOT_COMPLETE"
    return 0
  fi
  if [[ "$current_status" != "COMPLETED" ]]; then
    next_task_authorization_status="BLOCKED_MULTI_USER_AUTHORIZATION_INCOMPLETE"
    return 0
  fi
  if [[ "$declared_authorization_status" != "AUTHORIZED_PENDING_MERGED_MAIN" ]]; then
    next_task_authorization_status="BLOCKED_MULTI_USER_SCOPE_NOT_AUTHORIZED"
    return 0
  fi
  if ! is_true_flag "$repository_edits_allowed" \
    || ! is_true_flag "$implementation_allowed" \
    || ! is_true_flag "$implementation_pr_allowed" \
    || ! is_false_flag "$authorized_next_canonical_figma_desktop_implementation_allowed" \
    || ! is_false_flag "$authorized_next_mobile_implementation_allowed" \
    || [[ "$authorized_next_canonical_figma_file_key" != "NONE" ]]; then
    next_task_authorization_status="BLOCKED_MULTI_USER_PERMISSIONS_INCOMPLETE"
    return 0
  fi
  if [[ "$completion_state" != "EFFECTIVE_MERGED_MAIN" ]]; then
    next_task_authorization_status="BLOCKED_PENDING_MULTI_USER_AUTHORIZATION_MERGED_MAIN"
    multi_user_authorization_runtime_status="PENDING_MERGED_MAIN"
    return 0
  fi
  if [[ "$synced_main_status" != "YES" ]]; then
    next_task_authorization_status="BLOCKED_PENDING_LOCAL_ORIGIN_MAIN_MATCH"
    return 0
  fi
  if [[ "$source_gate_status" != "PASS" ]]; then
    next_task_authorization_status="BLOCKED_PRODUCT_SOURCE_GATE"
    return 0
  fi
  if [[ "$merged_main_validation_status" != "PASS" ]]; then
    next_task_authorization_status="BLOCKED_MULTI_USER_AUTHORIZATION_MERGED_MAIN_VALIDATION"
    return 0
  fi

  effective_task_mode="$next_mode"
  next_transition_allowed="YES"
  authorization_status="AUTHORIZED"
  next_task_authorization_status="ALLOWED"
  multi_user_authorization_runtime_status="EFFECTIVE_MERGED_MAIN"
}

evaluate_v4_1_telegram_remediation_transition() {
  local current_phase="$1" current_status="$2" current_mode="$3" next_phase="$4" next_mode="$5"
  local completion_state="$6" synced_main_status="$7" source_gate_status="$8"
  local merged_main_validation_status="$9" repository_edits_allowed="${10}"
  local implementation_allowed="${11}" implementation_pr_allowed="${12}"
  local declared_authorization_status="${13}" predecessor_status="${14}"

  effective_task_mode="$current_mode"
  p1a_transition_allowed="YES"
  p1a_completion_status="PASS"
  next_transition_allowed="NO"
  authorization_status="BLOCKED"
  next_task_authorization_status="BLOCKED_INVALID_TRANSITION_CONTRACT"
  v4_1_target_runtime_authorization_runtime_status="EFFECTIVE_MERGED_MAIN"
  v4_1_telegram_authorization_runtime_status="EFFECTIVE_MERGED_MAIN"
  v4_1_telegram_remediation_authorization_runtime_status="BLOCKED"
  local_real_authorization_runtime_status="EFFECTIVE_MERGED_MAIN"
  frontend_interaction_authorization_runtime_status="EFFECTIVE_MERGED_MAIN"
  multi_user_authorization_runtime_status="EFFECTIVE_MERGED_MAIN"

  [[ "$current_phase" == "FUNDAMENTAL_AI_V4_1_TELEGRAM_HIGH_VALUE_ALERT_CHANNEL_REMEDIATION_AUTHORIZATION" ]] || return 0
  [[ "$current_mode" == "BOUNDED_PRODUCT_DECISION_AND_AUTHORIZATION" ]] || return 0
  [[ "$next_phase" == "FUNDAMENTAL_AI_V4_1_TELEGRAM_HIGH_VALUE_ALERT_CHANNEL_INTEGRATION" ]] || return 0
  [[ "$next_mode" == "IMPLEMENTATION" ]] || return 0

  if [[ "$predecessor_status" != "COMPLETE" ]]; then
    next_task_authorization_status="BLOCKED_V4_1_TELEGRAM_HISTORICAL_IMPLEMENTATION_NOT_COMPLETE"
    return 0
  fi
  if [[ "$current_status" != "COMPLETED" ]]; then
    next_task_authorization_status="BLOCKED_V4_1_TELEGRAM_REMEDIATION_AUTHORIZATION_INCOMPLETE"
    return 0
  fi
  if [[ "$declared_authorization_status" != "AUTHORIZED_PENDING_MERGED_MAIN" ]]; then
    next_task_authorization_status="BLOCKED_V4_1_TELEGRAM_REMEDIATION_SCOPE_NOT_AUTHORIZED"
    return 0
  fi
  if ! is_true_flag "$repository_edits_allowed" \
    || ! is_true_flag "$implementation_allowed" \
    || ! is_true_flag "$implementation_pr_allowed" \
    || ! is_false_flag "$authorized_next_canonical_figma_desktop_implementation_allowed" \
    || ! is_false_flag "$authorized_next_mobile_implementation_allowed" \
    || [[ "$authorized_next_canonical_figma_file_key" != "NONE" ]]; then
    next_task_authorization_status="BLOCKED_V4_1_TELEGRAM_REMEDIATION_PERMISSIONS_INCOMPLETE"
    return 0
  fi
  if [[ "$completion_state" != "EFFECTIVE_MERGED_MAIN" ]]; then
    next_task_authorization_status="BLOCKED_PENDING_V4_1_TELEGRAM_REMEDIATION_AUTHORIZATION_MERGED_MAIN"
    v4_1_telegram_remediation_authorization_runtime_status="PENDING_MERGED_MAIN"
    return 0
  fi
  if [[ "$synced_main_status" != "YES" ]]; then
    next_task_authorization_status="BLOCKED_PENDING_LOCAL_ORIGIN_MAIN_MATCH"
    return 0
  fi
  if [[ "$source_gate_status" != "PASS" ]]; then
    next_task_authorization_status="BLOCKED_PRODUCT_SOURCE_GATE"
    return 0
  fi
  if [[ "$merged_main_validation_status" != "PASS" ]]; then
    next_task_authorization_status="BLOCKED_V4_1_TELEGRAM_REMEDIATION_AUTHORIZATION_MERGED_MAIN_VALIDATION"
    return 0
  fi

  effective_task_mode="$next_mode"
  next_transition_allowed="YES"
  authorization_status="AUTHORIZED"
  next_task_authorization_status="ALLOWED"
  v4_1_telegram_remediation_authorization_runtime_status="EFFECTIVE_MERGED_MAIN"
}

evaluate_v4_1_core_production_loop_transition() {
  local current_phase="$1" current_status="$2" current_mode="$3" next_phase="$4" next_mode="$5"
  local completion_state="$6" synced_main_status="$7" source_gate_status="$8"
  local merged_main_validation_status="$9" repository_edits_allowed="${10}"
  local implementation_allowed="${11}" implementation_pr_allowed="${12}"
  local declared_authorization_status="${13}" predecessor_status="${14}"

  effective_task_mode="$current_mode"
  p1a_transition_allowed="YES"
  p1a_completion_status="PASS"
  next_transition_allowed="NO"
  authorization_status="BLOCKED"
  next_task_authorization_status="BLOCKED_INVALID_TRANSITION_CONTRACT"
  v4_1_target_runtime_authorization_runtime_status="EFFECTIVE_MERGED_MAIN"
  v4_1_telegram_authorization_runtime_status="EFFECTIVE_MERGED_MAIN"
  v4_1_telegram_remediation_authorization_runtime_status="EFFECTIVE_MERGED_MAIN"
  v4_1_core_production_loop_authorization_runtime_status="BLOCKED"
  local_real_authorization_runtime_status="EFFECTIVE_MERGED_MAIN"
  frontend_interaction_authorization_runtime_status="EFFECTIVE_MERGED_MAIN"
  multi_user_authorization_runtime_status="EFFECTIVE_MERGED_MAIN"

  [[ "$current_phase" == "FUNDAMENTAL_AI_V4_1_CORE_PRODUCTION_LOOP_AUTOMATION_AUTHORIZATION" ]] || return 0
  [[ "$current_mode" == "BOUNDED_PRODUCT_DECISION_AND_AUTHORIZATION" ]] || return 0
  [[ "$next_phase" == "FUNDAMENTAL_AI_V4_1_CORE_PRODUCTION_LOOP_AUTOMATION" ]] || return 0
  [[ "$next_mode" == "IMPLEMENTATION" ]] || return 0

  if [[ "$predecessor_status" != "COMPLETE" ]]; then
    next_task_authorization_status="BLOCKED_V4_1_DECISION_CHAIN_IMPLEMENTATION_NOT_COMPLETE"
    return 0
  fi
  if [[ "$current_status" != "COMPLETED" ]]; then
    next_task_authorization_status="BLOCKED_V4_1_CORE_PRODUCTION_LOOP_AUTHORIZATION_INCOMPLETE"
    return 0
  fi
  if [[ "$declared_authorization_status" != "AUTHORIZED_PENDING_MERGED_MAIN" ]]; then
    next_task_authorization_status="BLOCKED_V4_1_CORE_PRODUCTION_LOOP_SCOPE_NOT_AUTHORIZED"
    return 0
  fi
  if ! is_true_flag "$repository_edits_allowed" \
    || ! is_true_flag "$implementation_allowed" \
    || ! is_true_flag "$implementation_pr_allowed" \
    || ! is_false_flag "$authorized_next_canonical_figma_desktop_implementation_allowed" \
    || ! is_false_flag "$authorized_next_mobile_implementation_allowed" \
    || [[ "$authorized_next_canonical_figma_file_key" != "NONE" ]]; then
    next_task_authorization_status="BLOCKED_V4_1_CORE_PRODUCTION_LOOP_PERMISSIONS_INCOMPLETE"
    return 0
  fi
  if [[ "$completion_state" != "EFFECTIVE_MERGED_MAIN" ]]; then
    next_task_authorization_status="BLOCKED_PENDING_V4_1_CORE_PRODUCTION_LOOP_AUTHORIZATION_MERGED_MAIN"
    v4_1_core_production_loop_authorization_runtime_status="PENDING_MERGED_MAIN"
    return 0
  fi
  if [[ "$synced_main_status" != "YES" ]]; then
    next_task_authorization_status="BLOCKED_PENDING_LOCAL_ORIGIN_MAIN_MATCH"
    return 0
  fi
  if [[ "$source_gate_status" != "PASS" ]]; then
    next_task_authorization_status="BLOCKED_PRODUCT_SOURCE_GATE"
    return 0
  fi
  if [[ "$merged_main_validation_status" != "PASS" ]]; then
    next_task_authorization_status="BLOCKED_V4_1_CORE_PRODUCTION_LOOP_AUTHORIZATION_MERGED_MAIN_VALIDATION"
    return 0
  fi

  effective_task_mode="$next_mode"
  next_transition_allowed="YES"
  authorization_status="AUTHORIZED"
  next_task_authorization_status="ALLOWED"
  v4_1_core_production_loop_authorization_runtime_status="EFFECTIVE_MERGED_MAIN"
}

evaluate_baseline_reconciliation_transition() {
  effective_task_mode="$current_package_mode"
  p1a_transition_allowed="YES"
  p1a_completion_status="PASS"
  next_transition_allowed="NO"
  authorization_status="BLOCKED"
  next_task_authorization_status="BLOCKED_INVALID_TRANSITION_CONTRACT"
  v4_1_target_runtime_authorization_runtime_status="EFFECTIVE_MERGED_MAIN"
  v4_1_telegram_authorization_runtime_status="EFFECTIVE_MERGED_MAIN"
  v4_1_telegram_remediation_authorization_runtime_status="EFFECTIVE_MERGED_MAIN"
  v4_1_core_production_loop_authorization_runtime_status="EFFECTIVE_MERGED_MAIN"
  v4_1_machine_gate_owner_amendment_runtime_status="EFFECTIVE_MERGED_MAIN"
  v4_1_baseline_reconciliation_gate_runtime_status="BLOCKED"
  real_data_home_blocker_closure_authorization_runtime_status="BLOCKED"
  local_real_authorization_runtime_status="EFFECTIVE_MERGED_MAIN"
  frontend_interaction_authorization_runtime_status="EFFECTIVE_MERGED_MAIN"
  multi_user_authorization_runtime_status="EFFECTIVE_MERGED_MAIN"

  [[ "$current_package_phase" == "TRINE_LOGIC_V4_1_BASELINE_RECONCILIATION_GATE" ]] || return 0
  [[ "$current_package_mode" == "DOCS_GATE_BASELINE_RECONCILIATION" ]] || return 0
  [[ "$authorized_next_package_phase" == "REAL_DATA_HOME_BLOCKER_CLOSURE" ]] || return 0
  [[ "$authorized_next_package_mode" == "IMPLEMENTATION" ]] || return 0

  if [[ "$current_package_status" != "COMPLETED" ]]; then
    next_task_authorization_status="BLOCKED_BASELINE_RECONCILIATION_INCOMPLETE"
    return 0
  fi
  if [[ "$v4_1_machine_gate_owner_amendment_declared_status" != "EFFECTIVE_MERGED_MAIN" \
    || "$v4_1_baseline_reconciliation_gate_declared_status" != "AUTHORIZED_PENDING_MERGED_MAIN" ]]; then
    next_task_authorization_status="BLOCKED_BASELINE_RECONCILIATION_NOT_AUTHORIZED"
    return 0
  fi
  if [[ "$real_data_home_blocker_closure_authorization_declared_status" != "PENDING_BASELINE_RECONCILIATION_MERGED_MAIN" ]]; then
    next_task_authorization_status="BLOCKED_REAL_DATA_HOME_SCOPE_NOT_AUTHORIZED"
    return 0
  fi
  if ! is_full_git_sha "$current_package_starting_full_sha" \
    || ! is_full_git_sha "$authorized_next_package_starting_full_sha" \
    || [[ "$authorized_next_normalization_source_parent_sha" != "$authorized_next_package_starting_full_sha" ]] \
    || [[ "$authorized_next_normalization_expected_source" != "MERGED_MAIN" ]] \
    || [[ "$authorized_next_normalization_extra_file_count" != "0" ]] \
    || [[ "$authorized_next_normalization_one_time_only" != "true" ]] \
    || [[ "$(printf '%s\n' "$authorized_next_normalization_allowed_files" | awk 'NF {count++} END {print count+0}')" != "4" ]]; then
    next_task_authorization_status="BLOCKED_INVALID_OR_MISSING_STARTING_FULL_SHA"
    return 0
  fi
  if ! is_true_flag "$current_package_repository_edits_allowed" \
    || ! is_false_flag "$current_package_implementation_allowed" \
    || ! is_true_flag "$current_package_implementation_pr_allowed" \
    || ! is_true_flag "$current_package_push_allowed" \
    || ! is_true_flag "$current_package_merge_allowed" \
    || ! is_false_flag "$current_package_deployment_allowed"; then
    next_task_authorization_status="BLOCKED_BASELINE_RECONCILIATION_PERMISSIONS_INVALID"
    return 0
  fi
  if ! is_true_flag "$authorized_next_repository_edits_allowed" \
    || ! is_true_flag "$authorized_next_implementation_allowed" \
    || ! is_true_flag "$authorized_next_implementation_pr_allowed" \
    || ! is_true_flag "$authorized_next_push_allowed" \
    || ! is_true_flag "$authorized_next_merge_allowed" \
    || ! is_false_flag "$authorized_next_deployment_allowed" \
    || ! is_false_flag "$authorized_next_canonical_figma_desktop_implementation_allowed" \
    || ! is_false_flag "$authorized_next_mobile_implementation_allowed" \
    || [[ "$authorized_next_canonical_figma_file_key" != "NONE" ]]; then
    next_task_authorization_status="BLOCKED_REAL_DATA_HOME_PERMISSIONS_INCOMPLETE"
    return 0
  fi
  if [[ "${machine_gate_effective_on_origin_main:-NO}" != "YES" ]]; then
    next_task_authorization_status="BLOCKED_PENDING_BASELINE_RECONCILIATION_MERGED_MAIN"
    v4_1_baseline_reconciliation_gate_runtime_status="PENDING_MERGED_MAIN"
    real_data_home_blocker_closure_authorization_runtime_status="PENDING_BASELINE_RECONCILIATION_MERGED_MAIN"
    return 0
  fi
  if [[ "${machine_identity_allowed:-NO}" != "YES" ]]; then
    next_task_authorization_status="${machine_identity_block_reason:-BLOCKED_EXACT_MACHINE_IDENTITY}"
    return 0
  fi
  if [[ "$product_source_gate_status" != "PASS" ]]; then
    next_task_authorization_status="BLOCKED_PRODUCT_SOURCE_GATE"
    return 0
  fi

  effective_task_mode="$authorized_next_package_mode"
  next_transition_allowed="YES"
  authorization_status="AUTHORIZED"
  next_task_authorization_status="ALLOWED"
  v4_1_machine_gate_owner_amendment_runtime_status="EFFECTIVE_MERGED_MAIN"
  v4_1_baseline_reconciliation_gate_runtime_status="EFFECTIVE_MERGED_MAIN"
  real_data_home_blocker_closure_authorization_runtime_status="AUTHORIZED"
}

evaluate_final_runtime_home_closure_transition() {
  effective_task_mode="$current_package_mode"
  p1a_transition_allowed="YES"
  p1a_completion_status="PASS"
  next_transition_allowed="NO"
  authorization_status="BLOCKED"
  next_task_authorization_status="BLOCKED_INVALID_TRANSITION_CONTRACT"
  v4_1_target_runtime_authorization_runtime_status="EFFECTIVE_MERGED_MAIN"
  v4_1_telegram_authorization_runtime_status="EFFECTIVE_MERGED_MAIN"
  v4_1_telegram_remediation_authorization_runtime_status="EFFECTIVE_MERGED_MAIN"
  v4_1_core_production_loop_authorization_runtime_status="EFFECTIVE_MERGED_MAIN"
  v4_1_machine_gate_owner_amendment_runtime_status="EFFECTIVE_MERGED_MAIN"
  v4_1_baseline_reconciliation_gate_runtime_status="EFFECTIVE_MERGED_MAIN"
  real_data_home_blocker_closure_authorization_runtime_status="EFFECTIVE_MERGED_MAIN"
  analysis_run_idempotency_tx_fix_authorization_runtime_status="EFFECTIVE_MERGED_MAIN"
  final_runtime_home_closure_authorization_runtime_status="BLOCKED"
  local_real_authorization_runtime_status="EFFECTIVE_MERGED_MAIN"
  frontend_interaction_authorization_runtime_status="EFFECTIVE_MERGED_MAIN"
  multi_user_authorization_runtime_status="EFFECTIVE_MERGED_MAIN"

  [[ "$current_package_phase" == "TRINE_LOGIC_V4_1_FINAL_RUNTIME_HOME_ACCESS_IDEMPOTENCY_CLOSURE_AUTHORIZATION" ]] || return 0
  [[ "$current_package_mode" == "DOCS_GATE_BASELINE_RECONCILIATION" ]] || return 0
  [[ "$authorized_next_package_phase" == "V41_FINAL_RUNTIME_HOME_ACCESS_IDEMPOTENCY_CLOSURE" ]] || return 0
  [[ "$authorized_next_package_mode" == "IMPLEMENTATION" ]] || return 0

  if [[ "$current_package_status" != "COMPLETED" ]]; then
    next_task_authorization_status="BLOCKED_FINAL_RUNTIME_HOME_CLOSURE_AUTHORIZATION_INCOMPLETE"
    return 0
  fi
  if [[ "$final_runtime_home_closure_authorization_declared_status" != "AUTHORIZED_PENDING_MERGED_MAIN" ]]; then
    next_task_authorization_status="BLOCKED_FINAL_RUNTIME_HOME_CLOSURE_SCOPE_NOT_AUTHORIZED"
    return 0
  fi
  if ! is_full_git_sha "$current_package_starting_full_sha" \
    || ! is_full_git_sha "$authorized_next_package_starting_full_sha" \
    || [[ "$current_package_starting_full_sha" != "0e9bd779b10e9d3140b8ceaea0a5193a28d6264f" ]] \
    || [[ "$authorized_next_package_starting_full_sha" != "$current_package_starting_full_sha" ]] \
    || [[ "$(printf '%s\n' "$authorized_next_package_allowed_paths" | awk 'NF {count++} END {print count+0}')" != "10" ]]; then
    next_task_authorization_status="BLOCKED_INVALID_OR_MISSING_STARTING_FULL_SHA"
    return 0
  fi
  if ! is_true_flag "$current_package_repository_edits_allowed" \
    || ! is_false_flag "$current_package_implementation_allowed" \
    || ! is_true_flag "$current_package_implementation_pr_allowed" \
    || ! is_true_flag "$current_package_push_allowed" \
    || ! is_true_flag "$current_package_merge_allowed" \
    || ! is_false_flag "$current_package_deployment_allowed"; then
    next_task_authorization_status="BLOCKED_FINAL_RUNTIME_HOME_CLOSURE_GATE_PERMISSIONS_INVALID"
    return 0
  fi
  if ! is_true_flag "$authorized_next_repository_edits_allowed" \
    || ! is_true_flag "$authorized_next_implementation_allowed" \
    || ! is_true_flag "$authorized_next_implementation_pr_allowed" \
    || ! is_true_flag "$authorized_next_push_allowed" \
    || ! is_true_flag "$authorized_next_merge_allowed" \
    || ! is_false_flag "$authorized_next_deployment_allowed" \
    || ! is_false_flag "$authorized_next_canonical_figma_desktop_implementation_allowed" \
    || ! is_false_flag "$authorized_next_mobile_implementation_allowed" \
    || [[ "$authorized_next_canonical_figma_file_key" != "NONE" ]]; then
    next_task_authorization_status="BLOCKED_FINAL_RUNTIME_HOME_CLOSURE_PERMISSIONS_INCOMPLETE"
    return 0
  fi
  if [[ "${machine_gate_effective_on_origin_main:-NO}" != "YES" ]]; then
    next_task_authorization_status="BLOCKED_PENDING_FINAL_RUNTIME_HOME_CLOSURE_AUTHORIZATION_MERGED_MAIN"
    final_runtime_home_closure_authorization_runtime_status="PENDING_MERGED_MAIN"
    return 0
  fi
  if [[ "${machine_identity_allowed:-NO}" != "YES" ]]; then
    next_task_authorization_status="${machine_identity_block_reason:-BLOCKED_EXACT_MACHINE_IDENTITY}"
    return 0
  fi
  if [[ "$product_source_gate_status" != "PASS" ]]; then
    next_task_authorization_status="BLOCKED_PRODUCT_SOURCE_GATE"
    return 0
  fi

  effective_task_mode="$authorized_next_package_mode"
  next_transition_allowed="YES"
  authorization_status="AUTHORIZED"
  next_task_authorization_status="ALLOWED"
  final_runtime_home_closure_authorization_runtime_status="EFFECTIVE_MERGED_MAIN"
}

evaluate_runtime_transition() {
  if [[ "$current_package_phase" == "TRINE_LOGIC_V4_1_FINAL_RUNTIME_HOME_ACCESS_IDEMPOTENCY_CLOSURE_AUTHORIZATION" \
    && "$authorized_next_package_phase" == "V41_FINAL_RUNTIME_HOME_ACCESS_IDEMPOTENCY_CLOSURE" ]]; then
    evaluate_final_runtime_home_closure_transition
    return 0
  fi

  if [[ "$current_package_phase" == "TRINE_LOGIC_V4_1_BASELINE_RECONCILIATION_GATE" \
    && "$authorized_next_package_phase" == "REAL_DATA_HOME_BLOCKER_CLOSURE" ]]; then
    evaluate_baseline_reconciliation_transition
    return 0
  fi

  if [[ "$current_package_phase" == "FUNDAMENTAL_AI_V4_1_CORE_PRODUCTION_LOOP_AUTOMATION_AUTHORIZATION" \
    && "$authorized_next_package_phase" == "FUNDAMENTAL_AI_V4_1_CORE_PRODUCTION_LOOP_AUTOMATION" ]]; then
    evaluate_v4_1_core_production_loop_transition \
      "$current_package_phase" \
      "$current_package_status" \
      "$current_task_mode" \
      "$authorized_next_package_phase" \
      "$authorized_next_task_mode" \
      "$completion_effective_state" \
      "$clean_synced_main" \
      "$product_source_gate_status" \
      "$p0_merged_main_validation_status" \
      "$authorized_next_repository_edits_allowed" \
      "$authorized_next_implementation_allowed" \
      "$authorized_next_implementation_pr_allowed" \
      "$v4_1_core_production_loop_authorization_declared_status" \
      "$v4_1_implementation_status"
    return 0
  fi

  if [[ "$current_package_phase" == "FUNDAMENTAL_AI_V4_1_TELEGRAM_HIGH_VALUE_ALERT_CHANNEL_REMEDIATION_AUTHORIZATION" \
    && "$authorized_next_package_phase" == "FUNDAMENTAL_AI_V4_1_TELEGRAM_HIGH_VALUE_ALERT_CHANNEL_INTEGRATION" ]]; then
    evaluate_v4_1_telegram_remediation_transition \
      "$current_package_phase" \
      "$current_package_status" \
      "$current_task_mode" \
      "$authorized_next_package_phase" \
      "$authorized_next_task_mode" \
      "$completion_effective_state" \
      "$clean_synced_main" \
      "$product_source_gate_status" \
      "$p0_merged_main_validation_status" \
      "$authorized_next_repository_edits_allowed" \
      "$authorized_next_implementation_allowed" \
      "$authorized_next_implementation_pr_allowed" \
      "$v4_1_telegram_remediation_authorization_declared_status" \
      "$v4_1_telegram_implementation_status"
    return 0
  fi

  if [[ "$current_package_phase" == "MULTI_USER_ACCOUNT_REGISTRATION_CLOSURE_AUTHORIZATION" \
    && "$authorized_next_package_phase" == "MULTI_USER_ACCOUNT_REGISTRATION_CLOSURE" ]]; then
    evaluate_multi_user_transition \
      "$current_package_phase" \
      "$current_package_status" \
      "$current_task_mode" \
      "$authorized_next_package_phase" \
      "$authorized_next_task_mode" \
      "$completion_effective_state" \
      "$clean_synced_main" \
      "$product_source_gate_status" \
      "$p0_merged_main_validation_status" \
      "$authorized_next_repository_edits_allowed" \
      "$authorized_next_implementation_allowed" \
      "$authorized_next_implementation_pr_allowed" \
      "$multi_user_authorization_declared_status" \
      "$frontend_interaction_implementation_status"
    return 0
  fi

  if [[ "$current_package_phase" == "FRONTEND_INTERACTION_RUNTIME_CLOSURE_AUTHORIZATION" \
    && "$authorized_next_package_phase" == "FRONTEND_INTERACTION_RUNTIME_CLOSURE" ]]; then
    evaluate_frontend_interaction_transition \
      "$current_package_phase" \
      "$current_package_status" \
      "$current_task_mode" \
      "$authorized_next_package_phase" \
      "$authorized_next_task_mode" \
      "$completion_effective_state" \
      "$clean_synced_main" \
      "$product_source_gate_status" \
      "$p0_merged_main_validation_status" \
      "$authorized_next_repository_edits_allowed" \
      "$authorized_next_implementation_allowed" \
      "$authorized_next_implementation_pr_allowed" \
      "$frontend_interaction_authorization_declared_status" \
      "$local_real_implementation_status"
    return 0
  fi

  if [[ "$current_package_phase" == "LOCAL_REAL_READINESS_SYNC_AND_REAL_ANALYSIS_ENABLEMENT_AUTHORIZATION" \
    && "$authorized_next_package_phase" == "LOCAL_REAL_READINESS_SYNC_AND_REAL_ANALYSIS_ENABLEMENT" ]]; then
    evaluate_local_real_transition \
      "$current_package_phase" \
      "$current_package_status" \
      "$current_task_mode" \
      "$authorized_next_package_phase" \
      "$authorized_next_task_mode" \
      "$completion_effective_state" \
      "$clean_synced_main" \
      "$product_source_gate_status" \
      "$p0_merged_main_validation_status" \
      "$authorized_next_repository_edits_allowed" \
      "$authorized_next_implementation_allowed" \
      "$authorized_next_implementation_pr_allowed" \
      "$local_real_authorization_declared_status" \
      "$v4_1_telegram_implementation_status"
    return 0
  fi

  if [[ "$current_package_phase" == "FUNDAMENTAL_AI_V4_1_TELEGRAM_HIGH_VALUE_ALERT_CHANNEL_AUTHORIZATION" \
    && "$authorized_next_package_phase" == "FUNDAMENTAL_AI_V4_1_TELEGRAM_HIGH_VALUE_ALERT_CHANNEL_INTEGRATION" ]]; then
    evaluate_v4_1_telegram_transition \
      "$current_package_phase" \
      "$current_package_status" \
      "$current_task_mode" \
      "$authorized_next_package_phase" \
      "$authorized_next_task_mode" \
      "$completion_effective_state" \
      "$clean_synced_main" \
      "$product_source_gate_status" \
      "$p0_merged_main_validation_status" \
      "$authorized_next_repository_edits_allowed" \
      "$authorized_next_implementation_allowed" \
      "$authorized_next_implementation_pr_allowed" \
      "$v4_1_telegram_authorization_declared_status" \
      "$v4_1_target_runtime_implementation_status"
    return 0
  fi

  if [[ "$current_package_phase" == "FUNDAMENTAL_AI_V4_1_TARGET_RUNTIME_BLOCKER_REMEDIATION_AUTHORIZATION" \
    && "$authorized_next_package_phase" == "FUNDAMENTAL_AI_V4_1_TARGET_RUNTIME_BLOCKER_REMEDIATION" ]]; then
    evaluate_v4_1_target_runtime_transition \
      "$current_package_phase" \
      "$current_package_status" \
      "$current_task_mode" \
      "$authorized_next_package_phase" \
      "$authorized_next_task_mode" \
      "$completion_effective_state" \
      "$clean_synced_main" \
      "$product_source_gate_status" \
      "$p0_merged_main_validation_status" \
      "$authorized_next_repository_edits_allowed" \
      "$authorized_next_implementation_allowed" \
      "$authorized_next_implementation_pr_allowed" \
      "$v4_1_target_runtime_authorization_declared_status" \
      "$v4_1_implementation_status"
    return 0
  fi

  if [[ "$current_package_phase" == "P2_POSITION_MONITORING_AUTHORIZATION" \
    && "$authorized_next_package_phase" == "P2_POSITION_MONITORING_BACKEND_IMPLEMENTATION" ]]; then
    evaluate_p2_position_monitoring_transition \
      "$current_package_phase" \
      "$current_package_status" \
      "$current_task_mode" \
      "$authorized_next_package_phase" \
      "$authorized_next_task_mode" \
      "$completion_effective_state" \
      "$clean_synced_main" \
      "$product_source_gate_status" \
      "$p0_merged_main_validation_status" \
      "$authorized_next_repository_edits_allowed" \
      "$authorized_next_implementation_allowed" \
      "$authorized_next_implementation_pr_allowed" \
      "$p2_authorization_declared_status" \
      "$product_p1b_declared_status"
    return 0
  fi

  if [[ "$current_package_phase" == "P1B_HOME_CORE_DATA_AUTHORIZATION" \
    && "$authorized_next_package_phase" == "P1B_HOME_CORE_DATA_COMPLETION" ]]; then
    evaluate_home_core_data_transition \
      "$current_package_phase" \
      "$current_package_status" \
      "$current_task_mode" \
      "$authorized_next_package_phase" \
      "$authorized_next_task_mode" \
      "$completion_effective_state" \
      "$clean_synced_main" \
      "$product_source_gate_status" \
      "$p0_merged_main_validation_status" \
      "$authorized_next_repository_edits_allowed" \
      "$authorized_next_implementation_allowed" \
      "$authorized_next_implementation_pr_allowed" \
      "$home_core_data_declared_status" \
      "$p1b_1_declared_status"
    return 0
  fi

  if [[ "$current_package_phase" == "P1A_HOME_ALIGNMENT_READINESS_AND_GAP_AUDIT" \
    && "$authorized_next_package_phase" == "P1B_HOME_ALIGNMENT_FIRST_IMPLEMENTATION" ]]; then
    evaluate_p1a_to_p1b_transition \
      "$current_package_phase" \
      "$current_package_status" \
      "$current_task_mode" \
      "$authorized_next_package_phase" \
      "$authorized_next_task_mode" \
      "$completion_effective_state" \
      "$clean_synced_main" \
      "$product_source_gate_status" \
      "$p0_merged_main_validation_status" \
      "$authorized_next_repository_edits_allowed" \
      "$authorized_next_implementation_allowed" \
      "$authorized_next_implementation_pr_allowed" \
      "$p1b_authorization_declared_status"
    return 0
  fi

  evaluate_p0_to_p1a_transition \
    "$current_task_mode" \
    "$authorized_next_task_mode" \
    "$completion_effective_state" \
    "$clean_synced_main" \
    "$product_source_gate_status" \
    "$p0_merged_main_validation_status" \
    "$p1a_repository_edits_allowed" \
    "$p1a_implementation_allowed" \
    "$p1a_implementation_pr_allowed"
}

evaluate_product_audit_policy() {
  local requested_mode="$1" source_gate_status="$2" worktree_status="$3" synced_main_status="$4"
  local current_pr_count="$5" active_conflict_count="$6" scope_contract="$7"
  local baseline_effective="$8" merged_main_validation_status="$9"
  local repository_edits_allowed="${10}" implementation_allowed="${11}" implementation_pr_allowed="${12}"

  product_audit_allowed="NO"
  read_only_product_audit_status="BLOCKED_NOT_READ_ONLY_PRODUCT_AUDIT"
  closed_technical_debt_blocks_audit="NO"
  closed_technical_debt_effective="NO"
  product_audit_blocker="NOT_READ_ONLY_PRODUCT_AUDIT"

  [[ "$requested_mode" == "READ_ONLY_PRODUCT_AUDIT" ]] || return 0

  if [[ "$source_gate_status" != "PASS" ]]; then
    read_only_product_audit_status="BLOCKED_PRODUCT_SOURCE_GATE"
    product_audit_blocker="PRODUCT_SOURCE_GATE_FAILED"
  elif [[ "$worktree_status" != "Yes" ]]; then
    read_only_product_audit_status="BLOCKED_WORKTREE_DIRTY"
    product_audit_blocker="WORKTREE_DIRTY"
  elif [[ "$synced_main_status" != "YES" ]]; then
    read_only_product_audit_status="BLOCKED_NOT_CLEAN_SYNCED_MAIN"
    product_audit_blocker="NOT_CLEAN_SYNCED_MAIN"
  elif [[ "$baseline_effective" != "YES" || "$merged_main_validation_status" != "PASS" ]]; then
    read_only_product_audit_status="BLOCKED_PENDING_P0_MERGED_MAIN"
    product_audit_blocker="P0_NOT_EFFECTIVE_MERGED_MAIN"
  elif [[ "$scope_contract" != "$readonly_audit_scope_contract" ]] \
    || ! is_false_flag "$repository_edits_allowed" \
    || ! is_false_flag "$implementation_allowed" \
    || ! is_false_flag "$implementation_pr_allowed"; then
    read_only_product_audit_status="BLOCKED_READ_ONLY_SCOPE"
    product_audit_blocker="READ_ONLY_SCOPE_NOT_LOCKED"
  elif [[ "$current_pr_count" != "0" ]]; then
    read_only_product_audit_status="BLOCKED_CURRENT_PACKAGE_PR"
    product_audit_blocker="CURRENT_PACKAGE_PR_PRESENT_OR_UNKNOWN"
  elif [[ "$active_conflict_count" != "0" ]]; then
    read_only_product_audit_status="BLOCKED_ACTIVE_CONFLICTING_PR"
    product_audit_blocker="ACTIVE_CONFLICTING_PR_PRESENT_OR_UNKNOWN"
  else
    product_audit_allowed="YES"
    product_audit_blocker="NONE"
    read_only_product_audit_status="ALLOWED"
  fi
}

run_product_audit_policy_self_test() {
  local failed=0

  assert_transition_case() {
    local name="$1" expected_mode="$2" expected_allowed="$3" expected_status="$4"
    shift 4
    evaluate_p0_to_p1a_transition "$@"
    if [[ "$effective_task_mode" == "$expected_mode" \
      && "$p1a_transition_allowed" == "$expected_allowed" \
      && "$next_task_authorization_status" == "$expected_status" \
      && "$p1b_authorization_runtime_status" == "BLOCKED_AS_EXPECTED" ]]; then
      echo "$name: PASS"
    else
      echo "$name: FAIL"
      failed=1
    fi
  }

  assert_audit_case() {
    local name="$1" expected_allowed="$2" expected_status="$3"
    shift 3
    evaluate_product_audit_policy "$@"
    if [[ "$product_audit_allowed" == "$expected_allowed" \
      && "$read_only_product_audit_status" == "$expected_status" \
      && "$closed_technical_debt_blocks_audit" == "NO" \
      && "$closed_technical_debt_effective" == "NO" ]]; then
      echo "$name: PASS"
    else
      echo "$name: FAIL"
      failed=1
    fi
  }

  assert_p1b_case() {
    local name="$1" expected_completion="$2" expected_allowed="$3" expected_authorization="$4"
    local expected_status="$5" expected_runtime="$6"
    shift 6
    evaluate_p1a_to_p1b_transition "$@"
    if [[ "$p1a_completion_status" == "$expected_completion" \
      && "$next_transition_allowed" == "$expected_allowed" \
      && "$authorization_status" == "$expected_authorization" \
      && "$next_task_authorization_status" == "$expected_status" \
      && "$p1b_authorization_runtime_status" == "$expected_runtime" ]]; then
      echo "$name: PASS"
    else
      echo "$name: FAIL"
      failed=1
    fi
  }

  assert_transition_case TRANSITION_TEST_P0_OPEN PRODUCT_FOUNDATION_REMEDIATION NO BLOCKED_PENDING_P0_MERGED_MAIN \
    PRODUCT_FOUNDATION_REMEDIATION READ_ONLY_PRODUCT_AUDIT PENDING_MERGED_MAIN NO PASS BLOCKED false false false
  assert_transition_case TRANSITION_TEST_P0_READY_UNMERGED PRODUCT_FOUNDATION_REMEDIATION NO BLOCKED_PENDING_P0_MERGED_MAIN \
    PRODUCT_FOUNDATION_REMEDIATION READ_ONLY_PRODUCT_AUDIT PENDING_MERGED_MAIN NO PASS BLOCKED false false false
  assert_transition_case TRANSITION_TEST_P0_MERGED_UNSYNCED PRODUCT_FOUNDATION_REMEDIATION NO BLOCKED_PENDING_LOCAL_ORIGIN_MAIN_MATCH \
    PRODUCT_FOUNDATION_REMEDIATION READ_ONLY_PRODUCT_AUDIT EFFECTIVE_MERGED_MAIN NO PASS PASS false false false
  assert_transition_case TRANSITION_TEST_P0_MERGED_VALIDATED READ_ONLY_PRODUCT_AUDIT YES ALLOWED \
    PRODUCT_FOUNDATION_REMEDIATION READ_ONLY_PRODUCT_AUDIT EFFECTIVE_MERGED_MAIN YES PASS PASS false false false
  assert_transition_case TRANSITION_TEST_P1B_REMAINS_BLOCKED READ_ONLY_PRODUCT_AUDIT YES ALLOWED \
    PRODUCT_FOUNDATION_REMEDIATION READ_ONLY_PRODUCT_AUDIT EFFECTIVE_MERGED_MAIN YES PASS PASS false false false

  assert_audit_case AUDIT_POLICY_TEST_CLOSED_UNMERGED_TECHNICAL_DEBT YES ALLOWED \
    READ_ONLY_PRODUCT_AUDIT PASS Yes YES 0 0 "$readonly_audit_scope_contract" YES PASS false false false
  assert_audit_case AUDIT_POLICY_TEST_P0_NOT_MERGED NO BLOCKED_PENDING_P0_MERGED_MAIN \
    READ_ONLY_PRODUCT_AUDIT PASS Yes YES 0 0 "$readonly_audit_scope_contract" NO BLOCKED false false false
  assert_audit_case AUDIT_POLICY_TEST_CURRENT_PACKAGE_PR NO BLOCKED_CURRENT_PACKAGE_PR \
    READ_ONLY_PRODUCT_AUDIT PASS Yes YES 1 0 "$readonly_audit_scope_contract" YES PASS false false false
  assert_audit_case AUDIT_POLICY_TEST_ACTIVE_CONFLICTING_PR NO BLOCKED_ACTIVE_CONFLICTING_PR \
    READ_ONLY_PRODUCT_AUDIT PASS Yes YES 0 1 "$readonly_audit_scope_contract" YES PASS false false false
  assert_audit_case AUDIT_POLICY_TEST_DIRTY_WORKTREE NO BLOCKED_WORKTREE_DIRTY \
    READ_ONLY_PRODUCT_AUDIT PASS No YES 0 0 "$readonly_audit_scope_contract" YES PASS false false false
  assert_audit_case AUDIT_POLICY_TEST_PRODUCT_SOURCE_GATE_FAILED NO BLOCKED_PRODUCT_SOURCE_GATE \
    READ_ONLY_PRODUCT_AUDIT BLOCKED Yes YES 0 0 "$readonly_audit_scope_contract" YES PASS false false false
  assert_audit_case AUDIT_POLICY_TEST_IMPLEMENTATION_ATTEMPT NO BLOCKED_NOT_READ_ONLY_PRODUCT_AUDIT \
    IMPLEMENTATION PASS Yes YES 0 0 "$readonly_audit_scope_contract" YES PASS false false false
  assert_audit_case AUDIT_POLICY_TEST_ATTEMPTED_REPOSITORY_EDIT NO BLOCKED_READ_ONLY_SCOPE \
    READ_ONLY_PRODUCT_AUDIT PASS Yes YES 0 0 "$readonly_audit_scope_contract" YES PASS true false false

  assert_p1b_case TRANSITION_TEST_P1A_INCOMPLETE BLOCKED NO BLOCKED BLOCKED_P1A_AUDIT_INCOMPLETE BLOCKED \
    P1A_HOME_ALIGNMENT_READINESS_AND_GAP_AUDIT IN_PROGRESS AUTHORIZATION_FLOW_REMEDIATION_ONLY \
    P1B_HOME_ALIGNMENT_FIRST_IMPLEMENTATION IMPLEMENTATION PENDING_MERGED_MAIN NO PASS BLOCKED true true true \
    EFFECTIVE_PENDING_MERGED_MAIN
  assert_p1b_case TRANSITION_TEST_P1B_AUTHORIZATION_UNMERGED PASS NO BLOCKED BLOCKED_PENDING_P1B_AUTHORIZATION_MERGED_MAIN PENDING_MERGED_MAIN \
    P1A_HOME_ALIGNMENT_READINESS_AND_GAP_AUDIT COMPLETED AUTHORIZATION_FLOW_REMEDIATION_ONLY \
    P1B_HOME_ALIGNMENT_FIRST_IMPLEMENTATION IMPLEMENTATION PENDING_MERGED_MAIN NO PASS BLOCKED true true true \
    EFFECTIVE_PENDING_MERGED_MAIN
  assert_p1b_case TRANSITION_TEST_P1B_AUTHORIZATION_EFFECTIVE PASS YES APPROVED ALLOWED EFFECTIVE_MERGED_MAIN \
    P1A_HOME_ALIGNMENT_READINESS_AND_GAP_AUDIT COMPLETED AUTHORIZATION_FLOW_REMEDIATION_ONLY \
    P1B_HOME_ALIGNMENT_FIRST_IMPLEMENTATION IMPLEMENTATION EFFECTIVE_MERGED_MAIN YES PASS PASS true true true \
    EFFECTIVE_PENDING_MERGED_MAIN
  assert_p1b_case TRANSITION_TEST_P1B_UNAUTHORIZED PASS NO BLOCKED BLOCKED_P1B_SCOPE_NOT_AUTHORIZED BLOCKED \
    P1A_HOME_ALIGNMENT_READINESS_AND_GAP_AUDIT COMPLETED AUTHORIZATION_FLOW_REMEDIATION_ONLY \
    P1B_HOME_ALIGNMENT_FIRST_IMPLEMENTATION IMPLEMENTATION EFFECTIVE_MERGED_MAIN YES PASS PASS true true true \
    BLOCKED_PENDING_REVIEW
  assert_p1b_case TRANSITION_TEST_P1B_SOURCE_GATE_FAILURE PASS NO BLOCKED BLOCKED_PRODUCT_SOURCE_GATE BLOCKED \
    P1A_HOME_ALIGNMENT_READINESS_AND_GAP_AUDIT COMPLETED AUTHORIZATION_FLOW_REMEDIATION_ONLY \
    P1B_HOME_ALIGNMENT_FIRST_IMPLEMENTATION IMPLEMENTATION EFFECTIVE_MERGED_MAIN YES BLOCKED PASS true true true \
    EFFECTIVE_PENDING_MERGED_MAIN

  if [[ "$failed" -eq 0 ]]; then
    echo "PRODUCT_AUDIT_POLICY_TESTS: PASS"
    return 0
  fi
  echo "PRODUCT_AUDIT_POLICY_TESTS: BLOCKED"
  return 1
}

machine_gate_policy_check() {
  local expected_package="$1" actual_package="$2" expected_branch="$3" actual_branch="$4"
  local expected_starting_sha="$5" observed_starting_sha="$6" mode="$7"
  local started_from_exact_origin_main="$8" gate_effective_on_origin_main="$9"
  local repository_edits_allowed="${10}" implementation_allowed="${11}" pr_allowed="${12}"
  local push_allowed="${13}" merge_allowed="${14}" deployment_allowed="${15}"
  local changed_files="${16}" allowed_paths="${17}" ordinary_gate_owners_unchanged="${18}"
  local normalized_base_valid="${19:-NO}"
  local changed_path

  [[ -n "$expected_package" && "$actual_package" == "$expected_package" ]] || return 1
  [[ -n "$expected_branch" && "$actual_branch" == "$expected_branch" ]] || return 1
  is_full_git_sha "$expected_starting_sha" || return 1
  is_full_git_sha "$observed_starting_sha" || return 1
  [[ "$observed_starting_sha" == "$expected_starting_sha" ]] || return 1
  is_false_flag "$deployment_allowed" || return 1

  if [[ "$mode" == "DOCS_GATE_BASELINE_RECONCILIATION" ]]; then
    [[ "$started_from_exact_origin_main" == "YES" ]] || return 1
    is_true_flag "$repository_edits_allowed" || return 1
    is_false_flag "$implementation_allowed" || return 1
    is_true_flag "$pr_allowed" || return 1
    is_true_flag "$push_allowed" || return 1
    is_true_flag "$merge_allowed" || return 1
    while IFS= read -r changed_path; do
      [[ -z "$changed_path" ]] && continue
      path_is_in_list "$changed_path" "$allowed_paths" || return 1
    done <<<"$changed_files"
    return 0
  fi

  [[ "$mode" == "IMPLEMENTATION" ]] || return 1
  [[ "$gate_effective_on_origin_main" == "YES" ]] || return 1
  [[ "$ordinary_gate_owners_unchanged" == "YES" ]] || return 1
  [[ "$normalized_base_valid" == "YES" ]] || return 1
  is_true_flag "$repository_edits_allowed" || return 1
  is_true_flag "$implementation_allowed" || return 1
  is_true_flag "$pr_allowed" || return 1
  is_true_flag "$push_allowed" || return 1
  is_true_flag "$merge_allowed" || return 1
  if [[ "$expected_package" == "ANALYSIS_RUN_IDEMPOTENCY_TRANSACTION_BOUNDARY_FIX" \
    || "$expected_package" == "V41_FINAL_RUNTIME_HOME_ACCESS_IDEMPOTENCY_CLOSURE" ]]; then
    [[ -n "$allowed_paths" ]] || return 1
    if printf '%s\n' "$allowed_paths" | grep -Eq '[*?]|(^|/)(src|docs|scripts)/?$'; then
      return 1
    fi
    while IFS= read -r changed_path; do
      [[ -z "$changed_path" ]] && continue
      path_is_in_list "$changed_path" "$allowed_paths" || return 1
    done <<<"$changed_files"
  fi
}

evaluate_machine_runtime_identity() {
  local actual_package expected_package expected_branch expected_starting_sha expected_mode
  local expected_repository_edits expected_implementation expected_pr expected_push expected_merge expected_deployment
  local observed_starting_sha="" started_from_exact_origin_main="NO" changed_files=""
  local gate_owners_unchanged="NO"
  local normalized_base_valid="NO"
  local policy_allowed_paths=""

  actual_package="${requested_package:-$current_package_phase}"
  if [[ "$actual_package" == "$current_package_phase" ]]; then
    expected_package="$current_package_phase"
    expected_branch="$current_package_branch"
    expected_starting_sha="$current_package_starting_full_sha"
    expected_mode="$current_package_mode"
    expected_repository_edits="$current_package_repository_edits_allowed"
    expected_implementation="$current_package_implementation_allowed"
    expected_pr="$current_package_implementation_pr_allowed"
    expected_push="$current_package_push_allowed"
    expected_merge="$current_package_merge_allowed"
    expected_deployment="$current_package_deployment_allowed"
    policy_allowed_paths="$current_package_allowed_paths"
  else
    expected_package="$authorized_next_package_phase"
    expected_branch="$authorized_next_package_branch"
    expected_starting_sha="$authorized_next_package_starting_full_sha"
    expected_mode="$authorized_next_package_mode"
    expected_repository_edits="$authorized_next_repository_edits_allowed"
    expected_implementation="$authorized_next_implementation_allowed"
    expected_pr="$authorized_next_implementation_pr_allowed"
    expected_push="$authorized_next_push_allowed"
    expected_merge="$authorized_next_merge_allowed"
    expected_deployment="$authorized_next_deployment_allowed"
    policy_allowed_paths="$current_package_allowed_paths"
  fi

  current_package_match="NO"
  current_branch_match="NO"
  current_starting_sha_match="NO"
  machine_identity_allowed="NO"
  machine_identity_block_reason="BLOCKED_EXACT_MACHINE_IDENTITY"
  machine_gate_effective_on_origin_main="NO"

  [[ "$actual_package" == "$expected_package" ]] && current_package_match="YES"
  [[ "${branch:-}" == "$expected_branch" ]] && current_branch_match="YES"
  if is_full_git_sha "$expected_starting_sha" \
    && git cat-file -e "$expected_starting_sha^{commit}" >/dev/null 2>&1 \
    && git merge-base --is-ancestor "$expected_starting_sha" HEAD >/dev/null 2>&1; then
    observed_starting_sha="$expected_starting_sha"
    current_starting_sha_match="YES"
  fi

  if [[ "$expected_package" == "$current_package_phase" ]]; then
    if [[ "$expected_package" == "TRINE_LOGIC_V4_1_FINAL_RUNTIME_HOME_ACCESS_IDEMPOTENCY_CLOSURE_AUTHORIZATION" ]] \
      && is_full_git_sha "$current_package_starting_full_sha" \
      && git merge-base --is-ancestor "$current_package_starting_full_sha" origin/main >/dev/null 2>&1 \
      && git merge-base --is-ancestor origin/main HEAD >/dev/null 2>&1; then
      started_from_exact_origin_main="YES"
      changed_files="$(changed_paths_from_origin_main)"
    else
      if is_full_git_sha "$current_package_starting_full_sha" \
        && [[ "${origin_main_sha:-}" == "$current_package_starting_full_sha" ]]; then
        started_from_exact_origin_main="YES"
      fi
      changed_files="$(changed_paths_from_starting_sha "$current_package_starting_full_sha")"
    fi
  else
    if git show origin/main:docs/CODEX_NEXT_TASK.yml 2>/dev/null \
      | grep -Fq "current_package_phase: \"$current_package_phase\"" \
      && git show origin/main:docs/CODEX_NEXT_TASK.yml 2>/dev/null \
      | grep -Fq "authorized_next_package_starting_full_sha: \"$authorized_next_package_starting_full_sha\"" \
      && git merge-base --is-ancestor origin/main HEAD >/dev/null 2>&1; then
      machine_gate_effective_on_origin_main="YES"
    fi
    if ordinary_package_preserves_gate_owners; then
      gate_owners_unchanged="YES"
    fi
    if [[ "$expected_package" == "REAL_DATA_HOME_BLOCKER_CLOSURE" ]] \
      && [[ "$authorized_next_normalization_one_time_only" == "true" ]] \
      && [[ "$authorized_next_normalization_expected_source" == "MERGED_MAIN" ]] \
      && normalization_commit_matches \
        "$authorized_next_normalization_source_parent_sha" \
        "$authorized_next_normalization_allowed_files" \
        "$authorized_next_normalization_extra_file_count"; then
      normalized_base_valid="YES"
    elif [[ "$expected_package" == "ANALYSIS_RUN_IDEMPOTENCY_TRANSACTION_BOUNDARY_FIX" \
      || "$expected_package" == "V41_FINAL_RUNTIME_HOME_ACCESS_IDEMPOTENCY_CLOSURE" ]] \
      && git merge-base --is-ancestor origin/main HEAD >/dev/null 2>&1; then
      changed_files="$(changed_paths_from_origin_main)"
      policy_allowed_paths="$authorized_next_package_allowed_paths"
      normalized_base_valid="YES"
    fi
  fi

  if machine_gate_policy_check \
    "$expected_package" "$actual_package" "$expected_branch" "${branch:-}" \
    "$expected_starting_sha" "$observed_starting_sha" "$expected_mode" \
    "$started_from_exact_origin_main" "$machine_gate_effective_on_origin_main" \
    "$expected_repository_edits" "$expected_implementation" "$expected_pr" \
    "$expected_push" "$expected_merge" "$expected_deployment" \
    "$changed_files" "$policy_allowed_paths" "$gate_owners_unchanged" "$normalized_base_valid"; then
    machine_identity_allowed="YES"
    machine_identity_block_reason="NONE"
  fi
}

run_exact_machine_gate_self_test() {
  local failed=0
  local owner_package="TRINE_LOGIC_V4_1_FINAL_RUNTIME_HOME_ACCESS_IDEMPOTENCY_CLOSURE_AUTHORIZATION"
  local owner_branch="codex/v4-1-final-runtime-home-access-idempotency-closure-authorization"
  local owner_sha="0e9bd779b10e9d3140b8ceaea0a5193a28d6264f"
  local target_package="V41_FINAL_RUNTIME_HOME_ACCESS_IDEMPOTENCY_CLOSURE"
  local target_branch="codex/v4-1-final-runtime-home-access-idempotency-closure"
  local target_sha="0e9bd779b10e9d3140b8ceaea0a5193a28d6264f"
  local owner_paths target_paths
  owner_paths="$(gate_owner_paths)"
  target_paths="$(printf '%s\n' \
    src/main/java/org/example/trademodel/service/impl/DashboardHomeServiceImpl.java \
    src/main/resources/templates/home.html \
    src/main/resources/static/css/home.css \
    src/main/resources/static/js/home-runtime.js \
    src/test/java/org/example/trademodel/service/impl/DashboardHomeServiceImplTest.java \
    src/test/java/org/example/trademodel/controller/ApprovedFigmaHomeRuntimeContractTest.java \
    src/test/java/org/example/trademodel/controller/HomeUiReviewRuntimeContractTest.java \
    src/test/java/org/example/trademodel/controller/FundamentalAiV41FrontendRuntimeAlignmentContractTest.java \
    docs/GLOBAL_UI_ALIGNMENT_VISUAL_ACCEPTANCE.md \
    docs/GLOBAL_UI_ALIGNMENT_IMPLEMENTATION_REPORT.md)"

  assert_machine_pass() {
    local name="$1"
    shift
    if machine_gate_policy_check "$@"; then
      printf '%s: PASS\n' "$name"
    else
      printf '%s: FAIL\n' "$name"
      failed=1
    fi
  }

  assert_machine_blocked() {
    local name="$1"
    shift
    if machine_gate_policy_check "$@"; then
      printf '%s: FAIL\n' "$name"
      failed=1
    else
      printf '%s: PASS\n' "$name"
    fi
  }

  assert_machine_pass EXACT_GATE_01_CORRECT_TRIPLE \
    "$target_package" "$target_package" "$target_branch" "$target_branch" "$target_sha" "$target_sha" \
    IMPLEMENTATION NO YES true true true true true false src/main/java/org/example/trademodel/service/impl/DashboardHomeServiceImpl.java "$target_paths" YES YES
  assert_machine_blocked EXACT_GATE_02_WRONG_SHA \
    "$target_package" "$target_package" "$target_branch" "$target_branch" "$target_sha" "b60eff8d83c0e1d04371bd425267f1e8d0e4f95c" \
    IMPLEMENTATION NO YES true true true true true false "" "$target_paths" YES YES
  assert_machine_blocked EXACT_GATE_03_SHORT_SHA \
    "$target_package" "$target_package" "$target_branch" "$target_branch" a60eff8d a60eff8d \
    IMPLEMENTATION NO YES true true true true true false "" "$target_paths" YES YES
  assert_machine_blocked EXACT_GATE_04_MISSING_SHA \
    "$target_package" "$target_package" "$target_branch" "$target_branch" "" "" \
    IMPLEMENTATION NO YES true true true true true false "" "$target_paths" YES YES
  assert_machine_blocked EXACT_GATE_05_WRONG_BRANCH \
    "$target_package" "$target_package" "$target_branch" codex/wrong-branch "$target_sha" "$target_sha" \
    IMPLEMENTATION NO YES true true true true true false "" "$target_paths" YES YES
  assert_machine_blocked EXACT_GATE_06_WRONG_PACKAGE \
    "$target_package" WRONG_PACKAGE "$target_branch" "$target_branch" "$target_sha" "$target_sha" \
    IMPLEMENTATION NO YES true true true true true false "" "$target_paths" YES YES
  assert_machine_blocked EXACT_GATE_07_OUT_OF_SCOPE_FILE \
    "$owner_package" "$owner_package" "$owner_branch" "$owner_branch" "$owner_sha" "$owner_sha" \
    DOCS_GATE_BASELINE_RECONCILIATION YES NO true false true true true false $'README.md' "$owner_paths" YES NO
  assert_machine_blocked EXACT_GATE_08_JAVA_SQL_CSS_MIX \
    "$owner_package" "$owner_package" "$owner_branch" "$owner_branch" "$owner_sha" "$owner_sha" \
    DOCS_GATE_BASELINE_RECONCILIATION YES NO true false true true true false $'src/main/java/Unsafe.java\nsrc/main/resources/db/migration/V16__unsafe.sql\nsrc/main/resources/static/css/unsafe.css' "$owner_paths" YES NO
  assert_machine_blocked EXACT_GATE_09_ORDINARY_PACKAGE_GATE_OWNER_MUTATION \
    "$target_package" "$target_package" "$target_branch" "$target_branch" "$target_sha" "$target_sha" \
    IMPLEMENTATION NO YES true true true true true false "" "$target_paths" NO YES
  assert_machine_blocked EXACT_GATE_10_OWNER_AMENDMENT_NON_MAIN_START \
    "$owner_package" "$owner_package" "$owner_branch" "$owner_branch" "$owner_sha" "$owner_sha" \
    DOCS_GATE_BASELINE_RECONCILIATION NO NO true false true true true false docs/CODEX_NEXT_TASK.yml "$owner_paths" YES NO
  assert_machine_blocked EXACT_GATE_11_OWNER_PERMISSION_MISMATCH \
    "$owner_package" "$owner_package" "$owner_branch" "$owner_branch" "$owner_sha" "$owner_sha" \
    DOCS_GATE_BASELINE_RECONCILIATION YES NO true false false false false false docs/CODEX_NEXT_TASK.yml "$owner_paths" YES NO
  assert_machine_blocked EXACT_GATE_12_BLOCKED_PACKAGE_REGRESSION \
    "$target_package" FUNDAMENTAL_AI_V4_1_AUTO_TRADING "$target_branch" "$target_branch" "$target_sha" "$target_sha" \
    IMPLEMENTATION NO YES true true true true true false "" "$target_paths" YES YES
  assert_machine_blocked EXACT_GATE_13_SCOPE_PROOF_MISSING \
    "$target_package" "$target_package" "$target_branch" "$target_branch" "$target_sha" "$target_sha" \
    IMPLEMENTATION NO YES true true true true true false "" "$target_paths" YES NO
  assert_machine_blocked EXACT_GATE_14_IMPLEMENTATION_OUT_OF_SCOPE_FILE \
    "$target_package" "$target_package" "$target_branch" "$target_branch" "$target_sha" "$target_sha" \
    IMPLEMENTATION NO YES true true true true true false src/main/java/org/example/trademodel/service/Unsafe.java "$target_paths" YES YES

  if [[ "$failed" -eq 0 ]]; then
    echo "EXACT_MACHINE_GATE_TESTS: PASS"
    return 0
  fi
  echo "EXACT_MACHINE_GATE_TESTS: BLOCKED"
  return 1
}

run_policy_self_test="NO"
run_exact_gate_self_test="NO"
while [[ "$#" -gt 0 ]]; do
  case "$1" in
    --self-test-product-audit-policy)
      run_policy_self_test="YES"
      shift
      ;;
    --self-test-exact-machine-gate)
      run_exact_gate_self_test="YES"
      shift
      ;;
    --open-pr-none-confirmed)
      open_pr_none_confirmed="YES"
      open_pr_evidence_input_valid="YES"
      shift
      ;;
    --request-package)
      [[ -n "${2:-}" ]] || { echo "--request-package requires a package identifier" >&2; exit 2; }
      requested_package="$2"
      shift 2
      ;;
    *)
      echo "usage: bash scripts/v1-state.sh [--self-test-product-audit-policy] [--self-test-exact-machine-gate] [--open-pr-none-confirmed] [--request-package PACKAGE]" >&2
      exit 2
      ;;
  esac
done

if [[ "$run_policy_self_test" == "YES" ]]; then
  run_product_audit_policy_self_test
  exit $?
fi

if [[ "$run_exact_gate_self_test" == "YES" ]]; then
  run_exact_machine_gate_self_test
  exit $?
fi

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

load_task_package_contract() {
  current_package_phase="$(yaml_value "$TASK_FILE" current_package_phase)"
  current_package_name="$(yaml_value "$TASK_FILE" current_package_name)"
  current_package_active_block="$(yaml_value "$TASK_FILE" current_package_active_block)"
  current_package_mode="$(yaml_value "$TASK_FILE" current_package_mode)"
  current_package_status="$(yaml_value "$TASK_FILE" current_package_status)"
  current_package_branch="$(yaml_value "$TASK_FILE" current_package_branch)"
  current_package_starting_full_sha="$(yaml_value "$TASK_FILE" current_package_starting_full_sha)"
  current_package_risk="$(yaml_value "$TASK_FILE" current_package_risk)"
  current_package_repository_edits_allowed="$(yaml_value "$TASK_FILE" current_package_repository_edits_allowed)"
  current_package_implementation_allowed="$(yaml_value "$TASK_FILE" current_package_implementation_allowed)"
  current_package_implementation_pr_allowed="$(yaml_value "$TASK_FILE" current_package_implementation_pr_allowed)"
  current_package_push_allowed="$(yaml_value "$TASK_FILE" current_package_push_allowed)"
  current_package_merge_allowed="$(yaml_value "$TASK_FILE" current_package_merge_allowed)"
  current_package_deployment_allowed="$(yaml_value "$TASK_FILE" current_package_deployment_allowed)"
  current_package_requires_clean_origin_main="$(yaml_value "$TASK_FILE" current_package_requires_clean_origin_main)"
  current_package_allowed_paths="$(yaml_list "$TASK_FILE" current_package_allowed_paths)"
  current_package_next_action="$(yaml_value "$TASK_FILE" current_package_next_action)"

  authorized_next_package_phase="$(yaml_value "$TASK_FILE" authorized_next_package_phase)"
  authorized_next_package_name="$(yaml_value "$TASK_FILE" authorized_next_package_name)"
  authorized_next_package_active_block="$(yaml_value "$TASK_FILE" authorized_next_package_active_block)"
  authorized_next_package_mode="$(yaml_value "$TASK_FILE" authorized_next_package_mode)"
  authorized_next_package_branch="$(yaml_value "$TASK_FILE" authorized_next_package_branch)"
  authorized_next_package_starting_full_sha="$(yaml_value "$TASK_FILE" authorized_next_package_starting_full_sha)"
  authorized_next_normalization_source_parent_sha="$(yaml_value "$TASK_FILE" authorized_next_package_normalization_source_parent_sha)"
  authorized_next_normalization_expected_source="$(yaml_value "$TASK_FILE" authorized_next_package_normalization_expected_source)"
  authorized_next_normalization_extra_file_count="$(yaml_value "$TASK_FILE" authorized_next_package_normalization_extra_file_count)"
  authorized_next_normalization_one_time_only="$(yaml_value "$TASK_FILE" authorized_next_package_normalization_one_time_only)"
  authorized_next_normalization_allowed_files="$(yaml_list "$TASK_FILE" authorized_next_package_normalization_allowed_files)"
  authorized_next_package_allowed_paths="$(yaml_list "$TASK_FILE" authorized_next_package_allowed_paths)"
  authorized_next_package_risk="$(yaml_value "$TASK_FILE" authorized_next_package_risk)"
  authorized_next_repository_edits_allowed="$(yaml_value "$TASK_FILE" authorized_next_package_repository_edits_allowed)"
  authorized_next_implementation_allowed="$(yaml_value "$TASK_FILE" authorized_next_package_implementation_allowed)"
  authorized_next_implementation_pr_allowed="$(yaml_value "$TASK_FILE" authorized_next_package_implementation_pr_allowed)"
  authorized_next_push_allowed="$(yaml_value "$TASK_FILE" authorized_next_package_push_allowed)"
  authorized_next_merge_allowed="$(yaml_value "$TASK_FILE" authorized_next_package_merge_allowed)"
  authorized_next_deployment_allowed="$(yaml_value "$TASK_FILE" authorized_next_package_deployment_allowed)"
  authorized_next_canonical_figma_desktop_implementation_allowed="$(yaml_value "$TASK_FILE" authorized_next_package_canonical_figma_desktop_implementation_allowed)"
  authorized_next_mobile_implementation_allowed="$(yaml_value "$TASK_FILE" authorized_next_package_mobile_implementation_allowed)"
  authorized_next_canonical_figma_file_key="$(yaml_value "$TASK_FILE" authorized_next_package_canonical_figma_file_key)"
  authorized_next_package_next_action="$(yaml_value "$TASK_FILE" authorized_next_package_next_action)"
  blocked_package_phase="$(yaml_value "$TASK_FILE" blocked_package_phase)"
  blocked_package_status="$(yaml_value "$TASK_FILE" blocked_package_status)"
  p1b_1_declared_status="$(yaml_value "$TASK_FILE" p1b_1_status)"
  p1b_authorization_declared_status="$(yaml_value "$TASK_FILE" p1b_authorization_status)"
  home_core_data_declared_status="$(yaml_value "$TASK_FILE" p1b_home_core_data_authorization_status)"
  home_core_data_implementation_status="$(yaml_value "$TASK_FILE" p1b_home_core_data_implementation_status)"
  product_p1b_declared_status="$(yaml_value "$TASK_FILE" product_p1b_status)"
  p2_authorization_declared_status="$(yaml_value "$TASK_FILE" p2_position_monitoring_authorization_status)"
  p2_implementation_status="$(yaml_value "$TASK_FILE" p2_position_monitoring_implementation_status)"
  v4_1_design_status="$(yaml_value "$TASK_FILE" v4_1_final_interaction_design_status)"
  v4_1_authorization_declared_status="$(yaml_value "$TASK_FILE" v4_1_final_interaction_authorization_status)"
  v4_1_implementation_status="$(yaml_value "$TASK_FILE" v4_1_final_interaction_implementation_status)"
  v4_1_target_runtime_authorization_declared_status="$(yaml_value "$TASK_FILE" v4_1_target_runtime_remediation_authorization_status)"
  v4_1_target_runtime_implementation_status="$(yaml_value "$TASK_FILE" v4_1_target_runtime_remediation_implementation_status)"
  v4_1_target_runtime_status="$(yaml_value "$TASK_FILE" v4_1_target_runtime_status)"
  v4_1_telegram_authorization_declared_status="$(yaml_value "$TASK_FILE" v4_1_telegram_authorization_status)"
  v4_1_telegram_implementation_status="$(yaml_value "$TASK_FILE" v4_1_telegram_implementation_status)"
  v4_1_telegram_live_acceptance_status="$(yaml_value "$TASK_FILE" v4_1_telegram_live_acceptance_status)"
  v4_1_telegram_remediation_authorization_declared_status="$(yaml_value "$TASK_FILE" v4_1_telegram_remediation_authorization_status)"
  v4_1_telegram_remediation_implementation_status="$(yaml_value "$TASK_FILE" v4_1_telegram_remediation_implementation_status)"
  v4_1_core_production_loop_authorization_declared_status="$(yaml_value "$TASK_FILE" v4_1_core_production_loop_authorization_status)"
  v4_1_core_production_loop_implementation_status="$(yaml_value "$TASK_FILE" v4_1_core_production_loop_implementation_status)"
  v4_1_machine_gate_owner_amendment_declared_status="$(yaml_value "$TASK_FILE" v4_1_machine_gate_owner_amendment_status)"
  v4_1_baseline_reconciliation_gate_declared_status="$(yaml_value "$TASK_FILE" v4_1_baseline_reconciliation_gate_status)"
  real_data_home_blocker_closure_authorization_declared_status="$(yaml_value "$TASK_FILE" real_data_home_blocker_closure_authorization_status)"
  real_data_home_blocker_closure_implementation_status="$(yaml_value "$TASK_FILE" real_data_home_blocker_closure_implementation_status)"
  analysis_run_idempotency_tx_fix_authorization_declared_status="$(yaml_value "$TASK_FILE" analysis_run_idempotency_tx_fix_authorization_status)"
  analysis_run_idempotency_tx_fix_implementation_status="$(yaml_value "$TASK_FILE" analysis_run_idempotency_tx_fix_implementation_status)"
  final_runtime_home_closure_authorization_declared_status="$(yaml_value "$TASK_FILE" v4_1_final_runtime_home_access_idempotency_closure_authorization_status)"
  final_runtime_home_closure_implementation_status="$(yaml_value "$TASK_FILE" v4_1_final_runtime_home_access_idempotency_closure_implementation_status)"
  local_real_authorization_declared_status="$(yaml_value "$TASK_FILE" local_real_authorization_status)"
  local_real_implementation_status="$(yaml_value "$TASK_FILE" local_real_implementation_status)"
  frontend_interaction_authorization_declared_status="$(yaml_value "$TASK_FILE" frontend_interaction_authorization_status)"
  frontend_interaction_implementation_status="$(yaml_value "$TASK_FILE" frontend_interaction_implementation_status)"
  multi_user_authorization_declared_status="$(yaml_value "$TASK_FILE" multi_user_authorization_status)"
  multi_user_implementation_status="$(yaml_value "$TASK_FILE" multi_user_implementation_status)"
  audit_scope_contract="$(yaml_value "$TASK_FILE" read_only_product_audit_scope_contract)"
}

run_handoff_resolution_simulation() {
  local scenario="$1"
  local provided_open_pr_confirmation="$open_pr_none_confirmed"
  local provided_evidence_valid="$open_pr_evidence_input_valid"
  local provided_requested_package="$requested_package"

  load_task_package_contract
  current_task_mode="$current_package_mode"
  authorized_next_task_mode="$authorized_next_package_mode"
  p1a_repository_edits_allowed="false"
  p1a_implementation_allowed="false"
  p1a_implementation_pr_allowed="false"
  completion_effective_state="PENDING_MERGED_MAIN"
  clean_synced_main="NO"
  product_source_gate_status="PASS"
  p0_merged_main_validation_status="BLOCKED"
  worktree_clean="Yes"
  current_package_pr_count="1"
  current_package_pr_draft="true"
  active_conflicting_pr_count="0"
  branch="$current_package_branch"
  main_sync="OK"
  open_prs="current authorization package"
  can_continue="NO"
  blockers_text="AUTHORIZATION_PENDING_MERGED_MAIN"
  requested_package="$provided_requested_package"
  open_pr_evidence_source="GH_QUERY"
  open_pr_none_confirmed="NO"
  open_pr_evidence_input_valid="$provided_evidence_valid"
  current_package_match="YES"
  current_branch_match="YES"
  current_starting_sha_match="YES"
  machine_identity_allowed="YES"
  machine_identity_block_reason="NONE"
  machine_gate_effective_on_origin_main="NO"

  case "$scenario" in
    authorization_pending|current_authorization_remediation)
      ;;
    authorization_ready_unmerged|current_authorization_final_gate|current_pr_self_conflict)
      current_package_pr_draft="false"
      ;;
    p1a_incomplete)
      current_package_status="IN_PROGRESS"
      requested_package="$authorized_next_package_phase"
      ;;
    predecessor_incomplete)
      if [[ "$current_package_phase" == "TRINE_LOGIC_V4_1_BASELINE_RECONCILIATION_GATE" ]]; then
        real_data_home_blocker_closure_authorization_declared_status="BLOCKED_PENDING_REVIEW"
        blockers_text="REAL_DATA_HOME_BLOCKER_CLOSURE_NOT_AUTHORIZED"
      elif [[ "$current_package_phase" == "TRINE_LOGIC_V4_1_FINAL_RUNTIME_HOME_ACCESS_IDEMPOTENCY_CLOSURE_AUTHORIZATION" ]]; then
        final_runtime_home_closure_authorization_declared_status="BLOCKED_PENDING_REVIEW"
        blockers_text="FINAL_RUNTIME_HOME_CLOSURE_NOT_AUTHORIZED"
      elif [[ "$current_package_phase" == "FUNDAMENTAL_AI_V4_1_CORE_PRODUCTION_LOOP_AUTOMATION_AUTHORIZATION" ]]; then
        v4_1_implementation_status="IN_PROGRESS"
      elif [[ "$current_package_phase" == "FUNDAMENTAL_AI_V4_1_TELEGRAM_HIGH_VALUE_ALERT_CHANNEL_REMEDIATION_AUTHORIZATION" ]]; then
        v4_1_telegram_implementation_status="IN_PROGRESS"
      elif [[ "$current_package_phase" == "MULTI_USER_ACCOUNT_REGISTRATION_CLOSURE_AUTHORIZATION" ]]; then
        frontend_interaction_implementation_status="IN_PROGRESS"
      elif [[ "$current_package_phase" == "FRONTEND_INTERACTION_RUNTIME_CLOSURE_AUTHORIZATION" ]]; then
        local_real_implementation_status="IN_PROGRESS"
      elif [[ "$current_package_phase" == "LOCAL_REAL_READINESS_SYNC_AND_REAL_ANALYSIS_ENABLEMENT_AUTHORIZATION" ]]; then
        v4_1_telegram_implementation_status="IN_PROGRESS"
      elif [[ "$current_package_phase" == "FUNDAMENTAL_AI_V4_1_TELEGRAM_HIGH_VALUE_ALERT_CHANNEL_AUTHORIZATION" ]]; then
        v4_1_target_runtime_implementation_status="IN_PROGRESS"
      elif [[ "$current_package_phase" == "FUNDAMENTAL_AI_V4_1_TARGET_RUNTIME_BLOCKER_REMEDIATION_AUTHORIZATION" ]]; then
        v4_1_implementation_status="IN_PROGRESS"
      elif [[ "$current_package_phase" == "P2_POSITION_MONITORING_AUTHORIZATION" ]]; then
        product_p1b_declared_status="IN_PROGRESS"
      else
        p1b_1_declared_status="IN_PROGRESS"
      fi
      requested_package="$authorized_next_package_phase"
      ;;
    authorization_pending_request_p1b|authorization_pending_request_p2|authorization_pending_request_v4_1)
      requested_package="$authorized_next_package_phase"
      ;;
    authorization_merged_unsynced)
      completion_effective_state="EFFECTIVE_MERGED_MAIN"
      branch="main"
      main_sync="BEHIND_ORIGIN_MAIN by 1"
      current_package_pr_count="0"
      current_package_pr_draft="NONE"
      open_prs="none"
      blockers_text="MAIN_BEHIND_ORIGIN"
      ;;
    authorization_merged_validated|p1b_operator|p2_operator|v4_1_operator|closed_pr_1156|merged_gh_no_pr)
      completion_effective_state="EFFECTIVE_MERGED_MAIN"
      clean_synced_main="YES"
      p0_merged_main_validation_status="PASS"
      branch="main"
      current_package_pr_count="0"
      current_package_pr_draft="NONE"
      open_prs="none"
      can_continue="YES"
      blockers_text="none"
      ;;
    merged_gh_unavailable_no_evidence|merged_gh_unavailable)
      completion_effective_state="EFFECTIVE_MERGED_MAIN"
      clean_synced_main="YES"
      p0_merged_main_validation_status="PASS"
      branch="main"
      current_package_pr_count="UNKNOWN"
      current_package_pr_draft="UNKNOWN"
      active_conflicting_pr_count="UNKNOWN"
      open_prs="GH_NOT_AVAILABLE"
      open_pr_evidence_source="UNAVAILABLE"
      blockers_text="OPEN_PR_STATUS_UNKNOWN_GH_NOT_AVAILABLE"
      if [[ "$scenario" == "merged_gh_unavailable" \
        && "$provided_open_pr_confirmation" == "YES" \
        && "$provided_evidence_valid" == "YES" ]]; then
        current_package_pr_count="0"
        current_package_pr_draft="NONE"
        active_conflicting_pr_count="0"
        open_prs="none"
        open_pr_evidence_source="EXPLICIT_CONFIRMED"
        open_pr_none_confirmed="YES"
        blockers_text="none"
      fi
      ;;
    merged_explicit_no_pr)
      completion_effective_state="EFFECTIVE_MERGED_MAIN"
      clean_synced_main="YES"
      p0_merged_main_validation_status="PASS"
      branch="main"
      current_package_pr_count="0"
      current_package_pr_draft="NONE"
      open_prs="none"
      open_pr_evidence_source="EXPLICIT_CONFIRMED"
      open_pr_none_confirmed="YES"
      blockers_text="none"
      ;;
    explicit_with_conflict)
      completion_effective_state="EFFECTIVE_MERGED_MAIN"
      clean_synced_main="YES"
      p0_merged_main_validation_status="PASS"
      branch="main"
      current_package_pr_count="0"
      current_package_pr_draft="NONE"
      active_conflicting_pr_count="1"
      open_prs="#2000 active-conflicting-pr"
      open_pr_evidence_source="GH_QUERY"
      open_pr_none_confirmed="$provided_open_pr_confirmation"
      blockers_text="ACTIVE_CONFLICTING_PR"
      ;;
    p1b_unauthorized|p2_unauthorized|v4_1_unauthorized|telegram_unauthorized)
      completion_effective_state="EFFECTIVE_MERGED_MAIN"
      clean_synced_main="YES"
      p0_merged_main_validation_status="PASS"
      branch="main"
      current_package_pr_count="0"
      current_package_pr_draft="NONE"
      open_prs="none"
      requested_package="$authorized_next_package_phase"
      if [[ "$current_package_phase" == "TRINE_LOGIC_V4_1_BASELINE_RECONCILIATION_GATE" ]]; then
        real_data_home_blocker_closure_authorization_declared_status="BLOCKED_PENDING_REVIEW"
        blockers_text="REAL_DATA_HOME_BLOCKER_CLOSURE_NOT_AUTHORIZED"
      elif [[ "$current_package_phase" == "TRINE_LOGIC_V4_1_FINAL_RUNTIME_HOME_ACCESS_IDEMPOTENCY_CLOSURE_AUTHORIZATION" ]]; then
        final_runtime_home_closure_authorization_declared_status="BLOCKED_PENDING_REVIEW"
        blockers_text="FINAL_RUNTIME_HOME_CLOSURE_NOT_AUTHORIZED"
      elif [[ "$current_package_phase" == "FUNDAMENTAL_AI_V4_1_CORE_PRODUCTION_LOOP_AUTOMATION_AUTHORIZATION" ]]; then
        v4_1_core_production_loop_authorization_declared_status="BLOCKED_PENDING_REVIEW"
        blockers_text="V4_1_CORE_PRODUCTION_LOOP_NOT_AUTHORIZED"
      elif [[ "$current_package_phase" == "FUNDAMENTAL_AI_V4_1_TELEGRAM_HIGH_VALUE_ALERT_CHANNEL_REMEDIATION_AUTHORIZATION" ]]; then
        v4_1_telegram_remediation_authorization_declared_status="BLOCKED_PENDING_REVIEW"
        blockers_text="V4_1_TELEGRAM_REMEDIATION_NOT_AUTHORIZED"
      elif [[ "$current_package_phase" == "MULTI_USER_ACCOUNT_REGISTRATION_CLOSURE_AUTHORIZATION" ]]; then
        multi_user_authorization_declared_status="BLOCKED_PENDING_REVIEW"
        blockers_text="MULTI_USER_NOT_AUTHORIZED"
      elif [[ "$current_package_phase" == "FRONTEND_INTERACTION_RUNTIME_CLOSURE_AUTHORIZATION" ]]; then
        frontend_interaction_authorization_declared_status="BLOCKED_PENDING_REVIEW"
        blockers_text="FRONTEND_INTERACTION_NOT_AUTHORIZED"
      elif [[ "$current_package_phase" == "LOCAL_REAL_READINESS_SYNC_AND_REAL_ANALYSIS_ENABLEMENT_AUTHORIZATION" ]]; then
        local_real_authorization_declared_status="BLOCKED_PENDING_REVIEW"
        blockers_text="LOCAL_REAL_NOT_AUTHORIZED"
      elif [[ "$current_package_phase" == "FUNDAMENTAL_AI_V4_1_TELEGRAM_HIGH_VALUE_ALERT_CHANNEL_AUTHORIZATION" ]]; then
        v4_1_telegram_authorization_declared_status="BLOCKED_PENDING_REVIEW"
        blockers_text="V4_1_TELEGRAM_NOT_AUTHORIZED"
      elif [[ "$current_package_phase" == "FUNDAMENTAL_AI_V4_1_TARGET_RUNTIME_BLOCKER_REMEDIATION_AUTHORIZATION" ]]; then
        v4_1_target_runtime_authorization_declared_status="BLOCKED_PENDING_REVIEW"
        blockers_text="V4_1_TARGET_RUNTIME_NOT_AUTHORIZED"
      elif [[ "$current_package_phase" == "P2_POSITION_MONITORING_AUTHORIZATION" ]]; then
        p2_authorization_declared_status="BLOCKED_PENDING_REVIEW"
        blockers_text="P2_NOT_AUTHORIZED"
      else
        home_core_data_declared_status="BLOCKED_PENDING_REVIEW"
        blockers_text="P1B_NOT_AUTHORIZED"
      fi
      ;;
    conflicting_pr|separate_conflicting_pr_successor)
      completion_effective_state="EFFECTIVE_MERGED_MAIN"
      clean_synced_main="YES"
      p0_merged_main_validation_status="PASS"
      branch="main"
      current_package_pr_count="0"
      current_package_pr_draft="NONE"
      active_conflicting_pr_count="1"
      open_prs="#2000 active-conflicting-pr"
      blockers_text="ACTIVE_CONFLICTING_PR"
      ;;
    separate_conflicting_pr_current)
      active_conflicting_pr_count="1"
      open_prs=$'current authorization package\n#2000 active-conflicting-pr'
      blockers_text="ACTIVE_CONFLICTING_PR_BLOCKS_SUCCESSOR_ONLY"
      ;;
    p1b_permission_missing|p2_permission_missing|v4_1_permission_missing|telegram_permission_missing)
      completion_effective_state="EFFECTIVE_MERGED_MAIN"
      clean_synced_main="YES"
      p0_merged_main_validation_status="PASS"
      branch="main"
      current_package_pr_count="0"
      current_package_pr_draft="NONE"
      open_prs="none"
      authorized_next_implementation_allowed="false"
      blockers_text="IMPLEMENTATION_PERMISSION_INCOMPLETE"
      ;;
    dirty_worktree)
      completion_effective_state="EFFECTIVE_MERGED_MAIN"
      clean_synced_main="YES"
      p0_merged_main_validation_status="PASS"
      branch="main"
      worktree_clean="No"
      current_package_pr_count="0"
      current_package_pr_draft="NONE"
      open_prs="none"
      blockers_text="WORKTREE_DIRTY"
      ;;
    product_source_failure)
      completion_effective_state="EFFECTIVE_MERGED_MAIN"
      clean_synced_main="YES"
      p0_merged_main_validation_status="PASS"
      branch="main"
      product_source_gate_status="BLOCKED"
      current_package_pr_count="0"
      current_package_pr_draft="NONE"
      open_prs="none"
      blockers_text="PRODUCT_SOURCE_GATE_FAILED"
      ;;
    unknown_state)
      completion_effective_state="UNKNOWN"
      clean_synced_main="UNKNOWN"
      current_package_pr_count="UNKNOWN"
      current_package_pr_draft="UNKNOWN"
      active_conflicting_pr_count="UNKNOWN"
      branch="main"
      main_sync="UNKNOWN"
      open_prs="UNKNOWN"
      blockers_text="UNKNOWN_STATE"
      ;;
    *)
      echo "unknown handoff self-test scenario: $scenario" >&2
      exit 2
      ;;
  esac

  if [[ -z "$requested_package" && "$completion_effective_state" == "EFFECTIVE_MERGED_MAIN" ]]; then
    requested_package="$authorized_next_package_phase"
  fi

  if [[ "$requested_package" == "$authorized_next_package_phase" \
    && "$completion_effective_state" == "EFFECTIVE_MERGED_MAIN" \
    && "$scenario" != "authorization_merged_unsynced" \
    && "$scenario" != "unknown_state" ]]; then
    branch="$authorized_next_package_branch"
    machine_gate_effective_on_origin_main="YES"
    current_package_match="YES"
    current_branch_match="YES"
    current_starting_sha_match="YES"
    machine_identity_allowed="YES"
    machine_identity_block_reason="NONE"
  elif [[ -n "$requested_package" && "$requested_package" != "$current_package_phase" ]]; then
    current_package_match="NO"
    current_branch_match="NO"
    current_starting_sha_match="NO"
    machine_identity_allowed="NO"
    machine_identity_block_reason="BLOCKED_EXACT_MACHINE_IDENTITY"
  fi

  evaluate_runtime_transition

  local baseline_effective="NO"
  [[ "$completion_effective_state" == "EFFECTIVE_MERGED_MAIN" ]] && baseline_effective="YES"
  evaluate_product_audit_policy \
    "$effective_task_mode" \
    "$product_source_gate_status" \
    "$worktree_clean" \
    "$clean_synced_main" \
    "$current_package_pr_count" \
    "$active_conflicting_pr_count" \
    "$audit_scope_contract" \
    "$baseline_effective" \
    "$p0_merged_main_validation_status" \
    "$p1a_repository_edits_allowed" \
    "$p1a_implementation_allowed" \
    "$p1a_implementation_pr_allowed"

  resolve_task_handoff

  if [[ "$resolution_status" == "ALLOWED" ]]; then
    can_continue="YES"
  else
    can_continue="NO"
  fi

  printf 'BRANCH: %s\n' "$branch"
  printf 'WORKTREE_CLEAN: %s\n' "$worktree_clean"
  printf 'MAIN_SYNC: %s\n' "$main_sync"
  printf 'OPEN_PRS: %s\n' "$open_prs"
  printf 'OPEN_PR_CHECK_SOURCE: SELF_TEST\n'
  if [[ "$current_package_pr_count" == "UNKNOWN" || "$active_conflicting_pr_count" == "UNKNOWN" ]]; then
    printf 'OPEN_PR_COUNT: UNKNOWN\n'
    printf 'OPEN_PR_STATUS: UNKNOWN\n'
  elif [[ "$open_prs" == "none" ]]; then
    printf 'OPEN_PR_COUNT: 0\n'
    printf 'OPEN_PR_STATUS: NONE\n'
  else
    printf 'OPEN_PR_COUNT: 1\n'
    printf 'OPEN_PR_STATUS: OPEN\n'
  fi
  printf 'CURRENT_PACKAGE_PR_DRAFT: %s\n' "$current_package_pr_draft"
  printf 'CONTRACT_MATRIX_SYNC: OK\n'
  printf 'CURRENT_PHASE_STATUS: DONE\n'
  printf 'COMPLETION_EFFECTIVE_STATE: %s\n' "$completion_effective_state"
  printf 'P0_MERGED_MAIN_VALIDATION_STATUS: %s\n' "$p0_merged_main_validation_status"
  printf 'PRODUCT_SOURCE_GATE_STATUS: %s\n' "$product_source_gate_status"
  printf 'CAN_CONTINUE_NEXT_PACKAGE: %s\n' "$can_continue"
  printf 'BLOCKERS: %s\n' "$blockers_text"
  emit_resolved_task_state
}

if [[ -n "${V1_HANDOFF_SELF_TEST_SCENARIO:-}" ]]; then
  if [[ "${V1_WORKFLOW_SELF_TEST:-0}" != "1" ]]; then
    echo "handoff simulation requires V1_WORKFLOW_SELF_TEST=1" >&2
    exit 2
  fi
  run_handoff_resolution_simulation "$V1_HANDOFF_SELF_TEST_SCENARIO"
  exit 0
fi

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

required_contract_files=(
  "$CONTRACT_FILE"
  "$CURRENT_STATE_FILE"
  "$MATRIX_FILE"
  "docs/CODEX_TASK_TEMPLATE.md"
  "docs/CONTRACT_CHANGE_LOG.md"
  "docs/DEAD_CODE_CANDIDATES.md"
  "docs/PROJECT_GLOBAL_AUDIT.md"
  "$ACTIVE_FILE"
  "$TASK_FILE"
)
missing=()
for f in "${required_contract_files[@]}"; do
  [[ -f "$f" ]] || missing+=("$f")
done
if (( ${#missing[@]} > 0 )); then
  contract_files_present="NO"
  blockers+=("CONTRACT_FILES_MISSING:${missing[*]}")
else
  contract_files_present="YES"
fi

current_phase="$(matrix_field P0-0 2)"
current_phase_status="$(matrix_field P0-0 4)"
existing_module_maturity="$(matrix_field P0-0 5)"
product_p1b_matrix_status="$(matrix_field "Product P1B" 4)"
product_p2_matrix_status="$(matrix_field "Product P2" 5)"
product_v4_1_matrix_authorization="$(matrix_field "Product v4.1" 5)"
current_work_package="$(current_state_value "Current Work Package")"
next_business_phase="$(current_state_value "Next Business Phase")"
next_business_phase_allowed_raw="$(current_state_value "Next Business Phase Allowed")"
production_deployment_readiness="$(current_state_value "Production Deployment Readiness")"

[[ -n "$current_phase" ]] || current_phase="P0-0"
[[ -n "$current_phase_status" ]] || current_phase_status="UNKNOWN"
[[ -n "$existing_module_maturity" ]] || existing_module_maturity="UNKNOWN"
[[ -n "$current_work_package" ]] || current_work_package="UNKNOWN"
[[ -n "$next_business_phase" ]] || next_business_phase="UNKNOWN"
[[ -n "$production_deployment_readiness" ]] || production_deployment_readiness="UNKNOWN"

matrix_sync="OK"
cs_phase="$(current_state_value "Current Phase")"
cs_status="$(current_state_value "Current Phase Status")"
if [[ -n "$cs_phase" && "$cs_phase" != P0-0* ]]; then
  matrix_sync="CONFLICT"
  blockers+=("CONTRACT_STATE_CONFLICT")
fi
if [[ -n "$cs_status" && "$cs_status" != "$current_phase_status" ]]; then
  matrix_sync="CONFLICT"
  blockers+=("CONTRACT_STATE_CONFLICT")
fi

active_compat="$(yaml_value "$ACTIVE_FILE" compatibility_status)"
task_compat="$(yaml_value "$TASK_FILE" compatibility_status)"
active_phase="$(yaml_value "$ACTIVE_FILE" current_phase)"
active_status="$(yaml_value "$ACTIVE_FILE" current_phase_status)"
task_phase="$(yaml_value "$TASK_FILE" current_phase)"
active_allowed="$(yaml_value "$ACTIVE_FILE" next_business_phase_allowed)"
task_allowed="$(yaml_value "$TASK_FILE" next_business_phase_allowed)"
task_mode="$(yaml_value "$TASK_FILE" task_mode)"
current_task_mode="$(yaml_value "$TASK_FILE" current_task_mode)"
authorized_next_task_mode="$(yaml_value "$TASK_FILE" authorized_next_task_mode)"
declared_current_effective_status="$(yaml_value "$TASK_FILE" current_effective_status)"
authorized_next_product_phase="$(yaml_value "$TASK_FILE" authorized_next_product_phase)"
p1a_repository_edits_allowed="$(yaml_value "$TASK_FILE" p1a_repository_edits_allowed)"
p1a_implementation_allowed="$(yaml_value "$TASK_FILE" p1a_implementation_allowed)"
p1a_implementation_pr_allowed="$(yaml_value "$TASK_FILE" p1a_implementation_pr_allowed)"
p1b_1_declared_status="$(yaml_value "$TASK_FILE" p1b_1_status)"
p1b_authorization_declared_status="$(yaml_value "$TASK_FILE" p1b_authorization_status)"
home_core_data_declared_status="$(yaml_value "$TASK_FILE" p1b_home_core_data_authorization_status)"
home_core_data_implementation_status="$(yaml_value "$TASK_FILE" p1b_home_core_data_implementation_status)"
product_p1b_declared_status="$(yaml_value "$TASK_FILE" product_p1b_status)"
p2_authorization_declared_status="$(yaml_value "$TASK_FILE" p2_position_monitoring_authorization_status)"
p2_implementation_status="$(yaml_value "$TASK_FILE" p2_position_monitoring_implementation_status)"
v4_1_design_status="$(yaml_value "$TASK_FILE" v4_1_final_interaction_design_status)"
v4_1_authorization_declared_status="$(yaml_value "$TASK_FILE" v4_1_final_interaction_authorization_status)"
v4_1_implementation_status="$(yaml_value "$TASK_FILE" v4_1_final_interaction_implementation_status)"
v4_1_target_runtime_authorization_declared_status="$(yaml_value "$TASK_FILE" v4_1_target_runtime_remediation_authorization_status)"
v4_1_target_runtime_implementation_status="$(yaml_value "$TASK_FILE" v4_1_target_runtime_remediation_implementation_status)"
v4_1_target_runtime_status="$(yaml_value "$TASK_FILE" v4_1_target_runtime_status)"
v4_1_telegram_authorization_declared_status="$(yaml_value "$TASK_FILE" v4_1_telegram_authorization_status)"
v4_1_telegram_implementation_status="$(yaml_value "$TASK_FILE" v4_1_telegram_implementation_status)"
v4_1_telegram_live_acceptance_status="$(yaml_value "$TASK_FILE" v4_1_telegram_live_acceptance_status)"
v4_1_telegram_remediation_authorization_declared_status="$(yaml_value "$TASK_FILE" v4_1_telegram_remediation_authorization_status)"
v4_1_telegram_remediation_implementation_status="$(yaml_value "$TASK_FILE" v4_1_telegram_remediation_implementation_status)"
v4_1_core_production_loop_authorization_declared_status="$(yaml_value "$TASK_FILE" v4_1_core_production_loop_authorization_status)"
v4_1_core_production_loop_implementation_status="$(yaml_value "$TASK_FILE" v4_1_core_production_loop_implementation_status)"
v4_1_machine_gate_owner_amendment_declared_status="$(yaml_value "$TASK_FILE" v4_1_machine_gate_owner_amendment_status)"
real_data_home_blocker_closure_authorization_declared_status="$(yaml_value "$TASK_FILE" real_data_home_blocker_closure_authorization_status)"
real_data_home_blocker_closure_implementation_status="$(yaml_value "$TASK_FILE" real_data_home_blocker_closure_implementation_status)"
analysis_run_idempotency_tx_fix_authorization_declared_status="$(yaml_value "$TASK_FILE" analysis_run_idempotency_tx_fix_authorization_status)"
analysis_run_idempotency_tx_fix_implementation_status="$(yaml_value "$TASK_FILE" analysis_run_idempotency_tx_fix_implementation_status)"
final_runtime_home_closure_authorization_declared_status="$(yaml_value "$TASK_FILE" v4_1_final_runtime_home_access_idempotency_closure_authorization_status)"
final_runtime_home_closure_implementation_status="$(yaml_value "$TASK_FILE" v4_1_final_runtime_home_access_idempotency_closure_implementation_status)"
local_real_authorization_declared_status="$(yaml_value "$TASK_FILE" local_real_authorization_status)"
local_real_implementation_status="$(yaml_value "$TASK_FILE" local_real_implementation_status)"
frontend_interaction_authorization_declared_status="$(yaml_value "$TASK_FILE" frontend_interaction_authorization_status)"
frontend_interaction_implementation_status="$(yaml_value "$TASK_FILE" frontend_interaction_implementation_status)"
multi_user_authorization_declared_status="$(yaml_value "$TASK_FILE" multi_user_authorization_status)"
multi_user_implementation_status="$(yaml_value "$TASK_FILE" multi_user_implementation_status)"
authorized_next_package_alias="$(yaml_value "$TASK_FILE" authorized_next_package)"
p1b_scope="$(yaml_value "$TASK_FILE" scope)"
audit_scope_contract="$(yaml_value "$TASK_FILE" read_only_product_audit_scope_contract)"
audit_scope_modules="$(yaml_list "$TASK_FILE" audit_scope_modules)"
audit_scope_paths="$(yaml_list "$TASK_FILE" audit_scope_paths)"
audit_scope_source_domains="$(yaml_list "$TASK_FILE" audit_scope_source_domains)"
closed_technical_debt_pr="$(yaml_value "$TASK_FILE" paused_governance_pr)"
closed_technical_debt_status="$(yaml_value "$TASK_FILE" paused_governance_status)"
closed_technical_debt_merged_status="$(yaml_value "$TASK_FILE" paused_governance_merged_status)"

load_task_package_contract
if [[ "$current_task_mode" != "$current_package_mode" \
  || "$authorized_next_task_mode" != "$authorized_next_package_mode" \
  || "$authorized_next_product_phase" != "$authorized_next_package_phase" \
  || "$authorized_next_package_alias" != "$authorized_next_package_phase" ]]; then
  blockers+=("TASK_PACKAGE_DECLARATION_CONFLICT")
elif ! is_false_flag "$p1a_repository_edits_allowed" \
  || ! is_false_flag "$p1a_implementation_allowed" \
  || ! is_false_flag "$p1a_implementation_pr_allowed"; then
  blockers+=("TASK_PACKAGE_DECLARATION_CONFLICT")
fi
if [[ "$current_package_phase" == "P1A_HOME_ALIGNMENT_READINESS_AND_GAP_AUDIT" ]]; then
  if [[ "$current_package_status" != "COMPLETED" \
    || "$authorized_next_package_phase" != "P1B_HOME_ALIGNMENT_FIRST_IMPLEMENTATION" \
    || "$authorized_next_package_mode" != "IMPLEMENTATION" \
    || "$p1b_authorization_declared_status" != "EFFECTIVE_PENDING_MERGED_MAIN" \
    || "$p1b_scope" != "HOME_READ_PROJECTION_ONLY" ]]; then
    blockers+=("TASK_PACKAGE_DECLARATION_CONFLICT")
  elif ! is_true_flag "$authorized_next_repository_edits_allowed" \
    || ! is_true_flag "$authorized_next_implementation_allowed" \
    || ! is_true_flag "$authorized_next_implementation_pr_allowed"; then
    blockers+=("TASK_PACKAGE_DECLARATION_CONFLICT")
  fi
fi
if [[ "$current_package_phase" == "P1B_HOME_CORE_DATA_AUTHORIZATION" ]]; then
  if [[ "$current_package_status" != "COMPLETED" \
    || "$current_package_mode" != "BOUNDED_PRODUCT_DECISION_AND_AUTHORIZATION" \
    || "$authorized_next_package_phase" != "P1B_HOME_CORE_DATA_COMPLETION" \
    || "$authorized_next_package_mode" != "IMPLEMENTATION" \
    || "$p1b_1_declared_status" != "EFFECTIVE_MERGED_MAIN" \
    || "$p1b_authorization_declared_status" != "EFFECTIVE_MERGED_MAIN" \
    || "$home_core_data_declared_status" != "AUTHORIZED_PENDING_MERGED_MAIN" \
    || "$home_core_data_implementation_status" != "NOT_STARTED" \
    || "$p1b_scope" != "HOME_CORE_DATA_READ_ONLY" ]]; then
    blockers+=("TASK_PACKAGE_DECLARATION_CONFLICT")
  elif ! is_true_flag "$authorized_next_repository_edits_allowed" \
    || ! is_true_flag "$authorized_next_implementation_allowed" \
    || ! is_true_flag "$authorized_next_implementation_pr_allowed"; then
    blockers+=("TASK_PACKAGE_DECLARATION_CONFLICT")
  fi
fi
if [[ "$current_package_phase" == "P2_POSITION_MONITORING_AUTHORIZATION" ]]; then
  if [[ "$current_package_status" != "COMPLETED" \
    || "$current_package_mode" != "BOUNDED_PRODUCT_DECISION_AND_AUTHORIZATION" \
    || "$authorized_next_package_phase" != "P2_POSITION_MONITORING_BACKEND_IMPLEMENTATION" \
    || "$authorized_next_package_mode" != "IMPLEMENTATION" \
    || "$product_p1b_declared_status" != "COMPLETE" \
    || "$product_p1b_matrix_status" != "COMPLETE" \
    || "$product_p2_matrix_status" != "AUTHORIZED_TO_IMPLEMENT" \
    || "$p2_authorization_declared_status" != "AUTHORIZED_PENDING_MERGED_MAIN" \
    || "$p2_implementation_status" != "NOT_STARTED" \
    || "$p1b_1_declared_status" != "EFFECTIVE_MERGED_MAIN" \
    || "$p1b_authorization_declared_status" != "EFFECTIVE_MERGED_MAIN" \
    || "$home_core_data_declared_status" != "EFFECTIVE_MERGED_MAIN" \
    || "$home_core_data_implementation_status" != "COMPLETE" \
    || "$p1b_scope" != "POSITION_MONITORING_BACKEND_CONTRACT_ONLY" ]]; then
    blockers+=("TASK_PACKAGE_DECLARATION_CONFLICT")
  elif ! is_true_flag "$authorized_next_repository_edits_allowed" \
    || ! is_true_flag "$authorized_next_implementation_allowed" \
    || ! is_true_flag "$authorized_next_implementation_pr_allowed"; then
    blockers+=("TASK_PACKAGE_DECLARATION_CONFLICT")
  fi
fi
if [[ "$current_package_phase" == "FUNDAMENTAL_AI_V4_1_TARGET_RUNTIME_BLOCKER_REMEDIATION_AUTHORIZATION" ]]; then
  if [[ "$current_package_status" != "COMPLETED" \
    || "$current_package_mode" != "BOUNDED_PRODUCT_DECISION_AND_AUTHORIZATION" \
    || "$authorized_next_package_phase" != "FUNDAMENTAL_AI_V4_1_TARGET_RUNTIME_BLOCKER_REMEDIATION" \
    || "$authorized_next_package_mode" != "IMPLEMENTATION" \
    || "$v4_1_design_status" != "FROZEN" \
    || "$product_v4_1_matrix_authorization" != "AUTHORIZED_TO_IMPLEMENT" \
    || "$v4_1_authorization_declared_status" != "EFFECTIVE_MERGED_MAIN" \
    || "$v4_1_implementation_status" != "COMPLETE" \
    || "$v4_1_target_runtime_authorization_declared_status" != "AUTHORIZED_PENDING_MERGED_MAIN" \
    || "$v4_1_target_runtime_implementation_status" != "NOT_STARTED" \
    || "$v4_1_target_runtime_status" != "BLOCKED_BY_IMPLEMENTATION_DEFECT" \
    || "$p1b_scope" != "V4_1_TARGET_RUNTIME_BLOCKER_REMEDIATION_ONLY" ]]; then
    blockers+=("TASK_PACKAGE_DECLARATION_CONFLICT")
  elif ! is_true_flag "$authorized_next_repository_edits_allowed" \
    || ! is_true_flag "$authorized_next_implementation_allowed" \
    || ! is_true_flag "$authorized_next_implementation_pr_allowed" \
    || ! is_false_flag "$authorized_next_canonical_figma_desktop_implementation_allowed" \
    || ! is_false_flag "$authorized_next_mobile_implementation_allowed" \
    || [[ "$authorized_next_canonical_figma_file_key" != "NONE" ]]; then
    blockers+=("TASK_PACKAGE_DECLARATION_CONFLICT")
  fi
fi
if [[ "$current_package_phase" == "FUNDAMENTAL_AI_V4_1_TELEGRAM_HIGH_VALUE_ALERT_CHANNEL_AUTHORIZATION" ]]; then
  if [[ "$current_package_status" != "COMPLETED" \
    || "$current_package_mode" != "BOUNDED_PRODUCT_DECISION_AND_AUTHORIZATION" \
    || "$authorized_next_package_phase" != "FUNDAMENTAL_AI_V4_1_TELEGRAM_HIGH_VALUE_ALERT_CHANNEL_INTEGRATION" \
    || "$authorized_next_package_mode" != "IMPLEMENTATION" \
    || "$v4_1_design_status" != "FROZEN" \
    || "$product_v4_1_matrix_authorization" != "AUTHORIZED_TO_IMPLEMENT" \
    || "$v4_1_authorization_declared_status" != "EFFECTIVE_MERGED_MAIN" \
    || "$v4_1_implementation_status" != "COMPLETE" \
    || "$v4_1_target_runtime_authorization_declared_status" != "EFFECTIVE_MERGED_MAIN" \
    || "$v4_1_target_runtime_implementation_status" != "COMPLETE" \
    || "$v4_1_telegram_authorization_declared_status" != "AUTHORIZED_PENDING_MERGED_MAIN" \
    || "$v4_1_telegram_implementation_status" != "NOT_STARTED" \
    || "$p1b_scope" != "V4_1_TELEGRAM_HIGH_VALUE_ALERT_CHANNEL_ONLY" ]]; then
    blockers+=("TASK_PACKAGE_DECLARATION_CONFLICT")
  elif ! is_true_flag "$authorized_next_repository_edits_allowed" \
    || ! is_true_flag "$authorized_next_implementation_allowed" \
    || ! is_true_flag "$authorized_next_implementation_pr_allowed" \
    || ! is_false_flag "$authorized_next_canonical_figma_desktop_implementation_allowed" \
    || ! is_false_flag "$authorized_next_mobile_implementation_allowed" \
    || [[ "$authorized_next_canonical_figma_file_key" != "NONE" ]]; then
    blockers+=("TASK_PACKAGE_DECLARATION_CONFLICT")
  fi
fi
if [[ "$current_package_phase" == "LOCAL_REAL_READINESS_SYNC_AND_REAL_ANALYSIS_ENABLEMENT_AUTHORIZATION" ]]; then
  if [[ "$current_package_status" != "COMPLETED" \
    || "$current_package_mode" != "BOUNDED_PRODUCT_DECISION_AND_AUTHORIZATION" \
    || "$authorized_next_package_phase" != "LOCAL_REAL_READINESS_SYNC_AND_REAL_ANALYSIS_ENABLEMENT" \
    || "$authorized_next_package_mode" != "IMPLEMENTATION" \
    || "$v4_1_design_status" != "FROZEN" \
    || "$product_v4_1_matrix_authorization" != "AUTHORIZED_TO_IMPLEMENT" \
    || "$v4_1_authorization_declared_status" != "EFFECTIVE_MERGED_MAIN" \
    || "$v4_1_implementation_status" != "COMPLETE" \
    || "$v4_1_target_runtime_authorization_declared_status" != "EFFECTIVE_MERGED_MAIN" \
    || "$v4_1_target_runtime_implementation_status" != "COMPLETE" \
    || "$v4_1_telegram_authorization_declared_status" != "EFFECTIVE_MERGED_MAIN" \
    || "$v4_1_telegram_implementation_status" != "COMPLETE" \
    || "$local_real_authorization_declared_status" != "AUTHORIZED_PENDING_MERGED_MAIN" \
    || "$local_real_implementation_status" != "NOT_STARTED" \
    || "$p1b_scope" != "LOCAL_REAL_READINESS_AND_CURRENT_HOME_BINDING_ONLY" ]]; then
    blockers+=("TASK_PACKAGE_DECLARATION_CONFLICT")
  elif ! is_true_flag "$authorized_next_repository_edits_allowed" \
    || ! is_true_flag "$authorized_next_implementation_allowed" \
    || ! is_true_flag "$authorized_next_implementation_pr_allowed" \
    || ! is_false_flag "$authorized_next_canonical_figma_desktop_implementation_allowed" \
    || ! is_false_flag "$authorized_next_mobile_implementation_allowed" \
    || [[ "$authorized_next_canonical_figma_file_key" != "NONE" ]]; then
    blockers+=("TASK_PACKAGE_DECLARATION_CONFLICT")
  fi
fi
if [[ "$current_package_phase" == "FRONTEND_INTERACTION_RUNTIME_CLOSURE_AUTHORIZATION" ]]; then
  if [[ "$current_package_status" != "COMPLETED" \
    || "$current_package_mode" != "BOUNDED_PRODUCT_DECISION_AND_AUTHORIZATION" \
    || "$authorized_next_package_phase" != "FRONTEND_INTERACTION_RUNTIME_CLOSURE" \
    || "$authorized_next_package_mode" != "IMPLEMENTATION" \
    || "$v4_1_design_status" != "FROZEN" \
    || "$product_v4_1_matrix_authorization" != "AUTHORIZED_TO_IMPLEMENT" \
    || "$v4_1_authorization_declared_status" != "EFFECTIVE_MERGED_MAIN" \
    || "$v4_1_implementation_status" != "COMPLETE" \
    || "$v4_1_target_runtime_authorization_declared_status" != "EFFECTIVE_MERGED_MAIN" \
    || "$v4_1_target_runtime_implementation_status" != "COMPLETE" \
    || "$v4_1_telegram_authorization_declared_status" != "EFFECTIVE_MERGED_MAIN" \
    || "$v4_1_telegram_implementation_status" != "COMPLETE" \
    || "$local_real_authorization_declared_status" != "EFFECTIVE_MERGED_MAIN" \
    || "$local_real_implementation_status" != "COMPLETE" \
    || "$frontend_interaction_authorization_declared_status" != "AUTHORIZED_PENDING_MERGED_MAIN" \
    || "$frontend_interaction_implementation_status" != "NOT_STARTED" \
    || "$p1b_scope" != "DESKTOP_RUNTIME_INTERACTION_CLOSURE_ONLY" ]]; then
    blockers+=("TASK_PACKAGE_DECLARATION_CONFLICT")
  elif ! is_true_flag "$authorized_next_repository_edits_allowed" \
    || ! is_true_flag "$authorized_next_implementation_allowed" \
    || ! is_true_flag "$authorized_next_implementation_pr_allowed" \
    || ! is_false_flag "$authorized_next_canonical_figma_desktop_implementation_allowed" \
    || ! is_false_flag "$authorized_next_mobile_implementation_allowed" \
    || [[ "$authorized_next_canonical_figma_file_key" != "NONE" ]]; then
    blockers+=("TASK_PACKAGE_DECLARATION_CONFLICT")
  fi
fi
if [[ "$current_package_phase" == "FUNDAMENTAL_AI_V4_1_CORE_PRODUCTION_LOOP_AUTOMATION_AUTHORIZATION" ]]; then
  if [[ "$current_package_status" != "COMPLETED" \
    || "$current_package_mode" != "BOUNDED_PRODUCT_DECISION_AND_AUTHORIZATION" \
    || "$authorized_next_package_phase" != "FUNDAMENTAL_AI_V4_1_CORE_PRODUCTION_LOOP_AUTOMATION" \
    || "$authorized_next_package_mode" != "IMPLEMENTATION" \
    || "$v4_1_design_status" != "FROZEN" \
    || "$v4_1_implementation_status" != "COMPLETE" \
    || "$v4_1_telegram_remediation_authorization_declared_status" != "EFFECTIVE_MERGED_MAIN" \
    || "$v4_1_telegram_remediation_implementation_status" != "NOT_STARTED" \
    || "$v4_1_core_production_loop_authorization_declared_status" != "AUTHORIZED_PENDING_MERGED_MAIN" \
    || "$v4_1_core_production_loop_implementation_status" != "NOT_STARTED" \
    || "$p2_implementation_status" != "COMPLETE" \
    || "$p1b_scope" != "V4_1_CORE_PRODUCTION_LOOP_AUTOMATION_ONLY" ]]; then
    blockers+=("TASK_PACKAGE_DECLARATION_CONFLICT")
  elif ! is_true_flag "$authorized_next_repository_edits_allowed" \
    || ! is_true_flag "$authorized_next_implementation_allowed" \
    || ! is_true_flag "$authorized_next_implementation_pr_allowed" \
    || ! is_false_flag "$authorized_next_canonical_figma_desktop_implementation_allowed" \
    || ! is_false_flag "$authorized_next_mobile_implementation_allowed" \
    || [[ "$authorized_next_canonical_figma_file_key" != "NONE" ]]; then
    blockers+=("TASK_PACKAGE_DECLARATION_CONFLICT")
  fi
fi
if [[ "$current_package_phase" == "TRINE_LOGIC_V4_1_BASELINE_RECONCILIATION_GATE" ]]; then
  if [[ "$current_package_status" != "COMPLETED" \
    || "$current_package_mode" != "DOCS_GATE_BASELINE_RECONCILIATION" \
    || "$current_package_branch" != "codex/v4-1-baseline-reconciliation-gate" \
    || "$current_package_starting_full_sha" != "08abe1f1040df0d4242a01cc306867ad5d3b4782" \
    || "$authorized_next_package_phase" != "REAL_DATA_HOME_BLOCKER_CLOSURE" \
    || "$authorized_next_package_mode" != "IMPLEMENTATION" \
    || "$authorized_next_package_branch" != "codex/v4-1-real-data-home-blocker-closure" \
    || "$authorized_next_package_starting_full_sha" != "a60eff8d83c0e1d04371bd425267f1e8d0e4f95c" \
    || "$authorized_next_normalization_source_parent_sha" != "a60eff8d83c0e1d04371bd425267f1e8d0e4f95c" \
    || "$authorized_next_normalization_expected_source" != "MERGED_MAIN" \
    || "$authorized_next_normalization_extra_file_count" != "0" \
    || "$authorized_next_normalization_one_time_only" != "true" \
    || "$v4_1_core_production_loop_authorization_declared_status" != "EFFECTIVE_MERGED_MAIN" \
    || "$v4_1_machine_gate_owner_amendment_declared_status" != "EFFECTIVE_MERGED_MAIN" \
    || "$v4_1_baseline_reconciliation_gate_declared_status" != "AUTHORIZED_PENDING_MERGED_MAIN" \
    || "$real_data_home_blocker_closure_authorization_declared_status" != "PENDING_BASELINE_RECONCILIATION_MERGED_MAIN" \
    || "$real_data_home_blocker_closure_implementation_status" != "NOT_STARTED" \
    || "$p1b_scope" != "REAL_DATA_HOME_BLOCKER_CLOSURE_ONLY" ]]; then
    blockers+=("TASK_PACKAGE_DECLARATION_CONFLICT")
  elif ! is_true_flag "$current_package_repository_edits_allowed" \
    || ! is_false_flag "$current_package_implementation_allowed" \
    || ! is_true_flag "$current_package_implementation_pr_allowed" \
    || ! is_true_flag "$current_package_push_allowed" \
    || ! is_true_flag "$current_package_merge_allowed" \
    || ! is_false_flag "$current_package_deployment_allowed" \
    || ! is_true_flag "$authorized_next_repository_edits_allowed" \
    || ! is_true_flag "$authorized_next_implementation_allowed" \
    || ! is_true_flag "$authorized_next_implementation_pr_allowed" \
    || ! is_true_flag "$authorized_next_push_allowed" \
    || ! is_true_flag "$authorized_next_merge_allowed" \
    || ! is_false_flag "$authorized_next_deployment_allowed" \
    || ! is_false_flag "$authorized_next_canonical_figma_desktop_implementation_allowed" \
    || ! is_false_flag "$authorized_next_mobile_implementation_allowed" \
    || [[ "$authorized_next_canonical_figma_file_key" != "NONE" ]] \
    || ! is_full_git_sha "$current_package_starting_full_sha" \
    || ! is_full_git_sha "$authorized_next_package_starting_full_sha"; then
    blockers+=("TASK_PACKAGE_DECLARATION_CONFLICT")
  fi
fi
if [[ "$current_package_phase" == "TRINE_LOGIC_V4_1_FINAL_RUNTIME_HOME_ACCESS_IDEMPOTENCY_CLOSURE_AUTHORIZATION" ]]; then
  if [[ "$current_package_status" != "COMPLETED" \
    || "$current_package_mode" != "DOCS_GATE_BASELINE_RECONCILIATION" \
    || "$current_package_branch" != "codex/v4-1-final-runtime-home-access-idempotency-closure-authorization" \
    || "$current_package_starting_full_sha" != "0e9bd779b10e9d3140b8ceaea0a5193a28d6264f" \
    || "$authorized_next_package_phase" != "V41_FINAL_RUNTIME_HOME_ACCESS_IDEMPOTENCY_CLOSURE" \
    || "$authorized_next_package_mode" != "IMPLEMENTATION" \
    || "$authorized_next_package_branch" != "codex/v4-1-final-runtime-home-access-idempotency-closure" \
    || "$authorized_next_package_starting_full_sha" != "0e9bd779b10e9d3140b8ceaea0a5193a28d6264f" \
    || "$analysis_run_idempotency_tx_fix_authorization_declared_status" != "EFFECTIVE_MERGED_MAIN" \
    || "$analysis_run_idempotency_tx_fix_implementation_status" != "COMPLETE" \
    || "$final_runtime_home_closure_authorization_declared_status" != "AUTHORIZED_PENDING_MERGED_MAIN" \
    || "$final_runtime_home_closure_implementation_status" != "NOT_STARTED" \
    || "$real_data_home_blocker_closure_authorization_declared_status" != "EFFECTIVE_MERGED_MAIN" \
    || "$real_data_home_blocker_closure_implementation_status" != "COMPLETE" \
    || "$p1b_scope" != "V41_FINAL_RUNTIME_HOME_ACCESS_IDEMPOTENCY_CLOSURE_ONLY" ]]; then
    blockers+=("TASK_PACKAGE_DECLARATION_CONFLICT")
  elif ! is_true_flag "$current_package_repository_edits_allowed" \
    || ! is_false_flag "$current_package_implementation_allowed" \
    || ! is_true_flag "$current_package_implementation_pr_allowed" \
    || ! is_true_flag "$current_package_push_allowed" \
    || ! is_true_flag "$current_package_merge_allowed" \
    || ! is_false_flag "$current_package_deployment_allowed" \
    || ! is_true_flag "$authorized_next_repository_edits_allowed" \
    || ! is_true_flag "$authorized_next_implementation_allowed" \
    || ! is_true_flag "$authorized_next_implementation_pr_allowed" \
    || ! is_true_flag "$authorized_next_push_allowed" \
    || ! is_true_flag "$authorized_next_merge_allowed" \
    || ! is_false_flag "$authorized_next_deployment_allowed" \
    || ! is_false_flag "$authorized_next_canonical_figma_desktop_implementation_allowed" \
    || ! is_false_flag "$authorized_next_mobile_implementation_allowed" \
    || [[ "$authorized_next_canonical_figma_file_key" != "NONE" ]] \
    || ! is_full_git_sha "$current_package_starting_full_sha" \
    || ! is_full_git_sha "$authorized_next_package_starting_full_sha" \
    || [[ "$(printf '%s\n' "$authorized_next_package_allowed_paths" | awk 'NF {count++} END {print count+0}')" != "6" ]]; then
    blockers+=("TASK_PACKAGE_DECLARATION_CONFLICT")
  fi
fi
if [[ "$current_package_phase" == "FUNDAMENTAL_AI_V4_1_TELEGRAM_HIGH_VALUE_ALERT_CHANNEL_REMEDIATION_AUTHORIZATION" ]]; then
  if [[ "$current_package_status" != "COMPLETED" \
    || "$current_package_mode" != "BOUNDED_PRODUCT_DECISION_AND_AUTHORIZATION" \
    || "$authorized_next_package_phase" != "FUNDAMENTAL_AI_V4_1_TELEGRAM_HIGH_VALUE_ALERT_CHANNEL_INTEGRATION" \
    || "$authorized_next_package_mode" != "IMPLEMENTATION" \
    || "$v4_1_design_status" != "FROZEN" \
    || "$v4_1_telegram_authorization_declared_status" != "EFFECTIVE_MERGED_MAIN" \
    || "$v4_1_telegram_implementation_status" != "COMPLETE" \
    || "$v4_1_telegram_remediation_authorization_declared_status" != "AUTHORIZED_PENDING_MERGED_MAIN" \
    || "$v4_1_telegram_remediation_implementation_status" != "NOT_STARTED" \
    || "$multi_user_authorization_declared_status" != "EFFECTIVE_MERGED_MAIN" \
    || "$p1b_scope" != "V4_1_TELEGRAM_TWO_CATEGORY_REMEDIATION_ONLY" ]]; then
    blockers+=("TASK_PACKAGE_DECLARATION_CONFLICT")
  elif ! is_true_flag "$authorized_next_repository_edits_allowed" \
    || ! is_true_flag "$authorized_next_implementation_allowed" \
    || ! is_true_flag "$authorized_next_implementation_pr_allowed" \
    || ! is_false_flag "$authorized_next_canonical_figma_desktop_implementation_allowed" \
    || ! is_false_flag "$authorized_next_mobile_implementation_allowed" \
    || [[ "$authorized_next_canonical_figma_file_key" != "NONE" ]]; then
    blockers+=("TASK_PACKAGE_DECLARATION_CONFLICT")
  fi
fi
if [[ "$current_package_phase" == "MULTI_USER_ACCOUNT_REGISTRATION_CLOSURE_AUTHORIZATION" ]]; then
  if [[ "$current_package_status" != "COMPLETED" \
    || "$current_package_mode" != "BOUNDED_PRODUCT_DECISION_AND_AUTHORIZATION" \
    || "$authorized_next_package_phase" != "MULTI_USER_ACCOUNT_REGISTRATION_CLOSURE" \
    || "$authorized_next_package_mode" != "IMPLEMENTATION" \
    || "$v4_1_design_status" != "FROZEN" \
    || "$frontend_interaction_authorization_declared_status" != "EFFECTIVE_MERGED_MAIN" \
    || "$frontend_interaction_implementation_status" != "COMPLETE" \
    || "$multi_user_authorization_declared_status" != "AUTHORIZED_PENDING_MERGED_MAIN" \
    || "$multi_user_implementation_status" != "NOT_STARTED" \
    || "$p1b_scope" != "PRIVATE_MULTI_USER_REGISTRATION_AND_DATA_ISOLATION_ONLY" ]]; then
    blockers+=("TASK_PACKAGE_DECLARATION_CONFLICT")
  elif ! is_true_flag "$authorized_next_repository_edits_allowed" \
    || ! is_true_flag "$authorized_next_implementation_allowed" \
    || ! is_true_flag "$authorized_next_implementation_pr_allowed" \
    || ! is_false_flag "$authorized_next_canonical_figma_desktop_implementation_allowed" \
    || ! is_false_flag "$authorized_next_mobile_implementation_allowed" \
    || [[ "$authorized_next_canonical_figma_file_key" != "NONE" ]]; then
    blockers+=("TASK_PACKAGE_DECLARATION_CONFLICT")
  fi
fi
current_task_mode="$current_package_mode"
authorized_next_task_mode="$authorized_next_package_mode"

product_source_gate_status="BLOCKED"
if bash scripts/product-source-gate.sh --task-file "$TASK_FILE" >/dev/null 2>&1; then
  product_source_gate_status="PASS"
fi

if [[ "$active_compat" != "DERIVED_ONLY" || "$task_compat" != "DERIVED_ONLY" ]]; then
  blockers+=("COMPATIBILITY_FACT_CONFLICT")
fi
if [[ -n "$active_phase" && "$active_phase" != "P0-0" ]]; then
  blockers+=("COMPATIBILITY_FACT_CONFLICT")
fi
if [[ -n "$active_status" && "$active_status" != "$current_phase_status" ]]; then
  blockers+=("COMPATIBILITY_FACT_CONFLICT")
fi
if [[ -n "$task_phase" && "$task_phase" != "P0-0" ]]; then
  blockers+=("COMPATIBILITY_FACT_CONFLICT")
fi

branch="$(git branch --show-current)"
status_short="$(git status --short)"
head_commit="$(git log -1 --oneline)"

head_matches_origin_main="UNKNOWN"
if git rev-parse --verify HEAD >/dev/null 2>&1 && git rev-parse --verify origin/main >/dev/null 2>&1; then
  head_sha="$(git rev-parse HEAD)"
  origin_main_sha="$(git rev-parse origin/main)"
  if [[ "$head_sha" == "$origin_main_sha" ]]; then
    head_matches_origin_main="YES"
  else
    head_matches_origin_main="NO"
  fi
fi

if [[ "$branch" == "main" ]]; then
  on_main_branch="YES"
else
  on_main_branch="NO"
fi

if [[ -z "$status_short" ]]; then
  worktree_clean="Yes"
else
  worktree_clean="No"
  blockers+=("WORKTREE_DIRTY")
fi

main_sync="UNKNOWN"
if [[ "$head_matches_origin_main" == "YES" ]]; then
  main_sync="OK"
elif git rev-parse --verify main >/dev/null 2>&1 && git rev-parse --verify origin/main >/dev/null 2>&1; then
  read -r main_ahead main_behind < <(git rev-list --left-right --count main...origin/main)
  if [[ "$main_behind" == "0" && "$main_ahead" == "0" ]]; then
    main_sync="OK"
  elif [[ "$main_behind" != "0" ]]; then
    main_sync="BEHIND_ORIGIN_MAIN by $main_behind"
    blockers+=("MAIN_BEHIND_ORIGIN")
  else
    main_sync="AHEAD_ORIGIN_MAIN by $main_ahead"
  fi
else
  main_sync="UNKNOWN_ORIGIN_MAIN"
  blockers+=("MAIN_SYNC_UNKNOWN")
fi

head_in_origin_main="UNKNOWN"
if git rev-parse --verify HEAD >/dev/null 2>&1 && git rev-parse --verify origin/main >/dev/null 2>&1; then
  if git merge-base --is-ancestor HEAD origin/main; then
    head_in_origin_main="YES"
  else
    head_in_origin_main="NO"
  fi
else
  blockers+=("HEAD_ORIGIN_MAIN_COMPARE_UNKNOWN")
fi

completion_effective_state="NOT_DONE"
if [[ "$current_phase_status" == "DONE" ]]; then
  if [[ "$head_in_origin_main" == "NO" ]]; then
    completion_effective_state="PENDING_MERGED_MAIN"
  elif [[ "$head_in_origin_main" == "YES" && "$worktree_clean" == "Yes" && "$main_sync" == "OK" ]]; then
    completion_effective_state="EFFECTIVE_MERGED_MAIN"
  else
    completion_effective_state="NOT_DONE"
  fi
fi

[[ -n "$current_package_branch" ]] || current_package_branch="$branch"

open_pr_check_source="not_checked"
open_pr_evidence_source="UNAVAILABLE"
open_pr_count="UNKNOWN"
open_pr_status="UNKNOWN"
open_prs="none"
current_package_pr="none"
unrelated_open_prs="none"
active_conflicting_open_prs="none"
authorized_successor_prs="none"
current_package_pr_count="0"
current_package_pr_draft="NONE"
active_conflicting_pr_count="0"
authorized_successor_pr_count="0"
block_next_business_phase_only="NO"
if command -v gh >/dev/null 2>&1 && gh auth status >/dev/null 2>&1; then
  open_pr_check_source="gh CLI"
  if ! pr_rows="$(gh pr list --state open --json number,title,headRefName,headRefOid,isDraft --jq '.[] | [.number, .headRefName, .headRefOid, .title, .isDraft] | @tsv' 2>/dev/null)"; then
    pr_rows=""
    if [[ "$open_pr_none_confirmed" == "YES" \
      && "$open_pr_evidence_input_valid" == "YES" \
      && "$completion_effective_state" == "EFFECTIVE_MERGED_MAIN" ]]; then
      open_pr_check_source="explicit confirmed"
      open_pr_evidence_source="EXPLICIT_CONFIRMED"
      open_pr_count="0"
      open_pr_status="NONE"
    else
      open_pr_evidence_source="UNAVAILABLE"
      open_prs="GH_NOT_AVAILABLE"
      current_package_pr="GH_NOT_AVAILABLE"
      unrelated_open_prs="GH_NOT_AVAILABLE"
      active_conflicting_open_prs="GH_NOT_AVAILABLE"
      current_package_pr_count="UNKNOWN"
      current_package_pr_draft="UNKNOWN"
      active_conflicting_pr_count="UNKNOWN"
      open_pr_status="UNKNOWN"
      blockers+=("OPEN_PR_STATUS_UNKNOWN_GH_NOT_AVAILABLE")
    fi
  else
    open_pr_evidence_source="GH_QUERY"
    open_pr_count="0"
    open_pr_status="NONE"
  fi
  if [[ -n "${pr_rows:-}" ]]; then
    open_pr_status="OPEN"
    open_pr_lines=()
    current_package_pr_lines=()
    unrelated_open_pr_lines=()
    active_conflicting_open_pr_lines=()
    authorized_successor_pr_lines=()
    while IFS=$'\t' read -r pr_number pr_head pr_oid pr_title pr_draft; do
      [[ -z "${pr_number:-}" ]] && continue
      ((open_pr_count+=1))
      pr_line="#$pr_number $pr_head head=$pr_oid $pr_title draft=$pr_draft"
      open_pr_lines+=("$pr_line")
      if [[ "$pr_head" == "$current_package_branch" || "$pr_head" == "$branch" ]]; then
        current_package_pr_lines+=("$pr_line")
        ((current_package_pr_count+=1))
        current_package_pr_draft="$pr_draft"
      elif [[ "$pr_head" == "$authorized_next_package_branch" ]]; then
        authorized_successor_pr_lines+=("$pr_line status=AUTHORIZED_SUCCESSOR_PR")
        ((authorized_successor_pr_count+=1))
      else
        unrelated_open_pr_lines+=("$pr_line")
        block_next_business_phase_only="YES"
        active_conflicting_open_pr_lines+=("$pr_line status=ACTIVE_CONFLICTING_PR")
        ((active_conflicting_pr_count+=1))
        blockers+=("ACTIVE_CONFLICTING_PR_${pr_number}_BLOCKS_NEXT_BUSINESS_PHASE")
      fi
    done <<<"$pr_rows"
    open_prs="$(printf '%s\n' "${open_pr_lines[@]}")"
    if (( ${#current_package_pr_lines[@]} > 0 )); then
      current_package_pr="$(printf '%s\n' "${current_package_pr_lines[@]}")"
    fi
    if (( ${#unrelated_open_pr_lines[@]} > 0 )); then
      unrelated_open_prs="$(printf '%s\n' "${unrelated_open_pr_lines[@]}")"
    fi
    if (( ${#active_conflicting_open_pr_lines[@]} > 0 )); then
      active_conflicting_open_prs="$(printf '%s\n' "${active_conflicting_open_pr_lines[@]}")"
    fi
    if (( ${#authorized_successor_pr_lines[@]} > 0 )); then
      authorized_successor_prs="$(printf '%s\n' "${authorized_successor_pr_lines[@]}")"
    fi
  fi
else
  open_pr_check_source="unavailable"
  if [[ "$open_pr_none_confirmed" == "YES" \
    && "$open_pr_evidence_input_valid" == "YES" \
    && "$completion_effective_state" == "EFFECTIVE_MERGED_MAIN" ]]; then
    open_pr_check_source="explicit confirmed"
    open_pr_evidence_source="EXPLICIT_CONFIRMED"
    open_pr_count="0"
    open_pr_status="NONE"
  else
    open_pr_evidence_source="UNAVAILABLE"
    open_prs="GH_NOT_AVAILABLE"
    current_package_pr="GH_NOT_AVAILABLE"
    unrelated_open_prs="GH_NOT_AVAILABLE"
    active_conflicting_open_prs="GH_NOT_AVAILABLE"
    current_package_pr_count="UNKNOWN"
    current_package_pr_draft="UNKNOWN"
    active_conflicting_pr_count="UNKNOWN"
    open_pr_status="UNKNOWN"
    blockers+=("OPEN_PR_STATUS_UNKNOWN_GH_NOT_AVAILABLE")
  fi
fi

# Phase gate.
next_business_phase_allowed="NO"
can_start_next_business_phase="NO"
p0_0_effective="NO"
clean_synced_main="NO"
test_evidence="$(matrix_field P0-0 7)"
if [[ "$worktree_clean" == "Yes" && "$main_sync" == "OK" ]] && { [[ "$branch" == "main" ]] || [[ "$head_matches_origin_main" == "YES" ]]; }; then
  clean_synced_main="YES"
fi
if [[ "$current_phase_status" == "DONE" && "$completion_effective_state" == "EFFECTIVE_MERGED_MAIN" ]]; then
  p0_0_effective="YES"
  # Require merged/synced main, clean main, no open PR, contract sync, and matrix test evidence.
  if [[ "$clean_synced_main" == "YES" && "$current_package_pr" == "none" && "$unrelated_open_prs" == "none" && "$open_pr_status" == "NONE" && "$matrix_sync" == "OK" && "$test_evidence" != "None" && "$test_evidence" != "Pending" ]]; then
    next_business_phase_allowed="YES"
    can_start_next_business_phase="YES"
  else
    blockers+=("NEXT_BUSINESS_PHASE_GATE_CLOSED")
  fi
elif [[ "$current_phase_status" == "DONE" && "$completion_effective_state" == "PENDING_MERGED_MAIN" ]]; then
  blockers+=("P0_0_DONE_PENDING_MERGED_MAIN")
elif [[ "$current_phase_status" == "DONE" ]]; then
  blockers+=("P0_0_DONE_NOT_EFFECTIVE_MERGED_MAIN")
else
  blockers+=("CURRENT_PHASE_NOT_DONE")
fi

# Compatibility conflict if derived files allow business phase while matrix blocks it.
if [[ "$next_business_phase_allowed" == "NO" ]]; then
  if [[ "$active_allowed" == "true" || "$active_allowed" == "YES" || "$task_allowed" == "true" || "$task_allowed" == "YES" ]]; then
    blockers+=("COMPATIBILITY_FACT_CONFLICT")
  fi
fi

if [[ "$p0_0_effective" == "YES" \
  && "$clean_synced_main" == "YES" \
  && "$product_source_gate_status" == "PASS" \
  && "$matrix_sync" == "OK" \
  && -n "$test_evidence" \
  && "$test_evidence" != "None" \
  && "$test_evidence" != "Pending" ]]; then
  p0_merged_main_validation_status="PASS"
else
  p0_merged_main_validation_status="BLOCKED"
fi

evaluate_machine_runtime_identity
evaluate_runtime_transition

evaluate_product_audit_policy \
  "$effective_task_mode" \
  "$product_source_gate_status" \
  "$worktree_clean" \
  "$clean_synced_main" \
  "$current_package_pr_count" \
  "$active_conflicting_pr_count" \
  "$audit_scope_contract" \
  "$p0_0_effective" \
  "$p0_merged_main_validation_status" \
  "$p1a_repository_edits_allowed" \
  "$p1a_implementation_allowed" \
  "$p1a_implementation_pr_allowed"

resolve_task_handoff

if [[ "$resolution_status" != "ALLOWED" ]]; then
  can_continue="NO"
elif [[ "$request_class" == "CURRENT_PACKAGE_CONTINUATION" ]]; then
  can_continue="$current_package_action_allowed"
elif [[ "$resolved_mode" == "READ_ONLY_PRODUCT_AUDIT" ]]; then
  can_continue="$product_audit_allowed"
else
  can_continue="$can_start_next_business_phase"
fi

printf 'BRANCH: %s\n' "$branch"
printf 'WORKTREE_CLEAN: %s\n' "$worktree_clean"
printf 'HEAD: %s\n' "$head_commit"
printf 'ON_MAIN_BRANCH: %s\n' "$on_main_branch"
echo "RECENT_COMMITS:"
git log --oneline -5
printf 'OPEN_PRS: %s\n' "$open_prs"
printf 'OPEN_PR_CHECK_SOURCE: %s\n' "$open_pr_check_source"
printf 'OPEN_PR_COUNT: %s\n' "$open_pr_count"
printf 'OPEN_PR_STATUS: %s\n' "$open_pr_status"
printf 'CURRENT_PACKAGE_PR: %s\n' "$current_package_pr"
printf 'CURRENT_PACKAGE_PR_DRAFT: %s\n' "$current_package_pr_draft"
printf 'UNRELATED_OPEN_PRS: %s\n' "$unrelated_open_prs"
printf 'ACTIVE_CONFLICTING_OPEN_PRS: %s\n' "$active_conflicting_open_prs"
printf 'AUTHORIZED_SUCCESSOR_PRS: %s\n' "$authorized_successor_prs"
printf 'CLOSED_TECHNICAL_DEBT_PR: %s\n' "${closed_technical_debt_pr:-UNDECLARED}"
printf 'CLOSED_TECHNICAL_DEBT_STATUS: %s\n' "${closed_technical_debt_status:-UNDECLARED}"
printf 'CLOSED_TECHNICAL_DEBT_MERGED_STATUS: %s\n' "${closed_technical_debt_merged_status:-UNDECLARED}"
printf 'CLOSED_TECHNICAL_DEBT_EFFECTIVE: %s\n' "$closed_technical_debt_effective"
printf 'CLOSED_TECHNICAL_DEBT_BLOCKS_AUDIT: %s\n' "$closed_technical_debt_blocks_audit"
printf 'BLOCK_NEXT_BUSINESS_PHASE_ONLY: %s\n' "$block_next_business_phase_only"
printf 'MAIN_SYNC: %s\n' "$main_sync"
printf 'HEAD_IN_ORIGIN_MAIN: %s\n' "$head_in_origin_main"
printf 'HEAD_MATCHES_ORIGIN_MAIN: %s\n' "$head_matches_origin_main"
printf 'CLEAN_SYNCED_MAIN: %s\n' "$clean_synced_main"
printf 'CONTRACT_FILES_PRESENT: %s\n' "$contract_files_present"
printf 'CONTRACT_MATRIX_SYNC: %s\n' "$matrix_sync"
printf 'CURRENT_PHASE: %s\n' "P0-0"
printf 'CURRENT_PHASE_STATUS: %s\n' "$current_phase_status"
printf 'EXISTING_MODULE_MATURITY: %s\n' "$existing_module_maturity"
printf 'COMPLETION_EFFECTIVE_STATE: %s\n' "$completion_effective_state"
printf 'P0_0_EFFECTIVE: %s\n' "$p0_0_effective"
printf 'DECLARED_TASK_MODE: %s\n' "${task_mode:-UNDECLARED}"
printf 'CURRENT_TASK_MODE: %s\n' "${current_task_mode:-UNDECLARED}"
printf 'AUTHORIZED_NEXT_TASK_MODE: %s\n' "${authorized_next_task_mode:-UNDECLARED}"
printf 'DECLARED_CURRENT_EFFECTIVE_STATUS: %s\n' "${declared_current_effective_status:-UNDECLARED}"
printf 'AUTHORIZED_NEXT_PRODUCT_PHASE: %s\n' "${authorized_next_product_phase:-UNDECLARED}"
printf 'TASK_MODE: %s\n' "${effective_task_mode:-UNDECLARED}"
printf 'P0_MERGED_MAIN_VALIDATION_STATUS: %s\n' "$p0_merged_main_validation_status"
printf 'NEXT_TASK_AUTHORIZATION_STATUS: %s\n' "$next_task_authorization_status"
printf 'P1A_TRANSITION_ALLOWED: %s\n' "$p1a_transition_allowed"
printf 'P1A_REPOSITORY_EDITS_ALLOWED: %s\n' "${p1a_repository_edits_allowed:-UNDECLARED}"
printf 'P1A_IMPLEMENTATION_ALLOWED: %s\n' "${p1a_implementation_allowed:-UNDECLARED}"
printf 'P1A_IMPLEMENTATION_PR_ALLOWED: %s\n' "${p1a_implementation_pr_allowed:-UNDECLARED}"
printf 'P1B_AUTHORIZATION_DECLARED_STATUS: %s\n' "${p1b_authorization_declared_status:-UNDECLARED}"
printf 'P1B_1_STATUS: %s\n' "${p1b_1_declared_status:-UNDECLARED}"
printf 'P1B_HOME_CORE_DATA_DECLARED_STATUS: %s\n' "${home_core_data_declared_status:-UNDECLARED}"
printf 'P1B_HOME_CORE_DATA_COMPLETION_STATUS: %s\n' "${home_core_data_implementation_status:-UNDECLARED}"
printf 'PRODUCT_P1B_DECLARED_STATUS: %s\n' "${product_p1b_declared_status:-UNDECLARED}"
printf 'PRODUCT_P2_MATRIX_STATUS: %s\n' "${product_p2_matrix_status:-UNDECLARED}"
printf 'P2_POSITION_MONITORING_DECLARED_STATUS: %s\n' "${p2_authorization_declared_status:-UNDECLARED}"
printf 'P2_POSITION_MONITORING_DECLARED_IMPLEMENTATION_STATUS: %s\n' "${p2_implementation_status:-UNDECLARED}"
printf 'PRODUCT_V4_1_MATRIX_AUTHORIZATION: %s\n' "${product_v4_1_matrix_authorization:-UNDECLARED}"
printf 'V4_1_DECISION_CHAIN_DECLARED_DESIGN_STATUS: %s\n' "${v4_1_design_status:-UNDECLARED}"
printf 'V4_1_DECISION_CHAIN_DECLARED_AUTHORIZATION_STATUS: %s\n' "${v4_1_authorization_declared_status:-UNDECLARED}"
printf 'V4_1_DECISION_CHAIN_DECLARED_IMPLEMENTATION_STATUS: %s\n' "${v4_1_implementation_status:-UNDECLARED}"
printf 'V4_1_TARGET_RUNTIME_REMEDIATION_DECLARED_AUTHORIZATION_STATUS: %s\n' "${v4_1_target_runtime_authorization_declared_status:-UNDECLARED}"
printf 'V4_1_TARGET_RUNTIME_REMEDIATION_DECLARED_IMPLEMENTATION_STATUS: %s\n' "${v4_1_target_runtime_implementation_status:-UNDECLARED}"
printf 'V4_1_TARGET_RUNTIME_DECLARED_STATUS: %s\n' "${v4_1_target_runtime_status:-UNDECLARED}"
printf 'V4_1_TELEGRAM_DECLARED_AUTHORIZATION_STATUS: %s\n' "${v4_1_telegram_authorization_declared_status:-UNDECLARED}"
printf 'V4_1_TELEGRAM_DECLARED_IMPLEMENTATION_STATUS: %s\n' "${v4_1_telegram_implementation_status:-UNDECLARED}"
printf 'V4_1_TELEGRAM_DECLARED_LIVE_ACCEPTANCE_STATUS: %s\n' "${v4_1_telegram_live_acceptance_status:-UNDECLARED}"
printf 'V4_1_TELEGRAM_REMEDIATION_DECLARED_AUTHORIZATION_STATUS: %s\n' "${v4_1_telegram_remediation_authorization_declared_status:-UNDECLARED}"
printf 'V4_1_TELEGRAM_REMEDIATION_DECLARED_IMPLEMENTATION_STATUS: %s\n' "${v4_1_telegram_remediation_implementation_status:-UNDECLARED}"
printf 'V4_1_CORE_PRODUCTION_LOOP_DECLARED_AUTHORIZATION_STATUS: %s\n' "${v4_1_core_production_loop_authorization_declared_status:-UNDECLARED}"
printf 'V4_1_CORE_PRODUCTION_LOOP_DECLARED_IMPLEMENTATION_STATUS: %s\n' "${v4_1_core_production_loop_implementation_status:-UNDECLARED}"
printf 'LOCAL_REAL_DECLARED_AUTHORIZATION_STATUS: %s\n' "${local_real_authorization_declared_status:-UNDECLARED}"
printf 'LOCAL_REAL_DECLARED_IMPLEMENTATION_STATUS: %s\n' "${local_real_implementation_status:-UNDECLARED}"
printf 'FRONTEND_INTERACTION_DECLARED_AUTHORIZATION_STATUS: %s\n' "${frontend_interaction_authorization_declared_status:-UNDECLARED}"
printf 'FRONTEND_INTERACTION_DECLARED_IMPLEMENTATION_STATUS: %s\n' "${frontend_interaction_implementation_status:-UNDECLARED}"
printf 'MULTI_USER_DECLARED_AUTHORIZATION_STATUS: %s\n' "${multi_user_authorization_declared_status:-UNDECLARED}"
printf 'MULTI_USER_DECLARED_IMPLEMENTATION_STATUS: %s\n' "${multi_user_implementation_status:-UNDECLARED}"
printf 'PRODUCT_SOURCE_GATE_STATUS: %s\n' "$product_source_gate_status"
printf 'PRODUCT_AUDIT_ALLOWED: %s\n' "$product_audit_allowed"
printf 'READ_ONLY_PRODUCT_AUDIT_STATUS: %s\n' "$read_only_product_audit_status"
printf 'PRODUCT_AUDIT_BLOCKER: %s\n' "$product_audit_blocker"
printf 'CURRENT_WORK_PACKAGE: %s\n' "$current_work_package"
printf 'NEXT_BUSINESS_PHASE: %s\n' "$next_business_phase"
printf 'NEXT_BUSINESS_PHASE_ALLOWED: %s\n' "$next_business_phase_allowed"
printf 'P0_1_ALLOWED: %s\n' "$next_business_phase_allowed"
printf 'PRODUCTION_DEPLOYMENT_READINESS: %s\n' "$production_deployment_readiness"
printf 'CAN_START_NEXT_BUSINESS_PHASE: %s\n' "$can_start_next_business_phase"
printf 'CAN_CONTINUE_NEXT_PACKAGE: %s\n' "$can_continue"
emit_resolved_task_state
if (( ${#blockers[@]} == 0 )); then
  echo "BLOCKERS: none"
else
  # Deduplicate while preserving readable output.
  printf '%s\n' "${blockers[@]}" | awk '!seen[$0]++' | paste -sd' ' - | sed 's/^/BLOCKERS: /'
fi
