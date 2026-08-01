# P1B-1 Home Structure and Projection Alignment Authorization

Status: `AUTHORIZED_PENDING_IMPLEMENTATION_AFTER_MERGED_MAIN`

Effectivity: This is a bounded authorization candidate only. It does not
authorize implementation until this document and its companion roadmap/task
records are independently reviewed and merged to clean, synced `main`.

## Product Sources

- `docs/PRODUCT_SOURCE_OF_TRUTH.md`
- `docs/design/P3_U2_IPHONE_HOME_SEMANTIC_CONTRACT.md`
- `docs/PRODUCT_ROADMAP_V2.md`
- `docs/PRODUCT_ACCEPTANCE_STANDARD.md`

## Problem

The current Home read projection follows this path:

```text
selectedSymbol
-> matching UserPosition selection
-> ExecutionSuggestion builder
-> POSITION_MONITORING
```

When a matching open UserPosition exists, `DashboardHomeServiceImpl` replaces
the selected asset's Execution Plan projection with position-monitoring
content. Position-selection failures can also block the asset plan. This mixes
two separate product domains.

## Product Rule

`ExecutionPlan` is the system's current plan suggestion for the selected
asset. It remains a review-only system suggestion and never represents a user
action or UserPosition.

`Position Monitor` is the owner-scoped read model for the authenticated user's
manually entered positions.

Both modules must be present independently. UserPosition existence, selection,
or monitoring state must not replace or gate the selected asset's
ExecutionPlan.

## Authorized Mapping

The authorized implementation must adjust the existing Home projection. It
must not add a new endpoint, response variant, or DTO field.

1. `executionSuggestion` is assembled only from `selectedDecision` and the
   existing asset ExecutionPlan validation/provenance rules.
2. `positions` continues to come only from owner-scoped UserPosition reads.
3. `selectedSymbol` may scope only selected-asset status, AssetState,
   ExecutionPlan, AI summaries, and the AI consistency summary.
4. Position selection is permitted only by an explicit, exact, owner-scoped
   position identity. It must not be inferred from `selectedSymbol`.
5. Missing, ambiguous, or mismatched position selection must not block or
   replace the selected asset's ExecutionPlan.
6. Existing Home compatibility fields may remain in the JSON shape, but the
   asset-plan projection must keep `positionMode=false` and
   `positionMonitor=null`.
7. Asset-context refresh failure must clear selected-asset KPI, ExecutionPlan,
   AI summary, AI consistency, and detail-entry state before rendering Error
   or Missing. Stale success cache must not restore those values.

## Allowed Scope

- `DashboardHomeServiceImpl` read-only Home projection assembly.
- Existing `DashboardHomeVO` projection semantics without adding, removing,
  or renaming JSON fields.
- Desktop and mobile Home template, CSS, and JavaScript bindings needed to
  present the separated projections and final module order.
- Focused service, controller-contract, DOM, WKWebView, accessibility, and
  stale-cache tests for this authorization.

## Unchanged

- `DashboardHomeController` endpoint, authentication, query parameters, and
  response envelope.
- `DashboardHomeVO` JSON shape.
- API routes and API schema.
- Database schema and persistence model.
- ExecutionPlan validation rules and state machine.
- UserPosition model, ownership rules, and lifecycle.
- PositionMonitor calculations and state machine.
- AI role, conflict, evidence, and fallback logic.
- Score algorithms and score provenance.

## Blocked Scope

- New endpoint, route, request parameter, response field, or DTO variant.
- Schema migration, Mapper refactor, or unrelated query change.
- UserPosition creation, update, close, reduce, add, reverse, or selection
  write.
- Order, execution, automatic trading, or any trading mutation.
- ExecutionPlan state-machine change.
- PositionMonitor algorithm change.
- AI logic, model, role, scoring, evidence, or conflict change.
- Score algorithm or confidence-calibration change.
- Message, Push, notification, external send, or Telegram capability.
- Frontend relabeling that presents POSITION_MONITORING data as an asset
  ExecutionPlan.

## Acceptance Tests

1. With an open UserPosition for the selected asset, Home returns both the
   asset ExecutionPlan and the owner-scoped Position Top3 projection.
2. Multiple positions for the same asset do not block or replace the asset
   ExecutionPlan.
3. Switching from asset A to asset B updates AssetState, ExecutionPlan, AI
   summaries, and AI consistency while leaving UserPosition content and an
   explicit position identity unchanged.
4. READY, PARTIAL, EMPTY, ERROR, and MISSING remain independent. Error is not
   rendered as Empty or restored from stale success cache.
5. Refresh failure exposes no old asset KPI, ExecutionPlan, AI summary, AI
   consistency, or detail link.
6. Every path remains read-only and proves that no UserPosition or trading
   mutation occurs.
7. Desktop and mobile render the same module order: status, alerts/events,
   focus assets, ExecutionPlan, Top3 positions, and three-AI summary.

## Authorization Effectivity

Before merge, this package is documentation-only and implementation remains
blocked. After independent review, merge, main sync, Product Source Gate PASS,
clean worktree, and explicit runtime recognition of this record, the next
allowed package is `P1B_HOME_ALIGNMENT_FIRST_IMPLEMENTATION` with scope
`HOME_READ_PROJECTION_ONLY`.

This authorization does not mark P1B-1 implemented, validated, accepted, or
complete.
