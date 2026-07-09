# Global Backend / Frontend Alignment Audit Before Stress Test

Package: Global Backend/Frontend Alignment Audit Before Stress Test
Branch: `codex/global-backend-frontend-alignment-audit`
Inspected main: `1d4f1466` or newer main after PR #1099 Dashboard AI role semantics restore
Scope: read-only audit report only
Status date: 2026-07-09

This audit reviews whether the current Dashboard Home, Review Center, Position Monitor, Push Recheck, Confused/Hot Reset, Decision/ExecutionPlan, UserPosition, schema/Flyway, and status-source surfaces are semantically aligned before any stress test. It does not run a stress test, does not call external providers, does not access production server/DB, does not print or commit secrets, and does not change runtime behavior.

## 1. Executive Summary

Overall alignment: `PARTIALLY_ALIGNED`.

Stress test readiness: `NOT_READY_FOR_STRESS_TEST`.

`STRESS_TEST_READY: NO`

The backend now has real read-only aggregation for `/api/dashboard/home` and `/api/review/center`, plus meaningful safety mechanisms for Position Monitor, Push Recheck, RuleConfig-driven thresholds, Hot Reset, and provider readiness. The homepage also avoids auto-trading/order semantics and generally preserves empty-state safety.

However, several visible homepage fields are still not reliably wired to the exact backend VO fields. These gaps can make a stress test produce misleading screenshots and false failures: real monitor results can be rendered as `等待监控`, an amount PnL can be formatted as a percent, adjudication consistency has no real backend consistency score/summary contract, and some diagnostics remain frontend-only or `UNKNOWN` even though backend read models exist.

Blockers before stress test:

| Priority | Blocker | Impact |
|---|---|---|
| P0 | Position monitor homepage renderer does not consume `entryLogicStatus`, `directionSupportStatus`, `suggestedManualAction`, or `suggestedManualActionText` from `DashboardHomeVO.PositionVO`. | Real monitor logs may display as `等待监控`; stress screenshots would under-report backend monitor state. |
| P0 | Position monitor homepage renders `floatingPnl` with percent formatting while backend `floatingPnl` is amount and `pnlPct` is the percent field. | Visible PnL semantics are wrong under real data. |
| P0 | Adjudication consistency card asks for a consistency score/summary, but backend `ConsistencyVO` only exposes `level`, `score`, and `confused`; `score` is currently `aiConflictScore`, not a real consistency score. | Card correctly avoids fake score by showing `--`, but the backend/frontend contract is incomplete. |
| P0 | Sidebar/system status contains hardcoded healthy wording such as `AI 服务 正常` instead of using provider readiness. | Stress run can show false healthy state when providers are waiting, missing, or skipped. |
| P1 | Dashboard diagnostics set `opportunityLog` and `review` to `UNKNOWN` even though Review Center and OpportunityLog read paths exist. | Integration status remains stale/ambiguous. |
| P1 | ExecutionSuggestion uses `validPeriod` as a carrier for fail-closed status (`边界不足...` / unsupported timeframe). | Frontend hides it correctly, but backend contract lacks explicit `boundaryStatus` / `failClosedReason`. |
| P1 | Top-card helper `pendingCount` leaks a backend/internal field name. | Main visual still has a technical helper under real payload. |
| P1 | Asset `compositeScore` is intentionally null because no real score exists. | Acceptable empty state, but stress test should not expect scoring until a real score contract exists. |
| P1 | Key event window is selected-symbol external-context only, not a global event window. | Homepage label can imply broader event coverage than the backend source supplies. |

Recommended decision: fix P0 alignment issues before any UI/data stress test. A backend-only stress test of fail-closed services can proceed separately, but homepage visual/data stress should wait.

## 2. Homepage Section Matrix

