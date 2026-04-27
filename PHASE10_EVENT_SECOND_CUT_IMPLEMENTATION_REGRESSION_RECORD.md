# Phase 10 Event Second Cut Implementation Regression Record

> 评审对象：事件第二刀（输入契约定义 -> 实现前评审 -> 实现后回归）
> 状态：定稿
> 归档口径：Implemented + Verified + Observation

---

## 1) 变更范围与冻结约束核对

- 变更范围：本次实现仅落在“事件第二刀输入契约”。
- 新增 `EventImpactInputVO`，并在 `AssetAnalysisVO` 承载 `eventImpactInput`。
- 契约字段均来自 `tm_hot_reset_event`：`eventFactHit`、`eventFactCount`、`eventLatestTime`、`eventReasonCode`、`eventTriggerType`、`eventVersion`、`eventTraceId`。

冻结约束核对结果：

- 不改 UI 展示。
- 不改评分公式或决策逻辑主路径。
- 不并行扩面，不引入外部事件源。
- 事件冲击分仍为评分模块内单项增量，不升级为 decision 硬闸门。

---

## 2) 输入契约落地清单

字段与来源映射（唯一真值源：`tm_hot_reset_event`）：

1. `eventFactHit`（Boolean）：是否命中事件事实。
2. `eventFactCount`（Integer）：按 `analysis_id` 统计事件条数。
3. `eventLatestTime`（Timestamp）：按 `analysis_id` 取 latest 事件行的 `event_time`（`ORDER BY event_time DESC LIMIT 1`）。
4. `eventReasonCode`（String）：latest 事件 `trigger_reason_code`。
5. `eventTriggerType`（String）：latest 事件 `trigger_type`。
6. `eventVersion`（Integer）：latest 事件 `event_version`。
7. `eventTraceId`（String）：latest 事件 `trace_id`。

来源映射结论：所有字段均通过 `analysis_id` 从 `tm_hot_reset_event` 提取，来源唯一且可回溯。

---

## 3) 真值唯一性与禁止来源核查结果

- 真值唯一性：通过。字段真值均来自 `tm_hot_reset_event`，并可回链到具体事件行。
- 禁止来源核查：通过。未使用以下来源：
  - 前端推断或 UI fallback 文案
  - `evidence.description` 文本反推结构化输入
  - 手工录入或临时字段
  - 外部事件源

---

## 4) 缺失语义核查结果（null/0/false）

- `eventFactHit=false`：表示未检出事件事实，不等于“无事件冲击”。
- `eventFactCount=0`：表示事件条数为 0，不自动推导低风险或无扰动。
- `eventLatestTime/eventReasonCode/eventTriggerType/eventVersion/eventTraceId=null`：表示缺失对应事件事实字段，不引入默认业务推断。

结论：缺失语义处理与评审定义一致，未出现“缺失即结论”。

---

## 5) 回放一致性与可追溯核查结果

约束条件：

- 在同一事实集（`tm_hot_reset_event` 数据不变）下，同一 `analysis_id` 回放结果一致。

核查结果：

- 回放一致性：通过。
- 可追溯性：通过。每个输入字段均可追溯到 `tm_hot_reset_event` 具体记录与关联键。

---

## 6) 测试执行记录

测试命令：

`./mvnw -Dtest=ScoreServiceImplTest,EvidenceServiceImplTest test`

测试结果：

- 总测试数：70
- 失败数：0
- 错误数：0
- 跳过数：0
- 构建结果：BUILD SUCCESS

回归结论：`ScoreServiceImplTest` 与 `EvidenceServiceImplTest` 均通过，未引入新增失败。

---

## 7) 结论与后续动作

最终结论：

事件第二刀输入契约已完成实现并通过回归验证（保持 GO）。

后续动作：

- 将本记录作为实现后归档凭证，纳入 Phase10 索引入口。
- 保持冻结约束（不改 UI / 不改公式 / 不改 decision 主路径）持续生效。
- 进入观察期；若后续需求超出输入契约范围，必须新开评审，不在本刀内扩面。

---

## 固定口径

事件第二刀本轮完成输入契约实现与回归验证，状态标记为：`Implemented + Verified + Observation`。
