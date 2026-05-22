# BACKEND-P142 Source-Owned Candidate Input Contract

## Baseline

- Branch context: PR #397 / Issue #396.
- Formal mainline title: BACKEND-P142 Source-Owned Candidate Input Contract.
- PR title note: PR #397 uses a shortened title as a platform workaround; Issue #396 and this document preserve the formal mainline title.
- Baseline commit: `0f185e6` (`P141 SourceTrace Gap Audit (#395)`).
- Scope: documentation-only source-owned candidate input contract.
- Line context: P142 continues the Production Wiring Preparation Line.
- Placeholder removed: `docs/P142.md`.

## P141 Audit Recap

P141 confirmed:

- runtime SourceTrace population remains missing
- source-owned candidate generation remains missing
- real entry / stop / TP / RR generation remains blocked
- BoundaryCandidateService production `VALID` path remains blocked
- ExecutionPlan readiness beyond review-only remains blocked

P142 responds to that audit by defining an input contract only. It does not design implementation and does not authorize implementation.

## Files Changed

- `docs/PHASE_BACKEND_P142_SOURCE_OWNED_CANDIDATE_INPUT_CONTRACT.md`
- Removed `docs/P142.md`

No Java, test source, `dashboard.html`, controller, endpoint, API, schema, config, service, mapper, runtime data reader, live market data reader, external data integration, readiness, order, execution, scheduler, automation, or auto-trading files are changed.

## Contract Purpose

P142 defines the exact evidence that a future source-owned candidate generation design must require before any candidate can leave `INCOMPLETE`.

This contract is conceptual. It does not add a Java class, DTO, schema, endpoint, API, service, mapper, dashboard field, runtime reader, or production adapter. Names in this document are contract names for future planning. They are not implementation names unless a later issue separately authorizes exact files and code changes.

P142 does not authorize:

- production implementation
- source-owned runtime candidate generation
- runtime SourceTrace field population
- real entry / stop / TP / RR value generation
- BoundaryCandidateService production `VALID` path
- `BoundaryCandidateDTO.valid(...)` calls in new production flows
- production `BoundaryStatusEnum.VALID` mapping
- ExecutionPlan readiness upgrade
- dashboard readiness mutation
- controller / endpoint / API / schema / config / service / mapper changes
- runtime/live/external data reads
- order / execution / scheduler / automation / auto-trading

## Conceptual Input Object

Future work may define a source-owned candidate input object conceptually as:

```text
SourceOwnedCandidateInput
  entryEvidence
  stopEvidence
  takeProfitEvidence
  riskRewardEvidence
  liquidityEvidence
  multiTimeframeEvidence
  eventEvidence
  wickEvidence
  ohlcvKlineEvidence
  dataQualityEvidence
  sourceTraceAudit
  riskActionGuardEvidence
```

This object is a contract shape only. It must not be created in Java by P142.

The conceptual object exists to answer one question before any future candidate generation design begins:

```text
Does every required source family have owner, ref, timeframe, window, freshness, rule, reason, conflict, quality, and audit evidence?
```

If the answer is no, the future candidate must remain `INCOMPLETE` or `BLOCKED` according to the rules below.

## Universal Evidence Fields

Every input family must carry the following fields before it can be considered complete:

| Field | Required Meaning |
| --- | --- |
| `owner` | Deterministic source owner. It must identify the producing rule, adapter, persisted source, or reviewed source family. |
| `sourceRef` | Stable reference to the source record, rule output, adapter output, or reviewed artifact. |
| `sourceTimeframe` | Timeframe or observation timeframe for the evidence. |
| `sourceWindow` | Source window start/end or equivalent bounded observation window. |
| `freshness` | Explicit freshness status and observed-time relationship to the decision time. |
| `ruleId` | Stable rule identifier for generated, interpreted, or selected evidence. |
| `ruleVersion` | Rule version used by the evidence. |
| `reason` | Review-only reason explaining why the evidence is eligible or why it blocks. |
| `conflictState` | Explicit no-conflict, conflict, unknown, or not-evaluated state for the family. |

Missing any universal field keeps the future candidate out of `VALID` and out of readiness.

## Required Input Families

### Entry

Entry evidence must define:

- entry numeric boundary value owner
- entry source ref
- entry source timeframe
- entry source window
- entry freshness status
- entry rule id
- entry rule version
- entry source reason
- entry conflict state

Latest price alone is not an entry source. RuntimeKline `latestPrice`, quote latest price, dashboard text, API text, display text, AI text, order data, or execution data must not populate entry ownership.

### Stop

Stop evidence must define:

- stop numeric boundary value owner
- stop source ref
- stop source timeframe
- stop source window
- stop freshness status
- stop rule id
- stop rule version
- stop source reason
- stop conflict state

