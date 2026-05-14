# PHASE: Dashboard Display Status Local Smoke Result（PR26）

## 1. 阶段目标

本阶段记录 dashboard display status 链路在本地环境中的 smoke 验证结果。

本 PR 只记录本地 smoke 结果，不修改 Java、不修改 dashboard、不修改 schema、不接 order API、不自动交易。

验证目标来自 PR #25：

- `/api/dashboard/detail` 返回四个 display 对象。
- dashboard 页面能够展示 display status。
- 页面保持 fallback 与安全边界。
- 页面不生成真实 entry / stop / TP。
- 页面继续展示“非交易指令 / 需要人工复核 / 不自动执行”。

---

## 2. 本地环境

用户本地环境：

- 路径：`/Users/xuchao/Documents/trade-model-v1`
- 时间：2026-05-14 下午
- 操作方式：本地 Mac 终端 + 浏览器页面验证

---

## 3. 已执行命令与结果

### 3.1 DashboardDetailResponseVOTest

用户提供的终端结果显示：

```text
Running org.example.trademodel.vo.DashboardDetailResponseVOTest
Tests run: 8, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

结论：

```text
DashboardDetailResponseVOTest：PASS
```

说明：

- 新增 display model getter / setter 测试通过。
- safe default helper 测试通过。
- PlanBoundaryDisplayVO 默认 BACKEND_PENDING 测试通过。
- ExecutionPlanDisplayVO 默认 BOUNDARY_PENDING 测试通过。
- RiskActionGuardDisplayVO fail-closed 默认值测试通过。
- PaperObservationDisplayVO notRealPosition / notTradeInstruction 默认值测试通过。

---

## 4. 页面截图验证结果

用户提供了本地 dashboard 页面截图，截图中可见：

- `模块接入状态（只读）`
- `PLANBOUNDARY 状态（占位）`
- `BACKEND_PENDING / 后端未接入`
- `Entry 状态：INCOMPLETE / 后端未接入`
- `Stop 状态：INCOMPLETE / 后端未接入`
- 页面仍保留“只读 / 非交易指令 / 人工复核”语义

结论：

```text
/dashboard 页面：已打开
PlanBoundary 状态卡片：可见
BACKEND_PENDING：可见
INCOMPLETE：可见
后端未接入：可见
```

---

## 5. API curl 验证状态

PR #25 建议执行：

```bash
curl -s "http://localhost:8081/api/dashboard/detail?symbol=BTCUSDT" | grep -E "planBoundaryDisplay|executionPlanDisplay|riskActionGuardDisplay|paperObservationDisplay|BACKEND_PENDING|BOUNDARY_PENDING|notTradeInstruction|manualReviewRequired"
```

当前用户尚未提供该 curl 原始输出。

记录状态：

```text
curl 原始输出：未提供 / 待补充
```

因此，本 PR 只能确认：

- 单测通过。
- 页面截图验证通过。
- display status 页面可见。

不能声称已经人工核验 curl 原始输出。

---

## 6. 当前 smoke 结论

| 验收项 | 结果 | 说明 |
|---|---|---|
| `DashboardDetailResponseVOTest` | PASS | 用户提供 BUILD SUCCESS，8 tests passed。 |
| `/dashboard` 页面打开 | PASS | 用户提供截图。 |
| 模块接入状态可见 | PASS | 截图可见。 |
| PlanBoundary 状态卡片可见 | PASS | 截图可见。 |
| BACKEND_PENDING 可见 | PASS | 截图可见。 |
| INCOMPLETE 可见 | PASS | 截图可见。 |
| 非交易 / 人工复核语义 | PASS | 截图与页面文案可见。 |
| `/api/dashboard/detail` curl 原始输出 | PENDING | 用户未提供原始 curl 输出。 |
| dashboard 是否读取真实 display JSON 字段 | PARTIAL | 页面可见默认 display 状态；原始 API 输出未贴出。 |

---

## 7. 安全边界复核

当前 smoke 结果未发现以下越界：

- 未发现 order API。
- 未发现自动交易。
- 未发现自动开仓。
- 未发现自动平仓。
- 未发现自动反手。
- 未发现真实 entry / stop / TP 数值生成。
- 未发现将纸面观察包装为真实持仓。

页面当前仍以：

- `BACKEND_PENDING`
- `INCOMPLETE`
- `后端未接入`
- `非交易指令`
- `需要人工复核`

作为主要安全展示语义。

---

## 8. 后续补充建议

为完成完整 smoke 闭环，后续建议补一轮 PR：

```text
PR #27：Dashboard display status API curl result 补充记录
```

该 PR 需要用户补充 curl 原始输出，确认 response JSON 中实际包含：

- `planBoundaryDisplay`
- `executionPlanDisplay`
- `riskActionGuardDisplay`
- `paperObservationDisplay`
- `BACKEND_PENDING`
- `BOUNDARY_PENDING`
- `notTradeInstruction`
- `manualReviewRequired`

---

## 9. 本 PR 验收标准

本 PR 验收标准：

- 只新增一个文档：`docs/PHASE_DASHBOARD_DISPLAY_STATUS_LOCAL_SMOKE_RESULT.md`。
- 无 Java 改动。
- 无 dashboard.html 改动。
- 无 schema 改动。
- 无 order API / 自动交易相关改动。
- 如实记录 DashboardDetailResponseVOTest PASS。
- 如实记录页面截图可见 BACKEND_PENDING / INCOMPLETE。
- 如实记录 curl 原始输出尚未提供。
