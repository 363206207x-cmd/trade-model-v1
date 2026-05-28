# P288 MarketReadRequestDTO Java Skeleton Closure

P288 implements the `MarketReadRequestDTO` Java DTO skeleton authorized by P287.

## Scope

P288 only implements a pure-data `MarketReadRequestDTO` skeleton and a targeted DTO test.

P288 is not a real market-read implementation. It does not execute market reads, call providers, create scan output, run a scan loop, compute score, create Candidate workflow, execute Push, upgrade Readiness, generate points, or create trading actions.

## DTO Field Freeze

`MarketReadRequestDTO` contains only the P287 frozen fields:

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

No price, entry, stop, take-profit, RR, order, execution, provider credential, message, channel, readiness, score, candidate, or push field is introduced.

## Safety Defaults

The DTO keeps:

- `reviewOnly=true`;
- `notTradeInstruction=true`;
- manual review required by review-only semantics;
- stale policy defaulting to fail-closed;
- missing-data policy defaulting to fail-closed;
- missing `sourceContractId` represented as blocked;
- missing `watchlistPoolProof` represented as blocked;
- missing `requestedTimeframes` represented as blocked;
- missing `scanTimestamp` represented as blocked;
- `blockingReasons` preserved;
- `riskBlockers` preserved.

The DTO does not provide a setter, builder parameter, constructor parameter, or factory parameter that can turn `reviewOnly` or `notTradeInstruction` to false.

## Not Authorized

P288 does not authorize:

- `MarketQuoteClient` wiring;
- `BinanceMarketQuoteClient` wiring;
- runtime/live/external data reads;
- scan output creation;
- real scan loop;
- production ScanScore computation;
- Candidate production workflow;
- Candidate Attention production workflow;
- Promote To Home runtime logic;
- Opportunity Push execution;
- scheduler/API/dashboard wiring;
- external channel behavior;
- Telegram / email / webhook / app notification / local notification;
- provider credential handling;
- live provider calls;
- message rendering;
- message sending;
- Readiness upgrade;
- point generation;
- entry / stop / TP / RR generation;
- order API;
- execution API;
- auto-trading.

## Boundary Rules

Watchlist Pool is the scan candidate boundary.

Display Slots / 默认六币 are not the scan universe, not the push universe, and not the Watchlist Pool proof source.

Risk Action Guard must remain before delivery / Push / Readiness.

踩踏状态禁止机会推送。

插针不等于趋势反转。

强反转不等于直接反手。

## Validation

P288 targeted tests cover DTO defaults, frozen fields, preserved `blockingReasons`, preserved `riskBlockers`, preserved `requestedTimeframes`, preserved source contract id, preserved Watchlist Pool proof, fail-closed stale/missing-data policies, and absence of forbidden executable/trading/provider fields by reflection.
