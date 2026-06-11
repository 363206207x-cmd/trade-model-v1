# V1 RuleConfig Runtime Audit / Rule Explainability Implementation Readiness Gate

## 1. Executive Summary

Decision: **GO** to `Minimal Review-Only RuleConfig Runtime Audit / Rule Explainability Runtime Wiring Implementation`.

This package is readiness-gate only. It does not implement Java business code, tests, dashboard business logic, schema/config/pom changes, endpoint wiring, external API refresh, scheduler/collector/API client triggers, Push, Candidate generation, Decision generation, Point generation, final direction, entry/stop/TP/RR, order/execution, auto-trading, DTO, Validator, Assembler, Orchestrator, replay execution, review result generation, P359, or P360.

GO is narrow:

- reuse the existing `RuleConfig` / Watchlist owner path;
- keep `RuleVersionLog` as context-only audit evidence;
- allow at most one minimal read-only `RuleController` `Map` status endpoint if implementation proceeds;
- allow at most one minimal dashboard status/copy/DOM surface near `watchlistStatusPanel` if implementation proceeds;
- require targeted controller/dashboard tests in the implementation package;
- keep disabled-vs-missing ambiguity explicit through enabled-only / partial / fail-closed status;
- forbid new DTO / Validator / Assembler / schema / service ownership.

Current capability level does not move. The project remains `REVIEW_ONLY_RUNTIME partial`, and the completed review-only runtime partial slice count remains 8.

## 2. Current Baseline

- Current merged main baseline: `2778b82 docsworkflow: implementation readiness gate for ruleconfig runtime audit / rule explainability`
- Prior handoff main in task prompt: `b4497e1 chore(workflow): add one-command operator orchestrator`
- Current module: `RuleConfig runtime audit / rule explainability`
- Current phase: `Implementation readiness gate`
- Risk level: `A` for this docs-only readiness gate
- Next implementation risk: `B`, because it may touch existing Java controller, dashboard template, and tests

Local state note: in this checkout, `main`, `origin/main`, and the task branch all contain `2778b82`; `b4497e1` is an ancestor. The readiness decision below uses the source-read and design handoff already present on merged main.

## 3. Source-Read And Design Summary

`docs/V1_RULECONFIG_RUNTIME_AUDIT_RULE_EXPLAINABILITY_SOURCE_READ.md` confirmed the reusable owner assets:

```text
tm_rule_config
  -> RuleConfigDO
  -> RuleConfigMapper
  -> RuleConfigService / RuleConfigServiceImpl
  -> RuleController
  -> /api/rule/push-watchlist
  -> dashboard watchlistStatusPanel
```

The source-read also confirmed the adjacent audit context:

```text
tm_rule_version_log
  -> RuleVersionLogDO / RuleVersionLogMapper
  -> RuleVersionLogQueryService
  -> ReviewController /api/review/rule-version-logs
  -> review page rule-version audit chain
```

The design file fixes the minimum future owner boundary:

- `/api/rule/push-watchlist` is reusable as the Watchlist-key evidence pattern, not as a generic RuleConfig audit endpoint.
- `/api/rule/reload` is mutating/cache-reload behavior and must not be used as a status/audit read path.
- `RuleVersionLog` is review/analysis-linked context only; it is not current RuleConfig runtime status.
- A minimal implementation may use existing objects or a small `Map`; no new DTO / Validator / Assembler family is allowed.

## 4. Readiness Questions

| Gate question | Decision | Reason |
|---|---|---|
| Is `/api/rule/push-watchlist` sufficient for the first RuleConfig explainability status? | **Partial / not sufficient alone** | It proves the Watchlist key and safe RuleConfig-backed status pattern, but it does not expose generic audit/explainability fields such as version/description presence, enabled-only view, audit context status, or explicit not-candidate/not-decision/not-point flags. |
| Is a generic read-only endpoint required? | **Yes, if implementation proceeds** | A tiny `RuleController` `Map` endpoint such as `GET /api/rule/config-audit-status?ruleKey=push.watchlist.symbols` is the smallest safe shape for current config status. |
| Can it live in existing `RuleController`? | **Yes** | `RuleController` already owns RuleConfig API status behavior and has targeted test precedent through `RuleControllerTest`. |
| Is enabled-rule visibility enough? | **Yes, only with explicit partial/fail-closed copy** | `RuleConfigServiceImpl#getRuleConfigMap()` exposes enabled rules only. The implementation must expose `enabledOnlyView=true` and treat disabled-vs-missing ambiguity as partial/fail-closed for downstream action. |
| Can implementation avoid mapper expansion or new service ownership? | **Yes** | The first slice can read from `RuleConfigService#getRuleConfigMap()` and existing Watchlist status evidence. Direct mapper expansion is not required. |
| Can implementation avoid new DTO / Validator / Assembler / Orchestrator? | **Yes** | Use a minimal `Map` / existing object projection and targeted tests. |
| Can version and description be summarized safely? | **Yes** | Show version and presence/absence. Summarize rule value only as bounded config state, not trading rationale or action output. |
| Can RuleVersionLog remain context-only? | **Yes** | It may appear as `auditContextStatus` / `RULECONFIG_AUDIT_CONTEXT_PARTIAL`, but it must not prove current config status or trigger mutation. |
| Does dashboard have a safe insertion neighborhood? | **Yes** | `watchlistStatusPanel` and adjacent completed slice panels already establish review-only copy and Display Slots boundary language. |
| What targeted tests are sufficient later? | **Controller + dashboard** | `RuleControllerTest` for ready/missing/empty/partial/fail-closed and safety flags; dashboard template/controller tests for panel/DOM/copy; forbidden path/semantics checks. |

## 5. Allowed Future Implementation Scope

