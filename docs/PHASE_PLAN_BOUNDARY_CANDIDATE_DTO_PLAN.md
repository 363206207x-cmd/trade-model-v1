# PlanBoundary BoundaryCandidate DTO Plan

## 一、方案背景

- BoundaryCandidate 是 PlanBoundary 后续最小实现的核心中间对象。
- 当前 clean 主线中，entry / stop / TP 暂无真实 numeric source。
- INCOMPLETE runtime gate 已有方案，但尚未实现。
- RuntimeKlineContext / Candidate service / RuleEngine 实现尚未进入 clean 主线。
- ExecutionPlan 仍主要是文本计划。
- PlanReadiness 是 read-model 标签，不是 runtime gate。
- BoundaryCandidate 后续用于连接 RuleEngine -> PlanBoundary -> PlanReadiness / ExecutionPlan。
- BoundaryCandidate 不是订单，不是交易指令。
- BoundaryCandidate 用于承载 entry / stop / TP 的结构化候选边界。
- BoundaryCandidate 必须携带 `sourceFields` 和 `blockingReasons`。
- 本阶段不实现，不改 schema，不改 dashboard，不接自动交易，不接 order API。

## 二、BoundaryCandidate 定位

- BoundaryCandidate 是运行时候选对象。
- BoundaryCandidate 由 RuleEngine / boundary service 生成。
- BoundaryCandidate 用于表达某个 `symbol` / `timeframe` / `direction` 下的价位边界可用性。
- BoundaryCandidate 可以是 `VALID` / `INCOMPLETE` / `INVALID` / `WATCH_ONLY`。
- BoundaryCandidate 不直接写交易所。
- BoundaryCandidate 不直接触发下单。
- BoundaryCandidate 不等同于 ExecutionPlan。
- BoundaryCandidate 可以作为后续 ExecutionPlan 结构化写入的来源。

## 三、candidate 状态字段

建议字段：

- `boundaryStatus`
  - `VALID`
  - `INCOMPLETE`
  - `INVALID`
  - `WATCH_ONLY`
- `boundaryStatusText`
- `statusReason`
- `blockingReasons`
- `missingFields`
- `invalidFields`
- `manualReviewRequired`
- `notTradeInstruction`

状态说明：

- `VALID` 仅代表可人工复核。
- `INCOMPLETE` 表示关键数据或边界不足。
- `INVALID` 表示规则冲突或边界无效。
- `WATCH_ONLY` 表示只观察，不给执行价位。

## 四、基础字段建议

建议字段：

- `candidateId`
- `symbol`
- `timeframe`
- `direction`
- `decisionBias`
- `decisionId`
- `analysisId`
- `ruleVersion`
- `generatedAt`
- `sourceType`
- `dataQualityScore`
- `confidenceLevel`
- `riskLevel`

说明：

- `candidateId` 用于审计追踪。
- `symbol` / `timeframe` 必填。
- `direction` 必须与 Decision direction 一致。
- `dataQualityScore` 低于阈值时不能 `VALID`。

## 五、entry 结构建议

entry 不应只是一个裸数字，建议结构：

- `entryType`
  - `BREAKOUT`
  - `PULLBACK`
  - `REJECTION`
  - `WATCH_ONLY`
- `entryZoneLow`
- `entryZoneHigh`
- `entryReferencePrice`
- `entrySource`
- `entryTimeframe`
- `entryReason`
- `entrySourceFields`

规则：

- entry 优先为 zone，不是单点。
- entry 必须有 `sourceFields`。
- entry 必须与 direction 一致。
- 没有 entry 时 candidate 必须 `INCOMPLETE` 或 `WATCH_ONLY`。
- AI 文本不能直接作为 `entrySource`。

## 六、stop 结构建议

建议结构：

- `stopLoss`
- `stopType`
  - `STRUCTURE_INVALIDATION`
  - `ATR_BUFFER`
  - `SWING_BREAK`
  - `INVALID`
- `stopSource`
- `stopTimeframe`
- `stopBufferValue`
- `stopReason`
- `stopSourceFields`

规则：

- stop 必须有结构失效依据。
- stop 不能只来自固定百分比。
- stop 不能只来自 AI 文本。
- 没有 stop 时不能 `VALID`。
- stop 与 entry 冲突时 `INVALID`。

## 七、TP / TP ladder 结构建议

建议结构：

- `takeProfitLevels`
  - `level`
  - `price`
  - `rr`
  - `source`
  - `reason`
- `tpSource`
- `tpTimeframe`
- `rrValue`
- `minRequiredRr`
- `tpSourceFields`

规则：

- TP 必须和 entry / stop 形成合理 RR。
- 没有 stop 不允许完整 TP ladder。
- RR 不达标时 `INCOMPLETE` 或 `INVALID`。
- TP 是人工复核计划，不是自动止盈指令。

## 八、sourceFields 结构建议

建议 `sourceFields` 包含：

- `klineWindowStart`
- `klineWindowEnd`
- `klineCount`
- `timeframe`
- `swingHighRef`
- `swingLowRef`
- `supportRef`
- `resistanceRef`
- `atrValue`
- `atrPeriod`
- `bufferRule`
- `rrRule`
- `dataSourceName`
- `sourceType`
- `dataQualityScore`
- `staleStatus`
- `evidenceRefs`
- `decisionRefs`
- `ruleVersion`

说明：

