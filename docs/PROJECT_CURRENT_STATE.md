# Trade Model V1 Current State

Contract: docs/PROJECT_DELIVERY_CONTRACT.md
Contract Version: v1.0
Current Phase: P0-0 Contract Lock + Baseline + Dead Code Candidate Report
Current Phase Status: DONE
Completion Effective State: derived by v1 state runtime
Existing Module Maturity: PARTIAL
Current Work Package: P2-3 Scheduler / Idempotency / Trace DONE candidate
Next Business Phase: P3-1 Dashboard Final
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

P1-4 OpportunityLog is effective because its implementation is merged to clean / synced `main` by PR #1017 and the runtime gate allowed P2-1.

P2-1 Macro / News / External Context is effective because its implementation is merged to clean / synced `main` by PR #1018 commit `d7fef874b39aabbd07f6b05fd97f4725e89e79b5` and the runtime gate allowed P2-2.

P2-2 AI Orchestrator + AiCallLog is effective because its implementation is merged to clean / synced `main` by PR #1019 commit `92fd7cbf17db31c8ea2bfd4673badde1c69d20cd` and the runtime gate allowed P2-3.

P2-3 Scheduler / Idempotency / Trace is only a branch DONE candidate in this worktree.
It is not effective until PR #1020 is reviewed, merged to `main`, local `main` is synced, the worktree is clean, and `bash scripts/v1-state.sh` confirms P2-3 effectivity.

`bash scripts/v1-state.sh` must distinguish `CURRENT_PACKAGE_PR`, `UNRELATED_OPEN_PRS`, and `BLOCK_NEXT_BUSINESS_PHASE_ONLY`. An unrelated Draft PR must not block merging the current P0-0 package PR, but it still blocks the next business phase.

---

## Current Allowed Work

Only the following work is allowed after this P2-3 branch-candidate update:

1. Checks, push, PR review handling, and merge-gate handling for the P2-3 Scheduler / Idempotency / Trace B-risk package in PR #1020.
2. Main sync after PR #1020 is reviewed and merged.
3. Runtime verification that P2-3 is effective on clean / synced main before any P3-1 work starts.

P2-3 is a DONE candidate on the task branch only. It is not effective until the branch commit is merged to `main`, local `main` is synced, and the worktree is clean.

PR #1004 was an unrelated Draft dashboard PR and no code from it is merged into this package.

---

## Current Forbidden Work

The following work is blocked until P2-3 is effective on merged main:

1. P3-1 Dashboard Final.
2. Full E2E Acceptance.
3. Auto-trading of any kind.

---

## Current Known Critical Gaps

1. P2-3 Scheduler / Idempotency / Trace is only a branch DONE candidate until merged main confirms it effective.
2. Dashboard Final remains blocked until P2-3 is reviewed, merged, synced, and runtime-confirmed effective.
3. Production deployment remains blocked by non-production runtime/config evidence.

## P2-3 Scheduler / Idempotency / Trace Branch Candidate

Branch: `p2-3-scheduler-idempotency-trace-full-implementation`
PR: #1020
Risk: B
Status: DONE candidate
Effective State: pending reviewed merge to clean / synced main

Implemented branch evidence:

1. Canonical idempotency key is exactly SHA-256 of normalized symbol, normalized timeframe, canonical timeframe bucket, and resolved rule version. Trigger type, requestId, trigger reference, and parent IDs remain audit metadata only and do not affect the key.
2. `AnalysisTimePolicy` floors supported timeframes `1m,3m,5m,15m,30m,1h,2h,4h,6h,8h,12h,1d`; 1d uses UTC date boundary, and illegal symbol/timeframe/time inputs fail closed instead of defaulting to BTCUSDT / 1m / now.
3. DB idempotency guard uses `tm_analysis_run` unique idempotency key, explicit claim/failure transactions, lease owner, claim version, attempt count, max recovery attempts, expired lease recovery, failed-run recovery, and partial-state recovery blocking.
4. `AnalysisExecutionContext` carries lease owner, claim version, attempt count, requestId, traceId, input snapshot, input hash, parent IDs, and trigger audit fields into the assembler.
5. `markSuccess` and `markFailed` are fenced by analysisId, STARTED status, leaseOwner, and versionNo / claimVersion; stale executors cannot overwrite recovered or successful runs.
6. Direct assembler bypass is disabled with `DIRECT_ASSEMBLER_ENTRY_DISABLED`; production analysis execution enters through `AnalysisRunOrchestrator` and `assemble(AnalysisExecutionContext)`.
7. Request and trace read APIs include `GET /api/analysis/runs/by-request/{requestId}`, `GET /api/analysis/runs/{analysisId}`, `GET /api/analysis/traces/{traceId}`, and `GET /api/analysis/scheduler/status`; these are read-only and do not trigger analysis, AI, monitor, review, Push, external channel, or trading actions.
8. `AnalysisTraceSnapshot` reports `traceStatus`, `missingSegments`, `generatedAt`, and `manualReviewOnly=true`, with COMPLETE / PARTIAL_TRACE / RUNNING / FAILED status semantics.
9. Tests cover canonical key behavior, cross-trigger dedupe, different tuple key changes, manual API 400 fail-closed input, scheduler invalid config fail-closed, lease fencing, real H2 unique index and concurrent idempotency behavior, failed recovery, partial recovery blocking, max attempts, expired lease single-winner recovery, trace by requestId, scheduler status, request correlation, and direct bypass guard.

P3-1 remains blocked until PR #1020 is reviewed, merged to `main`, local `main` is synced, the worktree is clean, and runtime state confirms P2-3 effective.

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

No later business phase may start until PR #1020 is reviewed, merged to main, local main is synced, the worktree is clean, and `Next Business Phase Allowed` becomes YES through the contract gate.
