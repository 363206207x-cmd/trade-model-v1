# PHASE: DashboardDetailResponseVO Java 最小扩展前最终收口（PR20）

## 1. 阶段目标

本阶段目标是对 Dashboard detail read model 的方案链路做 Java 实现前最终收口，确认后续可以进入 `DashboardDetailResponseVO` Java 最小扩展阶段。

本 PR 只做收口文档，不实现 Java 代码、不改 dashboard、不改 schema、不新增接口、不接 order API、不自动交易。

核心目标：

- 确认 PR #13–#19 已完成 Java 最小扩展前置方案链路。
- 明确后续 Java 最小实现的文件范围、字段范围、安全默认值和测试要求。
- 明确本阶段仍不接真实 PlanBoundary / ExecutionPlan / Risk Action Guard 后端业务逻辑。
- 防止 Java 实现阶段越界到交易执行、order API 或自动交易。

---

## 2. 已完成前置文档链路

当前已完成以下文档与审计：

- PR #13：Dashboard detail 展示字段契约方案。
- PR #14：Dashboard detail 字段契约与现有 VO 对照审计。
- PR #15：DashboardDetailResponseVO 最小扩展方案。
- PR #16：PlanBoundaryDisplayVO 最小对象方案。
- PR #17：ExecutionPlanDisplayVO 最小对象方案。
- PR #18：RiskActionGuardDisplayVO 最小对象方案。
- PR #19：PaperObservationDisplayVO 最小对象方案。

结论：

- 已具备进入 Java 最小扩展的方案依据。
- Java 实现目标应保持为 display/read model 层，不进入真实交易逻辑。

---

## 3. 当前事实边界

当前 `DashboardDetailResponseVO` 已有字段：

- `symbol`
- `decision`
- `marketEnvironmentMini`
- `evidenceTopItems`
- `scoreTopItems`

当前 detail 主要通过 `DecisionResultVO` 间接提供：

- `analysisId`
- `marketBiasHierarchy`
- `confidenceLevel`
- `riskLevel`
- `isWorthOpening`
- `executionPlanSummary`
- `recommendedAction`
- `entryZone`
- `stopLoss`
- `takeProfitRules`
- `readModelTruthStatus`
- `readModelFallbackReason`

关键结论：

- `entryZone` / `stopLoss` / `takeProfitRules` 仍是文本字段，不是结构化 PlanBoundary 真值。
- 当前仍缺少独立的 PlanBoundary / ExecutionPlanDisplay / RiskActionGuard / PaperObservation display 对象。

---

## 4. 后续 Java 最小实现建议范围

建议后续 Java PR 只允许修改：

- `src/main/java/org/example/trademodel/vo/DashboardDetailResponseVO.java`
- 新增或修改与其直接相关的最小 VO 测试文件，例如：
  - `src/test/java/org/example/trademodel/vo/DashboardDetailResponseVOTest.java`

若项目已有对应测试位置，应复用现有测试包结构。

首个 Java 最小实现不建议修改：

- `DashboardController.java`
- `DecisionResultVO.java`
- `ExecutionPlanVO.java`
- mapper
- service
- schema
- dashboard.html

---

## 5. 建议新增 display 字段

后续 Java 最小实现建议在 `DashboardDetailResponseVO` 中新增以下字段：

- `PlanBoundaryDisplayVO planBoundaryDisplay`
- `ExecutionPlanDisplayVO executionPlanDisplay`
- `RiskActionGuardDisplayVO riskActionGuardDisplay`
- `PaperObservationDisplayVO paperObservationDisplay`

建议实现方式：

- 可作为 `DashboardDetailResponseVO` 的内部静态类。
- 或独立 VO 类，但本阶段更推荐内部静态类，减少文件扩散。
- 必须提供 getter / setter。
- 必须保持现有字段不变，向后兼容。

---

## 6. PlanBoundaryDisplayVO 最小字段

建议第一阶段字段：

- `planBoundaryStatus`
- `planBoundaryStatusLabel`
- `sourceTraceStatus`
- `backendConnectionStatus`
- `incompleteReasons`
- `blockingReasons`
- `manualReviewRequired`
- `notTradeInstruction`
- `updatedAt`

安全默认值：

- `planBoundaryStatus = BACKEND_PENDING`
- `planBoundaryStatusLabel = 后端未接入`
- `sourceTraceStatus = BACKEND_PENDING`
- `backendConnectionStatus = BACKEND_PENDING`
- `manualReviewRequired = true`
- `notTradeInstruction = true`
- `incompleteReasons` 不为 null。
- `blockingReasons` 不为 null。

首阶段不得包含：

- entry price
- stop price
- take profit price
- RR 数值
- orderId
- autoExecute
- autoTrade

---

## 7. ExecutionPlanDisplayVO 最小字段

建议第一阶段字段：

- `executionPlanStatus`
- `executionPlanStatusLabel`
- `executionPlanBoundaryAligned`
- `planBoundaryStatus`
- `executionPlanSummary`
- `notExecutableReason`
- `incompleteReasons`
- `manualReviewRequired`
- `notTradeInstruction`
- `updatedAt`

安全默认值：

