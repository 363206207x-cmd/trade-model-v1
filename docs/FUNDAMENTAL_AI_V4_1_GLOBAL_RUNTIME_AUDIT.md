# Fundamental AI v4.1 Global Runtime Audit

Audit baseline: `a2168b784a3b181ea9e0d688f064d18e5091fd7b`
Branch: `codex/frontend-interaction-runtime-closure`
PR: `#1195` (Draft, unmerged)

## Executive conclusion

The active Home is the approved `home.html` implementation and the later branch work has already closed most of the 2026-08-20 visual findings: dynamic Top6, per-asset Final ownership, 7:3 decision layout, Position four-column grouping, trusted-monitor fail-closed behavior, validated-Final access, and the single three-role workspace are present. The remaining current-stage blockers are projection and presentation semantics, not a missing product skeleton.

Seven bounded blockers were confirmed before implementation: the System Status producer still uses the selected asset for environment/risk/data; GPT promotes `candidate.summary` above the three frozen primary values; Gemini merges three independently owned collections; Grok promotes `challengeSummary` instead of `failurePathState` and displays `planModeImpact` as if Grok changes the Final; unknown uppercase values collapse to role-unavailable copy; the derivatives strip invents a `CoinGlass v4` source label when no source is present; and the navigation label retains the unfrozen `AI分析` spelling. All are inside the task's allowed Home projection/mapper/copy boundary.

## Scope and exclusions

Audited end to end: canonical sources, routes, state legality, Asset Pool and Top6, three-AI structured contract, provider evidence, Candidate/Resolver/Rule Validation/Final ownership, Position lifecycle, Home VO, frontend mapper, renderer, responsive CSS, fixtures, and runtime guards.

Registered only: independent IA quality for `/positions`, `/analysis`, `/messages`, and `/me`.

Excluded and unchanged: Telegram implementation, authentication, Figma, Mobile, schema/migrations, AI prompts, domain enums/objects, orchestration refactors, automatic trading, and legacy `dashboard.html`.

## Source authority

| Priority | Source | Evidence |
|---|---|---|
| 1 | Unified v4.1 product freeze | `docs/product-sources/FUNDAMENTAL_AI_V4_1_DECISION_CHAIN.md`, registered by `docs/PRODUCT_SOURCE_OF_TRUTH.md`; original `/Users/xuchao/Documents/唯一产品开发方案_最终冻结版.docx`, SHA-256 matches registry |
| 2 | Final interaction specification | original `/Users/xuchao/Documents/Fundamental_AI_v4.1_最终交互逻辑与页面设计开发规格_冻结版.docx`, SHA-256 matches registry |
| 3 | UI execution freeze 1.2 | original `/Users/xuchao/Desktop/Fundamental_AI_v4.1_UI设计与交互执行冻结文件_今日细化最终版.docx` |
| Supporting | Three-AI/CoinGlass closure | `docs/THREE_AI_COINGLASS_SEMANTIC_AUDIT_CLOSURE.md` |
| Supporting | Desktop geometry | `docs/FUNDAMENTAL_AI_V4_1_VISUAL_DENSITY_AND_PROPORTION_CONTRACT.md` |

The requested filename variants with `(3)` / `(1)` were not present, but the registered originals without suffixes were present. The first two hashes match the authoritative source registry, so this is not a missing-source blocker.

## Route and architecture map

| Route | Owner | Current implementation | Result |
|---|---|---|---|
| `/dashboard` | Home | `DashboardController.dashboard()` returns `home`; `templates/home.html` + `home-runtime.js` | Active Home confirmed |
| `/positions`, `/analysis`, `/messages`, `/me` | Primary tasks | `DesktopWorkspaceController` returns shared `workspace.html` with route-specific `pageKey` | P1 IA remains shared and is registered only |
| `/plans/{planId}` | Focused Final detail | shared focused workspace route and existing runtime endpoint | Existing implementation, not changed here |
| legacy `dashboard.html` | historical compatibility | not returned by `GET /dashboard` | Not active Home |

## Business-object ownership

