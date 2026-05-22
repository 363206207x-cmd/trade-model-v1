# BACKEND-P144 Source-Owned Candidate Test Plan

## 1. Baseline

- Branch context: PR #402 / Issue #401.
- Formal mainline title: BACKEND-P144 Source-Owned Candidate Test Plan.
- PR title note: PR #402 uses a shortened title as a platform workaround; Issue #401 and this document preserve the formal mainline title.
- Baseline commit: `d2759bd` (`P143 Design Matrix (#400)`).
- Scope: documentation-only source-owned candidate test plan.
- Placeholder removed: `docs/P144.md`.

P143 confirmed:

- no input family is sufficient for runtime source-owned candidate generation today
- all families still require missing owner / ref / window / freshness / rule / conflict / runtime population work
- P144 must only plan future tests and must not add tests

## 2. Scope And Non-Authorization

P144 defines future test categories, future test file patterns, future scenarios, fail-closed expectations, rollback expectations, and still-blocked paths.

P144 does not add Java. P144 does not add test source. P144 does not modify `dashboard.html`. P144 does not add controller, endpoint, API, schema, config, service, or mapper changes.

P144 does not authorize:

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

## 3. Test Plan Objectives

Future tests must prove that source-owned candidate generation remains fail-closed until every P142/P143 source-owned requirement is satisfied.

Objectives:

- prove every input family has a dedicated source-owned input contract test
- prove entry / stop / TP / RR numeric sources cannot be accepted without owner, ref, timeframe, window, freshness, rule id, rule version, reason, conflict state, and audit trail
- prove missing owner / ref / timeframe / window / freshness / rule / reason / conflict keeps output `INCOMPLETE`
- prove forbidden, unsafe, no-go, or substitution evidence keeps output `BLOCKED`
- prove latest price alone and raw kline presence alone cannot create boundary sources
- prove AI, dashboard, API, display, order, and execution surfaces cannot become source ownership
- prove SourceTrace audit completeness is mandatory
- prove Risk Action Guard boundaries block action surfaces
- prove no production `VALID`, no ExecutionPlan readiness upgrade, and no order / execution / scheduler / automation / auto-trading behavior appears

## 4. Test Families Matrix

| Input Family | Future Test Category | Core Scenarios | Expected Fail-Closed Result |
| --- | --- | --- | --- |
| Entry | Entry source-owned input contract tests | Missing owner/ref/timeframe/window/freshness/rule/reason/conflict; latest-price-only; AI/display substitution. | `INCOMPLETE` for missing evidence; `BLOCKED` for substitution or unsafe evidence. |
| Stop | Stop source-owned input contract tests | Missing independent stop ownership; stop inferred from entry/latest price/raw kline; stale unsafe stop window. | `INCOMPLETE` or `BLOCKED` according to evidence. |
| Take-profit | TP source-owned input contract tests | Missing TP owner per level; unordered or mixed TP ownership; raw-kline-only TP. | `INCOMPLETE` or `BLOCKED`. |
| Risk-reward | RR source-owned input contract tests | Missing RR owner/rule/ref; RR not derived from owned entry/stop/TP; copied text RR. | `INCOMPLETE` or `BLOCKED`. |
| Liquidity | Liquidity source-owned input contract tests | Missing liquidity evidence; liquidity stress; deteriorating liquidity treated as opportunity or direct exit. | `INCOMPLETE` or `BLOCKED`. |
| Multi-timeframe | Multi-timeframe source-owned input contract tests | Missing participating timeframe refs; stale timeframe; agreement-alone readiness. | `INCOMPLETE` or `BLOCKED`. |
| Event | Event source-owned input contract tests | Missing event evidence; missing event treated as no risk; no-go event. | `INCOMPLETE` or `BLOCKED`. |
| Wick | Wick source-owned input contract tests | Missing wick confirmation; wick-only trend reversal; stale wick evidence. | `INCOMPLETE` or `BLOCKED`. |
| OHLCV/kline | OHLCV/kline source-owned input contract tests | Missing persisted readiness; raw-kline-only boundary; latest-price-only entry. | `INCOMPLETE` or `BLOCKED`. |
| Data quality | Data quality source-owned input contract tests | Missing score/owner/ref/window/rule; unrelated score substitution. | `INCOMPLETE` or `BLOCKED`. |
| SourceTrace audit | SourceTrace audit completeness tests | Missing family audit, missing rollback trail, runtime SourceTrace not populated from source-owned evidence. | `INCOMPLETE` or `BLOCKED`. |
| Risk Action Guard | Risk Action Guard source-owned candidate tests | Stampede, liquidity stress, wick-only, deteriorating liquidity, action flags. | `BLOCKED` or review-only fail-closed context. |

## 5. Numeric Source Ownership Test Plan

Future numeric source ownership tests must cover:

- entry numeric source owner required
- stop numeric source owner required
- TP numeric source owner required for every TP level
- RR numeric source owner required
- source ref required for every numeric boundary value
- source timeframe required for every numeric boundary value
- source window required for every numeric boundary value
- freshness status required for every numeric boundary value
- rule id required for every numeric boundary value
- rule version required for every numeric boundary value
- source reason required for every numeric boundary value
- conflict state required for every numeric boundary value
- audit trail required for every numeric boundary value

Expected future class group:

- `SourceOwnedNumericBoundarySourceTest`

This is a future test name only. P144 does not create this test.

## 6. INCOMPLETE Guard Test Plan

Future tests must prove candidate output remains `INCOMPLETE` when:

- source owner is missing
- source ref is missing
- source timeframe is missing
- source window is missing
- observed time is missing
- decision time relationship is missing
- freshness is missing
- OHLCV / kline context is missing
- persisted OHLCV readiness metadata is missing
- data quality score is missing
- data quality score owner is missing
- evidence completeness is insufficient
- SourceTrace is incomplete
- numeric source ownership is incomplete
- entry source reason is missing
- stop source reason is missing
- TP source reason is missing
- RR rule ref is missing
- rule id is missing
- rule version is missing
- conflict family state is missing
- liquidity evidence is missing
- multi-timeframe evidence is missing
- event evidence status is missing
- wick evidence status is missing
- rollback-safe evidence trail is missing
- runtime SourceTrace field is not populated from source-owned evidence

Expected future class group:

- `SourceOwnedCandidateIncompleteGuardTest`

This is a future test name only. P144 does not create this test.

## 7. BLOCKED Guard Test Plan

Future tests must prove candidate output remains `BLOCKED` when:

- forbidden input is present
- no-go evidence exists
- Risk Action Guard blocks action
- stampede condition exists
- deteriorating liquidity makes direct action unsafe
- wick-only evidence is being misread as trend reversal
- missing event evidence is being treated as no risk
- liquidity stress is being treated as opportunity
- stale source window appears with unsafe or contradictory evidence
- unsupported source owner is used
- unsupported source type is used
- source ref is duplicated, ambiguous, or fabricated
- dashboard/API/display text is used as source ownership
- AI text is used as source ownership
- latest price is used as entry source by itself
- raw kline item presence is used as entry / stop / TP / RR source by itself
- external data appears without a source ownership contract
- order / execution / automation surface appears

Expected future class group:

- `SourceOwnedCandidateBlockedGuardTest`

This is a future test name only. P144 does not create this test.

## 8. Substitution Blocked Test Plan

Future substitution blocked tests must cover:

- latest-price-only blocked tests
- raw-kline-only blocked tests
- AI text substitution blocked tests
- dashboard text substitution blocked tests
- API text substitution blocked tests
- display text substitution blocked tests
- order state substitution blocked tests
- execution state substitution blocked tests
- duplicate source ref blocked tests
- ambiguous source ref blocked tests
- fabricated source ref blocked tests
- unsupported source owner blocked tests
- unsupported source type blocked tests
- external data without source ownership contract blocked tests

Expected future class group:

- `SourceOwnedSubstitutionBlockedTest`

This is a future test name only. P144 does not create this test.

## 9. Risk Action Guard Test Plan

Future Risk Action Guard tests must prove:

- Stampede must not become reverse / new-position / opportunity-push display.
- Wick-only must not become trend reversal.
- Deteriorating liquidity must not become one-shot market exit instruction.
- Missing event evidence must not display as no risk.
- Liquidity stress must not display as opportunity.
- High risk alone must not mean direct stop loss, reverse, or new position.
- Risk high with normal liquidity may support review-only risk reduction context, not automatic action.
- Risk high with deteriorating liquidity must avoid one-shot market exit instruction.
- Risk high with stampede must block reverse / new position / opportunity push.
- Risk high with wick-only evidence must wait for confirmation.
- Any action flag or order / execution / automation surface remains blocked.

Expected future class group:

- `RiskActionGuardSourceOwnedCandidateTest`

This is a future test name only. P144 does not create this test.

## 10. SourceTrace Audit Completeness Test Plan

Future SourceTrace audit tests must prove:

- audit owner is required
- source refs are required for every family
- source timeframe coverage is required
- source windows are required
- freshness summary is required
- rule id / rule version coverage is required
- reasons are required
- conflict summary is required
- missing evidence summary is required
- blocked evidence summary is required
- rollback-safe evidence trail is required
- `manualReviewRequired=true` is preserved
- `notTradeInstruction=true` is preserved
- `reviewMode=REVIEW_ONLY` is preserved
- runtime SourceTrace fields must be populated only from source-owned evidence
- dashboard/API/display/AI/order/execution data cannot populate SourceTrace audit

Expected future class group:

- `SourceTraceAuditCompletenessTest`

This is a future test name only. P144 does not create this test.

## 11. No Production VALID / No Readiness / No Order-Execution Guard Plan

Future guard tests must prove:

- no production `VALID` mapping is introduced
- no BoundaryCandidateService production `VALID` path is wired
- no `BoundaryCandidateDTO.valid(...)` call is added in new production flows
- no production `BoundaryStatusEnum.VALID` mapping is introduced
- no ExecutionPlan readiness upgrade is introduced
- no dashboard readiness mutation is introduced
- no order API is introduced
- no execution API is introduced
- no scheduler behavior is introduced
- no automation behavior is introduced
- no auto-trading behavior is introduced
- no runtime/live/external data reader is introduced unless separately authorized

Expected future class group:

- `NoProductionValidReadinessExecutionGuardTest`

This is a future test name only. P144 does not create this test.

## 12. Allowed Future Test File Patterns

Future separately authorized issues may use focused test files under `src/test/java` only after the issue explicitly authorizes Java test source changes.

Allowed future class-name patterns:

- `SourceOwnedCandidateInputContractTest`
- `SourceOwnedNumericBoundarySourceTest`
- `SourceOwnedCandidateIncompleteGuardTest`
- `SourceOwnedCandidateBlockedGuardTest`
- `SourceOwnedSubstitutionBlockedTest`
- `SourceTraceAuditCompletenessTest`
- `RiskActionGuardSourceOwnedCandidateTest`
- `NoProductionValidReadinessExecutionGuardTest`

Preferred future package pattern, if consistent with the repository at that time:

```text
src/test/java/org/example/trademodel/dto/planboundary/*Test.java
src/test/java/org/example/trademodel/service/impl/*Test.java
src/test/java/org/example/trademodel/service/*Test.java
```

These patterns are future-only. P144 does not create test files.

## 13. Tests Still Requiring Separate Authorization

All tests named in P144 require separate authorization before implementation.

The following remain documentation-only until a future issue names exact files and validation commands:

- source-owned input contract tests
- numeric source ownership tests
- missing owner/ref/timeframe/window/freshness/rule/reason/conflict fail-closed tests
- latest-price-only blocked tests
- raw-kline-only blocked tests
- AI/dashboard/API/display text substitution blocked tests
- duplicate / ambiguous / fabricated source ref blocked tests
- missing event evidence incomplete tests
- liquidity stress blocked tests
- stampede blocked tests
- wick-only confirmation tests
- SourceTrace audit completeness tests
- no production `VALID` guard tests
- no ExecutionPlan readiness upgrade guard tests
- no order / execution / scheduler / automation / auto-trading guard tests

P144 does not authorize any of these test files.

## 14. Rollback Expectations

Rollback for P144 is limited to:

- remove `docs/PHASE_BACKEND_P144_SOURCE_OWNED_CANDIDATE_TEST_PLAN.md`
- restore `docs/P144.md` only if the PR is abandoned before merge

Rollback must not touch production Java, test source, `dashboard.html`, controller, endpoint, API, schema, config, service, mapper, runtime data, live market data, external data, readiness, order, execution, scheduler, automation, or auto-trading paths.

If a future change uses P144 to add tests or implementation without separate authorization, rollback must restore the last approved P143 design matrix state and keep all still-blocked paths blocked.

## 15. Recommended Next Step

Recommended next step after P144 is STOP unless a separately authorized issue exists.

If work continues, the safest next line is a documentation-only test authorization gate. That future issue should select one narrow test group, name exact files, define expected fail-closed behavior, and preserve all still-blocked implementation paths.

P144 does not authorize that future gate. P144 does not authorize tests. P144 does not authorize production implementation.

## 16. Still-Blocked Paths

The following paths remain blocked after P144:

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

## 17. Boundary Confirmations

- P144 is documentation-only test plan work.
- P144 removes the placeholder `docs/P144.md`.
- P144 adds one source-owned candidate test plan document.
- P144 does not modify production Java.
- P144 does not modify test source.
- P144 does not modify `dashboard.html`.
- P144 does not add dashboard UI code.
- P144 does not add controller / endpoint / API / schema / config / service / mapper changes.
- P144 does not read runtime data.
- P144 does not read live market data.
- P144 does not fetch external data.
- P144 does not generate real entry / stop / TP / RR values.
- P144 does not upgrade ExecutionPlan readiness.
- P144 does not map to production `VALID`.
- P144 does not wire BoundaryCandidateService `VALID` production path.
- P144 does not call `BoundaryCandidateDTO.valid(...)`.
- P144 does not add order API.
- P144 does not add execution API.
- P144 does not add scheduler / automation / auto-trading.
- P144 does not authorize tests.
- P144 does not authorize production implementation.
- P144 does not merge the PR.

## Validation

P144 is documentation-only, so Maven may be skipped because no Java or test source is modified. Validation is limited to:

```text
git diff --check
git diff --cached --check
```

## PR Body Checklist

The PR body must include:

- files changed
- validation performed
- test plan coverage
- planned test categories
- future allowed test file patterns
- fail-closed expectations
- recommended next step
- still-blocked paths
- boundary confirmations
- note that the PR short title is only a platform workaround; formal mainline is Issue #401 / BACKEND-P144

P144 stops here. It does not merge the PR.
