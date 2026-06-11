#!/usr/bin/env bash

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

usage() {
  cat <<'EOF'
usage: bash scripts/v1-merge-sync.sh <PR_NUMBER> "<SQUASH_SUBJECT>" [options]

options:
  --risk <A|B|B/C|C>  Merge risk class. Default: A for backward-compatible docs-only use.
  --confirm           Required for B, B/C, and C risk merges after explicit user approval.
  --no-confirm        Explicitly state that no user approval is present.
  --dry-run           Print the planned action without merging.
  --help              Show this help.

The script does not generate the next package and does not bypass approval rules.
EOF
}

if [[ "${1:-}" == "--help" || "${1:-}" == "-h" ]]; then
  usage
  exit 0
fi

if [[ "$#" -lt 2 ]]; then
  usage >&2
  exit 1
fi

PR_NUMBER="$1"
SQUASH_SUBJECT="$2"
shift 2

RISK="A"
CONFIRM="false"
DRY_RUN="false"

while [[ "$#" -gt 0 ]]; do
  case "$1" in
    --risk)
      if [[ "$#" -lt 2 ]]; then
        echo "MISSING_VALUE: --risk" >&2
        exit 1
      fi
      RISK="$2"
      shift 2
      ;;
    --confirm)
      CONFIRM="true"
      shift
      ;;
    --no-confirm)
      CONFIRM="false"
      shift
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
  A|B|"B/C"|C)
    ;;
  *)
    echo "UNSUPPORTED_RISK: $RISK" >&2
    exit 1
    ;;
esac

if [[ "$RISK" != "A" && "$CONFIRM" != "true" ]]; then
  echo "MERGE_REQUIRES_USER_CONFIRMATION: risk=$RISK requires --confirm after explicit user approval" >&2
  exit 1
fi

if [[ "$DRY_RUN" == "true" ]]; then
  echo "DRY_RUN: v1-merge-sync"
  echo "PR_NUMBER: $PR_NUMBER"
  echo "SQUASH_SUBJECT: $SQUASH_SUBJECT"
  echo "RISK: $RISK"
  echo "CONFIRM: $CONFIRM"
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

sync_main() {
  git switch main
  git pull origin main

  echo
  echo "git status:"
  git status

  echo
  echo "git log --oneline -5:"
  git log --oneline -5

  echo
  echo "v1-state:"
  bash scripts/v1-state.sh || true

  local worktree_clean="No"
  if [[ -z "$(git status --short)" ]]; then
    worktree_clean="Yes"
  fi

  echo
  echo "MERGE_SYNC_DONE"
  echo "WORKTREE_CLEAN: $worktree_clean"
  echo "HEAD: $(git log -1 --oneline)"
  local actual_head source_head
  actual_head="$(git rev-parse --short HEAD 2>/dev/null || true)"
  source_head="$(awk -F': *' '$1 == "current_head" {value=$0; sub("^[^:]*:[[:space:]]*", "", value); gsub(/^"/, "", value); gsub(/"$/, "", value); print value; exit}' docs/ACTIVE_MAINLINE_STATUS.yml 2>/dev/null || true)"
  echo "SOURCE_OF_TRUTH_HEAD: ${source_head:-UNKNOWN}"
  echo "EFFECTIVE_EXECUTION_BASELINE: ${actual_head:-UNKNOWN}"
  if [[ -n "$actual_head" && -n "$source_head" && "$source_head" != "$actual_head" ]]; then
    echo "BASELINE_NOTE: 不再创建 baseline sync（基线同步）小包；下一业务包使用 actual HEAD（实际 HEAD）作为执行基线，并顺手更新 source-of-truth（事实源）。"
  fi
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
  if [[ "$RISK" == "A" || "$CONFIRM" == "true" ]]; then
    gh pr ready "$PR_NUMBER"
  else
    echo "PR_DRAFT_REQUIRES_CONFIRMATION" >&2
    exit 1
  fi
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
