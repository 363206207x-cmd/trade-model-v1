#!/usr/bin/env bash

set -euo pipefail

EXPECTED_ROOT="/Users/xuchao/Documents/trade-model-v1"
GH_RETRY_ATTEMPTS="${GH_RETRY_ATTEMPTS:-4}"
GH_RETRY_SLEEP_SECONDS="${GH_RETRY_SLEEP_SECONDS:-5}"

MODE="${1:-}"
BRANCH=""
COMMIT_MSG=""
TITLE=""
BODY_FILE=""
PR_NUMBER=""
PR_URL=""
BASE_BRANCH="main"
FORCE_WITH_LEASE="false"
ALLOW_PREFIXES=()

AUTODELIVER_STATUS="FAIL"
SHIP_STATUS="NOT_STARTED"
CHECKS_STATUS="NOT_STARTED"
MERGE_STATUS="NOT_STARTED"
DELIVERY_CHECK_STATUS="NOT_RUN"
NEXT_STEP="not_started"
RESUME_COMMAND=""
COMMIT_SHA=""
FINAL_BRANCH=""

usage() {
  cat <<'EOF'
usage: bash scripts/v1-autodeliver.sh <check|ship|merge|full|resume> [options]

modes:
  check   Read-only delivery summary. Delegates to scripts/v1-delivery-check.sh.
  ship    Run tests and v1-state, commit allowed files, push, and create/reuse a ready PR.
  merge   Wait for checks, refuse draft/conflict/failing PRs, squash merge, and pull main.
  full    Run ship, then merge.
  resume  Resume after commit/push/PR/check/merge interruption for a branch.

options:
  --branch <branch>        Required for ship/full/resume. Must match current branch for ship/full.
  --commit <message>       Required for ship/full when local changes need committing.
  --title <title>          PR title and squash subject. Falls back to latest commit subject in resume.
  --body-file <file>       PR body. Falls back to a generated resume body when omitted in resume.
  --pr <number>            Optional for merge/resume. If omitted, derived from --branch/current branch.
  --base <branch>          PR base branch. Default: main.
  --allow <path-prefix>    Allow changed file path or directory. Repeat as needed.
  --force-with-lease       Use git push --force-with-lease. Never used unless passed.
  --help                   Show this help.

The script never runs git reset, git clean, or local repository file deletion.
EOF
}

current_branch() {
  git branch --show-current 2>/dev/null || true
}

summary_branch() {
  if [[ -n "${BRANCH:-}" ]]; then
    echo "$BRANCH"
  else
    current_branch
  fi
}

summary_commit() {
  if [[ -n "${COMMIT_SHA:-}" ]]; then
    echo "$COMMIT_SHA"
  else
    git rev-parse --short HEAD 2>/dev/null || echo "unknown"
  fi
}

set_resume_command() {
  local branch
  branch="$(summary_branch)"
  if [[ -n "$branch" && "$branch" != "main" ]]; then
    RESUME_COMMAND="bash scripts/v1-autodeliver.sh resume --branch $branch"
  fi
}

print_final_summary() {
  local status="${1:-$AUTODELIVER_STATUS}"
  FINAL_BRANCH="$(current_branch)"
  COMMIT_SHA="$(summary_commit)"
  echo "AUTODELIVER_STATUS: $status"
  echo "MODE: ${MODE:-UNKNOWN}"
  echo "BRANCH: $(summary_branch)"
  echo "COMMIT: ${COMMIT_SHA:-unknown}"
  echo "PR_NUMBER: ${PR_NUMBER:-none}"
  echo "PR_URL: ${PR_URL:-none}"
  echo "SHIP_STATUS: $SHIP_STATUS"
  echo "CHECKS_STATUS: $CHECKS_STATUS"
  echo "MERGE_STATUS: $MERGE_STATUS"
  echo "FINAL_BRANCH: ${FINAL_BRANCH:-UNKNOWN}"
  echo "DELIVERY_CHECK_STATUS: $DELIVERY_CHECK_STATUS"
  echo "NEXT_STEP: ${NEXT_STEP:-none}"
  if [[ -n "${RESUME_COMMAND:-}" ]]; then
    echo "RESUME_COMMAND: $RESUME_COMMAND"
  fi
}

