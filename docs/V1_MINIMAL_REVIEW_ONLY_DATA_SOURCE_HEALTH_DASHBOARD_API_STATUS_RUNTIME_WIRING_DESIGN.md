# V1 Minimal Review-Only Data Source Health Dashboard/API Status Runtime Wiring Design

## 1. Executive Summary

This package is design only. It does not implement Java, tests, dashboard logic, schema/config/pom changes, endpoint changes, replay execution, review result generation, Push, Candidate, Decision generation, Point, order/execution, or auto-trading.

Minimal future target: expose a review-only Data Source Health status that helps the user see whether the already completed runtime slices have readable, stale, partial, missing, watch-only, or blocked source-health signals.

Design conclusion: the minimal owner path should be a thin presentation aggregate over existing slice-local review-only status surfaces. It must not make `DataSourceHealthDO` the canonical runtime owner yet.

Owner path:

```text
Existing slice-local review-only status surfaces
  -> MarketQuote /api/market/quote-status
  -> Evidence / Score /api/dashboard/evidence-score-status
  -> DecisionResult /api/dashboard/decision-result-status
  -> ExecutionPlan / BoundaryCandidate /api/dashboard/execution-plan-boundary-status
  -> Review / Replay /api/dashboard/review-replay-result-status
  -> future minimal Data Source Health status mapping in DashboardController/dashboard display
```

Rejected owner path for the minimal slice:

```text
DataSourceHealthDO
  -> new mapper/service/schema/controller/dashboard
```

`DataSourceHealthDO` remains an inventory-only carrier for now. It may not become a runtime persistence/API owner without separate schema and owner authorization.

Next step: `Implementation readiness gate for Data Source Health dashboard/API status`.

## 2. Owner Path To Preserve

Fixed future owner boundary:

```text
MarketQuote status owner path
Evidence / Score status owner path
DecisionResult status owner path
ExecutionPlan / BoundaryCandidate status owner path
Review / Replay result status owner path
  -> minimal aggregate status mapping
  -> optional read-only dashboard/API status surface
```

Rules:

- Future implementation must reuse the completed slice-local status semantics and source-health fields.
- Do not create a new Data Source Health service, mapper, schema table, DTO, Validator, Assembler, or Orchestrator.
- Do not treat `DataSourceHealthDO` as wired persistence ownership.
- Do not trigger MarketQuote fetch, review save, replay execution, recheck mutation, Push, Candidate generation, Decision generation, Point generation, order, execution, or auto-trading.
- Do not make Data Source Health an input to executable trade decisions.
- Keep Display Slots as display-only; they are not a candidate pool.

## 3. Minimal Status Rollup

The aggregate status must be derived from existing local `sourceHealth` values only. It must not fetch live data or generate missing upstream artifacts.

Rollup precedence:

1. `BLOCKED` or unsafe/ambiguous source status wins.
2. `MISSING` wins over stale/partial.
3. `STALE` wins over partial/watch-only.
4. `PARTIAL` wins over watch-only/ok.
5. `WATCH_ONLY` remains review-only and not executable.
6. `OK` only when every scoped source is readable and no source is stale, partial, missing, watch-only, blocked, or unknown.

Allowed future statuses:

| Status | Trigger condition | Dashboard/API copy intent | Candidate/Decision/Point/Push allowed? | Review-only? | Fail-closed? |
|---|---|---|---|---|---|
| `DATA_SOURCE_HEALTH_REVIEW_ONLY_READY` | All scoped source-health values are `OK` and every scoped status surface is readable. | Data source health is readable for review-only display. | No | Yes | No for display; still no downstream action |
| `DATA_SOURCE_HEALTH_PARTIAL_REVIEW_ONLY` | One or more scoped sources are `PARTIAL`, while none are stale, missing, blocked, or ambiguous. | Some source health is partial; manual review required. | No | Yes | Yes for downstream action |
| `DATA_SOURCE_HEALTH_STALE_FAIL_CLOSED` | Any scoped source reports `STALE` or freshness cannot prove current data. | At least one source is stale; status is fail-closed. | No | Yes | Yes |
| `DATA_SOURCE_HEALTH_MISSING_FAIL_CLOSED` | Any required scoped source is `MISSING`, unavailable, or has no readable status surface. | At least one source is missing; status is fail-closed. | No | Yes | Yes |
| `DATA_SOURCE_HEALTH_WATCH_ONLY_REVIEW` | Scoped source data is readable but at least one upstream slice is watch-only and no stronger failure exists. | Source health is watch-only; display only, not executable. | No | Yes | Yes for downstream action |
| `DATA_SOURCE_HEALTH_BLOCKED_FAIL_CLOSED` | Any scoped source is `BLOCKED`, `UNKNOWN`, contradictory, unsupported, or would require runtime mutation/execution to answer. | Source health is blocked; keep status fail-closed. | No | Yes | Yes |

## 4. Scoped Sources

Default in-scope sources for a minimal future readiness gate:

| Source | Existing status surface | Required local fields |
|---|---|---|
| MarketQuote | `GET /api/market/quote-status?symbol=...` | `status`, `sourceHealth`, `fresh`, `fallbackActive`, `reviewOnly`, `notTradingSignal` |
| Evidence / Score | `GET /api/dashboard/evidence-score-status?symbol=...` | `status`, `sourceHealth`, `sourceTraceComplete`, `reviewOnly`, `notCandidateSignal`, `notDecisionSignal`, `notPointSignal` |
| DecisionResult | `GET /api/dashboard/decision-result-status?symbol=...` | `status`, `sourceHealth`, `sourceTraceComplete`, `reviewOnly`, `notDecisionGeneration`, `notTradingSignal` |
| ExecutionPlan / BoundaryCandidate | `GET /api/dashboard/execution-plan-boundary-status?symbol=...` | `status`, `sourceHealth`, `notExecutable`, `notTradingSignal`, `notPointSignal`, `failClosed` |
| Review / Replay | `GET /api/dashboard/review-replay-result-status?symbol=...` | `status`, `sourceHealth`, `notReplayExecution`, `notTradingSignal`, `notCandidateSignal`, `notPointSignal` |

PositionSync can remain outside this first Data Source Health aggregate unless the readiness gate confirms it belongs in the same rollup. The first aggregate should focus on the five source-health surfaces confirmed by the source read.

## 5. Minimal Future Fields

Allowed future fields:

- `status`
- `symbol`
- `sourceHealth`
- `scopedSources`
- `sourceStatuses`
- `okSources`
- `partialSources`
- `staleSources`
- `missingSources`
- `watchOnlySources`
- `blockedSources`
- `reason`
- `message`
- `failClosed`
- `reviewOnly = true`
- `notTradingSignal = true`
- `notCandidateSignal = true`
- `notDecisionGeneration = true`
- `notPointSignal = true`
- `notReplayExecution = true`
- `notExecutable = true`
- `displaySlotsAreCandidatePool = false`

Forbidden future fields:

- candidate ranking
- generated decision
- final direction
- entry
- stop
- TP
- RR
- position size
- leverage
- order action
- Push send state
- external channel state
- replay execution action
- review result generation action

Use a minimal Map/existing object shape if implementation proceeds. Do not add a new DTO family.

## 6. Dashboard/API Surface

Readiness gate should evaluate two safe implementation shapes:

1. A minimal read-only aggregate endpoint in `DashboardController`, for example:

```text
GET /api/dashboard/data-source-health-status?symbol=BTCUSDT
```

2. A dashboard-only aggregate display that composes the existing five status endpoint responses client-side.

Preferred direction: allow one minimal read-only `DashboardController` aggregate endpoint only if the readiness gate confirms it can reuse existing owner semantics without adding service/schema/DTO surface. If that would duplicate too much controller logic, keep source health distributed and only display a dashboard matrix assembled from existing endpoint responses.

The dashboard/API surface must show:

- overall Data Source Health status;
- per-source status for the five scoped sources;
- source-health rollup reason;
- stale/missing/partial/blocked source lists;
- review-only label;
- not trading / not candidate / not decision generation / not point labels;
- not replay execution label;
- Display Slots boundary label.

The surface must not add a complex dashboard workflow, scan, fetch, replay, review-save, Push, order, execution, or auto-trading action.

## 7. Fail-Closed Rules

Future status must fail closed when:

- any scoped endpoint/status surface is unavailable;
- any scoped source returns `BLOCKED`, `UNKNOWN`, unsupported, or contradictory status;
- a required source-health field is missing;
- determining the status would require live fetch, replay execution, review result generation, recheck mutation, Push, Candidate generation, Decision generation, Point generation, order, execution, or auto-trading;
- Display Slots would be mistaken for a candidate pool;
- `DataSourceHealthDO` would need new schema/mapper/service ownership to answer.

Fail-closed means display can remain visible, but no downstream Candidate/Decision/Point/Push/trading implication is allowed.

## 8. Minimal Future Implementation Boundary

If readiness gate returns GO, future implementation must stay within:

- existing `MarketController` quote status semantics;
- existing `DashboardController` status semantics for Evidence / Score, DecisionResult, ExecutionPlan / BoundaryCandidate, and Review / Replay;
- optional minimal `DashboardController` status mapping;
- optional minimal dashboard display/copy;
- targeted controller/dashboard tests only;
- source-of-truth docs.

Future implementation must not:

- add DTO / Validator / Assembler / Orchestrator;
- add mapper/service/schema/table ownership for `DataSourceHealthDO`;
- change schema/config/pom;
- call MarketQuote real-fetch;
- call review save;
- call replay execution or recheck mutation;
- connect Push or external channels;
- generate Candidate;
- generate a new Decision;
- generate Point;
- output final direction / entry / stop / TP / RR;
- connect order / execution / auto-trading;
- continue P359 / P360.

## 9. Readiness Checklist

The next readiness gate must answer:

- Is a backend aggregate endpoint needed, or can dashboard compose existing status endpoints?
- If a backend endpoint is needed, can it live in `DashboardController` as a minimal Map response?
- Can implementation avoid `DataSourceHealthDO` mapper/service/schema ownership?
- Can implementation avoid new DTO / Validator / Assembler / Orchestrator?
- Are the five scoped status surfaces sufficient and stable enough?
- How should `UNKNOWN` from a slice-local status map to aggregate status?
- Can missing/stale/partial/watch-only/blocked lists be computed without fetching or generating data?
- Does dashboard already have a safe insertion slot?
- What minimal tests would be required if implementation proceeds?
- Can forbidden semantics remain absent from changed files and UI copy?

## 10. Capability-Level Movement

- Current level: `REVIEW_ONLY_RUNTIME partial`.
- This package raises capability level: No, design only.
- Future minimal implementation target: keep `REVIEW_ONLY_RUNTIME partial` by adding review-only visibility over existing source-health signals.
- It is not Production Wiring.
- It is not source-health persistence.
- It is not Push.
- It is not Candidate generation.
- It is not Decision generation.
- It is not Point generation.
- It is not Trading.

## 11. Freeze Rule Compliance

- 是否创建新骨架: No
- 是否复用 Cursor-era / V1 资产: Yes
- 是否减少重复: Yes
- 是否提升 capability level: No, design only
- 是否接 service/runtime/dashboard/API: No, design only; future readiness may authorize minimal review-only dashboard/API wiring
- 是否符合 #830 审计建议: Yes

## 12. Final Recommendation

GO to `Implementation readiness gate for Data Source Health dashboard/API status`.

The readiness gate should prefer a minimal review-only aggregate over existing slice-local `sourceHealth` statuses, keep `DataSourceHealthDO` inventory-only, reject new schema/service/DTO ownership, and decide whether the safest implementation shape is one read-only `DashboardController` endpoint or dashboard composition of existing endpoint responses.
