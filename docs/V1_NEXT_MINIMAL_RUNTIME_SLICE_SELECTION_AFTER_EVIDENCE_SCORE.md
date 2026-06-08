# Next Minimal Runtime Slice Selection After Evidence / Score Closure

# 1. Executive Summary

当前已完成四条 review-only runtime 小闭环：

1. PositionSync + Dashboard review-only status；
2. Watchlist + RuleConfig + Dashboard/API review-only status；
3. MarketQuote freshness / fallback / dashboard API status；
4. Evidence / Score review-only runtime status。

下一条最小 runtime slice 推荐：**DecisionResult review-only dashboard/API status**。

原因：DecisionResult 是 Evidence / Score 后的自然下游 read-model owner，现有 `DecisionResultMapper`、`DecisionService`、`DecisionResultVO`、`DashboardController`、`/api/dashboard/summary`、`/api/dashboard/detail` 和 dashboard detail 展示资产已经存在；下一步做 source read 可以确认哪些字段适合只读状态展示，哪些字段必须保持为非候选、非决策生成、非点位、非交易语义。这是最大安全推进，因为它复用已有 read-model / dashboard/API 资产，而不是新增 DTO / Validator / Assembler，也不是直接进入 ExecutionPlan、Push、Point 或 Three AI。

为什么不是 Push：Push external channel 和 sendable message 仍然需要单独 C-level 授权，当前只允许 review-only runtime 状态展示。

为什么不是 Three AI：Three AI / multi-agent 会引入 provider orchestration、预算、fallback、真实裁决链路和 final-decision 风险，不是当前最小 runtime slice。

为什么不是 Point：Point 会靠近 entry / stop / TP / RR、final direction 和交易解释，当前仍然冻结。

为什么不是 Position Monitor expansion：Position Monitor 容易被误读为 close/reverse/open action suggestion；PositionSync 已完成只读状态，下一步不应扩张监控动作。

为什么不是 P359/P360：P359 未合并且 PR #829 已关闭，P360 禁止启动；继续 P359/P360 会回到重复 DTO / Validator / Assembler / runtime-candidate wrapper 膨胀。

下一步具体做：**Source Read for DecisionResult review-only dashboard/API status**。下一步仍然只读，不实现。

# 2. Current Completed Runtime Slices

| Completed slice | Capability | Output | Boundary |
|---|---|---|---|
| PositionSync + Dashboard review-only status | `REVIEW_ONLY_RUNTIME partial` | Dashboard 显示 provider / fallback / simulated / freshness / last sync / open position count。 | Review-only；不是 Production Wiring；不是交易能力。 |
| Watchlist + RuleConfig + Dashboard/API review-only status | `REVIEW_ONLY_RUNTIME partial` | `/api/rule/push-watchlist` 与 dashboard 显示 Watchlist Pool 状态和 Display Slots 边界。 | Review-only；不是 Push；不是候选生成；不是交易能力。 |
| MarketQuote freshness / fallback / dashboard API status | `REVIEW_ONLY_RUNTIME partial` | `/api/market/quote-status` 与 dashboard 显示 quote source / freshness / fallback / source health。 | Review-only；不是 Candidate；不是 Point；不是交易信号。 |
| Evidence / Score review-only runtime status | `REVIEW_ONLY_RUNTIME partial` | `/api/dashboard/evidence-score-status` 与 dashboard 显示 Evidence / Score availability/count/top summary/source health。 | Review-only；不是 Candidate；不是 Decision；不是 Point；不是交易能力。 |

这些小闭环都只提供用户可见的安全状态，不等于 Production Wiring，不等于 Push，不等于 Candidate / Decision / Point generation，也不等于交易能力。

# 3. Candidate Slice Comparison

