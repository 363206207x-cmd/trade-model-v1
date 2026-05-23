# P205 Low-Frequency Scan Scheduler Minimal Skeleton Closure（低频扫描定时器最小骨架收口）

## 1. 阶段定位

P205 是 P204 的 closure（收口文档）。

P205 只记录 P204 Low-Frequency Scan Scheduler Minimal Skeleton（低频扫描定时器最小骨架）已经完成了什么、仍然没有完成什么，以及后续继续推进时必须遵守哪些边界。

P205 不实现新功能。本轮不写 Java，不新增测试，不改 `dashboard.html`，不改配置，不改数据库结构，不接接口，不接行情，不接交易。

本轮只删除 `docs/P205.md` placeholder（占位文档），并新增本收口文档。

## 2. P204 合并基准

P204 的合并基准如下：

- PR（合并请求）：#525
- Issue（问题单）：#524
- merge commit（合并提交）：`72724c9`
- 标题：BACKEND-P204 Low-Frequency Scan Scheduler Minimal Skeleton

本轮已按最新事实处理：`main` 已同步到 `72724c9 BACKEND-P204 Low-Frequency Scan Scheduler Minimal Skeleton (#525)`，P204 已合并，WORKFLOW-P1 已合并。

注意：`docs/V1_CURRENT_STATE.md` 仍显示 PR #525 open（未合并），说明该文件已过时。本轮不修改 `docs/V1_CURRENT_STATE.md`，只在 P205 收口中记录最新事实。

## 3. P204 已完成内容

P204 新增了：

- `src/main/java/org/example/trademodel/service/watchlist/WatchlistLowFrequencyScanScheduler.java`
- `src/test/java/org/example/trademodel/service/watchlist/WatchlistLowFrequencyScanSchedulerTest.java`

`WatchlistLowFrequencyScanScheduler` 是 Low-Frequency Scan Scheduler（低频扫描定时器）的最小 skeleton（骨架）。

P204 已完成的能力是：

- scheduler（定时器）默认 disabled（关闭）。
- 无参构造默认 `enabled=false`。
- `runScheduledScan()` 在默认关闭时返回 `DISABLED`。
- `enabled=true` 时仍返回 `NOT_IMPLEMENTED`，不进入真实扫描。
- 结果对象保留 `notTradeInstruction=true`（不是交易指令）。
- 结果对象保留 `manualReviewRequired=true`（必须人工复核）。
- 结果对象保留 review-only（只允许复核）语义。
- 只定义频率常量，不启用真实扫描。

P204 定义的频率常量只是语义，不是运行时扫描策略：

- 普通 Watchlist Pool（观察库池）建议未来 15 分钟一次。
- 异常候选复扫建议未来 5 分钟一次。
- 已提升首页资产建议未来 1-3 分钟复核，但 P204 不实现 Promote To Home（提升到首页观察）。
- Position Monitor（持仓监控）30-60 秒属于持仓监控语义，不属于本轮低频扫描。

## 4. P204 测试确认

`WatchlistLowFrequencyScanSchedulerTest` 已确认：

- 默认 `enabled=false`。
- 调用 scheduler（定时器）方法后返回 `DISABLED`。
- 不调用 market data（市场数据）。
- 不创建 Opportunity Push（机会推送）。
- 不升级 Readiness（可执行就绪）。
- 不创建 trading action（交易动作）。
- 不扫描 Watchlist Pool（观察库池）。
- 不扫描 Display Slots（首页展示位）。
- 不扫描 non-watchlist assets（非观察库资产）。
- 不生成 ScanScore（扫描分数）。
- 不生成 Candidate Attention（候选关注）。
- 不生成 Promote To Home（提升到首页观察）。
- 不生成 entry / stop / TP / RR（入场 / 止损 / 止盈 / 盈亏比）。
- 不依赖 `MarketQuoteClient`（行情客户端）。
- 不依赖 `BinanceMarketQuoteClient`（币安行情客户端）。
- 不依赖 Push service（推送服务）。
- 不依赖 mapper（数据库映射）。

