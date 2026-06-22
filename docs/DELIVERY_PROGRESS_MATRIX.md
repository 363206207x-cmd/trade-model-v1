# Trade Model V1 Delivery Progress Matrix

Contract: docs/PROJECT_DELIVERY_CONTRACT.md
Contract Version: v1.0 + P0-0 reconciliation draft

Allowed Phase Status values:

- NOT_STARTED
- IN_PROGRESS
- BLOCKED
- DONE
- DEFERRED
- FROZEN

Allowed Existing Module Maturity values:

- NONE
- PARTIAL
- COMPLETE

Rules:

1. Phase Status tracks contract-phase progress.
2. Existing Module Maturity tracks pre-existing repository assets discovered by scan.
3. Phase DONE is effective only after merged to `main`.
4. docs-only, DTO-only, review-only, preview-only, dashboard-only work cannot mark a business phase DONE.
5. P0-0 is a governance phase; it can complete by P0-0 criteria, but this does not mark business modules DONE.
6. P0-1 must remain NOT_STARTED until P0-0 is DONE on merged main.
7. A-risk docs / contract / workflow packages may auto-merge only under the A-risk Auto Merge Rule; unrelated Draft PRs do not block current package merge but still block the next business phase.

| Phase | Module | Phase Status | Existing Module Maturity | Existing Evidence | Missing Evidence | Test Evidence | Next Allowed |
|---|---|---|---|---|---|---|---|
| P0-0 | Contract Lock + Baseline + Dead Code Candidate Report | DONE | PARTIAL | PROJECT_DELIVERY_CONTRACT.md exists; PROJECT_CURRENT_STATE.md exists; DELIVERY_PROGRESS_MATRIX.md exists; CODEX_TASK_TEMPLATE.md exists; CONTRACT_CHANGE_LOG.md exists; DEAD_CODE_CANDIDATES.md exists; PROJECT_GLOBAL_AUDIT.md exists; AGENTS.md references the contract; workflow automation migrated; A-risk Auto Merge Rule added for docs / contract / workflow packages; no business code changed; no code deleted; merged main effectivity confirmed by runtime gate. | None for P0-0 merged-main effectivity. | Maven PASS; shell syntax PASS; workflow contract PASS; task validation PASS; v1-state/v1-auto contract-first checks PASS; git diff --check PASS; A-risk scope check PASS. | P0-1 UserPosition |
| P0-1 | UserPosition | DONE | COMPLETE | `tm_user_position`, `UserPositionDO`, `UserPositionDTO`, `UserPositionResponseDTO`, `UserPositionVO`, `CreateUserPositionReq`, `CloseUserPositionReq`, status/side/direction/source/source-type enums, `UserPositionMapper`, `UserPositionService`, `UserPositionController`, manual open, manual close, open query excluding CLOSED, source_type fixed to MANUAL, safety fields fixed true, and fail-closed rejection for ExecutionPlan / triggered / real_position auto-create sources are merged on main. | None for P0-1 merged-main effectivity; P0-2 is allowed only through runtime gate on clean / synced main. | UserPosition service tests PASS; controller endpoint tests PASS; mapper integration tests PASS; Maven full-suite PASS on merged main. | P0-2 ExecutionPlan Source Gate |
| P0-2 | ExecutionPlan Source Gate | DONE | COMPLETE | `ExecutionPlanSourceGate`, `BoundaryCandidateSourceGate`, `NumericBoundarySourceValidator`, source gate result fields on `ExecutionPlanVO`, source gate persistence fields on `tm_execution_plan`, fail-closed source validation, DTO.valid gate enforcement, and source gate controller/service response exposure are merged on main. Runtime gate confirmed P0-2 effective on clean / synced main after PR #1008 and workflow gate fix PR #1009. | None for P0-2 merged-main effectivity. | ExecutionPlan Source Gate tests PASS; BoundaryCandidate DTO/service gate tests PASS; PlanService source gate tests PASS; controller response test PASS; Maven full-suite PASS on merged main. | P0-3 AccountRisk integrates UserPosition |
| P0-3 | AccountRisk integrates UserPosition | DONE | COMPLETE | `UserPositionRiskAdapter`, read-only UserPosition account risk calculation, read-only AccountRisk API, OPEN/PARTIALLY_CLOSED inclusion, CLOSED exclusion, leverage / position size / concentration / conservative directional correlation / drawdown-or-VaR proxy risk, high-risk blocking, PushRecheck read-only consumption, and stable service result for future PositionMonitor consumption are merged on main by PR #1010. Runtime gate confirmed P0-3 effective on clean / synced main. | None for P0-3 merged-main effectivity. | UserPosition risk adapter tests PASS; AccountRisk API test PASS; PushRecheck integration tests PASS; Maven full-suite PASS on merged main. | P0-4 PositionMonitorLog |
| P0-4 | PositionMonitorLog | DONE candidate | COMPLETE | Branch candidate adds `tm_position_monitor_log`, `PositionMonitorLogDO`, `PositionMonitorLogDTO`, `PositionMonitorLogMapper`, `PositionMonitorLogService`, `recordMonitorRun` one-log persistence, position / analysis / optional execution plan / current price / logic status / risk level / suggested action / snapshot fields, immutable read queries, and Review read-only monitor-log query path. | Effective only after this B-risk PR is reviewed, merged main, local main is synced, worktree is clean, and runtime state confirms P0-4 effectivity. P0-5 remains blocked until then. | PositionMonitorLog service tests PASS; mapper integration tests PASS; Review query tests PASS; Maven full-suite required before PR. | P0-4 B-risk PR review / merge gate; P0-5 blocked until effective |
| P0-5 | PositionMonitorService | NOT_STARTED | PARTIAL | PositionSync and `tm_real_position` exist as provider observation/read model. | `PositionMonitorService`, monitor-all-open UserPosition behavior, log writes, no auto-close/reverse guard tests. | PositionSync/status tests exist, not full PositionMonitor tests. | P0-6 |
| P0-6 | Review integrates UserPosition | NOT_STARTED | PARTIAL | ReviewController, ReviewAggregateService, ReviewResultMapper, review page and missed/recheck archive views exist. | UserPosition-linked review adapter, execution deviation against manual open/close, monitor-log integration. | Review aggregate tests exist, not UserPosition review closure. | P1-1 |
| P1-1 | PushRecheck semantic hardening | NOT_STARTED | PARTIAL | `PushRecheckController`, `PushRecheckService`, `PushRecheckScheduler`, `tm_push_snapshot`, `tm_push_recheck_log`, status contract and tests exist. | Trading-authorization semantics must be hardened; existing POST recheck/replay/config write paths remain risky. | PushRecheckServiceImplTest and contract tests exist. | P1-2 |
| P1-2 | ConfusedState + AiConflict hardening | NOT_STARTED | PARTIAL | `AiConflictResolverService`, `ConfusedStateService`, `DecisionEngineService`, `ai_conflict_*`, `confused_score`, AI role display exist. | Full hardening thresholds/exit semantics and AI cannot override rule-layer proof need contract tests. | DecisionEngineServiceTest and decision tests exist, partial. | P1-3 |
| P1-3 | HotReset real action | NOT_STARTED | PARTIAL | `HotResetService`, `tm_hot_reset_event`, `HotResetEventMapper`, `AssetStateService.recordHotResetEvent`, dashboard status exist. | Real action invalidating old candidate/waiting/triggered plans, `needs_revalidation`, rebuild trigger, account-risk/confused recalculation. | AssetState and dashboard HotReset tests exist. | P1-4 |
| P1-4 | OpportunityLog | NOT_STARTED | PARTIAL | `tm_missed_opportunity`, `MissedOpportunityService`, mapper, controller, review archive status exist. | Full `tm_opportunity_log`, OpportunityLog semantics/outcomes, MFE/MAE, target/invalidation stats. | MissedOpportunity tests exist, not full OpportunityLog. | P2-1 |
| P2-1 | Macro / News / External Context | NOT_STARTED | NONE | Exact scan for `MacroEvent`, `NewsEvent`, `ExternalContext`, `tm_macro_event`, `tm_news_event` found no code evidence. | schema, services, controller, evidence builder, decision/monitor integration, tests. | None. | P2-2 |
| P2-2 | AI Orchestrator + AiCallLog | NOT_STARTED | PARTIAL | `AiCallLogDO` and heuristic GPT/Gemini/Grok role strings exist; no exact `AiDecisionOrchestrator`. | provider clients/adapters, budget/rate-limit/cache/fallback, `tm_ai_call_log` schema/mapper/service/tests. | AI conflict heuristic tests only. | P2-3 |
| P2-3 | Scheduler / Idempotency / Trace | NOT_STARTED | PARTIAL | `AnalysisSchedulerService`, `PushRecheckScheduler`, `MarketDataScheduler`, requestId/traceId fields exist. | `AnalysisRunOrchestrator`, idempotency guard, duplicate/concurrent/failure recovery tests. | Scheduler and request-id tests partial. | P3-1 |
| P3-1 | Dashboard Final | NOT_STARTED | PARTIAL | Dashboard exists with many review-only panels and current UI branch PR #1004 is open/blocking evidence. | Final business semantics after UserPosition/PositionMonitor/Review integration; no accidental execution_plan/user_position conflation. | DashboardControllerTest exists. | P3-2 |
| P3-2 | Full E2E Acceptance | NOT_STARTED | NONE | No full UserPosition -> monitor -> close -> review -> feedback E2E evidence. | All E2E scenarios in contract plus production deployment readiness. | None. | P3-3 |
| P3-3 | Final Delivery Docs | NOT_STARTED | PARTIAL | Many historical docs exist. | Final API/state/dataflow/dashboard/final acceptance docs tied to completed business chain. | None. | Complete |
| P3-2A | Production Deployment Readiness | BLOCKED | NONE | H2 memory DB, empty password, H2 console enabled, no prod profile/auth/migration/rollback/smoke evidence, simulated provider default. | Production config, persistent DB, auth/authz, secrets, health, migrations, rollback, deployment smoke. | None. | P3-2 cannot be DONE while BLOCKED |
