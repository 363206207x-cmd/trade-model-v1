# PHASE: Dashboard Display Status Smoke Verification（PR25）

## 1. 阶段目标

本阶段目标是对 `/api/dashboard/detail` → `dashboard.html` 的 display status 字段链路进行 smoke 验证与截图验收定义。

本 PR 只新增验收文档，不改 Java、不改 dashboard、不改 schema、不接 order API、不自动交易。

核心目标：

- 确认 dashboard detail 后端已返回四个 display 对象。
- 确认 dashboard 前端已优先读取 detail display status 字段。
- 确认后端字段缺失时仍回退到安全占位文案。
- 确认页面不展示伪造 entry / stop / TP。
- 确认页面继续保留“非交易指令 / 需要人工复核 / 不自动执行”语义。

---

## 2. 已完成链路

当前已完成：

- PR #21：`DashboardDetailResponseVO` 新增四个 display model。
- PR #22：新增 `withSafeDefaultDisplays()` 与 `ensureSafeDefaultDisplays()`。
- PR #23：`/api/dashboard/detail` 使用 safe default displays 初始化响应。
- PR #24：`dashboard.html` 读取 detail display status 字段并保留 fallback。

链路目标：

```text
/api/dashboard/detail
↓
DashboardDetailResponseVO
↓
planBoundaryDisplay / executionPlanDisplay / riskActionGuardDisplay / paperObservationDisplay
↓
dashboard.html renderDisplayStatusCards(detail)
↓
PlanBoundary / Risk Action Guard / Paper Observation 状态卡片展示
```

---

## 3. Smoke 验证对象

后续 smoke 应验证以下 display 对象是否存在于 detail response：

1. `planBoundaryDisplay`
2. `executionPlanDisplay`
3. `riskActionGuardDisplay`
4. `paperObservationDisplay`

并验证以下默认值：

### 3.1 PlanBoundaryDisplay

- `planBoundaryStatus = BACKEND_PENDING`
- `planBoundaryStatusLabel = 后端未接入`
- `sourceTraceStatus = BACKEND_PENDING`
- `backendConnectionStatus = BACKEND_PENDING`
- `manualReviewRequired = true`
- `notTradeInstruction = true`

### 3.2 ExecutionPlanDisplay

- `executionPlanStatus = BOUNDARY_PENDING`
- `executionPlanStatusLabel = 等待边界接入`
- `executionPlanBoundaryAligned = false`
- `planBoundaryStatus = BACKEND_PENDING`
- `notExecutableReason = PLAN_BOUNDARY_BACKEND_PENDING`
- `manualReviewRequired = true`
- `notTradeInstruction = true`

### 3.3 RiskActionGuardDisplay

- `riskActionGuardStatus = BACKEND_PENDING`
- `riskActionGuardStatusLabel = 后端未接入`
- `liquidityState = BACKEND_PENDING`
- `opportunityPushAllowed = false`
- `reverseTradeAllowed = false`
- `newPositionAllowed = false`
- `marketOrderExitAllowed = false`
- `manualRiskReviewRequired = true`
- `notTradeInstruction = true`

### 3.4 PaperObservationDisplay

- `paperObservationStatus = BACKEND_PENDING`
- `paperObservationStatusLabel = 后端未接入`
- `paperObservationAvailable = false`
- `manualReviewEntryAvailable = false`
- `notRealPosition = true`
- `notTradeInstruction = true`
- `manualReviewRequired = true`

---

## 4. 建议本地命令

> 本 PR 不执行命令，只定义验收步骤。

建议本地执行：

```bash
cd /Users/xuchao/Documents/trade-model-v1
./mvnw clean compile
./mvnw test-compile
./mvnw -Dtest=DashboardDetailResponseVOTest test
./mvnw spring-boot:run
```

启动后访问：

```text
http://localhost:8081/dashboard
```

并测试 detail 接口：

```bash
curl -s "http://localhost:8081/api/dashboard/detail?symbol=BTCUSDT"
```

---

## 5. API 验收要求

`/api/dashboard/detail?symbol=BTCUSDT` 响应必须满足：

- 顶层仍包含 `symbol`。
- 顶层仍包含 `decision`。
- 顶层仍包含 `marketEnvironmentMini`。
- 顶层仍包含 `evidenceTopItems`。
- 顶层仍包含 `scoreTopItems`。
- 新增返回 `planBoundaryDisplay`。
- 新增返回 `executionPlanDisplay`。
- 新增返回 `riskActionGuardDisplay`。
- 新增返回 `paperObservationDisplay`。

不允许出现：

- `orderId`
- `orderRequest`
- `autoExecute`
- `autoTrade`
- 自动下单相关字段
- 自动开仓 / 平仓 / 反手字段

---

## 6. 页面截图验收要求

截图至少覆盖：

1. 首页顶部系统摘要。
2. PlanBoundary 状态卡片。
3. Risk Action Guard 状态卡片。
4. 当前选中资产触发 detail 后，状态卡片已刷新。
5. 页面仍显示“非交易指令 / 需要人工复核 / 不自动执行”。

截图中必须能看到：

- `BACKEND_PENDING`
- `BOUNDARY_PENDING`
- `后端未接入`
- `非交易指令`
- `需要人工复核`
- `不自动执行`

---

## 7. Fallback 验收要求

若 detail response 中某个 display 对象缺失，dashboard 必须：

- 不报错。
- 不空白崩溃。
- 回退到原来的 `BACKEND_PENDING / INCOMPLETE / 仅占位` 文案。
- 继续显示“非交易指令 / 需要人工复核”。

---

## 8. 安全边界复核

本阶段仍必须遵守：

- 不接 order API。
- 不自动交易。
- 不自动开仓。
- 不自动平仓。
- 不自动反手。
- 不生成真实 entry / stop / TP。
- 不把 `executionPlanSummary` 包装为可执行计划。
- 不把 WATCH_ONLY / INCOMPLETE / INVALID 包装为机会。
- 不把纸面观察包装为真实持仓。
- 不让 Risk Action Guard 触发任何自动动作。

---

## 9. 后续建议

完成本 smoke 验收文档后，建议进入：

1. PR #26：本地 smoke 结果记录文档。
2. PR #27：Dashboard display status 当前实现复核文档。
3. PR #28：真实 PlanBoundary adapter 接入前方案。
4. PR #29：PlanBoundaryDisplayVO 从后端真实 status 接入的最小 Java 方案。

---

## 10. 本 PR 验收标准

本 PR 验收标准：

- 只新增一个文档：`docs/PHASE_DASHBOARD_DISPLAY_STATUS_SMOKE_VERIFICATION.md`。
- 无 Java 改动。
- 无 dashboard.html 改动。
- 无 schema 改动。
- 无 order API / 自动交易相关改动。
- 文档明确 API 验收、页面截图验收、fallback 验收、安全边界。
