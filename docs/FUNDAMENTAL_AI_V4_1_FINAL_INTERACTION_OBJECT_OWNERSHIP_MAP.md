# Fundamental AI v4.1 Final Interaction Object Ownership Map

Status: `FROZEN_NORMATIVE_ANNEX`

Canonical source:
`docs/product-sources/FUNDAMENTAL_AI_V4_1_DECISION_CHAIN.md`

This map reconciles the frozen interaction contract with current merged-main
ownership. It authorizes extension of canonical owners, not a parallel stack.

## 1. Ownership Decisions

| Product Concept | Current Canonical Asset | Decision | Required Change | Forbidden Duplicate |
|---|---|---|---|---|
| Asset Pool | `AssetPoolItemDO`, `PersistentAssetPoolService`, `tm_asset_pool_item` | REUSE | expose complete search/add/remove/top-up/reset/scan semantics | second watchlist/pool table or fixed-symbol opportunity source |
| Analysis Run | existing AnalysisRun owner and persistence | EXTEND | add explicit `analysisMode` and mode-specific invariant | PreviewAnalysis second aggregate |
| Input Snapshot | existing analysis input snapshot | REUSE | retain source/freshness lineage | UI-only reconstructed snapshot |
| Evidence Item | existing Evidence owner | REUSE | preserve observedAt/freshness/source and collection states | AI-authored evidence store |
| Score Item | existing Eight Score owner | REUSE | retain exact eight dimensions and analysis link | page-specific score model |
| Decision Bundle | existing DecisionBundle owner | EXTEND | expose mode, multi-timeframe and rule-base context without semantic fallback | second decision aggregate |
| Opportunity | existing `AssetStateDO`/Opportunity semantics | EXTEND | keep eight states; support one asset slot with primary/secondary timeframe lineage | page Opportunity or Preview Opportunity |
| Opportunity State Log | `OpportunityStateTransitionDO`, `OpportunityLogDO` | REUSE | ensure every transition carries analysis/timeframe/reason/trigger/trace | ad hoc controller state writes |
| Home Top Opportunity Projection | `OpportunityPriorityRankingService`, `DashboardHomeVO/Service` | EXTEND | add primaryOpportunityId, primaryTimeframe, primaryPlanMode, secondaryOpportunityCount, timeframeConflictState and selected-context exit reason | fixed Top6 list or direct Pool-first-six projection |
| Execution Plan Candidate | `ExecutionPlanCandidateDO` and V11/V12 persistence | REUSE | prohibit Preview creation and API Final impersonation | second Candidate model |
| Conflict Resolver | `ConflictResolverResultDO` | REUSE | preserve independent non-AI ownership and before/after fields | Resolver AITrace role |
| Rule Validation | existing validation/Final validation ownership | EXTEND | expose queryable validation result and source gate; may be separate record only within this owner | validation AI role or implicit boolean-only owner |
| Final Execution Plan | `ExecutionPlanDO` | EXTEND | add lifecycle, version/supersession and revalidation links | second FinalPlan aggregate |
| Plan Revalidation | no independent durable owner confirmed; Final has limited revalidation flags | NEW_INDEPENDENT_OWNER | one planId-scoped record with trigger type, source/result version and audit links | reuse PushRecheck as plan revalidation or second Final |
| AI Trace | `AiCallLogDO` | REUSE | GPT/Gemini/Grok calls only; preserve mode, state, error and fallback | Resolver/Validation impersonating AITrace |
| Three-AI Workspace | approved single workspace and structured role DTOs | REUSE | bind Preview/Decision mode and complete role/collection states | second workspace, three cards or vote model |
| User Position | existing UserPosition owner | REUSE | keep source and opening `finalPlanId`; expose current-latest-plan comparison | plan-as-position or second position model |
| Position Monitor Log | existing PositionMonitorLog owner | REUSE | keep P2 VERIFIED/FRESH trust and original-plan baseline | Final fields mapped into monitoring |
| Account Risk Coverage | current account-risk projection | EXTEND | add `COMPLETE`, `PARTIAL`, `UNKNOWN` coverage state | fake full-account claim |
| Review | existing ReviewResult/Missed Opportunity owners | EXTEND | add independent `missedReason` and `laterOutcome`; preserve at-time snapshot | second review system or hindsight overwrite |
| Push Snapshot / Recheck | `TmPushSnapshotDO`, `TmPushRecheckLogDO` and services | REUSE | expose routed original/current comparison; keep non-trading boundary | Plan Revalidation or Message owner hidden inside Recheck |
| Message | current `MessagePushReadService`, `MessageListDTO`, `PushDetailDTO` projection assets | EXTEND | converge to one persisted Message fact owner with read/dedupe/cooldown/expiry/currentRecheck | Telegram message table as second business owner |
| Channel Delivery / Telegram | current delivery adapter including NoOp/review assets | NEW_INDEPENDENT_OWNER | delivery-attempt/status record subordinate to Message; binding/test settings | independent Telegram business message |
| Async Task | no unified cross-domain task owner confirmed | NEW_INDEPENDENT_OWNER | one task identity for scan, Preview, re-analysis, Three AI, revalidation and Hot Reset | per-page fake progress or six task stacks |
| Event | `MacroEventDO/Service`, `HotResetEventDO` | EXTEND | event type/window/source/scope and Plan Revalidation trigger | calendar-only fake events |
| Event-Asset Relation | no independent durable relation owner confirmed | NEW_INDEPENDENT_OWNER | traceable event-to-asset/plan relation with source | inferred UI-only relation |
| User / Notification Settings | `UserConfigDO/Service`, provider diagnostics and session owner | EXTEND | notification filters, Telegram binding, risk preference, default Pool config | separate settings profile per page |
| System / Provider Readiness | existing provider diagnostics/readiness owner | REUSE | one trusted state source for header and diagnostics | forced READY or duplicate readiness map |
| Full Audit Query | existing owners joined by analysisId/candidateId/traceId | EXTEND | aggregate query/projection only | second audit persistence chain |

