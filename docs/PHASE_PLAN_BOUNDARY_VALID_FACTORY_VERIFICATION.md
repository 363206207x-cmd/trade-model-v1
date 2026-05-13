# PHASE_PLAN_BOUNDARY_VALID_FACTORY_VERIFICATION

## 1. Verification Object

本文件记录 BoundaryCandidateDTO valid factory Java 最小实现的 verification 结果。

对应提交：

- 38bae5d feat(plan): add valid boundary candidate factory

本阶段只验证 BoundaryCandidateDTO static valid factory。

本阶段不验证 Builder。

本阶段不接 service、RuleEngine、PlanReadiness、ExecutionPlan、schema、dashboard 或 order API。

## 2. Implemented Files

本阶段提交文件仅包括：

- src/main/java/org/example/trademodel/dto/planboundary/BoundaryCandidateDTO.java
- src/test/java/org/example/trademodel/dto/planboundary/BoundaryCandidateDTOTest.java

未修改：

- BoundaryEntryDTO
- BoundaryStopDTO
- BoundaryTakeProfitLevelDTO
- BoundaryNumericSourceDTO
- BoundarySourceFieldsDTO
- BoundaryBlockingReasonDTO
- BoundaryCandidateService
- BoundaryCandidateServiceImpl
- schema
- dashboard
- config

## 3. Valid Factory Signature

新增 static valid factory：

```java
BoundaryCandidateDTO valid(
        String symbol,
        String timeframe,
        BoundaryEntryDTO entry,
        BoundaryStopDTO stop,
        List<BoundaryTakeProfitLevelDTO> takeProfitLevels,
        BoundarySourceFieldsDTO sourceFields,
        BigDecimal dataQualityScore
)
```

该 factory 只生成 BoundaryCandidateDTO，不生成交易执行动作。

## 4. Validation Rules

valid factory 已校验：

- symbol 非空白
- timeframe 非空白
- entry 非 null
- stop 非 null
- takeProfitLevels 非 null 且非空
- sourceFields 非 null
- dataQualityScore 非 null

缺少关键字段时抛出 IllegalArgumentException。

## 5. Valid Candidate Output

valid factory 生成结果确认：

- boundaryStatus = VALID
- symbol 正确写入
- timeframe 正确写入
- entry 正确写入
- stop 正确写入
- takeProfitLevels 正确写入
- sourceFields 正确写入
- dataQualityScore 正确写入
- blockingReasons 保持安全默认空列表

VALID candidate 只表示边界候选结构完整。

VALID candidate 不表示交易执行。

## 6. Safety Defaults

valid factory 显式保持：

- manualReviewRequired = true
- notTradeInstruction = true

factory 不提供关闭 manualReviewRequired 的参数。

factory 不提供关闭 notTradeInstruction 的参数。

## 7. Defensive Copy

takeProfitLevels 使用 defensive copy：

```java
new ArrayList<>(takeProfitLevels)
```

外部传入的 takeProfitLevels 后续被修改时，不影响 candidate 内部列表。

## 8. Test Coverage

BoundaryCandidateDTOTest 已覆盖：

- valid factory 可创建 VALID candidate
- boundaryStatus = VALID
- symbol 正确写入
- timeframe 正确写入
- entry 正确写入
- stop 正确写入
- takeProfitLevels 正确写入
- sourceFields 正确写入
- dataQualityScore 正确写入
- manualReviewRequired remains true
- notTradeInstruction remains true
- takeProfitLevels defensive copy
- symbol blank -> IllegalArgumentException
- timeframe blank -> IllegalArgumentException
- entry null -> IllegalArgumentException
- stop null -> IllegalArgumentException
- takeProfitLevels null -> IllegalArgumentException
- takeProfitLevels empty -> IllegalArgumentException
- sourceFields null -> IllegalArgumentException
- dataQualityScore null -> IllegalArgumentException
- factory methods do not expose trading action names
- incomplete factory remains safe
- watchOnly factory remains safe
- invalid factory remains safe

## 9. Verification Commands

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

## 10. Risk Grep Result

本阶段实现后执行 risk grep，未发现风险输出。

未发现以下实现痕迹：

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

## 11. Explicit Non-Goals Confirmed

本阶段未实现：

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

- BoundaryEntryDTO
- BoundaryStopDTO
- BoundaryTakeProfitLevelDTO
- BoundaryNumericSourceDTO
- BoundarySourceFieldsDTO
- BoundaryBlockingReasonDTO
- schema
- dashboard
- config
- mapper
- controller
- DB

## 12. Risk Action Guard Confirmation

BoundaryCandidateDTO valid factory 继续遵守 Risk Action Guard：

- 高风险不等于自动平仓
- 高风险不等于反手
- 插针不等于趋势反转
- 踩踏状态禁止机会推送
- 流动性恶化时不建议市价一次性砍仓
- VALID candidate 不等于交易动作

本阶段只提供 DTO valid factory，不输出自动交易动作，不改变执行边界。

## 13. Current Project State After Commit

提交后确认：

- staged 为空
- tracked diff 为空
- src/main/java clean
- src/test/java clean
- src/main/resources clean
- docs 其它 untracked 文件仍保留且未处理

## 14. Current Conclusion

BoundaryCandidateDTO valid factory Java 最小实现已完成并验证。

当前已具备：

- static valid factory
- required fields validation
- safety defaults
- takeProfitLevels defensive copy
- corresponding unit test coverage

但当前仍不是 RuleEngine、PlanReadiness、ExecutionPlan、schema、dashboard 或自动交易能力。

## 15. Recommended Next Step

下一步建议创建 BoundaryCandidateDTO valid factory verification 文档 staging 前复核。

后续仍应继续禁止：

- 直接接 BoundaryCandidateService
- 直接接 RuleEngine
- 直接接 PlanReadiness
- 直接接 ExecutionPlan
- 直接改 schema / dashboard
- 直接接 order API / 自动交易
