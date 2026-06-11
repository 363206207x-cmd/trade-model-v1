# V1 Next Minimal Runtime Slice Selection After RuleConfig Closure

## 1. Executive Summary

Current effective main: `2c3224f fix(workflow): handle completed slice fallback name`.

This package compares the remaining safe review-only runtime slice candidates after `RuleConfig runtime audit / rule explainability` visual closure. It is selection/source-read-lite only.

Completed `REVIEW_ONLY_RUNTIME partial` slices:

1. `PositionSync + Dashboard review-only status`
2. `Watchlist + RuleConfig + Dashboard/API review-only status`
3. `MarketQuote freshness / fallback / dashboard API status`
4. `Evidence / Score review-only runtime status`
5. `DecisionResult review-only dashboard/API status`
6. `ExecutionPlan / BoundaryCandidate review-only runtime status`
7. `Review / Replay result status`
8. `Data Source Health dashboard/API status`
9. `RuleConfig runtime audit / rule explainability`

Selected next slice: `Missed Opportunity / Review Archive status`.

Next allowed action: `Source Read for Missed Opportunity / Review Archive status`.

Capability movement: none. The project remains `REVIEW_ONLY_RUNTIME partial`, not Production Wiring.

## 2. Source-Read-Lite Inventory

The selected slice has existing owner assets:

| Asset | Evidence | Source-read implication |
|---|---|---|
| Missed Opportunity API | `MissedOpportunityController` exposes `GET /api/missed-opportunity/query`. | Existing read path can be inventoried before any status design. |
| Missed Opportunity service | `MissedOpportunityServiceImpl` has query methods and an existing guarded record path. | Source read must separate read-only status from generation/write behavior. |
| Mapper/schema | `MissedOpportunityMapper`, `MissedOpportunityDO`, `tm_missed_opportunity`. | Existing persistence owner exists; no schema/config/pom change is needed for source read. |
| Review aggregate | `ReviewAggregateServiceImpl` reads missed rows by analysis id. | Existing review/archive surface can be inspected without replay execution or review result generation. |
| Dashboard summary signal | `LightSystemStatusVO.missedValidOpportunityCount` and dashboard `missedOpportunityCount()` already expose a count-like status. | User-visible status exists partially; source read can decide whether a minimal dedicated panel/status is justified. |
| Tests | `MissedOpportunityServiceImplTest`, ReviewAggregate tests, dashboard/controller tests around review status. | Targeted future test scope can be identified without editing tests now. |

## 3. Candidate Comparison

| Candidate | Existing assets | User-visible value | Risk | Selection result |
|---|---|---|---|---|
| Missed Opportunity / Review Archive status | `MissedOpportunityController`, `MissedOpportunityServiceImpl`, `MissedOpportunityMapper`, `tm_missed_opportunity`, `ReviewAggregateServiceImpl`, dashboard missed count. | High. It explains whether the feedback/review archive has readable missed-valid context after the completed Review / Replay and RuleConfig slices. | Medium if it drifts into missed-opportunity generation, review result generation, or auto-correction. Low for source read only. | Selected for source read only. |
| RiskActionGuard read-only status | `DefaultRiskActionGuardDisplayAdapter`, `RiskActionGuardDisplayVO`, `DashboardController`, `riskActionGuardPlaceholderCard`, ExecutionPlan/BoundaryCandidate tests. | Medium-high. Safety boundary visibility is useful. | Medium-high. It overlaps action wording, Position Monitor, point/source-binding skeletons, and can be misread as an action decision. | Defer. |
| Position Monitor manual-input / monitor status | `PositionSyncService`, `MonitorController`, `MonitorServiceImpl`, `MonitorAlertMapper`, `tm_monitor_alert`. | Medium. Position visibility is useful. | High. Can imply close/reverse/open/moving-stop suggestions or provider/account writes. | Reject for now; Position Monitor expansion remains frozen by default. |
| Internal Push preview / recheck status | P302-P305 internal preview assets and dashboard section exist. | Medium. Preview status is user-visible. | High. Push, external channel, sendable message, and recheck execution semantics are blocked. | Reject. |
| Candidate preview / ranking status | Candidate attention/preview guard skeletons exist. | Medium. | High. Candidate generation/ranking/promotion semantics are explicitly blocked. | Reject. |
| Three AI / AI conflict status | `ai_role_results`, `AiConflictResolverService`, dashboard AI conflict shell. | Medium. | High. Provider orchestration, budget/cache/fallback, final arbiter, and final decision semantics are out of scope. | Reject. |
| SourceTrace / RuntimeKline / DataQuality / MultiTimeframe aggregate | Existing source trace/runtime kline display adapters and many source-binding skeletons exist. | Medium. | High duplicate-skeleton and point-adjacent risk; can reopen source-binding wrappers and entry/stop/TP/RR context. | Defer. |
| Account risk / system health / macro-news status | Some schema/docs exist. | Medium. | High. May require account/provider refresh, external API, scheduler/collector/API client trigger, or production-readiness wording. | Reject. |

