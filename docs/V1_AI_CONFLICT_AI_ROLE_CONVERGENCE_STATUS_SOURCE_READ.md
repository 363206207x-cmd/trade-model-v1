# V1 AI Conflict / AI Role Convergence Status Source Read

## Scope

This source read confirms whether `AI conflict / AI role convergence read-only status` can become the 19th minimal `REVIEW_ONLY_RUNTIME partial` slice.

This package is source-read only. It does not implement an endpoint, dashboard panel, AI call, provider orchestration, final arbiter, Decision generation, Candidate generation, Point generation, Push, Recheck, Replay, order/execution, auto-trading, Position Monitor execution, schema/config/pom changes, or new DTO / Validator / Assembler / Orchestrator / ownership-family code.

Effective execution baseline:

- Previous merged package: PR #985.
- Effective local main before this branch: `a29574f docs(runtime): select ai conflict next slice`.
- Source of Truth lag is not blocking; this package records the source read and hands off to design.

## Source Read Files

| Area | Files read | Evidence |
|---|---|---|
| AI conflict resolver contract | `src/main/java/org/example/trademodel/service/AiConflictResolverService.java` | Defines `resolve(DecisionContext context)`. This is a generation-time service contract, not a read-only status owner. |
| AI conflict result carrier | `src/main/java/org/example/trademodel/service/AiConflictResult.java` | Carries `level`, `finalMarketBias`, `adjustedConfidence`, `planMode`, and `aiConflictScore`. `finalMarketBias` and `adjustedConfidence` are generation-adjacent and must not become status outputs. |
| AI conflict resolver implementation | `src/main/java/org/example/trademodel/service/impl/AiConflictResolverServiceImpl.java` | Computes conflict score and returns plan/final-bias guidance from `DecisionContext`. It sets `context.setAiConflictScore(...)` and is not safe for runtime status reads. |
| AI conflict levels | `src/main/java/org/example/trademodel/enums/AiConflictLevelEnum.java` | Existing levels: `LEVEL_1_CONSISTENT`, `LEVEL_2_LIGHT_DIVERGENCE`, `LEVEL_3_SIGNIFICANT_DIVERGENCE`, `LEVEL_4_EXTREME_DIVERGENCE`. These can support read-only status names if read only from persisted DecisionResult fields. |
| Decision generation boundary | `src/main/java/org/example/trademodel/service/DecisionEngineService.java` | Calls `aiConflictResolverService.resolve(ctx)`, creates `aiRoleResults`, assigns AI conflict fields, and logs AI decision completion. This is a forbidden generation boundary for this slice. |
| Persisted read model | `src/main/java/org/example/trademodel/entity/DecisionResult.java` | Existing persisted fields include `aiRoleResults`, `aiConflictLevel`, `aiConflictScore`, and `aiPlanMode`. This is the safest read-model owner. |
| Mapper read owner | `src/main/java/org/example/trademodel/mapper/DecisionResultMapper.java` | Existing joined read queries select `ai_role_results`, `ai_conflict_level`, `ai_conflict_score`, and `ai_plan_mode`. Insert/write path is generation history only and must not be used. |
| Decision VO | `src/main/java/org/example/trademodel/vo/DecisionResultVO.java` | Exposes `aiRoleResults`, `aiConflictLevel`, `aiConflictScore`, `aiPlanMode`, and `confusedScore`. The same VO also carries executable-adjacent plan fields that must be excluded from any future AI status projection. |
| Decision bundle | `src/main/java/org/example/trademodel/vo/DecisionBundleVO.java` | Contains AI conflict fields plus Push/Recheck-adjacent fields. It is a generation output carrier, not a status owner. |
| Decision read fallback | `src/main/java/org/example/trademodel/service/impl/DecisionServiceImpl.java` | Existing fallback marks missing `ai_conflict_level`, `ai_conflict_score`, `confused_score`, and other read-model fields as partial. It does not currently mark missing `ai_role_results` in the same fallback list. |
| Dashboard status endpoint | `src/main/java/org/example/trademodel/controller/DashboardController.java` | Existing `/api/dashboard/decision-result-status` reads latest `DecisionResultVO`, reports `aiRoleResultsAvailable` / `aiRoleResultsSummary`, and exposes review-only safety flags. It does not call the resolver or external AI. |
| Dashboard AI display | `src/main/resources/templates/dashboard.html` | Existing `decisionResultStatusPanel`, `decisionAiRoleValue`, `resolveAiConflict(...)`, and AI shell display `aiConflictLevel`, `aiConflictScore`, `aiPlanMode`, and raw role output context with manual-review copy. |
| Review page display | `src/main/resources/static/js/review-page.js` | Existing review-page decision display includes AI conflict level/score, plan mode, and `confusedScore` as review context only. |
| Review aggregate display context | `src/main/java/org/example/trademodel/vo/ReviewAggregateVO.java`, `src/main/java/org/example/trademodel/service/impl/ReviewAggregateServiceImpl.java` | Review decision summaries carry AI conflict level/score/plan mode/confused score, but not raw `aiRoleResults`. This is display context, not execution logic. |
| Existing tests | `src/test/java/org/example/trademodel/controller/DashboardControllerTest.java`, `src/test/java/org/example/trademodel/service/DecisionEngineServiceTest.java` | Dashboard tests cover DecisionResult status endpoint safety, AI role missing fail-closed/partial behavior, and forbidden executable fields absent. DecisionEngine tests are generation-boundary tests and must not become status implementation evidence. |
| Prior DecisionResult docs | `docs/V1_DECISIONRESULT_REVIEW_ONLY_RUNTIME_SOURCE_READ.md`, `docs/V1_MINIMAL_REVIEW_ONLY_DECISIONRESULT_RUNTIME_WIRING_DESIGN.md` | Existing DecisionResult slice already called out `ai_role_results` and AI conflict fields as read-model assets, so this slice must avoid duplicating DecisionResult status behavior. |

