# P291 Score Candidate Push Readiness Still Blocked

P291 keeps score, Candidate, Push, Readiness, point, and trading behavior blocked.

## Still Blocked

The following remain blocked after P291:

- production ScanScore;
- Candidate Attention;
- Candidate production workflow;
- Promote To Home runtime logic;
- Opportunity Push execution;
- external channel behavior;
- provider credential handling;
- live provider calls;
- message rendering;
- message sending;
- Telegram / email / webhook / app notification / local notification;
- Readiness;
- point generation;
- entry / stop / TP / RR;
- order API;
- execution API;
- auto-trading.

## Interpretation Boundary

Validator output is not a scan output.

Validator output is not a score.

Validator output is not a Candidate.

Validator output is not a Push payload.

Validator output is not Readiness.

Validator output is not point generation.

Validator output is not entry / stop / TP / RR.

Validator output is not an order, execution, or trading instruction.

## Boundary Rules

Watchlist Pool remains the scan candidate boundary.

Display Slots / 默认六币 are not the scan universe, not the batch universe, not the push universe, and not the Watchlist Pool proof source.

Risk Action Guard must remain before delivery / Push / Readiness.

踩踏状态禁止机会推送。

插针不等于趋势反转。

强反转不等于直接反手。
