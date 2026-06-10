#!/usr/bin/env bash

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

print_hr() {
  echo "------------------------------------------------------------"
}

state_value() {
  local state_text="$1"
  local key="$2"
  printf '%s\n' "$state_text" | awk -F': ' -v key="$key" '$1 == key {print substr($0, length(key) + 3); exit}'
}

yaml_value() {
  local file="$1"
  local key="$2"
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

stop_with_task_path() {
  local reason="$1"
  local task_file="${2:-}"
  echo "STOP（停止）: $reason"
  if [[ -n "$task_file" ]]; then
    echo "任务文件 task file（任务文件）: $task_file"
    echo "请手动复制该任务给 Codex。"
  fi
  exit 1
}

echo "V1 Codex Run Next（一键启动下一步 Codex 任务）"
print_hr

state_text="$(bash scripts/v1-state.sh 2>&1 || true)"
echo "$state_text"
echo

branch="$(state_value "$state_text" "BRANCH")"
worktree="$(state_value "$state_text" "WORKTREE_CLEAN")"
open_prs="$(state_value "$state_text" "OPEN_PRS")"
can_continue="$(state_value "$state_text" "CAN_CONTINUE_NEXT_PACKAGE")"
blockers="$(state_value "$state_text" "BLOCKERS")"

if [[ "$branch" != "main" ]]; then
  stop_with_task_path "当前分支不是 main，不能自动启动下一包。当前分支: ${branch:-UNKNOWN}"
fi
if [[ "$worktree" != "Yes" ]]; then
  stop_with_task_path "Worktree Clean（工作区干净）不是 Yes。"
fi
if [[ "$open_prs" != "none" ]]; then
  if [[ "$open_prs" == "GH_NOT_AVAILABLE" ]]; then
    stop_with_task_path "Open PR（未合并 PR）状态未知。Codex GH_NOT_AVAILABLE 是 Codex GitHub 状态未知，需要用户本机 gh 或 GPT connector 证据。"
  fi
  stop_with_task_path "存在 Open PR（未合并 PR）: $open_prs"
fi
if [[ "$can_continue" != "YES" ]]; then
  stop_with_task_path "CAN_CONTINUE_NEXT_PACKAGE 不是 YES。Blockers（阻塞）: ${blockers:-UNKNOWN}"
fi

auto_output_file="${TMPDIR:-/tmp}/v1-auto-next-output.txt"
if ! bash scripts/v1-auto.sh next >"$auto_output_file"; then
  cat "$auto_output_file" || true
  stop_with_task_path "v1-auto.sh next 执行失败。"
fi

cat "$auto_output_file"

task_file="$(awk -F': ' '/临时文件:/ {print $2; exit}' "$auto_output_file")"
if [[ -z "$task_file" || ! -f "$task_file" ]]; then
  task_file="/tmp/v1-codex-next-task.txt"
  bash scripts/codex-next-task.sh >"$task_file"
fi

active_block="$(yaml_value docs/CODEX_NEXT_TASK.yml active_block)"
echo
echo "当前任务: ${active_block:-UNKNOWN}"
echo "task file（任务文件）: $task_file"

if ! command -v codex >/dev/null 2>&1; then
  stop_with_task_path "codex CLI 不存在，无法自动启动 Codex。" "$task_file"
fi

if [[ -n "${CODEX_RUNNER_COMMAND:-}" ]]; then
  echo "使用 CODEX_RUNNER_COMMAND（可配置执行命令）启动 Codex。"
  if CODEX_TASK_FILE="$task_file" bash -lc "$CODEX_RUNNER_COMMAND"; then
    echo "Codex 已启动。"
    exit 0
  fi
  stop_with_task_path "CODEX_RUNNER_COMMAND 执行失败。" "$task_file"
fi

echo "使用默认命令: codex exec <task>"
if codex exec "$(cat "$task_file")"; then
  echo "Codex 已启动。"
  exit 0
fi

stop_with_task_path "codex exec 执行失败。" "$task_file"
