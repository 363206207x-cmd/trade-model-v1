# Fundamental AI Frontend Interaction Runtime Closure Authorization

Authorization package: `FRONTEND_INTERACTION_RUNTIME_CLOSURE`

Status: `AUTHORIZED_PENDING_MERGED_MAIN`

## Purpose

Authorize the bounded Desktop runtime interaction closure requested after the
approved Home live-data binding reached merged main. This package repairs
source-defined visible controls and validates the existing interaction chain;
it does not redesign the product.

## Existing Owners Reused

- Home search and runtime state: `home.html`, `home-runtime.js`, existing
  Dashboard controllers and read models.
- Asset discovery and membership: the existing market catalog search and
  Asset Pool controller/service.
- On-demand analysis: the existing
  `/api/asset-pool/search/{symbol}/analysis-preview` contract and Analysis
  routes.
- Positions, Plans, Messages, Reviews, Audit and My: their existing route,
  controller and persistence owners.

No second Asset Pool, Opportunity, Final plan, UserPosition, Message or review
owner may be created.

## Authorized Scope

- Add explicit Home search-result selection and selected-state rendering.
- Wire `按需分析`, `加入观察资产池` and `已在观察资产池` through existing
  catalog, preview and Asset Pool owners.
- Close source-defined dead Desktop controls on Home, Asset Pool and linked
  existing routes or overlays.
- Add focused interaction, controller, route and browser acceptance tests.
- Use local runtime data only; missing objects remain empty or unavailable.

## Forbidden Scope

- Figma, Mobile, Schema or product-contract changes.
- CoinGlass configuration, new providers, AI enablement or quality-threshold
  changes.
- Fake Opportunity, Candidate, Final, UserPosition or monitor data.
- Automatic open, close, add, reduce, reverse or exchange order behavior.
- Home architecture, module order, 70:30 layout or unrelated backend refactor.

## Effectivity

Before this authorization is merged to clean synchronized `main`, repository
implementation and implementation PR creation are blocked. After merged-main
validation, only `FRONTEND_INTERACTION_RUNTIME_CLOSURE` may resolve:

- `IMPLEMENTATION_ALLOWED=true`
- `PR_CREATION_ALLOWED=true`
- `CANONICAL_FIGMA_DESKTOP_IMPLEMENTATION_ALLOWED=false`
- `MOBILE_IMPLEMENTATION_ALLOWED=false`

Capability movement: bounded user-visible interaction completion only. No new
business capability and no trading authority.
