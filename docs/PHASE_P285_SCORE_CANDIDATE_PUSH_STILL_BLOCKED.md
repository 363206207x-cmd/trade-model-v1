# P285 Score Candidate Push Still Blocked

P285 does not authorize production score, Candidate, or Push behavior.

## Still Blocked

The following remain blocked:

- scan output creation;
- real scan loop;
- production ScanScore computation;
- Candidate production workflow;
- Candidate Attention production workflow;
- Promote To Home runtime logic;
- Opportunity Push execution;
- delivery pipeline behavior;
- external channel behavior;
- provider credentials;
- live provider call;
- message rendering;
- message sending;
- Telegram / email / webhook / app notification / local notification.

P285 only audits boundaries and authorization gates.

## Guard Boundary

Future input assembly must pass through `RealScanInputContractGuardValidator` and preserve `manualReviewRequired=true` and `notTradeInstruction=true`.

A review-only input cannot be treated as a score, Candidate, Push payload, delivery payload, external message, Readiness upgrade, point generation, or trading instruction.

## Risk Boundary

Risk Action Guard must remain before delivery / Push / Readiness.

踩踏状态禁止机会推送。

插针不等于趋势反转。

强反转不等于直接反手。

Display Slots / 默认六币 cannot be scan universe or batch universe. Watchlist Pool remains the scan candidate boundary.
