# V1 Next Minimal Runtime Slice Selection After SourceTrace / RuntimeKline / DataQuality / MultiTimeframe Closure

## 1. Current Merged Main

- Current merged main: `f579b24 docs(runtime): record source trace data quality visual closure`
- Current capability level: `REVIEW_ONLY_RUNTIME partial`
- Completed review-only runtime partial slices: `13`
- Current task: select the 14th minimal review-only runtime slice.
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

## 3. Candidate Next Slices Considered

| Candidate | Existing owner path | Minimal closure fit | Boundary risk | Decision |
|---|---|---|---|---|
| Position Monitor manual-input / monitor status | Legacy PositionSync / monitor foundations and dashboard references exist. | Medium. The read surface exists, but the name and assets are close to monitor actions. | High risk of being misread as close / reverse / open / execution guidance or Position Monitor execution. | Defer. |
| Internal Push preview / recheck status | Dashboard placeholder and review page Push / Recheck assets exist. | Medium. There is a visible surface, but the owner path touches recheck and notification concerns. | High risk of Push send, external channel, scheduler, or recheck execution drift. | Defer. |
| Candidate preview / ranking status | Candidate and ranking-era skeletons exist. | Low for this phase because Candidate generation remains a frozen boundary. | High risk of Candidate generation / ranking semantics and source-owned candidate skeleton revival. | Defer. |
| Three AI / AI conflict status | DecisionResult and dashboard AI conflict fields exist. | Medium. Existing display fields exist. | Medium-high risk of drifting into provider orchestration, final decision, or final bias language. | Defer. |
| Account risk / system health / macro-news status | SystemHealthService and several docs exist; account-risk links appear in Push/Recheck flows. | Medium-low. The scope is broad and mixed. | Account risk is close to Push/Recheck, macro-news owner path is weak, and system health is broader than one small slice. | Defer. |
| Paper Observation / paper trading status | `PaperObservationDisplayAdapter`, `DefaultPaperObservationDisplayAdapter`, `DashboardDetailResponseVO.PaperObservationDisplayVO`, dashboard detail path, dashboard display, and tests exist. | High. Existing assets already expose review-only paper-observation state and not-real-position / not-trade-instruction semantics. | Manageable if the next source read explicitly forbids paper trading execution, backtest execution, generated results, orders, and Position Monitor execution. | Select. |
| Runtime readiness / system guardrail status | Readiness skeletons and docs exist. | Medium. | High risk of slipping toward Point readiness, entry / stop / TP / RR readiness, and executable semantics. | Defer. |
| Review archive analytics / missed opportunity aggregate status | Missed Opportunity / Review Archive owner path exists. | Medium. | This would be a second pass over a completed slice rather than a new minimal slice. | Defer. |
| Other source-discovered candidates | No smaller safer candidate was found than Paper Observation / paper trading status. | N/A | N/A | Not selected. |

## 4. Selected Next Slice

Selected next slice:

`Paper Observation / Paper Trading Status review-only status`

Chinese label:

`纸面观察 / 纸面交易状态只读状态`

Next branch:

`paper-observation-paper-trading-status-source-read`

Next allowed action:

`Source Read for Paper Observation / Paper Trading Status review-only status`

## 5. Why This Slice Now

This slice is the best next minimal target because it already has a concrete dashboard-detail owner path:

- `PaperObservationDisplayAdapter`
- `DefaultPaperObservationDisplayAdapter`
- `DashboardDetailResponseVO.PaperObservationDisplayVO`
- `DashboardController` dashboard detail response path
- `dashboard.html` paper observation display surface
- existing targeted tests around paper observation display and dashboard detail response

The existing owner path already carries safety semantics that fit a review-only runtime slice:

- paper observation availability is display-only;
- default backend-pending behavior is fail-closed / unavailable;
- not-real-position and not-trade-instruction copy already separates display from execution;
- missed-opportunity linkage is context only;
- no order / execution / auto-trading path is required.

The next source read can therefore answer whether the existing display owner path can become a minimal status slice without creating new DTO / Validator / Assembler / Orchestrator, schema/config/pom, paper trading execution, Position Monitor execution, Candidate generation, Decision generation, Point generation, or trading behavior.

## 6. Why Not The Others

- Position Monitor manual-input / monitor status is valuable, but should wait because its language and existing assets are close to close / reverse / open / monitor execution semantics.
- Internal Push preview / recheck status should wait because it is adjacent to Push send, external channel, scheduler, and recheck execution boundaries.
- Candidate preview / ranking status should wait because Candidate generation / ranking remains a frozen boundary.
- Three AI / AI conflict status should wait because it can drift into provider orchestration or final decision semantics.
- Account risk / system health / macro-news status should wait because the candidate is broad and mixed, with weak macro/news owner evidence and account-risk links to Push/Recheck flows.
- Runtime readiness / system guardrail status should wait because readiness can be confused with executable Point / entry / stop / TP / RR readiness.
- Review archive analytics / missed opportunity aggregate status should wait because Missed Opportunity / Review Archive is already a completed slice; analytics would be an expansion, not the smallest new slice.

## 7. Source Read Task Definition

Next source-read package must inventory only existing assets and must not implement anything.

Required owner-path reads:

- `PaperObservationDisplayAdapter`
- `DefaultPaperObservationDisplayAdapter`
- `DashboardDetailResponseVO.PaperObservationDisplayVO`
- `DashboardController`
- `dashboard.html`
- existing paper observation display tests
- existing dashboard detail response tests
- historical paper observation / paper trading docs
- any existing paper observation DB, mapper, service, or review linkage if present

Required source-read questions:

1. Is the current paper observation surface read-only?
2. Does it have fail-closed / backend-pending behavior?
3. Does it explicitly say not real position and not trade instruction?
4. Is there any existing paper trading execution, paper backtest execution, generated paper result, order, or Position Monitor execution path?
5. Can a later status slice reuse the existing dashboard detail owner path?
6. Would a later implementation require new DTO / Validator / Assembler / Orchestrator, schema/config/pom, or service ownership?
7. What dashboard panel / DOM / copy gaps remain?
8. What forbidden semantics must future design block?

## 8. Forbidden Scope

The next package and this selection package must not:

- modify Java business code;
- modify tests;
- modify dashboard business logic;
- modify schema/config/pom;
- create new DTO / Validator / Assembler / Orchestrator;
- trigger paper trading execution;
- trigger paper backtest execution;
- generate paper trading results;
- execute Position Monitor behavior;
- trigger Push send or external channel;
- trigger recheck execution;
- generate Candidate;
- generate Decision;
- generate Point;
- generate final direction;
- generate entry / stop / TP / RR;
- trigger order / execution / auto-trading;
- continue P359 / P360.

## 9. Risk Level

Risk level: `A`

Reason: this package is selection-only and changes only docs / source-of-truth handoff files.

The next source-read package is also expected to be A-risk if it remains docs-only and reads existing owner paths.

## 10. Capability Movement

Capability movement: none.

The project remains `REVIEW_ONLY_RUNTIME partial`. This package selects the next candidate slice only; it does not implement Paper Observation / paper trading status, does not close a new runtime slice, and does not authorize production wiring.
