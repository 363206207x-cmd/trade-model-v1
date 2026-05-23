# PROJECT_PROGRESS_INDEX

P196 是 Project Progress Index Refresh After Dashboard Risk Reminder Display（首页风险提醒展示后项目总进度索引刷新）。

本索引来自 P164 全局扫描后的持续维护。本轮只刷新 `docs/PROJECT_PROGRESS_INDEX.md`，并吸收 P192-P195 Dashboard Risk Reminder（首页风险提醒）Read-Only Display（只读展示）闭环结果。P196 不新增 Java，不新增测试，不改 `dashboard.html`，不接 API（接口），不生成真实 entry / stop / TP / RR（入场 / 止损 / 止盈 / 盈亏比），不创建 close / reverse / buy / sell（平仓 / 反手 / 买入 / 卖出）动作，不自动修改 Stop Loss（止损）/ Moving Stop（移动止损），不升级 Readiness（可执行就绪），不接 auto-trading（自动交易）。

## 一、当前总进度结论

当前项目仍然不是“快完成自动交易”的状态。更准确的状态是：安全地基、只读复核、失败关闭、SourceTrace（证据来源追踪）只读展示、BoundaryCandidate（边界候选交易计划）只读候选展示、ExecutionPlan（执行计划）review-only plan display（只允许复核展示）、Risk Action Guard（风险动作保护器）read-only risk display（只读风险展示）、Position Monitor Strong Reversal / Moving Stop review-only display（持仓强反转 / 移动止损只允许复核展示）以及 Dashboard Risk Reminder read-only display（首页风险提醒只读展示）已经更完整；但 production candidate generation（生产候选交易计划生成）、trading actions（交易动作）、production risk action（生产风控动作）、production VALID（生产环境有效候选状态）、Strong Reversal automation（强反转自动化）、Moving Stop automation（移动止损自动化）、auto close / reverse / buy / sell actions（自动平仓 / 反手 / 买入 / 卖出动作）、auto stop modification（自动修改止损）、真实 entry / stop / TP / RR（入场 / 止损 / 止盈 / 盈亏比）、ExecutionPlan Readiness（执行计划可执行就绪）和 auto-trading（自动交易）仍然没有闭环。

P165-P170 已完成 SourceTrace read-only display（证据来源追踪只读展示）闭环。P172-P175 已完成 BoundaryCandidate read-only candidate display（边界候选只读候选展示）闭环。P177-P180 已完成 ExecutionPlan review-only plan display（执行计划只允许复核展示）闭环。P182-P185 已完成 Risk Action Guard / Position Monitor read-only risk display（风险动作保护 / 持仓监控只读风险展示）闭环。P187-P190 已完成 Position Monitor Strong Reversal / Moving Stop Review-Only Pack（持仓强反转 / 移动止损只读复核包）闭环。P192-P195 已完成 Dashboard Risk Reminder Read-Only Display Pack（首页风险提醒只读展示包）闭环。所以项目总进度可以小幅上调。但这个上调只代表“可看见、可复核、可解释”的只读展示层推进，不代表 trading actions（交易动作）、production risk action（生产风控动作）、strong reversal automation（强反转自动化）、moving stop automation（移动止损自动化）、auto close / reverse / buy / sell actions（自动平仓 / 反手 / 买入 / 卖出动作）、auto stop modification（自动修改止损）、交易计划生成、生产 `VALID`（有效候选状态）、真实点位、Readiness（可执行就绪）或自动交易完成。

| 项目线 | 当前真实进度 |
|---|---:|
| 项目总进度 | 71%-76% |
| 安全地基进度 | 87%-93% |
| SourceTrace（证据来源追踪）进度 | 58%-66% |
| BoundaryCandidate（边界候选交易计划）进度 | 42%-52% |
| ExecutionPlan（执行计划）进度 | 45%-55% |
| Risk Action Guard（风险动作保护器）进度 | 47%-57% |
| Position Monitor（持仓监控）进度 | 52%-62% |
| 真实生产接线进度 | 26%-34% |
| entry / stop / TP / RR（入场 / 止损 / 止盈 / 盈亏比）真实点位进度 | 10%-18% |
| dashboard（首页工作台）展示进度 | 76%-84% |
| 自动交易进度 | 0%-3% |

这些百分比按“能否安全进入真实生产链路”估算，不按文档数量估算。SourceTrace（证据来源追踪）、BoundaryCandidate（边界候选交易计划）和 ExecutionPlan（执行计划）本轮不再上调。Position Monitor（持仓监控）保持 52%-62%，不因首页展示再明显上调。Risk Action Guard（风险动作保护器）和 Dashboard（首页工作台）因为 P192-P195 的首页风险提醒集中只读展示略微上调。真实生产接线、真实点位和自动交易仍保持谨慎，因为真实交易计划链路仍缺 source-owned runtime candidate generation（运行时证据来源候选生成）、numeric source ownership（数值来源归属）、production risk action（生产风控动作）、production VALID（生产环境有效候选状态）、Readiness（可执行就绪）和交易动作闭环。

## 二、已完成线路

