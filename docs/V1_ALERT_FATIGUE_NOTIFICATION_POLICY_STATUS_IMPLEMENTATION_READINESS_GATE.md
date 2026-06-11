# V1 Alert Fatigue / Notification Policy Status Implementation Readiness Gate

## 1. Executive Summary

- Current merged main: `36da811 docs(alerts): design notification policy status wiring (#938)`
- Current module: `Alert fatigue / notification policy status`
- Current phase: `Implementation Readiness Gate`
- Readiness decision: `GO` to `Minimal Review-Only Alert Fatigue / Notification Policy Status Runtime Wiring Implementation`
- Next implementation risk: `B`
- Capability movement: none. Current level remains `REVIEW_ONLY_RUNTIME partial`.

The next implementation is allowed because the existing MonitorAlert read path is enough for a minimal read-only status surface. The implementation can reuse stored `tm_monitor_alert` rows through existing read assets, expose a small dashboard status endpoint, and add a minimal dashboard panel without calling write-side alert logic, Push send, external channel dispatch, recheck execution, scheduler, collector, API-client refresh, Candidate generation, Decision generation, Point generation, or Trading behavior.

The implementation must stay display-only. If the status would need notification sending, alert writing, recheck execution, refresh, schema/config/pom changes, or a new DTO / Validator / Assembler / Orchestrator, the package becomes `NO-GO`.

## 2. Readiness Decision

Result: `GO`.

Allowed next action:

```text
Minimal Review-Only Alert Fatigue / Notification Policy Status Runtime Wiring Implementation
```

Why GO:

- `MonitorAlertDO` already carries alert status, cooldown, suppression reason, trace, rule version, timestamps, symbol, type, and level.
- `MonitorAlertMapper` already provides read methods for recent alerts and counts.
- `MonitorService.getRecentAlerts(limit)` is already wired into `DashboardController` and dashboard summary.
- `dashboard.html` already has an alert center through `sidebarAlertList`.
- The source-read and design packages already separate read-side visibility from `MonitorAlertWriteServiceImpl`.
- A minimal `Map` endpoint avoids new DTO / Validator / Assembler ownership.
- Fail-closed states can be represented without writing alert rows or triggering any external side effects.

Why this is still not implementation:

- This package does not add the endpoint.
- This package does not add dashboard DOM.
- This package does not add tests.
- This package does not change Java, templates, schema, config, or pom.

## 3. Existing Owner Path Readiness

Recommended future owner path:

```text
DashboardController
  -> existing MonitorService.getRecentAlerts(limit)
  -> existing MonitorAlertMapper read methods
  -> existing tm_monitor_alert read model
  -> minimal read-only status endpoint
  -> minimal dashboard status panel
```

Readiness notes:

- `DashboardController` is the correct dashboard/API owner for the status surface.
- `MonitorService.getRecentAlerts(limit)` is the safest existing read path for first implementation.
- `MonitorAlertMapper.selectRecent`, `countByStatusInWindow`, `countByStatusAndTypeInWindow`, `countOpenInThrottleWindow`, and `countAnyInSemanticWindow` are read evidence, but implementation should prefer the smallest needed read surface.
- `MonitorAlertWriteServiceImpl` is write-side only. It may explain cooldown/suppression semantics in docs, but future status implementation must not call it.
- `UserConfigDO` / `UserConfigMapper` can be treated as policy evidence only if already reachable without ownership expansion. Missing policy evidence must fail closed rather than force a new config/schema path.

## 4. Allowed Implementation Files

If this package is merged, the next B-risk implementation may touch only:

| File | Allowed use |
|---|---|
| `src/main/java/org/example/trademodel/controller/DashboardController.java` | Add one minimal read-only `Map` endpoint: `GET /api/dashboard/alert-fatigue-policy-status?symbol=BTCUSDT`. It may read existing MonitorAlert read paths only. |
| `src/main/resources/templates/dashboard.html` | Add one minimal status panel, DOM ids, review-only / fail-closed / not-push / not-recheck / not-refresh safety copy. |
| `src/test/java/org/example/trademodel/controller/DashboardControllerTest.java` | Add targeted endpoint/dashboard tests for safety fields, fail-closed states, Push/recheck boundaries, and forbidden executable fields absent. |
| `docs/V1_ALERT_FATIGUE_NOTIFICATION_POLICY_STATUS_RUNTIME_WIRING_IMPLEMENTATION.md` | Record implementation scope, reused assets, safety fields, tests, and no-overreach result. |
| Source-of-truth docs | Update handoff only after implementation facts are known. |

