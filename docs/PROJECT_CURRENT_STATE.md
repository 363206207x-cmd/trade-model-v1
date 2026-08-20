# Trade Model V1 Current State

Contract: docs/PROJECT_DELIVERY_CONTRACT.md
Contract Version: v1.0
Current Phase: P0-0 Contract Lock + Baseline + Dead Code Candidate Report
Current Phase Status: DONE
Completion Effective State: derived by v1 state runtime
Existing Module Maturity: PARTIAL
Product Direction: PRODUCT_FIRST
Product Authority: docs/PRODUCT_SOURCE_OF_TRUTH.md
Product Phase: Fundamental AI Frontend Interaction Runtime Closure Authorization
Product Phase Status: FRONTEND_INTERACTION_AUTHORIZATION_PENDING_MERGED_MAIN
Current Work Package: Register the exact Desktop runtime interaction closure permission; no application implementation is included here
Next Business Phase: FRONTEND_INTERACTION_RUNTIME_CLOSURE
Next Business Phase Allowed: NO on this authorization branch; YES only for the exact package after this authorization is merged and validated on clean/synced main
Production Deployment Readiness: BLOCKED
Historical Latest Production Readiness Package: PDR-M7 Real Provider Live Smoke Harness recorded on branch codex/pdr-m7-real-provider-live-smoke-harness

---

## Fundamental AI Frontend Interaction Runtime Closure Authorization

PR #1192 is effective on merged main
`141af9945b2e6219ab1a5fbbc904352539b1ac81`. The approved Home now consumes
current runtime data, while the owner walkthrough found one P0 interaction
gap: search results cannot be explicitly selected and therefore expose no
usable analysis-preview or Asset Pool add action.

This A-risk package maps the gap and the bounded Desktop walkthrough to the
existing catalog search, Asset Pool, analysis preview, Dashboard, route and
overlay owners. It authorizes exactly `FRONTEND_INTERACTION_RUNTIME_CLOSURE`
after clean synchronized merged-main validation. It does not authorize Figma,
Mobile, Schema, CoinGlass, AI enablement, quality-threshold changes, fake data,
Home redesign, automatic trading or automatic position mutation. Capability
movement is limited to completing already defined user actions.

---

## Fundamental AI Local-Real Readiness Authorization

PR #1190 is effective on merged main
`56028b21ac3d4ff9d1ee1368b6a144ad77382e19`. The owner-approved current Home
runtime work is preserved separately at commit `4e447e03` and is not part of
this authorization diff.

Real local validation reproduced one bounded defect: normal manual Asset Pool
scans completed and persisted six analyses while the existing
`LocalRealReadinessService` projection remained at zero, leaving the active
Home at `WAITING_SYNC`. The approved Home also needs its current authoritative
runtime/database read binding restored on an authorized implementation branch.

This A-risk package maps that gap to existing Asset Pool, analysis,
local-real readiness, Dashboard, Opportunity, FinalExecutionPlan,
UserPosition/PositionMonitor and Three-AI owners. It authorizes exactly
`LOCAL_REAL_READINESS_SYNC_AND_REAL_ANALYSIS_ENABLEMENT` after clean,
synchronized merged-main validation. It does not authorize Schema/API
redesign, new provider architecture, quality-threshold reduction, fake data,
Figma, Mobile, deployment or automatic trading. Capability movement is
`NONE`.

---

## Fundamental AI v4.1 Telegram High-Value Alert Authorization

PR #1187 merged the independently audited target-runtime remediation on main
`2787f2e999f7744f0bb3e032b0462c9ddea943e4`. The sole authoritative v4.1
Product Source already freezes Telegram as a delivery channel under the
persisted Message fact, with exactly three high-value categories and zero
automatic-trading authority.

This A-risk package maps that contract to existing Message, ChannelDelivery,
Push/Recheck, Opportunity/Final, UserPosition/PositionMonitorLog and
PersonalUser/UserConfig owners. It authorizes exactly
`FUNDAMENTAL_AI_V4_1_TELEGRAM_HIGH_VALUE_ALERT_CHANNEL_INTEGRATION` after
clean/synchronized merged-main validation. Before merge, repository edits,
implementation and successor PR creation remain false.

The authorization changes no application code, API, schema, Figma, Desktop or
Mobile. It does not read the private Telegram environment file and does not
claim application-level live delivery. Capability movement is `NONE`.

---

## Fundamental AI v4.1 Target Runtime Blocker Remediation Authorization

PR #1179 is complete and effective on merged main
`3a6f56afaf6fbba3d094d532f7f9555a23ac30a1`. Target-runtime acceptance then
reproduced four implementation blockers: B01 standard release-JAR Flyway
runtime packaging, B02 exact provider/instrument coverage and regional
failure isolation, B03 truthful AI readiness and exact-model preflight, and
B04 authentication bootstrap/readiness consistency.

The sole ACTIVE/AUTHORITATIVE v4.1 Product Source remains
`docs/product-sources/FUNDAMENTAL_AI_V4_1_DECISION_CHAIN.md`. The blocker
source mapping and ownership map do not redefine it; they connect the four
runtime failures to existing build/Flyway, provider, AI and authentication
owners. No duplicate business skeleton is authorized.

This package authorizes exactly
`FUNDAMENTAL_AI_V4_1_TARGET_RUNTIME_BLOCKER_REMEDIATION` after merged-main
validation. The successor may repair only B01-B04, with no Figma, Desktop UI,
Mobile, product-contract, automatic-trading or position-mutation changes.
CoinGlass protocol support may be tested without a live secret; a live secret
must never be written to the repository and live acceptance remains deferred.

This authorization diff contains only Product Source registration, contract,
ownership, delivery state, machine gate, validation and workflow evidence.
Before merged-main effectivity, implementation and successor PR creation are
false. After clean/synced merged-main effectivity, only the exact successor may
resolve repository edits, implementation and PR creation to true.
Implementation remains `NOT_STARTED`, target-runtime acceptance remains
`BLOCKED_BY_IMPLEMENTATION_DEFECT`, and capability movement is `NONE`.

---

## Product P2 Backend Acceptance Closure Candidate

`P2_POSITION_MONITORING_BACKEND_IMPLEMENTATION` is `COMPLETE` and effective on
merged main through PR #1169 / merge commit
`0aa67b5631a5450b215d6ce6a89474c687f68e70`. The merged implementation and
`docs/P2_POSITION_MONITORING_BACKEND_CAPABILITY_AUDIT.md` establish Schema V10,
independent entry-logic / conclusion / reversal / risk-reason semantics,
per-position risk, separate risk trend, verified-and-fresh monitor trust,
trusted provider consumption, explicit missing-data behavior, Dashboard Home
projection, and PostgreSQL migration validation.

This docs-only candidate records that backend subpackage result in
`docs/P2_POSITION_MONITORING_BACKEND_ACCEPTANCE_CLOSURE.md`. It does not make
the whole Product P2 package complete. Product P2 stays at
`FUNCTIONAL_UNVALIDATED` until frontend real-data integration, real and
historical monitoring scenarios, UI
acceptance, target-device validation, and the later required delivery evidence
are complete. The next package must not start before this acceptance closure is
reviewed and effective on clean/synced merged main. Auto trading, auto close,
auto reverse, Mobile, Figma, Three AI, and unrelated expansion remain blocked.

---

## Product-First Baseline Candidate

The P0 Product Foundation Freeze candidate is based on main
`2552dd24b1b756d5eb517e640baa772e1c5bcab6`. It registers the formal V1
architecture, Position Monitoring, AI Conflict/Confused/Push Recheck/Review,
final Home interaction, frozen Figma, and formal business-contract sources.
Product plans now precede current implementation, current UI, phase records,
Workflow, Governance, and tests as product authority.

This package changes product documentation and a minimal deterministic source
gate only. It does not change Java, business APIs, schema, business UI, Figma,
business tests, notification delivery, or trading capability. No business
module becomes complete because of P0.

PR #1156 is `CLOSED_PAUSED_TECHNICAL_DEBT` and was closed without merge.

- Closure reason: the unfinished governance parser work overlaps shared FE-04
  contracts and workflow dependencies, so Product First removes it from the
  active open-PR set without adopting its content.
- Resume condition: only when a real product regression demonstrates that the
  missing governance capability is necessary.
- Boundary: the PR remains unmerged, its exact Head and remote branch are
  preserved, and all eight findings remain unresolved. Its branch, stash,
  patch, and GitHub history are recovery evidence only, not current product or
  runtime truth. Resume requires a fresh comparison with the latest `main`.

The only action after this candidate is a Draft PR and independent review. P1
Home implementation, PR #1156 remediation, Telegram, external notification,
automatic notification, and trading remain blocked.

---

## FE-04 Figma Baseline Registration

The approved FE-04 baseline is frozen in `Trade Model Design System` and is
registered by `docs/FE04_POSITION_MONITORING_IMPLEMENTATION_FREEZE.md`.

Registered mobile frames are Home `296:2`, Position Monitor `296:3`, AI
Analysis `296:4`, Message Center `296:5`, Push Detail `296:6`, and Profile
`296:7`. Registered desktop frames are Dashboard `296:8`, Position Monitoring
`296:9`, AI Analysis `296:10`, Message Center `296:11`, and Profile `296:12`.
The registered component nodes are Asset Card `28:154`, Execution Plan Card
`31:23`, AI Role Card `35:97`, Position Monitor Card `32:26`, Message Card
`299:54`, and Push Detail Card `300:234`.

The design baseline is `FROZEN` and its repository registration is
`REGISTERED_ON_MAIN`. FE-04 frontend remains `IN_PROGRESS_PARTIAL`: FE-04A,
FE-04B, the bounded FE-04C read-only Position Monitoring package, and the
bounded FE-04D AI Analysis first package are effective on merged main. The
FE-04E Message/Push Contract Foundation is effective on merged main
`5ad8ddb24a8253180b3e2b0a34fec66b9928ace8` through PR #1154. Its privacy and
state remediation is also effective on merged main
`2552dd24b1b756d5eb517e640baa772e1c5bcab6` through PR #1155. That merged
boundary keeps shared `OPPORTUNITY` responses free of UserPosition- and
account-risk-derived fields, isolates cross-endpoint PushRecheck access, and
applies explicit Message/Push state validation. FE-04E UI and FE-04F remain
unimplemented. No Message/Push UI, schema, Figma node, notification delivery,
AI capability, or trading capability was authorized by either package.

---

## FE-02 through FE-04D First-Package Progression

The bounded frontend and prerequisite packages leading into FE-04D are
effective on merged main:

1. FE-02 Asset Detail is effective on merged main
   `654d3821ff7046037f2cd02bf5de645b4550f196` through PR #1137.
2. P3-H1 authoritative `analysisId` navigation is effective on merged main
   `e8bf2b66377cc2ef99c4aac2133d237e8d79bef0` through PR #1138.
3. FE-03 Analysis Detail is effective on merged main
   `76383725c19d038e0bf2065ae034b06b7f34b732` through PR #1140.
4. P3-H3 UserPosition ownership and authorization foundation is effective on
   merged main `d523dc3e69920d6dd80a0d49f344f86757eb7b9e` through PR #1141.
5. FE-04A Shell & Navigation and FE-04B Home Dashboard Integration are
   effective on merged main `aaf905b4f74ecafcf514aa34d7c06361461a0eb4`
   through PR #1146.
6. FE-04C read-only Position Monitoring is effective on merged main
   `cc39f0c6315812b1178427c29b8b422da511ba0d` through PR #1148.
7. FE-04D AI Analysis first package is effective on merged main
   `66362746fe3bd932061087bcd3496c5273cc218b` through PR #1151.

FE-04C provides Mobile and Desktop Position Monitoring, the frozen Position
Monitor Card projection, exact string-preserved `positionId`, owner-scoped
GET-only reads, existing monitor-log display, and loading/empty/error/partial/
missing fail-closed states. It preserves `OPEN`, `PARTIALLY_CLOSED`, and
`CLOSED` lifecycle semantics; `WAITING_MONITOR` remains a UI empty state rather
than a persisted monitor status. Missing or failed monitor-log reads do not
fabricate `WAITING_MONITOR`.

FE-04C does not add edit, manual close, partial-close UI, replay, trade
execution, automatic action, API expansion, or an automatic call to the
write-type monitor-run endpoint. Overall FE-04 therefore remains
`IN_PROGRESS_PARTIAL`.

### FE-04D AI Analysis First-Package Result

FE-04D implementation status is `COMPLETE_FIRST_PACKAGE`. The bounded package
is effective on merged main `66362746fe3bd932061087bcd3496c5273cc218b`
through PR #1151. It provides the Mobile and Desktop AI Analysis tab using the
existing selected-asset context, authoritative nullable `analysisId`, returned
summaries for exactly `GPT_FINAL`, `GEMINI_REVIEW`, and `GROK_CHALLENGE`, and
the existing FE-03 Analysis Detail route for deep detail.

The merged package preserves loading, empty, error, partial, and missing
fail-closed states. An AI role with `resultAvailable != true` exposes only its
status label and status message. Desktop refresh failure clears stale AI,
execution-plan, and FE-03-link state instead of restoring cached successful
Dashboard data.

Capability remains intentionally partial: market-asset search is unavailable,
authenticated watch-asset writes are blocked, and complete exact-analysis AI
provenance, eight-score detail, four-timeframe detail, and complete evidence
chains are not added or fabricated. PR #1151 introduces no API, schema, Figma,
AI backend, write, external-send, or trading capability.

### FE-04E Contract Foundation And UI Authorization

The FE-04E Message/Push Contract Foundation is
`CONTRACT_FOUNDATION_EFFECTIVE_MERGED_MAIN` on
`5ad8ddb24a8253180b3e2b0a34fec66b9928ace8` through PR #1154. It provides:

- exact string `messageId` and `pushId` identities;
- exact `sourceIdentity` for only `OPPORTUNITY` and `POSITION_RISK`;
- authenticated shared read-only opportunity reads;
- authenticated owner-scoped position-risk reads that inherit the P3-H3
  authorization boundary;
- a real UserPosition -> PositionMonitor -> risk-event -> message foundation;
- a read-only Push Detail GET contract that does not trigger Recheck, monitor
  run, delivery, or any mutation;
- explicit `READY`, `EMPTY`, `ERROR`, `MISSING`, and `PARTIAL` read states;
- fail-closed Push/Recheck validation and authoritative UserPosition symbol
  preservation.

The read-only UI readiness re-evaluation passed the message-list, Push Detail,
source-specific access, state-model, registered Figma, Telegram-boundary, and
capability gates. Subsequent P1 review found that the shared response still
carried private-risk-derived fields; merged PR #1155 closed that server-side
prerequisite. FE-04E Message/Push UI nevertheless remains `NOT_STARTED`. The P0
Product Foundation Freeze pauses implementation, and any future UI package must
be selected by the Product-First roadmap after P0 is independently reviewed and
merged.

Merged PR #1155 provides source-specific server-side read projections:

- `OPPORTUNITY` is
  `AUTHENTICATED_SHARED_PUBLIC_PROJECTION`. Its response contains only exact
  message/source/opportunity identity, a safe allowlisted public opportunity
  status, public timestamp, and public description. Its mapper projection does
  not select UserPosition, account-risk, position-risk, or private-risk-reason
  columns. Dashboard Home, Opportunity Log, and Message Detail use the same
  public opportunity projection policy. Public lifecycle, status, and
  readiness are calculated only from public opportunity data and remain
  identical across authenticated users.
- The serialized public `OPPORTUNITY` projection does not expose internal
  `pushId`, Recheck identity, Recheck existence/status, or a private-risk
  reference. Public opportunity evaluation and projection do not read
  PushRecheck, UserPosition, account risk, position risk, or private failure
  data, so private state cannot act as a public response oracle.
- `POSITION_RISK` is `OWNER_SCOPED_PRIVATE_PROJECTION`. Its risk and monitoring
  fields remain available only through exact current-user-scoped message,
  position, and monitor reads. A complete legal matching monitor projection is
  `READY`; an incomplete legal projection is `PARTIAL`; invalid, malformed, or
  contradictory data is `ERROR`; and a missing or inaccessible exact resource
  is `MISSING`.
- All raw user-facing PushRecheck reads, config/audit, summary/ops, trigger,
  and replay routes fail closed with `404` because persisted Push/Recheck rows
  do not carry the authoritative source-message-position-owner relationship
  needed for access. Their public service methods reject before repository
  reads or mutation. Dashboard `recheck-preview-status` also returns only a
  fail-closed status and does not read private Recheck or global ops data.
  Internal scheduled Recheck execution remains isolated behind a strict
  scheduler command and is not a user-facing read or mutation capability.
- The shared and private sources no longer use one response record that can
  carry both public opportunity and private risk fields.
- Across both source-specific projections, complete valid data maps to
  `READY`, incomplete valid data maps to `PARTIAL`, invalid or contradictory
  data maps to `ERROR`, missing or inaccessible exact resources map to
  `MISSING`, and only a successful empty collection maps to `EMPTY`.

This privacy boundary is effective on merged main
`2552dd24b1b756d5eb517e640baa772e1c5bcab6`. It changed the read API response
projection but added no new endpoint, mutation, schema, Message/Push UI,
notification send, or trading capability. Product-First P0 now stops further
implementation until its own baseline is independently reviewed and merged.

System notifications, Telegram, external send, automatic notification,
delivery acknowledgement, fabricated unread/message counts, fabricated
messages or Push data, frontend-generated identity, mutation, AI expansion,
and trading capability remain blocked. Figma example values without a real
returned field must be hidden or rendered fail closed; they must never become
static success data.

## P3-U2 iPhone Private Test App Foundation

P3-U1 is effective on merged main
`b7fb33d543927b6f770d6092fd6f5df3751f3d57` through PR #1133. P3-U2 is effective
on merged main `1e061d9cdb8d0e0722842f06be34c1bb6ddd8064` through stacked PRs #1134,
#1135, and #1136. The merged iPhone-only SwiftUI + `WKWebView` client reuses the
existing form-login, server-side Session, Cookie, CSRF, redirect, and logout
behavior without changing Java or backend authentication.

The merged P3-U2 chain adds strict environment/Info.plist base-URL configuration, private
LAN HTTP only in development, HTTPS-only production, no loopback/default host,
same-origin WebView navigation, system external HTTPS/mailto/tel handling,
default TLS validation, persistent default WebKit storage, native loading and
redacted error/retry UI, Xcode project/signing hygiene, 47 unit/security/project
tests, one UI launch test, and a passing iOS 26.5 iPhone 17 Pro Simulator
build/install/launch check. These changes are merged-main evidence. Deployed
HTTPS server, App Store, TestFlight, Ad Hoc, P4, and production readiness are
not claimed by the merge status.

See `docs/P3_U2_IPHONE_PRIVATE_TEST_APP.md`.

## P3-U1 Personal Login Page and Session Authentication

P3-U1 is effective on merged main
`b7fb33d543927b6f770d6092fd6f5df3751f3d57` through PR #1133.

The branch replaces the formal Basic-auth access path with a minimal personal
form login backed by Spring Security server-side Session and `tm_user`:

1. `GET /login` and CSRF-protected `POST /login` use the existing Thymeleaf stack;
2. BCrypt hashes, a unique normalized username, UTC-naive `created_at`, and success-only `last_login_at` are persisted;
3. login failure limiting is bounded to 1024 normalized usernames, locks after 5 failures for 15 minutes, expires, and resets on success;
4. browser routes redirect to login while APIs return sanitized JSON `401`;
5. logout is POST/CSRF-only, invalidates the Session, and deletes JSESSIONID;
6. Session fixation migration, 30-minute timeout, HttpOnly, SameSite=Lax, and prod Secure Cookie policies are explicit;
7. bootstrap requires explicit `TRADE_MODEL_INITIAL_USERNAME` and `TRADE_MODEL_INITIAL_PASSWORD`, is idempotent, and never overwrites an existing password;
8. existing Dashboard/Review browser writes now send the framework CSRF header.

Targeted security tests and the full 4099-test suite pass with 0 failures and 0
errors; 14 Docker/Testcontainers cases remain environment-gated. A bounded
localhost two-start file-H2 run proved generic failure, login, Session refresh,
logout, persistence, bootstrap no-overwrite, old-Session rejection after
restart, and zero password/hash matches in logs. The current branch additionally
updates the P3-H migration service and preflight to exact V8/`tm_user`, preserves
business-data read-only privileges with only bounded authentication writes, and
migrates current production smoke/release-gate authentication from Basic Auth
to form login, Session Cookie, CSRF logout, and post-logout invalidation. The
exact-Head disposable P3-H run passed canonical Flyway V1-V8 and `tm_user` on
local PostgreSQL 16 and passed the local browser-equivalent Session/CSRF smoke.
These changes are effective on merged main; real production PostgreSQL V8, real mobile
Safari/Chrome, real reverse-proxy Session/CSRF, real staging, Secret Store
injection/rotation, and production deployment were not run.
Production readiness remains `BLOCKED`. FE-02, P3-H1, FE-03, P3-H3, FE-04A,
FE-04B, FE-04C, the bounded FE-04D first package, and the FE-04E Contract
Foundation and PR #1155 privacy/state boundary are effective on merged main.
The FE-04 Figma baseline is registered on main. Message/Push UI remains
unimplemented and is not authorized by those merged backend packages. The P0
Product Foundation Freeze is the current work package; after P0 is merged, only
the Product-First P1 Home audit may begin. System notifications, Telegram,
external/automatic notification, fabricated UI data, market search, watch-asset writes,
unsupported deep analysis, unrelated API expansion, delivery/write actions,
FE-04F, P4, and production deployment remain blocked.

See `docs/P3_U1_PERSONAL_LOGIN_SESSION_AUTH.md`.

## P3-CALL1 Unified Provider Orchestration

P3-CALL1 is effective on merged main
`cac3d5ea139e26278cf5cf722975830099c23f65` by PR #1131. The package adds an
offline, default-disabled coordination foundation:

1. canonical spot/perpetual instrument identities and explicit provider symbol mappings;
2. strict `P0_POSITION > P2_CANDIDATE > P1_WATCHLIST > P3_DISCOVERY` planning;
3. replaceable watchlist, bounded configured discovery, and runtime auto-candidate stability;
4. user `AUTO/LOW/STANDARD/HIGH` profiles plus per-asset system HIGH/EMERGENCY escalation;
5. stable frequency-matrix versioning and independent dataset cadence;
6. global/provider/symbol budgets, reserved emergency capacity, provider/AI concurrency, health, circuit, Retry-After, cache, and single flight;
7. read-only snapshot query separated from controlled refresh;
8. NoCall provider/AI adapters, AI checkpoint policy, notification eligibility/dedup, and no external send;
9. authenticated profile/runtime-status APIs and a minimal Dashboard profile control.

P3-CALL1 does not call Binance, CoinGlass, external context, or AI; it does not
start a business scheduler, send Telegram/Push, create or mutate a position,
create an order, or trade. Its runtime auto-candidate owner remains in-memory.
Dynamic discovery, live adapters, Decision Cutoff Time, real AI, and Telegram
delivery are explicitly deferred. P4 remains disallowed and Production
Deployment Readiness remains `BLOCKED`. The reviewed correctness closures now
effective on main include:

1. physical provider work uses a dedicated bounded executor, and logical
   timeout cannot release its concurrency lease or Single Flight before the
   physical task actually ends;
2. stable `ProviderSnapshotKey` identity is separate from consumer TTL and
   refresh `timeBucket`, enabling cross-profile sharing, cross-bucket stale
   fallback, and bounded retention;
3. OHLCV minimum gaps include timeframe, so one due scan can independently
   attempt `5m`, `15m`, `1h`, and `4h` without one timeframe blocking another;
4. candidate confirmation uses `CandidateLogicIdentity`, while evidence hash
   remains provenance and notification deduplication;
5. SPOT and PERPETUAL identities remain exact end to end; unsupported
   perpetual price/OHLCV fails `NOT_CONFIGURED` and never falls back to spot.
6. caller wait timeout and interrupt cannot cancel, retry, or remove a shared
   physical flight; only the owner-fixed physical timeout supervisor owns
   attempt cancellation;
7. local queue, budget, concurrency, minimum-gap, disabled, and not-configured
   outcomes do not increment remote provider circuit failures or mark remote
   health down; `429` applies Retry-After without opening the circuit;
8. persisted OHLCV due-state reads bind provider and market type, so a recent
   `SPOT` row cannot suppress `USDT_PERP` refresh or create false perpetual
   `READY`; all four timeframe observations preserve market identity.
9. every physical attempt owns an idempotent circuit permit; local rejection
   releases a HALF_OPEN probe, `429`/auth settles remote reachability, remote
   failure reopens, success closes, and waiter timeout cannot release the
   owner's token. Completed fixture paths leave zero permanently claimed
   HALF_OPEN probes.
10. dataset retention is checked before caller freshness; a caller TTL or
    metadata expiry cannot exceed `staleUntil`, the exact boundary is removed,
    and shorter/longer consumers continue sharing the stable key only while the
    dataset remains retained.
11. `currentUniverse/currentPlan` are side-effect-free status paths, while
    `evaluateUniverseForExecution/planForExecution` are Scheduler-only. A real
    scan evaluates each relevant asset once, all due datasets reuse that
    profile, and only real scan cycles advance recovery or write changed
    transition audit. One hundred status reads leave state unchanged.
12. `evaluate`, `current`, and `currentProfile` use the same service monitor.
    A read cannot complete while a changed transition is paused inside audit,
    and completed execution state is published as one coherent profile,
    reason, effective time, downgrade time, and rule-version snapshot.
    Thirty-two concurrent readers repeating 100 queries produce zero mixed
    snapshots, state mutations, or additional audit rows.
13. optional Watchlist and Discovery count failures are isolated to count `0`;
    runtime-status remains readable and continues using the side-effect-free
    current plan without Provider, AI, transition, or audit mutation.
14. transition evaluation mutates a complete staged State copy, requires an
    audit insert count of exactly one, and publishes only after audit success.
    Audit exception, zero rows, or unexpected rows preserve the entire prior
    State and stop Scheduler refresh before any dataset call.
15. each Provider attempt uses atomic `QUEUED`, `LOCAL_ADMISSION`, and
    `REMOTE_IN_FLIGHT` phases. Queue and pre-remote timeouts remain local,
    cause zero remote health/circuit/retry/adapter effects, and a pure queue
    timeout consumes zero attempt budget. `PROVIDER_TIMEOUT` is reserved for a
    timeout after Adapter start wins the phase race.
16. cancellation of a queued task removes its exact control from the bounded
    executor queue before another admission can consume the slot. Twenty
    repeated P3 queue timeouts leave zero queued controls and zero queue growth;
    P0 reserved admission remains available. Running cancellation only requests
    interruption and cannot remove another request's queued control.
17. Shared Flight shares one physical request, retry, budget, circuit, health,
    and cache-write lifecycle, while every waiter result is rebuilt from its own
    cache lookup. Waiter trace, priority, profile, reason codes, frequency
    version, TTL, metadata, and request audit are caller-owned; additional
    Provider calls, budget reservations, circuit settlements, remote-health
    mutations, and physical-attempt audits are zero.
