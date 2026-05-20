# BACKEND-P104 BoundaryCandidate Numeric Source Assembler Fixture-Only

## Baseline

- Branch context: PR #318 / Issue #317.
- Formal mainline title: BACKEND-P104 BoundaryCandidate Numeric Source Assembler Fixture-Only.
- PR title note: PR #318 uses the shortened title `P104 Fixture Assembler` as a platform workaround.
- Baseline commit: `86e494e` (`chore: add P104 placeholder`), based on `7010fa6` (`P103 Stop TP RR Fixture (#316)`).
- Scope: documentation-only BoundaryCandidate numeric source assembler fixture contract.
- Placeholder removed: `docs/P104.md`.

## Files Changed

- `docs/PHASE_BACKEND_P104_BOUNDARY_CANDIDATE_NUMERIC_SOURCE_ASSEMBLER_FIXTURE_ONLY.md`
- Removed `docs/P104.md`

No Java, test, schema, dashboard, config, controller, endpoint, runtime, adapter, readiness, order, execution, automation, or external-data files are changed.

## Fixture-Only Scope

P104 defines a fixture-only BoundaryCandidate numeric source assembler design/contract.

The assembler consumes source-owned fixture entry / stop / TP / RR contracts and describes a review-only `BoundaryCandidateDTO`-style output.

P104 does not:

- generate real entry / stop / TP / RR values
- read runtime data
- read live market data
- implement production candidate generation
- wire BoundaryCandidateService `VALID` production path
- upgrade ExecutionPlan readiness
- mutate dashboard, schema, or config
- add controller or endpoint Java
- add test-scope Java
- modify production Java

## Assembler Purpose

The fixture-only assembler proves how future fixture contracts may be combined without becoming production valid.

It must preserve:

- source ownership
- fixture-only numeric source metadata
- dependency status
- blocker evidence
- review-only behavior
- not-trade-instruction behavior

It must not infer or compute trading levels from market data, runtime data, live data, dashboard text, AI text, order history, execution history, or aggregate scores.

## Inputs

Required inputs:

- P102 entry fixture contract
- P103 stop fixture contract
- P103 TP fixture contract
- P103 RR fixture contract

Each input must expose:

- fixture status
- source owner
- source ref
- source timeframe
- source reason
- source window
- rule id
- rule version
- freshness ownership
- conflict family ownership
- fixture-only numeric source envelope
- blocking reasons
- `manualReviewRequired=true`
- `notTradeInstruction=true`
- `reviewMode=REVIEW_ONLY`

## Output Shape

The output is `BoundaryCandidateDTO`-style fixture output only.

Conceptual output fields:

- `assemblerFixtureStatus`
- `symbol`
- `timeframe`
- `entry`
- `stop`
- `takeProfitLevels`
- `sourceFields`
- `fixtureNumericSourceTrace`
- `manualReviewRequired=true`
- `notTradeInstruction=true`
- `reviewMode=REVIEW_ONLY`
- `blockingReasons`

The output must not:

- set production `BoundaryStatusEnum.VALID`
- call `BoundaryCandidateDTO.valid(...)`
- wire BoundaryCandidateService `VALID`
- upgrade ExecutionPlan readiness
- mutate dashboard, schema, or config
- create order, execution, scheduler, automation, external-data, or auto-trading behavior
- generate real entry / stop / TP / RR values

## Assembler Statuses

Allowed assembler statuses:

- `INCOMPLETE`
- `BLOCKED`
- `FIXTURE_VALID_CANDIDATE`

No other status is authorized by P104.

### INCOMPLETE

Use `INCOMPLETE` when one or more required dependencies or source-owned fields are missing, stale-but-not-forbidden, ambiguous, or not fully evaluated.

`INCOMPLETE` must keep:

- `manualReviewRequired=true`
- `notTradeInstruction=true`
- `reviewMode=REVIEW_ONLY`
- blocker or missing-field evidence

### BLOCKED

