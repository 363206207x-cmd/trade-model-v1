# P177 ExecutionPlan Review-Only Plan Display Scope Audit（执行计划只允许复核展示范围审计）

## 一、这一步是干嘛的

P177 是 ExecutionPlan Review-Only Plan Display Scope Audit（执行计划只允许复核展示范围审计）。

P177 是 P177-P180 ExecutionPlan Review-Only Plan Display Pack（执行计划只允许复核展示包）的第一步。

本轮只做范围审计：

- 不写 Java。
- 不新增测试。
- 不接 Dashboard（首页工作台）新功能。
- 不新增 Controller（控制器）/ endpoint（接口）/ API（接口）。
- 只判断未来 ExecutionPlan（执行计划）是否可以进入 Review-Only Plan（只允许复核的计划）Display（展示）路径。
- 不生成真实 entry / stop / TP / RR（入场 / 止损 / 止盈 / 盈亏比）。
- 不升级 Readiness（可执行就绪）。
- 不接 order / execution / scheduler / automation / auto-trading（下单 / 执行 / 定时器 / 自动化 / 自动交易）。

P177 的核心结论是：可以继续审计 ExecutionPlan（执行计划）的只读展示路径，但不能把现有展示、摘要、状态或 READY_REVIEW_ONLY（只允许复核的就绪摘要）解释成可执行计划。

## 二、P176 / PROJECT_PROGRESS_INDEX 的依据

P177 以 `docs/PROJECT_PROGRESS_INDEX.md` 为总索引依据。

P176 已刷新项目总索引，确认：

- SourceTrace（证据来源追踪）只读输出和 Dashboard（首页工作台）只读展示已经完成。
- BoundaryCandidate（边界候选交易计划）read-only candidate display（只读候选展示）已经完成。
- `PROJECT_PROGRESS_INDEX.md` 推荐个人可用最快路径进入 ExecutionPlan Review-Only Plan Display（执行计划只允许复核展示）。
- 同时，`PROJECT_PROGRESS_INDEX.md` 仍明确暂停 ExecutionPlan Readiness（执行计划可执行就绪）。
- 真实 entry / stop / TP / RR（入场 / 止损 / 止盈 / 盈亏比）仍未闭环。
- production VALID（生产环境有效候选状态）仍未授权。
- 自动交易仍然禁止。

因此 P177 只能审计 review-only display（只允许复核展示），不能审计 executable plan（可执行计划），也不能审计 auto-trading（自动交易）。

## 三、当前 ExecutionPlan 能力

只读扫描后，当前 ExecutionPlan（执行计划）相关能力如下。

### 已存在的对象

- `src/main/java/org/example/trademodel/vo/ExecutionPlanVO.java` 已存在。
- `src/main/java/org/example/trademodel/entity/ExecutionPlanDO.java` 已存在。
- `src/main/java/org/example/trademodel/vo/DashboardDetailResponseVO.java` 内已存在 `ExecutionPlanDisplayVO`。
- `src/main/java/org/example/trademodel/service/PlanService.java` 已存在。
- `src/main/java/org/example/trademodel/service/impl/PlanServiceImpl.java` 已存在。
- `src/main/java/org/example/trademodel/service/dashboard/ExecutionPlanDisplayAdapter.java` 已存在。
- `src/main/java/org/example/trademodel/service/dashboard/DefaultExecutionPlanDisplayAdapter.java` 已存在。

### 已存在的保护语义

`ExecutionPlanVO` 里已有这些只读 / 保护语义：

- `PLAN_MODE_ADVISORY`：ADVISORY（建议性 / 仅供复核）。
- `READINESS_INCOMPLETE`：INCOMPLETE（证据不完整）。
- `READINESS_WATCH_ONLY`：WATCH_ONLY（仅观察）。
- `READINESS_READY_REVIEW_ONLY`：READY_REVIEW_ONLY（只允许复核的就绪摘要）。
- `manualReviewRequired=true`：必须人工复核。
- `notTradeInstruction=true`：不是交易指令。

`DefaultExecutionPlanDisplayAdapter` 里已有这些展示状态：

- `BACKEND_PENDING`：后端未接入。
- `BOUNDARY_PENDING`：边界未接入。
- `INCOMPLETE`（证据不完整）。
- `WATCH_ONLY`（仅观察）。
- `INVALID`（失效）。
- `VALID`（有效候选状态），只作为 PlanBoundary（计划边界）的输入状态读取。
- `READY_REVIEW_ONLY`（只允许复核的就绪摘要）。

