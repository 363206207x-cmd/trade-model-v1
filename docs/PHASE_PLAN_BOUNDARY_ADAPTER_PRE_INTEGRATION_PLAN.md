# PHASE: 真实 PlanBoundary Adapter 接入前方案（PR28）

## 1. 阶段目标

本阶段目标是定义真实 PlanBoundary adapter 接入前的方案边界，为后续把后端真实 PlanBoundary 状态逐步接入 `PlanBoundaryDisplayVO` 做准备。

本 PR 只做方案文档，不实现 Java 代码、不改 dashboard、不改 schema、不改 mapper、不改 service、不改 RuleEngine、不接 order API、不自动交易。

核心目标：

- 明确真实 PlanBoundary adapter 的职责边界。
- 明确 adapter 只负责 read model 展示拼装，不负责生成交易指令。
- 明确第一阶段只接状态、缺失原因、阻断原因与安全标记，不接真实 entry / stop / TP 数值。
- 防止 adapter 阶段越界到 RuleEngine、source assembler、ExecutionPlan 生产写入或自动交易。

---

## 2. 当前已完成链路

当前 display status 链路已完成：

- `DashboardDetailResponseVO` 已新增四个 display model。
- `withSafeDefaultDisplays()` / `ensureSafeDefaultDisplays()` 已提供安全默认对象。
- `/api/dashboard/detail` 已返回默认 display 对象。
- `dashboard.html` 已读取 detail display status 字段并保留 fallback。
- 本地单测、页面截图与 API curl 结果已记录。

当前 PlanBoundary 仍处于：

```text
BACKEND_PENDING / 后端未接入
```

因此后续需要 adapter 层，把已有后端状态以只读方式拼装到 `PlanBoundaryDisplayVO`。

---

## 3. Adapter 定位

建议新增的真实 PlanBoundary adapter 定位为：

```text
后端只读展示拼装层 / read model adapter
```

它负责：

- 从已有 BoundaryCandidate / PlanBoundary 相关服务或对象中读取状态。
- 将状态转换成 dashboard 可展示字段。
- 填充 `PlanBoundaryDisplayVO` 的 status / label / reasons / flags。
- 保持 fail-closed 默认值。
- 对缺失字段返回 INCOMPLETE / BACKEND_PENDING。

它不负责：

- 生成 entry / stop / TP。
- 生成新的 BoundaryCandidate。
- 生成新的 ExecutionPlan。
- 调用 RuleEngine。
- 调用 source assembler。
- 写数据库。
- 修改 schema。
- 触发交易动作。
- 调用 order API。

---

## 4. 第一阶段允许接入的字段

第一阶段真实 adapter 只允许填充：

- `planBoundaryStatus`
- `planBoundaryStatusLabel`
- `sourceTraceStatus`
- `backendConnectionStatus`
- `incompleteReasons`
- `blockingReasons`
- `manualReviewRequired`
- `notTradeInstruction`
- `updatedAt`

第一阶段不得填充：

- entry price
- entry zone
- stop price
- take profit price
- RR 数值
- invalidation price
- orderId
- orderRequest
- autoExecute
- autoTrade

---

## 5. 状态映射建议

### 5.1 VALID

只有后端已有明确 VALID 状态且来源可追踪时，才允许映射为：

```text
planBoundaryStatus = VALID
sourceTraceStatus = TRACEABLE 或 PARTIAL
manualReviewRequired = true
notTradeInstruction = true
```

注意：

- 即使 VALID，也仍不是交易指令。
- 第一阶段仍不展示 entry / stop / TP 数值。

### 5.2 WATCH_ONLY

当后端状态表示仅观察时，映射为：

```text
planBoundaryStatus = WATCH_ONLY
planBoundaryStatusLabel = 仅观察
```

展示规则：

- 不包装为机会。
- 不允许 ExecutionPlan 展示为完整可执行计划。

### 5.3 INCOMPLETE

当存在缺失字段、缺失来源、数据不足或 adapter 不能确认时，映射为：

```text
planBoundaryStatus = INCOMPLETE
sourceTraceStatus = MISSING 或 PARTIAL
```

并填充 `incompleteReasons`。

### 5.4 INVALID

当后端明确当前边界失效时，映射为：

```text
planBoundaryStatus = INVALID
```

展示规则：

- 不得包装为反向机会。
- 不得自动生成反手计划。

### 5.5 BACKEND_PENDING

当真实后端来源尚未接入或不可用时，保持：

```text
planBoundaryStatus = BACKEND_PENDING
sourceTraceStatus = BACKEND_PENDING
backendConnectionStatus = BACKEND_PENDING
```

---

## 6. incompleteReasons 建议 code

第一阶段建议使用字符串列表，保持实现简单：

- `BACKEND_FIELD_PENDING`
- `SOURCE_TRACE_PENDING`
- `BOUNDARY_CANDIDATE_MISSING`
- `ENTRY_SOURCE_MISSING`
- `STOP_SOURCE_MISSING`
- `TP_SOURCE_MISSING`
- `DATA_QUALITY_LOW`
- `RUNTIME_KLINE_MISSING`
- `RUNTIME_KLINE_STALE`
- `RISK_ACTION_GUARD_BLOCKED`

