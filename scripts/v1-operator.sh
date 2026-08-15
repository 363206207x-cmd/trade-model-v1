#!/usr/bin/env bash

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"
STATE_ARGS=()
TASK_ARGS=()
STATE_ARG_COUNT=0

usage() {
  cat <<'EOF'
V1 Operator（一键总控编排器）

用法:
  bash scripts/v1-operator.sh [--open-pr-none-confirmed] [--request-package PACKAGE]
      从 v1-state 的 resolved task 结果选择任务。只读审计仅打印任务且禁止仓库写入；可写包仍需通过全部状态门禁。

  bash scripts/v1-operator.sh --confirm-reviewed <PR_NUMBER>
      B-risk 经 GPT/人工复核后，继续检查并合并指定 Pull Request（拉取请求）。

说明:
  Codex 不再负责 git branch / commit / push / PR；Git 操作由终端脚本统一处理。
  本脚本不会绕过 v1-state / codex-next-task / v1-pr-complete，也不会为未知状态选择静态回退任务。
EOF
}

state_value() {
  local state_text="$1"
  local key="$2"
  printf '%s\n' "$state_text" | awk -F': ' -v key="$key" '$1 == key {print substr($0, length(key) + 3); exit}'
}

print_hr() {
  echo "------------------------------------------------------------"
}

stop() {
  echo "STOP（停止）: $*" >&2
  exit 1
}

require_resolved_field() {
  local state_text="$1"
  local key="$2"
  local value
  value="$(state_value "$state_text" "$key")"
  case "$value" in
    ""|UNKNOWN|UNDECLARED|UNAVAILABLE)
      stop "BLOCKED_UNKNOWN_RESOLVED_STATE: missing or unresolved $key."
      ;;
  esac
  printf '%s\n' "$value"
}

subject_from_task() {
  local risk="$1"
  local branch="$2"
  local active_block="$3"
  if [[ -z "$branch" || -z "$active_block" ]]; then
    return 1
  fi
  case "$risk" in
    A)
      printf 'docs(workflow): %s' "$active_block" | tr '[:upper:]' '[:lower:]' | sed -E 's#[^a-z0-9/:._ -]+##g; s#[[:space:]]+# #g'
      ;;
    B|B_*)
      printf 'feat(workflow): %s' "$active_block" | tr '[:upper:]' '[:lower:]' | sed -E 's#[^a-z0-9/:._ -]+##g; s#[[:space:]]+# #g'
      ;;
    *)
      return 1
      ;;
  esac
}

print_task_text() {
  local task_file="${TMPDIR:-/tmp}/v1-operator-next-task.txt"
  bash scripts/codex-next-task.sh ${TASK_ARGS[@]+"${TASK_ARGS[@]}"} >"$task_file"
  echo "任务文件 task file（任务文件）: $task_file"
  echo "请复制下方任务全文给 Codex:"
  print_hr
  cat "$task_file"
  print_hr
}

run_codex_or_print_task() {
  local task_file="${TMPDIR:-/tmp}/v1-operator-next-task.txt"
  bash scripts/codex-next-task.sh ${TASK_ARGS[@]+"${TASK_ARGS[@]}"} >"$task_file"
  echo "任务文件 task file（任务文件）: $task_file"
  if ! command -v codex >/dev/null 2>&1; then
    echo "Codex CLI 不存在；请复制下方任务全文给 Codex。"
    print_hr
    cat "$task_file"
    print_hr
    return 0
  fi

  if [[ -n "${CODEX_RUNNER_COMMAND:-}" ]]; then
    echo "使用 CODEX_RUNNER_COMMAND（可配置执行命令）启动 Codex。"
    if CODEX_TASK_FILE="$task_file" bash -lc "$CODEX_RUNNER_COMMAND"; then
      echo "Codex 已启动。"
      return 0
    fi
  else
    echo "使用默认命令 codex exec 启动 Codex。"
    if codex exec "$(cat "$task_file")"; then
      echo "Codex 已启动。"
      return 0
    fi
  fi

  echo "Codex CLI 启动失败；请复制下方任务全文给 Codex。"
  print_hr
  cat "$task_file"
  print_hr
}

