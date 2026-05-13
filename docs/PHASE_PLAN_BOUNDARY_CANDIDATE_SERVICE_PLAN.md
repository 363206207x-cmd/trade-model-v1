# PHASE_PLAN_BOUNDARY_CANDIDATE_SERVICE_PLAN

## 1. Stage Position

本阶段定义 BoundaryCandidateService 的最小方案边界。

BoundaryCandidateService 未来用于把 RuntimeKlineContextDTO、数据质量结果、当前价格与可追溯的规则来源，转换为 BoundaryCandidateDTO。

本阶段只创建方案文档，不实现 Java 代码，不接 service、mapper、controller、schema、dashboard、RuleEngine、PlanReadiness、ExecutionPlan 或 order API。

## 2. Current Upstream Status

已完成并收口的前置阶段：

- BoundaryCandidate DTO 最小实现
- BoundaryCandidate DTO verification
- RuntimeKlineContext DTO 最小实现
- RuntimeKlineContext DTO verification

当前主线仍未完成：

- BoundaryCandidateService 实现
- PlanReadiness 接入
- ExecutionPlan 接入
- RuleEngine 接入
- entry / stop / TP numeric source 生产链路
- INCOMPLETE runtime gate 代码落地

## 3. Future Service Input

未来 BoundaryCandidateService 的输入应至少包括：

- symbol
- timeframe
- RuntimeKlineContextDTO
- latest price / current price
- dataQualityScore
- staleStatus
- missingFields
- blockingReasons

可选依赖包括：

- ATR source
- swing high / swing low source
- support / resistance source
- liquidity source
- RR rule
- stop width rule
- TP ladder rule
- multi-timeframe confirmation result

这些可选依赖必须可追溯。缺失时不能伪造 entry / stop / TP 数值。

## 4. Future Service Output

未来 BoundaryCandidateService 只能输出 BoundaryCandidateDTO 类型结果。

允许输出方向包括：

- INCOMPLETE
- WATCH_ONLY
- INVALID
- future VALID candidate

当前 BoundaryCandidateDTO 阶段尚未提供 valid factory。

如果后续需要输出 VALID candidate，必须先单独补充 valid factory 或 Builder 方案，并经过 checklist 与 verification 后再进入实现。

不允许在 Service 内绕过 DTO 安全默认值，也不允许通过反射或临时 setter 伪造可交易结果。

## 5. Core Responsibilities

BoundaryCandidateService 未来只负责：

- 检查 RuntimeKlineContextDTO 是否存在
- 检查 OHLCV 是否完整
- 检查 kline window 是否足够
- 检查 staleStatus 是否可接受
- 检查 dataQualityScore 是否达到阈值
- 检查 latest price 是否存在
- 检查 entry / stop / TP numeric source 是否可追溯
- 判断是否必须 INCOMPLETE
- 生成 WATCH_ONLY / INCOMPLETE / INVALID / future VALID candidate
- 记录 blockingReasons
- 保持 manualReviewRequired = true
- 保持 notTradeInstruction = true

BoundaryCandidateService 不是交易执行器，不负责下单，不负责平仓，不负责反手。

## 6. Mandatory INCOMPLETE Conditions

以下情况必须输出 INCOMPLETE：

- RuntimeKlineContextDTO missing
- staleStatus = STALE 且无法确认行情有效
- staleStatus = UNKNOWN 且缺少补充确认
- OHLCV 缺失
- kline window 不足
- dataQualityScore 低于阈值
- latest price 缺失
- ATR source 缺失
- swing high / swing low source 缺失
- liquidity source 缺失
- 多周期关键依赖未准备好
- entry numeric source 不可追溯
- stop numeric source 不可追溯
- TP numeric source 不可追溯

INCOMPLETE 的含义是：当前信息不足以生成结构化交易边界。

INCOMPLETE 不等于看多、看空、开仓、平仓或反手。

## 7. WATCH_ONLY Conditions

以下情况只能 WATCH_ONLY：

