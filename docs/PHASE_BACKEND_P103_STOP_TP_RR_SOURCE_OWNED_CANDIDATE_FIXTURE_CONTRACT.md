# BACKEND-P103 Stop TP RR Source-Owned Candidate Fixture Contract

## Baseline

- Branch context: PR #316 / Issue #315.
- Formal mainline title: BACKEND-P103 Stop TP RR Source-Owned Candidate Fixture Contract.
- PR title note: PR #316 uses the shortened title `P103 Stop TP RR Fixture` as a platform workaround.
- Baseline commit: `e934389` (`chore: add P103 placeholder`), based on `bf3c8f4` (`P102 Entry Fixture (#314)`).
- Scope: documentation-only stop / TP / RR source-owned candidate fixture contract.
- Placeholder removed: `docs/P103.md`.

## Files Changed

- `docs/PHASE_BACKEND_P103_STOP_TP_RR_SOURCE_OWNED_CANDIDATE_FIXTURE_CONTRACT.md`
- Removed `docs/P103.md`

No Java, test, schema, dashboard, config, controller, endpoint, runtime, adapter, readiness, order, execution, automation, or external-data files are changed.

## Fixture-Only Scope

P103 defines fixture-only contracts for stop, take-profit, and risk/reward source-owned candidates.

This phase allows future stop / TP / RR candidate fixtures to be tested without:

- generating real stop / TP / RR values
- generating real entry values
- reading runtime data
- reading live market data
- wiring production candidate generation
- wiring BoundaryCandidateService `VALID` production path
- upgrading ExecutionPlan readiness
- changing dashboard, schema, config, or controller code
- changing production Java

P103 does not create test-scope Java. It documents the contract that a later fixture-only implementation may use.

## Contract Purpose

The stop / TP / RR fixture contract proves that downstream candidate fixtures can be represented only when they are owned by source evidence and remain review-only.

The fixture contract must not become:

- production candidate generation
- production `BoundaryStatusEnum.VALID`
- runtime SourceTrace completion
- readiness upgrade
- dashboard mutation
- order instruction
- execution instruction
- automation trigger
- trading signal

## Fixture Statuses

Allowed fixture statuses:

- `INCOMPLETE`
- `BLOCKED`
- `FIXTURE_VALID_CANDIDATE`

No other status is authorized by P103.

### INCOMPLETE

Use `INCOMPLETE` when required source-owned evidence is missing, stale-but-not-forbidden, ambiguous, or not fully evaluated.

`INCOMPLETE` must preserve missing-field or blocker evidence and must keep:

- `manualReviewRequired=true`
- `notTradeInstruction=true`
- `reviewMode=REVIEW_ONLY`

### BLOCKED

Use `BLOCKED` when the fixture contains forbidden source evidence, unsafe market shortcut evidence, or an explicit safety conflict.

`BLOCKED` must preserve field-specific blocker evidence and must keep:

- `manualReviewRequired=true`
- `notTradeInstruction=true`
- `reviewMode=REVIEW_ONLY`

### FIXTURE_VALID_CANDIDATE

Use `FIXTURE_VALID_CANDIDATE` only in a later fixture-only test phase when every required stop, TP, and RR source-owned field is present, fixture-only numeric ownership is present, P102 entry fixture dependency is satisfied, source freshness is acceptable, conflict family validation is complete, and no blocker is present.

`FIXTURE_VALID_CANDIDATE` still must keep:

- `manualReviewRequired=true`
- `notTradeInstruction=true`
- `reviewMode=REVIEW_ONLY`

`FIXTURE_VALID_CANDIDATE` must not:

- map to production `BoundaryStatusEnum.VALID`
- wire BoundaryCandidateService `VALID`
- generate real stop / TP / RR values
- generate real entry values
- upgrade ExecutionPlan readiness
- mutate dashboard, schema, or config
- create order, execution, scheduler, automation, external-data, or auto-trading behavior

## Common Fixture Contract Fields

Required common fields for stop, TP, and RR fixture contracts:

| Field | Requirement |
| --- | --- |
| `symbol` | Required fixture symbol identifier. Must not be sourced from runtime reads in P103. |
| `timeframe` | Required fixture timeframe. Must align with source timeframe. |
| `fixtureStatus` | Required. One of `INCOMPLETE`, `BLOCKED`, `FIXTURE_VALID_CANDIDATE`. |
| `manualReviewRequired` | Required `true`. |
| `notTradeInstruction` | Required `true`. |
| `reviewMode` | Required `REVIEW_ONLY`. |
| `blockingReasons` | Required list. Empty only for fixture-only valid candidates. |
| `sourceWindow` | Required before fixture-only valid status. |
| `ruleId` | Required before fixture-only valid status. |
| `ruleVersion` | Required before fixture-only valid status. |
| `freshnessOwnership` | Required before fixture-only valid status. |
| `conflictFamilyOwnership` | Required before fixture-only valid status. |
| `numericSource` | Required before fixture-only valid status. Fixture-only only. |
| `sourceRef` | Required before fixture-only valid status. |
| `sourceTimeframe` | Required before fixture-only valid status. |
| `sourceReason` | Required before fixture-only valid status. |
| `sourceOwner` | Required before fixture-only valid status. |

## Stop Candidate Fixture Contract Fields

Required stop fields:

| Field | Requirement |
| --- | --- |
| `stopFixtureStatus` | Required. One of the allowed fixture statuses. |
| `stopCandidateFamily` | Required. Must be `STRUCTURAL_INVALIDATION_WITH_BUFFER`. |
| `stopSourceOwner` | Required before fixture-only valid status. |
| `stopSourceType` | Required before fixture-only valid status. |
| `stopSourceTimeframe` | Required before fixture-only valid status. |
| `stopSourceReason` | Required before fixture-only valid status. |
| `stopSourceRef` | Required before fixture-only valid status. |
| `stopSourceWindow` | Required before fixture-only valid status. |
| `stopRuleId` | Required before fixture-only valid status. |
| `stopRuleVersion` | Required before fixture-only valid status. |
| `stopFreshnessOwnership` | Required before fixture-only valid status. |
| `stopConflictFamilyOwnership` | Required before fixture-only valid status. |
| `stopNumericSource` | Required before fixture-only valid status. Fixture-only only. |
| `volatilityBufferSourceOwner` | Required when the family uses ATR / volatility buffer. |
| `volatilityBufferSourceRef` | Required when the family uses ATR / volatility buffer. |

## TP Candidate Fixture Contract Fields

Required TP fields:

| Field | Requirement |
| --- | --- |
| `tpFixtureStatus` | Required. One of the allowed fixture statuses. |
| `tpCandidateFamily` | Required. Must be one of the allowed TP families. |
| `tpSourceOwner` | Required before fixture-only valid status. |
| `tpSourceType` | Required before fixture-only valid status. |
| `tpSourceTimeframe` | Required before fixture-only valid status. |
| `tpSourceReason` | Required before fixture-only valid status. |
| `tpSourceRef` | Required before fixture-only valid status. |
| `tpSourceWindow` | Required before fixture-only valid status. |
| `tpRuleId` | Required before fixture-only valid status. |
| `tpRuleVersion` | Required before fixture-only valid status. |
| `tpFreshnessOwnership` | Required before fixture-only valid status. |
| `tpConflictFamilyOwnership` | Required before fixture-only valid status. |
| `tpNumericSource` | Required before fixture-only valid status. Fixture-only only. |
| `tpLevel` | Required for multi-target fixture ladders. |
| `tpAllocationHint` | Optional fixture-only display hint. Must not become order sizing. |

## RR Candidate Fixture Contract Fields

Required RR fields:

| Field | Requirement |
| --- | --- |
| `rrFixtureStatus` | Required. One of the allowed fixture statuses. |
| `rrSourceOwner` | Required before fixture-only valid status. |
| `rrSourceType` | Required before fixture-only valid status. |
| `rrSourceTimeframe` | Required before fixture-only valid status. |
| `rrSourceReason` | Required before fixture-only valid status. |
| `rrSourceRef` | Required before fixture-only valid status. |
| `rrSourceWindow` | Required before fixture-only valid status. |
| `rrRuleId` | Required before fixture-only valid status. |
| `rrRuleVersion` | Required before fixture-only valid status. |
| `rrFreshnessOwnership` | Required before fixture-only valid status. |
| `rrConflictFamilyOwnership` | Required before fixture-only valid status. |
| `rrNumericSource` | Required before fixture-only valid status. Fixture-only only. |
| `entryFixtureDependencyRef` | Required. Must reference a P102 entry fixture contract. |
| `stopFixtureDependencyRef` | Required before RR fixture-only valid status. |
| `tpFixtureDependencyRef` | Required before RR fixture-only valid status. |
| `entryStopDistanceFixtureRef` | Required before RR fixture-only valid status. Fixture-only only. |

