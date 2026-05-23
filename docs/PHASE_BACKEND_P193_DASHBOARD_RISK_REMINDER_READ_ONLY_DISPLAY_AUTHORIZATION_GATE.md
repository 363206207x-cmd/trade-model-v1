# P193 Dashboard Risk Reminder Read-Only Display Authorization Gate（首页风险提醒只读展示授权门）

## 一、这一步是干嘛的

P193 是 Dashboard（首页工作台）Risk Reminder（风险提醒）Read-Only Display（只读展示）Authorization Gate（授权门）。

P193 是 P192-P195 Dashboard Risk Reminder Read-Only Display Pack（首页风险提醒只读展示包）的第二步。

本轮只新增一个 P193 授权门文档，并删除 `docs/P193.md` placeholder（占位文档）。

本轮不写 Java。本轮不新增测试。本轮不改 `dashboard.html`。本轮只规定 P194 如果写首页风险提醒 Read-Only Display（只读展示），允许改哪些文件。

本轮不生成真实 entry / stop / TP / RR（入场 / 止损 / 止盈 / 盈亏比）。本轮不创建 close / reverse / buy / sell（平仓 / 反手 / 买入 / 卖出）动作。本轮不自动修改 Stop Loss（止损）/ Moving Stop（移动止损）。本轮不升级 Readiness（可执行就绪）。本轮不接 order API（下单接口）、execution API（执行接口）、scheduler（定时器）、automation（自动化）或 auto-trading（自动交易）。

P193 的职责不是实现首页风险提醒，而是给 P194 划清最小代码授权边界：只能展示“为什么只能 Review-Only（只允许复核）”，不能把提醒变成交易动作。

## 二、P192 审计结论

P192 已完成 Dashboard Risk Reminder Read-Only Display Scope Audit（首页风险提醒只读展示范围审计）。

P192 已确认 `dashboard.html` 当前已有多个风险相关展示区域，但还不是集中命名的 Risk Reminder（风险提醒）区域。

P192 已确认 `DashboardDetailResponseVO` 已有以下字段：

- `riskActionGuardDisplay`（风险动作保护展示）
- `executionPlanDisplay`（执行计划展示）
- `planBoundaryDisplay`（计划边界展示）
- `paperObservationDisplay`（纸面观察展示）
- `sourceTrace`（证据来源追踪）
- `runtimeKlineContext`（运行时 K 线上下文）
- `derivativesRiskContext`（衍生品风险上下文）

P192 已确认 `DashboardController.dashboardDetail(...)` 已经组装以下对象：

- SourceTrace（证据来源追踪）
- PlanBoundaryDisplay（计划边界展示）
- ExecutionPlanDisplay（执行计划展示）
- RiskActionGuardDisplay（风险动作保护展示）
- PaperObservationDisplay（纸面观察展示）

P192 已确认首页已有这些安全文案：

- “非交易指令”
- “需要人工复核”
- “必须人工复核”
- “不连接 order API”
- “不触发自动交易动作”
- “不生成真实点位”
- “不生成真实 entry / stop / take-profit 数值”
- “高风险不等于直接止损或反手”

P192 已确认 `RiskActionGuardDisplayVO` 已有以下字段：

- `riskActionAdvice`（风险动作建议）
- `riskActionBlockingReason`（风险动作阻断原因）
- `liquidityState`（流动性状态）
- `stampedeDetected`（是否检测到踩踏）
- `wickOnlyRisk`（是否仅插针风险）
- `opportunityPushAllowed`（是否允许机会推送）
- `reverseTradeAllowed`（是否允许反手）
- `newPositionAllowed`（是否允许新开仓）
- `marketOrderExitAllowed`（是否允许市价退出）
- `manualRiskReviewRequired`（是否必须人工风险复核）
- `notTradeInstruction`（是否不是交易指令）

P192 已确认 `riskActionAdvice`（风险动作建议）和 `riskActionBlockingReason`（风险动作阻断原因）当前没有被集中渲染成统一 Risk Reminder（风险提醒）文案。

P192 已确认未来可以做集中只读风险提醒，但不能变成交易动作。

P192 的保守结论是：可以继续推进 P193 授权门，并允许未来 P194 做最小 Read-Only Display（只读展示），但必须继续禁止自动平仓、自动反手、自动修改止损、真实点位、Readiness（可执行就绪）、交易按钮和自动交易。

## 三、是否允许 P194 写代码

