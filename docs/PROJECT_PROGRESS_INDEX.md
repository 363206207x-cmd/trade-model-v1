# PROJECT_PROGRESS_INDEX

P201 是 Project Progress Index Refresh After Watchlist Scan Promote Semantics（观察库扫描提升语义后项目总进度索引刷新）。

本索引来自 P164 全局扫描后的持续维护。本轮只刷新 `docs/PROJECT_PROGRESS_INDEX.md`，并吸收 P197-P200 Watchlist Low-Frequency Scan / Opportunity Promote（观察库低频扫描 / 机会提升）read-only audit / docs-only semantics（只读审计 / 只改文档语义）闭环结果。P201 不新增 Java，不新增测试，不改 `dashboard.html`，不接 API（接口），不接 `MarketQuoteClient`，不创建 Low-Frequency Scan scheduler（低频扫描定时器），不创建 Opportunity Push execution（机会推送执行），不生成真实 entry / stop / TP / RR（入场 / 止损 / 止盈 / 盈亏比），不创建 close / reverse / buy / sell（平仓 / 反手 / 买入 / 卖出）动作，不升级 Readiness（可执行就绪），不接 auto-trading（自动交易）。

## 一、当前总进度结论

当前项目仍然不是“快完成自动交易”的状态。更准确的状态是：安全地基、只读复核、失败关闭、SourceTrace（证据来源追踪）只读展示、BoundaryCandidate（边界候选交易计划）只读候选展示、ExecutionPlan（执行计划）review-only plan display（只允许复核展示）、Risk Action Guard（风险动作保护器）read-only risk display（只读风险展示）、Position Monitor Strong Reversal / Moving Stop review-only display（持仓强反转 / 移动止损只允许复核展示）、Dashboard Risk Reminder read-only display（首页风险提醒只读展示）以及 Watchlist Low-Frequency Scan / Opportunity Promote read-only audit / docs-only semantics（观察库低频扫描 / 机会提升只读审计 / 只改文档语义）已经更完整；但 real low-frequency scan（真实低频扫描）、Watchlist runtime data source（观察库运行时数据源）、MarketQuoteClient scan integration（行情客户端扫描接入）、scan scheduler（扫描定时器）、Opportunity Promote execution（机会提升执行）、Opportunity Push execution（机会推送执行）、trading buttons（交易按钮）、production candidate generation（生产候选交易计划生成）、trading actions（交易动作）、production risk action（生产风控动作）、production VALID（生产环境有效候选状态）、真实 entry / stop / TP / RR（入场 / 止损 / 止盈 / 盈亏比）、ExecutionPlan Readiness（执行计划可执行就绪）和 auto-trading（自动交易）仍然没有闭环。

P165-P170 已完成 SourceTrace read-only display（证据来源追踪只读展示）闭环。P172-P175 已完成 BoundaryCandidate read-only candidate display（边界候选只读候选展示）闭环。P177-P180 已完成 ExecutionPlan review-only plan display（执行计划只允许复核展示）闭环。P182-P185 已完成 Risk Action Guard / Position Monitor read-only risk display（风险动作保护 / 持仓监控只读风险展示）闭环。P187-P190 已完成 Position Monitor Strong Reversal / Moving Stop Review-Only Pack（持仓强反转 / 移动止损只读复核包）闭环。P192-P195 已完成 Dashboard Risk Reminder Read-Only Display Pack（首页风险提醒只读展示包）闭环。P197-P200 已完成 Watchlist Low-Frequency Scan / Opportunity Promote Audit Pack（观察库低频扫描 / 机会提升审计包）闭环。所以项目总进度可以小幅上调。但这个上调只代表“边界更清楚、只读复核语义更完整”，不代表真实扫描器、实时数据读取、推送执行、交易计划生成、生产 `VALID`（有效候选状态）、真实点位、Readiness（可执行就绪）、交易动作或自动交易完成。

| 项目线 | 当前真实进度 |
|---|---:|
| 项目总进度 | 72%-77% |
| 安全地基进度 | 88%-94% |
| Watchlist / Display Slots / Opportunity Promote（观察库 / 首页展示位 / 机会提升）语义进度 | 65%-75% |
| SourceTrace（证据来源追踪）进度 | 58%-66% |
| BoundaryCandidate（边界候选交易计划）进度 | 42%-52% |
| ExecutionPlan（执行计划）进度 | 45%-55% |
| Risk Action Guard（风险动作保护器）进度 | 47%-57% |
| Position Monitor（持仓监控）进度 | 52%-62% |
| dashboard（首页工作台）展示进度 | 76%-84% |
| 真实生产接线进度 | 26%-34% |
| entry / stop / TP / RR（入场 / 止损 / 止盈 / 盈亏比）真实点位进度 | 10%-18% |
| 自动交易进度 | 0%-3% |

这些百分比按“能否安全进入真实生产链路”估算，不按文档数量估算。P197-P200 让 Watchlist / Display Slots / Opportunity Promote（观察库 / 首页展示位 / 机会提升）的语义边界明显更清楚，因此新增该模块进度为 65%-75%。Dashboard（首页工作台）保持 76%-84%，不因 P201 文档刷新明显上调。SourceTrace（证据来源追踪）、BoundaryCandidate（边界候选交易计划）、ExecutionPlan（执行计划）、Risk Action Guard（风险动作保护器）、Position Monitor（持仓监控）、真实生产接线、真实点位和自动交易保持谨慎，因为真实交易计划链路仍缺 source-owned runtime candidate generation（运行时证据来源候选生成）、runtime data source（运行时数据源）、numeric source ownership（数值来源归属）、production risk action（生产风控动作）、production VALID（生产环境有效候选状态）、Readiness（可执行就绪）和交易动作闭环。

