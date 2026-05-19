# BACKEND-P61 Entry Completion Read-Only Production Boundary Design Pack

## Baseline

- Issue context: #218.
- Branch: `q61`.
- Baseline commit: `65bc1e6` (`docs: freeze entry completion fixture extension safety`).
- Scope: documentation-only read-only production boundary design pack.
- P61 does not modify Java, tests, schema, `dashboard.html`, config, production wiring, external integrations, order APIs, or auto-trading.
- Temporary marker `m.txt` is removed.

## P50-P60 Chain Summary

The current Entry Completion chain is intentionally staged and fail-closed:

- P50 added `SourceTraceEntryPositiveCompletionContractDTO` and related status / transition / downgrade enums as inert DTO-only shapes.
- P51 expanded DTO guard coverage so malformed, unsafe, mutable, or production-like metadata cannot become runtime completion or readiness.
- P52 froze DTO safety invariants and authorized only fixture-only factory/mapper design as a future step.
- P53 designed fixture-only factory/mapper responsibilities without adding Java.
- P54 added test-scope fixture-only input / factory / mapper helpers for synthetic DTO metadata.
- P55 expanded fixture factory/mapper guards for null, empty, runtime-like, mutable, and production-like inputs.
- P56 froze the fixture factory/mapper chain as test-scope support only.
- P57 authorized a strictly test-scope fixture mapper/factory extension.
- P58 added deterministic synthetic evidence shape/ref fixture extension helpers under test sources.
- P59 expanded guards for evidence shape/ref metadata, runtime-looking text, duplicate refs, mixed runtime-like tags, defensive copies, and non-readiness behavior.
- P60 froze the P58-P59 fixture mapper extension safety state.

Across P50-P60, the chain remains:

- fail-closed by default
- review-only
- non-instructional
- not completion-ready
- not runtime SourceTrace completion
- not BoundaryCandidateService `VALID`
- not ExecutionPlan readiness
- not dashboard/schema persistence
- not order, execution, automation, or auto-trading

## Why Fixture-Only Chain Can Inform A Read-Only Production Boundary

The fixture-only chain can now inform a future read-only boundary because it has defined and guarded the metadata contract shape without granting runtime authority:

- the DTO carries the field names and safety flags a read-only boundary would need to expose
- tests prove positive-looking metadata cannot flip `sourceTraceEntryCompleted` or `completionReady`
- fixture mappers show how synthetic evidence can be represented without becoming trade instructions
- downgrade reasons and missing fields preserve explicit fail-closed state
- runtime-like source tags prove unsafe sources must downgrade rather than infer ownership
- defensive-copy guards prevent mutable input from changing stored output state
- repeated freezes document that no production wiring exists

This makes the chain useful as a design reference for a future assembler, but not as executable production completion logic.

## What Read-Only Production-Facing Means

Read-only production-facing means a future boundary may be shaped for production package visibility or future production callers, while remaining observational and inert:

- it may assemble a review-only DTO from already-available internal inputs
- it may normalize missing or unsafe inputs into fail-closed output
- it may expose missing-field and downgrade metadata for review
- it may preserve symbol/timeframe/provenance as metadata only
- it may preserve manual review and non-instructional flags
- it must not generate entry, stop, take-profit, risk-reward, liquidity, multi-timeframe, event, or wick values
- it must not mark SourceTrace complete
- it must not make completion ready
- it must not alter readiness, dashboard, schema, order, automation, or external data paths

Read-only production-facing does not mean production-wired, trade-ready, persisted, executable, or authoritative.

## What Is Still Forbidden

These remain forbidden after P61:

- real entry price generation
- real stop / take-profit / risk-reward generation
- real liquidity / multi-timeframe / event / wick value generation
- production completion implementation
- production adapter implementation
- `DefaultSourceTraceEntryOwnershipAdapter`
- production `DefaultSourceTraceEntryCompletionContract`
- Spring service/component registration for DTO/factory/mapper/assembler
- resolver wiring
- validation wiring
- readiness wiring
- dashboard wiring
- schema/database persistence
- order wiring
- automation wiring
- external data wiring
- runtime SourceTrace field population
- full SourceTrace runtime completion
- BoundaryCandidateService `VALID`
- ExecutionPlan readiness upgrade
- `dashboard.html` changes
- external data integration
- order API
- auto-trading

