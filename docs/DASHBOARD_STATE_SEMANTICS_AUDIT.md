# Dashboard State Semantics Audit

## 1. Audit conclusion

This package audits the path from rule inputs to the Dashboard Home read model and fixes the verified semantic breaks. The result remains review-only and fail-closed:

- Asset state, market bias, confidence, risk, opening worthiness, AI review, conflict blocking, execution-plan readiness, and position-monitor state are separate concepts.
- The authoritative asset state table is preferred over a decision snapshot; the snapshot is a compatibility fallback only.
- Market bias is derived independently for each symbol from the three persisted closed bars currently read for each of 5m, 15m, 1h, and 4h. This is a provisional `MarketBiasPolicy` mapping pending product-owned window and threshold rules, not a validated trend-strength model. Missing or invalid inputs return `WAIT`; no missing-data path defaults bullish.
- A data-quality score below the current engine threshold of 60 blocks a usable review plan and downgrades the user-facing result to `WAIT`, low confidence, high risk, and not worth opening.
- AI roles that are disabled, not called, timed out, failed, unavailable, or abstained are not counted as support. AI consistency and AI plan mode require at least one successful `SUPPORT` or `CHALLENGE`; successful calls that all return `ABSTAIN` remain not applicable.
- Only `SUCCESS` role payloads may populate role-level business fields. Every non-success role renders only its run status and status explanation; successful `ABSTAIN` renders a compact no-judgment conclusion without direction or plan claims.
- A conflict score above zero is not a directional block. Dashboard directional-block counts use the authoritative threshold of 85.
- A usable execution plan requires the authoritative asset-state `trace_id` to match the plan decision's `AnalysisRun.trace_id`. A mismatch leaves current state visible but clears the plan with `状态已更新，原计划需重新分析`.
- Plan validity is evaluated from offset-aware `validFrom`/`expiresAt` in the Home read model. A legacy absolute range without an offset is `LEGACY_TIMEZONE_UNVERIFIED` and fails closed; it is never assumed to be UTC. Missing or malformed validity also fails closed, and `now >= expiresAt` blocks the plan.
- Plan `validFrom`/`expiresAt` remain the offset-aware authority. The historical no-timezone `tm_push_snapshot.expires_at` column is a UTC-naive compatibility timestamp: every producer and consumer converts through UTC, uses a UTC clock, and treats `now >= expiresAt` as expired.
- `/api/dashboard/home` is the sole data authority for Home positions, execution suggestion, AI roles, consistency, asset tiles, and top KPIs. `/api/dashboard/detail` updates only the lower workbench and diagnostics and cannot overwrite those Home regions.
- Asset-tile, sidebar, and search selection all set the selected symbol and request a fresh Home payload. A failed Home request clears cached business conclusions and displays `首页数据暂不可用` / `等待重新同步` / `当前不展示执行计划`.
- An active manual position takes over the selected asset's main suggestion area. Position Monitor writes a trusted source only from an explicitly typed `EXECUTION_PLAN:<id>` reference, or from `ANALYSIS:<id>` when that analysis has exactly one plan; legacy untyped `sourceRefId` values remain diagnostic-only. Dashboard then resolves only from the same-position monitor log's verified source. Zero/multiple plans, missing runs, and symbol mismatches fail closed; neither layer selects a latest sibling or latest same-symbol decision. A verified original plan is retained only as a collapsed review/reference record.
- `ExecutionPlanReviewPolicy` is the single boundary/status interpretation used by Plan Service, Position Monitor, and Dashboard Home. Null, blank, `暂无`, `—`, and `待生成` are all non-concrete boundaries. A user's populated stop/take-profit values cannot upgrade an incomplete or revalidation-required source plan.
- `PositionPlanSourceResolver` is the shared read-only identity resolver for active monitoring, Dashboard original-plan review, and closed-position review. It accepts only an exact typed plan or a typed analysis with exactly one plan, then verifies the plan/run/analysis/symbol chain. Closed-position feedback uses `USER_POSITION_<id>` when identity is unverified and never attaches to a foreign analysis.
- `POSITION_SOURCE_UNVERIFIED` remains an internal non-null persistence sentinel only. Public monitor DTOs, the monitor-log API, Dashboard, and Review Center expose `sourceVerified=false`, `sourceStatus=UNVERIFIED`, and `sourceStatusLabel=来源不可验证`, with analysis/plan IDs cleared.
- Dashboard Home does not invent system stop/take-profit values or a next monitor time. It displays persisted monitor time only.

Production deployment readiness remains **BLOCKED**. This package adds no order, auto-open, auto-close, auto-reverse, auto-trading, position mutation, external Push, or Telegram capability.

## 2. Authority and conflicts

Authority was resolved in this order:

1. Runtime enums, policy classes, persisted owner tables, and current service contracts.
2. `docs/GLOBAL_STATE_TRANSITION_MATRIX.md`, which explicitly distinguishes implemented lifecycle states from enum-only states.
3. Current read-only Dashboard and Position Monitor contract documents.
4. Historical phase notes, which provide context but do not override current runtime contracts.