- P140-P163 SourceTrace（证据来源追踪）/ Production Wiring Preparation（生产接线准备）已完成：已经完成范围门、缺口审计、输入契约、设计矩阵、测试计划、`INCOMPLETE`（证据不完整）guard（保护）、`BLOCKED`（禁止推进）guard（保护）、SourceTrace runtime population（运行时证据来源填充）helper（辅助类）、SourceTrace service wrapper（服务包装器）和收口文档。证据线索：`docs/PHASE_BACKEND_P140_PRODUCTION_WIRING_PREPARATION_SCOPE_GATE.md` 到 `docs/PHASE_BACKEND_P163_SOURCETRACE_SERVICE_WIRING_CLOSURE.md`。
- P160-P163 SourceTrace Service Wiring Pack（证据来源追踪服务层接线包）已完成：`SourceTraceRuntimePopulationService` 和 `SourceTraceRuntimePopulationServiceImpl` 已存在，service wrapper（服务包装器）只调用 `SourceTraceRuntimePopulationHelper.populate(...)`。证据线索：`src/main/java/org/example/trademodel/service/planboundary/SourceTraceRuntimePopulationService.java`、`src/main/java/org/example/trademodel/service/planboundary/SourceTraceRuntimePopulationServiceImpl.java`、`src/test/java/org/example/trademodel/service/planboundary/SourceTraceRuntimePopulationServiceTest.java`。
- P165-P170 SourceTrace read-model / dashboard read-only display pack（证据来源追踪只读输出 / 首页只读展示包）已完成。
- P165 完成 SourceTrace Read Model / Controller Scope Audit（证据来源追踪只读输出范围审计）：确认 detail（详情）路径比 summary（汇总）路径安全，确认 `DashboardDetailResponseVO` 已有 `SourceTraceDTO` 字段，确认 `dashboard.html` 已有 `detail.sourceTrace` 读取片段。
- P166 完成 SourceTrace Read Model Authorization Gate（证据来源追踪只读输出授权门）：授权 P167 只允许 detail read model（详情只读模型）/ adapter（适配器）层最小接线。
- P167 完成 detail read model / adapter（详情只读模型 / 适配器）最小只读接线：`DefaultDashboardSourceTraceDetailAdapter` 已补强 `sourceOwner`、`sourceRef`、`sourceTimeframe`、`freshnessStatus`、`reviewMode`、`blockingReasons` 等只读字段和阻断原因。
- P168 完成 SourceTrace Dashboard Display Scope Gate（首页证据来源追踪展示范围门）：授权 P169 只能展示已有 `detail.sourceTrace` 字段，禁止页面交易动作、可执行状态、真实点位和自动交易。
- P169 完成 `dashboard.html` 只读展示：首页详情现在可以展示 SourceTrace（证据来源追踪）的来源、缺失项、阻断原因、复核模式、是否必须人工复核、是否不是交易指令。
- P170 完成 SourceTrace Read-Only Display Closure（证据来源追踪只读展示收口）：确认 P165-P170 完成的是只读展示闭环，不是生产候选、真实点位、Readiness（可执行就绪）或自动交易。
- P172-P175 BoundaryCandidate Read-Only Candidate Display Pack（边界候选只读候选展示包）已完成。
- P172 完成 BoundaryCandidate Read-Only Candidate Display Scope Audit（边界候选只读候选展示范围审计）：确认 `BoundaryCandidateDTO`、`BoundaryCandidateServiceImpl`、MarketReadOnlyCandidate（市场只读候选）相关对象存在，也确认 DTO（数据对象）/ service skeleton（服务骨架）/ valid factory（有效候选工厂）存在不等于生产候选完成。
- P173 完成 BoundaryCandidate Read-Only Candidate Display Authorization Gate（边界候选只读候选展示授权门）：授权 P174 只能围绕 `DefaultPlanBoundaryDisplayAdapter` 和测试做最小只读接线，默认禁止页面、Controller（控制器）、API（接口）、VO（视图对象）、service（服务）、schema（数据库结构）和 config（配置）扩散。
- P174 完成 PlanBoundaryDisplay（计划边界展示）/ `DefaultPlanBoundaryDisplayAdapter` 最小只读接线：PlanBoundaryDisplay（计划边界展示）现在能更清楚展示 BoundaryCandidate（边界候选交易计划）的只读候选状态、`INCOMPLETE`（证据不完整）、`BLOCKED`（禁止推进）、`WATCH_ONLY`（仅观察）、`REVIEW_ONLY`（只允许复核）、缺失字段、阻断原因、必须人工复核、不是交易指令。
- P174 对 `VALID`（有效候选状态）做安全降级，不作为 production VALID（生产环境有效候选状态）输出。
- P175 完成 BoundaryCandidate Read-Only Candidate Display Closure（边界候选只读候选展示收口）：确认 P172-P175 完成的是只读候选展示闭环，不是生产候选、真实点位、Readiness（可执行就绪）或自动交易。
- P177-P180 ExecutionPlan Review-Only Plan Display Pack（执行计划只允许复核展示包）已完成。
- P177 完成 ExecutionPlan Review-Only Plan Display Scope Audit（执行计划只允许复核展示范围审计）：确认 `ExecutionPlanVO`、`ExecutionPlanDO`、`ExecutionPlanDisplayVO`、`PlanServiceImpl` 和 `DefaultExecutionPlanDisplayAdapter` 已存在，也确认当前是 advisory（建议性）/ review-only（只允许复核）/ display（展示），不是可执行计划。
- P178 完成 ExecutionPlan Review-Only Plan Display Authorization Gate（执行计划只允许复核展示授权门）：授权 P179 只能围绕 `DefaultExecutionPlanDisplayAdapter` 和 `DefaultExecutionPlanDisplayAdapterTest` 做最小只读接线。
- P179 完成 `DefaultExecutionPlanDisplayAdapter` / `DefaultExecutionPlanDisplayAdapterTest` 最小只读接线：ExecutionPlanDisplay（执行计划展示）现在能更清楚展示 `READY_REVIEW_ONLY`（只允许复核的就绪摘要）只允许复核摘要、`EXECUTION_PLAN_REVIEW_ONLY_DISPLAY`、`EXECUTION_PLAN_NOT_EXECUTABLE`、`NOT_TRADE_INSTRUCTION`、`ENTRY_STOP_TP_RR_NOT_GENERATED`、`manualReviewRequired=true`（必须人工复核）和 `notTradeInstruction=true`（不是交易指令）。
- P179 没有升级 Readiness（可执行就绪），没有生成真实 entry / stop / TP / RR（入场 / 止损 / 止盈 / 盈亏比），没有改 `dashboard.html`，没有接 API（接口），没有接自动交易。
- P180 完成 ExecutionPlan Review-Only Plan Display Closure（执行计划只允许复核展示收口）：确认 P177-P180 完成的是只允许复核展示闭环，不是可执行计划、真实点位、Readiness（可执行就绪）或自动交易。
- P182-P185 Risk Action Guard / Position Monitor Read-Only Risk Display Pack（风险动作保护 / 持仓监控只读风险展示包）已完成。
- P182 完成 Risk Action Guard / Position Monitor Scope Audit（风险动作保护和持仓监控范围审计）：确认 `RiskActionGuardDisplayVO`、`DefaultRiskActionGuardDisplayAdapter`、Position Monitor（持仓监控）同步 / 告警 / 记录基础、`tm_real_position`（真实持仓表）和 `tm_monitor_alert`（监控告警表）存在，同时确认它们不是自动平仓、自动反手或自动下单。
- P183 完成 Risk Action Guard / Position Monitor Authorization Gate（风险动作保护和持仓监控授权门）：授权 P184 只能围绕 `DefaultRiskActionGuardDisplayAdapter` 和测试做最小只读接线。
- P184 完成 `DefaultRiskActionGuardDisplayAdapter` / `DefaultRiskActionGuardDisplayAdapterTest` 最小只读接线。
- P185 完成 Risk Action Guard / Position Monitor Closure（风险动作保护和持仓监控收口）。
- RiskActionGuardDisplay（风险动作保护展示）现在能更清楚展示 `LIQUIDITY_DETERIORATION_REVIEW_ONLY`、`STAMPEDE_REVIEW_ONLY`、`WICK_ONLY_REVIEW_ONLY`、`HIGH_RISK_REVIEW_ONLY`、流动性恶化时不做市价一次性砍仓、踩踏风险禁止机会推送 / 反手 / 新开仓、仅插针风险不等于趋势反转、强反转 / 移动止损仍未自动化且只能人工复核。
- RiskActionGuardDisplay（风险动作保护展示）继续保持 `opportunityPushAllowed=false`、`reverseTradeAllowed=false`、`newPositionAllowed=false`、`marketOrderExitAllowed=false`、`manualRiskReviewRequired=true`、`notTradeInstruction=true`。
- P184 没有创建 close / reverse / buy / sell（平仓 / 反手 / 买入 / 卖出）动作，没有生成真实 entry / stop / TP / RR（入场 / 止损 / 止盈 / 盈亏比），没有改 `dashboard.html`，没有接 API（接口），没有升级 Readiness（可执行就绪），没有接自动交易。
- P187-P190 Position Monitor Strong Reversal / Moving Stop Review-Only Pack（持仓强反转 / 移动止损只读复核包）已完成。
- P187 完成 Position Monitor Strong Reversal / Moving Stop Review-Only Scope Audit（持仓强反转 / 移动止损只读复核范围审计）。
- P188 完成 Position Monitor Strong Reversal / Moving Stop Authorization Gate（持仓强反转 / 移动止损授权门）。
- P189 完成 `DefaultRiskActionGuardDisplayAdapter` / `DefaultRiskActionGuardDisplayAdapterTest` 最小只读接线。
- P190 完成 Position Monitor Strong Reversal / Moving Stop Closure（持仓强反转 / 移动止损收口）。
- RiskActionGuardDisplay（风险动作保护展示）现在能更清楚展示 Strong Reversal（强反转）待确认、原入场逻辑疑似失效、Moving Stop（移动止损）需要人工复核、Strong Reversal（强反转）不等于反手或自动平仓、Moving Stop（移动止损）不等于自动改 Stop Loss（止损）、auto close / auto reverse / auto stop modification（自动平仓 / 自动反手 / 自动修改止损）均关闭、不生成真实 entry / stop / TP / RR（入场 / 止损 / 止盈 / 盈亏比）、不升级 Readiness（可执行就绪）。
- P189 没有创建 close / reverse / buy / sell（平仓 / 反手 / 买入 / 卖出）动作。
- P189 没有自动修改 Stop Loss（止损）/ Moving Stop（移动止损）。
- P189 没有生成真实 entry / stop / TP / RR（入场 / 止损 / 止盈 / 盈亏比）。
- P189 没有改 `dashboard.html`。
- P189 没有接 API（接口）。
- P189 没有升级 Readiness（可执行就绪）。
- P189 没有接 auto-trading（自动交易）。
- P192-P195 Dashboard Risk Reminder Read-Only Display Pack（首页风险提醒只读展示包）已完成。
- P192 完成 Dashboard Risk Reminder Read-Only Display Scope Audit（首页风险提醒只读展示范围审计）。
- P193 完成 Dashboard Risk Reminder Read-Only Display Authorization Gate（首页风险提醒只读展示授权门）。
- P194 完成 `dashboard.html` 最小 Read-Only Display（只读展示）接线。
- P195 完成 Dashboard Risk Reminder Read-Only Display Closure（首页风险提醒只读展示收口）。
- Dashboard（首页工作台）现在能更集中展示 `riskActionAdvice`（风险动作建议）、`riskActionBlockingReason`（风险动作阻断原因）、`liquidityState`（流动性状态）、`stampedeDetected`（是否检测到踩踏）、`wickOnlyRisk`（是否仅插针风险）、`marketOrderExitAllowed`（是否允许市价退出）、`opportunityPushAllowed`（是否允许机会推送）、`reverseTradeAllowed`（是否允许反手）、`newPositionAllowed`（是否允许新开仓）、`manualRiskReviewRequired`（是否必须人工复核）和 `notTradeInstruction`（是否不是交易指令）。
- Dashboard（首页工作台）现在能集中展示“不是交易指令”“必须人工复核”“不连接 order API（下单接口）”“不触发自动交易动作”“不生成真实点位”“自动平仓 / 自动反手 / 自动改止损关闭”。
- P194 只修改 `src/main/resources/templates/dashboard.html`。
- P194 没有改 Java。
- P194 没有改 `DashboardController.java` / `DashboardDetailResponseVO.java`。
- P194 没有接 API（接口）。
- P194 没有新增按钮。
- P194 没有创建 close / reverse / buy / sell（平仓 / 反手 / 买入 / 卖出）动作。
- P194 没有自动修改 Stop Loss（止损）/ Moving Stop（移动止损）。
- P194 没有生成真实 entry / stop / TP / RR（入场 / 止损 / 止盈 / 盈亏比）。
- P194 没有升级 Readiness（可执行就绪）。
- P194 没有接 auto-trading（自动交易）。
- Watchlist（观察库）/ Display Slots（首页展示位）已完成边界定义和 Dashboard（首页工作台）展示口径：Display Slots 是首页展示优先级，Watchlist Pool（观察库池）是推送候选边界，但低频扫描和机会提升还没有完成。证据线索：`docs/PHASE_DASHBOARD_LIVE_OBSERVATION_MVP_PLAN.md`、`docs/PHASE_HOME_P1_DASHBOARD_MAIN_WORKBENCH_CONSOLIDATION_RESULT.md`。
- BoundaryCandidate（边界候选交易计划）DTO / valid factory（有效候选工厂）/ service skeleton（服务骨架）已完成：`BoundaryCandidateDTO.valid(...)` 存在，`BoundaryCandidateServiceImpl` 存在，并有多组测试覆盖失败关闭、Risk Action Guard（风险动作保护器）阻断和 fixture（测试夹具）行为。注意：这不等于 production VALID（生产环境有效候选状态）已经可生成。
- RuntimeKlineContext（运行时 K 线上下文）/ BoundaryCandidateService（边界候选服务）相关已完成：`RuntimeKlineContextAssemblyServiceImpl` 能从 persisted OHLCV readiness（持久化 K 线就绪结果）组装只读上下文；`BoundaryCandidateServiceImpl` 能在输入完整时调用 valid factory（有效候选工厂），也能在缺证据或风险阻断时降级。
- Dashboard（首页工作台）已完成能力：`DashboardController` 有 `/dashboard`、`/api/dashboard/summary`、`/api/dashboard/detail`；detail（详情接口）已接 PlanBoundaryDisplay（计划边界展示）、ExecutionPlanDisplay（执行计划展示）、RiskActionGuardDisplay（风险动作保护展示）、PaperObservationDisplay（纸面观察展示）、SourceTrace（证据来源追踪）只读展示、BoundaryCandidate（边界候选交易计划）只读候选状态表达、ExecutionPlan（执行计划）只允许复核展示保护栏、Risk Action Guard（风险动作保护器）只读风险展示和 Dashboard Risk Reminder（首页风险提醒）集中只读展示。证据线索：`src/main/java/org/example/trademodel/controller/DashboardController.java`、`src/main/resources/templates/dashboard.html`、`src/main/java/org/example/trademodel/service/dashboard/DefaultPlanBoundaryDisplayAdapter.java`、`src/main/java/org/example/trademodel/service/dashboard/DefaultExecutionPlanDisplayAdapter.java`、`src/main/java/org/example/trademodel/service/dashboard/DefaultRiskActionGuardDisplayAdapter.java`、`docs/PHASE_BACKEND_P170_SOURCETRACE_READ_ONLY_DISPLAY_CLOSURE.md`、`docs/PHASE_BACKEND_P175_BOUNDARY_CANDIDATE_READ_ONLY_DISPLAY_CLOSURE.md`、`docs/PHASE_BACKEND_P180_EXECUTION_PLAN_REVIEW_ONLY_DISPLAY_CLOSURE.md`、`docs/PHASE_BACKEND_P185_RISK_ACTION_GUARD_POSITION_MONITOR_CLOSURE.md`、`docs/PHASE_BACKEND_P190_POSITION_MONITOR_STRONG_REVERSAL_MOVING_STOP_CLOSURE.md`、`docs/PHASE_BACKEND_P195_DASHBOARD_RISK_REMINDER_READ_ONLY_DISPLAY_CLOSURE.md`。
- position sync（持仓同步）/ manual position（手动或模拟持仓）/ monitoring（监控）已完成基础能力：`PositionSyncService`、`PositionSyncScheduler`、`PositionProvider`、`SimulatedPositionProvider`、`BinancePositionProvider`、`MonitorController`、`MonitorService` 和 `tm_real_position` / `tm_monitor_alert` schema（数据库结构）存在。当前更像同步、告警、记录和只读风险展示基础，不是自动平仓或自动反手。

