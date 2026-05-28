# P289 Market-Read Runtime Still Blocked

P289 keeps market-read runtime behavior blocked.

P289 is docs-only and does not implement Java, tests, DTO changes, runtime reads, provider calls, or market-read execution.

## Still Blocked After P289

The following remain blocked:

- `MarketQuoteClient`;
- `BinanceMarketQuoteClient`;
- runtime data read;
- live data read;
- external data read;
- provider credentials;
- live provider calls;
- scan output creation;
- real scan loop;
- scheduler-triggered market read;
- API-triggered market read;
- dashboard-triggered market read.

## No Client Wiring

P289 does not authorize any `MarketQuoteClient` or `BinanceMarketQuoteClient` wiring.

Future `MarketReadRequestGuardValidator` authorization, if used in P290, must stay validator-only and must not become a market client adapter, provider selector, source reader, scan runner, scheduler trigger, API endpoint, or dashboard-triggered market-read path.

## Source Boundary

Watchlist Pool remains the scan candidate boundary.

Display Slots / 默认六币 are not the scan universe, not the batch universe, not the push universe, and not the Watchlist Pool proof source.

Risk Action Guard must remain before delivery / Push / Readiness.
