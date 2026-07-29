# Trade Model V1 Interaction Contract V3

## 0. Contract Status

| Item | Value |
| --- | --- |
| Contract version | V3.1 / FE-04 semantic correction |
| Freeze date | 2026-07-28 |
| Audited repository HEAD | `d523dc3e69920d6dd80a0d49f344f86757eb7b9e` |
| Current work package | `FE-04 Information Architecture Freeze` |
| Contract result | `FROZEN_WITH_EXPLICIT_FAIL_CLOSED_GAPS` |
| Backend completeness | `PARTIAL` |
| Figma change in this task | None |
| Code/API/database change in this task | None |
| Production readiness | `BLOCKED` |

This document is the final interaction contract for the audited V1 capability.
It freezes what the product may display and how it may respond to user input.
It does not make a missing backend capability available.

Normative terms:

- **MUST / 必须**: required for a truthful implementation.
- **MUST NOT / 禁止**: violates the current backend or safety contract.
- **CURRENT**: supported by a current trustworthy backend contract.
- **PARTIAL**: real data exists, but coverage is incomplete.
- **FAIL_CLOSED**: only an unavailable, blocked, unverified, or historical
  presentation is allowed.
- **TARGET_ONLY**: a design reference that is not an implementation-ready
  successful flow.

### 0.1 Source precedence

When the inputs disagree, apply this order:

1. Current backend, schema, API, and safety contracts.
2. `docs/design/FE04_SEMANTIC_CONTRACT_V2.md` for corrected FE-04
   navigation and presentation ownership.
3. `docs/INTERACTION_BACKEND_AUDIT_V3.md`.
4. `docs/BACKEND_PRODUCT_AUDIT_REPORT.md`.
5. `docs/INTERACTION_SPEC_V2.md`.
6. Current Figma page structure.

Figma describes presentation structure. It does not prove that data,
persistence, navigation, or a successful action exists.

### 0.2 Current Figma structure reconciled

The read-only Figma inventory contains:

- `00 Design System`;
- `01 Overview Dashboard`;
- `01 Profile & Settings`;
- `01 V1 Product UI`;
- `Evidence & Scoring / Desktop Web`;
- `Evidence & Scoring / iPhone 430`;
- `Strategy & Monitoring / Desktop Web`;
- `Asset Detail / Desktop Web`;
- `Position Detail / Desktop Web`;
- `Review / Desktop Web`;
- `Mobile Home / iPhone 17 Pro Max`;
- `Mobile Asset Detail / iPhone 17 Pro Max`;
- `Mobile Position Monitor / iPhone 17 Pro Max`;
- `Mobile Push Detail / iPhone 17 Pro Max`;
- `Mobile Profile & Settings / iPhone 17 Pro Max`.

The following corrections are normative:

1. Any design helper that describes `triggered` as position active/opened is
   invalid. The correct meaning is `条件已触发，不代表已开仓`.
2. Profile/Settings switches and segmented controls are `TARGET_ONLY` unless
   backed by an authenticated read/update contract. They MUST NOT show a
   successful save.
3. Complete evidence or all-eight-score examples in Figma are layout fixtures
   unless the runtime response proves that coverage. Fixtures MUST be
   annotated `DESIGN_FIXTURE_NOT_RUNTIME`.
4. Existing Figma frames do not establish application routes by themselves.

## 1. Product Identity and Safety Contract

The UI MUST preserve these object identities:

```text
Decision
  = rule-led analytical conclusion

ExecutionPlan
  = system suggestion for manual review

UserPosition
  = manually recorded user fact

PositionMonitorLog
  = read-only monitoring result for one exact UserPosition
```

Global rules:

1. `ExecutionPlan != UserPosition`.
2. `tm_real_position != UserPosition`.
3. `TRIGGERED != opened`.
4. `analysisId != executionPlanId`.
5. A monitor suggestion is not proof that a human acted.
6. Push recheck is not trading authorization.
7. AI cannot replace the rule-layer base direction.
8. System stop, take-profit, leverage, and size fields MUST be labelled
   `系统建议 / 仅供复核`.
9. User entry price, quantity, leverage, stop loss, and take profit MUST come
   only from `UserPosition` and be labelled `用户记录`.
10. The UI MUST NOT expose buy, sell, order, execute, auto-open, auto-close,
    auto-reduce, or auto-reverse controls.

## 2. Page Navigation Structure

### 2.1 Desktop navigation

```text
Overview Dashboard
  |
  +-- Focus Asset ----------------------> Asset Detail
  |                                       |
  |                                       +--> Evidence & Scoring
  |                                       |
  |                                       +--> Strategy & Monitoring
  |                                            only with exact verified plan
  |
  +-- User Position / Monitor ----------> Position Detail
  |                                       |
  |                                       +--> Review
  |                                            only when position is CLOSED
  |
  +-- Alert / Push context -------------> Asset Detail or Position Detail
  |
  +-- Account/session affordance -------> Profile & Settings shell
                                          settings remain TARGET_ONLY
```

