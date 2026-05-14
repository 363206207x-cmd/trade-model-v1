# PHASE: PlanBoundary Real Source Trace Adapter 方案（PR57）

## 1. 阶段目标

本阶段定义 PlanBoundary real source trace adapter 的前置方案，为后续从 dashboard display adapter 外壳进入真实 read-model source trace 接入做准备。

本 PR 只做方案文档，不实现 Java 代码，不改 dashboard，不改 schema，不改 mapper，不改 RuleEngine，不接 order API，不自动交易。

核心目标：

- 明确真实 PlanBoundary source trace 应从哪里来。
- 明确什么时候仍必须保持 INCOMPLETE。
- 明确什么时候才允许 PlanBoundaryDisplay 从 BACKEND_PENDING / INCOMPLETE 升级。
- 明确 READY_REVIEW_ONLY 仍只是人工复核，不是交易指令。

---

## 2. 前置状态

Display adapter chain P0 已完成：

- PlanBoundaryDisplayAdapter
- ExecutionPlanDisplayAdapter
- RiskActionGuardDisplayAdapter
- PaperObservationDisplayAdapter
- DashboardController 接入
- API smoke
- adapter 单测
- controller 单测
- 总收口文档

当前链路仍是：

```text
dashboard read-model fail-closed 展示层
```

当前仍不是：

- 真实 PlanBoundary 生产链路。
- 真实 BoundaryCandidate source trace 链路。
- 真实 entry / stop / take profit 数值生成链路。
- 交易执行链路。

---

## 3. Source trace 的真实来源定义

后续真实 PlanBoundary source trace 不应来自普通文本字段。

### 3.1 不允许作为真实 source trace 的来源

以下字段不能作为真实 PlanBoundary source trace：

- `DecisionResultVO.entryZone`
- `DecisionResultVO.stopLoss`
- `DecisionResultVO.takeProfitRules`
- `DecisionResultVO.executionPlanSummary`
- `conclusionSummary`
- AI 自然语言摘要
- dashboard 文案
- 手工拼接字符串

这些字段最多只能作为展示摘要，不能证明数值边界的来源。

### 3.2 允许作为真实 source trace 的候选来源

真实 source trace 应来自结构化对象或可追溯 read-model，例如：

- BoundaryCandidateDTO
- BoundaryEntryDTO
- BoundaryStopDTO
- BoundaryTakeProfitLevelDTO
- BoundaryNumericSourceDTO
- BoundarySourceFieldsDTO
- RuntimeKlineContextDTO
- 未来真实 source assembler 输出
- 未来真实 PlanBoundary read model

每个数值边界必须可追溯到：

- sourceType
- sourceValue
- sourceTimeframe
- sourceReason
- sourceField
- sourceRef
- dataQualityScore
- runtime kline freshness
- blockingReasons / incompleteReasons

---

## 4. 必须 INCOMPLETE 的情况

以下任一情况出现时，PlanBoundaryDisplay 必须保持 INCOMPLETE 或 BACKEND_PENDING，不得升级为 VALID。

### 4.1 缺 source trace

必须 INCOMPLETE：

- 没有 BoundaryCandidate。
- 没有 sourceFields。
- entry / stop / take profit 中任意关键字段没有 numeric source。
- numeric source 缺 sourceTimeframe。
- numeric source 缺 sourceReason。
- numeric source 缺 sourceRef 或 sourceField。

### 4.2 缺 runtime kline context

必须 INCOMPLETE：

- RuntimeKlineContextDTO 缺失。
- kline window 缺失。
- OHLCV 缺失。
- klineItems 为空。
- runtime kline stale。
- timeframe 不匹配。

### 4.3 数据质量不足

必须 INCOMPLETE：

- dataQualityScore 缺失。
- dataQualityScore < 70。
- 数据源延迟 / 缺失 / 异常未解释。
- readModelTruthStatus = PARTIAL 且 fallbackReason 未解决。

### 4.4 边界对象不完整

必须 INCOMPLETE：

- entry 缺失。
- stop 缺失。
- takeProfitLevels 为空。
- stop 与 entry 方向冲突。
- RR 规则缺失。
- TP 分配 / partial ratio 缺失。
- invalidation 条件缺失或无法追踪。

