# V1 Minimal Review-Only Watchlist Runtime Wiring Readiness Gate

This document is an implementation readiness gate for the minimal review-only Watchlist runtime slice.

It does not add Java, tests, dashboard changes, schema/config/pom changes, service/runtime wiring, MarketQuote wiring, Push wiring, external channel, candidates, entry / stop / TP / RR, final direction, order execution, auto-trading, P359/P360 continuation, or any new DTO / Validator / Assembler / Orchestrator.

## 1. Executive Summary

结论：**NO-GO for direct implementation**。当前不允许直接进入最小 Watchlist implementation。下一步应进入 **Further Watchlist API / Dashboard source read**。

现有 owner path 是真实的：`tm_rule_config` 存在，`RuleConfigMapper` / `RuleConfigServiceImpl` 存在，`RuleConfigWatchlistPoolReadAdapter` 能读取 `push.watchlist.symbols` 并在缺配置、空配置、非 Watchlist symbol、读失败时保持 incomplete / source-unavailable / fail-closed 语义。

但当前直接 implementation 的关键前提不足：

- `/api/rule/push-watchlist` 未发现。
- `/api/rule/push-watchlist/audit` 未发现。
- dashboard 已有 Watchlist Pool / Display Slots 边界文案和模块状态，但没有 DB-backed current Watchlist Pool status DOM。
- `tm_rule_config` 字段足够表达 `rule_key` / `rule_value` / `enabled`，但不够表达 latest audit operator / reason / time。
- 当前 source read 未发现 `tm_push_watchlist_config_audit`。
- RuleConfig / Watchlist 字段足够支持后续更小的 API / dashboard source read，不足以直接证明 dashboard/API 最小实现边界。

不需要新增 DTO / Validator / Assembler。不应新增 Watchlist wrapper owner。是否需要新增最小 read endpoint，必须等下一步更小 source read 确认；本 readiness gate 不授权直接新增 endpoint。当前不需要改 schema，不接 Push，不接 MarketQuote，不生成候选、点位或方向。

当前 capability level 不提升，仍为 `REVIEW_ONLY_RUNTIME partial`，且该 level 仍只来自 PositionSync + Dashboard slice。

## 2. Endpoint Sufficiency Check

| Endpoint | Exists? | Fields available | Enough for minimal status? | Gap |
|---|---|---|---|---|
| `/api/rule/push-watchlist` | No | None found. | No | Need further source read to decide whether an existing RuleConfig endpoint can be reused or a tiny review-only read endpoint is necessary. |
| `/api/rule/push-watchlist/audit` | No | None found. | No | Watchlist Pool audit API is absent. Missing audit must remain `WATCHLIST_AUDIT_PARTIAL`; direct implementation cannot claim audit visibility. |
| Existing RuleConfig endpoint | Partial | `RuleController` exposes `GET /api/rule/reload` only. | No | Reload endpoint does not return Watchlist symbols, source type, empty/fail-closed status, or audit metadata. |
| Dashboard data endpoint | No dedicated Watchlist endpoint found | Dashboard currently uses existing page state and localStorage Display Slots; no DB-backed Watchlist status payload found. | No | Need targeted source read for the smallest existing dashboard data path or endpoint shape. |

## 3. Field Sufficiency Check

| Required Display | Existing source field / object | Enough? | Gap |
|---|---|---|---|
| Watchlist Pool 当前资产 | `RuleConfigDO.ruleValue` under `push.watchlist.symbols`; parsed by `RuleConfigWatchlistPoolReadAdapter` | Partial | Field exists through RuleConfig, but no current API/dashboard payload exposes it. |
| source: DB / missing / unknown | RuleConfig row presence and adapter read result | Partial | Can infer in adapter path, but not currently dashboard/API visible. |
| config key: `push.watchlist.symbols` | `RuleConfigWatchlistPoolReadAdapter.WATCHLIST_RULE_KEY` | Yes | Needs API/dashboard exposure before implementation. |
| enabled / disabled if existing | `tm_rule_config.enabled`; `RuleConfigMapper.findAllEnabled()` | Partial | Enabled rows are loaded; disabled state is hidden by current enabled-only reads, so direct disabled display is not yet proven. |
| empty symbols | adapter parse result and incomplete reason `WATCHLIST_CONFIG_MISSING_OR_EMPTY` | Yes in adapter/test path | Not dashboard/API visible. |
| parse error if detectable | comma parsing is simple; read failure maps to `RULE_CONFIG_READ_FAILED` | Partial | Malformed complex format is not modeled; future minimal display should avoid claiming parse diagnostics beyond safe missing/read-failed status. |
| latest audit operator | No Watchlist audit object found | No | Audit source missing. |
| latest audit reason | No Watchlist audit object found | No | Audit source missing. |
| latest audit time | No Watchlist audit object found | No | Audit source missing. |
| Display Slots assets | `dashboard.html` localStorage / `tradeModel.displaySlots.v1` behavior | Yes for UI display | Display Slots remain localStorage-only and must not become candidate proof. |
| Display Slots source: localStorage | `dashboard.html` Display Slots code/copy | Yes | Needs explicit status/copy if implementation proceeds later. |
| fail-closed status | adapter incomplete/source-unavailable behavior and design statuses | Partial | Exists in adapter/test semantics, not in current dashboard/API payload. |
| review-only label | dashboard boundary copy exists | Partial | Current copy explains boundary; future minimal implementation may need a direct Watchlist status label. |
| not Push label | dashboard copy and design docs state no Push | Partial | Future dashboard/API status should explicitly state `只读状态，不发送 Push`. |

