# FE-04 Semantic Contract V2

## 1. Contract Status

| Item | Value |
|---|---|
| Contract | `FE-04-SEMANTIC-V2` |
| Correction date | `2026-07-28` |
| Audited main | `d523dc3e69920d6dd80a0d49f344f86757eb7b9e` |
| Scope | Product semantics, navigation, field ownership, and fail-closed behavior |
| Code/API/schema/Figma change | None |
| Capability effect | None until separately implemented and merged |

This document corrects the FE-04 product contract without making a missing
route, API, persistence path, notification channel, or setting available. For
the subjects listed here, it supersedes the older mobile-navigation, home
status, asset-card, execution-advice, AI-entry, message, Telegram, and asset
search wording in:

- `docs/design/P3_U2_IPHONE_HOME_SEMANTIC_CONTRACT.md`;
- `docs/design/P3_U2_IPHONE_HOME_FIELD_MAPPING.md`;
- `docs/INTERACTION_CONTRACT_V3.md`; and
- `docs/FRONTEND_IMPLEMENTATION_CONTRACT_AUDIT_V2.md`.

Backend identity, ownership, safety, and availability still outrank visual
fixtures. A target navigation item may be present in the design baseline while
remaining disabled or fail-closed until its real route and data contract exist.

Capability labels:

- `READY`: a current authoritative route/data contract supports the behavior;
- `PARTIAL`: a real subset exists, but an end-to-end product flow does not;
- `FAIL_CLOSED`: the UI may show only unavailable/blocked copy;
- `EXTENSION`: outside current V1 delivery and never presented as completed.

## 2. Mobile Navigation V2

The final mobile information architecture contains exactly five primary tabs:

1. 首页
2. 持仓
3. AI分析
4. 消息
5. 我的

观察资产 is not a primary tab. Its intended entries are:

- 首页搜索资产;
- AI分析完成后加入观察资产;
- 我的中管理观察资产.

Current capability binding:

| Tab | Target responsibility | Current binding | Status |
|---|---|---|---|
| 首页 | Dashboard summary and selected-asset context | `/dashboard/mobile` | `READY` |
| 持仓 | UserPosition list and exact-position monitoring entry | Owner-scoped position reads exist; FE-04 page is not implemented | `PARTIAL` |
| AI分析 | Search asset, create/open analysis, and enter FE-03 | Analysis creation/detail exists; market search landing does not | `PARTIAL` |
| 消息 | Opportunity and UserPosition-risk events | Reduced in-app/read-only records exist; complete product inbox does not | `PARTIAL` |
| 我的 | Account/session, watch management, and supported settings | Logout/minimal shell only; watch/settings writes are unavailable | `PARTIAL` |

复盘 remains contextual. It is entered only from an eligible `CLOSED`
UserPosition with exact `positionId`; it is not a sixth primary tab.

观察, 计划, 告警, or a standalone AI-role destination must not be added as
additional primary tabs.

## 3. Home Status Area V2

The home header is a mixed **首页状态区**, not a pure system-status bar.

### 3.1 Current selected-asset status

These values follow the current `selectedSymbol`:

| Label | Authoritative field |
|---|---|
| 市场趋势 | `systemState.marketTrend.valueLabel` |
| 风险等级 | `systemState.riskLevel.valueLabel` |
| 数据质量分 | `systemState.dataQuality.valueLabel` |
| AI冲突等级 | `systemState.aiConflict.valueLabel` |

They must not be relabelled as market-wide risk, system-wide data quality, or
system-wide AI conflict.

### 3.2 System summary status

Only existing system summaries may be displayed:

| Label | Authoritative field |
|---|---|
| AI系统状态 | `header.aiStatusLabel` |
| 待复核机会 | `systemState.pendingReview.valueLabel` |
| 冲突阻断统计 | `systemState.confused.valueLabel` |
| Hot Reset | `systemState.hotReset.valueLabel` |

No system risk, system AI-conflict score, Confused total, holding-risk total,
or other aggregate may be derived on the client. Missing values use their
field-specific empty state.

## 4. Asset Card V2

The card body displays only:

- asset symbol/name;
- current price;
- direction;
- composite score;
- confidence;
- risk level;
- current asset state.

It does not display eight-score details, timeframe rows, complete evidence, or
complete AI-role output.

Interaction ownership:

- tapping the card body sets `selectedSymbol`;
- Execution Advice and the three-role AI region refresh for that asset;
- the UserPosition/Position Monitor DOM remains unchanged;
- tapping the body does not navigate.

A separate `查看详情` affordance may enter FE-03 Analysis Detail only with the
authoritative nullable `analysisId`. Missing or unverified `analysisId` disables
the affordance and displays `当前不可查看`; no symbol/latest/sibling fallback is
allowed.

## 5. Execution Advice V2

The product name is exactly `执行建议`. Do not rename it to `系统执行建议`.

Execution Advice is a rule-led system suggestion for manual review. Its
provenance is:

```text
rule layer
  + returned evidence
  + returned score context
  + returned multi-timeframe context
  + available AI review/downgrade context
  -> source-verified Execution Plan projection
```

AI never originates a plan and never overrides the rule-layer base direction.
When a required source or exact plan identity cannot be verified, the plan
fails closed instead of borrowing or generating values.

Display fields:

- recommended direction;
- worth-opening opinion;
- entry zone;
- stop loss;
- take-profit plan;
- leverage suggestion;
- position suggestion;
- invalidation condition;
- `validFrom`;
- `expiresAt`.

`validFrom/expiresAt` are plan-validity timestamps. They must not be replaced
by `1H/4H` analysis timeframes. Conflict details belong to the Execution Advice
detail and show the returned conflict block plus returned blocking reasons.

Execution Advice never proves an opened position and never creates or mutates a
UserPosition.

## 6. AI Analysis V2

Asset Detail is the AI-analysis entry surface. The target flow is:

```text
search market asset
  -> create authoritative analysisId
  -> open FE-03 Analysis Detail
  -> optionally add to watch assets
```

Current capability is `PARTIAL`:

- `POST /api/analysis/runs` can create an analysis when the caller supplies a
  valid symbol and timeframe;
- `GET /api/analysis/runs/{analysisId}` and FE-03 detail reads exist;
- no complete market-asset search contract exists;
- no authenticated user watch-asset add/remove contract exists.

The UI must not simulate search results, analysis success, or watchlist
persistence.

Exactly three roles remain:

| Role | Default responsibility |
|---|---|
| `GPT_FINAL` | Final review conclusion, direction, confidence, and core judgment |
| `GEMINI_REVIEW` | Risk weaknesses and primary objections |
| `GROK_CHALLENGE` | Counter-case and returned event risks |

AI consistency is a summary above the three roles, not a fourth role.
Eight-score, timeframe, supporting/opposing evidence, and source-chain detail
may be shown only when returned. Otherwise use the frozen FE-03 fail-closed
states.

## 7. Message And Telegram V2

There are exactly two product event sources:

1. asset-pool opportunity;
2. UserPosition monitoring risk.

System notices may remain a separate informational category only when a real
system-notice record is supplied. They are not a third opportunity/risk Push
source.

Telegram is an `EXTENSION / PENDING_IMPLEMENTATION` notification outlet. It is
not a business-decision layer and is not evidence that delivery occurred.

Permitted future Telegram categories are limited to:

- executable-opportunity review;
- UserPosition risk;
- take-profit proximity.

Telegram must not expose trading commands, order actions, open/close actions,
or authorization.

Push Detail remains review-only and shows the original snapshot, current
PushRecheck result, and returned change reasons. PushRecheck never authorizes a
trade.

## 8. Search Asset V2

Search targets market assets and never adds an asset automatically.

```text
search
  -> choose asset
  -> create analysisId
  -> open analysis
  -> explicit optional add-to-watch action
```

Status: `PARTIAL`.

Until market search and authenticated watch-asset persistence contracts exist:

- search/add controls are disabled or labelled `暂未开放`;
- no localStorage persistence represents server state;
- no fake card is created;
- no successful add state is shown.

## 9. Old To New Contract Difference

| Area | Old contract | Corrected contract |
|---|---|---|
| Mobile navigation | Four-tab historical proposal or three implementation-ready tabs | Five target tabs with per-tab capability status; Review becomes contextual |
| Home status | Seven values treated as one undifferentiated status band | Four selected-asset values plus four existing system summaries |
| Asset card | Whole card could double as detail navigation | Body switches context; separate authoritative detail affordance |
| Execution Advice | Source inputs and plan validity could be conflated | Rule-led, source-verified plan; `validFrom/expiresAt` are validity timestamps |
| AI entry | AI only as an embedded section | Asset Detail/FE-03 is the analysis entry; search/watch flow remains partial |
| Messages | System notices could look like a third business Push source | Two business event sources; system notices are informational only |
| Telegram | Could appear as a current channel | Explicit extension/pending implementation |
| Search | Search/add behavior was not fully bounded | Market search target, explicit add only, current capability `PARTIAL` |

## 10. Figma And Implementation Gate

This correction removes the semantic contradiction that blocked a new FE-04
Figma baseline. It does not make missing runtime capabilities ready.

Figma may now express:

- the five-tab target navigation;
- disabled/partial states for unavailable destinations;
- the mixed home status area;
- card-body context switching plus a separate detail affordance;
- two-source message semantics;
- Telegram as an unavailable extension.

FE-04 implementation remains a separate task and must continue to obey:

- `ExecutionPlan != UserPosition`;
- exact `positionId` and owner-scoped reads;
- no auto-open, auto-close, auto-reverse, order, or trading action;
- no AI rule-layer override;
- no PushRecheck trading authorization;
- no fabricated evidence, scores, timeframe detail, search result, watch state,
  message delivery, or settings-save success.

