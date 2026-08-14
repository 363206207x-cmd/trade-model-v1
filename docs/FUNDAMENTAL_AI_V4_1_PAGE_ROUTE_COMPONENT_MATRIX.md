# Fundamental AI v4.1 Page, Route and Component Matrix

Status: `FROZEN_NORMATIVE_ANNEX`

Canonical Product Source:
`docs/product-sources/FUNDAMENTAL_AI_V4_1_DECISION_CHAIN.md`

Interaction source SHA-256:
`43ec787f3228ec05e4e81a3c07fce4c3969c38850d709efa7097a2a406c463d3`

This matrix is a normative annex of the single v4.1 Product Source. It does
not create a second Product Source and does not claim implementation.

## 1. Routed Page Matrix

| ID | Page / Route | Responsibility | Data Owner | Reused Asset | New / Extended | Required States | Contract Tests |
|---|---|---|---|---|---|---|---|
| R01 | Login / Session Recovery `/login` | Authenticate, recover expired session and return to intended route | Session, LoginAttempt, returnUrl | current login shell and auth endpoint | deep-link return and recovery states | `DEFAULT`, `ERROR_OR_LOCKED`, `SESSION_EXPIRED` | login state, returnUrl, no data leak |
| R02 | Home Dashboard `/dashboard?asset={symbol}` | System context, alerts/events, dynamic Top6, positions, selected Final and Three AI | HomeTopOpportunityProjection, FinalExecutionPlan, AI results, ConflictResolverResult, UserPosition summary, system status | approved Desktop shell, Final-only renderer, single AI workspace, position trust gate | URL asset context, dynamic multi-timeframe projection, lifecycle and async states | `READY`, `NO_ELIGIBLE_OPPORTUNITY`, `SELECTED_ASSET_EXITED_TOP6`, `WAITING_DATA`, `STALE`, `CONFUSED_BLOCKED`, `NO_POSITION`, `PARTIAL_FAILURE` | Top6 lineage, selected context, Final-only, fail closed, no fake data |
| R03 | Asset Pool `/asset-pool` | Manage the sole continuous opportunity source and start scans/Preview | AssetPoolItem, AnalysisTask, Opportunity summary, Position presence | existing pool persistence/search services | full management UI, top-up/reset distinction, batch task state | `DEFAULT`, `SEARCHING`, `SCAN_RUNNING`, `PARTIAL_FAILURE`, `EMPTY_CUSTOM_POOL` | >6 assets, fuzzy search, add/remove, top-up, reset, scan, removal effects |
| R04 | Position Center `/positions` | List real UserPositions and account-risk coverage | UserPosition, PositionMonitorLog, RiskCoverageSnapshot | P2 position projection and trust gate | account-risk coverage and filtering | `NO_POSITION`, `WAITING_MONITOR`, `MONITOR_READY`, `PARTIAL_RISK_COVERAGE` | plan/position separation, trusted monitor, coverage state |
| R05 | Position Detail `/positions/{positionId}` | Compare actual position, opening Final version, latest Final and monitoring history | UserPosition, FinalExecutionPlan versions, PositionMonitorLog, ReviewResult | existing position detail and close lifecycle | original/latest plan comparison and lifecycle | `OPEN_READY`, `WAITING_MONITOR`, `RISK_ESCALATED`, `PLAN_VERSION_CHANGED`, `CLOSED` | immutable opening finalPlanId, manual close, monitor history |
| R06 | Review Center `/reviews` | Browse executed and missed reviews without mixing reasons/outcomes | ReviewResult and linked opportunity/plan/position | existing Review foundation | reason/outcome filters | `EMPTY`, `LIST_READY`, `PARTIAL_DATA` | missedReason/laterOutcome separation, source links |
| R07 | Review Detail `/reviews/{reviewId}` | Explain at-time evidence, later outcome and responsibility chain | ReviewResult, InputSnapshot, AITrace, Final snapshot, outcome | existing review and audit records | at-time/later comparison and responsibility UI | `EXECUTED_REVIEW`, `MISSED_REVIEW`, `PARTIAL_CHAIN`, `SOURCE_UNAVAILABLE` | no hindsight overwrite, full role/rule/user attribution |
| R08 | AI Analysis `/analysis`, `/analysis/{analysisId}` | Run Preview or inspect Opportunity Decision analysis | AnalysisRun, EvidenceItem, ScoreItem, AI role outputs, optional Opportunity/Final | analysis-detail assets and structured AI semantics | explicit analysisMode, Preview boundary, persisted route | `PREVIEW_READY`, `PREVIEW_RUNNING`, `PREVIEW_PARTIAL`, `DECISION_READY`, `DECISION_BLOCKED`, `SOURCE_UNAVAILABLE` | Preview creates no Opportunity/Candidate/Final; role/collection states |
| R09 | Message Center `/messages` | Sole business message feed with channel, read, dedupe, cooldown and expiry | Message, ChannelDelivery, PushSnapshot/Recheck | current message read projection and delivery adapter | canonical persistence and exact high-value filters | `EMPTY`, `LIST_READY`, `DELIVERY_PARTIAL`, `EXPIRED_OR_SUPERSEDED` | three categories only, dedupe/cooldown, Telegram not owner |
| R10 | Push Recheck `/recheck/{pushSnapshotId}` | Compare original push snapshot with current result; never authorize a trade | PushSnapshot, PushRecheckRecord, current market/risk/opportunity | existing push snapshot and recheck services | full routed result surface | `UNCHANGED`, `UPGRADED`, `DOWNGRADED`, `INVALIDATED`, `EXPIRED`, `CONFUSED`, `STALE`, `SOURCE_UNAVAILABLE`, `RUNNING`, `FAILED_RETRYABLE` | original/current separation, seven result classes, `notTradeInstruction` |
| R11 | Final Plan Detail `/plans/{planId}` | Show validated Final, lifecycle, source lineage and versions | FinalExecutionPlan, RuleValidationResult, Candidate link, lifecycle records | Final-only renderer and plan sections | lifecycle/version/revalidation | `CURRENT`, `NEEDS_REVALIDATION`, `SUPERSEDED`, `TRACKING_STOPPED`, `INVALIDATED`, `EXPIRED`, `SOURCE_PARTIAL` | Candidate cannot masquerade as Final, version links, source gate |
| R12 | Event Calendar `/calendar` | Show macro/industry/project events and affected assets/plans | Event, EventAssetRelation, PlanRevalidationRecord | current macro event foundation | event window and relation model | `EMPTY`, `CALENDAR_READY`, `EVENT_WINDOW_ACTIVE`, `SOURCE_UNAVAILABLE` | event types, affected scope, revalidation trigger |
| R13 | Full Audit Chain `/audit/{traceId}` | Query one immutable chain from inputs through review | InputSnapshot, Evidence, Scores, AITrace, Resolver, Validation, Recheck, Monitor, Review | current trace/query assets | aggregate route and role/non-role ownership display | `CHAIN_READY`, `PARTIAL_CHAIN`, `NOT_FOUND` | analysisId/candidateId/traceId joins, Resolver not AITrace |
| R14 | My / Settings `/me` | Manage notification, Telegram, risk preferences, default pool, provider and session settings | NotificationSettings, TelegramBinding, RiskPreferences, DefaultPoolConfig, ProviderStatus, Session | UserConfig, provider diagnostics, current auth | channel verification and scoped settings | `READY`, `TELEGRAM_UNBOUND`, `PROVIDER_DEGRADED`, `SAVE_ERROR` | Telegram bind/test, no secret exposure, fail-closed provider status |

