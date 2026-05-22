# BACKEND-P136 Static Guard Test Scope Gate

## Baseline

- Branch context: PR #384 / Issue #383.
- Formal mainline title: BACKEND-P136 Static Guard Test Scope Gate.
- PR title note: PR #384 uses a shortened title as a platform workaround; Issue #383 and this document preserve the formal mainline title.
- Baseline commit: `0f80990` (`P135 Global Freeze Closure (#382)`).
- Scope: documentation-only scope gate for the Static Guard Test Line.
- Line context: P136 starts the Static Guard Test Line.
- Placeholder removed: `docs/P136.md`.

## Files Changed

- `docs/PHASE_BACKEND_P136_STATIC_GUARD_TEST_SCOPE_GATE.md`
- Removed `docs/P136.md`

No production Java, test source, dashboard HTML, dashboard UI code, controller, endpoint, API, schema, config, service, mapper, runtime data reader, live market data reader, external data integration, readiness, order, execution, scheduler, automation, or auto-trading files are changed.

## Scope Gate Purpose

P136 defines the documentation-only scope gate for future static guard tests. It starts the Static Guard Test Line by naming the targets, forbidden wording, allowed negative guard context, mandatory safe labels, P137 file limits, search patterns, no-go triggers, rollback expectations, and boundary confirmations.

P136 itself does not add tests. P136 itself does not change UI. P136 does not authorize implementation work.

The purpose of the future static guard tests is to prevent dangerous dashboard, display, and documentation wording, plus accidental action surfaces, from entering the read-only system.

## Guard Targets

Future static guard tests must target only static text and static source review surfaces. The target set is:

- `src/main/resources/templates/dashboard.html`
- display docs
- read-only candidate docs
- ExecutionPlan display docs

The display-doc target includes documentation for dashboard display, read-only display contracts, no-trade-instruction UI guards, display skeletons, and display-line closure. The read-only-candidate-doc target includes documentation for source-owned candidate generation, missing-evidence behavior, blocked-context behavior, and read-only generator boundaries. The ExecutionPlan-display-doc target includes documentation that discusses ExecutionPlan display, ExecutionPlan readiness boundaries, and ExecutionPlan read-only candidate display.

The guard target set is read-only. Static guard tests may inspect file contents as text. They must not read runtime data, live market data, external data, database data, exchange data, or generated market values.

## Forbidden Positive Or Actionable Terms

The following terms are forbidden when they positively imply a trade action, action recommendation, executable plan, readiness state, production validity, or automation surface:

```text
buy
sell
open
close
reverse
signal
trade-ready
ready-to-trade
executable
production VALID
auto-trading
```

Case changes, punctuation changes, camelCase, snake_case, kebab-case, abbreviations, translated equivalents, icons, labels, badges, route names, component names, data attributes, field names, selectors, headings, tooltips, or aria labels that imply the same action or readiness meaning remain blocked unless a later issue explicitly authorizes a bounded exception.

## Reviewable Negative-Context Terms

The following terms are reviewable, not automatically forbidden, when used only in negative guard text:

```text
order
execution
reverse
signal
auto-trading
```

Allowed negative guard text means text that blocks, denies, forbids, documents absence of, or asserts non-authorization of an action surface. Examples of allowed negative contexts include blocked-path lists, no-go trigger lists, rollback expectations, test assertion descriptions, and boundary confirmations.

Any positive use of those terms as a label, affordance, route, data field, DTO surface, UI copy, readiness state, executable state, plan state, payload field, or implementation path is a no-go trigger.

## Mandatory Safe Labels

Future static guard tests must preserve the following safe labels where the target surface is expected to present read-only candidate context:

```text
review-only
manual review required
not trade instruction
missing evidence
blocked by guard
source-owned context
```

These labels are safety context only. They must not become action affordances, readiness indicators, executable-state indicators, production validity indicators, order surfaces, execution surfaces, scheduler surfaces, automation surfaces, or auto-trading surfaces.

## Future Static Test Categories

The Static Guard Test Line must use these future categories:

1. Forbidden actionable copy absence test.
2. Mandatory safe label presence test.
3. Negative-context allowed-term review test.
4. Dashboard static skeleton non-action test.
5. Documentation guard wording test.

P136 defines these categories only. P136 does not implement them.

## Allowed Files For P137

P137 may modify only:

- one focused test file under `src/test/java`, preferably `src/test/java/org/example/trademodel/dashboard/StaticNoTradeInstructionGuardTest.java`, or a similarly named static guard test path already consistent with repo conventions
- one P137 result document under `docs/`
- P137 placeholder removal

Any P137 test file must stay static. It may inspect approved target files as repository text. It must not instantiate application services, call runtime readers, fetch external data, generate market values, wire Spring context, add endpoints, or mutate dashboard behavior.

## Forbidden Implementation Files For P137

P137 must not modify:

- production Java
- `src/main/resources/templates/dashboard.html`
- controller files
- endpoint files
- API files
- schema files
- config files
- service files
- mapper files
- runtime data readers
- live market data readers
- external data integration
- order files
- execution files
- scheduler files
- automation files
- auto-trading files

P137 must remain a focused static test phase. It must not become production wiring, dashboard implementation, dashboard UI change, endpoint work, API wiring, service registration, data access, readiness upgrade, executable-state work, or action-surface work.

## Still-Blocked Paths

The following paths remain blocked after P136:

- production Java
- test source under P136
- `src/main/resources/templates/dashboard.html` changes
- dashboard UI implementation
- controller / endpoint Java
- API wiring
- schema changes
- config changes
- service changes
- mapper changes
- runtime data readers
- live market data readers
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

## Required Grep And Search Patterns

Future static guard work must search the changed files and the relevant target set for:

```text
buy
sell
open
close
reverse
signal
trade-ready
ready-to-trade
executable
production VALID
auto-trading
autoTrading
order
execution
scheduler
automation
entryPrice
stopPrice
takeProfitPrice
riskRewardValue
tradeReady
readyToTrade
orderAction
executionAction
closePlan
reversePlan
orderPlan
automationPlan
dashboard readiness
ExecutionPlan readiness
```

Search hits inside guard documentation or negative assertions require review. Search hits that positively imply a trade action, actionable plan, readiness state, executable state, production validity, real value display, order surface, execution surface, scheduler surface, automation surface, or auto-trading surface are no-go triggers.

## No-Go Triggers

The future static guard line must stop if any of the following appears:

- any UI or doc wording that positively implies trade action
- any button, link, form, click handler, fetch call, API call, or localStorage decision path
- any readiness language
- any executable state
- any production `VALID` wording outside guard or negative context
- any real entry / stop / TP / RR value
- any order / execution / scheduler / automation / auto-trading surface
- any production Java or dashboard implementation change

No-go triggers also include any runtime data read, live market data read, external data fetch, generated market value, BoundaryCandidateService `VALID` production path, `BoundaryCandidateDTO.valid(...)` call, production `BoundaryStatusEnum.VALID` mapping, ExecutionPlan readiness upgrade, dashboard readiness mutation, order API, execution API, scheduler path, automation path, or auto-trading path.

## Rollback Expectations

Future P137 and later static guard work must document rollback before implementation begins.

Rollback must:

- revert any file outside the exact allowed scope
- remove any positive trade-action wording
- remove any action affordance
- remove any button, link, form, click handler, fetch call, API call, or localStorage decision path
- remove any readiness wording
- remove any executable-state wording
- remove any production `VALID` wording outside guard or negative context
- remove any real entry / stop / TP / RR value
- remove any order / execution / scheduler / automation / auto-trading surface
- remove any production Java change
- remove any `dashboard.html` implementation change
- restore the last approved read-only static guard state
- keep `review-only`, `manual review required`, `not trade instruction`, `missing evidence`, `blocked by guard`, and `source-owned context` as safe labels only

If rollback cannot be applied cleanly, the future PR must remain blocked and must not proceed.

## Boundary Confirmations

- P136 is documentation-only scope-gate work.
- P136 starts the Static Guard Test Line.
- P136 removes the placeholder `docs/P136.md`.
- P136 adds one static guard test scope gate document.
- P136 does not add tests.
- P136 does not modify test source.
- P136 does not modify production Java.
- P136 does not modify `dashboard.html`.
- P136 does not change UI.
- P136 does not add dashboard UI code.
- P136 does not add controller / endpoint / API / schema / config / service changes.
- P136 does not modify mapper files.
- P136 does not modify runtime data readers.
- P136 does not modify live market data readers.
- P136 does not add external data integration.
- P136 does not read runtime data.
- P136 does not read live market data.
- P136 does not fetch external data.
- P136 does not generate real entry / stop / TP / RR values.
- P136 does not upgrade ExecutionPlan readiness.
- P136 does not map to production `VALID`.
- P136 does not wire BoundaryCandidateService `VALID` production path.
- P136 does not call `BoundaryCandidateDTO.valid(...)`.
- P136 does not add order API.
- P136 does not add execution API.
- P136 does not add scheduler / automation / auto-trading.

## Validation

P136 is documentation-only, so Maven may be skipped because no Java or test source is modified. Validation is limited to:

```text
git diff --check
git diff --cached --check
```

## PR Body Checklist

The PR body must include:

- files changed
- validation performed
- scope gate coverage
- future static test categories
- allowed P137 test files
- forbidden P137 implementation files
- grep/search patterns
- no-go triggers
- rollback expectations
- still-blocked paths
- boundary confirmations
- note that the PR short title is only a platform workaround; formal mainline is Issue #383 / BACKEND-P136

P136 stops here. It does not merge the PR.
