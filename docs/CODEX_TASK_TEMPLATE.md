# Codex Task Template

Every Codex task must start by reading:

1. docs/PROJECT_DELIVERY_CONTRACT.md
2. docs/PROJECT_CURRENT_STATE.md
3. docs/DELIVERY_PROGRESS_MATRIX.md
4. AGENTS.md

Chat history is not the source of truth.
The repository contract is the source of truth.

---

## Before Coding, Answer

Before making changes, answer:

1. Current branch:
2. Current phase:
3. Is this task allowed in current phase:
4. Previous phase DONE:
5. Files expected to change:
6. Tests expected to run:
7. Safety boundaries:
8. Stop conditions:

---

## Stop Conditions

Stop immediately if:

1. Worktree is not clean.
2. Maven tests fail before changes.
3. Task is outside current phase.
4. Task requires changing the contract without explicit approval.
5. Task may create auto-trading behavior.
6. Task treats execution_plan as user_position.
7. Task treats triggered as opened.
8. Task treats tm_real_position as user_position.
9. Task marks docs-only / DTO-only / review-only as DONE.

---

## End-of-task Report

Every task must end with:

1. Current branch
2. Changed files
3. Added files
4. Deleted files
5. Tests run
6. Maven result
7. Contract compliance
8. Whether current phase is DONE
9. Evidence for DONE
10. Whether next phase is allowed
11. Next allowed task
12. Whether files were staged
13. Whether files were committed

## Final Output Template / 最终输出模板

All future Codex tasks must use this final output template:

```text
WHAT_THIS_STEP_DOES（这一步在做什么）:
CURRENT_PROGRESS（当前进度）:
CURRENT_PHASE（当前阶段）:
CURRENT_BLOCK（当前模块）:
CURRENT_BRANCH（当前分支）:
CURRENT_PR（当前 PR）:
MERGED_MAIN_STATUS（合并主线状态）:
EFFECTIVE_STATUS（生效状态）:
NEXT_ALLOWED_ACTION（下一允许动作）:
NEXT_BLOCKED_ACTION（下一禁止动作）:
WHY_BLOCKED_OR_ALLOWED（为什么允许或阻塞）:
FILES_CHANGED（变更文件）:
CHECKS（检查）:
RISK_LEVEL（风险等级）:
OVERREACH_STATUS（越界状态）:
```

The output must begin with `WHAT_THIS_STEP_DOES（这一步在做什么）` and `CURRENT_PROGRESS（当前进度）`.
English technical terms must include Chinese explanations, for example merged main（已合并主线）, clean/synced main（干净且已同步主线）, open PR（未合并 PR）, Draft PR（草稿 PR）, A-risk（低风险）, B-risk（中风险）, effective（已生效）, blocked（阻塞）, allowed（允许）, worktree（工作区）, gate（门禁）, UserPosition（用户手动持仓）, ExecutionPlan（执行计划）, Source Gate（来源门禁）, AccountRisk（账户风险）, PositionMonitor（持仓监控）, and Review（复盘）.

`NEXT_ALLOWED_ACTION（下一允许动作）` must only show the contract-allowed next step.
`NEXT_BLOCKED_ACTION（下一禁止动作）` must say what must not be started.
Open PR（未合并 PR） and PENDING_MERGED_MAIN（等待合并主线） do not count as complete; merged main（已合并主线） is required before effective（已生效） status.

---

## Required Language for Phase Completion

If the phase is not complete, say:

Current phase is NOT DONE. Next phase is NOT allowed.

If the phase is complete, say:

Current phase is DONE according to docs/PROJECT_DELIVERY_CONTRACT.md. Next phase is allowed.


---

## P0-0 Reconciliation Addendum

Every task must also read:

5. docs/PROJECT_GLOBAL_AUDIT.md when it exists.
6. docs/CONTRACT_CHANGE_LOG.md.

Before coding or editing, answer both axes:

1. Phase Status:
2. Existing Module Maturity:
3. Is the requested work a governance task or business module task:
4. Is the previous phase DONE on merged main:
5. Is the task blocked by Production Deployment Readiness:

Compatibility files such as docs/ACTIVE_MAINLINE_STATUS.yml and docs/CODEX_NEXT_TASK.yml must be treated as derived files until migrated. They cannot override docs/PROJECT_DELIVERY_CONTRACT.md or docs/DELIVERY_PROGRESS_MATRIX.md.

If the current phase is P0-0 and the task is governance-only, business-module Done Criteria do not apply. If the task is a business module, docs-only / DTO-only / review-only / dashboard-only work remains insufficient for DONE.


---

## P0-0 Closure Candidate Note

A local branch DONE candidate is not effective project completion.
The next business phase remains blocked until the DONE candidate is merged to `main`, local `main` is synced, and the worktree is clean.

## A-risk Auto Merge Note

For P0-0 docs / contract / workflow packages only, Codex may complete the current package PR through the A-risk Auto Merge Rule when changed files are limited to docs / workflow scripts / contract files, Maven / workflow / task validation and PR checks passed, the PR is not Draft, and the target PR is the current package PR.

Unrelated Draft PRs do not block current package merge. They still block the next business phase. PR #1004 is unrelated to the P0-0 package and must not be modified, merged, closed, reviewed, or targeted by this flow.
