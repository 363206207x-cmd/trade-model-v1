# V1 Alert Fatigue / Notification Policy Status Runtime Wiring Implementation

## 1. Executive Summary

- Current baseline: `17ab553 docs(alerts): verify notification policy implementation readiness`
- Module: `Alert fatigue / notification policy status`
- Phase: `Implementation`
- Risk: `B`
- Capability movement: none. Current level remains `REVIEW_ONLY_RUNTIME partial`.

This package adds the minimal review-only runtime status surface for Alert fatigue / notification policy. It reuses the existing `MonitorService#getRecentAlerts` read path and stored `MonitorAlertDO` read model evidence. It does not call `MonitorAlertWriteServiceImpl`, does not send Push, does not use an external channel, does not execute recheck, and does not trigger scheduler / collector / API-client refresh.

## 2. Implemented Surface

| Surface | Result |
|---|---|
| Endpoint | `GET /api/dashboard/alert-fatigue-policy-status?symbol=BTCUSDT` |
| Dashboard panel | `alertFatiguePolicyStatusPanel` |
| Owner path | `DashboardController -> MonitorService#getRecentAlerts -> MonitorAlertDO read model` |
| Write side | Not used |
| External action | Not used |

The endpoint returns a `Map` payload only. No new DTO, Validator, Assembler, Orchestrator, schema, config, or pom change was added.

## 3. Status Mapping

Implemented status mapping:

- `ALERT_POLICY_REVIEW_ONLY_READY`
- `ALERT_POLICY_BACKEND_PENDING_FAIL_CLOSED`
- `ALERT_READ_MODEL_MISSING_FAIL_CLOSED`
- `ALERT_RECENT_EMPTY_REVIEW_ONLY`
- `ALERT_SUPPRESSION_ACTIVE_REVIEW_ONLY`
- `ALERT_COOLDOWN_ACTIVE_REVIEW_ONLY`
- `ALERT_DUPLICATE_RISK_REVIEW_ONLY`
- `ALERT_FATIGUE_HIGH_REVIEW_ONLY`
- `NOTIFICATION_POLICY_MISSING_FAIL_CLOSED`
- `PUSH_BOUNDARY_BLOCKED_FAIL_CLOSED`
- `RECHECK_BOUNDARY_BLOCKED_FAIL_CLOSED`

The last three boundary statuses are exposed in the status catalog and remain blocked/fail-closed guardrails. The implementation does not convert notification policy into a sending policy.

## 4. Safety Fields

The endpoint and dashboard preserve these safety fields:

- `reviewOnly=true`
- `notPushSend=true`
- `notExternalChannel=true`
- `notRecheckExecution=true`
- `notSchedulerTrigger=true`
- `notCollectorTrigger=true`
- `notApiClientRefresh=true`
- `notAlertWrite=true`
- `notTradingSignal=true`
- `notCandidateSignal=true`
- `notDecisionGeneration=true`
- `notPointSignal=true`
- `notExecutable=true`
- `displaySlotsAreCandidatePool=false`

Forbidden executable/send/refresh fields are covered by absence assertions in `DashboardControllerTest`; the endpoint returns only review-only status, counts, source health, and boundary flags.

## 5. Dashboard Panel

The dashboard adds `alertFatiguePolicyStatusPanel` after the RiskActionGuard panel and before Review / Replay. The panel displays:

- runtime status;
- symbol / source health;
- recent / open / suppressed count;
- cooldown / suppression flags;
- duplicate / fatigue flags;
- policy source / latest alert summary;
- review-only copy;
- not Push / not external channel / not recheck / not scheduler / not collector / not API-client refresh / not alert write copy;
- not trading / not candidate / not decision generation / not point / not executable copy.

The panel adds no action button and no send / refresh / execution operation.

## 6. Test Coverage

Targeted `DashboardControllerTest` coverage was added for:

- endpoint safety flags;
- read-model missing fail-closed state;
- suppression/cooldown review-only state;
- forbidden executable / send / refresh fields absent;
- dashboard panel DOM ids and safety copy.

## 7. Boundary Confirmation

- No `MonitorAlertWriteServiceImpl` invocation.
- No Push send.
- No external channel.
- No recheck execution.
- No scheduler / collector / API-client refresh.
- No Candidate generation.
- No Decision generation.
- No Point generation.
- No final direction / entry / stop / TP / RR.
- No order / execution / auto-trading.
- No Position Monitor execution.
- No replay / recheck.
- No DTO / Validator / Assembler / Orchestrator.
- No schema / config / pom.
- No P359 / P360.

## 8. Next Allowed Action

`Minimal Review-Only Alert Fatigue / Notification Policy Status Runtime Wiring Verification`

The next package is A-risk verification docs/source-of-truth only after this B-risk implementation receives review and is merged.