后续如需要结构化对象，再单独规划，不在第一阶段引入复杂 DTO。

---

## 7. blockingReasons 建议 code

第一阶段建议使用字符串列表：

- `PLAN_BOUNDARY_BACKEND_PENDING`
- `PLAN_BOUNDARY_NOT_VALID`
- `SOURCE_ASSEMBLER_PENDING`
- `RULE_ENGINE_NOT_CONNECTED`
- `RISK_ACTION_GUARD_LOCKED`
- `MANUAL_REVIEW_REQUIRED`

---

## 8. 与 DashboardController 的关系

当前 `DashboardController.dashboardDetail` 已使用：

```java
DashboardDetailResponseVO.withSafeDefaultDisplays()
```

后续 adapter 接入建议：

1. Controller 仍先创建 safe default displays。
2. 调用一个只读 adapter 尝试填充 `planBoundaryDisplay`。
3. adapter 失败或返回 null 时，不抛异常到页面。
4. 保持默认 BACKEND_PENDING。
5. 不影响 decision / evidence / score / marketEnvironmentMini 原有链路。

示意：

```text
DashboardDetailResponseVO body = DashboardDetailResponseVO.withSafeDefaultDisplays();
...
body.setPlanBoundaryDisplay(planBoundaryDisplayAdapter.build(...));
```

如果 adapter 不可用：

```text
保持 safe default display
```

---

## 9. 建议新增对象边界

后续如进入 Java 实现，建议先定义：

```text
PlanBoundaryDisplayAdapter
```

职责：

- 输入：symbol / analysisId / decision / 当前默认 display。
- 输出：`DashboardDetailResponseVO.PlanBoundaryDisplayVO`。
- 失败：返回 BACKEND_PENDING 或保留默认 display。

不建议第一阶段新增：

- 新表。
- 新 mapper。
- 新写入逻辑。
- 新 RuleEngine wiring。
- 新 ExecutionPlan 生成逻辑。

---

## 10. 与 ExecutionPlanDisplayVO 的关系

PlanBoundary adapter 接入后，ExecutionPlan display 仍不能自动变为可执行。

规则：

- `planBoundaryStatus != VALID`：ExecutionPlan 继续 BOUNDARY_PENDING / INCOMPLETE / WATCH_ONLY。
- `planBoundaryStatus = VALID`：也只允许展示为 READY_REVIEW_ONLY，而不是自动执行。
- ExecutionPlanDisplayVO 的状态映射应单独 PR 处理。

---

## 11. 与 Risk Action Guard 的关系

PlanBoundary adapter 不得绕过 Risk Action Guard。

规则：

- 踩踏状态：PlanBoundary 不得包装为机会。
- 流动性恶化：不得鼓励市价一次性砍仓。
- 短线插针：不得直接判定趋势反转。
- 风险动作提示仍需人工复核。

第一阶段可以只通过 `blockingReasons` 暂存风险阻断原因，不直接接真实 Risk Action Guard service。

---

## 12. 测试建议

后续 Java adapter PR 应至少测试：

- adapter 输入 null 时返回 BACKEND_PENDING。
- adapter 无 analysisId 时返回 BACKEND_PENDING 或 INCOMPLETE。
- adapter 无 BoundaryCandidate 时返回 INCOMPLETE。
- adapter 不生成 entry / stop / TP 数值。
- adapter 始终保留 `manualReviewRequired = true`。
- adapter 始终保留 `notTradeInstruction = true`。
- adapter 不包含 order / autoTrade / autoExecute 字段。

---

## 13. 明确不做什么

本阶段及下一阶段前置实现仍不做：

- 不改 schema。
- 不新增数据库表。
- 不新增 mapper 写入。
- 不改 RuleEngine。
- 不改 source assembler。
- 不生成真实 entry / stop / TP。
- 不生成真实 ExecutionPlan。
- 不接 order API。
- 不自动交易。
- 不自动开仓 / 平仓 / 反手。
- 不把 VALID 状态当作交易指令。

---

## 14. 建议后续 PR 顺序

建议后续按以下顺序：

1. PR #29：PlanBoundary adapter Java 实现前只读范围确认。
2. PR #30：PlanBoundaryDisplayAdapter 接口与默认实现方案。
3. PR #31：PlanBoundaryDisplayAdapter Java 最小实现。
4. PR #32：DashboardController 接入 adapter 但保持 fail-closed。
5. PR #33：本地 API smoke 验证。

---

## 15. 本 PR 验收标准

本 PR 验收标准：

- 只新增一个文档：`docs/PHASE_PLAN_BOUNDARY_ADAPTER_PRE_INTEGRATION_PLAN.md`。
- 无 Java 改动。
- 无 dashboard.html 改动。
- 无 schema 改动。
- 无 mapper / service / RuleEngine 改动。
- 无 order API / 自动交易相关改动。
- 文档明确 adapter 只做只读展示拼装。
- 文档明确第一阶段不接真实 entry / stop / TP 数值。