### 4.5 风控约束未通过

必须 INCOMPLETE 或保持 review-only：

- RiskActionGuard 未接入。
- liquidityState 缺失。
- stampedeDetected = true。
- wickOnlyRisk = true 且未多周期确认。
- PlanBoundary 与 ExecutionPlan 未对齐。

---

## 5. 允许升级为 VALID 的最低条件

PlanBoundaryDisplay 只有在以下条件全部满足时，才允许从 INCOMPLETE 升级到 VALID。

### 5.1 BoundaryCandidate 条件

必须满足：

- BoundaryCandidateDTO 存在。
- boundaryStatus = VALID。
- entry 非空。
- stop 非空。
- takeProfitLevels 非空。
- sourceFields 非空。
- dataQualityScore >= 70。
- manualReviewRequired = true。
- notTradeInstruction = true。

### 5.2 Source trace 条件

必须满足：

- entry 有明确 numeric source。
- stop 有明确 numeric source。
- 每个 TP level 有明确 numeric source 或 sourceTimeframe/sourceRef。
- 所有 source trace 可展示。
- source trace 与 RuntimeKlineContext 的 timeframe 不冲突。

### 5.3 Runtime context 条件

必须满足：

- RuntimeKlineContextDTO status = FRESH。
- klineItems 非空。
- OHLCV 完整。
- timeframe 与候选边界一致。
- staleStatus 不阻断。

### 5.4 Safety 条件

必须满足：

- manualReviewRequired = true。
- notTradeInstruction = true。
- 不生成 order payload。
- 不自动开仓。
- 不自动平仓。
- 不自动反手。

---

## 6. 与 ExecutionPlanDisplay 的关系

PlanBoundaryDisplay = VALID 时，只允许 ExecutionPlanDisplay 进入：

```text
READY_REVIEW_ONLY
```

仍不得进入：

```text
AUTO_EXECUTABLE
ORDER_READY
TRADE_READY
```

即使 PlanBoundary VALID，也只能说明：

```text
边界来源可追溯，可供人工复核。
```

不能说明：

```text
可以自动交易。
```

---

## 7. Adapter 设计建议

建议后续 Java 最小实现不是直接改现有 DefaultPlanBoundaryDisplayAdapter，而是先新增真实 source trace adapter：

```text
PlanBoundarySourceTraceAdapter
DefaultPlanBoundarySourceTraceAdapter
DefaultPlanBoundarySourceTraceAdapterTest
```

职责：

- 输入 BoundaryCandidateDTO / RuntimeKlineContextDTO / fallback display。
- 输出 PlanBoundaryDisplayVO。
- 缺任意关键 source trace 时返回 INCOMPLETE。
- 满足 VALID 最低条件时返回 VALID，但仍保留人工复核和非交易指令。

不建议本阶段直接：

- 修改 schema。
- 接 mapper。
- 接 RuleEngine。
- 接 order API。
- 生成真实执行计划。

---

## 8. 后续实现顺序建议

建议后续按以下 PR 顺序推进：

1. PR #58：PlanBoundarySourceTraceAdapter Java 最小实现前只读范围确认。
2. PR #59：PlanBoundarySourceTraceAdapter 接口与默认实现方案。
3. PR #60：PlanBoundarySourceTraceAdapter Java 最小实现。
4. PR #61：PlanBoundaryDisplayAdapter 调用 source trace adapter 的方案。
5. PR #62：Dashboard detail API smoke 验证。

每一步继续保持：

```text
小范围、可回滚、先文档、再 Java、最后 smoke。
```

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

## 10. 本 PR 验收标准

本 PR 验收标准：

- 只新增一个文档：`docs/PHASE_PLAN_BOUNDARY_REAL_SOURCE_TRACE_ADAPTER_PLAN.md`。
- 无 Java 改动。
- 无 dashboard.html 改动。
- 无 schema 改动。
- 无 mapper / RuleEngine 改动。
- 无交易执行相关改动。
- 文档明确真实 source trace 从哪里来。
- 文档明确什么时候仍必须 INCOMPLETE。
- 文档明确 READY_REVIEW_ONLY 不是交易指令。
