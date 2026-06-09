# Minimal Review-Only DecisionResult Runtime Wiring Design

# 1. Executive Summary

本任务只做 design，不实现。

DecisionResult review-only dashboard/API 的最小 runtime 目标是：复用现有 `tm_decision_result` / `DecisionResultMapper` / `DecisionService` / `DecisionResultVO` / `DashboardController` / dashboard detail owner path，把“已有 DecisionResult read-model 是否存在、是否完整、来源追踪是否完整、是否只读安全”整理成一个用户可见的 review-only status。

Owner path 是：

```text
Watchlist / MarketQuote / Evidence / Score completed slices
-> tm_decision_result / DecisionResultMapper
-> DecisionService / DecisionServiceImpl / DecisionResultVO
-> DashboardController summary/detail API
-> dashboard decision shells / source trace / display adapters
-> future minimal review-only DecisionResult status
```

是否需要新增 DTO / Validator / Assembler：No。未来最小实现必须优先使用 existing `DecisionResultVO`、`DashboardSummaryResponseVO`、`DashboardDetailResponseVO` 或 `Map`。

是否需要新增 endpoint：可能需要。现有 `/api/dashboard/summary` 和 `/api/dashboard/detail` 能读 DecisionResult，但没有 dedicated review-only DecisionResult status endpoint；下一步 readiness gate 再判断是否新增最小只读 `/api/dashboard/decision-result-status`。

是否需要改 schema：No。

是否需要接 Push / Candidate / Decision generation / Point / Trading：No。

是否会生成候选 / 点位 / 方向：No。`marketBiasHierarchy`、`tradeType`、`recommendedAction`、`entryZone`、`stopLoss`、`takeProfitRules`、`leverageSuggestion`、`positionSuggestion` 只能作为 existing read-model context；不得成为候选、点位、final direction 或交易动作。

下一步应该做：`Minimal Review-Only DecisionResult Runtime Wiring Implementation Readiness Gate`。

# 2. Owner Path To Preserve

固定 owner path：

```text
Watchlist / MarketQuote / Evidence / Score completed slices
-> tm_decision_result / DecisionResultMapper
-> DecisionService / DecisionServiceImpl / DecisionResultVO
-> DashboardController summary/detail API
-> dashboard decision shells / source trace / display adapters
-> future minimal review-only DecisionResult status
```

必须保持：

- `tm_decision_result` 是 persisted decision read-model source。
- `DecisionResultMapper` 是读取 latest decision / symbol detail / latest time / today count / reverse-signal count 的 mapper owner。
- `DecisionServiceImpl` 是 dashboard read-model enrichment owner；它补充 quote metadata、open position read-model、`readModelTruthStatus`、`readModelFallbackReason`。
- `DashboardController` 的 summary/detail path 是现有 API owner。
- `dashboard.html` 的 decision shells / source trace / display adapters 是现有 display owner。
- `DecisionResult` 只能读取已有 read model；不得生成新的 Decision。

禁止：

- 不允许新增 DecisionResult wrapper owner。
- 不允许绕过 `DecisionService` / `DecisionResultMapper` 直接拼 dashboard status。
- 不允许直接接 Push / Candidate / Decision generation / Point。
- 不允许把 `ai_role_results` 升级成 Three AI final arbiter。
- 不允许把 Display Slots 当候选池。
- 未来资产边界必须服从 Watchlist、MarketQuote、Evidence / Score 已完成 slice。

# 3. Minimal Future Status Mapping

Allowed status:

| Status | 触发条件 | Dashboard/API 文案 | Candidate / Decision generation / Point / Push | Review-only | Fail-closed |
|---|---|---|---|---|---|
| `DECISIONRESULT_REVIEW_ONLY_READY` | latest DecisionResult 存在，`decisionId` / `analysisId` / `symbol` / `createTime` 存在，`readModelTruthStatus=FULL`，source trace required anchors 可见。 | DecisionResult 只读状态可读；这是已有 read model，不是新的决策生成。 | No | Yes | No |
| `DECISIONRESULT_MISSING_FAIL_CLOSED` | 按 symbol 读取不到 DecisionResult，或 latest decision 为空。 | DecisionResult 缺失；候选、决策生成、点位、Push、交易全部关闭。 | No | Yes | Yes |
| `DECISIONRESULT_READ_MODEL_PARTIAL` | `readModelTruthStatus=PARTIAL` 或 `readModelFallbackReason=LEGACY_MISSING:*`。 | DecisionResult read model 不完整；仅显示降级原因，不作为交易信号。 | No | Yes | Yes |
| `DECISIONRESULT_SOURCE_TRACE_PARTIAL` | DecisionResult 存在，但 source trace missing fields、runtime kline unavailable、derivatives-risk context fail-closed 或 provenance 不完整。 | Source trace / provenance 不完整；只读展示，不能升级为点位或执行建议。 | No | Yes | Yes |
| `DECISIONRESULT_AI_ROLE_PARTIAL` | `ai_role_results` 缺失、AI conflict 字段缺失，或 AI role output 只作为 raw context。 | AI role results 缺失或仅为原始上下文；不是 Three AI 裁决，也不是 final direction。 | No | Yes | Yes |
| `DECISIONRESULT_STALE_OR_UNKNOWN_FAIL_CLOSED` | `createTime` 缺失、无法判断最新性，或未来 stale threshold 判定超时。 | DecisionResult 最新性未知或过期；fail-closed，只读展示。 | No | Yes | Yes |
| `DECISIONRESULT_BLOCKED_FAIL_CLOSED` | symbol 为空、owner path 异常、read-model ambiguity、任何边界无法判定。 | DecisionResult 状态被阻断；所有候选、决策生成、点位、Push、交易动作关闭。 | No | Yes | Yes |

# 4. Minimal Future Fields

允许字段：

- `status`
- `symbol`
- `decisionId`
- `analysisId`
- `decisionCreateTime`
- `lastDecisionTime` if available
- `decisionAgeSeconds` / `staleThresholdSeconds` if available
- `decisionAvailable`
- `readModelTruthStatus`
- `readModelFallbackReason`
- `sourceTraceStatus`
- `sourceTraceMissingFields`
- `sourceTraceComplete`
- `aiRoleResultsPresent`
- `aiConflictLevel`
- `aiConflictScore`
- `sourceHealth`
- `reason`
- `message`
- `reviewOnly = true`
- `notTradingSignal = true`
- `notCandidateSignal = true`
- `notDecisionGeneration = true`
- `notPointSignal = true`
- `watchlistBounded = true`
- `marketQuoteChecked = true`
- `evidenceScoreChecked = true`
- `displaySlotsAreCandidatePool = false`
- `failClosed`

不允许字段：

- candidate ranking
- candidate score
- final direction
- entry
- stop
- TP
- RR
- position size
- leverage
- order action
- Push send state
- sendable message

特别说明：existing `DecisionResultVO` 里已有 `marketBiasHierarchy`、`tradeType`、`recommendedAction`、`entryZone`、`stopLoss`、`takeProfitRules`、`leverageSuggestion`、`positionSuggestion`。这些字段未来最小 status endpoint 默认不应直接暴露；如果 dashboard/detail 已显示，也必须配套“read-model context only / not trade instruction / not point”文案。

# 5. Dashboard/API Minimal Surface

未来最小 dashboard/API 必须显示：

- DecisionResult status；
- symbol；
- latest DecisionResult 是否存在；
- read model completeness；
- read model fallback reason；
- source trace completeness；
- ai role results present / missing；
- source health；
- review-only label；
- not candidate / not decision generation / not point / not trading signal label；
- Watchlist / MarketQuote / Evidence / Score boundary label；
- Display Slots 不是候选池。

Endpoint 策略：

- Existing `/api/dashboard/summary` 和 `/api/dashboard/detail` 若足够，则优先复用。
- 若 dedicated status 缺失且 readiness gate 判定需要，未来只允许新增最小只读 endpoint，例如 `GET /api/dashboard/decision-result-status?symbol=...`。
- Endpoint 可返回 `Map` / existing object / existing VO projection；不得新增 DTO。
- Endpoint 必须只读；不得写入 decision、plan、candidate、push 或 audit。

Dashboard 策略：

- 优先复用已有 decision shell / workbench / source trace 区域。
- 若需要新增 DOM，只允许最小 `decisionResultStatusPanel` / status line / copy。
- 不允许复杂决策卡片。
- 不允许新增 final direction 卡片。
- 不允许把 ExecutionPlan 或 BoundaryCandidate 内容混入 DecisionResult status owner。
- 不允许直接接 Candidate / Decision generation / Point。

