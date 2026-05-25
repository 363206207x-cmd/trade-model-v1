# PHASE P241 - Orchestrator Java Authorization Gate

## Stage Position

P241 defines the future authorization gate for an Orchestrator Java skeleton.

P241 does not write Java.

## Future P242 Candidate Scope

Future P242 may consider:

- `LowFrequencyWatchlistScanOrchestrator` interface
- `DefaultLowFrequencyWatchlistScanOrchestrator` implementation
- `DefaultLowFrequencyWatchlistScanOrchestratorTest`

Future P242 may only:

- compose `WatchlistRuntimeSourceService`
- compose `WatchlistScanResultAssembler`
- process single symbol only
- output review-only / blocked / incomplete `WatchlistScanResultDTO`
- remain disabled by default
- avoid scheduler wiring
- avoid `MarketQuoteClient`
- avoid API / dashboard wiring

## Future P242 Must Preserve

Future P242 must preserve:

- Watchlist Pool only
- single symbol only
- no batch
- disabled by default
- no scheduler activation
- no market data
- no `ScanScore`
- no Candidate Attention
- no Promote To Home
- no Opportunity Push
- no readiness
- no entry / stop / TP / RR
- no trading action

## Future P242 Still Forbidden

Future P242 must not:

- read market data
- connect `MarketQuoteClient`
- connect `BinanceMarketQuoteClient`
- connect scheduler
- connect API
- modify dashboard
- generate `ScanScore`
- create Candidate Attention
- create Promote To Home
- create Opportunity Push
- generate entry / stop / TP / RR
- upgrade readiness
- create trading action

## Conclusion

If P242 enters Java, it can only be a disabled-by-default single-symbol Orchestrator skeleton.

P242 is not a real scan loop.

P242 is not `MarketQuoteClient` integration.

P242 is not scoring.
