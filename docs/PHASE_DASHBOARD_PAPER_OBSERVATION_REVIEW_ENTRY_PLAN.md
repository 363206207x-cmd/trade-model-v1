# PHASE: Dashboard 纸面交易 / 人工复盘入口方案（PR8）

## 1. 阶段目标

本阶段目标是定义首页“纸面交易 / 人工复盘入口”的**产品边界与语义约束**，用于后续 PR 按步骤落地。

- 该入口仅用于：
  - 记录观察（observation）
  - 纸面验证（paper validation）
  - 错失机会记录（missed opportunity）
  - 人工复盘（manual review）
- 该入口**不是**实盘下单入口。
- 本阶段明确**不接自动交易，不接 order API，不做真实下单、平仓、反手**。

---

## 2. 入口定位

### 2.1 纸面交易（Paper Observation / Paper Validation）

- “纸面交易”在本项目语义中定义为：
  - 仅在观察层记录“如果当时执行会怎样”的验证过程。
  - 不产生真实持仓，不改变真实资金状态。
- 纸面记录可用于策略判断质量评估，但**不代表真实仓位**。

### 2.2 人工复盘（Manual Review）

- 人工复盘用于用户手动记录：
  - 观察结果
  - 判断偏差
  - 错失机会
  - 风险误判
- 复盘内容用于后续回看与改进，不触发交易引擎动作。

### 2.3 入口动作边界

- 首页入口只负责“导向记录与复盘”。
- 首页入口**不产生任何交易动作**（包括但不限于下单、撤单、平仓、反手）。

---

## 3. 首页入口建议（方案级，暂不实现）

以下为 dashboard 后续可展示的入口建议，本 PR 不修改 dashboard：

1. **纸面观察记录入口**
   - 用于新增 paper observation / paper validation 记录。
2. **人工复盘记录入口**
   - 用于新增 manual review 记录。
3. **漏失机会记录入口**
   - 用于记录 missed opportunity 及原因标签。
4. **当前资产关联 analysisId / symbol 的记录入口**
   - 在已选资产上下文下快速进入记录页。
5. **“非交易指令 / 需要人工复核”提示**
   - 明确该区域不是交易执行区，所有内容仅为观察与复盘。

---

## 4. 建议记录字段草案（仅方案，不改 schema）

> 仅作为后续 DTO / schema 设计输入，本 PR 不落地数据结构变更。

- `symbol`
- `analysisId`
- `observationTime`
- `marketBiasAtObservation`
- `planBoundaryStatus`
- `riskLevel`
- `riskActionGuardStatus`
- `paperDecision`
- `manualNote`
- `expectedScenario`
- `actualOutcome`
- `missedOpportunityFlag`
- `mistakeTag`
- `reviewConclusion`
- `createdBy`
- `createTime`

---

## 5. 必须禁止的语义（强约束）

本入口及其后续实现必须明确禁止以下语义与能力：

- 自动下单
- 自动开仓
- 自动平仓
- 自动反手
- 一键交易
- 自动跟单
- 实盘成交确认
- 自动推送为真实交易执行
- 将纸面记录误标为真实持仓

---

## 6. 与现有模块关系

### 6.1 与 PlanBoundary

- 仅记录“当时看到的 PlanBoundary 状态”。
- 不生成新的真实边界，不改变边界判定逻辑。

### 6.2 与 ExecutionPlan

- 仅记录 ExecutionPlan 摘要（如用户观察到的关键信号）。
- 记录内容不代表计划已执行。

### 6.3 与 Risk Action Guard

- 仅记录当时风控状态。
- 不触发任何风控动作。

### 6.4 与 Watchlist Pool

- 只记录观察库资产相关观察。
- 不扩展为全市场扫描能力。

### 6.5 与 TradeReview

- 后续可逐步接入复盘表。
- 本 PR 不改 schema，不做表结构落地。

### 6.6 与 Position Monitor

- 纸面记录不等于手动持仓记录。
- Position Monitor 仍以真实持仓语义为准。

---

## 7. 后续 PR 拆分建议

建议按以下最小可审查单元推进：

1. dashboard 纸面观察入口只读占位。
2. 人工复盘入口只读占位。
3. 纸面观察记录 DTO / schema 方案。
4. 复盘记录最小后端接口方案。
5. dashboard 入口与 `symbol` / `analysisId` 关联方案。
6. smoke 验证与截图验收。

---

## 8. 验收标准

本 PR 的验收标准：

- 只新增一个方案文档：`docs/PHASE_DASHBOARD_PAPER_OBSERVATION_REVIEW_ENTRY_PLAN.md`。
- 无代码改动。
- 无 dashboard 改动。
- 无 schema 改动。
- 无 order API / 自动交易相关改动。
- 文档明确“纸面交易不是实盘交易”。