当前未看到 ExecutionPlanDisplay（执行计划展示）直接把 `BLOCKED`（禁止推进）作为最终状态输出；当前阻断主要通过 `WATCH_ONLY`（仅观察）、`INCOMPLETE`（证据不完整）、`notExecutableReason`（不可执行原因）和 `incompleteReasons`（不完整原因）表达。

### 已存在的 service 行为

`PlanServiceImpl` 已能生成 `ExecutionPlanVO`，但当前更像 advisory（建议性）/ display（展示）/ text summary（文本摘要）能力，不是可执行计划：

- 默认 `recommendedAction` 是“观望”。
- `entryZone` 默认“暂无”。
- `stopLoss` 默认“暂无”。
- `takeProfitRules` 默认“暂无”。
- 只要缺 SourceTrace（证据来源追踪），就保持 `INCOMPLETE`（证据不完整）。
- SourceTrace（证据来源追踪）完整时，也只是 `READY_REVIEW_ONLY`（只允许复核的就绪摘要），同时保持 `manualReviewRequired=true` 和 `notTradeInstruction=true`。
- Risk Action Guard（风险动作保护器）未就绪、高风险、踩踏、插针或动作标志出现时，会降为 `INCOMPLETE`（证据不完整）或 `WATCH_ONLY`（仅观察）。

这说明当前 ExecutionPlan（执行计划）已有只读计划壳和保护状态，但没有完成真实生产候选、真实点位、可执行就绪或自动交易。

### 已存在的测试证据

`src/test/java/org/example/trademodel/service/dashboard/DefaultExecutionPlanDisplayAdapterTest.java` 已覆盖：

- 输入缺失时保持 `BOUNDARY_PENDING`。
- `INCOMPLETE`（证据不完整）会继承不完整原因。
- `WATCH_ONLY`（仅观察）保持只读观察。
- `INVALID`（失效）不会推进。
- PlanBoundary（计划边界）为 `VALID`（有效候选状态）时最多映射为 `READY_REVIEW_ONLY`（只允许复核的就绪摘要）。
- SourceTrace（证据来源追踪）缺失、不完整、WATCH_ONLY（仅观察）或 fail-closed（失败关闭）时不会升级。
- runtime kline（运行时 K 线）单独存在不能升级 ExecutionPlan（执行计划）。
- Risk Action Guard（风险动作保护器）遇到高风险、踩踏、流动性缺失、插针或动作标志时会回落到 `WATCH_ONLY`（仅观察）。
- 每条路径都保持 `manualReviewRequired=true` 和 `notTradeInstruction=true`。

`src/test/java/org/example/trademodel/service/impl/PlanServiceImplTest.java` 已覆盖：

- 缺 SourceTrace（证据来源追踪）时保持 `PLAN_MODE_ADVISORY` 和 `INCOMPLETE`（证据不完整）。
- SourceTrace（证据来源追踪）完整时也只是 `READY_REVIEW_ONLY`（只允许复核的就绪摘要），不是交易指令。
- Risk Action Guard（风险动作保护器）不安全时回落 `WATCH_ONLY`（仅观察）。
- `manualReviewRequired=true` 和 `notTradeInstruction=true` 保持开启。

这些测试证明当前 ExecutionPlan（执行计划）不能被理解为自动执行路径；它只是人工复核材料。

## 四、当前 Dashboard / display 现状

只读扫描后，当前 Dashboard（首页工作台）和 display（展示）现状如下。

### DashboardDetailResponseVO

`DashboardDetailResponseVO` 已有：

- `executionPlanDisplay` 字段。
- `planBoundaryDisplay` 字段。
- `sourceTrace` 字段。
- `riskActionGuardDisplay` 字段。

其中 `ExecutionPlanDisplayVO` 已有：

- `executionPlanStatus`：执行计划展示状态。
- `executionPlanStatusLabel`：执行计划状态文案。
- `executionPlanBoundaryAligned`：是否与计划边界对齐。
- `planBoundaryStatus`：计划边界状态。
- `executionPlanSummary`：执行计划摘要。
- `notExecutableReason`：不可执行原因。
- `incompleteReasons`：不完整原因。
- `manualReviewRequired`：必须人工复核。
- `notTradeInstruction`：不是交易指令。

