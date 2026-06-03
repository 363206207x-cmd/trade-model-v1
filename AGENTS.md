# Codex Project Rules

Codex must read these files before each task:

- `docs/SESSION_BOOTSTRAP.md`
- `docs/ACTIVE_MAINLINE_STATUS.yml`
- `docs/V1_PROGRESS_SOURCE_OF_TRUTH.md`
- `docs/V1_CAPABILITY_MATRIX.md`
- `docs/V1_MVP_REALITY_ROADMAP.md`
- `docs/ANSWER_FORMAT_CONTRACT.md`
- `docs/WORKFLOW_GITHUB_AUTH_AND_HANDOFF_RULE.md`

## Default Workflow

Default workflow is GPT + Codex + GitHub-native.
（默认工作流是 GPT + Codex + GitHub 原生。）

Terminal scripts are fallback only except local main sync after merge.
（终端脚本除合并后同步 main 外，只作为兜底。）

GitHub connector / Codex GitHub auth / local `gh` responsibility split is governed by `docs/WORKFLOW_GITHUB_AUTH_AND_HANDOFF_RULE.md`.
（GitHub connector / Codex GitHub auth / 本地 `gh` 的责任分工以 `docs/WORKFLOW_GITHUB_AUTH_AND_HANDOFF_RULE.md` 为准。）

Codex must self-contain each task:

1. Check for existing Issue / PR / branch.
2. Create the Issue when missing.
3. Create the branch when missing.
4. Execute the scoped task.
5. Run required validation.
6. Push the branch.
7. Create a Draft PR only when the task and auth handoff rule assign PR creation to Codex.
8. Otherwise stop after push and report branch / commit / changed files / checks.

Open PR / branch / Issue does not count as done. Codex output does not count as done. Only merged `main` counts as done.

Every task must declare:

- Current Mainline（当前主线）
- Current Block（当前模块）
- Capability Movement（能力层级变化）
- User-visible Output（用户可见输出）
- Overreach Boundary（越界边界）

Do not continue into the next business package until the current package is merged on `main`.
