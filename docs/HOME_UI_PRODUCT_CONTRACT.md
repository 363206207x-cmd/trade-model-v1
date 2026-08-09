# Home UI Product Contract

> Status: `FIGMA_BASELINE_APPROVED_FOR_IMPLEMENTATION`
> Runtime baseline: `main@27687ad8357ecffb488a0978656fb248933b935a`
> Figma: [Home Final Product Design](https://www.figma.com/design/mTmcpv9u34IbGjlNfVvOZA)
> Scope: Home product design contract and editable Figma baseline only

## 1. Authority

This document is the Home-specific design projection of the repository product source. It does not replace the product source.

When sources disagree, use this order:

1. `docs/PRODUCT_SOURCE_OF_TRUTH.md`
2. `docs/product-sources/V1_PRODUCT_ARCHITECTURE.md`
3. `docs/product-sources/POSITION_MONITORING_COMPLETE_PLAN.md`
4. `docs/product-sources/AI_CONFLICT_RECHECK_REVIEW_PLAN.md`
5. `docs/design/P3_U2_IPHONE_HOME_SEMANTIC_CONTRACT.md`
6. `docs/design/FE04_SEMANTIC_CONTRACT_V2.md`
7. Current API and runtime implementation
8. This Home design projection
9. Figma frames and component examples

The formal product source decides meaning. Runtime DTOs prove that a field can be read. Figma decides presentation only. A field must satisfy both product meaning and an authoritative runtime source before it can appear as live Home data.

## 2. Scope And Safety Boundary

This baseline freezes:

- the Home field allowlist;
- information hierarchy and module order;
- state labels and fail-closed behavior;
- asset-context interaction;
- desktop and iPhone responsive disclosure;
- Light and Dark visual tokens;
- reusable Home component variants.

This baseline does not authorize:

- business-code changes;
- API or schema changes;
- order creation, trade execution, position mutation, or automatic action;
- Telegram, external notification, or automatic notification;
- new AI roles, AI voting, or AI trading authorization;
- new score semantics or market fields;
- implementation of the Figma design in the runtime UI.

All Figma values are marked `DESIGN_SAMPLE_ONLY`. They exist to validate length, hierarchy, and state treatment. They are not market data and must never be copied into production defaults.

## 3. Frozen Home Information Architecture

The Home order is fixed:

1. **System and selected-asset status**
2. **Risk alerts and news events**
3. **Watch assets**
4. **Execution Plan**
5. **Top UserPosition summaries**
6. **Three AI summary**

### 3.1 First Visual

Within five seconds, the first viewport must make these answers available:

- which asset is selected;
- its formal `MarketBiasHierarchy` direction;
- confidence;
- risk;
- current `AssetState`;
- whether an Execution Plan is available and its entry summary.

Execution Plan is the primary decision region. UserPosition is a separate, secondary region. Three AI follows plan and position context and never becomes the first visual.

### 3.2 Desktop Composition

- Fixed five-item primary navigation: Home, Positions, AI Analysis, Messages, Mine.
- Status strip remains compact; it cannot become a row of equal-weight dashboards.
- Alerts/events are a concise operational band, not a generic message center.
- Watch assets form a scannable card rail/grid.
- Execution Plan receives the highest section weight.
- Top3 UserPosition cards remain visually separate from the plan.
- Three AI uses one primary role and two supporting roles, plus a scoped consistency summary.

### 3.3 Mobile Composition

- The first viewport shows selected asset, direction, confidence, risk, plan status, and entry summary.
- Execution Plan directly exposes all nine frozen plan fields; mobile spacing cannot hide core plan content behind disclosure.
- UserPosition shows the owner-scoped Top3 summaries after the plan.
- Bottom navigation must not cover content or controls.
- Supporting metadata may wrap or move below the fold; core decision fields may not be removed.

## 4. Field Source Allowlist

API for runtime-bound fields: `GET /api/dashboard/home?selectedSymbol={symbol}`.

Levels:

- `L1`: first-decision visual;
- `L2`: plan/risk context;
- `L3`: supporting metadata or expanded disclosure;
- `Hidden`: identity used for routing or validation but not rendered as ordinary copy.

| UI field | Source object | Product source | Level | Allowed on Home | Empty / unavailable behavior |
|---|---|---|---|---|---|
| Page title | `DashboardHomeVO.header.pageTitle` | Home architecture | L3 | Yes | Use product title, never invent status |
| Selected asset | `DashboardHomeVO.selectedSymbol` + matching `assets[].symbol` | Home interaction contract | L1 | Yes | `--`; clear old selected context |
| Overall Home state | `states.overall` | Home five-state contract | L2 | Yes | Render the exact fail-closed state |
| Data sync label | `header.dataStatus` | Home semantic contract | L3 | Yes | `待同步` |
| AI system status | `header.aiStatusLabel` / `aiDecision.runStatusLabel` | Three-AI contract | L2 | Yes | `当前不可查看` |
| Data source text | `header.dataSourceText` | Data-integrity contract | L3 | Yes | Hide when absent |
| Home updated time | `header.updatedAt` | Home semantic contract | L3 | Yes | `--`; never generate client time |
| Market trend | `systemState.marketTrend.valueLabel` | Product architecture | L1 | Yes | `--` |
| Home risk level | `systemState.riskLevel.valueLabel` | Product architecture | L1 | Yes | `--` |
| Data quality score/state | `systemState.dataQuality.valueLabel` / `.score` | Product architecture | L2 | Yes | `待同步` |
| Hot Reset state | `systemState.hotReset.valueLabel` | Recovery contract | L2 | Yes | `--` |
| Alert severity | `alerts[].level` | Event and risk contract | L2 | Yes | Hide row when missing |
| Risk alert text | `alerts[].message` | Event and risk contract | L2 | Yes | Do not create replacement copy |
| Alert asset | `alerts[].symbol` | Event and risk contract | L2 | Yes | `--` |
| Alert time | `alerts[].time` | Event and risk contract | L3 | Yes | `--`; never use browser time |
| Event type | `events[].type` | Event system | L3 | Yes | Hide row when missing |
| Event label | `events[].label` | Event system | L2 | Yes | Hide row when missing |
| Event impact | `events[].impactLevel` | Event system | L2 | Yes | `--` |
| Event time window | `events[].timeWindow` | Event system | L3 | Yes | `--` |
| Asset name | `assets[].symbol` | Watch asset contract | L1 | Yes | Omit invalid card |
| Current price | `assets[].latestPrice` | Market data read projection | L1 | Yes | `--`; never restore stale price |
| Market bias | `assets[].marketBiasLabel` | `MarketBiasHierarchy` | L1 | Yes | `观望` only when authoritative; otherwise `--` |
| Composite score | `assets[].compositeScore` | Eight-score decision contract | L1 | Yes | `--`; no fake score |
| Confidence | `assets[].confidenceLabel` | Decision bundle | L1 | Yes | `--` |
| Asset risk | `assets[].riskLabel` | Decision bundle | L1 | Yes | `--` |
| Asset state | `assets[].assetStateLabel` | Asset state machine | L1 | Yes | `数据待同步` |
| Asset data quality | `assets[].dataQuality` | Home core-data contract | L2 | Yes | `待同步` |
| Multi-timeframe state | `assets[].multiTimeframeState` | Multi-timeframe contract | L2 | Yes | `--` |
| Asset Confused state | `assets[].confused` | Confused contract | L2 | Yes | `--` |
| Asset updated time | `assets[].updatedAt` | Home core-data contract | L3 | Yes | `--`; never generate time |
| Asset module state | `assets[].moduleState` | Home five-state contract | L2 | Yes | Render exact state; clear stale fields |
| Asset analysis identity | `assets[].analysisId` | Analysis identity contract | Hidden | Yes, identity only | No detail link without exact identity |
| Plan module state | `states.executionPlan` + `executionSuggestion.moduleState` | Execution Plan contract | L1 | Yes | Exact state label |
| Plan status | `executionSuggestion.statusLabel` | Execution Plan contract | L1 | Yes | `当前不可查看` |
| Plan recommended direction | `executionSuggestion.direction` | Execution Plan contract | L1 | Yes | `--` |
| Worth opening | `assets[].worthOpening` + plan status semantics | Execution Plan contract | L1 | Yes | `--`; no client inference |
| Entry zone | `executionSuggestion.entryZone` | Execution Plan contract | L1 | Yes | `--` |
| Stop loss | `executionSuggestion.stopLoss` | Execution Plan contract | L2 | Yes | `--` |
| Take-profit rules | `executionSuggestion.takeProfitRules` | Execution Plan contract | L2 | Yes | `--` |
| Leverage suggestion | `executionSuggestion.leverageSuggestion` | Execution Plan contract | L2 | Yes | `--` |
| Position-size suggestion | `executionSuggestion.positionSuggestion` | Execution Plan contract | L2 | Yes | `--` |
| Invalidation condition | `executionSuggestion.invalidCondition` | Execution Plan contract | L2 | Yes | `--` |
| Plan validity label | `executionSuggestion.validPeriod` | Execution Plan contract | L2 | Yes | `--` |
| Plan valid from | `executionSuggestion.validFrom` | Execution Plan contract | L3 | Yes | `--` |
| Plan expires at | `executionSuggestion.expiresAt` | Execution Plan contract | L3 | Yes | `--` |
| Plan blocked reason | `executionSuggestion.blockedReason` | Execution Plan contract | L2 | Yes | Show only in Blocked/Error state |
| Plan exact identity | `executionSuggestion.sourceExecutionPlanId` | Execution Plan identity contract | Hidden | Yes, identity only | No detail action without exact identity |
| Position symbol | `positions[].symbol` | UserPosition contract | L2 | Yes | Omit invalid row |
| Position direction | `positions[].directionLabel` | UserPosition contract | L2 | Yes | `--` |
| Position entry price | `positions[].entryPrice` | UserPosition contract | L2 | Yes | `--` |
| Position current price | `positions[].currentPrice` | Owner-scoped position read | L2 | Yes | `--`; never use asset-card fallback |
| Position PnL | `positions[].floatingPnl` / `.pnlPct` | UserPosition contract | L2 | Yes | `--` |
| Position risk | `positions[].riskLevelLabel` | Position monitoring plan | L2 | Yes | `--` |
| Monitor conclusion | `positions[].monitorConclusion` | Position monitoring plan | L2 | Yes | `当前不可查看` |
| Entry logic state | `positions[].entryLogicStatusLabel` | Position monitoring plan | L3 | Yes | `--` |
| Direction support state | `positions[].directionSupportStatusLabel` | Position monitoring plan | L3 | Yes | `--` |
| Reversal state | `positions[].reversalStatusLabel` | Position monitoring plan | L3 | Yes | `--` |
| Suggested manual action | `positions[].suggestedManualActionText` | Position monitoring plan | L3 | Yes | `--`; never convert to automatic action |
| Last monitor time | `positions[].lastMonitorAt` | Position monitoring plan | L3 | Yes | `--` |
| Next monitor time | `positions[].nextMonitorAt` | Position monitoring plan | L3 | Yes | `--` |
| Position module state | `positions[].moduleState` / `.warningState` | Position state contract | L2 | Yes | Exact state label |
| Position identity | `positions[].positionId` serialized as String | UserPosition identity contract | Hidden | Yes, identity only | No detail action without exact identity |
| AI run state | `aiDecision.runStatusLabel` | Three-AI contract | L2 | Yes | `当前不可查看` |
| AI role | `aiDecision.tabs[].role` / `.roleLabel` | Three-AI contract | L2 | Yes | Only the three frozen roles |
| AI role status | `aiDecision.tabs[].runStatusLabel` | Three-AI contract | L2 | Yes | Status-only when unavailable |
| AI unavailable message | `aiDecision.tabs[].statusMessage` | Three-AI fail-closed contract | L2 | Yes | Status-only; no conclusion fields |
| GPT final summary | `GPT_FINAL.finalConclusion` / `.decisionSummary` | AI conflict plan | L2 | Yes | Hide when `resultAvailable=false` |
| Gemini primary objection | `GEMINI_REVIEW.reviewVerdict` / `.detectedContradictions` | AI conflict plan | L2 | Yes | Hide when unavailable |
| Grok counter challenge | `GROK_CHALLENGE.challengeConclusion` / `.challengeThesis` | AI conflict plan | L2 | Yes | Hide when unavailable |
| AI consistency level | `aiDecision.consistency.consistencyLevel` | AI conflict plan | L2 | Yes | `--` |
| AI consistency summary | `aiDecision.consistency.consistencySummary` | AI conflict plan | L2 | Yes | `当前不可查看` |
| AI Confused state | `aiDecision.consistency.confused` | Confused contract | L2 | Yes | `--` |
| AI downgrade reason | `aiDecision.consistency.downgradeReason` | AI conflict plan | L3 | Yes | Hide when absent |

## 5. Explicitly Unsupported Home Fields

The following fields or semantics are prohibited from this design baseline:

| Prohibited field / semantic | Reason |
|---|---|
| Unscoped percentage change such as `+2.3%` | No product-defined period or authoritative source |
| Generic `Buy`, `Sell`, `Long`, `Short`, or `Neutral` replacing `MarketBiasHierarchy` | Breaks formal direction semantics |
| `Strong Buy`, `Ready Plus`, `Safe`, or `Hot` | Not a formal state |
| Client-generated price, score, confidence, risk, time, or message count | Violates real-data and fail-closed rules |
| Position data inferred by symbol | Violates exact UserPosition identity and ownership |
| Execution Plan rendered as an open position or current order | `ExecutionPlan != UserPosition` |
| UserPosition rendered as the selected asset plan | Breaks asset-context separation |
| Three-AI vote counts or majority result | The three roles are hierarchical, not a vote |
| AI result presented as trade authorization | AI cannot bypass the rule layer |
| Automatic order, close, reverse, notification, or Telegram action | Outside Home read-only capability |
| Private source IDs displayed as ordinary copy | Identities are for exact routing and validation only |
| Stale successful payload after Error or Missing | Violates failure isolation |

The frozen Figma baseline contains zero prohibited fields.

## 6. Formal Direction And State Vocabulary

### 6.1 MarketBiasHierarchy

Only these visible direction labels are allowed:

- `强偏多`
- `偏多`
- `弱偏多`
- `震荡`
- `弱偏空`
- `偏空`
- `强偏空`
- `观望`

### 6.2 Asset State

| State | Trigger | Recovery |
|---|---|---|
| `observing` | Valid asset exists but is not yet a candidate | Next authoritative analysis changes state |
| `candidate` | Rule layer marks a candidate opportunity | Trigger evaluation or invalidation updates it |
| `waiting_trigger` | Candidate waits for an explicit trigger | Trigger, invalidation, or cooling update |
| `triggered` | Rule condition is triggered; not an opened trade | Subsequent rule/plan state update |
| `high_risk` | Authoritative risk state is elevated | New valid risk evaluation |
| `invalidated` | Asset opportunity or plan is invalid | New authoritative analysis only |
| `cooling` | Product cooling rule is active | Cooling expiry plus valid reevaluation |
| `confused` | Formal conflict state is active | Conflict-resolution/recheck result |

`triggered` must never be displayed as opened, ordered, or executed.

### 6.3 Home Module State

| State | Trigger | Visible treatment | Recovery |
|---|---|---|---|
| `READY` | Complete, valid current payload | Show authoritative fields | Next refresh may change state |
| `PARTIAL` | Legal payload with incomplete fields | Show available fields and `--`/`待同步` | Complete authoritative refresh |
| `EMPTY` | Valid collection has no rows | Purpose-specific empty state | New valid data |
| `ERROR` | Read, parse, identity, or illegal-state failure | Error + retry; clear prior success | Successful authoritative retry |
| `MISSING` | Required resource is absent or not visible | `当前不可查看` / `数据待同步` | Resource becomes visible and valid |

Loading may use a transient skeleton, but it is not a sixth persisted result state.

### 6.4 AI State

| Visible state | Source | Treatment | Recovery |
|---|---|---|---|
| `一致` | Consistency result | Show one scoped consistency summary | Next evidence package |
| `降级` | Role/input unavailable or downgraded | Show run status and downgrade reason | Complete successful run |
| `冲突` | Formal AI conflict result | Show scoped conflict, not asset conflict | Recheck/resolution result |
| `confused` | Formal confused state | Block directional interpretation | Conflict-resolution result |

State labels must always show scope, for example `资产状态 · confused` and `AI 一致性 · 冲突`. The same word may not appear unscoped in two domains.

## 7. Interaction Contract

### 7.1 Asset Selection

Clicking an Asset Card:

1. updates `selectedSymbol`;
2. reloads AssetState for that exact asset;
3. reloads its Execution Plan;
4. reloads Three-AI and consistency context;
5. does not navigate to detail automatically;
6. does not select, create, or mutate a UserPosition;
7. does not create any trade action.

UserPosition remains owner-scoped and independent of the selected asset. A separate exact-identity action is required for any detail navigation.

### 7.2 Failure Isolation

If asset-context loading fails or becomes Missing:

- clear the selected asset KPI values;
- clear Execution Plan content;
- clear AI role summaries and consistency;
- disable exact-detail entry when identity is absent;
- do not restore `window.__lastDashboardHome` or any other stale success cache;
- preserve the independent UserPosition collection only when its own module state remains valid.

### 7.3 Disclosure

- Desktop may show the full Execution Plan in the main decision panel.
- Mobile shows the complete nine-field Execution Plan directly, with asset confidence and risk retained in the preceding Asset Card.
- Mobile shows the owner-scoped Top3 position summaries directly after Execution Plan.
- Any non-core supporting disclosure changes presentation only; it does not trigger recheck, monitor execution, or mutation.

### 7.4 Retry

Retry is permitted only for a failed read. It must request the same read contract and cannot create a monitor run, recheck mutation, order, notification, or new identity.

## 8. Visual Contract

### 8.1 Principles

- Professional AI decision product, not a back-office admin dashboard.
- Decision hierarchy before field density.
- Restrained neutral surfaces with teal accent and semantic risk colors.
- No gradients, neon crypto styling, decorative orbs, or equal-weight card soup.
- Cards use at most `8px` radius.
- Typography uses fixed sizes and zero letter spacing.
- Numeric values remain scannable; supporting metadata is quieter.
- Light and Dark modes preserve hierarchy rather than merely inverting colors.

### 8.2 Token Baseline

Figma collections:

- `Home Primitives`: 36 raw runtime-aligned colors, one `Value` mode.
- `Home Semantic Colors`: 20 semantic colors, `Light` and `Dark` modes.
- `Home Layout`: 16 spacing, radius, and stable-layout values.

Typography:

- Chinese product copy: `Noto Sans SC`.
- Latin labels and numeric metrics: `Inter`.
- Letter spacing: `0`.

## 9. Figma Baseline Inventory

File: [Home Final Product Design](https://www.figma.com/design/mTmcpv9u34IbGjlNfVvOZA)

Pages:

- `00 Cover & Contract`
- `01 Home Final Design` (`3:83`)

Component sets:

| Component | Node | Variants |
|---|---|---|
| State Badge | `7:2` | Risk, Confidence, Data Quality, Asset State |
| Asset Card | `9:2` | READY, PARTIAL, ERROR, MISSING, CONFUSED |
| Execution Plan | `10:147` | Valid, Partial, Blocked, Missing |
| Position Card | `12:2` | Monitoring, Risk Elevated, Invalidated, Empty |
| AI Summary | `14:2` | Consensus, Conflict, Degraded, Missing |

Required frame inventory:

- Home Desktop Light (`51:653`, `1440 x 1000`)
- Home Desktop Dark (`51:777`, `1440 x 1000`)
- Home iPhone Large Light (`51:849`)
- Home iPhone Large Dark (`51:921`)
- Home iPhone 12 Pro Max Light (`51:993`)
- Home iPhone 12 Pro Max Dark (`51:1065`)

Baseline completion state:

- Token collections, foundations, and all five component sets above are created and identified.
- All six required frames have returned node identities and passed independent Light/Dark, desktop/mobile, component-instance, semantic, and scroll/safe-area review.
- The approved Figma baseline is the unique visual source for the bounded Home UI implementation; it does not expand product semantics or business capability.

## 10. Acceptance Gate

The Home design baseline passes only when all are true:

1. Every visible field appears in the allowlist.
2. Every visible state has a source, trigger, treatment, and recovery.
3. Unsupported field count is zero.
4. Undefined semantic count is zero.
5. Execution Plan and UserPosition remain visually and semantically separate.
6. Three AI roles remain fixed and non-voting.
7. Desktop and both iPhone sizes exist in Light and Dark.
8. The mobile first viewport contains the selected asset decision and plan summary.
9. Error and Missing never show stale successful data.
10. No runtime code, API, schema, AI capability, or trading capability changes are included.

This document and its Figma file must be independently reviewed before any 1:1 Home UI implementation is authorized.
