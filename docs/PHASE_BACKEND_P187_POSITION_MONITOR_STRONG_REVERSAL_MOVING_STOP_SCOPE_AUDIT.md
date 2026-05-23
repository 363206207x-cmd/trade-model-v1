# P187 Position Monitor Strong Reversal / Moving Stop Review-Only Scope Audit（持仓强反转 / 移动止损只读复核范围审计）

## 一、这一步是干嘛的

P187 是 Position Monitor（持仓监控）Strong Reversal（强反转）/ Moving Stop（移动止损）Review-Only（只允许复核）Scope Audit（范围审计）。

P187 是 P187-P190 Position Monitor Strong Reversal / Moving Stop Review-Only Pack（持仓强反转 / 移动止损只读复核包）的第一步。这个任务包的最大安全边界是：先审计范围，再做授权门，再做最小只读展示，最后收口。

本轮只新增一个 P187 审计文档，并删除 `docs/P187.md` placeholder（占位文档）。

本轮不写 Java。本轮不新增测试。本轮不接 dashboard（首页工作台）新功能。本轮只判断未来持仓强反转和移动止损是否可以进入只读复核展示路径。

本轮不生成真实 entry / stop / TP / RR（入场 / 止损 / 止盈 / 盈亏比）。本轮不创建 close / reverse / buy / sell（平仓 / 反手 / 买入 / 卖出）动作。本轮不自动修改 stop loss / moving stop（止损 / 移动止损）。本轮不升级 ExecutionPlan Readiness（执行计划可执行就绪）。本轮不接 order API（下单接口）、execution API（执行接口）、scheduler（定时器）、automation（自动化）或 auto-trading（自动交易）。

P187 的判断对象不是“系统能否交易”，而是“未来是否可以把强反转和移动止损作为只读风险复核信息展示出来”。答案只能围绕 Review-Only（只允许复核）展示，不允许绕到生产风控动作。

## 二、P186 / PROJECT_PROGRESS_INDEX 的依据

P187 必须以 `docs/PROJECT_PROGRESS_INDEX.md` 为总索引依据。

P186 已完成 Project Progress Index Refresh After Risk Action Guard Display（风险动作保护展示后项目总进度索引刷新）。P186 的核心结论是：项目的只读展示层更完整，但自动交易和真实执行链路仍没有闭环。

`docs/PROJECT_PROGRESS_INDEX.md` 明确写清楚：

- SourceTrace（证据来源追踪）read-only display（只读展示）已完成。
- BoundaryCandidate（边界候选交易计划）read-only candidate display（只读候选展示）已完成。
- ExecutionPlan（执行计划）review-only display（只允许复核展示）已完成。
- Risk Action Guard（风险动作保护器）/ Position Monitor（持仓监控）Read-Only Risk Display（只读风险展示）已完成。
- 如果目标是个人可用最快路径，总索引推荐进入 Position Monitor Strong Reversal / Moving Stop Review-Only Audit（持仓强反转 / 移动止损只读审计）。

但总索引也同时明确暂停以下能力：

- Strong Reversal（强反转）automation（自动化）。
- Moving Stop（移动止损）automation（自动化）。
- auto close / auto reverse / auto buy / auto sell（自动平仓 / 自动反手 / 自动买入 / 自动卖出）。
- ExecutionPlan Readiness（执行计划可执行就绪）。
- production risk action（生产风控动作）。
- auto-trading（自动交易）。

因此 P187 只能审计 Review-Only（只允许复核）展示路径。P187 不能审计自动平仓、自动反手、自动修改止损或真实风控动作执行。

## 三、必须继承的持仓风险原则

P187 必须继承 P182-P186 已经形成的持仓风险原则：

- Strong Reversal（强反转）不等于直接反手。
- Strong Reversal（强反转）不等于自动平仓。
- Moving Stop（移动止损）不等于自动修改止损。
- 风险高不能直接等同于立即止损、立即反手或立即开仓。
- Wick-only Risk（仅插针风险）不等于趋势反转。
- Stampede（踩踏）状态禁止 opportunity push（机会推送）、禁止反手、禁止新开仓。
- Liquidity Deterioration（流动性恶化）时不建议 market order execution（市价执行）一次性砍仓。
- 高风险但流动性正常，只能提示可减仓 / 移动止损 / 降低杠杆，并且仍为人工复核。
- 强反转必须结合多周期确认、Liquidity State（流动性状态）、是否 Stampede（踩踏）、是否 Wick-only Risk（仅插针风险）、原入场逻辑是否失效。

这些原则只能进入解释层和复核层。它们不能被映射成反手按钮、平仓按钮、下单接口、自动修改止损或自动交易。

