#!/usr/bin/env bash

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

TASK_FILE="docs/CODEX_NEXT_TASK.yml"
TASK_CACHE="${TMPDIR:-/tmp}/v1-go-next-task.txt"

usage() {
  cat <<'EOF'
V1 Go（V1 人工总控唯一入口）

用法:
  bash scripts/v1-go.sh
      自动判断当前状态并选择下一步：
      - clean main + no Open PR（未合并 PR）: 调用 v1-operator.sh 生成/启动下一 Codex 任务；Codex 失败时自动复制任务全文到剪贴板。
      - dirty task branch（脏任务分支）: 调用 v1-package-dirty-work.sh 打包、push、创建 PR，并自动读取 PR 编号。
      - existing Open PR（已有未合并 PR）: 自动按风险等级检查；A-risk 自动完成，B-risk 输出 GPT 复核摘要。

  bash scripts/v1-go.sh --confirm-reviewed <PR_NUMBER>
      B-risk（实现包）经 GPT/人工明确复核后，继续调用固定合并流程。

  bash scripts/v1-go.sh --status
      只显示当前状态、Open PR（未合并 PR）和下一动作，不做写操作。

说明:
  本脚本只编排现有固定脚本，不绕过 v1-state / v1-auto / v1-operator / v1-package-dirty-work / v1-pr-complete / v1-merge-sync。
  Git Push（Git 分支推送）只用于分支上传，不是业务 Push（推送通道）。
EOF
}

print_hr() {
  echo "------------------------------------------------------------"
}

stop() {
  echo "STOP（停止）: $*" >&2
  exit 1
}

