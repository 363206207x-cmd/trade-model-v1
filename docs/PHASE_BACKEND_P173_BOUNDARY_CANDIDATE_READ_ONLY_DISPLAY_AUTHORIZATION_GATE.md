# BACKEND-P173 BoundaryCandidate Read-Only Candidate Display Authorization Gate

Issue: #460
PR: #461
Branch: `p173`
Base: `48a18fc BACKEND-P172 BoundaryCandidate Read-Only Candidate Display Scope Audit (#459)`

本文件是 P173 的唯一交付物。P173 只做 BoundaryCandidate（边界候选交易计划）Read-Only Candidate（只读候选）Display（展示）Authorization Gate（授权门），不写 Java，不新增测试，不改 `dashboard.html`，不接 API（接口），不生成真实 entry（入场）/ stop（止损）/ TP（止盈）/ RR（盈亏比）。

## 一、这一步是干嘛的

P173 是 BoundaryCandidate（边界候选交易计划）Read-Only Candidate（只读候选）Display（展示）Authorization Gate（授权门）。

P173 是 P172-P175 最大安全任务包的第二步。P172 已经完成 Scope Audit（范围审计），P173 只把 P174 的最小代码范围锁住，避免 P174 写成生产候选生成、页面动作入口或自动交易前置。

本轮边界固定如下：

- 不写 Java。
- 不新增测试。
- 不改 `dashboard.html`。
- 不新增 Controller（控制器）/ endpoint（接口入口）/ API（接口）。
- 不新增 schema（数据库结构）/ config（配置）/ service（服务）/ mapper（映射器）。
- 本轮只规定 P174 如果写最小只读候选展示，最多允许改哪些文件。
- 不生成真实 entry（入场）/ stop（止损）/ TP（止盈）/ RR（盈亏比）。
- 不生成生产 `VALID`（有效候选状态）。
- 不升级 ExecutionPlan（执行计划）Readiness（可执行就绪）。
- 不接 order（下单）/ execution（执行）/ scheduler（定时器）/ automation（自动化）/ auto-trading（自动交易）。

P173 的核心作用是给 P174 设门：允许做 read-only display（只读展示），不允许做 production candidate generation（生产候选交易计划生成）。

## 二、P172 审计结论

P172 的正式文档是 `docs/PHASE_BACKEND_P172_BOUNDARY_CANDIDATE_READ_ONLY_DISPLAY_SCOPE_AUDIT.md`。P173 以 P172 为依据，不重新扩大范围。

P172 已确认：

- `BoundaryCandidateDTO`（边界候选交易计划数据对象）存在。
- `BoundaryCandidateService`（边界候选交易计划服务接口）和 `BoundaryCandidateServiceImpl`（边界候选交易计划服务实现）存在。
- MarketReadOnly（市场只读）candidate（候选）相关对象存在。
- `BoundaryCandidateDTO`（边界候选交易计划数据对象）有 `VALID`（有效候选状态）/ `INCOMPLETE`（证据不完整）/ `WATCH_ONLY`（仅观察）/ `INVALID`（无效）等状态表达能力。
- `BoundaryCandidateDTO.valid(...)` 存在，但它不能解释为生产 `VALID`（有效候选状态）已经授权。
- `BoundaryCandidateServiceImpl` 存在，但生产候选生成仍未授权。
- `MarketReadOnlyCandidateResultDTO`（市场只读候选结果数据对象）和 `MarketReadOnlyCandidateStatusEnum`（市场只读候选状态枚举）已有 `INCOMPLETE`（证据不完整）/ `BLOCKED`（禁止推进）/ `REVIEW_ONLY_CANDIDATE`（只允许复核的候选）语义。
- Dashboard Detail（首页详情）路径比 summary（汇总）路径安全。
- `PlanBoundaryDisplayVO`（计划边界展示对象）已有 `planBoundaryStatus`（计划边界状态）/ `incompleteReasons`（证据不完整原因）/ `blockingReasons`（禁止推进原因）/ `manualReviewRequired`（必须人工复核）/ `notTradeInstruction`（不是交易指令）等字段。
- 未来最安全路径是 Dashboard Detail（首页详情）的 read-only display（只读展示）和 PlanBoundaryDisplay（计划边界展示）区域。

P172 还明确提醒：DTO（数据对象）字段存在、service（服务）存在、`valid(...)` 工厂存在、测试存在，都不等于真实生产候选已经完成。P173 必须把这个提醒变成 P174 的改动边界。

## 三、是否允许 P174 写代码

P173 的明确结论是：可以允许 P174 写最小只读候选展示代码。

但 P174 必须极小，并且只能围绕 Dashboard Detail（首页详情）display（展示）/ PlanBoundaryDisplay（计划边界展示）/ adapter（适配器）层。

