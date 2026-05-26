# P276 Candidate Return Scope

P276 returns the next planning direction to Candidate production, but only as a scope plan.

It does not implement Candidate production workflow.

## Planning Topics

Future Candidate planning may define:

- Candidate lifecycle boundaries
- Candidate Attention review-only semantics
- Promote To Home preview boundaries
- Watchlist Pool ownership
- Risk Action Guard blockers
- fail-closed incomplete states
- manual review language
- not-trade-instruction language

## Not Implementation

P276 does not authorize:

- Candidate production workflow
- Promote To Home runtime logic
- production ScanScore computation
- runtime/live/external data reads
- real scan loop
- scheduler/API/dashboard wiring
- Opportunity Push execution
- external channel behavior
- message rendering
- message sending
- Readiness
- point generation
- entry/stop/TP/RR
- order/execution
- auto-trading

## Candidate Safety Boundary

Display Slots are display priority only. 默认六币 cannot become a batch universe or push universe.

Watchlist Pool remains the candidate boundary.

Risk Action Guard must remain before delivery. Stampede state blocks opportunity push. A wick-only/pin-bar move is not a trend reversal. Strong reversal is not direct reverse trading.