| Homepage section | Frontend label / function | Backend VO field | Backend service source | DB/source | Semantic meaning | Empty-state rule | Status | Priority |
|---|---|---|---|---|---|---|---|---|
| Header status pills | `数据状态`, `AI 状态`, `数据源`; `renderDashboardHomePayload` | `header.dataStatus`, `header.aiStatus`, `header.dataSourceText`, `header.updatedAt` | `DashboardHomeServiceImpl.buildHeader` | `LightSystemStatusVO`, `PositionSyncStatusVO`, `ProviderReadinessVO`, `ExternalContextSnapshot` | Runtime and provider readiness summary | `WAITING_SYNC` when source missing | `BACKEND_FRONTEND_ALIGNED` | P2 |
| Seven status cards: market trend | `市场趋势`; `renderHomeSystemStateFromPayload` | `systemState.marketTrend` | selected decision or first latest decision | `DecisionResultVO.marketBiasHierarchy` | Selected/first decision directional bias | `待同步` / `—` | `BACKEND_FRONTEND_ALIGNED` | P2 |
| Seven status cards: risk level | `风险等级` | `systemState.riskLevel` | highest risk across visible decisions | `DecisionResultVO.riskLevel` | Aggregate highest risk among visible decision rows | `待同步` / `—` | `BACKEND_FRONTEND_ALIGNED` | P2 |
| Seven status cards: data quality | `数据质量分` | `systemState.dataQuality` | average dataQualityScore across visible decisions | `DecisionResultVO.dataQualityScore` | Average data quality, not selected-symbol quality | `待同步` / blank score | `BACKEND_FRONTEND_ALIGNED` | P2 |
| Seven status cards: AI conflict | `AI 冲突等级` | `systemState.aiConflict` | highest conflict score/level across decisions | `DecisionResultVO.aiConflictLevel`, `aiConflictScore` | Aggregate AI conflict severity | `待同步` | `BACKEND_FRONTEND_ALIGNED` | P2 |
| Seven status cards: pending review | `待复核机会` | `systemState.pendingReview` | `LightSystemStatusVO.pendingCount` | system status read model | Pending review count; not missed-valid count | `暂无` / `待同步` | `SEMANTIC_DRIFT` because helper leaks `pendingCount` | P1 |
| Seven status cards: confused | `冲突阻断` | `systemState.confused` | `LightSystemStatusVO.confusedCount`; fallback count decision `confusedScore > 0` | system status + decisions | Confused backlog/state signal | `待同步` | `BACKEND_FRONTEND_ALIGNED` | P2 |
| Seven status cards: hot reset | `热重置` | `systemState.hotReset` | `LightSystemStatusVO.hotResetFired` and metadata | system status / hot reset read model | Whether recent hot reset fired | `未触发` / `暂无` | `BACKEND_FRONTEND_ALIGNED` but not full event list | P2 |
| Risk alerts | `风险告警`; `renderHomeAlertEventRowsFromPayload` | `alerts[]` | `monitorService.getRecentAlerts(2)` | `MonitorAlertDO` | Two recent risk alerts | `暂无高优先级告警`; `等待风险事件同步` | `BACKEND_FRONTEND_ALIGNED` | P2 |
| Key event window | `关键事件窗口` | `events[]` | `ExternalContextEvidenceBuilder.buildSnapshot` for selected symbol | External context snapshot | Latest selected-symbol external event | `暂无高影响事件`; `等待事件窗口同步` | `PARTIALLY_ALIGNED`; not global event window | P1 |
| Watched asset cards | `重点资产监控`; `renderHomeAssetsFromPayload` | `assets[]` | `buildAssets` over latest decisions plus default slots | `DecisionResultVO`, fixed BTC/ETH/SOL/BNB/XRP/DOGE placeholders | Decision cards for six display slots | Blank clean fields for default slots | `PARTIALLY_ALIGNED`; score missing by design | P1 |
| Position monitor table | `资产 / 方向`, `入场逻辑`, `方向支持`, `反转状态`, `风险等级`, `当前建议`, `下次验证`, `人工处理`; `renderHomePositionsFromPayload` | `positions[]` | `buildPositions` from open manual positions and latest monitor log | `UserPositionVO`, `PositionMonitorLogDTO`, optional `MarketQuoteClient` | Real manual-position monitoring rows only | five-row table, `暂无持仓`, `等待监控` | `WRONG_SOURCE_MAPPING` | P0 |
| Execution suggestion | `执行建议`; `renderHomeExecutionFromPayload` | `executionSuggestion` | selected decision mapping | `DecisionResultVO` execution fields | Read-only execution-plan review fields | compact fail-closed status when boundary incomplete | `PARTIALLY_ALIGNED`; status overloaded into `validPeriod` | P1 |
| AI role tabs | `最终裁决官`, `冲突复核官`, `反方挑战官`; `renderHomeAiDecisionFromPayload` | `aiDecision.tabs[]` | `buildAiDecision` parses `aiRoleResults` | `DecisionResultVO.aiRoleResults` + selected decision fallbacks for GPT_FINAL | Role-scoped AI evidence summaries | role-level empty fields / `暂无该角色证据` | `BACKEND_FRONTEND_ALIGNED` with fallback caveats | P1 |
| Adjudication consistency card | `裁决一致性`; `renderHomeConsistencyCard` | `aiDecision.consistency` | `buildAiDecision` | `DecisionResultVO.aiConflictLevel`, `aiConflictScore`, `confusedScore` | Intended synthesis of three AI roles | score `--`, summary waiting text | `MISSING_BACKEND_SOURCE` / `BACKEND_FIELD_UNUSED` | P0 |
| Diagnostics / integration status | collapsed diagnostics + sidebar system status | `diagnostics` plus many independent runtime status fetches/constants | `buildDiagnostics`, many dashboard-only status endpoint snippets | provider readiness, push inbox, position sync, frontend constants | Integration readiness, safety diagnostics | fail-closed or hidden/collapsed | `PLACEHOLDER_ONLY` / `FRONTEND_ONLY_SEMANTIC` in several panels | P1 |

