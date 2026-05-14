# PHASE: PlanBoundaryDisplayAdapter 接口与默认实现方案（PR30）

## 1. 阶段目标

本阶段定义 `PlanBoundaryDisplayAdapter` 的接口与默认实现方案，为后续 Java 最小实现做准备。

本 PR 只做方案文档，不实现 Java 代码，不改 dashboard，不改 schema，不改 mapper，不改 service 业务逻辑，不改 RuleEngine，不接任何交易执行链路。

核心目标：

- 定义一个只读 display/read-model 拼装边界。
- 第一阶段只输出安全默认或不完整状态。
- 不依赖远端 main 当前不存在的 BoundaryCandidate 相关类。
- 不生成真实 entry / stop / take profit 数值。

---

## 2. 前置结论

PR #29 已确认，当前远端 main 未发现可直接复用的：

- `BoundaryCandidateDTO`
- `BoundaryCandidateService`
- `RuntimeKlineContextDTO`
- 完整 PlanBoundary 生产链路

当前可依赖的是：

- `DashboardDetailResponseVO.PlanBoundaryDisplayVO`
- `DashboardDetailResponseVO.withSafeDefaultDisplays()`
- `/api/dashboard/detail` 返回默认 display 对象
- dashboard 已读取 display status 字段

因此 PR #30 的方向应是先定义一个只读 adapter，而不是直接接真实边界生产链路。

---

## 3. 建议接口与包路径

建议后续 Java 最小实现新增：

```text
src/main/java/org/example/trademodel/service/dashboard/PlanBoundaryDisplayAdapter.java
src/main/java/org/example/trademodel/service/dashboard/DefaultPlanBoundaryDisplayAdapter.java
src/test/java/org/example/trademodel/service/dashboard/DefaultPlanBoundaryDisplayAdapterTest.java
```

说明：

- 放在 `service/dashboard` 下，强调 dashboard read-model 拼装层。
- 不放在 RuleEngine 或 ExecutionPlan 包中。
- 不新建 `planboundary` 生产链路包。

---

## 4. 接口职责

`PlanBoundaryDisplayAdapter` 第一阶段只负责：

- 接收 dashboard detail 所需上下文。
- 返回 `DashboardDetailResponseVO.PlanBoundaryDisplayVO`。
- 在真实来源不可用时保持安全默认。
- 根据已有 read model 字段做最小状态判断。
- 不把异常抛到页面展示层。

它不负责：

- 生成 BoundaryCandidate。
- 生成 entry / stop / take profit。
- 调用 RuleEngine。
- 调用 source assembler。
- 写数据库。
- 修改 schema。
- 触发任何交易执行。

---

## 5. 建议方法签名

建议第一阶段使用：

```java
DashboardDetailResponseVO.PlanBoundaryDisplayVO build(
        String symbol,
        DecisionResultVO decision,
        DashboardDetailResponseVO.PlanBoundaryDisplayVO fallbackDisplay
);
```

设计原因：

- `symbol`：基础上下文。
- `decision`：当前 dashboard detail 已有对象，可读取 `analysisId`、`readModelTruthStatus`、`readModelFallbackReason` 等只读字段。
- `fallbackDisplay`：来自 safe default helper 的默认对象，adapter 失败时直接返回。

第一阶段不输入真实 BoundaryCandidate、RuntimeKlineContext、RuleEngine 输出或交易执行请求。

---

## 6. 默认实现规则

### 6.1 fallback 为空

返回新的 `PlanBoundaryDisplayVO` 默认对象：

- `planBoundaryStatus = BACKEND_PENDING`
- `sourceTraceStatus = BACKEND_PENDING`
- `backendConnectionStatus = BACKEND_PENDING`
- `manualReviewRequired = true`
- `notTradeInstruction = true`

### 6.2 decision 为空

保持 `BACKEND_PENDING`，并在 `blockingReasons` 中记录：

```text
DECISION_MISSING
```

### 6.3 analysisId 为空

返回 `INCOMPLETE`，并记录：

```text
ANALYSIS_ID_MISSING
```

### 6.4 readModelTruthStatus = PARTIAL

返回 `INCOMPLETE`，并记录：

```text
READ_MODEL_PARTIAL
```

