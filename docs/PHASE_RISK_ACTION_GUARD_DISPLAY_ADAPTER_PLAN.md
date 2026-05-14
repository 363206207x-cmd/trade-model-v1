# PHASE: RiskActionGuardDisplayAdapter 方案（PR42）

## 1. 阶段目标

本阶段定义 `RiskActionGuardDisplayAdapter` 的接口与默认实现方案，为后续 Java 最小实现做准备。

本 PR 只做方案文档，不实现 Java 代码，不改 dashboard，不改 schema，不改 mapper，不改 RuleEngine，不接任何交易执行链路。

核心目标：

- 定义一个只读 dashboard display adapter。
- 将 Risk Action Guard 的展示状态保持 fail-closed。
- 明确风险动作分层只作为人工复核提示。
- 防止高风险提示被误解为立即止损、立即反手、立即开仓或自动动作。

---

## 2. 当前已完成基础

当前已完成：

- `DashboardDetailResponseVO.RiskActionGuardDisplayVO`
- `DashboardDetailResponseVO.withSafeDefaultDisplays()`
- `/api/dashboard/detail` 已返回默认 riskActionGuardDisplay。
- dashboard 已读取 riskActionGuardDisplay 字段。
- PlanBoundaryDisplayAdapter 已完成 fail-closed Java 最小实现与 Controller 接入。
- ExecutionPlanDisplayAdapter 已完成 fail-closed Java 最小实现与 Controller 接入。

下一步应为 RiskActionGuardDisplay 建立同样的只读 adapter 链路。

---

## 3. Adapter 定位

建议新增：

```text
src/main/java/org/example/trademodel/service/dashboard/RiskActionGuardDisplayAdapter.java
src/main/java/org/example/trademodel/service/dashboard/DefaultRiskActionGuardDisplayAdapter.java
src/test/java/org/example/trademodel/service/dashboard/DefaultRiskActionGuardDisplayAdapterTest.java
```

定位：

```text
dashboard read-model display adapter
```

它负责：

- 根据当前可见上下文输出风险动作展示状态。
- 保持默认 `BACKEND_PENDING` 或 fail-closed flags。
- 输出人工复核与非交易指令标记。
- 不把风险高直接翻译为交易动作。

它不负责：

- 执行下单。
- 自动开仓。
- 自动平仓。
- 自动反手。
- 生成真实止损 / 止盈 / 仓位调整。
- 修改 PlanBoundary 或 ExecutionPlan 原始业务对象。
- 写数据库或改 schema。

---

## 4. 建议接口签名

建议第一阶段接口：

```java
DashboardDetailResponseVO.RiskActionGuardDisplayVO build(
        DecisionResultVO decision,
        DashboardDetailResponseVO.PlanBoundaryDisplayVO planBoundaryDisplay,
        DashboardDetailResponseVO.ExecutionPlanDisplayVO executionPlanDisplay,
        DashboardDetailResponseVO.RiskActionGuardDisplayVO fallbackDisplay
);
```

说明：

- `decision`：提供风险等级、read model 完整性、文本摘要等现有只读字段。
- `planBoundaryDisplay`：提供边界状态。
- `executionPlanDisplay`：提供执行计划展示状态。
- `fallbackDisplay`：来自 safe default helper，adapter 失败时保留。

第一阶段不输入真实持仓动作对象、不输入交易请求、不输入 order 相关对象。

---

## 5. 第一阶段允许写入字段

只允许写入 `RiskActionGuardDisplayVO` 的以下字段：

- `riskActionGuardStatus`
- `riskActionGuardStatusLabel`
- `riskActionAdvice`
- `riskActionBlockingReason`
- `liquidityState`
- `stampedeDetected`
- `wickOnlyRisk`
- `opportunityPushAllowed`
- `reverseTradeAllowed`
- `newPositionAllowed`
- `marketOrderExitAllowed`
- `manualRiskReviewRequired`
- `notTradeInstruction`
- `updatedAt`

第一阶段不新增交易执行字段。

---

## 6. 默认安全规则

### 6.1 fallback 为空

返回新的 `RiskActionGuardDisplayVO` 默认对象：

- `riskActionGuardStatus = BACKEND_PENDING`
- `riskActionGuardStatusLabel = 后端未接入`
- `liquidityState = BACKEND_PENDING`
- `opportunityPushAllowed = false`
- `reverseTradeAllowed = false`
- `newPositionAllowed = false`
- `marketOrderExitAllowed = false`
- `manualRiskReviewRequired = true`
- `notTradeInstruction = true`

### 6.2 decision 为空

保持 `BACKEND_PENDING`，并可设置：

```text
riskActionBlockingReason = DECISION_MISSING
```

### 6.3 riskLevel 高但无流动性上下文

不得直接输出动作。

建议：

```text
riskActionGuardStatus = BACKEND_PENDING 或 MANUAL_REVIEW_REQUIRED
riskActionBlockingReason = LIQUIDITY_CONTEXT_MISSING
```

### 6.4 PlanBoundary 非 VALID

如果 PlanBoundary 未 VALID，风险动作展示应保持谨慎：

```text
opportunityPushAllowed = false
reverseTradeAllowed = false
newPositionAllowed = false
```

