# BACKEND-P75 Entry Completion MVP Read-Only Controller Endpoint Boundary Design Pack

## Baseline

- Branch context: PR #247 / Issue #246.
- Baseline commit: `596bd76` (`docs: freeze entry completion api controller gate`).
- Scope: documentation-only MVP read-only controller/endpoint boundary design pack.
- P75 does not modify Java, tests, schema, `dashboard.html`, config, or production wiring.
- P75 removes placeholder `docs/P75.md`.

## P72-P74 API / Controller Gate Summary

BACKEND-P72 introduced the inert read-only API response DTO and mapper:

- `SourceTraceEntryReadOnlyApiResponseDTO`
- `SourceTraceEntryReadOnlyApiResponseMapper`
- `SourceTraceEntryReadOnlyApiResponseMapperTest`
- `docs/PHASE_BACKEND_P72_ENTRY_COMPLETION_MVP_READ_ONLY_API_DTO_MAPPER_SKELETON_RESULT.md`

P72 established that API response serialization accepts already-built display DTO output only. It does not call or wire the display mapper, seam, assembler, validator, resolver, readiness logic, dashboard, schema, external data, order, or automation paths.

BACKEND-P73 expanded guard coverage for malformed, missing, unsafe, runtime-like, production-like, and trade-ready-looking display DTO output. P73 proved that the API mapper keeps output `INCOMPLETE` / `NONE` / review-only, preserves blocker evidence, and never serializes completion, readiness, validity, signal, advice, order behavior, or generated real entry / stop / TP / RR values.

BACKEND-P74 froze the API DTO / mapper safety contract and opened only a documentation/design gate for an MVP read-only controller/endpoint boundary. P74 did not authorize controller Java, endpoint creation, Spring registration, dashboard/schema persistence, readiness wiring, production completion, order APIs, external integrations, or auto-trading.

## MVP Read-Only Controller / Endpoint Purpose

A future MVP read-only endpoint may exist only to expose already-built `SourceTraceEntryReadOnlyApiResponseDTO` output for human review.

The endpoint purpose is:

- display fail-closed SourceTrace entry completion state as review evidence
- preserve blocker evidence for missing, unsafe, or unwired completion inputs
- preserve review-only and non-instructional safety flags
- make it clear that SourceTrace entry completion remains unwired
- make it clear that no BoundaryCandidateService `VALID` state is implied
- make it clear that no ExecutionPlan readiness is implied
- make it clear that no order, execution, close, reverse, or auto-trading behavior is available

The endpoint purpose is not:

- generate entry / stop / take-profit / risk-reward values
- complete SourceTrace
- mark an entry candidate valid
- provide a signal, trade setup, recommendation, or instruction
- create dashboard state
- persist schema state
- call external APIs
- initiate order, execution, automation, close, reverse, or auto-trading paths

## Allowed Route Naming Constraints

Any future route name must be neutral, read-only, and review-scoped.

Allowed route naming constraints:

- include `review`, `read-only`, or another explicit non-execution term
- include `source-trace-entry-completion` or `entry-completion-review` only as a review state, not an approval state
- avoid terms that imply readiness, validity, completion, signal, advice, execution, orders, or trading actions
- use a read-only HTTP shape if an endpoint is later authorized
- avoid path variables or verbs that imply opening, closing, reversing, validating, completing, executing, or submitting anything
- document that route output is a human-review representation of an already-built API DTO

Example route shapes that are acceptable for future design discussion only:

```text
/api/review/source-trace-entry-completion
/api/read-only/source-trace-entry-completion
/api/review/entry-completion-state
```

These examples are not implemented in P75 and do not authorize a controller or endpoint.

## Forbidden Route Names and Labels

Future route names, labels, field names, helper text, method names, or serialized values must not include approval, signal, action, or trade-ready wording such as:

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
- `Order`
- `Execute`
- `AutoTrade`
- `/trade-ready`
- `/ready-to-trade`
- `/entry-ready`
- `/execution-ready`
- `/valid`
- `/completed`
- `/signal`
- `/buy`
- `/sell`
- `/open`
- `/close`
- `/reverse`
- `/order`
- `/execute`
- `/auto-trade`

