# BACKEND-P81 Entry Completion Production Ownership Contract Design Pack

## Baseline

- Branch context: PR #266 / Issue #263.
- Duplicate P81 issues ignored: #264 and #265.
- Baseline commit: `d1d036c` (`docs: review entry completion wiring blockers`).
- Scope: documentation-only production ownership contract design pack for Entry Completion after BACKEND-P80.
- P81 does not modify Java, tests, schema, `dashboard.html`, config, or production wiring.
- P81 does not authorize production wiring.
- P81 removes placeholder `docs/P81.md`.

## P80 Blocker Recap

BACKEND-P80 confirmed that production wiring may not start after the P79 MVP read-only closure.

The blocking gaps are:

- production owners are missing for SourceTrace entry completion fields
- runtime data substitution risks remain unresolved
- endpoint and consumer isolation are not proven
- authentication and visibility contracts are not defined
- auditability is not defined for every production-owned field
- downgrade and rollback rules are not complete
- Risk Action Guard evidence remains review-only and cannot become an action shortcut
- required production owner, substitution, isolation, authentication, audit, serialization, and rollback tests do not exist yet

P81 responds by defining proposed ownership contracts. These contracts are design-only and must be validated in a later phase before any production wiring proposal.

## Proposed Ownership Contracts

### `sourceTraceEntryOwnershipCompletionPath`

Proposed owner: future SourceTrace entry completion ownership evaluator.

Allowed sources:

- rule-owned completion evidence produced from explicit SourceTrace ownership inputs
- validation-completion context that is already fail-closed
- read-only assembler/seam outputs only as blocker evidence, not as completion evidence

Forbidden sources:

- latest price
- raw kline data
- AI text
- dashboard text
- external feeds
- order or execution data
- display/API/controller output

Contract:

- must be explicitly present, singular, and owned
- must identify the rule-owned completion path without implying trade readiness
- must fail closed when missing, duplicated, contradictory, stale, ambiguous, unsafe, or unwired
- must not be inferred from review output or endpoint output
- must be auditable back to source refs, source window, rule id, rule version, freshness, and conflict evidence

### `entryPriceSource`

Proposed owner: future rule-owned entry source evaluator.

Allowed sources:

- synthetic fixture evidence in tests
- future rule-owned source metadata that explains the origin of an entry candidate without generating executable price output

Forbidden sources:

- latest price alone
- raw kline alone
- AI-generated text
- dashboard labels
- external feed values
- order price
- execution price

Contract:

- must identify source ownership, not calculate or emit a real entry value
- must not become `entryReady`, `tradeReady`, `executionReady`, signal, advice, or order intent
- must fail closed when absent, blank, unsupported, runtime-like, production-like, or trade-instruction-like
- must preserve `manualReviewRequired=true` and `notTradeInstruction=true`

### `entrySourceType`

Proposed owner: future source-family classifier for rule-owned entry evidence.

Allowed sources:

- allowlisted rule-owned source family names
- fixture-only synthetic values in tests

Forbidden sources:

- free-form AI text
- dashboard text
- external data labels without ownership
- order/execution state
- readiness, signal, valid, completed, buy, sell, open, close, reverse, execute, or auto-trade wording

Contract:

- must come from an explicit allowlist
- blank, unknown, unsupported, duplicated, mixed, or ambiguous source types fail closed
- safe-looking source type alone does not complete SourceTrace
- source type must be linked to source ref, source window, rule id/version, freshness, and conflict evidence

### `entrySourceTimeframe`

Proposed owner: future rule-owned timeframe mapper.

Allowed sources:

- explicit decision timeframe from rule-owned context
- explicit source timeframe from source-owned evidence

Forbidden sources:

- dashboard timeframe text
- free-form AI timeframe text
- inferred timeframe from latest price refresh cadence
- inferred timeframe from raw kline availability alone
- order/execution timeframe metadata

Contract:

- must match the decision context or be explicitly mapped
- blank, unknown, unsupported, mismatched, duplicated, or ambiguous timeframes fail closed
- multi-timeframe agreement alone does not complete SourceTrace
- timeframe evidence must remain review-only and non-instructional

### `entrySourceReason`

Proposed owner: future rule-owned reason taxonomy.

Allowed sources:

- controlled reason codes or controlled review-only reason labels
- fixture-only synthetic reason labels in tests

Forbidden sources:

- free-form AI recommendation text
- dashboard explanatory text
- external news/macro text
- order/execution notes
- buy, sell, open, close, reverse, signal, valid, completed, ready, order, execute, or auto-trade language

