# P203 Low-Frequency Scan Scheduler Authorization Gate（低频扫描定时器授权门）

## 一、这一步是干嘛的

P203 是 Low-Frequency Scan Scheduler Authorization Gate（低频扫描定时器授权门）。

P203 是真实低频扫描 scheduler（定时器）的前置授权门。它只规定 P204 如果创建最小 scheduler skeleton（定时器骨架），最多允许改哪些文件、哪些频率参数、哪些 Fail-Closed（失败关闭）条件。

本轮不写 Java。本轮不新增测试。本轮不改 `dashboard.html`。本轮不创建 scheduler（定时器）。

本轮不接 `MarketQuoteClient`（行情客户端）。本轮不读取 runtime / live / external data（运行时 / 实时 / 外部数据）。本轮不创建 Opportunity Push execution（机会推送执行）。

本轮不生成真实 entry / stop / TP / RR（入场 / 止损 / 止盈 / 盈亏比）。本轮不升级 Readiness（可执行就绪）。本轮不接 auto-trading（自动交易）。

## 二、P202 审计结论

P202 已确认项目已有 Spring scheduling（Spring 定时任务），入口为 `TradeModelApplication` 的 `@EnableScheduling`。

P202 已确认当前至少已有以下 scheduler（定时器）：

- `PushRecheckScheduler`
- `PositionSyncScheduler`
- `MarketDataScheduler`

`PushRecheckScheduler` 是 Push / Recheck（二次复核）链路，不是 Watchlist Low-Frequency Scan（观察库低频扫描）。

`PositionSyncScheduler` 是 position sync（持仓同步），不是观察库扫描。

`MarketDataScheduler` 是市场数据主链和决策落库路径，不是观察库扫描。

这些现有 scheduler（定时器）不能在 P203/P204 被直接改造成 Watchlist Scanner（观察库扫描哨兵）。未来如果要做 Low-Frequency Scan Scheduler（低频扫描定时器），应该新建默认关闭的最小入口，而不是改造已有链路。

P202 已确认 `MarketQuoteClient`（行情客户端）已存在，`fetch24hTicker` 当前被 `PushRecheckScheduler`、`DecisionServiceImpl`、`RealMarketEnvironmentService` 等使用。

P202 已确认 P202 不允许调用或修改 `MarketQuoteClient`（行情客户端）。

P202 已确认未来扫描器如需行情数据，必须先做 Watchlist Runtime Data Source Audit（观察库运行时数据源审计）和 MarketQuoteClient Scan Integration Audit（行情客户端扫描接入审计）。

P202 允许进入 P203 授权门，但不允许 P202 直接写 scheduler（定时器）。

## 三、是否允许 P204 创建最小 scheduler skeleton

可以允许 P204 创建最小 Low-Frequency Scan Scheduler skeleton（低频扫描定时器骨架）。

但 P204 只能是 skeleton（骨架）、dry-run（空跑）和 disabled-by-default（默认关闭）。

P204 不能接 `MarketQuoteClient`（行情客户端）。P204 不能读取 runtime / live / external data（运行时 / 实时 / 外部数据）。P204 不能扫描真实资产并生成机会。

P204 不能创建 Opportunity Push execution（机会推送执行）。P204 不能生成真实 entry / stop / TP / RR（入场 / 止损 / 止盈 / 盈亏比）。P204 不能升级 Readiness（可执行就绪）。

P204 不能调用 order API（下单接口）或 execution API（执行接口）。P204 不能自动交易。

保守结论：P204 可以创建一个默认关闭、只返回 disabled / skipped / review-only / not implemented 语义的 scheduler skeleton（定时器骨架），但不能让它变成真实扫描器。

## 四、P204 允许改哪些文件

P204 最多允许改 2-4 个文件。

建议授权范围如下：

1. 新增 `src/main/java/org/example/trademodel/service/watchlist/WatchlistLowFrequencyScanScheduler.java`
2. 新增 `src/test/java/org/example/trademodel/service/watchlist/WatchlistLowFrequencyScanSchedulerTest.java`
3. 仅在必要时新增 `src/main/java/org/example/trademodel/service/watchlist/WatchlistLowFrequencyScanConfig.java`，或等价 config holder（配置承载对象），但不能接 `application.yml`
4. 仅在必要时新增 `src/test/java/org/example/trademodel/service/watchlist/WatchlistLowFrequencyScanConfigTest.java`

默认不允许改 `application.yml`。

默认不允许改 `schema.sql`。

默认不允许改 `RuleController`、`RuleConfigService`、`RuleConfigMapper`。

