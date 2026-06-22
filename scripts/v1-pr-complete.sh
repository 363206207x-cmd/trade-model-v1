#!/usr/bin/env bash

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

usage() {
  cat <<'EOF'
用法:
  bash scripts/v1-pr-complete.sh <PR_NUMBER> <A|B|C> "<SUBJECT>" [--confirm-reviewed]

示例:
  bash scripts/v1-pr-complete.sh 893 A "docs(review-replay): verify review-only runtime wiring"
  bash scripts/v1-pr-complete.sh 892 B "feat(review-replay): show review-only runtime status"
  bash scripts/v1-pr-complete.sh 892 B "feat(review-replay): show review-only runtime status" --confirm-reviewed

说明:
  A-risk（低风险文档/脚本）在 PR 检查和 CI 通过后会自动 ready / merge / sync。
  B-risk（实现包）默认只检查，不自动合并；必须显式 --confirm-reviewed。
  C-risk（高风险）永远不自动合并。
  本脚本不会绕过 scripts/v1-merge-sync.sh。
EOF
}

if [[ "${1:-}" == "--help" || "${1:-}" == "-h" ]]; then
  usage
  exit 0
fi

if [[ "$#" -lt 3 ]]; then
  usage >&2
  exit 1
fi

PR_NUMBER="$1"
RISK="$2"
SUBJECT="$3"
shift 3

CONFIRM_REVIEWED="false"
while [[ "$#" -gt 0 ]]; do
  case "$1" in
    --confirm-reviewed)
      CONFIRM_REVIEWED="true"
      shift
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

case "$RISK" in
  A|B|C)
    ;;
  *)
    echo "STOP（停止）: unsupported risk（不支持的风险等级）: $RISK" >&2
    exit 1
    ;;
esac

ensure_gh() {
  if ! command -v gh >/dev/null 2>&1; then
    echo "GH_NOT_AVAILABLE_FOR_PR_MERGE"
    echo "NEXT_LOCAL_COMMAND: bash scripts/v1-pr-complete.sh $PR_NUMBER $RISK \"$SUBJECT\""
    echo "STOP（停止）: gh 不可用，无法检查 Pull Request（拉取请求）。"
    exit 1
  fi
  if ! gh auth status >/dev/null 2>&1; then
    echo "GH_NOT_AVAILABLE_FOR_PR_MERGE"
    echo "NEXT_LOCAL_COMMAND: bash scripts/v1-pr-complete.sh $PR_NUMBER $RISK \"$SUBJECT\""
    echo "STOP（停止）: Codex shell gh auth 不可用。"
    echo "这表示 Codex GitHub status unknown（Codex GitHub 状态未知），不是项目 main 失败。"
    echo "请使用用户本机 gh 或 GPT connector 完成 PR 检查 / 合并。"
    exit 1
  fi
}

print_hr() {
  echo "------------------------------------------------------------"
}