state_value() {
  local state_text="$1"
  local key="$2"
  printf '%s\n' "$state_text" | awk -F': ' -v key="$key" '$1 == key {print substr($0, length(key) + 3); exit}'
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

capture_state() {
  bash scripts/v1-state.sh 2>&1 || true
}

ensure_gh_for_pr_flow() {
  if ! command -v gh >/dev/null 2>&1; then
    stop "gh 不可用，无法自动读取 Open PR（未合并 PR）。请使用 GPT connector 或用户本机 gh 处理。"
  fi
  if ! gh auth status >/dev/null 2>&1; then
    echo "Codex shell gh auth 不可用。"
    echo "这表示 Codex GitHub status unknown（Codex GitHub 状态未知），不是项目 main 失败。"
    stop "自动 PR 流程需要 gh 或 GPT connector。"
  fi
}

copy_or_print_task() {
  bash scripts/codex-next-task.sh >"$TASK_CACHE"
  echo "任务文件 task file（任务文件）: $TASK_CACHE"
  if command -v pbcopy >/dev/null 2>&1 && pbcopy <"$TASK_CACHE"; then
    echo "任务已复制到剪贴板，请直接到 Codex 对话框 Command + V。"
    return 0
  fi

  echo "pbcopy 不可用或复制失败；请复制下方任务全文给 Codex。"
  print_hr
  cat "$TASK_CACHE"
  print_hr
}

open_pr_number_from_state() {
  local open_prs="$1"
  printf '%s\n' "$open_prs" | grep -Eo '[0-9]+' | head -1 || true
}

first_open_pr_number() {
  local open_prs="$1"
  local parsed
  parsed="$(open_pr_number_from_state "$open_prs")"
  if [[ -n "$parsed" ]]; then
    echo "$parsed"
    return 0
  fi

  ensure_gh_for_pr_flow
  gh pr list --state open --limit 1 --json number --jq '.[0].number'
}

pr_title() {
  local pr_number="$1"
  gh pr view "$pr_number" --json title --jq '.title'
}

pr_url() {
  local pr_number="$1"
  gh pr view "$pr_number" --json url --jq '.url'
}

pr_changed_files() {
  local pr_number="$1"
  gh pr diff "$pr_number" --name-only
}

infer_pr_risk() {
  local pr_number="$1"
  local changed_files file
  changed_files="$(pr_changed_files "$pr_number")"
  while IFS= read -r file; do
    [[ -z "$file" ]] && continue
    case "$file" in
      docs/*|scripts/*|AGENTS.md)
        ;;
      *)
        echo "B"
        return 0
        ;;
    esac
  done <<<"$changed_files"
  echo "A"
}

print_b_risk_review_summary() {
  local pr_number="$1"
  local title="$2"
  local url="$3"
  echo "B-risk PR，需要 GPT 复核"
  print_hr
  echo "Pull Request（拉取请求）: #$pr_number"
  echo "PR URL: $url"
  echo "Title（标题）: $title"
  echo
  echo "Changed files（变更文件）:"
  pr_changed_files "$pr_number" | sed 's/^/- /'
  echo
  echo "Checks（检查）:"
  gh pr checks "$pr_number" || true
  echo
  echo "请让 GPT/人工复核 changed files、CI、forbidden semantics 和实现边界。"
  echo "复核通过后运行:"
  echo "bash scripts/v1-go.sh --confirm-reviewed $pr_number"
}

complete_open_pr() {
  local pr_number="$1"
  local risk="${2:-}"
  ensure_gh_for_pr_flow
  local title url
  title="$(pr_title "$pr_number")"
  url="$(pr_url "$pr_number")"
  if [[ -z "$risk" ]]; then
    risk="$(infer_pr_risk "$pr_number")"
  fi

  echo "检测到 Open PR（未合并 PR）: #$pr_number"
  echo "Risk（风险等级）: $risk"
  echo "PR URL: $url"
  print_hr

  case "$risk" in
    A)
      echo "A-risk（低风险）: 自动检查、ready、merge、sync。"
      bash scripts/v1-pr-complete.sh "$pr_number" A "$title"
      ;;
    B)
      echo "B-risk（实现包）: 自动检查但不自动合并。"
      bash scripts/v1-auto.sh check-pr "$pr_number" B || true
      echo
      print_b_risk_review_summary "$pr_number" "$title" "$url"
      ;;
    *)
      echo "Risk（风险等级）: $risk。默认不自动合并，需要人工复核。"
      bash scripts/v1-auto.sh check-pr "$pr_number" "$risk" || true
      ;;
  esac
}

pr_number_from_package_output() {
  local output="$1"
  local number
  number="$(printf '%s\n' "$output" | grep -Eo 'pull/[0-9]+' | tail -1 | sed -E 's#pull/##' || true)"
  if [[ -n "$number" ]]; then
    echo "$number"
    return 0
  fi
  printf '%s\n' "$output" | grep -E 'Pull Request.*编号:' | grep -Eo '[0-9]+' | tail -1 || true
}

package_dirty_work() {
  local risk
  risk="$(yaml_value "$TASK_FILE" risk)"
  echo "检测到 dirty worktree（脏工作区），调用 v1-package-dirty-work.sh 自动打包。"
  print_hr

  local package_output package_status pr_number subject
  set +e
  package_output="$(bash scripts/v1-package-dirty-work.sh 2>&1)"
  package_status=$?
  set -e
  echo "$package_output"

  if [[ "$package_status" -ne 0 ]]; then
    stop "v1-package-dirty-work.sh 打包失败。"
  fi

  pr_number="$(pr_number_from_package_output "$package_output")"
  if [[ -z "$pr_number" ]]; then
    stop "未能自动读取创建出来的 PR 编号。上方输出保留了原始信息。"
  fi
  subject="$(git log -1 --pretty=%s)"

  echo
  echo "已自动读取 Pull Request（拉取请求）编号: #$pr_number"
  echo "Risk（风险等级）: ${risk:-UNKNOWN}"
  echo "Subject（标题）: $subject"

  case "$risk" in
    A)
      echo "A-risk（低风险）: 自动进入 PR 完成流程。"
      bash scripts/v1-pr-complete.sh "$pr_number" A "$subject"
      ;;
    B)
      echo "B-risk（实现包）: 自动检查但不自动合并。"
      bash scripts/v1-pr-complete.sh "$pr_number" B "$subject" || true
      if command -v gh >/dev/null 2>&1 && gh auth status >/dev/null 2>&1; then
        print_b_risk_review_summary "$pr_number" "$subject" "$(pr_url "$pr_number")"
      else
        echo "B-risk PR，需要 GPT 复核。当前 gh 不可用，请用 GPT connector 查看 PR #$pr_number。"
      fi
      ;;
    *)
      echo "Risk（风险等级）: ${risk:-UNKNOWN}，不自动合并。"
      ;;
  esac
}

run_clean_main_flow() {
  echo "clean main + Open PR none：调用 v1-operator.sh 生成/启动下一任务。"
  print_hr

  local operator_output operator_status
  set +e
  operator_output="$(bash scripts/v1-operator.sh 2>&1)"
  operator_status=$?
  set -e
  echo "$operator_output"

  if printf '%s\n' "$operator_output" | grep -Eq 'Codex CLI 不存在|Codex CLI 启动失败|codex exec|请复制下方任务全文给 Codex|readonly database|permission denied|Permission denied'; then
    echo
    echo "检测到 Codex CLI 未能可靠启动，自动复制任务全文。"
    copy_or_print_task
  fi

  if [[ "$operator_status" -ne 0 ]]; then
    stop "v1-operator.sh 执行失败。"
  fi
}

cmd_status() {
  local state_text open_prs pr_number
  state_text="$(capture_state)"
  echo "$state_text"
  echo
  bash scripts/v1-auto.sh status || true
  open_prs="$(state_value "$state_text" OPEN_PRS)"
  if [[ "$open_prs" != "none" && "$open_prs" != "GH_NOT_AVAILABLE" && -n "$open_prs" ]]; then
    pr_number="$(first_open_pr_number "$open_prs")"
    echo
    echo "Open PR（未合并 PR）详情:"
    ensure_gh_for_pr_flow
    gh pr view "$pr_number" --json number,title,state,isDraft,mergeable,headRefName,baseRefName,url
  fi
}

main() {
  case "${1:-}" in
    --help|-h|help)
      usage
      exit 0
      ;;
    --status|status)
      cmd_status
      exit 0
      ;;
    --confirm-reviewed)
      [[ -n "${2:-}" ]] || stop "缺少 PR_NUMBER。"
      bash scripts/v1-operator.sh --confirm-reviewed "$2"
      exit 0
      ;;
    "")
      ;;
    *)
      usage >&2
      exit 1
      ;;
  esac

  echo "V1 Go（V1 人工总控唯一入口）"
  print_hr

  local state_text branch worktree open_prs main_sync can_continue blockers pr_number
  state_text="$(capture_state)"
  echo "$state_text"
  echo

  branch="$(state_value "$state_text" BRANCH)"
  worktree="$(state_value "$state_text" WORKTREE_CLEAN)"
  open_prs="$(state_value "$state_text" OPEN_PRS)"
  main_sync="$(state_value "$state_text" MAIN_SYNC)"
  can_continue="$(state_value "$state_text" CAN_CONTINUE_NEXT_PACKAGE)"
  blockers="$(state_value "$state_text" BLOCKERS)"

  if [[ "$open_prs" != "none" && "$open_prs" != "GH_NOT_AVAILABLE" && -n "$open_prs" ]]; then
    pr_number="$(first_open_pr_number "$open_prs")"
    [[ -n "$pr_number" ]] || stop "检测到 Open PR（未合并 PR），但无法解析 PR 编号。"
    complete_open_pr "$pr_number"
    exit 0
  fi

  if [[ "$branch" != "main" && "$worktree" == "No" ]]; then
    package_dirty_work
    exit 0
  fi

  if [[ "$branch" == "main" && "$worktree" == "Yes" ]]; then
    if [[ "$open_prs" == "GH_NOT_AVAILABLE" ]]; then
      stop "Open PR（未合并 PR）状态未知。请先用 GPT connector / 用户本机 gh 确认 none，或使用已有 handoff 后再运行 operator。"
    fi
    [[ "$main_sync" == "OK" ]] || stop "Main Sync（主分支同步）不是 OK: ${main_sync:-UNKNOWN}"
    [[ "$can_continue" == "YES" ]] || stop "CAN_CONTINUE_NEXT_PACKAGE 不是 YES。Blockers（阻塞）: ${blockers:-UNKNOWN}"
    run_clean_main_flow
    exit 0
  fi

  if [[ "$branch" != "main" && "$worktree" == "Yes" ]]; then
    echo "当前分支不是 main 且工作区干净: $branch"
    echo "没有 dirty work（脏改动）可打包，也不能从该分支启动下一包。"
    echo "请切回 clean main 后运行: bash scripts/v1-go.sh"
    exit 0
  fi

  stop "当前状态无法自动处理。Branch=${branch:-UNKNOWN}, Worktree=${worktree:-UNKNOWN}, OpenPR=${open_prs:-UNKNOWN}"
}

main "$@"
