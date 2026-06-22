# Trade Model V1 Current State

Contract: docs/PROJECT_DELIVERY_CONTRACT.md
Contract Version: v1.0
Current Phase: P0-0 Contract Lock + Baseline + Dead Code Candidate Report
Current Phase Status: DONE
Completion Effective State: derived by v1 state runtime
Existing Module Maturity: PARTIAL
Current Work Package: P0-4 PositionMonitorLog DONE candidate
Next Business Phase: P0-5 PositionMonitorService
Next Business Phase Allowed: NO
Production Deployment Readiness: BLOCKED

---

## Effective State Rule

P0-0 is effective only because its DONE commit is merged to `main`, local `main` is synced, and the worktree was clean when the runtime gate evaluated it.

P0-1 UserPosition is effective because its implementation is merged to clean / synced `main`.

P0-2 ExecutionPlan Source Gate is effective because its implementation is merged to clean / synced `main` and the runtime gate allowed P0-3.

P0-3 AccountRisk integrates UserPosition is effective because its implementation is merged to clean / synced `main` and the runtime gate allowed P0-4.

P0-4 PositionMonitorLog is only a branch DONE candidate in this worktree.
It is not effective until this branch commit is reviewed, merged to `main`, local `main` is synced, the worktree is clean, and `bash scripts/v1-state.sh` confirms P0-4 effectivity.

`bash scripts/v1-state.sh` must distinguish `CURRENT_PACKAGE_PR`, `UNRELATED_OPEN_PRS`, and `BLOCK_NEXT_BUSINESS_PHASE_ONLY`. An unrelated Draft PR must not block merging the current P0-0 package PR, but it still blocks the next business phase.

---

## Current Allowed Work

Only the following work is allowed after this P0-4 branch-candidate update:

1. Checks, push, PR creation, and merge-gate handling for the P0-4 PositionMonitorLog B-risk package.
2. Main sync after the P0-4 PR is reviewed and merged.
3. Runtime verification that P0-4 is effective on clean / synced main before any P0-5 work starts.

P0-4 is a DONE candidate on the task branch only. It is not effective until the branch commit is merged to `main`, local `main` is synced, and the worktree is clean.

PR #1004 was an unrelated Draft dashboard PR and no code from it is merged into this package.

---

## Current Forbidden Work

The following work is blocked until P0-4 is effective on merged main:

1. PositionMonitorService implementation.
2. Review UserPosition integration.
3. PushRecheck semantic hardening beyond the read-only P0-3 risk consumption path.
4. ConfusedState + AiConflict hardening.
5. HotReset real action.
6. OpportunityLog.
7. Macro / News / External Context.
8. AI Orchestrator + AiCallLog.
9. Scheduler / Idempotency / Trace.
10. Dashboard Final.
11. Auto-trading of any kind.

---

## Current Known Critical Gaps

1. P0-4 PositionMonitorLog is only a branch DONE candidate until merged main confirms it effective.
2. PositionMonitorService missing.
3. Review does not fully integrate real user position.
4. P0-4 branch candidate must be merged main before PositionMonitorService work is allowed.
5. HotReset real action incomplete.
6. OpportunityLog incomplete.
7. Macro / News runtime not complete.
8. AI orchestrator and ai call log incomplete.
9. Dashboard Final must wait until business semantics are stable.

## P0-4 PositionMonitorLog Branch Candidate

Branch: `p0-4-position-monitor-log-full-implementation`
Risk: B
Status: DONE candidate
Effective State: pending merged main

Implemented branch evidence:

1. `tm_position_monitor_log` persists each monitor run log.
2. `PositionMonitorLogDO`, `PositionMonitorLogDTO`, `PositionMonitorLogMapper`, and `PositionMonitorLogService` define the single P0-4 owner.
3. `recordMonitorRun` validates UserPosition existence and OPEN / PARTIALLY_CLOSED status, then writes exactly one immutable log row.
4. The log stores `position_id`, `analysis_id`, optional `execution_plan_id`, `current_price`, `logic_status`, `risk_level`, `suggested_action`, reason, snapshots, trace id, and created time.
5. Review exposes `GET /api/review/positions/{positionId}/monitor-logs` as a read-only query path.
6. DTO output carries fixed safety fields and exposes no close / reduce / reverse / order / execution / auto-trading payload fields.
7. Tests cover normal, logic weakened, plan invalidated, high-risk, CLOSED rejection, mapper persistence, Review read-only query, safe limit, and forbidden action-word rejection.
8. No PositionMonitorService, monitor judgment logic, automatic reduce, close, reverse, order execution, auto-trading, Dashboard UI, ReviewSummary, P0-5, or PR #1004 changes are part of this package.

P0-5 remains blocked until this branch is reviewed, merged to `main`, main is synced, and runtime state confirms P0-4 effective.

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
