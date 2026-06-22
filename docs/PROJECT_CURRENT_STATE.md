# Trade Model V1 Current State

Contract: docs/PROJECT_DELIVERY_CONTRACT.md
Contract Version: v1.0
Current Phase: P0-0 Contract Lock + Baseline + Dead Code Candidate Report
Current Phase Status: DONE
Completion Effective State: derived by v1 state runtime
Existing Module Maturity: PARTIAL
Current Work Package: P1-1 PushRecheck semantic hardening DONE candidate
Next Business Phase: P1-2 ConfusedState + AiConflict hardening
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

P1-1 PushRecheck semantic hardening is only a branch DONE candidate in this worktree.
It is not effective until this branch commit is reviewed, merged to `main`, local `main` is synced, the worktree is clean, and `bash scripts/v1-state.sh` confirms P1-1 effectivity.

`bash scripts/v1-state.sh` must distinguish `CURRENT_PACKAGE_PR`, `UNRELATED_OPEN_PRS`, and `BLOCK_NEXT_BUSINESS_PHASE_ONLY`. An unrelated Draft PR must not block merging the current P0-0 package PR, but it still blocks the next business phase.

---

## Current Allowed Work

Only the following work is allowed after this P1-1 branch-candidate update:

1. Checks, push, PR creation, and merge-gate handling for the P1-1 PushRecheck semantic hardening B-risk package.
2. Main sync after the P1-1 PR is reviewed and merged.
3. Runtime verification that P1-1 is effective on clean / synced main before any P1-2 work starts.

P1-1 is a DONE candidate on the task branch only. It is not effective until the branch commit is merged to `main`, local `main` is synced, and the worktree is clean.

PR #1004 was an unrelated Draft dashboard PR and no code from it is merged into this package.

---

## Current Forbidden Work

The following work is blocked until P1-1 is effective on merged main:

1. ConfusedState + AiConflict hardening.
2. HotReset real action.
3. OpportunityLog.
4. Macro / News / External Context.
5. AI Orchestrator + AiCallLog.
6. Scheduler / Idempotency / Trace.
7. Dashboard Final.
8. Auto-trading of any kind.

---

## Current Known Critical Gaps

1. P1-1 PushRecheck semantic hardening is only a branch DONE candidate until merged main confirms it effective.
2. ConfusedState + AiConflict hardening remains incomplete.
3. P1-1 branch candidate must be merged main before P1-2 work is allowed.
4. HotReset real action incomplete.
5. OpportunityLog incomplete.
6. Macro / News runtime not complete.
7. AI orchestrator and ai call log incomplete.
8. Dashboard Final must wait until business semantics are stable.

## P1-1 PushRecheck Semantic Hardening Branch Candidate

Branch: `p1-1-push-recheck-semantic-hardening`
Risk: B
Status: DONE candidate
Effective State: pending merged main

Implemented branch evidence:

1. Canonical PushRecheck statuses are review-only: REVIEW_PASSED, REVIEW_WAITING, DRIFTED_FROM_ENTRY_ZONE, INVALIDATED, RISK_BLOCKED, CONFUSED_BLOCKED, and EXPIRED.
2. RecheckResult fixes notTradeInstruction, notExecutable, notAutoTrading, notOrderExecution, notUserPositionCreation, notPositionMutation, and notTradingAuthorization safety fields to true.
3. Legacy status reads remain compatible while new writes use canonical recheck and push statuses only.
4. Scheduler and backlog reads include CAPTURED, RECHECK_REVIEW_WAITING, and the historical waiting status for compatibility.
5. Replay summary and log APIs normalize historical statuses to safe canonical read models.
6. Tests cover all canonical statuses, legacy compatibility, controller safety fields, scheduler/backlog compatibility, replay summary, and forbidden action field absence.
7. No UserPosition creation or mutation, automatic open / close / reverse, order execution, auto-trading, Push send, external channel, Dashboard UI, P1-2, or PR #1004 changes are part of this package.

P1-2 remains blocked until this branch is reviewed, merged to `main`, main is synced, and runtime state confirms P1-1 effective.

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