## Required Source-Owned Fields

The following fields are mandatory for any fixture-only `FIXTURE_VALID_CANDIDATE`:

- stop source owner
- TP source owner
- RR source owner
- `sourceWindow`
- `ruleId`
- `ruleVersion`
- `freshnessOwnership`
- `conflictFamilyOwnership`
- `numericSource`
- `sourceRef`
- `sourceTimeframe`
- `sourceReason`
- `sourceOwner`

Missing any required source-owned field keeps the fixture out of `FIXTURE_VALID_CANDIDATE`.

## Stop Family

Allowed stop family:

- `STRUCTURAL_INVALIDATION_WITH_BUFFER`

### STRUCTURAL_INVALIDATION_WITH_BUFFER

Source-owned fixture contract:

- `stopSourceType=STRUCTURAL_INVALIDATION_WITH_BUFFER`
- stop source owner identifies structural invalidation evidence
- `stopSourceRef` points to fixture invalidation evidence
- `stopSourceWindow` identifies the fixture evidence window
- `stopRuleId` and `stopRuleVersion` identify the fixture rule
- `stopFreshnessOwnership` confirms the fixture evidence is not stale
- `stopConflictFamilyOwnership` confirms no unresolved stop conflict
- `stopNumericSource` is fixture-only
- ATR / volatility buffer has its own fixture source owner and source ref

The stop family must remain `INCOMPLETE` or `BLOCKED` if the structural invalidation source, buffer ownership, or entry fixture dependency is missing.

## TP Families

Allowed TP families:

- `STRUCTURE_TARGET`
- `LIQUIDITY_TARGET`
- `PRIOR_HIGH_LOW`
- `RR_LADDER`

### STRUCTURE_TARGET

Source-owned fixture contract:

- TP source owner identifies structure target evidence
- `tpSourceRef` points to fixture structure target evidence
- `tpSourceWindow` identifies the fixture evidence window
- `tpRuleId` and `tpRuleVersion` identify the fixture rule
- `tpFreshnessOwnership` confirms the fixture evidence is not stale
- `tpConflictFamilyOwnership` confirms no unresolved TP conflict
- `tpNumericSource` is fixture-only

### LIQUIDITY_TARGET

Source-owned fixture contract:

- TP source owner identifies liquidity target evidence
- `tpSourceRef` points to fixture liquidity target evidence
- `tpSourceWindow` identifies the fixture evidence window
- `tpRuleId` and `tpRuleVersion` identify the fixture rule
- `tpFreshnessOwnership` confirms the fixture evidence is not stale
- `tpConflictFamilyOwnership` confirms no unresolved liquidity conflict
- `tpNumericSource` is fixture-only

Liquidity stress or stampede evidence must not generate opportunity push.

### PRIOR_HIGH_LOW

Source-owned fixture contract:

- TP source owner identifies prior high or prior low evidence
- `tpSourceRef` points to fixture prior high / prior low evidence
- `tpSourceWindow` identifies the fixture evidence window
- `tpRuleId` and `tpRuleVersion` identify the fixture rule
- `tpFreshnessOwnership` confirms the fixture evidence is not stale
- `tpConflictFamilyOwnership` confirms no unresolved prior high / low conflict
- `tpNumericSource` is fixture-only

Prior high / low labels alone are insufficient.

### RR_LADDER

Source-owned fixture contract:

- TP source owner identifies RR ladder fixture evidence
- `tpSourceRef` points to fixture RR ladder evidence
- `tpSourceWindow` identifies the fixture evidence window
- `tpRuleId` and `tpRuleVersion` identify the fixture rule
- `tpFreshnessOwnership` confirms the fixture evidence is not stale
- `tpConflictFamilyOwnership` confirms no unresolved RR ladder conflict
- `tpNumericSource` is fixture-only
- RR fixture dependency is present and source-owned

RR ladder must remain fixture-only and must not generate real TP values.

## RR Rules

RR fixture candidates link fixture entry, stop, and TP contracts. They are not standalone signals.

Required RR rules:

