# AI Conflict, Confused, Push Recheck, and Review Source Snapshot

> Canonical repository text snapshot mechanically extracted from the original Word product plan.
> The wording is source content, not a completion claim. Tables are appended after paragraph content.

## Source Provenance

- Source ID: `PS-AI-CONFLICT-RECHECK-REVIEW`
- Original local path at freeze: `/Users/xuchao/Documents/复盘与 AI 冲突处理统一落地方案.docx`
- Original modified date: `2026-04-15`
- Original SHA-256: `d2cc0762bff67de46a31d99b5e9817b26de425382db51118be2ad1d8bd921352`
- Extraction mode: non-empty paragraphs in document order, followed by source tables

## Extracted Product Plan

这个解决方案的可行性，二十五至二十六、环境突变适应、推送二次确认、遗漏机会复盘与 AI 冲突处理统一落地方案（开发直用版）

为确保 V1 在仅完成第一、二阶段时，已经具备较强的实战稳定性、执行一致性、环境突变适应能力与 AI 协同可控性，现将以下机制合并为一套统一可落地方案：

1. Confused State / Logic Breaker（冲突熔断状态）

2. Push Recheck Rule（推送后二次确认规则）

3. Missed-but-valid Opportunity Logging（未入场但逻辑正确机会记录机制）

4. Hot Reset Mechanism（极端环境紧急重置机制）

5. AI Conflict Handling & Direction Output Rules（AI 冲突处理与方向输出规则）

本方案必须直接进入正式开发范围，不得只做页面展示或文案补充。

实现时必须覆盖：规则、状态、数据结构、接口、落库、前端、推送、审计、复盘、配置十个层面。

所有阈值必须配置化；所有关键动作必须落库；所有方向输出必须可追踪、可解释、可回放。

⸻

一、设计目标

本方案解决以下五类实战问题：

1. 证据冲突升高时，系统不应硬给方向

2. 推送发出后，机会可能已过期，必须重新确认

3. 未执行但正确的机会必须纳入复盘，防止幸存者偏差

4. 极端环境突变时，系统必须能跳过慢切换逻辑，快速重置状态

5. AI 意见打架时，系统不能永远不给方向，也不能让 AI 互相否决到瘫痪

一句话原则：

冲突高时暂停方向，点击推送时重新核验，没做但对的机会也要记录，极端行情必须允许强制重算，AI 分歧只能降级输出层级，不能无限阻断系统。

⸻

二、统一状态机与方向输出模型

1. 资产状态机

V1 统一使用以下状态：

• observing

• candidate

• waiting_trigger

• triggered

• high_risk

• invalidated

• cooling

• confused

2. 状态含义

observing

信息不足、质量不足或条件未成熟，仅观察。

candidate

方向开始形成，但尚未到可执行级别。

waiting_trigger

逻辑基本成立，等待价格或结构触发。

triggered

条件满足，可进入标准执行逻辑。

high_risk

风险急剧上升，不建议正常参与。

invalidated

原计划逻辑失效。

cooling

失效或高风险后冷却期，避免频繁反复参与。

confused

方向冲突、主驱动不稳定、执行环境恶化、存在流动性陷阱风险，暂停方向性执行建议。

3. 方向输出不是二元制，而是四级制

所有方向输出统一分为四级，不允许只用“给方向 / 不给方向”两种粗糙逻辑。

Level 1：一致

• 规则层方向清晰

• AI 基本一致

• 可输出明确方向

• 可输出确认型计划

Level 2：轻微分歧

• 规则层有方向

• AI 存在反方意见

• 保留方向

• 置信度降一级

• 计划从确认型降为缩减型或预备型

Level 3：显著分歧

• 规则层有方向，但冲突明显

• 不直接给确认型执行建议

• 只输出 candidate / waiting_trigger / 预备型计划

• 必须给出恢复条件

Level 4：极端分歧

• 主驱动不清晰

• 原因层快速打架

• 结构诱导性增强

• 进入 confused

• 禁止方向性执行建议

⸻

三、AI 职责边界与冲突处理总原则

1. AI 不是投票制，而是分层职责制

三类 AI 的职责必须固定：

GPT_FINAL

最终裁决官。

负责在规则层给出的基础方向与证据之上，输出最终方向、置信度、摘要与计划建议层级。

GEMINI_REVIEW

冲突复核官。

负责找漏洞、指出证据不足、识别需要降级的地方。

GROK_CHALLENGE

反方挑战官。

负责补充快讯、反证、短时事件面与微观风险提示。

