# TRINE LOGIC Telegram Two-Category Remediation Ownership Map

Status: `AUTHORIZATION_PENDING_MERGED_MAIN`

## Ownership

| Responsibility | Canonical owner | Authorization result |
|---|---|---|
| Business notification fact | Existing Message / `MessageFactService` | Reused; owner count remains one |
| Channel delivery fact | Existing ChannelDelivery / `ChannelDeliveryService` | Reused; owner count remains one |
| Telegram protocol and dispatch | Existing Telegram client and dispatcher | Reused; no second channel stack |
| Final eligibility | Existing FinalExecutionPlan and Rule Validation | Read only; Candidate cannot substitute for Final |
| Opportunity/plan safety Message | Existing Opportunity/Final lifecycle | Message retained; Telegram Delivery suppressed |
| Position eligibility | Existing UserPosition and PositionMonitor trust gate | Read only; no position mutation |
| Cooldown identity | Existing authenticated user and stable business IDs | User + Opportunity/FinalPlan/UserPosition + category + concrete change |
| Safety authority | Existing non-trade/non-order attributes | Retained internally |

## Duplicate Skeleton Gate

- New Message owner: `NO`.
- New ChannelDelivery owner: `NO`.
- New Opportunity, Final, UserPosition, or PositionMonitor owner: `NO`.
- New scheduler or automatic position owner: `NO`.
- Existing owners reused: `YES`.
- Capability movement in this authorization package: `NONE`.
- Compliance with the duplicate-skeleton freeze: `PASS`.