- `executionPlanStatus = BOUNDARY_PENDING`
- `executionPlanStatusLabel = 等待边界接入`
- `executionPlanBoundaryAligned = false`
- `planBoundaryStatus = BACKEND_PENDING`
- `notExecutableReason = PLAN_BOUNDARY_BACKEND_PENDING`
- `manualReviewRequired = true`
- `notTradeInstruction = true`
- `incompleteReasons` 不为 null。

原则：

- `executionPlanSummary` 只能作为文本摘要。
- 不得因为有摘要就判定为可执行计划。
- ExecutionPlan 必须受 PlanBoundary 状态约束。

---

## 8. RiskActionGuardDisplayVO 最小字段

建议第一阶段字段：

- `riskActionGuardStatus`
- `riskActionGuardStatusLabel`
- `riskActionAdvice`
- `riskActionBlockingReason`
- `liquidityState`
- `stampedeDetected`
- `wickOnlyRisk`
- `opportunityPushAllowed`
- `reverseTradeAllowed`
- `newPositionAllowed`
- `marketOrderExitAllowed`
- `manualRiskReviewRequired`
- `notTradeInstruction`
- `updatedAt`

安全默认值：

- `riskActionGuardStatus = BACKEND_PENDING`
- `riskActionGuardStatusLabel = 后端未接入`
- `liquidityState = BACKEND_PENDING`
- `stampedeDetected = false`
- `wickOnlyRisk = false`
- `opportunityPushAllowed = false`
- `reverseTradeAllowed = false`
- `newPositionAllowed = false`
- `marketOrderExitAllowed = false`
- `manualRiskReviewRequired = true`
- `notTradeInstruction = true`

硬约束：

- 踩踏状态禁止反手、新开仓、机会推送。
- 流动性恶化不建议市价一次性砍仓。
- 短线插针不等于趋势反转。

---

## 9. PaperObservationDisplayVO 最小字段

建议第一阶段字段：

- `paperObservationStatus`
- `paperObservationStatusLabel`
- `paperObservationAvailable`
- `manualReviewEntryAvailable`
- `linkedPaperObservationCount`
- `linkedReviewCount`
- `missedOpportunityFlag`
- `reviewSummary`
- `notRealPosition`
- `notTradeInstruction`
- `manualReviewRequired`
- `backendConnectionStatus`
- `updatedAt`

安全默认值：

- `paperObservationStatus = BACKEND_PENDING`
- `paperObservationStatusLabel = 后端未接入`
- `paperObservationAvailable = false`
- `manualReviewEntryAvailable = false`
- `linkedPaperObservationCount = 0`
- `linkedReviewCount = 0`
- `missedOpportunityFlag = false`
- `notRealPosition = true`
- `notTradeInstruction = true`
- `manualReviewRequired = true`
- `backendConnectionStatus = BACKEND_PENDING`

原则：

- 纸面观察不是实盘交易。
- 人工复盘不代表真实持仓。
- 纸面记录不得自动同步为真实持仓。

---

## 10. Java 实现阶段测试要求

后续 Java PR 至少应测试：

- 新增 display 字段 getter / setter。
- 四个 display VO 的安全默认值。
- `manualReviewRequired` 默认 true。
- `notTradeInstruction` 默认 true。
- `PlanBoundaryDisplayVO` 不包含 entry / stop / TP 数值。
- `ExecutionPlanDisplayVO.executionPlanBoundaryAligned` 默认 false。
- `RiskActionGuardDisplayVO` 的 allow flags 默认 false。
- `PaperObservationDisplayVO.notRealPosition` 默认 true。
- 不存在 order / autoTrade / autoExecute 字段。

---

## 11. 明确不做什么

Java 最小扩展阶段仍不做：

- 不接 order API。
- 不自动交易。
- 不自动开仓 / 平仓 / 反手。
- 不新增数据库表。
- 不新增后端接口。
- 不改 mapper。
- 不改 service 业务逻辑。
- 不改 RuleEngine。
- 不改 source assembler。
- 不接真实 PlanBoundary 生产写入。
- 不生成真实 entry / stop / TP。
- 不改 dashboard.html。

---

## 12. 建议后续 PR 顺序

建议后续按以下小 PR 推进：

1. PR #21：`DashboardDetailResponseVO` Java 最小扩展，只新增 display VO 内部类与字段。
2. PR #22：`DashboardDetailResponseVO` 安全默认 factory / helper 方案或实现。
3. PR #23：`DashboardController` detail 组装 BACKEND_PENDING 默认 display。
4. PR #24：dashboard 读取真实 display status 字段，而不是写死占位。
5. PR #25：smoke 验证与截图验收。

---

## 13. 验收标准

本 PR 验收标准：

- 只新增一个收口文档：`docs/PHASE_DASHBOARD_DETAIL_RESPONSE_VO_JAVA_EXTENSION_READINESS.md`。
- 无 Java 代码改动。
- 无 dashboard 改动。
- 无 schema 改动。
- 无 order API / 自动交易相关改动。
- 文档明确 Java 最小扩展范围。
- 文档明确四个 display VO 的安全默认值。
- 文档明确后续进入 Java 实现时仍禁止交易执行语义。
