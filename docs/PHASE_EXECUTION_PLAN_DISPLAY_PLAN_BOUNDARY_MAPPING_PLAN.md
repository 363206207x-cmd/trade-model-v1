# PHASE: ExecutionPlanDisplay 与 PlanBoundaryDisplay 状态映射方案（PR36）

## 1. 阶段目标

本阶段定义 `ExecutionPlanDisplayVO` 与 `PlanBoundaryDisplayVO` 的状态映射方案，为后续 Java 最小实现做准备。

本 PR 只做方案文档，不实现 Java 代码，不改 dashboard，不改 schema，不改 mapper，不改 service 业务逻辑，不改 RuleEngine，不接任何交易执行链路。

核心目标：

- 明确 ExecutionPlanDisplay 必须受 PlanBoundaryDisplay 状态约束。
- 明确 PlanBoundary 未 VALID 时，ExecutionPlan 不得展示为完整可执行计划。
- 明确第一阶段只映射 status / label / reason / flags，不生成真实 entry / stop / take profit。
- 明确状态映射仍保持人工复核与非交易指令语义。

---

## 2. 当前已完成基础

当前已完成：

- `DashboardDetailResponseVO.PlanBoundaryDisplayVO`
- `DashboardDetailResponseVO.ExecutionPlanDisplayVO`
- `DashboardDetailResponseVO.withSafeDefaultDisplays()`
- `PlanBoundaryDisplayAdapter`
- `DefaultPlanBoundaryDisplayAdapter`
- `DashboardController` 已接入 PlanBoundaryDisplayAdapter
- `dashboard.html` 已读取 display status 字段

当前 PlanBoundaryDisplay adapter 仍是 fail-closed read-model 拼装层，不生成真实边界数值。

---

## 3. 映射原则

ExecutionPlanDisplay 状态映射必须遵守：

1. **PlanBoundary 优先**
   - `planBoundaryStatus` 是 ExecutionPlan 是否可展示为更高 readiness 的前置条件。

2. **默认不可执行**
   - 默认 `executionPlanStatus = BOUNDARY_PENDING`。
   - 默认 `executionPlanBoundaryAligned = false`。

3. **VALID 也不是交易指令**
   - 即使 PlanBoundary 后续为 VALID，也只能映射为 `READY_REVIEW_ONLY`。
   - 必须继续保留 `manualReviewRequired = true` 与 `notTradeInstruction = true`。

4. **不生成价格边界**
   - 映射层不生成 entry / stop / take profit。
   - 映射层不把文本字段包装为结构化边界。

5. **Risk Action Guard 不可绕过**
   - 若后续风险状态为锁定或阻断，ExecutionPlan 不得显示为可执行。

---

## 4. 建议状态映射表

| PlanBoundaryDisplay 状态 | ExecutionPlanDisplay 状态 | boundaryAligned | notExecutableReason | 展示语义 |
|---|---|---|---|---|
| BACKEND_PENDING | BOUNDARY_PENDING | false | PLAN_BOUNDARY_BACKEND_PENDING | 等待边界接入 |
| INCOMPLETE | INCOMPLETE | false | PLAN_BOUNDARY_INCOMPLETE | 执行计划不完整 |
| WATCH_ONLY | WATCH_ONLY | false | PLAN_BOUNDARY_WATCH_ONLY | 仅观察 |
| INVALID | INVALID | false | PLAN_BOUNDARY_INVALID | 当前计划失效 |
| VALID | READY_REVIEW_ONLY | true | null 或 MANUAL_REVIEW_REQUIRED | 只允许人工复核 |

注意：

- `READY_REVIEW_ONLY` 不等于可自动执行。
- `boundaryAligned = true` 也不代表可以自动下单。
- 第一阶段不主动映射到真实交易动作。

---

## 5. 建议字段写入范围

后续 Java 最小实现只允许写入 `ExecutionPlanDisplayVO` 的以下字段：

- `executionPlanStatus`
- `executionPlanStatusLabel`
- `executionPlanBoundaryAligned`
- `planBoundaryStatus`
- `notExecutableReason`
- `incompleteReasons`
- `manualReviewRequired`
- `notTradeInstruction`
- `updatedAt`

可选择保留或传递：

- `executionPlanSummary`

但 `executionPlanSummary` 只能作为文本摘要，不得决定可执行状态。

---

## 6. 建议 status label

| executionPlanStatus | label |
|---|---|
| BOUNDARY_PENDING | 等待边界接入 |
| INCOMPLETE | 执行计划不完整 |
| WATCH_ONLY | 仅观察 |
| INVALID | 计划已失效 |
| READY_REVIEW_ONLY | 可复核摘要 |
| BACKEND_PENDING | 后端未接入 |

---

## 7. 建议 reason code

建议第一阶段使用字符串列表，保持简单：

- `PLAN_BOUNDARY_BACKEND_PENDING`
- `PLAN_BOUNDARY_INCOMPLETE`
- `PLAN_BOUNDARY_WATCH_ONLY`
- `PLAN_BOUNDARY_INVALID`
- `PLAN_BOUNDARY_NOT_VALID`
- `MANUAL_REVIEW_REQUIRED`
- `SOURCE_TRACE_PENDING`
- `RISK_ACTION_GUARD_BLOCKED`

---

## 8. 建议后续对象

建议后续 Java 最小实现新增：

```text
src/main/java/org/example/trademodel/service/dashboard/ExecutionPlanDisplayAdapter.java
src/main/java/org/example/trademodel/service/dashboard/DefaultExecutionPlanDisplayAdapter.java
src/test/java/org/example/trademodel/service/dashboard/DefaultExecutionPlanDisplayAdapterTest.java
```

