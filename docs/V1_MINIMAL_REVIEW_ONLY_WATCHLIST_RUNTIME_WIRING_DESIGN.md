# V1 Minimal Review-Only Watchlist Runtime Wiring Design

This document designs the minimal review-only Watchlist runtime wiring target after the Watchlist + RuleConfig + Dashboard/API source read.

This task is design only. It does not add Java, tests, dashboard changes, schema/config/pom changes, service/runtime wiring, MarketQuote wiring, Push wiring, external channel, candidates, entry / stop / TP / RR, final direction, order execution, auto-trading, P359/P360 continuation, or any new DTO / Validator / Assembler / Orchestrator.

## 1. Executive Summary

本任务只设计，不实现。

最小 runtime 目标是：用既有 `tm_rule_config` / `push.watchlist.symbols`、`RuleConfigMapper`、`RuleConfigServiceImpl`、`RuleConfigWatchlistPoolReadAdapter` 和 dashboard 现有 Watchlist Pool / Display Slots 边界文案，未来只读展示 Watchlist Pool 当前状态、Display Slots 当前状态、fail-closed 状态和 audit 可见性。

Owner path 必须是：

```text
tm_rule_config / push.watchlist.symbols
  -> RuleConfigMapper
  -> RuleConfigServiceImpl
  -> RuleConfigWatchlistPoolReadAdapter
  -> existing Watchlist API / future minimal API if missing
  -> dashboard watchlist status / audit display
```

本设计不需要新增 DTO / Validator / Assembler，也不允许新增 Watchlist wrapper owner。当前 source read 没有发现 dedicated `/api/rule/push-watchlist` 或 `/api/rule/push-watchlist/audit`，所以是否新增最小 endpoint 必须留给下一步 readiness gate 判断。当前 source read 也没有发现 `tm_push_watchlist_config_audit`，所以本设计不能要求直接改 schema。

本设计不接 Push，不接 MarketQuote，不生成候选，不生成点位，不生成方向，不接交易。下一步应进入 **Minimal Review-Only Watchlist Runtime Wiring Implementation Readiness Gate**。

## 2. Owner Path To Preserve

```text
tm_rule_config / push.watchlist.symbols
  -> RuleConfigMapper
  -> RuleConfigServiceImpl
  -> RuleConfigWatchlistPoolReadAdapter
  -> existing Watchlist API / future minimal API if missing
  -> dashboard watchlist status / audit display
```

Rules:

- Future implementation must not bypass the RuleConfig owner path.
- Future implementation must not add a parallel Watchlist wrapper owner.
- `RuleConfigWatchlistPoolReadAdapter` remains the source-read owner for Watchlist Pool membership semantics.
- Display Slots must not override Watchlist Pool.
- Push, MarketQuote, scheduler input, provider responses, dashboard ordering, or localStorage must not read Display Slots as the candidate universe.
- If a future minimal API is required, it must expose only review-only Watchlist status; it must not become Push, MarketQuote, candidate generation, point generation, or trade output.

## 3. Minimal Future Status Mapping

Allowed status:

- `WATCHLIST_REVIEW_ONLY_READY`
- `WATCHLIST_EMPTY_FAIL_CLOSED`
- `WATCHLIST_CONFIG_MISSING`
- `WATCHLIST_AUDIT_PARTIAL`
- `DISPLAY_SLOTS_ONLY_NOT_CANDIDATE_POOL`
- `BLOCKED_FAIL_CLOSED`

Suggested source fields or source objects:

| Status | Source fields / objects | Meaning |
|---|---|---|
| `WATCHLIST_REVIEW_ONLY_READY` | `tm_rule_config` row with key `push.watchlist.symbols`, enabled state if existing, parseable non-empty symbols list | Watchlist Pool can be shown as review-only status. It does not authorize Push or candidate generation. |
| `WATCHLIST_EMPTY_FAIL_CLOSED` | `push.watchlist.symbols` exists but the symbol list is empty after trim/parse | Candidate/push eligibility must fail closed. |
| `WATCHLIST_CONFIG_MISSING` | no enabled `push.watchlist.symbols` source or RuleConfig read unavailable | Watchlist Pool cannot be proven; Display Slots must not fill the gap. |
| `WATCHLIST_AUDIT_PARTIAL` | Watchlist config exists, but latest audit record / operator / reason / time is missing or unavailable | Watchlist may still be displayable as review-only status, but audit visibility is partial. |
| `DISPLAY_SLOTS_ONLY_NOT_CANDIDATE_POOL` | dashboard localStorage Display Slots exist, but DB Watchlist source is missing/empty/unavailable | Display Slots can be displayed, but they cannot become candidate pool proof. |
| `BLOCKED_FAIL_CLOSED` | parse error, contradictory source state, disabled source treated as active, localStorage promoted to candidate proof, or any Push/MarketQuote/point/trade semantic appears | Candidate/push eligibility is blocked fail-closed. |

Source type labels should be limited to:

- `DB` for RuleConfig-backed Watchlist Pool;
- `localStorage` for Display Slots;
- `unknown` for missing or unproven source.

If future source fields expose `last updated`, `operator`, or `reason`, they may be displayed as audit metadata only. Missing audit metadata must not be used to promote or demote assets by itself; it should surface as `WATCHLIST_AUDIT_PARTIAL`.

## 4. Watchlist Pool vs Display Slots Boundary

- Watchlist Pool is the candidate boundary.
- Display Slots are homepage display slots only.
- The homepage default six assets are not the push universe.
- The observation list may contain more than six assets.
- Assets outside Watchlist Pool must not enter candidate flow.
- Missing, empty, disabled, unreadable, or malformed Watchlist config must fail closed for candidate/push eligibility.
- Display Slots must not promote an asset into candidate status.
- Display Slots may display assets, but they cannot make assets eligible for Push, candidate generation, scan, MarketQuote reads, point generation, or final direction.
- A symbol visible in Display Slots without Watchlist Pool proof remains display-only.

## 5. Dashboard/API Minimal Surface

Future minimal dashboard/API display must show:

- Watchlist Pool current assets.
- Watchlist Pool source: `DB` / `missing` / `unknown`.
- Whether Watchlist Pool is empty.
- Whether Watchlist Pool is fail-closed.
- Latest audit status: latest operator / reason / time if present.
- Display Slots current assets.
- Display Slots source: `localStorage`.
- Label: `Display Slots 只是首页展示位，不是候选池`.
- Label: `不在 Watchlist Pool 的资产不会进入候选`.
- Label: `只读状态，不发送 Push`.

API / dashboard rules:

- If dedicated `/api/rule/push-watchlist` already exists in a future readiness read, reuse it.
- If it does not exist, the next readiness gate must decide whether a minimal review-only read endpoint is necessary.
- If `/api/rule/push-watchlist/audit` exists, reuse it for display-only audit metadata.
- If audit API/table is missing, display `WATCHLIST_AUDIT_PARTIAL` rather than claiming audit completion.
- Do not add complex dashboard cards by default.
- Do not connect Push.
- Do not connect MarketQuote.
- Do not generate candidates, points, final direction, or trade actions.

## 6. Fail-Closed Rules

- Config missing -> `WATCHLIST_CONFIG_MISSING` / fail-closed for candidate/push.
- Empty symbols -> `WATCHLIST_EMPTY_FAIL_CLOSED`.
- Disabled config -> fail-closed.
- Parse error -> `BLOCKED_FAIL_CLOSED`.
- RuleConfig service unavailable -> `WATCHLIST_CONFIG_MISSING` or `BLOCKED_FAIL_CLOSED`, depending on whether the failure is simple absence or unsafe ambiguity.
- localStorage Display Slots present but DB Watchlist missing -> `DISPLAY_SLOTS_ONLY_NOT_CANDIDATE_POOL`.
- Display Slots promoted to Watchlist proof -> `BLOCKED_FAIL_CLOSED`.
- Audit missing -> `WATCHLIST_AUDIT_PARTIAL`, but not automatic Watchlist invalidation.
- Any ambiguity about candidate/push eligibility -> fail-closed for candidate/push.
- Any Push send, MarketQuote read, candidate generation, point generation, final direction, order, execution, or auto-trading semantic -> `BLOCKED_FAIL_CLOSED`.

