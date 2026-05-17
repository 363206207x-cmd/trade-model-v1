# PHASE_HOME_RUNTIME_P2_FREEZE_DASHBOARD_RUNTIMEKLINE_CONTEXT_DETAIL_VISIBILITY

## 1. Document Purpose

This document records the HOME-RUNTIME-P2-FREEZE Dashboard RuntimeKline Detail Visibility Freeze Pack.

Issue context:

- `#123 HOME-RUNTIME-P2-FREEZE Dashboard RuntimeKline Detail Visibility Freeze Pack`

PR context:

- `#124 HOME-RUNTIME-P2-FREEZE trigger: RuntimeKline detail visibility freeze`

Baseline:

- `107811f feat(home): show RuntimeKline detail diagnostics`

This is a documentation-only freeze record.

It does not modify `dashboard.html`, backend logic, schema, external data integrations, order API, or auto-trading.

## 2. Freeze Scope

The freeze scope is limited to the current stable dashboard visibility behavior for `/api/dashboard/detail.runtimeKlineContext`.

The dashboard may display RuntimeKlineContext as read-only runtime diagnostics.

The dashboard must not present RuntimeKlineContext as:

- a trade signal,
- SourceTrace completion,
- BoundaryCandidate production readiness,
- ExecutionPlan readiness,
- entry / stop / TP / RR source completion,
- order readiness,
- auto-trading readiness.

`latestPrice` remains latest closed persisted close price / runtime metadata only.

`klineItems` remain compact persisted OHLCV metadata only.

## 3. Completed Chain Summary

| Phase | Completed Work | Freeze Status | Safety Boundary |
|---|---|---|---|
| BACKEND-P15 | Added `RuntimeKlineContextAssemblyService` and `RuntimeKlineContextAssemblyServiceImpl` to assemble safe RuntimeKline fields from `PersistedOhlcvReadinessResult`. | Closed as isolated assembly service. | Does not wire dashboard detail, complete SourceTrace, or generate entry / stop / TP / RR values. |
| BACKEND-P16 | Wired the P15 assembly service into `DefaultDashboardRuntimeKlineContextAdapter` after persisted OHLCV readiness evaluation. | Closed as dashboard RuntimeKline boundary integration. | SourceTrace remains incomplete; BoundaryCandidateService, ExecutionPlan, order API, and auto-trading remain unchanged. |
| BACKEND-P17 | Exposed a separate read-only `runtimeKlineContext` field in `/api/dashboard/detail`. | Closed as detail API exposure. | Field is separate from SourceTrace and does not complete boundary sources or execution readiness. |
| HOME-RUNTIME-P2 | Surfaced existing `/api/dashboard/detail.runtimeKlineContext` fields in the stable dashboard diagnostics area. | Closed as read-only UI visibility. | Does not create entry / stop / TP / RR UI; labels latest price and kline items as runtime diagnostics only. |
| HOME-RUNTIME-P2-FREEZE | Records the current RuntimeKline detail visibility baseline. | Documentation-only freeze. | No code, schema, integration, order, or auto-trading changes. |

## 4. Visible RuntimeKlineContext Fields

The stable dashboard visibility baseline includes these existing fields from `/api/dashboard/detail.runtimeKlineContext`:

| Field | Current Display | Display Boundary |
|---|---|---|
| `runtimeKlineContext.fallbackStatus` | RuntimeKline fallback status. | Read-only diagnostic status. |
| `runtimeKlineContext.latestPrice` | Latest closed persisted close price / runtime metadata only. | Not entry source, not order price, not execution price. |
| `runtimeKlineContext.klineItems` | Compact count and latest close summary when available. | Not entry / stop / TP / RR source data. |
| `runtimeKlineContext.persistedOhlcvReadinessStatus` | Persisted OHLCV readiness metadata. | Metadata only; not SourceTrace completion. |
| `runtimeKlineContext.persistedOhlcvStaleReasonCode` | Persisted OHLCV stale reason code. | Diagnostic metadata. |
| `runtimeKlineContext.persistedOhlcvStaleReasonText` | Persisted OHLCV stale reason text. | Diagnostic metadata. |
| `runtimeKlineContext.persistedOhlcvMissingFields` | Compact missing-field summary. | Diagnostic missing-field metadata. |
| `runtimeKlineContext.missingFields` | Compact RuntimeKline missing-field summary. | Diagnostic missing-field metadata. |
| `runtimeKlineContext.manualReviewRequired` | Manual review safety flag. | Read-only safety diagnostic; no execution permission. |
| `runtimeKlineContext.notTradeInstruction` | Non-trade-instruction safety flag. | Read-only safety diagnostic; no trading instruction. |

The dashboard may receive additional RuntimeKlineContext fields from the API, including `symbol`, `timeframe`, and execution-source fields, but HOME-RUNTIME-P2 visibility does not surface execution-source fields as UI sources.

## 5. Non-Actionable Boundaries

HOME-RUNTIME-P2-FREEZE preserves these non-actionable boundaries:

- documentation-only change,
- no `dashboard.html` change,
- no backend logic change,
- no schema change,
- no external data integration,
- no Coinglass integration,
- no order API,
- no auto-trading,
- no dashboard redesign,
- no Watchlist Pool semantics change,
- no Display Slots semantics change,
- no entry source UI,
- no stop source UI,
- no TP source UI,
- no RR source UI,
- no latestPrice-to-entry mapping,
- no klineItems-to-entry mapping,
- no klineItems-to-stop mapping,
- no klineItems-to-TP mapping,
- no klineItems-to-RR mapping,
- no RuntimeKline-as-trade-signal display,
- no SourceTrace completion display,
- no BoundaryCandidateService VALID upgrade,
- no ExecutionPlan executable-readiness upgrade.

