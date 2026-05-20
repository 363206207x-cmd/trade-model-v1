# BACKEND-P101 Entry Stop TP Source-Owned Candidate Generation Design

## Baseline

- Branch context: PR #312 / Issue #311.
- Formal mainline title: BACKEND-P101 Entry Stop TP Source-Owned Candidate Generation Design.
- PR title note: PR #312 uses the shortened title `BACKEND-P101 Entry Stop TP Candidate Design` as a platform workaround.
- Baseline commit: `9955489` (`chore: add P101 placeholder`), based on `625a1ac` (`P100 Closure Gate (#310)`).
- Scope: documentation-only source-owned candidate generation design after the P100 closure gate.
- Placeholder removed: `docs/P101.md`.

## Files Changed

- `docs/PHASE_BACKEND_P101_ENTRY_STOP_TP_SOURCE_OWNED_CANDIDATE_GENERATION_DESIGN.md`
- Removed `docs/P101.md`

No Java, test, schema, dashboard, config, controller, endpoint, runtime, adapter, readiness, order, execution, automation, or external-data files are changed.

## Design-Only Scope

P101 starts a new separately scoped design line after P100.

P101 does not:

- generate real entry / stop / TP / RR values
- add Java implementation
- add runtime wiring
- read runtime data
- read live market data
- mutate dashboard, schema, or config
- add controller or endpoint Java
- wire production ownership review
- wire BoundaryCandidateService `VALID`
- upgrade ExecutionPlan readiness

All candidate statuses described here remain design-only unless a later separately authorized fixture phase creates test-scope DTOs or fixtures.

## Source-Owned Generation Chain

The intended chain is:

```text
market structure evidence
-> SourceTrace ownership
-> entry source owner
-> stop source owner
-> TP source owner
-> RR source owner
-> freshness / source window / rule version
-> conflict family validation
-> BoundaryCandidateDTO
-> Risk Action Guard
-> ExecutionPlan / Dashboard read-only display
```

The chain is source-owned because every numeric candidate must be explainable by an explicit source owner and source reference before any DTO may display it.

The chain is read-only because `ExecutionPlan / Dashboard read-only display` may show reviewed candidate context only after earlier gates are satisfied. It must not mutate dashboard state, schema, config, runtime SourceTrace fields, readiness, order state, execution state, automation state, or external-data state.

## Candidate Ownership Model

Each candidate family must carry ownership metadata before it can leave `INCOMPLETE`.

Required common ownership fields:

- `symbol`
- `timeframe`
- `decisionId`
- `analysisId`
- `sourceWindowStart`
- `sourceWindowEnd`
- `sourceWindowTimeframe`
- `sourceRuleVersion`
- `sourceEvidenceRefs`
- `sourceOwner`
- `sourceOwnerField`
- `sourceReason`
- `sourceTraceRef`
- `freshnessStatus`
- `conflictFamilyStatus`
- `manualReviewRequired=true`
- `notTradeInstruction=true`
- `reviewMode=REVIEW_ONLY`

Required existing `SourceTraceDTO` ownership fields:

- `entrySourceType`
- `entrySourceTimeframe`
- `entrySourceReason`
- `entrySourceRef`
- `stopSourceType`
- `stopSourceTimeframe`
- `stopSourceReason`
- `stopSourceRef`
- `tpSourceType`
- `tpSourceTimeframe`
- `tpSourceReason`
- `tpSourceRef`
- `rrRuleRef`
- `liquiditySource`
- `multiTimeframeSource`
- `eventSource`
- `wickSource`

Required numeric source trace fields:

- `entryPriceSource`
- `stopPriceSource`
- `tpPriceSources`
- `rrSource`
- `dataQualityScore`
- `dataQualityScoreSource`

`quoteLatestPrice` and `quotePriceUpdateTimeMs` may be freshness context only. They must not directly generate entry, stop, TP, or RR candidates.

## BoundaryCandidateDTO Mapping Design

A later fixture-only phase may map source-owned candidate data into existing DTO names without changing production behavior.

Design mapping:

