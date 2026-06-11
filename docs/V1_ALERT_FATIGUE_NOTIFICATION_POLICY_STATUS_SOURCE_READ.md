# V1 Alert Fatigue / Notification Policy Status Source Read

## 1. Executive Summary

- Current merged main: `cf4f2f1 docs(runtime): select next slice after risk action guard closure (#936)`
- Current module: `Alert fatigue / notification policy status`
- Current phase: `Source Read`
- Readiness result: `GO` to `Minimal Review-Only Alert Fatigue / Notification Policy Status Runtime Wiring Design`
- Capability movement: none. Current level remains `REVIEW_ONLY_RUNTIME partial`.

Source read confirms an existing monitor-alert owner path is present and reusable for a future minimal read-only status:

- `MonitorAlertDO` carries alert status, cooldown, suppression reason, trace, rule version, and freshness fields.
- `MonitorAlertMapper` has read methods for recent alerts, per-analysis alerts, open/suppressed counts, throttle window counts, and semantic suppression window counts.
- `MonitorService.getRecentAlerts(int limit)` is an existing read-only service path.
- `DashboardController` already adds recent alerts to the dashboard model and `/api/dashboard/summary`.
- `dashboard.html` already renders a sidebar alert center through `sidebarAlertList`.
- `review.html` / `review-page.js` / `alert-explain.js` already explain alert type, status, cooldown, and suppression state.
- `RunBaselineServiceImpl` already derives alert open/suppressed counts and suppression ratios for a runtime baseline summary.

The write path also exists, but must remain excluded from the future status slice:

- `MonitorAlertWriteServiceImpl.emitAfterAnalysisPersist(...)` writes alerts after analysis persistence.
- `tryEmitOpenOrSuppressed(...)` performs dedupe, 15-minute cooldown throttling, and 45-minute semantic suppression writes.
- Future status wiring must not call this write path, must not trigger notification sending, and must not trigger recheck, scheduler, collector, external API client, Push, Candidate, Decision generation, Point, or Trading behavior.

## 2. Source Read Files

| Area | Files read | Source-read finding |
|---|---|---|
| Alert entity | `src/main/java/org/example/trademodel/entity/MonitorAlertDO.java` | Existing carrier includes `analysisId`, `assetSymbol`, `alertType`, `alertLevel`, `alertMessage`, `status`, `cooldownUntil`, `suppressReason`, `traceId`, `ruleVersion`, `createdAt`, and `updatedAt`. |
| Alert mapper | `src/main/java/org/example/trademodel/mapper/MonitorAlertMapper.java` | Existing reads include `selectRecent`, `listByAnalysisId`, `countByStatusInWindow`, `countByStatusAndTypeInWindow`, `countOpenInThrottleWindow`, and `countAnyInSemanticWindow`; insert exists but is write-side only. |
| Read service | `src/main/java/org/example/trademodel/service/MonitorService.java`, `src/main/java/org/example/trademodel/service/impl/MonitorServiceImpl.java` | `getRecentAlerts(limit)` is read-only and uses `MonitorAlertMapper.selectRecent(limit)`; suitable as a minimal owner path candidate. |
| Thin monitor controller | `src/main/java/org/example/trademodel/controller/MonitorController.java` | Existing `/api/monitor/status` only returns `monitoring: symbol`; it does not expose alert fatigue or notification policy status today. |
| Write service | `src/main/java/org/example/trademodel/service/MonitorAlertWriteService.java`, `src/main/java/org/example/trademodel/service/impl/MonitorAlertWriteServiceImpl.java` | Existing write-side alert generation, dedupe, cooldown, and suppression logic. Future status must read its stored effects only; it must not invoke the write path. |
| Dashboard owner path | `src/main/java/org/example/trademodel/controller/DashboardController.java`, `src/main/resources/templates/dashboard.html`, `src/main/java/org/example/trademodel/vo/DashboardSummaryResponseVO.java` | Dashboard already receives `alerts` and renders the sidebar alert center. A dedicated status panel/API is still missing. |
| Review alert explanation | `src/main/resources/templates/review.html`, `src/main/resources/static/js/review-page.js`, `src/main/resources/static/js/alert-explain.js` | Review page already uses `AlertExplain` to show alert type, status, cooldown, suppression reason, and why-copy. Reusable for wording and safety model. |
| Runtime baseline counts | `src/main/java/org/example/trademodel/service/impl/RunBaselineServiceImpl.java`, `src/main/java/org/example/trademodel/vo/RunBaselineVO.java` | Existing baseline summary can count `OPEN` and `SUPPRESSED` alerts and suppression ratios in a time window. Useful reference, but broader than a minimal dashboard status. |
| Notification policy carrier | `src/main/java/org/example/trademodel/entity/UserConfigDO.java`, `src/main/java/org/example/trademodel/mapper/UserConfigMapper.java`, `src/main/resources/schema.sql` | `notify_channels` and `cooldown_minutes` exist in user config/schema. No dedicated notification-policy status owner is wired into dashboard/API today. |
| Tests | `src/test/java/org/example/trademodel/service/impl/MonitorAlertWriteServiceImplTest.java`, `src/test/java/org/example/trademodel/controller/DashboardControllerTest.java` | Tests cover write-side cooldown/suppression behavior and dashboard summary alerts. A future status slice needs targeted read-only endpoint/panel tests. |
| Adjacent risk paths | `src/main/java/org/example/trademodel/controller/PushRecheckController.java`, Push recheck services/docs | Push/recheck exists as a separate path. Future alert fatigue status must not call or imply Push send, external channel dispatch, or recheck execution. |

