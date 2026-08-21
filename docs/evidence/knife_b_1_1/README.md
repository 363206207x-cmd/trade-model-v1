# Fundamental AI v4.1 Knife B.1.1 Evidence Index

Evidence source Head: `c376950f9ce7c0f2d7eae75c8eb861ca9ae38255`.

The screenshots were captured from the byte-identical worktree immediately
before that Head was committed. The `ui-review` profile is isolated, disables
external providers and schedulers, and is not live-provider evidence.

## Runtime visual evidence

| File | Route / state | Classification | Proves | Does not prove |
|---|---|---|---|---|
| `home-four-position-1440.png` | `/dashboard`, 1,440 px, four active fixture positions | `UI_REVIEW_FIXTURE` | aggregate says 4, highest trusted risk is EXTREME, coverage is partial, while the list has three rows | live positions, provider freshness |
| `home-four-position-1080.png` | `/dashboard`, 1,080 px, same state | `UI_REVIEW_FIXTURE` | same Top3/full-aggregate split at the narrow desktop width; document horizontal overflow is absent | live positions, provider freshness |
| `analysis-opportunity-gemini-1440.png` | `/analysis/ui-review-gemini-downgrade`, Opportunity, Gemini tab | `UI_REVIEW_FIXTURE` | formal `DOWNGRADE`, Candidate ownership, Before PREPARATION to OBSERVATION, and independent evidence-gap/conflict/risk groups | the other three Gemini enum visuals or live AI output |
| `analysis-opportunity-grok-1440.png` | `/analysis/ui-review-grok-found`, Opportunity, Grok tab | `UI_REVIEW_FIXTURE` | `FOUND` renders a verifiable trigger -> causal evolution -> invalidation path in the role panel | live Grok output or the empty/conflicting path combinations |
| `analysis-preview-1080.png` | `/analysis/ui-review-analysis-preview`, Preview | `UI_REVIEW_FIXTURE` | Preview is identified as Preview and does not render Candidate or Opportunity failure-path content | live Preview analysis |
| `analysis-unknown-1080.png` | `/analysis/ui-review-analysis-unknown`, unknown mode | `UI_REVIEW_FIXTURE` | unknown mode fails closed without Candidate or failure paths; document horizontal overflow is absent | a production persisted unknown mode |

All six files are actual PNG files. The authenticated UI-review session
returned HTTP 200 for Home and Analysis routes and recorded zero browser console
errors. Browser claims are limited to the routes and states listed above.

## Executed automated evidence

| Contract | Classification | Executed evidence |
|---|---|---|
| Gemini APPROVE, DOWNGRADE, REJECT_CANDIDATE, RISK_WARNING and unknown | `AUTOMATED_TEST` | `scripts/frontend-contract-state-matrix.mjs`, executed by `FrontendContractNodeMatrixTest` in Maven |
| Grok FOUND/non-empty, FOUND/empty, NO_VERIFIABLE_FAILURE_PATH/empty, non-FOUND/non-empty | `AUTOMATED_TEST` | same executable production-function matrix |
| roleState READY, PARTIAL, FALLBACK, UNAVAILABLE, ERROR and explicit `resultAvailable` | `AUTOMATED_TEST` | same matrix plus `AiRoleResultsCodecTest` and `UiReviewDecisionChainAuditQueryServiceTest` |
| Preview, Opportunity and unknown/missing mode isolation | `AUTOMATED_TEST` | same matrix plus `KnifeBFrontendContractTest` |
| Recheck atomic success and rollback/error boundaries | `AUTOMATED_TEST` | eight real Spring/H2 cases in `PushRecheckCoreTransactionIntegrationTest` |
| F5/bind/GET creates zero PUSH_OPEN attempts | `AUTOMATED_TEST` | `WorkspacePushRecheckServiceTest#reloadAndReadOnlyGetNeverCreateOpen` and frontend GET binding contract |
| Four active positions, Top3 list, fourth-position highest risk, partial coverage and selected-asset invariance | `AUTOMATED_TEST` | `DashboardHomeServiceImplTest` and `UiReviewDashboardHomeServiceTest` |

## Explicit boundaries

- `LIVE` evidence: none in this package.
- `FRESHNESS = NOT_VERIFIED`: no frozen freshness duration was invented.
- `CROSS_INSTANCE_IDEMPOTENCY = PARTIAL`: only in-process coalescing is verified.
- `SAFETY_MESSAGE_CHAIN = PARTIAL`: after-commit execution is verified; failure is recorded in `tm_push_recheck_log.execution_error_code=SAFETY_MESSAGE_FAILED`, but downstream delivery is not claimed.
- `KB-06 = BLOCKED_BY_MISSING_PERSISTENCE_SOURCE`.
- Data-rich Recheck browser evidence is `NOT_VERIFIED_BROWSER_DATA_BOUNDARY`; transaction evidence is automated.
- `LIVE_RUNTIME_ACCEPTANCE_DONE = NO`.
