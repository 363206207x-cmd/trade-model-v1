# PHASE: PlanBoundary Read Model 字段方案（PR12）

## 1. 阶段目标

本阶段目标是定义 **PlanBoundary read model 字段方案**，为后续 dashboard 从占位展示进入真实后端字段读取做准备。

本 PR 只做方案文档，不实现代码、不改 schema、不改 dashboard、不新增接口、不接 order API、不自动交易。

核心目标：

- 明确 PlanBoundary 在 dashboard read model 中需要哪些字段。
- 明确字段语义、状态枚举、缺失原因与来源追踪要求。
- 明确哪些字段可以先作为 read-only DTO / VO 扩展，哪些需要后续 schema 或 service 接入方案。
- 保持 V1 的安全边界：非交易指令、需要人工复核、不自动执行。

---

## 2. 设计原则

PlanBoundary read model 必须遵守：

1. **只读展示优先**。
   - 首阶段只做 read model / VO / DTO 语义定义，不改变交易逻辑。

2. **状态先于价格**。
   - 在展示 entry / stop / TP 数值前，必须先展示 `planBoundaryStatus` 与 `sourceTraceStatus`。

3. **无来源不显示价格**。
   - entry / stop / TP 没有可追踪来源时，必须显示 INCOMPLETE，而不是默认值或前端推导值。

4. **人工复核默认开启**。
   - `manualReviewRequired` 与 `notTradeInstruction` 默认必须为 true。

5. **不得绕过 Risk Action Guard**。
   - 风险动作保护器状态必须能够影响展示语义，尤其踩踏状态不得包装为机会。

---

## 3. 顶层状态字段

建议 PlanBoundary read model 顶层包含：

- `symbol`
- `timeframe`
- `analysisId`
- `planId`
- `planBoundaryStatus`
- `planBoundaryStatusLabel`
- `planBoundaryUpdatedAt`
- `sourceTraceStatus`
- `dataQualityScore`
- `staleStatus`
- `manualReviewRequired`
- `notTradeInstruction`
- `incompleteReasons`
- `blockingReasons`
- `backendConnectionStatus`

### 3.1 `planBoundaryStatus`

建议枚举：

- `VALID`
- `WATCH_ONLY`
- `INCOMPLETE`
- `INVALID`
- `BACKEND_PENDING`

### 3.2 `sourceTraceStatus`

建议枚举：

- `TRACEABLE`：关键价格来源可追踪。
- `PARTIAL`：部分来源可追踪。
- `MISSING`：关键来源缺失。
- `BACKEND_PENDING`：后端尚未接入。

### 3.3 `backendConnectionStatus`

建议枚举：

- `CONNECTED`
- `PARTIAL`
- `BACKEND_PENDING`
- `PLACEHOLDER_ONLY`
- `MANUAL_REVIEW_REQUIRED`

---

## 4. Entry read model 字段

建议 entry 区块字段：

- `entryStatus`
- `entryType`
- `entryPrice`
- `entryZoneLow`
- `entryZoneHigh`
- `entrySourceType`
- `entrySourceValue`
- `entrySourceTimeframe`
- `entrySourceReason`
- `entrySourceRef`
- `entryIncompleteReason`

### Entry 展示规则

- `entryStatus = VALID` 时，才允许展示结构化 entry 数值。
- `entryStatus = INCOMPLETE` 时，必须展示缺失原因。
- `entryStatus = BACKEND_PENDING` 时，必须显示后端未接入。
- 不允许前端根据最新价、K线、AI 文案自行推导 entry。

---

## 5. Stop read model 字段

建议 stop 区块字段：

- `stopStatus`
- `stopType`
- `stopPrice`
- `stopSourceType`
- `stopSourceValue`
- `stopSourceTimeframe`
- `stopSourceReason`
- `stopSourceRef`
- `stopIncompleteReason`

### Stop 展示规则

- `stopStatus = VALID` 时，才允许展示 stop 数值。
- stop 必须说明来源与失效逻辑。
- stop 缺失时不得展示默认百分比止损。
- stop 缺失时 ExecutionPlan 不得展示为完整可执行计划。

---

## 6. Take Profit read model 字段