## 四、当前 Position Monitor（持仓监控）能力

本轮只读扫描确认，当前已有 Position Monitor（持仓监控）基础能力：

- `PositionSyncService` 已存在，用于从 PositionProvider（持仓提供器）读取 open positions（未平持仓）并写入 `tm_real_position`（真实持仓表）。
- `PositionSyncScheduler` 已存在，用 `@Scheduled` 定时触发持仓同步。这里的 scheduler（定时器）只用于同步快照，不是 scheduler-triggered action（定时器触发交易动作）。
- `PositionProvider`、`SimulatedPositionProvider`、`BinancePositionProvider`、`SwitchablePositionProvider` 已存在。`SimulatedPositionProvider` 提供模拟持仓；`BinancePositionProvider` 读取 Binance（币安）持仓快照；`SwitchablePositionProvider` 在配置或凭证不足时会回退到模拟持仓。
- `MonitorController` 已存在，目前 `/api/monitor/status` 只返回监控状态文本，不是交易动作接口。
- `MonitorService` / `MonitorServiceImpl` 已存在，用于读取 recent alerts（最近告警）。
- `MonitorAlertWriteService` / `MonitorAlertWriteServiceImpl` 已存在，用于把高风险、数据质量不足、AI 冲突升高、多周期弱收敛等写入 `tm_monitor_alert`（监控告警表）。
- `RealPositionMapper` 已存在，用于查询、更新、插入 `tm_real_position`，并将同步快照中消失的 OPEN（未平）持仓标记为 CLOSED（已关闭）。
- `MonitorAlertMapper` 已存在，用于写入、查询和节流 `tm_monitor_alert`。
- `RealPositionVO` 已存在，用于承载持仓读模型字段。
- `MonitorAlertDO` 已存在，用于承载监控告警记录。
- `src/main/resources/schema.sql` 已有 `tm_real_position`（真实持仓表）和 `tm_monitor_alert`（监控告警表）。

需要避免误判：

- `RealPositionMapper.closeMissingOpenPositions(...)` 只是数据库状态记录，不是向交易所发送平仓订单。
- `PositionSyncScheduler` 只是同步，不是自动执行器。
- `MonitorAlertWriteServiceImpl` 只是写告警，不是执行交易。
- `DecisionServiceImpl` 会把 `tm_real_position` 的 OPEN（未平）持仓合并到 Dashboard（首页工作台）读模型里，但这仍是展示字段，不是交易动作。
- `DecisionResultMapper.countOpenSymbolsWithReverseSignal()` 能统计持仓方向和最新 bias（方向偏向）相反的数量，但它只是统计 read model（读模型）信号，不是 Strong Reversal（强反转）自动处理，更不是反手。

本轮没有发现以下命名或能力：

- 未发现 `PositionMonitorRecord` / `tm_position_monitor_record` 这类独立持仓监控记录对象或表。
- 未发现 `PositionTradeResult` / `tm_position_trade_result` 这类持仓交易结果对象或表。
- 未发现 Strong Reversal（强反转）真实识别、真实处理或生产动作逻辑。
- 未发现 Moving Stop（移动止损）真实计算、真实调整或自动修改止损逻辑。

保守结论：当前 Position Monitor（持仓监控）更像同步、展示、告警、记录和只读风险展示基础，不等于自动平仓、自动反手、自动买卖或自动移动止损。强反转 / 移动止损仍未实现。

## 五、当前 Risk Action Guard（风险动作保护器）能力

本轮只读扫描确认，`DashboardDetailResponseVO.RiskActionGuardDisplayVO` 已有以下字段：

- `riskActionAdvice`（风险动作建议）
- `liquidityState`（流动性状态）
- `stampedeDetected`（是否检测到踩踏）
- `wickOnlyRisk`（是否仅插针风险）
- `opportunityPushAllowed`（是否允许机会推送）
- `reverseTradeAllowed`（是否允许反手）
- `newPositionAllowed`（是否允许新开仓）
- `marketOrderExitAllowed`（是否允许市价退出）
- `manualRiskReviewRequired`（是否必须人工风险复核）
- `notTradeInstruction`（是否不是交易指令）

`DefaultRiskActionGuardDisplayAdapter` 已经把风险状态映射为只读复核：

