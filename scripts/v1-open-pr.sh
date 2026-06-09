#!/usr/bin/env bash

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

usage() {
  cat <<'EOF'
usage: bash scripts/v1-open-pr.sh <branch> "<title>" <risk> [options]

risks:
  A | B | B/C | C

options:
  --body-file <file>  Use a package-specific PR body.
  --ready             Create a ready PR instead of a Draft PR.
  --draft             Force Draft PR mode. This is the default.
  --base <branch>     Base branch. Default: main.
  --dry-run           Print the planned action without creating a PR.
  --help              Show this help.

The script does not merge, edit files, stage files, commit, or start the next package.
EOF
}

if [[ "${1:-}" == "--help" || "${1:-}" == "-h" ]]; then
  usage
  exit 0
fi

if [[ "$#" -lt 3 ]]; then
  usage >&2
  exit 1
fi

BRANCH="$1"
TITLE="$2"
RISK="$3"
shift 3

BODY_FILE=""
BASE_BRANCH="main"
DRAFT_MODE="true"
DRY_RUN="false"

while [[ "$#" -gt 0 ]]; do
  case "$1" in
    --body-file)
      if [[ "$#" -lt 2 ]]; then
        echo "MISSING_VALUE: --body-file" >&2
        exit 1
      fi
      BODY_FILE="$2"
      shift 2
      ;;
    --ready)
      DRAFT_MODE="false"
      shift
      ;;
    --draft)
      DRAFT_MODE="true"
      shift
      ;;
    --base)
      if [[ "$#" -lt 2 ]]; then
        echo "MISSING_VALUE: --base" >&2
        exit 1
      fi
      BASE_BRANCH="$2"
      shift 2
      ;;
    --dry-run)
      DRY_RUN="true"
      shift
      ;;
    --help|-h)
      usage
      exit 0
      ;;
    *)
      echo "UNKNOWN_OPTION: $1" >&2
      usage >&2
      exit 1
      ;;
  esac
done

case "$RISK" in
  A)
    risk_label="Risk: A docs-only"
    risk_note="Docs-only scope. May be ready only when the handoff rule permits it."
    ;;
  B)
    risk_label="Risk: B Java/test/dashboard"
    risk_note="Requires explicit user merge approval if implementation code, tests, or dashboard behavior changed."
    ;;
  "B/C")
    risk_label="Risk: B/C elevated review"
    risk_note="Requires explicit user merge approval."
    ;;
  C)
    risk_label="Risk: C high-risk"
    risk_note="Requires explicit user merge approval; disabled scopes remain disabled unless separately authorized."
    ;;
  *)
    echo "UNSUPPORTED_RISK: $RISK" >&2
    exit 1
    ;;
esac

if ! git rev-parse --is-inside-work-tree >/dev/null 2>&1; then
  echo "NOT_A_GIT_REPOSITORY" >&2
  exit 1
fi

if [[ -n "$BODY_FILE" && ! -f "$BODY_FILE" ]]; then
  echo "BODY_FILE_NOT_FOUND: $BODY_FILE" >&2
  exit 1
fi

if [[ "$DRY_RUN" == "true" ]]; then
  echo "DRY_RUN: v1-open-pr"
  echo "BRANCH: $BRANCH"
  echo "TITLE: $TITLE"
  echo "RISK: $RISK"
  echo "BASE: $BASE_BRANCH"
  echo "DRAFT: $DRAFT_MODE"
  if [[ -n "$BODY_FILE" ]]; then
    echo "BODY_FILE: $BODY_FILE"
  else
    echo "BODY_FILE: default-safe-body"
  fi
  exit 0
fi

if ! command -v gh >/dev/null 2>&1; then
  echo "GH_NOT_AVAILABLE" >&2
  exit 1
fi

if ! gh auth status >/dev/null 2>&1; then
  echo "GH_NOT_AVAILABLE" >&2
  exit 1
fi

if ! git ls-remote --exit-code --heads origin "$BRANCH" >/dev/null 2>&1; then
  echo "REMOTE_BRANCH_NOT_FOUND: $BRANCH" >&2
  exit 1
fi

existing_pr_url="$(gh pr list --head "$BRANCH" --state all --json url --jq '.[0].url // ""')"
if [[ -n "$existing_pr_url" ]]; then
  echo "PR_ALREADY_EXISTS: $existing_pr_url"
  exit 0
fi

tmp_body_file=""
if [[ -n "$BODY_FILE" ]]; then
  body_arg="$BODY_FILE"
else
  tmp_body_file="$(mktemp)"
  trap 'rm -f "$tmp_body_file"' EXIT
  cat >"$tmp_body_file" <<EOF
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
- no Candidate / Decision generation expansion unless explicitly scoped
- no order / execution / auto-trading
- incomplete-safe / fail-closed
- Risk Action Guard remains mandatory

## Workflow

- PR created by fixed workflow script.
- Do not merge until the applicable A/B/C rule is satisfied.
EOF
  body_arg="$tmp_body_file"
fi

create_args=(pr create --base "$BASE_BRANCH" --head "$BRANCH" --title "$TITLE" --body-file "$body_arg")
if [[ "$DRAFT_MODE" == "true" ]]; then
  create_args+=(--draft)
fi

gh "${create_args[@]}"
