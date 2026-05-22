# BACKEND-P175 BoundaryCandidate Read-Only Candidate Display Closure

Issue: #464
PR: #465
Branch: `p175`
Base: `62c2230 BACKEND-P174 BoundaryCandidate Read-Only Candidate Display Minimal Wiring (#463)`

本文件是 P175 的唯一交付物。P175 只做 BoundaryCandidate（边界候选交易计划）Read-Only Candidate（只读候选）Display（展示）Closure（收口），不写 Java，不新增测试，不改 `dashboard.html`，不接 API（接口），不生成真实 entry（入场）/ stop（止损）/ TP（止盈）/ RR（盈亏比）。

## 一、这一步是干嘛的

P175 是 BoundaryCandidate（边界候选交易计划）Read-Only Candidate（只读候选）Display（展示）Closure（收口）。

P175 是 P172-P175 这一组的最后一步。本轮只确认 P172-P174 已经完成 BoundaryCandidate（边界候选交易计划）的只读候选展示链路，并把完成内容、未完成内容、后续禁止路径写清楚。

本轮边界固定如下：

- 不写 Java。
- 不新增测试。
- 不改 `dashboard.html`。
- 不新增 Controller（控制器）/ endpoint（接口入口）/ API（接口）。
- 不新增 schema（数据库结构）/ config（配置）/ service（服务）/ mapper（映射器）。
- 只确认 P172-P174 已经完成 read-only candidate display（只读候选展示）链路。
- 不生成真实 entry（入场）/ stop（止损）/ TP（止盈）/ RR（盈亏比）。
- 不生成生产 `VALID`（有效候选状态）。
- 不升级 ExecutionPlan（执行计划）Readiness（可执行就绪）。
- 不接 order（下单）/ execution（执行）/ scheduler（定时器）/ automation（自动化）/ auto-trading（自动交易）。

P175 不是功能扩展任务。它只给 P172-P175 这一组做边界收口。

## 二、P172 做了什么

P172 是 BoundaryCandidate（边界候选交易计划）Read-Only Candidate（只读候选）Display（展示）Scope Audit（范围审计）。

P172 完成了以下确认：

- 已确认 `BoundaryCandidateDTO`（边界候选交易计划数据对象）存在。
- 已确认 `BoundaryCandidateService`（边界候选交易计划服务接口）和 `BoundaryCandidateServiceImpl`（边界候选交易计划服务实现）存在。
- 已确认 MarketReadOnlyCandidate（市场只读候选）相关对象存在。
- 已确认 DTO（数据对象）、service（服务）和 `valid(...)` factory（有效候选工厂）存在，不等于 production candidate generation（生产候选交易计划生成）完成。
- 已确认 `BoundaryCandidateDTO.valid(...)` 存在，但不能解释为生产 `VALID`（有效候选状态）已授权。
- 已确认最安全路径是 Dashboard Detail（首页详情）/ PlanBoundaryDisplay（计划边界展示）区域。
- 已确认只读展示只能表达状态、缺失字段、阻断原因和人工复核模式。

P172 没有写代码，没有新增测试，没有修改 Dashboard（首页工作台）/ API（接口）/ schema（数据库结构）/ config（配置）。

P172 的结论是：可以继续进入授权门，但只能审计和授权只读展示，不能进入真实候选生成。

## 三、P173 做了什么

P173 是 BoundaryCandidate（边界候选交易计划）Read-Only Candidate（只读候选）Display（展示）Authorization Gate（授权门）。

P173 完成了以下授权：

- 授权 P174 只能围绕 `DefaultPlanBoundaryDisplayAdapter`（默认计划边界展示适配器）和对应测试做最小接线。
- 默认不允许改 `dashboard.html`。
- 默认不允许改 `DashboardController.java`（首页控制器）。
- 默认不允许改 `DashboardDetailResponseVO.java`（首页详情响应对象）。
- 默认不允许改 `BoundaryCandidateDTO`（边界候选交易计划数据对象）。
- 默认不允许改 `BoundaryCandidateServiceImpl`（边界候选交易计划服务实现）。
- 默认不允许改 API（接口）/ endpoint（接口入口）/ mapper（映射器）/ schema（数据库结构）/ config（配置）。
- 默认不允许新增任何 order（下单）/ execution（执行）/ auto-trading（自动交易）字段或按钮。