## 7. Minimal Future Implementation Boundary

If the next package reaches implementation readiness, future minimal implementation must be limited to:

- Prefer reusing `RuleConfigServiceImpl`.
- Prefer reusing `RuleConfigWatchlistPoolReadAdapter`.
- Prefer reusing existing `/api/rule/push-watchlist` if it is found.
- Prefer reusing the existing dashboard watchlist status area if it exists.
- If the endpoint is missing, the readiness gate must decide whether a tiny review-only endpoint is truly required.
- If the dashboard DOM slot is missing, the readiness gate must identify the smallest existing area for status/copy mapping.
- Do not add DTO / Validator / Assembler.
- Do not change schema.
- Do not connect Push.
- Do not connect MarketQuote.
- Do not connect Candidate / Decision / Point.
- Do not change Display Slots localStorage semantics.
- Do not automatically write Display Slots into Watchlist.
- Do not turn Watchlist status into an external send, candidate output, scan universe, point source, final direction, or trade instruction.

## 8. Readiness Checklist

The next readiness gate must check:

- Whether `/api/rule/push-watchlist` truly exists.
- Whether `/api/rule/push-watchlist/audit` truly exists.
- Whether `RuleConfigServiceImpl` exposes enough data for `push.watchlist.symbols`.
- Whether `RuleConfigMapper.findByRuleKey` / `findAllEnabled` is enough for the minimal display path.
- Whether Watchlist audit exists, and whether missing audit should remain `WATCHLIST_AUDIT_PARTIAL`.
- Whether dashboard has an existing DOM slot for Watchlist status.
- Whether Display Slots copy is enough or needs a small status/copy line.
- Whether existing tests cover `RuleConfigWatchlistPoolReadAdapter` missing/empty/non-member behavior.
- Whether future implementation needs only dashboard copy/status, or a minimal read endpoint first.
- Whether any proposed change would accidentally connect Push, MarketQuote, candidate generation, point generation, or trading.

## 9. Capability-Level Movement

- Current level: `REVIEW_ONLY_RUNTIME partial`, only from the PositionSync slice.
- This package raises capability level: No.
- This package is still worthwhile because it selects the minimal Watchlist owner path and blocks another Watchlist wrapper family.
- Future minimal Watchlist implementation target: `REVIEW_ONLY_RUNTIME partial` for the Watchlist slice.
- It is not Production Wiring.
- It is not Push.
- It is not MarketQuote.
- It is not candidate generation.

## 10. Freeze Rule Compliance

- 是否创建新骨架: No.
- 是否复用 Cursor-era 资产: Yes.
- 是否减少重复: Yes.
- 是否提升 capability level: No, design only.
- 是否接 service/runtime/dashboard/API: No, design only.
- 是否符合 #830 审计建议: Yes.

## 11. Final Recommendation

可以进入 **Minimal Review-Only Watchlist Runtime Wiring Implementation Readiness Gate**。最小实现未来只允许围绕既有 RuleConfig / Watchlist owner path 做 review-only 状态读取与 dashboard/API 显示边界确认；是否新增最小 endpoint 必须由 readiness gate 证明。禁止直接 Push、禁止 MarketQuote、禁止候选、禁止点位、禁止交易，P359/P360 继续冻结。这不是 Push，因为不发送；不是 MarketQuote，因为不读行情；不是 P359/P360，因为不新增 runtime candidate wrapper、DTO、Validator、Assembler 或 Orchestrator。
