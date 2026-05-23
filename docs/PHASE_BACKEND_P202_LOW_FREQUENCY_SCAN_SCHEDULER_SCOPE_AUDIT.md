# P202 Low-Frequency Scan Scheduler Scope Audit（低频扫描定时器范围审计）

## 一、这一步是干嘛的

P202 是 Low-Frequency Scan Scheduler Scope Audit（低频扫描定时器范围审计）。

P202 是真实 Low-Frequency Scan（低频扫描）前置审计的第一步。它只回答一个问题：未来是否可以进入 Low-Frequency Scan Scheduler（低频扫描定时器）的 Authorization Gate（授权门）。

本轮不写 Java。本轮不新增测试。本轮不改 `dashboard.html`。本轮不创建 scheduler（定时器）。本轮不接 `MarketQuoteClient`（行情客户端）。本轮不读取 runtime / live / external data（运行时 / 实时 / 外部数据）。

本轮不创建 Opportunity Push execution（机会推送执行）。本轮不生成真实 entry / stop / TP / RR（入场 / 止损 / 止盈 / 盈亏比）。本轮不升级 Readiness（可执行就绪）。本轮不接 auto-trading（自动交易）。

P202 只做范围审计，不做功能实现。

## 二、P201 / PROJECT_PROGRESS_INDEX 的依据

`docs/PROJECT_PROGRESS_INDEX.md` 是本轮总索引依据。

P201 已刷新项目总索引，并确认 P197-P200 已完成 Watchlist Low-Frequency Scan / Opportunity Promote read-only audit / docs-only semantics（观察库低频扫描 / 机会提升只读审计 / 只改文档语义）闭环。

`PROJECT_PROGRESS_INDEX.md` 同时明确：real low-frequency scan（真实低频扫描）、Watchlist runtime data source（观察库运行时数据源）、MarketQuoteClient scan integration（行情客户端扫描接入）、scan scheduler（扫描定时器）、Opportunity Promote execution（机会提升执行）、Opportunity Push execution（机会推送执行）、真实 entry / stop / TP / RR（入场 / 止损 / 止盈 / 盈亏比）、Readiness（可执行就绪）和 auto-trading（自动交易）仍未完成。

如果目标开始真实 Low-Frequency Scan（低频扫描），`PROJECT_PROGRESS_INDEX.md` 推荐先进入 Low-Frequency Scan Scheduler Scope Audit（低频扫描定时器范围审计）。

因此 P202 只能审计 scheduler（定时器）范围，不能创建 scheduler（定时器），也不能把已有定时任务改造成观察库扫描器。

## 三、当前已有 scheduler 能力

当前项目已启用 Spring scheduling（Spring 定时任务），入口为 `TradeModelApplication` 上的 `@EnableScheduling`。

只读扫描确认，当前至少已有以下 scheduler（定时器）：

- `PushRecheckScheduler`
- `PositionSyncScheduler`
- `MarketDataScheduler`

`PushRecheckScheduler` 使用 `@Scheduled(initialDelay = 15000, fixedRate = 30000)`。它扫描 pending push snapshot（待二次复核的推送快照），状态范围是 `CAPTURED` 和 `RECHECK_VALID_WAITING`，再通过 `PushRecheckService` 做 Push / Recheck（二次复核）处理。它会通过 `MarketQuoteClient.fetch24hTicker(symbol)` 获取报价，用于当前推送快照的二次复核。

`PushRecheckScheduler` 是 Push / Recheck（二次复核）链路，不是 Watchlist Low-Frequency Scan（观察库低频扫描）。它不能在 P202 被直接改造成 Watchlist Scanner（观察库扫描哨兵）。

`PositionSyncScheduler` 使用 `@Scheduled(initialDelay = 15000, fixedRate = 30000)`。它调用 `PositionSyncService.syncPositions()` 做持仓同步。它属于 Position Monitor（持仓监控）和持仓同步，不是 Watchlist Pool（观察库池）扫描。

`MarketDataScheduler` 使用 `@Scheduled(initialDelay = 60000, fixedRate = 30000)`。它根据 `scheduler.symbols` 配置或默认六币，触发 `RealMarketDataFetcherService.fetchRealMarketData(symbol, "1m")`。它属于已有 V2 市场数据主链和决策落库路径，不是 Watchlist Low-Frequency Scan（观察库低频扫描），也不能在 P202 被偷换成观察库扫描器。

结论：现有 scheduler（定时器）能力已经存在，但职责分别是推送二次复核、持仓同步和市场数据主链。P202 不能修改这些 scheduler（定时器），也不能复用它们直接实现 Low-Frequency Scan Scheduler（低频扫描定时器）。如果未来要新建低频扫描 scheduler（定时器），必须另开 Authorization Gate（授权门）。

## 四、当前 MarketQuoteClient / runtime data 能力

