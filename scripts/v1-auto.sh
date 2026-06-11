#!/usr/bin/env bash

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

ACTIVE_FILE="docs/ACTIVE_MAINLINE_STATUS.yml"
NEXT_TASK_FILE="docs/CODEX_NEXT_TASK.yml"
PROGRESS_FILE="docs/V1_PROGRESS_SOURCE_OF_TRUTH.md"
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

  bash scripts/v1-auto.sh check-pr <PR_NUMBER> [A|B|B/C|C]
      中文检查 PR 状态、文件范围、checks、是否可以继续。
      不传 risk 时保持旧行为：按 A-risk docs/scripts-only（文档/脚本）范围检查。
      A-risk（低风险文档/脚本）: 只允许 docs/、scripts/ 等 workflow/doc 变更。
      B-risk（实现包）: 允许当前最小实现常用路径，但不自动合并。
      B/C-risk 或 C-risk（高风险）: 只检查并停止，需要人工复核。

  bash scripts/v1-pr-complete.sh <PR_NUMBER> <A|B|C> "<subject>"
      一键完成 PR 检查 / 等 CI / ready / merge / next 的固定入口。
      A-risk 可在检查通过后自动合并；B-risk 默认不合并，需 --confirm-reviewed。

  bash scripts/v1-codex-run-next.sh
      一键读取下一任务并尝试启动 Codex CLI；不自动提交、不创建 PR、不合并。
      如果 Codex shell 无法确认 Open PR（未合并 PR），但 GPT connector 或用户本机 terminal 已确认 Open PR none，可使用:
      bash scripts/v1-codex-run-next.sh --open-pr-none-confirmed

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

phase_label() {
  local phase="$1"
  local active="${2:-}"
  if [[ "$active" == *"Next minimal runtime slice selection"* ]]; then
    echo "Selection（选择下一个最小运行时模块）"
    return
  fi
  case "$phase" in
    source_read) echo "Source Read（源码读取）" ;;
    design) echo "Design（设计）" ;;
    readiness|readiness_gate) echo "Readiness Gate（实现前就绪门）" ;;
    implementation) echo "Implementation（实现）" ;;
    verification) echo "Verification（验证）" ;;
    visual_closure) echo "Visual Closure（视觉收口）" ;;
    *) echo "${phase:-UNKNOWN}（未知阶段）" ;;
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

completed_slice_entries() {
  if [[ ! -f "$PROGRESS_FILE" ]]; then
    return 0
  fi
  awk '
    /^Completed review-only runtime slices:/ {in_list=1; next}
    in_list && /^$/ {next}
    in_list && /^[0-9]+[.][[:space:]]/ {print; next}
    in_list && /^[^0-9]/ {exit}
  ' "$PROGRESS_FILE"
}

completed_slice_count() {
  local count
  count="$(completed_slice_entries | awk 'END {print NR + 0}')"
  echo "${count:-0}"
}

