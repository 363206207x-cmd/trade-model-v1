# PHASE: PlanBoundaryDisplayVO 最小对象方案（PR16）

## 1. 阶段目标

本阶段目标是定义 `PlanBoundaryDisplayVO` 的最小对象方案，为后续 Java 最小实现做准备。

本 PR 只做方案文档，不实现 Java 代码、不改 dashboard、不改 schema、不新增接口、不接 order API、不自动交易。

核心目标：

- 先定义 PlanBoundary 在 dashboard detail / summary 中的安全展示对象。
- 只承载状态、缺失原因、阻断原因、人工复核与非交易指令标记。
- 第一阶段不承载 entry / stop / TP 数值。
- 防止 dashboard 在后端真实边界未接入前伪造可执行交易计划。

---

## 2. 对象定位

`PlanBoundaryDisplayVO` 是 dashboard read model 的展示层对象，不是 RuleEngine 输出对象，也不是交易执行对象。

它的定位是：

- 给 dashboard 展示 PlanBoundary 当前接入状态。
- 告诉用户当前边界是否可展示、是否缺失、是否需要等待后端接入。
- 承载 INCOMPLETE / BACKEND_PENDING / WATCH_ONLY / INVALID 等状态原因。
- 明确所有内容均为非交易指令，需要人工复核。

它不是：

- 自动交易指令。
- 自动下单请求。
- 完整 ExecutionPlan。
- BoundaryCandidate 原始生产对象。
- RuleEngine 生产逻辑。

---

## 3. 最小字段建议

建议 `PlanBoundaryDisplayVO` 第一阶段只包含以下字段：

- `planBoundaryStatus`
- `planBoundaryStatusLabel`
- `sourceTraceStatus`
- `backendConnectionStatus`
- `incompleteReasons`
- `blockingReasons`
- `manualReviewRequired`
- `notTradeInstruction`
- `updatedAt`

说明：

- 不包含 entry / stop / TP 价格。
- 不包含 RR 数值。
- 不包含自动执行字段。
- 不包含 orderId / orderRequest / autoExecute / autoTrade。

---

## 4. 字段语义

### 4.1 `planBoundaryStatus`

建议枚举：

- `VALID`
- `WATCH_ONLY`
- `INCOMPLETE`
- `INVALID`
- `BACKEND_PENDING`

语义：

- `VALID`：后端已确认边界完整，但仍不是交易指令。
- `WATCH_ONLY`：只允许观察，不输出完整执行计划。
- `INCOMPLETE`：边界不完整，必须展示缺失原因。
- `INVALID`：当前边界失效，不得包装为机会。
- `BACKEND_PENDING`：后端真实字段尚未接入。

### 4.2 `planBoundaryStatusLabel`

用于展示中文可读文案，例如：

- `完整但需人工复核`
- `仅观察`
- `信息不完整`
- `已失效`
- `后端未接入`

### 4.3 `sourceTraceStatus`

建议枚举：

- `TRACEABLE`
- `PARTIAL`
- `MISSING`
- `BACKEND_PENDING`

语义：

- `TRACEABLE`：关键来源可追踪。
- `PARTIAL`：部分来源可追踪。
- `MISSING`：关键来源缺失。
- `BACKEND_PENDING`：source trace 尚未接入。

### 4.4 `backendConnectionStatus`

建议枚举：

- `CONNECTED`
- `PARTIAL`
- `BACKEND_PENDING`
- `PLACEHOLDER_ONLY`
- `MANUAL_REVIEW_REQUIRED`

用于提示前端模块接入成熟度。

### 4.5 `incompleteReasons`

结构化缺失原因列表。

建议后续每项包含：

- `code`
- `message`
- `field`
- `severity`
- `sourceRequired`

本对象方案中只定义字段，暂不实现类。

### 4.6 `blockingReasons`

用于承载阻断原因，例如：

- 数据质量不足。
- source assembler 未接入。
- Risk Action Guard 阻断。
- 后端字段未接入。
- PlanBoundary 已失效。

### 4.7 `manualReviewRequired`

必须默认 true。

含义：

- 当前展示仅供人工复核。
- 不允许自动执行。

### 4.8 `notTradeInstruction`

必须默认 true。

含义：

- 明确不是交易指令。
- 不可作为自动下单依据。

### 4.9 `updatedAt`

用于展示 PlanBoundary display 状态更新时间。

第一阶段可为 null，但前端必须安全兜底。

---

## 5. 安全默认值

后续 Java 最小实现建议默认值：

