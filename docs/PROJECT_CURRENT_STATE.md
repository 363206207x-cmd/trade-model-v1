# Trade Model V1 Current State

Contract: docs/PROJECT_DELIVERY_CONTRACT.md
Contract Version: v1.0
Current Phase: P0-0 Contract Lock + Baseline + Dead Code Candidate Report
Current Phase Status: DONE
Completion Effective State: derived by v1 state runtime
Existing Module Maturity: PARTIAL
Current Work Package: P0-1 UserPosition Manual Workflow DONE candidate
Next Business Phase: P0-2 ExecutionPlan Source Gate
Next Business Phase Allowed: NO
Production Deployment Readiness: BLOCKED

---

## Effective State Rule

P0-0 is only a local branch DONE candidate in this worktree.
It is not effective project completion until the commit containing this state is merged to `main`, local `main` is synced, and the worktree is clean.

P0-1 UserPosition remains blocked until `bash scripts/v1-state.sh` reports `COMPLETION_EFFECTIVE_STATE: EFFECTIVE_MERGED_MAIN` and the next-business-phase gate allows it.

`bash scripts/v1-state.sh` must distinguish `CURRENT_PACKAGE_PR`, `UNRELATED_OPEN_PRS`, and `BLOCK_NEXT_BUSINESS_PHASE_ONLY`. An unrelated Draft PR must not block merging the current P0-0 package PR, but it still blocks the next business phase.

---

## Current Allowed Work

Only the following work is allowed after this P0-1 branch-candidate update:

1. Review, checks, push, PR creation, and merge-gate handling for the P0-1 UserPosition B-risk package.
2. Main sync after the P0-1 PR is reviewed and merged.
3. Runtime verification that P0-1 is effective on clean / synced main before any P0-2 work starts.

P0-1 is a DONE candidate on the task branch only. It is not effective until the branch commit is merged to `main`, local `main` is synced, and the worktree is clean.

PR #1004 was an unrelated Draft dashboard PR and no code from it is merged into this package.

---

## Current Forbidden Work

The following work is blocked until P0-1 is effective on merged main:

1. ExecutionPlan Source Gate implementation.
3. AccountRisk UserPosition integration.
4. PositionMonitorLog implementation.
5. PositionMonitorService implementation.
6. Review UserPosition integration.
7. PushRecheck semantic hardening.
8. ConfusedState + AiConflict hardening.
9. HotReset real action.
10. OpportunityLog.
11. Macro / News / External Context.
12. AI Orchestrator + AiCallLog.
13. Scheduler / Idempotency / Trace.
14. Dashboard Final.
15. Auto-trading of any kind.

---

## Current Known Critical Gaps

1. P0-1 UserPosition is only a branch DONE candidate until merged main confirms it effective.
2. PositionMonitorService missing.
3. tm_position_monitor_log missing.
4. Review does not fully integrate real user position.
5. ExecutionPlan Source Gate not hardened.
6. HotReset real action incomplete.
7. OpportunityLog incomplete.
8. Macro / News runtime not complete.
9. AI orchestrator and ai call log incomplete.
10. Dashboard Final must wait until business semantics are stable.

## P0-1 UserPosition Branch Candidate

Branch: `p0-1-user-position-full-implementation`
Risk: B
Status: DONE candidate
Effective State: pending merged main

Implemented branch evidence:

1. `tm_user_position` persistence exists.
2. Manual open endpoint creates only `MANUAL` + `OPEN` UserPosition rows.
3. Manual close endpoint closes only `OPEN` / `PARTIALLY_CLOSED` rows and sets `CLOSED`.
4. Open query excludes `CLOSED` rows.
5. Output safety fields are fixed true: `manualReviewRequired`, `notTradeInstruction`, `notAutoTrading`, `notOrderExecution`, `notPositionSync`.
6. ExecutionPlan, triggered state, and real_position sync sources are rejected and cannot auto-create UserPosition.
7. No order, execution, auto-trading, dashboard UI, P0-2 Source Gate, AccountRisk, PositionMonitor, Review, or PR #1004 changes are part of this package.

P0-2 remains blocked until this branch is reviewed, merged to `main`, main is synced, and runtime state confirms P0-1 effective.

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
