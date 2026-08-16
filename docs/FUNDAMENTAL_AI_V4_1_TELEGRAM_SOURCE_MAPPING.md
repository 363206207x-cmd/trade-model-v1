# Fundamental AI v4.1 Telegram High-Value Alert Source Mapping

Status: `IMPLEMENTATION_CANDIDATE_PENDING_INDEPENDENT_AUDIT`

Canonical product source:
`docs/product-sources/FUNDAMENTAL_AI_V4_1_DECISION_CHAIN.md`

Authorization baseline: merged main
`2787f2e999f7744f0bb3e032b0462c9ddea943e4`.

Implementation baseline after authorization merge:
`21ab98ad4155f2bc5f7792d2290d00f8481b00db`.

This record maps the frozen Message and Telegram contract to existing owners.
It does not implement delivery and does not redefine the canonical Product
Source.

## Contract Mapping

| Product requirement | Canonical source | Existing owner | Authorized extension | Forbidden shortcut |
|---|---|---|---|---|
| Message is the sole business fact | Section 15.2 | `MessageDO`, `MessageFactService`, `MessageMapper`, `tm_message` | Persist qualified high-value facts with exact identity, trace, expiry and safety flags | No second Message owner and no channel-first write |
| Telegram is a channel | Section 15.2 | `ChannelDeliveryDO`, `ChannelDeliveryService`, `ChannelDeliveryMapper`, `tm_channel_delivery` | Add one Telegram client, durable dispatch, status, retry, dedupe and readiness | No business Service may call Telegram directly |
| High-permission opportunity | Sections 3, 6, 9, 11, 12 and 15.2 | Asset Pool, Opportunity, Rule Validation, FinalExecutionPlan, PushSnapshot | Queue only validated `CONFIRMATION` or configured high-quality `REDUCED` after PushSnapshot | No Preview, Candidate-only, OBSERVATION, stale or blocked directional push |
| Opportunity/plan safety change | Sections 6, 9, 11, 12 and 15.2 | Opportunity state owner, Final plan lifecycle, Hot Reset, Push Recheck | Persist Confused, invalidation, veto, drift, expiry, source/data failure and revalidation messages | No fabricated state and no Push Recheck trading authority |
| Position logic/risk change | Sections 13 and 15.2 | UserPosition, PositionMonitorLog, monitor trust gate | Persist only VERIFIED + FRESH weakened/invalidated/strong-reversal/material-risk events | No unverified monitor alert and no automatic position mutation |
| User notification configuration | Sections 15.2 and 15.4 | PersonalUser, UserConfig, existing settings/security boundary | Secret-safe readiness and owner-scoped status projection | No token/chat ID response or repository persistence |

## State and Interaction Mapping

- The only eligible category identifiers are `HIGH_PERMISSION_OPPORTUNITY`,
  `OPPORTUNITY_PLAN_SAFETY_CHANGE` and `POSITION_LOGIC_RISK_CHANGE`.
- Message is committed before ChannelDelivery is queued.
- Telegram failure never rolls back or deletes Message.
- Opportunity links return only to the existing Push Recheck identity.
- Position links return only to the authenticated Position Detail identity.
- Missing public HTTPS configuration produces text-only delivery.
- Telegram connectivity test is diagnostic only and creates no business fact.

## Stop-Rule Classification

| Gap | Class | Direct impact | Blocks implementation |
|---|---|---|---|
| Exact package is not machine-authorized | `NEXT_PRODUCT_STAGE_BLOCKER` | Repository edits and PR creation are correctly false for the requested package | YES until this authorization is effective on merged main |
| Existing delivery has no client/worker/retry/readiness | `BUILD_OR_RUNTIME_BLOCKER` | Persisted Telegram delivery cannot reach or truthfully report the provider | YES for channel integration acceptance |
| Existing Message owners are not wired to the three trusted business events | `PRODUCT_SEMANTIC_BLOCKER` | The frozen high-value categories cannot produce canonical persisted facts | YES for channel integration acceptance |

## Invariants

- `MESSAGE_OWNER_COUNT=1`
- `CHANNEL_DELIVERY_OWNER_COUNT=1`
- `PREVIEW_TELEGRAM_ALLOWED=false`
- `CANDIDATE_WITHOUT_FINAL_TELEGRAM_ALLOWED=false`
- `UNVERIFIED_POSITION_TELEGRAM_ALLOWED=false`
- `TELEGRAM_SECRET_REPOSITORY_WRITE_ALLOWED=false`
- `AUTOMATIC_TRADING_ALLOWED=false`

## Implemented Mapping

- `HighValueAlertMessageService` maps trusted Opportunity/Final/PushSnapshot,
  safety-change, and PositionMonitor owners into the three canonical Message
  categories.
- `MessageRecordedEvent` and `TelegramMessageCommitListener` preserve the
  commit-before-channel boundary.
- `ChannelDeliveryService` remains the delivery-fact owner; V14 adds durable
  Telegram queue semantics without creating another queue.
- `TelegramDeliveryDispatcher` is the only runtime owner allowed to invoke the
  single `TelegramClient`.
- `TelegramNotificationController` exposes only authenticated owner-scoped,
  secret-free status.
