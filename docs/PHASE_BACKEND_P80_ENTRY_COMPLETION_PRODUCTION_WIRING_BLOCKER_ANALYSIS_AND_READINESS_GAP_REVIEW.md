# BACKEND-P80 Entry Completion Production Wiring Blocker Analysis and Readiness Gap Review

## Baseline

- Issue context: #261.
- Branch: `w`.
- Baseline commit: `49ebfd9` (`docs: close entry completion chain`).
- Scope: documentation-only production wiring blocker analysis and readiness gap review after BACKEND-P79.
- P80 does not modify Java, tests, schema, `dashboard.html`, config, or production wiring.
- P80 does not start production wiring.

## P79 Closure Recap

BACKEND-P79 closed the P34-P78 Entry Completion MVP read-only safety chain as a fail-closed, review-only, non-instructional chain.

The closed MVP read-only chain includes:

- SourceTrace entry completion contract/result skeletons
- fail-closed completion resolver skeleton
- validation-completion context assembler
- pre-wiring readiness checklist and ownership fixture matrix
- positive completion contract design and DTO skeleton
- test-scope fixture factory/mapper helpers and extension guards
- read-only assembler
- read-only integration seam
- display DTO/mapper
- API DTO/mapper
- inert read-only review controller endpoint

P79 explicitly closed the MVP read-only chain only. It did not complete runtime SourceTrace, did not make completion ready, did not wire BoundaryCandidateService `VALID`, did not upgrade ExecutionPlan readiness, did not add schema/dashboard persistence, and did not authorize production completion.

## Why Production Wiring Is Still Blocked

Production wiring remains blocked because the chain has only proven safe fail-closed representation, not production-owned SourceTrace completion.

The current chain can show why entry completion remains incomplete. It cannot prove that a runtime entry is SourceTrace-owned, fresh, conflict-free, non-instructional, and safe to expose through production paths. Production wiring would require trusted owners for every completion field, downgrade rule, consumer boundary, audit trail, and failure mode before any runtime path may produce completed SourceTrace entry state.

The most important blocker is conceptual: the read-only chain is designed to avoid readiness. Wiring it into production before ownership and isolation are complete would convert a safety display surface into a decision surface. That is explicitly out of scope.

## Production Owner Gaps

### `sourceTraceEntryOwnershipCompletionPath`

Gap: no production owner is defined for the completion path.

Required before wiring:

- a named production owner for the path source
- explicit rules for when the path is present, absent, stale, ambiguous, or unsafe
- proof the path cannot be inferred from display output, API output, latest price, kline data, AI text, dashboard text, external data, order data, or execution data
- downgrade rules that force fail-closed output when the path is missing, duplicated, contradictory, or unwired

### `entryPriceSource`

Gap: no production owner is defined for entry price source.

Required before wiring:

- an ownership source that explains where the entry price source came from without generating a real entry price in the read-only chain
- a clear distinction between source ownership and price calculation
- explicit rejection of latest-price-only and raw-kline-only substitution
- proof the field cannot become trade advice, entry readiness, or execution readiness

### `entrySourceType`

Gap: no production owner is defined for allowed entry source types.

Required before wiring:

- an allowlist of production source types
- rejection rules for blank, unknown, unsupported, runtime-like, production-like, trade-ready-looking, signal-looking, advice-looking, or execution-looking values
- downgrade behavior for mixed allowed and disallowed source evidence

### `entrySourceTimeframe`

Gap: no production owner is defined for entry source timeframe.

Required before wiring:

- exact allowed timeframe set
- mapping between runtime decision timeframe and source timeframe
- mismatch handling
- explicit rule that multi-timeframe agreement alone does not complete SourceTrace

### `entrySourceReason`

Gap: no production owner is defined for entry source reason.

Required before wiring:

- reason taxonomy that is review-only and non-instructional
- rejection of reason text that looks like buy, sell, open, close, reverse, signal, valid, completed, ready, order, execute, or auto-trade language
- downgrade behavior for missing, blank, unsupported, mixed, or trade-instruction-like reason text

### `entrySourceRef`

Gap: no production owner is defined for singular source ref ownership.

Required before wiring:

- source ref provenance contract
- singularity rule
- duplicate and ambiguous ref handling
- audit trail for source ref creation and validation
- proof refs cannot be constructed from dashboard text, AI text, external data, order data, or execution data

### Source Window

Gap: no production owner is defined for source window.

Required before wiring:

- window start and end ownership
- relationship between source window, observed time, decision create time, and timeframe
- stale, future, inverted, empty, and ambiguous window downgrade rules
- fixture and production tests proving windows cannot be inferred from latest price or raw kline presence alone

### Rule ID / Rule Version

