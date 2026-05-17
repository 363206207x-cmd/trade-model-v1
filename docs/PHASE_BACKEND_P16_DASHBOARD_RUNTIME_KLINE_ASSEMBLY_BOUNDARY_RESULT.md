# PHASE_BACKEND_P16_DASHBOARD_RUNTIME_KLINE_ASSEMBLY_BOUNDARY_RESULT

## 1. Document Purpose

This document records the BACKEND-P16 Dashboard RuntimeKline Assembly Boundary Integration Pack result.

Issue context:

- `#117 BACKEND-P16 Dashboard RuntimeKline Assembly Boundary Integration Pack`

PR context:

- `#118`

Baseline:

- `d8e896f feat(backend): add RuntimeKline assembly service`

BACKEND-P16 lets `DefaultDashboardRuntimeKlineContextAdapter` call `RuntimeKlineContextAssemblyService` after persisted OHLCV readiness evaluation.

The dashboard RuntimeKline boundary can now expose safe assembled runtime kline fields when the P15 assembly service accepts `FRESH` persisted OHLCV readiness and safe persisted closed bars.

SourceTrace remains incomplete.

Boundary sources remain missing.

## 2. Files Changed

Production files:

- `src/main/java/org/example/trademodel/service/dashboard/DefaultDashboardRuntimeKlineContextAdapter.java`

Test files:

- `src/test/java/org/example/trademodel/service/dashboard/DefaultDashboardRuntimeKlineContextAdapterTest.java`
- `src/test/java/org/example/trademodel/service/dashboard/DefaultDashboardSourceTraceDetailAdapterTest.java`

Documentation:

- `docs/PHASE_BACKEND_P16_DASHBOARD_RUNTIME_KLINE_ASSEMBLY_BOUNDARY_RESULT.md`

Removed temporary trigger artifact:

- `docs/P16_TRIGGER.md`

## 3. RuntimeKline Adapter Integration

`DefaultDashboardRuntimeKlineContextAdapter` now depends on:

- `PersistedOhlcvQueryService`
- `RuntimeKlineContextAssemblyService`

The adapter flow is:

1. Build a dashboard RuntimeKline boundary object.
2. Evaluate persisted OHLCV readiness through `PersistedOhlcvQueryService`.
3. Pass the readiness result to `RuntimeKlineContextAssemblyService`.
4. Copy only safe assembled runtime fields into the dashboard RuntimeKline boundary when assembly is complete and safe.
5. Preserve `INCOMPLETE` fail-closed behavior for all non-FRESH or unsafe paths.

The adapter does not call:

- SourceTrace completion
- BoundaryCandidateService
- ExecutionPlan readiness
- RuleEngine
- order API

## 4. Assembled Fields Exposed

When assembly is safe, the dashboard RuntimeKline boundary may expose:

| Field | Source | Boundary |
|---|---|---|
| `symbol` | decision/request/readiness symbol | Safe runtime metadata only |
| `timeframe` | decision/readiness timeframe | Safe runtime metadata only |
| `latestPrice` | latest selected closed persisted bar `closePrice` | Runtime latest price only; not entry source |
| `klineItems` | selected persisted closed OHLCV bars | Safe OHLCV item metadata |
| `persistedOhlcvReadinessStatus` | readiness status | Metadata |
| `persistedOhlcvStaleReasonCode` | readiness stale reason code | Metadata |
| `persistedOhlcvStaleReasonText` | readiness stale reason text | Metadata |
| `persistedOhlcvMissingFields` | readiness missing fields | Metadata |
| `manualReviewRequired` | safety default | Always true |
| `notTradeInstruction` | safety default | Always true |

`latestPrice` may only come from the latest selected closed persisted bar `closePrice`.

It must not be treated as:

- entry source
- stop source
- TP source
- RR source
- order price
- execution price

## 5. Fields Still Not Populated

BACKEND-P16 does not populate:

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

The RuntimeKline boundary remains separate from SourceTrace boundary-source completion.

## 6. Fail-Closed Behavior

