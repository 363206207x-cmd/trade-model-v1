# PHASE_BACKEND_P10_PERSISTED_OHLCV_SOURCE_CONTRACT

## 1. Document Purpose

This document defines the BACKEND-P10 persisted OHLCV source contract for future dashboard `RuntimeKlineContextDTO` wiring.

Baseline:

- `d291ce1`

Issue context:

- `#101 BACKEND-P10 Persisted OHLCV Source Contract Pack`

P9 concluded:

- `NO_SAFE_PERSISTED_OHLCV_SOURCE_FOUND`
- no persisted kline / OHLCV table,
- no kline / OHLCV entity,
- no kline / OHLCV mapper,
- no persisted kline query service.

P10 is documentation-only. It defines the contract that must exist before any future RuntimeKline, SourceTrace, BoundaryCandidateService, or dashboard detail wiring.

## 2. Scope

This package does not modify:

- Java production code,
- Java tests,
- `dashboard.html`,
- schema,
- config,
- external data integrations,
- order API,
- auto-trading logic.

This package does not:

- wire `RuntimeKlineContextDTO`,
- complete `SourceTraceDTO`,
- complete `DerivativesRiskContextDTO`,
- generate entry / stop / TP values,
- treat `latestPrice` as an entry source,
- treat quote freshness as kline stale status,
- call external APIs from dashboard detail,
- add Coinglass or any other external data provider.

## 3. Contract Goal

The future persisted OHLCV source must provide a read-only, source-owned, freshness-aware kline window that can be queried without live dashboard calls.

It must support future `RuntimeKlineContextDTO` assembly only when all required persisted fields exist and pass freshness checks.

If any required persisted field is missing, stale, source-ambiguous, or internally inconsistent, future consumers must fail closed:

- `RuntimeKlineContextDTO` remains `INCOMPLETE`,
- `SourceTraceDTO` remains `INCOMPLETE`,
- dashboard RuntimeKline marker remains `UNAVAILABLE` or `INCOMPLETE`,
- `manualReviewRequired=true`,
- `notTradeInstruction=true`.

## 4. Proposed Persisted OHLCV Table

Proposed table name:

- `tm_persisted_ohlcv_bar`

This is a contract proposal only. P10 does not add schema.

| Field | Type | Required | Purpose |
|---|---|---:|---|
| `id` | BIGINT | Yes | Internal primary key. |
| `symbol` | VARCHAR(20) | Yes | Normalized trading symbol, e.g. `BTCUSDT`. |
| `timeframe` | VARCHAR(10) | Yes | Candle timeframe, e.g. `1m`, `5m`, `15m`, `1h`. |
| `open_time_ms` | BIGINT | Yes | Exchange candle open time in epoch milliseconds. |
| `close_time_ms` | BIGINT | Yes | Exchange candle close time in epoch milliseconds. |
| `open_price` | DECIMAL(20, 8) | Yes | Candle open price. |
| `high_price` | DECIMAL(20, 8) | Yes | Candle high price. |
| `low_price` | DECIMAL(20, 8) | Yes | Candle low price. |
| `close_price` | DECIMAL(20, 8) | Yes | Candle close price. |
| `volume` | DECIMAL(28, 8) | Yes | Base asset volume. |
| `quote_volume` | DECIMAL(28, 8) | No | Quote asset volume if provider supplies it. |
| `trade_count` | BIGINT | No | Number of trades if provider supplies it. |
| `taker_buy_base_volume` | DECIMAL(28, 8) | No | Taker buy base volume if provider supplies it. |
| `taker_buy_quote_volume` | DECIMAL(28, 8) | No | Taker buy quote volume if provider supplies it. |
| `is_closed` | BOOLEAN | Yes | Whether this candle is final and no longer mutable. |
| `provider` | VARCHAR(64) | Yes | Data provider name. |
| `provider_market_type` | VARCHAR(32) | Yes | Market type such as `SPOT`, `USDT_PERP`, or `UNKNOWN`. |
| `source_endpoint` | VARCHAR(256) | Yes | Logical source endpoint or ingestion job label. |
| `source_batch_id` | VARCHAR(64) | Yes | Ingestion batch id for traceability. |
| `source_trace_id` | VARCHAR(64) | Yes | Trace id linking ingestion and dashboard read path. |
| `source_version` | INT | Yes | Source contract version. |
| `ingested_at` | TIMESTAMP | Yes | Time the bar was persisted locally. |
| `updated_at` | TIMESTAMP | Yes | Last local update time. |
| `quality_status` | VARCHAR(32) | Yes | `OK`, `PARTIAL`, `MISSING_FIELD`, `CONFLICT`, `STALE`, `INVALID`. |
| `quality_reason` | VARCHAR(512) | No | Human-readable quality reason. |
| `raw_payload_hash` | VARCHAR(128) | No | Optional hash for raw payload traceability. |
| `is_deleted` | INT | Yes | Soft-delete marker, default `0`. |