Implementation-ready primary navigation is:

- `首页`;
- `持仓`;
- `复盘`.

Asset Detail, Evidence & Scoring, Strategy & Monitoring, Position Detail, and
Push Detail are contextual drill-down pages. AI is a section within analysis
pages, not a top-level product destination.

### 2.2 Mobile navigation

The FE-04 target bottom navigation contains exactly:

- `首页`;
- `持仓`;
- `AI分析`;
- `消息`;
- `我的`.

Target presence and runtime availability are separate:

| Tab | Current capability |
| --- | --- |
| 首页 | `READY`: existing authenticated mobile Dashboard |
| 持仓 | `PARTIAL`: owner-scoped position reads exist; FE-04 product route is pending |
| AI分析 | `PARTIAL`: analysis create/detail exists; market-search landing is pending |
| 消息 | `PARTIAL`: reduced read/recheck records exist; complete inbox route is pending |
| 我的 | `PARTIAL`: minimal session/logout shell; settings and watch persistence are unavailable |

Contextual navigation:

- asset-card body -> set `selectedSymbol` and stay on Mobile Home;
- separate asset-detail affordance -> FE-03 Analysis Detail only with an
  authoritative `analysisId`;
- exact position card -> Mobile Position Monitor;
- notification deep link -> Mobile Push Detail;
- closed position -> Review;
- account/session affordance -> Profile shell.

观察资产 is not a primary tab. `复盘` remains contextual and is not a sixth
tab. A `PARTIAL` tab must show an unavailable/disabled state until its real
route exists; Figma presence does not authorize simulated success.

### 2.3 Cross-page identity

| Context | Contract |
| --- | --- |
| `selectedSymbol` | Selects asset context and may be preserved in URL state |
| `analysisId` | Opaque analysis context; never sufficient plan identity |
| `executionPlanId` | Required before exact plan values are claimed |
| `positionId` | Required for one exact position, especially when a symbol has multiple open positions |
| `traceId` | Controlled diagnostics only; hidden from default product UI |

Selecting an asset may update Decision, Evidence/Score, Execution Plan, and AI
content for that asset. It MUST NOT replace, mutate, or rebind the Position
Monitor DOM.

## 3. Page Contracts

### 3.1 Overview Dashboard

**Status:** `CURRENT`, with `FAIL_CLOSED` plan identity handling.

**Purpose**

Provide one-screen awareness of selected-asset and system status, risk, focus assets,
reviewable decisions, real user positions, and monitoring.

**Data sources**

- `GET /api/dashboard/home` and `DashboardHomeVO`;
- Dashboard status, alert, event, decision, and asset projections;
- exact `UserPosition` and monitor projections;
- stored three-role AI summaries.

**Displayable fields**

- selected-asset 市场趋势, 风险等级, 数据质量分, AI 冲突等级;
- system-summary AI 系统状态, 待复核机会, 冲突阻断统计, 热重置;
- alert priority, symbol, sanitized message, and time;
- focus asset symbol, current price, direction, composite score, confidence,
  risk, and asset state on the card body;
- short conclusion outside the card body when returned;
- selected-asset decision summary;
- exact verified execution suggestion only;
- user-position facts and monitor summary in a separate region;
- consistency summary and exactly three AI role summaries.

**Click entries and targets**

| Entry | Behavior | Target |
| --- | --- | --- |
| Focus asset card body | Set `selectedSymbol`; update decision/plan/AI only | Stay on Overview |
| Asset detail affordance | Require authoritative `analysisId` | FE-03 Analysis Detail |
| Evidence/score affordance | Preserve coverage and analysis context | Evidence & Scoring |
| Verified plan card | Preserve exact `executionPlanId` | Strategy & Monitoring |
| User-position card | Select exact `positionId` | Position Detail |
| Monitor summary | Preserve exact position context | Position Detail |
| Closed-position review entry | Preserve exact `positionId` | Review |
| AI role tab | Switch one role panel only | Stay on Overview |

**Fail-closed behavior**

- no analysis: `等待首轮分析`;
- no alerts/events/positions: section-specific truthful empty state;
- multiple same-symbol positions without `positionId`:
  `请选择具体持仓`;
- unverified plan identity:
  `当前暂无可验证的执行建议`, with all plan values hidden;
- refresh failure: clear stale selection-dependent conclusions before error.
- missing `analysisId`: disable detail affordance and show `当前不可查看`.

### 3.2 Evidence & Scoring

**Status:** `PARTIAL`.

**Purpose**

Explain why the system reached the returned rule-led conclusion without
claiming evidence or score coverage that the current read contract does not
provide.

**Data sources**

- analysis/decision context;
- persisted evidence and score projections;
- current normal top-three evidence and top-three score briefs;
- convergence summary;
- stored AI role payload and decision review reasons.

