# V1 Current State

This file is a source-of-truth summary. Completion is based only on merged `main`.

## Current Main

- Source branch baseline: `main`
- Current merged main: `67b06b1 BACKEND-P340 MultiTimeframeContext Source Binding Ability Closure (#808)`
- Evidence / Score Mainline has completed through `24e120b BACKEND-P295 Review-Only Scan Output to Evidence / Score Entry Slice (#721)`.
- Workflow automation also includes `2efdd6b BACKEND-P291G Workflow Auto-Decision Runner Pack (#723)`, `58f69ef BACKEND-P291F Active Mainline Status Refresh Pack (#719)`, and `ba9cd2c BACKEND-P291E Workflow One-Command Runner Pack (#717)`.
- Market Read Mainline has completed through `a61a86b BACKEND-P294 Review-Only MarketRead Output and Scan Output Slice (#713)`.
- Evidence / Score Mainline has completed a review-only entry envelope through P295, review-only evidence normalization through P296, review-only score input / precheck through P297, and review-only score assembly through P298.
- Candidate / Push Mainline has completed review-only score-to-candidate handoff through P299, review-only candidate attention through P300, review-only candidate preview / ranking guard through P301, review-only internal push preview / recheck handoff through P302, push preview closure before external channel through P303, dashboard / internal push preview display gate through P304, and Candidate / Push review-only MVP closure through P305.
- Current active mainline is machine-readable in `docs/ACTIVE_MAINLINE_STATUS.yml`.
- Current open business-chain package is branch `p341`: `P341 RiskActionGuard Source Binding Ability Closure`.
- P306 is completed on main as Readiness / Point Boundary Planning Gate. It defines the boundary before Readiness, point proposal, external channel, and execution-adjacent work.
- P307 is completed on main as Review-only Readiness Gate Skeleton.
- P308 is completed on main as Review-only Point Boundary / Proposal Gate.
- P309 is completed on main as Source-owned Review-only Point Proposal Skeleton.
- P310 is completed on main as Point Proposal Closure / Dashboard Display Gate.
- P311 is completed on main as Executable Point Generation Pre-Approval Plan.
- P312 is completed on main as Source-owned Numeric Point Proposal Plan.
- P313 is completed on main as SourceTrace Numeric Point Contract Plan.
- P314 is completed on main as RuntimeKlineContext Numeric Point Contract Plan.
- P315 is completed on main as DataQuality Numeric Point Contract Plan.
- P316 is completed on main as MultiTimeframe Numeric Point Contract Plan.
- P317 is completed on main as Risk Action Guard Numeric Point Contract Plan.
- P318 is completed on main as Numeric Point Safety Validator Plan.
- P319 is completed on main as Numeric Point Fixture Matrix Plan.
- P320 is completed on main as ReviewOnlyNumericPointProposalDTO Java Skeleton.
- P321 is completed on main as Numeric Point Safety Validator Java Skeleton.
- P322 is completed on main as ReviewOnly Numeric Point Assembler Java Skeleton.
- P323 is completed on main as Source-owned Numeric Point Candidate Assembler Plan.
- P324 is completed on main as Source-owned Numeric Point Candidate Assembler Java Skeleton.
- P325 is completed on main as Source-owned Numeric Point Candidate Assembler Verification.
- P326 is completed on main as Source Context Integration Plan.
- P327 is completed on main as SourceTrace Numeric Source Read Model Plan.
- P328 is completed on main as SourceTraceNumericSourceContextDTO Java Skeleton.
- P329 is completed on main as SourceTraceNumericSourceReadModelValidator Java Skeleton.
- P330 is completed on main as SourceTrace Numeric Source Validator Verification.
- P331 is completed on main as SourceTraceNumericSourceReadModelAssembler Java Skeleton.
- P332 is completed on main as SourceTrace Numeric Source Assembler Verification.
- P333 is completed on main as SourceTrace Runtime / Source Binding Plan.
- P334 is completed on main as SourceTrace Runtime / Source Binding Verification.
- P335 is completed on main as RuntimeKlineContext Source Binding Plan.
- WORKFLOW-P336A is completed on main as GitHub Auth And Handoff Rule.
- P336 is completed on main as RuntimeKlineContextSourceBindingDTO Java Skeleton.
- WORKFLOW-P336C is completed on main as `v1-merge-sync.sh` already-merged PR sync fallback.
- WORKFLOW-P336B-R2 is completed on main as Workflow Command Automation Retry.
- P337 is completed on main as RuntimeKlineContextSourceBindingValidator Java Skeleton.
- P338 is completed on main as RuntimeKlineContextSourceBindingAssembler And Verification.
- P339 is completed on main as DataQualityContext Source Binding Ability Closure.
- P340 is completed on main as MultiTimeframeContext Source Binding Ability Closure.
- P341 is the active RiskActionGuard Source Binding Ability Closure package.
- Candidate / Push review-only MVP is completed to dashboard / internal preview display only.
- Readiness remains non-executable and review-only only.
- Point proposal remains non-executable and review-only only.
- Entry / stop / TP / RR are nullable, incomplete-safe proposal fields only; they are not executable trading instructions.
- External Channel is not authorized.
- P311 defines the pre-approval boundary before any future source-owned numeric point proposal or executable point-generation-adjacent work.
- P312 defines the future object boundary and field families for source-owned review-only numeric point proposals.
- P313 defines the future numeric point SourceTrace contract for entry / stop / TP / RR values.
- P314 defines the future RuntimeKlineContext contract for runtime kline windows, OHLCV completeness, latest price / close boundaries, wick / pin-bar, liquidity, stampede, multi-timeframe, event, abnormal data, and Risk Action Guard references.
- P315 defines the future DataQuality contract for numeric point scores, thresholds, source trace quality, runtime kline quality, OHLCV completeness, freshness, liquidity, stampede, wick, event, abnormal data, multi-timeframe consistency, and Risk Action Guard references.
- P316 defines the future MultiTimeframe contract for 4h / 1h / 15m / 5m roles, entry / stop / TP / RR timeframe confirmation, high-timeframe conflicts, low-timeframe noise, wick-only signals, strong reversal, and Risk Action Guard references.
- P317 defines the future Risk Action Guard numeric point contract for high-risk, liquidity-degraded, stampede-confirmed, wick-only, strong reversal, high-timeframe conflict, entry / stop / TP / RR review, and fail-closed behavior.
- P318 defines the future Numeric Point Safety Validator plan for checking SourceTrace, RuntimeKlineContext, DataQuality, MultiTimeframe, Risk Action Guard, Watchlist Pool proof, safety flags, forbidden semantics, partial candidates, incomplete states, and fail-closed states.
- P319 defines the future Numeric Point Fixture Matrix plan for positive, incomplete, fail-closed, degraded, partial, forbidden-semantics, Watchlist / Display Slots, external channel, order / execution / auto-trading, and cross-contract consistency fixture categories.
- P320 added only a plain Java DTO skeleton and targeted DTO tests.
- P321 added only a plain Java validator skeleton and targeted validator tests.
- P322 added only a plain Java assembler skeleton and targeted assembler tests.
- P323 added only a docs-only plan for future source-owned numeric point candidate assembly.
- P324 added only a plain Java source-owned candidate assembler skeleton and targeted tests.
- P325 verified P320-P324 as a review-only numeric candidate chain with DTO, safety validator, explicit assembler, and source-owned candidate assembler.
- P326 defined how future real SourceTrace, RuntimeKlineContext, DataQuality, MultiTimeframe, RiskActionGuard, WatchlistPoolProof, and source-owned point contexts may be integrated.
- P327 defines the future SourceTraceNumericSourceContext read model for entry / stop / TP / RR numeric source identity.
- P328 adds only a plain Java `SourceTraceNumericSourceContextDTO` skeleton and targeted DTO tests.
- P329 adds only a plain Java `SourceTraceNumericSourceReadModelValidator` skeleton and targeted validator tests.
- P330 verifies P327-P329 as a SourceTrace numeric source read model plan + DTO carrier + validator skeleton chain.
- P330 does not add Java, tests, SourceTrace assembler, source-owned candidate assembly, source context integration, RuntimeKlineContext wiring, DataQuality wiring, MultiTimeframe wiring, RiskActionGuard wiring, WatchlistPoolProof wiring, service wiring, dashboard runtime integration, external channel, order, execution, or auto-trading.
- P330 does not mean SourceTrace read model assembly is complete, source context is connected, real point generation is complete, or entry / stop / TP / RR are usable as trading output.
- P331 adds only a plain Java SourceTrace read-model assembler skeleton and targeted assembler tests.
- P331 only moves explicit SourceTrace read-model input into `SourceTraceNumericSourceContextDTO` and immediately calls `SourceTraceNumericSourceReadModelValidator`.
- P331 does not read real market data, read real SourceTrace runtime, connect RuntimeKlineContext, connect DataQualityContext, connect MultiTimeframeContext, connect RiskActionGuardContext, connect WatchlistPoolProof, assemble source-owned candidates from live contexts, generate real entry / stop / TP / RR, connect dashboard runtime, send externally, place orders, execute, or auto-trade.
- P332 verifies P331 as a docs-only closure package.
- P332 confirms the SourceTrace numeric source read-model assembler only moves explicit `AssemblyInput` fields into `SourceTraceNumericSourceContextDTO`, calls `SourceTraceNumericSourceReadModelValidator`, and returns both context and validation result.
- P332 does not add Java, tests, SourceTrace runtime reads, source context integration, RuntimeKlineContext runtime, DataQuality runtime, MultiTimeframe runtime, RiskActionGuard runtime, WatchlistPoolProof, dashboard runtime, external channel, order, execution, or auto-trading.
- P333 is a docs-only SourceTrace Runtime / Source Binding Plan skeleton.
- P333 defines the maximum safe planning boundary before any future source binding work may be considered.
- P333 does not add Java, tests, SourceTrace runtime reads, source context integration, RuntimeKlineContext runtime, DataQuality runtime, MultiTimeframe runtime, RiskActionGuard runtime, WatchlistPoolProof, market data, latest price, latest close, external provider reads, dashboard runtime, external channel, order, execution, or auto-trading.
- P334 verifies P333 as a docs-only SourceTrace Runtime / Source Binding Plan.
- P334 does not add Java, tests, SourceTrace runtime reads, source context integration, RuntimeKlineContext runtime, DataQuality runtime, MultiTimeframe runtime, RiskActionGuard runtime, WatchlistPoolProof, market data, latest price, latest close, external provider reads, service wiring, dashboard runtime, external channel, Push wiring, order, execution, or auto-trading.
- P334 confirms there is no real entry / stop / TP / RR generation and no RR generation.
- P335 defines only the future RuntimeKlineContext Source Binding Plan.
- P335 does not add Java, tests, RuntimeKlineContext runtime wiring, DataQualityContext, MultiTimeframeContext, RiskActionGuardContext, WatchlistPoolProof, market data reads, latest price reads, latest close reads, external provider reads, service wiring, dashboard runtime, external channel, Push wiring, order, execution, auto-trading, real entry, real stop, real TP, or RR generation.
- WORKFLOW-P336A adds only workflow documentation for GitHub connector / Codex GitHub auth / local `gh` handoff, completion-state rules, A/B/C merge authorization, no-skip rules, main-clean auto-next rules, token exposure handling, and new-window continuation.
- WORKFLOW-P336A does not change P335 business content, business capability, Java, tests, runtime wiring, dashboard, external channel, Push, order, execution, or auto-trading.
- P336 adds only a plain Java `RuntimeKlineContextSourceBindingDTO` skeleton and targeted DTO tests.
- P336 does not add validator, assembler, service, controller, mapper, repository, scheduler, resources, schema, config, pom, dashboard, MarketQuoteClient, RuntimeKlineContext real reads, DataQualityContext, MultiTimeframeContext, RiskActionGuardContext, external channel, Push, order, execution, auto-trading, real entry, real stop, real TP, or RR generation.
- WORKFLOW-P336C modifies only workflow script / workflow docs so `scripts/v1-merge-sync.sh` can sync local main when a PR is already merged.
- WORKFLOW-P336C does not change P336 business content, RuntimeKlineContext business content, SourceTrace business content, business Java, business tests, runtime wiring, dashboard, external channel, Push, order, execution, or auto-trading.
- P337 adds only a plain Java `RuntimeKlineContextSourceBindingValidator` skeleton and targeted validator tests.
- P337 does not add assembler, service, controller, mapper, repository, scheduler, resources, schema, config, pom, dashboard, MarketQuoteClient, RuntimeKlineContext real reads, DataQualityContext, MultiTimeframeContext, RiskActionGuardContext, external channel, Push, order, execution, auto-trading, real entry, real stop, real TP, or RR generation.
- P338 adds only a plain Java `RuntimeKlineContextSourceBindingAssembler` skeleton, targeted assembler tests, and verification docs for P335-P338.
- P338 does not add service, controller, mapper, repository, scheduler, resources, schema, config, pom, dashboard, MarketQuoteClient, RuntimeKlineContext real reads, DataQualityContext runtime, MultiTimeframeContext runtime, RiskActionGuardContext runtime, WatchlistPoolProof runtime, source-owned candidate integration, internal preview, external channel, Push, order, execution, auto-trading, real entry, real stop, real TP, or RR generation.
- P339 adds only DataQualityContext source-binding plan, DTO, validator, assembler, targeted tests, and verification docs.
- P339 does not add service, controller, mapper, repository, scheduler, resources, schema, config, pom, dashboard, MarketQuoteClient, DataQuality runtime, MultiTimeframe runtime, RiskActionGuard runtime, WatchlistPoolProof runtime, source-owned candidate integration, internal preview, external channel, Push, order, execution, auto-trading, real entry, real stop, real TP, or RR generation.
- P340 adds only MultiTimeframeContext source-binding plan, DTO, validator, assembler, targeted tests, and verification docs.
- P340 does not add service, controller, mapper, repository, scheduler, resources, schema, config, pom, dashboard, MarketQuoteClient, MultiTimeframe runtime, RiskActionGuard runtime, WatchlistPoolProof runtime, source-owned candidate integration, internal preview, external channel, Push, order, execution, auto-trading, real entry, real stop, real TP, or RR generation.
- P341 adds only RiskActionGuard source-binding plan, DTO, validator, assembler, targeted tests, and verification docs.
- P341 does not add service, controller, mapper, repository, scheduler, resources, schema, config, pom, dashboard, MarketQuoteClient, RiskActionGuard runtime, WatchlistPoolProof runtime, source-owned candidate integration, internal preview, external channel, Push, order, execution, auto-trading, real entry, real stop, real TP, RR generation, final direction, or executable action output.
- Fixed next recommended business package after P341 is `WatchlistPoolProof Source Binding Ability Closure`, still not runtime wiring.

