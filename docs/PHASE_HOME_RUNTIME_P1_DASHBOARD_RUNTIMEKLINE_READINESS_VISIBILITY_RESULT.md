# PHASE_HOME_RUNTIME_P1_DASHBOARD_RUNTIMEKLINE_READINESS_VISIBILITY_RESULT

## 1. Document Purpose

This document records the HOME-RUNTIME-P1 Dashboard RuntimeKline Readiness Metadata Visibility Pack result.

Issue context:

- `#109 HOME-RUNTIME-P1 Dashboard RuntimeKline Readiness Metadata Visibility Pack`

Baseline:

- `af6a72e feat(backend): expose RuntimeKline readiness metadata`

HOME-RUNTIME-P1 surfaces existing `/api/dashboard/detail` RuntimeKline readiness metadata in the stable homepage.

It is frontend-only.

It does not change backend logic, schema, external integrations, dashboard semantics, order APIs, or trading behavior.

## 2. Files Changed

Changed files:

- `src/main/resources/templates/dashboard.html`
- `docs/PHASE_HOME_RUNTIME_P1_DASHBOARD_RUNTIMEKLINE_READINESS_VISIBILITY_RESULT.md`

Removed temporary trigger artifact:

- `docs/HOME_RUNTIME_P1_TRIGGER.md`

## 3. Visible RuntimeKline Readiness Fields

The dashboard now displays these existing `/api/dashboard/detail` fields from `sourceTrace`:

- `sourceTrace.runtimeKlineReadinessStatus`
- `sourceTrace.runtimeKlineStaleReasonCode`
- `sourceTrace.runtimeKlineStaleReasonText`
- `sourceTrace.runtimeKlineReadinessMissingFields`

They appear in:

- selected-asset main workbench read-only state summary,
- SourceTrace metadata diagnostics panel.

## 4. Metadata-Only Semantics

The display intentionally labels readiness as metadata only.

If status is `FRESH`, the UI renders it as:

- `FRESH / metadata only`

Any other status is rendered as fail-closed metadata:

- `STALE / fail-closed`
- `PARTIAL / fail-closed`
- `MISSING / fail-closed`
- `UNKNOWN / fail-closed`
- `INVALID / fail-closed`

The page also states:

- RuntimeKline readiness is persisted OHLCV diagnostic metadata only.
- `FRESH` does not mean RuntimeKline completion.
- `FRESH` does not mean SourceTrace completion.

## 5. Hidden / Still-Missing Fields

HOME-RUNTIME-P1 does not expose or create any new execution fields.

The following remain hidden, missing, or backend-owned:

- entry source UI,
- stop source UI,
- TP source UI,
- RR source UI,
- order action UI,
- reverse action UI,
- close position action UI,
- auto-trading UI,
- RuntimeKline completion indicator,
- SourceTrace completion indicator,
- executable ExecutionPlan indicator.

Existing SourceTrace missing fields remain visible only as diagnostics.

## 6. Boundary Confirmations

HOME-RUNTIME-P1 confirms:

- no backend logic change,
- no schema change,
- no external data integration,
- no Coinglass integration,
- no order API,
- no auto-trading,
- no entry / stop / TP / RR UI,
- no dashboard redesign,
- no Watchlist Pool semantics change,
- no Display Slots semantics change,
- no latestPrice-to-entry display,
- quote freshness is not displayed as kline stale status,
- RuntimeKline remains incomplete,
- SourceTrace remains incomplete,
- VALID remains manual-review / not-trade-instruction.

## 7. Tests Run

Commands:

```bash
node --check /private/tmp/dashboard-home-runtime-p1-inline.js
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

HOME-RUNTIME-P1 makes RuntimeKline readiness metadata visible on the stable dashboard using only existing `/api/dashboard/detail` fields.

The homepage now shows readiness status, stale reason code, stale reason text, and a compact missing-fields summary.

RuntimeKline and SourceTrace remain incomplete and fail-closed.

No execution, order, or auto-trading behavior was added.
