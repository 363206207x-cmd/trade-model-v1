# P289 MarketReadRequestDTO Closure

P289 closes the P288 `MarketReadRequestDTO` Java skeleton.

P288 merged as `d837519` and completed a pure-data DTO skeleton plus a targeted DTO test. P288 did not implement real market reading and did not wire a market data client.

## P288 Completed Scope

P288 added:

- `src/main/java/org/example/trademodel/dto/marketread/MarketReadRequestDTO.java`;
- `src/test/java/org/example/trademodel/dto/marketread/MarketReadRequestDTOTest.java`;
- `docs/PHASE_P288_MARKET_READ_REQUEST_DTO_SKELETON_CLOSURE.md`.

P288 only represents future market-read request contract data. It is not a service, controller, endpoint, scheduler, mapper, repository, provider integration, scan loop, score calculator, Candidate workflow, Push executor, Readiness upgrader, point generator, or trading action.

## Frozen DTO Fields

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

No price, entry, stop, take-profit, RR, order, execution, provider credential, channel, message, readiness, score, candidate, or push field was introduced.

## Safety Defaults

The DTO keeps:

- `reviewOnly=true`;
- `notTradeInstruction=true`;
- `stalePolicy` defaulting to fail-closed;
- `missingDataPolicy` defaulting to fail-closed;
- missing `sourceContractId` represented as blocked;
- missing `watchlistPoolProof` represented as blocked;
- missing `requestedTimeframes` represented as blocked;
- missing `scanTimestamp` represented as blocked;
- `blockingReasons` preserved;
- `riskBlockers` preserved.

The DTO does not provide a setter, builder parameter, constructor parameter, or factory parameter that can turn `reviewOnly` or `notTradeInstruction` to false.

## Explicit Non-Implementation

P288 is not a real market-read implementation. It does not execute market reads, call providers, read runtime/live/external data, create scan output, run a scan loop, compute ScanScore, create Candidate workflow, execute Push, upgrade Readiness, generate points, or create entry / stop / TP / RR / order / execution / auto-trading behavior.

P288 does not connect `MarketQuoteClient` or `BinanceMarketQuoteClient`.

## P289 Closure Decision

P289 treats P288 as safely closed only as a pure-data DTO skeleton. The next Java step may be a `MarketReadRequestGuardValidator` skeleton only if it remains pure validator only and does not cross into service, runtime, market-read, score, push, readiness, point, or trading behavior.
