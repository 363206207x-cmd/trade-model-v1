# P287 Runtime Market Read Still Blocked

P287 does not authorize runtime market reads.

## Still Blocked

The following remain blocked:

- `MarketQuoteClient` wiring;
- `BinanceMarketQuoteClient` wiring;
- runtime data read;
- live data read;
- external data read;
- provider dependency;
- provider credential handling;
- live provider call;
- scheduler-triggered market read;
- API-triggered market read;
- dashboard-triggered market read;
- real scan loop.

## DTO Does Not Equal Market Read

P287 authorizes P288 only to implement a pure-data `MarketReadRequestDTO` skeleton if P288 stays inside the DTO boundary.

A DTO request is not a market-read execution. It cannot call a provider, load runtime data, create scan output, compute score, create Candidate production workflow, trigger Push, upgrade Readiness, generate points, or create trading actions.

## Fail-Closed Runtime Boundary

When source/proof/timeframe/timestamp/stale policy/missing-data policy is missing or invalid, the future DTO must remain blocked and must not attempt runtime market read fallback.

Display Slots / 默认六币 cannot be scan universe or batch universe. Watchlist Pool remains the scan candidate boundary. Risk Action Guard must remain before delivery / Push / Readiness.