Recommended uniqueness:

- unique key on `symbol`, `timeframe`, `open_time_ms`, `provider`, `provider_market_type`.

Recommended query index:

- `symbol`, `timeframe`, `close_time_ms`,
- `symbol`, `timeframe`, `ingested_at`,
- `source_batch_id`,
- `source_trace_id`.

## 5. Source Ownership Fields

The future source must make ownership explicit. RuntimeKline wiring must not infer source ownership from table presence alone.

Required source ownership fields:

| Ownership Field | Required | Rule |
|---|---:|---|
| `provider` | Yes | Must be non-blank. |
| `provider_market_type` | Yes | Must be explicit; unknown market type cannot complete RuntimeKline. |
| `source_endpoint` | Yes | Must identify ingestion path without requiring live dashboard calls. |
| `source_batch_id` | Yes | Must identify the persisted ingestion batch. |
| `source_trace_id` | Yes | Must allow read-path audit. |
| `source_version` | Yes | Must allow future contract migration. |
| `ingested_at` | Yes | Required for freshness and stale status. |
| `quality_status` | Yes | Must be evaluated before RuntimeKline assembly. |
| `quality_reason` | Conditional | Required when `quality_status != OK`. |

Invalid ownership cases:

- missing provider,
- missing source endpoint,
- missing source batch id,
- missing source trace id,
- ambiguous market type,
- mixed providers inside the same required window without an explicit merge rule,
- `quality_status` not `OK` for any required bar,
- deleted bars in required window,
- bars produced only by a live dashboard request.

## 6. Freshness Policy

Freshness must be evaluated on persisted bars, not on quote metadata.

Future policy should define:

| Input | Rule |
|---|---|
| `now_ms` | Evaluated at dashboard read time or service read time. |
| `timeframe` | Determines expected bar interval. |
| `required_window_size` | Minimum number of closed bars needed for RuntimeKline. |
| `latest_required_close_time_ms` | Latest closed bar required by timeframe. |
| `max_ingestion_lag_ms` | Maximum allowed lag between provider close time and local ingestion. |
| `max_read_lag_ms` | Maximum allowed age between latest persisted close time and read time. |
| `allow_open_candle` | Default false for RuntimeKline completeness. |

Recommended minimum policy:

- RuntimeKline must use closed candles only.
- Required window must contain contiguous bars.
- Latest closed bar must be within allowed read lag.
- Every required bar must be locally persisted.
- Every required bar must have `quality_status=OK`.
- Open candles may be retained for display metadata only, not RuntimeKline completeness.

## 7. Stale Status Rules

Future stale status must be calculated from persisted OHLCV state.

Recommended enum values:

- `FRESH`
- `STALE`
- `PARTIAL`
- `MISSING`
- `UNKNOWN`
- `INVALID`

Recommended rules:

| Status | Condition |
|---|---|
| `FRESH` | Required closed window is contiguous, quality is OK, latest close time and ingestion time satisfy freshness policy. |
| `STALE` | Required window exists but latest close time or ingestion time is older than policy. |
| `PARTIAL` | Some required bars exist, but the full required window is incomplete. |
| `MISSING` | No persisted bars exist for requested symbol/timeframe. |
| `UNKNOWN` | Source ownership or policy config is missing. |
| `INVALID` | Bars are internally inconsistent, deleted, non-positive, conflicting, or quality failed. |

Only `FRESH` may be eligible for future RuntimeKline completeness.

Every other status must fail closed.

## 8. Stale Reason Rules

Stale reason must be structured enough for dashboard display and tests.

Recommended reason codes:

| Reason Code | Meaning |
|---|---|
| `NO_PERSISTED_OHLCV_TABLE` | Schema is unavailable. |
| `NO_BARS_FOR_SYMBOL_TIMEFRAME` | No bars found. |
| `WINDOW_TOO_SHORT` | Required bar count is not satisfied. |
| `WINDOW_NOT_CONTIGUOUS` | Missing one or more candles inside the window. |
| `LATEST_BAR_TOO_OLD` | Latest close time violates read freshness policy. |
| `INGESTION_LAG_TOO_HIGH` | Local ingestion lag violates policy. |
| `OPEN_CANDLE_ONLY` | Only mutable/open candle is available. |
| `QUALITY_STATUS_NOT_OK` | One or more bars has non-OK quality. |
| `SOURCE_OWNER_MISSING` | Required source ownership is absent. |
| `SOURCE_OWNER_CONFLICT` | Multiple providers or market types conflict without merge policy. |
| `PRICE_FIELD_MISSING` | Required OHLC field is missing. |
| `PRICE_FIELD_INVALID` | OHLC field is zero, negative, or inconsistent. |
| `VOLUME_FIELD_MISSING` | Volume is missing when required. |
| `POLICY_NOT_CONFIGURED` | Freshness policy is missing. |

Recommended stale reason output:

- `staleStatus`
- `staleReasonCode`
- `staleReasonText`
- `missingFields`
- `sourceTraceId`
- `sourceBatchId`

## 9. RuntimeKlineContext Readiness Mapping

Future `RuntimeKlineContextDTO` may only become complete when all persisted OHLCV readiness checks pass.

| RuntimeKline Field | Future Persisted Source | Completion Rule |
|---|---|---|
| `symbol` | request + persisted bars | Must match normalized persisted symbol. |
| `timeframe` | request + persisted bars | Must match persisted timeframe. |
| `latestPrice` | latest closed candle close price | Must come from closed persisted bar, not quote metadata. |
| `dataQualityScore` | quality calculation from persisted window | Must be explicit; cannot reuse unrelated decision score without ownership. |
| `fallbackStatus` | readiness result | `null` or complete-only value only after all checks pass; otherwise `INCOMPLETE`. |
| `missingFields` | readiness result | Empty only when all required fields pass. |
| `manualReviewRequired` | safety default | Must stay true. |
| `notTradeInstruction` | safety default | Must stay true. |

P10 does not define entry / stop / TP generation from OHLCV.

Future boundary numeric sources require a separate source assembler contract and tests.

## 10. Mapper And Entity Boundary

Future entity:

- `PersistedOhlcvBarDO`

Future mapper:

- `PersistedOhlcvBarMapper`

Allowed mapper methods:

| Method | Purpose |
|---|---|
| `selectLatestClosedWindow(symbol, timeframe, limit)` | Read the latest closed persisted window. |
| `selectBySymbolTimeframeAndOpenTime(symbol, timeframe, openTimeMs)` | Audit a single bar. |
| `selectLatestIngestionBatch(symbol, timeframe)` | Audit source batch metadata. |

Forbidden mapper behavior:

- no external API call,
- no fallback to live Binance,
- no synthetic candle generation,
- no entry / stop / TP generation,
- no order API interaction,
- no mutable open-candle completion unless future policy explicitly allows it for display-only metadata.

## 11. Query Service Boundary

Future read-only service:

- `PersistedOhlcvQueryService`

Preferred methods:

```java
PersistedOhlcvWindowResult selectRuntimeWindow(String symbol, String timeframe, int requiredWindowSize);

PersistedOhlcvReadinessResult evaluateReadiness(String symbol, String timeframe, int requiredWindowSize);
```

Required behavior:

- read only from local persisted tables,
- return structured readiness status,
- return missing fields and stale reasons,
- never call external APIs,
- never infer data from quote-only fields,
- never generate trading actions,
- never complete RuntimeKline when source ownership is missing.

Forbidden behavior:

- no `RealMarketDataFetcherService.fetchKlines(...)` call,
- no Coinglass call,
- no exchange API call,
- no order API call,
- no auto-trading,
- no entry / stop / TP production value generation.

## 12. Fail-Closed Behavior

Any missing or unsafe condition must fail closed.

