# P197 Watchlist Low-Frequency Scan / Opportunity Promote Scope Audit（观察库低频扫描 / 机会提升范围审计）

## 一、这一步是干嘛的

P197 是 Watchlist Low-Frequency Scan / Opportunity Promote Scope Audit（观察库低频扫描 / 机会提升范围审计）。

Watchlist Pool（观察库池）是后续机会推送候选的最大资产边界。Display Slots（首页展示位）只是 Dashboard（首页工作台）上优先展示的资产位置。Low-Frequency Scan（低频扫描）是未来可能对观察库资产做低频复核的流程。Opportunity Promote（机会提升）是未来可能把观察库资产提升到首页观察或人工复核位置的语义，不是交易执行。Opportunity Push（机会推送）是更强的推送语义，本轮不实现。

P197 是 P197-P200 Watchlist Low-Frequency Scan / Opportunity Promote Audit Pack（观察库低频扫描 / 机会提升审计包）的第一步。

本轮只新增一个 P197 审计文档，并删除 `docs/P197.md` placeholder（占位文档）。

本轮不写 Java。本轮不新增测试。本轮不接 Dashboard（首页工作台）新功能。本轮只判断未来观察库低频扫描和机会提升是否可以进入 Review-Only（只允许复核）审计路径。

本轮不生成真实 entry / stop / TP / RR（入场 / 止损 / 止盈 / 盈亏比）。本轮不创建 opportunity push execution（机会推送执行）。本轮不创建 close / reverse / buy / sell（平仓 / 反手 / 买入 / 卖出）动作。本轮不升级 Readiness（可执行就绪）。本轮不接 order API（下单接口）、execution API（执行接口）、scheduler（定时器）、automation（自动化）或 auto-trading（自动交易）。

P197 的判断对象不是“能不能自动扫机会并推送交易”，而是“未来是否可以先定义观察库低频扫描 / 机会提升的只读审计边界”。

## 二、P196 / PROJECT_PROGRESS_INDEX 的依据

P197 必须以 `docs/PROJECT_PROGRESS_INDEX.md` 为总索引依据。

P196 已完成 Project Progress Index Refresh After Dashboard Risk Reminder Display（首页风险提醒展示后项目总进度索引刷新）。

P196 后，总索引明确写清楚：

- Dashboard Risk Reminder（首页风险提醒）Read-Only Display（只读展示）闭环已完成。
- Dashboard（首页工作台）现在可以更集中展示风险建议、阻断原因、流动性、踩踏、插针、市价退出是否允许、机会推送 / 反手 / 新开仓是否允许、人工复核、不是交易指令，以及自动平仓 / 自动反手 / 自动改止损关闭。
- 如果目标是个人可用最快路径，`PROJECT_PROGRESS_INDEX.md` 推荐下一步进入 Watchlist Low-Frequency Scan / Opportunity Promote Audit（观察库低频扫描 / 机会提升审计）。

但总索引也同时明确：

- Watchlist（观察库）低频扫描 / opportunity promote（机会提升）仍暂停。
- trading actions（交易动作）仍暂停。
- production risk action（生产风控动作）仍暂停。
- auto close / auto reverse / auto buy / auto sell（自动平仓 / 自动反手 / 自动买入 / 自动卖出）仍暂停。
- auto stop modification（自动修改止损）仍暂停。
- real entry / stop / TP / RR（真实入场 / 止损 / 止盈 / 盈亏比）仍暂停。
- ExecutionPlan Readiness（执行计划可执行就绪）仍暂停。
- auto-trading（自动交易）仍暂停。

因此 P197 只能审计 Watchlist Low-Frequency Scan / Opportunity Promote（观察库低频扫描 / 机会提升）范围，不能实现扫描任务、推送执行或交易动作。

## 三、必须继承的 Watchlist / Display Slots 边界

P197 必须继承之前 Dashboard（首页工作台）和 Watchlist（观察库）文档中已经定下的边界。

Display Slots（首页展示位）是首页展示优先级，不是推送全集。

Watchlist Pool（观察库池）才是推送候选最大边界。