| User statement | Formal owner | Producer / persistence | Home transport / consumer |
|---|---|---|---|
| Top6 opportunity | Asset Pool opportunity ranking | `OpportunityPriorityRankingService` / `HomeTopAssetProjection` | `DashboardHomeServiceImpl.buildRankedAssets` -> `AssetVO` -> `renderOpportunities` |
| Candidate judgment | GPT candidate | `AiDecisionChainRole.GPT_FINAL`, `ExecutionPlanCandidateDO` | `AiRoleResultsCodec` -> `AiTabVO.candidateSummary/coreJudgment` -> GPT panel |
| Candidate review | Gemini review | `GEMINI_REVIEW` structured `reviewResult` and three collection pairs | `AiRoleResultsPayload` -> `AiTabVO` -> Gemini panel |
| Failure path | Grok challenge | `GROK_CHALLENGE.failurePathState/failurePaths` | codec -> `AiTabVO` -> Grok panel |
| Conflict adjustment | Resolver | `ConflictResolverResultDO` | consistency projection -> Conflict Summary |
| Final plan | Rule-validated Final | `ExecutionPlanDO(finalPlan=true, ruleValidationStatus=PASS)` | exact plan identity resolver -> `ExecutionSuggestionVO` -> Final Plan |
| User position | User-entered position | `UserPosition` with explicit source type | `PositionVO` -> Position Monitoring |
| Monitor judgment | trusted monitor result | `PositionMonitorLogDTO` verified and fresh | `applyTrustedMonitor`; untrusted values cleared -> Position row |
| Account strip | recorded active positions + account risk coverage | Position list and account-risk snapshot | Home runtime aggregate; no opportunity/plan substitution |

## State-machine legality

| Contract | Code evidence | Audit result |
|---|---|---|
| `waiting_trigger` can carry PREPARATION, not REDUCED/CONFIRMATION Candidate | frozen state policy and candidate/rule-validation services; frontend renders source values and does not mutate them | PASS; add regression assertion |
| Candidate != Final | separate `ExecutionPlanCandidateDO` and `ExecutionPlanDO`; `executionPlanAccess` requires Final + validation + chain/source identity | PASS |
| Final != UserPosition | `UserPositionController` explicit create/manual-close paths; no plan conversion in Home | PASS |
| `triggered` means PREPARATION revalidation | persisted lifecycle and `NEEDS_REVALIDATION` projection; card says `正在重验` | PASS |
| `WAIT_CONFIRMATION` is a Position action only | Position action map; not a lawful waiting-trigger Candidate mode | PASS |
| frontend mapper cannot alter state | mapper only translates known values; unknown fallback was misleading but did not mutate the domain code | PARTIAL; copy correction required |

## Three-AI field lineage

All formal arrays and collection states exist in `AiDecisionChainSchema`, are parsed by `AiDecisionChainResponseParser`, normalized by `AiRoleResultsCodec`, copied independently by `DashboardHomeServiceImpl.copyFormalAiContract`, and carried by `DashboardHomeVO.AiTabVO`. The transport is complete; the confirmed defects are in the Home renderer.

| Role | Frozen primary | Current producer/transport | Current renderer finding |
|---|---|---|---|
| GPT | Market Bias / Opportunity State / Candidate Mode, Candidate not Final | `coreJudgment` + `candidateSummary` | `candidate.summary` is promoted as the main value; BLOCKER |
| Gemini | `reviewResult`; independent evidence gaps / logic conflicts / underestimated risks | three list/state pairs remain independent through VO | renderer concatenates them and merges state ownership; BLOCKER |
| Grok | `failurePathState`; FOUND chain trigger -> causal path -> invalidation | `failurePathState` and `failurePaths` survive schema-to-VO | renderer promotes `challengeSummary` and presents `planModeImpact` as plan authority; BLOCKER |
| Resolver | conflict level/reason/recovery | separate resolver result ownership; not an AI trace | Home summary uses resolver fields and hides L1; PASS |

Role failure is fail-closed through `resultAvailable`, but each valid role must still preserve its formal internal collection states. The patch must not alter prompts, role authority, or the backend role schema.

