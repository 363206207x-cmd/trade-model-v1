# P280 Real Scan Input Contract DTO Closure

P280 closes P279.

P279 merged as `409741c` (`BACKEND-P279 Real Scan Input Contract DTO Skeleton (#677)`). P279 CI passed before merge.

## P279 Added

P279 added:

- `RealScanInputContractDTO`
- `RealScanInputContractStatusEnum`
- `RealScanInputContractDTOTest`

## Confirmed Semantics

P279 is only a DTO-only / enum-only / targeted-test-only skeleton. It does not implement Java services, Spring annotations, controller, endpoint, API, scheduler, market-read wiring, runtime/live/external data reads, real scan loop, production ScanScore computation, Candidate production workflow, Opportunity Push execution, external channel behavior, provider credential handling, live provider call, message rendering, message sending, Readiness, point generation, entry-stop-TP-RR, order/execution, or auto-trading.

`RealScanInputContractDTO` defaults `manualReviewRequired=true` and `notTradeInstruction=true`.

Missing Watchlist Pool proof fails closed.

Non-watchlist input fails closed.

Valid-looking input remains review-only and not trade instruction.

The DTO has no trade action / order / execution / entry / stop / take profit / RR / provider / external channel / message sending / readiness fields.

## Closure Decision

P280 does not change the DTO or tests. P280 only records the closure and opens the next authorization gate for a future `RealScanInputContractGuardValidator` skeleton.

Display Slots / 默认六币 cannot be scan universe or batch universe. Watchlist Pool remains the scan candidate boundary. Risk Action Guard must remain before delivery / Push / Readiness.

踩踏状态禁止机会推送。

插针不等于趋势反转。

强反转不等于直接反手。
