# Trade Model V1 Project Delivery Contract

Contract Version: v1.0
Contract Status: ACTIVE
Source of Truth: This file is the only delivery standard for Trade Model V1.

---

## 1. Non-negotiable Goal / 项目唯一目标

The only valid completion target is:

**A fully usable trading decision closed-loop system.**

Chinese definition:

**项目唯一完成目标是：完整功能真实可用的交易决策闭环系统。**

The system is complete only when this full chain can run end-to-end:

Raw Data
→ Evidence
→ Score
→ Decision
→ Execution Plan
→ UserPosition
→ PositionMonitor
→ Risk Handling
→ Manual Close
→ Review
→ Rule Feedback

中文链路：

原始数据
→ 证据
→ 评分
→ 决策
→ 执行计划
→ 用户真实持仓
→ 持仓监控
→ 风险处理
→ 用户手动平仓
→ 复盘
→ 规则修正

No review-only slice, DTO-only package, docs-only package, dashboard-only panel, preview-only module, or test-only skeleton may be marked as complete unless it satisfies the Module Done Definition in this contract.

任何 review-only、DTO-only、docs-only、dashboard-only、preview-only、test-only skeleton 都不能被判定为模块完成，除非满足本契约的模块完成标准。

---

## 2. Module Done Definition / 模块完成标准

A module is DONE only when all items below are true:

1. Schema exists when persistence is required.
2. DO / Mapper exists when persistence is required.
3. DTO / Req / VO / Enum exist when API or service boundary requires them.
4. Service contains real business logic.
5. Controller or explicit internal caller exists.
6. Unit tests cover core rules.
7. Integration or chain test proves downstream usage.
8. Dashboard or downstream module consumes the result when applicable.
9. The module does not violate trading safety boundaries.
10. Maven test passes.

中文标准：

一个模块只有同时满足以下条件，才允许标记为 DONE：

1. 需要持久化时，必须有 schema。
2. 需要持久化时，必须有 DO / Mapper。
3. API 或服务边界需要时，必须有 DTO / Req / VO / Enum。
4. Service 必须包含真实业务逻辑。
5. 必须有 Controller 或明确的内部调用入口。
6. 必须有单元测试覆盖核心规则。
7. 必须有集成测试或链路测试证明下游使用。
8. 适用时，Dashboard 或下游模块必须消费该结果。
9. 不能违反交易安全边界。
10. Maven 测试必须通过。

If any item is missing, the module status must be PARTIAL, not DONE.

如果缺少任何一项，只能标记为 PARTIAL，不能标记为 DONE。

---

## 3. Forbidden Completion Claims / 禁止的完成表述

The following must never be called DONE:

- docs-only
- DTO-only
- enum-only
- schema-only
- test-only skeleton
- review-only endpoint
- preview-only panel
- dashboard-only display
- fallback-only implementation
- no-op service
- mock-only implementation
- placeholder-only implementation

以下内容不能被称为完成：

- 只有文档
- 只有 DTO
- 只有枚举
- 只有 schema
- 只有测试骨架
- 只有 review-only endpoint
- 只有 preview panel
- 只有 dashboard display
- 只有 fallback
- no-op service
- mock-only
- placeholder-only

---

## 4. Permanent Safety Rules / 永久安全规则

The system must never:

1. Auto-open a trade.
2. Auto-close a trade.
3. Auto-reverse a position.
4. Treat triggered as opened.
5. Treat execution_plan as user_position.
6. Treat tm_real_position as user_position.
7. Let AI bypass rule-layer base direction.
8. Treat PushRecheck as trading authorization.
9. Create UserPosition without manual user input.
10. Mark a plan VALID without required source evidence.

中文规则：

系统永远不能：

1. 自动开仓。
2. 自动平仓。
3. 自动反手。
4. 把 triggered 当成已开仓。
5. 把 execution_plan 当成 user_position。
6. 把 tm_real_position 当成 user_position。
7. 让 AI 绕过规则层基础方向。
8. 把 PushRecheck 当成交易授权。
9. 在没有用户手动输入时创建 UserPosition。
10. 在缺少 source evidence 时把计划标记为 VALID。

---

## 5. Development Order Gate / 开发顺序总门禁

Development must follow this exact order:

P0-0: Contract Lock + Baseline + Dead Code Candidate Report
P0-1: UserPosition
P0-2: ExecutionPlan Source Gate
P0-3: AccountRisk integrates UserPosition
P0-4: PositionMonitorLog
P0-5: PositionMonitorService
P0-6: Review integrates UserPosition

P1-1: PushRecheck semantic hardening
P1-2: ConfusedState + AiConflict hardening
P1-3: HotReset real action
P1-4: OpportunityLog

P2-1: Macro / News / External Context
P2-2: AI Orchestrator + AiCallLog
P2-3: Scheduler / Idempotency / Trace