## 二、已完成线路

- P140-P163 SourceTrace（证据来源追踪）/ Production Wiring Preparation（生产接线准备）已完成：已经完成范围门、缺口审计、输入契约、设计矩阵、测试计划、`INCOMPLETE`（证据不完整）guard（保护）、`BLOCKED`（禁止推进）guard（保护）、SourceTrace runtime population（运行时证据来源填充）helper（辅助类）、SourceTrace service wrapper（服务包装器）和收口文档。
- P160-P163 SourceTrace Service Wiring Pack（证据来源追踪服务层接线包）已完成：`SourceTraceRuntimePopulationService` 和 `SourceTraceRuntimePopulationServiceImpl` 已存在，service wrapper（服务包装器）只调用 `SourceTraceRuntimePopulationHelper.populate(...)`。
- P165-P170 SourceTrace read-model / dashboard read-only display pack（证据来源追踪只读输出 / 首页只读展示包）已完成。
- P165 完成 SourceTrace Read Model / Controller Scope Audit（证据来源追踪只读输出范围审计）。
- P166 完成 SourceTrace Read Model Authorization Gate（证据来源追踪只读输出授权门）。
- P167 完成 detail read model / adapter（详情只读模型 / 适配器）最小只读接线。
- P168 完成 SourceTrace Dashboard Display Scope Gate（首页证据来源追踪展示范围门）。
- P169 完成 `dashboard.html` 只读展示。
- P170 完成 SourceTrace Read-Only Display Closure（证据来源追踪只读展示收口）。
- P172-P175 BoundaryCandidate Read-Only Candidate Display Pack（边界候选只读候选展示包）已完成。
- P172 完成 BoundaryCandidate Read-Only Candidate Display Scope Audit（边界候选只读候选展示范围审计）。
- P173 完成 BoundaryCandidate Read-Only Candidate Display Authorization Gate（边界候选只读候选展示授权门）。
- P174 完成 PlanBoundaryDisplay（计划边界展示）/ `DefaultPlanBoundaryDisplayAdapter` 最小只读接线。
- P174 对 `VALID`（有效候选状态）做安全降级，不作为 production VALID（生产环境有效候选状态）输出。
- P175 完成 BoundaryCandidate Read-Only Candidate Display Closure（边界候选只读候选展示收口）。
- P177-P180 ExecutionPlan Review-Only Plan Display Pack（执行计划只允许复核展示包）已完成。
- P177 完成 ExecutionPlan Review-Only Plan Display Scope Audit（执行计划只允许复核展示范围审计）。
- P178 完成 ExecutionPlan Review-Only Plan Display Authorization Gate（执行计划只允许复核展示授权门）。
- P179 完成 `DefaultExecutionPlanDisplayAdapter` / `DefaultExecutionPlanDisplayAdapterTest` 最小只读接线。
- P179 没有升级 Readiness（可执行就绪），没有生成真实 entry / stop / TP / RR（入场 / 止损 / 止盈 / 盈亏比），没有改 `dashboard.html`，没有接 API（接口），没有接自动交易。
- P180 完成 ExecutionPlan Review-Only Plan Display Closure（执行计划只允许复核展示收口）。
- P182-P185 Risk Action Guard / Position Monitor Read-Only Risk Display Pack（风险动作保护 / 持仓监控只读风险展示包）已完成。
- P182 完成 Risk Action Guard / Position Monitor Scope Audit（风险动作保护和持仓监控范围审计）。
- P183 完成 Risk Action Guard / Position Monitor Authorization Gate（风险动作保护和持仓监控授权门）。
- P184 完成 `DefaultRiskActionGuardDisplayAdapter` / `DefaultRiskActionGuardDisplayAdapterTest` 最小只读接线。
- P185 完成 Risk Action Guard / Position Monitor Closure（风险动作保护和持仓监控收口）。
- RiskActionGuardDisplay（风险动作保护展示）继续保持 `opportunityPushAllowed=false`、`reverseTradeAllowed=false`、`newPositionAllowed=false`、`marketOrderExitAllowed=false`、`manualRiskReviewRequired=true`、`notTradeInstruction=true`。
- P187-P190 Position Monitor Strong Reversal / Moving Stop Review-Only Pack（持仓强反转 / 移动止损只读复核包）已完成。
- P187 完成 Position Monitor Strong Reversal / Moving Stop Review-Only Scope Audit（持仓强反转 / 移动止损只读复核范围审计）。
- P188 完成 Position Monitor Strong Reversal / Moving Stop Authorization Gate（持仓强反转 / 移动止损授权门）。
- P189 完成 `DefaultRiskActionGuardDisplayAdapter` / `DefaultRiskActionGuardDisplayAdapterTest` 最小只读接线。
- P190 完成 Position Monitor Strong Reversal / Moving Stop Closure（持仓强反转 / 移动止损收口）。
- RiskActionGuardDisplay（风险动作保护展示）现在能更清楚展示 Strong Reversal（强反转）待确认、原入场逻辑疑似失效、Moving Stop（移动止损）需要人工复核、Strong Reversal（强反转）不等于反手或自动平仓、Moving Stop（移动止损）不等于自动改 Stop Loss（止损）、auto close / auto reverse / auto stop modification（自动平仓 / 自动反手 / 自动修改止损）均关闭、不生成真实 entry / stop / TP / RR（入场 / 止损 / 止盈 / 盈亏比）、不升级 Readiness（可执行就绪）。
- P189 没有创建 close / reverse / buy / sell（平仓 / 反手 / 买入 / 卖出）动作，没有自动修改 Stop Loss（止损）/ Moving Stop（移动止损），没有生成真实 entry / stop / TP / RR，没有改 `dashboard.html`，没有接 API，没有升级 Readiness，没有接 auto-trading（自动交易）。
- P192-P195 Dashboard Risk Reminder Read-Only Display Pack（首页风险提醒只读展示包）已完成。
- P192 完成 Dashboard Risk Reminder Read-Only Display Scope Audit（首页风险提醒只读展示范围审计）。
- P193 完成 Dashboard Risk Reminder Read-Only Display Authorization Gate（首页风险提醒只读展示授权门）。
- P194 完成 `dashboard.html` 最小 Read-Only Display（只读展示）接线。
- P195 完成 Dashboard Risk Reminder Read-Only Display Closure（首页风险提醒只读展示收口）。
- Dashboard（首页工作台）现在能更集中展示 `riskActionAdvice`（风险动作建议）、`riskActionBlockingReason`（风险动作阻断原因）、`liquidityState`（流动性状态）、`stampedeDetected`（是否检测到踩踏）、`wickOnlyRisk`（是否仅插针风险）、`marketOrderExitAllowed`（是否允许市价退出）、`opportunityPushAllowed`（是否允许机会推送）、`reverseTradeAllowed`（是否允许反手）、`newPositionAllowed`（是否允许新开仓）、`manualRiskReviewRequired`（是否必须人工复核）和 `notTradeInstruction`（是否不是交易指令）。
- Dashboard（首页工作台）现在能集中展示“不是交易指令”“必须人工复核”“不连接 order API（下单接口）”“不触发自动交易动作”“不生成真实点位”“自动平仓 / 自动反手 / 自动改止损关闭”。
- P194 只修改 `src/main/resources/templates/dashboard.html`。P194 没有改 Java，没有改 `DashboardController.java` / `DashboardDetailResponseVO.java`，没有接 API（接口），没有新增按钮，没有创建 close / reverse / buy / sell（平仓 / 反手 / 买入 / 卖出）动作，没有自动修改 Stop Loss（止损）/ Moving Stop（移动止损），没有生成真实 entry / stop / TP / RR，没有升级 Readiness（可执行就绪），没有接 auto-trading（自动交易）。
- P197-P200 Watchlist Low-Frequency Scan / Opportunity Promote Audit Pack（观察库低频扫描 / 机会提升审计包）已完成。
- P197 完成 Watchlist Low-Frequency Scan / Opportunity Promote Scope Audit（观察库低频扫描 / 机会提升范围审计）。
- P198 完成 Watchlist Low-Frequency Scan / Opportunity Promote Authorization Gate（观察库低频扫描 / 机会提升授权门）。
- P199 完成 docs-only minimal wiring（只改文档的最小接线）。
- P200 完成 Watchlist Low-Frequency Scan / Opportunity Promote Closure（观察库低频扫描 / 机会提升收口）。
- 已明确语义：Display Slots（首页展示位）是首页展示优先级，不是推送全集。
- 已明确语义：Watchlist Pool（观察库池）才是推送候选最大边界。
- 已明确语义：首页默认 6 个资产不是后端推送全集，也不是唯一观察库。
- 已明确语义：观察库可以多于 6 个。
- 已明确语义：Low-Frequency Scan（低频扫描）未来只能从 Watchlist Pool（观察库池）开始。
- 已明确语义：Opportunity Promote（机会提升）只是提升到首页观察 / 人工复核。
- 已明确语义：Opportunity Promote（机会提升）不是 Opportunity Push execution（机会推送执行）。
- 已明确语义：Opportunity Promote（机会提升）不是订单。
- 已明确语义：Opportunity Promote（机会提升）不是交易信号。
- 已明确语义：Opportunity Promote（机会提升）不是 Readiness（可执行就绪）。
- 已明确语义：Opportunity Promote（机会提升）不生成 entry / stop / TP / RR（入场 / 止损 / 止盈 / 盈亏比）。
- 已明确语义：默认六币不能作为默认推送全集。
- 已明确语义：非观察库资产不能进入候选。
- P199 没有改 `dashboard.html`。
- P199 没有改 Java。
- P199 没有新增测试。
- P199 没有新增 API。
- P199 没有新增 schema / config / mapper / service（数据库结构 / 配置 / 映射 / 服务）。
- P199 没有接 `PushRecheckScheduler` / `PushRecheckService` / `PushSnapshotService`。
- P199 没有接 `RuleController` / `RuleConfigService` / `RuleConfigMapper`。
- P199 没有接 `MarketQuoteClient`。
- P199 没有创建 Low-Frequency Scan scheduler（低频扫描定时器）。
- P199 没有创建 Opportunity Push execution（机会推送执行）。
- P199 没有生成真实 entry / stop / TP / RR。
- P199 没有升级 Readiness（可执行就绪）。
- P199 没有接自动交易。
- BoundaryCandidate（边界候选交易计划）DTO / valid factory（有效候选工厂）/ service skeleton（服务骨架）已完成，但这不等于 production VALID（生产环境有效候选状态）已经可生成。
- RuntimeKlineContext（运行时 K 线上下文）/ BoundaryCandidateService（边界候选服务）相关已完成，但这不等于真实交易点位完成。
- Dashboard（首页工作台）已完成 SourceTrace（证据来源追踪）、BoundaryCandidate（边界候选交易计划）、ExecutionPlan（执行计划）、RiskActionGuard（风险动作保护展示）、PaperObservationDisplay（纸面观察展示）、Position Monitor（持仓监控）和 Dashboard Risk Reminder（首页风险提醒）只读展示。
- position sync（持仓同步）/ manual position（手动或模拟持仓）/ monitoring（监控）已完成基础能力，但当前更像同步、告警、记录和只读风险展示基础，不是自动平仓或自动反手。

