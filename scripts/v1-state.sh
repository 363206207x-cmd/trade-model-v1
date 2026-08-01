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

evaluate_product_audit_policy() {
  local requested_mode="$1"
  local source_gate_status="$2"
  local worktree_status="$3"
  local synced_main_status="$4"
  local current_pr_count="$5"
  local active_conflict_count="$6"
  local paused_unrelated_count="$7"
  local scope_contract="$8"
  local baseline_effective="$9"

  product_audit_allowed="NO"
  read_only_product_audit_status="BLOCKED_NOT_READ_ONLY_PRODUCT_AUDIT"
  paused_open_pr_blocks_audit="NOT_APPLICABLE"
  paused_open_pr_blocks_implementation="NO"
  product_audit_blocker="NOT_READ_ONLY_PRODUCT_AUDIT"

  if [[ "$paused_unrelated_count" != "0" ]]; then
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
  elif [[ "$baseline_effective" != "YES" ]]; then
    read_only_product_audit_status="BLOCKED_PENDING_P0_MERGED_MAIN"
    product_audit_blocker="P0_NOT_EFFECTIVE_MERGED_MAIN"
  elif [[ "$scope_contract" != "$readonly_audit_scope_contract" ]]; then
    read_only_product_audit_status="BLOCKED_READ_ONLY_SCOPE"
    product_audit_blocker="READ_ONLY_SCOPE_NOT_LOCKED"
  elif [[ "$current_pr_count" != "0" ]]; then
    read_only_product_audit_status="BLOCKED_CURRENT_PACKAGE_PR"
    product_audit_blocker="CURRENT_PACKAGE_PR_PRESENT"
  elif [[ "$active_conflict_count" != "0" ]]; then
    read_only_product_audit_status="BLOCKED_ACTIVE_CONFLICTING_PR"
    product_audit_blocker="ACTIVE_CONFLICTING_PR_PRESENT"
  else
    product_audit_allowed="YES"
    product_audit_blocker="NONE"
    if [[ "$paused_unrelated_count" == "0" ]]; then
      read_only_product_audit_status="ALLOWED"
    else
      read_only_product_audit_status="ALLOWED_WITH_PAUSED_UNRELATED_PR"
    fi
  fi
}

