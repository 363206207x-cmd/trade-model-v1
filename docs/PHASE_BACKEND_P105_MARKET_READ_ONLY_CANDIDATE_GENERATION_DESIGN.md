# BACKEND-P105 Market Read-Only Candidate Generation Design

## Baseline

- Branch context: PR #320 / Issue #319.
- Formal mainline title: BACKEND-P105 Market Read-Only Candidate Generation Design.
- PR title note: PR #320 uses the shortened title `P105 Market ReadOnly Design` as a platform workaround.
- Baseline commit: `46648ea` (`chore: add P105 placeholder`), based on `dafa0f8` (`P104 Fixture Assembler (#318)`).
- Scope: documentation-only market read-only candidate generation design.
- Placeholder removed: `docs/P105.md`.

## Files Changed

- `docs/PHASE_BACKEND_P105_MARKET_READ_ONLY_CANDIDATE_GENERATION_DESIGN.md`
- Removed `docs/P105.md`

No Java, test, schema, dashboard, config, controller, endpoint, runtime, adapter, readiness, order, execution, automation, or external-data files are changed.

## Design-Only Scope

P105 defines how a future market read-only candidate generator may use already-ingested evidence to produce review-only candidate outputs in a later phase.

P105 does not:

- read runtime data
- read live market data
- add candidate generator Java
- generate real entry / stop / TP / RR values
- wire BoundaryCandidateService `VALID` production path
- upgrade ExecutionPlan readiness
- mutate dashboard, schema, or config
- add controller or endpoint Java
- add external data integration
- add order API
- add execution API
- add scheduler / automation / auto-trading

Any future output must keep:

- `manualReviewRequired=true`
- `notTradeInstruction=true`
- `reviewMode=REVIEW_ONLY`

## Permitted Future Read-Only Market Inputs

A future market read-only candidate generator may use only already-ingested evidence snapshots.

Permitted future read-only inputs:

- already-ingested market structure evidence
- kline-derived structure evidence
- ATR / volatility evidence
- liquidity target evidence
- prior high / prior low evidence
- source windows
- freshness status
- rule versions
- data quality score

These inputs must be provided as an evidence snapshot. The generator must not fetch, subscribe, poll, or refresh market data.

Each permitted input must carry:

- source owner
- source reference
- source timeframe
- source reason
- source window
- rule id
- rule version
- freshness ownership
- conflict family ownership
- data quality ownership, when applicable

## Forbidden Direct Inputs

The following must not directly feed candidate generation:

- live market API reads inside candidate generator
- latest price only
- single kline only
- AI text
- dashboard text
- aggregate score only
- order / execution backfill

Forbidden inputs may be preserved as blocker evidence. They must not become source ownership for entry, stop, TP, or RR.

## Read-Only Candidate Flow

The future read-only flow is:

```text
evidence snapshot
-> source ownership extraction
-> entry / stop / TP / RR source-owned candidate precheck
-> freshness / source window / rule version check
-> conflict family validation
-> Risk Action Guard
-> review-only candidate output
```

The flow is read-only because it may assemble evidence into candidate review context only. It must not mutate runtime state, SourceTrace runtime fields, readiness, dashboard state, schema, config, order state, execution state, scheduler state, automation state, or external-data state.

## Evidence Snapshot Requirements

The evidence snapshot must be immutable within a generator run.

Required snapshot fields:

- `symbol`
- `timeframe`
- market structure evidence refs
- kline-derived structure evidence refs
- ATR / volatility evidence refs
- liquidity target evidence refs
- prior high / prior low evidence refs
- source window start
- source window end
- source window timeframe
- rule id
- rule version
- freshness status
- data quality score
- data quality source
- event evidence status
- wick / pin-bar evidence status
- multi-timeframe evidence status
- Risk Action Guard context

If any required source owner or source reference is absent, the future generator must return `INCOMPLETE` or `BLOCKED` according to the rules below.

## Source Ownership Extraction

Source ownership extraction maps evidence snapshot fields into the source-owned candidate model from P101-P104.

Extraction must produce:

- entry source owner
- stop source owner
- TP source owner
- RR source owner
- source refs
- source timeframes
- source reasons
- source windows
- rule ids
- rule versions
- freshness ownership
- conflict family ownership
- fixture/read-only numeric source envelopes

Extraction must not:

- infer missing source owner from AI text
- infer missing source owner from dashboard text
- infer missing numeric source from latest price
- infer missing numeric source from aggregate score
- backfill ownership from order or execution records

## Entry Candidate Generation Preconditions

Entry candidate generation may proceed only when all preconditions are true:

- evidence snapshot is present
- entry source owner is present
- entry source family is supported
- entry source ref is present and unambiguous
- entry source timeframe is present and compatible
- entry source reason is non-instructional
- source window is present and fresh
- rule id is present
- rule version is present and current
- freshness ownership is present and passing
- conflict family ownership is present and passing
- numeric source envelope is present and read-only / fixture-only
- no forbidden direct input is used
- no Risk Action Guard blocker is present

Supported entry families remain:

- structure confirmation zone
- breakout retest zone
- support/resistance flip zone

The future generator must not generate a real entry value in P105 or by implication from P105.

## Stop Candidate Generation Preconditions

Stop candidate generation may proceed only when all preconditions are true:

- P102-style entry dependency is present and not blocked
- stop source owner is present
- stop source family is `STRUCTURAL_INVALIDATION_WITH_BUFFER`
- structural invalidation source ref is present
- ATR / volatility buffer source owner is present
- ATR / volatility buffer source ref is present
- stop source timeframe is present and compatible
- stop source reason is non-instructional
- source window is present and fresh
- rule id is present
- rule version is present and current
- freshness ownership is present and passing
- conflict family ownership is present and passing
- numeric source envelope is present and read-only / fixture-only
- no entry-stop inversion exists
- no forbidden direct input is used
- no Risk Action Guard blocker is present

The future generator must not generate a real stop value in P105 or by implication from P105.

## TP Candidate Generation Preconditions

TP candidate generation may proceed only when all preconditions are true:

- P102-style entry dependency is present and not blocked
- P103-style stop dependency is present and not blocked
- TP source owner is present
- TP source family is supported
- TP source ref is present and unambiguous
- TP source timeframe is present and compatible
- TP source reason is non-instructional
- source window is present and fresh
- rule id is present
- rule version is present and current
- freshness ownership is present and passing
- conflict family ownership is present and passing
- numeric source envelope is present and read-only / fixture-only
- no entry-TP direction conflict exists
- no stop-TP overlap exists
- no forbidden direct input is used
- no Risk Action Guard blocker is present

Supported TP families remain:

- structure target
- liquidity target
- prior high / prior low
- RR ladder

The future generator must not generate real TP values in P105 or by implication from P105.

## RR Candidate Generation Preconditions

RR candidate generation may proceed only when all preconditions are true:

- P102-style entry dependency is present and not blocked
- P103-style stop dependency is present and not blocked
- P103-style TP dependency is present and not blocked
- RR source owner is present
- RR source ref is present and unambiguous
- RR source timeframe is present and compatible
- RR source reason is non-instructional
- source window is present and fresh
- rule id is present
- rule version is present and current
- freshness ownership is present and passing
- conflict family ownership is present and passing
- fixture/read-only entry-stop distance evidence is present
- entry-stop distance is not missing, zero, negative, ambiguous, stale, or unsupported
- TP ownership is present
- no forbidden direct input is used
- no Risk Action Guard blocker is present

RR must link entry, stop, and TP. It must not become a standalone signal.

The future generator must not generate real RR values in P105 or by implication from P105.

## INCOMPLETE Conditions

The future generator must return `INCOMPLETE` when evidence is missing, unevaluated, stale-but-not-forbidden, or incomplete.

`INCOMPLETE` conditions:

- missing evidence snapshot
- missing source owner
- missing numeric source
- missing source ref
- missing source timeframe
- missing source reason
- missing source window
- stale source window without a known unsafe source
- missing rule id
- missing rule version
- stale rule version without a known unsafe source
- missing freshness ownership
- missing conflict family ownership
- missing data quality score
- unevaluated event evidence
- unevaluated liquidity evidence
- unevaluated wick / pin-bar evidence
- unevaluated multi-timeframe evidence
- missing P102 entry dependency for downstream generation
- missing P103 stop dependency for TP or RR generation
- missing P103 TP dependency for RR generation

`INCOMPLETE` output must preserve missing-field evidence and must keep:

- `manualReviewRequired=true`
- `notTradeInstruction=true`
- `reviewMode=REVIEW_ONLY`

## BLOCKED Conditions

The future generator must return `BLOCKED` when evidence is unsafe, forbidden, contradictory, or explicitly blocked.

`BLOCKED` conditions:

- forbidden direct input is used
- unsupported source family
- stale source window with unsafe or contradictory evidence
- entry-stop inversion
- entry-TP direction conflict
- stop-TP overlap
- liquidity stress / stampede opportunity push
- missing event data treated as no risk
- wick / pin-bar overinterpretation
- multi-timeframe conflict
- high-risk without confirmation
- Risk Action Guard blocker
- order / execution backfill
- AI text directly generating numeric values
- dashboard text directly generating numeric values
- latest price only
- single kline only
- aggregate score only

`BLOCKED` output must preserve blocker evidence and must keep:

- `manualReviewRequired=true`
- `notTradeInstruction=true`
- `reviewMode=REVIEW_ONLY`

## No-Go Conditions

No-go conditions prevent candidate generation from becoming fixture-valid or display-ready.

No-go conditions:

- liquidity stress / stampede
- missing event data
- wick / pin-bar overinterpretation
- multi-timeframe conflict
- high-risk without confirmation
- missing source owner
- missing numeric source
- stale source window
- unsupported source family
- forbidden source

No-go evidence must be preserved as blocker evidence. No-go evidence must not be converted into opportunity, reverse entry, new position, order intent, execution intent, or readiness.

## Risk Action Guard Handling