P174 允许的目标只有一个：让现有 PlanBoundaryDisplay（计划边界展示）更明确地表达 BoundaryCandidate（边界候选交易计划）的只读候选状态、缺失原因、阻断原因和人工复核语义。

P174 必须继续禁止：

- 不能新增 action API（动作接口）。
- 不能生成真实 entry（入场）/ stop（止损）/ TP（止盈）/ RR（盈亏比）。
- 不能生成生产 `VALID`（有效候选状态）。
- 不能调用 `BoundaryCandidateDTO.valid(...)` 进入生产路径。
- 不能升级 ExecutionPlan（执行计划）Readiness（可执行就绪）。
- 不能新增买入 / 卖出 / 平仓 / 反手按钮。
- 不能接 order（下单）/ execution（执行）/ scheduler（定时器）/ automation（自动化）/ auto-trading（自动交易）。

P174 如果发现必须修改 Controller（控制器）、API（接口）、dashboard 页面、BoundaryCandidate service（边界候选交易计划服务）或 DTO（数据对象），说明它已经超出 P173 授权，应停止并另开授权。

## 四、P174 允许改哪些文件

P174 最多允许改 1-3 个文件。默认授权范围如下：

1. `src/main/java/org/example/trademodel/service/dashboard/DefaultPlanBoundaryDisplayAdapter.java`
2. `src/test/java/org/example/trademodel/service/dashboard/DefaultPlanBoundaryDisplayAdapterTest.java`
3. 仅在必要时允许：`src/main/java/org/example/trademodel/vo/PlanBoundaryDisplayVO.java`

当前只读扫描显示，`PlanBoundaryDisplayVO`（计划边界展示对象）实际是 `DashboardDetailResponseVO`（首页详情响应对象）里的内部类，不是独立文件。因此 P174 默认不允许改 `DashboardDetailResponseVO.java`。只有未来明确新建或存在独立 `PlanBoundaryDisplayVO.java`，并且 P174 仍控制在 1-3 个文件内时，才允许触碰该 VO（视图对象）文件。

P174 的默认授权解释如下：

- 如果 `PlanBoundaryDisplayVO`（计划边界展示对象）已有足够字段，就默认不改它。
- 默认不允许改 `src/main/resources/templates/dashboard.html`。
- 默认不允许改 `src/main/java/org/example/trademodel/controller/DashboardController.java`。
- 默认不允许改 `src/main/java/org/example/trademodel/vo/DashboardDetailResponseVO.java`。
- 默认不允许改 `src/main/java/org/example/trademodel/service/impl/BoundaryCandidateServiceImpl.java`。
- 默认不允许改 `src/main/java/org/example/trademodel/dto/planboundary/BoundaryCandidateDTO.java`。
- 默认不允许改 `SourceTraceRuntimePopulationService`（证据来源追踪运行时填充服务）。
- 默认不允许改 ExecutionPlanDisplay（执行计划展示）/ ExecutionPlan（执行计划）Readiness（可执行就绪）相关文件。
- 不允许改 API（接口）/ endpoint（接口入口）/ mapper（映射器）/ schema（数据库结构）/ config（配置）。
- 不允许新增任何 order（下单）/ execution（执行）/ auto-trading（自动交易）字段或按钮。

如果 P174 只通过 `DefaultPlanBoundaryDisplayAdapter`（默认计划边界展示适配器）和测试就能表达 read-only candidate（只读候选）状态，那就是最优解。

## 五、P174 允许做什么

P174 只能做以下事情：

- 让 PlanBoundaryDisplay（计划边界展示）更明确展示 BoundaryCandidate（边界候选交易计划）的 read-only candidate（只读候选）状态。
- 展示 candidate status（候选状态）。
- 展示 missing fields（缺失字段）。
- 展示 blocking reasons（禁止推进原因）。
- 展示 review mode（复核模式）。
- 展示 `manualReviewRequired`（必须人工复核）。
- 展示 `notTradeInstruction`（不是交易指令）。
- 保持 `REVIEW_ONLY`（只允许复核）/ `INCOMPLETE`（证据不完整）/ `BLOCKED`（禁止推进）/ `WATCH_ONLY`（仅观察）。
- 只解释“为什么不能推进”或“为什么只能复核”。
- 不生成交易指令。
- 不生成 Readiness（可执行就绪）。
- 不生成真实 entry（入场）/ stop（止损）/ TP（止盈）/ RR（盈亏比）。
- 不生成生产 `VALID`（有效候选状态）。

P174 的展示语义必须像审计说明，而不是交易建议。用户看到的应该是“证据缺什么、哪里被阻断、为什么需要人工复核”，不是“现在可以开仓”。

