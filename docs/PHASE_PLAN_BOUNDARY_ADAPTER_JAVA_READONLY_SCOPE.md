# PHASE: PlanBoundary Adapter Java 实现前只读范围确认（PR29）

## 1. 阶段目标

本阶段目标是在进入 PlanBoundary adapter Java 实现前，对当前远端 `main` 分支可见代码进行只读范围确认。

本 PR 只新增审计文档，不修改 Java、不修改 dashboard、不修改 schema、不修改 mapper、不修改 service、不修改 RuleEngine、不接 order API、不自动交易。

核心目标：

- 确认当前 `main` 上是否已有可复用的 PlanBoundary / BoundaryCandidate / RuntimeKlineContext 相关代码。
- 明确后续 Java 最小实现不能假定不存在于远端 main 的对象已经可用。
- 明确 adapter 第一阶段应保持 display/read-model fail-closed，不生成真实 entry / stop / TP。
- 给后续 PR #30 / PR #31 的最小实现范围提供依据。

---

## 2. 只读检查范围

本次只读检查围绕以下关键词与文件路径进行：

- `BoundaryCandidateDTO`
- `BoundaryCandidateService`
- `RuntimeKlineContextDTO`
- `PlanBoundaryDisplayVO`
- `planboundary`
- `src/main/java/org/example/trademodel/dto/planboundary/BoundaryCandidateDTO.java`
- `src/main/java/org/example/trademodel/service/planboundary/BoundaryCandidateService.java`

---

## 3. 当前远端 main 事实

### 3.1 PlanBoundary / BoundaryCandidate 相关包

当前 GitHub `main` 上搜索 `planboundary` 未发现结果。

尝试读取以下文件未找到：

```text
src/main/java/org/example/trademodel/dto/planboundary/BoundaryCandidateDTO.java
src/main/java/org/example/trademodel/service/planboundary/BoundaryCandidateService.java
```

结论：

```text
远端 main 当前未发现可直接复用的 planboundary DTO / Service 包。
```

### 3.2 当前已存在的 display/read-model 外壳

当前远端 main 已经完成：

- `DashboardDetailResponseVO.PlanBoundaryDisplayVO`
- `DashboardDetailResponseVO.ExecutionPlanDisplayVO`
- `DashboardDetailResponseVO.RiskActionGuardDisplayVO`
- `DashboardDetailResponseVO.PaperObservationDisplayVO`
- `DashboardDetailResponseVO.withSafeDefaultDisplays()`
- `DashboardDetailResponseVO.ensureSafeDefaultDisplays()`
- `DashboardController.dashboardDetail()` 返回 safe default display 对象。
- `dashboard.html` 已读取 detail display status 字段。

结论：

```text
当前可依赖的是 dashboard display/read-model 外壳，不是完整 PlanBoundary 生产链路。
```

---

## 4. 实现前风险判断

### 4.1 不能假设 BoundaryCandidate 已在远端 main 可用

由于远端 main 未发现 `BoundaryCandidateDTO` 与 `BoundaryCandidateService`，后续 adapter Java 实现不能直接引用这些类。

否则会导致：

- 编译失败。
- adapter 越界依赖不存在对象。
- 误以为真实 PlanBoundary 生产链路已完成。

### 4.2 不能从文本字段伪造 PlanBoundary

当前已有 `DecisionResultVO.entryZone` / `stopLoss` / `takeProfitRules` 等文本字段。

这些字段仍不能作为真实 PlanBoundary source trace。

后续 adapter 不得：

- 把 `entryZone` 映射为真实 entry。
- 把 `stopLoss` 映射为真实 stop。
- 把 `takeProfitRules` 映射为真实 TP。
- 把文本摘要包装为 VALID PlanBoundary。

### 4.3 第一阶段只能做 fail-closed adapter

由于真实 PlanBoundary 生产链路尚未在远端 main 可见，第一阶段 adapter 最合理边界是：

- 根据已有字段判断是否仍为 BACKEND_PENDING / INCOMPLETE。
- 保持 manualReviewRequired = true。
- 保持 notTradeInstruction = true。
- 不生成数值。
- 不接 RuleEngine。

---

## 5. 建议 PR #30 范围

建议 PR #30 先创建方案文档或最小接口方案：

```text
PlanBoundaryDisplayAdapter 接口与默认实现方案
```

推荐边界：

- 只定义 adapter 的输入输出。
- 不直接实现真实业务逻辑。
- 输入可为：symbol、DecisionResultVO、DashboardDetailResponseVO.PlanBoundaryDisplayVO 默认对象。
- 输出为：DashboardDetailResponseVO.PlanBoundaryDisplayVO。
- 默认输出 BACKEND_PENDING 或 INCOMPLETE。

---

## 6. 建议 PR #31 Java 最小实现范围

如果进入 Java 最小实现，建议只新增：

```text
src/main/java/org/example/trademodel/service/dashboard/PlanBoundaryDisplayAdapter.java
src/main/java/org/example/trademodel/service/dashboard/DefaultPlanBoundaryDisplayAdapter.java
src/test/java/org/example/trademodel/service/dashboard/DefaultPlanBoundaryDisplayAdapterTest.java
```

或者如果项目已有 dashboard read-model service 包，应复用现有包结构。

第一阶段实现规则：

- null 输入 → BACKEND_PENDING。
- 无 decision → BACKEND_PENDING。
- 有 decision 但只有文本执行计划字段 → INCOMPLETE 或 BACKEND_PENDING。
- readModelTruthStatus = PARTIAL → INCOMPLETE。
- 始终 notTradeInstruction = true。
- 始终 manualReviewRequired = true。
- 不生成 entry / stop / TP。

---

## 7. 不建议立即做的事

当前不建议：

- 恢复或假设未在远端 main 中存在的大轨道 `planboundary` 包。
- 直接引用 `BoundaryCandidateDTO`。
- 直接引用 `BoundaryCandidateService`。
- 新增 schema。
- 新增 mapper。
- 接 RuleEngine。
- 接 source assembler。
- 接真实 entry / stop / TP。
- 接 order API。
- 自动交易。

---

## 8. 与 Risk Action Guard 的提醒

后续 adapter 仍必须遵守 Risk Action Guard：

- 风险高不能直接等于立即止损、立即反手或立即开仓。
- 流动性恶化时不建议市价一次性砍仓。
- 踩踏状态禁止反手、禁止新开仓、禁止机会推送。
- 短线插针不等于趋势反转。

本阶段仅记录约束，不实现 Risk Action Guard 后端逻辑。

---

## 9. 当前结论

```text
PlanBoundary adapter Java 实现前只读范围确认：完成。
```

当前远端 main 可依赖：

- Dashboard detail display/read-model 外壳。
- Safe default display helper。
- DashboardController detail 默认 display 返回。
- dashboard.html 对 display status 的读取。

当前远端 main 不应假设可依赖：

- BoundaryCandidateDTO。
- BoundaryCandidateService。
- RuntimeKlineContextDTO。
- 完整 PlanBoundary 生产链路。
- 真实 entry / stop / TP source trace。

---

## 10. 本 PR 验收标准

本 PR 验收标准：

- 只新增一个文档：`docs/PHASE_PLAN_BOUNDARY_ADAPTER_JAVA_READONLY_SCOPE.md`。
- 无 Java 改动。
- 无 dashboard.html 改动。
- 无 schema 改动。
- 无 mapper / service / RuleEngine 改动。
- 无 order API / 自动交易相关改动。
- 文档如实记录当前远端 main 未发现 planboundary DTO / Service。
- 文档明确后续 adapter 只能先做 fail-closed read-model 拼装。
