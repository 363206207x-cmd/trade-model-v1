# PHASE_HOME_RUNTIME_FREEZE_DASHBOARD_RUNTIMEKLINE_READINESS_VISIBILITY

## 1. Document Purpose

This document records the HOME-RUNTIME-FREEZE Dashboard RuntimeKline Readiness Visibility baseline.

Issue context:

- `#111 HOME-RUNTIME-FREEZE Dashboard RuntimeKline Readiness Visibility Freeze Pack`

PR context:

- `#112`

Baseline chain:

- BACKEND-P11 persisted OHLCV schema/entity/mapper skeleton
- BACKEND-P12 persisted OHLCV readiness query service
- BACKEND-P13 dashboard RuntimeKline readiness boundary metadata
- HOME-RUNTIME-P1 dashboard RuntimeKline readiness metadata visibility

This freeze record is documentation-only.

It does not modify `dashboard.html`, backend logic, schema, external data integrations, order APIs, or auto-trading.

## 2. Freeze Scope

The freeze scope is limited to the current stable dashboard visibility behavior for RuntimeKline readiness metadata.

The dashboard may display read-only persisted OHLCV readiness metadata that already exists in `/api/dashboard/detail`.

The dashboard must not present readiness metadata as:

- RuntimeKlineContext completion
- SourceTrace completion
- BoundaryCandidate production readiness
- ExecutionPlan readiness
- order readiness
- auto-trading readiness

`FRESH` remains metadata only.

`FRESH` must not look like RuntimeKline completion or SourceTrace completion.

## 3. Completed Chain Summary

| Phase | Completed Work | Freeze Status | Safety Boundary |
|---|---|---|---|
| BACKEND-P11 | Added `tm_persisted_ohlcv_bar`, `PersistedOhlcvBarDO`, mapper, and mapper integration test. | Closed as persisted OHLCV skeleton. | Does not wire RuntimeKlineContext, SourceTrace, entry, stop, TP, RR, external APIs, order API, or auto-trading. |
| BACKEND-P12 | Added read-only `PersistedOhlcvQueryService` and readiness statuses `FRESH`, `STALE`, `PARTIAL`, `MISSING`, `UNKNOWN`, `INVALID`. | Closed as readiness query service. | `FRESH` is a source-readiness result only; non-`FRESH` statuses fail closed. |
| BACKEND-P13 | Exposed persisted OHLCV readiness result in dashboard RuntimeKline boundary and SourceTrace metadata. | Closed as metadata integration. | Keeps RuntimeKlineContext incomplete and SourceTrace incomplete. |
| HOME-RUNTIME-P1 | Surfaced existing `/api/dashboard/detail` RuntimeKline readiness metadata in the homepage. | Closed as read-only UI visibility. | Labels `FRESH` as metadata only and non-`FRESH` as fail-closed. |
| HOME-RUNTIME-FREEZE | Records the current stable visibility baseline. | Documentation-only freeze. | Does not change UI, backend, schema, integrations, order API, or trading behavior. |

## 4. Visible RuntimeKline Readiness Metadata Fields

The stable dashboard visibility baseline includes these existing `/api/dashboard/detail` fields from `sourceTrace`:

| Field | Meaning | Display Boundary |
|---|---|---|
| `sourceTrace.runtimeKlineReadinessStatus` | Persisted OHLCV readiness status from the backend readiness boundary. | Read-only diagnostic metadata. |
| `sourceTrace.runtimeKlineStaleReasonCode` | Machine-readable stale or missing reason code. | Diagnostic only; not a trade gate by itself. |
| `sourceTrace.runtimeKlineStaleReasonText` | Human-readable stale or missing reason text. | Diagnostic only; not an execution reason. |
| `sourceTrace.runtimeKlineReadinessMissingFields` | Missing fields that prevent readiness completion or explain fail-closed state. | Diagnostic missing-field summary. |

The dashboard may display these fields in:

- selected-asset main workbench read-only state summary
- SourceTrace metadata diagnostics panel

No new backend field is introduced by this freeze.

No new frontend field is introduced by this freeze.

## 5. Readiness Status Display Semantics

The current stable display semantics are:

| Readiness Status | Display Meaning | RuntimeKline Completion | SourceTrace Completion | Trading Meaning |
|---|---|---|---|---|
| `FRESH` | Persisted OHLCV readiness metadata is fresh. | No | No | None |
| `STALE` | Persisted OHLCV readiness metadata is stale. | No | No | None |
| `PARTIAL` | Persisted OHLCV readiness metadata has an incomplete window. | No | No | None |
| `MISSING` | Persisted OHLCV readiness metadata is missing. | No | No | None |
| `UNKNOWN` | Persisted OHLCV readiness metadata cannot be trusted or classified. | No | No | None |
| `INVALID` | Persisted OHLCV readiness metadata failed quality or validity checks. | No | No | None |

`FRESH` means only that the persisted OHLCV readiness query classified the persisted source window as fresh enough for metadata display.

`FRESH` does not mean:

- RuntimeKlineContext has been assembled
- SourceTrace has required boundary sources
- DerivativesRiskContext is complete
- BoundaryCandidateService may output a production VALID candidate
- ExecutionPlan readiness is satisfied
- any order action is allowed

