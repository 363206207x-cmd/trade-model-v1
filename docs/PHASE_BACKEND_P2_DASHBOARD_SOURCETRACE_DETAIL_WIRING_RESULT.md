# PHASE_BACKEND_P2_DASHBOARD_SOURCETRACE_DETAIL_WIRING_RESULT

## 1. Result Object

This document records the BACKEND-P2 Dashboard SourceTrace and DerivativesRiskContext Detail Wiring Pack.

The package is a minimal read-only, fail-closed implementation for `/api/dashboard/detail`.

It does not connect external derivatives data, does not generate trading instructions, and does not modify the dashboard page.

## 2. Files Changed

Production code:

- `src/main/java/org/example/trademodel/controller/DashboardController.java`
- `src/main/java/org/example/trademodel/vo/DashboardDetailResponseVO.java`
- `src/main/java/org/example/trademodel/service/dashboard/DashboardSourceTraceDetailAdapter.java`
- `src/main/java/org/example/trademodel/service/dashboard/DefaultDashboardSourceTraceDetailAdapter.java`

Tests:

- `src/test/java/org/example/trademodel/controller/DashboardControllerTest.java`
- `src/test/java/org/example/trademodel/vo/DashboardDetailResponseVOTest.java`
- `src/test/java/org/example/trademodel/service/dashboard/DefaultDashboardSourceTraceDetailAdapterTest.java`

Docs:

- `docs/PHASE_BACKEND_P2_DASHBOARD_SOURCETRACE_DETAIL_WIRING_RESULT.md`

Removed temporary trigger artifact:

- `docs/PHASE_BACKEND_P2_CLOUD_TRIGGER.md`

## 3. Implementation Summary

BACKEND-P2 adds a dashboard detail assembly boundary:

- `DashboardSourceTraceDetailAdapter`
- `DefaultDashboardSourceTraceDetailAdapter`

The adapter builds explicit read-only SourceTrace and DerivativesRiskContext objects when production runtime / derivatives sources are not available.

The response now exposes:

- `sourceTrace`
- `derivativesRiskContext`

The controller passes `sourceTrace` into the stricter `ExecutionPlanDisplayAdapter` overload.

Because production source fields are still missing, the new wiring remains fail-closed:

- `sourceTrace.fallbackStatus = INCOMPLETE`
- `derivativesRiskContext.fallbackStatus = SAFE_FAIL_CLOSED_ONLY`
- `manualReviewRequired = true`
- `notTradeInstruction = true`

## 4. Remaining Missing SourceTrace Fields

The fail-closed SourceTrace response explicitly lists:

- `runtimeKlineContext`
- `timeframe`
- `latestPrice`
- `dataQualityScore`
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
- `derivativesRiskContext`

## 5. Remaining Missing DerivativesRiskContext Fields

The fail-closed DerivativesRiskContext response explicitly lists:

- `timeframe`
- `contextTime`
- `openInterestHistory`
- `fundingHistory`
- `liquidationCluster`
- `leverageDistribution`
- `longShortRatio`
- `liquidityStress`
- `liquidityStressReason`
- `eventWindowBlockers`
- `wickConfirmationSources`
- `dataQualityScore`

## 6. Dashboard Detail Behavior

`/api/dashboard/detail` now has a visible read-only source context boundary.

When production runtime and derivatives sources are missing:

- SourceTrace is present but incomplete.
- DerivativesRiskContext is present but safe-fail-closed.
- ExecutionPlan remains gated by PlanBoundary and SourceTrace.
- No execution action is emitted.
- No entry / stop / TP numeric values are fabricated.

## 7. Tests

Focused tests cover:

- Dashboard detail exposes fail-closed SourceTrace fields.
- Dashboard detail exposes fail-closed DerivativesRiskContext fields.
- SourceTrace missing fields include entry / stop / TP / RR / liquidity / multi-timeframe / event / wick sources.
- DerivativesRiskContext missing fields include OI / funding / liquidation / leverage / long-short / liquidity / event / wick sources.
- Safety defaults remain true.
- The new detail adapter does not mark source completeness as ready.

## 8. Boundary Confirmations

Confirmed unchanged:

- No `dashboard.html` changes.
- No schema changes.
- No config changes.
- No external API integration.
- No Coinglass integration.
- No live exchange / live derivatives calls.
- No order API.
- No auto-trading.
- No executable trade instruction generation.
- No fabricated entry / stop / take-profit values.
- `VALID` remains manual-review / not-trade-instruction.
- Missing SourceTrace / derivatives-risk context remains fail-closed.

## 9. Current Conclusion

BACKEND-P2 closes the first dashboard detail wiring gap by exposing explicit SourceTrace and DerivativesRiskContext objects in `/api/dashboard/detail`.

The implementation is intentionally partial and fail-closed because production runtime / derivatives sources are not available in this package.

The next backend package can wire real production sources into this boundary, but it must keep the same missing-field and safety semantics.
