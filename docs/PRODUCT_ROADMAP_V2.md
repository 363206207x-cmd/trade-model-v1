# Trade Model V1 Product Roadmap V2

Status: `P0_PRODUCT_BASELINE_FREEZE_CANDIDATE`

Direction: `PRODUCT_FIRST`

Governance First is paused. Governance, Workflow, and tests remain delivery safeguards but cannot select product scope or prove product completion. Each phase starts with `scripts/product-source-gate.sh`, source reading, product/design/data mapping, and an implementation gap. Authority: `docs/PRODUCT_SOURCE_OF_TRUTH.md`.

## Universal Phase Rules

- A later phase cannot begin implementation until the prior required phase is accepted and effective on merged main.
- A read-only readiness/gap audit may run before implementation when its product sources and mapping are complete.
- Every phase preserves no automatic open, close, add, reduce, reverse, order, or trade execution.
- No phase treats Push Recheck as trading authorization, `triggered` as opened, or ExecutionPlan as UserPosition.
- Real data is required; fixtures and fallback values may test failure behavior but cannot prove product acceptance.
- Product completion follows `docs/PRODUCT_ACCEPTANCE_STANDARD.md`.

## PRODUCT_FIRST_STOP_RULE

This permanent rule is a simple human review rule. It must not become a new governance product or automated semantic engine.

A review finding may block the current product stage only when it is classified as exactly one of:

- `PRODUCT_SEMANTIC_BLOCKER`: a reproducible conflict with formal product semantics or interaction, including AI authority, ExecutionPlan/UserPosition separation, state separation, Home interaction, or Position Monitoring.
- `SECURITY_OR_PRIVACY_BLOCKER`: privacy leakage, owner-scope bypass, unauthorized mutation, automatic open/close/reverse/trade, or Push Recheck used as trading authorization.
- `REAL_DATA_INTEGRITY_BLOCKER`: mock/default/fallback data presented as real, failure presented as success, or fabricated product/AI fields.
- `NEXT_PRODUCT_STAGE_BLOCKER`: reproducible evidence that the current stage cannot merge or the next formal Product Roadmap stage cannot start after merge, creating a real delivery deadlock.
- `BUILD_OR_RUNTIME_BLOCKER`: compile failure, required-test failure, application startup failure, or failure of a core runtime chain.

Every other finding is `NON_BLOCKING_TECHNICAL_DEBT` and must set `BLOCKS_CURRENT_STAGE: NO`. Examples include non-critical wording or metadata, formatting/naming preference, theoretical future cases, non-critical Workflow improvement, parser/inventory/digest/helper refinement, non-security test idealization, maintainability advice, or refactoring outside the current product package.

Every review finding must report:

```text
FINDING_ID:
BLOCKER_CLASS:
DIRECT_PRODUCT_IMPACT:
REPRODUCTION_EVIDENCE:
BLOCKS_CURRENT_STAGE: YES / NO
```

A finding with `BLOCKS_CURRENT_STAGE: YES` must also identify the affected formal product source and explain why it cannot be deferred. Without concrete product impact, a reproducible path, the affected formal product source, and a non-deferrable reason, it must set `BLOCKS_CURRENT_STAGE: NO`. P1/P2/P3 priority and blocking status are independent.

Workflow, Governance, Metadata, and Review tooling together may consume at most an estimated 10% of a product stage. At 10%, stop expanding them, register remaining items as `NON_BLOCKING_TECHNICAL_DEBT`, and resume product work. Exceptions require a demonstrated product-semantic, security/privacy, build/runtime, or actual next-stage blocker. Use a reasonable human estimate; do not build a statistics system. Task reports include:

```text
PRODUCT_WORK_RATIO:
NON_PRODUCT_WORK_RATIO:
STOP_RULE_TRIGGERED: YES / NO
```

Implementation is limited to plain documentation, fixed review fields, minimal shell assertions, and explicit human classification. Do not build a natural-language classifier, synonym list, semantic parser, inventory, digest, whole-review analyzer, independent Stop Rule phase, or large meta-test suite.

