# P192 Dashboard Risk Reminder Read-Only Display Scope Audit（首页风险提醒只读展示范围审计）

## 一、这一步是干嘛的

P192 是 Dashboard（首页工作台）Risk Reminder（风险提醒）Read-Only Display（只读展示）Scope Audit（范围审计）。

P192 是 P192-P195 Dashboard Risk Reminder Read-Only Display Pack（首页风险提醒只读展示包）的第一步。这个任务包的最大安全边界是：先审计首页风险提醒能不能更清楚地只读展示，再做授权门，再做最小展示接线，最后收口。

本轮只新增一个 P192 审计文档，并删除 `docs/P192.md` placeholder（占位文档）。

本轮不写 Java。本轮不新增测试。本轮不接 Dashboard（首页工作台）新功能。本轮只判断未来首页风险提醒是否可以进入更清楚的 Read-Only Display（只读展示）路径。

本轮不生成真实 entry / stop / TP / RR（入场 / 止损 / 止盈 / 盈亏比）。本轮不创建 close / reverse / buy / sell（平仓 / 反手 / 买入 / 卖出）动作。本轮不自动修改 Stop Loss（止损）/ Moving Stop（移动止损）。本轮不升级 Readiness（可执行就绪）。本轮不接 order API（下单接口）、execution API（执行接口）、scheduler（定时器）、automation（自动化）或 auto-trading（自动交易）。

P192 的判断对象不是“首页能不能执行交易”，而是“首页是否可以把已有风险信息集中成更清楚的只读风险提醒”。答案只能围绕 Review-Only（只允许复核）展示，不允许绕到交易执行。

## 二、P191 / PROJECT_PROGRESS_INDEX 的依据

P192 必须以 `docs/PROJECT_PROGRESS_INDEX.md` 为总索引依据。

P191 已完成 Project Progress Index Refresh After Position Monitor Reversal Display（持仓强反转展示后项目总进度索引刷新）。P191 的核心结论是：P187-P190 已经完成 Position Monitor（持仓监控）Strong Reversal（强反转）/ Moving Stop（移动止损）Review-Only Display（只允许复核展示）闭环，Dashboard（首页工作台）展示层可以继续做风险提醒只读展示审计，但自动交易和真实执行仍然没有打开。

`docs/PROJECT_PROGRESS_INDEX.md` 明确写清楚：

- SourceTrace（证据来源追踪）Read-Only Display（只读展示）已完成。
- BoundaryCandidate（边界候选交易计划）read-only candidate display（只读候选展示）已完成。
- ExecutionPlan（执行计划）review-only display（只允许复核展示）已完成。
- Risk Action Guard（风险动作保护器）/ Position Monitor（持仓监控）Read-Only Risk Display（只读风险展示）已完成。
- Position Monitor Strong Reversal / Moving Stop（持仓强反转 / 移动止损）Read-Only Display（只读展示）已完成。
- 如果目标是个人可用最快路径，总索引推荐进入 Dashboard Risk Reminder Read-Only Display Gate（首页风险提醒只读展示范围门）。

但总索引也同时明确暂停以下能力：

- auto close position（自动平仓）
- auto reverse position（自动反手）
- auto-modify stop loss（自动修改止损）
- auto buy / auto sell（自动买入 / 自动卖出）
- real entry / stop / TP / RR（真实入场 / 止损 / 止盈 / 盈亏比）
- ExecutionPlan Readiness（执行计划可执行就绪）
- production risk action（生产风控动作）
- auto-trading（自动交易）

因此 P192 只能审计 Dashboard（首页工作台）Risk Reminder（风险提醒）Read-Only Display（只读展示）。P192 不能审计自动动作、交易执行、真实点位、可执行状态或生产风控动作。

## 三、当前 Dashboard（首页工作台）风险展示现状

本轮只读扫描确认，当前 `dashboard.html` 已经有多个风险相关展示区域，但它们还不是一个集中命名的 Risk Reminder（风险提醒）区域。

当前页面已有这些只读展示区域：