Default workflow is GPT + Codex + GitHub-native.
（默认工作流是 GPT + Codex + GitHub 原生。）

Terminal scripts are fallback only except local main sync after merge.
（终端脚本除合并后同步 main 外，只作为兜底。）

Codex must output the requested workflow result and stop.
（Codex 必须输出本包要求的工作流结果并停止。）

## Source-Of-Truth Rule

Only merged `main` counts as completed.

Open Issues, open PRs, local branches, draft PRs, and chat memory do not count as completed progress.

Progress must be read together with:

- `docs/WORKFLOW_GITHUB_AUTH_AND_HANDOFF_RULE.md`
- `docs/V1_PROGRESS_SOURCE_OF_TRUTH.md`
- `docs/V1_CAPABILITY_MATRIX.md`
- `docs/PROJECT_PROGRESS_INDEX.md`
- `docs/V1_MVP_REALITY_ROADMAP.md`
- `docs/ACTIVE_MAINLINE_STATUS.yml`
- `docs/SESSION_BOOTSTRAP.md`

If these sources disagree, merged `main` wins and the docs must be corrected.

## What P287-P340 Actually Completed

P287-P340 completed market-read request contract, DTO, validator, test-only wiring, review-only output, review-only scan output, review-only Evidence / Score entry, review-only evidence normalization, review-only score input / precheck, review-only score assembly, review-only candidate handoff, review-only candidate attention, review-only candidate preview / ranking guard, review-only internal push preview / recheck handoff skeleton, push preview closure before external channel, dashboard / internal push preview display gate, Candidate / Push review-only MVP closure, Readiness / Point boundary planning, review-only readiness gate skeleton, review-only point boundary gate skeleton, source-owned review-only point proposal skeleton, point proposal closure / display gate, executable point generation pre-approval plan, source-owned numeric point proposal plan, SourceTrace numeric point contract plan, RuntimeKlineContext numeric point contract plan, DataQuality numeric point contract plan, MultiTimeframe numeric point contract plan, Risk Action Guard numeric point contract plan, Numeric Point Safety Validator plan, Numeric Point Fixture Matrix plan, ReviewOnlyNumericPointProposalDTO Java skeleton, Numeric Point Safety Validator Java skeleton, ReviewOnly Numeric Point Assembler Java skeleton, Source-owned Numeric Point Candidate Assembler Plan, Source-owned Numeric Point Candidate Assembler Java Skeleton, Source-owned Numeric Point Candidate Assembler Verification, Source Context Integration Plan, SourceTrace Numeric Source Read Model Plan, SourceTraceNumericSourceContextDTO Java Skeleton, SourceTraceNumericSourceReadModelValidator Java Skeleton, SourceTrace Numeric Source Validator Verification, SourceTraceNumericSourceReadModelAssembler Java Skeleton, SourceTrace Numeric Source Assembler Verification, SourceTrace Runtime / Source Binding Plan, SourceTrace Runtime / Source Binding Verification, RuntimeKlineContext Source Binding Plan, RuntimeKlineContextSourceBindingDTO Java Skeleton, RuntimeKlineContextSourceBindingValidator Java Skeleton, RuntimeKlineContextSourceBindingAssembler And Verification, DataQualityContext Source Binding Ability Closure, and MultiTimeframeContext Source Binding Ability Closure only:

- P287: docs-only authorization gate for future `MarketReadRequestDTO`.
- P288: pure-data `MarketReadRequestDTO` skeleton plus targeted DTO test.
- P289: docs-only closure and authorization for future guard validator.
- P290: `MarketReadRequestGuardValidator` skeleton, validation result/status DTOs, and targeted validator test.
- P291A: workflow reset, progress source of truth, capability matrix, allowed review-only outputs, blocked capability registry, MVP reality roadmap, and drift guard checklist.
- P292: test-only `MarketReadRequestDTO` -> `MarketReadRequestGuardValidator` wiring and review-only validation output.
- P293: review-only output assembler from `MarketReadRequestDTO` + guard result to `MarketReadReviewOnlyOutputDTO`.
- P294: review-only scan output skeleton from `MarketReadReviewOnlyOutputDTO` to `MarketReadReviewOnlyScanOutputDTO`.
- P295: review-only Evidence / Score entry skeleton from `MarketReadReviewOnlyScanOutputDTO` to `ReviewOnlyEvidenceScoreEntryDTO`.
- P296: review-only evidence normalization skeleton from `ReviewOnlyEvidenceScoreEntryDTO` to `ReviewOnlyNormalizedEvidenceDTO`.
- P297: review-only score input / precheck skeleton from `ReviewOnlyNormalizedEvidenceDTO` to `ReviewOnlyScoreInputPrecheckDTO`.
- P298: review-only score assembly skeleton from `ReviewOnlyScoreInputPrecheckDTO` to `ReviewOnlyScoreAssemblyDTO`.
- P299: review-only candidate handoff skeleton from `ReviewOnlyScoreAssemblyDTO` to `ReviewOnlyCandidateHandoffDTO`.
- P300: review-only candidate attention skeleton from `ReviewOnlyCandidateHandoffDTO` to `ReviewOnlyCandidateAttentionDTO`.
- P301: review-only candidate preview / ranking guard skeleton from `ReviewOnlyCandidateAttentionDTO` to `ReviewOnlyCandidatePreviewGuardDTO`.
- P302: review-only internal push preview / recheck handoff skeleton from `ReviewOnlyCandidatePreviewGuardDTO` to `ReviewOnlyInternalPushPreviewDTO`.
- P303: push preview closure before external channel, with docs and targeted guard tests only.
- P304: dashboard / internal push preview display gate, with read-only status, external-channel-disabled copy, and targeted dashboard guard tests.
- P305: Candidate / Push review-only MVP closure, confirming P299-P304 form only an internal read-only preview loop.
- P306: Readiness / Point boundary planning gate, confirming readiness, points, external channel, and execution-adjacent work must remain blocked until separate review-only gates or authorization packages.
- P307: review-only readiness gate skeleton from `ReviewOnlyInternalPushPreviewDTO` to `ReviewOnlyReadinessGateDTO`.
- P308: review-only point boundary / proposal gate skeleton from `ReviewOnlyReadinessGateDTO` to `ReviewOnlyPointBoundaryGateDTO`.
- P309: source-owned review-only point proposal skeleton from `ReviewOnlyPointBoundaryGateDTO` to `ReviewOnlyPointProposalDTO`.
- P310: point proposal closure / display gate from `ReviewOnlyPointProposalDTO` to `ReviewOnlyPointProposalDisplayDTO`.
- P311: executable point generation pre-approval plan defining source trace, runtime kline context, data quality, multi-timeframe, Risk Action Guard, INCOMPLETE, and BLOCKED_FAIL_CLOSED boundaries.
- P312: source-owned numeric point proposal plan defining future review-only numeric point object boundaries, source trace metadata, nullable fields, INCOMPLETE rules, and BLOCKED_FAIL_CLOSED rules.
- P313: SourceTrace numeric point contract plan defining future entry / stop / TP / RR source trace fields, freshness states, fixture matrix expectations, and Risk Action Guard references.
- P314: RuntimeKlineContext numeric point contract plan defining future runtime kline context, OHLCV completeness, latest price / close boundaries, wick / pin-bar, liquidity, stampede, multi-timeframe, event, abnormal data, and Risk Action Guard references.
- P315: DataQuality numeric point contract plan defining future data quality score, hard / warning thresholds, SourceTrace quality, RuntimeKlineContext quality, OHLCV completeness, freshness, liquidity, stampede, wick, event, abnormal data, multi-timeframe consistency, and Risk Action Guard references.
- P316: MultiTimeframe numeric point contract plan defining future 4h / 1h / 15m / 5m roles, required timeframe presence, entry / stop / TP / RR timeframe confirmation, high-timeframe conflicts, low-timeframe noise, wick-only signals, strong reversal, fixture matrix expectations, and Risk Action Guard references.
- P317: Risk Action Guard numeric point contract plan defining future high-risk, liquidity-degraded, stampede-confirmed, wick-only, strong reversal, high-timeframe conflict, entry / stop / TP / RR review, fail-closed behavior, fixture matrix expectations, and Watchlist Pool / Display Slots boundary.
- P318: Numeric Point Safety Validator plan defining future validator inputs, output states, mandatory safety flags, SourceTrace / RuntimeKlineContext / DataQuality / MultiTimeframe / Risk Action Guard checks, entry / stop / TP / RR partial handling, forbidden semantics, and fixture matrix expectations.
- P319: Numeric Point Fixture Matrix plan defining future positive, incomplete, fail-closed, degraded, partial, forbidden-semantics, Watchlist / Display Slots, external-channel, order / execution / auto-trading, and cross-contract consistency fixture categories.
- P320: ReviewOnlyNumericPointProposalDTO Java skeleton defining a DTO-only, targeted-test-only carrier for future review-only numeric point proposal candidates with forced safety flags, nullable fields, incomplete-safe behavior, and fail-closed blocked behavior.
- P321: Numeric Point Safety Validator Java skeleton defining validator-only, targeted-test-only checks for DTO safety flags, required refs, required point-field presence, incomplete / degraded / fail-closed statuses, and forbidden executable semantics.
- P322: ReviewOnly Numeric Point Assembler Java skeleton defining assembler-only, targeted-test-only explicit input movement into `ReviewOnlyNumericPointProposalDTO` plus mandatory `NumericPointSafetyValidator` validation.
- P323: Source-owned Numeric Point Candidate Assembler Plan defining docs-only future source-owned context selection, explicit proposal input creation, mandatory P322 assembler invocation, and mandatory validator gating.
- P324: Source-owned Numeric Point Candidate Assembler Java skeleton defining source-owned-field-only conversion into P322 explicit assembly input, mandatory P322 assembler invocation, mandatory validator gating, trusted-source fail-closed handling, and targeted tests.
- P325: Source-owned Numeric Point Candidate Assembler Verification confirming P320-P324 are only a review-only DTO / validator / explicit assembler / source-owned assembler chain and still cannot read real source context or generate executable point values.
- P326: Source Context Integration Plan defining future required SourceTrace, RuntimeKlineContext, DataQuality, MultiTimeframe, RiskActionGuard, WatchlistPoolProof, and source-owned point context integration boundaries without connecting any real source context.
- P327: SourceTrace Numeric Source Read Model Plan defining future SourceTrace numeric source read model boundaries without creating Java or connecting source context.
- P328: SourceTraceNumericSourceContextDTO Java Skeleton defining a DTO-only, targeted-test-only carrier for future SourceTrace numeric source context fields.
- P329: SourceTraceNumericSourceReadModelValidator Java Skeleton defining a validator-only, targeted-test-only safety gate for the SourceTrace DTO.
- P330: SourceTrace Numeric Source Validator Verification confirming P327-P329 are only a SourceTrace read-model plan, DTO carrier, and validator skeleton chain.
- P331: SourceTraceNumericSourceReadModelAssembler Java Skeleton defining an assembler-only, targeted-test-only explicit SourceTrace input mover into the SourceTrace DTO plus validator result.
- P332: SourceTrace Numeric Source Assembler Verification confirming P331 is only a SourceTrace explicit-input assembler skeleton and targeted-test package.
- P333: SourceTrace Runtime / Source Binding Plan defining docs-only future source binding boundaries without runtime wiring.
- P334: SourceTrace Runtime / Source Binding Verification confirming P333 remains docs-only and review-only.
- P335: RuntimeKlineContext Source Binding Plan defining how future RuntimeKlineContext evidence may bind existing SourceTrace refs without reading market data.
- P336: RuntimeKlineContextSourceBindingDTO Java Skeleton defining a DTO-only, targeted-test-only carrier for future RuntimeKlineContext source binding fields.
- P337: RuntimeKlineContextSourceBindingValidator Java Skeleton defining a validator-only, targeted-test-only safety gate for RuntimeKlineContext source binding.
- P338: RuntimeKlineContextSourceBindingAssembler And Verification defining an explicit-input assembler skeleton and closing P335-P338 at L0-L4.
- P339: DataQualityContext Source Binding Ability Closure defining DataQualityContext source-binding plan, DTO, validator, assembler, targeted tests, and verification docs at L0-L4.
- P340: MultiTimeframeContext Source Binding Ability Closure defining MultiTimeframeContext source-binding plan, DTO, validator, assembler, targeted tests, and verification docs at L0-L4.