首页默认 6 个资产只是 Display Slots（首页展示位），不是后端推送全集，也不是唯一观察库。

当前 `dashboard.html` 里默认展示位是：

- `BTCUSDT`
- `ETHUSDT`
- `SOLUSDT`
- `BNBUSDT`
- `XRPUSDT`
- `DOGEUSDT`

这些默认六币只用于页面空态、系统推荐和首页展示排序。它们不能被恢复成“默认六币固定推送”。

观察库可以多于 6 个。在观察库中的资产，未来可以进入 Low-Frequency Scan（低频扫描）候选。

不在 Watchlist Pool（观察库池）中的资产，不能进入 Opportunity Push（机会推送）候选。

Display Slots（首页展示位）中但不在 Watchlist Pool（观察库池）的资产，不能进入推送候选。

不允许把首页展示资产直接等同于观察库。

不允许把 Opportunity Promote（机会提升）等同于自动交易。

Opportunity Promote（机会提升）最多只能表示“提升到首页观察 / 人工复核”，不能表示“下单”“自动推送执行”“可执行订单”或“交易机会已确认”。

## 四、当前 Watchlist 能力

本轮只读扫描确认，当前已有 `tm_rule_config` 表、`RuleConfigMapper`、`RuleConfigService` 和 `RuleConfigServiceImpl`。这些对象可以承载规则配置，`RuleController` 当前只有 `GET /api/rule/reload` 热加载接口。

本轮未发现已落地的 `push.watchlist.symbols` 配置键。也未发现针对 Watchlist Pool（观察库池）的独立配置读写 API。

本轮未发现以下 Push Watchlist API：

- `GET /api/rule/push-watchlist`
- `POST /api/rule/push-watchlist`
- `GET /api/rule/push-watchlist/audit`

本轮未发现独立的 watchlist audit（观察库审计）表、watchlist mapper（观察库映射器）、watchlist service（观察库服务）或 watchlist controller（观察库控制器）。

当前存在 Push Recheck Dispatch Config Audit（推送二次复核调度配置审计）相关表和接口，例如 `tm_push_recheck_dispatch_config_audit`、`PushRecheckDispatchConfigAuditMapper` 和 `/api/push/recheck/dispatch/config/audit`。但这属于 Push / Recheck（二次复核）调度配置审计，不是 Watchlist Pool（观察库池）审计。

当前 `dashboard.html` 已有 Display Slots（首页展示位）localStorage（浏览器本地存储）管理：

- `DEFAULT_DISPLAY_SLOT_SYMBOLS`
- `CUSTOM_LS_KEY = "trine_dashboard_custom_symbols"`
- `MODE_LS_KEY = "trine_dashboard_mode"`
- `loadCustomSymbols()`
- `saveCustomSymbols(...)`
- `buildDisplayList()`

`buildDisplayList()` 最终只取 `list.slice(0, 6)`，也就是页面最多展示 6 个资产。页面文案明确说明 Display Slots（首页展示位）只是首页展示优先级，Watchlist Pool（观察库池）仍是推送候选边界。

保守结论：

- 当前 Watchlist（观察库）能力主要是 Dashboard（首页工作台）展示口径、localStorage 本机自定义展示列表、文档边界、配置骨架和 fail-closed（失败关闭）语义。
- 当前 Watchlist（观察库）不是 Low-Frequency Scan（低频扫描）扫描器。
- 当前 Watchlist（观察库）不是 Opportunity Promote（机会提升）真实逻辑。
- 当前没有低频扫描 / 机会提升闭环。

因此必须明确：Low-Frequency Scan（低频扫描）/ Opportunity Promote（机会提升）仍未实现。

## 五、当前 Push / Opportunity 能力

当前已有 Push / Recheck（二次复核）相关能力：

- `tm_push_snapshot`（推送快照表）
- `tm_push_recheck_log`（推送二次复核日志表）
- `tm_push_recheck_dispatch_config`（推送二次复核调度配置表）
- `tm_push_recheck_dispatch_config_audit`（推送二次复核调度配置审计表）
- `PushSnapshotService`
- `PushRecheckService`
- `PushRecheckServiceImpl`
- `PushRecheckScheduler`
- `PushRecheckController`
- `PushSnapshotMapper`
- `PushRecheckLogMapper`
- `PushRecheckDispatchConfigMapper`
- `PushRecheckDispatchConfigAuditMapper`

