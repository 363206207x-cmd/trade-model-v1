# PROJECT_PROGRESS_INDEX

P164 是 Project Progress Global Index（项目总进度全局索引）。

本索引来自一次只读全局扫描，不是普通阶段总结。扫描范围包括 `docs/`、`src/main/java/`、`src/test/java/`、`src/main/resources/`、`schema/config/dashboard` 相关文件和最近 Git 记录。P164 只写这个索引，不新增 Java，不新增测试，不改 Dashboard（首页工作台），不接 API（接口），不生成真实 entry / stop / TP / RR（入场 / 止损 / 止盈 / 盈亏比），不接 auto-trading（自动交易）。

## 一、当前总进度结论

当前项目不是“快完成自动交易”的状态。更准确的状态是：安全地基、只读复核、失败关闭和 dashboard（首页工作台）展示骨架已经比较完整；真实生产接线、真实交易点位、ExecutionPlan（执行计划）可执行就绪和自动交易仍然没有闭环。

| 项目线 | 当前真实进度 |
|---|---:|
| 项目总进度 | 64%-69% |
| 安全地基进度 | 80%-86% |
| SourceTrace（证据来源追踪）进度 | 48%-56% |
| 真实生产接线进度 | 18%-25% |
| entry / stop / TP / RR（入场 / 止损 / 止盈 / 盈亏比）真实点位进度 | 10%-18% |
| dashboard（首页工作台）展示进度 | 62%-70% |
| 自动交易进度 | 0%-3% |

这些百分比按“能否安全进入真实生产链路”估算，不按文档数量估算。项目已经有很多 guard（保护）和 display（展示）工作，但真实交易计划链路仍缺 source-owned runtime candidate generation（运行时证据来源候选生成）和完整 SourceTrace（证据来源追踪）闭环。

## 二、已完成线路

- P140-P163 SourceTrace（证据来源追踪）/ Production Wiring Preparation（生产接线准备）当前完成情况：已经完成范围门、缺口审计、输入契约、设计矩阵、测试计划、INCOMPLETE（证据不完整）guard、BLOCKED（禁止推进）guard、SourceTrace runtime population（运行时证据来源填充）helper（辅助类）、SourceTrace service wrapper（服务包装器）和收口文档。证据线索：`docs/PHASE_BACKEND_P140_PRODUCTION_WIRING_PREPARATION_SCOPE_GATE.md` 到 `docs/PHASE_BACKEND_P163_SOURCETRACE_SERVICE_WIRING_CLOSURE.md`。
- P160-P163 SourceTrace Service Wiring Pack（证据来源追踪服务层接线包）已完成：`SourceTraceRuntimePopulationService` 和 `SourceTraceRuntimePopulationServiceImpl` 已存在，service wrapper（服务包装器）只调用 `SourceTraceRuntimePopulationHelper.populate(...)`。证据线索：`src/main/java/org/example/trademodel/service/planboundary/SourceTraceRuntimePopulationService.java`、`src/main/java/org/example/trademodel/service/planboundary/SourceTraceRuntimePopulationServiceImpl.java`、`src/test/java/org/example/trademodel/service/planboundary/SourceTraceRuntimePopulationServiceTest.java`。
- Watchlist（观察库）/ Display Slots（首页展示位）已完成边界定义和 dashboard（首页工作台）展示口径：Display Slots 是首页展示优先级，Watchlist Pool（观察库池）是推送候选边界，但低频扫描和机会提升还没有完成。证据线索：`docs/PHASE_DASHBOARD_LIVE_OBSERVATION_MVP_PLAN.md`、`docs/PHASE_HOME_P1_DASHBOARD_MAIN_WORKBENCH_CONSOLIDATION_RESULT.md`。
- BoundaryCandidate（边界候选交易计划）DTO / valid factory（有效候选工厂）/ service skeleton（服务骨架）已完成：`BoundaryCandidateDTO.valid(...)` 存在，`BoundaryCandidateServiceImpl` 存在，并有多组测试覆盖失败关闭、Risk Action Guard（风险动作保护器）阻断和 fixture（测试夹具）行为。注意：这不等于生产 VALID（有效候选状态）已经可生成。
- RuntimeKlineContext（运行时 K 线上下文）/ BoundaryCandidateService（边界候选服务）相关已完成：`RuntimeKlineContextAssemblyServiceImpl` 能从 persisted OHLCV readiness（持久化 K 线就绪结果）组装只读上下文；`BoundaryCandidateServiceImpl` 能在输入完整时调用 valid factory（有效候选工厂），也能在缺证据或风险阻断时降级。
- dashboard（首页工作台）已完成能力：`DashboardController` 有 `/dashboard`、`/api/dashboard/summary`、`/api/dashboard/detail`；detail（详情接口）已接 PlanBoundaryDisplay（计划边界展示）、ExecutionPlanDisplay（执行计划展示）、RiskActionGuardDisplay（风险动作保护展示）、PaperObservationDisplay（纸面观察展示）这些 fail-closed（失败关闭）展示对象。证据线索：`src/main/java/org/example/trademodel/controller/DashboardController.java`、`src/main/resources/templates/dashboard.html`、`docs/PHASE_DASHBOARD_CONTROLLER_TEST_RESULT.md`。
- position sync（持仓同步）/ manual position（手动或模拟持仓）/ monitoring（监控）已完成基础能力：`PositionSyncService`、`PositionSyncScheduler`、`PositionProvider`、`SimulatedPositionProvider`、`BinancePositionProvider`、`MonitorController`、`MonitorService` 和 `tm_real_position` / `tm_monitor_alert` schema（数据库结构）存在。当前更像同步和展示基础，不是自动平仓或自动反手。