明确结论：可以允许 P194 写最小只读展示代码。

但 P194 必须极小。P194 只能围绕 `dashboard.html` 的只读文案 / 展示区域，或现有 display adapter（展示适配器）文案补强。

P194 不能新增 action API（动作接口）。P194 不能创建 close / reverse / buy / sell（平仓 / 反手 / 买入 / 卖出）动作。P194 不能自动修改 Stop Loss（止损）/ Moving Stop（移动止损）。P194 不能生成真实 entry / stop / TP / RR（入场 / 止损 / 止盈 / 盈亏比）。P194 不能升级 Readiness（可执行就绪）。P194 不能新增交易动作按钮。P194 不能接 auto-trading（自动交易）。

P194 的目标只允许是：把已有风险提醒字段展示得更集中、更清楚，让用户看到“为什么不能执行”和“为什么只能人工复核”。

## 四、P194 允许改哪些文件

P194 最多允许改 1-3 个文件。授权范围如下。

优先允许：

1. `src/main/resources/templates/dashboard.html`

仅在需要调整后端只读文案时允许：

2. `src/main/java/org/example/trademodel/service/dashboard/DefaultRiskActionGuardDisplayAdapter.java`
3. `src/test/java/org/example/trademodel/service/dashboard/DefaultRiskActionGuardDisplayAdapterTest.java`

默认规则：

- 默认优先只改 `dashboard.html`，如果只是把已有 `riskActionAdvice` / `riskActionBlockingReason` 显示出来。
- 如果需要调整后端只读文案，才允许改 `DefaultRiskActionGuardDisplayAdapter.java` 和对应测试。
- 默认不允许改 `DashboardController.java`。
- 默认不允许改 `DashboardDetailResponseVO.java`，除非发现已有字段完全不够；如果字段不够，应停止并另开授权。
- 默认不允许改 `MonitorController.java`。
- 默认不允许改 `PositionSyncService` / `PositionSyncScheduler`。
- 默认不允许改 `PositionProvider` / `BinancePositionProvider` / `SimulatedPositionProvider` / `SwitchablePositionProvider`。
- 默认不允许改 `MonitorService` / `MonitorAlertWriteService`。
- 默认不允许改 `RealPositionMapper` / `MonitorAlertMapper`。
- 默认不允许改 `schema.sql`。
- 不允许新增 API（接口）/ endpoint（接口端点）。
- 不允许新增任何 order（下单）/ execution（执行）/ auto-trading（自动交易）字段或按钮。

如果 P194 发现必须超出这些文件才能实现，应停止并另开授权，不应在 P194 内直接扩散。

## 五、P194 允许做什么

P194 只能做 Read-Only Display（只读展示）的最小补强：

- 在首页集中展示只读风险提醒。
- 展示 `riskActionAdvice`（风险动作建议）。
- 展示 `riskActionBlockingReason`（风险动作阻断原因）。
- 展示 `liquidityState`（流动性状态）。
- 展示 `stampedeDetected`（是否检测到踩踏）。
- 展示 `wickOnlyRisk`（是否仅插针风险）。
- 展示 `marketOrderExitAllowed=false`（不允许市价退出）。
- 展示 `opportunityPushAllowed=false` / `reverseTradeAllowed=false` / `newPositionAllowed=false`（不允许机会推送 / 反手 / 新开仓）。
- 展示 `manualRiskReviewRequired=true`（必须人工风险复核）。
- 展示 `notTradeInstruction=true`（不是交易指令）。
- 展示“高风险不等于直接止损”。
- 展示“强反转不等于直接反手或自动平仓”。
- 展示“移动止损不等于自动改止损”。
- 展示“踩踏禁止机会推送 / 反手 / 新开仓”。
- 展示“插针不等于趋势反转”。
- 展示“自动平仓 / 自动反手 / 自动改止损关闭”。
- 只解释“为什么不能执行”或“为什么只能复核”。
- 不生成交易指令。
- 不生成 Readiness（可执行就绪）。
- 不生成真实 entry / stop / TP / RR（入场 / 止损 / 止盈 / 盈亏比）。
- 不创建 close / reverse / buy / sell（平仓 / 反手 / 买入 / 卖出）动作。

P194 可以改善页面只读文案、集中展示布局、风险原因显示和 fail-closed（失败关闭）提示，但不能把任何字段映射成可执行动作。

## 六、P194 禁止做什么

P194 禁止做以下事情：