- Entry candidate -> `BoundaryEntryDTO`
  - family -> `entryType`
  - candidate numeric field -> `entryPrice`, `entryZoneLow`, or `entryZoneHigh`
  - source family -> `numericSourceType`
  - source numeric anchor -> `numericSourceValue`
  - owner timeframe -> `sourceTimeframe`
  - source explanation -> `reason`

- Stop candidate -> `BoundaryStopDTO`
  - family -> `stopType`
  - candidate numeric field -> `stopPrice`, `stopZoneLow`, or `stopZoneHigh`
  - source family -> `numericSourceType`
  - source numeric anchor -> `numericSourceValue`
  - owner timeframe -> `sourceTimeframe`
  - source explanation -> `reason`

- TP candidate -> `BoundaryTakeProfitLevelDTO`
  - sequence -> `level`
  - candidate numeric field -> `price`
  - RR link -> `rr`
  - owner field -> `source`
  - source family -> `numericSourceType`
  - source numeric anchor -> `numericSourceValue`
  - owner timeframe -> `sourceTimeframe`
  - source reference -> `sourceRef`
  - display reason -> `reason`

- Ownership summary -> `BoundarySourceFieldsDTO`
  - entry source owner -> `entrySourceField`
  - stop source owner -> `stopSourceField`
  - take-profit source owner -> `takeProfitSourceField`
  - RR rule owner -> `rrRule`
  - evidence set owner -> `dataSource`
  - quality context -> `dataQualityScore`
  - evidence references -> `evidenceRefs`

This is a design contract only. P101 does not add or modify DTOs.

## Entry Candidate Families

### Structure Confirmation Zone

Purpose: candidate entry zone is owned by confirmed market structure evidence, such as a structure hold, confirmed reaction, or retest with source-owned structure context.

Required evidence:

- source-owned structure family
- entry source owner
- structure reference
- decision timeframe
- source window
- rule version
- freshness status
- conflict family validation

Must remain `INCOMPLETE` when the zone is inferred from AI text, dashboard text, latest price only, a single kline only, or aggregate score only.

### Breakout Retest Zone

Purpose: candidate entry zone is owned by a breakout level and retest evidence, not by the breakout label alone.

Required evidence:

- breakout source reference
- retest source reference
- entry source owner
- retest timeframe
- source window
- rule version
- freshness status
- conflict family validation

Must remain `INCOMPLETE` when breakout text or latest price is the only evidence.

### Support / Resistance Flip Zone

Purpose: candidate entry zone is owned by a prior support/resistance level that has source-owned flip evidence.

Required evidence:

- prior support/resistance source reference
- flip confirmation source reference
- entry source owner
- source timeframe
- source window
- rule version
- freshness status
- conflict family validation

Must remain `INCOMPLETE` when a label says support/resistance flipped without numeric source ownership.

## Stop Candidate Families

### Structural Invalidation Level With Buffer

Purpose: candidate stop zone is owned by the structure level that invalidates the entry thesis, with a separately sourced ATR or volatility buffer.

Required evidence:

- structural invalidation source reference
- stop source owner
- source timeframe
- source window
- rule version
- freshness status
- ATR / volatility buffer source reference
- conflict family validation

The buffer must be source-owned. It must not be guessed from AI text, dashboard text, latest price, or order/execution backfill.

The stop candidate must remain `INCOMPLETE` when the invalidation level is missing, ambiguous, stale, unsupported, or conflicts with entry ownership.

## TP Candidate Families

### Structure Target

Purpose: target is owned by market structure evidence such as a prior swing, measured structure target, or source-owned range boundary.

Required evidence:

- TP source owner
- structure target source reference
- source timeframe
- source window
- rule version
- freshness status
- conflict family validation

### Liquidity Target

Purpose: target is owned by liquidity evidence such as a source-owned liquidity pool or sweep target.

Required evidence:

- TP source owner
- liquidity source reference
- liquidity freshness status
- source timeframe
- source window
- rule version
- conflict family validation

Liquidity stress or stampede evidence must not generate an opportunity push.

### Prior High / Prior Low