If runtime-like, production-like, trade-ready-looking, valid-looking, signal-looking, advice-looking, buy/sell/open/close/reverse-looking values appear in already-built DTO evidence, they must remain blockers only. They must never become route names, positive labels, positive fields, status badges, commands, or endpoint affordances.

## Request Boundary

A future endpoint must accept or obtain already-built `SourceTraceEntryReadOnlyApiResponseDTO` output only.

The endpoint must not accept, read, infer from, or directly map:

- latest price
- raw kline items
- AI text
- dashboard text
- external data
- Coinglass data
- news data
- macro calendar data
- order data
- execution data
- account/position mutation data
- generated entry / stop / TP / RR values

The endpoint request boundary must not construct SourceTrace completion. It must not call the seam, assembler, display mapper, API mapper, validator, resolver, readiness logic, dashboard, schema, order, automation, or external data paths unless a later phase explicitly authorizes a still-read-only internal source of already-built API DTO output.

If no already-built API response DTO is available, the endpoint must return fail-closed review output. Absence of input is not a reason to infer completion.

## Response Boundary

A future endpoint response must preserve the already-built API response DTO safety fields and blocker evidence.

Required response values and interpretations:

| Field or evidence | Required representation | Required interpretation |
| --- | --- | --- |
| `completionStatus` | `INCOMPLETE` | SourceTrace entry completion is not complete |
| `completionTransition` | `NONE` | No completion state transition occurred |
| `downgradeReason` | `COMPLETION_UNWIRED`, `MISSING_REQUIRED_FIELD`, or `UNSAFE_COMPLETION` | Output is downgraded and review-blocking |
| `readOnlyIntegrationSeamUnwired` | present and visible | Read-only seam remains unwired from runtime completion |
| `missingFields` | visible blocker evidence | Missing evidence blocks completion |
| `unsafeFields` | visible blocker evidence | Unsafe evidence blocks completion |
| `blockingFields` | visible blocker evidence | Blockers must not be hidden or treated as approval |
| `reviewMode` | `REVIEW_ONLY` | Human review representation only |
| `manualReviewRequired` | `true` | Manual review remains mandatory |
| `notTradeInstruction` | `true` | Output is not advice or instruction |
| `sourceTraceEntryCompleted` | `false` | No runtime SourceTrace completion occurred |
| `completionReady` | `false` | No readiness or execution planning unlock occurred |

Missing or empty blocker lists must not imply completion. Null, malformed, missing, unsafe, runtime-like, production-like, or trade-ready-looking response data must remain fail-closed.

## Required No-Side-Effect Rules

A future endpoint must be read-only and side-effect free.

Required no-side-effect rules:

- no database writes
- no schema persistence
- no dashboard writes or `dashboard.html` changes
- no readiness state changes
- no BoundaryCandidateService `VALID` writes or transitions
- no ExecutionPlan readiness changes
- no order API calls
- no execution API calls
- no close/reverse/open action paths
- no automation dispatch
- no scheduled job creation
- no external API calls
- no Coinglass integration
- no news integration
- no macro calendar integration
- no account, position, or trade mutation
- no generated real entry / stop / TP / RR values

The endpoint must be observational only. Returning a response must not mutate SourceTrace, validation, completion, readiness, dashboard, schema, order, automation, external data, or trading state.

## Required Review-Only / Non-Instructional Copy

A future endpoint must expose copy that prevents trade-readiness interpretation.

Required user-facing copy concepts:

- `Incomplete - review only`
- `No completion transition`
- `Completion path unwired`
- `Missing required source evidence`
- `Unsafe completion evidence`
- `Manual review required`
- `Not a trade instruction`
- `SourceTrace entry not completed`
- `Completion not ready`
- `Read-only review evidence`

Required copy rules:

- show `REVIEW_ONLY` as review-only, not approval
- show `manualReviewRequired=true` as mandatory review, not optional review
- show `notTradeInstruction=true` as a hard non-instructional invariant
- show `sourceTraceEntryCompleted=false` as no runtime completion
- show `completionReady=false` as no readiness
- show blocker fields as blockers, not warnings that can be ignored
- do not use badges, headings, labels, or helper text that imply signal, validity, readiness, action, or trade instruction

## Required Authentication / Visibility Decision Placeholder

P75 does not decide production authentication or visibility.

Before any endpoint skeleton can be implemented, a later phase must explicitly decide:

- whether the endpoint is internal-only, admin-only, development-only, or unavailable outside tests
- whether authentication, authorization, or feature flag controls are required
- whether response caching must be disabled or constrained
- whether endpoint output may appear in logs
- whether endpoint output may be consumed by frontend, monitoring, automation, or integration clients
- whether any caller could misinterpret the response as readiness, validity, signal, advice, or trade instruction
- whether additional audit copy is required to keep output review-only

Until that decision is made, external exposure and dashboard consumption remain blocked.

## Required Serialization Safety Checks

Any future controller/endpoint skeleton must have tests or static checks proving:

- `completionStatus` serializes as `INCOMPLETE`
- `completionTransition` serializes as `NONE`
- `downgradeReason` serializes as `COMPLETION_UNWIRED`, `MISSING_REQUIRED_FIELD`, or `UNSAFE_COMPLETION`
- `readOnlyIntegrationSeamUnwired` is preserved and visible
- `missingFields`, `unsafeFields`, and `blockingFields` remain visible blocker evidence
- missing or empty blocker lists do not imply completion
- `reviewMode=REVIEW_ONLY` is preserved
- `manualReviewRequired=true` is preserved
- `notTradeInstruction=true` is preserved
- `sourceTraceEntryCompleted=false` is preserved
- `completionReady=false` is preserved
- null or malformed already-built API response DTO output fails closed
- runtime-like fields serialize only as blockers
- production-like fields serialize only as blockers
- trade-ready-looking, valid-looking, signal-looking, advice-looking, buy/sell/open/close/reverse-looking values serialize only as blockers
- forbidden route, field, method, label, endpoint, and helper-copy terms remain absent
- no generated real entry / stop / TP / RR values are serialized
- no controller method exposes order / execution / close / reverse / auto-trading / trade-ready wording

## Required Tests Before Any Controller / Endpoint Java Skeleton

Before any Java controller/endpoint skeleton is implemented, add or run focused tests proving:

- route naming uses only read-only review wording
- forbidden route names are absent
- endpoint labels do not include readiness, validity, signal, advice, instruction, order, execution, close, reverse, or auto-trading wording
- null already-built API response DTO fails closed
- missing API safety flags fail closed
- missing `readOnlyIntegrationSeamUnwired` fails closed
- missing or empty `missingFields`, `unsafeFields`, or `blockingFields` does not imply completion
- unsafe fields remain blocking review evidence
- runtime-like and production-like fields remain blockers
- trade-ready-looking, valid-looking, signal-looking, advice-looking, buy/sell/open/close/reverse-looking values remain blockers only
- response preserves `INCOMPLETE`, `NONE`, `COMPLETION_UNWIRED`, `MISSING_REQUIRED_FIELD`, and `UNSAFE_COMPLETION`
- response preserves `REVIEW_ONLY`, `manualReviewRequired=true`, and `notTradeInstruction=true`
- response preserves `sourceTraceEntryCompleted=false` and `completionReady=false`
- controller exposes no forbidden field/method names
- controller has no order/execution/close/reverse/auto-trading method surface
- controller creates no dashboard/schema/readiness/order/automation/external data wiring
- controller makes no database writes or external calls
- no real entry / stop / TP / RR values are generated or serialized
- existing P72-P73 API mapper tests continue to pass

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

## Controller / Endpoint Skeleton Decision

Decision: a future Java controller/endpoint skeleton may start next only as a separately authorized inert read-only skeleton.

This decision does not authorize production completion, production adapter implementation, dashboard wiring, schema persistence, readiness wiring, external integrations, order APIs, auto-trading, or generated entry / stop / TP / RR values.

