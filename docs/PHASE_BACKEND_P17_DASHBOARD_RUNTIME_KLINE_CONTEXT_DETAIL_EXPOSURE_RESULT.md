# PHASE_BACKEND_P17_DASHBOARD_RUNTIME_KLINE_CONTEXT_DETAIL_EXPOSURE_RESULT

## 1. Document Purpose

This document records the BACKEND-P17 Dashboard RuntimeKline Context Detail Exposure Pack result.

Issue context:

- `#119 BACKEND-P17 Dashboard RuntimeKline Context Detail Exposure Pack`

PR context:

- `#120`

Baseline:

- `a1a6d0e feat(backend): wire RuntimeKline assembly boundary`

BACKEND-P17 extends `/api/dashboard/detail` with a separate read-only `runtimeKlineContext` field.

The field is populated through the existing `DefaultDashboardRuntimeKlineContextAdapter` path.

It exposes only safe RuntimeKline boundary metadata and assembled kline context fields.

SourceTrace remains incomplete.

Boundary source fields remain missing.

## 2. Files Changed

Production files:

- `src/main/java/org/example/trademodel/vo/DashboardDetailResponseVO.java`
- `src/main/java/org/example/trademodel/controller/DashboardController.java`
- `src/main/java/org/example/trademodel/service/dashboard/DashboardSourceTraceDetailAdapter.java`
- `src/main/java/org/example/trademodel/service/dashboard/DefaultDashboardSourceTraceDetailAdapter.java`

Test files:

- `src/test/java/org/example/trademodel/vo/DashboardDetailResponseVOTest.java`
- `src/test/java/org/example/trademodel/controller/DashboardControllerTest.java`
- `src/test/java/org/example/trademodel/service/dashboard/DefaultDashboardSourceTraceDetailAdapterTest.java`

Documentation:

- `docs/PHASE_BACKEND_P17_DASHBOARD_RUNTIME_KLINE_CONTEXT_DETAIL_EXPOSURE_RESULT.md`

Removed temporary trigger artifact:

- `docs/P17_TRIGGER.md`

## 3. Dashboard Detail Exposure

`DashboardDetailResponseVO` now has a separate field:

- `runtimeKlineContext`

This field is independent from:

- `sourceTrace`
- `derivativesRiskContext`
- `planBoundaryDisplay`
- `executionPlanDisplay`

`DashboardController.dashboardDetail(...)` sets the field from the `DashboardSourceTraceDetailContext` returned by `DashboardSourceTraceDetailAdapter`.

The RuntimeKline context comes from the same existing dashboard RuntimeKline adapter path already used for SourceTrace readiness metadata.

The controller does not fetch live market data.

The controller does not call external APIs.

The controller does not generate entry, stop, TP, RR, liquidity, event, or wick sources.

## 4. Adapter Context Boundary

`DashboardSourceTraceDetailAdapter.DashboardSourceTraceDetailContext` now carries:

- `SourceTraceDTO sourceTrace`
- `RuntimeKlineContextDTO runtimeKlineContext`
- `DerivativesRiskContextDTO derivativesRiskContext`

`DefaultDashboardSourceTraceDetailAdapter` builds the RuntimeKline context once, then:

1. Uses it to copy readiness metadata into `SourceTraceDTO`.
2. Returns it separately for `/api/dashboard/detail`.

This avoids a second readiness query.

It also keeps SourceTrace and RuntimeKline boundaries separate.

## 5. RuntimeKline Fields Exposed

The dashboard detail response may expose the following RuntimeKline fields:

| Field | Source | Boundary |
|---|---|---|
| `symbol` | request / decision / readiness metadata | Read-only runtime metadata |
| `timeframe` | decision / readiness metadata | Read-only runtime metadata |
| `fallbackStatus` | RuntimeKline assembly boundary | Fail-closed status |
| `latestPrice` | latest selected closed persisted bar `closePrice` | Runtime latest price only |
| `klineItems` | safe selected persisted closed OHLCV bars | Runtime kline context only |
| `persistedOhlcvReadinessStatus` | persisted OHLCV readiness | Metadata |
| `persistedOhlcvStaleReasonCode` | persisted OHLCV readiness | Metadata |
| `persistedOhlcvStaleReasonText` | persisted OHLCV readiness | Metadata |
| `persistedOhlcvMissingFields` | persisted OHLCV readiness | Metadata |
| `missingFields` | RuntimeKline assembly boundary | Diagnostics |
| `manualReviewRequired` | safety default | Always true |
| `notTradeInstruction` | safety default | Always true |

