# PHASE: DefaultPlanBoundaryDisplayAdapter 接入 SourceTraceAdapter 本地单测结果记录（PR64）

## 1. 阶段目标

本阶段记录 `DefaultPlanBoundaryDisplayAdapterTest` 在 PR #63 接入 `PlanBoundarySourceTraceAdapter` 后的本地单测结果。

本 PR 只新增验证记录文档，不修改 Java、不修改 dashboard、不修改 schema、不接交易执行链路。

---

## 2. 本地验证命令

用户在本地执行：

```bash
cd /Users/xuchao/Documents/trade-model-v1

./mvnw -Dtest=DefaultPlanBoundaryDisplayAdapterTest test
```

执行时间：2026-05-14 23:02:54 +08:00。

---

## 3. 用户提供的终端结果

```text
Running org.example.trademodel.service.dashboard.DefaultPlanBoundaryDisplayAdapterTest
Tests run: 8, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

同时本轮测试触发了编译：

```text
Compiling 212 source files with javac [debug parameters release 17] to target/classes
Compiling 36 source files with javac [debug parameters release 17] to target/test-classes
```

结论：

```text
DefaultPlanBoundaryDisplayAdapterTest：PASS
```

---

## 4. 验证覆盖范围

该测试覆盖了 `DefaultPlanBoundaryDisplayAdapter` 接入 `PlanBoundarySourceTraceAdapter` 后的关键行为，包括：

- 输入缺失时仍返回 `BACKEND_PENDING`。
- fallback 对象被保留。
- safety flags 被强制保持：
  - `manualReviewRequired = true`
  - `notTradeInstruction = true`
- analysisId 缺失时仍返回 `INCOMPLETE`。
- readModelTruthStatus = PARTIAL 时仍保留 read model fallback reason。
- 文本 `entryZone / stopLoss / takeProfitRules / executionPlanSummary` 不会触发 VALID。
- decision 存在时会调用 source trace adapter。
- source trace adapter 返回 INCOMPLETE 时，最终 display 保持 INCOMPLETE。
- source trace adapter 在当前阶段即使返回 VALID，也会被降回 INCOMPLETE。

---

## 5. 安全边界复核

本阶段验证未发现以下越界：

- 未改 Controller。
- 未改 dashboard。
- 未改 schema。
- 未新增 mapper。
- 未接 RuleEngine。
- 未接 source assembler。
- 未引用不存在的 BoundaryCandidateDTO / RuntimeKlineContextDTO。
- 未生成真实 entry / stop / take profit。
- 未允许 VALID 穿透。
- 未接 order API。
- 未自动交易。

当前 `DefaultPlanBoundaryDisplayAdapter -> PlanBoundarySourceTraceAdapter` 仍是 fail-closed source trace readiness 链路。

---

## 6. 当前阶段结论

截至本 PR：

```text
DefaultPlanBoundaryDisplayAdapter 接入 SourceTraceAdapter + 本地单测验证 已完成闭环。
```

当前链路状态：

```text
DefaultPlanBoundaryDisplayAdapter
↓
PlanBoundarySourceTraceAdapter
↓
fail-closed source trace readiness
↓
DefaultPlanBoundaryDisplayAdapterTest PASS
```

尚未单独完成：

```text
DashboardControllerTest after PR #63
/api/dashboard/detail API smoke after PR #63
```

---

## 7. 后续建议

下一步建议进入：

```text
PR #65：DashboardControllerTest after PlanBoundary source trace wiring
```

用户本地运行：

```bash
./mvnw -Dtest=DashboardControllerTest test
```

再记录测试结果。

随后建议：

```text
PR #66：/api/dashboard/detail smoke after PlanBoundary source trace wiring
```

---

## 8. 本 PR 验收标准

本 PR 验收标准：

- 只新增一个文档：`docs/PHASE_PLAN_BOUNDARY_DISPLAY_ADAPTER_SOURCE_TRACE_TEST_RESULT.md`。
- 无 Java 改动。
- 无 dashboard.html 改动。
- 无 schema 改动。
- 无交易执行相关改动。
- 如实记录 `DefaultPlanBoundaryDisplayAdapterTest` 本地 PASS。