## 三、正在推进线路

Dashboard Risk Reminder（首页风险提醒）只读展示已完成。当前不应该继续把首页风险提醒误推进成自动平仓、自动反手、自动修改止损、真实点位、Readiness（可执行就绪）或 auto-trading（自动交易）。

下一阶段建议进入以下两类审计之一：

- Watchlist Low-Frequency Scan / Opportunity Promote Audit（观察库低频扫描 / 机会提升审计）或 Alert / Push Channel Review-Only Dispatch Audit（告警 / 推送通道只允许复核调度审计）：如果目标是个人可用最快路径，优先继续完善观察库低频扫描、机会提升、告警推送和人工复核提醒。
- BoundaryCandidate Source Wiring Scope Audit（边界候选来源接线范围审计）或 Numeric Source Ownership Audit（数值来源归属审计）：如果目标是严格生产候选，优先审计边界候选真实来源、数值来源、缺证据阻断和 production VALID（生产环境有效候选状态）边界。

无论走哪条路线，都不能直接进入自动平仓、自动反手、自动修改止损或真实点位。

## 四、暂停线路

- trading actions（交易动作）：仍暂停，原因是首页风险提醒只读展示只解释风险，不授权平仓、反手、买入、卖出或下单。
- production risk action（生产风控动作）：仍暂停，原因是 P182-P185 和 P192-P195 完成的是 Read-Only Risk Display（只读风险展示）和 Risk Reminder（风险提醒）展示，不是生产动作执行。
- auto close / auto reverse / auto buy / auto sell（自动平仓 / 自动反手 / 自动买入 / 自动卖出）：仍暂停，原因是没有授权交易动作和动作接口。
- auto stop modification（自动修改止损）：仍暂停，原因是 P189 和 P194 都明确展示自动修改止损关闭。
- Strong Reversal（强反转）automation（自动化）：仍暂停，原因是 P187-P190 完成的是只读展示，不是强反转自动识别或自动处理。
- Moving Stop（移动止损）automation（自动化）：仍暂停，原因是移动止损目前只能作为人工复核提醒，不能自动修改止损。
- ExecutionPlan readiness（执行计划可执行就绪）：仍暂停，原因是 P177-P180 完成的是 review-only display（只允许复核展示），不是可执行计划。
- BoundaryCandidate（边界候选交易计划）真实候选生成：仍暂停，原因是只读候选展示完成不等于真实来源接线和生产候选生成闭环。
- production VALID（生产环境有效候选状态）：仍暂停，原因是 P174 已对 `VALID`（有效候选状态）做安全降级，不能把它当成生产输出。
- entry / stop / TP / RR（入场 / 止损 / 止盈 / 盈亏比）真实点位：仍暂停，原因是 numeric source ownership（数值来源归属）没有真实闭环。
- Watchlist（观察库）低频扫描 / opportunity promote（机会提升）：仍暂停，原因是 Push / Recheck（推送 / 二次复核）已有基础，但低频机会扫描和提升规则未闭环。
- AI 多角色冲突处理落地：仍暂停，原因是已有 `AiConflictResolverService`，但多角色冲突处理还不是完整生产裁决链。
- 自动交易：仍暂停，原因是没有 order API（下单接口）、execution API（执行接口）、自动平仓或自动反手授权。

