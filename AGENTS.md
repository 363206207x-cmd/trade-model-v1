# Trade Model V1 Contract-First Agent Rules

Before each task, read these files first:

1. `docs/PROJECT_DELIVERY_CONTRACT.md`
2. `docs/PROJECT_CURRENT_STATE.md`
3. `docs/DELIVERY_PROGRESS_MATRIX.md`
4. `docs/CODEX_TASK_TEMPLATE.md`

Legacy V1 files, including `docs/ACTIVE_MAINLINE_STATUS.yml` and `docs/CODEX_NEXT_TASK.yml`, are historical or derived compatibility evidence only. They cannot define completion, select a business phase, or override the delivery contract and progress matrix.

Rules:

1. Do not redefine project completion from chat history, branch names, open PRs, or review-only slice count.
2. Do not call docs-only, DTO-only, review-only, preview-only, dashboard-only, fallback-only, no-op, or mock-only work DONE for a business module.
3. Do not start P0-1 until P0-0 is DONE in `docs/DELIVERY_PROGRESS_MATRIX.md` and that DONE commit is merged to `main`.
4. Do not auto-open, auto-close, or auto-reverse trades.
5. Do not treat `triggered` as opened.
6. Do not treat `execution_plan` or `tm_real_position` as `user_position`.
7. Do not let AI bypass the rule-layer base direction.
8. Do not treat PushRecheck as trading authorization.
9. Do not delete code without `docs/DEAD_CODE_CANDIDATES.md` evidence.
10. Token leakage is a hard stop.

---

# Trade Model V1 Agent Rules

All development must follow:

1. `docs/PROJECT_DELIVERY_CONTRACT.md`
2. `docs/DELIVERY_PROGRESS_MATRIX.md`
3. `docs/PROJECT_CURRENT_STATE.md`
4. `docs/CODEX_TASK_TEMPLATE.md`
5. `docs/PROJECT_GLOBAL_AUDIT.md` when present

Before any task, read:

1. `docs/PROJECT_DELIVERY_CONTRACT.md`
2. `docs/PROJECT_CURRENT_STATE.md`
3. `docs/DELIVERY_PROGRESS_MATRIX.md`
4. `docs/CODEX_TASK_TEMPLATE.md`
5. `docs/CONTRACT_CHANGE_LOG.md`
6. `AGENTS.md`

Rules:

1. Do not redefine project completion.
2. Do not use chat history as source of truth.
3. Do not skip the current phase.
4. Do not call docs-only, DTO-only, review-only, preview-only, dashboard-only, fallback-only, no-op, mock-only, or placeholder-only work DONE for a business module.
5. P0-0 is a governance phase; it can complete by P0-0 criteria only, and that exception cannot prove any business module DONE.
6. Distinguish Phase Status from Existing Module Maturity.
7. DONE only counts after the DONE commit is merged to `main`.
8. Treat `docs/ACTIVE_MAINLINE_STATUS.yml` and `docs/CODEX_NEXT_TASK.yml` as compatibility files until migrated; they cannot override the delivery contract or matrix.
9. Do not auto-open trades.
10. Do not auto-close trades.
11. Do not auto-reverse positions.
12. Do not treat triggered as opened.
13. Do not treat execution_plan as user_position.
14. Do not treat tm_real_position as user_position.
15. Do not let AI bypass rule-layer base direction.
16. Do not treat PushRecheck as trading authorization.
17. Do not delete code without DEAD_CODE_CANDIDATES.md evidence.
18. After changes, run `./mvnw test -q` unless explicitly impossible.
19. End every task by reporting whether the current phase is DONE.
20. A later business phase cannot start until the current phase is DONE in `docs/DELIVERY_PROGRESS_MATRIX.md` and merged to `main`.

---

# Codex Project Rules

Codex must read these files before each task:

- `docs/SESSION_BOOTSTRAP.md`
- `docs/ACTIVE_MAINLINE_STATUS.yml`
- `docs/V1_PROGRESS_SOURCE_OF_TRUTH.md`
- `docs/V1_CAPABILITY_MATRIX.md`
- `docs/V1_MVP_REALITY_ROADMAP.md`
- `docs/V1_DUPLICATE_SKELETON_FREEZE_RULE.md`
- `docs/ANSWER_FORMAT_CONTRACT.md`
- `docs/WORKFLOW_GITHUB_AUTH_AND_HANDOFF_RULE.md`
- `docs/WORKFLOW_COMMAND_AUTOMATION.md`

## Default Workflow

Default workflow is GPT + Codex + GitHub-native.
（默认工作流是 GPT + Codex + GitHub 原生。）

Terminal scripts are fallback only except local main sync after merge.
（终端脚本除合并后同步 main 外，只作为兜底。）

GitHub connector / Codex GitHub auth / local `gh` responsibility split is governed by `docs/WORKFLOW_GITHUB_AUTH_AND_HANDOFF_RULE.md`.
（GitHub connector / Codex GitHub auth / 本地 `gh` 的责任分工以 `docs/WORKFLOW_GITHUB_AUTH_AND_HANDOFF_RULE.md` 为准。）

Fixed local fallback commands are governed by `docs/WORKFLOW_COMMAND_AUTOMATION.md`.
（固定本地兜底命令以 `docs/WORKFLOW_COMMAND_AUTOMATION.md` 为准。）

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

Current task selection must use `docs/ACTIVE_MAINLINE_STATUS.yml`:

- `active_block` defines the current block.
- `next_required_action` defines the next allowed action.
- If chat memory, branch names, PR state, and docs conflict, merged `main` plus `bash scripts/v1-state.sh` wins.
- If Codex shell reports `GH_NOT_AVAILABLE`, treat Codex GitHub status as unknown, not as project state failure. GPT connector evidence or the user's local terminal `gh` output may be used as handoff evidence for open PR / main sync / clean worktree status.
- Do not open the next package until the current package is merged on `main`, local `main` is synced, and the worktree is clean.
- Prefer fixed workflow commands over handwritten long `gh pr create` / `gh pr merge` commands.

## Duplicate Skeleton Freeze

After #830, Codex must follow `docs/V1_DUPLICATE_SKELETON_FREEZE_RULE.md`.

Do not default to new DTO / Validator / Assembler / Orchestrator / docs-only plan / verification-only packages.

Do not continue P359 or start P360 by default.

Do not connect auto-trading, Push, external channels, Candidate generation, Point generation, order, execution, or executable trade semantics unless a future task explicitly authorizes that scope.

The next allowed track is not hard-coded here. It is whatever merged `main`, `docs/ACTIVE_MAINLINE_STATUS.yml`, and `bash scripts/v1-state.sh` jointly confirm as safe.