建议 take profit 区块字段：

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
- `sourceValue`
- `sourceTimeframe`
- `sourceReason`
- `sourceRef`

### Take Profit 展示规则

- TP levels 必须来自可追踪来源。
- 不允许只显示一个无来源的目标价。
- 分批止盈比例必须标记为建议性、人工复核，不是自动执行。

---

## 7. Invalidation read model 字段

建议 invalidation 区块字段：

- `invalidationStatus`
- `invalidationCondition`
- `invalidationReason`
- `invalidationSourceType`
- `invalidationSourceTimeframe`
- `invalidatedAt`

### Invalidation 展示规则

- INVALID 必须明确失效原因。
- INVALID 不得自动生成反向开仓计划。
- INVALID 只表示当前计划失效，不代表可以反手。

---

## 8. INCOMPLETE reasons 标准化

建议 `incompleteReasons` 使用结构化列表，每项包含：

- `code`
- `message`
- `field`
- `severity`
- `sourceRequired`

建议初始 code：

- `RUNTIME_KLINE_MISSING`
- `RUNTIME_KLINE_STALE`
- `LATEST_PRICE_INVALID`
- `DATA_QUALITY_LOW`
- `ENTRY_SOURCE_MISSING`
- `STOP_SOURCE_MISSING`
- `TP_SOURCE_MISSING`
- `SOURCE_ASSEMBLER_PENDING`
- `BACKEND_FIELD_PENDING`
- `RISK_ACTION_GUARD_BLOCKED`

---

## 9. 与 ExecutionPlan 的关系

PlanBoundary read model 必须能够驱动 ExecutionPlan 展示状态：

- `VALID`：ExecutionPlan 可展示结构化摘要，但仍非交易指令。
- `WATCH_ONLY`：ExecutionPlan 只展示观察摘要。
- `INCOMPLETE`：ExecutionPlan 显示缺失原因，不展示完整计划。
- `INVALID`：ExecutionPlan 显示失效，不包装为机会。
- `BACKEND_PENDING`：ExecutionPlan 显示后端未接入。

ExecutionPlan 不得绕过 PlanBoundary 自行声明“可执行”。

---

## 10. 与 Risk Action Guard 的关系

PlanBoundary read model 后续应暴露 Risk Action Guard 影响信息：

- `riskActionGuardStatus`
- `riskActionAdvice`
- `riskActionBlockingReason`
- `opportunityPushAllowed`
- `reverseTradeAllowed`
- `newPositionAllowed`

硬约束：

- 踩踏状态下：`opportunityPushAllowed = false`、`reverseTradeAllowed = false`、`newPositionAllowed = false`。
- 短线插针状态下：不得直接输出趋势反转或反向开仓计划。
- 流动性恶化状态下：不得鼓励市价一次性砍仓。

---

## 11. 最小接入顺序建议

建议后续实现按以下顺序：

1. 只新增 PlanBoundary read model DTO / VO 方案。
2. 在 dashboard detail read model 增加字段契约文档。
3. 后端 adapter 只读拼装 `planBoundaryStatus` 与 `incompleteReasons`。
4. dashboard 只读取真实状态，不展示数值。
5. 接入 Entry / Stop / TP source trace 字段。
6. 接入 ExecutionPlan 对齐状态。
7. smoke 验证与截图验收。

---

## 12. 明确不做什么

本阶段不做：

- 不改 `src/`。
- 不改 `dashboard.html`。
- 不改 schema。
- 不改 `pom.xml`。
- 不新增接口。
- 不新增数据库表。
- 不接 order API。
- 不自动交易。
- 不自动开仓 / 平仓 / 反手。
- 不把 WATCH_ONLY / INCOMPLETE / INVALID 包装为可执行机会。
- 不伪造 entry / stop / TP。

---

## 13. 验收标准

本 PR 验收标准：

- 只新增一个方案文档：`docs/PHASE_PLAN_BOUNDARY_READ_MODEL_FIELDS_PLAN.md`。
- 无代码改动。
- 无 dashboard 改动。
- 无 schema 改动。
- 无 order API / 自动交易相关改动。
- 文档明确 PlanBoundary read model 字段。
- 文档明确 entry / stop / TP 无来源不得展示。
- 文档明确 Risk Action Guard 的阻断语义。
