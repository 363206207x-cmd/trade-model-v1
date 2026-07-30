# Contract Change Log

## v1.0

Initial active project delivery contract.

Rules:

1. PROJECT_DELIVERY_CONTRACT.md cannot be silently changed.
2. Any contract change must be recorded here.
3. Any change that modifies phase order requires explicit human confirmation.
4. Any change that modifies Done Criteria requires explicit human confirmation.
5. Any change that weakens safety boundaries requires explicit human confirmation.

---

## Change Template

Date:
Changed by:
Reason:
Before:
After:
Does this change phase order:
Does this change done criteria:
Does this weaken safety boundaries:
Human confirmation required:


---

## v1.0-p0-0-reconciliation-draft

Date: 2026-06-20
Changed by: Codex
Reason: P0-0 Global Repository Audit Evidence + Contract Reconciliation Draft.
Before: Local P0-0 draft did not explicitly separate Phase Status from Existing Module Maturity and did not mark machine-readable legacy files as compatibility-only.
After: Draft adds P0-0 governance exception, Phase Status vs Existing Module Maturity, merged-main effectivity, fact-source priority, controlled emergency exception, production deployment readiness blockers, and automation migration list.
Does this change phase order: No.
Does this change done criteria: Clarifies only; business Done Criteria remain strict.
Does this weaken safety boundaries: No.
Human confirmation required: Yes before merging to main.


---

## v1.0-p0-0-workflow-migration-draft

Date: 2026-06-20
Changed by: Codex
Reason: P0-0 Contract Workflow Migration draft.
Before: Workflow scripts and compatibility files could still use legacy V1 review-only runtime facts as the current-task driver.
After: Workflow scripts and compatibility files are migrated to Project Delivery Contract / Delivery Progress Matrix / Project Current State priority, with ACTIVE_MAINLINE_STATUS and CODEX_NEXT_TASK marked DERIVED_ONLY.
Does this change phase order: No.
Does this change done criteria: No.
Does this weaken safety boundaries: No.
Human confirmation required: Yes before P0-0 closure.


---

## v1.0-p0-0-closure-readiness-candidate

Date: 2026-06-21
Changed by: Codex
Reason: P0-0 Contract Delivery Closure Readiness.
Before: P0-0 workflow migration draft remained IN_PROGRESS.
After: P0-0 is marked DONE candidate with Completion Effective State = PENDING_MERGED_MAIN; P0-1 remains blocked until the package is separately staged, committed, pushed, reviewed, merged to main, main is synced, and the worktree is clean.
Does this change phase order: No.
Does this change done criteria: Strengthens P0-0 governance criteria by explicitly requiring PROJECT_GLOBAL_AUDIT.md and migrated workflow automation.
Does this weaken safety boundaries: No.
Human confirmation required: Yes before staging / committing / pushing / PR creation.

---

## v1.0-p0-1-user-position-done-candidate

Date: 2026-06-22
Changed by: Codex
Reason: P0-1 UserPosition Manual Workflow B-risk implementation package.
Before: P0-1 UserPosition was NOT_STARTED with missing schema, DO, Mapper, DTO, Service, Controller, Req/VO/Enums, and tests.
After: P0-1 is a branch DONE candidate with manual-only UserPosition persistence, manual open / manual close APIs, open-position query excluding CLOSED rows, fixed safety fields, fail-closed validation, and tests. It is effective only after reviewed merge to clean / synced main.
Does this change phase order: No.
Does this change done criteria: No.
Does this weaken safety boundaries: No.
Human confirmation required: Yes before B-risk PR merge.

---

## v1.0-p1-2-confused-state-ai-conflict-hardening-done-candidate

