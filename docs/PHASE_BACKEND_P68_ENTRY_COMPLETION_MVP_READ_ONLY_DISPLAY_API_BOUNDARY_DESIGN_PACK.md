# BACKEND-P68 Entry Completion MVP Read-Only Display API Boundary Design Pack

## Baseline

- Branch context: PR #233 / Issue #232.
- Baseline commit: `b901907` (`docs: freeze entry completion seam display gate`).
- Scope: documentation-only MVP read-only display/API boundary design pack.
- P68 does not modify Java, tests, schema, `dashboard.html`, config, or production wiring.

## P65-P67 Seam and Display Gate Summary

BACKEND-P65 introduced `SourceTraceEntryReadOnlyIntegrationSeam` as an unwired, non-Spring boundary between already-built validation/completion context and the read-only assembler.

P65 seam behavior:

- accepts `EntryOwnershipValidationCompletionContext`
- accepts `SourceTraceEntryReadOnlyCompletionRequest`
- delegates read-only request evaluation to `SourceTraceEntryReadOnlyCompletionAssembler`
- forces DTO output back to `INCOMPLETE`
- forces transition back to `NONE`
- preserves context missing fields and assembler missing/unsafe fields
- adds `readOnlyIntegrationSeamUnwired`
- preserves `MISSING_REQUIRED_FIELD` and `UNSAFE_COMPLETION`
- otherwise downgrades to `COMPLETION_UNWIRED`
- remains unregistered as a Spring service
- implements no production boundary interfaces
- is not wired into resolver, validation, readiness, dashboard, schema, order, automation, or external data paths

BACKEND-P66 expanded guard coverage and hardened null-input behavior so every seam output appends `readOnlyIntegrationSeamUnwired`.

P66 guard coverage proves:

- seam presence alone fails closed
- null validation/completion context fails closed
- null read-only input fails closed
- both null inputs fail closed with both missing fields
- fail-closed validation context missing fields are preserved
- assembler missing fields are preserved
- assembler unsafe fields are preserved
- duplicate missing fields are de-duplicated while preserving first-seen order
- complete safe read-only input still returns `INCOMPLETE` / `NONE` / `COMPLETION_UNWIRED`
- runtime-like and production-like source tags remain unsafe through the seam
- seam output remains `REVIEW_ONLY`
- `manualReviewRequired=true`
- `notTradeInstruction=true`
- `sourceTraceEntryCompleted=false`
- `completionReady=false`
- no order / execution / close / reverse / auto-trading / trade-ready method names appear
- no Spring service/component annotations appear
- no production boundary interfaces are implemented

BACKEND-P67 froze the P65-P66 seam safety state and authorized this P68 stage only as documentation/design. P67 did not authorize Java display/API skeletons, dashboard changes, schema changes, controller wiring, persistence, readiness wiring, external integrations, order APIs, automation, or real entry/stop/TP/RR generation.

## MVP Read-Only Display/API Purpose

The MVP read-only display/API boundary is a future human-review representation for fail-closed seam output.

It must help an operator understand:

- the seam is incomplete
- the transition is none
- the completion path is unwired
- SourceTrace entry completion is not done
- readiness is false
- manual review is required
- the payload is not a trade instruction
- missing and unsafe fields are blockers, not hidden warnings

It must not:

- complete SourceTrace
- imply a valid entry source
- imply BoundaryCandidateService `VALID`
- imply ExecutionPlan readiness
- provide trade advice
- provide execution advice
- populate dashboard state
- persist schema fields
- trigger order, close, reverse, open, automation, or external data behavior

## Allowed Output Fields for Human Review

Future display/API design may expose only read-only review fields derived from already fail-closed seam output:

