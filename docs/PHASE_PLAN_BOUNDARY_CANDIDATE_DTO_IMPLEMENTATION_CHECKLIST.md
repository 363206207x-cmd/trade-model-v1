# PlanBoundary BoundaryCandidate DTO Implementation Checklist

## 一、implementation 总原则

- 本阶段只规划 BoundaryCandidate DTO 最小实现。
- 下一阶段才创建 Java DTO。
- BoundaryCandidate 只承载结构化候选边界，不是订单，不是交易指令。
- BoundaryCandidate 不接 service / mapper / controller。
- 本阶段不改 schema。
- 本阶段不改 dashboard。
- 本阶段不接自动交易。
- 本阶段不接 order API。

## 二、后续允许创建的 Java 文件范围

建议 package：

`src/main/java/org/example/trademodel/dto/planboundary/`

后续可创建：

- `BoundaryCandidateDTO.java`
- `BoundaryEntryDTO.java`
- `BoundaryStopDTO.java`
- `BoundaryTakeProfitLevelDTO.java`
- `BoundarySourceFieldsDTO.java`
- `BoundaryBlockingReasonDTO.java`
- `BoundaryStatusEnum.java`
- `BoundaryEntryTypeEnum.java`
- `BoundaryStopTypeEnum.java`

可选测试：

- `src/test/java/org/example/trademodel/dto/planboundary/BoundaryCandidateDTOTest.java`

## 三、后续禁止修改文件

后续 DTO 最小实现阶段不得修改：

- `schema.sql`
- `application.yml`
- `dashboard.html`
- mapper
- service
- controller
- `PlanServiceImpl`
- `PlanReadinessServiceImpl`
- `ExecutionPlanMapper`
- Push workflow
- TradeReview / Opportunity / RuleImprovement
- order API / apiKey / secret

## 四、BoundaryCandidateDTO 字段 checklist

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
- `boundaryStatus`
- `boundaryStatusText`
- `statusReason`
- `blockingReasons`
- `missingFields`
- `invalidFields`
- `manualReviewRequired`
- `notTradeInstruction`
- `entry`
- `stop`
- `takeProfitLevels`
- `sourceFields`

## 五、BoundaryEntryDTO 字段 checklist

建议字段：

- `entryType`
- `entryZoneLow`
- `entryZoneHigh`
- `entryReferencePrice`
- `entrySource`
- `entryTimeframe`
- `entryReason`
- `entrySourceFields`

要求：

- 价格字段后续用 `BigDecimal`。
- entry 优先是 zone，不是单点。
- entry 必须有 `sourceFields` 才可能 `VALID`。
- AI 文本不能直接作为 `entrySource`。

## 六、BoundaryStopDTO 字段 checklist

建议字段：

- `stopLoss`
- `stopType`
- `stopSource`
- `stopTimeframe`
- `stopBufferValue`
- `stopReason`
- `stopSourceFields`

要求：

- stop 必须有结构失效依据。
- stop 不能只来自固定百分比。
- stop 不能只来自 AI 文本。
- 没有 stop 不能 `VALID`。

## 七、BoundaryTakeProfitLevelDTO 字段 checklist

建议字段：

- `level`
- `price`
- `rr`
- `source`
- `reason`

要求：

- 支持多个 TP level。
- `price` / `rr` 后续用 `BigDecimal`。
- TP 不是自动止盈指令。

## 八、BoundarySourceFieldsDTO 字段 checklist

建议字段：

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

## 九、BoundaryBlockingReasonDTO 字段 checklist

建议字段：

- `code`
- `text`
- `field`
- `severity`

## 十、enum checklist

`BoundaryStatusEnum`：

- `VALID`
- `INCOMPLETE`
- `INVALID`
- `WATCH_ONLY`

`BoundaryEntryTypeEnum`：

- `BREAKOUT`
- `PULLBACK`
- `REJECTION`
- `WATCH_ONLY`

`BoundaryStopTypeEnum`：

- `STRUCTURE_INVALIDATION`
- `ATR_BUFFER`
- `SWING_BREAK`
- `INVALID`

## 十一、安全默认值

- `manualReviewRequired` 默认 `true`。
- `notTradeInstruction` 默认 `true`。
- DTO 不包含 `orderId`。
- DTO 不包含 exchange account。
- DTO 不包含 apiKey / secret。
- DTO 不包含自动下单字段。
- DTO 不包含自动平仓 / 自动反手字段。
- `VALID` 也只代表可人工复核，不代表自动交易。

## 十二、factory / helper 方法建议

后续可以考虑静态方法：

- `incomplete(symbol, timeframe, reason)`
- `watchOnly(symbol, timeframe, reason)`
- `invalid(symbol, timeframe, reason)`
- `valid(...)`

本阶段只规划，不实现。

## 十三、校验规则 checklist

后续测试应验证：

- `VALID` 必须有 entry / stop / `sourceFields`。
- `INCOMPLETE` 可以缺 entry / stop / TP。
- `WATCH_ONLY` 不应带完整执行价位。
- `INVALID` 不等于自动反手。
- `manualReviewRequired` 默认 `true`。
- `notTradeInstruction` 默认 `true`。
- DTO 不包含 order API / apiKey / secret 字段。

## 十四、测试 checklist

后续最小测试：

- DTO 可构造。
- enum 值完整。
- default flags 正确。
- incomplete candidate 可表达 `blockingReasons`。
- valid candidate 可表达 entry / stop / TP / `sourceFields`。
- DTO 不包含 order API / apiKey / secret 字段。

## 十五、后续实现顺序

1. 提交本 checklist。
2. 创建 enum。
3. 创建子 DTO。
4. 创建 BoundaryCandidateDTO。
5. 创建 DTO 测试。
6. compile / test。
7. 提交 Java DTO 最小实现。
8. 再创建 RuntimeKlineContext checklist。

## 十六、本阶段不做内容

- 不创建 Java 文件。
- 不改 schema。
- 不改 dashboard。
- 不改 service / mapper / controller。
- 不接 PlanReadiness。
- 不接 ExecutionPlan。
- 不接 RuleEngine。
- 不恢复 untracked 大轨道源码。
- 不接自动交易 / order API。
