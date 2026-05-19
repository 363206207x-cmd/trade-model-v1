# BACKEND-P74 Entry Completion MVP Read-Only API Mapper Safety Freeze and Controller Gate

## Baseline

- Branch context: PR #245 / Issue #244.
- Baseline commit: `753f0f1` (`test: expand entry completion api mapper guards`).
- Scope: documentation-only API DTO / mapper safety freeze and MVP read-only controller/endpoint gate.
- P74 does not modify Java, tests, schema, `dashboard.html`, config, or production wiring.
- P74 removes placeholder `docs/P74.md`.

## P72 API DTO / Mapper Skeleton Summary

BACKEND-P72 introduced the inert MVP read-only API DTO / mapper boundary:

- `SourceTraceEntryReadOnlyApiResponseDTO`
- `SourceTraceEntryReadOnlyApiResponseMapper`
- `SourceTraceEntryReadOnlyApiResponseMapperTest`
- `docs/PHASE_BACKEND_P72_ENTRY_COMPLETION_MVP_READ_ONLY_API_DTO_MAPPER_SKELETON_RESULT.md`

P72 established that the API mapper accepts already-built `SourceTraceEntryReadOnlyDisplayDTO` output only. It does not call or wire the display mapper, seam, assembler, validator, resolver, readiness logic, dashboard, schema, order, automation, or external data paths.

P72 API mapper behavior:

- null display DTO output maps to fail-closed API response output
- missing display safety flags remain fail closed
- missing or empty blocker lists remain fail closed
- unsafe fields serialize as blocking review evidence
- runtime-like and production-like fields serialize as blockers
- trade-ready-looking, valid-looking, signal-looking, advice-looking, buy/sell/open/close/reverse-looking values serialize as blockers
- `INCOMPLETE` is preserved as API status
- `NONE` is preserved as API transition
- `COMPLETION_UNWIRED`, `MISSING_REQUIRED_FIELD`, and `UNSAFE_COMPLETION` are preserved
- `readOnlyIntegrationSeamUnwired`, `missingFields`, `unsafeFields`, and `blockingFields` remain visible
- output remains review-only and non-instructional
- no generated real entry / stop / TP / RR values are serialized
- DTO/mapper are plain Java classes with no Spring controller/service/component/repository annotations

## P73 API Mapper Guard Expansion Summary

BACKEND-P73 expanded guard coverage around malformed, missing, unsafe, runtime-like, production-like, and trade-ready-looking display DTO output.

P73 proves:

- null display DTO fails closed
- missing `readOnlyIntegrationSeamUnwired` fails closed
- missing or empty `missingFields` does not imply completion
- missing or empty `blockingFields` does not imply completion
- missing `reviewMode`, unsafe `manualReviewRequired=false`, and unsafe `notTradeInstruction=false` fail closed
- unsafe `sourceTraceEntryCompleted=true` fails closed
- unsafe `completionReady=true` fails closed
- unsafe completion status other than `INCOMPLETE` fails closed
- unsafe completion transition other than `NONE` fails closed
- null or unsupported downgrade reason fails closed
- required API label/helper copy is preserved for `MISSING_REQUIRED_FIELD`, `UNSAFE_COMPLETION`, and `COMPLETION_UNWIRED`
- runtime-like fields serialize only as blockers
- production-like fields serialize only as blockers
- trade-ready-looking, valid-looking, signal-looking, advice-looking, buy/sell/open/close/reverse-looking values serialize only as blockers
- complete safe-looking display DTO still serializes as unwired / review-only
- generated real entry / stop / TP / RR value surfaces remain absent
- API DTO/mapper method and field surfaces avoid forbidden order / execution / close / reverse / auto-trading / trade-ready names
- API DTO/mapper remain free of Spring controller/service/component/repository annotations
- production adapter and production completion contract remain absent

## API DTO / Mapper Default Safety Invariants

The frozen API DTO / mapper contract requires these defaults and mapped outputs:

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

These values are safety invariants. They prevent API serialization from being interpreted as SourceTrace completion, BoundaryCandidateService `VALID`, ExecutionPlan readiness, advice, signal, trade instruction, executable output, or order behavior.

## API Fail-Closed Matrix Summary

