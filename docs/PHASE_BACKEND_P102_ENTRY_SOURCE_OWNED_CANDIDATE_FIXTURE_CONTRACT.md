# BACKEND-P102 Entry Source-Owned Candidate Fixture Contract

## Baseline

- Branch context: PR #314 / Issue #313.
- Formal mainline title: BACKEND-P102 Entry Source-Owned Candidate Fixture Contract.
- PR title note: PR #314 uses the shortened title `P102 Entry Fixture` as a platform workaround.
- Baseline commit: `dcbfdd6` (`chore: add P102 placeholder`), based on `bd835a6` (`BACKEND-P101 Entry Stop TP Candidate Design (#312)`).
- Scope: documentation-only entry source-owned candidate fixture contract.
- Placeholder removed: `docs/P102.md`.

## Files Changed

- `docs/PHASE_BACKEND_P102_ENTRY_SOURCE_OWNED_CANDIDATE_FIXTURE_CONTRACT.md`
- Removed `docs/P102.md`

No Java, test, schema, dashboard, config, controller, endpoint, runtime, adapter, readiness, order, execution, automation, or external-data files are changed.

## Fixture-Only Scope

P102 defines a fixture-only contract for entry source-owned candidates.

This phase allows future entry candidate fixtures to be tested without:

- generating real entry prices
- generating real stop / TP / RR values
- reading runtime data
- reading live market data
- wiring production candidate generation
- wiring BoundaryCandidateService `VALID` production path
- upgrading ExecutionPlan readiness
- changing dashboard, schema, config, or controller code
- changing production Java

P102 does not create test-scope Java. It documents the contract that a later fixture-only implementation may use.

## Contract Purpose

The entry fixture contract proves that an entry candidate can be represented only when the entry is owned by source evidence and remains review-only.

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

## Entry Candidate Fixture Statuses

Allowed fixture statuses:

- `INCOMPLETE`
- `BLOCKED`
- `FIXTURE_VALID_CANDIDATE`

No other status is authorized by P102.

### INCOMPLETE

Use `INCOMPLETE` when required source-owned evidence is missing, stale-but-not-forbidden, ambiguous, or not fully evaluated.

`INCOMPLETE` must preserve missing-field or blocker evidence and must keep:

- `manualReviewRequired=true`
- `notTradeInstruction=true`
- `reviewMode=REVIEW_ONLY`

### BLOCKED

Use `BLOCKED` when the fixture contains forbidden source evidence or an explicit safety blocker.

`BLOCKED` must preserve field-specific blocker evidence and must keep:

- `manualReviewRequired=true`
- `notTradeInstruction=true`
- `reviewMode=REVIEW_ONLY`

### FIXTURE_VALID_CANDIDATE

Use `FIXTURE_VALID_CANDIDATE` only in a later fixture-only test phase when every required entry source-owned field is present, fixture-only numeric ownership is present, source freshness is acceptable, conflict family validation is complete, and no blocker is present.

`FIXTURE_VALID_CANDIDATE` still must keep:

- `manualReviewRequired=true`
- `notTradeInstruction=true`
- `reviewMode=REVIEW_ONLY`

`FIXTURE_VALID_CANDIDATE` must not:

- map to production `BoundaryStatusEnum.VALID`
- wire BoundaryCandidateService `VALID`
- generate a real entry price
- upgrade ExecutionPlan readiness
- mutate dashboard, schema, or config
- create order, execution, scheduler, automation, external-data, or auto-trading behavior

## Entry Candidate Fixture Contract Fields

Required top-level fields:

| Field | Requirement |
| --- | --- |
| `symbol` | Required fixture symbol identifier. Must not be sourced from runtime reads in P102. |
| `timeframe` | Required fixture timeframe. Must align with entry source timeframe. |
| `entryCandidateFamily` | Required. Must be one of the allowed entry families. |
| `entryFixtureStatus` | Required. One of `INCOMPLETE`, `BLOCKED`, `FIXTURE_VALID_CANDIDATE`. |
| `manualReviewRequired` | Required `true`. |
| `notTradeInstruction` | Required `true`. |
| `reviewMode` | Required `REVIEW_ONLY`. |
| `blockingReasons` | Required list. Empty only for fixture-only valid candidates. |
| `sourceOwner` | Required before fixture-only valid status. |
| `entrySourceType` | Required before fixture-only valid status. |
| `entrySourceTimeframe` | Required before fixture-only valid status. |
| `entrySourceReason` | Required before fixture-only valid status. |
| `entrySourceRef` | Required before fixture-only valid status. |
| `sourceWindow` | Required before fixture-only valid status. |
| `ruleId` | Required before fixture-only valid status. |
| `ruleVersion` | Required before fixture-only valid status. |
| `freshnessOwnership` | Required before fixture-only valid status. |
| `conflictFamilyOwnership` | Required before fixture-only valid status. |
| `numericSource` | Required before fixture-only valid status. Fixture-only only. |

## Required Source-Owned Fields

The following fields are mandatory for any fixture-only `FIXTURE_VALID_CANDIDATE`:

- `symbol`
- `timeframe`
- `entrySourceType`
- `entrySourceTimeframe`
- `entrySourceReason`
- `entrySourceRef`
- `sourceWindow`
- `ruleId`
- `ruleVersion`
- `freshnessOwnership`
- `conflictFamilyOwnership`
- `numericSource`
- `sourceOwner`

Missing any required source-owned field keeps the fixture out of `FIXTURE_VALID_CANDIDATE`.

## Entry Families

Allowed entry families:

- `STRUCTURE_CONFIRMATION_ZONE`
- `BREAKOUT_RETEST_ZONE`
- `SUPPORT_RESISTANCE_FLIP_ZONE`

### STRUCTURE_CONFIRMATION_ZONE

Source-owned fixture contract:

- `entrySourceType=STRUCTURE_CONFIRMATION_ZONE`
- source owner identifies confirmed market structure evidence
- `entrySourceRef` points to fixture evidence for structure hold, reaction, or retest
- `sourceWindow` identifies the fixture evidence window
- `ruleId` and `ruleVersion` identify the fixture rule
- `freshnessOwnership` confirms the fixture evidence is not stale
- `conflictFamilyOwnership` confirms no unresolved entry conflict
- `numericSource` is fixture-only

### BREAKOUT_RETEST_ZONE

Source-owned fixture contract:

- `entrySourceType=BREAKOUT_RETEST_ZONE`
- source owner identifies breakout and retest evidence
- `entrySourceRef` points to fixture breakout/retest evidence
- `sourceWindow` identifies the fixture evidence window
- `ruleId` and `ruleVersion` identify the fixture rule
- `freshnessOwnership` confirms the fixture evidence is not stale
- `conflictFamilyOwnership` confirms no unresolved breakout/retest conflict
- `numericSource` is fixture-only

### SUPPORT_RESISTANCE_FLIP_ZONE

Source-owned fixture contract:

- `entrySourceType=SUPPORT_RESISTANCE_FLIP_ZONE`
- source owner identifies prior support/resistance and flip confirmation evidence
- `entrySourceRef` points to fixture flip evidence
- `sourceWindow` identifies the fixture evidence window
- `ruleId` and `ruleVersion` identify the fixture rule
- `freshnessOwnership` confirms the fixture evidence is not stale
- `conflictFamilyOwnership` confirms no unresolved support/resistance conflict
- `numericSource` is fixture-only

## Numeric Source Contract

`numericSource` is required before `FIXTURE_VALID_CANDIDATE`, but it must be fixture-only.

Required numeric source fields:

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

Forbidden numeric source value categories:

- real market values
- latest-price-only values
- runtime values
- live market data values
- order-derived values
- execution-derived values
- dashboard-derived values
- AI-text-derived values

P102 does not define any real numeric value. P102 does not generate an entry price.

## Fixture Status Rules

### Missing Source Owner

If `sourceOwner` is missing, blank, unsupported, or ambiguous:

```text
entryFixtureStatus=INCOMPLETE
```

Required blocker evidence:

- `sourceOwner`
- `missing_source_owner`

