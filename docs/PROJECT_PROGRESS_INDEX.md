# PROJECT_PROGRESS_INDEX

P176 是 Project Progress Index Refresh After BoundaryCandidate Display（BoundaryCandidate 展示后项目总进度索引刷新）。

本索引来自 P164 全局扫描后的持续维护。本轮只刷新 `docs/PROJECT_PROGRESS_INDEX.md`，并吸收 P172-P175 BoundaryCandidate（边界候选交易计划）Read-Only Candidate（只读候选）Display（展示）闭环结果。P176 不新增 Java，不新增测试，不改 `dashboard.html`，不接 API（接口），不生成真实 entry / stop / TP / RR（入场 / 止损 / 止盈 / 盈亏比），不生成 production VALID（生产环境有效候选状态），不接 auto-trading（自动交易）。

## 一、当前总进度结论

当前项目仍然不是“快完成自动交易”的状态。更准确的状态是：安全地基、只读复核、失败关闭、SourceTrace（证据来源追踪）只读展示和 BoundaryCandidate（边界候选交易计划）只读候选展示已经更完整；但 production candidate generation（生产候选交易计划生成）、production VALID（生产环境有效候选状态）、真实 entry / stop / TP / RR（入场 / 止损 / 止盈 / 盈亏比）、ExecutionPlan（执行计划）Readiness（可执行就绪）和自动交易仍然没有闭环。

P165-P170 已完成 SourceTrace read-only display（证据来源追踪只读展示）闭环。P172-P175 已完成 BoundaryCandidate read-only candidate display（边界候选只读候选展示）闭环。所以项目总进度可以小幅上调。但这个上调只代表“可看见、可复核、可解释”的只读展示层推进，不代表交易计划生成、生产 `VALID`（有效候选状态）或真实点位完成。

| 项目线 | 当前真实进度 |
|---|---:|
| 项目总进度 | 67%-72% |
| 安全地基进度 | 83%-89% |
| SourceTrace（证据来源追踪）进度 | 58%-66% |
| BoundaryCandidate（边界候选交易计划）进度 | 42%-52% |
| 真实生产接线进度 | 24%-32% |
| entry / stop / TP / RR（入场 / 止损 / 止盈 / 盈亏比）真实点位进度 | 10%-18% |
| dashboard（首页工作台）展示进度 | 68%-76% |
| 自动交易进度 | 0%-3% |

这些百分比按“能否安全进入真实生产链路”估算，不按文档数量估算。SourceTrace（证据来源追踪）和 BoundaryCandidate（边界候选交易计划）现在更容易被人工复核，但真实交易计划链路仍缺 source-owned runtime candidate generation（运行时证据来源候选生成）、numeric source ownership（数值来源归属）、production VALID（生产环境有效候选状态）和 Readiness（可执行就绪）闭环。

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
- Watchlist（观察库）/ Display Slots（首页展示位）已完成边界定义和 Dashboard（首页工作台）展示口径：Display Slots 是首页展示优先级，Watchlist Pool（观察库池）是推送候选边界，但低频扫描和机会提升还没有完成。证据线索：`docs/PHASE_DASHBOARD_LIVE_OBSERVATION_MVP_PLAN.md`、`docs/PHASE_HOME_P1_DASHBOARD_MAIN_WORKBENCH_CONSOLIDATION_RESULT.md`。
- BoundaryCandidate（边界候选交易计划）DTO / valid factory（有效候选工厂）/ service skeleton（服务骨架）已完成：`BoundaryCandidateDTO.valid(...)` 存在，`BoundaryCandidateServiceImpl` 存在，并有多组测试覆盖失败关闭、Risk Action Guard（风险动作保护器）阻断和 fixture（测试夹具）行为。注意：这不等于 production VALID（生产环境有效候选状态）已经可生成。
- RuntimeKlineContext（运行时 K 线上下文）/ BoundaryCandidateService（边界候选服务）相关已完成：`RuntimeKlineContextAssemblyServiceImpl` 能从 persisted OHLCV readiness（持久化 K 线就绪结果）组装只读上下文；`BoundaryCandidateServiceImpl` 能在输入完整时调用 valid factory（有效候选工厂），也能在缺证据或风险阻断时降级。
- Dashboard（首页工作台）已完成能力：`DashboardController` 有 `/dashboard`、`/api/dashboard/summary`、`/api/dashboard/detail`；detail（详情接口）已接 PlanBoundaryDisplay（计划边界展示）、ExecutionPlanDisplay（执行计划展示）、RiskActionGuardDisplay（风险动作保护展示）、PaperObservationDisplay（纸面观察展示）、SourceTrace（证据来源追踪）只读展示和 BoundaryCandidate（边界候选交易计划）只读候选状态表达。证据线索：`src/main/java/org/example/trademodel/controller/DashboardController.java`、`src/main/resources/templates/dashboard.html`、`src/main/java/org/example/trademodel/service/dashboard/DefaultPlanBoundaryDisplayAdapter.java`、`docs/PHASE_BACKEND_P170_SOURCETRACE_READ_ONLY_DISPLAY_CLOSURE.md`、`docs/PHASE_BACKEND_P175_BOUNDARY_CANDIDATE_READ_ONLY_DISPLAY_CLOSURE.md`。
- position sync（持仓同步）/ manual position（手动或模拟持仓）/ monitoring（监控）已完成基础能力：`PositionSyncService`、`PositionSyncScheduler`、`PositionProvider`、`SimulatedPositionProvider`、`BinancePositionProvider`、`MonitorController`、`MonitorService` 和 `tm_real_position` / `tm_monitor_alert` schema（数据库结构）存在。当前更像同步和展示基础，不是自动平仓或自动反手。

