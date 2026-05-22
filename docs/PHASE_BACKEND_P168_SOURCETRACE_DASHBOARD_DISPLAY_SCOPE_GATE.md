# BACKEND-P168 SourceTrace Dashboard Display Scope Gate

Issue: #450
PR: #451
Branch: p168
Base: a6d3a8c BACKEND-P167 SourceTrace Read Model Minimal Wiring (#449)

本文件是 P168 的唯一交付物。P168 只做 SourceTrace（证据来源追踪）Dashboard（首页工作台）Display（展示）Scope Gate（范围门），不写 Java，不新增测试，不改 `dashboard.html`，不接 Controller（控制器）/ API（接口），不生成真实 entry（入场）/ stop（止损）/ TP（止盈）/ RR（盈亏比）。

## 一、这一步是干嘛的

P168 是 SourceTrace（证据来源追踪）Dashboard（首页工作台）Display（展示）Scope Gate（范围门）。

P168 是 P165-P168 最大安全任务包的最后一步。这个任务包的目标是把 SourceTrace（证据来源追踪）从 service wrapper（服务包装层）推进到 Dashboard Detail（首页详情）Read Model（只读输出模型）边界，但不打开交易执行能力。

本轮明确不做以下事情：

- 不写 Java。
- 不新增测试。
- 不改 `dashboard.html`。
- 不新增 Controller（控制器）/ endpoint（接口入口）/ API（接口）。
- 不新增 schema（数据库结构）/ config（配置）/ service（服务）/ mapper（映射器）。
- 不读取 runtime data（运行时数据）/ live market data（实时行情）/ external data（外部数据）。
- 不生成真实 entry（入场）/ stop（止损）/ TP（止盈）/ RR（盈亏比）。
- 不打开 Readiness（可执行就绪）。
- 不调用 `BoundaryCandidateDTO.valid(...)`。
- 不接 order（下单）/ execution（执行）/ scheduler（定时器）/ automation（自动化）/ auto-trading（自动交易）。

P168 只决定未来是否允许单独做 Dashboard（首页工作台）Read-only（只读）Display（展示）增强，以及未来展示必须限制在哪些字段和文案语义内。

## 二、P165-P167 已完成什么

P165 已完成 SourceTrace（证据来源追踪）Read Model（只读输出模型）/ Controller（控制器）范围审计，结论是：

- detail（详情）路径比 summary（汇总）路径更安全。
- `DashboardDetailResponseVO`（首页详情响应对象）已有 `SourceTraceDTO`（证据来源追踪数据对象）字段。
- `dashboard.html` 已有读取 `detail.sourceTrace` 的片段。
- 页面已经有展示入口，但页面存在不等于 SourceTrace（证据来源追踪）运行时接线完成。
- 后续应该优先走 detail read model（详情只读模型），不要先改首页大 UI。

P166 已完成 SourceTrace（证据来源追踪）Read Model（只读输出模型）Authorization Gate（授权门），结论是：

- P167 只允许做 detail read model（详情只读模型）/ adapter（适配器）层最小接线。
- P167 默认不允许改 `DashboardController.java`。
- P167 默认不允许改 `dashboard.html`。
- P167 默认不允许改 summary（汇总）接口、schema（数据库结构）、config（配置）、mapper（映射器）。
- P167 不能生成 `VALID`（有效候选状态）。
- P167 不能生成 entry（入场）/ stop（止损）/ TP（止盈）/ RR（盈亏比）。
- P167 不能升级 Readiness（可执行就绪）。
- P167 不能接自动交易。

P167 已完成 SourceTrace（证据来源追踪）Read Model（只读输出模型）Minimal Wiring（最小接线），实际结果是：

- 已在 `DefaultDashboardSourceTraceDetailAdapter`（默认首页证据来源追踪详情适配器）中补强只读输出边界。
- 已显式设置 `sourceOwner`（证据来源所有者）、`sourceRef`（证据来源引用）、`sourceTimeframe`（证据来源周期）、`freshnessStatus`（新鲜度状态）、`reviewMode`（复核模式）和 `blockingReasons`（禁止推进原因）。
- 继续保持 `REVIEW_ONLY`（只允许复核）。
- 继续保持 `INCOMPLETE`（证据不完整）。
- 继续通过 blocking reasons（禁止推进原因）表达只读、边界来源缺失、运行时 K 线上下文不可用、交易点位生成关闭等状态。
- 没有改 `dashboard.html`。
- 没有改 Controller（控制器）/ API（接口）。
- 没有改 mapper（映射器）/ schema（数据库结构）/ config（配置）。
- 没有生成 `VALID`（有效候选状态）。
- 没有生成 entry（入场）/ stop（止损）/ TP（止盈）/ RR（盈亏比）。
- 没有升级 Readiness（可执行就绪）。
- 没有接自动交易。

因此 P168 的判断基础是：后端 detail read model（详情只读模型）边界已经更清楚，但页面展示增强还没有开始。

## 三、是否允许未来改 dashboard.html

P168 的保守结论是：可以允许未来单独开 P169 做 `dashboard.html` 的只读展示增强。

但 P169 必须极小，并且只能展示已有 `detail.sourceTrace` 字段。P169 不能把展示增强写成页面交易功能，也不能通过页面文案暗示 SourceTrace（证据来源追踪）已经可执行。

P169 必须遵守：

- 只能展示已有 `detail.sourceTrace` 字段。
- 不能改 Controller（控制器）。
- 不能改 API（接口）。
- 不能改 service（服务）/ mapper（映射器）/ schema（数据库结构）/ config（配置）。
- 不能新增交易动作按钮。
- 不能打开 Readiness（可执行就绪）状态。
- 不能生成交易点位。
- 不能自动交易。
- 不能把 `REVIEW_ONLY`（只允许复核）解释成可执行。
- 不能把 `INCOMPLETE`（证据不完整）/ `BLOCKED`（禁止推进）解释成交易机会。

P168 不允许本轮直接改 `dashboard.html`。P168 只是给 P169 定义范围。

## 四、未来 P169 允许改哪些文件

P169 最多允许改 1-3 个文件。默认授权范围如下：

1. `src/main/resources/templates/dashboard.html`
2. `src/test/java/org/example/trademodel/controller/DashboardControllerTest.java`
3. `src/test/java/org/example/trademodel/service/dashboard/DefaultDashboardSourceTraceDetailAdapterTest.java`

授权解释：

- 第一优先级是 `dashboard.html`。P169 如果实现，只允许在现有 Dashboard（首页工作台）detail（详情）展示区增强 `detail.sourceTrace` 的只读展示。
- 第二优先级是 `DashboardControllerTest.java`。如果需要证明 detail（详情）响应中已有 SourceTrace（证据来源追踪）字段，可补最小测试；不能新增 action API（动作接口）测试。
- 第三优先级是 `DefaultDashboardSourceTraceDetailAdapterTest.java`。如果需要补静态约束或只读边界测试，可以补充，但不能改 adapter（适配器）生产代码。

P169 默认不允许改以下文件：

- 不允许改 `src/main/java/org/example/trademodel/controller/DashboardController.java`。
- 不允许改 `src/main/java/org/example/trademodel/vo/DashboardDetailResponseVO.java`。
- 不允许改 `src/main/java/org/example/trademodel/service/dashboard/DefaultDashboardSourceTraceDetailAdapter.java`。
- 不允许改 API（接口）/ endpoint（接口入口）。
- 不允许改 schema（数据库结构）/ config（配置）/ mapper（映射器）。
- 不允许改 `SourceTraceRuntimePopulationService`（证据来源追踪运行时填充服务）。
- 不允许改 `SourceTraceRuntimePopulationServiceImpl`（证据来源追踪运行时填充服务实现）。
- 不允许改 `BoundaryCandidateServiceImpl`（边界候选交易计划服务实现）。
- 不允许改 `PlanServiceImpl`（执行计划服务实现）。
- 不允许新增任何 order（下单）/ execution（执行）/ auto-trading（自动交易）字段或按钮。

如果 P169 发现必须改 Controller（控制器）、API（接口）、VO（视图对象）、adapter（适配器）生产代码、schema（数据库结构）或 mapper（映射器），说明它已经不是纯页面只读展示，应停止并另开授权。

## 五、未来 P169 允许展示什么

P169 只能展示已有 `detail.sourceTrace` 中的只读字段：

- `sourceOwner`（证据来源所有者）。
- `sourceRef`（证据来源引用）。
- `sourceTimeframe`（证据来源周期）。
- `freshnessStatus`（新鲜度状态）。
- `missingFields`（缺失字段）。
- `blockingReasons`（禁止推进原因）。
- `reviewMode`（复核模式）。
- `fallbackStatus`（失败关闭状态）。
- `manualReviewRequired`（必须人工复核）。
- `notTradeInstruction`（不是交易指令）。

这些字段只能用于解释“证据来源是否完整、为什么不能推进、是否需要人工复核”。它们不能用于表达“可以买、可卖、可平仓、可反手、可自动执行”。

## 六、未来 P169 禁止展示什么

P169 禁止展示或暗示以下内容：

- 不展示“可执行”。
- 不展示“可下单”。
- 不展示“自动执行”。
- 不展示“建议立即开仓”。
- 不展示“建议立即平仓”。
- 不展示“建议反手”。
- 不展示真实 entry（入场）/ stop（止损）/ TP（止盈）/ RR（盈亏比）为可执行指令。
- 不新增买入按钮。
- 不新增卖出按钮。
- 不新增平仓按钮。
- 不新增反手按钮。
- 不把 `REVIEW_ONLY`（只允许复核）改成可执行状态。
- 不把 `INCOMPLETE`（证据不完整）/ `BLOCKED`（禁止推进）解释成交易机会。

如果页面文案需要解释 SourceTrace（证据来源追踪），只能解释为 Read-only（只读）证据追踪和人工复核材料，不能解释为交易建议或交易计划。

## 七、仍然禁止的路径

以下路径在 P168 之后仍然禁止，不能被 P169 放开：

- production candidate generation（生产候选交易计划生成）。
- source-owned runtime candidate generation（运行时证据来源候选生成）。
- real entry / stop / TP / RR value generation（真实入场 / 止损 / 止盈 / 盈亏比生成）。
- production VALID mapping（生产环境映射为有效候选）。
- `BoundaryCandidateDTO.valid(...)` production calls（生产环境调用 valid 工厂）。
- ExecutionPlan（执行计划）Readiness（可执行就绪）upgrade（升级）。
- Dashboard（首页工作台）readiness mutation（页面显示可执行状态）。
- Controller（控制器）/ endpoint（接口入口）/ API（接口）action wiring（动作接线）。
- schema（数据库结构）/ config（配置）/ mapper（映射器）changes（改动）。
- runtime data reads（读取运行时数据）。
- live market data reads（读取实时行情）。
- external data integration（接外部数据）。
- WebClient（网络请求工具）/ RestTemplate（网络请求工具）。
- order API（下单接口）。
- execution API（执行接口）。
- scheduler（定时器）/ automation（自动化）/ auto-trading（自动交易）。

这些路径继续禁止，是因为 SourceTrace（证据来源追踪）当前只说明证据来源和失败关闭原因，不证明交易计划有效，不证明执行计划就绪，也不触发任何动作。

## 八、推荐下一步

推荐下一步：

P169：SourceTrace Dashboard Read-Only Display（首页证据来源追踪只读展示）。

P169 才可以开始最小 `dashboard.html` 只读展示，但只能展示已有 `detail.sourceTrace` 字段。

P169 必须继续遵守：

- 不能改 Controller（控制器）。
- 不能改 API（接口）。
- 不能改 service（服务）/ mapper（映射器）/ schema（数据库结构）/ config（配置）。
- 不能生成交易点位。
- 不能生成 `VALID`（有效候选状态）。
- 不能升级 Readiness（可执行就绪）。
- 不能新增交易按钮。
- 不能自动交易。

P169 的正确形态是：让用户更清楚地看到 SourceTrace（证据来源追踪）的来源、缺失项、阻断原因和人工复核状态，而不是让用户以为系统已经可以执行交易。

## 九、P168 结论

P165-P168 这一组完成后，SourceTrace（证据来源追踪）已经从 service wrapper（服务包装层）推进到 Dashboard Detail（首页详情）Read Model（只读输出模型）边界。

当前真实状态是：

- 已完成 SourceTrace（证据来源追踪）Controller（控制器）范围审计。
- 已完成 SourceTrace（证据来源追踪）只读输出授权门。
- 已完成 Dashboard Detail（首页详情）adapter（适配器）层最小只读接线。
- 还没有完成页面增强。
- 还没有进入可执行状态。
- 还没有真实 entry（入场）/ stop（止损）/ TP（止盈）/ RR（盈亏比）。
- 还没有自动交易。

下一步可以进入 P169，但只能做 Read-only（只读）Display（展示）。P169 不能改变 Controller（控制器）、API（接口）、后端服务、数据库结构、执行计划状态或交易能力。

## P168 边界确认

- P168 只新增本范围门文档。
- P168 删除 `docs/P168.md` placeholder（占位文档）。
- P168 不新增 Java。
- P168 不新增测试。
- P168 不修改 production Java（生产 Java）。
- P168 不修改现有测试。
- P168 不修改 `dashboard.html`。
- P168 不新增 Controller（控制器）/ endpoint（接口入口）/ API（接口）/ schema（数据库结构）/ config（配置）/ service（服务）/ mapper（映射器）。
- P168 不读取 runtime（运行时）/ live（实时）/ external（外部）数据。
- P168 不生成真实 entry（入场）/ stop（止损）/ TP（止盈）/ RR（盈亏比）。
- P168 不调用 `BoundaryCandidateDTO.valid(...)`。
- P168 不升级 ExecutionPlan（执行计划）Readiness（可执行就绪）。
- P168 不接 order（下单）/ execution（执行）/ scheduler（定时器）/ automation（自动化）/ auto-trading（自动交易）。