## Future Read-Only Assembler Responsibilities

A future read-only assembler skeleton, if separately authorized, must be responsible only for safe, inert composition:

- accept explicitly provided read-only inputs
- start from fail-closed DTO defaults
- copy mutable lists defensively
- preserve review-only and non-instructional flags
- preserve `manualReviewRequired=true`
- preserve `notTradeInstruction=true`
- preserve `sourceTraceEntryCompleted=false`
- preserve `completionReady=false`
- populate only allowed metadata fields when provided by safe internal inputs
- emit missing fields for absent ownership/source/provenance/freshness/conflict/completion data
- emit downgrade reasons for unsafe, ambiguous, runtime-like, stale, conflicting, or incomplete data
- never infer ownership from latest price, raw kline data, AI text, dashboard text, external data, order data, or execution data
- never call order, execution, close, reverse, automation, dashboard, schema, or external-data services
- never register as a Spring service unless a future phase explicitly authorizes registration and wiring

## Future Read-Only Mapper Responsibilities

A future read-only mapper skeleton, if separately authorized, must remain subordinate to the assembler and must not create completion authority:

- map safe source metadata into DTO fields only when explicitly present
- map missing values into `missingFields`
- map unsafe values into fail-closed downgrade reasons
- treat runtime-like values as unsafe
- keep source tags and evidence tags informational only
- preserve defensive copies on all list-like evidence
- avoid method names that imply order, execution, close, reverse, auto-trading, or trade readiness
- implement no production resolver, adapter, validator, dashboard, schema, order, automation, or external data boundary

## Required Inputs

A future read-only assembler may accept only explicitly provided internal objects or DTO-like inputs, such as:

- source completion metadata candidate
- symbol and timeframe metadata
- `sourceTraceEntryOwnershipCompletionPath`
- entry source type / timeframe / reason / ref metadata
- rule id / rule version / source window metadata
- freshness status
- observed timestamp
- decision-create timestamp
- nullable conflict evidence for stop, take-profit, risk-reward, liquidity, multi-timeframe, event, and wick
- downgrade reason candidates
- missing-field candidates

Inputs must remain internal and read-only. They must not be fetched from external APIs, Coinglass, news, macro calendars, order APIs, dashboard text, AI text, or auto-trading systems in the read-only skeleton phase.

## Required Outputs

A future read-only assembler output must preserve:

- `completionStatus=INCOMPLETE` unless a future explicitly authorized fixture-only or read-only status says otherwise
- `completionTransition=NONE` or a non-production read-only transition only if separately authorized
- `reviewMode=REVIEW_ONLY`
- `manualReviewRequired=true`
- `notTradeInstruction=true`
- `sourceTraceEntryCompleted=false`
- `completionReady=false`
- downgrade reason
- missing fields
- optional read-only symbol/timeframe/provenance metadata

Output must not be a trade instruction, readiness signal, schema persistence model, dashboard persistence model, order request, execution request, close/reverse command, or automation trigger.

## Required Fail-Closed Downgrade Behavior

A future read-only assembler must downgrade fail-closed when any of these are present:

- null request / null input bundle
- missing completion path
- missing entry source type
- missing entry source timeframe
- missing entry source reason
- missing entry source ref
- missing rule id
- missing rule version
- missing source window
- missing freshness status
- missing observed time
- missing decision-create time
- stale freshness status
- future observed time
- clock inversion
- nullable conflict flag missing / unevaluated
- any conflict flag true
- liquidity stress or stampede
- missing event data
- ambiguous or duplicate source refs
- latest price only
- raw kline only
- AI text
- dashboard text
- external data
- order data
- execution data
- runtime-looking fixture metadata
- unsafe manual review flag
- unsafe non-instructional flag

Downgrade output must remain review-only and must keep completion/readiness false.

## Required Review-Only Invariants

Any future read-only assembler skeleton must preserve:

- `REVIEW_ONLY`
- `manualReviewRequired=true`
- `notTradeInstruction=true`
- `sourceTraceEntryCompleted=false`
- `completionReady=false`
- no real entry price generation
- no real stop / take-profit / risk-reward generation
- no runtime SourceTrace field population
- no BoundaryCandidateService `VALID`
- no ExecutionPlan readiness upgrade
- no order/execution/close/reverse/auto-trading method surface
- no Spring service/component registration unless explicitly authorized later
- no dashboard/schema persistence
- no external data integration
- missing event data is not no event risk
- liquidity stress / stampede blocks completion and requires review
- multi-timeframe agreement alone does not complete SourceTrace
- wick / pin-bar evidence alone does not confirm trend reversal

## Required Tests Before Any Java Read-Only Skeleton

Before any Java read-only assembler skeleton is added, a future phase must define or add focused tests proving:

- default output is fail-closed
- null input fails closed
- missing completion path fails closed
- missing source type / timeframe / reason / ref fail closed independently
- missing rule id / rule version / source window fail closed independently
- missing freshness status / observed time / decision-create time fail closed independently
- stale freshness fails closed
- future observed time fails closed
- clock inversion fails closed
- null conflict flags fail closed
- true conflict flags fail closed one at a time
- latest price only fails closed
- raw kline only fails closed
- AI text / dashboard text fail closed
- external data / order data / execution data fail closed
- duplicate or ambiguous source refs fail closed
- liquidity stress / stampede fails closed and requires review
- missing event data fails closed
- multi-timeframe agreement alone does not complete SourceTrace
- wick / pin-bar evidence alone does not prove reversal or completion
- assembler exposes no order / execution / close / reverse / auto-trading / trade-ready method names
- assembler has no Spring annotations unless a later phase explicitly authorizes registration
- assembler implements no production boundary interfaces unless separately authorized
- output remains review-only and non-instructional
- output keeps `sourceTraceEntryCompleted=false`
- output keeps `completionReady=false`
- production adapter and production completion contract remain absent

## Production Wiring Blockers

Production wiring remains blocked by:

- no approved production completion implementation
- no approved production entry ownership adapter
- no approved runtime SourceTrace completion path
- no approved BoundaryCandidateService `VALID` path
- no approved ExecutionPlan readiness upgrade
- no approved schema or dashboard persistence design
- no approved external data dependency design
- no approved order/execution isolation design
- no approved auto-trading prohibition enforcement design
- no approved operational rollback / audit design

## Authorization Decision

Decision: a future read-only assembler skeleton may start next only if separately authorized and kept strictly read-only, fail-closed, review-only, non-instructional, and unwired.

This is not authorization for production completion, production adapter behavior, readiness wiring, dashboard/schema persistence, external integrations, order APIs, or auto-trading.

## Strict Next-Stage Scope

The next allowed stage may add only one of:

- documentation-only read-only assembler Java design details
- Java skeleton for a read-only assembler with focused tests, if separately authorized
- fixture-only tests for the read-only assembler contract

If a Java skeleton is authorized, it must:

- avoid Spring service/component annotations
- avoid production wiring
- avoid resolver, validation, readiness, dashboard, schema, order, automation, and external-data paths
- start from fail-closed DTO defaults
- preserve all review-only invariants
- generate no real entry / stop / take-profit / risk-reward values
- complete no SourceTrace path

## Boundary Confirmations

- Documentation-only.
- No Java modified in P61.
- No tests modified in P61.
- No read-only assembler Java skeleton added in P61.
- No fixture mapper Java extension added in P61.
- No production completion added.
- No production adapter added.
- No `DefaultSourceTraceEntryOwnershipAdapter` added.
- No production `DefaultSourceTraceEntryCompletionContract` added.
- DTO/factory/mapper/assembler are not registered as Spring services.
- DTO/factory/mapper/assembler are not wired into resolver, validation, readiness, dashboard, schema, order, automation, or external data paths.
- Runtime SourceTrace fields are not populated.
- Full SourceTrace is not completed in runtime.
- BoundaryCandidateService `VALID` production path remains unwired.
- ExecutionPlan readiness is not upgraded.
- Schema and `dashboard.html` are unchanged.
- Config is unchanged.
- External data integration, order API, and auto-trading are not added.
- Real entry, stop, take-profit, and risk-reward values are not generated.
- Temporary marker `m.txt` was removed.

## Verification

Recommended verification for P61:

```bash
./mvnw -q -Dtest=SourceTraceEntryPositiveCompletionFixtureFactoryMapperTest test
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
