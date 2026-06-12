# V1 Next Minimal Runtime Slice Selection After Runtime Readiness / System Guardrail Closure

## 1. Current Merged Main

- Current merged main: `8069a48 docs(runtime): record runtime readiness visual closure`
- Effective execution baseline: `8069a48 docs(runtime): record runtime readiness visual closure`
- Current capability level: `REVIEW_ONLY_RUNTIME partial`
- Completed review-only runtime partial slices: `16`
- Current task: select the 17th minimal review-only runtime slice.
- Capability movement: none. This package is selection-only and does not raise capability beyond `REVIEW_ONLY_RUNTIME partial`.

## 2. Completed Slices

The completed `REVIEW_ONLY_RUNTIME partial` slices are:

1. PositionSync + Dashboard review-only status
2. Watchlist + RuleConfig + Dashboard/API review-only status
3. MarketQuote freshness / fallback / dashboard API status
4. Evidence / Score review-only runtime status
5. DecisionResult review-only dashboard/API status
6. ExecutionPlan / BoundaryCandidate review-only runtime status
7. Review / Replay result status
8. Data Source Health dashboard/API status
9. RuleConfig runtime audit / rule explainability
10. Missed Opportunity / Review Archive status
11. RiskActionGuard read-only status
12. Alert fatigue / notification policy status
13. SourceTrace / RuntimeKline / DataQuality / MultiTimeframe aggregate review-only status
14. Paper Observation / Paper Trading Status review-only status
15. Review Archive Analytics / Missed Opportunity Aggregate Status
16. Runtime readiness / system guardrail status

## 3. Source-Read-Lite Evidence

This selection pass read owner-path evidence only. It did not implement or change runtime behavior.

| Evidence area | Existing owner path / assets found | Selection impact |
|---|---|---|
| Account risk / account exposure | `TmAccountRiskSnapshotDO`, `AccountRiskSnapshotMapper`, `tm_account_risk_snapshot`, `PushRecheckServiceImpl` read/block references, `ReviewAggregateServiceImpl` read summaries, and account-risk JSON tests exist. | Strongest next source-read target if strictly scoped to read-only exposure/status visibility, not authorization, sizing, Push/Recheck execution, or order behavior. |
| Macro-news / event calendar | Macro/news/event references exist mostly through docs and decision/evidence metadata. | Defer because cohesive runtime owner path is weak and future status could require external API refresh, news collection, or event generation. |
| Three AI / AI conflict | `AiConflictResolverService`, AI conflict result types, DecisionResult AI conflict fields, and dashboard AI conflict rendering exist. | Defer because it is close to provider arbitration, final direction, and decision generation semantics. |
| Internal Push preview / notification preview | Dashboard internal Push preview placeholders and legacy preview skeletons exist. | Defer because Push send, external channel, notification dispatch, and duplicate skeleton revival risks remain high. |
| Recheck status / recheck preview | `PushRecheckService`, scheduler/replay/log assets, run-baseline recheck summaries, and tests exist. | Defer because the owner path is execution/replay/scheduler adjacent. |
| Candidate preview / ranking | Candidate preview/attention/ranking-era DTO and assembler skeletons exist. | Defer because Candidate generation/ranking and duplicate skeleton expansion remain frozen. |
| Position Monitor manual-input / monitor status | PositionSync, `RealPositionMapper`, monitor foundations, and dashboard status assets exist. | Defer because real-position monitoring, close/reverse/open/move-stop wording, and Position Monitor execution risk remain high. |
| Existing dashboard/system placeholder | Runtime readiness / system guardrail just closed this placeholder class as the 16th slice. | No smaller unclosed system placeholder was found in this pass. |

## 4. Candidate Next Slices Considered

