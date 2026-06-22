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
  local index="$2"
  [[ -f "$MATRIX_FILE" ]] || return 0
  awk -F'|' -v phase="$phase" -v index="$index" '
    $2 ~ "^[[:space:]]*" phase "[[:space:]]*$" {
      value=$index
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
if git rev-parse --verify main >/dev/null 2>&1 && git rev-parse --verify origin/main >/dev/null 2>&1; then
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

open_prs="none"
current_package_pr="none"
unrelated_open_prs="none"
block_next_business_phase_only="NO"
if command -v gh >/dev/null 2>&1 && gh auth status >/dev/null 2>&1; then
  if ! pr_rows="$(gh pr list --state open --json number,title,headRefName,isDraft --jq '.[] | [.number, .headRefName, .title, .isDraft] | @tsv' 2>/dev/null)"; then
    pr_rows=""
    open_prs="GH_NOT_AVAILABLE"
    current_package_pr="GH_NOT_AVAILABLE"
    unrelated_open_prs="GH_NOT_AVAILABLE"
    blockers+=("OPEN_PR_STATUS_UNKNOWN_GH_NOT_AVAILABLE")
  fi
  if [[ -n "${pr_rows:-}" ]]; then
    open_pr_lines=()
    current_package_pr_lines=()
    unrelated_open_pr_lines=()
    while IFS=$'\t' read -r pr_number pr_head pr_title pr_draft; do
      [[ -z "${pr_number:-}" ]] && continue
      pr_line="#$pr_number $pr_head $pr_title draft=$pr_draft"
      open_pr_lines+=("$pr_line")
      if [[ "$pr_head" == "$current_package_branch" || "$pr_head" == "$branch" ]]; then
        current_package_pr_lines+=("$pr_line")
      else
        unrelated_open_pr_lines+=("$pr_line")
        block_next_business_phase_only="YES"
        blockers+=("UNRELATED_OPEN_PR_${pr_number}_BLOCKS_NEXT_BUSINESS_PHASE")
      fi
    done <<<"$pr_rows"
    open_prs="$(printf '%s\n' "${open_pr_lines[@]}")"
    if (( ${#current_package_pr_lines[@]} > 0 )); then
      current_package_pr="$(printf '%s\n' "${current_package_pr_lines[@]}")"
    fi
    if (( ${#unrelated_open_pr_lines[@]} > 0 )); then
      unrelated_open_prs="$(printf '%s\n' "${unrelated_open_pr_lines[@]}")"
    fi
  fi
else
  open_prs="GH_NOT_AVAILABLE"
  current_package_pr="GH_NOT_AVAILABLE"
  unrelated_open_prs="GH_NOT_AVAILABLE"
  blockers+=("OPEN_PR_STATUS_UNKNOWN_GH_NOT_AVAILABLE")
fi

# Phase gate.
next_business_phase_allowed="NO"
can_start_next_business_phase="NO"
p0_0_effective="NO"
if [[ "$current_phase_status" == "DONE" && "$completion_effective_state" == "EFFECTIVE_MERGED_MAIN" ]]; then
  p0_0_effective="YES"
  # Require merged/synced main, clean main, no open PR, contract sync, and matrix test evidence.
  test_evidence="$(matrix_field P0-0 7)"
  if [[ "$branch" == "main" && "$worktree_clean" == "Yes" && "$main_sync" == "OK" && "$current_package_pr" == "none" && "$unrelated_open_prs" == "none" && "$matrix_sync" == "OK" && "$test_evidence" != "None" && "$test_evidence" != "Pending" ]]; then
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

can_continue="$can_start_next_business_phase"

printf 'BRANCH: %s\n' "$branch"
printf 'WORKTREE_CLEAN: %s\n' "$worktree_clean"
printf 'HEAD: %s\n' "$head_commit"
printf 'ON_MAIN_BRANCH: %s\n' "$on_main_branch"
echo "RECENT_COMMITS:"
git log --oneline -5
printf 'OPEN_PRS: %s\n' "$open_prs"
printf 'CURRENT_PACKAGE_PR: %s\n' "$current_package_pr"
printf 'UNRELATED_OPEN_PRS: %s\n' "$unrelated_open_prs"
printf 'BLOCK_NEXT_BUSINESS_PHASE_ONLY: %s\n' "$block_next_business_phase_only"
printf 'MAIN_SYNC: %s\n' "$main_sync"
printf 'HEAD_IN_ORIGIN_MAIN: %s\n' "$head_in_origin_main"
printf 'CONTRACT_FILES_PRESENT: %s\n' "$contract_files_present"
printf 'CONTRACT_MATRIX_SYNC: %s\n' "$matrix_sync"
printf 'CURRENT_PHASE: %s\n' "P0-0"
printf 'CURRENT_PHASE_STATUS: %s\n' "$current_phase_status"
printf 'EXISTING_MODULE_MATURITY: %s\n' "$existing_module_maturity"
printf 'COMPLETION_EFFECTIVE_STATE: %s\n' "$completion_effective_state"
printf 'P0_0_EFFECTIVE: %s\n' "$p0_0_effective"
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
