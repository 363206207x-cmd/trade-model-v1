# PHASE: PlanBoundarySourceTraceAdapter 接口与默认实现方案（PR59）

## 1. 阶段目标

本阶段定义 `PlanBoundarySourceTraceAdapter` 的接口与默认实现方案，为后续 Java 最小实现做准备。

本 PR 只做方案文档，不实现 Java 代码，不改 dashboard，不改 schema，不改 mapper，不改 RuleEngine，不接 order API，不自动交易。

核心目标：

- 在当前 main 缺少真实 BoundaryCandidate / RuntimeKlineContext DTO 的前提下，定义可编译、可回滚的 fail-closed readiness adapter。
- 明确当前阶段不得返回 VALID。
- 明确未来真实 DTO 出现后如何升级签名。
- 明确 source trace 缺失时必须保持 INCOMPLETE / MISSING。

---

## 2. 前置事实

PR #58 已确认当前远端 main 未发现以下对象：

```text
BoundaryCandidateDTO
BoundaryEntryDTO
BoundaryStopDTO
BoundaryTakeProfitLevelDTO
BoundaryNumericSourceDTO
BoundarySourceFieldsDTO
RuntimeKlineContextDTO
```

当前可复用的 display 对象为：

```text
DashboardDetailResponseVO.PlanBoundaryDisplayVO
```

它是 dashboard display VO，不是真实 source trace 输入对象。

---

## 3. 当前阶段设计原则

### 3.1 不引用不存在的 DTO

当前 Java 最小实现不得直接 import 或引用：

```text
BoundaryCandidateDTO
RuntimeKlineContextDTO
BoundaryNumericSourceDTO
```

原因：

```text
当前 main 未包含这些对象，直接引用会导致编译失败。
```

### 3.2 只做 readiness adapter

当前阶段的 `PlanBoundarySourceTraceAdapter` 只判断：

```text
真实 source trace 是否已具备接入条件
```

不是：

```text
真实边界计算器
真实 entry / stop / TP 生成器
真实 RuleEngine 接入层
```

### 3.3 默认 fail-closed

默认输出必须保持：

```text
planBoundaryStatus = INCOMPLETE 或 BACKEND_PENDING
sourceTraceStatus = MISSING 或 BACKEND_PENDING
manualReviewRequired = true
notTradeInstruction = true
```

当前阶段不允许输出：

```text
planBoundaryStatus = VALID
```

---

## 4. 建议接口

建议新增接口：

```text
src/main/java/org/example/trademodel/service/dashboard/PlanBoundarySourceTraceAdapter.java
```

建议方法签名：

```java
DashboardDetailResponseVO.PlanBoundaryDisplayVO build(
        String symbol,
        DecisionResultVO decision,
        DashboardDetailResponseVO.PlanBoundaryDisplayVO fallbackDisplay
);
```

说明：

- `symbol`：当前资产。
- `decision`：当前 dashboard detail 已有只读决策对象。
- `fallbackDisplay`：来自 `withSafeDefaultDisplays()` 或上游 display adapter。

当前不加入 BoundaryCandidate / RuntimeKlineContext 参数，避免引用不存在对象。

---

## 5. 默认实现建议

建议新增默认实现：

```text
src/main/java/org/example/trademodel/service/dashboard/DefaultPlanBoundarySourceTraceAdapter.java
```

职责：

- 复用 fallback display 对象。
- 保证 reason list 非空时可写。
- 在 decision 缺失时写入 `DECISION_MISSING`。
- 在当前真实 source trace 输入不可用时写入：
  - `SOURCE_TRACE_INPUT_NOT_AVAILABLE`
  - `BOUNDARY_CANDIDATE_DTO_MISSING`
  - `RUNTIME_KLINE_CONTEXT_DTO_MISSING`
- 输出 INCOMPLETE / MISSING。
- 保持人工复核与非交易指令。

不负责：

- 生成 entry。
- 生成 stop。
- 生成 take profit。
- 读取真实 K 线。
- 读取数据库。
- 调用 RuleEngine。
- 调用 order API。

---

## 6. 建议状态映射

### 6.1 decision 为空

输出建议：

```text
planBoundaryStatus = BACKEND_PENDING
sourceTraceStatus = BACKEND_PENDING
backendConnectionStatus = BACKEND_PENDING
blockingReasons = [DECISION_MISSING]
manualReviewRequired = true
notTradeInstruction = true
```

### 6.2 decision 存在但 source trace 输入不可用

输出建议：

