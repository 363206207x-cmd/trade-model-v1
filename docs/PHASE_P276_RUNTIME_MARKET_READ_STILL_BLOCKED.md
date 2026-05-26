# P276 Runtime Market Read Still Blocked

P276 does not authorize runtime market reads.

The return to real scan / real score / Candidate / Push preview is planning-only.

## Still Blocked

The following remain blocked:

- `MarketQuoteClient` wiring
- `BinanceMarketQuoteClient` wiring
- runtime data reads
- live data reads
- external data reads
- real scan loop
- production ScanScore computation
- Candidate production workflow
- Opportunity Push execution
- scheduler/API/dashboard wiring
- external channel behavior
- provider credentials
- live provider calls
- message rendering
- message sending

## Future Gate Required

Any future runtime market-read work must have a separate issue and authorization gate.

That future gate must define:

- market-read source ownership
- stale/missing data handling
- fail-closed behavior
- Watchlist Pool boundaries
- Risk Action Guard preconditions
- no-message and no-trade boundaries unless separately authorized

## Candidate Boundary

Display Slots / 默认六币 cannot become the batch universe.

Watchlist Pool remains the candidate boundary.

Risk Action Guard must remain before delivery. Stampede state blocks opportunity push. A wick-only/pin-bar move is not a trend reversal. Strong reversal is not direct reverse trading.
