# V1 Minimal Review-Only AI Conflict / AI Role Convergence Status Runtime Wiring Design

## Scope

This design defines whether `AI conflict / AI role convergence read-only status` can continue after source read as a minimal `REVIEW_ONLY_RUNTIME partial` runtime slice.

This package is design only. It does not implement an endpoint, dashboard panel, AI provider call, Three AI orchestration, final arbiter, Decision generation, Candidate generation, Point generation, Push, Recheck, Replay, order/execution, auto-trading, Position Monitor execution, schema/config/pom changes, or new DTO / Validator / Assembler / Orchestrator / ownership-family code.

Effective execution baseline:

- Previous merged package: PR #987.
- Effective local main before this branch: `4317597 docs(runtime): read ai conflict status source`.
- Source of Truth baseline lag is not blocking; this package uses actual merged main as the effective execution baseline.

## Design Result

Result: GO to implementation readiness gate only, with duplication review required.

This module is not a fully independent runtime capability. The completed DecisionResult review-only dashboard/API status already covers the canonical DecisionResult owner path, AI role availability, missing AI role partial status, dashboard DecisionResult status shell, and core review-only / not decision generation / not Candidate / not Point / not trading safety flags.

The only justifiable continuation is a narrow read-only status projection over persisted DecisionResult AI fields:

- `DecisionResult.aiRoleResults`
- `DecisionResult.aiConflictLevel`
- `DecisionResult.aiConflictScore`
- `DecisionResult.aiPlanMode`
- `DecisionResult.confusedScore`
- `DecisionResult.readModelTruthStatus`
- `DecisionResult.readModelFallbackReason`

The readiness gate must return NO-GO if this projection would duplicate the existing `/api/dashboard/decision-result-status` behavior without adding a distinct safety/readability closure.

## Duplication Decision

Decision: duplicate risk is high but not automatically blocking.

Allowed design continuation:

- A minimal explicit projection may be useful only to make AI conflict / role convergence status and AI-generation boundary visible as review-only evidence.
- The projection must reuse existing DecisionResult reads and must not recompute AI conflict.
- The projection must not interpret role text as final direction, entry, stop, TP, RR, Candidate, Point, Push, Recheck, Replay, order, execution, or trading intent.
- The projection must include `DUPLICATE_DECISIONRESULT_STATUS_REVIEW_REQUIRED` when existing DecisionResult status already satisfies the need.

NO-GO duplication conditions:

- The implementation would simply mirror `aiRoleResultsAvailable` / `aiRoleResultsSummary` from `/api/dashboard/decision-result-status`.
- The dashboard would add a second panel with the same meaning as `decisionResultStatusPanel` without clearer safety copy.
- The design would require a new DTO / Validator / Assembler / Orchestrator or a new service/domain/mapper/repository owner.
- The design would call `AiConflictResolverService`, `AiConflictResolverServiceImpl`, `DecisionEngineService`, provider clients, or final arbiter logic.

## Owner Path

Canonical owner path:

`DecisionResult` persisted AI fields -> `DecisionResultMapper` latest joined reads -> existing DecisionResult read service / dashboard detail read -> `DecisionResultVO` -> existing dashboard AI shell / review-page display context.

Primary runtime owner:

- Existing `/api/dashboard/decision-result-status`.

Read-only source fields:

- `aiRoleResults`
- `aiConflictLevel`
- `aiConflictScore`
- `aiPlanMode`
- `confusedScore`
- `readModelTruthStatus`
- `readModelFallbackReason`

Display context only:

- dashboard `decisionResultStatusPanel`
- dashboard AI shell / `decisionAiRoleValue`
- review-page AI conflict display
- ReviewAggregate decision summary fields

Forbidden owner paths:

- `AiConflictResolverService.resolve(...)`
- `AiConflictResolverServiceImpl`
- `DecisionEngineService`
- external AI provider clients
- `DecisionBundleVO` Push/Recheck-adjacent output path
- any write path on `DecisionResultMapper`

## Endpoint Decision

Default decision: do not add a dedicated endpoint.

Preferred path:

- Reuse `GET /api/dashboard/decision-result-status?symbol=BTCUSDT`.
- Add no new endpoint if the existing DecisionResult status can carry or already explain the AI role/convergence evidence.

Conditional path for readiness gate:

- A dedicated endpoint may be allowed only if the readiness gate proves the existing DecisionResult status cannot clearly express AI conflict / role convergence safety.
- If allowed, it must be at most one minimal read-only `Map` endpoint in `DashboardController`.
- A possible path would be `GET /api/dashboard/ai-conflict-role-convergence-status?symbol=BTCUSDT`, but this path is not approved by this design alone.
- The endpoint must only read the existing DecisionResult owner path and return status/safety fields.

## Dashboard Decision

Default decision: reuse the existing dashboard AI shell and DecisionResult status panel.

Preferred display:

- Existing `decisionResultStatusPanel`.
- Existing `decisionAiRoleValue`.
- Existing AI shell copy that AI output cannot bypass SourceTrace, RiskActionGuard, or manual review.

Conditional display for readiness gate:

- A minimal dashboard AI conflict status panel may be allowed only if it avoids duplicate DecisionResult display and adds clear safety value.
- If allowed, it may only contain DOM ids, read-only status values, fail-closed / manual-review copy, and negative safety copy.
- It must not add buttons, provider-call controls, recheck/replay controls, Push controls, candidate/ranking UI, point UI, order/execution UI, or trading authorization UI.

## Status Mapping

| Status | Condition | Boundary |
|---|---|---|
| `AI_CONFLICT_STATUS_REVIEW_ONLY_READY` | Latest DecisionResult exists and persisted AI conflict level/score/plan mode are readable. | Read-only evidence only. |
| `AI_CONFLICT_STATUS_MISSING_FAIL_CLOSED` | DecisionResult exists but AI conflict fields are missing or unknown. | Fail closed; do not infer conflict. |
| `AI_CONFLICT_STATUS_PARTIAL_REVIEW_ONLY` | Some AI conflict fields are readable but incomplete. | Partial review-only; no generation. |
| `AI_ROLE_CONVERGENCE_REVIEW_ONLY_READY` | AI role results and conflict metadata are present enough to show convergence/divergence context. | Manual review only. |
| `AI_ROLE_CONVERGENCE_MISSING_FAIL_CLOSED` | AI role results are missing, blank, or not safely available. | Fail closed; no role convergence claim. |
| `AI_ROLE_RESULTS_READ_ONLY_EVIDENCE` | `aiRoleResults` is displayed as persisted text evidence. | Not final arbiter output. |
| `DUPLICATE_DECISIONRESULT_STATUS_REVIEW_REQUIRED` | Existing DecisionResult status already covers the projection need. | Readiness gate must decide GO/NO-GO. |
| `EXTERNAL_AI_CALL_BOUNDARY_BLOCKED_FAIL_CLOSED` | Any design would require a provider call. | NO-GO. |
| `THREE_AI_PROVIDER_ORCHESTRATION_BOUNDARY_BLOCKED_FAIL_CLOSED` | Any design would orchestrate Grok/Gemini/GPT or similar providers. | NO-GO. |
| `FINAL_ARBITER_BOUNDARY_BLOCKED_FAIL_CLOSED` | Any design would produce or expose a new final arbiter output. | NO-GO. |
| `DECISION_GENERATION_BOUNDARY_BLOCKED_FAIL_CLOSED` | Any design would call Decision generation or resolver logic. | NO-GO. |
| `CANDIDATE_BOUNDARY_BLOCKED_FAIL_CLOSED` | Any design would create Candidate/ranking semantics. | NO-GO. |
| `POINT_BOUNDARY_BLOCKED_FAIL_CLOSED` | Any design would create Point or entry/stop/TP/RR semantics. | NO-GO. |
| `TRADING_BOUNDARY_BLOCKED_FAIL_CLOSED` | Any design would create order/execution/trading semantics. | NO-GO. |

## Safety Fields

Any future projection must return or display these fields with fixed values:

- `reviewOnly=true`
- `manualReviewOnly=true`
- `notExternalAiCall=true`
- `notThreeAiProviderOrchestration=true`
- `notFinalArbiter=true`
- `notDecisionGeneration=true`
- `notCandidateSignal=true`
- `notPointSignal=true`
- `notFinalDirection=true`
- `notEntryStopTpRr=true`
- `notPushSend=true`
- `notRecheckExecution=true`
- `notReplayExecution=true`
- `notTradingSignal=true`
- `notExecutable=true`
- `displaySlotsAreCandidatePool=false`

These fields are negative safety assertions. They must not be used to imply that AI output is complete, executable, final, trade-authorized, or source-of-truth for points.