P3-1: Dashboard Final
P3-2: Full E2E Acceptance
P3-3: Final Delivery Docs

No task may start unless the previous gate is DONE in DELIVERY_PROGRESS_MATRIX.md.

中文规则：

开发必须严格按以下顺序推进：

P0-0：契约锁定 + 基线确认 + 无用代码候选清单
P0-1：UserPosition
P0-2：ExecutionPlan Source Gate
P0-3：AccountRisk 接入 UserPosition
P0-4：PositionMonitorLog
P0-5：PositionMonitorService
P0-6：Review 承接 UserPosition

P1-1：PushRecheck 语义收紧
P1-2：ConfusedState + AiConflict 硬化
P1-3：HotReset 真动作
P1-4：OpportunityLog

P2-1：Macro / News / External Context
P2-2：AI Orchestrator + AiCallLog
P2-3：Scheduler / Idempotency / Trace

P3-1：Dashboard Final
P3-2：全链路 E2E 验收
P3-3：最终交付文档

任何任务不得跳过当前阶段。
只有上一阶段在 DELIVERY_PROGRESS_MATRIX.md 中标记 DONE，才允许进入下一阶段。

---

## 5A. Fixed Codex Output Contract / Codex 固定输出契约

Every Codex task must finish with user-readable progress fields defined in `docs/ANSWER_FORMAT_CONTRACT.md`.
The output must include:

- WHAT_THIS_STEP_DOES（这一步在做什么）
- CURRENT_PROGRESS（当前进度）
- NEXT_ALLOWED_ACTION（下一允许动作）
- NEXT_BLOCKED_ACTION（下一禁止动作）
- OVERREACH_STATUS（越界状态）

English technical terms must include Chinese explanations.
Open PR（未合并 PR） and PENDING_MERGED_MAIN（等待合并主线） do not count as effective（已生效） completion.
Only merged main（已合并主线） plus clean/synced main（干净且已同步主线） and a passing runtime gate（门禁） can make a package effective（已生效）.
This output contract changes reporting only; it does not change phase order, Done Criteria, business logic, gate（门禁） rules, or safety boundaries.

---

## 6. Phase Done Criteria / 阶段完成判定

### P0-0 Contract Lock + Baseline + Dead Code Candidate Report DONE

P0-0 is DONE only when:

1. PROJECT_DELIVERY_CONTRACT.md exists.
2. PROJECT_CURRENT_STATE.md exists.
3. DELIVERY_PROGRESS_MATRIX.md exists.
4. CODEX_TASK_TEMPLATE.md exists.
5. CONTRACT_CHANGE_LOG.md exists.
6. DEAD_CODE_CANDIDATES.md exists.
7. PROJECT_GLOBAL_AUDIT.md exists.
8. AGENTS.md references this contract.
9. Workflow automation is migrated to the contract / matrix / current-state fact hierarchy.
10. Maven test passes.
11. No business code was changed.
12. No code was deleted.

中文完成标准：

1. PROJECT_DELIVERY_CONTRACT.md 已存在。
2. PROJECT_CURRENT_STATE.md 已存在。
3. DELIVERY_PROGRESS_MATRIX.md 已存在。
4. CODEX_TASK_TEMPLATE.md 已存在。
5. CONTRACT_CHANGE_LOG.md 已存在。
6. DEAD_CODE_CANDIDATES.md 已存在。
7. PROJECT_GLOBAL_AUDIT.md 已存在。
8. AGENTS.md 已引用本契约。
9. 工作流自动化已迁移到契约 / 矩阵 / 当前状态事实源体系。
10. Maven 测试通过。
11. 没有修改业务代码。
12. 没有删除代码。

---

### P0-1 UserPosition DONE

P0-1 is DONE only when:

1. tm_user_position exists.
2. UserPositionDO exists.
3. UserPositionDTO exists.
4. UserPositionMapper exists.
5. UserPositionService exists.
6. UserPositionController exists.
7. CreateUserPositionReq exists.
8. CloseUserPositionReq exists.
9. UserPositionVO exists.
10. UserPositionStatusEnum exists.
11. UserPositionDirectionEnum exists.
12. UserPositionSourceEnum exists.
13. source = MANUAL on creation.
14. status = OPEN on creation.
15. close changes status to CLOSED.
16. getOpenPositions excludes CLOSED.
17. execution_plan cannot auto-create user_position.
18. triggered cannot auto-create user_position.
19. tm_real_position cannot be treated as user_position.
20. UserPositionVO contains notTradeInstruction = true.
21. Tests prove all rules.

中文完成标准：