Stop must be independently source-owned. It must not be inferred only from entry, latest price, raw kline presence, dashboard text, AI text, order data, or execution data.

### Take-Profit

Take-profit evidence must define:

- TP numeric boundary value owner for every TP level
- TP source ref for every source family or level
- TP source timeframe
- TP source window
- TP freshness status
- TP rule id
- TP rule version
- TP source reason
- TP conflict state

TP values must preserve ordering and source intent. Mixed TP source ownership must be explicit and reviewed. A non-empty TP list alone is not enough.

### Risk-Reward

Risk-reward evidence must define:

- RR numeric value owner
- RR source ref or source bundle ref
- RR source timeframe or source bundle timeframe
- RR source window
- RR freshness status
- RR rule id
- RR rule version
- RR source reason
- RR conflict state
- RR rule ref

RR must be computed from owned entry, stop, and TP sources. RR must not be copied from display text, AI text, latest price, raw kline values, or fixture-only tokens in production.

### Liquidity

Liquidity evidence must define:

- liquidity owner
- liquidity source ref
- liquidity source timeframe or observation window
- liquidity source window
- liquidity freshness status
- liquidity rule id
- liquidity rule version
- liquidity reason
- liquidity conflict state

Deteriorating liquidity and liquidity stress must not become opportunity, one-shot exit instruction, reverse instruction, or new-position prompt.

### Multi-Timeframe

Multi-timeframe evidence must define:

- multi-timeframe owner
- participating timeframe refs
- source timeframe set
- aggregation window
- freshness status per participating timeframe
- aggregation rule id
- aggregation rule version
- aggregation reason
- convergence/conflict state

Multi-timeframe agreement alone does not complete SourceTrace and does not imply readiness. Missing or conflicting participating timeframes keep the candidate fail-closed.

### Event

Event evidence must define:

- event owner
- event source ref
- event observation timeframe
- event source window
- event freshness status
- event rule id
- event rule version
- event reason
- event conflict or blocker state

Missing event evidence must not display as no risk. Unknown event state remains `INCOMPLETE` unless no-go evidence requires `BLOCKED`.

### Wick

Wick evidence must define:

- wick owner
- wick source ref
- wick source timeframe
- wick source window
- wick freshness status
- wick rule id
- wick rule version
- wick reason
- wick conflict state

Wick-only evidence must not become trend reversal. It must wait for confirmation from source-owned, fresh, non-conflicting evidence.

### OHLCV / Kline

OHLCV / kline evidence must define:

- persisted source owner
- persisted source ref or batch/source trace id
- symbol and timeframe
- required closed-bar window
- latest close time and ingestion time
- freshness status
- stale reason code
- missing fields
- quality status
- rule id
- rule version
- reason
- continuity/conflict state

Raw kline presence alone is not an entry / stop / TP / RR source. Kline data can be input evidence only after source ownership, freshness, quality, and continuity are complete.

### Data Quality

Data quality evidence must define:

- data quality owner
- data quality source ref
- data quality source timeframe or input window
- data quality source window
- data quality freshness status
- data quality rule id
- data quality rule version
- data quality reason
- data quality conflict state
- numeric data quality score

Data quality score must not be copied from unrelated decision scores, dashboard text, API text, AI text, or external feeds without a source ownership contract.

### SourceTrace Audit

SourceTrace audit evidence must define:

- audit owner
- audit source refs for every required family
- audit source timeframe coverage
- audit source windows
- audit freshness summary
- audit rule id / rule version coverage
- audit reasons
- audit conflict summary
- missing evidence summary
- blocked evidence summary
- rollback-safe evidence trail
- `manualReviewRequired=true`
- `notTradeInstruction=true`
- `reviewMode=REVIEW_ONLY`

Runtime SourceTrace fields must be populated only from source-owned evidence. If runtime SourceTrace is absent, partial, stale, unaudited, or derived from display/API/AI text, the future candidate must remain `INCOMPLETE` or `BLOCKED`.

## Numeric Source Ownership Requirements

Every numeric boundary value must have:

- owner
- source ref
- source timeframe
- source window
- freshness status
- rule id
- rule version
- source reason
- conflict state
- audit trail

Numeric requirements by family:

| Family | Numeric Requirement |
| --- | --- |
| Entry | `entryPriceSource` or future equivalent must be source-owned, fresh, referenced, reasoned, versioned, and non-conflicting. |
| Stop | `stopPriceSource` or future equivalent must be independently source-owned and must not be inferred from entry alone. |
| TP | Each `tpPriceSources` value or future equivalent must be source-owned, ordered, referenced, and traceable to a TP rule/version. |
| RR | `rrSource` or future equivalent must be computed from owned entry, stop, and TP sources and must carry `rrRuleRef` or future equivalent rule evidence. |