## 六、P174 禁止做什么

P174 禁止做以下事情：

- 不允许改 `dashboard.html`。
- 不允许新增 Controller（控制器）/ endpoint（接口入口）/ API（接口）。
- 不允许改 summary（汇总）接口。
- 不允许新增 mapper（映射器）。
- 不允许修改 schema（数据库结构）。
- 不允许修改 config（配置）。
- 不允许读取真实 runtime data（运行时数据）。
- 不允许读取 live market data（实时行情）。
- 不允许读取 external data（外部数据）。
- 不允许调用 `BoundaryCandidateDTO.valid(...)` 进入生产路径。
- 不允许生成生产 `VALID`（有效候选状态）。
- 不允许生成 entry（入场）/ stop（止损）/ TP（止盈）/ RR（盈亏比）。
- 不允许升级 ExecutionPlan（执行计划）Readiness（可执行就绪）。
- 不允许新增买入 / 卖出 / 平仓 / 反手按钮。
- 不允许接 order（下单）/ execution（执行）/ scheduler（定时器）/ automation（自动化）/ auto-trading（自动交易）。

P174 如果需要展示点位数字，也不能把它们展示成可执行点位；但 P173 的推荐做法是 P174 暂时不展示真实点位数字，只展示状态和原因。

## 七、仍然禁止的路径

以下路径在 P173 之后仍然禁止，不能被 P174 放开：

- production candidate generation（生产候选交易计划生成）。
- source-owned runtime candidate generation（运行时证据来源候选生成）。
- real entry / stop / TP / RR value generation（真实入场 / 止损 / 止盈 / 盈亏比生成）。
- production `VALID` mapping（生产环境映射为有效候选）。
- `BoundaryCandidateDTO.valid(...)` production calls（生产环境调用 valid 工厂）。
- ExecutionPlan（执行计划）Readiness（可执行就绪）upgrade（升级）。
- Dashboard（首页工作台）readiness mutation（页面显示可执行状态）。
- Dashboard（首页工作台）trading action buttons（页面交易动作按钮）。
- Controller（控制器）/ endpoint（接口入口）/ API（接口）action wiring（动作接线）。
- schema（数据库结构）/ config（配置）/ mapper（映射器）changes（改动）。
- runtime data reads（读取运行时数据）。
- live market data reads（读取实时行情）。
- external data integration（接外部数据）。
- WebClient / RestTemplate（网络请求工具）。
- order API（下单接口）。
- execution API（执行接口）。
- scheduler（定时器）/ automation（自动化）/ auto-trading（自动交易）。

这些路径仍然禁止，是因为 BoundaryCandidate（边界候选交易计划）的真实来源链路、数值归属、生产 `VALID`（有效候选状态）授权和 ExecutionPlan（执行计划）Readiness（可执行就绪）都还没有闭环。

## 八、推荐下一步

推荐下一步为：

P174：BoundaryCandidate Read-Only Candidate Display Minimal Wiring（边界候选只读候选展示最小接线）。

P174 才可以开始最小只读候选展示代码，但只能做 Dashboard Detail（首页详情）display（展示）/ PlanBoundaryDisplay（计划边界展示）/ adapter（适配器）层。

P174 必须继续遵守：

- 不能改 `dashboard.html`。
- 不能接 action API（动作接口）。
- 不能生成交易点位。
- 不能生成生产 `VALID`（有效候选状态）。
- 不能调用 `BoundaryCandidateDTO.valid(...)` 进入生产路径。
- 不能升级 ExecutionPlan（执行计划）Readiness（可执行就绪）。
- 不能新增交易按钮。
- 不能自动交易。

## P173 边界确认

- P173 只新增本授权门文档。
- P173 删除 `docs/P173.md` placeholder（占位文档）。
- P173 不新增 Java。
- P173 不新增测试。
- P173 不修改 production Java（生产 Java）。
- P173 不修改现有测试。
- P173 不修改 `dashboard.html`。
- P173 不新增 Controller（控制器）/ endpoint（接口入口）/ API（接口）/ schema（数据库结构）/ config（配置）/ service（服务）/ mapper（映射器）。
- P173 不读取 runtime（运行时）/ live（实时）/ external（外部）数据。
- P173 不生成真实 entry（入场）/ stop（止损）/ TP（止盈）/ RR（盈亏比）。
- P173 不调用 `BoundaryCandidateDTO.valid(...)` 进入生产路径。
- P173 不升级 ExecutionPlan（执行计划）Readiness（可执行就绪）。
- P173 不接 order（下单）/ execution（执行）/ scheduler（定时器）/ automation（自动化）/ auto-trading（自动交易）。
