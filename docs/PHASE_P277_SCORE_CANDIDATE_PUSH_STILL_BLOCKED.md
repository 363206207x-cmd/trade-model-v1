# P277 Score Candidate Push Still Blocked

P277 does not authorize production ScanScore, Candidate production workflow, or Opportunity Push execution.

The P277 audit is limited to real scan contract and market-read boundaries.

## Still Blocked

The following remain blocked:

- production ScanScore computation
- Candidate production workflow
- Candidate Attention production workflow
- Promote To Home runtime logic
- Opportunity Push execution
- Push execution
- delivery pipeline execution
- external push execution
- scheduler/API/dashboard wiring
- external channel behavior
- provider credentials
- live provider calls
- message rendering
- message sending

## Future Gate Required

Any future score, Candidate, or Push work must have a separate authorization gate.

That future gate must preserve review-only and fail-closed semantics until implementation is separately authorized.

## Safety Rules

Watchlist Pool remains the candidate boundary.

Display Slots / 默认六币 cannot be used as a batch universe.

Risk Action Guard must remain before delivery / Push / Readiness. 踩踏状态禁止机会推送。插针不等于趋势反转。强反转不等于直接反手。
