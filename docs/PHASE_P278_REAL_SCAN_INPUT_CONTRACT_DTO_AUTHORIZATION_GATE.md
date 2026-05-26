# P278 Real Scan Input Contract DTO Authorization Gate

P278 authorizes only a future real scan input contract DTO skeleton plan.

P277 merged as `82d5313`. P278 is docs-only and is not DTO implementation. It does not create Java, tests, enums, services, schedulers, APIs, dashboards, market-read adapters, scan loops, score computation, Candidate workflows, Push execution, Readiness, point generation, or trading paths.

## Authorization Boundary

P279 may add a DTO / enum / targeted test only if separately authorized. That future package must stay DTO-only and targeted-test-only unless another issue explicitly expands the scope.

The future DTO must be a contract object for review-only real scan input eligibility. It must not perform market reads, compute scores, create candidates, trigger Push, or route anything toward delivery.

## Required Safety Shape

The future DTO must preserve review-only / fail-closed / audit-only semantics where applicable.

It must default:

- `manualReviewRequired=true`
- `notTradeInstruction=true`

It must exclude trade action, order, execution, entry, stop, take profit, RR, provider, external channel, message sending, and readiness fields.

Watchlist Pool proof is required before any future scan input can be considered eligible. Display Slots / 默认六币 cannot be scan universe or batch universe.

Risk Action Guard must remain before delivery / Push / Readiness.

踩踏状态禁止机会推送。

插针不等于趋势反转。

强反转不等于直接反手。