Date: 2026-06-22
Changed by: Codex
Reason: P1-2 ConfusedState + AiConflict hardening B-risk implementation package.
Before: P1-2 ConfusedState + AiConflict hardening was NOT_STARTED/PARTIAL with existing DecisionEngine, ConfusedState, AiConflict, AssetState, and AI role display assets, but no contract-hard confused thresholds, persisted low-streak exit semantics, directional push block, or rule-layer direction preservation proof.
After: P1-2 is a branch DONE candidate with ConfusedStatePolicy thresholds, confused_score >= 70 CONFUSED entry, confused_score >= 85 directional push block, two consecutive low cycles below 55 before COOLING exit, per-symbol persisted confused_low_streak, no direct TRIGGERED / WAITING_TRIGGER after exit, bounded single-AI-objection behavior, rule-layer direction preservation, GPT / Gemini / Grok advisory-only handling, allowed AI effects limited to confidence / risk / plan mode / confused state, PushSnapshot directional guard, and tests for aligned, minor, major, and extreme conflicts. It is effective only after reviewed merge to clean / synced main.
Does this change phase order: No.
Does this change done criteria: No.
Does this weaken safety boundaries: No.
Human confirmation required: Yes before B-risk PR merge.

---

## v1.0-p0-5-position-monitor-service-done-candidate

Date: 2026-06-22
Changed by: Codex
Reason: P0-5 PositionMonitorService B-risk implementation package.
Before: P0-5 PositionMonitorService was NOT_STARTED/PARTIAL with PositionSync and provider observation assets, but no UserPosition monitor service, controller, single/batch monitor run, monitor judgment, or one-log-per-run integration.
After: P0-5 is a branch DONE candidate with PositionMonitorService, PositionMonitorController, single and batch active UserPosition monitoring, LONG / SHORT logic, LOGIC_VALID / LOGIC_WEAKENED / PLAN_INVALIDATED / HIGH_RISK states, near stop loss, near take profit, risk increased detection, read-only MarketQuote / ExecutionPlan / AccountRisk context, one PositionMonitorLog write per successful run, safety fields, and tests. It is effective only after reviewed merge to clean / synced main.
Does this change phase order: No.
Does this change done criteria: No.
Does this weaken safety boundaries: No.
Human confirmation required: Yes before B-risk PR merge.

---

## v1.0-p0-2-execution-plan-source-gate-done-candidate

Date: 2026-06-22
Changed by: Codex
Reason: P0-2 ExecutionPlan Source Gate B-risk implementation package.
Before: P0-2 ExecutionPlan Source Gate was NOT_STARTED/PARTIAL with existing SourceTrace and BoundaryCandidate assets but no exact ExecutionPlanSourceGate, BoundaryCandidateSourceGate, or NumericBoundarySourceValidator.
After: P0-2 is a branch DONE candidate with ExecutionPlan Source Gate validation, BoundaryCandidate DTO.valid gate enforcement, numeric boundary source validation, fail-closed handling for missing evidence/fallback/incomplete/review-only/AI-only/numeric-without-source outputs, safety fields, schema fields, and tests. It is effective only after reviewed merge to clean / synced main.
Does this change phase order: No.
Does this change done criteria: No.
Does this weaken safety boundaries: No.
Human confirmation required: Yes before B-risk PR merge.

---

## v1.0-gh-open-pr-gate-fix

Date: 2026-06-22
Changed by: Codex
Reason: Resolve GH_NOT_AVAILABLE gate after P0-2 merge.
Before: Runtime state could keep `GH_NOT_AVAILABLE` or stale local-main sync blockers even when `gh CLI` was available, PR #1008 was merged, HEAD matched `origin/main`, and open PR count was zero.
After: Runtime state reports open PR check source/count/status, treats `gh CLI` open PR count `0` as `OPEN_PR_STATUS=NONE`, keeps fail-closed behavior when `gh` is unavailable or open PRs exist, and accepts clean HEAD==origin/main verification as clean/synced main without weakening merged-main effectivity.
Does this change phase order: No.
Does this change done criteria: No.
Does this weaken safety boundaries: No.
Human confirmation required: No; A-risk workflow gate repair only.

---

## v1.0-p0-3-account-risk-user-position-done-candidate