**Displayable fields**

- symbol, analysis timeframe, analysis time, data quality, and symbol-level
  asset state;
- returned evidence type, description, direction, and source when present;
- returned score type, value, and explanation when present;
- supporting, opposing/challenging, and neutral groups from returned items;
- multi-timeframe convergence summary;
- AI status, stance, conflict, reason codes, summary, fallback, and synthesis.

**Click entries and targets**

| Entry | Behavior | Target |
| --- | --- | --- |
| Evidence filter | Filter only loaded items | Stay on page |
| Returned evidence item | Reveal only returned description/source | Stay on page |
| Returned score item | Reveal only returned explanation | Stay on page |
| AI role tab | Switch exactly one stored role payload | Stay on page |
| Asset header | Preserve `selectedSymbol` and analysis context | Asset Detail or Overview |
| Exact verified plan link | Preserve exact plan identity | Strategy & Monitoring |

**Fail-closed behavior**

- top-three data: label `证据摘要（前3条）` or `评分摘要（前3条）`;
- absent evidence group in a partial brief:
  `当前摘要未包含此类证据`;
- no evidence detail: `详细证据尚未提供`;
- no full score list: returned items only; other dimensions show
  `-- / 详细评分尚未提供`;
- no role provenance:
  `当前仅提供角色结论摘要，完整证据关联尚未提供`.

### 3.3 Strategy & Monitoring

**Status:** `PARTIAL`, with strict identity separation.

**Purpose**

Show the system suggestion, manually recorded user position, and monitoring
result as three separate business objects.

**Data sources**

- `ExecutionPlanDO` / `ExecutionPlanVO`;
- `UserPositionDO` and public position reads;
- `PositionMonitorResultDTO` and monitor-log reads;
- trusted source resolution for exact position-plan linkage.

**Displayable fields**

Execution Plan:

- direction from the linked decision;
- worth-opening opinion from the selected asset/decision projection when
  explicitly supplied;
- entry zone, stop-loss zone, take-profit rules;
- leverage and position suggestions;
- `validFrom`, `expiresAt`, and invalidation condition;
- plan/source/revalidation status.

The plan remains rule-led. Returned evidence, score, multi-timeframe, and
available AI review/downgrade context may contribute, but AI cannot originate
the plan or replace the rule-layer direction. `validFrom/expiresAt` are plan
validity timestamps, never analysis timeframes.

User Position:

- asset, side, status, user entry price, quantity, leverage;
- user stop loss, user take profit, open/close facts.

Position Monitoring:

- logic status, direction support, reversal status, risk;
- current price/P&L when supplied;
- conclusion, reason codes, suggested manual action text;
- monitor time and next validation time when supplied.

**Click entries and targets**

| Entry | Behavior | Target |
| --- | --- | --- |
| Plan detail | Open returned plan detail only | Stay on page |
| Evidence link | Preserve exact analysis/plan context | Evidence & Scoring |
| User-position card | Select exact `positionId` | Position Detail |
| Monitor timeline | Open exact position history | Position Detail |
| Closed review entry | Require `CLOSED` | Review |

**Fail-closed behavior**

- exact plan unresolved: `暂无可验证的执行建议`;
- conflict blocked: show the returned block and returned reasons in plan detail;
- original position plan unresolved: `暂无可关联的原执行计划`;
- invalid/revalidation/expired plan: muted `仅用于历史复核`;
- monitor never run: `等待首次监控`;
- source unverified: `监控来源不可验证`;
- absent value: `--`;
- no execute/order/open-position control.

### 3.4 Asset Detail

**Status:** `PARTIAL`.

**Purpose**

Present one asset's current rule-led decision context and available
explanation. Asset Detail is also the AI-analysis entry surface.

**Data sources**

- Dashboard asset/decision projection;
- analysis identity and data-quality context;
- evidence/score briefs;
- convergence summary;
- stored AI role summaries;
- exact plan only when separately verified.

**Displayable fields**

- symbol, current price, direction, confidence, risk;
- composite score when supplied;
- symbol-level asset state and short conclusion;
- data quality;
- returned evidence/score briefs;
- convergence summary;
- exactly three role summaries/statuses.

**Click entries and targets**

- back -> Overview with `selectedSymbol`;
- evidence/score -> Evidence & Scoring with the same analysis context;
- exact plan -> Strategy & Monitoring;
- AI tab -> switch role panel in place.

**Fail-closed behavior**

- no full evidence: `当前展示证据摘要（前3条）`;
- no full scores: returned scores only; remaining dimensions `--`;
- no timeframe detail: `各周期明细尚未提供`;
- no exact plan: hide plan values and navigation;
- no role provenance: summary/reason codes only.
- no authoritative `analysisId`: do not enter FE-03 or select a sibling run.

### 3.5 Position Detail

**Status:** `CURRENT` for user facts and logs; `PARTIAL` for complete change
history.

