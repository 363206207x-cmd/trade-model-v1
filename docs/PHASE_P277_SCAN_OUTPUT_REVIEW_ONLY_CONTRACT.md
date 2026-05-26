# P277 Scan Output Review-Only Contract

P277 defines the future scan output contract as review-only / fail-closed / audit-only where applicable.

This is a contract plan only. It does not implement DTO / Java.

## Future Contract Shape

Future scan result contract may include:

- symbol
- source
- watchlist membership proof
- data availability
- stale/missing data state
- raw market-read references
- scan timestamp
- risk blockers
- downstream eligibility flags

These fields are planning candidates only in P277.

## Required Output Semantics

Future scan output must remain:

- review-only
- fail-closed
- audit-only where applicable
- manual-review oriented
- not a trade instruction

Future scan output must not create:

- production ScanScore
- Candidate production workflow
- Opportunity Push execution
- delivery execution
- external push execution
- Readiness
- point generation
- entry/stop/TP/RR
- order/execution
- auto-trading

## Guard Ordering

Risk Action Guard must remain before delivery / Push / Readiness.

踩踏状态禁止机会推送。插针不等于趋势反转。强反转不等于直接反手。

## Universe Boundary

Scan output can only be planned for Watchlist Pool members.

Display Slots / 默认六币 cannot be used as a batch universe.
