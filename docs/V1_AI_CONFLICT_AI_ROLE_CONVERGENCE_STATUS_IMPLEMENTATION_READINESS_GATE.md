# V1 AI Conflict / AI Role Convergence Status Implementation Readiness Gate

## Scope

This readiness gate decides whether `AI conflict / AI role convergence read-only status` should continue to a B-risk minimal implementation.

This package is readiness-gate only. It does not implement an endpoint, dashboard panel, AI conflict projection, external AI call, Three AI provider orchestration, AI budget/cache/fallback orchestration, final arbiter behavior, Decision generation, Candidate generation, Point generation, Push, Recheck, Replay, order/execution, auto-trading, Position Monitor execution, schema/config/pom changes, or new DTO / Validator / Assembler / Orchestrator / ownership-family code.

Effective execution baseline:

- Previous merged package: PR #989.
- Effective local main before this branch: `fca42d8 docs(runtime): design ai conflict status wiring (#989)`.
- Source of Truth baseline lag is not blocking; this package uses actual merged main as the effective execution baseline.

## Readiness Decision

Decision: `NO-GO: duplicate with DecisionResult status`.

The next B-risk implementation is not allowed.

Reason:

- Existing `GET /api/dashboard/decision-result-status?symbol=BTCUSDT` already owns the persisted DecisionResult review-only status surface.
- Existing DecisionResult owner path already reads and exposes the key AI role/conflict evidence needed for this slice:
  - `DecisionResult.aiRoleResults`
  - `DecisionResult.aiConflictLevel`
  - `DecisionResult.aiConflictScore`
  - `DecisionResult.aiPlanMode`
  - `DecisionResult.confusedScore`
  - `DecisionResultMapper` latest joined reads
  - `DecisionResultVO`
  - dashboard AI shell / `decisionResultStatusPanel`
  - review-page AI conflict display context
- The design did not identify a separate owner path that can avoid duplicating DecisionResult status.
- A dedicated AI conflict / role convergence endpoint or dashboard panel would mostly mirror existing DecisionResult evidence while expanding runtime surface area.

## Duplication Decision

Decision: existing DecisionResult status is sufficient for this review-only slice.

The source read and design confirmed that the AI conflict / role convergence topic is not a new standalone runtime capability. It is a persisted DecisionResult read-model projection. Because the completed DecisionResult review-only dashboard/API status already covers this owner path, this gate blocks a standalone implementation.

Duplicate surfaces blocked by this gate:

- New `GET /api/dashboard/ai-conflict-role-convergence-status` endpoint.
- New dashboard AI conflict / role convergence status panel.
- New Java projection logic that mirrors `aiRoleResultsAvailable`, `aiRoleResultsSummary`, or existing DecisionResult status fields.
- New DTO / Validator / Assembler / Orchestrator or new service/domain/mapper/repository owner for AI conflict status.

Approved handling:

- Keep using `GET /api/dashboard/decision-result-status?symbol=BTCUSDT`.
- Keep using existing dashboard AI shell and DecisionResult status display as review-only context.
- Treat `DUPLICATE_DECISIONRESULT_STATUS_REVIEW_REQUIRED` as resolved by this gate with NO-GO for implementation.
- If future DecisionResult maintenance already touches the owner path for another non-duplicate reason, AI safety copy may be re-evaluated there. That would require a new readiness decision and is not authorized by this package.

## Owner Path Decision

Canonical owner path remains:

`DecisionResult` persisted AI fields -> `DecisionResultMapper` latest joined reads -> existing DecisionResult read service / dashboard detail read -> `DecisionResultVO` -> `GET /api/dashboard/decision-result-status` -> existing dashboard AI shell / review-page display context.

Allowed owner path for current state:

- `GET /api/dashboard/decision-result-status?symbol=BTCUSDT`
- dashboard `decisionResultStatusPanel`
- dashboard AI shell / `decisionAiRoleValue`
- review-page AI conflict display context