confirm_reviewed() {
  local pr_number="$1"
  local state_text resolved_from resolution_status resolved_mode risk task_branch active_block subject
  state_text="$(bash scripts/v1-state.sh 2>&1)" || stop "权威状态解析失败。"
  resolved_from="$(state_value "$state_text" RESOLVED_FROM_STATE)"
  resolution_status="$(state_value "$state_text" RESOLUTION_STATUS)"
  resolved_mode="$(state_value "$state_text" RESOLVED_MODE)"
  risk="$(state_value "$state_text" RESOLVED_RISK)"
  task_branch="$(state_value "$state_text" RESOLVED_BRANCH)"
  active_block="$(state_value "$state_text" RESOLVED_ACTIVE_BLOCK)"
  [[ "$resolved_from" == "YES" && "$resolution_status" == "ALLOWED" ]] || stop "无法从 v1-state 解析可执行任务。"
  [[ "$resolved_mode" != "READ_ONLY_PRODUCT_AUDIT" ]] || stop "只读审计禁止 PR merge 操作。"
  subject="$(subject_from_task "${risk:-B}" "$task_branch" "$active_block")" || stop "无法安全生成合并 subject。"
  [[ "$risk" == "B" || "$risk" == B_* ]] || stop "--confirm-reviewed 仅用于 B-risk；当前 risk: ${risk:-UNKNOWN}"
  bash scripts/v1-pr-complete.sh "$pr_number" B "$subject" --confirm-reviewed
}