职责：

- 输入 `DecisionResultVO`、`PlanBoundaryDisplayVO`、fallback `ExecutionPlanDisplayVO`。
- 输出 `ExecutionPlanDisplayVO`。
- 不修改 PlanBoundaryDisplay。
- 不生成真实执行计划。

---

## 9. 建议方法签名

建议接口：

```java
DashboardDetailResponseVO.ExecutionPlanDisplayVO build(
        DecisionResultVO decision,
        DashboardDetailResponseVO.PlanBoundaryDisplayVO planBoundaryDisplay,
        DashboardDetailResponseVO.ExecutionPlanDisplayVO fallbackDisplay
);
```

说明：

- `decision` 可提供文本摘要。
- `planBoundaryDisplay` 提供状态前置约束。
- `fallbackDisplay` 保持安全默认兜底。

---

## 10. 默认实现规则

### 10.1 fallback 为空

返回新的 `ExecutionPlanDisplayVO` 默认对象：

- `executionPlanStatus = BOUNDARY_PENDING`
- `executionPlanBoundaryAligned = false`
- `planBoundaryStatus = BACKEND_PENDING`
- `manualReviewRequired = true`
- `notTradeInstruction = true`

### 10.2 planBoundaryDisplay 为空

返回 `BOUNDARY_PENDING`，原因：

```text
PLAN_BOUNDARY_BACKEND_PENDING
```

### 10.3 PlanBoundary = BACKEND_PENDING

映射为：

```text
executionPlanStatus = BOUNDARY_PENDING
executionPlanBoundaryAligned = false
notExecutableReason = PLAN_BOUNDARY_BACKEND_PENDING
```

### 10.4 PlanBoundary = INCOMPLETE

映射为：

```text
executionPlanStatus = INCOMPLETE
executionPlanBoundaryAligned = false
notExecutableReason = PLAN_BOUNDARY_INCOMPLETE
```

可继承 PlanBoundary incompleteReasons。

### 10.5 PlanBoundary = WATCH_ONLY

映射为：

```text
executionPlanStatus = WATCH_ONLY
executionPlanBoundaryAligned = false
notExecutableReason = PLAN_BOUNDARY_WATCH_ONLY
```

### 10.6 PlanBoundary = INVALID

映射为：

```text
executionPlanStatus = INVALID
executionPlanBoundaryAligned = false
notExecutableReason = PLAN_BOUNDARY_INVALID
```

### 10.7 PlanBoundary = VALID

映射为：

```text
executionPlanStatus = READY_REVIEW_ONLY
executionPlanBoundaryAligned = true
manualReviewRequired = true
notTradeInstruction = true
```

注意：第一阶段不生成任何价格边界。

---

## 11. 与现有文本字段关系

当前 `DecisionResultVO` 中可能存在：

- `executionPlanSummary`
- `recommendedAction`
- `entryZone`
- `stopLoss`
- `takeProfitRules`

映射规则：

- `executionPlanSummary` 可以作为摘要展示。
- `recommendedAction` 不应直接决定 READY 状态。
- `entryZone`、`stopLoss`、`takeProfitRules` 仍是文本字段，不得作为真实结构化边界。
- 不能因为这些文本字段存在就绕过 PlanBoundary 状态。

---

## 12. 测试建议

后续 Java 实现应测试：

1. fallback 为空时返回安全默认。
2. PlanBoundary 为空时返回 BOUNDARY_PENDING。
3. PlanBoundary BACKEND_PENDING 映射为 BOUNDARY_PENDING。
4. PlanBoundary INCOMPLETE 映射为 INCOMPLETE。
5. PlanBoundary WATCH_ONLY 映射为 WATCH_ONLY。
6. PlanBoundary INVALID 映射为 INVALID。
7. PlanBoundary VALID 只映射为 READY_REVIEW_ONLY。
8. 文本 executionPlanSummary 不会使 boundaryAligned 变为 true。
9. 始终 `manualReviewRequired = true`。
10. 始终 `notTradeInstruction = true`。

---

## 13. 与 DashboardController 的关系

Controller 接入应单独 PR 处理。

建议顺序：

1. PR #37：ExecutionPlanDisplayAdapter Java 最小实现，不改 Controller。
2. PR #38：DashboardController 接入 ExecutionPlanDisplayAdapter。
3. PR #39：本地 API smoke 验证。

Controller 接入时必须保持：

- 先执行 PlanBoundaryDisplayAdapter。
- 再执行 ExecutionPlanDisplayAdapter。
- adapter 失败时保留 fallback。
- 不影响原有 decision / evidence / score / marketEnvironmentMini。

---

## 14. 明确不做什么

本阶段和后续最小实现仍不做：

- 不改 schema。
- 不新增 mapper。
- 不写数据库。
- 不接 RuleEngine。
- 不接 source assembler。
- 不生成真实 entry / stop / take profit。
- 不生成真实交易执行计划。
- 不接任何交易执行链路。
- 不把 VALID 当作自动执行信号。

---

## 15. 本 PR 验收标准

本 PR 验收标准：

- 只新增一个文档：`docs/PHASE_EXECUTION_PLAN_DISPLAY_PLAN_BOUNDARY_MAPPING_PLAN.md`。
- 无 Java 改动。
- 无 dashboard.html 改动。
- 无 schema 改动。
- 无 mapper / service / RuleEngine 改动。
- 无交易执行相关改动。
- 文档明确 ExecutionPlanDisplay 必须受 PlanBoundaryDisplay 约束。
- 文档明确 VALID 也只映射为人工复核，不是交易指令。
