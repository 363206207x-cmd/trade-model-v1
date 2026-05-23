# P199 Watchlist Low-Frequency Scan / Opportunity Promote Minimal Wiring

## 一、这一步是干嘛的

P199 是 Watchlist Low-Frequency Scan / Opportunity Promote Minimal Wiring（观察库低频扫描 / 机会提升最小接线）。

P199 是 P197-P200 最大安全任务包的第三步。

本轮采用 docs-only minimal wiring（只改文档的最小接线）。这意味着本轮不实现扫描器，只把未来 Low-Frequency Scan（低频扫描）/ Opportunity Promote（机会提升）的最小只读语义落成文档边界。

P199 不是 scan scheduler（扫描调度）。不是实时数据读取。不是 Opportunity Push execution（机会推送执行）。不是交易动作。不是 Readiness（可执行就绪）升级。不是自动交易。

本轮不写 Java。本轮不新增测试。本轮不改 `dashboard.html`。本轮不接 scheduler（定时器）。本轮不接 `MarketQuoteClient`。本轮不读取 runtime / live / external data（运行时 / 实时 / 外部数据）。

本轮不创建 Opportunity Push execution（机会推送执行）。本轮不生成真实 entry / stop / TP / RR（真实入场 / 止损 / 止盈 / 盈亏比）。本轮不升级 Readiness（可执行就绪）。本轮不接 order / execution / scheduler / automation / auto-trading（下单 / 执行 / 定时器 / 自动化 / 自动交易）。

## 二、P197 / P198 的依据

P197 已完成 Watchlist Low-Frequency Scan / Opportunity Promote Scope Audit（观察库低频扫描 / 机会提升范围审计）。

P198 已完成 Watchlist Low-Frequency Scan / Opportunity Promote Authorization Gate（观察库低频扫描 / 机会提升授权门）。

P198 明确 P199 默认优先 docs-only（只改文档）。

P198 明确 P199 不允许创建真实扫描器。

P198 明确 P199 不允许创建 scheduler（定时器）。

P198 明确 P199 不允许读取 runtime / live / external data（运行时 / 实时 / 外部数据）。

P198 明确 P199 不允许创建 Opportunity Push execution（机会推送执行）。

P198 明确 P199 不允许生成真实 entry / stop / TP / RR（真实入场 / 止损 / 止盈 / 盈亏比）。

P198 明确 P199 不允许升级 Readiness（可执行就绪）。

P198 明确 P199 不允许新增交易动作按钮。

P198 明确 P199 不允许自动交易。

因此 P199 只能把文档边界落地，不能把“未来可以审计”误写成“现在可以执行”。

## 三、最小只读接线语义

Low-Frequency Scan（低频扫描）在 P199 只是“未来可能的低频复核语义”，不是 scheduler（定时器）。

Opportunity Promote（机会提升）在 P199 只是“提升到首页观察 / 人工复核”的语义，不是 Opportunity Push execution（机会推送执行）。

Opportunity Promote（机会提升）不是订单。

Opportunity Promote（机会提升）不是交易信号。

Opportunity Promote（机会提升）不是 Readiness（可执行就绪）。

Opportunity Promote（机会提升）不生成 entry / stop / TP / RR（入场 / 止损 / 止盈 / 盈亏比）。

Opportunity Promote（机会提升）不触发下单。

Opportunity Promote（机会提升）不触发推送执行。

Opportunity Promote（机会提升）不自动修改 Display Slots（首页展示位）。

Opportunity Promote（机会提升）不自动把资产加入 Watchlist Pool（观察库池）。

Opportunity Promote（机会提升）只能作为后续人工复核材料。

P199 的“最小接线”只是在文档中把这些边界明确下来。它不产生运行时行为，不改变页面，不改变接口，不改变数据结构。

## 四、必须继承的 Watchlist / Display Slots 边界

Display Slots（首页展示位）是首页展示优先级，不是推送全集。

Watchlist Pool（观察库池）才是推送候选最大边界。

首页默认 6 个资产只是 Display Slots（首页展示位），不是后端推送全集，也不是唯一观察库。

观察库可以多于 6 个。

在 Watchlist Pool（观察库池）中的资产未来才可能进入 Low-Frequency Scan（低频扫描）候选。

不在 Watchlist Pool（观察库池）中的资产不能进入 Opportunity Push（机会推送）候选。

Display Slots（首页展示位）中但不在 Watchlist Pool（观察库池）的资产不能进入推送候选。

不允许恢复“默认六币固定推送”。

不允许把首页展示资产直接等同于观察库。

不允许把 Opportunity Promote（机会提升）等同于自动交易。

## 五、P199 本轮实际完成内容

本轮只新增 P199 文档。

本轮删除 `docs/P199.md` placeholder（占位文档）。

本轮没有修改 `dashboard.html`。

本轮没有修改 Java。

