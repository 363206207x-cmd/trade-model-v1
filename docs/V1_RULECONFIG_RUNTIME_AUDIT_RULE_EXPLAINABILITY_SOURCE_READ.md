# V1 RuleConfig Runtime Audit / Rule Explainability Source Read

## 1. Executive Summary

本包只做 `RuleConfig runtime audit / rule explainability` 的源码读取，不实现功能。

- Current actual main: `ed6def3 docs(runtime): select ruleconfig audit next slice`
- Previous source-of-truth baseline found before this package: `d9f7817 chore(workflow): fix status summary baseline`
- Current capability level: `REVIEW_ONLY_RUNTIME partial`
- Completed review-only runtime partial slices: 8
- Source-read conclusion: GO to design
- Next allowed action: `Minimal Review-Only RuleConfig Runtime Audit / Rule Explainability Runtime Wiring Design`
- Next branch: `minimal-review-only-ruleconfig-runtime-audit-rule-explainability-design`

RuleConfig owner assets exist and are usable for design: `tm_rule_config`, `RuleConfigDO`, `RuleConfigMapper`, `RuleConfigService`, `RuleConfigServiceImpl`, `RuleController`, and the already-completed Watchlist + RuleConfig status path. The existing `/api/rule/push-watchlist` endpoint and `watchlistStatusPanel` prove a safe read-only RuleConfig-backed status pattern for the Watchlist Pool key.

There is also a separate rule-version audit chain: `tm_rule_version_log`, `RuleVersionLogDO`, `RuleVersionLogMapper`, `RuleVersionLogQueryService`, `RuleVersionLogQueryServiceImpl`, `ReviewController /api/review/rule-version-logs`, and the review page `规则版本审计链`. This is reusable as audit context, but it is not the same as a dedicated RuleConfig runtime audit / explainability status for current rule config.

No Java, tests, dashboard business logic, schema/config/pom, Push, Candidate generation, Decision generation, Point generation, trading, DTO / Validator / Assembler, replay execution, review result generation, P359, or P360 were changed.

## 2. Files Read

Required / source-of-truth files:

- `AGENTS.md`
- `docs/SESSION_BOOTSTRAP.md`
- `docs/ACTIVE_MAINLINE_STATUS.yml`
- `docs/CODEX_NEXT_TASK.yml`
- `docs/V1_PROGRESS_SOURCE_OF_TRUTH.md`
- `docs/V1_CURRENT_STATE.md`
- `docs/PROJECT_PROGRESS_INDEX.md`
- `docs/V1_CAPABILITY_MATRIX.md`
- `docs/V1_MVP_REALITY_ROADMAP.md`
- `docs/V1_NEXT_MINIMAL_RUNTIME_SLICE_SELECTION_AFTER_DATA_SOURCE_HEALTH.md`
- `docs/V1_WATCHLIST_RULECONFIG_DASHBOARD_RUNTIME_SLICE_SOURCE_READ.md`
- `docs/V1_WATCHLIST_API_DASHBOARD_SOURCE_READ.md`

RuleConfig / Watchlist / audit source files:

- `src/main/java/org/example/trademodel/entity/RuleConfigDO.java`
- `src/main/java/org/example/trademodel/mapper/RuleConfigMapper.java`
- `src/main/java/org/example/trademodel/service/RuleConfigService.java`
- `src/main/java/org/example/trademodel/service/impl/RuleConfigServiceImpl.java`
- `src/main/java/org/example/trademodel/controller/RuleController.java`
- `src/main/java/org/example/trademodel/service/watchlistsource/RuleConfigWatchlistPoolReadAdapter.java`
- `src/main/java/org/example/trademodel/service/watchlistsource/DefaultWatchlistRuntimeSourceService.java`
- `src/main/java/org/example/trademodel/service/watchlistsource/DefaultWatchlistRuntimeSourceGuardValidator.java`
- `src/main/java/org/example/trademodel/dto/watchlistsource/WatchlistRuntimeSourceDTO.java`
- `src/main/java/org/example/trademodel/entity/RuleVersionLogDO.java`
- `src/main/java/org/example/trademodel/mapper/RuleVersionLogMapper.java`
- `src/main/java/org/example/trademodel/service/RuleVersionLogQueryService.java`
- `src/main/java/org/example/trademodel/service/impl/RuleVersionLogQueryServiceImpl.java`
- `src/main/java/org/example/trademodel/controller/ReviewController.java`
- `src/main/java/org/example/trademodel/service/RuleEngineService.java`
- `src/main/java/org/example/trademodel/service/RuleBaseOutput.java`
- `src/main/resources/schema.sql`
- `src/main/resources/templates/dashboard.html`
- `src/main/resources/templates/review.html`
- `src/main/resources/static/js/review-page.js`
- `src/test/java/org/example/trademodel/controller/RuleControllerTest.java`
- `src/test/java/org/example/trademodel/service/watchlistsource/RuleConfigWatchlistPoolReadAdapterTest.java`
- `src/test/java/org/example/trademodel/service/impl/RuleVersionLogQueryServiceImplTest.java`
- `src/test/java/org/example/trademodel/service/RuleEngineServiceSourceTraceTest.java`

