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
3. 可迭代：长期目标为复盘结果可支撑规则层迭代与版本优化；**当前落地**为复盘结论落库（`tm_review_result`）且每次保存**追加** `tm_rule_version_log` 审计记录（关联当时 `analysis_run` 的 `rule_version`），**不包含**自动修改规则配置、**不包含**自动生成或发布新规则版本

二、V1 范围控制

V1 范围严格控制但根据实际需求适度扩展：
1. 资产范围：BTC、ETH，以及 3 到 5 个用户常看的币，支持用户自定义关注列表，优先覆盖高流动性主流币种
2. 周期范围：5m、15m、1h、4h。其中 5m 专门用于短期插针风险（长影线、pin bar、流动性扫荡、极端 wick 波动）检测；15m、1h、4h 用于核心趋势结构确认和综合决策，确保系统能精准判断短期插针风险，并支持多周期收敛判断，即高周期定方向、中周期定结构、低周期定时机与插针过滤
3. 数据范围：行情/K线、成交量、OI（未平仓量）、Funding（资金费率）、爆仓数据、ETF 资金流、宏观日历、新闻事件
4. 功能范围：只做七块——市场环境判断、原始证据生成、八大评分、综合决策、执行计划、监控提醒、复盘留档
5. 执行边界：V1 不直接做自动下单，但所有执行计划必须具备可导出、可被外部执行端消费的结构化能力

三、系统总流程

系统固定走一条主链路：
原始数据接入 → 原始证据生成 → 证据标准化 → 八大评分 → 综合决策 → 执行计划 → 实时监控 → 结果复盘 → 规则修正与版本迭代

同时补齐一条分析触发链：
定时调度 / K线更新事件 / 外部 API 主动触发 → 幂等校验 → 分布式锁控制 → 创建分析上下文 → 执行主链路

四、旧模型中可保留的内容及归位方式

旧模型中有价值部分重新归位：
1. 市场参考 → 升级为市场环境模块
2. 结构带 → 归入执行计划模块
3. 自动计划 → 升级为执行计划引擎
4. 观察币看板 → 归入总览页
5. 初步监控思路 → 升级为监控与告警模块

五、系统结构

V1 拆成“两层基础层 + 六个业务模块”。

（一）基础层

1. 数据基础层
统一接收、整理、缓存原始数据。职责：拉取行情、K线、OI、Funding、爆仓、ETF、宏观日历、新闻事件；保存原始快照；检查数据缺失、延迟、异常。
必须记录每个数据源的抓取时间、源状态、延迟秒数、缺失率、异常标记；每次分析保留输入快照；同一分析时刻内数据快照保持一致性；同时内置 AI 调用成本监控与缓存管理，使用 Redis 缓存 LLM 响应结果，TTL 5 分钟；全局 Token 预算实时统计；调用前检查剩余配额与当前并发。

2. 规则基础层
统一定义所有判断标准，包括趋势成立、结构有效、过热、高风险、事件窗口、不适合交易、降杠杆、观望、短期插针风险、多周期收敛要求等。
采用“常量类 + 规则配置表”双层结构；每次规则调整产生版本号；支持热加载但必须有生效范围和版本审计；新增用户个性化配置与全局风险阈值管理，同时新增 AI 调用策略配置，包括调用触发阈值、单次 Token 上限、每日预算、限流窗口、fallback 策略。

（二）六个业务模块

1. 市场环境模块
判断当前大环境。输入：BTC/ETH 走势、ETF 流量、宏观日历、大盘波动率等。输出：环境类型、风险模式、趋势友好度、杠杆建议、总体摘要。

**（当前实现边界）** 上列为规格目标。**当前仓库**已形成本阶段最小闭环：`AnalysisAssemblerServiceImpl` 在主 `assemble` 链路调用 `RealMarketEnvironmentService.tryBuildFromRealQuote()`（heuristic 主链接入）；`tm_market_environment_snapshot` 已落库并作为 `analysis_id` 对齐事实锚点；Dashboard detail 采用 snapshot-first（未命中才 heuristic/fallback）；Review aggregate 读取同一 snapshot 事实源；Dashboard `s1` 已升级为 `marketEnvironmentMini` 极小独立块（`summary`、`environmentType`、`riskMode`、`sourceType`）。当前仍为最小实现：实时输入仍是**单一 24h quote heuristic（Binance ticker）**，并非多源融合，也非完整独立市场环境模块 UI。综合决策处的 **`market_bias_hierarchy`（市场倾向层级）** 归属**决策模块输出**，**不得**与 `MarketEnvironmentVO` 混称为同一套「市场环境模块真值」。
在与 evidence/score 的协同上，当前已不只是并排展示：已形成最小传导链 **`market environment + evidence -> score -> decision`**（其中 score 第一刀已显式读取一条“价格结构”evidence 信号）。但这仍非完整解释链完成态：evidence 仍偏最小实现、score 仍非八大评分完成、market environment 仍非多源完整模块。

2. 原始证据模块
把原始数据转成证据对象。证据分五类：价格结构证据、杠杆证据、资金证据、事件证据、风险证据。每条证据必须说明“当前是什么、相比基准变化多少、方向、强弱、可信度”。价格结构证据特别包含 5m 周期短期插针风险识别，包括长影线、pin bar、流动性扫荡。

