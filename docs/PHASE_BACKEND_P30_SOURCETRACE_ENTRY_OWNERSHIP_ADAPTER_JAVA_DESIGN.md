# PHASE_BACKEND_P30_SOURCETRACE_ENTRY_OWNERSHIP_ADAPTER_JAVA_DESIGN

## 1. Document Purpose

This document records the BACKEND-P30 SourceTrace Entry Ownership Adapter Java Design Pack.

Issue context:

- `#149 BACKEND-P30 SourceTrace Entry Ownership Adapter Java Design Pack`

PR context:

- `#150 BACKEND-P30 trigger: entry adapter Java design`

Baseline:

- `0baf8ce docs: define entry ownership adapter contract`

BACKEND-P30 is documentation-only.

It converts the BACKEND-P29 entry ownership adapter contract into a Java design package for a future implementation.

It does not add Java code, generate real entry price values, complete SourceTrace, wire BoundaryCandidateService `VALID`, upgrade ExecutionPlan readiness, add external data integrations, add Coinglass, add order API, add auto-trading, change schema, modify Java tests, or modify `dashboard.html`.

## 2. Relation To P29 Entry Adapter Contract

BACKEND-P29 defines the behavior contract for a future SourceTrace entry ownership adapter.

BACKEND-P30 narrows that behavior contract into proposed Java surfaces:

- current Java surfaces that already exist from BACKEND-P20,
- future interfaces and DTOs that may carry rule-owned entry candidates,
- design-only method signatures,
- allowed data flow from RuntimeKline context into a future rule-owned entry candidate,
- forbidden data flow from diagnostic or display-only values into entry ownership,
- fail-closed decision tree,
- required tests before implementation.

P30 does not implement the P29 contract.

P30 does not change the P20 fail-closed runtime behavior.

P30 keeps the P29 safety invariants:

- `manualReviewRequired=true`,
- `notTradeInstruction=true`,
- entry ownership alone cannot complete SourceTrace,
- RuntimeKline `latestPrice` cannot become `entryPriceSource`,
- raw RuntimeKline `klineItems` cannot become entry ownership.

## 3. Current Existing Java Surfaces From P20

The existing P20 entry skeleton created a narrow fail-closed Java surface.

Current service interface:

- `src/main/java/org/example/trademodel/service/SourceTraceEntrySourceOwnershipService.java`
- Method: `resolveEntrySourceOwnership(RuntimeKlineContextDTO runtimeKlineContext)`
- Return type: `SourceTraceEntrySourceOwnershipResult`

Current fail-closed implementation:

- `src/main/java/org/example/trademodel/service/impl/FailClosedSourceTraceEntrySourceOwnershipService.java`
- Behavior:
  - returns `SourceTraceEntrySourceOwnershipResult.missingSource(null, null)` when RuntimeKline context is missing,
  - otherwise copies only `symbol` and `timeframe` into a fail-closed result,
  - does not inspect RuntimeKline `latestPrice`,
  - does not inspect RuntimeKline `klineItems`,
  - does not produce entry ownership fields.

Current result DTO:

- `src/main/java/org/example/trademodel/dto/planboundary/SourceTraceEntrySourceOwnershipResult.java`
- Current fields:
  - `symbol`
  - `timeframe`
  - `ownershipStatus=INCOMPLETE`
  - `missingReason=MISSING_SOURCE`
  - `reviewMode=REVIEW_ONLY`
  - `entryPriceSource=null`
  - `entrySourceType=null`
  - `entrySourceTimeframe=null`
  - `entrySourceReason=null`
  - `entrySourceRef=null`
  - `missingFields=[entryPriceSource, entrySourceType, entrySourceTimeframe, entrySourceReason, entrySourceRef]`
  - `manualReviewRequired=true`
  - `notTradeInstruction=true`

Current enums:

- `SourceTraceEntrySourceOwnershipStatusEnum`
  - `INCOMPLETE`
- `SourceTraceEntrySourceMissingReasonEnum`
  - `MISSING_SOURCE`
- `SourceTraceEntrySourceReviewModeEnum`
  - `REVIEW_ONLY`

