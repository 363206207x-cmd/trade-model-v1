# PHASE_PLAN_BOUNDARY_CANDIDATE_SERVICE_IMPLEMENTATION_CHECKLIST

## 1. Stage Position

本阶段定义 BoundaryCandidateService 最小实现前的 implementation checklist。

本阶段只创建 checklist 文档，不实现 Java 代码，不修改 schema，不修改 dashboard，不接 RuleEngine、PlanReadiness、ExecutionPlan 或 order API。

该 checklist 基于已收口方案文档：

- docs/PHASE_PLAN_BOUNDARY_CANDIDATE_SERVICE_PLAN.md

## 2. Current Preconditions

进入后续最小实现前，必须确认以下前置阶段已经收口：

- BoundaryCandidateDTO 已存在
- BoundaryCandidateDTO verification 已收口
- RuntimeKlineContextDTO 已存在
- RuntimeKlineContextDTO verification 已收口
- BoundaryCandidateService 方案文档已收口

当前仍未完成：

- BoundaryCandidateService Java 实现
- BoundaryCandidateServiceImpl Java 实现
- BoundaryCandidateService 单元测试
- valid factory / Builder
- PlanReadiness 接入
- ExecutionPlan 接入
- RuleEngine 接入
- entry / stop / TP numeric source 生产链路
- INCOMPLETE runtime gate 生产链路

## 3. Allowed Future Implementation Files

后续最小实现阶段只允许新增或修改与 BoundaryCandidateService 最小服务层直接相关的文件。

建议文件范围：

- src/main/java/org/example/trademodel/service/planboundary/BoundaryCandidateService.java
- src/main/java/org/example/trademodel/service/planboundary/BoundaryCandidateServiceImpl.java
- src/test/java/org/example/trademodel/service/planboundary/BoundaryCandidateServiceTest.java

如果项目现有 package 结构不适合上述路径，后续实现时必须先只读确认现有结构，再选择最小一致路径。

## 4. Explicitly Forbidden Files For Next Implementation

后续最小实现阶段不允许修改：

- schema.sql
- application.yml
- dashboard.html
- controller
- mapper
- entity
- PlanReadiness
- ExecutionPlan
- RuleEngine
- Push workflow
- order API
- 自动交易相关代码

如确需修改上述文件，必须另开方案文档，不得混入 BoundaryCandidateService 最小实现阶段。

## 5. Service Interface Checklist

后续创建 BoundaryCandidateService 接口时，必须满足：

- 只暴露最小方法
- 输入应包含 RuntimeKlineContextDTO
- 输入应包含 symbol
- 输入应包含 timeframe
- 输入应包含 latest price / current price
- 输入应能承载 dataQualityScore / staleStatus / missingFields / blockingReasons
- 返回值必须是 BoundaryCandidateDTO
- 不返回交易指令
- 不返回 order request
- 不返回自动执行动作

接口命名必须避免暗示自动交易，例如不得使用：

- executeOrder
- autoTrade
- placeOrder
- closePosition
- reversePosition

## 6. Service Implementation Checklist

后续创建 BoundaryCandidateServiceImpl 时，必须满足：

- 只做 DTO 层判断
- 不访问 DB
- 不访问 mapper
- 不调用 controller
- 不调用外部交易所下单接口
- 不调用 order API
- 不推送机会
- 不生成自动开仓动作
- 不生成自动平仓动作
- 不生成自动反手动作
- 不绕过 BoundaryCandidateDTO 安全默认值
- 不通过反射设置交易字段
- 保持 manualReviewRequired = true
- 保持 notTradeInstruction = true

## 7. Mandatory INCOMPLETE Checklist

后续实现必须优先覆盖 INCOMPLETE。

以下情况必须返回 INCOMPLETE：

- RuntimeKlineContextDTO 为 null
- RuntimeKlineContextDTO missing
- staleStatus = STALE 且无法确认行情有效
- staleStatus = UNKNOWN 且缺少补充确认
- OHLCV 缺失
- klineItems 缺失
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

INCOMPLETE 不等于看多、看空、开仓、平仓或反手。

## 8. WATCH_ONLY Checklist

后续实现第二优先覆盖 WATCH_ONLY。

以下情况应返回 WATCH_ONLY：

- 数据基本可用但结构不完整
- 行情处于重大事件窗口
- 短线插针尚未确认
- 多周期方向冲突未收敛
- 风险升高但流动性状态尚未确认
- 候选边界存在但置信不足
- 价格已经偏离合理 entry zone
- RR 不足但风险未达到 INVALID
- 需要人工复核

