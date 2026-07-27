# P3-U2 iPhone Home Semantic Contract

## Purpose

This contract freezes the mobile information semantics before any production
Swift or Dashboard implementation. It is a design/read-model contract only.
It does not add backend fields, routes, states, Provider calls, AI calls,
position writes, orders, messages, or trading behavior.

Design base: `168ef18c7ad148d960902c913f6ddb4b53318e14`.

FE-04 correction: `FE04_SEMANTIC_CONTRACT_V2.md` supersedes this
document's mobile-navigation, home-status ownership, asset-card interaction,
AI-analysis entry, message/Telegram, and asset-search wording. The current
backend remains authoritative for availability and field values.

## Global Rules

1. The current backend and Dashboard code outrank proposal text and historical
   screenshots.
2. Every visible value comes from `DashboardHomeVO` or an existing route/section
   target documented in the field map.
3. Normal prototype values use `{fieldName}` tokens. Screenshot capture mode
   uses safe empty-state copy while preserving the exact path in
   `data-field-token`; both modes are labelled `STATIC_LAYOUT_FIXTURE`.
4. Empty data stays empty. The mobile layer does not infer, repair, summarize,
   or manufacture a business result.
5. Unknown enum values display `状态待同步` or `未知状态`; raw values remain in
   controlled diagnostics only.
6. All AI, execution, and position-monitor content remains manual-review-only,
   not a trade instruction, not executable, and not auto-trading.

## A. Home Status Area

The home read model is a mixed status area with explicit ownership.

Current selected-asset status follows `selectedSymbol`:

1. 市场趋势
2. 风险等级
3. 数据质量分
4. AI 冲突等级

System summary status uses existing aggregate/header fields only:

1. AI 系统状态
2. 待复核机会
3. 冲突阻断统计
4. 热重置

The client must not relabel selected-asset values as system-wide values. The
candidate 持仓风险 aggregate still has no authoritative field and remains
`UNRESOLVED_FIELD`.

Presentation contract:

- compact status band without independent decorative card surfaces;
- no status icons, emoji, ring, or decorative chart;
- text remains the primary state signal;
- labels remain at least `12pt` at the standard prototype scale;
- state color is supplemental text emphasis only;
- long enum labels wrap and are never abbreviated into opaque fragments;
- raw values may appear only in a future controlled expansion, not in the
  default mobile summary.

## B. Realtime Alert And Key Event

These modules form the second information row.

- 17PM: alert and event panels sit side by side; alert receives the larger
  visual priority.
- 12PM: panels stack vertically, alert first.
- Default view: one compact alert summary and one compact event summary.
  A native disclosure retains capacity for a second row; the current backend
  event builder returns at most one row.
- Empty states are exactly `暂无告警` and `暂无关键事件`.
- The prototype uses only structural tokens. It does not invent CPI events,
  abnormal prices, timestamps, or countdowns.

## C. Watch Asset Pager

The home screen uses the backend-supplied watch assets in a horizontal
scroll-snap pager. The current home contract supplies three display slots, but
placeholder/default slots are never rendered as analyzed assets.

The card body displays exactly:

- 资产
- 最新价
- 方向
- 综合评分
- 置信度
- 风险等级
- 当前状态

17PM and 12PM show the same compact P0/P1 selector without a nested control.
Data source, freshness, timeframe freshness, evidence count, analysis time,
worth-opening opinion, complete evidence, score dimensions, AI output, and
unavailable reason stay outside the card body.

Selection contract:

- selecting the card body changes execution-advice scope;
- selecting the card body changes AI-evidence and consistency scope;
- selecting an asset does **not** change the position-monitor list;
- selecting the card body does not navigate;
- a separate `查看详情` affordance may enter FE-03 only with an authoritative
  `analysisId`; missing identity displays `当前不可查看`;
- each selector is one radio control bound to `assets[index]`;
- no selector may generate the nonexistent singular `asset[index]` path;
- placeholder/default slots are not treated as analyzed assets;
- no chart, candle, sparkline, or generated price series is present.

## D. Execution Advice

The module title remains `执行建议`.

Execution Advice is rule-led and source-verified. Returned evidence, score,
multi-timeframe, and available AI review/downgrade context may contribute to
the plan, but AI never originates a plan or replaces the rule-layer direction.
Missing required provenance or exact plan identity fails closed.

Default presentation is `COMPACT_SUMMARY`: backend status, blocked reason,
direction, and entry zone remain visible. Stop loss, take-profit rules,
leverage, position suggestion, validity, invalid condition, structured time
boundaries, and verified original-plan copy remain complete in one native
disclosure.

The mobile definition list uses only:

- direction;
- worth-opening opinion;
- entry zone;
- stop loss;
- take-profit rules;
- leverage suggestion;
- position suggestion;
- validity period;
- invalid condition;
- structured `validFrom` and `expiresAt`;
- backend status/blocked reason above the fields.