## Fail-Closed Rules

The status must fail closed or remain partial review-only when any of the following are true:

- Latest DecisionResult is missing.
- Latest DecisionResult cannot be read.
- `readModelTruthStatus` is stale, unknown, or partial.
- `aiConflictLevel` is missing.
- `aiConflictScore` is missing.
- `aiPlanMode` is missing.
- `aiRoleResults` is missing or blank.
- `aiRoleResults` exists only as raw provider/final-arbiter prose that cannot be safely interpreted as convergence status.
- Existing DecisionResult status already covers the intended status and a new projection would duplicate it.
- Any implementation would require resolver/provider/Decision generation.

Fail-closed output must not fabricate:

- role agreement;
- role convergence;
- role divergence;
- final arbiter decision;
- final direction;
- entry / stop / TP / RR;
- Candidate/ranking;
- Point;
- Push/Recheck/Replay action;
- order/execution/trading action.

## AI Generation Boundary

This design blocks:

- external AI call;
- Three AI provider orchestration;
- final arbiter generation;
- `DecisionEngineService` call;
- `AiConflictResolverService` / `AiConflictResolverServiceImpl` call;
- new Decision generation;
- final direction generation;
- entry / stop / TP / RR generation;
- Candidate generation;
- Point generation;
- Push send / external channel;
- Recheck / Replay execution;
- order / execution / auto-trading;
- Position Monitor execution.

Any future implementation must prove that it only reads persisted DecisionResult AI fields. It must not recalculate conflict, re-run role providers, or convert `aiRoleResults` text into executable semantics.

## Implementation Readiness Gate Checklist

The next readiness gate must answer:

1. Does the existing `/api/dashboard/decision-result-status` already satisfy this slice?
2. If yes, should the next implementation be NO-GO to avoid duplicate DecisionResult status?
3. If no, what exact safety/readability gap requires one minimal projection?
4. Is the implementation limited to existing DecisionResult owner path reads?
5. Is a dedicated endpoint necessary, or can existing DecisionResult status be reused?
6. Is a dedicated dashboard panel necessary, or can existing AI shell copy be reused?
7. Are all safety fields present and negative-only?
8. Are final arbiter / final direction / entry-stop-TP-RR fields excluded?
9. Are Candidate, Point, Push, Recheck, Replay, order/execution, trading, and Position Monitor semantics absent?
10. Are new DTO / Validator / Assembler / Orchestrator and new service/domain/mapper/repository owners still forbidden?

## Maximum Allowed Future Implementation Files

Only if the readiness gate returns GO:

- `src/main/java/org/example/trademodel/controller/DashboardController.java` for one minimal read-only `Map` endpoint or a small projection inside an existing DecisionResult status path.
- `src/main/resources/templates/dashboard.html` for minimal AI conflict / role convergence status copy or DOM only if reuse is insufficient.
- `src/test/java/org/example/trademodel/controller/DashboardControllerTest.java` for safety flags, fail-closed states, duplication review, owner-path assertion, and forbidden executable/action fields absent.
- implementation report docs.
- source-of-truth docs.

Forbidden future implementation files:

- schema/config/pom;
- new DTO / Validator / Assembler / Orchestrator;
- new service/domain/mapper/repository ownership family;
- AI provider orchestration files;
- `AiConflictResolverService` / `AiConflictResolverServiceImpl`;
- `DecisionEngineService`;
- Push / Recheck / Replay / order / execution / Position Monitor implementation files.

## Next Allowed Action

`Implementation readiness gate for AI conflict / AI role convergence read-only status`

Next branch:

`ai-conflict-ai-role-convergence-status-implementation-readiness-gate`

The readiness gate may return GO only for a minimal read-only DecisionResult projection with duplication review. It must return NO-GO if existing DecisionResult status is sufficient or if implementation requires new owner skeletons, provider calls, resolver calls, or generation/execution behavior.

## Overreach Status

No overreach in this design package:

- No Java business code changed.
- No tests changed.
- No dashboard business logic changed.
- No schema/config/pom changed.
- No endpoint or panel behavior implemented.
- No DTO / Validator / Assembler / Orchestrator added.
- No service/domain/mapper/repository ownership family added.
- No external AI call, Three AI orchestration, final arbiter output, Decision generation, Candidate generation, Point generation, Push, Recheck, Replay, order/execution, auto-trading, Position Monitor execution, or P359/P360 work performed.
