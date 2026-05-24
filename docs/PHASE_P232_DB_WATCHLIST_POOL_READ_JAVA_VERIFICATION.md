# PHASE P232 DB Watchlist Pool Read Java Verification

## 1. Phase Position

P232 only implements the minimal DB Watchlist Pool read Java skeleton.

P232 adds `RuleConfigWatchlistPoolReadAdapter` and targeted tests.

P232 is not real low-frequency scan.

P232 is not market data read.

P232 is not production scan loop.

## 2. Implemented Scope

P232 adds:

- `src/main/java/org/example/trademodel/service/watchlistsource/RuleConfigWatchlistPoolReadAdapter.java`
- `src/test/java/org/example/trademodel/service/watchlistsource/RuleConfigWatchlistPoolReadAdapterTest.java`

The adapter:

- implements `WatchlistPoolRuntimeSourceReadAdapter`.
- uses constructor injection for `RuleConfigService`.
- only reads the `push.watchlist.symbols` rule key from the existing rule config map.
- returns fail-closed `RuntimeSourceReadResultDTO` states for missing request, non-Watchlist-Pool request, incomplete request, missing service, read failure, missing / empty config, and non-watchlist symbol.
- returns `RuntimeSourceReadResultDTO.fromRuntimeSource(...)` only when the requested symbol is present in the Watchlist Pool config.
- wraps an `AVAILABLE_REVIEW_ONLY` `WatchlistRuntimeSourceDTO`.
- keeps `manualReviewRequired=true`.
- keeps `notTradeInstruction=true`.
- keeps `opportunityPushAllowed=false`.
- keeps `readinessUpgraded=false`.
- keeps `tradingActionCreated=false`.
- keeps `entryStopTpRrGenerated=false`.

## 3. Reused Existing Contract

P232 only reuses existing `RuleConfigService` and `tm_rule_config` semantics.

P232 does not modify:

- `RuleConfigService`
- `RuleConfigServiceImpl`
- `RuleConfigMapper`
- `RuleConfigDO`
- `schema.sql`

## 4. Explicit Non-Scope

P232 includes:

- no schema change.
- no mapper change.
- no API wiring.
- no controller wiring.
- no dashboard wiring.
- no `MarketQuoteClient`.
- no `BinanceMarketQuoteClient`.
- no scheduler.
- no scan loop.
- no real scan.
- no real ScanScore computation.
- no Candidate Attention workflow.
- no Promote To Home workflow.
- no opportunity push execution.
- no readiness.
- no real entry / stop / TP / RR.
- no order / execution / auto-trading.

## 5. Targeted Test Coverage

`RuleConfigWatchlistPoolReadAdapterTest` verifies:

- null request fails closed as `INCOMPLETE`.
- non-Watchlist-Pool request fails closed with `WATCHLIST_POOL_ONLY_REQUIRED`.
- incomplete request fails closed and preserves request missing fields.
- missing `RuleConfigService` returns `SOURCE_UNAVAILABLE`.
- rule config read exception returns `SOURCE_UNAVAILABLE`.
- missing / empty Watchlist Pool config returns `INCOMPLETE`.
- symbol outside Watchlist Pool returns `INCOMPLETE` with `BLOCKED_NOT_WATCHLIST`.
- symbol inside Watchlist Pool returns `AVAILABLE_REVIEW_ONLY`.
- parsing trims symbols and matches case-insensitively.
- all outputs keep no-push / no-readiness / no-trading / no-entry-stop-TP-RR defaults.
- adapter declares no forbidden `MarketQuoteClient` / `BinanceMarketQuoteClient` / controller / scheduler / push service / runtime data / datasource fields.

## 6. Verification Commands

Commands required for P232:

```bash
./mvnw -q -Dtest=RuleConfigWatchlistPoolReadAdapterTest test
./mvnw -q -DskipTests compile
./mvnw -q -DskipTests test-compile
git diff --check
git diff --name-status main...HEAD
git status
```

Results:

- `./mvnw -q -Dtest=RuleConfigWatchlistPoolReadAdapterTest test`: passed.
- `./mvnw -q -DskipTests compile`: passed.
- `./mvnw -q -DskipTests test-compile`: passed.
- `git diff --check`: passed.

## 7. Current Conclusion

P232 is the minimal DB Watchlist Pool read skeleton.

P232 is not real scan.

P232 is not market quote read.

P232 does not authorize scheduler activation, scan loop, ScanScore, Candidate Attention, Promote To Home, Opportunity Push, readiness, entry / stop / TP / RR, order API, execution API, or auto-trading.
