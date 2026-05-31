#!/usr/bin/env bash

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

if [[ "$#" -lt 1 ]]; then
  echo "usage: bash scripts/v1-pr-review-input.sh <PR_NUMBER>" >&2
  exit 1
fi

PR_NUMBER="$1"

echo "PR review input:"
echo "PR number: $PR_NUMBER"

echo
echo "PR metadata:"
gh pr view "$PR_NUMBER" \
  --json number,title,state,isDraft,mergeable,headRefName,baseRefName,statusCheckRollup,files

echo
echo "changed file paths:"
changed_files="$(gh pr diff "$PR_NUMBER" --name-only)"
echo "$changed_files"

echo
echo "forbidden path check:"
if echo "$changed_files" | grep -E 'src/main/java|src/test/java|src/main/resources|dashboard.html|schema|application.yml|application.yaml|pom.xml'; then
  echo "FORBIDDEN_PATH_ATTENTION"
else
  echo "NO_FORBIDDEN_PATH_ATTENTION"
fi
