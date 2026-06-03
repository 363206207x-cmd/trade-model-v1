#!/usr/bin/env bash

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

branch="$(git branch --show-current)"
status_short="$(git status --short)"
head_commit="$(git log -1 --oneline)"
worktree_clean="YES"
can_continue="YES"
blockers=()

if [[ -n "$status_short" ]]; then
  worktree_clean="NO"
  can_continue="NO"
  blockers+=("WORKTREE_DIRTY")
fi

main_sync="UNKNOWN"
if git rev-parse --verify origin/main >/dev/null 2>&1; then
  counts="$(git rev-list --left-right --count main...origin/main)"
  main_ahead="${counts%%$'\t'*}"
  main_behind="${counts##*$'\t'}"
  if [[ "$main_behind" != "0" ]]; then
    main_sync="LOCAL_MAIN_BEHIND_ORIGIN_MAIN:$main_behind"
    can_continue="NO"
    blockers+=("MAIN_NOT_SYNCED")
  elif [[ "$main_ahead" != "0" ]]; then
    main_sync="LOCAL_MAIN_AHEAD_ORIGIN_MAIN:$main_ahead"
    can_continue="NO"
    blockers+=("MAIN_NOT_PUSHED_OR_UNEXPECTED_AHEAD")
  else
    main_sync="MAIN_SYNCED_WITH_ORIGIN_MAIN"
  fi
else
  main_sync="ORIGIN_MAIN_REF_MISSING"
  can_continue="NO"
  blockers+=("ORIGIN_MAIN_UNKNOWN")
fi

if [[ "$branch" != "main" ]]; then
  can_continue="NO"
  blockers+=("CURRENT_BRANCH_NOT_MAIN")
fi

open_prs="GH_NOT_AVAILABLE"
current_package_pr="GH_NOT_AVAILABLE"
if command -v gh >/dev/null 2>&1; then
  if gh auth status >/dev/null 2>&1; then
    if open_prs="$(gh pr list --state open --limit 20 --json number,title,headRefName,isDraft,url --template '{{range .}}{{printf "#%v %s head=%s draft=%v %s\n" .number .title .headRefName .isDraft .url}}{{end}}' 2>/dev/null)"; then
      if [[ -z "$open_prs" ]]; then
        open_prs="none"
      else
        can_continue="NO"
        blockers+=("OPEN_PR_EXISTS")
      fi
      if [[ "$branch" != "main" ]]; then
        if current_package_pr="$(gh pr list --head "$branch" --state open --limit 5 --json number,title,url --template '{{range .}}{{printf "#%v %s %s\n" .number .title .url}}{{end}}' 2>/dev/null)"; then
          if [[ -z "$current_package_pr" ]]; then
            current_package_pr="none"
            blockers+=("CURRENT_BRANCH_HAS_NO_OPEN_PR")
          fi
        else
          current_package_pr="GH_NOT_AVAILABLE"
          can_continue="NO"
          blockers+=("GH_NOT_AVAILABLE")
        fi
      else
        current_package_pr="none"
      fi
    else
      open_prs="GH_NOT_AVAILABLE"
      current_package_pr="GH_NOT_AVAILABLE"
      can_continue="NO"
      blockers+=("GH_NOT_AVAILABLE")
    fi
  else
    open_prs="GH_NOT_AVAILABLE"
    current_package_pr="GH_NOT_AVAILABLE"
    can_continue="NO"
    blockers+=("GH_NOT_AVAILABLE")
  fi
else
  can_continue="NO"
  blockers+=("GH_NOT_AVAILABLE")
fi

if [[ "${#blockers[@]}" -eq 0 ]]; then
  blockers_text="none"
else
  blockers_text="$(IFS=,; echo "${blockers[*]}")"
fi

echo "BRANCH: $branch"
echo "WORKTREE_CLEAN: $worktree_clean"
echo "HEAD: $head_commit"
echo "RECENT_COMMITS:"
git log --oneline -5
echo "STATUS_SHORT:"
if [[ -n "$status_short" ]]; then
  printf '%s\n' "$status_short"
else
  echo "clean"
fi
echo "OPEN_PRS:"
printf '%s\n' "$open_prs"
echo "CURRENT_PACKAGE_PR: $current_package_pr"
echo "MAIN_SYNC: $main_sync"
echo "CAN_CONTINUE_NEXT_PACKAGE: $can_continue"
echo "BLOCKERS: $blockers_text"