## 3. RuleConfig Asset Inventory

| Area | Existing assets | Existing behavior | Source-read conclusion |
|---|---|---|---|
| Domain / DO | `RuleConfigDO` | Holds `ruleId`, `ruleType`, `ruleKey`, `ruleValue`, `description`, `version`, `enabled`. | Reusable for read-only config inventory and explainability. |
| Schema | `tm_rule_config` | Stores `rule_id`, `rule_type`, `rule_key`, `rule_value`, `description`, `version`, `enabled`. | Enough for basic config status; lacks symbol/timeframe/scope/source-trace/audit timestamps. |
| Mapper | `RuleConfigMapper` | `findByRuleKey(ruleKey)` and `findAllEnabled()`. | Reusable; design must note `findAllEnabled()` hides disabled rules from the current cache. |
| Service | `RuleConfigService` / `RuleConfigServiceImpl` | Atomic enabled-rule cache; lazy reload on empty cache; reload reads enabled rules. | Reusable owner path; reload endpoint is mutating and must not be used as audit read. |
| Controller | `RuleController` | `GET /api/rule/reload`; `GET /api/rule/push-watchlist`. | Rule API owner exists. Push-watchlist endpoint is safe watchlist-specific status, not generic RuleConfig audit. |
| Watchlist adapter | `RuleConfigWatchlistPoolReadAdapter` | Reads `push.watchlist.symbols`, normalizes symbols, validates membership, returns review-only status. | Strong reusable pattern for fail-closed RuleConfig-backed status. |
| Watchlist runtime guard | `DefaultWatchlistRuntimeSourceGuardValidator` | Fails closed for missing source, non-watchlist, stale/unknown freshness, unavailable source. | Reusable safety pattern. |
| Watchlist status DTOs | `WatchlistRuntimeSourceDTO`, `RuntimeSourceReadResultDTO` | Carry review-only, not-trade-instruction, no-push/no-readiness/no-trading flags. | Reusable internally; design must decide whether to expose existing objects or a small map without new DTO. |
| Rule engine guard | `RuleEngineService` defaults | Forces advisory / non-executable output when source trace or Risk Action Guard is incomplete. | Useful safety precedent, but not a RuleConfig audit owner path. |

## 4. Watchlist + RuleConfig Completed Slice Assets

Reusable assets from the completed Watchlist + RuleConfig slice:

- `/api/rule/push-watchlist` returns `status`, `configKey`, `symbols`, `source`, `empty`, `failClosed`, `reviewOnly`, `displaySlotsAreCandidatePool=false`, `reason`, and `message`.
- `RuleControllerTest` verifies ready/missing/empty/fail-closed behavior and checks the response does not expose executable or external runtime fields.
- `dashboard.html` has `watchlistStatusPanel` with DOM ids:
  - `watchlistRuntimeStatusValue`
  - `watchlistSymbolsValue`
  - `watchlistSourceValue`
  - `watchlistFailClosedValue`
  - `watchlistDisplaySlotsBoundaryValue`
  - `watchlistReviewOnlyValue`
  - `watchlistReasonValue`
- Dashboard copy explicitly separates Display Slots from Watchlist Pool and says Display Slots are not the candidate pool.
- `RuleConfigWatchlistPoolReadAdapterTest` covers null request, non-watchlist-only request, incomplete request, missing service, read exception, missing/empty config, symbol not in watchlist, and symbol in watchlist.

