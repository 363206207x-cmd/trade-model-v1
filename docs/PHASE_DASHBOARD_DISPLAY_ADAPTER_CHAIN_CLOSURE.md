# PHASE: Dashboard Display Adapter Chain 总收口文档（PR54）

## 1. 阶段目标

本阶段对 dashboard display adapter chain 进行总收口，覆盖 PR #28 到 PR #53 的方案、实现、Controller 接入、API smoke 与本地测试记录。

本 PR 只新增收口文档，不修改 Java、不修改 dashboard、不修改 schema、不接交易执行链路。

---

## 2. 当前完成链路

当前 `/api/dashboard/detail` 的 display 链路已经形成：

```text
DashboardDetailResponseVO.withSafeDefaultDisplays()
↓
PlanBoundaryDisplayAdapter
↓
ExecutionPlanDisplayAdapter
↓
RiskActionGuardDisplayAdapter
↓
PaperObservationDisplayAdapter
↓
DashboardController.dashboardDetail
↓
/api/dashboard/detail
↓
dashboard.html display status 展示
```

当前链路定位：

```text
dashboard read-model fail-closed 展示层
```

不是：

- 真实 PlanBoundary 生产链路。
- 真实 ExecutionPlan 生产链路。
- 真实 Risk Action 自动动作链路。
- 真实纸面观察写入链路。
- 交易执行链路。

---

## 3. PR 范围汇总

### 3.1 PlanBoundaryDisplayAdapter 阶段

已完成：

- PR #28：真实 PlanBoundary adapter 接入前方案。
- PR #29：Java 实现前只读范围确认。
- PR #30：PlanBoundaryDisplayAdapter 接口与默认实现方案。
- PR #31：PlanBoundaryDisplayAdapter Java 最小实现。
- PR #32：DashboardController 接入 PlanBoundaryDisplayAdapter。
- PR #33：修复 DashboardControllerTest 构造参数。
- PR #34：PlanBoundary adapter API smoke 记录。
- PR #35：PlanBoundary adapter 单测结果记录。

当前能力：

- `planBoundaryDisplay` 已由 adapter 生成。
- decision 缺失时 fail-closed。
- readModelTruthStatus = PARTIAL 时映射为 INCOMPLETE。
- 文本 entry / stop / take profit 不会被包装为真实边界。
- `manualReviewRequired = true`。
- `notTradeInstruction = true`。

---

### 3.2 ExecutionPlanDisplayAdapter 阶段

已完成：

- PR #36：ExecutionPlanDisplay 与 PlanBoundaryDisplay 状态映射方案。
- PR #37：ExecutionPlanDisplayAdapter Java 最小实现。
- PR #38：DashboardController 接入 ExecutionPlanDisplayAdapter。
- PR #39：ExecutionPlan display API smoke 记录。
- PR #40：ExecutionPlan adapter 单测结果记录。
- PR #41：DashboardControllerTest 结果记录。

当前能力：

- `executionPlanDisplay` 受 `planBoundaryDisplay` 约束。
- PlanBoundary BACKEND_PENDING -> ExecutionPlan BOUNDARY_PENDING。
- PlanBoundary INCOMPLETE -> ExecutionPlan INCOMPLETE。
- PlanBoundary WATCH_ONLY -> ExecutionPlan WATCH_ONLY。
- PlanBoundary INVALID -> ExecutionPlan INVALID。
- PlanBoundary VALID -> ExecutionPlan READY_REVIEW_ONLY。
- READY_REVIEW_ONLY 仍只是人工复核，不是交易指令。
- `executionPlanBoundaryAligned = false` 默认保持谨慎。
- 不生成真实 entry / stop / take profit。

---

### 3.3 RiskActionGuardDisplayAdapter 阶段

已完成：

- PR #42：RiskActionGuardDisplayAdapter 方案。
- PR #43：RiskActionGuardDisplayAdapter Java 最小实现。
- PR #44：DashboardController 接入 RiskActionGuardDisplayAdapter。
- PR #45：修复 BACKEND_PENDING 流动性上下文判断。
- PR #46：RiskActionGuard adapter 测试结果记录。
- PR #47：RiskActionGuard API smoke 记录。

当前能力：

- `riskActionGuardDisplay` 已由 adapter 生成。
- 默认 fail-closed。
- `opportunityPushAllowed = false`。
- `reverseTradeAllowed = false`。
- `newPositionAllowed = false`。
- `marketOrderExitAllowed = false`。
- `manualRiskReviewRequired = true`。
- `notTradeInstruction = true`。
- PlanBoundary 非 VALID 时阻断。
- ExecutionPlan 非 READY_REVIEW_ONLY 时阻断。
- HIGH / EXTREME 风险且流动性上下文缺失时返回 `LIQUIDITY_CONTEXT_MISSING`。

关键安全修复：

```text
liquidityState = BACKEND_PENDING 也视为缺少真实流动性上下文。
```

---

### 3.4 PaperObservationDisplayAdapter 阶段

已完成：

