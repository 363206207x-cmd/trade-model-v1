# GitHub-Native Workflow

Default workflow is GPT + Codex + GitHub-native.
（默认工作流是 GPT + Codex + GitHub 原生。）

Terminal scripts are fallback only except local main sync after merge.
（终端脚本除合并后同步 main 外，只作为兜底。）

GitHub auth and handoff priority is defined in `docs/WORKFLOW_GITHUB_AUTH_AND_HANDOFF_RULE.md`.
（GitHub 认证和交接优先级由 `docs/WORKFLOW_GITHUB_AUTH_AND_HANDOFF_RULE.md` 定义。）

## Default Flow

1. GPT decides next pack（GPT 判断下一包）.
2. Codex executes scoped work, checks, commits, and pushes（Codex 执行限定工作、检查、commit、push）.
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

Codex must perform branch / duplicate checks when assigned, run validation, push the branch, and report branch / commit / changed files / checks. Issue / PR creation is performed by GPT connector, local `gh`, or Codex according to `docs/WORKFLOW_GITHUB_AUTH_AND_HANDOFF_RULE.md`.

## Fallback Scripts

Use terminal scripts only when GitHub tools are unavailable, local sync is needed after merge, or a precise local diagnostic is requested.

Fallback commands include:

- `bash scripts/v1-pr-review-input.sh <PR_NUMBER>`
- `bash scripts/v1-status.sh`
- `bash scripts/v1-merge-sync.sh <PR_NUMBER> "<SUBJECT>"`

`bash scripts/v1-auto.sh` and `bash scripts/v1.sh` remain fallback helpers. They are not the default workflow.