die() {
  local reason="$*"
  AUTODELIVER_STATUS="FAIL"
  NEXT_STEP="fix failure reason and rerun the safe mode"
  set_resume_command
  echo "REASON: $reason" >&2
  print_final_summary "$AUTODELIVER_STATUS" >&2
  exit 1
}

block() {
  local reason="$*"
  AUTODELIVER_STATUS="BLOCKED"
  NEXT_STEP="$reason"
  set_resume_command
  echo "REASON: $reason" >&2
  print_final_summary "$AUTODELIVER_STATUS" >&2
  exit 2
}

print_action() {
  echo "ACTION: $*"
}

require_project_path() {
  local root
  root="$(pwd -P)"
  [[ "$root" == "$EXPECTED_ROOT" ]] || die "WRONG_PROJECT_PATH current=$root expected=$EXPECTED_ROOT"
}

is_retryable_gh_error() {
  local text
  text="$(printf '%s' "$1" | tr '[:upper:]' '[:lower:]')"
  case "$text" in
    *eof*|*timeout*|*"timed out"*|*"connection reset"*|*"connection refused"*|*"temporary failure"*|*"tls handshake"*|*"bad gateway"*|*"service unavailable"*|*"gateway timeout"*|*"api.github.com"*500*|*"api.github.com"*502*|*"api.github.com"*503*|*"api.github.com"*504*|*graphql*eof*|*graphql*timeout*)
      return 0
      ;;
    *)
      return 1
      ;;
  esac
}

gh_retry() {
  local attempt output rc
  for attempt in $(seq 1 "$GH_RETRY_ATTEMPTS"); do
    echo "GH_RETRY_ATTEMPT: $attempt/$GH_RETRY_ATTEMPTS gh $*" >&2
    set +e
    output="$(gh "$@" 2>&1)"
    rc=$?
    set -e

    if [[ "$rc" -eq 0 ]]; then
      printf '%s\n' "$output"
      return 0
    fi

    if [[ "$attempt" -lt "$GH_RETRY_ATTEMPTS" ]] && is_retryable_gh_error "$output"; then
      printf '%s\n' "$output" >&2
      sleep "$GH_RETRY_SLEEP_SECONDS"
      continue
    fi

    printf '%s\n' "$output" >&2
    return "$rc"
  done
}

require_gh() {
  command -v gh >/dev/null 2>&1 || die "GH_NOT_AVAILABLE"
  gh_retry auth status >/dev/null || die "GH_AUTH_NOT_AVAILABLE"
}

if [[ -z "$MODE" || "$MODE" == "--help" || "$MODE" == "-h" ]]; then
  usage
  exit 0
fi
shift

while [[ "$#" -gt 0 ]]; do
  case "$1" in
    --branch)
      [[ "$#" -ge 2 ]] || die "MISSING_VALUE --branch"
      BRANCH="$2"
      shift 2
      ;;
    --commit)
      [[ "$#" -ge 2 ]] || die "MISSING_VALUE --commit"
      COMMIT_MSG="$2"
      shift 2
      ;;
    --title)
      [[ "$#" -ge 2 ]] || die "MISSING_VALUE --title"
      TITLE="$2"
      shift 2
      ;;
    --body-file)
      [[ "$#" -ge 2 ]] || die "MISSING_VALUE --body-file"
      BODY_FILE="$2"
      shift 2
      ;;
    --pr)
      [[ "$#" -ge 2 ]] || die "MISSING_VALUE --pr"
      PR_NUMBER="$2"
      shift 2
      ;;
    --base)
      [[ "$#" -ge 2 ]] || die "MISSING_VALUE --base"
      BASE_BRANCH="$2"
      shift 2
      ;;
    --allow)
      [[ "$#" -ge 2 ]] || die "MISSING_VALUE --allow"
      ALLOW_PREFIXES+=("${2%/}")
      shift 2
      ;;
    --force-with-lease)
      FORCE_WITH_LEASE="true"
      shift
      ;;
    --help|-h)
      usage
      exit 0
      ;;
    *)
      die "UNKNOWN_OPTION $1"
      ;;
  esac