Fixed examples:

- naming preference -> `NON_BLOCKING_TECHNICAL_DEBT` -> `BLOCKS_CURRENT_STAGE: NO`
- reproducible cross-user data leak -> `SECURITY_OR_PRIVACY_BLOCKER` -> `BLOCKS_CURRENT_STAGE: YES`
- reproducible post-merge P1A deadlock -> `NEXT_PRODUCT_STAGE_BLOCKER` -> `BLOCKS_CURRENT_STAGE: YES`

Every phase uses four distinct task modes. They cannot be combined to turn an audit result into implementation permission:

| Task mode | Purpose | May change business code/UI/tests | May merge business capability |
|---|---|---|---|
| `READINESS_AND_GAP_AUDIT` / `READ_ONLY_PRODUCT_AUDIT` | compare formal product/design/data requirements with current runtime and record exact gaps | no | no |
| `IMPLEMENTATION` | implement only the bounded package authorized by a reviewed, merged-main audit decision | only explicitly authorized scope | no; implementation first returns to review |
| `VALIDATION` | exact-Head review, real/controlled scenarios, screenshots, payload/source checks, regression and scope validation | no remediation inside a read-only review | no |
| `MERGE` | final identity/check/thread gate and separately authorized merge/merged-main validation | no feature expansion | only the exact reviewed Head |

| Phase | READINESS_AND_GAP_AUDIT | IMPLEMENTATION | VALIDATION | MERGE |
|---|---|---|---|---|
| P0 Product Foundation | source/fact inventory and product baseline | docs/minimal gate only | source hashes, mappings, workflow, Maven | independent P0 Draft review and explicit merge gate |
| P1 Home | P1A Home alignment audit | P1B bounded Home package | Home payload, screenshot, interaction, five-state and real/fallback review | exact reviewed Home Head only |
| P2 Position | position/monitor identity, state, data and scenario audit | bounded manual position/monitor package | owner, exact-ID, real/historical monitor and UI validation | exact reviewed Position Head only |
| P3 AI Analysis | evidence/score/model/role/fallback audit | bounded rule-first AI analysis package | immutable evidence, role availability, conflict/fallback review | exact reviewed AI Head only |
| P4 Detail Pages | exact identity/navigation/source audit | bounded detail package | route, source, state and navigation validation | exact reviewed Detail Head only |
| P5 Message/Push | public/private projection and field audit | bounded read-only Message/Push UI | payload privacy, owner denial, UI/state validation | exact reviewed Message/Push Head only |
| P6 My/Settings | real account/settings field audit | bounded real-backed account/settings package | session/logout/unsupported-field validation | exact reviewed My/Settings Head only |
| P7 Integration | cross-module identity/state/privacy audit | bounded integration corrections | full journeys, browser matrix, failure/recovery | exact reviewed integration Head only |
| P8 Server | infrastructure/security/operations readiness audit | bounded deployment package | deployment, migration, rollback and observability drills | exact reviewed server/deployment Head only |
| P9 iPhone | existing simulator foundation and real-device gap audit | bounded device/server integration | physical-device, network, lifecycle, accessibility evidence | exact reviewed iPhone Head only |
| P10 Real-World Validation | scenario/corpus/readiness audit | only reviewed calibration or defect corrections | sustained representative operation and outcome review | exact reviewed correction Head only |

## P0 — Product Foundation Freeze

**Input:** four formal product source classes, frozen design references, current runtime evidence, and the decision to pause Governance First.

**Deliverables:** Product Source of Truth; registered source snapshots and hashes; Product Source Gate; permanent bootstrap/task/output rules; module tree; relation graph; state machines; interaction baseline; field sources; completion matrix; gaps; Roadmap V2; acceptance standard; freeze report.

**Dependencies:** exact source paths and readable content; clean independent branch; PR #1156 recovery assets and unresolved review history preserved after close without merge.

**Allowed scope:** product documentation, permanent agent/workflow instructions, minimal deterministic source gate, workflow invocation.