Route-state total: `70`.

## 2. Shared Overlay Matrix

| ID | Overlay | Responsibility | Data Owner | Reuse / Change | Required State | Tests |
|---|---|---|---|---|---|---|
| O01 | Status / Recovery Drawer | Explain degraded system/provider state and recovery action | SystemStatus, ProviderStatus | extend existing diagnostics | current scoped failure | no forced READY, action glossary |
| O02 | Quick Asset Search | Find market asset and choose Preview or Pool add | Asset search provider, AssetPoolItem | extend current search | results/empty/unavailable | fuzzy search, no automatic add |
| O03 | Pool Asset Detail | Inspect membership, latest analysis and Opportunity lineage | AssetPoolItem, AnalysisRun, Opportunity | reuse/extend | ready/partial | source lineage and remove effect |
| O04 | Pool Batch Management | Add/remove/scan and top-up/reset with explicit confirmation | AssetPoolItem, AsyncTask | new composition over existing owners | selection/confirm/running/failure | reset versus top-up |
| O05 | FinalPlanDetail Drawer | Reuse Final detail without changing route ownership | FinalExecutionPlan | reuse R11 component | same lifecycle states | Final-only contract |
| O06 | Actual Position Modal | Confirm actual values and create UserPosition manually | UserPosition, FinalExecutionPlan | extend existing manual form | system-plan/manual-independent | CTA modes and explicit confirmation |
| O07 | Close Position Modal | Record actual close and start review path | UserPosition, ReviewResult | reuse current manual close | confirm/success/failure | no automatic close |
| O08 | Audit Detail Drawer | Inspect one audit record without creating a second chain | canonical audit owners | reuse R13 components | ready/not found | immutable source |
| O09 | Async Task Center | Show stage and retry for long-running work | AsyncTask | new independent task owner | queued/running/partial/succeeded/failed/cancelled | no fake percentage |
| O10 | Telegram Binding / Test | Bind destination and send non-business verification | TelegramBinding, ChannelDelivery | extend current adapter | unbound/verifying/ready/error | no second message owner |
| O11 | Event Detail | Show event window, affected scope and revalidation link | Event, EventAssetRelation | extend event owner | ready/source unavailable | no fabricated relation |