WATCH_ONLY 只能表示继续观察或等待确认，不能表示交易执行动作。

## 9. INVALID Checklist

后续实现第三优先覆盖 INVALID。

以下情况可以返回 INVALID：

- 候选边界已经失效
- 当前价格已远离候选结构
- stop 位置不再合理
- RR 明显不满足最低要求
- 结构被破坏
- 风险状态超过允许范围
- 数据证明原候选假设不成立

INVALID 不等于反手。

INVALID 只表示当前候选边界不可用。

## 10. VALID Candidate Boundary

当前 BoundaryCandidateDTO 尚未提供 valid factory。

因此后续最小实现阶段默认不输出 VALID candidate。

如果需要 VALID candidate，必须先单独补充：

- valid factory 方案文档
- valid factory checklist
- BoundaryCandidateDTO 最小改动
- 单元测试
- verification 文档

在 valid factory / Builder 收口前，BoundaryCandidateService 不允许绕过 DTO 安全默认值生成可交易候选。

## 11. Risk Action Guard Checklist

后续实现必须遵守 Risk Action Guard：

- 高风险不等于自动平仓
- 高风险不等于反手
- 插针不等于趋势反转
- 踩踏状态禁止机会推送
- 流动性恶化时不建议市价一次性砍仓
- 高风险但流动性正常时，只能进入减仓、移动止损、降杠杆等人工复核方向
- 高风险且流动性恶化时，优先分批降风险、等待流动性恢复、只降杠杆
- 高风险且存在踩踏时，进入极端压力锁定，不生成机会推送，不生成反手计划
- 高风险但只是短线插针时，不直接判定趋势反转

BoundaryCandidateService 只能输出 BoundaryCandidateDTO 状态结果，不输出自动交易动作。

## 12. Unit Test Checklist

后续实现必须添加单元测试，至少覆盖：

- null RuntimeKlineContextDTO -> INCOMPLETE
- missing RuntimeKlineContextDTO -> INCOMPLETE
- STALE context -> INCOMPLETE
- UNKNOWN context without confirmation -> INCOMPLETE
- missing OHLCV -> INCOMPLETE
- insufficient kline window -> INCOMPLETE
- low dataQualityScore -> INCOMPLETE
- missing latest price -> INCOMPLETE
- missing numeric source -> INCOMPLETE
- event window -> WATCH_ONLY
- wick not confirmed -> WATCH_ONLY
- multi-timeframe conflict -> WATCH_ONLY
- invalidated structure -> INVALID
- poor RR -> INVALID
- manualReviewRequired remains true
- notTradeInstruction remains true
- no order related fields or methods are introduced

## 13. Forbidden Test Assertions

测试中不允许出现：

- 自动下单成功
- 自动开仓成功
- 自动平仓成功
- 自动反手成功
- order API called
- entry / stop / TP 已经落地
- RuleEngine 已经完成
- PlanBoundary 已经完成
- production migration has been executed

## 14. Verification After Future Implementation

后续最小实现完成后，必须执行：

- compile
- test-compile
- BoundaryCandidateServiceTest

如果项目环境或 sandbox 限制导致测试无法执行，必须如实记录原因，不得写 PASS。

后续还必须新增 verification 文档，记录：

- 实现文件
- 测试结果
- DTO 边界
- Risk Action Guard
- 禁止事项
- 当前未接模块
- 后续建议

## 15. Explicit Non-Goals For Next Implementation

下一阶段仍不做：

- 不接 DB
- 不接 mapper
- 不接 controller
- 不改 schema
- 不改 dashboard
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
- 不声明 entry / stop / TP 已经落地
- 不声明 RuleEngine 已经完成
- 不声明 PlanBoundary 已经完成
- 不声明生产链路已经完成

## 16. Acceptance Criteria For This Checklist Stage

本阶段验收标准：

- 目标 checklist 文档存在
- 文档只定义 BoundaryCandidateService 后续实现清单
- 文档明确允许文件范围
- 文档明确禁止文件范围
- 文档明确 INCOMPLETE / WATCH_ONLY / INVALID 优先级
- 文档明确 VALID candidate 暂缓
- 文档明确 Risk Action Guard
- 文档明确禁止 order API 和自动交易
- 文档明确后续测试要求
- 文档明确本阶段不实现 Java 代码

本阶段不要求 compile。

本阶段不要求 test。

## 17. Current Conclusion

BoundaryCandidateService 仍未实现。

本阶段只完成 implementation checklist。

下一步应在 checklist staging 前复核通过后，再精准 stage 并 commit 本文档。

在 checklist 提交前，不应直接实现 Java service。
