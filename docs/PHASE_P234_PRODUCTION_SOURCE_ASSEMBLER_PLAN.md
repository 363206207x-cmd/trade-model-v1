# P234 Production Source Assembler Plan

## 1. Phase Position

This document only plans a future Production Source Assembler.

P234 does not implement Java.

## 2. Future Assembler Responsibility

The future Production Source Assembler may only assemble DB Watchlist Pool read results into safe runtime source output.

The future assembler may only handle `WatchlistRuntimeSourceDTO` and `RuntimeSourceReadResultDTO`.

The future assembler may only preserve review-only and fail-closed semantics.

The future assembler may only perform source-level assembly, not scan-result assembly.

## 3. Out Of Scope

Production Source Assembler must not generate `WatchlistScanResultDTO`.

Production Source Assembler must not generate `ScanScore`.

Production Source Assembler must not connect `MarketQuoteClient`.

Production Source Assembler must not connect scheduler behavior.

Production Source Assembler must not read market data.

Production Source Assembler must not enter a scan loop.

Production Source Assembler must not trigger Candidate Attention.

Production Source Assembler must not trigger Promote To Home.

Production Source Assembler must not trigger Opportunity Push.

Production Source Assembler must not generate entry / stop / TP / RR.

Production Source Assembler must not upgrade readiness.

Production Source Assembler must not create trading actions.

## 4. Future Candidate Files

Only as a future plan:

- `src/main/java/org/example/trademodel/service/watchlistsource/WatchlistRuntimeSourceService.java`
- `src/main/java/org/example/trademodel/service/watchlistsource/DefaultWatchlistRuntimeSourceService.java`
- `src/test/java/org/example/trademodel/service/watchlistsource/DefaultWatchlistRuntimeSourceServiceTest.java`
- `docs/PHASE_P235_RUNTIME_SOURCE_SERVICE_JAVA_SKELETON_VERIFICATION.md`

## 5. Conclusion

Production Source Assembler and Runtime Source Service can be merged into one minimum service skeleton.

Do not create a separate complex assembler layer unless a future authorization gate clearly requires it.
