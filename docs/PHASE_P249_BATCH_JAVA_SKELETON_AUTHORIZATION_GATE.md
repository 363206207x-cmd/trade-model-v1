# PHASE P249 - Batch Java Skeleton Authorization Gate

## 1. Phase Position

P249 is the Batch Java Skeleton Authorization Gate.

P249 does not implement Java.

## 2. Future Java Scope To Consider

Future Java work may consider:

- `BatchWatchlistScanOrchestrator` interface.
- `DefaultBatchWatchlistScanOrchestrator` implementation.
- `BatchWatchlistScanResultEnvelopeDTO`, or safe reuse of existing DTOs.
- `DefaultBatchWatchlistScanOrchestratorTest`.

Future Java may only call `LowFrequencyWatchlistScanOrchestrator.scanSingleSymbol(...)`.

Future Java may only process explicit watchlist symbols.

Future Java may only output a review-only / blocked / incomplete envelope.

## 3. Future Java Must Preserve

Future Java must keep all of the following:

- Disabled-by-default.
- Watchlist Pool only.
- No Display Slots universe.
- No default-six universe.
- No arbitrary market universe.
- No scheduler trigger.
- No `MarketQuoteClient`.
- No `ScanScore`.
- No Candidate Attention.
- No Promote To Home.
- No Opportunity Push.
- No readiness.
- No entry / stop / TP / RR.
- No order / execution / auto-trading.

## 4. Future Java Prohibitions

Future Java is not allowed to:

- Share the same PR with scheduler.
- Share the same PR with `MarketQuoteClient`.
- Share the same PR with `ScanScore`.
- Create a real scan loop.
- Automatically scan a watchlist without explicit input.
- Automatically scan the default six symbols.
- Automatically scan Display Slots.
- Automatically push.
- Automatically promote to home.

## 5. Conclusion

If P250 enters Java, it can only be a minimal batch skeleton.

P250 is not market-read, not scheduler, not `ScanScore`, and not push/readiness.