## CoinGlass / Binance field lineage

| Evidence category | Current capability | Classification | Evidence path |
|---|---|---|---|
| Open interest | CoinGlass v4 snapshot; existing Binance/provider fallback where configured | REAL_CURRENT only when source READY + FRESH; otherwise REAL_STALE/FALLBACK/UNAVAILABLE | `providercall/coinglass` client/adapters -> `DerivativesRiskSnapshot` -> `DerivativesBusinessIntegrationService` -> analysis evidence / Home summary |
| Funding rate | same trust-gated snapshot chain | same | weighted funding evidence -> structured role evidence / compact summary |
| Liquidations | CoinGlass snapshot fields | REAL_CURRENT/REAL_STALE/UNAVAILABLE; never synthesized | liquidation fields -> evidence items -> role facts |
| Long/short ratio | CoinGlass snapshot field | REAL_CURRENT/REAL_STALE/UNAVAILABLE | snapshot -> crowding evidence |
| UI-review values | controlled profile fixture only | FIXTURE_ONLY | `@Profile("ui-review")`, explicit enable property, production guard |

`DashboardHomeServiceImpl.buildDerivativesSummary` correctly withholds positive decision impact when source freshness is unavailable. The Home renderer still defaults a missing source label to `CoinGlass v4`; that is a display-only truthfulness blocker and must become source-unavailable copy.

## Execution Plan and stop-loss lineage

Existing market-structure boundary extraction and rule path create the Candidate stop (`ExecutionPlanCandidateDO.stopLoss`); the validated Final persists it as `ExecutionPlanDO.stopLoss`. `DashboardHomeServiceImpl.buildExecutionSuggestion` requires exact analysis/decision/plan identity, Final status, rule PASS, complete boundaries, valid lifecycle, and then transports the persisted stop to both `stopZone` and compatibility `stopLoss`. `home-runtime.js` displays `stopZone || stopLoss` and does not derive stop from invalidation text. Missing stop blocks the whole Final projection (`BOUNDARY_INCOMPLETE`) and no fixture value is permitted in normal/prod mode. Result: PASS, no new stop algorithm required.

## Position lifecycle and close-path lineage

`UserPosition` is created explicitly with a source (`SYSTEM_PLAN_POSITION` with Final identity or `MANUAL_INDEPENDENT`). `DashboardHomeServiceImpl.buildPositions` keeps persistent entry facts, calls the monitor read path, and only `applyTrustedMonitor` exposes mark price, PnL, risk, conclusion, and action when the result is VERIFIED + fresh and complete. `applyWaitingMonitor` clears all such fields. The Home renderer repeats one trust-state message and keeps entry/opened facts. The row geometry is 22/28/28/22. Manual close is user-operated through `/positions/{id}` -> O07 -> `POST /api/user-positions/{id}/manual-close`; it records closure and does not claim broker execution. Result: PASS.

## Top6 binding

`OpportunityPriorityRankingService.rankForHome` is the producer; `buildRankedAssets` deduplicates asset IDs and symbols and carries opportunity score, confidence/risk source decision, timeframe fields, ranking reason and per-asset Final projection. `applyCardFinalProjection` requires an exact validated visible Final. `renderOpportunities` defensively filters eligibility, deduplicates, limits to six, and never pads. Unselected cards do not borrow the selected plan. Result: PASS.

## System Status ownership

The view has the correct six display slots and no opportunity count. Account status is derived from recorded active positions. However, `buildSystemState` still produces environment, risk, and data quality from the selected decision, making asset scope look like system scope. Service display uses only the header AI status and can hide partial provider availability. Hot Reset lacks explicit scope in compact display. Result: BLOCKER; fix only the projection and existing compact bindings.

## Fixtures and production guard

`UiReviewDashboardHomeService`, `UiReviewAssetPoolService`, and the workspace plan fixture are `@Profile("ui-review")`. `UiReviewRuntimeGuard` rejects prod and requires `trade-model.ui-review.enabled=true`. `UiReviewDashboardHomeServiceTest` verifies the bean does not exist without the profile; `UiReviewRuntimeGuardTest` verifies the guard. Normal/prod do not receive controlled fixture data. Result before rerun: source-level PASS; runtime rerun required after patch.

