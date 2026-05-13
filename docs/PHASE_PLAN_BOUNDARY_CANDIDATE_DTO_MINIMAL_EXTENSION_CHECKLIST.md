# PHASE_PLAN_BOUNDARY_CANDIDATE_DTO_MINIMAL_EXTENSION_CHECKLIST

## 1. Stage Position

本阶段定义 BoundaryCandidateDTO minimal extension 的 implementation checklist。

本阶段只创建 checklist 文档，不修改 Java，不修改 DTO，不新增 BoundaryNumericSourceDTO，不实现 valid factory，不实现 Builder。

该 checklist 基于已收口方案文档：

- docs/PHASE_PLAN_BOUNDARY_CANDIDATE_DTO_MINIMAL_EXTENSION_PLAN.md

## 2. Current Preconditions

进入后续 DTO minimal extension 实现前，必须确认以下前置阶段已经收口：

- BoundaryCandidateDTO 最小实现已收口
- BoundaryCandidateDTO verification 已收口
- RuntimeKlineContextDTO 最小实现已收口
- RuntimeKlineContextDTO verification 已收口
- BoundaryCandidateService Java 最小实现已收口
- BoundaryCandidateService verification 已收口
- valid factory / Builder 方案文档已收口
- valid factory / Builder checklist 已收口
- BoundaryCandidateDTO 字段充足性只读确认已完成
- BoundaryCandidateDTO minimal extension 方案文档已收口

当前仍未完成：

- BoundaryCandidateDTO minimal extension Java 实现
- BoundaryNumericSourceDTO
- TP traceability 字段补强
- valid factory / Builder Java 实现
- VALID candidate 输出能力
- entry / stop / TP numeric source 生产链路
- INCOMPLETE runtime gate 生产接入
- PlanReadiness 接入
- ExecutionPlan 接入
- RuleEngine 接入
- dashboard 展示

## 3. Mandatory Read-Only Check Before Implementation

后续进入 Java 实现前，必须再次只读确认真实 DTO 类名、字段和测试文件。

必须确认以下真实类：

- BoundaryCandidateDTO
- BoundaryEntryDTO
- BoundaryStopDTO
- BoundaryTakeProfitLevelDTO
- BoundarySourceFieldsDTO
- BoundaryBlockingReasonDTO
- BoundaryCandidateDTOTest

不得使用错误类名：

- EntryBoundaryDTO
- StopBoundaryDTO
- TakeProfitBoundaryDTO

如果真实字段与本文档不一致，必须以源码为准，并在实现前输出差异说明。

## 4. Minimal Extension Decision

当前方案建议倾向 Option B：

- 新增 BoundaryNumericSourceDTO
- Entry / Stop / TP 可引用统一 numeric source
- 优先补强 TP traceability

但 checklist 阶段仍要求实现前再做最终确认。

### Option A: Extend Existing DTO Fields

可选字段：

- numericSourceType
- numericSourceValue
- numericSourceTimeframe
- numericSourceReason

适用条件：

- 不希望新增 DTO
- 字段数量极少
- 后续 PlanReadiness 暂不消费统一 numeric source

缺点：

- Entry / Stop / TP 字段重复
- 后续扩展成本较高

### Option B: Add BoundaryNumericSourceDTO

推荐字段：

- sourceType
- sourceValue
- sourceTimeframe
- sourceReason
- sourceField
- sourceRef

适用条件：

- 需要统一表达 entry / stop / TP numeric source
- 需要后续 PlanReadiness / ExecutionPlan 可追溯消费
- 需要降低 String source 语义不清的问题

当前倾向：

- Option B 更稳
- 但实现前必须再次只读确认 DTO 当前字段与测试结构

## 5. Allowed Future Implementation Scope

如果后续确认采用 Option B，最小允许文件范围应优先限制为：

- src/main/java/org/example/trademodel/dto/planboundary/BoundaryNumericSourceDTO.java
- src/main/java/org/example/trademodel/dto/planboundary/BoundaryTakeProfitLevelDTO.java
- src/test/java/org/example/trademodel/dto/planboundary/BoundaryCandidateDTOTest.java

如确需 Entry / Stop 也引用 BoundaryNumericSourceDTO，必须在实现前说明原因，并尽量保持最小范围。

可能涉及：

- src/main/java/org/example/trademodel/dto/planboundary/BoundaryEntryDTO.java
- src/main/java/org/example/trademodel/dto/planboundary/BoundaryStopDTO.java

不允许混入 Service、RuleEngine、PlanReadiness、ExecutionPlan、schema 或 dashboard。

