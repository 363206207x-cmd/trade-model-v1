# TRINE LOGIC Telegram Two-Category Remediation Source Mapping

Status: `AUTHORIZATION_PENDING_MERGED_MAIN`

Canonical source:
`docs/product-sources/FUNDAMENTAL_AI_V4_1_DECISION_CHAIN.md`, Section 15.2.

Exact implementation package:
`FUNDAMENTAL_AI_V4_1_TELEGRAM_HIGH_VALUE_ALERT_CHANNEL_INTEGRATION`

| Frozen fact | Existing owner | First-release Telegram rule | Missing behavior authorized later |
|---|---|---|---|
| Three in-app Message categories remain | `MessageFactService` / `tm_message` | Preserve all three | None in this authorization |
| Final opportunity | FinalExecutionPlan after Rule Validation | `CONFIRMATION` only; `REDUCED` suppressed; real required fields and stop loss; optional existing PushSnapshot | Bounded policy/template wiring |
| Opportunity/plan safety change | Existing Opportunity/Final lifecycle owners | In-app Message only; Telegram Delivery count must be zero | Explicit channel suppression |
| Position material change | UserPosition + trusted PositionMonitorLog | Only `OPEN`/`PARTIALLY_CLOSED`, `VERIFIED + FRESH`, using existing risk/trend/stop/target/strong-reversal facts | Bounded policy/template wiring |
| Channel persistence and dispatch | `ChannelDeliveryService` / `tm_channel_delivery` / existing Telegram dispatcher | Reuse; cooldown uses user + stable object + category + concrete change | Focused cooldown-key remediation |
| Safety copy | Existing Message safety attributes | Remove fixed visible disclaimer only; retain internal no-trade/no-order properties | Bounded presentation remediation |

No runtime field is invented by this authorization. Missing Final or trusted
position-monitor facts fail closed. Section 15.2 remains unchanged.
