# PHASE_BACKEND_P29_SOURCETRACE_ENTRY_OWNERSHIP_ADAPTER_CONTRACT

## 1. Document Purpose

This document records the BACKEND-P29 SourceTrace Entry Ownership Adapter Contract Pack.

Issue context:

- `#147 BACKEND-P29 SourceTrace Entry Ownership Adapter Contract Pack`

PR context:

- `#148 BACKEND-P29 trigger: entry adapter contract`

Baseline:

- `8166f90 docs: freeze sourcetrace skeleton baseline`

BACKEND-P29 is documentation-only.

It defines the contract a future entry ownership adapter must satisfy before it is allowed to populate SourceTrace entry ownership fields.

It does not generate real entry price values, complete SourceTrace, wire BoundaryCandidateService `VALID`, upgrade ExecutionPlan readiness, add external data integrations, add Coinglass, add order API, add auto-trading, change schema, modify Java production code, modify Java tests, or modify `dashboard.html`.

## 2. Relation To P18 SourceTrace Ownership Contract

BACKEND-P18 defines the full SourceTrace boundary source ownership contract.

P18 requires SourceTrace to remain incomplete until all required source families are implemented, fresh, non-conflicting, and safe:

- entry,
- stop,
- TP,
- RR,
- liquidity,
- multi-timeframe,
- event,
- wick.

BACKEND-P29 narrows only the entry portion of that contract.

P29 does not weaken P18. A future entry adapter may satisfy the entry gate only if it produces deterministic ownership for:

- `entryPriceSource`,
- `entrySourceType`,
- `entrySourceTimeframe`,
- `entrySourceReason`,
- `entrySourceRef`.

Even if those entry fields are completed in a future package, SourceTrace must remain incomplete until the stop, TP, RR, liquidity, multi-timeframe, event, wick, conflict, safety, missing-fields, and fallback gates also pass.

## 3. Relation To P20 Entry Skeleton

BACKEND-P20 added the current fail-closed entry source ownership skeleton.

Current P20 behavior:

- `SourceTraceEntrySourceOwnershipService.resolveEntrySourceOwnership(RuntimeKlineContextDTO runtimeKlineContext)` exists as the narrow entry ownership service boundary.
- `FailClosedSourceTraceEntrySourceOwnershipService` returns symbol and timeframe context when RuntimeKlineContext is present.
- All entry ownership value fields remain null.
- `missingFields=[entryPriceSource, entrySourceType, entrySourceTimeframe, entrySourceReason, entrySourceRef]`.
- `ownershipStatus=INCOMPLETE`.
- `missingReason=MISSING_SOURCE`.
- `reviewMode=REVIEW_ONLY`.
- `manualReviewRequired=true`.
- `notTradeInstruction=true`.

BACKEND-P29 does not replace that runtime behavior.

It defines what a future implementation must prove before it can move beyond the P20 fail-closed skeleton for entry ownership.

## 4. Required Input Objects And Fields

The future adapter must treat all input as source material that needs explicit ownership validation.

Existing input object allowed as context only:

| Object | Allowed fields | Contract use |
|---|---|---|
| `RuntimeKlineContextDTO` | `symbol`, `timeframe` | Anchor the candidate to the requested market and decision timeframe. |
| `RuntimeKlineContextDTO` | `latestPrice` | Diagnostic market context only; never the entry value by itself. |
| `RuntimeKlineContextDTO` | `klineItems` | Raw OHLCV context only; never selected entry ownership by itself. |
| `RuntimeKlineContextDTO` | `persistedOhlcvReadinessStatus`, `persistedOhlcvStaleReasonCode`, `persistedOhlcvStaleReasonText`, `persistedOhlcvMissingFields` | Runtime data readiness context only; stale or missing readiness must fail closed. |
| `RuntimeKlineContextDTO` | `dataQualityScore`, `fallbackStatus`, `missingFields`, `manualReviewRequired`, `notTradeInstruction` | Safety and completeness context only; unsafe or incomplete context must fail closed. |

Future adapter-owned input families, not implemented by P29:

| Future input family | Required fields or meaning |
|---|---|
| Rule-owned market structure output | Candidate entry boundary, source type, source timeframe or window, eligibility reason, rule id or version, source reference. |
| Future source reference | Stable reference to persisted input, rule output, adapter output, analysis artifact, or reviewed source bundle. |
| Freshness metadata | Observation time or window, freshness status, stale reason when stale, and decision-time alignment. |
| Conflict metadata | Explicit compatibility state versus stop, TP, RR, liquidity, multi-timeframe, event, and wick constraints. |

The adapter must not accept an entry value unless the entry candidate is owned by a deterministic source selector and can be traced back through source type, timeframe or window, reason, reference, freshness, and conflict state.