1. 有 tm_user_position。
2. 有 UserPositionDO。
3. 有 UserPositionDTO。
4. 有 UserPositionMapper。
5. 有 UserPositionService。
6. 有 UserPositionController。
7. 有 CreateUserPositionReq。
8. 有 CloseUserPositionReq。
9. 有 UserPositionVO。
10. 有 UserPositionStatusEnum。
11. 有 UserPositionDirectionEnum。
12. 有 UserPositionSourceEnum。
13. 创建时 source = MANUAL。
14. 创建时 status = OPEN。
15. close 后 status = CLOSED。
16. getOpenPositions 不返回 CLOSED。
17. execution_plan 不能自动创建 user_position。
18. triggered 不能自动创建 user_position。
19. tm_real_position 不能当成 user_position。
20. UserPositionVO 必须包含 notTradeInstruction = true。
21. 测试覆盖以上全部规则。

---

### P0-2 ExecutionPlan Source Gate DONE

P0-2 is DONE only when:

1. ExecutionPlanSourceGate exists.
2. BoundaryCandidateSourceGate exists.
3. NumericBoundarySourceValidator exists.
4. Entry without source cannot be VALID.
5. Stop without source cannot be VALID.
6. Take-profit without source cannot be VALID.
7. RR without source cannot be VALID.
8. Liquidity source is required when liquidity is used as evidence.
9. Wick source is required when wick is used as evidence.
10. Event window source is required when event blocker is used.
11. Multi-timeframe source is required when multi-timeframe convergence is used.
12. fallback / incomplete / review-only cannot become VALID.
13. DTO.valid cannot bypass gate.
14. Tests cover all missing-source failure paths.
15. Tests cover valid source success path.

中文完成标准：

1. 有 ExecutionPlanSourceGate。
2. 有 BoundaryCandidateSourceGate。
3. 有 NumericBoundarySourceValidator。
4. entry 无 source 不能 VALID。
5. stop 无 source 不能 VALID。
6. take-profit 无 source 不能 VALID。
7. RR 无 source 不能 VALID。
8. 使用流动性证据时必须有 liquidity source。
9. 使用 wick 证据时必须有 wick source。
10. 使用事件阻断时必须有 event window source。
11. 使用多周期收敛时必须有 multi-timeframe source。
12. fallback / incomplete / review-only 不能升级为 VALID。
13. DTO.valid 不能绕过 gate。
14. 测试覆盖所有缺 source 的失败路径。
15. 测试覆盖 source 完整时的成功路径。

---

### P0-3 AccountRisk integrates UserPosition DONE

P0-3 is DONE only when:

1. UserPositionRiskAdapter exists.
2. OPEN UserPosition enters risk calculation.
3. PARTIALLY_CLOSED UserPosition enters risk calculation.
4. CLOSED UserPosition is excluded.
5. Leverage risk is calculated.
6. Position size risk is calculated.
7. Concentration risk is calculated.
8. Correlation risk is calculated.
9. Drawdown or VaR-style risk is calculated.
10. High risk returns risk_blocked.
11. Risk result is read-only and never auto-reduces position.
12. PositionMonitor can consume risk result.
13. PushRecheck can consume risk result.
14. Tests cover open, closed, high leverage, high concentration.

中文完成标准：

1. 有 UserPositionRiskAdapter。
2. OPEN UserPosition 进入风险计算。
3. PARTIALLY_CLOSED UserPosition 进入风险计算。
4. CLOSED UserPosition 不进入当前风险。
5. 计算杠杆风险。
6. 计算仓位大小风险。
7. 计算集中度风险。
8. 计算相关性风险。
9. 计算最大回撤或 VaR 风格风险。
10. 高风险返回 risk_blocked。
11. 风险结果只读，不能自动减仓。
12. PositionMonitor 可以消费风险结果。
13. PushRecheck 可以消费风险结果。
14. 测试覆盖 open、closed、高杠杆、高集中度。

---

### P0-4 PositionMonitorLog DONE

P0-4 is DONE only when:

1. tm_position_monitor_log exists.
2. PositionMonitorLogDO exists.
3. PositionMonitorLogDTO exists.
4. PositionMonitorLogMapper exists.
5. PositionMonitorLogService exists.
6. Every monitor run writes one log.
7. Log includes position_id.
8. Log includes analysis_id.
9. Log includes execution_plan_id when available.
10. Log includes current_price.
11. Log includes logic_status.
12. Log includes risk_level.
13. Log includes suggested_action.
14. Log includes evidence / score / decision snapshot when available.
15. Review can query logs.
16. Tests cover normal, weakened, invalidated, high-risk.

中文完成标准：

1. 有 tm_position_monitor_log。
2. 有 PositionMonitorLogDO。
3. 有 PositionMonitorLogDTO。
4. 有 PositionMonitorLogMapper。
5. 有 PositionMonitorLogService。
6. 每次 monitor 都写一条 log。
7. log 包含 position_id。
8. log 包含 analysis_id。
9. 可用时 log 包含 execution_plan_id。
10. log 包含 current_price。
11. log 包含 logic_status。
12. log 包含 risk_level。
13. log 包含 suggested_action。
14. 可用时 log 包含 evidence / score / decision snapshot。
15. Review 可以查询这些 log。
16. 测试覆盖正常、逻辑弱化、计划失效、高风险。

