# V1 Next Minimal Runtime Slice Selection After Review Archive Analytics / Missed Opportunity Aggregate Closure

## 1. Current Merged Main

- Current merged main: `1a83f6b docs(review): record review archive aggregate visual closure`
- Current capability level: `REVIEW_ONLY_RUNTIME partial`
- Completed review-only runtime partial slices: `15`
- Current task: select the 16th minimal review-only runtime slice.
- Capability movement: none. This package is selection-only and does not raise capability beyond `REVIEW_ONLY_RUNTIME partial`.

## 2. Completed Slices

The completed `REVIEW_ONLY_RUNTIME partial` slices are:

1. PositionSync + Dashboard review-only status
2. Watchlist + RuleConfig + Dashboard/API review-only status
3. MarketQuote freshness / fallback / dashboard API status
4. Evidence / Score review-only runtime status
5. DecisionResult review-only dashboard/API status
6. ExecutionPlan / BoundaryCandidate review-only runtime status
7. Review / Replay result status
8. Data Source Health dashboard/API status
9. RuleConfig runtime audit / rule explainability
10. Missed Opportunity / Review Archive status
11. RiskActionGuard read-only status
12. Alert fatigue / notification policy status
13. SourceTrace / RuntimeKline / DataQuality / MultiTimeframe aggregate review-only status
14. Paper Observation / Paper Trading Status review-only status
15. Review Archive Analytics / Missed Opportunity Aggregate Status

## 3. Source-Read-Lite Evidence

This selection pass only read existing owner-path evidence. It did not implement or change runtime behavior.

| Evidence area | Existing owner path / assets found | Selection impact |
|---|---|---|
| Runtime readiness / system guardrail | `SystemController` already exposes `/api/system/health`, `/api/system/position-sync-status`, and `/api/system/run-baseline`; `SystemHealthService` / `SystemHealthServiceImpl` return database and scheduler health; `RunBaselineServiceImpl` aggregates system health, position sync, performance, alerts, data quality, recheck counts, and hot reset summaries. | Strongest low-conflict source-read target because assets are already operational/status oriented and can be scoped as read-only guardrail status. |
| Dashboard system surface | `dashboard.html` already consumes `systemStatus`, `systemHealth`, sidebar/system KPI state, `Runtime readiness` workbench text, and `position-sync-status` fetches. | A future source read can inventory current DOM/copy gaps without adding dashboard behavior in this package. |
| Runtime metric read path | `RuntimeMetricService` records in-process metric snapshots used by `RunBaselineVO.PerformanceSummary`. | Useful as read-only metadata only; must not become scheduler, collector, external refresh, or performance automation. |
| Account risk | `AccountRiskSnapshotMapper`, account-risk JSON in execution/review/push aggregate paths, and Push/Recheck references exist. | Too action/exposure-adjacent for the next source read; defer until a narrower read-only account-risk boundary is proven. |
| Macro-news / event calendar | Macro/news/event references are mostly historical docs or conflict metadata; cohesive runtime owner path is weaker than system readiness. | Defer because any future macro/news status must first prove no external API refresh, news collection, or event generation. |
| Three AI / AI conflict | `DecisionEngineService`, AI conflict fields, and DecisionResult display history exist. | Defer because it can drift into provider orchestration, arbitration, final direction, or decision generation. |
| Push / notification / recheck | `PushRecheckService`, scheduler, replay summary, and internal push preview history exist. | Defer because they touch recheck execution, scheduler, Push send, and external-channel boundaries. |
| Candidate preview / ranking | Candidate preview/attention/ranking skeleton history exists. | Defer because Candidate generation/ranking and duplicate skeleton revival remain frozen boundaries. |
| Position Monitor | PositionSync and legacy monitor assets exist. | Defer because monitor language is close to real position monitoring, close/reverse/open/move-stop guidance, and Position Monitor execution. |

## 4. Candidate Next Slices Considered

