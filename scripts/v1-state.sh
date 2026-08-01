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

readonly_audit_scope_contract="NO_CODE_NO_TEST_NO_BUSINESS_PR_NO_PAUSED_PR_CHANGES"

trim() { sed -E 's/^[[:space:]]+|[[:space:]]+$//g'; }

is_false_flag() {
  [[ "$1" == "false" || "$1" == "NO" ]]
}

path_module() {
  case "$1" in
    src/main/resources/templates/dashboard*.html|src/main/resources/static/js/dashboard*.js|src/main/resources/static/css/dashboard*.css|src/main/java/org/example/trademodel/controller/*Dashboard*.java|src/main/java/org/example/trademodel/service/*Dashboard*.java|src/main/java/org/example/trademodel/service/impl/*Dashboard*.java|src/main/java/org/example/trademodel/service/dashboard/*|src/main/java/org/example/trademodel/vo/*Dashboard*.java|docs/design/P3_U2_IPHONE_HOME_*)
      echo "HOME"
      ;;
    docs/design/FE04_SEMANTIC_CONTRACT_V2.md|docs/INTERACTION_CONTRACT_V3.md|docs/FRONTEND_IMPLEMENTATION_CONTRACT_AUDIT_V2.md|docs/FE04_POSITION_MONITORING_IMPLEMENTATION_FREEZE.md)
      echo "SHARED_FE04_CONTRACT"
      ;;
    docs/PRODUCT_SOURCE_OF_TRUTH.md|docs/PRODUCT_FIELD_SOURCE.md|docs/PRODUCT_COMPLETION_MATRIX.md|docs/PRODUCT_GAP_ANALYSIS.md|docs/PRODUCT_ROADMAP_V2.md)
      echo "SHARED_PRODUCT_CONTRACT"
      ;;
    docs/CODEX_NEXT_TASK.yml|docs/SESSION_BOOTSTRAP.md|docs/WORKFLOW_COMMAND_AUTOMATION.md|docs/DELIVERY_PROGRESS_MATRIX.md|docs/PROJECT_CURRENT_STATE.md|scripts/v1-state.sh|scripts/check-workflow-contract.sh)
      echo "AUDIT_WORKFLOW_DEPENDENCY"
      ;;
    docs/ACTIVE_MAINLINE_STATUS.yml|docs/CONTRACT_CHANGE_LOG.md)
      echo "GOVERNANCE_RECORD"
      ;;
    scripts/check_fe04e_governance_semantics.py|scripts/test_check_fe04e_governance_semantics.py|scripts/check-fe04e-governance-contract.sh)
      echo "GOVERNANCE_TOOLING"
      ;;
    *)
      return 1
      ;;
  esac
}

path_source_domain() {
  case "$1" in
    src/main/resources/templates/dashboard*.html|src/main/resources/static/js/dashboard*.js|src/main/resources/static/css/dashboard*.css|src/main/java/org/example/trademodel/controller/*Dashboard*.java|src/main/java/org/example/trademodel/service/*Dashboard*.java|src/main/java/org/example/trademodel/service/impl/*Dashboard*.java|src/main/java/org/example/trademodel/service/dashboard/*|src/main/java/org/example/trademodel/vo/*Dashboard*.java|docs/design/P3_U2_IPHONE_HOME_*|docs/design/FE04_SEMANTIC_CONTRACT_V2.md|docs/INTERACTION_CONTRACT_V3.md|docs/FRONTEND_IMPLEMENTATION_CONTRACT_AUDIT_V2.md|docs/FE04_POSITION_MONITORING_IMPLEMENTATION_FREEZE.md)
      echo "HOME_INTERACTION"
      ;;
    docs/PRODUCT_SOURCE_OF_TRUTH.md|docs/PRODUCT_FIELD_SOURCE.md|docs/PRODUCT_COMPLETION_MATRIX.md|docs/PRODUCT_GAP_ANALYSIS.md|docs/PRODUCT_ROADMAP_V2.md)
      echo "PRODUCT_SOURCE"
      ;;
    docs/CODEX_NEXT_TASK.yml|docs/SESSION_BOOTSTRAP.md|docs/WORKFLOW_COMMAND_AUTOMATION.md|docs/DELIVERY_PROGRESS_MATRIX.md|docs/PROJECT_CURRENT_STATE.md|scripts/v1-state.sh|scripts/check-workflow-contract.sh)
      echo "PRODUCT_WORKFLOW"
      ;;
    docs/ACTIVE_MAINLINE_STATUS.yml|docs/CONTRACT_CHANGE_LOG.md)
      echo "GOVERNANCE_STATE"
      ;;
    scripts/check_fe04e_governance_semantics.py|scripts/test_check_fe04e_governance_semantics.py|scripts/check-fe04e-governance-contract.sh)
      echo "GOVERNANCE_SEMANTICS"
      ;;
    *)
      return 1
      ;;
  esac
}

list_has_line() {
  local list="$1" expected="$2" line
  while IFS= read -r line; do
    [[ "$line" == "$expected" ]] && return 0
  done <<<"$list"
  return 1
}

classify_paused_pr_scope() {
  local changed_source="$1" changed_paths="$2" audit_paths="$3" audit_modules="$4" audit_domains="$5"
  local changed audit module domain unmapped="NO"

  paused_pr_scope_relation="UNKNOWN"
  paused_pr_scope_reason="CHANGED_FILE_LOOKUP_UNAVAILABLE"
  paused_pr_changed_modules=""
  paused_pr_changed_source_domains=""

  if [[ "$changed_source" == "UNAVAILABLE" || -z "$changed_paths" ]]; then
    return 0
  fi
  if [[ -z "$audit_paths" || -z "$audit_modules" || -z "$audit_domains" ]]; then
    paused_pr_scope_reason="AUDIT_SCOPE_MISSING"
    return 0
  fi

  while IFS= read -r audit; do
    if [[ -z "$audit" || "$audit" == /* || "$audit" == *".."* ]]; then
      paused_pr_scope_reason="AUDIT_PATH_INVALID"
      return 0
    fi
  done <<<"$audit_paths"

  while IFS= read -r changed; do
    if [[ -z "$changed" || "$changed" == /* || "$changed" == *".."* ]]; then
      paused_pr_scope_reason="CHANGED_PATH_INVALID"
      return 0
    fi
    if module="$(path_module "$changed")"; then
      paused_pr_changed_modules="${paused_pr_changed_modules}${paused_pr_changed_modules:+$'\n'}$module"
    else
      unmapped="YES"
    fi
    if domain="$(path_source_domain "$changed")"; then
      paused_pr_changed_source_domains="${paused_pr_changed_source_domains}${paused_pr_changed_source_domains:+$'\n'}$domain"
    else
      unmapped="YES"
    fi
  done <<<"$changed_paths"

  paused_pr_changed_modules="$(printf '%s\n' "$paused_pr_changed_modules" | sed '/^$/d' | sort -u)"
  paused_pr_changed_source_domains="$(printf '%s\n' "$paused_pr_changed_source_domains" | sed '/^$/d' | sort -u)"

  while IFS= read -r changed; do
    while IFS= read -r audit; do
      if [[ "$changed" == "$audit" ]]; then
        paused_pr_scope_relation="OVERLAPPING"
        paused_pr_scope_reason="EXACT_PATH_OVERLAP:$changed"
        return 0
      fi
      if [[ "$changed" == "$audit/"* || "$audit" == "$changed/"* ]]; then
        paused_pr_scope_relation="OVERLAPPING"
        paused_pr_scope_reason="DIRECTORY_PREFIX_OVERLAP:$changed:$audit"
        return 0
      fi
    done <<<"$audit_paths"
  done <<<"$changed_paths"

  while IFS= read -r module; do
    if list_has_line "$audit_modules" "$module"; then
      paused_pr_scope_relation="OVERLAPPING"
      paused_pr_scope_reason="MODULE_OVERLAP:$module"
      return 0
    fi
  done <<<"$paused_pr_changed_modules"
  while IFS= read -r domain; do
    if list_has_line "$audit_domains" "$domain"; then
      paused_pr_scope_relation="OVERLAPPING"
      paused_pr_scope_reason="SOURCE_DOMAIN_OVERLAP:$domain"
      return 0
    fi
  done <<<"$paused_pr_changed_source_domains"

  if [[ "$unmapped" == "YES" ]]; then
    paused_pr_scope_reason="MODULE_OR_SOURCE_DOMAIN_MAPPING_UNKNOWN"
    return 0
  fi

  paused_pr_scope_relation="UNRELATED"
  paused_pr_scope_reason="NO_PATH_MODULE_OR_SOURCE_DOMAIN_OVERLAP"
}

evaluate_p0_to_p1a_transition() {
  local current_mode="$1" next_mode="$2" completion_state="$3" synced_main_status="$4"
  local source_gate_status="$5" merged_main_validation_status="$6" repository_edits_allowed="$7"
  local implementation_allowed="$8" implementation_pr_allowed="$9"

  effective_task_mode="$current_mode"
  p1a_transition_allowed="NO"
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
  next_task_authorization_status="ALLOWED"
}

evaluate_product_audit_policy() {
  local requested_mode="$1" source_gate_status="$2" worktree_status="$3" synced_main_status="$4"
  local current_pr_count="$5" active_conflict_count="$6" paused_pr_count="$7" paused_scope_relation="$8"
  local scope_contract="$9" baseline_effective="${10}" merged_main_validation_status="${11}"
  local repository_edits_allowed="${12}" implementation_allowed="${13}" implementation_pr_allowed="${14}"

  product_audit_allowed="NO"
  read_only_product_audit_status="BLOCKED_NOT_READ_ONLY_PRODUCT_AUDIT"
  paused_open_pr_blocks_audit="NOT_APPLICABLE"
  paused_open_pr_blocks_implementation="NO"
  product_audit_blocker="NOT_READ_ONLY_PRODUCT_AUDIT"

  if [[ "$paused_pr_count" != "0" ]]; then
    paused_open_pr_blocks_implementation="YES"
  fi

  [[ "$requested_mode" == "READ_ONLY_PRODUCT_AUDIT" ]] || return 0

  paused_open_pr_blocks_audit="NO"
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
  elif [[ "$paused_pr_count" != "0" && "$paused_scope_relation" == "OVERLAPPING" ]]; then
    paused_open_pr_blocks_audit="YES"
    read_only_product_audit_status="BLOCKED_PAUSED_PR_OVERLAP"
    product_audit_blocker="PAUSED_PR_SCOPE_OVERLAPPING"
  elif [[ "$paused_pr_count" != "0" && "$paused_scope_relation" != "UNRELATED" ]]; then
    paused_open_pr_blocks_audit="YES"
    read_only_product_audit_status="REQUIRE_HUMAN_DECISION_PAUSED_PR_SCOPE_UNKNOWN"
    product_audit_blocker="PAUSED_PR_SCOPE_UNKNOWN"
  else
    product_audit_allowed="YES"
    product_audit_blocker="NONE"
    if [[ "$paused_pr_count" == "0" ]]; then
      read_only_product_audit_status="ALLOWED"
    else
      read_only_product_audit_status="ALLOWED_WITH_PAUSED_UNRELATED_PR"
    fi
  fi
}

run_product_audit_policy_self_test() {
  local failed=0
  local home_path="src/main/resources/templates/dashboard.html"
  local home_modules="HOME"
  local home_domains="HOME_INTERACTION"

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

  assert_scope_case() {
    local name="$1" expected_relation="$2" changed_source="$3" changed_paths="$4" audit_paths="$5" audit_modules="$6" audit_domains="$7"
    classify_paused_pr_scope "$changed_source" "$changed_paths" "$audit_paths" "$audit_modules" "$audit_domains"
    if [[ "$paused_pr_scope_relation" == "$expected_relation" ]]; then
      echo "$name: PASS"
    else
      echo "$name: FAIL"
      failed=1
    fi
  }

  assert_audit_case() {
    local name="$1" expected_allowed="$2" expected_status="$3" expected_audit_block="$4" expected_implementation_block="$5"
    shift 5
    evaluate_product_audit_policy "$@"
    if [[ "$product_audit_allowed" == "$expected_allowed" \
      && "$read_only_product_audit_status" == "$expected_status" \
      && "$paused_open_pr_blocks_audit" == "$expected_audit_block" \
      && "$paused_open_pr_blocks_implementation" == "$expected_implementation_block" ]]; then
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

  assert_scope_case SCOPE_TEST_UNRELATED_GOVERNANCE_PARSER UNRELATED LOCAL_EXACT_BASE_HEAD \
    scripts/check_fe04e_governance_semantics.py "$home_path" "$home_modules" "$home_domains"
  assert_scope_case SCOPE_TEST_EXACT_OVERLAP OVERLAPPING LOCAL_EXACT_BASE_HEAD \
    "$home_path" "$home_path" "$home_modules" "$home_domains"
  assert_scope_case SCOPE_TEST_DIRECTORY_PREFIX_OVERLAP OVERLAPPING LOCAL_EXACT_BASE_HEAD \
    src/main/resources/templates "$home_path" "$home_modules" "$home_domains"
  assert_scope_case SCOPE_TEST_SHARED_CONTRACT_OVERLAP OVERLAPPING LOCAL_EXACT_BASE_HEAD \
    docs/PRODUCT_SOURCE_OF_TRUTH.md docs/PRODUCT_SOURCE_OF_TRUTH.md SHARED_PRODUCT_CONTRACT PRODUCT_SOURCE
  assert_scope_case SCOPE_TEST_LOOKUP_UNAVAILABLE UNKNOWN UNAVAILABLE "" "$home_path" "$home_modules" "$home_domains"
  assert_scope_case SCOPE_TEST_MISSING_AUDIT_SCOPE UNKNOWN LOCAL_EXACT_BASE_HEAD "$home_path" "" "" ""

  assert_audit_case AUDIT_POLICY_TEST_PAUSED_UNRELATED_AFTER_P0_MERGED YES ALLOWED_WITH_PAUSED_UNRELATED_PR NO YES \
    READ_ONLY_PRODUCT_AUDIT PASS Yes YES 0 0 1 UNRELATED "$readonly_audit_scope_contract" YES PASS false false false
  assert_audit_case AUDIT_POLICY_TEST_PAUSED_OVERLAP NO BLOCKED_PAUSED_PR_OVERLAP YES YES \
    READ_ONLY_PRODUCT_AUDIT PASS Yes YES 0 0 1 OVERLAPPING "$readonly_audit_scope_contract" YES PASS false false false
  assert_audit_case AUDIT_POLICY_TEST_PAUSED_SCOPE_UNKNOWN NO REQUIRE_HUMAN_DECISION_PAUSED_PR_SCOPE_UNKNOWN YES YES \
    READ_ONLY_PRODUCT_AUDIT PASS Yes YES 0 0 1 UNKNOWN "$readonly_audit_scope_contract" YES PASS false false false
  assert_audit_case AUDIT_POLICY_TEST_P0_NOT_MERGED NO BLOCKED_PENDING_P0_MERGED_MAIN NO YES \
    READ_ONLY_PRODUCT_AUDIT PASS Yes YES 0 0 1 UNRELATED "$readonly_audit_scope_contract" NO BLOCKED false false false
  assert_audit_case AUDIT_POLICY_TEST_ACTIVE_CONFLICTING_PR NO BLOCKED_ACTIVE_CONFLICTING_PR NO YES \
    READ_ONLY_PRODUCT_AUDIT PASS Yes YES 0 1 1 UNRELATED "$readonly_audit_scope_contract" YES PASS false false false
  assert_audit_case AUDIT_POLICY_TEST_DIRTY_WORKTREE NO BLOCKED_WORKTREE_DIRTY NO YES \
    READ_ONLY_PRODUCT_AUDIT PASS No YES 0 0 1 UNRELATED "$readonly_audit_scope_contract" YES PASS false false false
  assert_audit_case AUDIT_POLICY_TEST_PRODUCT_SOURCE_GATE_FAILED NO BLOCKED_PRODUCT_SOURCE_GATE NO YES \
    READ_ONLY_PRODUCT_AUDIT BLOCKED Yes YES 0 0 1 UNRELATED "$readonly_audit_scope_contract" YES PASS false false false
  assert_audit_case AUDIT_POLICY_TEST_IMPLEMENTATION_ATTEMPT NO BLOCKED_NOT_READ_ONLY_PRODUCT_AUDIT NOT_APPLICABLE YES \
    IMPLEMENTATION PASS Yes YES 0 0 1 UNRELATED "$readonly_audit_scope_contract" YES PASS false false false
  assert_audit_case AUDIT_POLICY_TEST_ATTEMPTED_REPOSITORY_EDIT NO BLOCKED_READ_ONLY_SCOPE NO YES \
    READ_ONLY_PRODUCT_AUDIT PASS Yes YES 0 0 1 UNRELATED "$readonly_audit_scope_contract" YES PASS true false false

  if [[ "$failed" -eq 0 ]]; then
    echo "PRODUCT_AUDIT_POLICY_TESTS: PASS"
    return 0
  fi
  echo "PRODUCT_AUDIT_POLICY_TESTS: BLOCKED"
  return 1
}

if [[ "${1:-}" == "--self-test-product-audit-policy" ]]; then
  run_product_audit_policy_self_test
  exit $?
elif [[ "$#" -gt 0 ]]; then
  echo "usage: bash scripts/v1-state.sh [--self-test-product-audit-policy]" >&2
  exit 2
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
audit_scope_contract="$(yaml_value "$TASK_FILE" read_only_product_audit_scope_contract)"
audit_scope_modules="$(yaml_list "$TASK_FILE" audit_scope_modules)"
audit_scope_paths="$(yaml_list "$TASK_FILE" audit_scope_paths)"
audit_scope_source_domains="$(yaml_list "$TASK_FILE" audit_scope_source_domains)"
paused_governance_pr="$(yaml_value "$TASK_FILE" paused_governance_pr)"
paused_governance_status="$(yaml_value "$TASK_FILE" paused_governance_status)"
paused_governance_branch="$(yaml_value "$TASK_FILE" paused_governance_branch)"
paused_governance_base="$(yaml_value "$TASK_FILE" paused_governance_base)"
paused_governance_head="$(yaml_value "$TASK_FILE" paused_governance_head)"
paused_governance_pr_number="${paused_governance_pr#\#}"

[[ -n "$current_task_mode" ]] || current_task_mode="$task_mode"

product_source_gate_status="BLOCKED"
if bash scripts/product-source-gate.sh --task-file "$TASK_FILE" >/dev/null 2>&1; then
  product_source_gate_status="PASS"
fi

paused_pr_changed_files_source="UNAVAILABLE"
paused_pr_changed_paths=""
if command -v gh >/dev/null 2>&1 && gh auth status >/dev/null 2>&1; then
  if paused_pr_changed_paths="$(gh pr diff "$paused_governance_pr_number" --name-only 2>/dev/null)" \
    && [[ -n "$paused_pr_changed_paths" ]]; then
    paused_pr_changed_files_source="GITHUB_PR_CHANGED_FILES"
  else
    paused_pr_changed_paths=""
  fi
fi
if [[ -z "$paused_pr_changed_paths" \
  && -n "$paused_governance_base" \
  && -n "$paused_governance_head" \
  ]] \
  && git cat-file -e "$paused_governance_base^{commit}" 2>/dev/null \
  && git cat-file -e "$paused_governance_head^{commit}" 2>/dev/null; then
  if paused_pr_changed_paths="$(git diff --name-only --diff-filter=ACDMRTUXB "$paused_governance_base...$paused_governance_head" 2>/dev/null)" \
    && [[ -n "$paused_pr_changed_paths" ]]; then
    paused_pr_changed_files_source="LOCAL_EXACT_BASE_HEAD"
  else
    paused_pr_changed_paths=""
  fi
fi

classify_paused_pr_scope \
  "$paused_pr_changed_files_source" \
  "$paused_pr_changed_paths" \
  "$audit_scope_paths" \
  "$audit_scope_modules" \
  "$audit_scope_source_domains"

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

current_package_branch="$(yaml_value "$TASK_FILE" branch)"
[[ -n "$current_package_branch" ]] || current_package_branch="$branch"

open_pr_check_source="not_checked"
open_pr_count="UNKNOWN"
open_pr_status="UNKNOWN"
open_prs="none"
current_package_pr="none"
unrelated_open_prs="none"
paused_candidate_open_prs="none"
paused_unrelated_open_prs="none"
paused_overlapping_open_prs="none"
paused_unknown_scope_open_prs="none"
active_conflicting_open_prs="none"
current_package_pr_count="0"
paused_candidate_pr_count="0"
paused_unrelated_pr_count="0"
paused_overlapping_pr_count="0"
paused_unknown_scope_pr_count="0"
active_conflicting_pr_count="0"
block_next_business_phase_only="NO"
if command -v gh >/dev/null 2>&1 && gh auth status >/dev/null 2>&1; then
  open_pr_check_source="gh CLI"
  if ! pr_rows="$(gh pr list --state open --json number,title,headRefName,headRefOid,isDraft --jq '.[] | [.number, .headRefName, .headRefOid, .title, .isDraft] | @tsv' 2>/dev/null)"; then
    pr_rows=""
    open_prs="GH_NOT_AVAILABLE"
    current_package_pr="GH_NOT_AVAILABLE"
    unrelated_open_prs="GH_NOT_AVAILABLE"
    paused_candidate_open_prs="GH_NOT_AVAILABLE"
    paused_unrelated_open_prs="GH_NOT_AVAILABLE"
    paused_overlapping_open_prs="GH_NOT_AVAILABLE"
    paused_unknown_scope_open_prs="GH_NOT_AVAILABLE"
    active_conflicting_open_prs="GH_NOT_AVAILABLE"
    current_package_pr_count="UNKNOWN"
    paused_candidate_pr_count="UNKNOWN"
    paused_unrelated_pr_count="UNKNOWN"
    paused_overlapping_pr_count="UNKNOWN"
    paused_unknown_scope_pr_count="UNKNOWN"
    active_conflicting_pr_count="UNKNOWN"
    open_pr_status="UNKNOWN"
    blockers+=("OPEN_PR_STATUS_UNKNOWN_GH_NOT_AVAILABLE")
  else
    open_pr_count="0"
    open_pr_status="NONE"
  fi
  if [[ -n "${pr_rows:-}" ]]; then
    open_pr_status="OPEN"
    open_pr_lines=()
    current_package_pr_lines=()
    unrelated_open_pr_lines=()
    paused_candidate_open_pr_lines=()
    paused_unrelated_open_pr_lines=()
    paused_overlapping_open_pr_lines=()
    paused_unknown_scope_open_pr_lines=()
    active_conflicting_open_pr_lines=()
    while IFS=$'\t' read -r pr_number pr_head pr_oid pr_title pr_draft; do
      [[ -z "${pr_number:-}" ]] && continue
      ((open_pr_count+=1))
      pr_line="#$pr_number $pr_head head=$pr_oid $pr_title draft=$pr_draft"
      open_pr_lines+=("$pr_line")
      if [[ "$pr_head" == "$current_package_branch" || "$pr_head" == "$branch" ]]; then
        current_package_pr_lines+=("$pr_line")
        ((current_package_pr_count+=1))
      else
        unrelated_open_pr_lines+=("$pr_line")
        block_next_business_phase_only="YES"
        if [[ "$pr_number" == "$paused_governance_pr_number" \
          && "$pr_head" == "$paused_governance_branch" \
          && "$pr_oid" == "$paused_governance_head" \
          && "$paused_governance_status" == "PAUSED_TECHNICAL_DEBT" \
          && "$pr_draft" == "true" ]]; then
          paused_candidate_open_pr_lines+=("$pr_line status=PAUSED_TECHNICAL_DEBT scope=$paused_pr_scope_relation")
          ((paused_candidate_pr_count+=1))
          case "$paused_pr_scope_relation" in
            UNRELATED)
              paused_unrelated_open_pr_lines+=("$pr_line status=PAUSED_TECHNICAL_DEBT scope=UNRELATED")
              ((paused_unrelated_pr_count+=1))
              blockers+=("PAUSED_UNRELATED_OPEN_PR_${pr_number}_BLOCKS_IMPLEMENTATION_BY_PHASE_RULE")
              ;;
            OVERLAPPING)
              paused_overlapping_open_pr_lines+=("$pr_line status=PAUSED_TECHNICAL_DEBT scope=OVERLAPPING")
              ((paused_overlapping_pr_count+=1))
              blockers+=("PAUSED_OVERLAPPING_OPEN_PR_${pr_number}_BLOCKS_AUDIT_AND_IMPLEMENTATION")
              ;;
            *)
              paused_unknown_scope_open_pr_lines+=("$pr_line status=PAUSED_TECHNICAL_DEBT scope=UNKNOWN")
              ((paused_unknown_scope_pr_count+=1))
              blockers+=("PAUSED_UNKNOWN_SCOPE_OPEN_PR_${pr_number}_REQUIRES_HUMAN_DECISION")
              ;;
          esac
        else
          active_conflicting_open_pr_lines+=("$pr_line status=ACTIVE_CONFLICTING_PR")
          ((active_conflicting_pr_count+=1))
          blockers+=("ACTIVE_CONFLICTING_PR_${pr_number}_BLOCKS_NEXT_BUSINESS_PHASE")
        fi
      fi
    done <<<"$pr_rows"
    open_prs="$(printf '%s\n' "${open_pr_lines[@]}")"
    if (( ${#current_package_pr_lines[@]} > 0 )); then
      current_package_pr="$(printf '%s\n' "${current_package_pr_lines[@]}")"
    fi
    if (( ${#unrelated_open_pr_lines[@]} > 0 )); then
      unrelated_open_prs="$(printf '%s\n' "${unrelated_open_pr_lines[@]}")"
    fi
    if (( ${#paused_candidate_open_pr_lines[@]} > 0 )); then
      paused_candidate_open_prs="$(printf '%s\n' "${paused_candidate_open_pr_lines[@]}")"
    fi
    if (( ${#paused_unrelated_open_pr_lines[@]} > 0 )); then
      paused_unrelated_open_prs="$(printf '%s\n' "${paused_unrelated_open_pr_lines[@]}")"
    fi
    if (( ${#paused_overlapping_open_pr_lines[@]} > 0 )); then
      paused_overlapping_open_prs="$(printf '%s\n' "${paused_overlapping_open_pr_lines[@]}")"
    fi
    if (( ${#paused_unknown_scope_open_pr_lines[@]} > 0 )); then
      paused_unknown_scope_open_prs="$(printf '%s\n' "${paused_unknown_scope_open_pr_lines[@]}")"
    fi
    if (( ${#active_conflicting_open_pr_lines[@]} > 0 )); then
      active_conflicting_open_prs="$(printf '%s\n' "${active_conflicting_open_pr_lines[@]}")"
    fi
  fi
else
  open_pr_check_source="unavailable"
  open_prs="GH_NOT_AVAILABLE"
  current_package_pr="GH_NOT_AVAILABLE"
  unrelated_open_prs="GH_NOT_AVAILABLE"
  paused_candidate_open_prs="GH_NOT_AVAILABLE"
  paused_unrelated_open_prs="GH_NOT_AVAILABLE"
  paused_overlapping_open_prs="GH_NOT_AVAILABLE"
  paused_unknown_scope_open_prs="GH_NOT_AVAILABLE"
  active_conflicting_open_prs="GH_NOT_AVAILABLE"
  current_package_pr_count="UNKNOWN"
  paused_candidate_pr_count="UNKNOWN"
  paused_unrelated_pr_count="UNKNOWN"
  paused_overlapping_pr_count="UNKNOWN"
  paused_unknown_scope_pr_count="UNKNOWN"
  active_conflicting_pr_count="UNKNOWN"
  open_pr_status="UNKNOWN"
  blockers+=("OPEN_PR_STATUS_UNKNOWN_GH_NOT_AVAILABLE")
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

evaluate_product_audit_policy \
  "$effective_task_mode" \
  "$product_source_gate_status" \
  "$worktree_clean" \
  "$clean_synced_main" \
  "$current_package_pr_count" \
  "$active_conflicting_pr_count" \
  "$paused_candidate_pr_count" \
  "$paused_pr_scope_relation" \
  "$audit_scope_contract" \
  "$p0_0_effective" \
  "$p0_merged_main_validation_status" \
  "$p1a_repository_edits_allowed" \
  "$p1a_implementation_allowed" \
  "$p1a_implementation_pr_allowed"

if [[ "$effective_task_mode" == "READ_ONLY_PRODUCT_AUDIT" ]]; then
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
printf 'UNRELATED_OPEN_PRS: %s\n' "$unrelated_open_prs"
printf 'PAUSED_CANDIDATE_OPEN_PRS: %s\n' "$paused_candidate_open_prs"
printf 'PAUSED_UNRELATED_OPEN_PRS: %s\n' "$paused_unrelated_open_prs"
printf 'PAUSED_OVERLAPPING_OPEN_PRS: %s\n' "$paused_overlapping_open_prs"
printf 'PAUSED_UNKNOWN_SCOPE_OPEN_PRS: %s\n' "$paused_unknown_scope_open_prs"
printf 'ACTIVE_CONFLICTING_OPEN_PRS: %s\n' "$active_conflicting_open_prs"
printf 'PAUSED_PR_CHANGED_FILES_SOURCE: %s\n' "$paused_pr_changed_files_source"
printf 'PAUSED_PR_CHANGED_PATHS: %s\n' "${paused_pr_changed_paths:-none}"
printf 'PAUSED_PR_CHANGED_MODULES: %s\n' "${paused_pr_changed_modules:-none}"
printf 'PAUSED_PR_CHANGED_SOURCE_DOMAINS: %s\n' "${paused_pr_changed_source_domains:-none}"
printf 'PAUSED_PR_SCOPE_RELATION: %s\n' "$paused_pr_scope_relation"
printf 'PAUSED_PR_SCOPE_REASON: %s\n' "$paused_pr_scope_reason"
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
printf 'P1B_AUTHORIZATION_RUNTIME_STATUS: %s\n' "$p1b_authorization_runtime_status"
printf 'PRODUCT_SOURCE_GATE_STATUS: %s\n' "$product_source_gate_status"
printf 'PRODUCT_AUDIT_ALLOWED: %s\n' "$product_audit_allowed"
printf 'READ_ONLY_PRODUCT_AUDIT_STATUS: %s\n' "$read_only_product_audit_status"
printf 'PAUSED_OPEN_PR_BLOCKS_AUDIT: %s\n' "$paused_open_pr_blocks_audit"
printf 'PAUSED_OPEN_PR_BLOCKS_IMPLEMENTATION: %s\n' "$paused_open_pr_blocks_implementation"
printf 'PRODUCT_AUDIT_BLOCKER: %s\n' "$product_audit_blocker"
printf 'CURRENT_WORK_PACKAGE: %s\n' "$current_work_package"
printf 'NEXT_BUSINESS_PHASE: %s\n' "$next_business_phase"
printf 'NEXT_BUSINESS_PHASE_ALLOWED: %s\n' "$next_business_phase_allowed"
printf 'P0_1_ALLOWED: %s\n' "$next_business_phase_allowed"
printf 'PRODUCTION_DEPLOYMENT_READINESS: %s\n' "$production_deployment_readiness"
printf 'CAN_START_NEXT_BUSINESS_PHASE: %s\n' "$can_start_next_business_phase"
printf 'CAN_CONTINUE_NEXT_PACKAGE: %s\n' "$can_continue"
if (( ${#blockers[@]} == 0 )); then
  echo "BLOCKERS: none"
else
  # Deduplicate while preserving readable output.
  printf '%s\n' "${blockers[@]}" | awk '!seen[$0]++' | paste -sd' ' - | sed 's/^/BLOCKERS: /'
fi
