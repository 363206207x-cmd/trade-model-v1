# PHASE P248 - Batch Scan Java Authorization Gate

## 1. Phase Position

P248 is the Batch Scan Java Authorization Gate.

P248 does not implement Java.

P248 does not create a batch implementation.

P248 does not connect scheduler.

P248 does not connect `MarketQuoteClient`.

P248 does not create a real scan loop.

P248 does not generate `ScanScore`.

## 2. Future P249 Or Later Scope To Consider

Future P249 or a later phase may consider a batch scan Java skeleton only after a separate B/C or C authorization gate.

Future scope may consider:

- `BatchWatchlistScanOrchestrator` interface.
- `DefaultBatchWatchlistScanOrchestrator` implementation.
- `BatchWatchlistScanResultEnvelope` DTO, or a plan that reuses existing DTOs safely.

Future batch Java must only call the single-symbol orchestrator.

Future batch Java must only output a review-only / blocked / incomplete result envelope.

Future batch Java must not connect scheduler.

Future batch Java must not connect `MarketQuoteClient`.

Future batch Java must not generate `ScanScore`.

Future batch Java must not trigger Candidate Attention / Push / Readiness / point generation.

## 3. Future Batch Java Must Preserve

Future batch Java must keep all of the following:

- Watchlist Pool only.
- No Display Slots universe.
- No default-six universe.
- No arbitrary market universe.
- Non-watchlist fail-closed.
- Empty input fail-closed.
- Disabled-by-default.
- No scheduler trigger.
- No market read.
- No `ScanScore`.
- No push.
- No readiness.
- No entry / stop / TP / RR.
- No trading action.

## 4. Future Batch Java Prohibitions

Future batch Java is not allowed to:

- Automatically scan the default six symbols.
- Automatically scan Display Slots.
- Scan the full market.
- Automatically push.
- Upgrade readiness.
- Generate points.
- Create order / execution / auto-trading.
- Share the same PR with scheduler / `MarketQuoteClient` / `ScanScore`.

## 5. Conclusion

P248 does not authorize batch Java implementation.

Any future Java phase must open a separate B/C or C authorization gate before implementing batch behavior.
