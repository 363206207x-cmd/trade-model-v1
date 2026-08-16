# Fundamental AI v4.1 Telegram Ownership Map

Status: `AUTHORIZATION_CANDIDATE`

Exact successor package:
`FUNDAMENTAL_AI_V4_1_TELEGRAM_HIGH_VALUE_ALERT_CHANNEL_INTEGRATION`

## Telegram Integration Ownership Matrix

| Capability | Existing owner | Current state | Required extension | Duplicate forbidden |
|---|---|---|---|---|
| Business message fact | `MessageDO` / `MessageFactService` / `tm_message` | Persisted categories, identity, dedupe and safety flags exist | Trusted event producers and post-commit channel handoff | Second Message entity/service/table |
| Message read model | Existing Message Center and Message Push read owners | In-app views exist | Include canonical Message and channel delivery state without exposing secrets | Second Message page/read stack |
| Channel delivery fact | `ChannelDeliveryDO` / `ChannelDeliveryService` / `tm_channel_delivery` | Basic queued/suppressed records exist | Idempotent Telegram queue, claim, retry, recovery and terminal states | Second channel-delivery table or queue owner |
| Push/Recheck | `TmPushSnapshotDO`, `PushSnapshotService`, `PushRecheckService` | Existing snapshot and recheck identity | Link qualified opportunity messages to the existing snapshot/recheck | Second Push or generic revalidation owner |
| Opportunity and Final | Existing Opportunity, Rule Validation and FinalExecutionPlan owners | Frozen decision chain is effective on main | Read qualification facts after Final validation only | Candidate-as-Final or Telegram-owned opportunity logic |
| Position alert source | `UserPosition`, `PositionMonitorLog`, monitor trust gate | VERIFIED/FRESH contract exists | Emit canonical high-value change after trusted persistence | Second position-alert model or UI-derived alert |
| User and settings | `PersonalUser`, `UserConfig`, authenticated settings owner | Telegram binding placeholders exist | Secret-safe configured/readiness projection | Token or chat ID in user-facing API |
| Provider client | None | Not implemented | One `TelegramClient` owner for `getMe` and `sendMessage` only | Direct HTTP calls from business services |
| Dispatch runtime | Existing ChannelDelivery owner | Not implemented | One dispatcher/worker under ChannelDelivery | Second scheduler or business-event queue |
| Runtime preflight | Existing `TargetRuntimePreflight` | Telegram absent | Extend with set/missing and ready/not-configured/blocked states | Parallel deployment/preflight system |

## Call Boundary

```text
Trusted business owner
  -> MessageFactService
  -> committed tm_message
  -> ChannelDeliveryService
  -> tm_channel_delivery
  -> TelegramDeliveryDispatcher
  -> TelegramClient
```

Only the final two owners may depend on the Telegram protocol package.

## Duplicate Skeleton Gate

- Creates a second Message/Push/Position/Opportunity owner: NO.
- Reuses existing owners: YES.
- Adds one external-provider client owner: YES, because none exists.
- Increases duplicate skeleton surface: NO.
- Capability movement in this authorization package: NONE.
- Compliance with the #830 freeze recommendation: PASS.