## 三、正在推进线路

Watchlist Low-Frequency Scan / Opportunity Promote（观察库低频扫描 / 机会提升）语义边界已完成。当前不应该继续把语义边界误推进成扫描器、实时数据读取、机会推送执行、真实点位、Readiness（可执行就绪）或 auto-trading（自动交易）。

如果目标是个人可用最快路径，建议进入：

1. Alert / Push Channel Review-Only Dispatch Audit（告警 / 推送通道只允许复核调度审计）。
2. Manual Position Review Workflow Audit（手动持仓复核流程审计）。
3. Dashboard Personal Use Smoke Checklist（个人可用冒烟清单）。

如果目标是开始真正实现 Low-Frequency Scan（低频扫描），必须先进入：

1. Low-Frequency Scan Scheduler Scope Audit（低频扫描定时器范围审计）。
2. Watchlist Runtime Data Source Audit（观察库运行时数据源审计）。
3. WatchlistScanResult DTO / Contract Audit（观察库扫描结果 DTO / 契约审计）。
4. ScanScore Rule Definition Audit（扫描分数规则定义审计）。

如果目标是严谨后端交易候选，仍建议进入：

1. BoundaryCandidate Source Wiring Scope Audit（边界候选来源接线范围审计）。
2. Numeric Source Ownership Audit（数值来源归属审计）。
3. ExecutionPlan Readiness Scope Audit（执行计划可执行就绪范围审计）。

