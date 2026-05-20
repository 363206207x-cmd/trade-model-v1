# BACKEND-P87 Entry Completion Production Ownership Review Boundary Java Skeleton Authorization Gate

## Baseline

- Branch context: PR #280 / Issue #279.
- Baseline commit: `45c0198` (`docs: design entry completion wiring`).
- Scope: documentation-only Java skeleton authorization gate for the future production-facing read-only ownership boundary.
- P87 removes placeholder `docs/P87.md`.

## P86 Design Recap

BACKEND-P86 created a documentation-only production wiring design pack for Entry Completion ownership.

P86 proposed the smallest future production-facing boundary as a read-only ownership evidence boundary that accepts explicit production owner evidence and returns fail-closed review output.

P86 proposed design-only boundary name:

- `SourceTraceEntryProductionOwnershipReviewBoundary`

P86 design intent:

- collect already-owned production evidence for Entry Completion ownership fields
- validate that each ownership field has exactly one allowed owner
- reject missing, duplicate, ambiguous, stale, forbidden, unaudited, unauthorized, or non-isolated evidence
- preserve review-only and non-instructional output
- expose blockers for human review without runtime SourceTrace completion
- never generate real entry / stop / TP / RR values
- never feed readiness, BoundaryCandidateService `VALID`, dashboard mutation, schema persistence, order, execution, automation, or external data paths

P86 explicitly decided that Java implementation may not start after P86. It recommended a Java skeleton authorization gate before any minimal skeleton work.

## Proposed Future Java Skeleton File Scope

If a future phase is separately authorized, the Java skeleton scope should be limited to inert read-only DTO and interface surfaces.

Proposed future files, subject to existing package conventions:

- `src/main/java/org/example/trademodel/service/SourceTraceEntryProductionOwnershipReviewBoundary.java`
- `src/main/java/org/example/trademodel/service/dto/SourceTraceEntryProductionOwnershipReviewRequest.java`
- `src/main/java/org/example/trademodel/service/dto/SourceTraceEntryProductionOwnershipReviewResult.java`
- `src/main/java/org/example/trademodel/service/dto/SourceTraceEntryProductionOwnershipAuditEnvelope.java`
- `src/main/java/org/example/trademodel/service/dto/SourceTraceEntryProductionOwnershipConsumerIsolationEnvelope.java`
- `src/test/java/org/example/trademodel/service/SourceTraceEntryProductionOwnershipReviewBoundaryTest.java`

The exact package may be adjusted to match current repository conventions, but the scope must remain DTO/interface skeleton only.

## Explicitly Allowed Future Skeleton Scope

A future minimal Java skeleton phase may add:

- one read-only boundary interface
- inert request DTO carrying explicit owner evidence fields
- inert result DTO preserving fail-closed review output
- inert audit envelope DTO
- inert consumer isolation envelope DTO
- enum names only if needed for fail-closed review status, missing reason, downgrade reason, rollback reason, or visibility state
- focused tests proving default fail-closed behavior, no Spring registration, no forbidden method names, no generated entry / stop / TP / RR values, and no production wiring

Allowed future skeleton fields:

- ownership field keys
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
- conflict family evidence status
- audit metadata presence
- authentication / visibility state
- consumer isolation proof state
- downgrade reason
- rollback reason
- missing fields
- unsafe fields
- blocking fields
- review mode
- `manualReviewRequired`
- `notTradeInstruction`
- `sourceTraceEntryCompleted`
- `completionReady`

Allowed future skeleton defaults:

- `completionStatus=INCOMPLETE`
- `completionTransition=NONE`
- `reviewMode=REVIEW_ONLY`
- `manualReviewRequired=true`
- `notTradeInstruction=true`
- `sourceTraceEntryCompleted=false`
- `completionReady=false`

## Explicitly Forbidden Future Skeleton Scope

A future Java skeleton must not add:

- production completion implementation
- production adapter implementation
- `DefaultSourceTraceEntryOwnershipAdapter`
- production `DefaultSourceTraceEntryCompletionContract`
- Spring `@Service`, `@Component`, `@Repository`, `@Controller`, or `@RestController` registration
- controller or endpoint Java
- dashboard wiring
- schema changes
- config changes
- resolver wiring
- validation readiness upgrades
- BoundaryCandidateService `VALID` wiring
- ExecutionPlan readiness changes
- order APIs
- execution APIs
- close, reverse, open, buy, sell, signal, trade-ready, or auto-trading method surfaces
- scheduler or automation wiring
- external data, Coinglass, news API, or macro API integrations
- runtime SourceTrace field population
- full SourceTrace completion
- generated real entry / stop / TP / RR values