- SourceTrace（证据来源追踪）/ RuntimeKlineContext（运行时 K 线上下文）诊断区域：展示来源、缺失项、fallback（回退）状态、人工复核和不是交易指令。
- PlanBoundary（计划边界）状态区域：展示 PlanBoundary（计划边界）、SourceTrace（证据来源追踪）、ExecutionPlan（执行计划）接入状态，并明确不生成 entry / stop / take profit（入场 / 止损 / 止盈）数值。
- Risk Action Guard（风险动作保护器）占位和动态展示区域：展示风险动作保护状态、机会推送、反手动作、新开仓动作、Paper Observation（纸面观察）和持仓属性。
- 已开仓监控区域：展示已有持仓字段、方向一致性、当前盈亏和 Paper Observation（纸面观察），并说明高风险不等于直接止损或反手。
- 执行建议区域：展示 ExecutionPlan（执行计划）只读状态、不可执行原因、建议摘要和 RiskActionGuard（风险动作保护器）状态，并说明不生成真实 entry / stop / take-profit（入场 / 止损 / 止盈）数值、不连接 order API（下单接口）。
- 主工作台 Risk Guard 面板：展示 RiskActionGuard（风险动作保护器）状态、Liquidity（流动性）、人工复核、交易语义、市场环境、风险模式和机会推送。
- 人工复核重点列表：会提示复核 RiskActionGuard（风险动作保护器）、踩踏、流动性恶化、插针风险、高风险不等于直接止损或反手。

`DashboardDetailResponseVO` 已有以下只读展示字段：

- `riskActionGuardDisplay`（风险动作保护展示）
- `executionPlanDisplay`（执行计划展示）
- `planBoundaryDisplay`（计划边界展示）
- `paperObservationDisplay`（纸面观察展示）
- `sourceTrace`（证据来源追踪）
- `runtimeKlineContext`（运行时 K 线上下文）
- `derivativesRiskContext`（衍生品风险上下文）

`DashboardController.dashboardDetail(...)` 已经组装以下对象：

- `DashboardSourceTraceDetailAdapter` 生成 `sourceTrace`、`runtimeKlineContext` 和 `derivativesRiskContext`。
- `PlanBoundaryDisplayAdapter` 生成 `planBoundaryDisplay`。
- `ExecutionPlanDisplayAdapter` 生成 `executionPlanDisplay`。
- `RiskActionGuardDisplayAdapter` 生成 `riskActionGuardDisplay`。
- `PaperObservationDisplayAdapter` 生成 `paperObservationDisplay`。

当前首页已经有以下安全文案：

- “非交易指令”
- “需要人工复核”
- “必须人工复核”
- “不连接 order API”
- “不触发自动交易动作”
- “当前区域仅为状态占位，不生成交易计划，不提供自动执行建议”
- “不生成真实 entry / stop / take-profit 数值”
- “高风险不等于直接止损或反手”

当前风险提醒展示情况需要分开看：

- `RiskActionGuardDisplayVO` 后端字段已经承载 `opportunityPushAllowed`（是否允许机会推送）、`reverseTradeAllowed`（是否允许反手）、`newPositionAllowed`（是否允许新开仓）、`marketOrderExitAllowed`（是否允许市价退出）、`manualRiskReviewRequired`（是否必须人工风险复核）、`notTradeInstruction`（是否不是交易指令）。
- `dashboard.html` 当前已经显式展示机会推送、反手动作、新开仓动作、人工复核和交易语义。
- `marketOrderExitAllowed` 当前在 VO（视图对象）和 adapter（适配器）里是 fail-closed（失败关闭）字段，但页面没有作为独立指标集中展示。
- `riskActionAdvice`（风险动作建议）和 `riskActionBlockingReason`（风险动作阻断原因）当前在 VO 和 adapter 里存在，但页面没有集中展示为统一 Risk Reminder（风险提醒）文案。

当前是否已经展示 Strong Reversal（强反转）/ Moving Stop（移动止损）/ high risk（高风险）/ liquidity deterioration（流动性恶化）/ stampede（踩踏）/ wick-only risk（仅插针风险）相关只读信息：

- high risk（高风险）、liquidity deterioration（流动性恶化）、stampede（踩踏）、wick-only risk（仅插针风险）在页面静态文案和主工作台人工复核重点里已经有只读提醒。
- Strong Reversal（强反转）/ Moving Stop（移动止损）在 `DefaultRiskActionGuardDisplayAdapter` 的 `riskActionAdvice` 里已经有只读复核说明。
- 但页面当前没有把 `riskActionAdvice` 集中渲染出来，因此 Strong Reversal（强反转）/ Moving Stop（移动止损）的后端只读提示还没有形成清晰的首页集中风险提醒区域。

保守结论：当前 Dashboard（首页工作台）已经有风险提醒材料，但风险信息分散在 PlanBoundary、ExecutionPlan、RiskActionGuard、已开仓监控、主工作台 Risk Guard 和人工复核重点里。未来可以审计是否把这些信息集中成更清楚的只读提醒，但不能把提醒解释为交易动作。