Disallowed owner paths:

- `AiConflictResolverService`
- `AiConflictResolverServiceImpl`
- `DecisionEngineService`
- external AI provider clients
- AI budget/cache/fallback orchestration
- final arbiter output path
- `DecisionBundleVO` Push/Recheck-adjacent output path
- any DecisionResult write or generation path

## Endpoint Decision

Dedicated endpoint decision: not approved.

This gate does not allow a new dedicated AI conflict / role convergence status endpoint because existing `/api/dashboard/decision-result-status` already covers the persisted DecisionResult owner path. A new endpoint would create duplicate status ownership without reducing duplication or moving capability forward.

## Dashboard Decision

Dedicated dashboard panel decision: not approved.

This gate does not allow a new dashboard AI conflict status panel because existing dashboard AI shell / DecisionResult status display already carries the review-only AI role/conflict context. Adding another panel would duplicate DecisionResult status unless a future package proves a separate, non-duplicative safety closure.

## Allowed Implementation Files

Allowed implementation files for the next package: none.

Because the readiness decision is NO-GO, there is no authorized B-risk implementation package for this module.

If a future package reopens this topic, it must start from a new source/readiness decision and prove that it is not duplicating DecisionResult status. The old conditional maximum file list is not currently approved:

- `DashboardController.java`: not approved in this package.
- `dashboard.html`: not approved in this package.
- `DashboardControllerTest.java`: not approved in this package.
- existing DecisionResult tests: not approved in this package.

## Forbidden Files

This gate keeps the following forbidden for this module:

- Java business code.
- Tests.
- Dashboard business logic.
- Schema/config/pom.
- New DTO / Validator / Assembler / Orchestrator.
- New service/domain/mapper/repository ownership family.
- AI provider orchestration files.
- `AiConflictResolverService` / `AiConflictResolverServiceImpl`.
- `DecisionEngineService`.
- Push / Recheck / Replay / order / execution / Position Monitor implementation files.

## Required Tests

Required tests for this package:

- No Maven tests are required because this package is docs/source-of-truth only.
- Required validation is documentation and workflow validation:
  - workflow contract check;
  - state / next-task checks;
  - readiness doc grep for NO-GO, duplication decision, DecisionResult owner path, forbidden AI generation boundaries;
  - forbidden scope diff check;
  - `git diff --check`.

If a future package reopens the topic with a new GO decision, targeted tests would need to cover:

- endpoint safety flags;
- fail-closed states;
- duplication boundary;
- AI generation boundary;
- forbidden executable/action fields absent;
- existing DecisionResult owner-path assertion.

These tests are not authorized now because the implementation itself is not authorized.

## Safety Fields Decision

The following safety fields remain required only for any future non-duplicate projection. This gate does not authorize implementing them in a new endpoint or panel:

- `reviewOnly=true`
- `manualReviewOnly=true`
- `notExternalAiCall=true`
- `notThreeAiProviderOrchestration=true`
- `notAiBudgetCacheFallbackOrchestration=true`
- `notFinalArbiter=true`
- `notAiConflictResolverRuntimeCall=true`
- `notDecisionEngineRuntimeCall=true`
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

## Status Mapping Decision

The proposed status mapping is not approved for standalone implementation in this module:

| Status | Readiness decision |
|---|---|
| `AI_CONFLICT_STATUS_REVIEW_ONLY_READY` | Not approved as a new standalone status; use DecisionResult status. |
| `AI_CONFLICT_STATUS_MISSING_FAIL_CLOSED` | Not approved as a new standalone status; use existing DecisionResult fail-closed/read-model status. |
| `AI_CONFLICT_STATUS_PARTIAL_REVIEW_ONLY` | Not approved as a new standalone status; use existing DecisionResult partial status. |
| `AI_ROLE_CONVERGENCE_REVIEW_ONLY_READY` | Not approved as a new standalone status; AI role evidence remains DecisionResult review context. |
| `AI_ROLE_CONVERGENCE_MISSING_FAIL_CLOSED` | Not approved as a new standalone status; use existing DecisionResult missing role evidence. |
| `AI_ROLE_RESULTS_READ_ONLY_EVIDENCE` | Existing DecisionResult status/dashboard context already covers this as read-only evidence. |
| `DUPLICATE_DECISIONRESULT_STATUS_REVIEW_REQUIRED` | Resolved as NO-GO in this gate. |
| `EXTERNAL_AI_CALL_BOUNDARY_BLOCKED_FAIL_CLOSED` | Blocked. |
| `THREE_AI_PROVIDER_ORCHESTRATION_BOUNDARY_BLOCKED_FAIL_CLOSED` | Blocked. |
| `FINAL_ARBITER_BOUNDARY_BLOCKED_FAIL_CLOSED` | Blocked. |
| `DECISION_GENERATION_BOUNDARY_BLOCKED_FAIL_CLOSED` | Blocked. |
| `CANDIDATE_BOUNDARY_BLOCKED_FAIL_CLOSED` | Blocked. |
| `POINT_BOUNDARY_BLOCKED_FAIL_CLOSED` | Blocked. |
| `TRADING_BOUNDARY_BLOCKED_FAIL_CLOSED` | Blocked. |

## NO-GO Conditions

The readiness gate returns NO-GO because this condition is true:

- Existing `/api/dashboard/decision-result-status` already covers the AI conflict / role convergence owner path sufficiently for current review-only runtime status.

Additional NO-GO conditions remain:

- Requires external AI call.
- Requires Three AI provider orchestration.
- Requires AI budget/cache/fallback orchestration.
- Requires final arbiter output.
- Requires calling `AiConflictResolverService` or `DecisionEngineService` to generate new status.
- Requires Decision generation.
- Requires final direction.
- Requires entry / stop / TP / RR.
- Requires Candidate generation.
- Requires Point generation.
- Requires Push send.
- Requires external channel.
- Requires Recheck / Replay execution.
- Requires order / execution / auto-trading.
- Requires Position Monitor execution.
- Requires schema/config/pom.
- Requires new DTO / Validator / Assembler / Orchestrator.
- Requires new service/domain/mapper/repository ownership family.
- Cannot guarantee the module is only a persisted DecisionResult read-only projection.

## AI Generation Boundary

This gate blocks:

- external AI call;
- Three AI provider orchestration;
- AI budget/cache/fallback orchestration;
- final arbiter behavior;
- `AiConflictResolverService` runtime call;
- `DecisionEngineService` runtime call;
- new Decision generation;
- final direction generation;
- entry / stop / TP / RR generation;
- Candidate generation;
- Point generation;
- Push send / external channel;
- Recheck / Replay execution;
- order / execution / auto-trading;
- Position Monitor execution.

The only valid read path remains persisted DecisionResult evidence already exposed through DecisionResult review-only status.

## Next Allowed Action

Next allowed action:

`Next minimal runtime slice selection after AI conflict / AI role convergence NO-GO readiness`

Next branch:

`next-minimal-runtime-slice-selection-after-ai-conflict-role-convergence-no-go`

The next package must be selection/source-of-truth only and must choose a different minimal, low-conflict, review-only runtime slice. It must not implement AI conflict / role convergence status after this NO-GO.

## Overreach Status

No overreach in this readiness gate:

- No Java business code changed.
- No tests changed.
- No dashboard business logic changed.
- No schema/config/pom changed.
- No endpoint or panel behavior implemented.
- No new DTO / Validator / Assembler / Orchestrator added.
- No new service/domain/mapper/repository ownership family added.
- No external AI call, provider orchestration, final arbiter, Decision generation, Candidate generation, Point generation, Push, Recheck, Replay, order/execution, trading, or Position Monitor behavior added.
- Capability level remains `REVIEW_ONLY_RUNTIME partial`.