---

### P0-5 PositionMonitorService DONE

P0-5 is DONE only when:

1. PositionMonitorService exists.
2. PositionMonitorController exists.
3. Can monitor one UserPosition.
4. Can monitor all OPEN UserPositions for a user.
5. Can judge logic still valid.
6. Can judge logic weakened.
7. Can judge plan invalidated.
8. Can judge near stop loss.
9. Can judge near take profit.
10. Can judge risk increased.
11. Writes PositionMonitorLog each run.
12. Does not auto-close.
13. Does not auto-reverse.
14. Does not place orders.
15. Tests cover long, short, weakened, invalidated, near stop, near take profit.

中文完成标准：

1. 有 PositionMonitorService。
2. 有 PositionMonitorController。
3. 能监控单笔 UserPosition。
4. 能批量监控用户 OPEN 持仓。
5. 能判断逻辑仍成立。
6. 能判断逻辑弱化。
7. 能判断计划失效。
8. 能判断接近止损。
9. 能判断接近止盈。
10. 能判断风险升高。
11. 每次监控写 PositionMonitorLog。
12. 不自动平仓。
13. 不自动反手。
14. 不下单。
15. 测试覆盖 long、short、逻辑弱化、计划失效、接近止损、接近止盈。

---

### P0-6 Review integrates UserPosition DONE

P0-6 is DONE only when:

1. UserPositionReviewAdapter exists.
2. CLOSED UserPosition can generate ReviewSummary.
3. Review reads linked ExecutionPlan.
4. Review reads real open price.
5. Review reads user stop loss.
6. Review reads user take profit.
7. Review reads quantity and leverage.
8. Review reads all PositionMonitorLog entries.
9. Review can judge execution deviation.
10. Review can judge whether system warned in time.
11. Review can record rule feedback.
12. Tests cover win, loss, user deviation, plan invalidation, ignored warning.

中文完成标准：

1. 有 UserPositionReviewAdapter。
2. CLOSED UserPosition 可以生成 ReviewSummary。
3. Review 能读取关联 ExecutionPlan。
4. Review 能读取真实开仓价。
5. Review 能读取用户止损。
6. Review 能读取用户止盈。
7. Review 能读取数量和杠杆。
8. Review 能读取所有 PositionMonitorLog。
9. Review 能判断执行偏离。
10. Review 能判断系统是否及时提醒。
11. Review 能记录规则反馈。
12. 测试覆盖盈利、亏损、用户偏离、计划失效、忽略提醒。

---

### P1-1 PushRecheck semantic hardening DONE

P1-1 is DONE only when:

1. No state name implies trading authorization.
2. Recheck returns notTradeInstruction = true.
3. Expired push returns EXPIRED.
4. Price drift returns DRIFTED_FROM_ENTRY_ZONE.
5. high_risk returns RISK_BLOCKED.
6. confused returns CONFUSED_BLOCKED.
7. Recheck cannot create UserPosition.
8. Recheck cannot trigger trade actions.
9. Tests cover every status.

中文完成标准：

1. 状态命名不能暗示交易授权。
2. Recheck 返回 notTradeInstruction = true。
3. 过期 push 返回 EXPIRED。
4. 价格漂移返回 DRIFTED_FROM_ENTRY_ZONE。
5. high_risk 返回 RISK_BLOCKED。
6. confused 返回 CONFUSED_BLOCKED。
7. Recheck 不能创建 UserPosition。
8. Recheck 不能触发交易动作。
9. 测试覆盖所有状态。

---

### P1-2 ConfusedState + AiConflict hardening DONE

P1-2 is DONE only when:

1. confused_score >= 70 enters confused.
2. confused_score >= 85 blocks directional push.
3. Two consecutive cycles below 55 are required to exit.
4. Exit cannot go directly to triggered.
5. One AI objection cannot force infinite wait.
6. AI cannot bypass rule layer.
7. Gemini / Grok cannot override state machine.
8. AI disagreement can only change confidence, risk, plan mode, or confused state.
9. Tests cover aligned, minor conflict, major conflict, extreme conflict.

中文完成标准：

1. confused_score >= 70 进入 confused。
2. confused_score >= 85 禁止方向性推送。
3. 连续两个周期低于 55 才能退出。
4. 退出后不能直接 triggered。
5. 单个 AI 反对不能导致无限观望。
6. AI 不能绕过规则层。
7. Gemini / Grok 不能覆盖状态机。
8. AI 分歧只能影响置信度、风险、计划模式或 confused。
9. 测试覆盖一致、轻微分歧、显著分歧、极端分歧。

---

### P1-3 HotReset real action DONE

P1-3 is DONE only when:

1. Extreme event can trigger HotReset.
2. Old candidate loses immediate validity.
3. Old waiting_trigger loses immediate validity.
4. Old triggered loses immediate validity.
5. Old ExecutionPlan is marked needs_revalidation.
6. AssetState can change to high_risk / invalidated / confused / cooling.
7. confused_score is recalculated.
8. account risk is recalculated.
9. analysis rebuild is triggered.
10. HotReset event is persisted.
11. Tests cover extreme price move, OI collapse, liquidity drain, systemic shock.

中文完成标准：

1. 极端事件能触发 HotReset。
2. 旧 candidate 失去即时有效性。
3. 旧 waiting_trigger 失去即时有效性。
4. 旧 triggered 失去即时有效性。
5. 旧 ExecutionPlan 标记 needs_revalidation。
6. AssetState 可变为 high_risk / invalidated / confused / cooling。
7. 重新计算 confused_score。
8. 重新计算 account risk。
9. 触发 analysis rebuild。
10. HotReset event 落库。
11. 测试覆盖极端价格波动、OI 崩塌、流动性抽空、系统性冲击。

---

### P1-4 OpportunityLog DONE

P1-4 is DONE only when:

1. tm_opportunity_log exists.
2. OpportunityLogDO exists.
3. OpportunityLogService exists.
4. OpportunityLogController exists.
5. Supports EXECUTED_VALID.
6. Supports EXECUTED_INVALID.
7. Supports MISSED_VALID.
8. Supports MISSED_INVALID.
9. Supports PUSHED_NOT_FILLED_VALID.
10. Supports BLOCKED_BY_RISK_VALID.
11. Records MFE / MAE.
12. Records target / invalidation.
13. Review can query opportunity stats.
14. Tests cover target hit, invalidation first, not clicked, risk blocked.

中文完成标准：

1. 有 tm_opportunity_log。
2. 有 OpportunityLogDO。
3. 有 OpportunityLogService。
4. 有 OpportunityLogController。
5. 支持 EXECUTED_VALID。
6. 支持 EXECUTED_INVALID。
7. 支持 MISSED_VALID。
8. 支持 MISSED_INVALID。
9. 支持 PUSHED_NOT_FILLED_VALID。
10. 支持 BLOCKED_BY_RISK_VALID。
11. 记录 MFE / MAE。
12. 记录 target / invalidation。
13. Review 可以查询机会统计。
14. 测试覆盖达成目标、先触发失效、未点击、风险阻断。

---

### P2-1 Macro / News / External Context DONE

P2-1 is DONE only when:

1. tm_macro_event exists.
2. tm_news_event exists.
3. MacroEventService exists.
4. NewsEventService exists.
5. ExternalContextEvidenceBuilder exists.
6. External events can become EvidenceItem.
7. Event source is traceable.
8. Event window can affect Decision.
9. Event window can block ExecutionPlan.
10. Event context can enter PositionMonitor.
11. Dashboard shows external event state.
12. Tests cover near event, expired event, major news, missing source.

中文完成标准：

1. 有 tm_macro_event。
2. 有 tm_news_event。
3. 有 MacroEventService。
4. 有 NewsEventService。
5. 有 ExternalContextEvidenceBuilder。
6. 外部事件能生成 EvidenceItem。
7. 事件 source 可追踪。
8. 事件窗口能影响 Decision。
9. 事件窗口能阻断 ExecutionPlan。
10. 事件上下文能进入 PositionMonitor。
11. Dashboard 展示外部事件状态。
12. 测试覆盖事件临近、事件过期、重大新闻、source 缺失。

---

### P2-2 AI Orchestrator + AiCallLog DONE

P2-2 is DONE only when:

1. AiDecisionOrchestratorService exists.
2. Provider client abstraction exists.
3. GPT client or safe adapter exists.
4. Gemini client or safe adapter exists.
5. Grok client or safe adapter exists.
6. Rule layer produces base direction first.
7. AI only enhances / reviews / challenges / downgrades.
8. AI cannot write state machine directly.
9. AI cannot create ExecutionPlan directly.
10. AI failure falls back to rule mode.
11. tm_ai_call_log records token, cost, latency, provider, fallback, traceId.
12. Rate limit is tested.
13. Budget guard is tested.
14. Tests cover success, timeout, failure, budget exhausted, partial provider failure.

中文完成标准：

1. 有 AiDecisionOrchestratorService。
2. 有 provider client abstraction。
3. 有 GPT client 或安全 adapter。
4. 有 Gemini client 或安全 adapter。
5. 有 Grok client 或安全 adapter。
6. 规则层先产生基础方向。
7. AI 只能增强 / 复核 / 挑战 / 降级。
8. AI 不能直接写状态机。
9. AI 不能直接创建 ExecutionPlan。
10. AI 失败 fallback 到规则模式。
11. tm_ai_call_log 记录 token、cost、latency、provider、fallback、traceId。
12. 限流有测试。
13. budget guard 有测试。
14. 测试覆盖成功、超时、失败、预算耗尽、部分 provider 失败。

