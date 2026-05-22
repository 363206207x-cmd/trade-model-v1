# BACKEND-P145 Source-Owned Candidate Test Authorization Gate

## 1. Baseline

- Branch context: PR #404 / Issue #403.
- Formal mainline title: BACKEND-P145 Source-Owned Candidate Test Authorization Gate.
- PR title note: PR #404 uses a shortened title as a platform workaround; Issue #403 and this document preserve the formal mainline title.
- Baseline commit: `6ddeef4` (`P144 Test Plan (#402)`).
- Scope: documentation-only source-owned candidate test authorization gate.
- Placeholder removed: `docs/P145.md`.

P144 created the source-owned candidate test plan and explicitly did not authorize adding tests. P145 narrows the next possible work to one future P146 test group only.

## 2. Scope And Non-Authorization

P145 authorizes only a future P146 test group:

```text
SourceOwnedCandidateIncompleteGuardTest
```

P145 does not add Java. P145 does not add test source. P145 does not modify `dashboard.html`. P145 does not add controller, endpoint, API, schema, config, service, or mapper changes.

P145 does not authorize:

- production implementation
- source-owned runtime candidate generation
- runtime SourceTrace field population
- real entry / stop / TP / RR value generation
- BoundaryCandidateService production `VALID` path
- `BoundaryCandidateDTO.valid(...)` calls in new production flows
- production `BoundaryStatusEnum.VALID` mapping
- ExecutionPlan readiness upgrade
- dashboard readiness mutation
- runtime/live/external data reads
- order / execution / scheduler / automation / auto-trading

## 3. Authorization Coverage

P145 authorizes only future test planning for `INCOMPLETE` fail-closed behavior. The future P146 test must prove that missing source-owned evidence cannot leave `INCOMPLETE`.

P145 does not authorize `BLOCKED` tests, substitution blocked tests, Risk Action Guard implementation tests, SourceTrace runtime population tests, production `VALID` tests, readiness tests, dashboard tests, or order / execution / scheduler / automation / auto-trading tests.

## 4. Allowed P146 File

Future P146 may add one focused test file only:

```text
src/test/java/org/example/trademodel/dto/planboundary/SourceOwnedCandidateIncompleteGuardTest.java
```

If repository package conventions require a closest equivalent at the time P146 is implemented, that future issue must name the equivalent path explicitly and keep the same one-test-file limit.

Future P146 may also add one P146 result document and remove the P146 placeholder.

## 5. Forbidden P146 Files

Future P146 must not modify:

- production Java
- `src/main/resources/templates/dashboard.html`
- dashboard UI code beyond the P130 static skeleton
- controller Java
- endpoint Java
- API wiring
- schema files
- config files
- service files
- mapper files
- runtime data readers
- live market data readers
- external data integration
- exchange clients
- `WebClient`
- `RestTemplate`
- order files
- execution files
- scheduler files
- automation files
- auto-trading files

Future P146 must not add additional test files beyond the one authorized `SourceOwnedCandidateIncompleteGuardTest` path.

## 6. Allowed P146 Assertions

Future P146 may assert `INCOMPLETE` fail-closed behavior only.

Allowed future assertions:

- missing source owner -> `INCOMPLETE`
- missing source ref -> `INCOMPLETE`
- missing source timeframe -> `INCOMPLETE`
- missing source window -> `INCOMPLETE`
- missing observed time -> `INCOMPLETE`
- missing decision time relationship -> `INCOMPLETE`
- missing freshness -> `INCOMPLETE`
- missing OHLCV / kline context -> `INCOMPLETE`
- missing persisted OHLCV readiness metadata -> `INCOMPLETE`
- missing data quality score -> `INCOMPLETE`
- missing data quality score owner -> `INCOMPLETE`
- insufficient evidence completeness -> `INCOMPLETE`
- incomplete SourceTrace -> `INCOMPLETE`
- incomplete numeric source ownership -> `INCOMPLETE`
- missing entry source reason -> `INCOMPLETE`
- missing stop source reason -> `INCOMPLETE`
- missing TP source reason -> `INCOMPLETE`
- missing RR rule ref -> `INCOMPLETE`
- missing rule id -> `INCOMPLETE`
- missing rule version -> `INCOMPLETE`
- missing conflict family state -> `INCOMPLETE`
- missing liquidity evidence -> `INCOMPLETE`
- missing multi-timeframe evidence -> `INCOMPLETE`
- missing event evidence status -> `INCOMPLETE`
- missing wick evidence status -> `INCOMPLETE`
- missing rollback-safe evidence trail -> `INCOMPLETE`
- runtime SourceTrace not populated from source-owned evidence -> `INCOMPLETE`

