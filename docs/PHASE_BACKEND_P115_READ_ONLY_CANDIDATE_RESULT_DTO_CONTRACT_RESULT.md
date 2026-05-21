# BACKEND-P115 Read-Only Candidate Result DTO Contract Result

## Baseline

- Branch context: PR #340 / Issue #339.
- Formal mainline title: BACKEND-P115 Read-Only Candidate Result DTO Contract.
- PR title note: PR #340 uses the shortened title `P115 Candidate Result DTO` as a platform workaround.
- Baseline commit: `28effb9` (`chore: add P115 placeholder`), based on `32ff1b4` (`P114 Snapshot DTO (#338)`).
- Scope: inert DTO/enum contract and focused DTO tests for read-only candidate results derived from already-ingested snapshot contracts in future phases.
- Placeholder removed: `docs/P115.md`.

## Files Changed

- `src/main/java/org/example/trademodel/dto/planboundary/MarketReadOnlyCandidateResultDTO.java`
- `src/main/java/org/example/trademodel/dto/planboundary/MarketReadOnlyCandidateStatusEnum.java`
- `src/test/java/org/example/trademodel/dto/planboundary/MarketReadOnlyCandidateResultDTOTest.java`
- `docs/PHASE_BACKEND_P115_READ_ONLY_CANDIDATE_RESULT_DTO_CONTRACT_RESULT.md`
- Removed `docs/P115.md`

No service, generator, runtime, dashboard, schema, config, controller, endpoint, order, execution, scheduler, automation, auto-trading, or external-data files are changed.

## DTO Coverage

P115 adds an inert read-only candidate result DTO contract. The DTO represents review-only candidate context that may be derived from already-ingested P114 snapshot DTOs in future phases.

Candidate result statuses covered:

- `INCOMPLETE`
- `BLOCKED`
- `REVIEW_ONLY_CANDIDATE`

Result fields covered:

- `symbol`
- `timeframe`
- `snapshotStatus`
- `candidateStatus`
- `entryReview`
- `stopReview`
- `tpReview`
- `rrReview`
- `sourceOwnershipSummary`
- `numericSourceSummary`
- `freshnessStatus`
- `sourceWindow`
- `ruleVersion`
- `conflictFamilyStatus`
- `dataQualityScore`
- `riskActionGuardReview`
- `blockingReasons`
- `manualReviewRequired`
- `notTradeInstruction`
- `reviewMode`

Entry, stop, TP, RR, source ownership, numeric source, and Risk Action Guard review fields are string/token fields only. They are not prices, not generated values, not production candidate outputs, and not trade instructions.

Every DTO output keeps:

- `manualReviewRequired=true`
- `notTradeInstruction=true`
- `reviewMode=REVIEW_ONLY`

`REVIEW_ONLY_CANDIDATE` is review-only candidate context. It is not production `VALID`, not readiness, not a trade instruction, not candidate generation, and not production wiring.

## Status Rules

Focused tests cover these status rules:

- Missing snapshot -> `INCOMPLETE`.
- Snapshot `INCOMPLETE` -> candidate `INCOMPLETE`.
- Snapshot `BLOCKED` -> candidate `BLOCKED`.
- Forbidden input blocker evidence -> `BLOCKED`.
- No-go blocker evidence -> `BLOCKED`.
- Risk Action Guard blocker evidence -> `BLOCKED`.
- Complete snapshot may produce `REVIEW_ONLY_CANDIDATE` only.
- `REVIEW_ONLY_CANDIDATE` does not map to production `VALID`.
- `REVIEW_ONLY_CANDIDATE` does not upgrade readiness.
- `REVIEW_ONLY_CANDIDATE` is not a trade instruction.

Missing snapshot, snapshot missing-field evidence, snapshot blocker evidence, and direct blocker evidence are preserved in `blockingReasons` for review-only inspection.

## Guard Coverage

P115 focused tests guard the DTO and enum against accidental production surface:

- DTO source does not call `BoundaryCandidateDTO.valid(...)`.
- DTO source does not reference `BoundaryStatusEnum.VALID`.
- DTO does not return or accept production `BoundaryCandidateDTO`.
- DTO does not expose `BigDecimal` real trading value fields, parameters, or returns.
- DTO review fields remain strings/tokens only.
- DTO does not expose trade-ready, order, execution, automation, auto-trading, open, close, reverse, signal, buy, sell, generated entry, generated stop, generated take-profit, generated RR, stop value, take-profit value, or risk-reward value surface.
- DTO and enum have no Spring annotations.
- DTO and enum have no controller / endpoint annotations.
- DTO source contains no runtime/live/external data fetch API terms.
- `blockingReasons` is defensively copied on input and output.

## Tests Run

```text
./mvnw -q -Dtest=MarketReadOnlyCandidateResultDTOTest test
./mvnw -q -Dtest=MarketReadOnlyEvidenceSnapshotDTOTest test
./mvnw -q -DskipTests compile
./mvnw -q -DskipTests test-compile
```

All commands passed.

## Still-Blocked Paths

The following paths remain blocked after P115:

- candidate generator implementation
- real entry / stop / TP / RR value generation
- runtime data reads
- live market data reads
- external data fetches
- external data integration
- production `VALID` mapping
- BoundaryCandidateService `VALID` production path
- `BoundaryCandidateDTO.valid(...)` calls
- production `BoundaryStatusEnum.VALID` mapping
- ExecutionPlan readiness upgrade
- dashboard mutation
- `dashboard.html` changes
- schema changes
- config changes
- controller / endpoint Java
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

- P115 adds DTO/enum contract classes only.
- P115 DTOs are inert, non-Spring, non-wired, and review-only.
- P115 adds focused DTO tests only under `src/test/java`.
- P115 does not implement a candidate generator.
- P115 does not generate real entry / stop / TP / RR values.
- P115 does not read runtime data.
- P115 does not read live market data.
- P115 does not fetch external data.
- P115 does not add external data integration.
- P115 does not map to production `VALID`.
- P115 does not wire BoundaryCandidateService `VALID` production path.
- P115 does not call `BoundaryCandidateDTO.valid(...)`.
- P115 does not map to production `BoundaryStatusEnum.VALID`.
- P115 does not upgrade ExecutionPlan readiness.
- P115 does not modify `dashboard.html`.
- P115 does not modify schema.
- P115 does not modify config.
- P115 does not add controller / endpoint Java.
- P115 does not add order API.
- P115 does not add execution API.
- P115 does not add scheduler / automation / auto-trading.
- Placeholder `docs/P115.md` is removed.
