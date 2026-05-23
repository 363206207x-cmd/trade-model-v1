# P190 Position Monitor Strong Reversal / Moving Stop Closure（持仓强反转 / 移动止损收口）

## 一、这一步是干嘛的

P190 是 Position Monitor（持仓监控）Strong Reversal（强反转）/ Moving Stop（移动止损）Closure（收口）。

P190 是 P187-P190 这一组的最后一步。这一组的目标不是让系统自动交易，而是把 Strong Reversal（强反转）/ Moving Stop（移动止损）放进 Review-Only Display（只允许复核展示）链路里，并确认边界没有越过人工复核。

本轮只新增一个 P190 收口文档，并删除 `docs/P190.md` placeholder（占位文档）。

本轮不写 Java。本轮不新增测试。本轮不改 `dashboard.html`。本轮只确认 P187-P189 已经完成只读展示链路。

本轮不生成真实 entry / stop / TP / RR（入场 / 止损 / 止盈 / 盈亏比）。本轮不创建 close / reverse / buy / sell（平仓 / 反手 / 买入 / 卖出）动作。本轮不自动修改 Stop Loss（止损）/ Moving Stop（移动止损）。本轮不升级 Readiness（可执行就绪）。本轮不接 order API（下单接口）、execution API（执行接口）、scheduler（定时器）、automation（自动化）或 auto-trading（自动交易）。

P190 的判断对象只有一个：P187-P189 是否已经把持仓强反转 / 移动止损这件事收进 Risk Action Guard（风险动作保护器）的只读复核展示里，并继续阻断自动执行。

## 二、P187 做了什么

P187 是 Position Monitor Strong Reversal / Moving Stop Review-Only Scope Audit（持仓强反转 / 移动止损只读复核范围审计）。

P187 确认当前项目已经存在 Position Monitor（持仓监控）基础对象：

- `PositionSyncService`
- `PositionSyncScheduler`
- `PositionProvider`
- `SimulatedPositionProvider`
- `BinancePositionProvider`
- `SwitchablePositionProvider`

P187 确认当前项目已经存在监控服务和告警对象：

- `MonitorController`
- `MonitorService`
- `MonitorAlertWriteService`

P187 确认当前项目已经存在 mapper（映射器）：

- `RealPositionMapper`
- `MonitorAlertMapper`

P187 确认当前项目已经存在数据库表：

- `tm_real_position`（真实持仓表）
- `tm_monitor_alert`（监控告警表）

P187 确认当前项目已经存在读模型和实体对象：

- `RealPositionVO`
- `MonitorAlertDO`

P187 确认 `DashboardDetailResponseVO` 和 `dashboard.html` 已有持仓字段 / Risk Action Guard（风险动作保护器）展示基础。

P187 确认 Position Monitor（持仓监控）当前是同步、展示、告警、记录和 Read-Only Risk Display（只读风险展示）基础，不是自动平仓 / 自动反手 / 自动买卖 / 自动移动止损。

P187 确认 Strong Reversal（强反转）/ Moving Stop（移动止损）仍未实现。

P187 确认 `DecisionResultMapper.countOpenSymbolsWithReverseSignal()` 只是统计读模型方向相反，不是 Strong Reversal（强反转）自动处理，更不是 reverse position（反手）。

P187 没有写代码。P187 没有新增测试。P187 没有改 dashboard（首页工作台）/ API（接口）/ schema（数据库结构）/ config（配置）。

P187 的结论是：可以继续推进 P188 Authorization Gate（授权门），但只能授权未来 P189 做只读复核展示，不能授权自动交易动作。

## 三、P188 做了什么

P188 是 Position Monitor Strong Reversal / Moving Stop Authorization Gate（持仓强反转 / 移动止损授权门）。

P188 授权 P189 只能围绕以下两个文件做最小接线：

- `src/main/java/org/example/trademodel/service/dashboard/DefaultRiskActionGuardDisplayAdapter.java`
- `src/test/java/org/example/trademodel/service/dashboard/DefaultRiskActionGuardDisplayAdapterTest.java`

P188 默认不允许改 `dashboard.html`。

