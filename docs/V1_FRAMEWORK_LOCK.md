# V1_FRAMEWORK_LOCK — Trade Model V1 框架锁定说明

## 1. V1 总定位

Trade Model V1 是多源证据驱动的交易决策闭环系统。

它不是自动交易系统，不负责直接下单，不负责自动平仓，不承诺盈利。

系统只负责：

- 数据接入
- 证据生成
- 证据评分
- 综合决策
- 执行计划
- 持仓 / 机会监控
- 告警
- 复盘
- 规则修正

## 2. 固定主链路

所有功能必须能回到这条链路：

原始数据 → 原始证据 → 标准化证据 → 八大评分 → 综合决策 → 执行计划 → 监控 → 复盘 → 规则迭代

如果一个功能不能说明属于哪一环，不应优先进入 V1。

## 3. 当前最高治理边界

AI conflict / confused state / Push Recheck / Missed Opportunity / Hot Reset 是当前最高治理边界。

任何涉及方向输出、候选计划、执行建议、推送、持仓监控、复盘的功能，必须先检查这些治理边界。

治理边界未冻结前，不允许推进：

- 生产 `VALID`
- mapper
- Assembler
- `plan_boundary_json`
- 方向性推送
- 自动执行链路

这些治理边界高于局部 runtime 能力接线。Loader context 足量、K 线窗口足量、候选信息足量，都不等于生产 candidate 可 `VALID`。

## 4. AI 与规则层关系

规则层先产出基础方向、证据、评分、风险和状态。

AI 只能用于：

- 增强
- 解释
- 复核
- 反证
- 摘要
- 降级建议
- 风险提示

AI 不得替代规则层，不得直接触发下单、平仓、`triggered`、`VALID` 或可执行计划。

AI 不是三票投票制。不允许任一 AI 单独反对就无限观望。不允许 Gemini 或 Grok 直接覆盖规则状态机。

AI 冲突只能改变置信度、风险等级、计划模式和是否进入 confused。只有规则层冲突高，且 AI 冲突进一步放大冲突时，才允许进入 confused。

AI 冲突四级处理固定为：

1. Level 1：一致。保留方向，确认型计划可用。
2. Level 2：轻微分歧。保留方向，置信度降一级，风险或计划模式降级。
3. Level 3：显著分歧。不输出确认型执行建议，只允许 candidate / waiting_trigger / 预备型计划，并给恢复条件。
4. Level 4：极端分歧。进入 confused，禁止方向性执行建议。

## 5. Confused State / Logic Breaker

confused state 是 V1 的冲突熔断状态。

固定规则：

- `confused_score >= 70` 进入 confused。
- `confused_score >= 85` 禁止方向性推送。
- confused 状态下禁止标准看多 / 看空执行计划。
- confused 状态下 `triggered` 必须降级为 `observing` / `candidate` / `waiting_trigger` / `warning_only`。
- 退出 confused 后只能进入 `observing` 或 `candidate`，不能直接回 `triggered`。
- Loader context 足量不等于 candidate 可 `VALID`。
- 高 confused 下不能让生产 candidate 进入 `VALID`。

confused state 必须约束方向输出、候选计划、推送、持仓监控和复盘。

## 6. Push Recheck

推送不是静态机会，点击时必须重新核验。

Push Recheck 必须检查：

- 价格漂移
- 滑点
- 数据质量
- 账户风险
- 当前状态
- 原因层是否仍成立
- 是否过期

confused / high_risk / invalidated / cooling 应阻断或等待。Push Recheck 必须支持 `CONFUSED_BLOCKED`。

推送失败、漂移、失效、二次确认结果必须可追踪、可审计、可复盘。

## 7. Missed Opportunity

未执行但正确的机会必须记录，避免幸存者偏差。

以下机会必须纳入复盘视角：

- 推送未成交但正确
- 风险阻断但正确
- 已执行正确
- 已执行错误
- 未执行但正确
- 未执行且失效

Missed Opportunity 不得绕过 confused / high_risk / invalidated / existing position 边界。

## 8. Hot Reset

极端环境突变时允许 Hot Reset 高于常规慢状态机。

Hot Reset 只能触发：

- 重新计算
- 状态重建
- 风险提示
- 审计记录

Hot Reset 不得直接触发自动下单或自动平仓。

Hot Reset 之后不允许直接回到 `triggered`。

## 9. 两层基础能力

### 数据基础层

