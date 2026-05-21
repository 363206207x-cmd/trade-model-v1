# BACKEND-P125 Dashboard Read-Only Display Authorization Plan

## Baseline

- Branch context: PR #360 / Issue #359.
- Formal mainline title: BACKEND-P125 Dashboard Read-Only Display Authorization Plan.
- PR title note: PR #360 uses a shortened title as a platform workaround; Issue #359 and this document preserve the formal mainline title.
- Baseline commit: `6788603` (`chore: add P125 placeholder`), based on `686e27f` (`P124 Readiness Review (#358)`).
- Scope: documentation-only authorization plan for future dashboard read-only display of read-only candidate context.
- Line context: P125 remains part of the D line, Production Authorization Preparation / Safety Gate.
- Placeholder removed: `docs/P125.md`.

## Files Changed

- `docs/PHASE_BACKEND_P125_DASHBOARD_READ_ONLY_DISPLAY_AUTHORIZATION_PLAN.md`
- Removed `docs/P125.md`

No production Java, test source, runtime, dashboard HTML, dashboard UI code, schema, config, controller, endpoint, service registration, readiness, order, execution, scheduler, automation, auto-trading, exchange-client, or external-data files are changed.

## Scope Statement

P125 is a dashboard display authorization plan only.

P125 does not modify `dashboard.html`. P125 does not add dashboard UI code. P125 does not add endpoints. P125 does not wire runtime/API paths. P125 does not display trade instructions. P125 does not authorize production wiring. P125 does not authorize order, execution, scheduler, automation, or auto-trading.

The D line remains Production Authorization Preparation / Safety Gate only.

## Display Authorization Plan

Dashboard display is future-line work only. It is not implemented in P125.

Any future dashboard display line must preserve read-only semantics:

- display is review-only
- manual review is required
- output is not a trade instruction
- read-only candidate context does not imply production `VALID`
- read-only candidate context does not imply ExecutionPlan readiness
- read-only candidate context does not imply order, execution, scheduler, automation, or auto-trading

Future display work must not convert review-only candidate context into actionable UI language.

## Allowed Future Display Semantics

Allowed future display semantics are limited to:

- `review-only`
- `manual review required`
- `not trade instruction`
- `requires evidence review`
- `missing required evidence`
- `blocked by no-go / forbidden / Risk Action Guard evidence`
- `source-owned context for review`

Future display copy must make the non-actionable status visible near the candidate status, source ownership context, and any blocker evidence.

## Forbidden UI Labels

Future dashboard display work must not use these UI labels for read-only candidate output:

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

Equivalent labels, icons, badges, button text, hover text, tooltip text, aria labels, route names, component names, or data attributes that imply the same action are also blocked unless a later issue explicitly authorizes a different bounded scope.

## Required UI Copy Boundaries

Future dashboard copy must preserve these meanings:

- `REVIEW_ONLY_CANDIDATE` means review-only context, not actionable instruction.
- `INCOMPLETE` means missing required evidence, not safe-to-trade.
- `BLOCKED` means no-go / forbidden / Risk Action Guard blocked, not an opportunity.

Required state handling:

- Missing evidence -> `INCOMPLETE` display only.
- Forbidden input blocker -> `BLOCKED` display only.
- No-go evidence blocker -> `BLOCKED` display only.
- Risk Action Guard blocker -> `BLOCKED` display only.
- Risk high + stampede -> forbid reverse / new position / opportunity push display.
- Risk high + wick only -> no trend reversal display.

Future UI must not render blocked states as opportunity, momentum, reversal, signal, buy, sell, open, close, reverse, execution, automation, or readiness language.

## Explicit Non-Authorization

P125 does not authorize:

- dashboard readiness mutation
- ExecutionPlan readiness mutation
- controller changes
- endpoint changes
- API changes
- schema changes
- config changes
- service registration
- Spring bean registration
- runtime data reads
- live market data reads
- external data fetches
- production candidate generation
- real entry / stop / TP / RR value generation
- BoundaryCandidateService `VALID` production path
- `BoundaryCandidateDTO.valid(...)` calls
- production `BoundaryStatusEnum.VALID` mapping
- order API
- execution API
- scheduler behavior
- automation behavior
- auto-trading behavior