## 三、正在推进线路

BoundaryCandidate（边界候选交易计划）只读候选展示已完成。当前不应该继续把只读状态展示误推进成真实点位，也不能直接进入 production VALID（生产环境有效候选状态）。

下一阶段建议进入以下两类审计之一：

- ExecutionPlan Review-Only Plan Display Audit（执行计划只允许复核展示审计）：如果目标是个人可用最快路径，优先审计如何展示可复核计划摘要，同时保持 `REVIEW_ONLY`（只允许复核），不升级 Readiness（可执行就绪）。
- BoundaryCandidate Source Wiring Scope Audit（边界候选来源接线范围审计）：如果目标是严格生产候选，优先审计边界候选真实来源、数值来源、缺证据阻断和 production VALID（生产环境有效候选状态）边界。

如果继续按最短个人可用路径推进，建议先做 ExecutionPlan（执行计划）只读计划展示审计。它必须保持 `REVIEW_ONLY`（只允许复核），不能生成真实点位，不能打开可执行状态。

## 四、暂停线路

- BoundaryCandidate（边界候选交易计划）真实候选生成：仍暂停，原因是只读候选展示完成不等于真实来源接线和生产候选生成闭环。
- production VALID（生产环境有效候选状态）：仍暂停，原因是 P174 已对 `VALID`（有效候选状态）做安全降级，不能把它当成生产输出。
- entry / stop / TP / RR（入场 / 止损 / 止盈 / 盈亏比）真实点位：仍暂停，原因是 numeric source ownership（数值来源归属）没有真实闭环。
- ExecutionPlan readiness（执行计划可执行就绪）升级：仍暂停，原因是真实边界来源和风险保护还没有闭环。
- Risk Action Guard（风险动作保护器）生产接线：仍暂停，原因是目前主要是 display adapter（展示适配器）和 fail-closed（失败关闭）状态，不是真实风险动作保护链路。
- 持仓监控强反转 / 移动止损：仍暂停，原因是当前持仓同步不等于自动风控动作。
- Watchlist（观察库）低频扫描 / opportunity promote（机会提升）：仍暂停，原因是 Push / Recheck（推送 / 二次复核）已有基础，但低频机会扫描和提升规则未闭环。
- AI 多角色冲突处理落地：仍暂停，原因是已有 `AiConflictResolverService`，但多角色冲突处理还不是完整生产裁决链。
- 自动交易：仍暂停，原因是没有 order API（下单接口）、execution API（执行接口）、自动平仓或自动反手授权。

## 五、后期必须回来做的线路

