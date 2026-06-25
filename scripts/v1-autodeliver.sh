#!/usr/bin/env bash

set -euo pipefail

EXPECTED_ROOT="/Users/xuchao/Documents/trade-model-v1"

usage() {
  cat <<'EOF'
usage: bash scripts/v1-autodeliver.sh <check|ship|merge|full> [options]

modes:
  check   Read-only delivery summary. Delegates to scripts/v1-delivery-check.sh.
  ship    Run tests and v1-state, commit allowed files, push, and create a ready PR.
  merge   Wait for checks, refuse draft/conflict/failing PRs, squash merge, and pull main.
  full    Run ship, then merge.

options:
  --branch <branch>        Required for ship/full. Must match the current branch.
  --commit <message>       Required for ship/full.
  --title <title>          Required for ship/full. Used for PR title and squash subject.
  --body-file <file>       Required for ship/full.
  --pr <number>            Optional for merge. If omitted, derived from --branch/current branch.
  --base <branch>          PR base branch. Default: main.
  --allow <path-prefix>    Allow changed file path or directory. Repeat as needed.
  --force-with-lease       Use git push --force-with-lease. Never used unless passed.
  --help                   Show this help.

The script never runs git reset, git clean, or local file deletion.
EOF
}

die() {
  echo "AUTODELIVER_STATUS: FAIL" >&2
  echo "REASON: $*" >&2
  exit 1
}

require_project_path() {
  local root
  root="$(pwd -P)"
  [[ "$root" == "$EXPECTED_ROOT" ]] || die "WRONG_PROJECT_PATH current=$root expected=$EXPECTED_ROOT"
}

require_gh() {
  command -v gh >/dev/null 2>&1 || die "GH_NOT_AVAILABLE"
  gh auth status >/dev/null 2>&1 || die "GH_AUTH_NOT_AVAILABLE"
}

print_action() {
  echo "ACTION: $*"
}

MODE="${1:-}"
if [[ -z "$MODE" || "$MODE" == "--help" || "$MODE" == "-h" ]]; then
  usage
  exit 0
fi
shift

BRANCH=""
COMMIT_MSG=""
TITLE=""
BODY_FILE=""
PR_NUMBER=""
BASE_BRANCH="main"
FORCE_WITH_LEASE="false"
ALLOW_PREFIXES=()

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
    die "NO_CHANGED_FILES_TO_SHIP"
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
      WORKTREE_DIRTY|P0_0_DONE_NOT_EFFECTIVE_MERGED_MAIN)
        ;;
      *)
        tail -n 100 "$state_log" >&2
        die "V1_STATE_BLOCKER_UNRELATED_TO_EXPECTED_DIRTY_WORKTREE: $token"
        ;;
    esac
  done
}