无论走哪条路线，都不能直接进入扫描器、实时数据读取、机会推送执行、自动平仓、自动反手、自动修改止损或真实点位。

## 四、暂停线路

- real low-frequency scan（真实低频扫描）：仍暂停，原因是 P197-P200 只完成语义和边界，不创建扫描器。
- Watchlist runtime data source（观察库运行时数据源）：仍暂停，原因是没有审计数据来源、刷新频率、缺失字段和失败关闭规则。
- MarketQuoteClient scan integration（行情客户端扫描接入）：仍暂停，原因是 P199/P200 明确不接 `MarketQuoteClient`。
- scan scheduler（扫描定时器）：仍暂停，原因是没有授权 Low-Frequency Scan scheduler（低频扫描定时器）。
- Opportunity Promote execution（机会提升执行）：仍暂停，原因是 Opportunity Promote（机会提升）当前只是提升到首页观察 / 人工复核语义。
- Opportunity Push execution（机会推送执行）：仍暂停，原因是没有推送执行授权，也没有交易动作授权。
- default-six opportunity push（默认六币机会推送）：仍禁止，原因是默认六币只是 Display Slots（首页展示位）空态 / 排序，不是推送全集。
- non-watchlist push candidate（非观察库推送候选）：仍禁止，原因是不在 Watchlist Pool（观察库池）的资产不能进入候选。
- trading actions（交易动作）：仍暂停，原因是只读展示和文档语义不授权平仓、反手、买入、卖出或下单。
- production risk action（生产风控动作）：仍暂停，原因是 Risk Action Guard（风险动作保护器）仍是只读展示，不是生产动作执行。
- auto close / auto reverse / auto buy / auto sell（自动平仓 / 自动反手 / 自动买入 / 自动卖出）：仍暂停，原因是没有授权交易动作和动作接口。
- auto stop modification（自动修改止损）：仍暂停，原因是移动止损和风险提醒仍然只能人工复核。
- Strong Reversal（强反转）automation（自动化）：仍暂停，原因是 P187-P190 完成的是只读展示，不是强反转自动识别或自动处理。
- Moving Stop（移动止损）automation（自动化）：仍暂停，原因是移动止损目前只能作为人工复核提醒，不能自动修改止损。
- ExecutionPlan readiness（执行计划可执行就绪）：仍暂停，原因是 P177-P180 完成的是 review-only display（只允许复核展示），不是可执行计划。
- BoundaryCandidate（边界候选交易计划）真实候选生成：仍暂停，原因是只读候选展示完成不等于真实来源接线和生产候选生成闭环。
- production VALID（生产环境有效候选状态）：仍暂停，原因是 `VALID`（有效候选状态）没有生产来源闭环。
- entry / stop / TP / RR（入场 / 止损 / 止盈 / 盈亏比）真实点位：仍暂停，原因是 numeric source ownership（数值来源归属）没有真实闭环。
- AI 多角色冲突处理落地：仍暂停，原因是已有 `AiConflictResolverService`，但多角色冲突处理还不是完整生产裁决链。
- 自动交易：仍暂停，原因是没有 order API（下单接口）、execution API（执行接口）、自动平仓或自动反手授权。