done

tmp_dir="$(mktemp -d)"
trap 'rm -rf "$tmp_dir"' EXIT

changed_file_list="$tmp_dir/changed-files.txt"
test_log="$tmp_dir/maven-test.log"
state_log="$tmp_dir/v1-state.log"
checks_log="$tmp_dir/pr-checks.log"
delivery_check_log="$tmp_dir/delivery-check.log"

collect_changed_files() {
  {
    git diff --name-only
    git diff --cached --name-only
    git ls-files --others --exclude-standard
  } | sort -u >"$changed_file_list"
}

path_allowed() {
  local path="$1"
  local prefix
  for prefix in "${ALLOW_PREFIXES[@]}"; do
    [[ -n "$prefix" ]] || continue
    if [[ "$path" == "$prefix" || "$path" == "$prefix"/* ]]; then
      return 0
    fi
  done
  return 1
}

verify_allowed_changed_files() {
  local forbidden=()
  local file
  collect_changed_files
  if [[ ! -s "$changed_file_list" ]]; then
    return 1
  fi
  echo "CHANGED_FILES:"
  sed 's/^/- /' "$changed_file_list"
  while IFS= read -r file; do
    [[ -n "$file" ]] || continue
    if ! path_allowed "$file"; then
      forbidden+=("$file")
    fi
  done <"$changed_file_list"
  if (( ${#forbidden[@]} > 0 )); then
    echo "DISALLOWED_CHANGED_FILES:" >&2
    printf '%s\n' "${forbidden[@]}" | sed 's/^/- /' >&2
    die "CHANGED_FILES_OUTSIDE_ALLOWLIST"
  fi
  return 0
}

run_tests() {
  print_action "./mvnw test -q"
  if ./mvnw test -q >"$test_log" 2>&1; then
    echo "TEST_RESULT: PASS"
  else
    echo "TEST_RESULT: FAIL" >&2
    tail -n 100 "$test_log" >&2
    die "MAVEN_TEST_FAILED"
  fi
}

state_field() {
  local key="$1"
  awk -F': ' -v key="$key" '$1 == key { print substr($0, length(key) + 3); exit }' "$state_log"
}

run_v1_state_for_ship() {
  print_action "bash scripts/v1-state.sh"
  if ! bash scripts/v1-state.sh >"$state_log" 2>&1; then
    tail -n 100 "$state_log" >&2
    die "V1_STATE_SCRIPT_FAILED"
  fi
  local blockers
  blockers="$(state_field BLOCKERS || true)"
  echo "V1_STATE_BLOCKERS: ${blockers:-UNKNOWN}"
  if [[ -z "$blockers" || "$blockers" == "none" ]]; then
    return 0
  fi
  local token
  for token in $blockers; do
    case "$token" in
      WORKTREE_DIRTY|P0_0_DONE_NOT_EFFECTIVE_MERGED_MAIN|P0_0_DONE_PENDING_MERGED_MAIN)
        ;;
      *)
        tail -n 100 "$state_log" >&2
        die "V1_STATE_BLOCKER_UNRELATED_TO_EXPECTED_TASK_BRANCH: $token"
        ;;
    esac
  done
}

run_delivery_check() {
  print_action "bash scripts/v1-delivery-check.sh"
  if bash scripts/v1-delivery-check.sh >"$delivery_check_log" 2>&1; then
    DELIVERY_CHECK_STATUS="PASS"
    cat "$delivery_check_log"
  else
    DELIVERY_CHECK_STATUS="FAIL"
    tail -n 120 "$delivery_check_log" >&2
    return 1
  fi
}

require_ship_args() {
  [[ -n "$BRANCH" ]] || die "MISSING_REQUIRED --branch"
  [[ "$BRANCH" != "main" ]] || die "REFUSE_COMMIT_ON_MAIN"
  if [[ -s "$changed_file_list" ]]; then
    [[ -n "$COMMIT_MSG" ]] || die "MISSING_REQUIRED --commit"
    (( ${#ALLOW_PREFIXES[@]} > 0 )) || die "MISSING_REQUIRED --allow"
  fi
}

local_branch_exists() {
  git show-ref --verify --quiet "refs/heads/$1"
}

remote_branch_exists() {
  git ls-remote --exit-code --heads origin "$1" >/dev/null 2>&1
}

ensure_on_branch() {
  local branch="$1"
  local current
  current="$(current_branch)"
  if [[ "$current" == "$branch" ]]; then
    return 0
  fi
  if local_branch_exists "$branch"; then
    print_action "git switch $branch"
    git switch "$branch"
    return 0
  fi
  if remote_branch_exists "$branch"; then
    print_action "git fetch origin $branch"
    git fetch origin "$branch"
    print_action "git switch --track -c $branch origin/$branch"
    git switch --track -c "$branch" "origin/$branch"
    return 0
  fi
  block "BRANCH_NOT_FOUND_LOCAL_OR_REMOTE branch=$branch"
}

ensure_clean_worktree_for_resume() {
  collect_changed_files
  if [[ -s "$changed_file_list" ]]; then
    echo "CHANGED_FILES:" >&2
    sed 's/^/- /' "$changed_file_list" >&2
    block "DIRTY_WORKTREE_BEFORE_RESUME"
  fi
}

branch_remote_sha() {
  git ls-remote --heads origin "$1" | awk '{ print $1; exit }'
}

ensure_branch_pushed() {
  local branch="$1"
  local local_sha remote_sha
  ensure_on_branch "$branch"
  local_sha="$(git rev-parse "$branch")"
  remote_sha="$(branch_remote_sha "$branch" || true)"

  if [[ -z "$remote_sha" ]]; then
    if [[ "$FORCE_WITH_LEASE" == "true" ]]; then
      print_action "git push --force-with-lease -u origin $branch"
      git push --force-with-lease -u origin "$branch"
    else
      print_action "git push -u origin $branch"
      git push -u origin "$branch"
    fi
    SHIP_STATUS="PUSHED"
    return 0
  fi

  if [[ "$remote_sha" == "$local_sha" ]]; then
    echo "REMOTE_BRANCH_STATUS: already_pushed"
    SHIP_STATUS="ALREADY_PUSHED"
    return 0
  fi

  print_action "git fetch origin $branch"
  git fetch origin "$branch"
  if git merge-base --is-ancestor "origin/$branch" "$branch"; then
    if [[ "$FORCE_WITH_LEASE" == "true" ]]; then
      print_action "git push --force-with-lease -u origin $branch"
      git push --force-with-lease -u origin "$branch"
    else
      print_action "git push -u origin $branch"
      git push -u origin "$branch"
    fi
    SHIP_STATUS="PUSHED"
    return 0
  fi

  block "REMOTE_BRANCH_DIVERGED branch=$branch"
}

find_pr_for_branch() {
  local branch="$1"
  local info
  info="$(gh_retry pr list --head "$branch" --state all --json number,url,state --jq 'sort_by(.number) | reverse | .[0] | select(. != null) | [.number, .url, .state] | @tsv')"
  if [[ -z "$info" ]]; then
    return 1
  fi
  IFS=$'\t' read -r PR_NUMBER PR_URL _pr_state <<EOF
$info
EOF
  return 0
}

refresh_pr_metadata() {
  local info
  [[ -n "$PR_NUMBER" ]] || return 1
  info="$(gh_retry pr view "$PR_NUMBER" --json number,url --jq '[.number, .url] | @tsv')"
  IFS=$'\t' read -r PR_NUMBER PR_URL <<EOF
$info
EOF
}

pr_state() {
  gh_retry pr view "$PR_NUMBER" --json state --jq '.state'
}

latest_commit_subject() {
  git log -1 --pretty=%s "${BRANCH:-HEAD}" 2>/dev/null || echo "Autonomous delivery resume"
}

ensure_pr_exists() {
  local branch="$1"
  local state create_output pr_title pr_body
  BRANCH="$branch"

  if [[ -n "$PR_NUMBER" ]]; then
    refresh_pr_metadata || die "PR_NUMBER_NOT_FOUND #$PR_NUMBER"
    state="$(pr_state)"
    echo "PR_STATUS: #$PR_NUMBER $state"
    return 0
  fi

  if find_pr_for_branch "$branch"; then
    state="$(pr_state)"
    echo "PR_ALREADY_EXISTS: #$PR_NUMBER $state"
    return 0
  fi

  pr_title="${TITLE:-}"
  if [[ -z "$pr_title" ]]; then
    pr_title="$(latest_commit_subject)"
    TITLE="$pr_title"
  fi

  if [[ -n "$BODY_FILE" ]]; then
    [[ -f "$BODY_FILE" ]] || die "BODY_FILE_NOT_FOUND $BODY_FILE"
    pr_body="$BODY_FILE"
  else
    pr_body="$tmp_dir/resume-pr-body.md"
    {
      echo "Autonomous delivery resume for branch $branch."
      echo
      echo "Commit: $(git rev-parse --short "$branch" 2>/dev/null || echo unknown)"
      echo
      echo "Created by scripts/v1-autodeliver.sh resume after an interrupted delivery flow."
      echo
      echo "Safety:"
      echo "- No auto-trading, order, execution, Push send, or business capability change is implied by this workflow helper."
    } >"$pr_body"
  fi

  print_action "gh pr create --base $BASE_BRANCH --head $branch --title \"$pr_title\" --body-file $pr_body"
  create_output="$(gh_retry pr create --base "$BASE_BRANCH" --head "$branch" --title "$pr_title" --body-file "$pr_body")"
  printf '%s\n' "$create_output"
  if ! find_pr_for_branch "$branch"; then
    die "PR_CREATE_DID_NOT_RETURN_PR_FOR_BRANCH $branch"
  fi
  echo "PR_CREATED: #$PR_NUMBER"
}

derive_pr_number() {
  if [[ -n "$PR_NUMBER" ]]; then
    refresh_pr_metadata || die "PR_NUMBER_NOT_FOUND #$PR_NUMBER"
    return 0
  fi
  local head="${BRANCH:-}"
  if [[ -z "$head" ]]; then
    head="$(current_branch)"
  fi
  if ! find_pr_for_branch "$head"; then
    die "PR_NUMBER_NOT_FOUND_FOR_HEAD $head"
  fi
}

assert_pr_mergeable() {
  local info state is_draft mergeable merge_state pr_title url
  info="$(gh_retry pr view "$PR_NUMBER" --json state,isDraft,mergeable,mergeStateStatus,title,url --jq '[.state, .isDraft, .mergeable, .mergeStateStatus, .title, .url] | @tsv')"
  IFS=$'\t' read -r state is_draft mergeable merge_state pr_title url <<EOF
$info
EOF
  PR_URL="$url"
  [[ "$state" == "OPEN" ]] || block "PR_NOT_OPEN state=$state"
  [[ "$is_draft" != "true" ]] || block "PR_IS_DRAFT"
  [[ "$mergeable" == "MERGEABLE" ]] || block "PR_NOT_MERGEABLE mergeable=$mergeable"
  case "$merge_state" in
    CLEAN|HAS_HOOKS|UNSTABLE)
      ;;
    *)
      block "PR_MERGE_STATE_NOT_ALLOWED mergeStateStatus=$merge_state"
      ;;
  esac
  if [[ -z "$TITLE" ]]; then
    TITLE="$pr_title"
  fi
}

wait_for_checks() {
  print_action "gh pr checks $PR_NUMBER --watch"
  if ! gh_retry pr checks "$PR_NUMBER" --watch --interval 10 >"$checks_log" 2>&1; then
    CHECKS_STATUS="FAIL"
    tail -n 100 "$checks_log" >&2
    block "GITHUB_CHECKS_FAILED"
  fi

  local failing
  failing="$(gh_retry pr checks "$PR_NUMBER" --json name,bucket --jq '[.[] | select((.bucket != "pass") and (.bucket != "skipping"))] | length')"
  [[ "$failing" == "0" ]] || block "GITHUB_CHECKS_NOT_ALL_PASSING count=$failing"
  CHECKS_STATUS="PASS"
  echo "GITHUB_CHECKS: PASS"
}

sync_main_and_check() {
  print_action "git switch main"
  git switch main
  print_action "git pull --ff-only origin main"
  git pull --ff-only origin main
  if run_delivery_check; then
    DELIVERY_CHECK_STATUS="PASS"
  else
    DELIVERY_CHECK_STATUS="FAIL"
    die "FINAL_DELIVERY_CHECK_FAILED"
  fi
}

ship() {
  require_project_path
  require_gh
  [[ -n "$BRANCH" ]] || die "MISSING_REQUIRED --branch"

  local current
  current="$(current_branch)"
  [[ "$current" != "main" ]] || die "REFUSE_COMMIT_ON_MAIN"
  [[ "$current" == "$BRANCH" ]] || die "CURRENT_BRANCH_MISMATCH current=$current expected=$BRANCH"

  collect_changed_files
  require_ship_args
  run_tests
  run_v1_state_for_ship

  if [[ -s "$changed_file_list" ]]; then
    verify_allowed_changed_files

    local files=()
    local file
    while IFS= read -r file; do
      [[ -n "$file" ]] && files+=("$file")
    done <"$changed_file_list"

    print_action "git add allowed changed files"
    git add -- "${files[@]}"

    if git diff --cached --quiet; then
      die "NO_STAGED_CHANGES_AFTER_ALLOWLIST_ADD"
    fi

    print_action "git commit -m \"$COMMIT_MSG\""
    git commit -m "$COMMIT_MSG"
    COMMIT_SHA="$(git rev-parse --short HEAD)"
    SHIP_STATUS="COMMITTED"
  else
    echo "NO_LOCAL_CHANGES_TO_COMMIT: using existing branch commit"
    COMMIT_SHA="$(git rev-parse --short HEAD)"
    SHIP_STATUS="NO_LOCAL_CHANGES"
  fi

  ensure_branch_pushed "$BRANCH"
  ensure_pr_exists "$BRANCH"
  state="$(pr_state)"
  if [[ "$state" == "MERGED" ]]; then
    MERGE_STATUS="ALREADY_MERGED"
    SHIP_STATUS="ALREADY_MERGED"
  elif [[ "$state" == "CLOSED" ]]; then
    block "PR_CLOSED_NOT_MERGED #$PR_NUMBER"
  else
    SHIP_STATUS="PASS"
  fi

  echo "SHIP_SUMMARY: $SHIP_STATUS"
  echo "BRANCH: $BRANCH"
  echo "COMMIT: $COMMIT_SHA"
  echo "PR_NUMBER: ${PR_NUMBER:-none}"
  echo "PR_URL: ${PR_URL:-none}"
}

merge_pr() {
  require_project_path
  require_gh
  derive_pr_number

  local state subject
  state="$(pr_state)"
  if [[ "$state" == "MERGED" ]]; then
    echo "PR_ALREADY_MERGED: #$PR_NUMBER"
    MERGE_STATUS="ALREADY_MERGED"
    sync_main_and_check
    return 0
  fi
  [[ "$state" == "OPEN" ]] || block "PR_NOT_OPEN state=$state"

  wait_for_checks
  assert_pr_mergeable

  subject="${TITLE:-}"
  if [[ -z "$subject" ]]; then
    subject="$(gh_retry pr view "$PR_NUMBER" --json title --jq '.title')"
  fi

  print_action "gh pr merge $PR_NUMBER --squash --delete-branch --subject \"$subject\""
  gh_retry pr merge "$PR_NUMBER" --squash --delete-branch --subject "$subject"
  MERGE_STATUS="PASS"
  sync_main_and_check

  echo "MERGE_SUMMARY: PASS"
  echo "PR_NUMBER: $PR_NUMBER"
  echo "PR_URL: ${PR_URL:-none}"
  echo "BRANCH: main"
}

resume() {
  require_project_path
  require_gh
  if [[ -z "$BRANCH" ]]; then
    BRANCH="$(current_branch)"
  fi
  [[ -n "$BRANCH" ]] || die "MISSING_REQUIRED --branch"
  [[ "$BRANCH" != "main" ]] || die "RESUME_REQUIRES_TASK_BRANCH"

  if [[ -n "$PR_NUMBER" ]]; then
    refresh_pr_metadata || die "PR_NUMBER_NOT_FOUND #$PR_NUMBER"
  elif find_pr_for_branch "$BRANCH"; then
    echo "RESUME_FOUND_PR: #$PR_NUMBER"
  fi

  if [[ -n "$PR_NUMBER" ]]; then
    local state
    state="$(pr_state)"
    if [[ "$state" == "MERGED" ]]; then
      MERGE_STATUS="ALREADY_MERGED"
      SHIP_STATUS="ALREADY_MERGED"
      sync_main_and_check
      return 0
    fi
    [[ "$state" != "CLOSED" ]] || block "PR_CLOSED_NOT_MERGED #$PR_NUMBER"
  fi

  ensure_on_branch "$BRANCH"
  ensure_clean_worktree_for_resume
  COMMIT_SHA="$(git rev-parse --short HEAD)"
  ensure_branch_pushed "$BRANCH"
  ensure_pr_exists "$BRANCH"

  local state
  state="$(pr_state)"
  if [[ "$state" == "MERGED" ]]; then
    MERGE_STATUS="ALREADY_MERGED"
    SHIP_STATUS="ALREADY_MERGED"
    sync_main_and_check
    return 0
  fi
  [[ "$state" == "OPEN" ]] || block "PR_NOT_OPEN state=$state"
  SHIP_STATUS="PASS"
  merge_pr
}

require_project_path

case "$MODE" in
  check)
    if run_delivery_check; then
      AUTODELIVER_STATUS="PASS"
      NEXT_STEP="delivery check passed"
    else
      AUTODELIVER_STATUS="FAIL"
      NEXT_STEP="fix delivery check failure"
      print_final_summary "$AUTODELIVER_STATUS"
      exit 1
    fi
    ;;
  ship)
    ship
    AUTODELIVER_STATUS="PASS"
    NEXT_STEP="run resume or merge when ready"
    ;;
  merge)
    merge_pr
    AUTODELIVER_STATUS="PASS"
    NEXT_STEP="delivery merged and main synced"
    ;;
  full)
    ship
    if [[ "$MERGE_STATUS" == "ALREADY_MERGED" ]]; then
      sync_main_and_check
    else
      merge_pr
    fi
    AUTODELIVER_STATUS="PASS"
    NEXT_STEP="delivery merged and main synced"
    ;;
  resume)
    resume
    AUTODELIVER_STATUS="PASS"
    NEXT_STEP="delivery resumed, merged, and main synced"
    ;;
  *)
    usage >&2
    die "UNKNOWN_MODE $MODE"
    ;;
esac

print_final_summary "$AUTODELIVER_STATUS"