Contract:

- must use controlled non-instructional language
- must explain source ownership, not trade action
- missing, blank, unsupported, free-form, mixed, or instruction-like reason text fails closed
- reason text must not be shown as advice or readiness

### `entrySourceRef`

Proposed owner: future source provenance registry.

Allowed sources:

- singular rule-owned source reference
- source ref generated by a production-owned evidence registry
- fixture-only source refs in tests

Forbidden sources:

- dashboard element ids
- AI text fragments
- external feed ids without SourceTrace ownership
- order ids
- execution ids
- constructed refs from latest price or kline items alone

Contract:

- must be present and singular
- duplicate, missing, blank, unsupported, stale, or ambiguous refs fail closed
- source ref must link to source window, rule id/version, freshness, and conflict evidence
- source ref must be auditable without becoming an executable instruction

### Source Window

Proposed owner: future source window resolver.

Allowed sources:

- explicit window start/end from rule-owned source evidence
- fixture-only windows in tests

Forbidden sources:

- inferred windows from latest price timestamp alone
- inferred windows from raw kline presence alone
- dashboard visible range
- AI narrative time range
- order/execution timestamps

Contract:

- must include start, end, timeframe, and relationship to decision create time
- missing, empty, stale, future, inverted, overlapping-ambiguous, or unsupported windows fail closed
- source window must not imply freshness without freshness owner confirmation
- source window must not imply entry readiness

### Rule ID / Rule Version

Proposed owner: future immutable rule metadata registry.

Allowed sources:

- rule id from a production-owned rule registry
- rule version from a production-owned rule registry
- fixture-only rule metadata in tests

Forbidden sources:

- AI text
- dashboard text
- branch names
- PR numbers
- external feed identifiers
- order/execution identifiers

Contract:

- rule id and rule version must both be present
- missing, blank, unknown, unsupported, stale, incompatible, or mismatched rule metadata fails closed
- rule version must be linked to source type, source timeframe, source ref, source window, freshness, and conflicts
- rule metadata must be auditable and immutable for the decision

### Freshness Ownership

Proposed owner: future freshness evaluator.

Allowed sources:

- production-owned freshness status
- production-owned `observedAtMs`
- production-owned `decisionCreateTimeMs`
- fixture-only freshness evidence in tests

Forbidden sources:

- latest price recency alone
- raw kline timestamp alone
- external feed timestamp alone
- dashboard refresh time
- AI response time
- order/execution timestamps

Contract:

- freshness status, observed time, and decision-create time must all be present
- stale, unknown, future observed time, observed-after-decision, missing timestamp, and clock inversion fail closed
- freshness cannot substitute for source ownership
- freshness cannot make completion ready without complete ownership and conflict evidence

### Conflict Family Ownership

Proposed owner: future conflict evidence evaluators for each source family.

Required conflict families:

- stop
- take profit
- risk/reward
- liquidity
- multi-timeframe
- event
- wick

Allowed sources:

- explicit rule-owned conflict evidence per family
- nullable Boolean conflict flags where null means missing or unevaluated
- fixture-only conflict evidence in tests

Forbidden sources:

- price movement alone
- raw kline shape alone
- external liquidity labels without ownership
- external event labels without ownership
- dashboard summaries
- AI interpretation
- order/execution outcomes

Contract:

- every conflict family must be independently evaluated
- null conflict flags fail closed
- true conflict flags fail closed
- false conflict flags do not substitute for missing completion path or source ownership
- liquidity stress / stampede blocks opportunity push and requires review
- missing event data is not no event risk
- multi-timeframe agreement alone does not complete SourceTrace
- wick / pin-bar evidence alone does not confirm trend reversal or completion

## Allowed Source Families

Allowed source families are limited to future production-owned rule evidence and test-only fixtures:

- rule-owned completion path evidence
- rule-owned entry source evidence
- rule-owned source family classification
- production-owned source ref provenance
- production-owned source window evidence
- production-owned immutable rule id/version metadata
- production-owned freshness evaluation
- production-owned conflict family evaluation
- fixture-only synthetic evidence used by tests and never by runtime wiring

Allowed source families must remain review-only until a separately authorized future phase proves production ownership, auditability, consumer isolation, downgrade rules, and tests.

## Forbidden Substitution Sources

The following sources must never substitute for production SourceTrace entry completion ownership:

- latest price only
- raw kline only
- AI text
- dashboard text
- external data without SourceTrace ownership
- Coinglass data
- news API data
- macro calendar API data
- exchange feed recency alone
- order data
- execution data
- endpoint output
- API DTO output
- display DTO output
- read-only seam output
- read-only assembler output
- review labels
- helper copy
- route names
- status names
- positive-looking fixture metadata

If any forbidden source appears in production-looking evidence, the output must downgrade to fail-closed review-only state and record blocker evidence.

## Consumer Isolation Contract

Proposed owner: future read-only consumer boundary contract.

Required isolation rules:

- review endpoint output cannot feed BoundaryCandidateService `VALID`
- review endpoint output cannot feed ExecutionPlan readiness
- review endpoint output cannot feed order, execution, close, reverse, scheduler, automation, or external API paths
- display/API labels cannot be parsed as signal, advice, approval, completion, readiness, or trade instruction
- missing, unsafe, and blocking evidence must remain visible to consumers
- consumers cannot hide, drop, overwrite, or reinterpret blocker evidence as approval
- route names, DTO fields, method names, and serialized values must remain review-only and non-instructional

Contract status: proposed only. Production wiring remains blocked until this isolation contract has tests and enforcement design.

## Authentication / Visibility Contract Proposal

Proposed owner: future endpoint access policy owner.

Required decisions:

- who may view read-only completion review output
- whether output is internal-only, operator-only, or broader human-review output
- whether symbol and timeframe metadata are sensitive
- whether missing, unsafe, and blocking field names reveal internal rule structure
- how access decisions are audited
- how unauthorized access fails closed

Proposed default:

- deny by default
- expose only to authorized human review contexts
- no automated clients may use the output as completion, readiness, signal, advice, or instruction
- unauthorized access returns fail-closed or no review payload, depending on future security design

Contract status: proposed only. No authentication or visibility wiring is authorized in P81.

## Auditability Contract Proposal

Proposed owner: future SourceTrace audit owner.

Every production-owned field must record:

- field name
- field owner
- source family
- source ref
- source window
- rule id
- rule version
- freshness status
- observed time
- decision-create time
- conflict family decision when applicable
- downgrade reason when applicable
- missing fields
- unsafe fields
- blocking fields
- consumer path
- review-only copy
- non-instructional flag state
- audit timestamp

Audit output must not become trade advice, readiness, execution instruction, or order intent. Missing audit metadata must fail closed.

Contract status: proposed only. No audit persistence or schema change is authorized in P81.

## Downgrade / Rollback Contract Proposal

Proposed owner: future completion downgrade policy owner.

Required downgrade rules:

- missing completion path downgrades to `INCOMPLETE` / `NONE`
- missing source type, timeframe, reason, or ref downgrades to `INCOMPLETE` / `NONE`
- missing source window, rule id, or rule version downgrades to `INCOMPLETE` / `NONE`
- stale, unknown, future, or clock-inverted freshness downgrades to `INCOMPLETE` / `NONE`
- null conflict flags downgrade to `INCOMPLETE` / `NONE`
- true conflict flags downgrade to `INCOMPLETE` / `NONE`
- liquidity stress or stampede downgrades to fail-closed review output
- missing event data downgrades to fail-closed review output
- multi-timeframe agreement without ownership downgrades to fail-closed review output
- wick / pin-bar-only evidence downgrades to fail-closed review output
- forbidden substitution source evidence downgrades to fail-closed review output
- consumer isolation, authentication, visibility, audit, serialization, or endpoint safety failures downgrade to fail-closed review output

Required rollback rules:

- every positive-looking production state must roll back to `INCOMPLETE` / `NONE`
- rollback must preserve blocker evidence
- rollback must preserve `manualReviewRequired=true`
- rollback must preserve `notTradeInstruction=true`
- rollback must not create real entry / stop / TP / RR values
- rollback must not emit trade advice, readiness, or order instructions
- rollback must keep runtime completion and readiness blocked unless a separately authorized future phase explicitly changes those invariants

Contract status: proposed only. No downgrade implementation or production wiring is authorized in P81.

## Risk Action Guard Ownership Contract

Proposed owner: future Risk Action Guard evidence policy owner.

Required contracts:

- High risk does not directly mean close, reverse, or open.
- Wick / pin-bar evidence does not confirm trend reversal.
- Liquidity stress / stampede must block opportunity push and require review.
- Missing event data is not no event risk.
- Multi-timeframe agreement alone does not complete SourceTrace.

Each guard requires:

- production-owned evidence source
- downgrade rule
- audit entry
- review-only helper copy
- tests proving it cannot become trade action, signal, advice, readiness, completion, or order intent

