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

emit_resolved_task_state() {
  printf 'CURRENT_PACKAGE: %s\n' "${current_package_phase:-UNDECLARED}"
  printf 'REQUESTED_PACKAGE: %s\n' "${requested_package:-AUTO}"
  printf 'CURRENT_PACKAGE_ACTION_ALLOWED: %s\n' "${current_package_action_allowed:-NO}"
  printf 'CURRENT_PACKAGE_BLOCK_REASON: %s\n' "${current_package_block_reason:-BLOCKED_UNKNOWN_STATE}"
  printf 'CURRENT_EFFECTIVE_STATUS: %s\n' "${completion_effective_state:-UNKNOWN}"
  printf 'AUTHORIZED_NEXT_PACKAGE: %s\n' "${authorized_next_package_phase:-UNDECLARED}"
  printf 'AUTHORIZED_NEXT_TASK_MODE: %s\n' "${authorized_next_package_mode:-UNDECLARED}"
  printf 'NEXT_PACKAGE_ALLOWED: %s\n' "${next_package_allowed:-NO}"
  printf 'NEXT_PACKAGE_BLOCK_REASON: %s\n' "${next_package_block_reason:-BLOCKED_UNKNOWN_STATE}"
  printf 'OPEN_PR_EVIDENCE_SOURCE: %s\n' "${open_pr_evidence_source:-UNAVAILABLE}"
  printf 'OPEN_PR_NONE_CONFIRMED: %s\n' "${open_pr_none_confirmed:-NO}"
  printf 'ACTIVE_CONFLICTING_PRS: %s\n' "${active_conflicting_pr_count:-UNKNOWN}"
  printf 'REQUEST_CLASS: %s\n' "${request_class:-UNKNOWN}"
  printf 'REPOSITORY_EDITS_ALLOWED: %s\n' "${resolved_edit_permission:-false}"
  printf 'IMPLEMENTATION_ALLOWED: %s\n' "${resolved_implementation_permission:-false}"
  printf 'PR_CREATION_ALLOWED: %s\n' "${resolved_pr_creation_permission:-false}"
  # Compatibility alias for older consumers. New launchers consume PR_CREATION_ALLOWED.
  printf 'IMPLEMENTATION_PR_ALLOWED: %s\n' "${resolved_pr_creation_permission:-false}"
  printf 'P1B_AUTHORIZATION_STATUS: %s\n' "${p1b_authorization_declared_status:-UNDECLARED}"
  printf 'P1B_AUTHORIZATION_RUNTIME_STATUS: %s\n' "${p1b_authorization_runtime_status:-BLOCKED}"
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
    elif [[ "$requested_package" == "${authorized_next_package_phase:-UNDECLARED}" \
      || "$requested_package" == "${blocked_package_phase:-UNDECLARED}" ]]; then
      request_class="SUCCESSOR_PACKAGE"
    fi
    return 0
  fi

  case "${completion_effective_state:-UNKNOWN}" in
    PENDING_MERGED_MAIN)
      request_class="CURRENT_PACKAGE_CONTINUATION"
      ;;
    EFFECTIVE_MERGED_MAIN)
      request_class="SUCCESSOR_PACKAGE"
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
    elif [[ "${current_package_pr_count:-UNKNOWN}" != "0" \
      && "${current_package_pr_count:-UNKNOWN}" != "1" ]]; then
      current_package_block_reason="BLOCKED_UNKNOWN_CURRENT_PACKAGE_PR_STATE"
    else
      current_package_action_allowed="YES"
      current_package_block_reason="NONE"
    fi

    if [[ "${completion_effective_state:-UNKNOWN}" != "EFFECTIVE_MERGED_MAIN" ]]; then
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
    fi
    if [[ "${current_package_pr_count:-0}" == "1" && "${current_package_pr_draft:-UNKNOWN}" == "false" ]]; then
      resolved_handoff_stage="CURRENT_PACKAGE_FINAL_MERGE_PATH"
      if [[ "$current_package_phase" == "P0_PRODUCT_FOUNDATION" ]]; then
        resolved_handoff_stage="P0_FINAL_MERGE_PATH"
      elif [[ "$current_package_phase" == "P1A_HOME_ALIGNMENT_READINESS_AND_GAP_AUDIT" ]]; then
        resolved_handoff_stage="P1B_AUTHORIZATION_FINAL_MERGE_PATH"
      fi
    fi
    resolved_edit_permission="$current_package_repository_edits_allowed"
    resolved_implementation_permission="$current_package_implementation_allowed"
    resolved_pr_creation_permission="$current_package_implementation_pr_allowed"
    resolved_next_action="$current_package_next_action"
    return 0
  fi

  if [[ "$request_class" != "SUCCESSOR_PACKAGE" ]]; then
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
  fi
  resolved_edit_permission="$authorized_next_repository_edits_allowed"
  resolved_implementation_permission="$authorized_next_implementation_allowed"
  resolved_pr_creation_permission="$authorized_next_implementation_pr_allowed"
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