18. READY, `EMPTY_CONFIRMED`, cache-hit, stale-fallback, read-only peek, and
    waiter metadata expiry use the earlier caller-TTL and dataset-retention
    boundary. Long TTLs cannot outlive retention, short TTLs remain short, exact
    boundaries are preserved, overflow falls back to retention, and a short
    Owner expiry does not permanently cap a later caller.

Each physical retry receives its own budget reservation and audit lifecycle.
These are fixture-only branch results: real provider calls, real AI calls,
Telegram sends, orders, and trading remain zero. Only reviewed merged main may
make the package effective.

Details: `docs/PROVIDER_CALL_ORCHESTRATION_AUDIT.md` and the P3-CALL1 contract
documents.

---

## Controlled Staging P3-H

Merged main `230528b0942737275a397323bcfff874541e2ea8` includes the reviewed
P3-H offline Harness from PR #1129. The separate real-staging/lab PR #1130 is
frozen and is not part of P3-CALL1. Round 1 closed the offline
template gaps for deterministic Greenfield bootstrap, four-role provisioning,
fixed non-root Secret materialization, strict attestations, systemd
credentials, runtime mount verification, Host-header rejection, TLS target
binding, and TLS 1.3 behavior. Round 2 adds explicit Greenfield/steady-state
modes, retained-volume and reboot-like restarts, Flyway checksum/V7 validation,
V2 activation persistence, failed-start cleanup, strict object inventory,
exact SSH-line pinning, exact committed Git archive builds, and systemd-only
lifecycle ownership.

Round 3 adds a separately confirmed `RECOVER_GREENFIELD_INITIALIZATION` path
for checksum-valid continuous Flyway prefixes and V7 pre-grant states, with
zero business rows and no unknown objects. It separates core state from full
read-only verification, measures failed-start cleanup as zero project
containers while preserving Primary/Backup volumes, rejects app/backup role
memberships and unsafe default/Sequence ACLs, and re-proves V2 admin/database
success plus V1 denial after the reboot-like restart.

Round 4 adds exact full-row rule-default contracts for every Flyway prefix,
normalized exact V1-V7 PostgreSQL schema fingerprints, effective/PUBLIC/column
read-only privilege checks, and strict staging DNS/SSH host/deployment-user
grammar. Disposable PostgreSQL 16 mutation fixtures prove rule, schema, and
privilege drift is rejected; injection fixtures prove malformed inputs stop
before source upload or network access.

Every required controlled server and Secret Store input was absent. The
default runner returned `BLOCKED_MISSING_CONTROLLED_STAGING_INPUT` before any
network or secret access. Separately, an explicitly enabled disposable local
Compose run completed empty PostgreSQL, role bootstrap, Flyway V1-V7,
retained-volume steady and reboot-like restarts with zero migrations and
matching fingerprints, V2 persistence/V1 denial, injected-failure cleanup,
read-only grants, non-root Config Tree, app/proxy health, Host/TLS checks,
denied writes, leak checks, and cleanup as
`PASS_LOCAL_DISPOSABLE_P3H_TEMPLATE_SMOKE`. `SERVER_ACCESS` and
`SECRET_ACCESS` are `NOT_ATTEMPTED`; real staging remains
`BLOCKED_MISSING_AUTHORIZED_INPUT`, and no real-server status is PASS.

P3-H offline evidence is not production deployment and real staging remains
blocked by missing authorized input. P4 is not allowed, Production Deployment
Readiness remains `BLOCKED`, and production deployment cannot proceed. See
`docs/CONTROLLED_STAGING_READONLY_TLS_SECRETSTORE_P3H.md`.

---

## Greenfield P3-G First-Boot Rehearsal

Merged main `8f0640331e58e8b8b657c7db08e6d79b03d37a4f` makes the approved
Greenfield provenance decision and P3-G evidence effective. P3-G records
`PASS_LOCAL_CONTROLLED_GREENFIELD_REHEARSAL` for a disposable localhost
PostgreSQL 16 environment:

1. A genuinely empty Primary database migrated through Flyway V1-V7; repeat
   migrate applied zero migrations and final version was 7.
2. Runtime business rows remained zero. The exact versioned migration seed
   allowlist is `tm_rule_config=59`.
3. The repository's `prod-backup.sh` and `prod-restore.sh` ran in a
   digest-pinned PostgreSQL 16 Ops container.
4. Primary and Recovery structure, full-content, Flyway history, schema-type,
   sequence, and historical-inventory evidence matched.
5. The exact committed application image ran as non-root with a read-only
   database role against Primary, Primary restart, and Recovery; write probes
   were denied and application reads did not change content fingerprints.
6. All AI/provider external calls, schedulers, trading paths, and external
   sends remained disabled on an internal Docker network.
7. Reviewer Round 1 closes the release-gate split-smoke inheritance bypass,
   labels split smoke as local-only, and validates empty asset/system cards
   against directional, opening, high-confidence, and Hot Reset false positives.
8. Reviewer Round 2 restricts empty market bias to `WAIT`/empty, rejects
   non-enum asset states, requires real JSON array/object shapes, and validates
   the exact four no-data timeframes plus field-level no-conclusion semantics.

This is local operational evidence, not server deployment, secret-store,
live-provider, or production-readiness evidence. P4 is not allowed. Details:
`docs/GREENFIELD_POSTGRESQL_FIRST_BOOT_REHEARSAL_P3G.md`.

---

## P3 Greenfield Provenance Closure

Merged main `c94c99dfa72843e558ac4ce87037bfe71bd5dfaf` remains the effective P2.1
PostgreSQL/Flyway V7 evidence baseline. The P3 branch adds a guarded localhost
runner, deterministic generated V6 fixture, aggregate-only
fingerprint/verification SQL, offline safety tests, and evidence/status
documentation.

Release Owner / Data Owner decision
`TMV1-GREENFIELD-20260715-001` approves:

- `DATA_PROVENANCE_MODE: GREENFIELD_NEW_DATABASE`;
- no historical business data must be preserved;
- no existing formal business database exists; and
- the go-live database initial state is `EMPTY`.

P3.1 result remains `PASS_GENERATED_RELEASE_LIKE_REHEARSAL`.

The deterministic generated dataset completed source/recovery inventory and
fingerprint matching, PostgreSQL 16 container-native backup/restore,
V6-to-V7 migration, historical-time inventory, fail-closed application smoke,
and zero unexpected business writes. Its source status is
`GENERATED_RELEASE_LIKE_NOT_SANITIZED_CLONE`; it proves the harness and
generated rehearsal only. Local H2, test fixtures, and generated PostgreSQL
data are not production history.

The P3.2 sanitized-clone route and final gate are
`NOT_APPLICABLE_BY_APPROVED_GREENFIELD_DECISION`, not PASS. No fake sanitized
clone will be created. The retained sanitized-clone support code remains
available for recovery/incident use or a future separately approved migration
mode.

PR #1127 and P3-G are merged/effective on main. P3-H is the current scoped
follow-up package and is blocked before server access because its controlled
inputs are missing. P4 cannot start and production deployment cannot proceed.

Details: `docs/GREENFIELD_DATABASE_PROVENANCE_DECISION.md` and
`docs/POSTGRESQL_CURRENT_STATE_CLONE_REHEARSAL_P3.md`.

---

## AI-E2E-1 Controlled Three-Provider Parallel Live Evidence

AI-E2E-1 status is
`EFFECTIVE_MERGED_IMPLEMENTATION_WITH_OPERATOR_LIVE_EVIDENCE` on merged `main` commit
`eacc224f23f8a63a1294bed4813a0aec5c5614bf`.

Three operator-controlled runs completed with `PASS_3_OF_3`. OpenAI, Gemini, and xAI each returned
HTTP 2XX and passed strict parsing once per run through the formal bounded parallel orchestrator.
The nine total calls produced zero failure, zero timeout, zero partial fallback, and zero global
deadline exceedance. Deterministic GPT / Gemini / Grok result ordering was preserved. Average
orchestration latency was approximately 9,168 ms, with an observed range of 7,239-10,550 ms.

This package closes the fixed-fixture controlled live evidence only. It does not prove sustained
availability, production-load concurrency, monthly cost, quota behavior, AI correctness,
directional accuracy, profitability, or production deployment readiness. Production Deployment
Readiness remains `BLOCKED`.

Remaining gates include sustained soak testing, cost-budget validation, rate-limit and quota
validation, real business-chain E2E beyond the fixed review fixture, and an approved production
rollout and rollback plan.

Detailed evidence: `docs/AI_E2E_CONTROLLED_PARALLEL_LIVE_HARNESS.md`.

---

## P1 Dashboard Stress Test Plan & Harness Preparation

P1 Dashboard Stress Test Plan & Harness Preparation is merged/effective historical preparation after P0 backend/frontend alignment closure. It prepared a local-only dashboard stress-test plan, guarded dry-run harness, and evidence template. It did not execute stress traffic.

Current P1 preparation status:

1. P0 blockers are closed by `docs/P0_BACKEND_FRONTEND_ALIGNMENT_CLOSURE_REVIEW.md`.
2. `STRESS_TEST_PREPARATION_ALLOWED: YES`.
3. `STRESS_TEST_EXECUTION_ALLOWED: NO` for this preparation PR.
4. Default target is local only: `http://localhost:8081`.
5. Allowed endpoints are only `GET /actuator/health`, `GET /dashboard`, and `GET /api/dashboard/home`.
6. The harness requires `DASHBOARD_STRESS_CONFIRM=YES` before any future execution and defaults to dry-run.
7. No production server, production DB, provider endpoint, write endpoint, Push/Recheck/Telegram send, order execution, auto-open, auto-close, auto-reverse, or auto-trading behavior is allowed.

Production readiness remains BLOCKED and production deployment cannot proceed.

Next recommendation after P1 preparation: run a separate explicitly approved local dashboard stress execution package, or continue scoped remediation/business work. The next package must not be production deployment.

---

## P2 V1 Business Stress Test

P2 V1 Business Stress Test is merged/effective on main by PR #1104. It verifies opportunity discovery, manual execution-plan completeness, paper/manual position monitoring usefulness, Push Recheck freshness validation, and closed-position exclusion without production access, provider calls, external Push/Telegram send, or trading/order behavior.

Current P2 stress status:

1. Deterministic `SYNTHETIC_SCENARIO_DATA` is used for opportunity and monitoring scenarios.
2. `V1BusinessStressTest` covers bullish, bearish, no-trade, high-risk, confused/conflict, price-drift, monitor-valid, monitor-weakened, reversal, take-profit, stop-zone, and closed-position scenarios.
3. `scripts/v1-business-stress-local.sh` defaults to dry-run and requires `V1_BUSINESS_STRESS_CONFIRM=YES` before running the local JUnit harness.
4. No production server, production DB, provider endpoint, write endpoint, Push/Recheck send, Telegram send, order execution, auto-open, auto-close, auto-reverse, or auto-trading behavior is allowed.
5. Production readiness remains BLOCKED and production deployment cannot proceed.
6. P2 deterministic business stress result is PASS.

Next recommendation after P2: run P3 local historical/replay-style validation while keeping synthetic fixture claims separate from real historical/provider evidence.

---

## P3 Historical Market Replay Business Validation

P3 is merged/effective on main by PR #1105. It feeds clearly labeled in-memory OHLCV paths through the existing Decision Engine, review-only execution-plan boundary adapter, Push Recheck, and Position Monitor services.

Current P3 status:

1. Replay source is `LOCAL_REPLAY_FIXTURE_NOT_PROVIDER`; no real local historical CSV/JSON fixture exists.
2. At least nine decision/recheck paths and six paper-monitor outcomes cover uptrend, downtrend, fake breakout, range, wick, crash/rebound, pullback, high-risk, drift, TP, stop, reversal, and closed-position exclusion.
3. Focused opportunity, execution-plan, and position-monitor assertions pass for the local replay contract.
4. Package-level result remains `PARTIAL` because synthetic replay is not real historical data and the production system lacks a first-class candle replay session.
5. No provider, production server/DB, external send, order execution, auto-open, auto-close, auto-reverse, or auto-trading surface is used.
6. Production readiness remains BLOCKED and production deployment cannot proceed.

Next recommendation after P3: add a versioned real local historical fixture and direct replay adapter in a separately scoped local-only package, or remediate the replay gaps recorded in `docs/V1_HISTORICAL_REPLAY_VALIDATION_EVIDENCE.md`.

---

## P4 Versioned Real Local Historical Fixture + Direct Replay Adapter

P4 is the current local-only real-history evidence package. Its first mandatory discovery gate found no fixture in either repository allowlist directory and no `V1_REAL_HISTORICAL_FIXTURE_DIR` environment path.

Current P4 status:

1. `REAL_HISTORICAL_FIXTURE_STATUS: MISSING`.
2. `REAL_HISTORICAL_REPLAY_RESULT: BLOCKED_MISSING_REAL_FIXTURE`.
3. A versioned missing-state manifest and fixture contract are committed without raw candle data or invented provenance.
4. Test-support CSV integrity, hash, gap reporting, monotonic replay clock, label isolation, and no-lookahead guards are prepared.
5. The actual Evidence -> Score -> Decision -> Plan -> manual paper position monitor replay is not run and no final business output is constructed directly.
6. No provider, production server/DB, order, external send, auto-open, auto-close, auto-reverse, or auto-trading surface is used.
7. Production readiness remains BLOCKED and production deployment cannot proceed.

Next recommendation after P4: provide a provenance-backed real fixture through one of the three allowed paths, review redistribution status, populate the manifest/hash, and then add the smallest time-bounded full-pipeline replay input seam.

---

## BIZ-1 CoinGlass Derivatives Business Integration

BIZ-1 is implemented on the current branch and remains pending merge/effective review. It consumes only the shared cached CG-1 snapshot and makes zero live provider calls.