- BoundaryCandidate（边界候选交易计划）真实来源接线：必须回来做，因为没有真实来源，`VALID`（有效候选状态）只能停留在 DTO / 测试或局部服务语义。
- ExecutionPlan（执行计划）只读计划输出：必须回来做，因为 Dashboard（首页工作台）当前可以展示状态，但真实可复核计划还不能从真实边界来源闭环产生。
- Risk Action Guard（风险动作保护器）：必须回来做，因为 stampede（踩踏）、强反转、移动止损、流动性恶化这些风险不能只靠展示适配器表达。
- 持仓监控：必须回来做，因为 position sync（持仓同步）只说明当前持仓可被记录，不说明系统能安全处理强反转、移动止损或退出策略。
- Watchlist Push（观察库推送）后续：必须回来做，因为 Push / Recheck（推送 / 二次复核）已有状态和调度，但低频扫描、机会提升和安全语义仍需统一。
- AI 冲突处理：必须回来做，因为 `AiConflictResolverService` 只是规则化冲突分层，还没有形成多角色证据仲裁和生产降级链。
- PROJECT progress index（项目总进度索引）后续维护：必须回来做，因为项目阶段很多，如果索引不更新，后续很容易把 helper（辅助类）、DTO（数据传输对象）或 display（展示）误当成生产完成。

## 六、容易误判为完成但其实没完成的线路

- SourceTrace（证据来源追踪）已完成 Dashboard（首页工作台）只读展示，不等于真实运行时候选生成完成。
- SourceTrace（证据来源追踪）已显示在页面，不等于可以交易。
- BoundaryCandidate（边界候选交易计划）已完成 read-only candidate display（只读候选展示），不等于真实候选生成完成。
- PlanBoundaryDisplay（计划边界展示）能显示候选状态，不等于可执行计划。
- `REVIEW_ONLY`（只允许复核）/ `INCOMPLETE`（证据不完整）/ `BLOCKED`（禁止推进）/ `WATCH_ONLY`（仅观察）显示出来，不等于方向信号。
- `VALID`（有效候选状态）被安全降级，不等于 production VALID（生产环境有效候选状态）可用。
- adapter（适配器）能展示状态，不等于真实 entry / stop / TP / RR（入场 / 止损 / 止盈 / 盈亏比）已可用。
- Dashboard（首页工作台）能看 BoundaryCandidate（边界候选交易计划）状态，不等于能下单。
- 页面显示 `blockingReasons`（禁止推进原因），不等于交易机会。
- `REVIEW_ONLY`（只允许复核）显示出来，不等于可执行。
- `INCOMPLETE`（证据不完整）/ `BLOCKED`（禁止推进）显示出来，不等于方向信号。
- Dashboard（首页工作台）能看 SourceTrace（证据来源追踪），不等于 entry / stop / TP / RR（入场 / 止损 / 止盈 / 盈亏比）已真实可用。
- SourceTrace（证据来源追踪）service wrapper（服务包装器）完成，不等于真实运行时生产接线完成。
- helper（辅助类）/ service（服务）完成，不等于 Controller（控制器）/ API（接口）动作能力完成。
- DTO（数据传输对象）字段完成，不等于真实数据来源完成。
- `INCOMPLETE`（证据不完整）/ `BLOCKED`（禁止推进）guard（保护）完成，不等于 `VALID`（有效候选状态）可以生成。
- BoundaryCandidate（边界候选交易计划）valid factory（有效候选工厂）完成，不等于生产 `VALID`（有效候选状态）可以生成。
- Dashboard（首页工作台）有页面，不等于可执行计划已真实可用。
- Watchlist Pool（观察库池）完成，不等于低频扫描和机会提升完成。
- Risk Action Guard（风险动作保护器）方案或 display adapter（展示适配器）完成，不等于生产动作保护已接线。

## 七、禁止提前做的线路

- 自动交易。
- 下单接口。
- 自动平仓。
- 自动反手。
- 真实 entry / stop / TP / RR（入场 / 止损 / 止盈 / 盈亏比）在 SourceTrace（证据来源追踪）/ BoundaryCandidate（边界候选交易计划）来源未闭环前不能做。
- `VALID`（有效候选状态）在来源链路未闭环前不能做。
- Readiness（可执行就绪）在真实边界来源未闭环前不能升级。
- Dashboard（首页工作台）可执行状态在 Readiness（可执行就绪）未闭环前不能打开。
- order（下单）/ execution（执行）/ scheduler（定时器）/ automation（自动化）/ auto-trading（自动交易）不能借 Push / Recheck（推送 / 二次复核）、position sync（持仓同步）或 read-only display（只读展示）名义提前进入。

