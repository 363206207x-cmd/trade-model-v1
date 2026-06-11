# V1 Minimal Review-Only RuleConfig Runtime Audit / Rule Explainability Runtime Wiring Design

## 1. Executive Summary

This package is design only. It does not implement Java business code, tests, dashboard business logic, schema/config/pom changes, endpoint wiring, external API refresh, scheduler/collector/API client triggers, Push, Candidate generation, Decision generation, Point generation, order/execution, auto-trading, DTO, Validator, Assembler, Orchestrator, replay execution, review result generation, P359, or P360.

Minimal future target: expose a review-only RuleConfig runtime audit / rule explainability status that tells the user whether the existing RuleConfig owner path can read the selected rule key, explain its safe configuration boundary, and keep all downstream action semantics fail-closed.

Design conclusion: the minimal owner path should stay on the existing RuleConfig / Watchlist owner assets. `RuleVersionLog` may be displayed only as adjacent review/analysis audit context; it must not be treated as proof that current RuleConfig runtime audit already exists.

Owner path:

```text
tm_rule_config / RuleConfigDO
  -> RuleConfigMapper enabled-rule reads
  -> RuleConfigServiceImpl enabled-rule cache
  -> RuleController existing /api/rule/push-watchlist pattern
  -> dashboard watchlistStatusPanel neighborhood / future minimal RuleConfig status surface
  -> RuleVersionLog query chain as separate audit context only
```

Rejected owner paths for the minimal slice:

```text
new RuleConfig audit table / mapper / service / DTO family
new RuleConfigAudit Validator / Assembler / Orchestrator
RuleVersionLog as generic current RuleConfig runtime status
/api/rule/reload as an audit/status read surface
```

Next step: `Implementation readiness gate for RuleConfig runtime audit / rule explainability`.

## 2. Owner Path To Preserve

Fixed future owner boundary:

```text
tm_rule_config
  -> RuleConfigDO(ruleId, ruleType, ruleKey, ruleValue, description, version, enabled)
  -> RuleConfigMapper.findByRuleKey / findAllEnabled
  -> RuleConfigServiceImpl.getRuleConfigMap enabled-rule cache
  -> RuleController read-only status surface
  -> dashboard review-only status/copy if readiness gate allows
```

Rules:

- Future implementation must reuse `RuleConfigDO`, `RuleConfigMapper`, `RuleConfigServiceImpl`, and `RuleController` instead of adding another RuleConfig owner.
- `RuleConfigServiceImpl#getRuleConfigMap()` currently exposes enabled rules only. The minimal design must treat disabled-rule visibility as partial unless a future readiness gate proves a safe existing path.
- `/api/rule/push-watchlist` is reusable as a Watchlist-key pattern and supporting input only. It is not a generic RuleConfig audit endpoint.
- `/api/rule/reload` mutates/reloads cache state and must not be used as the audit/status read surface.
- `RuleConfigWatchlistPoolReadAdapter` remains the Watchlist Pool membership owner. Generic RuleConfig explainability must not override Watchlist Pool boundaries.
- `RuleVersionLogQueryService` and `/api/review/rule-version-logs` are audit context for review/analysis history only. They are not current RuleConfig runtime status.
- Display Slots remain homepage display slots only. They are not Watchlist Pool, Candidate pool, scan universe, Push permission, Point source, or trading authorization.

## 3. Minimal Status Mapping

Allowed future statuses:

| Status | Trigger condition | Dashboard/API copy intent | Candidate/Decision/Point/Push allowed? | Review-only? | Fail-closed? |
|---|---|---|---|---|---|
| `RULECONFIG_AUDIT_REVIEW_ONLY_READY` | Requested enabled rule key is readable through the RuleConfig owner path; key, version/presence, description presence, source, reason, and safe boundary can be shown. | RuleConfig status is readable for manual review; this explains configuration state only. | No | Yes | No for display; still no downstream action |
| `RULECONFIG_WATCHLIST_KEY_READY_CONTEXT` | `push.watchlist.symbols` is readable and the existing Watchlist status path returns review-only ready. | Watchlist-backed RuleConfig key is readable; Watchlist Pool remains a boundary, not Push permission. | No | Yes | No for display; still no downstream action |
| `RULECONFIG_ENABLED_ONLY_PARTIAL` | The rule can only be inspected through enabled-rule cache semantics, so disabled vs missing cannot be proven. | Enabled-rule view is partial; readiness must decide whether that is sufficient. | No | Yes | Yes for downstream action |
| `RULECONFIG_CONFIG_MISSING_FAIL_CLOSED` | Requested rule key is missing, cache is empty/unavailable, or RuleConfig owner path cannot prove the key. | RuleConfig key is missing or unproven; keep all downstream behavior closed. | No | Yes | Yes |
| `RULECONFIG_VALUE_EMPTY_OR_PARSE_RISK_FAIL_CLOSED` | Rule value is blank, malformed, unsupported, or unsafe for the known parser, especially Watchlist symbols. | RuleConfig value cannot be safely explained; status remains fail-closed. | No | Yes | Yes |
| `RULECONFIG_VERSION_OR_DESCRIPTION_PARTIAL` | Config is readable, but version or description is absent. | Explainability metadata is partial; display only with an explicit partial reason. | No | Yes | Yes for downstream action |
| `RULECONFIG_AUDIT_CONTEXT_PARTIAL` | RuleVersionLog context exists but is review/analysis-linked, missing, or not scoped to current config status. | Audit context is partial and separate from current RuleConfig runtime status. | No | Yes | Yes for downstream action |
| `RULECONFIG_AUDIT_BLOCKED_FAIL_CLOSED` | The only way to answer would require reload/mutation, new schema/service ownership, external refresh, scheduler/collector/API-client trigger, replay execution, review result generation, Push, Candidate generation, Decision generation, Point generation, or trading semantics. | RuleConfig audit/explainability is blocked; keep status fail-closed. | No | Yes | Yes |

Status precedence:

1. `RULECONFIG_AUDIT_BLOCKED_FAIL_CLOSED`
2. `RULECONFIG_CONFIG_MISSING_FAIL_CLOSED`
3. `RULECONFIG_VALUE_EMPTY_OR_PARSE_RISK_FAIL_CLOSED`
4. `RULECONFIG_ENABLED_ONLY_PARTIAL`
5. `RULECONFIG_AUDIT_CONTEXT_PARTIAL`
6. `RULECONFIG_VERSION_OR_DESCRIPTION_PARTIAL`
7. `RULECONFIG_WATCHLIST_KEY_READY_CONTEXT`
8. `RULECONFIG_AUDIT_REVIEW_ONLY_READY`

`READY` means readable for display only. It never permits Candidate generation, Decision generation, Point generation, Push send, external channel, order/execution, or auto-trading.

## 4. Minimal Future Fields

Allowed future fields:

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
- `watchlistSymbols` only for the existing Watchlist key if already parsed safely
- `auditContextStatus`
- `ruleVersionLogContext`
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

Forbidden future fields:

- candidate ranking
- generated decision
- final direction
- entry
- stop
- TP
- RR
- position size
- leverage
- order action
- Push send state
- external channel state
- replay execution action
- review result generation action
- rule mutation action

The implementation readiness gate should prefer a minimal `Map` or existing object projection. A new DTO / Validator / Assembler family is not allowed for this minimal slice.

## 5. Dashboard/API Surface

Readiness gate should evaluate two safe implementation shapes:

1. Reuse existing `/api/rule/push-watchlist` and `watchlistStatusPanel` as the Watchlist-key RuleConfig explainability surface only.
2. Allow one minimal read-only `RuleController` status endpoint, for example:

```text
GET /api/rule/config-audit-status?ruleKey=push.watchlist.symbols
```

Preferred direction: allow a minimal RuleController `Map` response only if the readiness gate proves that the existing Watchlist endpoint cannot carry the required current RuleConfig status. The endpoint must be read-only and must not call `/api/rule/reload`.

The dashboard/API surface may show:

- selected rule key;
- enabled-rule visibility status;
- version and description presence;
- safe value summary, not executable action;
- Watchlist key status if the key is `push.watchlist.symbols`;
- separate RuleVersionLog context status, if shown;
- fail-closed reason;
- review-only label;
- not trading / not candidate / not decision generation / not point labels;
- Display Slots boundary label.

The surface must not add rule editing, reload buttons, review save, replay execution, Push actions, Candidate ranking, Decision generation, Point generation, final direction, entry/stop/TP/RR, order, execution, or auto-trading.

## 6. RuleVersionLog Boundary

RuleVersionLog is useful context but not the canonical current RuleConfig runtime audit owner.

Allowed use:

- Show whether review/analysis-linked rule-version logs are available as adjacent context.
- Link or describe that `/api/review/rule-version-logs` is separate from current config status.
- Map missing or unrelated logs to `RULECONFIG_AUDIT_CONTEXT_PARTIAL`.

