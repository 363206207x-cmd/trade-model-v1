# P263 External Channel Still Blocked

## 1. Blocked Channels

External providers remain blocked:

- Telegram
- email
- webhook
- app notification
- local notification

P263 does not connect any external or local notification channel.

P263 does not send any message.

## 2. P262 Does Not Change This

P262 added only an audit-only internal envelope skeleton:

- `OpportunityPushAuditEnvelopeDTO`
- `OpportunityPushAuditEnvelopeStatusEnum`
- `OpportunityPushAuditEnvelopeAssembler`
- `NoOpOpportunityPushAuditEnvelopeAssembler`
- `NoOpOpportunityPushAuditEnvelopeAssemblerTest`

P262 CI passed before merge.

P262 is not external push delivery, not a real push channel, not persistence, not queue behavior, not Readiness, not point generation, and not trading advice.

## 3. Future Channel Requirements

Any future channel must be separately authorized and must start:

- disabled-by-default
- fail-closed
- audit-only
- review-only
- not a trade instruction
- with throttling requirements
- with idempotency requirements
- with audit requirements
- with Risk Action Guard before delivery eligibility
- within the Watchlist Pool candidate boundary

## 4. Still Blocked

Scheduler / API / dashboard wiring remains blocked.

Runtime / live / external data reads remain blocked.

MarketQuoteClient / BinanceMarketQuoteClient integration remains blocked.

Readiness / point generation / entry-stop-TP-RR / order / execution / auto-trading remain blocked.

Display Slots / 默认六币不能作为 batch universe。

Watchlist Pool 才是推送候选边界。

踩踏状态禁止机会推送。

插针不等于趋势反转。

强反转不等于直接反手。