**（当前实现边界）** 当前仓库 evidence 三字段已形成最小受控骨架（非仅 type 轻治理）：
- **生成覆盖（阶段收口）：** assemble 主链稳定产出 **四类**证据（价格结构、风险、资金、杠杆）；**事件类**当前 **无生成路径、明确后置**。整体为 **四类最小真实证据闭环**（单路行情环境链上的结构化注解 + `tm_evidence_item` 写入 + top3 读模型回放），**不等于**完整五类证据模块或完整证据解释层。
- **价格结构（当前实现）：** **日内启发式结构代理**（`priceChangePercent24h` / 方向 ε 死区，`MARKET_HEURISTIC`），**非**上列规格目标中的 5m 插针/pin bar/形态完备实现；边界须与实现描述模板一致。
- `evidence_type` 受控值：`价格结构`、`杠杆`、`资金`、`事件`、`风险`
- `direction` 受控值：`BULLISH`、`BEARISH`、`NEUTRAL`
- `source` 受控值：`SYSTEM_GENERATED`、`MARKET_HEURISTIC`、`MANUAL_INPUT`
- 三字段当前均已具备最小治理闭环：**生成侧受控默认值** + **写库前 normalize/fallback 兜底**
- **消费链进展（文档同步口径）：** 读模型仍以 **`analysis_id -> evidenceTopItems(top3)`** 为界；Dashboard 详情 **`s2` 证据明细**已对单条证据做 **三字段最小可感知消费**：`evidenceType`、`description`、`direction`（主标签）、`source`（次级弱化信息）。**请勿再按旧叙事**将 Dashboard `s2` 理解为「仅 type + description」或「direction/source 完全未消费」。
- **Review 最小结构化承接（文档同步口径）：** **`GET /api/review/aggregate`** 已挂载与 Dashboard detail **同源**的 **`evidenceTopItems`**（`EvidenceBriefVO`）；复盘页 **`review-page.js`** 已增加只读 **「结构化证据（前3条）」**，展示字段与 Dashboard **一致**。决策侧 **`evidenceSummary`**（`tm_decision_result.evidence_summary`）仍在 **`reviewReasons` / evidenceSummary 条目区**保留，为**决策摘要文本**，**不等于**结构化证据解释层，**不与** `evidenceTopItems` 互替。
- **边界保留：** 读模型仍为 **top3**；**未**扩展 evidence brief 之外的更多列；整体仍**非**完整证据解释模块交付。

3. 证据评分模块
把证据转成八大评分：趋势结构分、资金推动分、杠杆风险分、流动性质量分、情绪温度分、事件冲击分、宏观环境分、综合可信度分。杠杆风险分与流动性质量分特别强化对短期插针风险的扣分权重。

**（当前实现边界）** 当前仓库已具备 score 真值表 `tm_score_item` 与最小 detail 展示承接（`analysis_id -> scoreTopItems(top3)`，Dashboard 仅在 detail `s2` 展示「评分明细（前3条）」）。**Review 聚合**已挂载与 Dashboard **同源**的 **`scoreTopItems`**（`ScoreBriefVO`：`scoreType`、`scoreValue`）；复盘页已增加只读 **「评分明细（前3条）」**，与 Dashboard **同字段语义**。score 已进入 8/8（新增事件冲击分），并形成最小 `evidence -> score` 传导；资金推动分 = 最小真实评分扩展（Funding 真值分档 + 轻量方向冲突惩罚），不等于八大评分体系完成，且当前不硬接 decision 主路径；杠杆风险分 = 最小真实评分扩展（杠杆建议主输入 + 波动体制/振幅与 riskMode 轻量校正），不等于风险建模完成，且当前不硬接 decision 主路径；流动性质量分 = 最小真实评分扩展（`volatilityRegime` + `rangePct24h` 轻规则），不等于完整流动性模型完成，且当前不硬接 decision 主路径；宏观环境分 = 基于现有宏观白名单字段的单项增量，不等于宏观模块完成，也不改变当前 decision 主路径；事件冲击分 = 基于现有 event evidence 命中的单项负向惩罚，不等于事件系统完成，也不改变当前 decision 主路径；仍非八大评分完成态、非全量评分维度、无完整评分解释模块，不得将 top3 明细展示误判为「八大评分模块已完成」。scoreTopItems(top3) = 同源 brief 裁剪视图（按记录顺序），不代表按 scoreValue 排序，不代表评分优先级排序，不等于完整评分解释模块。

4. 综合决策模块
把八个评分整合成最终结论。固定输出七项：市场倾向强度、交易类型、置信等级、风险等级、动作优先级、结论摘要、是否值得开仓意见。
市场倾向强度采用分层级设计：强偏多、偏多、弱偏多、震荡、弱偏空、偏空、强偏空、观望，使结果更直观清晰；置信等级仍保留 high/medium/low 作为辅助层级。
必须包含多周期收敛逻辑：5m 用于插针过滤，15m/1h/4h 用于趋势与结构确认，所有周期信号一致且无重大冲突时才输出进攻型决策，否则自动降级或转观望。
多周期收敛具体算法与阈值规则完整落地：
- 规则表 tm_rule_config 新增 multi_tf_convergence 字段（JSON），定义每周期权重（4h:40%、1h:30%、15m:20%、5m:10%）与一致性阈值（趋势得分差异 ≤ 15%、方向相同且至少 3 个周期一致才视为收敛）
- 算法实现：MultiTimeframeConvergenceChecker 计算加权趋势得分，若差异 > 15% 或方向冲突则直接降级置信等级一级并强制转“观望”；冲突详情记录到 DecisionBundle 的 multi_tf_conflict 字段，供复盘使用
- 阈值支持热更新（配置/热加载侧）；复盘保存另走路径：结论写 `tm_review_result` 并**追加** `tm_rule_version_log` 审计链，**不**因保存复盘而自动修改 `tm_rule_config` 或自动升版

