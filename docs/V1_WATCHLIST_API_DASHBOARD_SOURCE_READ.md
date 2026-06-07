# V1 Watchlist API / Dashboard Source Read

This source read narrows the Watchlist + RuleConfig + Dashboard/API gap after #851.

It does not add Java, tests, dashboard changes, schema/config/pom changes, service/runtime wiring, endpoint wiring, MarketQuote wiring, Push wiring, external channel, candidates, entry / stop / TP / RR, final direction, order execution, auto-trading, P359/P360 continuation, or any new DTO / Validator / Assembler / Orchestrator.

## 1. Executive Summary

`/api/rule/push-watchlist` does not currently exist.

`/api/rule/push-watchlist/audit` does not currently exist.

The only current Rule API owner found is `RuleController` under `/api/rule`, and it exposes only `GET /api/rule/reload`. That endpoint reloads rule config and returns a status string; it cannot be reused as a read-only Watchlist Pool status endpoint.

The existing reusable owner path is real but internal: `tm_rule_config` -> `RuleConfigMapper` -> `RuleConfigServiceImpl` -> `RuleConfigWatchlistPoolReadAdapter`. It already reads the `push.watchlist.symbols` rule key through the enabled rule cache and has fail-closed / incomplete-safe test coverage.

`dashboard.html` has Display Slots DOM and clear boundary copy saying Display Slots are homepage display only and Watchlist Pool is the candidate boundary. It does not yet have a dedicated DB-backed Watchlist Pool status DOM or Watchlist audit DOM.

Current minimal implementation is **NO-GO as a direct dashboard-only implementation** because there is no existing read endpoint and no dedicated live Watchlist status/audit DOM to consume it.

The next step can be **GO to a minimal implementation plan / readiness design**, not implementation. That plan may evaluate a tiny review-only read endpoint and a tiny dashboard status slot, while still reusing the RuleConfig owner path and adding no DTO / Validator / Assembler.

No new DTO / Validator / Assembler is needed. A future implementation should first prove whether existing `ApiResponse` plus existing watchlist runtime source/result objects or a simple response map is sufficient.

No schema change is needed for current Watchlist Pool status because `tm_rule_config` already stores `rule_key`, `rule_value`, `version`, and `enabled`. Watchlist audit remains absent and must stay `WATCHLIST_AUDIT_PARTIAL` unless a separate minimal audit design is approved.

Push, MarketQuote, candidate generation, point generation, final direction, order, execution, and auto-trading remain forbidden.

## 2. Endpoint Inventory

| Endpoint / Controller | Exists? | Method | Returned fields / behavior | Reusable for minimal Watchlist status? | Gap |
|---|---|---|---|---|---|
| `RuleController` | Yes | `@RequestMapping("/api/rule")` | Rule API owner. | Partial owner only. | It exposes reload only, not Watchlist status. |
| `/api/rule/reload` | Yes | `GET` | Calls `RuleConfigService.reloadRules()` and returns `ApiResponse<String>` with reload success/failure text. | No. | It mutates/reloads cache and does not expose `push.watchlist.symbols`, enabled state, parsed symbols, source, or fail-closed status. |
| `/api/rule/push-watchlist` | No | Not found | No endpoint found in controllers, tests, resources, or docs beyond planning docs. | No. | A future minimal read endpoint may be required. |
| `/api/rule/push-watchlist/audit` | No | Not found | No endpoint found. | No. | Watchlist Pool audit API is absent. |
| Existing RuleConfig read endpoint | No | Not found | No general read endpoint for `RuleConfigDO` was found. | No. | `RuleConfigServiceImpl` is internal service/cache only. |
| `/api/push/recheck/dispatch/config` | Yes | `GET` | Returns Push Recheck dispatch config. | No. | Different domain; not Watchlist Pool. |
| `/api/push/recheck/dispatch/config/audit` | Yes | `GET` | Returns Push Recheck dispatch config audit. | No. | Audit exists for Push Recheck dispatch config only, not Watchlist Pool config. |
| `/api/dashboard/*` | Yes | `GET` | Dashboard summary/detail/refresh. | No direct Watchlist Pool source. | Dashboard can display copy and Display Slots, but does not expose DB Watchlist status. |