Current RuntimeKline context object:

- `src/main/java/org/example/trademodel/dto/planboundary/RuntimeKlineContextDTO.java`
- Context-only fields relevant to future design:
  - `symbol`
  - `timeframe`
  - `latestPrice`
  - `dataQualityScore`
  - `klineItems`
  - `persistedOhlcvReadinessStatus`
  - `persistedOhlcvStaleReasonCode`
  - `persistedOhlcvStaleReasonText`
  - `persistedOhlcvMissingFields`
  - `fallbackStatus`
  - `missingFields`
  - `manualReviewRequired`
  - `notTradeInstruction`

## 4. Proposed Future Java Design Surfaces

The following Java surfaces are proposed for a future implementation package only.

They are design-only in BACKEND-P30 and are not added in this PR.

### 4.1 Future Adapter Interface

Proposed class:

- `src/main/java/org/example/trademodel/service/SourceTraceEntryOwnershipAdapter.java`

Design-only signature:

```java
SourceTraceEntrySourceOwnershipResult resolveEntryOwnership(EntryOwnershipRequest request);
```

Design intent:

- orchestrate entry ownership validation,
- accept RuntimeKline context only through `EntryOwnershipRequest`,
- accept rule-owned entry candidate only when produced by a future source selector,
- return the P20-style fail-closed result for any missing or unsafe input.

### 4.2 Future Request DTO

Proposed class:

- `src/main/java/org/example/trademodel/dto/planboundary/EntryOwnershipRequest.java`

Design-only fields:

```java
RuntimeKlineContextDTO runtimeKlineContext;
RuleOwnedEntryCandidateDTO ruleOwnedEntryCandidate;
EntrySourceFreshnessDTO freshness;
EntrySourceConflictDTO conflict;
boolean manualReviewRequired;
boolean notTradeInstruction;
```

Design intent:

- keep RuntimeKline context separate from rule-owned entry candidate,
- make freshness and conflict metadata explicit,
- preserve manual-review and not-trade-instruction safety at the request boundary.

### 4.3 Future Rule-Owned Candidate DTO

Proposed class:

- `src/main/java/org/example/trademodel/dto/planboundary/RuleOwnedEntryCandidateDTO.java`

Design-only fields:

```java
String symbol;
String decisionTimeframe;
BigDecimal candidateEntryBoundary;
String entrySourceType;
String entrySourceTimeframe;
String entrySourceReason;
String entrySourceRef;
String ruleId;
String ruleVersion;
String sourceWindow;
```

Design intent:

- carry an entry boundary only after a future deterministic source selector owns it,
- prove source type, timeframe or window, reason, and reference before entry ownership can be populated,
- avoid copying RuntimeKline `latestPrice`, raw kline item values, quote values, AI text, or dashboard text.

### 4.4 Future Freshness DTO

Proposed class:

- `src/main/java/org/example/trademodel/dto/planboundary/EntrySourceFreshnessDTO.java`

Design-only fields:

```java
String freshnessStatus;
String staleReasonCode;
String staleReasonText;
Long observedAtMs;
Long decisionCreateTimeMs;
List<String> missingFields;
```

Design intent:

- make source freshness explicit,
- fail closed when freshness metadata is missing, stale, partial, or not aligned to the decision time.

### 4.5 Future Conflict DTO

Proposed class:

- `src/main/java/org/example/trademodel/dto/planboundary/EntrySourceConflictDTO.java`

Design-only fields:

```java
boolean conflictsWithStop;
boolean conflictsWithTakeProfit;
boolean conflictsWithRiskReward;
boolean conflictsWithLiquidity;
boolean conflictsWithMultiTimeframe;
boolean conflictsWithEvent;
boolean conflictsWithWick;
List<String> conflictReasons;
List<String> missingFields;
```

Design intent:

- make sibling-source compatibility explicit,
- fail closed if any conflict metadata is missing,
- fail closed if any unresolved conflict is present.

### 4.6 Future Validator

Proposed class:

- `src/main/java/org/example/trademodel/service/impl/SourceTraceEntryOwnershipValidator.java`

Design-only signature:

```java
EntryOwnershipValidationResult validate(EntryOwnershipRequest request);
```

Design intent:

- centralize fail-closed checks,
- reject missing candidate fields,
- reject missing freshness metadata,
- reject missing or conflicting sibling-source metadata,
- reject forbidden source attempts.

### 4.7 Future Adapter Implementation

Proposed class:

- `src/main/java/org/example/trademodel/service/impl/DefaultSourceTraceEntryOwnershipAdapter.java`

Design-only signature:

```java
SourceTraceEntrySourceOwnershipResult resolveEntryOwnership(EntryOwnershipRequest request);
```

Design intent:

- use `SourceTraceEntryOwnershipValidator`,
- map a valid rule-owned entry candidate to `SourceTraceEntrySourceOwnershipResult`,
- fall back to `FailClosedSourceTraceEntrySourceOwnershipService` behavior when validation fails,
- never complete SourceTrace by itself.

## 5. Minimal Method Signatures As Design Only

The signatures below are documentation-only and are not implemented in BACKEND-P30.

```java
public interface SourceTraceEntryOwnershipAdapter {
    SourceTraceEntrySourceOwnershipResult resolveEntryOwnership(EntryOwnershipRequest request);
}
```

```java
public interface RuleOwnedEntryCandidateProvider {
    RuleOwnedEntryCandidateDTO resolveCandidate(RuntimeKlineContextDTO runtimeKlineContext);
}
```

```java
public interface EntrySourceFreshnessResolver {
    EntrySourceFreshnessDTO resolveFreshness(RuleOwnedEntryCandidateDTO candidate);
}
```

```java
public interface EntrySourceConflictResolver {
    EntrySourceConflictDTO resolveConflicts(
            RuleOwnedEntryCandidateDTO candidate,
            SourceTraceSiblingConstraintSnapshot siblingConstraints
    );
}
```

```java
public interface SourceTraceEntryOwnershipValidator {
    EntryOwnershipValidationResult validate(EntryOwnershipRequest request);
}
```

```java
public interface SourceTraceEntryOwnershipResultMapper {
    SourceTraceEntrySourceOwnershipResult toResult(EntryOwnershipRequest request);
}
```

Design constraint:

- These signatures must not be added before a future implementation package includes tests that prove fail-closed behavior and forbidden data-flow guards.

## 6. Allowed Data Flow

Allowed future data flow from RuntimeKline context into rule-owned entry candidate:

1. `RuntimeKlineContextDTO.symbol` and `RuntimeKlineContextDTO.timeframe` may anchor the request.
2. RuntimeKline persisted OHLCV readiness fields may determine whether raw runtime context is usable as an input.
3. RuntimeKline `klineItems` may be passed only to a future deterministic rule-owned market-structure selector.
4. That selector must emit a separate `RuleOwnedEntryCandidateDTO`.
5. The candidate must carry `candidateEntryBoundary`, `entrySourceType`, `entrySourceTimeframe`, `entrySourceReason`, and `entrySourceRef`.
6. Freshness metadata must prove the candidate is fresh enough for the decision.
7. Conflict metadata must prove the candidate does not conflict with stop, TP, RR, liquidity, multi-timeframe, event, or wick constraints.
8. Only after all validation passes may the future adapter map the candidate into SourceTrace entry ownership fields.

Allowed flow summary:

```text
RuntimeKlineContextDTO
  -> future rule-owned market-structure selector input
  -> RuleOwnedEntryCandidateDTO
  -> freshness validation
  -> conflict validation
  -> SourceTraceEntrySourceOwnershipResult
```

RuntimeKline remains context and input material only. It is not itself the owner of entry semantics.

## 7. Forbidden Data Flow

The future Java implementation must not allow:

- `RuntimeKlineContextDTO.latestPrice -> entryPriceSource`
- raw `RuntimeKlineContextDTO.klineItems[*].close -> entryPriceSource`
- raw `RuntimeKlineContextDTO.klineItems -> entrySourceReason`
- quote latest price -> `entryPriceSource`
- AI text -> any entry ownership field
- dashboard display text -> any entry ownership field
- stop skeleton output -> entry ownership
- TP skeleton output -> entry ownership
- RR skeleton output -> entry ownership
- liquidity skeleton output -> entry ownership
- multi-timeframe skeleton output -> entry ownership
- event skeleton output -> entry ownership
- wick skeleton output -> entry ownership

