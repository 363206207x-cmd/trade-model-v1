# P276 Readiness Point Generation Still Blocked

P276 does not authorize Readiness, point generation, or trading path behavior.

This remains true even though P276 redirects planning toward real scan / real score / Candidate / Push preview.

## Still Blocked

The following remain blocked:

- ExecutionPlan Readiness upgrade
- point generation
- real entry
- real stop
- real take profit
- real risk/reward
- order API
- execution API
- auto-trading

P276 also does not authorize:

- production ScanScore computation
- Candidate production workflow
- Opportunity Push execution
- external channel behavior
- provider credential handling
- live provider calls
- message rendering
- message sending
- scheduler/API/dashboard wiring
- runtime/live/external data reads
- `MarketQuoteClient` wiring
- `BinanceMarketQuoteClient` wiring
- real scan loop
- Promote To Home runtime logic

## Required Future Gate

Any future Readiness or point-generation work must have a separate issue and authorization gate.

That future gate must explicitly define data ownership, numeric source ownership, Risk Action Guard preconditions, manual review requirements, not-trade-instruction language, and no auto-trading boundaries.

## Safety Rules

Display Slots / 默认六币 cannot become the batch universe or default push universe.

Watchlist Pool remains the push candidate boundary.

Risk Action Guard must remain before any delivery path.

Stampede state blocks opportunity push. A wick-only/pin-bar move is not a trend reversal. Strong reversal is not direct reverse trading.
