# BACKEND-P53 Entry Completion Positive DTO Fixture Factory Mapper Design Pack

## Baseline

- Branch context: PR #203 / Issue #202.
- Baseline commit: `c3d2906` (`docs: freeze entry completion DTO safety`).
- Scope: documentation-only fixture factory/mapper design pack for `SourceTraceEntryPositiveCompletionContractDTO`.
- P53 does not modify Java, tests, schema, `dashboard.html`, config, or production wiring.

## P50/P51/P52 DTO Safety Summary

P50 added the DTO-only positive completion contract skeleton:

- `SourceTraceEntryPositiveCompletionContractDTO`
- `SourceTraceEntryPositiveCompletionStatusEnum`
- `SourceTraceEntryPositiveCompletionTransitionEnum`
- `SourceTraceEntryPositiveCompletionDowngradeReasonEnum`

P51 expanded DTO-only guard coverage and proved:

- default state remains fail-closed
- positive fixture-ready metadata remains non-production
- null status / transition / downgrade reason normalize fail-closed
- empty missing fields normalize fail-closed
- `missingFields` set/get behavior uses defensive copies
- transition/status mismatch does not imply readiness
- unsafe downgrade reason does not change review-only safety flags
- `sourceTraceEntryCompleted=false` even with positive status
- `completionReady=false` even with positive transition
- synthetic fixture values do not infer real entry readiness
- DTO exposes no order / execution / close / reverse / auto-trading / trade-ready methods
- DTO is not a Spring service or component
- DTO implements no production boundary interfaces
- production adapter and production completion contract remain absent

P52 froze the DTO safety state and allowed only a separately authorized fixture-only factory or mapper design stage. P53 is that design stage. It does not add a Java factory or mapper skeleton.

## Fixture-Only Factory Responsibilities

A future fixture-only factory may be designed to create `SourceTraceEntryPositiveCompletionContractDTO` instances from deterministic test fixtures only.

The factory must:

- be named and packaged as fixture-only
- avoid Spring annotations and runtime service registration
- use synthetic fixture values only
- start from DTO fail-closed defaults
- populate only allowed synthetic fields
- keep `reviewMode=REVIEW_ONLY`
- keep `manualReviewRequired=true`
- keep `notTradeInstruction=true`
- keep `sourceTraceEntryCompleted=false` by default
- keep `completionReady=false` by default
- preserve missing fields unless a fixture-only test explicitly models a design-only positive metadata shape
- never derive entry metadata from runtime latest price, raw kline items, quote text, AI text, dashboard text, or external data
- never create order, execution, close, reverse, automation, auto-trading, or trade-ready behavior
- never wire DTO output into resolver, assembler, validation, readiness, dashboard, schema, order, automation, or external data paths

The factory may only help tests build deterministic DTO metadata. It must not be a production completion implementation.

## Fixture-Only Mapper Responsibilities

A future fixture-only mapper may be designed to map deterministic fixture structures into DTO metadata for test readability.

The mapper must:

- accept only fixture-owned inputs
- reject or downgrade missing, ambiguous, unsafe, stale, conflicting, or runtime-like inputs
- preserve all DTO safety invariants
- preserve downgrade reasons and missing-field evidence
- copy mutable collections defensively
- avoid output that can be interpreted as runtime SourceTrace completion
- avoid output that can be interpreted as BoundaryCandidateService `VALID`
- avoid output that can be interpreted as ExecutionPlan readiness
- expose no order / execution / close / reverse / auto-trading / trade-ready method names
- remain unregistered and unwired

The mapper may only support fixture-only tests. It must not map runtime market data into completion readiness.

## Allowed Synthetic Fields

Future fixture-only factory/mapper code may populate synthetic values for:

- symbol
- timeframe
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
- nullable conflict evidence for stop, take-profit, risk-reward, liquidity, multi-timeframe, event, and wick
- downgrade reason
- missing fields
- fixture-only completion status metadata
- fixture-only completion transition metadata

Allowed synthetic values must remain clearly fixture-owned. They must not represent real entry, stop, take-profit, risk-reward, liquidity, event, multi-timeframe, or wick trading values.

## Forbidden Runtime Fields And Sources

Future fixture-only factory/mapper code must not use or infer from:

- runtime latest price
- raw kline items alone
- quote text
- AI-generated text
- dashboard text
- persisted dashboard values
- external data integration
- Coinglass
- news API
- macro calendar API
- order API
- execution results
- live position state
- real stop, take-profit, risk-reward, liquidity, event, multi-timeframe, or wick values

Future fixture-only factory/mapper output must not populate runtime SourceTrace fields or signal:

- production `COMPLETE`
- runtime SourceTrace completion
- BoundaryCandidateService `VALID`
- ExecutionPlan readiness
- dashboard readiness
- schema persistence readiness
- order readiness
- automation readiness
- auto-trading readiness

## Required Downgrade Behavior

Future fixture-only factory/mapper code must downgrade or preserve fail-closed output when:

- completion path is missing or ambiguous
- entry source fields are missing
- source type is unsupported
- source timeframe is unsupported or mismatched
- source reason is missing, generic, or trade-instructional
- source ref is missing, duplicate, or ambiguous
- rule id, rule version, or source window is missing
- freshness status is missing, stale, unknown, or unsafe
- observed time is missing or future
- decision-create time is missing
- observed time is after decision-create time
- any conflict flag is null
- any conflict flag is true
- liquidity stress or stampede evidence exists
- event data is missing
- multi-timeframe agreement is used alone
- wick or pin-bar evidence is used alone
- runtime latest price is used alone
- raw kline items are used alone
- DTO metadata suggests production wiring
- DTO metadata suggests readiness, order, execution, close, reverse, automation, or auto-trading

