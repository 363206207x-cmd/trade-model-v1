# PHASE P223 Adapter Interface Skeleton Plan

## 1. Phase Position

This document only plans a future adapter interface skeleton.

P223 does not implement Java.

## 2. Suggested Future Files

The following are only P224 plan candidates:

- `src/main/java/org/example/trademodel/service/watchlistsource/ProductionRuntimeSourceReadAdapter.java`
- `src/main/java/org/example/trademodel/service/watchlistsource/WatchlistPoolRuntimeSourceReadAdapter.java`
- `src/main/java/org/example/trademodel/dto/watchlistsource/RuntimeSourceReadRequestDTO.java`
- `src/main/java/org/example/trademodel/dto/watchlistsource/RuntimeSourceReadResultDTO.java`
- `src/test/java/org/example/trademodel/service/watchlistsource/ProductionRuntimeSourceReadAdapterTest.java`

## 3. Future Interface May Only Do

Future interface skeleton work may only:

- declare method signatures.
- express input / output contracts.
- return `WatchlistRuntimeSourceDTO` or a safe result.
- avoid DB / `MarketQuoteClient` / scheduler fields.
- avoid read logic implementation.
- avoid Spring bean wiring.
- avoid mapper / controller / API creation.

## 4. Future Tests Must Prove

Future tests must prove:

- interfaces do not implement reads.
- default stub / no-op returns `INCOMPLETE` / `SOURCE_UNAVAILABLE`.
- non-watchlist input fails closed.
- no push.
- no readiness.
- no entry / stop / TP / RR.
- no trading action.
- no `MarketQuoteClient`, Mapper, Controller, or Scheduler fields.

## 5. Conclusion

If P224 is created, it may only be interface skeleton / DTO / tests.

Real adapter implementation must continue to wait for a higher-risk authorization gate.
