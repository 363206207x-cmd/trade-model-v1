# Fundamental AI v4.1 Object Ownership Map

Status: `IMPLEMENTED_AND_FROZEN_PENDING_MERGE`

Authority: `docs/product-sources/FUNDAMENTAL_AI_V4_1_DECISION_CHAIN.md`

This is the post-implementation ownership map for PR #1177. It records the
single canonical owner for each product object and prevents a second decision
or position stack.

| Product object | Canonical repository owner | Implementation decision |
|---|---|---|
| Asset | `AssetDO`, `AssetMapper`, `tm_asset` | one stable identity backing the existing Pool/Analysis/state path |
| Asset Pool | `AssetPoolItemDO`, `AssetPoolItemMapper`, `PersistentAssetPoolService`, `tm_asset_pool_item` | extended existing watchlist source; no parallel Pool |
| AnalysisRun | `AnalysisRunDO`, mapper and `AnalysisRunOrchestratorImpl` | reused and extended with owner/Asset/preview |
| EvidenceItem | `EvidenceItemDO`, mapper and existing evidence service | reused and extended with current/change/time/freshness |
| ScoreItem | existing `ScoreItemDO`, mapper and score service | reused |
| DecisionBundle | existing `DecisionResult`/`DecisionBundleVO` and decision services | reused and extended with rule/final Bias and modes |
| Opportunity | `AssetStateDO` + `AssetStateServiceImpl` for current state; `OpportunityStateTransitionDO` and existing `OpportunityLogDO` for audit/outcome | one state owner; no second Opportunity mutation stack |
| ExecutionPlanCandidate | `ExecutionPlanCandidateDO`, mapper and `tm_execution_plan_candidate` | authorized separate Candidate owner; never Final |
| GPT/Gemini/Grok trace | existing `AiCallLogDO`, service, mapper and `tm_ai_call_log` | reused as AITrace; only the three AI roles are stored here |
| ConflictResolverResult | existing resolver service plus `ConflictResolverResultDO`, mapper and table | one resolver owner, separate from AITrace |
| RuleValidationResult | `DecisionChainRuleValidatorImpl` plus `validationResultId` and Final validation fields | one non-AI validation owner; no synthetic AI role |
| FinalExecutionPlan | existing `ExecutionPlanDO`, mapper, services and `tm_execution_plan` | extended in place; requires Candidate, Resolver and Rule Validation PASS |
| UserPosition | existing `UserPositionDO`, mapper/service/controller and `tm_user_position` | preserved as explicit user-action owner |
| PositionMonitorLog | existing P2 monitor entity/mapper/service and `tm_position_monitor_log` | preserved; no v4.1 replacement |
| ReviewResult | existing `ReviewResultDO`, mapper/service and `tm_review_result` | extended in place with decision-chain responsibility |

## Relationship Freeze

- Final references Candidate, Analysis, Opportunity, Resolver, Trace and Rule
  Validation identities.
- Candidate references Analysis, Opportunity, Evidence/Score sources and Trace.
- AITrace references Analysis/Trace and optionally Candidate/Opportunity; it
  never owns Resolver or Rule Validation.
- UserPosition references Final only for `SYSTEM_PLAN_POSITION`; an independent
  manual position is explicitly `MANUAL_INDEPENDENT`.
- Position Monitoring begins only after explicit UserPosition creation.
- Opportunity State is not Market Bias, Plan Mode, Final or UserPosition.

## Duplicate-Skeleton Decision

- duplicate Asset Pool: `0`;
- duplicate Analysis/Evidence/Score/Decision owner: `0`;
- duplicate Candidate/Final pipeline: `0`;
- duplicate AITrace owner: `0`;
- duplicate Resolver or Rule Validation owner: `0`;
- duplicate Position/Monitor/Review stack: `0`;
- automatic trading owner: `0`.

No existing business code was deleted without dead-code evidence.