**Purpose**

Inspect one exact manually recorded position and its monitoring lifecycle.

**Data sources**

- `GET /api/user-positions/{id}`;
- exact monitor result/log reads;
- `PositionPlanSourceResolver` trusted plan context;
- review eligibility and closed-position review summary.

**Displayable fields**

- UserPosition facts;
- verified original plan as a separate reference;
- current monitor result and monitoring timeline;
- returned risk changes and stored counts/presence;
- role-level change summary only when actually supplied;
- review entry for a closed position.

**Click entries and targets**

- monitor log -> expand exact log;
- verified plan -> historical/current plan context;
- closed position -> Review;
- authenticated manual full-close record flow when currently implemented.

**Fail-closed behavior**

- unverified plan: `暂无可关联的原执行计划`;
- counts/presence remain counts; no reconstructed evidence diff;
- no AI change detail: `本次监控未提供角色级变化明细`;
- no next schedule: `暂无下次监控排期`;
- P/L discloses exclusions such as fees, funding, and slippage;
- no partial-close control.

### 3.6 Review

**Status:** `PARTIAL`.

**Purpose**

Close the manual decision loop for one fully closed UserPosition while keeping
system suggestion, user execution facts, monitoring, and outcome separate.

**Data sources**

- `GET /api/review/user-positions/{positionId}/summary`;
- `POST /api/review/user-positions/{positionId}/feedback`;
- UserPosition facts;
- trusted exact plan context;
- monitor logs and available review projections.

**Displayable fields**

- exact original system suggestion when trusted;
- actual user entry/close facts;
- monitor history and logic/reversal/risk conclusions;
- actual outcome and bounded P/L;
- execution deviation when computable;
- manual error type, outcome, and adjustment suggestion;
- rule-version audit reference when supplied.

**Click entries and targets**

- monitor entry -> Position Detail;
- exact plan/evidence reference -> related read-only detail;
- feedback form -> authenticated manual feedback save.

**Fail-closed behavior**

- position not `CLOSED`: no review summary;
- plan context missing: `计划上下文缺失`;
- deviation unavailable: `执行偏差暂不可计算`;
- no monitor history: `暂无监控记录`;
- review absent: `待复盘`;
- feedback failure: preserve typed form content and show failure, never success;
- feedback MUST NOT claim automatic rule change or release.

### 3.7 Mobile Home

**Status:** `CURRENT`, using the same fail-closed rules as Overview.

**Purpose**

Prioritize risk awareness, the selected asset decision, current positions,
monitoring, and analysis entry on a phone.

**Data sources**

The same Dashboard, UserPosition, monitor, and AI read models as Overview. No
mobile-only business field exists.

**Displayable fields**

- top risk/status summary;
- one high-priority alert/event;
- one selected focus asset summary;
- decision and exact verified plan summary;
- independent UserPosition/monitor summary;
- exactly three AI role entry summaries.

**Click entries and targets**

- asset pager -> update selected asset decision/plan/AI;
- asset detail -> Mobile Asset Detail;
- position card -> Mobile Position Monitor;
- review nav -> Review.

**Fail-closed behavior**

Matches Overview. Only the focus-asset pager may scroll horizontally.

### 3.8 Mobile Asset Detail

**Status:** `PARTIAL`.

**Purpose**

Provide mobile access to the available decision explanation for one asset.

**Data sources**

The same Asset Detail and Evidence & Scoring contracts. Mobile MUST NOT
manufacture additional evidence or score detail.

**Displayable fields**

- asset/decision summary;
- returned supporting and opposing evidence;
- returned score items;
- convergence summary;
- exactly three AI role panels.

**Click entries and targets**

- evidence filter/row and score row -> in-place read interaction;
- AI tabs -> one role at a time;
- exact plan reference -> Strategy & Monitoring when verified.

**Fail-closed behavior**

Coverage labels, missing values, timeframe limits, and AI provenance rules are
identical to the desktop detail pages.

### 3.9 Mobile Position Monitor

**Status:** `CURRENT/PARTIAL`.

**Purpose**

Monitor one exact existing UserPosition on a phone.

**Data sources**

UserPosition, trusted plan reference, current monitor result, and monitor-log
reads.

**Displayable order**

1. User Position.
2. Current System Monitoring Result.
3. Verified Execution Plan Reference.
4. Monitoring History.
5. Review entry for a closed position.

**Click entries and targets**

- monitor history -> exact log detail;
- verified plan -> read-only plan context;
- closed-position review -> Review.

**Fail-closed behavior**

The plan never merges with user facts. Suggested action remains text. There
are no close, sell, order, or automatic controls.

### 3.10 Mobile Push Detail

**Status:** `CURRENT_REVIEW_ONLY`.

**Purpose**

Explain whether the original notification context still passes current
recheck rules.

**Data sources**