require_ship_args() {
  [[ -n "$BRANCH" ]] || die "MISSING_REQUIRED --branch"
  [[ -n "$COMMIT_MSG" ]] || die "MISSING_REQUIRED --commit"
  [[ -n "$TITLE" ]] || die "MISSING_REQUIRED --title"
  [[ -n "$BODY_FILE" ]] || die "MISSING_REQUIRED --body-file"
  [[ -f "$BODY_FILE" ]] || die "BODY_FILE_NOT_FOUND $BODY_FILE"
  (( ${#ALLOW_PREFIXES[@]} > 0 )) || die "MISSING_REQUIRED --allow"
}

current_branch() {
  git branch --show-current
}

ship() {
  require_project_path
  require_ship_args
  require_gh

  local current
  current="$(current_branch)"
  [[ "$current" != "main" ]] || die "REFUSE_COMMIT_ON_MAIN"
  [[ "$current" == "$BRANCH" ]] || die "CURRENT_BRANCH_MISMATCH current=$current expected=$BRANCH"

  verify_allowed_changed_files
  run_tests
  run_v1_state_for_ship

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

  if [[ "$FORCE_WITH_LEASE" == "true" ]]; then
    print_action "git push --force-with-lease -u origin $BRANCH"
    git push --force-with-lease -u origin "$BRANCH"
  else
    print_action "git push -u origin $BRANCH"
    git push -u origin "$BRANCH"
  fi

  PR_NUMBER="$(gh pr list --head "$BRANCH" --state open --json number --jq '.[0].number // ""')"
  if [[ -n "$PR_NUMBER" ]]; then
    echo "PR_ALREADY_EXISTS: #$PR_NUMBER"
  else
    print_action "gh pr create --base $BASE_BRANCH --head $BRANCH --title \"$TITLE\" --body-file $BODY_FILE"
    gh pr create --base "$BASE_BRANCH" --head "$BRANCH" --title "$TITLE" --body-file "$BODY_FILE"
    PR_NUMBER="$(gh pr list --head "$BRANCH" --state open --json number --jq '.[0].number // ""')"
    [[ -n "$PR_NUMBER" ]] || die "PR_CREATE_DID_NOT_RETURN_OPEN_PR"
  fi

  echo "SHIP_SUMMARY: PASS"
  echo "BRANCH: $BRANCH"
  echo "PR_NUMBER: $PR_NUMBER"
}

derive_pr_number() {
  if [[ -n "$PR_NUMBER" ]]; then
    return 0
  fi
  local head="${BRANCH:-}"
  if [[ -z "$head" ]]; then
    head="$(current_branch)"
  fi
  PR_NUMBER="$(gh pr list --head "$head" --state open --json number --jq '.[0].number // ""')"
  [[ -n "$PR_NUMBER" ]] || die "PR_NUMBER_NOT_FOUND_FOR_HEAD $head"
}

assert_pr_mergeable() {
  local state is_draft mergeable merge_state
  state="$(gh pr view "$PR_NUMBER" --json state --jq '.state')"
  is_draft="$(gh pr view "$PR_NUMBER" --json isDraft --jq '.isDraft')"
  mergeable="$(gh pr view "$PR_NUMBER" --json mergeable --jq '.mergeable')"
  merge_state="$(gh pr view "$PR_NUMBER" --json mergeStateStatus --jq '.mergeStateStatus')"

  [[ "$state" == "OPEN" ]] || die "PR_NOT_OPEN state=$state"
  [[ "$is_draft" != "true" ]] || die "PR_IS_DRAFT"
  [[ "$mergeable" == "MERGEABLE" ]] || die "PR_NOT_MERGEABLE mergeable=$mergeable"
  case "$merge_state" in
    CLEAN|HAS_HOOKS|UNSTABLE)
      ;;
    *)
      die "PR_MERGE_STATE_NOT_ALLOWED mergeStateStatus=$merge_state"
      ;;
  esac
}

wait_for_checks() {
  print_action "gh pr checks $PR_NUMBER --watch"
  if ! gh pr checks "$PR_NUMBER" --watch --interval 10 >"$checks_log" 2>&1; then
    tail -n 100 "$checks_log" >&2
    die "GITHUB_CHECKS_FAILED"
  fi

  local failing
  failing="$(gh pr checks "$PR_NUMBER" --json name,bucket --jq '[.[] | select(.bucket != "pass" and .bucket != "skipping")] | length')"
  [[ "$failing" == "0" ]] || die "GITHUB_CHECKS_NOT_ALL_PASSING count=$failing"
  echo "GITHUB_CHECKS: PASS"
}

merge_pr() {
  require_project_path
  require_gh
  derive_pr_number
  wait_for_checks
  assert_pr_mergeable

  local subject="${TITLE:-}"
  if [[ -z "$subject" ]]; then
    subject="$(gh pr view "$PR_NUMBER" --json title --jq '.title')"
  fi

  print_action "gh pr merge $PR_NUMBER --squash --delete-branch --subject \"$subject\""
  gh pr merge "$PR_NUMBER" --squash --delete-branch --subject "$subject"

  print_action "git switch main"
  git switch main
  print_action "git pull --ff-only origin main"
  git pull --ff-only origin main
  print_action "bash scripts/v1-state.sh"
  bash scripts/v1-state.sh

  echo "MERGE_SUMMARY: PASS"
  echo "PR_NUMBER: $PR_NUMBER"
  echo "BRANCH: main"
}

require_project_path

case "$MODE" in
  check)
    bash scripts/v1-delivery-check.sh
    ;;
  ship)
    ship
    ;;
  merge)
    merge_pr
    ;;
  full)
    ship
    merge_pr
    ;;
  *)
    usage >&2
    die "UNKNOWN_MODE $MODE"
    ;;
esac

echo "AUTODELIVER_STATUS: PASS"
echo "MODE: $MODE"
echo "BRANCH: $(current_branch)"
echo "PR_NUMBER: ${PR_NUMBER:-none}"
