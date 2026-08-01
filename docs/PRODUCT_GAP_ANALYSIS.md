# Trade Model V1 Product Gap Analysis

Status: `P0_PRODUCT_BASELINE_FREEZE_CANDIDATE`

This analysis compares the formal product requirement with the baseline at main `2552dd24b1b756d5eb517e640baa772e1c5bcab6`. Priorities describe product impact, not permission to begin implementation. Authority: `docs/PRODUCT_SOURCE_OF_TRUTH.md`.

## GAP-01 Home Overall Alignment

**MODULE:** Home

**PRODUCT REQUIREMENT:** Final five-tab Home; fixed module order; asset card changes context without default detail navigation; plan and three-AI summary follow the selected asset; Top3 owner positions; distinct Loading/Empty/Error/Partial/Missing.

**CURRENT IMPLEMENTATION:** Desktop/mobile shells and `GET /api/dashboard/home` provide substantial state, asset, plan, AI, and position projections. Contract tests cover several interactions and fail-closed paths.

**GAP:** No single real-data acceptance demonstrates the final information order, all context linkages, all five states, responsive screenshots, and iPhone behavior. Current UI also contains legacy/diagnostic surface area beyond the concise product Home.

**USER IMPACT:** Users cannot yet trust that changing asset context updates every relevant module without stale or mixed data.

**REAL_DATA_STATUS:** PARTIAL

**DESIGN_STATUS:** PARTIAL

**SEMANTIC_STATUS:** PARTIAL

**PRIORITY:** P1

**BLOCKS_USABLE_VERSION:** YES

**NEXT DELIVERY PACKAGE:** P1 Home Alignment readiness and exact gap audit, followed by scoped implementation only after approval.

## GAP-02 Focus Asset Meaning and Provenance

**MODULE:** Focus Assets

**PRODUCT REQUIREMENT:** Each card presents real price/time, market bias, confidence, risk, AssetState, worth-opening, plan mode, data quality, multi-timeframe convergence, and concise support/opposition evidence with exact analysis identity.

**CURRENT IMPLEMENTATION:** Home assets expose many of these fields and an `analysisId`; selection uses symbol context and can link to Analysis Detail.

**GAP:** Complete source trace and calibrated semantics across every field are not proven together. Multi-timeframe and evidence summaries appear incomplete or detail-dependent. Percentage-like confidence must not be invented from labels or examples.

**USER IMPACT:** A card can look decisive while a source, freshness, or semantic dimension is incomplete.

**REAL_DATA_STATUS:** PARTIAL

**DESIGN_STATUS:** PARTIAL

**SEMANTIC_STATUS:** PARTIAL

**PRIORITY:** P1

**BLOCKS_USABLE_VERSION:** YES

**NEXT DELIVERY PACKAGE:** P1 Focus Asset field-source and interaction alignment.

## GAP-03 Execution Plan Completeness

**MODULE:** Execution Plan

**PRODUCT REQUIREMENT:** Exact versioned plan with rule-led direction, worth-opening, entry, stop, targets, add/reduce/abandon/invalidation, leverage, position size, validity/revalidation, and source trace; advisory only.

**CURRENT IMPLEMENTATION:** A mature plan domain, `ExecutionPlanVO`, generation services, safety boundaries, and Home presentation exist.

**GAP:** The product needs an exact read/display trace from evidence and rule version to the visible plan, with stale/expired/revalidation behavior and a real market scenario. Generation endpoints do not by themselves prove the user-facing plan.

**USER IMPACT:** Users may not be able to audit why a plan is current, expired, or invalidated.

**REAL_DATA_STATUS:** PARTIAL

**DESIGN_STATUS:** PARTIAL

**SEMANTIC_STATUS:** PASS

**PRIORITY:** P1

**BLOCKS_USABLE_VERSION:** YES

**NEXT DELIVERY PACKAGE:** P1 Home plan projection and exact source-trace acceptance.

## GAP-04 Three-AI Evidence and Authority

**MODULE:** AI Analysis / Three AI

**PRODUCT REQUIREMENT:** Rule base first; checkpoint-triggered GPT Final, Gemini Review, Grok Challenge over one traceable evidence package; four conflict levels; fallback; no vote or trade authorization.

**CURRENT IMPLEMENTATION:** Three fixed role summaries, `resultAvailable` hard gate, Analysis Detail reuse, analysis/read traces, and fallback-oriented contracts exist.

