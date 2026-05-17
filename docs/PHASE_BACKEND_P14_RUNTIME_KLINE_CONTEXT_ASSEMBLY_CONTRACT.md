# PHASE_BACKEND_P14_RUNTIME_KLINE_CONTEXT_ASSEMBLY_CONTRACT

## 1. Document Purpose

This document defines the BACKEND-P14 RuntimeKlineContext Assembly Contract Pack.

Issue context:

- `#113 BACKEND-P14 RuntimeKlineContext Assembly Contract Pack`

PR context:

- `#114`

This package is documentation-only.

It defines the contract for a future implementation that may assemble `RuntimeKlineContextDTO` from `PersistedOhlcvReadinessResult` and persisted OHLCV bars.

It does not implement that assembly.

It does not wire `RuntimeKlineContextDTO`.

It does not complete `SourceTraceDTO`.

It does not generate entry, stop, TP, or RR values.

It does not add external data integration, Coinglass, order API, auto-trading, or `dashboard.html` changes.

`FRESH` remains metadata until a separate implementation package explicitly maps it into a guarded runtime context.

## 2. Baseline Context

BACKEND-P14 builds on the following closed packages:

| Phase | Existing Result | P14 Contract Position |
|---|---|---|
| BACKEND-P11 | Added persisted OHLCV schema/entity/mapper skeleton. | Provides persisted bar field vocabulary. |
| BACKEND-P12 | Added read-only persisted OHLCV readiness query service. | Provides `PersistedOhlcvReadinessResult` and readiness statuses. |
| BACKEND-P13 | Exposed RuntimeKline readiness metadata in dashboard SourceTrace boundary. | Confirms readiness metadata is not completion. |
| HOME-RUNTIME-P1 | Surfaced readiness metadata on dashboard as read-only diagnostics. | Confirms `FRESH / metadata only` UI semantics. |
| HOME-RUNTIME-FREEZE | Froze metadata-only visibility chain. | Keeps RuntimeKline and SourceTrace incomplete. |

P14 is the next contract layer.

It defines what a future implementation must prove before `RuntimeKlineContextDTO` can be assembled.

## 3. Required Inputs From PersistedOhlcvReadinessResult

A future RuntimeKlineContext assembly service may only consume `PersistedOhlcvReadinessResult` after verifying these inputs:

| Input | Required | Rule |
|---|---:|---|
| `symbol` | Yes | Must be non-blank and match the requested normalized symbol. |
| `timeframe` | Yes | Must be non-blank and match the requested timeframe. |
| `requiredWindowSize` | Yes | Must be configured and greater than zero. |
| `status` | Yes | Must be `FRESH` for assembly eligibility. |
| `staleReasonCode` | Yes | Must be `NONE` or an explicitly complete equivalent when status is `FRESH`. |
| `staleReasonText` | Conditional | Required when status is not `FRESH`; optional for `FRESH`. |
| `missingFields` | Yes | Must be empty for assembly eligibility. Any missing field blocks assembly. |
| `bars` | Yes | Must contain the required closed persisted OHLCV window. |
| `latestCloseTimeMs` | Yes | Must match the latest selected closed persisted bar. |
| `latestIngestedAt` | Yes | Must be present and satisfy freshness policy. |
| `manualReviewRequired` | Yes | Must remain `true`. |
| `notTradeInstruction` | Yes | Must remain `true`. |

The future implementation must defensively copy or otherwise protect result lists before storing them in a DTO.

The future implementation must treat a null readiness result as fail-closed.

## 4. Required Persisted Bar Fields

Every bar used to assemble `RuntimeKlineContextDTO` must be a selected, closed, non-deleted persisted OHLCV bar.

Required `PersistedOhlcvBarDO` fields:

| Field | Required | Runtime Rule |
|---|---:|---|
| `symbol` | Yes | Must match readiness result and request. |
| `timeframe` | Yes | Must match readiness result and request. |
| `openTimeMs` | Yes | Must be present and contiguous with adjacent bars. |
| `closeTimeMs` | Yes | Must be present and contiguous with adjacent bars. |
| `openPrice` | Yes | Must be positive. |
| `highPrice` | Yes | Must be positive and not lower than open, close, or low. |
| `lowPrice` | Yes | Must be positive and not higher than open, close, or high. |
| `closePrice` | Yes | Must be positive. |
| `volume` | Yes | Must be present and non-negative. |
| `quoteVolume` | Optional | May enrich diagnostics but cannot be required unless policy says so. |
| `tradeCount` | Optional | May enrich diagnostics but cannot complete missing OHLCV by itself. |
| `takerBuyBaseVolume` | Optional | Diagnostic only unless a separate policy is added. |
| `takerBuyQuoteVolume` | Optional | Diagnostic only unless a separate policy is added. |
| `closed` | Yes | Must be `true`; open candles cannot complete RuntimeKlineContext. |
| `provider` | Yes | Must be non-blank. |
| `providerMarketType` | Yes | Must be non-blank and source-owned. |
| `sourceEndpoint` | Yes | Must be non-blank. |
| `sourceBatchId` | Yes | Must be non-blank. |
| `sourceTraceId` | Yes | Must be non-blank. |
| `sourceVersion` | Yes | Must be present. |
| `ingestedAt` | Yes | Must be present and pass freshness policy. |
| `updatedAt` | Conditional | Required if implementation relies on update freshness. |
| `qualityStatus` | Yes | Must be `OK`. |
| `qualityReason` | Conditional | Required when quality is not `OK`; non-OK blocks assembly. |
| `rawPayloadHash` | Optional | Diagnostic traceability only. |
| `isDeleted` | Yes | Must be `0` or equivalent non-deleted value. |

Presence of rows alone is not enough.

The selected bar window must be contiguous, source-owned, quality-OK, closed, fresh, and internally consistent.

## 5. FRESH-Only Eligibility Rules

Only `PersistedOhlcvReadinessStatus.FRESH` may be eligible for future RuntimeKlineContext assembly.

Eligibility requires all of the following:

- readiness result exists
- readiness `status = FRESH`
- stale reason indicates no blocking stale condition
- `missingFields` is empty
- `bars` count satisfies `requiredWindowSize`
- selected bars are closed candles
- selected bars are non-deleted
- selected bars are contiguous for the requested timeframe
- selected bars have valid OHLC fields
- selected bars have valid volume fields
- selected bars have `qualityStatus = OK`
- selected bars have explicit source ownership
- latest selected bar matches `latestCloseTimeMs`
- latest selected bar and `latestIngestedAt` satisfy freshness policy
- `manualReviewRequired = true`
- `notTradeInstruction = true`

Any failed condition must block assembly and produce a fail-closed status in the future implementation.

`FRESH` is necessary but not sufficient unless every source and mapping rule above is also satisfied.

## 6. Why FRESH Alone Is Not SourceTrace Completion

`FRESH` persisted OHLCV readiness proves only that a local persisted candle window passed readiness checks.

It does not prove:

- entry source completeness
- stop source completeness
- TP source completeness
- RR source completeness
- liquidity source completeness
- multi-timeframe source completeness
- event source completeness
- wick source completeness
- derivatives-risk context completeness
- BoundaryCandidateService VALID production readiness
- ExecutionPlan readiness

Therefore:

- `RuntimeKlineContextDTO` assembly must not complete `SourceTraceDTO`.
- `SourceTraceDTO.hasRequiredBoundarySources()` must remain false unless boundary sources are separately implemented.
- `FRESH` must not generate entry, stop, TP, or RR values.
- `FRESH` must not create a trade instruction.
- `FRESH` must not bypass manual review.

RuntimeKlineContext assembly and SourceTrace completion are separate gates.

## 7. RuntimeKlineContext Field Mapping Rules

A future implementation may map only safe runtime fields from readiness and persisted bars.