Gap: no production owner is defined for rule id or rule version.

Required before wiring:

- immutable rule id and version provenance
- rule version compatibility checks
- handling for missing, blank, unsupported, stale, or mismatched rule metadata
- audit trail linking rule metadata to source refs and source window

### Freshness Ownership

Gap: freshness status and timestamps are not production-owned.

Required before wiring:

- production owner for freshness status
- production owner for `observedAtMs`
- production owner for `decisionCreateTimeMs`
- stale, unknown, future observed time, observed-after-decision, missing timestamp, and clock inversion downgrade rules
- tests proving freshness cannot be substituted by current latest price, kline item presence, or external feed recency

### Conflict Family Ownership

Gap: stop, take-profit, risk/reward, liquidity, multi-timeframe, event, and wick conflict families do not have production-owned completion evidence.

Required before wiring:

- explicit owner for each conflict family
- nullable Boolean semantics preserved: null means missing or unevaluated and must fail closed
- true conflict flags fail closed
- false conflict flags do not substitute for missing completion evidence
- liquidity stress and stampede must block opportunity push and require review
- missing event data must fail closed and is not no event risk
- multi-timeframe agreement alone must not complete SourceTrace
- wick / pin-bar evidence alone must not confirm trend reversal or completion

## Runtime Data Substitution Risks

### Latest Price Only

Risk: latest price can look precise but does not prove SourceTrace ownership.

Blocker: production wiring must prove latest price cannot populate or imply completion path, entry source fields, freshness, conflict state, readiness, or trade instruction.

### Raw Kline Only

Risk: raw kline data can look like source evidence but does not prove rule ownership.

Blocker: production wiring must prove kline item presence cannot substitute for source ref provenance, source window, rule id/version, freshness, conflict evidence, or completion readiness.

### AI Text

Risk: AI narrative can describe an entry-like idea without owning SourceTrace.

Blocker: AI text must not populate completion fields, source refs, source reasons, downgrade reasons, display labels, API labels, readiness flags, or order/execution intent.

### Dashboard Text

Risk: dashboard text can be copied into completion-like fields and appear user-facing.

Blocker: dashboard text must not become source ownership, source reason, source ref, completion status, readiness, or trade instruction.

### External Data

Risk: external data can be fresh or persuasive but still lacks SourceTrace ownership.

Blocker: Coinglass, news, macro calendar, exchange, or other external feeds must remain blocked from completion wiring until each feed has explicit ownership, freshness, conflict, and downgrade contracts.

### Order / Execution Data

Risk: order or execution data can create a false feedback loop where executed state validates source ownership.

Blocker: order and execution data must never populate SourceTrace completion, entry source ownership, readiness, display/API labels, or controller output. Completion must not depend on whether an order exists, filled, failed, closed, reversed, or executed.

## Endpoint / Consumer Isolation Blockers

Production wiring cannot start until endpoint and consumer isolation is proven.

Required isolation:

- read-only endpoint output cannot be consumed by readiness paths
- read-only endpoint output cannot be consumed by BoundaryCandidateService `VALID`
- read-only endpoint output cannot be consumed by ExecutionPlan readiness
- read-only endpoint output cannot be consumed by order, execution, close, reverse, scheduler, automation, or external API clients
- display/API labels cannot be parsed as signals, advice, approval, or completion
- missing, unsafe, and blocking evidence cannot be hidden or dropped by consumers
- route names, DTO fields, method names, and serialized values remain review-only and non-instructional

Blocker: no production consumer contract exists yet that proves these guarantees.

## Authentication / Visibility Blockers

Production wiring cannot start until authentication and visibility are explicitly defined.

Required decisions:

- who may view read-only completion review output
- whether endpoint output is internal-only, operator-only, or broader human-review output
- whether symbol/timeframe metadata is sensitive
- whether missing and unsafe fields reveal internal rule structure
- how access decisions are audited
- how unauthorized access fails closed

Blocker: no authentication and visibility contract exists for production endpoint consumers.

## Auditability Blockers

Production wiring cannot start until every production-owned field is auditable.

Required auditability:

- field owner
- source timestamp
- source ref
- source window
- rule id
- rule version
- freshness decision
- conflict family decisions
- downgrade reason
- missing fields
- unsafe fields
- blocking fields
- consumer path
- non-instructional review copy

Blocker: no production audit contract exists that proves every field can be traced without creating trade advice or executable instruction.

## Downgrade / Rollback Rules Required Before Wiring

Production wiring requires deterministic downgrade and rollback rules before any implementation.

Required downgrade rules:

- missing completion path downgrades to fail-closed
- missing source type, timeframe, reason, or ref downgrades to fail-closed
- missing source window, rule id, or rule version downgrades to fail-closed
- stale, unknown, future, or clock-inverted freshness downgrades to fail-closed
- null conflict flags downgrade to fail-closed
- true conflict flags downgrade to fail-closed
- liquidity stress or stampede downgrades to fail-closed and requires review
- missing event data downgrades to fail-closed
- multi-timeframe agreement without ownership downgrades to fail-closed
- wick / pin-bar-only evidence downgrades to fail-closed
- runtime-like, production-like, trade-ready-looking, signal-looking, or advice-looking text downgrades to fail-closed
- any consumer, authentication, visibility, audit, serialization, or endpoint isolation failure downgrades to fail-closed

Required rollback rules:

- any positive-looking production evidence must be reversible back to `INCOMPLETE` / `NONE`
- rollback must preserve blocker evidence
- rollback must preserve `manualReviewRequired=true`
- rollback must preserve `notTradeInstruction=true`
- rollback must keep `sourceTraceEntryCompleted=false` and `completionReady=false` unless a separately authorized future phase explicitly changes those invariants

## Risk Action Guard Blockers

Production wiring cannot start until these Risk Action Guard blockers are satisfied:

- High risk does not directly mean close, reverse, or open.
- Wick / pin-bar evidence does not confirm trend reversal.
- Liquidity stress / stampede must block opportunity push and require review.
- Missing event data is not no event risk.
- Multi-timeframe agreement alone does not complete SourceTrace.

Each reminder needs production-owned evidence, downgrade rules, tests, and review copy before any wiring discussion. None may become a trade action, trade recommendation, readiness shortcut, or completion shortcut.

## Required Tests Before Any Production Wiring Proposal

Before any production wiring proposal, run at minimum:

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

- production owner tests for every completion field
- source ref singularity and ambiguity tests
- source window stale, future, missing, and inversion tests
- rule id/version missing, unsupported, and mismatch tests
- freshness owner tests proving latest price and kline data cannot substitute for freshness
- conflict family tests preserving nullable Boolean fail-closed semantics
- runtime substitution tests for latest price, raw kline, AI text, dashboard text, external data, order data, and execution data
- consumer isolation tests proving no readiness, `VALID`, dashboard mutation, automation, order, or execution consumer can use review output as completion
- authentication and visibility tests
- audit trail tests for every production-owned field and downgrade reason
- serialization tests proving no forbidden route, field, label, method, or value implies signal, advice, validity, completion, readiness, or trade instruction
- rollback tests from every positive-looking state back to fail-closed review output

## Explicit Production Wiring Decision

Decision: production wiring may not start after P80.

P80 is blocker analysis only. It confirms that the P79 read-only MVP chain is closed, but it also confirms that production SourceTrace completion remains blocked by unresolved ownership, substitution, consumer isolation, authentication, visibility, auditability, downgrade, rollback, and Risk Action Guard gaps.

## Recommended Next Phase

Recommended next phase: a documentation-only production ownership contract design pack.

That phase may define proposed owners and contracts for:

- completion path
- entry price source
- entry source type
- entry source timeframe
- entry source reason
- entry source ref
- source window
- rule id and rule version
- freshness ownership
- conflict family ownership
- consumer isolation
- authentication and visibility
- auditability
- downgrade and rollback rules

The recommended next phase must not implement production wiring, add production adapters, register services, complete SourceTrace, wire BoundaryCandidateService `VALID`, upgrade ExecutionPlan readiness, modify schema/dashboard/config, add external integrations, add order APIs, add auto-trading, or generate real entry / stop / TP / RR values.

## Boundary Confirmations

- P80 is documentation-only.
- P80 does not modify Java.
- P80 does not modify tests.
- P80 does not add controller/endpoint Java.
- P80 does not modify `dashboard.html`.
- P80 does not modify schema.
- P80 does not modify config.
- P80 does not register display DTO/mapper, API DTO/mapper, seam, assembler, fixture helper, or production completion helper as Spring services.
- P80 does not wire endpoint/API mapper into resolver, validation, readiness, dashboard, schema, order, automation, or external data paths.
- P80 does not implement production completion.
- P80 does not add production adapter.
- P80 does not add `DefaultSourceTraceEntryOwnershipAdapter`.
- P80 does not add production `DefaultSourceTraceEntryCompletionContract`.
- P80 does not populate real SourceTrace fields in runtime.
- P80 does not complete full SourceTrace in runtime.
- P80 does not wire BoundaryCandidateService `VALID`.
- P80 does not upgrade ExecutionPlan readiness.
- P80 does not add external data integration, order API, or auto-trading.
- P80 does not generate real entry / stop / TP / RR values.
