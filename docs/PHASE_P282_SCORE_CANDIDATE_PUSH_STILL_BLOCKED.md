# P282 Score Candidate Push Still Blocked

P282 does not authorize score, Candidate, or Push production behavior.

P281 validator only validates DTO safety / Watchlist Pool proof / fail-closed states / review-only flags. It preserves `manualReviewRequired=true` and `notTradeInstruction=true`. Valid-looking input remains review-only and not trade instruction.

## Still Blocked

The following remain blocked:

- Scan output creation.
- Real scan loop.
- Production ScanScore computation.
- Candidate production workflow.
- Promote To Home runtime logic.
- Opportunity Push execution.
- External channel behavior.
- Provider credentials.
- Live provider call.
- Message rendering.
- Message sending.
- Telegram / email / webhook / app notification / local notification.

Future test-only wiring must not read market data, create scan output, compute score, create Candidate, trigger Push, upgrade Readiness, or generate point.

Risk Action Guard must remain before delivery / Push / Readiness.

踩踏状态禁止机会推送。

插针不等于趋势反转。

强反转不等于直接反手。
