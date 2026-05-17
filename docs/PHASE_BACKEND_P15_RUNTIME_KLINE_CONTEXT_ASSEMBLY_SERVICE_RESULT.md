# PHASE_BACKEND_P15_RUNTIME_KLINE_CONTEXT_ASSEMBLY_SERVICE_RESULT

## 1. Document Purpose

This document records the BACKEND-P15 RuntimeKlineContext Assembly Service Pack result.

Issue context:

- `#115 BACKEND-P15 RuntimeKlineContext Assembly Service Pack`

PR context:

- `#116`

Baseline:

- `a056449 docs(backend): add RuntimeKline assembly contract`

BACKEND-P15 adds a minimal `RuntimeKlineContextAssemblyService` that consumes `PersistedOhlcvReadinessResult`.

It assembles only safe runtime kline fields from persisted closed OHLCV bars.

It does not wire this service into dashboard detail.

It does not complete SourceTrace.

It does not generate entry / stop / TP / RR values.

## 2. Files Changed

Production files:

- `src/main/java/org/example/trademodel/dto/planboundary/RuntimeKlineContextDTO.java`
- `src/main/java/org/example/trademodel/dto/planboundary/RuntimeKlineItemDTO.java`
- `src/main/java/org/example/trademodel/service/RuntimeKlineContextAssemblyService.java`
- `src/main/java/org/example/trademodel/service/impl/RuntimeKlineContextAssemblyServiceImpl.java`

Test files:

- `src/test/java/org/example/trademodel/service/impl/RuntimeKlineContextAssemblyServiceImplTest.java`

Documentation:

- `docs/PHASE_BACKEND_P15_RUNTIME_KLINE_CONTEXT_ASSEMBLY_SERVICE_RESULT.md`

Removed temporary trigger artifact:

- `docs/P15_TRIGGER.md`

## 3. Service Status

Added:

- `RuntimeKlineContextAssemblyService`
- `RuntimeKlineContextAssemblyServiceImpl`

The service accepts:

- `PersistedOhlcvReadinessResult`

The service returns:

- `RuntimeKlineContextDTO`

The service is not called by:

- dashboard detail
- SourceTrace adapter
- BoundaryCandidateService
- ExecutionPlan readiness
- RuleEngine
- order API

This keeps the implementation isolated and testable.

## 4. Safe Runtime Fields Mapped

For eligible `FRESH` readiness with safe persisted closed bars, the service maps only:

| Runtime field | Source |
|---|---|
| `symbol` | `PersistedOhlcvReadinessResult.symbol` |
| `timeframe` | `PersistedOhlcvReadinessResult.timeframe` |
| `latestPrice` | latest selected closed persisted bar `closePrice` |
| `klineItems` | supported fields from selected persisted closed bars |
| `persistedOhlcvReadinessStatus` | readiness status |
| `persistedOhlcvStaleReasonCode` | stale reason code |
| `persistedOhlcvStaleReasonText` | stale reason text |
| `persistedOhlcvMissingFields` | readiness missing fields |
| `missingFields` | empty only for eligible safe `FRESH` assembly |
| `manualReviewRequired` | always `true` |
| `notTradeInstruction` | always `true` |

The newly added `RuntimeKlineItemDTO` carries only safe persisted OHLCV item fields:

- open time
- close time
- open price
- high price
- low price
- close price
- volume
- provider
- provider market type
- source endpoint
- source batch id
- source trace id
- source version
- ingested time
- quality status

## 5. Fields Not Generated

BACKEND-P15 intentionally does not populate:

- entry price source
- entry source type
- entry source timeframe
- entry source reason
- entry source ref
- stop price source
- stop source type
- stop source timeframe
- stop source reason
- stop source ref
- TP price sources
- TP source type
- TP source timeframe
- TP source reason
- TP source ref
- RR source
- RR rule ref
- liquidity source
- multi-timeframe source
- event source
- wick source

RuntimeKline assembly is not a boundary source assembler.

`latestPrice` comes only from the latest selected closed bar close price.

`latestPrice` is not an entry source.

## 6. Fail-Closed Behavior

The service returns `fallbackStatus=INCOMPLETE` for:

- null readiness result
- non-`FRESH` readiness
- stale readiness
- partial readiness
- missing readiness
- unknown readiness
- invalid readiness
- empty bars
- too few bars
- non-contiguous bars
- open candles
- deleted bars
- missing source ownership
- missing provider
- missing provider market type
- missing source endpoint
- missing source batch id
- missing source trace id
- missing source version
- missing ingestion time
- non-OK quality status
- invalid OHLC price fields
- missing or invalid volume
- mismatched latest close time
- unsafe readiness safety defaults

Fail-closed outputs keep:

- `latestPrice = null`
- `klineItems = []`
- `manualReviewRequired = true`
- `notTradeInstruction = true`

## 7. FRESH Eligibility

`FRESH` readiness is eligible only when all P14 safety checks pass.

The service verifies:

- symbol is present
- timeframe is present
- required window size is positive
- stale reason code is `NONE`
- readiness missing fields are empty
- bars are present
- selected bar count satisfies required window size
- bars are closed
- bars are non-deleted
- bars have valid OHLC fields
- bars have non-negative volume
- bars are contiguous for the timeframe
- bars have source ownership fields
- bars have `qualityStatus=OK`
- latest close time matches the latest selected closed bar
- latest ingested time is present
- readiness safety defaults are true

If any condition fails, assembly fails closed.

## 8. Safety Defaults

Every path preserves:

- `manualReviewRequired=true`
- `notTradeInstruction=true`

Even if the input readiness result incorrectly carries false safety defaults, the output resets them to true and fails closed.

No caller can disable these flags through the assembly service.

## 9. Tests Run

Commands:

```bash
./mvnw -q -Dtest=RuntimeKlineContextAssemblyServiceImplTest test
./mvnw -q -Dtest=DefaultDashboardRuntimeKlineContextAdapterTest test
./mvnw -q -DskipTests compile
./mvnw -q -DskipTests test-compile
```

Result:

- PASS `RuntimeKlineContextAssemblyServiceImplTest`
- PASS `DefaultDashboardRuntimeKlineContextAdapterTest`
- PASS compile
- PASS test-compile

## 10. Focused Test Coverage

`RuntimeKlineContextAssemblyServiceImplTest` covers:

- `FRESH` readiness maps safe runtime fields only
- `latestPrice` comes from latest selected closed bar close price only
- `STALE` readiness fails closed
- `PARTIAL` readiness fails closed
- `MISSING` readiness fails closed
- `UNKNOWN` readiness fails closed
- `INVALID` readiness fails closed
- null readiness result fails closed
- `FRESH` readiness with no bars fails closed
- `FRESH` readiness with open candle fails closed
- `FRESH` readiness with deleted bar fails closed
- `FRESH` readiness with missing source ownership fails closed
- `FRESH` readiness with unsafe OHLC fields fails closed
- unsafe input safety defaults fail closed
- output safety defaults remain true

## 11. Boundary Confirmations

BACKEND-P15 confirms:

- no dashboard detail wiring
- no `dashboard.html` change
- no schema change
- no live dashboard fetch
- no external data integration
- no Coinglass integration
- no order API
- no auto-trading
- no SourceTrace completion
- no BoundaryCandidateService VALID production path
- no ExecutionPlan readiness
- no entry / stop / TP / RR generation
- no latestPrice-to-entry mapping
- VALID remains manual-review / not-trade-instruction

## 12. Unwired Fields And Modules

The following remain unwired:

- dashboard detail RuntimeKline assembly
- SourceTrace completion
- DerivativesRiskContext completion
- BoundaryCandidateService VALID production path
- ExecutionPlan readiness
- RuleEngine readiness
- PlanReadiness
- live dashboard fetch
- entry source
- stop source
- TP source
- RR source
- liquidity source
- multi-timeframe source
- event source
- wick source

## 13. Current Conclusion

BACKEND-P15 adds a focused RuntimeKlineContext assembly service over persisted OHLCV readiness results.

The service can assemble safe runtime kline context fields only when readiness is `FRESH` and all persisted bar safety checks pass.

All non-eligible inputs fail closed as `INCOMPLETE`.

The service remains isolated and is not wired into dashboard detail or SourceTrace.

Future packages may decide where to consume this service, but must preserve fail-closed behavior, review-only boundaries, and non-trading semantics.
