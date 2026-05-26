# P276 Real Scan Return Scope

P276 returns the next planning direction to real scan, but only as a scope plan.

It does not implement real scan behavior.

## Planning Topics

Future real scan planning may define:

- scan contract boundaries
- Watchlist Pool candidate universe
- market-read ownership boundaries
- scan output shape
- incomplete data handling
- fail-closed behavior
- Risk Action Guard preconditions before any downstream preview or delivery path

## Not Implementation

P276 does not authorize:

- `MarketQuoteClient` wiring
- `BinanceMarketQuoteClient` wiring
- runtime/live/external data reads
- real scan loop
- scheduler activation
- API/dashboard wiring
- production ScanScore computation
- Candidate production workflow
- Opportunity Push execution
- external channel behavior
- message rendering
- message sending
- Readiness
- point generation
- entry/stop/TP/RR
- order/execution
- auto-trading

## Candidate Boundary

Display Slots / 默认六币 cannot be used as the batch universe.

Watchlist Pool is the only candidate boundary for future scan and push preview planning.

Risk Action Guard must remain before any delivery or preview escalation. Stampede state blocks opportunity push. A wick-only/pin-bar move is not a trend reversal. Strong reversal is not direct reverse trading.
