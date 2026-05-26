# P277 Market-Read Boundary Plan

P277 defines a market-read boundary plan for future real scan work.

This is planning only. It does not perform market reads.

## Boundary Plan

Future market-read authorization must define:

- who owns market-read input
- how a symbol proves Watchlist Pool membership before any read
- how stale data is represented
- how missing data is represented
- how raw market-read references are carried for audit
- how scan timestamp and data timestamp differ
- how fail-closed state is produced when market data is unavailable
- how Risk Action Guard blockers are preserved before downstream use

## Still Not Authorized

P277 does not authorize:

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

## Future Gate

Any future market-read adapter production authorization gate must be separate from P277.

That gate may discuss production adapter authorization, but implementation still requires separate authorization if the next package is only a gate.

## Scan Universe

Market-read planning must not expand the scan universe beyond Watchlist Pool.

Display Slots / 默认六币 cannot be used as a batch universe, scan universe, or push universe.

Risk Action Guard must remain before delivery / Push / Readiness. 踩踏状态禁止机会推送。插针不等于趋势反转。强反转不等于直接反手。
