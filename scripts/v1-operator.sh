#!/usr/bin/env bash

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

TASK_FILE="docs/CODEX_NEXT_TASK.yml"
ACTIVE_FILE="docs/ACTIVE_MAINLINE_STATUS.yml"

usage() {
  cat <<'EOF'
V1 Operator（一键总控编排器）

用法:
  bash scripts/v1-operator.sh
      自动判断当前状态：main clean 时创建任务分支并启动/打印 Codex 任务；dirty worktree 时打包当前工作；A-risk PR 可自动检查并合并。

  bash scripts/v1-operator.sh --confirm-reviewed <PR_NUMBER>
      B-risk 经 GPT/人工复核后，继续检查并合并指定 Pull Request（拉取请求）。

说明:
  Codex 不再负责 git branch / commit / push / PR；Git 操作由终端脚本统一处理。
  本脚本不会改业务代码，也不会绕过 v1-state / codex-next-task / v1-package-dirty-work / v1-pr-complete。
EOF
}

yaml_value() {
  local file="$1"
  local key="$2"
  if [[ ! -f "$file" ]]; then
    return 0
  fi
  awk -v key="$key" '
    $0 ~ "^" key ":" {
      value=$0
      sub("^[^:]*:[[:space:]]*", "", value)
      gsub(/^"/, "", value)
      gsub(/"$/, "", value)
      print value
      exit
    }
  ' "$file"
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
    B)
      printf 'feat(workflow): %s' "$active_block" | tr '[:upper:]' '[:lower:]' | sed -E 's#[^a-z0-9/:._ -]+##g; s#[[:space:]]+# #g'
      ;;
    *)
      return 1
      ;;
  esac
}

print_task_text() {
  local task_file="${TMPDIR:-/tmp}/v1-operator-next-task.txt"
  bash scripts/codex-next-task.sh >"$task_file"
  echo "任务文件 task file（任务文件）: $task_file"
  echo "请复制下方任务全文给 Codex:"
  print_hr
  cat "$task_file"
  print_hr
}

run_codex_or_print_task() {
  local task_file="${TMPDIR:-/tmp}/v1-operator-next-task.txt"
  bash scripts/codex-next-task.sh >"$task_file"
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
  local risk
  local subject
  risk="$(yaml_value "$TASK_FILE" risk)"
  subject="$(subject_from_task "${risk:-B}" "$(yaml_value "$TASK_FILE" branch)" "$(yaml_value "$TASK_FILE" active_block)")" || stop "无法安全生成合并 subject。"
  [[ "$risk" == "B" ]] || stop "--confirm-reviewed 仅用于 B-risk；当前 risk: ${risk:-UNKNOWN}"
  bash scripts/v1-pr-complete.sh "$pr_number" B "$subject" --confirm-reviewed
}