如存在 `readModelFallbackReason`，可加入原因列表。

### 6.5 只有文本执行计划字段

如果只有 `entryZone` / `stopLoss` / `takeProfitRules` / `executionPlanSummary` 等文本字段，不得映射为 VALID。

建议返回：

```text
INCOMPLETE 或 BACKEND_PENDING
SOURCE_TRACE_PENDING
```

---

## 7. 第一阶段允许输出的字段

只允许写入：

- `planBoundaryStatus`
- `planBoundaryStatusLabel`
- `sourceTraceStatus`
- `backendConnectionStatus`
- `incompleteReasons`
- `blockingReasons`
- `manualReviewRequired`
- `notTradeInstruction`
- `updatedAt`

第一阶段不新增价格字段，不生成真实边界数值。

---

## 8. 状态映射建议

| status | label | 说明 |
|---|---|---|
| BACKEND_PENDING | 后端未接入 | 默认安全状态 |
| INCOMPLETE | 信息不完整 | 缺上下文或缺来源 |
| WATCH_ONLY | 仅观察 | 只读观察，不包装为机会 |
| INVALID | 已失效 | 不包装为反向机会 |
| VALID | 完整但需人工复核 | 第一阶段原则上不主动输出 |

即使后续出现 VALID，也仍为人工复核信息，不是交易指令。

---

## 9. 测试建议

后续 Java 实现应测试：

1. `build(null, null, null)` 返回 BACKEND_PENDING。
2. fallback 非空时不破坏安全默认。
3. decision 为空时记录 `DECISION_MISSING`。
4. analysisId 为空时返回 INCOMPLETE。
5. readModelTruthStatus 为 PARTIAL 时返回 INCOMPLETE。
6. 有文本 entry / stop / TP 字段时不返回 VALID。
7. 输出始终 `manualReviewRequired = true`。
8. 输出始终 `notTradeInstruction = true`。
9. 不生成真实 entry / stop / take profit 数值。

---

## 10. 与 DashboardController 的关系

Controller 接入应单独 PR 处理。

建议顺序：

1. PR #31：只新增 adapter 接口、默认实现、测试，不改 Controller。
2. PR #32：Controller 注入 adapter，并在 `dashboardDetail` 中调用。
3. PR #33：本地 API smoke 验证。

Controller 接入时必须保持：

- 先创建 `withSafeDefaultDisplays()`。
- adapter 失败时保留 fallback。
- 不影响 decision / evidence / score / marketEnvironmentMini 原有链路。

---

## 11. 与 ExecutionPlanDisplayVO 的关系

本 adapter 只处理 PlanBoundaryDisplay。

它不应：

- 修改 `ExecutionPlanDisplayVO`。
- 把 ExecutionPlan 映射为可执行。
- 根据文本摘要生成执行计划。

ExecutionPlanDisplayVO 的状态映射应单独 PR。

---

## 12. 与 Risk Action Guard 的关系

本 adapter 第一阶段可在 `blockingReasons` 中记录风险阻断 code，但不接真实 Risk Action Guard 逻辑。

仍需遵守：

- 踩踏状态不包装为机会。
- 短线插针不等于趋势反转。
- 流动性恶化不建议市价一次性砍仓。
- 风控提示仍需人工复核。

---

## 13. 明确不做什么

本阶段和下一阶段默认实现仍不做：

- 不改 schema。
- 不新增 mapper。
- 不写数据库。
- 不接 RuleEngine。
- 不接 source assembler。
- 不引用当前远端 main 不存在的 BoundaryCandidate 相关类。
- 不生成真实 entry / stop / take profit。
- 不生成真实 ExecutionPlan。
- 不接任何交易执行链路。

---

## 14. 本 PR 验收标准

本 PR 验收标准：

- 只新增一个文档：`docs/PHASE_PLAN_BOUNDARY_DISPLAY_ADAPTER_DEFAULT_PLAN.md`。
- 无 Java 改动。
- 无 dashboard.html 改动。
- 无 schema 改动。
- 无 mapper / service / RuleEngine 改动。
- 无交易执行相关改动。
- 文档明确 adapter 接口职责。
- 文档明确默认实现 fail-closed。
- 文档明确不生成真实 entry / stop / take profit。
