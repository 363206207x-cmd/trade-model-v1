# PHASE_HOME_SOURCE_P1_DASHBOARD_SOURCETRACE_METADATA_VISIBILITY_RESULT

## 1. Document Purpose

This document records the HOME-SOURCE-P1 Dashboard SourceTrace Metadata Visibility Pack result.

HOME-SOURCE-P1 surfaces safe read-only SourceTrace and derivatives-risk metadata already returned by `/api/dashboard/detail`.

The pack is frontend visibility only.

It does not modify backend logic.

It does not modify schema.

It does not add external integrations.

It does not add order API or auto-trading behavior.

## 2. Changed Files

Changed files:

- src/main/resources/templates/dashboard.html
- docs/PHASE_HOME_SOURCE_P1_DASHBOARD_SOURCETRACE_METADATA_VISIBILITY_RESULT.md

Removed temporary trigger artifact:

- docs/PHASE_HOME_SOURCE_P1_CLOUD_TRIGGER.md

No Java production logic, backend DTO, schema, config, external integration, order API, or auto-trading files were changed.

## 3. Visible SourceTrace Fields

The dashboard now uses existing `/api/dashboard/detail` response fields only.

Visible SourceTrace metadata:

- sourceTrace.fallbackStatus
- sourceTrace.decisionId
- sourceTrace.decisionIdSource
- sourceTrace.analysisId
- sourceTrace.analysisIdSource
- sourceTrace.decisionCreateTime
- sourceTrace.decisionCreateTimeSource
- sourceTrace.timeframe
- sourceTrace.timeframeSource
- sourceTrace.quoteLatestPrice
- sourceTrace.quoteLatestPriceSource
- sourceTrace.quoteFreshnessStatus
- sourceTrace.quotePriceUpdateTimeSource
- sourceTrace.runtimeKlineContextStatus
- sourceTrace.runtimeKlineContextSource
- sourceTrace.missingFields summary

These fields are shown as read-only diagnostics.

They are not displayed as execution inputs.

They are not displayed as entry / stop / TP / RR values.

## 4. Visible DerivativesRiskContext Fields

Visible derivatives-risk metadata:

- derivativesRiskContext.fallbackStatus
- derivativesRiskContext.timeframe
- derivativesRiskContext.timeframeSource
- derivativesRiskContext.missingFields summary

The dashboard keeps derivatives-risk context visibly fail-closed when production derivatives sources are missing.

## 5. Hidden Or Missing Fields

HOME-SOURCE-P1 intentionally does not surface these as actionable values:

- entry price
- entry source
- stop price
- stop source
- TP price
- TP source
- RR source
- liquidity source
- event source
- wick source
- runtime kline OHLCV completeness
- kline stale status
- order intent
- execution readiness

Missing fields are shown only as compact missingFields summaries.

Missing fields are not transformed into trade actions.

## 6. Placement

The selected-asset main workbench now includes compact status metrics:

- SourceTrace fallback status
- RuntimeKline status
- DerivativesRisk fallback status

The diagnostics panel now includes a read-only SourceTrace metadata block:

- analysis anchors
- timeframe ownership
- quote metadata ownership
- runtime-kline unavailable marker
- missingFields summary
- derivatives-risk fallback summary

This keeps the first-screen workbench readable while putting technical source details in the diagnostics area.

## 7. Boundary Confirmations

HOME-SOURCE-P1 preserves the following boundaries:

- Uses only existing `/api/dashboard/detail` fields
- No backend logic changes
- No schema changes
- No external API integration
- No Coinglass integration
- No order API
- No auto-trading
- No dashboard redesign
- No Watchlist Pool semantic changes
- No Display Slots semantic changes

latestPrice is displayed only as quote metadata.

latestPrice is not displayed as entry source.

quote freshness is not displayed as kline stale status.

SourceTrace is not made to look complete.

RuntimeKline is not made to look complete.

VALID remains manual-review / not-trade-instruction.

## 8. Tests Run

Verification commands:

```bash
node --check /private/tmp/home-source-p1-dashboard-inline.js
./mvnw -q -Dtest=DashboardControllerTest test
./mvnw -q -DskipTests compile
./mvnw -q -DskipTests test-compile
```

All commands should pass before merge.

## 9. Current Conclusion

HOME-SOURCE-P1 makes existing SourceTrace and derivatives-risk metadata visible on the stable dashboard without changing backend behavior or trading semantics.

The page now helps reviewers see why SourceTrace and RuntimeKline remain incomplete.

It does not create any executable plan, order action, or automated trading path.