Verified source contracts:

| Contract | Current authority | Result |
|---|---|---|
| Eight asset states | `AssetStateEnum`, authoritative asset-state persistence, `GLOBAL_STATE_TRANSITION_MATRIX.md` | Enum, API, and UI mapping covers all eight values. The lifecycle is not complete: `WAITING_TRIGGER` and `TRIGGERED` have no normal production writer. |
| Eight market-bias labels | `MarketBiasEnum`, provisional `MarketBiasPolicy` | Enum and label coverage exists. Runtime currently reads three bars per timeframe; no product-owned numeric/window specification proves this as a trend-strength model. |
| Data-quality opening gate | `DecisionEngineService`, `ReviewReasonsBuilder`, monitor/baseline guards | Current engine threshold is 60. Historical/future notes mentioning 70 do not match the active decision contract. |
| Confused thresholds | `ConfusedStatePolicy` | Enter at 70; directional push blocked at 85; exit requires two consecutive cycles below 55. |
| AI roles and safety | AI role payload/orchestrator/conflict resolver contracts | Rule direction remains authoritative; AI is review-only and cannot create plans, positions, or orders. |
| Position Monitor | `PositionMonitorResultDTO`, monitor log enums/services, `ExecutionPlanReviewPolicy` | Monitor writes logs only; it never closes, reduces, reverses, or otherwise mutates a position. Invalid/blocked plans yield `PLAN_INVALIDATED`; revalidation, incomplete/review-only status, or missing exact boundaries yield `LOGIC_WEAKENED` and `RECHECK_PLAN`. |
| Position original-plan identity | `PositionMonitorSourceContract`, `PositionPlanSourceResolver`, latest same-position monitor log, `ExecutionPlanMapper`, `AnalysisRunMapper` | Active monitoring, Dashboard, and closed review share one resolver. Only `EXECUTION_PLAN:<id>` and `ANALYSIS:<id>` are typed sources; plan/run/analysis/symbol agreement is mandatory, analysis-only sources require exactly one plan, and no latest/sibling/symbol fallback exists. |
| Monitor source presentation | `PositionMonitorLogSourceViewPolicy`, `PositionMonitorLogDTO` | The database may retain `POSITION_SOURCE_UNVERIFIED` to satisfy the internal non-null contract. User-facing DTO/API/review paths clear it and expose structured `UNVERIFIED` plus `来源不可验证`; the raw sentinel is never user business copy. |
| Execution plan | `ExecutionPlanVO`, exact `ExecutionPlanDO`, `ExecutionPlanReviewPolicy`, plan/boundary services, Dashboard Home gate | `VALID` metadata does not prove actual boundaries. Entry, stop, and take-profit must be concrete on the exact plan row; null/blank/`暂无`/`—`/`待生成` yields `PLAN_INCOMPLETE`. Historical boundary/leverage/position/invalidation fields come from that exact plan, never a joined sibling decision. |

Repository conflicts and limitations are not hidden:

- `GLOBAL_STATE_TRANSITION_MATRIX.md` records that there is no central legal transition graph for all eight asset states.
- `WAITING_TRIGGER` and `TRIGGERED` are enum/schema/UI-complete but have no normal production writer.
- The product vocabulary defines eight bias levels, but no authoritative numeric trend-strength threshold or window specification was found. `MarketBiasPolicy` is marked as a temporary implementation pending product confirmation. It currently compares the first open with the last close across only three bars per timeframe.
- Position Monitor has persisted `monitoredAt`, but no authoritative per-row `nextMonitorAt` source. Dashboard leaves the next time empty instead of using a page-refresh countdown.

### 2.1 Home rendering ownership

`renderDashboardHomePayload(home)` is the only successful Home main-surface entry point. It delegates to the `renderHome*FromPayload` functions for KPIs, assets, positions, execution, and AI. `requestDetailForSelectedSymbol()` is intentionally limited to the detail workbench and diagnostic fetches. The old `renderHomeDashboardRows`, `renderHomePosition`, `renderHomeExecution`, `renderHomeAiDecision`, and `renderTiles` functions were removed, so the current Home call graph has no compatibility path that can reintroduce stale plan or AI data.

`renderDashboardHomeUnavailable()` is the only exceptional Home writer. It clears cached Home decisions and writes explicit fail-closed empty states. The summary endpoint may continue to refresh diagnostics, but its call graph has no Home semantic renderer.

## 3. Semantic matrix

### 3.1 Asset state