当前 `PushSnapshotService` 会在权威分析主链落库后，在满足 `decision.isWorthOpening=true` 且已有 plan（计划）时写入 push snapshot（推送快照），状态为 `CAPTURED`。这仍然是快照和状态链，不是交易执行。

当前 `PushRecheckScheduler` 会对 `CAPTURED` / `RECHECK_VALID_WAITING` 状态的 push snapshot 做二次复核，并通过 `MarketQuoteClient.fetch24hTicker(symbol)` 读取报价。这个已有 scheduler（定时器）属于 Push / Recheck（二次复核）链，不是 Watchlist Low-Frequency Scan（观察库低频扫描）扫描器。P197-P200 不能直接新增或改造它来做观察库扫描。

当前 `PushRecheckController` 已有：

- `POST /api/push/recheck/{pushId}`
- `GET /api/push/recheck/dispatch/config`
- `POST /api/push/recheck/dispatch/config`
- `GET /api/push/recheck/dispatch/config/audit`
- `POST /api/push/recheck/replay`
- `GET /api/push/recheck/replay/summary`
- `GET /api/push/recheck/ops/overview`
- `GET /api/push/recheck/{pushId}/latest`
- `GET /api/push/recheck/{pushId}/logs`

这些接口是 Push / Recheck（二次复核）和 ops overview（运维概览）能力，不是 Watchlist Low-Frequency Scan（观察库低频扫描）能力，也不是 opportunity push execution（机会推送执行）。

当前已有 MissedOpportunity（错失机会）相关能力：

- `tm_missed_opportunity`
- `MissedOpportunityService`
- `MissedOpportunityServiceImpl`
- `MissedOpportunityController`
- `MissedOpportunityMapper`
- `MissedOpportunityQueryItemVO`

`MissedOpportunityServiceImpl` 的最小规则是：当决策值得开仓、没有触发 Hot Reset、没有失效、没有观察到同标的持仓、并且当前系统没有执行交易时，记录“错失机会”。这用于复盘和 review-only（只允许复核）展示，不是 opportunity promote（机会提升）执行，更不是自动交易。

当前 RiskActionGuardDisplay（风险动作保护展示）继续 fail-closed（失败关闭）：

- `opportunityPushAllowed=false`
- `reverseTradeAllowed=false`
- `newPositionAllowed=false`
- `marketOrderExitAllowed=false`
- `manualRiskReviewRequired=true`
- `notTradeInstruction=true`

当前没有发现真实 opportunity push execution（机会推送执行）。当前没有发现 order API（下单接口）或 execution API（执行接口）交易执行入口。当前不具备自动交易能力。

如果存在 push / recheck / ops overview（推送 / 二次复核 / 运维概览），也必须解释为 read-only（只读）/ ops（运维）/ review（复核）链路，不等于真实机会推送执行，不等于下单，不等于自动交易。

## 六、是否允许未来进入低频扫描 / 机会提升审计

P197 不允许直接写代码。

可以允许未来 P198 做 Watchlist Low-Frequency Scan / Opportunity Promote Authorization Gate（观察库低频扫描 / 机会提升授权门）。

P198 必须明确 P199 最多允许改哪些文件。P198 的职责不是实现扫描器，而是定义 P199 的最小安全范围。

如果 P199 未来实现，也只能做 read-only audit（只读审计）/ configuration（配置）/ documentation（文档）/ display（展示）：

- P199 不能创建 Low-Frequency Scan scheduler（低频扫描定时器）。
- P199 不能读取 runtime / live / external data（运行时 / 实时 / 外部数据）。
- P199 不能生成真实 entry / stop / TP / RR（入场 / 止损 / 止盈 / 盈亏比）。
- P199 不能创建 opportunity push execution（机会推送执行）。
- P199 不能升级 Readiness（可执行就绪）。
- P199 不能新增交易动作按钮。
- P199 不能接 order API（下单接口）/ execution API（执行接口）/ automation（自动化）/ auto-trading（自动交易）。

