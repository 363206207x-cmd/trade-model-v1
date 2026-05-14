# PHASE: ExecutionPlanDisplayVO 最小对象方案（PR17）

## 1. 阶段目标

本阶段目标是定义 `ExecutionPlanDisplayVO` 的最小对象方案，为后续 dashboard detail read model 的 Java 最小实现做准备。

本 PR 只做方案文档，不实现 Java 代码、不改 dashboard、不改 schema、不新增接口、不接 order API、不自动交易。

核心目标：

- 让 ExecutionPlan 在 dashboard 中以安全展示对象呈现。
- 防止现有 `executionPlanSummary` / `entryZone` / `stopLoss` / `takeProfitRules` 被误读为结构化可执行计划。
- 明确 ExecutionPlan 必须受 PlanBoundary 状态约束。
- 第一阶段只承载状态、摘要、对齐标记、不可执行原因、人工复核与非交易指令标记。
- 不承载真实 entry / stop / TP 数值。

---

## 2. 对象定位

`ExecutionPlanDisplayVO` 是 dashboard read model 的展示层对象，不是交易执行对象，也不是订单请求对象。

它的定位是：

- 解释当前 ExecutionPlan 是否具备可展示条件。
- 说明它是否已与 PlanBoundary 对齐。
- 在 Boundary 不完整时明确给出不可执行原因。
- 将原有文本摘要安全展示为“观察摘要 / 建议性摘要”，而不是自动执行计划。
- 明确非交易指令，需要人工复核。

它不是：

- 自动交易计划。
- 下单请求。
- 持仓调整指令。
- RuleEngine 输出的完整边界。
- PlanBoundary 的替代物。

---

## 3. 最小字段建议

建议 `ExecutionPlanDisplayVO` 第一阶段包含以下字段：

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

说明：

- 第一阶段不包含 entry / stop / TP 数值。
- 第一阶段不包含 orderId / orderRequest / autoExecute / autoTrade。
- `executionPlanSummary` 可以复用现有文本摘要，但必须被标记为非交易指令。

---

## 4. 字段语义

### 4.1 `executionPlanStatus`

建议枚举：

- `READY_REVIEW_ONLY`
- `BOUNDARY_PENDING`
- `WATCH_ONLY`
- `INCOMPLETE`
- `INVALID`
- `BACKEND_PENDING`

语义：

- `READY_REVIEW_ONLY`：具备展示摘要的条件，但仍只允许人工复核。
- `BOUNDARY_PENDING`：等待 PlanBoundary 对齐。
- `WATCH_ONLY`：只观察，不输出完整执行计划。
- `INCOMPLETE`：执行计划不完整，必须展示原因。
- `INVALID`：执行计划已失效。
- `BACKEND_PENDING`：后端真实字段尚未接入。

### 4.2 `executionPlanStatusLabel`

建议中文文案：

- `可复核摘要`
- `等待边界接入`
- `仅观察`
- `计划不完整`
- `计划已失效`
- `后端未接入`

### 4.3 `executionPlanBoundaryAligned`

布尔值。

- `true`：ExecutionPlan 已与 VALID PlanBoundary 对齐。
- `false`：未对齐，不得展示为完整可执行计划。

默认必须为 false。

### 4.4 `planBoundaryStatus`

用于携带当前关联的 PlanBoundary 状态。

规则：

- `planBoundaryStatus != VALID` 时，ExecutionPlan 不得展示为完整可执行计划。
- `planBoundaryStatus = INCOMPLETE` 时，必须展示缺失原因。
- `planBoundaryStatus = WATCH_ONLY` 时，只展示观察摘要。
- `planBoundaryStatus = INVALID` 时，必须显示失效。

### 4.5 `executionPlanSummary`

可复用当前已有 `DecisionResultVO.executionPlanSummary`。

限制：

- 只能作为文本摘要。
- 不代表真实执行已准备完成。
- 不得与 `entryZone` / `stopLoss` / `takeProfitRules` 拼接成伪结构化计划。

### 4.6 `notExecutableReason`

用于说明当前为什么不能被展示为完整可执行计划。

建议原因：

- `PLAN_BOUNDARY_NOT_VALID`
- `PLAN_BOUNDARY_BACKEND_PENDING`
- `ENTRY_SOURCE_MISSING`
- `STOP_SOURCE_MISSING`
- `TP_SOURCE_MISSING`
- `RISK_ACTION_GUARD_BLOCKED`
- `DATA_QUALITY_LOW`
- `EXECUTION_PLAN_TEXT_ONLY`

### 4.7 `incompleteReasons`

结构化缺失原因列表。

建议每项包含：

- `code`
- `message`
- `field`
- `severity`

### 4.8 `manualReviewRequired`

必须默认 true。

### 4.9 `notTradeInstruction`

必须默认 true。

### 4.10 `updatedAt`

展示状态更新时间。

第一阶段可为 null，前端需安全兜底。

---

## 5. 安全默认值

后续 Java 最小实现建议默认值：

