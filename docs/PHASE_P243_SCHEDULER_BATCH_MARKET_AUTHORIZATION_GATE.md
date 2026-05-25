# PHASE P243 - Scheduler Batch Market Authorization Gate

## Stage Position

P243 only defines the future Scheduler / Batch / Market-read authorization gate.

P243 does not write Java.

P243 does not connect `MarketQuoteClient`.

P243 does not enable scheduler behavior.

P243 does not create batch scan.

P243 does not create a real scan loop.

P243 does not generate `ScanScore`.

## Future P244 Candidate Scope

Future P244 may consider:

- docs-only scheduler / batch / market-read scope audit
- docs-only scheduler trigger authorization gate
- docs-only batch scan boundary plan
- docs-only `MarketQuoteClient` integration plan

If future work enters Java, it needs a separate B/C or C authorization gate.

Future scheduler / batch / market-read work must not be implemented together in a single Java PR.

## Required Future Layering

Recommended future layering:

- P244 scheduler / batch / market-read scope audit
- P245 scheduler trigger authorization gate
- P246 disabled scheduler wiring skeleton
- P247 batch scan authorization gate
- P248 single-symbol to batch orchestrator skeleton
- P249 `MarketQuoteClient` read-only authorization gate
- P250 `MarketQuoteClient` adapter skeleton

The numbers are advisory, but the layers must remain separate.

No later stage may treat this docs-only gate as permission to combine scheduler, batch scan, and market read implementation.

## Scheduler Still Forbidden

Scheduler work remains blocked:

- scheduler must not call the orchestrator by default
- scheduler must not automatically scan watchlist assets
- scheduler must not trigger push
- scheduler must not trigger readiness
- scheduler must not trigger entry / stop / TP / RR
- scheduler integration must remain disabled-by-default
- scheduler integration needs a separate authorization gate

## Batch Still Forbidden

Batch work remains blocked:

- no multi-symbol batch
- no default-six batch
- Display Slots must not become batch universe
- batch universe must come from Watchlist Pool
- batch needs a separate authorization gate

## Market-Read Still Forbidden

Market-read work remains blocked:

- no `MarketQuoteClient`
- no `BinanceMarketQuoteClient`
- no runtime / live / external data read
- no real market data fetch
- market read needs a separate authorization gate

## Conclusion

P244 should not directly write scheduler Java.

P244 should not directly write batch Java.

P244 should not directly connect `MarketQuoteClient`.

P244 should first do scheduler / batch / market-read scope audit or an authorization gate.
