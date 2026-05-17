# PHASE_BACKEND_P13_DASHBOARD_RUNTIME_KLINE_READINESS_BOUNDARY_RESULT

## 1. Document Purpose

This document records the BACKEND-P13 Dashboard RuntimeKline Readiness Boundary Integration Pack result.

Issue context:

- `#107 BACKEND-P13 Dashboard RuntimeKline Readiness Boundary Integration Pack`

Baseline:

- `01b30de feat(backend): add persisted OHLCV readiness service`

BACKEND-P13 integrates the P12 persisted OHLCV readiness query result into the dashboard RuntimeKline boundary as metadata only.

It does not complete RuntimeKlineContext or SourceTrace.

## 2. Files Changed

Production files:

- `src/main/java/org/example/trademodel/dto/planboundary/RuntimeKlineContextDTO.java`
- `src/main/java/org/example/trademodel/dto/planboundary/SourceTraceDTO.java`
- `src/main/java/org/example/trademodel/service/dashboard/DefaultDashboardRuntimeKlineContextAdapter.java`
- `src/main/java/org/example/trademodel/service/dashboard/DefaultDashboardSourceTraceDetailAdapter.java`

Test files:

- `src/test/java/org/example/trademodel/service/dashboard/DefaultDashboardRuntimeKlineContextAdapterTest.java`
- `src/test/java/org/example/trademodel/service/dashboard/DefaultDashboardSourceTraceDetailAdapterTest.java`
- `src/test/java/org/example/trademodel/controller/DashboardControllerTest.java`

Documentation:

- `docs/PHASE_BACKEND_P13_DASHBOARD_RUNTIME_KLINE_READINESS_BOUNDARY_RESULT.md`

Removed temporary trigger artifact:

- `docs/P13_TRIGGER.md`

## 3. Readiness Metadata Exposed

RuntimeKline boundary metadata now includes:

- `persistedOhlcvReadinessStatus`
- `persistedOhlcvStaleReasonCode`
- `persistedOhlcvStaleReasonText`
- `persistedOhlcvMissingFields`

Dashboard SourceTrace metadata now mirrors this as:

- `runtimeKlineReadinessStatus`
- `runtimeKlineStaleReasonCode`
- `runtimeKlineStaleReasonText`
- `runtimeKlineReadinessMissingFields`

These fields are metadata only.

They do not mark RuntimeKlineContext complete.

They do not mark SourceTrace complete.

## 4. RuntimeKline Boundary Behavior

`DefaultDashboardRuntimeKlineContextAdapter` now asks `PersistedOhlcvQueryService` for readiness metadata when building the dashboard RuntimeKline boundary.

The adapter keeps:

- `fallbackStatus = INCOMPLETE`
- `manualReviewRequired = true`
- `notTradeInstruction = true`
- `latestPrice = null`
- `dataQualityScore = null`
- entry / stop / TP / RR sources unset

If readiness is `FRESH`, it remains display metadata only.

If readiness is `STALE`, `PARTIAL`, `MISSING`, `UNKNOWN`, or `INVALID`, it remains fail-closed metadata.

If the readiness query is unavailable or fails, the adapter returns `UNKNOWN` metadata with a fail-closed missing field.

## 5. Dashboard SourceTrace Behavior

`DefaultDashboardSourceTraceDetailAdapter` now copies RuntimeKline readiness metadata into `SourceTraceDTO`.

The SourceTrace still keeps:

- `runtimeKlineContextStatus = UNAVAILABLE`
- `runtimeKlineContextSource = dashboardDetail.noRuntimeKlineContext`
- `fallbackStatus = INCOMPLETE`
- `runtimeKlineContext` in missing fields
- entry / stop / TP / RR / liquidity / event / wick sources missing
- `manualReviewRequired = true`
- `notTradeInstruction = true`

`SourceTraceDTO.hasRequiredBoundarySources()` remains false when only readiness metadata exists.

## 6. Incomplete Fields

The following remain incomplete and unwired:

- RuntimeKlineContext assembly
- SourceTrace completion
- DerivativesRiskContext completion
- entry source
- stop source
- TP source
- RR source
- liquidity source
- multi-timeframe source
- event source
- wick source
- BoundaryCandidateService VALID production path
- ExecutionPlan readiness
- live dashboard fetch

## 7. Tests Run

Commands:

```bash
./mvnw -q -Dtest=DefaultDashboardRuntimeKlineContextAdapterTest test
./mvnw -q -Dtest=PersistedOhlcvQueryServiceTest test
./mvnw -q -Dtest=DefaultDashboardSourceTraceDetailAdapterTest test
./mvnw -q -Dtest=DashboardControllerTest test
./mvnw -q -DskipTests compile
./mvnw -q -DskipTests test-compile
```

Result:

- PASS `DefaultDashboardRuntimeKlineContextAdapterTest`
- PASS `PersistedOhlcvQueryServiceTest`
- PASS `DefaultDashboardSourceTraceDetailAdapterTest`
- PASS `DashboardControllerTest`
- PASS compile
- PASS test-compile

## 8. Boundary Confirmations

BACKEND-P13 confirms:

- no `dashboard.html` change,
- no schema change,
- no external data integration,
- no Coinglass integration,
- no live dashboard fetch,
- no RuntimeKlineContext completion,
- no SourceTrace completion,
- no DerivativesRiskContext completion,
- no entry / stop / TP / RR generation,
- no order API,
- no auto-trading,
- no latestPrice-to-entry mapping,
- quote freshness is not kline stale status,
- `FRESH` readiness is metadata only,
- VALID remains manual-review / not-trade-instruction.

## 9. Current Conclusion

BACKEND-P13 safely exposes persisted OHLCV readiness metadata in the dashboard RuntimeKline boundary.

The dashboard can now display whether persisted OHLCV readiness is `FRESH`, `STALE`, `PARTIAL`, `MISSING`, `UNKNOWN`, or `INVALID`.

RuntimeKlineContext remains incomplete.

SourceTrace remains incomplete.

Future phases may assemble RuntimeKlineContext only after explicit source assembly, completion rules, and tests are added in a separate package.