| Display/API input condition | API response result | Required interpretation |
| --- | --- | --- |
| null display DTO | `INCOMPLETE` / `NONE` / `MISSING_REQUIRED_FIELD` | Missing display evidence, review required |
| missing `readOnlyIntegrationSeamUnwired` | `INCOMPLETE` / `NONE` / `MISSING_REQUIRED_FIELD` | Unwired marker missing, review required |
| empty or missing `missingFields` | `INCOMPLETE` / `NONE` / `MISSING_REQUIRED_FIELD` | Empty missing evidence is not completion |
| empty or missing `blockingFields` | `INCOMPLETE` / `NONE` / `MISSING_REQUIRED_FIELD` | Empty blockers are not approval |
| missing review-only or non-instructional flags | `INCOMPLETE` / `NONE` / `MISSING_REQUIRED_FIELD` | Safety flags malformed, review required |
| `sourceTraceEntryCompleted=true` | forced API `false`, blocker recorded | Unsafe display value, no runtime completion |
| `completionReady=true` | forced API `false`, blocker recorded | Unsafe display value, no readiness |
| status other than `INCOMPLETE` | forced API `INCOMPLETE`, blocker recorded | Positive-looking status is not API readiness |
| transition other than `NONE` | forced API `NONE`, blocker recorded | Transition-looking value is not completion |
| null or unsupported downgrade reason | `MISSING_REQUIRED_FIELD` | Missing or unsafe downgrade metadata |
| runtime-like fields | `UNSAFE_COMPLETION` | Runtime evidence blocks completion |
| production-like fields | `UNSAFE_COMPLETION` | Production wording blocks API readiness |
| trade-ready-looking fields | `UNSAFE_COMPLETION` | Advice/signal/trade wording blocks API readiness |
| complete safe-looking display DTO | `INCOMPLETE` / `NONE` / `COMPLETION_UNWIRED` | Still unwired, review-only, non-instructional |

## Review-Only / Non-Instructional API Invariants

Any future controller or endpoint design that exposes this API DTO must keep these values visible and unchanged:

```text
reviewMode=REVIEW_ONLY
manualReviewRequired=true
notTradeInstruction=true
sourceTraceEntryCompleted=false
completionReady=false
```

Required API-facing interpretation:

- `Incomplete - review only` means not complete and not approved
- `No completion transition` means no completion state transition occurred
- `Completion path unwired` means the completion path is not active
- `Missing required source evidence` means evidence blocks completion
- `Unsafe completion evidence` means unsafe evidence blocks completion
- `Review only` means human review representation only
- `Manual review required` means no automated unlock
- `Not a trade instruction` means no open, close, reverse, or order action
- `SourceTrace entry not completed` means no runtime SourceTrace completion occurred
- `Completion not ready` means no readiness or execution planning unlock

API output must never be styled, named, serialized, filtered, or routed as approval, validity, readiness, signal, advice, instruction, executable output, or order behavior.

## Forbidden Label / Field / Method / Endpoint Surface Summary

The frozen API DTO / mapper chain must not introduce user-facing labels, fields, method names, endpoint names, or route names such as:

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
- `/trade-ready`
- `/ready-to-trade`
- `/entry-ready`
- `/execution-ready`
- `/signal`
- `/buy`
- `/sell`
- `/open`
- `/close`
- `/reverse`
- `/order`
- `/execute`
- `/auto-trade`

Unsafe display fields containing those meanings must be serialized only as blockers under `unsafeFields` / `blockingFields`, never as positive API status, positive labels, action names, or route names.

## Still-Blocked Production Paths

These remain blocked after P74:

- controller or endpoint Java skeleton in P74
- controller or endpoint creation
- `dashboard.html` changes
- schema changes
- Spring registration for display DTO/mapper, API DTO/mapper, seam, or assembler
- API mapper wiring into resolver, validation, readiness, dashboard, schema, order, automation, or external data paths
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
- real entry / stop / TP / RR generation or serialization

## Controller / Endpoint Decision

Decision: a future MVP read-only controller/endpoint boundary design may start next, but only as a documentation/design package.

This decision does not authorize a Java controller skeleton, endpoint creation, Spring registration, dashboard integration, schema persistence, readiness wiring, order behavior, automation, external integrations, production completion, or generated entry/stop/TP/RR values.

The next stage may design what a future endpoint would require, but runtime exposure remains blocked until the controller safety contract, route naming, serialization guarantees, and no-side-effect tests are separately reviewed and authorized.