保守结论：可以进入 P198 授权门；不允许在 P197 或 P199 直接实现真实扫描、真实数据读取、真实推送执行或交易动作。

## 七、未来 P198/P199 最安全方向

P198/P199 最安全方向是先做文档或 read-only audit（只读审计），不直接接扫描器。

优先定义 Low-Frequency Scan（低频扫描）候选边界：

- only Watchlist Pool（仅限观察库池）
- exclude non-watchlist（排除非观察库资产）
- Display Slots only as UI priority（首页展示位只作为页面优先级）
- no default six push（不恢复默认六币推送）

优先定义 Opportunity Promote（机会提升）语义：

- 只是“提升到首页观察 / 人工复核”
- 不是下单
- 不是 Opportunity Push（机会推送）执行
- 不是可执行订单
- 不是 Readiness（可执行就绪）
- 不是真实交易机会确认

优先复用已有 Watchlist（观察库）config（配置）/ audit（审计）/ Dashboard（首页工作台）状态。但如果发现已有对象不够，也应该先停在授权门，不应该直接新增大范围 service（服务）、mapper（映射器）、schema（数据库结构）或 controller（控制器）。

P198/P199 不应新增 scheduler（定时器）。不应新增 data fetch（数据抓取）。不应新增 order API（下单接口）或 execution API（执行接口）。不应新增 auto-trading（自动交易）。

状态语义应保持：

- `REVIEW_ONLY`（只允许复核）
- `WATCH_ONLY`（仅观察）
- `BLOCKED`（禁止推进）
- `INCOMPLETE`（证据不完整）

如果需要真实扫描数据，必须另开数据源和调度审计，不能在 P197-P200 内直接做。

## 八、仍然禁止的路径

以下路径仍然禁止，不能借 P197-P200 的名义提前进入：

- default-six opportunity push（默认六币机会推送）
- Display Slots -> push candidates（首页展示位直接变成推送候选）
- non-watchlist asset -> push candidate（非观察库资产进入推送候选）
- low-frequency scan scheduler（低频扫描定时器）
- runtime / live / external data reads（运行时 / 实时 / 外部数据读取）
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

P198：Watchlist Low-Frequency Scan / Opportunity Promote Authorization Gate（观察库低频扫描 / 机会提升授权门）。

中文解释：

- P198 仍然不写代码。
- P198 只定义 P199 最小只读审计 / 配置展示 / 文档允许改哪些文件。
- P198 必须继续禁止 default-six opportunity push（默认六币机会推送）、non-watchlist push（非观察库推送）、scan scheduler（扫描定时器）、live data reads（实时数据读取）、真实点位、Readiness（可执行就绪）、交易按钮和 auto-trading（自动交易）。

P197 的推荐结论是：先做 P198 授权门，把 P199 的可改文件和禁止路径写清楚，再考虑是否做最小 read-only audit（只读审计）或配置展示。

## 十、P197 硬边界确认

本轮只新增一个 P197 审计文档。

本轮删除 `docs/P197.md` placeholder（占位文档）。

本轮不新增 Java。

本轮不新增测试。

本轮不改 production Java（生产 Java 代码）。

本轮不改现有测试。

本轮不改 `dashboard.html`。

本轮不新增 controller（控制器）/ endpoint（接口端点）/ API（接口）/ schema（数据库结构）/ config（配置）/ service（服务）/ mapper（映射器）。

本轮不读取 runtime / live / external data（运行时 / 实时 / 外部数据）。

本轮不生成真实 entry / stop / TP / RR（入场 / 止损 / 止盈 / 盈亏比）。

本轮不创建 opportunity push execution（机会推送执行）。

本轮不创建 close / reverse / buy / sell（平仓 / 反手 / 买入 / 卖出）动作。

本轮不升级 ExecutionPlan Readiness（执行计划可执行就绪）。

本轮不接 order（下单）/ execution（执行）/ scheduler（定时器）/ automation（自动化）/ auto-trading（自动交易）。