No other Java ownership path is allowed by default. If implementation cannot stay inside these files, it must stop and return to design/readiness correction.

## 5. Forbidden Implementation Files

Future implementation must not touch:

- `src/main/java/org/example/trademodel/service/impl/MonitorAlertWriteServiceImpl.java`
- `src/main/java/org/example/trademodel/service/MonitorAlertWriteService.java`
- Push / external channel services or controllers
- Push recheck services, scheduler, dispatch config, or mutation controllers
- scheduler / collector / API client refresh paths
- schema files
- config files
- `pom.xml`
- new DTO / Validator / Assembler / Orchestrator files
- Position Monitor execution paths
- Candidate / Decision generation / Point / Trading paths

## 6. Status Mapping Readiness

| Status | Implementable with existing assets? | Source | Fail-closed? | Notes |
|---|---:|---|---:|---|
| `ALERT_POLICY_REVIEW_ONLY_READY` | Yes | Recent alert read path and boundary flags | No | Shows the status surface is readable and strictly review-only. |
| `ALERT_POLICY_BACKEND_PENDING_FAIL_CLOSED` | Yes | Controller/read path unavailable or exception | Yes | Used when backend cannot produce the status safely. |
| `ALERT_READ_MODEL_MISSING_FAIL_CLOSED` | Yes | Missing/ambiguous MonitorAlert read model | Yes | Do not fabricate alert state when read model is unavailable. |
| `ALERT_RECENT_EMPTY_REVIEW_ONLY` | Yes | `MonitorService.getRecentAlerts(limit)` empty result | No | Empty alerts are not proof of safety; display only. |
| `ALERT_SUPPRESSION_ACTIVE_REVIEW_ONLY` | Yes | `status=SUPPRESSED`, `suppressReason`, count reads | No | Shows suppression evidence without sending or writing. |
| `ALERT_COOLDOWN_ACTIVE_REVIEW_ONLY` | Yes | `cooldownUntil` / throttle evidence | No | Shows cooldown evidence only. |
| `ALERT_DUPLICATE_RISK_REVIEW_ONLY` | Partial / Yes | throttle or semantic-window count evidence | No | Must remain conservative and display-only. |
| `ALERT_FATIGUE_HIGH_REVIEW_ONLY` | Partial / Yes | recent/open/suppressed count evidence | No | Thresholds must be conservative and only indicate manual review. |
| `NOTIFICATION_POLICY_MISSING_FAIL_CLOSED` | Yes | missing/ambiguous policy evidence | Yes | Do not invent notification policy state. |
| `PUSH_BOUNDARY_BLOCKED_FAIL_CLOSED` | Yes | guardrail condition | Yes | Any need for Push send makes implementation blocked. |
| `RECHECK_BOUNDARY_BLOCKED_FAIL_CLOSED` | Yes | guardrail condition | Yes | Any need for recheck execution makes implementation blocked. |

## 7. Required Safety Fields

Future endpoint and dashboard copy must expose these safety fields:

| Field | Required value |
|---|---:|
| `reviewOnly` | `true` |
| `notPushSend` | `true` |
| `notExternalChannel` | `true` |
| `notRecheckExecution` | `true` |
| `notSchedulerTrigger` | `true` |
| `notCollectorTrigger` | `true` |
| `notApiClientRefresh` | `true` |
| `notAlertWrite` | `true` |
| `notTradingSignal` | `true` |
| `notCandidateSignal` | `true` |
| `notDecisionGeneration` | `true` |
| `notPointSignal` | `true` |
| `notExecutable` | `true` |
| `displaySlotsAreCandidatePool` | `false` |

Suggested non-executable status fields:

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

Forbidden response fields:

- Push send state
- external channel dispatch action
- recheck action
- scheduler trigger state
- collector trigger state
- API-client refresh action
- alert write action
- candidate ranking
- final direction
- entry
- stop
- TP
- RR
- position size
- leverage
- order action
- execution action
- auto-trading action

