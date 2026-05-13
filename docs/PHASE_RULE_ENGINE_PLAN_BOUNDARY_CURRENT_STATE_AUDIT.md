# PHASE_RULE_ENGINE_PLAN_BOUNDARY_CURRENT_STATE_AUDIT

## 一、审计背景

本阶段只做只读审计，不做实现。

目标是确认 clean 主线中 RuleEngine / PlanBoundary 的真实落地状态，区分已经进入 `src/main/java`、`src/test/java`、`src/main/resources` 的代码能力，与仅存在于 docs closure / untracked 文档中的方案或历史记录。

本审计不把 docs closure / untracked 文档当成已实现代码，不恢复项目外大轨道源码，不接自动交易 / order API。

## 二、当前 repo 状态

- 当前 HEAD：`9e6052b docs(dashboard): record home layout P2 verification`
- tracked clean
- staged 为空
- `src/main/java` / `src/test/java` / `src/main/resources` 下无 untracked
- `docs` 下仍有大量 untracked，未处理

## 三、当前已实现内容

当前 clean 主线中已经存在以下能力：

- `ExecutionPlanVO`
- `ExecutionPlanDO`
- `ExecutionPlanMapper`
- `tm_execution_plan` 文本字段
- `PlanServiceImpl`
- `PlanReadinessServiceImpl`
- dashboard detail 消费 plan / readiness
- `PlanBoundaryDisplayHelper` 只读解析展示
- PositionMonitor 边界展示语义

其中 `tm_execution_plan` 当前主要承载 `entry_zone`、`stop_loss`、`take_profit_rules`、`invalid_condition`、`plan_boundary_json` 等字段。已存在展示与读模型能力，但这不等于已经具备 RuleEngine 数值候选链路。

## 四、部分实现 / 弱实现内容

当前存在一些弱实现或承载位：

- `RuleEngineService` 目前只有接口。
- `DecisionContext` / `RuleBaseOutput` 是轻量对象。
- `plan_boundary_json` 有 schema 字段和展示 helper。
- `plan_boundary_json` 生产写入未接上。
- `PlanReadiness` 只是 read-model 标签和人工复核提示。

这些内容能支撑后续设计和展示，但不能视为完整 RuleEngine / PlanBoundary 主链路。

## 五、文档已有但代码未进 clean 主线

审计中发现大量 RuleEngine / PlanBoundary 相关文档或 closure 记录，包括但不限于：

- Runtime Kline
- Candidate
- VALID assembler
- RR / TP ladder
- swing / ATR
- structured would-write

这些大多存在于 docs / untracked 文档中，不能视为 clean 主线实现。后续不得直接把这些文档结论当成当前代码能力，也不应直接恢复未跟踪大轨道源码。

## 六、未实现内容

当前 clean 主线中未发现：

- 完整 RuleEngine 实现类
- `RuntimeKlineContext`
- Candidate DTO / service
- valid candidate assembler
- swing / ATR / RR / TP ladder 工具
- 真实行情窗口输入接入 PlanBoundary candidate
- ExecutionPlan 消费真实 numeric boundary candidate

因此当前还没有形成 `RuleEngine -> PlanBoundary candidate -> ExecutionPlan numeric boundary` 的闭环。

## 七、entry / stop / TP 当前状态

当前状态如下：

- entry 无真实数值来源。
- stop 无真实数值来源。
- TP 无真实数值来源。
- 当前 `entry_zone` / `stop_loss` / `take_profit_rules` 为文本字段。
- `PlanServiceImpl` 当前写入 `暂无` 或文本。
- 测试可能插入字符串，但不能视为真实数值链路。
- 这些字段不是 K 线 / ATR / swing / RR 产生。
- 这些字段不可作为结构化价位真值追溯。
- dashboard/detail 可以展示文本，但不是可执行价位证明。

结论：entry / stop / TP 当前不能进入 numeric source runtime，也不能作为自动执行或价格比较依据。

## 八、INCOMPLETE 当前状态

当前没有独立 INCOMPLETE runtime gate。

已有相关但不等价的能力：

- `PlanReadinessServiceImpl` 有 `PLAN_FIELDS_INCOMPLETE` reason。
- `PlanReadiness` 可降为 `WATCH_ONLY`。
- `PlanBoundaryDisplayHelper` 有 `MISSING` / `INVALID` / `PARTIAL` / `STRUCTURED` / `UNSTRUCTURED_TEXT_ONLY` 展示状态。