## 八、推荐下一阶段顺序

### 路线 A：个人可用最快路径

1. ExecutionPlan Review-Only Plan Display Scope Audit（执行计划只允许复核展示范围审计）。
2. ExecutionPlan Review-Only Plan Display Authorization Gate（执行计划只允许复核展示授权门）。
3. ExecutionPlan Review-Only Plan Display Minimal Wiring（执行计划只允许复核展示最小接线）。
4. Risk Action Guard Display / Position Monitor Audit（风险动作保护和持仓监控审计）。

### 路线 B：继续严谨后端路径

1. BoundaryCandidate Source Wiring Scope Audit（边界候选来源接线范围审计）。
2. Numeric Source Ownership Audit（数值来源归属审计）。
3. ExecutionPlan Readiness Scope Audit（执行计划可执行就绪范围审计）。
4. Dashboard Read-Only Plan Display Scope Gate（首页只读计划展示范围门）。

推荐结论：

- 如果目标是个人可用，优先路线 A。
- 如果目标是严格生产候选，优先路线 B。
- 但无论哪条路线，都不能直接进入自动交易或真实点位。

## 九、模块进度表

| 模块 | 当前状态 | 完成度 | 证据 / 文件线索 | 下一步 |
|---|---|---:|---|---|
| Project Overall（项目总进度） | 安全地基、只读展示、SourceTrace（证据来源追踪）和 BoundaryCandidate（边界候选交易计划）只读展示闭环小幅推进；真实候选、真实点位、Readiness（可执行就绪）、自动交易未完成 | 67%-72% | `PROJECT_PROGRESS_INDEX.md`、`PHASE_BACKEND_P170_SOURCETRACE_READ_ONLY_DISPLAY_CLOSURE.md`、`PHASE_BACKEND_P175_BOUNDARY_CANDIDATE_READ_ONLY_DISPLAY_CLOSURE.md` | P176 后继续按本索引选择路线 A 或路线 B |
| SourceTrace（证据来源追踪） | 只读输出 + Dashboard（首页工作台）只读展示已完成；真实候选 / 真实点位 / Readiness（可执行就绪）未完成 | 58%-66% | `SourceTraceRuntimePopulationHelper.java`、`SourceTraceRuntimePopulationServiceImpl.java`、`DefaultDashboardSourceTraceDetailAdapter.java`、`dashboard.html`、`PHASE_BACKEND_P170*` | 转入 ExecutionPlan 只读计划展示审计或 BoundaryCandidate 来源审计 |
| BoundaryCandidate（边界候选交易计划） | DTO（数据对象）/ service skeleton（服务骨架）/ read-only candidate display（只读候选展示）已完成；生产候选 / 真实点位 / production VALID（生产环境有效候选状态）未完成 | 42%-52% | `BoundaryCandidateDTO.java`、`BoundaryCandidateServiceImpl.java`、`BoundaryCandidateServiceImplTest.java`、`DefaultPlanBoundaryDisplayAdapter.java`、`DefaultPlanBoundaryDisplayAdapterTest.java`、`PHASE_BACKEND_P175*` | 个人可用路线转 ExecutionPlan 只读计划展示审计；严谨路线做 source wiring audit（来源接线审计） |
| ExecutionPlan（执行计划） | advisory（建议性）输出和 display（展示）链路存在；Readiness（可执行就绪）不能升级 | 38%-48% | `PlanServiceImpl.java`、`ExecutionPlanVO.java`、`DefaultExecutionPlanDisplayAdapter.java` | 只读计划输出审计，保持 `REVIEW_ONLY`（只允许复核） |
| Dashboard（首页工作台） | 页面、summary/detail API、display objects（展示对象）已存在；SourceTrace（证据来源追踪）+ BoundaryCandidate（边界候选交易计划）只读展示增强已完成；可执行状态未打开 | 68%-76% | `DashboardController.java`、`dashboard.html`、`DashboardControllerTest.java`、`DefaultPlanBoundaryDisplayAdapter.java`、`PHASE_BACKEND_P168*`、`PHASE_BACKEND_P170*`、`PHASE_BACKEND_P175*` | 只允许继续做只读计划展示范围门，不打开可执行状态 |
| Watchlist / Push（观察库 / 推送） | Push snapshot（推送快照）、Recheck（二次复核）、scheduler（定时器）和 ops overview（运维总览）存在；低频扫描和机会提升未闭环 | 45%-55% | `PushRecheckServiceImpl.java`、`PushRecheckScheduler.java`、`PushSnapshotService.java`、`PHASE_P11A_PUSH_RECHECK_NAMING_VERIFICATION.md` | 先统一 review-only（只允许复核）命名，再做机会提升审计 |
| Position Monitor（持仓监控） | position sync（持仓同步）、provider（持仓提供器）、schema（数据库结构）和 dashboard 计数存在；强反转 / 移动止损未实现 | 45%-55% | `PositionSyncService.java`、`PositionSyncScheduler.java`、`RealPositionMapper.java`、`tm_real_position` | 做持仓风险只读监控审计，不做自动动作 |
| Risk Action Guard（风险动作保护器） | display adapter（展示适配器）和 fail-closed（失败关闭）动作开关存在；生产风控来源未接 | 35%-45% | `DefaultRiskActionGuardDisplayAdapter.java`、`DefaultRiskActionGuardDisplayAdapterTest.java`、`PHASE_RISK_ACTION_GUARD_DISPLAY_*` | 审计真实风险来源，继续禁止动作指令 |
| AI multi-agent（AI 多角色） | `AiConflictResolverService` 已有冲突分层；多角色生产仲裁链未落地 | 25%-35% | `AiConflictResolverService.java`、`AiConflictResolverServiceImpl.java`、`DecisionEngineServiceTest.java` | 定义多角色输入、冲突降级和人工复核边界 |
| entry / stop / TP / RR（入场 / 止损 / 止盈 / 盈亏比） | DTO 和 fixture（测试夹具）存在；真实数值来源未闭环，不因 BoundaryCandidate 只读展示完成而上调 | 10%-18% | `BoundaryEntryDTO.java`、`BoundaryStopDTO.java`、`BoundaryTakeProfitLevelDTO.java`、`StopTpRrSourceOwnedCandidateFixtureHelper.java` | 先做 numeric source ownership（数值来源归属）审计 |
| Auto-trading（自动交易） | 未实现；只有明确禁止和无交易指令保护 | 0%-3% | `StaticNoTradeInstructionGuardTest.java`、多份 P140-P176 禁止清单 | 继续禁止，直到真实点位、Readiness（可执行就绪）、风控和授权全部闭环 |