### 6.5 ExecutionPlan 非 READY_REVIEW_ONLY

如果 ExecutionPlan 不是 `READY_REVIEW_ONLY`，不得出现可执行语义。

---

## 7. 四类风险动作分层展示规则

### 7.1 风险高但流动性正常

展示语义：

- 可提示减仓。
- 可提示移动止损。
- 可提示降低杠杆。

限制：

- 只是建议语义。
- 必须人工复核。
- 不自动执行。

### 7.2 风险高且流动性恶化

展示语义：

- 不建议市价一次性退出。
- 可提示分批降风险。
- 可提示等待流动性恢复。
- 可提示只降杠杆。

限制：

- 不鼓励踩踏式错误执行。
- 不自动平仓。

### 7.3 风险高且存在踩踏

硬约束：

- 禁止反手。
- 禁止新开仓。
- 禁止机会推送。
- 先保护本金。

字段建议：

```text
opportunityPushAllowed = false
reverseTradeAllowed = false
newPositionAllowed = false
marketOrderExitAllowed = false
```

### 7.4 风险高但仅短线插针

展示语义：

- 不直接判定趋势反转。
- 不生成反向开仓计划。
- 只做短线风险提醒。
- 等待多周期确认。

限制：

- 不允许反手语义。
- 不允许强反转自动判定。

---

## 8. 建议状态值

| status | label | 说明 |
|---|---|---|
| BACKEND_PENDING | 后端未接入 | 默认状态 |
| NORMAL | 正常观察 | 未触发风险动作保护 |
| HIGH_RISK_LIQUIDITY_NORMAL | 高风险：流动性正常 | 可提示降风险 |
| HIGH_RISK_LIQUIDITY_DEGRADED | 高风险：流动性恶化 | 禁止市价一次性退出语义 |
| STAMPEDE_LOCK | 踩踏锁定 | 禁止反手、新开仓、机会推送 |
| WICK_ONLY_CONFIRMATION_REQUIRED | 短线插针，等待确认 | 不判定趋势反转 |
| MANUAL_REVIEW_REQUIRED | 需要人工复核 | 默认人工判断 |

---

## 9. 与 PlanBoundaryDisplay 的关系

RiskActionGuardDisplay 不应覆盖 PlanBoundaryDisplay。

它可以读取 PlanBoundary 状态，用于决定展示是否更保守：

- PlanBoundary BACKEND_PENDING：默认 fail-closed。
- PlanBoundary INCOMPLETE：不得允许机会推送。
- PlanBoundary WATCH_ONLY：不得允许新开仓语义。
- PlanBoundary INVALID：不得包装为反向机会。
- PlanBoundary VALID：仍需经过人工复核。

---

## 10. 与 ExecutionPlanDisplay 的关系

RiskActionGuardDisplay 不应覆盖 ExecutionPlanDisplay。

它可以读取 ExecutionPlan 状态，用于防止执行语义越界：

- ExecutionPlan BOUNDARY_PENDING：禁止可执行语义。
- ExecutionPlan INCOMPLETE：禁止可执行语义。
- ExecutionPlan WATCH_ONLY：只观察。
- ExecutionPlan INVALID：不得包装为反向计划。
- ExecutionPlan READY_REVIEW_ONLY：仍需人工复核，不自动执行。

---

## 11. 测试建议

后续 Java 最小实现应测试：

1. 输入全为空时返回 BACKEND_PENDING。
2. decision 为空时记录 DECISION_MISSING。
3. 默认 allow flags 全部 false。
4. manualRiskReviewRequired 始终 true。
5. notTradeInstruction 始终 true。
6. PlanBoundary 非 VALID 时不允许 opportunityPush / reverseTrade / newPosition。
7. ExecutionPlan 非 READY_REVIEW_ONLY 时不允许可执行语义。
8. STAMPEDE_LOCK 状态下三类允许标记全部 false。
9. WICK_ONLY_CONFIRMATION_REQUIRED 不允许反手。

---

## 12. Controller 接入建议

Controller 接入应单独 PR 处理。

建议顺序：

1. PR #43：RiskActionGuardDisplayAdapter Java 最小实现，不改 Controller。
2. PR #44：DashboardController 接入 RiskActionGuardDisplayAdapter。
3. PR #45：本地 API smoke 验证。

Controller 顺序建议：

```text
withSafeDefaultDisplays()
↓
PlanBoundaryDisplayAdapter
↓
ExecutionPlanDisplayAdapter
↓
RiskActionGuardDisplayAdapter
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
- 不把风险高直接等于立即动作。

---

## 14. 本 PR 验收标准

本 PR 验收标准：

- 只新增一个文档：`docs/PHASE_RISK_ACTION_GUARD_DISPLAY_ADAPTER_PLAN.md`。
- 无 Java 改动。
- 无 dashboard.html 改动。
- 无 schema 改动。
- 无 mapper / RuleEngine 改动。
- 无交易执行相关改动。
- 文档明确 RiskActionGuardDisplayAdapter 只做只读展示拼装。
- 文档明确 fail-closed 安全默认值。
- 文档明确踩踏、插针、流动性恶化的动作边界。
