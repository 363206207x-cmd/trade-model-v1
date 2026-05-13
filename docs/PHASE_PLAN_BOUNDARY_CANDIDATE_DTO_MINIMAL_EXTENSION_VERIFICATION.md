# PHASE_PLAN_BOUNDARY_CANDIDATE_DTO_MINIMAL_EXTENSION_VERIFICATION

## 1. Verification Object

本文件记录 BoundaryCandidateDTO minimal extension Java 最小实现的 verification 结果。

对应提交：

- e884209 feat(plan): extend boundary candidate DTO traceability

本阶段只验证 DTO minimal extension：

- 新增 BoundaryNumericSourceDTO
- 补强 BoundaryTakeProfitLevelDTO 的 TP traceability
- 更新 BoundaryCandidateDTOTest

## 2. Implemented Files

本阶段提交文件仅包括：

- src/main/java/org/example/trademodel/dto/planboundary/BoundaryNumericSourceDTO.java
- src/main/java/org/example/trademodel/dto/planboundary/BoundaryTakeProfitLevelDTO.java
- src/test/java/org/example/trademodel/dto/planboundary/BoundaryCandidateDTOTest.java

未修改：

- BoundaryCandidateDTO
- BoundaryEntryDTO
- BoundaryStopDTO
- BoundarySourceFieldsDTO
- BoundaryBlockingReasonDTO
- BoundaryCandidateService
- BoundaryCandidateServiceImpl
- schema
- dashboard
- config

## 3. BoundaryNumericSourceDTO Verification

BoundaryNumericSourceDTO 已新增，字段包括：

- sourceType
- sourceValue
- sourceTimeframe
- sourceReason
- sourceField
- sourceRef

sourceValue 使用 BigDecimal。

BoundaryNumericSourceDTO 不包含交易执行字段或方法。

未包含：

- orderId
- orderSide
- executionId
- autoTrade
- placeOrder
- closePosition
- reversePosition

## 4. BoundaryTakeProfitLevelDTO Verification

BoundaryTakeProfitLevelDTO 保留既有字段：

- level
- price
- rr
- source
- reason

本阶段新增字段：

- numericSource
- sourceTimeframe
- sourceRef
- partialRatio
- allocationRatio

这些字段只用于 TP traceability 与候选边界解释。

TP ladder 不等于自动止盈。

partialRatio / allocationRatio 不等于自动下单。

## 5. Test Coverage

BoundaryCandidateDTOTest 已覆盖：

- BoundaryNumericSourceDTO 字段承载
- BoundaryNumericSourceDTO 不暴露执行类字段 / 方法
- BoundaryTakeProfitLevelDTO 可承载 numericSource
- BoundaryTakeProfitLevelDTO 可承载 sourceTimeframe
- BoundaryTakeProfitLevelDTO 可承载 sourceRef
- BoundaryTakeProfitLevelDTO 可承载 partialRatio
- BoundaryTakeProfitLevelDTO 可承载 allocationRatio
- incomplete factory remains safe
- watchOnly factory remains safe
- invalid factory remains safe
- manualReviewRequired remains true
- notTradeInstruction remains true

## 6. Verification Commands

本阶段已执行：

```bash
./mvnw -q -DskipTests compile
./mvnw -q -DskipTests test-compile
./mvnw -q -Dtest=BoundaryCandidateDTOTest test
```

验证结果：

- compile PASS
- test-compile PASS
- BoundaryCandidateDTOTest PASS

执行过程中仅出现 JDK / Maven 运行时 warning，不影响测试结果。

## 7. Risk Grep Result

本阶段实现后执行 risk grep，未发现以下正向风险表述或实现痕迹：

- executeOrder
- placeOrder
- closePosition
- reversePosition
- autoTrade
- orderId
- orderSide
- executionId
- 自动下单
- 自动开仓
- 自动平仓
- 自动反手
- order API
- entry 已落地
- stop 已落地
- TP 已落地
- PlanBoundary 已完成
- RuleEngine 已完成

## 8. Boundary Confirmation

本阶段未实现：

- valid factory
- Builder
- BoundaryCandidateService 接入
- RuleEngine 接入
- PlanReadiness 接入
- ExecutionPlan 接入
- Push workflow 接入
- order API 接入
- 自动交易
- entry / stop / TP 生产数值生成

本阶段未修改：

- schema
- dashboard
- config
- mapper
- controller
- DB

## 9. Safety Defaults

本阶段未修改 BoundaryCandidateDTO 既有 factory。

既有测试继续确认：

- incomplete factory remains safe
- watchOnly factory remains safe
- invalid factory remains safe
- manualReviewRequired remains true
- notTradeInstruction remains true

新增 TP traceability 字段不改变交易语义。

BoundaryNumericSourceDTO 只承载 numeric source traceability，不承载交易执行动作。

## 10. Risk Action Guard Confirmation

BoundaryCandidateDTO minimal extension 继续遵守 Risk Action Guard：

- 高风险不等于自动平仓
- 高风险不等于反手
- 插针不等于趋势反转
- 踩踏状态禁止机会推送
- 流动性恶化时不建议市价一次性砍仓
- VALID candidate 不等于交易动作

本阶段仅增强 DTO traceability，不输出自动交易动作，不改变执行边界。

## 11. Relationship To Previous Plans

本阶段承接：

- BoundaryCandidateDTO minimal extension 方案文档
- BoundaryCandidateDTO minimal extension checklist 文档
- BoundaryCandidateDTO 字段充足性只读确认

本阶段只完成 DTO traceability 最小扩展。

valid factory / Builder 仍未实现。

## 12. Current Workspace State

提交后确认：

- staged 为空
- tracked diff 为空
- src/main/java clean
- src/test/java clean
- src/main/resources clean
- docs 其它 untracked 文件仍保留且未处理

## 13. Current Conclusion

BoundaryCandidateDTO minimal extension Java 最小实现已完成并验证。

当前已具备：

- BoundaryNumericSourceDTO
- BoundaryTakeProfitLevelDTO TP traceability 字段
- 对应单元测试覆盖

但当前仍不是 VALID candidate 完整实现。

本阶段不代表 RuleEngine、PlanBoundary 生产链路或自动交易能力完成。

## 14. Next Step Recommendation

下一步建议创建 valid factory / Builder Java 实现前只读范围确认。

在 valid factory / Builder 实现前，仍需确认：

- BoundaryCandidateDTO 是否继续不改
- valid factory 是否采用 static factory 或 Builder
- VALID candidate 是否能强制要求 traceable entry / stop / TP numeric source
- manualReviewRequired 与 notTradeInstruction 是否不可关闭

继续禁止：

- 直接接 RuleEngine
- 直接接 PlanReadiness
- 直接接 ExecutionPlan
- 直接改 schema / dashboard
- 直接接 order API / 自动交易
