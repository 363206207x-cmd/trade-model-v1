# PHASE: PaperObservationDisplayAdapter 方案（PR48）

## 1. 阶段目标

本阶段定义 `PaperObservationDisplayAdapter` 的接口与默认实现方案，为后续 Java 最小实现做准备。

本 PR 只做方案文档，不实现 Java 代码，不改 dashboard，不改 schema，不改 mapper，不改 RuleEngine，不接任何交易执行链路。

核心目标：

- 定义一个只读 dashboard display adapter。
- 将纸面观察 / 人工复盘入口保持 fail-closed。
- 明确纸面观察不是实盘持仓、不是交易指令、不是一键执行。
- 防止纸面记录被误同步为真实仓位或真实盈亏。

---

## 2. 当前已完成基础

当前已完成：

- `DashboardDetailResponseVO.PaperObservationDisplayVO`
- `DashboardDetailResponseVO.withSafeDefaultDisplays()`
- `/api/dashboard/detail` 已返回默认 `paperObservationDisplay`。
- dashboard 已读取 display status 字段。
- PlanBoundaryDisplayAdapter 已完成 fail-closed Java 最小实现与 Controller 接入。
- ExecutionPlanDisplayAdapter 已完成 fail-closed Java 最小实现与 Controller 接入。
- RiskActionGuardDisplayAdapter 已完成 fail-closed Java 最小实现与 Controller 接入。

下一步应为 PaperObservationDisplay 建立同样的只读 adapter 链路。

---

## 3. Adapter 定位

建议新增：

```text
src/main/java/org/example/trademodel/service/dashboard/PaperObservationDisplayAdapter.java
src/main/java/org/example/trademodel/service/dashboard/DefaultPaperObservationDisplayAdapter.java
src/test/java/org/example/trademodel/service/dashboard/DefaultPaperObservationDisplayAdapterTest.java
```

定位：

```text
dashboard read-model display adapter
```

它负责：

- 根据当前可见上下文输出纸面观察展示状态。
- 保持默认 `BACKEND_PENDING` 或 fail-closed flags。
- 输出非实盘持仓、非交易指令、人工复核标记。
- 为后续人工复盘 / 纸面观察入口打安全基础。

它不负责：

- 创建真实持仓。
- 创建真实订单。
- 自动同步纸面观察为实盘仓位。
- 写数据库。
- 修改 schema。
- 生成真实交易执行计划。
- 计算真实盈亏。

---

## 4. 建议接口签名

建议第一阶段接口：

```java
DashboardDetailResponseVO.PaperObservationDisplayVO build(
        DecisionResultVO decision,
        DashboardDetailResponseVO.PlanBoundaryDisplayVO planBoundaryDisplay,
        DashboardDetailResponseVO.ExecutionPlanDisplayVO executionPlanDisplay,
        DashboardDetailResponseVO.RiskActionGuardDisplayVO riskActionGuardDisplay,
        DashboardDetailResponseVO.PaperObservationDisplayVO fallbackDisplay
);
```

说明：

- `decision`：提供当前资产、analysisId、read model 完整性等只读上下文。
- `planBoundaryDisplay`：提供边界状态。
- `executionPlanDisplay`：提供执行计划展示状态。
- `riskActionGuardDisplay`：提供风险动作安全状态。
- `fallbackDisplay`：来自 safe default helper，adapter 失败时保留。

第一阶段不输入真实订单对象、不输入真实持仓对象、不输入交易执行请求。

---

## 5. 第一阶段允许写入字段

只允许写入 `PaperObservationDisplayVO` 的以下字段：

- `paperObservationStatus`
- `paperObservationStatusLabel`
- `paperObservationAvailable`
- `manualReviewEntryAvailable`
- `linkedPaperObservationCount`
- `linkedReviewCount`
- `missedOpportunityFlag`
- `reviewSummary`
- `notRealPosition`
- `notTradeInstruction`
- `manualReviewRequired`
- `backendConnectionStatus`
- `updatedAt`

第一阶段不新增交易执行字段。

---

## 6. 默认安全规则

### 6.1 fallback 为空

返回新的 `PaperObservationDisplayVO` 默认对象：

- `paperObservationStatus = BACKEND_PENDING`
- `paperObservationStatusLabel = 后端未接入`
- `paperObservationAvailable = false`
- `manualReviewEntryAvailable = false`
- `linkedPaperObservationCount = 0`
- `linkedReviewCount = 0`
- `missedOpportunityFlag = false`
- `notRealPosition = true`
- `notTradeInstruction = true`
- `manualReviewRequired = true`
- `backendConnectionStatus = BACKEND_PENDING`

### 6.2 decision 为空

保持 `BACKEND_PENDING`。

不得创建纸面观察入口。

### 6.3 PlanBoundary 非 VALID

如果 PlanBoundary 未 VALID：

```text
paperObservationAvailable = false
manualReviewEntryAvailable = false
notRealPosition = true
notTradeInstruction = true
```

### 6.4 ExecutionPlan 非 READY_REVIEW_ONLY

如果 ExecutionPlan 不是 `READY_REVIEW_ONLY`：

```text
paperObservationStatus = BACKEND_PENDING 或 MANUAL_REVIEW_REQUIRED
paperObservationAvailable = false
manualReviewEntryAvailable = false
```