P188 默认不允许改 `DashboardController.java`。

P188 默认不允许改 `MonitorController.java`。

P188 默认不允许改 `PositionSyncService` / `PositionSyncScheduler`。

P188 默认不允许改 `PositionProvider` / `BinancePositionProvider` / `SimulatedPositionProvider` / `SwitchablePositionProvider`。

P188 默认不允许改 `MonitorService` / `MonitorAlertWriteService`。

P188 默认不允许改 `RealPositionMapper` / `MonitorAlertMapper`。

P188 默认不允许改 `schema.sql`。

P188 没有写代码。P188 没有新增测试。

P188 的结论是：P189 可以写极小的 Review-Only Display（只允许复核展示）代码，但只能在 display / adapter（展示 / 适配器）层解释“为什么不能执行”和“为什么只能人工复核”。

## 四、P189 做了什么

P189 是 Position Monitor Strong Reversal / Moving Stop Minimal Wiring（持仓强反转 / 移动止损最小接线）。

P189 修改了：

- `src/main/java/org/example/trademodel/service/dashboard/DefaultRiskActionGuardDisplayAdapter.java`

P189 修改了：

- `src/test/java/org/example/trademodel/service/dashboard/DefaultRiskActionGuardDisplayAdapterTest.java`

P189 没有修改 `dashboard.html`。

P189 没有修改 `DashboardController.java`。

P189 没有修改 `MonitorController.java`。

P189 没有修改 `PositionSyncService` / `PositionSyncScheduler`。

P189 没有修改 `PositionProvider` / `BinancePositionProvider` / `SimulatedPositionProvider` / `SwitchablePositionProvider`。

P189 没有修改 `MonitorService` / `MonitorAlertWriteService`。

P189 没有修改 `RealPositionMapper` / `MonitorAlertMapper`。

P189 没有修改 `schema.sql`。

P189 没有改 API（接口）/ service（服务）/ mapper（映射器）/ schema（数据库结构）/ config（配置）。

P189 强化了 RiskActionGuardDisplay（风险动作保护展示）的只读提示：

- Strong Reversal（强反转）待确认。
- 原入场逻辑疑似失效也只能进入复核。
- Moving Stop（移动止损）需要人工复核。
- Strong Reversal（强反转）不等于反手或自动平仓。
- Moving Stop（移动止损）不等于自动改 Stop Loss（止损）。
- auto close / auto reverse / auto stop modification（自动平仓 / 自动反手 / 自动修改止损）均关闭。
- 不生成真实 entry / stop / TP / RR（入场 / 止损 / 止盈 / 盈亏比）。
- 不升级 Readiness（可执行就绪）。

P189 保持 `manualRiskReviewRequired=true`（必须人工风险复核）。

P189 保持 `notTradeInstruction=true`（不是交易指令）。

P189 没有生成真实 entry / stop / TP / RR（入场 / 止损 / 止盈 / 盈亏比）。

P189 没有创建 close / reverse / buy / sell（平仓 / 反手 / 买入 / 卖出）动作。

P189 没有自动修改 Stop Loss（止损）/ Moving Stop（移动止损）。

P189 没有升级 Readiness（可执行就绪）。

P189 没有新增交易按钮。

P189 没有接 auto-trading（自动交易）。

P189 的实质结果是：在已有 Risk Action Guard（风险动作保护器）只读展示里，把强反转和移动止损的人工复核含义说清楚，同时继续保持 fail-closed（失败关闭）。

## 五、必须重申的持仓风险原则

P190 收口必须重申这些持仓风险原则，避免后续任务把只读复核误解成自动执行：

- Strong Reversal（强反转）不等于直接反手。
- Strong Reversal（强反转）不等于自动平仓。
- Moving Stop（移动止损）不等于自动修改 Stop Loss（止损）。
- 风险高不能直接等同于立即止损、立即反手或立即开仓。
- Wick-only Risk（仅插针风险）不等于趋势反转。
- Stampede（踩踏）状态禁止 opportunity push（机会推送）、禁止反手、禁止新开仓。
- Liquidity Deterioration（流动性恶化）时不建议 market order execution（市价执行）一次性砍仓。
- 高风险但流动性正常，只能提示可减仓 / Moving Stop（移动止损）/ 降低杠杆，并且仍为人工复核。
- Strong Reversal（强反转）必须结合多周期确认、Liquidity State（流动性状态）、是否 Stampede（踩踏）、是否 Wick-only Risk（仅插针风险）、原入场逻辑是否失效。

