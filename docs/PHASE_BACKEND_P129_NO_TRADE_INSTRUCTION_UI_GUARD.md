# BACKEND-P129 No Trade Instruction UI Guard

## Baseline

- Branch context: PR #370 / Issue #369.
- Formal mainline title: BACKEND-P129 No Trade Instruction UI Guard.
- PR title note: PR #370 uses a shortened title as a platform workaround; Issue #369 and this document preserve the formal mainline title.
- Baseline commit: `a4168d1` (`chore: add P129 placeholder`), based on `1a2fa74` (`P128 ExecutionPlan Display Contract (#368)`).
- Scope: documentation-only No Trade Instruction UI Guard contract for future dashboard / ExecutionPlan display work.
- Line context: P129 continues the E line, Dashboard / ExecutionPlan Read-Only Display Line.
- Placeholder removed: `docs/P129.md`.

## Files Changed

- `docs/PHASE_BACKEND_P129_NO_TRADE_INSTRUCTION_UI_GUARD.md`
- Removed `docs/P129.md`

No production Java, test source, runtime, dashboard HTML, dashboard UI code, schema, config, controller, endpoint, API wiring, service registration, readiness, order, execution, scheduler, automation, auto-trading, exchange-client, or external-data files are changed.

## Scope Statement

P129 is a no-trade-instruction UI guard contract only.

P129 does not implement dashboard UI. P129 does not modify `dashboard.html`. P129 does not add endpoints. P129 does not wire runtime or API paths. P129 does not upgrade readiness. P129 does not add production wiring. P129 does not add order, execution, scheduler, automation, or auto-trading behavior.

The guard prevents future dashboard / ExecutionPlan display work from introducing trade-instruction language, action affordances, readiness language, executable-state language, order surfaces, execution surfaces, scheduler surfaces, automation surfaces, or auto-trading surfaces.

## Required Review-Only Display Flags

Future UI / display work must visibly preserve:

- `manualReviewRequired=true`
- `notTradeInstruction=true`
- `reviewMode=REVIEW_ONLY`

These flags must be rendered as display-only safety context. They must not become buttons, badges, tooltips, aria labels, route names, component names, data attributes, action affordances, readiness affordances, order affordances, execution affordances, scheduler affordances, automation affordances, or auto-trading affordances.

## State Copy Guard

Future dashboard / ExecutionPlan UI copy must preserve these meanings:

- `REVIEW_ONLY_CANDIDATE` remains non-actionable context only.
- `INCOMPLETE` remains missing-evidence context only.
- `BLOCKED` remains no-go / forbidden / Risk Action Guard blocked context only.

Required state handling:

- `REVIEW_ONLY_CANDIDATE` must not imply buy, sell, open, close, reverse, signal, executable state, readiness, order, execution, scheduler, automation, or auto-trading.
- `INCOMPLETE` must not imply safe-to-trade, no risk, low risk, ready-to-trade, executable state, or review passed.
- `BLOCKED` must not imply opportunity, reversal, recommendation, signal, order, execution, scheduler, automation, or auto-trading.
- Missing evidence must display only as missing-evidence context.
- Forbidden input, no-go evidence, or Risk Action Guard blocker evidence must display only as blocked context.

UI copy may describe review context, missing evidence, or blocker evidence. UI copy must not instruct, suggest, imply, or afford a trade action.

## Forbidden Labels And UI Text

Future dashboard / ExecutionPlan display work must not use these labels or UI text for read-only candidate context:

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

Case changes, punctuation changes, camelCase, snake_case, kebab-case, abbreviations, translations, icons, badges, or equivalent terms that imply the same behavior remain blocked unless a later issue explicitly authorizes a bounded exception.

## Forbidden UI Surfaces

Future dashboard / ExecutionPlan display work must not introduce forbidden labels, action copy, readiness copy, or executable-state copy through:

- buttons
- badges
- tooltips
- aria labels
- route names
- component names
- data attributes
- headings
- tabs
- menu items
- modal titles
- link text
- icon labels
- hover text
- CSS class names
- view-model field names
- DTO field names
- API payload fields
- test fixture display names

The guard applies to visible copy and non-visible UI metadata because screen readers, route names, selectors, and component names can become user-facing or integration-facing action surfaces.

## Forbidden Action And Readiness Surfaces

Future display work must not introduce:

- trade-instruction language
- action affordances
- readiness language
- executable-state language
- order plan language
- execution plan language
- close plan language
- reverse plan language
- scheduler language
- automation language
- auto-trading language
- generated entry / stop / TP / RR values
- real trading value display

This includes direct UI text, labels, badges, buttons, tooltips, aria labels, route names, component names, data attributes, DTO fields, view-model fields, CSS classes, tests, or documentation attached to implementation PRs.

## Risk Action Guard UI Guard

Risk Action Guard output must remain visible and non-actionable.

Required no-action rules:

- Stampede must not display reverse.
- Stampede must not display new position.
- Stampede must not display opportunity push.
- Wick-only evidence must not display trend reversal.
- Wick-only evidence must not display reverse entry.
- Risk high with deteriorating liquidity must not display one-shot market exit instruction.
- Missing event evidence must not display as no risk.
- Liquidity stress must not display as opportunity.

Risk Action Guard display may identify blocker evidence for manual review. It must not become action copy, readiness copy, executable-state copy, order copy, execution copy, scheduler copy, automation copy, or auto-trading copy.

## Required Grep / Search Patterns

Future UI / display PR review must search changed files for these patterns:

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

Search hits are not automatically invalid if they are inside guard documentation or negative assertions, but every hit must be reviewed. Any positive use as a UI label, action affordance, readiness state, executable state, route, component, DTO field, view-model field, payload field, selector, or data attribute remains blocked unless separately authorized.

## Future UI Guard Tests

Future dashboard / ExecutionPlan display implementation must define guard tests before implementation begins.

Required future test coverage:

- `manualReviewRequired=true` is displayed as safety context.
- `notTradeInstruction=true` is displayed as safety context.
- `reviewMode=REVIEW_ONLY` is displayed as safety context.
- `REVIEW_ONLY_CANDIDATE` renders as non-actionable context only.
- `INCOMPLETE` renders as missing-evidence context only.
- `BLOCKED` renders as no-go / forbidden / Risk Action Guard blocked context only.
- Forbidden labels and UI text are absent.
- Forbidden buttons, badges, tooltips, aria labels, route names, component names, and data attributes are absent.
- Trade-instruction language is absent.
- Action affordances are absent.
- Readiness language is absent.
- Executable-state language is absent.
- Order / execution / scheduler / automation / auto-trading surfaces are absent.
- Real entry / stop / TP / RR values are absent.
- `entryPrice`, `stopPrice`, `takeProfitPrice`, and `riskRewardValue` fields are absent from UI display surfaces.
- Risk Action Guard no-action rules remain visible and non-actionable.
- Stampede does not display reverse / new position / opportunity push.
- Wick-only evidence does not display trend reversal.
- Risk high with deteriorating liquidity does not display one-shot market exit instruction.
- Missing event evidence does not display as no risk.
- Liquidity stress does not display as opportunity.
- No dashboard readiness mutation is present.
- No ExecutionPlan readiness mutation is present.
- No controller, endpoint, API, schema, config, service registration, dashboard implementation, or `dashboard.html` change is present unless explicitly authorized.
- Rollback can restore the last approved no-trade-instruction display state.

Future PRs must include both positive copy tests for allowed review-only wording and negative guard tests for forbidden trade-instruction wording.

## Future PR Review Checklist

Any future UI / display PR must confirm:

- all required review-only display flags remain visible
- no forbidden label appears as actionable UI text
- no forbidden label appears as a button, badge, tooltip, aria label, route name, component name, or data attribute
- no readiness language appears
- no executable-state language appears
- no order / execution / scheduler / automation / auto-trading surface appears
- no real entry / stop / TP / RR value appears
- Risk Action Guard no-action rules remain visible
- missing evidence remains `INCOMPLETE`
- no-go / forbidden / Risk Action Guard blocked context remains `BLOCKED`
- rollback is documented before implementation

If any checklist item fails, the future PR must remain blocked.

## Rollback Expectations

Future UI / display work must document rollback before implementation begins.

Rollback must:

- identify the last approved no-trade-instruction display freeze point
- identify exact files that can be reverted
- remove any trade-instruction language introduced by a future PR
- remove any action affordance introduced by a future PR
- remove any readiness language introduced by a future PR
- remove any executable-state language introduced by a future PR
- remove any forbidden button, badge, tooltip, aria label, route name, component name, or data attribute introduced by a future PR
- remove any order, execution, scheduler, automation, or auto-trading surface introduced by a future PR
- remove any real entry / stop / TP / RR display or generation introduced by a future PR
- remove any dashboard implementation, `dashboard.html`, dashboard UI code, schema, config, controller, endpoint, API, service registration, or Spring bean registration introduced by a future PR
- remove any production wiring introduced by a future PR
- remove any production `VALID`, BoundaryCandidateService `VALID`, `BoundaryCandidateDTO.valid(...)`, or production `BoundaryStatusEnum.VALID` mapping introduced by a future PR
- restore `manualReviewRequired=true`
- restore `notTradeInstruction=true`
- restore `reviewMode=REVIEW_ONLY`
- restore `REVIEW_ONLY_CANDIDATE` as non-actionable context only
- restore `INCOMPLETE` as missing-evidence context only
- restore `BLOCKED` as no-go / forbidden / Risk Action Guard blocked context only

If rollback cannot be applied cleanly, the future PR must remain blocked and must not proceed.

## Still-Blocked Paths

The following paths remain blocked after P129:

- dashboard implementation
- `dashboard.html` changes
- dashboard UI code
- dashboard readiness mutation
- ExecutionPlan readiness mutation
- ExecutionPlan readiness upgrade
- executable plan state
- action affordances
- action plan
- order plan
- close plan
- reverse plan
- automation plan
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

- P129 is documentation-only no-trade-instruction UI guard work.
- P129 continues the E line, Dashboard / ExecutionPlan Read-Only Display Line.
- P129 removes the placeholder `docs/P129.md`.
- P129 adds one No Trade Instruction UI Guard document.
- P129 does not implement dashboard UI.
- P129 does not modify `dashboard.html`.
- P129 does not add endpoints.
- P129 does not wire runtime or API paths.
- P129 does not upgrade readiness.
- P129 does not add production wiring.
- P129 does not authorize order, execution, scheduler, automation, or auto-trading.
- P129 does not modify production Java.
- P129 does not modify test source.
- P129 does not add dashboard UI code.
- P129 does not add controller / endpoint Java.
- P129 does not modify schema.
- P129 does not modify config.
- P129 does not add service registration.
- P129 does not read runtime data.
- P129 does not read live market data.
- P129 does not fetch external data.
- P129 does not add exchange clients, `WebClient`, or `RestTemplate`.
- P129 does not implement production candidate generation.
- P129 does not generate real entry / stop / TP / RR values.
- P129 does not wire BoundaryCandidateService `VALID` production path.
- P129 does not call `BoundaryCandidateDTO.valid(...)`.
- P129 does not map to production `BoundaryStatusEnum.VALID`.
- P129 does not add order API.
- P129 does not add execution API.
- P129 does not add scheduler / automation / auto-trading.

## Validation

P129 is documentation-only, so Maven may be skipped. Validation is limited to git diff checks confirming the No Trade Instruction UI Guard document and placeholder removal only.