| Candidate | Existing owner path / assets | Minimal closure fit | Boundary risk | Decision |
|---|---|---|---|---|
| Account risk / account exposure status | `TmAccountRiskSnapshotDO`, `AccountRiskSnapshotMapper`, `tm_account_risk_snapshot`, Push/Recheck read references, ReviewAggregate account-risk summaries, account-risk JSON tests. | High for source read: concrete read model exists and can be inventoried without implementation. | Medium-high unless the next package explicitly blocks account write, Push/Recheck execution, position sizing, trading authorization, reduce/close/stop/reverse, and order behavior. | Select. |
| Macro-news / event calendar status | Mostly docs and decision/evidence context; no clearly cohesive runtime status owner found. | Low-medium. | External API refresh, news collection, event generation, and decision influence. | Defer. |
| Three AI / AI conflict status | AI conflict resolver/result fields and dashboard display history exist. | Medium. | Provider orchestration, arbitration, final direction, and decision generation drift. | Defer. |
| Internal Push preview / notification preview status | Dashboard preview placeholders and legacy review-only preview assets exist. | Medium. | Push send, external channel, notification dispatch, and frozen skeleton revival. | Defer. |
| Recheck status / recheck preview | PushRecheck service/log/scheduler/replay assets exist. | Low for the next slice. | Recheck execution, replay, scheduler, mutation, and Push adjacency. | Defer. |
| Candidate preview / ranking status | Candidate attention / preview guard / ranking-era history exists. | Low. | Candidate generation, ranking, score-to-candidate revival, duplicate skeleton expansion. | Defer. |
| Position Monitor manual-input / monitor status | PositionSync, `RealPositionMapper`, real-position tables, and monitor foundations exist. | Medium, but still action-adjacent. | Real position monitoring, close/reverse/open/move-stop guidance, stop-loss action, and Position Monitor execution. | Defer. |
| Existing dashboard/system placeholder with review-only owner path | No smaller unclosed placeholder was found after runtime readiness closure. | N/A | Runtime readiness already consumed the safest system placeholder. | Not selected. |
| Other smaller safer source-discovered slice | No smaller lower-risk unclosed slice was found. | N/A | N/A | Not selected. |

## 5. Selected Next Slice

Selected next slice:

`Account risk / account exposure status`

Chinese label:

`账户风险 / 账户暴露状态`

Next branch:

`account-risk-account-exposure-status-source-read`

Next allowed action:

`Source Read for Account risk / account exposure status`

## 6. Why This Slice Now

Account risk / account exposure is now the best next source-read-only target because it has concrete owner-path evidence while still being small enough to audit before any implementation:

- `tm_account_risk_snapshot` exists as a persisted account-risk snapshot table.
- `TmAccountRiskSnapshotDO` and `AccountRiskSnapshotMapper` provide an existing read model.
- `PushRecheckServiceImpl` reads account-risk snapshot state when classifying post-price checks.
- `ReviewAggregateServiceImpl` reads account-risk snapshot state for review/push aggregate summaries.
- Existing targeted tests already exercise account-risk JSON and recheck blocked behavior.

This is still only a source-read selection. The next package must prove that account risk can be displayed as read-only exposure/status visibility and cannot become trading authorization, position sizing, reduce/close/stop/reverse guidance, Push/Recheck execution, order behavior, or auto-trading.

## 7. Why Not The Others

- Macro-news / event calendar status is deferred because the source evidence is weaker and likely needs external API refresh, news collection, or event generation boundaries.
- Three AI / AI conflict status is deferred because it can drift into provider orchestration, arbitration, final direction, and decision generation.
- Internal Push preview / notification preview status is deferred because Push send, external channel, notification dispatch, and duplicate skeleton revival remain sensitive.
- Recheck status / recheck preview is deferred because existing assets include scheduler, replay, mutation, and execution paths.
- Candidate preview / ranking status is deferred because Candidate generation/ranking and duplicate skeleton revival remain frozen.
- Position Monitor manual-input / monitor status is deferred because it can be read as real position monitoring, close/reverse/open/move-stop guidance, stop-loss action, or Position Monitor execution.
- Existing dashboard/system placeholders are not selected because Runtime readiness / system guardrail just closed the safest system placeholder path.

