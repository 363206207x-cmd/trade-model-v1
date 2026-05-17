# PHASE_BACKEND_P6_DASHBOARD_TIMEFRAME_QUOTE_FRESHNESS_SOURCE_OWNERSHIP_RESULT

## 1. Result Purpose

This document records the BACKEND-P6 Dashboard Timeframe Quote Freshness Source Ownership Pack.

The implementation adds only safe source ownership metadata for persisted timeframe, quote update time, and dashboard RuntimeKline availability.

It does not complete SourceTrace.

It does not complete RuntimeKlineContext.

It does not generate entry, stop, take-profit, RR, liquidity, event, or wick values.

## 2. Baseline And Scope

| Item | Value |
|---|---|
| Target PR | #90 |
| Target issue | #89 |
| Baseline | `da83cc7 feat(backend): wire safe RuntimeKline metadata` |
| Package | BACKEND-P6 source ownership metadata wiring |
| Dashboard HTML | Not modified |
| Schema | Not modified |
| External API integration | Not added |
| Order API | Not added |
| Auto-trading | Not added |

## 3. Changed Files

| File | Purpose |
|---|---|
| `src/main/java/org/example/trademodel/vo/DecisionResultVO.java` | Adds `timeframe` to the decision read model. |
| `src/main/java/org/example/trademodel/mapper/DecisionResultMapper.java` | Selects persisted `tm_analysis_run.timeframe` for dashboard decision rows. |
| `src/main/java/org/example/trademodel/dto/planboundary/SourceTraceDTO.java` | Adds timeframe source ownership, quote freshness status, and RuntimeKline unavailable marker fields. |
| `src/main/java/org/example/trademodel/dto/planboundary/DerivativesRiskContextDTO.java` | Adds timeframe source ownership metadata. |
| `src/main/java/org/example/trademodel/service/dashboard/DefaultDashboardSourceTraceDetailAdapter.java` | Wires safe ownership metadata and preserves fail-closed missing fields. |
| `src/test/java/org/example/trademodel/service/dashboard/DefaultDashboardSourceTraceDetailAdapterTest.java` | Verifies ownership wiring and incomplete SourceTrace behavior. |
| `src/test/java/org/example/trademodel/controller/DashboardControllerTest.java` | Verifies API exposure for safe ownership metadata. |
| `src/test/java/org/example/trademodel/mapper/DecisionResultMapperLatestPlanIntegrationTest.java` | Verifies mapper timeframe ownership from `tm_analysis_run`. |
| `docs/PHASE_BACKEND_P6_DASHBOARD_TIMEFRAME_QUOTE_FRESHNESS_SOURCE_OWNERSHIP_RESULT.md` | Records this result package. |

The temporary cloud trigger artifact was removed from the final PR package.

## 4. Wired Fields

| Field | Source | Target | Classification | Safety Boundary |
|---|---|---|---|---|
| `timeframe` | `tm_analysis_run.timeframe` via `DecisionResultVO.timeframe` | `SourceTraceDTO`, `DerivativesRiskContextDTO` | `REAL_PARTIAL` | Persisted analysis timeframe metadata only. Does not complete RuntimeKline. |
| `timeframeSource` | Constant label `DecisionResultVO.timeframe` | `SourceTraceDTO`, `DerivativesRiskContextDTO` | metadata | Source ownership label only. |
| `runtimeKlineContextStatus` | Adapter marker | `SourceTraceDTO` | unavailable marker | Explicitly marks dashboard detail RuntimeKline as not assembled. |
| `runtimeKlineContextSource` | Constant label `dashboardDetail.noRuntimeKlineContext` | `SourceTraceDTO` | unavailable marker | Explains why RuntimeKline remains missing. |
| `quoteFreshnessStatus` | `DecisionResultVO.priceUpdateTimeMs` presence | `SourceTraceDTO` | quote metadata | `QUOTE_UPDATE_TIME_ONLY`; not kline stale status. |

Existing BACKEND-P5 metadata remains:

- `quoteLatestPrice`
- `quoteLatestPriceSource`
- `quotePriceUpdateTimeMs`
- `quotePriceUpdateTimeSource`
- `dataQualityScore`
- `dataQualityScoreSource`
- partial `multiTimeframeSource`

## 5. Fields Intentionally Kept Missing

The following SourceTrace fields remain missing / fail-closed:

- `runtimeKlineContext`
- `latestPrice` as full RuntimeKline source
- OHLCV / kline window
- kline stale status
- `entryPriceSource`
- entry source metadata
- `stopPriceSource`
- stop source metadata
- `tpPriceSources`
- TP source metadata
- `rrSource`
- `rrRuleRef`
- `liquiditySource`
- `eventSource`
- `wickSource`
- full `derivativesRiskContext`

The following derivatives-risk fields remain missing:

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

## 6. RuntimeKline Completeness Result

RuntimeKlineContext remains incomplete.

The adapter explicitly emits:

- `runtimeKlineContextStatus = UNAVAILABLE`
- `runtimeKlineContextSource = dashboardDetail.noRuntimeKlineContext`

This marker is a safe ownership statement, not a RuntimeKline object.

The presence of timeframe and quote freshness metadata does not prove:

- OHLCV window availability,
- kline freshness,
- stale status,
- entry / stop / TP / RR numeric source readiness,
- execution readiness.

## 7. Quote Freshness Boundary

`quoteFreshnessStatus = QUOTE_UPDATE_TIME_ONLY` means the dashboard decision row has quote update-time metadata.

It does not mean:

- kline stale status is available,
- OHLCV window is fresh,
- RuntimeKlineContext is complete,
- latest price can be used as entry source,
- an ExecutionPlan can become executable.

## 8. SourceTrace Completeness Result

`SourceTraceDTO.hasRequiredBoundarySources()` remains false for dashboard detail metadata-only output.

Reason:

- fallback status remains `INCOMPLETE`,
- `runtimeKlineContext` remains missing,
- entry / stop / TP / RR numeric sources remain missing,
- liquidity / event / wick sources remain missing,
- full derivatives-risk context remains missing.

## 9. Tests Run

The following tests were run:

```bash
./mvnw -q -Dtest=DefaultDashboardSourceTraceDetailAdapterTest test
./mvnw -q -Dtest=DashboardControllerTest test
./mvnw -q -Dtest=DecisionResultMapperLatestPlanIntegrationTest test
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
- quote freshness is not treated as kline stale status,
- SourceTrace remains incomplete,
- RuntimeKlineContext remains incomplete,
- BoundaryCandidate `VALID` remains manual-review and not-trade-instruction,
- missing SourceTrace / derivatives-risk context remains fail-closed.

## 11. Current Conclusion

BACKEND-P6 safely adds source ownership metadata for persisted timeframe, quote update-time metadata, and dashboard RuntimeKline unavailability.

The implementation deliberately keeps SourceTrace and RuntimeKline incomplete.

Future backend phases may assemble a true RuntimeKlineContext only after OHLCV window, stale status, and source ownership are explicit and tested.
