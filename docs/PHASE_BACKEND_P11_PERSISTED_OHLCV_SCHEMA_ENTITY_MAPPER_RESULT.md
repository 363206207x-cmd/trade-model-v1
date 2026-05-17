# PHASE_BACKEND_P11_PERSISTED_OHLCV_SCHEMA_ENTITY_MAPPER_RESULT

## 1. Document Purpose

This document records the BACKEND-P11 Persisted OHLCV Schema Entity Mapper Skeleton Pack result.

Issue context:

- `#103 BACKEND-P11 Persisted OHLCV Schema Entity Mapper Skeleton Pack`

Baseline:

- `beb2849 docs(backend): add P10 persisted OHLCV source contract`

P11 implements the foundational persisted OHLCV skeleton that P10 defined.

It does not wire RuntimeKlineContext or SourceTrace.

## 2. Files Changed

Added / updated files:

- `src/main/resources/schema.sql`
- `src/main/java/org/example/trademodel/entity/PersistedOhlcvBarDO.java`
- `src/main/java/org/example/trademodel/mapper/PersistedOhlcvBarMapper.java`
- `src/test/java/org/example/trademodel/mapper/PersistedOhlcvBarMapperIntegrationTest.java`
- `docs/PHASE_BACKEND_P11_PERSISTED_OHLCV_SCHEMA_ENTITY_MAPPER_RESULT.md`

Removed temporary trigger artifact:

- `docs/P11_TRIGGER.md`

## 3. Schema Status

Added table:

- `tm_persisted_ohlcv_bar`

The table is a local persisted OHLCV read-model skeleton.

It includes:

- symbol / timeframe,
- open / close time,
- OHLC price fields,
- volume fields,
- closed-candle flag,
- provider and market type,
- source endpoint,
- source batch id,
- source trace id,
- source version,
- ingestion and update timestamps,
- quality status and reason,
- raw payload hash,
- soft-delete marker.

Indexes added:

- unique source key on symbol / timeframe / open time / provider / provider market type,
- window query index on symbol / timeframe / close time,
- ingestion-time index,
- source batch index,
- source trace index.

## 4. Entity Status

Added:

- `PersistedOhlcvBarDO`

The entity mirrors the table fields.

It is a passive persistence object only.

It does not:

- compute stale status,
- compute freshness,
- generate RuntimeKlineContext,
- generate entry / stop / TP,
- call external APIs,
- produce trading actions.

## 5. Mapper Status

Added:

- `PersistedOhlcvBarMapper`

Mapper methods:

- `selectLatestClosedWindow(symbol, timeframe, limit)`
- `selectBySymbolTimeframeAndOpenTime(symbol, timeframe, openTimeMs)`
- `selectLatestIngestionBatch(symbol, timeframe)`

The mapper is read-only for production use.

The test uses `JdbcTemplate` to insert fixtures, then verifies mapper reads.

The mapper does not include a production insert method.

## 6. Focused Test Coverage

Added:

- `PersistedOhlcvBarMapperIntegrationTest`

Covered behavior:

- latest closed window returns closed and non-deleted rows only,
- latest closed window is ordered by close time descending,
- latest closed window honors limit,
- open candles are excluded from closed-window reads,
- deleted rows are excluded,
- source ownership fields round-trip,
- OHLC and volume fields round-trip,
- quality status fields round-trip,
- latest ingestion batch metadata returns newest non-deleted row.

## 7. Unwired Fields And Boundaries

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
- wick source,
- stale-status calculation,
- stale reason calculation,
- freshness policy evaluation.

This is intentional.

The P11 skeleton only creates the local persisted source layer needed before future fail-closed readiness evaluation.

## 8. Boundary Confirmations

BACKEND-P11 confirms:

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
./mvnw -q -Dtest=PersistedOhlcvBarMapperIntegrationTest test
./mvnw -q -DskipTests compile
./mvnw -q -DskipTests test-compile
```

Result:

- PASS `PersistedOhlcvBarMapperIntegrationTest`
- PASS compile
- PASS test-compile

## 10. Current Conclusion

BACKEND-P11 adds a minimal persisted OHLCV schema/entity/mapper skeleton consistent with the P10 contract.

The implementation is intentionally read-only from the mapper boundary.

RuntimeKlineContext and SourceTrace remain incomplete and unwired.

Future phases may add a readiness query service and stale-status policy, but must stay fail-closed until persisted data exists and passes source ownership, freshness, quality, and continuity checks.
