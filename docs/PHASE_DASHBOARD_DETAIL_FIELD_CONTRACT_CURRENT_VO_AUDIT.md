# PHASE: Dashboard Detail 字段契约与现有 VO 对照审计（PR14）

## 1. 阶段目标

本阶段目标是对 PR #13 定义的 Dashboard detail 展示字段契约，与当前 `main` 分支已有 Controller / VO / dashboard read model 进行只读对照审计。

本 PR 只新增审计文档，不实现代码、不改 dashboard、不改后端、不改 schema、不新增接口、不接 order API、不自动交易。

核心目标：

- 明确当前 `/api/dashboard/detail` 已经返回哪些字段。
- 明确哪些字段已可复用。
- 明确哪些字段仅文本化存在，不能视为结构化真值。
- 明确哪些 PlanBoundary / ExecutionPlan / Risk Action Guard 字段当前缺失。
- 为后续最小 VO / read model 扩展提供边界依据。

---

## 2. 审计依据

本次只读审计基于当前 `main` 分支可见文件：

- `src/main/java/org/example/trademodel/controller/DashboardController.java`
- `src/main/java/org/example/trademodel/vo/DashboardDetailResponseVO.java`
- `src/main/java/org/example/trademodel/vo/DashboardSummaryResponseVO.java`
- `src/main/java/org/example/trademodel/vo/DecisionResultVO.java`
- `src/main/java/org/example/trademodel/vo/ExecutionPlanVO.java`

注意：本 PR 未修改上述文件，仅记录审计结论。

---

## 3. 当前 DashboardController 事实

当前 `/api/dashboard/detail` 的组成逻辑为：

- 标准化 `symbol`。
- 创建 `DashboardDetailResponseVO`。
- 设置：
  - `symbol`
  - `decision = decisionService.getLatestDecisionResultBySymbol(symbol)`
  - `marketEnvironmentMini`
  - `evidenceTopItems`
  - `scoreTopItems`

当前 `/api/dashboard/summary` 的组成逻辑为：

- `systemStatus`
- `openPositionCount`
- `systemHealth`
- `alerts`
- `decisions = decisionService.getLatestDecisionResults(limit)`

结论：

- 当前 detail 接口的核心交易展示仍主要依赖 `DecisionResultVO`。
- 当前 detail 接口尚未显式返回独立的 PlanBoundary read model 对象。
- 当前 detail 接口尚未显式返回独立的 ExecutionPlan status / PlanBoundary alignment 对象。
- 当前 detail 接口尚未显式返回独立的 Risk Action Guard 对象。

---

## 4. DashboardDetailResponseVO 当前字段对照

当前 `DashboardDetailResponseVO` 已有字段：

- `symbol`
- `decision`
- `marketEnvironmentMini`
- `evidenceTopItems`
- `scoreTopItems`

与 PR #13 字段契约对照：

| PR #13 字段类型 | 当前状态 | 说明 |
|---|---|---|
| `symbol` | 已有 | 顶层字段已存在。 |
| `analysisId` | 间接已有 | 通过 `decision.analysisId` 获取。 |
| `marketBias` | 间接已有 | 通过 `decision.marketBiasHierarchy` 获取。 |
| `confidenceLevel` | 间接已有 | 通过 `decision.confidenceLevel` 获取。 |
| `riskLevel` | 间接已有 | 通过 `decision.riskLevel` 获取。 |
| `isWorthOpening` | 间接已有 | 通过 `decision.isWorthOpening` 获取。 |
| `PlanBoundary` 独立对象 | 缺失 | 当前没有独立 PlanBoundary read model。 |
| `ExecutionPlan` 独立状态对象 | 缺失 | 当前主要通过 `decision.executionPlanSummary` 与相关文本字段表达。 |
| `RiskActionGuard` 独立对象 | 缺失 | 当前没有独立 Risk Action Guard read model。 |
| `paperObservation` 关联字段 | 缺失 | 当前没有纸面观察 / 人工复盘入口字段。 |