同时引入多 AI 角色辅助裁决机制，实现三方制衡决策：
1) GPT-5.4：最终裁决官
负责汇总三方证据、统一打分、输出偏多 / 偏空 / 观望、给出置信度、给出核心支持证据与核心反证、给出是否值得开仓的最终意见

2) Gemini 2.5 Pro：冲突复核官
负责专门找主裁决里的漏洞、对冲突证据做二次审查、判断是否需要降级为“观望”、对终裁结果给出“维持 / 调整 / 驳回”意见

3) Grok 4.20：快讯与反方挑战官
负责快速抓突发信息、做情绪和事件面补充、专门提出反方论点、在有搜索工具时补实时外部信息，含短期插针触发事件

AI 调用采用智能分级调度策略彻底解决费用失控与调用超时：
- 触发条件：仅当 data_quality_score ≥ 85 且存在显著价格/成交量/OI/Funding 变化时才发起 AI 调用，否则直接走纯规则引擎模式
- 缓存机制：Redis 缓存最近 5 分钟内相同 symbol + timeframe + 关键证据哈希的 AI 决策结果，命中即跳过外部调用
- 配额管理：全局 + 用户级 Token 预算，每日/每小时上限可配，调用前实时检查剩余配额，剩余不足时自动降级为规则模式并告警
- 智能限流：采用 Redis + Token Bucket 算法实现 per-provider 限流、per-asset 限流、并发控制
- 超时与 fallback：每路调用设置 8 秒超时，超时或失败自动 fallback 到规则引擎，并记录到调用日志
- 成本记录：每次调用记录 Token 消耗、实际费用、耗时、是否命中缓存，落库 tm_ai_call_log 供后续审计与优化

5. 执行计划模块
**（当前实现边界）** 当前仓库 `schema.sql` 中 `tm_execution_plan` 已落库窄表字段为：`plan_id`、`analysis_id`、`recommended_action`、`entry_zone`、`stop_loss`、`take_profit_rules`、`leverage_suggestion`、`position_suggestion`、`create_time`。Dashboard 侧 `DecisionResultVO` 已从该表 **同一 latest-plan 子查询行**贯通至少：**`recommendedAction`、`entryZone`、`stopLoss`、`takeProfitRules`、`leverageSuggestion`、`positionSuggestion`**（`DecisionResultMapper` 与决策行 JOIN，`rn = 1` 取每 `analysis_id` 最新一条计划；详见 `PHASE5_FIELD_TRUTH_MATRIX.md`）。Dashboard **`s4`「执行建议」**顶部另有**边界文案**，明示本 tab 仅为计划表节选 + 派生摘要，**不应**视为完整执行方案。`valid_period`、`invalid_condition` 属 **`tm_decision_result`**；`executionPlanSummary` 仅为查询层对这两列的**拼接派生（执行要点摘要）**，**不是**完整执行计划解释，也不是 `tm_execution_plan` 物理列。下文所述加仓/减仓/放弃条件及账户风险 JSON 等与 **当前窄表 / 写入链路** 的对齐关系见「字段设计要求」后的**当前实现边界**说明。
把结论转成结构化方案。输出：推荐动作、触发条件、入场区、止损区、止盈规则、加仓条件、减仓条件、放弃条件、失效条件、杠杆建议、仓位建议。
仓位建议必须量化，默认单笔风险不超过账户总资金的 1% 到 2%，支持用户个性化风险偏好配置；新增轻量级账户级整体风险评估，包括当前持仓总风险、相关性风险、最大回撤预估。
账户级整体风险量化算法完整落地：
- 总风险分 = Σ（仓位占比 × 资产波动率） + 相关性惩罚；同一方向持仓相关性 > 0.7 时额外加 20% 风险
- 最大回撤预估：基于历史 30 天波动率模拟 95% VaR，公式为 position_value × volatility × 1.65
- （**后续规划 / 当前未实现：** `schema.sql` 尚无 `account_risk_json` 列）结果实时写入 tm_execution_plan 的 account_risk_json 字段，并落地 tm_account_risk_snapshot 表供前端展示与监控使用
- 规则表支持用户自定义风险上限，单笔 ≤ 2%、总风险 ≤ 8%，超过阈值自动降仓位 50% 或转观望

6. 监控与复盘模块
盯住结论是否失效，同时保存每次结论供复盘。监控重点：证据反转、结构失效、风险升级、短期插针风险告警、多周期信号背离。
复盘记录：当时结论、证据、评分、后续走势、实际盈亏、错误类型、调整建议；同时支持轻量级历史回测能力，可基于历史 analysis_run 数据快速验证规则版本效果。
告警模块防疲劳保护机制完整落地：
- Redis 实现 alert_throttle，key 为 asset+alert_type，TTL 15 分钟，同一资产同一告警类型 15 分钟内只发一次
- tm_monitor_alert 新增 cooldown_until 字段，冷却期内告警直接标记为 suppressed
- 支持用户个性化冷却时长配置，默认 15 分钟，可通过 tm_user_config 调整
- 重复告警自动合并为一条“已持续 X 分钟”通知，避免轰炸