## 3. Existing Runtime Flow

```text
Analysis persistence
  -> MonitorAlertWriteServiceImpl.emitAfterAnalysisPersist(...)
     -> MonitorAlertMapper.insert(...)
     -> tm_monitor_alert stores OPEN / SUPPRESSED / cooldown_until / suppress_reason
        [exists, write-side, not allowed for future status invocation]

Dashboard summary
  -> DashboardController dashboard model / /api/dashboard/summary
  -> MonitorService.getRecentAlerts(3)
  -> MonitorAlertMapper.selectRecent(limit)
  -> dashboard.html sidebarAlertList
        [exists, runtime read-only list, dashboard visible, partial status only]

Review page
  -> ReviewAggregateServiceImpl listByAnalysisId(...)
  -> review-page.js renderAlerts(...)
  -> alert-explain.js explains status/cooldown/suppress
        [exists, per-analysis read-only explanation, review page visible]

Run baseline
  -> RunBaselineServiceImpl.buildAlertSummary(...)
  -> MonitorAlertMapper countByStatusInWindow / countByStatusAndTypeInWindow
        [exists, runtime counts, not dedicated dashboard status]

Push / recheck
  -> PushRecheckController / services
        [exists, separate path, forbidden for this slice]
```

## 4. Reusable Assets

- `tm_monitor_alert` already stores status, cooldown, suppression, trace, rule version, and timestamps.
- `MonitorAlertDO` is sufficient as a current read model for source-read/design; a future implementation can return a minimal `Map` rather than adding DTOs.
- `MonitorAlertMapper` can read recent alerts and aggregate open/suppressed counts.
- `MonitorService.getRecentAlerts` is already wired into `DashboardController`.
- `DashboardSummaryResponseVO.alerts` and `dashboard.html` `sidebarAlertList` already expose recent alerts to the dashboard.
- `alert-explain.js` already maps alert type, status, cooldown, and suppression reason into user-facing explanations.
- `RunBaselineVO.AlertSummary` already shows how to compute open/suppressed counts and suppression ratios.
- `UserConfigDO.cooldownMinutes` and `UserConfigDO.notifyChannels` are inventory evidence for notification policy fields, but they are not enough by themselves to prove a dashboard status owner.
- `MonitorAlertWriteServiceImpl.DEFAULT_ALERT_COOLDOWN_MINUTES = 15` and `DEFAULT_SEMANTIC_SUPPRESS_WINDOW_MINUTES = 45` establish current cooldown/suppression semantics.

## 5. Gaps

- No dedicated `Alert fatigue / notification policy status` endpoint exists.
- No dedicated dashboard panel or DOM ids exist for notification policy status, cooldown health, suppression state, or not-push boundary.
- `MonitorController` only returns a generic monitoring string.
- `dashboard.html` alert center currently shows only recent alert level/message; it does not show cooldown/suppression policy completeness or fail-closed status.
- `UserConfigDO.notifyChannels` and `cooldownMinutes` exist, but no read-only status service/controller currently summarizes whether notification policy is configured, unknown, disabled, or fail-closed.
- There is no explicit `reviewOnly`, `notPushSend`, `notRecheckExecution`, `notTradingSignal`, `notCandidateSignal`, `notDecisionGeneration`, `notPointSignal`, or `notExecutable` alert status response today.
- There is no targeted dashboard/controller test for an alert fatigue / notification policy status panel.
- `MonitorAlertWriteServiceImpl` is a tempting owner path but is write-side; future design must keep it as source semantics only, not an invocation path.

## 6. Alert Fatigue / Notification Semantics Found

- Dedupe by `analysisId + alertType` via `countByAnalysisIdAndAlertType`.
- DB throttle by `assetSymbol + alertType` within `DEFAULT_ALERT_COOLDOWN_MINUTES = 15`.
- Semantic suppression by `assetSymbol + alertType` within `DEFAULT_SEMANTIC_SUPPRESS_WINDOW_MINUTES = 45`.
- `OPEN` alerts can carry `cooldownUntil`.
- `SUPPRESSED` alerts can carry `suppressReason`, including `THROTTLE_DB` and `SEMANTIC_SIMILAR_RECENT`.
- Review page explanation renders “已触发”, “已抑制”, cooldown detail, and suppression detail without changing stored truth.
- User config/schema carry `notify_channels` and `cooldown_minutes`, but the current alert write service uses hardcoded constants and does not expose a policy status surface.

