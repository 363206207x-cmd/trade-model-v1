# P284 Market-Read Java Authorization Gate

P284 does not authorize market-read Java.

Future market-read Java must not be implemented until a separate authorization gate has passed.

## Gate Conditions For Future Java

Before any future market-read Java is allowed, a separate authorization gate must define:

- The exact Java surface to add.
- Whether the package is docs-only, test-only, skeleton-only, or production-capable.
- Whether `MarketQuoteClient` / `BinanceMarketQuoteClient` are still blocked.
- Whether any runtime/live/external data read is still blocked.
- The fail-closed behavior for missing Watchlist Pool proof.
- The review-only and not-trade-instruction invariants.
- The no-score, no-Candidate, no-Push, no-Readiness, no-point, no-trading boundaries.

## P284 Explicit Non-Authorization

P284 does not add Java, tests, DTOs, Spring annotations, controller, endpoint, API, service, scheduler, mapper, repository, DB write, migration, dashboard, schema, config, market-read client wiring, provider credentials, live provider calls, message rendering, or message sending.

Future recommended next package after P284 should be P285 Market-Read Boundary Audit and Real Scan Input Assembly Authorization Gate, docs-only only.
