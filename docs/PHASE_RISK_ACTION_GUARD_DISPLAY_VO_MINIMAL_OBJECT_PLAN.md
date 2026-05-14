# PHASE: RiskActionGuardDisplayVO 最小对象方案（PR18）

## 1. 阶段目标

本阶段目标是定义 `RiskActionGuardDisplayVO` 的最小对象方案，为后续 dashboard detail read model 的 Java 最小实现做准备。

本 PR 只做方案文档，不实现 Java 代码、不改 dashboard、不改 schema、不新增接口、不接 order API、不自动交易。

核心目标：

- 将 Risk Action Guard 作为 dashboard 的安全展示对象，而不是交易执行对象。
- 承载风险动作分层状态、建议语义、阻断原因与禁止标记。
- 明确踩踏、流动性恶化、短线插针等状态下的 fail-closed 展示规则。
- 防止风险提示被误写成自动减仓、自动止损、自动反手或自动开仓。

---

## 2. 对象定位

`RiskActionGuardDisplayVO` 是 dashboard read model 的展示层对象，用于说明当前风险动作保护器状态。

它的定位是：

- 展示风险动作分层。
- 展示是否禁止新开仓、反手、机会推送。
- 展示为什么当前只能观察、等待确认或人工复核。
- 为 ExecutionPlan / PlanBoundary 展示提供动作审核约束。

它不是：

- 自动交易模块。
- 自动平仓模块。
- 自动反手机制。
- order API 请求对象。
- 交易所执行指令。

---

## 3. 最小字段建议

建议 `RiskActionGuardDisplayVO` 第一阶段包含以下字段：

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

说明：

- 不包含 orderId / orderRequest / autoExecute / autoTrade。
- 不包含真实交易执行状态。
- 不触发自动交易动作。
- 只作为 dashboard 展示与人工审核依据。

---

## 4. 字段语义

### 4.1 `riskActionGuardStatus`

建议枚举：

- `NORMAL`
- `HIGH_RISK_LIQUIDITY_NORMAL`
- `HIGH_RISK_LIQUIDITY_DEGRADED`
- `STAMPEDE_LOCK`
- `WICK_ONLY_CONFIRMATION_REQUIRED`
- `BACKEND_PENDING`

语义：

- `NORMAL`：未触发特殊风险动作保护。
- `HIGH_RISK_LIQUIDITY_NORMAL`：风险高但流动性正常，可展示降风险建议语义。
- `HIGH_RISK_LIQUIDITY_DEGRADED`：风险高且流动性恶化，不建议市价一次性砍仓。
- `STAMPEDE_LOCK`：踩踏锁定，禁止反手、新开仓、机会推送。
- `WICK_ONLY_CONFIRMATION_REQUIRED`：短线插针，仅提醒，等待确认。
- `BACKEND_PENDING`：后端真实字段尚未接入。

### 4.2 `riskActionGuardStatusLabel`

建议中文文案：

- `正常观察`
- `高风险：流动性正常`
- `高风险：流动性恶化`
- `踩踏锁定`
- `短线插针，等待确认`
- `后端未接入`

### 4.3 `riskActionAdvice`

只承载建议语义，不代表执行。

示例：

- `可考虑减仓 / 移动止损 / 降低杠杆`
- `优先分批降风险 / 等待流动性恢复 / 只降杠杆`
- `禁止反手、禁止新开仓、禁止机会推送`
- `只做短线风险提醒，等待多周期确认`

### 4.4 `riskActionBlockingReason`

用于说明阻断原因。

建议 code：

- `LIQUIDITY_DEGRADED`
- `STAMPEDE_DETECTED`
- `WICK_ONLY_RISK`
- `DATA_QUALITY_LOW`
- `BACKEND_PENDING`
- `PLAN_BOUNDARY_INCOMPLETE`

### 4.5 `liquidityState`

建议枚举：

- `NORMAL`
- `DEGRADED`
- `SEVERELY_DEGRADED`
- `UNKNOWN`
- `BACKEND_PENDING`

### 4.6 `stampedeDetected`

布尔值。

- true：检测到踩踏 / 极端压力锁定语义。
- false：未检测到踩踏。

未接入后端时默认 false，但 `riskActionGuardStatus` 应为 `BACKEND_PENDING`。

### 4.7 `wickOnlyRisk`

布尔值。

- true：当前仅为短线插针风险，不能直接判定趋势反转。
- false：未标记为仅插针。

### 4.8 行为允许标记

建议字段：

- `opportunityPushAllowed`
- `reverseTradeAllowed`
- `newPositionAllowed`
- `marketOrderExitAllowed`

这些字段不是执行动作，只是展示当前是否允许出现相关语义。

安全默认：

- 未接入后端时全部 false。
- 踩踏锁定时全部 false。
- 流动性恶化时 `marketOrderExitAllowed = false`，避免鼓励市价一次性砍仓。

### 4.9 `manualRiskReviewRequired`

必须默认 true。

### 4.10 `notTradeInstruction`

必须默认 true。

### 4.11 `updatedAt`

展示状态更新时间。

第一阶段可为 null，前端需安全兜底。

---

## 5. 安全默认值

后续 Java 最小实现建议默认值：