**Blocked scope:** Java, business APIs, schema, UI, Figma, business tests, business features, semantic-parser expansion, PR #1156 modification. Minimal deterministic Product Source/audit-policy workflow assertions are permitted only inside P0.

**Acceptance evidence:** source hashes; gate output; Home/Position/AI simulations; six fixed audit-policy boundary cases; Markdown/YAML/link/path checks; workflow contract; scope check; Maven when required; Draft PR.

**Real scenario requirement:** not a business scenario; deterministic simulation proves missing mappings fail closed and three representative tasks request the right sources/boundaries.

**Exit criteria:** P0 package independently reviewed and merged to main; main synced and clean; product gate effective; no P1 implementation in the P0 PR.

## P1 — Home Alignment

### P1A — Home Alignment Readiness and Gap Audit

**Task mode:** `READINESS_AND_GAP_AUDIT` / `READ_ONLY_PRODUCT_AUDIT`.

**Status:** `COMPLETED`. This is an audit-completion claim only; Home remains
`PARTIAL` and no implementation capability moved.

**Durable evidence:** `docs/P1A_HOME_ALIGNMENT_AUDIT.md`.

**Input:** P0 baseline effective on clean/synced merged main, Home interaction source bundle, Figma Home nodes, current `GET /api/dashboard/home`, current UI/runtime, and exact field-source evidence.

**Deliverables:** read-only comparison of final module order, field provenance, real/derived/fallback status, context-only asset click, plan/three-AI/Top3 linkage, all five states, desktop/mobile screenshots, and a prioritized bounded gap list.

**Allowed scope:** source/merged-code/API/test/Figma inspection, runtime/network inspection, screenshots, evidence capture, and audit output. Every active non-current open PR is a conflict blocker. Closed unmerged technical debt does not block the audit, but its branch/patch/stash content is recovery evidence only and cannot be treated as current implementation.

**Blocked scope:** code or test modification, business PR creation, PR #1156 reopen/recovery-content use, UI/API/schema/Figma changes, implementation, Ready transition, merge, deployment, Telegram/notification/trading, and presenting fallback scores as real confidence.

**Audit exit:** findings receive independent review and a separate merged-main authorization record. Audit completion alone does not authorize implementation.

### P1B — Home Alignment First Implementation

**Task mode:** `IMPLEMENTATION`, followed separately by `VALIDATION` and `MERGE`.

#### P1B-1 Authorization

**Status:** `EFFECTIVE_MERGED_MAIN`. The bounded P1B-1 implementation is
effective on merged main through PR #1159 / commit
`458c7fe49a9eee929fa90ad2de2d9d10ad86adb2`.

**Authorization record:** `docs/P1B_AUTHORIZATION_SCOPE.md`.

**Bounded scope:** existing Home read-projection assembly and frontend binding
only. `executionSuggestion` remains the selected asset's verified plan;
`positions` remains the independent owner-scoped UserPosition projection. The
endpoint and JSON shape remain unchanged. No trading, position mutation, AI,
score, notification, Telegram, schema, or ExecutionPlan/PositionMonitor state
machine change is authorized.

**Effectivity:** the authorization and bounded implementation have both passed
their independent merged-main gates. This proves only P1B-1 projection
separation; it does not complete the remaining Home core-data fields, exact
plan identity, Top3 projection, six-state behavior, or full Home acceptance.

**Input:** accepted P1A artifacts and explicit bounded P1B authorization effective on merged main.

**Deliverables:** only the authorized subset of final Home module order, focus-asset cards, context-only selection, verified ExecutionPlan summary, three-AI summary, Top3 positions, alerts/events, five-state handling, and responsive desktop/mobile views.

**Dependencies:** P0 and P1A authorization merged; real Home source data available; exact analysis/plan/position identity contracts; no unresolved public/private leakage.

**Allowed scope:** Home template/CSS/JS/read projection changes and focused tests explicitly named by the accepted P1B package; read-only data wiring.

**Blocked scope:** automatic trading, watch writes, market search not formally authorized, message expansion, new AI model, fake counts/percentages/records, or treating `/api/score/list` as calibrated formal confidence without a proven evidence chain.

