# BACKEND-P47 Entry Completion Positive Contract Fixture Design Matrix

## Baseline

- Branch context: PR #191 / Issue #190.
- Baseline commit: `54323d0` (`docs: design entry completion positive contract`).
- Scope: documentation-only fixture design matrix for the P46 positive SourceTrace entry completion contract.
- Runtime completion remains unimplemented and unwired.
- This matrix does not add Java positive completion DTOs, production completion, production adapters, readiness wiring, schema/dashboard persistence, external integrations, order APIs, or auto-trading.

## Design Matrix Coverage

P47 encodes the P46 design into fixture-only matrices covering:

- positive fixture required fields
- allowed fixture-only transitions
- downgrade paths for missing, unsupported, mismatched, ambiguous, stale, conflicting, unsafe, or production-wired inputs
- review-only acceptance gates
- trade-instruction prohibitions
- still-blocked Java implementation paths
- still-blocked production wiring paths
- exact future tests required before any implementation phase

## Positive Fixture Required Fields Matrix

| Field / Evidence | Required Fixture Condition | Positive Fixture Meaning | Downgrade If |
| --- | --- | --- | --- |
| `sourceTraceEntryOwnershipCompletionPath` | present and fixture-owned | fixture has a declared completion path | missing, ambiguous, or runtime-wired |
| `entryPriceSource` | present as synthetic fixture value | fixture source owns an entry source candidate | missing, latest-price-derived, quote-derived, AI-derived, dashboard-derived, or raw-kline-derived alone |
| `entrySourceType` | allowed fixture source family | source family is explicitly owned | blank, unknown, unsupported |
| `entrySourceTimeframe` | equals runtime decision timeframe | timeframe ownership matches runtime context | blank, unknown, unsupported, mismatched |
| `entrySourceReason` | present and specific | source reason is auditable provenance | blank, generic, trade-instructional |
| `entrySourceRef` | present, singular, unambiguous | one source reference owns the fixture | blank, duplicate, ambiguous |
| candidate symbol | equals runtime symbol | candidate and runtime refer to same market | missing, mismatched |
| candidate decision timeframe | equals runtime timeframe | candidate and runtime share decision timeframe | missing, mismatched |
| candidate boundary | present as fixture metadata | rule-owned candidate boundary exists | missing or treated as real entry price |
| `ruleId` | present | rule provenance exists | missing |
| `ruleVersion` | present | rule version provenance exists | missing |
| `sourceWindow` | present | source evidence window exists | missing |
| `freshnessStatus` | `FRESH` | freshness is explicitly evaluated | missing, stale, unknown |
| `observedAtMs` | present and not after decision time | observation time is valid | missing, future, clock-inverted |
| `decisionCreateTimeMs` | present | decision time exists | missing |
| stop conflict | `conflictsWithStop=false` | stop conflict evaluated as absent | `null`, `true`, unevaluated |
| take-profit conflict | `conflictsWithTakeProfit=false` | TP conflict evaluated as absent | `null`, `true`, unevaluated |
| risk-reward conflict | `conflictsWithRiskReward=false` | RR conflict evaluated as absent | `null`, `true`, unevaluated |
| liquidity conflict | `conflictsWithLiquidity=false` | liquidity conflict evaluated as absent | `null`, `true`, liquidity stress, stampede |
| multi-timeframe conflict | `conflictsWithMultiTimeframe=false` | MTF conflict evaluated as absent | `null`, `true`, agreement-only evidence |
| event conflict | `conflictsWithEvent=false` | event conflict evaluated as absent | `null`, `true`, missing event data |
| wick conflict | `conflictsWithWick=false` | wick conflict evaluated as absent | `null`, `true`, wick/pin-bar-only reversal claim |
| `reviewMode` | `REVIEW_ONLY` | fixture is for human review only | any readiness or execution mode |
| `manualReviewRequired` | `true` | human review remains mandatory | `false` |
| `notTradeInstruction` | `true` | fixture is non-instructional | `false` |

All positive fixture values are synthetic. No real entry, stop, take-profit, risk-reward, liquidity, event, multi-timeframe, or wick trading values may be generated.