## 4. Dashboard DOM / Copy Check

Dashboard status:

- Existing Watchlist status area: partial. `dashboard.html` has Watchlist Pool / 观察库状态 in module status and boundary copy around Display Slots.
- Existing audit area: not found for Watchlist Pool.
- Existing Display Slots area: yes. Display Slots are rendered as homepage display slots with localStorage/custom/default behavior.
- Existing Watchlist Pool vs Display Slots boundary copy: yes. The page already states Display Slots are homepage display positions and Watchlist Pool / 观察库 is the push candidate boundary.
- Risk of default six being misread as candidate pool: still present at a medium level because there is no live DB-backed Watchlist Pool status panel showing current pool assets and source.
- Minimal dashboard copy/status change may be needed later, but only after the next source read identifies a safe insert point and endpoint/data source.
- Large dashboard rewrite is not needed and not allowed.
- New complex card is not allowed by default; a tiny existing-area status line is the maximum future shape if readiness becomes GO later.

## 5. Minimal Implementation Scope If GO

This gate returns NO-GO, so the following is only the future boundary if a later source read returns GO.

Allowed future scope would be limited to:

- Reuse `RuleConfigServiceImpl`.
- Reuse `RuleConfigWatchlistPoolReadAdapter`.
- Reuse existing `/api/rule/push-watchlist` if a future read finds it.
- Reuse existing `/api/rule/push-watchlist/audit` if a future read finds it.
- Reuse dashboard existing Watchlist / Display Slots area.
- Optional minimal dashboard copy/status mapping.
- Optional minimal existing controller test or dashboard static test.

Not allowed:

- new DTO / Validator / Assembler;
- new wrapper owner;
- schema/config/pom;
- Push external channel;
- MarketQuote;
- Candidate / Decision / Point;
- auto-write Display Slots into Watchlist;
- treating Display Slots as candidate pool;
- order / execution / auto-trading.

## 6. No-Go Conditions

Implementation cannot proceed if any of the following is true:

- It needs a new DTO / Validator / Assembler.
- It needs a large schema change.
- It needs direct Push wiring.
- It needs MarketQuote wiring.
- It needs candidate, point, or final direction generation.
- It treats Display Slots as candidate pool.
- Dashboard has no safe insertion point.
- Watchlist Pool and Display Slots cannot be clearly separated in display.
- API fields are insufficient and the proposed endpoint shape is still complex or unclear.
- Missing audit would mislead the user into thinking Watchlist audit is complete.

The current state triggers NO-GO because the dedicated watchlist endpoint, watchlist audit endpoint, and live dashboard Watchlist Pool status surface are all missing or unproven.

## 7. Go / No-Go Decision

Decision: **B. NO-GO: Further Watchlist API / Dashboard source read**.

Reason:

- Direct implementation would have to decide endpoint shape and dashboard DOM shape in the same package.
- `/api/rule/push-watchlist` and `/api/rule/push-watchlist/audit` are not present.
- `tm_push_watchlist_config_audit` was not found.
- Existing dashboard copy is useful, but not a current DB-backed Watchlist Pool status display.
- A smaller source read can determine whether the next implementation is dashboard-copy-only, a tiny read endpoint, or another NO-GO.

## 8. Required Tests / Checks For Future Implementation

Future implementation, if later authorized, must run:

- `bash scripts/check-workflow-contract.sh`
- `./mvnw -q -DskipTests compile`
- `./mvnw -q -DskipTests test-compile`
- targeted controller / dashboard tests if existing
- `git diff --check`
- forbidden path check
- grep to confirm no Push / MarketQuote / candidate / point / trade semantics were added
- API smoke for `/api/rule/push-watchlist` if implemented or found
- API smoke for `/api/rule/push-watchlist/audit` if implemented or found
- dashboard visual smoke if dashboard is touched

Future implementation must also confirm:

- no P359 / P360;
- no new DTO / Validator / Assembler;
- no Display Slots promotion to Watchlist Pool;
- no Push external channel;
- no MarketQuote;
- no candidate generation;
- no point generation;
- no order / execution / auto-trading.

## 9. Capability-Level Statement

- Current level: `REVIEW_ONLY_RUNTIME partial`, only from the PositionSync slice.
- This package raises capability level: No, readiness gate only.
- Future Watchlist minimal implementation target: `REVIEW_ONLY_RUNTIME partial` for Watchlist slice.
- This is not Production Wiring.
- This is not Push.
- This is not MarketQuote.
- This is not candidate generation.
- This is not point generation.

## 10. Freeze Rule Compliance

- 是否创建新骨架: No.
- 是否复用 Cursor-era 资产: Yes.
- 是否减少重复: Yes.
- 是否提升 capability level: No, readiness gate only.
- 是否接 service/runtime/dashboard/API: No, readiness only.
- 是否符合 #830 审计建议: Yes.

## 11. Final Recommendation

当前不允许直接进入 Watchlist 最小 implementation。下一步应做 **Further Watchlist API / Dashboard source read**，把 `/api/rule/push-watchlist`、`/api/rule/push-watchlist/audit`、RuleConfig 暴露字段、dashboard DOM 插入点和 audit 缺口读清楚。未来最小 implementation 只能复用 RuleConfig / Watchlist owner path、现有 dashboard Watchlist / Display Slots 区域、最小 review-only copy/status mapping；禁止 Push、MarketQuote、候选、点位、方向、交易，P359/P360 继续冻结。
