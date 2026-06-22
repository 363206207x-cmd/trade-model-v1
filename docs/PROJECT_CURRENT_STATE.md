# Trade Model V1 Current State

Contract: docs/PROJECT_DELIVERY_CONTRACT.md
Contract Version: v1.0
Current Phase: P0-0 Contract Lock + Baseline + Dead Code Candidate Report
Current Phase Status: DONE
Completion Effective State: derived by v1 state runtime
Existing Module Maturity: PARTIAL
Current Work Package: P1-4 OpportunityLog DONE candidate
Next Business Phase: P2-1 Macro / News / External Context
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

P1-3 HotReset real action is effective because its implementation is merged to clean / synced `main` and the runtime gate allowed P1-4.

P1-4 OpportunityLog is only a branch DONE candidate in this worktree.
It is not effective until this branch commit is reviewed, merged to `main`, local `main` is synced, the worktree is clean, and `bash scripts/v1-state.sh` confirms P1-4 effectivity.

`bash scripts/v1-state.sh` must distinguish `CURRENT_PACKAGE_PR`, `UNRELATED_OPEN_PRS`, and `BLOCK_NEXT_BUSINESS_PHASE_ONLY`. An unrelated Draft PR must not block merging the current P0-0 package PR, but it still blocks the next business phase.

---

## Current Allowed Work

Only the following work is allowed after this P1-4 branch-candidate update:

1. Checks, push, PR creation, and merge-gate handling for the P1-4 OpportunityLog B-risk package.
2. Main sync after the P1-4 PR is reviewed and merged.
3. Runtime verification that P1-4 is effective on clean / synced main before any P2-1 work starts.

P1-4 is a DONE candidate on the task branch only. It is not effective until the branch commit is merged to `main`, local `main` is synced, and the worktree is clean.

PR #1004 was an unrelated Draft dashboard PR and no code from it is merged into this package.

---

## Current Forbidden Work

The following work is blocked until P1-4 is effective on merged main:

1. Macro / News / External Context.
2. AI Orchestrator + AiCallLog.
3. Scheduler / Idempotency / Trace.
4. Dashboard Final.
5. Auto-trading of any kind.

---

## Current Known Critical Gaps

1. P1-4 OpportunityLog is only a branch DONE candidate until merged main confirms it effective.
2. Macro / News runtime not complete.
3. AI orchestrator and ai call log incomplete.
4. Dashboard Final must wait until business semantics are stable.

## P1-4 OpportunityLog Branch Candidate

Branch: `p1-4-opportunity-log-full-implementation`
Risk: B
Status: DONE candidate
Effective State: pending merged main

Implemented branch evidence:

1. `tm_opportunity_log` is added as the authoritative opportunity outcome owner.
2. OpportunityLog DO / Mapper / Service / Controller support candidate creation, read, query, evaluate, and Review stats.
3. Analysis mainline creates PENDING opportunity candidates only after AnalysisRun, Decision, ExecutionPlan, AccountRiskSnapshot, and optional PushSnapshot are persisted.
4. Legacy MissedOpportunity is retained for historical compatibility but no longer writes authoritative missed outcomes or treats `tm_real_position` as user execution.
5. User execution evidence uses only exact MANUAL UserPosition `source_ref_id` matching executionPlanId or analysisId.
6. The final statuses EXECUTED_VALID, EXECUTED_INVALID, MISSED_VALID, MISSED_INVALID, PUSHED_NOT_FILLED_VALID, and BLOCKED_BY_RISK_VALID are implemented.
7. Target / invalidation ordering, same-bar ambiguity, unresolved pending state, and missing market-path fail-closed behavior are implemented.
8. MFE / MAE use only persisted closed OHLCV bars and record market data source / trace id.
9. Risk-blocked classification uses persisted AccountRiskSnapshot or PushRecheck RISK_BLOCKED evidence, not current live risk.
10. Resolved opportunity logs are immutable and duplicate candidate/evaluate calls are idempotent.
11. Review opportunity stats return review-only aggregate counts, ratios, status/source maps, and fixed safety fields.
12. No tm_real_position substitution, UserPosition creation or mutation, Recheck / Replay execution, HotReset execution, external quote refresh, Push send, external channel, order execution, auto-trading, Dashboard UI, Macro / News, or P2-1 changes are part of this package.

P2-1 remains blocked until this branch is reviewed, merged to `main`, main is synced, and runtime state confirms P1-4 effective.

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