## 五、后期必须回来做的线路

- BoundaryCandidate（边界候选交易计划）真实来源接线：必须回来做，因为没有真实来源，`VALID`（有效候选状态）只能停留在 DTO / 测试或局部服务语义。
- ExecutionPlan Readiness（执行计划可执行就绪）：必须回来做，因为 P177-P180 只完成 review-only display（只允许复核展示），真实边界来源、真实点位和风险保护没有闭环前不能升级。
- Risk Action Guard（风险动作保护器）生产接线：必须回来做，因为 P182-P185 只完成只读风险展示，stampede（踩踏）、强反转、移动止损、流动性恶化这些风险还没有生产动作链路。
- Position Monitor（持仓监控）：必须回来做，因为 position sync（持仓同步）和只读风险展示只说明当前持仓可被记录和解释，不说明系统能安全处理强反转、移动止损或退出策略。
- Watchlist / Opportunity Promote（观察库 / 机会提升）后续：必须回来做，因为 P197-P200 只完成语义和安全边界，真实低频扫描、运行时数据源、扫描结果契约、扫描分数、机会提升执行和机会推送执行都还没有完成。
- Alert / Push Channel（告警 / 推送通道）后续：必须回来做，因为 Push / Recheck（推送 / 二次复核）已有状态和调度，但仍需要 review-only（只允许复核）调度审计，不能直接变成交易推送。
- AI 冲突处理：必须回来做，因为 `AiConflictResolverService` 只是规则化冲突分层，还没有形成多角色证据仲裁和生产降级链。
- PROJECT progress index（项目总进度索引）后续维护：必须回来做，因为项目阶段很多，如果索引不更新，后续很容易把 helper（辅助类）、DTO（数据传输对象）或 display（展示）误当成生产完成。

## 六、容易误判为完成但其实没完成的线路

- Watchlist Low-Frequency Scan（观察库低频扫描）语义完成，不等于低频扫描器完成。
- Opportunity Promote（机会提升）语义完成，不等于 Opportunity Push execution（机会推送执行）完成。
- Display Slots（首页展示位）不是推送候选。
- 默认六币不是默认推送全集。
- Watchlist Pool（观察库池）是最大候选边界，但不是自动交易候选。
- 非观察库资产不能进入推送候选。
- Opportunity Promote（机会提升）只是提升到首页观察 / 人工复核。
- Opportunity Promote（机会提升）不是订单。
- Opportunity Promote（机会提升）不是交易信号。
- Opportunity Promote（机会提升）不是 Readiness（可执行就绪）。
- docs-only minimal wiring（只改文档的最小接线）不等于运行时功能。
- 没有 MarketQuoteClient scan integration（行情客户端扫描接入），不等于可扫实时行情。
- 没有 scheduler（定时器），不等于系统会自动扫描。
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
- Strong Reversal（强反转）提示不等于自动平仓。
- 原入场逻辑疑似失效，不等于立即退出。
- Moving Stop（移动止损）需要人工复核，不等于自动修改止损。
- Stampede（踩踏）风险提示不等于 Opportunity Push（机会推送）。
- Wick-only Risk（仅插针风险）/ 插针风险不等于趋势反转。
- auto close / auto reverse / auto stop modification（自动平仓 / 自动反手 / 自动修改止损）均关闭，说明交易动作仍关闭。
- SourceTrace（证据来源追踪）已完成 Dashboard（首页工作台）只读展示，不等于真实运行时候选生成完成。
- BoundaryCandidate（边界候选交易计划）已完成 read-only candidate display（只读候选展示），不等于真实候选生成完成。
- `VALID`（有效候选状态）被安全降级，不等于 production VALID（生产环境有效候选状态）可用。
- ExecutionPlan（执行计划）已完成 review-only display（只允许复核展示），不等于可执行计划完成。
- `READY_REVIEW_ONLY`（只允许复核的就绪摘要）是只允许复核摘要，不是 Readiness（可执行就绪）已打开。
- `ENTRY_STOP_TP_RR_NOT_GENERATED` 出现，说明真实 entry / stop / TP / RR（入场 / 止损 / 止盈 / 盈亏比）未生成。
- helper（辅助类）/ service（服务）完成，不等于 Controller（控制器）/ API（接口）动作能力完成。
- DTO（数据传输对象）字段完成，不等于真实数据来源完成。
- Watchlist Pool（观察库池）完成，不等于低频扫描和机会提升执行完成。

## 七、禁止提前做的线路

