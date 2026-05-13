# PHASE_PLAN_BOUNDARY_CANDIDATE_DTO_MINIMAL_EXTENSION_PLAN

## 1. Stage Position

本阶段定义 BoundaryCandidateDTO 为支持 VALID candidate 所需的最小字段扩展方案。

本阶段只创建方案文档，不修改 Java，不修改 DTO，不实现 valid factory，不实现 Builder。

该阶段承接：

- docs/PHASE_PLAN_BOUNDARY_VALID_FACTORY_BUILDER_PLAN.md
- docs/PHASE_PLAN_BOUNDARY_VALID_FACTORY_BUILDER_CHECKLIST.md
- BoundaryCandidateDTO 字段充足性只读确认结果

## 2. Current Read-Only Confirmation Result

只读确认结论：

BoundaryCandidateDTO 当前字段“部分足够”。

当前 DTO 已经可以承载基础 boundary candidate 结构，但还不足以严格支持完整 VALID candidate traceability。

尤其是：

- TP source timeframe 不足
- TP sourceFields 不足
- partial TP allocation 不足
- numeric source type / value / timeframe / reason 仍偏 String 化
- riskRewardRatio / invalidationCondition 缺少清晰统一位置

因此当前不应直接实现 valid factory / Builder。

## 3. Current Existing DTO Capability

当前已经存在的核心对象包括：

- BoundaryCandidateDTO
- BoundaryEntryDTO
- BoundaryStopDTO
- BoundaryTakeProfitLevelDTO
- BoundarySourceFieldsDTO
- BoundaryBlockingReasonDTO

BoundaryCandidateDTO 当前已具备：

- symbol
- timeframe
- boundaryStatus
- entry
- stop
- takeProfitLevels
- sourceFields
- blockingReasons
- manualReviewRequired
- notTradeInstruction
- dataQualityScore

BoundarySourceFieldsDTO 当前可承载：

- staleStatus
- klineWindowStart
- klineWindowEnd
- klineCount
- swing / support / resistance / ATR source
- rrRule
- bufferRule
- data source
- data quality
- evidenceRefs
- decisionRefs

当前缺口不是完全没有字段，而是字段语义还不够结构化、不够统一，尤其在 VALID candidate 的 numeric source traceability 上仍不够严格。

## 4. Minimal Extension Goal

本阶段的目标不是做大规模 DTO 重构。

最小扩展目标是：

- 支持 VALID candidate 的可追溯字段承载
- 保持 incomplete / watchOnly / invalid 既有 factory 不受影响
- 为后续 valid factory / Builder 提供安全字段基础
- 避免 Service 层绕过 DTO 安全默认值
- 避免生成不可追溯 entry / stop / TP 数值
- 保持 manualReviewRequired = true
- 保持 notTradeInstruction = true

## 5. Required Capability For VALID Candidate

VALID candidate 最低需要承载：

- symbol
- timeframe
- status
- entry boundary
- stop boundary
- TP ladder
- entry numeric source
- stop numeric source
- TP numeric source
- risk reward ratio
- invalidation condition
- dataQualityScore
- staleStatus
- kline window metadata
- source references
- blocking reasons
- manualReviewRequired
- notTradeInstruction

如果这些能力不能被 DTO 明确承载，后续 valid factory / Builder 不应实现。

## 6. Entry Boundary Minimal Gap

BoundaryEntryDTO 当前已能承载：

- entry zone
- reference price
- type
- source
- source timeframe
- reason
- sourceFields

Entry 侧相对足够。

建议后续只读实现前确认：

- source 是否只是 String
- 是否需要 numericSourceType
- 是否需要 numericSourceValue
- 是否需要 numericSourceReason

如果现有 source / sourceFields 已能满足最小可追溯要求，Entry 侧可以暂不扩展。

## 7. Stop Boundary Minimal Gap

BoundaryStopDTO 当前已能承载：

- stopLoss
- type
- source
- source timeframe
- buffer
- reason
- sourceFields

Stop 侧基本足够。

建议后续只读实现前确认：

- stop 是否能表达 invalidation condition
- stopReason 是否足够替代独立 invalidationCondition
- 是否需要在 candidate 顶层新增 invalidationCondition

如果 stopType / stopReason 足够表达结构失效条件，可以暂不新增顶层 invalidationCondition。

如果后续需要 dashboard 或 PlanReadiness 明确读取，则应新增独立 invalidationCondition 字段。

## 8. Take Profit Boundary Minimal Gap

BoundaryTakeProfitLevelDTO 当前可承载：

- level
- price
- rr
- source
- reason

并支持 TP ladder。

但 TP 侧存在最小缺口：

- 缺少 source timeframe
- 缺少 sourceFields
- 缺少 partial ratio / allocation
- 缺少明确 numeric source type
- 缺少明确 numeric source value

TP 是当前最需要补强的对象。

最小方案优先考虑扩展 BoundaryTakeProfitLevelDTO，而不是改动整个 BoundaryCandidateDTO 架构。

## 9. Numeric Source Representation Options

后续有两种最小方案：

### Option A: Extend Existing DTO Fields

在 Entry / Stop / TP DTO 中分别增加：

- numericSourceType
- numericSourceValue
- numericSourceTimeframe
- numericSourceReason

优点：

- 直观
- 改动较小
- 不需要新增对象

缺点：

- 字段重复
- 后续扩展成本较高

### Option B: Add BoundaryNumericSourceDTO

