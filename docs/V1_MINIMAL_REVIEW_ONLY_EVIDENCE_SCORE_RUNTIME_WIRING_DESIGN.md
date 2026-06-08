# V1 Minimal Review-Only Evidence / Score Runtime Wiring Design

This document designs the smallest future `Evidence / Score review-only runtime status` slice. It does not implement Java, tests, dashboard changes, schema changes, endpoint changes, Push, Candidate, Decision, Point, or trading behavior.

## 1. Executive Summary

本任务只做 design，不实现。

Evidence / Score runtime status 的最小目标是：复用既有 `EvidenceService` / `ScoreService` / mapper / schema / dashboard detail assets，把 Evidence 与 Score 当前是否可读、是否完整、是否只能只读展示、是否因为缺失或边界不清而 fail-closed，设计成未来可通过 dashboard/API 看见的非交易状态。

Owner path 必须保留为：

```text
Watchlist / MarketQuote completed slices
  -> EvidenceService / Evidence owner path
  -> ScoreService / Score owner path
  -> existing controller/API/dashboard detail assets
  -> future minimal review-only Evidence / Score runtime status
```

本设计不要求新增 DTO / Validator / Assembler。未来如果 readiness gate 允许实现，也应优先使用 minimal map / existing VO / existing owner object，不创建新的 wrapper owner。

本设计可能需要未来新增一个最小只读 status endpoint，但是否新增 endpoint 必须在下一步 readiness gate 中判断；现有 `POST /api/evidence/build`、`POST /api/score/build`、`GET /api/score/list` 和 dashboard detail output 不能直接等同于 dedicated runtime status endpoint。

本设计不需要改 schema，不接 Push / Candidate / Decision / Point / Trading，不生成候选 / 点位 / 方向，不生成 entry / stop / TP / RR，不调用 order / execution / auto-trading。

下一步应该进入：`Minimal Review-Only Evidence / Score Runtime Wiring Implementation Readiness Gate`。

## 2. Owner Path To Preserve

固定 owner path：

```text
Watchlist / MarketQuote completed slices
  -> EvidenceService / Evidence owner path
  -> ScoreService / Score owner path
  -> existing controller/API/dashboard detail assets
  -> future minimal review-only Evidence / Score runtime status
```

按 #866 source-read 的现有资产展开：

```text
tm_evidence_item
  -> EvidenceItemMapper
  -> EvidenceService / EvidenceServiceImpl
  -> EvidenceController / DashboardController detail
  -> evidenceTopItems

tm_score_item
  -> ScoreItemMapper
  -> ScoreService / ScoreServiceImpl
  -> ScoreController / DashboardController detail
  -> scoreTopItems
```

未来实现不得绕过现有 Evidence / Score owner path。

不允许新增 Evidence / Score wrapper owner。

不允许直接接 Push / Candidate / Decision / Point。

不允许把 Display Slots 当候选池。

未来资产边界必须服从已完成的 Watchlist / MarketQuote review-only slices。Watchlist Pool 是候选边界，MarketQuote freshness / fallback status 是行情状态边界。

## 3. Minimal Future Status Mapping

| Status | Trigger condition | Dashboard/API copy | Candidate / Decision / Point / Push allowed? | Review-only? | Fail-closed? |
|---|---|---|---|---|---|
| `EVIDENCE_SCORE_REVIEW_ONLY_READY` | Evidence top items and Score top items are both readable from owner path for the selected review context, and source boundary is not ambiguous. | `Evidence / Score 只读状态可读，仅用于人工复核，不是候选、决策、点位或交易信号。` | No | Yes | No for status display; Yes for candidate/push/point use |
| `EVIDENCE_MISSING_FAIL_CLOSED` | Evidence rows are missing, `analysisId` is missing, or Evidence owner path returns empty while Score may or may not exist. | `Evidence 缺失，Evidence / Score 链路对候选、推送、点位继续 fail-closed。` | No | Yes | Yes |
| `SCORE_MISSING_FAIL_CLOSED` | Score rows are missing, `analysisId` is missing, or Score owner path returns empty while Evidence may or may not exist. | `Score 缺失，评分不能作为候选排序、决策或点位依据。` | No | Yes | Yes |
| `EVIDENCE_SCORE_INCOMPLETE_FAIL_CLOSED` | Evidence or Score exists only partially, counts are below expected minimal display threshold, or owner path cannot confirm both sides. | `Evidence / Score 不完整，只能展示为不完整只读状态。` | No | Yes | Yes |
| `EVIDENCE_SCORE_SOURCE_TRACE_PARTIAL` | Evidence or Score top items are readable, but provenance / source trace completeness cannot be fully proven from existing fields. | `Evidence / Score 来源追踪不完整，仅显示摘要，不作为候选、决策或点位信号。` | No | Yes | Yes for candidate/push/point use |
| `EVIDENCE_SCORE_BLOCKED_FAIL_CLOSED` | Watchlist boundary, MarketQuote freshness/fallback, analysis context, service read, or source ambiguity blocks safe interpretation. | `Evidence / Score 状态被阻断，候选、推送、点位、交易全部关闭。` | No | Yes | Yes |

## 4. Minimal Future Fields

允许字段：

