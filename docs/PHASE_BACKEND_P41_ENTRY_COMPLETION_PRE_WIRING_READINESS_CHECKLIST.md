# BACKEND-P41 Entry Completion Pre-Wiring Readiness Checklist

## Baseline

- Branch context: PR #178 / Issue #177.
- Baseline commit: `bee0434` (`docs: freeze entry completion safety chain`).
- Scope: documentation-only pre-wiring checklist before any future SourceTrace entry completion path can be considered for implementation.
- This checklist does not modify Java, tests, schema, dashboard, config, production wiring, external integrations, order APIs, or auto-trading.

## Current Frozen Safety Chain Summary

- BACKEND-P34 introduced `SourceTraceEntryCompletionResult` and `SourceTraceEntryCompletionContract` as an explicit completion boundary.
- BACKEND-P35 introduced `EntryOwnershipValidationCompletionContext` as the validator-facing validation/completion seam.
- BACKEND-P36 introduced `FailClosedSourceTraceEntryCompletionResolver` as a concrete, testable, fail-closed resolver skeleton.
- BACKEND-P37 expanded resolver guard coverage across malformed, ambiguous, incomplete, duplicate, metadata-only, unwired, and non-instructional cases.
- BACKEND-P38 introduced `EntryCompletionValidationContextAssembler` as a minimal unregistered assembler facade.
- BACKEND-P39 expanded assembler guard coverage across null, duplicate, mixed, metadata-only, and SourceTrace-field-null cases.
- BACKEND-P40 froze the safety chain with `INCOMPLETE`, `REVIEW_ONLY`, `sourceTraceEntryCompleted=false`, `completionReady=false`, `manualReviewRequired=true`, and `notTradeInstruction=true` as the required invariants.

## Preconditions Before Any Future Wiring

Future phases must satisfy all of these before implementation work may consider wiring completion output:

- A written contract must define exactly which rule-owned source owns each entry completion field.
- Fixture-only evidence must prove every required field is present before any completion candidate can be considered.
- Fixture-only evidence must prove missing, null, ambiguous, conflicting, unsafe, stale, duplicate, or unwired data remains fail closed.
- The SourceTrace entry completion path must have a dedicated status, missing-reason, and missing-field story that does not depend on UI text, AI text, latest price, or raw kline lists.
- `EntryOwnershipValidationResult` must be complete under review-only rules before it can be used as an input to any future completion implementation.
- `SourceTraceEntryCompletionResult` must remain the explicit completion output boundary.
- `EntryOwnershipValidationCompletionContext` must remain the explicit validation/completion composition boundary.
- Any future implementation must prove it does not expose order, execution, close, reverse, auto-trading, or trade-ready method surfaces.
- Any future wiring proposal must include rollback criteria that return to `INCOMPLETE`, `REVIEW_ONLY`, `manualReviewRequired=true`, and `notTradeInstruction=true`.

## Mandatory Blocking Conditions

Any of the following must block completion wiring and keep output fail closed:

- Missing validation result.
- Missing completion result.
- Missing runtime kline context.
- Missing rule-owned entry candidate.
- Missing candidate boundary, source type, source timeframe, source reason, or source ref.
- Missing freshness metadata.
- Missing freshness status, observed time, or decision-create time.
- Missing conflict metadata.
- Any nullable conflict flag is `null`.
- Any conflict flag is `true`.
- `manualReviewRequired=false`.
- `notTradeInstruction=false`.
- Empty, duplicate, malformed, or mixed missing fields that do not prove completion.
- Missing, ambiguous, unsafe, stale, or unwired completion state.
- `sourceTraceEntryOwnershipCompletionPath` is missing or unwired.
- SourceTrace entry fields are null, synthetic, dashboard-derived, AI-derived, quote-derived, or latest-price-derived.
- Liquidity stress or stampede evidence exists.
- Event data is missing or unevaluated.
- Multi-timeframe evidence is incomplete, unevaluated, or only agreement without SourceTrace ownership.

## Required Fixture-Only Evidence Before Implementation

Before any Java implementation or production wiring, a future phase must add fixture-only proof for:

- Valid-looking fixture input still remains review-only until every SourceTrace ownership field is explicitly owned.
- Null validation and null completion remain fail closed.
- Missing completion path remains fail closed.
- Candidate boundary, entry source type, timeframe, reason, and ref are each independently required.
- Freshness status, observed time, and decision-create time are each independently required.
- Conflict metadata preserves nullable Boolean semantics and fails closed on `null`.
- Explicit non-conflict flags do not substitute for missing SourceTrace entry completion.
- Latest price alone is not sufficient.
- Kline items alone are not sufficient.
- Symbol/timeframe metadata alone is not sufficient.
- Assembler presence alone is not sufficient.
- Resolver presence alone is not sufficient.
- Any future completion output remains non-instructional in fixture assertions.

