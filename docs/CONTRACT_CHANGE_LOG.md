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
