# V1 Next Minimal Runtime Slice Selection After Missed Opportunity / Review Archive

## 1. Executive Summary

本包只做 selection（选择）和 source-of-truth（事实源）交接，不做 implementation（实现）。

当前已合并主线为 `239664d docs(missed): record review archive visual closure (#928)`。Missed Opportunity / Review Archive status 已完成 implementation、verification、visual closure，并成为第 10 个 `REVIEW_ONLY_RUNTIME partial` 小闭环。

推荐第 11 个最小 runtime slice 为：`RiskActionGuard read-only status`。

选择理由：RiskActionGuard 已有 `DefaultRiskActionGuardDisplayAdapter`、`DashboardDetailResponseVO.RiskActionGuardDisplayVO`、dashboard detail display path、dashboard placeholder/copy、以及 targeted tests。它天然表达风险阻断和 fail-closed 语义，比 Push、Candidate、Three AI、Position Monitor expansion、SourceTrace aggregate 等候选更小、更安全、更接近现有 owner path。

下一允许动作：`Source Read for RiskActionGuard read-only status`。

能力层级不提升，仍为 `REVIEW_ONLY_RUNTIME partial`。本包不是 Push，不是 Candidate generation，不是 Decision generation，不是 Point generation，不是 final direction / entry / stop / TP / RR，不是 order / execution / auto-trading。

## 2. Current Merged Main

- Current merged main: `239664d docs(missed): record review archive visual closure (#928)`
- Current capability level: `REVIEW_ONLY_RUNTIME partial`
- Current completed slices: 10
- Current package risk: A
- Current package scope: selection docs and source-of-truth updates only

## 3. Completed Slices

1. `PositionSync + Dashboard review-only status`
2. `Watchlist + RuleConfig + Dashboard/API review-only status`
3. `MarketQuote freshness / fallback / dashboard API status`
4. `Evidence / Score review-only runtime status`
5. `DecisionResult review-only dashboard/API status`
6. `ExecutionPlan / BoundaryCandidate review-only runtime status`
7. `Review / Replay result status`
8. `Data Source Health dashboard/API status`
9. `RuleConfig runtime audit / rule explainability`
10. `Missed Opportunity / Review Archive status`

这些闭环都是 review-only，不是 Production Wiring，不是交易能力。

## 4. Candidate Next Slices Considered

| Candidate | Existing assets | Advantage | Risk / conflict | Recommendation |
|---|---|---|---|---|
| RiskActionGuard read-only status | `DefaultRiskActionGuardDisplayAdapter`, `RiskActionGuardDisplayAdapter`, `DashboardDetailResponseVO.RiskActionGuardDisplayVO`, dashboard placeholder/copy, `RuleEngineService` guard consumption, targeted tests | 最小、只读、fail-closed 语义天然清楚，能复用现有 dashboard detail owner path | 需要先确认 dedicated status endpoint/panel 是否已有；不得变成 Point / execution guard implementation | Select |
| Position Monitor manual-input / monitor status | PositionSync foundation, position monitor docs/tests, legacy monitor concepts | 用户可见价值高 | 容易进入 close/reverse/open/action suggestion，且 PositionSync 已是完成闭环；monitor expansion 边界更大 | Defer |
| Internal Push preview / recheck status | internal push preview display, push recheck service/log assets | 现有资产多 | Push/recheck 语义靠近 external channel、send/recheck trigger，风险高 | Defer |
| Candidate preview / ranking status | ReviewOnlyCandidateAttention / CandidatePreviewGuard assets | 可延续历史 Candidate / Push 链 | Candidate/ranking 语义直接靠近 Candidate generation，当前禁止 | Reject for now |
| Three AI / AI conflict status | DecisionResult `ai_role_results`, role/conflict copy | 可读性强 | 易误入 provider orchestration / final arbiter / decision generation | Defer |
| SourceTrace / RuntimeKline / DataQuality / MultiTimeframe aggregate | 多个 source binding / dashboard/runtime assets | 长期价值高 | 横跨多个 source families，容易复活 DTO/Validator/Assembler skeleton 和 aggregate schema/service 需求 | Defer |
| Account risk / system health / macro-news status | SystemHealthService, account risk snapshot assets, macro evidence concepts | 可做运维/风险可见性 | Account risk 贴近 PushSnapshot/PushRecheck；macro/news 可能触发 external API refresh；system health 已偏 workflow/ops | Defer |
| Other smaller slice found | RiskActionGuard placeholder and display adapter path is the smallest discovered review-only runtime gap | 最小 owner path 已存在 | 需要 source read 进一步确认 endpoint/panel/test gaps | Covered by selected slice |