结论：

- 顶层基础信息已有一部分。
- 真实 PlanBoundary / ExecutionPlan / Risk Action Guard 字段仍未结构化进入 detail response。

---

## 5. DecisionResultVO 当前字段对照

当前 `DecisionResultVO` 已具备较多 dashboard 可展示字段，包括：

### 5.1 决策基础字段

- `decisionId`
- `analysisId`
- `symbol`
- `marketBiasHierarchy`
- `tradeType`
- `confidenceLevel`
- `riskLevel`
- `actionPriority`
- `conclusionSummary`
- `isWorthOpening`
- `multiTfConvergence`
- `aiRoleResults`
- `isAdopted`
- `validPeriod`
- `invalidCondition`
- `evidenceSummary`
- `dataQualityScore`
- `createTime`

这些字段可支撑当前 detail 页的基础分析展示。

### 5.2 执行计划相关文本字段

当前已存在：

- `executionPlanSummary`
- `recommendedAction`
- `planMode`
- `entryZone`
- `stopLoss`
- `takeProfitRules`
- `leverageSuggestion`
- `positionSuggestion`

审计结论：

- 这些字段可以用于“文本摘要展示”。
- 但它们当前不能直接等同于结构化 PlanBoundary 真值。
- `entryZone`、`stopLoss`、`takeProfitRules` 是字符串字段，当前缺少 source trace、status、timeframe、reason 等结构化来源字段。
- 后续 dashboard 不应把这些字符串直接包装成 VALID entry / stop / TP。

### 5.3 持仓相关字段

当前已存在：

- `hasOpenPosition`
- `positionSide`
- `avgOpenPrice`
- `positionOpenTime`
- `positionQuantity`
- `unrealizedPnlPct`
- `positionStatus`
- `markPrice`
- `breakEvenPrice`
- `liquidationPrice`

审计结论：

- 已具备部分手动/同步持仓展示基础。
- 但持仓字段不等于纸面交易记录。
- 也不等于 PlanBoundary 或 Risk Action Guard 的动作结论。

### 5.4 Read model 完整性字段

当前已存在：

- `readModelTruthStatus`
- `readModelFallbackReason`

审计结论：

- 这两个字段非常适合作为后续 detail 缺失字段展示策略的基础。
- 但它们目前是通用 read model 完整性提示，不是专门的 PlanBoundary status / incompleteReasons。

---

## 6. ExecutionPlanVO 当前字段对照

当前 `ExecutionPlanVO` 已有：

- `planId`
- `planMode`
- `recommendedAction`
- `entryZone`
- `stopLoss`
- `takeProfitRules`
- `addPositionCondition`
- `reducePositionCondition`
- `abandonCondition`
- `invalidCondition`
- `leverageSuggestion`
- `positionSuggestion`

并已有模式常量：

- `ADVISORY`
- `SEMI_STRUCTURED`

审计结论：

- 当前 `ExecutionPlanVO` 适合承载“建议性执行摘要 / 半结构化文本”。
- 当前缺少 `executionPlanStatus`、`executionPlanBoundaryAligned`、`executionPlanIncompleteReasons`、`executionPlanNotExecutableReason`。
- 当前 `entryZone` / `stopLoss` / `takeProfitRules` 仍为字符串字段，不能证明其来自可追踪 BoundaryCandidate。
- 后续应先增加 status / alignment / incomplete reason，而不是直接增强页面展示数值。

---

## 7. 当前缺失字段清单

### 7.1 PlanBoundary 缺失字段

当前 detail response 未显式提供：

- `planBoundaryStatus`
- `planBoundaryStatusLabel`
- `sourceTraceStatus`
- `backendConnectionStatus`
- `planBoundaryUpdatedAt`
- `planBoundaryIncompleteReasons`
- `blockingReasons`
- `sourceRequiredFields`
- `backendPendingFields`

