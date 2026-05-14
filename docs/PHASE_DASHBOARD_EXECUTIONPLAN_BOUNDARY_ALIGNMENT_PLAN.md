# PHASE: Dashboard ExecutionPlan 与 Boundary 状态展示对齐方案（PR9）

## 1. 阶段目标

本阶段目标是定义首页中 **ExecutionPlan 摘要** 与 **PlanBoundary 状态** 的展示对齐规则，为后续前端展示与后端字段接入提供边界依据。

本 PR 只做方案文档，不实现代码、不改 dashboard、不改后端、不改 schema。

核心目标：

- 防止 ExecutionPlan 在 PlanBoundary 未完整时被误读为可执行交易计划。
- 防止 WATCH_ONLY / INCOMPLETE 被包装成“可执行机会”。
- 让首页能够清楚显示：当前执行建议是否具备真实边界来源。
- 继续保持 V1 非自动交易、非 order API、非无人值守执行的边界。

---

## 2. 基本展示原则

首页展示 ExecutionPlan 与 Boundary 时必须遵循：

1. **PlanBoundary 优先于 ExecutionPlan 可执行性判断**。
   - 没有 VALID Boundary，ExecutionPlan 不得被展示为可直接执行计划。

2. **ExecutionPlan 可以展示摘要，但必须标记来源状态**。
   - 摘要可以来自已有后端文本字段。
   - 但如果没有结构化 entry / stop / TP 来源，必须显示“边界未完整”。

3. **所有涉及交易动作的展示必须标注：非交易指令 / 需要人工复核**。

4. **前端不得伪造 entry / stop / TP / RR / invalidation condition**。

5. **后端未接入时必须显示 BACKEND_PENDING / 后端未接入**。

---

## 3. 状态对齐矩阵

| PlanBoundary 状态 | ExecutionPlan 首页展示 | 可展示内容 | 必须禁止 |
|---|---|---|---|
| VALID | 可展示结构化执行计划摘要 | entry / stop / TP / RR / invalidation 摘要，但仍需人工复核 | 自动下单、自动开仓、自动止损、自动止盈 |
| WATCH_ONLY | 只展示观察摘要 | 方向、风险、等待条件、观察理由 | 完整交易计划、可执行机会、开仓按钮 |
| INCOMPLETE | 展示不完整原因 | 缺失字段、缺失来源、数据质量问题、后端未接入 | 伪造价格、包装为机会 |
| INVALID | 展示失效状态 | 失效原因、放弃条件、等待重评估 | 开仓建议、反手建议、执行计划 |
| BACKEND_PENDING | 展示后端未接入 | 占位说明、待接入模块 | 默认值假装完成 |

---

## 4. 首页字段草案（仅方案）

后续 dashboard 展示可参考以下字段，但本 PR 不实现接口：

- `symbol`
- `analysisId`
- `planBoundaryStatus`
- `executionPlanStatus`
- `executionPlanSummary`
- `entryStatus`
- `stopStatus`
- `takeProfitStatus`
- `riskRewardStatus`
- `invalidationStatus`
- `incompleteReasons`
- `sourceTraceStatus`
- `manualReviewRequired`
- `notTradeInstruction`
- `moduleConnectionStatus`

---

## 5. 展示规则

### 5.1 VALID

当 `planBoundaryStatus = VALID` 时：

- 首页可以展示结构化执行计划摘要。
- 允许展示：entry / stop / TP / RR / invalidation condition 的摘要。
- 必须显示：
  - 需要人工复核
  - 非交易指令
  - 不自动执行

### 5.2 WATCH_ONLY

当 `planBoundaryStatus = WATCH_ONLY` 时：

- 首页只展示观察摘要。
- 可以展示：等待确认、观察理由、风险提示。
- 不得展示完整开仓计划。
- 不得出现“可执行机会”语义。

### 5.3 INCOMPLETE

当 `planBoundaryStatus = INCOMPLETE` 时：

- 首页必须展示 `incompleteReasons`。
- 必须明确 entry / stop / TP 哪些缺失。
- 不允许以静态默认价、前端推导价、AI 文案价替代真实来源。

### 5.4 INVALID

当 `planBoundaryStatus = INVALID` 时：

- 首页必须显示失效。
- 不允许生成开仓建议。
- 不允许包装为反向机会或强反手计划。

### 5.5 BACKEND_PENDING

当后端尚未接入真实字段时：

- 首页必须显示“后端未接入 / 等待接入”。
- 不允许用默认值、空对象、假状态伪装为 VALID。

---

## 6. 与 Risk Action Guard 的关系

ExecutionPlan 与 Boundary 对齐时必须尊重 Risk Action Guard：

- 风险高但流动性正常：可以展示减仓 / 移动止损 / 降低杠杆建议语义，但仍需人工复核。
- 风险高且流动性恶化：不建议市价一次性砍仓，优先分批降风险 / 等待流动性恢复 / 只降杠杆。
- 风险高且存在踩踏：禁止反手、禁止新开仓、禁止机会推送。
- 风险高但仅短线插针：不直接判定趋势反转，不生成反向开仓计划。

Risk Action Guard 不触发自动交易；仅作为动作审核层与展示约束。

---

## 7. 必须禁止的语义

后续任何 dashboard / backend 接入都必须禁止：

- 自动下单
- 自动开仓
- 自动平仓
- 自动反手
- 自动止损
- 自动止盈
- 一键执行
- 将 WATCH_ONLY 显示为可执行机会
- 将 INCOMPLETE 包装为完整计划
- 将 INVALID 包装为反向开仓机会
- 前端伪造 entry / stop / TP

---

## 8. 后续 PR 拆分建议

建议后续按以下顺序推进：

1. dashboard ExecutionPlan / Boundary 对齐只读占位。
2. dashboard 显示 executionPlanStatus 与 planBoundaryStatus 的组合文案。
3. dashboard 显示 INCOMPLETE 原因与缺失字段。
4. 后端 ExecutionPlan read model 字段方案。
5. PlanBoundary / ExecutionPlan 真实字段接入方案。
6. dashboard smoke 验证与截图验收。

---

## 9. 验收标准

本 PR 验收标准：

- 只新增一个方案文档：`docs/PHASE_DASHBOARD_EXECUTIONPLAN_BOUNDARY_ALIGNMENT_PLAN.md`。
- 无代码改动。
- 无 dashboard 改动。
- 无 schema 改动。
- 无 order API / 自动交易相关改动。
- 文档明确 ExecutionPlan 不得绕过 PlanBoundary。
- 文档明确 INCOMPLETE / WATCH_ONLY / INVALID 不得包装为可执行机会。