Use `BLOCKED` when any dependency is blocked, any forbidden source appears, or any safety conflict appears.

`BLOCKED` must keep:

- `manualReviewRequired=true`
- `notTradeInstruction=true`
- `reviewMode=REVIEW_ONLY`
- field-specific blocker evidence

### FIXTURE_VALID_CANDIDATE

Use `FIXTURE_VALID_CANDIDATE` only in a later fixture-only test phase when every input dependency is fixture-valid, all numeric source envelopes are fixture-only, all conflict family checks pass, and no blocker is present.

`FIXTURE_VALID_CANDIDATE` must keep:

- `manualReviewRequired=true`
- `notTradeInstruction=true`
- `reviewMode=REVIEW_ONLY`

`FIXTURE_VALID_CANDIDATE` must not:

- map to production `BoundaryStatusEnum.VALID`
- wire BoundaryCandidateService `VALID`
- upgrade ExecutionPlan readiness
- mutate dashboard, schema, or config
- create order, execution, scheduler, automation, external-data, or auto-trading behavior
- generate real entry / stop / TP / RR values

## Numeric Source Trace Envelope

Every assembled numeric field must preserve a fixture-only numeric source envelope.

Required envelope fields:

- `fixtureOnly=true`
- `sourceRef`
- `sourceTimeframe`
- `sourceReason`
- `ruleId`
- `ruleVersion`
- `freshnessOwnership`
- `conflictFamilyOwnership`
- `numericSourceType`
- `numericSourceValue`
- `numericSourceToken`

Allowed numeric source value categories:

- fixture-only numeric source values only
- symbolic fixture value tokens
- synthetic fixture anchors created only inside future tests
- fixture-only entry-stop distance tokens for RR

Forbidden numeric source value categories:

- real market values
- latest-price-only values
- runtime values
- live market values
- order-derived values
- execution-derived values
- dashboard-derived values
- AI-text-derived values

If a future fixture helper maps into existing DTO fields that accept `BigDecimal`, the value must remain synthetic fixture data and must be paired with the token metadata. P104 does not define or generate any numeric value.

## Entry Mapping

Map P102 entry fixture contract to `BoundaryEntryDTO`-style fields:

| P102 entry fixture field | BoundaryEntryDTO-style field |
| --- | --- |
| `entryCandidateFamily` | `entryType` |
| fixture entry zone low token/value | `entryZoneLow` |
| fixture entry zone high token/value | `entryZoneHigh` |
| fixture entry price token/value, if present | `entryPrice` |
| `numericSource.numericSourceType` | `numericSourceType` |
| `numericSource.numericSourceValue/token` | `numericSourceValue` plus fixture token metadata |
| `entrySourceTimeframe` | `sourceTimeframe` |
| `entrySourceReason` | `reason` |

Entry mapping must preserve:

- `fixtureOnly`
- `entrySourceRef`
- `ruleId`
- `ruleVersion`
- `freshnessOwnership`
- `conflictFamilyOwnership`
- `sourceOwner`

Entry mapping must not generate a real entry price.

## Stop Mapping

Map P103 stop fixture contract to `BoundaryStopDTO`-style fields:

| P103 stop fixture field | BoundaryStopDTO-style field |
| --- | --- |
| `stopCandidateFamily` | `stopType` |
| fixture stop zone low token/value | `stopZoneLow` |
| fixture stop zone high token/value | `stopZoneHigh` |
| fixture stop price token/value, if present | `stopPrice` |
| `stopNumericSource.numericSourceType` | `numericSourceType` |
| `stopNumericSource.numericSourceValue/token` | `numericSourceValue` plus fixture token metadata |
| `stopSourceTimeframe` | `sourceTimeframe` |
| `stopSourceReason` | `reason` |

Stop mapping must preserve:

- `fixtureOnly`
- `stopSourceRef`
- `stopRuleId`
- `stopRuleVersion`
- `stopFreshnessOwnership`
- `stopConflictFamilyOwnership`
- `stopSourceOwner`
- volatility buffer source owner and source ref