5m 高频插针检测性能与资源消耗完整优化：
- 数据基础层采用 Redis + Caffeine 双层缓存预计算 5m K 线指标，包括长影线比率、wick 幅度、成交量突增，更新时仅增量计算
- 调度引擎对 5m 任务使用异步线程池，最大并发 10，同一资产 5m 分析强制 10 秒防抖
- 资源监控：AnalysisSchedulerService 实时统计 CPU/内存占用，超过 70% 时自动降频或跳过非核心资产的 5m 分析
- 性能测试阈值记录到 tm_data_source_health，确保并发 20+ 资产时延迟 < 2 秒

用户个性化配置已在规则层与前端完整支持落地：
- 新增 tm_user_config 表，包括 user_id、risk_preference、ai_model_preference、notify_channels、cooldown_minutes 等字段
- RuleConfigService 启动时加载用户配置并与全局规则合并，优先级为用户 > 全局
- 前端新增配置接口，支持实时保存与热生效
- 决策与计划模块在生成结果前读取用户配置，自动调整仓位上限、AI 偏好、通知渠道

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

所有写接口必须支持幂等控制；告警接口预留通知下发能力；决策接口组新增多 AI 角色调用子接口，支持 GPT、Gemini、Grok API 智能调度、fallback 与汇总。

八、前端页面建议

V1 前端只做三页：
1. 总览页：**当前市场环境**（规格项；独立区块的落地边界见「五、（二）、1」**（当前实现边界）**）、数据质量分、重点资产列表、综合结论、风险等级、动作建议
2. 证据与评分页：原始证据、支持/反对证据、八大评分、数据质量说明、多 AI 角色裁决过程、短期插针风险标记、多周期收敛状态
3. 策略与监控页：执行计划、结构带、入场区、止损区、止盈方案、监控状态、告警、近几次复盘结果、账户整体风险概览

页面中必须预留规则/版本信息展示入口，至少展示当前规则版本、最近发布时间、多周期阈值摘要、风险阈值摘要、AI 调度策略摘要、用户个性化覆盖项摘要。

轮询策略：
- 5m 周期：15 秒
- 15m 周期：30 秒
- 1h 周期：60 秒
- 4h 周期：120 秒

九、V1 开发顺序

1. 先定五个核心对象
2. 做基础数据接口
3. 做全量证据接口
4. 做全量评分接口
5. 做综合决策接口，优先实现多 AI 角色智能调度、fallback、多周期收敛、缓存、配额检查、限流与用户配置加载
6. 做执行计划接口
7. 做监控与复盘接口
8. 实现分析调度引擎与幂等控制
9. 最后做页面

十、V1 的执行计划模板

固定四类模板：
1. 趋势突破型
2. 回踩承接型
3. 事件观望型
4. 高风险反向挤仓型

模板通过“评分组合 → 模板匹配”规则自动切换，多 AI 角色裁决结果与多周期收敛结果作为最终模板切换依据。

十一、V1 的增强机制

1. 反对证据列表，必须同时展示支持与反对证据
2. 数据质量折扣，自动降低置信度，低于 70 分触发熔断
3. 资产状态机：observing、candidate、waiting_trigger、triggered、high_risk、invalidated、cooling
4. 熔断机制：data_quality_score < 70 时强制切到暂不交易或事件观望
5. 规则反馈闭环（目标能力）：复盘结果可经治理流程映射为规则调整并产生新版本；**当前阶段实现**为每次复盘保存**仅追加** `tm_rule_version_log`（`REVIEW_FEEDBACK_SAVED`，append-only 审计），**不**自动改规则配置、**不**自动发版
6. 多 AI 角色辅助裁决机制，形成实时情报 → 冲突复核 → 最终裁决的三层制衡，包含智能分级触发、Redis 缓存、Token 预算配额管理、Token Bucket 限流、8 秒超时 fallback、成本实时记录等完整策略
7. 短期插针风险专属检测机制，5m 周期证据与评分权重直接影响风险等级与计划模板切换
8. 决策结果分层级机制，市场倾向强度采用强/中/弱层级呈现，使偏多/偏空/观望 + 置信度输出更直观清晰
9. 多周期收敛机制，高周期趋势 + 中周期结构 + 低周期时机必须一致，否则自动降级
10. 账户级整体风险控制机制，量化单笔与总仓位风险，实时评估组合相关性与最大回撤
11. 异常处理与降级容错机制，AI 调用失败、数据异常、多周期背离时自动切换至纯规则引擎模式并记录
12. 告警防疲劳保护机制，Redis throttle + 冷却期 + 合并通知
13. 5m 插针检测性能优化机制，双层缓存 + 异步防抖 + 资源监控
14. 用户个性化配置机制，tm_user_config 表 + RuleConfigService 动态加载

十二、数据库表设计（V1）

数据库设计原则：
- 业务结果落库，原始高频数据不全量落库
- 核心字段固定，扩展内容放 JSON
- 只用一个业务库：trade_model_v1
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
2. tm_analysis_run
3. tm_analysis_input_snapshot
4. tm_evidence_item
5. tm_score_item
6. tm_decision_result
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
16. tm_ai_call_log
17. tm_account_risk_snapshot
18. tm_user_config