## 5. Selected Next Slice

Selected next slice: `RiskActionGuard read-only status`.

Candidate owner path for source read:

```text
Completed Watchlist / MarketQuote / Evidence-Score / DecisionResult / ExecutionPlan-BoundaryCandidate / Review-Replay / Missed Archive slices
→ RiskActionGuardDisplayAdapter / DefaultRiskActionGuardDisplayAdapter
→ DashboardDetailResponseVO.RiskActionGuardDisplayVO
→ DashboardController / dashboard detail path
→ dashboard RiskActionGuard placeholder / status copy
→ future minimal review-only RiskActionGuard status
```

This source-read must confirm:

- 是否已有 dedicated RiskActionGuard status endpoint；
- 是否已有 dashboard safe DOM slot；
- 是否可直接复用 `RiskActionGuardDisplayVO` / adapter / dashboard detail path；
- 是否已有 fail-closed / blocked / action-disabled / not executable copy；
- 是否不需要 DTO / Validator / Assembler；
- 是否不会触发 Candidate / Decision generation / Point / Push / Trading；
- 是否能形成最小用户可见 runtime 小闭环。

## 6. Why This Slice Now

RiskActionGuard 是当前最小安全推进点，因为它已经处在 dashboard detail 和 rule/plan safety path 上，但尚未作为独立 review-only runtime status 小闭环被 source-read/design/implementation/verification/visual closure 过。

它比 Position Monitor 更小，因为不涉及仓位动作建议。它比 Internal Push 更安全，因为不涉及外部发送或 recheck trigger。它比 Candidate preview 更安全，因为不进入候选池或 ranking。它比 Three AI 更小，因为不接 provider orchestration。它比 SourceTrace aggregate 更保守，因为不跨多个 source-binding skeleton 家族。

## 7. Why Not The Others

- Push external channel / Internal Push preview: 暂不选，因为 Push/recheck 容易被误解为发送或外部通道能力。
- Candidate preview / ranking: 暂不选，因为 Candidate/ranking 是明确禁止的生成语义附近区域。
- Point generation: 暂不选，因为 entry / stop / TP / RR、final direction、Point generation 仍冻结。
- Position Monitor expansion: 暂不选，因为 close/reverse/open/action suggestion 边界大于本轮最小 slice。
- Three AI / AI conflict: 暂不选，因为 real provider orchestration / final arbiter / Decision generation 风险高。
- SourceTrace / RuntimeKline / DataQuality / MultiTimeframe aggregate: 暂不选，因为聚合面太宽，容易引入新 DTO / Validator / Assembler / schema ownership。
- Account risk / macro-news: 暂不选，因为 account risk 靠近 push snapshot/recheck，macro-news 靠近 external API refresh。

## 8. Next Step Definition

Next allowed action: `Source Read for RiskActionGuard read-only status`.

Next branch: `riskactionguard-read-only-status-source-read`.

Risk level: A.

Allowed changes:

- source-read docs；
- source-of-truth docs。

Forbidden scope:

- Java business code；
- tests；
- dashboard business logic；
- schema/config/pom；
- endpoint/panel implementation；
- external API refresh；
- scheduler / collector / API client trigger；
- Push / external channel；
- Candidate generation；
- Decision generation；
- Point generation；
- final direction；
- entry / stop / TP / RR；
- order / execution / auto-trading；
- DTO / Validator / Assembler / Orchestrator；
- P359 / P360。

## 9. Capability Movement

- Current level: `REVIEW_ONLY_RUNTIME partial`
- This package movement: none, selection only
- Selected slice target: source-read only
- Not Production Wiring
- Not Push
- Not Candidate generation
- Not Decision generation
- Not Point generation
- Not Trading

## 10. Final Recommendation

可以进入 `Source Read for RiskActionGuard read-only status`。下一步只读确认 RiskActionGuard owner path、dashboard/API surface、fail-closed semantics、tests、缺口和禁止边界；不得实现、不得生成候选、不得生成点位、不得接 Push、不得接订单/执行/自动交易。
