# PHASE: Dashboard Live Observation MVP Plan

## 1. 阶段目标

本阶段目标是将首页升级为 **V1 实盘观察驾驶舱 MVP**，优先服务于以下使用场景：

- 重点观察资产的实时观察。
- 当前决策状态查看与人工判断辅助。
- PlanBoundary 状态可视化与边界约束提示。
- 风险动作提示（Risk Action Guard）首页占位提醒。
- 纸面验证（paper observation）与人工复盘入口聚合。

> 重要边界：本阶段不是自动交易系统。
>
> 本阶段不代表允许：自动下单、自动平仓、自动反手。

---

## 2. 首页必须优先展示的核心模块

首页作为主工作台，至少包含以下模块：

1. **Watchlist Pool / 观察库状态**
   - 展示资产是否位于观察库内。
   - 展示观察库状态摘要（如候选数量、更新时间、人工复核标记）。

2. **Display Slots / 首页展示位**
   - 展示当前首页展示位资产与顺序。
   - 显示槽位与观察库的关系（仅展示子集，不等于推送全集）。

3. **当前资产决策状态**
   - 偏多 / 偏空 / 观望（marketBias）。

4. **是否值得关注 / 是否值得开仓**
   - 价值判断分离：
     - 值得关注（可观察）
     - 值得开仓（可进入计划评估）

5. **PlanBoundary 状态**
   - VALID / WATCH_ONLY / INCOMPLETE / INVALID。

6. **entry / stop / TP 完整性状态**
   - 分别显示 entryStatus、stopStatus、takeProfitStatus。

7. **INCOMPLETE 原因展示**
   - 当 entry / stop / TP 不完整时，必须展示 incompleteReasons。

8. **ExecutionPlan 基础摘要**
   - 仅展示结构化摘要，不做自动执行语义。

9. **Risk Action Guard 状态占位**
   - 展示风险动作建议状态，不触发自动交易。

10. **手动持仓监控入口**
    - 提供跳转/入口用于人工盯盘与手动风险处理。

11. **纸面交易 / 复盘 / 漏失机会记录入口**
    - 支持记录观察结论、错失机会、后验复盘标签。

12. **模块接入状态**
    - 每模块显示：已接入 / 部分接入 / 后端未接入 / 仅占位 / 需要人工复核。

---

## 3. 前后端字段契约草案（仅方案，不实现接口）

首页展示字段草案如下：

- `symbol`
- `displaySlot`
- `inWatchlistPool`
- `marketBias`
- `confidenceLevel`
- `riskLevel`
- `isWorthOpening`
- `planBoundaryStatus`
- `entryStatus`
- `stopStatus`
- `takeProfitStatus`
- `incompleteReasons`
- `executionPlanSummary`
- `riskActionGuardStatus`
- `riskActionAdvice`
- `manualReviewRequired`
- `notTradeInstruction`
- `moduleConnectionStatus`

字段语义约束（草案）：

- `manualReviewRequired` 默认应为 true（除纯信息展示外）。
- `notTradeInstruction` 用于明确“非交易指令”提示文案。
- `moduleConnectionStatus` 统一枚举接入状态，避免 UI 假完成。

---

## 4. PlanBoundary 展示规则

首页必须严格遵循以下规则：

- **VALID**
  - 可展示结构化 entry / stop / TP。
  - 但必须明确标记“需要人工复核”，且“不是交易指令”。

- **WATCH_ONLY**
  - 仅允许观察，不给完整交易计划。
  - 不输出可直接执行的开仓结构。

- **INCOMPLETE**
  - 必须显示缺失原因（`incompleteReasons`）。
  - 前端禁止伪造价格、禁止补全虚构 stop/TP。

- **INVALID**
  - 明确失效。
  - 不允许生成开仓建议，不允许包装为可执行机会。

- **后端未接入场景**
  - 首页必须显示“后端未接入 / 等待接入”。
  - 禁止以默认值假装状态完整。

---

## 5. Risk Action Guard 首页占位规则

首页占位必须覆盖以下四类提醒：

1. **风险高但流动性正常**
   - 可考虑：减仓 / 移动止损 / 降低杠杆。

2. **风险高且流动性恶化**
   - 不建议市价一次性砍仓。
   - 优先：分批降风险 / 等待流动性恢复 / 只降杠杆。

3. **风险高且存在踩踏**
   - 禁止反手。
   - 禁止新开仓。
   - 禁止机会推送。
   - 第一优先级：保护本金。

4. **风险高但仅短线插针**
   - 不直接判定趋势反转。
   - 不生成反向开仓计划。
   - 仅做短线风险提醒与等待确认。

---

## 6. Watchlist / Display Slots 边界

必须在文档中明确以下边界：

- Display Slots 是首页展示位，默认可显示 **6 个资产**。
- Watchlist Pool 是推送候选的最大边界。
- 不在 Watchlist Pool 中的资产，不允许进入机会推送候选。
- Display Slots 不等于推送全集，也不是唯一观察库。
- 观察库数量可以多于 6 个。
- 后续低频扫描和机会提升能力属于后续阶段，不在本 MVP 内。

---

## 7. MVP 不做什么（明确排除项）

本阶段明确不做：

- 不自动交易。
- 不接 order API。
- 不自动开仓。
- 不自动平仓。
- 不自动反手。
- 不做全市场扫描。
- 不做 meme 系统并入。
- 不做完整三 AI 裁决重构。
- 不做完整 RuleEngine 重构。
- 不做完整复盘回流。
- 不直接大改 `dashboard.html`。

---

## 8. 后续 PR 拆分建议（小步推进）

建议按以下顺序推进：

- **PR #4**：创建 Dashboard Live Observation MVP checklist。
- **PR #5**：dashboard 只读展示接入状态，不改后端。
- **PR #6**：PlanBoundary 状态展示占位，不伪造数据。
- **PR #7**：Risk Action Guard 首页占位展示。
- **PR #8**：纸面观察 / 复盘入口方案。
- **PR #9**：ExecutionPlan 与 Boundary 状态展示对齐方案。

---

## 9. 验收标准

- 本 PR 只新增一个文档。
- 无 `src/` 改动。
- 无 `dashboard.html` 改动。
- 无 schema 改动。
- 无 order API / 自动交易相关改动。
- 文档可作为下一阶段首页 MVP 边界依据。

