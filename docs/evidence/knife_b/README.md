# Fundamental AI v4.1 Knife B Evidence Index

> Historical evidence captured before Knife B.1 residual closure. Current evidence and status are indexed in `../knife_b_1/README.md`; this directory must not be used as exact-Head proof for the current PR.

## Evidence Classification

- Files prefixed `normal-` are local standard-release runtime evidence using persisted local state. They are not live-provider acceptance evidence.
- Files prefixed `ui-review-` are **FIXTURE / UI-REVIEW** visual evidence. They are isolated from production capabilities and must not be described as live data.
- Owner, trust, idempotency and object-lineage assertions that require controlled records are proven by Java tests rather than by fabricated browser data.

## Browser Runtime Evidence

| Evidence | Path | Result |
|---|---|---|
| Normal Home, 1,440 width | `normal-home-1440.png` | Standard JAR, HTTP 200, real empty state |
| Normal Home, 1,080 width | `normal-home-1080.png` | No document overflow; no user-visible text clipping |
| **FIXTURE / UI-REVIEW** Home, 1,440 width | `ui-review-home-1440.png` | Knife-A Home regression and Three-AI hierarchy |
| **FIXTURE / UI-REVIEW** Home, 1,080 width | `ui-review-home-1080.png` | No document overflow; no user-visible text clipping |
| **FIXTURE / UI-REVIEW** Analysis preview selection | `ui-review-analysis-preview.png` | Preview mode selected; Candidate/Resolver/Validation/Final DOM count 0 |
| **FIXTURE / UI-REVIEW** Active Positions | `ui-review-positions-active.png` | Active tab and owner-scoped empty state |
| **FIXTURE / UI-REVIEW** Position History | `ui-review-positions-history.png` | Real tab click changes URL to `?tab=history`; Review remains separate |
| **FIXTURE / UI-REVIEW** Current Final | `ui-review-plan-current.png` | Validated Final-only detail boundary |
| **FIXTURE / UI-REVIEW** Home audit-link destination | `ui-review-audit-link.png` | Home full-audit link routes to `/audit/{traceId}` and preserves Home context |

Browser metrics at both 1,440 and 1,080: document `scrollWidth == clientWidth`, user-visible overflow count 0, user-visible text clipping count 0. Browser console error count: 0.

## Required Evidence Mapping

| # | Requirement | Evidence |
|---:|---|---|
| 1 | Four active positions; #4 has its own monitor | `PositionMonitoringProjectionServiceTest#fourthPositionUsesItsOwnLatestMonitorInsteadOfHomeTopThree` |
| 2 | Non-Top3 Position Detail | `WorkspacePositionMonitoringController` owner-scoped detail API plus controller/security tests |
| 3 | Active/History tabs | `ui-review-positions-active.png`, `ui-review-positions-history.png`, `KnifeBFrontendContractTest` |
| 4 | Missing monitor keeps facts and one fail-closed state | `PositionMonitoringProjectionServiceTest#missingMonitorPreservesPositionFactsAndFailsClosedOnce` |
| 5 | `ANALYSIS_PREVIEW` | `ui-review-analysis-preview.png`, `KnifeBFrontendContractTest` |
| 6 | `OPPORTUNITY_DECISION` | `KnifeBFrontendContractTest#analysisUsesFormalModeStructuredRolesAndConflictGate` |
| 7 | Unknown Mode fail closed | `KnifeBFrontendContractTest#analysisUsesFormalModeStructuredRolesAndConflictGate` |
| 8 | Preview has no Candidate/Resolver/Validation/Final | Browser DOM count 0 in the preview capture; `KnifeBFrontendContractTest` |
| 9 | Role-specific full IA; no primary raw JSON | `DecisionChainAuditVO.AiRoleResultsPayload`, `workspace.js`, `KnifeBFrontendContractTest` |
| 10 | L1 hides Conflict Summary | `workspace.js` formal conflict-level gate and `KnifeBFrontendContractTest` |
| 11 | Recheck original snapshot | `WorkspacePushRecheckService.Projection.originalSnapshot`, `workspace.html#originalSnapshot` |
| 12 | Recheck current metrics and diff | `workspace.html#recheckCurrentMetrics`, `workspace.html#recheckDiff` |
| 13 | Persisted `PUSH_OPEN` | `WorkspacePushRecheckServiceTest#firstOpenCreatesOnePushOpenAndKeepsAllIdsDistinct` |
| 14 | No fake `SCHEDULED` metadata | `PushRecheckAccessBoundaryKnifeBTest#ownerScopedPushOpenUsesNoSchedulerIdentityAndManualRemainsDenied` |
| 15 | Read-only GET creates no record | `WorkspacePushRecheckServiceTest#reloadAndReadOnlyGetNeverCreateOpen` |
| 16 | ERROR retry creates new attempt | `WorkspacePushRecheckServiceTest#errorRetryCreatesNewAttemptForSameTarget` |
| 17 | IDs remain separate | Recheck audit IA plus `WorkspacePushRecheckServiceTest#firstOpenCreatesOnePushOpenAndKeepsAllIdsDistinct` |
| 18 | Cross-user Recheck 404 | `WorkspacePushRecheckServiceTest#crossUserOrIdentityMismatchFailsAsNotFoundWithoutEngineInvocation` and owner-mismatch test |
| 19 | Raw pushId route remains denied | `PushRecheckAccessBoundaryKnifeBTest`; no raw route enabled |
| 20 | O06 Final prefill + actual `openedAt` | `UserPositionServiceImplTest` and O06 frontend contract assertions |
| 21 | Closed Position appears in History | closed-position service/controller mapping and full-lifecycle tests |
| 22 | Corrected Home audit link | `ui-review-audit-link.png`, `KnifeBFrontendContractTest#recheckAndHomeAuditLinksPreserveObjectAndReturnIdentity` |
| 23 | Legacy route matrix | `docs/FUNDAMENTAL_AI_V4_1_KNIFE_B_LEGACY_ROUTE_MATRIX.md` |
| 24 | Knife-A Home regression | `ui-review-home-1440.png`, `ui-review-home-1080.png`, Home contract tests |

## Known Evidence Boundary

The isolated UI-review profile does not persist four synthetic UserPositions or Recheck rows. Controlled owner/trust/lineage cases therefore remain automated-test evidence, not screenshots. No fake production data was added to satisfy the visual checklist.
