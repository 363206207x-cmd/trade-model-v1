# V1 Watchlist + RuleConfig + Dashboard/API Runtime Slice Source Read

本文件只做 Watchlist / RuleConfig / Dashboard/API 的 source read + target confirmation。

本任务不新增 Java，不修改 Java，不新增测试，不修改测试，不改 dashboard，不改 schema/config/pom，不接 service/runtime，不接 MarketQuote，不接 Push，不接 external channel，不生成 entry / stop / TP / RR，不生成 final direction，不接 order / execution / auto-trading，不继续 P359/P360，不新增 DTO / Validator / Assembler / Orchestrator。

## 1. Executive Summary

Watchlist + RuleConfig + Dashboard/API 链路真实存在，但当前可用程度必须拆开看。

- Watchlist config source exists: `tm_rule_config` exists, `RuleConfigServiceImpl` uses `RuleConfigMapper.findAllEnabled()`, and `RuleConfigWatchlistPoolReadAdapter` reads `push.watchlist.symbols`.
- Watchlist Pool and Display Slots are separated in code and dashboard copy: Display Slots are local dashboard display priority only; Watchlist Pool remains the candidate boundary.
- API exists only partially: `RuleController` exposes `/api/rule/reload`; the current source read did not find `/api/rule/push-watchlist` or `/api/rule/push-watchlist/audit`.
- Dashboard surface exists partially: `dashboard.html` displays Watchlist Pool / Display Slots boundary copy and module status, but does not yet expose a DB-backed current Watchlist Pool status panel.
- Audit exists for Push Recheck dispatch config, not for Watchlist Pool config. This source read did not find `tm_push_watchlist_config_audit`.
- Fail-closed semantics exist in the adapter/test path: missing config, empty config, read failure, missing service, non-watchlist symbol, and non-watchlist-pool requests all stay incomplete/source-unavailable or blocked-not-watchlist with no push/readiness/trading flags.
- This slice is suitable as the next minimal review-only runtime slice target, but only for **Minimal Review-Only Watchlist Runtime Wiring Design**. It is not ready for direct implementation until the design fixes the current API/audit/dashboard-surface boundary.
- P359 and P360 remain frozen.

Capability level does not change in this task. The project remains `REVIEW_ONLY_RUNTIME partial`, currently anchored by the PositionSync + Dashboard slice. This source read reduces duplication risk by choosing the existing RuleConfig / Watchlist adapter / dashboard owner path instead of adding another WatchlistPoolProof or runtime-candidate wrapper.

## 2. Source Read Inventory

| Area | Files/classes found | Existing behavior | Runtime/API connection | Dashboard connection | Gap |
|---|---|---|---|---|---|
| RuleConfig | `RuleConfigService`, `RuleConfigServiceImpl`, `RuleConfigMapper`, `RuleConfigDO`, `tm_rule_config` | Loads enabled rules into an atomic in-memory map; `RuleController` can reload rules. | Partial. `GET /api/rule/reload` exists; direct watchlist read/write API was not found. | Indirect only. | No dedicated Watchlist API; no confirmed seed row for `push.watchlist.symbols` in schema. |
| Watchlist Pool | `RuleConfigWatchlistPoolReadAdapter`, `RuntimeSourceReadRequestDTO`, `RuntimeSourceReadResultDTO`, `WatchlistRuntimeSourceDTO` | Reads `push.watchlist.symbols` from existing rule config and returns review-only source status. | Partial runtime source adapter exists; not wired to a dashboard/API status endpoint. | Dashboard has boundary copy, not current pool values. | Need design for displaying current pool status without external Push or MarketQuote. |
| Display Slots | `dashboard.html` localStorage custom list, `DEFAULT_DISPLAY_SLOT_SYMBOLS`, `buildDisplayList()` | Uses local browser state and summary decisions to render at most six homepage display slots. | Dashboard/UI only, not runtime source eligibility. | Yes. | Must remain UI-only and must not override Watchlist Pool membership. |
| Watchlist API | `RuleController` only has `/api/rule/reload`; grep found no `push-watchlist` endpoint in source. | No dedicated Watchlist Pool GET/POST endpoint found. | Missing. | Missing as direct status API. | Next design must decide whether to reuse existing RuleConfig/reload only or design a minimal review-only read endpoint later. |
| Watchlist audit | No `tm_push_watchlist_config_audit`; Push Recheck dispatch audit exists as `tm_push_recheck_dispatch_config_audit`. | Audit exists for dispatch config only, not Watchlist Pool. | Missing for Watchlist Pool. | Missing. | Do not claim watchlist audit as complete. |
| dashboard watchlist status | `dashboard.html` module status and copy around Watchlist Pool / Display Slots. | Shows boundary language and module status, not DB-backed watchlist symbols. | No direct current Watchlist status fetch found. | Partial. | Current display can explain the boundary, but cannot prove membership status. |
| dashboard display slots | `dashboard.html` `DEFAULT_DISPLAY_SLOT_SYMBOLS`, `loadCustomSymbols()`, `saveCustomSymbols()`, `buildDisplayList()` | Renders max six display items and supports local custom mode. | UI only. | Yes. | Needs continued copy guard so default six are not mistaken as Watchlist Pool. |
| fail-closed rule | `RuleConfigWatchlistPoolReadAdapterTest`, `DefaultWatchlistRuntimeSourceGuardValidatorTest`, `DefaultWatchlistRuntimeSourceServiceTest`, `WatchlistLowFrequencyScanSchedulerTest` | Missing/empty config, non-member symbols, missing service, bad requests, stale/unknown freshness, and disabled scheduler remain safe. | Test-backed adapter/service semantics. | Not directly visible as a dashboard status yet. | Next design should expose safe status without turning on scan/push. |
| schema / mapper / test coverage | `schema.sql: tm_rule_config`, `RuleConfigMapper`, watchlist source tests | Schema and mapper support generic rule config; tests cover adapter behavior. | Partial. | Partial. | No dedicated watchlist schema/audit; no direct dashboard/API runtime status. |