默认不允许改 `PushRecheckScheduler`、`PushRecheckService`、`PushSnapshotService`。

默认不允许改 `PositionSyncScheduler`、`PositionSyncService`。

默认不允许改 `MarketDataScheduler`、`RealMarketDataFetcherService`。

默认不允许改 `MarketQuoteClient`、`BinanceMarketQuoteClient`。

默认不允许改 `DashboardController`、`dashboard.html`。

默认不允许新增 controller（控制器）、endpoint（接口端点）或 API（接口）。

默认不允许新增 mapper（映射）、schema（数据库结构）或 DB table（数据库表）。

如果 P204 发现必须新增配置、schema（数据库结构）、API（接口）、mapper（映射）、实时数据源或 Watchlist API（观察库接口），必须停止并另开授权，不能在 P204 内扩散。

## 五、P204 scheduler skeleton 允许做什么

P204 允许新增一个 disabled-by-default scheduler skeleton（默认关闭的定时器骨架）。

可以有 `@Scheduled`，但 scheduler 方法内部第一行必须做 Fail-Closed（失败关闭）检查：`enabled=false` 时立即跳过。

默认 `enabled=false`。

默认 scan interval（扫描间隔）语义只能写在代码常量或 config holder（配置承载对象）里，不能改 `application.yml`。

可以定义建议频率语义：

- 普通 Watchlist Pool（观察库池）：15 分钟。
- 异常候选复扫：5 分钟。
- 首页提升资产：1-3 分钟，但后续不在 P204 实现。
- 持仓监控：30-60 秒，属于 Position Monitor（持仓监控），不属于 P204。

scheduler（定时器）方法只能调用 no-op（无操作）或 dry-run（空跑）逻辑。

可以记录或返回 skipped（已跳过）、disabled（已关闭）或 Review-Only（只允许复核）状态。

可以在测试中验证默认 disabled（关闭）、不会执行扫描、不会调用行情、不会创建推送、不会生成交易动作。

可以定义类名、方法名和注释，说明这是 Watchlist Scanner（观察库扫描哨兵）未来入口。

输出语义只能是 `REVIEW_ONLY` / `DISABLED` / `SKIPPED` / `NOT_IMPLEMENTED`。

不允许生成真实扫描结果。不允许生成 ScanScore（扫描分数）。不允许生成 Candidate Attention（候选关注）。不允许生成 Promote To Home（提升到首页观察）。

## 六、P204 scheduler skeleton 禁止做什么

P204 不允许接 `MarketQuoteClient`（行情客户端）。

P204 不允许接 `BinanceMarketQuoteClient`（币安行情客户端）。

P204 不允许读取 runtime / live / external data（运行时 / 实时 / 外部数据）。

P204 不允许读取真实 Watchlist Pool（观察库池）。

P204 不允许扫描非观察库资产。

P204 不允许把 Display Slots（首页展示位）当 scan universe（扫描全集）。

P204 不允许 default-six scan-to-push（默认六币扫描后推送）。

P204 不允许接 `PushRecheckScheduler`、`PushRecheckService`、`PushSnapshotService`。

P204 不允许创建 Opportunity Push execution（机会推送执行）。

P204 不允许创建 Opportunity Promote execution（机会提升执行）。

P204 不允许自动修改 Display Slots（首页展示位）。

P204 不允许自动加入 Watchlist Pool（观察库池）。

P204 不允许生成 entry / stop / TP / RR（入场 / 止损 / 止盈 / 盈亏比）。

P204 不允许升级 Readiness（可执行就绪）。

P204 不允许创建 close / reverse / buy / sell（平仓 / 反手 / 买入 / 卖出）动作。

P204 不允许调用 order API（下单接口）或 execution API（执行接口）。

P204 不允许新增 trading buttons（交易按钮）。

P204 不允许自动交易。

## 七、P204 fail-closed 条件

P204 skeleton（骨架）至少要满足以下 Fail-Closed（失败关闭）条件：

- `enabled` 默认为 `false`。
- watchlist provider（观察库提供者）未接入时跳过。
- data source（数据源）未接入时跳过。
- `MarketQuoteClient`（行情客户端）未授权时跳过。
- Watchlist Pool（观察库池）为空时跳过。
- 非观察库资产禁止扫描。
- Display Slots（首页展示位）不能作为 scan universe（扫描全集）。
- data quality（数据质量）未知时跳过。
- risk state（风险状态）未知时只允许 Review-Only（只允许复核），不允许 promote（提升）。
- stampede（踩踏）状态必须禁止 Opportunity Promote（机会提升）。
- wick-only risk（仅插针风险）不能升级趋势判断。
- 任一异常都不能创建交易动作。