## 4. Why Missed Opportunity / Review Archive Now

- It is the smallest remaining review-feedback slice after Review / Replay status and RuleConfig explainability are closed.
- It reuses existing service/controller/mapper/schema/dashboard read-model assets instead of creating a new DTO / Validator / Assembler / Orchestrator.
- It is user-visible through the existing missed-valid count and query/read paths.
- It can be source-read without changing Java, tests, dashboard business logic, schema/config/pom, or runtime behavior.
- It keeps RiskActionGuard, Position Monitor, Push, Candidate, Point, Three AI, account risk, and external provider tracks deferred until a narrower authorization exists.

## 5. Next Source Read Boundary

Next package name: `Source Read for Missed Opportunity / Review Archive status`.

Recommended branch: `missed-opportunity-review-archive-status-source-read`.

The source read must answer:

- Which owner path is canonical: `MissedOpportunityController` / `MissedOpportunityServiceImpl` / `MissedOpportunityMapper` / `tm_missed_opportunity`, `ReviewAggregateServiceImpl`, dashboard summary count, or some combination?
- Which existing fields can support a review-only status: count, latest row availability, query availability, reason-view parse status, archive linkage, and source-health/fail-closed state?
- Can the future slice remain read-only and avoid missed-opportunity generation/write behavior?
- Can it avoid replay execution and review result generation?
- Can it avoid Push, Candidate generation, Decision generation, Point, final direction, entry/stop/TP/RR, order/execution, auto-trading, external API refresh, scheduler/collector/API client trigger, schema/config/pom, and new DTO/Validator/Assembler?
- Is a future design package justified, or should existing Review / Replay status remain the canonical surface?

## 6. Forbidden Scope

This selection package and the next source-read package must not:

- modify Java business code;
- modify tests;
- modify dashboard business logic;
- modify schema/config/pom;
- implement an endpoint, dashboard panel, mapper, service, or query;
- generate missed opportunity records;
- generate review results;
- execute replay or recheck behavior;
- connect Push or external channel;
- generate Candidate;
- generate Decision;
- generate Point;
- generate final direction;
- generate entry / stop / TP / RR;
- connect order / execution / auto-trading;
- add DTO / Validator / Assembler / Orchestrator;
- continue P359 or P360;
- claim Production Wiring or capability-level increase.

## 7. Freeze Rule Compliance

- 是否创建新骨架: No
- 是否复用 Cursor-era / V1 资产: Yes
- 是否减少重复: Yes, by selecting existing Missed Opportunity / Review Archive owner assets before any new surface.
- 是否提升 capability level: No, selection only
- 是否接 service/runtime/dashboard/API: No, selection only; the next source read inventories existing service/API/dashboard owners.
- 是否符合 #830 审计建议: Yes
