#!/usr/bin/env bash

set -euo pipefail

usage() {
  cat <<'EOF'
usage: bash scripts/v1-pr-flow-helper.sh --branch <branch> --title "<title>" --risk <A|B|B/C|C> [--body-file <file>]

Prints the fixed PR flow commands. It does not create, merge, edit files, stage, or commit.
EOF
}

if [[ "$#" -eq 0 || "${1:-}" == "--help" || "${1:-}" == "-h" ]]; then
  usage
  exit 0
fi

BRANCH=""
TITLE=""
RISK=""
BODY_FILE=""

while [[ "$#" -gt 0 ]]; do
  case "$1" in
    --branch)
      BRANCH="${2:-}"
      shift 2
      ;;
    --title)
      TITLE="${2:-}"
      shift 2
      ;;
    --risk)
      RISK="${2:-}"
      shift 2
      ;;
    --body-file)
      BODY_FILE="${2:-}"
      shift 2
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

if [[ -z "$BRANCH" || -z "$TITLE" || -z "$RISK" ]]; then
  echo "MISSING_REQUIRED_ARGUMENT" >&2
  usage >&2
  exit 1
fi

case "$RISK" in
  A|B|"B/C"|C)
    ;;
  *)
    echo "UNSUPPORTED_RISK: $RISK" >&2
    exit 1
    ;;
esac

open_cmd="bash scripts/v1-open-pr.sh $BRANCH \"$TITLE\" $RISK"
if [[ -n "$BODY_FILE" ]]; then
  open_cmd="$open_cmd --body-file $BODY_FILE"
fi

echo "Open PR:"
echo "$open_cmd"
echo
echo "Check PR:"
echo "gh pr checks <PR_NUMBER>"
echo
echo "Merge and sync:"
if [[ "$RISK" == "A" ]]; then
  echo "bash scripts/v1-merge-sync.sh <PR_NUMBER> \"$TITLE (#<PR_NUMBER>)\" --risk A"
else
  echo "bash scripts/v1-merge-sync.sh <PR_NUMBER> \"$TITLE (#<PR_NUMBER>)\" --risk $RISK --confirm"
  echo
  echo "NOTE: risk $RISK requires explicit user approval before --confirm."
fi
