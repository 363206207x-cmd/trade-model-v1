# P200 Watchlist Low-Frequency Scan / Opportunity Promote Closure

## 一、这一步是干嘛的

P200 是 Watchlist Low-Frequency Scan / Opportunity Promote Closure（观察库低频扫描 / 机会提升收口）。

P200 是 P197-P200 这一组的最后一步。

本轮不写 Java。本轮不新增测试。本轮不改 `dashboard.html`。本轮只确认 P197-P199 已完成 read-only audit / documentation semantics（只读审计 / 文档语义）闭环。

本轮不创建 Low-Frequency Scan scheduler（低频扫描定时器）。本轮不接 `MarketQuoteClient`。本轮不读取 runtime / live / external data（运行时 / 实时 / 外部数据）。

本轮不创建 Opportunity Push execution（机会推送执行）。本轮不生成真实 entry / stop / TP / RR（真实入场 / 止损 / 止盈 / 盈亏比）。本轮不升级 Readiness（可执行就绪）。本轮不接 order / execution / scheduler / automation / auto-trading（下单 / 执行 / 定时器 / 自动化 / 自动交易）。

## 二、P197 做了什么

P197 是 Watchlist Low-Frequency Scan / Opportunity Promote Scope Audit（观察库低频扫描 / 机会提升范围审计）。

P197 确认 Display Slots（首页展示位）是首页展示优先级，不是推送全集。

P197 确认 Watchlist Pool（观察库池）才是推送候选最大边界。

P197 确认首页默认六币只用于页面空态 / 系统推荐 / 首页展示排序，不能恢复成“默认六币固定推送”。

P197 确认观察库可以多于 6 个。

P197 确认不在 Watchlist Pool（观察库池）的资产不能进入 Opportunity Push（机会推送）候选。

P197 确认 Display Slots（首页展示位）中但不在 Watchlist Pool（观察库池）的资产不能进入推送候选。

P197 确认当前 Watchlist（观察库）能力主要是 dashboard 展示口径、localStorage 展示列表、文档边界、配置骨架和 fail-closed（失败时关闭）语义。

P197 确认 Low-Frequency Scan（低频扫描）/ Opportunity Promote（机会提升）仍未实现。

P197 确认当前 Push / Recheck / Ops overview（推送 / 复查 / 运维总览）是 read-only / ops / review（只读 / 运维 / 复核）链路，不等于真实 Opportunity Push execution（机会推送执行）。

P197 确认没有真实 Opportunity Push execution（机会推送执行），没有 order API（下单接口），没有 execution API（执行接口），没有自动交易能力。

P197 没有写代码。P197 没有新增测试。P197 没有改 dashboard / API / schema / config（首页工作台 / 接口 / 数据库结构 / 配置）。

## 三、P198 做了什么

P198 是 Watchlist Low-Frequency Scan / Opportunity Promote Authorization Gate（观察库低频扫描 / 机会提升授权门）。

P198 授权 P199 默认优先 docs-only（只改文档）。

P198 明确 P199 不允许创建真实扫描器。

P198 明确 P199 不允许创建 scheduler（定时器）。

P198 明确 P199 不允许读取 runtime / live / external data（运行时 / 实时 / 外部数据）。

P198 明确 P199 不允许创建 Opportunity Push execution（机会推送执行）。

P198 明确 P199 不允许生成真实 entry / stop / TP / RR（真实入场 / 止损 / 止盈 / 盈亏比）。

P198 明确 P199 不允许升级 Readiness（可执行就绪）。

P198 明确 P199 不允许新增交易动作按钮。

P198 明确 P199 不允许自动交易。

P198 没有写代码。P198 没有新增测试。

## 四、P199 做了什么

P199 采用 docs-only minimal wiring（只改文档的最小接线）。

P199 只新增：

`docs/PHASE_BACKEND_P199_WATCHLIST_LOW_FREQUENCY_SCAN_OPPORTUNITY_PROMOTE_MINIMAL_WIRING.md`

P199 删除 `docs/P199.md`。

P199 没有修改 `dashboard.html`。

P199 没有修改 Java。

P199 没有新增测试。

P199 没有新增 API（接口）。

P199 没有新增 schema（数据库结构）。

P199 没有新增 config（配置）。

P199 没有新增 mapper（映射）。

P199 没有新增 service（服务）。

P199 没有接 `PushRecheckScheduler` / `PushRecheckService` / `PushSnapshotService`。

P199 没有接 `RuleController` / `RuleConfigService` / `RuleConfigMapper`。

P199 没有接 `MarketQuoteClient`。

P199 没有创建 Low-Frequency Scan scheduler（低频扫描定时器）。

P199 没有创建 Opportunity Push execution（机会推送执行）。

P199 没有生成真实 entry / stop / TP / RR（真实入场 / 止损 / 止盈 / 盈亏比）。

P199 没有升级 Readiness（可执行就绪）。

P199 没有接自动交易。

P199 定义 Low-Frequency Scan（低频扫描）只是未来可能的低频复核语义，不是 scheduler（定时器）。

P199 定义 Opportunity Promote（机会提升）只是提升到首页观察 / 人工复核，不是 Opportunity Push execution（机会推送执行）、订单、交易信号、Readiness（可执行就绪）或自动交易。

## 五、P197-P200 这组完成了什么

P197-P200 完成的是 Watchlist Low-Frequency Scan / Opportunity Promote（观察库低频扫描 / 机会提升）的 read-only audit / documentation semantics（只读审计 / 文档语义）闭环。

