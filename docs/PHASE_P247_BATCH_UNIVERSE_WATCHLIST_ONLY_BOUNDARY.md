# PHASE P247 - Batch Universe Watchlist-Only Boundary

## 1. Phase Position

This document fixes the batch universe boundary.

P247 does not implement batch.

## 2. Universe Definition

The batch universe boundary is:

- Watchlist Pool is the only batch universe source.
- Display Slots are only homepage display slots, not a batch universe.
- The default six symbols are not a batch universe.
- Open positions are not the opportunity batch universe.
- Manual position monitoring and watchlist opportunity scan are two separate logical paths.
- Assets outside the Watchlist Pool must fail closed.

## 3. Future Batch Input Rules

Future batch input must follow these rules:

- Batch request must explicitly come from Watchlist Pool.
- Empty watchlist must fail closed.
- Disabled watchlist must fail closed.
- Stale watchlist must fail closed or stay review-only.
- Duplicated symbols may be deduplicated, but deduplication must not expand the universe.
- Invalid symbols must fail closed or be skipped with a blocking reason.
- Non-watchlist symbols must not enter the result as opportunities.

## 4. Relationship With Homepage Display Slots

Display Slots may show assets.

Display Slots must not drive batch scan.

Promote To Home requires a separate future authorization gate.

The homepage default six symbols must not be interpreted as the full Watchlist Pool.

## 5. Conclusion

Batch universe must remain strictly watchlist-only.

P247 does not authorize batch implementation.
