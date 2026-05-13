# PHASE_PLAN_BOUNDARY_CANDIDATE_SERVICE_VERIFICATION

## 1. Verification Object

本文件记录 BoundaryCandidateService Java 最小实现的 verification 结果。

对应提交：

- f901145 feat(plan): add boundary candidate service

本阶段只验证 BoundaryCandidateService 的 DTO-only 最小服务层实现。

## 2. Implemented Files

本阶段提交文件仅包括：

- src/main/java/org/example/trademodel/service/planboundary/BoundaryCandidateService.java
- src/main/java/org/example/trademodel/service/planboundary/BoundaryCandidateServiceImpl.java
- src/test/java/org/example/trademodel/service/planboundary/BoundaryCandidateServiceTest.java

未修改既有 Java 文件。

未修改 schema。

未修改 dashboard。

未修改 config。

## 3. Service Method Signature

BoundaryCandidateService 暴露的最小方法为：

```java
BoundaryCandidateDTO evaluateBoundaryCandidate(
        String symbol,
        String timeframe,
        RuntimeKlineContextDTO runtimeKlineContext,
        BigDecimal latestPrice,
        BigDecimal dataQualityScore
);
```

方法名不包含 execute / trade / order / place / close / reverse 等交易执行语义。

## 4. Implemented Behavior

BoundaryCandidateServiceImpl 只做 DTO 层判断。

已实现的最小判断包括：

- null RuntimeKlineContextDTO -> INCOMPLETE
- missing / UNKNOWN / STALE -> INCOMPLETE
- latestPrice null / <= 0 -> INCOMPLETE
- dataQualityScore null / < 70 -> INCOMPLETE
- OHLCV 缺失 -> INCOMPLETE
- klineItems 缺失 -> INCOMPLETE
- fresh context 且无 valid factory -> WATCH_ONLY

当前不输出 VALID candidate。

当前不生成 entry / stop / TP 数值。

## 5. Safety Defaults

所有返回结果仍依赖 BoundaryCandidateDTO 的安全默认值：

- manualReviewRequired = true
- notTradeInstruction = true

本阶段未绕过 DTO factory。

本阶段未通过反射设置交易字段。

## 6. Verification Results

本阶段已完成以下验证：

- compile PASS
- test-compile PASS
- BoundaryCandidateServiceTest PASS

验证范围仅覆盖 BoundaryCandidateService 最小 DTO-only 实现。

## 7. Test Coverage

BoundaryCandidateServiceTest 覆盖：

- null RuntimeKlineContextDTO -> INCOMPLETE
- missing RuntimeKlineContextDTO -> INCOMPLETE
- stale RuntimeKlineContextDTO -> INCOMPLETE
- UNKNOWN status -> INCOMPLETE
- latestPrice null -> INCOMPLETE
- latestPrice <= 0 -> INCOMPLETE
- dataQualityScore null -> INCOMPLETE
- dataQualityScore < 70 -> INCOMPLETE
- OHLCV 缺失 -> INCOMPLETE
- klineItems 缺失 -> INCOMPLETE
- complete fresh context -> WATCH_ONLY
- manualReviewRequired remains true
- notTradeInstruction remains true
- service interface does not expose order / trade / execute / close / reverse method names

## 8. Explicit Boundaries

本阶段没有补 valid factory。

本阶段没有补 Builder。

本阶段没有接 mapper。

本阶段没有接 controller。

本阶段没有接 DB。

本阶段没有接 RuleEngine。

本阶段没有接 PlanReadiness。

本阶段没有接 ExecutionPlan。

本阶段没有接 Push workflow。

本阶段没有改 schema。

本阶段没有改 dashboard。

本阶段没有改 config。

本阶段没有接 order API。

本阶段没有自动交易。

## 9. Risk Action Guard Confirmation

BoundaryCandidateService 只输出 BoundaryCandidateDTO 状态结果。

高风险不等于自动平仓。

高风险不等于反手。

插针不等于趋势反转。

踩踏状态禁止机会推送。

流动性恶化时不建议市价一次性砍仓。

本阶段不输出自动交易动作。

## 10. Risk Grep Result

实现后复核中 risk grep 无输出。

未发现以下误导性内容：

- 自动交易承诺
- 立即下单
- 一键下单
- 一键平仓
- 自动交易已接入
- order API 已接入
- entry / stop / TP 已落地口径
- VALID candidate 已完成口径
- valid factory 已完成口径
- RuleEngine 已完成口径
- PlanBoundary 已完成口径

## 11. Current Workspace Status

提交 f901145 后确认：

- staged 为空
- tracked diff 为空
- src/main/java clean
- src/test/java clean
- src/main/resources clean
- docs 其它 untracked 文件仍保留，未处理

## 12. Current Conclusion

BoundaryCandidateService Java minimal implementation completed and verified.

当前实现只是 DTO-only 最小服务层判断。

它不是完整 RuleEngine。

它不是完整 PlanBoundary。

它不是交易执行链路。

## 13. Recommended Next Step

下一步建议创建 BoundaryCandidateService verification 文档提交前复核。

后续如继续实现，应先创建 valid factory / Builder 方案，或创建 BoundaryCandidateService 与 PlanReadiness 的只读映射方案。

不要直接接 schema / dashboard。

不要直接接 order API。

不要恢复 untracked 大轨道源码。