Contract status: proposed only. Risk Action Guard evidence remains blocker/review evidence until separately authorized implementation work exists.

## Required Tests Before Any Production Wiring Proposal

Before any production wiring proposal, run the existing read-only safety regression set:

```text
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

Additional required tests before any production wiring proposal:

- completion path owner present/missing/duplicate/ambiguous/unsafe tests
- entry price source owner tests that reject latest-price-only and raw-kline-only substitution
- entry source type allowlist and forbidden wording tests
- entry source timeframe match/mismatch/unsupported tests
- entry source reason controlled taxonomy and trade-instruction wording rejection tests
- entry source ref singularity, provenance, duplicate, stale, and ambiguity tests
- source window missing/stale/future/inverted/ambiguous tests
- rule id/version missing/unsupported/mismatch/audit tests
- freshness status, observed time, decision-create time, stale, future, and clock inversion tests
- conflict family null/true/false semantics for stop, take profit, risk/reward, liquidity, multi-timeframe, event, and wick
- forbidden substitution tests for latest price, raw kline, AI text, dashboard text, external data, Coinglass, news API, macro API, order data, and execution data
- consumer isolation tests proving review output cannot feed `VALID`, readiness, dashboard mutation, order, execution, automation, or external paths
- authentication and visibility tests
- auditability tests for every production-owned field
- downgrade and rollback tests from every positive-looking state
- serialization tests proving no route, label, field, method, or value implies signal, advice, validity, completion, readiness, or trade instruction

## Explicit Production Wiring Decision

Decision: production wiring may not start after P81.

P81 defines proposed ownership contracts only. The contracts have not been implemented, tested as production owners, audited, isolated from consumers, secured by authentication/visibility, or wired into rollback/downgrade enforcement. Production SourceTrace entry completion remains blocked.

## Recommended Next Phase

Recommended next phase: documentation-only production ownership contract fixture matrix design.

That phase may define deterministic fixture cases for the proposed P81 ownership contracts, including owner-present, owner-missing, forbidden substitution, downgrade, rollback, audit, consumer isolation, authentication, visibility, and Risk Action Guard scenarios.

The next phase must not implement production wiring, add Java production adapters, register services, complete SourceTrace, wire BoundaryCandidateService `VALID`, upgrade ExecutionPlan readiness, modify schema/dashboard/config, add external integrations, add order APIs, add auto-trading, or generate real entry / stop / TP / RR values.

## Still-Blocked Paths

These remain blocked after P81:

- Java changes in P81
- test changes in P81
- controller/endpoint Java changes in P81
- `dashboard.html` changes
- schema changes
- config changes
- Spring service registration for display DTO/mapper, API DTO/mapper, seam, assembler, fixture helper, or production completion helper
- endpoint/API mapper wiring into resolver, validation, readiness, dashboard, schema, order, automation, or external data paths
- production SourceTrace completion implementation
- production adapter implementation
- `DefaultSourceTraceEntryOwnershipAdapter`
- production `DefaultSourceTraceEntryCompletionContract`
- runtime SourceTrace field population
- full SourceTrace completion
- BoundaryCandidateService `VALID`
- ExecutionPlan readiness
- external data integration
- Coinglass integration
- news or macro API integration
- order API
- auto-trading
- generated real entry / stop / TP / RR values

## Boundary Confirmations

- P81 is documentation-only.
- P81 does not modify Java.
- P81 does not modify tests.
- P81 does not add controller/endpoint Java.
- P81 does not modify `dashboard.html`.
- P81 does not modify schema.
- P81 does not modify config.
- P81 does not register display DTO/mapper, API DTO/mapper, seam, assembler, fixture helper, or production completion helper as Spring services.
- P81 does not wire endpoint/API mapper into resolver, validation, readiness, dashboard, schema, order, automation, or external data paths.
- P81 does not implement production completion.
- P81 does not add production adapter.
- P81 does not add `DefaultSourceTraceEntryOwnershipAdapter`.
- P81 does not add production `DefaultSourceTraceEntryCompletionContract`.
- P81 does not populate real SourceTrace fields in runtime.
- P81 does not complete full SourceTrace in runtime.
- P81 does not wire BoundaryCandidateService `VALID`.
- P81 does not upgrade ExecutionPlan readiness.
- P81 does not add external data integration, order API, or auto-trading.
- P81 does not generate real entry / stop / TP / RR values.
- Placeholder `docs/P81.md` is removed.
