# P198 Watchlist Low-Frequency Scan / Opportunity Promote Authorization Gate

## 一、这一步是干嘛的

P198 是 Watchlist Low-Frequency Scan / Opportunity Promote Authorization Gate（观察库低频扫描 / 机会提升授权门）。

P198 是 P197-P200 最大安全任务包的第二步。它只负责给 P199 划定最小安全边界：如果 P199 要做最小只读审计、配置展示或文档说明，最多允许碰哪些文件，哪些路径必须继续禁止。

本轮不写 Java。本轮不新增测试。本轮不改 `dashboard.html`。本轮只规定 P199 如果写最小只读审计 / 配置展示 / 文档，允许改哪些文件。

本轮不生成真实 entry / stop / TP / RR（真实入场 / 止损 / 止盈 / 盈亏比）。本轮不创建 opportunity push execution（机会推送执行）。本轮不创建平仓 / 反手 / 买入 / 卖出动作。本轮不升级 Readiness（可执行就绪）。本轮不接自动交易。

## 二、P197 审计结论

P197 已确认 Display Slots（首页展示位）是首页展示优先级，不是推送全集。

P197 已确认 Watchlist Pool（观察库池）才是 Opportunity Push（机会推送）候选的最大边界。

P197 已确认首页默认六币只用于页面空态 / 系统推荐 / 首页展示排序，不能恢复成“默认六币固定推送”。

P197 已确认观察库可以多于 6 个。

P197 已确认不在 Watchlist Pool（观察库池）的资产不能进入 Opportunity Push（机会推送）候选。

P197 已确认 Display Slots（首页展示位）中但不在 Watchlist Pool（观察库池）的资产不能进入推送候选。

P197 已确认当前 Watchlist（观察库）能力主要是 dashboard 展示口径、localStorage 展示列表、文档边界、配置骨架和 fail-closed（失败时关闭）语义。

P197 已确认 Low-Frequency Scan（低频扫描）/ Opportunity Promote（机会提升）仍未实现。

P197 已确认当前 Push / Recheck / Ops overview（推送 / 复查 / 运维总览）是 read-only / ops / review（只读 / 运维 / 复核）链路，不等于真实 Opportunity Push execution（机会推送执行）。

P197 已确认当前没有真实 Opportunity Push execution（机会推送执行），没有 order API（下单接口），没有 execution API（执行接口），没有自动交易能力。

## 三、必须继承的 Watchlist / Display Slots 边界

Display Slots（首页展示位）是首页展示优先级，不是推送全集。

Watchlist Pool（观察库池）才是推送候选最大边界。

首页默认 6 个资产只是 Display Slots（首页展示位），不是后端推送全集，也不是唯一观察库。

观察库可以多于 6 个。

在 Watchlist Pool（观察库池）中的资产可以未来进入 Low-Frequency Scan（低频扫描）候选。

不在 Watchlist Pool（观察库池）中的资产不能进入 Opportunity Push（机会推送）候选。

Display Slots（首页展示位）中但不在 Watchlist Pool（观察库池）的资产不能进入推送候选。

不允许恢复“默认六币固定推送”。

不允许把首页展示资产直接等同于观察库。

不允许把 Opportunity Promote（机会提升）等同于自动交易。

## 四、是否允许 P199 写代码

结论：可以允许 P199 做最小只读审计 / 配置展示 / 文档，但 P199 必须极小。

P199 默认优先只改 docs（文档）或 dashboard（首页工作台）只读文案。

P199 不允许创建真实扫描器。

P199 不允许创建 scheduler（定时器）。

P199 不允许读取 runtime / live / external data（运行时 / 实时 / 外部数据）。

P199 不允许创建 Opportunity Push execution（机会推送执行）。

P199 不允许生成真实 entry / stop / TP / RR（真实入场 / 止损 / 止盈 / 盈亏比）。

P199 不允许升级 Readiness（可执行就绪）。

P199 不允许新增交易动作按钮。

P199 不允许自动交易。

## 五、P199 允许改哪些文件

P199 最多允许改 1-3 个文件，并且默认优先 docs-only（只改文档）。

1. `docs/PHASE_BACKEND_P199_WATCHLIST_LOW_FREQUENCY_SCAN_OPPORTUNITY_PROMOTE_MINIMAL_WIRING.md`

   或等价 P199 文档文件。如果最终选择 docs-only minimal wiring（只改文档的最小接线），这是首选文件。

2. `src/main/resources/templates/dashboard.html`

   仅当需要补充页面只读说明时允许。

3. `docs/PROJECT_PROGRESS_INDEX.md`

   不建议在 P199 修改。除非只补一行“P199 未完成 / 待 P200/P201 更新”，否则默认放到 P200/P201 再更新。

如果 P199 修改 `dashboard.html`，只能增加只读文案，说明：

- Display Slots（首页展示位）不是推送候选。
- Watchlist Pool（观察库池）才是推送候选边界。
- Low-Frequency Scan（低频扫描）未实现。
- Opportunity Promote（机会提升）不是下单。
- 默认六币不等于默认推送全集。

默认不允许 P199 改 Java。

默认不允许 P199 改 tests（测试）。

