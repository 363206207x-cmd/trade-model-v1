# P284 Guard Validator Test-Only Wiring Closure

P284 closes P283.

P283 merged as `f26befe` and added `RealScanInputContractGuardValidatorTestOnlyWiringTest`. P283 CI passed before merge.

## P283 Closure

P283 was only test-only / targeted-test-only.

P283 confirmed:

- Test-only wiring can pass valid-looking `RealScanInputContractDTO` into `RealScanInputContractGuardValidator` and remain `REVIEW_ONLY`.
- Missing Watchlist Pool proof remains `BLOCKED_MISSING_WATCHLIST_PROOF`.
- Non-watchlist input remains `BLOCKED_NOT_WATCHLIST`.
- Null input remains `INCOMPLETE`.
- Blocked input cannot be upgraded to `REVIEW_ONLY`.
- Outputs preserve `manualReviewRequired=true` and `notTradeInstruction=true`.

P283 did not modify production Java or DTO files.

## Non-Scope Confirmed

P283 and P284 do not add:

- `MarketQuoteClient` / `BinanceMarketQuoteClient` wiring.
- Runtime/live/external data reads.
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