Date: 2026-06-22
Changed by: Codex
Reason: P0-3 AccountRisk integrates UserPosition B-risk implementation package.
Before: P0-3 AccountRisk had snapshot/read assets, but did not consume UserPosition as the manual position fact source and did not provide open / partially closed / closed UserPosition risk semantics.
After: P0-3 is a branch DONE candidate with UserPositionRiskAdapter, read-only AccountRisk API, OPEN / PARTIALLY_CLOSED inclusion, CLOSED exclusion, leverage / position size / concentration / conservative directional correlation / drawdown-or-VaR proxy risk, high-risk blocking, PushRecheck read-only consumption, safety fields, and tests. It is effective only after reviewed merge to clean / synced main.
Does this change phase order: No.
Does this change done criteria: No.
Does this weaken safety boundaries: No.
Human confirmation required: Yes before B-risk PR merge.

---

## v1.0-p0-4-position-monitor-log-done-candidate

Date: 2026-06-22
Changed by: Codex
Reason: P0-4 PositionMonitorLog B-risk implementation package.
Before: P0-4 PositionMonitorLog was NOT_STARTED with no `tm_position_monitor_log`, DO, DTO, Mapper, Service, Review query path, or monitor-log tests.
After: P0-4 is a branch DONE candidate with monitor-log persistence, one-log-per-recordMonitorRun service behavior, immutable read queries, Review read-only monitor-log query path, safety fields, forbidden action guardrails, and tests for normal, weakened, invalidated, and high-risk scenarios. It is effective only after reviewed merge to clean / synced main.
Does this change phase order: No.
Does this change done criteria: No.
Does this weaken safety boundaries: No.
Human confirmation required: Yes before B-risk PR merge.

---

## v1.0-p0-6-review-user-position-done-candidate

Date: 2026-06-22
Changed by: Codex
Reason: P0-6 Review integrates UserPosition B-risk implementation package.
Before: P0-6 Review integrates UserPosition was NOT_STARTED/PARTIAL with ReviewController, ReviewService, ReviewResultMapper, ReviewAggregateService, and PositionMonitorLog read assets, but no closed UserPosition review adapter, real user-position ReviewSummary, execution deviation, warning timeliness, ignored-warning judgment, or manual rule feedback endpoint tied to UserPosition.
After: P0-6 is a branch DONE candidate with UserPositionReviewAdapter, closed UserPosition ReviewSummary, exact ExecutionPlan read, real user entry / close / stop-loss / take-profit / quantity / leverage reads, all PositionMonitorLog timeline reads, win / loss / breakeven calculation, execution deviation, plan invalidation, warning timeliness, ignored warning, manual rule feedback through existing ReviewService and rule-version audit owners, safety fields, and tests. It is effective only after reviewed merge to clean / synced main.
Does this change phase order: No.
Does this change done criteria: No.
Does this weaken safety boundaries: No.
Human confirmation required: Yes before B-risk PR merge.

---

## v1.0-p1-1-push-recheck-semantic-hardening-done-candidate

Date: 2026-06-22
Changed by: Codex
Reason: P1-1 PushRecheck semantic hardening B-risk implementation package.
Before: P1-1 PushRecheck semantic hardening was NOT_STARTED/PARTIAL with Recheck assets that still used legacy status names that could be misread as trading authorization.
After: P1-1 is a branch DONE candidate with review-only canonical Recheck statuses, fixed RecheckResult safety fields, EXPIRED / DRIFTED_FROM_ENTRY_ZONE / RISK_BLOCKED / CONFUSED_BLOCKED semantics, legacy status compatibility reads, scheduler/backlog compatibility, replay/log canonical status output, and tests proving Recheck cannot create UserPosition or trigger trade actions. It is effective only after reviewed merge to clean / synced main.
Does this change phase order: No.
Does this change done criteria: No.
Does this weaken safety boundaries: No.
Human confirmation required: Yes before B-risk PR merge.

---

## v1.0-p1-3-hot-reset-real-action-done-candidate