Forbidden use:

- Claim that RuleVersionLog proves current RuleConfig status.
- Use RuleVersionLog to mutate or reload RuleConfig.
- Use RuleVersionLog to generate Candidate, Decision, Point, final direction, entry/stop/TP/RR, Push, or trading output.

## 7. Fail-Closed Rules

Future status must fail closed when:

- the requested rule key is absent from the enabled-rule owner path;
- the cache or mapper read is unavailable;
- disabled vs missing cannot be distinguished and the readiness gate requires disabled visibility;
- rule value is blank, malformed, unsupported, or unsafe to summarize;
- version/description/audit context is required but unavailable;
- answering the status would require `/api/rule/reload`, schema changes, new service ownership, external API refresh, scheduler/collector/API-client trigger, replay execution, review result generation, Push, Candidate generation, Decision generation, Point generation, order/execution, or auto-trading;
- Display Slots would be treated as Watchlist Pool or Candidate pool;
- any output could be mistaken as trading advice or executable rule intent.

Fail-closed means display can remain visible, but no downstream Candidate / Decision / Point / Push / Trading implication is allowed.

## 8. Minimal Future Implementation Boundary

If readiness gate returns GO, future implementation must stay within:

- existing `RuleConfigDO`;
- existing `RuleConfigMapper` and `RuleConfigServiceImpl` enabled-rule owner path;
- existing `RuleController`;
- existing `/api/rule/push-watchlist` as Watchlist-key evidence;
- optional minimal `RuleController` Map status endpoint only after readiness approval;
- optional minimal dashboard status/copy/DOM only after readiness approval;
- targeted controller/dashboard tests only if implementation touches endpoint/dashboard;
- source-of-truth docs.

Future implementation must not:

- add DTO / Validator / Assembler / Orchestrator;
- add schema/config/pom changes;
- add a new RuleConfig audit table, mapper, service, or persistence owner;
- call `/api/rule/reload` from the status path;
- call external API refresh, scheduler, collector, API client, review save, replay execution, or review result generation;
- connect Push or external channels;
- generate Candidate;
- generate a new Decision;
- generate Point;
- output final direction / entry / stop / TP / RR;
- connect order / execution / auto-trading;
- continue P359 or start P360.

## 9. Readiness Checklist

The next readiness gate must answer:

- Is the existing `/api/rule/push-watchlist` sufficient for the first RuleConfig explainability status, or is a generic read-only endpoint required?
- If a generic endpoint is needed, can it live in existing `RuleController` and return a minimal `Map`?
- Is enabled-rule visibility enough for the first slice, or does disabled/missing ambiguity require NO-GO?
- Can implementation avoid direct mapper expansion or new service ownership?
- Can implementation avoid new DTO / Validator / Assembler / Orchestrator?
- Can version and description be summarized without exposing unsafe raw rule value?
- Can RuleVersionLog remain context-only and not current runtime status?
- Does dashboard already have a safe insertion neighborhood near `watchlistStatusPanel`?
- What targeted tests are sufficient if endpoint/dashboard implementation proceeds?
- Can forbidden semantics remain absent from changed business files and visible UI copy?

## 10. Capability-Level Movement

- Current level: `REVIEW_ONLY_RUNTIME partial`.
- This package raises capability level: No, design only.
- Future minimal implementation target: keep `REVIEW_ONLY_RUNTIME partial` by adding review-only visibility over existing RuleConfig owner assets.
- It is not Production Wiring.
- It is not Push.
- It is not Candidate generation.
- It is not Decision generation.
- It is not Point generation.
- It is not Trading.

## 11. Freeze Rule Compliance

- 是否创建新骨架: No
- 是否复用 Cursor-era / V1 资产: Yes
- 是否减少重复: Yes
- 是否提升 capability level: No, design only
- 是否接 service/runtime/dashboard/API: No, design only; future readiness may authorize minimal review-only RuleController/dashboard wiring
- 是否符合 #830 审计建议: Yes

## 12. Final Recommendation

GO to `Implementation readiness gate for RuleConfig runtime audit / rule explainability`.

The readiness gate should prefer the existing RuleConfig / Watchlist owner path, keep RuleVersionLog as context-only, reject new DTO / Validator / Assembler / schema/service ownership, and decide whether the safest implementation shape is reuse of `/api/rule/push-watchlist` for the Watchlist key or one minimal read-only RuleController Map endpoint.
