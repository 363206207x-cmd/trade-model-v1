# P286 Market-Read Request DTO Authorization Gate

P286 defines the future DTO skeleton authorization gate. It does not authorize Java in this package.

## Future DTO Skeleton Conditions

Before a future `MarketReadRequestDTO` skeleton can be implemented, a later authorization gate must confirm:

- the DTO is pure data and has no runtime reader;
- the DTO has no Spring annotation, service wiring, controller, endpoint, scheduler, mapper, repository, DB read/write, schema, config, or dashboard dependency;
- the DTO has no `MarketQuoteClient` / `BinanceMarketQuoteClient` dependency;
- the DTO carries `reviewOnly=true` and `notTradeInstruction=true`;
- the DTO can only originate from a GuardValidator-approved `RealScanInputContractDTO`;
- missing field/source/proof/timeframe/timestamp/stale policy/missing-data policy fails closed;
- targeted tests cover defaults, source boundary, and fail-closed behavior if Java is authorized.

## Future DTO Non-Goals

The future DTO skeleton must not:

- read runtime/live/external data;
- create scan output;
- create real scan loop;
- compute production ScanScore;
- create Candidate production workflow;
- trigger Opportunity Push execution;
- wire scheduler/API/dashboard;
- implement external channel behavior;
- handle provider credentials;
- make live provider calls;
- render or send messages;
- upgrade Readiness;
- generate point generation;
- generate entry-stop-TP-RR;
- connect order or execution APIs;
- enable auto-trading.

## P286 Decision

P286 keeps Java blocked. Future recommended next package after P286 should be P287 Market-Read Request DTO Authorization Gate or DTO Skeleton, depending on whether another docs-only gate is needed.

Risk Action Guard must remain before delivery / Push / Readiness. Watchlist Pool remains the scan candidate boundary. Display Slots / 默认六币 cannot be scan universe or batch universe.
