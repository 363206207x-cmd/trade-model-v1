# P206 观察库运行时数据源审计

## 1. 未来扫描宇宙

未来 Low-Frequency Scan（低频扫描）的 scan universe（扫描全集）必须来自 Watchlist Pool（观察库池）。

- Watchlist Pool（观察库池）是候选最大边界，但不是交易候选全集。
- Display Slots（首页展示位）只是首页展示优先级，不是 scan universe（扫描全集）。
- 默认六币不是 scan universe（扫描全集）。
- 非观察库资产必须 fail-closed（失败关闭）。
- Display Slots 中但不在 Watchlist Pool 的资产，不能进入低频扫描候选。
- 不允许恢复默认六币固定扫描或默认六币固定推送。
- 不允许把首页展示资产直接等同于观察库资产。

这一步只定义未来边界，不读取数据库，不读取接口，不读取运行时行情。

## 2. 未来 Runtime Snapshot 字段候选

未来如果定义 Watchlist runtime snapshot（观察库运行时快照），可以考虑这些字段，但 P206 只停留在文档层：

- symbol：交易对。
- watchlistEnabled：是否在 Watchlist Pool（观察库池）中启用。
- source：数据来源。
- lastUpdatedAt：最后更新时间。
- dataQualityStatus：数据质量状态。
- marketSnapshotStatus：行情快照状态。
- missingFields：缺失字段。
- staleFields：过期字段。
- blockingReasons：阻断原因。
- manualReviewRequired：是否需要人工复核，默认应为 true。
- notTradeInstruction：是否不是交易指令，默认应为 true。

这些字段不是 Java DTO（数据传输对象），不是数据库结构，也不是 API 返回体。本轮不实现 runtime snapshot Java（运行时快照 Java 类）。

## 3. 缺失 / 过期 / 部分数据规则

- missing watchlist membership（缺失观察库成员关系）：fail-closed（失败关闭）。
- stale market snapshot（过期行情快照）：只能 REVIEW_ONLY（只允许复核）或 INCOMPLETE（信息不完整）。
- missing core market fields（缺失核心行情字段）：必须 INCOMPLETE（信息不完整）。
- partial evidence（部分证据）：不得生成 ScanScore（扫描分数）。
- unknown source（未知来源）：不得进入 Candidate Attention（候选关注）。
- data quality unknown（数据质量未知）：不得 Promote To Home（提升到首页观察）。
- risk state unknown（风险状态未知）：只能 Review-Only（只允许复核），不能 opportunity promote execution（机会提升执行）。

## 4. 禁止实现

P206 不做任何运行时代码实现：

- 不读取 DB（数据库）。
- 不读取 API（接口）。
- 不接 MarketQuoteClient（行情客户端）。
- 不接 mapper（映射器）。
- 不接 service（服务）。
- 不改 scheduler behavior（定时器行为）。
- 不读取 live data（实时数据）。
- 不实现 runtime snapshot Java（运行时快照 Java 类）。
- 不创建 Watchlist Runtime Data Source（观察库运行时数据源）。
- 不创建真实 Low-Frequency Scan（低频扫描）。
- 不创建 Opportunity Push execution（机会推送执行）。
- 不生成真实 entry / stop / TP / RR（入场 / 止损 / 止盈 / 盈亏比）。
- 不升级 Readiness（可执行就绪）。
- 不接 auto-trading（自动交易）。

