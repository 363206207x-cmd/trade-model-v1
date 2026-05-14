# Dashboard Live Observation MVP Checklist

## 一、Checklist 目标

- 将 PR #3 的 V1 实盘观察驾驶舱 MVP 方案拆解为可执行、可审查、可验收的小 PR。
- 采用“先首页只读展示与状态占位，再逐步接入后端真实字段”的实施路径。
- 明确首页不得伪造后端未接入能力，所有未接入信息必须显式标注风险与限制。

## 二、实施顺序（后续 PR 拆分）

1. **dashboard 只读展示“模块接入状态”**  
   输出首页只读模块清单与状态枚举展示框架（不改后端业务逻辑）。
2. **dashboard 展示 PlanBoundary 状态占位**  
   输出 PlanBoundary 状态卡片占位（含 VALID / WATCH_ONLY / INCOMPLETE / INVALID / BACKEND_PENDING）。
3. **dashboard 展示 entry / stop / TP 完整性状态占位**  
   输出关键价格字段完整性状态，不展示伪造数值。
4. **dashboard 展示 INCOMPLETE 原因占位**  
   输出缺失原因区域，支持“字段缺失 / 来源缺失 / 后端未接入”等可读原因。
5. **dashboard 展示 Risk Action Guard 四类状态占位**  
   输出四类风险状态占位与禁止项提示（仅展示，不触发交易）。
6. **dashboard 展示 Watchlist Pool / Display Slots 边界说明**  
   输出观察池与展示位边界规则说明，避免误解为全市场扫描。
7. **纸面交易 / 人工复盘入口方案**  
   输出只读入口设计说明与跳转策略（不接实盘下单）。
8. **ExecutionPlan 与 Boundary 状态展示对齐方案**  
   输出状态映射规则，确保两者在首页语义一致。
9. **后端 PlanBoundary / ExecutionPlan 真实字段接入方案**  
   输出后端字段接入步骤与接口契约变更计划（仍不涉及 order API / 自动交易）。
10. **dashboard smoke 验证与截图验收文档**  
    输出最小 smoke 清单、截图要求与验收记录模板。

## 三、每个 PR 的验收原则

- 一个 PR 只交付一个最小可验收物，范围必须单一、可回滚。
- 页面类 PR 不得顺手修改后端业务逻辑。
- 后端类 PR 不得顺手大改 dashboard。
- 未接入字段必须明确显示：**“后端未接入 / 仅占位 / 需要人工复核”**。
- 任意 entry / stop / TP 在无真实来源时，必须显示 **INCOMPLETE**，严禁伪造价格。
- 任意 Risk Action Guard 相关内容仅允许展示与提示，严禁触发自动交易。
- 任意 order API / 自动交易改动默认不允许，若出现视为越界。

## 四、模块状态枚举草案

- **CONNECTED**：已接入。
- **PARTIAL**：部分接入。
- **BACKEND_PENDING**：后端未接入。
- **PLACEHOLDER_ONLY**：仅占位。
- **MANUAL_REVIEW_REQUIRED**：需要人工复核。

## 五、PlanBoundary 展示验收

- **VALID**：可展示结构化边界，但必须同时显示“非交易指令 / 需要人工复核”。
- **WATCH_ONLY**：仅观察状态，不输出完整执行计划。
- **INCOMPLETE**：必须展示缺失原因，且禁止包装为可执行机会。
- **INVALID**：必须明确标记失效，不包装为机会。
- **BACKEND_PENDING**：必须展示“后端未接入”提示。

## 六、Risk Action Guard 首页验收

首页展示必须覆盖以下四类状态：

1. 风险高但流动性正常。
2. 风险高且流动性恶化。
3. 风险高且存在踩踏。
4. 风险高但仅短线插针。

并必须明确以下硬约束：

- “风险高且存在踩踏”状态下：**禁止反手、禁止新开仓、禁止机会推送**。
- 所有 Risk Action Guard 状态均为观察提示，不构成自动交易指令。

## 七、不做什么（Out of Scope）

- 不改 `src/`。
- 不改 `dashboard.html`。
- 不改 schema。
- 不接 order API。
- 不自动交易。
- 不做全市场扫描。
- 不做完整 RuleEngine 重构。
- 不做完整三 AI 重构。
- 不做 meme 系统并入。

## 八、验收标准

- 本 PR 只新增一个 checklist 文档：`docs/PHASE_DASHBOARD_LIVE_OBSERVATION_MVP_CHECKLIST.md`。
- 无代码改动。
- 无 dashboard 改动。
- 无 schema 改动。
- 文档可直接作为 PR #5 开始实现“dashboard 只读接入状态展示”的执行依据。