`latestPrice` may only come from latest selected closed persisted bar `closePrice`.

It must not be treated as:

- entry source
- stop source
- TP source
- RR source
- order price
- execution price

## 6. Fields Not Exposed As Complete Boundary Sources

BACKEND-P17 does not populate:

- `entryPriceSource`
- `entrySourceType`
- `entrySourceTimeframe`
- `entrySourceReason`
- `entrySourceRef`
- `stopPriceSource`
- `stopSourceType`
- `stopSourceTimeframe`
- `stopSourceReason`
- `stopSourceRef`
- `tpPriceSources`
- `tpSourceType`
- `tpSourceTimeframe`
- `tpSourceReason`
- `tpSourceRef`
- `rrSource`
- `rrRuleRef`
- `liquiditySource`
- `multiTimeframeSource`
- `eventSource`
- `wickSource`

These fields remain null or empty unless a future package wires real verified sources.

## 7. SourceTrace Boundary

SourceTrace remains fail-closed.

Even when `runtimeKlineContext` is safe enough to expose selected closed-bar runtime metadata, `sourceTrace` still reports:

- `runtimeKlineContextStatus = UNAVAILABLE`
- `runtimeKlineContextSource = dashboardDetail.noRuntimeKlineContext`
- `fallbackStatus = INCOMPLETE`
- missing `runtimeKlineContext`
- missing entry source
- missing stop source
- missing TP source
- missing RR source
- missing liquidity source
- missing event source
- missing wick source

`SourceTraceDTO.hasRequiredBoundarySources()` remains false.

RuntimeKline exposure does not complete SourceTrace.

RuntimeKline exposure does not upgrade BoundaryCandidateService VALID output.

RuntimeKline exposure does not upgrade ExecutionPlan readiness.

## 8. Fail-Closed Behavior

For non-FRESH or unsafe paths, the separate RuntimeKline field remains fail-closed.

Fail-closed paths include:

- null readiness result
- missing readiness service
- readiness query failure
- `STALE`
- `PARTIAL`
- `MISSING`
- `UNKNOWN`
- `INVALID`
- FRESH readiness with no selected bars
- FRESH readiness with open candles
- FRESH readiness with unsafe bar fields
- FRESH readiness with deleted rows
- FRESH readiness with missing source ownership

Fail-closed output keeps:

- `fallbackStatus = INCOMPLETE`
- `latestPrice = null`
- `klineItems = []`
- diagnostic `missingFields`
- `manualReviewRequired = true`
- `notTradeInstruction = true`

## 9. Tests Run

Commands:

```bash
./mvnw -q -Dtest=DashboardDetailResponseVOTest test
./mvnw -q -Dtest=DefaultDashboardRuntimeKlineContextAdapterTest test
./mvnw -q -Dtest=DefaultDashboardSourceTraceDetailAdapterTest test
./mvnw -q -Dtest=DashboardControllerTest test
./mvnw -q -Dtest=RuntimeKlineContextAssemblyServiceImplTest test
./mvnw -q -DskipTests compile
./mvnw -q -DskipTests test-compile
```

Expected result:

- PASS

## 10. Boundary Confirmations

BACKEND-P17 keeps the following boundaries:

- no `dashboard.html` change
- no schema change
- no config change
- no external data integration
- no Coinglass integration
- no order API
- no auto-trading
- no live dashboard fetch
- no entry generation
- no stop generation
- no TP generation
- no RR generation
- no liquidity source generation
- no event source generation
- no wick source generation
- no BoundaryCandidateService VALID upgrade
- no ExecutionPlan readiness upgrade

## 11. Current Conclusion

BACKEND-P17 exposes a separate read-only `runtimeKlineContext` field from `/api/dashboard/detail`.

The field is safe RuntimeKline metadata only.

It may include latest selected closed persisted bar `closePrice` as runtime `latestPrice`.

It may include assembled closed-bar `klineItems`.

It does not complete SourceTrace.

It does not make `latestPrice` an entry source.

It does not create trading instructions.

It preserves `manualReviewRequired=true`.

It preserves `notTradeInstruction=true`.
