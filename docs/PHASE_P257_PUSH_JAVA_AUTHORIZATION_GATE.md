# P257 Push Java Authorization Gate

## 1. 阶段定位

P257 是 Push Java authorization gate。

P257 不写 Java。

## 2. P258 可考虑内容

P258 可考虑：

- `OpportunityPushDTO`
- `OpportunityPushStatusEnum`
- `OpportunityPushRule`
- `DefaultOpportunityPushRule`
- targeted test
- 只能 review-only
- 不能真正发送外部消息
- 不能接 Telegram / email / webhook / app notification
- 不能 readiness
- 不能 point generation
- 不能 trading action

## 3. P258 必须保持

- `manualReviewRequired=true`
- `notTradeInstruction=true`
- `externalPushSent=false`
- `readinessUpgraded=false`
- `tradingActionCreated=false`
- `entryStopTpRrGenerated=false`
- Risk Action Guard placeholder fail-closed
- stampede / liquidity reasons must block push eligibility

## 4. 结论

P258 可以进入 Opportunity Push review-only skeleton。

P258 不是外部推送，不是 readiness，不是交易指令。
