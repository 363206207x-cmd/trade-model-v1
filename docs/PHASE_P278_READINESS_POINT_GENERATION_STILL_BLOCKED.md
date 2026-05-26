# P278 Readiness Point Generation Still Blocked

P278 does not authorize Readiness, point generation, or trading path behavior.

Still blocked:

- Readiness upgrade
- point generation
- true entry / stop / TP / RR
- order API
- execution API
- auto-trading
- trade action creation
- direct reverse trading
- automatic close or stop modification

The future DTO must not contain trade action, order, execution, entry, stop, take profit, RR, provider, external channel, message sending, or readiness fields.

The future DTO must default `manualReviewRequired=true` and `notTradeInstruction=true`, and it must remain review-only / fail-closed / audit-only where applicable.

踩踏状态禁止机会推送。

插针不等于趋势反转。

强反转不等于直接反手。