Purpose: target is owned by prior high or prior low evidence with explicit timeframe and source reference.

Required evidence:

- TP source owner
- prior high / prior low source reference
- source timeframe
- source window
- rule version
- freshness status
- conflict family validation

Prior high / low labels alone are insufficient.

### RR Ladder

Purpose: target levels are derived from source-owned entry and stop candidates through an explicit RR rule owner.

Required evidence:

- source-owned entry candidate
- source-owned stop candidate
- `rrRuleRef`
- `rrSource`
- TP source owner
- source window
- rule version
- conflict family validation

RR ladder levels must remain fixture-only until a later phase defines a test-scope fixture contract. P101 does not generate real RR ladder prices.

## RR Candidate Rules

RR candidates link entry, stop, and TP ownership. They are not standalone signals.

Required RR rules:

- RR must be computed only from source-owned entry and source-owned stop distance plus a source-owned TP candidate.
- RR must carry `rrRuleRef` and `rrSource`.
- RR must use the same symbol and compatible timeframe as entry, stop, and TP.
- RR must fail closed when entry, stop, or TP ownership is incomplete.
- RR must fail closed when the entry-stop distance is absent, ambiguous, zero, negative, stale, or unsupported.
- RR must fail closed when any TP candidate lacks ownership.
- RR must fail closed when the RR rule version is missing or stale.
- RR must never be produced from AI text, dashboard text, latest price only, aggregate score only, order/execution backfill, or a single kline only.

RR values may be fixture-only in a later test phase. P101 does not generate RR values.

## Conflict Family Validation

Candidate status must remain `INCOMPLETE` until conflict family validation is complete.

Conflict families:

- ownership conflict
- timeframe conflict
- freshness conflict
- source window conflict
- rule version conflict
- entry/stop inversion conflict
- entry/TP direction conflict
- stop/TP overlap conflict
- liquidity stress conflict
- event-risk missing or unresolved conflict
- wick / pin-bar overinterpretation conflict
- stampede conflict
- high-risk conflict
- multi-timeframe conflict
- Risk Action Guard conflict

Conflict family validation must preserve blocker evidence. Positive-looking labels must not override blockers.

## Candidate Status Rules

### Must Remain INCOMPLETE

Candidate status must remain `INCOMPLETE` when any of the following is true:

- source owner is missing for entry, stop, TP, or RR
- numeric source field is missing
- source reference is missing
- source timeframe is missing or incompatible
- source window is missing or stale
- rule version is missing or stale
- freshness status is missing, stale, or ambiguous
- conflict family validation is missing
- conflict family validation reports any blocker
- evidence is malformed, unsupported, ambiguous, empty-but-present, or mixed safe/unsafe
- candidate uses AI text directly
- candidate uses dashboard text directly
- candidate uses latest price only
- candidate uses a single kline only
- candidate uses aggregate score only
- candidate uses order / execution backfill
- strong reversal is treated as direct reverse entry
- wick / pin-bar is treated as direct trend reversal
- liquidity stress or stampede is treated as an opportunity push
- missing event data is treated as no risk
- Risk Action Guard blocks completion
- `manualReviewRequired` would become false
- `notTradeInstruction` would become false
- review mode would become production, execution, order, automation, or trade-ready

### Later Fixture-Only VALID_CANDIDATE

A later separately authorized fixture phase may introduce fixture-only `VALID_CANDIDATE` status when all fixture requirements are met.

Fixture-only `VALID_CANDIDATE` must:

- stay test-scope / fixture-scope only
- require complete entry, stop, TP, and RR source ownership
- require numeric source trace fields
- require source window and rule version
- require completed conflict family validation
- preserve blocker evidence when downgraded
- keep `manualReviewRequired=true`
- keep `notTradeInstruction=true`
- keep `reviewMode=REVIEW_ONLY`
- not map to production `BoundaryStatusEnum.VALID`
- not wire BoundaryCandidateService `VALID`
- not upgrade ExecutionPlan readiness
- not mutate dashboard, schema, or config
- not create order, execution, scheduler, automation, external-data, or auto-trading behavior
- not generate real entry / stop / TP / RR values