**GAP:** Real model invocation with one immutable evidence package, actual model metadata, calibrated conflict behavior, and failure fallback have not passed end-to-end real-data validation. Some role/deep fields remain partial.

**USER IMPACT:** Users cannot yet assess whether summaries are grounded in the same evidence or correctly subordinate to the rule base.

**REAL_DATA_STATUS:** PARTIAL

**DESIGN_STATUS:** PARTIAL

**SEMANTIC_STATUS:** PARTIAL

**PRIORITY:** P3

**BLOCKS_USABLE_VERSION:** YES

**NEXT DELIVERY PACKAGE:** P3 AI Analysis real-input and role-boundary integration.

## GAP-04A Eight-Score Real Evidence Chain

**MODULE:** Evidence / Eight Scores / Home Confidence

**PRODUCT REQUIREMENT:** Real raw market/event/macro data must become standardized evidence before scoring rules/models produce the eight named scores. Each score needs traceable inputs, freshness, exact analysis snapshot, calibration evidence, and fail-closed behavior before it can support formal Home confidence.

**CURRENT IMPLEMENTATION:** `GET /api/score/list` constructs a `MarketEnvironmentVO` from `symbol` and `timeframe`, then calls `ScoreServiceImpl.buildScoreListFromEnvironment`. That path supplies an empty `AssetAnalysisVO` and computes all eight dimensions with fixed base/default values and light-rule adjustments.

**GAP:** The endpoint is `PARTIAL / FUNCTIONAL_UNVALIDATED` with `FALLBACK_PRESENT`. Complete per-dimension real evidence, persisted exact-analysis linkage, real-market calibration, and one accepted end-to-end scenario are not proven. The endpoint's existence cannot establish an evidence-driven complete score product, and its numeric output is not authorized as formal Home confidence.

**USER IMPACT:** A plausible score can appear more certain than its inputs justify, causing users to over-trust Home or AI summaries.

**REAL_DATA_STATUS:** PARTIAL / FALLBACK_PRESENT

**DESIGN_STATUS:** PARTIAL

**SEMANTIC_STATUS:** PARTIAL

**PRIORITY:** P1

**BLOCKS_USABLE_VERSION:** YES

**NEXT DELIVERY PACKAGE:** P1A Home Alignment Readiness and Gap Audit must trace every visible score/confidence field; scoring implementation remains unauthorized until that audit is reviewed and merged-main authorization exists.

## GAP-05 Manual UserPosition Workflow

**MODULE:** Positions

**PRODUCT REQUIREMENT:** Authenticated user-entered actual direction, entry, time, size, leverage, stop/target, notes, and optional exact original-plan link; OPEN/PARTIALLY_CLOSED/CLOSED lifecycle.

**CURRENT IMPLEMENTATION:** Manual-open/manual-close and exact/open owner-scoped reads exist with a web position surface and string-safe IDs.

**GAP:** Full user flow, partial-close semantics, plan-versus-actual presentation, validation copy, mobile form ergonomics, and a real user scenario are not accepted.

**USER IMPACT:** Users may be unable to accurately record and review the position facts the monitor depends on.

**REAL_DATA_STATUS:** FUNCTIONAL_UNVALIDATED

**DESIGN_STATUS:** PARTIAL

**SEMANTIC_STATUS:** PASS

**PRIORITY:** P2

**BLOCKS_USABLE_VERSION:** YES

**NEXT DELIVERY PACKAGE:** P2 UserPosition real workflow and position-detail alignment.

## GAP-06 Position Monitor Real-Market Behavior

**MODULE:** Position Monitoring

**PRODUCT REQUIREMENT:** Continuously compare original plan and actual user facts against current evidence; classify logic, reversal, stop/target distance, size/leverage/liquidity/wick risk; alert and advise manually; log every result.

**CURRENT IMPLEMENTATION:** Owner-scoped reads, logs, exact string IDs, authoritative latest state resolver, logic/risk output, and fail-closed contracts exist. Explicit run endpoints exist but read-only UI is forbidden from invoking them automatically.

**GAP:** No accepted real/historical scenario shows price movement causing the correct monitor state, wick filtering, weak/strong reversal, liquidity/account risk, alert timeliness, and suggestion quality. Scheduling/refresh ownership needs product verification.

**USER IMPACT:** The most safety-relevant feature is not yet proven under realistic movement and failure.

**REAL_DATA_STATUS:** PARTIAL

**DESIGN_STATUS:** PARTIAL

**SEMANTIC_STATUS:** PASS

**PRIORITY:** P2

**BLOCKS_USABLE_VERSION:** YES

