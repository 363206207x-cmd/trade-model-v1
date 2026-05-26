# P277 Runtime Market Read Still Blocked

P277 does not authorize runtime market reads.

The real scan contract audit and market-read boundary plan are docs-only.

## Still Blocked

The following remain blocked:

- `MarketQuoteClient` wiring
- `BinanceMarketQuoteClient` wiring
- runtime data reads
- live data reads
- external data reads
- real scan loop
- scheduler/API/dashboard wiring
- production ScanScore computation
- Candidate production workflow
- Opportunity Push execution
- external channel behavior
- provider credentials
- live provider calls
- message rendering
- message sending

## Future Gate Required

Any future runtime market-read work must have a separate issue and authorization gate.

That future gate must define source ownership, stale/missing data handling, raw market-read references, Watchlist Pool proof, fail-closed behavior, and Risk Action Guard preconditions.

## Safety Rules

Display Slots / 默认六币 cannot be used as a batch universe.

Watchlist Pool is the scan universe boundary.

Risk Action Guard must remain before delivery / Push / Readiness. 踩踏状态禁止机会推送。插针不等于趋势反转。强反转不等于直接反手。
