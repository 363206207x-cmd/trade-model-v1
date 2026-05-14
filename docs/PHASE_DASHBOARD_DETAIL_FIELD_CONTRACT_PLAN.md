# PHASE: Dashboard Detail 展示字段契约方案（PR13）

## 1. 阶段目标

本阶段目标是定义 **Dashboard detail 展示字段契约方案**，为后续 dashboard 从占位展示进入真实后端 read model 字段接入做准备。

本 PR 只做方案文档，不实现代码、不改 dashboard、不改后端、不改 schema、不新增接口、不接 order API、不自动交易。

核心目标：

- 明确 dashboard detail 需要展示哪些 PlanBoundary / ExecutionPlan / Risk Action Guard 字段。
- 明确字段缺失、后端未接入、仅占位时的展示语义。
- 明确 detail 页展示不得伪造 entry / stop / TP。
- 明确 ExecutionPlan 不得绕过 PlanBoundary 被展示为可执行计划。
- 保持 V1 的安全边界：非交易指令、需要人工复核、不自动执行。

---

## 2. 当前背景

截至当前阶段，已完成：

- 首页 MVP 总方案。
- 首页 MVP checklist。
- 模块接入状态只读展示。
- PlanBoundary 状态占位展示。
- Risk Action Guard 占位展示。
- 纸面交易 / 人工复盘入口方案。
- ExecutionPlan 与 Boundary 状态展示对齐方案。
- Dashboard smoke 验收文档。
- PlanBoundary / ExecutionPlan 后端字段接入方案。
- PlanBoundary read model 字段方案。

下一步需要明确 dashboard detail 展示字段契约，避免后续后端字段接入时出现字段语义漂移。

---

## 3. Detail 展示总原则

Dashboard detail 字段展示必须遵守：

1. **状态优先于数值**。
   - 先展示 `planBoundaryStatus` / `executionPlanStatus`，再展示 entry / stop / TP。

2. **来源优先于价格**。
   - 没有 source trace 的价格不得展示。

3. **不完整必须显式展示**。
   - INCOMPLETE 必须显示缺失原因，不得留空或伪装为正常。

4. **后端未接入必须显式展示**。
   - BACKEND_PENDING / PLACEHOLDER_ONLY 必须出现在页面语义中。

5. **交易动作必须带安全提示**。
   - 任何类似入场、止损、止盈、减仓、移动止损的展示都必须标注“非交易指令 / 需要人工复核”。

---

## 4. Detail 顶层字段契约草案

后续 dashboard detail 可接收以下顶层字段：

- `symbol`
- `analysisId`
- `planId`
- `timeframe`
- `decisionTime`
- `marketBias`
- `confidenceLevel`
- `riskLevel`
- `isWorthOpening`
- `moduleConnectionStatus`
- `manualReviewRequired`
- `notTradeInstruction`

字段语义：

- `manualReviewRequired` 默认 true。
- `notTradeInstruction` 默认 true。
- `moduleConnectionStatus` 用于提示该模块是已接入、部分接入、后端未接入、仅占位或需要人工复核。

---

## 5. PlanBoundary detail 字段契约

### 5.1 状态字段

- `planBoundaryStatus`
- `planBoundaryStatusLabel`
- `sourceTraceStatus`
- `backendConnectionStatus`
- `dataQualityScore`
- `staleStatus`
- `planBoundaryUpdatedAt`

### 5.2 缺失与阻断字段

- `incompleteReasons`
- `blockingReasons`
- `sourceRequiredFields`
- `backendPendingFields`

展示规则：

- `VALID`：允许展示结构化边界，但仍必须显示人工复核与非交易指令。
- `WATCH_ONLY`：只展示观察摘要，不展示完整执行计划。
- `INCOMPLETE`：必须展示缺失原因。
- `INVALID`：必须展示失效原因，不包装为反向机会。
- `BACKEND_PENDING`：必须展示后端未接入。

---

## 6. Entry / Stop / TP detail 字段契约

### 6.1 Entry

- `entryStatus`
- `entryType`
- `entryPrice`
- `entryZoneLow`
- `entryZoneHigh`
- `entrySourceType`
- `entrySourceTimeframe`
- `entrySourceReason`
- `entrySourceRef`
- `entryIncompleteReason`

展示规则：

- 只有 `entryStatus = VALID` 且 source trace 完整时，才允许展示 entry 数值。
- 否则必须展示 INCOMPLETE / BACKEND_PENDING。
- 前端不得自行推导 entry。

### 6.2 Stop