| Missing / Unsafe Condition | Required Result |
|---|---|
| No persisted OHLCV table | RuntimeKline `INCOMPLETE`; SourceTrace `INCOMPLETE`. |
| No bars for symbol/timeframe | RuntimeKline `INCOMPLETE`; stale status `MISSING`. |
| Window too short | RuntimeKline `INCOMPLETE`; stale status `PARTIAL`. |
| Non-contiguous bars | RuntimeKline `INCOMPLETE`; stale reason `WINDOW_NOT_CONTIGUOUS`. |
| Latest bar too old | RuntimeKline `INCOMPLETE`; stale status `STALE`. |
| Source ownership missing | RuntimeKline `INCOMPLETE`; stale status `UNKNOWN`. |
| Quality not OK | RuntimeKline `INCOMPLETE`; stale status `INVALID` or `PARTIAL`. |
| Only quote latest price exists | RuntimeKline `INCOMPLETE`; quote metadata only. |
| Only dashboard analysis anchor exists | RuntimeKline `INCOMPLETE`; analysis metadata only. |
| Live fetch would be required | RuntimeKline `INCOMPLETE`; do not call external API. |

Safety defaults must remain:

- `manualReviewRequired=true`
- `notTradeInstruction=true`

## 13. Required Tests Before Wiring

No RuntimeKline wiring should begin until these tests exist.

### 13.1 Entity / Mapper Tests

Required coverage:

- insert / select latest closed window,
- order by close time,
- limit by required window size,
- exclude deleted bars,
- reject open candles for completeness,
- preserve source ownership fields,
- preserve quality fields.

### 13.2 Query Service Tests

Required coverage:

- no table / no rows -> `MISSING`,
- too few rows -> `PARTIAL`,
- non-contiguous rows -> `WINDOW_NOT_CONTIGUOUS`,
- stale latest bar -> `STALE`,
- missing source owner -> `UNKNOWN`,
- non-OK quality -> fail closed,
- fresh contiguous window -> `FRESH`,
- quote-only latest price does not complete RuntimeKline,
- live fetcher is not called.

### 13.3 Dashboard Adapter Tests

Required coverage:

- missing persisted source keeps `runtimeKlineContextStatus=UNAVAILABLE`,
- incomplete persisted source keeps SourceTrace incomplete,
- fresh persisted window may expose read-only RuntimeKline metadata only after all checks pass,
- quote freshness is not kline stale status,
- `latestPrice` is not entry source,
- no `dashboard.html` change required.

### 13.4 Boundary Tests

Required coverage:

- SourceTrace remains incomplete without required boundary sources,
- RuntimeKline complete does not generate entry / stop / TP,
- BoundaryCandidate VALID remains manual-review / not-trade-instruction,
- ExecutionPlan remains review-only,
- no order API / auto-trading path is introduced.

Recommended command set when code wiring begins:

```bash
./mvnw -q -Dtest=PersistedOhlcvBarMapperTest test
./mvnw -q -Dtest=PersistedOhlcvQueryServiceTest test
./mvnw -q -Dtest=DefaultDashboardRuntimeKlineContextAdapterTest test
./mvnw -q -Dtest=DefaultDashboardSourceTraceDetailAdapterTest test
./mvnw -q -Dtest=DashboardControllerTest test
./mvnw -q -DskipTests compile
./mvnw -q -DskipTests test-compile
```

## 14. Next Implementation Boundary

The next implementation phase may create a minimal persisted OHLCV read contract only if it remains read-only and fail-closed.

Allowed next-phase scope:

- schema proposal implementation for persisted OHLCV table,
- entity and mapper for persisted bars,
- read-only query service,
- readiness result DTO,
- tests for freshness, stale status, and fail-closed behavior.

Still forbidden in the next phase unless explicitly approved later:

- RuntimeKline dashboard completion,
- SourceTrace completion,
- external API integration,
- Coinglass integration,
- live fetch from dashboard detail,
- order API,
- auto-trading,
- entry / stop / TP generation,
- treating quote latest price as entry source,
- treating quote freshness as kline stale status,
- `dashboard.html` changes.

## 15. Current Conclusion

BACKEND-P10 defines the persisted OHLCV source contract required before any safe RuntimeKline wiring.

The current package is documentation-only.

RuntimeKline remains unavailable and incomplete.

SourceTrace remains incomplete.

DerivativesRiskContext remains fail-closed.

Dashboard detail remains read-only and review-only.

No external data integration, order API, auto-trading, entry / stop / TP generation, or dashboard HTML changes are introduced.