- 自动交易。
- 下单接口。
- 自动平仓。
- 自动反手。
- 自动买入 / 自动卖出。
- 自动修改 Stop Loss（止损）/ Moving Stop（移动止损）。
- Low-Frequency Scan scheduler（低频扫描定时器）。
- Watchlist runtime data source（观察库运行时数据源）。
- MarketQuoteClient scan integration（行情客户端扫描接入）。
- Opportunity Promote execution（机会提升执行）。
- Opportunity Push execution（机会推送执行）。
- default-six opportunity push（默认六币机会推送）。
- non-watchlist asset -> push candidate（非观察库资产进入推送候选）。
- 真实 entry / stop / TP / RR（入场 / 止损 / 止盈 / 盈亏比）在 SourceTrace（证据来源追踪）/ BoundaryCandidate（边界候选交易计划）来源未闭环前不能做。
- `VALID`（有效候选状态）在来源链路未闭环前不能做。
- Readiness（可执行就绪）在真实边界来源未闭环前不能升级。
- Dashboard（首页工作台）可执行状态在 Readiness（可执行就绪）未闭环前不能打开。
- order（下单）/ execution（执行）/ scheduler（定时器）/ automation（自动化）/ auto-trading（自动交易）不能借 Push / Recheck（推送 / 二次复核）、Watchlist（观察库）、Opportunity Promote（机会提升）、position sync（持仓同步）或 read-only display（只读展示）名义提前进入。

## 八、推荐下一阶段顺序

### 路线 A：个人可用最快路径

1. Alert / Push Channel Review-Only Dispatch Audit（告警 / 推送通道只允许复核调度审计）。
2. Manual Position Review Workflow Audit（手动持仓复核流程审计）。
3. Dashboard Personal Use Smoke Checklist（个人可用冒烟清单）。
4. Personal Paper Trading Observation Plan（个人纸面观察计划）。

### 路线 B：开始真实低频扫描前置

1. Low-Frequency Scan Scheduler Scope Audit（低频扫描定时器范围审计）。
2. Watchlist Runtime Data Source Audit（观察库运行时数据源审计）。
3. WatchlistScanResult DTO / Contract Audit（观察库扫描结果 DTO / 契约审计）。
4. ScanScore Rule Definition Audit（扫描分数规则定义审计）。
5. Low-Cost AI Event Explanation Gate（低成本 AI 事件解释授权门）。
6. Three-AI Promote-To-Home Review Gate（三 AI 提升到首页复核授权门）。

### 路线 C：继续严谨后端交易候选

1. BoundaryCandidate Source Wiring Scope Audit（边界候选来源接线范围审计）。
2. Numeric Source Ownership Audit（数值来源归属审计）。
3. ExecutionPlan Readiness Scope Audit（执行计划可执行就绪范围审计）。
4. Risk Action Guard Production Wiring Scope Audit（风险动作保护器生产接线范围审计）。

推荐结论：

- 如果目标是个人可用，优先路线 A。
- 如果目标是开始做真实 Low-Frequency Scan（低频扫描），先走路线 B，但第一步仍然是审计，不是直接写 scheduler（定时器）。
- 如果目标是严格生产候选，优先路线 C。
- 无论哪条路线，都不能直接进入自动交易、自动平仓、自动反手、自动改止损、真实点位或 Opportunity Push execution（机会推送执行）。

## 九、模块进度表