## 6. Forbidden Files For Next Implementation

下一阶段仍不允许修改：

- schema.sql
- application.yml
- dashboard.html
- controller
- mapper
- entity
- service
- BoundaryCandidateService
- BoundaryCandidateServiceImpl
- PlanReadiness
- ExecutionPlan
- RuleEngine
- Push workflow
- order API
- 自动交易相关代码

如确需修改上述文件，必须另开方案文档。

## 7. Required BoundaryNumericSourceDTO Checklist

如果采用 Option B，BoundaryNumericSourceDTO 至少应承载：

- sourceType
- sourceValue
- sourceTimeframe
- sourceReason
- sourceField
- sourceRef

字段语义：

- sourceType：如 swing_high、swing_low、support、resistance、atr、rr_ladder、liquidity、structure_level
- sourceValue：实际数值来源
- sourceTimeframe：来源周期
- sourceReason：为什么采用该来源
- sourceField：对应原始字段名或规则字段
- sourceRef：可选证据引用或决策引用

BoundaryNumericSourceDTO 不应包含交易执行字段。

不得包含：

- orderId
- orderSide
- executionId
- autoTrade
- placeOrder
- closePosition
- reversePosition

## 8. Take Profit Traceability Checklist

TP 是当前最小缺口。

后续最小实现必须优先补强 BoundaryTakeProfitLevelDTO：

- numericSource 或等价字段
- sourceTimeframe
- sourceFields 或 sourceRef
- partialRatio / allocationRatio

TP level 必须继续支持：

- level
- price
- rr
- source
- reason

TP ladder 不等于自动止盈。

TP allocation 不等于自动下单。

TP traceability 只用于解释候选边界，不用于自动执行。

## 9. Entry / Stop Extension Checklist

Entry 当前相对足够。

Stop 当前基本足够。

后续实现前必须确认是否需要 Entry / Stop 引入 BoundaryNumericSourceDTO。

最小建议：

- 如果只做 TP traceability，可以暂不改 Entry / Stop
- 如果为了统一 numeric source 表达，可以让 Entry / Stop 也引用 BoundaryNumericSourceDTO
- 不允许为了统一而大规模重构 DTO

Entry / Stop 扩展不得改变既有 incomplete / watchOnly / invalid factory 行为。

## 10. Risk Reward Ratio Checklist

当前暂不优先新增 top-level riskRewardRatio。

现有可复用：

- BoundaryTakeProfitLevelDTO.rr
- BoundarySourceFieldsDTO.rrRule

后续如果 PlanReadiness 需要 candidate 级别统一 RR，再另开字段扩展方案。

本阶段不新增：

- overallRiskRewardRatio
- minAcceptableRiskRewardRatio

## 11. Invalidation Condition Checklist

当前暂不优先新增 top-level invalidationCondition。

现有可复用：

- BoundaryStopDTO.stopType
- BoundaryStopDTO.reason
- BoundaryStopDTO.source

后续如果 dashboard / PlanReadiness 需要独立展示，再另开字段扩展方案。

本阶段不新增：

- invalidationCondition

## 12. Safety Defaults Checklist

任何 DTO minimal extension 都必须保持：

- manualReviewRequired = true
- notTradeInstruction = true

不允许新增字段或方法关闭这些安全默认值。

不允许新增：

- disableManualReview
- markAsTradeInstruction
- enableExecution
- enableOrder
- enableAutoTrade

## 13. Factory Compatibility Checklist

后续实现必须保证既有 factory 行为不被破坏：

- incomplete factory 仍可用
- watchOnly factory 仍可用
- invalid factory 仍可用
- manualReviewRequired 默认仍为 true
- notTradeInstruction 默认仍为 true

本阶段不实现：

- valid factory
- Builder

valid factory / Builder 必须等 DTO minimal extension verification 后再做。

## 14. Unit Test Checklist

后续实现必须更新或新增测试，至少覆盖：

- BoundaryNumericSourceDTO 可承载 sourceType
- BoundaryNumericSourceDTO 可承载 sourceValue
- BoundaryNumericSourceDTO 可承载 sourceTimeframe
- BoundaryNumericSourceDTO 可承载 sourceReason
- BoundaryNumericSourceDTO 不包含 order / execution 字段
- BoundaryTakeProfitLevelDTO 可承载 numeric source
- BoundaryTakeProfitLevelDTO 可承载 sourceTimeframe
- BoundaryTakeProfitLevelDTO 可承载 partialRatio / allocationRatio
- incomplete factory remains safe
- watchOnly factory remains safe
- invalid factory remains safe
- manualReviewRequired remains true
- notTradeInstruction remains true

