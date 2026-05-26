# P281 Real Scan Input Contract Guard Validator Skeleton Verification

P281 adds a Java skeleton plus targeted test for `RealScanInputContractGuardValidator`.

## Added

- `RealScanInputContractGuardValidator`
- `DefaultRealScanInputContractGuardValidator`
- `DefaultRealScanInputContractGuardValidatorTest`

## Verified Semantics

The targeted test covers:

- null input fails closed
- missing Watchlist Pool proof fails closed
- non-watchlist input fails closed
- valid-looking input remains review-only and not trade instruction
- validator preserves `manualReviewRequired=true` and `notTradeInstruction=true`
- blocked input cannot be upgraded to `REVIEW_ONLY`
- incomplete input remains fail-closed
- validator does not create scan output / score / Candidate / Push / Readiness / point / trading action
- implementation exposes no `MarketQuoteClient` / `BinanceMarketQuoteClient` / controller / scheduler / mapper / repository / `DataSource` / `JdbcTemplate` / webhook / Telegram / email / provider / order / execution / auto-trading dependency surface

## Still Blocked

P281 does not add Spring annotations, service wiring, controller, endpoint, API, scheduler, `MarketQuoteClient`, `BinanceMarketQuoteClient`, runtime/live/external data reads, scan output, real scan loop, production ScanScore computation, Candidate production workflow, Opportunity Push execution, external channel behavior, provider credentials, live provider calls, message rendering, message sending, Readiness, point generation, entry-stop-TP-RR, order/execution, or auto-trading.

Display Slots / 默认六币 cannot be scan universe or batch universe. Watchlist Pool remains the scan candidate boundary. Risk Action Guard must remain before delivery / Push / Readiness.

踩踏状态禁止机会推送。

插针不等于趋势反转。

强反转不等于直接反手。
