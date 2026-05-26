# P260 Push Channel Disabled No-Op Skeleton Verification

## 1. 阶段目标

P260 只新增 future Opportunity Push delivery/channel handling 的 disabled-by-default / no-op Java skeleton。

P260 不实现 external push execution，不连接 Telegram / email / webhook / app notification / local notification，不发送任何消息，不接 scheduler / API / dashboard，不读取 runtime / live / external data，不升级 Readiness，不生成 point generation，不生成 entry / stop / TP / RR，不接 order / execution / auto-trading。

## 2. 本轮新增文件

- `src/main/java/org/example/trademodel/dto/watchlistscan/OpportunityPushDeliveryDecisionDTO.java`
- `src/main/java/org/example/trademodel/dto/watchlistscan/OpportunityPushDeliveryDecisionStatusEnum.java`
- `src/main/java/org/example/trademodel/service/watchlistscan/OpportunityPushDeliveryPolicy.java`
- `src/main/java/org/example/trademodel/service/watchlistscan/NoOpOpportunityPushDeliveryPolicy.java`
- `src/test/java/org/example/trademodel/service/watchlistscan/NoOpOpportunityPushDeliveryPolicyTest.java`

## 3. 安全语义

- disabled-by-default / no-op skeleton only
- no network call
- no external provider dependency
- no message sending
- no scheduler activation
- no API / dashboard wiring
- no runtime / live / external data read
- no external Opportunity Push execution
- no Promote To Home runtime logic
- no Readiness
- no point generation
- no entry / stop / TP / RR
- no order / execution / auto-trading
- can use `OpportunityPushDTO` as input
- policy never performs push delivery
- all outputs remain review-only / fail-closed
- `manualReviewRequired=true`
- `notTradeInstruction=true`
- `externalPushSent=false`
- `deliveryAttempted=false`
- `deliveryEnabled=false`
- `readinessUpgraded=false`
- `tradingActionCreated=false`
- `entryStopTpRrGenerated=false`

## 4. Boundary Rules

- Missing / null / unsafe `OpportunityPushDTO` fails closed.
- Non-review-only `OpportunityPushDTO` remains disabled.
- Risk Action Guard blockers block delivery eligibility.
- stampede / extreme stress blocks delivery eligibility.
- liquidity deterioration blocks delivery semantics.
- wick-only / pin-bar direct reversal blocks trend-reversal delivery semantics.
- safe review-only `OpportunityPushDTO` can produce only no-op review-only delivery decision.
- Display Slots / 默认六币不能作为 batch universe。
- Watchlist Pool 才是推送候选边界。
- Risk Action Guard 必须位于 delivery 前。
- 踩踏状态禁止机会推送。
- 插针不等于趋势反转。
- 强反转不等于直接反手。

## 5. 测试覆盖

`NoOpOpportunityPushDeliveryPolicyTest` 覆盖：

- null input fails closed
- blank symbol fails closed if symbol is separately provided
- missing Opportunity Push fails closed
- unsafe Opportunity Push fails closed
- non-review-only Opportunity Push remains disabled
- stampede / extreme stress blocks delivery eligibility
- liquidity deterioration blocks delivery semantics
- wick-only / pin-bar direct reversal reason blocks trend-reversal delivery semantics
- safe review-only Opportunity Push can produce only no-op review-only delivery decision
- every output keeps `manualReviewRequired=true` and `notTradeInstruction=true`
- every output keeps `externalPushSent=false`, `deliveryAttempted=false`, `deliveryEnabled=false`, `readinessUpgraded=false`, `tradingActionCreated=false`, `entryStopTpRrGenerated=false`
- DTO defensive copy
- enum names expose no BUY / SELL / LONG / SHORT / READY / EXECUTABLE / SENT / TRADE / ORDER / ENTRY / STOP / TAKE_PROFIT surface
- implementation has no controller / scheduler / MarketQuoteClient / BinanceMarketQuoteClient / webhook / Telegram / email / app notification / local notification / order / execution / auto-trading dependency
- method names expose no send / notify / deliverNow / execute / trade / order surface

## 6. 验证命令

```bash
./mvnw -q -Dtest=NoOpOpportunityPushDeliveryPolicyTest test
./mvnw -q -DskipTests compile
./mvnw -q -DskipTests test-compile
mvn -B verify -Pci
git diff --name-status main...HEAD
git diff --check
git status
```

## 7. 当前结论

P260 是 disabled no-op delivery policy skeleton。

P260 不是 external push delivery，不是真实推送通道，不是 Readiness，不是 point generation，不是交易建议。
