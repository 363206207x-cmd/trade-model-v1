#!/usr/bin/env bash

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

CONTRACT_FILE="docs/PROJECT_DELIVERY_CONTRACT.md"
CURRENT_STATE_FILE="docs/PROJECT_CURRENT_STATE.md"
MATRIX_FILE="docs/DELIVERY_PROGRESS_MATRIX.md"
TEMPLATE_FILE="docs/CODEX_TASK_TEMPLATE.md"
TASK_FILE="docs/CODEX_NEXT_TASK.yml"

usage() {
  cat <<'EOF'
usage: bash scripts/codex-next-task.sh [--validate]

Reads the Project Delivery Contract, Delivery Progress Matrix, Project Current State,
Codex Task Template, and the derived CODEX_NEXT_TASK.yml handoff.
It does not modify files, stage, commit, push, call gh, or run business commands.
EOF
}

yaml_value() {
  local file="$1"
  local key="$2"
  [[ -f "$file" ]] || return 0
  awk -v key="$key" '
    $0 ~ "^" key ":" {
      value=$0
      sub("^[^:]*:[[:space:]]*", "", value)
      gsub(/^\"/, "", value)
      gsub(/\"$/, "", value)
      print value
      exit
    }
  ' "$file"
}

matrix_field() {
  local phase="$1"
  local field_index="$2"
  [[ -f "$MATRIX_FILE" ]] || return 0
  awk -F'|' -v phase="$phase" -v field_index="$field_index" '
    $2 ~ "^[[:space:]]*" phase "[[:space:]]*$" {
      value=$field_index
      gsub(/^[[:space:]]+|[[:space:]]+$/, "", value)
      print value
      exit
    }
  ' "$MATRIX_FILE"
}

current_state_value() {
  local key="$1"
  [[ -f "$CURRENT_STATE_FILE" ]] || return 0
  awk -F':' -v key="$key" '
    tolower($1) == tolower(key) {
      value=$0
      sub("^[^:]*:[[:space:]]*", "", value)
      gsub(/^`|`$/, "", value)
      gsub(/^[[:space:]]+|[[:space:]]+$/, "", value)
      sub(/[.]$/, "", value)
      result=value
    }
    END { if (result != "") print result }
  ' "$CURRENT_STATE_FILE"
}

validate_contract_task() {
  local failed=0
  for f in "$CONTRACT_FILE" "$CURRENT_STATE_FILE" "$MATRIX_FILE" "$TEMPLATE_FILE" "$TASK_FILE"; do
    if [[ ! -f "$f" ]]; then
      echo "TASK_VALIDATION_FAILED missing file: $f" >&2
      failed=1
    fi
  done
  [[ "$failed" -eq 0 ]] || return 1

  local matrix_phase="P0-0"
  local matrix_status task_phase task_allowed current_phase current_status effective compat state_text
  matrix_status="$(matrix_field P0-0 4)"
  task_phase="$(yaml_value "$TASK_FILE" current_phase)"
  task_allowed="$(yaml_value "$TASK_FILE" next_business_phase_allowed)"
  current_phase="$(current_state_value "Current Phase")"
  current_status="$(current_state_value "Current Phase Status")"
  state_text="$(bash scripts/v1-state.sh)"
  effective="$(printf '%s\n' "$state_text" | awk -F': ' '$1 == "COMPLETION_EFFECTIVE_STATE" {print $2; exit}')"
  compat="$(yaml_value "$TASK_FILE" compatibility_status)"

  [[ "$compat" == "DERIVED_ONLY" ]] || { echo "TASK_VALIDATION_FAILED CODEX_NEXT_TASK must be DERIVED_ONLY" >&2; failed=1; }
  [[ "$task_phase" == "$matrix_phase" ]] || { echo "TASK_VALIDATION_FAILED task current_phase mismatch: $task_phase" >&2; failed=1; }
  [[ "$current_phase" == P0-0* ]] || { echo "TASK_VALIDATION_FAILED current state phase mismatch: $current_phase" >&2; failed=1; }
  [[ "$current_status" == "$matrix_status" ]] || { echo "TASK_VALIDATION_FAILED current state status mismatch: $current_status != $matrix_status" >&2; failed=1; }
  if [[ "$matrix_status" != "DONE" || "$effective" != "EFFECTIVE_MERGED_MAIN" ]]; then
    [[ "$task_allowed" == "false" || "$task_allowed" == "NO" ]] || { echo "TASK_VALIDATION_FAILED next business phase must be blocked while current phase is not effective" >&2; failed=1; }
    local task_active_block task_module task_next_action
    task_active_block="$(yaml_value "$TASK_FILE" active_block)"
    task_module="$(yaml_value "$TASK_FILE" module)"
    task_next_action="$(yaml_value "$TASK_FILE" next_allowed_action)"
    if printf '%s\n%s\n%s\n' "$task_active_block" "$task_module" "$task_next_action" | grep -Eiq 'P0-1[[:space:]]+UserPosition[[:space:]]+implementation|UserPosition implementation|active_block:[[:space:]]*P0-1|module:[[:space:]]*UserPosition'; then
      echo "TASK_VALIDATION_FAILED task must not generate P0-1 implementation while P0-0 is not effective" >&2
      failed=1
    fi
  fi
  [[ "$failed" -eq 0 ]] || return 1
  echo "CODEX_TASK_VALIDATION_OK"
}

if [[ "${1:-}" == "--help" || "${1:-}" == "-h" ]]; then
  usage
  exit 0
fi

if [[ "${1:-}" == "--validate" ]]; then
  validate_contract_task
  exit 0
fi

validate_contract_task >/dev/null

state_text="$(bash scripts/v1-state.sh 2>&1 || true)"
branch="$(printf '%s\n' "$state_text" | awk -F': ' '$1 == "BRANCH" {print $2; exit}')"
worktree="$(printf '%s\n' "$state_text" | awk -F': ' '$1 == "WORKTREE_CLEAN" {print $2; exit}')"
main_sync="$(printf '%s\n' "$state_text" | awk -F': ' '$1 == "MAIN_SYNC" {print $2; exit}')"
open_prs="$(printf '%s\n' "$state_text" | awk -F': ' '$1 == "OPEN_PRS" {print substr($0, length("OPEN_PRS") + 3); exit}')"
open_pr_source="$(printf '%s\n' "$state_text" | awk -F': ' '$1 == "OPEN_PR_CHECK_SOURCE" {print substr($0, length("OPEN_PR_CHECK_SOURCE") + 3); exit}')"
open_pr_count="$(printf '%s\n' "$state_text" | awk -F': ' '$1 == "OPEN_PR_COUNT" {print substr($0, length("OPEN_PR_COUNT") + 3); exit}')"
open_pr_status="$(printf '%s\n' "$state_text" | awk -F': ' '$1 == "OPEN_PR_STATUS" {print substr($0, length("OPEN_PR_STATUS") + 3); exit}')"
contract_sync="$(printf '%s\n' "$state_text" | awk -F': ' '$1 == "CONTRACT_MATRIX_SYNC" {print $2; exit}')"
next_allowed="$(printf '%s\n' "$state_text" | awk -F': ' '$1 == "NEXT_BUSINESS_PHASE_ALLOWED" {print $2; exit}')"
next_phase="$(printf '%s\n' "$state_text" | awk -F': ' '$1 == "NEXT_BUSINESS_PHASE" {print $2; exit}')"
current_package="$(printf '%s\n' "$state_text" | awk -F': ' '$1 == "CURRENT_WORK_PACKAGE" {print $2; exit}')"
blockers="$(printf '%s\n' "$state_text" | awk -F': ' '$1 == "BLOCKERS" {print $2; exit}')"
current_phase_status="$(printf '%s\n' "$state_text" | awk -F': ' '$1 == "CURRENT_PHASE_STATUS" {print $2; exit}')"
completion_effective_state="$(printf '%s\n' "$state_text" | awk -F': ' '$1 == "COMPLETION_EFFECTIVE_STATE" {print $2; exit}')"

# Normal mode fail-closed for next-business generation. Current P0-0 handoff text is allowed;
# P0-1 is not generated unless the contract gate opens.
if [[ "$contract_sync" != "OK" ]]; then
  echo "STOP: contract sync is not OK ($contract_sync)." >&2
  exit 1
fi
if [[ "$current_phase_status" != "DONE" ]]; then
  if [[ "$(yaml_value "$TASK_FILE" current_phase)" != "P0-0" ]]; then
    echo "STOP: current phase is not DONE and task is outside P0-0." >&2
    exit 1
  fi
fi
if [[ "$(yaml_value "$TASK_FILE" current_phase)" != "P0-0" ]]; then
  if [[ "$branch" != "main" || "$worktree" != "Yes" || "$main_sync" != "OK" || "$open_prs" != "none" || "$next_allowed" != "YES" ]]; then
    echo "STOP: next business phase generation is blocked by branch/worktree/open PR/main sync/contract gate." >&2
    exit 1
  fi
fi

cat "$TEMPLATE_FILE"
cat <<EOF

---

# Fixed Codex Output Contract Hints / Codex 固定输出契约提示

WHAT_THIS_STEP_DOES（这一步在做什么）: Generate a user-readable task handoff without modifying files, staging, committing, pushing, creating PRs, merging, or starting a blocked package.
CURRENT_PROGRESS（当前进度）: Branch=${branch:-UNKNOWN}; worktree（工作区） clean=${worktree:-UNKNOWN}; main sync=${main_sync:-UNKNOWN}; open PR（未合并 PR）=${open_prs:-UNKNOWN}; open PR source（未合并 PR 来源）=${open_pr_source:-UNKNOWN}; open PR count（未合并 PR 数量）=${open_pr_count:-UNKNOWN}; open PR status（未合并 PR 状态）=${open_pr_status:-UNKNOWN}; current package=${current_package:-UNKNOWN}; completion=${completion_effective_state:-UNKNOWN}; next phase=${next_phase:-UNKNOWN}; next allowed（允许）=${next_allowed:-UNKNOWN}; blockers=${blockers:-UNKNOWN}.
NEXT_ALLOWED_ACTION（下一允许动作）: ${next_phase:-UNKNOWN} only when the runtime gate（门禁） reports allowed（允许） and the worktree（工作区） is clean/synced main（干净且已同步主线）.
NEXT_BLOCKED_ACTION（下一禁止动作）: Do not start blocked（阻塞） packages, do not treat open PR（未合并 PR） as done, do not bypass PENDING_MERGED_MAIN（等待合并主线）, and do not auto-trade.

---

# Machine-Readable P0-0 Task Handoff

Current Phase: $(yaml_value "$TASK_FILE" current_phase)
Current Phase Status: $(yaml_value "$TASK_FILE" current_phase_status)
Completion Effective State: ${completion_effective_state:-UNKNOWN}
Existing Module Maturity: $(yaml_value "$TASK_FILE" existing_module_maturity)
Active Block: $(yaml_value "$TASK_FILE" active_block)
Module: $(yaml_value "$TASK_FILE" module)
Branch: $(yaml_value "$TASK_FILE" branch)
Risk: $(yaml_value "$TASK_FILE" risk)
Compatibility Status: $(yaml_value "$TASK_FILE" compatibility_status)
Next Business Phase: $(yaml_value "$TASK_FILE" next_business_phase)
Next Business Phase Allowed: $(yaml_value "$TASK_FILE" next_business_phase_allowed)
Next Allowed Action: $(yaml_value "$TASK_FILE" next_allowed_action)

Allowed Scope:
$(awk '/^allowed_scope:/{flag=1; next} /^forbidden_scope:/{flag=0} flag {print}' "$TASK_FILE")

Forbidden Scope:
$(awk '/^forbidden_scope:/{flag=1; next} /^checks:/{flag=0} flag {print}' "$TASK_FILE")

Checks:
$(awk '/^checks:/{flag=1; next} /^stop_conditions:/{flag=0} flag {print}' "$TASK_FILE")

Stop Conditions:
$(awk '/^stop_conditions:/{flag=1; next} flag {print}' "$TASK_FILE")
EOF