测试还确认：即使使用 `new WatchlistLowFrequencyScanScheduler(true)`，结果也仍是 `NOT_IMPLEMENTED`，并且不会扫描、不会调用行情、不会创建推送、不会升级 readiness（可执行就绪）、不会生成真实点位、不会创建交易动作。

## 5. 明确没有完成

P204 / P205 没有完成以下能力：

- 真实 Low-Frequency Scan（低频扫描）未完成。
- Watchlist runtime data source（观察库运行时数据源）未完成。
- MarketQuoteClient scan integration（行情客户端扫描接入）未完成。
- ScanScore（扫描分数）未完成。
- Candidate Attention（候选关注）未完成。
- Promote To Home execution（提升到首页观察执行）未完成。
- Opportunity Push execution（机会推送执行）未完成。
- entry / stop / TP / RR（入场 / 止损 / 止盈 / 盈亏比）未完成。
- ExecutionPlan readiness（执行计划可执行就绪）未升级。
- order API（下单接口）未接入。
- execution API（执行接口）未接入。
- auto-trading（自动交易）未接入。

P204 只是创建了一个 disabled-by-default（默认关闭）的 scheduler skeleton（定时器骨架）。它不是 Watchlist Scanner（观察库扫描哨兵）的真实执行器。

## 6. 安全边界

P204 只是 disabled-by-default scheduler skeleton（默认关闭的定时器骨架）。

P204 不是 production scanner（生产扫描器）。

P204 不是 trade signal（交易信号）。

P204 不是 Opportunity Push execution（机会推送执行）。

P204 不是 Opportunity Promote execution（机会提升执行）。

P204 不是 Readiness（可执行就绪）。

P204 不是 auto-trading（自动交易）。

P204 不是 default-six scan（默认六币扫描）。

P204 没有把 Display Slots（首页展示位）当作 Watchlist Pool（观察库池）。

P204 没有把 Display Slots（首页展示位）当作 scan universe（扫描全集）。

P204 没有恢复 default-six fixed push（默认六币固定推送）。

非观察库资产仍不得进入候选。

Watchlist Pool（观察库池）仍是未来推送候选的最大边界，但它本身不等于自动交易候选。

Display Slots（首页展示位）仍只是首页展示位，不是扫描全集，不是推送全集，也不是交易候选全集。

任何后续阶段都不能把 P204 的 scheduler skeleton（定时器骨架）解释成已经具备真实扫描、机会提升、机会推送、真实点位、readiness（可执行就绪）或交易执行能力。

## 7. 当前结论

P204 可以视为 Low-Frequency Scan Scheduler Minimal Skeleton（低频扫描定时器最小骨架）闭环。

这个闭环只代表：低频扫描定时器入口已经有一个默认关闭、只允许复核、不是交易指令、不会越界的最小骨架。

这个闭环不代表真实低频扫描完成，不代表行情接入完成，不代表观察库运行时数据源完成，不代表机会推送完成，不代表交易点位完成，不代表 readiness（可执行就绪）完成。

下一步如继续推进真实低频扫描，必须先做以下任一审计：

- Watchlist Runtime Data Source Audit（观察库运行时数据源审计）
- WatchlistScanResult DTO / Contract Audit（观察库扫描结果 DTO / 契约审计）
- Scan Result Contract Audit（扫描结果契约审计）

不能直接接 `MarketQuoteClient`（行情客户端）。

不能直接做 real scan（真实扫描）。

不能直接做 Opportunity Push execution（机会推送执行）。

不能直接生成 ScanScore（扫描分数）。

不能直接生成 Candidate Attention（候选关注）。

不能直接生成 Promote To Home（提升到首页观察）。

不能直接生成 entry / stop / TP / RR（入场 / 止损 / 止盈 / 盈亏比）。

不能直接升级 ExecutionPlan readiness（执行计划可执行就绪）。

不能直接接 order API（下单接口）、execution API（执行接口）或 auto-trading（自动交易）。