| Field | Purpose | Safety requirement |
| --- | --- | --- |
| `symbol` | Optional review metadata | Must be label-only and never readiness evidence |
| `timeframe` | Optional review metadata | Must be label-only and never readiness evidence |
| `completionStatus` | Show seam status | Must display `INCOMPLETE` for current seam chain |
| `completionTransition` | Show transition state | Must display `NONE` for current seam chain |
| `downgradeReason` | Show downgrade state | Must preserve `MISSING_REQUIRED_FIELD`, `UNSAFE_COMPLETION`, or `COMPLETION_UNWIRED` |
| `readOnlyIntegrationSeamUnwired` | Show seam is unwired | Must be explicit and visible |
| `missingFields` | Show missing blockers | Must be visible and not summarized as success |
| `unsafeFields` | Show unsafe blockers when distinguishable | Must require review and block readiness interpretation |
| `reviewMode` | Show review-only mode | Must display `REVIEW_ONLY` |
| `manualReviewRequired` | Show review gate | Must display `true` |
| `notTradeInstruction` | Show non-instructional state | Must display `true` |
| `sourceTraceEntryCompleted` | Show completion flag | Must display `false` |
| `completionReady` | Show readiness flag | Must display `false` |
| `sourceTraceEntryOwnershipCompletionPath` | Optional review metadata | May be displayed only as untrusted/unwired review metadata |
| `entrySourceType` | Optional review metadata | May be displayed only as untrusted/unwired review metadata |
| `entrySourceTimeframe` | Optional review metadata | May be displayed only as untrusted/unwired review metadata |
| `entrySourceReason` | Optional review metadata | Must not be phrased as trade advice |
| `entrySourceRef` | Optional review metadata | Must not be treated as ownership completion |
| `ruleId` | Optional provenance metadata | Must be label-only |
| `ruleVersion` | Optional provenance metadata | Must be label-only |
| `sourceWindow` | Optional provenance metadata | Must be label-only |
| `freshnessStatus` | Optional freshness metadata | Must not imply readiness |
| `observedAtMs` | Optional freshness metadata | Must not imply readiness |
| `decisionCreateTimeMs` | Optional freshness metadata | Must not imply readiness |
| conflict flags | Optional conflict metadata | Null or true remains blocking; false does not complete SourceTrace |

## Forbidden Output Fields and Labels

Future display/API design must not expose or introduce:

- `tradeReady`
- `readyToTrade`
- `entryReady`
- `executionReady`
- `sourceTraceReady`
- `boundaryCandidateValid`
- `executionPlanReady`
- `orderReady`
- `autoTradeReady`
- `openSignal`
- `closeSignal`
- `reverseSignal`
- `entryInstruction`
- `tradeInstruction`
- `orderInstruction`
- `entryPrice`
- generated real entry price
- generated stop price
- generated take-profit price
- generated risk-reward value
- generated liquidity decision
- generated event decision
- generated wick reversal confirmation
- dashboard persistence status
- schema persistence status

Forbidden user-facing labels include:

- `Ready`
- `Valid`
- `Completed`
- `Trade Ready`
- `Entry Ready`
- `Execution Ready`
- `Safe to Trade`
- `Signal`
- `Buy`
- `Sell`
- `Open`
- `Close`
- `Reverse`
- `Auto`

## Required User-Facing Labels

Current seam states must be represented with explicit review-only labels:

| Internal value | Required label | Required helper copy |
| --- | --- | --- |
| `INCOMPLETE` | `Incomplete - review only` | `SourceTrace entry completion is not complete.` |
| `NONE` | `No completion transition` | `No completion transition has occurred.` |
| `COMPLETION_UNWIRED` | `Completion path unwired` | `The read-only seam is present, but completion wiring is not active.` |
| `MISSING_REQUIRED_FIELD` | `Missing required source evidence` | `Required ownership/source/freshness/conflict evidence is missing.` |
| `UNSAFE_COMPLETION` | `Unsafe completion evidence` | `Unsafe or runtime-like evidence blocks completion and requires review.` |
| `REVIEW_ONLY` | `Review only` | `This output is for human review and cannot be used as an instruction.` |
| `manualReviewRequired=true` | `Manual review required` | `Human review is mandatory before any future interpretation.` |
| `notTradeInstruction=true` | `Not a trade instruction` | `Do not use this output to open, close, reverse, or place orders.` |
| `sourceTraceEntryCompleted=false` | `SourceTrace entry not completed` | `No runtime SourceTrace entry completion has occurred.` |
| `completionReady=false` | `Completion not ready` | `This output is not readiness and cannot unlock execution planning.` |