## 三、正在推进线路

当前正在推进的主线应该只有一条：SourceTrace（证据来源追踪）从 helper（辅助类）和 service wrapper（服务包装器）走向 read model（只读输出模型）审计。

当前状态是：SourceTrace（证据来源追踪）已完成最小 service wrapper（服务包装器），但还未进入 controller（控制器）/ API（接口）/ dashboard（首页工作台）/ runtime data（运行时数据）接线。

下一阶段必须按 P164 之后的推荐顺序推进，不要继续乱开功能。推荐先做 SourceTrace read model（证据来源追踪只读输出模型）的范围审计和授权门，再考虑最小只读接线。不能从 P164 直接跳到真实候选生成、真实点位、dashboard 可执行状态或自动交易。

## 四、暂停线路

- BoundaryCandidate（边界候选交易计划）真实候选生成：暂停，原因是 SourceTrace（证据来源追踪）来源链路没有闭环。
- entry / stop / TP / RR（入场 / 止损 / 止盈 / 盈亏比）真实点位：暂停，原因是 numeric source ownership（数值来源归属）没有真实闭环。
- ExecutionPlan readiness（执行计划可执行就绪）升级：暂停，原因是真实边界来源和风险保护还没有闭环。
- Dashboard SourceTrace（首页证据来源追踪）展示：暂停，原因是 SourceTrace read model（证据来源追踪只读输出模型）范围未审计。
- Risk Action Guard（风险动作保护器）生产接线：暂停，原因是目前主要是 display adapter（展示适配器）和 fail-closed（失败关闭）状态，不是真实风险动作保护链路。
- 持仓监控强反转 / 移动止损：暂停，原因是当前持仓同步不等于自动风控动作。
- Watchlist（观察库）低频扫描 / opportunity promote（机会提升）：暂停，原因是 Push / Recheck（推送 / 二次复核）已有基础，但低频机会扫描和提升规则未闭环。
- AI 多角色冲突处理落地：暂停，原因是已有 `AiConflictResolverService`，但多角色冲突处理还不是完整生产裁决链。
- 自动交易：暂停，原因是没有 order API（下单接口）、execution API（执行接口）、自动平仓或自动反手授权。

## 五、后期必须回来做的线路