**NEXT DELIVERY PACKAGE:** P2 PositionMonitor real-price/replay scenario package.

## GAP-07 Messages Are Backend-First

**MODULE:** Message Center and Push Detail

**PRODUCT REQUIREMENT:** User-visible list and detail for only OPPORTUNITY and POSITION_RISK, with exact identities, source-specific privacy, original snapshot, current Recheck/monitor state, change reason, and five-state behavior.

**CURRENT IMPLEMENTATION:** Read-only list/detail APIs and server-side OPPORTUNITY public/POSITION_RISK private projections exist with state and security tests.

**GAP:** Product UI, final Figma alignment, real interactions, screenshots, and real message scenarios are not implemented/accepted. Telegram and external sends remain absent by design.

**USER IMPACT:** Users cannot yet consume the contract foundation as a complete Message Center experience.

**REAL_DATA_STATUS:** PARTIAL

**DESIGN_STATUS:** PARTIAL

**SEMANTIC_STATUS:** PASS

**PRIORITY:** P5

**BLOCKS_USABLE_VERSION:** NO

**NEXT DELIVERY PACKAGE:** P5 Message/Push UI readiness and first product implementation.

## GAP-08 Detail Page Coverage

**MODULE:** Analysis Detail, Position Detail, Execution Plan Detail, Replay Detail

**PRODUCT REQUIREMENT:** Exact identities; deep evidence/scores/timeframes; user/plan/monitor separation; source-specific Push detail; traceable replay.

**CURRENT IMPLEMENTATION:** Analysis Detail and asset/position/review read foundations exist. Some details are embedded in Home or operational dashboards.

**GAP:** The four product detail flows are not one coherent, design-aligned, exact-identity set with real screenshots and failure paths. Execution Plan and Replay details are especially incomplete as product pages.

**USER IMPACT:** Users cannot reliably move from concise summary to full evidence and back without context loss.

**REAL_DATA_STATUS:** PARTIAL

**DESIGN_STATUS:** PARTIAL

**SEMANTIC_STATUS:** PARTIAL

**PRIORITY:** P4

**BLOCKS_USABLE_VERSION:** YES

**NEXT DELIVERY PACKAGE:** P4 Detail Pages.

## GAP-09 Review Closed Loop

**MODULE:** Review

**PRODUCT REQUIREMENT:** Join original evidence, decision, plan, user action, monitor history, Recheck, actual result, feedback, and rule version; classify executed and missed outcomes.

**CURRENT IMPLEMENTATION:** Review aggregate/detail/summary/state/log endpoints and classification foundations are present.

**GAP:** No accepted real closed-position or missed-opportunity archive proves the closed loop or safe human-reviewed rule iteration.

**USER IMPACT:** The system cannot yet demonstrate that recommendations improve from verifiable outcomes.

**REAL_DATA_STATUS:** PARTIAL

**DESIGN_STATUS:** MISSING

**SEMANTIC_STATUS:** PASS

**PRIORITY:** P4

**BLOCKS_USABLE_VERSION:** NO

**NEXT DELIVERY PACKAGE:** P4 Replay Detail followed by P10 outcome validation.

## GAP-10 My and Settings Contract

**MODULE:** My / Settings

**PRODUCT REQUIREMENT:** Only real supported account/session/system information and logout; unsupported preferences are hidden or unavailable; no invented community, referral, paid-plan, exchange order, or auto-trading modules.

**CURRENT IMPLEMENTATION:** Shell/profile design node and authentication/logout foundations exist.

**GAP:** A formal real-field contract, backing API/data, final interactions, and device acceptance are missing.

**USER IMPACT:** A polished but unsupported profile/settings page would misrepresent capability.

**REAL_DATA_STATUS:** MISSING

**DESIGN_STATUS:** PARTIAL

**SEMANTIC_STATUS:** PARTIAL

**PRIORITY:** P6

**BLOCKS_USABLE_VERSION:** NO

**NEXT DELIVERY PACKAGE:** P6 My and Settings source audit before implementation.

## GAP-11 iPhone Usability

**MODULE:** iPhone

**PRODUCT REQUIREMENT:** Five-tab usable iPhone experience with exact identities, Session/Cookie/CSRF, Dynamic Type, safe areas, touch targets, navigation, errors, and real installation.