changed_files_for_pr() {
  local pr_number="$1"
  gh pr diff "$pr_number" --name-only
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

verify_target_pr() {
  local pr_number="$1"
  local expected_subject="$2"
  local expected_branch number title state is_draft base_ref head_ref

  if [[ "$pr_number" == "1004" ]]; then
    echo "STOP（停止）: PR #1004 is unrelated and protected; this flow must not modify, close, or merge it."
    exit 1
  fi

  expected_branch="$(yaml_value docs/CODEX_NEXT_TASK.yml branch)"
  [[ -n "$expected_branch" ]] || expected_branch="$(git branch --show-current)"

  number="$(gh pr view "$pr_number" --json number --jq '.number')"
  title="$(gh pr view "$pr_number" --json title --jq '.title')"
  state="$(gh pr view "$pr_number" --json state --jq '.state')"
  is_draft="$(gh pr view "$pr_number" --json isDraft --jq '.isDraft')"
  base_ref="$(gh pr view "$pr_number" --json baseRefName --jq '.baseRefName')"
  head_ref="$(gh pr view "$pr_number" --json headRefName --jq '.headRefName')"

  [[ "$number" == "$pr_number" ]] || { echo "STOP（停止）: target PR mismatch: $number != $pr_number"; exit 1; }
  [[ "$title" == "$expected_subject" ]] || { echo "STOP（停止）: target PR title mismatch: $title"; exit 1; }
  [[ "$state" == "OPEN" ]] || { echo "STOP（停止）: target PR is not OPEN: $state"; exit 1; }
  [[ "$is_draft" == "false" ]] || { echo "STOP（停止）: A-risk current package PR must not be Draft."; exit 1; }
  [[ "$base_ref" == "main" ]] || { echo "STOP（停止）: target PR base is not main: $base_ref"; exit 1; }
  [[ "$head_ref" == "$expected_branch" ]] || { echo "STOP（停止）: target PR head is not current package branch: $head_ref != $expected_branch"; exit 1; }
}

verify_a_risk_scope() {
  local pr_number="$1"
  local file
  local forbidden=()

  while IFS= read -r file; do
    [[ -z "$file" ]] && continue
    case "$file" in
      AGENTS.md|docs/*|scripts/*.sh)
        ;;
      *)
        forbidden+=("$file")
        ;;
    esac

    case "$file" in
      *.java|src/main/java/*|src/test/*|src/main/resources/schema.sql|src/main/resources/db/*|src/main/resources/templates/*|src/main/resources/static/*|pom.xml|src/main/resources/application.yml|src/main/resources/application.properties|src/main/resources/application-*.yml|src/main/resources/application-*.properties)
        forbidden+=("$file")
        ;;
    esac

    if printf '%s\n' "$file" | grep -Eiq '(^|/)schema|dashboard|pom\.xml|runtime config'; then
      forbidden+=("$file")
    fi
  done < <(changed_files_for_pr "$pr_number")

  if (( ${#forbidden[@]} > 0 )); then
    echo "STOP（停止）: A-risk scope violation（低风险范围越界）:"
    printf '%s\n' "${forbidden[@]}" | awk '!seen[$0]++' | sed 's/^/- /'
    exit 1
  fi

  echo "A_RISK_SCOPE_OK"
}

print_pr_snapshot() {
  local pr_number="$1"
  echo "Pull Request（拉取请求）状态:"
  gh pr view "$pr_number" --json number,title,state,isDraft,mergeable,statusCheckRollup
  echo
  echo "Changed files（变更文件）:"
  changed_files_for_pr "$pr_number" | sed 's/^/- /'
}

check_required_ci_once() {
  local pr_number="$1"
  local check_rows
  if ! check_rows="$(gh pr checks "$pr_number" --json name,state,conclusion,bucket,workflow --jq '.[] | [.name, (.state // ""), (.conclusion // ""), (.bucket // ""), (.workflow // "")] | @tsv' 2>/dev/null)"; then
    check_rows="$(gh pr checks "$pr_number" --json name,state,bucket,workflow --jq '.[] | [.name, (.state // ""), "", (.bucket // ""), (.workflow // "")] | @tsv')"
  fi

  local quality_seen=0
  local quality_pending=0
  local quality_failed=0
  local workflow_seen=0
  local workflow_pending=0
  local workflow_failed=0
  local name state conclusion bucket workflow_name status

  while IFS=$'\t' read -r name state conclusion bucket workflow_name; do
    [[ -z "${name:-}" ]] && continue
    status="$(classify_check "$state" "$conclusion" "$bucket")"
    if is_quality_gate_check "$name" "$workflow_name"; then
      quality_seen=$((quality_seen + 1))
      case "$status" in
        pending) quality_pending=$((quality_pending + 1)) ;;
        failed) quality_failed=$((quality_failed + 1)) ;;
      esac
    fi
    if is_workflow_contract_check "$name" "$workflow_name"; then
      workflow_seen=$((workflow_seen + 1))
      case "$status" in
        pending) workflow_pending=$((workflow_pending + 1)) ;;
        failed) workflow_failed=$((workflow_failed + 1)) ;;
      esac
    fi
  done <<<"$check_rows"

  local quality workflow
  quality="$(rollup_required_check "$quality_seen" "$quality_pending" "$quality_failed")"
  workflow="$(rollup_required_check "$workflow_seen" "$workflow_pending" "$workflow_failed")"

  echo "quality-gate=$quality workflow-contract=$workflow"

  if [[ "$quality" == "success" && "$workflow" == "success" ]]; then
    return 0
  fi
  if [[ "$quality" == "pending" || "$workflow" == "pending" ]]; then
    return 2
  fi
  return 1
}

is_quality_gate_check() {
  local name="$1"
  local workflow_name="${2:-}"
  [[ "$name" == "quality-gate" || "$name" == "ci/quality-gate" || "$name" == */quality-gate || "$workflow_name" == "quality-gate" || "$workflow_name" == "ci/quality-gate" || "$workflow_name" == */quality-gate ]]
}

