#!/usr/bin/env bash

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

ACTIVE_FILE="docs/ACTIVE_MAINLINE_STATUS.yml"
NEXT_TASK_FILE="docs/CODEX_NEXT_TASK.yml"
NEXT_TASK_CACHE="${TMPDIR:-/tmp}/v1-auto-next-task.txt"

usage() {
  cat <<'EOF'
V1 Auto Operator（V1 自动操作台）

用法:
  bash scripts/v1-auto.sh status
      查看当前状态：模块、阶段、能力层级、Main Sync（主分支同步）、Open PR（未合并 PR）、Blockers（阻塞）。
      No active PR 表示没有当前未合并 PR。

  bash scripts/v1-auto.sh summary
      查看白话进度摘要：已经完成哪些 Review-Only Runtime partial（只读运行时部分完成）小闭环，下一步是什么。

  bash scripts/v1-auto.sh task
      生成下一步 Codex 任务提示词，保存到临时文件并尽量复制到剪贴板。

  bash scripts/v1-auto.sh next
      一次执行 status + summary + task。日常优先用这个命令。

  bash scripts/v1-auto.sh pr <branch> "<title>" <risk>
      通过固定脚本创建 Pull Request（拉取请求）。如果 Remote Branch（远端分支）不存在，且当前本地分支就是目标分支，会先执行 Git Push（Git 分支推送）。
      示例:
      bash scripts/v1-auto.sh pr decisionresult-visual-verification-closure "docs(decision): record visual verification closure" A

  bash scripts/v1-auto.sh check-pr <PR_NUMBER>
      中文检查 PR 状态、文件范围、checks、是否可以继续。

  bash scripts/v1-auto.sh merge <PR_NUMBER> "<title>" <risk> [--confirm]
      通过固定脚本合并并同步。A-risk 可直接执行；B/B/C/C 默认停止，必须加 --confirm 表示已有用户明确同意。

  bash scripts/v1-auto.sh help
      显示本帮助。

安全规则:
  - 状态必须来自 scripts/v1-state.sh。
  - 下一任务必须来自 scripts/codex-next-task.sh。
  - 创建 PR 必须通过 scripts/v1-open-pr.sh。
  - 合并同步必须通过 scripts/v1-merge-sync.sh。
  - 不接 Push（推送通道）、Candidate（候选）、Decision generation（新决策生成）、Point（点位）、order/execution（订单/执行）、auto-trading（自动交易）。
  - Codex GH_NOT_AVAILABLE 表示 Codex GitHub 状态未知，不等于项目失败；需要本机 gh 或 GPT connector 证据补足。
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

capture_state() {
  bash scripts/v1-state.sh 2>&1 || true
}

translate_yes_no() {
  case "$1" in
    Yes|YES|yes) echo "Yes（是）" ;;
    No|NO|no) echo "No（否）" ;;
    *) echo "${1:-UNKNOWN（未知）}" ;;
  esac
}

describe_open_pr() {
  local open_prs="$1"
  case "$open_prs" in
    none|"")
      echo "none（没有未合并 PR）"
      ;;
    GH_NOT_AVAILABLE)
      echo "GH_NOT_AVAILABLE（Codex GitHub 状态未知；需要用户本机 gh 或 GPT connector 证据）"
      ;;
    *)
      echo "$open_prs"
      ;;
  esac
}

describe_can_continue() {
  local can_continue="$1"
  local blockers="$2"
  if [[ "$can_continue" == "YES" ]]; then
    echo "YES（可以继续）"
    return
  fi
  if [[ "$blockers" == "OPEN_PR_STATUS_UNKNOWN_GH_NOT_AVAILABLE" ]]; then
    echo "UNKNOWN（Codex 无法确认 GitHub；若用户本机 gh/GPT connector 已确认无 open PR，可作为交接证据继续）"
    return
  fi
  echo "NO（不能继续）"
}