Risk Action Guard runs after source ownership, freshness, source window, rule version, and conflict family validation.

Risk Action Guard rules:

- Risk high but liquidity normal: may suggest reduce size / move stop / reduce leverage.
- Risk high and liquidity deteriorating: do not suggest one-shot market exit; prefer staged risk reduction / wait for liquidity recovery / reduce leverage.
- Risk high and stampede exists: forbid reverse, forbid new position, forbid opportunity push, protect principal first.
- Risk high but only short-term wick / pin-bar: do not treat as trend reversal; do not generate reverse entry; only warn and wait for confirmation.
- Missing event data cannot be treated as no risk.

Risk Action Guard output remains review-only. It must not become an order instruction, execution instruction, automation instruction, or trade-ready signal.

## Data Quality And Stale-Data Handling

Data quality score is context, not authorization.

Rules:

- missing data quality score -> `INCOMPLETE`
- stale data quality score -> `INCOMPLETE`
- low data quality score -> `INCOMPLETE` or `BLOCKED` depending on blocker evidence
- high data quality score alone cannot produce a candidate
- data quality score cannot override missing source ownership
- data quality score cannot override forbidden direct inputs
- data quality score cannot override Risk Action Guard blockers

Stale-data handling:

- stale source window -> `INCOMPLETE` when no unsafe source is known
- stale source window plus unsafe evidence -> `BLOCKED`
- stale rule version -> `INCOMPLETE` when no unsafe source is known
- stale rule version plus contradictory evidence -> `BLOCKED`
- stale freshness ownership -> `INCOMPLETE` or `BLOCKED` according to blocker evidence

## Review-Only Output Shape

Future output may use a `BoundaryCandidateDTO`-style shape only as review context.

Review-only output fields:

- `candidateGenerationStatus`
- `symbol`
- `timeframe`
- entry candidate review fields
- stop candidate review fields
- TP ladder review fields
- RR review fields
- source ownership summary
- source refs
- source windows
- rule ids
- rule versions
- freshness ownership
- conflict family ownership
- data quality score
- Risk Action Guard review message
- `manualReviewRequired=true`
- `notTradeInstruction=true`
- `reviewMode=REVIEW_ONLY`
- `blockingReasons`

The output must not:

- map to production `BoundaryStatusEnum.VALID`
- call `BoundaryCandidateDTO.valid(...)`
- wire BoundaryCandidateService `VALID`
- upgrade ExecutionPlan readiness
- mutate dashboard, schema, or config
- create order, execution, scheduler, automation, external-data, or auto-trading behavior
- generate real entry / stop / TP / RR values

## Source Trace Requirements

Future read-only output must preserve source trace requirements:

- entry source owner, type, timeframe, reason, and ref
- stop source owner, type, timeframe, reason, and ref
- TP source owner, type, timeframe, reason, and ref
- RR source owner, rule id, rule version, reason, and ref
- source window start / end / timeframe
- freshness status
- rule version status
- data quality source
- liquidity source
- multi-timeframe source
- event source
- wick / pin-bar source
- conflict family evidence
- Risk Action Guard evidence

Missing source trace requirements keep the output `INCOMPLETE` or `BLOCKED`.

## Future P106 Skeleton Constraints

P106 may define a market read-only candidate generator skeleton only if it remains read-only.

P106 skeleton constraints:

- no runtime data reads
- no live market data reads
- no external data integration
- no order API
- no execution API
- no scheduler / automation / auto-trading
- no dashboard mutation
- no schema mutation
- no config mutation
- no controller / endpoint Java
- no BoundaryCandidateService `VALID` production path
- no ExecutionPlan readiness upgrade
- no real entry / stop / TP / RR generation
- no trade instruction output
- keep `manualReviewRequired=true`
- keep `notTradeInstruction=true`
- keep `reviewMode=REVIEW_ONLY`

P106 must fail closed on missing source ownership, missing numeric source, stale source windows, unsupported families, forbidden sources, no-go evidence, or Risk Action Guard blockers.

## Still-Blocked Paths

The following paths remain blocked:

- runtime data reads
- live market data reads
- candidate generator Java in P105
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

- P105 is design-only.
- P105 does not read runtime data.
- P105 does not read live market data.
- P105 does not add candidate generator Java.
- P105 does not modify production Java.
- P105 does not modify test source.
- P105 does not generate real entry / stop / TP / RR values.
- P105 does not wire BoundaryCandidateService `VALID` production path.
- P105 does not upgrade ExecutionPlan readiness.
- P105 does not modify `dashboard.html`.
- P105 does not modify schema.
- P105 does not modify config.
- P105 does not add controller/endpoint Java.
- P105 does not add external data integration.
- P105 does not add order API.
- P105 does not add execution API.
- P105 does not add scheduler / automation / auto-trading.
- P105 keeps `manualReviewRequired=true` and `notTradeInstruction=true` mandatory for future outputs.
- Placeholder `docs/P105.md` is removed.

## Validation

P105 changes documentation only. No Java or test source was modified, so Maven was not required for this phase.

Required validation:

```text
git diff --check
```