这些规则只能作为人工复核材料，不能被解释成 production risk action（生产风控动作）。它们不能生成订单，不能触发平仓，不能触发反手，不能自动修改止损，也不能让执行计划进入可执行状态。

## 六、P187-P190 这组完成了什么

P187-P190 已经完成 Position Monitor Strong Reversal / Moving Stop Review-Only Display（持仓强反转 / 移动止损只允许复核展示）最小闭环。

用户未来可以通过 RiskActionGuardDisplay（风险动作保护展示）更清楚地看到：

- Strong Reversal（强反转）待确认。
- 原入场逻辑疑似失效。
- Moving Stop（移动止损）需要人工复核。
- Strong Reversal（强反转）为什么不能直接反手。
- Strong Reversal（强反转）为什么不能自动平仓。
- Moving Stop（移动止损）为什么不能自动改 Stop Loss（止损）。
- 为什么不生成真实点位。
- 为什么不升级 Readiness（可执行就绪）。

这只是只读解释层，不是 production risk action（生产风控动作）。

这只是人工复核材料，不是交易指令。

仍然不能自动平仓。

仍然不能自动反手。

仍然不能自动修改 Stop Loss（止损）。

仍然不能自动买入 / 自动卖出。

仍然不能生成真实交易点位。

仍然不能升级 Readiness（可执行就绪）。

仍然不能 auto-trading（自动交易）。

P187-P190 的价值是把“风险提醒”和“交易动作”之间的边界写清楚、接清楚、测清楚。它没有把任何风险提醒升级为交易执行。

## 七、P190 的结论

P187-P190 这一组完成。

完成的是 Position Monitor Strong Reversal / Moving Stop Review-Only Display（持仓强反转 / 移动止损只允许复核展示）闭环。

还不是 Strong Reversal（强反转）自动识别与自动处理。

还不是 Moving Stop（移动止损）自动调整。

还不是 auto close / auto reverse / auto buy / auto sell（自动平仓 / 自动反手 / 自动买入 / 自动卖出）。

还不是真实 entry / stop / TP / RR（入场 / 止损 / 止盈 / 盈亏比）。

还不是 ExecutionPlan Readiness（执行计划可执行就绪）。

还不是 auto-trading（自动交易）。

下一步必须回到 `docs/PROJECT_PROGRESS_INDEX.md` 的推荐路线，不能直接跳到自动交易、平仓、反手、自动改止损或真实点位。

P190 的收口结论很保守：这一组只完成了只读解释，不完成任何自动执行。

## 八、推荐下一步

推荐下一步为：

P191：Project Progress Index Refresh After Position Monitor Reversal Display（持仓强反转展示后项目总进度索引刷新）。

中文解释：

- P191 应该更新 `docs/PROJECT_PROGRESS_INDEX.md`。
- 因为 P187-P190 已经完成 Position Monitor Strong Reversal / Moving Stop Review-Only Display（持仓强反转 / 移动止损只允许复核展示）。
- 需要把 Position Monitor（持仓监控）进度从“同步 / 告警 / 记录基础 + 只读风险展示已完成”更新为“已完成强反转 / 移动止损只读展示，但未完成强反转自动识别 / 自动处理 / 自动改止损 / 自动交易”。
- P191 仍然只改文档，不写代码。

P191 不应该直接进入自动平仓、自动反手、自动修改止损、真实交易点位、Readiness（可执行就绪）或 auto-trading（自动交易）。

## 九、仍然禁止的路径

以下路径仍然禁止，不能借 P190 收口或 P187-P190 完成的名义进入：

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

## 十、P190 硬边界确认

本轮只新增一个 P190 收口文档。

本轮删除 `docs/P190.md` placeholder（占位文档）。

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
