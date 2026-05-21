# BACKEND-P127 Dashboard / ExecutionPlan Read-Only Display Contract

## Baseline

- Branch context: PR #366 / Issue #365.
- Formal mainline title: BACKEND-P127 Dashboard / ExecutionPlan Read-Only Display Contract.
- PR title note: PR #366 uses a shortened title as a platform workaround; Issue #365 and this document preserve the formal mainline title.
- Baseline commit: `d7303b5` (`chore: add P127 placeholder`), based on `1b27bb1` (`P126A Trace Correction (#364)`).
- Scope: documentation-only display contract for future dashboard and ExecutionPlan read-only display of read-only candidate context.
- Line context: P127 starts the E line, Dashboard / ExecutionPlan Read-Only Display Line.
- Placeholder removed: `docs/P127.md`.

## Files Changed

- `docs/PHASE_BACKEND_P127_DASHBOARD_EXECUTIONPLAN_READ_ONLY_DISPLAY_CONTRACT.md`
- Removed `docs/P127.md`

No production Java, test source, runtime, dashboard HTML, dashboard UI code, schema, config, controller, endpoint, API wiring, service registration, readiness, order, execution, scheduler, automation, auto-trading, exchange-client, or external-data files are changed.

## Scope Statement

P127 is a display contract only.

P127 does not implement dashboard UI. P127 does not modify `dashboard.html`. P127 does not add endpoints. P127 does not wire runtime or API paths. P127 does not upgrade ExecutionPlan readiness. P127 does not add production wiring. P127 does not add order, execution, scheduler, automation, or auto-trading behavior.

The E line begins as read-only display planning only. It does not inherit production authorization, readiness authorization, dashboard implementation authorization, order authorization, execution authorization, scheduler authorization, automation authorization, or auto-trading authorization from any prior line.

## Display States

Future dashboard and ExecutionPlan read-only display may reference only these read-only candidate states:

- `REVIEW_ONLY_CANDIDATE`
- `INCOMPLETE`
- `BLOCKED`

These states are display context only. They are not production `VALID`, not readiness, not executable state, not trade-ready state, not a trade instruction, not an order instruction, not an execution instruction, not scheduler behavior, not automation behavior, and not auto-trading behavior.

## Required Display Flags

All future display output must preserve these flags:

- `manualReviewRequired=true`
- `notTradeInstruction=true`
- `reviewMode=REVIEW_ONLY`

These flags must be visible in any future dashboard or ExecutionPlan display model as review-only, manual-review-required, non-actionable context.

## Required Copy Boundaries

Future dashboard and ExecutionPlan read-only display copy must preserve these meanings:

- `REVIEW_ONLY_CANDIDATE` means non-actionable review-only context only.
- `INCOMPLETE` means missing required evidence only.
- `BLOCKED` means no-go / forbidden / Risk Action Guard blocked only.

Required display semantics:

- `REVIEW_ONLY_CANDIDATE` must not imply completion, production readiness, trade readiness, order readiness, execution readiness, opportunity, signal, buy, sell, open, close, reverse, scheduler behavior, automation behavior, or auto-trading behavior.
- `INCOMPLETE` must not imply safe-to-trade, low risk, no risk, ready-to-trade, or review passed.
- `BLOCKED` must not imply opportunity, reversal, signal, recommendation, execution, automation, or any action path.
- Missing evidence must display only as `INCOMPLETE`.
- Forbidden input, no-go evidence, or Risk Action Guard blocker evidence must display only as `BLOCKED`.

Display copy may say that information is present for review, missing for review, or blocked for review. Display copy must not tell a user to enter, exit, reverse, open, close, buy, sell, place, execute, automate, or schedule a trade.

## Forbidden UI And Action Labels

Future dashboard and ExecutionPlan read-only display must not use these labels for read-only candidate context:

- `buy`
- `sell`
- `open`
- `close`
- `reverse`
- `signal`
- `trade-ready`
- `ready-to-trade`
- `production VALID`
- `auto-trading`

