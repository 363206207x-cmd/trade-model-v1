# PHASE_BACKEND_P30A_ENTRY_CONFLICT_METADATA_NULLABILITY_FIX

## 1. Document Purpose

This document records the BACKEND-P30A Entry Conflict Metadata Nullability Fix.

Issue context:

- `#151 BACKEND-P30A Entry Conflict Metadata Nullability Fix`

PR context:

- `#152 BACKEND-P30A trigger: conflict metadata fix`

Baseline:

- `b60eebf docs: define entry ownership adapter Java design`

BACKEND-P30A is documentation-only.

It corrects the BACKEND-P30 Java design for future entry conflict metadata by requiring nullable conflict flags or an explicit conflict metadata presence / completeness contract.

It does not implement Java DTOs, modify Java production code, modify Java tests, change schema, modify `dashboard.html`, add external data integrations, add Coinglass, add order API, add auto-trading, generate real entry price values, complete SourceTrace, wire BoundaryCandidateService `VALID`, or upgrade ExecutionPlan readiness.

## 2. Why Primitive Boolean Is Unsafe

BACKEND-P30 proposed a future design-only `EntrySourceConflictDTO` with primitive boolean fields:

```java
boolean conflictsWithStop;
boolean conflictsWithTakeProfit;
boolean conflictsWithRiskReward;
boolean conflictsWithLiquidity;
boolean conflictsWithMultiTimeframe;
boolean conflictsWithEvent;
boolean conflictsWithWick;
```

Primitive `boolean` is unsafe for conflict metadata because Java defaults an unset primitive boolean to `false`.

That makes these states indistinguishable:

- conflict flag explicitly evaluated and no conflict found,
- conflict flag never supplied,
- conflict resolver skipped that sibling family,
- partially assembled DTO forgot one field,
- deserialization or mapping lost one field,
- test fixture accidentally omitted one conflict dimension.

For SourceTrace entry ownership, missing conflict metadata must fail closed. A primitive boolean risks converting missing metadata into an apparent safe non-conflict.

That violates the P29 / P30 contract requirement that a future entry adapter must fail closed when conflict metadata is missing, incomplete, stale, partial, or unsafe.

## 3. Corrected Future Design

A future Java implementation must use one of the corrected designs below.

### Option A: Nullable Conflict Flags

Use nullable `Boolean` fields so missing metadata remains observable:

```java
Boolean conflictsWithStop;
Boolean conflictsWithTakeProfit;
Boolean conflictsWithRiskReward;
Boolean conflictsWithLiquidity;
Boolean conflictsWithMultiTimeframe;
Boolean conflictsWithEvent;
Boolean conflictsWithWick;
List<String> conflictReasons;
List<String> missingFields;
```

Required interpretation:

- `Boolean.TRUE` means the candidate conflicts with that sibling family and must fail closed.
- `Boolean.FALSE` means that sibling family was evaluated and no conflict was found.
- `null` means that sibling family was not evaluated or was not supplied and must fail closed.

### Option B: Explicit Presence / Completeness Contract

Keep conflict values separate from an explicit completeness contract:

```java
boolean conflictMetadataPresent;
boolean stopConflictEvaluated;
boolean takeProfitConflictEvaluated;
boolean riskRewardConflictEvaluated;
boolean liquidityConflictEvaluated;
boolean multiTimeframeConflictEvaluated;
boolean eventConflictEvaluated;
boolean wickConflictEvaluated;
List<String> missingFields;
```

Required interpretation:

- every evaluated flag must be present and true before a corresponding primitive non-conflict value can be trusted,
- missing or false evaluation metadata must fail closed,
- `conflictMetadataPresent=false` must fail closed,
- `missingFields` must name every missing conflict dimension.

Option A is the simpler preferred shape for the next implementation package because nullable `Boolean` maps directly to the required missing / evaluated / conflict states.

## 4. Required Future Conflict Dimensions

The future entry conflict metadata design must represent every sibling-source compatibility dimension:

- stop conflict,
- TP conflict,
- RR conflict,
- liquidity conflict,
- multi-timeframe conflict,
- event conflict,
- wick conflict.

All seven dimensions must be evaluated before entry ownership can be considered complete.

Entry ownership remains incomplete if any dimension is unknown, null, missing, stale, partial, or conflicting.

## 5. Fail-Closed Behavior

The future entry adapter must fail closed when:

- conflict metadata object is null,
- conflict metadata presence marker is missing or false,
- any nullable conflict flag is null,
- any evaluated / present marker is missing or false,
- `missingFields` names any conflict dimension,
- stop conflict is present,
- TP conflict is present,
- RR conflict is present,
- liquidity conflict is present,
- multi-timeframe conflict is present,
- event conflict is present,
- wick conflict is present,
- conflict reasons are required but missing,
- conflict metadata cannot be tied to the same entry candidate,
- conflict metadata cannot be tied to the same symbol and timeframe,
- conflict metadata is stale or not aligned to the decision time.