Forbidden flow examples:

```text
latestPrice -> entryPriceSource
klineItems[0].close -> entryPriceSource
quoteLatestPrice -> entryPriceSource
dashboard label -> entrySourceReason
AI generated prose -> entrySourceRef
```

Any future code path that resembles these flows must fail closed.

## 8. Fail-Closed Decision Tree

The future adapter should follow this fail-closed decision tree:

1. If `EntryOwnershipRequest` is null, return P20-style missing source.
2. If `RuntimeKlineContextDTO` is null, return missing source.
3. If `symbol` or `timeframe` is blank, return missing source.
4. If RuntimeKline safety flags are unsafe, return missing source:
   - `manualReviewRequired=false`,
   - `notTradeInstruction=false`.
5. If persisted OHLCV readiness is missing, stale, partial, or unsafe, return missing source.
6. If `RuleOwnedEntryCandidateDTO` is null, return missing source.
7. If the candidate boundary is missing, return missing source.
8. If source type is blank or unsupported, return missing source.
9. If source timeframe or source window is blank, return missing source.
10. If source reason is blank, return missing source.
11. If source reference is blank, return missing source.
12. If freshness metadata is null, incomplete, stale, or misaligned with decision time, return missing source.
13. If conflict metadata is null or incomplete, return missing source.
14. If the candidate conflicts with stop, TP, RR, liquidity, multi-timeframe, event, or wick constraints, return missing source.
15. If the candidate was derived directly from latest price, raw kline item value, quote price, AI text, or dashboard text without a rule-owned selector and source reference, return missing source.
16. If output would set `manualReviewRequired=false` or `notTradeInstruction=false`, return missing source.
17. Only after every check passes may a future adapter produce completed entry ownership fields.

Even after step 17, SourceTrace must remain incomplete unless every sibling source family also passes its own ownership gate.

Fail-closed output must preserve:

- `ownershipStatus=INCOMPLETE`,
- `missingReason=MISSING_SOURCE`,
- `reviewMode=REVIEW_ONLY`,
- null entry ownership fields,
- `missingFields` covering unsafe or missing entry ownership dimensions,
- `manualReviewRequired=true`,
- `notTradeInstruction=true`.

## 9. Required Tests Before Implementation

Before any Java implementation package is allowed, tests must be added for the proposed design.

Required service and adapter tests:

- null request returns P20-style missing source,
- null RuntimeKline context returns P20-style missing source,
- missing symbol fails closed,
- missing timeframe fails closed,
- unsafe `manualReviewRequired=false` fails closed,
- unsafe `notTradeInstruction=false` fails closed,
- missing persisted OHLCV readiness fails closed,
- stale persisted OHLCV readiness fails closed,
- missing rule-owned candidate fails closed,
- missing candidate boundary fails closed,
- missing source type fails closed,
- missing source timeframe or source window fails closed,
- missing source reason fails closed,
- missing source reference fails closed,
- missing freshness metadata fails closed,
- stale freshness fails closed,
- missing conflict metadata fails closed,
- conflict with stop fails closed,
- conflict with TP fails closed,
- conflict with RR fails closed,
- conflict with liquidity fails closed,
- conflict with multi-timeframe fails closed,
- conflict with event fails closed,
- conflict with wick fails closed.

Forbidden data-flow tests:

- RuntimeKline `latestPrice` alone never becomes `entryPriceSource`,
- raw RuntimeKline `klineItems` alone never become entry ownership,
- quote latest price never becomes `entryPriceSource`,
- AI text never populates entry ownership,
- dashboard display text never populates entry ownership,
- sibling skeleton outputs never populate entry ownership.

SourceTrace guard tests:

- completed entry ownership alone does not complete SourceTrace,
- `SourceTraceDTO.hasRequiredBoundarySources()` remains false while stop, TP, RR, liquidity, multi-timeframe, event, or wick ownership is missing,
- SourceTrace remains manual-review and not-trade-instruction,
- SourceTrace missing fields still include sibling source families.