默认值仍是 fail-closed（失败关闭）：`BOUNDARY_PENDING` / `BACKEND_PENDING` / `PLAN_BOUNDARY_BACKEND_PENDING`，并且默认要求人工复核，不是交易指令。

### dashboard.html

`src/main/resources/templates/dashboard.html` 已有执行计划展示区域：

- PlanBoundary（计划边界）状态卡里读取 `detail.executionPlanDisplay`。
- 决策壳里的“执行建议”读取 ExecutionPlan（执行计划）展示状态、状态标签、不可执行原因、建议摘要和 RiskActionGuard（风险动作保护器）。
- 页面文案已明确 ExecutionPlan（执行计划）是 review-only / advisory（只允许复核 / 建议性）壳展示。
- 页面已有“不生成真实 entry / stop / take-profit 数值，不连接 order API”的说明。
- 页面已有“ExecutionPlan readiness 不自动执行”的说明。

当前最安全展示路径是 Dashboard Detail（首页详情）read-only display（只读展示），不是 summary（摘要接口），不是 action API（动作接口），也不是 Dashboard（首页工作台）可执行状态。

### 当前风险点

`READY_REVIEW_ONLY`（只允许复核的就绪摘要）里含有 READY 字样，容易被误读为可以执行。后续 P178 / P179 必须继续把它解释为“可复核摘要”，不能解释为 Readiness（可执行就绪）已打开。

## 五、是否允许未来进入只读计划展示

保守结论：

- P177 不允许直接写代码。
- 可以允许未来 P178 做 ExecutionPlan Review-Only Plan Display Authorization Gate（执行计划只允许复核展示授权门）。
- P178 必须明确 P179 最多允许改哪些文件。
- P179 如果实现，也只能做 review-only display（只允许复核展示）。
- P179 不能生成真实 entry / stop / TP / RR（入场 / 止损 / 止盈 / 盈亏比）。
- P179 不能升级 Readiness（可执行就绪）。
- P179 不能新增交易动作按钮。
- P179 不能接 order / execution / scheduler / automation / auto-trading（下单 / 执行 / 定时器 / 自动化 / 自动交易）。

未来允许推进的边界只有：把现有 ExecutionPlanDisplay（执行计划展示）表达得更清楚、更保守、更不容易误读。

## 六、未来 P178 / P179 最安全方向

P178 / P179 的最安全方向如下：

- 优先展示 ExecutionPlan（执行计划）的 review-only（只允许复核）状态。
- 优先展示计划摘要、缺失原因、阻断原因、人工复核模式。
- 优先复用现有 Dashboard Detail（首页详情）/ ExecutionPlanDisplay（执行计划展示）区域。
- 不先改 summary（摘要接口）。
- 不新增 action API（动作接口）。
- 不新增交易按钮。
- 如果现有 display DTO（展示对象）足够，只授权最小字段展示或文案补强。
- 如果现有对象不够，先定义 review-only display DTO（只允许复核展示对象）或 adapter（适配器），但必须另行授权。
- 保持 REVIEW_ONLY（只允许复核）/ INCOMPLETE（证据不完整）/ BLOCKED（禁止推进）/ WATCH_ONLY（仅观察）的安全语义。
- 不把 `READY_REVIEW_ONLY`（只允许复核的就绪摘要）映射成可执行 Readiness（可执行就绪）。
- 不把真实 entry / stop / TP / RR（入场 / 止损 / 止盈 / 盈亏比）展示成可执行点位。

如果 P179 写代码，建议优先围绕这些文件评估授权：

- `src/main/java/org/example/trademodel/service/dashboard/DefaultExecutionPlanDisplayAdapter.java`
- `src/test/java/org/example/trademodel/service/dashboard/DefaultExecutionPlanDisplayAdapterTest.java`
- 仅在字段确实不足时，再评估 `src/main/java/org/example/trademodel/vo/DashboardDetailResponseVO.java`

默认不应该改：

- `src/main/resources/templates/dashboard.html`
- `src/main/java/org/example/trademodel/controller/DashboardController.java`
- summary（摘要）接口相关文件
- schema（数据库结构）
- config（配置）
- mapper（映射）
- order / execution / scheduler / automation / auto-trading（下单 / 执行 / 定时器 / 自动化 / 自动交易）相关文件

## 七、仍然禁止的路径

以下路径仍然禁止：

