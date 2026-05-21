# BACKEND-P117 Fail-Closed Tests for Missing Evidence / Missing Source Owner Result

## Baseline

- Branch context: PR #344 / Issue #343.
- Formal mainline title: BACKEND-P117 Fail-Closed Tests for Missing Evidence / Missing Source Owner.
- PR title note: PR #344 uses a shortened title as a platform workaround.
- Baseline commit: `2fe63df` (`chore: add P117 placeholder`), based on `23e7947` (`P116 Inert Generator (#342)`).
- Scope: focused test-only fail-closed coverage for missing evidence and missing source ownership in the inert read-only generator and DTO contracts.
- Placeholder removed: `docs/P117.md`.

## Files Changed

- `src/test/java/org/example/trademodel/dto/planboundary/MarketReadOnlyMissingEvidenceFailClosedTest.java`
- `docs/PHASE_BACKEND_P117_FAIL_CLOSED_TESTS_FOR_MISSING_EVIDENCE_SOURCE_OWNER_RESULT.md`
- Removed `docs/P117.md`

No production Java, service registration, runtime, dashboard, schema, config, controller, endpoint, order, execution, scheduler, automation, auto-trading, or external-data files are changed.

## Fail-Closed Coverage

P117 adds focused tests proving the P114 snapshot DTO, P115 candidate result DTO, and P116 inert generator remain fail-closed when required evidence or source ownership is missing.

Covered fail-closed outcomes:

- Null snapshot -> `INCOMPLETE` with `missing_snapshot`.
- Snapshot missing-field evidence is preserved in candidate `blockingReasons`.
- All missing-evidence outputs remain `INCOMPLETE`.
- No missing-evidence output becomes `REVIEW_ONLY_CANDIDATE`.
- No output maps to production `VALID`.
- No output implies readiness.
- All outputs remain review-only.

Every tested output keeps:

- `manualReviewRequired=true`
- `notTradeInstruction=true`
- `reviewMode=REVIEW_ONLY`

## Missing Evidence / Source Owner Coverage

P117 covers these missing or incomplete fields:

- Missing `evidenceRefs` -> `INCOMPLETE`.
- Empty `evidenceRefs` -> `INCOMPLETE`.
- Missing `evidenceFamilies` -> `INCOMPLETE`.
- Missing `sourceOwner` -> `INCOMPLETE`.
- Missing `sourceRef` -> `INCOMPLETE`.
- Missing `sourceTimeframe` -> `INCOMPLETE`.
- Missing `sourceReason` -> `INCOMPLETE`.
- Missing `sourceWindow` -> `INCOMPLETE`.
- Missing `ruleId` -> `INCOMPLETE`.
- Missing `ruleVersion` -> `INCOMPLETE`.
- Missing `freshnessStatus` -> `INCOMPLETE`.
- Missing `dataQualityScore` -> `INCOMPLETE`.
- Missing event evidence status -> `INCOMPLETE`.
- Missing liquidity evidence status -> `INCOMPLETE`.
- Missing wick / pin-bar evidence status -> `INCOMPLETE`.
- Missing multi-timeframe evidence status -> `INCOMPLETE`.
- Explicit `MISSING` event evidence status -> `INCOMPLETE`.
- Explicit `MISSING` liquidity evidence status -> `INCOMPLETE`.
- Explicit `MISSING` wick / pin-bar evidence status -> `INCOMPLETE`.
- Explicit `MISSING` multi-timeframe evidence status -> `INCOMPLETE`.

Each case asserts the field is preserved through snapshot `missingFields` and candidate `blockingReasons`.

## Guard Coverage

P117 tests retain the guard boundary for the P114-P116 contracts:

- No Spring annotations.
- No service/component/repository/controller/restcontroller/configuration annotations.
- No endpoint annotations.
- No runtime/live/external data API terms.
- No exchange clients.
- No `WebClient` or `RestTemplate`.
- No `BigDecimal` real-value fields, parameters, or returns.
- No generated entry / stop / TP / RR fields.
- No buy / sell / open / close / reverse / signal fields.
- No trade-ready / order / execution / automation / auto-trading surface.
- No `BoundaryCandidateDTO.valid(...)` calls.
- No production `BoundaryStatusEnum.VALID` mapping.

## Tests Run

```text
./mvnw -q -Dtest=MarketReadOnlyMissingEvidenceFailClosedTest test
./mvnw -q -Dtest=InertMarketReadOnlyCandidateGeneratorTest test
./mvnw -q -Dtest=MarketReadOnlyEvidenceSnapshotDTOTest test
./mvnw -q -Dtest=MarketReadOnlyCandidateResultDTOTest test
./mvnw -q -DskipTests compile
./mvnw -q -DskipTests test-compile
```

All commands passed.

## Still-Blocked Paths

The following paths remain blocked after P117:

- production candidate generation
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
- `BoundaryCandidateDTO.valid(...)` calls
- production `BoundaryStatusEnum.VALID` mapping
- ExecutionPlan readiness upgrade
- dashboard mutation
- `dashboard.html` changes
- schema changes
- config changes
- controller / endpoint Java
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

## Boundary Confirmations

- P117 is test-focused fail-closed coverage only.
- P117 adds tests only under `src/test/java`.
- P117 does not modify production Java.
- P117 does not add Spring annotations.
- P117 does not add service registration.
- P117 does not add endpoint annotations.
- P117 does not implement production candidate generation.
- P117 does not generate real entry / stop / TP / RR values.
- P117 does not read runtime data.
- P117 does not read live market data.
- P117 does not fetch external data.
- P117 does not add exchange clients, `WebClient`, or `RestTemplate`.
- P117 does not wire BoundaryCandidateService `VALID` production path.
- P117 does not call `BoundaryCandidateDTO.valid(...)`.
- P117 does not map to production `BoundaryStatusEnum.VALID`.
- P117 does not upgrade ExecutionPlan readiness.
- P117 does not modify `dashboard.html`.
- P117 does not modify schema.
- P117 does not modify config.
- P117 does not add controller / endpoint Java.
- P117 does not add order API.
- P117 does not add execution API.
- P117 does not add scheduler / automation / auto-trading.
- Placeholder `docs/P117.md` is removed.
