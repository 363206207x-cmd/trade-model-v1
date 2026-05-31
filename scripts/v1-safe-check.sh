#!/usr/bin/env bash

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

echo "workflow contract:"
if bash scripts/check-workflow-contract.sh; then
  :
else
  echo "WORKFLOW_CONTRACT_FAILED"
  exit 1
fi

echo
echo "git status --short:"
git status --short

echo
echo "git diff --name-only:"
git diff --name-only

echo
echo "git diff --stat:"
git diff --stat

echo
echo "forbidden change check:"
if git diff --name-only | grep -E 'src/main/java/.*/(controller|mapper|repository|scheduler)|src/main/resources|dashboard.html|schema|application.yml|application.yaml|pom.xml'; then
  echo "FORBIDDEN_CHANGE_FOUND"
else
  echo "NO_FORBIDDEN_CHANGE"
fi
