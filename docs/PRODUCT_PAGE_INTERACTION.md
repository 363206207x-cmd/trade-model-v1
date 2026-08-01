# Trade Model V1 Product Page Interaction Baseline

Status: `P0_PRODUCT_BASELINE_FREEZE_CANDIDATE`

This document freezes page entry, context, clicks, cross-module linkage, navigation, and failure behavior. It does not certify that the current pages already implement every interaction. Authority: `docs/PRODUCT_SOURCE_OF_TRUTH.md`.

## 1. Global Navigation and Identity

Mobile has exactly five primary tabs in this order:

1. 首页
2. 持仓
3. AI分析
4. 消息
5. 我的

观察资产 and 复盘 are contextual workflows, not additional primary tabs. Desktop may use a wider navigation presentation but preserves the same product modules.

Navigation rules:

- Page state is keyed by exact authoritative identity: `analysisId`, `executionPlanId`, `positionId`, or `messageId` as applicable.
- Symbol may select a public asset context; it cannot replace an exact identity for position, analysis detail, plan detail, or private message detail.
- No route may infer “latest” when an exact identity is required.
- Browser back returns to the prior page and prior selected context when still valid; it does not silently select another record.
- Session expiry returns to Login and preserves no private data in a public fallback.
- Unsupported actions are hidden or disabled with “暂未开放”; they never simulate success.

## 2. Shared Five-State Behavior

| State | Page behavior | Content rule | Retry |
|---|---|---|---|
| Loading | stable skeleton/progress in the target region | no old success presented as current | no duplicate concurrent action |
| Empty | valid authorized collection has no records | explicit empty copy; no fabricated rows/counts | refresh may be offered |
| Error | request, parse, or legal-state failure | error remains distinct from Empty; stale cache cannot overwrite it | retry only where the same safe read is supported |
| Partial | legal identity/data exists but required fields are incomplete | show only verified fields plus partial reason | refresh/revalidation where read-only |
| Missing | exact identity absent or not visible | “当前不可查看” or equivalent; no symbol/latest fallback | return to parent collection |

## 3. Login

**Entry:** unauthenticated root redirect or expired session.

**Default context:** no private product data.

| User action | Result | Boundary |
|---|---|---|
| Submit valid credentials | server establishes authenticated Session; enter Home | no client-only authentication claim |
| Submit invalid credentials | remain on Login and show failure | no account existence disclosure beyond supported message |
| Retry | resubmit current explicit input | no automatic repeated login |
| Logout from My/Settings | invalidate Session and return to Login | clear private page context |

## 4. Home

**Entry:** Home tab, authenticated root, or safe return from contextual detail.

**Default context:** the server-provided selected asset or first valid focus asset. If none exists, remain Empty; do not fabricate a default symbol.

**Module order:** top system/market state, alerts/events, focus assets, Execution Plan, three-AI summary, Top3 positions, contextual detail entry.

### 4.1 Focus asset interactions

| Click/tap | Context change | Navigation | Linked refresh |
|---|---|---|---|
| Asset card body | set selected public asset context | no default detail jump | asset status, Execution Plan, GPT/Gemini/Grok summaries, AI consistency, data-quality/source time |
| Explicit Analysis Detail icon/link | preserve authoritative `analysisId` | open Analysis Detail | exact analysis only |
| Retry after asset refresh error | re-read selected context | remain on Home | success replaces error only after verified response |

Selecting an asset never selects, creates, rebinds, or mutates a UserPosition. Existing position cards keep their own exact `positionId`.

### 4.2 Execution Plan on Home

Home directly presents verified plan fields when available:

- recommended direction/action;
- whether worth opening;
- entry zone;
- stop;
- take-profit targets;
- leverage suggestion;
- position-size suggestion;
- add/reduce/abandon/invalid conditions;
- validity/revalidation time;
- source/readiness status.

| Action | Result | Boundary |
|---|---|---|
| Change focus asset | load that asset's exact plan summary | no previous asset plan retained as current |
| Open explicit plan/detail link | open exact contextual detail if a supported route and identity exist | never infer by symbol/latest |
| Missing/incomplete plan | show Partial/Missing and only verified fields | no synthetic plan, percentage, or trade button |

ExecutionPlan is advice. Home exposes no order, auto-open, auto-close, or automatic position action.