### 7.2 Entry / Stop / TP 缺失字段

当前未结构化提供：

- `entryStatus`
- `entrySourceType`
- `entrySourceTimeframe`
- `entrySourceReason`
- `entrySourceRef`
- `stopStatus`
- `stopSourceType`
- `stopSourceTimeframe`
- `stopSourceReason`
- `stopSourceRef`
- `takeProfitStatus`
- `takeProfitLevels`
- TP level source trace 字段

### 7.3 ExecutionPlan 对齐字段缺失

当前未提供：

- `executionPlanStatus`
- `executionPlanBoundaryAligned`
- `executionPlanReadiness`
- `executionPlanIncompleteReasons`
- `executionPlanNotExecutableReason`

### 7.4 Risk Action Guard 字段缺失

当前未提供：

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

### 7.5 纸面观察 / 人工复盘字段缺失

当前未提供：

- `paperObservationAvailable`
- `manualReviewEntryAvailable`
- `linkedPaperObservationCount`
- `linkedReviewCount`
- `missedOpportunityFlag`
- `reviewSummary`

---

## 8. 风险判断

### 8.1 最大风险

当前已有字段中存在 `entryZone`、`stopLoss`、`takeProfitRules` 等文本字段。

如果 dashboard 后续直接把这些文本字段显示成“真实结构化 entry / stop / TP”，会违反当前 V1 边界。

正确做法：

- 在没有 PlanBoundary read model 前，仅展示为“历史执行建议摘要 / 后端文本字段”。
- 必须同时显示 `BACKEND_PENDING`、`INCOMPLETE` 或 `source trace missing`。
- 不得展示为 VALID 结构化边界。

### 8.2 第二风险

ExecutionPlan 文本摘要可能被误解为可执行计划。

正确做法：

- 增加 `executionPlanBoundaryAligned` 或同义状态。
- 如果 `planBoundaryStatus != VALID`，ExecutionPlan 只能显示为观察摘要。

### 8.3 第三风险

Risk Action Guard 当前尚无结构化字段。

正确做法：

- 当前 dashboard 只能展示占位与文案规则。
- 不得把风险提示转成自动动作。

---

## 9. 推荐后续最小实现顺序

建议后续 PR 按以下顺序推进：

1. **DashboardDetailResponseVO 最小扩展方案**。
   - 只定义新增 read model 对象结构。
   - 不立即实现业务逻辑。

2. **PlanBoundaryDisplayVO / DTO 最小对象方案**。
   - 只承载 status / incompleteReasons / manualReviewRequired / notTradeInstruction。

3. **ExecutionPlanDisplayVO 最小对象方案**。
   - 只承载 status / boundaryAligned / summary / notExecutableReason。

4. **RiskActionGuardDisplayVO 最小对象方案**。
   - 只承载 status / advice / blocking flags。

5. **DashboardController detail 只读组装方案**。
   - 初期可输出 BACKEND_PENDING / PLACEHOLDER_ONLY。
   - 不生成真实 entry / stop / TP。

6. **dashboard 读取真实 status 字段方案**。
   - 页面从后端读取 status，而不是继续写死占位。

7. **smoke 验证与截图验收**。

---

## 10. 明确不做什么

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
- 不把 `entryZone` / `stopLoss` / `takeProfitRules` 直接认定为结构化真值。

---

## 11. 验收标准

本 PR 验收标准：

- 只新增一个审计文档：`docs/PHASE_DASHBOARD_DETAIL_FIELD_CONTRACT_CURRENT_VO_AUDIT.md`。
- 无代码改动。
- 无 dashboard 改动。
- 无 schema 改动。
- 无 order API / 自动交易相关改动。
- 文档明确当前已有字段与缺失字段。
- 文档明确 `entryZone` / `stopLoss` / `takeProfitRules` 不能直接视为结构化 PlanBoundary 真值。