- authenticated shared server-side `OPPORTUNITY` public projection;
- exact current-user-scoped `POSITION_RISK` private projection;
- no frontend-composed or frontend-filtered privacy projection.

**Displayable fields**

- for `OPPORTUNITY`: exact public identity, safe allowlisted public opportunity
  status, public timestamp, and public description only;
- for `POSITION_RISK`: exact position identity plus returned owner-scoped
  monitoring risk, status, and reason;
- UserPosition, account risk, position risk, Recheck risk, `failReasonJson`,
  and private risk reasons are forbidden in the shared `OPPORTUNITY` payload.

**Click entries and targets**

- `查看现有证据` -> Evidence & Scoring or Mobile Asset Detail;
- `查看资产详情` -> Asset Detail;
- `查看监控状态` -> Mobile Position Monitor only with exact `positionId`.

**Fail-closed behavior**

- raw executable-sounding codes are mapped to review-only wording;
- public and private source DTOs remain separate;
- no position context means no monitor link;
- no execute, buy, sell, order, or create-position action.

### 3.11 Mobile Profile & Settings

**Status:** `PARTIAL / FAIL_CLOSED_FOR_UNSUPPORTED_SETTINGS`.

**Purpose**

Provide a future account/session and system-preference control center without
pretending unsupported settings persistence exists.

**Data sources**

- authenticated account/session information when returned;
- build/rule/system information when returned;
- existing logout flow;
- `/api/user-config/ping` proves service presence only, not settings
  read/update capability.

**Displayable fields**

- minimal account/session state;
- system/rule version only when supplied;
- logout;
- disabled notification, AI, and risk-preference references labelled
  `暂未开放`.

**Click entries and targets**

- logout -> existing authenticated logout flow;
- disabled setting row -> no save action; optional capability explanation.

**Fail-closed behavior**

- no editable notification, AI mode/frequency/model, token budget, or risk
  preference success;
- no fabricated remaining-token balance;
- the `我的` target tab may be shown, but unsupported rows remain disabled;
- no local-only persistence represented as account state.

### 3.12 Mobile Message Center

**Status:** `PARTIAL`.

**Purpose**

Provide one read-only entry for high-value product events without becoming a
trading or delivery-control surface.

**Business sources**

1. asset-pool opportunity;
2. exact UserPosition monitoring risk.

System notices may be shown as a separate informational category only when a
real record exists. They are not a third opportunity/risk Push source.

Telegram is `EXTENSION / PENDING_IMPLEMENTATION`. It is a future delivery
outlet only and must not be presented as connected, delivered, or actionable.

**Fail-closed behavior**

- no complete inbox route: target tab shows `当前不可查看`;
- delivery disabled: `通知送达能力未启用`;
- missing exact asset/position identity: no detail navigation;
- no buy, sell, close, order, execute, or authorization control.

### 3.13 AI Analysis And Asset Search

**Status:** `PARTIAL`.

**Target flow**

```text
search market asset
  -> create authoritative analysisId
  -> open FE-03 Analysis Detail
  -> explicit optional add-to-watch action
```

Analysis create/read and FE-03 detail capability exist. Complete market-asset
search and authenticated watch-asset persistence do not.

**Fail-closed behavior**

- no market-search contract: search is disabled or `暂未开放`;
- no watch persistence: add/manage action is disabled;
- no automatic add after search;
- no localStorage success, fake card, fabricated analysisId, or sibling-run
  fallback.

## 4. State Models

The client consumes backend states. It MUST NOT calculate or persist a
transition.

### 4.1 Asset state

The authoritative state is one current symbol-level state. A timeframe may be
shown as analysis context, but the UI MUST NOT present state as separately
authoritative for 4h, 1h, 15m, or 5m.

| State | UI meaning | Allowed interactions | Forbidden implications |
| --- | --- | --- | --- |
| `observing` | 当前条件不足，继续观察 | View available evidence/history | Current directional plan or position |
| `candidate` | 方向开始形成，仍需人工复核 | Inspect decision, evidence, and verified plan if present | Triggered, opened, or auto promotion |
| `waiting_trigger` | 等待后端返回的确认条件 | Review conditions, validity, blockers | Client countdown, triggered/opened claim |
| `triggered` | 条件已触发，不代表已开仓 | Inspect matched conditions and review suggestion | Order filled, opened position, inferred UserPosition |
| `high_risk` | 当前方向展示不安全 | Inspect risk reasons and recovery conditions | Emphasized plan direction or execution CTA |
| `invalidated` | 既有逻辑已失效 | Inspect reason and history | Reuse old plan or substitute sibling plan |
| `cooling` | 冷却中，方向输出受抑制 | Inspect current state/history | Countdown unless supplied, promotion, directional plan |
| `confused` | 冲突状态，需要人工复核 | Inspect rule evidence and three AI summaries | AI vote, dominant direction, plan action |

Known backend transition thresholds may be used in controlled explanation
only when returned by the contract:

- enter `confused` at score `>= 70`;
- direction/push block at score `>= 85`;
- leave `confused` only below `55` for two consecutive cycles, then enter
  `cooling`.

The UI MUST NOT run these thresholds locally to change state.

### 4.2 User position state

| State | Meaning | Allowed interactions | Forbidden interactions |
| --- | --- | --- | --- |
| `OPEN` | Manually recorded position remains open | Read facts, monitor, record full close through authenticated flow | Infer from plan, auto close/reduce/reverse |
| `PARTIALLY_CLOSED` | Backend recognizes a remaining open position | Read facts, continue monitoring, record full close | Offer or claim a public partial-close action |
| `CLOSED` | Manual close facts are recorded | Read history, optionally enter Review | Current monitoring run, forced review, automatic rule change |

`PARTIALLY_CLOSED` may be displayed only when returned. The current public API
does not provide a complete partial-close mutation lifecycle.

### 4.3 Execution suggestion state

Canonical persisted `ExecutionPlan` statuses:

| Status | UI contract |
| --- | --- |
| `VALID` | Reviewable system suggestion only; still non-executable |
| `INCOMPLETE` | Show missing requirements; unavailable numeric fields remain hidden/`--` |
| `BLOCKED` | Show block reason before all plan values |
| `REVIEW_ONLY` | Historical/manual-review context; never an order permission |
| `INVALID` | Historical muted display only; not reusable as current |

Projection-only presentation states such as `DATA_QUALITY_BLOCKED`,
`CONFLICT_BLOCKED`, and `DIRECTION_BLOCKED` are not additional persisted plan
statuses. They display `执行建议不可用` plus the returned reason and hide entry,
stop, take-profit, leverage, and sizing values.

Plan display gate, in order:

1. exact `executionPlanId`;
2. verified source relationship;
3. consistent analysis/plan relationship;
4. current validity and source gate;
5. no revalidation, invalidation, risk, conflict, direction, or expiry block.

Failure at any step is `FAIL_CLOSED`. The client MUST NOT select the latest,
first, or highest-ID sibling plan.

### 4.4 Push recheck state

Raw or compatibility status names MUST be translated to review-only product
language:

| Source meaning/code | User-facing state | Meaning |
| --- | --- | --- |
| `VALID_EXECUTABLE` or review passed | `复核通过` | Current recheck passed; not execution authorization |
| `VALID_WAITING` or waiting | `等待复核条件` | Current conditions are not yet complete |
| `DRIFTED` | `偏离入场区间` | Current market context drifted from the original snapshot |
| `INVALIDATED` | `已失效` | Original recommendation is no longer current |
| `RISK_BLOCKED` | `风险阻断` | Risk policy blocks current presentation |
| `CONFUSED_BLOCKED` | `冲突阻断` | Conflict policy blocks direction |
| `EXPIRED` | `已过期` | Snapshot validity window ended |

All Push states permit information review only. None permits plan execution,
position creation, or an order.

## 5. Fail-Closed Rules

Fail-closed precedence:

1. identity unverified or mismatched;
2. invalidation, source-gate block, or revalidation requirement;
3. risk, data-quality, or confused block;
4. expiry or stale data;
5. partial coverage;
6. current reviewable content.

| Condition | Required display | Forbidden behavior |
| --- | --- | --- |
| Required data object missing | `暂无可验证数据` or section-specific empty state | Reuse stale values or fixture values |
| One numeric field missing | `--` | Default to zero or calculate from unrelated fields |
| Evidence missing | `详细证据尚未提供` | Fabricate evidence cards or source links |
| Evidence is top-three only | `证据摘要（前3条）` | Call it complete; claim absent opposition |
| Score detail missing | `完整评分明细尚未提供` | Generate all eight values, weights, or rankings |
| Score is top-three only | Show returned items; remaining dimensions `--` | Recompute or backfill missing scores |
| AI provenance insufficient | Stored status/stance/reason codes/summary plus `完整证据关联尚未提供` | Relabel general evidence as role-specific evidence |
| Exact plan identity missing | `计划来源不可验证`; hide all plan values and IDs | Choose latest/first sibling plan |
| Position selection ambiguous | `请选择具体持仓` | Choose first, newest, or largest-ID position |
| Monitor source missing/mismatched | `监控来源不可验证` | Attach another plan or analysis |
| Plan invalid/revalidation required | `仅用于历史复核` | Present as a current suggestion |
| Current refresh fails | Clear stale selection-dependent conclusions, then show retry | Leave old plan/AI conclusion as current |
| Monitor run fails | Keep stored position facts separate; mark current run unavailable | Delete position/history or present prior result as current |
| Settings API unavailable | Disabled/omitted control with `暂未开放` | Save-success animation or local fake persistence |
| Raw backend error | Sanitized product copy | Expose credentials, endpoints, trace IDs, or stack details |

Missing opposing evidence in a partial brief means
`当前摘要未包含反向证据`. It never means `不存在反向证据`.