## 五、后期必须回来做的线路

- BoundaryCandidate（边界候选交易计划）真实来源接线：必须回来做，因为没有真实来源，`VALID`（有效候选状态）只能停留在 DTO / 测试或局部服务语义。
- ExecutionPlan Readiness（执行计划可执行就绪）：必须回来做，因为 P177-P180 只完成 review-only display（只允许复核展示），真实边界来源、真实点位和风险保护没有闭环前不能升级。
- Risk Action Guard（风险动作保护器）生产接线：必须回来做，因为 P182-P185 只完成只读风险展示，stampede（踩踏）、强反转、移动止损、流动性恶化这些风险还没有生产动作链路。
- Position Monitor（持仓监控）：必须回来做，因为 position sync（持仓同步）和只读风险展示只说明当前持仓可被记录和解释，不说明系统能安全处理强反转、移动止损或退出策略。
- Watchlist Push（观察库推送）后续：必须回来做，因为 Push / Recheck（推送 / 二次复核）已有状态和调度，但低频扫描、机会提升和安全语义仍需统一。
- AI 冲突处理：必须回来做，因为 `AiConflictResolverService` 只是规则化冲突分层，还没有形成多角色证据仲裁和生产降级链。
- PROJECT progress index（项目总进度索引）后续维护：必须回来做，因为项目阶段很多，如果索引不更新，后续很容易把 helper（辅助类）、DTO（数据传输对象）或 display（展示）误当成生产完成。

