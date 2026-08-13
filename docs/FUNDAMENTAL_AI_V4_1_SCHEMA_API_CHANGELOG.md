# Fundamental AI v4.1 Schema And API Changelog

Status: `FINAL_CONTRACT_ALIGNMENT_COMPLETE_PENDING_REAUDIT`

## Schema

### V11 decision-chain foundation

Migration:
`src/main/resources/db/migration/V11__fundamental_ai_v4_1_decision_chain.sql`

V11 introduced the authorized Asset Pool item, Opportunity transition,
ExecutionPlanCandidate and ConflictResolverResult stores and extended the
existing AssetState, Final Plan, UserPosition, Review and AI call-log owners.
It preserved legacy rows as non-Final/non-executable evidence.

### V12 final-contract alignment

Migration:
`src/main/resources/db/migration/V12__fundamental_ai_v4_1_final_contract_alignment.sql`

New canonical table:

- `tm_asset`: stable Asset identity used by the existing Pool, Analysis, state
  and plan owners. It is not a duplicate Opportunity or Analysis stack.

Extended contracts:

| Owner | V12 contract |
|---|---|
| `tm_asset_pool_item` | Asset identity, watch state, version and extension payload |
| `tm_analysis_run` | system/user owner, Asset identity and preview flag |
| `tm_hot_reset_event` | owner, Asset and rule-version provenance |
| `tm_evidence_item` | current value, baseline change, observation time and freshness |
| `tm_decision_result` | rule/final Bias, rule/candidate/final mode, rule execution permission and adjustment reasons |
| `tm_asset_state` | owner/Asset/Pool identity, ranking inputs, versioned timestamps and exact owner-scoped identity |
| `tm_opportunity_state_transition` | owner, Asset and rule-version audit fields |
| `tm_execution_plan_candidate` | exact five Plan Modes, eight Bias values, complete plan body, source-backed numeric fields, risk/time/validity/reference lineage and version |
| `tm_conflict_resolver_result` | Bias before/after, adjustments, recovery, complete rule/data/confused/account-risk inputs and exact mode constraints |
| `tm_execution_plan` | complete Final body, source lineage, resolver/validation ownership, account risk, feasibility trust gate, time contract and strict Final boundary |
| `tm_account_risk_snapshot` | owner, candidate exposure dimensions, aggregate assessment, source status and freshness |
| `tm_ai_call_log` | Opportunity identity, cache-hit and observation time |
| `tm_review_result` | Opportunity, Resolver and Validation identity, review type/outcome, deviation, AI/rule assessment, feedback and metrics |
| `tm_user_position` | exact `MANUAL_INDEPENDENT` versus `SYSTEM_PLAN_POSITION` source contract |

Historical safety:

- unknown legacy Bias/Plan Mode values fail closed to `WAIT`/`BLOCKED`;
- V11 Candidate modes are converted only after the legacy constraint is
  removed;
- all historical AssetState rows receive conservative timestamps even when no
  Asset identity can be verified;
- unmatched Asset and Pool references remain null rather than being invented;
- all previously marked Finals are downgraded until the complete V12 Final
  contract is independently satisfied;
- legacy manual positions become `MANUAL_INDEPENDENT`;
- resolver/rule veto audit text is not truncated.

`src/main/resources/schema.sql` mirrors the current contract for local H2 and
integration tests.

## API Changes

### Asset Pool

Base: `/api/asset-pool`

| Method | Path | Contract |
|---|---|---|
| GET | `/api/asset-pool` | effective system + user Pool |
| GET | `/api/asset-pool/search` | full-market/fuzzy catalog search |
| POST | `/api/asset-pool/search/{symbol}/analysis-preview` | on-demand Analysis + Three-AI preview, no persistent Opportunity/Candidate/Final |
| POST | `/api/asset-pool` | explicit add |
| POST | `/api/asset-pool/batch-add` | explicit batch add |
| DELETE | `/api/asset-pool/{symbol}` | explicit remove while history remains |
| POST | `/api/asset-pool/batch-remove` | explicit batch remove |
| POST | `/api/asset-pool/restore-default` | restore default membership |
| POST | `/api/asset-pool/scan` | explicit Pool scan |
| POST | `/api/asset-pool/batch-scan` | explicit batch Pool scan |

### Opportunity and audit reads

- `GET /api/opportunities`: filtered Opportunity query.
- `GET /api/opportunities/top`: current ranked Opportunity projection.
- `GET /api/opportunities/{opportunityId}`: exact Opportunity.
- `GET /api/opportunities/{opportunityId}/history`: transition history.
- `GET /api/ai/audit-chain`: ordered Analysis-to-Review aggregate by safe
  Analysis/Candidate/Trace filters.
- `GET /api/ai/call-logs`: complete GPT/Gemini/Grok terminal trace query.

### Existing response extensions

- Dashboard Home focus assets now come only from Opportunity ranking and expose
  Asset, Opportunity, Analysis, score, Bias, mode, confidence, risk, freshness,
  AI result and ranking reason.
- Dashboard Three-AI output carries one workspace with three role payloads,
  exact role/collection states and consistency metadata.
- Plan APIs return only a Rule-validated Final as an execution plan; Candidate
  remains separate.
- Position output preserves independent monitor fields and fail-closed nulls.
- Review output exposes Candidate/Resolver/Validation/Final provenance and
  responsibility metrics.
- Common response envelope is `code`, `msg`, `request_id`, `server_time`.

## Explicit Non-Changes

- No Figma or Mobile change.
- No order or exchange execution endpoint.
- No automatic open, close, add, reduce, reverse or position conversion.
- No rewrite of the P2 Position Monitoring owner.
- No Candidate endpoint can masquerade as Final.

`SCHEMA_CHANGED = YES`

`API_CHANGED = YES`
