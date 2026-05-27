# P284 Runtime Market Read Still Blocked

P284 does not authorize runtime market reads.

P283 merged as `f26befe` and added only `RealScanInputContractGuardValidatorTestOnlyWiringTest`. P283 was test-only / targeted-test-only, and P284 closes it in docs.

## Still Blocked

The following remain blocked:

- `MarketQuoteClient` wiring.
- `BinanceMarketQuoteClient` wiring.
- Runtime data reads.
- Live data reads.
- External data reads.
- Provider credential handling.
- Live provider calls.
- Scheduler/API/dashboard wiring that reaches runtime market reads.
- DB read/write, mapper, repository, or migration work.

P284 does not create scan output, does not create a real scan loop, and does not make any market-read call.

## Boundary

The next-stage market-read boundary scope must start with docs-only boundary audit / authorization gate, not implementation.

Future market-read Java must not be implemented until a separate authorization gate has passed.

Display Slots / 默认六币 cannot be scan universe or batch universe. Watchlist Pool remains the scan candidate boundary.