Equivalent labels, badges, button text, tooltips, aria labels, route names, component names, object field names, CSS classes, data attributes, icons, or hover copy that imply the same action are also blocked unless a later issue explicitly authorizes a different bounded scope.

## Risk Action Guard Display Boundaries

Risk Action Guard evidence must remain visible, review-only, and non-actionable.

Required display boundaries:

- Stampede evidence forbids reverse display.
- Stampede evidence forbids new position display.
- Stampede evidence forbids opportunity push display.
- Wick-only evidence does not imply trend reversal.
- Wick-only evidence must not display reverse entry, reversal signal, buy, sell, open, close, or execution copy.
- Missing event evidence must not display as no risk.
- Liquidity stress must not display as opportunity.
- Risk high with deteriorating liquidity must not display a one-shot market exit instruction.

Risk Action Guard summaries may describe blocker context for manual review. They must not become action copy, order copy, execution copy, automation copy, dashboard readiness copy, or ExecutionPlan readiness copy.

## No Mutation Contract

P127 preserves these non-mutation rules:

- No dashboard readiness mutation.
- No ExecutionPlan readiness mutation.
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

## Future Allowed Display Fields

If a later separately authorized line implements a display DTO or view model, the future allowed fields are limited to display-only context such as:

- `statusLabel`
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
- `displayOnlyWarnings`

These fields must remain strings, labels, summaries, or warning tokens for read-only display. They must not carry executable state, production readiness state, order state, execution state, automation state, or real trading values.

## Forbidden Future Display Fields

Future display DTOs, view models, templates, components, or ExecutionPlan display models must not introduce these fields for read-only candidate context:

- `buy`
- `sell`
- `open`
- `close`
- `reverse`
- `signal`
- `tradeReady`
- `readyToTrade`
- `productionValid`
- `orderAction`
- `executionAction`
- `autoTrading`
- `executable`
- `entryPrice`
- `stopPrice`
- `takeProfitPrice`
- `riskRewardValue`

Equivalent aliases, abbreviations, nested fields, boolean flags, enums, CSS selectors, data attributes, route parameters, component props, and API fields that imply those same meanings remain blocked.

## Future Display Authorization Gates

Future dashboard or ExecutionPlan display implementation may begin only in a separately authorized line with all of these gates satisfied:

- A new issue explicitly authorizes dashboard and/or ExecutionPlan read-only display implementation.
- The future issue lists exact allowed files.
- The future issue states whether `dashboard.html` is allowed; it remains blocked unless explicitly listed.
- The future issue states whether any Java DTO, mapper, controller, endpoint, schema, config, or service file is allowed; each remains blocked unless explicitly listed.
- The implementation preserves `manualReviewRequired=true`, `notTradeInstruction=true`, and `reviewMode=REVIEW_ONLY`.
- The implementation preserves `REVIEW_ONLY_CANDIDATE`, `INCOMPLETE`, and `BLOCKED` copy boundaries.
- The implementation excludes all forbidden UI/action labels and forbidden future display fields.
- The implementation excludes dashboard readiness mutation and ExecutionPlan readiness mutation.
- The implementation excludes order, execution, scheduler, automation, and auto-trading surfaces.
- The implementation includes rollback expectations and test commands before code changes begin.

Broad language such as "show it", "wire it", "make dashboard ready", "enable readiness", "make it actionable", or "make production ready" is insufficient authorization.

## Future Tests

Future display implementation must define tests before implementation.

Required future test coverage:

- `REVIEW_ONLY_CANDIDATE` renders as non-actionable review-only context.
- `INCOMPLETE` renders as missing required evidence only.
- `BLOCKED` renders as no-go / forbidden / Risk Action Guard blocked only.
- Display output preserves `manualReviewRequired=true`.
- Display output preserves `notTradeInstruction=true`.
- Display output preserves `reviewMode=REVIEW_ONLY`.
- Forbidden UI/action labels are absent.
- Forbidden future display fields are absent.
- Missing evidence displays only as `INCOMPLETE`.
- Forbidden / no-go / Risk Action Guard blockers display only as `BLOCKED`.
- Stampede evidence forbids reverse / new position / opportunity push display.
- Wick-only evidence does not display trend reversal.
- No dashboard readiness mutation is present.
- No ExecutionPlan readiness mutation is present.
- No controller, endpoint, API, schema, config, or service registration is present unless explicitly authorized.
- No order, execution, scheduler, automation, or auto-trading surface is present.
- No real entry / stop / TP / RR values are generated or displayed.
- Rollback can restore the last approved read-only display state.

The existing market read-only guard command set remains a prerequisite for future display work unless a later issue explicitly narrows or expands it:

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

Future display work must document rollback before implementation begins.

Rollback must:

- identify the last approved read-only display freeze point
- identify exact files that can be reverted
- remove any dashboard implementation introduced by a future PR
- remove any `dashboard.html`, dashboard UI code, schema, config, controller, endpoint, API, service registration, or Spring bean registration introduced by a future PR
- remove any dashboard readiness mutation introduced by a future PR
- remove any ExecutionPlan readiness mutation introduced by a future PR
- remove any forbidden UI/action labels introduced by a future PR
- remove any forbidden future display fields introduced by a future PR
- remove any production wiring introduced by a future PR
- remove any production `VALID`, BoundaryCandidateService `VALID`, `BoundaryCandidateDTO.valid(...)`, or production `BoundaryStatusEnum.VALID` mapping introduced by a future PR
- remove any real entry / stop / TP / RR display or generation introduced by a future PR
- remove any order, execution, scheduler, automation, or auto-trading surface introduced by a future PR
- restore `manualReviewRequired=true`
- restore `notTradeInstruction=true`
- restore `reviewMode=REVIEW_ONLY`
- restore `REVIEW_ONLY_CANDIDATE` as non-actionable review-only context
- restore `INCOMPLETE` as missing required evidence only
- restore `BLOCKED` as no-go / forbidden / Risk Action Guard blocked only

If rollback cannot be applied cleanly, the future PR must remain blocked and must not proceed.

## Still-Blocked Paths

The following paths remain blocked after P127:

- dashboard implementation
- `dashboard.html` changes
- dashboard UI code
- dashboard readiness mutation
- ExecutionPlan readiness mutation
- ExecutionPlan readiness upgrade
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

- P127 is documentation-only display contract work.
- P127 starts the E line, Dashboard / ExecutionPlan Read-Only Display Line.
- P127 removes the placeholder `docs/P127.md`.
- P127 adds one display contract document.
- P127 does not implement dashboard UI.
- P127 does not modify `dashboard.html`.
- P127 does not add endpoints.
- P127 does not wire runtime or API paths.
- P127 does not upgrade ExecutionPlan readiness.
- P127 does not add production wiring.
- P127 does not authorize order, execution, scheduler, automation, or auto-trading.
- P127 does not modify production Java.
- P127 does not modify test source.
- P127 does not add dashboard UI code.
- P127 does not add controller / endpoint Java.
- P127 does not modify schema.
- P127 does not modify config.
- P127 does not add service registration.
- P127 does not read runtime data.
- P127 does not read live market data.
- P127 does not fetch external data.
- P127 does not add exchange clients, `WebClient`, or `RestTemplate`.
- P127 does not implement production candidate generation.
- P127 does not generate real entry / stop / TP / RR values.
- P127 does not wire BoundaryCandidateService `VALID` production path.
- P127 does not call `BoundaryCandidateDTO.valid(...)`.
- P127 does not map to production `BoundaryStatusEnum.VALID`.
- P127 does not add order API.
- P127 does not add execution API.
- P127 does not add scheduler / automation / auto-trading.

## Validation

P127 is documentation-only, so Maven may be skipped. Validation is limited to git diff checks confirming the display contract document and placeholder removal only.
