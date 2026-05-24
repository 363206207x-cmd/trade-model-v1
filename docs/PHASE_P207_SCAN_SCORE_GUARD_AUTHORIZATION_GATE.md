# P207 ScanScore Guard 授权门

## 1. 阶段定位

P207 只定义 ScanScore guard（扫描分数保护器）的授权边界。

P207 不实现 ScanScore（扫描分数）。

P207 不计算生产分数。

本文件只约束未来如果进入 P208 / P209，应先做 guard / DTO / tests（保护器 / 数据传输对象 / 测试），不得直接进入真实评分或排序。

## 2. 未来 guard 允许做什么

未来 ScanScore guard（扫描分数保护器）可以在以下情况将 score（分数）保持为 `INCOMPLETE` / `REVIEW_ONLY`：

- 非观察库。
- 成员关系未知。
- 数据过期。
- 核心字段缺失。
- 数据质量低。
- 踩踏。
- 插针未确认。
- source trace incomplete（证据来源追踪不完整）。
- 多周期冲突。

未来 guard 可以定义 blocking reasons（阻断原因）。

未来 guard 可以定义 no-score result（无分数结果）。

未来 guard 可以定义 `notTradeInstruction=true` 和 `manualReviewRequired=true` 的安全默认值。

## 3. 未来 guard 不允许做什么

未来 ScanScore guard（扫描分数保护器）不允许：

- 不计算真实分数。
- 不排序真实资产。
- 不创建 Candidate Attention（候选关注）。
- 不创建 Promote To Home（提升到首页观察）。
- 不创建 Opportunity Push execution（机会推送执行）。
- 不生成 entry / stop / TP / RR（入场 / 止损 / 止盈 / 盈亏比）。
- 不升级 readiness（可执行就绪）。
- 不创建 trading action（交易动作）。
- 不接 MarketQuoteClient（行情客户端）。
- 不读 runtime data（运行时数据）。

## 4. 结论

P208 如进入实现，也应先做 guard / DTO / tests（保护器 / 数据传输对象 / 测试），不做真实 score（真实分数）。
