# TRINE LOGIC Telegram Two-Category Remediation Authorization

Authorization status: `AUTHORIZED_PENDING_MERGED_MAIN`

Exact authorized implementation package:
`FUNDAMENTAL_AI_V4_1_TELEGRAM_HIGH_VALUE_ALERT_CHANNEL_INTEGRATION`

Authorization branch:
`codex/telegram-two-category-remediation-authorization`

Authorized successor branch:
`codex/frontend-interaction-runtime-closure`

Change type: `DOCS_GATE_ONLY`

Capability movement: `NONE`

## Authority

This record is subordinate to the sole v4.1 Product Source,
`docs/product-sources/FUNDAMENTAL_AI_V4_1_DECISION_CHAIN.md`, including the
frozen Section 15.2 capability ceiling. It does not edit or supersede Section
15.2.

The canonical three in-application Message categories remain available:

1. high-permission Final opportunity;
2. material Opportunity or plan safety change;
3. material active-position logic or risk change.

For the Owner's first release, Telegram ChannelDelivery is narrower than that
ceiling. Only these two categories may be connected by the successor:

1. a short alert for a validated `CONFIRMATION` FinalExecutionPlan;
2. a short alert for a material active-position risk, stop-loss/take-profit,
   or `STRONG_REVERSAL` change.

Opportunity or plan safety changes remain canonical in-application Messages
but must not create Telegram ChannelDelivery in this first release. This is a
delivery-scope narrowing, not a product-capability expansion or a rewrite of
the frozen Message contract.

## Authorized Successor Scope

After this authorization is effective on clean, synchronized merged main, the
exact successor may only:

- reuse the existing `Message -> ChannelDelivery -> Telegram` pipeline;
- keep all three in-application Message categories;
- allow Telegram delivery only for the two Owner-approved first-release
  categories;
- require `CONFIRMATION` for an opportunity alert and suppress `REDUCED`;
- use an existing PushSnapshot when present, without requiring a new one;
- fail closed when required Final plan fields or the real stop loss are
  missing;
- require an active `OPEN` or `PARTIALLY_CLOSED` UserPosition and a
  `VERIFIED + FRESH` PositionMonitor result;
- reuse existing `riskLevel`, `riskTrend`, stop-loss/take-profit result flags,
  and `STRONG_REVERSAL` without changing PositionMonitor enums;
- key delivery cooldown by user, stable business object, category, and the
  concrete material change;
- retain the existing three Telegram switches with defaults off;
- remove the fixed user-visible disclaimer tail while retaining internal
  non-trade and non-order safety properties;
- add only the focused tests and audit evidence needed for this remediation.

## Not Authorized

- no Telegram implementation in this authorization change;
- no Telegram policy, template, allowlist, cooldown, or dedupe code here;
- no automatic position scheduler;
- no Telegram switch activation or real Telegram send;
- no Bot Token or Chat ID read, write, logging, hashing, or output;
- no Staging or Production deployment;
- no Home, login, market, OHLCV, CoinGlass, Figma, Mobile, Schema, API, or
  automatic-trading change;
- no automatic open, close, add, reduce, reverse, order, or trade;
- no change to PR #1197 from this authorization branch.

## Machine Permission Contract

Before this authorization is merged, the exact successor must resolve to:

```text
REPOSITORY_EDITS_ALLOWED: false
IMPLEMENTATION_ALLOWED: false
PR_CREATION_ALLOWED: false
```

After clean/synchronized merged-main effectivity, only the exact package may
resolve to:

```text
REQUESTED_PACKAGE: FUNDAMENTAL_AI_V4_1_TELEGRAM_HIGH_VALUE_ALERT_CHANNEL_INTEGRATION
REQUEST_CLASS: AUTHORIZED_IMPLEMENTATION_PACKAGE
REPOSITORY_EDITS_ALLOWED: true
IMPLEMENTATION_ALLOWED: true
PR_CREATION_ALLOWED: true
CANONICAL_FIGMA_DESKTOP_IMPLEMENTATION_ALLOWED: false
MOBILE_IMPLEMENTATION_ALLOWED: false
CANONICAL_FIGMA_FILE_KEY: NONE
```

Typos, expanded scopes, Production, Figma, Mobile, automatic trading, and any
different package fail closed.

## Effectivity

This authorization is not active while its PR is open or Draft. It becomes
effective only after Owner approval, merge to main, clean/synchronized local
main validation, Product Source Gate PASS, and Workflow Contract PASS.
