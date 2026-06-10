# V1 Data Source Health Dashboard/API Status Source Read

## 1. Executive Summary

This task is source-read only. It does not implement Java, tests, dashboard logic, schema/config/pom changes, endpoint changes, Push, Candidate, Decision generation, Point, replay execution, review result generation, order/execution, or auto-trading.

Conclusion: `Data Source Health dashboard/API status` can move to a minimal review-only runtime design package, but it must not go directly to implementation.

Findings:

- `DataSourceHealthDO` exists as a plain Java carrier with source status, delay, missing-rate, anomaly, quality-score, snapshot, trace, rule-version, audit, deletion, and version fields.
- No mapper, service, controller, schema table, dashboard DOM, or test usage for `DataSourceHealthDO` was found.
- No dedicated `data-source-health` dashboard/API route or panel was found.
- Existing user-visible source-health signals are distributed across completed review-only slices:
  - MarketQuote: `/api/market/quote-status`
  - Evidence / Score: `/api/dashboard/evidence-score-status`
  - DecisionResult: `/api/dashboard/decision-result-status`
  - ExecutionPlan / BoundaryCandidate: `/api/dashboard/execution-plan-boundary-status`
  - Review / Replay: `/api/dashboard/review-replay-result-status`
- `dashboard.html` already resolves and renders local `sourceHealth` fields for those slice-local panels.
- Existing `DashboardControllerTest` and `MarketControllerTest` already assert `sourceHealth` values and forbidden executable semantics for the slice-local status surfaces.
- The next step can be design because the source signals are real and dashboard-visible. The design must choose whether to aggregate existing slice-local status outputs or keep source health distributed.

Direct implementation is not safe yet because a canonical aggregate owner path is not established. The future design must avoid creating a new DTO / Validator / Assembler / Orchestrator family and must avoid pretending `DataSourceHealthDO` is already a wired persistence/API owner.

Next step: **GO: Design for Data Source Health dashboard/API status**.

## 2. Source Read Inventory

| Area | Files/classes found | Existing behavior | Runtime/API connection | Dashboard connection | Gap |
|---|---|---|---|---|---|
| DataSourceHealth carrier | `src/main/java/org/example/trademodel/entity/DataSourceHealthDO.java` | Plain fields for source name/status, delay seconds, missing rate, anomaly flag, quality score, snapshot time, trace/rule metadata, audit, delete/version. | None found. | None found. | Orphan carrier; no mapper/service/controller/schema/dashboard/test usage found. |
| MarketQuote source health | `MarketController`, `MarketControllerTest` | `/api/market/quote-status` maps quote source to `OK`, `STALE`, `PARTIAL`, or `MISSING`, with review-only and not-trading flags. | Yes. Runtime status endpoint exists. | Yes through `dashboard.html` MarketQuote status fetch/display. | Slice-local only; not an aggregate data-source-health owner. |
| Evidence / Score source health | `DashboardController`, `DashboardControllerTest` | `/api/dashboard/evidence-score-status` maps Evidence/Score availability and source trace to `OK`, `PARTIAL`, `MISSING`, or `BLOCKED`. | Yes. Runtime status endpoint exists. | Yes through `dashboard.html` Evidence / Score status display. | Slice-local only; depends on existing analysis context. |
| DecisionResult source health | `DashboardController`, `DashboardControllerTest` | `/api/dashboard/decision-result-status` maps DecisionResult read model, source trace, and AI role availability to `OK`, `PARTIAL`, `MISSING`, or `UNKNOWN`. | Yes. Runtime status endpoint exists. | Yes through `dashboard.html` DecisionResult status display. | Slice-local only; not new Decision generation. |
| ExecutionPlan / BoundaryCandidate source health | `DashboardController`, `DashboardControllerTest` | `/api/dashboard/execution-plan-boundary-status` maps plan boundary, source trace, risk guard, and execution plan status to `OK`, `PARTIAL`, `MISSING`, `WATCH_ONLY`, or `BLOCKED`. | Yes. Runtime status endpoint exists. | Yes through `dashboard.html` ExecutionPlan / BoundaryCandidate status display. | Slice-local only; must not expose entry/stop/TP/RR as executable values. |
| Review / Replay source health | `DashboardController`, `DashboardControllerTest` | `/api/dashboard/review-replay-result-status` maps review result, aggregate, replay summary, and source trace to `OK`, `PARTIAL`, `MISSING`, or `BLOCKED`. | Yes. Runtime status endpoint exists. | Yes through `dashboard.html` Review / Replay status display. | Slice-local only; replay execution remains forbidden. |
| Dashboard local defaults | `src/main/resources/templates/dashboard.html` | Initializes local status objects with conservative `sourceHealth` defaults and renders source-health labels per completed slice. | Display-only. | Yes. | No aggregate panel or matrix exists. |
| Tests | `MarketControllerTest`, `DashboardControllerTest` | Assert source-health values and safe flags for existing endpoints/panels. | Yes for existing local status endpoints. | Yes for template/static mapping. | No aggregate Data Source Health status tests yet. |
| Schema/resources | `schema.sql`, resource search | No `tm_data_source_health`, `data_source_health`, or DataSourceHealth mapper/table usage found. | None. | None. | Future design must not require schema changes in a minimal review-only slice. |

## 3. Existing Runtime Flow

Existing source-health flow is distributed:

```text
MarketQuoteClient / MarketQuote status
  -> MarketController /api/market/quote-status
  -> dashboard.html marketQuoteSourceHealthValue

Evidence / Score owner path
  -> DashboardController /api/dashboard/evidence-score-status
  -> dashboard.html evidenceScoreSourceHealthValue

DecisionResult read model
  -> DashboardController /api/dashboard/decision-result-status
  -> dashboard.html decisionSourceHealthValue

Dashboard detail / PlanBoundary / ExecutionPlan adapters
  -> DashboardController /api/dashboard/execution-plan-boundary-status
  -> dashboard.html executionPlanSourceHealthValue

ReviewResult / ReviewAggregate / replay summary read path
  -> DashboardController /api/dashboard/review-replay-result-status
  -> dashboard.html reviewReplaySourceHealthValue
```

No aggregate flow was found:

```text
DataSourceHealthDO
  -> mapper/service/controller/API/dashboard
  [missing]
```

## 4. Readiness Assessment

Can read Data Source Health as an aggregate: **No, not yet**. `DataSourceHealthDO` exists but is not wired to persistence, service, controller, API, dashboard, or tests.

Can read source health locally by completed slice: **Yes**. MarketQuote, Evidence / Score, DecisionResult, ExecutionPlan / BoundaryCandidate, and Review / Replay all have local `sourceHealth` values in review-only runtime status endpoints and dashboard rendering.

Can form a minimal review-only runtime design: **Yes**. A design package can define an aggregate status over existing local endpoint/status outputs, or deliberately keep the source-health model distributed if aggregation would duplicate completed panels.

Can implement directly now: **No**. The owner path decision is not made, and a direct implementation would risk creating a duplicate source-health wrapper or pretending `DataSourceHealthDO` is canonical runtime ownership.

## 5. Minimal Design Questions

The next design package must answer:

- Is the minimal owner path an aggregate in `DashboardController`, or should Data Source Health remain distributed across existing slice-local endpoints?
- If aggregate, which existing local statuses are in scope: MarketQuote, Evidence / Score, DecisionResult, ExecutionPlan / BoundaryCandidate, Review / Replay?
- Can the aggregate be returned as a minimal Map/status surface without new DTO / Validator / Assembler / Orchestrator?
- What status rollup is allowed: `OK`, `PARTIAL`, `MISSING`, `STALE`, `WATCH_ONLY`, `BLOCKED`, fail-closed?
- How should missing upstream context fail closed without triggering fetch, generation, replay, push, or trading behavior?
- Should `DataSourceHealthDO` remain an inventory-only carrier for now, or can it be named as a future non-runtime candidate only after explicit schema/owner authorization?

## 6. Boundary Confirmation

Future Data Source Health status must not:

- Modify Java business code in this source-read package.
- Modify tests in this source-read package.
- Modify dashboard business logic in this source-read package.
- Modify schema/config/pom.
- Create DTO / Validator / Assembler / Orchestrator.
- Call `/api/market/real-fetch`.
- Call Review save paths.
- Call replay execution or recheck mutation paths.
- Generate Candidate.
- Generate a new Decision.
- Generate Point.
- Output final direction, entry, stop, TP, RR, position size, leverage, or order action.
- Send Push or connect external channels.
- Connect order / execution / auto-trading.
- Treat Display Slots as a candidate pool.
- Continue P359 or start P360.

## 7. Candidate Slice Comparison / Go-NoGo

Decision: **A. GO: Design for Data Source Health dashboard/API status**.

Why GO:

- Existing `sourceHealth` values are real and already visible in five completed review-only runtime slices.
- Existing controller/dashboard/tests prove the local source-health status path.
- A design package can reduce duplicate risk by selecting an ownership boundary before any aggregate implementation.
- No new skeleton family is required for the next step.

Why not implementation:

- `DataSourceHealthDO` is not wired as a canonical owner.
- No aggregate endpoint/panel exists.
- Rollup semantics are not designed.
- A direct implementation could duplicate MarketQuote, Evidence / Score, DecisionResult, ExecutionPlan / BoundaryCandidate, and Review / Replay status panels.

Owner path candidate for design:

```text
Existing slice-local review-only status endpoints
  -> minimal Data Source Health status mapping in Dashboard/API design
  -> dashboard aggregate or explicit distributed-health decision
```

Rejected owner path for now:

```text
DataSourceHealthDO
  -> new mapper/service/schema/controller/dashboard
```

That rejected path would be a new owner surface and is not authorized by this source-read package.

## 8. Rejected Expansion

Not doing now:

- Data Source Health schema/table implementation.
- New health-service runtime.
- New dashboard aggregate panel implementation.
- Java/test/dashboard code changes.
- MarketQuote fetch trigger wiring.
- Review save/replay execution.
- Push / external channel.
- Candidate generation.
- Decision generation.
- Point generation.
- final direction / entry / stop / TP / RR.
- order / execution / auto-trading.
- P359 / P360.

## 9. Freeze Rule Compliance

- 是否创建新骨架: No
- 是否复用 Cursor-era / V1 资产: Yes
- 是否减少重复: Yes, by selecting a source-read conclusion before any aggregate source-health implementation
- 是否提升 capability level: No, source read only
- 是否接 service/runtime/dashboard/API: No, source read only; existing service/runtime/dashboard/API paths are inventoried
- 是否符合 #830 审计建议: Yes