`validFrom/expiresAt` are plan-validity timestamps. They are not replaced by
`1H/4H` analysis timeframes. Returned conflict-block status and reasons belong
to the Execution Advice detail.

The mobile client does not add plan status, trigger conditions, execution cost,
liquidity state, funding rate, evidence summary, or action commands.

Fail-closed contract:

- if `status != USABLE_REVIEW_PLAN`, plan values remain empty and the backend
  status/blocked reason is shown;
- source-trace mismatch remains `状态已更新，原计划需重新分析`;
- malformed, expired, unsupported-timeframe, incomplete-boundary, high-risk,
  confused, direction-blocked, and not-worth-opening outcomes remain blocked;
- no mobile fallback selects another decision or plan;
- no field produces a UserPosition or order.

Long values occupy their own row. The module never uses a compressed desktop
table or horizontal scrolling.

## E. Position Monitor

Position monitor is a separate read model for active, manually entered
`UserPosition` rows. It is not a rendering of `ExecutionSuggestionVO`.

Home summary fields:

- 资产 / 方向
- 用户开仓价
- 入场逻辑
- 方向支持
- 反转状态
- 风险等级
- 当前建议
- 下次验证
- 人工处理入口

17PM shows at most three compact summaries; 12PM shows at most two. Each
summary defaults to asset/direction, risk, entry logic, direction support,
reversal state, and current advice; remaining confirmed fields are disclosed.
The repository has no authenticated full-position page route. The separate
view-all control is therefore `CONTRACT_UNRESOLVED`, visibly disabled, and
never self-links to fake navigation. The prototype does not implement a write.

### Product status mapping

| Raw contract | Display | Business meaning | Actionable |
|---|---|---|---|
| `LOGIC_VALID` | 入场逻辑仍成立 | Latest monitor still supports the recorded entry logic | Manual review only |
| `LOGIC_WEAKENED` | 入场逻辑减弱 | Supporting logic weakened | Manual review only |
| `PLAN_INVALIDATED` | 原计划已失效 | Historical plan no longer supports current use | Manual review only |
| `HIGH_RISK` | 风险升高 | Monitor risk rose | Manual risk review only |
| `WAITING_MONITOR` | 等待首次监控 | No monitor result exists | No |
| unknown | 状态待同步 | Mobile cannot safely interpret it | No |

| Raw contract | Display | Business meaning | Actionable |
|---|---|---|---|
| `SUPPORTED` | 当前方向仍获支持 | Latest monitor supports the position direction | Manual review only |
| `WEAKENED` | 方向支持减弱 | Support weakened | Manual review only |
| `NOT_SUPPORTED` | 当前方向不再获支持 | Plan was invalidated | Manual review only |
| `RISK_BLOCKED` | 方向结论受风险阻断 | Risk blocks the direction conclusion | Manual risk review only |
| `WAITING_SYNC` | 等待首次监控 | No trusted monitor result is available | No |
| unknown | 状态待同步 | Unknown contract | No |

| Raw contract | Display | Business meaning | Actionable |
|---|---|---|---|
| `NO_REVERSAL_SIGNAL` | 暂无反转信号 | No current reversal signal in the monitor result | Manual review only |
| `MANUAL_REVIEW_REQUIRED` | 需人工复核反转风险 | Plan invalidation requires review | Manual review only |
| `RISK_REVIEW` | 需复核高风险变化 | High-risk change requires review | Manual risk review only |
| `WAITING_MONITOR` | 等待首次监控 | No monitor result | No |
| unknown | 状态待同步 | Unknown contract | No |

If `lastMonitorAt` exists and `nextMonitorAt` is absent, the display is
`暂无下次监控排期`, not `等待首次监控`.

User-entered stop loss/take profit remain distinct from system-suggested plan
boundaries. Position source identifiers are not default user-facing content.

## F. AI Evidence Review

The module title remains `AI 证据复核`. A segmented control presents exactly:

1. 最终裁决官 (`GPT_FINAL`)
2. 冲突复核官 (`GEMINI_REVIEW`)
3. 反方挑战官 (`GROK_CHALLENGE`)

Only one role panel is visible at a time. Switching role does not change the
selected asset. Switching asset preserves the active role while changing the
AI field scope.

Each role defaults to a compact conclusion, risk/confidence where applicable,
and one or two role-specific evidence fields. All remaining confirmed role
fields stay available in the role disclosure; no responsibility is removed.

### GPT_FINAL

Displays final market bias, confidence, risk, plan mode, worth-opening opinion,
final conclusion, decision summary, core supporting evidence, core counter
evidence, and downgrade/block reason. These remain review evidence, not rule
override or execution authority.

### GEMINI_REVIEW

