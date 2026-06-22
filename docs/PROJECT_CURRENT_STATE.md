# Trade Model V1 Current State

Contract: docs/PROJECT_DELIVERY_CONTRACT.md
Contract Version: v1.0
Current Phase: P0-0 Contract Lock + Baseline + Dead Code Candidate Report
Current Phase Status: DONE
Completion Effective State: derived by v1 state runtime
Existing Module Maturity: PARTIAL
Current Work Package: P0-6 Review integrates UserPosition DONE candidate
Next Business Phase: P1-1 PushRecheck semantic hardening
Next Business Phase Allowed: NO
Production Deployment Readiness: BLOCKED

---

## Effective State Rule

P0-0 is effective only because its DONE commit is merged to `main`, local `main` is synced, and the worktree was clean when the runtime gate evaluated it.

P0-1 UserPosition is effective because its implementation is merged to clean / synced `main`.

P0-2 ExecutionPlan Source Gate is effective because its implementation is merged to clean / synced `main` and the runtime gate allowed P0-3.

P0-3 AccountRisk integrates UserPosition is effective because its implementation is merged to clean / synced `main` and the runtime gate allowed P0-4.

P0-4 PositionMonitorLog is effective because its implementation is merged to clean / synced `main` and the runtime gate allowed P0-5.

P0-5 PositionMonitorService is effective because its implementation is merged to clean / synced `main` and the runtime gate allowed P0-6.

P0-6 Review integrates UserPosition is only a branch DONE candidate in this worktree.
It is not effective until this branch commit is reviewed, merged to `main`, local `main` is synced, the worktree is clean, and `bash scripts/v1-state.sh` confirms P0-6 effectivity.

`bash scripts/v1-state.sh` must distinguish `CURRENT_PACKAGE_PR`, `UNRELATED_OPEN_PRS`, and `BLOCK_NEXT_BUSINESS_PHASE_ONLY`. An unrelated Draft PR must not block merging the current P0-0 package PR, but it still blocks the next business phase.

---

## Current Allowed Work

Only the following work is allowed after this P0-6 branch-candidate update:

1. Checks, push, PR creation, and merge-gate handling for the P0-6 Review integrates UserPosition B-risk package.
2. Main sync after the P0-6 PR is reviewed and merged.
3. Runtime verification that P0-6 is effective on clean / synced main before any P1-1 work starts.

P0-6 is a DONE candidate on the task branch only. It is not effective until the branch commit is merged to `main`, local `main` is synced, and the worktree is clean.

PR #1004 was an unrelated Draft dashboard PR and no code from it is merged into this package.

---

## Current Forbidden Work

The following work is blocked until P0-6 is effective on merged main:

1. PushRecheck semantic hardening beyond the read-only P0-3 risk consumption path.
3. ConfusedState + AiConflict hardening.
4. HotReset real action.
5. OpportunityLog.
6. Macro / News / External Context.
7. AI Orchestrator + AiCallLog.
8. Scheduler / Idempotency / Trace.
9. Dashboard Final.
10. Auto-trading of any kind.

---

## Current Known Critical Gaps

1. P0-6 Review integrates UserPosition is only a branch DONE candidate until merged main confirms it effective.
2. PushRecheck semantic hardening remains incomplete beyond the read-only P0-3 risk consumption path.
3. P0-6 branch candidate must be merged main before P1-1 work is allowed.
4. HotReset real action incomplete.
5. OpportunityLog incomplete.
6. Macro / News runtime not complete.
7. AI orchestrator and ai call log incomplete.
8. Dashboard Final must wait until business semantics are stable.

## P0-6 Review Integrates UserPosition Branch Candidate

Branch: `p0-6-review-user-position-integration`
Risk: B
Status: DONE candidate
Effective State: pending merged main

Implemented branch evidence:

1. `UserPositionReviewAdapter` and `DefaultUserPositionReviewAdapter` define the P0-6 owner.
2. `GET /api/review/user-positions/{positionId}/summary` generates ReviewSummary only for CLOSED UserPosition records.
3. `POST /api/review/user-positions/{positionId}/feedback` records manual rule feedback through existing ReviewService and rule-version audit owners.
4. Review reads linked ExecutionPlan by exact planId / analysisId, or returns PLAN_CONTEXT_MISSING without fabricating plan context.
5. Review reads real entry, close, stop-loss, take-profit, quantity, leverage, openedAt, closedAt, and all PositionMonitorLog entries.
6. Review calculates gross win / loss / breakeven, execution deviation, plan invalidation, warning timeliness, and ignored warning evidence.
7. DTO output carries fixed safety fields and exposes no open / close / reduce / reverse / order / execution / auto-trading / rule-apply payload fields.
8. Tests cover win / loss / breakeven, user deviation, plan invalidation, ignored warning, feedback persistence, controller endpoints, safety fields, and no business mutation.
9. No UserPosition mutation, ExecutionPlan mutation, PositionMonitor execution, automatic rule application, automatic open / close / reverse, order execution, auto-trading, scheduler, Push send, Dashboard UI, P1-1, or PR #1004 changes are part of this package.

P1-1 remains blocked until this branch is reviewed, merged to `main`, main is synced, and runtime state confirms P0-6 effective.

---

## Current Deployment Readiness

Production deployment remains BLOCKED.

Blocking evidence:

- `src/main/resources/application.yml` uses `jdbc:h2:mem:trade_model_v1`.
- `src/main/resources/application.yml` has empty datasource password.
- `src/main/resources/application.yml` and `src/main/resources/application.properties` enable H2 console.
- `src/main/resources/application.properties` defaults `position.provider.type` to `SIMULATED`.
- No production profile, migration/rollback pipeline, auth/authz evidence, or deployment smoke/rollback evidence was found in the P0-0 audit pass.

---

## Derived / Compatibility Sources

`docs/ACTIVE_MAINLINE_STATUS.yml` and `docs/CODEX_NEXT_TASK.yml` are derived compatibility files only.
They do not override the Delivery Contract, Delivery Progress Matrix, or this Current State file.

Legacy V1 documents remain historical audit and asset evidence only.
Review-only slice count is no longer a delivery completion standard.

---

## Rule

No later business phase may start until the current branch candidate is merged to main and `Next Business Phase Allowed` becomes YES through the contract gate.