```text
planBoundaryStatus = INCOMPLETE
planBoundaryStatusLabel = 信息不完整
sourceTraceStatus = MISSING
backendConnectionStatus = BACKEND_PENDING 或 PARTIAL
incompleteReasons = [SOURCE_TRACE_INPUT_NOT_AVAILABLE]
blockingReasons = [BOUNDARY_CANDIDATE_DTO_MISSING, RUNTIME_KLINE_CONTEXT_DTO_MISSING]
manualReviewRequired = true
notTradeInstruction = true
```

### 6.3 readModelTruthStatus = PARTIAL

如果 `decision.readModelTruthStatus = PARTIAL`，继续输出 INCOMPLETE，并保留 fallback reason：

```text
READ_MODEL_PARTIAL
LEGACY_MISSING:invalid_condition
```

### 6.4 文本字段存在

即使存在：

```text
entryZone
stopLoss
takeProfitRules
executionPlanSummary
```

也不得因此输出 VALID。

原因：

```text
这些字段不是 numeric source trace。
```

---

## 7. 建议测试文件

建议新增：

```text
src/test/java/org/example/trademodel/service/dashboard/DefaultPlanBoundarySourceTraceAdapterTest.java
```

测试建议：

1. 输入全为空时返回 BACKEND_PENDING。
2. fallback 对象被保留。
3. decision 为空时写入 DECISION_MISSING。
4. decision 存在但 source trace DTO 不可用时返回 INCOMPLETE。
5. readModelTruthStatus = PARTIAL 时继承 read model 缺失原因。
6. entryZone / stopLoss / takeProfitRules 存在时仍不返回 VALID。
7. manualReviewRequired 始终 true。
8. notTradeInstruction 始终 true。
9. 不生成真实 entry / stop / take profit。

---

## 8. 与现有 PlanBoundaryDisplayAdapter 的关系

当前已有：

```text
PlanBoundaryDisplayAdapter
DefaultPlanBoundaryDisplayAdapter
```

后续不建议直接大改现有 adapter。

建议顺序：

1. 先新增 `PlanBoundarySourceTraceAdapter`，只做 source trace readiness。
2. 再单独方案说明如何让 `DefaultPlanBoundaryDisplayAdapter` 调用它。
3. 再单独 Java PR 接入调用。
4. 最后 API smoke 验证。

这样可以避免一次性改大、难回滚、难定位问题。

---

## 9. 未来真实 DTO 出现后的升级方向

未来当 main 中出现真实 DTO 后，再单独升级接口，例如：

```java
DashboardDetailResponseVO.PlanBoundaryDisplayVO buildFromCandidate(
        BoundaryCandidateDTO boundaryCandidate,
        RuntimeKlineContextDTO runtimeKlineContext,
        DashboardDetailResponseVO.PlanBoundaryDisplayVO fallbackDisplay
);
```

升级条件：

- BoundaryCandidateDTO 已在 main 中存在并通过测试。
- RuntimeKlineContextDTO 已在 main 中存在并通过测试。
- BoundaryNumericSourceDTO 已在 main 中存在并可承载 source trace。
- 不再需要通过字符串推断数值来源。

该升级必须单独 PR，不能混入当前 fail-closed readiness adapter。

---

## 10. 明确不做什么

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
- 不把文本字段包装为真实 source trace。

---

## 11. 后续建议

下一步建议进入：

```text
PR #60：PlanBoundarySourceTraceAdapter Java 最小实现
```

实现边界应严格限制为新增 3 个文件：

```text
src/main/java/org/example/trademodel/service/dashboard/PlanBoundarySourceTraceAdapter.java
src/main/java/org/example/trademodel/service/dashboard/DefaultPlanBoundarySourceTraceAdapter.java
src/test/java/org/example/trademodel/service/dashboard/DefaultPlanBoundarySourceTraceAdapterTest.java
```

仍不接 Controller，不改现有 `DefaultPlanBoundaryDisplayAdapter`。

---

## 12. 本 PR 验收标准

本 PR 验收标准：

- 只新增一个文档：`docs/PHASE_PLAN_BOUNDARY_SOURCE_TRACE_ADAPTER_INTERFACE_PLAN.md`。
- 无 Java 改动。
- 无 dashboard.html 改动。
- 无 schema 改动。
- 无 mapper / RuleEngine 改动。
- 无交易执行相关改动。
- 文档明确当前接口不引用不存在 DTO。
- 文档明确当前默认实现只能 fail-closed。
- 文档明确 VALID 阶段必须后续单独 PR。
