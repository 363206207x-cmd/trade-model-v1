# BACKEND-P109 Stop TP RR Source-Owned Candidate Fixture Test Helper Result

## Baseline

- Branch context: PR #328 / Issue #327.
- Formal mainline title: BACKEND-P109 Stop TP RR Source-Owned Candidate Fixture Test Helper.
- PR title note: PR #328 uses the shortened title `P109 Stop TP RR Fixture Helper` as a platform workaround.
- Baseline commit: `f265bc8` (`chore: add P109 placeholder`), based on `2419909` (`P108 Entry Fixture Helper (#326)`).
- Scope: test-scope stop / TP / RR source-owned candidate fixture helper and focused tests.
- Placeholder removed: `docs/P109.md`.

## Files Changed

- `src/test/java/org/example/trademodel/dto/planboundary/StopTpRrSourceOwnedCandidateFixtureHelper.java`
- `src/test/java/org/example/trademodel/dto/planboundary/StopTpRrSourceOwnedCandidateFixtureHelperTest.java`
- `docs/PHASE_BACKEND_P109_STOP_TP_RR_SOURCE_OWNED_CANDIDATE_FIXTURE_TEST_HELPER_RESULT.md`
- Removed `docs/P109.md`

No production Java, runtime, dashboard, schema, config, controller, endpoint, adapter, readiness, order, execution, scheduler, automation, auto-trading, or external-data files are changed.

## Helper Coverage

P109 adds a test-scope helper that represents stop, TP, and RR source-owned candidate fixture outcomes without production candidate generation.

Covered fixture statuses:

- `INCOMPLETE`
- `BLOCKED`
- `FIXTURE_VALID_CANDIDATE`

Covered stop family:

- `STRUCTURAL_INVALIDATION_WITH_BUFFER`

Covered TP families:

- `STRUCTURE_TARGET`
- `LIQUIDITY_TARGET`
- `PRIOR_HIGH_LOW`
- `RR_LADDER`

Covered RR dependency requirements:

- RR requires the P108 entry fixture dependency.
- RR requires a stop fixture dependency.
- RR requires a TP fixture dependency.
- RR fails closed when entry, stop, or TP dependency is missing, incomplete, or blocked.
- RR fails closed when entry-stop distance is missing, zero, negative, ambiguous, stale, or unsupported.
- RR fails closed when TP ownership is missing.

The helper keeps numeric source values as fixture tokens only. It does not use real entry, stop, TP, or RR values; live market data; runtime data; or order/execution-derived values.

## Status Rules

Implemented focused test-scope status rules:

- Missing stop owner -> `INCOMPLETE` with `stopSourceOwner` and `missing_stop_owner` evidence.
- Missing TP owner -> `INCOMPLETE` with `tpSourceOwner` and `missing_tp_owner` evidence.
- Missing RR owner -> `INCOMPLETE` with `rrSourceOwner` and `missing_rr_owner` evidence.
- Missing numeric source -> `INCOMPLETE` with numeric source blocker evidence.
- Missing entry fixture dependency -> `INCOMPLETE` with `missing_entry_fixture_dependency` evidence.
- Missing stop or TP fixture dependency -> `INCOMPLETE` with dependency evidence.
- Incomplete entry, stop, or TP dependency -> `INCOMPLETE` while preserving dependency evidence.
- Blocked entry, stop, or TP dependency -> `BLOCKED` while preserving dependency evidence.
- Stale source window without unsafe evidence -> `INCOMPLETE`.
- Stale source window with unsafe evidence -> `BLOCKED`.
- Unsupported source family -> `BLOCKED` with field-specific evidence.
- Entry-stop inversion -> `BLOCKED`.
- Entry-TP direction conflict -> `BLOCKED`.
- Stop-TP overlap -> `BLOCKED`.
- Forbidden source -> `BLOCKED` with blocker evidence.
- Risk Action Guard blocker -> `BLOCKED` with review-only blocker evidence.

Every fixture output keeps:

- `manualReviewRequired=true`
- `notTradeInstruction=true`
- `reviewMode=REVIEW_ONLY`

Fixture-valid output remains fixture-only. It does not map to production `BoundaryStatusEnum.VALID`, does not call `BoundaryCandidateDTO.valid(...)`, and does not expose trade-ready, order, execution, or automation surface.

## Blocked Sources And Conflicts

P109 covers these forbidden sources as blockers:

- AI text
- dashboard text
- latest price only
- single kline only
- aggregate score only
- order / execution backfill
- strong reversal direct reverse
- wick / pin-bar direct trend reversal
- liquidity stress / stampede opportunity push

P109 covers these conflicts as blockers:

- entry-stop inversion
- entry-TP direction conflict
- stop-TP overlap
- blocked P108 entry fixture dependency
- blocked stop fixture dependency
- blocked TP fixture dependency
- Risk Action Guard blocker

Blocked source and conflict evidence is preserved in the fixture result and never becomes candidate ownership.

## Tests Run

```text
./mvnw -q -Dtest=StopTpRrSourceOwnedCandidateFixtureHelperTest test
./mvnw -q -Dtest=EntrySourceOwnedCandidateFixtureHelperTest test
./mvnw -q -DskipTests compile
./mvnw -q -DskipTests test-compile
```

All commands passed.

## Still-Blocked Paths

The following paths remain blocked after P109:

- production Java changes
- real entry / stop / TP / RR value generation
- production candidate generation
- runtime data reads
- live market data reads
- external data fetches
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

- P109 adds test-scope stop / TP / RR fixture helper only.
- P109 adds focused tests only under `src/test/java`.
- P109 depends on the P108 entry fixture helper only in test scope.
- P109 does not modify production Java.
- P109 does not generate real entry / stop / TP / RR values.
- P109 does not implement production candidate generation.
- P109 does not read runtime data.
- P109 does not read live market data.
- P109 does not fetch external data.
- P109 does not wire BoundaryCandidateService `VALID` production path.
- P109 does not upgrade ExecutionPlan readiness.
- P109 does not modify `dashboard.html`.
- P109 does not modify schema.
- P109 does not modify config.
- P109 does not add controller / endpoint Java.
- P109 does not add external data integration.
- P109 does not add order API.
- P109 does not add execution API.
- P109 does not add scheduler / automation / auto-trading.
- Placeholder `docs/P109.md` is removed.
