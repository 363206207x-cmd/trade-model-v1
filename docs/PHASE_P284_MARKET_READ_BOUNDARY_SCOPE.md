# P284 Market-Read Boundary Scope

P284 defines only the next-stage market-read boundary scope.

The next-stage market-read boundary scope must start with docs-only boundary audit / authorization gate, not implementation.

## Required Boundary Questions

The next docs-only gate must define:

- Whether market-read work is still only planning / audit, or whether a future Java skeleton is separately authorized.
- Which source may prove Watchlist Pool membership before any market-read candidate exists.
- Which fields of `RealScanInputContractDTO` may describe a future read request without performing it.
- Which fail-closed states apply when Watchlist Pool proof, source, timeframe, timestamp, or data availability is missing.
- How review-only and not-trade-instruction flags remain preserved after validation.
- Why Display Slots / 默认六币 cannot be scan universe or batch universe.
- Why Watchlist Pool remains the scan candidate boundary.
- Why Risk Action Guard must remain before delivery / Push / Readiness.

## Still Blocked

P284 does not authorize:

- `MarketQuoteClient` wiring.
- `BinanceMarketQuoteClient` wiring.
- Runtime data reads.
- Live data reads.
- External data reads.
- Provider credential handling.
- Live provider calls.
- Scheduler/API/dashboard wiring that reaches market reads.
- DB read/write, mapper, repository, or migration work.

P284 does not create scan output, does not create a real scan loop, and does not make any market-read call.

Future market-read Java must not be implemented until a separate authorization gate has passed.