## Required Representation for `readOnlyIntegrationSeamUnwired`

`readOnlyIntegrationSeamUnwired` must be visible as a blocking review item.

Required representation:

- display/API field: `readOnlyIntegrationSeamUnwired: true`
- label: `Read-only seam unwired`
- severity: `blocking_review`
- helper copy: `The read-only seam exists only as a fail-closed review boundary. It is not wired to runtime completion or readiness.`
- readiness effect: `blocks_completion_ready`
- SourceTrace effect: `source_trace_entry_completed_false`
- instruction effect: `not_trade_instruction`

It must not be hidden in a generic warnings list or mapped to informational-only severity.

## Required Representation for Missing Fields and Unsafe Fields

Missing fields must be represented as blockers:

- display/API field: `missingFields`
- label: `Missing required evidence`
- severity: `blocking_review`
- helper copy: `Missing evidence prevents SourceTrace entry completion.`
- empty list behavior: must not imply completion; missing list absence must fail closed in any future skeleton

Unsafe fields must be represented as blockers:

- display/API field: `unsafeFields`
- label: `Unsafe evidence`
- severity: `blocking_review`
- helper copy: `Unsafe or runtime-like evidence prevents completion and requires review.`
- examples: latest-price-only, raw-kline-only, AI text, dashboard text, external, order, execution, BoundaryCandidateService `VALID`, ExecutionPlan readiness, runtime SourceTrace completion, trade-ready text

If future code cannot distinguish unsafe fields from missing fields, it must display the combined field list as blocking review evidence and remain fail closed.

## Required Representation for Review-Only and Non-Instructional Flags

Future display/API design must keep these flags visible:

```text
reviewMode=REVIEW_ONLY
manualReviewRequired=true
notTradeInstruction=true
sourceTraceEntryCompleted=false
completionReady=false
```

Required display copy:

- `Review only`
- `Manual review required`
- `Not a trade instruction`
- `SourceTrace entry not completed`
- `Completion not ready`

The display/API boundary must not transform these into badges or colors that imply success, readiness, or approval.

## Preventing Trade-Readiness Interpretation

The display/API design must prevent trade-readiness interpretation by requiring:

- no `ready` labels except the negative label `Completion not ready`
- no `valid` labels except in blocked explanatory text such as `BoundaryCandidateService VALID is not implied`
- no success color/status for seam output
- no order/position/action verbs
- no entry/stop/TP/RR values generated or displayed as actionable values
- no CTA copy such as `trade`, `execute`, `place`, `open`, `close`, or `reverse`
- explicit `notTradeInstruction=true`
- explicit `manualReviewRequired=true`

## Preventing BoundaryCandidateService VALID Interpretation

The display/API design must state:

- `BoundaryCandidateService VALID is not produced by this output.`
- `BoundaryCandidateService VALID remains unwired.`
- `Read-only review metadata cannot be used as boundary candidate validation.`

Any field or label that includes `VALID` must be forbidden unless it is part of a blocking unsafe-field explanation.

## Preventing ExecutionPlan Readiness Interpretation

The display/API design must state:

- `ExecutionPlan readiness is not produced by this output.`
- `ExecutionPlan readiness remains unwired.`
- `CompletionReady=false cannot be overridden by display/API representation.`

Any display/API field that suggests plan readiness, execution readiness, or action readiness is forbidden.

## Fixture-Only Examples

