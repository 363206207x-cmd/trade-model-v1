# Minimal Review-Only SourceTrace / RuntimeKline / DataQuality / MultiTimeframe Status Runtime Wiring Implementation

## 1. Executive Summary

This B-risk implementation adds a minimal review-only status surface for `SourceTrace / RuntimeKline / DataQuality / MultiTimeframe aggregate review-only status`.

Implemented surface:

- Endpoint: `GET /api/dashboard/source-runtime-data-quality-status?symbol=BTCUSDT`
- Dashboard panel: `sourceRuntimeDataQualityStatusPanel`
- Owner path: existing dashboard detail / SourceTrace / RuntimeKline owner path
- Capability movement: none, still `REVIEW_ONLY_RUNTIME partial`

The endpoint is read-only. It reuses the existing dashboard detail owner path and existing SourceTrace / RuntimeKline / persisted OHLCV metadata. It does not trigger scheduler, collector, API client refresh, external refresh, source-binding generation, Candidate generation, Decision generation, Point generation, final direction, entry / stop / TP / RR, Push, external channel, order/execution, auto-trading, replay/recheck, P359, or P360.

## 2. Reused Existing Assets

| Asset | Reused? | Notes |
|---|---:|---|
| `DashboardController` | Yes | Adds one thin read-only Map endpoint. |
| `/api/dashboard/detail` owner path | Yes | The status endpoint calls the existing detail path and derives status from its read model. |
| `DashboardDetailResponseVO` | Yes | Provides `sourceTrace`, `runtimeKlineContext`, and DecisionResult analysis id context. |
| `DefaultDashboardSourceTraceDetailAdapter` | Yes | Existing SourceTrace adapter remains the owner of source trace fields. |
| `DefaultDashboardRuntimeKlineContextAdapter` | Yes | Existing RuntimeKline adapter remains the owner of runtime kline context. |
| `PersistedOhlcvReadiness` | Yes | Existing readiness value is mapped to review-only persisted OHLCV statuses. |
| `dashboard.html` | Yes | Adds one minimal status panel and safety copy. |
| `DashboardControllerTest` | Yes | Adds targeted endpoint/template assertions. |

No DTO, Validator, Assembler, Orchestrator, schema, config, pom, or source-binding family was added.

## 3. Implemented Endpoint

| Endpoint | Method | Purpose | Trigger refresh? | Trigger generation? | Trading semantics? |
|---|---|---|---:|---:|---:|
| `/api/dashboard/source-runtime-data-quality-status?symbol=BTCUSDT` | GET | Review-only SourceTrace / RuntimeKline / DataQuality / MultiTimeframe status | No | No | No |

Allowed response fields include status, symbol, analysis id, source trace readiness, runtime kline readiness, persisted OHLCV readiness, data quality status, multi-timeframe status, reason, message, safety fields, and fail-closed.

Forbidden response fields are not exposed: final direction, entry, stop, TP, RR, candidate ranking, order action, execution action, auto-trading action, Push send state, refresh action, source-binding generation action, or executable trading fields.

## 4. Dashboard Panel

Dashboard panel:

- `sourceRuntimeDataQualityStatusPanel`

DOM ids:

- `sourceRuntimeStatusValue`
- `sourceTraceReadinessValue`
- `runtimeKlineReadinessValue`
- `persistedOhlcvReadinessValue`
- `dataQualityStatusValue`
- `multiTimeframeStatusValue`
- `sourceRuntimeRefreshBoundaryValue`
- `sourceRuntimeSignalBoundaryValue`
- `sourceRuntimeReasonValue`

The panel is a status surface only. It displays review-only, fail-closed, not candidate, not decision generation, not point, not final direction, not entry / stop / TP / RR, not trading, not executable, not scheduler trigger, not collector trigger, not API client refresh, not external refresh, and not source-binding generation copy.

## 5. Status Mapping

Implemented statuses:

- `SOURCE_RUNTIME_STATUS_REVIEW_ONLY_READY`
- `SOURCE_TRACE_MISSING_FAIL_CLOSED`
- `SOURCE_TRACE_PARTIAL_REVIEW_ONLY`
- `RUNTIME_KLINE_CONTEXT_READY_REVIEW_ONLY`
- `RUNTIME_KLINE_CONTEXT_MISSING_FAIL_CLOSED`
- `PERSISTED_OHLCV_READY_REVIEW_ONLY`
- `PERSISTED_OHLCV_STALE_REVIEW_ONLY`
- `PERSISTED_OHLCV_MISSING_FAIL_CLOSED`
- `DATA_QUALITY_PARTIAL_REVIEW_ONLY`
- `DATA_QUALITY_BLOCKED_FAIL_CLOSED`
- `MULTITIMEFRAME_ALIGNMENT_REVIEW_ONLY`
- `MULTITIMEFRAME_CONFLICT_REVIEW_ONLY`
- `MULTITIMEFRAME_MISSING_FAIL_CLOSED`
- `REFRESH_BOUNDARY_BLOCKED_FAIL_CLOSED`
- `GENERATION_BOUNDARY_BLOCKED_FAIL_CLOSED`

Fail-closed rules:

- Missing SourceTrace fails closed.
- Missing RuntimeKline context fails closed.
- Missing / unknown persisted OHLCV readiness fails closed.
- Missing DataQuality metadata fails closed.
- Missing MultiTimeframe metadata fails closed.
- Refresh/generation boundaries remain blocked and fail-closed.

Review-only partial rules:

- Partial SourceTrace remains review-only partial.
- Stale / partial / invalid persisted OHLCV remains review-only stale.
- MultiTimeframe conflict remains review-only conflict.
- DataQuality partial remains review-only partial.

## 6. Safety Fields

The endpoint returns:

- `reviewOnly=true`
- `notCandidateSignal=true`
- `notDecisionGeneration=true`
- `notPointSignal=true`
- `notFinalDirection=true`
- `notEntryStopTpRr=true`
- `notTradingSignal=true`
- `notExecutable=true`
- `notSchedulerTrigger=true`
- `notCollectorTrigger=true`
- `notApiClientRefresh=true`
- `notExternalRefresh=true`
- `notSourceBindingGeneration=true`
- `displaySlotsAreCandidatePool=false`

## 7. Tests

Targeted tests were added to `DashboardControllerTest`:

- Endpoint returns review-only ready status and safety fields.
- Missing SourceTrace / RuntimeKline states fail closed.
- Persisted OHLCV stale / missing, DataQuality partial / blocked, and MultiTimeframe conflict / missing states are mapped.
- Forbidden executable, refresh, generation, candidate, point, final direction, entry / stop / TP / RR, Push, order, execution, and auto-trading fields are absent.
- Dashboard template includes `sourceRuntimeDataQualityStatusPanel`, DOM ids, status constants, and safety copy.

Checks run during implementation:

- `./mvnw -q -DskipTests compile` PASS
- `./mvnw -q -DskipTests test-compile` PASS
- `./mvnw -q -Dtest=DashboardControllerTest test` PASS
- `./mvnw -q test` PASS

## 8. Forbidden Scope Confirmation

This package did not add:

- DTO / Validator / Assembler / Orchestrator
- source-binding family
- schema/config/pom changes
- scheduler / collector / API client refresh
- external refresh
- Candidate generation
- Decision generation
- Point generation
- final direction
- entry / stop / TP / RR
- Push send / external channel
- order / execution / auto-trading
- Position Monitor execution
- replay / recheck
- P359 / P360

## 9. Next Allowed Action

Next allowed action:

`Minimal Review-Only SourceTrace / RuntimeKline / DataQuality / MultiTimeframe Aggregate Runtime Wiring Verification`

The next package is A-risk verification docs/source-of-truth only after this B-risk implementation PR is reviewed and merged.