Future P146 must preserve:

- `manualReviewRequired=true`
- `notTradeInstruction=true`
- `reviewMode=REVIEW_ONLY` if such a field exists in the test target
- no production `VALID`
- no readiness
- no executable state
- no trade instruction
- no order / execution / automation surface

## 7. Forbidden P146 Assertions

Future P146 must not test or assert:

- `VALID` candidate generation
- `BoundaryCandidateDTO.valid(...)`
- real entry / stop / TP / RR values
- ExecutionPlan readiness
- dashboard readiness
- order API
- execution API
- scheduler behavior
- automation behavior
- auto-trading behavior
- runtime data reads
- live market data reads
- external data reads
- production candidate generation
- source-owned runtime candidate generation
- BoundaryCandidateService production `VALID` path

## 8. Required P146 Test Scenarios

The future P146 test should cover missing source-owned evidence scenarios only. It should remain deterministic, narrow, and focused on existing DTO / plan-boundary behavior available at that time.

Required scenario set:

- source ownership omitted
- source reference omitted
- source timeframe omitted
- source window omitted
- observed time omitted
- decision time relationship omitted
- freshness omitted
- OHLCV / kline context omitted
- persisted OHLCV readiness metadata omitted
- data quality score omitted
- data quality score owner omitted
- evidence completeness insufficient
- SourceTrace incomplete
- numeric source ownership incomplete
- entry source reason omitted
- stop source reason omitted
- TP source reason omitted
- RR rule reference omitted
- rule id omitted
- rule version omitted
- conflict family state omitted
- liquidity evidence omitted
- multi-timeframe evidence omitted
- event evidence status omitted
- wick evidence status omitted
- rollback-safe evidence trail omitted
- runtime SourceTrace not populated from source-owned evidence

All scenarios must fail closed to `INCOMPLETE`. They must not promote to `VALID`, readiness, executable state, or action behavior.

## 9. P146 Validation Commands

Future P146 must run:

```text
./mvnw -q -Dtest=SourceOwnedCandidateIncompleteGuardTest test
./mvnw -q -DskipTests compile
./mvnw -q -DskipTests test-compile
git diff --check
```

If the future test class name differs because a future issue explicitly authorizes a package-consistent equivalent, P146 must run the actual focused test class.

## 10. P146 Rollback Expectations

Rollback for future P146 is limited to:

- remove the one P146 test file
- remove the one P146 result document
- restore the P146 placeholder only if the PR is abandoned before merge

Rollback must not touch production Java, `dashboard.html`, controller, endpoint, API, schema, config, service, mapper, runtime data readers, live market data readers, external data integration, order, execution, scheduler, automation, or auto-trading paths.

If future P146 expands beyond the one authorized `INCOMPLETE` guard test, rollback must remove the extra scope and return to the P145 authorization boundary.

## 11. Still-Blocked Paths

The following paths remain blocked after P145:

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

## 12. Boundary Confirmations

- P145 is documentation-only authorization gate work.
- P145 removes the placeholder `docs/P145.md`.
- P145 adds one source-owned candidate test authorization gate document.
- P145 does not modify production Java.
- P145 does not modify test source.
- P145 does not modify `dashboard.html`.
- P145 does not add dashboard UI code.
- P145 does not add controller / endpoint / API / schema / config / service / mapper changes.
- P145 does not read runtime data.
- P145 does not read live market data.
- P145 does not fetch external data.
- P145 does not generate real entry / stop / TP / RR values.
- P145 does not upgrade ExecutionPlan readiness.
- P145 does not map to production `VALID`.
- P145 does not wire BoundaryCandidateService `VALID` production path.
- P145 does not call `BoundaryCandidateDTO.valid(...)`.
- P145 does not add order API.
- P145 does not add execution API.
- P145 does not add scheduler / automation / auto-trading.
- P145 does not add tests.
- P145 does not authorize production implementation.
- P145 does not merge the PR.

## 13. Validation

P145 is documentation-only, so Maven may be skipped because no Java or test source is modified. Validation is limited to:

```text
git diff --check
git diff --cached --check
```

## 14. PR Body Checklist

The PR body must include:

- files changed
- validation performed
- authorization coverage
- allowed P146 file
- forbidden P146 files
- P146 test scenarios
- validation commands
- rollback expectations
- still-blocked paths
- boundary confirmations
- note that the PR short title is only a platform workaround; formal mainline is Issue #403 / BACKEND-P145

P145 stops here. It does not merge the PR.
