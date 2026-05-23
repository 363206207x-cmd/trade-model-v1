# P206 观察库扫描结果契约审计

## 1. 未来 Scan Result 合约字段候选

未来 Watchlist ScanResult（观察库扫描结果）可以考虑这些字段，但 P206 只做文档层审计，不创建 Java DTO（数据传输对象）：

- symbol：交易对。
- watchlistMember：是否属于 Watchlist Pool（观察库池）。
- scanStatus：扫描状态。
- scanReason：扫描原因。
- dataQualityStatus：数据质量状态。
- blockingReasons：阻断原因。
- candidateAttentionAllowed：是否允许进入 Candidate Attention（候选关注）。
- promoteToHomeAllowed：是否允许进入 Promote To Home（提升到首页观察）复核。
- opportunityPushAllowed：是否允许 Opportunity Push（机会推送）。
- manualReviewRequired：是否需要人工复核。
- notTradeInstruction：是否不是交易指令。
- entryStopTpRrGenerated：是否生成 entry / stop / TP / RR（入场 / 止损 / 止盈 / 盈亏比）。
- readinessUpgraded：是否升级 Readiness（可执行就绪）。
- tradingActionCreated：是否创建交易动作。

这些字段只是未来契约候选，不是 P206 代码实现。

## 2. 状态枚举建议

未来 scanStatus（扫描状态）可以考虑这些文档层状态：

- DISABLED：扫描关闭。
- BLOCKED_NOT_WATCHLIST：非观察库资产，阻断。
- INCOMPLETE：信息不完整。
- REVIEW_ONLY：只允许复核。
- CANDIDATE_ATTENTION：候选关注。
- PROMOTE_TO_HOME_REVIEW：提升到首页观察复核。
- NOT_IMPLEMENTED：未实现。

这些状态不代表真实扫描器已经存在，也不代表系统具备 opportunity promote execution（机会提升执行）或 opportunity push execution（机会推送执行）。

## 3. 强制默认值

未来任何 ScanResult（扫描结果）在没有进一步授权前，都必须保持这些默认值：

- manualReviewRequired=true。
- notTradeInstruction=true。
- opportunityPushAllowed=false。
- entryStopTpRrGenerated=false。
- readinessUpgraded=false。
- tradingActionCreated=false。

这表示扫描结果只能作为人工复核材料，不是交易建议，不是交易指令，不是执行计划。

## 4. 语义边界

- Candidate Attention（候选关注）不等于交易信号。
- Promote To Home（提升到首页观察）不等于 Opportunity Push execution（机会推送执行）。
- ScanResult（扫描结果）不等于 ExecutionPlan（执行计划）。
- ScanResult 不生成 entry / stop / TP / RR（入场 / 止损 / 止盈 / 盈亏比）。
- ScanResult 不升级 Readiness（可执行就绪）。
- ScanResult 不触发下单。
- ScanResult 不触发平仓。
- ScanResult 不触发反手。
- ScanResult 不触发买入 / 卖出。
- ScanResult 不连接 order API（下单接口）。
- ScanResult 不连接 execution API（执行接口）。
- ScanResult 不连接 auto-trading（自动交易）。