| 业务概念 | 方案定义 | 内部枚举/字段 | API 字段 | 当前前端显示 | 当前问题 | 修正结果 |
|---|---|---|---|---|---|---|
| 观察 | 普通观察状态 | `OBSERVING` | `assetState`, `assetStateLabel` | 观察 | Previously reconstructed only from a decision snapshot. | Authoritative state row is preferred; snapshot is fallback only. |
| 候选 | 条件形成但仍需人工确认 | `CANDIDATE` | `assetState`, `assetStateLabel` | 候选 | Could be mixed with direction/worth-opening copy. | State, bias, and worthiness are rendered separately. |
| 等待触发 | 候选条件等待触发 | `WAITING_TRIGGER` | `assetState`, `assetStateLabel` | 等待触发 | UI support exists without a normal producer. | Enum/API/UI mapping only; lifecycle producer remains missing. |
| 已触发 | 触发条件成立 | `TRIGGERED` | `assetState`, `assetStateLabel` | 已触发 | UI support exists without a normal producer. | Enum/API/UI mapping only; lifecycle producer remains missing. |
| 高风险观察 | Risk gate requires observation/review | `HIGH_RISK` | `assetState`, `assetStateLabel` | 高风险观察 | Low data quality could coexist with a normal-looking plan. | Low quality and high risk now block a usable plan. |
| 已失效 | Current candidate/plan no longer valid | `INVALIDATED` | `assetState`, `assetStateLabel` | 已失效 | Boundary completeness could still dominate display. | Asset-state gate blocks plan fields. |
| 冷却 | Post-conflict/event cooling | `COOLING` | `assetState`, `assetStateLabel` | 冷却 | Could be inferred from stale snapshot data. | Authoritative state is used first. |
| 冲突状态 | Confused policy entered | `CONFUSED` | `assetState`, `assetStateLabel` | 冲突状态 | Mixed Chinese/English wording and score/block conflation. | Chinese label; enter state and directional block remain separate. |

### 3.2 Market bias, confidence, and risk

| 业务概念 | 方案定义 | 内部枚举/字段 | API 字段 | 当前前端显示 | 当前问题 | 修正结果 |
|---|---|---|---|---|---|---|
| 强偏多 | Current four-timeframe mapping is unanimously bullish | `STRONG_BULLISH` | `marketBias`, `marketBiasLabel` | 强偏多 | Direction used a short/latest-candle shortcut. | Provisional mapping uses three closed bars per timeframe; not a validated trend-strength claim. |
| 偏多 | Three of four current timeframe mappings are bullish | `BULLISH` | same | 偏多 | Intermediate levels were not produced consistently. | Deterministic but temporary four-timeframe mapping pending product confirmation. |
| 弱偏多 | Bullish windows exceed bearish without 3/4 agreement | `WEAK_BULLISH` | same | 弱偏多 | Missing data could default bullish. | Any missing/invalid timeframe returns `WAIT`. |
| 震荡 | Bullish and bearish timeframe counts tie | `RANGE` | same | 震荡 | Range could be collapsed into a binary direction. | Preserved as its own final result when quality is sufficient. |
| 弱偏空 | Bearish windows exceed bullish without 3/4 agreement | `WEAK_BEARISH` | same | 弱偏空 | Intermediate levels were not produced consistently. | Deterministic four-timeframe level. |
| 偏空 | Three of four current timeframe mappings are bearish | `BEARISH` | same | 偏空 | Direction-family consumers expected exact binary values. | Consumers recognize the bearish family; upstream mapping remains provisional. |
| 强偏空 | Current four-timeframe mapping is unanimously bearish | `STRONG_BEARISH` | same | 强偏空 | Direction-family consumers expected exact binary values. | Consumers recognize the bearish family; this is not evidence of a validated strength model. |
| 观望 | Missing/invalid structure or unified quality gate failure | `WAIT` | same | 观望 | Missing data or low quality could retain bullish output. | Fail-closed final user bias; raw rule bias stays diagnostic-only. |
| 高/中/低置信 | Decision confidence after gates and AI review | `HIGH`, `MEDIUM`, `LOW` | `confidence`, `confidenceLabel` | 高/中/低 | Low quality could still show high confidence. | Quality failure forces low confidence. |
| 低/中/高/极高风险 | Current selected-asset decision risk | `LOW`, `MEDIUM`, `HIGH`, `EXTREME` | `riskLevel`, `riskLevelLabel` | 低/中/高/极高 | Header used an aggregate maximum while other panels used selected asset. | Header and selected modules use the selected decision snapshot. |

### 3.3 AI run, conflict, and plan mode