字段设计要求：
1. 所有核心业务表必须统一具备基础审计字段：created_by、updated_by、created_at、updated_at、is_deleted、version_no
2. 所有核心链路表必须统一具备链路字段：analysis_id、trace_id、rule_version
3. tm_decision_result 增加 ai_role_results JSON 字段，记录三 AI 角色原始意见
4. **（规范目标 / 当前未落地）** tm_execution_plan 增加 account_risk_json、plan_mode、execution_disclaimer 字段 —— **当前 `schema.sql` 尚未包含上述列**，属后续迁移与实现范围；与已实现窄表字段的区分见下款「当前实现边界」。
5. tm_monitor_alert 增加 cooldown_until、suppress_reason 字段
6. tm_review_result：**当前已实现**（与 `schema.sql` 一致）列：`id`、`analysis_id`、`error_type`、`actual_outcome`、`adjustment_suggestion`、`create_time`、`update_time`。**未实现 / 后续规划**（不作为当前表结构约束）：`error_stage`、`review_tag`、`improvement_suggestion`
7. 所有明细子表与 tm_analysis_run 建立明确关联；写入主表 + 证据 + 评分 + 决策 + 计划必须同一事务

**当前实现边界（执行计划 ↔ Dashboard，避免与规范目标混淆）**

1. **`tm_execution_plan`（已实现窄表）：** `plan_id`、`analysis_id`、`recommended_action`、`entry_zone`、`stop_loss`、`take_profit_rules`、`leverage_suggestion`、`position_suggestion`、`create_time`。
2. **Dashboard API（`DecisionResultVO`）：** 当前从 **`tm_execution_plan`**（**同一 latest-plan 行**）贯通到前端的计划窄表字段至少包括：**`recommendedAction`、`entryZone`、`stopLoss`、`takeProfitRules`、`leverageSuggestion`、`positionSuggestion`**（`DecisionResultMapper`：`LEFT JOIN` **派生表**内含 `ROW_NUMBER() ... PARTITION BY analysis_id`，**`rn = 1`** 对应「该 `analysis_id` 最新一条计划」）。
3. **`valid_period`、`invalid_condition`：** 落在 **`tm_decision_result`**，由同一查询映射到 `DecisionResultVO`；**不是**执行计划窄表列。
4. **`executionPlanSummary`：** 仅在 Mapper 查询中由 `valid_period` 与 `invalid_condition` **拼接派生**，**不是** `tm_execution_plan` 或决策表上的物理列；语义上仅是 **有效期与失效条件的派生摘要（「执行要点」文案）**，**不得**误读为完整执行计划解释或替代窄表字段。
   - 补充边界：`validPeriod` / `invalidCondition` 已完成真实化第一刀（不再按旧占位常量口径理解），但这**不等于**整套 execution **深层解释模块**已完成。
5. **`take_profit_rules` → Dashboard：** 已完成 **Dashboard s4 单点贯通**：与 **`entry_zone` / `stop_loss`** 同源 **latest-plan** 子查询；**不与** `executionPlanSummary` 拼接；前端以 **独立多行说明块**展示（细则见 **`PHASE5_FIELD_TRUTH_MATRIX.md`** — **`take_profit_rules` — Dashboard display contract**）。
6. **Dashboard `s4` 顶部边界文案：** `dashboard.html` 在「执行建议」标题下另有全宽 muted 提示，约束用户勿将本 tab 当作完整执行方案（计划表节选 + 派生摘要）。
7. **Review Plan：** `ReviewAggregateVO.ReviewPlanSummary` / 复盘页 Plan 摘要仍承接完整窄表 kv；与 Dashboard **在窄表字段层面已对齐**，复盘侧仍可保留 **更完整的编排与上下文**；**不等于** Dashboard 已交付完整「执行解释模块」。
8. **`plan_mode`、`execution_disclaimer`、`account_risk_json`、`template_type`（见下文「十四、核心返回对象标准」执行计划对象）及更大颗粒模板化 / 账户风险 / 监控一致化：** 仍为 **后续规划或未收口**，以 **`schema.sql` 与落地代码为准**；**execution 深层解释层整体仍未完成**。

十三、后端接口返回 JSON 标准

所有接口统一返回结构：
code、msg、request_id、server_time、data

统一规范：
1. 时间格式统一为 ISO 8601 带时区
2. 空数组返回 [] 而非 null
3. 数值字段直接返回数字
4. 枚举值固定
5. 所有主键 ID 用字符串
6. 写接口支持幂等
7. 每个返回对象必须携带 analysis_id、rule_version、trace_id 等元信息
8. 决策接口新增 ai_roles 字段返回三 AI 角色完整过程
9. 所有列表接口支持分页参数与排序字段
10. 所有写接口应返回本次是否命中幂等、是否为重试执行、是否触发降级

十四、核心返回对象标准

1. 市场环境对象
2. 单条证据对象
3. 单项评分对象
4. 综合决策对象
5. 执行计划对象
6. 监控状态对象
7. 复盘对象
8. 聚合分析对象（AssetAnalysisVO）

