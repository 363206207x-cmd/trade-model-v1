# PlanBoundary INCOMPLETE Runtime Gate Plan

## 一、方案背景

- 当前 clean 主线中的 PlanBoundary 仍缺少独立 runtime gate。
- entry / stop / TP 不可在数据不足、结构冲突或来源不可追溯时硬给。
- 本方案承接 `docs/PHASE_PLAN_BOUNDARY_ENTRY_STOP_TP_NUMERIC_SOURCE_PLAN.md`，用于定义 INCOMPLETE runtime gate 的规则、触发条件、输出状态和后续最小实现路径。
- 当前 `PlanReadiness` 只是 read-model 标签，不是 runtime gate。
- 当前 `ExecutionPlan` 仍主要是文本计划。
- 本阶段不实现，不改 schema，不改 dashboard，不接自动交易，不接 order API。

## 二、INCOMPLETE runtime gate 定义

- `INCOMPLETE` 是运行时阻断状态。
- `INCOMPLETE` 表示当前不能生成完整结构化价位边界。
- `INCOMPLETE` 不是系统失败。
- `INCOMPLETE` 不是交易信号。
- `INCOMPLETE` 不是方向反转。
- `INCOMPLETE` 不是自动平仓 / 反手理由。
- `INCOMPLETE` 只能提示人工复核或继续观察。

## 三、candidate 状态定义

- `VALID`：结构化边界足够，可人工复核。
- `INCOMPLETE`：关键数据或边界不足，不能给完整计划。
- `INVALID`：规则冲突或边界无效。
- `WATCH_ONLY`：只观察，不给执行价位。

必须明确：

- `VALID` 不等于自动交易。
- `INCOMPLETE` 不等于失败。
- `WATCH_ONLY` 不等于看空 / 看多。
- `INVALID` 不等于自动反手。

## 四、必须 INCOMPLETE 的条件

以下情况必须进入 `INCOMPLETE`：

- 缺少 runtime kline window。
- 缺少 timeframe。
- kline stale。
- `dataQualityScore` 低于阈值。
- `sourceType` 不可信或 fallback 不足以支持边界。
- 缺少 swing high / swing low。
- 缺少结构失效位。
- entry 缺失。
- stop 缺失。
- TP 缺失。
- entry / stop / TP 任一 `sourceFields` 缺失。
- RR 不达标。
- direction 与 boundary 冲突。
- 多周期冲突严重。
- 只有 AI 文本。
- 只有 latestPrice。
- 只有 dashboard 文本。
- 只有 `PlanServiceImpl` 文本字段。
- 只有测试字符串。
- 强反转未多周期确认。
- 仅短线插针。
- 踩踏 / 极端流动性恶化。
- 流动性不足以支持执行计划。

## 五、WATCH_ONLY 条件

以下情况可进入 `WATCH_ONLY`：

- 市场方向可观察，但价位边界不完整。
- 风险较高但未触发 `INVALID`。
- 数据质量边缘。
- 多周期轻微冲突。
- 事件窗口中。
- 只有方向没有可执行边界。
- AI / 规则意见冲突但未到 `INVALID`。

说明：

- `WATCH_ONLY` 只能显示观察建议。
- `WATCH_ONLY` 不给 entry / stop / TP 完整计划。
- `WATCH_ONLY` 不推导自动交易动作。

## 六、INVALID 条件

以下情况应进入 `INVALID`：

- entry 与 stop 方向冲突。
- stop 在逻辑错误一侧。
- TP 与方向冲突。
- RR 明显不达标。
- boundary 与 decision direction 冲突。
- boundary `sourceFields` 互相矛盾。
- 数据源明显错误。
- 规则版本不匹配。
- 结构失效已经发生。

说明：

- `INVALID` 是计划无效。
- `INVALID` 不是自动反手。
- `INVALID` 不是自动平仓。

## 七、VALID 条件

进入 `VALID` 必须同时满足：

