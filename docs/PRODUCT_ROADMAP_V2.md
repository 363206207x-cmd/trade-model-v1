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

**Dependencies:** exact source paths and readable content; clean independent branch; paused PR #1156 preserved unchanged.

**Allowed scope:** product documentation, permanent agent/workflow instructions, minimal deterministic source gate, workflow invocation.

**Blocked scope:** Java, business APIs, schema, UI, Figma, business tests, business features, semantic-parser expansion, PR #1156 modification. Minimal deterministic Product Source/audit-policy workflow assertions are permitted only inside P0.

**Acceptance evidence:** source hashes; gate output; Home/Position/AI simulations; six fixed audit-policy boundary cases; Markdown/YAML/link/path checks; workflow contract; scope check; Maven when required; Draft PR.

**Real scenario requirement:** not a business scenario; deterministic simulation proves missing mappings fail closed and three representative tasks request the right sources/boundaries.

**Exit criteria:** P0 package independently reviewed and merged to main; main synced and clean; product gate effective; no P1 implementation in the P0 PR.

## P1 — Home Alignment

### P1A — Home Alignment Readiness and Gap Audit

**Task mode:** `READINESS_AND_GAP_AUDIT` / `READ_ONLY_PRODUCT_AUDIT`.

**Input:** P0 baseline effective on clean/synced merged main, Home interaction source bundle, Figma Home nodes, current `GET /api/dashboard/home`, current UI/runtime, and exact field-source evidence.

**Deliverables:** read-only comparison of final module order, field provenance, real/derived/fallback status, context-only asset click, plan/three-AI/Top3 linkage, all five states, desktop/mobile screenshots, and a prioritized bounded gap list.

**Allowed scope:** source/code/API/test/Figma inspection, runtime/network inspection, screenshots, evidence capture, and audit output. A paused unrelated `PAUSED_TECHNICAL_DEBT` PR such as #1156 does not block this audit when `PRODUCT_AUDIT_ALLOWED=YES`.

**Blocked scope:** code or test modification, business PR creation, PR #1156 changes, UI/API/schema/Figma changes, implementation, Ready transition, merge, deployment, Telegram/notification/trading, and presenting fallback scores as real confidence.

**Audit exit:** findings receive independent review and a separate merged-main authorization record. Audit completion alone does not authorize implementation.

### P1B — Home Alignment First Implementation

**Task mode:** `IMPLEMENTATION`, followed separately by `VALIDATION` and `MERGE`.

**Input:** accepted P1A artifacts and explicit bounded P1B authorization effective on merged main.

**Deliverables:** only the authorized subset of final Home module order, focus-asset cards, context-only selection, verified ExecutionPlan summary, three-AI summary, Top3 positions, alerts/events, five-state handling, and responsive desktop/mobile views.

**Dependencies:** P0 and P1A authorization merged; real Home source data available; exact analysis/plan/position identity contracts; no unresolved public/private leakage.

**Allowed scope:** Home template/CSS/JS/read projection changes and focused tests explicitly named by the accepted P1B package; read-only data wiring.

**Blocked scope:** automatic trading, watch writes, market search not formally authorized, message expansion, new AI model, fake counts/percentages/records, or treating `/api/score/list` as calibrated formal confidence without a proven evidence chain.

**Acceptance evidence:** field-source trace; API payload; Playwright/browser screenshots at desktop and mobile widths; asset selection interaction recording; accessibility checks; five-state tests; stale-cache failure test.

**Real scenario requirement:** at least two real assets with different plan/AI contexts, one partial-data asset, and one forced read failure; selection must update linked regions without navigating or changing a position.

**Exit criteria:** all authorized Home acceptance items pass in a separate validation; screenshots and payload traces are archived; no unsupported field shown; the exact reviewed Head is merged and effective on main.

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

P0 creates truth and measurement only. It does not make P1 implementation automatic. P1 begins with P1A, a `READ_ONLY_PRODUCT_AUDIT`, only after P0 is reviewed, merged, and effective on clean/synced main. An explicitly paused, unrelated Draft PR such as #1156 may coexist with that audit, while active/conflicting PRs, dirty worktrees, failed Product Source Gate, code/test changes, business PR creation, implementation, merge, and deployment remain blocked. P1B starts only after P1A is independently reviewed and its bounded authorization is effective on merged main. PR #1156 remains `PAUSED_TECHNICAL_DEBT` and is resumed only if a real product regression proves that its missing governance capability is necessary.
