# V1 Product Architecture Source Snapshot

> Canonical repository text snapshot mechanically extracted from the original Word product plan.
> The wording is source content, not a completion claim. Tables are appended after paragraph content.

## Source Provenance

- Source ID: `PS-V1-ARCHITECTURE`
- Original local path at freeze: `/Users/xuchao/Documents/多源证据驱动的交易决策闭环系统.docx`
- Original modified date: `2026-03-28`
- Original SHA-256: `822865bc41e34d660d96bd36ba1d78d18f902a5626efe27fba48c4b60fde9f0a`
- Extraction mode: non-empty paragraphs in document order, followed by source tables

## Extracted Product Plan

多源证据驱动的交易决策闭环系统（V1）

项目总框架 + 数据库表设计 + 后端接口返回标准 + Java 实体类与 VO 命名

一、项目定位

本系统定位为一套“多源证据驱动的交易决策闭环系统（V1）”。

V1 的目标是优先实现四件事：

1. 能稳定跑起来

2. 每个结论都能看到证据来源

3. 每个建议都能转成结构化计划

4. 每次判断都能留档，后面能复盘和修正

V1 先做成一个“能看证据、能出评分、能出计划、能监控失效、能留痕复盘”的稳版本。

同时，V1 从一开始就具备三种工程属性：

1. 可追踪：每一次分析都能追到输入快照、证据、评分、决策、计划、告警、复盘

2. 可约束：所有判断标准、状态流转、告警触发都不能散落在代码里随意变化

3. 可迭代：复盘结果能够回流到规则层，形成后续版本优化依据

二、V1 范围控制

V1 范围严格控制但根据实际需求适度扩展：

1. 资产范围：BTC、ETH，以及 3 到 5 个用户常看的币（支持用户自定义关注列表，优先覆盖高流动性主流币种）

2. 周期范围：5m、15m、1h、4h。其中 5m 专门用于短期插针风险（长影线、pin bar、流动性扫荡、极端 wick 波动）检测；15m、1h、4h 用于核心趋势结构确认和综合决策，确保系统能精准判断短期插针风险，并支持多周期收敛判断（高周期定方向、中周期定结构、低周期定时机与插针过滤）

3. 数据范围：行情/K线、成交量、OI（未平仓量）、Funding（资金费率）、爆仓数据、ETF 资金流、宏观日历、新闻事件

4. 功能范围：只做七块——市场环境判断、原始证据生成、八大评分、综合决策、执行计划、监控提醒、复盘留档

5. 执行边界：V1 不直接做自动下单，但所有执行计划必须具备可导出、可被外部执行端消费的结构化能力

三、系统总流程

系统固定走一条主链路：

原始数据接入 → 原始证据生成 → 证据标准化 → 八大评分 → 综合决策 → 执行计划 → 实时监控 → 结果复盘 → 规则修正与版本迭代

同时补齐一条“分析触发链”：

定时调度 / K线更新事件 / 外部 API 主动触发 → 幂等校验 → 分布式锁控制 → 创建分析上下文 → 执行主链路

四、旧模型中可保留的内容及归位方式

旧模型中有价值部分重新归位：

1. 市场参考 → 升级为“市场环境模块”

2. 结构带 → 归入“执行计划模块”

3. 自动计划 → 升级为“执行计划引擎”

4. 观察币看板 → 归入“总览页”

5. 初步监控思路 → 升级为“监控与告警模块”

五、系统结构

V1 拆成“两层基础层 + 六个业务模块”。

（一）基础层

1. 数据基础层

统一接收、整理、缓存原始数据。职责：拉取行情、K线、OI、Funding、爆仓、ETF、宏观日历、新闻事件；保存原始快照；检查数据缺失、延迟、异常。

必须记录每个数据源的抓取时间、源状态、延迟秒数、缺失率、异常标记；每次分析保留输入快照；同一分析时刻内数据快照保持一致性；同时内置 AI调用成本监控与缓存管理（Redis 缓存 LLM 响应结果，TTL 5 分钟；全局 Token 预算实时统计；调用前检查剩余配额与当前并发）。

2. 规则基础层

统一定义所有判断标准（趋势成立、结构有效、过热、高风险、事件窗口、不适合交易、降杠杆、观望、短期插针风险、多周期收敛要求等）。

