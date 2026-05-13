# PlanBoundary RuntimeKlineContext DTO Verification

## 一、验证对象

- `3ce98c5 docs(plan): define runtime kline context plan`
- `b7d51f4 docs(plan): add runtime kline context checklist`
- `2c9c912 feat(plan): add runtime kline context DTOs`

## 二、实现文件清单

- `src/main/java/org/example/trademodel/dto/planboundary/RuntimeKlineContextDTO.java`
- `src/main/java/org/example/trademodel/dto/planboundary/RuntimeKlineItemDTO.java`
- `src/main/java/org/example/trademodel/dto/planboundary/RuntimeKlineContextStatusEnum.java`
- `src/test/java/org/example/trademodel/dto/planboundary/RuntimeKlineContextDTOTest.java`

## 三、已完成能力

- 新增 RuntimeKlineContext DTO 最小结构。
- `RuntimeKlineContextDTO` 可承载 `symbol` / `timeframe` / kline window。
- 可承载 `latestOpen` / `latestHigh` / `latestLow` / `latestClose` / `latestVolume`。
- 可承载 `previousClose` / `highestHigh` / `lowestLow` / `averageVolume`。
- 可承载 `dataSourceName` / `sourceType` / `dataQualityScore`。
- 可承载 `staleStatus` / `fetchTime` / `generatedAt`。
- 可承载 `missingFields` / `blockingReasons`。
- 可承载 `klineItems`。
- `RuntimeKlineItemDTO` 可承载单根 K 线 OHLCV。
- `RuntimeKlineContextStatusEnum` 包含 `FRESH` / `STALE` / `UNKNOWN`。
- 已实现 `missing` / `stale` / `fresh` factory。
- 未推导 `entry` / `stop` / `TP`。
- 未加入交易语义。

## 四、测试结果

- compile PASS
- test-compile PASS
- `RuntimeKlineContextDTOTest` PASS

## 五、测试覆盖

`RuntimeKlineContextDTOTest` 已覆盖：

- `RuntimeKlineContextStatusEnum` values 完整。
- `missing` factory 状态为 `UNKNOWN`。
- `missing` factory 包含 `blockingReasons`。
- `stale` factory 状态为 `STALE`。
- `stale` factory 包含 `blockingReasons`。
- `fresh` factory 状态为 `FRESH`。
- DTO 可承载 OHLCV / `klineItems` / `missingFields` / `blockingReasons`。
- 反射确认不包含禁止交易字段。

## 六、安全字段验证

已确认未发现：

- `orderId`
- `apiKey`
- `secret`
- `exchangeAccount`
- `autoOrder`
- `autoOpen`
- `autoClose`
- `autoReverse`
- 自动下单字段
- 自动平仓字段
- 自动反手字段

## 七、边界确认

- 本阶段只做 DTO / enum / test。
- 不接 service。
- 不接 mapper。
- 不接 controller。
- 不接 `BoundaryCandidateService`。
- 不接 `PlanReadiness`。
- 不接 `ExecutionPlan`。
- 不接 `RuleEngine`。
- 不改 schema。
- 不改 dashboard。
- 不改 config。
- 不改 Push workflow。
- 不接 order API。
- 不自动交易。
- 不推导 `entry` / `stop` / `TP`。

## 八、与前置方案关系

- 承接 RuntimeKlineContext plan。
- 承接 RuntimeKlineContext checklist。
- 承接 BoundaryCandidate DTO 阶段。
- 承接 INCOMPLETE runtime gate plan。
- 但本阶段不实现 numeric source。
- 不实现 runtime gate。
- 不实现 `BoundaryCandidateService`。

## 九、当前工作区状态

- tracked clean。
- staged 为空。
- `src/main/java` / `src/test/java` / `src/main/resources` 下无 untracked。
- docs untracked 仍保留。

## 十、当前结论

RuntimeKlineContext Java DTO minimal implementation completed and verified。

但它只是行情上下文承载层，不是 RuleEngine / PlanBoundary 完整实现。

## 十一、后续建议

- 下一步不要直接接 service。
- 建议先创建 `BoundaryCandidateService` 只读生成方案。
- 或创建 RuntimeKlineContext 与 BoundaryCandidate `sourceFields` 映射方案。
- 不要直接改 schema / dashboard。
- 不要恢复 untracked 大轨道源码。
- 不要接自动交易 / order API。
