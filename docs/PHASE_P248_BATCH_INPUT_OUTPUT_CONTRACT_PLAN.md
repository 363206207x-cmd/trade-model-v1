# PHASE P248 - Batch Input Output Contract Plan

## 1. Phase Position

This document plans the batch input / output contract.

P248 does not implement Java.

P248 does not modify DTOs.

## 2. Input Boundary

Future batch input must follow these boundaries:

- Input must come from Watchlist Pool.
- Display Slots are not allowed.
- The default six symbols are not allowed.
- Arbitrary market lists are not allowed.
- Open positions are not an opportunity batch universe.
- Batch request must explicitly set `watchlistPoolOnly=true`.
- `source` must be recorded.
- `reason` / `operator` may be optional initially, but future implementation should consider making them required.

## 3. Output Boundary

Future batch output must follow these boundaries:

- Each symbol outputs `WatchlistScanResultDTO`.
- Batch output can only be review-only / blocked / incomplete.
- No score / no candidate / no push / no readiness / no point generation.
- Non-watchlist symbols must not output opportunities.
- Invalid symbols must have a blocking reason.
- Duplicate symbols must be deduplicated and recorded.

## 4. Relationship With Scheduler

Scheduler must not directly trigger batch unless a separate authorization gate allows it.

Disabled scheduler wiring does not equal batch scheduler.

Batch scheduler requires a separate PR.

## 5. Relationship With Market-Read

Batch must not directly connect `MarketQuoteClient`.

Market-read requires a separate authorization gate.

Batch input must not expand the universe because market data is available.

## 6. Conclusion

P248 only defines the contract.

Any implementation must open a separate authorization gate.
