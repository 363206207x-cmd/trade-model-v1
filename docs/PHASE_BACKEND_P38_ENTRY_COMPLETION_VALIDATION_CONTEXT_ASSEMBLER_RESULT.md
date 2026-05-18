# BACKEND-P38 Entry Completion Validation Context Assembler Result

## Baseline

- Branch context: PR #172 / Issue #171.
- Baseline commit: `319db03` (`test: expand entry completion resolver guards`).
- P38 adds a minimal assembler skeleton for composing validation and completion results into `EntryOwnershipValidationCompletionContext`.
- P38 does not wire completion into production validation, readiness, dashboard, schema, order, or automation paths.

## Files Changed

- `src/main/java/org/example/trademodel/service/EntryCompletionValidationContextAssembler.java`
- `src/test/java/org/example/trademodel/service/EntryCompletionValidationContextAssemblerTest.java`
- `docs/PHASE_BACKEND_P38_ENTRY_COMPLETION_VALIDATION_CONTEXT_ASSEMBLER_RESULT.md`

The temporary `z38.txt` marker is removed from the final branch state.

## Assembler Surface

`EntryCompletionValidationContextAssembler` exposes:

```java
EntryOwnershipValidationCompletionContext assemble(
        EntryOwnershipValidationResult validationResult,
        SourceTraceEntryCompletionResult completionResult
);
```

The assembler is intentionally not registered as a Spring service. It delegates to the existing `EntryOwnershipValidationCompletionContext.from(...)` fail-closed composition rule and does not add readiness behavior.

## Fail-Closed Behavior

Focused tests prove:

- assembler presence alone does not make `sourceTraceEntryCompleted=true`
- assembler presence alone does not make `completionReady=true`
- null validation result fails closed
- null completion result fails closed
- incomplete validation result fails closed
- unwired completion path fails closed
- mixed missing fields remain fail closed
- completion output remains `REVIEW_ONLY`
- `manualReviewRequired=true`
- `notTradeInstruction=true`
- SourceTrace entry fields remain null
- assembler exposes no order/execution/close/reverse/auto-trading method surface

## Still Unwired Fields

P38 does not populate SourceTrace entry fields and does not wire ownership into production readiness. These remain intentionally unwired:

- SourceTrace entry ownership completion path
- full SourceTrace completion
- BoundaryCandidateService `VALID`
- ExecutionPlan readiness upgrades
- entry/stop/take-profit/risk-reward/liquidity/multi-timeframe/event/wick executable values
- dashboard rendering or schema persistence of completed SourceTrace entry ownership

## Boundary Confirmations

- No real entry price generation.
- No real stop/take-profit/risk-reward generation.
- No production entry ownership adapter implementation.
- No `DefaultSourceTraceEntryOwnershipAdapter`.
- No production `DefaultSourceTraceEntryCompletionContract`.
- Assembler and resolver are not registered as Spring services.
- Assembler and resolver are not wired into validation, readiness, dashboard, schema, order, or automation.
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
./mvnw -q -Dtest=FailClosedSourceTraceEntryCompletionResolverTest test
./mvnw -q -Dtest=EntryOwnershipValidationCompletionContextTest test
./mvnw -q -Dtest=EntryCompletionValidationContextAssemblerTest test
./mvnw -q -DskipTests compile
./mvnw -q -DskipTests test-compile
```

## Risk Action Guard

- High risk does not directly mean close, reverse, or open.
- Wick / pin-bar evidence does not confirm trend reversal.
- Liquidity stress / stampede must block opportunity push and require review.
- Missing event data is not no event risk.
- Multi-timeframe agreement alone does not complete SourceTrace.