默认不允许 P199 改 controller / endpoint / API（控制器 / 接口 / API）。

默认不允许 P199 改 service / mapper / schema / config（服务 / 映射 / 数据库结构 / 配置）。

默认不允许 P199 改 `PushRecheckScheduler` / `PushRecheckService` / `PushSnapshotService`。

默认不允许 P199 改 `RuleController` / `RuleConfigService` / `RuleConfigMapper`。

如果 P199 发现必须新增 Watchlist API（观察库接口）、schema（数据库结构）、service（服务）、mapper（映射）或 scheduler（定时器），必须停止并另开授权，不允许在 P199 内扩散。

## 六、P199 允许做什么

P199 只能做以下最小工作：

- 写最小只读文档。
- 或在 `dashboard.html` 加只读说明。
- 或补充 Watchlist（观察库）/ Display Slots（首页展示位）/ Opportunity Promote（机会提升）的 Review-Only（只允许复核）文案。
- 说明 Low-Frequency Scan（低频扫描）尚未实现。
- 说明 Opportunity Promote（机会提升）只是“提升到首页观察 / 人工复核”。
- 说明 Opportunity Promote（机会提升）不是 Opportunity Push execution（机会推送执行）。
- 说明 Opportunity Promote（机会提升）不是订单。
- 说明不在 Watchlist Pool（观察库池）的资产不能进入候选。
- 说明默认六币不能作为默认推送全集。
- 说明不生成真实 entry / stop / TP / RR（真实入场 / 止损 / 止盈 / 盈亏比）。
- 说明不升级 Readiness（可执行就绪）。
- 说明不自动交易。

P199 只能解释“为什么还不能扫描 / 为什么只能复核 / 为什么不能推送执行”。P199 不能把解释变成执行入口。

## 七、P199 禁止做什么

P199 不允许新增 Low-Frequency Scan scheduler（低频扫描定时器）。

P199 不允许读取 runtime / live / external data（运行时 / 实时 / 外部数据）。

P199 不允许接 `MarketQuoteClient`。

P199 不允许改 `PushRecheckScheduler`。

P199 不允许改 `PushRecheckService` / `PushSnapshotService`。

P199 不允许新增 Watchlist API（观察库接口）。

P199 不允许新增 controller / endpoint / API（控制器 / 接口 / API）。

P199 不允许新增 mapper（映射）。

P199 不允许修改 schema（数据库结构）。

P199 不允许修改 config（配置）。

P199 不允许生成 entry / stop / TP / RR（入场 / 止损 / 止盈 / 盈亏比）。

P199 不允许升级 ExecutionPlan readiness（执行计划可执行就绪）。

P199 不允许创建 Opportunity Push execution（机会推送执行）。

P199 不允许创建 close / reverse / buy / sell（平仓 / 反手 / 买入 / 卖出）动作。

P199 不允许新增买入 / 卖出 / 平仓 / 反手按钮。

P199 不允许接 order / execution / scheduler / automation / auto-trading（下单 / 执行 / 定时器 / 自动化 / 自动交易）。

## 八、仍然禁止的路径

- default-six opportunity push（默认六币机会推送）
- Display Slots -> push candidates（首页展示位直接变成推送候选）
- non-watchlist asset -> push candidate（非观察库资产进入推送候选）
- low-frequency scan scheduler（低频扫描定时器）
- runtime / live / external data reads（运行时 / 实时 / 外部数据读取）
- MarketQuoteClient scan integration（行情客户端扫描接入）
- opportunity push execution（机会推送执行）
- opportunity promote -> order（机会提升变成下单）
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

## 九、推荐下一步

推荐下一步为：

P199：Watchlist Low-Frequency Scan / Opportunity Promote Minimal Wiring
（观察库低频扫描 / 机会提升最小接线）

中文解释：

P199 可以开始最小只读工作。

推荐 P199 做 docs-only（只改文档）或 dashboard（首页工作台）只读文案。

P199 不能接扫描器。

P199 不能接实时数据。

P199 不能接推送执行。

P199 不能创建交易动作。

P199 不能升级 Readiness（可执行就绪）。

P199 不能自动交易。

## 十、硬边界

本轮只新增一个 P198 授权门文档。

本轮删除 placeholder：`docs/P198.md`。

本轮不新增 Java。

本轮不新增测试。

本轮不改 production Java。

本轮不改现有测试。

本轮不改 `dashboard.html`。

本轮不新增 controller / endpoint / API / schema / config / service / mapper（控制器 / 接口 / API / 数据库结构 / 配置 / 服务 / 映射）。

本轮不读取 runtime / live / external data（运行时 / 实时 / 外部数据）。

本轮不生成真实 entry / stop / TP / RR（真实入场 / 止损 / 止盈 / 盈亏比）。

本轮不创建 Opportunity Push execution（机会推送执行）。

本轮不创建 close / reverse / buy / sell（平仓 / 反手 / 买入 / 卖出）动作。

本轮不升级 ExecutionPlan readiness（执行计划可执行就绪）。

本轮不接 order / execution / scheduler / automation / auto-trading（下单 / 执行 / 定时器 / 自动化 / 自动交易）。
