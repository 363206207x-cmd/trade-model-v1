# PHASE P230 DB Watchlist Pool Read Plan

## 1. Phase Position

P230 is the plan document for future DB-backed Watchlist Pool read.

P230 does not implement Java.

P230 does not read DB.

P230 does not connect `MarketQuoteClient`.

P230 does not enable scheduler.

P230 does not create real scan.

## 2. Future DB Read Universe

Future DB read must use only Watchlist Pool as the scan universe.

Future DB read must not use Display Slots as the scan universe.

Future DB read must not use the default six symbols as the scan universe.

Non-watchlist assets must fail closed.

Empty watchlist must fail closed.

Disabled watchlist must fail closed.

## 3. Recommended Future DB Read Source

The current read-only audit shows these reusable pieces already exist:

- `src/main/java/org/example/trademodel/service/RuleConfigService.java`
- `src/main/java/org/example/trademodel/service/impl/RuleConfigServiceImpl.java`
- `src/main/java/org/example/trademodel/mapper/RuleConfigMapper.java`
- `src/main/java/org/example/trademodel/entity/RuleConfigDO.java`
- `src/main/resources/schema.sql` contains `tm_rule_config`

The user-specified path `src/main/java/org/example/trademodel/service/rule/RuleConfigService.java` does not exist in the current branch. The existing service is `src/main/java/org/example/trademodel/service/RuleConfigService.java`.

Future DB read should first audit whether the Watchlist Pool is already stored in `tm_rule_config` under `push.watchlist.symbols`.

This P230 read-only audit did not find a live Java or schema definition for `push.watchlist.symbols`.

If the watchlist is already stored in `tm_rule_config` / `push.watchlist.symbols`, future work should prefer that source.

Future work should not create a new DB table unless a later authorization gate explicitly allows it.

Future work should not bypass existing watchlist configuration or audit semantics.

## 4. Future DB Read Output

Future DB read may only output safe source read states:

- `RuntimeSourceReadResultDTO.incomplete(...)`
- `RuntimeSourceReadResultDTO.sourceUnavailable(...)`
- a safe result wrapping `WatchlistRuntimeSourceDTO`

Future `readStatus` may only be:

- `INCOMPLETE`
- `SOURCE_UNAVAILABLE`
- `STALE_REVIEW_ONLY`
- `AVAILABLE_REVIEW_ONLY`

Future DB read must not output:

- `WatchlistScanResultDTO`
- ScanScore
- push
- readiness
- trading
- entry / stop / TP / RR

## 5. Future DB Read Required Guards

Future DB read must pass through:

- `WatchlistRuntimeSourceGuardValidator`
- fail-closed no-op fallback
- missing / stale / unavailable expression
- `manualReviewRequired=true`
- `notTradeInstruction=true`

## 6. Conclusion

P230 does not authorize P231 to directly write DB read Java.

If P231 continues this track, it should first do a RuleConfig / mapper / schema read-only audit closure or a DB read Java authorization gate.
