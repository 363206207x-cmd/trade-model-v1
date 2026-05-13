# PHASE_PLAN_BOUNDARY_VALID_FACTORY_BUILDER_PLAN

## 1. Stage Position

本阶段定义 BoundaryCandidateDTO valid factory / Builder 的方案边界。

当前 BoundaryCandidateService 已经具备 DTO-only 最小判断能力：

- 不完整或不可信 RuntimeKlineContext -> INCOMPLETE
- fresh context 但尚无 valid factory -> WATCH_ONLY

当前缺口是：系统还没有一个安全、可追溯、受约束的方式生成 VALID candidate。

本阶段只创建方案文档，不修改 Java，不修改 DTO，不实现 valid factory，不实现 Builder。

## 2. Why A Valid Factory / Builder Is Needed

当前 BoundaryCandidateDTO 已有：

- incomplete factory
- watchOnly factory
- invalid factory

但尚未提供 valid factory。

这意味着即使 RuntimeKlineContext 足够新鲜、数据质量足够高、OHLCV 与 klineItems 完整，BoundaryCandidateService 仍不能输出 VALID candidate，只能 fallback 到 WATCH_ONLY。

valid factory / Builder 的目标不是让系统自动交易，而是让系统在条件充分时，可以生成一个结构化、可追溯、需要人工复核的边界候选。

## 3. VALID Candidate Definition

VALID candidate 只表示：

- 当前数据满足最小完整性要求
- entry / stop / TP numeric source 可追溯
- 风险边界可解释
- 候选边界可以进入人工复核或后续 PlanReadiness 判断

VALID candidate 不表示：

- 自动开仓
- 自动下单
- 自动平仓
- 自动反手
- 交易已经执行
- order API 已接入
- RuleEngine 已全部接入
- PlanBoundary 生产链路已经完成

VALID candidate 仍必须保持：

- manualReviewRequired = true
- notTradeInstruction = true

## 4. Minimum Preconditions For VALID Candidate

后续 valid factory / Builder 至少需要满足以下前置条件：

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

如果上述任一关键条件不足，应继续输出 INCOMPLETE 或 WATCH_ONLY，而不是生成 VALID candidate。

## 5. Required Traceable Fields

VALID candidate 必须能承载或引用以下可追溯字段：

- symbol
- timeframe
- candidate status
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

如果 DTO 当前字段不足，后续不得在 Service 内临时绕过，而应先设计 DTO 最小扩展方案。

## 6. Entry Numeric Source Boundary

entry 数值不能凭空生成。

entry numeric source 未来可来自：

- swing high / swing low
- support / resistance
- breakout level
- pullback zone
- ATR-based zone
- liquidity zone
- prior structure level
- multi-timeframe confirmation level

entry numeric source 必须记录来源类型与来源字段。

如果 entry 来源不可追溯，则必须 INCOMPLETE。

## 7. Stop Numeric Source Boundary

stop 数值不能凭空生成。

stop numeric source 未来可来自：

- recent swing low / swing high
- ATR stop width
- structure invalidation level
- liquidity invalidation level
- volatility-adjusted stop
- max risk per trade rule

stop 必须与结构失效条件绑定，不能只按固定百分比硬算。

如果 stop 来源不可追溯，则必须 INCOMPLETE。

## 8. TP Numeric Source Boundary

TP 数值不能凭空生成。

TP numeric source 未来可来自：

- RR ladder
- prior high / prior low
- resistance / support level
- liquidity target
- ATR extension
- partial take-profit ladder

TP ladder 应支持分层，而不是只有单一 TP。

如果 TP 来源不可追溯，则必须 INCOMPLETE。

## 9. Builder vs Factory Decision

后续可以采用两种方案之一：

### Option A: Static Valid Factory

优点：

- 简单
- 与现有 incomplete / watchOnly / invalid factory 风格一致
- 测试容易

缺点：

- VALID candidate 字段多，参数列表可能过长
- 后续扩展不方便

