# Fundamental AI v4.1 Telegram High-Value Alert Contract

Status: `IMPLEMENTATION_CANDIDATE_PENDING_INDEPENDENT_AUDIT`

Exact package:
`FUNDAMENTAL_AI_V4_1_TELEGRAM_HIGH_VALUE_ALERT_CHANNEL_INTEGRATION`

Telegram is an outbound `ChannelDelivery`; it is never a business fact owner.
The canonical chain is:

```text
trusted business state
  -> MessageFactService.recordIfAbsent
  -> committed tm_message
  -> AFTER_COMMIT MessageRecordedEvent
  -> ChannelDeliveryService.queueTelegram
  -> durable tm_channel_delivery
  -> TelegramDeliveryDispatcher
  -> TelegramClient
```

Delivery failure cannot roll back, replace, or delete the canonical Message.

## Allowed Message Categories

| Category | Required qualification | Explicit rejection |
|---|---|---|
| `HIGH_PERMISSION_OPPORTUNITY` | User Asset Pool source, persisted Opportunity, Rule-validated Final plan, eligible Plan Mode, non-blocked Opportunity state, valid data/freshness/source/feasibility gates, traceable IDs, PushSnapshot, and both safety flags | Preview, Candidate-only, unvalidated or expired Final, PREPARATION, OBSERVATION, BLOCKED, confused, cooling, invalidated, high-risk, missing source, missing PushSnapshot |
| `OPPORTUNITY_PLAN_SAFETY_CHANGE` | Traceable trusted owner event for confused/high-confused, liquidity trap, Hot Reset, invalidation, risk veto, execution drift, expiry, data-quality/source failure, or revalidation | UI-derived state, fabricated state, directional opportunity copy while confused, or ordinary price movement |
| `POSITION_LOGIC_RISK_CHANGE` | Active manually entered UserPosition plus VERIFIED and FRESH monitor result plus material logic/reversal/risk/stop/target change | Pending, stale, invalid, untrusted, ordinary movement, closed position, or missing monitor provenance |

No fourth category is introduced. Message safety flags are always
`notTradeInstruction=true` and `notOrderExecution=true`.

## Idempotency And Escalation

- `Message.dedupeKey` is the business dedupe owner.
- `(user, object, event type, state, severity, cooldown window)` identifies one
  event fact.
- `(message_id, TELEGRAM)` identifies one active delivery fact.
- Equivalent events inside cooldown are retained as `SUPPRESSED` with
  `DUPLICATE_OR_COOLDOWN`.
- A higher severity can create a new event inside the same cooldown window.
- Existing rows are never deleted to obtain idempotency.

## Delivery States

`QUEUED`, `SENDING`, `SENT`, `RETRYING`, `FAILED`, `SUPPRESSED`, and
`NOT_CONFIGURED` are the only Telegram delivery states. `SENT` requires a
successful provider response and stored provider message reference.

## Message Content Boundary

Telegram content is a compact human-review alert. It includes identity,
current trusted change, concise evidence/risk, validity or monitoring time,
and an explicit manual action. It does not expose a full execution-plan field
wall, AI prompt, account secret, database information, or automatic command.

## Link Boundary

- Opportunity and safety alerts may link only to `/recheck/{pushSnapshotId}`.
- Position alerts may link only to `/positions/{positionId}`.
- A link is emitted only for a configured public HTTPS same-host base URL.
- Loopback, private, HTTP, cross-host, query-secret, and unrelated paths are
  rejected. Without a safe public URL, delivery remains text-only.

## Status Boundary

Authenticated users may read
`GET /api/settings/notifications/telegram/status`. The projection contains
configuration/readiness and owner-scoped delivery state only. It never returns
the bot token, chat ID, recipient, full request URL, or secret-file path.

After configuration recovery, an authenticated owner may request
`POST /api/settings/notifications/telegram/messages/{messageId}/retry`. It can
only requeue that user's existing `FAILED` or `NOT_CONFIGURED` delivery fact;
it cannot create a Message or bypass qualification.

## Safety Invariants

- `TELEGRAM_CLIENT_OWNER_COUNT=1`
- `DIRECT_BUSINESS_SERVICE_TELEGRAM_HTTP_CALL_COUNT=0`
- `PREVIEW_TELEGRAM_DELIVERY_COUNT=0`
- `CANDIDATE_WITHOUT_FINAL_TELEGRAM_DELIVERY_COUNT=0`
- `UNVERIFIED_POSITION_TELEGRAM_DELIVERY_COUNT=0`
- `AUTOMATIC_OPEN_CLOSE_REVERSE_ORDER_CAPABILITY_COUNT=0`