## Strict Next Controller Boundary Design Scope

The next controller boundary design stage may:

- define a read-only endpoint purpose in documentation
- define allowed route naming constraints
- define forbidden route names and labels
- define request/response boundaries for already-built API response DTO output
- require that a future endpoint return human-review data only
- require that a future endpoint preserve `INCOMPLETE`, `NONE`, `COMPLETION_UNWIRED`, `MISSING_REQUIRED_FIELD`, and `UNSAFE_COMPLETION`
- require that a future endpoint preserve `readOnlyIntegrationSeamUnwired`, `missingFields`, `unsafeFields`, and `blockingFields`
- require that a future endpoint preserve `REVIEW_ONLY`, `manualReviewRequired=true`, and `notTradeInstruction=true`
- require that a future endpoint preserve `sourceTraceEntryCompleted=false` and `completionReady=false`
- define tests required before any Java controller skeleton
- add one documentation-only result/gate document

The next controller boundary design stage must not:

- add a Java controller or endpoint skeleton
- add Spring registration
- modify `dashboard.html`
- modify schema
- wire API mapper into resolver, validation, readiness, dashboard, schema, order, automation, or external data paths
- implement production completion
- add production adapters or contracts
- populate runtime SourceTrace fields
- complete full SourceTrace
- wire BoundaryCandidateService `VALID`
- upgrade ExecutionPlan readiness
- add external data integration, order API, or auto-trading
- generate real entry / stop / TP / RR values

## Exact Blockers Before Any Controller / Endpoint Skeleton

Even after a controller boundary design stage, a Java controller/endpoint skeleton remains blocked until all of the following are separately satisfied:

- explicit route naming contract with no forbidden readiness/trading/action wording
- explicit controller annotation and registration safety contract
- proof that any endpoint accepts or obtains already-built API response DTO output only
- proof that endpoint serialization cannot create completion/readiness/advice/instruction fields
- tests proving null or malformed API response DTO fails closed
- tests proving blocker lists cannot be dropped or hidden
- tests proving unsafe/runtime-like/production-like/trade-ready-looking values serialize only as blockers
- tests proving no dashboard/schema/readiness/order/automation/external data side effects
- review of whether endpoint output can be consumed by any runtime client
- explicit non-trading and review-only copy for users of the endpoint
- separate approval for controller skeleton implementation

## Required Tests Before Any Controller / Endpoint Skeleton

Before any Java controller/endpoint skeleton is implemented, add or run focused tests proving:

- no forbidden route names are introduced
- no controller exposes readiness, validity, signal, advice, instruction, order, execution, close, reverse, or auto-trading wording
- null API response DTO fails closed
- missing API safety flags fail closed
- missing or empty blocker lists do not imply completion
- unsafe fields serialize as blocking review evidence
- runtime-like and production-like fields serialize as blockers
- trade-ready-looking, valid-looking, signal-looking, advice-looking, buy/sell/open/close/reverse-looking values remain blockers only
- API response exposes no forbidden field/method names
- controller design creates no dashboard/schema/readiness/order/automation/external data wiring
- existing P72-P73 mapper tests continue to pass

Recommended regression set:

```text
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

## Boundary Confirmations

- P74 is documentation-only.
- P74 does not modify Java, tests, schema, `dashboard.html`, config, or production wiring.
- P74 does not add a controller/endpoint Java skeleton.
- P74 does not create a controller or endpoint.
- P74 does not register display DTO/mapper, API DTO/mapper, seam, or assembler as Spring services.
- P74 does not wire the API mapper into resolver, validation, readiness, dashboard, schema, order, automation, or external data paths.
- P74 does not implement production completion or adapters.
- P74 does not complete SourceTrace or populate runtime SourceTrace fields.
- P74 does not wire BoundaryCandidateService `VALID` or ExecutionPlan readiness.
- P74 does not add external data integration, order API, or auto-trading.
- P74 does not generate real entry / stop / TP / RR values.

## Risk Action Guard

- High risk does not directly mean close, reverse, or open.
- Wick / pin-bar evidence does not confirm trend reversal.
- Liquidity stress / stampede must block opportunity push and require review.
- Missing event data is not no event risk.
- Multi-timeframe agreement alone does not complete SourceTrace.
