# BACKEND-P128 ExecutionPlan Read-Only Candidate Display Contract

## Baseline

- Branch context: PR #368 / Issue #367.
- Formal mainline title: BACKEND-P128 ExecutionPlan Read-Only Candidate Display Contract.
- PR title note: PR #368 uses a shortened title as a platform workaround; Issue #367 and this document preserve the formal mainline title.
- Baseline commit: `4de86ce` (`chore: add P128 placeholder`), based on `907f872` (`P127 Display Contract (#366)`).
- Scope: documentation-only ExecutionPlan read-only candidate display contract.
- Line context: P128 continues the E line, Dashboard / ExecutionPlan Read-Only Display Line.
- Placeholder removed: `docs/P128.md`.

## Files Changed

- `docs/PHASE_BACKEND_P128_EXECUTIONPLAN_READ_ONLY_CANDIDATE_DISPLAY_CONTRACT.md`
- Removed `docs/P128.md`

No production Java, test source, runtime, dashboard HTML, dashboard UI code, schema, config, controller, endpoint, API wiring, service registration, readiness, order, execution, scheduler, automation, auto-trading, exchange-client, or external-data files are changed.

## Scope Statement

P128 is an ExecutionPlan display contract only.

P128 does not implement dashboard UI. P128 does not modify `dashboard.html`. P128 does not add endpoints. P128 does not wire runtime or API paths. P128 does not upgrade readiness. P128 does not add production wiring. P128 does not add order, execution, scheduler, automation, or auto-trading behavior.

ExecutionPlan display is future-only and is not implemented in P128. This document only defines how future work may represent read-only candidate context in ExecutionPlan display without producing executable plans, action plans, order plans, close plans, reverse plans, automation plans, or real entry / stop / TP / RR values.

## ExecutionPlan Display State Contract

Future ExecutionPlan read-only candidate display may reference only these source states:

- `REVIEW_ONLY_CANDIDATE`
- `INCOMPLETE`
- `BLOCKED`

Required mapping semantics:

- `REVIEW_ONLY_CANDIDATE` may only map to non-actionable display context.
- `INCOMPLETE` may only map to missing-evidence display context.
- `BLOCKED` may only map to no-go / forbidden / Risk Action Guard blocked display context.

These states must not map to production `VALID`, ExecutionPlan readiness, dashboard readiness, trade readiness, executable state, order state, execution state, scheduler state, automation state, or auto-trading state.

## Required Display Flags

All future ExecutionPlan read-only display output must preserve these flags:

- `manualReviewRequired=true`
- `notTradeInstruction=true`
- `reviewMode=REVIEW_ONLY`

These flags must remain visible as display-only context. They must not become an action affordance, readiness affordance, order affordance, execution affordance, scheduler affordance, automation affordance, or auto-trading affordance.

## ExecutionPlan Non-Action Contract

Future ExecutionPlan display must preserve these non-action boundaries:

- No ExecutionPlan readiness upgrade.
- No executable plan state.
- No action plan.
- No order plan.
- No close plan.
- No reverse plan.
- No automation plan.
- No real entry / stop / TP / RR values.
- No production `VALID` mapping.
- No trade-ready or ready-to-trade display.
- No buy / sell / open / close / reverse / signal display.

ExecutionPlan display may describe review context, missing evidence, or blocker evidence. It must not direct or imply a trade action.

## Required Copy Boundaries

Future ExecutionPlan read-only display copy must preserve these meanings:

- `REVIEW_ONLY_CANDIDATE` means non-actionable review-only context only.
- `INCOMPLETE` means missing required evidence only.
- `BLOCKED` means no-go / forbidden / Risk Action Guard blocked only.

Required display behavior:

- `REVIEW_ONLY_CANDIDATE` must not imply completion, readiness, trade readiness, executable readiness, order readiness, execution readiness, opportunity, signal, buy, sell, open, close, reverse, scheduler behavior, automation behavior, or auto-trading behavior.
- `INCOMPLETE` must not imply safe-to-trade, no risk, low risk, ready-to-trade, review passed, or executable context.
- `BLOCKED` must not imply opportunity, reversal, signal, recommendation, order, execution, automation, or any action path.
- Missing evidence must display only as missing-evidence context.
- Forbidden input, no-go evidence, or Risk Action Guard blocker evidence must display only as blocked context.

ExecutionPlan display copy may say that review context is available, missing, or blocked. It must not tell a user to enter, exit, reverse, open, close, buy, sell, place, execute, automate, or schedule a trade.

## Allowed Future ExecutionPlan Display Fields

If a later separately authorized line implements an ExecutionPlan display DTO or view model, the future allowed fields are limited to token and summary context:

- `planDisplayStatus`
- `reviewModeLabel`
- `manualReviewRequiredLabel`
- `notTradeInstructionLabel`
- `sourceOwnershipSummary`
- `evidenceCompletenessSummary`
- `missingEvidenceSummary`
- `blockerSummary`
- `riskActionGuardSummary`
- `freshnessSummary`
- `conflictSummary`
- `dataQualitySummary`
- `executionPlanDisplayWarnings`
- `nonActionableDisplayReason`

These fields must remain strings, labels, summaries, or warning tokens for read-only display. They must not carry numeric trading values, executable state, readiness state, order state, execution state, scheduler state, automation state, or auto-trading state.

## Forbidden Future ExecutionPlan Display Fields

Future ExecutionPlan display DTOs, view models, templates, components, mappers, or API payloads must not introduce these fields for read-only candidate context:

- `entryPrice`
- `stopPrice`
- `takeProfitPrice`
- `riskRewardValue`
- `orderAction`
- `executionAction`
- `tradeReady`
- `readyToTrade`
- `executable`
- `buy`
- `sell`
- `open`
- `close`
- `reverse`
- `signal`
- `autoTrading`
- `closePlan`
- `reversePlan`
- `orderPlan`
- `executionPlanAction`
- `automationPlan`

Equivalent aliases, abbreviations, nested fields, boolean flags, enums, object names, method names, component props, route parameters, CSS selectors, data attributes, or API fields that imply the same meanings remain blocked.

## Risk Action Guard Display Boundaries

Risk Action Guard evidence must remain visible, review-only, and non-actionable in future ExecutionPlan display.

Required boundaries:

- Stampede must not become reverse display.
- Stampede must not become new-position display.
- Stampede must not become opportunity-push display.
- Wick-only evidence must not become trend reversal display.
- Wick-only evidence must not become reverse entry display.
- Risk high with deteriorating liquidity must not become one-shot market exit display.
- Missing event evidence must not display as no risk.
- Liquidity stress must not display as opportunity.

Risk Action Guard summaries may describe blocker context for manual review. They must not become action copy, order copy, execution copy, automation copy, dashboard readiness copy, or ExecutionPlan readiness copy.

## No Mutation Contract

P128 preserves these non-mutation rules:

- No ExecutionPlan readiness mutation.
- No ExecutionPlan readiness upgrade.
- No executable plan state.
- No action plan.
- No order plan.
- No close plan.
- No reverse plan.
- No automation plan.
- No dashboard implementation.
- No `dashboard.html` change.
- No dashboard UI code.
- No controller change.
- No endpoint change.
- No API wiring.
- No schema change.
- No config change.
- No service registration.
- No Spring bean registration.
- No production candidate generation.
- No real entry / stop / TP / RR value generation.
- No BoundaryCandidateService `VALID` production path.
- No `BoundaryCandidateDTO.valid(...)` call.
- No production `BoundaryStatusEnum.VALID` mapping.
- No order API.
- No execution API.
- No scheduler behavior.
- No automation behavior.
- No auto-trading behavior.

## Future ExecutionPlan Display Authorization Gates

Future ExecutionPlan display implementation may begin only in a separately authorized line with all of these gates satisfied:

- A new issue explicitly authorizes ExecutionPlan read-only display implementation.
- The future issue lists exact allowed files.
- The future issue states whether any Java DTO, mapper, display adapter, controller, endpoint, schema, config, service, or dashboard file is allowed; each remains blocked unless explicitly listed.
- The implementation preserves `manualReviewRequired=true`, `notTradeInstruction=true`, and `reviewMode=REVIEW_ONLY`.
- The implementation preserves `REVIEW_ONLY_CANDIDATE`, `INCOMPLETE`, and `BLOCKED` display boundaries.
- The implementation uses only token/summary display fields.
- The implementation excludes all forbidden future ExecutionPlan display fields.
- The implementation excludes ExecutionPlan readiness mutation and readiness upgrade.
- The implementation excludes executable plan, action plan, order plan, close plan, reverse plan, and automation plan surfaces.
- The implementation excludes order, execution, scheduler, automation, and auto-trading surfaces.
- The implementation includes rollback expectations and test commands before code changes begin.

Broad language such as "show it", "wire it", "make plan ready", "enable readiness", "make executable", "create plan actions", or "make production ready" is insufficient authorization.

## Future Tests

Future ExecutionPlan display implementation must define tests before implementation.

Required future test coverage:

- `REVIEW_ONLY_CANDIDATE` maps only to non-actionable ExecutionPlan display context.
- `INCOMPLETE` maps only to missing-evidence ExecutionPlan display context.
- `BLOCKED` maps only to no-go / forbidden / Risk Action Guard blocked ExecutionPlan display context.
- Display output preserves `manualReviewRequired=true`.
- Display output preserves `notTradeInstruction=true`.
- Display output preserves `reviewMode=REVIEW_ONLY`.
- Allowed ExecutionPlan display fields are token/summary only.
- Forbidden ExecutionPlan display fields are absent.
- No `entryPrice`, `stopPrice`, `takeProfitPrice`, or `riskRewardValue` display exists.
- No `orderAction`, `executionAction`, `tradeReady`, `readyToTrade`, or `executable` display exists.
- No buy / sell / open / close / reverse / signal display exists.
- No close plan, reverse plan, order plan, execution plan action, automation plan, scheduler behavior, or auto-trading surface exists.
- Missing evidence displays only as missing-evidence context.
- Forbidden / no-go / Risk Action Guard blockers display only as blocked context.
- Stampede evidence does not display reverse / new position / opportunity push context.
- Wick-only evidence does not display trend reversal context.
- Risk high with deteriorating liquidity does not display a one-shot market exit instruction.
- Missing event evidence does not display as no risk.
- Liquidity stress does not display as opportunity.
- No ExecutionPlan readiness mutation is present.
- No controller, endpoint, API, schema, config, service registration, dashboard implementation, or `dashboard.html` change is present unless explicitly authorized.
- No real entry / stop / TP / RR values are generated or displayed.
- Rollback can restore the last approved read-only ExecutionPlan display state.

The existing market read-only guard command set remains a prerequisite for future ExecutionPlan display work unless a later issue explicitly narrows or expands it:

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

## Rollback Expectations

Future ExecutionPlan display work must document rollback before implementation begins.

Rollback must:

- identify the last approved read-only ExecutionPlan display freeze point
- identify exact files that can be reverted
- remove any ExecutionPlan readiness mutation introduced by a future PR
- remove any readiness upgrade introduced by a future PR
- remove any executable plan state introduced by a future PR
- remove any action plan, order plan, close plan, reverse plan, or automation plan introduced by a future PR
- remove any forbidden ExecutionPlan display field introduced by a future PR
- remove any real entry / stop / TP / RR display or generation introduced by a future PR
- remove any dashboard implementation, `dashboard.html`, dashboard UI code, schema, config, controller, endpoint, API, service registration, or Spring bean registration introduced by a future PR
- remove any production wiring introduced by a future PR
- remove any production `VALID`, BoundaryCandidateService `VALID`, `BoundaryCandidateDTO.valid(...)`, or production `BoundaryStatusEnum.VALID` mapping introduced by a future PR
- remove any order, execution, scheduler, automation, or auto-trading surface introduced by a future PR
- restore `manualReviewRequired=true`
- restore `notTradeInstruction=true`
- restore `reviewMode=REVIEW_ONLY`
- restore `REVIEW_ONLY_CANDIDATE` as non-actionable ExecutionPlan display context
- restore `INCOMPLETE` as missing-evidence ExecutionPlan display context
- restore `BLOCKED` as no-go / forbidden / Risk Action Guard blocked ExecutionPlan display context

If rollback cannot be applied cleanly, the future PR must remain blocked and must not proceed.

## Still-Blocked Paths

The following paths remain blocked after P128:

- ExecutionPlan readiness mutation
- ExecutionPlan readiness upgrade
- executable plan state
- action plan
- order plan
- close plan
- reverse plan
- automation plan
- dashboard implementation
- `dashboard.html` changes
- dashboard UI code
- dashboard readiness mutation
- controller / endpoint Java
- API wiring
- schema changes
- config changes
- service registration
- Spring bean registration
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

- P128 is documentation-only ExecutionPlan display contract work.
- P128 continues the E line, Dashboard / ExecutionPlan Read-Only Display Line.
- P128 removes the placeholder `docs/P128.md`.
- P128 adds one ExecutionPlan display contract document.
- P128 does not implement dashboard UI.
- P128 does not modify `dashboard.html`.
- P128 does not add endpoints.
- P128 does not wire runtime or API paths.
- P128 does not upgrade readiness.
- P128 does not add production wiring.
- P128 does not authorize order, execution, scheduler, automation, or auto-trading.
- P128 does not modify production Java.
- P128 does not modify test source.
- P128 does not add dashboard UI code.
- P128 does not add controller / endpoint Java.
- P128 does not modify schema.
- P128 does not modify config.
- P128 does not add service registration.
- P128 does not read runtime data.
- P128 does not read live market data.
- P128 does not fetch external data.
- P128 does not add exchange clients, `WebClient`, or `RestTemplate`.
- P128 does not implement production candidate generation.
- P128 does not generate real entry / stop / TP / RR values.
- P128 does not wire BoundaryCandidateService `VALID` production path.
- P128 does not call `BoundaryCandidateDTO.valid(...)`.
- P128 does not map to production `BoundaryStatusEnum.VALID`.
- P128 does not add order API.
- P128 does not add execution API.
- P128 does not add scheduler / automation / auto-trading.

## Validation

P128 is documentation-only, so Maven may be skipped. Validation is limited to git diff checks confirming the ExecutionPlan display contract document and placeholder removal only.