Manual-review and non-trade defaults remain the only acceptable interpretation:

- `manualReviewRequired=true`
- `notTradeInstruction=true`

If either safety flag is missing or false in a future response, the UI must still remain read-only and non-actionable until a separate reviewed package changes that behavior.

## 6. Known Remaining Gaps

The following gaps remain after HOME-RUNTIME-P2:

| Area | Remaining Gap | Required Future Boundary |
|---|---|---|
| SourceTrace | Required boundary sources remain incomplete. | Must remain incomplete until entry, stop, TP, RR, liquidity, multi-timeframe, event, and wick sources are implemented and tested. |
| Entry / stop / TP / RR | RuntimeKlineContext does not provide executable boundary prices. | Must not infer source values from `latestPrice` or `klineItems`. |
| BoundaryCandidateService VALID path | No production VALID upgrade from RuntimeKline visibility alone. | VALID must stay manual-review / not-trade-instruction until all required sources are complete. |
| ExecutionPlan readiness | No executable readiness gate from RuntimeKline detail visibility. | Must remain review-only until SourceTrace, RiskActionGuard, PlanReadiness, and manual gates are explicit. |
| DerivativesRiskContext | Complete derivatives-risk source chain remains outside this freeze. | Must continue fail-closed when missing or incomplete. |
| Liquidity / event / wick sources | Not generated by RuntimeKlineContext detail visibility. | Must be implemented as separate verified source packages. |
| External integrations | No live external fetch or Coinglass integration is part of this visibility baseline. | Any future integration must remain isolated, tested, and fail-closed. |
| Trading actions | No order, reverse, close, or auto-trading behavior exists in this freeze. | Any future trading action path requires a separate explicit safety review. |

## 7. Recommended Next Backend Work

Recommended next backend work should remain narrow and fail-closed:

1. Define and test real SourceTrace boundary-source ownership for entry, stop, TP, RR, liquidity, multi-timeframe, event, and wick inputs.
2. Keep `latestPrice` restricted to latest selected closed persisted close metadata; never use it as an entry source.
3. Keep `klineItems` restricted to persisted OHLCV context diagnostics; never use them directly as entry / stop / TP / RR UI sources.
4. Add focused SourceTrace completion tests that prove RuntimeKlineContext visibility alone cannot complete SourceTrace.
5. Add BoundaryCandidateService tests that keep VALID blocked unless all required boundary sources and safety flags are complete.
6. Add ExecutionPlan readiness tests that keep execution review-only unless PlanReadiness, RiskActionGuard, SourceTrace, and manual-review gates are complete.
7. Keep external data and Coinglass work separate from dashboard detail visibility, with explicit fail-closed behavior before any UI exposure.
8. Keep order API and auto-trading out of RuntimeKline visibility work.

## 8. Tests Run

No tests were run for HOME-RUNTIME-P2-FREEZE.

Reason:

- documentation-only change,
- no Java production code changed,
- no Java test code changed,
- no `dashboard.html` change,
- no schema change,
- no backend logic change.

Prior implementation test results remain recorded in:

- `docs/PHASE_BACKEND_P15_RUNTIME_KLINE_CONTEXT_ASSEMBLY_SERVICE_RESULT.md`
- `docs/PHASE_BACKEND_P16_DASHBOARD_RUNTIME_KLINE_ASSEMBLY_BOUNDARY_RESULT.md`
- `docs/PHASE_BACKEND_P17_DASHBOARD_RUNTIME_KLINE_CONTEXT_DETAIL_EXPOSURE_RESULT.md`
- `docs/PHASE_HOME_RUNTIME_P2_DASHBOARD_RUNTIMEKLINE_CONTEXT_DETAIL_VISIBILITY_RESULT.md`

## 9. Boundary Confirmation

HOME-RUNTIME-P2-FREEZE confirms:

- documentation-only final package,
- no temporary trigger artifact included in the final package,
- no `dashboard.html` change,
- no backend logic change,
- no schema change,
- no external data integration,
- no Coinglass integration,
- no order API,
- no auto-trading,
- no RuntimeKline trade-signal display,
- no SourceTrace completion display,
- no entry / stop / TP / RR generation,
- no latestPrice-to-entry mapping,
- no klineItems-to-entry / stop / TP / RR mapping,
- no executable ExecutionPlan readiness,
- VALID remains manual-review / not-trade-instruction.

## 10. Current Conclusion

The RuntimeKline detail visibility chain is frozen at a safe read-only diagnostics baseline.

BACKEND-P15 through BACKEND-P17 created safe RuntimeKline assembly, dashboard boundary integration, and a separate `/api/dashboard/detail.runtimeKlineContext` field.

HOME-RUNTIME-P2 surfaced that field in the stable dashboard using existing API data only.

HOME-RUNTIME-P2-FREEZE records the completed visibility baseline without changing code.

RuntimeKline remains diagnostic metadata only.

SourceTrace remains incomplete.

`latestPrice` remains latest closed persisted close metadata only, not an entry source.

`klineItems` remain compact persisted OHLCV diagnostics only, not entry / stop / TP / RR sources.