其中综合决策对象必须新增：
- market_bias_hierarchy：值固定为 STRONG_BULLISH、BULLISH、WEAK_BULLISH、RANGE、WEAK_BEARISH、BEARISH、STRONG_BEARISH、WAIT
- multi_tf_convergence：记录多周期收敛状态
- hard_block_reasons：记录熔断、事件窗口、多周期冲突、插针风险、账户总风险等硬阻断原因
- ai_roles：记录三 AI 角色意见摘要、是否命中缓存、是否 fallback

执行计划对象必须新增：
- plan_mode：ADVISORY / SEMI_STRUCTURED
- execution_disclaimer：固定风险提示文案
- account_risk_json：账户级风险摘要
- template_type：趋势突破型 / 回踩承接型 / 事件观望型 / 高风险反向挤仓型

> **说明（当前仓库）：** 上述四项及 `tm_execution_plan` 的 `account_risk_json` / `plan_mode` / `execution_disclaimer` 等**尚未**在 `schema.sql` 与持久化链路中实现，属**目标契约**；当前已落库与 Dashboard 贯通范围以「字段设计要求」章节**当前实现边界**及 `schema.sql` 为准。

十五、数据质量分与折扣机制

采用 100 分制，默认 100 分，按问题扣减，例如 ETF 缺失扣 15、新闻延迟扣 10。
data_quality_score < 85 时，置信等级降一档；data_quality_score < 70 时，触发熔断。
所有扣分项写入 tm_data_source_health 和分析结果 ext_info。
data_quality_score 仅表示数据源质量，不等于结论可信度。
综合可信度分表示当前结论本身的可信程度，两者必须分开计算、分开展示。

**（当前实现边界）** 本仓库 Dashboard 决策读模型贯通 **run 级** `data_quality_score` 时，**主真值源为 `tm_analysis_run.data_quality_score`**（按 `analysis_id` 与决策行对齐）；**不在 `tm_decision_result` 落列**。`tm_push_snapshot.data_quality_score_snapshot`、`tm_push_recheck_log.current_data_quality_score` 属 Push/二次校验链路字段，**不与 run 级分混同**。**`tm_data_source_health`**：若当前 `schema.sql` 未落地该表或写入链路未实现，则扣分明细与健康矩阵仍视为**规划/未实现**，以 schema 与代码为准。

**（契约冻结索引）** 市场环境快照、`tm_analysis_run.data_quality_score`、Push/Recheck 质量字段与 `systemHealth` 之**三线边界**及下一阶段**允许入口 / 反模式**，以 `PHASE10_DASHBOARD_FREEZE_INDEX.md` 章节「**市场环境快照 + run级 DQ 联合契约（冻结）**」为准。**OI（未平仓）最小接入**单独契约见根目录 **`OI_MINIMAL_ACCESS_CONTRACT.md`**。

**（run 级 DQ 实现叙述索引）** 当前仓库内 run 表分数的**真实生成规则**（三档条数逻辑、非矩阵非规格熔断接线）见同文件小节「**run级 DQ 当前实现现状（代码级，防误读）**」及 `AnalysisAssemblerServiceImpl#estimateDataQualityScore` JavaDoc。

十六、资产状态机与监控触发规则

状态固定为：
observing、candidate、waiting_trigger、triggered、high_risk、invalidated、cooling

约束要求：
1. 状态只能由 MonitorService 独占写入
2. 同一 symbol + timeframe 同一监控周期内不允许多次翻转
3. 必须有防抖逻辑
4. 每次状态迁移必须写入迁移原因、触发证据、触发时间、触发规则版本
5. 高风险、失效、冷却状态必须能追溯到具体阻断条件和监控证据

十七、分析调度、幂等与链路日志

调度机制：定时调度 + 事件触发 + 外部调用触发。
并发控制：Redis 分布式锁；同一 symbol + timeframe + analysis_time + 版本只允许一条有效记录。
全程带 traceId；写接口带 request_id；决策环节必须记录三 AI 角色调用 trace、成本与 fallback 事件，同时在 tm_ai_call_log 中持久化成本数据。

**（当前实现边界）** `GET /api/dashboard/summary` 的 `systemHealth` 中，`schedulerStatus` 表示**持仓同步活动**的代理语义，**不等同**全站分析调度总览；`cpuUsage`、`memoryUsage` 仅为当前实现下的技术指标（负载启发式、JVM 堆占比），**不**等同于完整运维监控口径。

幂等规则：
1. 幂等键建议为 symbol + timeframe + analysis_time + trigger_type + rule_version
2. 相同幂等键重复请求不得重复落库
3. 幂等命中时允许直接返回已有 analysis_id 与结果摘要
4. 幂等命中、分布式锁获取失败、降级执行都必须写链路日志

十八、Java 实体类与 VO 命名

（一）命名规则
- 数据库实体类：DO 后缀
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
BaseAnalysisReq、BuildEvidenceReq、BuildScoreReq、BuildDecisionReq、GeneratePlanReq、UpdateMonitorReq、SaveAnalysisReq、SavePlanReq、WriteReviewResultReq、QueryHistoryReq、RetryAnalysisReq、UserConfigReq

（五）核心返回对象
MarketEnvironmentVO、EvidenceItemVO、ScoreItemVO、DecisionBundleVO、StructureBandVO、ExecutionPlanVO、MonitorStateVO、ReviewStateVO、AssetAnalysisVO、ReviewHistoryItemVO、RuleVersionVO、AlertNoticeVO

