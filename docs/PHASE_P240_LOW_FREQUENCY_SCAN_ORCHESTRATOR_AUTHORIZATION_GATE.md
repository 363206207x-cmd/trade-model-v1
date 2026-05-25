# PHASE P240 - Low-Frequency Scan Orchestrator Authorization Gate

## Stage Position

P240 only defines the future authorization gate for a Low-Frequency Scan Orchestrator / scan loop.

P240 does not write Java.

P240 does not connect `MarketQuoteClient`.

P240 does not enable scheduler behavior.

P240 does not create real scanning.

P240 does not generate `ScanScore`.

## Future P241 Candidate Scope

Future P241 may consider:

- docs-only Low-Frequency Scan Orchestrator plan
- docs-only scan loop boundary audit
- disabled-by-default orchestrator skeleton authorization
- a future Java path only after a separate B/C authorization gate

Future orchestrator work may only compose:

- `WatchlistRuntimeSourceService`
- `WatchlistScanResultAssembler`
- `WatchlistScanGuardValidator`

Future orchestrator output may only be review-only / blocked / incomplete scan result output.

Future orchestrator work must not read market data.

Future orchestrator work must not compute `ScanScore`.

Future orchestrator work must not trigger Candidate Attention, Promote To Home, Push, readiness, or point generation.

## Questions Future P241 Must Answer First

Future P241 must answer:

- Is the orchestrator disabled by default? It must be.
- Is scheduler invocation allowed? Default answer is no; it requires a separate authorization gate.
- Is `MarketQuoteClient` allowed? No.
- Is scan loop allowed? No real scan loop; only planning is allowed.
- Is multi-symbol batch allowed? Default answer is no; start with single symbol / single request.
- Is `ScanScore` allowed? No.
- Are Candidate Attention / Promote To Home / Push allowed? No.
- Are entry / stop / TP / RR allowed? No.
- Is API / dashboard output allowed? No.
- Is the universe still Watchlist Pool only? Yes, it must be.

## Future P241 Still Forbidden

Future P241 still forbids:

- connecting `MarketQuoteClient`
- connecting `BinanceMarketQuoteClient`
- connecting scheduler
- creating a real scan loop
- creating real scan
- computing `ScanScore`
- creating Candidate Attention
- creating Promote To Home
- creating Opportunity Push
- generating entry / stop / TP / RR
- upgrading readiness
- creating trading action

## Conclusion

P241 should not directly write scan orchestrator Java.

P241 should first do a Low-Frequency Scan Orchestrator plan / disabled-by-default scan loop boundary audit.
