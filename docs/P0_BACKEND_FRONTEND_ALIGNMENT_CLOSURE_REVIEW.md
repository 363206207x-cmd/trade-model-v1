# P0 Backend / Frontend Alignment Closure Review

Package: P0 Backend/Frontend Alignment Closure Review
Branch: `codex/p0-alignment-closure-review`
Reviewed main: `1b0cf721` or newer, after PR #1101
Scope: read-only closure review plus docs/status update only
Status date: 2026-07-10

This review verifies whether the P0 dashboard backend/frontend alignment blockers from `docs/GLOBAL_BACKEND_FRONTEND_ALIGNMENT_AUDIT.md` are closed after PR #1101. It does not execute a stress test, call external providers, access production server/DB, print or commit secrets, or change runtime behavior.

## 1. Executive Summary

`P0_ALIGNMENT_STATUS: CLOSED`

`STRESS_TEST_PREPARATION_ALLOWED: YES`

`STRESS_TEST_EXECUTION_ALLOWED: NO`

Production readiness remains `BLOCKED`.

All four P0 blockers from the global backend/frontend alignment audit are closed on main after PR #1101:

1. Position monitor homepage rendering now consumes the real `DashboardHomeVO.PositionVO` monitor fields and remains restricted to active manual positions.
2. `floatingPnl` is displayed as an amount, not as a percent.
3. Adjudication consistency now has explicit backend contract fields; missing `consistencyScore` remains `null` and the UI shows `--` rather than deriving a fake score.
4. Sidebar health/status rendering no longer hardcodes unsupported healthy labels such as `AI 服务 正常`; it uses backend diagnostics/readiness or waiting fallbacks.

Stress-test preparation can start because the P0 screenshot/data-misleading blockers are closed. Stress-test execution is still not allowed until a follow-up package defines the stress target, dataset/baseline, scope, safety guard, expected outputs, and stop conditions.

## 2. P0 Closure Matrix

| P0 item | Original blocker | Files reviewed | Current implementation evidence | Tests covering it | Closure status | Remaining gap |
|---|---|---|---|---|---|---|
| Position monitor field alignment | Homepage did not consume `entryLogicStatus`, `directionSupportStatus`, `suggestedManualAction`, or `suggestedManualActionText`; real monitor logs could display as `等待监控`. | `DashboardHomeVO.java`, `DashboardHomeServiceImpl.java`, `dashboard.html`, `DashboardHomeServiceImplTest.java`, `DashboardControllerTest.java` | `PositionVO` exposes `entryLogicStatus`, `directionSupportStatus`, `suggestedManualAction`, `suggestedManualActionText`; service maps them from latest `PositionMonitorLogDTO`; template reads exact camel/snake fields; service filters active manual `OPEN` / `PARTIALLY_CLOSED` only. | `positionMonitorUsesRealMonitorFields`, `positionMonitorTemplateRendersRealMonitorFields`, `closedPositionNotDisplayedAsActiveMonitoring`, `executionSuggestionDoesNotBecomePosition` | `CLOSED` | None for P0. P1/P2 display copy can still be refined. |
| `floatingPnl` unit correction | Homepage rendered `floatingPnl` with percent formatting while backend `floatingPnl` is amount and `pnlPct` is percent. | `DashboardHomeVO.java`, `DashboardHomeServiceImpl.java`, `dashboard.html`, tests | Service computes `floatingPnl` amount and `pnlPct` percent separately; template uses `formatSignedAmount(p.floatingPnl)` and no longer uses `formatPct(p.floatingPnl)`. | `floatingPnlDisplayedAsAmountNotPercent`, existing long/short PnL tests | `CLOSED` | Percent display is not added to the homepage unless a separate real percent field is intentionally displayed later. |
| Adjudication consistency backend contract | Consistency card wanted score/summary, but backend only exposed `level`, `score`, `confused`; `score` was AI conflict score, not consistency score. | `DashboardHomeVO.java`, `DashboardHomeServiceImpl.java`, `dashboard.html`, tests | `ConsistencyVO` now exposes `consistencyScore`, `consistencyLevel`, `consistencySummary`, and `downgradeReason`; service leaves missing consistency fields `null`; UI reads explicit consistency fields and does not use conflict level as consistency fallback; `actualConsistencyScore()` only accepts explicit consistency/agreement score fields. | `adjudicationConsistencyHasExplicitBackendContract`, `adjudicationConsistencyDoesNotFakeScore`, `consistencyCardDoesNotFakeScore`, `consistencyCardShowsSemanticLabels`, `consistencyCardExplainsWaitingState` | `CLOSED` | A future real consistency resolver can populate the explicit fields; until then empty-state behavior is correct. |
| Sidebar/header health hardcoding | Sidebar showed healthy-looking labels such as `AI 服务 正常`, `数据源 正常`, `定时任务 正常` without backend support. | `dashboard.html`, `DashboardControllerTest.java` | `renderSidebarPanel()` reads `window.__lastHomeDiagnostics`, `positionProviderStatus`, and `window.__lastHealth`; fallbacks are `WAITING_SYNC` localized to waiting state rather than fake normal. | `sidebarHealthDoesNotHardcodeHealthy`, provider readiness service tests already covering header/diagnostics | `CLOSED` | Authenticated runtime smoke can later verify real provider status through browser/API during stress preparation. |

