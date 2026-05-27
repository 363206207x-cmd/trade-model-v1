# P283 Real Scan Input Contract Guard Validator Test-Only Wiring Verification

P283 adds a test-only wiring skeleton for `RealScanInputContractGuardValidator`.

P282 merged as `d15ced8` (`BACKEND-P282 RealScanInputContractGuardValidator Closure and Test-Only Wiring Authorization Scope Pack (#683)`). P282 authorized only a future test-only wiring skeleton, with no production service wiring.

## Added

- `RealScanInputContractGuardValidatorTestOnlyWiringTest`

## Verified Semantics

The targeted test verifies:

- Test-only wiring can pass valid-looking `RealScanInputContractDTO` into `RealScanInputContractGuardValidator` and remain `REVIEW_ONLY`.
- Missing Watchlist Pool proof remains `BLOCKED_MISSING_WATCHLIST_PROOF`.
- Non-watchlist input remains `BLOCKED_NOT_WATCHLIST`.
- Null input remains `INCOMPLETE`.
- Blocked input cannot be upgraded to `REVIEW_ONLY`.
- Output always preserves `manualReviewRequired=true` and `notTradeInstruction=true`.
- Test-only wiring exposes no production dependency surface: no Spring, controller, scheduler, `MarketQuoteClient`, `BinanceMarketQuoteClient`, mapper, repository, `DataSource`, `JdbcTemplate`, provider, webhook, Telegram, email, order, execution, or auto-trading.
- Test-only wiring does not expose scan output / score / Candidate / Push / Readiness / point / trading action surface.

## Verification Commands

- `./mvnw -q -Dtest=RealScanInputContractGuardValidatorTestOnlyWiringTest test` passed.
- `./mvnw -q -DskipTests compile` passed.
- `./mvnw -q -DskipTests test-compile` passed.
- `mvn -B verify -Pci` requires Java 17 for the project enforcer rule; local rerun with `JAVA_HOME=/Library/Java/JavaVirtualMachines/temurin-17.jdk/Contents/Home` passed.

## Still Blocked

P283 does not modify production Java, DTOs, existing tests, dashboard, schema, config, controller, endpoint, API, service, scheduler, mapper, repository, DB write, or migration.

P283 does not wire `MarketQuoteClient` / `BinanceMarketQuoteClient`, read runtime/live/external data, create scan output, create a real scan loop, compute production ScanScore, create Candidate production workflow, execute Opportunity Push, implement external channel behavior, handle provider credentials, make live provider calls, render/send messages, upgrade Readiness, generate point generation, generate real entry / stop / TP / RR, connect order/execution APIs, or enable auto-trading.

Display Slots / 默认六币 cannot be scan universe or batch universe. Watchlist Pool remains the scan candidate boundary. Risk Action Guard must remain before delivery / Push / Readiness.

踩踏状态禁止机会推送。

插针不等于趋势反转。

强反转不等于直接反手。
