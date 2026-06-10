#!/usr/bin/env bash

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

TASK_FILE="docs/CODEX_NEXT_TASK.yml"

usage() {
  cat <<'EOF'
用法:
  bash scripts/v1-package-dirty-work.sh

说明:
  当 Codex 已经写了文件但未成功创建分支 / commit / PR 时，用当前 CODEX_NEXT_TASK.yml
  中的 branch / risk / active_block 安全打包 dirty worktree（脏工作区）。

安全边界:
  A-risk 只自动 stage docs/ 和 scripts/。
  B-risk 只自动 stage 当前最小实现允许路径。
  C-risk 不自动 stage，直接 STOP。
EOF
}

if [[ "${1:-}" == "--help" || "${1:-}" == "-h" ]]; then
  usage
  exit 0
fi

yaml_value() {
  local key="$1"
  awk -v key="$key" '
    $0 ~ "^" key ":" {
      value=$0
      sub("^[^:]*:[[:space:]]*", "", value)
      gsub(/^"/, "", value)
      gsub(/"$/, "", value)
      print value
      exit
    }
  ' "$TASK_FILE"
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

is_forbidden_staged_path() {
  local file="$1"
  case "$file" in
    pom.xml|config/*|src/main/resources/schema.sql|src/main/resources/db/*|src/main/resources/*schema*|src/main/resources/application*.yml|src/main/resources/application*.yaml|src/main/resources/application*.properties|*DTO*.java|*Validator*.java|*Assembler*.java)
      return 0
      ;;
    *)
      return 1
      ;;
  esac
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
      printf 'docs(workflow): package %s' "$branch"
      ;;
    B)
      printf 'feat(workflow): package %s' "$branch"
      ;;
    *)
      return 1
      ;;
  esac
}

if [[ ! -f "$TASK_FILE" ]]; then
  stop "找不到 $TASK_FILE，无法读取下一任务配置。"
fi

target_branch="$(yaml_value branch)"
risk="$(yaml_value risk)"
active_block="$(yaml_value active_block)"
allowed_changes="$(yaml_value allowed_changes)"

[[ -n "$target_branch" ]] || stop "CODEX_NEXT_TASK.yml 缺少 branch。"
[[ -n "$risk" ]] || stop "CODEX_NEXT_TASK.yml 缺少 risk。"
[[ -n "$active_block" ]] || stop "CODEX_NEXT_TASK.yml 缺少 active_block。"

case "$risk" in
  A|B)
    ;;
  C|B/C)
    stop "$risk-risk（高风险）不自动打包，请人工复核。"
    ;;
  *)
    stop "不支持的 risk（风险等级）: $risk"
    ;;
esac

state_text="$(bash scripts/v1-state.sh 2>&1 || true)"
current_branch="$(git branch --show-current)"
open_prs="$(state_value "$state_text" "OPEN_PRS")"
main_sync="$(state_value "$state_text" "MAIN_SYNC")"

echo "V1 Dirty Work Packager（脏工作区安全打包）"
print_hr
echo "$state_text"
echo
echo "目标分支: $target_branch"
echo "Risk（风险等级）: $risk"
echo "Active block（当前任务）: $active_block"
echo "Allowed changes（允许改动）: $allowed_changes"

if [[ "$open_prs" != "none" ]]; then
  if [[ "$open_prs" == "GH_NOT_AVAILABLE" ]]; then
    stop "Open PR（未合并 PR）状态未知。可以让 GPT connector 确认并创建 PR；本脚本不会在状态未知时自动打包。"
  fi
  stop "存在 Open PR（未合并 PR）: $open_prs"
fi

if [[ "$current_branch" != "main" && "$current_branch" != "$target_branch" ]]; then
  stop "当前分支既不是 main，也不是目标任务分支。当前分支: $current_branch"
fi

if [[ "$current_branch" == "main" && "$main_sync" != "OK" ]]; then
  stop "当前在 main，但 Main Sync（主分支同步）不是 OK: ${main_sync:-UNKNOWN}"
fi

staged_now="$(git diff --cached --name-only)"
if [[ -n "$staged_now" ]]; then
  while IFS= read -r file; do
    [[ -z "$file" ]] && continue
    if is_forbidden_staged_path "$file"; then
      stop "已 stage 禁止路径: $file"
    fi
  done <<<"$staged_now"
fi

if [[ "$current_branch" != "$target_branch" ]]; then
  if git show-ref --verify --quiet "refs/heads/$target_branch"; then
    echo "切换到已有目标分支，保留当前 dirty worktree（脏工作区）。"
    git switch "$target_branch"
  else
    echo "创建目标分支，保留当前 dirty worktree（脏工作区）。"
    git switch -c "$target_branch"
  fi
fi

changed_files="$(git status --porcelain=v1 | sed -E 's/^.{3}//')"
[[ -n "$changed_files" ]] || stop "没有 dirty worktree（脏工作区）可打包。"

allowed_files=()
violations=()
while IFS= read -r file; do
  [[ -z "$file" ]] && continue
  case "$risk" in
    A)
      if is_a_risk_allowed_file "$file"; then
        allowed_files+=("$file")
      else
        violations+=("$file")
      fi
      ;;
    B)
      if is_b_risk_allowed_file "$file" && ! is_forbidden_staged_path "$file"; then
        allowed_files+=("$file")
      else
        violations+=("$file")
      fi
      ;;
  esac
done <<<"$changed_files"

if [[ "${#violations[@]}" -gt 0 ]]; then
  echo "违规文件:"
  printf '%s\n' "${violations[@]}" | sed 's/^/- /'
  stop "dirty worktree（脏工作区）包含当前 risk 不允许自动 stage 的路径。"
fi

if [[ "${#allowed_files[@]}" -eq 0 ]]; then
  stop "没有可安全 stage 的文件。"
fi

git add -- "${allowed_files[@]}"

echo
echo "已 stage 文件:"
git diff --cached --name-only | sed 's/^/- /'

bash scripts/check-workflow-contract.sh
git diff --cached --check

while IFS= read -r file; do
  [[ -z "$file" ]] && continue
  if is_forbidden_staged_path "$file"; then
    stop "forbidden path check（禁止路径检查）失败: $file"
  fi
done < <(git diff --cached --name-only)

subject="$(subject_from_task "$risk" "$target_branch" "$active_block")" || stop "无法从 CODEX_NEXT_TASK.yml 安全生成 commit subject，请用户手动提供。"

git commit -m "$subject"
commit_sha="$(git rev-parse --short HEAD)"
git push -u origin "$target_branch"

echo
echo "Commit（提交）完成: $commit_sha"
echo "准备创建 Pull Request（拉取请求）。"

set +e
pr_output="$(bash scripts/v1-auto.sh pr "$target_branch" "$subject" "$risk" 2>&1)"
pr_status=$?
set -e

if [[ "$pr_status" -ne 0 ]]; then
  echo "$pr_output"
  echo "STOP（停止）: Pull Request（拉取请求）创建失败。"
  echo "branch: $target_branch"
  echo "subject: $subject"
  echo "risk: $risk"
  echo "如果本机 gh 失效，可以让 GPT connector 创建 PR。"
  exit 1
fi

echo "$pr_output"
echo
echo "DIRTY_WORK_PACKAGE_DONE（脏工作区打包完成）"
echo "Branch（分支）: $target_branch"
echo "Risk（风险等级）: $risk"
echo "Commit（提交）: $commit_sha"
