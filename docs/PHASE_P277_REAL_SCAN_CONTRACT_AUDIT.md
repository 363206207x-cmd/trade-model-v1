# P277 Real Scan Contract Audit

P277 audits the future real scan contract.

This is a docs-only audit. It does not implement real scan behavior.

## Audit Scope

The future real scan contract may need to define:

- scan input ownership
- scan universe source
- Watchlist Pool membership proof
- market-read reference ownership
- stale data handling
- missing data handling
- raw data availability state
- scan timestamp semantics
- risk blockers
- downstream eligibility flags
- review-only output semantics
- fail-closed output semantics
- audit-only output semantics where applicable

## Contract Boundary

Future scan output must not imply a trading instruction.

Future scan output must not imply Readiness, point generation, entry, stop, take profit, risk/reward, order creation, execution creation, or auto-trading.

Any future scan result must remain review-only and fail-closed until a separate issue authorizes implementation.

## Not Authorized In P277

P277 does not authorize:

- DTO implementation
- Java implementation
- `MarketQuoteClient` wiring
- `BinanceMarketQuoteClient` wiring
- runtime/live/external data reads
- real scan loop
- production ScanScore computation
- Candidate production workflow
- Opportunity Push execution
- scheduler/API/dashboard wiring
- external channel behavior
- Readiness
- point generation
- entry/stop/TP/RR
- order/execution
- auto-trading

## Safety Rules

The scan universe can only be Watchlist Pool.

Display Slots / 默认六币 cannot be used as a batch universe.

Risk Action Guard must remain before delivery / Push / Readiness. 踩踏状态禁止机会推送。插针不等于趋势反转。强反转不等于直接反手。