- `sourceFields` 是后续可追溯性的核心。
- 没有 `sourceFields` 的价位不能 `VALID`。
- `sourceFields` 不完整时必须 `INCOMPLETE`。

## 九、blockingReasons 设计

`blockingReasons` 建议使用固定 code + text：

- `MISSING_KLINE_WINDOW`
- `MISSING_TIMEFRAME`
- `DATA_QUALITY_LOW`
- `KLINE_STALE`
- `MISSING_SWING_STRUCTURE`
- `MISSING_ENTRY`
- `MISSING_STOP`
- `MISSING_TP`
- `MISSING_SOURCE_FIELDS`
- `RR_NOT_SATISFIED`
- `DIRECTION_BOUNDARY_CONFLICT`
- `MULTI_TIMEFRAME_CONFLICT`
- `AI_TEXT_ONLY`
- `LATEST_PRICE_ONLY`
- `STAMPEDE_RISK`
- `SHORT_WICK_ONLY`
- `LIQUIDITY_DEGRADED`
- `EVENT_WINDOW`

要求：

- `blockingReasons` 必须可展示给 dashboard / PlanReadiness。
- `blockingReasons` 不能触发自动交易。
- `blockingReasons` 用于人工复核和调试。

## 十、invalidFields 设计

建议固定值：

- `ENTRY_CONFLICT`
- `STOP_CONFLICT`
- `TP_CONFLICT`
- `RR_INVALID`
- `SOURCE_CONFLICT`
- `DIRECTION_CONFLICT`
- `STRUCTURE_ALREADY_INVALIDATED`
- `RULE_VERSION_MISMATCH`

说明：

- `invalidFields` 表示 candidate 不可用。
- `INVALID` 不等于自动反手。
- `INVALID` 不等于自动平仓。

## 十一、与 INCOMPLETE runtime gate 的关系

- BoundaryCandidate 是承载 INCOMPLETE gate 输出的 DTO。
- runtime gate 负责判断状态。
- BoundaryCandidate 承载状态、字段、原因和 `sourceFields`。
- `INCOMPLETE` candidate 不得写完整 numeric truth。
- `WATCH_ONLY` candidate 不得给完整执行计划。
- `INVALID` candidate 不得变成反向交易信号。

## 十二、与 PlanReadiness 的关系

- PlanReadiness 后续应消费 BoundaryCandidate。
- `boundaryStatus` 可映射到 `readinessStatus`。
- `blockingReasons` 可映射到 readiness `primaryReason` / `blockingReasons`。
- `sourceFields` 可映射到 PlanReadiness `sourceFields`。
- PlanReadiness 不应替代 BoundaryCandidate runtime gate。

## 十三、与 ExecutionPlan 的关系

- `VALID` candidate 后续可用于生成结构化 ExecutionPlan。
- `INCOMPLETE` / `WATCH_ONLY` 只能生成观察型文本 plan。
- `INVALID` 应生成不可用计划或不写执行价位。
- 当前 `entryZone` / `stopLoss` / `takeProfitRules` 文本字段不能作为 numeric truth。
- 后续可以考虑 `plan_boundary_json` 记录 candidate 摘要。

## 十四、与 dashboard 的关系

- dashboard 后续可以展示 `boundaryStatus`。
- dashboard 可以展示 `missingFields` / `blockingReasons`。
- dashboard 可以展示 `sourceFields` 摘要。
- dashboard 不应把 `VALID` 显示成自动交易信号。
- dashboard 不应把 `INCOMPLETE` 显示成系统错误。
- 本阶段不改 dashboard。

## 十五、与 Risk Action Guard 的关系

- 高风险不等于自动平仓。
- 高风险不等于反手。
- 插针不等于趋势反转。
- 踩踏状态禁止机会推送。
- 流动性恶化时不建议市价一次性砍仓。
- BoundaryCandidate 不生成自动交易动作。
- 不接 order API。
- `manualReviewRequired` 默认应为 `true`。
- `notTradeInstruction` 默认应为 `true`。

## 十六、DTO 分层建议

后续最小 Java 结构可以拆成：

- `BoundaryCandidateDTO`
- `BoundaryEntryDTO`
- `BoundaryStopDTO`
- `BoundaryTakeProfitLevelDTO`
- `BoundarySourceFieldsDTO`
- `BoundaryBlockingReasonDTO`

本阶段只定义方案，不创建 Java 文件。

## 十七、后续最小实现路径

1. 提交本 BoundaryCandidate DTO 方案文档。
2. 创建 BoundaryCandidate DTO implementation checklist。
3. 创建 Java DTO 最小实现。
4. 创建 RuntimeKlineContext 最小方案 / checklist。
5. 创建 BoundaryCandidateService 只读生成骨架。
6. 创建 INCOMPLETE gate 单元测试。
7. 再评估是否接 PlanReadiness / ExecutionPlan。
8. 最后再考虑 schema / dashboard。

## 十八、本阶段不做内容

- 不创建 Java DTO。
- 不实现 service。
- 不改 schema。
- 不改 dashboard。
- 不写 `plan_boundary_json`。
- 不改 ExecutionPlan。
- 不接 PlanReadiness 代码。
- 不恢复 untracked 大轨道源码。
- 不接自动交易。
- 不接 order API。
- 不做 TradeReview / Opportunity / RuleImprovement。
- 不改 Push Watchlist / Display Slots。
