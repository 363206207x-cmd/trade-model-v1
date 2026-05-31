#!/usr/bin/env bash

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

require_file() {
  local file="$1"
  if [[ ! -f "$file" ]]; then
    echo "missing file: $file" >&2
    exit 1
  fi
}

yaml_value() {
  local key="$1"
  awk -F': ' -v key="$key" '$1 == key { gsub(/^"|"$/, "", $2); print $2 }' docs/ACTIVE_MAINLINE_STATUS.yml
}

is_number() {
  [[ "$1" =~ ^[0-9]+$ ]]
}

is_empty_pr_value() {
  local value="$1"
  [[ -z "$value" || "$value" == "none" || "$value" == "null" ]]
}

open_pr_number_for_ref() {
  local ref="$1"
  local state
  if ! state="$(gh pr view "$ref" --json state --jq '.state' 2>/dev/null)"; then
    return 1
  fi
  if [[ "$state" != "OPEN" ]]; then
    return 1
  fi
  gh pr view "$ref" --json number --jq '.number'
}

current_pr_number() {
  local branch
  branch="$(git branch --show-current)"
  if [[ "$branch" != "main" && "$branch" != "master" ]]; then
    if open_pr_number_for_ref "$branch"; then
      return 0
    fi
  fi

  local configured_pr
  configured_pr="$(yaml_value current_pr)"
  if ! is_empty_pr_value "$configured_pr" && is_number "$configured_pr"; then
    if open_pr_number_for_ref "$configured_pr"; then
      return 0
    fi
  fi

  return 1
}

pr_checks_jq() {
  local pr_number="$1"
  local jq_filter="$2"
  local no_checks_default="$3"
  local output

  if output="$(gh pr checks "$pr_number" --json name,bucket --jq "$jq_filter" 2>&1)"; then
    echo "$output"
    return 0
  fi

  if echo "$output" | grep -qi "no checks"; then
    echo "$no_checks_default"
    return 0
  fi

  echo "$output" >&2
  exit 1
}

ci_status_for_pr() {
  local pr_number="$1"
  local total
  local pending
  local failed

  total="$(pr_checks_jq "$pr_number" 'length' '0')"
  pending="$(pr_checks_jq "$pr_number" '[.[] | select(.bucket == "pending")] | length' '0')"
  failed="$(pr_checks_jq "$pr_number" '[.[] | select(.bucket != "pass" and .bucket != "skipping" and .bucket != "pending")] | length' '0')"

  if [[ "$pending" -gt 0 ]]; then
    echo "IN_PROGRESS"
  elif [[ "$failed" -gt 0 ]]; then
    echo "FAILED"
  elif [[ "$total" -gt 0 ]]; then
    echo "SUCCESS"
  else
    echo "UNKNOWN"
  fi
}

workflow_contract_status_for_pr() {
  local pr_number="$1"
  pr_checks_jq \
    "$pr_number" \
    '[.[] | select((.name | ascii_downcase) | contains("workflow")) | "\(.name)=\(.bucket)"] | if length == 0 then "UNKNOWN" else join(", ") end' \
    'UNKNOWN'
}

has_match() {
  local values="$1"
  local pattern="$2"
  echo "$values" | grep -Eq "$pattern"
}

all_match() {
  local values="$1"
  local pattern="$2"
  [[ -n "$values" ]] && ! echo "$values" | grep -Ev "$pattern" >/dev/null
}

require_file "docs/ACTIVE_MAINLINE_STATUS.yml"

active_mainline="$(yaml_value active_mainline)"
active_mainline_cn="$(yaml_value active_mainline_cn)"
active_block="$(yaml_value active_block)"
active_block_cn="$(yaml_value active_block_cn)"
current_level="$(yaml_value current_level)"
next_required_action="$(yaml_value next_required_action)"
do_not_continue_to="$(yaml_value do_not_continue_to)"

can_merge="No current PR（没有当前 PR）"
next_step="$next_required_action"
remaining_steps="Run the next authorized pack or open/review the active PR when it exists."
current_pr_label="none"