## 四、当前 Risk Action Guard（风险动作保护器）/ Position Monitor（持仓监控）能力

`DashboardDetailResponseVO.RiskActionGuardDisplayVO` 已有字段承载只读风险提醒：

- `riskActionGuardStatus`（风险动作保护状态）
- `riskActionGuardStatusLabel`（风险动作保护状态标签）
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

`DefaultRiskActionGuardDisplayAdapter` 已经补强以下只读提示：

- high risk（高风险）只触发人工复核，可复核减仓 / Moving Stop（移动止损）/ 降低杠杆，不自动止损、反手或开仓。
- liquidity deterioration（流动性恶化）时不做市价一次性砍仓，只能人工复核分批降风险、等待流动性恢复或只降杠杆。
- stampede（踩踏）风险进入极端压力锁定，禁止机会推送、反手和新开仓。
- wick-only risk（仅插针风险）不等于趋势反转，不生成反向开仓计划，只能等待确认。
- Strong Reversal（强反转）/ Moving Stop（移动止损）仍未自动化，只能人工复核。
- Strong Reversal（强反转）待确认，原入场逻辑疑似失效也只能进入复核。
- Moving Stop（移动止损）需要人工复核。
- Strong Reversal（强反转）不等于反手或自动平仓。
- Moving Stop（移动止损）不等于自动改 Stop Loss（止损）。
- 自动平仓 / 自动反手 / 自动修改止损均关闭。
- 不生成真实 entry / stop / TP / RR（入场 / 止损 / 止盈 / 盈亏比），不升级 Readiness（可执行就绪）。

`DefaultRiskActionGuardDisplayAdapter` 继续强制 fail-closed（失败关闭）：

- `opportunityPushAllowed=false`
- `reverseTradeAllowed=false`
- `newPositionAllowed=false`
- `marketOrderExitAllowed=false`
- `manualRiskReviewRequired=true`
- `notTradeInstruction=true`

Position Monitor（持仓监控）已经有同步 / 告警 / 记录基础：

- `PositionSyncService`（持仓同步服务）
- `PositionSyncScheduler`（持仓同步定时器）
- `PositionProvider`（持仓提供器）
- `SimulatedPositionProvider`（模拟持仓提供器）
- `BinancePositionProvider`（币安持仓提供器）
- `SwitchablePositionProvider`（可切换持仓提供器）
- `RealPositionMapper`（真实持仓映射器）
- `MonitorAlertMapper`（监控告警映射器）
- `tm_real_position`（真实持仓表）
- `tm_monitor_alert`（监控告警表）

强反转 / 移动止损当前仍只是 Review-Only（只允许复核）材料，不是自动执行：

- Strong Reversal（强反转）不是直接反手。
- Strong Reversal（强反转）不是自动平仓。
- Moving Stop（移动止损）不是自动修改 Stop Loss（止损）。
- high risk（高风险）不是立即止损、立即反手或立即开仓。

这些能力只是 display（展示）/ adapter（适配器）/ read model（读模型）/ fail-closed（失败关闭）。它们不等于 production risk action（生产风控动作），不等于自动平仓，不等于自动反手，不等于自动买卖，不等于自动改止损，也不等于自动交易。

## 五、是否允许未来进入首页风险提醒只读展示

P192 不允许直接写代码。

可以允许未来 P193 做 Dashboard Risk Reminder Read-Only Display Authorization Gate（首页风险提醒只读展示授权门）。

P193 必须明确 P194 最多允许改哪些文件。P193 的职责不是实现功能，而是定义 P194 的最小可动边界。

如果 P194 未来实现，也只能做 Read-Only Display（只读展示）：

- P194 不能创建 close / reverse / buy / sell（平仓 / 反手 / 买入 / 卖出）动作。
- P194 不能自动修改 Stop Loss（止损）/ Moving Stop（移动止损）。
- P194 不能生成真实 entry / stop / TP / RR（入场 / 止损 / 止盈 / 盈亏比）。
- P194 不能升级 Readiness（可执行就绪）。
- P194 不能新增交易动作按钮。
- P194 不能新增 action API（动作接口）或交易 endpoint（接口端点）。
- P194 不能接 scheduler（定时器）、automation（自动化）或 auto-trading（自动交易）。

保守结论：允许未来进入首页风险提醒只读展示授权；不允许直接进入自动动作或交易执行。

## 六、未来 P193/P194 最安全方向

P193/P194 最安全方向是让风险提醒更集中、更清楚，但仍保持只读：