（六）内部 DTO
AnalysisContextDTO、EvidenceBundleDTO、ScoreBundleDTO、DecisionBundleDTO、PlanContextDTO、MonitorContextDTO、RuleFeedbackDTO、AiRoleDecisionDTO

（七）枚举类
MarketBiasEnum、TradeTypeEnum、ConfidenceLevelEnum、RiskLevelEnum、ActionPriorityEnum、EvidenceTypeEnum、EvidenceDirectionEnum、StrengthLevelEnum、ScoreDirectionEnum、AlertTypeEnum、AlertLevelEnum、DataSourceStatusEnum、AssetStateEnum、TriggerLogicTypeEnum、PlanStatusEnum、AnalysisStatusEnum、AiRoleEnum、ReviewErrorTypeEnum、ReviewErrorStageEnum、PlanModeEnum

其中：
- MarketBiasEnum：STRONG_BULLISH、BULLISH、WEAK_BULLISH、RANGE、WEAK_BEARISH、BEARISH、STRONG_BEARISH、WAIT
- AiRoleEnum：GPT_FINAL、GEMINI_REVIEW、GROK_CHALLENGE
- PlanModeEnum：ADVISORY、SEMI_STRUCTURED

（八）Controller
MarketController、SystemController、EvidenceController、ScoreController、DecisionController、PlanController、MonitorController、ReviewController、RuleController、UserConfigController

（九）Service
MarketService、SystemHealthService、EvidenceService、ScoreService、DecisionService、PlanService、MonitorService、ReviewService、AssetStateService、RuleConfigService、AnalysisSchedulerService、AiDecisionOrchestratorService、UserConfigService

（十）Mapper
AssetMapper、AnalysisRunMapper、AnalysisInputSnapshotMapper、EvidenceItemMapper、ScoreItemMapper、DecisionResultMapper、ExecutionPlanMapper、MonitorAlertMapper、ReviewResultMapper、DataSourceHealthMapper、AssetStateMapper、RuleConfigMapper、RuleVersionLogMapper、AiCallLogMapper、AccountRiskSnapshotMapper、UserConfigMapper

（十一）统一返回壳
ApiResponse，提供 success 和 fail 静态方法。

十九、项目实施建议

第一轮：完成主链路空跑 + 规则配置化雏形 + traceId 全链路日志 + 多 AI 角色智能调度框架，含缓存、配额、限流、多周期收敛算法
第二轮：接数据库，打通落库、事务、幂等、状态机、用户配置表
第三轮：接入真实数据源，开启数据质量打分、熔断、告警、5m 插针检测、多周期收敛验证、账户风险量化
第四轮：接入调度与监控任务、账户级风险评估、防疲劳告警
第五轮：做前端三页与个性化配置页面

V1 的关键词：稳、清晰、可解释、可追踪、可迭代。
主链路、状态机、规则层、幂等控制、数据质量折扣、复盘反馈、多 AI 角色制衡、短期插针风险检测、决策结果分层级、多周期收敛机制、账户级风险控制、AI 调用成本控制、告警防疲劳、5m 性能优化、用户个性化配置从 V1 就必须打稳。

二十、V1 当前约束与落地原则

1. 规则引擎是主骨架，AI 只做增强，不得成为单点真相源
2. 下列结果必须由规则引擎直接产出，不允许仅依赖 AI：
   - data_quality_score
   - 熔断判断
   - 多周期收敛判断
   - 风险等级下限
   - 仓位上限
   - 状态机流转
3. AI 可参与的范围限定为：
   - 证据解释与摘要
   - 支持/反对证据归纳
   - 冲突复核
   - 计划模板微调
   - 新闻/事件语义理解
4. AI 输出不得直接覆盖硬规则结论，只能在硬规则允许范围内做增强修正
5. 执行计划属于结构化执行建议，不属于保证式信号，不承诺胜率，不直接接交易所下单
6. 外部执行端若消费该计划，必须自行完成二次确认与风控校验

二十一、评分、决策、执行、复盘的统一口径

（一）八大评分统一口径

1. 八大评分统一采用 0 到 100 分制
2. 50 为中性
3. 70 以上为偏强
4. 30 以下为偏弱或偏风险
5. 每项评分由三部分组成：规则基础分 + 证据加减分 + 数据质量折扣
6. 缺失数据默认不加分，但必须触发综合可信度扣分
7. 综合可信度分不等于 data_quality_score，二者必须严格区分

（二）综合决策优先级矩阵

冲突条件同时出现时，优先级必须固定，按以下顺序执行：
1. 熔断优先级最高：data_quality_score < 70 时直接输出“暂不交易/事件观望”
2. 重大事件窗口次高：宏观/ETF/突发事件窗口内自动降级至少一级
3. 多周期冲突优先：方向冲突或差异超阈值时强制观望
4. 5m 插针高风险优先：禁止输出进攻型模板，只能保守模板或放弃
5. 账户总风险限制优先：超总风险阈值时降仓位或不新开仓
6. AI 角色结论只作为增强修正，不得覆盖前述硬规则

（三）执行计划边界