### Missing Numeric Source

If `numericSource` is missing or lacks fixture-only ownership:

```text
entryFixtureStatus=INCOMPLETE
```

Required blocker evidence:

- `numericSource`
- `missing_numeric_source`

### Stale Source Window

If `sourceWindow` is stale but the evidence is otherwise source-owned:

```text
entryFixtureStatus=INCOMPLETE
```

If stale source window is paired with unsafe, contradictory, or forbidden evidence:

```text
entryFixtureStatus=BLOCKED
```

Required blocker evidence:

- `sourceWindow`
- `stale_source_window`

### Unsupported Source Family

If `entryCandidateFamily` or `entrySourceType` is outside the allowed entry families:

```text
entryFixtureStatus=BLOCKED
```

Required blocker evidence:

- `entryCandidateFamily`
- `unsupported_source_family`

### Risk Action Guard Blockers

If Risk Action Guard evidence blocks completion:

```text
entryFixtureStatus=BLOCKED
```

Risk Action Guard blockers remain review-only and must block fixture valid status.

Required blocker evidence:

- `riskActionGuard`
- specific Risk Action Guard token

## Blocked Sources

The following sources always force `BLOCKED`:

| Source | Required blocker evidence |
| --- | --- |
| AI text directly generating entry | `ai_text_entry_source` |
| Dashboard text directly generating entry | `dashboard_text_entry_source` |
| Latest price only | `latest_price_only_entry_source` |
| Single kline only | `single_kline_only_entry_source` |
| Aggregate score only | `aggregate_score_only_entry_source` |
| Order / execution backfill | `order_execution_backfill_entry_source` |
| Strong reversal directly becoming reverse entry | `strong_reversal_direct_reverse_entry` |
| Wick / pin-bar directly becoming trend reversal | `wick_pin_bar_direct_trend_reversal` |

Blocked sources must not be downgraded into `INCOMPLETE` if their unsafe source type is known. They must remain review-only blockers.

## Fixture-Only Valid Candidate Requirements

`FIXTURE_VALID_CANDIDATE` is allowed only when every item below is true:

- `symbol` is present
- `timeframe` is present
- `entryCandidateFamily` is allowed
- `entrySourceType` is allowed
- `entrySourceTimeframe` is present and compatible
- `entrySourceReason` is present and non-instructional
- `entrySourceRef` is present and unambiguous
- `sourceOwner` is present and supported
- `sourceWindow` is present and fresh
- `ruleId` is present
- `ruleVersion` is present and current for the fixture
- `freshnessOwnership` is present and passing
- `conflictFamilyOwnership` is present and passing
- `numericSource` is present
- `numericSource.fixtureOnly=true`
- no forbidden source is present
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

Any future entry fixture output should expose only review fields:

- `entryFixtureStatus`
- `entryCandidateFamily`
- `entrySourceType`
- `entrySourceTimeframe`
- `entrySourceReason`
- `entrySourceRef`
- `sourceWindow`
- `ruleId`
- `ruleVersion`
- `freshnessOwnership`
- `conflictFamilyOwnership`
- `numericSource.fixtureOnly`
- `sourceOwner`
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

- P102 is entry fixture contract only.
- P102 is documentation-only.
- P102 does not add test-scope Java.
- P102 does not modify production Java.
- P102 does not generate real entry / stop / TP / RR values.
- P102 does not implement production candidate generation.
- P102 does not read runtime data.
- P102 does not read live market data.
- P102 does not wire BoundaryCandidateService `VALID` production path.
- P102 does not upgrade ExecutionPlan readiness.
- P102 does not modify `dashboard.html`.
- P102 does not modify schema.
- P102 does not modify config.
- P102 does not add controller/endpoint Java.
- P102 does not add external data integration.
- P102 does not add order API.
- P102 does not add execution API.
- P102 does not add scheduler / automation / auto-trading.
- Placeholder `docs/P102.md` is removed.

## Validation

P102 changes documentation only. No Java or test source was modified, so Maven was not required for this phase.

Required validation:

```text
git diff --check
```
