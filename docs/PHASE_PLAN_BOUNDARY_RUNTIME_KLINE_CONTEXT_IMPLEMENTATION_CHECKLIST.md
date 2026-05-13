# PHASE_PLAN_BOUNDARY_RUNTIME_KLINE_CONTEXT_IMPLEMENTATION_CHECKLIST

## 一、implementation 总原则

- 本阶段只规划 RuntimeKlineContext DTO 最小实现。
- 下一阶段才创建 Java DTO。
- RuntimeKlineContext 只是运行时行情上下文。
- 它不是交易信号。
- 它不是执行计划。
- 它不是 AI 输出。
- 不接 BoundaryCandidateService。
- 不接 PlanReadiness。
- 不接 ExecutionPlan。
- 不接 RuleEngine。
- 不改 schema。
- 不改 dashboard。
- 不接自动交易 / order API。

## 二、允许的后续 Java 文件范围

建议 package：

- `src/main/java/org/example/trademodel/dto/planboundary/`

后续可创建：

- `RuntimeKlineContextDTO.java`
- `RuntimeKlineItemDTO.java`
- `RuntimeKlineContextStatusEnum.java`

测试文件：

- `src/test/java/org/example/trademodel/dto/planboundary/RuntimeKlineContextDTOTest.java`

## 三、严格禁止文件范围

后续实现不得修改：

- `schema.sql`
- `application.yml`
- `dashboard.html`
- mapper
- service
- controller
- `PlanServiceImpl`
- `PlanReadinessServiceImpl`
- `ExecutionPlanMapper`
- `RuleEngineService`
- `BoundaryCandidateService`
- Push workflow
- TradeReview / Opportunity / RuleImprovement
- order API / apiKey / secret

## 四、RuntimeKlineContextDTO 字段 checklist

建议字段：

- `symbol`
- `timeframe`
- `klineWindowStart`
- `klineWindowEnd`
- `klineCount`
- `latestOpen`
- `latestHigh`
- `latestLow`
- `latestClose`
- `latestVolume`
- `previousClose`
- `highestHigh`
- `lowestLow`
- `averageVolume`
- `dataSourceName`
- `sourceType`
- `dataQualityScore`
- `staleStatus`
- `fetchTime`
- `generatedAt`
- `missingFields`
- `blockingReasons`
- `ruleVersion`
- `klineItems`

## 五、RuntimeKlineItemDTO 字段 checklist

建议字段：

- `openTime`
- `closeTime`
- `open`
- `high`
- `low`
- `close`
- `volume`
- `sourceType`

字段类型建议：

- 价格 / 成交量字段使用 `BigDecimal`。
- 时间字段使用 `LocalDateTime`。
- `klineCount` / `dataQualityScore` 使用 `Integer`。
- `missingFields` / `blockingReasons` 使用 `List<String>`。
- `klineItems` 使用 `List<RuntimeKlineItemDTO>`。

## 六、RuntimeKlineContextStatusEnum checklist

建议 enum：

- `FRESH`
- `STALE`
- `UNKNOWN`

说明：

- FRESH 不代表可交易。
- STALE 不能生成 VALID BoundaryCandidate。
- UNKNOWN 应降级为 INCOMPLETE 或 WATCH_ONLY。

## 七、factory / helper 方法建议

后续可以考虑静态方法：

- `missing(symbol, timeframe, reason)`
- `stale(symbol, timeframe, reason)`
- `fresh(symbol, timeframe)`

要求：

- missing / stale 应加入 `blockingReasons`。
- missing / stale 不应被误解为交易信号。
- 本阶段只规划，不实现 Java。

## 八、校验规则 checklist

后续测试应验证：

- 缺少 timeframe 可表达 `missingFields` / `blockingReasons`。
- stale 状态可表达。
- `dataQualityScore` 低可表达。
- `latestPrice` / `latestClose` 单点不能代表完整 window。
- `klineItems` 可承载 OHLCV 列表。
- DTO 不包含 order API / apiKey / secret / 自动交易字段。

## 九、与 BoundaryCandidate 的关系

- RuntimeKlineContext 是 BoundaryCandidate 的输入。
- `BoundaryCandidate.sourceFields` 后续应引用 RuntimeKlineContext 的 `timeframe` / `klineWindowStart` / `klineWindowEnd` / `sourceType` / `dataQualityScore`。
- RuntimeKlineContext 不负责生成 entry / stop / TP。
- 没有 RuntimeKlineContext 时 BoundaryCandidate 必须 INCOMPLETE 或 WATCH_ONLY。

## 十、与 INCOMPLETE gate 的关系

- missing context -> INCOMPLETE。
- stale context -> INCOMPLETE 或 WATCH_ONLY。
- low `dataQualityScore` -> INCOMPLETE。
- source unreliable -> INCOMPLETE。
- RuntimeKlineContext 不直接决定 VALID，只提供输入。

## 十一、安全默认边界

- DTO 不包含 `orderId`。
- DTO 不包含 exchange account。
- DTO 不包含 apiKey / secret。
- DTO 不包含自动下单字段。
- DTO 不包含自动平仓字段。
- DTO 不包含自动反手字段。
- RuntimeKlineContext 不生成交易动作。
- 不接 order API。

## 十二、测试 checklist

建议后续最小测试：

- DTO 可构造。
- enum 值完整。
- OHLCV 字段可承载 `BigDecimal`。
- `missingFields` / `blockingReasons` 可承载。
- `klineItems` 可承载。
- `staleStatus` 可表达 FRESH / STALE / UNKNOWN。
- 反射确认 DTO 不包含 order API / apiKey / secret / 自动交易字段。

## 十三、后续实现顺序

建议：

1. 提交本 checklist。
2. 创建 RuntimeKlineContextStatusEnum。
3. 创建 RuntimeKlineItemDTO。
4. 创建 RuntimeKlineContextDTO。
5. 创建 RuntimeKlineContextDTOTest。
6. compile / test。
7. 提交 RuntimeKlineContext DTO 最小实现。
8. 再创建 BoundaryCandidateService 只读生成方案。

## 十四、本阶段不做内容

- 不创建 Java 文件。
- 不实现 service。
- 不改 schema。
- 不改 dashboard。
- 不改行情拉取逻辑。
- 不接 PlanReadiness。
- 不接 ExecutionPlan。
- 不接 RuleEngine。
- 不接 BoundaryCandidateService。
- 不恢复 untracked 大轨道源码。
- 不接自动交易 / order API。