These packages are DTO / validator / skeleton / targeted-test / test-only wiring / review-only output work.

They are not production market-read wiring.

They do not connect `MarketQuoteClient` / `BinanceMarketQuoteClient` into the new scan-chain.

They do not create runtime market reads, production scan output, real scan loop, production ScanScore, Evidence generation, production Candidate workflow, Opportunity Push execution, executable Readiness, point generation, order execution, or auto-trading.

## Completed P302 Scope

P302 is merged on main and adds a review-only internal push preview / recheck handoff skeleton.

P302 turns `ReviewOnlyCandidatePreviewGuardDTO` into `ReviewOnlyInternalPushPreviewDTO`, so review-only candidate preview / ranking guard can become safe manual-review internal push preview and recheck handoff context for later push preview closure / external channel authorization work.

P302 is not a real Push.

P302 does not connect Telegram, email, webhook, app notification, or local notification.

P302 does not generate external channel messages.

P302 does not render sendable messages and does not send any messages.

P302 does not generate Readiness.

P302 does not generate point generation, entry, stop, TP, RR, final direction, long-short signal, order intent, execution intent, or auto-trading.

P302 preserves `recheckRequired = true`.

P302 preserves `riskActionGuardRequired = true`.

P302 does not connect `MarketQuoteClient` / `BinanceMarketQuoteClient`.