采用“常量类 + 规则配置表”双层结构；每次规则调整产生版本号；支持热加载但必须有生效范围和版本审计；新增用户个性化配置与全局风险阈值管理，同时新增 AI调用策略配置（调用触发阈值、单次 Token 上限、每日预算、限流窗口、fallback 策略）。

（二）六个业务模块

1. 市场环境模块

判断当前大环境。输入：BTC/ETH 走势、ETF 流量、宏观日历、大盘波动率等。输出：环境类型、风险模式、趋势友好度、杠杆建议、总体摘要。

2. 原始证据模块

把原始数据转成证据对象。证据分五类：价格结构证据、杠杆证据、资金证据、事件证据、风险证据。每条证据必须说明“当前是什么、相比基准变化多少、方向、强弱、可信度”。价格结构证据特别包含 5m 周期短期插针风险识别（长影线、pin bar、流动性扫荡）。

3. 证据评分模块

把证据转成八大评分：趋势结构分、资金推动分、杠杆风险分、流动性质量分、情绪温度分、事件冲击分、宏观环境分、综合可信度分。杠杆风险分与流动性质量分特别强化对短期插针风险的扣分权重。

4. 综合决策模块

把八个评分整合成最终结论。固定输出七项：市场倾向强度、交易类型、置信等级、风险等级、动作优先级、结论摘要、是否值得开仓意见。

市场倾向强度采用分层级设计（强偏多、偏多、弱偏多、震荡、弱偏空、偏空、强偏空、观望），让结果更直观清晰；置信等级仍保留 high/medium/low 作为辅助层级。

必须包含多周期收敛逻辑：5m 用于插针过滤，15m/1h/4h 用于趋势与结构确认，所有周期信号一致且无重大冲突时才输出进攻型决策，否则自动降级或转观望。

多周期收敛具体算法与阈值规则已完整落地：

- 规则表 tm_rule_config 新增 multi_tf_convergence 字段（JSON），定义每周期权重（4h:40%、1h:30%、15m:20%、5m:10%）与一致性阈值（趋势得分差异 ≤ 15%、方向相同且至少 3 个周期一致才视为收敛）。

- 算法实现：MultiTimeframeConvergenceChecker 计算加权趋势得分，若差异 > 15% 或方向冲突则直接降级置信等级一级并强制转“观望”；冲突详情记录到 DecisionBundle 的 multi_tf_conflict 字段，供复盘使用。

- 阈值支持热更新，复盘反馈可直接修改规则版本。

同时引入多AI角色辅助裁决机制，实现三方制衡决策：

1) GPT-5.4：最终裁决官

负责汇总三方证据、统一打分、输出偏多 / 偏空 / 观望、给出置信度、给出核心支持证据与核心反证、给出是否值得开仓的最终意见。

2) Gemini 2.5 Pro：冲突复核官

负责专门找主裁决里的漏洞、对冲突证据做二次审查、判断是否需要降级为“观望”、对终裁结果给出“维持 / 调整 / 驳回”意见。

3) Grok 4.20：快讯与反方挑战官

负责快速抓突发信息、做情绪和事件面补充、专门提出反方论点、在有搜索工具时补实时外部信息（含短期插针触发事件）。

AI 调用采用智能分级调度策略彻底解决费用失控与调用超时：

- 触发条件：仅当 data_quality_score ≥ 85 且存在显著价格/成交量/OI/Funding 变化（规则表可配）时才发起 AI 调用，否则直接走纯规则引擎模式。

- 缓存机制：Redis 缓存最近 5 分钟内相同 symbol + timeframe + 关键证据哈希的 AI 决策结果，命中即跳过外部调用。

- 配额管理：全局 + 用户级 Token 预算（每日/每小时上限可配），调用前实时检查剩余配额，剩余不足时自动降级为规则模式并告警。

- 智能限流：采用 Redis + Token Bucket 算法实现 per-provider 限流（每秒/每分钟调用上限）、per-asset 限流（同一资产 30 秒内最多一次 AI 调用）、并发控制（Semaphore 限制同时进行 AI 任务数 ≤ 3）。

- 超时与 fallback：每路调用设置 8 秒超时，超时或失败自动 fallback 到规则引擎，并记录到调用日志。

