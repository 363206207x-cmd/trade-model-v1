# Fundamental AI v4.1 Telegram High-Value Alert Authorization

Authorization status: `AUTHORIZED_PENDING_MERGED_MAIN`

Exact authorized package:
`FUNDAMENTAL_AI_V4_1_TELEGRAM_HIGH_VALUE_ALERT_CHANNEL_INTEGRATION`

Implementation branch:
`codex/v4-1-telegram-high-value-alert-channel`

Risk: `B_RISK_EXTERNAL_NOTIFICATION_CHANNEL`

Implementation status: `NOT_STARTED`

Source date: `2026-08-16`

Tracking issue: `#1188`

## Authority and Effectivity

This authorization is subordinate to
`docs/product-sources/FUNDAMENTAL_AI_V4_1_DECISION_CHAIN.md`, especially
Section 15.2. It becomes effective only after this exact authorization is
merged to clean, synchronized main and its Product Source, Workflow,
authorization, Duplicate Skeleton and test gates pass.

## Exact Allowed Scope

The successor may:

1. extend the existing Message and ChannelDelivery owners;
2. implement one Telegram Bot API client and one ChannelDelivery dispatcher;
3. add qualification for the three frozen high-value categories;
4. add durable dedupe, cooldown, escalation, claim, retry and crash recovery;
5. add secret-safe Telegram properties, readiness and explicit preflight;
6. add owner-scoped read-only status and, only if secure, a protected test action;
7. add public-HTTPS-safe Push Recheck and Position Detail links;
8. add necessary tests, environment examples and deployment/audit docs;
9. add sequential migration V14 only when current V13 storage cannot safely
   express unique channel delivery, due retry and crash recovery;
10. use mock Telegram HTTP only during implementation and tests;
11. create one Draft implementation PR and stop for independent audit.

## Live Secret Boundary

```text
TELEGRAM_DIRECT_CONNECTIVITY=PASS_USER_VERIFIED
TELEGRAM_LIVE_SECRET_REQUIRED_FOR_IMPLEMENTATION=false
TELEGRAM_SECRET_FILE_READ_ALLOWED=false
TELEGRAM_SECRET_REPOSITORY_WRITE_ALLOWED=false
TELEGRAM_APPLICATION_LIVE_ACCEPTANCE_DEFERRED=true
```

The private file under the user's home directory must not be read, copied,
printed, logged, hashed, committed or used by this implementation task.

## Forbidden Scope

- no Product Source, Figma, Desktop or Mobile redesign;
- no second Message, Push, Position Alert, Opportunity or Position owner;
- no Preview, Candidate-only, ordinary-price or unverified-monitor delivery;
- no token/chat ID in Git, logs, database, API, screenshot or error text;
- no market, AI, Data Quality, opportunity, plan or monitoring algorithm change;
- no automatic open, close, add, reduce, reverse, order or trade;
- no live Telegram application smoke before audited code is merged;
- no production deployment or next product package.

## Machine Permission Contract

Before merge all successor permissions are false. After clean/synchronized
merged-main effectivity, only the exact package resolves to:

```text
REQUESTED_PACKAGE: FUNDAMENTAL_AI_V4_1_TELEGRAM_HIGH_VALUE_ALERT_CHANNEL_INTEGRATION
REQUEST_CLASS: AUTHORIZED_IMPLEMENTATION_PACKAGE
REPOSITORY_EDITS_ALLOWED: true
IMPLEMENTATION_ALLOWED: true
PR_CREATION_ALLOWED: true
CANONICAL_FIGMA_DESKTOP_IMPLEMENTATION_ALLOWED: false
MOBILE_IMPLEMENTATION_ALLOWED: false
```

Typos, expanded packages, Figma, Mobile and automatic-trading packages fail
closed.

## Capability Boundary

- `CAPABILITY_MOVEMENT=NONE`
- `TELEGRAM_APPLICATION_INTEGRATION_STATUS=NOT_STARTED`
- `TELEGRAM_LIVE_APPLICATION_ACCEPTANCE=DEFERRED_UNTIL_MERGED_MAIN`
- `PRODUCTION_DEPLOYMENT_READINESS=BLOCKED`

