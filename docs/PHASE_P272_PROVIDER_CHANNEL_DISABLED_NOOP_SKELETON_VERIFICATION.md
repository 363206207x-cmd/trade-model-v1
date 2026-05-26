# P272 Provider Channel Disabled No-Op Skeleton Verification

## 1. Purpose

This document verifies that P272 only adds a provider channel disabled no-op Java skeleton.

The skeleton expresses that a future provider/channel remains disabled. It does not select a provider, does not read or use credentials, does not call live providers, does not render messages, and does not send messages.

## 2. Added Java Surface

P272 adds:

- `OpportunityPushProviderChannelDTO`
- `OpportunityPushProviderChannelStatusEnum`
- `OpportunityPushProviderChannelPolicy`
- `NoOpOpportunityPushProviderChannelPolicy`
- `NoOpOpportunityPushProviderChannelPolicyTest`

The policy accepts internal `OpportunityPushMessageEnvelopeDTO` input and returns only a review-only / audit-only provider channel decision object.

## 3. Required No-Op Semantics

Every output remains:

- disabled-by-default
- fail-closed
- audit-only
- review-only
- no-message
- no-provider
- no-credential
- no-live-provider-call

Every output keeps:

- `manualReviewRequired=true`
- `notTradeInstruction=true`
- `auditOnly=true`
- `providerChannelEnabled=false`
- `providerSelected=false`
- `providerCredentialRequired=false`
- `providerCredentialUsed=false`
- `liveProviderCallAttempted=false`
- `messageRendered=false`
- `messageSent=false`
- `externalPushSent=false`
- `deliveryAttempted=false`
- `deliveryEnabled=false`
- `messageEnvelopeCreated=false`
- `messageRenderable=false`
- `messageSendable=false`
- `deliveryPipelineEnabled=false`
- `pipelineStarted=false`
- `queueCreated=false`
- `queued=false`
- `enqueueAttempted=false`
- `dequeueAttempted=false`
- `workerStarted=false`
- `persisted=false`
- `persistenceAttempted=false`
- `readinessUpgraded=false`
- `tradingActionCreated=false`
- `entryStopTpRrGenerated=false`

Null input, missing message envelope input, blank explicitly supplied symbol, unsafe message envelope input, and non-disabled-noop message envelope input all fail closed or remain incomplete / blocked / disabled.

Safe disabled-noop message envelope input may only become `DISABLED_NOOP` provider channel output.

Reasons and blocking reasons are preserved, and list fields use defensive copy behavior.

## 4. Forbidden Surface

P272 does not add:

- controller / endpoint / API
- scheduler
- dashboard wiring
- schema / mapper / repository / DB write
- provider selection
- provider credential handling
- Telegram
- email
- webhook
- app notification
- local notification
- live provider call
- message rendering
- final send text
- message sending
- delivery attempt
- runtime / live / external data read
- Readiness
- point generation
- entry / stop / TP / RR
- order / execution / auto-trading

The targeted test verifies that enum names expose no BUY / SELL / LONG / SHORT / READY / EXECUTABLE / SENT / TRADE / ORDER / ENTRY / STOP / TAKE_PROFIT surface.

The targeted test also verifies that the implementation has no forbidden controller / scheduler / MarketQuoteClient / BinanceMarketQuoteClient / webhook / Telegram / email / app notification / local notification / mapper / repository / DataSource / JdbcTemplate / credential / secret / order / execution / auto-trading dependency surface and exposes no method names such as send, notify, deliverNow, enqueueNow, dequeue, worker, schedule, persistNow, save, insert, update, execute, trade, order, render, provider, credential, or secret.

## 5. Boundary Rules

Display Slots / 默认六币不能作为 batch universe。

Watchlist Pool 才是推送候选边界。

Risk Action Guard 必须位于 delivery 前。

踩踏状态禁止机会推送。

插针不等于趋势反转。

强反转不等于直接反手。

## 6. Verification Commands

```bash
./mvnw -q -Dtest=NoOpOpportunityPushProviderChannelPolicyTest test
./mvnw -q -DskipTests compile
./mvnw -q -DskipTests test-compile
mvn -B verify -Pci
git diff --name-status main...HEAD
git diff --check
git status
```

Docs-only Maven skipping is not used for P272 because P272 adds Java and a targeted test.

## 7. Result

P272 is a disabled no-op provider channel Java skeleton only.

It is not provider selection, not provider integration, not credential handling, not live provider call, not external push execution, not message rendering, not message sending, not scheduler/API/dashboard wiring, not Readiness, not point generation, and not a trading path.
