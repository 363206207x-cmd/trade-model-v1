# PHASE_BACKEND_P7_DASHBOARD_SOURCETRACE_ANALYSIS_ANCHOR_METADATA_RESULT

## 1. Document Purpose

This document records the BACKEND-P7 Dashboard SourceTrace Analysis Anchor Metadata Pack result.

BACKEND-P7 wires only safe analysis-anchor metadata into dashboard SourceTrace detail output.

It does not complete SourceTrace.

It does not complete RuntimeKlineContext.

It does not wire external derivatives-risk data.

It does not create executable plan semantics.

## 2. Scope

BACKEND-P7 is limited to production-backed metadata already available through the dashboard decision read model.

The pack adds SourceTrace fields that identify which decision or analysis row the SourceTrace detail belongs to:

- decisionId
- decisionIdSource
- analysisId
- analysisIdSource
- symbolSource
- decisionCreateTime
- decisionCreateTimeSource

The existing symbol field remains present.

The SourceTrace fallback state remains INCOMPLETE.

RuntimeKlineContext remains unavailable in dashboard detail.

## 3. Changed Files

Changed files:

- src/main/java/org/example/trademodel/dto/planboundary/SourceTraceDTO.java
- src/main/java/org/example/trademodel/service/dashboard/DefaultDashboardSourceTraceDetailAdapter.java
- src/test/java/org/example/trademodel/service/dashboard/DefaultDashboardSourceTraceDetailAdapterTest.java
- src/test/java/org/example/trademodel/controller/DashboardControllerTest.java
- docs/PHASE_BACKEND_P7_DASHBOARD_SOURCETRACE_ANALYSIS_ANCHOR_METADATA_RESULT.md

Removed temporary trigger artifact:

- docs/PHASE_BACKEND_P7_CLOUD_TRIGGER.md

No dashboard HTML, schema, config, external integration, order API, or auto-trading files were changed.

## 4. Wired Fields

The following fields are wired when the values are available from DecisionResultVO:

| SourceTrace field | Source label | Source object |
|---|---|---|
| decisionId | DecisionResultVO.decisionId | DecisionResultVO |
| analysisId | DecisionResultVO.analysisId | DecisionResultVO |
| symbol | DecisionResultVO.symbol | DecisionResultVO |
| symbolSource | DecisionResultVO.symbol | DecisionResultVO |
| decisionCreateTime | DecisionResultVO.createTime | DecisionResultVO |
| decisionCreateTimeSource | DecisionResultVO.createTime | DecisionResultVO |

When DecisionResultVO.symbol is not available, SourceTrace keeps the request symbol and labels it as:

- dashboardDetail.requestSymbol

This is still metadata only.

It is not a boundary source.

It is not a RuntimeKline source.

It is not a SourceTrace completeness signal.

## 5. Fields Still Missing

BACKEND-P7 intentionally keeps these fields missing or fail-closed:

- runtimeKlineContext
- entryPriceSource
- entrySourceType
- entrySourceTimeframe
- entrySourceReason
- entrySourceRef
- stopPriceSource
- stopSourceType
- stopSourceTimeframe
- stopSourceReason
- stopSourceRef
- tpPriceSources
- tpSourceType
- tpSourceTimeframe
- tpSourceReason
- tpSourceRef
- rrSource
- rrRuleRef
- liquiditySource
- eventSource
- wickSource
- derivativesRiskContext production sources

BACKEND-P7 also keeps RuntimeKline status as:

- runtimeKlineContextStatus = UNAVAILABLE
- runtimeKlineContextSource = dashboardDetail.noRuntimeKlineContext

## 6. Boundary Confirmations

BACKEND-P7 preserves the following boundaries:

- No external API integration
- No Coinglass integration
- No order API
- No auto-trading
- No dashboard.html changes
- No schema changes
- No production entry source generation
- No production stop source generation
- No production TP source generation
- No RR source generation
- No liquidity source generation
- No event source generation
- No wick source generation

latestPrice remains quote metadata only.

latestPrice is not treated as entry source.

quote freshness is not treated as kline stale status.

analysis-anchor metadata is not treated as source completeness.

## 7. Safety Defaults

SourceTrace remains fail-closed:

- fallbackStatus = INCOMPLETE
- manualReviewRequired = true
- notTradeInstruction = true

DerivativesRiskContext remains fail-closed:

- fallbackStatus = SAFE_FAIL_CLOSED_ONLY
- manualReviewRequired = true
- notTradeInstruction = true

SourceTrace.hasRequiredBoundarySources() remains false when only analysis-anchor metadata is present.

## 8. Test Coverage

The adapter test verifies:

- analysis-anchor metadata is wired from DecisionResultVO when available
- request symbol source is preserved when decision-owned symbol is unavailable
- anchor metadata remains optional
- anchor metadata does not complete SourceTrace
- quote metadata does not become entry source
- SourceTrace remains INCOMPLETE
- RuntimeKlineContext remains unavailable
- manualReviewRequired remains true
- notTradeInstruction remains true

The controller test verifies:

- dashboard detail JSON exposes decisionId / analysisId / symbol source metadata
- dashboard detail JSON exposes decisionCreateTime source metadata
- dashboard detail SourceTrace remains INCOMPLETE
- missingFields remain explicit
- RuntimeKlineContext remains unavailable
- derivatives-risk context remains SAFE_FAIL_CLOSED_ONLY

## 9. Tests Run

Expected verification commands:

```bash
./mvnw -q -Dtest=DefaultDashboardSourceTraceDetailAdapterTest test
./mvnw -q -Dtest=DashboardControllerTest test
./mvnw -q -DskipTests compile
./mvnw -q -DskipTests test-compile
```

All commands should pass before merge.

## 10. Current Conclusion

BACKEND-P7 safely adds dashboard SourceTrace analysis-anchor metadata.

The implementation improves traceability of the dashboard detail payload without changing SourceTrace readiness, RuntimeKline readiness, boundary source completeness, or trading semantics.

SourceTrace and RuntimeKline remain incomplete.

BoundaryCandidate VALID remains manual-review and not-trade-instruction.

Missing SourceTrace or derivatives-risk context continues to fail closed.