## Existing Owner Path

The canonical read-only owner path is:

`DecisionResult` persisted fields -> `DecisionResultMapper` joined latest-result reads -> `DecisionService.getLatestDecisionResultBySymbol(...)` / dashboard detail reads -> `DecisionResultVO` -> existing dashboard/review display surfaces.

Safe read fields:

- `aiRoleResults`
- `aiConflictLevel`
- `aiConflictScore`
- `aiPlanMode`
- `confusedScore`
- `readModelTruthStatus`
- `readModelFallbackReason`

Unsafe or executable-adjacent fields that must not be exposed as AI role convergence status outputs:

- `recommendedAction`
- `planMode` when interpreted as instruction rather than persisted status
- `entryZone`
- `stopLoss`
- `takeProfitRules`
- `leverageSuggestion`
- `positionSuggestion`
- `finalMarketBias`
- `adjustedConfidence`
- Push/Recheck trigger fields

## Existing API / Dashboard Evidence

Existing API / controller path:

- `GET /api/dashboard/decision-result-status?symbol=...`

Existing dashboard status and AI shell assets:

- `decisionResultStatusPanel`
- `decisionAiRoleValue`
- `resolveAiConflict(...)`
- AI shell title `AI 三方裁决`
- AI shell copy: AI convergence cannot bypass SourceTrace, RiskActionGuard, or manual review.
- AI role missing copy: missing AI output does not infer opportunities, prices, scores, or trading actions.

Existing review-page display context:

- AI conflict level / score display.
- AI plan mode display.
- `confusedScore` display.
- Raw review context only; no execution entry.

No dedicated AI conflict / AI role convergence status endpoint or panel was found.

## Existing Review-Only / Fail-Closed Semantics

Already present through DecisionResult status wiring:

- `reviewOnly=true`
- `notTradingSignal=true`
- `notCandidateSignal=true`
- `notDecisionGeneration=true`
- `notPointSignal=true`
- `displaySlotsAreCandidatePool=false`
- `failClosed=true`
- Missing DecisionResult -> `DECISIONRESULT_MISSING_FAIL_CLOSED`
- Stale or unknown read model -> `DECISIONRESULT_STALE_OR_UNKNOWN_FAIL_CLOSED`
- Partial read model -> `DECISIONRESULT_READ_MODEL_PARTIAL`
- Partial source trace -> `DECISIONRESULT_SOURCE_TRACE_PARTIAL`
- Missing AI role output -> `DECISIONRESULT_AI_ROLE_PARTIAL`

Partially present through dashboard copy:

- Manual review requirement for AI shell output.
- AI output cannot bypass SourceTrace or RiskActionGuard.

Not yet dedicated to this slice:

- `manualReviewOnly=true`
- `notExternalAiCall=true`
- `notThreeAiProviderOrchestration=true`
- `notFinalArbiter=true`
- A normalized role convergence / role agreement / role divergence status field.

## Duplication Check

This module is not a fully independent capability gap. The completed DecisionResult review-only dashboard/API status already covers:

- Existing DecisionResult owner path.
- `ai_role_results` availability.
- Missing AI role fail-closed / partial status.
- Review-only / not decision generation / not candidate / not point / not trading safety flags.
- Dashboard status panel and tests.

The remaining possible gap is narrower:

- A read-only projection of persisted `aiConflictLevel`, `aiConflictScore`, `aiPlanMode`, and `aiRoleResults` into an explicit AI conflict / role convergence status.
- Clear safety fields for no external AI calls, no Three AI provider orchestration, no final arbiter output, and no Decision generation.
- Clear dashboard copy that AI role convergence is review context only and cannot produce final direction, entry/stop/TP/RR, Candidate, Point, Push, Recheck, Replay, order/execution, or trading.

