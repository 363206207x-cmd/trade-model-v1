#!/usr/bin/env bash

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

if [[ "$#" -lt 2 ]]; then
  echo 'usage: bash scripts/v1-merge-sync.sh <PR_NUMBER> "<SQUASH_SUBJECT>"' >&2
  echo 'run only after user explicitly says: 同意合并 PR #<PR_NUMBER>' >&2
  exit 1
fi

PR_NUMBER="$1"
SQUASH_SUBJECT="$2"

if ! command -v gh >/dev/null 2>&1; then
  echo "GH_NOT_AVAILABLE" >&2
  exit 1
fi

if ! gh auth status >/dev/null 2>&1; then
  echo "GH_NOT_AVAILABLE" >&2
  exit 1
fi

sync_main() {
  git switch main
  git pull origin main

  echo
  echo "git status:"
  git status

  echo
  echo "git log --oneline -5:"
  git log --oneline -5

  local worktree_clean="No"
  if [[ -z "$(git status --short)" ]]; then
    worktree_clean="Yes"
  fi

  echo
  echo "MERGE_SYNC_DONE"
  echo "WORKTREE_CLEAN: $worktree_clean"
  echo "HEAD: $(git log -1 --oneline)"
}

if ! gh pr view "$PR_NUMBER" --json number >/dev/null 2>&1; then
  echo "PR_NOT_FOUND: $PR_NUMBER" >&2
  exit 1
fi

echo "PR status before merge:"
gh pr view "$PR_NUMBER" --json number,title,state,isDraft,mergeable,headRefName,baseRefName,statusCheckRollup

state="$(gh pr view "$PR_NUMBER" --json state --jq '.state')"
if [[ "$state" == "MERGED" ]]; then
  echo "PR already merged; entering already merged sync mode."
  sync_main
  exit 0
fi

if [[ "$state" == "CLOSED" ]]; then
  echo "PR_CLOSED_NOT_MERGED" >&2
  exit 1
fi

if [[ "$state" != "OPEN" ]]; then
  echo "PR is not OPEN: $state" >&2
  exit 1
fi

mergeable="$(gh pr view "$PR_NUMBER" --json mergeable --jq '.mergeable')"
if [[ "$mergeable" != "MERGEABLE" ]]; then
  echo "PR is not MERGEABLE: $mergeable" >&2
  exit 1
fi

is_draft="$(gh pr view "$PR_NUMBER" --json isDraft --jq '.isDraft')"
if [[ "$is_draft" == "true" ]]; then
  gh pr ready "$PR_NUMBER"
else
  echo "PR is already ready for review."
fi

failing_checks="$(gh pr checks "$PR_NUMBER" --json name,bucket --jq '[.[] | select(.bucket != "pass" and .bucket != "skipping")] | length')"
if [[ "$failing_checks" != "0" ]]; then
  echo "CHECKS_NOT_SUCCESSFUL" >&2
  gh pr checks "$PR_NUMBER"
  exit 1
fi

gh pr merge "$PR_NUMBER" --squash --delete-branch --subject "$SQUASH_SUBJECT"

sync_main