## 8. Required Targeted Tests

Next implementation must add targeted tests covering:

- endpoint smoke for `GET /api/dashboard/alert-fatigue-policy-status?symbol=BTCUSDT`;
- `reviewOnly=true`;
- `notPushSend=true`;
- `notExternalChannel=true`;
- `notRecheckExecution=true`;
- `notSchedulerTrigger=true`;
- `notCollectorTrigger=true`;
- `notApiClientRefresh=true`;
- `notAlertWrite=true`;
- `notTradingSignal=true`;
- `notCandidateSignal=true`;
- `notDecisionGeneration=true`;
- `notPointSignal=true`;
- `notExecutable=true`;
- `displaySlotsAreCandidatePool=false`;
- missing read model / unavailable alert read path maps to fail-closed;
- missing policy evidence maps to `NOTIFICATION_POLICY_MISSING_FAIL_CLOSED`;
- recent empty state maps to `ALERT_RECENT_EMPTY_REVIEW_ONLY`;
- suppression/cooldown evidence maps to review-only states when present;
- Push/recheck boundary states stay blocked and fail-closed;
- forbidden executable/send/refresh fields are absent;
- dashboard template contains the panel DOM ids and safety copy.

Recommended DOM ids for future implementation:

- `alertFatiguePolicyStatusPanel`
- `alertFatiguePolicyRuntimeStatusValue`
- `alertFatiguePolicyCountsValue`
- `alertFatiguePolicyCooldownValue`
- `alertFatiguePolicySuppressionValue`
- `alertFatiguePolicyBoundaryValue`

## 9. NO-GO Conditions

Future implementation must stop and return to design/readiness correction if it requires any of these:

- calling `MonitorAlertWriteServiceImpl`;
- Push send;
- external channel dispatch;
- recheck execution;
- scheduler trigger;
- collector trigger;
- API-client refresh;
- Candidate generation;
- Decision generation;
- Point generation;
- Trading behavior;
- final direction;
- entry / stop / TP / RR;
- order / execution / auto-trading;
- schema/config/pom changes;
- new DTO / Validator / Assembler / Orchestrator;
- Position Monitor execution;
- replay / recheck execution;
- changing notification policy status into a send strategy, auto-push strategy, or executable notification workflow.

## 10. Push / Recheck Boundary

Push and recheck are explicitly out of scope.

Allowed:

- reading stored alert rows;
- reading recent alert list;
- reading stored alert counts;
- displaying cooldown/suppression state;
- displaying fail-closed / not-push / not-recheck / not-refresh labels.

Forbidden:

- sending a notification;
- dispatching through external channel;
- calling Push services;
- calling PushRecheck mutation or scheduler paths;
- invoking `PushRecheckService#recheck`;
- refreshing market/API data;
- writing alert rows;
- treating notification policy as send authorization.

## 11. Boundary With Completed Slices

This future slice must not bypass existing V1 review-only boundaries:

- completed Watchlist / RuleConfig boundary remains read-only;
- completed Data Source Health boundary remains read-only and does not trigger refresh;
- completed Review / Replay boundary remains no replay execution;
- completed RiskActionGuard boundary remains manual review / non-executable;
- Display Slots are not a candidate pool;
- source trace and source health ambiguity must fail closed.

## 12. Next Task

- Next allowed action: `Minimal Review-Only Alert Fatigue / Notification Policy Status Runtime Wiring Implementation`
- Next branch: `minimal-review-only-alert-fatigue-notification-policy-status-runtime-wiring-implementation`
- PR risk: `B`
- Required future merge rule: B-risk implementation must not auto-merge without explicit GPT / human review.

## 13. Freeze Rule Compliance

- 是否创建新骨架: No.
- 是否复用 Cursor-era / V1 资产: Yes, existing `MonitorAlert` / dashboard alert owner assets.
- 是否减少重复: Yes, it keeps alert fatigue / policy status on the existing monitor-alert ownership path.
- 是否提升 capability level: No, readiness gate only.
- 是否接 service/runtime/dashboard/API: No implementation in this package; it allows the next minimal review-only endpoint/dashboard wiring.
- 是否符合 #830 审计建议: Yes.