---

### P2-3 Scheduler / Idempotency / Trace DONE

P2-3 is DONE only when:

1. AnalysisSchedulerService exists.
2. AnalysisRunOrchestrator exists.
3. AnalysisIdempotencyGuard exists.
4. Same symbol + timeframe + analysis_time + rule_version cannot create duplicate valid analysis.
5. traceId exists for every analysis.
6. requestId exists for write APIs.
7. input snapshot / evidence / score / decision / plan / monitor / review are traceable.
8. Tests cover duplicate trigger.
9. Tests cover concurrent trigger.
10. Tests cover failure recovery.

中文完成标准：

1. 有 AnalysisSchedulerService。
2. 有 AnalysisRunOrchestrator。
3. 有 AnalysisIdempotencyGuard。
4. 同一 symbol + timeframe + analysis_time + rule_version 不能重复创建有效分析。
5. 每次 analysis 有 traceId。
6. 写接口有 requestId。
7. input snapshot / evidence / score / decision / plan / monitor / review 可追踪。
8. 测试覆盖重复触发。
9. 测试覆盖并发触发。
10. 测试覆盖失败恢复。

---

### P3-1 Dashboard Final DONE

P3-1 is DONE only when:

1. Homepage separates system suggestion from UserPosition.
2. No UserPosition means no open-position display.
3. ExecutionPlan appears only as suggestion.
4. UserPosition triggers position-monitor display.
5. Closed position disappears from homepage.
6. Closed position appears in review.
7. Confused state blocks directional execution suggestion.
8. HotReset shows needs_revalidation.
9. PushRecheck never displays trading authorization.
10. Dashboard tests pass.

中文完成标准：

1. 首页区分系统建议和 UserPosition。
2. 没有 UserPosition 时不能显示已持仓。
3. ExecutionPlan 只能显示为系统建议。
4. UserPosition 存在时才显示持仓监控。
5. 已平仓持仓从首页消失。
6. 已平仓持仓进入复盘。
7. Confused 状态阻断方向性执行建议。
8. HotReset 显示 needs_revalidation。
9. PushRecheck 不能显示交易授权。
10. Dashboard 测试通过。

---

### P3-2 Full E2E Acceptance DONE

All scenarios must pass:

1. ExecutionPlan without UserPosition does not show opened position.
2. Manual UserPosition creation shows real position.
3. PositionMonitor logs HOLD when logic still valid.
4. PositionMonitor logs LOGIC_WEAKENED when evidence weakens.
5. PositionMonitor logs PLAN_INVALIDATED when plan breaks.
6. PushRecheck returns drifted / expired / risk_blocked correctly.
7. Confused blocks directional suggestions.
8. HotReset marks old plan needs_revalidation.
9. Closed UserPosition generates Review with execution deviation.
10. MISSED_VALID is recorded in OpportunityLog.
11. Macro / News event enters Decision / Monitor.
12. AI failure falls back and writes AiCallLog.

中文完成标准：

以下场景全部通过：

1. 有 ExecutionPlan 但无 UserPosition 时，不能显示已开仓。
2. 用户手动创建 UserPosition 后，显示真实持仓。
3. 持仓逻辑仍成立时，PositionMonitor 写 HOLD。
4. 证据弱化时，PositionMonitor 写 LOGIC_WEAKENED。
5. 计划破坏时，PositionMonitor 写 PLAN_INVALIDATED。
6. PushRecheck 正确返回 drifted / expired / risk_blocked。
7. Confused 阻断方向性建议。
8. HotReset 将旧计划标记 needs_revalidation。
9. CLOSED UserPosition 生成带执行偏离的 Review。
10. MISSED_VALID 写入 OpportunityLog。
11. Macro / News 事件进入 Decision / Monitor。
12. AI 失败 fallback 并写 AiCallLog。

---

### P3-3 Final Delivery Docs DONE

P3-3 is DONE only when:

1. API docs updated.
2. State machine docs updated.
3. Data flow docs updated.
4. Dashboard usage docs updated.
5. Full acceptance report exists.
6. Final delivery checklist exists.
7. Maven test passes.
8. E2E acceptance passes.

中文完成标准：

1. API 文档已更新。
2. 状态机文档已更新。
3. 数据流文档已更新。
4. Dashboard 使用文档已更新。
5. 有完整验收报告。
6. 有最终交付清单。
7. Maven 测试通过。
8. E2E 验收通过。

---

## 7. Dead Code Policy / 无用代码清理政策

Dead code cleanup must be evidence-based.

No file can be deleted only because it looks old, unused, duplicated, or review-only.

Before deletion, docs/DEAD_CODE_CANDIDATES.md must exist.

Each candidate must record:

- file path
- candidate type
- production references
- test references
- schema support
- whether it is needed by future phases in this contract
- delete risk
- recommended action
- reason

Only LOW risk + DELETE candidates may be deleted.