## 3. Field-Level Traceability Matrix

| Field / label | Frontend read path | Backend field | Backend source | Traceability status | Note |
|---|---|---|---|---|---|
| Header data status | `header.dataStatus` | `DashboardHomeVO.HeaderVO.dataStatus` | `LightSystemStatusVO.status` | `ALIGNED` | Localized in frontend. |
| Header AI status | `header.aiStatus` | `HeaderVO.aiStatus` | `ProviderReadinessVO.aiProviderStatus` | `ALIGNED` | Does not prove provider live PASS. |
| Market trend | `systemState.marketTrend.valueLabel/value` | `StatusCardVO` | selected/first decision `marketBiasHierarchy` | `ALIGNED` | Selected symbol preferred. |
| Risk level | `systemState.riskLevel` | `StatusCardVO` | highest `DecisionResultVO.riskLevel` | `ALIGNED` | Aggregate rule should be documented in UI spec. |
| Data quality | `systemState.dataQuality.score/value` | `StatusCardVO.score/value` | average `dataQualityScore` | `ALIGNED` | Average, not selected-symbol. |
| AI conflict top card | `systemState.aiConflict` | `StatusCardVO.score/value` | highest `aiConflictScore/level` | `ALIGNED` | Correct for conflict, not consistency. |
| Pending review | `systemState.pendingReview` | `StatusCardVO.value/helper` | `LightSystemStatusVO.pendingCount` | `PARTIAL` | Helper emits `pendingCount`. |
| Asset symbol | `asset.rawSymbol/symbol` | `AssetVO.rawSymbol/symbol` | decisions + defaults | `ALIGNED` | Six fixed display slots. |
| Asset direction | `asset.marketBiasLabel/marketBias` | `AssetVO.marketBias*` | `DecisionResultVO.marketBiasHierarchy` | `ALIGNED` | Placeholder blank when no decision. |
| Asset composite score | `asset.compositeScore` | `AssetVO.compositeScore` | set `null` | `MISSING_BACKEND_SOURCE` | Frontend keeps pill label only; no fake score. |
| Asset risk/confidence | `riskLabel/riskLevel`, `confidenceLabel/confidenceLevel` | `AssetVO` | `DecisionResultVO` | `ALIGNED` | Labels localized. |
| Position entry logic | frontend looks for `entryLogic`, `positionEntryLogic`, etc. | backend sends `entryLogicStatus` | latest `PositionMonitorLogDTO.logicStatus` or `WAITING_MONITOR` | `WRONG_SOURCE_MAPPING` | Backend field is not read. |
| Position direction support | frontend looks for `directionSupport`, etc. | backend sends `directionSupportStatus` | derived from monitor log logic status | `WRONG_SOURCE_MAPPING` | Backend field is not read. |
| Position reversal status | frontend reads `reversalStatus` | backend sends `reversalStatus` | derived from monitor log | `ALIGNED` | Label localization should be tested. |
| Position risk level | frontend reads `riskLevel` | backend sends `riskLevel` | latest monitor log or `WAITING_SYNC` | `ALIGNED` | Empty fallback says `等待监控`, slightly different from backend `WAITING_SYNC`. |
| Position current advice | frontend reads `currentAdvice`, `monitorSuggestedAction`, `suggestedAction` | backend sends `suggestedManualAction` and `suggestedManualActionText` | latest monitor log suggested action or `MANUAL_REVIEW` | `WRONG_SOURCE_MAPPING` | Actual backend suggestion text can be lost. |
| Position floating PnL | frontend `formatPct(p.floatingPnl)` | backend `floatingPnl` amount; `pnlPct` percent | computed from manual position + current price | `WRONG_SOURCE_MAPPING` | Amount displayed as percent. |
| Position close action | `positionCloseActionHtml` | `positionId`, `positionStatus` | open manual UserPosition | `ALIGNED` | Uses manual close wording only. |
| Execution entry/stop/TP | `s.entryZone`, `s.stopLoss`, `s.takeProfitRules` | `ExecutionSuggestionVO` | selected `DecisionResultVO` | `ALIGNED` | No cross-field fallback. |
| Execution valid period | `s.validPeriod` but hidden if fail-closed | `ExecutionSuggestionVO.validPeriod` | decision valid period or fail-closed sentinel | `SEMANTIC_DRIFT` | Needs explicit readiness/fail reason field. |
| AI role tabs | `aiDecision.tabs[]` | `AiTabVO` | parsed `aiRoleResults` | `ALIGNED` | GPT_FINAL has decision fallback values. |
| Consistency score | frontend `consistencyScore/agreementScore` only | backend `ConsistencyVO.score` | `DecisionResultVO.aiConflictScore` | `BACKEND_FIELD_UNUSED` | Correctly avoids fake score but contract missing. |
| Consistency summary | frontend `summary/conclusion/oneSentenceSummary` | not in `ConsistencyVO` | none | `MISSING_BACKEND_SOURCE` | Always waiting unless future field added. |
| Diagnostics opportunity/review | `diagnostics.opportunityLog/review` | set `UNKNOWN` | none in `DashboardHomeServiceImpl` | `MISSING_BACKEND_SOURCE` | Review Center has data; home diagnostics do not consume it. |
| Push inbox counts | `pushInbox.counts` | `PushCountsVO` | `PushSnapshotMapper`, `PushRecheckLogMapper` | `PARTIAL` | `positionRisk` currently hardcoded 0. |
| Telegram status | `pushInbox.telegramStatus`, `diagnostics.telegram` | fixed `WAITING_SYNC` | no verified connection source | `ALIGNED_FAIL_CLOSED` | Correct until verified source exists. |