P173 没有写代码，没有新增测试。

P173 的结论是：P174 可以写最小 read-only candidate display（只读候选展示）代码，但只能在 Dashboard Detail（首页详情）/ PlanBoundaryDisplay（计划边界展示）/ adapter（适配器）层。

## 四、P174 做了什么

P174 是 BoundaryCandidate（边界候选交易计划）Read-Only Candidate（只读候选）Display（展示）Minimal Wiring（最小接线）。

P174 只修改了以下文件：

- `src/main/java/org/example/trademodel/service/dashboard/DefaultPlanBoundaryDisplayAdapter.java`
- `src/test/java/org/example/trademodel/service/dashboard/DefaultPlanBoundaryDisplayAdapterTest.java`

P174 删除了 `docs/P174.md` placeholder（占位文档）。

P174 没有修改：

- `src/main/resources/templates/dashboard.html`
- `src/main/java/org/example/trademodel/controller/DashboardController.java`
- `src/main/java/org/example/trademodel/vo/DashboardDetailResponseVO.java`
- `src/main/java/org/example/trademodel/dto/planboundary/BoundaryCandidateDTO.java`
- `src/main/java/org/example/trademodel/service/impl/BoundaryCandidateServiceImpl.java`
- `src/main/java/org/example/trademodel/service/impl/PlanServiceImpl.java`
- API（接口）/ service（服务）/ mapper（映射器）/ schema（数据库结构）/ config（配置）

P174 的实际完成内容如下：

- 补强了 PlanBoundaryDisplay（计划边界展示）对 BoundaryCandidate（边界候选交易计划）read-only candidate（只读候选）状态的表达。
- 对 `REVIEW_ONLY_CANDIDATE`（只允许复核的候选）映射为 `REVIEW_ONLY`（只允许复核）。
- 对 `BLOCKED`（禁止推进）做只读展示，并保留 blockingReasons（禁止推进原因）。
- 对 `WATCH_ONLY`（仅观察）做只读展示，并保留 blockingReasons（禁止推进原因）。
- 对 `INCOMPLETE`（证据不完整）做只读展示，并标记 incompleteReasons（证据不完整原因）。
- 对 `VALID`（有效候选状态）做安全降级，降级为 `INCOMPLETE`（证据不完整），并写入 unsafe status（不安全状态）标记，不允许作为生产 `VALID`（有效候选状态）输出。
- 给只读候选展示统一补充 `REVIEW_MODE:REVIEW_ONLY`（复核模式：只允许复核）和 `NOT_TRADE_INSTRUCTION`（不是交易指令）。
- 保持 `manualReviewRequired=true`（必须人工复核）。
- 保持 `notTradeInstruction=true`（不是交易指令）。

P174 测试覆盖了：

- adapter（适配器）能输出 read-only candidate（只读候选）状态。
- 缺证据时保持 `INCOMPLETE`（证据不完整）。
- `BLOCKED`（禁止推进）保持 fail-closed（失败关闭）。
- `WATCH_ONLY`（仅观察）保持 fail-closed（失败关闭）。
- `REVIEW_ONLY_CANDIDATE`（只允许复核的候选）映射为 `REVIEW_ONLY`（只允许复核）。
- `VALID`（有效候选状态）安全降级，不能作为生产 `VALID` 输出。
- 不暴露 production candidate point（生产候选点位）、Readiness（可执行就绪）、order（下单）/ execution（执行）/ automation（自动化）/ auto-trading（自动交易）字段或方法。
- 不调用 `BoundaryCandidateDTO.valid(...)` 进入生产路径。

P174 没有生成真实 entry（入场）/ stop（止损）/ TP（止盈）/ RR（盈亏比），没有升级 ExecutionPlan（执行计划）Readiness（可执行就绪），没有新增交易按钮，没有接自动交易。

## 五、P172-P175 这组完成了什么

P172-P175 这一组完成的是 BoundaryCandidate（边界候选交易计划）read-only candidate display（只读候选展示）最小链路。

现在已经完成的能力是：

