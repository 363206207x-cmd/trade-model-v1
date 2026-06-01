#!/usr/bin/env bash

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

is_number() {
  [[ "$1" =~ ^[0-9]+$ ]]
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

explicit_pr_number="${1:-${APPROVED_PR_NUMBER:-}}"

if [[ -z "$explicit_pr_number" ]]; then
  echo "MERGE_CURRENT_DISABLED_REQUIRE_EXPLICIT_PR"
  exit 1
fi

if ! is_number "$explicit_pr_number"; then
  echo "usage: bash scripts/v1-merge-current.sh <PR_NUMBER>" >&2
  echo "or: APPROVED_PR_NUMBER=<PR_NUMBER> bash scripts/v1-merge-current.sh" >&2
  exit 1
fi

pr_number="$explicit_pr_number"

echo "Only run this after user explicitly approved merge."
echo "（只能在用户明确同意合并 PR #$pr_number 后运行）"
echo

state="$(gh pr view "$pr_number" --json state --jq '.state')"
if [[ "$state" != "OPEN" ]]; then
  echo "PR is not open: $state" >&2
  exit 1
fi

title="$(gh pr view "$pr_number" --json title --jq '.title')"
branch="$(gh pr view "$pr_number" --json headRefName --jq '.headRefName')"
mergeable="$(gh pr view "$pr_number" --json mergeable --jq '.mergeable')"
ci_status="UNKNOWN"

total_checks="$(pr_checks_jq "$pr_number" 'length' '0')"
non_success_checks="$(pr_checks_jq "$pr_number" '[.[] | select(.bucket != "pass" and .bucket != "skipping")] | length' '0')"
if [[ "$total_checks" -gt 0 && "$non_success_checks" -eq 0 ]]; then
  ci_status="SUCCESS"
elif [[ "$total_checks" -eq 0 ]]; then
  ci_status="UNKNOWN"
else
  ci_status="NOT_SUCCESS"
fi

echo "PR number: $pr_number"
echo "PR title: $title"
echo "branch: $branch"
echo "mergeable: $mergeable"
echo "CI status: $ci_status"
echo

if [[ "$mergeable" != "MERGEABLE" ]]; then
  echo "PR is not MERGEABLE: $mergeable" >&2
  exit 1
fi

if [[ "$total_checks" -eq 0 || "$non_success_checks" -ne 0 ]]; then
  echo "CI is not all SUCCESS." >&2
  gh pr checks "$pr_number"
  exit 1
fi

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
