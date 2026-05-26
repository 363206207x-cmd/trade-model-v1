# P263 Next Java Authorization Gate

## 1. Next Java Must Choose One Path

The next Java PR may choose only one of these two paths:

1. audit-envelope persistence skeleton
2. audit-queue skeleton

One Java PR must not implement both persistence and queue.

## 2. Shared Minimum Rules

Whichever path is chosen later must start:

- disabled-by-default
- fail-closed
- audit-only
- review-only
- not a trade instruction
- without external provider dependency
- without message sending
- without scheduler / API / dashboard wiring
- without runtime / live / external data read
- without Readiness upgrade
- without point generation
- without entry-stop-TP-RR
- without order / execution / auto-trading

## 3. If Persistence Skeleton Is Chosen

The PR may only define the minimum audit-envelope persistence skeleton authorized by that future issue.

It must not implement audit queue behavior.

It must not implement delivery pipeline behavior.

It must not connect external providers.

It must not send messages.

## 4. If Audit Queue Skeleton Is Chosen

The PR may only define the minimum audit-queue skeleton authorized by that future issue.

It must not implement audit envelope persistence.

It must not implement delivery pipeline behavior.

It must not connect external providers.

It must not send messages.

## 5. Explicitly Not Authorized For The Next Java PR

The next Java PR is not authorized to implement:

- delivery pipeline
- Telegram
- email
- webhook
- app notification
- local notification
- scheduler activation
- API / dashboard controls
- MarketQuoteClient
- BinanceMarketQuoteClient
- runtime / live / external data reads
- real scan loop
- external Opportunity Push execution
- Promote To Home runtime logic
- Readiness
- point generation
- entry-stop-TP-RR
- order API
- execution API
- auto-trading

## 6. Boundary Rules

Display Slots / 默认六币不能作为 batch universe。

Watchlist Pool 才是推送候选边界。

Risk Action Guard 必须位于 delivery 前。

踩踏状态禁止机会推送。

插针不等于趋势反转。

强反转不等于直接反手。

## 7. P263 Decision

P263 authorizes no Java implementation.

It only defines that the next Java step must be split into one safe skeleton path at a time.