Forbidden numeric substitutions:

- latest price alone as entry
- raw kline item presence as entry / stop / TP / RR
- AI text as owner or reason source
- dashboard text as source ownership
- API/display text as source ownership
- order or execution state as source ownership
- external data without a source ownership contract
- fixture-only numeric tokens in production

## Completeness Rules

A future source-owned candidate may leave `INCOMPLETE` only when all of the following are true:

- every required input family is present
- every required input family has owner, source ref, timeframe, window, freshness, rule id, rule version, reason, and conflict state
- every numeric boundary value is source-owned and audited
- SourceTrace audit is complete
- runtime SourceTrace fields are populated from source-owned evidence
- data quality score and owner are complete
- persisted OHLCV / kline readiness metadata is complete
- no required freshness state is stale, unknown, future, or clock-inverted
- no conflict family state is missing or blocking
- no no-go evidence exists
- no forbidden input exists
- Risk Action Guard does not block
- `manualReviewRequired=true`
- `notTradeInstruction=true`
- `reviewMode=REVIEW_ONLY`
- rollback-safe evidence trail is complete

Passing these completeness rules would only make a future candidate eligible for a later separately authorized design. It would not itself authorize production `VALID`, ExecutionPlan readiness, dashboard readiness, order, execution, scheduler, automation, or auto-trading.

## INCOMPLETE Rules

Future candidate output must remain `INCOMPLETE` when:

- source owner is missing
- source ref is missing
- source timeframe is missing
- source window is missing
- observed time is missing
- decision time relationship is missing
- freshness is missing
- source is stale without known no-go evidence
- OHLCV / kline context is missing
- persisted OHLCV readiness metadata is missing
- data quality score is missing
- data quality score owner is missing
- evidence completeness is insufficient
- SourceTrace is incomplete
- numeric source ownership is incomplete
- entry source reason is missing
- stop source reason is missing
- TP source reason is missing
- RR rule ref is missing
- rule id is missing
- rule version is missing
- conflict family state is missing
- liquidity evidence is missing
- multi-timeframe evidence is missing
- event evidence status is missing
- wick evidence status is missing
- rollback-safe evidence trail is missing
- runtime SourceTrace field is not populated from source-owned evidence

`INCOMPLETE` remains missing-evidence context only. It is not production `VALID`, not readiness, not executable state, not a trade instruction, and not an order or execution surface.

## BLOCKED Rules

Future candidate output must remain `BLOCKED` when:

- forbidden input is present
- no-go evidence exists
- Risk Action Guard blocks action
- stampede condition exists
- deteriorating liquidity makes direct action unsafe
- wick-only evidence is being misread as trend reversal
- missing event evidence is being treated as no risk
- liquidity stress is being treated as opportunity
- stale source window appears with unsafe or contradictory evidence
- unsupported source owner is used
- unsupported source type is used
- source ref is duplicated, ambiguous, or fabricated
- dashboard/API/display text is used as source ownership
- AI text is used as source ownership
- latest price is used as entry source by itself
- raw kline item presence is used as entry / stop / TP / RR source by itself
- external data appears without a source ownership contract
- order / execution / automation surface appears

`BLOCKED` remains no-go / forbidden / Risk Action Guard blocked context only. It is not production `VALID`, not readiness, not executable state, not a trade instruction, and not an order or execution path.

## Risk Action Guard Requirements

Risk Action Guard evidence must be evaluated after source ownership, freshness, source window, rule version, conflict state, OHLCV/kline, and data quality evidence are known.

Risk Action Guard requirements:

- stampede state is explicit
- wick-only state is explicit
- liquidity state is explicit
- deteriorating liquidity state is explicit
- missing event evidence state is explicit
- high-risk state is explicit
- blocking reason is explicit
- action flags remain false unless separately authorized, and P142 does not authorize that
- output remains `manualReviewRequired=true`
- output remains `notTradeInstruction=true`
- output remains `reviewMode=REVIEW_ONLY`

Risk Action Guard boundaries:

- Stampede must not become reverse / new-position / opportunity-push display.
- Wick-only must not become trend reversal.
- Deteriorating liquidity must not become one-shot market exit instruction.
- Missing event evidence must not display as no risk.
- Liquidity stress must not display as opportunity.
- High risk alone must not mean direct stop loss, reverse, or new position.
- Risk high with normal liquidity may support review-only risk reduction context, not automatic action.
- Risk high with deteriorating liquidity must avoid one-shot market exit instruction.
- Risk high with stampede must block reverse / new position / opportunity push.
- Risk high with wick-only evidence must wait for confirmation.