is_workflow_contract_check() {
  local name="$1"
  local workflow_name="${2:-}"
  [[ "$name" == "workflow-contract" || "$name" == "ci/workflow-contract" || "$name" == */workflow-contract || "$workflow_name" == "workflow-contract" || "$workflow_name" == "ci/workflow-contract" || "$workflow_name" == */workflow-contract ]]
}

rollup_required_check() {
  local seen="$1"
  local pending="$2"
  local failed="$3"
  if (( seen == 0 )); then
    echo "missing"
  elif (( failed > 0 )); then
    echo "failed"
  elif (( pending > 0 )); then
    echo "pending"
  else
    echo "success"
  fi
}

normalize_check_value() {
  printf '%s' "${1:-}" | tr '[:upper:]' '[:lower:]' | tr ' ' '_' | tr '-' '_'
}

classify_check() {
  local state="$1"
  local conclusion="$2"
  local bucket="$3"
  local state_norm conclusion_norm bucket_norm combined_norm
  state_norm="$(normalize_check_value "$state")"
  conclusion_norm="$(normalize_check_value "$conclusion")"
  bucket_norm="$(normalize_check_value "$bucket")"
  combined_norm="${state_norm}+${conclusion_norm}"

  if [[ "$state_norm" == "success" || "$state_norm" == "successful" || "$conclusion_norm" == "success" || "$conclusion_norm" == "successful" || "$bucket_norm" == "pass" || "$bucket_norm" == "success" || "$bucket_norm" == "successful" || "$combined_norm" == "completed+success" || "$combined_norm" == "completed+successful" ]]; then
    echo "success"
    return
  fi
  if [[ -z "$state_norm$conclusion_norm$bucket_norm" || "$state_norm" == "in_progress" || "$state_norm" == "pending" || "$state_norm" == "queued" || "$state_norm" == "waiting" || "$state_norm" == "requested" || "$state_norm" == "expected" || "$conclusion_norm" == "in_progress" || "$conclusion_norm" == "pending" || "$conclusion_norm" == "queued" || "$bucket_norm" == "pending" ]]; then
    echo "pending"
    return
  fi
  if [[ "$state_norm" == "failing" || "$state_norm" == "failed" || "$state_norm" == "failure" || "$state_norm" == "cancelled" || "$state_norm" == "canceled" || "$state_norm" == "timed_out" || "$conclusion_norm" == "failing" || "$conclusion_norm" == "failed" || "$conclusion_norm" == "failure" || "$conclusion_norm" == "cancelled" || "$conclusion_norm" == "canceled" || "$conclusion_norm" == "timed_out" || "$bucket_norm" == "fail" || "$bucket_norm" == "failed" || "$bucket_norm" == "failure" ]]; then
    echo "failed"
    return
  fi
  echo "failed"
}

wait_for_ci() {
  local pr_number="$1"
  local max_attempts=90
  local attempt=1
  local result

  echo "等待 CI（持续集成）完成：quality-gate 和 workflow-contract 必须 SUCCESS（成功）。"
  while (( attempt <= max_attempts )); do
    set +e
    result="$(check_required_ci_once "$pr_number")"
    local status=$?
    set -e

    echo "CI attempt（尝试） $attempt/$max_attempts: $result"
    if [[ "$status" -eq 0 ]]; then
      echo "CI_RESULT（CI 结果）: SUCCESS（成功）"
      return 0
    fi
    if [[ "$status" -eq 1 ]]; then
      echo "STOP（停止）: CI 未通过或必需检查缺失。"
      gh pr checks "$pr_number" || true
      return 1
    fi

    sleep 10
    attempt=$((attempt + 1))
  done

  echo "STOP（停止）: CI 等待超过 15 分钟，未合并。"
  return 1
}

