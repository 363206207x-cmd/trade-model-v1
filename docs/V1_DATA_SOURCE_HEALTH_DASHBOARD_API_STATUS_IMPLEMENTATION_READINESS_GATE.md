# V1 Data Source Health Dashboard/API Status Implementation Readiness Gate

## 1. Current merged main

- Current merged main（当前已合并主线）: `62843de chore(workflow): normalize data source health readiness phase`
- Current module（当前模块）: `Data Source Health dashboard/API status`
- Current phase（当前阶段）: `Readiness Gate`
- Capability level（能力层级）: `REVIEW_ONLY_RUNTIME partial`
- Risk level（风险等级）: `A`

This package is readiness-gate only. It does not implement Java business code, tests, dashboard business logic, schema/config/pom, endpoint wiring, external refresh, scheduler, collector, API client calls, Push, Candidate generation, Decision generation, Point generation, order/execution, auto-trading, DTO, Validator, Assembler, Orchestrator, P359, or P360.

## 2. Source-read summary

`docs/V1_DATA_SOURCE_HEALTH_DASHBOARD_API_STATUS_SOURCE_READ.md` found that `DataSourceHealthDO` exists as a plain carrier, but it is not wired to mapper, service, schema, controller, API, dashboard DOM, or tests.

The usable source-health assets are distributed across completed review-only runtime slices:

| Source surface | Existing owner path | Existing status evidence |
|---|---|---|
| MarketQuote | `MarketController` / `/api/market/quote-status` | `sourceHealth` can be `OK`, `STALE`, `PARTIAL`, or `MISSING`. |
| Evidence / Score | `DashboardController` / `/api/dashboard/evidence-score-status` | `sourceHealth` can be `OK`, `PARTIAL`, `MISSING`, or `BLOCKED`. |
| DecisionResult | `DashboardController` / `/api/dashboard/decision-result-status` | `sourceHealth` can be `OK`, `PARTIAL`, `MISSING`, `UNKNOWN`, or blocked-like safe states. |
| ExecutionPlan / BoundaryCandidate | `DashboardController` / `/api/dashboard/execution-plan-boundary-status` | `sourceHealth` can be `OK`, `PARTIAL`, `MISSING`, `WATCH_ONLY`, or `BLOCKED`. |
| Review / Replay | `DashboardController` / `/api/dashboard/review-replay-result-status` | `sourceHealth` can be `OK`, `PARTIAL`, `MISSING`, or `BLOCKED`. |

Dashboard evidence exists in `dashboard.html` through the slice-local source-health DOM values:

- `marketQuoteSourceHealthValue`
- `evidenceScoreSourceHealthValue`
- `decisionSourceHealthValue`
- `executionPlanSourceHealthValue`
- `reviewReplaySourceHealthValue`

Test evidence exists in `MarketControllerTest` and `DashboardControllerTest`, which assert slice-local `sourceHealth` values and safety boundaries.

## 3. Design summary

`docs/V1_MINIMAL_REVIEW_ONLY_DATA_SOURCE_HEALTH_DASHBOARD_API_STATUS_RUNTIME_WIRING_DESIGN.md` selected a thin review-only aggregate over existing slice-local status surfaces.

The design explicitly rejects this owner path for the minimal slice:

```text
DataSourceHealthDO
  -> new mapper/service/schema/controller/dashboard
```

The design allows this owner path for the future minimal implementation:

```text
MarketQuote / Evidence-Score / DecisionResult / ExecutionPlan-BoundaryCandidate / Review-Replay status surfaces
  -> minimal Data Source Health rollup
  -> read-only dashboard/API status display
```

## 4. Existing assets found

| Asset | Found? | Reusable for minimal implementation? | Notes |
|---|---:|---:|---|
| `DataSourceHealthDO` | Yes | Inventory only | Must not become canonical runtime owner without separate schema/owner authorization. |
| Existing `sourceHealth` fields | Yes | Yes | Already present in five completed review-only runtime status surfaces. |
| `MarketController` quote status | Yes | Yes | Existing read-only MarketQuote source-health status. |
| `DashboardController` status paths | Yes | Yes | Existing Evidence/Score, DecisionResult, ExecutionPlan/BoundaryCandidate, Review/Replay status paths. |
| Dashboard DOM source-health slots | Yes | Yes | Existing slice-local display can be reused for a small aggregate/matrix insertion. |
| Controller/dashboard tests | Yes | Yes | Existing tests can be extended in the implementation package. |
| `tm_data_source_health` schema / mapper / service | No | No | Not required for minimal review-only implementation. |

## 5. Missing assets / gaps

- No dedicated Data Source Health aggregate endpoint exists.
- No dedicated Data Source Health dashboard panel exists.
- No `DataSourceHealthDO` mapper/service/schema owner exists.
- No aggregate tests exist.
- No aggregate status rollup exists yet.

These gaps do not block a minimal implementation because the next package can show a review-only aggregate/matrix over existing `sourceHealth` surfaces. They do block any implementation that would claim persisted data-source-health ownership.

