# BACKEND-P106 Market Read-Only Candidate Generator Skeleton

## Baseline

- Branch context: PR #322 / Issue #321.
- Formal mainline title: BACKEND-P106 Market Read-Only Candidate Generator Skeleton.
- PR title note: PR #322 uses the shortened title `P106 ReadOnly Skeleton` as a platform workaround.
- Baseline commit: `e5a7617` (`chore: add P106 placeholder`), based on `dc91bed` (`P105 Market ReadOnly Design (#320)`).
- Scope: documentation-only market read-only candidate generator skeleton.
- Placeholder removed: `docs/P106.md`.

## Files Changed

- `docs/PHASE_BACKEND_P106_MARKET_READ_ONLY_CANDIDATE_GENERATOR_SKELETON.md`
- Removed `docs/P106.md`

No Java, test, schema, dashboard, config, controller, endpoint, runtime, adapter, readiness, order, execution, automation, or external-data files are changed.

## Skeleton-Only Scope

P106 defines the maximum safe skeleton for a future market read-only candidate generator.

The skeleton may accept already-ingested evidence snapshot fixtures or test-scope inputs and return review-only candidate output.

P106 does not:

- read runtime data
- read live market data
- fetch external data
- add candidate generator Java
- generate real entry / stop / TP / RR values
- wire BoundaryCandidateService `VALID` production path
- upgrade ExecutionPlan readiness
- mutate dashboard, schema, or config
- add controller or endpoint Java
- create order, execution, scheduler, automation, external-data, or auto-trading behavior
- modify production Java
- add test-scope Java

Documentation-only is the safest P106 implementation because it freezes the skeleton contract before any executable code exists.

## Skeleton Contract

The future skeleton shape is:

```text
already-ingested evidence snapshot
-> validate snapshot source ownership
-> validate numeric source ownership
-> validate source refs / rule versions / freshness ownership
-> validate source window
-> reject forbidden inputs
-> reject no-go evidence
-> preserve blocker evidence
-> return review-only candidate result
```

The skeleton is fail-closed. It must return `INCOMPLETE` or `BLOCKED` unless all required review-only evidence gates pass.

## Input Contract

Allowed input:

- already-ingested evidence snapshot only

The skeleton may accept:

- fixture evidence snapshot
- test-scope evidence snapshot
- already-ingested market structure evidence snapshot

The skeleton must not accept or perform:

- runtime fetch
- runtime data read
- live market fetch
- live market data read
- external API fetch
- order data read
- execution data read
- dashboard text scrape
- AI text as numeric candidate source

Required snapshot fields:

- `symbol`
- `timeframe`
- source owner fields
- numeric source fields
- source refs
- source timeframes
- source reasons
- source windows
- rule ids
- rule versions
- freshness ownership
- conflict family ownership
- data quality score or data quality ownership
- Risk Action Guard evidence
- no-go evidence flags

## Output Contract

Allowed output:

- review-only candidate result shape only

Required output flags:

- `manualReviewRequired=true`
- `notTradeInstruction=true`
- `reviewMode=REVIEW_ONLY`

The output must be:

- not production `VALID`
- not a trade instruction
- not readiness
- not an order signal
- not an execution signal
- not an automation signal
- not dashboard mutation
- not schema mutation
- not config mutation

The output must preserve:

- candidate status
- blocking reasons
- missing-field evidence
- forbidden-input evidence
- no-go evidence
- source owner evidence
- source ref evidence
- rule version evidence
- freshness ownership evidence
- conflict family evidence
- Risk Action Guard evidence

## Allowed Statuses

Allowed statuses:

- `INCOMPLETE`
- `BLOCKED`
- `REVIEW_ONLY_CANDIDATE`
- `FIXTURE_VALID_CANDIDATE` only if test-scope

No other status is authorized by P106.

### INCOMPLETE

Use `INCOMPLETE` when required evidence is missing, unevaluated, stale-but-not-unsafe, ambiguous, or incomplete.

`INCOMPLETE` must keep:

- `manualReviewRequired=true`
- `notTradeInstruction=true`
- `reviewMode=REVIEW_ONLY`

### BLOCKED

Use `BLOCKED` when forbidden input, blocked dependency, Risk Action Guard blocker, or no-go evidence is present.

`BLOCKED` must keep:

- `manualReviewRequired=true`
- `notTradeInstruction=true`
- `reviewMode=REVIEW_ONLY`

### REVIEW_ONLY_CANDIDATE

Use `REVIEW_ONLY_CANDIDATE` only when the evidence snapshot is complete enough to display candidate review context, but remains non-production.

`REVIEW_ONLY_CANDIDATE` must:

- keep `manualReviewRequired=true`
- keep `notTradeInstruction=true`
- keep `reviewMode=REVIEW_ONLY`
- preserve source ownership
- preserve numeric source ownership
- preserve rule version and freshness ownership
- not map to production `BoundaryStatusEnum.VALID`
- not call `BoundaryCandidateDTO.valid(...)`
- not wire BoundaryCandidateService `VALID`
- not upgrade ExecutionPlan readiness
- not generate real entry / stop / TP / RR values

### Test-Scope FIXTURE_VALID_CANDIDATE

`FIXTURE_VALID_CANDIDATE` may exist only in test-scope fixture work.

If introduced later, it must:

- remain test-scope only
- keep `manualReviewRequired=true`
- keep `notTradeInstruction=true`
- keep `reviewMode=REVIEW_ONLY`
- not map to production `BoundaryStatusEnum.VALID`
- not call `BoundaryCandidateDTO.valid(...)`
- not wire BoundaryCandidateService `VALID`
- not upgrade ExecutionPlan readiness
- not generate real entry / stop / TP / RR values

## Fail-Closed Requirements

The skeleton must fail closed as follows:

| Condition | Required status |
| --- | --- |
| Missing evidence snapshot | `INCOMPLETE` |
| Missing source owner | `INCOMPLETE` |
| Missing numeric source | `INCOMPLETE` |
| Missing source ref | `INCOMPLETE` |
| Missing rule version | `INCOMPLETE` |
| Missing freshness ownership | `INCOMPLETE` |
| Stale source window without unsafe evidence | `INCOMPLETE` |
| Stale source window with unsafe evidence | `BLOCKED` |
| Unsupported source family | `BLOCKED` |
| Any blocked P102 / P103 / P104 dependency | `BLOCKED` |
| Risk Action Guard blocker | `BLOCKED` |
| No-go evidence | `BLOCKED` |

Every fail-closed result must preserve specific blocker or missing-field evidence.

## Dependency Handling

The skeleton may reference the P102-P104 fixture line only as review-only dependency contracts.

Dependencies:

- P102 entry fixture dependency
- P103 stop fixture dependency
- P103 TP fixture dependency
- P103 RR fixture dependency
- P104 BoundaryCandidate numeric source assembler fixture dependency

Rules:

- Missing dependency -> `INCOMPLETE`
- Incomplete dependency -> `INCOMPLETE`
- Blocked dependency -> `BLOCKED`
- Dependency with forbidden input -> `BLOCKED`
- Dependency with no-go evidence -> `BLOCKED`

Dependency blockers must be preserved and must not be collapsed into a generic failure.

## Forbidden Inputs

The following inputs are forbidden:

- live market read
- runtime data read
- external data read
- latest price only
- single kline only
- AI text
- dashboard text
- aggregate score only
- order / execution backfill

Forbidden inputs force `BLOCKED` when present as candidate source evidence.

Forbidden inputs may be recorded as blocker evidence only. They must not become candidate ownership.

## No-Go Evidence

The following no-go evidence forces `BLOCKED`:

- liquidity stress / stampede
- missing event data
- wick / pin-bar overinterpretation
- multi-timeframe conflict
- high-risk without confirmation
- missing source owner
- missing numeric source
- stale source window with unsafe evidence
- unsupported source family
- forbidden source

No-go evidence must not be converted into:

- opportunity push
- reverse entry
- new position
- order intent
- execution intent
- readiness
- dashboard mutation
- trade-ready state

## Risk Action Guard Handling

Risk Action Guard evaluation remains review-only and completion-blocking when unsafe.

Required handling:

- Risk high but liquidity normal: may suggest reduce size / move stop / reduce leverage.
- Risk high and liquidity deteriorating: do not suggest one-shot market exit; prefer staged risk reduction / wait for liquidity recovery / reduce leverage.
- Risk high and stampede exists: forbid reverse, forbid new position, forbid opportunity push, protect principal first.
- Risk high but only short-term wick / pin-bar: do not treat as trend reversal; do not generate reverse entry; only warn and wait for confirmation.
- Missing event data cannot be treated as no risk.

Risk Action Guard output must not become an order instruction, execution instruction, automation instruction, or trade-ready signal.

## Strict Prohibitions

The skeleton must not:

- call `BoundaryCandidateDTO.valid(...)`
- map to production `BoundaryStatusEnum.VALID`
- wire BoundaryCandidateService `VALID`
- upgrade ExecutionPlan readiness
- generate real entry / stop / TP / RR values
- mutate dashboard, schema, or config
- add controller / endpoint Java
- create order, execution, scheduler, automation, external-data, or auto-trading behavior
- use latest price only as a candidate source
- use a single kline as a candidate source
- use AI text as a numeric source
- use dashboard text as a numeric source
- use aggregate score only as a numeric source
- use order / execution backfill as a numeric source

## Review-Only Result Shape

Future result shape:

- `candidateStatus`
- `symbol`
- `timeframe`
- `entryReview`
- `stopReview`
- `tpReview`
- `rrReview`
- `sourceOwnershipSummary`
- `numericSourceSummary`
- `freshnessOwnership`
- `sourceWindow`
- `ruleVersion`
- `conflictFamilyOwnership`
- `riskActionGuardReview`
- `manualReviewRequired=true`
- `notTradeInstruction=true`
- `reviewMode=REVIEW_ONLY`
- `blockingReasons`

The result shape must not expose:

- production valid status
- readiness
- trade-ready status
- order action
- execution action
- scheduler action
- automation action
- external data action
- auto-trading action
- generated real entry / stop / TP / RR values

## Still-Blocked Paths

The following paths remain blocked:

- runtime data reads
- live market data reads
- external data fetches
- production candidate generator Java
- real entry / stop / TP / RR value generation
- BoundaryCandidateService `VALID` production path
- ExecutionPlan readiness upgrade
- dashboard mutation
- `dashboard.html` changes
- schema changes
- config changes
- controller / endpoint Java
- external data integration
- order API
- execution API
- scheduler / automation / auto-trading
- production ownership review wiring
- production completion
- production adapter
- `DefaultSourceTraceEntryOwnershipAdapter`
- production `DefaultSourceTraceEntryCompletionContract`
- runtime SourceTrace field population
- full SourceTrace runtime completion
- production Java changes

## Boundary Confirmations

- P106 is skeleton documentation only.
- P106 does not add test-scope Java.
- P106 does not add production Java.
- P106 does not read runtime data.
- P106 does not read live market data.
- P106 does not fetch external data.
- P106 does not generate real entry / stop / TP / RR values.
- P106 does not wire BoundaryCandidateService `VALID` production path.
- P106 does not upgrade ExecutionPlan readiness.
- P106 does not mutate dashboard, schema, or config.
- P106 does not modify `dashboard.html`.
- P106 does not add controller/endpoint Java.
- P106 does not add external data integration.
- P106 does not add order API.
- P106 does not add execution API.
- P106 does not add scheduler / automation / auto-trading.
- P106 does not create order, execution, scheduler, automation, external-data, or auto-trading behavior.
- Placeholder `docs/P106.md` is removed.

## Validation

P106 changes documentation only. No Java or test source was modified, so Maven was not required for this phase.

Required validation:

```text
git diff --check
```