- 成本记录：每次调用记录 Token 消耗、实际费用、耗时、是否命中缓存，落库 tm_ai_call_log 供后续审计与优化。

5. 执行计划模块

把结论转成结构化方案。输出：推荐动作、触发条件、入场区、止损区、止盈规则、加仓条件、减仓条件、放弃条件、失效条件、杠杆建议、仓位建议。

仓位建议必须量化（默认单笔风险不超过账户总资金的 1-2%，支持用户个性化风险偏好配置）；新增轻量级账户级整体风险评估（当前持仓总风险、相关性风险、最大回撤预估）。

账户级整体风险量化算法已完整落地：

- 采用简单可控公式：总风险分 = Σ（仓位占比 × 资产波动率） + 相关性惩罚（Pearson 相关系数矩阵，同一方向持仓相关性 > 0.7 时额外加 20% 风险）。

- 最大回撤预估：基于历史 30 天波动率模拟 95% VaR（Value at Risk），公式为 position_value × volatility × 1.65。

- 结果实时写入 tm_execution_plan 的 account_risk_json 字段，并落地 tm_account_risk_snapshot 表供前端展示与监控使用。

- 规则表支持用户自定义风险上限（单笔 ≤ 2%、总风险 ≤ 8%），超过阈值自动降仓位 50% 或转观望。

6. 监控与复盘模块

盯住结论是否失效，同时保存每次结论供复盘。监控重点：证据反转、结构失效、风险升级、短期插针风险告警、多周期信号背离。复盘记录：当时结论、证据、评分、后续走势、实际盈亏、错误类型、调整建议；同时支持轻量级历史回测能力（可基于历史 analysis_run 数据快速验证规则版本效果）。

告警模块防疲劳保护机制已完整落地：

- Redis 实现 alert_throttle（key: asset+alert_type，TTL 15 分钟），同一资产同一告警类型 15 分钟内只发一次。

- tm_monitor_alert 新增 cooldown_until 字段，冷却期内告警直接标记为 suppressed。

- 支持用户个性化冷却时长配置（默认 15 分钟，可通过 tm_user_config 调整）。

- 重复告警自动合并为一条“已持续 X 分钟”通知，避免轰炸。

5m 高频插针检测性能与资源消耗已完整优化：

- 数据基础层采用 Redis + Caffeine 双层缓存预计算 5m K线指标（长影线比率、wick 幅度、成交量突增），更新时仅增量计算。

- 调度引擎对 5m 任务使用异步线程池（最大并发 10），同一资产 5m 分析强制 10 秒防抖。

- 资源监控：AnalysisSchedulerService 实时统计 CPU/内存占用，超过 70% 时自动降频或跳过非核心资产的 5m 分析。

- 性能测试阈值记录到 tm_data_source_health，确保并发 20+ 资产时延迟 < 2 秒。

用户个性化配置已在规则层与前端完整支持落地：

- 新增 tm_user_config 表（user_id、risk_preference、ai_model_preference、notify_channels、cooldown_minutes 等字段）。

- RuleConfigService 启动时加载用户配置并与全局规则合并，优先级：用户 > 全局。

- 前端新增配置接口（RuleController），支持实时保存与热生效。

- 决策与计划模块在生成结果前读取用户配置自动调整仓位上限、AI 偏好、通知渠道。

六、V1 的五个核心数据对象

1. AssetAnalysis：单个资产某一时刻的完整分析结果

2. EvidenceItem：单条证据

3. ScoreItem：单项评分结果

4. DecisionBundle：综合决策结果

5. ExecutionPlan：执行计划

七、接口分组框架

后端接口按七组划分：

1. 基础数据接口组

2. 数据健康接口组

3. 原始证据接口组

4. 评分接口组

5. 决策接口组

6. 执行接口组

7. 监控与复盘接口组

所有写接口必须支持幂等控制；告警接口预留通知下发能力；决策接口组新增多AI角色调用子接口（支持GPT、Gemini、Grok API智能调度、fallback 与汇总）。

八、前端页面建议

V1 前端只做三页：

1. 总览页：当前市场环境、数据质量分、重点资产列表、综合结论、风险等级、动作建议

2. 证据与评分页：原始证据、支持/反对证据、八大评分、数据质量说明、多AI角色裁决过程（含每角色意见）、短期插针风险标记、多周期收敛状态