P302 does not create production scan output, real EvidenceItem, real ScoreItem, real Candidate, real Push, external channel behavior, Readiness, point generation, order execution, execution API, or auto-trading.

## Completed P303 Scope

P303 is merged on main and closes the internal push preview boundary before any external channel.

P303 adds closure docs and strengthens targeted guard tests for `ReviewOnlyInternalPushPreviewDTO` / `ReviewOnlyInternalPushPreviewAssembler`.

P303 confirms internal push preview remains review-only, not a trade instruction, manual-review required, recheck-required, Risk Action Guard required, non-sendable, and blocked from external channel behavior.

P303 is not a real Push.

P303 does not connect Telegram, email, webhook, app notification, local notification, or any external channel.

P303 does not generate external channel messages, sendable messages, Readiness, point generation, entry, stop, TP, RR, order intent, execution intent, or auto-trading.

If the next step enters external channels, it requires separate C-level authorization. The default next safe step is dashboard / internal preview display.

## Current P304 Scope

P304 is merged on main and adds a dashboard / internal push preview display gate.

P304 displays internal push preview status as review-only, not a trade instruction, manual-review required, recheck-required, Risk Action Guard required, external-channel disabled, and blocked / fail-closed when source proof or guard conditions are missing.

P304 is not a real Push.