These examples are synthetic and fixture-only. They do not contain real entry, stop, take-profit, risk-reward, liquidity, event, wick, or order values.

### Missing Context Fixture

```json
{
  "symbol": "BTCUSDT",
  "timeframe": "15m",
  "completionStatus": "INCOMPLETE",
  "completionTransition": "NONE",
  "downgradeReason": "MISSING_REQUIRED_FIELD",
  "readOnlyIntegrationSeamUnwired": true,
  "missingFields": [
    "entryOwnershipValidationCompletionContext",
    "readOnlyIntegrationSeamUnwired"
  ],
  "reviewMode": "REVIEW_ONLY",
  "manualReviewRequired": true,
  "notTradeInstruction": true,
  "sourceTraceEntryCompleted": false,
  "completionReady": false,
  "displayLabel": "Incomplete - review only",
  "blockingCopy": "SourceTrace entry completion is not complete."
}
```

### Safe-Looking Read-Only Fixture

```json
{
  "symbol": "BTCUSDT",
  "timeframe": "15m",
  "completionStatus": "INCOMPLETE",
  "completionTransition": "NONE",
  "downgradeReason": "COMPLETION_UNWIRED",
  "readOnlyIntegrationSeamUnwired": true,
  "missingFields": [
    "readOnlyCompletionProductionPathUnwired",
    "entryPriceSource",
    "readOnlyIntegrationSeamUnwired"
  ],
  "reviewMode": "REVIEW_ONLY",
  "manualReviewRequired": true,
  "notTradeInstruction": true,
  "sourceTraceEntryCompleted": false,
  "completionReady": false,
  "displayLabel": "Completion path unwired",
  "blockingCopy": "The read-only seam is present, but completion wiring is not active."
}
```

### Unsafe Runtime-Like Fixture

```json
{
  "symbol": "BTCUSDT",
  "timeframe": "15m",
  "completionStatus": "INCOMPLETE",
  "completionTransition": "NONE",
  "downgradeReason": "UNSAFE_COMPLETION",
  "readOnlyIntegrationSeamUnwired": true,
  "unsafeFields": [
    "BOUNDARYCANDIDATESERVICE_VALID",
    "readOnlyIntegrationSeamUnwired"
  ],
  "reviewMode": "REVIEW_ONLY",
  "manualReviewRequired": true,
  "notTradeInstruction": true,
  "sourceTraceEntryCompleted": false,
  "completionReady": false,
  "displayLabel": "Unsafe completion evidence",
  "blockingCopy": "Unsafe or runtime-like evidence blocks completion and requires review."
}
```

## Required Tests Before Any Display/API Skeleton

Before any future display/API skeleton is accepted, tests must prove:

- display/API boundary presence alone does not make `sourceTraceEntryCompleted=true`
- display/API boundary presence alone does not make `completionReady=true`
- display/API boundary presence alone does not imply BoundaryCandidateService `VALID`
- display/API boundary presence alone does not imply ExecutionPlan readiness
- display/API boundary presence alone does not imply a trade instruction
- null seam output fails closed
- missing `readOnlyIntegrationSeamUnwired` fails closed
- missing review-only flags fail closed
- missing non-instructional flags fail closed
- missing `sourceTraceEntryCompleted=false` fails closed
- missing `completionReady=false` fails closed
- missing or empty missing-field lists do not imply completion
- unsafe fields remain visible as review-required blockers
- runtime-like source tags remain unsafe
- production-like tags remain unsafe
- complete safe-looking read-only seam output still displays as unwired / review-only
- `INCOMPLETE`, `NONE`, and `COMPLETION_UNWIRED` use required labels
- `MISSING_REQUIRED_FIELD` and `UNSAFE_COMPLETION` use required labels
- forbidden fields and labels are absent
- no real entry, stop, take-profit, or risk-reward values are displayed as generated values
- no order / execution / close / reverse / auto-trading / trade-ready method or field surface appears
- no Spring controller, service, repository, schema, dashboard, order, automation, or external data wiring is added unless separately authorized

