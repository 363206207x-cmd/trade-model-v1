# GitHub-Native Workflow

Default workflow is GPT + Codex + GitHub-native.
（默认工作流是 GPT + Codex + GitHub 原生。）

Terminal scripts are fallback only except local main sync after merge.
（终端脚本除合并后同步 main 外，只作为兜底。）

Codex must output PR number and stop.
（Codex 必须输出 PR 编号并停止。）

## Default Flow

1. GPT decides next pack（GPT 判断下一包）.
2. Codex executes and creates Draft PR（Codex 执行并创建 Draft PR）.
3. GPT reviews GitHub PR（GPT 审 GitHub PR）.
4. User approves B/C or C merge（用户确认 B/C 或 C 档合并）.
5. Terminal sync only after merge（终端只在合并后同步）.

## What Changes

Users should no longer be asked by default to:

- run menu scripts;
- find PR numbers;
- decide mergeability;
- inspect CI status;
- judge whether stale PRs or Issues are current work.

Codex must perform Issue / PR / branch duplicate checks, run validation, push the branch, create the Draft PR, and report the PR number.

## Fallback Scripts

Use terminal scripts only when GitHub tools are unavailable, local sync is needed after merge, or a precise local diagnostic is requested.

Fallback commands include:

- `bash scripts/v1-pr-review-input.sh <PR_NUMBER>`
- `bash scripts/v1-status.sh`
- `bash scripts/v1-merge-sync.sh <PR_NUMBER> "<SUBJECT>"`

`bash scripts/v1-auto.sh` and `bash scripts/v1.sh` remain fallback helpers. They are not the default workflow.