P304 does not connect Telegram, email, webhook, app notification, local notification, or any external channel.

P304 does not generate external channel messages, sendable messages, Readiness, point generation, entry, stop, TP, RR, order intent, execution intent, or auto-trading.

If the next step enters external channels, it requires separate C-level authorization. The default next safe step is internal push preview smoke / closure.

## Completed P305 Scope

P305 is merged on main and closes the P299-P304 Candidate / Push review-only MVP chain.

P305 confirms the chain reaches dashboard-visible internal push preview display only.

Candidate / Push currently completes only this internal read-only chain:

- `ReviewOnlyScoreAssemblyDTO` -> `ReviewOnlyCandidateHandoffDTO`
- `ReviewOnlyCandidateHandoffDTO` -> `ReviewOnlyCandidateAttentionDTO`
- `ReviewOnlyCandidateAttentionDTO` -> `ReviewOnlyCandidatePreviewGuardDTO`
- `ReviewOnlyCandidatePreviewGuardDTO` -> `ReviewOnlyInternalPushPreviewDTO`
- Dashboard / Internal Push Preview Display Gate

P305 is not a real Candidate.

P305 is not a real Push.

P305 does not connect Telegram, email, webhook, app notification, local notification, or any external channel.

P305 does not generate external channel messages, sendable messages, Readiness, point generation, entry, stop, TP, RR, order intent, execution intent, or auto-trading.

If the next step enters external channels, it requires separate C-level authorization. The default does not enter external channel.

## Completed P306 Scope

P306 is merged on main and defines the Readiness / Point Boundary Planning Gate.

P306 is docs-only planning.

P306 does not implement Readiness.

P306 does not implement point generation.

P306 does not generate entry, stop, TP, RR, final direction, long-short signal, order intent, execution intent, or auto-trading.

P306 does not authorize external channel behavior.

P306 defines the required boundary before future Review-only Readiness Gate and Review-only Point Proposal work:

- recheck must pass;
- Risk Action Guard must pass;
- data quality must be sufficient;
- source trace must be present and source-owned;
- missing data must become `INCOMPLETE`;
- liquidity / stampede state must be checked;
- wick / pin-bar behavior must not be mistaken for trend reversal;
- strong reversal must not become direct reverse.

The next recommended package is P307 Review-only Readiness Gate Skeleton.

## Completed P307 Scope

P307 is merged on main and adds the Review-only Readiness Gate Skeleton.

P307 turns `ReviewOnlyInternalPushPreviewDTO` into `ReviewOnlyReadinessGateDTO`.

P307 is review-only only.

P307 does not create executable readiness.

P307 does not implement point generation.

P307 does not generate entry, stop, TP, RR, final direction, long-short signal, order intent, execution intent, or auto-trading.

P307 does not authorize external channel behavior.

P307 preserves recheck-required and Risk Action Guard required status from the internal push preview boundary.

The next recommended package is P308 Review-only Point Boundary / Proposal Gate.

## Completed P308 Scope

P308 is merged on main and adds the Review-only Point Boundary / Proposal Gate.

P308 turns `ReviewOnlyReadinessGateDTO` into `ReviewOnlyPointBoundaryGateDTO`.

P308 is review-only only.

P308 does not create executable readiness.

P308 does not implement executable point generation.

P308 does not generate entry, stop, TP, RR, price, final direction, long-short signal, order intent, execution intent, or auto-trading.

P308 does not authorize external channel behavior.

P308 preserves recheck-required and Risk Action Guard required status from the readiness boundary.

P308 may expose only a review-only `pointProposalAllowed` gate state or a point proposal unavailable reason.

The next recommended package after P308 is Source-owned Review-only Point Proposal Skeleton.

## Completed P309 Scope

P309 is merged on main and adds the Source-owned Review-only Point Proposal Skeleton.

P309 turns `ReviewOnlyPointBoundaryGateDTO` into `ReviewOnlyPointProposalDTO`.

P309 is review-only only.

P309 may carry nullable proposal fields for entry, stop, take-profit, and RR, but the assembler must not invent or execute values.

If source trace, runtime kline context, data quality, or multi-timeframe confirmation is missing, P309 remains `INCOMPLETE`.

P309 does not create executable readiness.

P309 does not implement executable point generation.

P309 does not generate executable entry, stop, TP, RR, price, final direction, long-short signal, order intent, execution intent, or auto-trading.

P309 does not authorize external channel behavior.

P309 preserves recheck-required, Risk Action Guard required, source-trace-required, runtime-kline-context-required, review-only, manual-review-required, and not-trade-instruction status.

P309 fed P310 Point Proposal Closure / Dashboard Display Gate.

## Completed P310 Scope

P310 is merged on main and adds the Point Proposal Closure / Dashboard Display Gate.

P310 turns `ReviewOnlyPointProposalDTO` into `ReviewOnlyPointProposalDisplayDTO`.

P310 is review-only only.

P310 maps proposal values to internal display-safe unavailable placeholders and proves they cannot be interpreted as executable entry, stop, TP, or RR instructions.

P310 does not modify dashboard HTML.

P310 does not add controller, endpoint, API, mapper, repository, scheduler, resource, schema, config, external channel, Telegram, email, webhook, push send, order, execution, or auto-trading.

P310 does not implement executable point generation.

The next recommended package after P310 is a separate executable point generation pre-approval plan.

## Completed P311 Scope

P311 is merged on main and adds the Executable Point Generation Pre-Approval Plan.

P311 defines the approval boundary before any future source-owned numeric point proposal or executable point-generation-adjacent work.

P311 answers when source trace, runtime kline context, data quality, multi-timeframe confirmation, liquidity / stampede / wick / strong reversal checks, Risk Action Guard, and manual review are required before considering a future review-only numeric point proposal.

P311 does not modify Java, tests, dashboard, resources, schema, config, pom, external channel, push send, order, execution, or auto-trading.

P311 does not implement executable point generation.

P311 does not complete entry, stop, TP, or RR as executable outputs.

## Current P312 Scope

P312 is the active docs-only package.

