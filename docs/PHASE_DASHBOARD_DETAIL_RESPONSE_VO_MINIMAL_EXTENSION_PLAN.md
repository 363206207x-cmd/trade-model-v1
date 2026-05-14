# PHASE: DashboardDetailResponseVO 最小扩展方案（PR15）

## 1. 阶段目标

本阶段目标是定义 `DashboardDetailResponseVO` 的最小扩展方案，为后续在 `/api/dashboard/detail` 中接入 PlanBoundary、ExecutionPlan 对齐状态、Risk Action Guard、纸面观察 / 人工复盘入口等 read model 字段做准备。

本 PR 只做方案文档，不实现 Java 代码、不改 dashboard、不改 schema、不新增接口、不接 order API、不自动交易。

核心目标：

- 在不破坏现有 `/api/dashboard/detail` 返回结构的前提下，定义最小新增 display VO 结构。
- 优先补状态、缺失原因、人工复核、安全边界，而不是直接补交易价格。
- 避免将当前 `DecisionResultVO.entryZone / stopLoss / takeProfitRules` 文本字段误包装为结构化 PlanBoundary 真值。
- 为后续 Java 实现拆分小 PR 提供边界。

---

## 2. 当前事实

当前 `DashboardDetailResponseVO` 已有字段：

- `symbol`
- `decision`
- `marketEnvironmentMini`
- `evidenceTopItems`
- `scoreTopItems`

当前 detail 核心交易字段主要来自 `decision: DecisionResultVO`，其中已有：

- 决策基础字段：`analysisId`、`symbol`、`marketBiasHierarchy`、`confidenceLevel`、`riskLevel`、`isWorthOpening` 等。
- 执行计划文本字段：`executionPlanSummary`、`recommendedAction`、`planMode`、`entryZone`、`stopLoss`、`takeProfitRules` 等。
- 持仓字段：`hasOpenPosition`、`positionSide`、`avgOpenPrice`、`unrealizedPnlPct` 等。
- read model 完整性字段：`readModelTruthStatus`、`readModelFallbackReason`。

但当前缺少独立结构化对象：

- `planBoundaryDisplay`
- `executionPlanDisplay`
- `riskActionGuardDisplay`
- `paperObservationDisplay`

---

## 3. 最小扩展原则

后续 Java 实现必须遵守：

1. **向后兼容**
   - 不删除现有字段。
   - 不改变 `decision` 语义。
   - 新增字段允许为 null，但前端必须能安全兜底。

2. **先状态，后数值**
   - 第一阶段只补 status / reason / flags。
   - 不直接补 entry / stop / TP 数值，除非 source trace 已接入。

3. **先 read model，后业务逻辑**
   - 本阶段只定义展示对象。
   - 不实现 RuleEngine、source assembler、真实 PlanBoundary 生产写入。

4. **安全默认**
   - `manualReviewRequired = true`。
   - `notTradeInstruction = true`。
   - `backendConnectionStatus = BACKEND_PENDING` 或 `PLACEHOLDER_ONLY`，直到真实后端接入。

5. **禁止自动交易语义**
   - 不新增任何 orderId、orderRequest、autoExecute、autoTrade 字段。

---

## 4. 建议新增字段

建议在 `DashboardDetailResponseVO` 中逐步新增以下顶层字段：

- `PlanBoundaryDisplayVO planBoundaryDisplay`
- `ExecutionPlanDisplayVO executionPlanDisplay`
- `RiskActionGuardDisplayVO riskActionGuardDisplay`
- `PaperObservationDisplayVO paperObservationDisplay`

建议实现顺序：

1. 先新增内部静态类 / 独立 VO 方案文档。
2. 再做 Java DTO / VO 最小实现。
3. 再由 Controller 组装 BACKEND_PENDING / PLACEHOLDER_ONLY 默认值。
4. 最后逐步接真实后端字段。

---

## 5. PlanBoundaryDisplayVO 最小结构

建议字段：

- `planBoundaryStatus`
- `planBoundaryStatusLabel`
- `sourceTraceStatus`
- `backendConnectionStatus`
- `incompleteReasons`
- `blockingReasons`
- `manualReviewRequired`
- `notTradeInstruction`
- `updatedAt`

建议初始默认：

- `planBoundaryStatus = BACKEND_PENDING`
- `sourceTraceStatus = BACKEND_PENDING`
- `backendConnectionStatus = BACKEND_PENDING`
- `manualReviewRequired = true`
- `notTradeInstruction = true`

说明：

- 第一阶段不放 entry / stop / TP 数值。
- 如果后续需要数值，必须单独接 source trace 后再加入。

---

## 6. ExecutionPlanDisplayVO 最小结构

建议字段：

- `executionPlanStatus`
- `executionPlanBoundaryAligned`
- `executionPlanSummary`
- `notExecutableReason`
- `incompleteReasons`
- `manualReviewRequired`
- `notTradeInstruction`

建议初始默认：

- `executionPlanStatus = BOUNDARY_PENDING`
- `executionPlanBoundaryAligned = false`
- `notExecutableReason = PlanBoundary not VALID or backend pending`
- `manualReviewRequired = true`
- `notTradeInstruction = true`

展示原则：