Stop mapping must not generate a real stop value.

## TP Ladder Mapping

Map P103 TP fixture contract to `BoundaryTakeProfitLevelDTO`-style fields:

| P103 TP fixture field | BoundaryTakeProfitLevelDTO-style field |
| --- | --- |
| `tpLevel` | `level` |
| fixture TP token/value | `price` |
| linked fixture RR token/value, if present | `rr` |
| `tpSourceOwner` or `tpSourceType` | `source` |
| `tpNumericSource.numericSourceType` | `numericSourceType` |
| `tpNumericSource.numericSourceValue/token` | `numericSourceValue` plus fixture token metadata |
| `tpSourceTimeframe` | `sourceTimeframe` |
| `tpSourceRef` | `sourceRef` |
| fixture-only allocation hint, if present | `allocationRatio` or `partialRatio` as display-only fixture metadata |
| `tpSourceReason` | `reason` |

TP mapping must preserve:

- `fixtureOnly`
- `tpRuleId`
- `tpRuleVersion`
- `tpFreshnessOwnership`
- `tpConflictFamilyOwnership`
- TP family: structure target, liquidity target, prior high / prior low, or RR ladder

TP mapping must not generate real TP values.

## RR Mapping

RR is not a standalone output object in the current `BoundaryCandidateDTO` shape. P104 preserves RR in fixture numeric source metadata, TP ladder `rr` fields, and ownership summary.

RR mapping must preserve:

- `rrSourceOwner`
- `rrSourceRef`
- `rrSourceTimeframe`
- `rrSourceReason`
- `rrRuleId`
- `rrRuleVersion`
- `rrFreshnessOwnership`
- `rrConflictFamilyOwnership`
- `rrNumericSource.fixtureOnly`
- `rrNumericSource.numericSourceType`
- `rrNumericSource.numericSourceValue/token`
- `entryFixtureDependencyRef`
- `stopFixtureDependencyRef`
- `tpFixtureDependencyRef`
- `entryStopDistanceFixtureRef`

RR mapping must fail closed when entry, stop, or TP dependency is incomplete or blocked.

RR mapping must not generate real RR.

## Ownership Summary Mapping

Map dependency ownership into `BoundarySourceFieldsDTO`-style fields:

| Fixture ownership field | BoundarySourceFieldsDTO-style field |
| --- | --- |
| P102 entry source owner and ref | `entrySourceField` |
| P103 stop source owner and ref | `stopSourceField` |
| P103 TP source owner and ref | `takeProfitSourceField` |
| P103 RR rule id / version / source owner | `rrRule` |
| fixture data source description | `dataSource` |
| fixture-only quality token/value, if present | `dataQualityScore` as synthetic fixture data only |
| all source refs and blocker refs | `evidenceRefs` |

The ownership summary must preserve all source refs required to audit the assembled candidate.

## Status Rules

### Missing Dependencies

Any missing dependency forces:

```text
assemblerFixtureStatus=INCOMPLETE
```

Specific missing dependency rules:

- Missing P102 entry fixture dependency -> `INCOMPLETE`
- Missing P103 stop fixture dependency -> `INCOMPLETE`
- Missing P103 TP fixture dependency -> `INCOMPLETE`
- Missing P103 RR fixture dependency -> `INCOMPLETE`

Required blocker evidence:

- missing dependency field name
- dependency phase reference
- `missing_dependency`

### Blocked Dependencies

Any blocked entry / stop / TP / RR dependency forces:

```text
assemblerFixtureStatus=BLOCKED
```

Required blocker evidence:

- blocked dependency field name
- dependency blocking reasons
- `blocked_dependency`

### Forbidden Sources

Any forbidden source forces:

```text
assemblerFixtureStatus=BLOCKED
```

Forbidden sources:

- AI text directly generating numeric values
- dashboard text directly generating numeric values
- latest price only
- single kline only
- aggregate score only
- order / execution backfill
- strong reversal directly becoming reverse entry
- wick / pin-bar directly becoming trend reversal
- liquidity stress / stampede opportunity push

### Direction And Boundary Conflicts

The following conflicts force:

```text
assemblerFixtureStatus=BLOCKED
```

Blocked conflicts:

- entry-stop inversion
- entry-TP direction conflict
- stop-TP overlap

Required blocker evidence:

- conflict family name
- conflicting dependency refs
- source-owned conflict evidence

### Risk Action Guard Blocker

Any Risk Action Guard blocker forces:

```text
assemblerFixtureStatus=BLOCKED
```

Risk Action Guard blockers remain review-only and must block fixture valid status.

### Fixture-Only Valid Output

Fixture-only valid assembler output is allowed only when all conditions are true:

- P102 entry fixture dependency is present and fixture-valid
- P103 stop fixture dependency is present and fixture-valid
- P103 TP fixture dependency is present and fixture-valid
- P103 RR fixture dependency is present and fixture-valid
- every numeric source envelope has `fixtureOnly=true`
- every required source ref is present
- every required source timeframe is present
- every required source reason is present
- every required rule id is present
- every required rule version is present
- freshness ownership is passing
- conflict family ownership is passing
- no forbidden source is present
- no direction or boundary conflict is present
- no Risk Action Guard blocker is present
- `manualReviewRequired=true`
- `notTradeInstruction=true`
- `reviewMode=REVIEW_ONLY`

Even then, the output is not production valid.

## Production VALID Mapping Prohibition

Fixture-only valid assembler output must not map to:

- production `BoundaryStatusEnum.VALID`
- `BoundaryCandidateDTO.valid(...)`
- BoundaryCandidateService `VALID`
- ExecutionPlan readiness
- dashboard mutation
- order intent
- execution intent
- trade-ready state
- buy / sell / open / close / reverse / signal behavior

Future tests must assert this prohibition before any test-scope assembler can be introduced.

## Review-Only Output Guard

The assembler output must always keep:

- `manualReviewRequired=true`
- `notTradeInstruction=true`
- `reviewMode=REVIEW_ONLY`

The output must not expose:

- trade-ready status
- order action
- execution action
- scheduler action
- automation action
- external-data fetch action
- auto-trading action
- production valid surface

## Blocked Dependencies And Evidence Preservation

When the assembler returns `BLOCKED`, it must preserve blocker evidence from every blocked dependency.

Required evidence preservation:

- dependency name
- dependency status
- dependency source owner
- dependency source ref
- dependency rule id
- dependency rule version
- dependency freshness ownership
- dependency conflict family ownership
- dependency numeric source type
- dependency numeric source value/token
- dependency blocking reasons

If more than one dependency is blocked, the assembler must preserve all blocker evidence and must not collapse blockers into a generic failure.

## Still-Blocked Paths

The following paths remain blocked:

- production Java changes
- real entry / stop / TP / RR value generation
- production candidate generation
- runtime data reads
- live market data reads
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

## Boundary Confirmations

- P104 is fixture-only assembler contract only.
- P104 is documentation-only.
- P104 does not add test-scope Java.
- P104 does not modify production Java.
- P104 does not generate real entry / stop / TP / RR values.
- P104 does not implement production candidate generation.
- P104 does not read runtime data.
- P104 does not read live market data.
- P104 does not wire BoundaryCandidateService `VALID` production path.
- P104 does not upgrade ExecutionPlan readiness.
- P104 does not modify `dashboard.html`.
- P104 does not modify schema.
- P104 does not modify config.
- P104 does not add controller/endpoint Java.
- P104 does not add external data integration.
- P104 does not add order API.
- P104 does not add execution API.
- P104 does not add scheduler / automation / auto-trading.
- Placeholder `docs/P104.md` is removed.

## Validation

P104 changes documentation only. No Java or test source was modified, so Maven was not required for this phase.

Required validation:

```text
git diff --check
```