- 高风险但流动性正常：映射为 `HIGH_RISK_REVIEW_ONLY`，文案只提示人工复核减仓 / 移动止损 / 降低杠杆，不自动止损、反手或开仓。
- Liquidity Deterioration（流动性恶化）：映射为 `LIQUIDITY_DETERIORATION_REVIEW_ONLY`，文案明确不做市价一次性砍仓。
- Stampede（踩踏）：映射为 `STAMPEDE_REVIEW_ONLY`，文案明确禁止机会推送、反手和新开仓。
- Wick-only Risk（仅插针风险）：映射为 `WICK_ONLY_REVIEW_ONLY`，文案明确不等于趋势反转、不生成反向开仓计划。
- 强反转 / 移动止损仍未自动化，只能人工复核。

`DefaultRiskActionGuardDisplayAdapter` 会继续强制：

- `opportunityPushAllowed=false`
- `reverseTradeAllowed=false`
- `newPositionAllowed=false`
- `marketOrderExitAllowed=false`
- `manualRiskReviewRequired=true`
- `notTradeInstruction=true`

这些能力只是 display（展示）/ adapter（适配器）/ fail-closed（失败关闭）。它们不是 production risk action（生产风控动作），不是自动降杠杆，不是自动平仓，不是自动反手，不是自动修改止损，也不是自动交易。

## 六、当前 Dashboard（首页工作台）/ display（展示）现状

当前 `DashboardDetailResponseVO` 已有以下只读展示字段：

- `planBoundaryDisplay`（计划边界展示）
- `executionPlanDisplay`（执行计划展示）
- `riskActionGuardDisplay`（风险动作保护展示）
- `paperObservationDisplay`（纸面观察展示）
- `sourceTrace`（证据来源追踪）
- `runtimeKlineContext`（运行时 K 线上下文）
- `derivativesRiskContext`（衍生品风险上下文）

`DashboardDetailResponseVO` 本身没有独立命名为 `position` 或 `monitor` 的字段，但 `decision` 字段承载了持仓读模型字段，例如 `hasOpenPosition`、`positionSide`、`avgOpenPrice`、`positionOpenTime`、`positionQuantity`、`unrealizedPnlPct`、`positionStatus`、`markPrice`、`breakEvenPrice`、`liquidationPrice`。这些字段来自 `DecisionServiceImpl` 对 `RealPositionMapper.findOpenPositions()` 的读模型合并。

`DashboardController.dashboardDetail(...)` 已经走 detail（详情）路径组装 `planBoundaryDisplay`、`executionPlanDisplay`、`riskActionGuardDisplay` 和 `paperObservationDisplay`。这是最安全的只读展示路径。

`dashboard.html` 已有 Risk Action Guard（风险动作保护器）和持仓监控相关只读区域：

- 页面已有“非交易指令”“需要人工复核”“必须人工复核”“不连接 order API”“不触发自动交易动作”等文案。
- Risk Action Guard（风险动作保护器）区域展示机会推送、反手动作、新开仓动作，并以禁止 / 占位语义呈现。
- 页面明确写出：风险高但流动性正常只能考虑减仓 / 移动止损 / 降低杠杆，且只是风险提醒。
- 页面明确写出：风险高且流动性恶化不建议市价一次性砍仓。
- 页面明确写出：踩踏状态禁止反手、禁止新开仓、禁止机会推送。
- 页面明确写出：短线插针不等于趋势反转，不生成反向开仓计划。
- 页面已有“已开仓监控”区域，但它只描述已有持仓字段；无真实持仓时不生成平仓、反手或新开仓建议。

最安全展示路径是 detail read-only risk display（详情只读风险展示），不是 summary（摘要），不是 action API（动作接口），不是交易按钮。

如果页面或复盘中心出现“复查”“人工复核”“复盘录入”“记录”这类入口，只能解释为人工复核或人工记录入口，不能解释为自动执行、自动平仓或自动反手。

## 七、是否允许未来进入只读强反转 / 移动止损展示

P187 不允许直接写代码。

可以允许未来 P188 做 Position Monitor Strong Reversal / Moving Stop Authorization Gate（持仓强反转 / 移动止损授权门）。P188 的职责是定义 P189 最多允许改哪些文件，以及如何保证 P189 只能做 Review-Only（只允许复核）展示。

如果 P189 未来实现，也只能做 review-only display（只允许复核展示）：

- P189 不能创建 close / reverse / buy / sell（平仓 / 反手 / 买入 / 卖出）动作。
- P189 不能自动修改 stop loss / moving stop（止损 / 移动止损）。
- P189 不能生成真实 entry / stop / TP / RR（入场 / 止损 / 止盈 / 盈亏比）。
- P189 不能升级 ExecutionPlan Readiness（执行计划可执行就绪）。
- P189 不能新增交易动作按钮。
- P189 不能接 order API（下单接口）或 execution API（执行接口）。
- P189 不能接 scheduler-triggered action（定时器触发动作）、automation（自动化）或 auto-trading（自动交易）。