Fail-closed output must preserve:

- `ownershipStatus=INCOMPLETE`,
- `missingReason=MISSING_SOURCE` or a future typed missing reason that remains fail-closed,
- `reviewMode=REVIEW_ONLY`,
- null entry ownership fields,
- missing fields for every missing or unsafe conflict dimension,
- `manualReviewRequired=true`,
- `notTradeInstruction=true`.

Fail-closed conflict metadata must not complete SourceTrace, must not make BoundaryCandidateService return production `VALID`, and must not upgrade ExecutionPlan readiness.

## 6. Required Test Additions Before Java Implementation

Before any Java implementation package adds entry conflict metadata DTOs or validators, tests must prove the corrected nullability design.

Required nullable-flag tests:

- null conflict metadata object fails closed,
- `conflictsWithStop=null` fails closed,
- `conflictsWithTakeProfit=null` fails closed,
- `conflictsWithRiskReward=null` fails closed,
- `conflictsWithLiquidity=null` fails closed,
- `conflictsWithMultiTimeframe=null` fails closed,
- `conflictsWithEvent=null` fails closed,
- `conflictsWithWick=null` fails closed,
- `Boolean.TRUE` on any conflict flag fails closed,
- all conflict flags `Boolean.FALSE` can pass only if every other entry ownership gate also passes.

Required explicit-completeness tests if that design is chosen:

- `conflictMetadataPresent=false` fails closed,
- any missing evaluated marker fails closed,
- any false evaluated marker fails closed,
- `missingFields` containing any conflict dimension fails closed,
- primitive false conflict values cannot be trusted unless evaluated markers are complete.

Required integration guard tests before Java implementation:

- completed entry ownership still does not complete SourceTrace by itself,
- missing conflict metadata keeps `SourceTraceDTO.hasRequiredBoundarySources()` false,
- entry ownership with null conflict flags cannot wire BoundaryCandidateService production `VALID`,
- entry ownership with null conflict flags cannot upgrade ExecutionPlan readiness,
- manual-review and not-trade-instruction safety remain true.

Required forbidden-flow tests remain unchanged:

- RuntimeKline `latestPrice` does not become `entryPriceSource`,
- raw RuntimeKline `klineItems` do not become entry ownership,
- quote latest price does not become entry ownership,
- AI text does not become entry ownership,
- dashboard display text does not become entry ownership.

## 7. P31 Implementation Requirement

P31 must use this corrected design.

P31 must not implement primitive-only conflict booleans for future entry conflict metadata.

P31 must choose one of:

- nullable `Boolean` conflict flags, preferably,
- explicit conflict metadata presence / completeness markers that make missing evaluation impossible to confuse with no conflict.

P31 must include fail-closed tests for null or missing conflict metadata before any entry ownership field can be populated.

P31 must keep P20 fail-closed behavior as the default fallback until all corrected conflict metadata checks pass.

## 8. Boundary Confirmation

BACKEND-P30A confirms:

- documentation-only final package,
- temporary marker file `x30a.txt` removed from the final PR,
- baseline commit `b60eebf` recorded,
- no Java DTO implementation,
- no Java production code change,
- no Java test change,
- no schema change,
- no `dashboard.html` change,
- no external data integration,
- no Coinglass integration,
- no order API,
- no auto-trading,
- no real entry price generation,
- no SourceTrace completion,
- no BoundaryCandidateService `VALID` production wiring,
- no ExecutionPlan readiness upgrade,
- `manualReviewRequired=true` preserved as a required safety invariant,
- `notTradeInstruction=true` preserved as a required safety invariant.

## 9. Risk Action Guard Reminders

Risk Action Guard remains separate from entry conflict metadata and SourceTrace completion.

- High risk does not directly mean close, reverse, or open.
- Wick or pin-bar evidence does not confirm trend reversal.
- Liquidity stress or stampede must block opportunity push and require review.
- Missing event data is not no event risk.
- Multi-timeframe agreement alone does not complete SourceTrace.
- Risk blocking can downgrade or block behavior, but it must not manufacture missing SourceTrace entry ownership or missing conflict metadata.

## 10. Tests Run

No tests were run for BACKEND-P30A.

Reason:

- documentation-only change,
- no Java production code changed,
- no Java test code changed,
- no schema changed,
- no `dashboard.html` change,
- no backend logic changed,
- no external data integration changed,
- no trading or auto-trading path changed.

## 11. Current Conclusion

BACKEND-P30A corrects the future entry conflict metadata design before Java implementation begins.

Primitive `boolean` conflict flags are not sufficient for fail-closed SourceTrace entry ownership because they cannot represent missing metadata.

Future implementation must use nullable `Boolean` conflict flags or an explicit presence / completeness contract, fail closed on any null or missing conflict dimension, and carry this corrected design into P31.
