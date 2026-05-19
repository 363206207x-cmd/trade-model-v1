# BACKEND-P71 Entry Completion MVP Read-Only Display Mapper Safety Freeze and API Gate

## Baseline

- Branch context: PR #239 / Issue #238.
- Baseline commit: `3452ae9` (`test: expand entry completion display mapper guards`).
- Scope: documentation-only display DTO / mapper safety freeze and MVP read-only API gate.
- P71 does not modify Java, tests, schema, `dashboard.html`, config, or production wiring.
- P71 removes placeholder `docs/P71.md`.

## P69 Display DTO / Mapper Skeleton Summary

BACKEND-P69 introduced the inert MVP read-only display DTO / mapper boundary:

- `SourceTraceEntryReadOnlyDisplayDTO`
- `SourceTraceEntryReadOnlyDisplayMapper`
- `SourceTraceEntryReadOnlyDisplayMapperTest`
- `docs/PHASE_BACKEND_P69_ENTRY_COMPLETION_MVP_READ_ONLY_DISPLAY_DTO_MAPPER_SKELETON_RESULT.md`

P69 established that the mapper accepts already-built fail-closed seam output only. It does not call or wire the seam, assembler, validator, resolver, readiness logic, dashboard, schema, order, automation, or external data paths.

P69 display behavior:

- null seam output maps to fail-closed display evidence
- `INCOMPLETE` is preserved as display status
- `NONE` is preserved as display transition
- `COMPLETION_UNWIRED`, `MISSING_REQUIRED_FIELD`, and `UNSAFE_COMPLETION` are represented with required labels/helper copy
- `readOnlyIntegrationSeamUnwired` remains visible as blocking review evidence
- missing fields and unsafe fields remain visible as blockers
- output remains review-only and non-instructional
- no generated real entry / stop / TP / RR values are displayed
- DTO/mapper are plain Java classes with no Spring controller/service/component/repository annotations

## P70 Display Mapper Guard Expansion Summary

BACKEND-P70 expanded guard coverage around malformed, missing, unsafe, runtime-like, production-like, and trade-ready-looking seam output.

P70 proves:

- null seam output fails closed
- missing `readOnlyIntegrationSeamUnwired` fails closed
- missing or empty `missingFields` does not imply completion
- missing `reviewMode`, unsafe `manualReviewRequired=false`, and unsafe `notTradeInstruction=false` fail closed
- unsafe `sourceTraceEntryCompleted=true` fails closed
- unsafe `completionReady=true` fails closed
- unsafe completion status other than `INCOMPLETE` fails closed
- unsafe completion transition other than `NONE` fails closed
- null downgrade reason fails closed
- required label/helper copy is preserved for `MISSING_REQUIRED_FIELD`, `UNSAFE_COMPLETION`, and `COMPLETION_UNWIRED`
- runtime-like fields remain unsafe blockers
- production-like fields remain unsafe blockers
- trade-ready-looking, validity-looking, signal-looking, advice-looking, buy/sell/open/close/reverse-looking fields remain unsafe blockers
- complete safe-looking seam output still displays as unwired / review-only
- forbidden display field/method surfaces and generated entry / stop / TP / RR value surfaces remain absent
- production adapter and production completion contract remain absent

## Display DTO / Mapper Default Safety Invariants

The frozen display DTO / mapper contract requires these defaults and mapped outputs:

- `completionStatus=INCOMPLETE`
- `completionTransition=NONE`
- `reviewMode=REVIEW_ONLY`
- `manualReviewRequired=true`
- `notTradeInstruction=true`
- `sourceTraceEntryCompleted=false`
- `completionReady=false`
- `severity=blocking_review`
- `readinessEffect=blocks_completion_ready`
- `sourceTraceEffect=source_trace_entry_completed_false`
- `instructionEffect=not_trade_instruction`

These values are not optional UX decoration. They are safety invariants that prevent display output from being read as SourceTrace completion, BoundaryCandidateService `VALID`, ExecutionPlan readiness, advice, signal, or trade instruction.

## Display Fail-Closed Matrix Summary