- 数据基本可用但结构不完整
- 行情处于重大事件窗口
- 短线插针尚未确认
- 多周期方向冲突未收敛
- 风险升高但流动性状态尚未确认
- 需要人工复核
- 候选边界存在但置信不足
- 价格已偏离合理 entry zone
- RR 不足但风险未达到 INVALID

WATCH_ONLY 的含义是：可以继续观察或等待确认，但不能生成交易执行动作。

## 8. INVALID Conditions

以下情况可以输出 INVALID：

- 候选边界已经失效
- 当前价格已远离候选结构
- stop 位置不再合理
- RR 明显不满足最低要求
- 结构被破坏
- 风险状态超过允许范围
- 数据证明原候选假设不成立

INVALID 不等于反手。

INVALID 只表示当前候选边界不可用。

## 9. Risk Action Guard Boundary

BoundaryCandidateService 必须遵守 Risk Action Guard：

- 高风险不等于自动平仓
- 高风险不等于反手
- 插针不等于趋势反转
- 踩踏状态禁止机会推送
- 流动性恶化时不建议市价一次性砍仓
- 高风险但流动性正常时，只能进入减仓、移动止损、降杠杆等人工复核方向
- 高风险且流动性恶化时，优先分批降风险、等待流动性恢复、只降杠杆
- 高风险且存在踩踏时，进入极端压力锁定，不生成机会推送，不生成反手计划
- 高风险但只是短线插针时，不直接判定趋势反转

BoundaryCandidateService 只能输出边界候选、INCOMPLETE、WATCH_ONLY 或 INVALID。

BoundaryCandidateService 不输出自动交易动作。

## 10. Explicit Non-Goals

本阶段不做：

- 不实现 Java service
- 不创建 BoundaryCandidateService 接口
- 不创建 BoundaryCandidateServiceImpl
- 不创建 controller
- 不创建 mapper
- 不改 schema
- 不改 dashboard
- 不改 config
- 不接 RuleEngine
- 不接 PlanReadiness
- 不接 ExecutionPlan
- 不接 Push workflow
- 不接 order API
- 不自动下单
- 不自动开仓
- 不自动平仓
- 不自动反手
- 不写交易执行器
- 不写生产迁移
- 不声明 RuleEngine 已经完成
- 不声明 PlanBoundary 已经完成
- 不声明 BoundaryCandidateService 已经完成
- 不声明 entry / stop / TP 已经落地
- 不声明生产链路完成

## 11. Future Minimal Implementation Order

后续最小实现建议：

1. 创建 BoundaryCandidateService 接口
2. 创建 BoundaryCandidateServiceImpl
3. 只接 DTO 输入输出
4. 不接 DB
5. 不接 controller
6. 不接 dashboard
7. 添加单元测试
8. 优先覆盖 INCOMPLETE
9. 再覆盖 WATCH_ONLY
10. 再覆盖 INVALID
11. VALID candidate 必须等待 valid factory / Builder 方案确认
12. 单独完成 implementation checklist
13. 实现后单独创建 verification 文档

## 12. Acceptance Criteria For This Stage

本阶段验收标准：

- 目标文档存在
- 文档只定义 BoundaryCandidateService 方案
- 文档明确输入、输出、职责、INCOMPLETE、WATCH_ONLY、INVALID
- 文档明确 Risk Action Guard
- 文档明确禁止自动交易和 order API
- 文档明确不改 schema / dashboard
- 文档明确不接 RuleEngine / PlanReadiness / ExecutionPlan
- 文档明确当前不实现 Java 代码
- 文档明确后续最小实现顺序

本阶段不要求 compile。

本阶段不要求 test。

## 13. Current Conclusion

BoundaryCandidateService 仍处于方案阶段。

当前项目已经具备 BoundaryCandidateDTO 与 RuntimeKlineContextDTO 的前置承载对象，但尚未进入服务层实现。

下一步应先创建 implementation checklist，再进入最小 Java service 实现。

在 checklist 完成前，不应直接实现 BoundaryCandidateService。
