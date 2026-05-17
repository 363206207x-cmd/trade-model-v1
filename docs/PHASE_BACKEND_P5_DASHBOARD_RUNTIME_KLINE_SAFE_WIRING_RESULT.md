# PHASE_BACKEND_P5_DASHBOARD_RUNTIME_KLINE_SAFE_WIRING_RESULT

## 1. Result Purpose

This document records the BACKEND-P5 Dashboard RuntimeKline Minimal Safe Wiring Pack.

The implementation wires only production-backed metadata fields that are already available from the dashboard decision read model.

It does not complete SourceTrace.

It does not complete RuntimeKlineContext.

It does not generate entry, stop, take-profit, RR, liquidity, event, or wick production values.

## 2. Baseline And Scope

| Item | Value |
|---|---|
| Target PR | #88 |
| Target issue | #87 |
| Baseline | `b347621 Merge pull request #86 from 363206207x-cmd/codex/backend-p4-runtime-kline-readiness` |
| Package | BACKEND-P5 RuntimeKline minimal safe metadata wiring |
| Dashboard HTML | Not modified |
| Schema | Not modified |
| External API integration | Not added |
| Order API | Not added |
| Auto-trading | Not added |

## 3. Changed Files

| File | Purpose |
|---|---|
| `src/main/java/org/example/trademodel/dto/planboundary/SourceTraceDTO.java` | Adds quote/data-quality metadata carrier fields that do not participate in source completeness. |
| `src/main/java/org/example/trademodel/dto/planboundary/DerivativesRiskContextDTO.java` | Adds data-quality source label metadata. |
| `src/main/java/org/example/trademodel/service/dashboard/DefaultDashboardSourceTraceDetailAdapter.java` | Wires only safe decision read-model metadata into SourceTrace / DerivativesRiskContext. |
| `src/test/java/org/example/trademodel/service/dashboard/DefaultDashboardSourceTraceDetailAdapterTest.java` | Verifies safe metadata wiring and fail-closed completeness behavior. |
| `docs/PHASE_BACKEND_P5_DASHBOARD_RUNTIME_KLINE_SAFE_WIRING_RESULT.md` | Records this result package. |

The temporary cloud trigger artifact was removed from the final PR package.

## 4. Wired Fields

| Field | Source | Target | Classification | Safety Boundary |
|---|---|---|---|---|
| `quoteLatestPrice` | `DecisionResultVO.latestPrice` | `SourceTraceDTO` | `REAL_PARTIAL` | Quote/display metadata only. Not entry, stop, TP, RR, or execution readiness. |
| `quoteLatestPriceSource` | Constant label `DecisionResultVO.latestPrice` | `SourceTraceDTO` | metadata | Source label only. |
| `quotePriceUpdateTimeMs` | `DecisionResultVO.priceUpdateTimeMs` | `SourceTraceDTO` | `REAL_PARTIAL` | Quote freshness metadata only. Not kline stale status. |
| `quotePriceUpdateTimeSource` | Constant label `DecisionResultVO.priceUpdateTimeMs` | `SourceTraceDTO` | metadata | Source label only. |
| `dataQualityScore` | `DecisionResultVO.dataQualityScore` | `SourceTraceDTO` | `REAL_PARTIAL` | Metadata only. Does not complete SourceTrace. |
| `dataQualityScoreSource` | Constant label `DecisionResultVO.dataQualityScore` | `SourceTraceDTO` | metadata | Source label only. |
| `dataQualityScoreSource` | Constant label `DecisionResultVO.dataQualityScore` | `DerivativesRiskContextDTO` | metadata | Source label for existing data-quality metadata. |
| `multiTimeframeSource` | `DecisionResultVO.multiTfConvergence` | `SourceTraceDTO` | `REAL_PARTIAL` | Existing partial label-only wiring preserved. |

## 5. Fields Intentionally Kept Missing

The following fields remain missing / fail-closed:

- `runtimeKlineContext`
- `timeframe`
- `latestPrice` as a RuntimeKline completeness field
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
- `eventSource`
- `wickSource`
- full `derivativesRiskContext`

The derivatives-risk context still keeps missing:

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

## 6. SourceTrace Completeness Result

`SourceTraceDTO.hasRequiredBoundarySources()` remains false for dashboard detail metadata-only output.

Reason:

- quote metadata is not boundary source evidence,
- data quality is not boundary source evidence,
- multi-timeframe label is partial only,
- entry / stop / TP / RR numeric sources remain absent,
- liquidity / event / wick sources remain absent,
- fallback status remains `INCOMPLETE`.

## 7. RuntimeKline Completeness Result

RuntimeKlineContext remains incomplete.

BACKEND-P5 does not build or expose a complete RuntimeKlineContext object.

The latest price metadata is explicitly named `quoteLatestPrice` so it cannot be confused with:

- entry source,
- stop source,
- TP source,
- RR source,
- kline stale status,
- execution readiness.

## 8. Text Execution Plan Boundary

Text fields remain excluded from numeric boundary wiring.

The implementation does not parse or convert:

- `entryZone`
- `stopLoss`
- `takeProfitRules`
- `executionPlanSummary`
- `recommendedAction`
- `leverageSuggestion`
- `positionSuggestion`

These fields must not become entry / stop / TP / RR numeric sources.

## 9. Tests Run

The following tests were run:

```bash
./mvnw -q -Dtest=DefaultDashboardSourceTraceDetailAdapterTest test
./mvnw -q -Dtest=DashboardControllerTest test
./mvnw -q -DskipTests compile
./mvnw -q -DskipTests test-compile
```

Result:

- PASS

## 10. Boundary Confirmations

This package confirms:

- no `dashboard.html` change,
- no schema change,
- no external API integration,
- no Coinglass integration,
- no live exchange integration added,
- no order API,
- no auto-trading,
- no executable ExecutionPlan,
- no entry / stop / TP production numeric values,
- latest price is not treated as entry source,
- SourceTrace remains incomplete,
- RuntimeKlineContext remains incomplete,
- BoundaryCandidate `VALID` remains manual-review and not-trade-instruction,
- missing SourceTrace / derivatives-risk context remains fail-closed.

## 11. Current Conclusion

BACKEND-P5 safely exposes only quote and data-quality metadata that was already available from the dashboard decision read model.

The implementation deliberately keeps SourceTrace and RuntimeKline incomplete.

BACKEND-P6 or a later backend phase may consider persisted timeframe or runtime-context assembly only if the source ownership is explicit, tested, and still fail-closed when incomplete.