## 六、容易误判为完成但其实没完成的线路

- Dashboard Risk Reminder（首页风险提醒）已完成只读展示，不等于首页能交易。
- 风险建议展示，不等于交易建议。
- 阻断原因展示，不等于可以执行。
- `marketOrderExitAllowed=false` 说明不允许市价退出。
- `opportunityPushAllowed=false` / `reverseTradeAllowed=false` / `newPositionAllowed=false` 说明机会推送 / 反手 / 新开仓仍关闭。
- `manualRiskReviewRequired=true`（必须人工复核）说明必须人工复核。
- `notTradeInstruction=true`（不是交易指令）说明不是交易指令。
- Dashboard（首页工作台）能集中看风险提醒，不等于能下单。
- Risk Reminder（风险提醒）只能解释为什么不能执行，不能触发执行。
- Risk Action Guard（风险动作保护器）已完成 read-only risk display（只读风险展示），不等于 production risk action（生产风控动作）。
- Position Monitor（持仓监控）有同步 / 告警 / 展示基础，不等于自动平仓。
- Position Monitor（持仓监控）已完成 Strong Reversal（强反转）/ Moving Stop（移动止损）只读展示，不等于 Strong Reversal（强反转）自动识别与自动处理。
- 高风险提示不等于立即止损。
- Strong Reversal（强反转）待确认，不等于直接反手。
- Strong Reversal（强反转）提示不等于直接反手。
- Strong Reversal（强反转）提示不等于自动平仓。
- 原入场逻辑疑似失效，不等于立即退出。
- Moving Stop（移动止损）需要人工复核，不等于自动修改止损。
- Moving Stop（移动止损）提醒不等于自动修改止损。
- Stampede（踩踏）风险提示不等于 opportunity push（机会推送）。
- Wick-only Risk（仅插针风险）/ 插针风险不等于趋势反转。
- auto close / auto reverse / auto stop modification（自动平仓 / 自动反手 / 自动修改止损）均关闭，说明交易动作仍关闭。
- `opportunityPushAllowed=false` / `reverseTradeAllowed=false` / `newPositionAllowed=false` / `marketOrderExitAllowed=false` 说明交易动作仍关闭。
- `notTradeInstruction=true`（不是交易指令）不等于交易建议。
- `manualRiskReviewRequired=true`（必须人工复核）说明必须人工复核。
- Dashboard（首页工作台）能看持仓强反转 / 移动止损状态，不等于能下单。
- Dashboard（首页工作台）能看风险动作状态，不等于能下单。
- SourceTrace（证据来源追踪）已完成 Dashboard（首页工作台）只读展示，不等于真实运行时候选生成完成。
- SourceTrace（证据来源追踪）已显示在页面，不等于可以交易。
- BoundaryCandidate（边界候选交易计划）已完成 read-only candidate display（只读候选展示），不等于真实候选生成完成。
- PlanBoundaryDisplay（计划边界展示）能显示候选状态，不等于可执行计划。
- `REVIEW_ONLY`（只允许复核）/ `INCOMPLETE`（证据不完整）/ `BLOCKED`（禁止推进）/ `WATCH_ONLY`（仅观察）显示出来，不等于方向信号。
- `VALID`（有效候选状态）被安全降级，不等于 production VALID（生产环境有效候选状态）可用。
- adapter（适配器）能展示状态，不等于真实 entry / stop / TP / RR（入场 / 止损 / 止盈 / 盈亏比）已可用。
- Dashboard（首页工作台）能看 BoundaryCandidate（边界候选交易计划）状态，不等于能下单。
- ExecutionPlan（执行计划）已完成 review-only display（只允许复核展示），不等于可执行计划完成。
- `READY_REVIEW_ONLY`（只允许复核的就绪摘要）是只允许复核摘要，不是 Readiness（可执行就绪）已打开。
- ExecutionPlanDisplay（执行计划展示）能展示计划摘要，不等于可以执行。
- `ENTRY_STOP_TP_RR_NOT_GENERATED` 出现，说明真实 entry / stop / TP / RR（入场 / 止损 / 止盈 / 盈亏比）未生成。
- 页面显示 `blockingReasons`（禁止推进原因），不等于交易机会。
- `REVIEW_ONLY`（只允许复核）显示出来，不等于可执行。
- `INCOMPLETE`（证据不完整）/ `BLOCKED`（禁止推进）显示出来，不等于方向信号。
- SourceTrace（证据来源追踪）service wrapper（服务包装器）完成，不等于真实运行时生产接线完成。
- helper（辅助类）/ service（服务）完成，不等于 Controller（控制器）/ API（接口）动作能力完成。
- DTO（数据传输对象）字段完成，不等于真实数据来源完成。
- `INCOMPLETE`（证据不完整）/ `BLOCKED`（禁止推进）guard（保护）完成，不等于 `VALID`（有效候选状态）可以生成。
- BoundaryCandidate（边界候选交易计划）valid factory（有效候选工厂）完成，不等于生产 `VALID`（有效候选状态）可以生成。
- Dashboard（首页工作台）有页面，不等于可执行计划已真实可用。
- Watchlist Pool（观察库池）完成，不等于低频扫描和机会提升完成。

