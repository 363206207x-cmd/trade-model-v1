# BACKEND-P166 SourceTrace Read Model Authorization Gate

Issue: #446
PR: #447
Branch: p166
Base: 9b3fa6b BACKEND-P165 SourceTrace Read Model Controller Scope Audit (#445)

本文件是 P166 的唯一交付物。P166 只做 SourceTrace（证据来源追踪）Read Model（只读输出模型）Authorization Gate（授权门），不写 Java，不新增测试，不接 Controller（控制器）/ API（接口）/ Dashboard（首页工作台），不生成真实 entry（入场）/ stop（止损）/ TP（止盈）/ RR（盈亏比）。

## 一、这一步是干嘛的

P166 是 SourceTrace（证据来源追踪）Read Model（只读输出模型）Authorization Gate（授权门）。

P166 是 P166-P168 最大安全任务包的第一步。这个任务包只允许推进 SourceTrace（证据来源追踪）只读输出和 Dashboard（首页工作台）只读展示，不允许进入交易执行、真实点位生成或自动交易。

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

P166 只规定一件事：P167 如果开始写最小只读输出接线，最多允许改哪些文件、允许做什么、禁止做什么。

## 二、P165 审计结论

P165 的正式文档是 `docs/PHASE_BACKEND_P165_SOURCETRACE_READ_MODEL_CONTROLLER_SCOPE_AUDIT.md`。P166 以 P165 的审计结论为前提，不重新扩大范围。

P165 已确认：

- SourceTrace（证据来源追踪）已有 helper（辅助器）和 service wrapper（服务包装层）。
- `SourceTraceRuntimePopulationService`（证据来源追踪运行时填充服务）只能接收 `MarketReadOnlyEvidenceSnapshotDTO`（市场只读证据快照）和 `MarketReadOnlyCandidateResultDTO`（市场只读候选结果），只能输出 `SourceTraceDTO`（证据来源追踪数据对象）。
- DashboardController（首页控制器）已有 `/dashboard`、`/api/dashboard/summary`、`/api/dashboard/detail`。
- `DashboardDetailResponseVO`（首页详情响应对象）已有 `SourceTraceDTO`（证据来源追踪数据对象）字段。
- detail（详情）路径比 summary（汇总）路径更安全。
- `dashboard.html` 已有读取 `detail.sourceTrace` 的展示片段。
- P167 不应该先改 `dashboard.html`。
- P167 应优先走 detail read model（详情只读模型），不要先改首页大 UI。

这些结论说明：P167 可以考虑最小只读输出接线，但不能把 P167 写成 Controller（控制器）扩展、API（接口）扩展、页面改造或交易能力接线。

## 三、是否允许 P167 写代码

P166 的明确结论是：可以允许 P167 写最小只读输出接线。

但 P167 必须极小，并且只能围绕 Dashboard Detail（首页详情）Read Model（只读输出模型）和 adapter（适配器）层进行。P167 的目标不是“新增能力”，而是“把已有只读 SourceTrace（证据来源追踪）输出边界表达得更清楚”。

P167 必须遵守：

- 只能围绕 dashboard detail read model（首页详情只读模型）。
- 不能新增 action API（动作接口）。
- 不能改 `dashboard.html`。
- 不能生成交易点位。
- 不能生成 `VALID`（有效候选状态）。
- 不能升级 Readiness（可执行就绪）。
- 不能接自动交易。
- 不能把 SourceTrace（证据来源追踪）解释为交易指令。

P167 如果发现必须改 Controller（控制器）、summary（汇总）接口、schema（数据库结构）、mapper（映射器）、config（配置）或 service（服务），必须停止，不能在 P167 内自行扩大范围。

## 四、P167 允许改哪些文件

P167 最多允许改 1-3 个文件。默认授权范围如下：

1. `src/main/java/org/example/trademodel/service/dashboard/DefaultDashboardSourceTraceDetailAdapter.java`
2. `src/test/java/org/example/trademodel/service/dashboard/DefaultDashboardSourceTraceDetailAdapterTest.java`
3. `src/main/java/org/example/trademodel/vo/DashboardDetailResponseVO.java`

授权解释：

- 第一优先级是 `DefaultDashboardSourceTraceDetailAdapter.java`。如果 P167 需要最小只读输出接线，只能在这个 adapter（适配器）里补全只读字段、缺失原因或禁止推进原因。
- 第二优先级是 `DefaultDashboardSourceTraceDetailAdapterTest.java`。如果 P167 必须测试，只允许围绕 adapter（适配器）补最小测试，证明仍然 `REVIEW_ONLY`（只允许复核）、`INCOMPLETE`（证据不完整）、`BLOCKED`（禁止推进），且不生成交易点位。
- 第三优先级是 `DashboardDetailResponseVO.java`。但是 P165 已确认它已有 `SourceTraceDTO`（证据来源追踪数据对象）字段，所以默认不允许改它。只有当 P167 证明现有字段无法表达只读展示边界，且不新增交易语义字段时，才可以把它作为第三个文件处理。

P167 默认不授权以下文件：

- 不允许改 `src/main/java/org/example/trademodel/controller/DashboardController.java`。除非 P167 证明无法绕开 Controller（控制器），否则默认不改；如果必须改，P167 应停止并另开授权。
- 不允许改 `src/main/resources/templates/dashboard.html`。
- 不允许改 summary（汇总）接口相关文件。
- 不允许改 schema（数据库结构）/ config（配置）/ mapper（映射器）。
- 不允许改 `SourceTraceRuntimePopulationService`（证据来源追踪运行时填充服务）；如果必须改，停止并另开授权。
- 不允许改 `SourceTraceRuntimePopulationServiceImpl`（证据来源追踪运行时填充服务实现）。
- 不允许改 `BoundaryCandidateServiceImpl`（边界候选交易计划服务实现）。
- 不允许改 `PlanServiceImpl`（执行计划服务实现）。
- 不允许新增 Controller（控制器）/ endpoint（接口入口）/ API（接口）/ service（服务）/ mapper（映射器）。

如果 P167 只需要确认现有 `DashboardDetailResponseVO`（首页详情响应对象）已经承载 `SourceTraceDTO`（证据来源追踪数据对象），则不应该修改 `DashboardDetailResponseVO.java`。

如果 adapter（适配器）已经能返回 `SourceTraceDTO`（证据来源追踪数据对象），P167 只能做最小补测试或只读字段整理，不允许扩成业务接线。

## 五、P167 允许做什么

P167 只能做以下事情：

- 让 detail read model（详情只读模型）更明确承载 `SourceTraceDTO`（证据来源追踪数据对象）。
- 保持 SourceTrace（证据来源追踪）为 read-only（只读）。
- 保持 `REVIEW_ONLY`（只允许复核）。
- 保持 `INCOMPLETE`（证据不完整）。
- 保持 `BLOCKED`（禁止推进）。
- 只展示 source owner（来源归属）、source reference（来源引用）、timeframe（周期）、freshness（新鲜度）、missing fields（缺失字段）、blocking reasons（禁止推进原因）、reviewMode（复核模式）。
- 只展示证据来源、缺失原因、禁止推进原因、reviewMode（复核模式）。
- 不生成交易指令。
- 不生成 Readiness（可执行就绪）。
- 不生成 entry（入场）/ stop（止损）/ TP（止盈）/ RR（盈亏比）。
- 不生成 `VALID`（有效候选状态）。

P167 的正确形态应该是“只读展示更清楚”，不是“业务状态更靠近可执行”。

## 六、P167 禁止做什么

P167 禁止做以下事情：

- 不允许改 `dashboard.html`。
- 不允许新增 Controller（控制器）/ endpoint（接口入口）/ API（接口）。
- 不允许改 summary（汇总）接口。
- 不允许新增 mapper（映射器）。
- 不允许修改 schema（数据库结构）。
- 不允许修改 config（配置）。
- 不允许读取真实 runtime data（运行时数据）。
- 不允许读取 live market data（实时行情）。
- 不允许读取 external data（外部数据）。
- 不允许调用 `BoundaryCandidateDTO.valid(...)`。
- 不允许生成 `VALID`（有效候选状态）。
- 不允许生成 entry（入场）/ stop（止损）/ TP（止盈）/ RR（盈亏比）。
- 不允许升级 ExecutionPlan（执行计划）Readiness（可执行就绪）。
- 不允许接 order（下单）/ execution（执行）/ scheduler（定时器）/ automation（自动化）/ auto-trading（自动交易）。

如果 P167 需要越过上述任意一条，说明 P167 已经不再是最小只读输出接线，必须停止。

## 七、仍然禁止的路径

以下路径在 P166 之后仍然禁止，不能被 P167 或 P168 放开：

- production candidate generation（生产候选交易计划生成）。
- source-owned runtime candidate generation（运行时证据来源候选生成）。
- real entry / stop / TP / RR value generation（真实入场 / 止损 / 止盈 / 盈亏比生成）。
- production VALID mapping（生产环境映射为有效候选）。
- `BoundaryCandidateDTO.valid(...)` production calls（生产环境调用 valid 工厂）。
- ExecutionPlan（执行计划）Readiness（可执行就绪）upgrade（升级）。
- Dashboard（首页工作台）readiness mutation（页面显示可执行状态）。
- `dashboard.html` changes（页面改动）。
- Controller（控制器）/ endpoint（接口入口）/ API（接口）action wiring（动作接线）。
- schema（数据库结构）/ config（配置）/ mapper（映射器）changes（改动）。
- runtime data reads（读取运行时数据）。
- live market data reads（读取实时行情）。
- external data integration（接外部数据）。
- WebClient（网络请求工具）/ RestTemplate（网络请求工具）。
- order API（下单接口）。
- execution API（执行接口）。
- scheduler（定时器）/ automation（自动化）/ auto-trading（自动交易）。

这些路径继续禁止，是因为 SourceTrace（证据来源追踪）的当前职责只是把证据来源、缺失项和阻断原因说清楚。它不能证明候选交易计划有效，不能证明执行计划就绪，也不能触发任何交易动作。

## 八、推荐下一步

推荐下一步：

P167：SourceTrace Read Model Minimal Wiring（证据来源追踪只读输出最小接线）。

P167 才可以开始最小只读输出接线，但只能做 detail read model（详情只读模型）/ adapter（适配器）层。

P167 必须继续遵守：

- 不能接 action API（动作接口）。
- 不能改 `dashboard.html`。
- 不能生成交易点位。
- 不能生成 `VALID`（有效候选状态）。
- 不能升级 Readiness（可执行就绪）。
- 不能接 order（下单）/ execution（执行）/ scheduler（定时器）/ automation（自动化）/ auto-trading（自动交易）。

P166 最终结论：授权 P167 做极小的 SourceTrace（证据来源追踪）Dashboard Detail（首页详情）Read Model（只读输出模型）最小接线；默认只允许碰 adapter（适配器）和必要的 adapter 测试。`DashboardDetailResponseVO.java` 因为已经有 `SourceTraceDTO`（证据来源追踪数据对象）字段，默认不改。Controller（控制器）、API（接口）、summary（汇总）、`dashboard.html`、schema（数据库结构）、mapper（映射器）、config（配置）、交易点位、Readiness（可执行就绪）和自动交易全部继续关闭。

## P166 边界确认

- P166 只新增本授权门文档。
- P166 删除 `docs/P166.md` placeholder（占位文档）。
- P166 不新增 Java。
- P166 不新增测试。
- P166 不修改 production Java（生产 Java）。
- P166 不修改现有测试。
- P166 不修改 `dashboard.html`。
- P166 不新增 Controller（控制器）/ endpoint（接口入口）/ API（接口）/ schema（数据库结构）/ config（配置）/ service（服务）/ mapper（映射器）。
- P166 不读取 runtime（运行时）/ live（实时）/ external（外部）数据。
- P166 不生成真实 entry（入场）/ stop（止损）/ TP（止盈）/ RR（盈亏比）。
- P166 不调用 `BoundaryCandidateDTO.valid(...)`。
- P166 不升级 ExecutionPlan（执行计划）Readiness（可执行就绪）。
- P166 不接 order（下单）/ execution（执行）/ scheduler（定时器）/ automation（自动化）/ auto-trading（自动交易）。