## 3. Service / Mapper / Field Inventory

| Source | Exists? | Fields / methods | Enough for status mapping? | Gap |
|---|---|---|---|---|
| `tm_rule_config` | Yes | `rule_id`, `rule_type`, `rule_key`, `rule_value`, `description`, `version`, `enabled`. | Yes for current Watchlist Pool source and basic status. | No operator / reason / timestamp fields for Watchlist audit. |
| `push.watchlist.symbols` | Yes as expected key | Read by `RuleConfigWatchlistPoolReadAdapter` from `RuleConfigService.getRuleConfigMap()`. | Yes for parsed symbol pool. | Not currently exposed by API/dashboard. |
| `tm_push_watchlist_config_audit` | No | Not found in `schema.sql` or source. | No. | Audit must remain partial/missing. |
| `RuleConfigMapper` | Yes | `findByRuleKey(ruleKey)` and `findAllEnabled()`. | Yes for enabled config reads. | Disabled config state is hidden from `findAllEnabled()` cache, so future status may not distinguish missing vs disabled without a targeted mapper/service decision. |
| `RuleConfigServiceImpl` | Yes | Atomic cache; `getRuleConfigMap()` lazy reload; `reloadRules()` loads enabled rules. | Yes for internal owner path. | No public read API and no audit metadata. |
| `RuleConfigWatchlistPoolReadAdapter` | Yes | Reads `push.watchlist.symbols`, parses symbols, returns `RuntimeSourceReadResultDTO` / `WatchlistRuntimeSourceDTO`, fails closed for missing service/config/request/non-member. | Yes for review-only source-read semantics. | It is not exposed as a Spring bean endpoint and does not provide dashboard-facing status on its own. |
| `WatchlistRuntimeSourceDTO` / `RuntimeSourceReadResultDTO` | Yes | Status, source type/ref, missing fields, blocking reasons, safety flags. | Potentially yes for future minimal read endpoint without new DTO. | Need implementation plan to decide whether reusing these internal DTOs in API is acceptable. |
| Watchlist audit mapper / VO | No dedicated Watchlist Pool version found. | Push Recheck dispatch audit exists, not Watchlist Pool audit. | No. | Keep `WATCHLIST_AUDIT_PARTIAL`; do not invent audit completeness. |

## 4. Dashboard DOM Inventory

| Dashboard area / DOM | Exists? | Current behavior | Can reuse? | Gap |
|---|---|---|---|---|
| Watchlist current status area | Partial only | Module status board says `Watchlist Pool / 观察库状态` is connected. | Only as a copy/status placeholder. | It is not DB-backed current pool status and does not list actual `push.watchlist.symbols`. |
| Watchlist audit area | No | No dedicated watchlist audit DOM found. | No. | Audit display is absent. |
| Display Slots area | Yes | `Display Slots / 首页展示位`, `btnReset`, `tilesRow`, local browser display list. | Yes for separation copy. | It cannot prove Watchlist Pool membership. |
| `tradeModel.displaySlots.v1` | Yes | Browser localStorage key for homepage display slots. | Yes for Display Slots status only. | LocalStorage is not Watchlist Pool source. |
| Watchlist Pool vs Display Slots copy | Yes | Multiple copies state Display Slots are homepage display and Watchlist Pool is the candidate boundary. | Yes. | Needs live Watchlist status so users can distinguish current DB pool from local display slots. |
| default six / restore default copy | Yes | `恢复默认`; homepage display limited to 6. | Yes for Display Slots UX. | Must not be interpreted as default Watchlist Pool. |
| fail-closed / not candidate pool copy | Partial | Candidate source boundary says push candidates come from Watchlist Pool, not Display Slots. | Yes for guardrail copy. | No dynamic fail-closed Watchlist status yet. |