## 4. Frontend-Only Semantic Audit

Frontend-only or mostly frontend-owned semantics found:

1. Collapsed diagnostics panels define many runtime status constants directly in `dashboard.html`, including Watchlist, RuleConfig audit, MarketQuote, Evidence/Score, DecisionResult, ExecutionPlan boundary, RiskActionGuard, PaperObservation, SourceRuntime, MissedArchive, RuntimeReadiness, AccountRisk, HotReset event source, RecheckPreview, and InternalPushPreview statuses.
2. These panels are generally marked review-only/fail-closed, which is safe, but they are not a single normalized `/api/dashboard/home` contract.
3. Sidebar health currently hardcodes healthy-looking labels such as `AI 服务` -> `正常`; this can contradict provider readiness from `/api/dashboard/home`.
4. Position countdown is purely frontend cadence based on refresh timing. This is acceptable for display, but it should not be interpreted as a backend monitor SLA.
5. Asset display slots are frontend-fixed to six symbols, while backend also fills default slot placeholders. This is aligned as display-only, not watchlist/candidate semantics.
6. The adjudication consistency ring is frontend-safe because it shows `--` unless future `consistencyScore/agreementScore` exists. It is still frontend-only until backend provides those fields.

Frontend-only status: `PARTIALLY_SAFE_BUT_NOT_STRESS_READY`.

