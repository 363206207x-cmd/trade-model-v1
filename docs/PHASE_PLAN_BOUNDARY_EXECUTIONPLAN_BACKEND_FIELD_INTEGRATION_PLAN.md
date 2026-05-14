# PHASE: PlanBoundary / ExecutionPlan 后端真实字段接入方案（PR12）

## 1. 阶段目标

本阶段目标是定义 **PlanBoundary 与 ExecutionPlan 后端真实字段接入方案**，为后续从 dashboard 占位展示进入真实 read model 接入做准备。

本 PR 只做方案文档，不实现代码、不改 schema、不改 dashboard、不改后端业务逻辑。

核心目标：

- 明确 ExecutionPlan 不得绕过 PlanBoundary。
- 明确 entry / stop / TP 必须来自可追踪 BoundaryCandidate，而不是前端或 AI 文案伪造。
- 明确什么时候可以显示 VALID，什么时候必须 INCOMPLETE / WATCH_ONLY / INVALID。
- 明确后续后端字段接入的最小顺序，避免一次性大改 RuleEngine / ExecutionPlan / Dashboard。

---

## 2. 当前背景

截至当前阶段，首页已具备以下 MVP 占位能力：

- 模块接入状态只读展示。
- PlanBoundary 状态占位展示。
- Risk Action Guard 占位展示。
- 纸面交易 / 人工复盘入口方案。
- ExecutionPlan 与 Boundary 状态展示对齐方案。
- Dashboard smoke 验收文档。

但当前阶段仍存在关键缺口：

- dashboard 占位尚未接入真实 PlanBoundary 后端字段。
- ExecutionPlan 摘要仍可能只是文本摘要，不等于结构化可执行边界。
- BoundaryCandidateService 已有基础 DTO / factory / service 能力，但尚未形成完整生产写入链路。
- RuleEngine / source assembler 尚未正式接入真实 entry / stop / TP 来源。

---

## 3. 后端字段接入原则

后续任何后端接入必须遵守：

1. **PlanBoundary 是 ExecutionPlan 可执行性前置条件**。
   - 没有 VALID Boundary，不得将 ExecutionPlan 标记为完整可执行。

2. **BoundaryCandidate 必须可追踪来源**。
   - entry / stop / TP / RR / invalidation condition 都需要 sourceType / sourceValue / sourceTimeframe / sourceReason / sourceRef 等来源说明。

3. **数据不足必须 INCOMPLETE**。
   - 不允许后端为了让 dashboard 好看而补默认价格。

4. **WATCH_ONLY 不等于可执行计划**。
   - 可展示观察摘要，但不得生成完整开仓结构。

5. **INVALID 不得包装为反向机会**。
   - INVALID 表示当前计划失效，不自动生成反向交易计划。

6. **manualReviewRequired 与 notTradeInstruction 默认保持 true**。
   - 后续任何输出都必须明确“非交易指令 / 需要人工复核”。

---

## 4. 建议后端 read model 字段草案

后续可在 Dashboard detail / summary read model 中逐步补充以下字段，但本 PR 不改接口：

### 4.1 PlanBoundary 状态字段

- `planBoundaryStatus`
- `planBoundaryStatusLabel`
- `planBoundaryUpdatedAt`
- `planBoundarySourceStatus`
- `planBoundaryIncompleteReasons`
- `manualReviewRequired`
- `notTradeInstruction`

### 4.2 Entry 字段

- `entryStatus`
- `entryType`
- `entryPrice`
- `entryZoneLow`
- `entryZoneHigh`
- `entrySourceType`
- `entrySourceTimeframe`
- `entrySourceReason`
- `entrySourceRef`

### 4.3 Stop 字段

- `stopStatus`
- `stopType`
- `stopPrice`
- `stopSourceType`
- `stopSourceTimeframe`
- `stopSourceReason`
- `stopSourceRef`

### 4.4 Take Profit 字段