The future skeleton may be considered only if it:

- exposes already-built API response DTO output for human review
- returns fail-closed output when already-built API response DTO output is unavailable or malformed
- preserves blocker evidence without hiding or converting it to approval
- preserves review-only and non-instructional safety flags
- has no side effects
- has no production completion, readiness, order, execution, automation, dashboard, schema, or external data wiring
- is covered by focused controller/serialization tests before it is treated as usable

## Strict Next-Stage Scope

If separately authorized, the next Java controller/endpoint skeleton stage may:

- add the smallest inert read-only controller/endpoint boundary
- use a route name that follows the allowed read-only review naming constraints
- accept or obtain already-built `SourceTraceEntryReadOnlyApiResponseDTO` output only
- return a fail-closed API response when input is null, missing, malformed, unsafe, runtime-like, production-like, or trade-ready-looking
- preserve `INCOMPLETE`, `NONE`, `COMPLETION_UNWIRED`, `MISSING_REQUIRED_FIELD`, and `UNSAFE_COMPLETION`
- preserve `readOnlyIntegrationSeamUnwired`, `missingFields`, `unsafeFields`, and `blockingFields`
- preserve `REVIEW_ONLY`, `manualReviewRequired=true`, `notTradeInstruction=true`, `sourceTraceEntryCompleted=false`, and `completionReady=false`
- add focused controller/serialization tests
- add a result document

The next Java controller/endpoint skeleton stage must not:

- modify `dashboard.html`
- modify schema
- create dashboard persistence
- register display DTO/mapper, API DTO/mapper, seam, or assembler as Spring services
- wire the endpoint into resolver, validation, readiness, dashboard, schema, order, automation, or external data paths
- implement production completion
- add production adapters or contracts
- populate runtime SourceTrace fields
- complete full SourceTrace
- wire BoundaryCandidateService `VALID`
- upgrade ExecutionPlan readiness
- add external data integration, order API, or auto-trading
- generate real entry / stop / TP / RR values

## Still-Blocked Production Paths

These remain blocked after P75:

- controller or endpoint Java skeleton in P75
- controller or endpoint creation in P75
- `dashboard.html` changes
- schema changes
- Spring service registration for display DTO/mapper, API DTO/mapper, seam, or assembler
- API mapper wiring into resolver, validation, readiness, dashboard, schema, order, automation, or external data paths
- endpoint wiring into resolver, validation, readiness, dashboard, schema, order, automation, or external data paths
- production completion implementation
- production adapter implementation
- `DefaultSourceTraceEntryOwnershipAdapter`
- production `DefaultSourceTraceEntryCompletionContract`
- runtime SourceTrace field population
- full SourceTrace completion
- BoundaryCandidateService `VALID` production path
- ExecutionPlan readiness upgrade
- external data integration
- Coinglass integration
- news or macro API integration
- order API
- auto-trading
- real entry / stop / TP / RR generation or serialization

## Boundary Confirmations

- P75 is documentation-only.
- P75 does not modify Java, tests, schema, `dashboard.html`, config, or production wiring.
- P75 does not add a controller/endpoint Java skeleton.
- P75 does not create a controller or endpoint.
- P75 does not register display DTO/mapper, API DTO/mapper, seam, or assembler as Spring services.
- P75 does not wire the API mapper into resolver, validation, readiness, dashboard, schema, order, automation, or external data paths.
- P75 does not implement production completion or adapters.
- P75 does not complete SourceTrace or populate runtime SourceTrace fields.
- P75 does not wire BoundaryCandidateService `VALID` or ExecutionPlan readiness.
- P75 does not add external data integration, order API, or auto-trading.
- P75 does not generate real entry / stop / TP / RR values.
- Placeholder `docs/P75.md` is removed.

## Risk Action Guard

- High risk does not directly mean close, reverse, or open.
- Wick / pin-bar evidence does not confirm trend reversal.
- Liquidity stress / stampede must block opportunity push and require review.
- Missing event data is not no event risk.
- Multi-timeframe agreement alone does not complete SourceTrace.
