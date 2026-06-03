# Session Bootstrap

Use this file first in every new window.

Default workflow is GPT + Codex + GitHub-native.
（默认工作流是 GPT + Codex + GitHub 原生。）

Terminal scripts are fallback only except local main sync after merge.
（终端脚本除合并后同步 main 外，只作为兜底。）

GitHub auth and GPT / Codex / local `gh` handoff must follow `docs/WORKFLOW_GITHUB_AUTH_AND_HANDOFF_RULE.md`.
（GitHub 认证与 GPT / Codex / 本地 `gh` 交接必须遵守 `docs/WORKFLOW_GITHUB_AUTH_AND_HANDOFF_RULE.md`。）

Codex must output the result requested by the current package and stop; PR number is required only when PR creation is assigned to Codex.
（Codex 必须输出当前包要求的结果并停止；只有 PR 创建分配给 Codex 时才必须输出 PR 编号。）

Fallback bootstrap command:

```bash
bash scripts/v1-session-bootstrap.sh
```

1. Read `docs/ACTIVE_MAINLINE_STATUS.yml`.
2. Read `docs/V1_CAPABILITY_MATRIX.md`.
3. Read `docs/V1_PROGRESS_SOURCE_OF_TRUTH.md`.
4. Read `docs/WORKFLOW_GITHUB_AUTH_AND_HANDOFF_RULE.md`.
5. Run `git branch --show-current`, `git status --short`, and `git log --oneline -5`.
6. Never use chat memory as progress.
7. Reply using `docs/ANSWER_FORMAT_CONTRACT.md`.
8. Do not continue to next package unless current PR is merged, main is synced, and worktree is clean.
9. Open PR / branch / Issue does not count as done.

## Workflow Command Shortcuts

- Default workflow: GPT decides the next pack, Codex executes scoped file changes / checks / commit / push, and PR creation / merge follows `docs/WORKFLOW_GITHUB_AUTH_AND_HANDOFF_RULE.md`.
- Fallback new window command: `bash scripts/v1-session-bootstrap.sh`
- Fallback status check: `bash scripts/v1-status.sh`
- Fallback PR review input: `bash scripts/v1-pr-review-input.sh <PR_NUMBER>`
- Local merge sync after explicit approval: `bash scripts/v1-merge-sync.sh <PR_NUMBER> "<SUBJECT>"`
- Fallback Codex completion safe check: `bash scripts/v1-safe-check.sh`