Fixture evidence must be synthetic, deterministic, and isolated from production data feeds. It must not generate or assert real entry, stop, take-profit, risk-reward, liquidity, event, multi-timeframe, or wick trading values.

## What Must Remain Unwired

These must remain unwired until a later phase explicitly authorizes implementation and passes the checklist:

- `sourceTraceEntryOwnershipCompletionPath`
- `entryPriceSource`
- `entrySourceType`
- `entrySourceTimeframe`
- `entrySourceReason`
- `entrySourceRef`
- full SourceTrace completion
- BoundaryCandidateService `VALID`
- ExecutionPlan readiness upgrades
- dashboard rendering or schema persistence of completed SourceTrace entry ownership
- production validation, readiness, order, automation, or external data paths
- external API, Coinglass, news, macro calendar, order API, and auto-trading paths

## What Must Not Become Readiness

The following cannot be treated as completion readiness:

- Presence of `SourceTraceEntryCompletionContract`.
- Presence of `FailClosedSourceTraceEntryCompletionResolver`.
- Presence of `EntryCompletionValidationContextAssembler`.
- Presence of `EntryOwnershipValidationCompletionContext`.
- Symbol/timeframe metadata.
- Latest price.
- Raw kline items.
- Quote text, AI text, dashboard text, or external data text.
- A candidate boundary without source type, timeframe, reason, and ref.
- Freshness metadata without complete observed and decision timing.
- Conflict metadata with any nullable conflict flag missing.
- Duplicate missing fields.
- Empty missing fields.
- Multi-timeframe agreement alone.
- Wick or pin-bar evidence alone.
- High-risk classification.
- Missing event data.

## Review-Only Acceptance Gates

Until a future implementation is separately approved, every P41-adjacent result must satisfy:

- `completionStatus=INCOMPLETE`
- `reviewMode=REVIEW_ONLY`
- `sourceTraceEntryCompleted=false`
- `completionReady=false`
- `manualReviewRequired=true`
- `notTradeInstruction=true`
- SourceTrace entry fields remain null.
- No entry, stop, take-profit, risk-reward, liquidity, multi-timeframe, event, or wick executable values are generated.
- No order, execution, close, reverse, auto-trading, or trade-ready behavior is exposed.
- Resolver and assembler remain unregistered and unwired from Spring production paths.

## Verification Commands

Recommended verification for this documentation-only checklist:

```bash
./mvnw -q -Dtest=EntryCompletionValidationContextAssemblerTest test
./mvnw -q -Dtest=FailClosedSourceTraceEntryCompletionResolverTest test
./mvnw -q -Dtest=EntryOwnershipValidationCompletionContextTest test
./mvnw -q -DskipTests compile
./mvnw -q -DskipTests test-compile
```

## Boundary Confirmations

- No real entry price values are generated.
- No real stop, take-profit, or risk-reward values are generated.
- No production entry ownership adapter is implemented.
- No `DefaultSourceTraceEntryOwnershipAdapter` is added.
- No production `DefaultSourceTraceEntryCompletionContract` is added.
- Resolver and assembler are not registered as production Spring services.
- Resolver and assembler are not wired into validation, readiness, dashboard, schema, order, or automation paths.
- Real SourceTrace fields are not populated.
- Full SourceTrace completion is not completed.
- BoundaryCandidateService `VALID` production path is not wired.
- ExecutionPlan readiness is not upgraded.
- Schema and `dashboard.html` are not modified.
- External data integration, order API, and auto-trading are not added.

## Risk Action Guard

- High risk does not directly mean close, reverse, or open.
- Wick / pin-bar evidence does not confirm trend reversal.
- Liquidity stress / stampede must block opportunity push and require review.
- Missing event data is not no event risk.
- Multi-timeframe agreement alone does not complete SourceTrace.

## Recommended Next Stage

The next stage should remain fixture-only unless explicitly authorized otherwise. Recommended P42 scope is a test-design pack or fixture matrix that enumerates safe and unsafe SourceTrace entry completion inputs without implementing production adapters, wiring resolver/assembler output, completing SourceTrace, upgrading readiness, or generating real trading values.
