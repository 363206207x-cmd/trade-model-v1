# PlanBoundary Entry / Stop / TP Numeric Source Plan

## 一、方案背景

- 本方案基于 `docs/PHASE_RULE_ENGINE_PLAN_BOUNDARY_CURRENT_STATE_AUDIT.md` 的当前状态审计结论。
- 当前 clean 主线中，entry / stop / TP 不能视为真实可执行价位。
- 当前 `entry_zone` / `stop_loss` / `take_profit_rules` 仍主要是文本字段，不能作为结构化 numeric truth。
- 本方案只定义后续 PlanBoundary numeric source 规则、禁止来源、降级规则和最小实现路径。
- 本阶段不做实现，不改 schema，不改 dashboard，不接自动交易，不接 order API。

## 二、核心原则

- 不能凭 AI 自然语言文本直接生成 entry / stop / TP。
- 不能凭 summary 文案生成价位。
- 不能凭单一价格点生成完整计划。
- entry / stop / TP 必须有可追溯数据来源。
- 数据不足必须返回 `INCOMPLETE`。
- 结构冲突必须返回 `INCOMPLETE` 或 `WATCH_ONLY`。
- 价格边界只能用于人工复核计划，不能用于自动交易动作。
- 不自动下单。
- 不自动开仓。
- 不自动平仓。
- 不自动反手。

## 三、允许的 numeric source 类型

### 1. Kline window

- 最近 N 根 K 线。
- 必须包含 `high` / `low` / `close` / `volume`。
- 多周期优先使用 `15m` / `1h` / `4h`。
- `5m` 只用于短期插针 / 流动性风险辅助，不单独决定趋势边界。

### 2. Swing structure

- recent swing high。
- recent swing low。
- local support / resistance。
- structure invalidation level。

### 3. ATR / volatility buffer

- 用于 stop buffer。
- 用于 entry zone 宽度。
- 用于避免止损过近。

### 4. Risk reward rules

- minimum RR。
- TP ladder。
- partial take profit。
- 如果 RR 不达标，candidate 必须降级或 invalid。

### 5. Decision direction

- 支持 `bullish` / `bearish` / `wait`。
- direction 只提供方向，不直接提供价位。
- direction 与 entry / stop / TP 边界必须一致。

### 6. Data quality / source health

- `dataQualityScore`。
- `sourceType`。
- stale / missing / fallback。
- 数据质量不足时必须降级为 `INCOMPLETE` 或 `WATCH_ONLY`。

## 四、禁止的 numeric source 类型

- AI 自然语言直接给价。
- dashboard 展示文本反推价位。
- `PlanServiceImpl` 的 `暂无` / 占位文本。
- 测试插入字符串。
- 新闻摘要中的价格。
- 单点 `latestPrice` 直接推导 entry / stop / TP。
- 没有 timeframe 的裸价格。
- 没有 `sourceFields` 的价格。
- 无法追溯的数据。

## 五、entry 数值来源规则

- 趋势突破型 entry：基于结构突破位 / 回踩确认区。
- 回踩承接型 entry：基于支撑区 / swing low buffer。
- 反弹做空型 entry：基于阻力区 / swing high buffer。
- 事件观望型：不给 entry，返回 `INCOMPLETE` 或 `WATCH_ONLY`。
- 高风险 / 数据不足：不给 entry。

必须满足：

- entry 不是一个点，优先使用 entry zone。
- entry zone 必须包含 `sourceFields`。
- entry 必须与 decision direction 一致。
- entry 与 stop 必须能形成有效 RR。

## 六、stop 数值来源规则

- 多头 stop：结构低点下方 + buffer。
- 空头 stop：结构高点上方 + buffer。
- buffer 可来自 ATR / volatility。
- stop 不能只来自固定百分比。
- stop 不能离 entry 过近。
- stop 不能违反最大风险约束。

必须满足：

- 没有结构失效位时，不给 stop。
- 没有 stop 时，整个 plan 必须 `INCOMPLETE`。
- 高流动性踩踏时，不直接给市价退出建议。