## 七、禁止提前做的线路

- 自动交易。
- 下单接口。
- 自动平仓。
- 自动反手。
- 自动买入 / 自动卖出。
- 自动修改 Stop Loss（止损）/ Moving Stop（移动止损）。
- 真实 entry / stop / TP / RR（入场 / 止损 / 止盈 / 盈亏比）在 SourceTrace（证据来源追踪）/ BoundaryCandidate（边界候选交易计划）来源未闭环前不能做。
- `VALID`（有效候选状态）在来源链路未闭环前不能做。
- Readiness（可执行就绪）在真实边界来源未闭环前不能升级。
- Dashboard（首页工作台）可执行状态在 Readiness（可执行就绪）未闭环前不能打开。
- order（下单）/ execution（执行）/ scheduler（定时器）/ automation（自动化）/ auto-trading（自动交易）不能借 Push / Recheck（推送 / 二次复核）、position sync（持仓同步）或 read-only display（只读展示）名义提前进入。

## 八、推荐下一阶段顺序

### 路线 A：个人可用最快路径

1. Watchlist Low-Frequency Scan / Opportunity Promote Audit（观察库低频扫描 / 机会提升审计）。
2. Alert / Push Channel Review-Only Dispatch Audit（告警 / 推送通道只允许复核调度审计）。
3. Manual Position Review Workflow Audit（手动持仓复核流程审计）。
4. Dashboard Personal Use Smoke Checklist（个人可用冒烟清单）。

