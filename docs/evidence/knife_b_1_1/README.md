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

## B.1.1 visual evidence micro-close

Visual measurement source Head: `6a90d16ba0c4c01c87403f79d41d0f76037337b5`.

The following measurements were taken from an authenticated `ui-review`
runtime at 100% browser zoom. Every top capture was made with
`fullPage=false` after `window.scrollTo(0, 0)` and a settled `scrollY=0`.
The fixtures remain `UI_REVIEW_FIXTURE`; they do not prove live providers,
live AI, CoinGlass, or production market data.

### Controlled viewport measurements

| Route / state | Requested viewport | innerWidth | clientWidth | scrollWidth | body scrollWidth | scrollbarWidth | overflowDelta | DPR | scrollY | Visible overflow offenders | PageHeader/content intersection | Console errors | Screenshot |
|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---|---:|---|
| `/dashboard` / Home | 1080x900 | 1080 | 1080 | 1080 | 1080 | 0 | 0 | 1 | 0 | 0 | N/A | 0 | `visual_micro_close/home-1080x900-top.png` |
| `/dashboard` / Home | 1440x900 | 1440 | 1440 | 1440 | 1440 | 0 | 0 | 1 | 0 | 0 | N/A | 0 | `visual_micro_close/home-1440x900-top.png` |
| `/analysis/ui-review-analysis-preview` / Preview | 1080x900 | 1080 | 1080 | 1080 | 1080 | 0 | 0 | 1 | 0 | 0 | None | 0 | `visual_micro_close/analysis-preview-1080x900-top.png` |
| `/analysis/ui-review-analysis-preview` / Preview | 1440x900 | 1440 | 1440 | 1440 | 1440 | 0 | 0 | 1 | 0 | 0 | None | 0 | Measurement only |
| `/analysis/ui-review-gemini-downgrade` / Opportunity, Gemini data | 1080x900 | 1080 | 1080 | 1080 | 1080 | 0 | 0 | 1 | 0 | 0 | None | 0 | Measurement only |
| `/analysis/ui-review-gemini-downgrade` / Opportunity, Gemini data | 1440x900 | 1440 | 1440 | 1440 | 1440 | 0 | 0 | 1 | 0 | 0 | None | 0 | `visual_micro_close/analysis-gemini-1440x900-top.png` |
| `/analysis/ui-review-grok-found` / Opportunity, Grok data | 1080x900 | 1080 | 1080 | 1080 | 1080 | 0 | 0 | 1 | 0 | 0 | None | 0 | Measurement only |
| `/analysis/ui-review-grok-found` / Opportunity, Grok data | 1440x900 | 1440 | 1440 | 1440 | 1440 | 0 | 0 | 1 | 0 | 0 | None | 0 | Measurement only |
| `/analysis/ui-review-analysis-unknown` / unknown mode | 1080x900 | 1080 | 1080 | 1080 | 1080 | 0 | 0 | 1 | 0 | 0 | None | 0 | `visual_micro_close/analysis-unknown-1080x900-top.png` |
| `/analysis/ui-review-analysis-unknown` / unknown mode | 1440x900 | 1440 | 1440 | 1440 | 1440 | 0 | 0 | 1 | 0 | 0 | None | 0 | Measurement only |

`window.visualViewport` matched the requested viewport in all ten runs:
1080x900 or 1440x900. All runs had `scrollX=0`, no clipped visible product
text detected by the controlled scan, and zero localhost console errors.

### Role-section viewport evidence

| Screenshot | Route / state | CSS viewport | innerWidth | clientWidth | PNG physical size | DPR | scrollY | Header bottom | Role tabs top/bottom | Role content top | Focused tab intersects Header | Proves | Does not prove |
|---|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|---|---|---|
| `visual_micro_close/analysis-preview-1080x900-role.png` | Preview / GPT | 1080x900 | 1080 | 1080 | 1080x900 | 1 | 304.5 | 52 | 317.41 / 359.41 | 359.41 | No | role tabs, selected GPT tab, and first visual are readable and unobstructed | live AI output |
| `visual_micro_close/analysis-gemini-1440x900-role.png` | Opportunity / Gemini downgrade | 1440x900 | 1440 | 1440 | 1440x900 | 1 | 457.5 | 52 | 164.41 / 206.41 | 206.41 | No | complete tabs and Gemini first visual remain reachable after normal scrolling | every Gemini enum combination |
| `visual_micro_close/analysis-grok-1440x900-role.png` | Opportunity / Grok found | 1440x900 | 1440 | 1440 | 1440x900 | 1 | 554 | 52 | 67.91 / 109.91 | 109.91 | No | complete tabs and Grok first visual remain reachable after normal scrolling | live Grok output |

The eight PNGs have real PNG signatures and exact 1080x900 or 1440x900
physical dimensions. They were transcoded losslessly from the in-app browser's
uncropped viewport capture encoding only; no crop, resize, browser frame,
transform, zoom, or content-concealing edit was applied.

### Controlled reproduction result

- Home 1080 and 1440: `overflowDelta=0`, visible overflow offender count `0`,
  visible clipped-product-content count `0`. Therefore
  `HOME_HORIZONTAL_OVERFLOW = 0`; `home.css` was not changed.
- Analysis initial navigation: the 52 px sticky PageHeader did not intersect
  the section heading, search, mode banner, overview, role tabs, or role content
  at `scrollY=0` on any route or width.
- Analysis interactions: switching GPT, Gemini, and Grok at 1080 left each
  selected tab below the Header (`focusedIntersectsHeader=false` and
  `roleTabsIntersectsHeader=false`). Focusing the visible Search control at
  `scrollY=0` left its rect at `top=172.95`, `bottom=209.79`, with no Header
  intersection.
- During ordinary manual scrolling, earlier sections naturally pass behind the
  sticky Header while the role tabs and role first visual remain visible. A
  full-page capture started at non-zero `scrollY` can therefore paint that
  sticky Header in the middle of the composed long image. That capture behavior
  is not a runtime obstruction of a required control.
- Result:
  `ANALYSIS_RUNTIME_OVERLAP = NOT_REPRODUCED_CAPTURE_STATE_ARTIFACT`.
  `workspace.css` was not changed and no CSS fix is claimed.

### Micro-close status

| Gate | Status |
|---|---|
| `HOME_HORIZONTAL_OVERFLOW` | `0` |
| `ANALYSIS_SCREENSHOT_OVERLAP` | `CONFIRMED` (historical non-zero-scroll full-page capture) |
| `ANALYSIS_RUNTIME_OVERLAP` | `NOT_REPRODUCED_CAPTURE_STATE_ARTIFACT` |
| Browser console errors | `0` |
| `B1.1-04_EVIDENCE` | `PASS` |
| `KNIFE_B_1_1_IMPLEMENTATION_DONE` | `YES` |
| Package type | `EVIDENCE_ONLY` |
| `CURRENT_PHASE_DONE` | `NO` |
| `LIVE_RUNTIME_ACCEPTANCE_DONE` | `NO` |
| `GLOBAL_SEMANTIC_RUNTIME_DONE` | `NO` |