3. 策略与监控页：执行计划、结构带、入场区、止损区、止盈方案、监控状态、告警、近几次复盘结果、账户整体风险概览

轮询策略：5m 周期 15 秒、15m 周期 30 秒、1h 周期 60 秒、4h 周期 120 秒。

九、V1 开发顺序

1. 先定五个核心对象

2. 做基础数据接口

3. 做全量证据接口

4. 做全量评分接口

5. 做综合决策接口（优先实现多AI角色智能调度、fallback、多周期收敛、缓存、配额检查、限流与用户配置加载）

6. 做执行计划接口

7. 做监控与复盘接口

7.5. 实现分析调度引擎与幂等控制

8. 最后做页面

十、V1 的执行计划模板

固定四类模板：

1. 趋势突破型

2. 回踩承接型

3. 事件观望型

4. 高风险反向挤仓型

模板通过“评分组合 → 模板匹配”规则自动切换，多AI角色裁决结果与多周期收敛结果作为最终模板切换依据。

十一、V1 的增强机制

1. 反对证据列表（必须同时展示支持与反对证据）

2. 数据质量折扣（自动降低置信度，低于 70 分触发熔断）

3. 资产状态机（observing、candidate、waiting_trigger、triggered、high_risk、invalidated、cooling）

4. 熔断机制（data_quality_score < 70 时强制切到暂不交易或事件观望）

5. 规则反馈闭环（复盘结果直接映射到规则调整，生成新版本）

6. 多AI角色辅助裁决机制（GPT-5.4最终裁决官、Gemini 2.5 Pro冲突复核官、Grok 4.20快讯与反方挑战官），形成实时情报→冲突复核→最终裁决的三层制衡，包含智能分级触发、Redis 缓存、Token 预算配额管理、Token Bucket 限流、8 秒超时 fallback、成本实时记录等完整策略

7. 短期插针风险专属检测机制（5m 周期证据与评分权重直接影响风险等级与计划模板切换）

8. 决策结果分层级机制（市场倾向强度采用强/中/弱层级呈现，使偏多/偏空/观望 + 置信度输出更直观清晰）

9. 多周期收敛机制（高周期趋势 + 中周期结构 + 低周期时机必须一致，否则自动降级；具体算法与阈值已在规则表与 MultiTimeframeConvergenceChecker 中落地）

10. 账户级整体风险控制机制（量化单笔与总仓位风险，实时评估组合相关性与最大回撤；具体公式已在执行计划模块落地）

11. 异常处理与降级容错机制（AI 调用失败、数据异常、多周期背离时自动切换至纯规则引擎模式并记录）

12. 告警防疲劳保护机制（Redis throttle + 冷却期 + 合并通知，已在监控模块完整实现）

13. 5m 插针检测性能优化机制（双层缓存 + 异步防抖 + 资源监控，已在数据基础层与调度引擎完整实现）

14. 用户个性化配置机制（tm_user_config 表 + RuleConfigService 动态加载，已在规则层与前端完整支持）

十二、数据库表设计（V1）

数据库设计原则：

- 业务结果落库，原始高频数据不全量落库

- 核心字段固定，扩展内容放 JSON

- 只用一个业务库（trade_model_v1）

- 表数量控制在 11 张核心表 + 补充表

- 主键类 ID 一律使用字符串

- 高频查询字段提前加复合索引

- 分析主表是全链路锚点

- 时间字段统一存 UTC

- JSON 字段只作为扩展槽

- 状态写入必须统一入口

- 分析记录支持版本对比

- 关键写操作放入同一事务

核心表（11 张）：

1. tm_asset

2. tm_analysis_run（全系统最核心表）

3. tm_analysis_input_snapshot

4. tm_evidence_item

5. tm_score_item

6. tm_decision_result（新增 ai_role_results JSON 字段记录三AI角色原始意见）

7. tm_execution_plan

8. tm_monitor_alert

9. tm_review_result

10. tm_data_source_health

11. tm_asset_state

补充表：

12. tm_macro_event

13. tm_news_event

14. tm_rule_config

15. tm_rule_version_log

16. tm_ai_call_log（记录每次 AI 调用 Token 消耗、费用、耗时、命中缓存、fallback 情况）