## 7. Boundary Risks

| Risk | Source-read evidence | Required boundary |
|---|---|---|
| Push send / external channel confusion | `notify_channels`, Push/recheck docs/controllers, alert wording | Future status must say `notPushSend=true` and `notExternalChannel=true`; no send/dispatch state. |
| Recheck execution confusion | `PushRecheckController` exists separately | Future status must say `notRecheckExecution=true` and must not call recheck services. |
| Scheduler / collector / API client refresh | Alert status could be misread as live refresh | Future status must be read-only from stored rows and existing reads; no refresh trigger. |
| Write-side invocation | `MonitorAlertWriteServiceImpl` writes `OPEN` / `SUPPRESSED` rows | Future endpoint/panel must not call `emitAfterAnalysisPersist` or `tryEmitOpenOrSuppressed`. |
| Trading semantics | Alert messages can mention high risk, blocked open, data quality | Future copy must mark not trading, not candidate, not decision generation, not point, and not executable. |
| DTO/schema drift | `MonitorAlertDO`, `DashboardSummaryResponseVO`, `RunBaselineVO`, user config already exist | Do not add DTO / Validator / Assembler / schema/config/pom by default. |

## 8. Design Direction

Recommended next step: `Minimal Review-Only Alert Fatigue / Notification Policy Status Runtime Wiring Design`.

Design should evaluate a thin read-only dashboard/API status over the existing monitor alert owner path:

- Owner path candidate: `DashboardController` + `MonitorService` + `MonitorAlertMapper` + existing dashboard alert assets.
- Optional API candidate: one minimal read-only dashboard status endpoint if existing summary is insufficient.
- Dashboard candidate: a minimal status panel or sidebar enhancement showing review-only policy visibility, recent alert counts, open/suppressed counts, cooldown/suppression health, and fail-closed status.
- Response shape: use `Map` or existing objects; do not add DTO / Validator / Assembler unless a later readiness gate proves it is unavoidable.
- Status-only fields should include review-only, not-push, not-external-channel, not-recheck-execution, not-refresh-trigger, not-trading, not-candidate, not-decision-generation, not-point, not-executable, fail-closed, sourceHealth, reason, and message.

Suggested status mapping for design consideration only:

- `ALERT_POLICY_REVIEW_ONLY_READY`
- `ALERT_POLICY_SOURCE_PARTIAL`
- `ALERT_POLICY_MISSING_FAIL_CLOSED`
- `ALERT_COOLDOWN_ACTIVE_REVIEW_ONLY`
- `ALERT_SUPPRESSION_ACTIVE_REVIEW_ONLY`
- `ALERT_STATUS_EMPTY_REVIEW_ONLY`
- `ALERT_STATUS_UNKNOWN_FAIL_CLOSED`
- `ALERT_PUSH_RECHECK_BOUNDARY_BLOCKED_FAIL_CLOSED`

## 9. Go / No-Go

Result: `GO` to design only.

Why GO:

- Existing read-side owner assets are present.
- Dashboard and review page already consume alert records.
- Cooldown/suppression/dedupe semantics are already persisted/readable.
- A minimal status can be designed without schema/config/pom or DTO/Validator/Assembler.
- The future slice can remain review-only and fail-closed.

Why not implementation yet:

- Dedicated status mapping, endpoint/panel decision, fail-closed rules, and exact safety fields still need design.
- Push/recheck/external channel boundaries must be explicitly designed before any code change.
- Notification policy fields exist but are not a complete runtime status owner today.

## 10. Next Task

- Next allowed action: `Minimal Review-Only Alert Fatigue / Notification Policy Status Runtime Wiring Design`
- Next branch: `minimal-review-only-alert-fatigue-notification-policy-status-runtime-wiring-design`
- PR risk: `A`
- Allowed changes: design docs and source-of-truth updates only.
- Forbidden changes: Java business code, tests, dashboard business logic, schema/config/pom, endpoint/panel implementation, external API refresh, scheduler/collector/API client trigger, Push send, external channel, recheck execution, Candidate generation, Decision generation, Point, final direction, entry/stop/TP/RR, order/execution, auto-trading, DTO/Validator/Assembler/Orchestrator, Position Monitor execution, replay/recheck, P359/P360.

## 11. Freeze Rule Compliance

- 是否创建新骨架: No
- 是否复用 Cursor-era / V1 资产: Yes
- 是否减少重复: Yes, by selecting the existing MonitorAlert / dashboard alert owner path instead of inventing a new notification-policy owner.
- 是否提升 capability level: No, source read only.
- 是否接 service/runtime/dashboard/API: No implementation; source read only.
- 是否符合 #830 审计建议: Yes.
