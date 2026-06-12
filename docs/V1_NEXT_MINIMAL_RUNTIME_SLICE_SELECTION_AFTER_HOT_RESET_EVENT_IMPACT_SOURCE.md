# V1 Next Minimal Runtime Slice Selection After Hot Reset / Event Impact Source

## Scope

This A-risk package selects the 19th minimal, low-conflict, verifiable
`REVIEW_ONLY_RUNTIME partial` slice after Hot Reset / Event Impact Source
visual closure.

Allowed changes:

- selection documentation
- source-of-truth documentation

Forbidden changes:

- Java business code, tests, dashboard business logic, schema/config/pom
- new DTO / Validator / Assembler / Orchestrator
- new service/domain/mapper/repository ownership family
- Push send, external channel, replay/recheck execution
- Candidate generation, Decision generation, Point generation
- final direction, entry, stop, TP, RR
- order, execution, auto-trading, Position Monitor execution
- missed-opportunity generation/write, review result generation
- paper order, simulated execution, paper PnL
- executable readiness, trading authorization
- position sizing, reduce/close/stop/reverse guidance
- recovery, repair, restart, auto-fix
- external API refresh, scheduler trigger, collector trigger, API client refresh
- Hot Reset execution/write, event generation, news fetch
- capability-level promotion

## Effective Baseline

- User-provided current main HEAD: `9e9fd2f docs(runtime): close hot reset event source visual verification`
- Source-of-truth baseline lag is not blocking. This package uses actual merged
  main as the effective execution baseline.
- Completed `REVIEW_ONLY_RUNTIME partial` slices before this selection: 18.
- Capability level remains `REVIEW_ONLY_RUNTIME partial`.

## Source-Read-Lite Evidence

This selection pass read owner-path evidence only. It did not implement or
change runtime behavior.

| Evidence area | Existing owner path / assets found | Selection impact |
|---|---|---|
| AI conflict / AI role convergence | `AiConflictResolverService`, `AiConflictResult`, `DecisionContext.aiConflictScore`, `DecisionResult.ai_role_results`, `DecisionResult.ai_conflict_level`, `DecisionResult.ai_conflict_score`, `DecisionResultVO`, `DecisionBundleVO`, `ReviewAggregateVO`, dashboard AI shell, review-page AI conflict fields, and targeted `DashboardControllerTest` assertions exist. | Strongest next source-read target if narrowed to conflict / convergence / missing status only. |
| Internal Push preview / notification preview | `PushRecheckScheduler`, `PushSnapshot` assets, push/recheck status contracts, and historical review-only preview skeletons exist. | Defer because send, external channel, recheck execution, scheduler, mutation, and duplicate skeleton revival risks are high. |
| Recheck status / recheck preview | PushRecheck service/log/scheduler/replay assets exist. | Defer because scheduler, replay, mutation, and execution paths are too close for the next minimal source read. |
| Candidate preview / ranking | Candidate attention / preview guard / ranking-era skeletons and tests exist. | Defer because Candidate generation/ranking/score and duplicate skeleton expansion remain frozen boundaries. |
| Position Monitor manual-input / monitor status | PositionSync and position-monitor foundations exist. | Defer because real-position monitoring, stop/close/reverse/open guidance, and Position Monitor execution are action-adjacent. |
| Macro-news / event calendar | Macro/news/event references exist through docs, score/evidence context, and the just-closed local Hot Reset / Event Impact slice. | Defer because broad macro/news scope can require external API refresh, news fetch, scheduler/collector behavior, or event generation. |
| Account risk downstream continuation | Account risk / exposure status is already closed as the 17th slice. | Defer to avoid drifting into trading authorization, position sizing, or account action guidance. |
| Hot Reset / Event Impact downstream continuation | Hot Reset / Event Impact Source status is already closed as the 18th slice. | Defer to avoid extending into Hot Reset write/execution, event generation, external refresh, or news fetch. |
| Existing dashboard/system placeholder | Dashboard AI role convergence shell exists and is not yet a dedicated status slice. | Covered by the selected AI conflict / AI role convergence source-read target. |

## Candidate Comparison

