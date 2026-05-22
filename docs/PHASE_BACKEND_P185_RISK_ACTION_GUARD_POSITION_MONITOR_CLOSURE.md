# P185 Risk Action Guard / Position Monitor Closure（风险动作保护和持仓监控收口）

## 一、这一步是干嘛的

P185 是 Risk Action Guard / Position Monitor Closure（风险动作保护和持仓监控收口）。

P185 是 P182-P185 Risk Action Guard / Position Monitor Read-Only Risk Display Pack（风险动作保护 / 持仓监控只读风险展示包）的最后一步。

本轮不写 Java，不新增测试，不改 `dashboard.html`。本轮只确认 P182-P184 已经完成 Read-Only Risk Display（只读风险展示）链路。

本轮不生成真实 entry / stop / TP / RR（入场 / 止损 / 止盈 / 盈亏比）。本轮不创建 close / reverse / buy / sell（平仓 / 反手 / 买入 / 卖出）动作。本轮不升级 Readiness（可执行就绪）。本轮不接 order（下单）、execution（执行）、scheduler（定时器）、automation（自动化）或 auto-trading（自动交易）。

## 二、P182 做了什么

P182 是 Risk Action Guard / Position Monitor Scope Audit（风险动作保护和持仓监控范围审计）。

P182 确认 `RiskActionGuardDisplayVO` 存在。P182 确认 `DefaultRiskActionGuardDisplayAdapter` 和测试存在。

P182 确认以下 Position Monitor（持仓监控）和监控基础对象存在：

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

P182 确认 `tm_real_position`（真实持仓表）和 `tm_monitor_alert`（监控告警表）存在。

P182 确认这些能力目前是同步、展示、告警、记录，不是自动平仓、自动反手或自动下单。P182 确认 Strong Reversal（强反转）/ Moving Stop（移动止损）仍未实现。

P182 确认 Risk Action Guard（风险动作保护器）当前是 display（展示）/ adapter（适配器）/ fail-closed（失败关闭），不是 production risk action（生产风控动作）。

P182 没有写代码，没有新增测试，没有改 Dashboard（首页工作台）/ API（接口）/ schema（数据库结构）/ config（配置）。

## 三、P183 做了什么

P183 是 Risk Action Guard / Position Monitor Authorization Gate（风险动作保护和持仓监控授权门）。

P183 授权 P184 只能围绕 `DefaultRiskActionGuardDisplayAdapter` 和测试做最小接线。

P183 默认不允许改 `dashboard.html`。P183 默认不允许改 `DashboardController.java`。P183 默认不允许改 `MonitorController.java`。

P183 默认不允许改 `PositionSyncService` / `PositionSyncScheduler`。P183 默认不允许改 `PositionProvider` / `BinancePositionProvider` / `SimulatedPositionProvider`。

P183 默认不允许改 `MonitorService` / `MonitorAlertWriteService`。P183 默认不允许改 `RealPositionMapper` / `MonitorAlertMapper`。P183 默认不允许改 `schema.sql`。

P183 没有写代码，没有新增测试。

## 四、P184 做了什么

P184 修改：

- `src/main/java/org/example/trademodel/service/dashboard/DefaultRiskActionGuardDisplayAdapter.java`
- `src/test/java/org/example/trademodel/service/dashboard/DefaultRiskActionGuardDisplayAdapterTest.java`

P184 没有修改 `dashboard.html`。P184 没有修改 `DashboardController.java`。P184 没有修改 `MonitorController.java`。

P184 没有修改 `PositionSyncService` / `PositionSyncScheduler`。P184 没有修改 `PositionProvider` / `BinancePositionProvider` / `SimulatedPositionProvider`。

P184 没有修改 `MonitorService` / `MonitorAlertWriteService`。P184 没有修改 `RealPositionMapper` / `MonitorAlertMapper`。P184 没有修改 `schema.sql`。

P184 没有改 API（接口）/ service（服务）/ mapper（映射器）/ schema（数据库结构）/ config（配置）。

P184 补强了 `RiskActionGuardDisplay` 的只读风险状态表达。P184 增加或强化了这些只读原因 / 文案：

- `LIQUIDITY_DETERIORATION_REVIEW_ONLY`
- `STAMPEDE_REVIEW_ONLY`
- `WICK_ONLY_REVIEW_ONLY`
- `HIGH_RISK_REVIEW_ONLY`
- 流动性恶化时不做市价一次性砍仓
- 踩踏风险禁止机会推送、反手和新开仓
- 仅插针风险不等于趋势反转
- 强反转 / 移动止损仍未自动化，只能人工复核

