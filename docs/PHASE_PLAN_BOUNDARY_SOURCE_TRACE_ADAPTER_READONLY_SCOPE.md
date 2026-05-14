# PHASE: PlanBoundarySourceTraceAdapter Java 最小实现前只读范围确认（PR58）

## 1. 阶段目标

本阶段在进入 `PlanBoundarySourceTraceAdapter` Java 最小实现前，对当前远端 `main` 可见代码进行只读范围确认。

本 PR 只新增审计文档，不修改 Java、不修改 dashboard、不修改 schema、不新增 mapper、不接 RuleEngine、不接 order API、不自动交易。

核心目标：

- 确认当前 main 是否已有可直接引用的 BoundaryCandidate / RuntimeKlineContext 相关 DTO。
- 确认当前 main 是否已有可直接复用的 PlanBoundaryDisplayVO。
- 明确下一步 Java 最小实现不能假设不存在的 DTO 已经可用。
- 明确如果真实 source trace 对象缺失，应继续保持 fail-closed，不伪造 VALID。

---

## 2. 只读检查范围

本次只读检查围绕以下关键词进行：

- `BoundaryCandidateDTO`
- `BoundaryEntryDTO`
- `BoundaryStopDTO`
- `BoundaryTakeProfitLevelDTO`
- `BoundaryNumericSourceDTO`
- `BoundarySourceFieldsDTO`
- `RuntimeKlineContextDTO`
- `PlanBoundaryDisplayVO`
- `DashboardDetailResponseVO`
- `planboundary`
- `RuntimeKlineContext`
- `BoundaryCandidate`

---

## 3. 当前远端 main 事实

### 3.1 BoundaryCandidate / RuntimeKlineContext 相关对象

当前远端 `main` 搜索未发现以下可直接复用对象：

```text
BoundaryCandidateDTO
BoundaryEntryDTO
BoundaryStopDTO
BoundaryTakeProfitLevelDTO
BoundaryNumericSourceDTO
BoundarySourceFieldsDTO
RuntimeKlineContextDTO
```

也未发现 `planboundary` 包。

结论：

```text
当前 main 不具备真实 source trace adapter 所需的 BoundaryCandidate / RuntimeKlineContext DTO 生产输入。
```

---

### 3.2 PlanBoundaryDisplayVO 当前存在形式

当前 main 中已存在：

```text
DashboardDetailResponseVO.PlanBoundaryDisplayVO
```

它是 `DashboardDetailResponseVO` 的内部静态类，字段包括：

- `planBoundaryStatus`
- `planBoundaryStatusLabel`
- `sourceTraceStatus`
- `backendConnectionStatus`
- `incompleteReasons`
- `blockingReasons`
- `manualReviewRequired`
- `notTradeInstruction`
- `updatedAt`

结论：

```text
当前可复用的是 dashboard display VO，不是真实 PlanBoundary source trace 输入对象。
```

---

## 4. 对 PR #59 / PR #60 的影响

由于当前 main 没有 BoundaryCandidate / RuntimeKlineContext 相关 DTO，后续 Java 最小实现不应直接写成：

```java
build(BoundaryCandidateDTO candidate, RuntimeKlineContextDTO context, ...)
```

否则会导致：

- 编译失败。
- 过早绑定不存在的生产链路。
- 将 display adapter 与尚未完成的 source trace 生产链路混在一起。

更合理的下一步是：

```text
PR #59：PlanBoundarySourceTraceAdapter 接口与默认实现方案
```

先定义两阶段策略：

1. 当前 main 可实现的 fail-closed adapter。
2. 等 BoundaryCandidate / RuntimeKlineContext DTO 真正在 main 出现后，再扩展真实输入签名。

---

## 5. 当前允许的 Java 最小实现方向

在现有 main 条件下，Java 最小实现只能是：

```text
fail-closed source trace readiness adapter
```

它可以做：

- 接收 symbol / DecisionResultVO / fallback PlanBoundaryDisplayVO。
- 检查当前是否存在可追溯 source trace。
- 因 source trace DTO 缺失，返回 INCOMPLETE 或 BACKEND_PENDING。
- 写入 incompleteReasons / blockingReasons。
- 保持 manualReviewRequired = true。
- 保持 notTradeInstruction = true。

它不能做：

- 返回 VALID。
- 生成 entry / stop / take profit。
- 从文本字段伪造 source trace。
- 接 order API。
- 接自动交易。

---

## 6. 必须保持 INCOMPLETE 的当前原因

当前 main 下，以下原因必须阻止 VALID：

```text
BOUNDARY_CANDIDATE_DTO_MISSING
RUNTIME_KLINE_CONTEXT_DTO_MISSING
NUMERIC_SOURCE_TRACE_MISSING
SOURCE_TRACE_INPUT_NOT_AVAILABLE
```

如果后续 adapter 输出 PlanBoundaryDisplay，则建议：

```text
planBoundaryStatus = INCOMPLETE
sourceTraceStatus = MISSING
backendConnectionStatus = PARTIAL 或 BACKEND_PENDING
manualReviewRequired = true
notTradeInstruction = true
```

---

## 7. 不允许使用的替代来源

当前 main 中存在一些文本字段，例如：

- `entryZone`
- `stopLoss`
- `takeProfitRules`
- `executionPlanSummary`

这些仍不允许作为真实 source trace。

原因：

```text
它们是展示文本或摘要，不携带 numeric source / timeframe / sourceReason / sourceRef / sourceField。
```

因此不能因为这些字段存在就把 PlanBoundaryDisplay 标记为 VALID。

---

## 8. 下一步建议

下一步建议进入：

```text
PR #59：PlanBoundarySourceTraceAdapter 接口与默认实现方案
```

PR #59 应继续 docs-only，明确：

- 当前 main 只允许 fail-closed readiness adapter。
- 不直接引用不存在的 BoundaryCandidateDTO / RuntimeKlineContextDTO。
- 后续 DTO 出现后再做签名升级。
- VALID 阶段必须另行 PR。

---

## 9. 明确不做什么

本阶段明确不做：

- 不改 Java。
- 不改 dashboard。
- 不改 schema。
- 不新增 mapper。
- 不接 RuleEngine。
- 不接 source assembler。
- 不生成真实 entry / stop / take profit。
- 不生成真实 ExecutionPlan。
- 不接 order API。
- 不自动交易。
- 不自动开仓 / 平仓 / 反手。

---

## 10. 今日阶段进度估算

按今天推进的目标轨道计算：

```text
Display adapter chain P0：100%
PlanBoundary real source trace adapter 方案启动：约 20%
今日整体推进：约 90% - 95%
```

说明：

- 已完成 display adapter chain 的方案、Java 最小实现、Controller 接入、API smoke、单测记录、总收口和本地验证反馈。
- 已启动真实 source trace 阶段，但该阶段目前仍在方案 / 只读范围确认阶段，尚未进入 Java 实现。

---

## 11. 本 PR 验收标准

本 PR 验收标准：

- 只新增一个文档：`docs/PHASE_PLAN_BOUNDARY_SOURCE_TRACE_ADAPTER_READONLY_SCOPE.md`。
- 无 Java 改动。
- 无 dashboard.html 改动。
- 无 schema 改动。
- 无 mapper / RuleEngine 改动。
- 无交易执行相关改动。
- 文档如实记录当前 main 未发现 BoundaryCandidate / RuntimeKlineContext DTO。
- 文档确认当前只可做 fail-closed source trace readiness adapter。
