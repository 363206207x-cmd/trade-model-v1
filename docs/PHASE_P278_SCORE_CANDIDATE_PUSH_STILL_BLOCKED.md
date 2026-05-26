# P278 Score Candidate Push Still Blocked

P278 does not authorize score, Candidate, or Push implementation.

Still blocked:

- production ScanScore computation
- Candidate production workflow
- Candidate Attention production workflow
- Promote To Home runtime logic
- Opportunity Push execution
- scheduler/API/dashboard wiring for scan/score/candidate/push
- external channel behavior
- provider credentials
- live provider call
- message rendering
- message sending

Future scan input contracts must remain review-only and fail-closed. They must not imply that a symbol is scored, promoted, pushed, executable, or ready.

Watchlist Pool remains the candidate boundary. Display Slots / 默认六币 cannot be scan universe or batch universe.

Risk Action Guard must remain before delivery / Push / Readiness.

踩踏状态禁止机会推送。

插针不等于趋势反转。

强反转不等于直接反手。
