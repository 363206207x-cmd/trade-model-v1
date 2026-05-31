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

echo "PR status before merge:"
gh pr view "$PR_NUMBER" --json number,title,state,isDraft,mergeable,headRefName,baseRefName,statusCheckRollup

mergeable="$(gh pr view "$PR_NUMBER" --json mergeable --jq '.mergeable')"
if [[ "$mergeable" != "MERGEABLE" ]]; then
  echo "PR is not MERGEABLE: $mergeable" >&2
  exit 1
fi

failing_checks="$(gh pr checks "$PR_NUMBER" --json name,bucket --jq '[.[] | select(.bucket != "pass" and .bucket != "skipping")] | length')"
if [[ "$failing_checks" != "0" ]]; then
  echo "CI is not successful; failing or pending checks exist." >&2
  gh pr checks "$PR_NUMBER"
  exit 1
fi

is_draft="$(gh pr view "$PR_NUMBER" --json isDraft --jq '.isDraft')"
if [[ "$is_draft" == "true" ]]; then
  gh pr ready "$PR_NUMBER"
else
  echo "PR is already ready for review."
fi

gh pr merge "$PR_NUMBER" --squash --subject "$SQUASH_SUBJECT"

git switch main
git pull origin main

echo
echo "git status:"
git status

echo
echo "git log --oneline -5:"
git log --oneline -5
