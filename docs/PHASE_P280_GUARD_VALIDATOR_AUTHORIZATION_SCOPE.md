# P280 Guard Validator Authorization Scope

P280 authorizes only the scope for a future `RealScanInputContractGuardValidator` skeleton. It does not implement the validator.

## Future Validator May Check

A future validator may validate only:

- DTO exists.
- `manualReviewRequired=true`.
- `notTradeInstruction=true`.
- Watchlist Pool membership proof is present.
- Non-watchlist input fails closed.
- Missing proof fails closed.
- Review-only safety flags remain present.
- Blocking reasons preserve fail-closed context.

The validator may report fail-closed validation state for manual review. It must remain review-only and must not convert any scan input into a production scan output, score, Candidate, Push, Readiness state, point, or trading action.

## Future Validator Must Not Do

The future GuardValidator must not:

- Read market data.
- Call `MarketQuoteClient`.
- Call `BinanceMarketQuoteClient`.
- Read runtime/live/external data.
- Create a real scan loop.
- Create production scan output.
- Compute production ScanScore.
- Create Candidate production workflow.
- Trigger Opportunity Push.
- Wire scheduler/API/dashboard.
- Touch external channels or provider credentials.
- Render or send messages.
- Upgrade Readiness.
- Generate entry / stop / TP / RR.
- Connect order/execution/auto-trading.

## Candidate Boundary

Display Slots / 默认六币 cannot be scan universe or batch universe. Watchlist Pool remains the scan candidate boundary.

Risk Action Guard must remain before delivery / Push / Readiness.

踩踏状态禁止机会推送。

插针不等于趋势反转。

强反转不等于直接反手。