## 5. Backend-Only / Unused Field Audit

Backend fields not fully consumed by the current homepage:

| Backend field | Current frontend behavior | Status | Priority |
|---|---|---|---|
| `PositionVO.entryLogicStatus` | frontend looks for `entryLogic` aliases, not `entryLogicStatus` | `BACKEND_FIELD_UNUSED` | P0 |
| `PositionVO.directionSupportStatus` | frontend looks for `directionSupport` aliases, not `directionSupportStatus` | `BACKEND_FIELD_UNUSED` | P0 |
| `PositionVO.suggestedManualAction` | frontend does not include this exact field in current advice lookup | `BACKEND_FIELD_UNUSED` | P0 |
| `PositionVO.suggestedManualActionText` | frontend does not prefer this display text | `BACKEND_FIELD_UNUSED` | P0 |
| `PositionVO.pnlPct` | frontend instead formats `floatingPnl` as percent | `BACKEND_FIELD_UNUSED` / `WRONG_SOURCE_MAPPING` | P0 |
| `PositionVO.accountImpactPct` | not displayed in homepage basic metadata | `BACKEND_FIELD_UNUSED` | P2 |
| `ConsistencyVO.score` | frontend deliberately ignores because it is conflict score, not consistency score | `BACKEND_FIELD_UNUSED` | P0 |
| `DiagnosticsVO.opportunityLog` / `review` | backend hardcodes `UNKNOWN` | `MISSING_BACKEND_SOURCE` | P1 |
| `PushCountsVO.positionRisk` | backend hardcodes 0 and frontend may display it | `PLACEHOLDER_ONLY` | P1 |

## 6. AI Role Alignment Audit

Current AI role model:

| Role | Backend source | Frontend display | Alignment |
|---|---|---|---|
| `GPT_FINAL` | parsed `DecisionResultVO.aiRoleResults`, with selected decision fallback for final bias/confidence/risk/plan mode/worth opening | final adjudicator panel | `PARTIALLY_ALIGNED` |
| `GEMINI_REVIEW` | role JSON only | conflict reviewer panel | `ALIGNED_EMPTY_SAFE` |
| `GROK_CHALLENGE` | role JSON only | counter-challenge panel | `ALIGNED_EMPTY_SAFE` |
| Consistency synthesis | backend `ConsistencyVO.level/score/confused`; `score` from `aiConflictScore` | synthesis card wants consistency level/score/summary/downgrade | `MISSING_BACKEND_SOURCE` |

Findings:

1. Role tabs are semantically separate after PR #1099 and no longer use identical generic field lists.
2. Empty role panels do not collapse to a single row and do not fabricate evidence.
3. `GPT_FINAL` fallback to selected `DecisionResultVO` is useful, but should be documented as decision read-model fallback, not necessarily raw GPT output.
4. Consistency is not a fourth AI role, but backend currently exposes conflict fields rather than a true three-role agreement object.
5. There is no explicit backend `consistencyScore`, `agreementScore`, `oneSentenceSummary`, or `downgradeReason` field under `ConsistencyVO`.

AI alignment status: `PARTIALLY_ALIGNED`.

## 7. Position / Execution Plan Boundary Audit

Position monitor:

- Backend source is correctly restricted to real open manual positions via `userPositionService.listOpenPositions()` and `sourceType == MANUAL`.
- Closed positions are excluded by backend open-position source and frontend local close guard.
- ExecutionPlan is not converted into UserPosition.
- Latest monitor log can provide current price, logic status, risk level, and suggested action; fallback current price can come from `MarketQuoteClient`.
- Backend computes `floatingPnl` amount, `pnlPct`, and `accountImpactPct` for LONG/SHORT when entry/current price are real.
- Frontend field aliases are currently wrong for several important fields, so real monitor outputs can be hidden.

Execution suggestion:

- Backend source is selected `DecisionResultVO`, not UserPosition.
- `entryZone`, `stopLoss`, `takeProfitRules`, `leverageSuggestion`, `positionSuggestion`, `validPeriod`, and `invalidCondition` have direct mappings.
- Unsupported timeframe and incomplete boundary fail closed.
- Frontend correctly moves `边界不足，等待结构确认` into a compact status and keeps valid period blank.
- Backend still overloads `validPeriod` with a fail-closed message. A dedicated `boundaryStatus` / `failClosedReason` would be safer for stress tests.

Boundary status: `PARTIALLY_ALIGNED_WITH_P0_POSITION_RENDER_GAPS`.

## 8. Push Recheck / Confused / Hot Reset Alignment Audit

Push Recheck:

- Backend preserves caller-provided `currentPrice` API and can fetch current price through `MarketQuoteClient` when omitted.
- Missing quote, null last price, thrown quote client, or missing snapshot symbol fail closed to `INVALIDATED` with `QUOTE_UNAVAILABLE` or `PRICE_REQUIRED` in `fail_reason_json`.
- Tests cover missing quote fail-closed cases and valid provided/current quote behavior.
- Homepage push inbox reads counts/items from snapshot/recheck read models but `positionRisk` remains placeholder 0.

Confused:

- Asset state enum includes `CONFUSED`.
- Dashboard top card uses `LightSystemStatusVO.confusedCount`, with fallback count from decision `confusedScore`.
- Push Recheck can block on confused thresholds from `push_recheck_config`.
- Homepage does not expose full confused enter/exit reasons; that belongs to detail/diagnostics.

Hot Reset:

- `RuleConfigContractService` defines `hot_reset_config` and `HotResetServiceImpl` reads thresholds fail-closed through `requireHotResetThresholds()`.
- `HotResetPolicy.evaluate(command)` without thresholds returns `HOT_RESET_CONFIG_NOT_READY`, preventing silent permissive defaults.
- `HotResetServiceImpl` persists hot reset events, invalidates decisions/plans/push snapshots, and triggers rebuild only after commit.
- Homepage top card uses `LightSystemStatusVO.hotResetFired`/metadata, not full hot reset event log.

Alignment status: `BACKEND_MECHANISMS_PARTIAL_HOMEPAGE_SUMMARY`.

## 9. Empty-State Contract Audit

| Area | Empty-state behavior | Status |
|---|---|---|
| Header | `WAITING_SYNC` / localized waiting labels | `ALIGNED` |
| Seven top cards | `待同步`, `暂无`, `未触发`; pending helper leaks `pendingCount` | `PARTIAL` |
| Alerts/events | two compact fallback rows | `ALIGNED` |
| Asset cards | fixed cards remain; values blank when no real data; score not faked | `ALIGNED` |
| Position table | five-row empty table, `暂无持仓`, `等待监控` | `ALIGNED` |
| Position active rows | fallback `等待监控`; currently masks real fields due alias mismatch | `MISALIGNED` |
| Execution suggestion | fail-closed card + blank field values | `ALIGNED_FRONTEND`, `PARTIAL_BACKEND` |
| AI role tabs | role labels remain; missing evidence text is compact | `ALIGNED` |
| Consistency card | labels remain; score `--`; summary waiting | `ALIGNED_FRONTEND`, `MISSING_BACKEND_SOURCE` |
| Diagnostics | collapsed fail-closed/read-only copy, many frontend constants | `PARTIAL` |

## 10. Test Coverage Gap

Existing useful tests:

1. `DashboardHomeServiceImplTest` covers stable read-only aggregation, pending count source, data-quality average, highest risk, fixed asset cards, manual-position-only source, no ExecutionPlan-as-position fallback, execution suggestion mapping, push inbox counts, Telegram `WAITING_SYNC`, and safety flags.
2. `DashboardControllerTest` covers AI role-specific rendering, consistency card labels, no fake consistency score, waiting state copy, and no forbidden trading instructions.
3. `PushRecheckServiceImplTest` covers quote-unavailable fail-closed paths.
4. `PositionMonitorServiceImplTest` covers open-position monitoring, closed-position exclusion, PnL LONG/SHORT, near stop/take profit, high risk, plan invalidation, and safety fields.
5. `ReviewCenterServiceImplTest` covers Review Center summary and readonly source mapping.

Missing or insufficient tests before stress:

| Gap | Needed test | Priority |
|---|---|---|
| Position homepage field aliases | Template test asserting `renderHomePositionsFromPayload` reads `entryLogicStatus`, `directionSupportStatus`, `suggestedManualAction`, and `suggestedManualActionText` | P0 |
| PnL unit correctness | Template test asserting `floatingPnl` uses amount formatting and `pnlPct` uses percent formatting | P0 |
| Consistency backend contract | Service/VO test for actual `consistencyScore/agreementScore` once added, or explicit assertion that score remains blank until real field exists | P0 |
| Sidebar false health | Template test preventing hardcoded `AI 服务 正常` and requiring provider readiness mapping | P0 |
| Dashboard diagnostics Review/Opportunity | Service test wiring diagnostics to Review Center / OpportunityLog readiness or explicitly classifying as omitted | P1 |
| Top card helper no technical key | Template/service test preventing visible `pendingCount` helper | P1 |
| Execution boundary state | Service test for explicit `boundaryStatus/failClosedReason` if contract is added | P1 |
| Key event scope | Test documenting selected-symbol external event only, or new global event source | P2 |

## 11. Stress Test Readiness Gate

Stress test gate decision: `NOT_READY_FOR_STRESS_TEST`.

`STRESS_TEST_READY: NO`

Must pass before homepage/data stress test:

1. Fix position monitor renderer field aliases to consume backend VO fields exactly.
2. Fix PnL display unit mapping: `floatingPnl` as amount, `pnlPct` as percent.
3. Add or explicitly decide the adjudication consistency backend contract: `consistencyScore/agreementScore`, `summary`, and downgrade/block reason, or keep score permanently omitted.
4. Remove hardcoded healthy provider/system labels from sidebar and map them to provider readiness or waiting states.
5. Wire Dashboard diagnostics `opportunityLog` and `review` to existing readonly backend sources, or explicitly remove them from the homepage diagnostics contract.
6. Remove visible technical helper text such as `pendingCount` from top cards.
7. Add targeted regression tests for the above.

Allowed separately before these fixes:

- Backend-only service stress on Push Recheck fail-closed behavior.
- Backend-only PositionMonitor tests using controlled MarketQuote fixtures.
- Review Center readonly API load checks.

Not allowed yet:

- End-to-end homepage visual/data stress test that would be used as acceptance evidence.
- Production deployment stress or public release stress.
- Any stress path that calls external AI providers, production DB/server, external push send, or trading/order endpoints.

## 12. Recommended Fix Packages

### Package A: Dashboard Position Monitor VO Field Alignment (P0)

Scope: `dashboard.html` + targeted `DashboardControllerTest` only unless backend naming is changed intentionally.

Required fixes:

1. Update position monitor renderer to read `entryLogicStatus` and `directionSupportStatus`.
2. Update current advice renderer to prefer `suggestedManualActionText`, then `suggestedManualAction`.
3. Display `floatingPnl` as amount and `pnlPct` as percent.
4. Keep manual close wording as `记录平仓` only.
5. Add tests for exact aliases and no buy/sell/order/auto wording.

### Package B: Adjudication Consistency Contract Closure (P0)

Scope: `DashboardHomeVO`, `DashboardHomeServiceImpl`, `dashboard.html`, focused tests.

Required decision:

1. Either add real `consistencyScore/agreementScore`, `summary`, and `downgradeReason` fields from a backend resolver, or keep the card score permanently blank and relabel it as conflict summary.
2. Do not reuse `aiConflictScore` as a consistency score.
3. Add tests proving missing score renders `--` and real score only renders when backend provides an actual consistency score.

### Package C: Dashboard Diagnostics Source Cleanup (P1)

Scope: `DashboardHomeServiceImpl` + tests, or frontend copy removal if diagnostics are intentionally out of home scope.

Required fixes:

1. Wire `diagnostics.opportunityLog` and `diagnostics.review` to existing readonly sources, or remove these labels from home diagnostics.
2. Replace static sidebar `AI 服务 正常` with provider readiness mapping.
3. Keep all diagnostics collapsed/read-only/fail-closed.

### Package D: Execution Suggestion Boundary Status Field (P1)

