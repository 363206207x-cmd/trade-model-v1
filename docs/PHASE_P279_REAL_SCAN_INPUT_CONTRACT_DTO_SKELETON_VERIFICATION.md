# P279 Real Scan Input Contract DTO Skeleton Verification

P279 adds a DTO-only / enum-only / targeted-test-only skeleton for the future real scan input contract.

## Added

- `RealScanInputContractDTO`
- `RealScanInputContractStatusEnum`
- `RealScanInputContractDTOTest`

## Verified Semantics

The targeted test covers:

- default/manual review safety flags remain true
- missing Watchlist Pool proof fails closed
- non-watchlist input fails closed
- valid-looking input remains review-only and not trade instruction
- defensive copy for `requestedTimeframes`, `riskBlockers`, `reviewOnlySafetyFlags`, and `blockingReasons`
- enum names expose no BUY / SELL / LONG / SHORT / READY / EXECUTABLE / SENT / TRADE / ORDER / ENTRY / STOP / TAKE_PROFIT surface
- DTO fields and method names expose no order / execution / entry / stop / takeProfit / riskReward / provider / externalChannel / messageSending / readiness surface
- implementation has no controller / scheduler / MarketQuoteClient / BinanceMarketQuoteClient / webhook / Telegram / email / mapper / repository / DataSource / JdbcTemplate / order / execution / auto-trading dependency surface

## Still Blocked

P279 does not add services, Spring annotations, controller, endpoint, API, scheduler, `MarketQuoteClient`, `BinanceMarketQuoteClient`, runtime/live/external data reads, real scan loop, production ScanScore computation, Candidate production workflow, Opportunity Push execution, external channel behavior, provider credentials, live provider calls, message rendering, message sending, Readiness, point generation, entry-stop-TP-RR, order/execution, or auto-trading.

Display Slots / 默认六币 cannot be scan universe or batch universe. Watchlist Pool remains the scan candidate boundary. Risk Action Guard must remain before delivery / Push / Readiness.

踩踏状态禁止机会推送。

插针不等于趋势反转。

强反转不等于直接反手。
