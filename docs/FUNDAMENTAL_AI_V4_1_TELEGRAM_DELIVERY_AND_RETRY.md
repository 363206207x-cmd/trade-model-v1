# Fundamental AI v4.1 Telegram Delivery And Retry

Status: `IMPLEMENTATION_CANDIDATE_PENDING_INDEPENDENT_AUDIT`

## Durable Queue

V14 extends the existing `tm_channel_delivery` owner with due time, claim
lease, response classification, recipient fingerprint, cooldown key, and
severity. It does not add a second queue or Message model.

1. A trusted owner records a Message transactionally.
2. An `AFTER_COMMIT` listener queues the Telegram delivery.
3. The dispatcher reconciles any committed Message missing a delivery fact.
4. A due row is atomically changed to `SENDING`, assigned a claim token, and
   given a lease.
5. The worker rechecks Message expiry before external I/O; an expired alert is
   retained as `SUPPRESSED/MESSAGE_EXPIRED` and is not sent.
6. Success records `SENT`, provider reference, attempt time, and delivery time.
7. Retryable failure records `RETRYING` and a bounded next-attempt time.
8. Terminal failure records `FAILED`; no success fields are fabricated.
9. Expired `SENDING` leases recover to `RETRYING` after process failure.

## Error Classification

| Provider result | State behavior | Retry |
|---|---|---|
| HTTP 200 and `ok=true` | `SENT` | no |
| HTTP 400 | `FAILED/BAD_REQUEST` | no |
| HTTP 401 | `FAILED/AUTH_FAILED` | no |
| HTTP 403 | `FAILED/CHAT_UNAVAILABLE` | no |
| HTTP 429 | `RETRYING/RATE_LIMITED` | use positive `retry_after` |
| HTTP 5xx | `RETRYING/PROVIDER_UNAVAILABLE` | bounded exponential backoff plus deterministic jitter |
| network/timeout | `RETRYING/PROVIDER_UNAVAILABLE` | bounded exponential backoff plus deterministic jitter |
| invalid body or `ok=false` | classified failure | only when the classification is retryable |

Retries stop at the configured maximum. There is no infinite retry loop.
An owner-scoped manual requeue is permitted only for `FAILED` or
`NOT_CONFIGURED` after configuration or provider recovery. The authenticated
message-scoped retry API resolves the existing delivery and never creates or
requalifies a business Message.

## Idempotency And Cooldown

- Database uniqueness prevents more than one active delivery per Message and
  channel.
- Concurrent queue attempts return the existing delivery fact.
- Cooldown suppression retains audit evidence rather than deleting it.
- Severity escalation is allowed because it represents a new business event.
- `SENT` is never reclaimed or resent.

## V14 Historical Compatibility

- Historical `DELIVERED` is normalized to `SENT`.
- Existing `QUEUED` and `RETRYING` rows receive a due time.
- Historical duplicate rows are retained as `SUPPRESSED` with
  `DUPLICATE_MIGRATED`.
- The active `(message_id, channel)` uniqueness rule excludes only those
  immutable migrated duplicate evidence rows.
- V1 through V13 remain unchanged.