| Field | Meaning |
|---|---|
| `status` | One of the allowed review-only statuses above. |
| `symbol` | Selected review symbol when the owner path has one. |
| `evidenceCount` | Count of Evidence rows available for display. |
| `scoreCount` | Count of Score rows available for display. |
| `evidenceAvailable` | Whether Evidence owner path returned readable rows. |
| `scoreAvailable` | Whether Score owner path returned readable rows. |
| `evidenceTopItems` | Existing top Evidence summaries if available. |
| `scoreTopItems` | Existing top Score summaries if available. |
| `sourceTraceComplete` | Whether source/provenance is complete enough for status display. If unknown, use `false` or partial reason. |
| `sourceHealth` | Review-only source health summary if existing owner path can provide it. |
| `reason` | Machine-readable reason for ready / missing / incomplete / blocked. |
| `message` | Human-readable safety message. |
| `reviewOnly = true` | Forced safety flag. |
| `notTradingSignal = true` | Forced safety flag. |
| `notCandidateSignal = true` | Forced safety flag. |
| `notDecisionSignal = true` | Forced safety flag. |
| `notPointSignal = true` | Forced safety flag. |
| `watchlistBounded = true` | Status is bounded by Watchlist Pool semantics. |
| `marketQuoteChecked = true` | Status acknowledges MarketQuote freshness/fallback boundary. |

不允许字段：

- candidate ranking
- final direction
- entry
- stop
- TP
- RR
- position size
- leverage
- order action
- Push send state

## 5. Dashboard/API Minimal Surface

未来最小 dashboard/API 显示必须包含：

- Evidence status；
- Score status；
- evidence count；
- score count；
- top evidence / top score summary if available；
- source trace completeness；
- review-only label；
- not candidate / not decision / not point label；
- Watchlist / MarketQuote boundary label。

如果 existing endpoint 已经足够，未来实现应优先复用。当前 #866 的 source-read 显示现有 build/list/detail API 不是 dedicated runtime status endpoint，因此下一步 readiness gate 必须判断是否新增最小只读 status endpoint。

如果新增 endpoint，只能返回最小 review-only status mapping，不得新增复杂评分卡片，不得生成候选，不得直接接 Candidate / Decision / Point。

Dashboard 可以复用现有 detail area 或新增最小 status/copy slot，但不能大改布局，不能把 Score 值渲染成候选排序、决策结论、点位准备度或交易方向。

## 6. Watchlist / MarketQuote Boundary

Evidence / Score slice 不得绕过 Watchlist Pool。

Evidence / Score slice 不得绕过 MarketQuote freshness / fallback status。

不得全市场默认扫描。

不得把 Display Slots 当候选池。

不在 Watchlist Pool 的资产不能进入候选 / 推送 / 点位链路。

MarketQuote stale / missing / fallback ambiguity 必须 fail closed。

Any ambiguity must fail closed for Candidate / Push / Decision / Point / Trading.

## 7. Minimal Future Implementation Boundary

如果下一步进入 readiness gate，未来最小实现必须限制为：

- 优先复用 existing `EvidenceService` / `ScoreService`；
- 优先复用 existing mapper / schema / dashboard detail assets；
- 可选最小 API/status mapping only after readiness gate；
- 可选最小 dashboard status/copy only after readiness gate；
- 不新增 DTO / Validator / Assembler；
- 不改 schema；
- 不接 Push；
- 不接 Candidate；
- 不接 Decision；
- 不接 Point；
- 不生成交易动作。

任何 future implementation 都必须保持 Evidence / Score output as review-only status display，不能把 Evidence / Score 变成候选生成器、决策生成器、点位生成器或交易信号。

## 8. Readiness Checklist

下一步 readiness gate 必须检查：

- 是否已有可复用 endpoint；
- 是否必须新增最小 endpoint；
- Evidence / Score 字段是否足够；
- source trace 字段是否足够；
- dashboard 是否已有 safe DOM slot；
- tests 是否已有；
- 是否可以不新增 DTO；
- 是否仍不接 Push / Candidate / Decision / Point / Trading；
- 是否能明确显示 `reviewOnly = true`、`notTradingSignal = true`、`notCandidateSignal = true`、`notDecisionSignal = true`、`notPointSignal = true`；
- 是否能在 Evidence / Score 缺失、不完整、source trace partial、MarketQuote ambiguity 时 fail closed。

## 9. Capability-Level Movement

当前 level: `REVIEW_ONLY_RUNTIME partial`。

本包是否提升 level: No, design only。

未来最小 Evidence / Score implementation 目标：`REVIEW_ONLY_RUNTIME partial for Evidence / Score slice`。

不等于 Production Wiring。

不等于 Push。

不等于 Candidate generation。

不等于 Decision generation。

不等于 Point generation。

不等于 Trading。

## 10. Freeze Rule Compliance

- 是否创建新骨架: No
- 是否复用 Cursor-era 资产: Yes
- 是否减少重复: Yes
- 是否提升 capability level: No, design only
- 是否接 service/runtime/dashboard/API: No, design only
- 是否符合 #830 审计建议: Yes

## 11. Final Recommendation

可以进入 `Minimal Review-Only Evidence / Score Runtime Wiring Implementation Readiness Gate`；最小实现只允许在 readiness gate 之后复用既有 Evidence / Score service、mapper、schema、dashboard detail assets，并可选新增最小只读 status endpoint 或最小 dashboard copy/status；禁止 Push、Candidate、Decision、Point、P359/P360、交易动作和新 DTO / Validator / Assembler，因为现有 Cursor-era owner path 已足够作为设计基础，下一步只需要验证实现边界是否可行。