## 2. Required Semantic Extensions

| Field / Contract | Owner | Persistence / Projection Rule |
|---|---|---|
| `analysisMode` | AnalysisRun | required enum `ANALYSIS_PREVIEW` / `OPPORTUNITY_DECISION`; immutable for run |
| `planLifecycleState` | FinalExecutionPlan | independent of Plan Mode; preserve version history |
| `revalidationTriggerType` | PlanRevalidationRecord | HOT_RESET, EVENT_WINDOW, DATA_REFRESH, EVIDENCE_CHANGED, MANUAL_REVALIDATION |
| `primaryOpportunityId`, `primaryTimeframe`, `primaryPlanMode` | HomeTopOpportunityProjection | derived from all eligible Pool opportunities, never fixed config |
| `secondaryOpportunityCount`, `timeframeConflictState` | HomeTopOpportunityProjection | one slot per asset; opposing frames never silently average |
| `messageChannelStatus` | ChannelDelivery subordinate to Message | delivery status cannot alter Message business truth |
| `accountRiskCoverageState` | account-risk projection | COMPLETE/PARTIAL/UNKNOWN; UNKNOWN fails closed |
| `asyncTaskState` | AsyncTask | QUEUED/RUNNING/PARTIAL/SUCCEEDED/FAILED/CANCELLED plus stage/retry |
| `missedReason`, `laterOutcome` | Review | separate at-time reason from later result |
| selected asset URL context | Home projection/UI state | route state, not a new business aggregate |

## 3. Production-Path Disposition

| Asset / Pattern | Disposition | Condition |
|---|---|---|
| Candidate fields in Analysis Preview | REMOVE_FROM_PRODUCTION_PATH | replace with Preview role explanations and explicit mode |
| Candidate rendered as Final | REMOVE_FROM_PRODUCTION_PATH | retain Candidate only in audit/detail context |
| fixed/default-symbol Top6 backfill | REMOVE_FROM_PRODUCTION_PATH | dynamic ranking may return fewer than six |
| generic semantic fallback across AI/plan/position fields | REMOVE_FROM_PRODUCTION_PATH | missing values retain exact fail-closed state |
| Telegram NoOp/review-only adapter as final production delivery | DEPRECATE | keep safe fallback/test evidence; replace with authorized delivery under Message |
| historical ownership/authorization docs | DEPRECATE | mark historical and remove from active registry; do not delete evidence |

## 4. Duplicate Skeleton Gate

- New independent owners are limited to PlanRevalidationRecord,
  ChannelDelivery, AsyncTask and EventAssetRelation because each has identity,
  lifecycle and audit semantics not safely owned by an existing aggregate.
- They are subordinate or relational records, not replacements for Plan,
  Message, Analysis, Opportunity, Position, Monitoring, Review, Home or Pool.
- Any new DTO, validator, assembler or service must map to one owner above and
  remove or bypass no canonical owner.
- Existing objects are never deleted without `docs/DEAD_CODE_CANDIDATES.md`
  evidence and replacement tests.

## 5. Position Monitoring Protection

P2 Position Monitoring remains authoritative and unchanged in this
authorization package. Final Plan never auto-creates UserPosition. New Final
versions never replace the opening plan baseline. No automatic open, close,
add, reduce, reverse or exchange-order capability is authorized.
