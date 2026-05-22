# P183 Risk Action Guard / Position Monitor Authorization Gate（风险动作保护和持仓监控授权门）

## 一、这一步是干嘛的

P183 是 Risk Action Guard / Position Monitor Authorization Gate（风险动作保护和持仓监控授权门）。

P183 是 P182-P185 Risk Action Guard / Position Monitor Read-Only Risk Display Pack（风险动作保护 / 持仓监控只读风险展示包）的第二步。

本轮不写 Java，不新增测试，不改 `dashboard.html`。本轮只规定 P184 如果写最小 Read-Only Risk Display（只读风险展示），允许改哪些文件。

本轮不生成真实 entry / stop / TP / RR（入场 / 止损 / 止盈 / 盈亏比）。本轮不创建 close / reverse / buy / sell（平仓 / 反手 / 买入 / 卖出）动作。本轮不升级 Readiness（可执行就绪）。本轮不接 order（下单）、execution（执行）、scheduler（定时器）、automation（自动化）或 auto-trading（自动交易）。

## 二、P182 审计结论

P182 已确认 RiskActionGuardDisplayVO（风险动作保护展示对象）存在。

P182 已确认 `DefaultRiskActionGuardDisplayAdapter` 和 `DefaultRiskActionGuardDisplayAdapterTest` 存在。

P182 已确认以下 Risk Action Guard（风险动作保护器）字段存在：

- `riskActionGuardStatus`
- `riskActionAdvice`
- `liquidityState`
- `stampedeDetected`
- `wickOnlyRisk`
- `opportunityPushAllowed`
- `reverseTradeAllowed`
- `newPositionAllowed`
- `marketOrderExitAllowed`
- `manualRiskReviewRequired`
- `notTradeInstruction`

P182 已确认以下 Position Monitor（持仓监控）和监控基础对象存在：

- `PositionSyncService`
- `PositionSyncScheduler`
- `PositionProvider`
- `BinancePositionProvider`
- `SimulatedPositionProvider`
- `MonitorController`
- `MonitorService`
- `MonitorAlertWriteService`
- `RealPositionMapper`
- `MonitorAlertMapper`

P182 已确认 `tm_real_position`（真实持仓表）和 `tm_monitor_alert`（监控告警表）存在。

P182 的关键结论是：这些能力目前是同步、展示、告警、记录，不是自动平仓、自动反手或自动下单。`PositionSyncScheduler` 只是同步持仓快照，不是 scheduler-triggered action（定时器触发交易动作）。`RealPositionMapper.closeMissingOpenPositions(...)` 只是数据库状态记录，不是交易所平仓。

P182 已确认 Strong Reversal（强反转）/ Moving Stop（移动止损）仍未实现。P182 也已确认 Risk Action Guard（风险动作保护器）当前是 display（展示）/ adapter（适配器）/ fail-closed（失败关闭），不是 production risk action（生产风控动作）。

## 三、必须继承的风险动作分层规则

P184 必须继承以下风险动作分层规则：

- 风险高不能直接等同于立即止损、立即反手或立即开仓。
- 风险高但流动性正常：只能提示可减仓、Moving Stop（移动止损）、降低杠杆，且仍为 REVIEW_ONLY（只允许复核）。
- 风险高且 Liquidity Deterioration（流动性恶化）：不建议市价一次性砍仓，应优先分批降风险、等待流动性恢复、只降杠杆。
- 风险高且存在 Stampede（踩踏）：进入极端压力锁定，禁止反手、禁止新开仓、禁止 opportunity push（机会推送）。
- 风险高但仅 Wick-only Risk（仅插针风险）：不直接判定趋势反转，不生成反向开仓计划，只做短线风险提醒和等待确认。
- Strong Reversal（强反转）不等于直接反手。
- 插针不等于趋势反转。
- Stampede（踩踏）状态禁止 opportunity push（机会推送）。

这些规则只能进入 Read-Only Risk Display（只读风险展示），不能变成交易动作、交易按钮或执行权限。

## 四、是否允许 P184 写代码

可以允许 P184 写最小只读风险展示代码。

但 P184 必须极小。P184 只能围绕 Dashboard Detail（首页详情）/ RiskActionGuardDisplay（风险动作保护展示）/ Position Monitor read-only display（持仓监控只读展示）/ adapter（适配器）层。

P184 不能新增 action API（动作接口）。P184 不能创建 close / reverse / buy / sell（平仓 / 反手 / 买入 / 卖出）动作。P184 不能生成真实 entry / stop / TP / RR（入场 / 止损 / 止盈 / 盈亏比）。P184 不能升级 Readiness（可执行就绪）。P184 不能新增交易动作按钮。P184 不能接 auto-trading（自动交易）。

## 五、P184 允许改哪些文件

P184 最多允许改 1-4 个文件，授权范围如下。

优先允许：

1. `src/main/java/org/example/trademodel/service/dashboard/DefaultRiskActionGuardDisplayAdapter.java`
2. `src/test/java/org/example/trademodel/service/dashboard/DefaultRiskActionGuardDisplayAdapterTest.java`

仅在必要时允许：

3. `src/main/java/org/example/trademodel/vo/DashboardDetailResponseVO.java`

仅在必要时允许：