These assets should be reused or mirrored in design. They should not be replaced with a new wrapper owner.

## 5. Dashboard / API Reusable Surface

| Surface | Exists? | Reusable? | Notes |
|---|---:|---:|---|
| `/api/rule/push-watchlist` | Yes | Yes, as pattern and maybe as upstream input | It is watchlist-specific and not a generic RuleConfig audit endpoint. |
| `/api/rule/reload` | Yes | No for status | It reloads cache, so it is not an audit/status read surface. |
| Generic RuleConfig read endpoint | No | Missing | Design must decide whether a minimal read-only status endpoint is needed. |
| RuleConfig audit endpoint | No | Missing | No dedicated current RuleConfig audit/status endpoint found. |
| `watchlistStatusPanel` | Yes | Yes, as insertion neighborhood / pattern | Already user-visible. |
| dedicated RuleConfig audit panel | No | Missing | Design may propose a minimal status panel if needed. |
| review page rule-version audit chain | Yes | Partial context | Shows rule version logs by analysis; not current config audit. |

## 6. Rule Field / Status Coverage

| Requested field / concept | Present? | Evidence | Gap |
|---|---:|---|---|
| rule key | Yes | `RuleConfigDO.ruleKey`, `tm_rule_config.rule_key`, `/api/rule/push-watchlist.configKey`. | Good for source-read design. |
| rule value | Yes | `RuleConfigDO.ruleValue`, `tm_rule_config.rule_value`. | Current endpoint exposes parsed watchlist symbols, not raw rule value. |
| enabled / disabled | Partial | `RuleConfigDO.enabled`, `tm_rule_config.enabled`, `findAllEnabled()`. | Enabled rules are visible; disabled rules are not visible through current cache. |
| description | Yes | `RuleConfigDO.description`, `tm_rule_config.description`. | Not exposed in current dashboard/API status. |
| version | Yes | `RuleConfigDO.version`, `tm_rule_config.version`, `RuleVersionLog.ruleVersion`. | Need design to avoid confusing config version with review rule-version log. |
| scope | No dedicated field | Could be inferred from `ruleType` or `ruleKey` only. | Design risk; do not invent scope without source proof. |
| symbol | Partial | Watchlist rule value stores symbols as CSV for `push.watchlist.symbols`; status endpoint returns parsed symbols. | RuleConfig itself has no symbol column. |
| timeframe | No dedicated RuleConfig field | Timeframe appears in other source trace/dashboard contexts. | Not a RuleConfig audit field today. |
| risk action | Indirect | RiskActionGuard display and RuleEngine safety checks exist elsewhere. | Not RuleConfig audit owner path. |
| source trace | Partial / indirect | Watchlist status exposes source/ref and adapter reasons; SourceTrace classes exist elsewhere. | RuleConfig has no source trace columns. |
| review-only status | Yes | Watchlist endpoint, DTO flags, dashboard copy. | Generic RuleConfig audit status missing. |
| fail-closed | Yes for Watchlist path | Missing/empty config and unsafe reads fail closed. | Generic RuleConfig audit fail-closed mapping not designed yet. |
| notTradingSignal / notExecutable | Partial | DTOs and dashboard safety copy carry `notTradeInstruction` / non-executable flags in adjacent paths. | `/api/rule/push-watchlist` currently has `reviewOnly` and `displaySlotsAreCandidatePool=false` but not all later safety flags. |

## 7. Rule Explanation / Audit Status

Existing explanation / audit sources:

- `RuleVersionLogQueryService` and `RuleVersionLogMapper` can query `tm_rule_version_log`.
- `ReviewController` exposes `GET /api/review/rule-version-logs`.
- `review-page.js` renders `规则版本审计链` with rule version, error type, operator, rollback flag, fallbackMatched, changeSummary, and changeDetail.
- `ReviewServiceImpl` writes rule version logs when review is saved.

Important boundary:

- `tm_rule_version_log` is review/analysis audit context, not current RuleConfig runtime status.
- It can provide audit context in design, but it should not be used to claim that RuleConfig runtime audit / explainability already exists.
- No dedicated `tm_rule_config_audit` or current RuleConfig audit endpoint was found.