**Acceptance evidence:** field-source trace; API payload; Playwright/browser screenshots at desktop and mobile widths; asset selection interaction recording; accessibility checks; five-state tests; stale-cache failure test.

**Real scenario requirement:** at least two real assets with different plan/AI contexts, one partial-data asset, and one forced read failure; selection must update linked regions without navigating or changing a position.

**Exit criteria:** all authorized Home acceptance items pass in a separate validation; screenshots and payload traces are archived; no unsupported field shown; the exact reviewed Head is merged and effective on main.

#### P1B Home Core Data Authorization

**Status:** `AUTHORIZED_PENDING_MERGED_MAIN`.

**Authorization record:** `docs/P1B_HOME_CORE_DATA_AUTHORIZATION.md`.

**Product decision:** an Asset Card retains seven primary business fields and
may add a visually subordinate four-field status strip for `dataQuality`,
`multiTimeframeState`, `Confused`, and `updatedAt`. Supporting metadata does
not replace or outrank the primary fields.

**Bounded implementation scope after effectivity:** complete the Asset Card
read projection and source classifications, connect exact persisted
ExecutionPlan identity without latest/symbol/timeframe inference, expose
owner-scoped independent Top3 UserPosition data, and complete module-local
LOADING/READY/PARTIAL/EMPTY/ERROR/MISSING plus retry/stale-context handling.

**Effectivity:** this authorization is not effective on its branch. Before it
is merged and validated on clean/synced `main`, a request for
`P1B_HOME_CORE_DATA_COMPLETION` must remain blocked with repository edits,
implementation, and PR creation disabled. After merged-main effectivity, the
exact request may resolve to `IMPLEMENTATION` with those permissions enabled.
The implementation package remains `NOT_STARTED`.

**Blocked scope:** Three AI Evidence Package, AI provider/model expansion,
Score redesign, notification, Telegram, UserPosition mutation, order/trading,
broad schema change, guessed plan identity, and PR #1156 recovery content.

## P2 — Position and Position Monitoring

**Input:** formal Position Monitoring plan, P1 Home identity behavior, real UserPosition contract, original ExecutionPlan links, market evidence and monitor logs.

**Deliverables:** manual position workflow; plan-versus-actual presentation; open/partial/closed lifecycle; authoritative latest monitoring; logic/reversal/risk/liquidity/wick handling; manual suggestions; alerts; complete logs; close/review entry.

**Dependencies:** P1 accepted; current-user ownership proven; exact string IDs; reliable current-market feed; review identity available.

**Allowed scope:** UserPosition/PositionMonitor product paths explicitly mapped to the plan; owner-scoped read/write only for explicit user actions; tests and UI needed by the package.

**Blocked scope:** auto-open, auto-close, auto-reduce, auto-add, auto-reverse, monitor-triggered orders, ExecutionPlan substitution for actual user facts.

**Acceptance evidence:** owner-isolation tests; plan/actual screenshots; monitor state/log trace; no-run-on-read proof; alert/suggestion semantics; accessibility and device-width evidence.

**Real scenario requirement:** historical or real sequences covering valid thesis, weakening, invalidation, wick without strong reversal, true strong reversal, liquidity stress, partial close, final close, and source failure.

**Exit criteria:** list/detail agree on authoritative latest monitor; every change is traceable; no automatic mutation; merged main effective.

## P3 — AI Analysis

**Input:** formal V1 architecture and AI conflict plan, real normalized evidence, eight scores, four timeframes, rule base, AI trigger records, existing FE-03 detail path.

**Deliverables:** rule-base view; data-quality gate; eight scores; multi-timeframe convergence; support/opposition evidence; exactly three role outputs; four conflict levels; Confused; fallback; traceable model/source/time/rule version.

**Dependencies:** P1/P2 identities stable; production-like evidence package; model configuration and safe failure mode; exact `analysisId`.

**Allowed scope:** read and analysis behavior within formal role boundaries; FE-03 detail reuse; focused AI integration tests.

**Blocked scope:** fourth AI, majority vote replacing rules, AI trading authorization, AI state-machine override, fake scores/evidence/confidence, automatic watch writes.