如果实现范围没有新增 BoundaryNumericSourceDTO，则测试必须覆盖 Option A 字段。

## 15. Forbidden Test Assertions

测试中不允许出现：

- 自动下单成功
- 自动开仓成功
- 自动平仓成功
- 自动反手成功
- order API called
- entry / stop / TP 生产链路已经落地
- RuleEngine 已经完成
- PlanBoundary 已经完成
- production migration has been executed

## 16. Mandatory INCOMPLETE Boundary

DTO minimal extension 不改变 INCOMPLETE 语义。

以下情况仍必须 INCOMPLETE：

- RuntimeKlineContextDTO missing
- staleStatus = STALE
- staleStatus = UNKNOWN 且无补充确认
- OHLCV 缺失
- klineItems 缺失
- kline window 不足
- dataQualityScore 低于阈值
- latestPrice 缺失或非法
- entry numeric source 不可追溯
- stop numeric source 不可追溯
- TP numeric source 不可追溯
- RR 无法计算
- liquidity source 缺失
- 多周期关键依赖未准备好
- 数据质量不足以支持结构化边界

INCOMPLETE 不等于看多、看空、开仓、平仓或反手。

## 17. WATCH_ONLY Boundary

DTO minimal extension 不改变 WATCH_ONLY 语义。

以下情况仍只能 WATCH_ONLY：

- 数据基本可用但结构不完整
- 行情处于重大事件窗口
- 短线插针尚未确认
- 多周期方向冲突未收敛
- 风险升高但流动性状态尚未确认
- 候选边界存在但置信不足
- 价格已偏离合理 entry zone
- RR 不足但风险未达到 INVALID
- 需要人工复核

WATCH_ONLY 不表示交易执行动作。

## 18. Risk Action Guard Checklist

DTO minimal extension 必须继续遵守 Risk Action Guard：

- 高风险不等于自动平仓
- 高风险不等于反手
- 插针不等于趋势反转
- 踩踏状态禁止机会推送
- 流动性恶化时不建议市价一次性砍仓
- VALID candidate 不等于交易动作

DTO 只能承载候选边界，不承载自动交易动作。

## 19. Verification After Future Implementation

后续最小实现完成后，必须执行：

- compile
- test-compile
- BoundaryCandidateDTOTest

如果新增 BoundaryNumericSourceDTO 对应单测，也必须执行相关测试。

如果环境限制导致测试无法执行，必须如实记录，不能写 PASS。

实现后必须创建 verification 文档，记录：

- 实现文件
- 字段变化
- Option A / Option B 最终选择
- 测试结果
- safety defaults
- Risk Action Guard
- 未接模块
- 后续建议

## 20. Explicit Non-Goals For This Checklist Stage

本阶段不做：

- 不修改 Java
- 不修改 DTO
- 不新增 BoundaryNumericSourceDTO
- 不修改测试
- 不实现 valid factory
- 不实现 Builder
- 不接 mapper
- 不接 controller
- 不接 DB
- 不接 RuleEngine
- 不接 PlanReadiness
- 不接 ExecutionPlan
- 不改 schema
- 不改 dashboard
- 不改 config
- 不接 Push workflow
- 不接 order API
- 不自动交易
- 不生成生产 entry / stop / TP 数值
- 不声明生产链路完成

## 21. Acceptance Criteria For This Checklist Stage

本阶段验收标准：

- 目标 checklist 文档存在
- 文档只定义 BoundaryCandidateDTO minimal extension 后续实现清单
- 文档明确后续实现前必须再次只读确认真实类名和字段
- 文档明确 Option A / Option B
- 文档明确当前倾向 Option B
- 文档明确 TP traceability 是最小缺口
- 文档明确暂不优先新增 top-level riskRewardRatio / invalidationCondition
- 文档明确 safety defaults
- 文档明确 factory compatibility
- 文档明确 INCOMPLETE / WATCH_ONLY 边界
- 文档明确 Risk Action Guard
- 文档明确禁止 order API 和自动交易
- 文档明确本阶段不实现 Java

本阶段不要求 compile。

本阶段不要求 test。

## 22. Current Conclusion

BoundaryCandidateDTO minimal extension 仍未实现。

本阶段只完成 implementation checklist。

下一步应在 checklist staging 前复核通过后，再精准 stage 并 commit 本文档。

在 checklist 提交前，不应修改 BoundaryCandidateDTO 或新增 BoundaryNumericSourceDTO。