## 6. AI Interaction Rules

### 6.1 Shared contract

The AI area contains exactly:

- `GPT_FINAL` / 最终裁决官;
- `GEMINI_REVIEW` / 冲突复核官;
- `GROK_CHALLENGE` / 反方挑战官.

Rules:

1. AI is not a voting system.
2. The rule layer owns the base direction.
3. One role panel is visible at a time.
4. The selected asset/analysis remains fixed when switching roles.
5. Consistency summary is an internal summary, not a fourth role.
6. Allowed statuses include success, abstain, unavailable, timeout, failed,
   not applicable, and explicit fallback.
7. AI may explain backend-provided changes to confidence, risk, plan mode,
   manual-review requirement, and `confused`.
8. No chain-of-thought, vote count, vote percentage, winner, tie-break, or
   consensus animation is displayed.
9. Role-linked evidence is shown only when the response supplies an explicit
   normalized role-to-evidence relationship.

### 6.2 GPT_FINAL / 最终裁决官

**Allowed**

- role/run status and provider role label;
- rule-led final bias context supplied by synthesis;
- confidence, risk, worth-opening review opinion, and plan mode;
- stance, conflict level, summary, reason codes;
- fallback/manual-review state;
- explicit role-linked supporting/opposing evidence when supplied.

**Unavailable**

- no result: `最终裁决暂不可用`;
- summary only: `当前仅提供结论摘要`;
- no provenance: `完整证据关联尚未提供`.

**Forbidden**

- replacing the rule-layer direction;
- trading command;
- evidence invented from the summary;
- hidden reasoning chain.

### 6.3 GEMINI_REVIEW / 冲突复核官

**Allowed**

- role/run status;
- stance and conflict level;
- returned contradiction, weakness, logic-gap, and downgrade reason summary;
- maintain/adjust/downgrade result when supplied;
- reason codes and fallback state.

**Unavailable**

- no result: `冲突复核暂不可用`;
- no detailed arrays: `当前仅提供冲突原因摘要`.

**Forbidden**

- independent trading direction;
- general evidence relabelled as Gemini evidence;
- becoming final decision owner;
- trading command.

### 6.4 GROK_CHALLENGE / 反方挑战官

**Allowed**

- role/run status;
- stance and conflict level;
- returned external-event, counterargument, short-term risk, market-structure,
  or liquidity challenge summary;
- returned event/source reference;
- reason codes and fallback state.

**Unavailable**

- no result: `反方挑战暂不可用`;
- no source-linked details:
  `当前仅提供挑战摘要，外部证据明细尚未提供`.

**Forbidden**

- invented news, event, technical evidence, or source;
- unsupported market conclusion;
- unilateral veto;
- trading command.

## 7. User Position Lifecycle

```text
Authenticated manual position record
  -> OPEN
       |
       +-> monitoring cycles
       |     - read-only monitor result
       |     - suggestedManualAction is information only
       |     - no dedicated persisted human-handling record
       |
       +-> PARTIALLY_CLOSED
       |     - display only when returned
       |     - monitoring continues
       |     - no public partial-close action
       |
       +-> authenticated manual full-close record
             - records close price/reason/time
             - does not submit an exchange order
             |
             -> CLOSED
                  |
                  +-> no review yet
                  |
                  +-> optional Review
                         - summary assembled from available sources
                         - optional manual feedback
                         - no automatic rule change
```

Lifecycle rules:

1. `OPEN` begins from a manually recorded fact, never from `TRIGGERED` or an
   Execution Plan.
2. Monitoring accepts `OPEN` and `PARTIALLY_CLOSED`; it does not mutate the
   position.
3. Suggested actions MUST be labelled as system suggestions. The absence of a
   human-handling record MUST NOT be represented as completed action.
4. Manual full close records facts and changes status to `CLOSED`; it does not
   close an exchange position.
5. Review requires a fully `CLOSED` position.
6. Review is optional and may remain `待复盘`.
7. Review feedback is governance evidence; it does not automatically update or
   release rules.

## 8. Prototype Contract

### 8.1 Allowed prototype behavior

- navigate between the frozen existing page frames;
- select a predeclared asset and update linked Decision/Plan/AI fixtures;
- preserve `selectedSymbol`;
- select one exact fixture position by `positionId`;
- switch exactly three AI role variants;
- filter only visible evidence fixtures;
- demonstrate loading, empty, partial, unavailable, blocked, identity
  unverified, historical-only, and error states;
- open information-only Push destinations;
- demonstrate record-only manual open/full-close entry points without an
  exchange outcome.

All static examples MUST be annotated `DESIGN_FIXTURE_NOT_RUNTIME`.

### 8.2 Prohibited prototype behavior

The prototype MUST NOT simulate:

- automatic trading;
- buy, sell, order, execute, auto-open, auto-close, auto-reduce, or
  auto-reverse;