Forbidden input substitutions remain:

- latest price only
- raw kline only
- AI text
- dashboard text
- external data
- order data
- execution data
- display/API/controller copy
- positive-looking labels or names

## Required Tests Before Skeleton Implementation

Before any Java skeleton phase is accepted, that phase must add or preserve tests proving:

- default DTO state remains fail-closed
- null request fails closed
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
- missing audit metadata fails closed
- missing authentication / visibility fails closed or withholds payload
- missing consumer isolation fails closed
- Risk Action Guard blockers remain review-only and block completion
- positive-looking labels do not imply completion, readiness, BoundaryCandidateService `VALID`, dashboard mutation, order, execution, automation, or external paths
- downgrade output preserves fail-closed safety flags
- rollback output preserves fail-closed safety flags and blocker evidence
- no fixture or skeleton generates real entry / stop / TP / RR values
- no Spring service registration is introduced
- no production adapter implementation is present
- no production completion contract implementation is present
- boundary/interface method names expose no order, execution, close, reverse, auto-trading, trade-ready, ready-to-trade, valid, completed, signal, buy, sell, or open surface

Recommended regression set before and after any future skeleton:

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

## Required Invariants For Any Future Skeleton

Any future Java skeleton must preserve:

- read-only behavior
- fail-closed defaults
- review-only output
- non-instructional output
- `manualReviewRequired=true`
- `notTradeInstruction=true`
- `sourceTraceEntryCompleted=false`
- `completionReady=false`
- no BoundaryCandidateService `VALID`
- no ExecutionPlan readiness
- no dashboard mutation
- no schema persistence
- no order behavior
- no execution behavior
- no automation behavior
- no external data calls
- no real entry / stop / TP / RR values
- no Spring service registration
- no production adapter implementation
- no production completion contract implementation

## Production Wiring Decision

Production wiring implementation remains blocked after P87.

P87 does not authorize runtime SourceTrace completion, readiness wiring, dashboard/schema mutation, resolver wiring, validation readiness upgrades, external integrations, order APIs, execution APIs, automation, or auto-trading.

## Java Skeleton Authorization Decision

Decision: a future minimal Java skeleton may be authorized next, but only under the explicitly allowed skeleton scope in this document.

Authorized next skeleton characteristics:

- DTO/interface skeleton only
- read-only
- inert
- fail-closed by default
- no Spring registration
- no production wiring
- no production completion
- no production adapter
- no controller or endpoint
- focused tests required in the same future phase

Any implementation that goes beyond DTO/interface shape, fail-closed defaults, and tests remains blocked.

## Recommended Next Phase

Recommended next phase: BACKEND-P88 Entry Completion Production Ownership Review Boundary DTO / Interface Skeleton.

That future phase may add the minimal DTO/interface skeleton and focused tests described above. It must not add production wiring, Spring registration, production completion, production adapter implementation, controller/endpoint Java, schema/dashboard changes, external integrations, order APIs, execution APIs, automation, auto-trading, or generated real entry / stop / TP / RR values.

## Risk Action Guard Reminders

- High risk does not directly mean close, reverse, or open.
- Wick / pin-bar evidence does not confirm trend reversal.
- Liquidity stress / stampede must block opportunity push and require review.
- Missing event data is not no event risk.
- Multi-timeframe agreement alone does not complete SourceTrace.

## Boundary Confirmations

- P87 is documentation-only.
- P87 does not modify Java.
- P87 does not modify tests.
- P87 does not add controller/endpoint Java.
- P87 does not modify `dashboard.html`.
- P87 does not modify schema.
- P87 does not modify config.
- P87 does not register display DTO/mapper, API DTO/mapper, seam, assembler, fixture helper, or production completion helper as Spring services.
- P87 does not wire endpoint/API mapper into resolver, validation, readiness, dashboard, schema, order, automation, or external data paths.
- P87 does not implement production completion.
- P87 does not add production adapter.
- P87 does not add `DefaultSourceTraceEntryOwnershipAdapter`.
- P87 does not add production `DefaultSourceTraceEntryCompletionContract`.
- P87 does not populate real SourceTrace fields in runtime.
- P87 does not complete full SourceTrace in runtime.
- P87 does not wire BoundaryCandidateService `VALID`.
- P87 does not upgrade ExecutionPlan readiness.
- P87 does not add external data integration, order API, or auto-trading.
- P87 does not generate real entry / stop / TP / RR values.
- Placeholder `docs/P87.md` is removed.
