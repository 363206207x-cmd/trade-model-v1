# BACKEND-P48 Entry Completion Positive Contract Fixture Test Skeletons

## Baseline

- Branch context: PR #193 / Issue #192.
- Baseline commit: `f8b8176` (`docs: add entry completion fixture design matrix`).
- Relation to P47: P47 defined the positive completion fixture design matrix and exact future tests required before any implementation phase.
- P48 turns that matrix into fixture-only Java test skeletons and keeps runtime completion unwired.

## Scope

P48 adds deterministic fixture-only test coverage for positive-looking SourceTrace entry completion inputs. The tests prove that those fixtures still downgrade to fail-closed, review-only, non-instructional output until a separately authorized implementation phase exists.

P48 does not add Java positive completion DTOs, production completion, production adapters, readiness wiring, schema or dashboard persistence, external integrations, order APIs, or auto-trading.

## Files Changed

- Added `src/test/java/org/example/trademodel/service/EntryCompletionPositiveContractFixtureSkeletonTest.java`.
- Added `docs/PHASE_BACKEND_P48_ENTRY_COMPLETION_POSITIVE_CONTRACT_FIXTURE_TEST_SKELETONS.md`.
- Removed placeholder `docs/P48.md`.

## Fixture Test Skeleton Coverage

The new P48 fixture test class covers the P47 positive fixture matrix names and keeps every case downgraded:

- positive fixture requires completion path
- positive fixture requires entry price source
- positive fixture rejects latest-price-only input
- positive fixture rejects raw-kline-only input
- positive fixture requires allowed source type
- positive fixture requires matching source timeframe
- positive fixture requires source reason
- positive fixture rejects trade-instruction reason
- positive fixture requires singular source ref
- positive fixture requires candidate provenance
- positive fixture rejects symbol mismatch
- positive fixture rejects timeframe mismatch
- positive fixture rejects stale freshness
- positive fixture rejects future observed time
- positive fixture rejects clock inversion
- positive fixture rejects null conflict flags
- positive fixture rejects true conflict flags
- positive fixture rejects liquidity stress
- positive fixture rejects missing event data
- positive fixture rejects multi-timeframe agreement only
- positive fixture rejects wick / pin-bar only
- positive fixture remains review-only
- positive fixture cannot become a trade instruction
- positive fixture does not wire readiness
- positive fixture does not persist dashboard or schema state
- positive fixture downgrades deterministically

## Downgrade Behavior

Every positive-looking fixture remains downgraded to fail closed:

- validation status remains `INCOMPLETE`
- completion status remains `INCOMPLETE`
- review mode remains `REVIEW_ONLY`
- `sourceTraceEntryCompleted=false`
- `completionReady=false`
- `manualReviewRequired=true`
- `notTradeInstruction=true`
- SourceTrace entry fields remain null
- missing, ambiguous, stale, conflicting, unsafe, or unwired fixture evidence never becomes readiness

The current completed-looking fixture still downgrades through the unwired completion path:

- `sourceTraceEntryOwnershipCompletionPath`
- `COMPLETION_UNWIRED`

Invalid fixture variants downgrade through their existing fail-closed validation fields or through the still-unwired completion path when the future positive contract does not exist yet.

## Still-Blocked Implementation Paths

The following remain blocked after P48:

- Java positive completion DTO or implementation
- production completion resolver
- production entry ownership adapter
- `DefaultSourceTraceEntryOwnershipAdapter`
- production `DefaultSourceTraceEntryCompletionContract`
- real entry, stop, take-profit, risk-reward, liquidity, event, multi-timeframe, or wick value generation
- SourceTrace runtime completion
- BoundaryCandidateService `VALID`
- ExecutionPlan readiness upgrade
- schema or dashboard persistence
- external data integration
- order API
- auto-trading

## Still-Blocked Production Wiring

No resolver, assembler, positive fixture, or completion result is wired into:

- validation readiness
- dashboard rendering
- schema persistence
- order paths
- automation paths
- external API paths
- Coinglass
- news or macro APIs
- auto-trading

## Still-Unwired Fields

These fields remain intentionally unwired and null or missing in runtime completion output:

- `sourceTraceEntryOwnershipCompletionPath`
- `entryPriceSource`
- `entrySourceType`
- `entrySourceTimeframe`
- `entrySourceReason`
- `entrySourceRef`
- full SourceTrace entry completion
- BoundaryCandidateService `VALID`
- ExecutionPlan readiness
- dashboard and schema persistence

## Verification

Required verification for P48:

```bash
./mvnw -q -Dtest=EntryCompletionOwnershipContractFixtureTest test
./mvnw -q -Dtest=EntryCompletionFixtureMatrixGuardTest test
./mvnw -q -Dtest=FailClosedSourceTraceEntryOwnershipValidatorTest test
./mvnw -q -Dtest=EntryCompletionPositiveContractFixtureSkeletonTest test
./mvnw -q -DskipTests compile
./mvnw -q -DskipTests test-compile
```

## Boundary Confirmations

- Fixture-only / test-skeleton-only.
- No Java positive completion DTO or implementation was added.
- No production completion was implemented.
- No production adapter was implemented.
- No readiness wiring was added.
- No schema or `dashboard.html` persistence was added.
- No external integration, order API, or auto-trading was added.
- No real entry, stop, take-profit, or risk-reward values were generated.
- `docs/P48.md` was removed.

## Risk Action Guard

- High risk does not directly mean close, reverse, or open.
- Wick / pin-bar evidence does not confirm trend reversal.
- Liquidity stress / stampede must block opportunity push and require review.
- Missing event data is not no event risk.
- Multi-timeframe agreement alone does not complete SourceTrace.

## Recommended Next Stage

The next stage should remain separately authorized and should not implement production completion unless all fixture-only downgrade tests remain green and a future task explicitly authorizes a Java positive contract boundary. Until then, P48 keeps positive-looking fixtures review-only, non-instructional, and fail-closed.