- 不允许新增 Controller（控制器）/ endpoint（接口端点）/ API（接口）。
- 不允许改 summary（摘要）接口。
- 不允许新增 mapper（映射器）。
- 不允许修改 schema（数据库结构）。
- 不允许修改 config（配置）。
- 不允许读取真实 runtime data（运行时数据）。
- 不允许读取 live market data（实时行情）。
- 不允许读取 external data（外部数据）。
- 不允许生成 entry / stop / TP / RR（入场 / 止损 / 止盈 / 盈亏比）。
- 不允许升级 ExecutionPlan Readiness（执行计划可执行就绪）。
- 不允许创建 close / reverse / buy / sell（平仓 / 反手 / 买入 / 卖出）动作。
- 不允许自动修改 Stop Loss（止损）/ Moving Stop（移动止损）。
- 不允许新增平仓 / 反手 / 买入 / 卖出按钮。
- 不允许接 order（下单）/ execution（执行）/ scheduler（定时器）/ automation（自动化）/ auto-trading（自动交易）。

## 七、仍然禁止的路径

以下路径仍然禁止，不能借 P193 或 P194 的名义进入：

- auto close position（自动平仓）
- auto reverse position（自动反手）
- auto buy / auto sell（自动买入 / 自动卖出）
- auto-modify stop loss（自动修改止损）
- market order execution（市价执行）
- order API（下单接口）
- execution API（执行接口）
- scheduler-triggered action（定时器触发动作）
- strong reversal -> reverse position（强反转直接反手）
- strong reversal -> auto close（强反转直接自动平仓）
- moving stop -> auto modify stop（移动止损自动改止损）
- risk high -> immediate stop loss（风险高直接止损）
- wick-only -> trend reversal（仅插针等于趋势反转）
- stampede -> opportunity push（踩踏状态推送机会）
- production risk action（生产风控动作）
- real entry / stop / TP / RR value generation（真实入场 / 止损 / 止盈 / 盈亏比生成）
- ExecutionPlan readiness upgrade（执行计划升级为可执行）
- dashboard trading action buttons（页面交易动作按钮）
- controller / endpoint / API action wiring（控制器 / 接口动作接线）
- schema / config / mapper changes（数据库 / 配置 / 映射改动）
- runtime / live / external data reads（运行时 / 实时 / 外部数据读取）
- WebClient / RestTemplate（网络请求工具）
- scheduler / automation / auto-trading（定时器 / 自动化 / 自动交易）

## 八、推荐下一步

推荐下一步为：

P194：Dashboard Risk Reminder Read-Only Display Minimal Wiring（首页风险提醒只读展示最小接线）。

P194 才可以开始最小只读展示代码。

P194 默认优先改 `dashboard.html`，只把已有风险提醒字段展示得更集中、更清楚。

P194 不能接 action API（动作接口）。P194 不能创建交易动作。P194 不能自动修改 Stop Loss（止损）。P194 不能升级 Readiness（可执行就绪）。P194 不能接 auto-trading（自动交易）。

P193 的结论是：可以授权 P194 做极小的首页风险提醒只读展示补强，但授权只到 `dashboard.html` 和必要时的 RiskActionGuardDisplay（风险动作保护展示）adapter（适配器）文案层，不授权生产风控动作、交易动作、止损修改、可执行状态或自动交易。

## 九、P193 硬边界确认

本轮只新增一个 P193 授权门文档。

本轮删除 `docs/P193.md` placeholder（占位文档）。

本轮不新增 Java。

本轮不新增测试。

本轮不改 production Java（生产 Java 代码）。

本轮不改现有测试。

本轮不改 `dashboard.html`。

本轮不新增 controller（控制器）/ endpoint（接口端点）/ API（接口）/ schema（数据库结构）/ config（配置）/ service（服务）/ mapper（映射器）。

本轮不读取 runtime / live / external data（运行时 / 实时 / 外部数据）。

本轮不生成真实 entry / stop / TP / RR（入场 / 止损 / 止盈 / 盈亏比）。

本轮不创建 close / reverse / buy / sell（平仓 / 反手 / 买入 / 卖出）动作。

本轮不自动修改 Stop Loss（止损）/ Moving Stop（移动止损）。

本轮不升级 ExecutionPlan Readiness（执行计划可执行就绪）。

本轮不接 order（下单）/ execution（执行）/ scheduler（定时器）/ automation（自动化）/ auto-trading（自动交易）。
