# V1 RuleConfig Runtime Audit / Rule Explainability Runtime Wiring Implementation

## 1. Executive Summary

This package implements the minimal review-only runtime status for `RuleConfig runtime audit / rule explainability`.

- New endpoint: `GET /api/rule/config-audit-status?ruleKey=push.watchlist.symbols`
- New dashboard panel: `ruleConfigAuditStatusPanel`
- Owner path: existing `RuleConfigService#getRuleConfigMap` enabled-rule view through existing `RuleController`
- Watchlist key context: existing `push.watchlist.symbols` / `/api/rule/push-watchlist` status pattern
- RuleVersionLog boundary: context-only audit evidence, not current RuleConfig status owner
- Response shape: `Map<String, Object>`, no new DTO
- Current capability level remains `REVIEW_ONLY_RUNTIME partial`

This package does not add schema/config/pom changes, DTO, Validator, Assembler, Orchestrator, new service ownership, external API refresh, scheduler/collector/API-client trigger, Push, Candidate generation, Decision generation, Point generation, final direction, entry/stop/TP/RR, order/execution, auto-trading, replay execution, review result generation, P359, or P360.

RuleConfig runtime audit / rule explainability now has an implementation slice, but it still requires runtime wiring verification before it can be counted as a verified review-only runtime slice.

## 2. Implemented Endpoint

| Item | Value |
|---|---|
| Endpoint | `/api/rule/config-audit-status?ruleKey=push.watchlist.symbols` |
| Method | `GET` |
| Controller | Existing `RuleController` |
| Response shape | `ApiResponse<Map<String, Object>>` |
| Rule owner path | Existing `RuleConfigService#getRuleConfigMap` enabled-rule cache |
| Status path calls `/api/rule/reload` | No |
| New DTO / Validator / Assembler | No |

Implemented status fields:

- `status`
- `ruleKey`
- `ruleType`
- `configKey`
- `version`
- `descriptionPresent`
- `ruleValuePresent`
- `ruleValueSummary`
- `enabledKnown`
- `enabledOnlyView`
- `source`
- `sourceRef`
- `watchlistStatus`
- `watchlistSymbols`
- `auditContextStatus`
- `ruleVersionLogContext`
- `versionOrDescriptionPartial`
- `reason`
- `message`
- `failClosed`
- `reviewOnly = true`
- `notTradingSignal = true`
- `notCandidateSignal = true`
- `notDecisionGeneration = true`
- `notPointSignal = true`
- `notExecutable = true`
- `displaySlotsAreCandidatePool = false`

## 3. Status Mapping

| Status | Trigger | Fail-closed | Notes |
|---|---|---:|---|
| `RULECONFIG_WATCHLIST_KEY_READY_CONTEXT` | `push.watchlist.symbols` is readable, safely parsed, and has version/description metadata | No for display | Still no downstream action semantics |
| `RULECONFIG_AUDIT_REVIEW_ONLY_READY` | Non-Watchlist rule key is readable with bounded metadata | No for display | Generic config state only |
| `RULECONFIG_VERSION_OR_DESCRIPTION_PARTIAL` | Rule key is readable but version or description is missing | Yes | Disabled-vs-missing and metadata ambiguity stay partial |
| `RULECONFIG_CONFIG_MISSING_FAIL_CLOSED` | Requested key is absent from enabled-rule view | Yes | Disabled vs missing cannot be proven through this owner path |
| `RULECONFIG_VALUE_EMPTY_OR_PARSE_RISK_FAIL_CLOSED` | Rule value is blank, unsafe, or Watchlist parsing yields no symbols | Yes | No raw rule value is exposed as action content |
| `RULECONFIG_AUDIT_BLOCKED_FAIL_CLOSED` | RuleConfig status read throws or cannot be safely answered | Yes | Conservative default |

`RuleVersionLog` is always exposed as `RULECONFIG_AUDIT_CONTEXT_PARTIAL` and context-only. It is not used to prove current RuleConfig runtime status.

## 4. Dashboard Surface

Dashboard file:

```text
src/main/resources/templates/dashboard.html
```

Added panel and DOM ids:

- `ruleConfigAuditStatusPanel`
- `ruleConfigAuditRuntimeStatusValue`
- `ruleConfigAuditKeyValue`
- `ruleConfigAuditMetadataValue`
- `ruleConfigAuditSourceValue`
- `ruleConfigAuditEnabledOnlyValue`
- `ruleConfigAuditWatchlistValue`
- `ruleConfigAuditContextValue`
- `ruleConfigAuditReviewOnlyValue`
- `ruleConfigAuditSignalBoundaryValue`
- `ruleConfigAuditReloadBoundaryValue`
- `ruleConfigAuditReasonValue`

Dashboard fetch:

```text
/api/rule/config-audit-status?ruleKey=push.watchlist.symbols
```

Dashboard copy confirms:

- RuleConfig explainability is current config state only.
- RuleVersionLog is context-only and not the current RuleConfig status owner.
- The status path does not call `/api/rule/reload`.
- The status does not send Push.
- It is not Candidate, Decision generation, Point, trading signal, or executable behavior.
- It does not trigger schema/service expansion.

## 5. Test Coverage

Targeted tests updated:

```text
src/test/java/org/example/trademodel/controller/RuleControllerTest.java
src/test/java/org/example/trademodel/controller/DashboardControllerTest.java
```

Coverage:

- `config-audit-status` returns `RULECONFIG_WATCHLIST_KEY_READY_CONTEXT` for a readable Watchlist key.
- Missing enabled-rule config returns `RULECONFIG_CONFIG_MISSING_FAIL_CLOSED`.
- Empty/unsafe Watchlist config returns `RULECONFIG_VALUE_EMPTY_OR_PARSE_RISK_FAIL_CLOSED`.
- Missing version/description returns `RULECONFIG_VERSION_OR_DESCRIPTION_PARTIAL`.
- Safety booleans remain review-only and non-executable.
- Endpoint body does not expose mutation/action fields.
- Dashboard template contains the endpoint, panel DOM, statuses, RuleVersionLog context-only copy, reload boundary copy, and forbidden-action boundary copy.

## 6. Boundary Confirmation

- No schema/config/pom changed.
- No new DTO / Validator / Assembler / Orchestrator added.
- No new RuleConfig audit table, mapper, service, repository, or persistence owner added.
- `/api/rule/reload` is not used as the status path.
- No external API refresh, scheduler, collector, or API-client trigger added.
- No Push or external channel wiring added.
- No Candidate generation added.
- No Decision generation added.
- No Point generation added.
- No final direction, entry, stop, TP, RR, position size, leverage, order action, execution action, or auto-trading action added.
- No replay execution or review result generation added.
- P359 / P360 remain frozen.

## 7. Capability-Level Statement

- Current level: `REVIEW_ONLY_RUNTIME partial`
- This implementation moves RuleConfig runtime audit / rule explainability toward a review-only runtime slice.
- It does not complete or verify the slice by itself.
- Completed review-only runtime partial slices remain 8 until verification and visual closure complete.
- It is not Production Wiring.
- It is not Push.
- It is not Candidate generation.
- It is not Decision generation.
- It is not Point generation.
- It is not Trading.

## 8. Freeze Rule Compliance

- 是否创建新骨架: No
- 是否复用 Cursor-era / V1 资产: Yes
- 是否减少重复: Yes, by reusing existing RuleConfig / Watchlist owner assets instead of adding a new owner family
- 是否提升 capability level: Yes, toward a minimal review-only runtime status, pending verification
- 是否接 service/runtime/dashboard/API: Yes, existing RuleController / RuleConfigService enabled-rule view plus dashboard status
- 是否符合 #830 审计建议: Yes

## 9. Next Allowed Action

Next allowed action:

```text
Minimal Review-Only RuleConfig Runtime Audit / Rule Explainability Runtime Wiring Verification
```

Verification must confirm workflow contract, compile, test-compile, targeted controller/dashboard tests, endpoint and dashboard status fields, RuleConfig owner path, Watchlist key status, RuleVersionLog context-only boundary, no new DTO/Validator/Assembler/schema/service owner, forbidden semantics grep, forbidden path check, and `git diff --check`.