## 七、TP 数值来源规则

- TP1 / TP2 / TP3 可来自 RR ladder。
- TP 可参考前高 / 前低 / 阻力 / 支撑。
- TP 必须与 entry / stop 形成合理 RR。
- TP 不能只由 AI 文案给出。
- TP ladder 需要记录 `sourceFields`。

必须满足：

- 没有 stop 不允许给完整 TP ladder。
- RR 不达标时，计划降级。
- TP 是人工复核计划，不是自动止盈指令。

## 八、INCOMPLETE 条件

以下情况必须 `INCOMPLETE`：

- 缺少 runtime kline window。
- 缺少 timeframe。
- 数据质量低于阈值。
- kline stale。
- 无法识别 swing high / low。
- 无法生成结构失效位。
- entry / stop / TP 任一关键边界缺失。
- entry-stop RR 不达标。
- direction 与 boundary 冲突。
- 多周期冲突严重。
- 仅有 AI 文本，无结构化 numeric source。
- 仅有 latestPrice，无边界依据。
- 存在踩踏 / 极端流动性恶化。
- 强反转未被多周期确认。
- 仅短线插针。

## 九、sourceFields / audit 字段建议

每个 boundary candidate 建议记录：

- `symbol`
- `timeframe`
- `sourceType`
- `dataQualityScore`
- `klineWindowStart`
- `klineWindowEnd`
- `entrySource`
- `stopSource`
- `tpSource`
- `swingHighRef`
- `swingLowRef`
- `atrValue`
- `bufferRule`
- `rrValue`
- `invalidReason`
- `evidenceRefs`
- `ruleVersion`

## 十、candidate 状态建议

- `VALID`
- `INCOMPLETE`
- `INVALID`
- `WATCH_ONLY`

状态含义：

- `VALID` 仅代表可人工复核，不代表自动交易。
- `INCOMPLETE` 表示不能给完整价位。
- `INVALID` 表示边界冲突或规则失败。
- `WATCH_ONLY` 表示只观察，不给执行价位。

## 十一、与 PlanReadiness 的关系

- 当前 `PlanReadiness` 不能证明价位真实。
- 后续 `PlanReadiness` 应消费 boundary candidate 状态。
- `PlanReadiness` 可以展示 `blockingReasons`。
- `PlanReadiness` 不应替代 RuleEngine numeric gate。

## 十二、与 ExecutionPlan 的关系

- 当前 `ExecutionPlan` 是文本计划。
- 后续可新增 structured boundary 写入。
- 在没有 structured boundary 前，`entryZone` / `stopLoss` / `takeProfitRules` 只能作为文本展示。
- 不得把文本 plan 误标为 numeric truth。

## 十三、与 dashboard 的关系

- dashboard 可以展示 `VALID` / `INCOMPLETE` / `WATCH_ONLY`。
- 不应把 `INCOMPLETE` 展示成失败。
- 应提示“当前缺少结构化价位来源”。
- 本阶段不改 dashboard。

## 十四、风险动作分层提醒

- 高风险不等于自动平仓。
- 高风险不等于反手。
- 插针不等于趋势反转。
- 踩踏状态禁止机会推送。
- 流动性恶化时不建议市价一次性砍仓。
- RuleEngine / PlanBoundary 只生成计划边界，不生成自动交易动作。
- 不接 order API。

## 十五、后续最小实现路径

1. 提交本 numeric source 方案文档。
2. 创建 INCOMPLETE runtime gate 方案文档。
3. 创建 boundary candidate DTO 方案。
4. 创建 RuntimeKlineContext 最小实现 checklist。
5. 创建 candidate service 最小实现。
6. 创建 tests。
7. 再考虑 schema / dashboard 写入。

## 十六、本阶段不做内容

- 不实现 RuleEngine。
- 不改 schema。
- 不改 dashboard。
- 不恢复 untracked 大轨道源码。
- 不接 AI 真下单。
- 不接 order API。
- 不改 Push Watchlist。
- 不改 Display Slots。
- 不做 TradeReview / Opportunity / RuleImprovement。