## Display/API Java Skeleton Decision

Decision: a future display/API Java skeleton may start next only if it is a DTO/mapper skeleton with focused tests and no runtime wiring.

This authorization is limited to a future phase. P68 itself does not add Java. The next skeleton phase may define an inert DTO and/or mapper that converts an already-built fail-closed seam DTO into a read-only display/API DTO for tests only or internal review serialization. It must not create controllers, endpoints, dashboard wiring, schema persistence, readiness wiring, order paths, automation, or external integrations.

## Strict Next-Stage Scope

The next display/API skeleton stage may:

- add a minimal inert DTO for read-only human-review representation
- add a minimal mapper from already-built seam output into the display DTO
- preserve `INCOMPLETE`, `NONE`, `COMPLETION_UNWIRED`, `MISSING_REQUIRED_FIELD`, and `UNSAFE_COMPLETION`
- preserve `readOnlyIntegrationSeamUnwired`
- preserve missing/unsafe fields as blocking review evidence
- preserve `REVIEW_ONLY`
- preserve `manualReviewRequired=true`
- preserve `notTradeInstruction=true`
- preserve `sourceTraceEntryCompleted=false`
- preserve `completionReady=false`
- reject or downgrade missing safety flags
- add focused DTO/mapper tests
- add a result document

The next display/API skeleton stage must not:

- create a controller or endpoint
- modify `dashboard.html`
- modify schema
- register seam, assembler, display DTO, or mapper as Spring services
- wire seam or assembler into resolver, validation, readiness, dashboard, schema, order, automation, or external data paths
- implement production completion
- add a production adapter
- add `DefaultSourceTraceEntryOwnershipAdapter`
- add production `DefaultSourceTraceEntryCompletionContract`
- populate real SourceTrace fields in runtime
- complete full SourceTrace in runtime
- wire BoundaryCandidateService `VALID`
- upgrade ExecutionPlan readiness
- add external data integration
- add order API
- add auto-trading
- generate real entry, stop, take-profit, or risk-reward values

## Still-Blocked Production Paths

These remain blocked after P68:

- production completion implementation
- production entry ownership adapter
- `DefaultSourceTraceEntryOwnershipAdapter`
- production `DefaultSourceTraceEntryCompletionContract`
- Spring registration for the read-only seam
- Spring registration for the read-only assembler
- controller/API endpoint wiring
- dashboard wiring
- schema or database persistence wiring
- resolver wiring
- validation wiring
- readiness wiring
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

## Verification

Recommended verification for P68:

```bash
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

- Documentation-only.
- No Java modified in P68.
- No tests modified in P68.
- No display/API Java skeleton added in P68.
- `dashboard.html` is unchanged.
- Schema is unchanged.
- Config is unchanged.
- Production wiring is unchanged.
- No Spring service registration added.
- No resolver, validation, readiness, dashboard, schema, order, automation, or external data wiring added.
- No production completion implemented.
- No production adapter added.
- No `DefaultSourceTraceEntryOwnershipAdapter` added.
- No production `DefaultSourceTraceEntryCompletionContract` added.
- Runtime SourceTrace fields are not populated.
- Full SourceTrace is not completed in runtime.
- BoundaryCandidateService `VALID` production path remains unwired.
- ExecutionPlan readiness is not upgraded.
- External data integration, order API, and auto-trading are not added.
- Real entry, stop, take-profit, and risk-reward values are not generated.
- Placeholder `docs/P68.md` was removed.

## Risk Action Guard

- High risk does not directly mean close, reverse, or open.
- Wick / pin-bar evidence does not confirm trend reversal.
- Liquidity stress / stampede must block opportunity push and require review.
- Missing event data is not no event risk.
- Multi-timeframe agreement alone does not complete SourceTrace.