| RuntimeKlineContextDTO Field | Future Source | Mapping Rule |
|---|---|---|
| `symbol` | `PersistedOhlcvReadinessResult.symbol` | Copy only after request/result/bar symbol consistency is verified. |
| `timeframe` | `PersistedOhlcvReadinessResult.timeframe` | Copy only after request/result/bar timeframe consistency is verified. |
| `latestPrice` | latest selected persisted closed bar `closePrice` | May be set only from a closed persisted bar; must not be treated as entry source. |
| `dataQualityScore` | future explicit runtime quality calculation | Must not reuse unrelated decision score unless a separate policy owns it. |
| `persistedOhlcvReadinessStatus` | readiness `status` | Copy as metadata. |
| `persistedOhlcvStaleReasonCode` | readiness `staleReasonCode` | Copy as metadata. |
| `persistedOhlcvStaleReasonText` | readiness `staleReasonText` | Copy as metadata. |
| `persistedOhlcvMissingFields` | readiness `missingFields` | Defensive copy. |
| `fallbackStatus` | assembly result | `INCOMPLETE` unless the future assembly package explicitly proves complete runtime context. |
| `missingFields` | assembly validation | Empty only after future complete runtime assembly, not after readiness metadata alone. |
| `manualReviewRequired` | safety default | Must stay `true`; no parameter may disable it. |
| `notTradeInstruction` | safety default | Must stay `true`; no parameter may disable it. |

The following fields must not be populated by RuntimeKline assembly unless a separate boundary-source package owns them:

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

RuntimeKline OHLCV context is not a boundary source assembler.

## 8. Fail-Closed Behavior By Readiness Status

Future implementation must apply these fail-closed rules:

| Readiness Status | RuntimeKline Assembly Eligibility | Required Behavior |
|---|---|---|
| `FRESH` | Eligible only after all mapping and bar validation rules pass. | Metadata can be copied; runtime completion requires separate implementation checks. |
| `STALE` | Not eligible. | Keep `fallbackStatus=INCOMPLETE`, carry stale reason, keep manual review. |
| `PARTIAL` | Not eligible. | Keep `fallbackStatus=INCOMPLETE`, preserve missing fields, do not assemble runtime context. |
| `MISSING` | Not eligible. | Keep `fallbackStatus=INCOMPLETE`, report no persisted bars. |
| `UNKNOWN` | Not eligible. | Keep `fallbackStatus=INCOMPLETE`, report source ownership or policy ambiguity. |
| `INVALID` | Not eligible. | Keep `fallbackStatus=INCOMPLETE`, report quality or field validation failure. |
| null / exception | Not eligible. | Return fail-closed incomplete context with diagnostic missing fields. |

No non-`FRESH` status may create:

- RuntimeKline completion
- SourceTrace completion
- BoundaryCandidate VALID output
- ExecutionPlan readiness
- order action

## 9. Manual Review And Non-Trade Requirements

Every future assembly output must preserve:

- `manualReviewRequired = true`
- `notTradeInstruction = true`

These flags must remain true for:

- complete runtime context
- incomplete runtime context
- stale source
- partial source
- missing source
- unknown source
- invalid source
- exception fallback

No factory, mapper, adapter, or service parameter may allow callers to disable these safety defaults.

## 10. Tests Required Before Implementation

Before any Java implementation package wires RuntimeKlineContext assembly, focused tests must cover:

| Test Area | Required Cases |
|---|---|
| FRESH eligibility | Fresh contiguous closed window maps safe metadata and latest closed price only. |
| Stale fallback | `STALE` result stays incomplete and carries stale reason. |
| Partial fallback | `PARTIAL` result stays incomplete and carries missing fields. |
| Missing fallback | `MISSING` result stays incomplete and reports missing persisted bars. |
| Unknown fallback | `UNKNOWN` result stays incomplete when source ownership or policy is absent. |
| Invalid fallback | `INVALID` result stays incomplete when quality or OHLCV validation fails. |
| Null result | Null readiness result fails closed. |
| Exception path | Query or mapping exception fails closed. |
| Open candle exclusion | Open candles cannot complete RuntimeKlineContext. |
| Deleted row exclusion | Deleted bars cannot complete RuntimeKlineContext. |
| Source ownership | Missing provider, endpoint, batch, trace id, or market type blocks assembly. |
| Latest price mapping | Latest price may only come from latest selected closed bar `closePrice`. |
| No boundary generation | RuntimeKline assembly does not populate entry, stop, TP, RR, liquidity, event, or wick fields. |
| Safety defaults | `manualReviewRequired` and `notTradeInstruction` remain true in every case. |
| SourceTrace separation | `SourceTraceDTO.hasRequiredBoundarySources()` remains false unless separate boundary sources exist. |

Recommended test classes for a future implementation:

- `RuntimeKlineContextAssemblyServiceTest`
- `DefaultDashboardRuntimeKlineContextAdapterTest`
- `DefaultDashboardSourceTraceDetailAdapterTest`
- `DashboardControllerTest`

Recommended verification commands for the future implementation:

```bash
./mvnw -q -Dtest=RuntimeKlineContextAssemblyServiceTest test
./mvnw -q -Dtest=DefaultDashboardRuntimeKlineContextAdapterTest test
./mvnw -q -Dtest=DefaultDashboardSourceTraceDetailAdapterTest test
./mvnw -q -Dtest=DashboardControllerTest test
./mvnw -q -DskipTests compile
./mvnw -q -DskipTests test-compile
```

## 11. Next Implementation Boundary

The next implementation package may add a minimal RuntimeKlineContext assembly service only if it stays within these boundaries:

- consume `PersistedOhlcvReadinessResult`
- consume selected persisted OHLCV bars
- map safe RuntimeKline metadata and latest closed price
- preserve incomplete/fail-closed behavior for all non-eligible inputs
- preserve `manualReviewRequired=true`
- preserve `notTradeInstruction=true`
- add focused tests before exposing behavior to dashboard consumers

The next package must not:

- complete SourceTrace
- generate entry values
- generate stop values
- generate TP values
- generate RR values
- add liquidity source completion
- add multi-timeframe source completion
- add event source completion
- add wick source completion
- produce BoundaryCandidate VALID from readiness alone
- produce ExecutionPlan readiness
- modify `dashboard.html`
- add schema changes unless explicitly scoped
- call external APIs
- add Coinglass
- add order API
- add auto-trading

## 12. Boundary Confirmations For P14

BACKEND-P14 confirms:

- documentation-only package
- no Java production code change
- no Java test code change
- no schema change
- no `dashboard.html` change
- no RuntimeKlineContext wiring
- no SourceTrace completion
- no DerivativesRiskContext completion
- no BoundaryCandidateService VALID production path
- no ExecutionPlan readiness
- no entry / stop / TP / RR generation
- no external data integration
- no Coinglass integration
- no order API
- no auto-trading
- `FRESH` remains metadata until a separate implementation package
- VALID remains manual-review / not-trade-instruction

## 13. Tests Run

No tests were run for BACKEND-P14.

Reason:

- documentation-only change
- no Java production code changed
- no Java test code changed
- no schema changed
- no dashboard changed

## 14. Current Conclusion

BACKEND-P14 defines the contract for future RuntimeKlineContext assembly from persisted OHLCV readiness.

It makes the next boundary explicit:

- `FRESH` is required but not sufficient.
- persisted bars must pass field, continuity, freshness, quality, source ownership, and safety checks.
- RuntimeKline assembly must remain separate from SourceTrace completion.
- latest closed price must not become an entry source.
- all outputs remain manual-review and not trade instructions.

This package does not implement or wire the runtime context.

RuntimeKlineContext, SourceTrace, BoundaryCandidate VALID production output, and ExecutionPlan readiness remain incomplete until future implementation packages prove the missing gates with tests.
