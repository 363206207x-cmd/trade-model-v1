# Trade Model V1 Product-First Agent Rules

Before any audit, design, coding, test, repair, PR, merge-gate, or deployment task, Codex must complete this order before editing:

1. confirm repository identity and current worktree;
2. run `bash scripts/product-source-gate.sh`;
3. read `docs/PRODUCT_SOURCE_OF_TRUTH.md` and every task-specific registered product source;
4. output Product Contract Mapping;
5. output Design / Interaction Mapping;
6. output Data Source Mapping;
7. state the Current Implementation Gap;
8. state allowed scope, blocked scope, hard boundaries, real-scenario requirement, and stop conditions;
9. edit only after `PRODUCT_SOURCE_GATE_STATUS: PASS`;
10. validate and finish with a Product Alignment Report.

Permanent authority rules:

1. Formal product plans are higher product authority than current code, current UI, Governance, Workflow, and tests.
2. Do not reverse-engineer product requirements from the current implementation.
3. Do not invent a module, field, identity, state, interaction, route, weakening copy, or completion claim.
4. When a required source or mapping is missing, or formal sources have a real unresolved conflict, stop instead of guessing.
5. Governance, Workflow, and tests remain delivery controls; none can override `docs/PRODUCT_SOURCE_OF_TRUTH.md`.
6. The Product Source Gate must remain deterministic and minimal. Do not extend it into a natural-language parser, synonym inventory, digest system, or independent governance roadmap.
7. A read-only product-gap audit may proceed only when `task_mode=READ_ONLY_PRODUCT_AUDIT`, Product Source Gate passes, P0 is effective on clean/synced merged main, the worktree is clean, editable scope is locked out, and no current or active conflicting open PR exists. Closed unmerged technical debt is not a blocker and is not effective/current content. This never authorizes implementation, Ready, merge, deployment, reopening debt, or using recovery content as current implementation.

The permanent hard boundaries remain: no automatic open/close/add/reduce/reverse/order/trade; no Push Recheck trading authorization; no fake data as real; no owner-scope bypass; no public/private leakage; no ExecutionPlan-as-UserPosition; no `triggered`-as-opened.

---

# Trade Model V1 Contract-First Delivery Rules

After the Product First gate and mappings above, read these delivery-control files:

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

After the Product First gate and mappings above, also read:

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

After product-source reading and mapping, Codex must also read these delivery and compatibility files:

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

Product-package selection must follow `docs/PRODUCT_SOURCE_OF_TRUTH.md` and
`docs/PRODUCT_ROADMAP_V2.md`, then use the delivery contract, progress matrix,
current state, merged `main`, and `bash scripts/v1-state.sh` for delivery and
runtime facts. `docs/ACTIVE_MAINLINE_STATUS.yml` is a derived compatibility
mirror: its `active_block` and `next_required_action` cannot define product
direction or override the Product First roadmap.

- If chat memory, branch names, PR state, and docs conflict, the registered product sources decide product meaning; merged `main`, the delivery matrix/current state, and `bash scripts/v1-state.sh` decide implementation effectivity.
- If Codex shell reports `GH_NOT_AVAILABLE`, treat Codex GitHub status as unknown, not as project state failure. GPT connector evidence or the user's local terminal `gh` output may be used as handoff evidence for open PR / main sync / clean worktree status.
- Do not open the next package until the current package is merged on `main`, local `main` is synced, and the worktree is clean.
- Every active non-current open PR is conflicting for the next product package. Closed unmerged technical debt is not an active blocker, is not effective/current content, and must not be used as implementation evidence.
- Prefer fixed workflow commands over handwritten long `gh pr create` / `gh pr merge` commands.

## Duplicate Skeleton Freeze

After #830, Codex must follow `docs/V1_DUPLICATE_SKELETON_FREEZE_RULE.md`.

Do not default to new DTO / Validator / Assembler / Orchestrator / docs-only plan / verification-only packages.

Do not continue P359 or start P360 by default.

Do not connect auto-trading, Push, external channels, Candidate generation, Point generation, order, execution, or executable trade semantics unless a future task explicitly authorizes that scope.

The next allowed track is the next Product First package authorized by the Product Source of Truth and Product Roadmap, after merged `main`, the delivery matrix/current state, and `bash scripts/v1-state.sh` confirm it is safe to start.
