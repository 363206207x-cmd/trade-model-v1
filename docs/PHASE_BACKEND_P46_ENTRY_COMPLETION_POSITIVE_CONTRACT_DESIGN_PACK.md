# BACKEND-P46 Entry Completion Positive Contract Design Pack

## Baseline

- Branch context: PR #189 / Issue #188.
- Baseline commit: `b59acfe` (`docs: freeze entry completion contract review`).
- Scope: documentation-only positive SourceTrace entry completion contract design.
- This pack does not implement Java DTOs, production completion, production adapters, readiness wiring, schema/dashboard persistence, external integrations, order APIs, or auto-trading.

## Design Scope

P46 defines the future positive completion contract shape that may be implemented only in a later separately authorized phase. The design is intentionally non-runtime and non-production. It describes statuses, transitions, required fields, downgrade behavior, review-only invariants, test obligations, and still-blocked production paths.

## Positive Contract Shape

A future positive completion result may be designed around these conceptual fields:

- `completionStatus`
- `completionTransition`
- `sourceTraceEntryCompleted`
- `completionReady`
- `reviewMode`
- `manualReviewRequired`
- `notTradeInstruction`
- `symbol`
- `timeframe`
- `sourceTraceEntryOwnershipCompletionPath`
- `entryPriceSource`
- `entrySourceType`
- `entrySourceTimeframe`
- `entrySourceReason`
- `entrySourceRef`
- `ruleId`
- `ruleVersion`
- `sourceWindow`
- `freshnessStatus`
- `observedAtMs`
- `decisionCreateTimeMs`
- conflict evidence for stop, take-profit, risk-reward, liquidity, multi-timeframe, event, and wick
- `downgradeReason`
- `missingFields`

This is a design shape only. P46 does not add a Java DTO, implementation class, adapter, service registration, schema field, dashboard output, or readiness path.

## Future Status Names

Future design may use these status names:

- `INCOMPLETE`: completion is missing, unsafe, ambiguous, stale, conflicting, unwired, or not evaluated.
- `POSITIVE_FIXTURE_READY`: fixture-only evidence satisfies the positive completion ownership contract, but output remains review-only and non-trading.
- `POSITIVE_DESIGN_REVIEW_ONLY`: design-only positive shape is accepted for review but is not runtime completion.

P46 does not authorize a runtime `COMPLETE` status. A production `COMPLETE` status remains blocked until a later phase explicitly designs and approves production readiness, persistence, rollback, and safety behavior.

## Allowed Transitions

Allowed design transitions:

- `INCOMPLETE -> POSITIVE_FIXTURE_READY` only in deterministic fixture-only tests after every required ownership, freshness, conflict, provenance, and completion field is present.
- `POSITIVE_FIXTURE_READY -> INCOMPLETE` whenever any required field becomes missing, stale, conflicting, ambiguous, unsupported, mismatched, or unsafe.
- `INCOMPLETE -> POSITIVE_DESIGN_REVIEW_ONLY` only in documentation or design review artifacts.
- `POSITIVE_DESIGN_REVIEW_ONLY -> INCOMPLETE` whenever the design lacks proof of review-only safety, non-instructional output, or fail-closed rollback.

Forbidden transitions:

- `INCOMPLETE -> production COMPLETE`
- `POSITIVE_FIXTURE_READY -> BoundaryCandidateService VALID`
- `POSITIVE_FIXTURE_READY -> ExecutionPlan readiness`
- `POSITIVE_FIXTURE_READY -> order, close, reverse, automation, or auto-trading behavior`
- any transition driven only by latest price, kline items, symbol/timeframe metadata, multi-timeframe agreement, wick evidence, or AI/dashboard text

## `sourceTraceEntryCompleted` And `completionReady`

In a future non-production fixture-only contract, `sourceTraceEntryCompleted` and `completionReady` may become `true` only when all of these conditions are true:

- execution is inside a deterministic fixture-only test or design-only artifact
- every ownership field is explicitly present
- `sourceTraceEntryOwnershipCompletionPath` is present and fixture-owned
- `entryPriceSource` is fixture-owned and not derived from latest price, quote text, AI text, dashboard text, or raw kline data alone
- `entrySourceType` is from an allowed fixture source family
- `entrySourceTimeframe` matches the runtime decision timeframe
- `entrySourceReason` and `entrySourceRef` are present, singular, and unambiguous
- runtime symbol and timeframe match candidate symbol and decision timeframe
- `ruleId`, `ruleVersion`, and `sourceWindow` are present
- freshness is `FRESH`, observed time is present, decision-create time is present, and observed time is not after decision-create time
- every nullable conflict flag is explicitly `false`
- liquidity stress/stampede evidence is absent
- event data is present and evaluated
- multi-timeframe evidence is evaluated but not treated as completion by itself
- wick/pin-bar evidence is evaluated but not treated as reversal confirmation by itself
- output remains `REVIEW_ONLY`
- `manualReviewRequired=true`
- `notTradeInstruction=true`

Even under these non-production conditions, true values must not imply trade readiness, order readiness, dashboard readiness, schema persistence, BoundaryCandidateService `VALID`, ExecutionPlan readiness, or auto-trading readiness.

## Required Ownership Fields For Positive Fixture

A positive fixture must include:

- `sourceTraceEntryOwnershipCompletionPath`
- `entryPriceSource`
- `entrySourceType`
- `entrySourceTimeframe`
- `entrySourceReason`
- `entrySourceRef`
- candidate symbol
- candidate decision timeframe
- candidate boundary
- `ruleId`
- `ruleVersion`
- `sourceWindow`
- `freshnessStatus`
- `observedAtMs`
- `decisionCreateTimeMs`
- `conflictsWithStop=false`
- `conflictsWithTakeProfit=false`
- `conflictsWithRiskReward=false`
- `conflictsWithLiquidity=false`
- `conflictsWithMultiTimeframe=false`
- `conflictsWithEvent=false`
- `conflictsWithWick=false`
- event data evaluated marker
- liquidity stress evaluated marker
- multi-timeframe evidence evaluated marker
- wick evidence evaluated marker

The fixture may use synthetic values only. It must not generate real entry, stop, take-profit, risk-reward, liquidity, event, multi-timeframe, or wick trading values.

## Required Fail-Closed Downgrade Reasons

Any future positive-looking completion must downgrade to `INCOMPLETE` for:

- missing completion path
- missing entry source field
- missing provenance field
- unsupported source type
- unsupported source timeframe
- symbol mismatch
- timeframe mismatch
- duplicate or ambiguous source ref
- missing freshness status
- stale freshness status
- missing observed time
- missing decision-create time
- observed time after decision-create time
- missing conflict metadata
- any conflict flag `null`
- any conflict flag `true`
- liquidity stress
- stampede risk
- missing event data
- multi-timeframe agreement alone
- wick or pin-bar evidence alone
- unsafe `manualReviewRequired=false`
- unsafe `notTradeInstruction=false`
- attempted production wiring
- attempted readiness upgrade
- attempted schema/dashboard persistence
- attempted order/automation behavior

Downgrade must preserve missing field names where possible.

## Review-Only Acceptance Rules

Every future positive fixture must still satisfy:

- `reviewMode=REVIEW_ONLY`
- `manualReviewRequired=true`
- `notTradeInstruction=true`
- no trade instruction text
- no order placement
- no close or reverse behavior
- no auto-trading behavior
- no BoundaryCandidateService `VALID`
- no ExecutionPlan readiness upgrade
- no dashboard or schema persistence
- no external data integration

Review-only acceptance means the fixture is acceptable for contract design review only. It does not mean trade readiness.

## Trade Instruction Prohibition

The positive contract must explicitly prohibit:

- entry instructions
- stop instructions
- take-profit instructions
- risk-reward instructions
- order placement
- position close
- position reverse
- auto-trading
- implied readiness from risk score, wick evidence, liquidity evidence, event absence, or multi-timeframe agreement

`notTradeInstruction=true` is mandatory for every positive fixture and every downgrade path.

## Tests Required Before Any Java Implementation

Before any Java positive completion DTO, adapter, resolver, service, or runtime path is implemented, a future phase must add tests proving:

- default positive contract design remains fail closed
- positive fixture cannot exist with any missing ownership field
- positive fixture cannot exist with unsupported source type
- positive fixture cannot exist with unsupported source timeframe
- positive fixture cannot exist with mismatched symbol or timeframe
- positive fixture cannot exist with ambiguous source ref
- positive fixture cannot exist with missing provenance
- positive fixture cannot exist with stale, future, inverted, or missing freshness timestamps
- positive fixture cannot exist with null conflict flags
- positive fixture cannot exist with true conflict flags
- explicit false conflict flags do not bypass missing completion fields
- liquidity stress and stampede downgrade to incomplete
- missing event data downgrades to incomplete
- multi-timeframe agreement alone downgrades to incomplete
- wick or pin-bar evidence alone downgrades to incomplete
- positive fixture remains review-only and non-instructional
- positive fixture does not expose order, execution, close, reverse, auto-trading, or trade-ready method names
- positive fixture does not register Spring services
- positive fixture does not wire BoundaryCandidateService `VALID`
- positive fixture does not upgrade ExecutionPlan readiness
- positive fixture does not modify schema or dashboard output
- downgrade reasons are preserved with missing fields
- rollback from positive-looking fixture to `INCOMPLETE` is deterministic

## Conditions Still Blocking Production Wiring

Production wiring remains blocked until a later phase separately authorizes and proves:

- production positive completion contract implementation
- production adapter safety
- persistence model and migration safety
- dashboard rendering safety
- readiness transition safety
- BoundaryCandidateService `VALID` safety
- ExecutionPlan readiness safety
- external data dependency safety
- order API isolation
- auto-trading prohibition
- rollback from any runtime completion state to fail-closed state
- operational observability and audit requirements

None of these are authorized by P46.

## Rollback / Downgrade Contract

Rollback must be deterministic and immediate:

- if any required field is missing, downgrade to `INCOMPLETE`
- if any field is ambiguous, downgrade to `INCOMPLETE`
- if any freshness evidence is stale, future, inverted, or missing, downgrade to `INCOMPLETE`
- if any conflict flag is `null` or `true`, downgrade to `INCOMPLETE`
- if any safety invariant is false, downgrade to `INCOMPLETE`
- if any production wiring is detected in a fixture-only phase, downgrade to `INCOMPLETE`
- if any order/readiness/automation behavior is requested or implied, downgrade to `INCOMPLETE`

Downgraded output must keep:

- `sourceTraceEntryCompleted=false`
- `completionReady=false`
- `reviewMode=REVIEW_ONLY`
- `manualReviewRequired=true`
- `notTradeInstruction=true`

## Still-Unwired Fields

These remain intentionally unwired after P46:

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

- Design-only.
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

Recommended verification for this documentation-only design pack:

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

The next safe stage should remain fixture-only unless explicitly authorized otherwise. A safe P47 boundary would be a fixture-only positive contract test-design matrix that encodes this P46 design without adding Java production DTOs, production adapters, readiness wiring, schema/dashboard persistence, order APIs, or auto-trading.