- `stopStatus`
- `stopType`
- `stopPrice`
- `stopSourceType`
- `stopSourceTimeframe`
- `stopSourceReason`
- `stopSourceRef`
- `stopIncompleteReason`

展示规则：

- stop 必须有来源。
- 不允许展示默认百分比止损。
- stop 缺失时，ExecutionPlan 不得展示为完整可执行计划。

### 6.3 Take Profit

- `takeProfitStatus`
- `takeProfitLevels`
- `takeProfitIncompleteReason`

每个 TP level 建议包含：

- `level`
- `price`
- `rr`
- `partialRatio`
- `allocationRatio`
- `sourceType`
- `sourceTimeframe`
- `sourceReason`
- `sourceRef`

展示规则：

- 不允许显示无来源目标价。
- 分批比例仅为建议性展示，不是自动执行。

---

## 7. ExecutionPlan detail 字段契约

建议字段：

- `executionPlanStatus`
- `executionPlanBoundaryAligned`
- `executionPlanReadiness`
- `executionPlanSummary`
- `executionPlanIncompleteReasons`
- `executionPlanNotExecutableReason`
- `recommendedActionLabel`
- `invalidCondition`
- `validPeriod`

展示规则：

- `executionPlanBoundaryAligned = false` 时，不得展示为完整执行计划。
- `planBoundaryStatus != VALID` 时，不得显示“可直接执行”。
- `WATCH_ONLY` 只能展示观察摘要。
- `INCOMPLETE` 必须显示缺失原因。
- `INVALID` 必须显示失效，不得包装为反向开仓机会。

---

## 8. Risk Action Guard detail 字段契约

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

展示规则：

- 流动性正常：可展示减仓 / 移动止损 / 降低杠杆建议语义，但仍需人工复核。
- 流动性恶化：不建议市价一次性砍仓。
- 踩踏状态：禁止反手、禁止新开仓、禁止机会推送。
- 短线插针：不直接判定趋势反转，不生成反向开仓计划。

Risk Action Guard 在 detail 页只做动作审核展示，不触发自动交易。

---

## 9. 纸面观察 / 人工复盘 detail 关联字段

建议字段：

- `paperObservationAvailable`
- `manualReviewEntryAvailable`
- `linkedPaperObservationCount`
- `linkedReviewCount`
- `missedOpportunityFlag`
- `reviewSummary`

展示规则：

- 纸面观察不是实盘交易。
- 人工复盘不代表真实持仓。
- 不允许一键交易或自动执行语义。

---

## 10. 缺失字段展示标准

若字段缺失，页面必须按以下规则展示：

- 后端未返回字段：显示 `BACKEND_PENDING / 后端未接入`。
- 字段返回但缺来源：显示 `INCOMPLETE / 来源缺失`。
- 字段无效：显示 `INVALID / 字段无效`。
- 字段仅占位：显示 `PLACEHOLDER_ONLY / 仅占位`。
- 需要人工判断：显示 `MANUAL_REVIEW_REQUIRED / 需要人工复核`。

不得用 `—`、空字符串、默认 0、默认价格来掩盖关键字段缺失。

---

## 11. 禁止语义

Dashboard detail 不允许出现：

- 自动下单
- 自动开仓
- 自动平仓
- 自动反手
- 自动止损
- 自动止盈
- 一键执行
- 可直接执行
- 已自动执行
- WATCH_ONLY 被包装为机会
- INCOMPLETE 被包装为完整计划
- INVALID 被包装为反向机会
- 无来源 entry / stop / TP

---

## 12. 后续 PR 拆分建议

建议后续按以下最小单元推进：

1. Dashboard detail 字段契约与现有 VO 对照审计。
2. Dashboard detail read model VO 最小扩展方案。
3. PlanBoundary status / incompleteReasons 后端 adapter 方案。
4. Dashboard 只读读取真实 status，不展示数值。
5. Entry / Stop / TP source trace 字段接入方案。
6. ExecutionPlan status 对齐字段接入方案。
7. smoke 验证与截图验收。

---

## 13. 明确不做什么

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
- 不伪造 entry / stop / TP。

---

## 14. 验收标准

本 PR 验收标准：

- 只新增一个方案文档：`docs/PHASE_DASHBOARD_DETAIL_FIELD_CONTRACT_PLAN.md`。
- 无代码改动。
- 无 dashboard 改动。
- 无 schema 改动。
- 无 order API / 自动交易相关改动。
- 文档明确 detail 字段契约。
- 文档明确缺失字段展示标准。
- 文档明确 ExecutionPlan / PlanBoundary / Risk Action Guard 的安全展示边界。