BoundaryCandidate and ExecutionPlan tests:

- entry ownership alone does not make BoundaryCandidateService return production `VALID`,
- entry ownership alone does not upgrade ExecutionPlan readiness,
- downstream display remains review-only and not a trade instruction.

Risk Action Guard tests:

- high risk does not directly mean close, reverse, or open,
- wick or pin-bar evidence does not confirm trend reversal,
- liquidity stress or stampede blocks opportunity push and requires review,
- missing event data is not treated as no event risk,
- multi-timeframe agreement alone does not complete SourceTrace,
- Risk Action Guard blocking does not manufacture missing entry ownership.

## 10. Still-Unwired Fields

The following SourceTrace entry ownership fields remain unwired in BACKEND-P30:

- `entryPriceSource`,
- `entrySourceType`,
- `entrySourceTimeframe`,
- `entrySourceReason`,
- `entrySourceRef`.

The following sibling SourceTrace families remain outside BACKEND-P30:

- stop source ownership,
- TP source ownership,
- RR source ownership,
- liquidity source ownership,
- multi-timeframe source ownership,
- event source ownership,
- wick source ownership.

BACKEND-P30 does not populate, infer, complete, or wire any of these fields.

## 11. Next Implementation Package Boundary

The next implementation package should still be narrow and test-first.

Recommended next package:

- add future request / candidate / freshness / conflict DTOs,
- add future adapter interface,
- keep `FailClosedSourceTraceEntrySourceOwnershipService` as the default fallback,
- add tests for the fail-closed decision tree and forbidden data flows,
- do not generate real entry prices until a deterministic rule-owned candidate provider exists,
- do not wire SourceTrace completion,
- do not wire BoundaryCandidateService `VALID`,
- do not upgrade ExecutionPlan readiness,
- do not touch stop, TP, RR, liquidity, multi-timeframe, event, or wick ownership,
- do not change schema,
- do not change `dashboard.html`,
- do not add external data integrations, Coinglass, order API, or auto-trading.

Implementation should stop at entry adapter shape and guard tests unless a later issue explicitly approves real rule-owned entry candidate production.

## 12. Risk Action Guard Reminders

Risk Action Guard remains separate from entry ownership.

- High risk does not directly mean close, reverse, or open.
- Wick or pin-bar evidence does not confirm trend reversal.
- Liquidity stress or stampede must block opportunity push and require review.
- Missing event data is not no event risk.
- Multi-timeframe agreement alone does not complete SourceTrace.
- Risk blocking can downgrade or block behavior, but it must not manufacture missing SourceTrace entry ownership.

## 13. Tests Run

No tests were run for BACKEND-P30.

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

BACKEND-P30 confirms:

- documentation-only final package,
- temporary marker file `z30.txt` removed from the final PR,
- baseline commit `0baf8ce` recorded,
- relation to P29 entry adapter contract recorded,
- current P20 Java surfaces recorded,
- proposed future Java classes / interfaces / DTOs recorded as design only,
- minimal method signatures recorded as design only,
- no real entry price generation,
- no Java production code change,
- no Java test change,
- no schema change,
- no `dashboard.html` change,
- no SourceTrace completion,
- no BoundaryCandidateService `VALID` production wiring,
- no ExecutionPlan readiness upgrade,
- no external data integration,
- no Coinglass integration,
- no order API,
- no auto-trading,
- `manualReviewRequired=true` preserved as a required safety invariant,
- `notTradeInstruction=true` preserved as a required safety invariant.

## 15. Current Conclusion

BACKEND-P30 defines the future Java design for SourceTrace entry ownership adapter work without implementing it.

The P20 fail-closed entry skeleton remains the current runtime behavior.

SourceTrace remains incomplete. Entry ownership remains unwired until a future test-first implementation package introduces approved Java surfaces, validates deterministic rule-owned entry candidates, blocks forbidden data flows, preserves manual-review / not-trade-instruction safety, and keeps sibling source families incomplete until their own ownership gates are implemented.