这些条件的目的不是让 P204 变聪明，而是让 P204 在未授权、未接数据源、未接观察库、未明确风险状态时默认不做事。

## 八、P204 测试必须覆盖

P204 测试必须覆盖：

- 默认 `enabled=false`。
- scheduler（定时器）调用时不会执行真实扫描。
- 不会调用 `MarketQuoteClient`（行情客户端）。
- 不会读取 runtime / live / external data（运行时 / 实时 / 外部数据）。
- 不会创建 Opportunity Push execution（机会推送执行）。
- 不会生成 entry / stop / TP / RR（入场 / 止损 / 止盈 / 盈亏比）。
- 不会升级 Readiness（可执行就绪）。
- 不会创建 buy / sell / close / reverse（买入 / 卖出 / 平仓 / 反手）。
- Display Slots（首页展示位）不能作为 scan universe（扫描全集）。
- 非 Watchlist Pool（观察库池）不能被扫描。
- 输出只能是 disabled / skipped / review-only / not implemented（已关闭 / 已跳过 / 只允许复核 / 未实现）语义。

测试只能证明“默认关闭且不会越界”，不能伪造真实扫描结果。

## 九、仍然禁止的路径

以下路径仍然禁止，不能借 P203 或 P204 的名义提前进入：

- creating active scanner in P204（P204 创建激活扫描器）
- MarketQuoteClient integration（行情客户端接入）
- BinanceMarketQuoteClient integration（币安行情客户端接入）
- runtime / live / external data reads（运行时 / 实时 / 外部数据读取）
- scanning Display Slots as universe（把首页展示位当扫描全集）
- scanning non-watchlist assets（扫描非观察库资产）
- default-six scan-to-push（默认六币扫描后推送）
- opportunity promote execution（机会提升执行）
- opportunity push execution（机会推送执行）
- opportunity promote -> order（机会提升变成下单）
- opportunity promote -> trade signal（机会提升变成交易信号）
- opportunity promote -> readiness（机会提升变成可执行就绪）
- real entry / stop / TP / RR value generation（真实入场 / 止损 / 止盈 / 盈亏比生成）
- ExecutionPlan readiness upgrade（执行计划升级为可执行）
- dashboard trading action buttons（页面交易动作按钮）
- schema / config / mapper changes（数据库 / 配置 / 映射改动）
- controller / endpoint / API action wiring（控制器 / 接口动作接线）
- order / execution / scheduler / automation / auto-trading（下单 / 执行 / 定时器 / 自动化 / 自动交易）

## 十、推荐下一步

推荐下一步是：

P204：Low-Frequency Scan Scheduler Minimal Skeleton（低频扫描定时器最小骨架）。

P204 可以创建 disabled-by-default scheduler skeleton（默认关闭的定时器骨架）。

P204 只能证明低频扫描入口存在但默认关闭。

P204 不能接行情。不能读实时数据。不能扫真实资产。不能产生机会。不能推送。不能给点位。不能交易。

## 十一、P203 硬边界确认

本轮只新增一个 P203 授权门文档，并删除 `docs/P203.md` placeholder（占位文档）。

本轮不新增 Java。本轮不新增测试。本轮不改 production Java。本轮不改现有测试。本轮不改 `dashboard.html`。

本轮不新增 controller（控制器）、endpoint（接口端点）、API（接口）、schema（数据库结构）、config（配置）、service（服务）或 mapper（映射）。

本轮不改 `PushRecheckScheduler`、`PushRecheckService`、`PushSnapshotService`。

本轮不改 `PositionSyncScheduler`、`PositionSyncService`。

本轮不改 `MarketDataScheduler`、`RealMarketDataFetcherService`。

本轮不改 `RuleController`、`RuleConfigService`、`RuleConfigMapper`。

本轮不读取 runtime / live / external data（运行时 / 实时 / 外部数据）。

本轮不接 `MarketQuoteClient`（行情客户端）。

本轮不创建 Low-Frequency Scan scheduler（低频扫描定时器）。

本轮不创建 Opportunity Push execution（机会推送执行）。

本轮不生成真实 entry / stop / TP / RR（入场 / 止损 / 止盈 / 盈亏比）。

本轮不创建 close / reverse / buy / sell（平仓 / 反手 / 买入 / 卖出）动作。

本轮不升级 ExecutionPlan Readiness（执行计划可执行就绪）。

本轮不接 order（下单）、execution（执行）、scheduler（定时器）、automation（自动化）或 auto-trading（自动交易）。