# 6. Watchlist / MarketQuote / Evidence / Score Boundary

必须明确：

- DecisionResult slice 不得绕过 Watchlist Pool。
- DecisionResult slice 不得绕过 MarketQuote freshness/fallback status。
- DecisionResult slice 不得绕过 Evidence / Score completeness status。
- 不得全市场默认扫描。
- 不得把 Display Slots 当候选池。
- 不在 Watchlist Pool 的资产不能进入候选/推送/点位链路。
- MarketQuote stale/missing/fallback ambiguity 必须 fail closed。
- Evidence / Score missing/incomplete 必须 fail closed。
- DecisionResult read-model partial / stale / ambiguous 必须 fail closed。
- `ai_role_results` 只能显示为 raw read-model context；不得成为 Three AI expansion、final arbiter 或 final direction。

# 7. Minimal Future Implementation Boundary

如果下一步进入 readiness gate，未来最小实现必须限制：

- 优先复用 existing `DecisionService` / `DecisionServiceImpl`。
- 优先复用 existing `DecisionResultMapper` / `tm_decision_result`。
- 优先复用 existing `DecisionResultVO` / dashboard summary/detail response。
- 优先复用 existing `DashboardController`。
- 优先复用 existing dashboard decision shells / source trace / display adapters。
- 可选最小 API/status mapping only after readiness gate。
- 可选最小 dashboard status/copy only after readiness gate。
- 不新增 DTO / Validator / Assembler。
- 不改 schema。
- 不接 Push。
- 不接 Candidate。
- 不生成新的 Decision。
- 不接 Point。
- 不生成 final direction。
- 不生成交易动作。

未来最小实现最多允许候选文件：

- existing `DashboardController` only if endpoint missing；
- existing `DecisionServiceImpl` only if absolutely necessary；
- existing `dashboard.html` only minimal status/copy/DOM；
- existing `DashboardControllerTest` / `DecisionServiceImplTest` / mapper or dashboard display tests if needed；
- source-of-truth docs。

# 8. Readiness Checklist

下一步 readiness gate 必须检查：

- 是否已有可复用 endpoint；
- 是否必须新增最小 endpoint；
- 是否可以不用新 DTO，直接返回 Map / existing VO / existing response object；
- `DecisionResultVO` 字段是否足够；
- `readModelTruthStatus` / `readModelFallbackReason` 是否足够；
- source trace 字段是否足够；
- ai role fields 是否足够；
- stale / missing / partial 判定是否足够；
- dashboard 是否已有 safe DOM slot；
- tests 是否已有；
- 是否可以不改 schema；
- 是否仍不接 Push / Candidate / Decision generation / Point / Trading；
- 是否避免暴露 `entryZone` / `stopLoss` / `takeProfitRules` / `leverageSuggestion` 为 status 字段；
- 是否明确 Display Slots 不是候选池。

# 9. Capability-Level Movement

当前 level: `REVIEW_ONLY_RUNTIME partial`。

本包是否提升 level: No, design only。

未来最小 DecisionResult implementation 目标：`REVIEW_ONLY_RUNTIME partial for DecisionResult slice`。

不等于 Production Wiring。

不等于 Push。

不等于 Candidate generation。

不等于 Decision generation。

不等于 Point generation。

不等于 final direction。

不等于 Trading。

# 10. Freeze Rule Compliance

- 是否创建新骨架: No
- 是否复用 Cursor-era 资产: Yes
- 是否减少重复: Yes
- 是否提升 capability level: No, design only
- 是否接 service/runtime/dashboard/API: No, design only
- 是否符合 #830 审计建议: Yes

# 11. Final Recommendation

明确结论：可以进入 `Minimal Review-Only DecisionResult Runtime Wiring Implementation Readiness Gate`。最小实现大概允许一个只读 DecisionResult status endpoint 或复用 existing summary/detail API、最小 dashboard status/copy/DOM、targeted tests 和 source-of-truth docs；禁止新增 DTO / Validator / Assembler、禁止改 schema、禁止接 Push、Candidate、Decision generation、Point、final direction、order / execution / auto-trading。它不是 Push，因为不发送任何消息；不是 Candidate，因为不排序或生成候选；不是 Decision generation，因为只读取已有 persisted DecisionResult；不是 Point，因为不生成 entry / stop / TP / RR；不是 P359/P360，因为不恢复 runtime-candidate wrapper / assembler 路线。