保守结论：允许未来进入只读强反转 / 移动止损展示审计和授权；不允许直接进入自动执行。

## 八、未来 P188/P189 最安全方向

P188/P189 最安全方向是先把风险复核信息讲清楚，而不是生成动作：

- 优先展示 Strong Reversal（强反转）是否待确认。
- 优先展示原入场逻辑是否疑似失效。
- 优先展示 Moving Stop（移动止损）是否需要人工复核。
- 优先展示风险原因、Liquidity State（流动性状态）、Stampede（踩踏）状态、Wick-only Risk（仅插针风险）状态、人工复核模式。
- 优先复用现有 Dashboard Detail（首页详情）/ RiskActionGuardDisplay（风险动作保护展示）/ Position Monitor（持仓监控）区域。
- 不先改 summary（摘要）。
- 不新增 action API（动作接口）。
- 不新增交易按钮。
- 如果已有 display DTO（展示对象）可以承载字段，P188 可以授权 P189 做最小字段展示。
- 如果没有合适对象，应先定义 review-only position risk display DTO（只读持仓风险展示对象）或 adapter（适配器）。
- 保持 `REVIEW_ONLY`（只允许复核）/ `INCOMPLETE`（证据不完整）/ `BLOCKED`（禁止推进）/ `WATCH_ONLY`（仅观察）。
- 不把 Strong Reversal（强反转）映射成反手。
- 不把 Moving Stop（移动止损）映射成自动改止损。
- 不把风险提醒映射成自动执行。

建议 P188 授权时优先考虑最小 display / adapter（展示 / 适配器）层。默认不要改 `dashboard.html`，不要改 `DashboardController.java`，不要改 `MonitorController.java`，不要改 `PositionSyncService` / `PositionSyncScheduler`，不要改 provider（持仓提供器），不要改 mapper（映射器），不要改 schema（数据库结构），不要改 config（配置）。

## 九、仍然禁止的路径

以下路径仍然禁止，不能借 P187-P190 的名义提前进入：

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

## 十、推荐下一步

推荐下一步为：

P188：Position Monitor Strong Reversal / Moving Stop Authorization Gate（持仓强反转 / 移动止损授权门）。

P188 仍然不写代码。P188 只定义 P189 最小只读展示允许改哪些文件。

P188 必须继续禁止自动平仓、自动反手、自动改止损、真实点位、Readiness（可执行就绪）、交易按钮和自动交易。

P188 的重点不是“如何执行强反转或移动止损”，而是“如果 P189 只展示强反转待确认、原入场逻辑疑似失效、移动止损需要人工复核，最多能碰哪些文件、哪些字段、哪些文案”。

## 十一、本轮只读扫描范围

本轮只读扫描了：

- `docs/PROJECT_PROGRESS_INDEX.md`
- `docs/PHASE_BACKEND_P182_RISK_ACTION_GUARD_POSITION_MONITOR_SCOPE_AUDIT.md`
- `docs/PHASE_BACKEND_P183_RISK_ACTION_GUARD_POSITION_MONITOR_AUTHORIZATION_GATE.md`
- `docs/PHASE_BACKEND_P185_RISK_ACTION_GUARD_POSITION_MONITOR_CLOSURE.md`
- `src/main/java/org/example/trademodel/service/dashboard/`
- `src/main/java/org/example/trademodel/service/impl/`
- `src/main/java/org/example/trademodel/service/`
- `src/main/java/org/example/trademodel/controller/`
- `src/main/java/org/example/trademodel/vo/`
- `src/main/java/org/example/trademodel/entity/`
- `src/main/java/org/example/trademodel/mapper/`
- `src/main/java/org/example/trademodel/position/`
- `src/main/resources/templates/dashboard.html`
- `src/main/resources/schema.sql`
- `src/test/java/org/example/trademodel/`
- PositionMonitor / Monitor / Position / Reversal / MovingStop / RiskActionGuard 相关搜索结果

本轮没有读取 runtime / live / external data（运行时 / 实时 / 外部数据），没有调用 Binance（币安）或其他外部接口。

## 十二、P187 结论

P187 的结论是：可以继续推进 P188 授权门，但只能授权未来 P189 做持仓强反转 / 移动止损的只读复核展示。

当前项目已有 Position Monitor（持仓监控）的同步、告警、记录、Dashboard（首页工作台）读模型展示基础，也已有 Risk Action Guard（风险动作保护器）的只读风险展示基础。

但 Strong Reversal（强反转）和 Moving Stop（移动止损）仍未实现。它们不能被解释成自动平仓、自动反手、自动修改止损、真实点位生成、Readiness（可执行就绪）升级或自动交易。