- 优先把风险提醒集中展示为 Read-Only Display（只读展示），不生成动作。
- 优先复用 `RiskActionGuardDisplay`（风险动作保护展示）、`ExecutionPlanDisplay`（执行计划展示）、Position Monitor（持仓监控）已有字段。
- 优先展示“不是交易指令”。
- 优先展示“必须人工复核”。
- 优先展示 high risk（高风险）不等于直接止损。
- 优先展示 Strong Reversal（强反转）不等于反手。
- 优先展示 Moving Stop（移动止损）不等于自动改 Stop Loss（止损）。
- 优先展示 stampede（踩踏）禁止机会推送 / 反手 / 新开仓。
- 优先展示 wick-only risk（仅插针风险）不等于趋势反转。
- 优先展示 auto close / auto reverse / auto stop modification（自动平仓 / 自动反手 / 自动修改止损）关闭。
- 不先新增 API（接口）。
- 不新增交易按钮。
- 默认只允许 `dashboard.html` 的只读文案 / 展示区域最小改动，或者只允许 display adapter（展示适配器）文案补强。
- 如果需要后端字段，必须先确认已有 VO（视图对象）字段是否足够。
- 优先使用已有 `riskActionAdvice`、`riskActionBlockingReason`、`liquidityState`、`stampedeDetected`、`wickOnlyRisk`、`manualRiskReviewRequired`、`notTradeInstruction` 等字段。
- 保持 `REVIEW_ONLY`（只允许复核）/ `INCOMPLETE`（证据不完整）/ `BLOCKED`（禁止推进）/ `WATCH_ONLY`（仅观察）。
- 不把任何风险提醒映射成自动执行。

如果 P194 发现必须新增后端字段、Controller（控制器）、endpoint（接口端点）、schema（数据库结构）或 service（服务），应停止并回到授权门，不应在 P194 内扩散。

## 七、仍然禁止的路径

以下路径仍然禁止，不能借 P192-P195 的名义提前进入：

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

P193：Dashboard Risk Reminder Read-Only Display Authorization Gate（首页风险提醒只读展示授权门）。

P193 仍然不写代码。P193 只定义 P194 最小只读展示允许改哪些文件。

P193 必须继续禁止自动平仓、自动反手、自动改止损、真实点位、Readiness（可执行就绪）、交易按钮和自动交易。

P193 的重点不是“让首页能交易”，而是“如果 P194 只把风险提醒集中成只读展示，最多能碰哪些文件、哪些字段、哪些文案”。

## 九、本轮只读扫描范围

本轮只读扫描了：

- `docs/PROJECT_PROGRESS_INDEX.md`
- `docs/PHASE_BACKEND_P187_POSITION_MONITOR_STRONG_REVERSAL_MOVING_STOP_SCOPE_AUDIT.md`
- `docs/PHASE_BACKEND_P188_POSITION_MONITOR_STRONG_REVERSAL_MOVING_STOP_AUTHORIZATION_GATE.md`
- `docs/PHASE_BACKEND_P190_POSITION_MONITOR_STRONG_REVERSAL_MOVING_STOP_CLOSURE.md`
- `docs/PHASE_BACKEND_P189*` 匹配结果为空；P189 的结果通过 P190 收口文档、P191 总索引和 P189 commit 记录确认。
- `src/main/resources/templates/dashboard.html`
- `src/main/java/org/example/trademodel/controller/DashboardController.java`
- `src/main/java/org/example/trademodel/vo/DashboardDetailResponseVO.java`
- `src/main/java/org/example/trademodel/service/dashboard/`
- `src/main/java/org/example/trademodel/service/impl/`
- `src/test/java/org/example/trademodel/`
- Dashboard / Risk Reminder / RiskActionGuard / Position Monitor / ExecutionPlan display 相关搜索结果

本轮没有读取 runtime / live / external data（运行时 / 实时 / 外部数据），没有调用 Binance（币安）或任何外部接口。

## 十、P192 结论

P192 的结论是：可以继续推进 P193 Authorization Gate（授权门），但只能授权未来 P194 做 Dashboard（首页工作台）Risk Reminder（风险提醒）Read-Only Display（只读展示）。

未来 P194 的合理目标是：把分散在 RiskActionGuardDisplay（风险动作保护展示）、ExecutionPlanDisplay（执行计划展示）、Position Monitor（持仓监控）和主工作台里的风险提醒集中展示清楚，让用户更容易看到“为什么只能人工复核、为什么不能执行”。

未来 P194 的不合理目标是：自动平仓、自动反手、自动修改止损、生成真实点位、升级 Readiness（可执行就绪）、新增交易按钮、接入 order / execution / scheduler / automation / auto-trading。