Date: 2026-06-23
Changed by: Codex
Reason: P1-3 HotReset real action B-risk implementation package.
Before: P1-3 HotReset real action was NOT_STARTED/PARTIAL with HotReset event evidence, AssetState hot-reset flags, and a compatibility no-op path, but no real action invalidating candidate / waiting / triggered state, plan revalidation marking, risk/confused recalculation, event-key idempotency, or rebuild trigger.
After: P1-3 is a branch DONE candidate with structured EXTREME_PRICE_MOVE / OI_COLLAPSE / LIQUIDITY_DRAIN / SYSTEMIC_SHOCK triggers, event-key idempotency, immediate CANDIDATE / WAITING_TRIGGER / TRIGGERED invalidation, safe AssetState transitions, ExecutionPlan needs_revalidation marking, Decision HotReset invalidation metadata, pending PushSnapshot invalidation, ConfusedState recalculation, AccountRisk recalculation, analysis rebuild after transaction commit, complete HotReset event persistence, rebuild failure evidence, and tests. It is effective only after reviewed merge to clean / synced main.
Does this change phase order: No.
Does this change done criteria: No.
Does this weaken safety boundaries: No.
Human confirmation required: Yes before B-risk PR merge.

---

## v1.0-p1-4-opportunity-log-done-candidate

Date: 2026-06-23
Changed by: Codex
Reason: P1-4 OpportunityLog B-risk implementation package.
Before: P1-4 OpportunityLog was NOT_STARTED/PARTIAL with historical `tm_missed_opportunity` assets only, no authoritative `tm_opportunity_log`, no final opportunity outcome classification, no persisted OHLCV target/invalidation ordering, no MFE / MAE, and no Review opportunity stats.
After: P1-4 is a branch DONE candidate with `tm_opportunity_log`, OpportunityLog DO / Mapper / Service / Controller, authoritative candidate creation after persisted analysis facts, exact MANUAL UserPosition source-ref execution evidence, legacy MissedOpportunity compatibility freeze, EXECUTED_VALID / EXECUTED_INVALID / MISSED_VALID / MISSED_INVALID / PUSHED_NOT_FILLED_VALID / BLOCKED_BY_RISK_VALID statuses, target / invalidation ordering, same-bar ambiguity, MFE / MAE from persisted OHLCV, Review opportunity stats, fixed safety fields, and tests. It is effective only after reviewed merge to clean / synced main.
Does this change phase order: No.
Does this change done criteria: No.
Does this weaken safety boundaries: No.
Human confirmation required: Yes before B-risk PR merge.

---

## v1.0-p2-1-macro-news-external-context-done-candidate

Date: 2026-06-23
Changed by: Codex
Reason: P2-1 Macro / News / External Context B-risk implementation package.
Before: P2-1 Macro / News / External Context was NOT_STARTED/NONE with no `tm_macro_event`, `tm_news_event`, MacroEventService, NewsEventService, ExternalContextEvidenceBuilder, source traceable external EvidenceItem, event-window Decision / ExecutionPlan / PositionMonitor integration, or dashboard panel.
After: P2-1 is a branch DONE candidate with macro/news event persistence, import/query services, source validation and dedupe idempotency, external-context evidence generation with eventId/provider/sourceReference/sourceTraceId/window trace fields, high-impact/manual-review policy, active blocking-window and missing-source fail-closed policy, Decision risk/confidence integration without direction reversal, ExecutionPlan source-gate blocking, PositionMonitor external context status, read-only dashboard status panel, controller endpoints, and tests. It is effective only after reviewed merge to clean / synced main.
Does this change phase order: No.
Does this change done criteria: No.
Does this weaken safety boundaries: No.
Human confirmation required: Yes before B-risk PR merge.

---

## v1.0-p2-2-ai-orchestrator-ai-call-log-done-candidate