**Acceptance evidence:** immutable role input fingerprint; actual call logs/model metadata; role outputs; conflict/fallback traces; unavailable hard gate; screenshots and field provenance.

**Real scenario requirement:** aligned roles, each conflict level, one model unavailable, all AI unavailable with rule fallback, low data quality, and Confused recovery.

**Exit criteria:** rule-first authority and traceability pass every scenario; no unavailable-field leakage; merged main effective.

## P4 — Detail Pages

**Input:** accepted Home/Position/AI summaries and exact identities.

**Deliverables:** Analysis Detail, Position Detail, Execution Plan Detail, Replay Detail; coherent navigation/back behavior; deep evidence and logs; exact source/version trace; five-state handling.

**Dependencies:** P1-P3 accepted summaries and source contracts; exact identity access control.

**Allowed scope:** read-only detail pages and explicit manual review/feedback actions already authorized by formal sources.

**Blocked scope:** latest/symbol inference, cross-user detail, duplicate analysis system, hidden execution actions, fabricated deep data.

**Acceptance evidence:** exact-ID route tests; source payloads; screenshots; navigation/back-context proof; public/private tests; failure states.

**Real scenario requirement:** one complete analysis, one active position, one closed position/replay, and missing/unauthorized identities.

**Exit criteria:** every summary has a valid detail path or is intentionally non-clickable; merged main effective.

## P5 — Message and Push Detail

**Input:** accepted backend public/private projections, Figma message/push nodes, P2 position identity, P3 analysis identity.

**Deliverables:** Message Center and Push Detail UI for only OPPORTUNITY and POSITION_RISK; original/current comparison; change reason; source-specific navigation; five-state behavior.

**Dependencies:** OPPORTUNITY public projection contains no private reference; POSITION_RISK exact owner scope; message IDs string-safe; read APIs stable.

**Allowed scope:** read-only Message/Push UI and source-specific GET integration.

**Blocked scope:** system messages, free-form AI messages, fake unread/counts, Telegram, external send, auto notification, POST Recheck, monitor run, mutation, trading.

**Acceptance evidence:** network payload privacy, cross-user denial, source-specific screenshots, exact message/detail navigation, error-not-empty tests.

**Real scenario requirement:** public opportunity, owner risk message, changed Recheck, unchanged Recheck, partial, error, empty, missing, and cross-user denial.

**Exit criteria:** public/private data never mixes; no unsupported badges/actions; merged main effective.

## P6 — My and Settings

**Input:** formal product decision for real profile/settings fields, Figma Profile nodes, auth/session capabilities.

**Deliverables:** real account/session summary, logout, real system/rule version where available, and explicitly unavailable preferences.

**Dependencies:** human-approved field contract and real backing source; P0 source registry updated if a new formal plan is required.

**Allowed scope:** authenticated account/settings fields with real APIs and explicit logout.

**Blocked scope:** invented community, referral, paid plan, recommendation, exchange ordering, automatic trading, simulated preference saves.

**Acceptance evidence:** field-source mapping, session/logout tests, screenshots, unsupported-state behavior, accessibility.

**Real scenario requirement:** login, session refresh/expiry, logout, unsupported preference, and reconnect on target browsers/device.

**Exit criteria:** every visible field/action has a real contract; merged main effective.

## P7 — Full Product Integration

**Input:** accepted P1-P6 modules.

**Deliverables:** consistent asset/analysis/plan/position/message identities; cross-page navigation; snapshot/time consistency; owner scope; public/private boundary; global five-state recovery; coherent shell.

**Dependencies:** all user-facing modules merged and individually accepted.

**Allowed scope:** integration defects, shared read models, navigation, state consistency, performance/accessibility work.

**Blocked scope:** new business capability, weakened privacy, automatic trading, hidden fallback, scope expansion under “integration”.

**Acceptance evidence:** full journey tests, browser matrix, network traces, session tests, source-failure drills, responsive screenshots, performance baseline.