17. tm_account_risk_snapshot（记录账户级风险量化结果）

18. tm_user_config（用户个性化配置表）

所有明细子表与 tm_analysis_run 建立明确关联；写入主表+证据+评分+决策+计划必须同一事务。

十三、后端接口返回 JSON 标准

所有接口统一返回结构：

code、msg、request_id、server_time、data

时间格式统一为 ISO 8601 带时区；空数组返回 [] 而非 null；数值字段直接返回数字；枚举值固定；所有主键 ID 用字符串；写接口支持幂等；每个返回对象携带分析ID、规则版本、traceId 等元信息；决策接口新增 ai_roles 字段返回三AI角色完整过程。

十四、核心返回对象标准

1. 市场环境对象

2. 单条证据对象

3. 单项评分对象

4. 综合决策对象（新增 market_bias_hierarchy 字段，值固定为强偏多、偏多、弱偏多、震荡、弱偏空、偏空、强偏空、观望；新增 multi_tf_convergence 字段记录多周期收敛状态）

5. 执行计划对象

6. 监控状态对象

7. 复盘对象

8. 聚合分析对象（AssetAnalysisVO）

十五、数据质量分与折扣机制

采用 100 分制，默认 100 分，按问题扣减（ETF 缺失扣 15、新闻延迟扣 10 等）。

data_quality_score < 85 置信等级降一档；< 70 触发熔断。

所有扣分项写入 tm_data_source_health 和分析结果 ext_info。

十六、资产状态机与监控触发规则

状态固定为：observing、candidate、waiting_trigger、triggered、high_risk、invalidated、cooling。

状态只能由 MonitorService 独占写入；同一 symbol + timeframe 同一监控周期内不允许多次翻转；必须有防抖逻辑。

十七、分析调度、幂等与链路日志

调度机制：定时调度 + 事件触发 + 外部调用触发。

并发控制：Redis 分布式锁；同一 symbol + timeframe + analysis_time + 版本只允许一条有效记录。

全程带 traceId；写接口带 request_id；决策环节必须记录三AI角色调用 trace、成本与 fallback 事件，同时在 tm_ai_call_log 中持久化成本数据。

十八、Java 实体类与 VO 命名

（一）命名规则

- 数据库实体类：DO 后缀（AssetDO、AnalysisRunDO 等）

- 接口入参：Req 后缀

- 接口返回：VO 后缀

- 统一外层返回壳：ApiResponse

- 枚举类：Enum 后缀

- 常量类：Constants

- 转换器：Converter

- 服务：Service / ServiceImpl

- 校验器：Validator

（二）包结构

主包：org.example.trademodel

子包：common、config、controller、service、service.impl、mapper、entity、dto.req、dto、vo、enums、converter、validator

（三）数据库实体类

AssetDO、AnalysisRunDO、AnalysisInputSnapshotDO、EvidenceItemDO、ScoreItemDO、DecisionResultDO、ExecutionPlanDO、MonitorAlertDO、ReviewResultDO、DataSourceHealthDO、AssetStateDO、RuleConfigDO、RuleVersionLogDO、AiCallLogDO、AccountRiskSnapshotDO、UserConfigDO

（四）请求对象

BaseAnalysisReq、BuildEvidenceReq、BuildScoreReq、BuildDecisionReq、GeneratePlanReq、UpdateMonitorReq、SaveAnalysisReq、SavePlanReq、WriteReviewResultReq、QueryHistoryReq、RetryAnalysisReq、UserConfigReq（新增）

（五）核心返回对象

MarketEnvironmentVO、EvidenceItemVO、ScoreItemVO、DecisionBundleVO、StructureBandVO、ExecutionPlanVO、MonitorStateVO、ReviewStateVO、AssetAnalysisVO、ReviewHistoryItemVO、RuleVersionVO、AlertNoticeVO

（六）内部 DTO

AnalysisContextDTO、EvidenceBundleDTO、ScoreBundleDTO、DecisionBundleDTO、PlanContextDTO、MonitorContextDTO、RuleFeedbackDTO、AiRoleDecisionDTO（新增，承载三AI角色结果）

（七）枚举类