## 5. Minimal Implementation Feasibility

Current direct implementation cannot only reuse an existing endpoint because `/api/rule/push-watchlist` and `/api/rule/push-watchlist/audit` are absent.

A future minimal implementation likely needs a tiny review-only read surface. It must reuse `RuleConfigServiceImpl` and `RuleConfigWatchlistPoolReadAdapter`, and it must not create a new Watchlist wrapper owner.

If a minimal endpoint is added later, it should avoid new DTO / Validator / Assembler by reusing existing `ApiResponse`, existing watchlist runtime source/result objects, or a very small map response only if the implementation plan proves that is safer and compatible with local patterns.

Dashboard cannot be completed by copy-only changes because the current page lacks a DB-backed Watchlist Pool status DOM. It can reuse the existing Display Slots area and module status area as insertion neighborhood, but future work must add only a tiny status slot, not a dashboard expansion.

No schema change is required for basic current pool status. Audit is absent; the future implementation must display audit as partial/missing rather than inventing audit state.

No service/runtime expansion, Push, MarketQuote, candidate, decision, point, or trading path is needed or allowed.

Future tests likely need:

- targeted controller test for the minimal read endpoint if one is added;
- dashboard static test for boundary copy and safety labels if dashboard is touched;
- existing `RuleConfigWatchlistPoolReadAdapterTest` remains the owner-path fail-closed proof;
- forbidden token checks for Push / MarketQuote / point / trading semantics.

## 6. Go / No-Go Decision

Decision: **GO to Minimal Review-Only Watchlist Runtime Wiring Implementation Plan / Readiness Design, not implementation.**

Reason:

- The missing pieces are now specific: no dedicated read endpoint, no audit endpoint, no DB-backed dashboard status DOM.
- The owner path is real and reusable: `tm_rule_config` -> `RuleConfigMapper` -> `RuleConfigServiceImpl` -> `RuleConfigWatchlistPoolReadAdapter`.
- Current direct implementation remains blocked because there is no endpoint to call and no live Watchlist status DOM.
- Further broad source read is not needed; the next step should define the smallest safe implementation scope, including whether a tiny read endpoint is allowed and how dashboard consumes it.

Future minimal implementation plan may allow:

- `RuleController` or an existing rule/config controller path only if it stays review-only;
- existing `RuleConfigServiceImpl`;
- existing `RuleConfigWatchlistPoolReadAdapter`;
- existing `ApiResponse`;
- existing watchlist runtime source/result objects if acceptable;
- a tiny dashboard status DOM near existing Watchlist / Display Slots copy.

Future minimal implementation plan must prohibit:

- new DTO / Validator / Assembler / Orchestrator;
- new wrapper owner;
- schema/config/pom changes;
- Push external channel;
- MarketQuote wiring;
- candidate / Decision / Point wiring;
- writing Display Slots into Watchlist;
- treating Display Slots as candidate pool;
- order / execution / auto-trading.

## 7. Freeze Rule Compliance

- 是否创建新骨架: No
- 是否复用 Cursor-era 资产: Yes
- 是否减少重复: Yes
- 是否提升 capability level: No, source read only
- 是否接 service/runtime/dashboard/API: No, source read only
- 是否符合 #830 审计建议: Yes

## 8. Final Recommendation

下一步应做 **Minimal Review-Only Watchlist Runtime Wiring Implementation Plan / Readiness Design**，把最小 read endpoint 是否允许、是否复用现有 watchlist runtime source/result object、dashboard 最小 DOM 插入点、测试边界和 forbidden path 全部定死。

这不是 Push，不是 MarketQuote，不是 P359/P360，也不是直接候选/点位链路。当前不能直接 implementation，因为 `/api/rule/push-watchlist`、`/api/rule/push-watchlist/audit` 和 DB-backed dashboard Watchlist status DOM 都不存在；但已经不需要继续泛泛 source read，缺口足够明确，可以进入最小实现计划。
