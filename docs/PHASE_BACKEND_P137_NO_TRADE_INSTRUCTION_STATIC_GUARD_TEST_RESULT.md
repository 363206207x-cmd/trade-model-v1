# BACKEND-P137 No Trade Instruction Static Guard Test

## Baseline

- Branch context: PR #386 / Issue #385.
- Formal mainline title: BACKEND-P137 No Trade Instruction Static Guard Test.
- PR title note: PR #386 uses a shortened title as a platform workaround; Issue #385 and this document preserve the formal mainline title.
- Baseline commit: `79b5a7a` (`P136 Static Guard Scope (#384)`).
- Scope: focused static no-trade-instruction guard test plus result documentation.
- Line context: P137 continues the Static Guard Test Line opened by P136.
- Placeholder removed: `docs/P137.md`.

## Files Changed

- `src/test/java/org/example/trademodel/dashboard/StaticNoTradeInstructionGuardTest.java`
- `docs/PHASE_BACKEND_P137_NO_TRADE_INSTRUCTION_STATIC_GUARD_TEST_RESULT.md`
- Removed `docs/P137.md`

No production Java, dashboard HTML, controller, endpoint, API, schema, config, service, mapper, runtime data reader, live market data reader, external data integration, readiness, order, execution, scheduler, automation, or auto-trading files are changed.

## Static Guard Coverage

P137 adds one focused static test file. The test reads `src/main/resources/templates/dashboard.html` as repository text with `Files.readString(Path.of(...))`.

The test scopes assertions to the `candidateReviewDisplay` read-only skeleton section so it guards the P130 candidate review skeleton without exercising or rewriting unrelated legacy dashboard controls elsewhere in the template.

Coverage includes:

- mandatory safe labels exist in the candidate review skeleton:
  - `review-only`
  - `manual review required`
  - `not trade instruction`
- forbidden positive/actionable labels are absent outside allowed negative guard wording:
  - `buy`
  - `sell`
  - `open`
  - `close`
  - `reverse`
  - `signal`
  - `trade-ready`
  - `ready-to-trade`
  - `executable`
  - `production VALID`
  - `auto-trading`
- no buttons, links, forms, click handlers, fetch calls, API paths, or localStorage decision paths are introduced in the candidate review skeleton
- no `entryPrice`, `stopPrice`, `takeProfitPrice`, `riskRewardValue`, `tradeReady`, `readyToTrade`, `orderAction`, or `executionAction` field surfaces are introduced in the static skeleton
- no readiness, production `VALID`, or executable positive surface exists in the candidate review skeleton

## Allowed Negative Context

P137 allows the reviewable terms `order`, `execution`, `reverse`, `signal`, and `auto-trading` only inside the existing negative guard sentence:

```text
No order, execution, reverse, signal, or auto-trading action is available here.
```

The static test removes that allowed sentence before checking that those reviewable terms do not appear elsewhere in the candidate review skeleton.

## Still-Blocked Paths

The following paths remain blocked after P137:

- production Java
- `src/main/resources/templates/dashboard.html` changes
- dashboard UI implementation
- controller / endpoint Java
- API wiring
- schema changes
- config changes
- service changes
- mapper changes
- runtime data reads
- live market data reads
- external data fetches
- external data integration
- production candidate generation
- real entry / stop / TP / RR value generation
- production `VALID` mapping
- BoundaryCandidateService `VALID` production path
- `BoundaryCandidateDTO.valid(...)` calls
- production `BoundaryStatusEnum.VALID` mapping
- ExecutionPlan readiness upgrade
- dashboard readiness mutation
- action affordances
- executable plan state
- order API
- execution API
- scheduler / automation / auto-trading

## Rollback Expectations

Rollback is limited to:

- remove `src/test/java/org/example/trademodel/dashboard/StaticNoTradeInstructionGuardTest.java`
- remove `docs/PHASE_BACKEND_P137_NO_TRADE_INSTRUCTION_STATIC_GUARD_TEST_RESULT.md`
- restore `docs/P137.md` only if the PR is abandoned before merge

Rollback must not touch production Java, `dashboard.html`, controller, endpoint, API, schema, config, service, mapper, runtime data, live market data, external data, readiness, order, execution, scheduler, automation, or auto-trading paths.

## Boundary Confirmations

- P137 adds one focused static test file only.
- P137 reads approved static files as repository text only.
- P137 does not start Spring context.
- P137 does not instantiate services.
- P137 does not call controllers.
- P137 does not call APIs.
- P137 does not use network.
- P137 does not parse runtime data.
- P137 removes the placeholder `docs/P137.md`.
- P137 does not modify production Java.
- P137 does not modify `dashboard.html`.
- P137 does not add dashboard UI code.
- P137 does not add controller / endpoint / API / schema / config / service / mapper changes.
- P137 does not read runtime data.
- P137 does not read live market data.
- P137 does not fetch external data.
- P137 does not generate real entry / stop / TP / RR values.
- P137 does not upgrade ExecutionPlan readiness.
- P137 does not map to production `VALID`.
- P137 does not wire BoundaryCandidateService `VALID` production path.
- P137 does not call `BoundaryCandidateDTO.valid(...)`.
- P137 does not add order API.
- P137 does not add execution API.
- P137 does not add scheduler / automation / auto-trading.

## Validation

Validation required for P137:

```text
./mvnw -q -Dtest=StaticNoTradeInstructionGuardTest test
./mvnw -q -DskipTests compile
./mvnw -q -DskipTests test-compile
git diff --check
```

## PR Body Checklist

The PR body must include:

- files changed
- validation performed
- static guard coverage
- allowed negative context
- still-blocked paths
- rollback expectations
- boundary confirmations
- note that the PR short title is only a platform workaround; formal mainline is Issue #385 / BACKEND-P137

P137 stops here. It does not merge the PR.
