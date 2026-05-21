# BACKEND-P122 Read-Only Generator Authorization Checklist

## Baseline

- Branch context: PR #354 / Issue #353.
- Formal mainline title: BACKEND-P122 Read-Only Generator Authorization Checklist.
- PR title note: PR #354 uses a shortened title as a platform workaround; Issue #353 and this document preserve the formal mainline title.
- Baseline commit: `08ea008` (`chore: add P122 placeholder`), based on `69ec09f` (`P121 (#352)`).
- Scope: documentation-only authorization checklist for any future use of the read-only generator.
- Line context: this starts the D line, Production Authorization Preparation / Safety Gate.
- Placeholder removed: `docs/P122.md`.

## Files Changed

- `docs/PHASE_BACKEND_P122_READ_ONLY_GENERATOR_AUTHORIZATION_CHECKLIST.md`
- Removed `docs/P122.md`

No production Java, test source, runtime, dashboard, schema, config, controller, endpoint, readiness, service registration, order, execution, scheduler, automation, auto-trading, exchange-client, or external-data files are changed.

## P122 Scope Statement

P122 is an authorization-preparation checklist only.

P122 does not authorize production wiring. P122 does not authorize order, execution, scheduler, automation, or auto-trading. P122 does not authorize runtime data reads, live market reads, external data fetches, production `VALID`, ExecutionPlan readiness upgrades, dashboard mutation, schema/config/controller changes, endpoint Java, service registration, or Spring bean registration.

Any future production-adjacent line must receive explicit manual approval with a new issue, branch, allowed file set, still-blocked path list, validation plan, rollback plan, and PR body checklist.

## P114-P121 Prerequisite Confirmation

Before any future read-only generator work is considered, the reviewer must confirm the P114-P121 artifacts are present and unchanged except by separately authorized work:

- P114 snapshot DTO contract: `docs/PHASE_BACKEND_P114_MARKET_READ_ONLY_SNAPSHOT_DTO_CONTRACT_RESULT.md`
- P115 candidate result DTO contract: `docs/PHASE_BACKEND_P115_READ_ONLY_CANDIDATE_RESULT_DTO_CONTRACT_RESULT.md`
- P116 inert generator interface / skeleton: `docs/PHASE_BACKEND_P116_READ_ONLY_GENERATOR_INTERFACE_INERT_SKELETON_RESULT.md`
- P117 missing evidence fail-closed tests: `docs/PHASE_BACKEND_P117_FAIL_CLOSED_TESTS_FOR_MISSING_EVIDENCE_SOURCE_OWNER_RESULT.md`
- P118 forbidden / no-go / Risk Action Guard blocked tests: `docs/PHASE_BACKEND_P118_FORBIDDEN_INPUTS_NO_GO_EVIDENCE_BLOCKED_TESTS_RESULT.md`
- P119 fixture snapshot -> review-only candidate tests: `docs/PHASE_BACKEND_P119_FIXTURE_SNAPSHOT_REVIEW_ONLY_CANDIDATE_TEST_RESULT.md`
- P120 no runtime / no live market / no production `VALID` guard tests: `docs/PHASE_BACKEND_P120_NO_RUNTIME_NO_LIVE_MARKET_NO_PRODUCTION_VALID_GUARD_TEST_RESULT.md`
- P121 market read-only implementation line closure: `docs/PHASE_BACKEND_P121_MARKET_READ_ONLY_IMPLEMENTATION_LINE_CLOSURE.md`

The preserved chain remains:

```text
P114 snapshot DTO
-> P115 candidate result DTO
-> P116 inert generator
-> P117 missing evidence fail-closed tests
-> P118 blocked input tests
-> P119 review-only candidate tests
-> P120 final guard tests
-> P121 market read-only line closure
```

## Required Authorization Gates

Every future production-adjacent proposal involving the read-only generator must pass these gates before implementation begins.

### Gate 1: P114-P121 Artifacts Present

- Confirm every P114-P121 artifact exists.
- Confirm the P121 closure remains the current line boundary.
- Confirm the proposed work names the exact boundary being changed.
- Reject broad language such as "wire it", "make it production ready", "continue", or "enable trading" unless the issue explicitly enumerates allowed paths and still-blocked paths.

### Gate 2: No Runtime / Live / External Data Access

- Inputs must be already-ingested evidence only.
- No runtime data reads.
- No live market data reads.
- No external data fetches.
- No external data integration.
- No exchange clients.
- No `WebClient`.
- No `RestTemplate`.

### Gate 3: No Production VALID Mapping

- No production `VALID` mapping.
- No BoundaryCandidateService `VALID` production path.
- No `BoundaryCandidateDTO.valid(...)` calls.
- No production `BoundaryStatusEnum.VALID` mapping.
- Complete snapshots can only become `REVIEW_ONLY_CANDIDATE`.

### Gate 4: No Readiness Upgrade

- No ExecutionPlan readiness upgrade.
- No readiness surface or trade-ready equivalent.
- Review-only output cannot imply production readiness.

### Gate 5: No Dashboard / Schema / Config / Controller Changes

- No dashboard mutation.
- No `dashboard.html` changes.
- No schema changes.
- No config changes.
- No controller Java.
- No endpoint Java.

### Gate 6: No Order / Execution / Automation Surface

- No order API.
- No execution API.
- No scheduler behavior.
- No automation behavior.
- No auto-trading behavior.
- No buy / sell / open / close / reverse / signal surface.

### Gate 7: Outputs Remain Review-Only

Every output must keep:

- `manualReviewRequired=true`
- `notTradeInstruction=true`
- `reviewMode=REVIEW_ONLY`

Status rules remain:

- Complete already-ingested snapshot -> `REVIEW_ONLY_CANDIDATE` only.
- Missing evidence -> `INCOMPLETE`.
- Forbidden input blocker -> `BLOCKED`.
- No-go evidence blocker -> `BLOCKED`.
- Risk Action Guard blocker -> `BLOCKED`.

### Gate 8: Rollback Path Documented

Future work must document rollback before implementation:

- Identify the last known freeze point.
- Identify files allowed to be reverted.
- Confirm rollback returns the line to inert, non-Spring, non-wired, review-only behavior.
- Confirm rollback removes any accidental runtime/live/external access, production `VALID`, readiness, dashboard/schema/config/controller mutation, order/execution/scheduler/automation/auto-trading surface, or real value generation.

### Gate 9: Manual Approval Required

Manual approval is required before any future production-adjacent line.

The approval must:

- Name the exact boundary being changed.
- Name the allowed files.
- Name still-blocked paths.
- Define validation commands.
- Define rollback expectations.
- State whether Spring registration, service registration, endpoint work, schema/config changes, readiness, production `VALID`, runtime/live/external reads, or order/execution/automation are explicitly allowed.

If any of those items are not explicitly authorized, they remain blocked.

## Required Validation Commands Before Future Authorization

Before any future authorization is accepted, the proposer must run or explicitly schedule this validation set:

```text
./mvnw -q -Dtest=MarketReadOnlyNoRuntimeNoProductionValidGuardTest test
./mvnw -q -Dtest=MarketReadOnlyFixtureSnapshotReviewOnlyCandidateTest test
./mvnw -q -Dtest=MarketReadOnlyForbiddenInputBlockedTest test
./mvnw -q -Dtest=MarketReadOnlyMissingEvidenceFailClosedTest test
./mvnw -q -Dtest=InertMarketReadOnlyCandidateGeneratorTest test
./mvnw -q -Dtest=MarketReadOnlyEvidenceSnapshotDTOTest test
./mvnw -q -Dtest=MarketReadOnlyCandidateResultDTOTest test
./mvnw -q -DskipTests compile
./mvnw -q -DskipTests test-compile
git diff --check
```

If the future proposal touches production-adjacent paths, the same commands must run before and after the change. Any failure is a no-go trigger until fixed within the authorized scope or rolled back.

## Required Future PR Body Confirmations

Any future PR that proposes production-adjacent read-only generator work must include:

- Files changed.
- Exact boundary changed.
- Manual approval reference.
- Validation commands run.
- P114-P121 prerequisite confirmation.
- Authorization gate checklist results.
- Confirmation that inputs are already-ingested evidence only.
- Confirmation that complete snapshots remain `REVIEW_ONLY_CANDIDATE` only unless separately authorized.
- Confirmation that missing evidence remains `INCOMPLETE`.
- Confirmation that forbidden / no-go / Risk Action Guard blockers remain `BLOCKED`.
- Confirmation that `manualReviewRequired=true`, `notTradeInstruction=true`, and `reviewMode=REVIEW_ONLY` remain mandatory.
- Still-blocked paths.
- Rollback plan.
- Boundary confirmations.

## Rollback Expectations

If any no-go trigger appears, stop the phase and roll back to the last approved freeze point.

No-go triggers include:

- runtime data read
- live market read
- external data fetch
- exchange client
- `WebClient`
- `RestTemplate`
- production `VALID` mapping
- BoundaryCandidateService `VALID` production path
- `BoundaryCandidateDTO.valid(...)` call
- production `BoundaryStatusEnum.VALID` mapping
- ExecutionPlan readiness upgrade
- dashboard mutation
- `dashboard.html` change
- schema change
- config change
- controller / endpoint Java
- service registration
- Spring bean registration
- order API
- execution API
- scheduler / automation / auto-trading
- production candidate generation
- real entry / stop / TP / RR value generation

Rollback must preserve inert, non-Spring, non-wired, review-only behavior and restore the mandatory output flags.

## Still-Blocked Paths

The following paths remain blocked after P122:

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

- P122 is documentation-only authorization checklist work.
- P122 removes the placeholder `docs/P122.md`.
- P122 adds one authorization checklist document.
- P122 starts the D line, Production Authorization Preparation / Safety Gate.
- P122 does not authorize production wiring.
- P122 does not authorize order, execution, scheduler, automation, or auto-trading.
- P122 does not modify production Java.
- P122 does not modify test source.
- P122 does not implement production candidate generation.
- P122 does not generate real entry / stop / TP / RR values.
- P122 does not read runtime data.
- P122 does not read live market data.
- P122 does not fetch external data.
- P122 does not add exchange clients, `WebClient`, or `RestTemplate`.
- P122 does not wire BoundaryCandidateService `VALID` production path.
- P122 does not call `BoundaryCandidateDTO.valid(...)`.
- P122 does not map to production `BoundaryStatusEnum.VALID`.
- P122 does not upgrade ExecutionPlan readiness.
- P122 does not modify `dashboard.html`.
- P122 does not modify schema.
- P122 does not modify config.
- P122 does not add controller / endpoint Java.
- P122 does not add order API.
- P122 does not add execution API.
- P122 does not add scheduler / automation / auto-trading.

## Validation

P122 is documentation-only, so Maven was skipped. Validation is limited to git diff checks confirming the placeholder removal and authorization checklist document only.