Displays review verdict, detected contradictions, weak evidence, logic gaps,
downgrade recommendation, risk-adjustment suggestion, manual-review flag, and
review conclusion. Empty lists stay empty.

### GROK_CHALLENGE

Displays challenge thesis, event risks, sentiment-reversal risks,
microstructure traps, liquidity risks, counter evidence, and challenge
conclusion. The prototype never invents news or event evidence.

### Missing and failed roles

- unavailable role: show only `runStatusLabel` and `statusMessage`;
- successful `ABSTAIN`: show the backend's evidence-insufficient conclusion;
- malformed or legacy unstructured payload: no role evidence;
- one role never borrows another role's content;
- AI disabled/not configured/timeout/failure remains visible and fail-closed.

## G. Adjudication Consistency

Adjudication consistency is a synthesis summary, not a fourth AI role. IA v2
places it in the AI module header rather than a separate large card.

Visible summary:

- 一致性等级
- 冲突等级
- 是否进入冲突阻断
- 一句话摘要

Expanded consistency fields are applicability, score, asset-direction block,
and downgrade reason. `finalPlanMode` is not a consistency field. Its only
valid path is `aiDecision.tabs[GPT_FINAL].finalPlanMode`, displayed in the
GPT_FINAL role disclosure.

`aiApplicable == false` takes precedence over `confused` and
`directionalPushBlocked`. In that state:

- consistency level: 不适用;
- conflict level: 不适用;
- final tendency: 暂无;
- score: `--`;
- directional asset block may still independently display 是 in expansion;
- the status must never become 极端分歧 merely because the asset is blocked.

No score ring is used. A number is shown only if
`consistency.consistencyScore` is an actual backend value.

## H. Bottom Navigation

The FE-04 target bottom navigation contains exactly:

1. 首页
2. 持仓
3. AI分析
4. 消息
5. 我的

Only 首页 currently has a complete mobile route. The remaining tabs retain
their target responsibility but must expose disabled, partial, or unavailable
states until their real route and data contract exist. A visible target tab is
not evidence of implemented capability.

观察资产 is not a primary tab. Search, analysis, and add-to-watch remain
explicit separate actions. 复盘 is contextual and requires an eligible
`CLOSED` UserPosition with exact `positionId`.

## Responsive Semantics

### iPhone 17 Pro Max

- measured logical size: `440 x 956pt`;
- measured safe area: `62 / 0 / 34 / 0pt` (top/right/bottom/left);
- alert and event panels share a row;
- selected asset card is 86% of content width;
- P0 and P1 asset fields remain visible;
- execution compact status, direction, and entry zone appear above the fold;
- up to three position summaries appear before “view all”.

### iPhone 12 Pro Max

- measured logical size: `428 x 926pt`;
- measured safe area: `47 / 0 / 34 / 0pt`;
- alert and event panels stack;
- selected asset card is 91% of content width;
- asset P1 fields remain in the single selector control;
- up to two position summaries appear before “view all”.

No responsive mode scales the whole Dashboard. Text remains at least the iOS
body baseline, controls retain a 44pt target, and long Chinese or enum text
wraps.

Rendered acceptance at standard text measured the 17PM execution core at
`732.4pt`, above the bottom-navigation top at `861pt`. The page has no
horizontal overflow: the watch section establishes layout/paint containment,
so only its pager owns horizontal scrolling.

## Review Checklist

### Semantics

- [x] Four selected-asset and four existing system-summary statuses have
  explicit ownership.
- [x] Candidate holding-risk aggregate is explicitly unresolved, not fabricated.
- [x] Watch cards contain no new field.
- [x] Card-body selection and the separate detail affordance are distinct.
- [x] Execution advice contains no new action semantics.
- [x] Position monitor and execution advice are separate.
- [x] AI role responsibilities are distinct.
- [x] One AI role is visible at a time.
- [x] Consistency is embedded in AI.
- [x] Empty states remain truthful.

### Layout and interaction

- [x] 17PM is primary.
- [x] 12PM has an independent stacked adaptation.
- [x] No whole-page scaling or desktop table compression.
- [x] No horizontal page overflow.
- [x] Asset selection scopes execution and AI.
- [x] Position content remains unchanged by asset selection.
- [x] Long text uses native expand/collapse.
- [x] Asset selection uses one radio-group control with no nested interaction.
- [x] Five target tabs retain truthful capability states; Review remains
  contextual.
- [x] Bottom navigation reserves the measured home-indicator inset.

### Visual and safety

- [x] No top-status icons.
- [x] No candle, line, price, or decorative market chart.
- [x] No consistency ring.
- [x] No fake market, position, event, or AI value.
- [x] No Provider, AI, Telegram, order, position mutation, or trading call.
- [x] Production readiness remains `BLOCKED`.