### 4.3 Three-AI summary

The section has exactly `GPT_FINAL`, `GEMINI_REVIEW`, and `GROK_CHALLENGE`.

| Interaction | Result | Boundary |
|---|---|---|
| Change focus asset | reload the three roles for the same authoritative `analysisId` | no mixed analysis IDs |
| Role result available | show verified role output, direction/confidence only where contracted | role must satisfy `resultAvailable == true` |
| Role unavailable | show role name, status label, and status message only | discard conclusion/direction/confidence/metrics/evidence even if legacy payload contains them |
| Open Analysis Detail | navigate with exact `analysisId` | deeper scores/evidence/timeframes live there |

### 4.4 Top3 positions

- Only the first three owner-scoped active positions are shown.
- Ordering is pinned first, then risk priority, then authoritative update time; ties are deterministic.
- A card click opens Position Detail/monitor page with exact string-safe `positionId`.
- The position remains independent of the selected asset unless exact identity explicitly matches.
- Empty means no open/partially closed owner positions, not a position-read error.

### 4.5 Alerts and events

- Clicking an alert opens only its supported exact context.
- A position-risk alert may open exact owner-scoped Position Detail or Message Detail.
- A public opportunity event may switch public asset context or open exact Analysis Detail.
- No alert click executes a trade or runs a monitor/recheck mutation.

## 5. Positions

**Entry:** 持仓 tab or Home Top3 “view all”.

**Default context:** owner-scoped `OPEN` and `PARTIALLY_CLOSED` list. `CLOSED` is excluded from active monitoring.

| Click/tap | Result | Boundary |
|---|---|---|
| Position card | select exact string-safe `positionId`; open detail/monitor surface | no Number conversion, symbol fallback, or latest fallback |
| Manual add/open | open authenticated manual-entry form | only explicit user submission creates UserPosition |
| Submit manual entry | persist user facts and optional exact original plan link | plan values cannot overwrite actual user facts |
| Record partial close | explicit user confirmation updates remaining user fact | no automatic monitor action |
| Record final close | explicit user confirmation sets CLOSED and opens Review eligibility | no auto-close |
| Refresh | read current owner-scoped position and monitor projection | UI must not invoke monitor-run POST implicitly |

Position cards show actual user entry facts separately from original system plan and current monitor conclusion. Suggestions are labeled advisory/manual.

## 6. Position Detail and Monitoring

**Entry:** exact `positionId` from Positions, Home Top3, or owner-scoped risk message.

**Context:** UserPosition, linked original ExecutionPlan, authoritative latest PositionMonitor result, and logs.

| Action | Result | Boundary |
|---|---|---|
| View original plan | display linked exact plan when authorized and available | no symbol/latest inference |
| Expand monitor reason/log | show owner-scoped timestamped history | no cross-user data |
| Refresh | re-read current projection | no implicit `/run`, trade, close, reverse, add, or reduce |
| Record close | explicit manual workflow | monitor suggestion never submits it |
| Open Review after CLOSED | navigate to exact Review context | open positions do not masquerade as completed review |

Risk-level differences across account and monitor dimensions are displayed as separate concepts, not treated as an error solely because they differ. A wick-only movement is not presented as strong reversal without the complete defined evidence.

## 7. AI Analysis

**Entry:** AI分析 tab with an existing asset/analysis context, or explicit link from Home.

**Default context:** exact authoritative `analysisId`; if absent, Missing rather than “latest analysis”. Market-wide asset search is not part of the frozen first package unless separately authorized.

| Action | Result | Boundary |
|---|---|---|
| Select an available existing analysis | switch exact `analysisId` context | symbol alone is not identity |
| Open Analysis Detail | navigate to `/dashboard/analysis-detail?analysisId=<exact>` | reuse FE-03 detail rather than a second analysis system |
| View role summary | show only available role result | no fourth AI or new scoring model |
| Search/add watch asset control, if visible | disabled/“暂未开放” | no fake result or watch write |

Loading, Empty, Error, Partial, and Missing remain independent. No state invents scores, timeframes, evidence, or AI output.

## 8. Analysis Detail

**Entry:** explicit detail link carrying exact `analysisId`.

