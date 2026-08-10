# Fundamental AI v4.1 Object Ownership Implementation Report

Status: `IMPLEMENTATION_CANDIDATE_COMPLETE_PENDING_AUDIT`

Authority:

- `docs/product-sources/FUNDAMENTAL_AI_V4_1_DECISION_CHAIN.md`
- `docs/FUNDAMENTAL_AI_V4_1_OBJECT_OWNERSHIP_MAP.md`

## Ownership Result

| Product concept | Canonical implementation owner | Persistence | Implementation action |
|---|---|---|---|
| Asset Pool | existing `service/watchlistsource` and `providercall/universe` families | new `tm_asset_pool_item` | Extended the existing watchlist-source boundary with one persistent owner; no parallel pool service family |
| Opportunity | `AssetStateDO`, `AssetStateService`, and `tm_asset_state` | existing state row plus new `tm_opportunity_state_transition` audit | Reconciled stable `opportunity_id`, exact eight states, canonical transitions, debounce, cooling, and priority |
| AnalysisRun | `AnalysisRunDO`, mapper, and orchestrator | existing `tm_analysis_run` | Reused unchanged |
| EvidenceItem | `EvidenceItemDO` and mapper | existing `tm_evidence_item` | Reused unchanged |
| ScoreItem | `ScoreItemDO` and mapper | existing `tm_score_item` | Reused unchanged |
| DecisionBundle | `DecisionBundleVO` and existing decision owner | existing `tm_decision_result` | Extended current chain output; no second DecisionBundle |
| ExecutionPlanCandidate | `ExecutionPlanCandidateDO` and mapper | new `tm_execution_plan_candidate` | Introduced the single authorized persisted Candidate boundary |
| FinalExecutionPlan | existing `ExecutionPlanDO`, mapper, and plan services | extended `tm_execution_plan` | Existing ExecutionPlan is the Final owner; Final requires Candidate, Opportunity, Resolver, Trace, and Rule Validation PASS |
| AITrace | existing `AiCallLogDO`, service, and mapper | extended `tm_ai_call_log` | Reused and extended with role contract, Candidate link, bounded output, complete input hash, and Final-authority prohibition |
| ConflictResolverResult | existing `AiConflictResolverService` | new `tm_conflict_resolver_result` | Extended the existing resolver with the frozen structured result; no second resolver service family |
| UserPosition | existing `UserPositionDO`, mapper, service, and controller | extended `tm_user_position` with optional `final_plan_id` | Reused; creation remains authenticated and manual, and a supplied Final reference must be validated |
| PositionMonitorLog | existing PositionMonitor ownership | existing `tm_position_monitor_log` | Reused unchanged; P2 Position Monitoring was not rewritten |
| Review | existing `ReviewResultDO`, mapper, and services | extended `tm_review_result` | Reused with Final/Candidate/Trace linkage; no second review subsystem |

## Role Output Ownership

- GPT_FINAL output is persisted as one `ExecutionPlanCandidate` and one AI trace.
- GEMINI_REVIEW and GROK_CHALLENGE outputs remain review/challenge artifacts in
  the AI trace and `ConflictResolverResult`; they do not create plans or a
  duplicate post-trade Review owner.
- Rule Validation alone confirms an existing ExecutionPlan as Final.

## Relationship Result

- FinalExecutionPlan -> `candidate_id`, `analysis_id`, `opportunity_id`,
  `resolver_result_id`, and `trace_id`.
- UserPosition -> optional validated `final_plan_id`, only through the existing
  manual create operation.
- AI trace -> `analysis_id`, `trace_id`, and `candidate_id`.
- Opportunity transition -> `opportunity_id`, `analysis_id`, and `trace_id`.
- Review -> Final, Candidate, and Trace when a validated chain exists.

## Duplicate-Skeleton Check

- Duplicate Analysis/Evidence/Score/Decision family: `0`
- Duplicate ExecutionPlan owner: `0`
- Duplicate UserPosition/PositionMonitor family: `0`
- Duplicate Review family: `0`
- Duplicate AI trace family: `0`
- Duplicate conflict resolver service: `0`

`OBJECT_OWNERSHIP_ALIGNMENT = PASS`
