# Dashboard Offline Interaction Acceptance

## Scope and method

This acceptance closes the Dashboard Home interaction semantics on branch `codex/dashboard-state-semantics-audit-fix`. It is offline and read-only:

- Backend states are exercised with deterministic service fixtures and the existing in-memory integration-test database.
- Frontend ownership and interaction order are exercised against the shipped `dashboard.html` source through deterministic call-graph and DOM-target assertions in `DashboardControllerTest`.
- `scripts/dashboard-visual-acceptance-fixture.py` includes `ai-all-abstain` plus verified/unverified position-source scenarios and can generate a self-contained, no-network fixture when localhost binding is unavailable. Reviewer regressions use deterministic fixture/DOM-target assertions; they do not claim a new PNG screenshot.
- No AI/provider call, secret read, scheduler trigger, position mutation, order action, external Push, or Telegram action occurs.

## Scenario acceptance

| Scenario | Operation | Expected | Actual | DOM or test evidence | Result |
|---|---|---|---|---|---|
| AI disabled plus asset directional block | Initial Home load | AI consistency and plan mode are `不适用`; asset directional block remains independently `是`; no synthetic extreme divergence | Home read model separates `aiApplicable=false` from `directionalPushBlocked=true`; disabled role contains no business result | `aiNotApplicableOverridesDirectionalBlockInConsistencyCard`, `disabledAiRoleHasNoBusinessResult`, `headerDisabledAiShowsChineseDisabledLabel` | PASS |
| Three successful AI roles all abstain | Initial Home load and all three role tabs | Header remains `正常`; every role says `复核成功 / 证据不足，暂不判断`; KPI, consistency, conflict, and plan mode are not applicable; score is `--` | Applicability requires support or challenge, not merely a successful call; the exact all-abstain summary is retained | `allThreeSuccessfulAbstainMakeConsistencyNotApplicable`, `allAbstainKpiShowsNotApplicable`, `allAbstainOfflineFixtureUsesNotApplicableDomContract` | PASS |
| AI timeout | Initial Home load and role-tab view | Role shows timeout status and explanation only; no direction, confidence, risk, or AI plan | `resultAvailable=false`; role renderer exits through the unavailable-state panel | `timeoutAiRoleHasNoBusinessResult`, `unavailableAiRoleRendersStatusOnly` | PASS |
| Data quality insufficient | Initial Home load | Final suggestion is fail-closed and contains no plan boundary | Low quality produces `DATA_QUALITY_BLOCKED`; all direction/entry/stop/TP/validity fields are clear | `lowDataQualityAndMissingAnalysisSnapshotHideEveryPlanBoundary` | PASS |
| Plan expired | Initial Home load at before/equal/after expiry clocks | Before expiry is usable; equal/after expiry are blocked and plan fields are clear | Offset-aware `Instant` comparison blocks at `now >= expiresAt` | `expiredAbsoluteValidPeriodBlocksSuggestion`, `offsetAwarePlanExpiryIsTimezoneIndependent` | PASS |
| Asset-state trace differs from decision analysis trace | Initial Home load | Current asset state remains visible; execution plan says `状态已更新，原计划需重新分析` and exposes no boundary | Trace mismatch returns `STATE_SNAPSHOT_MISMATCH` and clears plan fields | `mismatchedAssetStateTraceBlocksPlan` | PASS |
| Manual position with source-verified monitor record | Initial Home load and position panel view | Position monitoring takes over; persisted monitor result/time and calculated PnL are shown; exact source plan A is collapsed for historical review; newer same-symbol plan B is absent | Position mode uses the same-position monitor plan/analysis IDs and read-only current quote. Plan, decision, run, analysis ID, and symbol are verified before plan fields are exposed | `positionFromPlanA_latestDecisionB_neverShowsBAsOriginalPlan`, `monitorExecutionPlanIdResolvesExactOriginalPlan`, `monitorAnalysisIdResolvesExactOriginalPlan`, `findByAnalysisIdAndPlanIdJoined_returnsExactSourcePlanInsteadOfLatestSiblingPlan` | PASS |
| Manual position without a verifiable plan source | Initial Home load and position panel view | Monitoring fields remain available, but the plan area says `暂无可关联的原执行计划`; no empty disclosure or plan B fields appear | No monitor source means `originalPlanIdentity=UNVERIFIED`; ambiguous `sourceRefId` is diagnostic-only and no latest-symbol fallback runs | `positionWithNoSourceReferenceHidesOriginalPlan`, `activePositionNeverFallsBackToLatestSymbolDecision`, `unverifiedPositionSourceDoesNotRenderEmptyOriginalPlanTable` | PASS |
| Source plan belongs to another symbol | Initial Home load | Original plan fails closed and no current/new same-symbol plan replaces it | Exact source identity requires position, decision, and analysis-run symbols to match | `originalPlanSymbolMismatchFailsClosed` | PASS |
| Source plan A trace differs from current state B | Initial Home load | Plan A may remain as an explicitly stale historical review record; plan B is never promoted | Identity stays `VERIFIED`; current validity becomes `STATE_MISMATCH` with `状态已更新，原计划不再作为当前执行依据` | `sourceTraceMismatchDoesNotPromoteNewDecision` | PASS |
| Manual position without monitor record | Initial Home load and position panel view | Monitoring fields say `等待首次监控`; no monitor result or next time is fabricated | No log maps to `WAITING_MONITOR`; a prior log with no next schedule instead says `暂无下次监控排期` | `existingMonitorWithoutNextScheduleDoesNotSayFirstMonitor` | PASS |
| Backend `DEFAULT_SLOT` asset | Initial Home load, then attempted tile/sidebar click | Placeholder says `等待首轮分析`, is noninteractive, and starts no detail/Home selection | Both tile and sidebar identify `slotType=DEFAULT_SLOT`, omit a selectable symbol, and reject click handling | `defaultSlotIsNonInteractivePlaceholder` | PASS |
| Home API failure | Refresh | Cached position/plan/AI conclusions are cleared; main surface says `首页数据暂不可用`, `等待重新同步`, `当前不展示执行计划` | Failure path calls only the unavailable Home renderer plus diagnostics refresh; summary cannot reconstruct Home | `homeApiFailureRendersFailClosedEmptyState`, `legacyHomeRenderersAreNotCalledByCurrentHomeFlow` | PASS |