The dashboard RuntimeKline boundary remains `INCOMPLETE` when:

- persisted OHLCV readiness service is unavailable
- persisted OHLCV readiness query fails
- readiness result is null
- readiness is `STALE`
- readiness is `PARTIAL`
- readiness is `MISSING`
- readiness is `UNKNOWN`
- readiness is `INVALID`
- `FRESH` readiness has no bars
- `FRESH` readiness has open candles
- `FRESH` readiness has deleted bars
- `FRESH` readiness has unsafe OHLCV fields
- `FRESH` readiness has missing source ownership
- assembly service returns missing fields

Fail-closed output keeps:

- `fallbackStatus=INCOMPLETE`
- `latestPrice=null`
- `klineItems=[]`
- `manualReviewRequired=true`
- `notTradeInstruction=true`

## 7. SourceTrace Boundary

`DefaultDashboardSourceTraceDetailAdapter` remains fail-closed.

Even if the RuntimeKline boundary can assemble safe runtime kline fields, SourceTrace still reports:

- `runtimeKlineContextStatus = UNAVAILABLE`
- `runtimeKlineContextSource = dashboardDetail.noRuntimeKlineContext`
- `fallbackStatus = INCOMPLETE`
- `runtimeKlineContext` missing
- entry source missing
- stop source missing
- TP source missing
- RR source missing
- liquidity source missing
- event source missing
- wick source missing

`SourceTraceDTO.hasRequiredBoundarySources()` remains false.

This is intentional.

RuntimeKline assembly is not SourceTrace completion.

## 8. Tests Run

Commands:

```bash
./mvnw -q -Dtest=DefaultDashboardRuntimeKlineContextAdapterTest test
./mvnw -q -Dtest=DefaultDashboardSourceTraceDetailAdapterTest test
./mvnw -q -Dtest=RuntimeKlineContextAssemblyServiceImplTest test
./mvnw -q -Dtest=DashboardControllerTest test
./mvnw -q -DskipTests compile
./mvnw -q -DskipTests test-compile
```

Result:

- PASS `DefaultDashboardRuntimeKlineContextAdapterTest`
- PASS `DefaultDashboardSourceTraceDetailAdapterTest`
- PASS `RuntimeKlineContextAssemblyServiceImplTest`
- PASS `DashboardControllerTest`
- PASS compile
- PASS test-compile

## 9. Focused Test Coverage

Updated tests cover:

- non-wired fallback when readiness service is unavailable
- non-FRESH readiness remains fail-closed
- `FRESH` readiness without safe bars remains fail-closed
- `FRESH` readiness with safe persisted closed bars exposes safe RuntimeKline fields
- latest price comes from latest selected closed persisted bar `closePrice`
- unsafe open candle fails closed
- SourceTrace remains incomplete even when RuntimeKline assembly is available
- entry / stop / TP / RR remain absent
- manual review and not-trade defaults remain true

## 10. Boundary Confirmations

BACKEND-P16 confirms:

- no `dashboard.html` change
- no schema change
- no live dashboard fetch
- no external data integration
- no Coinglass integration
- no order API
- no auto-trading
- no SourceTrace completion
- no BoundaryCandidateService VALID upgrade
- no ExecutionPlan readiness upgrade
- no entry / stop / TP / RR generation
- no liquidity / event / wick generation
- no latestPrice-to-entry mapping
- `manualReviewRequired=true`
- `notTradeInstruction=true`

## 11. Unwired Fields And Modules

The following remain unwired:

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

## 12. Current Conclusion

BACKEND-P16 integrates the P15 RuntimeKline assembly service into the dashboard RuntimeKline boundary.

Safe assembled runtime kline fields can now appear in the RuntimeKline boundary when persisted OHLCV readiness is `FRESH` and persisted closed bars pass assembly checks.

All non-FRESH or unsafe paths remain fail-closed as `INCOMPLETE`.

SourceTrace remains incomplete and required boundary sources remain missing.

No trading, execution, order, external API, schema, or dashboard HTML behavior was added.