## 3. Field Traceability Recheck

| Frontend label | Template / JS location | VO field | Service mapping | Upstream source | Fallback behavior | Test coverage |
|---|---|---|---|---|---|---|
| `入场逻辑` | `renderHomePositionsFromPayload()` reads `entryLogicStatus` / `entry_logic_status`; legacy `renderHomePosition()` also reads them. | `PositionVO.entryLogicStatus` | `buildPositions()` sets latest monitor `logicStatus`, else `WAITING_MONITOR`. | `PositionMonitorLogDTO.logicStatus` from `PositionMonitorLogService.listByPositionId(positionId, 1)`. | UI fallback: `等待监控`. | `positionMonitorUsesRealMonitorFields`, `positionMonitorTemplateRendersRealMonitorFields` |
| `方向支持` | `renderHomePositionsFromPayload()` reads `directionSupportStatus` / `direction_support_status`; legacy path also reads them. | `PositionVO.directionSupportStatus` | `directionSupportStatus(latestMonitorLog)` maps `LOGIC_VALID` to `SUPPORTED`, `LOGIC_WEAKENED` to `WEAKENED`, `PLAN_INVALIDATED` to `NOT_SUPPORTED`, `HIGH_RISK` to `RISK_BLOCKED`, else `WAITING_SYNC`. | Latest monitor log logic status. | UI fallback: `等待监控`. | `positionMonitorUsesRealMonitorFields`, `positionMonitorTemplateRendersRealMonitorFields` |
| `当前建议` | `positionCurrentAdviceHtml()` prefers `suggestedManualActionText` / `suggested_manual_action_text`, then `suggestedManualAction` / `suggested_manual_action`. | `PositionVO.suggestedManualAction`, `PositionVO.suggestedManualActionText` | `buildPositions()` maps latest monitor `suggestedAction`; display text from `suggestedActionText(...)`. | `PositionMonitorLogDTO.suggestedAction`. | No monitor log maps to conservative `MANUAL_REVIEW` / `等待监控`. | `positionMonitorUsesRealMonitorFields`, `positionMonitorTemplateRendersRealMonitorFields` |
| `浮动盈亏` | `renderHomePositionsFromPayload()` uses `formatSignedAmount(p.floatingPnl)`; legacy path uses `formatSignedAmount(floatingPnlAmount)`. | `PositionVO.floatingPnl` | `applyPositionPnl()` computes amount from real entry/current price, side, and quantity. | Manual `UserPositionVO` plus latest monitor price or quote-client current price. | Blank/`—` if source price or quantity is missing; no fake amount/percent. | `floatingPnlDisplayedAsAmountNotPercent`; existing long/short PnL tests |
| Percent PnL | Not displayed in the repaired homepage row. | `PositionVO.pnlPct` | `applyPositionPnl()` computes true LONG/SHORT percent when entry/current price exist. | Manual position + current price. | Not rendered unless future UI explicitly uses this real field. | Existing PnL service tests |
| Active position source | `renderHomePositionsFromPayload()` filters local closed rows and closeable active statuses. | `positions[]` | `buildPositions()` uses `isActiveManualPosition()`: `sourceType=MANUAL` and status `OPEN` / `PARTIALLY_CLOSED`. | `UserPositionService.listOpenPositions()`. | Empty table: `暂无持仓` / `等待监控`. | `closedPositionNotDisplayedAsActiveMonitoring`, `executionSuggestionDoesNotBecomePosition` |
| Consistency score | `actualConsistencyScore()` reads only `consistencyScore`, `agreementScore`, snake-case equivalents. | `ConsistencyVO.consistencyScore` | Set to `null` unless a real consistency source exists. | No real resolver source yet. | UI score circle shows `--`; no fake formula. | `adjudicationConsistencyDoesNotFakeScore`, `consistencyCardDoesNotFakeScore` |
| Consistency level | `renderHomeAiDecisionFromPayload()` passes `c.consistencyLevel` / `c.consistency_level` to `renderHomeConsistencyCard()`. | `ConsistencyVO.consistencyLevel` | Set to `null` unless a real consistency source exists. | No real resolver source yet. | UI status pill: `等待同步`. | `adjudicationConsistencyHasExplicitBackendContract`, `consistencyCardExplainsWaitingState` |
| Consistency summary | `renderHomeAiDecisionFromPayload()` reads `c.consistencySummary` / `c.consistency_summary`. | `ConsistencyVO.consistencySummary` | Set to `null` unless a real consistency source exists. | No real resolver source yet. | UI summary: `等待 AI 三角色结果同步后生成一致性结论`. | `adjudicationConsistencyHasExplicitBackendContract`, `consistencyCardExplainsWaitingState` |
| Downgrade reason | Consistency card reads `c.downgradeReason` first, then GPT final role downgrade reason. | `ConsistencyVO.downgradeReason` | Copied from real parsed `GPT_FINAL` role downgrade/block reason if present. | `DecisionResultVO.aiRoleResults` parsed GPT final role. | UI fallback: `暂无降级原因`. | `adjudicationConsistencyHasExplicitBackendContract`, `adjudicationConsistencyDoesNotFakeScore` |
| Sidebar `AI 服务` | `renderSidebarPanel()` uses `diagnostics.aiProvider` / `diagnostics.ai_provider`. | `DiagnosticsVO.aiProvider` through `window.__lastHomeDiagnostics`. | `buildDiagnostics()` maps provider readiness. | `ProviderReadinessService.getReadiness()`. | `WAITING_SYNC` / localized waiting state. | `sidebarHealthDoesNotHardcodeHealthy`, provider readiness tests |
| Sidebar `数据源健康` | `renderSidebarPanel()` uses `positionProviderStatus.text` or diagnostics provider fields. | `DiagnosticsVO.marketDataProvider` and position provider runtime status. | Provider readiness + position sync runtime frontend status. | Backend readiness read model and existing position provider status. | `WAITING_SYNC` / localized waiting state. | `sidebarHealthDoesNotHardcodeHealthy` |

