# V1 Minimal Review-Only Alert Fatigue / Notification Policy Status Runtime Wiring Design

## 1. Executive Summary

- Current merged main: `14e0e07 docs(alerts): read notification policy status source path (#937)`
- Current module: `Alert fatigue / notification policy status`
- Current phase: `Design`
- Design result: `GO` to `Implementation readiness gate for Alert fatigue / notification policy status`
- Capability movement: none. Current level remains `REVIEW_ONLY_RUNTIME partial`.

This design keeps the future slice strictly read-only. It should expose whether alert-fatigue and notification-policy state is visible, stale, missing, suppressed, or cooldown-bound, but it must never send a notification, trigger a recheck, run a scheduler, refresh an API client, generate a candidate, generate a decision, generate a point, or produce a trading instruction.

Recommended owner path:

```text
DashboardController
  -> existing MonitorService.getRecentAlerts(limit)
  -> existing MonitorAlertMapper read methods
  -> existing tm_monitor_alert read model
  -> minimal dashboard status panel / read-only status endpoint
```

`MonitorAlertWriteServiceImpl` remains write-side only. It may inform the design semantics for cooldown and suppression, but future runtime status wiring must not call it.

## 2. Existing Assets Used By Design

| Asset | Reuse decision | Notes |
|---|---|---|
| `MonitorAlertDO` | Reuse as read model | Carries alert status, cooldown, suppression reason, trace, rule version, and timestamps. |
| `MonitorAlertMapper` | Reuse read methods only | `selectRecent`, status-count, type-count, throttle-window, and semantic-window queries are useful. `insert` is forbidden for this slice. |
| `MonitorService` / `MonitorServiceImpl` | Reuse read path | `getRecentAlerts(limit)` is already used by dashboard. |
| `MonitorController` | Not sufficient alone | `/api/monitor/status` is generic and does not expose policy/fatigue status. |
| `MonitorAlertWriteServiceImpl` | Do not invoke | Existing write-side cooldown/suppression behavior is source evidence only. |
| `DashboardController` | Future owner path candidate | Already adds alert data to dashboard and `/api/dashboard/summary`. |
| `dashboard.html` | Future minimal panel location | Existing alert center is recent-alert display; a small policy/status panel is clearer. |
| `alert-explain.js` / `review-page.js` | Reuse wording model | Already explains status, cooldown, suppression, and why-copy. |
| `RunBaselineServiceImpl` / `RunBaselineVO` | Reuse as count precedent | Useful for open/suppressed count semantics; not required as owner. |
| `UserConfigDO` / `UserConfigMapper` | Policy source evidence only | `notify_channels` and `cooldown_minutes` exist, but no send policy status is wired. |
| `tm_monitor_alert` | Reuse as persisted state | Existing stored alert rows are enough for a read-only status design. |

## 3. Endpoint / Owner Path Decision

Future implementation should add one dedicated, minimal read-only dashboard status endpoint rather than overloading `/api/dashboard/summary` or `/api/monitor/status`.

Recommended endpoint candidate:

```text
GET /api/dashboard/alert-fatigue-policy-status?symbol=BTCUSDT
```

Why dedicated:

- `/api/dashboard/summary` already carries recent alerts, but not explicit policy completeness, fatigue status, not-push boundary, not-recheck boundary, or fail-closed reason.
- `/api/monitor/status` is too generic and cannot safely express the V1 safety boundary.
- A dedicated endpoint can return a small `Map` and avoid new DTO / Validator / Assembler.
- It can fail closed when the alert read model or policy evidence is absent.

The endpoint must be read-only:

- It may read stored alerts and counts.
- It may summarize stored cooldown / suppression evidence.
- It must not call `MonitorAlertWriteServiceImpl`.
- It must not call Push, external channel, recheck, scheduler, collector, or API-client refresh paths.

## 4. Read-Only Source Fields

| Source | Field / signal | Design use |
|---|---|---|
| Recent alerts | `MonitorService.getRecentAlerts(limit)` | List presence, newest status, recent alert count. |
| Stored alert rows | `status`, `alertType`, `assetSymbol`, `createdAt`, `updatedAt` | Open/suppressed counts and fatigue trend. |
| Cooldown evidence | `cooldownUntil` | `cooldownActive` when a future timestamp is present. |
| Suppression evidence | `suppressReason`, `status=SUPPRESSED` | `suppressionActive`, dedupe / throttle explanation. |
| Mapper count queries | `countByStatusInWindow`, `countByStatusAndTypeInWindow` | Open/suppressed/fatigue counts without writing. |
| Throttle / semantic window queries | `countOpenInThrottleWindow`, `countAnyInSemanticWindow` | Duplicate-risk evidence if future implementation needs it. |
| User config | `notifyChannels`, `cooldownMinutes` | Policy-source evidence only; no send/dispatch interpretation. |
| Alert explanation JS | status/cooldown/suppress copy | Wording precedent for dashboard status copy. |
| Run baseline summary | open/suppressed ratios | Reference only; not required for first implementation. |