Date: 2026-06-23
Changed by: Codex
Reason: P2-2 AI Orchestrator + AiCallLog B-risk implementation package.
Before: P2-2 AI Orchestrator + AiCallLog was NOT_STARTED/PARTIAL with a legacy `AiCallLogDO` and heuristic GPT/Gemini/Grok display strings, but no provider client abstraction, safe provider adapters, usage guard, real call-log persistence, fallback orchestration, or read-only AI status/log APIs.
After: P2-2 is a branch DONE candidate with `AiDecisionOrchestratorService`, `AiProviderClient` abstraction, safe OpenAI/GPT, Gemini, and xAI/Grok adapters, strict review-only prompt/response parsing, prompt-injection and forbidden-field guards, timeout/failure/malformed/rate-limit/budget fallback, `tm_ai_call_log` persistence with token/cost/latency/provider/fallback/traceId evidence, read-only AI status/log APIs, and DecisionEngine integration that preserves rule-layer direction while AI can only support/challenge/abstain through bounded conflict downgrade. It is effective only after reviewed merge to clean / synced main.
Does this change phase order: No.
Does this change done criteria: No.
Does this weaken safety boundaries: No.
Human confirmation required: Yes before B-risk PR merge.

---

## v1.0-p2-3-scheduler-idempotency-trace-done-candidate

Date: 2026-06-23
Changed by: Codex
Reason: P2-3 Scheduler / Idempotency / Trace B-risk implementation package contract blocker fixes for PR #1020.
Before: P2-3 had scheduler/requestId assets but idempotency keys included trigger metadata, invalid time input could fall back, lease completion was not fenced, direct assembler entry remained available, by-request trace and scheduler status read APIs were missing, and real DB concurrency/recovery tests were incomplete.
After: P2-3 is a branch DONE candidate with canonical tuple idempotency, timeframe-aware buckets, fail-closed input validation, DB unique-key concurrency guard, leaseOwner/claimVersion/attemptCount fencing, failed and expired-lease recovery, partial-state/max-attempt blocking, redacted error persistence, disabled direct assembler bypass, requestId/traceId/input snapshot persistence, by-request trace, scheduler status, traceStatus/missingSegments/generatedAt/manualReviewOnly trace snapshots, and real DB integration tests. It is effective only after reviewed merge to clean / synced main.
Does this change phase order: No.
Does this change done criteria: No.
Does this weaken safety boundaries: No.
Human confirmation required: Yes before B-risk PR merge.

---

## v1.0-fe04-semantic-contract-v2

Date: 2026-07-28
Changed by: Codex
Reason: Reconcile the FE-04 information architecture with the approved mobile
product decisions and the capabilities already exposed by the current
Dashboard, analysis, position, notification, and settings contracts.
Before: Mobile navigation, Home status ownership, Asset Card interaction,
Execution Advice provenance, AI-analysis entry, notification scope, Telegram
status, and asset-search availability were described inconsistently across the
existing frontend contract documents.
After: The contract freezes five target mobile tabs with per-tab capability
status; separates selected-asset Home status from existing system summaries;
keeps the Asset Card body as a context selector with a separate authoritative
Analysis Detail affordance; defines rule-led, source-verified Execution Advice;
keeps exactly three AI roles; limits product notification sources to
asset-opportunity and UserPosition-risk events; marks Telegram as an extension;
and marks market search/watch persistence as partial and fail-closed.
Does this change phase order: No.
Does this change done criteria: No.
Does this weaken safety boundaries: No.
Human confirmation required: Yes before a separate Figma baseline freeze or
frontend implementation task. This documentation-only update does not make a
runtime capability effective.

---

## v1.0-p3-h3-merged-main-governance-alignment