| 业务概念 | 方案定义 | 内部枚举/字段 | API 字段 | 当前前端显示 | 当前问题 | 修正结果 |
|---|---|---|---|---|---|---|
| AI 成功 | Role call succeeded | `SUCCESS` | `runStatus`, `runStatusLabel` | 复核成功 | Consistency booleans defaulted true. | Success is counted only from actual successful role calls. |
| AI 部分成功 | At least one but fewer than all roles succeeded | `PARTIAL_SUCCESS` | same | 部分角色复核成功 | Missing roles could be treated as support. | Missing roles are excluded from support counts. |
| 成功支持 | Successful role stance supports rule | `SUPPORT` | role `stance`/labels | 成功支持 | `3 - objectionCount` treated non-objection as support. | Explicit successful-support count only. |
| 成功反对 | Successful role challenges rule | `CHALLENGE` | same | 成功反对 | Failed roles could influence conflict synthesis. | Explicit successful-objection count only. |
| 成功弃权 | Successful role declines a stance | `ABSTAIN` | same | 成功弃权 | Abstention could be interpreted as agreement. | Abstention is neither support nor objection. |
| 未调用 | No role call was made | `NOT_CALLED` | `runStatus`, `runStatusLabel` | 未调用 | Could display three-role consistency/confirm plan. | AI consistency and AI plan mode become not applicable. |
| 已禁用 | Provider/orchestrator disabled | `DISABLED` | same | 已禁用 | Disabled could inherit true consistency defaults. | Defaults are false; disabled is not support. |
| 超时 | Provider timeout | `TIMEOUT` | same | 调用超时 | Timeout could be treated as absence of objection. | Timeout is excluded from support and conflict votes. |
| 失败 | Provider/response failure | `FAILED`, `INVALID_RESPONSE` | same | 调用失败 | Failure could be treated as absence of objection. | Failure is excluded from support and conflict votes. |
| 预算阻断 | Budget/rate limit prevented call | `BUDGET_BLOCKED`, `RATE_LIMITED` | same | 预算阻断 | Internal code could leak. | Structured Chinese label, no raw fallback. |
| 模型不可用 | Model is unconfigured/unavailable | `MODEL_UNAVAILABLE`, `NOT_CONFIGURED` | same | 模型不可用 | Internal code could leak. | Structured Chinese label, no raw fallback. |
| 不适用 | Zero adjudicative AI roles, including all-success/all-`ABSTAIN` | null conflict/mode | `consistencyLevelLabel`, `planModeLabel` | 不适用 | Successful calls could be mistaken for a usable consensus. | No synthetic score or mode; all-abstain summary is `AI 成功返回，但所有角色均因证据不足而弃权`. |
| 无显著分歧 | Successful roles materially agree | `LEVEL_1_CONSISTENT` | `conflictLevel`, label | 无显著分歧 | Could be produced with zero success. | Requires actual successful role evidence. |
| 轻微分歧 | Review-level disagreement | `LEVEL_2_LIGHT_DIVERGENCE` | same | 轻微分歧 | Aliases leaked as raw enums. | Dedicated mapping accepts current aliases. |
| 显著分歧 | Material disagreement | `LEVEL_3_SIGNIFICANT_DIVERGENCE` | same | 显著分歧 | Raw enum leaked. | Chinese label only. |
| 极端分歧 | Strong contradiction/review block | `LEVEL_4_EXTREME_DIVERGENCE` | same | 极端分歧 | Raw enum leaked. | Chinese label only. |
| 完整/确认复核 | AI review mode, never execution authorization | `FULL`, `CONFIRM` | `planMode`, `planModeLabel` | 完整计划复核/确认复核 | Could appear when AI never ran. | Hidden/not applicable with zero successful roles. |
| 准备/建议/仅复核 | Downgraded manual review modes | `PREPARE_ONLY`, `ADVISORY`, `REVIEW_ONLY` | same | 仅准备复核/人工复核建议/仅供人工复核 | Raw enum could leak. | Dedicated labels and unknown fail-closed display. |
| 降级复核 | Conflict/risk downgrade | `DOWNGRADED`, `REDUCED` | same | 已降级复核/降级复核 | Downgrade reasons could expose English codes. | User-facing reason mapping is Chinese and conservative. |
| 冲突/等待/阻断/无计划 | No confirmable AI plan | `CONFUSED`, `WAIT`, `BLOCKED`, `NO_PLAN` | same | 冲突阻断/等待更多证据/已阻断/暂无计划 | Could look executable. | Remains review-only and does not expose plan fields. |

### 3.4 Position Monitor

| 业务概念 | 方案定义 | 内部枚举/字段 | API 字段 | 当前前端显示 | 当前问题 | 修正结果 |
|---|---|---|---|---|---|---|
| 逻辑成立 | Entry logic remains valid | `LOGIC_VALID` | `entryLogicStatus`, label | 入场逻辑仍成立 | Raw enum leaked. | Backend label plus dedicated frontend mapping. |
| 逻辑减弱 | Original entry evidence weakened | `LOGIC_WEAKENED` | same | 入场逻辑减弱 | Raw enum leaked. | Chinese business label. |
| 计划失效 | Original plan invalidated | `PLAN_INVALIDATED` | same | 原计划已失效 | Could be shown as new entry advice. | Position mode owns main panel; old plan is reference only. |
| 风险升高 | Position risk requires manual review | `HIGH_RISK` | same | 风险升高 | Could imply an automatic action. | Manual-review wording only. |
| 等待首次监控 | No persisted monitor log | `WAITING_MONITOR` | status plus labels | 等待首次监控 | Technical sync state leaked. | Explicit empty state, not a fake monitor result. |
| 暂无下次监控排期 | A monitor log exists but no next schedule is persisted | null `nextMonitorAt` with non-null `lastMonitorAt` | `lastMonitorAt`, `nextMonitorAt` | 暂无下次监控排期 | Could incorrectly say first monitoring had never happened. | `等待首次监控` is reserved for positions with no monitor history. |
| 方向支持 | Direction remains supported | `SUPPORTED` | `directionSupportStatus`, label | 当前方向仍获支持 | Raw enum leaked. | Dedicated mapping. |
| 方向减弱 | Direction support weakened | `WEAKENED` | same | 方向支持减弱 | Raw enum leaked. | Dedicated mapping. |
| 方向不支持 | Current direction no longer supported | `NOT_SUPPORTED` | same | 当前方向不再获支持 | Raw enum leaked. | Dedicated mapping. |
| 方向风险阻断 | Risk blocks directional conclusion | `RISK_BLOCKED` | same | 方向结论受风险阻断 | Could be confused with asset `CONFUSED`. | Separate label and field. |
| 无反转信号 | No monitor evidence of reversal | `NO_REVERSAL_SIGNAL` | `reversalStatus`, label | 暂无反转信号 | Raw enum leaked. | Dedicated mapping. |
| 反转人工复核 | Reversal risk needs human review | `MANUAL_REVIEW_REQUIRED` | same | 需人工复核反转风险 | Could imply automatic reverse. | Review-only wording and safety flags preserved. |
| 高风险变化复核 | High-risk change needs review | `RISK_REVIEW` | same | 需复核高风险变化 | Could imply automatic close/reduce. | Review-only wording and safety flags preserved. |
| 人工动作建议 | Hold/review/recheck risk only | `HOLD`, `MANUAL_REVIEW`, `TIGHTEN_STOP_REVIEW`, `REDUCE_POSITION_REVIEW`, `RECHECK_PLAN`, `RISK_REVIEW` | `suggestedManualAction`, label/text | Chinese manual-review labels | Internal enum or imperative wording leaked. | Every action is explicitly manual/review; no mutation path added. |