| Input / seam condition | Display result | Required interpretation |
| --- | --- | --- |
| null seam output | `INCOMPLETE` / `NONE` / `MISSING_REQUIRED_FIELD` | Missing seam evidence, review required |
| missing `readOnlyIntegrationSeamUnwired` | `INCOMPLETE` / `NONE` / `MISSING_REQUIRED_FIELD` | Seam unwired marker missing, review required |
| empty or missing `missingFields` | `INCOMPLETE` / `NONE` / `MISSING_REQUIRED_FIELD` | Empty blockers are not completion |
| missing review-only or non-instructional flags | `INCOMPLETE` / `NONE` / `MISSING_REQUIRED_FIELD` | Safety flags malformed, review required |
| `sourceTraceEntryCompleted=true` | forced display `false`, blocker recorded | Unsafe seam value, no runtime completion |
| `completionReady=true` | forced display `false`, blocker recorded | Unsafe seam value, no readiness |
| status other than `INCOMPLETE` | forced display `INCOMPLETE`, blocker recorded | Positive-looking status is not display readiness |
| transition other than `NONE` | forced display `NONE`, blocker recorded | Transition-looking value is not completion |
| null downgrade reason | `MISSING_REQUIRED_FIELD` | Missing downgrade metadata, review required |
| runtime-like fields | `UNSAFE_COMPLETION` | Runtime evidence blocks completion |
| production-like fields | `UNSAFE_COMPLETION` | Production wording blocks display readiness |
| trade-ready-looking fields | `UNSAFE_COMPLETION` | Advice/signal/trade wording blocks display readiness |
| complete safe-looking seam output | `INCOMPLETE` / `NONE` / `COMPLETION_UNWIRED` | Still unwired, review-only, non-instructional |

## Review-Only / Non-Instructional Display Invariants

Any future display/API use of the P69-P70 mapper must keep these visible and unchanged:

```text
reviewMode=REVIEW_ONLY
manualReviewRequired=true
notTradeInstruction=true
sourceTraceEntryCompleted=false
completionReady=false
```

Required copy remains:

- `Incomplete - review only`
- `No completion transition`
- `Completion path unwired`
- `Missing required source evidence`
- `Unsafe completion evidence`
- `Review only`
- `Manual review required`
- `Not a trade instruction`
- `SourceTrace entry not completed`
- `Completion not ready`
- `Read-only seam unwired`

Display output must remain human-review representation only. It must not be styled, named, serialized, or filtered as approval, validity, readiness, signal, advice, or instruction.

## Forbidden Label / Field / Method Surface Summary

The frozen display DTO / mapper chain must not introduce user-facing fields, labels, or method surfaces such as:

- `tradeReady`
- `readyToTrade`
- `entryReady`
- `executionReady`
- `Valid`
- `Completed`
- `Signal`
- `Buy`
- `Sell`
- `Open`
- `Close`
- `Reverse`
- generated real `entryPrice`
- generated stop price
- generated take-profit price
- generated risk-reward value
- order / execution / close / reverse / auto-trading / trade-ready method names

Unsafe seam fields containing those meanings must be displayed only as blockers under `unsafeFields` / `blockingFields`, never as positive status or call-to-action copy.

## Still-Blocked Production Paths

These remain blocked after P71:

- API/controller Java skeleton in P71
- controller or endpoint creation
- `dashboard.html` changes
- schema changes
- Spring registration for display DTO/mapper, seam, or assembler
- display mapper wiring into resolver, validation, readiness, dashboard, schema, order, automation, or external data paths
- production completion implementation
- production adapter implementation
- `DefaultSourceTraceEntryOwnershipAdapter`
- production `DefaultSourceTraceEntryCompletionContract`
- runtime SourceTrace field population
- full SourceTrace completion
- BoundaryCandidateService `VALID` production path
- ExecutionPlan readiness upgrade
- external data integration
- order API
- auto-trading
- real entry / stop / TP / RR generation

## API Decision

Decision: a future MVP read-only API boundary skeleton may start next, but only as an inert API-shape skeleton that serializes already-built display DTO output for human review.