main() {
  local confirm_reviewed_pr=""
  while [[ "$#" -gt 0 ]]; do
    case "$1" in
      --open-pr-none-confirmed)
        STATE_ARGS+=("--open-pr-none-confirmed")
        TASK_ARGS+=("--open-pr-none-confirmed")
        ((STATE_ARG_COUNT+=1))
        shift
        ;;
      --request-package)
        [[ -n "${2:-}" ]] || stop "--request-package requires a package identifier."
        STATE_ARGS+=("--request-package" "$2")
        TASK_ARGS+=("--request-package" "$2")
        ((STATE_ARG_COUNT+=2))
        shift 2
        ;;
      --confirm-reviewed)
        [[ -n "${2:-}" ]] || stop "缺少 PR_NUMBER。"
        confirm_reviewed_pr="$2"
        shift 2
        ;;
      --help|-h)
        usage
        exit 0
        ;;
      *)
        usage >&2
        exit 1
        ;;
    esac
  done

  if [[ -n "$confirm_reviewed_pr" ]]; then
    [[ "$STATE_ARG_COUNT" -eq 0 ]] || stop "--confirm-reviewed cannot be combined with task-resolution options."
    confirm_reviewed "$confirm_reviewed_pr"
    exit 0
  fi

  echo "V1 Operator（一键总控编排器）"
  print_hr

  local state_text branch worktree open_prs main_sync can_continue blockers
  local resolved_from resolution_status resolved_package resolved_mode resolved_branch
  local resolved_active_block resolved_risk resolved_stage resolved_edit_permission
  local resolved_implementation_permission resolved_pr_creation_permission block_reason resolution_block_reason
  local current_package authorized_next_package request_class
  local current_package_action_allowed current_package_block_reason next_package_allowed
  local repository_edits_allowed implementation_allowed pr_creation_allowed open_pr_evidence_source
  state_text="$(bash scripts/v1-state.sh ${STATE_ARGS[@]+"${STATE_ARGS[@]}"} 2>&1)" || stop "权威状态解析失败。"
  echo "$state_text"
  echo

  branch="$(state_value "$state_text" BRANCH)"
  worktree="$(state_value "$state_text" WORKTREE_CLEAN)"
  open_prs="$(state_value "$state_text" OPEN_PRS)"
  main_sync="$(state_value "$state_text" MAIN_SYNC)"
  can_continue="$(state_value "$state_text" CAN_CONTINUE_NEXT_PACKAGE)"
  blockers="$(state_value "$state_text" BLOCKERS)"
  resolved_from="$(state_value "$state_text" RESOLVED_FROM_STATE)"
  resolution_status="$(state_value "$state_text" RESOLUTION_STATUS)"
  resolved_package="$(state_value "$state_text" RESOLVED_PACKAGE)"
  resolved_mode="$(state_value "$state_text" RESOLVED_MODE)"
  resolved_branch="$(state_value "$state_text" RESOLVED_BRANCH)"
  resolved_active_block="$(state_value "$state_text" RESOLVED_ACTIVE_BLOCK)"
  resolved_risk="$(state_value "$state_text" RESOLVED_RISK)"
  resolved_stage="$(state_value "$state_text" RESOLVED_HANDOFF_STAGE)"
  resolved_edit_permission="$(state_value "$state_text" RESOLVED_EDIT_PERMISSION)"
  resolved_implementation_permission="$(state_value "$state_text" RESOLVED_IMPLEMENTATION_PERMISSION)"
  resolved_pr_creation_permission="$(state_value "$state_text" RESOLVED_PR_CREATION_PERMISSION)"
  block_reason="$(state_value "$state_text" NEXT_PACKAGE_BLOCK_REASON)"
  resolution_block_reason="$(state_value "$state_text" RESOLUTION_BLOCK_REASON)"
  current_package="$(require_resolved_field "$state_text" CURRENT_PACKAGE)"
  current_package_action_allowed="$(require_resolved_field "$state_text" CURRENT_PACKAGE_ACTION_ALLOWED)"
  current_package_block_reason="$(require_resolved_field "$state_text" CURRENT_PACKAGE_BLOCK_REASON)"
  authorized_next_package="$(require_resolved_field "$state_text" AUTHORIZED_NEXT_PACKAGE)"
  next_package_allowed="$(require_resolved_field "$state_text" NEXT_PACKAGE_ALLOWED)"
  block_reason="$(require_resolved_field "$state_text" NEXT_PACKAGE_BLOCK_REASON)"
  request_class="$(require_resolved_field "$state_text" REQUEST_CLASS)"
  resolved_mode="$(require_resolved_field "$state_text" RESOLVED_MODE)"
  repository_edits_allowed="$(require_resolved_field "$state_text" REPOSITORY_EDITS_ALLOWED)"
  implementation_allowed="$(require_resolved_field "$state_text" IMPLEMENTATION_ALLOWED)"
  pr_creation_allowed="$(require_resolved_field "$state_text" PR_CREATION_ALLOWED)"
  open_pr_evidence_source="$(require_resolved_field "$state_text" OPEN_PR_EVIDENCE_SOURCE)"
  require_resolved_field "$state_text" PRODUCT_SOURCE_GATE_STATUS >/dev/null

  [[ "$repository_edits_allowed" == "$resolved_edit_permission" \
    && "$implementation_allowed" == "$resolved_implementation_permission" \
    && "$pr_creation_allowed" == "$resolved_pr_creation_permission" ]] \
    || stop "BLOCKED_UNKNOWN_RESOLVED_STATE: resolved permissions are inconsistent."

  [[ "$resolved_from" == "YES" ]] || stop "状态输出缺少 RESOLVED_FROM_STATE=YES。"
  [[ "$resolution_status" == "ALLOWED" ]] || stop "状态解析已阻断: ${resolution_block_reason:-UNKNOWN}."

  echo "Resolved Package（解析包）: ${resolved_package:-UNKNOWN}"
  echo "Resolved Mode（解析模式）: ${resolved_mode:-UNKNOWN}"
  echo "Resolved Stage（解析阶段）: ${resolved_stage:-UNKNOWN}"
  echo "Resolved Branch（解析分支）: ${resolved_branch:-UNKNOWN}"
  echo "Risk（风险等级）: ${resolved_risk:-UNKNOWN}"
  print_hr

  if [[ "$request_class" == "CURRENT_PACKAGE_CONTINUATION" ]]; then
    [[ "$current_package_action_allowed" == "YES" ]] \
      || stop "当前 package continuation 未获 resolver 授权: ${current_package_block_reason:-UNKNOWN}."
    echo "OPERATOR_MODE: CURRENT_PACKAGE_CONTINUATION"
    echo "CURRENT_PACKAGE_ACTION: ALLOWED"
    echo "CURRENT_PACKAGE_BRANCH: ACCEPTED"
    echo "OPERATOR_RESULT_STATUS: PASS"
    if [[ "${V1_WORKFLOW_SELF_TEST:-0}" == "1" ]]; then
      print_task_text
    else
      run_codex_or_print_task
    fi
    exit 0
  fi

  [[ "$request_class" == "AUTHORIZED_IMPLEMENTATION_PACKAGE" ]] || stop "未知 request class: ${request_class:-UNKNOWN}."
  [[ "$next_package_allowed" == "YES" ]] || stop "Successor package 未获 resolver 授权: ${block_reason:-UNKNOWN}."

  if [[ "$resolved_mode" == "READ_ONLY_PRODUCT_AUDIT" ]]; then
    [[ "$resolved_edit_permission" == "false" ]] || stop "只读审计错误地获得了仓库编辑权限。"
    [[ "$resolved_implementation_permission" == "false" ]] || stop "只读审计错误地获得了实现权限。"
    [[ "$resolved_pr_creation_permission" == "false" ]] || stop "只读审计错误地获得了 PR 创建权限。"
    [[ "$can_continue" == "YES" ]] || stop "Resolver 输出不一致。Blockers: ${blockers:-UNKNOWN}"
    echo "OPERATOR_MODE: READ_ONLY_AUDIT"
    echo "REPOSITORY_MUTATION: DISABLED"
    echo "PR_CREATION: DISABLED"
    echo "IMPLEMENTATION: DISABLED"
    echo "OPERATOR_RESULT_STATUS: PASS"
    print_task_text
    exit 0
  fi

  [[ "$resolved_mode" == "IMPLEMENTATION" ]] || stop "未知 successor mode: ${resolved_mode:-UNKNOWN}."
  [[ "$resolved_edit_permission" == "true" ]] || stop "当前 resolved task 不允许仓库编辑。"
  [[ "$resolved_implementation_permission" == "true" ]] || stop "当前 resolved task 不允许实现。"
  [[ "$resolved_pr_creation_permission" == "true" ]] || stop "当前 resolved task 不允许创建实现 PR。"
  [[ "$worktree" == "Yes" ]] || stop "工作区不是 clean，resolved task 不允许自动打包或回退。"
  if [[ "$branch" != "main" ]]; then
    stop "当前不是 main 且工作区干净。请切回 main 或在当前分支产生/打包任务改动。当前分支: ${branch:-UNKNOWN}"
  fi

  if [[ "$main_sync" != "OK" ]]; then
    stop "Main Sync（主分支同步）不是 OK: ${main_sync:-UNKNOWN}"
  fi

  if [[ "$open_prs" != "none" ]]; then
    stop "存在或无法确认 Open PR（未合并 PR）: $open_prs"
  fi

  if [[ "$can_continue" != "YES" ]]; then
    stop "CAN_CONTINUE_NEXT_PACKAGE 不是 YES。Blockers（阻塞）: ${blockers:-UNKNOWN}"
  fi

  [[ -n "$resolved_branch" && "$resolved_branch" != "NONE" && "$resolved_branch" != "NONE_READ_ONLY_AUDIT" ]] || stop "resolved task 缺少可写分支。"

  echo "OPERATOR_MODE: IMPLEMENTATION"
  echo "REPOSITORY_MUTATION: ENABLED"
  echo "PR_CREATION: ENABLED"
  echo "IMPLEMENTATION: ENABLED"
  echo "OPERATOR_RESULT_STATUS: PASS"
  if [[ "${V1_WORKFLOW_SELF_TEST:-0}" == "1" ]]; then
    print_task_text
    exit 0
  fi

  local subject
  subject="$(subject_from_task "${resolved_risk:-A}" "$resolved_branch" "$resolved_active_block")" || stop "无法从 resolved task 生成 subject。"
  echo "Subject（提交/PR 标题）: $subject"

  if git show-ref --verify --quiet "refs/heads/$resolved_branch"; then
    echo "任务分支已存在，切换到: $resolved_branch"
    git switch "$resolved_branch"
  else
    echo "创建任务分支: $resolved_branch"
    git switch -c "$resolved_branch"
  fi

  echo
  echo "已由终端脚本创建/切换任务分支；Codex 不需要再创建 branch。"
  echo "尝试启动 Codex；失败时会直接打印完整任务文本。"
  run_codex_or_print_task
}

main "$@"