## Future Dashboard Authorization Gates

Any future dashboard display line must pass every gate before implementation begins.

### Gate 1: Explicit New Issue And Manual Approval

- A new issue must explicitly authorize dashboard read-only display work.
- The issue must state that display remains read-only and non-actionable.
- Broad language such as "show it", "make dashboard ready", "wire dashboard", or "enable signals" is insufficient.

### Gate 2: Exact Allowed Files Listed

- The future issue must list exact allowed files.
- `dashboard.html`, controller, endpoint, schema, config, mapper, runtime, service, and API files remain blocked unless explicitly listed.
- Any unlisted file remains out of scope.

### Gate 3: Copy Uses Review-Only Wording

- UI copy must use review-only wording.
- UI copy must show manual review required.
- UI copy must show not trade instruction.
- UI copy must preserve blocker evidence and missing evidence context.

### Gate 4: No Trade Instruction Language

- No buy / sell / open / close / reverse / signal language.
- No labels, badges, buttons, tooltips, headings, route names, or data attributes that imply action.
- No copy that implies a user should place, close, reverse, or automate a position.

### Gate 5: No Production VALID / Readiness Language

- No production `VALID` copy.
- No readiness copy.
- No trade-ready copy.
- No ready-to-trade copy.
- No executable-state copy.
- No dashboard readiness mutation.
- No ExecutionPlan readiness mutation.

### Gate 6: No Order / Execution / Automation Surface

- No order API.
- No execution API.
- No scheduler behavior.
- No automation behavior.
- No auto-trading behavior.
- No UI command path that can create or imply those behaviors.

### Gate 7: Missing Evidence And Blocked States Display As Non-Actionable

- Missing evidence must display only as `INCOMPLETE`.
- Forbidden input blockers must display only as `BLOCKED`.
- No-go evidence blockers must display only as `BLOCKED`.
- Risk Action Guard blockers must display only as `BLOCKED`.
- `INCOMPLETE` and `BLOCKED` displays must not imply safe-to-trade, opportunity, or readiness.

### Gate 8: Risk Action Guard Blockers Visible And Non-Actionable

- Risk Action Guard blocker evidence must remain visible.
- Risk high + stampede must forbid reverse / new position / opportunity push display.
- Risk high + wick only must not display trend reversal.
- Risk Action Guard output must remain review-only and cannot become a direct action instruction.

### Gate 9: Rollback Path Documented

- Future display work must identify the last approved freeze point.
- Future display work must identify exact files that can be reverted.
- Future display work must define how to remove any accidental actionable copy, readiness language, dashboard mutation, endpoint/API/schema/config change, or order/execution/automation surface.

## Future Dashboard Tests

Future dashboard display work must define tests before implementation.

Required future tests:

- Copy test proving `REVIEW_ONLY_CANDIDATE` renders as review-only context and not actionable instruction.
- Copy test proving `INCOMPLETE` renders as missing required evidence and not safe-to-trade.
- Copy test proving `BLOCKED` renders as no-go / forbidden / Risk Action Guard blocked and not opportunity.
- Guard test rejecting forbidden UI labels: `buy`, `sell`, `open`, `close`, `reverse`, `signal`, `trade-ready`, `ready-to-trade`, `production VALID`, `auto-trading`.
- Guard test proving no dashboard readiness mutation.
- Guard test proving no ExecutionPlan readiness mutation.
- Guard test proving no controller / endpoint / API / schema / config authorization unless explicitly approved.
- Guard test proving no order / execution / scheduler / automation / auto-trading surface.
- Risk Action Guard display test proving risk high + stampede forbids reverse / new position / opportunity push display.
- Risk Action Guard display test proving risk high + wick only does not display trend reversal.
- Rollback test or checklist proving the UI can return to the last non-actionable display state.

The existing D-line command set remains required before and after future display work:

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

## Future PR Body Checklist

