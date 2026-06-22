# Trade Model V1 Current State

Contract: docs/PROJECT_DELIVERY_CONTRACT.md
Contract Version: v1.0
Current Phase: P0-0 Contract Lock + Baseline + Dead Code Candidate Report
Current Phase Status: DONE
Completion Effective State: derived by v1 state runtime
Existing Module Maturity: PARTIAL
Current Work Package: P1-3 HotReset real action DONE candidate
Next Business Phase: P1-4 OpportunityLog
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

P0-6 Review integrates UserPosition is effective because its implementation is merged to clean / synced `main` and the runtime gate allowed P1-1.

P1-1 PushRecheck semantic hardening is effective because its implementation is merged to clean / synced `main` and the runtime gate allowed P1-2.

P1-2 ConfusedState + AiConflict hardening is effective because its implementation is merged to clean / synced `main` and the runtime gate allowed P1-3.

P1-3 HotReset real action is only a branch DONE candidate in this worktree.
It is not effective until this branch commit is reviewed, merged to `main`, local `main` is synced, the worktree is clean, and `bash scripts/v1-state.sh` confirms P1-3 effectivity.

`bash scripts/v1-state.sh` must distinguish `CURRENT_PACKAGE_PR`, `UNRELATED_OPEN_PRS`, and `BLOCK_NEXT_BUSINESS_PHASE_ONLY`. An unrelated Draft PR must not block merging the current P0-0 package PR, but it still blocks the next business phase.

---

## Current Allowed Work

Only the following work is allowed after this P1-3 branch-candidate update:

1. Checks, push, PR creation, and merge-gate handling for the P1-3 HotReset real action B-risk package.
2. Main sync after the P1-3 PR is reviewed and merged.
3. Runtime verification that P1-3 is effective on clean / synced main before any P1-4 work starts.

P1-3 is a DONE candidate on the task branch only. It is not effective until the branch commit is merged to `main`, local `main` is synced, and the worktree is clean.

PR #1004 was an unrelated Draft dashboard PR and no code from it is merged into this package.

---

## Current Forbidden Work

The following work is blocked until P1-3 is effective on merged main:

1. OpportunityLog.
2. Macro / News / External Context.
3. AI Orchestrator + AiCallLog.
4. Scheduler / Idempotency / Trace.
5. Dashboard Final.
6. Auto-trading of any kind.

---

## Current Known Critical Gaps

1. P1-3 HotReset real action is only a branch DONE candidate until merged main confirms it effective.
2. OpportunityLog incomplete.
3. Macro / News runtime not complete.
4. AI orchestrator and ai call log incomplete.
5. Dashboard Final must wait until business semantics are stable.

## P1-3 HotReset Real Action Branch Candidate

Branch: `p1-3-hot-reset-real-action`
Risk: B
Status: DONE candidate
Effective State: pending merged main

Implemented branch evidence:

1. HotResetService now evaluates structured extreme events and executes real HotReset actions through a single canonical service path.
2. HotResetPolicy covers EXTREME_PRICE_MOVE, OI_COLLAPSE, LIQUIDITY_DRAIN, and SYSTEMIC_SHOCK with named deterministic thresholds.
3. Event-key idempotency prevents duplicate state invalidation, plan marking, pending push invalidation, and rebuild triggering.
4. CANDIDATE, WAITING_TRIGGER, and TRIGGERED are immediately invalidated into safe AssetState targets; TRIGGERED is not treated as an opened position.
5. ExecutionPlan rows are marked `needs_revalidation=true` with HotReset reason and event evidence.
6. Decision facts are preserved while HotReset invalidation metadata is appended.
7. Pending PushSnapshot rows are marked RECHECK_INVALIDATED without Recheck, Replay, Push send, external channel, UserPosition creation, or order creation.
8. ConfusedState and AccountRisk are recalculated through existing owners, and new confused score / low streak / risk snapshot evidence is persisted.
9. Existing analysis rebuild is triggered after transaction commit with HOT_RESET event evidence; rebuild failure is recorded without restoring old plans.
10. HotReset event persistence records source evidence, action counts, pre/post state, risk/confused recalculation, rebuild result, and execution status.
11. Tests cover all four extreme-event types, state invalidation paths, plan / decision / push invalidation, idempotency, rebuild boundary, safety fields, and no UserPosition / order / auto-trading actions.
12. No OpportunityLog, Macro / News, external AI provider, UserPosition mutation, automatic reduce / close / reverse, order execution, auto-trading, Push send, external channel, Dashboard UI, or P1-4 changes are part of this package.

P1-4 remains blocked until this branch is reviewed, merged to `main`, main is synced, and runtime state confirms P1-3 effective.

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
