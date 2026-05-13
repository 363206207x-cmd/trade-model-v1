# PHASE_PLAN_BOUNDARY_CANDIDATE_SERVICE_VALID_INTEGRATION_PLAN

## 1. Stage Position

本阶段定义 BoundaryCandidateService 接入 VALID candidate 的方案边界。

当前 BoundaryCandidateDTO 已经具备 static valid factory。

当前 BoundaryCandidateService 仍是 DTO-only 最小实现：

- 不完整或不可信 RuntimeKlineContext -> INCOMPLETE
- fresh context 但未接 VALID source assembly -> WATCH_ONLY

本阶段只创建方案文档，不修改 Java，不接 RuleEngine、PlanReadiness、ExecutionPlan、schema、dashboard 或 order API。

## 2. Current Preconditions

已完成并收口：

- BoundaryCandidateDTO 最小实现
- RuntimeKlineContextDTO 最小实现
- BoundaryCandidateService 最小实现
- BoundaryCandidateDTO minimal extension
- BoundaryCandidateDTO valid factory
- BoundaryCandidateDTO valid factory verification

当前仍未完成：

- BoundaryCandidateService 接入 BoundaryCandidateDTO.valid(...)
- entry numeric source 生产链路
- stop numeric source 生产链路
- TP numeric source 生产链路
- RuleEngine 接入
- PlanReadiness 接入
- ExecutionPlan 接入
- dashboard 展示
- order API / 自动交易

## 3. VALID Integration Goal

BoundaryCandidateService 后续接入 VALID 的目标是：

- 在数据完整、来源可追溯、规则条件满足时，输出 BoundaryCandidateDTO.valid(...)
- 让 fresh context 不再永远 fallback 到 WATCH_ONLY
- 让 VALID candidate 进入后续人工复核或 PlanReadiness 判断

VALID integration 不表示：

- 自动开仓
- 自动下单
- 自动平仓
- 自动反手
- order API 已接入
- RuleEngine 已完成
- PlanBoundary 生产链路已完成

## 4. Required Inputs For VALID

BoundaryCandidateService 后续要输出 VALID，至少需要以下输入或可追溯来源：

- symbol
- timeframe
- RuntimeKlineContextDTO
- latestPrice
- dataQualityScore
- BoundaryEntryDTO
- BoundaryStopDTO
- List<BoundaryTakeProfitLevelDTO>
- BoundarySourceFieldsDTO

其中 entry / stop / TP 必须带 numeric source 或可追溯 sourceFields。

如果 entry / stop / TP source 不完整，不允许输出 VALID。

## 5. Mandatory VALID Preconditions

后续 Service 输出 VALID 前必须确认：

- RuntimeKlineContextDTO exists
- staleStatus = FRESH
- OHLCV 完整
- klineItems 完整
- kline window 足够
- dataQualityScore >= 70
- latestPrice > 0
- entry 不为空
- stop 不为空
- takeProfitLevels 非空
- entry numeric source 可追溯
- stop numeric source 可追溯
- TP numeric source 可追溯
- sourceFields 不为空
- 没有 blocking reason 阻断 VALID
- 非踩踏状态
- 短线插针已确认或已排除
- 多周期关键依赖已准备好
- 无重大事件窗口阻断

任一关键条件不足时，仍必须 INCOMPLETE 或 WATCH_ONLY。

## 6. INCOMPLETE Boundary

以下情况必须继续 INCOMPLETE：

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

## 7. WATCH_ONLY Boundary

以下情况仍只能 WATCH_ONLY：

- RuntimeKlineContext 基本可用但 entry / stop / TP source 不完整
- 行情处于重大事件窗口
- 短线插针尚未确认
- 多周期方向冲突未收敛
- 风险升高但流动性状态尚未确认
- 候选边界存在但置信不足
- 价格已偏离合理 entry zone
- RR 不足但风险未达到 INVALID
- 需要人工复核

WATCH_ONLY 只表示继续观察或等待确认，不表示交易执行动作。

## 8. Service Must Not Forge Boundaries

BoundaryCandidateService 不允许伪造：

- entry price
- stop price
- TP price
- RR
- numeric source
- source timeframe
- source reason

如果缺少可追溯来源，必须返回 INCOMPLETE 或 WATCH_ONLY。

不得为了输出 VALID 而硬编码 entry / stop / TP。

## 9. Safety Defaults

即使 Service 后续接入 VALID，返回结果仍必须保持：

- manualReviewRequired = true
- notTradeInstruction = true

Service 不允许关闭这些安全默认值。

Service 不允许返回交易执行动作。

## 10. Risk Action Guard Boundary

BoundaryCandidateService 接入 VALID 后仍必须遵守 Risk Action Guard：

- 高风险不等于自动平仓
- 高风险不等于反手
- 插针不等于趋势反转
- 踩踏状态禁止机会推送
- 流动性恶化时不建议市价一次性砍仓
- VALID candidate 不等于交易动作

高风险或踩踏状态下，不应生成机会推送或反手计划。

## 11. Explicit Non-Goals

本阶段不做：

- 不修改 Java
- 不修改 BoundaryCandidateService
- 不修改 BoundaryCandidateDTO
- 不接 RuleEngine
- 不接 PlanReadiness
- 不接 ExecutionPlan
- 不改 schema
- 不改 dashboard
- 不改 config
- 不接 mapper
- 不接 controller
- 不接 DB
- 不接 Push workflow
- 不接 order API
- 不自动下单
- 不自动开仓
- 不自动平仓
- 不自动反手
- 不生成生产 entry / stop / TP 数值
- 不声明 PlanBoundary 生产链路完成
- 不声明 RuleEngine 完成

## 12. Future Minimal Implementation Order

后续建议顺序：

1. 创建 BoundaryCandidateService VALID integration checklist
2. 只读确认 Service 当前方法签名是否需要扩展
3. 确认 entry / stop / TP source 输入从哪里来
4. 若缺 source assembler，先创建 source assembler 方案文档
5. 最小实现 Service 输出 VALID
6. 更新 BoundaryCandidateServiceTest
7. compile
8. test-compile
9. BoundaryCandidateServiceTest
10. 创建 verification 文档

在 source assembler 明确前，不应直接让 Service 生成真实 entry / stop / TP 数值。

## 13. Acceptance Criteria For This Stage

本阶段验收标准：

- 目标文档存在
- 文档只定义 BoundaryCandidateService VALID integration 方案
- 文档明确 VALID 前置条件
- 文档明确 INCOMPLETE / WATCH_ONLY 边界
- 文档明确 Service 不允许伪造 entry / stop / TP
- 文档明确 safety defaults
- 文档明确 Risk Action Guard
- 文档明确不接 RuleEngine / PlanReadiness / ExecutionPlan
- 文档明确不接 order API 和自动交易
- 文档明确本阶段不修改 Java

本阶段不要求 compile。

本阶段不要求 test。

## 14. Current Conclusion

BoundaryCandidateDTO 已具备 valid factory。

BoundaryCandidateService 尚未接入 VALID 输出。

下一步应先提交本文档，然后创建 BoundaryCandidateService VALID integration checklist。

在 checklist 完成前，不应修改 BoundaryCandidateService。
