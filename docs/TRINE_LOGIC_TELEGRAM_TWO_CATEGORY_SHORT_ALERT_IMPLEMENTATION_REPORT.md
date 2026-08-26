# TRINE LOGIC Telegram Two-Category Short-Alert Implementation Report

## Scope

- Authorized package: `FUNDAMENTAL_AI_V4_1_TELEGRAM_HIGH_VALUE_ALERT_CHANNEL_INTEGRATION`
- Starting merged main: `cf76ad1b2288e815c4d853dcfbb69a8da9c33e44`
- Implementation issue: `#1200`
- Implementation branch: `codex/telegram-two-category-short-alert-remediation`
- Prior PR `#1197`: closed without merge; its remote branch and commits were preserved.
- Capability movement: none until this candidate is reviewed and merged to `main`.

The implementation reuses the existing `Message -> ChannelDelivery -> Telegram` ownership chain. It does not create another Message, Delivery, Push, Position, or Plan owner.

## Product Contract Mapping

| Contract | Implementation |
| --- | --- |
| Three in-application Message categories remain | Opportunity, plan-safety, and position Message creation remain available. |
| Telegram first release has two categories | Only validated CONFIRMATION Final plans and material active-position changes enter the Delivery allowlist. |
| Plan safety remains in application only | Safety TG1 facts remain; Listener, queue, orphan, requeue, and Dispatcher reject Telegram Delivery. |
| REDUCED is not Telegram eligible | Eligibility is CONFIRMATION-only; the legacy configuration cannot bypass it. |
| PushSnapshot is optional | A real snapshot is used when present and fresh; otherwise the Message source is the real Final plan and no recheck link is generated. |
| Position Message and Telegram eligibility are separate | Broader trusted in-application position facts remain; only the nine canonical changes can produce Delivery. |
| Candidate and position facts remain distinct | Plans never create UserPosition, and Telegram creates neither plan nor position facts. |

## Data Source Mapping

### Executable Final Plan

- Identity: persisted `ExecutionPlanDO.planId`
- Opportunity: persisted transition/log relation to the same `opportunityId`
- Analysis and trace: `AnalysisRunDO` must match the Final plan
- Mode and lifecycle: `CONFIRMATION` and `CURRENT`
- Entry: `ExecutionPlanDO.entryZone`
- Trigger: `ExecutionPlanDO.triggerCondition`
- Stop: `ExecutionPlanDO.stopLoss`
- Target: `takeProfitRules`, then `targetLogic`
- Expiry: `validUntil`, then a real fresh `PushSnapshot.expiresAt`
- Missing required values: fail closed; no placeholder or derived price

### Active Position Attention

- Position: user-owned `UserPositionDO` in `OPEN` or `PARTIALLY_CLOSED`
- Entry, stop, target: actual UserPosition fields only
- Current price: the same VERIFIED and FRESH monitor result/log source
- Risk and reversal: the same monitor result, without Telegram-side calculation
- Stop/target events: require the matching actual UserPosition stop/target
- Risk/reversal events without stop/target: render `未设置`, never a fabricated value

The fixed one-result priority is:

`STOP_LOSS_BREACHED > RISK_EXTREME > TAKE_PROFIT_REACHED > NEAR_STOP_LOSS > RISK_SHARPLY_INCREASED > RISK_HIGH > RISK_INCREASED > NEAR_TAKE_PROFIT > STRONG_REVERSAL`.

## Delivery Safety

- Shared positive allowlist checks category, TG1 event/state, safety flags, source IDs, trace, expiry, exact business-subject fingerprint, and exact short-template shape.
- Creation is guarded in the after-commit Listener and `ChannelDeliveryService`.
- Orphan reconciliation selects only the two canonical short-message shapes.
- Manual requeue cannot reactivate an ineligible Message.
- Dispatcher suppresses legacy/ineligible rows with `TELEGRAM_CATEGORY_NOT_ELIGIBLE` before any Bot API call.
- Message TG1 ownership is unchanged.
- Delivery cooldown identity is `user + canonical object + Telegram category + concrete change`.
- Final plans cool down by `FINAL_PLAN + planId`; positions by `USER_POSITION + positionId + change`.
- The three existing Telegram switches remain unchanged and default off.
- Real Telegram send attempts: `0`.

## Validation

| Check | Result |
| --- | --- |
| Focused Telegram/Delivery/H2 mapper tests | 58 passed, 0 failed, 0 skipped |
| Java 17 full Maven | 4,829 run; 4,815 passed; 0 failures; 0 errors; 14 skipped |
| Product Source Gate | PASS |
| Workflow Contract | PASS on clean implementation commit |
| Telegram authorization validator | PASS |
| Exact package preflight on clean merged main | `IMPLEMENTATION_ALLOWED=true`, `PR_CREATION_ALLOWED=true` |
| `git diff --check` | PASS |

The authorization validator retains its docs-only allowlist before authorization merge. Once that authorization document exists in the merged baseline, it permits only the exact implementation and test files listed for this package; every other file still fails closed.

## Boundaries

- Database schema changed: no
- API contract changed: no
- Message schema changed: no
- Monitor enums changed: no
- Market/OHLCV/provider code changed: no
- Home/Figma/Mobile changed: no
- Login or account behavior changed: no
- Scheduler or production guard changed: no
- New Telegram switch: no
- Staging or production deployment: not executed
- Real Telegram message: not sent
- Automated order/open/close/reduce/reverse capability: zero

## Effective Status

This is an implementation candidate. It is not an effective completed capability until exact-head CI passes, review is complete, and the candidate is merged to `main`.
