# PHASE: PaperObservationDisplayVO 最小对象方案（PR19）

## 1. 阶段目标

本阶段目标是定义 `PaperObservationDisplayVO` 的最小对象方案，为后续 dashboard detail read model 的 Java 最小实现做准备。

本 PR 只做方案文档，不实现 Java 代码、不改 dashboard、不改 schema、不新增接口、不接 order API、不自动交易。

核心目标：

- 将纸面观察 / 人工复盘入口作为 dashboard 的安全展示对象。
- 明确纸面观察不是实盘交易，人工复盘不代表真实持仓。
- 承载入口可用性、关联记录数量、漏失机会标记、复盘摘要与安全提示。
- 防止纸面记录被误写成真实仓位、实盘成交、一键交易或自动执行。

---

## 2. 对象定位

`PaperObservationDisplayVO` 是 dashboard read model 的展示层对象，用于说明当前资产是否具备纸面观察 / 人工复盘入口与关联记录状态。

它的定位是：

- 展示纸面观察入口是否可用。
- 展示人工复盘入口是否可用。
- 展示关联纸面观察 / 复盘记录数量。
- 展示是否存在漏失机会记录。
- 明确当前展示不是实盘持仓、不是交易指令、不是下单入口。

它不是：

- 实盘交易记录。
- 真实持仓记录。
- order API 请求对象。
- 自动跟单记录。
- 一键交易入口。
- 自动交易执行状态。

---

## 3. 最小字段建议

建议 `PaperObservationDisplayVO` 第一阶段包含以下字段：

- `paperObservationStatus`
- `paperObservationStatusLabel`
- `paperObservationAvailable`
- `manualReviewEntryAvailable`
- `linkedPaperObservationCount`
- `linkedReviewCount`
- `missedOpportunityFlag`
- `reviewSummary`
- `notRealPosition`
- `notTradeInstruction`
- `manualReviewRequired`
- `backendConnectionStatus`
- `updatedAt`

说明：

- 不包含 orderId / orderRequest / autoExecute / autoTrade。
- 不包含真实成交状态。
- 不包含真实持仓状态。
- 不触发任何交易动作。

---

## 4. 字段语义

### 4.1 `paperObservationStatus`

建议枚举：

- `AVAILABLE`
- `UNAVAILABLE`
- `BACKEND_PENDING`
- `PLACEHOLDER_ONLY`
- `MANUAL_REVIEW_REQUIRED`

语义：

- `AVAILABLE`：纸面观察入口可展示。
- `UNAVAILABLE`：当前不可用。
- `BACKEND_PENDING`：后端真实字段尚未接入。
- `PLACEHOLDER_ONLY`：仅占位展示。
- `MANUAL_REVIEW_REQUIRED`：需要人工复核。

### 4.2 `paperObservationStatusLabel`

建议中文文案：

- `纸面观察可用`
- `暂不可用`
- `后端未接入`
- `仅占位`
- `需要人工复核`

### 4.3 `paperObservationAvailable`

布尔值。

- true：前端可展示纸面观察入口。
- false：不展示入口或展示为不可用。

注意：可展示入口不代表可实盘交易。

### 4.4 `manualReviewEntryAvailable`

布尔值。

- true：前端可展示人工复盘入口。
- false：不展示入口或展示为不可用。

### 4.5 `linkedPaperObservationCount`

当前资产 / analysisId 关联的纸面观察记录数量。

第一阶段可为 0。

### 4.6 `linkedReviewCount`

当前资产 / analysisId 关联的人工复盘记录数量。

第一阶段可为 0。

### 4.7 `missedOpportunityFlag`

布尔值。

- true：存在或可记录漏失机会。
- false：未标记。

注意：漏失机会记录只用于复盘，不代表交易机会推送。

### 4.8 `reviewSummary`

复盘摘要文案。

第一阶段可以为空或显示后端未接入。

### 4.9 `notRealPosition`

必须默认 true。

含义：

- 纸面观察不是实盘持仓。
- 纸面记录不进入真实持仓监控。

### 4.10 `notTradeInstruction`

必须默认 true。

含义：

- 不是交易指令。
- 不能作为下单依据。

### 4.11 `manualReviewRequired`

必须默认 true。

### 4.12 `backendConnectionStatus`

建议枚举：

- `CONNECTED`
- `PARTIAL`
- `BACKEND_PENDING`
- `PLACEHOLDER_ONLY`
- `MANUAL_REVIEW_REQUIRED`

### 4.13 `updatedAt`

展示状态更新时间。

第一阶段可为 null，前端需安全兜底。

---

## 5. 安全默认值

后续 Java 最小实现建议默认值：

