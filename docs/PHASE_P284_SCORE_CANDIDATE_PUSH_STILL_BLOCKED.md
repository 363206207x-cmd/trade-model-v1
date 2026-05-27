# P284 Score Candidate Push Still Blocked

P284 does not authorize score, Candidate, or Push production behavior.

P283 test-only wiring confirmed that guarded input stays review-only and not a trade instruction. That does not authorize scan output, scoring, Candidate workflow, or Push execution.

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

Future market-read boundary or real scan input assembly work must not read market data, create scan output, compute score, create Candidate, trigger Push, upgrade Readiness, or generate point unless a later explicit authorization gate permits it.

Risk Action Guard must remain before delivery / Push / Readiness.

踩踏状态禁止机会推送。

插针不等于趋势反转。

强反转不等于直接反手。
