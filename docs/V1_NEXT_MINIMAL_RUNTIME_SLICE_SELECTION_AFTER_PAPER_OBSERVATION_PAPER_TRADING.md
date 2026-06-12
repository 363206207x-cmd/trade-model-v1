# V1 Next Minimal Runtime Slice Selection After Paper Observation / Paper Trading Closure

## 1. Current Merged Main

- Current merged main: `8a4e594 docs(paper): record paper observation visual closure`
- Current capability level: `REVIEW_ONLY_RUNTIME partial`
- Completed review-only runtime partial slices: `14`
- Current task: select the 15th minimal review-only runtime slice.
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

## 3. Candidate Next Slices Considered

| Candidate | Existing owner path / assets | Minimal closure fit | Boundary risk | Decision |
|---|---|---|---|---|
| Position Monitor manual-input / monitor status | PositionSync / position-monitor foundations and dashboard references exist. | Medium, but still monitor/action adjacent. | High risk of close / reverse / open / move-stop / Position Monitor execution semantics. | Defer. |
| Internal Push preview / notification preview status | Internal push preview assembler/tests and dashboard display history exist. | Medium. | High risk of Push send / external channel drift. | Defer. |
| Recheck status / recheck preview | PushRecheck controller/service/scheduler/log assets exist. | Low for this phase because recheck has mutation and scheduler paths. | High risk of recheck execution, replay, scheduler, or external notification flow. | Defer. |
| Candidate preview / ranking status | Candidate attention / preview guard skeletons and tests exist. | Low. | Directly adjacent to Candidate generation / ranking and frozen duplicate skeleton history. | Defer. |
| Three AI / AI conflict status | DecisionResult `ai_role_results`, dashboard AI role fields, and AI role enum exist. | Medium. | Medium-high risk of provider orchestration, arbitration, final decision, and final direction language. | Defer. |
| Account risk / system health / macro-news status | `SystemHealthService`, `RunBaselineVO`, account-risk snapshot table, and macro evidence/score traces exist. | Medium-low. | Mixed scope; account risk is exposure/action adjacent and macro-news can imply external refresh. | Defer. |
| Runtime readiness / system guardrail status | Readiness and guardrail docs/skeletons exist. | Medium-low. | Can be misread as executable readiness for Point, entry / stop / TP / RR, or order flow. | Defer. |
| Review archive analytics / missed opportunity aggregate status | Missed Opportunity / Review Archive owner path is already read-only; `ReviewAggregateService` and dashboard count assets exist. | High for source read: existing read/aggregate assets can be inventoried without implementation. | Manageable if kept as analytics/status only and explicitly forbids missed-opportunity generation/write behavior, review result generation, replay/recheck, Push, Candidate, Decision generation, Point, and trading. | Select. |
| Paper Observation extension only if strictly status-only | Paper Observation just closed as the 14th slice. | Low for a new slice because it would be immediate expansion. | Risk of paper order / simulated execution / paper PnL confusion if extended too soon. | Defer. |
| Other source-discovered candidates | No smaller safer candidate was found in this selection pass. | N/A | N/A | Not selected. |

## 4. Selected Next Slice

Selected next slice:

`Review Archive Analytics / Missed Opportunity Aggregate Status`

Chinese label:

`复盘归档分析 / 错失机会聚合状态`

Next branch:

`review-archive-analytics-missed-opportunity-aggregate-status-source-read`

Next allowed action:

`Source Read for Review Archive Analytics / Missed Opportunity Aggregate Status`

## 5. Why This Slice Now

This is the lowest-conflict next target because it is a source-read-only pass over existing read/aggregate assets rather than a new execution-adjacent surface.

The existing owner evidence is concrete:

- `MissedOpportunityController` and `MissedOpportunityService` already provide read/query/count owner paths.
- `MissedOpportunityMapper` and `tm_missed_opportunity` already anchor persisted missed-opportunity rows.
- `ReviewAggregateService` already links review archive context to missed sections.
- Dashboard summary already has missed-count style visibility.
- Existing Missed Opportunity / Review Archive status work already proved that generation/write behavior, review result generation, replay/recheck, Push, Candidate, Decision generation, Point, and trading can be kept outside the read surface.

The selected source read must treat this as an analytics/status inventory only. It must not reopen the completed Missed Opportunity / Review Archive status implementation, and it must not create a second implementation path in this package.

## 6. Why Not The Others

- Position Monitor manual-input / monitor status is deferred because it can slide into real position monitoring, close / reverse / open guidance, move-stop language, or Position Monitor execution.
- Internal Push preview / notification preview status is deferred because any notification preview must first prove it cannot become Push send or external channel behavior.
- Recheck status / recheck preview is deferred because existing `PushRecheck` assets include execution, replay, scheduler, and mutation paths.
- Candidate preview / ranking status is deferred because Candidate generation and ranking remain frozen boundaries.
- Three AI / AI conflict status is deferred because it can become provider orchestration, arbitration, final direction, or decision generation.
- Account risk / system health / macro-news status is deferred because the scope is broad and mixed; account risk is exposure/action-adjacent, while macro/news can imply external refresh.
- Runtime readiness / system guardrail status is deferred because "readiness" can be mistaken for executable readiness.
- Paper Observation extension is deferred because Paper Observation just closed and extension would be a same-slice expansion, not the smallest new slice.

## 7. Source Read Task Definition

Next task:

`Source Read for Review Archive Analytics / Missed Opportunity Aggregate Status`

Branch:

`review-archive-analytics-missed-opportunity-aggregate-status-source-read`

Required source-read inventory:

- `MissedOpportunityController`
- `MissedOpportunityService` / `MissedOpportunityServiceImpl`
- `MissedOpportunityMapper`
- `MissedOpportunityDO`
- `tm_missed_opportunity`
- `MissedReasonViewParser`
- `ReviewAggregateService` / `ReviewAggregateServiceImpl`
- review page missed section / archive analytics display
- dashboard missed-count or archive-count status
- existing Missed Opportunity / Review Archive tests and docs

Required source-read questions:

1. Can review archive analytics / missed opportunity aggregate status reuse only existing read/query/count owner paths?
2. Which fields can be treated as read-only aggregate/status sources?
3. Is there an existing dashboard/API surface, or only a candidate for future minimal status panel?
4. Can missing aggregate, empty archive, parse failure, and partial linkage fail closed or stay review-only partial?
5. What generation/write paths exist and must remain forbidden?
6. Does any path trigger replay/recheck execution, review result generation, Push, Candidate, Decision generation, Point, or trading?
7. Would future design require new DTO / Validator / Assembler / Orchestrator, schema/config/pom, or a new owner family?

## 8. Forbidden Scope

This package and the next source-read package must not:

- modify Java business code;
- modify tests;
- modify dashboard business logic;
- modify schema/config/pom;
- add endpoint or panel behavior;
- add DTO / Validator / Assembler / Orchestrator;
- add service/domain/mapper/repository ownership families;
- generate missed-opportunity records;
- call missed-opportunity write paths;
- generate review results;
- execute replay or recheck;
- send Push or use external channels;
- generate Candidate, Decision, Point, final direction, entry, stop, TP, RR, order action, execution action, or auto-trading action;
- execute Position Monitor behavior;
- execute paper order, simulated execution, or paper PnL;
- continue P359 or P360.

## 9. Risk And Capability

- Risk level: A
- Allowed next action: source-read docs and source-of-truth updates only
- Capability movement: none
- Current capability level remains: `REVIEW_ONLY_RUNTIME partial`