P312 defines the future shape of a source-owned review-only numeric point proposal, including planned entry, stop, TP ladder, RR, and source trace metadata fields.

P312 does not create `ReviewOnlyNumericPointProposalDTO` or any Java file.

P312 does not generate numeric entry, stop, TP, or RR values.

P312 does not connect dashboard, controller, mapper, repository, DB, external channel, push send, order, execution, or auto-trading.

## Current Workflow Scope

P291D, P291E, and P291G are merged on main and provide terminal helpers.

P291H changes the default priority: GitHub-native workflow first, terminal scripts fallback only except local main sync after merge.

Workflow/source-of-truth packages are not production runtime progress.

Use `docs/GITHUB_NATIVE_WORKFLOW.md` and `docs/WORKFLOW_COMMAND_AUTOMATION.md` for workflow rules.

## Current Next Mainline

The current mainline is Readiness / Point Mainline.

The current block is Source-owned Numeric Point Proposal Plan.

Evidence / Score entry is completed at `REVIEW_ONLY_EVIDENCE_SCORE_ENTRY_SKELETON` after P295.

Evidence normalization is completed at `REVIEW_ONLY_EVIDENCE_NORMALIZATION_SKELETON` after P296.

Score input / precheck is completed at `REVIEW_ONLY_SCORE_INPUT_PRECHECK_SKELETON` after P297.

Review-only score assembly is completed at `REVIEW_ONLY_SCORE_ASSEMBLY_SKELETON` after P298.

Review-only Point Boundary / Proposal Gate is completed at `REVIEW_ONLY_POINT_BOUNDARY_GATE_SKELETON` after P308.

Source-owned Review-only Point Proposal is completed at `SOURCE_OWNED_REVIEW_ONLY_POINT_PROPOSAL_SKELETON` after P309.

Point Proposal Closure / Dashboard Display Gate is completed at `REVIEW_ONLY_POINT_PROPOSAL_CLOSURE_DISPLAY_GATE` after P310.

Executable Point Generation Pre-Approval Plan is completed at `EXECUTABLE_POINT_GENERATION_PRE_APPROVAL_PLAN` after P311.

Source-owned Numeric Point Proposal Plan is active at `SOURCE_OWNED_NUMERIC_POINT_PROPOSAL_PLAN` in P312.

Score-to-Candidate handoff is completed at `REVIEW_ONLY_CANDIDATE_HANDOFF_SKELETON` after P299.

Candidate Attention is completed at `REVIEW_ONLY_CANDIDATE_ATTENTION_SKELETON` after P300.

Candidate Preview / Ranking Guard is completed at `REVIEW_ONLY_CANDIDATE_PREVIEW_GUARD_SKELETON` after P301.

Internal Push Preview / Recheck Handoff is completed after P302.

Push Preview Closure Before External Channel is completed after P303.

Dashboard / Internal Push Preview Display Gate is completed after P304.

Candidate / Push Review-Only MVP Closure is completed after P305.

Readiness / Point Boundary Planning Gate is completed after P306.

Review-only Readiness Gate Skeleton is completed after P307.

Evidence generation and score calculation are not completed.

Executable Readiness, executable point generation, external channel, order execution, execution API, and auto-trading are not completed.

## MarketReadRequest Current Capability

- `MarketReadRequestDTO`: `3 TARGETED_TEST`
- `MarketReadRequestGuardValidator`: `3 TARGETED_TEST`
- `MarketReadRequest test-only wiring`: `4 TEST_ONLY_WIRING`
- `MarketReadRequest review-only output assembler`: `REVIEW_ONLY_OUTPUT_SKELETON`, completed after P293, not production assembler
- `Review-only MarketRead scan output`: `REVIEW_ONLY_SCAN_OUTPUT_SKELETON`, completed after P294, not production scan output
- `Review-only Evidence / Score entry`: `REVIEW_ONLY_EVIDENCE_SCORE_ENTRY_SKELETON`, completed after P295, not real evidence generation or score calculation
- `Review-only evidence normalization`: `REVIEW_ONLY_EVIDENCE_NORMALIZATION_SKELETON`, completed after P296, not real evidence generation or score calculation
- `Review-only score input / precheck`: `REVIEW_ONLY_SCORE_INPUT_PRECHECK_SKELETON`, completed after P297, not real score calculation or ScoreItem generation
- `Review-only score assembly`: `REVIEW_ONLY_SCORE_ASSEMBLY_SKELETON`, completed after P298, not real score calculation, final score, direction, or ScoreItem generation
- `Review-only candidate handoff`: `REVIEW_ONLY_CANDIDATE_HANDOFF_SKELETON`, completed after P299, not real Candidate, Candidate Attention, Push, Readiness, or point generation
- `Review-only candidate attention`: `REVIEW_ONLY_CANDIDATE_ATTENTION_SKELETON`, completed after P300, not real Candidate, candidate rank, candidate score, Push, Readiness, or point generation
- `Review-only candidate preview / ranking guard`: `REVIEW_ONLY_CANDIDATE_PREVIEW_GUARD_SKELETON`, completed after P301, not real Candidate, candidate rank, candidate score, real ranking result, Push, Readiness, or point generation
- `Review-only internal push preview / recheck handoff`: `REVIEW_ONLY_INTERNAL_PUSH_PREVIEW_RECHECK_SKELETON`, completed after P302, not real Push, external channel, Readiness, point generation, or message send
- `Push preview closure before external channel`: `REVIEW_ONLY_PUSH_PREVIEW_CLOSURE`, completed after P303, not real Push, external channel, Readiness, point generation, or message send
- `Dashboard / internal push preview display gate`: `DASHBOARD_INTERNAL_PUSH_PREVIEW_DISPLAY_GATE`, completed after P304, not real Push, external channel, Readiness, point generation, or message send
- `Candidate / Push review-only MVP closure`: `CANDIDATE_PUSH_REVIEW_ONLY_MVP_CLOSURE`, completed after P305, not real Candidate, real Push, external channel, Readiness, point generation, or message send
- `Readiness / Point Boundary Planning Gate`: `READINESS_POINT_BOUNDARY_PLAN`, completed after P306, not real Readiness, not point generation, not entry / stop / TP / RR, and not external channel authorization
- `Review-only Readiness Gate Skeleton`: `REVIEW_ONLY_READINESS_GATE_SKELETON`, completed after P307, not executable Readiness, not point generation, not entry / stop / TP / RR, and not external channel authorization
- `Review-only Point Boundary / Proposal Gate`: `REVIEW_ONLY_POINT_BOUNDARY_GATE_SKELETON`, completed after P308, not executable point generation
- `Source-owned Review-only Point Proposal Skeleton`: `SOURCE_OWNED_REVIEW_ONLY_POINT_PROPOSAL_SKELETON`, completed after P309, not executable entry / stop / TP / RR
- `Point Proposal Closure / Dashboard Display Gate`: `REVIEW_ONLY_POINT_PROPOSAL_CLOSURE_DISPLAY_GATE`, completed after P310, not dashboard runtime integration and not executable point generation
- `Executable Point Generation Pre-Approval Plan`: `EXECUTABLE_POINT_GENERATION_PRE_APPROVAL_PLAN`, completed after P311, docs-only and not Java implementation
- `Source-owned Numeric Point Proposal Plan`: `SOURCE_OWNED_NUMERIC_POINT_PROPOSAL_PLAN`, pending in P312, docs-only and not numeric point generation implementation