mark_ready_if_needed() {
  local pr_number="$1"
  local is_draft
  is_draft="$(gh pr view "$pr_number" --json isDraft --jq '.isDraft')"
  if [[ "$is_draft" == "true" ]]; then
    echo "PR 仍是 Draft（草稿），准备标记 Ready（可审查）。"
    gh pr ready "$pr_number"
  else
    echo "PR 已经是 Ready（可审查），无需重复标记。"
  fi
}

print_post_merge_baseline_note() {
  local actual_head
  local source_head
  actual_head="$(git rev-parse --short HEAD 2>/dev/null || true)"
  source_head="$(awk -F': *' '$1 == "current_head" {value=$0; sub("^[^:]*:[[:space:]]*", "", value); gsub(/^"/, "", value); gsub(/"$/, "", value); print value; exit}' docs/ACTIVE_MAINLINE_STATUS.yml 2>/dev/null || true)"
  echo "最新 main HEAD（实际当前 HEAD）: ${actual_head:-UNKNOWN}"
  echo "Source of Truth current_head（事实源当前主线 HEAD）: ${source_head:-UNKNOWN}"
  if [[ -n "$actual_head" && -n "$source_head" && "$source_head" != "$actual_head" ]]; then
    echo "提示: 不再创建 baseline sync（基线同步）小包。下一业务包将使用 actual HEAD（实际 HEAD）作为执行基线，并在业务包内顺手更新 source-of-truth（事实源）。"
  fi
}

ensure_gh

echo "V1 PR Complete（一键 PR 完成）"
print_hr
echo "PR 编号: #$PR_NUMBER"
echo "Risk（风险等级）: $RISK"
echo "Subject（合并标题）: $SUBJECT"
echo

verify_target_pr "$PR_NUMBER" "$SUBJECT"
if [[ "$RISK" == "A" ]]; then
  verify_a_risk_scope "$PR_NUMBER"
fi
echo

bash scripts/v1-auto.sh check-pr "$PR_NUMBER" "$RISK"
echo
print_pr_snapshot "$PR_NUMBER"
echo

if [[ "$RISK" == "C" ]]; then
  echo "STOP（停止）: C-risk（高风险）永远不自动合并，需要人工复核。"
  exit 1
fi

wait_for_ci "$PR_NUMBER"
echo

case "$RISK" in
  A)
    mark_ready_if_needed "$PR_NUMBER"
    echo "A-risk（低风险）检查通过，调用固定合并脚本 v1-merge-sync.sh。"
    bash scripts/v1-merge-sync.sh "$PR_NUMBER" "$SUBJECT" --risk A --confirm
    echo
    echo "合并后状态:"
    bash scripts/v1-state.sh || true
    print_post_merge_baseline_note
    echo
    bash scripts/v1-auto.sh next || true
    ;;
  B)
    if [[ "$CONFIRM_REVIEWED" != "true" ]]; then
      echo "B-risk（实现包）已通过检查，需要人工/助手复核后再合并。"
      echo "如已明确复核并同意，可运行:"
      echo "bash scripts/v1-pr-complete.sh $PR_NUMBER B \"$SUBJECT\" --confirm-reviewed"
      exit 0
    fi
    echo "B-risk（实现包）已带 --confirm-reviewed，重新显示风险边界并调用固定合并脚本。"
    print_pr_snapshot "$PR_NUMBER"
    echo
    echo "风险边界: 不得接 Push（推送）、Candidate（候选）、Decision generation（新决策生成）、Point（点位）、entry/stop/TP/RR（入场/止损/止盈/盈亏比）、order/execution（订单/执行）、auto-trading（自动交易）。"
    mark_ready_if_needed "$PR_NUMBER"
    bash scripts/v1-merge-sync.sh "$PR_NUMBER" "$SUBJECT" --risk B --confirm
    echo
    bash scripts/v1-state.sh || true
    print_post_merge_baseline_note
    echo
    bash scripts/v1-auto.sh next || true
    ;;
esac
