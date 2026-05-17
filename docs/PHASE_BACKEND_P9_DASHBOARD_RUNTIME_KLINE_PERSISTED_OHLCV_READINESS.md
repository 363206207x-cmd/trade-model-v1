# PHASE_BACKEND_P9_DASHBOARD_RUNTIME_KLINE_PERSISTED_OHLCV_READINESS

## 1. Document Purpose

This document records the BACKEND-P9 Dashboard RuntimeKline Persisted OHLCV Readiness audit.

Baseline:

- `5ed5f93 feat(backend): add dashboard RuntimeKline boundary`

Issue context:

- `#99 BACKEND-P9 Dashboard RuntimeKline Persisted OHLCV Readiness Pack`

BACKEND-P8 introduced an explicit dashboard RuntimeKline boundary that remains unavailable and fail-closed.

BACKEND-P9 audits whether the current repository already contains a safe persisted OHLCV / kline source that can feed `RuntimeKlineContextDTO` without live calls from dashboard detail.

## 2. Scope

This package is documentation-only.

It does not modify:

- `dashboard.html`
- Java production code
- Java tests
- schema
- config
- external integrations
- order API
- auto-trading logic

It does not complete:

- RuntimeKlineContext
- SourceTrace
- DerivativesRiskContext
- BoundaryCandidate VALID production wiring
- ExecutionPlan readiness

## 3. Readiness Result

Result:

- `NO_SAFE_PERSISTED_OHLCV_SOURCE_FOUND`

The current repository does not contain a persisted kline / OHLCV table, mapper, entity, or service that can safely feed dashboard `RuntimeKlineContextDTO`.

Therefore BACKEND-P9 does not add a RuntimeKline data adapter and does not wire dashboard detail to any OHLCV source.

The existing BACKEND-P8 behavior remains correct:

- RuntimeKlineContext remains `INCOMPLETE`.
- SourceTrace remains `INCOMPLETE`.
- RuntimeKline marker remains `UNAVAILABLE`.
- DerivativesRiskContext remains `SAFE_FAIL_CLOSED_ONLY`.
- Dashboard output remains read-only and review-only.

## 4. Audited Areas

| Area | Files / Components Reviewed | Finding | Readiness |
|---|---|---|---|
| Schema | `src/main/resources/schema.sql` | No `tm_kline`, `tm_ohlcv`, candle, bar, OHLCV window, freshness, or stale-status table exists. | Not ready |
| Persisted kline entity | `src/main/java/org/example/trademodel/entity` | No kline / OHLCV entity exists. | Not ready |
| Persisted kline mapper | `src/main/java/org/example/trademodel/mapper` | No kline / OHLCV mapper exists. | Not ready |
| Persisted kline service | `src/main/java/org/example/trademodel/service` | No persisted kline query service exists. | Not ready |
| RuntimeKline dashboard boundary | `DefaultDashboardRuntimeKlineContextAdapter` | Correctly returns unavailable `RuntimeKlineContextDTO` with missing persisted OHLCV and stale-status fields. | Safe fail-closed |
| SourceTrace dashboard adapter | `DefaultDashboardSourceTraceDetailAdapter` | Wires only unavailable RuntimeKline marker plus safe metadata; does not assemble RuntimeKline. | Safe fail-closed |
| Analysis run snapshot | `tm_analysis_run`, `AnalysisRunDO`, `AnalysisRunMapper` | Persists symbol, timeframe, analysis time, data quality, trace id, and status; does not persist OHLCV window or kline stale status. | Metadata only |
| Market environment snapshot | `tm_market_environment_snapshot`, `MarketEnvironmentSnapshotDO`, `MarketEnvironmentSnapshotMapper` | Persists market environment and derivative summary fields; does not persist candle windows or OHLCV freshness. | Not RuntimeKline-ready |
| Decision result read model | `DecisionResultVO`, `DecisionResultMapper` | Can expose quote-like latest price and price update time; these are not OHLCV window or stale-status sources. | Metadata only |
| Market quote snapshot | `MarketQuoteSnapshot` | Runtime DTO for quote provider result; no persisted quote table or OHLCV window. | Not RuntimeKline-ready |
| RealMarketDataFetcherService | `fetchKlines(...)` | Performs live Binance HTTP request. Dashboard detail must not call it. | Not allowed |
| DecisionEngineService | `makeDecision(...)` | Uses live `fetchKlines(...)` during analysis generation, not persisted dashboard read path. | Not dashboard-safe source |

## 5. Persisted Source Audit Details

### 5.1 Schema

`src/main/resources/schema.sql` contains persisted tables for:

- analysis runs
- evidence items
- score items
- decision results
- execution plans
- market environment snapshots
- rule config
- user config
- real positions
- push snapshots
- account risk snapshots
- push recheck logs
- monitor alerts
- missed opportunities
- review results
- rule version logs
- asset state
- hot reset events

It does not contain a persisted OHLCV or kline table.

Missing schema concepts:

- kline open time
- kline close time
- open price
- high price
- low price
- close price
- volume
- quote volume
- candle source
- persisted kline window
- kline freshness
- stale status
- stale reason

### 5.2 AnalysisRun

`tm_analysis_run` can provide:

- `symbol`
- `timeframe`
- `analysis_time`
- `data_quality_score`
- `trace_id`
- `status`

It cannot provide:

- OHLCV window
- kline items
- latest candle close
- kline freshness
- stale status
- candle source ownership

Conclusion:

- `tm_analysis_run` may remain safe metadata.
- It must not be used to complete `RuntimeKlineContextDTO`.

### 5.3 MarketEnvironmentSnapshot

`tm_market_environment_snapshot` can provide:

- market environment type
- risk mode
- trend friendliness
- range percent 24h
- volatility regime
- last funding rate
- last open interest
- open interest delta
- derivatives crowding state
- source type

It cannot provide a persisted OHLCV window.

Conclusion:

- It is useful derivative / market-environment metadata.
- It is not a RuntimeKline OHLCV source.
- It must not complete RuntimeKlineContext.

### 5.4 DecisionResult / Quote Metadata

Dashboard detail can expose decision read-model fields such as:

- symbol
- timeframe
- latest price
- price update time
- data quality score
- multi-timeframe convergence

These fields remain safe metadata only.

They do not prove:

- persisted kline window exists,
- OHLCV is fresh,
- stale status is available,
- latest price is a boundary entry source,
- kline stale status is available from quote freshness.

Conclusion:

- `latestPrice` must not be treated as entry source.
- quote update time must not be treated as kline stale status.

### 5.5 RealMarketDataFetcherService

`RealMarketDataFetcherService.fetchKlines(...)` calls Binance HTTP directly.

This is not a safe dashboard detail source because BACKEND-P9 forbids:

- live OHLCV fetch from dashboard detail,
- external calls from dashboard detail,
- completing RuntimeKlineContext without persisted source ownership.

Conclusion:

- `fetchKlines(...)` must not be wired into dashboard detail.
- It can only be considered in a future phase after a persisted ingestion and read-model contract exists.

## 6. Current RuntimeKline Boundary Status

Current dashboard RuntimeKline boundary remains:

| Field / Marker | Current Status |
|---|---|
| RuntimeKlineContext fallback | `INCOMPLETE` |
| RuntimeKlineContext completeness | `false` |
| SourceTrace fallback | `INCOMPLETE` |
| SourceTrace `runtimeKlineContextStatus` | `UNAVAILABLE` |
| SourceTrace `runtimeKlineContextSource` | `dashboardDetail.noRuntimeKlineContext` |
| DerivativesRiskContext fallback | `SAFE_FAIL_CLOSED_ONLY` |
| `manualReviewRequired` | `true` |
| `notTradeInstruction` | `true` |

## 7. Still-Missing Fields

RuntimeKline / SourceTrace must remain missing for:

- persisted OHLCV table
- persisted kline mapper
- persisted kline service
- persisted kline window
- kline items
- kline open time
- kline close time
- open price
- high price
- low price
- close price
- volume
- kline freshness
- stale status
- stale reason
- runtime latest price source
- data quality source ownership for RuntimeKline
- entry price source
- stop price source
- TP price sources
- RR source
- liquidity source
- multi-timeframe source backed by persisted RuntimeKline
- event source
- wick source

## 8. Boundary Confirmations

BACKEND-P9 confirms:

- No `dashboard.html` change.
- No schema change.
- No config change.
- No Java production code change.
- No Java test change.
- No external data integration.
- No Coinglass integration.
- No order API.
- No auto-trading.
- No live OHLCV fetch from dashboard detail.
- No SourceTrace completion.
- No RuntimeKline completion.
- No BoundaryCandidate VALID production upgrade.
- No ExecutionPlan readiness upgrade.
- `latestPrice` is not treated as entry source.
- quote freshness is not treated as kline stale status.
- VALID remains manual-review / not-trade-instruction.

## 9. Future Readiness Requirements

Before dashboard RuntimeKline can move beyond `UNAVAILABLE`, a future phase must add or confirm:

- persisted OHLCV schema,
- kline entity,
- kline mapper,
- kline query service,
- source ownership fields,
- kline window freshness policy,
- stale-status calculation,
- stale reason output,
- no-live-call dashboard read path,
- tests proving missing kline data remains fail-closed,
- tests proving latest price is not entry source,
- tests proving quote freshness is not kline stale status.

Only after these exist should a read-only adapter be considered.

Even then, SourceTrace and RuntimeKline must remain incomplete unless all required fields are persisted, source-owned, and tested.

## 10. Tests

This package is documentation-only.

Tests were not run because no Java, dashboard, schema, config, or resource files changed.

Recommended future tests when a persisted OHLCV contract is introduced:

```bash
./mvnw -q -Dtest=DefaultDashboardRuntimeKlineContextAdapterTest test
./mvnw -q -Dtest=DefaultDashboardSourceTraceDetailAdapterTest test
./mvnw -q -Dtest=DashboardControllerTest test
./mvnw -q -DskipTests compile
./mvnw -q -DskipTests test-compile
```

## 11. Current Conclusion

BACKEND-P9 finds no safe persisted OHLCV / kline source in the current repository.

The correct result is documentation-only readiness:

- keep dashboard RuntimeKline boundary unavailable,
- keep SourceTrace incomplete,
- keep DerivativesRiskContext safe-fail-closed,
- keep dashboard detail read-only,
- do not call live market APIs from dashboard detail,
- do not complete RuntimeKline without persisted OHLCV, freshness, and stale-status sources.

This preserves the BACKEND-P8 fail-closed boundary and prepares the next phase to design a real persisted OHLCV source contract before any RuntimeKline wiring.