### 3.5 Data quality and execution-plan status

| 业务概念 | 方案定义 | 内部枚举/字段 | API 字段 | 当前前端显示 | 当前问题 | 修正结果 |
|---|---|---|---|---|---|---|
| 数据充分 | Score is present and at least 60 | `SUFFICIENT` / score | `dataQualityScore`, label | 数据充分 | Quality only changed worthiness. | Unified gate covers bias, confidence, risk, state, AI plan mode, and plan display. |
| 数据部分可用 | Evidence exists but is degraded | `PARTIAL` | status/label | 数据部分可用 | Internal state could leak. | Dedicated label. |
| 数据不足 | Score below 60 | `INSUFFICIENT` | status/label | 数据不足 | Full plan fields could still appear. | Final bias `WAIT`; low confidence; high risk; no usable plan. |
| 数据缺失 | No current score/snapshot | `MISSING` | status/label | 数据缺失 | Could be combined with stale plan data. | No usable plan; selected snapshot must exist. |
| 数据源不可用 | Required source unavailable | `UNAVAILABLE` | status/label | 数据源不可用 | Technical code could leak. | Dedicated label and fail-closed plan. |
| 可用人工复核计划 | All business and boundary gates pass | `USABLE_REVIEW_PLAN` | `executionSuggestion.status`, `validFrom`, `expiresAt` | 完整只读计划 fields | Completeness alone previously allowed display. | Checks quality, state, state/run trace match, risk, confused, directional block, worthiness, boundary, absolute expiry, position, timeframe, and analysis ID. |
| 当前持仓监控 | Selected symbol has active manual position | `POSITION_MONITORING` | same plus `positionMonitor` | 持仓监控主视图 | New entry plan remained primary. | Actual entry/current/PnL/user stops and monitor state take over; only a source-verified original plan may collapse below it. |
| 原计划来源不可验证 | No trusted monitor source, analysis has zero/multiple plans, or any source join fails | `originalPlanIdentity=UNVERIFIED` | identity/validity plus empty plan fields | 暂无可关联的原执行计划 | Latest sibling or same-symbol decision could be mislabeled as the position's original plan. | No latest-plan selection, symbol fallback, `sourceRefId` guessing, plan fields, or empty original-plan table. |
| 原计划身份已确认、当前非活动 | Exact monitor source joins to same-symbol plan/decision/run, but the plan status, source gate, revalidation flag, validity, or current-state trace blocks current use | `originalPlanIdentity=VERIFIED`; validity is `REVALIDATION_REQUIRED`, `PLAN_INVALID`, `PLAN_BLOCKED`, `PLAN_INCOMPLETE`, `STATE_MISMATCH`, `EXPIRED`, `TIMEZONE_UNVERIFIED`, or another review state | identity and current validity are separate | Backend historical-review label plus collapsed plan | Identity and current executability were conflated. | Historical plan A remains review-only and can never become a new executable suggestion or be replaced by plan B. |
| 阻断/缺失/不匹配 | Any required gate fails | `NO_DECISION`, `DATA_QUALITY_BLOCKED`, `ASSET_STATE_BLOCKED`, `STATE_SNAPSHOT_MISMATCH`, `STATE_SNAPSHOT_UNVERIFIED`, `RISK_BLOCKED`, `CONFLICT_BLOCKED`, `DIRECTION_BLOCKED`, `BOUNDARY_INCOMPLETE`, `VALID_PERIOD_INVALID`, `PLAN_EXPIRED`, `ANALYSIS_MISMATCH` | status, Chinese title/reason | 当前暂无完整执行计划 + Chinese reason | Direction/boundary fields could survive a blocked state. | Blocked response clears direction, entry, stop, TP, leverage, position, validity, and invalid condition. |