- PR #48：PaperObservationDisplayAdapter 方案。
- PR #49：PaperObservationDisplayAdapter Java 最小实现。
- PR #50：DashboardController 接入 PaperObservationDisplayAdapter。
- PR #51：PaperObservation API smoke 记录。
- PR #52：PaperObservation adapter 单测结果记录。
- PR #53：DashboardControllerTest 结果记录。

当前能力：

- `paperObservationDisplay` 已由 adapter 生成。
- 默认 fail-closed。
- `paperObservationAvailable = false`。
- `manualReviewEntryAvailable = false`。
- `notRealPosition = true`。
- `notTradeInstruction = true`。
- `manualReviewRequired = true`。
- PlanBoundary 非 VALID 时不可用。
- ExecutionPlan 非 READY_REVIEW_ONLY 时不可用。
- RiskActionGuard 有 blocking reason 时不可用。
- 纸面观察不创建真实持仓。
- 纸面观察不创建交易指令。

---

## 4. 已验证结果汇总

### 4.1 API smoke 已记录

已记录 API smoke：

- PlanBoundaryDisplay API smoke。
- ExecutionPlanDisplay API smoke。
- RiskActionGuardDisplay API smoke。
- PaperObservationDisplay API smoke。

关键共同结论：

```text
/api/dashboard/detail 可返回四个 display 对象。
四个 display 对象保持 fail-closed。
页面展示字段不依赖写死占位。
```

### 4.2 单测已记录

已记录单测：

- `DefaultPlanBoundaryDisplayAdapterTest` PASS。
- `DefaultExecutionPlanDisplayAdapterTest` PASS。
- `DefaultRiskActionGuardDisplayAdapterTest` 用户反馈 PASS，完整输出未提供。
- `DefaultPaperObservationDisplayAdapterTest` PASS。
- `DashboardControllerTest` PASS。

其中已提供完整测试明细的包括：

- PlanBoundary adapter test。
- ExecutionPlan adapter test。
- PaperObservation adapter test。
- DashboardControllerTest。

RiskActionGuard adapter test 在 PR #46 中按用户反馈记录为通过，但完整终端明细未提供。

---

## 5. 安全边界总复核

当前链路明确没有做：

- 不接 order API。
- 不自动交易。
- 不自动开仓。
- 不自动平仓。
- 不自动反手。
- 不生成真实 entry / stop / take profit。
- 不生成真实 ExecutionPlan。
- 不写数据库。
- 不改 schema。
- 不新增 mapper。
- 不接 RuleEngine。
- 不接 source assembler。
- 不把纸面观察同步为真实持仓。
- 不把纸面观察收益作为真实盈亏。

当前所有 display adapter 的共同默认语义：

```text
人工复核
非交易指令
fail-closed
只读展示
```

---

## 6. Risk Action Guard 关键边界确认

当前链路已固化以下展示边界：

- 风险高不能直接等于立即止损。
- 风险高不能直接等于立即反手。
- 风险高不能直接等于立即开仓。
- 流动性恶化时不建议市价一次性退出。
- 踩踏状态禁止反手、禁止新开仓、禁止机会推送。
- 短线插针不等于趋势反转。
- 风控提示仍需人工复核。

这些规则当前只是 display / read-model 层的安全展示边界，不是完整 Risk Action Guard 生产实现。

---

## 7. 当前完成状态

当前 display adapter chain 已完成：

```text
方案
↓
Java 最小实现
↓
DashboardController 接入
↓
本地 API smoke
↓
单测记录
↓
收口文档
```

当前阶段可视为：

```text
Dashboard detail display adapter chain P0 完成。
```

---

## 8. 仍未完成的真实后端能力

当前仍未完成：

- 真实 PlanBoundary production adapter。
- 真实 BoundaryCandidate / source trace 接入。
- 真实 RuntimeKlineContext / source assembler 接入。
- 真实 Risk Action Guard service 接入。
- 真实 PaperObservation backend / review backend 接入。
- 真实 execution plan readiness 与数值边界生成。
- dashboard 前端对 READY_REVIEW_ONLY / MANUAL_REVIEW_REQUIRED 的更精细展示。

这些必须后续单独规划，不应在当前 display adapter P0 中混入。

---

## 9. 后续建议顺序

建议下一阶段不要直接接交易执行，先进入真实 read-model 后端接入前置方案：

1. PlanBoundary real source trace adapter 方案。
2. ExecutionPlanDisplay 与真实 PlanBoundary 的只读映射增强。
3. RiskActionGuard 真实只读 service 方案。
4. PaperObservation review backend 方案。
5. dashboard 前端展示细化。

继续禁止：

```text
order API / 自动交易 / 自动开仓 / 自动平仓 / 自动反手
```

---

## 10. 本 PR 验收标准

本 PR 验收标准：

- 只新增一个文档：`docs/PHASE_DASHBOARD_DISPLAY_ADAPTER_CHAIN_CLOSURE.md`。
- 无 Java 改动。
- 无 dashboard.html 改动。
- 无 schema 改动。
- 无 mapper / RuleEngine 改动。
- 无交易执行相关改动。
- 文档总结 PR #28 到 PR #53 的 display adapter chain。
- 文档明确当前是 read-model fail-closed 展示层。
- 文档明确后续真实后端接入仍需单独规划。
