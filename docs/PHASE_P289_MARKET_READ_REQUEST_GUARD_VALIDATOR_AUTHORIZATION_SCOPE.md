# P289 MarketReadRequestGuardValidator Authorization Scope

P289 authorizes a future P290 `MarketReadRequestGuardValidator` Java skeleton only within a pure validator boundary.

P289 does not implement that validator. P289 is docs-only and does not modify Java, tests, or DTO code.

## Allowed Future P290 Scope

Future P290 may add a `MarketReadRequestGuardValidator` Java skeleton if it only validates `MarketReadRequestDTO`.

Future P290 may return a blocked / review-only / fail-closed validation result. The result must not be interpreted as a trade instruction, a scan output, a score, a Candidate, a Push payload, a Readiness upgrade, a point-generation result, or an execution command.

Future P290 may validate only DTO contract safety, including:

- `sourceContractId` is present;
- `watchlistPoolProof` is present;
- `requestedTimeframes` are present;
- `scanTimestamp` is present;
- `stalePolicy` is present and fail-closed compatible;
- `missingDataPolicy` is present and fail-closed compatible;
- `reviewOnly` remains true or otherwise fails closed;
- `notTradeInstruction` remains true or otherwise fails closed;
- `blockingReasons` are preserved;
- `riskBlockers` are preserved.

## Required Fail-Closed Behavior

Future P290 must not upgrade a blocked or incomplete request into a market-read action.

Missing source contract, missing Watchlist Pool proof, missing requested timeframes, missing scan timestamp, missing or invalid stale policy, missing or invalid missing-data policy, false review-only state, and false not-trade-instruction state must stay blocked or impossible.

## Not Authorized In P290

Future P290 must not:

- execute market reads;
- call providers;
- wire `MarketQuoteClient`;
- wire `BinanceMarketQuoteClient`;
- read runtime/live/external data;
- create scan output;
- run a real scan loop;
- compute production ScanScore;
- create Candidate workflow;
- trigger Candidate Attention;
- trigger Promote To Home;
- execute Opportunity Push;
- upgrade Readiness;
- generate point output;
- generate entry / stop / TP / RR;
- call order API;
- call execution API;
- enable auto-trading.

## Wiring Boundary

Future P290 must not add service, controller, endpoint, API, scheduler, dashboard, mapper, repository, DB write, migration, schema, config, provider credential handling, live provider call, external channel behavior, message rendering, or message sending.

The validator must remain a local contract guard over `MarketReadRequestDTO` only.