Overlay-state total: `11`; Desktop acceptance total: `81`.

## 3. Component Family Registry

| Family | Count | Components | Ownership Rule |
|---|---:|---|---|
| Global | 10 | AppShell, SideNav, PageHeader, SystemStatusBar, StateBadge, EmptyState, AsyncTaskIndicator, Drawer, Modal, AuditMetaDisclosure | visual/shared interaction only; no business owner |
| Asset / Opportunity | 9 | AssetSearch, SearchResultItem, AssetPoolToolbar, AssetPoolTable, PoolScanStatus, OpportunityGrid, OpportunityCard, MultiTimeframeSummary, DataQualityGate | bind canonical Pool, Analysis and Opportunity owners |
| Plan | 8 | PlanSummaryCard, PlanModeHeader, PlanLifecycleBadge, EntryTriggerSection, InvalidationStopSection, TargetTrendSection, RiskLimitSection, FinalPlanDetail | render validated Final only unless explicitly labeled Candidate inside audit |
| AI | 8 | AnalysisModeBanner, AIWorkspace, AIRoleTabs, EvidenceList, MultiTimeframeMatrix, BeforeAfterDiff, FailurePathList, ConflictSummary | one workspace; role and collection states remain independent |
| Position / Review | 8 | PositionRiskAggregate, PositionCard, PositionActualForm, PlanActualComparison, MonitorTimeline, ReviewCard, AtTimeLaterCompare, ResponsibilityChain | UserPosition remains manual and distinct from plan |
| Message / Recheck | 5 | MessageListItem, ChannelDeliveryStatus, OriginalSnapshotCard, RecheckResultHero, RecheckActionBar | Message is fact owner; Recheck is non-trading |
| Event / Settings / Audit | 6 | EventCalendar, EventWindowBadge, TelegramBindingPanel, RiskPreferenceForm, ProviderStatusPanel, AuditChainStepper | reuse Event, UserConfig, Provider and canonical audit owners |

Total component families: `54`.

## 4. Frozen Interaction Flows

| Flow | Required Path | Hard Boundary |
|---|---|---|
| F01 First use | login -> default Pool -> Home no/eligible opportunities | defaults are Pool membership, not fixed Home slots |
| F02 Daily opportunity | Home Top6 -> select asset -> Final -> Three AI -> detail | selection persists in URL; no ranking auto-switch |
| F03 Preview to Pool | search -> ANALYSIS_PREVIEW -> explicit add -> later continuous discovery | Preview creates no Opportunity/Candidate/Final |
| F04 Plan to Position | validated Confirmation/Reduced Final -> actual-position modal -> explicit submit -> UserPosition | no automatic position creation |
| F05 Message to Recheck | Message -> original PushSnapshot -> current Recheck | Recheck never grants trading authority |
| F06 Abnormal recovery | scoped stale/source/confused/Hot Reset -> recovery drawer -> retry/revalidate | no global freeze unless scope is GLOBAL |
| F07 Close to Review | manual close -> remove active Home position -> Review detail | no automatic close; at-time and later facts separate |

## 5. Responsive Boundary

Desktop implementation must cover all 81 acceptance frames. Sixteen Mobile
adaptation scenarios are reserved for a later explicit package. This package
may define responsive collapse principles but must not implement Mobile pages,
Mobile CSS/JS, Mobile screenshots or Mobile navigation.

## 6. Acceptance Rule

No page, overlay or component may fabricate market, AI, progress, opportunity,
plan, position, message or audit data for visual completeness. Missing,
pending, stale, unavailable and failed states are first-class acceptance
frames. A visual fixture is test evidence only and cannot prove runtime data.