- SourceTrace（证据来源追踪）接 controller（控制器）/ read model（只读输出模型）/ dashboard（首页工作台）展示：必须回来做，因为后续 BoundaryCandidate（边界候选交易计划）、ExecutionPlan（执行计划）和 dashboard 状态都依赖证据来源可追踪。
- BoundaryCandidate（边界候选交易计划）真实来源接线：必须回来做，因为没有真实来源，VALID（有效候选状态）只能停留在 DTO / 测试或局部服务语义。
- ExecutionPlan（执行计划）只读计划输出：必须回来做，因为 dashboard 当前可以展示状态，但真实可复核计划还不能从真实边界来源闭环产生。
- Risk Action Guard（风险动作保护器）：必须回来做，因为 stampede（踩踏）、强反转、移动止损、流动性恶化这些风险不能只靠展示适配器表达。
- 持仓监控：必须回来做，因为 position sync（持仓同步）只说明当前持仓可被记录，不说明系统能安全处理强反转、移动止损或退出策略。
- Watchlist Push（观察库推送）后续：必须回来做，因为 Push / Recheck（推送 / 二次复核）已有状态和调度，但低频扫描、机会提升和安全语义仍需统一。
- AI 冲突处理：必须回来做，因为 `AiConflictResolverService` 只是规则化冲突分层，还没有形成多角色证据仲裁和生产降级链。
- PROJECT progress index（项目总进度索引）后续维护：必须回来做，因为项目阶段很多，如果索引不更新，后续很容易把 helper（辅助类）、DTO（数据传输对象）或 display（展示）误当成生产完成。

## 六、容易误判为完成但其实没完成的线路

- SourceTrace（证据来源追踪）service wrapper（服务包装器）完成，不等于真实运行时接线完成。
- helper（辅助类）/ service（服务）完成，不等于 controller（控制器）/ API（接口）完成。
- DTO（数据传输对象）字段完成，不等于真实数据来源完成。
- INCOMPLETE（证据不完整）/ BLOCKED（禁止推进）guard（保护）完成，不等于 VALID（有效候选状态）可以生成。
- BoundaryCandidate（边界候选交易计划）valid factory（有效候选工厂）完成，不等于生产 VALID（有效候选状态）可以生成。
- dashboard（首页工作台）有页面，不等于可执行计划已真实可用。
- Watchlist Pool（观察库池）完成，不等于低频扫描和机会提升完成。
- Risk Action Guard（风险动作保护器）方案或 display adapter（展示适配器）完成，不等于生产动作保护已接线。

## 七、禁止提前做的线路

- 自动交易。
- 下单接口。
- 自动平仓。
- 自动反手。
- 真实 entry / stop / TP / RR（入场 / 止损 / 止盈 / 盈亏比）在 SourceTrace（证据来源追踪）/ BoundaryCandidate（边界候选交易计划）来源未闭环前不能做。
- VALID（有效候选状态）在来源链路未闭环前不能做。
- readiness（可执行就绪）在真实边界来源未闭环前不能升级。
- dashboard（首页工作台）可执行状态在 readiness（可执行就绪）未闭环前不能打开。
- order（下单）/ execution（执行）/ scheduler（定时器）/ automation（自动化）/ auto-trading（自动交易）不能借 Push / Recheck（推送 / 二次复核）或 position sync（持仓同步）名义提前进入。

## 八、推荐下一阶段顺序

1. P165：SourceTrace Read Model / Controller Scope Audit（证据来源追踪只读输出范围审计）。
2. P166：SourceTrace Read Model Authorization Gate（证据来源追踪只读输出授权门）。
3. P167：SourceTrace Read Model Minimal Wiring（证据来源追踪只读输出最小接线）。
4. P168：SourceTrace Dashboard Display Scope Gate（首页证据来源追踪展示范围门）。

更保守的顺序也可以是：

1. SourceTrace read-model audit（证据来源追踪只读输出模型审计）。
2. SourceTrace read-model minimal wiring（证据来源追踪只读输出最小接线）。
3. Dashboard read-only display（首页只读展示）。
4. BoundaryCandidate source wiring audit（边界候选来源接线审计）。

无论选择哪套顺序，都不能在 P165-P168 期间打开真实点位、VALID（有效候选状态）、readiness（可执行就绪）或自动交易。

## 九、模块进度表