Date: 2026-07-28
Changed by: Codex
Reason: Remove stale FE-02/P3-U2 handoff references after FE-02, P3-H1,
FE-03, and P3-H3 became effective on merged main.
Before: Current-state and compatibility files still pointed to PR #1134 or PR
#1137 as open/unmerged work, and the Delivery Progress Matrix still blocked
FE-03 behind FE-02.
After: Governance records the merged-main evidence through P3-H3 at
`d523dc3e69920d6dd80a0d49f344f86757eb7b9e`, keeps FE-04 frontend
`NOT_STARTED`, and limits the next allowed package to a separately authorized
FE-04 Figma Baseline Freeze.
Does this change phase order: No.
Does this change done criteria: No.
Does this weaken safety boundaries: No.
Human confirmation required: Yes before Figma modification or any later FE-04
implementation package. This local documentation alignment becomes effective
project governance only after it is merged to clean, synced `main`.

---

## v1.0-fe04-figma-baseline-registration

Date: 2026-07-28
Changed by: Codex
Reason: Register the already-frozen FE-04 Figma baseline in repository
governance so the semantic contract, design identity, delivery matrix, current
state, and next-task mirror agree.
Before: The approved design contained exact FE-04 frames and components, but
merged repository governance still stated that no authoritative Figma
Page/Frame/Frame ID baseline had been frozen.
After: Merged-main governance records `Trade Model Design System`, mobile
frames `296:2` through `296:7`, desktop frames `296:8` through `296:12`, the
six approved component node IDs, and Mobile Navigation V2 in
`docs/FE04_POSITION_MONITORING_IMPLEMENTATION_FREEZE.md`. FE-04 frontend
remains `NOT_STARTED`; the registration authorizes no runtime, API, Figma, or
trading change. The next bounded package is FE-04A Shell & Navigation plus
FE-04B Home Dashboard Integration.
Does this change phase order: No.
Does this change done criteria: No.
Does this weaken safety boundaries: No.
Human confirmation required: Yes before FE-04A + FE-04B implementation. This
record describes the registration's merged-main state; it does not mark FE-04
implemented or complete.

---

## v1.0-fe04e-opportunity-public-projection-candidate

Date: 2026-07-30
Changed by: Codex
Reason: Close the FE-04E P1 privacy boundary in which an authenticated shared
`OPPORTUNITY` response could carry UserPosition- or account-risk-derived
fields and relied on frontend filtering.
Before: `OPPORTUNITY` and `POSITION_RISK` Push Detail shared one
private-field-capable response record. The shared path could serialize Recheck
account-risk status, risk level, or `failReasonJson`.
After: PR #1155 defines `OPPORTUNITY` as
`AUTHENTICATED_SHARED_PUBLIC_PROJECTION` and `POSITION_RISK` as
`OWNER_SCOPED_PRIVATE_PROJECTION`. The public mapper reads only exact public
identity, safe allowlisted opportunity status, public timestamp, and public
description inputs; the public response variant cannot carry UserPosition,
account-risk, position-risk, Recheck risk, `failReasonJson`, or private risk
reason fields. It also omits internal `pushId` and other private Recheck
references. Raw user-facing `/{pushId}/latest`, `/{pushId}/logs`, and Dashboard
preview-by-`pushId` reads fail closed before raw Recheck data is read because
their persisted rows do not provide an owner identity. The legacy raw
`PushRecheckService` latest/list methods also fail closed; lower-level mapper
reads remain internal operational inputs and are not user authorization paths.
The private variant remains exact current-user scoped.

The same PR candidate restores the read-state contract. `READY` now requires a
complete legal Push/Recheck pair, completed execution, and matching statuses.
Known incomplete/in-progress data maps to `PARTIAL`; illegal enum, malformed
JSON, and contradictory states map to `ERROR`; absent/inaccessible exact
resources map to `MISSING`; and only an empty successful collection maps to
`EMPTY`. The structural readiness mapper does not select account-risk or
position-risk columns; `failReasonJson` is inspected only inside the service
for structural validity and is never part of the public DTO or response.
Does this change phase order: No.
Does this change done criteria: No.
Does this weaken safety boundaries: No; it strengthens transport-level privacy.
Human confirmation required: Yes before B-risk PR #1155 merge. The candidate
is not effective until exact-head review and merged main.
