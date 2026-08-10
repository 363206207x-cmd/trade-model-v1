# Fundamental AI v4.1 Object Ownership Map

Status: `FROZEN_FOR_AUTHORIZED_IMPLEMENTATION`

Authority: `docs/product-sources/FUNDAMENTAL_AI_V4_1_DECISION_CHAIN.md`

This map prevents the v4.1 implementation from creating a second object or
service family where the repository already has a canonical owner. Exact
implementation details remain subject to the implementation audit, but the
ownership decision below is frozen.

## Reuse Existing Owners

| Product object | Canonical repository owner | v4.1 action |
|---|---|---|
| Asset Pool | existing `providercall/universe`, `service/watchlistsource`, and `service/watchlistscan` families | consolidate and extend the existing watchlist/asset-pool path; do not create a parallel pool stack |
| AnalysisRun | `AnalysisRunDO`, `AnalysisRunMapper`, and `analysisrun/AnalysisRunOrchestrator` | reuse |
| EvidenceItem | `EvidenceItemDO` and `EvidenceItemMapper` | reuse |
| ScoreItem | `ScoreItemDO` and `ScoreItemMapper` | reuse |
| DecisionBundle | `DecisionBundleVO` plus existing decision result/service ownership | extend the existing contract; do not create a second decision bundle |
| ExecutionPlan | `ExecutionPlanDO`, `ExecutionPlanMapper`, and existing execution-plan source gates/services | extend to the validated Final contract; preserve existing identity |
| UserPosition | `UserPositionDO`, mapper, service, controller, and `tm_user_position` | reuse unchanged as the manual-position owner |
| PositionMonitorLog | `PositionMonitorLogDO`, mapper/service, and `tm_position_monitor_log` | reuse unchanged as the monitor-log owner |
| Review | `ReviewResultDO`, mapper/services, and `tm_review_result` | extend the existing review chain; do not create a second review subsystem |

## Canonical Reconciliation Before New Persistence

| Frozen product concept | Existing repository assets | Ownership decision |
|---|---|---|
| Opportunity | `AssetStateDO`/`AssetStateService`/`tm_asset_state` already own the eight current states; `OpportunityLogDO`/`tm_opportunity_log` own opportunity history | use the existing state owner and audit owner as the canonical base; a new Opportunity persistence owner is allowed only if the implementation proves these owners cannot satisfy stable identity without duplication |
| ExecutionPlanCandidate | review-only candidate DTOs, candidate promotion classes, boundary candidate source gates, and the current ExecutionPlan owner already exist | introduce one persisted Candidate/Final boundary only after reconciling these assets; delete nothing without dead-code evidence and create no parallel candidate pipelines |
| AITrace | `AiCallLogDO`, `AiCallLogService`, `AiCallLogMapper`, and `tm_ai_call_log` already own AI call trace data | extend/normalize this owner to satisfy AITrace; do not create a second AI trace table or service family |
| ConflictResolverResult | `AiConflictResolverService`, `AiConflictResolverServiceImpl`, and `AiConflictResult` already own conflict resolution | extend/normalize this owner to satisfy the frozen result contract; do not create a second resolver |

## Object Relationship Freeze

- FinalExecutionPlan references `candidate_id` and `analysis_id`.
- UserPosition references `final_plan_id` only after an authenticated explicit
  manual action.
- AI call records reference `trace_id` and `analysis_id`.
- Opportunity transition records reference `opportunity_id` and `analysis_id`.
- Candidate is never returned as Final before Rule Validation.
- ExecutionPlan, AssetState/Opportunity, and UserPosition remain separate
  identities and state domains.

## Duplicate-Skeleton Decision

- New business skeleton created by authorization: `NO`.
- Existing repository owners reused: `YES`.
- Duplicate reduction required during implementation: `YES`.
- Capability level changed by this document: `NO`.
- Service/runtime/dashboard/API connected by this document: `NO`.
- Alignment with duplicate-skeleton freeze: `PASS`.
