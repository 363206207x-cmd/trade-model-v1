# BACKEND-P165 SourceTrace Read Model Controller Scope Audit

Issue: #444
PR: #445
Branch: p165
Base: a534705 BACKEND-P164 Project Progress Global Index (#443)

本文件是 P165 的唯一交付物。P165 只做 SourceTrace（证据来源追踪）Read Model（只读输出模型）Controller（控制器）范围审计，不写 Java，不新增测试，不接 Controller（控制器）/ API（接口）/ Dashboard（首页工作台），不生成真实 entry（入场）/ stop（止损）/ TP（止盈）/ RR（盈亏比）。

## 一、这一步是干嘛的

P165 是 SourceTrace（证据来源追踪）Read Model（只读输出模型）/ Controller（控制器）Scope Audit（范围审计）。

P165 是 P164 全局索引之后的第一步。P164 已经把项目总进度、禁止提前推进的线路、下一阶段顺序固定下来，P165 只能沿着这个总索引审计 SourceTrace（证据来源追踪）未来是否可以进入只读输出路径。

本轮明确不做以下事情：

- 不写 Java。
- 不新增测试。
- 不接 Controller（控制器）/ API（接口）/ Dashboard（首页工作台）。
- 不修改 `dashboard.html`。
- 不新增 endpoint（接口入口）。
- 不新增 schema（数据库结构）/ config（配置）/ mapper（映射器）/ service（服务）。
- 不读取 runtime data（运行时数据）/ live market data（实时行情）/ external data（外部数据）。
- 不生成真实 entry（入场）/ stop（止损）/ TP（止盈）/ RR（盈亏比）。
- 不调用 `BoundaryCandidateDTO.valid(...)`。
- 不升级 ExecutionPlan（执行计划）Readiness（可执行就绪）。
- 不接 order（下单）/ execution（执行）/ scheduler（定时器）/ automation（自动化）/ auto-trading（自动交易）。

P165 只回答一个问题：SourceTrace（证据来源追踪）在未来是否可以进入 read-only（只读）输出路径，以及最安全的下一步应该如何限定。

## 二、P164 的总索引结论

P164 的总索引文件是 `docs/PROJECT_PROGRESS_INDEX.md`。P165 以它为准，不重新扩大范围。

P164 已经确认：

- SourceTrace（证据来源追踪）已经完成 helper（辅助器）和 service wrapper（服务包装层）。
- SourceTrace（证据来源追踪）还没有进入 Controller（控制器）/ API（接口）/ Dashboard（首页工作台）/ runtime data（运行时数据）接线。
- P164 推荐下一阶段先做 SourceTrace（证据来源追踪）read-model audit（只读输出模型审计）。
- P164 禁止提前做 VALID（有效候选状态）、Readiness（可执行就绪）、真实 entry（入场）/ stop（止损）/ TP（止盈）/ RR（盈亏比）、Dashboard（首页工作台）可执行状态、自动交易。

因此 P165 的结论不能写成“SourceTrace（证据来源追踪）已经可用”。更准确的说法是：SourceTrace（证据来源追踪）已有最小服务包装能力，但仍处在只读、人工复核、缺证据时失败关闭的阶段。

## 三、当前 SourceTrace 能力

只读扫描确认，当前 SourceTrace（证据来源追踪）相关能力主要来自以下文件：

- `src/main/java/org/example/trademodel/dto/planboundary/SourceTraceDTO.java`
- `src/main/java/org/example/trademodel/dto/planboundary/SourceTraceRuntimePopulationHelper.java`
- `src/main/java/org/example/trademodel/service/planboundary/SourceTraceRuntimePopulationService.java`
- `src/main/java/org/example/trademodel/service/planboundary/SourceTraceRuntimePopulationServiceImpl.java`
- `src/test/java/org/example/trademodel/service/planboundary/SourceTraceRuntimePopulationServiceTest.java`

当前已有 `SourceTraceRuntimePopulationHelper`（证据来源追踪运行时填充辅助器）。它负责把输入快照和候选结果整理成 `SourceTraceDTO`（证据来源追踪数据对象）。

当前已有 `SourceTraceRuntimePopulationService`（证据来源追踪运行时填充服务）和 `SourceTraceRuntimePopulationServiceImpl`（证据来源追踪运行时填充服务实现）。这个 service wrapper（服务包装层）只暴露一个最小方法：

- 输入：`MarketReadOnlyEvidenceSnapshotDTO`（市场只读证据快照）和 `MarketReadOnlyCandidateResultDTO`（市场只读候选结果）。
- 输出：`SourceTraceDTO`（证据来源追踪数据对象）。
- 实现：只委托 `SourceTraceRuntimePopulationHelper`（证据来源追踪运行时填充辅助器）。

这说明当前能力是“把已经给定的只读证据整理成 SourceTrace（证据来源追踪）结果”，不是“从生产环境生成真实候选计划”。

当前 SourceTrace（证据来源追踪）状态必须继续保持：

- `REVIEW_ONLY`（只允许复核）：输出只能用于人工复核，不能变成交易指令。
- `INCOMPLETE`（证据不完整）：缺少必要证据时必须保持不完整。
- `BLOCKED`（禁止推进）：存在冲突、不安全、无法确认来源时必须阻断。
- 不能生成 `VALID`（有效候选状态）。
- 不能生成真实 entry（入场）/ stop（止损）/ TP（止盈）/ RR（盈亏比）。
- 不能升级 ExecutionPlan（执行计划）Readiness（可执行就绪）。

补充审计发现：本次扫描没有发现独立的 `docs/PHASE_BACKEND_P162*` 文档；P162 的实际完成情况由 `docs/PHASE_BACKEND_P163_SOURCETRACE_SERVICE_WIRING_CLOSURE.md` 收口说明。P163 已确认 P162 只新增 service wrapper（服务包装层）和测试，未进入 Controller（控制器）/ API（接口）/ Dashboard（首页工作台）。

## 四、当前 dashboard / controller / read model 现状

只读扫描确认，当前 Dashboard（首页工作台）相关路径已经存在，但不能因此判断 SourceTrace（证据来源追踪）已经完整接线。

### 1. DashboardController（首页控制器）

`src/main/java/org/example/trademodel/controller/DashboardController.java` 已有以下入口：

- `/dashboard`：返回页面。
- `/api/dashboard/refresh`：旧刷新接口，返回 `DashboardSummaryResponseVO`（首页汇总响应对象）。
- `/api/dashboard/summary`：summary（汇总）接口，返回 `DashboardSummaryResponseVO`（首页汇总响应对象）。
- `/api/dashboard/detail`：detail（详情）接口，返回 `DashboardDetailResponseVO`（首页详情响应对象）。

审计结论：

- summary（汇总）接口目前主要承载系统状态、持仓数量、健康状态、告警和决策列表。
- summary（汇总）接口没有发现独立 SourceTrace（证据来源追踪）字段。
- detail（详情）接口已经有 SourceTrace（证据来源追踪）相关字段承载路径。

### 2. DashboardDetailResponseVO（首页详情响应对象）

`src/main/java/org/example/trademodel/vo/DashboardDetailResponseVO.java` 已有以下只读展示对象：

- `PlanBoundaryDisplayVO`（计划边界展示对象）。
- `ExecutionPlanDisplayVO`（执行计划展示对象）。
- `RiskActionGuardDisplayVO`（风险动作保护展示对象）。
- `PaperObservationDisplayVO`（纸面观察展示对象）。
- `SourceTraceDTO`（证据来源追踪数据对象）。
- `RuntimeKlineContextDTO`（运行时 K 线上下文数据对象）。
- `DerivativesRiskContextDTO`（衍生品风险上下文数据对象）。

审计结论：

- detail（详情）响应已经有可承载 SourceTrace（证据来源追踪）的字段。
- 这个字段目前承载的是只读诊断和缺失原因，不代表可执行计划完成。
- 如果未来继续接线，detail（详情）路径比 summary（汇总）路径更安全。

### 3. Dashboard SourceTrace adapter（首页证据来源追踪适配器）

只读扫描确认存在：

- `src/main/java/org/example/trademodel/service/dashboard/DashboardSourceTraceDetailAdapter.java`
- `src/main/java/org/example/trademodel/service/dashboard/DefaultDashboardSourceTraceDetailAdapter.java`

这些 adapter（适配器）用于生成 Dashboard（首页工作台）detail（详情）里的 SourceTrace（证据来源追踪）只读诊断对象。它们的安全语义是：

- 不获取外部数据。
- 不伪造边界价格。
- 不输出交易动作。
- 缺少 runtime kline（运行时 K 线）、latest price（最新价格）、entry（入场）、stop（止损）、TP（止盈）、RR（盈亏比）等证据时保持 `INCOMPLETE`（证据不完整）。

审计结论：

- 当前已经有 Dashboard（首页工作台）detail（详情）级别的 SourceTrace（证据来源追踪）展示承载对象。
- 当前 `SourceTraceRuntimePopulationService`（证据来源追踪运行时填充服务）尚未进入 DashboardController（首页控制器）调用链。
- P165 不允许把它接进去。

### 4. dashboard.html（首页页面）

`src/main/resources/templates/dashboard.html` 已有 SourceTrace（证据来源追踪）相关只读展示片段，读取 `detail.sourceTrace`、`detail.runtimeKlineContext`、`detail.derivativesRiskContext` 等字段。

审计结论：

- 页面已经有展示入口，但页面存在不等于 SourceTrace（证据来源追踪）运行时接线完成。
- P165 不允许修改 `dashboard.html`。
- P167 即便实现只读输出，也不应该先改 `dashboard.html`。

### 5. 当前最安全路径

当前最安全路径是 detail read model（详情只读模型），不是 summary（汇总），也不是直接改 Dashboard（首页工作台）大 UI。

原因：

- detail（详情）已经有 `DashboardDetailResponseVO`（首页详情响应对象）承载 SourceTrace（证据来源追踪）。
- detail（详情）天然是单标的、低冲击、便于人工复核的路径。
- summary（汇总）会影响首页列表和状态感知，过早放入 SourceTrace（证据来源追踪）容易被误读成可执行状态。
- dashboard.html（首页页面）已有读取逻辑，继续改页面会扩大 P167 风险面。

如果现有 `DashboardDetailResponseVO`（首页详情响应对象）和 adapter（适配器）足够，未来应优先授权最小接线。如果不够，未来应先新增 `SourceTraceReadModel`（证据来源追踪只读输出模型）或 `SourceTraceDisplayDTO`（证据来源追踪展示数据对象），而不是直接改页面。

## 五、是否允许未来进入只读输出

P165 的保守结论是：允许未来继续评估 SourceTrace（证据来源追踪）进入只读输出路径，但 P165 本身不允许直接写代码。

允许的未来方向：

- P166 可以做 SourceTrace（证据来源追踪）Read Model（只读输出模型）Authorization Gate（授权门）。
- P166 必须明确 P167 最多允许改哪些文件。
- P167 如果实现，也只能做 read-only（只读）输出。
- P167 最多允许把已有 SourceTrace（证据来源追踪）只读结果进入安全的 detail read model（详情只读模型）。

P167 必须继续禁止：

- 不能改 `dashboard.html`。
- 不能打开 Dashboard（首页工作台）可执行状态。
- 不能生成 entry（入场）/ stop（止损）/ TP（止盈）/ RR（盈亏比）。
- 不能映射 `VALID`（有效候选状态）。
- 不能升级 Readiness（可执行就绪）。
- 不能接自动交易。

## 六、未来 P166/P167 最安全方向

P166/P167 的最安全方向如下：

1. 优先 detail read model（详情只读模型），不要先改首页大 UI。
2. 优先新增或扩展一个只读 display（展示对象）/ VO（视图对象），不要直接改 `dashboard.html`。
3. 如果现有 `DashboardDetailResponseVO`（首页详情响应对象）和 `DashboardSourceTraceDetailAdapter`（首页证据来源追踪详情适配器）足够，P166 可以授权 P167 做最小接线。
4. 如果没有合适对象，先定义 `SourceTraceReadModel`（证据来源追踪只读输出模型）或 `SourceTraceDisplayDTO`（证据来源追踪展示数据对象）。
5. 继续保持 `REVIEW_ONLY`（只允许复核）。
6. 继续保持缺证据为 `INCOMPLETE`（证据不完整）。
7. 继续保持冲突或不安全为 `BLOCKED`（禁止推进）。
8. 不映射 `VALID`（有效候选状态）。
9. 不映射 Readiness（可执行就绪）。
10. 不映射 entry（入场）/ stop（止损）/ TP（止盈）/ RR（盈亏比）。

推荐的 P167 接线边界应接近“把已有只读 SourceTrace（证据来源追踪）结果放进 detail read model（详情只读模型）”，而不是“让 Controller（控制器）生成候选交易计划”。

## 七、仍然禁止的路径

以下路径在 P165 之后仍然禁止，不能被 P166/P167 偷偷放开：

- production candidate generation（生产候选交易计划生成）。
- source-owned runtime candidate generation（运行时证据来源候选生成）。
- real entry / stop / TP / RR value generation（真实入场 / 止损 / 止盈 / 盈亏比生成）。
- production VALID mapping（生产环境映射为有效候选）。
- `BoundaryCandidateDTO.valid(...)` production calls（生产环境调用 valid 工厂）。
- ExecutionPlan（执行计划）Readiness（可执行就绪）upgrade（升级）。
- Dashboard（首页工作台）readiness mutation（可执行状态变更）。
- `dashboard.html` changes（页面改动）。
- Controller（控制器）/ endpoint（接口入口）/ API（接口）write/action wiring（写入或动作接线）。
- schema（数据库结构）/ config（配置）/ mapper（映射器）changes（改动）。
- runtime data reads（读取运行时数据）。
- live market data reads（读取实时行情）。
- external data integration（接外部数据）。
- WebClient（网络请求客户端）/ RestTemplate（网络请求模板）。
- order API（下单接口）。
- execution API（执行接口）。
- scheduler（定时器）/ automation（自动化）/ auto-trading（自动交易）。

这些禁线的核心原因只有一个：SourceTrace（证据来源追踪）现在只能说明“证据是否可复核”，不能说明“交易是否可以执行”。

## 八、推荐下一步

推荐下一步：

P166：SourceTrace Read Model Authorization Gate（证据来源追踪只读输出授权门）。

P166 仍然不写代码。P166 只定义 P167 最小只读输出接线允许改哪些文件，以及这些文件必须遵守哪些失败关闭规则。

P166 必须继续禁止：

- `VALID`（有效候选状态）。
- entry（入场）/ stop（止损）/ TP（止盈）/ RR（盈亏比）。
- Readiness（可执行就绪）。
- Dashboard（首页工作台）可执行状态。
- API action（接口动作）。
- order（下单）/ execution（执行）/ scheduler（定时器）/ automation（自动化）/ auto-trading（自动交易）。

P165 最终结论：SourceTrace（证据来源追踪）可以进入“未来只读输出授权审计”的下一步，但不能在 P165 直接进入 Controller（控制器）/ API（接口）/ Dashboard（首页工作台）接线。后续必须先用 P166 锁死 P167 的最小改动范围，再考虑只读 detail read model（详情只读模型）接线。

## P165 边界确认

- P165 只新增本审计文档。
- P165 删除 `docs/P165.md` placeholder（占位文档）。
- P165 不新增 Java。
- P165 不新增测试。
- P165 不修改 production Java（生产 Java）。
- P165 不修改现有测试。
- P165 不修改 `dashboard.html`。
- P165 不新增 Controller（控制器）/ endpoint（接口入口）/ API（接口）/ schema（数据库结构）/ config（配置）/ service（服务）/ mapper（映射器）。
- P165 不读取 runtime（运行时）/ live（实时）/ external（外部）数据。
- P165 不生成真实 entry（入场）/ stop（止损）/ TP（止盈）/ RR（盈亏比）。
- P165 不调用 `BoundaryCandidateDTO.valid(...)`。
- P165 不升级 ExecutionPlan（执行计划）Readiness（可执行就绪）。
- P165 不接 order（下单）/ execution（执行）/ scheduler（定时器）/ automation（自动化）/ auto-trading（自动交易）。
