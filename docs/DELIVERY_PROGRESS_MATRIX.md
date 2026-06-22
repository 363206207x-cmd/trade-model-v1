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
| P0-0 | Contract Lock + Baseline + Dead Code Candidate Report | DONE | PARTIAL | PROJECT_DELIVERY_CONTRACT.md exists; PROJECT_CURRENT_STATE.md exists; DELIVERY_PROGRESS_MATRIX.md exists; CODEX_TASK_TEMPLATE.md exists; CONTRACT_CHANGE_LOG.md exists; DEAD_CODE_CANDIDATES.md exists; PROJECT_GLOBAL_AUDIT.md exists; AGENTS.md references the contract; workflow automation migrated; A-risk Auto Merge Rule added for docs / contract / workflow packages; no business code changed; no code deleted. | Effective only after merged main; P0-1 remains blocked while completion state is PENDING_MERGED_MAIN or while unrelated Draft PRs remain open. | Maven PASS; shell syntax PASS; workflow contract PASS; task validation PASS; v1-state/v1-auto contract-first checks PASS; git diff --check PASS; A-risk scope check PASS; no staged files; HEAD unchanged. | P0-0 Contract Delivery Package and Merge |
| P0-1 | UserPosition | DONE candidate | COMPLETE | Branch candidate adds `tm_user_position`, `UserPositionDO`, `UserPositionDTO`, `UserPositionResponseDTO`, `UserPositionVO`, `CreateUserPositionReq`, `CloseUserPositionReq`, status/side/direction/source/source-type enums, `UserPositionMapper`, `UserPositionService`, `UserPositionController`, manual open, manual close, open query excluding CLOSED, source_type fixed to MANUAL, safety fields fixed true, and fail-closed rejection for ExecutionPlan / triggered / real_position auto-create sources. | Effective only after merged main; P0-2 remains blocked until this B-risk PR is reviewed, merged, local main is synced, worktree is clean, and runtime state confirms P0-1 effective. | UserPosition service tests PASS; controller endpoint tests PASS; mapper integration tests PASS; Maven full-suite required before PR. | P0-1 B-risk PR review / merge gate; P0-2 blocked until effective |
| P0-2 | ExecutionPlan Source Gate | NOT_STARTED | PARTIAL | `ExecutionPlan*`, `BoundaryCandidate*`, SourceTrace ownership services and validators exist. | `ExecutionPlanSourceGate`, `BoundaryCandidateSourceGate`, `NumericBoundarySourceValidator` not found by exact scan; gate must block source-less VALID. | Partial existing BoundaryCandidate/SourceTrace tests. | P0-3 |
| P0-3 | AccountRisk integrates UserPosition | NOT_STARTED | PARTIAL | `tm_account_risk_snapshot`, `TmAccountRiskSnapshotDO`, `AccountRiskSnapshotMapper`, account risk dashboard status, PushRecheck read references. | UserPosition adapter/integration, open/closed position risk semantics, true VaR/correlation acceptance. | Account risk JSON and DashboardController tests exist. | P0-4 |
| P0-4 | PositionMonitorLog | NOT_STARTED | NONE | Position monitor log keywords did not find `tm_position_monitor_log` or `PositionMonitorLog`. | schema, DO, Mapper, DTO, Service, tests, Review query path. | None. | P0-5 |
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
