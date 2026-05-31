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

require_file "docs/ACTIVE_MAINLINE_STATUS.yml"

echo "Only run this after user explicitly approved merge."
echo "（只能在用户明确同意合并后运行）"

if ! pr_number="$(current_pr_number)"; then
  echo "NO_CURRENT_PR" >&2
  exit 1
fi

echo
echo "PR status before merge:"
gh pr view "$pr_number" --json number,title,state,isDraft,mergeable,headRefName,baseRefName,statusCheckRollup

mergeable="$(gh pr view "$pr_number" --json mergeable --jq '.mergeable')"
if [[ "$mergeable" != "MERGEABLE" ]]; then
  echo "PR is not MERGEABLE: $mergeable" >&2
  exit 1
fi

total_checks="$(pr_checks_jq "$pr_number" 'length' '0')"
non_success_checks="$(pr_checks_jq "$pr_number" '[.[] | select(.bucket != "pass" and .bucket != "skipping")] | length' '0')"
if [[ "$total_checks" -eq 0 || "$non_success_checks" -ne 0 ]]; then
  echo "CI is not all SUCCESS." >&2
  gh pr checks "$pr_number"
  exit 1
fi

title="$(gh pr view "$pr_number" --json title --jq '.title')"
subject="$title (#$pr_number)"

is_draft="$(gh pr view "$pr_number" --json isDraft --jq '.isDraft')"
if [[ "$is_draft" == "true" ]]; then
  gh pr ready "$pr_number"
else
  echo "PR is already ready for review."
fi

gh pr merge "$pr_number" --squash --subject "$subject"

git switch main
git pull origin main

echo
echo "git status:"
git status

echo
echo "git log --oneline -5:"
git log --oneline -5
