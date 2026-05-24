# PHASE P231 DB Watchlist Pool Read Java Authorization Gate

## 1. Phase Position

P231 only defines the authorization gate for future DB Watchlist Pool read Java.

P231 does not write Java.

P231 does not read DB.

P231 does not connect `MarketQuoteClient`.

P231 does not enable scheduler.

P231 does not create real scan.

## 2. Future P232 Candidate Scope

Future P232 may consider one minimal DB Watchlist Pool read Java skeleton.

The future skeleton may only read the Watchlist Pool configuration source.

Future work should prefer reusing:

- `RuleConfigService`
- `RuleConfigMapper`
- `tm_rule_config`

Future work may consider reading `ruleKey=push.watchlist.symbols`, but it must first prove fail-closed behavior when the key is missing.

If the key does not exist, the output must be `INCOMPLETE` or `SOURCE_UNAVAILABLE`.

P232 must not create new schema unless a separate authorization gate explicitly allows it.

P232 must not add watchlist API or dashboard wiring.

P232 must not connect `MarketQuoteClient` or scheduler.

## 3. Future P232 Recommended Candidate Files

Only as an authorization plan:

- `src/main/java/org/example/trademodel/service/watchlistsource/RuleConfigWatchlistPoolReadAdapter.java`
- `src/test/java/org/example/trademodel/service/watchlistsource/RuleConfigWatchlistPoolReadAdapterTest.java`
- `docs/PHASE_P232_DB_WATCHLIST_POOL_READ_JAVA_VERIFICATION.md`

## 4. Future P232 Required Safety Defaults

Future P232 must keep:

- `manualReviewRequired=true`
- `notTradeInstruction=true`
- `opportunityPushAllowed=false`
- `readinessUpgraded=false`
- `tradingActionCreated=false`
- `entryStopTpRrGenerated=false`
- empty watchlist fail-closed
- disabled / missing config fail-closed
- invalid config fail-closed
- DB unavailable fail-closed
- non-watchlist fail-closed
- Watchlist Pool only

## 5. Future P232 Prohibited Scope

Future P232 must not:

- read market data.
- connect `MarketQuoteClient`.
- connect `BinanceMarketQuoteClient`.
- connect scheduler.
- connect controller / API.
- modify dashboard.
- create scan loop.
- create real scan.
- generate `WatchlistScanResultDTO`.
- generate ScanScore.
- create Candidate Attention.
- create Promote To Home.
- create Opportunity Push execution.
- generate entry / stop / TP / RR.
- upgrade readiness.
- create trading action.

## 6. Conclusion

P231 only authorizes future consideration of a minimal DB Watchlist Pool read Java skeleton.

P231 does not authorize market, scheduler, scan loop, push, readiness, or point generation.
