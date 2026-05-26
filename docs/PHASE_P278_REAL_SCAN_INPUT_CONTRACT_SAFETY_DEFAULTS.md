# P278 Real Scan Input Contract Safety Defaults

P278 defines safety defaults for a future DTO skeleton. It does not implement Java or tests.

## Required Defaults

Any future real scan input contract DTO must default:

- `manualReviewRequired=true`
- `notTradeInstruction=true`

The DTO must preserve review-only / fail-closed / audit-only semantics where applicable.

Missing Watchlist Pool proof, stale data requirements, missing data requirements, or risk blocker state must fail closed. A future DTO must not imply that scan input is executable, pushable, ready, tradable, or eligible for direct order flow.

## Forbidden Semantics

The future DTO must not create or expose:

- trade action
- order
- execution
- entry
- stop
- take profit
- RR
- provider
- external channel
- message sending
- readiness

Risk Action Guard must remain before delivery / Push / Readiness.

踩踏状态禁止机会推送。

插针不等于趋势反转。

强反转不等于直接反手。