If this readiness gate is merged, the next implementation package may change only:

- existing `RuleController`, to add one minimal read-only `Map` status endpoint;
- `dashboard.html`, to add one small RuleConfig audit / explainability status panel or reuse adjacent Watchlist status copy;
- targeted controller/dashboard tests;
- implementation report documentation;
- source-of-truth documents.

Allowed future status fields:

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
- `watchlistSymbols` only for `push.watchlist.symbols` when safely parsed
- `auditContextStatus`
- `ruleVersionLogContext`
- `reason`
- `message`
- `failClosed`
- `reviewOnly=true`
- `notTradingSignal=true`
- `notCandidateSignal=true`
- `notDecisionGeneration=true`
- `notPointSignal=true`
- `notExecutable=true`
- `displaySlotsAreCandidatePool=false`

## 6. Forbidden Future Implementation Scope

The next implementation must not:

- add DTO / Validator / Assembler / Orchestrator;
- add schema/config/pom changes;
- add a new RuleConfig audit table, mapper, service, repository, or persistence owner;
- call `/api/rule/reload` from the status path;
- call external API refresh, scheduler, collector, API client, review save, replay execution, or review result generation;
- connect Push or external channels;
- generate Candidate;
- generate Decision;
- generate Point;
- output final direction;
- output entry / stop / TP / RR;
- output position size, leverage, order action, or executable action;
- connect order / execution / auto-trading;
- continue P359 or start P360;
- treat Display Slots as Watchlist Pool, Candidate pool, scan universe, Push permission, Point source, or trading authorization.

## 7. Fail-Closed Rules

The minimal implementation must fail closed when:

- the requested rule key is absent from the enabled-rule owner path;
- the cache or owner read is unavailable;
- disabled vs missing cannot be distinguished and the display would otherwise imply completeness;
- rule value is blank, malformed, unsupported, or unsafe to summarize;
- version, description, or audit context is missing and required for the selected status;
- answering would require reload/mutation, schema/service expansion, external refresh, scheduler/collector/API-client trigger, replay execution, review result generation, Push, Candidate generation, Decision generation, Point generation, order/execution, or auto-trading;
- output could be mistaken as trading advice, candidate eligibility, Point generation, Push permission, or executable rule intent.

Fail-closed means the dashboard/API can remain visible for manual review, but no downstream Candidate / Decision / Point / Push / Trading implication is allowed.

## 8. Required Future Checks

The implementation package must run:

- `bash scripts/check-workflow-contract.sh`
- `bash scripts/v1-state.sh`
- `bash scripts/v1-auto.sh next`
- `bash scripts/codex-next-task.sh`
- compile / test-compile if Java is touched
- targeted `RuleControllerTest`
- targeted dashboard/controller/template tests if dashboard is touched
- readiness/status grep for RuleConfig owner path, Watchlist key status, RuleVersionLog context-only boundary, and no new DTO/Validator/Assembler/schema/service owner
- forbidden semantics grep
- forbidden path check
- `git diff --check`

## 9. Readiness Result

Result: **GO**.

GO rationale:

- Existing RuleConfig owner assets are real and already used by completed Watchlist status behavior.
- Existing `/api/rule/push-watchlist` proves a safe review-only status pattern for the Watchlist key.
- Existing dashboard `watchlistStatusPanel` gives a safe user-visible insertion neighborhood.
- The missing generic RuleConfig audit/explainability status can be handled with one minimal read-only `RuleController` `Map` endpoint and dashboard copy.
- Disabled-rule visibility is partial but not blocking if represented explicitly as enabled-only / partial / fail-closed.
- `RuleVersionLog` can remain context-only and separate from current RuleConfig runtime status.
- No new DTO / Validator / Assembler / schema / service owner is needed.

NO-GO conditions for the next package:

- it requires schema/config/pom changes;
- it requires a new DTO / Validator / Assembler / Orchestrator;
- it requires a new RuleConfig audit table, mapper, service, repository, or persistence owner;
- it uses `/api/rule/reload` as status read behavior;
- it triggers external refresh, scheduler, collector, API client, replay execution, or review result generation;
- it generates Candidate, Decision, Point, final direction, entry / stop / TP / RR, Push, order/execution, or auto-trading.

## 10. Capability Movement

- Current level: `REVIEW_ONLY_RUNTIME partial`.
- This package raises capability level: No, readiness gate only.
- Completed review-only runtime partial slices remain 8.
- RuleConfig runtime audit / rule explainability is not implemented by this package.
- Future implementation target: remain `REVIEW_ONLY_RUNTIME partial`, not Production Wiring.

## 11. Freeze Rule Compliance

- 是否创建新骨架: No
- 是否复用 Cursor-era / V1 资产: Yes
- 是否减少重复: Yes, by forcing reuse of the existing RuleConfig / Watchlist owner path and rejecting wrapper owners
- 是否提升 capability level: No, readiness gate only
- 是否接 service/runtime/dashboard/API: No in this package; future implementation may minimally connect existing `RuleController` / dashboard review-only status
- 是否符合 #830 审计建议: Yes

## 12. Next Allowed Action

Next allowed action: **Minimal Review-Only RuleConfig Runtime Audit / Rule Explainability Runtime Wiring Implementation**.

Next implementation risk: **B**.

The next package may add only a minimal read-only `RuleController` status endpoint, minimal dashboard status/copy/DOM, targeted tests, implementation docs, and source-of-truth updates over existing RuleConfig / Watchlist assets. It must not add DTO / Validator / Assembler, schema/config/pom, Push, Candidate generation, Decision generation, Point generation, final direction, entry/stop/TP/RR, order/execution, auto-trading, replay execution, review result generation, P359, or P360.