### 路线 B：继续严谨后端路径

1. BoundaryCandidate Source Wiring Scope Audit（边界候选来源接线范围审计）。
2. Numeric Source Ownership Audit（数值来源归属审计）。
3. ExecutionPlan Readiness Scope Audit（执行计划可执行就绪范围审计）。
4. Risk Action Guard Production Wiring Scope Audit（风险动作保护器生产接线范围审计）。

推荐结论：

- 如果目标是个人可用，优先路线 A。
- 如果目标是严格生产候选，优先路线 B。
- 但无论哪条路线，都不能直接进入自动交易、自动平仓、自动反手、自动改止损或真实点位。

## 九、模块进度表

| 模块 | 当前状态 | 完成度 | 证据 / 文件线索 | 下一步 |
|---|---|---:|---|---|
| Project Overall（项目总进度） | 安全地基、只读展示、SourceTrace（证据来源追踪）、BoundaryCandidate（边界候选交易计划）、ExecutionPlan（执行计划）、Risk Action Guard（风险动作保护器）、Position Monitor（持仓监控）和 Dashboard Risk Reminder（首页风险提醒）只读展示闭环小幅推进；真实候选、真实点位、Readiness（可执行就绪）、生产风控动作、交易动作、自动交易未完成 | 71%-76% | `PROJECT_PROGRESS_INDEX.md`、`PHASE_BACKEND_P170_SOURCETRACE_READ_ONLY_DISPLAY_CLOSURE.md`、`PHASE_BACKEND_P175_BOUNDARY_CANDIDATE_READ_ONLY_DISPLAY_CLOSURE.md`、`PHASE_BACKEND_P180_EXECUTION_PLAN_REVIEW_ONLY_DISPLAY_CLOSURE.md`、`PHASE_BACKEND_P185_RISK_ACTION_GUARD_POSITION_MONITOR_CLOSURE.md`、`PHASE_BACKEND_P190_POSITION_MONITOR_STRONG_REVERSAL_MOVING_STOP_CLOSURE.md`、`PHASE_BACKEND_P195_DASHBOARD_RISK_REMINDER_READ_ONLY_DISPLAY_CLOSURE.md` | P196 后继续按本索引选择路线 A 或路线 B |
| Safety Foundation（安全地基） | 失败关闭、只读复核、不是交易指令、人工复核、禁止自动动作和首页集中风险提醒的地基继续增强；仍不能替代真实生产授权 | 87%-93% | `StaticNoTradeInstructionGuardTest.java`、`DefaultRiskActionGuardDisplayAdapterTest.java`、多份 P140-P196 文档 | 继续把新增能力先放进范围门和只读复核 |
| SourceTrace（证据来源追踪） | 只读输出 + Dashboard（首页工作台）只读展示已完成；真实候选 / 真实点位 / Readiness（可执行就绪）未完成，本轮不再上调 | 58%-66% | `SourceTraceRuntimePopulationHelper.java`、`SourceTraceRuntimePopulationServiceImpl.java`、`DefaultDashboardSourceTraceDetailAdapter.java`、`dashboard.html`、`PHASE_BACKEND_P170*` | 个人可用路线转观察库或告警推送审计；严谨路线继续 BoundaryCandidate 来源审计 |
| BoundaryCandidate（边界候选交易计划） | DTO（数据对象）/ service skeleton（服务骨架）/ read-only candidate display（只读候选展示）已完成；生产候选 / 真实点位 / production VALID（生产环境有效候选状态）未完成 | 42%-52% | `BoundaryCandidateDTO.java`、`BoundaryCandidateServiceImpl.java`、`BoundaryCandidateServiceImplTest.java`、`DefaultPlanBoundaryDisplayAdapter.java`、`DefaultPlanBoundaryDisplayAdapterTest.java`、`PHASE_BACKEND_P175*` | 个人可用路线转观察库或告警推送审计；严谨路线做 source wiring audit（来源接线审计） |
| ExecutionPlan（执行计划） | 已完成 review-only display（只允许复核展示）；Readiness（可执行就绪）/ 真实点位 / 自动交易未完成 | 45%-55% | `PlanServiceImpl.java`、`ExecutionPlanVO.java`、`DefaultExecutionPlanDisplayAdapter.java`、`DefaultExecutionPlanDisplayAdapterTest.java`、`PHASE_BACKEND_P180_EXECUTION_PLAN_REVIEW_ONLY_DISPLAY_CLOSURE.md` | 继续禁止 Readiness（可执行就绪）升级，严谨路线再做 readiness scope audit（可执行就绪范围审计） |
| Risk Action Guard（风险动作保护器） | 已完成只读风险展示、持仓强反转 / 移动止损只读解释、首页风险提醒集中展示；生产风控动作 / 自动执行未完成 | 47%-57% | `DefaultRiskActionGuardDisplayAdapter.java`、`DefaultRiskActionGuardDisplayAdapterTest.java`、`dashboard.html`、`PHASE_BACKEND_P185_RISK_ACTION_GUARD_POSITION_MONITOR_CLOSURE.md`、`PHASE_BACKEND_P190_POSITION_MONITOR_STRONG_REVERSAL_MOVING_STOP_CLOSURE.md`、`PHASE_BACKEND_P195_DASHBOARD_RISK_REMINDER_READ_ONLY_DISPLAY_CLOSURE.md` | 个人路线做告警 / 推送只读调度审计；严谨路线做生产接线范围审计 |
| Position Monitor（持仓监控） | 同步 / 告警 / 记录基础 + 只读风险展示 + 强反转 / 移动止损只读展示已完成；强反转自动识别 / 自动处理 / 自动改止损 / 自动平仓未完成 | 52%-62% | `PositionSyncService.java`、`PositionSyncScheduler.java`、`RealPositionMapper.java`、`MonitorAlertMapper.java`、`tm_real_position`、`tm_monitor_alert`、`DefaultRiskActionGuardDisplayAdapter.java`、`PHASE_BACKEND_P190*` | 下一步只能走人工复核流程或风险提醒展示，继续禁止自动动作 |
| Dashboard（首页工作台） | SourceTrace（证据来源追踪）+ BoundaryCandidate（边界候选交易计划）+ ExecutionPlan（执行计划）+ RiskActionGuard（风险动作保护器）+ Position Monitor（持仓监控）+ Dashboard Risk Reminder（首页风险提醒）只读展示增强已完成；可执行状态未打开 | 76%-84% | `DashboardController.java`、`dashboard.html`、`DashboardControllerTest.java`、`DefaultPlanBoundaryDisplayAdapter.java`、`DefaultExecutionPlanDisplayAdapter.java`、`DefaultRiskActionGuardDisplayAdapter.java`、`PHASE_BACKEND_P170*`、`PHASE_BACKEND_P175*`、`PHASE_BACKEND_P180*`、`PHASE_BACKEND_P185*`、`PHASE_BACKEND_P190*`、`PHASE_BACKEND_P195*` | 只允许继续做观察库、告警推送或人工复核的只读路径，不打开可执行状态 |
| Watchlist / Push（观察库 / 推送） | Push snapshot（推送快照）、Recheck（二次复核）、scheduler（定时器）和 ops overview（运维总览）存在；低频扫描和机会提升未闭环 | 45%-55% | `PushRecheckServiceImpl.java`、`PushRecheckScheduler.java`、`PushSnapshotService.java`、`PHASE_P11A_PUSH_RECHECK_NAMING_VERIFICATION.md` | 先统一 review-only（只允许复核）命名，再做机会提升审计 |
| AI multi-agent（AI 多角色） | `AiConflictResolverService` 已有冲突分层；多角色生产仲裁链未落地 | 25%-35% | `AiConflictResolverService.java`、`AiConflictResolverServiceImpl.java`、`DecisionEngineServiceTest.java` | 定义多角色输入、冲突降级和人工复核边界 |
| Production Wiring（真实生产接线） | 真实来源、真实候选、生产风控动作、执行授权仍未闭环，不因 P192-P195 只读展示明显上调 | 26%-34% | P140-P196 文档和现有 service / adapter / dashboard 只读链路 | 先做 BoundaryCandidate 来源接线或数值来源归属审计 |
| entry / stop / TP / RR（入场 / 止损 / 止盈 / 盈亏比） | DTO 和 fixture（测试夹具）存在；真实数值来源未闭环，不因只读风险展示完成而上调 | 10%-18% | `BoundaryEntryDTO.java`、`BoundaryStopDTO.java`、`BoundaryTakeProfitLevelDTO.java`、`StopTpRrSourceOwnedCandidateFixtureHelper.java` | 先做 numeric source ownership（数值来源归属）审计 |
| Auto-trading（自动交易） | 未实现；只有明确禁止和无交易指令保护，本轮不因只读展示上调 | 0%-3% | `StaticNoTradeInstructionGuardTest.java`、多份 P140-P196 禁止清单 | 继续禁止，直到真实点位、Readiness（可执行就绪）、风控和授权全部闭环 |