- `takeProfitStatus`
- `takeProfitLevels`
- `takeProfitSourceType`
- `takeProfitSourceTimeframe`
- `takeProfitSourceReason`
- `partialRatio`
- `allocationRatio`

### 4.5 ExecutionPlan 对齐字段

- `executionPlanStatus`
- `executionPlanBoundaryAligned`
- `executionPlanReadiness`
- `executionPlanSummary`
- `executionPlanIncompleteReasons`
- `executionPlanNotExecutableReason`

---

## 5. 状态转换规则草案

### 5.1 VALID

只有满足以下条件，才允许输出 VALID：

- symbol / timeframe 完整。
- runtime kline context fresh 且数据质量达标。
- entry / stop / TP 至少一组完整且来源可追踪。
- sourceFields 存在。
- dataQualityScore 达到阈值。
- manualReviewRequired = true。
- notTradeInstruction = true。

### 5.2 WATCH_ONLY

以下情况应输出 WATCH_ONLY：

- 数据足以观察方向，但不足以形成完整 entry / stop / TP。
- 当前可以展示观察摘要，但不能展示完整执行计划。
- Risk Action Guard 或多周期冲突要求等待确认。

### 5.3 INCOMPLETE

以下情况必须输出 INCOMPLETE：

- runtime kline context 缺失或 stale。
- latest price 缺失或非法。
- dataQualityScore 低于阈值。
- entry / stop / TP 任一关键来源缺失。
- source assembler 未接入。
- backend field 尚未接入。

### 5.4 INVALID

以下情况应输出 INVALID：

- 原计划条件失效。
- 结构被破坏。
- PlanBoundary 已不适用于当前行情。
- 风险状态使该计划不再允许展示为机会。

---

## 6. 最小接入顺序建议

后续建议按以下 PR 顺序推进，避免一次性大改：

1. **PlanBoundary read model 字段方案文档**。
2. **Dashboard detail 展示字段契约方案**。
3. **BoundaryCandidateService 输出到 read model 的最小 adapter 方案**。
4. **ExecutionPlan status 与 PlanBoundary status 对齐的后端 DTO 方案**。
5. **dashboard 读取真实状态字段的只读实现**。
6. **INCOMPLETE reasons 后端最小接入**。
7. **source trace 字段最小接入**。
8. **smoke 验证与截图验收**。

---

## 7. 与 Risk Action Guard 的关系

后端字段接入不能绕过 Risk Action Guard：

- 风险高但流动性正常：可以提示减仓 / 移动止损 / 降低杠杆，但仍需人工复核。
- 风险高且流动性恶化：不建议市价一次性砍仓，优先分批降风险 / 等待流动性恢复 / 只降杠杆。
- 风险高且存在踩踏：禁止反手、禁止新开仓、禁止机会推送。
- 风险高但仅短线插针：不直接判定趋势反转，不生成反向开仓计划。

Risk Action Guard 是动作审核层，不是自动交易执行层。

---

## 8. 明确不做什么

本阶段明确不做：

- 不改 `src/`。
- 不改 `dashboard.html`。
- 不改 schema。
- 不改 `pom.xml`。
- 不新增接口。
- 不新增数据库表。
- 不接 order API。
- 不自动交易。
- 不自动开仓。
- 不自动平仓。
- 不自动反手。
- 不重构完整 RuleEngine。
- 不一次性接入完整 PlanBoundary 生产写入链路。

---

## 9. 验收标准

本 PR 验收标准：

- 只新增一个方案文档：`docs/PHASE_PLAN_BOUNDARY_EXECUTIONPLAN_BACKEND_FIELD_INTEGRATION_PLAN.md`。
- 无代码改动。
- 无 dashboard 改动。
- 无 schema 改动。
- 无 order API / 自动交易相关改动。
- 文档明确后端真实字段接入顺序。
- 文档明确 ExecutionPlan 不得绕过 PlanBoundary。
- 文档明确 entry / stop / TP 必须有可追踪来源。
