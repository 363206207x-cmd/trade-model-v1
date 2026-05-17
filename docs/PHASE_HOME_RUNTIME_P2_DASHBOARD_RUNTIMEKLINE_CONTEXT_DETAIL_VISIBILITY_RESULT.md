# PHASE_HOME_RUNTIME_P2_DASHBOARD_RUNTIMEKLINE_CONTEXT_DETAIL_VISIBILITY_RESULT

## 1. Document Purpose

This document records the HOME-RUNTIME-P2 Dashboard RuntimeKline Context Detail Visibility Pack result.

Issue context:

- `#121 HOME-RUNTIME-P2 Dashboard RuntimeKline Context Detail Visibility Pack`

PR context:

- `#122 HOME-RUNTIME-P2 trigger: RuntimeKline detail visibility`

Baseline:

- `196b077 feat(backend): expose RuntimeKline detail`

HOME-RUNTIME-P2 surfaces existing `/api/dashboard/detail.runtimeKlineContext` fields in the stable dashboard as read-only diagnostics.

It is frontend-only.

It does not change backend logic, schema, external integrations, dashboard semantics, order APIs, auto-trading, Watchlist Pool semantics, or Display Slots semantics.

## 2. Files Changed

Changed files:

- `src/main/resources/templates/dashboard.html`
- `docs/PHASE_HOME_RUNTIME_P2_DASHBOARD_RUNTIMEKLINE_CONTEXT_DETAIL_VISIBILITY_RESULT.md`

Removed temporary trigger artifact:

- `docs/HOME_RUNTIME_P2_TRIGGER.md`

## 3. Visible RuntimeKlineContext Fields

The dashboard now attaches `resp.runtimeKlineContext` from `/api/dashboard/detail` to the selected decision detail model.

The diagnostics area displays these existing `runtimeKlineContext` fields:

- `runtimeKlineContext.fallbackStatus`
- `runtimeKlineContext.latestPrice`
- `runtimeKlineContext.klineItems`
- `runtimeKlineContext.persistedOhlcvReadinessStatus`
- `runtimeKlineContext.persistedOhlcvStaleReasonCode`
- `runtimeKlineContext.persistedOhlcvStaleReasonText`
- `runtimeKlineContext.persistedOhlcvMissingFields`
- `runtimeKlineContext.missingFields`
- `runtimeKlineContext.manualReviewRequired`
- `runtimeKlineContext.notTradeInstruction`

Display labels intentionally keep the fields diagnostic:

- `latestPrice` is labeled as latest closed persisted close price / runtime metadata only.
- `klineItems` is shown as a compact count.
- `klineItems` latest close summary is shown only when an item is available.
- persisted OHLCV readiness fields are labeled as metadata.
- missing fields are compact summaries.
- manual review and non-trade instruction flags remain visible as read-only diagnostics.

## 4. Hidden / Not Surfaced Fields

HOME-RUNTIME-P2 does not expose the RuntimeKlineContext execution-source fields.

The following remain hidden from the new RuntimeKlineContext diagnostics UI:

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
- liquidity / event / wick / multi-timeframe source fields

The UI does not create any new entry / stop / TP / RR display from `runtimeKlineContext`.

## 5. Read-Only Semantics

The new dashboard diagnostics state:

- RuntimeKlineContext is read-only diagnostics.
- RuntimeKlineContext does not complete SourceTrace.
- RuntimeKlineContext does not become a trade signal.
- `latestPrice` is runtime metadata only and not an entry source.
- `klineItems` are compact persisted OHLCV metadata and not entry / stop / TP / RR sources.
- `manualReviewRequired` and `notTradeInstruction` remain visible safety diagnostics.

## 6. Boundary Confirmations

HOME-RUNTIME-P2 confirms:

- no backend logic change,
- no schema change,
- no external data integration,
- no Coinglass integration,
- no order API,
- no auto-trading,
- no dashboard redesign,
- no Watchlist Pool semantics change,
- no Display Slots semantics change,
- no entry / stop / TP / RR UI created from RuntimeKlineContext,
- no latestPrice-to-entry display,
- no klineItems-to-entry / stop / TP / RR display,
- no RuntimeKline-as-trade-signal display,
- no SourceTrace completion display,
- VALID remains manual-review / not-trade-instruction.

## 7. Tests Run

Commands:

```bash
node --check /private/tmp/dashboard-home-runtime-p2-inline.js
./mvnw -q -Dtest=DashboardControllerTest test
./mvnw -q -DskipTests compile
./mvnw -q -DskipTests test-compile
```

Result:

- PASS dashboard inline script syntax check
- PASS `DashboardControllerTest`
- PASS compile
- PASS test-compile

## 8. Current Conclusion

HOME-RUNTIME-P2 makes the dashboard detail RuntimeKlineContext visible as read-only runtime diagnostics using only existing `/api/dashboard/detail.runtimeKlineContext` fields.

The UI shows fallback status, latest persisted closed close metadata, compact kline item count, latest close summary, persisted OHLCV readiness metadata, missing-field summaries, manual review, and non-trade instruction flags.

RuntimeKline remains diagnostic metadata only.

SourceTrace completion, trade-signal interpretation, entry / stop / TP / RR UI, order API, and auto-trading remain out of scope.
