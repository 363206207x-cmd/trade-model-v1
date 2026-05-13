# PHASE_PLAN_BOUNDARY_VALID_FACTORY_BUILDER_CHECKLIST

## 1. Stage Position

本阶段定义 BoundaryCandidate valid factory / Builder 最小实现前的 checklist。

本阶段只创建 checklist 文档，不实现 Java，不修改 BoundaryCandidateDTO，不接 RuleEngine、PlanReadiness、ExecutionPlan 或 order API。

该 checklist 基于已收口方案文档：

- docs/PHASE_PLAN_BOUNDARY_VALID_FACTORY_BUILDER_PLAN.md

## 2. Current Preconditions

进入后续 valid factory / Builder 最小实现前，必须确认以下前置阶段已经收口：

- BoundaryCandidateDTO 最小实现已收口
- BoundaryCandidateDTO verification 已收口
- RuntimeKlineContextDTO 最小实现已收口
- RuntimeKlineContextDTO verification 已收口
- BoundaryCandidateService Java 最小实现已收口
- BoundaryCandidateService verification 已收口
- valid factory / Builder 方案文档已收口

当前仍未完成：

- valid factory / Builder Java 实现
- BoundaryCandidateDTO 字段充足性只读确认
- entry / stop / TP numeric source 生产链路
- VALID candidate 输出能力
- INCOMPLETE runtime gate 生产接入
- PlanReadiness 接入
- ExecutionPlan 接入
- RuleEngine 接入
- dashboard 展示

## 3. Mandatory Read-Only Check Before Implementation

后续进入 Java 实现前，必须先只读确认 BoundaryCandidateDTO 当前字段是否足够承载 VALID candidate。

必须确认字段或对象是否能承载：

- symbol
- timeframe
- status
- entry type
- entry price / entry zone
- entry numeric source
- stop type
- stop price / stop zone
- stop numeric source
- TP ladder
- TP numeric source
- risk reward ratio
- invalidation condition
- dataQualityScore
- staleStatus
- kline window metadata
- sourceFields
- blockingReasons
- manualReviewRequired
- notTradeInstruction

如果字段不足，不允许直接实现 valid factory / Builder。

必须先创建 DTO 最小扩展方案文档。

## 4. Allowed Future Implementation Scope

后续最小实现阶段只允许围绕 BoundaryCandidateDTO 做最小改动。

可能允许的文件范围，需在只读确认后最终确定：

- src/main/java/org/example/trademodel/dto/planboundary/BoundaryCandidateDTO.java
- src/test/java/org/example/trademodel/dto/planboundary/BoundaryCandidateDTOTest.java

如果需要新增 Builder 类，必须先单独论证是否比 static valid factory 更合适。

不允许把 Service、RuleEngine、PlanReadiness、ExecutionPlan、schema 或 dashboard 混入本阶段。

## 5. Static Factory vs Builder Decision Checklist

后续只读确认后，需要在以下两种方案中选择其一：

### Option A: Static Valid Factory

适用条件：

- BoundaryCandidateDTO 当前字段已经足够
- VALID candidate 参数数量可控
- 不需要复杂分步构造
- 测试可以清晰覆盖安全默认值

风险：

- 参数过长
- 后续扩展困难

### Option B: Builder

适用条件：

- VALID candidate 字段较多
- 需要承载 entry / stop / TP / sourceFields / blockingReasons
- 需要更清晰的可读性
- 后续扩展概率较高

风险：

- 实现更重
- 必须防止绕过 manualReviewRequired / notTradeInstruction
- 必须防止生成交易执行语义

建议：

- 字段不足时，不要强行做 static factory
- 字段较多时优先 Builder
- 不管选择哪种，安全默认值必须不可被关闭

## 6. VALID Candidate Preconditions Checklist

后续 valid factory / Builder 必须要求以下前置条件：

- RuntimeKlineContextDTO exists
- staleStatus = FRESH
- OHLCV 完整
- klineItems 完整
- kline window 足够
- dataQualityScore >= 70
- latestPrice 存在且 > 0
- entry numeric source 可追溯
- stop numeric source 可追溯
- TP numeric source 可追溯
- RR rule 可计算
- 风险等级未进入极端压力锁定
- 不处于踩踏状态
- 短线插针已确认或已排除
- 多周期关键依赖已准备好
- 没有重大事件窗口阻断

任一关键条件不足时，不得生成 VALID candidate。

## 7. Required Traceability Checklist

VALID candidate 必须包含或引用以下可追溯内容：

- entry numeric source type
- entry numeric source value
- entry source timeframe
- entry source reason
- stop numeric source type
- stop numeric source value
- stop source timeframe
- stop source reason
- TP numeric source type
- TP numeric source value
- TP ladder reason
- RR calculation input
- invalidation condition
- dataQualityScore source
- RuntimeKlineContext reference
- blockingReasons