evaluate_runtime_transition() {
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

run_policy_self_test="NO"
while [[ "$#" -gt 0 ]]; do
  case "$1" in
    --self-test-product-audit-policy)
      run_policy_self_test="YES"
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
      echo "usage: bash scripts/v1-state.sh [--self-test-product-audit-policy] [--open-pr-none-confirmed] [--request-package PACKAGE]" >&2
      exit 2
      ;;
  esac
done

if [[ "$run_policy_self_test" == "YES" ]]; then
  run_product_audit_policy_self_test
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
  current_package_risk="$(yaml_value "$TASK_FILE" current_package_risk)"
  current_package_repository_edits_allowed="$(yaml_value "$TASK_FILE" current_package_repository_edits_allowed)"
  current_package_implementation_allowed="$(yaml_value "$TASK_FILE" current_package_implementation_allowed)"
  current_package_implementation_pr_allowed="$(yaml_value "$TASK_FILE" current_package_implementation_pr_allowed)"
  current_package_next_action="$(yaml_value "$TASK_FILE" current_package_next_action)"

  authorized_next_package_phase="$(yaml_value "$TASK_FILE" authorized_next_package_phase)"
  authorized_next_package_name="$(yaml_value "$TASK_FILE" authorized_next_package_name)"
  authorized_next_package_active_block="$(yaml_value "$TASK_FILE" authorized_next_package_active_block)"
  authorized_next_package_mode="$(yaml_value "$TASK_FILE" authorized_next_package_mode)"
  authorized_next_package_branch="$(yaml_value "$TASK_FILE" authorized_next_package_branch)"
  authorized_next_package_risk="$(yaml_value "$TASK_FILE" authorized_next_package_risk)"
  authorized_next_repository_edits_allowed="$(yaml_value "$TASK_FILE" authorized_next_package_repository_edits_allowed)"
  authorized_next_implementation_allowed="$(yaml_value "$TASK_FILE" authorized_next_package_implementation_allowed)"
  authorized_next_implementation_pr_allowed="$(yaml_value "$TASK_FILE" authorized_next_package_implementation_pr_allowed)"
  authorized_next_package_next_action="$(yaml_value "$TASK_FILE" authorized_next_package_next_action)"
  blocked_package_phase="$(yaml_value "$TASK_FILE" blocked_package_phase)"
  blocked_package_status="$(yaml_value "$TASK_FILE" blocked_package_status)"
  p1b_authorization_declared_status="$(yaml_value "$TASK_FILE" p1b_authorization_status)"
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
  blockers_text="P1B_AUTHORIZATION_PENDING_MERGED_MAIN"
  requested_package="$provided_requested_package"
  open_pr_evidence_source="GH_QUERY"
  open_pr_none_confirmed="NO"
  open_pr_evidence_input_valid="$provided_evidence_valid"

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
    authorization_pending_request_p1b)
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
    authorization_merged_validated|p1b_operator|closed_pr_1156|merged_gh_no_pr)
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
    p1b_unauthorized)
      completion_effective_state="EFFECTIVE_MERGED_MAIN"
      clean_synced_main="YES"
      p0_merged_main_validation_status="PASS"
      branch="main"
      current_package_pr_count="0"
      current_package_pr_draft="NONE"
      open_prs="none"
      requested_package="$authorized_next_package_phase"
      p1b_authorization_declared_status="BLOCKED_PENDING_REVIEW"
      blockers_text="P1B_NOT_AUTHORIZED"
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
    p1b_permission_missing)
      completion_effective_state="EFFECTIVE_MERGED_MAIN"
      clean_synced_main="YES"
      p0_merged_main_validation_status="PASS"
      branch="main"
      current_package_pr_count="0"
      current_package_pr_draft="NONE"
      open_prs="none"
      authorized_next_implementation_allowed="false"
      blockers_text="P1B_PERMISSION_INCOMPLETE"
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
p1b_authorization_declared_status="$(yaml_value "$TASK_FILE" p1b_authorization_status)"
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
current_package_pr_count="0"
current_package_pr_draft="NONE"
active_conflicting_pr_count="0"
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
    while IFS=$'\t' read -r pr_number pr_head pr_oid pr_title pr_draft; do
      [[ -z "${pr_number:-}" ]] && continue
      ((open_pr_count+=1))
      pr_line="#$pr_number $pr_head head=$pr_oid $pr_title draft=$pr_draft"
      open_pr_lines+=("$pr_line")
      if [[ "$pr_head" == "$current_package_branch" || "$pr_head" == "$branch" ]]; then
        current_package_pr_lines+=("$pr_line")
        ((current_package_pr_count+=1))
        current_package_pr_draft="$pr_draft"
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
