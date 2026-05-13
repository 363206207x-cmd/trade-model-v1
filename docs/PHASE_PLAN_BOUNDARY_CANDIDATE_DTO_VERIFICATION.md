# PHASE_PLAN_BOUNDARY_CANDIDATE_DTO_VERIFICATION

## 一、验证对象

- `c6ab731 docs(plan): add boundary candidate DTO checklist`
- `95882ee feat(plan): add boundary candidate DTOs`

## 二、实现文件清单

本阶段实现范围仅包含以下 10 个文件：

- `src/main/java/org/example/trademodel/dto/planboundary/BoundaryCandidateDTO.java`
- `src/main/java/org/example/trademodel/dto/planboundary/BoundaryEntryDTO.java`
- `src/main/java/org/example/trademodel/dto/planboundary/BoundaryStopDTO.java`
- `src/main/java/org/example/trademodel/dto/planboundary/BoundaryTakeProfitLevelDTO.java`
- `src/main/java/org/example/trademodel/dto/planboundary/BoundarySourceFieldsDTO.java`
- `src/main/java/org/example/trademodel/dto/planboundary/BoundaryBlockingReasonDTO.java`
- `src/main/java/org/example/trademodel/dto/planboundary/BoundaryStatusEnum.java`
- `src/main/java/org/example/trademodel/dto/planboundary/BoundaryEntryTypeEnum.java`
- `src/main/java/org/example/trademodel/dto/planboundary/BoundaryStopTypeEnum.java`
- `src/test/java/org/example/trademodel/dto/planboundary/BoundaryCandidateDTOTest.java`

## 三、已完成能力

- 新增 `planboundary` DTO package。
- `BoundaryCandidateDTO` 可承载 `boundaryStatus` / `statusReason` / `blockingReasons` / `missingFields` / `invalidFields`。
- `BoundaryCandidateDTO` 可承载 `entry` / `stop` / `takeProfitLevels` / `sourceFields`。
- `BoundaryEntryDTO` 支持 entry zone。
- `BoundaryStopDTO` 支持 stop loss 和 stop buffer。
- `BoundaryTakeProfitLevelDTO` 支持 TP ladder。
- `BoundarySourceFieldsDTO` 支持可追溯来源字段。
- `BoundaryBlockingReasonDTO` 支持 `code` / `text` / `field` / `severity`。
- `BoundaryStatusEnum` 包含 `VALID` / `INCOMPLETE` / `INVALID` / `WATCH_ONLY`。
- `manualReviewRequired` 默认 `true`。
- `notTradeInstruction` 默认 `true`。
- 已实现 `incomplete` / `watchOnly` / `invalid` factory。
- 未实现 `valid` factory，符合最小边界。

## 四、测试结果

- `compile` PASS。
- `test-compile` PASS。
- `BoundaryCandidateDTOTest` PASS。

## 五、测试覆盖

`BoundaryCandidateDTOTest` 已覆盖：

- enum values 完整。
- `incomplete` factory 状态正确。
- `watchOnly` factory 状态正确。
- `invalid` factory 状态正确。
- `manualReviewRequired` 默认 `true`。
- `notTradeInstruction` 默认 `true`。
- DTO 可承载 entry / stop / TP / sourceFields。
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
- 不接 PlanReadiness。
- 不接 ExecutionPlan。
- 不接 RuleEngine。
- 不改 schema。
- 不改 dashboard。
- 不改 config。
- 不改 Push workflow。
- 不接 order API。
- 不自动交易。

## 八、与前置方案关系

- 承接 BoundaryCandidate DTO plan。
- 承接 INCOMPLETE runtime gate plan。
- 承接 entry / stop / TP numeric source plan。
- 本阶段不实现 numeric source。
- 本阶段不实现 runtime gate。
- 本阶段不实现 candidate service。

## 九、当前工作区状态

- tracked clean。
- staged 为空。
- `src/main/java` / `src/test/java` / `src/main/resources` 下无 untracked。
- docs untracked 仍保留，未处理。

## 十、当前结论

BoundaryCandidate Java DTO minimal implementation completed and verified.

该实现只是结构承载层，不是 RuleEngine / PlanBoundary 完整实现。

## 十一、后续建议

- 下一步不要直接接 service。
- 建议先创建 RuntimeKlineContext 最小实现方案或 checklist。
- 或创建 BoundaryCandidateService 只读生成方案。
- 不要直接改 schema / dashboard。
- 不要恢复 untracked 大轨道源码。
- 不要接自动交易 / order API。
