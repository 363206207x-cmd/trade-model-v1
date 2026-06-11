#!/usr/bin/env bash

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

TASK_FILE="docs/CODEX_NEXT_TASK.yml"
ACTIVE_FILE="docs/ACTIVE_MAINLINE_STATUS.yml"
TEMPLATE_DIR="docs/CODEX_TASK_TEMPLATES"

usage() {
  cat <<'EOF'
usage: bash scripts/codex-next-task.sh

Reads docs/CODEX_NEXT_TASK.yml and renders a Codex task prompt to stdout.
This script does not modify files, stage, commit, call gh, or run business commands.
EOF
}

if [[ "${1:-}" == "--help" || "${1:-}" == "-h" ]]; then
  usage
  exit 0
fi

if [[ ! -f "$TASK_FILE" ]]; then
  echo "TASK_FILE_NOT_FOUND: $TASK_FILE" >&2
  exit 1
fi

yaml_value() {
  local key="$1"
  awk -F': *' -v key="$key" '
    $1 == key {
      value=$0
      sub("^[^:]*:[[:space:]]*", "", value)
      gsub(/^"/, "", value)
      gsub(/"$/, "", value)
      print value
      exit
    }
  ' "$TASK_FILE"
}

state_value() {
  local state_text="$1"
  local key="$2"
  printf '%s\n' "$state_text" | awk -F': ' -v key="$key" '$1 == key {print substr($0, length(key) + 3); exit}'
}

effective_current_main() {
  local configured="$1"
  local state_text branch worktree open_prs main_sync actual_line
  state_text="$(bash scripts/v1-state.sh 2>/dev/null || true)"
  branch="$(state_value "$state_text" BRANCH)"
  worktree="$(state_value "$state_text" WORKTREE_CLEAN)"
  open_prs="$(state_value "$state_text" OPEN_PRS)"
  main_sync="$(state_value "$state_text" MAIN_SYNC)"
  actual_line="$(git log -1 --oneline 2>/dev/null || true)"

  if [[ "$branch" == "main" && "$worktree" == "Yes" && "$open_prs" == "none" && "$main_sync" == "OK" && -n "$actual_line" ]]; then
    printf '%s\n' "$actual_line"
    return
  fi

  printf '%s\n' "$configured"
}

current_main="$(yaml_value current_main)"
active_block="$(yaml_value active_block)"
phase="$(yaml_value phase)"
module="$(yaml_value module)"
branch="$(yaml_value branch)"
risk="$(yaml_value risk)"
allowed_changes="$(yaml_value allowed_changes)"
forbidden="$(yaml_value forbidden)"
required_reads="$(yaml_value required_reads)"
required_checks="$(yaml_value required_checks)"
expected_output="$(yaml_value expected_output)"
next_allowed_action="$(yaml_value next_allowed_action)"
current_main="$(effective_current_main "$current_main")"

case "$phase" in
  source_read)
    template="$TEMPLATE_DIR/RUNTIME_SOURCE_READ_TEMPLATE.md"
    ;;
  design)
    template="$TEMPLATE_DIR/RUNTIME_DESIGN_TEMPLATE.md"
    ;;
  readiness|readiness_gate)
    template="$TEMPLATE_DIR/RUNTIME_READINESS_GATE_TEMPLATE.md"
    ;;
  implementation)
    template="$TEMPLATE_DIR/RUNTIME_IMPLEMENTATION_TEMPLATE.md"
    ;;
  verification)
    template="$TEMPLATE_DIR/RUNTIME_VERIFICATION_TEMPLATE.md"
    ;;
  visual_closure)
    template="$TEMPLATE_DIR/RUNTIME_VISUAL_CLOSURE_TEMPLATE.md"
    ;;
  selection)
    template="$TEMPLATE_DIR/RUNTIME_SELECTION_TEMPLATE.md"
    ;;
  *)
    echo "UNSUPPORTED_PHASE: $phase" >&2
    exit 1
    ;;
esac

if [[ ! -f "$template" ]]; then
  echo "TEMPLATE_NOT_FOUND: $template" >&2
  exit 1
fi

sed \
  -e "s|{module}|$module|g" \
  -e "s|{phase}|$phase|g" \
  -e "s|{branch}|$branch|g" \
  -e "s|{current_main}|$current_main|g" \
  -e "s|{next_allowed_action}|$next_allowed_action|g" \
  "$template"

cat <<EOF

## Machine-Readable Handoff

- Active block: $active_block
- Risk: $risk
- Allowed changes: $allowed_changes
- Forbidden: $forbidden
- Required reads: $required_reads
- Required checks: $required_checks
- Expected output: $expected_output
EOF
