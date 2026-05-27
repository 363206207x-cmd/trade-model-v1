# P286 Market-Read Java Still Blocked

P286 does not authorize market-read Java.

## Current Status

P285 merged as `c64c8b8`.

P285 was docs-only / boundary-only and kept market-read Java blocked. P286 continues to keep Java blocked while defining the future `MarketReadRequestDTO` contract gate.

## Still Blocked

P286 does not authorize:

- production Java changes;
- test Java changes;
- DTO files;
- service wiring;
- controller / endpoint / API;
- scheduler;
- mapper / repository / DB read/write / migration;
- schema / config;
- dashboard;
- `MarketQuoteClient`;
- `BinanceMarketQuoteClient`;
- runtime/live/external data reads.

## Future Java Requirements

Future Java cannot begin unless a separate authorization gate explicitly allows it and defines:

- exact DTO skeleton scope;
- fields and safety defaults;
- GuardValidator-approved source contract requirement;
- Watchlist Pool proof handling;
- stale policy and missing-data policy;
- fail-closed behavior;
- targeted-test-only coverage;
- no scan output / score / Candidate / Push / Readiness / point / trading behavior.

Until that gate passes, market-read Java remains blocked.

## Safety Boundaries

- No scan output creation.
- No real scan loop.
- No production ScanScore computation.
- No Candidate production workflow.
- No Opportunity Push execution.
- No external channel behavior / provider credentials / live provider call / message rendering / sending.
- No Readiness / point generation / entry-stop-TP-RR / order / execution / auto-trading.
- Display Slots / 默认六币 cannot be scan universe or batch universe.
- Watchlist Pool remains the scan candidate boundary.
- Risk Action Guard must remain before delivery / Push / Readiness.
- 踩踏状态禁止机会推送。
- 插针不等于趋势反转。
- 强反转不等于直接反手。