当前项目已有 `MarketQuoteClient`（行情客户端）接口，提供 `fetch24hTicker(String assetSymbol)` 方法。

当前项目也已有 `BinanceMarketQuoteClient` 实现。它使用 Binance public REST 24h ticker（币安公开 24 小时行情接口），并在失败、非 200 响应或字段缺失时返回空结果。

只读扫描确认，`fetch24hTicker` 当前被以下链路使用：

- `PushRecheckScheduler`：在推送快照二次复核前获取当前报价。
- `DecisionServiceImpl`：在读模型决策结果展示中尝试补充最新报价。
- `RealMarketEnvironmentService`：在构造真实市场环境时尝试读取 24h ticker。

当前 `PushRecheckService` 本身不直接拥有 `MarketQuoteClient`（行情客户端）接入；它接收调度器或控制器传入的当前价格并执行二次复核。也就是说，行情读取目前发生在 `PushRecheckScheduler` 等调用方，不代表已经有 Watchlist Scanner（观察库扫描哨兵）数据源。

P202 不允许调用或修改 `MarketQuoteClient`（行情客户端）。P202 不允许读取 Runtime Data（运行时数据）、live data（实时数据）或 external data（外部数据）。

未来如果扫描器需要行情数据，必须先做 Watchlist Runtime Data Source Audit（观察库运行时数据源审计）和 MarketQuoteClient Scan Integration Audit（行情客户端扫描接入审计）。不能在 P202 直接决定数据源，也不能在 P202 直接把 `MarketQuoteClient`（行情客户端）接到扫描器。

## 五、未来低频扫描 scheduler 的安全边界

未来 Low-Frequency Scan Scheduler（低频扫描定时器）如果要实现，必须先满足以下安全边界：

- 只能扫描 Watchlist Pool（观察库池）。
- 不允许扫描 non-watchlist assets（非观察库资产）。
- 不允许把 Display Slots（首页展示位）直接变成 scan universe（扫描全集）。
- 不允许 default-six scan-to-push（默认六币扫描后推送）。
- scheduler（定时器）输出只能是 WatchlistScanCandidate（观察库扫描候选）、ScanResult（扫描结果）或 Review-Only（只允许复核）状态。
- scheduler（定时器）输出不能是交易动作。
- scheduler（定时器）不能生成真实 entry / stop / TP / RR（入场 / 止损 / 止盈 / 盈亏比）。
- scheduler（定时器）不能创建 Opportunity Push execution（机会推送执行）。
- scheduler（定时器）不能升级 Readiness（可执行就绪）。
- scheduler（定时器）不能调用 order API（下单接口）或 execution API（执行接口）。
- scheduler（定时器）不能自动修改 Display Slots（首页展示位）。
- scheduler（定时器）不能自动把资产加入 Watchlist Pool（观察库池）。
- scheduler（定时器）不能自动交易。

低频扫描结果只能留给后续 manual review（人工复核）、Candidate Attention（候选关注）或 Promote To Home（提升到首页观察）审计链路，不能直接变成 Opportunity Push（机会推送）、订单或交易指令。

## 六、建议的扫描频率语义

以下频率只是建议语义，不是 P202 实现：

- 普通 Watchlist Pool（观察库池）资产：15 分钟一次。
- 异常候选复扫：5 分钟一次。
- 已提升首页资产：1-3 分钟一次，但这是首页复核语义，不是 P202 实现。
- 已有持仓风险监控：30-60 秒一次，但这属于 Position Monitor（持仓监控），不属于 Watchlist Low-Frequency Scan（观察库低频扫描）。
- 极端行情或 stampede（踩踏）状态：暂停 Opportunity Promote（机会提升），只保留 Risk Reminder（风险提醒）。

这些频率不在 P202 落地，只能作为后续 Low-Frequency Scan Scheduler Authorization Gate（低频扫描定时器授权门）的候选参数。

## 七、建议的角色语义

以下角色语义只是后续方向，不是 P202 实现：

- 主角色是 Watchlist Scanner（观察库扫描哨兵）。
- 实现方式应规则优先，不默认调用 AI。
- low-cost AI / Grok（低成本 AI / Grok）只在规则触发异常后做 event explanation（事件解释）或 noise filtering（噪音过滤）。
- Gemini 只在候选有冲突时复核。
- GPT 只在准备 Promote To Home（提升到首页观察）前做最终解释或裁决。
- Low-Frequency Scan（低频扫描）不是交易员，是雷达。

P202 不接任何 AI，不接三 AI 流程，不接 Grok，不接 Gemini，不接 GPT。

## 八、未来真正实现前必须分开的后续审计

未来真正实现之前，至少必须分开以下审计，不能混在 P202 内做：

