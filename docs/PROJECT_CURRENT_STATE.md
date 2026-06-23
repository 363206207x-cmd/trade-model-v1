# Trade Model V1 Current State

Contract: docs/PROJECT_DELIVERY_CONTRACT.md
Contract Version: v1.0
Current Phase: P0-0 Contract Lock + Baseline + Dead Code Candidate Report
Current Phase Status: DONE
Completion Effective State: derived by v1 state runtime
Existing Module Maturity: PARTIAL
Current Work Package: P2-1 Macro / News / External Context DONE candidate
Next Business Phase: P2-2 AI Orchestrator + AiCallLog
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

P2-1 Macro / News / External Context is only a branch DONE candidate in this worktree.
It is not effective until this branch commit is reviewed, merged to `main`, local `main` is synced, the worktree is clean, and `bash scripts/v1-state.sh` confirms P2-1 effectivity.

`bash scripts/v1-state.sh` must distinguish `CURRENT_PACKAGE_PR`, `UNRELATED_OPEN_PRS`, and `BLOCK_NEXT_BUSINESS_PHASE_ONLY`. An unrelated Draft PR must not block merging the current P0-0 package PR, but it still blocks the next business phase.

---

## Current Allowed Work

Only the following work is allowed after this P2-1 branch-candidate update:

1. Checks, push, PR creation, and merge-gate handling for the P2-1 Macro / News / External Context B-risk package.
2. Main sync after the P2-1 PR is reviewed and merged.
3. Runtime verification that P2-1 is effective on clean / synced main before any P2-2 work starts.

P2-1 is a DONE candidate on the task branch only. It is not effective until the branch commit is merged to `main`, local `main` is synced, and the worktree is clean.

PR #1004 was an unrelated Draft dashboard PR and no code from it is merged into this package.

---

## Current Forbidden Work

The following work is blocked until P2-1 is effective on merged main:

1. AI Orchestrator + AiCallLog.
2. Scheduler / Idempotency / Trace.
3. Dashboard Final.
4. Auto-trading of any kind.

---

## Current Known Critical Gaps

1. P2-1 Macro / News / External Context is only a branch DONE candidate until merged main confirms it effective.
2. AI orchestrator and ai call log incomplete.
3. Dashboard Final must wait until business semantics are stable.

## P2-1 Macro / News / External Context Branch Candidate

Branch: `p2-1-macro-news-external-context-full-implementation`
Risk: B
Status: DONE candidate
Effective State: pending merged main

Implemented branch evidence:

1. `tm_macro_event` and `tm_news_event` are added as persistent external-context event owners.
2. MacroEvent and NewsEvent DO / Mapper / Service support import, query, source validation, dedupe idempotency, and window candidate reads.
3. ExternalContextEvidenceBuilder creates one EvidenceItem per eligible event with eventId, provider, sourceReference, sourceTraceId, event window, impact score, severity, and event type.
4. EvidenceItem schema / DO / VO / mapper preserve source trace fields while ordinary evidence may keep them null.
5. Macro sourcePublishedAt may fall back to eventTime with `MACRO_SOURCE_PUBLISHED_AT_FALLBACK_EVENT_TIME`; News sourcePublishedAt is mandatory.
6. ACTIVE / NEAR / EXPIRED / CANCELLED / RETRACTED and future-published source windows are enforced locally without external network fetch.
7. Decision applies external high-impact risk, lowers confidence one level, preserves rule-layer direction, and fail-closes blocking or missing-source contexts.
8. ExecutionPlan source gate becomes BLOCKED with fixed external reason codes when active blocking windows or missing source are present.
9. PositionMonitor reads external context, exposes external context fields, sets HIGH_RISK / RISK_REVIEW for active blocking context, and does not mutate UserPosition or issue order actions.
10. Dashboard adds a read-only external context panel and `/api/external-context/dashboard-status` status feed.
11. ExternalContextController exposes import/query/current/detail/dashboard-status endpoints only; no delete, provider refresh, external fetch, Push send, order, trade, or execution endpoint is added.
12. Tests cover near events, expired events, major news, missing source, source traceability, dedupe, blocking policy, PositionMonitor integration, controller safety, and dashboard DOM IDs.

P2-2 remains blocked until this branch is reviewed, merged to `main`, main is synced, and runtime state confirms P2-1 effective.

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
