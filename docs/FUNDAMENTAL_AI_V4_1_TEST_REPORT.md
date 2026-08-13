# Fundamental AI v4.1 Test Report

Status: `FINAL_CONTRACT_REGRESSION_PASS`

## Full Maven Validation

Command: `./mvnw test -q`

Regular isolated result:

- tests: `4497`
- passed: `4483`
- failures: `0`
- errors: `0`
- skipped: `14`
- suites: `408`

The regular sandbox run conditionally skipped the Docker-backed PostgreSQL
migration test. That exact test was then run against a fresh controlled
PostgreSQL instance and replaced one skip with one pass.

Final combined Surefire report set:

- tests: `4497`
- passed: `4484`
- failures: `0`
- errors: `0`
- skipped: `13`
- suites: `408`

## PostgreSQL V12 Migration

Test: `PostgreSqlFlywayMigrationSmokeTest`

Controlled target:

- PostgreSQL `16.14` in a disposable local container;
- fixed non-production database `trade_model_v1_test`;
- migrations validated: `12`;
- path: empty -> V8 historical fixture -> V9/V10/V11 fixture -> V12;
- result: tests `1`, passed `1`, failures `0`, errors `0`, skipped `0`;
- container cleanup: `PASS`.

The real run verified historical UserPosition/Monitor migrations, V11 decision
chain data, V12 Asset ownership, exact Market Bias and Plan Mode constraints,
Candidate/Resolver/Final audit text, dynamic ranking reads and Flyway history.

It also found and drove correction of two PostgreSQL-only migration defects:

1. unmatched historical AssetState rows had null V12 timestamps;
2. Candidate Plan Mode values were converted before the legacy constraint was
   removed.

The final clean rerun passed after both fixes.

## Final Contract Coverage

| Requirement | Result | Principal suites |
|---|---|---|
| Asset Pool search/fuzzy/add/remove/restore/batch/scan | PASS | `PersistentAssetPoolServiceTest` and controller tests |
| Asset Pool is the sole persistent Opportunity source | PASS | `DecisionChainSourceGateTest`, `AssetPoolBackedUniverseSourceTest` |
| Search preview isolation and on-demand Three-AI | PASS | pool/controller and decision-chain tests |
| Dynamic eligible/fresh/configured Top 6 | PASS | `OpportunityPriorityRankingServiceImplTest`, `DashboardHomeServiceImplTest` |
| Eight Market Bias values | PASS | `MarketBiasPolicyTest`, validator and migration tests |
| Eight Opportunity states | PASS | `AssetStateServiceImplTest` |
| Five Plan Modes | PASS | resolver, validator, ranking and migration tests |
| One canonical state writer, audit, debounce, cooling, precedence | PASS | state service and mapper integration tests |
| GPT/Gemini/Grok authority isolation | PASS | AI contract and orchestrator tests |
| Complete structured role semantics | PASS | schema, codec, orchestrator and Dashboard tests |
| Role and collection state separation | PASS | AI contract/codec tests |
| Evidence/source anti-hallucination | PASS | orchestrator provenance/state tests |
| Success/failure/timeout/fallback/cache trace | PASS | orchestrator and call-log tests |
| Candidate/Final isolation | PASS | persistence and controller source-gate tests |
| Conflict Level 1-4 and opportunity preservation | PASS | conflict resolver and decision-chain tests |
| Rule Validation and numeric source gate | PASS | rule validator tests |
| Push Recheck is not trading permission | PASS | push snapshot/message tests |
| UserPosition is separate from Plan | PASS | lifecycle, controller and ownership tests |
| Existing P2 Position Monitoring preserved | PASS | monitor, risk and Dashboard suites |
| Review responsibility chain and metrics | PASS | review policy/ownership/metrics tests |
| Common API envelope | PASS | `ApiResponseContractTest` |
| Zero automatic trading capability | PASS | safety tests and static scan |

## Focused Contract Suites

The final targeted contract run covered AI schema/codec/orchestration,
Candidate/Resolver/Validation/Final, audit aggregation, Dynamic Top 6, state
machine, Asset Pool, Review, Position Monitoring and Dashboard contracts and
completed with exit code `0`.

## Governance Gates

- `bash scripts/product-source-gate.sh`: `PASS`.
- `bash scripts/check-workflow-contract.sh`: `PASS`,
  `WORKFLOW_CONTRACT_OK`.
- `git diff --check`: `PASS`.
- `bash scripts/v1-state.sh`: diagnostic completed; the dirty candidate branch
  is correctly not treated as merged-main completion.

## Environment Notes

- External AI live smoke remained disabled; no credentials were read and no
  provider success was fabricated.
- Real historical replay without a provider fixture remained explicitly
  blocked rather than simulated.
- Controlled PostgreSQL tests use a fixed target and must run in isolated
  database instances; sharing that target among unrelated controlled suites is
  intentionally not used as a valid combined test mode.

`TEST_STATUS = PASS`

`READY_FOR_INDEPENDENT_FINAL_REAUDIT`