Any future dashboard display PR must include:

- Files changed.
- Manual approval reference.
- Exact dashboard display boundary changed.
- Exact allowed files.
- Confirmation that display remains review-only.
- Confirmation that manual review is required.
- Confirmation that output is not a trade instruction.
- Confirmation that forbidden UI labels are absent.
- Confirmation that `REVIEW_ONLY_CANDIDATE`, `INCOMPLETE`, and `BLOCKED` copy boundaries are preserved.
- Confirmation that missing evidence displays only as `INCOMPLETE`.
- Confirmation that forbidden / no-go / Risk Action Guard blockers display only as `BLOCKED`.
- Confirmation that Risk Action Guard blockers remain visible and non-actionable.
- Confirmation that no dashboard readiness mutation was added.
- Confirmation that no ExecutionPlan readiness mutation was added.
- Confirmation that no controller / endpoint / API / schema / config authorization was added unless explicitly approved.
- Confirmation that no order / execution / scheduler / automation / auto-trading surface was added.
- Tests and guards run.
- Rollback plan.
- Still-blocked paths.
- Boundary confirmations.

## Rollback Expectations

Future dashboard display work must document rollback before implementation begins.

Rollback must:

- identify the last approved freeze point
- identify exact files that can be reverted
- remove any actionable UI copy introduced by the future PR
- remove any forbidden UI label introduced by the future PR
- remove any readiness language introduced by the future PR
- remove any dashboard readiness mutation introduced by the future PR
- remove any ExecutionPlan readiness mutation introduced by the future PR
- remove any controller / endpoint / API / schema / config change introduced by the future PR
- remove any order / execution / scheduler / automation / auto-trading surface introduced by the future PR
- restore review-only wording
- restore `manualReviewRequired=true`
- restore `notTradeInstruction=true`
- restore `reviewMode=REVIEW_ONLY`
- restore `REVIEW_ONLY_CANDIDATE` as non-actionable context
- restore `INCOMPLETE` as missing evidence only
- restore `BLOCKED` as no-go / forbidden / Risk Action Guard blocked only

If rollback cannot be applied cleanly, the future PR must remain blocked and must not proceed into dashboard display behavior.

## Still-Blocked Paths

The following paths remain blocked after P125:

- dashboard implementation
- `dashboard.html` changes
- dashboard UI code
- dashboard readiness mutation
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
- schema changes
- config changes
- controller / endpoint Java
- API wiring
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

- P125 is documentation-only authorization plan work.
- P125 removes the placeholder `docs/P125.md`.
- P125 adds one dashboard read-only display authorization plan document.
- P125 remains within the D line, Production Authorization Preparation / Safety Gate.
- P125 does not authorize dashboard implementation.
- P125 does not modify `dashboard.html`.
- P125 does not add dashboard UI code.
- P125 does not add endpoints.
- P125 does not wire runtime/API paths.
- P125 does not display trade instructions.
- P125 does not authorize production wiring.
- P125 does not authorize order, execution, scheduler, automation, or auto-trading.
- P125 does not modify production Java.
- P125 does not modify test source.
- P125 does not implement production candidate generation.
- P125 does not generate real entry / stop / TP / RR values.
- P125 does not read runtime data.
- P125 does not read live market data.
- P125 does not fetch external data.
- P125 does not add exchange clients, `WebClient`, or `RestTemplate`.
- P125 does not wire BoundaryCandidateService `VALID` production path.
- P125 does not call `BoundaryCandidateDTO.valid(...)`.
- P125 does not map to production `BoundaryStatusEnum.VALID`.
- P125 does not upgrade ExecutionPlan readiness.
- P125 does not modify schema.
- P125 does not modify config.
- P125 does not add controller / endpoint Java.
- P125 does not add service registration.
- P125 does not add order API.
- P125 does not add execution API.
- P125 does not add scheduler / automation / auto-trading.

## Validation

P125 is documentation-only, so Maven was skipped. Validation is limited to git diff checks confirming the placeholder removal and dashboard read-only display authorization plan document only.