- 可以复用 `decision.executionPlanSummary` 作为摘要。
- 不得因为有 `executionPlanSummary` 就判定为完整可执行计划。
- 若 PlanBoundary 不是 VALID，ExecutionPlan 只可展示为观察摘要。

---

## 7. RiskActionGuardDisplayVO 最小结构

建议字段：

- `riskActionGuardStatus`
- `riskActionAdvice`
- `riskActionBlockingReason`
- `liquidityState`
- `stampedeDetected`
- `wickOnlyRisk`
- `opportunityPushAllowed`
- `reverseTradeAllowed`
- `newPositionAllowed`
- `manualRiskReviewRequired`

建议初始默认：

- `riskActionGuardStatus = BACKEND_PENDING`
- `opportunityPushAllowed = false`（未接入时 fail-closed）
- `reverseTradeAllowed = false`
- `newPositionAllowed = false`
- `manualRiskReviewRequired = true`

硬约束：

- 踩踏状态必须禁止反手、禁止新开仓、禁止机会推送。
- 短线插针不能直接判定趋势反转。
- 流动性恶化不能鼓励市价一次性砍仓。

---

## 8. PaperObservationDisplayVO 最小结构

建议字段：

- `paperObservationAvailable`
- `manualReviewEntryAvailable`
- `linkedPaperObservationCount`
- `linkedReviewCount`
- `missedOpportunityFlag`
- `reviewSummary`
- `notRealPosition`
- `notTradeInstruction`

建议初始默认：

- `paperObservationAvailable = false` 或 `BACKEND_PENDING`
- `manualReviewEntryAvailable = false` 或 `BACKEND_PENDING`
- `notRealPosition = true`
- `notTradeInstruction = true`

说明：

- 纸面观察不是实盘交易。
- 人工复盘不代表真实持仓。
- 不允许出现一键交易 / 自动执行语义。

---

## 9. Controller 最小组装方案

后续 Java 实现建议在 `DashboardController.dashboardDetail` 中按最小方式组装：

1. 保持现有：
   - `symbol`
   - `decision`
   - `marketEnvironmentMini`
   - `evidenceTopItems`
   - `scoreTopItems`

2. 新增默认只读 display：
   - `planBoundaryDisplay = BACKEND_PENDING`
   - `executionPlanDisplay = BOUNDARY_PENDING`
   - `riskActionGuardDisplay = BACKEND_PENDING`
   - `paperObservationDisplay = BACKEND_PENDING / unavailable`

3. 不生成真实 entry / stop / TP。

4. 不调用交易接口。

5. 不改变现有 decisionService 逻辑。

---

## 10. 与现有字段的关系

### 10.1 `DecisionResultVO.entryZone / stopLoss / takeProfitRules`

这些字段后续仍可展示为“历史执行建议摘要 / 文本字段”，但不得视为结构化 PlanBoundary 真值。

### 10.2 `readModelTruthStatus / readModelFallbackReason`

可作为缺失字段提示的辅助来源，但不能替代专门的 PlanBoundary status / incompleteReasons。

### 10.3 `ExecutionPlanVO`

现有 `ExecutionPlanVO` 可保留，不应直接替代新增的 `ExecutionPlanDisplayVO`。

建议：

- `ExecutionPlanVO` 继续承载执行计划原有文本结构。
- `ExecutionPlanDisplayVO` 承载 dashboard 安全展示状态。

---

## 11. 测试建议

后续 Java PR 应至少覆盖：

- `DashboardDetailResponseVO` 新字段 getter / setter。
- 默认 display VO 的安全默认值。
- `manualReviewRequired` 默认 true。
- `notTradeInstruction` 默认 true。
- BACKEND_PENDING 状态不包含 entry / stop / TP 数值。
- 不包含 order / autoExecute / autoTrade 字段。

---

## 12. 明确不做什么

本阶段明确不做：

- 不改 `src/`。
- 不改 `dashboard.html`。
- 不改 schema。
- 不改 `pom.xml`。
- 不新增接口。
- 不新增数据库表。
- 不接 order API。
- 不自动交易。
- 不自动开仓 / 平仓 / 反手。
- 不生成真实 entry / stop / TP。
- 不实现 RuleEngine / source assembler。

---

## 13. 后续 PR 拆分建议

建议后续按以下顺序推进：

1. `PlanBoundaryDisplayVO` 最小对象方案。
2. `ExecutionPlanDisplayVO` 最小对象方案。
3. `RiskActionGuardDisplayVO` 最小对象方案。
4. `PaperObservationDisplayVO` 最小对象方案。
5. `DashboardDetailResponseVO` Java 最小扩展。
6. `DashboardController` 组装 BACKEND_PENDING 默认 display。
7. dashboard 读取真实 display status 字段。
8. smoke 验证与截图验收。

---

## 14. 验收标准

本 PR 验收标准：

- 只新增一个方案文档：`docs/PHASE_DASHBOARD_DETAIL_RESPONSE_VO_MINIMAL_EXTENSION_PLAN.md`。
- 无 Java 代码改动。
- 无 dashboard 改动。
- 无 schema 改动。
- 无 order API / 自动交易相关改动。
- 文档明确 DashboardDetailResponseVO 最小扩展方向。
- 文档明确先补安全状态，不直接补交易数值。
