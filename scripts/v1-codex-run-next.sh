#!/usr/bin/env bash

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

print_hr() {
  echo "------------------------------------------------------------"
}

usage() {
  cat <<'EOF'
用法:
  bash scripts/v1-codex-run-next.sh [--open-pr-none-confirmed] [--request-package PACKAGE]

说明:
  本脚本只转交参数并消费权威 operator 结果。阶段、package、分支、Open PR、
  只读权限和 Product Source Gate 由以下唯一调用链解析:

  v1-codex-run-next.sh -> v1-operator.sh -> codex-next-task.sh -> v1-state.sh

  如果 Codex shell 的 gh 不可用，但外部证据已确认 Open PR none，可使用:

  bash scripts/v1-codex-run-next.sh --open-pr-none-confirmed

  可用 --request-package 显式请求当前或已授权后续 package。本脚本不解释这些
  参数，只原样转交，并对 operator 返回的权威字段做完整性检查。
EOF
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
    if [[ -f "$task_file" ]]; then
      echo
      echo "请复制下方任务全文给 Codex:"
      print_hr
      cat "$task_file"
      print_hr
    else
      echo "任务文件不存在，无法打印全文。"
    fi
  fi
  exit 1
}

OPERATOR_ARGS=()
while [[ "$#" -gt 0 ]]; do
  case "$1" in
    --open-pr-none-confirmed)
      OPERATOR_ARGS+=("$1")
      shift
      ;;
    --request-package)
      [[ -n "${2:-}" ]] || stop_with_task_path "BLOCKED_UNKNOWN_RESOLVED_STATE: --request-package requires a package identifier."
      OPERATOR_ARGS+=("$1" "$2")
      shift 2
      ;;
    --help|-h)
      usage
      exit 0
      ;;
    *)
      echo "STOP（停止）: unknown option（未知选项）: $1" >&2
      usage >&2
      exit 1
      ;;
  esac
done

echo "V1 Codex Run Next（一键启动下一步 Codex 任务）"
print_hr

set +e
operator_output="$(bash scripts/v1-operator.sh ${OPERATOR_ARGS[@]+"${OPERATOR_ARGS[@]}"} 2>&1)"
operator_status=$?
set -e
printf '%s\n' "$operator_output"
[[ "$operator_status" -eq 0 ]] || exit "$operator_status"

required_fields=(
  CURRENT_PACKAGE
  CURRENT_PACKAGE_ACTION_ALLOWED
  CURRENT_PACKAGE_BLOCK_REASON
  AUTHORIZED_NEXT_PACKAGE
  NEXT_PACKAGE_ALLOWED
  NEXT_PACKAGE_BLOCK_REASON
  REQUEST_CLASS
  RESOLVED_MODE
  REPOSITORY_EDITS_ALLOWED
  IMPLEMENTATION_ALLOWED
  PR_CREATION_ALLOWED
  OPEN_PR_EVIDENCE_SOURCE
  PRODUCT_SOURCE_GATE_STATUS
)
for key in "${required_fields[@]}"; do
  value="$(state_value "$operator_output" "$key")"
  case "$value" in
    ""|UNKNOWN|UNDECLARED|UNAVAILABLE)
      stop_with_task_path "BLOCKED_UNKNOWN_RESOLVED_STATE: operator result omitted or did not resolve $key."
      ;;
  esac
done

[[ "$(state_value "$operator_output" OPERATOR_RESULT_STATUS)" == "PASS" ]] \
  || stop_with_task_path "BLOCKED_UNKNOWN_RESOLVED_STATE: operator did not return OPERATOR_RESULT_STATUS: PASS."

echo "OUTER_LAUNCHER_STATUS: PASS"
