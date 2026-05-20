# BACKEND-P86 Entry Completion Production Wiring Design Pack

## Baseline

- Branch context: PR #278 / Issue #277.
- Baseline commit: `4b6042c` (`docs: freeze entry ownership fixture gate`).
- Scope: documentation-only production wiring design pack for Entry Completion ownership.
- P86 removes placeholder `docs/P86.md`.

## P85 Freeze Recap

BACKEND-P85 froze the P83-P84 production ownership fixture matrix safety state.

Frozen fixture-only safety state:

- P83 established deterministic fixture-only skeleton tests for every production ownership field and matrix dimension.
- P84 expanded deterministic fixture-only guard coverage for runtime substitution sources, owner-present safety prerequisites, Risk Action Guard blockers, and positive-looking fixture values.
- Fixture output remains review-only and non-instructional.
- `completionStatus=INCOMPLETE`
- `completionTransition=NONE`
- `reviewMode=REVIEW_ONLY`
- `manualReviewRequired=true`
- `notTradeInstruction=true`
- `sourceTraceEntryCompleted=false`
- `completionReady=false`
- BoundaryCandidateService `VALID=false`
- ExecutionPlan readiness false
- dashboard mutation disabled
- production wiring not created
- Spring service registration not created
- no real entry / stop / TP / RR values generated

P85 decided that production wiring implementation may not start. It allowed only a documentation-only production wiring design stage next.

## Proposed Future Production-Facing Read-Only Ownership Boundary

The smallest future production-facing boundary should be a read-only ownership evidence boundary that accepts explicit production owner evidence and returns fail-closed review output.

Proposed boundary purpose:

- collect already-owned production evidence for Entry Completion ownership fields
- validate that each field has exactly one allowed owner
- reject missing, duplicate, ambiguous, stale, forbidden, unaudited, unauthorized, or non-isolated evidence
- preserve review-only and non-instructional output
- expose blockers for human review without creating runtime SourceTrace completion
- never generate real entry / stop / TP / RR values
- never feed readiness, `VALID`, dashboard mutation, schema persistence, order, execution, automation, or external data paths

Proposed boundary name, design-only:

- `SourceTraceEntryProductionOwnershipReviewBoundary`

This name is descriptive only. P86 does not add Java.

## Proposed Component Boundaries

Future Java skeleton work, if separately authorized, should remain read-only and split responsibilities into narrow components.

### Production Ownership Request

Design-only responsibility:

- carry explicit production owner evidence supplied by already-approved internal owners
- carry metadata for audit, authentication / visibility, and consumer isolation
- reject runtime substitutes as ownership evidence

Required evidence groups:

- `sourceTraceEntryOwnershipCompletionPath`
- `entryPriceSource`
- `entrySourceType`
- `entrySourceTimeframe`
- `entrySourceReason`
- `entrySourceRef`
- source window
- rule id / rule version
- freshness ownership
- conflict family ownership
- audit metadata
- authentication / visibility metadata
- consumer isolation metadata
- Risk Action Guard blocker metadata

### Production Ownership Reviewer

Design-only responsibility:

- validate singular allowed ownership for each field
- preserve missing fields, unsafe fields, and blocking fields
- downgrade or roll back to fail-closed review output
- produce no runtime completion signal

### Production Ownership Review Result

Design-only responsibility:

- carry fail-closed review output
- preserve `INCOMPLETE` / `NONE`
- preserve `REVIEW_ONLY`
- preserve `manualReviewRequired=true`
- preserve `notTradeInstruction=true`
- preserve `sourceTraceEntryCompleted=false`
- preserve `completionReady=false`
- preserve blocker evidence

### Production Ownership Audit Envelope

Design-only responsibility:

- capture field owner id
- capture source ref
- capture source window
- capture rule id / rule version
- capture observed time and decision-create time
- capture downgrade / rollback reason
- capture visibility policy result
- capture consumer isolation proof

### Production Ownership Consumer Isolation Envelope

Design-only responsibility:

- prove review output cannot feed BoundaryCandidateService `VALID`
- prove review output cannot feed ExecutionPlan readiness
- prove review output cannot mutate dashboard or schema
- prove review output cannot call order, execution, automation, scheduler, or external data paths

## Proposed Data Flow

Future production-facing read-only data flow should be:

1. Already-approved internal owner evidence is assembled outside the runtime completion path.
2. The future ownership request receives only explicit owner evidence and safety metadata.
3. The future ownership reviewer checks every ownership field for singular allowed ownership.
4. The reviewer rejects missing, duplicate, ambiguous, stale, forbidden, unaudited, unauthorized, non-isolated, or Risk Action Guard-blocked evidence.
5. The reviewer returns fail-closed review output with blockers and no runtime completion signal.
6. Existing read-only display/API/controller surfaces may consume only already-built fail-closed review output, not owner evidence directly.

Forbidden data flow:

- latest price into ownership fields
- raw kline items into ownership fields
- AI text into ownership fields
- dashboard text into ownership fields
- external data into ownership fields
- order data into ownership fields
- execution data into ownership fields
- display/API/controller copy back into ownership fields
- positive-looking fixture labels into completion or readiness

## Proposed Downgrade Design

Any ownership evidence must downgrade to fail-closed review output when:

- required owner evidence is missing
- owner evidence is duplicated
- owner evidence is ambiguous
- owner evidence is stale
- owner evidence is forbidden or substituted from runtime/display/external/order/execution sources
- audit metadata is missing or incomplete
- authentication / visibility is missing, unauthorized, or ambiguous
- consumer isolation proof is missing
- Risk Action Guard blocker evidence is present or incomplete
- source window is stale, future, inverted, or unsupported
- rule id / rule version is missing, stale, unsupported, or incompatible
- freshness is missing, stale, future, inverted, or unknown
- conflict family evidence is missing, null, true, stale, or unaudited

Downgraded output must preserve:

- `completionStatus=INCOMPLETE`
- `completionTransition=NONE`
- `reviewMode=REVIEW_ONLY`
- `manualReviewRequired=true`
- `notTradeInstruction=true`
- `sourceTraceEntryCompleted=false`
- `completionReady=false`
- blocker evidence

## Proposed Rollback Design

Rollback must apply when previously positive-looking ownership evidence later becomes unsafe.

Rollback triggers:

- owner evidence becomes stale
- rule version becomes incompatible
- source ref becomes duplicate, ambiguous, deleted, or unauditable
- audit envelope becomes incomplete
- authentication / visibility changes to unauthorized or unknown
- consumer isolation proof is lost
- Risk Action Guard evidence becomes blocking
- a forbidden runtime substitution is detected after initial review
- positive-looking labels are found to imply readiness, `VALID`, order, execution, automation, or external paths

Rollback output must:

- return to `INCOMPLETE` / `NONE`
- preserve `REVIEW_ONLY`
- preserve `manualReviewRequired=true`
- preserve `notTradeInstruction=true`
- keep `sourceTraceEntryCompleted=false`
- keep `completionReady=false`
- preserve missing, unsafe, and blocking evidence
- withhold payload when visibility is unsafe

## Proposed Auditability Requirements

Every ownership field must have audit evidence before any future Java skeleton can be considered.

Required audit fields:

- ownership field key
- owner family
- owner id
- source ref
- source window
- source timeframe
- rule id
- rule version
- freshness status
- observed time
- decision-create time
- conflict family evaluation source
- downgrade reason, when present
- rollback reason, when present
- authentication / visibility result
- consumer isolation proof

Audit failures must fail closed. Missing audit evidence is not neutral.

## Proposed Authentication / Visibility Requirements

Future production-facing read-only ownership output must define visibility before payload exposure.

Required design rules:

- unauthorized consumers receive fail-closed output or withheld payload
- ambiguous visibility withholds payload
- sensitive rule metadata is withheld unless visibility is explicit
- source refs are withheld unless visibility is explicit
- review-only labels must remain visible enough to explain blocking state
- no consumer may infer completion, readiness, or action from withheld payload

Authentication / visibility failures must preserve:

- `manualReviewRequired=true`
- `notTradeInstruction=true`
- `sourceTraceEntryCompleted=false`
- `completionReady=false`

## Proposed Consumer Isolation Requirements

Future production-facing ownership review output must be isolated from:

- BoundaryCandidateService `VALID`
- ExecutionPlan readiness
- dashboard mutation
- schema persistence
- resolver wiring
- validator readiness upgrades
- order paths
- execution paths
- close / reverse behavior
- automation paths
- scheduler creation
- external data paths
- Coinglass integration
- news or macro APIs
- auto-trading

Consumer isolation must be proven before any Java skeleton can be considered. Missing isolation proof fails closed.

## Required Tests Before Any Java Skeleton

Before any Java skeleton is authorized, add or confirm tests for:

- missing owner evidence fails closed for every ownership field
- duplicate owner evidence fails closed for every ownership field
- ambiguous owner evidence fails closed for every ownership field
- stale owner evidence fails closed for every ownership field
- latest-price-only substitution fails closed for every ownership field
- raw-kline-only substitution fails closed for every ownership field
- AI text substitution fails closed for every ownership field
- dashboard text substitution fails closed for every ownership field
- external data substitution fails closed for every ownership field
- order / execution data substitution fails closed for every ownership field
- missing audit metadata fails closed for every owner-present fixture
- missing authentication / visibility fails closed or withholds payload
- missing consumer isolation fails closed
- Risk Action Guard blockers remain review-only and block completion
- positive-looking labels do not imply completion, readiness, `VALID`, dashboard mutation, order, execution, automation, or external paths
- downgrade output preserves fail-closed safety flags
- rollback output preserves fail-closed safety flags and blocker evidence
- no fixture generates real entry / stop / TP / RR values
- no Spring service registration is introduced
- no production adapter implementation is present
- no production completion contract implementation is present

Recommended regression set:

```text
./mvnw -q -Dtest=EntryCompletionProductionOwnershipFixtureMatrixTest test
./mvnw -q -Dtest=SourceTraceEntryReadOnlyReviewControllerTest test
./mvnw -q -Dtest=SourceTraceEntryReadOnlyApiResponseMapperTest test
./mvnw -q -Dtest=SourceTraceEntryReadOnlyDisplayMapperTest test
./mvnw -q -Dtest=SourceTraceEntryReadOnlyIntegrationSeamTest test
./mvnw -q -Dtest=SourceTraceEntryReadOnlyCompletionAssemblerTest test
./mvnw -q -Dtest=SourceTraceEntryPositiveCompletionFixtureFactoryMapperTest test
./mvnw -q -Dtest=SourceTraceEntryPositiveCompletionContractDTOTest test
./mvnw -q -Dtest=EntryCompletionPositiveContractFixtureSkeletonTest test
./mvnw -q -Dtest=EntryCompletionOwnershipContractFixtureTest test
./mvnw -q -Dtest=EntryCompletionFixtureMatrixGuardTest test
./mvnw -q -Dtest=FailClosedSourceTraceEntryOwnershipValidatorTest test
./mvnw -q -DskipTests compile
./mvnw -q -DskipTests test-compile
```

## Java Implementation Decision

Decision: Java implementation may not start after P86.

P86 is a design pack only. It defines a proposed future production-facing read-only ownership boundary and required tests, but does not authorize Java skeletons, production wiring, Spring registration, schema/dashboard changes, runtime SourceTrace completion, readiness upgrades, external integrations, order APIs, or auto-trading.

## Recommended Next Phase

Recommended next phase: fixture-only test design or documentation-only Java skeleton authorization gate for the production-facing read-only ownership boundary.

The next phase may decide whether a minimal Java skeleton can begin later. It must not implement that skeleton unless separately authorized.

## Risk Action Guard Reminders

- High risk does not directly mean close, reverse, or open.
- Wick / pin-bar evidence does not confirm trend reversal.
- Liquidity stress / stampede must block opportunity push and require review.
- Missing event data is not no event risk.
- Multi-timeframe agreement alone does not complete SourceTrace.

## Boundary Confirmations

- P86 is documentation-only.
- P86 does not modify Java.
- P86 does not modify tests.
- P86 does not add controller/endpoint Java.
- P86 does not modify `dashboard.html`.
- P86 does not modify schema.
- P86 does not modify config.
- P86 does not register display DTO/mapper, API DTO/mapper, seam, assembler, fixture helper, or production completion helper as Spring services.
- P86 does not wire endpoint/API mapper into resolver, validation, readiness, dashboard, schema, order, automation, or external data paths.
- P86 does not implement production completion.
- P86 does not add production adapter.
- P86 does not add `DefaultSourceTraceEntryOwnershipAdapter`.
- P86 does not add production `DefaultSourceTraceEntryCompletionContract`.
- P86 does not populate real SourceTrace fields in runtime.
- P86 does not complete full SourceTrace in runtime.
- P86 does not wire BoundaryCandidateService `VALID`.
- P86 does not upgrade ExecutionPlan readiness.
- P86 does not add external data integration, order API, or auto-trading.
- P86 does not generate real entry / stop / TP / RR values.
- Placeholder `docs/P86.md` is removed.