main() {
  if [[ "${1:-}" == "--help" || "${1:-}" == "-h" ]]; then
    usage
    exit 0
  fi
  if [[ "${1:-}" == "--confirm-reviewed" ]]; then
    [[ -n "${2:-}" ]] || stop "缺少 PR_NUMBER。"
    confirm_reviewed "$2"
    exit 0
  fi
  if [[ "$#" -gt 0 ]]; then
    usage >&2
    exit 1
  fi

  echo "V1 Operator（一键总控编排器）"
  print_hr

  local state_text branch worktree open_prs main_sync can_continue blockers
  state_text="$(bash scripts/v1-state.sh 2>&1 || true)"
  echo "$state_text"
  echo

  branch="$(state_value "$state_text" BRANCH)"
  worktree="$(state_value "$state_text" WORKTREE_CLEAN)"
  open_prs="$(state_value "$state_text" OPEN_PRS)"
  main_sync="$(state_value "$state_text" MAIN_SYNC)"
  can_continue="$(state_value "$state_text" CAN_CONTINUE_NEXT_PACKAGE)"
  blockers="$(state_value "$state_text" BLOCKERS)"

  local task_branch risk active_block subject
  local source_head actual_head
  task_branch="$(yaml_value "$TASK_FILE" branch)"
  risk="$(yaml_value "$TASK_FILE" risk)"
  active_block="$(yaml_value "$TASK_FILE" active_block)"
  subject="$(subject_from_task "${risk:-A}" "$task_branch" "$active_block")" || subject="docs(workflow): package $task_branch"
  source_head="$(yaml_value "$ACTIVE_FILE" current_head)"
  actual_head="$(git rev-parse --short HEAD 2>/dev/null || true)"

  echo "当前任务: ${active_block:-UNKNOWN}"
  echo "任务分支: ${task_branch:-UNKNOWN}"
  echo "Risk（风险等级）: ${risk:-UNKNOWN}"
  echo "Subject（提交/PR 标题）: $subject"
  echo "Source of Truth 当前主线 HEAD: ${source_head:-UNKNOWN}"
  echo "实际当前 HEAD: ${actual_head:-UNKNOWN}"
  print_hr

  if [[ "$worktree" == "No" ]]; then
    echo "检测到 dirty worktree（脏工作区），进入自动打包流程。"
    echo "如果当前分支不是 main，将优先按当前分支打包，避免被 dirty CODEX_NEXT_TASK 下一阶段覆盖。"
    local package_output pr_number
    set +e
    package_output="$(bash scripts/v1-package-dirty-work.sh 2>&1)"
    local package_status=$?
    set -e
    echo "$package_output"
    if [[ "$package_status" -ne 0 ]]; then
      stop "dirty worktree（脏工作区）打包失败。"
    fi
    pr_number="$(printf '%s\n' "$package_output" | grep -Eo 'pull/[0-9]+' | tail -1 | sed -E 's#pull/##' || true)"
    echo
    if [[ -z "$pr_number" ]]; then
      echo "未能解析 Pull Request（拉取请求）编号；请查看上方输出。"
      exit 0
    fi
    echo "已识别 Pull Request（拉取请求）: #$pr_number"
    case "$risk" in
      A)
        echo "A-risk（低风险）: 自动进入 PR 检查 / ready / merge / next 流程。"
        bash scripts/v1-pr-complete.sh "$pr_number" A "$subject"
        ;;
      B)
        echo "B-risk（实现包）: 自动检查但不自动合并。"
        bash scripts/v1-pr-complete.sh "$pr_number" B "$subject" || true
        echo
        echo "GPT review summary（给 GPT 的复核摘要）:"
        print_hr
        echo "PR: #$pr_number"
        echo "Risk（风险等级）: B"
        echo "Subject: $subject"
        echo "请复核 changed files、forbidden semantics、CI 和实现边界。若明确同意合并，运行:"
        echo "bash scripts/v1-operator.sh --confirm-reviewed $pr_number"
        ;;
      *)
        echo "Risk（风险等级）: $risk，不自动合并。"
        ;;
    esac
    exit 0
  fi

  if [[ "$branch" != "main" ]]; then
    stop "当前不是 main 且工作区干净。请切回 main 或在当前分支产生/打包任务改动。当前分支: ${branch:-UNKNOWN}"
  fi

  if [[ "$main_sync" != "OK" ]]; then
    stop "Main Sync（主分支同步）不是 OK: ${main_sync:-UNKNOWN}"
  fi

  if [[ "$open_prs" != "none" ]]; then
    if [[ "$open_prs" == "GH_NOT_AVAILABLE" ]]; then
      echo "Open PR（未合并 PR）状态未知；如果 GPT connector / 用户本机 gh 已确认 none，可继续生成任务，但不会自动 PR/merge。"
    else
      stop "存在 Open PR（未合并 PR）: $open_prs"
    fi
  fi

  if [[ "$can_continue" != "YES" && "$blockers" != "OPEN_PR_STATUS_UNKNOWN_GH_NOT_AVAILABLE" ]]; then
    stop "CAN_CONTINUE_NEXT_PACKAGE 不是 YES。Blockers（阻塞）: ${blockers:-UNKNOWN}"
  fi

  if [[ -n "${source_head:-}" && -n "${actual_head:-}" && "$source_head" != "$actual_head" ]]; then
    echo "Source of Truth baseline（事实源基线）落后，但当前 main clean / synced / open PR none。"
    echo "本次使用 actual HEAD（实际 HEAD）作为 Effective execution baseline（实际执行基线）。"
    echo "不再创建 baseline sync（基线同步）小包；后续业务包会顺手更新 source-of-truth（事实源）。"
    print_hr
  fi

  [[ -n "$task_branch" ]] || stop "CODEX_NEXT_TASK.yml 缺少 branch。"

  if git show-ref --verify --quiet "refs/heads/$task_branch"; then
    echo "任务分支已存在，切换到: $task_branch"
    git switch "$task_branch"
  else
    echo "创建任务分支: $task_branch"
    git switch -c "$task_branch"
  fi

  echo
  echo "已由终端脚本创建/切换任务分支；Codex 不需要再创建 branch。"
  echo "尝试启动 Codex；失败时会直接打印完整任务文本。"
  run_codex_or_print_task
}

main "$@"
