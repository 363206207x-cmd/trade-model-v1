# P275 Real Scan Score Candidate Return Gate

P275 redirects the next planning direction away from further external-channel no-op expansion and back toward real scan / real score / Candidate / Push preview.

This is a planning return gate only. P275 does not implement any runtime or production behavior.

## Recommended Next Planning Direction

The next phase should evaluate returning to:

- real scan planning
- real score planning
- Candidate planning
- Push preview planning

The goal is to move from increasingly deep disabled push channel skeletons back toward the upstream data and review surfaces that decide whether anything is worth previewing at all.

## Planning Scope Only

This gate may be used to plan future work around:

- scan source boundaries
- score computation boundaries
- Candidate attention semantics
- Push preview display and review semantics
- Watchlist Pool candidate boundaries
- Risk Action Guard pre-delivery checks

It does not authorize implementation in P275.

## Still Not Authorized

P275 does not authorize:

- MarketQuoteClient integration
- BinanceMarketQuoteClient integration
- runtime/live/external data reads
- real scan loop
- production ScanScore computation
- Candidate production workflow
- Promote To Home runtime logic
- Opportunity Push execution
- delivery pipeline execution
- external channel behavior
- Telegram/email/webhook/app notification/local notification
- provider credentials
- live provider calls
- message rendering
- message sending
- scheduler/API/dashboard wiring
- Readiness
- point generation
- real entry/stop/TP/RR
- order/execution
- auto-trading

## Candidate Boundary

Display Slots / 默认六币 cannot be used as a batch universe.

Watchlist Pool is the push candidate boundary.

Risk Action Guard must be evaluated before any future delivery or preview escalation. Stampede state blocks opportunity push. A wick-only/pin-bar move is not a trend reversal. Strong reversal is not direct reverse trading.