- an Execution Plan becoming a UserPosition;
- `TRIGGERED` becoming an opened position;
- plan execution or order completion;
- a complete evidence chain when only a brief exists;
- nonexistent supporting/opposing evidence;
- complete eight-score detail from partial score data;
- per-timeframe direction from a convergence summary;
- role-scoped AI evidence without provenance;
- AI voting, winner selection, or hidden reasoning;
- settings save success;
- fake asset-add success or local-only account persistence;
- automatic position mutation or public partial close;
- latest/first sibling plan or position selection;
- a monitor suggestion as completed human action;
- provider/AI calls, external notification delivery, or rule correction as
  completed runtime events.

Prototype variant selection may demonstrate layout states. It MUST NOT be
described as backend persistence or a completed business transition.

## 9. Implementation Readiness

| Surface | Backend support | Successful interaction allowed | Contract state |
| --- | --- | --- | --- |
| Overview status and focus assets | Yes/mostly | Yes, returned fields only | `CURRENT` |
| Asset selection by symbol | Yes | Yes | `CURRENT` |
| Exact multi-position selection | Yes | Yes, with `positionId` | `CURRENT` |
| Asset Detail | Partial | Yes, with coverage labels | `PARTIAL` |
| Evidence top-three brief | Yes | Yes | `CURRENT_PARTIAL` |
| Complete evidence chain | No authoritative public read | No; unavailable state only | `FAIL_CLOSED` |
| Score top-three brief | Yes | Yes | `CURRENT_PARTIAL` |
| Complete persisted eight-score detail | No authoritative public read | No; unavailable state only | `FAIL_CLOSED` |
| Multi-timeframe convergence summary | Yes | Yes | `CURRENT_SUMMARY` |
| Authoritative 4h/1h/15m/5m rows | No | No; unavailable state only | `FAIL_CLOSED` |
| Exact trusted position plan | Conditional | Yes when resolver evidence passes | `CONDITIONAL_CURRENT` |
| New-opportunity/legacy sibling plan | Unsafe | No numeric plan display | `FAIL_CLOSED_IDENTITY` |
| UserPosition facts | Yes | Yes | `CURRENT` |
| Position monitoring | Yes | Yes | `CURRENT` |
| Human-handling history | No dedicated contract | No successful-action history | `FAIL_CLOSED` |
| Manual full-close record | Yes | Yes, fact-recording only | `CURRENT_BOUNDED` |
| Manual partial-close flow | No | No | `PROHIBITED` |
| Closed-position Review | Partial/assembled | Yes with section fallbacks | `CURRENT_PARTIAL` |
| Three AI role summaries | Yes | Yes | `CURRENT` |
| Complete role-scoped AI evidence | No normalized provenance | No; summary-only fallback | `FAIL_CLOSED_PROVENANCE` |
| Push recheck detail | Yes | Yes, review-only | `CURRENT_REVIEW_ONLY` |
| Profile account/logout shell | Partial | Conditional | `PARTIAL` |
| Editable settings and save | No complete public API | No | `TARGET_ONLY` |
| Buy/sell/order/automatic trading | No and prohibited | No | `PROHIBITED` |

## 10. Validation and Freeze Decision

Validation checklist:

- [x] Current backend is the highest-priority capability source.
- [x] Existing Figma pages were inventoried read-only.
- [x] Page purpose, data source, visible fields, click entry, and destination
      are defined.
- [x] Asset, UserPosition, Execution Plan, and Push states are separate.
- [x] `triggered` is not presented as opened.
- [x] Execution Plan remains separate from UserPosition.
- [x] Exact plan identity is required before numeric plan values appear.
- [x] Multiple same-symbol positions require exact `positionId`.
- [x] Missing evidence, scores, and AI provenance fail closed.
- [x] AI roles remain exactly three and do not vote.
- [x] Position monitoring remains read-only.
- [x] Manual close remains fact recording.
- [x] Review remains optional after `CLOSED`.
- [x] Profile settings remain disabled/target-only.
- [x] Push recheck remains review-only.
- [x] No automatic trading or execution controls exist.
- [x] No future capability is presented as current.

Final freeze:

```text
INTERACTION_CONTRACT_V3:
FROZEN_WITH_EXPLICIT_FAIL_CLOSED_GAPS

CURRENT_BACKEND_SUPPORT:
PARTIAL

CURRENT_FIGMA_ALIGNMENT:
STRUCTURE_RECONCILED_WITH_BACKEND_OVERRIDES

FUTURE_CAPABILITY_MASQUERADE:
NONE_ALLOWED

CURRENT_WORK_PACKAGE:
P3_U2_IN_PROGRESS_PARTIAL

PRODUCTION_READINESS:
BLOCKED
```

Every successful interaction in this contract has a current backend basis.
Where the backend is partial or missing, the only allowed behavior is an
explicit partial, unavailable, blocked, unverified, historical, or
target-only state.