- `planBoundaryStatus = BACKEND_PENDING`
- `planBoundaryStatusLabel = 后端未接入`
- `sourceTraceStatus = BACKEND_PENDING`
- `backendConnectionStatus = BACKEND_PENDING`
- `manualReviewRequired = true`
- `notTradeInstruction = true`
- `incompleteReasons` 不为 null，默认空列表或包含 BACKEND_FIELD_PENDING。
- `blockingReasons` 不为 null，默认空列表或包含 BACKEND_FIELD_PENDING。

推荐第一阶段采用 fail-closed：

- 未接入真实字段时，不显示任何价格。
- 未接入真实字段时，不允许机会推送语义。
- 未接入真实字段时，不允许可执行计划语义。

---

## 6. 状态展示规则

### 6.1 VALID

当状态为 `VALID`：

- 可以显示“边界完整”。
- 仍必须显示“非交易指令 / 需要人工复核”。
- 后续如展示价格，必须另接 source trace 字段。

### 6.2 WATCH_ONLY

当状态为 `WATCH_ONLY`：

- 只允许观察。
- 不得展示完整执行计划。
- 不得包装成可执行机会。

### 6.3 INCOMPLETE

当状态为 `INCOMPLETE`：

- 必须展示缺失原因。
- 不得展示伪造 entry / stop / TP。
- 不得用默认价格或空对象假装完整。

### 6.4 INVALID

当状态为 `INVALID`：

- 必须显示失效。
- 不得自动生成反向机会。
- 不得触发反手语义。

### 6.5 BACKEND_PENDING

当状态为 `BACKEND_PENDING`：

- 必须显示后端未接入。
- 不得假装已完成。
- dashboard 可展示占位，但不得展示数值。

---

## 7. 与 DashboardDetailResponseVO 的关系

后续建议在 `DashboardDetailResponseVO` 中新增：

- `private PlanBoundaryDisplayVO planBoundaryDisplay;`

但本 PR 不改 Java。

后续 Java 实现应保持：

- 现有 `decision` 字段不变。
- `planBoundaryDisplay` 作为新增展示对象，向后兼容。
- 当前 `DecisionResultVO.entryZone / stopLoss / takeProfitRules` 不得直接映射为 `VALID` PlanBoundary。

---

## 8. 与 ExecutionPlanDisplayVO 的关系

`PlanBoundaryDisplayVO` 应作为 `ExecutionPlanDisplayVO` 的前置状态来源。

规则：

- `planBoundaryStatus != VALID` 时，ExecutionPlan 不得显示为完整可执行计划。
- `planBoundaryStatus = INCOMPLETE` 时，ExecutionPlan 必须显示缺失原因。
- `planBoundaryStatus = WATCH_ONLY` 时，ExecutionPlan 只能展示观察摘要。
- `planBoundaryStatus = INVALID` 时，ExecutionPlan 必须显示失效。

---

## 9. 与 Risk Action Guard 的关系

后续 `PlanBoundaryDisplayVO` 可以通过 `blockingReasons` 承载 Risk Action Guard 影响，但不直接执行动作。

必须遵守：

- 踩踏状态不得包装为机会。
- 短线插针不得直接判定趋势反转。
- 流动性恶化不得鼓励市价一次性砍仓。
- 风险动作提示不等于自动交易。

---

## 10. 后续 Java 实现建议

建议后续 Java PR 拆成：

1. 新增 `PlanBoundaryDisplayVO` 类或 `DashboardDetailResponseVO.PlanBoundaryDisplayVO` 内部静态类。
2. 新增最小 getter / setter。
3. 默认构造或 factory 输出 BACKEND_PENDING。
4. 单测验证安全默认值。
5. `DashboardDetailResponseVO` 新增字段。
6. `DashboardController` detail 组装默认 display。
7. 后续再接真实 PlanBoundary service / adapter。

---

## 11. 测试建议

后续 Java 最小实现应测试：

- 默认 `planBoundaryStatus = BACKEND_PENDING`。
- 默认 `manualReviewRequired = true`。
- 默认 `notTradeInstruction = true`。
- 默认不包含 entry / stop / TP 数值字段。
- 不存在 order / autoTrade / autoExecute 字段。
- incompleteReasons / blockingReasons 不为 null。

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
- 不生成真实 entry / stop / TP。
- 不实现 RuleEngine / source assembler。

---

## 13. 验收标准

本 PR 验收标准：

- 只新增一个方案文档：`docs/PHASE_PLAN_BOUNDARY_DISPLAY_VO_MINIMAL_OBJECT_PLAN.md`。
- 无 Java 代码改动。
- 无 dashboard 改动。
- 无 schema 改动。
- 无 order API / 自动交易相关改动。
- 文档明确 `PlanBoundaryDisplayVO` 最小字段。
- 文档明确安全默认值。
- 文档明确第一阶段不承载 entry / stop / TP 数值。
