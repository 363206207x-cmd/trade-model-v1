# P188 Position Monitor Strong Reversal / Moving Stop Authorization Gate（持仓强反转 / 移动止损授权门）

## 一、这一步是干嘛的

P188 是 Position Monitor（持仓监控）Strong Reversal（强反转）/ Moving Stop（移动止损）Authorization Gate（授权门）。

P188 是 P187-P190 Position Monitor Strong Reversal / Moving Stop Review-Only Pack（持仓强反转 / 移动止损只读复核包）的第二步。

本轮不写 Java。本轮不新增测试。本轮不改 `dashboard.html`。本轮只规定 P189 如果写最小 Review-Only Display（只允许复核展示），允许改哪些文件。

本轮不生成真实 entry / stop / TP / RR（入场 / 止损 / 止盈 / 盈亏比）。本轮不创建 close / reverse / buy / sell（平仓 / 反手 / 买入 / 卖出）动作。本轮不自动修改 Stop Loss（止损）/ Moving Stop（移动止损）。本轮不升级 Readiness（可执行就绪）。本轮不接 order API（下单接口）、execution API（执行接口）、scheduler（定时器）、automation（自动化）或 auto-trading（自动交易）。

P188 的职责不是实现强反转或移动止损，而是给 P189 划清最小代码授权边界：只允许展示“需要人工复核”，不允许进入自动执行。

## 二、P187 审计结论

P187 已完成 Position Monitor Strong Reversal / Moving Stop Review-Only Scope Audit（持仓强反转 / 移动止损只读复核范围审计）。

P187 已确认 Position Monitor（持仓监控）基础对象存在：

- `PositionSyncService`
- `PositionSyncScheduler`
- `PositionProvider`
- `SimulatedPositionProvider`
- `BinancePositionProvider`
- `SwitchablePositionProvider`

P187 已确认监控服务和告警对象存在：

- `MonitorController`
- `MonitorService`
- `MonitorAlertWriteService`

P187 已确认 mapper（映射器）存在：

- `RealPositionMapper`
- `MonitorAlertMapper`

P187 已确认数据库表存在：

- `tm_real_position`（真实持仓表）
- `tm_monitor_alert`（监控告警表）

P187 已确认读模型和实体对象存在：

- `RealPositionVO`
- `MonitorAlertDO`

P187 已确认 `DashboardDetailResponseVO` 和 `dashboard.html` 已有持仓字段 / Risk Action Guard（风险动作保护器）展示基础。持仓字段主要通过 `DecisionResultVO` / Dashboard Detail（首页详情）读模型承载，例如 `hasOpenPosition`、`positionSide`、`avgOpenPrice`、`positionOpenTime`、`positionQuantity`、`unrealizedPnlPct`、`positionStatus`、`markPrice`、`breakEvenPrice`、`liquidationPrice`。

P187 已确认当前 Position Monitor（持仓监控）是同步、展示、告警、记录和 Read-Only Risk Display（只读风险展示）基础，不是自动平仓 / 自动反手 / 自动买卖 / 自动移动止损。

P187 已确认 Strong Reversal（强反转）/ Moving Stop（移动止损）仍未实现。

P187 已确认 `DecisionResultMapper.countOpenSymbolsWithReverseSignal()` 只是统计读模型里持仓方向和最新 bias（方向偏向）相反的数量，不是 Strong Reversal（强反转）自动处理，更不是反手。

因此，P188 只能授权未来 P189 做最小只读展示，不能授权生产风控动作或交易动作。

## 三、必须继承的持仓风险原则

P189 必须继承以下持仓风险原则：

- Strong Reversal（强反转）不等于直接反手。
- Strong Reversal（强反转）不等于自动平仓。
- Moving Stop（移动止损）不等于自动修改 Stop Loss（止损）。
- 风险高不能直接等同于立即止损、立即反手或立即开仓。
- Wick-only Risk（仅插针风险）不等于趋势反转。
- Stampede（踩踏）状态禁止 opportunity push（机会推送）、禁止反手、禁止新开仓。
- Liquidity Deterioration（流动性恶化）时不建议 market order execution（市价执行）一次性砍仓。
- 高风险但流动性正常，只能提示可减仓 / Moving Stop（移动止损）/ 降低杠杆，并且仍为人工复核。
- Strong Reversal（强反转）必须结合多周期确认、流动性状态、是否 Stampede（踩踏）、是否 Wick-only Risk（仅插针风险）、原入场逻辑是否失效。

这些原则只能进入 Review-Only Display（只允许复核展示），不能被解释为自动执行条件。

## 四、是否允许 P189 写代码

明确结论：可以允许 P189 写最小只读展示代码。

但 P189 必须极小。P189 只能围绕 Dashboard Detail（首页详情）/ RiskActionGuardDisplay（风险动作保护展示）/ Position Monitor read-only display（持仓监控只读展示）/ adapter（适配器）层。