4. 实际存在的 dashboard detail assembler / service（首页详情组装服务）文件。

当前只读扫描没有发现名为 `DefaultDashboardDetailService.java` 的独立文件，现有 detail 组装主要在 `DashboardController.dashboardDetail(...)` 里。但 P184 默认不允许改 `DashboardController.java`。如果 P184 发现必须修改 controller（控制器）才能完成，应停止并另开授权，不应在 P184 内直接修改。

P184 的默认边界：

- 如果 `RiskActionGuardDisplayVO` 已有足够字段，就默认不改 `DashboardDetailResponseVO.java`。
- 默认不允许改 `dashboard.html`。
- 默认不允许改 `DashboardController.java`。
- 默认不允许改 `MonitorController.java`。
- 默认不允许改 `PositionSyncService` / `PositionSyncScheduler`。
- 默认不允许改 `PositionProvider` / `BinancePositionProvider` / `SimulatedPositionProvider`。
- 默认不允许改 `MonitorService` / `MonitorAlertWriteService`。
- 默认不允许改 `RealPositionMapper` / `MonitorAlertMapper`。
- 默认不允许改 `schema.sql`。
- 不允许新增 API（接口）/ endpoint（接口端点）。
- 不允许新增任何 order（下单）/ execution（执行）/ auto-trading（自动交易）字段或按钮。

## 六、P184 允许做什么

P184 只能做 Read-Only Risk Display（只读风险展示）的最小补强：

- 让 `RiskActionGuardDisplay` 更明确展示只读风险状态。
- 展示 risk action status（风险动作状态）。
- 展示 risk action advice（风险动作建议），但必须是人工复核提醒，不是动作指令。
- 展示 `liquidityState`（流动性状态）。
- 展示 `stampedeDetected`（是否踩踏）。
- 展示 `wickOnlyRisk`（是否仅插针风险）。
- 展示 `opportunityPushAllowed=false`。
- 展示 `reverseTradeAllowed=false`。
- 展示 `newPositionAllowed=false`。
- 展示 `marketOrderExitAllowed=false`。
- 展示 `manualRiskReviewRequired=true`。
- 展示 `notTradeInstruction=true`。
- 展示 Strong Reversal（强反转）/ Moving Stop（移动止损）仍未自动化，只能人工复核。
- 只解释“为什么不能执行”或“为什么只能复核”。
- 不生成交易指令。
- 不生成 Readiness（可执行就绪）。
- 不生成真实 entry / stop / TP / RR（入场 / 止损 / 止盈 / 盈亏比）。
- 不创建 close / reverse / buy / sell（平仓 / 反手 / 买入 / 卖出）动作。

P184 可以补强 fail-closed（失败关闭）文案、阻断原因、人工复核原因或默认只读状态，但不能把风险提醒升级成交易动作。

## 七、P184 禁止做什么

P184 禁止做以下事情：

- 不允许改 `dashboard.html`。
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
- 不允许新增平仓 / 反手 / 买入 / 卖出按钮。
- 不允许接 order（下单）/ execution（执行）/ scheduler（定时器）/ automation（自动化）/ auto-trading（自动交易）。

## 八、仍然禁止的路径

以下路径仍然禁止，不能借 P183 或 P184 的名义进入：

- auto close position（自动平仓）
- auto reverse position（自动反手）
- auto buy / auto sell（自动买入 / 自动卖出）
- market order execution（市价执行）
- order API（下单接口）
- execution API（执行接口）
- scheduler-triggered action（定时器触发动作）
- risk high -> immediate stop loss（风险高直接止损）
- risk high -> reverse position（风险高直接反手）
- wick-only -> trend reversal（仅插针等于趋势反转）
- stampede -> opportunity push（踩踏状态推送机会）
- production candidate generation（生产候选交易计划生成）
- real entry / stop / TP / RR value generation（真实入场 / 止损 / 止盈 / 盈亏比生成）
- ExecutionPlan readiness upgrade（执行计划升级为可执行）
- dashboard trading action buttons（页面交易动作按钮）
- controller / endpoint / API action wiring（控制器 / 接口动作接线）
- schema / config / mapper changes（数据库 / 配置 / 映射改动）
- runtime / live / external data reads（运行时 / 实时 / 外部数据读取）
- WebClient / RestTemplate（网络请求工具）
- scheduler / automation / auto-trading（定时器 / 自动化 / 自动交易）

## 九、推荐下一步

推荐下一步为：

P184：Risk Action Guard / Position Monitor Minimal Wiring（风险动作保护和持仓监控最小接线）

P184 才可以开始最小只读风险展示代码。

P184 只能做 Dashboard Detail（首页详情）/ RiskActionGuardDisplay（风险动作保护展示）/ Position Monitor read-only display（持仓监控只读展示）/ adapter（适配器）层。

P184 不能改 `dashboard.html`。P184 不能接 action API（动作接口）。P184 不能创建交易动作。P184 不能升级 Readiness（可执行就绪）。P184 不能接 auto-trading（自动交易）。

P183 的结论是：可以授权 P184 做极小的只读风险展示补强，但授权只到 display / adapter（展示 / 适配器）层，不授权生产风控动作、交易动作、可执行状态或自动交易。