本轮没有新增测试。

本轮没有新增 API（接口）。

本轮没有新增 schema（数据库结构）。

本轮没有新增 config（配置）。

本轮没有新增 mapper（映射）。

本轮没有新增 service（服务）。

本轮没有接 `PushRecheckScheduler` / `PushRecheckService` / `PushSnapshotService`。

本轮没有接 `RuleController` / `RuleConfigService` / `RuleConfigMapper`。

本轮没有接 `MarketQuoteClient`。

本轮没有创建 Low-Frequency Scan scheduler（低频扫描定时器）。

本轮没有创建 Opportunity Push execution（机会推送执行）。

本轮没有生成真实 entry / stop / TP / RR（真实入场 / 止损 / 止盈 / 盈亏比）。

本轮没有升级 Readiness（可执行就绪）。

本轮没有接自动交易。

## 六、未来真正实现前必须另开的审计

如果未来要做真实 Low-Frequency Scan（低频扫描），必须另开以下审计，不能在 P199 内做：

- Low-Frequency Scan Scheduler Scope Audit（低频扫描定时器范围审计）
- Watchlist Runtime Data Source Audit（观察库运行时数据源审计）
- MarketQuoteClient Scan Integration Audit（行情客户端扫描接入审计）
- Opportunity Promote Data Contract Audit（机会提升数据契约审计）
- Opportunity Push Execution Authorization Gate（机会推送执行授权门）

这些审计必须分别确认数据来源、调度边界、候选边界、展示语义、执行权限和失败关闭规则。它们都不能借 P199 的名义提前落地。

## 七、仍然禁止的路径

以下路径仍然禁止：

- default-six opportunity push（默认六币机会推送）
- Display Slots -> push candidates（首页展示位直接变成推送候选）
- non-watchlist asset -> push candidate（非观察库资产进入推送候选）
- low-frequency scan scheduler（低频扫描定时器）
- runtime / live / external data reads（运行时 / 实时 / 外部数据读取）
- MarketQuoteClient scan integration（行情客户端扫描接入）
- opportunity push execution（机会推送执行）
- opportunity promote -> order（机会提升变成下单）
- opportunity promote -> trade signal（机会提升变成交易信号）
- opportunity promote -> readiness（机会提升变成可执行就绪）
- auto close position（自动平仓）
- auto reverse position（自动反手）
- auto buy / auto sell（自动买入 / 自动卖出）
- market order execution（市价执行）
- order API（下单接口）
- execution API（执行接口）
- real entry / stop / TP / RR value generation（真实入场 / 止损 / 止盈 / 盈亏比生成）
- ExecutionPlan readiness upgrade（执行计划升级为可执行）
- dashboard trading action buttons（页面交易动作按钮）
- schema / config / mapper changes（数据库 / 配置 / 映射改动）
- scheduler / automation / auto-trading（定时器 / 自动化 / 自动交易）

这些路径都不属于 Review-Only（只允许复核）或 Read-Only Display（只读展示）。只要进入执行、调度、下单、真实点位或自动交易，就必须另开授权。

## 八、推荐下一步

推荐下一步为：

P200：Watchlist Low-Frequency Scan / Opportunity Promote Closure
（观察库低频扫描 / 机会提升收口）

中文解释：

P200 只写收口文档。

P200 确认 P197-P199 完成的是只读审计 / 文档语义闭环。

P200 必须继续确认 Low-Frequency Scan（低频扫描）、Opportunity Promote execution（机会提升执行）、Opportunity Push execution（机会推送执行）、真实 entry / stop / TP / RR（真实入场 / 止损 / 止盈 / 盈亏比）、Readiness（可执行就绪）和自动交易都没有完成。

## 九、硬边界

本轮只新增一个 P199 文档。

本轮删除 placeholder：`docs/P199.md`。

本轮不新增 Java。

本轮不新增测试。

本轮不改 production Java。

本轮不改现有测试。

本轮不改 `dashboard.html`。

本轮不新增 controller / endpoint / API / schema / config / service / mapper（控制器 / 接口 / API / 数据库结构 / 配置 / 服务 / 映射）。

本轮不读取 runtime / live / external data（运行时 / 实时 / 外部数据）。

本轮不接 `MarketQuoteClient`。

本轮不创建 Low-Frequency Scan scheduler（低频扫描定时器）。

本轮不创建 Opportunity Push execution（机会推送执行）。

本轮不生成真实 entry / stop / TP / RR（真实入场 / 止损 / 止盈 / 盈亏比）。

本轮不创建 close / reverse / buy / sell（平仓 / 反手 / 买入 / 卖出）动作。

本轮不升级 ExecutionPlan readiness（执行计划可执行就绪）。

本轮不接 order / execution / scheduler / automation / auto-trading（下单 / 执行 / 定时器 / 自动化 / 自动交易）。