| Candidate | Existing owner path / assets | Minimal closure fit | Boundary risk | Decision |
|---|---|---|---|---|
| Runtime readiness / system guardrail status | `SystemController`, `SystemHealthService`, `RunBaselineService`, `RunBaselineVO`, `RuntimeMetricService`, dashboard `systemStatus/systemHealth` surfaces, and existing PositionSync system status endpoint. | High for source read: existing status/read paths can be inventoried without implementation, new owners, or runtime behavior. | Manageable if the next package explicitly treats "readiness" as review-only operational guardrail status, not executable Point/order/trading readiness. | Select. |
| Account risk / system health status | SystemHealth assets exist, and account-risk snapshots exist through Push/Recheck/ExecutionPlan contexts. | Medium. System health is safe, but account risk is mixed with exposure/action paths. | Account risk may imply position sizing, reduce/close/stop, or Push/Recheck gating. | Defer; split out system readiness first. |
| Macro-news / event calendar status | Macro/news/event references exist mostly in docs and conflict metadata. | Low-medium. | External API refresh, news collection, event generation, and decision influence risks. | Defer. |
| Three AI / AI conflict status | DecisionResult/DecisionEngine AI conflict fields exist. | Medium. | Provider orchestration, arbitration, final decision, final direction, entry/stop/TP/RR drift. | Defer. |
| Internal Push preview / notification preview status | Internal push preview and notification policy history exist. | Medium. | Push send and external channel drift. | Defer. |
| Recheck status / recheck preview | PushRecheck service, scheduler, logs, and replay summary exist. | Low for the next slice. | Recheck execution, replay, scheduler, mutation, and Push adjacency. | Defer. |
| Candidate preview / ranking status | Candidate attention / preview guard / ranking-era history exists. | Low. | Candidate generation, ranking, score-to-candidate revival, duplicate skeleton expansion. | Defer. |
| Position Monitor manual-input / monitor status | PositionSync and monitor foundations exist. | Medium. | Real position monitoring, close/reverse/open/move-stop, execution advice, Position Monitor execution. | Defer. |
| Existing dashboard/system placeholder with review-only owner path | Dashboard system/KPI/health surfaces exist and overlap with `SystemController`/`RunBaselineService`. | High, as the concrete owner path for the selected slice. | Same readiness wording risk; bounded by source read only. | Fold into selected runtime readiness / system guardrail status. |
| Other smaller safer source-discovered slice | No smaller safer unclosed slice was found. | N/A | N/A | Not selected. |

## 5. Selected Next Slice

Selected next slice:

`Runtime readiness / system guardrail status`

Chinese label:

`运行时就绪 / 系统防护栏状态`

Next branch:

`runtime-readiness-system-guardrail-status-source-read`

Next allowed action:

`Source Read for Runtime readiness / system guardrail status`

## 6. Why This Slice Now

This is the safest next source-read target because it can start from existing operational status owners instead of creating a new business domain:

- `/api/system/health` already exposes system health read status.
- `/api/system/run-baseline` already aggregates read-only operational summaries.
- `SystemHealthServiceImpl` probes database and scheduler health and returns fail-closed-style `ERROR`, `DOWN`, `NO_RECENT_ACTIVITY`, or `STALE` statuses rather than triggering recovery.
- `RunBaselineServiceImpl` already treats alerts, data quality, recheck counts, hot reset, and runtime metrics as snapshots.
- `dashboard.html` already has system health/status display surfaces and can be source-read for DOM/copy gaps later.

This selection also reduces risk compared with the other candidates. It does not require Push send, external channels, recheck/replay execution, Candidate generation, Decision generation, Point generation, final direction, entry/stop/TP/RR, order/execution, auto-trading, schema/config/pom, or new DTO / Validator / Assembler / Orchestrator work.

## 7. Why Not The Others