2. AI 不得越权替代规则引擎

规则引擎负责：

• 基础方向

• 原因结果匹配度

• 状态机切换

• Hot Reset 触发

• Push Recheck 结果

• 执行可行性

• 风险限制

• 是否进入 confused

AI 负责：

• 解释

• 复核

• 反证

• 摘要

• 降级建议

• 风险提示

3. AI 分歧的核心处理原则

原则一

AI 有分歧，不自动等于观望。

原则二

AI 分歧优先影响：

• 置信度

• 风险等级

• 计划模式

• 是否进入 confused

而不是直接否掉基础方向。

原则三

只有当规则层冲突已高，且 AI 分歧进一步放大冲突时，才允许进入 confused。

⸻

四、Confused State / Logic Breaker 机制

1. 机制目标

当系统发现当前市场不是普通观望，而是冲突持续升高、主驱动不稳定、价格行为具有明显诱导性、执行环境快速恶化时，必须进入 confused，暂停方向性执行建议。

2. confused 的触发不是单点，而是联合判定

新增综合指标：

confused_score

建议公式：

confused_score = 0.30 * driver_conflict_score + 0.20 * execution_instability_score + 0.20 * microstructure_trap_score + 0.15 * cause_effect_divergence_score + 0.15 * ai_conflict_score

2.1 子指标定义

driver_conflict_score

衡量原因层内部冲突强度。

来源包括：

• 主驱动连续切换

• 资金偏多但事件偏空

• 宏观偏暖但杠杆高危

• 驱动方向分裂

execution_instability_score

衡量当前计划是否可稳定执行。

来源包括：

• 滑点升高

• 深度变薄

• 入场区间被频繁穿越

• 推送后位置迅速漂移

microstructure_trap_score

衡量流动性陷阱和诱导性。

来源包括：

• 长影线

• 快速拉升回吐

• 扫流动性后收回

• 盘口失真

cause_effect_divergence_score

衡量原因层与结果层背离程度。

ai_conflict_score

衡量 GPT、Gemini、Grok 在方向、风险、计划模式上的分歧强度。

3. 触发阈值建议

• confused_score ≥ 70：进入 confused

• confused_score ≥ 85：进入 confused 且禁止方向性推送

• confused_score < 55 且连续 2 个周期回落：允许退出 confused

4. AI 冲突如何计入 ai_conflict_score

4.1 方向分歧

• 三者方向一致：0

• 一方反对但两方一致：20

• 两方明显对冲：40

• GPT 自身低信心且另外两方冲突明显：60

4.2 风险分歧

• 风险等级差 1 级：10

• 风险等级差 2 级及以上：20

4.3 计划分歧

• 确认型 vs 预备型：10

• 确认型 vs 观望/禁止执行：25

最终：

ai_conflict_score = min(100, direction_conflict + risk_conflict + plan_conflict)

5. 进入 confused 后的系统行为

1. 状态切为 confused

2. 禁止输出标准看多 / 看空执行计划

3. 所有 triggered 自动降级为：

• observing

• candidate

• waiting_trigger

• warning_only

具体由规则引擎决定，不允许继续维持 triggered

4. 所有移动端方向性推送停止，仅允许：

• 冲突升高提醒

• 流动性陷阱警告

• 暂停执行提示

5. 页面高亮显示：

• 当前主导方向不稳定

• 冲突来源

• 恢复正常判断的条件

6. 退出 confused 条件

必须同时满足：

1. confused_score 连续 2 个周期 < 55

2. dominant_driver_type 稳定

3. Cause-Effect Alignment 至少回到 WEAKLY_ALIGNED

4. execution_instability_score 回落

5. microstructure_trap_score 回落

退出后只允许进入 observing 或 candidate，不允许直接回到 triggered。

⸻

五、AI 冲突处理与方向输出规则

1. 规则层先产出基础方向

规则引擎必须先输出：

• rule_market_bias

• rule_confidence_level

• rule_risk_level

• rule_plan_mode

• rule_can_execute

AI 在此基础上增强，不允许跳过规则层直接主导。

2. GPT/Gemini/Grok 的合成规则

输入

• 规则层基础输出

• 原因层、结果层、风险层证据

• 数据质量

• Push Recheck 状态

• Confused State 状态

输出

• ai_final_bias

• ai_confidence_adjustment

• ai_risk_adjustment

• ai_plan_mode_adjustment

• ai_challenge_summary

• ai_review_summary

3. AI 冲突四级处理逻辑

Level 1：一致