**Content:** rule base, eight score items returned by the source, 5m/15m/1h/4h convergence, supporting/opposing/neutral evidence, role details, conflict reason, source timestamps, trace, and rule version.

| Action | Result | Boundary |
|---|---|---|
| Expand score/evidence/timeframe | reveal verified exact-analysis detail | missing items remain absent/Partial |
| Open linked ExecutionPlan | exact plan detail if route and identity exist | no generated “latest” plan |
| Back | return to prior asset context | no context reset to another symbol |

## 9. Messages

**Entry:** 消息 tab.

**Default context:** authenticated list containing only `OPPORTUNITY` public projections and current-user `POSITION_RISK` private projections.

| Action | Result | Boundary |
|---|---|---|
| Select OPPORTUNITY message | open public Push Detail by exact public `messageId` | payload has no UserPosition/private risk/private Recheck identity |
| Select POSITION_RISK message | open owner-scoped private Push Detail by exact string-safe `messageId` | current-user ownership required |
| Refresh list | read the same authorized source set | no system/AI free-form/fake messages |
| Retry error | repeat safe GET | error never becomes an empty list |

Unread/count badges appear only when a real contracted field exists. No fake unread or message count is derived from visual examples.

## 10. Push Detail

**Entry:** exact message identity from Message Center.

**Context varies by source:**

- OPPORTUNITY: public opportunity identity, public original snapshot/status/time/description, and only public re-evaluation fields.
- POSITION_RISK: exact owner-scoped position identity, original private monitor snapshot, current authorized monitor/recheck state, and private change reason.

| Action | Result | Boundary |
|---|---|---|
| Compare original/current | show verified source-specific projection | no mixed public/private DTO |
| Open asset/analysis context | exact public context when identity exists | no private pushId path |
| Open position context | exact owner-scoped `positionId` | unavailable to other users |
| Retry | repeat read-only GET | no POST Recheck, monitor run, mutation, or trade |

Telegram is not a page action in this baseline. It remains a future external outlet.

## 11. Review

**Entry:** CLOSED position, exact analysis review link, or authorized opportunity review context.

| Action | Result | Boundary |
|---|---|---|
| Inspect timeline | combine immutable evidence, plan, user facts, monitor logs, message/recheck, and outcome | no live-state mutation |
| Submit user feedback where supported | record explicit review feedback | not a trade action |
| Compare rule versions | show traceable version evidence | no automatic deployment of a rule change |

Review classification such as `EXECUTED_VALID`, `EXECUTED_INVALID`, `MISSED_VALID`, `MISSED_INVALID`, `PUSHED_NOT_FILLED_VALID`, or `BLOCKED_BY_RISK_VALID` is evidence for human-reviewed iteration.

## 12. My and Settings

**Entry:** 我的 tab; contextual Settings link.

| Action | Result | Boundary |
|---|---|---|
| View account/session | show only real server-returned identity/session summary | no fabricated profile data |
| Logout | invalidate session and return to Login | clear private context |
| View rule/system version | show only real available version | otherwise hide or “暂未开放” |
| Select unsupported preference | disabled/“暂未开放” | no simulated save |

Community, referrals, paid plans, exchange order entry, and automatic trading are not inferred from Figma decoration and are not part of this baseline.

## 13. iPhone Behavior

- The five-tab bar respects safe areas and 44pt minimum interactive targets.
- Text supports Dynamic Type without clipping or overlapping adjacent controls.
- WKWebView/bridge payloads preserve exact string IDs and public/private state separation.
- Mobile refresh failure wins over stale cached success.
- Deep links retain authenticated Session/Cookie/CSRF behavior where required.
- Current simulator/WKWebView evidence is not real-device acceptance. P9 requires Xcode, installation, real iPhone navigation, network/session, rotation/text-size, and failure-path validation.

## 14. Explicitly Forbidden Interactions

- Card click that silently opens, closes, reduces, adds, reverses, or orders.
- Asset-card click that defaults to detail or changes a UserPosition.
- Automatic monitor-run or Push Recheck POST from a read-only page.
- AI role card that reveals unavailable conclusion/direction/confidence/metrics.
- Public OPPORTUNITY detail that exposes a private push identity or risk field.
- Error rendered as Empty, stale success rendered after error, or Missing recovered by symbol/latest fallback.
- Telegram/external notification presented as current Message Center capability.
