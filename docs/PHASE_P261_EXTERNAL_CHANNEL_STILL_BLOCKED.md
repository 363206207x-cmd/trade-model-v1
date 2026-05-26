# P261 External Channel Still Blocked

## 1. Still Blocked Channels

P261 confirms external providers remain blocked:

- Telegram
- email
- webhook
- app notification
- local notification

P261 does not connect these channels.

P261 does not send messages.

P261 does not add provider dependencies.

P261 does not perform network calls.

## 2. P260 Does Not Change This

P260 added a disabled-by-default / no-op Java skeleton only.

P260 is not external push execution.

P260 is not provider delivery.

P260 is not scheduler / API / dashboard wiring.

P260 is not message sending.

## 3. Future Channel Requirements

Any future external channel must have a separate explicit authorization gate.

Any future external channel must define:

- channel scope
- opt-in / enablement control
- throttling
- idempotency
- audit requirements
- failure handling
- manual review semantics
- no trade instruction semantics
- Risk Action Guard before delivery

P261 documents these requirements only.

## 4. Candidate Boundary

Display Slots / 默认六币不能作为 batch universe。

Watchlist Pool 才是推送候选边界。

踩踏状态禁止机会推送。

插针不等于趋势反转。

强反转不等于直接反手。

## 5. Conclusion

External push channel implementation remains blocked after P261.
