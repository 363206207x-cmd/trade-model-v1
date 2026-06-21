#!/usr/bin/env bash

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

NEXT_TASK_CACHE="${TMPDIR:-/tmp}/v1-auto-next-task.txt"

usage() {
  cat <<'EOF'
V1 Auto Operator（V1 自动操作台）

用法:
  bash scripts/v1-auto.sh status
      查看 Contract / Matrix / Current State 优先的当前状态。

  bash scripts/v1-auto.sh summary
      查看当前 Phase、Existing Module Maturity、Next Business Phase、Contract Sync、Blockers。
      该命令不再使用 completed review-only runtime slice count 作为交付进度或下一任务依据。

  bash scripts/v1-auto.sh task
      生成当前 P0-0 handoff 任务文本，保存到临时文件并尽量复制到剪贴板。

  bash scripts/v1-auto.sh next
      执行 status + summary + task。

  bash scripts/v1-auto.sh pr <branch> "<title>" <risk> [...]
      保留 PR helper 行为，委托 scripts/v1-open-pr.sh。

  bash scripts/v1-auto.sh check-pr <PR_NUMBER> [risk]
      保留 PR 检查入口，优先委托 scripts/v1-pr-review-input.sh 并打印 gh checks（可用时）。

  bash scripts/v1-auto.sh merge <PR_NUMBER> "<title>" <risk> [--confirm]
      保留合并 helper 行为，委托 scripts/v1-merge-sync.sh。

安全规则:
  - 状态来自 scripts/v1-state.sh。
  - 任务来自 scripts/codex-next-task.sh。
  - P0-0 未 DONE 时，不显示 P0-1 为可执行任务。
  - 不自动交易、不自动平仓、不自动反手、不把 Push/Recheck/Preview 当交易授权。
EOF
}

print_hr() { echo "------------------------------------------------------------"; }

state_value() {
  local state_text="$1"
  local key="$2"
  printf '%s\n' "$state_text" | awk -F': ' -v key="$key" '$1 == key {print substr($0, length(key) + 3); exit}'
}

capture_state() {
  bash scripts/v1-state.sh 2>&1 || true
}

cmd_status() {
  capture_state
}

cmd_summary() {
  local state_text current_phase current_status maturity effective package next_phase next_allowed prod contract_sync blockers
  state_text="$(capture_state)"
  current_phase="$(state_value "$state_text" CURRENT_PHASE)"
  current_status="$(state_value "$state_text" CURRENT_PHASE_STATUS)"
  maturity="$(state_value "$state_text" EXISTING_MODULE_MATURITY)"
  effective="$(state_value "$state_text" COMPLETION_EFFECTIVE_STATE)"
  package="$(state_value "$state_text" CURRENT_WORK_PACKAGE)"
  next_phase="$(state_value "$state_text" NEXT_BUSINESS_PHASE)"
  next_allowed="$(state_value "$state_text" NEXT_BUSINESS_PHASE_ALLOWED)"
  prod="$(state_value "$state_text" PRODUCTION_DEPLOYMENT_READINESS)"
  contract_sync="$(state_value "$state_text" CONTRACT_MATRIX_SYNC)"
  blockers="$(state_value "$state_text" BLOCKERS)"

  echo "项目交付契约摘要（Contract-first Summary）"
  print_hr
  echo "Current Phase: ${current_phase:-UNKNOWN}"
  echo "Current Phase Status: ${current_status:-UNKNOWN}"
  echo "Existing Module Maturity: ${maturity:-UNKNOWN}"
  echo "Completion Effective State: ${effective:-UNKNOWN}"
  echo "Current Work Package: ${package:-UNKNOWN}"
  echo "Next Business Phase: ${next_phase:-UNKNOWN}"
  echo "Next Business Phase Allowed: ${next_allowed:-UNKNOWN}"
  echo "Production Deployment Readiness: ${prod:-UNKNOWN}"
  echo "Contract Sync: ${contract_sync:-UNKNOWN}"
  echo "Blockers: ${blockers:-UNKNOWN}"
  if [[ "${current_status:-}" != "DONE" || "${effective:-}" != "EFFECTIVE_MERGED_MAIN" ]]; then
    echo
    echo "Current phase is NOT EFFECTIVE on merged main."
    echo "Next business phase is NOT allowed."
    echo "The only allowed work is the current P0-0 work package."
  fi
}

cmd_task() {
  bash scripts/codex-next-task.sh >"$NEXT_TASK_CACHE"
  echo "任务临时文件: $NEXT_TASK_CACHE"
  if command -v pbcopy >/dev/null 2>&1 && pbcopy <"$NEXT_TASK_CACHE"; then
    echo "任务已复制到剪贴板。"
  else
    cat "$NEXT_TASK_CACHE"
  fi
}

cmd_next() {
  cmd_status
  echo
  cmd_summary
  echo
  cmd_task
}

cmd_check_pr() {
  local pr_number="${1:-}"
  [[ -n "$pr_number" ]] || { echo "missing PR_NUMBER" >&2; exit 1; }
  if [[ -x scripts/v1-pr-review-input.sh ]]; then
    bash scripts/v1-pr-review-input.sh "$pr_number" || true
  fi
  if command -v gh >/dev/null 2>&1; then
    gh pr checks "$pr_number" || true
  fi
}

cmd="${1:-status}"
case "$cmd" in
  status)
    shift || true
    cmd_status "$@"
    ;;
  summary)
    shift || true
    cmd_summary "$@"
    ;;
  task)
    shift || true
    cmd_task "$@"
    ;;
  next)
    shift || true
    cmd_next "$@"
    ;;
  pr)
    shift
    bash scripts/v1-open-pr.sh "$@"
    ;;
  check-pr)
    shift
    cmd_check_pr "$@"
    ;;
  merge)
    shift
    bash scripts/v1-merge-sync.sh "$@"
    ;;
  help|--help|-h)
    usage
    ;;
  *)
    usage >&2
    exit 1
    ;;
esac
