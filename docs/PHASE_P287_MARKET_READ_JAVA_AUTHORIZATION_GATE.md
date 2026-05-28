# P287 Market-Read Java Authorization Gate

P287 defines whether P288 may implement market-read Java.

## Decision

P287 authorizes P288 to implement `MarketReadRequestDTO` Java skeleton only if it is pure data only.

This authorization is limited to a DTO skeleton and targeted DTO tests. It does not authorize service Java, runtime reader Java, provider Java, scheduler Java, controller Java, mapper Java, repository Java, dashboard Java, or production workflow Java.

If P288 needs anything beyond a pure-data DTO and targeted tests, P288 must be replaced by another docs-only gate.

## Allowed Future Java

P288 may implement:

- a plain `MarketReadRequestDTO`;
- only the frozen P287 fields;
- safe defaults for `reviewOnly=true` and `notTradeInstruction=true`;
- fail-closed stale and missing-data policy defaults;
- source identity and Watchlist Pool proof fields;
- blocking reasons and risk blockers;
- targeted DTO tests for defaults, source boundary, and fail-closed behavior.

## Required Future DTO Fields

The future DTO skeleton may contain only:

- `symbol`;
- `requestId`;
- `sourceContractId`;
- `watchlistPoolProof`;
- `watchlistConfigVersion`;
- `requestedScanReason`;
- `requestedTimeframes`;
- `scanTimestamp`;
- `dataAvailabilityExpectation`;
- `stalePolicy`;
- `missingDataPolicy`;
- `riskBlockers`;
- `reviewOnly`;
- `notTradeInstruction`;
- `guardValidationStatus`;
- `blockingReasons`.

## Still Forbidden

P288 must not add:

- Spring annotations;
- service wiring;
- controller / endpoint / API;
- scheduler;
- mapper / repository / DB read/write / migration;
- schema / config;
- dashboard;
- `MarketQuoteClient` dependency or wiring;
- `BinanceMarketQuoteClient` dependency or wiring;
- provider dependency;
- runtime/live/external data read;
- scan output creation;
- real scan loop;
- production ScanScore computation;
- Candidate production workflow;
- Candidate Attention production workflow;
- Promote To Home runtime logic;
- Opportunity Push execution;
- external channel behavior / provider credentials / live provider call / message rendering / sending;
- Telegram / email / webhook / app notification / local notification;
- Readiness / point generation / entry-stop-TP-RR / order / execution / auto-trading.

## Boundary Reminders

- Only a GuardValidator-approved `RealScanInputContractDTO` can source the request.
- Missing proof is blocked.
- Missing source contract is blocked.
- Missing timestamp/timeframe is blocked.
- Display Slots / 默认六币 cannot be scan universe or batch universe.
- Watchlist Pool remains the scan candidate boundary.
- Risk Action Guard must remain before delivery / Push / Readiness.
