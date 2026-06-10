# Next Minimal Runtime Slice Selection After DecisionResult Closure

# 1. Executive Summary

当前已完成五条 review-only runtime 小闭环：

1. PositionSync + Dashboard review-only status；
2. Watchlist + RuleConfig + Dashboard/API review-only status；
3. MarketQuote freshness / fallback / dashboard API status；
4. Evidence / Score review-only runtime status；
5. DecisionResult review-only dashboard/API status。

下一条最小 runtime slice 推荐：**ExecutionPlan / BoundaryCandidate review-only display continuation**。

原因：DecisionResult 已经完成 implementation、verification、visual closure 后，最自然的下游只读状态是 PlanBoundary / ExecutionPlan / BoundaryCandidate 的展示延续。现有 `BoundaryCandidateService`、`PlanService`、`ExecutionPlanMapper`、`ExecutionPlanDO/VO`、`DefaultPlanBoundaryDisplayAdapter`、`DefaultExecutionPlanDisplayAdapter`、`DashboardController`、dashboard workbench / placeholder DOM、以及 targeted display adapter tests 已经存在；下一步做 source read 可以确认哪些资产只适合展示 readiness/display 状态，哪些字段必须保持 fail-closed、非候选、非点位、非交易语义。

为什么它是最大安全推进：它复用已有 owner path 和 dashboard display assets，避免新增 DTO / Validator / Assembler / Orchestrator；并且下一步只做 source read，不进入 implementation。它比 Push、Three AI、Point generation、Position Monitor expansion 更安全，也比 Data Source Health 重复度更低。

为什么不是 Push：Push external channel、sendable message、真实发送仍未授权，当前只允许 review-only 状态。

为什么不是 Three AI：Three AI / multi-agent 会引入 provider orchestration、预算、fallback、真实裁决链路和 final arbiter 风险，不是最小 review-only runtime slice。

为什么不是 Point：Point 会靠近 entry / stop / TP / RR、final direction 和可执行交易解释，当前仍冻结。

为什么不是 Position Monitor expansion：PositionSync 已完成只读状态；Position Monitor expansion 容易滑向 close / reverse / open action suggestion。

为什么不是 P359/P360：P359 未合并且 PR #829 已关闭，P360 禁止启动；继续 P359/P360 会回到重复 DTO / Validator / Assembler / runtime-candidate wrapper 膨胀。

下一步具体做：**Source Read for ExecutionPlan / BoundaryCandidate review-only display continuation**。下一步仍然只读，不实现。

# 2. Current Completed Runtime Slices

| Completed slice | Capability | Output | Boundary |
|---|---|---|---|
| PositionSync + Dashboard review-only status | `REVIEW_ONLY_RUNTIME partial` | Dashboard 显示 provider / fallback / simulated / freshness / last sync / open position count。 | Review-only；不是 Production Wiring；不是交易能力。 |
| Watchlist + RuleConfig + Dashboard/API review-only status | `REVIEW_ONLY_RUNTIME partial` | `/api/rule/push-watchlist` 与 dashboard 显示 Watchlist Pool 状态和 Display Slots 边界。 | Review-only；不是 Push；不是候选生成；不是交易能力。 |
| MarketQuote freshness / fallback / dashboard API status | `REVIEW_ONLY_RUNTIME partial` | `/api/market/quote-status` 与 dashboard 显示 quote source / freshness / fallback / source health。 | Review-only；不是 Candidate；不是 Point；不是交易信号。 |
| Evidence / Score review-only runtime status | `REVIEW_ONLY_RUNTIME partial` | `/api/dashboard/evidence-score-status` 与 dashboard 显示 Evidence / Score availability/count/top summary/source health。 | Review-only；不是 Candidate；不是 Decision；不是 Point；不是交易能力。 |
| DecisionResult review-only dashboard/API status | `REVIEW_ONLY_RUNTIME partial` | `/api/dashboard/decision-result-status` 与 dashboard 显示 DecisionResult availability、ai_role_results/source trace/source health、fail-closed 和 safety copy。 | Review-only；不是 Candidate；不是 Decision generation；不是 Point；不是交易能力。 |

