# P261 Delivery Wiring Scope Gate

## 1. Gate Purpose

This document defines the future delivery wiring scope gate after P260.

P261 does not implement delivery wiring.

P261 only records what must be true before any future delivery wiring work can start.

## 2. Future Allowed Direction After Separate Authorization

Future wiring may connect internal review-only output to one of these internal-only surfaces only after separate authorization:

- local queue
- audit-only envelope

The future surface must remain review-only, fail-closed, and non-trading.

The future surface must not send messages.

The future surface must not connect any external provider.

## 3. Still Out Of Scope

P261 does not authorize:

- production delivery wiring
- external provider delivery
- Telegram
- email
- webhook
- app notification
- local notification
- message sending
- scheduler activation
- API wiring
- dashboard wiring
- runtime / live / external data read
- real scan loop
- external Opportunity Push execution
- Promote To Home runtime logic
- Readiness upgrade
- point generation
- entry / stop / TP / RR
- order API
- execution API
- auto-trading

## 4. Required Safety Boundary

Any future delivery wiring must preserve:

- disabled-by-default behavior until explicitly authorized
- no external provider dependency
- no network call
- no message sending
- review-only / fail-closed output
- audit trail before wider exposure
- idempotency before repeated delivery
- throttling before any repeated delivery surface
- no trade instruction
- manual review required

## 5. Candidate Boundary

Display Slots / 默认六币不能作为 batch universe。

Default six symbols must not become the push universe.

Watchlist Pool 才是推送候选边界。

Any future delivery wiring must start after Watchlist Pool boundary checks and before any external channel work.

Risk Action Guard 必须位于 delivery 前。

踩踏状态禁止机会推送。

插针不等于趋势反转。

强反转不等于直接反手。