if pr_number="$(current_pr_number)"; then
  pr_title="$(gh pr view "$pr_number" --json title --jq '.title')"
  pr_branch="$(gh pr view "$pr_number" --json headRefName --jq '.headRefName')"
  pr_draft="$(gh pr view "$pr_number" --json isDraft --jq '.isDraft')"
  pr_mergeable="$(gh pr view "$pr_number" --json mergeable --jq '.mergeable')"
  ci_status="$(ci_status_for_pr "$pr_number")"
  workflow_contract_status="$(workflow_contract_status_for_pr "$pr_number")"
  changed_files="$(gh pr diff "$pr_number" --name-only)"

  current_pr_label="#$pr_number $pr_title"

  forbidden_path_attention="NO_FORBIDDEN_PATH_ATTENTION"
  if has_match "$changed_files" 'src/main/java|src/test/java|src/main/resources|dashboard.html|schema|application.yml|application.yaml|pom.xml'; then
    forbidden_path_attention="FORBIDDEN_PATH_ATTENTION"
  fi

  deeper_review=false
  if has_match "$changed_files" 'src/main/java/.*/(controller|mapper|repository|scheduler)|src/main/resources|dashboard.html|schema|config|application.yml|application.yaml|pom.xml'; then
    deeper_review=true
  fi

  contains_java_or_test=false
  if has_match "$changed_files" '^src/main/java|^src/test/java'; then
    contains_java_or_test=true
  fi

  docs_scripts_only=false
  if all_match "$changed_files" '^(docs/|scripts/|\.github/)'; then
    docs_scripts_only=true
  fi

  if [[ "$pr_mergeable" == "CONFLICTING" ]]; then
    can_merge="No（不能）"
    next_step="Fix conflict（修复冲突）"
  elif [[ "$ci_status" == "IN_PROGRESS" ]]; then
    can_merge="No（不能）"
    next_step="Wait for CI（等待 CI）"
  elif [[ "$ci_status" == "FAILED" ]]; then
    can_merge="No（不能）"
    next_step="Fix CI（修复 CI）"
  elif [[ "$deeper_review" == "true" ]]; then
    can_merge="Needs deeper review（需要深入审查）"
    next_step="Review risky paths（审查高风险路径）"
  elif [[ "$docs_scripts_only" == "true" && "$ci_status" == "SUCCESS" && "$pr_mergeable" == "MERGEABLE" ]]; then
    can_merge="Yes, A-level direct merge allowed（可以，A 档可直接合并）"
    next_step="Merge current PR after review（审查后合并当前 PR）"
  elif [[ "$contains_java_or_test" == "true" && "$ci_status" == "SUCCESS" && "$pr_mergeable" == "MERGEABLE" ]]; then
    can_merge="Yes, needs user approval if CI success and mergeable（可以，但需要用户确认）"
    next_step="Approval required（需要确认）: 同意合并当前 PR"
  else
    can_merge="No（不能）"
    next_step="Wait for mergeability / CI / review（等待可合并状态、CI 或审查）"
  fi

  remaining_steps="Review PR #$pr_number, resolve CI/mergeability if needed, then use approved merge flow."

  echo "Current Mainline（当前主线）: $active_mainline / $active_mainline_cn"
  echo "Current Block（当前模块）: $active_block / $active_block_cn"
  echo "Current Level（当前层级）: $current_level"
  echo "Current PR（当前 PR）: $current_pr_label"
  echo "Can Merge?（能否合并）: $can_merge"
  echo "Next Step（下一步）: $next_step"
  echo "Remaining Steps（剩余步骤）: $remaining_steps"
  echo "Do Not Do（禁止事项）: do not continue to $do_not_continue_to; no order / execution / auto-trading"
  echo
  echo "PR number: $pr_number"
  echo "PR title: $pr_title"
  echo "branch: $pr_branch"
  echo "draft status: $pr_draft"
  echo "mergeable: $pr_mergeable"
  echo "CI status: $ci_status"
  echo "workflow-contract status: $workflow_contract_status"
  echo "forbidden path attention: $forbidden_path_attention"
  if [[ "$contains_java_or_test" == "true" ]]; then
    echo "Approval required（需要确认）: 同意合并当前 PR"
  fi
  echo
  echo "changed files:"
  echo "$changed_files"
  exit 0
fi

echo "Current Mainline（当前主线）: $active_mainline / $active_mainline_cn"
echo "Current Block（当前模块）: $active_block / $active_block_cn"
echo "Current Level（当前层级）: $current_level"
echo "Current PR（当前 PR）: $current_pr_label"
echo "Can Merge?（能否合并）: $can_merge"
echo "Next Step（下一步）: $next_step"
echo "Remaining Steps（剩余步骤）: $remaining_steps"
echo "Do Not Do（禁止事项）: do not continue to $do_not_continue_to; no order / execution / auto-trading"