完成的是边界定义，不是功能实现。

完成的是“以后怎么安全做”的语义，不是“已经会扫描”。

Display Slots（首页展示位）/ Watchlist Pool（观察库池）/ Low-Frequency Scan（低频扫描）/ Opportunity Promote（机会提升）的边界已经清晰：

- Display Slots（首页展示位）是首页展示优先级。
- Watchlist Pool（观察库池）是推送候选最大边界。
- Low-Frequency Scan（低频扫描）未来只能从 Watchlist Pool（观察库池）开始。
- Opportunity Promote（机会提升）只是提升到首页观察 / 人工复核。
- 默认六币不能作为默认推送全集。
- 非观察库资产不能进入候选。

仍然没有实现 Low-Frequency Scan（低频扫描）。

仍然没有实现 Opportunity Promote execution（机会提升执行）。

仍然没有实现 Opportunity Push execution（机会推送执行）。

仍然没有读取实时行情。

仍然没有真实点位。

仍然没有 Readiness（可执行就绪）。

仍然没有自动交易。

## 六、P200 的结论

P197-P200 这一组完成。

完成的是 Watchlist Low-Frequency Scan / Opportunity Promote read-only audit / docs-only semantics（观察库低频扫描 / 机会提升只读审计 / 只改文档语义）闭环。

这还不是 Low-Frequency Scan scheduler（低频扫描定时器）。

这还不是 Watchlist Runtime Data Source（观察库运行时数据源）。

这还不是 MarketQuoteClient scan integration（行情客户端扫描接入）。

这还不是 Opportunity Promote execution（机会提升执行）。

这还不是 Opportunity Push execution（机会推送执行）。

这还不是真实 entry / stop / TP / RR（真实入场 / 止损 / 止盈 / 盈亏比）。

这还不是 ExecutionPlan readiness（执行计划可执行就绪）。

这还不是自动交易。

下一步必须回到 `docs/PROJECT_PROGRESS_INDEX.md` 的推荐路线，不能直接跳扫描器、实时数据、机会推送执行、真实点位、Readiness（可执行就绪）或自动交易。

## 七、推荐下一步

推荐下一步为：

P201：Project Progress Index Refresh After Watchlist Scan Promote Semantics
（观察库扫描提升语义后项目总进度索引刷新）

中文解释：

P201 应该更新 `docs/PROJECT_PROGRESS_INDEX.md`。

因为 P197-P200 已经完成 Watchlist Low-Frequency Scan / Opportunity Promote（观察库低频扫描 / 机会提升）只读审计 / 文档语义闭环。

需要把 Watchlist / Opportunity Promote（观察库 / 机会提升）进度从“低频扫描 / opportunity promote（机会提升）仍暂停”更新为“语义和安全边界已完成，但真实扫描器 / 实时数据 / 推送执行 / 真实点位 / Readiness（可执行就绪）/ 自动交易未完成”。

P201 仍然只改文档，不写代码。

## 八、如果未来要真正实现，必须另开哪些审计

如果未来要真正实现，必须另开以下审计：

- Low-Frequency Scan Scheduler Scope Audit（低频扫描定时器范围审计）
- Watchlist Runtime Data Source Audit（观察库运行时数据源审计）
- MarketQuoteClient Scan Integration Audit（行情客户端扫描接入审计）
- WatchlistScanResult DTO / Contract Audit（观察库扫描结果 DTO / 契约审计）
- ScanScore Rule Definition Audit（扫描分数规则定义审计）
- Low-Cost AI Event Explanation Gate（低成本 AI 事件解释授权门）
- Three-AI Promote-To-Home Review Gate（三 AI 提升到首页复核授权门）
- Opportunity Push Execution Authorization Gate（机会推送执行授权门）

这些都不能在 P200 内做。

真正实现时建议先用规则扫描哨兵，不默认调用 AI。

低成本 AI / Grok 只在规则触发异常后做解释。

三 AI 只在准备 Promote To Home（提升到首页）前复核。

Low-Frequency Scan（低频扫描）不是交易员，是雷达。

## 九、仍然禁止的路径

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

## 十、硬边界

本轮只新增一个 P200 收口文档。

本轮删除 placeholder：`docs/P200.md`。

本轮不新增 Java。

本轮不新增测试。

本轮不改 production Java。

本轮不改现有测试。

本轮不改 `dashboard.html`。

本轮不新增 controller / endpoint / API / schema / config / service / mapper（控制器 / 接口 / API / 数据库结构 / 配置 / 服务 / 映射）。

本轮不改 `PushRecheckScheduler` / `PushRecheckService` / `PushSnapshotService`。

本轮不改 `RuleController` / `RuleConfigService` / `RuleConfigMapper`。

本轮不读取 runtime / live / external data（运行时 / 实时 / 外部数据）。

本轮不接 `MarketQuoteClient`。

本轮不创建 Low-Frequency Scan scheduler（低频扫描定时器）。

本轮不创建 Opportunity Push execution（机会推送执行）。

本轮不生成真实 entry / stop / TP / RR（真实入场 / 止损 / 止盈 / 盈亏比）。

本轮不创建 close / reverse / buy / sell（平仓 / 反手 / 买入 / 卖出）动作。

本轮不升级 ExecutionPlan readiness（执行计划可执行就绪）。

本轮不接 order / execution / scheduler / automation / auto-trading（下单 / 执行 / 定时器 / 自动化 / 自动交易）。