- P203：Low-Frequency Scan Scheduler Authorization Gate（低频扫描定时器授权门）
- Watchlist Runtime Data Source Audit（观察库运行时数据源审计）
- MarketQuoteClient Scan Integration Audit（行情客户端扫描接入审计）
- WatchlistScanResult DTO / Contract Audit（观察库扫描结果 DTO / 契约审计）
- ScanScore Rule Definition Audit（扫描分数规则定义审计）
- Low-Cost AI Event Explanation Gate（低成本 AI 事件解释授权门）
- Three-AI Promote-To-Home Review Gate（三 AI 提升到首页复核授权门）
- Opportunity Push Execution Authorization Gate（机会推送执行授权门）

其中 P203 只能继续做 Authorization Gate（授权门），不能直接写 scheduler（定时器）。数据源、扫描结果契约、ScanScore（扫描分数）、AI 解释、Promote To Home（提升到首页观察）和 Opportunity Push execution（机会推送执行）都必须另开边界。

## 九、是否允许进入 P203

P202 不允许直接写 Low-Frequency Scan Scheduler（低频扫描定时器）。

可以允许 P203 做 Low-Frequency Scan Scheduler Authorization Gate（低频扫描定时器授权门）。

P203 仍然不应该直接实现 scheduler（定时器）。P203 只定义 P204 如果要创建 scheduler（定时器），最多允许哪些文件、哪些参数、哪些阻断规则。

P203 必须继续禁止：

- 扫描非 Watchlist Pool（观察库池）资产。
- 恢复 default-six push（默认六币推送）。
- 读取 Runtime Data（运行时数据）、live data（实时数据）或 external data（外部数据）。
- 生成真实 entry / stop / TP / RR（入场 / 止损 / 止盈 / 盈亏比）。
- 升级 Readiness（可执行就绪）。
- 新增 trading buttons（交易按钮）。
- 接入 auto-trading（自动交易）。

## 十、仍然禁止的路径

以下路径仍然禁止，不能借 P202 的名义提前进入：

- creating low-frequency scan scheduler in P202（在 P202 创建低频扫描定时器）
- scanning non-watchlist assets（扫描非观察库资产）
- default-six scan-to-push（默认六币扫描后推送）
- Display Slots -> scan universe（首页展示位变成扫描全集）
- MarketQuoteClient integration（行情客户端接入）
- runtime / live / external data reads（运行时 / 实时 / 外部数据读取）
- opportunity promote execution（机会提升执行）
- opportunity push execution（机会推送执行）
- opportunity promote -> order（机会提升变成下单）
- opportunity promote -> trade signal（机会提升变成交易信号）
- opportunity promote -> readiness（机会提升变成可执行就绪）
- auto close / reverse / buy / sell（自动平仓 / 反手 / 买入 / 卖出）
- real entry / stop / TP / RR value generation（真实入场 / 止损 / 止盈 / 盈亏比生成）
- ExecutionPlan readiness upgrade（执行计划升级为可执行）
- dashboard trading action buttons（页面交易动作按钮）
- schema / config / mapper changes（数据库 / 配置 / 映射改动）
- scheduler / automation / auto-trading（定时器 / 自动化 / 自动交易）

## 十一、推荐下一步

推荐下一步是：

P203：Low-Frequency Scan Scheduler Authorization Gate（低频扫描定时器授权门）。

P203 仍然不写 scheduler（定时器）。P203 只规定 P204 如果创建 Low-Frequency Scan Scheduler（低频扫描定时器），最多允许改哪些文件。

P203 必须先限定 Watchlist Pool（观察库池）、frequency parameters（频率参数）、fail-closed（失败关闭）、禁止交易动作、禁止 runtime data（运行时数据）扩散，再判断是否允许 P204 做最小实现。

## 十二、P202 硬边界确认

本轮只新增一个 P202 审计文档，并删除 `docs/P202.md` placeholder（占位文档）。

本轮不新增 Java。本轮不新增测试。本轮不改 production Java。本轮不改现有测试。本轮不改 `dashboard.html`。

本轮不新增 controller（控制器）、endpoint（接口端点）、API（接口）、schema（数据库结构）、config（配置）、service（服务）或 mapper（映射）。

本轮不改 `PushRecheckScheduler`、`PushRecheckService`、`PushSnapshotService`。本轮不改 `RuleController`、`RuleConfigService`、`RuleConfigMapper`。

本轮不读取 runtime / live / external data（运行时 / 实时 / 外部数据）。本轮不接 `MarketQuoteClient`（行情客户端）。

本轮不创建 Low-Frequency Scan scheduler（低频扫描定时器）。本轮不创建 Opportunity Push execution（机会推送执行）。

本轮不生成真实 entry / stop / TP / RR（入场 / 止损 / 止盈 / 盈亏比）。本轮不创建 close / reverse / buy / sell（平仓 / 反手 / 买入 / 卖出）动作。

本轮不升级 ExecutionPlan Readiness（执行计划可执行就绪）。本轮不接 order（下单）、execution（执行）、scheduler（定时器）、automation（自动化）或 auto-trading（自动交易）。