## 5. Allowed Input Families

Allowed input families for a future entry adapter:

- RuntimeKline as context only,
- future rule-owned market structure output,
- future source reference,
- freshness metadata,
- conflict metadata.

RuntimeKline can help establish symbol, timeframe, persisted OHLCV readiness, and raw context. It cannot be the owner of an entry source unless a future source selector explicitly consumes it and emits a separate owned entry output.

Future rule-owned market structure output may own entry only when it provides a selected boundary with source type, timeframe or window, reason, stable reference, freshness status, and conflict status.

Freshness and conflict metadata are required input families, not optional decorations.

## 6. Disallowed Inputs

The future entry adapter must not populate entry ownership from:

- `RuntimeKlineContextDTO.latestPrice` directly as entry,
- raw `RuntimeKlineContextDTO.klineItems` directly as entry,
- quote latest price,
- AI text,
- dashboard display text,
- free-form analyst notes without stable source reference,
- stop, TP, or RR skeleton output,
- liquidity, multi-timeframe, event, or wick skeleton output,
- any value lacking source type, timeframe or window, reason, reference, freshness status, or conflict state.

The adapter must not turn latest price into entry.

The adapter must not treat raw OHLCV bars as selected entry ownership by themselves.

The adapter must not infer entry from dashboard labels, display adapters, generated prose, or downstream execution text.

## 7. Required Output Fields

The future adapter may be allowed to populate SourceTrace entry ownership only when it can produce all required output fields:

| Field | Required meaning |
|---|---|
| `entryPriceSource` | Numeric candidate entry boundary selected by a deterministic source owner. |
| `entrySourceType` | Deterministic source type, for example support, pullback zone, breakout retest, or rule-owned derived boundary. |
| `entrySourceTimeframe` | Source timeframe or source window that produced the entry boundary. |
| `entrySourceReason` | Human-readable reason explaining why this source is eligible. |
| `entrySourceRef` | Stable reference to the persisted input, rule output, adapter output, or analysis artifact that owns the selected entry. |

Required safety outputs:

- `manualReviewRequired=true`,
- `notTradeInstruction=true`,
- review-only semantics preserved,
- no order, close, reverse, opportunity push, or auto-trading semantics.

If any required entry ownership field cannot be produced, the adapter must return the P20-style fail-closed result instead of partial entry ownership.

## 8. Fail-Closed Cases

The future entry adapter must fail closed when:

- RuntimeKline context is missing,
- symbol is missing or mismatched,
- timeframe is missing or mismatched,
- persisted OHLCV readiness is missing, stale, partial, or unsafe,
- no rule-owned market structure output is available,
- candidate entry boundary is missing,
- source type is missing or unsupported,
- source timeframe or window is missing,
- entry reason is missing,
- source reference is missing,
- source freshness metadata is missing or stale,
- conflict metadata is missing,
- candidate entry conflicts with stop constraints,
- candidate entry conflicts with TP constraints,
- candidate entry conflicts with RR constraints,
- candidate entry conflicts with liquidity constraints,
- candidate entry conflicts with multi-timeframe constraints,
- candidate entry conflicts with event constraints,
- candidate entry conflicts with wick constraints,
- latest price is the only candidate,
- raw kline items are the only candidate,
- quote latest price is the only candidate,
- AI text or dashboard display text is the only candidate,
- `manualReviewRequired=false`,
- `notTradeInstruction=false`.

Fail-closed output must preserve:

- `ownershipStatus=INCOMPLETE`,
- `missingReason=MISSING_SOURCE` or a future typed missing reason that remains fail-closed,
- `reviewMode=REVIEW_ONLY`,
- null entry ownership fields,
- populated missing fields for every missing or unsafe entry ownership dimension,
- `manualReviewRequired=true`,
- `notTradeInstruction=true`.

Fail-closed entry ownership must not complete SourceTrace, must not make BoundaryCandidateService return production `VALID`, and must not upgrade ExecutionPlan readiness.

## 9. Still-Unwired Fields

The following entry ownership fields remain unwired in BACKEND-P29:

- `entryPriceSource`,
- `entrySourceType`,
- `entrySourceTimeframe`,
- `entrySourceReason`,
- `entrySourceRef`.

The following sibling SourceTrace families also remain unwired and outside BACKEND-P29:

- `stopPriceSource`,
- `stopSourceType`,
- `stopSourceTimeframe`,
- `stopSourceReason`,
- `stopSourceRef`,
- `tpPriceSources`,
- `tpSourceType`,
- `tpSourceTimeframe`,
- `tpSourceReason`,
- `tpSourceRef`,
- `rrSource`,
- `rrRuleRef`,
- `liquiditySource`,
- `multiTimeframeSource`,
- `eventSource`,
- `wickSource`.

