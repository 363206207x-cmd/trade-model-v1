# P287 Score Candidate Push Still Blocked

P287 does not authorize score, Candidate, or Push behavior.

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

## DTO Boundary

P288 may implement a pure-data `MarketReadRequestDTO` skeleton only if it remains review-only and not a trade instruction.

The DTO cannot compute score, cannot generate Candidate, cannot promote anything to home, cannot render a message, cannot send a message, and cannot execute Opportunity Push.

Only a GuardValidator-approved `RealScanInputContractDTO` can source the request. Missing proof, missing source contract, missing timestamp, missing timeframe, stale policy gaps, and missing-data policy gaps must fail closed.

## Risk Boundary

Risk Action Guard must remain before delivery / Push / Readiness.

踩踏状态禁止机会推送。

插针不等于趋势反转。

强反转不等于直接反手。

Display Slots / 默认六币 cannot be scan universe or batch universe. Watchlist Pool remains the scan candidate boundary.