slice_display_name() {
  local raw="$1"
  local index name
  index="${raw%%.*}"
  name="${raw#*. }"
  name="${name%%: *}"
  name="${name//\`/}"
  case "$name" in
    "PositionSync + Dashboard review-only status")
      echo "$index. PositionSync（持仓同步）+ Dashboard（仪表盘）review-only status（只读运行时状态）完整闭环。"
      ;;
    "Watchlist + RuleConfig + Dashboard/API review-only status")
      echo "$index. Watchlist + RuleConfig（观察列表 + 规则配置）+ Dashboard/API（仪表盘 / 接口）review-only status（只读运行时状态）完整闭环。"
      ;;
    "MarketQuote freshness / fallback / dashboard API status")
      echo "$index. MarketQuote（行情报价）freshness/fallback/dashboard API（新鲜度 / 兜底 / 仪表盘接口）完整闭环。"
      ;;
    "Evidence / Score review-only runtime status")
      echo "$index. Evidence / Score（证据 / 评分）review-only runtime status（只读运行时状态）完整闭环。"
      ;;
    "DecisionResult review-only dashboard/API status")
      echo "$index. DecisionResult（决策结果）review-only dashboard/API status（只读仪表盘 / 接口状态）完整闭环。"
      ;;
    "ExecutionPlan / BoundaryCandidate review-only runtime status")
      echo "$index. ExecutionPlan / BoundaryCandidate（执行计划 / 边界候选）review-only runtime status（只读运行时状态）完整闭环。"
      ;;
    "Review / Replay result status")
      echo "$index. Review / Replay result status（复盘 / 回放结果状态）review-only runtime status（只读运行时状态）完整闭环。"
      ;;
    "Data Source Health dashboard/API status")
      echo "$index. Data Source Health dashboard/API status（数据源健康仪表盘 / 接口状态）review-only runtime status（只读运行时状态）完整闭环。"
      ;;
    *)
      echo "$index. $name（已完成 Review-Only Runtime partial，只读运行时部分完成）完整闭环。"
      ;;
  esac
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
  echo "当前阶段: $(phase_label "${next_phase:-UNKNOWN}" "${next_active:-$active_block}")"
  echo "当前项目能力层级: ${current_level:-UNKNOWN}（Review-Only Runtime partial，只读运行时部分完成）"
  echo "ACTIVE current_head（Source of Truth 当前主线 HEAD）: ${current_head:-UNKNOWN}"
  echo "实际当前 HEAD: ${head:-UNKNOWN}"
  if [[ -n "${current_head:-}" && -n "${head:-}" && "$head" != "$current_head"* ]]; then
    echo "HEAD 差异说明: Source of Truth 当前主线 HEAD 与实际当前 HEAD 不一致。若差异来自 workflow-only（纯工作流）commit，通常不阻塞；若差异来自业务包滞后，需要修正 source-of-truth baseline（事实源基线）。"
  fi
  echo "当前分支: ${branch:-UNKNOWN}"
  echo "Worktree Clean（工作区干净）: $(translate_yes_no "${worktree:-UNKNOWN}")"
  echo "Open PR（未合并 PR）: $(describe_open_pr "${open_prs:-UNKNOWN}")"
  echo "Main Sync（主分支同步）: ${main_sync:-UNKNOWN}"
  echo "是否可以继续: $(describe_can_continue "${can_continue:-UNKNOWN}" "${blockers:-UNKNOWN}")"
  echo "Blockers（阻塞）: ${blockers:-none}"
  echo "下一业务动作: ${next_action:-UNKNOWN}"
  echo "CODEX_NEXT_TASK（下一任务配置）: ${next_active:-UNKNOWN}"
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
  local active_block next_action next_active next_branch next_phase
  local slice_count slice_entries entry
  active_block="$(yaml_value "$ACTIVE_FILE" "active_block")"
  next_action="$(yaml_value "$ACTIVE_FILE" "next_required_action")"
  next_active="$(yaml_value "$NEXT_TASK_FILE" "active_block")"
  next_branch="$(yaml_value "$NEXT_TASK_FILE" "branch")"
  next_phase="$(yaml_value "$NEXT_TASK_FILE" "phase")"
  slice_count="$(completed_slice_count)"
  slice_entries="$(completed_slice_entries)"

  echo "项目总进度摘要（白话版）"
  print_hr
  echo "已完成 ${slice_count:-0} 个 Review-Only Runtime partial（只读运行时部分完成）小闭环:"
  if [[ -n "$slice_entries" ]]; then
    while IFS= read -r entry; do
      [[ -z "$entry" ]] && continue
      slice_display_name "$entry"
    done <<<"$slice_entries"
  else
    echo "- Source of Truth（事实源）未记录 completed slice list（已完成切片列表）。"
  fi
  echo
  echo "DecisionResult（决策结果）当前进度:"
  echo "- implementation（实现）已完成并合并。"
  echo "- verification（验证）已完成并合并。"
  echo "- visual closure（视觉收口）已完成并合并。"
  echo
  echo "ExecutionPlan / BoundaryCandidate（执行计划 / 边界候选）当前进度:"
  echo "- implementation（实现）已完成并合并。"
  echo "- verification（验证）已完成并合并。"
  echo "- visual closure（视觉收口）已完成并合并。"
  echo
  echo "Review / Replay result status（复盘 / 回放结果状态）当前进度:"
  echo "- implementation（实现）已完成并合并。"
  echo "- verification（验证）已完成并合并。"
  echo "- visual closure（视觉收口）已完成并合并。"
  echo
  echo "Data Source Health dashboard/API status（数据源健康仪表盘 / 接口状态）当前进度:"
  echo "- implementation（实现）已完成并合并。"
  echo "- verification（验证）已完成并合并。"
  echo "- visual closure（视觉收口）已完成并合并。"
  echo
  echo "当前模块: ${active_block:-UNKNOWN}"
  echo "下一业务动作: ${next_active:-$next_action}（$(phase_label "${next_phase:-UNKNOWN}" "${next_active:-$active_block}")）"
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
  echo "Module（模块）: $(yaml_value "$NEXT_TASK_FILE" "module")"
  echo "Phase（阶段）: $(yaml_value "$NEXT_TASK_FILE" "active_block")"
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

diff_for_pr() {
  local pr_number="$1"
  local head_ref="$2"
  if git show-ref --verify --quiet "refs/heads/$head_ref"; then
    git diff "main...$head_ref"
  else
    gh pr diff "$pr_number"
  fi
}

