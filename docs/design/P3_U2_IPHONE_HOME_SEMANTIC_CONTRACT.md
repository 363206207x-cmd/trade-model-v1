# P3-U2 iPhone Home Semantic Contract

## Purpose

This contract freezes the mobile information semantics before any production
Swift or Dashboard implementation. It is a design/read-model contract only.
It does not add backend fields, routes, states, Provider calls, AI calls,
position writes, orders, messages, or trading behavior.

Design base: `168ef18c7ad148d960902c913f6ddb4b53318e14`.

## Global Rules

1. The current backend and Dashboard code outrank proposal text and historical
   screenshots.
2. Every visible value comes from `DashboardHomeVO` or an existing route/section
   target documented in the field map.
3. Prototype values use `{fieldName}` tokens and are labelled
   `STATIC_LAYOUT_FIXTURE`.
4. Empty data stays empty. The mobile layer does not infer, repair, summarize,
   or manufacture a business result.
5. Unknown enum values display `状态待同步` or `未知状态`; raw values remain in
   controlled diagnostics only.
6. All AI, execution, and position-monitor content remains manual-review-only,
   not a trade instruction, not executable, and not auto-trading.

## A. Top Status

The authoritative home read model currently contains seven status cells:

1. 市场趋势
2. 风险等级
3. 数据质量分
4. AI 冲突等级
5. 待复核机会
6. 冲突阻断
7. 热重置

The task's candidate eighth item, 持仓风险, has no field in
`DashboardHomeVO.SystemStateVO`, no assignment in `buildSystemState`, no card
in `dashboard.html`, and no supporting test contract. It is therefore recorded
as `UNRESOLVED_FIELD` and is not rendered.

Presentation contract:

- compact four-column text grid;
- no status icons, emoji, ring, or decorative chart;
- text remains the primary state signal;
- state color is a thin supplemental accent only;
- long enum labels wrap and are never abbreviated into opaque fragments;
- raw values may appear only in a future controlled expansion, not in the
  default mobile summary.

## B. Realtime Alert And Key Event

These modules form the second information row.

- 17PM: alert and event panels sit side by side; alert receives the larger
  visual priority.
- 12PM: panels stack vertically, alert first.
- Default limit: two alert rows and two event rows. The current backend event
  builder returns at most one row.
- Empty states are exactly `暂无告警` and `暂无关键事件`.
- The prototype uses only structural tokens. It does not invent CPI events,
  abnormal prices, timestamps, or countdowns.

## C. Watch Asset Pager

The home screen contains exactly three asset cards in a horizontal
scroll-snap pager.

P0 summary:

- 资产
- 综合评分
- 方向
- 风险等级
- 是否值得开仓

P1 summary:

- 置信度
- 最新价

17PM shows P0 and P1. On 12PM, P1 moves into the native card expansion so the
P0 semantics remain readable. Data source, freshness, timeframe freshness,
evidence count, analysis time, and unavailable reason stay in a detail layer.

Selection contract:

- selecting an asset changes execution-advice scope;
- selecting an asset changes AI-evidence and consistency scope;
- selecting an asset does **not** change the position-monitor list;
- placeholder/default slots are not treated as analyzed assets;
- no chart, candle, sparkline, or generated price series is present.

## D. Execution Advice

The module title remains `执行建议`.

The mobile definition list uses only:

- direction;
- entry zone;
- stop loss;
- take-profit rules;
- leverage suggestion;
- position suggestion;
- validity period;
- invalid condition;
- backend status/blocked reason above the fields.

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

17PM shows at most three summaries; 12PM shows at most two. Additional rows use
the existing review/detail route. The prototype does not implement a write.

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
- AI 计划模式 (reused from GPT_FINAL)
- 是否进入冲突阻断
- 降级原因
- 一句话摘要

`aiApplicable == false` takes precedence over `confused` and
`directionalPushBlocked`. In that state:

- consistency level: 不适用;
- conflict level: 不适用;
- AI plan mode: 不适用;
- final tendency: 暂无;
- score: `--`;
- directional asset block may still independently display 是 in expansion;
- the status must never become 极端分歧 merely because the asset is blocked.

No score ring is used. A number is shown only if
`consistency.consistencyScore` is an actual backend value.

## H. Bottom Navigation

The fixed bottom navigation contains three truthful targets:

- 首页: existing `/dashboard` route;
- 持仓: in-page position-monitor anchor in this prototype;
- 复盘: existing `/review/dashboard` route.

The in-page anchor is an IA navigation device, not a new backend route. The
static prototype prevents navigation and performs no write.

## Responsive Semantics

### iPhone 17 Pro Max

- measured logical size: `440 x 956pt`;
- measured safe area: `62 / 0 / 34 / 0pt` (top/right/bottom/left);
- alert and event panels share a row;
- selected asset card is 86% of content width;
- P0 and P1 asset fields remain visible;
- up to three position summaries appear before “view all”.

### iPhone 12 Pro Max

- measured logical size: `428 x 926pt`;
- measured safe area: `47 / 0 / 34 / 0pt`;
- alert and event panels stack;
- selected asset card is 91% of content width;
- asset P1 fields move into expansion;
- up to two position summaries appear before “view all”.

No responsive mode scales the whole Dashboard. Text remains at least the iOS
body baseline, controls retain a 44pt target, and long Chinese or enum text
wraps.

## Review Checklist

### Semantics

- [x] Seven rendered top statuses come from current fields.
- [x] Candidate eighth status is explicitly unresolved, not fabricated.
- [x] Watch cards contain no new field.
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
- [x] Bottom navigation reserves the measured home-indicator inset.

### Visual and safety

- [x] No top-status icons.
- [x] No candle, line, price, or decorative market chart.
- [x] No consistency ring.
- [x] No fake market, position, event, or AI value.
- [x] No Provider, AI, Telegram, order, position mutation, or trading call.
- [x] Production readiness remains `BLOCKED`.