## 十、P176 结论

P176 只刷新 `docs/PROJECT_PROGRESS_INDEX.md`。

P176 不写代码。

P176 不新增测试。

P176 不改 `dashboard.html`。

P176 不接 API（接口）。

P176 不生成交易点位。

P176 不生成 production VALID（生产环境有效候选状态）。

P176 不接自动交易。

P176 的核心结论是：P172-P175 已经完成 BoundaryCandidate（边界候选交易计划）只读候选展示闭环，项目总进度、BoundaryCandidate（边界候选交易计划）进度和 Dashboard（首页工作台）展示进度可以小幅上调；但 production candidate generation（生产候选交易计划生成）、production VALID（生产环境有效候选状态）、真实 entry / stop / TP / RR（入场 / 止损 / 止盈 / 盈亏比）、Readiness（可执行就绪）和自动交易仍未完成。

后续继续推进必须以 `docs/PROJECT_PROGRESS_INDEX.md` 为准。任何后续阶段如果想打开 SourceTrace（证据来源追踪）、BoundaryCandidate（边界候选交易计划）、ExecutionPlan（执行计划）、Dashboard（首页工作台）、Risk Action Guard（风险动作保护器）、Watchlist（观察库）或 auto-trading（自动交易）的新能力，都必须先对照本索引确认它属于“已完成”“正在推进”“暂停”“后期必须回来做”还是“禁止提前做”。