P189 不能新增 action API（动作接口）。P189 不能创建 close / reverse / buy / sell（平仓 / 反手 / 买入 / 卖出）动作。P189 不能自动修改 Stop Loss（止损）。P189 不能生成真实 entry / stop / TP / RR（入场 / 止损 / 止盈 / 盈亏比）。P189 不能升级 Readiness（可执行就绪）。P189 不能新增交易动作按钮。P189 不能接 auto-trading（自动交易）。

P189 的目标只允许是：让已有只读展示更明确表达“强反转 / 移动止损仍然需要人工复核，不能执行”。

## 五、P189 允许改哪些文件

P189 最多允许改 1-4 个文件。授权范围如下。

优先允许：

1. `src/main/java/org/example/trademodel/service/dashboard/DefaultRiskActionGuardDisplayAdapter.java`
2. `src/test/java/org/example/trademodel/service/dashboard/DefaultRiskActionGuardDisplayAdapterTest.java`

仅在必要时允许：

3. `src/main/java/org/example/trademodel/vo/DashboardDetailResponseVO.java`

仅在必要时允许第 4 个文件槽位，二选一，不得两个都改：

4. `src/main/java/org/example/trademodel/service/dashboard/DefaultExecutionPlanDisplayAdapter.java`，或实际存在的 dashboard detail display adapter（首页详情展示适配器）文件。

默认规则：

- 如果 `RiskActionGuardDisplayVO` 已有足够字段，就默认不改 `DashboardDetailResponseVO.java`。
- 默认不允许改 `dashboard.html`。
- 默认不允许改 `DashboardController.java`。
- 默认不允许改 `MonitorController.java`。
- 默认不允许改 `PositionSyncService` / `PositionSyncScheduler`。
- 默认不允许改 `PositionProvider` / `BinancePositionProvider` / `SimulatedPositionProvider` / `SwitchablePositionProvider`。
- 默认不允许改 `MonitorService` / `MonitorAlertWriteService`。
- 默认不允许改 `RealPositionMapper` / `MonitorAlertMapper`。
- 默认不允许改 `schema.sql`。
- 不允许新增 API（接口）/ endpoint（接口端点）。
- 不允许新增任何 order（下单）/ execution（执行）/ auto-trading（自动交易）字段或按钮。

如果 P189 发现必须超出这些文件才能实现，应停止并另开授权，不应在 P189 内直接扩散。

## 六、P189 允许做什么

P189 只能做 Review-Only Display（只允许复核展示）的最小补强：

- 让 `RiskActionGuardDisplay` 或持仓风险展示更明确表达 Strong Reversal（强反转）/ Moving Stop（移动止损）仍是 review-only（只允许复核）。
- 展示 strong reversal pending confirmation（强反转待确认）。
- 展示 entry logic possibly invalidated（原入场逻辑疑似失效）。
- 展示 moving stop review required（移动止损需要人工复核）。
- 展示 Strong Reversal（强反转）不等于反手。
- 展示 Moving Stop（移动止损）不等于自动改 Stop Loss（止损）。
- 展示 auto close（自动平仓）/ auto reverse（自动反手）/ auto stop modification（自动修改止损）都关闭。
- 展示 `manualRiskReviewRequired=true`。
- 展示 `notTradeInstruction=true`。
- 只解释“为什么不能执行”或“为什么只能复核”。
- 不生成交易指令。
- 不生成 Readiness（可执行就绪）。
- 不生成真实 entry / stop / TP / RR（入场 / 止损 / 止盈 / 盈亏比）。
- 不创建 close / reverse / buy / sell（平仓 / 反手 / 买入 / 卖出）动作。

P189 可以补强 fail-closed（失败关闭）文案、阻断原因、人工复核原因或默认只读状态，但不能把风险提醒升级成交易动作。

## 七、P189 禁止做什么

P189 禁止做以下事情：

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
- 不允许自动修改 Stop Loss（止损）/ Moving Stop（移动止损）。
- 不允许新增平仓 / 反手 / 买入 / 卖出按钮。
- 不允许接 order（下单）/ execution（执行）/ scheduler（定时器）/ automation（自动化）/ auto-trading（自动交易）。

## 八、仍然禁止的路径

以下路径仍然禁止，不能借 P188 或 P189 的名义进入：

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

## 九、推荐下一步

推荐下一步为：

P189：Position Monitor Strong Reversal / Moving Stop Minimal Wiring（持仓强反转 / 移动止损最小接线）。

P189 才可以开始最小只读展示代码。

P189 只能做 Dashboard Detail（首页详情）/ RiskActionGuardDisplay（风险动作保护展示）/ Position Monitor read-only display（持仓监控只读展示）/ adapter（适配器）层。

P189 不能改 `dashboard.html`。P189 不能接 action API（动作接口）。P189 不能创建交易动作。P189 不能自动修改 Stop Loss（止损）。P189 不能升级 Readiness（可执行就绪）。P189 不能接 auto-trading（自动交易）。

P188 的结论是：可以授权 P189 做极小的只读展示补强，但授权只到 display / adapter（展示 / 适配器）层，不授权生产风控动作、交易动作、止损修改、可执行状态或自动交易。