条件：

• GPT 与规则层一致

• Gemini 仅轻微修正

• Grok 无重大反证

输出：

• 保留方向

• 正常置信度

• 确认型计划可用

Level 2：轻微分歧

条件：

• GPT 给方向

• Gemini/Grok 提出有效反证，但未达到高冲突阈值

输出：

• 保留方向

• 置信度降一级

• 风险等级升一级或保持

• 计划从确认型降为缩减型/预备型

Level 3：显著分歧

条件：

• GPT 给方向

• Gemini 明确认为证据不足或需要降级

• Grok 存在显著反方挑战

• 但 confused_score 未达极端阈值

输出：

• 不取消方向

• 仅输出 candidate / waiting_trigger

• 只允许预备型计划

• 必须给恢复条件

Level 4：极端分歧

条件：

• GPT 自身低信心

• Gemini 与 Grok 均给出强反证

• 规则层冲突高

• confused_score 达阈值

输出：

• 进入 confused

• 禁止方向性执行建议

• 只输出冲突来源与恢复条件

4. 绝对禁止规则

1. 不允许因为任一 AI 单独反对，就直接无限观望

2. 不允许把 AI 做成三票投票制

3. 不允许 Gemini 或 Grok 直接覆盖规则状态机

4. 不允许 AI 在规则层可执行时无理由阻断输出

5. 不允许 AI 永久阻断系统，只能让系统：

• 降置信

• 升风险

• 降计划级别

• 进入 confused

⸻

六、Push Recheck Rule 机制

1. 机制目标

推送打开时，必须重新确认当前是否仍可执行。

推送不是静态消息，而是“带有效期的执行建议”。

2. 推送分类

• PREPARE_PUSH（预备型机会）

• CONFIRM_PUSH（确认型入场）

• WARNING_PUSH（风险或失效提醒）

仅前两类需要 Recheck。

3. 推送发出时必须保存快照

新增表：tm_push_snapshot

字段如下：

• push_id

• analysis_id

• symbol

• timeframe

• push_type

• push_status

• push_create_time

• rule_version

• trigger_price

• entry_zone_json

• stop_zone_json

• invalidation_condition_json

• plan_mode_snapshot

• cause_effect_alignment_snapshot

• execution_feasibility_snapshot

• data_quality_score_snapshot

• confused_score_snapshot

• account_risk_snapshot_id

• expires_at

4. 用户点击后二次确认接口

POST /api/push/recheck/{pushId}

由 PushRecheckService 执行。

5. 最小校验集

点击推送时必须校验：

1. 当前价格是否仍在可接受入场区

2. 当前执行滑点是否仍达标

3. 数据质量是否达标

4. 账户级风险是否允许

5. 当前状态是否为：

• invalidated

• cooling

• high_risk

• confused

6. 原因层是否仍成立

7. 推送是否过期

6. Recheck 返回状态

• VALID_EXECUTABLE

• VALID_WAITING

• DRIFTED

• INVALIDATED

• RISK_BLOCKED

• CONFUSED_BLOCKED

• EXPIRED

7. 页面行为

VALID_EXECUTABLE

正常展示计划，可执行。

VALID_WAITING

逻辑未失效，但需等待触发。

DRIFTED

机会已偏离原高质量区间，不建议按原计划执行。

INVALIDATED

逻辑失效，显示失效原因。

RISK_BLOCKED

账户层或系统层风险阻断。

CONFUSED_BLOCKED

当前进入冲突状态，暂停执行。

EXPIRED

推送已过期，仅供回看。

8. 防重复与防轰炸

1. 同一 symbol + push_type + entry_zone_hash 在 cooldown 内不得重复推送

2. 持续有效机会合并提醒

3. 短寿命推送必须进入统计

4. 二次确认失败必须记录原因

新增表：tm_push_recheck_log

字段：

• log_id

• push_id

• recheck_time

• recheck_status

• current_price

• price_drift_ratio

• current_slippage_estimation

• current_data_quality_score

• current_confused_score

• current_account_risk_allowed

• fail_reason_json

⸻

七、Missed-but-valid Opportunity Logging 机制

1. 机制目标

复盘不能只看“做过的单”，还必须记录：

• 没做但逻辑正确的机会

• 推送发出但未成交的机会

• 因风险限制被拦下但后续证明正确的机会

否则系统会被幸存者偏差误导。

2. 新增机会类型

• EXECUTED_VALID

• EXECUTED_INVALID

• MISSED_VALID

• MISSED_INVALID

