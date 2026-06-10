# Session Bootstrap

Use this file first in every new window.

Default workflow is GPT + Codex + GitHub-native.
（默认工作流是 GPT + Codex + GitHub 原生。）

Terminal scripts are fallback only except local main sync after merge.
（终端脚本除合并后同步 main 外，只作为兜底。）

GitHub auth and GPT / Codex / local `gh` handoff must follow `docs/WORKFLOW_GITHUB_AUTH_AND_HANDOFF_RULE.md`.
（GitHub 认证与 GPT / Codex / 本地 `gh` 交接必须遵守 `docs/WORKFLOW_GITHUB_AUTH_AND_HANDOFF_RULE.md`。）

Fixed local fallback commands must follow `docs/WORKFLOW_COMMAND_AUTOMATION.md`.
（固定本地兜底命令必须遵守 `docs/WORKFLOW_COMMAND_AUTOMATION.md`。）

After #830, duplicate skeleton packages must follow `docs/V1_DUPLICATE_SKELETON_FREEZE_RULE.md`.
（#830 之后，重复骨架包必须遵守 `docs/V1_DUPLICATE_SKELETON_FREEZE_RULE.md`。）

Codex must output the result requested by the current package and stop; PR number is required only when PR creation is assigned to Codex.
（Codex 必须输出当前包要求的结果并停止；只有 PR 创建分配给 Codex 时才必须输出 PR 编号。）

Fallback bootstrap command:

```bash
bash scripts/v1-session-bootstrap.sh
```

Preferred user-facing operator command:

```bash
bash scripts/v1-auto.sh next
```

`v1-auto.sh` is a Chinese workflow operator. It summarizes state, progress, blockers, and the next Codex task while still delegating to the fixed workflow scripts.

One-command Codex runner:

```bash
bash scripts/v1-codex-run-next.sh
```

This starts from clean/synced `main`, generates the next Codex task through `v1-auto.sh next`, and tries to launch Codex CLI. It does not stage, commit, push, create PRs, or merge.

If Codex shell cannot confirm Open PR because local `gh` is unavailable, but GPT connector or the user's terminal has already confirmed Open PR none, the allowed handoff form is:

```bash
bash scripts/v1-codex-run-next.sh --open-pr-none-confirmed
```

This only bypasses Codex GitHub status unknown. It does not bypass non-main branch, dirty worktree, explicit open PR, failed Main Sync, or other blockers.

One-command PR completion helper:

```bash
bash scripts/v1-pr-complete.sh <PR_NUMBER> A "<SUBJECT>"
bash scripts/v1-pr-complete.sh <PR_NUMBER> B "<SUBJECT>" --confirm-reviewed
```

This helper always checks through `v1-auto.sh check-pr` and merges only through `v1-merge-sync.sh`.

1. Read `docs/ACTIVE_MAINLINE_STATUS.yml`.
2. Read `docs/V1_CAPABILITY_MATRIX.md`.
3. Read `docs/V1_PROGRESS_SOURCE_OF_TRUTH.md`.
4. Read `docs/V1_DUPLICATE_SKELETON_FREEZE_RULE.md`.
5. Read `docs/WORKFLOW_GITHUB_AUTH_AND_HANDOFF_RULE.md`.
6. Read `docs/WORKFLOW_COMMAND_AUTOMATION.md`.
7. Run `git branch --show-current`, `git status --short`, and `git log --oneline -5`.
8. Never use chat memory as progress.
9. Reply using `docs/ANSWER_FORMAT_CONTRACT.md`.
10. Do not continue to next package unless current PR is merged, main is synced, and worktree is clean.
11. Open PR / branch / Issue does not count as done.
12. Do not continue P359 or start P360 by default.
13. Do not default back to a historical track. The current active block comes from `docs/ACTIVE_MAINLINE_STATUS.yml`.
14. Continue only when `bash scripts/v1-state.sh` or accepted handoff evidence confirms clean/synced `main`, no open PR, and no blockers.

If Codex shell prints `OPEN_PRS: GH_NOT_AVAILABLE`, treat it as Codex GitHub status unknown. It is not, by itself, proof that the project has an open PR or an unsynced main. GPT connector evidence or the user's local terminal `gh` output may be accepted as handoff evidence when it explicitly confirms open PR none, main sync, and clean worktree.

For one-command execution, use `bash scripts/v1-codex-run-next.sh --open-pr-none-confirmed` only after that handoff evidence exists.

## Workflow Command Shortcuts

- Default workflow: GPT decides the next pack, Codex executes scoped file changes / checks / commit / push, and PR creation / merge follows `docs/WORKFLOW_GITHUB_AUTH_AND_HANDOFF_RULE.md`.
- Fallback new window command: `bash scripts/v1-session-bootstrap.sh`
- Fixed status check: `bash scripts/v1-state.sh`
- Chinese operator entry: `bash scripts/v1-auto.sh next`
- One-command Codex runner: `bash scripts/v1-codex-run-next.sh`
- One-command Codex runner with explicit Open PR none handoff: `bash scripts/v1-codex-run-next.sh --open-pr-none-confirmed`
- PR completion helper: `bash scripts/v1-pr-complete.sh <PR_NUMBER> <A|B|C> "<SUBJECT>" [--confirm-reviewed]`
- Fixed PR creation: `bash scripts/v1-open-pr.sh <branch> "<title>" <risk> [--body-file <file>] [--draft|--ready]`
- Fallback PR review input: `bash scripts/v1-pr-review-input.sh <PR_NUMBER>`
- Local merge sync after approval: `bash scripts/v1-merge-sync.sh <PR_NUMBER> "<SUBJECT>" --risk <risk> [--confirm]`
- Fallback Codex completion safe check: `bash scripts/v1-safe-check.sh`

Token leakage remains a hard stop: never paste or repeat GitHub tokens in chat or logs.
