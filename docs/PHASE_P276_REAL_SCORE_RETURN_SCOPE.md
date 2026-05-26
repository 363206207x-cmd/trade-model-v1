# P276 Real Score Return Scope

P276 returns the next planning direction to real score, but only as a scope plan.

It does not implement production ScanScore computation.

## Planning Topics

Future real score planning may define:

- score input ownership
- numeric source ownership
- incomplete market data behavior
- fail-closed score states
- review-only score display semantics
- Risk Action Guard blockers before any candidate or push preview escalation

## Not Implementation

P276 does not authorize:

- production ScanScore computation
- runtime/live/external data reads
- `MarketQuoteClient` wiring
- `BinanceMarketQuoteClient` wiring
- real scan loop
- Candidate production workflow
- Promote To Home runtime logic
- Opportunity Push execution
- scheduler/API/dashboard wiring
- external channel behavior
- provider credentials
- live provider calls
- message rendering
- message sending
- Readiness
- point generation
- entry/stop/TP/RR
- order/execution
- auto-trading

## Review-Only Boundary

Any future score surface must remain review-only and fail-closed until a separate issue authorizes implementation.

Display Slots / 默认六币 cannot be used as the batch universe. Watchlist Pool remains the push candidate boundary. Risk Action Guard must remain before delivery. Stampede state blocks opportunity push. A wick-only/pin-bar move is not a trend reversal. Strong reversal is not direct reverse trading.