Therefore the next design must first decide whether a dedicated AI conflict status slice is justified, or whether it should be a NO-GO as duplicate DecisionResult coverage.

No direct duplication was found with Evidence / Score or ExecutionPlan / BoundaryCandidate as long as the future work remains a persisted DecisionResult status projection and does not expose scores, plans, entry/stop/TP/RR, or candidate/point semantics.

## Reusable Assets

- `DecisionResult.aiRoleResults`
- `DecisionResult.aiConflictLevel`
- `DecisionResult.aiConflictScore`
- `DecisionResult.aiPlanMode`
- `DecisionResult.confusedScore`
- `DecisionResultMapper` latest joined reads.
- `DecisionResultVO` read model fields.
- `DashboardController.decisionResultStatus(...)` and its existing read-only safety map.
- `dashboard.html` DecisionResult status panel and AI shell display.
- `review-page.js` AI conflict display context.
- `ReviewAggregateVO.ReviewDecisionSummary` AI conflict fields.
- `DashboardControllerTest` DecisionResult status coverage.

## Gaps

- No dedicated AI conflict / role convergence status endpoint was found.
- No dedicated AI conflict / role convergence dashboard panel was found.
- Existing DecisionResult status endpoint checks AI role availability, but does not expose a normalized AI conflict / convergence status map.
- Existing DecisionResult read-model fallback checks AI conflict fields, but does not list `ai_role_results` in the same fallback annotation path.
- No dedicated `roleConvergenceStatus`, `roleAgreement`, or `roleDivergence` read-model field was found.
- Existing dashboard AI shell can display raw AI role output context; any future status must avoid turning raw text into final direction or trading instruction.
- Existing `AiConflictResult.finalMarketBias` and `adjustedConfidence` are generation-adjacent and must not be exposed by a review-only status projection.
- Existing DecisionEngine and AiConflictResolver paths remain generation-only and must not be called by future runtime status.

## Design Risk Notes

- External AI call risk: present only if future implementation calls provider/DecisionEngine/orchestration code. A read-only persisted DecisionResult projection can avoid it.
- Three AI provider orchestration risk: high if design tries to recompute roles; blocked for this slice.
- Final arbiter risk: high because source code contains generated final-bias language. Future status must use only persisted conflict status and negative safety copy.
- Decision generation risk: high if `DecisionEngineService` or `AiConflictResolverServiceImpl` is called. Future design must forbid those paths.
- Final direction / entry / stop / TP / RR risk: present in adjacent DecisionResultVO fields; future status must explicitly omit them.
- Candidate / Point risk: no need for candidate or point owners; future status must keep `displaySlotsAreCandidatePool=false`.
- Push / Recheck / Replay risk: adjacent DecisionBundle fields exist, but no future AI conflict status should read or trigger them.
- Duplicate-skeleton risk: high if new DTO / Validator / Assembler / Orchestrator / service family is proposed. Future design should reuse existing DecisionResult read paths and at most a minimal read-only Map endpoint if the readiness gate later approves.

## Source Read Decision

Result: GO to design only.

The source read supports a narrow design step for `AI conflict / AI role convergence read-only status`, but only as a persisted DecisionResult read-model projection. The design must prefer reusing existing `/api/dashboard/decision-result-status` owner path. A dedicated endpoint or panel is not automatically justified and must be explicitly weighed against duplication with the completed DecisionResult status slice.

NO-GO for any future design or implementation that requires:

- External AI call.
- Three AI provider orchestration.
- Final arbiter output generation.
- Calling `DecisionEngineService` or `AiConflictResolverServiceImpl` for status.
- New Decision generation.
- Final direction / entry / stop / TP / RR.
- Candidate generation.
- Point generation.
- Push send / external channel.
- Recheck / Replay execution.
- Order / execution / auto-trading.
- Position Monitor execution.
- New DTO / Validator / Assembler / Orchestrator.
- New service/domain/mapper/repository ownership family.
- Schema/config/pom changes.
- P359 / P360 continuation.

## Next Allowed Action

`Minimal Review-Only AI Conflict / AI Role Convergence Status Runtime Wiring Design`

Next branch:

`minimal-review-only-ai-conflict-ai-role-convergence-status-runtime-wiring-design`

## Overreach Status

No overreach in this package:

- No Java business code changed.
- No tests changed.
- No dashboard business logic changed.
- No schema/config/pom changed.
- No endpoint or panel behavior implemented.
- No external AI call, provider orchestration, final arbiter, Decision generation, Candidate generation, Point generation, Push, Recheck, Replay, order/execution, auto-trading, Position Monitor execution, or P359/P360 work performed.
