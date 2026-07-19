# V1 Unified Call and CoinGlass Compatibility Amendment

## Revised Product Language

The formal scan-universe description is:

> Manual watchlist assets + auto candidates + a bounded discovery pool.

It replaces any operational assumption of a permanent fixed six-asset scan.

The formal discovery-notification description is:

> Raw discovery assets do not create execution-level notifications. After
> formal promotion to candidate they may create potential-opportunity and
> human-review-ready eligibility events.

The safe outward semantics are `VALID_FOR_MANUAL_REVIEW_ONLY` and
`OPPORTUNITY_REVIEW_READY`. Persisted legacy names such as
`VALID_EXECUTABLE` remain compatibility evidence only and never grant
execution permission. `CONFIRM_PUSH` is not the P3-CALL1 event name.

## CoinGlass Compatibility

CoinGlass remains an optional incremental derivatives-evidence layer. Its
four isolated datasets use the same canonical instrument mapping, request key,
budget, single-flight, cache, source status, freshness status, audit, and
query/refresh contracts. CoinGlass absence never becomes normal/low risk and
never blocks the independent Binance position-price safety path.

No CoinGlass call is made by this package.

## AI and Human Decision Boundary

The existing roles remain:

- `GPT_FINAL`: final review/adjudication role
- `GEMINI_REVIEW`: consistency review role
- `GROK_CHALLENGE`: adversarial challenge role

P3-CALL1 defines checkpoint depth only. It does not call those providers or
rewrite the existing conflict resolver. AI may later affect candidate review,
waiting/degradation, Confused, and human-review eligibility, but it cannot
override hard data gates, create a position, mutate a position, submit an
order, or authorize trading.

The system may actively discover and eventually notify opportunities. The
user still makes the final manual trading decision. Telegram delivery is
deferred to P3-N1. Production readiness remains `BLOCKED`.
