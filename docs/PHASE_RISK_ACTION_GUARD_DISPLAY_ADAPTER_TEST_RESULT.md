# PHASE: RiskActionGuardDisplayAdapter 本地测试结果记录（PR46）

## 1. 阶段目标

本阶段记录 `DefaultRiskActionGuardDisplayAdapterTest` 在 PR #45 修复后的本地测试结果。

本 PR 只新增验证记录文档，不修改 Java、不修改 dashboard、不修改 schema、不接交易执行链路。

---

## 2. 背景

PR #43 新增了：

- `RiskActionGuardDisplayAdapter`
- `DefaultRiskActionGuardDisplayAdapter`
- `DefaultRiskActionGuardDisplayAdapterTest`

用户在执行本地测试时发现失败：

```text
DefaultRiskActionGuardDisplayAdapterTest.shouldNotTurnHighRiskIntoActionWithoutLiquidityContext
expected: LIQUIDITY_CONTEXT_MISSING
actual: MANUAL_REVIEW_REQUIRED
```

根因：

```text
adapter 在高风险判断前已经把 liquidityState 补为 BACKEND_PENDING，导致缺少流动性上下文判断未命中。
```

PR #45 已修复：

```text
将 liquidityState = BACKEND_PENDING 也视为缺少流动性上下文。
```

---

## 3. 用户本地验证状态

用户反馈：

```text
DefaultRiskActionGuardDisplayAdapterTest：通过
```

当前用户未提供完整终端明细，例如：

```text
Tests run: ...
Failures: ...
Errors: ...
BUILD SUCCESS
```

因此本 PR 只记录为：

```text
用户反馈通过；完整终端输出未提供。
```

---

## 4. 验证意义

该结果说明 PR #45 的修复方向有效：

- `BACKEND_PENDING` liquidity state 不再被误认为具备流动性上下文。
- HIGH / EXTREME 风险在缺少真实流动性上下文时，应进入人工复核并返回 `LIQUIDITY_CONTEXT_MISSING`。
- RiskActionGuardDisplayAdapter 仍保持 fail-closed。

---

## 5. 安全边界复核

当前阶段仍未进入交易执行：

- 未改 schema。
- 未新增 mapper。
- 未接 RuleEngine。
- 未接 source assembler。
- 未生成真实 entry / stop / take profit。
- 未生成真实交易执行计划。
- 未接任何交易执行链路。
- 未把风险高直接等于立即动作。

当前 RiskActionGuardDisplayAdapter 仍是 dashboard read-model fail-closed 拼装层。

---

## 6. 后续建议

建议下一步进入：

```text
PR #47：RiskActionGuardDisplayAdapter API smoke 验证记录
```

用户可同步最新 main 后执行 `/api/dashboard/detail` curl，确认 response 中 `riskActionGuardDisplay` 仍保持：

- `opportunityPushAllowed = false`
- `reverseTradeAllowed = false`
- `newPositionAllowed = false`
- `marketOrderExitAllowed = false`
- `manualRiskReviewRequired = true`
- `notTradeInstruction = true`

---

## 7. 本 PR 验收标准

本 PR 验收标准：

- 只新增一个文档：`docs/PHASE_RISK_ACTION_GUARD_DISPLAY_ADAPTER_TEST_RESULT.md`。
- 无 Java 改动。
- 无 dashboard.html 改动。
- 无 schema 改动。
- 无交易执行相关改动。
- 如实记录用户反馈 `DefaultRiskActionGuardDisplayAdapterTest` 已通过。
- 如实记录完整终端输出未提供。
