# P291 MarketReadRequest Test-Only Wiring Authorization Scope

P291 authorizes a future P292 test-only wiring package.

Future P292 may only wire `MarketReadRequestDTO` into `MarketReadRequestGuardValidator` inside tests.

## Allowed Future P292 Scope

Future P292 may add a test-only assembler, fixture, or wiring helper.

The helper may only:

- build or carry `MarketReadRequestDTO` fixtures for tests;
- pass `MarketReadRequestDTO` into `MarketReadRequestGuardValidator`;
- verify fail-closed behavior;
- verify review-only behavior;
- verify blocked behavior;
- preserve `blockingReasons`;
- preserve `riskBlockers`.

Future P292 must keep outputs review-only, not-trade-instruction, fail-closed, and manual-review-required.

## Explicit Limits

Future P292 must not use Spring context.

Future P292 must not introduce production service wiring.

Future P292 must not add controller, scheduler, API, endpoint, mapper, repository, DB write, migration, dashboard, schema, or config changes.

Future P292 must not read runtime/live/external data.

Future P292 must not connect `MarketQuoteClient` or `BinanceMarketQuoteClient`.

Future P292 must not call a provider, use provider credentials, or make live provider calls.

Future P292 must not create scan output, a real scan loop, production ScanScore, Candidate, Push, Readiness, point generation, entry / stop / TP / RR, order, execution, or auto-trading behavior.

## Production Boundary

Test-only wiring is not production wiring.

Passing a DTO through a validator in a targeted test does not authorize runtime market reads, production assembly, scheduler activation, API exposure, dashboard-triggered market reads, scan output creation, score computation, Candidate production workflow, Push execution, Readiness upgrade, point generation, or trading execution.

## Boundary Rules

Watchlist Pool remains the scan candidate boundary.

Display Slots / 默认六币 are not the scan universe, not the batch universe, not the push universe, and not the Watchlist Pool proof source.

Risk Action Guard must remain before delivery / Push / Readiness.

踩踏状态禁止机会推送。

插针不等于趋势反转。

强反转不等于直接反手。
