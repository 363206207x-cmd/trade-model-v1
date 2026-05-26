# P275 Readiness Point Generation Still Blocked

P275 does not authorize Readiness, point generation, or trading path behavior.

This remains true after P274 and after the P274 local-validation exception merge.

## Still Blocked

The following remain blocked:

- ExecutionPlan Readiness upgrade
- point generation
- real entry
- real stop
- real take profit
- real risk/reward
- order API
- execution API
- auto-trading

P275 also does not authorize:

- external channel behavior
- Telegram/email/webhook/app notification/local notification
- provider credential handling
- live provider calls
- provider selection / provider integration
- message rendering
- message sending
- scheduler/API/dashboard wiring
- runtime/live/external data reads
- real scan loop
- external Opportunity Push execution
- Promote To Home runtime logic

## Required Future Gate

Any future Readiness or point-generation work must have a separate issue and authorization gate.

That future gate must explicitly define:

- data ownership
- numeric source ownership
- Risk Action Guard preconditions
- manual review requirements
- not-trade-instruction language
- no auto-trading boundary

## Safety Rules

Display Slots / 默认六币 cannot become the batch universe or default push universe.

Watchlist Pool remains the push candidate boundary.

Risk Action Guard must remain before any delivery path.

Stampede state blocks opportunity push. A wick-only/pin-bar move is not a trend reversal. Strong reversal is not direct reverse trading.