## What Is Still Not Completed

The following remain incomplete for the new MVP chain:

- production MarketReadRequest assembler.
- scan-chain market read adapter connected to authorized review-only inputs.
- production/runtime market read for the new scan-chain.
- production scan output from live market data.
- Evidence generation.
- Score calculation over real evidence.
- production ScanScore over live scan output.
- review-only Candidate from scored scan output.
- internal Opportunity Push preview from candidate output.
- Push Recheck integration for preview expiry/drift handling.
- review-only Execution Advice from a complete candidate chain.
- runtime entry / stop / TP / RR proposal chain.
- manual position entry to monitoring loop closure.
- position-monitor downgrade/action suggestion loop.
- real GPT / Gemini / Grok arbitration.
- missed-valid opportunity logging and feedback loop.
- dashboard MVP smoke over the full review-only chain.

## Legacy Runtime Clarification

Legacy market and monitor components exist in the repository, including market clients, dashboard services, scheduled recheck, and position monitoring foundations.

Those legacy capabilities must not be described as completion of the P287-P307 market-read / evidence-entry / score assembly / candidate handoff / candidate attention / candidate preview guard / internal push preview / push preview closure / dashboard display gate / Candidate-Push MVP closure / Readiness-Point boundary planning / review-only readiness gate scan-chain.

Any use of legacy `MarketQuoteClient` / `BinanceMarketQuoteClient` in the new scan-chain requires a separate authorization package.

## Review-Only Output Clarification

P291A restores the principle that review-only does not mean no output.

The system may produce safe manual-review proposals, such as entry zone proposal, stop zone proposal, TP proposal, RR estimate, position size suggestion, leverage cap suggestion, invalidation condition, reduce-position suggestion, tighten-stop suggestion, move-stop suggestion, partial take-profit suggestion, wait-for-trigger state, plan-invalidated state, internal push preview, risk-downgraded candidate, and confused-with-recovery-condition state.

Those outputs must remain non-executable, manual-review required, and not trade instructions.

Automatic order, close, reverse, leverage change, execution, and auto-trading remain blocked.

## Current Recommendation

Use GPT + Codex + GitHub-native workflow by default.

Review P312 before any merge decision.

Do not describe P295 as real evidence generation or real score calculation.

Do not describe P296 as real evidence generation, persisted evidence, or real score calculation.

Do not describe P297 as real scoring, ScoreItem generation, or score calculation.

Do not describe P298 as real scoring, ScoreItem generation, score calculation, final score, direction, or Candidate handoff completion.

Do not describe P299 as real Candidate, Candidate Attention, Promote To Home, Push, Readiness, or point generation.

Do not describe P300 as real Candidate, candidate rank, candidate score, Promote To Home, Push, Readiness, or point generation.

Do not describe P301 as real Candidate, candidate rank, candidate score, real ranking result, Promote To Home, Push, Readiness, or point generation.

Do not describe P302 as real Push, external channel behavior, Telegram/email/webhook/app/local notification, sendable message rendering, Readiness, point generation, or trading behavior.

Do not describe P303 as external channel authorization, real Push, Telegram/email/webhook/app/local notification, sendable message rendering, message sending, Readiness, point generation, or trading behavior.

Do not describe P304 as external channel authorization, real Push, Telegram/email/webhook/app/local notification, sendable message rendering, message sending, Readiness, point generation, or trading behavior.

Do not describe P305 as real Candidate, real Push, external channel authorization, Telegram/email/webhook/app/local notification, sendable message rendering, message sending, Readiness, point generation, or trading behavior.

Do not describe P306 as real Readiness, point generation, entry / stop / TP / RR, external channel authorization, order execution, execution API, or auto-trading.

Do not describe P307 as executable Readiness, point generation, entry / stop / TP / RR, external channel authorization, order execution, execution API, or auto-trading.

Do not describe P308 as executable point generation, entry / stop / TP / RR, external channel authorization, order execution, execution API, or auto-trading.

Do not describe P309 as executable point generation, executable entry / stop / TP / RR, external channel authorization, order execution, execution API, or auto-trading.

Do not describe P310 as executable point generation, executable entry / stop / TP / RR, dashboard runtime integration, external channel authorization, order execution, execution API, or auto-trading.

Do not describe P311 as executable point generation, source-owned numeric proposal implementation, executable entry / stop / TP / RR, dashboard runtime integration, external channel authorization, order execution, execution API, or auto-trading.

Do not describe P312 as Java DTO completion, numeric point proposal implementation, executable point generation, executable entry / stop / TP / RR, dashboard runtime integration, external channel authorization, order execution, execution API, or auto-trading.

Do not describe real Candidate, real Push, executable Readiness, external channel, or point generation as completed.
