#!/usr/bin/env bash

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

blockers=()

branch="$(git branch --show-current)"
status_short="$(git status --short)"
head_commit="$(git log -1 --oneline)"

if [[ -z "$status_short" ]]; then
  worktree_clean="Yes"
else
  worktree_clean="No"
  blockers+=("WORKTREE_DIRTY")
fi

if [[ "$branch" != "main" ]]; then
  blockers+=("NOT_ON_MAIN")
fi

main_sync="UNKNOWN"
if git rev-parse --verify main >/dev/null 2>&1 && git rev-parse --verify origin/main >/dev/null 2>&1; then
  read -r main_ahead main_behind < <(git rev-list --left-right --count main...origin/main)
  if [[ "$main_behind" == "0" && "$main_ahead" == "0" ]]; then
    main_sync="OK"
  elif [[ "$main_behind" != "0" ]]; then
    main_sync="BEHIND_ORIGIN_MAIN by $main_behind"
    blockers+=("MAIN_BEHIND_ORIGIN")
  else
    main_sync="AHEAD_ORIGIN_MAIN by $main_ahead"
  fi
else
  main_sync="UNKNOWN_ORIGIN_MAIN"
  blockers+=("MAIN_SYNC_UNKNOWN")
fi

open_prs="none"
if command -v gh >/dev/null 2>&1 && gh auth status >/dev/null 2>&1; then
  if ! pr_lines="$(gh pr list --state open --json number,title,headRefName,isDraft --jq '.[] | "#\(.number) \(.headRefName) \(.title) draft=\(.isDraft)"' 2>/dev/null)"; then
    pr_lines=""
    open_prs="GH_NOT_AVAILABLE"
    blockers+=("OPEN_PR_STATUS_UNKNOWN_GH_NOT_AVAILABLE")
  fi
  if [[ -n "$pr_lines" ]]; then
    open_prs="$pr_lines"
    blockers+=("OPEN_PR_EXISTS")
  fi
else
  open_prs="GH_NOT_AVAILABLE"
  blockers+=("OPEN_PR_STATUS_UNKNOWN_GH_NOT_AVAILABLE")
fi

can_continue="YES"
if (( ${#blockers[@]} > 0 )); then
  can_continue="NO"
fi

echo "BRANCH: $branch"
echo "WORKTREE_CLEAN: $worktree_clean"
echo "HEAD: $head_commit"
echo "RECENT_COMMITS:"
git log --oneline -5
echo "OPEN_PRS: $open_prs"
echo "MAIN_SYNC: $main_sync"
echo "CAN_CONTINUE_NEXT_PACKAGE: $can_continue"
if (( ${#blockers[@]} == 0 )); then
  echo "BLOCKERS: none"
else
  echo "BLOCKERS: ${blockers[*]}"
fi
