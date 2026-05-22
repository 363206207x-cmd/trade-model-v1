# BACKEND-P170 SourceTrace Read-Only Display Closure

Issue: #454
PR: #455
Branch: p170
Base: 9bb2a2f BACKEND-P169 SourceTrace Dashboard Read-Only Display (#453)

本文件是 P170 的唯一交付物。P170 只做 SourceTrace（证据来源追踪）Read-only（只读）Display（展示）Closure（收口），不写 Java，不新增测试，不改 `dashboard.html`，不接 Controller（控制器）/ API（接口），不生成真实 entry（入场）/ stop（止损）/ TP（止盈）/ RR（盈亏比）。

## 一、这一步是干嘛的

P170 是 SourceTrace（证据来源追踪）Read-Only Display（只读展示）Closure（收口）。

P170 是 P165-P170 这一组的最后一步。本轮只确认 P165-P169 已经完成 SourceTrace（证据来源追踪）只读输出和 Dashboard（首页工作台）只读展示闭环。

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

P170 的作用是把这一组的边界说清楚：完成的是只读解释层，不是交易计划生成层。

## 二、P165 做了什么

P165 是 SourceTrace（证据来源追踪）Read Model（只读输出模型）/ Controller（控制器）Scope Audit（范围审计）。

P165 已确认：

- detail（详情）路径比 summary（汇总）路径安全。
- `DashboardDetailResponseVO`（首页详情响应对象）已有 `SourceTraceDTO`（证据来源追踪数据对象）字段。
- `dashboard.html` 已有读取 `detail.sourceTrace` 的片段。
- Dashboard（首页工作台）已有展示入口，但这不等于 SourceTrace（证据来源追踪）运行时接线完成。
- 后续应该优先走 detail read model（详情只读模型），不要先改首页大 UI。

P165 没有写代码，没有新增测试，没有接 API（接口）/ Controller（控制器）/ Dashboard（首页工作台）。

## 三、P166 做了什么

P166 是 SourceTrace（证据来源追踪）Read Model（只读输出模型）Authorization Gate（授权门）。

P166 已确认：

- 授权 P167 只允许 detail read model（详情只读模型）/ adapter（适配器）层最小接线。
- 默认不允许改 `DashboardController.java`。
- 默认不允许改 `dashboard.html`。
- 默认不允许改 summary（汇总）接口 / schema（数据库结构）/ config（配置）/ mapper（映射器）。
- 默认不允许改 `SourceTraceRuntimePopulationService`（证据来源追踪运行时填充服务）。
- 默认不允许改 `BoundaryCandidateServiceImpl`（边界候选交易计划服务实现）/ `PlanServiceImpl`（执行计划服务实现）。

P166 没有写代码，没有新增测试。

## 四、P167 做了什么

P167 是 SourceTrace（证据来源追踪）Read Model（只读输出模型）Minimal Wiring（最小接线）。

P167 修改了：

- `src/main/java/org/example/trademodel/service/dashboard/DefaultDashboardSourceTraceDetailAdapter.java`
- `src/test/java/org/example/trademodel/service/dashboard/DefaultDashboardSourceTraceDetailAdapterTest.java`

P167 没有修改：

- `src/main/java/org/example/trademodel/vo/DashboardDetailResponseVO.java`
- `src/main/resources/templates/dashboard.html`
- `src/main/java/org/example/trademodel/controller/DashboardController.java`
- API（接口）/ service（服务）/ mapper（映射器）/ schema（数据库结构）/ config（配置）。

P167 完成的实际内容：

- 补强了 detail read model（详情只读模型）只读输出边界。
- 显式设置或验证 `sourceOwner`（证据来源所有者）。
- 显式设置或验证 `sourceRef`（证据来源引用）。
- 显式设置或验证 `sourceTimeframe`（证据来源周期）。
- 显式设置或验证 `freshnessStatus`（新鲜度状态）。
- 显式设置或验证 `reviewMode`（复核模式）。
- 显式设置或验证 `blockingReasons`（禁止推进原因）。
- 保持 `REVIEW_ONLY`（只允许复核）。
- 保持 `INCOMPLETE`（证据不完整）。
- 通过 `blockingReasons`（禁止推进原因）表达 `BLOCKED`（禁止推进）语义。

P167 没有生成 `VALID`（有效候选状态），没有生成 entry（入场）/ stop（止损）/ TP（止盈）/ RR（盈亏比），没有升级 Readiness（可执行就绪），没有接自动交易。

## 五、P168 做了什么

P168 是 SourceTrace（证据来源追踪）Dashboard（首页工作台）Display（展示）Scope Gate（范围门）。

P168 已确认：

- 未来可以单独开 P169 做 `dashboard.html` 只读展示。
- P169 只能展示已有 `detail.sourceTrace` 字段。
- P169 禁止改 Controller（控制器）/ API（接口）/ service（服务）/ mapper（映射器）/ schema（数据库结构）/ config（配置）。
- P169 禁止新增交易按钮、可执行状态、交易点位、自动交易。
- P169 只能解释证据来源、缺失项、禁止推进原因和人工复核状态。

P168 没有写代码，没有新增测试，没有改 `dashboard.html`。

## 六、P169 做了什么

P169 是 SourceTrace（证据来源追踪）Dashboard（首页工作台）Read-Only Display（只读展示）。

P169 只修改了：

- `src/main/resources/templates/dashboard.html`

P169 删除了：

- `docs/P169.md`

P169 新增或增强了 SourceTrace（证据来源追踪）只读证据来源追踪展示。页面展示已有 `detail.sourceTrace` 字段，包括：

- `sourceOwner`
- `sourceRef`
- `sourceTimeframe`
- `freshnessStatus`
- `missingFields`
- `blockingReasons`
- `reviewMode`
- `fallbackStatus`
- `manualReviewRequired`
- `notTradeInstruction`

P169 页面文案明确：

- 这是只读证据来源追踪。
- 不是交易指令。
- 需要人工复核。
- `INCOMPLETE`（证据不完整）/ `BLOCKED`（禁止推进）表示不能推进，不是交易机会。

P169 没有修改：

- `DashboardController.java`
- `DashboardDetailResponseVO.java`
- `DefaultDashboardSourceTraceDetailAdapter.java`
- endpoint（接口入口）/ API（接口）。
- service（服务）/ mapper（映射器）/ schema（数据库结构）/ config（配置）。

P169 没有生成 `VALID`（有效候选状态），没有生成 entry（入场）/ stop（止损）/ TP（止盈）/ RR（盈亏比），没有新增买入 / 卖出 / 平仓 / 反手按钮，没有接 order（下单）/ execution（执行）/ scheduler（定时器）/ automation（自动化）/ auto-trading（自动交易）。

## 七、P165-P170 这组完成了什么

P165-P170 完成的是 SourceTrace（证据来源追踪）read-only display（只读展示）闭环。

这一组完成后，SourceTrace（证据来源追踪）已经从 service wrapper（服务包装层）推进到 Dashboard（首页工作台）detail read model（详情只读模型），再推进到 `dashboard.html` 的只读展示。

用户现在可以在首页详情区域看到：

- SourceTrace（证据来源追踪）的来源。
- SourceTrace（证据来源追踪）的缺失项。
- SourceTrace（证据来源追踪）的禁止推进原因。
- SourceTrace（证据来源追踪）的复核模式。
- 是否必须人工复核。
- 是否不是交易指令。

但这只是只读解释层，不是交易计划生成。这只是人工复核材料，不是交易指令。

当前仍然不能做：

- 不能生成真实交易点位。
- 不能生成 `VALID`（有效候选状态）。
- 不能升级 Readiness（可执行就绪）。
- 不能自动交易。

## 八、P170 的结论

P165-P170 这一组完成。

完成的是 SourceTrace（证据来源追踪）read-only display（只读展示）闭环。

它还不是 production candidate generation（生产候选交易计划生成）。

它还不是 source-owned runtime candidate generation（运行时证据来源候选生成）。

它还不是真实 entry（入场）/ stop（止损）/ TP（止盈）/ RR（盈亏比）。

它还不是 ExecutionPlan（执行计划）Readiness（可执行就绪）。

它还不是自动交易。

下一步必须回到 `docs/PROJECT_PROGRESS_INDEX.md` 的推荐路线，不能直接跳到自动交易或真实点位。

## 九、推荐下一步

推荐下一步：

P171：Project Progress Index Refresh After SourceTrace Display（SourceTrace 展示后项目总进度索引刷新）。

P171 应该更新 `docs/PROJECT_PROGRESS_INDEX.md`。

原因：

- P165-P170 已经完成 SourceTrace（证据来源追踪）只读输出和首页只读展示。
- 项目总索引需要把 SourceTrace（证据来源追踪）进度从“未接 dashboard 展示”更新为“已完成只读展示，但未完成生产候选 / 真实点位 / Readiness（可执行就绪）”。
- P171 仍然只改文档，不写代码。

P171 不应接 API（接口），不应改 Dashboard（首页工作台）功能，不应生成交易点位，不应升级 Readiness（可执行就绪），不应接自动交易。

## 十、仍然禁止的路径

以下路径在 P170 之后仍然禁止：

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

这些路径继续禁止，是因为 SourceTrace（证据来源追踪）当前只完成只读展示闭环。它只能帮助人工复核证据来源和阻断原因，不能证明交易计划有效，不能证明执行计划就绪，也不能触发任何交易动作。

## P170 边界确认

- P170 只新增本收口文档。
- P170 删除 `docs/P170.md` placeholder（占位文档）。
- P170 不新增 Java。
- P170 不新增测试。
- P170 不修改 production Java（生产 Java）。
- P170 不修改现有测试。
- P170 不修改 `dashboard.html`。
- P170 不新增 Controller（控制器）/ endpoint（接口入口）/ API（接口）/ schema（数据库结构）/ config（配置）/ service（服务）/ mapper（映射器）。
- P170 不读取 runtime（运行时）/ live（实时）/ external（外部）数据。
- P170 不生成真实 entry（入场）/ stop（止损）/ TP（止盈）/ RR（盈亏比）。
- P170 不调用 `BoundaryCandidateDTO.valid(...)`。
- P170 不升级 ExecutionPlan（执行计划）Readiness（可执行就绪）。
- P170 不接 order（下单）/ execution（执行）/ scheduler（定时器）/ automation（自动化）/ auto-trading（自动交易）。