current_status_summary() {
  local state_text="$1"
  local branch worktree head open_prs main_sync can_continue blockers
  local active_block active_block_cn current_level current_head next_action next_active next_phase next_branch next_main

  branch="$(state_value "$state_text" "BRANCH")"
  worktree="$(state_value "$state_text" "WORKTREE_CLEAN")"
  head="$(state_value "$state_text" "HEAD")"
  open_prs="$(state_value "$state_text" "OPEN_PRS")"
  main_sync="$(state_value "$state_text" "MAIN_SYNC")"
  can_continue="$(state_value "$state_text" "CAN_CONTINUE_NEXT_PACKAGE")"
  blockers="$(state_value "$state_text" "BLOCKERS")"

  active_block="$(yaml_value "$ACTIVE_FILE" "active_block")"
  active_block_cn="$(yaml_value "$ACTIVE_FILE" "active_block_cn")"
  current_level="$(yaml_value "$ACTIVE_FILE" "current_level")"
  current_head="$(yaml_value "$ACTIVE_FILE" "current_head")"
  next_action="$(yaml_value "$ACTIVE_FILE" "next_required_action")"

  next_active="$(yaml_value "$NEXT_TASK_FILE" "active_block")"
  next_phase="$(yaml_value "$NEXT_TASK_FILE" "phase")"
  next_branch="$(yaml_value "$NEXT_TASK_FILE" "branch")"
  next_main="$(yaml_value "$NEXT_TASK_FILE" "current_main")"

  echo "当前状态摘要（中文）"
  print_hr
  echo "当前模块: ${active_block:-UNKNOWN}（${active_block_cn:-未记录中文名}）"
  echo "当前阶段: workflow efficiency package（工作流提效包） / next task phase=${next_phase:-UNKNOWN}"
  echo "当前项目能力层级: ${current_level:-UNKNOWN}（Review-Only Runtime partial，只读运行时部分完成）"
  echo "ACTIVE current_head（事实源记录）: ${current_head:-UNKNOWN}"
  echo "实际当前 HEAD: ${head:-UNKNOWN}"
  echo "当前分支: ${branch:-UNKNOWN}"
  echo "Worktree Clean（工作区干净）: $(translate_yes_no "${worktree:-UNKNOWN}")"
  echo "Open PR（未合并 PR）: $(describe_open_pr "${open_prs:-UNKNOWN}")"
  echo "Main Sync（主分支同步）: ${main_sync:-UNKNOWN}"
  echo "是否可以继续: $(describe_can_continue "${can_continue:-UNKNOWN}" "${blockers:-UNKNOWN}")"
  echo "Blockers（阻塞）: ${blockers:-none}"
  echo "下一业务动作: ${next_action:-UNKNOWN}"
  echo "CODEX_NEXT_TASK（下一任务）: ${next_active:-UNKNOWN}"
  echo "下一任务分支: ${next_branch:-UNKNOWN}"
  echo "下一任务 main 基线: ${next_main:-UNKNOWN}"
}

cmd_status() {
  local state_text
  state_text="$(capture_state)"
  echo "$state_text"
  echo
  current_status_summary "$state_text"
}

cmd_summary() {
  local active_block next_action next_active next_branch
  active_block="$(yaml_value "$ACTIVE_FILE" "active_block")"
  next_action="$(yaml_value "$ACTIVE_FILE" "next_required_action")"
  next_active="$(yaml_value "$NEXT_TASK_FILE" "active_block")"
  next_branch="$(yaml_value "$NEXT_TASK_FILE" "branch")"

  echo "项目总进度摘要（白话版）"
  print_hr
  echo "已完成 4 个 Review-Only Runtime partial（只读运行时部分完成）小闭环:"
  echo "1. PositionSync（持仓同步）+ Dashboard（仪表盘）完整闭环。"
  echo "2. Watchlist + RuleConfig（观察列表 + 规则配置）+ Dashboard/API 完整闭环。"
  echo "3. MarketQuote（行情报价）freshness/fallback/dashboard API 完整闭环。"
  echo "4. Evidence / Score（证据 / 评分）review-only runtime status 完整闭环。"
  echo
  echo "DecisionResult（决策结果）当前进度:"
  echo "- implementation（实现）已完成并合并。"
  echo "- verification（验证）已完成并合并。"
  echo "- 下一步是 Visual Closure（视觉收口）：确认 dashboard 上 DecisionResult panel 真实可见、文案清楚、没有交易含义。"
  echo
  echo "当前暂停业务推进，正在做: ${active_block:-V1 Auto Operator Pack（V1 自动操作台包）}"
  echo "完成本 workflow efficiency package（工作流提效包）后，下一步恢复: ${next_active:-$next_action}"
  echo "建议下一任务分支: ${next_branch:-UNKNOWN}"
  echo
  echo "能力层级不变: REVIEW_ONLY_RUNTIME partial（只读运行时部分完成）。"
  echo "本包不会接 Push（推送）、Candidate（候选）、Decision generation（新决策生成）、Point（点位）或 Trading（交易）。"
}