- `riskActionGuardStatus = BACKEND_PENDING`
- `riskActionGuardStatusLabel = 后端未接入`
- `liquidityState = BACKEND_PENDING`
- `stampedeDetected = false`
- `wickOnlyRisk = false`
- `opportunityPushAllowed = false`
- `reverseTradeAllowed = false`
- `newPositionAllowed = false`
- `marketOrderExitAllowed = false`
- `manualRiskReviewRequired = true`
- `notTradeInstruction = true`

推荐第一阶段采用 fail-closed：

- 未接入真实字段时，不允许机会推送语义。
- 未接入真实字段时，不允许反手语义。
- 未接入真实字段时，不允许新开仓语义。
- 未接入真实字段时，不允许市价退出建议语义。

---

## 6. 四类核心规则展示

### 6.1 风险高但流动性正常

展示语义：

- 可考虑减仓。
- 可考虑移动止损。
- 可考虑降低杠杆。

限制：

- 仅为风险提示。
- 不自动执行。
- 需要人工复核。

### 6.2 风险高且流动性恶化

展示语义：

- 不建议市价一次性砍仓。
- 优先分批降风险。
- 等待流动性恢复。
- 只降杠杆。

限制：

- 不得鼓励踩踏式错误执行。
- 不得自动平仓。

### 6.3 风险高且存在踩踏

硬约束：

- 禁止反手。
- 禁止新开仓。
- 禁止机会推送。
- 第一优先级保护本金。

建议字段默认：

- `opportunityPushAllowed = false`
- `reverseTradeAllowed = false`
- `newPositionAllowed = false`
- `marketOrderExitAllowed = false`

### 6.4 风险高但仅短线插针

展示语义：

- 不直接判定趋势反转。
- 不生成反向开仓计划。
- 只做短线风险提醒。
- 等待多周期确认。

限制：

- 不允许反手语义。
- 不允许强反转自动判定。

---

## 7. 与 PlanBoundaryDisplayVO 的关系

`RiskActionGuardDisplayVO` 可以通过阻断语义影响 PlanBoundary 展示：

- 踩踏锁定时，PlanBoundary 不得包装为机会。
- 短线插针时，PlanBoundary 不得直接输出趋势反转结论。
- 流动性恶化时，PlanBoundary 不得鼓励市价一次性砍仓。

但 Risk Action Guard 不直接修改 BoundaryCandidate 原始对象。

---

## 8. 与 ExecutionPlanDisplayVO 的关系

ExecutionPlan display 必须尊重 Risk Action Guard：

- `STAMPEDE_LOCK`：ExecutionPlan 不得展示为新开仓 / 反手计划。
- `HIGH_RISK_LIQUIDITY_DEGRADED`：ExecutionPlan 不得展示市价一次性退出建议。
- `WICK_ONLY_CONFIRMATION_REQUIRED`：ExecutionPlan 不得展示趋势反转或反向开仓计划。

建议通过 `notExecutableReason` 或 `incompleteReasons` 记录：

- `RISK_ACTION_GUARD_BLOCKED`
- `STAMPEDE_LOCK`
- `LIQUIDITY_DEGRADED`
- `WICK_CONFIRMATION_REQUIRED`

---

## 9. 与 Push 的关系

Risk Action Guard 可影响机会推送展示语义：

- 踩踏状态：禁止机会推送。
- 非 Watchlist Pool 资产：禁止机会推送。
- 后端未接入：默认不允许机会推送。

但本对象不实现 Push 逻辑，只承载展示字段。

---

## 10. 后续 Java 实现建议

建议后续 Java PR 拆成：

1. 新增 `RiskActionGuardDisplayVO` 类或 `DashboardDetailResponseVO.RiskActionGuardDisplayVO` 内部静态类。
2. 新增 getter / setter。
3. 默认构造或 factory 输出 BACKEND_PENDING + fail-closed flags。
4. 单测验证安全默认值。
5. `DashboardDetailResponseVO` 新增字段。
6. `DashboardController` detail 组装默认 display。
7. 后续再接真实 Risk Action Guard service / adapter。

---

## 11. 测试建议

后续 Java 最小实现应测试：

- 默认 `riskActionGuardStatus = BACKEND_PENDING`。
- 默认 `opportunityPushAllowed = false`。
- 默认 `reverseTradeAllowed = false`。
- 默认 `newPositionAllowed = false`。
- 默认 `marketOrderExitAllowed = false`。
- 默认 `manualRiskReviewRequired = true`。
- 默认 `notTradeInstruction = true`。
- 不包含 order / autoTrade / autoExecute 字段。

---

## 12. 明确不做什么

本阶段明确不做：

- 不改 `src/`。
- 不改 `dashboard.html`。
- 不改 schema。
- 不改 `pom.xml`。
- 不新增接口。
- 不新增数据库表。
- 不接 order API。
- 不自动交易。
- 不自动开仓 / 平仓 / 反手。
- 不实现 Risk Action Guard 后端业务逻辑。
- 不实现 Push 逻辑。

---

## 13. 验收标准

本 PR 验收标准：

- 只新增一个方案文档：`docs/PHASE_RISK_ACTION_GUARD_DISPLAY_VO_MINIMAL_OBJECT_PLAN.md`。
- 无 Java 代码改动。
- 无 dashboard 改动。
- 无 schema 改动。
- 无 order API / 自动交易相关改动。
- 文档明确 `RiskActionGuardDisplayVO` 最小字段。
- 文档明确 fail-closed 安全默认值。
- 文档明确踩踏状态禁止反手、新开仓、机会推送。
