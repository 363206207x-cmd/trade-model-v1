# PHASE: Dashboard Live Observation MVP Smoke Verification（PR10）

## 1. 阶段目标

本阶段目标是对首页实盘观察驾驶舱 MVP 的已完成方案与占位展示做一次 smoke 验证与截图验收定义。

本 PR 只做验收文档，不修改代码、不启动服务、不运行交易逻辑、不改 dashboard、不改后端。

本阶段用于收口以下链路：

- PR #3：首页 MVP 总方案。
- PR #4：首页 MVP checklist。
- PR #5：模块接入状态只读展示。
- PR #6：PlanBoundary 状态占位展示。
- PR #7：Risk Action Guard 占位展示。
- PR #8：纸面交易 / 人工复盘入口方案。
- PR #9：ExecutionPlan 与 Boundary 状态展示对齐方案。

---

## 2. Smoke 验证对象

后续本地或 CI smoke 应验证以下页面区域是否存在且语义正确：

1. **模块接入状态（只读）**
   - 展示模块接入成熟度。
   - 明确非交易指令 / 需要人工复核。

2. **PlanBoundary 状态（占位）**
   - 显示 BACKEND_PENDING / 后端未接入。
   - Entry / Stop / Take Profit 显示 INCOMPLETE / 后端未接入。
   - 不展示任何伪造 entry / stop / TP 数值。

3. **Risk Action Guard（占位）**
   - 覆盖四类风险动作分层。
   - 明确踩踏状态禁止反手、禁止新开仓、禁止机会推送。
   - 明确短线插针不等于趋势反转。

4. **纸面交易 / 人工复盘入口方案边界**
   - 当前仅有方案文档。
   - 后续入口不得被实现为真实交易入口。

5. **ExecutionPlan 与 Boundary 对齐边界**
   - ExecutionPlan 不得绕过 PlanBoundary。
   - WATCH_ONLY / INCOMPLETE / INVALID 不得包装为可执行机会。

---

## 3. 本地 smoke 建议步骤（后续人工执行）

> 本 PR 不执行以下命令，只定义后续验收步骤。

建议后续在本地执行：

```bash
cd /Users/xuchao/Documents/trade-model-v1
./mvnw clean compile
./mvnw test-compile
./mvnw spring-boot:run
```

启动后打开：

```text
http://localhost:8081/dashboard
```

---

## 4. 截图验收要求

后续截图至少应覆盖：

1. 首页顶部与系统摘要区域。
2. 模块接入状态（只读）卡片。
3. PlanBoundary 状态（占位）卡片。
4. Risk Action Guard（占位）卡片。
5. 第二层重点资产卡片区域。
6. 第三层当前结论 / 执行建议区域。

截图中必须能看到：

- 非交易指令。
- 需要人工复核。
- 后端未接入 / 仅占位 / INCOMPLETE。
- 不出现自动下单、自动开仓、自动平仓、自动反手文案。

---

## 5. 页面文案验收标准

页面必须符合以下文案边界：

- 可以出现：
  - 只读
  - 占位
  - 后端未接入
  - INCOMPLETE
  - 非交易指令
  - 需要人工复核
  - 等待后端接入

- 不允许出现：
  - 自动下单
  - 自动开仓
  - 自动平仓
  - 自动反手
  - 一键交易
  - 自动止损
  - 自动止盈
  - 可直接执行
  - 已自动执行

---

## 6. V1 安全边界复核

首页 MVP 当前仍必须遵守：

- 不接 order API。
- 不自动交易。
- 不自动开仓。
- 不自动平仓。
- 不自动反手。
- 不做全市场扫描。
- 不将 Display Slots 误认为推送全集。
- 不将 Watchlist Pool 之外的资产纳入机会推送候选。
- 不伪造 entry / stop / TP。
- 不把 Risk Action Guard 展示转换成自动动作。

---

## 7. 后续建议

完成本 smoke 验收文档后，建议进入下一阶段：

1. dashboard ExecutionPlan / Boundary 对齐只读占位。
2. dashboard INCOMPLETE 原因与缺失字段展示。
3. dashboard 纸面观察 / 人工复盘入口只读占位。
4. 后端 PlanBoundary / ExecutionPlan 真实字段接入方案。
5. 本地截图验收记录文档。

---

## 8. 本 PR 验收标准

本 PR 的验收标准：

- 只新增一个文档：`docs/PHASE_DASHBOARD_LIVE_OBSERVATION_MVP_SMOKE_VERIFICATION.md`。
- 无 `src/` 改动。
- 无 `dashboard.html` 改动。
- 无 schema 改动。
- 无 `pom.xml` 改动。
- 无 order API / 自动交易相关改动。
- 文档可作为首页 MVP 当前阶段的 smoke 验收依据。
