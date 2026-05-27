# P285 Market-Read Java Still Blocked

P285 does not authorize market-read Java.

## Current Status

P284 merged as `cbcc34d` and was docs-only / boundary-only. P285 continues the docs-only line by auditing market-read boundaries and defining the future real scan input assembly authorization gate.

Market-read Java remains blocked in P285. No production Java, test Java, DTO, service, controller, endpoint, scheduler, mapper, repository, schema, config, dashboard, or runtime path is modified by this package.

## Future Authorization Requirements

Future market-read Java cannot begin until a separate authorization gate passes. That gate must define:

- the market-read request contract;
- the Watchlist Pool proof source;
- proof version and timestamp requirements;
- requested scan reason;
- requested timeframes;
- scan timestamp;
- stale/missing data behavior;
- data availability state;
- fail-closed validation;
- `RealScanInputContractGuardValidator` usage;
- targeted-test-only requirements;
- explicit non-goals for scan output, score, Candidate, Push, Readiness, point generation, and trading.

Future Java authorization must be separate from P285 and must be narrowly scoped.

## P285 Non-Authorization

P285 does not authorize:

- `MarketQuoteClient` wiring;
- `BinanceMarketQuoteClient` wiring;
- runtime/live/external data read;
- provider credential handling;
- live provider call;
- scan output creation;
- real scan loop;
- production ScanScore computation;
- Candidate production workflow;
- Opportunity Push execution;
- scheduler/API/dashboard wiring;
- external channel behavior;
- message rendering;
- message sending;
- Readiness upgrade;
- point generation;
- entry / stop / TP / RR generation;
- order API;
- execution API;
- auto-trading.

## Boundary Reminders

- Display Slots / 默认六币 cannot be scan universe or batch universe.
- Watchlist Pool remains the scan candidate boundary.
- Risk Action Guard must remain before delivery / Push / Readiness.
- 踩踏状态禁止机会推送。
- 插针不等于趋势反转。
- 强反转不等于直接反手。
