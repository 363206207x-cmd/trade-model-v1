# PHASE_BACKEND_P12_PERSISTED_OHLCV_READINESS_QUERY_SERVICE_RESULT

## 1. Document Purpose

This document records the BACKEND-P12 Persisted OHLCV Readiness Query Service Pack result.

Issue context:

- `#105 BACKEND-P12 Persisted OHLCV Readiness Query Service Pack`

Baseline:

- `cf505d0 feat(backend): add persisted OHLCV skeleton`

P12 adds a read-only readiness layer on top of `tm_persisted_ohlcv_bar`.

It does not wire RuntimeKlineContext or complete SourceTrace.

## 2. Files Changed

Added files:

- `src/main/java/org/example/trademodel/dto/ohlcv/PersistedOhlcvReadinessStatus.java`
- `src/main/java/org/example/trademodel/dto/ohlcv/PersistedOhlcvStaleReasonCode.java`
- `src/main/java/org/example/trademodel/dto/ohlcv/PersistedOhlcvReadinessResult.java`
- `src/main/java/org/example/trademodel/service/PersistedOhlcvQueryService.java`
- `src/main/java/org/example/trademodel/service/impl/PersistedOhlcvQueryServiceImpl.java`
- `src/test/java/org/example/trademodel/service/impl/PersistedOhlcvQueryServiceTest.java`
- `docs/PHASE_BACKEND_P12_PERSISTED_OHLCV_READINESS_QUERY_SERVICE_RESULT.md`

Removed temporary trigger artifact:

- `docs/P12_TRIGGER.md`

## 3. Readiness Statuses

The readiness service returns structured statuses:

- `FRESH`
- `STALE`
- `PARTIAL`
- `MISSING`
- `UNKNOWN`
- `INVALID`

The readiness result also carries:

- stale reason code,
- stale reason text,
- missing fields,
- latest close time,
- latest ingestion time,
- selected persisted bars,
- `manualReviewRequired=true`,
- `notTradeInstruction=true`.

## 4. Stale Reason Codes

Implemented stale reason codes:

- `NONE`
- `NO_BARS_FOR_SYMBOL_TIMEFRAME`
- `WINDOW_TOO_SHORT`
- `WINDOW_NOT_CONTIGUOUS`
- `LATEST_BAR_TOO_OLD`
- `QUALITY_STATUS_NOT_OK`
- `SOURCE_OWNER_MISSING`
- `PRICE_FIELD_MISSING`
- `PRICE_FIELD_INVALID`
- `VOLUME_FIELD_MISSING`
- `POLICY_NOT_CONFIGURED`

## 5. Readiness Rules

Implemented checks:

- no rows -> `MISSING`
- too few rows -> `PARTIAL`
- non-contiguous closed window -> `PARTIAL` / `WINDOW_NOT_CONTIGUOUS`
- latest bar older than read-lag policy -> `STALE`
- missing source ownership -> `UNKNOWN`
- non-OK quality -> `INVALID`
- invalid OHLC fields -> `INVALID`
- missing / invalid volume -> `INVALID`
- fresh contiguous closed source-owned quality-OK window -> `FRESH`
- open candles are excluded by the read-only mapper and cannot complete readiness
- deleted rows are excluded by the read-only mapper and cannot complete readiness

## 6. Fail-Closed Behavior

All non-`FRESH` statuses are fail-closed for future consumers.

P12 does not turn `FRESH` into a trade action.

P12 does not turn `FRESH` into RuntimeKline completion.

P12 only provides a readiness result that a future phase may consume after additional wiring and tests.

Safety defaults remain:

- `manualReviewRequired=true`
- `notTradeInstruction=true`

## 7. Unwired Fields And Modules

The following remain unwired:

- RuntimeKlineContext,
- SourceTrace,
- DerivativesRiskContext,
- BoundaryCandidateService VALID production path,
- ExecutionPlan readiness,
- dashboard detail RuntimeKline completion,
- entry price source,
- stop price source,
- TP price source,
- RR source,
- liquidity source,
- multi-timeframe source,
- event source,
- wick source.

## 8. Boundary Confirmations

BACKEND-P12 confirms:

- no `dashboard.html` change,
- no RuntimeKlineContext wiring,
- no SourceTrace completion,
- no DerivativesRiskContext completion,
- no external data integration,
- no Coinglass integration,
- no live dashboard fetch,
- no order API,
- no auto-trading,
- no entry / stop / TP generation,
- no latestPrice-to-entry mapping,
- quote freshness is not kline stale status,
- VALID remains manual-review / not-trade-instruction.

## 9. Tests Run

Commands:

```bash
./mvnw -q -Dtest=PersistedOhlcvQueryServiceTest test
./mvnw -q -Dtest=PersistedOhlcvBarMapperIntegrationTest test
./mvnw -q -DskipTests compile
./mvnw -q -DskipTests test-compile
```

Result:

- PASS `PersistedOhlcvQueryServiceTest`
- PASS `PersistedOhlcvBarMapperIntegrationTest`
- PASS compile
- PASS test-compile

## 10. Current Conclusion

BACKEND-P12 adds the first read-only readiness layer above the persisted OHLCV skeleton.

The service can classify persisted OHLCV windows as `FRESH`, `STALE`, `PARTIAL`, `MISSING`, `UNKNOWN`, or `INVALID`.

RuntimeKlineContext and SourceTrace remain incomplete and unwired.

Future phases may consume this readiness result only after preserving fail-closed behavior, review-only defaults, and explicit source ownership checks.