P184 保持 `opportunityPushAllowed=false`。P184 保持 `reverseTradeAllowed=false`。P184 保持 `newPositionAllowed=false`。P184 保持 `marketOrderExitAllowed=false`。P184 保持 `manualRiskReviewRequired=true`。P184 保持 `notTradeInstruction=true`。

P184 没有生成真实 entry / stop / TP / RR（入场 / 止损 / 止盈 / 盈亏比）。P184 没有创建 close / reverse / buy / sell（平仓 / 反手 / 买入 / 卖出）动作。P184 没有升级 Readiness（可执行就绪）。P184 没有新增交易按钮。P184 没有接 auto-trading（自动交易）。

## 五、必须重申的风险动作分层规则

以下规则仍然必须保留，不能因为 P184 已完成只读展示而放松：

- 风险高不能直接等同于立即止损、立即反手或立即开仓。
- 风险高但流动性正常：只能提示可减仓、Moving Stop（移动止损）、降低杠杆，且仍为人工复核。
- 风险高且 Liquidity Deterioration（流动性恶化）：不建议市价一次性砍仓，应优先分批降风险、等待流动性恢复、只降杠杆。
- 风险高且存在 Stampede（踩踏）：进入极端压力锁定，禁止反手、禁止新开仓、禁止机会推送。
- 风险高但仅 Wick-only Risk（仅插针风险）：不直接判定趋势反转，不生成反向开仓计划，只做短线风险提醒和等待确认。
- Strong Reversal（强反转）不等于直接反手。
- 插针不等于趋势反转。
- Stampede（踩踏）状态禁止机会推送。

这些规则只能作为人工复核材料，不能被解释成自动风控动作。

## 六、P182-P185 这组完成了什么

Risk Action Guard / Position Monitor（风险动作保护和持仓监控）已经有 Read-Only Risk Display（只读风险展示）最小闭环。

用户未来可以通过 `RiskActionGuardDisplay` 更清楚地看到：

- 风险动作状态
- 风险动作建议
- 流动性状态
- 是否 Stampede（踩踏）
- 是否 Wick-only Risk（仅插针风险）
- 为什么只能人工复核
- 为什么不是交易指令
- 为什么不允许机会推送 / 反手 / 新开仓 / 市价退出

这只是只读解释层，不是 production risk action（生产风控动作）。这只是人工复核材料，不是交易指令。

仍然不能自动平仓。仍然不能自动反手。仍然不能自动买入 / 自动卖出。仍然不能生成真实交易点位。仍然不能升级 Readiness（可执行就绪）。仍然不能 auto-trading（自动交易）。

## 七、P185 的结论

P182-P185 这一组完成。

完成的是 Risk Action Guard / Position Monitor read-only risk display（风险动作保护 / 持仓监控只读风险展示）闭环。

还不是 production risk action（生产风控动作）。还不是 Strong Reversal（强反转）自动识别与自动处理。还不是 Moving Stop（移动止损）自动调整。还不是 auto close / auto reverse / auto buy / auto sell（自动平仓 / 自动反手 / 自动买入 / 自动卖出）。

还不是真实 entry / stop / TP / RR（入场 / 止损 / 止盈 / 盈亏比）。还不是 ExecutionPlan Readiness（执行计划可执行就绪）。还不是 auto-trading（自动交易）。

下一步必须回到 `docs/PROJECT_PROGRESS_INDEX.md` 的推荐路线，不能直接跳自动交易、平仓、反手或真实点位。

## 八、推荐下一步

推荐下一步为：

P186：Project Progress Index Refresh After Risk Action Guard Display（风险动作保护展示后项目总进度索引刷新）

P186 应该更新 `docs/PROJECT_PROGRESS_INDEX.md`。

因为 P182-P185 已经完成 Risk Action Guard / Position Monitor（风险动作保护和持仓监控）只读风险展示，需要把 Risk Action Guard（风险动作保护器）和 Position Monitor（持仓监控）进度从“display adapter（展示适配器）/ 同步 / 告警基础存在”更新为“已完成只读风险展示，但未完成 production risk action（生产风控动作）/ Strong Reversal（强反转）/ Moving Stop（移动止损）/ auto-trading（自动交易）”。

P186 仍然只改文档，不写代码。

## 九、仍然禁止的路径

以下路径仍然禁止：

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

P185 的最终收口判断：P182-P185 完成了只读风险展示，不完成也不授权任何交易动作。