## Forbidden Sources

The following sources must not directly generate entry, stop, TP, or RR candidates:

- AI text directly generating entry / stop / TP
- dashboard text directly generating entry / stop / TP
- latest price only
- single kline only
- aggregate score only
- order / execution backfill
- strong reversal directly becoming reverse entry
- wick / pin-bar directly becoming trend reversal
- liquidity stress / stampede generating opportunity push

Forbidden sources may be preserved as blocker evidence in fixture tests. They must not become candidate ownership.

## Risk Action Guard Handling

Risk Action Guard handling remains review-only and completion-blocking when unsafe.

Rules:

- Risk high but liquidity normal: may suggest reduce size / move stop / reduce leverage.
- Risk high and liquidity deteriorating: do not suggest one-shot market exit; prefer staged risk reduction / wait for liquidity recovery / reduce leverage.
- Risk high and stampede exists: forbid reverse, forbid new position, forbid opportunity push, protect principal first.
- Risk high but only short-term wick / pin-bar: do not treat as trend reversal; do not generate reverse entry; only warn and wait for confirmation.
- Missing event data cannot be treated as no risk.

Risk Action Guard output must not become an order instruction. It may only annotate review-only display or block fixture candidate status.

## ExecutionPlan / Dashboard Read-Only Display

The display target is read-only.

Allowed in a later separately scoped fixture or read-only phase:

- show candidate family names
- show source owner names
- show source references
- show freshness status
- show rule version
- show conflict blockers
- show Risk Action Guard review messages

Still not allowed:

- dashboard mutation
- `dashboard.html` changes in P101
- readiness upgrade
- order or execution intent
- trade-ready labels
- buy / sell / open / close / reverse / signal behavior
- generated real entry / stop / TP / RR values

## Future Phase Outline

Recommended future phases:

- P102: Entry Source-Owned Candidate Fixture Contract
- P103: Stop / TP / RR Source-Owned Candidate Fixture Contract
- P104: BoundaryCandidate Numeric Source Assembler Fixture-Only
- P105: Market Read-Only Candidate Generation Design
- P106: Market Read-Only Candidate Generator Skeleton

Each future phase must define its own scope. None of these future phase names authorize production wiring, runtime data reads, live market data reads, dashboard mutation, schema/config changes, order/execution behavior, scheduler, automation, auto-trading, or real entry / stop / TP / RR generation by implication.

## Still-Blocked Paths

The following paths remain blocked:

- production ownership review wiring
- production completion
- production adapter
- `DefaultSourceTraceEntryOwnershipAdapter`
- production `DefaultSourceTraceEntryCompletionContract`
- runtime SourceTrace field population
- full SourceTrace runtime completion
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
- real entry / stop / TP / RR generation
- runtime data reads
- live market data reads

## Boundary Confirmations

- P101 is documentation-only source-owned candidate generation design.
- P101 starts a new separately scoped line after P100.
- P101 does not add production wiring.
- P101 does not implement production completion.
- P101 does not add production adapter.
- P101 does not add `DefaultSourceTraceEntryOwnershipAdapter`.
- P101 does not add production `DefaultSourceTraceEntryCompletionContract`.
- P101 does not populate real SourceTrace fields in runtime.
- P101 does not complete full SourceTrace in runtime.
- P101 does not wire BoundaryCandidateService `VALID` production path.
- P101 does not upgrade ExecutionPlan readiness.
- P101 does not add controller/endpoint Java.
- P101 does not modify `dashboard.html`.
- P101 does not modify schema.
- P101 does not modify config.
- P101 does not add external data integration.
- P101 does not add order API.
- P101 does not add execution API.
- P101 does not add scheduler / automation / auto-trading.
- P101 does not generate real entry / stop / TP / RR values.
- P101 does not read runtime data or live market data.
- Placeholder `docs/P101.md` is removed.

## Validation

P101 changes documentation only. No Java or test source was modified, so Maven was not required for this phase.

Required validation:

```text
git diff --check
```