## Allowed Fixture-Only Transitions Matrix

| From | To | Allowed Only When | Runtime Meaning |
| --- | --- | --- | --- |
| `INCOMPLETE` | `POSITIVE_FIXTURE_READY` | every required field in the positive fixture matrix is present, evaluated, non-conflicting, and review-only | no runtime completion; fixture design only |
| `POSITIVE_FIXTURE_READY` | `INCOMPLETE` | any required field becomes missing, unsupported, mismatched, ambiguous, stale, conflicting, unsafe, or runtime-wired | deterministic fail-closed downgrade |
| `INCOMPLETE` | `POSITIVE_DESIGN_REVIEW_ONLY` | documentation accepts the shape for review | design-only, not runtime |
| `POSITIVE_DESIGN_REVIEW_ONLY` | `INCOMPLETE` | review-only safety, non-instructional output, or rollback proof is missing | design rejected back to fail closed |

Forbidden transitions:

- `INCOMPLETE -> production COMPLETE`
- `POSITIVE_FIXTURE_READY -> production COMPLETE`
- `POSITIVE_FIXTURE_READY -> BoundaryCandidateService VALID`
- `POSITIVE_FIXTURE_READY -> ExecutionPlan readiness`
- `POSITIVE_FIXTURE_READY -> dashboard/schema persistence`
- `POSITIVE_FIXTURE_READY -> order, close, reverse, automation, or auto-trading`

## Downgrade Matrix

| Trigger | Downgrade Status | Required Missing / Reason Signal |
| --- | --- | --- |
| missing completion path | `INCOMPLETE` | `sourceTraceEntryOwnershipCompletionPath` |
| ambiguous completion path | `INCOMPLETE` | `sourceTraceEntryOwnershipCompletionPath` |
| missing `entryPriceSource` | `INCOMPLETE` | `entryPriceSource` |
| latest price used as entry source alone | `INCOMPLETE` | `entryPriceSource` |
| raw kline data used as entry source alone | `INCOMPLETE` | `entryPriceSource` |
| missing source type | `INCOMPLETE` | `entrySourceType` |
| unsupported source type | `INCOMPLETE` | `entrySourceType` |
| missing source timeframe | `INCOMPLETE` | `entrySourceTimeframe` |
| unsupported source timeframe | `INCOMPLETE` | `entrySourceTimeframe` |
| runtime/candidate timeframe mismatch | `INCOMPLETE` | `entrySourceTimeframe` or candidate decision timeframe |
| missing source reason | `INCOMPLETE` | `entrySourceReason` |
| source reason contains trade instruction | `INCOMPLETE` | `entrySourceReason`, `notTradeInstruction` |
| missing source ref | `INCOMPLETE` | `entrySourceRef` |
| duplicate or ambiguous source ref | `INCOMPLETE` | `entrySourceRef` |
| missing candidate symbol | `INCOMPLETE` | candidate symbol |
| runtime/candidate symbol mismatch | `INCOMPLETE` | candidate symbol |
| missing candidate boundary | `INCOMPLETE` | candidate boundary |
| candidate boundary treated as real entry price | `INCOMPLETE` | candidate boundary, `entryPriceSource` |
| missing rule id | `INCOMPLETE` | `ruleId` |
| missing rule version | `INCOMPLETE` | `ruleVersion` |
| missing source window | `INCOMPLETE` | `sourceWindow` |
| missing freshness status | `INCOMPLETE` | `freshnessStatus` |
| stale freshness status | `INCOMPLETE` | `freshnessStatus` |
| missing observed time | `INCOMPLETE` | `observedAtMs` |
| missing decision-create time | `INCOMPLETE` | `decisionCreateTimeMs` |
| observed time after decision-create time | `INCOMPLETE` | `observedAtMs`, `decisionCreateTimeMs` |
| missing conflict metadata | `INCOMPLETE` | conflict metadata |
| any conflict flag `null` | `INCOMPLETE` | matching conflict field |
| any conflict flag `true` | `INCOMPLETE` | matching conflict field |
| liquidity stress | `INCOMPLETE` | `conflictsWithLiquidity` |
| stampede risk | `INCOMPLETE` | `conflictsWithLiquidity` |
| missing event data | `INCOMPLETE` | `conflictsWithEvent` |
| multi-timeframe agreement alone | `INCOMPLETE` | `conflictsWithMultiTimeframe` |
| wick or pin-bar evidence alone | `INCOMPLETE` | `conflictsWithWick` |
| `manualReviewRequired=false` | `INCOMPLETE` | `manualReviewRequired` |
| `notTradeInstruction=false` | `INCOMPLETE` | `notTradeInstruction` |
| attempted production wiring | `INCOMPLETE` | production wiring violation |
| attempted readiness upgrade | `INCOMPLETE` | readiness violation |
| attempted schema/dashboard persistence | `INCOMPLETE` | persistence violation |
| attempted order/automation behavior | `INCOMPLETE` | trade action violation |

