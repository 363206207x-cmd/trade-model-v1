# Fundamental AI v4.1 Schema And API Changelog

Status: `DYNAMIC_ASSET_RANKING_COMPLETE_PENDING_REAUDIT`

## Schema V11

Migration:
`src/main/resources/db/migration/V11__fundamental_ai_v4_1_decision_chain.sql`

New tables:

1. `tm_asset_pool_item`
2. `tm_opportunity_state_transition`
3. `tm_execution_plan_candidate`
4. `tm_conflict_resolver_result`

Extended tables:

| Table | Added contract |
|---|---|
| `tm_asset_state` | Opportunity identity by symbol+timeframe, state-entry/cooling metadata, last transition reason/source/analysis, exact eight-state constraint |
| `tm_execution_plan` | Candidate/Opportunity/Resolver/Trace links, chain status, Rule Validation status/veto, Final timestamp and Final marker |
| `tm_user_position` | explicit manual/system-plan source contract and validated `final_plan_id` association; no automatic position creation |
| `tm_review_result` | Final/Candidate/Trace links |
| `tm_ai_call_log` | decision-chain contract type, Candidate link, output payload, explicit no-Final authority |

Safety and compatibility:

- six system default Asset Pool entries are seeded idempotently;
- legacy unknown AssetState values are normalized to `OBSERVING` before the
  exact-state constraint is applied;
- historical Opportunity rows receive `timeframe=global`; new state uniqueness
  is `(symbol, timeframe)`;
- historical UserPosition source `MANUAL` is normalized to
  `MANUAL_POSITION`;
- existing ExecutionPlan rows remain `LEGACY` and are not presented as newly
  validated Final plans;
- database checks prevent Candidate authority escalation, invalid Final rows,
  noncanonical Conflict Levels, invalid UserPosition source/Final pairings, and
  AI role authority escalation;
- foreign keys preserve Candidate -> Resolver -> Final and explicit
  UserPosition/Review provenance.

`src/main/resources/schema.sql` mirrors the V11 contract for H2/local tests.

## New Asset Pool API

Base path: `/api/asset-pool`

| Method | Path | Behavior |
|---|---|---|
| `GET` | `/api/asset-pool` | authenticated effective system + user pool |
| `GET` | `/api/asset-pool/search?query=&limit=` | authenticated full-market/fuzzy catalog search |
| `POST` | `/api/asset-pool` | explicit authenticated add |
| `DELETE` | `/api/asset-pool/{symbol}` | explicit authenticated remove/override |
| `POST` | `/api/asset-pool/restore-default` | remove user overrides and restore defaults |
| `POST` | `/api/asset-pool/scan?timeframe=` | explicit analysis scan over the effective user pool |

## Extended Existing API Contracts

- UserPosition create/read requires an explicit source. A
  `SYSTEM_PLAN_POSITION` requires a `finalPlanId` for a Rule Validation PASS
  Final of the same symbol; a `MANUAL_POSITION` explicitly carries no Final
  plan. UserPosition creation remains an explicit user action.
- AI trace query responses expose terminal error, Candidate/contract identity,
  and persisted role output needed to reconstruct success and fallback paths.
- ExecutionPlan output adds Candidate, Opportunity, Resolver, Trace, chain,
  Rule Validation, veto, Final timestamp, and Final marker fields.
- Review output adds Final, Candidate, and Trace provenance.
- Asset Pool rows expose their persisted `assetId`; pool size remains unbounded
  by the six Home slots.
- `GET /api/dashboard/home` focus assets are the current Opportunity-ranked Top
  6 projection, not the first six Asset Pool rows. Each projected asset exposes
  `assetId`, `analysisId`, `opportunityId`, `opportunityState`,
  `opportunityScore`, `planMode`, `aiDecisionResult`, `dataQualityScore`, and
  `rankingReason`.
- ranking reads `planMode` only from a Rule Validation PASS Final Plan; a
  Candidate, legacy, or blocked plan cannot be projected as the Final mode.
- current decision-chain role output is serialized into the existing Three-AI
  payload for the selected ranked asset.

No new endpoint was added for ranking. The existing Dashboard Home endpoint is
the projection boundary.

## Explicit Non-Changes

- No order API.
- No automatic open, close, reduce, add, reverse, or position conversion API.
- No PositionMonitor API or schema rewrite.
- No new migration or schema change for dynamic ranking; it is a read model over
  V11 Asset Pool, Analysis, Score, Decision, Plan, and Opportunity data.
- No Figma or Mobile change.

`SCHEMA_CHANGED = YES`

`API_CHANGED = YES`
