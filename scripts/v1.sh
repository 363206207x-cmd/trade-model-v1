#!/usr/bin/env bash

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

required_scripts=(
  "scripts/v1-status.sh"
  "scripts/v1-session-bootstrap.sh"
  "scripts/v1-pr-review-input.sh"
  "scripts/v1-merge-sync.sh"
  "scripts/v1-safe-check.sh"
  "scripts/v1-next-pack-context.sh"
)

for script in "${required_scripts[@]}"; do
  if [[ ! -f "$script" ]]; then
    echo "MISSING_SCRIPT: $script" >&2
    exit 1
  fi
done

print_menu() {
  echo "V1 Workflow Menu"
  echo "1. Status（查看当前状态）"
  echo "2. Session Bootstrap（新窗口启动）"
  echo "3. Review PR（生成 PR 审查输入）"
  echo "4. Merge PR after approval（确认后合并 PR）"
  echo "5. Safe Check（安全检查）"
  echo "6. Next Pack Context（下一包上下文）"
  echo "7. Exit（退出）"
}

while true; do
  print_menu
  printf "Choose an option [1-7]: "

  if ! IFS= read -r choice; then
    echo "No option selected." >&2
    exit 1
  fi

  case "$choice" in
    1)
      bash scripts/v1-status.sh
      ;;
    2)
      bash scripts/v1-session-bootstrap.sh
      ;;
    3)
      printf "PR number: "
      if ! IFS= read -r pr_number || [[ -z "$pr_number" ]]; then
        echo "usage: bash scripts/v1-pr-review-input.sh <PR_NUMBER>" >&2
        exit 1
      fi
      bash scripts/v1-pr-review-input.sh "$pr_number"
      ;;
    4)
      echo "Only run this after user explicitly said: 同意合并 PR #xxx"
      echo "（只能在用户明确说“同意合并 PR #xxx”后运行）"
      printf "PR number: "
      if ! IFS= read -r pr_number || [[ -z "$pr_number" ]]; then
        echo 'usage: bash scripts/v1-merge-sync.sh <PR_NUMBER> "<SQUASH_SUBJECT>"' >&2
        exit 1
      fi
      printf "squash subject: "
      if ! IFS= read -r squash_subject || [[ -z "$squash_subject" ]]; then
        echo 'usage: bash scripts/v1-merge-sync.sh <PR_NUMBER> "<SQUASH_SUBJECT>"' >&2
        exit 1
      fi
      bash scripts/v1-merge-sync.sh "$pr_number" "$squash_subject"
      ;;
    5)
      bash scripts/v1-safe-check.sh
      ;;
    6)
      bash scripts/v1-next-pack-context.sh
      ;;
    7)
      exit 0
      ;;
    *)
      echo "Invalid option. Please choose 1-7."
      ;;
  esac

  echo
done
