# Trade Model V1 Current State

Contract: docs/PROJECT_DELIVERY_CONTRACT.md
Contract Version: v1.0
Current Phase: P0-0 Contract Lock + Baseline + Dead Code Candidate Report
Current Phase Status: DONE
Completion Effective State: derived by v1 state runtime
Existing Module Maturity: PARTIAL
Current Work Package: P0-2 ExecutionPlan Source Gate DONE candidate
Next Business Phase: P0-3 AccountRisk integrates UserPosition
Next Business Phase Allowed: NO
Production Deployment Readiness: BLOCKED

---

## Effective State Rule

P0-0 is effective only because its DONE commit is merged to `main`, local `main` is synced, and the worktree was clean when the runtime gate evaluated it.

P0-1 UserPosition is effective because its implementation is merged to clean / synced `main`.

P0-2 ExecutionPlan Source Gate is only a branch DONE candidate in this worktree.
It is not effective until this branch commit is reviewed, merged to `main`, local `main` is synced, the worktree is clean, and `bash scripts/v1-state.sh` confirms P0-2 effectivity.

`bash scripts/v1-state.sh` must distinguish `CURRENT_PACKAGE_PR`, `UNRELATED_OPEN_PRS`, and `BLOCK_NEXT_BUSINESS_PHASE_ONLY`. An unrelated Draft PR must not block merging the current P0-0 package PR, but it still blocks the next business phase.

---

## Current Allowed Work

Only the following work is allowed after this P0-2 branch-candidate update:

1. Review, checks, push, PR creation, and merge-gate handling for the P0-2 ExecutionPlan Source Gate B-risk package.
2. Main sync after the P0-2 PR is reviewed and merged.
3. Runtime verification that P0-2 is effective on clean / synced main before any P0-3 work starts.

P0-2 is a DONE candidate on the task branch only. It is not effective until the branch commit is merged to `main`, local `main` is synced, and the worktree is clean.

PR #1004 was an unrelated Draft dashboard PR and no code from it is merged into this package.

---

## Current Forbidden Work

The following work is blocked until P0-2 is effective on merged main:

1. AccountRisk UserPosition integration.
2. PositionMonitorLog implementation.
3. PositionMonitorService implementation.
4. Review UserPosition integration.
5. PushRecheck semantic hardening.
6. ConfusedState + AiConflict hardening.
7. HotReset real action.
8. OpportunityLog.
9. Macro / News / External Context.
10. AI Orchestrator + AiCallLog.
11. Scheduler / Idempotency / Trace.
12. Dashboard Final.
13. Auto-trading of any kind.

---

## Current Known Critical Gaps

1. P0-2 ExecutionPlan Source Gate is only a branch DONE candidate until merged main confirms it effective.
2. PositionMonitorService missing.
3. tm_position_monitor_log missing.
4. Review does not fully integrate real user position.
5. AccountRisk does not yet consume UserPosition.
6. HotReset real action incomplete.
7. OpportunityLog incomplete.
8. Macro / News runtime not complete.
9. AI orchestrator and ai call log incomplete.
10. Dashboard Final must wait until business semantics are stable.

## P0-2 ExecutionPlan Source Gate Branch Candidate

Branch: `p0-2-execution-plan-source-gate-full-implementation`
Risk: B
Status: DONE candidate
Effective State: pending merged main

Implemented branch evidence:

1. `ExecutionPlanSourceGate` requires entry, stop, TP, RR, liquidity, wick confirmation, multi-timeframe, event window, timeframe, and reason sources before VALID.
2. `BoundaryCandidateSourceGate` and `NumericBoundarySourceValidator` prevent source-less numeric boundaries from using `BoundaryCandidateDTO.valid`.
3. Fallback, incomplete, review-only, AI-only, and numeric-without-source outputs fail closed as INCOMPLETE, BLOCKED, or REVIEW_ONLY.
4. ExecutionPlan responses expose source gate status, source completeness summary, missing source reasons, and blocker reasons.
5. Output safety fields are fixed true: `manualReviewRequired`, `notTradeInstruction`, `notExecutable`, `notAutoTrading`, `notOrderExecution`, `notUserPositionCreation`.
6. VALID source-gated plans remain advisory / review-only and do not create UserPosition or execute orders.
7. No UserPosition implementation, order execution, auto-trading, dashboard UI, AccountRisk, PositionMonitor, Review, or PR #1004 changes are part of this package.

P0-3 remains blocked until this branch is reviewed, merged to `main`, main is synced, and runtime state confirms P0-2 effective.

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
