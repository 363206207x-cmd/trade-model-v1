# BACKEND-P130 Dashboard Display Skeleton

## Baseline

- Branch context: PR #372 / Issue #371.
- Formal mainline title: BACKEND-P130 Dashboard Display Skeleton.
- PR title note: PR #372 uses a shortened title as a platform workaround; Issue #371 and this document preserve the formal mainline title.
- Baseline commit: `f8b474c` (`chore: add P130 placeholder`), based on `5d507f1` (`P129 UI Guard (#370)`).
- Scope: minimal dashboard read-only display skeleton.
- Line context: P130 continues the E line, Dashboard / ExecutionPlan Read-Only Display Line.
- Placeholder removed: `docs/P130.md`.

## Files Changed

- `src/main/resources/templates/dashboard.html`
- `docs/PHASE_BACKEND_P130_DASHBOARD_DISPLAY_SKELETON_RESULT.md`
- Removed `docs/P130.md`

No production Java, test source, controller, endpoint, API, schema, config, service, runtime, live-market, external-data, readiness, order, execution, scheduler, automation, or auto-trading files are changed.

## Skeleton Coverage

P130 adds one static dashboard skeleton section:

- Title: `Read-only candidate display`
- Required copy: `Review-only candidate context`
- Required copy: `Manual review required`
- Required copy: `Not a trade instruction`
- Required copy: `Missing evidence remains incomplete`
- Required copy: `Blocked guard evidence remains non-actionable`
- Required copy: `Risk Action Guard: stampede / wick-only / liquidity stress never become direct action`
- Future authorization copy: `Future read-only candidate context will appear here only after separate authorization.`
- Guard note: `INCOMPLETE = missing evidence`
- Guard note: `BLOCKED = no-go / Risk Action Guard blocked`
- Guard note: `No order, execution, reverse, signal, or auto-trading action is available here.`
- Source boundary: `source-owned context`
- Display boundary: `review-only / manual review required / not trade instruction`

The skeleton is static markup only. It does not bind runtime data, does not call an API, does not read live or external data, does not generate real values, and does not create a user action path.

## Forbidden Labels Check

The P130 skeleton does not introduce positive/actionable UI labels for:

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

The skeleton includes `order`, `execution`, `reverse`, `signal`, and `auto-trading` only inside the required negative guard sentence: `No order, execution, reverse, signal, or auto-trading action is available here.`

The skeleton does not add buttons, links, click handlers, fetch calls, forms, localStorage-backed decision logic, order / execution / position action CSS reuse, or IDs/classes that imply buy, sell, open, close, reverse, signal, order, execution, ready, valid, or executable behavior.

## Static UI Boundary

The dashboard section remains:

- read-only
- manual-review-required
- not a trade instruction
- source-owned context only
- missing evidence as incomplete only
- guard-blocked context as non-actionable only

Risk Action Guard wording remains non-actionable. Stampede, wick-only, and liquidity stress are presented as blockers against direct action, not as opportunities, entries, exits, reversals, or execution prompts.

## Validation Performed

- `git diff --check`
- `git diff --cached --check`

Maven was skipped because P130 changes only static dashboard markup and documentation. A source scan found dashboard/service/controller tests, but no focused static dashboard-template copy test that targets only `dashboard.html` skeleton copy without exercising controller/API/runtime wiring outside the P130 scope.

## Rollback Expectations

Rollback is limited to:

- remove the static dashboard skeleton section from `src/main/resources/templates/dashboard.html`
- remove this P130 result document
- restore the previous placeholder state only if the PR is abandoned before merge

Rollback must not touch production Java, controller, endpoint, API, schema, config, service, runtime data, live data, external data, readiness, order, execution, scheduler, automation, or auto-trading paths.

## Still-Blocked Paths

The following paths remain blocked after P130:

- production Java changes
- test source changes unless a future issue explicitly authorizes focused static template tests
- controller / endpoint Java
- API wiring
- schema changes
- config changes
- service registration
- runtime data reads
- live market data reads
- external data fetches
- external data integration
- exchange clients
- `WebClient`
- `RestTemplate`
- real entry / stop / TP / RR value generation
- generated trading values
- dashboard readiness mutation
- ExecutionPlan readiness mutation
- ExecutionPlan readiness upgrade
- production `VALID` mapping
- BoundaryCandidateService `VALID` production path
- `BoundaryCandidateDTO.valid(...)` calls
- production `BoundaryStatusEnum.VALID` mapping
- order API
- execution API
- scheduler / automation / auto-trading
- localStorage-backed decision logic for this skeleton
- default trading action
- production ownership review wiring
- production completion
- production adapter
- `DefaultSourceTraceEntryOwnershipAdapter`
- production `DefaultSourceTraceEntryCompletionContract`
- runtime SourceTrace field population
- full SourceTrace runtime completion

## Boundary Confirmations

- P130 adds a minimal non-actionable dashboard skeleton only.
- P130 modifies only `src/main/resources/templates/dashboard.html` plus docs.
- P130 removes the placeholder `docs/P130.md`.
- P130 does not modify production Java.
- P130 does not modify test source.
- P130 does not add controller / endpoint Java.
- P130 does not modify schema.
- P130 does not modify config.
- P130 does not add service registration.
- P130 does not read runtime data.
- P130 does not read live market data.
- P130 does not fetch external data.
- P130 does not create API fetch calls.
- P130 does not create endpoint wiring.
- P130 does not create forms.
- P130 does not create click handlers.
- P130 does not create localStorage-backed decision logic.
- P130 does not generate real entry / stop / TP / RR values.
- P130 does not add order API.
- P130 does not add execution API.
- P130 does not add scheduler / automation / auto-trading.
- P130 does not upgrade ExecutionPlan readiness.
- P130 does not map to production `VALID`.
- P130 does not wire BoundaryCandidateService `VALID` production path.
- P130 does not call `BoundaryCandidateDTO.valid(...)`.

## PR Body Checklist

The PR body must include:

- files changed
- validation performed
- skeleton coverage
- forbidden labels check
- still-blocked paths
- rollback expectations
- boundary confirmations
- note that the PR short title is only a platform workaround; formal mainline is Issue #371 / BACKEND-P130

P130 stops here. It does not merge the PR.