## 4. Exact root causes and corrections

| Observed problem | Root cause | Correction |
|---|---|---|
| Six assets appeared bullish | Binary latest/short-window direction and bullish-compatible defaults conflated missing data with direction. | Symbol-scoped 5m/15m/1h/4h inputs and missing-data `WAIT` are covered in tests. Current inputs are only three bars per timeframe, so no six-asset runtime difference or validated strength claim is made. |
| Quality 55 still looked confirmable | Quality gated `worthOpening` but not every downstream presentation field. | A single threshold-60 decision gate now downgrades all user conclusions and Dashboard plan output. |
| AI disabled looked consistent | Orchestrator consistency flags defaulted true and conflict support used `3 - objectionCount`. | Defaults are false; support/objection/abstain are explicit successful-role counts; zero success is N/A. |
| Conflict block count was inflated | Dashboard counted any `confusedScore > 0`. | Mapper query counts only scores at or above the directional block threshold 85. |
| Asset state could be stale | Dashboard reconstructed current state from decision snapshot JSON. | Authoritative asset-state table first; snapshot compatibility fallback is explicit. |
| Current state and old plan could be silently combined | Current asset state and latest decision were read by symbol without proving they came from the same analysis trace. | Compare authoritative state `trace_id` with the decision's `AnalysisRun.trace_id`; mismatch/unverified association blocks all plan fields. |
| Plan expiry was textual only | The gate only searched for `EXPIRED`/`已过期` markers and could display an elapsed absolute range. | Persist offset-aware `validFrom`/`expiresAt`, expose them as the authority, and block before activation or at/after expiry. Offset-aware compatibility ranges may be evaluated; legacy no-offset ranges return `LEGACY_TIMEZONE_UNVERIFIED`. |
| Detail responses could overwrite Home | The selected-symbol detail request still called legacy Home renderers after the Home payload was displayed. | Home and detail now have disjoint renderer call graphs; all three selection paths request Home first and detail only supplements the lower workbench. |
| Failed AI roles retained conclusions | Role payload fields were mapped even when the call status was disabled, failed, timed out, or not configured. | `resultAvailable` is true only for `SUCCESS`; non-success roles return before mapping any business fields, and successful abstention exposes no final direction or plan. |
| Modules contradicted selected asset | Some header values used first/average/max decisions. | Selected asset's decision supplies trend, risk, quality, AI conflict, execution, and AI review. |
| Strong/weak direction levels disappeared downstream | Reverse-position and Hot Reset mapper SQL recognized only exact `BULLISH`/`BEARISH`. | Mapper contracts now recognize all strong/normal/weak bullish and bearish family values while excluding `RANGE`, `WAIT`, and unknown states. |
| Position did not take over | Execution suggestion had no selected-position precedence. | Active selected manual position returns `POSITION_MONITORING`; original plan is review reference only. |
| Position showed a newer same-symbol plan as its origin | The position branch received the selected latest decision and reused its plan fields without proving position provenance. | The branch now receives a separately resolved original-plan object. It accepts only the latest same-position monitor's exact plan/analysis reference, verifies plan/decision/run analysis IDs and symbols, and never reads the new-opportunity decision while a position is active. |
| Monitor countdown was fake | Browser refresh cadence was rendered as next business validation. | Fake countdown removed; latest persisted log time is shown and unknown next time remains empty. |
| English/internal values leaked | Raw enum/free-text fallback was used across business fields. | Dedicated field-specific mappings return Chinese labels and unknown values return `未知状态`; generated boundary explanations are Chinese at source. |

## 5. Asset-card whitelist

The compact asset card now contains only:

1. Trading pair/symbol.
2. Asset state.
3. Final user market bias.
4. Confidence level.
5. Risk level.
6. Whether conditions are worth opening for manual review.
7. Current conclusion.

It does not display provider, data status, four-timeframe freshness, evidence count, analysis ID, trace ID, internal error code, database time, or debug text. Those may remain in diagnostic read models but are not compact-card content.

## 6. Position takeover contract

When the selected symbol has an active `OPEN` or `PARTIALLY_CLOSED` manual position, the main right panel prioritizes:

- Actual entry price, current quote, floating PnL, user stop, and user take profit.
- System suggested stop/take profit only when real backend values exist; otherwise `暂无`.
- Entry-logic status, direction support, reversal status, risk, and manual action.
- Persisted last monitor time and real next monitor time only when provided.

Original-plan identity and current validity are independent:

1. `PositionPlanSourceResolver` recognizes only `EXECUTION_PLAN:<id>` and `ANALYSIS:<id>`. Before returning trusted source IDs it verifies the exact plan, its analysis ID, the corresponding `AnalysisRun`, ID agreement, and the position/run symbol. Position Monitor, Dashboard, and closed-position review all call this resolver rather than maintaining separate validation strength.
2. An untyped/invalid/mismatched source still permits price/risk/manual monitoring, but the persisted log uses `POSITION_SOURCE_UNVERIFIED`, leaves `executionPlanId` empty, and keeps its `traceId` as monitor-run trace rather than source trace. At every external boundary that sentinel becomes a null analysis/plan ID with `sourceVerified=false`, `sourceStatus=UNVERIFIED`, and `sourceStatusLabel=来源不可验证`.
3. Dashboard identity is verified only from the latest monitor row for the same `positionId`. An `executionPlanId` selects that exact plan and must agree with the monitor `analysisId` when both exist. With only `analysisId`, `selectOnlyByAnalysisId` returns a plan only when exactly one row exists; zero or multiple rows are deliberately indistinguishable from an unverified source and no ordering is consulted.
4. Closed-position review resolves the typed position source through the same resolver. A verified result alone may expose plan boundaries, calculate execution deviation, or bind feedback to the real analysis. Failure yields `PLAN_CONTEXT_MISSING`, `NOT_COMPUTABLE`, empty plan fields, `PLAN_SOURCE_UNVERIFIED`, and feedback identity `USER_POSITION_<positionId>`.
5. The retained source model carries the exact `ExecutionPlanDO`, `AnalysisRunDO`, analysis ID, plan ID, and trace ID. Dashboard additionally joins the matching `DecisionResultVO`; all records must share identity and symbol before the historical plan is shown.
6. Legacy untyped `UserPosition.sourceRefId` is retained only for diagnostics. Dashboard never reads it as identity and Position Monitor never guesses it by query order.
7. Current validity follows this deterministic precedence: unverified identity; explicit plan/source-gate `INVALID`; plan/source-gate `BLOCKED`; `needsRevalidation`; status/source-gate `INCOMPLETE` or `REVIEW_ONLY`; actual exact-plan boundary incompleteness; current asset-state trace mismatch/unverified trace; decision time expiry or invalid timezone; then `ACTIVE`.
8. Entry, stop, take-profit, leverage, position suggestion, and invalid condition are copied only from the verified exact `ExecutionPlanDO`. The joined decision contributes direction and offset-aware validity only; it cannot fill an incomplete plan A with sibling plan B fields.
9. Revalidation copy is whitelist-only: `EXTREME_PRICE_MOVE`, `OI_COLLAPSE`, `LIQUIDITY_DRAIN`, and `SYSTEMIC_SHOCK` map to fixed Chinese labels. A Hot Reset event without a reason uses `热重置已触发重新验证`; every unknown/mixed/JSON/internal-code reason uses `重验证原因已记录，等待人工复核`.
10. A verified active source is labeled `原执行计划，仅用于持仓复核和复盘对照`. Any non-active state uses the backend historical-review label and never replaces plan A with a newer plan B.
11. An unverified source is labeled `暂无可关联的原执行计划`. Direction, entry, stop, take profit, leverage, position suggestion, validity, invalid condition, and source analysis ID are all empty, and the browser does not render an empty original-plan disclosure/table.

No historical original plan is executable, and no new leverage/position proposal is promoted in position mode.

## 7. Snapshot consistency

Dashboard Home resolves a selected decision once and uses it for selected-asset trend, risk, data quality, execution-plan gates, AI review, and conflict presentation. A plan must match the selected symbol/timeframe/source analysis ID, and the authoritative asset-state `trace_id` must equal the corresponding `AnalysisRun.trace_id`. A missing, unreadable, or mismatched trace association fails closed and clears all plan boundary fields while the current state remains visible.

This selected decision is the new-opportunity read model only. When an active position exists, its original plan is loaded through the separate position-source resolver described above. The selected latest same-symbol decision, current asset-state decision, first decision, and frontend cache are prohibited fallbacks for original-plan identity.

Global counters remain explicitly aggregate. The directional conflict-block counter is aggregate by definition and uses the 85 threshold; it is not presented as the selected asset's conflict score.

## 8. Test evidence

Focused tests cover:

- All eight asset-state enum/API/UI mappings; this does not prove a complete lifecycle.
- All eight market-bias labels under the current provisional three-bar-per-timeframe mapping.
- Independent symbol inputs and missing-data `WAIT` behavior.
- Low/missing data-quality plan blocking.
- AI disabled, failed, partial, support, objection, abstain, zero-success, and all-success/all-abstain semantics. `ABSTAIN` is neither support nor objection.
- Confused scores 0, 1, 69, 70, 84, and 85, including unknown output when the authoritative system-status read fails.
- Zero successful AI roles with asset directional blocking, confused score 100, and disabled AI remain `不适用`, never synthetic `极端分歧`.
- Position Monitor-to-Dashboard tests capture the actual monitor command/log: a typed exact plan A passes full plan/run/symbol validation and only A is shown; untyped analysis references with A/B plans write `POSITION_SOURCE_UNVERIFIED`, leave plan ID empty, and never reintroduce B.
- Position Monitor and Dashboard consume the same persisted-plan policy: `needsRevalidation` produces `LOGIC_WEAKENED`/`RECHECK_PLAN` plus `REVALIDATION_REQUIRED`, and missing/placeholder exact boundaries produce `LOGIC_WEAKENED`/`RECHECK_PLAN` plus `PLAN_INCOMPLETE`. Populated user stop/take-profit values do not upgrade either result.
- Position plan A remains isolated from a newer same-symbol decision B. Exact monitor plan ID works among siblings; typed analysis-only identity succeeds only for a single plan, while zero/multiple plans fail closed and never select the latest sibling.
- The open-monitor -> manual-close -> closed-review acceptance keeps exact plan A through both lifecycle reads while a mocked latest sibling B remains unused. Closed-review source mismatch, missing run, run/plan mismatch, and ambiguous analysis all clear the plan and keep feedback on `USER_POSITION_<id>`.
- Monitor-log service, controller, Review Center, and visible-template tests prove the internal `POSITION_SOURCE_UNVERIFIED` sentinel is persisted when required but absent from external DTO/API/DOM output; verified analysis/plan IDs remain intact.
- Missing/ambiguous `sourceRefId`, wrong-symbol sources, wrong-position monitor rows, and failed source joins hide every original-plan field instead of falling back by symbol.
- Verified historical plans cover revalidation/Hot Reset, invalid, blocked, incomplete/source-gate-incomplete, exact-boundary placeholders, trace mismatch, expiry, and timezone failure. `VALID` plus missing/`暂无`/`—`/`待生成` boundaries is `PLAN_INCOMPLETE`; plan A never borrows plan B fields.
- Revalidation copy tests cover the four known Chinese labels, Chinese-only Hot Reset fallback, mixed Chinese/internal codes, unknown JSON, and generic fail-closed copy without raw-code leakage.
- Position takeover, real monitor timestamps, no fake next-monitor countdown, the existing-monitor/no-next-schedule state, and Chinese labels.
- Plan analysis mismatch, state/run trace mismatch, blocked-field clearing, and absolute expiry before/equal/after/malformed cases.
- Final user-visible Home DOM copy checks for raw `pendingCount`, `degraded`, and local-real failure codes.
- Home/detail renderer ownership, asset-selection request order, fail-closed Home failure, and noninteractive `DEFAULT_SLOT` behavior through deterministic template call-graph assertions.
- Offset-aware plan validity under JVM defaults `UTC`, `Asia/Shanghai`, and `America/New_York`, plus legacy no-offset fail-closed behavior.
- Decision-to-Push Snapshot UTC-naive conversion under JVM defaults `UTC`, `Asia/Shanghai`, and `America/New_York`; all three preserve the same 24-hour expiry. Push Recheck is valid one second before expiry and expired at the exact boundary and one second after it.
- The AI conflict KPI and consistency card consume the same assembled backend consistency object; the KPI preserves the raw enum in `value` and emits its Chinese label in `valueLabel`.

The offline acceptance uses controlled service fixtures and the in-memory test database only. No live provider, external database, or six-asset runtime environment was used. Therefore this audit does not claim a fresh six-asset live-data result and does not fabricate one. Runtime evidence must be collected separately with real persisted symbol/analysis IDs. See `docs/DASHBOARD_INTERACTION_ACCEPTANCE.md`.

Browser and deterministic DOM-target rendering evidence is recorded separately in `docs/DASHBOARD_VISUAL_ACCEPTANCE.md`. The original browser pass covers ten scenarios, thirteen interactions, and CSS viewports 1920 x 1080, 1440 x 900, and 1366 x 768; Reviewer regressions add `ai-all-abstain` and verified/unverified position-source fixture/DOM assertions without fabricating new screenshots. Existing screenshots remain local `.runtime` evidence and are not real-market results.

## 9. Remaining gaps

1. The repository still lacks a central legal transition graph for all eight asset states.
2. `WAITING_TRIGGER` and `TRIGGERED` still lack normal production writers.
3. A formal product-owned numeric threshold/window specification for the eight market-bias levels is absent. `MarketBiasPolicy` is a temporary implementation pending product confirmation, reads only three bars per timeframe, and must not be described as a validated trend-strength model.
4. `nextMonitorAt` has no authoritative scheduler-derived per-position value, so Dashboard cannot display a real next time yet.
5. System stop/take-profit values have no real Position Monitor source in the current Home read model and remain blank rather than fabricated.
6. Historical engineering diagnostics still carry internal vocabulary inside `.runtime-status-stack` and `.diagnostics-only`, both of which are explicitly `display:none`. They are not part of the user-visible Home surface; they must be translated or moved to a dedicated diagnostic page before either hidden container is ever exposed.
7. Production readiness remains blocked by the repository's existing release-gate evidence requirements.
8. Flyway V7 adds the offset-aware plan columns, but this branch does not claim a fresh controlled PostgreSQL V1-V7 run. PostgreSQL migration evidence for V7 remains a separate production-readiness gate.

## 10. Safety boundary

All outputs remain informational and manual-review-only. This package does not create or modify `UserPosition`, generate or submit orders, execute a plan, change a stop, close/reduce/reverse a position, trigger a scheduler, call live AI/provider endpoints, or send Push/Telegram messages.