| 模块 | 当前状态 | 完成度 | 证据 / 文件线索 | 下一步 |
|---|---|---:|---|---|
| Project Overall（项目总进度） | 安全地基、只读展示、SourceTrace（证据来源追踪）、BoundaryCandidate（边界候选交易计划）、ExecutionPlan（执行计划）、Risk Action Guard（风险动作保护器）、Position Monitor（持仓监控）、Dashboard Risk Reminder（首页风险提醒）和 Watchlist / Opportunity Promote（观察库 / 机会提升）语义边界继续推进；真实扫描器、真实候选、真实点位、Readiness（可执行就绪）、生产风控动作、交易动作、自动交易未完成 | 72%-77% | `PROJECT_PROGRESS_INDEX.md`、`PHASE_BACKEND_P170_SOURCETRACE_READ_ONLY_DISPLAY_CLOSURE.md`、`PHASE_BACKEND_P175_BOUNDARY_CANDIDATE_READ_ONLY_DISPLAY_CLOSURE.md`、`PHASE_BACKEND_P180_EXECUTION_PLAN_REVIEW_ONLY_DISPLAY_CLOSURE.md`、`PHASE_BACKEND_P185_RISK_ACTION_GUARD_POSITION_MONITOR_CLOSURE.md`、`PHASE_BACKEND_P190_POSITION_MONITOR_STRONG_REVERSAL_MOVING_STOP_CLOSURE.md`、`PHASE_BACKEND_P195_DASHBOARD_RISK_REMINDER_READ_ONLY_DISPLAY_CLOSURE.md`、`PHASE_BACKEND_P200_WATCHLIST_LOW_FREQUENCY_SCAN_OPPORTUNITY_PROMOTE_CLOSURE.md` | P201 后继续按本索引选择路线 A / B / C |
| Safety Foundation（安全地基） | 失败关闭、只读复核、不是交易指令、人工复核、禁止自动动作、首页集中风险提醒和观察库扫描提升语义边界继续增强；仍不能替代真实生产授权 | 88%-94% | `StaticNoTradeInstructionGuardTest.java`、`DefaultRiskActionGuardDisplayAdapterTest.java`、多份 P140-P201 文档 | 继续把新增能力先放进范围门和只读复核 |
| SourceTrace（证据来源追踪） | 只读输出 + Dashboard（首页工作台）只读展示已完成；真实候选 / 真实点位 / Readiness（可执行就绪）未完成，本轮不再上调 | 58%-66% | `SourceTraceRuntimePopulationHelper.java`、`SourceTraceRuntimePopulationServiceImpl.java`、`DefaultDashboardSourceTraceDetailAdapter.java`、`dashboard.html`、`PHASE_BACKEND_P170*` | 个人可用路线转告警推送审计；严谨路线继续 BoundaryCandidate 来源审计 |
| BoundaryCandidate（边界候选交易计划） | DTO（数据对象）/ service skeleton（服务骨架）/ read-only candidate display（只读候选展示）已完成；生产候选 / 真实点位 / production VALID（生产环境有效候选状态）未完成 | 42%-52% | `BoundaryCandidateDTO.java`、`BoundaryCandidateServiceImpl.java`、`BoundaryCandidateServiceImplTest.java`、`DefaultPlanBoundaryDisplayAdapter.java`、`DefaultPlanBoundaryDisplayAdapterTest.java`、`PHASE_BACKEND_P175*` | 严谨路线做 source wiring audit（来源接线审计） |
| ExecutionPlan（执行计划） | 已完成 review-only display（只允许复核展示）；Readiness（可执行就绪）/ 真实点位 / 自动交易未完成 | 45%-55% | `PlanServiceImpl.java`、`ExecutionPlanVO.java`、`DefaultExecutionPlanDisplayAdapter.java`、`DefaultExecutionPlanDisplayAdapterTest.java`、`PHASE_BACKEND_P180_EXECUTION_PLAN_REVIEW_ONLY_DISPLAY_CLOSURE.md` | 继续禁止 Readiness（可执行就绪）升级，严谨路线再做 readiness scope audit（可执行就绪范围审计） |
| Risk Action Guard（风险动作保护器） | 已完成只读风险展示、持仓强反转 / 移动止损只读解释、首页风险提醒集中展示；生产风控动作 / 自动执行未完成，本轮不明显上调 | 47%-57% | `DefaultRiskActionGuardDisplayAdapter.java`、`DefaultRiskActionGuardDisplayAdapterTest.java`、`dashboard.html`、`PHASE_BACKEND_P185_RISK_ACTION_GUARD_POSITION_MONITOR_CLOSURE.md`、`PHASE_BACKEND_P190_POSITION_MONITOR_STRONG_REVERSAL_MOVING_STOP_CLOSURE.md`、`PHASE_BACKEND_P195_DASHBOARD_RISK_REMINDER_READ_ONLY_DISPLAY_CLOSURE.md` | 个人路线做告警 / 推送只读调度审计；严谨路线做生产接线范围审计 |
| Position Monitor（持仓监控） | 同步 / 告警 / 记录基础 + 只读风险展示 + 强反转 / 移动止损只读展示已完成；强反转自动识别 / 自动处理 / 自动改止损 / 自动平仓未完成，本轮不明显上调 | 52%-62% | `PositionSyncService.java`、`PositionSyncScheduler.java`、`RealPositionMapper.java`、`MonitorAlertMapper.java`、`tm_real_position`、`tm_monitor_alert`、`DefaultRiskActionGuardDisplayAdapter.java`、`PHASE_BACKEND_P190*` | 下一步只能走人工复核流程或风险提醒展示，继续禁止自动动作 |
| Dashboard（首页工作台） | SourceTrace（证据来源追踪）+ BoundaryCandidate（边界候选交易计划）+ ExecutionPlan（执行计划）+ RiskActionGuard（风险动作保护器）+ Position Monitor（持仓监控）+ Dashboard Risk Reminder（首页风险提醒）只读展示增强已完成；可执行状态未打开，本轮不明显上调 | 76%-84% | `DashboardController.java`、`dashboard.html`、`DashboardControllerTest.java`、`DefaultPlanBoundaryDisplayAdapter.java`、`DefaultExecutionPlanDisplayAdapter.java`、`DefaultRiskActionGuardDisplayAdapter.java`、`PHASE_BACKEND_P170*`、`PHASE_BACKEND_P175*`、`PHASE_BACKEND_P180*`、`PHASE_BACKEND_P185*`、`PHASE_BACKEND_P190*`、`PHASE_BACKEND_P195*` | 只允许继续做告警推送、人工复核或个人可用冒烟，不打开可执行状态 |
| Watchlist / Display Slots / Opportunity Promote（观察库 / 首页展示位 / 机会提升） | Display Slots / Watchlist Pool / Low-Frequency Scan / Opportunity Promote（首页展示位 / 观察库池 / 低频扫描 / 机会提升）语义边界已完成；真实扫描器、实时数据、推送执行、自动交易未完成 | 65%-75% | `PHASE_BACKEND_P197_WATCHLIST_LOW_FREQUENCY_SCAN_OPPORTUNITY_PROMOTE_SCOPE_AUDIT.md`、`PHASE_BACKEND_P198_WATCHLIST_LOW_FREQUENCY_SCAN_OPPORTUNITY_PROMOTE_AUTHORIZATION_GATE.md`、`PHASE_BACKEND_P199_WATCHLIST_LOW_FREQUENCY_SCAN_OPPORTUNITY_PROMOTE_MINIMAL_WIRING.md`、`PHASE_BACKEND_P200_WATCHLIST_LOW_FREQUENCY_SCAN_OPPORTUNITY_PROMOTE_CLOSURE.md`、`dashboard.html` | 个人路线做告警 / 推送只读调度审计；真实扫描路线先做 scheduler / data source / DTO contract 审计 |
| Watchlist / Push（观察库 / 推送） | Push snapshot（推送快照）、Recheck（二次复核）、scheduler（定时器）和 ops overview（运维总览）存在；但它们不是 Watchlist Low-Frequency Scan（观察库低频扫描），不是 Opportunity Push execution（机会推送执行） | 45%-55% | `PushRecheckServiceImpl.java`、`PushRecheckScheduler.java`、`PushSnapshotService.java`、`PHASE_P11A_PUSH_RECHECK_NAMING_VERIFICATION.md` | 先做 Alert / Push Channel Review-Only Dispatch Audit（告警 / 推送通道只允许复核调度审计） |
| AI multi-agent（AI 多角色） | `AiConflictResolverService` 已有冲突分层；多角色生产仲裁链未落地 | 25%-35% | `AiConflictResolverService.java`、`AiConflictResolverServiceImpl.java`、`DecisionEngineServiceTest.java` | 定义多角色输入、冲突降级和人工复核边界 |
| Production Wiring（真实生产接线） | 真实来源、真实候选、生产风控动作、执行授权仍未闭环，不因 P197-P200 文档语义明显上调 | 26%-34% | P140-P201 文档和现有 service / adapter / dashboard 只读链路 | 先做 BoundaryCandidate 来源接线或数值来源归属审计 |
| entry / stop / TP / RR（入场 / 止损 / 止盈 / 盈亏比） | DTO 和 fixture（测试夹具）存在；真实数值来源未闭环，不因观察库语义完成而上调 | 10%-18% | `BoundaryEntryDTO.java`、`BoundaryStopDTO.java`、`BoundaryTakeProfitLevelDTO.java`、`StopTpRrSourceOwnedCandidateFixtureHelper.java` | 先做 numeric source ownership（数值来源归属）审计 |
| Auto-trading（自动交易） | 未实现；只有明确禁止和无交易指令保护，本轮不因文档语义上调 | 0%-3% | `StaticNoTradeInstructionGuardTest.java`、多份 P140-P201 禁止清单 | 继续禁止，直到真实点位、Readiness（可执行就绪）、风控和授权全部闭环 |