**CURRENT IMPLEMENTATION:** A merged Xcode project under `ios/TradeModelApp` provides a SwiftUI/WKWebView client, environment-based server configuration, Session/Cookie/CSRF compatibility foundations, native fail-closed states, Debug and Release simulator builds, 47 unit/security/project tests, one UI launch test, and a passing iPhone 17 Pro Simulator install/launch check.

**GAP:** The remaining gap is real-device and production validation: physical iPhone install, login/logout/session expiry and persistence, Cookie/CSRF behavior over an authorized server, background/foreground recovery, real network conditions, push permission if later authorized, final 375/390/430-point interaction/accessibility acceptance, long-term personal use, and an approved TestFlight/installation route where applicable.

**USER IMPACT:** The product cannot yet be considered an iPhone application or reliably usable on a real device.

**REAL_DATA_STATUS:** PARTIAL

**DESIGN_STATUS:** PARTIAL

**SEMANTIC_STATUS:** PARTIAL

**PRIORITY:** P9

**BLOCKS_USABLE_VERSION:** YES

**NEXT DELIVERY PACKAGE:** P9 iPhone Usable Version validates and completes the existing simulator-level foundation after full web-product integration and server readiness; it does not restart from a nonexistent-Xcode assumption.

## GAP-12 Production Data

**MODULE:** Market Data, Evidence, Data Quality

**PRODUCT REQUIREMENT:** Sustained real multi-source market/event/macro data, raw evidence, standardized evidence, freshness/completeness, data-quality scoring, and traceable snapshots.

**CURRENT IMPLEMENTATION:** Provider adapters, evidence/scoring pipelines, source health, and real-data status foundations exist.

**GAP:** Sustained production coverage, provider failure/degradation, clock alignment, retention, and one complete evidence package across eight scores and four timeframes are not accepted.

**USER IMPACT:** Every downstream plan, AI summary, monitor, and message can be weakened by unseen source gaps.

**REAL_DATA_STATUS:** PARTIAL

**DESIGN_STATUS:** N/A

**SEMANTIC_STATUS:** PARTIAL

**PRIORITY:** P0

**BLOCKS_USABLE_VERSION:** YES

**NEXT DELIVERY PACKAGE:** P1/P3/P7 real-data mapping and P8 production source validation.

## GAP-13 Server Deployment

**MODULE:** Server

**PRODUCT REQUIREMENT:** Production configuration, HTTPS, database, secrets, real providers, migrations, logs, monitoring, backup/rollback, and verified runtime.

**CURRENT IMPLEMENTATION:** Spring Boot application, database/migration foundations, health/status surfaces, and extensive tests exist.

**GAP:** No deployment evidence establishes production-grade configuration, secret rotation, migration rehearsal, HTTPS/session policy, rollback, or long-running stability.

**USER IMPACT:** A locally functional system cannot be safely relied on as a service.

**REAL_DATA_STATUS:** PARTIAL

**DESIGN_STATUS:** N/A

**SEMANTIC_STATUS:** PASS

**PRIORITY:** P8

**BLOCKS_USABLE_VERSION:** YES

**NEXT DELIVERY PACKAGE:** P8 Server Deployment.

## GAP-14 Observability and Recovery

**MODULE:** Observability / System State

**PRODUCT REQUIREMENT:** Actionable source/service/decision/AI/monitor health, logs, alerts, trace IDs, SLOs, retention, incident response, and recovery proof.

**CURRENT IMPLEMENTATION:** Numerous health, status, trace, and operational read surfaces exist.

**GAP:** Operational signals are fragmented; no production SLO, alert routing, retention, incident drill, or user-facing degradation acceptance is established.

**USER IMPACT:** Failures may be visible in code or diagnostics but not reliably detected, explained, and recovered in operation.

**REAL_DATA_STATUS:** PARTIAL

**DESIGN_STATUS:** N/A

**SEMANTIC_STATUS:** PARTIAL

**PRIORITY:** P8

**BLOCKS_USABLE_VERSION:** YES

**NEXT DELIVERY PACKAGE:** P8 observability and recovery acceptance.

## 15. Highest-Impact Sequence

1. Preserve this Product Source baseline and require source mapping before edits.
2. Run P1A as a read-only Home alignment and gap audit, including exact eight-score/confidence provenance and explicit real/derived/fallback labels; authorize implementation only through a later merged-main decision.
3. Validate real UserPosition and PositionMonitor behavior under historical/real movement.
4. Complete deep evidence/detail and AI integration.
5. Build Message/Push UI only on the existing public/private contract.
6. Integrate and deploy safely, then validate the existing iPhone simulator foundation on real hardware, a real server, and real-world operation.
