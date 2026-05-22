# P178 ExecutionPlan Review-Only Plan Display Authorization Gate（执行计划只允许复核展示授权门）

## 一、这一步是干嘛的

P178 是 ExecutionPlan Review-Only Plan Display Authorization Gate（执行计划只允许复核展示授权门）。

P178 是 P177-P180 ExecutionPlan Review-Only Plan Display Pack（执行计划只允许复核展示包）的第二步。

本轮只做授权门文档：

- 不写 Java。
- 不新增测试。
- 不改 `dashboard.html`。
- 不新增 Controller（控制器）/ endpoint（接口）/ API（接口）。
- 只规定 P179 如果写最小只读计划展示，允许改哪些文件。
- 不生成真实 entry / stop / TP / RR（入场 / 止损 / 止盈 / 盈亏比）。
- 不升级 Readiness（可执行就绪）。
- 不接 order / execution / scheduler / automation / auto-trading（下单 / 执行 / 定时器 / 自动化 / 自动交易）。

P178 的核心作用是给 P179 画清楚边界：可以做 ExecutionPlan（执行计划）Review-Only Plan（只允许复核的计划）Display（展示）的最小补强，但不能让它变成可执行计划、交易指令或自动交易入口。

## 二、P177 审计结论

P177 已完成 ExecutionPlan Review-Only Plan Display Scope Audit（执行计划只允许复核展示范围审计）。P178 继承 P177 的保守结论。

P177 已确认：

- `ExecutionPlanVO` 已存在。
- `ExecutionPlanDO` 已存在。
- `DashboardDetailResponseVO.ExecutionPlanDisplayVO` 已存在。
- `PlanServiceImpl` 已存在。
- `DefaultExecutionPlanDisplayAdapter` 已存在。
- ExecutionPlan（执行计划）当前是 advisory（建议性）/ review-only（只允许复核）/ display（展示）能力，不是 executable plan（可执行计划）。
- `READY_REVIEW_ONLY`（只允许复核的就绪摘要）只是“只允许复核的就绪摘要”，不是 Readiness（可执行就绪）已打开。
- `dashboard.html` 已有执行计划只读展示区域。
- 当前最安全路径是 Dashboard Detail（首页详情）/ ExecutionPlanDisplay（执行计划展示）区域。

P177 也确认：

- 现有 `PlanServiceImpl` 会默认保持 `PLAN_MODE_ADVISORY`（建议性模式）。
- 缺 SourceTrace（证据来源追踪）时保持 `INCOMPLETE`（证据不完整）。
- Risk Action Guard（风险动作保护器）不安全时回落到 `WATCH_ONLY`（仅观察）或 `INCOMPLETE`（证据不完整）。
- display adapter（展示适配器）每条路径都保持 `manualReviewRequired=true`（必须人工复核）和 `notTradeInstruction=true`（不是交易指令）。

因此，P179 可以补强的是“展示边界更清楚”，不能补强成“可执行能力更强”。

## 三、是否允许 P179 写代码

明确结论：可以允许 P179 写最小只读计划展示代码。

但 P179 必须极小：

- 只能围绕 Dashboard Detail（首页详情）/ ExecutionPlanDisplay（执行计划展示）/ adapter（适配器）层。
- 不能新增 action API（动作接口）。
- 不能新增 Controller（控制器）或 endpoint（接口）。
- 不能生成真实 entry / stop / TP / RR（入场 / 止损 / 止盈 / 盈亏比）。
- 不能升级 Readiness（可执行就绪）。
- 不能新增买入 / 卖出 / 平仓 / 反手按钮。
- 不能接 order / execution / scheduler / automation / auto-trading（下单 / 执行 / 定时器 / 自动化 / 自动交易）。

P179 的允许目标只有一个：让 ExecutionPlanDisplay（执行计划展示）更清楚地表达 review-only（只允许复核）、not executable（不可执行）、manual review required（必须人工复核）、not trade instruction（不是交易指令）。

## 四、P179 允许改哪些文件

P179 最多允许改 1-3 个文件。

保守授权范围如下：

1. `src/main/java/org/example/trademodel/service/dashboard/DefaultExecutionPlanDisplayAdapter.java`
2. `src/test/java/org/example/trademodel/service/dashboard/DefaultExecutionPlanDisplayAdapterTest.java`
3. 仅在必要时允许：`src/main/java/org/example/trademodel/vo/DashboardDetailResponseVO.java`

限制说明：