### 6.5 RiskActionGuard 阻断状态

如果风险动作保护器仍为 BACKEND_PENDING 或存在阻断原因：

```text
paperObservationAvailable = false
manualReviewEntryAvailable = false
```

第一阶段不把 risk blocked 状态包装为机会或复盘入口。

---

## 7. 建议状态值

| status | label | 说明 |
|---|---|---|
| BACKEND_PENDING | 后端未接入 | 默认状态 |
| PLACEHOLDER_ONLY | 仅占位 | 只展示说明，不可写 |
| MANUAL_REVIEW_REQUIRED | 需要人工复核 | 可作为后续人工复盘入口前置 |
| AVAILABLE_REVIEW_ONLY | 可纸面观察 | 只允许纸面记录，不代表实盘 |
| UNAVAILABLE | 暂不可用 | 上下文不足或风险阻断 |

注意：

- `AVAILABLE_REVIEW_ONLY` 不等于实盘交易。
- 第一阶段原则上不主动输出 AVAILABLE_REVIEW_ONLY，除非后续真实 review backend 可用。

---

## 8. 与 PlanBoundaryDisplay 的关系

PaperObservationDisplay 不应覆盖 PlanBoundaryDisplay。

它可以读取 PlanBoundary 状态，用于决定入口是否可展示：

- PlanBoundary BACKEND_PENDING：默认不可用。
- PlanBoundary INCOMPLETE：默认不可用。
- PlanBoundary WATCH_ONLY：可作为未来纸面观察候选，但第一阶段仍默认不可用。
- PlanBoundary INVALID：不可用。
- PlanBoundary VALID：仍需经过 ExecutionPlan 与 RiskActionGuard。

---

## 9. 与 ExecutionPlanDisplay 的关系

PaperObservationDisplay 不应覆盖 ExecutionPlanDisplay。

它可以读取 ExecutionPlan 状态，用于防止纸面观察被误写成真实执行：

- ExecutionPlan BOUNDARY_PENDING：不可用。
- ExecutionPlan INCOMPLETE：不可用。
- ExecutionPlan WATCH_ONLY：未来可考虑纸面观察，但第一阶段默认不可写。
- ExecutionPlan INVALID：不可用。
- ExecutionPlan READY_REVIEW_ONLY：仍需 RiskActionGuard 通过人工复核。

---

## 10. 与 RiskActionGuardDisplay 的关系

PaperObservationDisplay 不应绕过 RiskActionGuard。

规则：

- RiskActionGuard BACKEND_PENDING：默认不可用。
- RiskActionGuard 有 blockingReason：默认不可用。
- RiskActionGuard 标记踩踏：不可用。
- RiskActionGuard 标记短线插针：不包装为趋势反转记录。
- RiskActionGuard 仍为人工复核：只可作为未来人工复盘入口前置，不自动写入。

---

## 11. 测试建议

后续 Java 最小实现应测试：

1. 输入全为空时返回 BACKEND_PENDING。
2. decision 为空时保持不可用。
3. 默认 `paperObservationAvailable = false`。
4. 默认 `manualReviewEntryAvailable = false`。
5. 默认 `notRealPosition = true`。
6. 默认 `notTradeInstruction = true`。
7. 默认 `manualReviewRequired = true`。
8. PlanBoundary 非 VALID 时不可用。
9. ExecutionPlan 非 READY_REVIEW_ONLY 时不可用。
10. RiskActionGuard 有 blocking reason 时不可用。
11. 纸面观察不会生成真实持仓或真实执行语义。

---

## 12. Controller 接入建议

Controller 接入应单独 PR 处理。

建议顺序：

1. PR #49：PaperObservationDisplayAdapter Java 最小实现，不改 Controller。
2. PR #50：DashboardController 接入 PaperObservationDisplayAdapter。
3. PR #51：本地 API smoke 验证。

Controller 顺序建议：

```text
withSafeDefaultDisplays()
↓
PlanBoundaryDisplayAdapter
↓
ExecutionPlanDisplayAdapter
↓
RiskActionGuardDisplayAdapter
↓
PaperObservationDisplayAdapter
```

---

## 13. 明确不做什么

本阶段和后续最小实现仍不做：

- 不改 schema。
- 不新增 mapper。
- 不写数据库。
- 不接 RuleEngine。
- 不接 source assembler。
- 不生成真实 entry / stop / take profit。
- 不生成真实交易执行计划。
- 不接任何交易执行链路。
- 不把纸面观察同步为真实持仓。
- 不把纸面观察收益作为真实盈亏。

---

## 14. 本 PR 验收标准

本 PR 验收标准：

- 只新增一个文档：`docs/PHASE_PAPER_OBSERVATION_DISPLAY_ADAPTER_PLAN.md`。
- 无 Java 改动。
- 无 dashboard.html 改动。
- 无 schema 改动。
- 无 mapper / RuleEngine 改动。
- 无交易执行相关改动。
- 文档明确 PaperObservationDisplayAdapter 只做只读展示拼装。
- 文档明确 fail-closed 安全默认值。
- 文档明确纸面观察不是实盘持仓、不是交易指令。