Each deletion batch must contain at most 10 files.

After every deletion batch, run:

./mvnw test -q

If tests fail, restore the batch.

中文规则：

无用代码清理必须基于证据，不能凭感觉删除。

不能因为文件看起来旧、重复、review-only、preview-only 就删除。

删除前必须先有 docs/DEAD_CODE_CANDIDATES.md。

每个候选必须记录：

- 文件路径
- 候选类型
- 是否有生产引用
- 是否有测试引用
- 是否有 schema 支撑
- 是否在本契约后续阶段中需要
- 删除风险
- 建议动作
- 理由

只有 LOW 风险 + DELETE 的候选才能删除。

每批最多删除 10 个文件。

每批删除后必须执行：

./mvnw test -q

如果测试失败，必须恢复该批删除。

---

## 8. Final Project Completion / 项目最终完成判定

Trade Model V1 is complete only when:

1. Raw data can become Evidence.
2. Evidence can become Score.
3. Score can become Decision.
4. Decision can become ExecutionPlan.
5. ExecutionPlan has source-gated numeric boundaries.
6. User can manually create UserPosition.
7. UserPosition can be monitored.
8. PositionMonitor writes logs.
9. Risk changes are handled and recorded.
10. User can manually close UserPosition.
11. Review can explain system suggestion vs user execution deviation.
12. OpportunityLog records missed and blocked opportunities.
13. HotReset can invalidate stale plans.
14. Confused blocks directional suggestions.
15. Macro / News context can enter Evidence / Decision / Monitor.
16. AI can assist but cannot override rules.
17. Dashboard accurately shows real business state.
18. Full E2E acceptance passes.
19. Maven test passes.
20. Final docs are complete.

中文最终判定：

Trade Model V1 只有满足以下条件，才算完整完成：

1. 原始数据能生成 Evidence。
2. Evidence 能生成 Score。
3. Score 能生成 Decision。
4. Decision 能生成 ExecutionPlan。
5. ExecutionPlan 的 numeric boundary 经过 source gate。
6. 用户能手动创建 UserPosition。
7. UserPosition 能被监控。
8. PositionMonitor 写监控日志。
9. 风险变化能被处理和记录。
10. 用户能手动平仓。
11. Review 能解释系统建议与用户执行偏离。
12. OpportunityLog 记录 missed 和 blocked 机会。
13. HotReset 能让过期计划失效。
14. Confused 能阻断方向性建议。
15. Macro / News 能进入 Evidence / Decision / Monitor。
16. AI 只能辅助，不能覆盖规则。
17. Dashboard 准确展示真实业务状态。
18. 全链路 E2E 验收通过。
19. Maven 测试通过。
20. 最终文档完整。


---

## 9. P0-0 Governance Exception / P0-0 治理例外

P0-0 is a governance phase, not a business module.

P0-0 may be completed by its own P0-0 criteria: delivery contract, current state, progress matrix, task template, change log, dead-code candidate evidence, AGENTS.md contract reference, Maven test pass, and no business-code deletion or mutation.

This exception does not weaken business-module Done Criteria. A docs-only, DTO-only, enum-only, review-only, preview-only, dashboard-only, fallback-only, no-op, mock-only, or placeholder-only package still cannot mark any business module DONE.

中文：P0-0 是治理阶段，不是业务模块。P0-0 可按自身治理完成标准完成，但该例外不能用于证明任何业务模块 DONE。

---

## 10. Status Vocabulary / 状态词汇

Delivery tracking must distinguish two axes:

- Phase Status: `NOT_STARTED`, `IN_PROGRESS`, `BLOCKED`, `DONE`, `DEFERRED`, `FROZEN`.
- Existing Module Maturity: `NONE`, `PARTIAL`, `COMPLETE`.

Phase Status describes whether the contract phase itself has been executed under this contract.
Existing Module Maturity describes what the current merged repository already contains before the phase starts.

A phase may be `NOT_STARTED` while its existing module maturity is `PARTIAL`.
A phase may be `IN_PROGRESS` while no business module is allowed to start.
A maturity value of `COMPLETE` is not equivalent to Phase Status `DONE` unless the phase Done Criteria are also satisfied and merged to main.

中文：阶段状态和现有模块成熟度必须分开。不能因为仓库已有部分代码，就把契约阶段标记为 DONE。

---

## 11. Merged Main Effectivity / 合并主线生效规则

A phase status of DONE is effective only after the commit containing that DONE update is merged into `main`.

Local branch state, draft PR state, Codex output, unmerged docs, unmerged progress matrix edits, or local worktree status must not be treated as merged-main completion.

中文：DONE 只有在包含该状态的提交进入 merged main 后才生效。本地草案、分支、PR、Codex 输出都不能当作主线完成。

---

## 12. Source of Truth Priority / 事实源优先级

Authoritative delivery-state priority is:

1. `docs/PROJECT_DELIVERY_CONTRACT.md`
2. `docs/DELIVERY_PROGRESS_MATRIX.md`
3. `docs/PROJECT_CURRENT_STATE.md`
4. machine-readable compatibility files
5. historical V1 docs

Machine-readable compatibility files include, but are not limited to, `docs/ACTIVE_MAINLINE_STATUS.yml` and `docs/CODEX_NEXT_TASK.yml`.
They are derived compatibility files and cannot override this delivery contract or the progress matrix.

中文：ACTIVE_MAINLINE_STATUS.yml 和 CODEX_NEXT_TASK.yml 只能作为派生/兼容文件，不能覆盖 Delivery Contract 或 Progress Matrix。

---

## 13. Controlled Emergency Exception / 受控紧急例外

Production incident fixes, security vulnerability fixes, rollback repairs, or emergency data-safety repairs may bypass phase order only with explicit human authorization.

Such an exception may ship a repair. It must not mark a business phase DONE unless all Done Criteria are satisfied.

中文：生产事故、安全漏洞、回滚修复可以在人明确授权下绕过阶段顺序，但不能因此把业务阶段标记为 DONE。

---

## 14. Production Deployment Readiness / 生产部署就绪

Production Deployment Readiness is currently `BLOCKED`.

The current repository evidence shows deployment blockers that must be cleared before a production-ready deployment claim can be made:

1. H2 in-memory database must be replaced or gated behind a non-production profile.
2. Empty database password must not be a production default.
3. H2 console must not be enabled in production.
4. Production profile evidence must exist.
5. Authentication / authorization evidence must exist for operational and write APIs.
6. Secret management must be explicit.
7. Health/readiness/liveness checks must be deployment-grade.
8. Migration and rollback evidence must exist.
9. Deployment smoke and rollback pipeline must exist.
10. Simulated position provider default must not be ambiguous in production.

P3-3 local acceptance freeze can be DONE without production deployment approval. Production Deployment Readiness remains BLOCKED until these blockers are fixed or explicitly accepted by a separate human production release gate.

中文：生产部署就绪当前为 BLOCKED。P3-2 必须纳入生产配置、持久化数据库、认证、secret、health、migration、rollback、deployment smoke 等验收。

### 14A. PDR-2A Database Migration + Rollback Decision / 数据库迁移与回滚决策

PDR-2A locks the database migration direction before implementation:

1. Production database target is PostgreSQL.
2. Migration framework target is Flyway with SQL-first migrations.
3. Rollback policy is forward-only migrations plus pre-migration backup and restore.
4. Production migration execution must be an explicit manual pre-deploy step.
5. Application startup must not silently mutate production schema without a controlled migration process.
6. Initial recovery target is RPO 24h and RTO 4h.
7. `src/main/resources/schema.sql` remains local/test bootstrap until a later Flyway/Testcontainers package changes that behavior.
8. No PostgreSQL driver, Flyway dependency, migration SQL, schema change, mapper SQL change, production DB connection, backup script, deployment script, secret, auth, Telegram send, Push send, order/execution, or auto-trading semantics are introduced by PDR-2A.
9. PDR-2B may add the Flyway baseline skeleton only under a separate scoped package.

Production Deployment Readiness remains `BLOCKED` after PDR-2A.

---

## 15. A-risk Auto Merge Rule / A-risk 自动合并规则

P0-0 governance packages that are limited to docs / contract / workflow scripts may use the A-risk Auto Merge Rule.

A-risk automatic completion is allowed only when all items below are true:

1. Changed files only contain docs / workflow scripts / contract files.
2. No Java changes.
3. No tests changes.
4. No schema changes.
5. No dashboard changes.
6. No pom changes.
7. No runtime config changes.
8. Maven test passed.
9. Workflow contract passed.
10. Codex task validation passed.
11. PR checks passed.
12. PR is not Draft.
13. Target PR is the current package PR.
14. Unrelated Draft PRs do not block current package merge.
15. Unrelated Draft PRs still block the next business phase.

PR #1004 is unrelated Draft PR evidence for this P0-0 package. It must not be modified, merged, closed, reviewed, or targeted by the P0-0 completion flow. It must not block merging PR #1005, but while open it still blocks P0-1 through the next-business-phase gate.

Codex must use `scripts/v1-pr-complete.sh` when possible and `scripts/v1-merge-sync.sh` for merge plus local main sync. If GitHub auth is unavailable, Codex must stop and print the exact command. Codex must not bypass GitHub permission and must not manually merge with unsafe git commands.

After merge, `main` must be checked out / synced, `scripts/v1-state.sh` must run, and P0-0 is effective only when `COMPLETION_EFFECTIVE_STATE: EFFECTIVE_MERGED_MAIN` and `P0_0_EFFECTIVE: YES` are reported. If PR #1004 remains open, `P0_1_ALLOWED: NO` remains expected.