## Current-stage blockers before implementation

| ID | Frozen clause | Producer/binding defect | Bounded correction |
|---|---|---|---|
| N-01 | status objects have distinct scope | selected decision projected as macro/system/global data | aggregate environment/risk/data and combine provider/AI availability; render explicit Hot Reset state/scope |
| N-02 | GPT primary = bias/state/mode | `candidate.summary` is primary | render the exact three values and Candidate-not-Final label |
| N-03 | Gemini collection ownership | arrays and collection states concatenated | render three independent list/state groups |
| N-04 | Grok primary and authority | challenge summary primary; plan impact shown as authority | make `failurePathState` primary and show direction challenge/observation without plan mutation semantics |
| N-05 | unknown enum fail-closed | unknown uppercase -> `当前不可查看` | unknown enum -> `—`; reserve unavailable copy for actual role/data state |
| N-06 | provider source truth | missing source -> hardcoded `CoinGlass v4` | use explicit source only, otherwise `来源不可用`; keep freshness/status visible |
| N-07 | frozen navigation naming | `AI分析` | change visible/ARIA copy to `AI 分析` only |

## P1 registered gaps (not implemented)

1. `/positions` remains part of the shared `workspace.html` architecture; independent IA/density warrants a later P1 review.
2. `/analysis` Preview vs Opportunity and detail density remain a P1 acceptance item.
3. `/messages` grouping and target routing remain a P1 acceptance item; Telegram is excluded.
4. `/me` left-anchor/right-form density remains a P1 acceptance item.
5. Shared focused-detail shell quality for route-specific tasks remains a P1 acceptance item.

## Post-remediation validation

All seven bounded P0 findings are closed without changing the product contract,
schema, AI authority, Position lifecycle, Figma, Mobile, authentication, or
Telegram behavior.

| Gate | Result | Evidence |
|---|---|---|
| N-01 status ownership | PASS | aggregate system risk/global quality, BTC macro environment, combined provider/AI service state; selected-asset isolation test |
| N-02 GPT primary | PASS | runtime first visual is `GPT Candidate · 非 Final` plus Market Bias / Opportunity State / Candidate Mode |
| N-03 Gemini collections | PASS | three separate runtime sections and independent collection states |
| N-04 Grok authority | PASS | `failurePathState` is primary; no Grok plan-mode mutation field is rendered |
| N-05 unknown enum | PASS | shared and Home mappers return `—`; actual role/data unavailability remains explicit |
| N-06 provider truth | PASS | source comes from the snapshot provider; missing source renders `来源不可用` |
| N-07 navigation copy | PASS | Home and shared workspace both expose `AI 分析` |

Runtime verification used the standard Java 17 release JAR twice. Normal mode
returned authenticated `/dashboard` and `/api/dashboard/home` HTTP 200 with no
UI-review marker. The isolated `ui-review` profile produced the controlled
visual states only after its explicit profile and runtime guard were enabled.

Browser evidence at 1440, 1280, and 1080 reports document overflow 0, clipped
leaf text 0, visible raw enum count 0, and console error/warning count 0. Four
untrusted Position scenarios each retain entry/open-time facts, render exactly
one trust-state message, and omit mark price, PnL, judgment, conclusion, and
action. Screenshots are stored in
`docs/evidence/global_semantic_runtime_audit/`.

Validation results: Java 17 compile PASS; directed tests PASS; full Maven
`4719` tests, `0` failures, `0` errors, `14` skipped; Product Source Gate PASS;
Workflow Contract PASS; JavaScript syntax PASS; `git diff --check` PASS. The
14 skips are the repository's Docker-unavailable Testcontainers behavior and
did not hide a Maven failure.

GLOBAL_AUDIT_BLOCKERS: 0
CURRENT_PHASE_DONE: YES (this bounded audit/remediation package only)
