# Opportunity Notification Eligibility

## Event Types

- `OPPORTUNITY_DISCOVERED`
- `OPPORTUNITY_REVIEW_READY`
- `POSITION_RISK_WARNING`
- `SYSTEM_DATA_WARNING`

P3-CALL1 evaluates and collects eligibility only. The runtime publisher is
NoOp; the in-memory collector is test-only. There is no Telegram outbox or send.

## Opportunity Rules

`OPPORTUNITY_DISCOVERED` may come from a promoted watchlist candidate or a
promoted discovery candidate. It means a potential opportunity is waiting for
confirmation. Entry, stop, and take-profit boundaries are not required.

`OPPORTUNITY_REVIEW_READY` additionally requires triggered state, fresh data,
four complete timeframes, data-quality pass, complete entry/stop/take-profit,
computable reward/risk, current plan, risk-gate pass, no confused block,
completed Hot Reset review, and passed Push Recheck. It means ready for human
review only.

`WATCHLIST_ONLY` excludes discovery-origin opportunity events.
`WATCHLIST_AND_DISCOVERY` allows formally promoted discovery candidates. Raw
P3 discovery never emits review-ready eligibility.

Position warnings require P0 active-position origin. System data warnings
require a system origin and an explicit stale/disconnected/unavailable condition.

The SHA-256 dedup identity includes event type, canonical instrument, strategy
version, evidence hash, plan ID, and risk level. A triggered review-ready event
is distinct from the earlier discovered event. Every eligible event forces:

- `notTradeInstruction=true`
- `manualDecisionRequired=true`

No event creates a position, plan, order, trade, or external message.
Production readiness remains `BLOCKED`.