• PUSHED_NOT_FILLED_VALID

• BLOCKED_BY_RISK_VALID

3. MISSED_VALID 判定规则

满足以下条件则记为 MISSED_VALID：

1. 原始计划逻辑成立

2. 观察窗口内达成最小目标

3. 未先触发失效条件

4. 未被实际执行

4. 观察窗口

按 plan_mode 配置：

• 预备型：30m ~ 4h

• 确认型：15m ~ 2h

• 缩减型：按保守窗口

写入规则表配置。

5. 新增表：tm_opportunity_log

字段如下：

• opportunity_id

• analysis_id

• symbol

• timeframe

• opportunity_type

• plan_mode

• push_id

• was_pushed

• was_clicked

• was_executed

• was_blocked_by_risk

• entry_zone_json

• invalidation_condition_json

• target_condition_json

• opportunity_start_time

• opportunity_end_time

• outcome_status

• max_favorable_excursion

• max_adverse_excursion

• reached_target

• hit_invalidation

• opportunity_review_json

• market_context_tag_json

• rule_version

6. 复盘必须支持的视角

1. 已执行机会成功率

2. 未执行但正确机会数量

3. 推送未成交但正确的机会

4. 因风险限制放弃的正确机会

5. 入场区间是否定义过窄

6. 推送是否太晚

7. 新增接口

• GET /api/opportunity-log/{analysisId}

• GET /api/opportunity-log/missed-valid

• GET /api/opportunity-log/stats

• POST /api/opportunity-log/review

⸻

八、Hot Reset Mechanism 机制

1. 机制目标

在极端环境突变时，系统不能被最短保持时间、滞回逻辑拖住。

必须允许绕过状态稳定机制，强制重置状态。

2. 触发场景

A. 极端价格波动

• 5m 波动 > 3 × ATR_5m

• 或 15m 波动 > 2.5 × ATR_15m

B. 杠杆结构崩塌

• OI 快速下坠

• Funding 极端快速归零或反转

• 清算量暴增

C. 系统性联动冲击

• BTC 极端波动带动全场联动

• 宏观事件导致多资产同步跳变

D. 流动性瞬间抽空

• 深度骤降

• 滑点急剧放大

• 连续插针 / 扫流动性

3. 触发后动作

1. 清空当前 candidate / waiting_trigger / triggered 的即时有效性

2. 立即重算：

• dominant_driver

• cause_effect_alignment

• execution feasibility

• confused_score

3. 允许直接切换至：

• high_risk

• invalidated

• confused

• cooling

4. 所有旧计划标记为：

• needs_revalidation

5. 所有未执行推送必须重新校验

4. 优先级

Hot Reset 高于滞回机制。

正常市场走稳定逻辑；极端突变由 Hot Reset 接管。

5. 新增字段

在 tm_asset_state 新增：

• hot_reset_flag

• hot_reset_trigger_type

• hot_reset_trigger_value

• hot_reset_time

• pre_reset_state

• post_reset_state

在 tm_analysis_run 新增：

• was_hot_reset

• hot_reset_detail_json

6. 新增接口

• POST /api/state/hot-reset/{symbol}

• GET /api/state/hot-reset-log/{symbol}

• POST /api/analysis/rebuild-after-hot-reset

⸻

九、规则表配置建议

在 tm_rule_config 新增以下配置：

• confused_state_config

• ai_conflict_config

• push_recheck_config

• missed_opportunity_config

• hot_reset_config

1. confused_state_config

• confused_score_threshold_enter

• confused_score_threshold_exit

• driver_switch_limit

• conflict_uptrend_period

• confused_push_block_enabled

2. ai_conflict_config

• ai_conflict_enable

• direction_conflict_weight

• risk_conflict_weight

• plan_conflict_weight

• ai_conflict_confused_threshold

• ai_conflict_max_downgrade_level

3. push_recheck_config

• push_recheck_enabled

• push_expire_seconds

• max_price_drift_ratio

• max_slippage_on_recheck

• recheck_account_risk_required

4. missed_opportunity_config

• opportunity_tracking_enabled

• missed_valid_window_by_plan_mode

• target_reach_threshold

• invalidation_priority

5. hot_reset_config

• hot_reset_enabled

• atr_multiplier_5m

• atr_multiplier_15m

• oi_drop_threshold

• liquidation_spike_threshold

• btc_systemic_shock_threshold

⸻

十、服务层设计

新增服务：

• ConfusedStateService

• AiConflictResolverService

• PushRecheckService

• OpportunityLogService