## 3. Existing Runtime Flow

```text
tm_rule_config / push.watchlist.symbols
  -> RuleConfigServiceImpl / RuleConfigMapper
  -> RuleConfigWatchlistPoolReadAdapter
  -> WatchlistRuntimeSourceGuardValidator / DefaultWatchlistRuntimeSourceService
  -> no dedicated /api/rule/push-watchlist endpoint currently found
  -> dashboard.html boundary copy / module status
  -> Display Slots localStorage only for homepage display
```

Segment status:

| Segment | Status | Runtime? | Dashboard visible? | Review-only safe? |
|---|---|---|---|---|
| `tm_rule_config` | exists | yes, generic config table | no direct display | yes, if read through adapter |
| `push.watchlist.symbols` | adapter expects it, schema seed not confirmed | partial | no | missing/empty fails incomplete |
| `RuleConfigServiceImpl / RuleConfigMapper` | exists | yes | no direct display | generic rule loader only |
| `RuleConfigWatchlistPoolReadAdapter` | exists | partial runtime read adapter | no direct display | yes, fail-closed tests exist |
| `DefaultWatchlistRuntimeSourceService` | exists as plain service class, not Spring-wired in this read | partial | no | yes, guard-enforced |
| `/api/rule/push-watchlist` | not found | missing | missing | not applicable |
| dashboard Watchlist boundary copy | exists | display only | yes | yes, explanatory only |
| Display Slots localStorage | exists | no, UI-only | yes | safe if kept separate |

## 4. Watchlist Pool vs Display Slots Boundary

The boundary is clear and must stay clear:

- Watchlist Pool is the maximum candidate boundary.
- Display Slots are homepage display positions only.
- The homepage default six assets are not the push universe.
- Assets outside Watchlist Pool must not enter candidate flow.
- Missing, empty, unavailable, disabled, or unreadable Watchlist Pool config must fail closed or remain incomplete.
- Display Slots must never override Watchlist Pool membership.
- A symbol shown in Display Slots without Watchlist Pool proof is still not eligible.

Current dashboard copy already states that Display Slots are homepage display positions and Watchlist Pool / 观察库 remains the candidate boundary. Current tests also assert the default scheduler does not scan Display Slots or non-watchlist assets.

## 5. Dashboard/API Visibility

