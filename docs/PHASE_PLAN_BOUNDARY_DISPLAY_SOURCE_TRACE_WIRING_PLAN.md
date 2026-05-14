# PHASE: PlanBoundaryDisplayAdapter 调用 SourceTraceAdapter 方案（PR62）

## 1. 阶段目标

本阶段定义 `DefaultPlanBoundaryDisplayAdapter` 与 `PlanBoundarySourceTraceAdapter` 的组合方案。

本 PR 只新增方案文档，不改 Java、不改 dashboard、不改 schema、不改 mapper、不改 RuleEngine。

---

## 2. 当前基础

当前已完成：

- `PlanBoundaryDisplayAdapter`
- `DefaultPlanBoundaryDisplayAdapter`
- `PlanBoundarySourceTraceAdapter`
- `DefaultPlanBoundarySourceTraceAdapter`
- `DefaultPlanBoundarySourceTraceAdapterTest`

当前 `PlanBoundarySourceTraceAdapter` 是 fail-closed readiness adapter。

它不会返回 `VALID`，也不会生成 entry / stop / TP。

---

## 3. 建议调用方向

后续建议调用方向：

```text
DashboardController
↓
PlanBoundaryDisplayAdapter
↓
PlanBoundarySourceTraceAdapter
```

Controller 仍只依赖 `PlanBoundaryDisplayAdapter`。

`PlanBoundarySourceTraceAdapter` 作为内部 readiness 检查，不直接暴露给 Controller。

---

## 4. 构造器注入建议

建议 `DefaultPlanBoundaryDisplayAdapter` 通过构造器接收：

```java
private final PlanBoundarySourceTraceAdapter sourceTraceAdapter;
```

后续 Java PR 只需要让 `DefaultPlanBoundaryDisplayAdapter` 在 build 过程中调用 source trace adapter。

---

## 5. 合并策略

### 5.1 fallback 为空

仍先创建 safe default display。

### 5.2 decision 为空

保持：

```text
BACKEND_PENDING
DECISION_MISSING
manualReviewRequired = true
notTradeInstruction = true
```

### 5.3 decision 存在但 source trace 不可用

source trace adapter 应返回：

```text
planBoundaryStatus = INCOMPLETE
sourceTraceStatus = MISSING
backendConnectionStatus = PARTIAL
incompleteReasons 包含 SOURCE_TRACE_INPUT_NOT_AVAILABLE
blockingReasons 包含 BOUNDARY_CANDIDATE_DTO_MISSING
blockingReasons 包含 RUNTIME_KLINE_CONTEXT_DTO_MISSING
manualReviewRequired = true
notTradeInstruction = true
```

### 5.4 readModelTruthStatus = PARTIAL

必须保留：

```text
READ_MODEL_PARTIAL
readModelFallbackReason
```

不能被 source trace adapter 覆盖。

---

## 6. 当前阶段禁止 VALID

当前 main 仍缺少真实 source trace 输入对象。

因此当前组合后仍不得输出：

```text
planBoundaryStatus = VALID
```

当前阶段只允许：

```text
BACKEND_PENDING
INCOMPLETE
```

---

## 7. 测试建议

后续 Java 接入 PR 应验证：

1. adapter 构造器可接收 source trace adapter。
2. decision 为空时仍 fail-closed。
3. decision 存在时会调用 source trace adapter。
4. source trace adapter 返回 INCOMPLETE 时最终 display 为 INCOMPLETE。
5. source trace reasons 被保留。
6. read model partial reasons 被保留。
7. 文本字段存在时仍不返回 VALID。
8. safety flags 始终为 true。

---

## 8. 后续 Java 范围建议

建议后续 Java PR 只修改：

```text
src/main/java/org/example/trademodel/service/dashboard/DefaultPlanBoundaryDisplayAdapter.java
src/test/java/org/example/trademodel/service/dashboard/DefaultPlanBoundaryDisplayAdapterTest.java
```

不修改：

```text
DashboardController.java
dashboard.html
schema.sql
mapper
RuleEngine
```

---

## 9. 风险点

### 9.1 依赖方向

正确方向：

```text
PlanBoundaryDisplayAdapter -> PlanBoundarySourceTraceAdapter
```

不得反向依赖。

### 9.2 reason 合并

必须追加 reason，不清空已有 reason。

### 9.3 VALID 误升级

当前阶段不得输出 VALID。

VALID 必须等真实 source trace 输入对象存在后另行规划。

---

## 10. 后续建议

下一步建议进入：

```text
PR #63：DefaultPlanBoundaryDisplayAdapter 接入 SourceTraceAdapter Java 最小实现
```

实现边界：

- 只改 `DefaultPlanBoundaryDisplayAdapter` 与对应测试。
- 不改 Controller。
- 不改 dashboard。
- 不改 schema。
- 不返回 VALID。
- 不生成 entry / stop / TP。

---

## 11. 本 PR 验收标准

- 只新增 `docs/PHASE_PLAN_BOUNDARY_DISPLAY_SOURCE_TRACE_WIRING_PLAN.md`。
- 无 Java 改动。
- 无 dashboard 改动。
- 无 schema 改动。
- 无 mapper / RuleEngine 改动。
- 文档明确调用方向。
- 文档明确当前阶段不得返回 VALID。