- 有可信 runtime kline window。
- 有明确 timeframe。
- 有 direction。
- 有 entry zone。
- 有 stop。
- 有 TP 或 TP ladder。
- 有 `sourceFields`。
- 有 `dataQualityScore`。
- 有 `rrValue`。
- 有 `invalidCondition`。
- direction 与 boundary 一致。
- RR 达标。
- 没有严重多周期冲突。
- 没有踩踏 / 极端流动性恶化。

说明：

- `VALID` 只是可人工复核计划。
- `VALID` 仍不自动交易。

## 八、INCOMPLETE 输出字段建议

建议输出字段：

- `boundaryStatus`
- `boundaryStatusText`
- `statusReason`
- `blockingReasons`
- `missingFields`
- `invalidFields`
- `sourceFields`
- `dataQualityScore`
- `sourceType`
- `timeframe`
- `ruleVersion`
- `canGenerateEntry`
- `canGenerateStop`
- `canGenerateTp`
- `manualReviewRequired`
- `notTradeInstruction`

## 九、与 PlanReadiness 的关系

- 当前 `PlanReadiness` 只是 read-model 标签。
- 后续 `PlanReadiness` 应消费 `boundaryStatus`。
- `INCOMPLETE` 应映射为 `WATCH_ONLY` / `NOT_READY` 类提示。
- `blockingReasons` 应进入 `PlanReadiness`。
- `sourceFields` 应进入 `PlanReadiness`。
- `PlanReadiness` 不应替代 runtime gate。

## 十、与 ExecutionPlan 的关系

- `VALID` 时可写结构化 boundary。
- `INCOMPLETE` 时不得写完整 entry / stop / TP numeric truth。
- `INCOMPLETE` 时可以写 watch-only 文本 plan。
- `INVALID` 时应标记计划不可用。
- 当前文本字段不能被误认为结构化价位。
- 后续可在 `plan_boundary_json` 或结构化字段中记录状态。

## 十一、与 dashboard 的关系

- dashboard 应展示 `VALID` / `INCOMPLETE` / `WATCH_ONLY` / `INVALID`。
- `INCOMPLETE` 文案应是“缺少结构化价位来源”。
- `INCOMPLETE` 不应展示为系统错误。
- `INCOMPLETE` 不应展示为自动交易建议。
- 本阶段不改 dashboard。

## 十二、与 Risk Action Guard 的关系

- 高风险不等于自动平仓。
- 高风险不等于反手。
- 插针不等于趋势反转。
- 踩踏状态禁止机会推送。
- 流动性恶化时不建议市价一次性砍仓。
- `INCOMPLETE` 不能触发自动交易。
- `INVALID` 不能触发自动反手。
- `WATCH_ONLY` 不能触发开仓。

## 十三、状态转换建议

- missing data -> `INCOMPLETE`。
- weak data / event window -> `WATCH_ONLY`。
- conflict boundary -> `INVALID`。
- sufficient numeric source -> `VALID`。
- stampede / extreme liquidity -> `WATCH_ONLY` 或 `INCOMPLETE`，并禁止机会推送。
- short wick only -> `WATCH_ONLY` / `INCOMPLETE`，不判趋势反转。

## 十四、后续最小实现路径

1. 提交本 INCOMPLETE runtime gate 方案文档。
2. 创建 BoundaryCandidate DTO 方案文档。
3. 创建 RuntimeKlineContext 最小实现 checklist。
4. 创建 BoundaryCandidateService 最小实现 checklist。
5. 创建 INCOMPLETE gate 单元测试方案。
6. 最小实现只读 candidate，不写 ExecutionPlan。
7. 再评估 schema / dashboard 接入。

## 十五、本阶段不做内容

- 不实现 RuntimeKlineContext。
- 不实现 candidate service。
- 不改 schema。
- 不改 dashboard。
- 不改 ExecutionPlan 写入。
- 不恢复 untracked 大轨道源码。
- 不接自动交易。
- 不接 order API。
- 不做 TradeReview / Opportunity / RuleImprovement。
- 不改 Push Watchlist / Display Slots。