MarketBiasEnum（升级为分层级：STRONG_BULLISH、BULLISH、WEAK_BULLISH、RANGE、WEAK_BEARISH、BEARISH、STRONG_BEARISH、WAIT）、TradeTypeEnum、ConfidenceLevelEnum、RiskLevelEnum、ActionPriorityEnum、EvidenceTypeEnum、EvidenceDirectionEnum、StrengthLevelEnum、ScoreDirectionEnum、AlertTypeEnum、AlertLevelEnum、DataSourceStatusEnum、AssetStateEnum、TriggerLogicTypeEnum、PlanStatusEnum、AnalysisStatusEnum、AiRoleEnum（新增：GPT_FINAL、GEMINI_REVIEW、GROK_CHALLENGE）

（八）Controller

MarketController、SystemController、EvidenceController、ScoreController、DecisionController、PlanController、MonitorController、ReviewController、RuleController、UserConfigController（新增）

（九）Service

MarketService、SystemHealthService、EvidenceService、ScoreService、DecisionService、PlanService、MonitorService、ReviewService、AssetStateService、RuleConfigService、AnalysisSchedulerService、AiDecisionOrchestratorService（新增，负责三AI角色智能调度、fallback、多周期收敛、缓存检查、配额校验、限流与成本记录）、UserConfigService（新增）

（十）Mapper

AssetMapper、AnalysisRunMapper、AnalysisInputSnapshotMapper、EvidenceItemMapper、ScoreItemMapper、DecisionResultMapper、ExecutionPlanMapper、MonitorAlertMapper、ReviewResultMapper、DataSourceHealthMapper、AssetStateMapper、RuleConfigMapper、RuleVersionLogMapper、AiCallLogMapper、AccountRiskSnapshotMapper、UserConfigMapper（新增）

（十一）统一返回壳

ApiResponse（提供 success 和 fail 静态方法）

十九、项目实施建议

第一轮：完成主链路空跑 + 规则配置化雏形 + traceId 全链路日志 + 多AI角色智能调度框架（含缓存、配额、限流、多周期收敛算法）

第二轮：接数据库，打通落库、事务、幂等、状态机、用户配置表

第三轮：接入真实数据源，开启数据质量打分、熔断、告警、5m 插针检测、多周期收敛验证、账户风险量化

第四轮：接入调度与监控任务、账户级风险评估、防疲劳告警

第五轮：做前端三页与个性化配置页面

V1 的关键词：稳、清晰、可解释、可追踪、可迭代。

主链路、状态机、规则层、幂等控制、数据质量折扣、复盘反馈、多AI角色制衡、短期插针风险检测、决策结果分层级、多周期收敛机制、账户级风险控制、AI 调用成本控制、告警防疲劳、5m 性能优化、用户个性化配置从 V1 就必须打稳。

二十、V1 当前考虑不足与待改进点

**<span style="color:red">【已解决】AI 调用成本控制、配额管理、智能限流策略已在规则层、数据基础层和 AiDecisionOrchestratorService 中完整落地（智能分级触发、Redis 缓存、Token 预算、Token Bucket 限流、8秒超时 fallback、tm_ai_call_log 记录），V1 阶段费用可控、超时自动容错</span>**

**<span style="color:red">【已解决】多周期收敛的具体冲突解决算法与阈值规则已在 tm_rule_config + MultiTimeframeConvergenceChecker 中完整落地（加权得分 + 15% 差异阈值 + 方向一致校验），信号不一致时自动降级并记录冲突</span>**

**<span style="color:red">【已解决】账户级整体风险量化算法已在执行计划模块与 tm_account_risk_snapshot 表中完整落地（相关性 Pearson 矩阵 + VaR 公式），支持用户自定义风险上限</span>**

**<span style="color:red">【已解决】告警模块防疲劳保护机制已在 MonitorService 与 Redis throttle 中完整落地（15分钟冷却 + 重复合并），彻底杜绝通知轰炸</span>**

**<span style="color:red">【已解决】5m 高频插针检测性能与资源消耗已在数据基础层 + 调度引擎中完整优化（双层缓存 + 异步防抖 + 资源监控），并发 20+ 资产延迟 < 2 秒</span>**

**<span style="color:red">【已解决】用户个性化配置已在 tm_user_config 表 + RuleConfigService + UserConfigController 中完整落地，支持风险偏好、AI 偏好、通知渠道实时热生效</span>**
