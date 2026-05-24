# P206 低频扫描运行时契约审计

## 1. 阶段定位

P206 是 Low-Frequency Scan Runtime Contract Audit（低频扫描运行时契约审计）。

这一步发生在真实 Low-Frequency Scan（低频扫描）实现之前，只定义审计结论和未来实现约束。本轮不实现运行时代码，不接行情，不创建真实扫描，不生成 ScanScore（扫描分数），不创建 Candidate Attention（候选关注），不创建 Promote To Home（提升到首页观察），也不进入 Opportunity Push（机会推送）执行。

P206 是 A 档 docs-only（只改文档）最大安全任务包，不写 Java，不新增测试，不改 dashboard.html，不改 schema，不改 config，不接 API，不改 service / scheduler implementation（服务 / 定时器实现），不改 mapper（映射器）。

## 2. 前置事实

- P204 已完成 disabled-by-default scheduler skeleton（默认关闭的定时器骨架）。
- P204 的 WatchlistLowFrequencyScanScheduler（观察库低频扫描定时器）默认 disabled（关闭）。
- P204 的 runScheduledScan 默认返回 DISABLED（已关闭），enabled=true 时仍返回 NOT_IMPLEMENTED（未实现）。
- P204 只保留 Review-Only（只允许复核）、not-trade-instruction（不是交易指令）、manual-review-required（需要人工复核）语义。
- P204 只定义未来频率常量，不启用真实扫描。
- P205 已完成 P204 closure（收口），并把最大安全任务包规则写入工作流契约和 Codex 任务模板。
- P204 不是真实扫描器。
- P206 也不是真实扫描器。

## 3. 必须先定义的三类契约

真实低频扫描之前，至少要先拆开三类契约：

1. Watchlist runtime data source contract（观察库运行时数据源契约）
   - 定义未来扫描资产来自哪里。
   - 定义 Watchlist Pool（观察库池）如何成为 scan universe（扫描全集）的最大边界。
   - 定义缺失、过期、部分数据、未知来源时如何 fail-closed（失败关闭）。

2. Watchlist scan result contract（观察库扫描结果契约）
   - 定义未来扫描结果字段。
   - 定义 DISABLED / BLOCKED_NOT_WATCHLIST / INCOMPLETE / REVIEW_ONLY / CANDIDATE_ATTENTION / PROMOTE_TO_HOME_REVIEW / NOT_IMPLEMENTED 等状态语义。
   - 定义默认值必须保持 notTradeInstruction=true、manualReviewRequired=true、opportunityPushAllowed=false。

3. Scan score rule definition（扫描分数规则定义）
   - 定义未来 ScanScore（扫描分数）只是排序 / 关注度辅助分。
   - 定义哪些证据族可以参与文档层候选。
   - 定义哪些情况必须阻断分数。

这三类契约都只是 P206 的文档审计范围，不产生 Java DTO（数据传输对象）、数据库表、接口、扫描器或实时行情读取。

## 4. 安全边界

- Candidate Attention（候选关注）只能是候选关注状态，不是交易信号。
- Promote To Home（提升到首页观察）只能是提升到首页观察 / 人工复核，不是 Opportunity Push execution（机会推送执行）。
- ScanScore（扫描分数）只能是审计层未来规则定义，不是现阶段实现。
- ScanResult（扫描结果）不是 ExecutionPlan（执行计划）。
- ScanResult 不生成真实 entry / stop / TP / RR（入场 / 止损 / 止盈 / 盈亏比）。
- ScanResult 不升级 Readiness（可执行就绪）。
- ScanResult 不触发 order API（下单接口）。
- ScanResult 不触发 execution API（执行接口）。
- ScanResult 不创建自动平仓、自动反手、自动买入、自动卖出。
- 不允许 MarketQuoteClient integration（行情客户端接入）。
- 不允许 runtime / live / external data read（运行时 / 实时 / 外部数据读取）。
- 不允许 opportunity promote execution（机会提升执行）。
- 不允许 opportunity push execution（机会推送执行）。
- 不允许真实 entry / stop / TP / RR。
- 不允许 readiness upgrade（可执行就绪升级）。
- 不允许 auto-trading（自动交易）。

## 5. 下一步建议

P207 才能考虑 Watchlist runtime data source authorization gate（观察库运行时数据源授权门）或 DTO / contract skeleton（数据传输对象 / 契约骨架）。

P207 仍不能直接进入 MarketQuoteClient real scan implementation（行情客户端真实扫描实现）。如果未来要接 MarketQuoteClient（行情客户端）、读取运行时数据、生成 ScanScore（扫描分数）、创建 Candidate Attention（候选关注）或 Promote To Home（提升到首页观察）执行，都必须继续拆分授权门。

