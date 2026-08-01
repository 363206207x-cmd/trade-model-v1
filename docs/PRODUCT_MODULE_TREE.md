# Trade Model V1 Product Module Tree

Status: `P0_PRODUCT_BASELINE_FREEZE_CANDIDATE`

This is a product tree, not a source-code directory and not a completion claim. Authority: `docs/PRODUCT_SOURCE_OF_TRUTH.md`.

## Product Tree

```text
Trade Model V1
|
+-- 登录
|   +-- 用户名/密码表单
|   +-- 服务端 Session
|   +-- Cookie / CSRF
|   +-- 登录失败与重试
|   +-- 登出
|   `-- 会话失效与重新登录
|
+-- 首页
|   +-- 顶部系统与市场状态
|   |   +-- 当前资产市场趋势
|   |   +-- 当前资产风险等级
|   |   +-- 当前资产数据质量
|   |   +-- AI 冲突等级
|   |   +-- 待复核机会
|   |   +-- Confused 状态
|   |   `-- Hot Reset 状态
|   +-- 实时告警
|   +-- 关键事件窗口
|   +-- 重点资产
|   |   +-- 当前资产上下文
|   |   +-- 价格与更新时间
|   |   +-- 市场倾向
|   |   +-- 综合评分
|   |   +-- 置信等级
|   |   +-- 风险等级
|   |   +-- AssetState
|   |   +-- 是否值得开仓
|   |   +-- 卡片正文切换上下文
|   |   `-- 独立 Analysis Detail 入口
|   +-- Execution Plan
|   |   +-- 推荐方向/动作
|   |   +-- 是否值得开仓
|   |   +-- 入场区
|   |   +-- 止损
|   |   +-- 止盈
|   |   +-- 加仓/减仓/放弃条件
|   |   +-- 失效条件
|   |   +-- 杠杆建议
|   |   +-- 仓位建议
|   |   +-- 有效期
|   |   `-- source trace / 验证状态
|   +-- 三 AI 摘要
|   |   +-- GPT Final
|   |   +-- Gemini Review
|   |   +-- Grok Challenge
|   |   +-- 一致性摘要
|   |   +-- 冲突/降级原因
|   |   `-- unavailable / fallback 状态
|   +-- Top3 持仓监控
|   |   +-- 置顶优先
|   |   +-- 风险排序
|   |   +-- 更新时间
|   |   `-- exact positionId 详情入口
|   +-- contextual details
|   `-- Loading / Empty / Error / Partial / Missing
|
+-- 持仓
|   +-- 用户真实持仓列表
|   +-- 手动录入持仓
|   |   +-- symbol
|   |   +-- 多/空方向
|   |   +-- 实际开仓价与时间
|   |   +-- 实际数量/仓位
|   |   +-- 实际杠杆
|   |   +-- 用户止损/止盈
|   |   +-- 原 ExecutionPlan 关联
|   |   `-- 用户备注
|   +-- 系统原计划与用户实际执行对比
|   +-- 原入场逻辑持续验证
|   |   +-- LOGIC_VALID
|   |   +-- LOGIC_WEAKENED
|   |   `-- PLAN_INVALIDATED
|   +-- 反转判断
|   |   +-- 无反转
|   |   +-- 弱反转
|   |   `-- 强反转
|   +-- 止损/止盈距离
|   +-- 浮盈浮亏与账户影响
|   +-- 仓位/杠杆/相关性风险
|   +-- 流动性与插针过滤
|   +-- 最新监控结论
|   +-- 调整建议（仅人工建议）
|   +-- 风险告警
|   +-- PositionMonitorLog 时间线
|   +-- 手动记录平仓
|   `-- CLOSED 后复盘入口
|
+-- AI分析
|   +-- 已有资产上下文/分析入口
|   +-- authoritative analysisId
|   +-- 规则基础结论
|   +-- 数据质量门禁
|   +-- 八大评分
|   |   +-- 趋势结构
|   |   +-- 资金推动
|   |   +-- 杠杆风险
|   |   +-- 流动性质量
|   |   +-- 情绪温度
|   |   +-- 事件冲击
|   |   +-- 宏观环境
|   |   `-- 综合可信度
|   +-- 5m / 15m / 1h / 4h
|   +-- 支持证据
|   +-- 反对证据
|   +-- GPT Final
|   +-- Gemini Review
|   +-- Grok Challenge
|   +-- 四级冲突
|   +-- Confused
|   +-- AI trigger gate
|   +-- AI fallback
|   +-- 数据来源/时间戳/rule version/trace
|   `-- Analysis Detail
|
+-- 消息
|   +-- OPPORTUNITY
|   |   +-- authenticated shared public projection
|   |   +-- public opportunity identity/status/timestamp/description
|   |   `-- 不包含任何用户私有风险字段
|   +-- POSITION_RISK
|   |   +-- owner-scoped private projection
|   |   +-- exact position identity
|   |   +-- monitor risk/status/reason
|   |   `-- current-user ownership isolation
|   +-- Message Center 列表
|   +-- 原始快照
|   +-- 当前状态
|   +-- Push Recheck 复核结果
|   +-- 变化原因
|   +-- Push Detail
|   `-- READY / EMPTY / ERROR / MISSING / PARTIAL
|
+-- 我的
|   +-- 当前账号/Session 摘要（仅真实返回时）
|   +-- 登出
|   +-- 系统/规则版本（仅真实返回时）
|   +-- 通知偏好（暂未开放）
|   +-- AI 偏好（暂未开放）
|   +-- 风险偏好（暂未开放）
|   `-- 不提供社区、推荐、付费套餐、自动交易或交易所下单
|
+-- 资产分析详情
|   +-- 资产/Decision 摘要
|   +-- Evidence & Scoring
|   +-- 八大评分（仅返回项）
|   +-- 多周期收敛
|   +-- 支持/反对/中性证据
|   +-- 三 AI 角色详情
|   +-- 冲突原因
|   +-- 数据质量与来源
|   `-- exact ExecutionPlan 入口
|
+-- 持仓详情
|   +-- UserPosition 用户事实
|   +-- 原 ExecutionPlan 参考
|   +-- 当前 PositionMonitor 结果
|   +-- PositionMonitorLog 历史
|   +-- 人工处理记录
|   `-- CLOSED 后 Review 入口
|
+-- Push Detail
|   +-- source-specific public/private projection
|   +-- 原始消息上下文
|   +-- 当前复核状态
|   +-- 漂移/失效/风险/冲突原因
|   +-- 资产详情或持仓监控入口
|   `-- 不含执行、下单或 UserPosition 变更操作
|
+-- 复盘
|   +-- 已平仓 UserPosition
|   +-- 原计划与实际执行偏差
|   +-- 监控与告警时间线
|   +-- 实际结果
|   +-- EXECUTED_VALID / EXECUTED_INVALID
|   +-- MISSED_VALID / MISSED_INVALID
|   +-- PUSHED_NOT_FILLED_VALID
|   +-- BLOCKED_BY_RISK_VALID
|   +-- 用户反馈
|   `-- rule-version iteration evidence
|
+-- 设置
|   +-- 账号与 Session
|   +-- 只显示后端真实支持项
|   `-- 不支持项禁用并显示暂未开放
|
+-- 账户与风险
|   +-- OPEN / PARTIALLY_CLOSED 汇总
|   +-- CLOSED 排除
|   +-- 单笔暴露
|   +-- 杠杆
|   +-- 集中度/相关性
|   +-- 回撤/VaR proxy
|   `-- high-risk block（不是交易授权）
|
+-- 数据质量
|   +-- Provider/source health
|   +-- freshness / missing / delayed / malformed
|   +-- 统一分析输入快照
|   +-- 数据质量评分与扣分原因
|   +-- 多周期覆盖
|   +-- source trace
|   `-- fail-closed degradation
|
`-- 系统状态
    +-- Provider 调用档位与预算
    +-- Scheduler / idempotency / trace
    +-- AI provider availability/fallback
    +-- Confused / Hot Reset
    +-- 告警抑制与冷却
    +-- 日志/可观察性
    +-- 生产就绪度
    `-- 安全/隐私/无交易边界
```

## Primary Navigation Freeze

Mobile has exactly five primary tabs: 首页, 持仓, AI分析, 消息, 我的. 观察资产 and 复盘 are contextual, not additional primary tabs. Detail pages preserve authoritative `analysisId`, `executionPlanId`, `positionId`, or `messageId` as applicable.

## Non-Product Modules

Governance parsers, semantic inventories, digest generators, PR metadata, workflow helpers, test counts, and audit slices are not product modules. They may support delivery but cannot appear in the user-facing module tree or prove a module complete.