cmd_task() {
  bash scripts/codex-next-task.sh >"$NEXT_TASK_CACHE"
  local copied="No（未复制）"
  if command -v pbcopy >/dev/null 2>&1; then
    if pbcopy <"$NEXT_TASK_CACHE"; then
      copied="Yes（已复制到剪贴板）"
    else
      copied="No（pbcopy 可用但复制失败；任务仍已写入临时文件）"
    fi
  fi

  echo "下一步 Codex 任务已生成。"
  echo "临时文件: $NEXT_TASK_CACHE"
  echo "剪贴板: $copied"
  echo
  echo "任务中文摘要:"
  print_hr
  echo "模块: $(yaml_value "$NEXT_TASK_FILE" "module")"
  echo "阶段: $(yaml_value "$NEXT_TASK_FILE" "active_block")"
  echo "分支: $(yaml_value "$NEXT_TASK_FILE" "branch")"
  echo "风险: $(yaml_value "$NEXT_TASK_FILE" "risk")"
  echo "允许改动: $(yaml_value "$NEXT_TASK_FILE" "allowed_changes")"
  echo "禁止范围: $(yaml_value "$NEXT_TASK_FILE" "forbidden")"
  echo "下一允许动作: $(yaml_value "$NEXT_TASK_FILE" "next_allowed_action")"
}

ensure_gh() {
  if ! command -v gh >/dev/null 2>&1; then
    echo "STOP（停止）: gh 不可用。GitHub 状态未知，需要用户本机 gh 或 GPT connector 证据。"
    exit 1
  fi
  if ! gh auth status >/dev/null 2>&1; then
    echo "STOP（停止）: Codex shell gh auth 不可用。按 #877 规则，这是 Codex GitHub 状态未知，不等于项目失败。"
    echo "请使用用户本机 terminal 或 GPT connector 完成 GitHub 状态确认。"
    exit 1
  fi
}

cmd_pr() {
  if [[ "$#" -lt 3 ]]; then
    echo "用法: bash scripts/v1-auto.sh pr <branch> \"<title>\" <risk>" >&2
    exit 1
  fi
  local branch="$1"
  local title="$2"
  local risk="$3"
  shift 3

  local output draft="true" pr_url pr_number current_branch push_output
  for arg in "$@"; do
    if [[ "$arg" == "--ready" ]]; then
      draft="false"
    fi
  done

  current_branch="$(git branch --show-current)"
  if ! git ls-remote --exit-code --heads origin "$branch" >/dev/null 2>&1; then
    if [[ "$current_branch" != "$branch" ]]; then
      echo "STOP（停止）: Remote Branch（远端分支）不存在: $branch"
      echo "当前本地分支是: $current_branch"
      echo "请先切换到目标分支后再运行: git checkout $branch"
      exit 1
    fi

    echo "Remote Branch（远端分支）不存在: $branch"
    echo "当前本地分支匹配目标分支，准备执行 Git Push（Git 分支推送，不是业务 Push 推送通道）。"
    if ! push_output="$(git push -u origin "$branch" 2>&1)"; then
      echo "$push_output"
      echo "STOP（停止）: Git Push（Git 分支推送）失败，未创建 Pull Request（拉取请求）。"
      exit 1
    fi
    echo "$push_output"
    echo "Git Push（Git 分支推送）完成，继续创建 Pull Request（拉取请求）。"
  fi

  if ! output="$(bash scripts/v1-open-pr.sh "$branch" "$title" "$risk" "$@" 2>&1)"; then
    echo "$output"
    echo "STOP（停止）: Pull Request（拉取请求）创建失败。"
    exit 1
  fi
  echo "固定脚本输出（可能包含英文原始信息）:"
  echo "$output"

  pr_url="$(printf '%s\n' "$output" | grep -Eo 'https://[^[:space:]]+/pull/[0-9]+' | tail -1 || true)"
  pr_number="$(printf '%s\n' "$pr_url" | sed -E 's#.*/pull/([0-9]+)#\1#')"

  echo
  echo "Pull Request（拉取请求）创建摘要（中文）"
  print_hr
  echo "Pull Request（拉取请求）编号: ${pr_number:-UNKNOWN}"
  echo "Pull Request（拉取请求）URL: ${pr_url:-UNKNOWN}"
  echo "分支: $branch"
  echo "风险等级: $risk"
  echo "Draft（草稿）: $draft"
  echo "下一步: bash scripts/v1-auto.sh check-pr ${pr_number:-<PR_NUMBER>}"
}

changed_files_for_pr() {
  local pr_number="$1"
  local head_ref="$2"
  if git show-ref --verify --quiet "refs/heads/$head_ref"; then
    git diff --name-only "main...$head_ref"
  else
    gh pr diff "$pr_number" --name-only
  fi
}

has_forbidden_business_path() {
  grep -Eq '^(src/main/java|src/test/java|src/main/resources/templates/dashboard\.html|src/main/resources/.*schema|src/main/resources/application[^/]*\.(yml|yaml|properties)|config/|pom\.xml)'
}