## Validation Expectations For Future Implementation

Any future implementation proposal based on this contract must be separately authorized and must define focused validation before implementation begins.

Future validation should include:

- source-owned input contract tests
- one test per required input family
- numeric source ownership tests for entry, stop, TP, and RR
- freshness and source-window fail-closed tests
- rule id / rule version fail-closed tests
- source ref duplicate/ambiguous/fabricated blocked tests
- latest-price-only substitution blocked tests
- raw-kline-only substitution blocked tests
- dashboard/API/display text substitution blocked tests
- AI text substitution blocked tests
- external data without ownership contract blocked tests
- missing event evidence incomplete tests
- liquidity stress blocked tests
- stampede blocked tests
- wick-only confirmation tests
- Risk Action Guard action-surface blocked tests
- SourceTrace audit completeness tests
- no production `VALID` mapping guard tests unless separately authorized
- no ExecutionPlan readiness upgrade guard tests unless separately authorized
- no order / execution / scheduler / automation / auto-trading guard tests
- rollback validation proving output returns to `INCOMPLETE` or `BLOCKED`

Future implementation validation must also include normal compile/test checks and `git diff --check`, but P142 itself does not add or run Java tests.

## Recommended Next Step

Recommended next step after P142 is STOP unless a separately authorized issue exists.

If work continues, the safest next line is a documentation-only source-owned candidate design matrix. That future matrix should map each conceptual input family to existing fields, missing fields, required tests, rollback behavior, and still-blocked implementation paths.

P142 does not authorize that future matrix. P142 does not authorize Java implementation.

## Still-Blocked Paths

The following paths remain blocked after P142:

- production candidate generation
- source-owned runtime candidate generation
- real entry / stop / TP / RR value generation
- runtime data reads
- live market data reads
- external data fetches
- external data integration
- exchange clients
- `WebClient`
- `RestTemplate`
- production `VALID` mapping
- BoundaryCandidateService `VALID` production path
- `BoundaryCandidateDTO.valid(...)` calls in new production flows
- production `BoundaryStatusEnum.VALID` mapping
- ExecutionPlan readiness upgrade
- dashboard readiness mutation
- dashboard implementation beyond P130 static skeleton
- `dashboard.html` changes beyond P130 static skeleton
- dashboard UI code beyond P130 static skeleton
- controller / endpoint Java
- API wiring
- schema changes
- config changes
- service registration
- Spring bean registration
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

## Rollback Expectations

Rollback for P142 is limited to:

- remove `docs/PHASE_BACKEND_P142_SOURCE_OWNED_CANDIDATE_INPUT_CONTRACT.md`
- restore `docs/P142.md` only if the PR is abandoned before merge

Rollback must not touch production Java, test source, `dashboard.html`, controller, endpoint, API, schema, config, service, mapper, runtime data, live market data, external data, readiness, order, execution, scheduler, automation, or auto-trading paths.

If a future change uses P142 to widen scope without authorization, rollback must restore the last approved P141 SourceTrace runtime gap audit and keep all still-blocked paths blocked.

## Boundary Confirmations

- P142 is documentation-only input contract work.
- P142 removes the placeholder `docs/P142.md`.
- P142 adds one source-owned candidate input contract document.
- P142 does not modify production Java.
- P142 does not modify test source.
- P142 does not modify `dashboard.html`.
- P142 does not add dashboard UI code.
- P142 does not add controller / endpoint / API / schema / config / service / mapper changes.
- P142 does not read runtime data.
- P142 does not read live market data.
- P142 does not fetch external data.
- P142 does not generate real entry / stop / TP / RR values.
- P142 does not upgrade ExecutionPlan readiness.
- P142 does not map to production `VALID`.
- P142 does not wire BoundaryCandidateService `VALID` production path.
- P142 does not call `BoundaryCandidateDTO.valid(...)`.
- P142 does not add order API.
- P142 does not add execution API.
- P142 does not add scheduler / automation / auto-trading.
- P142 does not authorize production implementation.
- P142 does not merge the PR.

## Validation

P142 is documentation-only, so Maven may be skipped because no Java or test source is modified. Validation is limited to:

```text
git diff --check
git diff --cached --check
```

## PR Body Checklist

The PR body must include:

- files changed
- validation performed
- contract coverage
- required input families
- numeric source ownership requirements
- completeness rules
- `INCOMPLETE` rules
- `BLOCKED` rules
- Risk Action Guard boundaries
- validation expectations
- rollback expectations
- recommended next step
- still-blocked paths
- boundary confirmations
- note that the PR short title is only a platform workaround; formal mainline is Issue #396 / BACKEND-P142

P142 stops here. It does not merge the PR.
