# P282 Guard Validator Closure

P282 closes P281 RealScanInputContractGuardValidator Skeleton.

P281 merged as `e65b3a7` (`BACKEND-P281 RealScanInputContractGuardValidator Skeleton (#681)`). P281 added:

- `RealScanInputContractGuardValidator`
- `DefaultRealScanInputContractGuardValidator`
- `DefaultRealScanInputContractGuardValidatorTest`

P281 CI passed before merge.

## Confirmed P281 Scope

P281 was only Java skeleton + targeted-test-only. The validator is a plain Java skeleton and does not expose Spring annotations, production service wiring, controller, endpoint, API, scheduler, mapper, repository, DB write, schema migration, dashboard wiring, or external channel wiring.

The validator only validates:

- DTO safety.
- Watchlist Pool proof.
- Fail-closed states.
- Review-only flags.
- `manualReviewRequired=true`.
- `notTradeInstruction=true`.

The validator preserves `manualReviewRequired=true` and `notTradeInstruction=true` across outputs.

## Verified Fail-Closed Semantics

P281 targeted tests confirm:

- Null input fails closed.
- Missing Watchlist Pool proof fails closed.
- Non-watchlist input fails closed.
- Valid-looking input remains review-only and not trade instruction.
- Blocked input cannot be upgraded to `REVIEW_ONLY`.

## Explicit Non-Scope Confirmed

P281 did not add:

- `MarketQuoteClient` / `BinanceMarketQuoteClient` wiring.
- Runtime/live/external data read.
- Scan output creation.
- Real scan loop.
- Production ScanScore computation.
- Candidate production workflow.
- Opportunity Push execution.
- Scheduler/API/dashboard wiring.
- External channel behavior / provider credentials / live provider call / message rendering / sending.
- Readiness / point generation / entry-stop-TP-RR / order / execution / auto-trading.

Display Slots / 默认六币 cannot be scan universe or batch universe. Watchlist Pool remains the scan candidate boundary. Risk Action Guard must remain before delivery / Push / Readiness.

踩踏状态禁止机会推送。

插针不等于趋势反转。

强反转不等于直接反手。