### Option B: Builder

优点：

- 更适合复杂字段
- 更适合逐步构造 entry / stop / TP / sourceFields / blockingReasons
- 更适合后续扩展

缺点：

- 实现稍重
- 需要更严格测试，防止绕过安全默认值

建议：

- 如果 valid 字段不多，先用 static valid factory
- 如果需要承载完整 entry / stop / TP traceability，优先 Builder
- 不管选择哪种方式，都必须保证 manualReviewRequired 和 notTradeInstruction 默认 true

## 10. Mandatory INCOMPLETE Conditions

即使后续存在 valid factory / Builder，以下情况仍必须 INCOMPLETE：

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

## 11. WATCH_ONLY Conditions

即使后续存在 valid factory / Builder，以下情况仍只能 WATCH_ONLY：

- 数据基本可用但结构不完整
- 行情处于重大事件窗口
- 短线插针尚未确认
- 多周期方向冲突未收敛
- 风险升高但流动性状态尚未确认
- 候选边界存在但置信不足
- 价格已偏离合理 entry zone
- RR 不足但风险未达到 INVALID
- 需要人工复核

WATCH_ONLY 只表示继续观察或等待确认，不表示交易执行动作。

## 12. INVALID Conditions

后续 valid factory / Builder 不改变 INVALID 语义。

以下情况可以 INVALID：

- 候选边界已经失效
- 当前价格已远离候选结构
- stop 位置不再合理
- RR 明显不满足最低要求
- 结构被破坏
- 风险状态超过允许范围
- 数据证明原候选假设不成立

INVALID 不等于反手。

INVALID 只表示当前候选边界不可用。

## 13. Risk Action Guard Boundary

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

## 14. Explicit Non-Goals

本阶段不做：

- 不修改 BoundaryCandidateDTO
- 不实现 valid factory
- 不实现 Builder
- 不修改 BoundaryCandidateService
- 不修改 BoundaryCandidateServiceImpl
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
- 不写交易执行器
- 不生成生产 entry / stop / TP 数值
- 不声明 PlanBoundary 生产链路完成
- 不声明 RuleEngine 完成
- 不声明自动交易接入

## 15. Future Minimal Implementation Order

后续建议顺序：

1. 创建 valid factory / Builder implementation checklist
2. 只读确认 BoundaryCandidateDTO 当前字段是否足够
3. 若字段不足，先设计 DTO 最小扩展方案
4. 实现 valid factory 或 Builder
5. 添加 BoundaryCandidateDTOTest 覆盖
6. 保证 manualReviewRequired = true
7. 保证 notTradeInstruction = true
8. 更新 BoundaryCandidateServiceTest，确认 fresh context 可在满足全部 source 条件后进入 VALID
9. 暂不接 RuleEngine
10. 暂不接 PlanReadiness
11. 暂不接 ExecutionPlan
12. 单独创建 verification 文档

## 16. Acceptance Criteria For This Stage

本阶段验收标准：

- 目标文档存在
- 文档只定义 valid factory / Builder 方案
- 文档明确 VALID candidate 的含义
- 文档明确 VALID candidate 不等于交易执行
- 文档明确 entry / stop / TP numeric source 必须可追溯
- 文档明确 INCOMPLETE / WATCH_ONLY / INVALID 边界
- 文档明确 Risk Action Guard
- 文档明确不接 order API 和自动交易
- 文档明确不改 Java / schema / dashboard
- 文档明确后续实现顺序

本阶段不要求 compile。

本阶段不要求 test。

## 17. Current Conclusion

BoundaryCandidateService 最小实现已完成 DTO-only evaluation。

valid factory / Builder 仍处于方案阶段。

在 valid factory / Builder 方案、checklist、实现与 verification 完成前，BoundaryCandidateService 不应输出 VALID candidate。

在 entry / stop / TP numeric source 生产链路可追溯前，不应生成结构化交易边界。
