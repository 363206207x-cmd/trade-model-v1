# PHASE P252 Market Read Adapter Skeleton Verification

## 1. Phase Positioning

P252 combines the Market-read authorization boundary with a maximum-safe adapter skeleton.

P252 adds only:

- `WatchlistMarketReadAdapter`
- `DefaultWatchlistMarketReadAdapter`
- `DefaultWatchlistMarketReadAdapterTest`

P252 is read-only, fail-closed, and no-op by default.

P252 is not real market data read.

P252 is not production MarketQuoteClient integration.

## 2. Implemented Behavior

`WatchlistMarketReadAdapter` defines only:

- `readMarket(RuntimeSourceReadRequestDTO request)`

`DefaultWatchlistMarketReadAdapter`:

- returns `INCOMPLETE` for missing request.
- returns `INCOMPLETE` for non-Watchlist-Pool requests.
- returns `INCOMPLETE` for blank symbol.
- returns `SOURCE_UNAVAILABLE` for valid Watchlist Pool requests.
- records `MARKET_READ_ADAPTER_NO_OP`.
- records `MARKET_CLIENT_NOT_CONNECTED`.
- keeps no-execution defaults.

## 3. Safety Boundary

P252 does not include:

- schema change.
- mapper change.
- API wiring.
- controller wiring.
- dashboard wiring.
- real `MarketQuoteClient`.
- real `BinanceMarketQuoteClient`.
- runtime / live / external data reads.
- scheduler.
- scan loop.
- real scan.
- real ScanScore computation.
- Candidate Attention workflow.
- Promote To Home workflow.
- opportunity push execution.
- readiness.
- real entry / stop / TP / RR.
- order / execution / auto-trading.

All outputs preserve:

- `manualReviewRequired=true`
- `notTradeInstruction=true`
- `opportunityPushAllowed=false`
- `readinessUpgraded=false`
- `tradingActionCreated=false`
- `entryStopTpRrGenerated=false`

## 4. Test Coverage

Targeted test:

```bash
./mvnw -q -Dtest=DefaultWatchlistMarketReadAdapterTest test
```

Result: PASS.

The test proves:

- null request fails closed.
- non-Watchlist-Pool request fails closed.
- blank symbol fails closed.
- valid Watchlist Pool request returns source unavailable no-op.
- no runtime / live / external data dependency is declared.
- no `MarketQuoteClient` field is declared.
- no `BinanceMarketQuoteClient` field is declared.
- no scheduler field / method is declared.
- no `DataSource` / `JdbcTemplate` field is declared.
- no score / push / readiness / trading semantics are enabled in output.
- no forbidden fields / methods for `MarketQuoteClient`, `BinanceMarketQuoteClient`, `Scheduler`, `Controller`, `PushRecheckService`, `PushSnapshotService`, `DataSource`, `JdbcTemplate`, or `Scheduled` annotation.

Compile:

```bash
./mvnw -q -DskipTests compile
```

Result: PASS.

Test compile:

```bash
./mvnw -q -DskipTests test-compile
```

Result: PASS.

Diff check:

```bash
git diff --check
```

Result: PASS.

## 5. Current Conclusion

P252 is a Market-read adapter skeleton.

P252 is not real market data read.

P252 is not production market integration.

P252 does not authorize real `MarketQuoteClient`, `BinanceMarketQuoteClient`, scheduler, scan loop, ScanScore, Candidate Attention, Promote To Home, Opportunity Push, readiness, point generation, order, execution, or auto-trading.