| 模块 | 当前状态 | 完成度 | 证据 / 文件线索 | 下一步 |
|---|---|---:|---|---|
| SourceTrace（证据来源追踪） | DTO（数据传输对象）、helper（辅助类）、service wrapper（服务包装器）已完成；未接 read model/controller/API/dashboard/runtime data | 48%-56% | `SourceTraceRuntimePopulationHelper.java`、`SourceTraceRuntimePopulationServiceImpl.java`、`PHASE_BACKEND_P159*`、`PHASE_BACKEND_P163*` | P165 先做 read model / controller scope audit（只读输出范围审计） |
| BoundaryCandidate（边界候选交易计划） | DTO、valid factory（有效候选工厂）、service skeleton（服务骨架）和测试存在；生产 VALID（有效候选状态）仍未授权 | 35%-45% | `BoundaryCandidateDTO.java`、`BoundaryCandidateServiceImpl.java`、`BoundaryCandidateServiceImplTest.java` | 等 SourceTrace 来源闭环后再做真实来源接线审计 |
| ExecutionPlan（执行计划） | advisory（建议性）输出和 display（展示）链路存在；readiness（可执行就绪）不能升级 | 38%-48% | `PlanServiceImpl.java`、`ExecutionPlanVO.java`、`DefaultExecutionPlanDisplayAdapter.java` | 只读计划输出审计，保持 REVIEW_ONLY（只允许复核） |
| Dashboard（首页工作台） | 页面、summary/detail API、display objects（展示对象）已存在；SourceTrace 真实展示和可执行状态未打开 | 62%-70% | `DashboardController.java`、`dashboard.html`、`DashboardControllerTest.java` | 等 SourceTrace read model 授权后做只读展示范围门 |
| Watchlist / Push（观察库 / 推送） | Push snapshot（推送快照）、Recheck（二次复核）、scheduler（定时器）和 ops overview（运维总览）存在；低频扫描和机会提升未闭环 | 45%-55% | `PushRecheckServiceImpl.java`、`PushRecheckScheduler.java`、`PushSnapshotService.java`、`PHASE_P11A_PUSH_RECHECK_NAMING_VERIFICATION.md` | 先统一 review-only（只允许复核）命名，再做机会提升审计 |
| Position Monitor（持仓监控） | position sync（持仓同步）、provider（持仓提供器）、schema（数据库结构）和 dashboard 计数存在；强反转 / 移动止损未实现 | 45%-55% | `PositionSyncService.java`、`PositionSyncScheduler.java`、`RealPositionMapper.java`、`tm_real_position` | 做持仓风险只读监控审计，不做自动动作 |
| Risk Action Guard（风险动作保护器） | display adapter（展示适配器）和 fail-closed（失败关闭）动作开关存在；生产风控来源未接 | 35%-45% | `DefaultRiskActionGuardDisplayAdapter.java`、`DefaultRiskActionGuardDisplayAdapterTest.java`、`PHASE_RISK_ACTION_GUARD_DISPLAY_*` | 审计真实风险来源，继续禁止动作指令 |
| AI multi-agent（AI 多角色） | `AiConflictResolverService` 已有冲突分层；多角色生产仲裁链未落地 | 25%-35% | `AiConflictResolverService.java`、`AiConflictResolverServiceImpl.java`、`DecisionEngineServiceTest.java` | 定义多角色输入、冲突降级和人工复核边界 |
| entry / stop / TP / RR（入场 / 止损 / 止盈 / 盈亏比） | DTO 和 fixture（测试夹具）存在；真实数值来源未闭环 | 10%-18% | `BoundaryEntryDTO.java`、`BoundaryStopDTO.java`、`BoundaryTakeProfitLevelDTO.java`、`StopTpRrSourceOwnedCandidateFixtureHelper.java` | 先做 numeric source ownership（数值来源归属）审计 |
| Auto-trading（自动交易） | 未实现；只有明确禁止和无交易指令保护 | 0%-3% | `StaticNoTradeInstructionGuardTest.java`、多份 P140-P163 禁止清单 | 继续禁止，直到真实点位、readiness、风控和授权全部闭环 |

## 十、P164 结论

P164 只完成全局索引文档。

P164 不新增 Java。

P164 不新增测试。

P164 不改 dashboard（首页工作台）。

P164 不接 API（接口）。

P164 不生成交易点位。

P164 不接自动交易。

后续继续推进必须以 `docs/PROJECT_PROGRESS_INDEX.md` 为总索引。任何后续阶段如果想打开 SourceTrace（证据来源追踪）、BoundaryCandidate（边界候选交易计划）、ExecutionPlan（执行计划）、dashboard（首页工作台）、Risk Action Guard（风险动作保护器）、Watchlist（观察库）或 auto-trading（自动交易）的新能力，都必须先对照本索引确认它属于“已完成”“正在推进”“暂停”“后期必须回来做”还是“禁止提前做”。