Scope: `DashboardHomeVO`, `DashboardHomeServiceImpl`, tests, small frontend mapping.

Required fixes:

1. Add explicit `boundaryStatus` and `failClosedReason` or equivalent.
2. Stop using `validPeriod` as the carrier for fail-closed reasons.
3. Keep `validPeriod` only for complete/usable plan validity.

### Package E: Homepage Top Card Copy and Field Contract Polish (P1/P2)

Scope: `DashboardHomeServiceImpl`, `dashboard.html`, tests.

Required fixes:

1. Replace `pendingCount` helper with user-facing copy.
2. Clarify aggregate-vs-selected semantics for risk/data quality/conflict.
3. Document asset `compositeScore` as missing until a real score exists.
4. Clarify key event window as selected-symbol external context unless a global source is added.

## Safety Boundary Confirmation

This audit found no need to add or permit:

- auto-open
- auto-close
- auto-reverse
- order execution
- auto-trading
- external push send
- fake positions
- fake review records
- production-ready claim

## 13. P0 Closure Addendum (2026-07-10, after PR #1101)

PR #1101 was merged into main as `1b0cf721`, closing the P0 dashboard backend/frontend alignment blockers identified in this audit.

Closure review document: `docs/P0_BACKEND_FRONTEND_ALIGNMENT_CLOSURE_REVIEW.md`

`P0_ALIGNMENT_STATUS: CLOSED`

`STRESS_TEST_PREPARATION_ALLOWED: YES`

`STRESS_TEST_EXECUTION_ALLOWED: NO`

Production readiness remains `BLOCKED`.

P0 closure matrix:

| P0 blocker | Closure status | Closure evidence |
|---|---|---|
| Position monitor renderer did not consume `entryLogicStatus`, `directionSupportStatus`, `suggestedManualAction`, or `suggestedManualActionText`. | `CLOSED` | `DashboardHomeVO.PositionVO` exposes the fields; `DashboardHomeServiceImpl.buildPositions()` populates them from latest `PositionMonitorLogDTO`; `dashboard.html` reads the exact camel/snake fields; tests cover real monitor fields, CLOSED exclusion, and no ExecutionSuggestion-as-position fallback. |
| `floatingPnl` rendered as percent though backend `floatingPnl` is amount. | `CLOSED` | `dashboard.html` now uses `formatSignedAmount(p.floatingPnl)` and no longer applies `formatPct()` to `floatingPnl`; service keeps `floatingPnl` amount and `pnlPct` percent separate. |
| Adjudication consistency lacked explicit backend contract and risked confusing AI conflict score with consistency score. | `CLOSED` | `ConsistencyVO` now has `consistencyScore`, `consistencyLevel`, `consistencySummary`, and `downgradeReason`; missing consistency score remains `null`; the UI shows `--` and does not derive `100 - aiConflictScore` or use conflict level as consistency fallback. |
| Sidebar/system status hardcoded healthy labels such as `AI 服务 正常`. | `CLOSED` | `renderSidebarPanel()` now uses backend diagnostics/readiness and `WAITING_SYNC` fallback; tests prevent hardcoded healthy sidebar labels. |

Remaining non-P0 items from this audit stay open for later packages, including diagnostics `opportunityLog` / `review` cleanup, explicit execution boundary status fields, top-card helper copy polish, asset score contract, and selected-symbol event-window clarification.

Stress-test preparation may proceed as the next package, but stress-test execution still requires an explicit plan/harness package defining target, scope, baseline data, safety guard, pass/fail criteria, evidence outputs, and stop conditions.

## Validation Notes

No runtime code was changed by this audit package. The report was authored from read-only repository inspection through the GitHub connector because the local Codex shell in this session could not start `/bin/zsh`, `/bin/bash`, or `/bin/sh` after context transition.

Required local validation still needs to be run before merging this docs-only branch:

1. `git diff --check`
2. `bash scripts/check-workflow-contract.sh`
3. `bash scripts/v1-delivery-check.sh`
4. `bash scripts/v1-state.sh`

Because this package is docs-only and does not change YAML or Java/tests, `./mvnw test -q` is not required by the task unless maintainers choose to run the full gate anyway.