All downgrades must preserve:

- `sourceTraceEntryCompleted=false`
- `completionReady=false`
- `reviewMode=REVIEW_ONLY`
- `manualReviewRequired=true`
- `notTradeInstruction=true`

## Review-Only Acceptance Matrix

| Acceptance Gate | Required Value | Failure Result |
| --- | --- | --- |
| review mode | `REVIEW_ONLY` | downgrade to `INCOMPLETE` |
| manual review | `manualReviewRequired=true` | downgrade to `INCOMPLETE` |
| non-instructional flag | `notTradeInstruction=true` | downgrade to `INCOMPLETE` |
| order surface | absent | downgrade to `INCOMPLETE` |
| close/reverse surface | absent | downgrade to `INCOMPLETE` |
| auto-trading surface | absent | downgrade to `INCOMPLETE` |
| BoundaryCandidateService `VALID` | not wired | downgrade to `INCOMPLETE` |
| ExecutionPlan readiness | not upgraded | downgrade to `INCOMPLETE` |
| schema/dashboard persistence | absent | downgrade to `INCOMPLETE` |
| external integration | absent | downgrade to `INCOMPLETE` |

Review-only acceptance means the fixture is acceptable for contract design review only. It does not mean runtime completion, trade readiness, order readiness, or automation readiness.

## Trade-Instruction Prohibition Matrix

| Prohibited Input / Behavior | Required Response |
| --- | --- |
| entry instruction text | downgrade to `INCOMPLETE` |
| stop instruction text | downgrade to `INCOMPLETE` |
| take-profit instruction text | downgrade to `INCOMPLETE` |
| risk-reward instruction text | downgrade to `INCOMPLETE` |
| order placement method or intent | downgrade to `INCOMPLETE` |
| close/reverse method or intent | downgrade to `INCOMPLETE` |
| auto-trading method or intent | downgrade to `INCOMPLETE` |
| risk score treated as action | downgrade to `INCOMPLETE` |
| wick/pin-bar treated as reversal confirmation | downgrade to `INCOMPLETE` |
| liquidity stress treated as opportunity | downgrade to `INCOMPLETE` |
| missing event data treated as no event risk | downgrade to `INCOMPLETE` |
| multi-timeframe agreement treated as SourceTrace completion | downgrade to `INCOMPLETE` |

## Conditions Still Blocking Java Implementation

Java implementation remains blocked until a future phase adds and passes tests for:

- default positive contract object remains fail closed
- positive fixture cannot exist with any missing required field
- positive fixture cannot exist with unsupported or mismatched source metadata
- positive fixture cannot exist with ambiguous source reference
- positive fixture cannot exist with stale, future, inverted, or missing freshness
- positive fixture cannot exist with null or true conflict flags
- positive fixture cannot bypass missing completion with false conflict flags
- positive fixture remains review-only and non-instructional
- positive fixture exposes no order, execution, close, reverse, auto-trading, or trade-ready method names
- positive fixture registers no Spring production services
- positive fixture wires no BoundaryCandidateService `VALID`
- positive fixture upgrades no ExecutionPlan readiness
- downgrade reasons and missing fields are preserved
- rollback from positive-looking fixture to `INCOMPLETE` is deterministic

P47 does not add Java positive completion DTOs or implementations.

## Conditions Still Blocking Production Wiring

Production wiring remains blocked by:

- no production positive completion contract implementation
- no production adapter safety proof
- no persistence model or migration safety proof
- no dashboard rendering safety proof
- no readiness transition safety proof
- no BoundaryCandidateService `VALID` safety proof
- no ExecutionPlan readiness safety proof
- no external data dependency safety proof
- no order API isolation proof
- no auto-trading prohibition proof
- no runtime rollback proof
- no operational observability and audit design

None of these are authorized by P47.

## Exact Future Tests Required Before Any Implementation Phase

Before any Java implementation phase, add tests named or scoped to prove:

- `positiveFixtureRequiresCompletionPath`
- `positiveFixtureRequiresEntryPriceSource`
- `positiveFixtureRejectsLatestPriceOnly`
- `positiveFixtureRejectsRawKlineOnly`
- `positiveFixtureRequiresAllowedSourceType`
- `positiveFixtureRequiresMatchingSourceTimeframe`
- `positiveFixtureRequiresSourceReason`
- `positiveFixtureRejectsTradeInstructionReason`
- `positiveFixtureRequiresSingularSourceRef`
- `positiveFixtureRequiresCandidateProvenance`
- `positiveFixtureRejectsSymbolMismatch`
- `positiveFixtureRejectsTimeframeMismatch`
- `positiveFixtureRejectsStaleFreshness`
- `positiveFixtureRejectsFutureObservedTime`
- `positiveFixtureRejectsClockInversion`
- `positiveFixtureRejectsNullConflictFlags`
- `positiveFixtureRejectsTrueConflictFlags`
- `positiveFixtureRejectsLiquidityStress`
- `positiveFixtureRejectsMissingEventData`
- `positiveFixtureRejectsMultiTimeframeAgreementOnly`
- `positiveFixtureRejectsWickPinBarOnly`
- `positiveFixtureRemainsReviewOnly`
- `positiveFixtureCannotBecomeTradeInstruction`
- `positiveFixtureDoesNotWireReadiness`
- `positiveFixtureDoesNotPersistDashboardOrSchema`
- `positiveFixtureDowngradesDeterministically`

These tests must be fixture-only until a later phase separately authorizes implementation.

## Still-Unwired Fields

These remain intentionally unwired after P47:

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

## Boundary Confirmations

- Design-only / fixture-matrix-only.
- No Java positive completion DTO or implementation added.
- No Java production code changed.
- No Java tests changed.
- No schema, `dashboard.html`, config, or production wiring changed.
- No real entry, stop, take-profit, or risk-reward values are generated.
- No production entry ownership adapter is implemented.
- No `DefaultSourceTraceEntryOwnershipAdapter` is added.
- No production `DefaultSourceTraceEntryCompletionContract` is added.
- Resolver and assembler are not registered as production Spring services.
- Resolver and assembler are not wired into validation, readiness, dashboard, schema, order, or automation paths.
- Real SourceTrace fields are not populated.
- Full SourceTrace completion is not completed in runtime.
- BoundaryCandidateService `VALID` production path is not wired.
- ExecutionPlan readiness is not upgraded.
- External data integration, order API, and auto-trading are not added.

## Verification Commands

Recommended verification for this documentation-only matrix:

```bash
./mvnw -q -Dtest=EntryCompletionOwnershipContractFixtureTest test
./mvnw -q -Dtest=EntryCompletionFixtureMatrixGuardTest test
./mvnw -q -Dtest=FailClosedSourceTraceEntryOwnershipValidatorTest test
./mvnw -q -DskipTests compile
./mvnw -q -DskipTests test-compile
```

## Risk Action Guard

- High risk does not directly mean close, reverse, or open.
- Wick / pin-bar evidence does not confirm trend reversal.
- Liquidity stress / stampede must block opportunity push and require review.
- Missing event data is not no event risk.
- Multi-timeframe agreement alone does not complete SourceTrace.

## Recommended Next Stage

The next safe stage should remain fixture-only unless explicitly authorized otherwise. A safe P48 boundary would add fixture-only test skeletons for the P47 matrix without adding Java positive completion DTOs, production adapters, readiness wiring, schema/dashboard persistence, order APIs, or auto-trading.