This decision does not authorize production completion, controller activation, endpoint exposure to runtime clients, dashboard integration, schema persistence, readiness wiring, order behavior, automation, external integrations, or generated entry/stop/TP/RR values.

## Strict Next API Skeleton Stage Scope

The next stage may:

- define a minimal read-only API response DTO if needed
- define a minimal mapper from `SourceTraceEntryReadOnlyDisplayDTO` to that API response DTO
- accept already-built display DTO output only
- preserve `INCOMPLETE`, `NONE`, `COMPLETION_UNWIRED`, `MISSING_REQUIRED_FIELD`, and `UNSAFE_COMPLETION`
- preserve `readOnlyIntegrationSeamUnwired`
- preserve `missingFields`, `unsafeFields`, and `blockingFields` as blocking review evidence
- preserve `REVIEW_ONLY`, `manualReviewRequired=true`, and `notTradeInstruction=true`
- preserve `sourceTraceEntryCompleted=false` and `completionReady=false`
- reject or downgrade malformed display DTO inputs
- add focused API DTO/mapper tests
- add a result document

The next stage must not:

- create a controller or endpoint unless separately authorized after API DTO/mapper skeleton review
- modify `dashboard.html`
- modify schema
- register display DTO/mapper, seam, assembler, or API mapper as Spring services
- wire into resolver, validation, readiness, dashboard, schema, order, automation, or external data paths
- implement production completion
- add production adapters or contracts
- populate runtime SourceTrace fields
- complete full SourceTrace
- wire BoundaryCandidateService `VALID`
- upgrade ExecutionPlan readiness
- add external data integration, order API, or auto-trading
- generate real entry / stop / TP / RR values

## Exact Blockers Before Runtime API Wiring

Even if a future API DTO/mapper skeleton is added, runtime API wiring remains blocked until all of the following are separately designed, tested, and authorized:

- explicit controller/endpoint safety contract
- endpoint authentication/visibility decision
- serialization contract proving forbidden labels and readiness wording remain absent
- tests proving malformed display DTO cannot serialize as completion/readiness/advice/instruction
- tests proving no dashboard/schema/readiness/order/automation side effects
- review of whether API output can be consumed by any runtime client
- explicit non-trading legal/safety copy for user-facing API consumers
- separate approval for any controller registration

## Required Tests Before Any API Skeleton

Before any MVP read-only API boundary skeleton is implemented, add or run focused tests proving:

- null display DTO fails closed
- missing display safety flags fail closed
- missing or empty blocker lists do not imply completion
- unsafe fields serialize as blocking review evidence
- runtime-like and production-like fields serialize as blockers
- trade-ready-looking, valid-looking, signal-looking, advice-looking, buy/sell/open/close/reverse-looking values do not become labels or positive fields
- API response exposes no forbidden field/method names
- API response has no Spring controller/service/component/repository annotations
- no controller or endpoint is created unless explicitly authorized
- no dashboard/schema/readiness/order/automation/external data wiring is created
- existing P69-P70 mapper tests continue to pass

Recommended regression set:

```text
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

## Boundary Confirmations

- P71 is documentation-only.
- P71 does not modify Java, tests, schema, `dashboard.html`, config, or production wiring.
- P71 does not add an API/controller Java skeleton.
- P71 does not create a controller or endpoint.
- P71 does not register display DTO/mapper, seam, or assembler as Spring services.
- P71 does not wire the display mapper into resolver, validation, readiness, dashboard, schema, order, automation, or external data paths.
- P71 does not implement production completion or adapters.
- P71 does not complete SourceTrace or populate runtime SourceTrace fields.
- P71 does not wire BoundaryCandidateService `VALID` or ExecutionPlan readiness.
- P71 does not add external data integration, order API, or auto-trading.
- P71 does not generate real entry / stop / TP / RR values.

## Risk Action Guard

- High risk does not directly mean close, reverse, or open.
- Wick / pin-bar evidence does not confirm trend reversal.
- Liquidity stress / stampede must block opportunity push and require review.
- Missing event data is not no event risk.
- Multi-timeframe agreement alone does not complete SourceTrace.