## 十、P196 结论

P196 只刷新 `docs/PROJECT_PROGRESS_INDEX.md`。

P196 不写代码。

P196 不新增测试。

P196 不改 `dashboard.html`。

P196 不接 API（接口）。

P196 不生成交易点位。

P196 不创建 close / reverse / buy / sell（平仓 / 反手 / 买入 / 卖出）动作。

P196 不自动修改 Stop Loss（止损）/ Moving Stop（移动止损）。

P196 不升级 Readiness（可执行就绪）。

P196 不接 auto-trading（自动交易）。

P196 的核心结论是：P192-P195 已经完成 Dashboard Risk Reminder Read-Only Display（首页风险提醒只读展示）闭环，项目总进度、安全地基进度、Risk Action Guard（风险动作保护器）进度和 Dashboard（首页工作台）展示进度可以小幅上调；但 trading actions（交易动作）、production risk action（生产风控动作）、auto close / reverse / buy / sell（自动平仓 / 反手 / 买入 / 卖出）、auto stop modification（自动修改止损）、真实 entry / stop / TP / RR（入场 / 止损 / 止盈 / 盈亏比）、ExecutionPlan Readiness（执行计划可执行就绪）和 auto-trading（自动交易）仍未完成。

后续继续推进必须以 `docs/PROJECT_PROGRESS_INDEX.md` 为准。任何后续阶段如果想打开 SourceTrace（证据来源追踪）、BoundaryCandidate（边界候选交易计划）、ExecutionPlan（执行计划）、Dashboard（首页工作台）、Risk Action Guard（风险动作保护器）、Position Monitor（持仓监控）、Watchlist（观察库）或 auto-trading（自动交易）的新能力，都必须先对照本索引确认它属于“已完成”“正在推进”“暂停”“后期必须回来做”还是“禁止提前做”。
