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
    echo "STOP（停止）: gh 不可用，无法检查 Pull Request（拉取请求）。"
    exit 1
  fi
  if ! gh auth status >/dev/null 2>&1; then
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
  check_rows="$(gh pr checks "$pr_number" --json name,state,conclusion,bucket --jq '.[] | [.name, (.state // ""), (.conclusion // ""), (.bucket // "")] | @tsv')"

  local quality="missing"
  local workflow="missing"
  local name state conclusion bucket

  while IFS=$'\t' read -r name state conclusion bucket; do
    case "$name" in
      quality-gate)
        quality="$(classify_check "$state" "$conclusion" "$bucket")"
        ;;
      workflow-contract)
        workflow="$(classify_check "$state" "$conclusion" "$bucket")"
        ;;
    esac
  done <<<"$check_rows"

  echo "quality-gate=$quality workflow-contract=$workflow"

  if [[ "$quality" == "success" && "$workflow" == "success" ]]; then
    return 0
  fi
  if [[ "$quality" == "pending" || "$workflow" == "pending" ]]; then
    return 2
  fi
  return 1
}

classify_check() {
  local state="$1"
  local conclusion="$2"
  local bucket="$3"
  if [[ "$conclusion" == "SUCCESS" || "$conclusion" == "success" || "$bucket" == "pass" ]]; then
    echo "success"
    return
  fi
  if [[ "$state" == "IN_PROGRESS" || "$state" == "PENDING" || "$state" == "QUEUED" || "$state" == "in_progress" || "$state" == "pending" || "$state" == "queued" || "$bucket" == "pending" ]]; then
    echo "pending"
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

ensure_gh

echo "V1 PR Complete（一键 PR 完成）"
print_hr
echo "PR 编号: #$PR_NUMBER"
echo "Risk（风险等级）: $RISK"
echo "Subject（合并标题）: $SUBJECT"
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
    echo
    bash scripts/v1-auto.sh next || true
    ;;
esac