## 4. Forbidden Semantic Regression Check

| Regression guard | Closure result | Evidence |
|---|---|---|
| System suggestion is not a real position | `PASS` | `executionSuggestionDoesNotBecomePosition` asserts a real execution suggestion does not create `positions[]`. |
| Triggered plan is not an opened position | `PASS` | Dashboard position source is `UserPositionService.listOpenPositions()` plus manual active status; no ExecutionPlan source is used. |
| CLOSED position excluded | `PASS` | `isActiveManualPosition()` requires `OPEN` / `PARTIALLY_CLOSED`; `closedPositionNotDisplayedAsActiveMonitoring` covers service and push mode. |
| AI roles remain role-specific | `PASS` | Existing role-specific tests cover GPT_FINAL, GEMINI_REVIEW, and GROK_CHALLENGE content separation. |
| `裁决一致性` is not a fourth AI role | `PASS` | Consistency is rendered as synthesis card under `aiDecision.consistency`; role tabs remain the three defined roles only. |
| Push Recheck is not trade authorization | `PASS` | No Push/Recheck code changed; existing no-trading guard remains. |
| No fake AI evidence | `PASS` | Consistency score/summary stay empty without real backend fields; malformed/raw AI role data tests preserve empty evidence. |
| No fake position | `PASS` | Homepage position rows come from manual active `UserPositionVO` only. |
| No auto-open / auto-close / auto-reverse / order execution / auto-trading | `PASS` | No trading/order/execution behavior changed; template tests retain forbidden wording guards. |
| No external push send | `PASS` | No push dispatch/send logic changed. |

## 5. Stress Test Gate Recommendation

P0 alignment blockers are closed, so the next package can be:

`P1 Dashboard Stress Test Plan & Harness Preparation`

Recommended P1 preparation scope:

1. Define dashboard stress-test target routes and APIs.
2. Define local-only dataset/baseline fixtures and expected screenshots/API snapshots.
3. Define safety guard: no provider calls, no production DB/server, no external push send, no trading/order/auto behavior.
4. Define pass/fail checklist for homepage, Review Center, Position Monitor, ExecutionSuggestion, AI role panels, consistency card, sidebar health, and no-fake-data states.
5. Define rollback/stop conditions and evidence artifact locations.

`STRESS_TEST_EXECUTION_ALLOWED` remains `NO` until that plan/harness package is complete and explicitly approved.

Production readiness remains `BLOCKED`; this closure review is only about P0 dashboard backend/frontend semantic alignment, not release readiness.

## 6. P1 Preparation Handoff

P1 Dashboard Stress Test Plan & Harness Preparation is authorized as the next package after this P0 closure. The P1 package may define local-only targets, guarded dry-run behavior, metrics, stop conditions, and evidence templates. It must not execute stress traffic in the preparation PR.

Required P1 handoff state:

- `STRESS_TEST_PREPARATION_ALLOWED: YES`
- `STRESS_TEST_EXECUTION_ALLOWED: NO`
- local-only default target: `http://localhost:8081`
- allowed endpoints only: `GET /actuator/health`, `GET /dashboard`, `GET /api/dashboard/home`
- no production server / DB access
- no provider calls
- no write endpoints
- no Push/Recheck/Telegram send
- no order execution / auto-trading behavior

Stress-test execution remains a separate future package requiring explicit approval.
