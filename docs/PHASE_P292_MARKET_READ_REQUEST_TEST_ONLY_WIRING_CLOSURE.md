# P292 MarketReadRequest Test-Only Wiring Closure

P292 completes the DTO -> GuardValidator test-only wiring slice for `MarketReadRequest`.

## Completed

P292 adds:

- `src/test/java/org/example/trademodel/service/watchlistscan/MarketReadRequestTestOnlyWiring.java`;
- `src/test/java/org/example/trademodel/service/watchlistscan/MarketReadRequestTestOnlyWiringTest.java`.

The helper exists under test scope only. It accepts `MarketReadRequestDTO`, calls `MarketReadRequestGuardValidator`, and returns `MarketReadRequestGuardValidationResult`.

## Safety Semantics

The output remains:

- fail-closed;
- review-only;
- manual-review required;
- not a trade instruction.

The wiring preserves:

- `blockingReasons`;
- `riskBlockers`;
- blocked status for missing source contract id;
- blocked status for missing Watchlist Pool proof;
- blocked status for missing requested timeframes;
- blocked status for missing scan timestamp;
- blocked status for invalid stale policy;
- blocked status for invalid missing-data policy.

## Capability Movement

`MarketReadRequest test-only wiring` moves from `0 NOT_STARTED` to `4 TEST_ONLY_WIRING`.

`MarketReadRequest assembler` remains test-only only. P292 does not create a production assembler.

## Still Not Authorized

P292 has no provider, runtime, production wiring, Spring context, controller, scheduler, API, mapper, repository, DB write, schema change, config change, dashboard change, `MarketQuoteClient`, or `BinanceMarketQuoteClient`.

P292 does not create scan output, score, Candidate, Push, Readiness, point generation, entry / stop / TP / RR, order behavior, execution behavior, or auto-trading.

## Next Step

The next business-chain step should be review-only MarketRead output / scan output.

Do not repeat a closure-only or broad authorization-gate loop unless a new risk boundary appears.
