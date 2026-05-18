# BACKEND-P32 SourceTrace Entry Ownership Validator Skeleton Result

## Baseline

- Branch context: PR #158 / Issue #155.
- Baseline commit: `6cde5a9` (`feat: add entry ownership adapter skeleton`).
- P31 dependency: entry ownership request, rule-owned candidate, freshness metadata, and nullable conflict metadata already exist.

## Files Added

- `src/main/java/org/example/trademodel/dto/planboundary/EntryOwnershipValidationResult.java`
- `src/main/java/org/example/trademodel/dto/planboundary/EntryOwnershipValidationStatusEnum.java`
- `src/main/java/org/example/trademodel/dto/planboundary/EntryOwnershipValidationMissingReasonEnum.java`
- `src/main/java/org/example/trademodel/service/SourceTraceEntryOwnershipValidator.java`
- `src/main/java/org/example/trademodel/service/impl/FailClosedSourceTraceEntryOwnershipValidator.java`
- `src/test/java/org/example/trademodel/service/impl/FailClosedSourceTraceEntryOwnershipValidatorTest.java`

The temporary `x32.txt` marker is removed from the final branch state.

## Validator Surfaces

### EntryOwnershipValidationResult

The validation result is a fail-closed DTO with:

- `symbol`
- `timeframe`
- `validationStatus=INCOMPLETE`
- `missingReason=MISSING_SOURCE`
- `reviewMode=REVIEW_ONLY`
- `missingFields`
- `manualReviewRequired=true`
- `notTradeInstruction=true`

It does not carry entry, stop, take-profit, risk-reward, liquidity, multi-timeframe, event, wick, order, execution, close, reverse, or auto-trading values.

### SourceTraceEntryOwnershipValidator

The validator interface exposes only:

```java
EntryOwnershipValidationResult validateEntryOwnership(EntryOwnershipRequest request);
```

No adapter implementation is added or required by P32.

### FailClosedSourceTraceEntryOwnershipValidator

The fail-closed implementation checks the P31 request envelope and returns `INCOMPLETE / MISSING_SOURCE / REVIEW_ONLY` for every path. Even a request with all nullable conflict flags explicitly set to `false` remains incomplete because the SourceTrace completion path is still unwired.

## Corrected Nullable Conflict Behavior

P32 continues the P30A/P31 conflict metadata correction:

- `EntrySourceConflictDTO` uses nullable `Boolean` flags.
- Primitive-only boolean conflict metadata remains disallowed.
- `null` means missing or unevaluated.
- Any nullable conflict flag equal to `null` fails closed.
- Any conflict flag equal to `true` fails closed.
- `false` means an explicit evaluated non-conflict, but it does not complete SourceTrace in P32.

The guarded conflict fields remain:

- `conflictsWithStop`
- `conflictsWithTakeProfit`
- `conflictsWithRiskReward`
- `conflictsWithLiquidity`
- `conflictsWithMultiTimeframe`
- `conflictsWithEvent`
- `conflictsWithWick`

## Fail-Closed Cases

The validator fails closed for:

- missing request
- missing `RuntimeKlineContextDTO`
- missing rule-owned candidate
- missing freshness metadata
- missing conflict metadata
- any nullable conflict flag being `null`
- any conflict flag being `true`
- unsafe `manualReviewRequired=false`
- unsafe `notTradeInstruction=false`
- otherwise complete skeleton request while SourceTrace completion remains unwired

## Still Unwired Fields

P32 does not populate SourceTrace entry fields and does not wire ownership into production readiness. These remain intentionally unwired:

- SourceTrace entry ownership completion path
- BoundaryCandidateService `VALID`
- ExecutionPlan readiness upgrades
- entry/stop/take-profit/risk-reward/liquidity/multi-timeframe/event/wick executable values
- dashboard rendering or schema persistence of completed SourceTrace entry ownership

## Boundary Confirmations

- Minimal Java validator skeleton only.
- No real entry price generation.
- No production entry ownership adapter implementation.
- No `DefaultSourceTraceEntryOwnershipAdapter`.
- No change to `FailClosedSourceTraceEntrySourceOwnershipService`.
- No SourceTrace entry field population.
- No SourceTrace completion.
- No BoundaryCandidateService `VALID` wiring.
- No ExecutionPlan readiness upgrade.
- No schema changes.
- No `dashboard.html` changes.
- No external data integration.
- No Coinglass, news, macro calendar, order API, or auto-trading changes.

## Tests

Focused validator tests cover:

- default validation output is fail-closed review-only
- missing request, runtime context, candidate, freshness, and conflict metadata fail closed
- nullable conflict flags defaulting to `null` fail closed as missing or unevaluated
- explicit conflict flags equal to `true` fail closed
- unsafe `manualReviewRequired=false` and `notTradeInstruction=false` fail closed
- a complete skeleton request remains incomplete because SourceTrace completion is unwired
- validator surfaces expose no order/execution/close/reverse/auto-trading method names
- no production adapter implementation is required

Run:

```bash
./mvnw -q -Dtest=FailClosedSourceTraceEntryOwnershipValidatorTest test
```

## Risk Action Guard

This pack is not a trading implementation. It remains manual-review-only and non-instructional:

- `manualReviewRequired=true`
- `notTradeInstruction=true`
- no entry/stop/take-profit/risk-reward generation
- no order placement
- no close/reverse action
- no auto-trading readiness upgrade