cmd_check_pr() {
  if [[ "$#" -ne 1 ]]; then
    echo "用法: bash scripts/v1-auto.sh check-pr <PR_NUMBER>" >&2
    exit 1
  fi
  ensure_gh

  local pr_number="$1"
  local state draft mergeable base head title checks changed_files forbidden="No"
  title="$(gh pr view "$pr_number" --json title --jq '.title')"
  state="$(gh pr view "$pr_number" --json state --jq '.state')"
  draft="$(gh pr view "$pr_number" --json isDraft --jq '.isDraft')"
  mergeable="$(gh pr view "$pr_number" --json mergeable --jq '.mergeable')"
  base="$(gh pr view "$pr_number" --json baseRefName --jq '.baseRefName')"
  head="$(gh pr view "$pr_number" --json headRefName --jq '.headRefName')"
  checks="$(gh pr view "$pr_number" --json statusCheckRollup --jq '[.statusCheckRollup[]? | (.name // .context // "check") + "=" + (.conclusion // .state // .status // "unknown")] | if length == 0 then "no checks reported" else join(", ") end')"
  changed_files="$(changed_files_for_pr "$pr_number" "$head")"

  if printf '%s\n' "$changed_files" | has_forbidden_business_path; then
    forbidden="Yes"
  fi

  echo "PR 检查摘要（中文）"
  print_hr
  echo "PR: #$pr_number $title"
  echo "State（状态）: $state"
  echo "Draft（草稿）: $draft"
  echo "Mergeable（可合并）: $mergeable"
  echo "Base（目标分支）: $base"
  echo "Head（来源分支）: $head"
  echo "Checks（检查）: $checks"
  echo
  echo "Changed files（变更文件）:"
  printf '%s\n' "$changed_files"
  echo

  if [[ "$forbidden" == "Yes" ]]; then
    echo "STOP（停止）: A-risk docs/scripts-only 自动流程发现 Java/tests/dashboard/schema/config/pom 等业务路径。"
    exit 1
  fi
  if [[ "$state" != "OPEN" ]]; then
    echo "STOP（停止）: PR 不是 OPEN（打开）状态。"
    exit 1
  fi
  if [[ "$mergeable" != "MERGEABLE" ]]; then
    echo "STOP（停止）: PR 当前不可合并。"
    exit 1
  fi

  echo "结果: PASS（通过）。未发现 A-risk 自动流程禁止的业务路径。"
}

cmd_merge() {
  if [[ "$#" -lt 3 ]]; then
    echo "用法: bash scripts/v1-auto.sh merge <PR_NUMBER> \"<title>\" <risk> [--confirm]" >&2
    exit 1
  fi
  local pr_number="$1"
  local title="$2"
  local risk="$3"
  shift 3
  local confirm="false"
  if [[ "${1:-}" == "--confirm" ]]; then
    confirm="true"
  fi

  if [[ "$risk" != "A" && "$confirm" != "true" ]]; then
    echo "STOP（停止）: $risk 风险默认不自动合并。"
    echo "需要用户明确同意后再运行: bash scripts/v1-auto.sh merge $pr_number \"$title\" $risk --confirm"
    exit 1
  fi

  cmd_check_pr "$pr_number"

  local merge_args=(scripts/v1-merge-sync.sh "$pr_number" "$title" --risk "$risk")
  if [[ "$risk" != "A" || "$confirm" == "true" ]]; then
    merge_args+=(--confirm)
  fi

  bash "${merge_args[@]}"
  echo
  echo "合并后状态摘要（中文）"
  print_hr
  bash scripts/v1-state.sh || true
  echo
  cat "$NEXT_TASK_FILE"
  echo
  bash scripts/codex-next-task.sh
}

cmd_next() {
  cmd_status
  echo
  cmd_summary
  echo
  cmd_task
}

main() {
  local cmd="${1:-help}"
  if [[ "$#" -gt 0 ]]; then
    shift
  fi

  case "$cmd" in
    help|-h|--help)
      usage
      if [[ "$#" -eq 0 ]]; then
        echo
        local state_text
        state_text="$(capture_state)"
        current_status_summary "$state_text"
      fi
      ;;
    status)
      cmd_status
      ;;
    summary)
      cmd_summary
      ;;
    task)
      cmd_task
      ;;
    pr)
      cmd_pr "$@"
      ;;
    check-pr)
      cmd_check_pr "$@"
      ;;
    merge)
      cmd_merge "$@"
      ;;
    next)
      cmd_next
      ;;
    *)
      echo "UNKNOWN_COMMAND（未知命令）: $cmd" >&2
      usage >&2
      exit 1
      ;;
  esac
}

main "$@"