**Real scenario requirement:** login -> Home -> analysis -> manual position -> monitor -> risk message -> detail -> close -> review, plus failure/recovery paths.

**Exit criteria:** no identity/state/privacy contradiction across modules; merged main effective.

## P8 — Server Deployment

**Input:** P7 integrated product, production environment, provider credentials, database plan, security and operations plan.

**Deliverables:** production configuration; HTTPS; secure secrets; database/migrations; provider connectivity; logs/metrics/alerts; backup/restore; rollback; runbooks.

**Dependencies:** product integration accepted; approved infrastructure and security review.

**Allowed scope:** deployment, configuration, observability, data migration, operational safeguards.

**Blocked scope:** product semantic changes hidden in deployment, secrets in repository, bypassed tests, automatic trading.

**Acceptance evidence:** deployment log, HTTPS/session/CSRF proof, migration rehearsal, backup/restore, rollback drill, provider degradation, alerts/SLO dashboards.

**Real scenario requirement:** sustained production-like run through market-open/volatile periods and controlled provider/service failures.

**Exit criteria:** deployment readiness standard passes; no user traffic claim until approved release.

## P9 — iPhone Usable Version

**Input:** P7 integrated web product, P8 reachable secure server, the existing merged Xcode SwiftUI/WKWebView simulator foundation, and frozen mobile Figma.

**Deliverables:** complete and validate the existing Xcode route; real-device Session/Cookie/CSRF; five-tab navigation; exact deep links; Dynamic Type; safe area; signed/installable build; background recovery; real-device logs/screenshots.

**Dependencies:** secure deployed environment and Apple development setup.

**Allowed scope:** target-device integration and adaptation without changing business semantics.

**Blocked scope:** device-only fake data, relaxed auth/privacy, hidden unsupported native actions, automatic trading.

**Acceptance evidence:** signed/installable build, real iPhone video/screenshots, network/session trace, accessibility sizes, background/foreground, offline/error recovery.

**Real scenario requirement:** complete P7 journey on a real iPhone over a real network, including session expiry and source failure.

**Exit criteria:** user can install and safely use all accepted read/manual workflows on target hardware.

## P10 — Real-World Validation

**Input:** deployed server, usable iPhone, accepted product modules, real market data, authorized manual user scenarios.

**Deliverables:** continuous-operation evidence; real/historical positions; monitor timeliness; advice quality; confidence calibration; AI quality; review outcomes; prioritized product corrections.

**Dependencies:** P8 and P9 accepted; data/privacy/legal/operational approval.

**Allowed scope:** observation, calibration, product bug fixes, human-reviewed rule iteration, safety improvements.

**Blocked scope:** automatic execution, silent rule/model changes, outcome cherry-picking, fabricated performance, Push/AI authorization of trades.

**Acceptance evidence:** timestamped scenario corpus, source/decision/plan/position/monitor/review traces, false-positive/negative analysis, incident logs, user feedback.

**Real scenario requirement:** sustained representative periods including trend, range, volatility, wick, provider failure, AI failure, missed opportunity, manual entry/exit, and post-close review.

**Exit criteria:** human product review confirms usable value, calibrated limitations, no critical safety/privacy gap, and an evidence-backed next roadmap.

## Roadmap Boundary

P0 creates truth and measurement only. It does not make P1 implementation automatic. The persisted workflow separates the current P0 remediation mode from the authorized next P1A mode. P1A becomes an effective `READ_ONLY_PRODUCT_AUDIT` only after P0 is reviewed, merged, validated, and effective on clean/synced main with Product Source Gate `PASS`; P0 open or Ready/unmerged remains blocked. Every active non-current open PR blocks. Closed unmerged technical debt does not block and is not effective/current content. Dirty worktrees, failed Product Source Gate, repository edits, business PR creation, implementation, merge, and deployment remain blocked. P1B starts only after P1A is independently reviewed and its bounded authorization is effective on merged main. PR #1156 remains `CLOSED_PAUSED_TECHNICAL_DEBT` and may be reconsidered only if a real product regression proves need, followed by a fresh comparison against latest `main` and new authorization.