## 5. Status Mapping

| Status | Meaning | Source | Fail-closed? |
|---|---|---|---:|
| `ALERT_POLICY_REVIEW_ONLY_READY` | Read-side alert data and policy boundary are visible enough to show status. | Recent alerts / mapper counts / optional config evidence. | No |
| `ALERT_POLICY_BACKEND_PENDING_FAIL_CLOSED` | Future endpoint/panel backend is unavailable or cannot compute status. | Controller/read path exception or unavailable service. | Yes |
| `ALERT_READ_MODEL_MISSING_FAIL_CLOSED` | `MonitorAlert` read model or required read path is absent. | Missing mapper/service data. | Yes |
| `ALERT_RECENT_EMPTY_REVIEW_ONLY` | No recent alerts are present. This is an empty read-only state, not proof of safety. | `getRecentAlerts` empty list. | No |
| `ALERT_SUPPRESSION_ACTIVE_REVIEW_ONLY` | Suppressed alerts exist in the observed window. | `status=SUPPRESSED`, `suppressReason`. | No |
| `ALERT_COOLDOWN_ACTIVE_REVIEW_ONLY` | Cooldown evidence is active or recently observed. | `cooldownUntil`, throttle-window evidence. | No |
| `ALERT_DUPLICATE_RISK_REVIEW_ONLY` | Duplicate or semantically similar alerts are visible in stored rows. | throttle / semantic window counts. | No |
| `ALERT_FATIGUE_HIGH_REVIEW_ONLY` | Alert volume or suppression ratio is high enough to require manual attention. | recent/open/suppressed counts. | No |
| `NOTIFICATION_POLICY_MISSING_FAIL_CLOSED` | Notification policy evidence is required by the status but missing or ambiguous. | `UserConfigDO` / policy evidence unavailable. | Yes |
| `PUSH_BOUNDARY_BLOCKED_FAIL_CLOSED` | Any status path attempts to represent notification send or dispatch state. | Guardrail condition. | Yes |
| `RECHECK_BOUNDARY_BLOCKED_FAIL_CLOSED` | Any status path attempts to trigger or imply recheck execution. | Guardrail condition. | Yes |

Implementation readiness must decide conservative thresholds for `ALERT_DUPLICATE_RISK_REVIEW_ONLY` and `ALERT_FATIGUE_HIGH_REVIEW_ONLY`. Thresholds must remain display-only.

## 6. Safety Fields

Future endpoint/panel must expose or display these safety boundaries:

| Field | Required value | Reason |
|---|---:|---|
| `reviewOnly` | `true` | Status is review-only. |
| `notPushSend` | `true` | It is not notification sending. |
| `notExternalChannel` | `true` | It does not call Telegram/email/webhook/app channels. |
| `notRecheckExecution` | `true` | It does not trigger recheck. |
| `notSchedulerTrigger` | `true` | It does not run scheduler logic. |
| `notCollectorTrigger` | `true` | It does not run collectors. |
| `notApiClientRefresh` | `true` | It does not refresh market/API clients. |
| `notTradingSignal` | `true` | It is not a trade signal. |
| `notCandidateSignal` | `true` | It is not a candidate signal. |
| `notDecisionGeneration` | `true` | It does not create a decision. |
| `notPointSignal` | `true` | It is not point generation. |
| `notExecutable` | `true` | It cannot be executed. |
| `displaySlotsAreCandidatePool` | `false` | Alert display is not a candidate pool. |

Suggested supporting fields:

- `status`
- `symbol`
- `recentAlertCount`
- `openAlertCount`
- `suppressedAlertCount`
- `cooldownActive`
- `suppressionActive`
- `duplicateRiskVisible`
- `policySource`
- `sourceHealth`
- `reason`
- `message`
- `failClosed`

No response should include send state, dispatch action, recheck action, order action, candidate ranking, final direction, entry, stop, TP, RR, position size, or leverage.

## 7. Dashboard Design

Future implementation should add a minimal status panel instead of changing dashboard alert-center behavior.

Recommended DOM candidates:

- `alertFatiguePolicyStatusPanel`
- `alertFatiguePolicyRuntimeStatusValue`
- `alertFatiguePolicyCountsValue`
- `alertFatiguePolicyCooldownValue`
- `alertFatiguePolicySuppressionValue`
- `alertFatiguePolicyBoundaryValue`

Dashboard copy must say:

- alert fatigue / notification policy status is review-only;
- it is not Push send;
- it is not external channel dispatch;
- it is not recheck execution;
- it is not scheduler / collector / API-client refresh;
- it is not a trading signal, candidate signal, decision generation, point signal, or executable action;
- fail-closed applies when read model, policy source, or boundary is ambiguous.

The existing `sidebarAlertList` should remain the recent-alert center. It can be referenced as source evidence, but it should not become the status panel itself because it lacks fixed safety labels.

## 8. Fail-Closed Rules

The future implementation must fail closed when:

- alert read model is missing or unavailable;
- mapper/service read path throws or returns ambiguous data;
- policy source is required but missing;
- the status would need to call `MonitorAlertWriteServiceImpl`;
- the status would need to call Push / external channel services;
- the status would need to call recheck execution;
- the status would need to trigger scheduler / collector / API-client refresh;
- cooldown / suppression data is contradictory or cannot be classified;
- any output starts to look like candidate generation, decision generation, point generation, or trading action.

`ALERT_RECENT_EMPTY_REVIEW_ONLY` is not fail-closed by itself. It is simply an empty read-only state and must not be described as market safety or no-risk proof.

## 9. Push / Recheck / Refresh Boundary

Push and recheck remain outside this slice.

Forbidden future calls:

- `MonitorAlertWriteServiceImpl.emitAfterAnalysisPersist(...)`
- `MonitorAlertWriteServiceImpl.tryEmitOpenOrSuppressed(...)`
- Push send / external channel dispatch services
- `PushRecheckController` mutation endpoints
- `PushRecheckService#recheck`
- `PushRecheckService#replayByDispatch`
- scheduler entrypoints
- collector entrypoints
- market/API client refresh entrypoints

Allowed future reads:

- stored alert rows from `tm_monitor_alert`;
- read-only recent alert list;
- read-only alert counts;
- read-only user config policy evidence, if already available and not used to send.

## 10. Future Implementation Boundary

If readiness remains `GO`, future implementation may touch only:

- `src/main/java/org/example/trademodel/controller/DashboardController.java`
- `src/main/resources/templates/dashboard.html`
- `src/test/java/org/example/trademodel/controller/DashboardControllerTest.java`
- optional tiny read-only service glue only if the readiness gate proves `DashboardController` cannot safely reuse current reads directly;
- implementation docs;
- source-of-truth docs.

Future implementation must not touch:

- schema / config / pom;
- DTO / Validator / Assembler / Orchestrator;
- `MonitorAlertWriteServiceImpl`;
- Push / recheck / scheduler / collector / API-client refresh paths;
- Candidate / Decision generation / Point / Trading paths.

## 11. Readiness Gate Checklist

The next readiness gate must confirm:

- whether `DashboardController` can build the status from existing read paths without service ownership expansion;
- whether a minimal `Map` response is enough;
- whether `MonitorAlertMapper` read methods are safe to call directly or through `MonitorService`;
- whether user-config policy evidence is required in the first implementation;
- whether missing policy evidence should map to `NOTIFICATION_POLICY_MISSING_FAIL_CLOSED`;
- exact thresholds for fatigue / duplicate-risk display, if used;
- exact dashboard DOM ids and copy;
- targeted test scope;
- no DTO / Validator / Assembler / Orchestrator;
- no schema / config / pom;
- no write-side alert generation;
- no Push send, external channel, recheck execution, scheduler, collector, or API-client refresh.

## 12. Next Task

- Next allowed action: `Implementation readiness gate for Alert fatigue / notification policy status`
- Next branch: `alert-fatigue-notification-policy-status-implementation-readiness-gate`
- PR risk: `A`
- Allowed changes: readiness-gate docs and source-of-truth updates only.

## 13. Freeze Rule Compliance

- Whether this creates a new skeleton: No.
- Whether it reuses Cursor-era / V1 assets: Yes, it anchors on `MonitorAlert` / dashboard alert owner assets.
- Whether it reduces duplication: Yes, it keeps the future status on existing monitor-alert ownership instead of inventing a notification-policy owner family.
- Whether it raises capability level: No, design only.
- Whether it wires service/runtime/dashboard/API: No implementation; it designs a future minimal review-only endpoint/panel.
- Whether it follows #830 audit guidance: Yes.
