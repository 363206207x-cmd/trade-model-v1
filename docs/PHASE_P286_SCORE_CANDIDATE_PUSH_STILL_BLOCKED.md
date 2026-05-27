# P286 Score Candidate Push Still Blocked

P286 does not authorize score, Candidate, or Push behavior.

## Still Blocked

The following remain blocked:

- scan output creation;
- real scan loop;
- production ScanScore computation;
- Candidate production workflow;
- Candidate Attention production workflow;
- Promote To Home runtime logic;
- Opportunity Push execution;
- scheduler/API/dashboard wiring;
- external channel behavior;
- provider credentials;
- live provider call;
- message rendering;
- message sending;
- Telegram / email / webhook / app notification / local notification.

## Request Contract Limit

Future `MarketReadRequestDTO` may only be a review-only request contract if separately authorized. It cannot compute a score, produce Candidate, trigger Push, render a message, send a message, or promote anything to home.

The request must originate only from a GuardValidator-approved `RealScanInputContractDTO` and preserve review-only / not-trade-instruction semantics.

## Risk Boundary

Risk Action Guard must remain before delivery / Push / Readiness.

踩踏状态禁止机会推送。

插针不等于趋势反转。

强反转不等于直接反手。

Display Slots / 默认六币 cannot be scan universe or batch universe. Watchlist Pool remains the scan candidate boundary.