Current BIZ-1 status:

1. OI, Funding, liquidation, crowding, concentration, partial, stale, and unavailable states become source-traced evidence without converting missing values to zero.
2. Derivatives inputs contribute bounded deltas to all eight scores; the complete eight-score set contributes a capped `-10..+10` decision-score adjustment.
3. The formal direction remains 4h-owned; 5m/15m/1h/4h require at least three aligned timeframes and 4h/1h agreement.
4. CoinGlass can confirm, downgrade, raise risk, block confirm, or request manual review. It cannot independently flip direction.
5. Asset state, internal Push/Recheck, execution-plan readiness, manual-position monitor reasons, Confused inputs, Hot Reset candidate input, and Dashboard Home use the same cached assessment.
6. Plan price boundaries remain OHLCV-owned. UserPosition creation/status mutation, external Push/Telegram send, order execution, and auto-trading are absent.
7. Nineteen derivatives rule defaults are aligned between H2 bootstrap and PostgreSQL Flyway V6.
8. Deterministic focused coverage exceeds forty BIZ-1 scenarios; full-suite and delivery validation remain required before draft PR handoff.
9. Real CoinGlass live smoke is not run and live provider calls are zero.
10. Production readiness remains BLOCKED and production deployment cannot proceed.

Detailed contract: `docs/COINGLASS_DERIVATIVES_BUSINESS_INTEGRATION.md`.

---

## CG-1 CoinGlass v4 Provider Adapter

CG-1 is merged/effective on main by PR #1112. It implements the source, normalization, cache, rate-budget, health, and trace boundary for CoinGlass v4.

Current CG-1 status:

1. Official v4 documentation verifies the OI exchange list, OI-weighted funding history, aggregated liquidation history, and Binance global account ratio contracts.
2. OI, Funding, liquidation, and long/short ratio use four independent request keys, caches, freshness/error states, and endpoint capability IDs under the shared CoinGlass provider budget.
3. `DefaultProviderDatasetRefreshPort` routes aggregate derivatives refresh through `CoinGlassDerivativesSnapshotService` and `ProviderCallCoordinator`.
4. Partial data is `DEGRADED`; missing values remain null; stale time remains `STALE`; disabled/missing-key states make no transport call.
5. CoinGlass and external calls remain default-off. No key is present in the current environment, so live smoke is `SKIPPED` and live calls are zero.
6. The normalized snapshot itself remains judgment-free; BIZ-1 owns downstream business interpretation.
7. BIZ-1 is now separately implemented on its own branch and does not change CG-1 provider-call safety.
8. Production readiness remains BLOCKED and production deployment cannot proceed.

---

## Post-1066 Status Closure

PR #1066 is merged into `main` by merge commit `694c68d8418a207ac54c825f6c8e7e63f0853859`.

Post-merge local validation is recorded as passed:

1. `./mvnw test -q` PASS.
2. `bash scripts/v1-delivery-check.sh` PASS.
3. `V1_STATE_RESULT` PASS.
4. `WORKTREE_CLEAN` Yes.
5. `MAIN_SYNC` OK.
6. `OPEN_PR_STATUS` NONE.
7. `BLOCKERS` none.

Current package status after #1066:

- Review + AI conflict package: current round DONE / usable, progress 86%.
- Position monitor package: current round DONE / usable, progress 83%.
- Overall project real progress: 74%.
- Project real status: V1 local acceptance-ready, not production-ready.
- Production readiness remains BLOCKED.
- Next business phase allowed: YES.

The following prohibited items remain outside V1 scope:

- no auto-open
- no auto-close
- no auto-reverse
- no order execution
- no auto-trading
- no external push send
- no fake positions
- no fake review records


## PDR-PF2 Production Scheduler Policy

PDR-PF2 is DONE/effective on merged main. It defines production scheduler policy, requires explicit production scheduler classification, and keeps production deployment fail-closed unless each scheduler is explicitly approved.

Current production scheduler policy status:

1. `application-prod.yml` exposes production scheduler flags as explicit default-off settings.
2. `ProductionProfileSafetyGuard` rejects missing production scheduler policy and missing scheduler classifications.
3. `LOCKED_DOWN` mode requires all production schedulers to stay disabled.
4. `EXPLICIT_OPT_IN` mode requires scheduler-specific `PROD_ALLOWED_EXPLICIT_OPT_IN` classification before an effective scheduler can run.
5. Position Monitor scheduler remains default-off and production guard rejects enabling it in this package.
6. Production readiness remains BLOCKED and production deployment cannot proceed.

Next recommended remediation package after PDR-PF2: `PDR-PF3 PostgreSQL Migration Evidence`.

---

## Effective State Rule

Compatibility note: `scripts/v1-state.sh` still prints `CURRENT_PHASE: P0-0` as the contract-baseline phase. The active delivery handoff is tracked by `Current Work Package`, `Next Business Phase`, and the Delivery Progress Matrix.

P0-0 is effective only because its DONE commit is merged to `main`, local `main` is synced, and the worktree was clean when the runtime gate evaluated it.

P0-1 UserPosition is effective because its implementation is merged to clean / synced `main`.

P0-2 ExecutionPlan Source Gate is effective because its implementation is merged to clean / synced `main` and the runtime gate allowed P0-3.

P0-3 AccountRisk integrates UserPosition is effective because its implementation is merged to clean / synced `main` and the runtime gate allowed P0-4.

P0-4 PositionMonitorLog is effective because its implementation is merged to clean / synced `main` and the runtime gate allowed P0-5.

P0-5 PositionMonitorService is effective because its implementation is merged to clean / synced `main` and the runtime gate allowed P0-6.

P0-6 Review integrates UserPosition is effective because its implementation is merged to clean / synced `main` and the runtime gate allowed P1-1.

P1-1 PushRecheck semantic hardening is effective because its implementation is merged to clean / synced `main` and the runtime gate allowed P1-2.

P1-2 ConfusedState + AiConflict hardening is effective because its implementation is merged to clean / synced `main` and the runtime gate allowed P1-3.

P1-3 HotReset real action is effective because its implementation is merged to clean / synced `main` and the runtime gate allowed P1-4.

P1-4 OpportunityLog is effective because its implementation is merged to clean / synced `main` by PR #1017 and the runtime gate allowed P2-1.

P2-1 Macro / News / External Context is effective because its implementation is merged to clean / synced `main` by PR #1018 commit `d7fef874b39aabbd07f6b05fd97f4725e89e79b5` and the runtime gate allowed P2-2.

P2-2 AI Orchestrator + AiCallLog is effective because its implementation is merged to clean / synced `main` by PR #1019 commit `92fd7cbf17db31c8ea2bfd4673badde1c69d20cd` and the runtime gate allowed P2-3.

P2-3 Scheduler / Idempotency / Trace is effective because its implementation is merged to clean / synced `main` by PR #1020 commit `5c2b2b47eb7fa4cfc9c428ef022375f4ca890b23` and runtime state allowed P3-1 to proceed.

P3-1 Dashboard Final is effective because its final homepage UI layout is merged to clean / synced `main` by PR #1023 commit `f543832cf5907fe00920ca3f05666566daa16b7a`, full Maven validation passed, and the merged PR changed only `src/main/resources/templates/dashboard.html`.

P3-2 Full E2E Acceptance is effective because its acceptance evidence is merged to clean / synced `main` by PR #1025 Dashboard Manual UserPosition Binding, PR #1027 Full UserPosition lifecycle E2E acceptance, and PR #1028 Dashboard E2E state proof / commit `1b08abd`. Full Maven validation, delivery check, and `v1-state` pass on clean / synced main.

P3-3 Final Delivery & System Freeze is effective because final docs/status closure now records the local acceptance-ready freeze state on clean / synced `main`: full Maven validation passed, delivery check passed, `v1-state` passed with blockers none, `/api/dashboard/home` returned HTTP 200 success with the expected Dashboard Home shape, and `/api/review/center` returned HTTP 200 success with the expected Review Center shape.

---


## PDR-PF3 PostgreSQL Migration Evidence

PDR-PF3 is DONE/effective on merged main by PR #1071. It records PostgreSQL migration evidence for current Flyway files without changing business runtime behavior.

Current PDR-PF3 evidence status:

1. Reviewed migration files: `V1__baseline_schema_tables.sql`, `V2__baseline_schema_indexes.sql`, and `V3__scheme_rule_config_defaults.sql`.
2. Static scan found no H2-only `AUTO_INCREMENT`, `MERGE INTO`, `ON DUPLICATE`, `DATEADD`, `FORMATDATETIME`, or `CLOB` usage inside Flyway migration SQL.
3. `schema.sql` remains H2 local/test bootstrap and still contains H2-specific syntax; it is not production Flyway migration SQL.
4. `PostgreSqlFlywayMigrationSmokeTest` is present and designed to verify empty PostgreSQL migration with Testcontainers.
5. Empty PostgreSQL migration result is `BLOCKED_TIMEOUT`: the PostgreSQL migration evidence run lasted approximately 1h27m before manual interruption and produced no completed trustworthy migration success log.
6. Production readiness remains BLOCKED and production deployment cannot proceed.

Next recommendation: first resolve Docker/Testcontainers availability or rerun PDR-PF3 in a controlled server-backed PostgreSQL test environment. Proceed to `PDR-PF4 Current-State Migration + Rollback Drill` only after empty migration evidence is PASS.

---

## PDR-PF4 Current-State Migration + Rollback Drill

PDR-PF4 is DONE/effective on merged main by PR #1072. It defines safe current-state migration and rollback rehearsal requirements without accessing production DB, running destructive DB operations, changing runtime behavior, or claiming production readiness.

Current PDR-PF4 status:

1. `docs/CURRENT_STATE_MIGRATION_ROLLBACK_DRILL.md` defines required preconditions, backup plan, restore drill plan, current-state migration rehearsal plan, rollback decision tree, evidence bundle, and safe staging/server-backed command templates.
2. Existing `scripts/prod-backup.sh` requires explicit database environment variables and has no hardcoded DB URL or secrets.
3. Existing `scripts/prod-restore.sh` requires explicit restore environment variables and refuses to run without `RESTORE_CONFIRM=I_UNDERSTAND_RESTORE_CAN_OVERWRITE_DATA`.
4. Existing `scripts/prod-release-gate.sh` does not run restore automatically and keeps release-gate status incomplete until restore/human evidence exists.
5. No production DB was accessed and no destructive DB operation was run by this package.
6. Production readiness remains BLOCKED and production deployment cannot proceed.

Next recommendation: collect PDR-PF4 evidence in a safe staging/server-backed PostgreSQL environment, or first resolve Docker/Testcontainers/PostgreSQL availability. After backup, restore, migration, rollback, and smoke evidence exists, continue to the next explicitly scoped remediation package.

---

## PDR-PF5 Secrets and Access Hardening

PDR-PF5 is DONE/effective on merged main by PR #1073. It defines production secrets/access hardening requirements and safe guard evidence without accessing real secrets, production servers, or production DB.

Current PDR-PF5 status:

1. `docs/SECRETS_AND_ACCESS_HARDENING.md` records existing secret-related guards, missing hardening evidence, required env vars, secrets manager / rotation plan, HTTPS / reverse proxy checklist, audit/access logging checklist, rate limiting checklist, actuator exposure policy, and prohibited secret handling.
2. Existing `ProductionProfileSafetyGuard` rejects missing/unsafe production datasource, admin, Binance, public bind, actuator, and scheduler policy settings.
3. AI provider secrets are required only when the matching provider is explicitly enabled.
4. Existing scripts avoid printing passwords and default live provider smoke to skipped/no external calls.
5. No real secrets were accessed, no production server was accessed, and no production DB was accessed by this package.
6. Production readiness remains BLOCKED and production deployment cannot proceed.

Next recommendation: implement or collect evidence for secrets manager integration, credential rotation, HTTPS/reverse-proxy hardening, access/audit logging, and rate limiting before any release-gate decision.

---

## PDR-PF6 Provider Live Smoke Evidence

PDR-PF6 is DONE/effective on merged main by PR #1074. It records provider live-smoke readiness evidence, safe default-disabled smoke behavior, provider result statuses, and redaction policy without accessing real secrets or making unapproved live external calls.

Current PDR-PF6 status:

1. `docs/PROVIDER_LIVE_SMOKE_EVIDENCE.md` records provider paths reviewed, safe no-call smoke output, result per provider, redaction policy, remaining blockers, and blocked production readiness.
2. `scripts/prod-provider-smoke.sh` defaults to `PROVIDER_SMOKE_ENABLE_EXTERNAL_CALLS=false` and returns SKIPPED without provider network calls.
3. Binance public, OpenAI, Gemini, xAI/Grok, and external context provider evidence is recorded as skipped/disabled by default unless explicitly approved and configured in a safe environment.
4. No real secrets were accessed, no secret values were printed, no production server was accessed, and no production DB was accessed by this package.
5. Production readiness remains BLOCKED and production deployment cannot proceed.

Next recommendation: `PDR-PF7 Push Recheck Quote-Unavailable Guard`, or a separately scoped controlled-server provider evidence rerun if secrets manager / server env exists and the operator explicitly approves live external calls.


---

## PDR-PF7 Push Recheck Quote-Unavailable Guard

PDR-PF7 is DONE/effective on merged main by PR #1075. It adds focused Push Recheck quote-unavailable guard tests and records evidence that missing `currentPrice` plus unavailable quote data fails closed as review-only/non-executable.

Current PDR-PF7 status:

1. `PushRecheckServiceImplTest` covers `Optional.empty`, null `lastPrice`, quote client exception, missing snapshot symbol, and provided valid current price behavior.
2. Missing quote paths write `fail_reason_json.code = QUOTE_UNAVAILABLE` and `RecheckStatusEnum.INVALIDATED`.
3. Missing snapshot symbol writes `fail_reason_json.code = PRICE_REQUIRED` and `RecheckStatusEnum.INVALIDATED`.
4. Fail-closed paths update push status to `RECHECK_INVALIDATED` and keep `RecheckResult` review-only, non-executable, not order execution, not auto-trading, not user-position creation, and not position mutation.
5. No production code change was required; this package is test/docs/status-source guard evidence.
6. Production readiness remains BLOCKED and production deployment cannot proceed.

Next recommendation after PF7: run PDR-PF8 Production Release Gate Closure to aggregate PF1-PF7 evidence and record the final release-gate decision.

---

## PDR-PF8 Production Release Gate Closure

PDR-PF8 is DONE/effective on merged main by PR #1076. It aggregates PF1-PF7 production-readiness evidence and records the final production release-gate decision without changing runtime behavior.

Current PDR-PF8 status:

1. `docs/PRODUCTION_RELEASE_GATE_DECISION.md` records the gate decision as `BLOCKED` and production deployment decision as `DO NOT DEPLOY`.
2. PDR-PF3 PostgreSQL empty migration evidence remains `BLOCKED_TIMEOUT` and not proven.
3. PDR-PF4 current-state migration plus rollback drill is documented but not executed against a production-like database.
4. PDR-PF5 secrets/access hardening is documented but lacks a complete secrets manager, rotation, HTTPS/reverse-proxy, audit logging, and rate limiting evidence bundle.
5. PDR-PF6 provider live smoke was `SKIPPED_DISABLED_BY_DEFAULT` for that release-gate closure; later LIVE8 adds controlled Binance public `PASS`, while AI/external provider proof remains missing.
6. PDR-PF7 quote-unavailable guard is PASS safety evidence, but it is not enough to unlock production deployment.
7. Production readiness remains BLOCKED and production deployment cannot proceed.

Next recommendation after PF8: `PDR-PF9 PostgreSQL Migration Evidence Recovery`.

---

## PDR-PF9 PostgreSQL Migration Evidence Recovery

PDR-PF9 is DONE/effective on merged main by PR #1077. It reran only a bounded targeted PostgreSQL/Flyway smoke path and recorded the real local evidence status.

Current PDR-PF9 status:

1. Existing targeted test exists: `PostgreSqlFlywayMigrationSmokeTest`.
2. Existing skip condition is present and works when Docker/Testcontainers is unavailable.
3. Local `timeout` command was unavailable, so the targeted Maven command was run through a Python 600-second timeout wrapper.
4. Local `docker` command was unavailable and Testcontainers could not find `/var/run/docker.sock`.
5. Bounded command completed in 1.61 seconds with JUnit skip: `Tests run: 1, Failures: 0, Errors: 0, Skipped: 1`.
6. Result is `BLOCKED_ENV_UNAVAILABLE`, not PASS, because no PostgreSQL container ran and no Flyway V1/V2/V3 success log exists.
7. No production DB was accessed and no destructive DB operation was run.
8. Production readiness remains BLOCKED and production deployment cannot proceed.

Next recommendation after PF9: `PDR-PF10 PostgreSQL Environment Provisioning Evidence`.

---

## PDR-PF10 PostgreSQL Environment Provisioning Evidence

PDR-PF10 is DONE/effective on merged main by PR #1078. It checks whether this local/server environment can support PostgreSQL migration smoke evidence before any migration smoke is attempted.

Current PDR-PF10 status:

1. `docker version` and `docker info` return `zsh:1: command not found: docker`.
2. `command -v docker` returns no path.
3. `/var/run/docker.sock` is unavailable.
4. `~/.docker/run/docker.sock` is unavailable.
5. Existing PostgreSQL/Flyway smoke tests are present, including `PostgreSqlFlywayMigrationSmokeTest`.
6. Migration smoke was not run because Docker availability was not confirmed.
7. Docker availability result is `DOCKER_MISSING`.
8. Migration smoke result is `SKIPPED_ENV_UNAVAILABLE`.
9. No Flyway V1/V2/V3 PostgreSQL success evidence exists.
10. No production DB was accessed and no destructive DB operation was run.
11. Production readiness remains BLOCKED and production deployment cannot proceed.

Next recommendation after PF10: `PDR-PF11 Controlled PostgreSQL Migration Smoke Evidence`.

---

## PDR-PF11 Controlled PostgreSQL Migration Smoke Evidence

PDR-PF11 is DONE/effective on merged main by PR #1079. It attempts to obtain a trustworthy bounded Flyway V1/V2/V3 success log only if Docker/Testcontainers or a controlled non-production PostgreSQL environment is available.

PDR-PF11 status:

1. `command -v docker` returns no path.
2. `docker version || true` and `docker info || true` return `zsh:1: command not found: docker`.
3. `/var/run/docker.sock` is unavailable.
4. `~/.docker/run/docker.sock` is unavailable.
5. Existing PostgreSQL/Flyway smoke tests are present, including `PostgreSqlFlywayMigrationSmokeTest`.
6. Bounded Maven/Testcontainers migration smoke was not run because Docker/Testcontainers availability was not confirmed.
7. Docker availability result is `DOCKER_MISSING`.
8. Migration smoke result is `BLOCKED_ENV_UNAVAILABLE`.
9. Flyway V1/V2/V3 PostgreSQL success is not proven.
10. No production DB was accessed and no destructive DB operation was run.
11. Production readiness remains BLOCKED and production deployment cannot proceed.

Next recommendation after PF11: `PDR-LIVE1 Controlled Live Dependency Acceptance`, focused on controlled DB/provider/app-prod/scheduler/Push Recheck evidence without production deployment approval.

---

## PDR-LIVE1 Controlled Live Dependency Acceptance

PDR-LIVE1 is DONE/effective on merged main by PR #1080. It prepares or records real dependency acceptance evidence in a controlled staging, pre-prod, or local-controlled environment. It is not production deployment, not public release, and not auto-trading.

PDR-LIVE1 status:

1. Controlled PostgreSQL DB result is `SKIPPED_MISSING_CONTROLLED_DB` because no disposable non-production PostgreSQL URL was present in environment.
2. Binance public market data smoke result is `SKIPPED_DISABLED` because live external calls were not explicitly enabled.
3. AI provider results are `SKIPPED_MISSING_SECRET` for OpenAI, Gemini, and xAI because keys were not present.
4. External context/news/macro provider result is `SKIPPED_MISSING_SECRET` because keys/configuration were not present.
5. `ProductionProfileSafetyGuardTest`, `PositionMonitorSchedulerTest`, and `PushRecheckServiceImplTest` passed in a bounded run.
6. application-prod safety remains fail-closed.
7. Production scheduler policy remains fail-closed and Position Monitor scheduler remains default-off.
8. Push Recheck quote-unavailable behavior remains fail-closed/review-only.
9. No production DB was accessed, no destructive DB operation was run, and no secrets were printed or committed.
10. Production readiness remains BLOCKED and production deployment cannot proceed.

Next recommendation after LIVE1: `PDR-LIVE2 Controlled PostgreSQL Evidence Setup`. The next package must not be deployment.

---

## PDR-LIVE2 Controlled PostgreSQL Evidence Setup

PDR-LIVE2 is DONE/effective on merged main by PR #1081. It prepares the next concrete path to obtain PostgreSQL Flyway migration PASS evidence in a controlled environment. It is not production deployment and does not run a migration in this package.

PDR-LIVE2 status:

1. Existing migration files V1/V2/V3 are present and reviewed.
2. Existing `PostgreSqlFlywayMigrationSmokeTest` is present and remains the Docker/Testcontainers path.
3. Docker result is `DOCKER_MISSING`.
4. Controlled PostgreSQL DB env result is `SKIPPED_MISSING_CONTROLLED_DB`.
5. No external controlled-DB Flyway runner exists yet.
6. `scripts/controlled-postgresql-evidence-plan.sh` is added as a no-op dry-run setup helper only; it never connects to DB or runs Flyway.
7. No production DB was accessed and no destructive DB operation was run.
8. Production readiness remains BLOCKED and production deployment cannot proceed.

Next recommendation after LIVE2: `PDR-LIVE3 Controlled PostgreSQL Flyway Smoke Runner`. The next package must not be deployment.

---

## PDR-LIVE3 Controlled PostgreSQL Flyway Smoke Runner

PDR-LIVE3 is DONE/effective on merged main by PR #1082. It adds a guarded external controlled-DB runner and a test-only external smoke path. It is not production deployment and does not run a migration unless explicit disposable non-production DB env and run confirmations are supplied.

Current PDR-LIVE3 status:

1. `scripts/controlled-postgresql-flyway-smoke.sh` is added as a guarded bounded runner.
2. `ControlledPostgreSqlFlywaySmokeTest` is added as test-only external PostgreSQL Flyway smoke.
3. Missing controlled DB env produces `SKIPPED_MISSING_CONTROLLED_DB` and no database access.
4. Runner requires explicit non-production confirmation and explicit Flyway run confirmation.
5. Runner refuses production-like JDBC URL indicators and does not print DB URL, username, password, host, or database name.
6. No production DB was accessed and no destructive DB operation was run by the runner package.
7. Production readiness remains BLOCKED and production deployment cannot proceed.

## PDR-LIVE4 Controlled PostgreSQL Flyway Evidence Run

PDR-LIVE4 is DONE/effective on merged main by PR #1083. It records operator-provided PASS evidence from a disposable local Docker PostgreSQL run. It is not production deployment and does not access production DB.

Current PDR-LIVE4 status:

1. The operator ran `scripts/controlled-postgresql-flyway-smoke.sh` with explicit disposable non-production and schema-write confirmations.
2. The controlled PostgreSQL target was `localhost:55432` / `trade_model_smoke`.
3. PostgreSQL version was `16.14`.
4. Flyway validated 3 migrations.
5. Applied migrations: V1 baseline schema tables, V2 baseline schema indexes, and V3 scheme rule config defaults.
6. Final schema version was `v3`.
7. `CONTROLLED_POSTGRESQL_FLYWAY_RESULT: PASS`.
8. No production DB was accessed, no destructive operation outside the disposable controlled DB was run, and no secrets were printed.
9. Production readiness remains BLOCKED and production deployment cannot proceed.

## PDR-LIVE5 Controlled Current-State Migration + Restore Drill Evidence

PDR-LIVE5 is DONE/effective on merged main by PR #1084. It adds a safe no-op/dry-run default helper and records the actual local skipped evidence status. It is not production deployment and does not access production DB.

Current PDR-LIVE5 status:

1. `scripts/controlled-current-state-migration-restore-drill.sh` is added as a guarded helper with no-op default behavior.
2. Controlled source DB env is missing in this execution environment.
3. Controlled recovery DB env is missing in this execution environment.
4. Local PostgreSQL client tools `pg_dump`, `pg_restore`, and `psql` are missing.
5. Backup result is `SKIPPED_MISSING_CONTROLLED_DB`.
6. Restore result is `SKIPPED_MISSING_RECOVERY_DB`.
7. Current-state migration rehearsal result is `SKIPPED`.
8. No production DB was accessed, no destructive operation outside a disposable controlled DB was run, and no secrets were printed.
9. Production readiness remains BLOCKED and production deployment cannot proceed.

## PDR-LIVE6 Controlled Backup Restore Evidence Run

PDR-LIVE6 is DONE/effective on merged main by PR #1085. It records operator-provided disposable local `pg_dump` / `pg_restore` evidence. It is not production deployment and does not access production DB.

Current PDR-LIVE6 status:

1. Source DB was disposable local PostgreSQL at `localhost:55432` / `trade_model_smoke`.
2. Restore DB was disposable local PostgreSQL at `localhost:55433` / `trade_model_restore`.
3. `pg_dump` custom-format backup completed with result `PASS`.
4. `pg_restore` completed with `transaction_timeout` compatibility warning and result `PASS_WITH_WARNING`.
5. Restored `tm_*` table count is `27`.
6. Restored successful Flyway migration count is `3`.
7. Restore validation result is `PASS`.
8. No production DB was accessed, no destructive operation outside disposable controlled DB was run, and no secrets were printed or committed.
9. Production readiness remains BLOCKED and production deployment cannot proceed.

## PDR-LIVE7 PostgreSQL 16-aligned Clean Restore Evidence

PDR-LIVE7 is DONE/effective on merged main by PR #1086. It records operator-provided PostgreSQL 16 container-native backup/restore evidence. It is not production deployment and does not access production DB.

Current PDR-LIVE7 status:

1. Source container was `trade-model-pg-smoke`; restore container was `trade-model-pg-restore`.
2. Source DB was `trade_model_smoke`; restore DB was `trade_model_restore`.
3. Backup used `pg_dump` from `postgres:16-alpine` and completed with result `PASS`.
4. Restore used `pg_restore` from `postgres:16-alpine` and completed with result `PASS_CLEAN`.
5. Restored `tm_*` table count is `27`.
6. Restored successful Flyway migration count is `3`.
7. Restore validation result is `PASS`.
8. No production DB was accessed, no destructive operation outside disposable controlled DB was run, and no secrets were printed or committed.
9. Production readiness remains BLOCKED and production deployment cannot proceed.

Next recommendation after LIVE7: `PDR-LIVE8 Controlled Provider Live Smoke Evidence Run`. The next package must not be deployment.

## PDR-LIVE8 Controlled Provider Live Smoke Evidence Run

PDR-LIVE8 is DONE/effective on merged main by PR #1087. It records controlled provider live-smoke evidence after the PostgreSQL clean restore evidence packages. It is not production deployment, does not place orders, does not send external Push, and does not print or commit secrets.

Current PDR-LIVE8 status:

1. `scripts/prod-provider-smoke.sh` was inspected and remains opt-in for live external calls.
2. Default no-call provider smoke returned `PROVIDER_LIVE_SMOKE: SKIPPED` and skipped Binance/OpenAI/Gemini/xAI without network calls.
3. Controlled Binance public smoke was run with explicit opt-in flags and returned `BINANCE_PUBLIC_SMOKE: PASS` against the public futures time endpoint.
4. OpenAI, Gemini, and xAI were not called because keys were missing and provider flags remained disabled; results are recorded as `SKIPPED_MISSING_SECRET`.
5. External context/news/macro provider smoke is recorded as `SKIPPED_MISSING_SECRET` because keys/configuration were missing and no live external-context call is implemented by the harness.
6. No secret values were printed, no `.env` was committed, no orders were placed, and no external Push was sent.
7. Production readiness remains BLOCKED and production deployment cannot proceed.

Next recommendation after LIVE8: `PDR-LIVE9 Controlled AI Provider Smoke Evidence Run`. The next package must not be deployment.

## PDR-LIVE9 Controlled AI Provider Smoke Evidence Run

PDR-LIVE9 is DONE/effective on merged main by PR #1088. It records controlled AI provider smoke evidence for OpenAI, Gemini, and xAI/Grok. It is not production deployment, does not place orders, does not send external Push, and does not print or commit secrets.

Current PDR-LIVE9 status:

1. `scripts/prod-provider-smoke.sh`, AI provider config, `.env.example`, and provider smoke docs were inspected.
2. AI key presence was checked as boolean-only redacted status: OpenAI, Gemini, and xAI keys were missing.
3. Controlled AI provider smoke was run with AI provider flags enabled and Binance disabled through a bounded 300-second wrapper.
4. OpenAI returned `NOT_CONFIGURED - OPENAI_API_KEY missing`, recorded as `SKIPPED_MISSING_SECRET`.
5. Gemini returned `NOT_CONFIGURED - GEMINI_API_KEY missing`, recorded as `SKIPPED_MISSING_SECRET`.
6. xAI/Grok returned `NOT_CONFIGURED - XAI_API_KEY missing`, recorded as `SKIPPED_MISSING_SECRET`.
7. No AI provider endpoint was called, no secret values were printed, no `.env` was committed, no orders were placed, and no external Push was sent.
8. Production readiness remains BLOCKED and production deployment cannot proceed.

Next recommendation after LIVE9: `PDR-LIVE10 Secrets HTTPS Access Logging Rate Limit Evidence`. The next package must not be deployment.

## PDR-LIVE10 Secrets HTTPS Access Logging Rate Limit Evidence

PDR-LIVE10 is DONE/effective on merged main by PR #1089. It records production security evidence status for secrets, HTTPS/reverse proxy, access logging, auth audit logging, rate limiting, actuator exposure, and production guard behavior. It is not production deployment and does not access production server, production DB, or real secrets.

Current PDR-LIVE10 status:

1. `application-prod.yml`, `.env.example`, `ProductionProfileSafetyGuard`, `SecurityConfig`, relevant security/actuator tests, and existing hardening docs were inspected.
2. Production profile guard evidence is `GUARD_PASS` for datasource/admin/Binance/AI provider fail-closed settings, public bind, actuator exposure, and scheduler policy.
3. Auth access-control evidence is `GUARD_PASS`; dashboard/review/API/write/recheck routes require Basic Auth and no executable trading route surface is introduced.
4. Actuator exposure evidence is `GUARD_PASS`; health/liveness/readiness are minimal and sensitive actuator endpoints are not exposed.
5. Secret handling has repository hygiene `GUARD_PASS` because `.env.example` is placeholder-only and no real `.env` is tracked, but real secrets manager injection is `MISSING_EVIDENCE`.
6. HTTPS/reverse proxy is `DOCUMENTED_NOT_EVIDENCED`.
7. Access logging, auth audit logging, and rate limiting are `MISSING_EVIDENCE`.
8. No production server was accessed, no production DB was accessed, no real secrets were printed or committed, and production readiness remains BLOCKED.

## PDR-LIVE11 Release Evidence Bundle + Remaining Blockers Closure

PDR-LIVE11 is DONE/effective on merged main by PR #1090. It aggregates controlled PostgreSQL, provider, AI-provider, and security/access evidence into a single release-gate status report. It is not production deployment and does not access production server, production DB, or real secrets.

Current PDR-LIVE11 status:

1. `docs/RELEASE_EVIDENCE_BUNDLE_CURRENT_STATUS.md` records the current release evidence table and blocker closure status.
2. PostgreSQL controlled evidence is materially improved: empty Flyway V1/V2/V3 is `PASS`, PostgreSQL 16 backup is `PASS`, clean restore is `PASS_CLEAN`, restored `tm_*` table count is `27`, and restored Flyway success count is `3`.
3. Provider evidence is partial: Binance public smoke is `PASS`, while OpenAI/Gemini/xAI and external context providers remain `SKIPPED_MISSING_SECRET`.
4. Security/access evidence at LIVE11 was partial: production profile guard, auth access control, actuator exposure, and repository secret hygiene were `GUARD_PASS`; secrets manager and credential rotation were `MISSING_EVIDENCE`; HTTPS/reverse proxy was `DOCUMENTED_NOT_EVIDENCED`; access logging, auth audit logging, and rate limiting were still missing before LIVE12.
5. Production readiness remains `BLOCKED`; production deployment decision remains `DO NOT DEPLOY`.
6. No production server was accessed, no production DB was accessed, no secrets were printed or committed, and no runtime trading behavior changed.

## PDR-LIVE12 Access Logging Auth Audit Rate Limit Evidence Remediation

PDR-LIVE12 is DONE/effective on merged main by PR #1091. It was the controlled security evidence and remediation package. It adds application-level access logging, authentication failure audit logging, sensitive-data redaction, and rate-limit guard evidence. It is not production deployment and does not access production server, production DB, or real secrets.

Current PDR-LIVE12 status:

1. `docs/ACCESS_AUDIT_RATE_LIMIT_EVIDENCE_RUN.md` records access logging, auth audit logging, rate limiting, and sensitive-data redaction evidence.
2. `AccessLoggingFilter` emits sanitized `ACCESS_LOG` records without request bodies, query strings, Authorization headers, cookies, API keys, passwords, tokens, datasource URLs, or provider secrets.
3. `AuthAuditAuthenticationEntryPoint` emits `AUTH_AUDIT outcome=FAILURE` records for authentication challenges without credential values.
4. `RequestRateLimitFilter` returns HTTP 429 with `Retry-After` after configured per-client/path thresholds are exceeded.
5. `ProductionProfileSafetyGuard` rejects prod config when rate limiting is disabled or thresholds are invalid.
6. Targeted security tests prove access log presence, auth audit presence, sensitive header/query redaction, rate-limit blocking, and production rate-limit fail-closed validation.
7. Access logging, auth audit logging, rate limiting, and sensitive-data redaction move to `GUARD_PASS` for controlled application-level evidence.
8. Production readiness remains `BLOCKED`; production deployment decision remains `DO NOT DEPLOY`.

Next recommendation after LIVE12: `PDR-LIVE13 HTTPS / Reverse Proxy Evidence`, or a controlled secrets-manager / credential-rotation evidence package. The next package must not be deployment.

## PDR-LIVE13 HTTPS Reverse Proxy Evidence

PDR-LIVE13 is DONE/effective on merged main by PR #1092. It was the controlled HTTPS / reverse proxy evidence package. It records template-only reverse proxy configuration, TLS/redirect/HSTS/proxy-header checklists, and remaining real-server smoke blockers. It is not production deployment and does not access production server, production DB, or real secrets.

Current PDR-LIVE13 status:

1. `docs/HTTPS_REVERSE_PROXY_EVIDENCE_RUN.md` records HTTPS / reverse proxy status as `DOCUMENTED_WITH_CONFIG`.
2. The package provides an Nginx-style template marked TEMPLATE ONLY / NOT PRODUCTION SECRET.
3. TLS certificate, HTTP-to-HTTPS redirect, HSTS, and proxy-header requirements are documented with concrete evidence criteria.
4. App actuator exposure remains `GUARD_PASS`; proxy routing to only health/liveness/readiness is documented but not smoke-tested through a real proxy.
5. Auth smoke through HTTPS proxy remains `MISSING_EVIDENCE`.
6. Access logging, auth audit logging, and rate limiting remain app-level `GUARD_PASS`; proxy-level retention/aggregation and forwarded-IP evidence remain missing.
7. Production readiness remains BLOCKED and production deployment cannot proceed.

Next recommendation after LIVE13: `PDR-LIVE14 Secrets Manager / Credential Rotation Evidence`, or a controlled real-server HTTPS reverse-proxy smoke package if an approved non-production server endpoint is available. The next package must not be deployment.

## PDR-LIVE14 Secrets Manager Credential Rotation Evidence

PDR-LIVE14 is DONE/effective on merged main by PR #1093. It records controlled secrets manager / credential rotation evidence status. It is not production deployment and does not access production server, production DB, secret manager, or real secrets.

Current PDR-LIVE14 status:

1. `docs/SECRETS_MANAGER_CREDENTIAL_ROTATION_EVIDENCE_RUN.md` records repo secret hygiene as `GUARD_PASS` for tracked-file hygiene.
2. `application-prod.yml` env/secret requirements are `GUARD_PASS`.
3. `ProductionProfileSafetyGuard` secret validation is `GUARD_PASS`.
4. Secrets manager integration is `DOCUMENTED_WITH_PLAN`; no real secret-store injection evidence exists.
5. Credential rotation evidence is `DOCUMENTED_WITH_PLAN`; no actual admin, datasource, Binance/API, or AI provider rotation drill was run.
6. No production server, production DB, secret manager, real secret, or untracked `.env` was accessed.
7. Production readiness remains BLOCKED and production deployment cannot proceed.

## PDR-LIVE15 Real Server Smoke Evidence Plan / Gate

PDR-LIVE15 is the current controlled real-server / staging-server smoke evidence gate. It is not production deployment, does not access production DB, does not place orders, does not send external Push, does not print or commit secrets, and does not approve production deployment.

Current PDR-LIVE15 status:

1. `docs/REAL_SERVER_SMOKE_EVIDENCE_GATE.md` records the real-server smoke gate status.
2. Controlled server env presence is `SKIPPED_MISSING_CONTROLLED_SERVER`; `CONTROLLED_SERVER_BASE_URL` was not present and no server was contacted.
3. Server smoke, HTTPS endpoint classification, health/readiness smoke, and authenticated dashboard/review smoke are `SKIPPED_MISSING_CONTROLLED_SERVER`.
4. Access logging / auth audit / rate-limit through server is `SKIPPED_MISSING_CONTROLLED_SERVER`; app-level guards remain `GUARD_PASS` from LIVE12.
5. `scripts/controlled-real-server-smoke.sh` is added as a safe default-skip wrapper. It checks controlled server env presence without printing values, requires HTTPS for non-local endpoints, and delegates authenticated checks to `scripts/prod-smoke.sh` only when credentials are already present in env.
6. No production server was accessed, no production DB was accessed, no orders were placed, no external Push was sent, and no secrets were printed or committed.
7. Production readiness remains BLOCKED and production deployment cannot proceed.

Next recommendation after LIVE15: PDR-LIVE16 Final Conditional Readiness Review was selected to aggregate LIVE1-LIVE15 evidence before any later provider or real-server PASS evidence package. The next package must not be deployment.

## PDR-LIVE16 Final Conditional Readiness Review

PDR-LIVE16 is the current final conditional readiness review package. It aggregates LIVE1-LIVE15 evidence and decides whether production readiness can move beyond BLOCKED. It is not production deployment and does not access production server, production DB, real secrets, or provider endpoints.

Evidence doc: `docs/FINAL_CONDITIONAL_READINESS_REVIEW.md`.

Current LIVE16 decision:

1. Readiness decision remains `BLOCKED`.
2. Deployment decision remains `DO NOT DEPLOY`.
3. V1 cannot move to `CONDITIONALLY_READY_CANDIDATE` because real server smoke is `SKIPPED_MISSING_CONTROLLED_SERVER`, real HTTPS/proxy auth smoke is missing, real secret-store injection and rotation drill evidence are missing, AI/external providers remain skipped or missing, and release-owner approval is missing.
4. PostgreSQL, Binance public smoke, application access/auth/rate-limit guards, production profile guard, auth access control, actuator exposure, and repo secret hygiene remain meaningful PASS/GUARD_PASS evidence.
5. `DOCUMENTED_WITH_PLAN`, `DOCUMENTED_WITH_CONFIG`, `SKIPPED`, and `MISSING_EVIDENCE` are not treated as PASS.
6. Production readiness remains BLOCKED and production deployment cannot proceed.

Next recommendation after LIVE16: run a controlled real-server PASS evidence package if infrastructure is available, or proceed to an AI Provider / External Context Release Policy Evidence package. The next package must not be deployment.

## PDR-LIVE17 AI External Provider Release Policy Evidence

PDR-LIVE17 is the current AI / external provider release policy evidence package. It determines whether AI providers and external context/news/macro providers are required release gates or optional/waivable dependencies for the next controlled release candidate. It is not production deployment and does not access production server, production DB, real secrets, or provider endpoints.

Evidence doc: `docs/AI_EXTERNAL_PROVIDER_RELEASE_POLICY_EVIDENCE.md`.

Current LIVE17 policy status:

1. Binance public market data remains `PASS` from controlled LIVE8 evidence, limited to public market-data reachability.
2. OpenAI, Gemini, and xAI/Grok remain `SKIPPED_MISSING_SECRET`; they are not PASS and were not called by this package.
3. External context/news, macro calendar, and ETF flow remain `SKIPPED_MISSING_SECRET` or missing live harness evidence.
4. Missing providers are `RELEASE_OWNER_DECISION_REQUIRED` until each provider is classified as `REQUIRED_PASS`, `OPTIONAL_WITH_WAIVER`, `DISABLED_FOR_RELEASE`, or `NOT_APPLICABLE`.
5. Pure-rule fallback may be accepted only by an explicit release-owner decision for the target release.
6. Production readiness remains BLOCKED and production deployment cannot proceed.

Next recommendation after LIVE17: record release-owner provider policy decisions, run controlled real-server PASS evidence if infrastructure becomes available, or collect controlled secrets/rotation/proxy evidence. The next package must not be deployment.

## PDR-LIVE18 Release Owner Decision Register / Waiver Policy

PDR-LIVE18 is DONE/effective as the release-owner decision register and waiver policy evidence package. It records remaining gates that require explicit human approval, waiver, or controlled evidence before readiness can move beyond BLOCKED. It is not production deployment and does not access production server, production DB, real secrets, or provider endpoints.

Evidence doc: `docs/RELEASE_OWNER_DECISION_REGISTER.md`.

LIVE18 register status:

1. Release owner status is `MISSING_EVIDENCE`; no named owner approval was found.
2. Real server smoke, HTTPS/proxy auth smoke, secret-store injection, credential rotation drill, release timing, rollback owner, incident owner, and final release approval remain required gates.
3. OpenAI, Gemini, xAI/Grok, and external context/news/macro/ETF remain `RELEASE_OWNER_DECISION_REQUIRED` unless controlled PASS evidence or explicit owner waiver/disablement is recorded.
4. No waiver is approved by this package.
5. `SKIPPED_MISSING_SECRET`, `DOCUMENTED_WITH_PLAN`, and `DOCUMENTED_WITH_CONFIG` are not PASS.
6. Production readiness remains BLOCKED and production deployment cannot proceed.

Next recommendation after LIVE18: capture explicit release-owner decisions if available, or collect controlled real-server, HTTPS/proxy auth, secret-store injection, or credential-rotation evidence. The next package must not be deployment.

---

## Current Allowed Work

Only the following work is allowed under the current P3-H closure:

1. Perform Reviewer P3-H Offline Harness Round 5 re-review.
2. Keep offline contract evidence distinct from real server execution.
3. Retain existing P3/P3-G/P3-H safety and recovery tooling.
4. Keep P4 and production deployment blocked.
5. Run a later controlled server package only with complete explicit
   attestations and secret-safe inputs.

PDR-M7 is the historical latest production-readiness package, not the only currently allowed work. Production deployment remains BLOCKED until a separate production release gate clears every required gate with PASS evidence.

---

## Current Forbidden Work

The following work remains blocked during and after P2 V1 Business Stress Test:

1. no auto-open
2. no auto-close
3. no auto-reverse
4. no order execution
5. no auto-trading
6. no external push send
7. no fake positions
8. no fake review records
9. no production-ready claim
10. Treating local acceptance readiness as production deployment approval.
11. Treating PF8 release-gate closure as deployment approval.
12. Treating LIVE4 Flyway PASS, LIVE6 backup/restore warning evidence, LIVE7 clean local restore evidence, LIVE8 Binance public PASS evidence, LIVE9 AI skipped evidence, LIVE10 guard-pass evidence, LIVE11 release evidence bundle status, LIVE12 access/audit/rate-limit guard evidence, LIVE13 HTTPS template evidence, LIVE14 secrets/rotation plan evidence, LIVE15 skipped real-server smoke gate evidence, LIVE16 blocked readiness review, LIVE17 provider policy evidence, or LIVE18 decision-register evidence as full production deployment approval; they are not release-gate approval.
13. Executing dashboard stress traffic in the P1 preparation PR.
14. Stressing any non-local target or any endpoint outside `GET /actuator/health`, `GET /dashboard`, and `GET /api/dashboard/home`.
15. Treating deterministic local business stress PASS as production readiness, live profitability, or release approval.

---

## Current Known Critical Gaps

1. Production deployment remains blocked by non-production runtime/config evidence.
2. P3-3 Final Delivery & System Freeze completion does not prove production readiness, Push send, Telegram send, external channel, order execution, or auto-trading capability.
3. Production-readiness remediation remains a separate future scope and requires a separate human release gate.

## P3-2 Full E2E Acceptance Closure

Merged main evidence:

1. `DecisionServiceImplTest.getLatestDecisionResultsDoesNotInferOpenPositionFromTriggeredDecisionWithoutManualUserPosition` proves ExecutionPlan / triggered DecisionResult state without UserPosition does not render as an opened position.
2. `DecisionServiceImplTest.getLatestDecisionResultBySymbolUsesManualOpenUserPositionAsDashboardPositionSource` proves manual OPEN UserPosition is the dashboard real-position source.
3. `DecisionServiceImplTest.getLatestDecisionResultBySymbolExcludesClosedAndNonManualUserPositionRows` and `countOpenPositionsCountsOnlyManualOpenUserPositions` prove CLOSED and non-MANUAL UserPosition rows are excluded from open dashboard display.
4. `UserPositionFullLifecycleE2EAcceptanceTest.manualUserPositionFlowsThroughMonitorCloseReviewAndRuleFeedbackWithoutExecutableSurfaces` proves the UserPosition -> PositionMonitor -> manual close -> Review -> rule feedback chain.
5. `DashboardControllerTest.summary_json_exposesManualUserPositionFieldsAndKeepsExecutionPlanOnlyRowsNonPosition`, `dashboardTemplateHomePositionReadsOnlyManualUserPositionReadModelFields`, `StaticNoTradeInstructionGuardTest.dashboardPositionExecutionRowKeepsManualPositionDisplayPassive`, and `CandidatePushReviewOnlyMvpClosureTest.dashboardDisplaysInternalPushPreviewAsDisabledReviewOnlySurface` prove dashboard E2E key states and safety anchors.
6. `PositionMonitorServiceImplTest` and `PositionMonitorLogServiceImplTest` prove HOLD / LOGIC_WEAKENED / PLAN_INVALIDATED monitor outcomes and one-log monitor persistence.
7. `UserPositionRiskAdapterTest` and `PositionMonitorServiceImplTest.riskBlockedAndRiskIncreasedAreFailClosed` prove AccountRisk high-risk blocking.
8. `PushRecheckServiceImplTest` proves PushRecheck risk/confused/drifted/expired behavior remains review-only and does not create UserPosition.
9. `ConfusedStateServiceImplTest` and `HotResetServiceImplTest` prove ConfusedState and HotReset safety states.
10. `UserPositionReviewAdapterTest` and `ReviewControllerUserPositionReviewTest` prove Review execution deviation and rule feedback.
11. `OpportunityLogServiceImplTest`, `MacroEventServiceImplTest`, `NewsEventServiceImplTest`, `ExternalContextEvidenceBuilderTest`, `AiDecisionOrchestratorServiceImplTest`, and `AiCallLogServiceImplTest` prove the remaining contract E2E evidence around OpportunityLog, Macro / News, AI fallback, and AiCallLog.
12. `./mvnw test -q` passed on clean / synced `main`.
13. `bash scripts/v1-delivery-check.sh` passed on clean / synced `main`.
14. `bash scripts/v1-state.sh` passed with `WORKTREE_CLEAN: Yes`, `OPEN_PR_STATUS: NONE`, `MAIN_SYNC: OK`, `CLEAN_SYNCED_MAIN: YES`, and `BLOCKERS: none`.

P3-2 Full E2E Acceptance is DONE/effective as E2E acceptance evidence. Production Deployment Readiness remains BLOCKED and no production-ready claim is made.

## P3-3 Final Delivery & System Freeze Closure

Final local acceptance-ready freeze evidence:

1. `GET /api/dashboard/home` returned HTTP 200 success with `header`, `systemState`, `assets`, `positions`, `executionSuggestion`, `aiDecision`, `pushInbox`, `diagnostics`, and `safety`.
2. Dashboard Home Aggregation API is merged/effective.
3. Dashboard Data Fill P1-P5 are merged/effective: decision/systemState/assets, manual positions/executionSuggestion, AI role evidence, pushInbox readonly data, and Telegram readonly status contract.
4. Push Inbox remains readonly.
5. Telegram status remains `WAITING_SYNC` until a verified status source exists; no Telegram send is implemented.
6. `GET /api/review/center` returned HTTP 200 success with `summary`, `positionReviews`, `opportunityReviews`, `pushReviews`, and `ruleFeedback`.
7. `/review/dashboard` exists as the Review Center route; four tabs exist: 持仓复盘 / 机会复盘 / 推送复盘 / 规则反馈.
8. Review Center data is readonly and does not fabricate records.
9. Mainline validation passed on `main`: clean worktree before closure, no open PRs, MAIN_SYNC OK, full Maven PASS, delivery check PASS, and `v1-state` blockers none.
10. P0-0 through P3-2 remain DONE/effective.
11. Production Deployment Readiness remains BLOCKED.
12. No production deployment approval is granted.
13. No order / execution / auto-trading capability exists.

P3-3 Final Delivery & System Freeze is DONE/effective as a local acceptance-ready / read-only decision support / review workflow freeze. It is not production deployment ready.

---

## Current Deployment Readiness

Production deployment remains BLOCKED.

Blocking evidence:

- `src/main/resources/application.yml` uses `jdbc:h2:mem:trade_model_v1`.
- `src/main/resources/application.yml` has empty datasource password.
- `src/main/resources/application.yml` and `src/main/resources/application.properties` enable H2 console.
- `src/main/resources/application.properties` defaults `position.provider.type` to `SIMULATED`.
- PDR-1 added `src/main/resources/application-prod.yml` and `ProductionProfileSafetyGuard`, but this is only a production config/profile safety gate and does not prove production deployment readiness.
- PostgreSQL JDBC driver, test-only Testcontainers/Flyway smoke, mapper DATEADD / FORMATDATETIME variants, and backup/restore templates exist after PDR-M1, but no real production database is connected.
- Dockerfile, Docker Compose skeleton, `.env.example`, readonly smoke script, and backup/restore template scripts exist after PDR-M2, but no real server is deployed.
- Single-operator Basic Auth exists after PDR-M3, but no real-server HTTPS/reverse-proxy smoke, real credential rotation drill, real secrets manager integration, real server auth smoke, or production release gate exists yet.
- Minimal public health/readiness endpoints and authenticated smoke checks exist after PDR-M4, readonly provider readiness checks exist after PDR-M5, the PDR-M6A acceptance evidence framework exists, and the PDR-M7 opt-in provider live smoke harness exists, but no completed real-server evidence, metrics dashboards, log aggregation, alerting, full provider connection proof, or production release approval exists yet.
- No production database is connected in this package.
- No full observability stack, real server deployment smoke/rollback evidence, real restore drill evidence, secrets manager integration, verified external-provider integration, or production release gate exists yet.

### PDR-2A Database Migration + Rollback Decision Pack

PDR-2A records the production database and migration decisions only. It does not add runtime implementation.

- Production database target: PostgreSQL.
- Migration framework target: Flyway, SQL-first.
- Rollback policy: forward-only migrations plus pre-migration backup and restore.
- Migration execution model: explicit manual pre-deploy migration; application startup must not silently mutate the production schema without a controlled migration process.
- Initial recovery target: RPO 24h and RTO 4h.
- Current schema state: `src/main/resources/schema.sql` remains local/test bootstrap for now.
- No PostgreSQL driver, Flyway dependency, migration SQL, mapper SQL change, production DB connection, backup script, deployment script, secret, auth, Telegram send, Push send, order/execution, or auto-trading semantics are added by PDR-2A.
- Production readiness remains BLOCKED.

Database / deployment remaining blockers after PDR-M7:

- Flyway remains non-default for runtime startup; PDR-M1 adds only test/manual smoke coverage.
- PostgreSQL baseline schema SQL has a Testcontainers smoke path, but local evidence depends on Docker availability.
- Mapper PostgreSQL variants cover known upsert and DATEADD / FORMATDATETIME blockers; broader live mapper execution remains deferred.
- Docker Compose deployment skeleton, `.env.example`, smoke/backup/restore scripts, the PDR-M6A evidence template, the conservative release gate runner, and the opt-in provider live smoke harness exist, but real server deployment smoke, AI/external provider smoke evidence, and production-like restore drill evidence are still missing.
- Auth/access control baseline exists as single-operator Basic Auth; LIVE14 records secrets manager / credential rotation as DOCUMENTED_WITH_PLAN, and LIVE15 records real-server smoke as SKIPPED_MISSING_CONTROLLED_SERVER; real server auth smoke, real HTTPS proxy smoke, actual credential rotation drill, and real secrets manager integration remain missing.
- Observability is minimal: health/readiness exists, but metrics dashboards, log aggregation, alerting, real server smoke evidence, and restore drill evidence remain missing.
- Deployment packaging is skeletal only and not release-gated.
- Secrets contract exists as placeholders and LIVE14 documents the rotation plan; actual secret-store injection and rotation evidence remain missing.

Next production-readiness packages:

1. A release-owner decision capture package if actual owner decisions are available, controlled real-server PASS evidence run if infrastructure becomes available, or another explicitly scoped provider/secrets/access evidence package.
2. Secrets/access/HTTPS evidence only after explicitly scoped controlled environment evidence is available.
3. Production release-gate status closure only after completed redacted evidence and explicit approval.

### PDR-2B Flyway Baseline Skeleton

PDR-2B adds the Flyway project skeleton without changing default local/test runtime behavior.