## 8. Source Read Task Definition

Next task:

`Source Read for Account risk / account exposure status`

Branch:

`account-risk-account-exposure-status-source-read`

Risk:

`A`

Required owner-path reads:

- `TmAccountRiskSnapshotDO`
- `AccountRiskSnapshotMapper`
- `tm_account_risk_snapshot`
- `PushSnapshotService` / `PushSnapshotServiceImpl` context only, especially account-risk snapshot write-side boundaries
- `PushRecheckServiceImpl` account-risk read/classification path
- `ReviewAggregateServiceImpl` account-risk summary path
- `AnalysisAssemblerServiceImpl` account-risk JSON context, strictly as risk evidence and write-side boundary evidence
- dashboard/review surfaces that already display Push/Recheck/account-risk summaries, if any
- existing account-risk tests such as `PushRecheckServiceImplTest`, `AnalysisAssemblerServiceImplAccountRiskJsonTest`, and ReviewAggregate tests

Required source-read questions:

1. Which `tm_account_risk_snapshot` fields can be read-only status source: `riskAllowed`, `riskReasonCode`, `riskReasonText`, `positionExposure`, `maxAllowedExposure`, `snapshotSource`, `snapshotVersion`, and snapshot timestamps?
2. Is `AccountRiskSnapshotMapper` sufficient as an existing owner path without new mapper/schema/config/pom?
3. Which uses are write-side only, especially `PushSnapshotService.ensureAccountRiskSnapshot`, and must remain excluded from status runtime wiring?
4. Which Push/Recheck paths only read account-risk state, and which paths would trigger recheck execution or mutation?
5. Can the future status fail closed when snapshot is missing, stale, malformed, or says `riskAllowed=false`?
6. How should account exposure be shown without implying executable readiness, trading authorization, position sizing, reduce/close/stop/reverse advice, order action, or auto-trading?
7. Does any path generate Candidate, Decision generation, Point, final direction, entry/stop/TP/RR, Push send, external channel, order/execution, or auto-trading?
8. Would future design require DTO / Validator / Assembler / Orchestrator, new service/domain/mapper/repository owner, schema/config/pom, or dashboard business logic? Default answer must be NO unless source read proves existing assets are enough.

## 9. Forbidden Scope

This selection package and the next source-read package must not:

- modify Java business code;
- modify tests;
- modify dashboard business logic;
- modify schema/config/pom;
- add endpoint or panel behavior;
- add DTO / Validator / Assembler / Orchestrator;
- add service/domain/mapper/repository ownership families;
- write account-risk snapshots;
- call PushSnapshot write-side behavior;
- trigger Push send or external channel;
- trigger replay or recheck execution;
- generate Candidate, Decision, Point, final direction, entry, stop, TP, RR, order action, execution action, or auto-trading action;
- generate executable readiness or trading authorization;
- trigger external API refresh, scheduler, collector, or API-client refresh;
- execute Position Monitor behavior;
- generate missed opportunity or review results;
- execute paper order, simulated execution, or paper PnL;
- continue P359 or P360.

## 10. Risk And Capability

- Risk level: A
- Allowed next action: source-read docs and source-of-truth updates only
- Capability movement: none
- Current capability level remains: `REVIEW_ONLY_RUNTIME partial`

Freeze-rule compliance:

- 是否创建新骨架: No
- 是否复用 Cursor-era / V1 资产: Yes
- 是否减少重复: Yes, by selecting the existing account-risk snapshot/read owner path before any new wrapper family.
- 是否提升 capability level: No, selection only.
- 是否接 service/runtime/dashboard/API: No, selection only; the next source read will inventory existing service/runtime/dashboard/API assets.
- 是否符合 #830 审计建议: Yes