| Candidate | Existing owner/read path | Main risk | Decision |
|---|---|---|---|
| Three AI / AI conflict status | `AiConflictResolverService`, `AiConflictResult`, DecisionResult `ai_role_results` / `ai_conflict_*`, dashboard AI shell, review page AI conflict display. | Real Three AI provider orchestration, final arbiter language, final direction, and Decision generation drift. | Select only the narrowed `AI conflict / AI role convergence read-only status`; reject Three AI provider expansion. |
| Internal Push preview / notification preview status | PushSnapshot / PushRecheck / internal preview assets exist. | Push send, external channel, notification dispatch, scheduler/recheck execution, and duplicate skeleton revival. | Reject. |
| Recheck status / recheck preview | PushRecheck read/log/replay assets exist. | Recheck execution, replay execution, scheduler, and mutation adjacency. | Reject. |
| Candidate preview / ranking status | Candidate attention / preview guard skeleton history exists. | Candidate generation, ranking, score, Point, and duplicate skeleton expansion. | Reject. |
| Position Monitor manual-input / monitor status | PositionSync / monitor / real position foundations exist. | Real position monitoring, close/reverse/open/move-stop guidance, stop-loss action, and Position Monitor execution. | Reject. |
| Macro-news / event calendar status | Event/macro/news context exists, and local Hot Reset / Event Impact is now closed. | Broad scope can imply external API refresh, news fetch, event generation, scheduler/collector trigger. | Reject. |
| Account risk downstream review-only display continuation | Account risk / exposure endpoint/panel is already closed. | Continuation can imply trading authorization, position sizing, reduce/close/stop/reverse guidance. | Reject. |
| Hot Reset / Event Impact downstream review-only display continuation | Hot Reset / Event Impact Source endpoint/panel is already closed. | Continuation can imply Hot Reset execution/write, event generation, external refresh/news fetch. | Reject. |
| Existing dashboard/system placeholder with review-only owner path | Dashboard AI role convergence shell already exists. | Must not become Three AI provider arbitration or final decision. | Select through the narrowed AI conflict / AI role convergence source read. |
| Other source-discovered slice | No smaller lower-risk unfinished owner path was found. | N/A | Not selected. |

## Selected Next Slice

Selected next slice:

`AI conflict / AI role convergence read-only status`

Chinese label:

`AI 冲突 / AI 角色收敛只读状态`

This is a narrowed interpretation of the Three AI / AI conflict candidate. It
does not authorize GPT/Gemini/Grok provider orchestration, new AI calls, budget
logic, cache/fallback logic, final arbitration, final direction, entry/stop/TP/RR,
Decision generation, Candidate generation, Point generation, Push, external
channel, order/execution, or trading.

## Reusable Evidence To Source Read Next

The next source-read package should inventory, at minimum:

- `AiConflictResolverService`
- `AiConflictResolverServiceImpl`
- `AiConflictResult`
- `AiConflictLevelEnum`
- `DecisionContext`
- `DecisionEngineService` AI conflict write path as boundary evidence only
- `DecisionResult`
- `DecisionResultMapper`
- `DecisionResultVO`
- `DecisionBundleVO`
- `ReviewAggregateVO` AI conflict fields
- `DashboardController` existing DecisionResult status / dashboard detail owner path
- `dashboard.html` AI role convergence / AI conflict shell and DOM/copy
- `review-page.js` AI conflict display context
- `DashboardControllerTest`
- `DecisionEngineServiceTest`

## Selection Reason

AI conflict / AI role convergence read-only status is the smallest safe next
candidate because:

- existing DecisionResult and dashboard/review display assets already expose
  AI role / AI conflict read-model context;
- the next package can be source-read only and decide whether a dedicated status
  slice would duplicate existing DecisionResult status or fill a genuine gap;
- the scope can be limited to conflict/convergence/missing/fail-closed status;
- it can explicitly keep Three AI provider orchestration, final arbiter behavior,
  final direction, entry/stop/TP/RR, Candidate generation, Decision generation,
  Point generation, Push, external channel, order/execution, and trading outside
  the boundary;
- it avoids returning to downstream continuation of recently closed account-risk
  and Hot Reset slices.

## Risk Notes

- The selected slice is not `Three AI expansion`. It is only a source-read
  target for existing AI conflict / role convergence status evidence.
- `AiConflictResult.finalMarketBias` and `DecisionEngineService` are generation
  adjacent. The next source read must classify these as boundary evidence and
  must not authorize final direction or new Decision generation.
- Existing DecisionResult status already handles `ai_role_results` availability.
  The next source read must explicitly answer whether a new slice would reduce
  ambiguity or would duplicate completed DecisionResult status.
- Any need for a new DTO / Validator / Assembler / Orchestrator, provider
  orchestration family, schema/config/pom, external AI call, Push/Recheck,
  Candidate, Point, final direction, entry/stop/TP/RR, order/execution, or
  trading is an immediate NO-GO for later design/implementation.

## Next Allowed Action

`Source Read for AI conflict / AI role convergence read-only status`

Next branch:

`ai-conflict-ai-role-convergence-status-source-read`

## Overreach Check

- No Java business code changed.
- No tests changed.
- No dashboard business logic changed.
- No schema/config/pom changed.
- No DTO / Validator / Assembler / Orchestrator added.
- No service/domain/mapper/repository ownership family added.
- No Push send / external channel connected.
- No replay / recheck execution connected.
- No Candidate / Decision generation / Point / final direction / entry / stop / TP / RR generated.
- No order / execution / auto-trading connected.
- No Position Monitor execution connected.
- No external API refresh / scheduler / collector / API client refresh triggered.
- No Hot Reset execution/write, event generation, or news fetch connected.
- No capability-level promotion.

## #830 Audit

- New skeleton created: no.
- Cursor-era / existing assets reused: yes, selection points to existing AI
  conflict, DecisionResult, dashboard, and review-page assets for source read.
- Duplicate reduction: yes, the next source read must decide whether this is a
  real status gap or duplicate DecisionResult status before any wiring.
- Capability level raised: no.
- Service/runtime/dashboard/API connected: no, selection only.
- #830 compliance: yes, this selects an existing-owner source-read direction and
  keeps P359/P360, provider orchestration, and new wrapper families frozen.