- Flyway dependency scope: explicit non-default Maven profile `flyway-migration` only.
- Migration directory: `src/main/resources/db/migration/` with README placeholder only.
- Executable migration files: none; no `V*.sql` baseline exists yet.
- Current schema state: `src/main/resources/schema.sql` remains local/test bootstrap and is not copied.
- Production target database remains PostgreSQL.
- Real PostgreSQL-compatible baseline migration is deferred to PDR-2C.
- No PostgreSQL driver, production DB connection, schema change, mapper SQL change, application config change, backup script, deployment script, secret, auth, Telegram send, Push send, order/execution, or auto-trading semantics are added by PDR-2B.
- Production readiness remains BLOCKED.

### PDR-2C1 PostgreSQL Baseline Schema SQL

PDR-2C1 adds PostgreSQL-compatible Flyway baseline schema SQL drafts without changing local/test runtime behavior.

- Table migration: `src/main/resources/db/migration/V1__baseline_schema_tables.sql`.
- Index migration: `src/main/resources/db/migration/V2__baseline_schema_indexes.sql`.
- Scope: current V1 table and index semantics only.
- JSON-like fields remain `TEXT`; timestamp fields use `TIMESTAMP WITHOUT TIME ZONE`; generated numeric ids use PostgreSQL identity columns.
- V1 remains foreign-key-free for this baseline, matching current schema semantics.
- No seed data, schema.sql change, mapper SQL change, Java/config/test change, PostgreSQL driver, Testcontainers, production DB connection, backup script, deployment script, secret, auth, Telegram send, Push send, order/execution, or auto-trading semantics are added by PDR-2C1.
- Mapper compatibility and real PostgreSQL validation remain deferred to PDR-2C2/PDR-2C3.
- Production readiness remains BLOCKED.

### PDR-2C2A Mapper PostgreSQL Upsert Variants

PDR-2C2A adds MyBatis mapper-level PostgreSQL upsert variants without changing default local/test H2 behavior.

- `MyBatisDatabaseIdProviderConfig` maps PostgreSQL to `postgresql`, H2 to `h2`, and MySQL to `mysql` for MyBatis database-specific annotation selection.
- `AssetStateMapper.mergeUpsertCore` keeps the generic H2 `MERGE INTO ... KEY` fallback and adds a PostgreSQL `ON CONFLICT (symbol) DO UPDATE` variant that does not overwrite `hot_reset_*` fields.
- `UserConfigMapper.saveOrUpdate` keeps the generic MySQL/H2 `ON DUPLICATE KEY UPDATE` fallback and adds a PostgreSQL `ON CONFLICT (user_id) DO UPDATE` variant.
- Focused tests prove default H2 upsert behavior still works and annotation guards prove PostgreSQL variants do not contain H2/MySQL upsert syntax.
- DATEADD / FORMATDATETIME mapper compatibility, PostgreSQL driver/Testcontainers validation, production DB connection, backup script, deployment script, secret, auth, Telegram send, Push send, order/execution, and auto-trading semantics remain deferred / blocked.
- Production readiness remains BLOCKED.

### PDR-M1 PostgreSQL Runtime Pack

PDR-M1 adds PostgreSQL runtime smoke readiness while preserving default H2/local behavior.

- `pom.xml` includes PostgreSQL JDBC runtime dependency and test-scoped Testcontainers / Flyway PostgreSQL smoke dependencies.
- `src/test/resources/application.properties` disables Spring Boot Flyway auto-configuration for default tests so `schema.sql` remains the H2 local/test bootstrap.
- `PostgreSqlFlywayMigrationSmokeTest` manually runs Flyway V1/V2 migrations against PostgreSQL Testcontainers when Docker is available, verifies the 27 V1 tables, critical indexes, Flyway history success, and PostgreSQL identity generated-key behavior.
- PostgreSQL `databaseId = "postgresql"` mapper variants replace DATEADD / FORMATDATETIME syntax for AnalysisRun, PushSnapshot, HotResetEvent, PushRecheckLog, and MonitorAlert targeted methods while leaving generic H2 SQL unchanged.
- `UserPositionMapper.insert` specifies `keyColumn = "id"` for generated-key compatibility.
- `PRODUCTION_READINESS_RUNBOOK.md` records pg_dump / pg_restore / psql restore templates and a restore smoke checklist.
- No real PostgreSQL connection, schema.sql change, production config change, deployment script, secret, auth, Telegram send, Push send, order/execution, or auto-trading semantics are added.
- Production readiness remains BLOCKED.

### PDR-M2 Server Deployment + Secrets + Smoke Pack

PDR-M2 adds a Docker Compose server deployment skeleton while preserving the no-trading and blocked-production boundaries.

- `Dockerfile` builds the Spring Boot app with Maven wrapper in a JDK build stage and runs the packaged jar in a JRE runtime stage as a non-root user.
- `docker-compose.yml` defines PostgreSQL, a manual Flyway migration runner profile, and the app service. The host app port binds to `127.0.0.1` by default.
- `.env.example` records placeholder-only app, PostgreSQL, Binance position provider, optional AI, future Telegram, backup, and restore variables. `.env` and secret/backup outputs are ignored.
- `scripts/prod-smoke.sh` performs readonly checks for `/api/dashboard/home` and `/api/review/center`, including safety fields and Telegram non-connected status.
- `scripts/prod-backup.sh` and `scripts/prod-restore.sh` provide PostgreSQL backup/restore templates with required env vars and no hard-coded secrets; restore requires explicit confirmation.
- No real server deployment, real secrets, Java business logic change, schema.sql change, mapper SQL change, Flyway migration change, auth implementation, Telegram send, Push dispatch, order/execution, or auto-trading semantics are added.
- Production readiness remains BLOCKED.

### PDR-M3 Auth + Access Control Gate

PDR-M3 adds a single-operator Spring Security Basic Auth gate while preserving the no-trading and blocked-production boundaries.

- `pom.xml` includes Spring Security and Spring Security test support.
- `SecurityConfig` protects dashboard/review pages and operational/dashboard/review API routes when `trade-model.auth.enabled=true`.
- The operator account is sourced from `APP_ADMIN_USERNAME` and `APP_ADMIN_PASSWORD` through configuration; broad legacy tests explicitly disable auth through test config.
- `ProductionProfileSafetyGuard` rejects missing admin credentials and unsafe defaults in the prod profile.
- `.env.example`, `docker-compose.yml`, and `scripts/prod-smoke.sh` now include auth credential handling without printing passwords.
- Targeted security tests prove protected routes require Basic Auth, authenticated requests succeed, write endpoints are protected, static resources are not turned into auth challenges, and no buy/sell/order/execute/auto-trading route surface is introduced.
- No real server deployment, real secrets, database user table, signup/login UI, OAuth, role UI, Java trading logic change, schema.sql change, mapper SQL change, Flyway migration change, Telegram send, Push dispatch, order/execution, or auto-trading semantics are added.
- Production readiness remains BLOCKED.

### PDR-M4 Observability + Production Smoke Gate

PDR-M4 adds minimal health/readiness observability and strengthens authenticated production smoke checks while preserving blocked-production and no-trading boundaries.

- `pom.xml` includes Spring Boot Actuator.
- `application.yml` exposes only the `health` actuator endpoint and enables `/actuator/health/liveness` and `/actuator/health/readiness` with details/components hidden.
- `SecurityConfig` permits public minimal health/liveness/readiness and keeps dashboard/review plus operational APIs authenticated.
- `ProductionProfileSafetyGuard` rejects prod actuator exposure wider than `health`, including wildcard exposure.
- `scripts/prod-smoke.sh` checks public health/liveness/readiness, authenticated `/api/dashboard/home`, authenticated `/api/review/center`, no-auto-trading/no-order safety fields, and Telegram non-connected status without printing passwords.
- Targeted health/actuator/security tests prove public minimal health behavior, sensitive actuator non-exposure, auth boundary preservation, prod guard rejection, and smoke script syntax.
- No real server deployment, real secrets, sensitive actuator endpoints, Prometheus/Grafana, alerting stack, Java business logic change, schema.sql change, mapper SQL change, Flyway migration change, Telegram send, Push dispatch, order/execution, or auto-trading semantics are added.
- Production readiness remains BLOCKED.

### PDR-M5 Real Data Provider Readiness Pack

PDR-M5 adds readonly provider-readiness status and production smoke checks while preserving blocked-production and no-trading boundaries.

- `ProviderReadinessService` maps Binance public market data, AI providers, and external context placeholders to safe statuses: `CONFIGURED`, `WAITING_SYNC`, `NOT_CONFIGURED`, `FAIL_CLOSED`, or `UNKNOWN`.
- `CONNECTED` is not reported from config-only fields; live-provider proof remains deferred.
- `/api/dashboard/home.header.dataSourceText` and `/api/dashboard/home.diagnostics` expose provider readiness without external calls.
- `.env.example` records placeholder-only Binance, AI, news, macro calendar, ETF flow, and smoke external-call variables; no real secrets are committed.
- `ProductionProfileSafetyGuard` rejects explicitly enabled production AI providers with missing key/model/base URL while preserving local/dev compatibility.
- `scripts/prod-smoke.sh` checks dashboard provider readiness and defaults `SMOKE_ALLOW_EXTERNAL_CALLS=false`, so smoke does not call live providers by default.
- Targeted provider/config/production tests prove config-only is not `CONNECTED`, simulated fallback remains local/dev only, AI missing keys fail closed when explicitly enabled, and smoke syntax/provider checks remain safe.
- No real server deployment, real secrets, live external provider call in default tests/smoke, Binance private trading, Telegram send, Push dispatch, schema change, mapper SQL change, dashboard/review template change, order/execution, or auto-trading semantics are added.
- Production readiness remains BLOCKED.

### PDR-M6A Real Server Acceptance Evidence Gate

PDR-M6A adds the real-server acceptance evidence gate framework while preserving blocked-production and no-trading boundaries.

- `docs/PRODUCTION_ACCEPTANCE_EVIDENCE_TEMPLATE.md` records required redacted evidence for Docker Compose config, PostgreSQL startup, Flyway migration, app prod startup, authenticated smoke, backup drill, restore drill, HTTPS/reverse-proxy/auth smoke, provider live smoke, and safety boundary checks.
- `scripts/prod-release-gate.sh` orchestrates only safe checks: Docker Compose config, `scripts/prod-smoke.sh`, and an optional backup drill when explicitly enabled. It does not run restore automatically and does not print secrets.
- `.env.example` documents conservative release gate flags: `RELEASE_GATE_REQUIRE_DOCKER=true`, `RELEASE_GATE_REQUIRE_BACKUP=false`, and `RELEASE_GATE_ALLOW_EXTERNAL_CALLS=false`.
- `PRODUCTION_READINESS_RUNBOOK.md` now includes the release gate checklist and server evidence collection process.
- No real server deployment, real secrets, production DB connection from Codex, restore execution against a real DB, Telegram send, Push dispatch, Push Recheck execution, Binance private trading, Java/test/schema/mapper/template change, order/execution, or auto-trading semantics are added.
- Production readiness remains BLOCKED until the user supplies completed real-server evidence and explicitly approves a release-gate status closure.

### PDR-M7 Real Provider Live Smoke Harness

PDR-M7 adds an opt-in live provider smoke harness while preserving blocked-production and no-trading boundaries.

- `scripts/prod-provider-smoke.sh` defaults to `PROVIDER_LIVE_SMOKE: SKIPPED` with no network calls unless `PROVIDER_SMOKE_ENABLE_EXTERNAL_CALLS=true` is explicitly set.
- Binance public market smoke is controlled by `PROVIDER_SMOKE_BINANCE_PUBLIC_ENABLED=true` and uses a public market endpoint only, with no trading, withdrawal, or private order permission required.
- OpenAI, Gemini, and XAI smoke checks are controlled by provider-specific flags and only run when server-side keys are configured; keys and response bodies are not printed.
- `scripts/prod-release-gate.sh` can require provider smoke only with `RELEASE_GATE_REQUIRE_PROVIDER_SMOKE=true`; otherwise provider smoke remains incomplete evidence, not a readiness signal.
- `.env.example`, `PRODUCTION_READINESS_RUNBOOK.md`, and `PRODUCTION_ACCEPTANCE_EVIDENCE_TEMPLATE.md` document the provider smoke env contract and redacted evidence fields.
- `ProdSmokeScriptHealthTest` adds static/default checks proving shell syntax, default skipped behavior, no Binance order/withdraw endpoints, no obvious secret echo, and optional release-gate integration.
- No real server deployment, real secrets, committed `.env`, real network calls in tests/default smoke, Telegram send, Push dispatch, Push Recheck execution, Binance private trading, schema/mapper/template change, order/execution, or auto-trading semantics are added.
- Production readiness remains BLOCKED until the user supplies completed real-server and provider-live-smoke evidence and explicitly approves a release-gate status closure.

---

## Derived / Compatibility Sources

`docs/ACTIVE_MAINLINE_STATUS.yml` and `docs/CODEX_NEXT_TASK.yml` are derived compatibility files only.
They do not override the Delivery Contract, Delivery Progress Matrix, or this Current State file.

Legacy V1 documents remain historical audit and asset evidence only.
Review-only slice count is no longer a delivery completion standard.

---

## Rule

No production deployment approval or runtime production implementation package may start until a separate explicit production release gate addresses the blocked runtime/config evidence and preserves the permanent no auto-trading / no order-execution safety boundaries. Docs-only production-readiness decision packs may record decisions while keeping deployment readiness BLOCKED.

## Workflow PR Status

- CURRENT_PACKAGE_PR: NONE; derive current workflow identity at runtime. PR #1151 is merged and the bounded FE-04D first package is effective on main.
- OPEN_PR_COUNT: not re-verified by this local documentation task; runtime status remains derived by `scripts/v1-state.sh`
- UNRELATED_OPEN_PRS: not re-verified by this local documentation task; runtime status remains derived by `scripts/v1-state.sh`