run_product_audit_policy_self_test() {
  local failed=0

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

  assert_audit_case AUDIT_POLICY_TEST_PAUSED_UNRELATED_READ_ONLY YES ALLOWED_WITH_PAUSED_UNRELATED_PR NO YES \
    READ_ONLY_PRODUCT_AUDIT PASS Yes YES 0 0 1 "$readonly_audit_scope_contract" YES
  assert_audit_case AUDIT_POLICY_TEST_PAUSED_IMPLEMENTATION_BLOCKED_PENDING_P0_MERGED_MAIN NO BLOCKED_NOT_READ_ONLY_PRODUCT_AUDIT NOT_APPLICABLE YES \
    IMPLEMENTATION PASS Yes NO 0 0 1 "$readonly_audit_scope_contract" NO
  assert_audit_case AUDIT_POLICY_TEST_ACTIVE_CONFLICTING_PR NO BLOCKED_ACTIVE_CONFLICTING_PR NO YES \
    READ_ONLY_PRODUCT_AUDIT PASS Yes YES 0 1 1 "$readonly_audit_scope_contract" YES
  assert_audit_case AUDIT_POLICY_TEST_DIRTY_WORKTREE NO BLOCKED_WORKTREE_DIRTY NO YES \
    READ_ONLY_PRODUCT_AUDIT PASS No YES 0 0 1 "$readonly_audit_scope_contract" YES
  assert_audit_case AUDIT_POLICY_TEST_PRODUCT_SOURCE_GATE_FAILED NO BLOCKED_PRODUCT_SOURCE_GATE NO YES \
    READ_ONLY_PRODUCT_AUDIT BLOCKED Yes YES 0 0 1 "$readonly_audit_scope_contract" YES
  assert_audit_case AUDIT_POLICY_TEST_ATTEMPTED_CODE_CHANGE NO BLOCKED_READ_ONLY_SCOPE NO YES \
    READ_ONLY_PRODUCT_AUDIT PASS Yes YES 0 0 1 CODE_CHANGES_ATTEMPTED YES

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

trim() { sed -E 's/^[[:space:]]+|[[:space:]]+$//g'; }

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
audit_scope_contract="$(yaml_value "$TASK_FILE" read_only_product_audit_scope_contract)"
paused_governance_pr="$(yaml_value "$TASK_FILE" paused_governance_pr)"
paused_governance_status="$(yaml_value "$TASK_FILE" paused_governance_status)"
paused_governance_branch="$(yaml_value "$TASK_FILE" paused_governance_branch)"
paused_governance_head="$(yaml_value "$TASK_FILE" paused_governance_head)"
paused_governance_pr_number="${paused_governance_pr#\#}"

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

current_package_branch="$(yaml_value "$TASK_FILE" branch)"
[[ -n "$current_package_branch" ]] || current_package_branch="$branch"

open_pr_check_source="not_checked"
open_pr_count="UNKNOWN"
open_pr_status="UNKNOWN"
open_prs="none"
current_package_pr="none"
unrelated_open_prs="none"
paused_unrelated_open_prs="none"
active_conflicting_open_prs="none"
current_package_pr_count="0"
paused_unrelated_pr_count="0"
active_conflicting_pr_count="0"
block_next_business_phase_only="NO"
if command -v gh >/dev/null 2>&1 && gh auth status >/dev/null 2>&1; then
  open_pr_check_source="gh CLI"
  if ! pr_rows="$(gh pr list --state open --json number,title,headRefName,headRefOid,isDraft --jq '.[] | [.number, .headRefName, .headRefOid, .title, .isDraft] | @tsv' 2>/dev/null)"; then
    pr_rows=""
    open_prs="GH_NOT_AVAILABLE"
    current_package_pr="GH_NOT_AVAILABLE"
    unrelated_open_prs="GH_NOT_AVAILABLE"
    paused_unrelated_open_prs="GH_NOT_AVAILABLE"
    active_conflicting_open_prs="GH_NOT_AVAILABLE"
    current_package_pr_count="UNKNOWN"
    paused_unrelated_pr_count="UNKNOWN"
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
    paused_unrelated_open_pr_lines=()
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
          paused_unrelated_open_pr_lines+=("$pr_line status=PAUSED_TECHNICAL_DEBT")
          ((paused_unrelated_pr_count+=1))
          blockers+=("PAUSED_UNRELATED_OPEN_PR_${pr_number}_BLOCKS_IMPLEMENTATION_BY_PHASE_RULE")
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
    if (( ${#paused_unrelated_open_pr_lines[@]} > 0 )); then
      paused_unrelated_open_prs="$(printf '%s\n' "${paused_unrelated_open_pr_lines[@]}")"
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
  paused_unrelated_open_prs="GH_NOT_AVAILABLE"
  active_conflicting_open_prs="GH_NOT_AVAILABLE"
  current_package_pr_count="UNKNOWN"
  paused_unrelated_pr_count="UNKNOWN"
  active_conflicting_pr_count="UNKNOWN"
  open_pr_status="UNKNOWN"
  blockers+=("OPEN_PR_STATUS_UNKNOWN_GH_NOT_AVAILABLE")
fi

# Phase gate.
next_business_phase_allowed="NO"
can_start_next_business_phase="NO"
p0_0_effective="NO"
clean_synced_main="NO"
if [[ "$worktree_clean" == "Yes" && "$main_sync" == "OK" ]] && { [[ "$branch" == "main" ]] || [[ "$head_matches_origin_main" == "YES" ]]; }; then
  clean_synced_main="YES"
fi
if [[ "$current_phase_status" == "DONE" && "$completion_effective_state" == "EFFECTIVE_MERGED_MAIN" ]]; then
  p0_0_effective="YES"
  # Require merged/synced main, clean main, no open PR, contract sync, and matrix test evidence.
  test_evidence="$(matrix_field P0-0 7)"
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

evaluate_product_audit_policy \
  "$task_mode" \
  "$product_source_gate_status" \
  "$worktree_clean" \
  "$clean_synced_main" \
  "$current_package_pr_count" \
  "$active_conflicting_pr_count" \
  "$paused_unrelated_pr_count" \
  "$audit_scope_contract" \
  "$p0_0_effective"

if [[ "$task_mode" == "READ_ONLY_PRODUCT_AUDIT" ]]; then
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
printf 'PAUSED_UNRELATED_OPEN_PRS: %s\n' "$paused_unrelated_open_prs"
printf 'ACTIVE_CONFLICTING_OPEN_PRS: %s\n' "$active_conflicting_open_prs"
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
printf 'TASK_MODE: %s\n' "${task_mode:-UNDECLARED}"
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
