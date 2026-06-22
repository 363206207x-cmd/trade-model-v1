# Trade Model V1 Current State

Contract: docs/PROJECT_DELIVERY_CONTRACT.md
Contract Version: v1.0
Current Phase: P0-0 Contract Lock + Baseline + Dead Code Candidate Report
Current Phase Status: DONE
Completion Effective State: derived by v1 state runtime
Existing Module Maturity: PARTIAL
Current Work Package: P0-3 AccountRisk integrates UserPosition DONE candidate
Next Business Phase: P0-4 PositionMonitorLog
Next Business Phase Allowed: NO
Production Deployment Readiness: BLOCKED

---

## Effective State Rule

P0-0 is effective only because its DONE commit is merged to `main`, local `main` is synced, and the worktree was clean when the runtime gate evaluated it.

P0-1 UserPosition is effective because its implementation is merged to clean / synced `main`.

P0-2 ExecutionPlan Source Gate is effective because its implementation is merged to clean / synced `main` and the runtime gate allowed P0-3.

P0-3 AccountRisk integrates UserPosition is only a branch DONE candidate in this worktree.
It is not effective until this branch commit is reviewed, merged to `main`, local `main` is synced, the worktree is clean, and `bash scripts/v1-state.sh` confirms P0-3 effectivity.

`bash scripts/v1-state.sh` must distinguish `CURRENT_PACKAGE_PR`, `UNRELATED_OPEN_PRS`, and `BLOCK_NEXT_BUSINESS_PHASE_ONLY`. An unrelated Draft PR must not block merging the current P0-0 package PR, but it still blocks the next business phase.

---

## Current Allowed Work

Only the following work is allowed after this P0-3 branch-candidate update:

1. Checks, push, PR creation, and merge-gate handling for the P0-3 AccountRisk integrates UserPosition B-risk package.
2. Main sync after the P0-3 PR is reviewed and merged.
3. Runtime verification that P0-3 is effective on clean / synced main before any P0-4 work starts.

P0-3 is a DONE candidate on the task branch only. It is not effective until the branch commit is merged to `main`, local `main` is synced, and the worktree is clean.

PR #1004 was an unrelated Draft dashboard PR and no code from it is merged into this package.

---

## Current Forbidden Work

The following work is blocked until P0-3 is effective on merged main:

1. PositionMonitorLog implementation.
2. PositionMonitorService implementation.
3. Review UserPosition integration.
4. PushRecheck semantic hardening beyond the read-only P0-3 risk consumption path.
5. ConfusedState + AiConflict hardening.
6. HotReset real action.
7. OpportunityLog.
8. Macro / News / External Context.
9. AI Orchestrator + AiCallLog.
10. Scheduler / Idempotency / Trace.
11. Dashboard Final.
12. Auto-trading of any kind.

---

## Current Known Critical Gaps

1. P0-3 AccountRisk integrates UserPosition is only a branch DONE candidate until merged main confirms it effective.
2. PositionMonitorService missing.
3. tm_position_monitor_log missing.
4. Review does not fully integrate real user position.
5. P0-3 branch candidate must be merged main before AccountRisk UserPosition consumption is effective.
6. HotReset real action incomplete.
7. OpportunityLog incomplete.
8. Macro / News runtime not complete.
9. AI orchestrator and ai call log incomplete.
10. Dashboard Final must wait until business semantics are stable.

## P0-3 AccountRisk UserPosition Branch Candidate

Branch: `p0-3-account-risk-user-position-integration`
Risk: B
Status: DONE candidate
Effective State: pending merged main

Implemented branch evidence:

1. `UserPositionRiskAdapter` reads `tm_user_position` through the existing `UserPositionMapper` read path.
2. OPEN and PARTIALLY_CLOSED UserPosition rows enter risk calculation.
3. CLOSED UserPosition rows are excluded and counted separately.
4. Leverage, position size, concentration, conservative directional correlation proxy, and drawdown-or-VaR proxy risk are calculated with BigDecimal.
5. High risk returns `riskBlocked=true` / `RISK_BLOCKED`.
6. The result is read-only and carries fixed safety fields: `reviewOnly`, `manualReviewOnly`, `notTradeInstruction`, `notExecutable`, `notAutoTrading`, `notOrderExecution`, `notAutoReduce`, `notAutoClose`, `notAutoReverse`, and `notUserPositionMutation`.
7. PushRecheck consumes the read-only UserPosition risk result and uses the stricter risk status.
8. The risk result is exposed through a stable service interface and a read-only AccountRisk API for future PositionMonitor consumption.
9. No automatic reduce, close, reverse, order execution, auto-trading, Dashboard UI, PositionMonitor, Review, P0-4, or PR #1004 changes are part of this package.

P0-4 remains blocked until this branch is reviewed, merged to `main`, main is synced, and runtime state confirms P0-3 effective.

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