## Interaction-order acceptance

| Operation | Expected | Actual | DOM or test evidence | Result |
|---|---|---|---|---|
| First load | Fetch Home, render all main semantics once, then request selected-symbol detail | `refreshDashboard -> fetchDashboardHome -> renderDashboardHomePayload`; detail is requested only after Home resolves | `legacyHomeRenderersAreNotCalledByCurrentHomeFlow`, `detailResponseCannotOverwriteHomeSemanticSections` | PASS |
| Click asset tile | Normalize symbol, assign `selectedSymbol`, refresh Home | Click handler assigns symbol before `refreshDashboard()` | `sidebarSelectionRefreshesDashboardHome` | PASS |
| Click sidebar asset | Normalize symbol, assign `selectedSymbol`, refresh Home | Sidebar handler assigns symbol before `refreshDashboard()` | `sidebarSelectionRefreshesDashboardHome` | PASS |
| Select search result | Normalize symbol, assign `selectedSymbol`, refresh Home | Search selection assigns symbol before `refreshDashboard()` | `sidebarSelectionRefreshesDashboardHome` | PASS |
| Refresh page | Home remains the sole successful main renderer | No summary/detail renderer is reachable as a Home business renderer | `legacyHomeRenderersAreNotCalledByCurrentHomeFlow` | PASS |
| Detail response arrives after Home | Home position, execution, AI role, consistency, tiles, and KPIs remain unchanged | Detail call graph updates only status cards, workbench, detail sections, and diagnostics | `detailResponseCannotOverwriteHomeSemanticSections` | PASS |
| Home request fails | Fail closed without a legacy detail/summary plan fallback | Home caches are cleared and diagnostics refresh has no Home semantic renderer | `homeApiFailureRendersFailClosedEmptyState` | PASS |

## Decision

Offline Dashboard interaction acceptance: **PASS** for the scoped semantic contract.

Plan `validFrom`/`expiresAt` are offset-aware authority. The historical Push Snapshot timestamp remains no-timezone storage, but its production and consumption contract is explicitly UTC-naive. AI call success does not establish consistency applicability, `ABSTAIN` is neither support nor objection, and the AI conflict KPI is derived from the same backend consistency object as the card. In position mode, latest-symbol decision B is never an identity fallback for source plan A; missing provenance hides all historical plan fields.

This is not production acceptance. The eight-state lifecycle remains incomplete, the three-bar-per-timeframe market-bias policy remains provisional, Flyway V7 still needs separate controlled PostgreSQL evidence, and production deployment readiness remains **BLOCKED**.