Downgrade output must keep:

- `completionStatus=INCOMPLETE` unless a fixture-only test explicitly models metadata that remains non-production
- `reviewMode=REVIEW_ONLY`
- `manualReviewRequired=true`
- `notTradeInstruction=true`
- `sourceTraceEntryCompleted=false`
- `completionReady=false`
- missing fields or downgrade reason explaining why the fixture is not runtime-ready

## Required Tests Before Any Fixture Factory/Mapper Skeleton

Before any Java fixture factory/mapper skeleton is added, a future phase must add or prepare tests proving:

- default factory output remains fail-closed
- default mapper output remains fail-closed
- fixture-only positive metadata remains non-production
- synthetic entry metadata does not infer real entry readiness
- latest-price-only input is rejected
- raw-kline-only input is rejected
- quote, AI, dashboard, external, Coinglass, news, macro, order, or execution inputs are rejected or unavailable
- missing completion path downgrades
- missing entry source fields downgrade
- unsupported source type downgrades
- mismatched source timeframe downgrades
- missing or trade-instructional reason downgrades
- duplicate or ambiguous source ref downgrades
- missing provenance downgrades
- stale, future, inverted, or missing freshness downgrades
- null conflict flags downgrade
- true conflict flags downgrade
- liquidity stress or stampede downgrades
- missing event data downgrades
- multi-timeframe agreement alone downgrades
- wick / pin-bar evidence alone downgrades
- mutable input collections are defensively copied
- returned missing fields cannot mutate internal state
- factory/mapper exposes no order / execution / close / reverse / auto-trading / trade-ready method names
- factory/mapper has no Spring service/component annotations
- factory/mapper implements no production resolver, adapter, validator, assembler, readiness, dashboard, schema, order, automation, or external data boundary

## Authorization Decision

Decision: a future fixture-only factory/mapper skeleton may start next, but only if separately authorized and only under the strict scope below.

This decision does not authorize production completion, production adapters, runtime readiness, schema/dashboard persistence, order APIs, external integrations, or auto-trading.

## Strict Next-Stage Scope

The next fixture-only skeleton stage may add only:

- fixture-only factory and/or mapper Java skeletons
- focused fixture-only tests
- a result document

The next stage must:

- keep factory/mapper unregistered as Spring services
- keep factory/mapper outside production wiring
- use synthetic fixture values only
- preserve DTO fail-closed defaults
- preserve `REVIEW_ONLY`
- preserve `manualReviewRequired=true`
- preserve `notTradeInstruction=true`
- preserve `sourceTraceEntryCompleted=false` by default
- preserve `completionReady=false` by default
- prove no order / execution / close / reverse / auto-trading / trade-ready surface exists
- prove no resolver, assembler, validation, readiness, dashboard, schema, order, automation, or external data path is wired
- avoid real entry, stop, take-profit, risk-reward, liquidity, event, multi-timeframe, or wick values

## Still-Blocked Production Paths

These remain blocked after P53:

- production completion implementation
- production entry ownership adapter
- `DefaultSourceTraceEntryOwnershipAdapter`
- production `DefaultSourceTraceEntryCompletionContract`
- DTO/factory/mapper registration as Spring services
- resolver wiring
- assembler wiring
- validation wiring
- readiness wiring
- dashboard wiring
- schema or database persistence wiring
- order wiring
- automation wiring
- external data wiring
- runtime SourceTrace field population
- full SourceTrace runtime completion
- BoundaryCandidateService `VALID`
- ExecutionPlan readiness upgrade
- schema changes
- `dashboard.html` changes
- external data integration
- order API
- auto-trading
- real entry, stop, take-profit, or risk-reward value generation

## Boundary Confirmations

- Documentation-only.
- No Java modified in P53.
- No tests modified in P53.
- No fixture factory/mapper Java skeleton added in P53.
- No production completion implemented.
- No production adapter added.
- No `DefaultSourceTraceEntryOwnershipAdapter` added.
- No production `DefaultSourceTraceEntryCompletionContract` added.
- DTO/factory/mapper are not registered as Spring services.
- DTO is not wired into resolver, assembler, validation, readiness, dashboard, schema, order, automation, or external data paths.
- Runtime SourceTrace fields are not populated.
- Full SourceTrace is not completed in runtime.
- BoundaryCandidateService `VALID` production path remains unwired.
- ExecutionPlan readiness is not upgraded.
- Schema and `dashboard.html` are unchanged.
- Config is unchanged.
- External data integration, order API, and auto-trading are not added.
- Real entry, stop, take-profit, and risk-reward values are not generated.
- Placeholder `docs/P53_PLACEHOLDER.md` was removed.

## Verification

Recommended verification for P53:

```bash
./mvnw -q -Dtest=SourceTraceEntryPositiveCompletionContractDTOTest test
./mvnw -q -Dtest=EntryCompletionPositiveContractFixtureSkeletonTest test
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

The next safe step may be a separately authorized fixture-only factory/mapper skeleton stage with focused tests. It must preserve the DTO-only fail-closed, review-only, non-instructional boundary and must not introduce production wiring.