- `executionPlanStatus = BOUNDARY_PENDING`
- `executionPlanStatusLabel = 等待边界接入`
- `executionPlanBoundaryAligned = false`
- `planBoundaryStatus = BACKEND_PENDING`
- `notExecutableReason = PLAN_BOUNDARY_BACKEND_PENDING`
- `manualReviewRequired = true`
- `notTradeInstruction = true`
- `incompleteReasons` 不为 null，默认包含边界未接入说明或空列表。

安全默认策略：

- 未接入 PlanBoundary 前，不展示为完整计划。
- 未接入 source trace 前，不展示价格。
- 未通过人工复核前，不显示可执行语义。

---

## 6. 状态展示规则

### 6.1 READY_REVIEW_ONLY

当状态为 `READY_REVIEW_ONLY`：

- 可以展示结构化摘要。
- 必须显示“非交易指令 / 需要人工复核”。
- 不允许自动执行。

### 6.2 BOUNDARY_PENDING

当状态为 `BOUNDARY_PENDING`：

- 显示等待 PlanBoundary 接入或对齐。
- 不得展示为完整执行计划。
- 可以展示已有文本摘要，但必须标记为观察摘要。

### 6.3 WATCH_ONLY

当状态为 `WATCH_ONLY`：

- 只允许观察。
- 不得包装成可执行机会。

### 6.4 INCOMPLETE

当状态为 `INCOMPLETE`：

- 必须展示缺失原因。
- 不得展示完整执行计划。
- 不得伪造 entry / stop / TP。

### 6.5 INVALID

当状态为 `INVALID`：

- 必须显示计划失效。
- 不得包装为反向机会。
- 不得触发反手语义。

### 6.6 BACKEND_PENDING

当状态为 `BACKEND_PENDING`：

- 必须显示后端未接入。
- 不得假装已有计划。

---

## 7. 与 PlanBoundaryDisplayVO 的关系

`ExecutionPlanDisplayVO` 必须依赖 `PlanBoundaryDisplayVO` 的状态。

建议映射：

| PlanBoundary 状态 | ExecutionPlanDisplay 状态 | 说明 |
|---|---|---|
| VALID | READY_REVIEW_ONLY | 可展示摘要，但仍需人工复核。 |
| WATCH_ONLY | WATCH_ONLY | 只观察。 |
| INCOMPLETE | INCOMPLETE | 显示缺失原因。 |
| INVALID | INVALID | 显示失效。 |
| BACKEND_PENDING | BOUNDARY_PENDING | 等待边界接入。 |

---

## 8. 与现有 DecisionResultVO 字段的关系

当前已有：

- `executionPlanSummary`
- `recommendedAction`
- `planMode`
- `entryZone`
- `stopLoss`
- `takeProfitRules`

展示策略：

- `executionPlanSummary` 可以进入 `ExecutionPlanDisplayVO.executionPlanSummary`。
- `recommendedAction` 可以作为文字摘要参考。
- `entryZone` / `stopLoss` / `takeProfitRules` 暂不进入第一阶段 display VO。
- 若后续进入 display VO，必须先补 status 与 source trace。

---

## 9. 与 Risk Action Guard 的关系

ExecutionPlan display 不能绕过 Risk Action Guard。

必须遵守：

- 踩踏状态：不得展示新开仓或反手计划。
- 流动性恶化：不得鼓励市价一次性砍仓。
- 短线插针：不得直接生成趋势反转或反向开仓计划。
- 风险动作建议只作为人工复核提示。

后续可通过 `notExecutableReason` 或 `incompleteReasons` 承载：

- `RISK_ACTION_GUARD_BLOCKED`
- `STAMPEDE_LOCK`
- `LIQUIDITY_DEGRADED`
- `WICK_CONFIRMATION_REQUIRED`

---

## 10. 后续 Java 实现建议

建议后续 Java PR 拆成：

1. 新增 `ExecutionPlanDisplayVO` 类或 `DashboardDetailResponseVO.ExecutionPlanDisplayVO` 内部静态类。
2. 新增 getter / setter。
3. 默认构造或 factory 输出 BOUNDARY_PENDING。
4. 单测验证安全默认值。
5. `DashboardDetailResponseVO` 新增字段。
6. `DashboardController` detail 组装默认 display。
7. 后续接入真实 PlanBoundaryDisplayVO 状态映射。

---

## 11. 测试建议

后续 Java 最小实现应测试：

- 默认 `executionPlanStatus = BOUNDARY_PENDING`。
- 默认 `executionPlanBoundaryAligned = false`。
- 默认 `manualReviewRequired = true`。
- 默认 `notTradeInstruction = true`。
- 不包含 order / autoTrade / autoExecute 字段。
- 不包含 entry / stop / TP 数值字段。
- `executionPlanSummary` 只作为文本摘要，不影响可执行状态。

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

## 13. 验收标准

本 PR 验收标准：

- 只新增一个方案文档：`docs/PHASE_EXECUTION_PLAN_DISPLAY_VO_MINIMAL_OBJECT_PLAN.md`。
- 无 Java 代码改动。
- 无 dashboard 改动。
- 无 schema 改动。
- 无 order API / 自动交易相关改动。
- 文档明确 `ExecutionPlanDisplayVO` 最小字段。
- 文档明确 ExecutionPlan 必须受 PlanBoundary 状态约束。
- 文档明确第一阶段不承载 entry / stop / TP 数值。