- `paperObservationStatus = BACKEND_PENDING`
- `paperObservationStatusLabel = 后端未接入`
- `paperObservationAvailable = false`
- `manualReviewEntryAvailable = false`
- `linkedPaperObservationCount = 0`
- `linkedReviewCount = 0`
- `missedOpportunityFlag = false`
- `notRealPosition = true`
- `notTradeInstruction = true`
- `manualReviewRequired = true`
- `backendConnectionStatus = BACKEND_PENDING`

推荐第一阶段采用 fail-closed：

- 未接入真实后端记录前，不展示可写入口。
- 未接入真实后端记录前，不允许一键交易语义。
- 任何纸面记录都不得自动同步为真实持仓。

---

## 6. 展示规则

### 6.1 AVAILABLE

当状态为 `AVAILABLE`：

- 可以展示纸面观察入口。
- 可以展示人工复盘入口。
- 必须显示“非交易指令 / 非实盘持仓 / 需要人工复核”。
- 不得显示一键交易或自动执行。

### 6.2 UNAVAILABLE

当状态为 `UNAVAILABLE`：

- 展示不可用原因。
- 不得展示可操作入口。

### 6.3 BACKEND_PENDING

当状态为 `BACKEND_PENDING`：

- 显示后端未接入。
- 不得假装已可记录。
- 不得展示为实盘交易入口。

### 6.4 PLACEHOLDER_ONLY

当状态为 `PLACEHOLDER_ONLY`：

- 可以展示占位说明。
- 不得提供真实写入动作。

### 6.5 MANUAL_REVIEW_REQUIRED

当状态为 `MANUAL_REVIEW_REQUIRED`：

- 显示需要人工复核。
- 不触发任何自动动作。

---

## 7. 与 DashboardDetailResponseVO 的关系

后续建议在 `DashboardDetailResponseVO` 中新增：

- `private PaperObservationDisplayVO paperObservationDisplay;`

但本 PR 不改 Java。

后续 Java 实现应保持：

- 现有 `decision` 字段不变。
- `paperObservationDisplay` 作为新增展示对象，向后兼容。
- 纸面观察字段不替代真实持仓字段。

---

## 8. 与 Position Monitor 的关系

纸面观察与 Position Monitor 必须严格区分：

- PaperObservation：假设性观察 / 纸面验证 / 人工复盘。
- Position Monitor：真实或手动录入持仓监控。

禁止：

- 将纸面观察误标为真实持仓。
- 将纸面记录自动进入持仓监控。
- 将纸面收益视作真实盈亏。

---

## 9. 与 PlanBoundary / ExecutionPlan 的关系

纸面观察可以记录当时状态，但不改变状态：

- 可以记录当时 `planBoundaryStatus`。
- 可以记录当时 `executionPlanStatus`。
- 可以记录当时风险状态。
- 不生成新的真实 PlanBoundary。
- 不生成新的真实 ExecutionPlan。
- 不触发任何执行动作。

---

## 10. 与 Risk Action Guard 的关系

纸面观察可以记录 Risk Action Guard 当时状态，但不得绕过它：

- 踩踏状态下，不得把纸面观察包装成机会推送。
- 流动性恶化状态下，不得把纸面记录包装成市价砍仓建议。
- 短线插针状态下，不得把纸面观察包装成趋势反转确认。

---

## 11. 后续 Java 实现建议

建议后续 Java PR 拆成：

1. 新增 `PaperObservationDisplayVO` 类或 `DashboardDetailResponseVO.PaperObservationDisplayVO` 内部静态类。
2. 新增 getter / setter。
3. 默认构造或 factory 输出 BACKEND_PENDING + fail-closed flags。
4. 单测验证安全默认值。
5. `DashboardDetailResponseVO` 新增字段。
6. `DashboardController` detail 组装默认 display。
7. 后续再接真实 PaperObservation / Review service。

---

## 12. 测试建议

后续 Java 最小实现应测试：

- 默认 `paperObservationStatus = BACKEND_PENDING`。
- 默认 `paperObservationAvailable = false`。
- 默认 `manualReviewEntryAvailable = false`。
- 默认 `notRealPosition = true`。
- 默认 `notTradeInstruction = true`。
- 默认 `manualReviewRequired = true`。
- 不包含 order / autoTrade / autoExecute 字段。
- 不包含真实持仓写入字段。

---

## 13. 明确不做什么

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
- 不实现纸面观察后端写入。
- 不实现 TradeReview 后端逻辑。
- 不把纸面观察同步为真实持仓。

---

## 14. 验收标准

本 PR 验收标准：

- 只新增一个方案文档：`docs/PHASE_PAPER_OBSERVATION_DISPLAY_VO_MINIMAL_OBJECT_PLAN.md`。
- 无 Java 代码改动。
- 无 dashboard 改动。
- 无 schema 改动。
- 无 order API / 自动交易相关改动。
- 文档明确 `PaperObservationDisplayVO` 最小字段。
- 文档明确纸面观察不是实盘交易。
- 文档明确纸面观察不等于真实持仓。
