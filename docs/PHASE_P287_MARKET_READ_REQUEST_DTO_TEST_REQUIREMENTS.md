# P287 Market-Read Request DTO Test Requirements

P287 defines targeted-test requirements for a future `MarketReadRequestDTO` skeleton.

P287 itself does not add tests. These requirements apply only if P288 implements the pure-data DTO skeleton.

## Required Future Targeted Tests

Future P288 tests must verify:

- default `reviewOnly=true`;
- default `notTradeInstruction=true`;
- stale policy fails closed by default;
- missing-data policy fails closed by default;
- missing Watchlist Pool proof is blocked;
- missing source contract is blocked;
- missing scan timestamp is blocked;
- missing requested timeframe is blocked;
- blocking reasons are preserved and can be expanded;
- risk blockers are preserved;
- source requirement is limited to GuardValidator-approved `RealScanInputContractDTO`;
- blocked or incomplete source cannot be upgraded into a valid request;
- valid-looking request remains review-only and not a trade instruction.

## Test Scope Limit

Future tests must be targeted DTO tests only.

They must not:

- start Spring context;
- add service wiring;
- add controller, endpoint, API, scheduler, mapper, repository, DB read/write, schema, config, or dashboard coverage;
- mock or call `MarketQuoteClient`;
- mock or call `BinanceMarketQuoteClient`;
- call provider dependencies;
- read runtime/live/external data;
- create scan output;
- create a real scan loop;
- compute production ScanScore;
- create Candidate production workflow;
- execute Opportunity Push;
- render or send messages;
- touch Telegram / email / webhook / app notification / local notification;
- upgrade Readiness;
- generate point, entry, stop, TP, RR, order, execution, or auto-trading behavior.

## Safety Assertions

Future tests must make the non-goals visible enough that a later PR cannot confuse the DTO with executable market-read behavior.

Risk Action Guard must remain before delivery / Push / Readiness. 踩踏状态禁止机会推送。插针不等于趋势反转。强反转不等于直接反手。
