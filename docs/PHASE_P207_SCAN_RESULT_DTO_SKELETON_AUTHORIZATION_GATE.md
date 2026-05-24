# P207 观察库扫描结果 DTO 骨架授权门

## 1. 阶段定位

P207 只定义未来 ScanResult DTO skeleton（扫描结果数据传输对象骨架）的授权边界。

P207 不创建 Java DTO（Java 数据传输对象）。

本文件只说明未来如果进入 P208 / P209，哪些字段、枚举和安全默认值可以作为 pure DTO / enum / tests（纯数据传输对象 / 枚举 / 测试）范围考虑。

## 2. 未来允许的 DTO 字段

未来 WatchlistScanResultDTO（观察库扫描结果数据传输对象）可以考虑以下只读字段：

- symbol。
- watchlistMember。
- scanStatus。
- scanReason。
- dataQualityStatus。
- blockingReasons。
- candidateAttentionAllowed。
- promoteToHomeAllowed。
- opportunityPushAllowed。
- manualReviewRequired。
- notTradeInstruction。
- entryStopTpRrGenerated。
- readinessUpgraded。
- tradingActionCreated。

这些字段只能表达人工复核和阻断状态，不代表真实扫描已经存在。

## 3. 未来允许的 enum

未来 WatchlistScanStatusEnum（观察库扫描状态枚举）可以考虑：

- `DISABLED`。
- `BLOCKED_NOT_WATCHLIST`。
- `INCOMPLETE`。
- `REVIEW_ONLY`。
- `CANDIDATE_ATTENTION`。
- `PROMOTE_TO_HOME_REVIEW`。
- `NOT_IMPLEMENTED`。

这些枚举不能被解释为交易信号、机会推送执行或可执行计划。

## 4. 强制默认值

未来 DTO skeleton（数据传输对象骨架）必须保留以下强制默认值：

- `manualReviewRequired=true`。
- `notTradeInstruction=true`。
- `opportunityPushAllowed=false`。
- `entryStopTpRrGenerated=false`。
- `readinessUpgraded=false`。
- `tradingActionCreated=false`。

这些默认值表示扫描结果只能作为人工复核材料，不是交易建议，不是交易指令，不是执行计划。

## 5. 禁止接线

P207 和未来纯 skeleton（骨架）阶段均禁止：

- 不接 service（服务）。
- 不接 mapper（映射器）。
- 不接 controller（控制器）。
- 不接 scheduler（定时器）。
- 不接 MarketQuoteClient（行情客户端）。
- 不接 DB（数据库）。
- 不接 API（接口）。
- 不接 dashboard（首页）。
- 不读 runtime data（运行时数据）。

## 6. 结论

下一步如实现，只能做 pure DTO / enum / tests（纯数据传输对象 / 枚举 / 测试）。

不能把 DTO skeleton（数据传输对象骨架）当作真实扫描。