如果这些字段不能承载，必须 INCOMPLETE 或 WATCH_ONLY。

## 8. Safety Defaults Checklist

valid factory / Builder 必须保证：

- manualReviewRequired = true
- notTradeInstruction = true

不允许通过参数传入 false 来关闭安全默认值。

不允许通过 Builder 方法暴露以下能力：

- markAsTradeInstruction
- disableManualReview
- enableAutoTrade
- enableOrder
- enableExecution

## 9. Mandatory INCOMPLETE Checklist

即使存在 valid factory / Builder，以下情况仍必须 INCOMPLETE：

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

## 10. WATCH_ONLY Checklist

即使存在 valid factory / Builder，以下情况仍只能 WATCH_ONLY：

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

## 11. INVALID Checklist

后续实现不改变 INVALID 语义。

以下情况可以 INVALID：

- 候选边界已经失效
- 当前价格已远离候选结构
- stop 位置不再合理
- RR 明显不满足最低要求
- 结构被破坏
- 风险状态超过允许范围
- 数据证明原候选假设不成立

INVALID 不等于反手。

## 12. Risk Action Guard Checklist

valid factory / Builder 必须继续遵守 Risk Action Guard：

- 高风险不等于自动平仓
- 高风险不等于反手
- 插针不等于趋势反转
- 踩踏状态禁止机会推送
- 流动性恶化时不建议市价一次性砍仓
- 高风险但流动性正常时，只能进入减仓、移动止损、降杠杆等人工复核方向
- 高风险且流动性恶化时，优先分批降风险、等待流动性恢复、只降杠杆
- 高风险且存在踩踏时，进入极端压力锁定，不生成机会推送，不生成反手计划
- 高风险但只是短线插针时，不直接判定趋势反转

VALID candidate 不等于交易动作。

## 13. Forbidden Implementation Items

后续 valid factory / Builder 实现阶段禁止：

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
- 不写交易执行器
- 不声明 entry / stop / TP 生产链路已经落地
- 不声明 PlanBoundary 生产链路已经完成
- 不声明 RuleEngine 已经完成
- 不声明自动交易已经接入

## 14. Unit Test Checklist

后续实现必须添加或更新单元测试，至少覆盖：

- valid factory / Builder 创建 VALID candidate
- manualReviewRequired remains true
- notTradeInstruction remains true
- entry numeric source required
- stop numeric source required
- TP numeric source required
- missing entry source cannot create VALID
- missing stop source cannot create VALID
- missing TP source cannot create VALID
- no trade instruction field can be enabled
- no order / execute / close / reverse methods are introduced
- incomplete / watchOnly / invalid factories remain unchanged

如果 DTO 字段不足导致上述测试不可写，必须先回到 DTO 最小扩展方案。

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

## 16. Verification After Future Implementation

后续最小实现完成后，必须执行：

- compile
- test-compile
- BoundaryCandidateDTOTest
- 如更新 service 行为，还需执行 BoundaryCandidateServiceTest

如果环境限制导致测试无法执行，必须如实记录，不能写 PASS。

实现后必须创建 verification 文档，记录：

- 实现文件
- 字段变化
- factory / Builder 选择原因
- 测试结果
- 安全默认值
- Risk Action Guard
- 未接模块
- 后续建议

## 17. Explicit Non-Goals For This Checklist Stage

本阶段不做：

- 不修改 Java
- 不修改 BoundaryCandidateDTO
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
- 不自动交易
- 不生成生产 entry / stop / TP 数值
- 不声明生产链路完成

## 18. Acceptance Criteria For This Checklist Stage

本阶段验收标准：

- 目标 checklist 文档存在
- 文档只定义 valid factory / Builder 后续实现清单
- 文档明确 DTO 字段充足性需要先只读确认
- 文档明确字段不足时必须先写 DTO 最小扩展方案
- 文档明确 VALID candidate 前置条件
- 文档明确 entry / stop / TP numeric source 可追溯
- 文档明确 safety defaults
- 文档明确 INCOMPLETE / WATCH_ONLY / INVALID 边界
- 文档明确 Risk Action Guard
- 文档明确禁止 order API 和自动交易
- 文档明确本阶段不实现 Java

本阶段不要求 compile。

本阶段不要求 test。

## 19. Current Conclusion

valid factory / Builder 仍未实现。

本阶段只完成 implementation checklist。

下一步应在 checklist staging 前复核通过后，再精准 stage 并 commit 本文档。

在 checklist 提交前，不应直接修改 BoundaryCandidateDTO。