- 如果 `ExecutionPlanDisplayVO` 已有足够字段，就默认不改 `DashboardDetailResponseVO.java`。
- 默认不允许改 `src/main/resources/templates/dashboard.html`。
- 默认不允许改 `src/main/java/org/example/trademodel/controller/DashboardController.java`。
- 默认不允许改 `src/main/java/org/example/trademodel/service/impl/PlanServiceImpl.java`。
- 默认不允许改 `src/main/java/org/example/trademodel/vo/ExecutionPlanVO.java`。
- 默认不允许改 `src/main/java/org/example/trademodel/dto/planboundary/BoundaryCandidateDTO.java`。
- 默认不允许改 `src/main/java/org/example/trademodel/service/impl/BoundaryCandidateServiceImpl.java`。
- 不允许改 API（接口）/ endpoint（接口）/ mapper（映射）/ schema（数据库结构）/ config（配置）。
- 不允许新增任何 order / execution / auto-trading（下单 / 执行 / 自动交易）字段或按钮。

如果 P179 发现上述 1-3 个文件无法完成最小只读展示补强，必须停止并另开授权，不能在 P179 中自行扩大范围。

## 五、P179 允许做什么

P179 只能做以下事情：

- 让 ExecutionPlanDisplay（执行计划展示）更明确展示 review-only（只允许复核）状态。
- 展示 plan summary（计划摘要）。
- 展示 incomplete reasons（不完整原因）。
- 展示 blocking / not executable reason（阻断 / 不可执行原因）。
- 展示 `manualReviewRequired`（必须人工复核）。
- 展示 `notTradeInstruction`（不是交易指令）。
- 把 `READY_REVIEW_ONLY`（只允许复核的就绪摘要）继续解释为“只允许复核的摘要”，不是可执行。
- 保持 REVIEW_ONLY（只允许复核）/ INCOMPLETE（证据不完整）/ BLOCKED（禁止推进）/ WATCH_ONLY（仅观察）的安全语义。
- 只解释“为什么不能执行”或“为什么只能复核”。
- 不生成交易指令。
- 不生成 Readiness（可执行就绪）。
- 不生成真实 entry / stop / TP / RR（入场 / 止损 / 止盈 / 盈亏比）。

推荐 P179 的实现方向：

- 优先补强 `DefaultExecutionPlanDisplayAdapter` 对安全状态的映射。
- 优先补测试证明 `READY_REVIEW_ONLY`（只允许复核的就绪摘要）不会变成可执行。
- 优先补测试证明 `INCOMPLETE`（证据不完整）、`WATCH_ONLY`（仅观察）和阻断原因都会保持 fail-closed（失败关闭）。
- 优先证明 `manualReviewRequired=true` 和 `notTradeInstruction=true` 在所有路径保持开启。

## 六、P179 禁止做什么

P179 禁止：

- 不允许改 `dashboard.html`。
- 不允许新增 Controller（控制器）/ endpoint（接口）/ API（接口）。
- 不允许改 summary（摘要）接口。
- 不允许新增 mapper（映射）。
- 不允许修改 schema（数据库结构）。
- 不允许修改 config（配置）。
- 不允许读取真实 runtime data（运行时数据）。
- 不允许读取 live market data（实时行情）。
- 不允许读取 external data（外部数据）。
- 不允许调用 `BoundaryCandidateDTO.valid(...)` 进入生产路径。
- 不允许生成 production VALID（生产环境有效候选状态）。
- 不允许生成 entry / stop / TP / RR（入场 / 止损 / 止盈 / 盈亏比）。
- 不允许升级 ExecutionPlan Readiness（执行计划可执行就绪）。
- 不允许新增买入 / 卖出 / 平仓 / 反手按钮。
- 不允许接 order / execution / scheduler / automation / auto-trading（下单 / 执行 / 定时器 / 自动化 / 自动交易）。

P179 也不允许通过文案、字段名或状态名暗示“可执行”“可下单”“立即开仓”“立即平仓”“反手”“自动执行”。

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

P179：ExecutionPlan Review-Only Plan Display Minimal Wiring（执行计划只允许复核展示最小接线）。

中文解释：

- P179 才可以开始最小只读计划展示代码。
- P179 只能做 Dashboard Detail（首页详情）/ ExecutionPlanDisplay（执行计划展示）/ adapter（适配器）层。
- P179 不能改 `dashboard.html`。
- P179 不能接 action API（动作接口）。
- P179 不能生成交易点位。
- P179 不能升级 Readiness（可执行就绪）。
- P179 不能自动交易。

## 九、P178 结论

P178 只完成 ExecutionPlan Review-Only Plan Display Authorization Gate（执行计划只允许复核展示授权门）。

P178 授权 P179 可以做极小代码变更，但范围只限：

- `DefaultExecutionPlanDisplayAdapter.java`
- `DefaultExecutionPlanDisplayAdapterTest.java`
- 仅在必要时才允许 `DashboardDetailResponseVO.java`

P178 不写 Java，不新增测试，不改 dashboard.html，不接 API，不生成 entry / stop / TP / RR（入场 / 止损 / 止盈 / 盈亏比），不调用 `BoundaryCandidateDTO.valid(...)` 进入生产路径，不升级 ExecutionPlan Readiness（执行计划可执行就绪），不接自动交易。