- production candidate generation（生产候选交易计划生成）。
- source-owned runtime candidate generation（运行时证据来源候选生成）。
- real entry / stop / TP / RR value generation（真实入场 / 止损 / 止盈 / 盈亏比生成）。
- production VALID mapping（生产环境映射为有效候选）。
- BoundaryCandidateDTO.valid(...) production calls（生产环境调用 valid 工厂）。
- ExecutionPlan readiness upgrade（执行计划升级为可执行）。
- dashboard readiness mutation（页面显示可执行状态）。
- dashboard trading action buttons（页面交易动作按钮）。
- controller / endpoint / API action wiring（控制器 / 接口动作接线）。
- schema / config / mapper changes（数据库 / 配置 / 映射改动）。
- runtime data reads（读取运行时数据）。
- live market data reads（读取实时行情）。
- external data integration（接外部数据）。
- WebClient / RestTemplate（网络请求工具）。
- order API（下单接口）。
- execution API（执行接口）。
- scheduler / automation / auto-trading（定时器 / 自动化 / 自动交易）。

## 八、推荐下一步

推荐下一步为：

P178：ExecutionPlan Review-Only Plan Display Authorization Gate（执行计划只允许复核展示授权门）。

P178 仍然不写代码。P178 只定义 P179 最小只读计划展示允许改哪些文件。P178 必须继续禁止 Readiness（可执行就绪）、真实 entry / stop / TP / RR（入场 / 止损 / 止盈 / 盈亏比）、交易按钮和自动交易。

## 九、P177 结论

P177 只完成 ExecutionPlan Review-Only Plan Display Scope Audit（执行计划只允许复核展示范围审计）。

当前可以确认：

- ExecutionPlan（执行计划）已有 VO / DO / service / display adapter / 测试基础。
- Dashboard Detail（首页详情）已有 `executionPlanDisplay` 展示对象。
- dashboard.html 已有执行计划只读展示区域。
- 现有能力只能作为 review-only（只允许复核）和 advisory（建议性）材料。
- 现有能力不是 production candidate generation（生产候选交易计划生成）。
- 现有能力不是真实 entry / stop / TP / RR（入场 / 止损 / 止盈 / 盈亏比）。
- 现有能力不是 Readiness（可执行就绪）升级。
- 现有能力不是自动交易。

P177 不新增 Java，不新增测试，不改 dashboard.html，不接 API，不生成交易点位，不调用 `BoundaryCandidateDTO.valid(...)` 进入生产路径，不升级 ExecutionPlan Readiness（执行计划可执行就绪），不接自动交易。

## 十、本轮只读扫描范围

本轮只读扫描了：

- `docs/PROJECT_PROGRESS_INDEX.md`
- `docs/PHASE_BACKEND_P172_BOUNDARY_CANDIDATE_READ_ONLY_DISPLAY_SCOPE_AUDIT.md`
- `docs/PHASE_BACKEND_P173_BOUNDARY_CANDIDATE_READ_ONLY_DISPLAY_AUTHORIZATION_GATE.md`
- `docs/PHASE_BACKEND_P175_BOUNDARY_CANDIDATE_READ_ONLY_DISPLAY_CLOSURE.md`
- `src/main/java/org/example/trademodel/vo/ExecutionPlanVO.java`
- `src/main/java/org/example/trademodel/entity/ExecutionPlanDO.java`
- `src/main/java/org/example/trademodel/vo/DashboardDetailResponseVO.java`
- `src/main/java/org/example/trademodel/service/PlanService.java`
- `src/main/java/org/example/trademodel/service/impl/PlanServiceImpl.java`
- `src/main/java/org/example/trademodel/service/dashboard/ExecutionPlanDisplayAdapter.java`
- `src/main/java/org/example/trademodel/service/dashboard/DefaultExecutionPlanDisplayAdapter.java`
- `src/test/java/org/example/trademodel/service/dashboard/DefaultExecutionPlanDisplayAdapterTest.java`
- `src/test/java/org/example/trademodel/service/impl/PlanServiceImplTest.java`
- `src/main/resources/templates/dashboard.html`
- docs / main java / test java 中 ExecutionPlan（执行计划）、PlanBoundary（计划边界）、SourceTrace（证据来源追踪）、BoundaryCandidate（边界候选交易计划）、readiness（可执行就绪）、entry / stop / TP / RR（入场 / 止损 / 止盈 / 盈亏比）相关只读搜索结果。