is_a_risk_allowed_file() {
  local file="$1"
  case "$file" in
    docs/*|scripts/*|AGENTS.md)
      return 0
      ;;
    *)
      return 1
      ;;
  esac
}

is_b_risk_allowed_file() {
  local file="$1"
  case "$file" in
    src/main/java/org/example/trademodel/controller/DashboardController.java|\
src/main/resources/templates/dashboard.html|\
src/test/java/org/example/trademodel/controller/DashboardControllerTest.java|\
docs/*|scripts/*)
      return 0
      ;;
    *)
      return 1
      ;;
  esac
}

is_schema_config_pom_path() {
  local file="$1"
  case "$file" in
    pom.xml|config/*|src/main/resources/schema.sql|src/main/resources/db/*|src/main/resources/*schema*|src/main/resources/application*.yml|src/main/resources/application*.yaml|src/main/resources/application*.properties)
      return 0
      ;;
    *)
      return 1
      ;;
  esac
}

is_dto_validator_assembler_path() {
  local file="$1"
  case "$file" in
    *DTO*.java|*Validator*.java|*Assembler*.java)
      return 0
      ;;
    *)
      return 1
      ;;
  esac
}

print_changed_files_with_prefix() {
  local changed_files="$1"
  if [[ -z "$changed_files" ]]; then
    echo "- none（无变更文件）"
    return
  fi
  printf '%s\n' "$changed_files" | sed 's/^/- /'
}

classify_changed_files_for_risk() {
  local risk="$1"
  local changed_files="$2"
  local violations=()
  local file

  while IFS= read -r file; do
    [[ -z "$file" ]] && continue
    case "$risk" in
      A|LEGACY)
        if ! is_a_risk_allowed_file "$file"; then
          violations+=("$file")
        fi
        ;;
      B)
        if ! is_b_risk_allowed_file "$file"; then
          violations+=("$file")
          continue
        fi
        if is_schema_config_pom_path "$file" || is_dto_validator_assembler_path "$file"; then
          violations+=("$file")
        fi
        ;;
      C)
        ;;
    esac
  done <<<"$changed_files"

  if [[ "${#violations[@]}" -gt 0 ]]; then
    printf '%s\n' "${violations[@]}"
  fi
}

is_allowed_negative_safety_assertion() {
  local line="$1"
  printf '%s\n' "$line" | grep -Eiq 'doesNotExist|does not expose|does not contain|No final direction|No entry|No stop|No TP|No RR|notTradingSignal|notCandidateSignal|notDecisionGeneration|notPointSignal|notExecutable|externalRefreshTriggered[[:space:]]*[=:][[:space:]]*false|displaySlotsAreCandidatePool[[:space:]]*[=:][[:space:]]*false|failClosed|forbidden scope|Forbidden scope|forbidden-field absence|absence check|negative safety|negative guardrail|禁止范围|禁止|不得|不能|不触发|不生成|不接|不是|不可执行|只读|非交易'
}

is_forbidden_positive_semantic_line() {
  local line="$1"
  if printf '%s\n' "$line" | grep -Eq 'finalDirection|takeProfit|riskReward|positionSize|leverage|orderAction|executionAction|autoTradingAction|candidateRanking|pushSend|externalRefreshTriggered[[:space:]]*[=:][[:space:]]*true|schedulerTrigger|collectorTrigger|apiClientTrigger|API-client trigger|external API refresh trigger'; then
    return 0
  fi
  if printf '%s\n' "$line" | grep -Eq "\"(entry|stop|tp|rr)\"[[:space:]]*[:=]|'(entry|stop|tp|rr)'[[:space:]]*[:=]|\\.put\\(\"(entry|stop|tp|rr)\""; then
    return 0
  fi
  return 1
}

diff_allowed_negative_safety_assertions() {
  local diff_text="$1"
  local line
  while IFS= read -r line; do
    [[ "$line" == +* && "$line" != +++* ]] || continue
    if is_allowed_negative_safety_assertion "$line"; then
      printf '%s\n' "$line"
    fi
  done <<<"$diff_text"
}

diff_has_forbidden_positive_semantics() {
  local diff_text="$1"
  local line
  while IFS= read -r line; do
    [[ "$line" == +* && "$line" != +++* ]] || continue
    if is_allowed_negative_safety_assertion "$line"; then
      continue
    fi
    if is_forbidden_positive_semantic_line "$line"; then
      printf '%s\n' "$line"
      continue
    fi
    printf '%s\n' "$line" | grep -E '\+.*(placeOrder|createOrder|submitOrder|autoTradingAction|entryPrice|stopPrice|tpPrice|sendPush|executeReplay|runReplay|generateReviewResult)' || true
  done <<<"$diff_text"
}

cmd_check_pr() {
  if [[ "$#" -lt 1 || "$#" -gt 2 ]]; then
    echo "用法: bash scripts/v1-auto.sh check-pr <PR_NUMBER> [A|B|B/C|C]" >&2
    exit 1
  fi
  ensure_gh

  local pr_number="$1"
  local risk="${2:-LEGACY}"
  local state draft mergeable base head title checks changed_files violations diff_text forbidden_hits="" allowed_negative_hits=""
  case "$risk" in
    LEGACY|A|B|"B/C"|C)
      ;;
    *)
      echo "STOP（停止）: unsupported risk（不支持的风险等级）: $risk" >&2
      exit 1
      ;;
  esac

  title="$(gh pr view "$pr_number" --json title --jq '.title')"
  state="$(gh pr view "$pr_number" --json state --jq '.state')"
  draft="$(gh pr view "$pr_number" --json isDraft --jq '.isDraft')"
  mergeable="$(gh pr view "$pr_number" --json mergeable --jq '.mergeable')"
  base="$(gh pr view "$pr_number" --json baseRefName --jq '.baseRefName')"
  head="$(gh pr view "$pr_number" --json headRefName --jq '.headRefName')"
  checks="$(gh pr view "$pr_number" --json statusCheckRollup --jq '[.statusCheckRollup[]? | (.name // .context // "check") + "=" + (.conclusion // .state // .status // "unknown")] | if length == 0 then "no checks reported" else join(", ") end')"
  changed_files="$(changed_files_for_pr "$pr_number" "$head")"
  violations="$(classify_changed_files_for_risk "$risk" "$changed_files")"
  if [[ "$risk" == "B" ]]; then
    diff_text="$(diff_for_pr "$pr_number" "$head")"
    forbidden_hits="$(diff_has_forbidden_positive_semantics "$diff_text")"
    allowed_negative_hits="$(diff_allowed_negative_safety_assertions "$diff_text")"
  fi

  echo "PR 检查摘要（中文）"
  print_hr
  echo "PR: #$pr_number $title"
  if [[ "$risk" == "LEGACY" ]]; then
    echo "Risk（风险等级）: LEGACY（旧行为，等同 A-risk docs/scripts-only 文档/脚本检查）"
  else
    echo "Risk（风险等级）: $risk"
  fi
  echo "State（状态）: $state"
  echo "Draft（草稿）: $draft"
  echo "Mergeable（可合并）: $mergeable"
  echo "Base（目标分支）: $base"
  echo "Head（来源分支）: $head"
  echo "Checks（检查）: $checks"
  echo
  echo "Changed files（变更文件）:"
  print_changed_files_with_prefix "$changed_files"
  echo

  if [[ "$risk" == "C" || "$risk" == "B/C" ]]; then
    echo "STOP（停止）: $risk-risk（高风险）只做检查，不自动继续；需要人工复核。"
    exit 1
  fi
  if [[ -n "$violations" ]]; then
    echo "STOP（停止）: 文件范围与 $risk risk（风险等级）不匹配。"
    echo "违规文件:"
    print_changed_files_with_prefix "$violations"
    if [[ "$risk" == "B" ]]; then
      echo "B-risk（实现包）仅允许 DashboardController.java、dashboard.html、DashboardControllerTest.java、docs/*、scripts/*。"
    else
      echo "A-risk（低风险）仅允许 docs/*、scripts/* 等 workflow/doc 变更。"
    fi
    exit 1
  fi
  if [[ -n "$forbidden_hits" ]]; then
    echo "STOP（停止）: B-risk diff（差异）中出现正向禁用语义，需要人工修正或重新拆包。"
    echo "$forbidden_hits"
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

  case "$risk" in
    B)
      echo "结果: PASS（通过）。B-risk（实现包）文件范围与语义分类通过；默认不自动合并，需要人工/助手复核后再合并。"
      if [[ -n "$allowed_negative_hits" ]]; then
        echo "负向安全断言: 已识别为 allowed negative safety assertions（允许的负向安全断言），不会误判为正向越界。"
      else
        echo "负向安全断言: 本次 diff 未发现需要分类的 allowed negative safety assertions（允许的负向安全断言）。"
      fi
      echo "真正正向危险语义仍会 STOP（停止），例如 finalDirection / entryPrice / stopPrice / takeProfit / riskReward / orderAction / executionAction / autoTradingAction / candidateRanking / pushSend / externalRefreshTriggered=true。"
      ;;
    LEGACY|A)
      echo "结果: PASS（通过）。未发现 A-risk（低风险）自动流程禁止的业务路径。"
      ;;
  esac
  echo "是否允许继续: Yes（允许继续后续检查）；是否自动合并取决于 risk（风险等级）和 v1-pr-complete.sh 规则。"
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

  cmd_check_pr "$pr_number" "$risk"

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