- `/api/rule/push-watchlist`: not found in current `src/main/java` / `src/test/java` / `src/main/resources` source read.
- `/api/rule/push-watchlist/audit`: not found.
- `RuleController`: only exposes `/api/rule/reload`.
- Dashboard current watchlist display: partial. It shows Watchlist Pool / Display Slots boundary copy and module status but not a live/current pool symbol list from `tm_rule_config`.
- Dashboard audit display: not found for Watchlist Pool. Push Recheck dispatch config audit is a different capability and must not be treated as Watchlist Pool audit.
- Dashboard Display Slots: exists and visible via localStorage/custom/system mode behavior.
- Misleading risk: medium. The dashboard copy reduces the risk, but because there is no live Watchlist Pool status panel, a user could still over-read default six Display Slots as the observed universe unless the next slice makes the distinction more explicit.

## 6. Review-only Runtime Readiness

| Requirement | Status | Evidence | Gap |
|---|---|---|---|
| Watchlist config source exists | Partial / yes | `tm_rule_config`, `RuleConfigMapper`, `RuleConfigServiceImpl`, `RuleConfigWatchlistPoolReadAdapter` | `push.watchlist.symbols` seed/current value not confirmed. |
| API exists | Partial / no for dedicated watchlist | `/api/rule/reload` exists; no `/api/rule/push-watchlist` found | Need design before any endpoint work. |
| audit exists | No for Watchlist Pool | no `tm_push_watchlist_config_audit`; only Push Recheck dispatch audit exists | Need design if audit is required later. |
| dashboard surface exists | Partial | `dashboard.html` Watchlist Pool / Display Slots boundary copy and module status | No DB-backed watchlist current status panel. |
| Watchlist vs Display Slots separated | Yes | dashboard copy, `buildDisplayList()` local UI behavior, tests asserting no Display Slots scan universe | Keep enforced in future design. |
| fail-closed semantics exist | Yes in adapter/test path | `RuleConfigWatchlistPoolReadAdapterTest`, `DefaultWatchlistRuntimeSourceGuardValidatorTest`, `DefaultWatchlistRuntimeSourceServiceTest` | Not yet visible as a dashboard/API runtime status. |
| no Push external channel | Yes for this path | source read only; scheduler disabled-by-default; DTO flags `opportunityPushAllowed=false` | Do not connect external channel. |
| no point / trade semantics | Yes | runtime source DTO/result force no readiness/trading/entry-stop-TP-RR flags | Keep this boundary in next design. |

## 7. Next Step Decision

Decision: **A. GO: Minimal Review-Only Watchlist Runtime Wiring Design**.

Reason:

- The owner path is real enough for design: `tm_rule_config` / `RuleConfigServiceImpl` / `RuleConfigWatchlistPoolReadAdapter` / watchlist runtime source guard tests / dashboard boundary copy exist.
- The slice directly reuses Cursor-era and merged-main assets instead of creating a new WatchlistPoolProof wrapper.
- The current gaps are exactly design questions: how to expose current watchlist status, how to handle no dedicated API/audit, and how to keep Display Slots separate.
- Direct implementation is not allowed yet because the dedicated Watchlist API and Watchlist audit path are missing, and the dashboard does not yet display current DB-backed Watchlist Pool status.

Do not recommend:

- P359 or P360.
- new DTO / Validator / Assembler.
- Three AI.
- Position Monitor expansion.
- Push external channel.
- MarketQuote wiring.
- point generation.
- order / execution / auto-trading.

## 8. Freeze Rule Compliance

- 是否创建新骨架: No.
- 是否复用 Cursor-era 资产: Yes.
- 是否减少重复: Yes.
- 是否提升 capability level: No, source read only.
- 是否接 service/runtime/dashboard/API: No new wiring; source-read only.
- 是否符合 #830 审计建议: Yes.

## 9. Final Recommendation

Watchlist + RuleConfig + Dashboard/API 适合作为下一个 runtime slice 的设计目标，但只能先进入 **Minimal Review-Only Watchlist Runtime Wiring Design**。当前真实 owner 是 `RuleConfigServiceImpl` / `RuleConfigMapper` / `tm_rule_config` / `RuleConfigWatchlistPoolReadAdapter`，dashboard 只负责显示和解释边界；Display Slots 继续只是首页展示位。P359/P360、外部 Push、MarketQuote、点位、交易全部继续冻结。下一步不要直接实现 Push 或 MarketQuote，也不要新增 DTO/Validator/Assembler；先把最小 Watchlist runtime status 的 owner path、API/dashboard边界、fail-closed 状态映射设计清楚。
