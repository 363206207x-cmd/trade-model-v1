# P273 External Channel Authorization Scope Gate

## 1. Purpose

This document defines the authorization scope for a future external channel skeleton.

P273 does not implement that skeleton.

## 2. Future External Channel Minimum Scope

A future external channel may only be introduced after separate authorization.

The first allowed shape must be a disabled no-op Java skeleton.

That future skeleton must be:

- disabled-by-default
- fail-closed
- audit-only
- review-only
- no-message-sending
- no-credential
- no-live-provider-call

The future external channel may only represent internal review metadata about whether a channel remains disabled.

It must not become real external channel behavior, provider selection, provider integration, provider credential handling, live provider calls, message rendering, or message sending.

## 3. Blocked Channels

The following remain blocked unless a later separate authorization gate explicitly allows them:

- Telegram
- email
- webhook
- app notification
- local notification
- any provider client
- channel configuration
- channel delivery receipt
- channel retry / backoff

## 4. Required Safety Order

Risk Action Guard must remain before delivery.

Watchlist Pool remains the candidate boundary.

Display Slots / 默认六币不能作为 batch universe。

踩踏状态禁止机会推送。

插针不等于趋势反转。

强反转不等于直接反手。

## 5. P273 Result

P273 authorizes documentation only.

P273 does not authorize external channel implementation.