这些小闭环都只提供用户可见的安全状态，不等于 Production Wiring，不等于 Push，不等于 Candidate / Decision generation / Point generation，也不等于交易能力。

# 3. Candidate Slice Comparison

| Candidate slice | Existing assets | User-visible value | Runtime/API readiness | Risk | Duplicate risk | Recommendation |
|---|---|---|---|---|---|---|
| ExecutionPlan / BoundaryCandidate review-only display continuation | `BoundaryCandidateService`, `BoundaryCandidateServiceImpl`, `PlanService`, `PlanServiceImpl`, `ExecutionPlanMapper`, `ExecutionPlanDO/VO`, `DefaultPlanBoundaryDisplayAdapter`, `DefaultExecutionPlanDisplayAdapter`, `DashboardController`, `DashboardDetailResponseVO`, `dashboard.html` `planBoundaryPlaceholderCard`, dashboard workbench render path, display adapter tests. | 高。DecisionResult 之后用户自然会问 PlanBoundary / ExecutionPlan 是否可见、是否完整、是否 fail-closed。 | 高。owner assets、dashboard detail display、placeholder/status DOM 和 tests 都存在。 | 高。靠近 entry / stop / TP / RR、readiness、advisory execution 和 point semantics，必须 source-read first。 | 中。若新增 wrapper 会重复；若复用 owner path 则可控。 | **推荐。下一步只做 source read，不实现。** |
| Data Source Health dashboard status | `DataSourceHealthDO`, MarketQuote `sourceHealth`, Evidence / Score `sourceHealth`, DecisionResult `sourceHealth`, dashboard source-health labels/tests. | 中。能集中解释数据源健康。 | 中。局部 sourceHealth 已完成，但 dedicated data-source-health owner/API 不清晰。 | 低中。主要是状态展示。 | 中高。容易重复 MarketQuote / Evidence / Score / DecisionResult 已完成的 source-health 状态。 | 暂缓，避免重复刚完成的 source-health slices。 |
| Review / Replay result status | `ReviewResultDO`, `ReviewResultMapper`, `ReviewService`, `ReviewController`, `PushRecheckService`, `PushRecheckReplaySummaryVO`, `tm_review_result`, replay tests. | 中。可显示复盘/回放状态。 | 中。Review read/write 和 replay assets 存在，但 replay 与 PushRecheck 交织。 | 中高。容易滑向 PushRecheck / recheck / replay execution semantics。 | 中。可能与 existing review aggregate / replay summary 重叠。 | 暂缓，先读 PlanBoundary / ExecutionPlan owner boundary。 |
| Internal Push preview status only | `internalPushPreviewDisplay`, internal push preview safety copy, PushRecheck assets/tests. | 中。可见内部 Push preview 状态。 | 中。旧链和 dashboard placeholder 存在。 | 高。外部通道、sendable message、Push send 语义敏感。 | 中高。容易复活 Candidate/Push skeleton。 | 不选。Push 仍冻结。 |
| Position Monitor manual-input source read | PositionSync、RealPosition、monitor alert、Position Monitor docs/assets。 | 中。持仓监控可见性有用。 | 中。PositionSync 已完成，monitor action source 需单独审计。 | 高。容易滑向 close/reverse/open action suggestion。 | 中。会扩张 Position Monitor。 | 暂缓，不做 expansion。 |
| Three AI / multi-agent status read-only inventory | `ai_role_results`, DecisionResult AI role summary, AI conflict docs/assets. | 中。可展示 AI role / conflict status。 | 中低。真实 multi-agent provider orchestration 未授权。 | 高。容易被误读成 final arbiter / final decision。 | 高。可能新增 provider/orchestration wrappers。 | 不选，保持冻结。 |

