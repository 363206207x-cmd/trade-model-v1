#!/usr/bin/env bash

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

if [[ "$#" -ne 3 ]]; then
  echo 'usage: bash scripts/v1-open-pr.sh <branch> "<title>" <risk>' >&2
  echo 'risk must be one of: A, B, B/C, C' >&2
  exit 1
fi

BRANCH="$1"
TITLE="$2"
RISK="$3"

if ! git rev-parse --is-inside-work-tree >/dev/null 2>&1; then
  echo "NOT_A_GIT_REPOSITORY" >&2
  exit 1
fi

if ! command -v gh >/dev/null 2>&1; then
  echo "GH_NOT_AVAILABLE" >&2
  exit 1
fi

if ! gh auth status >/dev/null 2>&1; then
  echo "GH_NOT_AVAILABLE" >&2
  exit 1
fi

case "$RISK" in
  A)
    risk_label="Risk: A docs-only"
    risk_note="Docs-only scope."
    ;;
  B)
    risk_label="Risk: B Java/test"
    risk_note="Java/test skeleton scope."
    ;;
  "B/C")
    risk_label="Risk: B/C elevated review"
    risk_note="Requires explicit user merge approval."
    ;;
  C)
    risk_label="Risk: C high-risk"
    risk_note="Requires explicit user merge approval."
    ;;
  *)
    echo "UNSUPPORTED_RISK: $RISK" >&2
    exit 1
    ;;
esac

if ! git ls-remote --exit-code --heads origin "$BRANCH" >/dev/null 2>&1; then
  echo "REMOTE_BRANCH_NOT_FOUND: $BRANCH" >&2
  exit 1
fi

existing_pr_url="$(gh pr list --head "$BRANCH" --state all --json url --jq '.[0].url // ""')"
if [[ -n "$existing_pr_url" ]]; then
  echo "PR_ALREADY_EXISTS: $existing_pr_url"
  exit 0
fi

body_file="$(mktemp)"
trap 'rm -f "$body_file"' EXIT

cat >"$body_file" <<EOF
## Summary

$TITLE

## Scope

- $risk_label
- $risk_note

## Required safety boundaries

- no executable point generation
- no executable entry / stop / TP / RR
- no external channel
- no Push send
- no order / execution / auto-trading
- incomplete-safe / fail-closed
- Risk Action Guard remains mandatory

## Workflow

- Draft PR created by fixed workflow script.
- Do not merge until the applicable A/B/C rule is satisfied.
EOF

gh pr create \
  --draft \
  --base main \
  --head "$BRANCH" \
  --title "$TITLE" \
  --body-file "$body_file"
