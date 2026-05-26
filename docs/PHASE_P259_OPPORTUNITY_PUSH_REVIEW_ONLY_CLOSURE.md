# P259 Opportunity Push Review-Only Closure

## 1. 阶段定位

P259 收口 P258 Opportunity Push Review-Only Skeleton。

P259 不写 Java，不修改测试，不实现 external Opportunity Push execution。

## 2. P258 合并基准

- PR #635
- Issue #634
- merge commit: `1567ba0`
- 标题：BACKEND-P258 Opportunity Push Review-Only Skeleton
- P258 CI passed before merge

## 3. P258 已完成内容

P258 新增：

- `src/main/java/org/example/trademodel/dto/watchlistscan/OpportunityPushDTO.java`
- `src/main/java/org/example/trademodel/dto/watchlistscan/OpportunityPushStatusEnum.java`
- `src/main/java/org/example/trademodel/service/watchlistscan/OpportunityPushRule.java`
- `src/main/java/org/example/trademodel/service/watchlistscan/DefaultOpportunityPushRule.java`
- `src/test/java/org/example/trademodel/service/watchlistscan/DefaultOpportunityPushRuleTest.java`
- `docs/P258.md`
- `docs/PHASE_P258_OPPORTUNITY_PUSH_REVIEW_ONLY_SKELETON_VERIFICATION.md`

## 4. P258 安全语义

P258 只表达 review-only Opportunity Push candidate（只允许人工复核的机会提醒候选）。

P258 所有输出必须保持：

- `manualReviewRequired=true`
- `notTradeInstruction=true`
- `externalPushSent=false`
- `readinessUpgraded=false`
- `tradingActionCreated=false`
- `entryStopTpRrGenerated=false`

P258 `DefaultOpportunityPushRuleTest` 已覆盖 fail-closed、Risk Action Guard blocker、stampede / extreme stress、liquidity deterioration、wick-only / pin-bar direct reversal reason、DTO defensive copy、enum 无交易词面，以及没有 controller / scheduler / MarketQuoteClient / BinanceMarketQuoteClient / webhook / Telegram / email / order / execution / auto-trading dependency。

## 5. P258 不是

P258 不是 external push execution。

P258 不发送 Telegram / email / webhook / app notification / local notification。

P258 不接 API / dashboard / scheduler。

P258 不接 MarketQuoteClient / BinanceMarketQuoteClient。

P258 不读取 runtime / live / external data。

P258 不实现 Promote To Home runtime logic。

P258 不升级 Readiness。

P258 不生成 point generation 或真实 entry / stop / TP / RR。

P258 不接 order API / execution API / auto-trading。

## 6. P259 收口结论

P259 只确认 P258 review-only skeleton 已收口。

后续 external channel、delivery、Readiness、point generation、order / execution / auto-trading 仍必须另开授权门。
