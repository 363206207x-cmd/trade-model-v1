# P1B Home Core Data Completion Authorization

Status: `AUTHORIZED_PENDING_MERGED_MAIN`

Package: `P1B_HOME_CORE_DATA_COMPLETION`

This product decision reconciles the compact Asset Card contract with the
read-only status metadata required to judge whether its values are usable. It
authorizes a later implementation package only after this authorization is
reviewed, merged to `main`, and accepted by the runtime gate. It does not
implement Home, change an API, change a schema, or move any product capability.

## 1. Seven-Field Primary Card Body

The primary card body retains exactly these seven business fields:

1. `symbol`;
2. `latestPrice`;
3. `direction`;
4. `score`;
5. `confidence`;
6. `riskLevel`;
7. `AssetState`.

These fields remain the visual and semantic priority. None may be replaced by
status metadata. Asset risk must remain separate from private UserPosition
risk, and AssetState must remain separate from ExecutionPlan, UserPosition,
and PositionMonitor state.

## 2. Four-Field Secondary Status Strip

The same Asset Card may contain a subordinate status strip, footer metadata
row, or non-navigating disclosure with:

1. `dataQuality`;
2. `multiTimeframeState`;
3. `Confused`;
4. `updatedAt`.

These are supporting status metadata, not additional primary card-body
fields. They must use lower visual emphasis than direction, confidence, risk,
and AssetState. Mobile may use compact labels or a secondary row, but the
values must remain readable and accessible. The strip must not navigate by
default and must contain no position or trading action.

```text
Asset Card
|-- Primary body: symbol / price / direction / score / confidence / risk / AssetState
`-- Secondary strip: data quality / timeframe state / Confused / updated time
```

## 3. Field Source Requirements

| Field | Required source | Fail-closed behavior |
|---|---|---|
| `symbol` | Authoritative watched-asset identity | Do not render a placeholder as an analyzed asset |
| `latestPrice` | Persisted market/OHLCV close | Empty when no real quote exists |
| `direction` | Current valid asset decision | Clear an old direction when the decision is unavailable |
| `score` | Returned rule/model score with source classification | Never label a fixed fallback as a real model score |
| `confidence` | Returned rule/model confidence with source classification | Preserve `REAL`, `DERIVED`, `FALLBACK`, `MISSING`, or `ERROR` semantics |
| `riskLevel` | Asset-risk projection | Never use private UserPosition risk |
| `AssetState` | Asset-state domain | Never reuse plan or position state |
| `dataQuality` | Source completeness/freshness evaluation | Missing or degraded data cannot appear fully ready |
| `multiTimeframeState` | Actual multi-timeframe aggregation | Use `MISSING` or `PARTIAL` when unavailable/incomplete |
| `Confused` | Existing backend Confused policy and persisted state | Never calculate the state in the frontend |
| `updatedAt` | Real quote, analysis, or decision business time | Never use render time as business freshness |

Runtime source classification is limited to `REAL`, `DERIVED`, `FALLBACK`,
`MISSING`, and `ERROR`. `MOCK` is not a successful runtime source.

## 4. Exact ExecutionPlan Identity

The implementation package may connect this exact read-only chain:

```text
selectedDecision
  -> sourceExecutionPlanId (or an equivalent persisted exact relation)
  -> exact persisted ExecutionPlan
  -> identity/source/state/boundary validation
  -> Home executionSuggestion
```

It must not select a plan by latest record, symbol, timeframe, or fuzzy
analysis identity. A usable plan must pass the existing source and review
policies, have complete required boundaries, and have
`needsRevalidation=false`. Missing identity is `MISSING`, incomplete data is
`PARTIAL`, no plan is `EMPTY` or `MISSING` according to the read contract, and
identity/state/source contradictions are `ERROR`.

If no real persisted exact relation exists, implementation must stop with
`REAL_DATA_INTEGRITY_BLOCKER`; this authorization does not permit a guessed ID
or a schema migration.

## 5. Owner-Scoped Top3 UserPosition

Home Top3 positions must come only from authenticated, owner-scoped
UserPosition reads. They remain independent of `selectedSymbol`; changing an
Asset Card may update asset, plan, AI, and consistency context but must not
change the position collection or select a position.

`tm_real_position`, ExecutionPlan, PushRecheck, global positions, inferred
positions, and another user's positions are forbidden sources. Exact position
interaction requires an owner-authorized string-safe `positionId`.

## 6. Home State Contract

Home modules independently support:

- `LOADING`;
- `READY`;
- `PARTIAL`;
- `EMPTY`;
- `ERROR`;
- `MISSING`.

A global ready label must not hide a local failure. Starting an asset switch
clears the prior asset KPI, ExecutionPlan, AI, consistency, detail identity,
and asset cache while retaining independent Top3 UserPosition data. A failed
switch remains `ERROR` or `MISSING`, exposes retry, and must not restore a stale
successful payload.

## 7. Allowed Scope After Merged-Main Effectivity

The later `P1B_HOME_CORE_DATA_COMPLETION` package may modify only the Home
read-only data chain required for:

- the seven primary Asset Card fields and four secondary status fields;
- explicit runtime source classifications;
- exact ExecutionPlan identity and fail-closed validation;
- owner-scoped independent Top3 UserPosition projection;
- six Home states, retry, and stale-context clearing;
- matching desktop/mobile bindings, fixtures, and focused tests.

Minimal read-only DTO/VO/mapper fields are allowed only when backed by an
existing real persisted relation and do not require schema change.

## 8. Blocked Scope

This authorization does not include:

- Three AI Evidence Package or AI provider/model expansion;
- Score algorithm or calibration redesign;
- a new external data provider;
- Telegram, system notification, external notification, or automatic send;
- UserPosition mutation;
- order, open, close, reverse, execution, or trading behavior;
- broad schema migration;
- inferred plan or position identity;
- changes to PR #1156 recovery assets.

## 9. Acceptance Tests

The implementation package must demonstrate:

1. seven primary fields remain complete and visually dominant;
2. four supporting fields remain subordinate and source truthful;
3. full, missing, fallback, degraded, and error Asset Card sources;
4. exact plan selection when latest and exact plans differ;
5. missing/mismatched/blocked/revalidation/incomplete plan states fail closed;
6. current-user Top3, more-than-three, same-symbol, empty, and cross-user cases;
7. asset selection does not alter UserPosition identity or collection;
8. all six Home states, retry success/failure, and stale-context clearing;
9. asset failure may coexist with retained independent UserPosition data;
10. desktop/mobile DOM, fixture, accessibility, and screenshot evidence;
11. no UserPosition/ExecutionPlan mutation, order, notification, or Telegram path.

## 10. Runtime Authorization

Before this file is effective on clean, synced merged main, an exact request
for `P1B_HOME_CORE_DATA_COMPLETION` must return
`BLOCKED_PENDING_AUTHORIZATION_MERGED_MAIN` with all mutation permissions
false. After merged-main validation, the exact request may return `ALLOWED`
with repository edits, implementation, and implementation PR creation true.

The implementation status remains `NOT_STARTED` until the separately reviewed
implementation package is merged and validated. Authorization is not
implementation and is not product completion.