• HotResetService

1. ConfusedStateService

职责：

• 计算 confused_score

• 判断进入/退出 confused

• 记录冲突原因

• 生成前端陷阱警告

2. AiConflictResolverService

职责：

• 接收 GPT/Gemini/Grok 输出

• 计算 ai_conflict_score

• 生成 AI 四级冲突结果

• 产出方向保留/降级/转 confused 的最终建议

3. PushRecheckService

职责：

• 管理推送快照

• 用户点击后执行 Recheck

• 记录推送质量

• 决定当前是否仍可执行

4. OpportunityLogService

职责：

• 跟踪未执行机会

• 判定 MISSED_VALID / MISSED_INVALID

• 输出机会遗漏报表

5. HotResetService

职责：

• 监听极端环境指标

• 触发紧急重置

• 清理旧计划有效性

• 驱动重新分析

⸻

十一、AI 编排服务必须修改

现有 AiDecisionOrchestratorService 必须增加以下流程：

1. 基础流程

1. 规则层先产出基础结果

2. GPT/Gemini/Grok 读取规则基础结果和证据

3. AiConflictResolverService 计算 ai_conflict_score

4. 将 AI 冲突结果回写至：

• 决策层

• 计划模式

• confused 判定

5. 若规则层允许方向，但 AI 冲突中等，则方向保留但降级

6. 若规则层冲突高且 AI 冲突进一步放大，则进入 confused

2. 禁止逻辑

禁止一

不允许 Gemini != GPT 就直接观望。

禁止二

不允许 Grok 提反证 就直接取消方向。

禁止三

不允许 AI 输出绕过规则层直接进入 triggered。

禁止四

不允许 AI 分歧导致无限停滞。

⸻

十二、落库与审计要求

以下事件必须落库：

1. 进入 confused

2. 退出 confused

3. AI 冲突评分

4. AI 降级原因

5. 推送发出

6. 推送打开后二次确认

7. 推送失效

8. Missed Valid 机会

9. Hot Reset 触发

10. Hot Reset 后状态重建

所有记录必须包含：

• traceId

• requestId

• analysisId

• symbol

• timeframe

• ruleVersion

• serverTime

⸻

十三、前端展示要求

1. confused 状态展示

• 置顶显示“方向冲突升高 / 流动性陷阱风险”

• 不显示明确看多/看空主色块

• 展示恢复条件

2. AI 分歧展示

• 显示最终方向是否被保留

• 显示：

• GPT 最终裁决

• Gemini 主要异议

• Grok 主要反方挑战

• 用“降级”表达，不用“互相否决”表达

3. 推送详情页

打开时必须实时显示：

• 当前是否仍可执行

• 原推送区间是否已漂移

• 当前风险状态

• 当前是否处于 confused

4. 机会复盘页

必须能查看：

• 已执行机会

• 未执行但正确机会

• 风险阻断但正确机会

• 推送质量统计

⸻

十四、开发顺序建议

第一批必须落地

1. AI 冲突处理四级规则

2. Confused State

3. Push Snapshot + Push Recheck

4. Hot Reset 基础触发

5. 关键日志落库

第二批落地

1. Missed Opportunity Logging

2. Confused 前端展示

3. 推送质量统计

4. Hot Reset 后重建逻辑

5. 机会复盘统计页

第三批优化

1. ai_conflict_score 精细化

2. confused_score 模型优化

3. missed_valid 自动判定优化

4. 推送冷却与质量评分优化

⸻

十五、最小可用实现口径

如果要先做最小可用版本，必须先实现以下最小集：

1. 新增 confused 状态

2. 定义 AI 冲突四级输出规则

3. 推送打开后二次确认

4. Hot Reset 极端触发

5. Missed Opportunity 基础记录

6. 关键事件全部落库

只有做到这六项，系统才算真正具备：

• 冲突时不乱给方向

• 推送时不把过期机会继续当机会

• 复盘时不被幸存者偏差带偏

• 极端环境下不被慢状态拖死

• AI 打架时不至于永远不给方向

⸻

十六、一句话交付原则

请按以下原则开发，不得偏离：

规则先给基础方向，AI 只做增强与降级；冲突高时进入 confused 而不是硬给方向；推送点击时必须重新确认有效性；未执行但正确的机会必须记录；极端环境时必须允许 Hot Reset 跳过常规滞回；AI 分歧不能无限阻断系统，只能改变置信度、风险等级和计划模式，只有在规则层与 AI 冲突同时达到高阈值时，才允许进入 confused。