当前没有代码强制 `entry/stop/TP 缺失 => INCOMPLETE candidate`。前端展示的是 readiness / plan 文本，不是 INCOMPLETE runtime gate。

## 九、PlanReadiness 当前状态

当前已存在：

- `PlanReadinessVO`
- reason
- sourceFields
- `PlanReadinessService`
- `PlanReadinessServiceImpl`
- dashboard detail 接入

`PlanReadinessServiceImpl` 会检查：

- `entryZone`
- `stopLoss`
- `takeProfitRules`
- `validPeriod`
- `invalidCondition`

但它只检查这些字段是否非空，不验证这些字段是否为真实数值，不连接 RuleEngine candidate。本质是 read-model 标签和人工复核提示，不是执行计划数值完备性证明。

## 十、Runtime Kline / Candidate 当前状态

当前 clean 主线状态：

- `RuntimeKlineContext` 主线未发现。
- Candidate DTO / service 主线未发现。
- valid candidate assembler 主线未发现。
- swing / ATR / RR / TP ladder 工具主线未发现。
- 真实行情窗口输入未接入 PlanBoundary candidate。
- ExecutionPlan 当前只消费 `PlanServiceImpl` 文本 plan。

因此 Runtime Kline / Candidate 仍应视为未进入 clean 主线的后续阶段能力。

## 十一、测试状态

已有相关测试：

- `PlanBoundaryDisplayHelperTest`
- `DecisionResultMapperLatestPlanIntegrationTest`
- `DashboardControllerTest`
- PositionMonitor 相关边界展示测试

缺失测试：

- RuleEngine runtime candidate
- `RuntimeKlineContext`
- entry / stop / TP numeric source
- VALID / INCOMPLETE candidate gate
- `RuleEngine -> PlanBoundary -> ExecutionPlan` 真实数值链路

当前已有测试多验证 DTO / mapper / 展示 helper / read-model 拼接，不验证真实 RuleEngine numeric candidate 生产链路。

## 十二、与 V1 框架差距

### 数据基础层

缺 runtime kline window 给 RuleEngine boundary 使用。

### 规则基础层

只有接口和轻量 context，缺完整 RuleEngine 实现。

### 证据评分

已有 evidence / score，但未映射为 numeric boundary candidate。

### 综合决策

Decision 可产出 invalid 相关信息，但 entry / stop / TP 未结构化。

### 执行计划

ExecutionPlan 仍是文本 / 占位。

### 监控复盘

`PlanBoundaryDisplayHelper` 只读展示，不做价格比较，不触发执行。

## 十三、风险动作分层提醒

必须继续保持以下边界：

- 高风险不等于自动平仓。
- 高风险不等于反手。
- 插针不等于趋势反转。
- 踩踏状态禁止机会推送。
- RuleEngine / PlanBoundary 后续不能直接生成自动交易动作。
- 只能生成人工复核计划或 INCOMPLETE。
- 不接 order API。

任何 numeric boundary、INCOMPLETE gate、candidate service 的后续实现，都必须保持人工复核语义，不得直接转成交易指令。

## 十四、后续推荐最小路径

建议顺序：

1. 先提交本当前状态审计文档。
2. 再创建 entry / stop / TP numeric source 方案文档。
3. 再创建 INCOMPLETE runtime gate 方案文档。
4. 再进入最小 DTO / service 实现。
5. 后续才考虑 dashboard 展示增强。

## 十五、明确不建议做什么

当前不建议：

- 不建议直接恢复 untracked 大轨道源码。
- 不建议直接实现 RuleEngine 大模块。
- 不建议直接改 schema。
- 不建议直接改 dashboard。
- 不建议把文档 closure 当成已落地代码。
- 不建议接自动交易 / order API。

## 当前结论

当前 clean 主线已经具备 ExecutionPlan 文本读写、PlanReadiness read-model、PlanBoundary 展示 helper 和 PositionMonitor 展示语义，但尚未具备 RuleEngine / PlanBoundary numeric candidate 主链路。

entry / stop / TP 当前没有真实数值来源，不可追溯，不可作为结构化价位真值，也不可进入自动比较或交易行为。下一步应先做 numeric source 与 INCOMPLETE runtime gate 的方案拆分，而不是直接恢复或实现大轨道源码。