## 6. Non-Actionable Safety Boundaries

HOME-RUNTIME-FREEZE preserves these safety boundaries:

- no `dashboard.html` change
- no backend logic change
- no schema change
- no external data integration
- no Coinglass integration
- no order API
- no auto-trading
- no entry source generation
- no stop source generation
- no TP source generation
- no RR source generation
- no entry / stop / TP / RR UI
- no latestPrice-to-entry mapping
- no quote-freshness-to-kline-stale mapping
- no RuntimeKlineContext completion
- no SourceTrace completion
- no DerivativesRiskContext completion
- no BoundaryCandidateService VALID production path
- no ExecutionPlan executable readiness

Manual-review and non-trade defaults remain the only acceptable interpretation:

- `manualReviewRequired=true`
- `notTradeInstruction=true`

## 7. Known Remaining Gaps

The following gaps remain after HOME-RUNTIME-FREEZE:

| Area | Remaining Gap | Required Future Boundary |
|---|---|---|
| RuntimeKlineContext | No completed runtime context assembly from persisted OHLCV bars. | Must remain incomplete until a separate backend implementation builds and tests the context. |
| SourceTrace | Required boundary sources are still missing. | Must remain incomplete until entry, stop, TP, RR, liquidity, multi-timeframe, event, and wick sources exist. |
| DerivativesRiskContext | No complete derivatives-risk context source chain. | Must fail closed until real source ownership and completeness rules exist. |
| BoundaryCandidateService | No production VALID path from RuntimeKline + SourceTrace completeness. | Must not infer VALID from readiness metadata alone. |
| ExecutionPlan readiness | No executable readiness gate from RuntimeKline or SourceTrace. | Must remain review-only and non-actionable. |
| Live dashboard fetch | No live external market or derivatives fetch was added. | Must not call external APIs from dashboard detail. |
| Entry / stop / TP / RR | No production numeric source generation. | Must not use latest price as entry source. |
| Liquidity / event / wick sources | Still missing or incomplete. | Missing source must remain fail-closed or diagnostic only. |

## 8. Recommended Next Backend Work

Recommended next backend work should stay narrow and fail-closed:

1. Define a RuntimeKlineContext assembly contract that consumes persisted OHLCV bars without completing SourceTrace.
2. Add explicit tests for all readiness statuses at the dashboard boundary:
   - `FRESH`
   - `STALE`
   - `PARTIAL`
   - `MISSING`
   - `UNKNOWN`
   - `INVALID`
3. Define source ownership requirements before any RuntimeKline assembly can be considered complete.
4. Keep SourceTrace incomplete until entry, stop, TP, RR, liquidity, multi-timeframe, event, and wick sources are implemented and tested.
5. Keep BoundaryCandidateService VALID output blocked unless RuntimeKlineContext and SourceTrace are complete.
6. Keep ExecutionPlan readiness review-only until PlanReadiness, RiskActionGuard, SourceTrace, and manual-review gates are explicitly implemented.
7. Keep external API and Coinglass integration deferred until fail-closed behavior is tested with local persisted sources.

## 9. Tests Run

No tests were run for HOME-RUNTIME-FREEZE.

Reason:

- documentation-only change
- no Java production code changed
- no Java test code changed
- no `dashboard.html` change
- no schema change
- no backend logic change

Prior baseline test results remain recorded in:

- `docs/PHASE_BACKEND_P11_PERSISTED_OHLCV_SCHEMA_ENTITY_MAPPER_RESULT.md`
- `docs/PHASE_BACKEND_P12_PERSISTED_OHLCV_READINESS_QUERY_SERVICE_RESULT.md`
- `docs/PHASE_BACKEND_P13_DASHBOARD_RUNTIME_KLINE_READINESS_BOUNDARY_RESULT.md`
- `docs/PHASE_HOME_RUNTIME_P1_DASHBOARD_RUNTIMEKLINE_READINESS_VISIBILITY_RESULT.md`

## 10. Boundary Confirmation

HOME-RUNTIME-FREEZE confirms:

- documentation-only final package
- no Codex Cloud trigger artifact included in the final package
- no `dashboard.html` change
- no backend logic change
- no schema change
- no external data integration
- no Coinglass integration
- no order API
- no auto-trading
- no RuntimeKlineContext completion
- no SourceTrace completion
- no DerivativesRiskContext completion
- no entry / stop / TP / RR generation
- no latestPrice-to-entry mapping
- no executable ExecutionPlan readiness
- `FRESH` remains metadata only
- VALID remains manual-review / not-trade-instruction

## 11. Current Conclusion

The RuntimeKline readiness visibility chain is frozen at a safe metadata-only baseline.

BACKEND-P11 through BACKEND-P13 created persisted OHLCV source skeletons, read-only readiness classification, and dashboard readiness metadata.

HOME-RUNTIME-P1 surfaced that metadata in the stable homepage using existing `/api/dashboard/detail` fields.

HOME-RUNTIME-FREEZE records that completed visibility chain without changing code.

RuntimeKlineContext and SourceTrace remain incomplete.

`FRESH` readiness remains metadata only and must not be interpreted as completion, execution readiness, or a trading signal.