| Candidate slice | Existing assets | User-visible value | Runtime/API readiness | Risk | Duplicate risk | Recommendation |
|---|---|---|---|---|---|---|
| DecisionResult review-only dashboard/API status | `DecisionResultMapper`, `DecisionService`, `DecisionResultVO`, `DashboardSummaryResponseVO`, `DashboardDetailResponseVO`, `/api/dashboard/summary`, `/api/dashboard/detail`, dashboard detail display, source trace tests. | 高。能把 Evidence / Score 下游 read-model 状态、read-model completeness、latest decision availability、source trace / dashboard detail 边界展示清楚。 | 中高。已有 summary/detail API 和 dashboard detail owner path，但 dedicated review-only DecisionResult status endpoint/panel 是否存在仍需 source read。 | 中。`DecisionResultVO` 包含 bias、tradeType、recommendedAction、entry/stop/TP 文本等容易被误读的字段。 | 低中。若只复用 read-model owner path 并避免 wrapper，重复风险可控。 | **推荐。下一步做 source read，不实现。** |
| ExecutionPlan / BoundaryCandidate review-only display continuation | `BoundaryCandidateService`, `BoundaryCandidateDTO`, `PlanService`, `ExecutionPlanVO/DO/Mapper`, `DefaultExecutionPlanDisplayAdapter`, `DefaultPlanBoundaryDisplayAdapter`, dashboard workbench display, owner-path tests. | 中高。可见 plan boundary / execution plan readiness。 | 高。资产丰富且测试较强。 | 高。靠近 entry / stop / TP / RR、readiness、advisory execution 语义。 | 中。容易和 point proposal / SourceOwned wrappers 再次缠绕。 | 暂缓。等 DecisionResult status 边界读清楚后再考虑。 |
| Data Source Health dashboard status | `DataSourceHealthDO`, MarketQuote `sourceHealth`, Evidence / Score `sourceHealth`, SourceTrace fallback/missing fields, dashboard source-health labels. | 中。可解释数据源健康。 | 中。已有局部 sourceHealth，但 dedicated data-source-health table/API/owner path 不清晰。 | 低中。主要是状态显示。 | 中。可能重复 MarketQuote 和 Evidence / Score 已完成的 source-health 展示。 | 暂缓，避免刚完成的 sourceHealth 语义重复。 |
| Review / Replay result status | `ReviewService`, `ReviewResultMapper`, `tm_review_result`, `ReviewAggregateService`, `PushRecheck` replay summary, `ReviewController`. | 中。能显示人工复盘/回放结果。 | 中。Review write/read 与 aggregate path 存在，但 replay 可能靠近 PushRecheck。 | 中。可能滑向 recheck / Push / feedback correction。 | 中。ReviewResult 与 SourceTrace review boundary skeleton 有重名/重叠风险。 | 暂缓，先处理 DecisionResult read-model 状态。 |
| Internal Push preview status only | Earlier internal push preview / recheck / dashboard display artifacts exist. | 中。可见内部 Push preview 状态。 | 中。旧链资产存在。 | 高。外部通道、sendable message、Push send 语义非常敏感。 | 中高。容易复活 Candidate/Push skeleton。 | 不选。Push 仍冻结。 |
| Position Monitor manual-input source read | PositionSync、RealPosition、monitor alert、position monitor docs/assets 存在。 | 中。持仓可见性有用。 | 中。PositionSync 已完成，monitor action source 需单独审计。 | 高。容易滑向 close/reverse/open action suggestion。 | 中。会扩张 Position Monitor。 | 暂缓，不做 expansion。 |
| Three AI / multi-agent status read-only inventory | `ai_role_results`, `DecisionEngineService`, `AiConflictResolverService`, AI conflict fields, dashboard AI conflict render. | 中。可展示 AI conflict / role summary。 | 中低。真实三 AI provider orchestration 不存在或未授权。 | 高。容易被误读成 final arbiter / final decision。 | 高。可能创建新 provider/orchestration wrapper。 | 不选。保持冻结。 |

# 4. Recommended Next Slice

推荐：**DecisionResult review-only dashboard/API status**。

Owner path 候选：

```text
Evidence / Score completed slice
-> DecisionResultMapper / tm_decision_result
-> DecisionService / DecisionResultVO
-> DashboardController summary/detail API
-> dashboard detail read-model display
-> future minimal review-only DecisionResult status surface
```

现有 API / dashboard 可能复用：

- `/api/dashboard/summary` 已返回 latest `DecisionResultVO` list；
- `/api/dashboard/detail?symbol=...` 已返回 selected `DecisionResultVO`、sourceTrace、PlanBoundary / ExecutionPlan / RiskActionGuard display objects、Evidence / Score top items；
- dashboard 已经消费 decision fields、source trace、plan boundary、execution plan、risk guard 等 detail 数据。

它能形成 review-only runtime 小闭环，但下一步必须先 source read，确认 dedicated status endpoint/panel 是否已有、哪些字段可安全展示、哪些字段必须被标为 context-only / not final decision / not point / not trading signal。

预计不需要新 DTO / Validator / Assembler；如果未来实现，优先用 Map / existing VO / existing `DecisionResultVO` / existing dashboard response owner path。

禁止接 Push / Candidate / Point / Trading。也不得把 DecisionResult read-model status 升级为 Decision generation；本 slice 只允许读“已有 decision read model 是否可见、完整、fail-closed、review-only”。

为什么这是 Evidence / Score 后的自然后续：Evidence / Score 已经把上游证据和评分做成只读状态；DecisionResult 是下游 read-model 聚合点，适合检查它是否能以安全状态显示而不产生候选、点位或交易指令。

# 5. Rejected Options

- Push external channel：暂不选。外部发送和 sendable message 需要单独 C-level 授权，当前没有授权。
- Three AI：暂不选。真实 GPT / Gemini / Grok provider orchestration、预算、缓存、fallback 和 final arbiter 风险太高。
- Point generation：暂不选。任何 entry / stop / TP / RR、RR、final direction 或 executable point 都继续冻结。
- ExecutionPlan / BoundaryCandidate continuation：暂不选。资产真实且强，但更接近 advisory execution / point semantics；DecisionResult read-model status 边界应先读清楚。
- Position Monitor expansion：暂不选。PositionSync 已完成可见状态；扩张 monitor 容易靠近 close/reverse/open action suggestion。
- P359 / P360：继续冻结。P359 未合并，P360 禁止启动；不能回到新 runtime candidate wrapper / assembler 路线。

# 6. Next Step Decision

Decision: **A. GO: Source Read for DecisionResult review-only dashboard/API status**。

下一步命名：`Source Read for DecisionResult review-only dashboard/API status`。

下一步必须保持 source-read only，不实现；不得新增 Java、test、dashboard、schema、config、pom；不得新增 DTO / Validator / Assembler；不得接 Push / Candidate / Decision generation / Point / Trading。

# 7. Freeze Rule Compliance

- 是否创建新骨架: No
- 是否复用 Cursor-era 资产: Yes
- 是否减少重复: Yes
- 是否提升 capability level: No, selection only
- 是否接 service/runtime/dashboard/API: No, selection only
- 是否符合 #830 审计建议: Yes