- BoundaryCandidate（边界候选交易计划）相关的只读候选展示范围已审计。
- P174 最小代码范围已授权。
- PlanBoundaryDisplay（计划边界展示）已经能更清楚地表达只读候选状态。
- 用户未来可以通过 PlanBoundaryDisplay（计划边界展示）更清楚地看到候选状态、missing fields（缺失字段）、blocking reasons（禁止推进原因）、review mode（复核模式）。
- `REVIEW_ONLY`（只允许复核）、`INCOMPLETE`（证据不完整）、`BLOCKED`（禁止推进）、`WATCH_ONLY`（仅观察）这些失败关闭语义已经进入展示边界。

但这只是只读解释层，不是 production candidate generation（生产候选交易计划生成）。

这只是人工复核材料，不是交易指令。

仍然不能生成真实交易点位。仍然不能生成生产 `VALID`（有效候选状态）。仍然不能升级 Readiness（可执行就绪）。仍然不能自动交易。

## 六、P175 的结论

P172-P175 这一组完成。

完成的是 BoundaryCandidate（边界候选交易计划）read-only candidate display（边界候选只读候选展示）闭环。

还不是 production candidate generation（生产候选交易计划生成）。

还不是 source-owned runtime candidate generation（运行时证据来源候选生成）。

还不是真实 entry（入场）/ stop（止损）/ TP（止盈）/ RR（盈亏比）。

还不是 ExecutionPlan（执行计划）Readiness（可执行就绪）。

还不是自动交易。

下一步必须回到 `docs/PROJECT_PROGRESS_INDEX.md` 的推荐路线，不能直接跳自动交易或真实点位。

## 七、推荐下一步

推荐下一步为：

P176：Project Progress Index Refresh After BoundaryCandidate Display（BoundaryCandidate 展示后项目总进度索引刷新）。

P176 应该更新 `docs/PROJECT_PROGRESS_INDEX.md`。

原因是 P172-P175 已经完成 BoundaryCandidate（边界候选交易计划）read-only candidate display（只读候选展示），项目总索引需要把 BoundaryCandidate（边界候选交易计划）进度从“DTO（数据对象）/ service skeleton（服务骨架）有但展示未闭环”更新为“已完成只读候选展示，但未完成生产候选 / 真实点位 / Readiness（可执行就绪）”。

P176 仍然只改文档，不写代码，不新增测试，不接 Dashboard（首页工作台）新功能，不接 API（接口），不生成交易点位，不接自动交易。

## 八、仍然禁止的路径

以下路径在 P175 之后仍然禁止：

- production candidate generation（生产候选交易计划生成）。
- source-owned runtime candidate generation（运行时证据来源候选生成）。
- real entry / stop / TP / RR value generation（真实入场 / 止损 / 止盈 / 盈亏比生成）。
- production `VALID` mapping（生产环境映射为有效候选）。
- `BoundaryCandidateDTO.valid(...)` production calls（生产环境调用 valid 工厂）。
- ExecutionPlan（执行计划）Readiness（可执行就绪）upgrade（升级为可执行）。
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

这些路径仍然禁止，是因为 P172-P175 只完成了 read-only display（只读展示）。它没有证明真实来源链路闭环，没有证明数值归属闭环，没有授权生产 `VALID`（有效候选状态），也没有让 ExecutionPlan（执行计划）进入 Readiness（可执行就绪）。

## P175 边界确认

- P175 只新增本收口文档。
- P175 删除 `docs/P175.md` placeholder（占位文档）。
- P175 不新增 Java。
- P175 不新增测试。
- P175 不修改 production Java（生产 Java）。
- P175 不修改现有测试。
- P175 不修改 `dashboard.html`。
- P175 不新增 Controller（控制器）/ endpoint（接口入口）/ API（接口）/ schema（数据库结构）/ config（配置）/ service（服务）/ mapper（映射器）。
- P175 不读取 runtime（运行时）/ live（实时）/ external（外部）数据。
- P175 不生成真实 entry（入场）/ stop（止损）/ TP（止盈）/ RR（盈亏比）。
- P175 不调用 `BoundaryCandidateDTO.valid(...)` 进入生产路径。
- P175 不生成生产 `VALID`（有效候选状态）。
- P175 不升级 ExecutionPlan（执行计划）Readiness（可执行就绪）。
- P175 不接 order（下单）/ execution（执行）/ scheduler（定时器）/ automation（自动化）/ auto-trading（自动交易）。
