# Fundamental AI v4.1 Schema And API Changelog

Status: `IMPLEMENTATION_CANDIDATE_COMPLETE_PENDING_AUDIT`

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
| `tm_asset_state` | stable Opportunity identity, state-entry/cooling metadata, last transition reason/source/analysis, exact eight-state constraint |
| `tm_execution_plan` | Candidate/Opportunity/Resolver/Trace links, chain status, Rule Validation status/veto, Final timestamp and Final marker |
| `tm_user_position` | optional `final_plan_id` foreign key; no automatic position creation |
| `tm_review_result` | Final/Candidate/Trace links |
| `tm_ai_call_log` | decision-chain contract type, Candidate link, output payload, explicit no-Final authority |

Safety and compatibility:

- six system default Asset Pool entries are seeded idempotently;
- legacy unknown AssetState values are normalized to `OBSERVING` before the
  exact-state constraint is applied;
- existing ExecutionPlan rows remain `LEGACY` and are not presented as newly
  validated Final plans;
- database checks prevent Candidate authority escalation, invalid Final rows,
  and AI role authority escalation;
- foreign keys preserve Candidate -> Resolver -> Final and optional manual
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

- UserPosition create/read accepts and returns optional `finalPlanId`; the
  reference is accepted only for a Rule Validation PASS Final of the same
  symbol. UserPosition creation remains explicit and manual.
- ExecutionPlan output adds Candidate, Opportunity, Resolver, Trace, chain,
  Rule Validation, veto, Final timestamp, and Final marker fields.
- Review output adds Final, Candidate, and Trace provenance.
- Dashboard Home focus assets are projected from Asset Pool, and current
  decision-chain role output is serialized into the existing Three-AI payload.

## Explicit Non-Changes

- No order API.
- No automatic open, close, reduce, add, reverse, or position conversion API.
- No PositionMonitor API or schema rewrite.
- No Figma or Mobile change.

`SCHEMA_CHANGED = YES`

`API_CHANGED = YES`
