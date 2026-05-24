# PHASE P233 Runtime Source Service Authorization Gate

## 1. Phase Position

P233 only defines the authorization gate for a future Runtime Source Service / Production Source Assembler.

P233 does not write Java.

P233 does not connect `MarketQuoteClient`.

P233 does not enable scheduler behavior.

P233 does not create real scan.

## 2. Future P234 Candidate Scope

Future P234 may consider a docs-only runtime source service plan / assembler plan.

If future work enters Java, it requires a separate B/C authorization gate.

A future service may only compose:

- `RuntimeSourceReadRequestDTO`
- `RuleConfigWatchlistPoolReadAdapter`
- `WatchlistRuntimeSourceGuardValidator`
- `RuntimeSourceReadResultDTO`

A future service may only output safe `RuntimeSourceReadResultDTO` or `WatchlistRuntimeSourceDTO`.

A future service must not output `WatchlistScanResultDTO`.

A future service must not output ScanScore.

A future service must not trigger Candidate Attention / Promote To Home / Push / readiness / point generation.

## 3. Questions Future P234 Must Answer First

- Is Runtime Source Service only for Watchlist Pool? It must be.
- Are Display Slots allowed as input? They are not allowed.
- Are default six symbols allowed as input? They are not allowed.
- Does it call `RuleConfigWatchlistPoolReadAdapter`? It may.
- Does it call `WatchlistRuntimeSourceGuardValidator`? It must pass through or preserve the guard.
- Does it call `MarketQuoteClient`? It is not allowed.
- Is it called by scheduler? It is not allowed.
- Does it enter a scan loop? It is not allowed.
- Does it generate ScanScore? It is not allowed.
- Does it create push / readiness / entry-stop-TP-RR? It is not allowed.

## 4. Future P234 Candidate Files

Only as a plan:

- `src/main/java/org/example/trademodel/service/watchlistsource/WatchlistRuntimeSourceService.java`
- `src/main/java/org/example/trademodel/service/watchlistsource/DefaultWatchlistRuntimeSourceService.java`
- `src/test/java/org/example/trademodel/service/watchlistsource/DefaultWatchlistRuntimeSourceServiceTest.java`
- `docs/PHASE_P234_RUNTIME_SOURCE_SERVICE_PLAN.md`

## 5. Conclusion

P234 should not directly write Runtime Source Service Java.

P234 should first do a runtime source service plan / authorization gate, or if Java is written, it must use a separate B/C authorization gate.