## 十、P201 结论

P201 只刷新 `docs/PROJECT_PROGRESS_INDEX.md`。

P201 不写代码。

P201 不新增测试。

P201 不改 `dashboard.html`。

P201 不接 API（接口）。

P201 不接 `MarketQuoteClient`。

P201 不创建 Low-Frequency Scan scheduler（低频扫描定时器）。

P201 不创建 Opportunity Push execution（机会推送执行）。

P201 不生成交易点位。

P201 不创建 close / reverse / buy / sell（平仓 / 反手 / 买入 / 卖出）动作。

P201 不升级 Readiness（可执行就绪）。

P201 不接 auto-trading（自动交易）。

P201 本轮严格禁止并确认：

- 不新增 Java。
- 不新增测试。
- 不改 production Java。
- 不改现有测试。
- 不改 `dashboard.html`。
- 不新增 controller / endpoint / API / schema / config / service / mapper（控制器 / 接口 / API / 数据库结构 / 配置 / 服务 / 映射）。
- 不改 `PushRecheckScheduler` / `PushRecheckService` / `PushSnapshotService`。
- 不改 `RuleController` / `RuleConfigService` / `RuleConfigMapper`。
- 不读取 runtime / live / external data（运行时 / 实时 / 外部数据）。
- 不接 `MarketQuoteClient`。
- 不创建 Low-Frequency Scan scheduler（低频扫描定时器）。
- 不创建 Opportunity Push execution（机会推送执行）。
- 不生成真实 entry / stop / TP / RR（入场 / 止损 / 止盈 / 盈亏比）。
- 不创建 close / reverse / buy / sell（平仓 / 反手 / 买入 / 卖出）动作。
- 不升级 ExecutionPlan readiness（执行计划可执行就绪）。
- 不接 order / execution / scheduler / automation / auto-trading（下单 / 执行 / 定时器 / 自动化 / 自动交易）。

P201 的核心结论是：P197-P200 已经完成 Watchlist Low-Frequency Scan / Opportunity Promote read-only audit / docs-only semantics（观察库低频扫描 / 机会提升只读审计 / 只改文档语义）闭环，项目总进度、安全地基进度和 Watchlist / Display Slots / Opportunity Promote（观察库 / 首页展示位 / 机会提升）语义进度可以小幅上调；但 real low-frequency scan（真实低频扫描）、runtime data source（运行时数据源）、MarketQuoteClient scan integration（行情客户端扫描接入）、scan scheduler（扫描定时器）、Opportunity Promote execution（机会提升执行）、Opportunity Push execution（机会推送执行）、真实 entry / stop / TP / RR（入场 / 止损 / 止盈 / 盈亏比）、ExecutionPlan Readiness（执行计划可执行就绪）、trading buttons（交易按钮）和 auto-trading（自动交易）仍未完成。

后续继续推进必须以 `docs/PROJECT_PROGRESS_INDEX.md` 为准。任何后续阶段如果想打开 SourceTrace（证据来源追踪）、BoundaryCandidate（边界候选交易计划）、ExecutionPlan（执行计划）、Dashboard（首页工作台）、Risk Action Guard（风险动作保护器）、Position Monitor（持仓监控）、Watchlist（观察库）、Opportunity Promote（机会提升）、Push（推送）或 auto-trading（自动交易）的新能力，都必须先对照本索引确认它属于“已完成”“正在推进”“暂停”“后期必须回来做”还是“禁止提前做”。
