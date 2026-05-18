# BACKEND-P36 SourceTrace Entry Completion Fail-Closed Resolver Result

## Baseline

- Branch context: PR #168 / Issue #167.
- Baseline commit: `a5ce5a7` (`feat: add entry completion validation context`).
- P36 adds a concrete fail-closed resolver skeleton for the P34 completion contract.
- P36 does not wire completion into production readiness.

## Files Changed

- `src/main/java/org/example/trademodel/service/impl/FailClosedSourceTraceEntryCompletionResolver.java`
- `src/test/java/org/example/trademodel/service/impl/FailClosedSourceTraceEntryCompletionResolverTest.java`
- `docs/PHASE_BACKEND_P36_SOURCETRACE_ENTRY_COMPLETION_FAIL_CLOSED_RESOLVER_RESULT.md`

The temporary `z36.txt` marker is removed from the final branch state.

## Resolver Surface

`FailClosedSourceTraceEntryCompletionResolver` implements:

```java
SourceTraceEntryCompletionResult resolveEntryCompletion(EntryOwnershipValidationResult validationResult);
```

The resolver is intentionally not registered as a Spring service and is not wired into validation, readiness, dashboard, schema, order, or automation paths. It exists only as a concrete, testable fail-closed boundary for future phases.

## Fail-Closed Behavior

The resolver always returns an incomplete completion result:

- null validation result returns `UNSAFE_COMPLETION`
- missing validation fields return `MISSING_COMPLETION`
- incomplete validation result remains fail closed
- `sourceTraceEntryOwnershipCompletionPath` returns `COMPLETION_UNWIRED`
- resolver presence alone never sets `sourceTraceEntryCompleted=true`
- resolver presence alone never sets `completionReady=true`
- completion output remains `REVIEW_ONLY`
- `manualReviewRequired=true`
- `notTradeInstruction=true`

P34/P35 behavior remains review-only and non-instructional.

## Still Unwired Fields

P36 does not populate SourceTrace entry fields and does not wire ownership into production readiness. These remain intentionally unwired:

- SourceTrace entry ownership completion path
- full SourceTrace completion
- BoundaryCandidateService `VALID`
- ExecutionPlan readiness upgrades
- entry/stop/take-profit/risk-reward/liquidity/multi-timeframe/event/wick executable values
- dashboard rendering or schema persistence of completed SourceTrace entry ownership

## Boundary Confirmations

- No real entry price generation.
- No production entry ownership adapter implementation.
- No `DefaultSourceTraceEntryOwnershipAdapter`.
- No production `DefaultSourceTraceEntryCompletionContract`.
- No real SourceTrace field population.
- No full SourceTrace completion.
- No BoundaryCandidateService `VALID` production path.
- No ExecutionPlan readiness upgrade.
- No schema changes.
- No `dashboard.html` changes.
- No external data integration.
- No Coinglass, news, macro calendar, order API, or auto-trading changes.

## Tests

Run:

```bash
./mvnw -q -Dtest=SourceTraceEntryCompletionContractTest test
./mvnw -q -Dtest=EntryOwnershipValidationCompletionContextTest test
./mvnw -q -Dtest=FailClosedSourceTraceEntryCompletionResolverTest test
./mvnw -q -DskipTests compile
./mvnw -q -DskipTests test-compile
```

Focused resolver tests cover:

- resolver presence alone does not complete SourceTrace
- resolver presence alone does not make completion ready
- null validation result fails closed
- missing validation fields fail closed
- incomplete validation result fails closed
- unwired completion path fails closed
- missing completion state fails closed through the validator-completion context
- no default production completion contract or ownership adapter is introduced

## Risk Action Guard

- High risk does not directly mean close, reverse, or open.
- Wick / pin-bar evidence does not confirm trend reversal.
- Liquidity stress / stampede must block opportunity push and require review.
- Missing event data is not no event risk.
- Multi-timeframe agreement alone does not complete SourceTrace.