## 6. Minimal implementation owner path

The allowed minimal owner path is:

```text
Existing slice-local review-only status owners
  -> DashboardController minimal aggregate status mapping
  -> dashboard.html minimal status panel / copy / DOM
  -> targeted DashboardControllerTest coverage
```

The implementation may use a minimal `Map` response or existing object shapes. It must not introduce a new DTO / Validator / Assembler / Orchestrator.

## 7. Allowed implementation scope

If this gate returns GO, the next implementation package may change only the smallest necessary files from these categories:

- Existing dashboard/status controller path, preferably `DashboardController`, to expose one read-only status endpoint such as `/api/dashboard/data-source-health-status`.
- `src/main/resources/templates/dashboard.html` for a minimal status panel / copy / DOM only.
- Targeted controller/dashboard tests for the new status endpoint/panel.
- Implementation report documentation.
- Source-of-truth documents.

The minimal endpoint/dashboard may show:

- overall Data Source Health status;
- scoped source names;
- per-source source-health values;
- partial/stale/missing/watch-only/blocked lists;
- `reviewOnly = true`;
- `notTradingSignal = true`;
- `notCandidateSignal = true`;
- `notDecisionGeneration = true`;
- `notPointSignal = true`;
- `notReplayExecution = true`;
- `notExecutable = true`;
- `displaySlotsAreCandidatePool = false`;
- `failClosed`.

## 8. Forbidden implementation scope

The next implementation must not:

- add schema/config/pom changes;
- add mapper/service/table ownership for `DataSourceHealthDO`;
- add DTO / Validator / Assembler / Orchestrator;
- trigger external API refresh, scheduler, collector, or API client calls;
- call MarketQuote real-fetch;
- call review save, replay execution, or recheck mutation paths;
- connect Push or external channels;
- generate Candidate;
- generate Decision;
- generate Point;
- output final direction;
- output entry / stop / TP / RR;
- output position size, leverage, order action, or executable action;
- connect order / execution / auto-trading;
- continue P359 or start P360;
- treat Display Slots as a candidate pool.

## 9. Fail-closed rules

The minimal implementation must fail closed when:

- any scoped status surface is unavailable;
- any required source-health field is missing;
- any source is stale, missing, blocked, unknown, contradictory, or unsupported;
- determining the answer would require external refresh, scheduler, collector, API client calls, replay execution, review result generation, Push, Candidate generation, Decision generation, Point generation, order/execution, or auto-trading;
- the rollup cannot prove a source is safe for review-only display.

Fail-closed means the display may be visible, but it must not imply downstream Candidate / Decision / Point / Push / Trading permission.

## 10. No external refresh guarantee

GO is allowed only because the implementation can read existing local status surfaces and static dashboard state. It must not:

- refresh MarketQuote from a provider;
- run schedulers or collectors;
- invoke an API client;
- run replay execution;
- regenerate review results;
- mutate upstream state.

If implementation discovers that the aggregate cannot be computed without one of those actions, it must stop and return fail-closed or NO-GO.

## 11. No trading / candidate / point / push guarantee

The Data Source Health slice remains review-only and display-only. It must not generate or authorize:

- Push;
- external channel;
- Candidate;
- Decision generation;
- Point;
- final direction;
- entry / stop / TP / RR;
- order / execution / auto-trading.

## 12. DTO / Validator / Assembler decision

Decision: **No new DTO / Validator / Assembler is allowed or required**.

Reason:

- Existing source-health values are already exposed as simple status fields.
- The minimal aggregate can use a `Map` / existing response shape.
- Adding a new DTO/Validator/Assembler family would repeat the duplicate-skeleton problem that #830 froze.

## 13. Readiness result

Result: **GO**.

GO rationale:

- Existing slice-local `sourceHealth` status surfaces are real and tested.
- A minimal aggregate endpoint/panel can be built as a read-only presentation layer.
- Missing aggregate persistence/schema does not block a fail-closed display.
- The implementation can avoid DTO / Validator / Assembler, schema/config/pom, external refresh, Push, Candidate, Decision generation, Point, and Trading.

NO-GO conditions for the next package:

- It requires new schema/config/pom.
- It requires new DTO / Validator / Assembler.
- It requires new DataSourceHealth mapper/service/table ownership.
- It must trigger external refresh, scheduler, collector, API client, replay execution, or review result generation.
- It must generate Candidate, Decision, Point, final direction, entry / stop / TP / RR, Push, order/execution, or auto-trading.

## 14. Next allowed action

Next allowed action: **Minimal Review-Only Data Source Health Dashboard/API Status Runtime Wiring Implementation**.

Risk level: **B** because the next package may touch controller/dashboard/tests, but only for minimal review-only endpoint/status display.

## 15. Capability movement

- Current level: `REVIEW_ONLY_RUNTIME partial`.
- This package raises capability level: No, readiness gate only.
- Data Source Health is not a completed runtime slice yet.
- The completed slice count remains 7.
- Future implementation target: keep `REVIEW_ONLY_RUNTIME partial`, not Production Wiring.