- Account risk / system health status is deferred as a combined slice because account risk is exposure/action-adjacent; the selected slice narrows to runtime readiness / system guardrail first.
- Macro-news / event calendar status is deferred because source evidence is weaker and external API refresh / news collection / event generation boundaries need a separate source read.
- Three AI / AI conflict status is deferred because it can drift into provider orchestration, arbitration, final direction, and decision generation.
- Internal Push preview / notification preview status is deferred because Push send and external channel remain sensitive boundaries.
- Recheck status / recheck preview is deferred because existing assets include scheduler, replay, and execution methods.
- Candidate preview / ranking status is deferred because Candidate generation/ranking and duplicate skeleton revival remain frozen.
- Position Monitor manual-input / monitor status is deferred because it can be read as real position monitoring, close/reverse/open/move-stop guidance, or Position Monitor execution.
- Other placeholders were not smaller or safer than the selected system/readiness owner path.

## 8. Source Read Task Definition

Next task:

`Source Read for Runtime readiness / system guardrail status`

Branch:

`runtime-readiness-system-guardrail-status-source-read`

Risk:

`A`

Required owner-path reads:

- `SystemController`
- `SystemHealthService` / `SystemHealthServiceImpl`
- `RunBaselineService` / `RunBaselineServiceImpl`
- `RunBaselineVO`
- `RuntimeMetricService`
- `DashboardController` summary path and `DashboardSummaryResponseVO`
- `dashboard.html` system status / system health / runtime readiness / guardrail surfaces
- targeted controller/dashboard/system health/run-baseline tests
- existing docs for PositionSync, Data Source Health, Alert fatigue, SourceTrace/DataQuality, and runtime baseline/guardrail boundaries

Required source-read questions:

1. Can runtime readiness / system guardrail status reuse existing `/api/system/health`, `/api/system/run-baseline`, and dashboard summary owner paths?
2. Which fields are safe as read-only source: database status, scheduler status, PositionSync availability, runtime metric sample boundary, alert summary, data quality summary, recheck count summary, and hot reset summary?
3. Which fields must be treated as context-only because they touch recheck, Push, account risk, or execution-adjacent summaries?
4. Can missing system health, database probe failure, stale scheduler, missing PositionSync status, empty runtime samples, and partial baseline data fail closed or stay partial review-only?
5. Does any path trigger scheduler/collector/API-client refresh, external refresh, recovery actions, Push send, recheck execution, Candidate generation, Decision generation, Point generation, entry/stop/TP/RR, order/execution, or auto-trading?
6. Would future design require a new DTO / Validator / Assembler / Orchestrator, schema/config/pom, or a new service/domain/mapper/repository owner family?
7. How should the term "readiness" be constrained so it cannot be mistaken for executable readiness?

## 9. Forbidden Scope

This selection package and the next source-read package must not:

- modify Java business code;
- modify tests;
- modify dashboard business logic;
- modify schema/config/pom;
- add endpoint or panel behavior;
- add DTO / Validator / Assembler / Orchestrator;
- add service/domain/mapper/repository ownership families;
- trigger external API refresh, scheduler, collector, or API-client refresh;
- trigger recovery, repair, replay, recheck, or push dispatch;
- send Push or use external channels;
- generate Candidate, Decision, Point, final direction, entry, stop, TP, RR, order action, execution action, or auto-trading action;
- execute Position Monitor behavior;
- generate missed opportunity or review results;
- execute paper order, simulated execution, or paper PnL;
- continue P359 or P360.

## 10. Risk And Capability

- Risk level: A
- Allowed next action: source-read docs and source-of-truth updates only
- Capability movement: none
- Current capability level remains: `REVIEW_ONLY_RUNTIME partial`

Freeze-rule compliance:

- 是否创建新骨架: No
- 是否复用 Cursor-era / V1 资产: Yes
- 是否减少重复: Yes, by selecting an existing `SystemController` / dashboard system status owner path instead of creating a new wrapper family.
- 是否提升 capability level: No, selection only.
- 是否接 service/runtime/dashboard/API: No, selection only; future source read will inventory existing service/runtime/dashboard/API assets.
- 是否符合 #830 审计建议: Yes