## 8. Current Gaps

- No generic read-only RuleConfig runtime audit/status endpoint found.
- No dedicated dashboard panel for RuleConfig runtime audit / rule explainability.
- No dedicated RuleConfig audit table found.
- `tm_rule_config` lacks explicit `symbol`, `timeframe`, `scope`, `sourceTrace`, updated timestamp, or operator fields.
- `RuleConfigServiceImpl.getRuleConfigMap()` exposes only enabled rules, so current design cannot distinguish disabled vs missing through that cache alone.
- Existing `/api/rule/push-watchlist` is watchlist-specific; a future generic RuleConfig audit design must not overload it into Candidate or Push semantics.
- Existing RuleVersionLog chain is review-linked audit, not a current runtime config audit.
- No evidence found for a safe design that should directly implement before a design package.

## 9. Minimal Design Direction

Recommended next stage: `Minimal Review-Only RuleConfig Runtime Audit / Rule Explainability Runtime Wiring Design`.

The design should decide whether the minimal future status can:

- reuse `RuleConfigServiceImpl` / `RuleConfigMapper` / `tm_rule_config`;
- reuse `/api/rule/push-watchlist` as evidence for the Watchlist key only;
- optionally define a new minimal read-only status endpoint only after design/readiness approval;
- optionally define a minimal dashboard status panel/copy only after design/readiness approval;
- represent missing/disabled/empty/parse-risk states as fail-closed;
- expose rule key, enabled state, version, description presence, source type/ref, and reason without exposing trading semantics;
- treat RuleVersionLog as audit context only, not as a generic RuleConfig audit completion proof;
- avoid new DTO / Validator / Assembler by using existing objects or a small map response if implementation is later approved.

## 10. Boundary Conflicts

The future RuleConfig audit / explainability slice must not:

- connect Push or external channel;
- treat `push.watchlist.symbols` as permission to send Push;
- generate Candidate;
- generate Decision;
- generate Point;
- generate final direction;
- generate entry / stop / TP / RR;
- connect order / execution / auto-trading;
- trigger external API refresh;
- trigger scheduler / collector / API client;
- mutate rules through `/api/rule/reload`;
- create a new DTO / Validator / Assembler owner;
- continue P359 / P360.

Design risk notes:

- The word `audit` can accidentally pull in rule-version review logs, Push audit, or config mutation history. The design must pin audit to read-only current config explainability unless a later gate explicitly authorizes another asset.
- The word `explainability` can be mistaken as trading rationale. It must explain rule configuration and boundary status only, not produce advice.
- If disabled-rule visibility is required, current `findAllEnabled()` cache may be insufficient; that is a design gap, not an implementation instruction.

## 11. Go / No-Go

Decision: GO to design.

Reason:

- Existing RuleConfig and Watchlist owner paths are real and test-covered.
- Dashboard/API patterns already exist for one RuleConfig-backed review-only status.
- Missing pieces are design questions: generic status scope, audit definition, dashboard surface, safety flags, and disabled/missing/fail-closed mapping.
- No new schema/config/pom/DTO/Validator/Assembler is required for source read.
- Direct implementation is not authorized.

## 12. Next Task Definition

Next allowed action: `Minimal Review-Only RuleConfig Runtime Audit / Rule Explainability Runtime Wiring Design`.

Next branch: `minimal-review-only-ruleconfig-runtime-audit-rule-explainability-design`.

Risk: `A`, design docs and source-of-truth updates only.

The design package must:

- preserve the existing RuleConfig owner path;
- decide the minimal status mapping;
- decide dashboard/API surface;
- define fail-closed rules;
- define no-Push / no-Candidate / no-Decision-generation / no-Point / no-trading copy;
- decide whether existing objects or a small map can avoid new DTO / Validator / Assembler later;
- keep capability level unchanged until a future implementation is approved, verified, and visually closed.

## 13. Capability Level

- Current level: `REVIEW_ONLY_RUNTIME partial`
- This package movement: none, source read only
- Completed review-only runtime partial slices remain 8
- RuleConfig runtime audit / rule explainability is not implemented
- Still not Production Wiring
- Still not Push
- Still not Candidate generation
- Still not Decision generation
- Still not Point generation
- Still not Trading