- RR must link fixture entry / stop / TP.
- RR must require entry fixture dependency from P102.
- RR must require stop fixture dependency from P103.
- RR must require TP fixture dependency from P103.
- RR must fail closed if entry, stop, or TP fixture contract is incomplete.
- RR must fail closed if entry, stop, or TP fixture contract is blocked.
- RR must fail closed if entry-stop distance is missing, zero, negative, ambiguous, stale, or unsupported.
- RR must fail closed if TP ownership is missing.
- RR must carry RR source owner, source ref, rule id, rule version, freshness ownership, and conflict family ownership.
- RR must be fixture-only and must not generate real RR.
- RR must not map to production `BoundaryStatusEnum.VALID`.
- RR must not wire BoundaryCandidateService `VALID`.

## Numeric Source Contract

`numericSource` is required before `FIXTURE_VALID_CANDIDATE`, but it must be fixture-only.

Required numeric source fields for stop, TP, and RR:

- `numericSource.fixtureOnly=true`
- `numericSource.sourceType`
- `numericSource.sourceRef`
- `numericSource.sourceTimeframe`
- `numericSource.valueToken`
- `numericSource.valueKind`
- `numericSource.valueOwner`
- `numericSource.ruleId`
- `numericSource.ruleVersion`

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

P103 does not define any real numeric value. P103 does not generate stop, TP, or RR values.

## Fixture Status Rules

### Missing Stop Owner

If stop source owner is missing, blank, unsupported, or ambiguous:

```text
stopFixtureStatus=INCOMPLETE
```

Required blocker evidence:

- `stopSourceOwner`
- `missing_stop_owner`

### Missing TP Owner

If TP source owner is missing, blank, unsupported, or ambiguous:

```text
tpFixtureStatus=INCOMPLETE
```

Required blocker evidence:

- `tpSourceOwner`
- `missing_tp_owner`

### Missing RR Owner

If RR source owner is missing, blank, unsupported, or ambiguous:

```text
rrFixtureStatus=INCOMPLETE
```

Required blocker evidence:

- `rrSourceOwner`
- `missing_rr_owner`

### Missing Numeric Source

If any required stop, TP, or RR numeric source is missing or lacks fixture-only ownership:

```text
fixtureStatus=INCOMPLETE
```

Required blocker evidence:

- `numericSource`
- `missing_numeric_source`

### Missing Entry Fixture Contract Dependency

If the P102 entry fixture contract dependency is missing:

```text
fixtureStatus=INCOMPLETE
```

Required blocker evidence:

- `entryFixtureDependencyRef`
- `missing_entry_fixture_dependency`

### Stale Source Window

If source window is stale but the evidence is otherwise source-owned:

```text
fixtureStatus=INCOMPLETE
```

If stale source window is paired with unsafe, contradictory, or forbidden evidence:

```text
fixtureStatus=BLOCKED
```

Required blocker evidence:

- `sourceWindow`
- `stale_source_window`

### Unsupported Source Family

If stop, TP, or RR source family is outside the allowed families:

```text
fixtureStatus=BLOCKED
```

Required blocker evidence:

- `sourceFamily`
- `unsupported_source_family`

### Entry-Stop Inversion

If fixture entry and stop ownership imply inverted or contradictory direction:

```text
fixtureStatus=BLOCKED
```

Required blocker evidence:

- `entryStopDirection`
- `entry_stop_inversion`

### Entry-TP Direction Conflict

If fixture entry and TP ownership imply contradictory direction:

```text
fixtureStatus=BLOCKED
```

Required blocker evidence:

- `entryTpDirection`
- `entry_tp_direction_conflict`

### Stop-TP Overlap

If fixture stop and TP ownership overlap or collapse the risk boundary:

```text
fixtureStatus=BLOCKED
```

Required blocker evidence:

- `stopTpBoundary`
- `stop_tp_overlap`

### Risk Action Guard Blockers

If Risk Action Guard evidence blocks completion:

```text
fixtureStatus=BLOCKED
```

Risk Action Guard blockers remain review-only and must block fixture valid status.

Required blocker evidence:

- `riskActionGuard`
- specific Risk Action Guard token

## Blocked Sources And Conflicts

The following sources and conflicts always force `BLOCKED`:

| Source or conflict | Required blocker evidence |
| --- | --- |
| AI text directly generating stop / TP / RR | `ai_text_numeric_source` |
| Dashboard text directly generating stop / TP / RR | `dashboard_text_numeric_source` |
| Latest price only | `latest_price_only_numeric_source` |
| Single kline only | `single_kline_only_numeric_source` |
| Aggregate score only | `aggregate_score_only_numeric_source` |
| Order / execution backfill | `order_execution_backfill_numeric_source` |
| Strong reversal directly becoming reverse entry | `strong_reversal_direct_reverse_entry` |
| Wick / pin-bar directly becoming trend reversal | `wick_pin_bar_direct_trend_reversal` |
| Liquidity stress / stampede opportunity push | `liquidity_stress_stampede_opportunity_push` |
| Entry-stop inversion | `entry_stop_inversion` |
| Entry-TP direction conflict | `entry_tp_direction_conflict` |
| Stop-TP overlap | `stop_tp_overlap` |

Blocked sources must not be downgraded into `INCOMPLETE` if their unsafe source type is known. They must remain review-only blockers.

## Fixture-Only Valid Candidate Requirements

`FIXTURE_VALID_CANDIDATE` is allowed only when every item below is true:

- P102 entry fixture dependency is present and fixture-valid
- stop fixture source owner is present and supported
- TP fixture source owner is present and supported
- RR fixture source owner is present and supported
- stop family is `STRUCTURAL_INVALIDATION_WITH_BUFFER`
- TP family is allowed
- RR dependency links fixture entry / stop / TP
- source windows are present and fresh
- rule ids are present
- rule versions are present and current for the fixture
- freshness ownership is present and passing
- conflict family ownership is present and passing
- fixture-only numeric sources are present
- entry-stop distance fixture ref is present and supported
- TP ownership is present
- no forbidden source is present
- no direction conflict is present
- no stop/TP overlap is present
- no Risk Action Guard blocker is present
- `manualReviewRequired=true`
- `notTradeInstruction=true`
- `reviewMode=REVIEW_ONLY`

Even when all items are true, the fixture-only valid candidate is not production valid.

## Production VALID Mapping Prohibition

Fixture-only `FIXTURE_VALID_CANDIDATE` must not map to:

- production `BoundaryStatusEnum.VALID`
- BoundaryCandidateService `VALID` production path
- ExecutionPlan readiness
- dashboard mutation
- order intent
- execution intent
- trade-ready state
- buy / sell / open / close / reverse / signal behavior

Future tests must assert this boundary before any fixture valid status can be introduced.

## Review-Only Output Shape

Any future stop / TP / RR fixture output should expose only review fields:

- `stopFixtureStatus`
- `tpFixtureStatus`
- `rrFixtureStatus`
- `stopCandidateFamily`
- `tpCandidateFamily`
- `rrSourceType`
- `sourceWindow`
- `ruleId`
- `ruleVersion`
- `freshnessOwnership`
- `conflictFamilyOwnership`
- `numericSource.fixtureOnly`
- `sourceOwner`
- `sourceRef`
- `sourceTimeframe`
- `sourceReason`
- `entryFixtureDependencyRef`
- `manualReviewRequired=true`
- `notTradeInstruction=true`
- `reviewMode=REVIEW_ONLY`
- `blockingReasons`

The output must not expose trade-ready, order, execution, automation, or production valid surface.

## Still-Blocked Paths

The following paths remain blocked:

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
- production Java changes

## Boundary Confirmations

- P103 is stop / TP / RR fixture contract only.
- P103 is documentation-only.
- P103 does not add test-scope Java.
- P103 does not modify production Java.
- P103 does not generate real entry / stop / TP / RR values.
- P103 does not implement production candidate generation.
- P103 does not read runtime data.
- P103 does not read live market data.
- P103 does not wire BoundaryCandidateService `VALID` production path.
- P103 does not upgrade ExecutionPlan readiness.
- P103 does not modify `dashboard.html`.
- P103 does not modify schema.
- P103 does not modify config.
- P103 does not add controller/endpoint Java.
- P103 does not add external data integration.
- P103 does not add order API.
- P103 does not add execution API.
- P103 does not add scheduler / automation / auto-trading.
- Placeholder `docs/P103.md` is removed.

## Validation

P103 changes documentation only. No Java or test source was modified, so Maven was not required for this phase.

Required validation:

```text
git diff --check
```