包括：

- 行情
- K 线
- 成交量
- OI
- funding
- 清算
- ETF
- 宏观
- 新闻
- 数据质量
- 数据源健康

### 规则基础层

包括：

- 趋势有效性
- 结构有效性
- 过热
- 高风险
- 事件窗口
- 不适合交易
- 降杠杆
- 观望
- 短周期 pin risk
- 多周期收敛
- 用户偏好
- AI 调用策略

## 10. 六个业务模块

V1 固定为六个业务模块：

1. 市场环境
2. 原始证据
3. 证据评分
4. 综合决策
5. 执行计划
6. 监控复盘

## 11. 八大评分

八大评分固定为：

1. 趋势结构分
2. 资金推动分
3. 杠杆风险分
4. 流动性质量分
5. 情绪温度分
6. 事件冲击分
7. 宏观环境分
8. 综合可信度分

不允许跳过八大评分直接输出多空。

## 12. 综合决策边界

综合决策应输出：

- marketBias
- marketBiasHierarchy
- tradeType
- confidenceLevel
- riskLevel
- actionPriority
- isWorthOpening
- supportingEvidence
- opposingEvidence
- summary
- multiTfConvergence
- aiConflictLevel
- ruleVersion
- traceId / analysisId

严重冲突时优先观望。

数据质量不足时优先降级或观望。

AI 冲突明显时必须解释冲突来源。

## 13. 执行计划边界

执行计划可以输出：

- 推荐动作
- 触发条件
- 入场区
- 止损区
- 止盈方案
- 加仓条件
- 减仓条件
- 失效条件
- 放弃条件
- 紧急退出条件
- 杠杆建议
- 仓位建议
- 最大风险

执行计划不能直接替用户下单。

## 14. 持仓监控边界

持仓监控必须基于用户手动录入或系统已确认的真实持仓事实。

没有以下信息，不允许展示为真实持仓：

- symbol
- positionSide
- avgOpenPrice
- positionQuantity 或仓位描述
- positionOpenTime 或录入时间

达到开仓条件只代表“候选机会”，不代表“已经开仓”。

系统建议不等于用户真实开仓。

只有用户手动录入真实持仓后，才进入持仓监控。

AI 三方重大冲突是持仓监控重新分析和风险升级触发源。

计划失效只能提示人工处理，不能自动假设用户已平仓。

平仓后首页监控清空，记录进入复盘。

系统建议与用户实际执行必须分开。

持仓监控属于：

执行计划 → 入场后重新验证 → 风险变化 → 平仓记录 → 复盘

## 15. 多周期规则

多周期收敛默认权重：

- 4h：40%
- 1h：30%
- 15m：20%
- 5m：10%

至少 3 个周期方向一致，且趋势分差异不超过阈值，才认为收敛。

不收敛时降低置信度。

严重冲突时切换为观望。

## 16. 数据质量规则

数据质量默认 100 分。

数据缺失、延迟、异常时必须扣分。

低于 85 时应降低置信度。

低于 70 时应切换为观望或事件观望。

不能把缺失数据伪装成完整数据。

## 17. AI 调用边界

AI 只能作为证据解释、冲突复核、最终裁决辅助，不得替代规则层和证据链。

AI 角色：

- 最终裁决官
- 冲突复核官
- 快讯 / 反方挑战官

AI 不得直接触发下单或平仓。

## 18. 首页边界

首页方向已冻结为：

- 重点资产监控
- 首页工作台 homeWorkbench
- 已开仓监控
- 执行建议
- AI 三方裁决
- 实时告警
- 关键事件
- 系统状态

未经用户明确要求，不允许重构首页 UI。

## 19. UI 边界

UI 是决策工作台，不是交易所下单终端。

所有视觉和文案必须服务：

- 证据可追踪
- 结论可解释
- 计划可执行
- 结果可复盘

不允许制造自动交易、自动执行、自动平仓的暗示。

不允许只展示结论不展示证据。

风险提示必须有文字原因。

## 20. API 契约边界

接口外层结构尽量保持：

{
  "code": 200,
  "msg": "success",
  "requestId": "...",
  "serverTime": "...",
  "data": {}
}

要求：

- 时间使用 ISO 8601。
- 数字字段使用 number。
- ID 使用 string。
- 空数组返回 []，不要返回 null。
- 关键链路保留 analysisId、ruleVersion、traceId。
- 不允许无说明修改已有字段语义。