新增一个统一对象：

- sourceType
- sourceValue
- sourceTimeframe
- sourceReason
- sourceField
- sourceRef

然后 Entry / Stop / TP 分别引用该对象。

优点：

- 更统一
- 更可追溯
- 更适合后续 PlanReadiness / ExecutionPlan 消费

缺点：

- 多新增一个 DTO
- 测试稍多

推荐：

如果只做最小实现，Option A 可接受。

如果要为后续 entry / stop / TP 生产链路和 PlanReadiness 留接口，Option B 更稳。

当前建议倾向 Option B，但必须在 checklist 阶段再确认。

## 10. Risk Reward Ratio Field

当前 risk reward ratio 不是 BoundaryCandidateDTO 顶层字段。

已有能力：

- BoundaryTakeProfitLevelDTO.rr
- BoundarySourceFieldsDTO.rrRule

这可以表达 TP 层的 RR。

但如果后续需要 candidate 级别整体排序或 PlanReadiness 判断，可能需要新增：

- overallRiskRewardRatio
- minAcceptableRiskRewardRatio
- rrRule

最小方案建议：

- 暂不新增 top-level riskRewardRatio
- 优先复用 TP level rr 与 sourceFields.rrRule
- 如果 PlanReadiness 后续需要统一读取，再另开字段扩展方案

## 11. Invalidation Condition Field

当前 invalidation condition 没有独立字段。

已有能力：

- BoundaryStopDTO.stopType
- BoundaryStopDTO.reason
- BoundaryStopDTO.source

这可以部分表达失效条件。

最小方案建议：

- 暂不新增 top-level invalidationCondition
- 先要求 stop reason 明确结构失效条件
- 如果后续 dashboard / PlanReadiness 需要独立展示，再另开字段扩展方案

## 12. Safety Defaults

任何 DTO 扩展都必须保持：

- manualReviewRequired = true
- notTradeInstruction = true

不允许新增字段或 Builder 方法关闭这些安全默认值。

不允许新增以下字段或方法：

- autoTrade
- orderEnabled
- executionEnabled
- placeOrder
- closePosition
- reversePosition
- disableManualReview
- markAsTradeInstruction

## 13. Recommended Minimal Extension Scope

后续 checklist 阶段应优先确认以下最小范围：

### Required

- BoundaryTakeProfitLevelDTO 增强 TP traceability
- 或新增 BoundaryNumericSourceDTO
- BoundaryCandidateDTOTest 增加字段承载与安全默认测试

### Optional

- Entry / Stop 引入 numeric source DTO
- Candidate 顶层增加 invalidationCondition
- Candidate 顶层增加 overallRiskRewardRatio

### Deferred

- PlanReadiness 接入
- ExecutionPlan 接入
- RuleEngine 接入
- dashboard 展示
- schema / DB 映射
- order API
- 自动交易

## 14. Mandatory INCOMPLETE Boundary

即使 DTO 完成最小扩展，以下情况仍必须 INCOMPLETE：

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

## 15. WATCH_ONLY Boundary

即使 DTO 完成最小扩展，以下情况仍只能 WATCH_ONLY：

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

## 16. Risk Action Guard Boundary

DTO 最小扩展不得破坏 Risk Action Guard：

- 高风险不等于自动平仓
- 高风险不等于反手
- 插针不等于趋势反转
- 踩踏状态禁止机会推送
- 流动性恶化时不建议市价一次性砍仓
- VALID candidate 不等于交易动作

DTO 只能承载候选边界，不承载自动交易动作。

## 17. Explicit Non-Goals

本阶段不做：

- 不修改 Java
- 不修改 DTO
- 不实现 valid factory
- 不实现 Builder
- 不修改测试
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
- 不自动下单
- 不自动开仓
- 不自动平仓
- 不自动反手
- 不生成生产 entry / stop / TP 数值
- 不声明 PlanBoundary 生产链路完成
- 不声明 RuleEngine 完成
- 不声明自动交易接入

## 18. Future Minimal Implementation Order

后续建议顺序：

1. 创建 BoundaryCandidateDTO minimal extension checklist
2. 只读确认字段扩展范围
3. 选择 Option A 或 Option B
4. 最小修改 DTO
5. 添加 / 更新 BoundaryCandidateDTOTest
6. compile
7. test-compile
8. BoundaryCandidateDTOTest
9. 创建 verification 文档
10. 然后才回到 valid factory / Builder 实现

在 DTO minimal extension verification 前，不应实现 valid factory / Builder。

## 19. Acceptance Criteria For This Stage

本阶段验收标准：

- 目标文档存在
- 文档只定义 BoundaryCandidateDTO 最小扩展方案
- 文档明确当前字段部分足够但不够严格
- 文档明确 TP traceability 是最小缺口
- 文档明确 numeric source 表达方案
- 文档明确 safety defaults
- 文档明确 INCOMPLETE / WATCH_ONLY 边界
- 文档明确 Risk Action Guard
- 文档明确不接 order API 和自动交易
- 文档明确本阶段不修改 Java

本阶段不要求 compile。

本阶段不要求 test。

## 20. Current Conclusion

BoundaryCandidateDTO 当前字段部分足够，但不足以严格支撑完整 VALID candidate traceability。

下一步应先完成 BoundaryCandidateDTO minimal extension checklist。

在 DTO minimal extension checklist、实现与 verification 完成前，不应实现 valid factory / Builder。
