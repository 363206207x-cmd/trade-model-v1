#!/usr/bin/env bash

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

if [[ "$#" -ne 3 ]]; then
  echo 'usage: bash scripts/v1-open-pr.sh <branch> "<title>" <risk>' >&2
  echo 'risk: A | B | B/C | C' >&2
  exit 1
fi

BRANCH="$1"
TITLE="$2"
RISK="$3"

case "$RISK" in
  A)
    RISK_LABEL="Risk: A docs-only"
    RISK_SCOPE="A-risk docs-only package. Confirm docs-only scope before merge."
    ;;
  B)
    RISK_LABEL="Risk: B Java/test"
    RISK_SCOPE="B-risk Java/test package. Requires explicit user merge approval."
    ;;
  "B/C")
    RISK_LABEL="Risk: B/C guarded"
    RISK_SCOPE="B/C-risk package. Requires explicit user merge approval."
    ;;
  C)
    RISK_LABEL="Risk: C high-risk"
    RISK_SCOPE="C-risk package. Requires explicit user merge approval and must remain within authorized scope."
    ;;
  *)
    echo "unsupported risk: $RISK" >&2
    echo 'risk: A | B | B/C | C' >&2
    exit 1
    ;;
esac

if ! command -v gh >/dev/null 2>&1; then
  echo "GH_NOT_AVAILABLE" >&2
  exit 1
fi

if ! gh auth status >/dev/null 2>&1; then
  echo "GH_NOT_AVAILABLE" >&2
  exit 1
fi

if ! git rev-parse --is-inside-work-tree >/dev/null 2>&1; then
  echo "NOT_A_GIT_REPOSITORY" >&2
  exit 1
fi

repo_root="$(git rev-parse --show-toplevel)"
if [[ "$repo_root" != "$ROOT_DIR" ]]; then
  echo "UNEXPECTED_REPOSITORY_ROOT: $repo_root" >&2
  exit 1
fi

if ! git ls-remote --exit-code --heads origin "$BRANCH" >/dev/null 2>&1; then
  echo "REMOTE_BRANCH_NOT_FOUND: origin/$BRANCH" >&2
  exit 1
fi

existing_pr_url="$(gh pr list --head "$BRANCH" --state open --json url --jq '.[0].url // empty')"
if [[ -n "$existing_pr_url" ]]; then
  echo "PR_ALREADY_EXISTS: $existing_pr_url"
  exit 0
fi

body="$(cat <<BODY
## Summary

$TITLE

## Risk

- $RISK_LABEL
- $RISK_SCOPE

## Scope Guard

- No executable point generation
- No executable entry / stop / TP / RR
- No final direction
- No external channel
- No Push send
- No order / execution / auto-trading
- Incomplete-safe / fail-closed remains mandatory
- Risk Action Guard remains mandatory

## Workflow

Created by \`scripts/v1-open-pr.sh\`.
BODY
)"

gh pr create \
  --draft \
  --base main \
  --head "$BRANCH" \
  --title "$TITLE" \
  --body "$body"