# 4. Recommended Next Slice

推荐：**ExecutionPlan / BoundaryCandidate review-only display continuation**。

Owner path 候选：

```text
DecisionResult completed slice
-> DashboardController / DashboardDetailResponseVO
-> PlanBoundaryDisplayAdapter / DefaultPlanBoundaryDisplayAdapter
-> ExecutionPlanDisplayAdapter / DefaultExecutionPlanDisplayAdapter
-> BoundaryCandidateService / PlanService / ExecutionPlanMapper
-> dashboard PlanBoundary / ExecutionPlan read-only display status
```

现有 API / dashboard 可能复用：

- `/api/dashboard/detail?symbol=...` 已经携带 `planBoundaryDisplay` / `executionPlanDisplay` / risk guard / source trace 等 detail read-model；
- `dashboard.html` 已有 `planBoundaryPlaceholderCard` 和 workbench ExecutionPlan/PlanBoundary render path；
- display adapter tests 已覆盖 fail-closed、安全标签、不可执行语义。

它能形成 review-only runtime 小闭环，但下一步必须先 source read，确认 dedicated status endpoint/panel 是否已有、哪些字段可安全显示、哪些字段必须继续标为 incomplete / fail-closed / not trading instruction。

预计不需要新 DTO / Validator / Assembler。若未来进入 implementation，优先使用 existing `DashboardDetailResponseVO`、existing display adapters、existing controller/detail path 或最小 Map status；不得新增 wrapper owner。

禁止接 Push / Candidate / Decision generation / Point / Trading。也不得把 BoundaryCandidate / ExecutionPlan display status 升级为 point generation 或 executable plan；本 slice 只允许读“已有 display/read-model 是否可见、完整、fail-closed、review-only”。

为什么这是 DecisionResult 后的自然后续：DecisionResult 已经证明 read-model status 可见；PlanBoundary / ExecutionPlan 是 dashboard detail 中紧邻 DecisionResult 的下游展示层。下一步审计它们，比跳到 Push、Point、Three AI 或 Position Monitor expansion 更符合最小、安全、可验证推进。

# 5. Rejected Options

- Push external channel：暂不选。外部发送和 sendable message 需要单独 C-level 授权，当前没有授权。
- Three AI：暂不选。真实 GPT / Gemini / Grok provider orchestration、预算、缓存、fallback 和 final arbiter 风险太高。
- Point generation：暂不选。任何 entry / stop / TP / RR、RR、final direction 或 executable point 都继续冻结。
- Data Source Health dashboard status：暂不选。source health 已在 MarketQuote、Evidence / Score、DecisionResult 三个小闭环中显示，单独推进会有重复风险。
- Review / Replay result status：暂不选。ReviewResult 与 PushRecheck replay 资产存在，但 replay/recheck 容易靠近 Push or historical re-execution semantics。
- Position Monitor expansion：暂不选。PositionSync 已完成可见状态；扩张 monitor 容易靠近 close/reverse/open action suggestion。
- P359 / P360：继续冻结。P359 未合并，P360 禁止启动；不能回到新 runtime candidate wrapper / assembler 路线。

# 6. Next Step Decision

Decision: **A. GO: Source Read for ExecutionPlan / BoundaryCandidate review-only display continuation**。

下一步命名：`Source Read for ExecutionPlan / BoundaryCandidate review-only display continuation`。

下一步必须保持 source-read only，不实现；不得新增 Java、test、dashboard、schema、config、pom；不得新增 DTO / Validator / Assembler；不得接 Push / Candidate / Decision generation / Point / Trading。

# 7. Freeze Rule Compliance

- 是否创建新骨架: No
- 是否复用 Cursor-era 资产: Yes
- 是否减少重复: Yes
- 是否提升 capability level: No, selection only
- 是否接 service/runtime/dashboard/API: No, selection only
- 是否符合 #830 审计建议: Yes