1. V1 输出的是结构化执行建议，不是直接交易信号
2. 所有计划必须说明适用前提、失效条件、风险边界、仓位约束
3. 计划模板切换必须受到熔断、事件窗口、多周期冲突、插针风险、账户总风险共同约束
4. 计划对象必须明确标识当前是 advisory 模式还是 semi_structured 模式
5. 所有计划结果必须携带固定风险提示文案

（四）复盘归因分类标准

复盘错误类型固定为：
- DATA_ISSUE：数据缺失/延迟/异常
- RULE_TOO_LOOSE：规则过松
- RULE_TOO_STRICT：规则过严
- TF_CONFLICT_MISJUDGED：多周期冲突误判
- EVENT_RISK_UNDERWEIGHT：事件权重低估
- LEVERAGE_RISK_UNDERWEIGHT：杠杆风险低估
- LIQUIDITY_TRAP_MISSED：插针/流动性扫荡漏判
- AI_OVERRULE_BIAS：AI 辅助结论偏移
- PLAN_EXECUTION_MISMATCH：计划和市场演化不匹配
- UNKNOWN：未知原因

复盘错误阶段固定为：
- JUDGEMENT_ERROR：方向判断错
- TIMING_ERROR：入场时机错
- POSITION_ERROR：仓位管理错
- EXECUTION_ERROR：执行偏差
- MONITORING_ERROR：监控失效
- REVIEW_MISSING：复盘缺失

复盘必须至少输出：
1. 判断是否正确
2. 时机是否正确
3. 仓位是否合理
4. 执行是否偏离
5. 哪条规则需要调整
6. 是否生成下一版本规则建议

（五）adjustment_suggestion 填写约定

以下仅作复盘时填写「调整建议」的**半结构化模板与协作习惯**说明，便于读写一致与后续治理沟通；**不**作为数据库列格式契约、接口入参契约或服务端校验规则。

可选在 `adjustment_suggestion` 正文中写明本笔复盘对应的错误阶段，词汇以上文「（四）复盘错误阶段固定为」所列为准；该写法仍属人工协作习惯，**不**新增库表列义务、接口字段契约或服务端校验义务（与段首「仅作填写约定」口径一致）。

建议按四段书写（可用空行或短标题分段，缺一也可，以事实清楚为准）：
1. **建议动作**：拟采取的调整方向（例如收紧/放宽哪类判断、加强哪类监控、计划侧如何收口等）。
2. **目标对象**：调整作用于什么范围（规则族或业务含义层面的对象即可，无需绑定具体表字段写法）。
3. **与 error_type 对齐说明**：说明本建议如何呼应或解释当前选择的 `error_type`；若仍为 `UNKNOWN`，简述保留理由或待核实点。
4. **验收/执行节奏**（可选）：期望的验证方式、观察窗口或跟进节奏（不等于发布承诺）。

**示例（简短）**：
- **建议动作**：多周期冲突时一律先降为观望，不再输出进攻型模板。
- **目标对象**：多周期收敛相关的规则权重与一致性判定（由规则负责人评估后落地）。
- **与 error_type 对齐说明**：对应 `TF_CONFLICT_MISJUDGED`，误判来自「不一致仍被当成可交易收敛」。
- **验收/执行节奏**（可选）：观察两周内降级为观望的比例与误伤情况，再决定是否固化参数。

二十二、开发验收标准

（一）第一轮验收标准
1. 能创建 analysis_run
2. 能生成输入快照
3. 能产出 mock evidence、score、decision、plan
4. 全链路 traceId 可贯通
5. 幂等键生效，重复请求不重复落库

（二）第二轮验收标准
1. 主表、证据、评分、决策、计划事务一致
2. 规则表可热加载
3. 用户配置覆盖全局规则生效
4. 状态机只能由 MonitorService 写入
5. 审计字段、trace_id、rule_version、analysis_id 落库完整

（三）第三轮验收标准
1. 接入真实行情与事件数据
2. data_quality_score 正常扣分
3. 熔断机制生效
4. 5m 插针识别可稳定触发
5. 多周期收敛可验证通过路径与失败路径

（四）第四轮验收标准
1. 告警冷却、防抖、合并通知生效
2. 账户级风险评估写库成功
3. AI 调用预算、限流、fallback、生效率可观测
4. tm_ai_call_log 可完整查看成本、耗时、缓存命中、fallback 记录

（五）第五轮验收标准
1. 前端三页可串通主链路
2. 单个资产可完整查看“证据 → 评分 → 决策 → 计划 → 监控 → 复盘”
3. 页面可查看当前规则版本与关键阈值摘要
4. 用户配置修改后可实时影响决策与计划输出

二十三、结论性要求

1. V1 必须先把“证据可见、规则可控、决策可解释、计划可结构化、过程可追踪、结果可复盘”打稳
2. V1 不追求自动交易闭环，不追求资产全覆盖，不追求复杂策略全接入
3. 所有开发实现必须优先保证稳定性、一致性、可追踪性、幂等性、可回滚性
4. 所有规则必须配置化，所有关键判断必须可审计，所有降级必须留痕
5. 所有模块最终都要服务于同一件事：让每一次交易判断都有证据、有约束、有记录、有反馈、有迭代依据
6. CI 当前以 JDK 17 为唯一受支持执行环境，发布门禁统一执行 `mvn verify -Pci`
7. 工程验证口径统一为：`mvn compile`（快速编译检查）、`mvn test`（本地常规回归）、`mvn verify -Pci`（发布门禁）
