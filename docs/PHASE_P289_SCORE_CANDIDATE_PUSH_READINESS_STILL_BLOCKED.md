# P289 Score Candidate Push Readiness Still Blocked

P289 keeps score, Candidate, Push, Readiness, point generation, and trading behavior blocked.

P289 is docs-only. It does not implement Java, tests, DTO changes, score computation, Candidate workflow, Push execution, Readiness upgrade, point generation, or trading path.

## Still Blocked After P289

The following remain blocked:

- ScanScore;
- production ScanScore computation;
- Candidate production workflow;
- Candidate Attention;
- Promote To Home;
- Opportunity Push;
- external channel;
- message rendering;
- message sending;
- Telegram / email / webhook / app notification / local notification;
- Readiness;
- point generation;
- entry / stop / TP / RR;
- order API;
- execution API;
- auto-trading.

## Guard Boundary

A future `MarketReadRequestGuardValidator` may only return blocked / review-only / fail-closed validation results. It must not create scan output, score, Candidate, Push payload, Readiness state, point-generation output, entry, stop, TP, RR, order instruction, execution instruction, or auto-trading behavior.

## Risk Rules

Risk Action Guard must remain before delivery / Push / Readiness.

踩踏状态禁止机会推送。

插针不等于趋势反转。

强反转不等于直接反手。