BACKEND-P29 does not complete any of these fields.

## 10. Required Tests Before Java Implementation

Before a future Java implementation populates entry ownership, focused tests must be added.

Required entry adapter tests:

- default adapter path remains `INCOMPLETE / MISSING_SOURCE / REVIEW_ONLY`,
- RuntimeKline `latestPrice` alone does not become `entryPriceSource`,
- RuntimeKline `klineItems` alone do not become entry ownership,
- quote latest price does not become entry ownership,
- AI text and dashboard display text do not become entry ownership,
- missing candidate entry boundary fails closed,
- missing source type fails closed,
- missing source timeframe or window fails closed,
- missing source reason fails closed,
- missing source reference fails closed,
- missing freshness metadata fails closed,
- stale source fails closed,
- missing conflict metadata fails closed,
- conflict with stop fails closed,
- conflict with TP fails closed,
- conflict with RR fails closed,
- conflict with liquidity fails closed,
- conflict with multi-timeframe fails closed,
- conflict with event fails closed,
- conflict with wick fails closed,
- unsafe `manualReviewRequired=false` fails closed,
- unsafe `notTradeInstruction=false` fails closed.

Required SourceTrace / assembler guard tests:

- completed entry ownership alone does not complete SourceTrace,
- SourceTrace `hasRequiredBoundarySources()` remains false while sibling families are missing,
- SourceTrace still reports missing stop, TP, RR, liquidity, multi-timeframe, event, and wick fields,
- SourceTrace remains manual-review and not-trade-instruction.

Required BoundaryCandidate / ExecutionPlan tests:

- entry ownership alone does not wire BoundaryCandidateService production `VALID`,
- entry ownership alone does not upgrade ExecutionPlan readiness,
- review-only and not-trade-instruction semantics remain visible downstream.

Risk Action Guard tests must continue to prove risk blocking does not manufacture missing entry ownership.

## 11. Next Implementation Boundary

The next implementation boundary should be a narrow Java adapter design package for entry ownership only.

Recommended scope:

- define the future adapter interface and DTOs for rule-owned entry source candidates,
- keep SourceTrace DTO and schema unchanged unless a separate schema package is approved,
- keep RuntimeKline as context only,
- keep P20 fail-closed behavior as the default fallback,
- implement no real entry generation until deterministic source ownership inputs exist,
- add focused tests before any production path can populate entry fields,
- leave stop, TP, RR, liquidity, multi-timeframe, event, and wick ownership untouched,
- leave BoundaryCandidateService `VALID` blocked,
- leave ExecutionPlan readiness review-only,
- avoid dashboard changes,
- avoid external data integrations, Coinglass, order API, and auto-trading.

## 12. Risk Action Guard Reminders

Risk Action Guard remains separate from entry ownership.

- High risk does not directly mean close, reverse, or open.
- Wick or pin-bar evidence does not confirm trend reversal.
- Liquidity stress or stampede must block opportunity push and require review.
- Missing event data is not no event risk.
- Multi-timeframe agreement alone does not complete SourceTrace.
- Risk blocking can downgrade or block behavior, but it must not manufacture missing SourceTrace entry ownership.

## 13. Tests Run

No tests were run for BACKEND-P29.

Reason:

- documentation-only change,
- no Java production code changed,
- no Java test code changed,
- no schema changed,
- no `dashboard.html` change,
- no backend logic changed,
- no external data integration changed,
- no trading or auto-trading path changed.

## 14. Boundary Confirmation

BACKEND-P29 confirms:

- documentation-only final package,
- temporary marker file `x29.txt` removed from the final PR,
- baseline commit `8166f90` recorded,
- relation to P18 SourceTrace ownership contract recorded,
- relation to P20 entry skeleton recorded,
- no real entry price generation,
- no SourceTrace completion,
- no BoundaryCandidateService `VALID` production wiring,
- no ExecutionPlan readiness upgrade,
- no Java production code change,
- no Java test change,
- no schema change,
- no `dashboard.html` change,
- no external data integration,
- no Coinglass integration,
- no order API,
- no auto-trading,
- `manualReviewRequired=true` preserved as a required safety invariant,
- `notTradeInstruction=true` preserved as a required safety invariant.

## 15. Current Conclusion

BACKEND-P29 defines the future SourceTrace entry ownership adapter contract without implementing the adapter.

The P20 fail-closed entry skeleton remains the current runtime behavior.

SourceTrace remains incomplete. Entry ownership remains unwired until a future Java implementation package supplies deterministic rule-owned entry source inputs, freshness metadata, conflict metadata, focused tests, and preserves manual-review / not-trade-instruction safety.
