# Answer Format Contract

Every status, progress, task-handoff, and PR-review answer must use the fields below.

Default workflow is GPT + Codex + GitHub-native.
（默认工作流是 GPT + Codex + GitHub 原生。）

Terminal scripts are fallback only except local main sync after merge.
（终端脚本除合并后同步 main 外，只作为兜底。）

GitHub auth and GPT / Codex / local `gh` handoff must follow `docs/WORKFLOW_GITHUB_AUTH_AND_HANDOFF_RULE.md`.
（GitHub 认证与 GPT / Codex / 本地 `gh` 交接必须遵守 `docs/WORKFLOW_GITHUB_AUTH_AND_HANDOFF_RULE.md`。）

Use both English and Chinese labels. Do not answer only in English or only in Chinese.

```text
Current Mainline（当前主线）:
Current Block（当前模块）:
Current Level（当前层级）:
Current Phase Status（当前阶段状态）:
Existing Module Maturity（现有模块成熟度）:
Production Deployment Readiness（生产部署就绪度）:
Contract Sync（契约同步状态）:
Done Criteria（完成标准）:
Current PR（当前 PR）:
Can Merge?（能否合并）:
Next Step（下一步）:
Remaining Steps（剩余步骤）:
Do Not Do（禁止事项）:
```

## Rules

- `Current PR（当前 PR）` must say open / merged / none.
- `Can Merge?（能否合并）` must mention risk level and approval rule.
- For A-risk Auto Merge Rule（A-risk 自动合并规则） answers, include whether the target PR is the current package PR, whether unrelated Draft PRs only block the next business phase, and whether PR #1004 was untouched.
- `Next Step（下一步）` must not skip an open required PR, unsynced main, dirty worktree, Draft PR, failed CI, or unresolved merge conflict.
- `Do Not Do（禁止事项）` must include no auto-trading when trading paths are discussed.
- Open PR / branch / Issue does not count as done.
- Merged `main` is the only completed state.
- When current package is merged, main is synced, worktree is clean, CI passed, and no blocker exists, do not ask whether to continue; generate the next maximum safe Codex task prompt.


## Contract-First Fields

- `Current Phase Status（当前阶段状态）` must come from `docs/DELIVERY_PROGRESS_MATRIX.md`.
- `Existing Module Maturity（现有模块成熟度）` must come from `docs/DELIVERY_PROGRESS_MATRIX.md`.
- `Production Deployment Readiness（生产部署就绪度）` must come from `docs/PROJECT_CURRENT_STATE.md` / compatibility mirror.
- `Contract Sync（契约同步状态）` must be based on agreement between contract, matrix, current state, and derived compatibility files.

Legacy review-only slice count must not be used as project delivery completion.
