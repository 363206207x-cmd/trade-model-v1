# V1 Minimal Review-Only Watchlist Runtime Wiring Implementation Plan

This package is an implementation plan / readiness design only.

It does not add Java, tests, dashboard changes, schema/config/pom changes, service/runtime wiring, endpoints, MarketQuote wiring, Push wiring, external channel, candidates, entry / stop / TP / RR, final direction, order execution, auto-trading, P359/P360 continuation, or any new DTO / Validator / Assembler / Orchestrator.

## 1. Executive Summary

本任务只做 implementation plan / readiness design，不实现。

未来最小 Watchlist implementation 可行，但不能直接跳过 readiness gate。#852 已确认专用 `/api/rule/push-watchlist`、`/api/rule/push-watchlist/audit`、DB-backed dashboard Watchlist status DOM 仍缺失，因此下一步必须先做 implementation readiness gate，锁定 exact file scope、endpoint shape、dashboard DOM 插入点和 tests。

未来允许新增一个最小 `GET /api/rule/push-watchlist` endpoint，但只允许作为 review-only read endpoint，且必须复用现有 RuleConfig owner path。它不得成为新的 Watchlist wrapper owner。

未来第一阶段不允许新增完整 `GET /api/rule/push-watchlist/audit` endpoint。当前没有 `tm_push_watchlist_config_audit`、watchlist audit mapper 或 watchlist audit VO。Audit 状态应先通过主 endpoint 或 dashboard 文案明确显示为 `WATCHLIST_AUDIT_PARTIAL`。只有后续单独证明不需要 schema / mapper / DTO 扩张时，才可重新评估一个最小 audit read endpoint。

不允许新增 DTO / Validator / Assembler / Orchestrator。

不允许改 schema。

未来允许改 dashboard，但只限最小 Watchlist status / copy / DOM 区域，不允许 dashboard expansion。

不允许接 Push、MarketQuote、Candidate、Decision、Point、final direction、order、execution 或 auto-trading。

未来最小实现候选文件只应包括：

- existing `RuleController` 或现有 rule/config controller；
- existing `RuleConfigServiceImpl` only if endpoint cannot read safely through current method;
- existing `RuleConfigWatchlistPoolReadAdapter` only if status payload needs an owner-path helper;
- `src/main/resources/templates/dashboard.html` only for tiny status/copy DOM;
- existing controller/dashboard/watchlist tests;
- source-of-truth docs.

下一步应做 **Minimal Review-Only Watchlist Runtime Wiring Implementation Readiness Gate**，不得直接 implementation。

## 2. Minimal Owner Path

Future minimal implementation owner path:

```text
tm_rule_config / push.watchlist.symbols
  -> RuleConfigMapper
  -> RuleConfigServiceImpl
  -> RuleConfigWatchlistPoolReadAdapter
  -> minimal RuleController endpoint if missing
  -> dashboard Watchlist status area
```

RuleConfig is the configuration owner.

Watchlist Pool is the candidate asset boundary.

Display Slots are only homepage display positions.

Display Slots must not become the candidate pool.

Push, MarketQuote, and Candidate code must not bypass Watchlist Pool.

No new Watchlist wrapper owner is allowed.

The future endpoint must expose owner-path status only. It must not invent a separate candidate universe, point proposal owner, Push universe, or MarketQuote scan source.

## 3. Minimal Endpoint Plan

| Endpoint | Add / Reuse | Purpose | Allowed fields | Not allowed |
|---|---|---|---|---|
| `GET /api/rule/push-watchlist` | Add if missing; must live in existing `RuleController` or existing rule/config controller path. | Expose current Watchlist Pool status from `push.watchlist.symbols` as review-only runtime status. | `symbols`, `source=DB/missing/unknown`, `configKey=push.watchlist.symbols`, `empty`, `missing`, `parseError`, `failClosed`, `status`, `blockingReasons`, `reviewOnly`, `notTradeInstruction`, `manualReviewRequired`, `auditStatus=WATCHLIST_AUDIT_PARTIAL` when audit is absent. | Trading signal, candidate ranking, MarketQuote data, Push send, entry / stop / TP / RR, final direction, order / execution. |
| `GET /api/rule/push-watchlist/audit` | Do not add in the first minimal implementation unless a readiness gate proves it can be implemented without schema, mapper, DTO, or fake audit data. | If ever allowed, expose existing audit only. Current source read found no Watchlist audit table/API. | Existing latest audit operator / reason / time only if already present; otherwise return `WATCHLIST_AUDIT_PARTIAL` through primary status, not a fake audit endpoint. | New audit table, fake audit rows, Push audit reuse as Watchlist audit, trading signal, candidate ranking, MarketQuote data, Push send, point fields, order / execution. |

Endpoint response must be review-only and fail-closed. It must not authorize Push or scan.

If `push.watchlist.symbols` is absent, blank, unreadable, or unsafe, the endpoint must return a fail-closed status rather than treating Display Slots as the Watchlist Pool.

## 4. Minimal Dashboard Plan

Allowed future dashboard scope:

- reuse the existing Watchlist / Display Slots area;
- add one minimal Watchlist status line or compact status block;
- add a clear DB Watchlist vs localStorage Display Slots label;
- add fail-closed status copy;
- add latest audit summary only if endpoint can honestly provide it;
- keep Display Slots localStorage behavior unchanged.

Forbidden future dashboard scope:

- no large layout refactor;
- no complex new card set;
- no automatic write from Display Slots into Watchlist;
- no default-six-as-Watchlist-Pool behavior;
- no Push;
- no MarketQuote;
- no candidate / Decision / Point wiring;
- no entry / stop / TP / RR;
- no final direction.

Dashboard copy must say:

- Watchlist Pool is the candidate boundary.
- Display Slots are homepage display only.
- Default six assets are not a Watchlist Pool.
- Empty / missing Watchlist is fail-closed for candidate and Push.
- This is review-only status and does not send Push.

## 5. Minimal Status Mapping

| Status | Trigger condition | Dashboard copy | Candidate / Push allowed? | Review-only? | Fail-closed? |
|---|---|---|---|---|---|
| `WATCHLIST_REVIEW_ONLY_READY` | `push.watchlist.symbols` exists, parses to one or more normalized symbols, owner path read succeeds, and no unsafe state is detected. | `Watchlist Pool loaded from DB config. Review-only status; no Push is sent.` | No | Yes | No for display; still no candidate/Push authorization in this slice. |
| `WATCHLIST_EMPTY_FAIL_CLOSED` | Config exists but parsed symbols are empty. | `Watchlist Pool is empty. Candidate and Push remain blocked.` | No | Yes | Yes |
| `WATCHLIST_CONFIG_MISSING` | `push.watchlist.symbols` is missing from enabled RuleConfig cache or RuleConfig read is unavailable. | `Watchlist config is missing. Display Slots are not a fallback candidate pool.` | No | Yes | Yes |
| `WATCHLIST_AUDIT_PARTIAL` | Watchlist Pool status exists but no dedicated Watchlist audit source is available. | `Watchlist audit is partial or unavailable; status remains review-only.` | No | Yes | No for pool display if config is valid; audit remains partial. |
| `DISPLAY_SLOTS_ONLY_NOT_CANDIDATE_POOL` | Dashboard has Display Slots but DB Watchlist status is missing, empty, or unavailable. | `Display Slots are local homepage display only and cannot become the candidate pool.` | No | Yes | Yes for candidate/Push. |
| `BLOCKED_FAIL_CLOSED` | Parse error, inconsistent owner-path state, unsafe ambiguity, or any attempt to use Display Slots / Push / MarketQuote / point / trading semantics. | `Watchlist status blocked fail-closed. No candidate, Push, point, or trading action is allowed.` | No | Yes | Yes |

All statuses are review-only.

No status authorizes candidate generation, Push send, MarketQuote read, point generation, final direction, order execution, or auto-trading.

## 6. Allowed File Scope For Future Implementation

Future implementation candidate files:

Production Java candidates:

- existing `RuleController` / existing `RuleConfigController` only if endpoint is missing;
- existing `RuleConfigServiceImpl` only if necessary;
- existing `RuleConfigWatchlistPoolReadAdapter` only if necessary.

Test candidates:

- existing `RuleControllerTest` if present;
- existing `DashboardControllerTest`;
- existing dashboard static test;
- existing `RuleConfigWatchlistPoolReadAdapterTest`;
- existing Watchlist / RuleConfig tests.

Frontend candidate:

- `src/main/resources/templates/dashboard.html`, only minimal status/copy area.

Documentation candidates:

- source-of-truth docs.

Explicitly forbidden:

- new DTO / Validator / Assembler / Orchestrator;
- `schema.sql`;
- config / pom;
- MarketQuote;
- Push external channel;
- Candidate / Decision / Point;
- order / execution / auto-trading;
- P359 / P360.

## 7. Implementation Readiness Decision

Decision: **A. GO: Minimal Review-Only Watchlist Runtime Wiring Implementation Readiness Gate.**

Reason:

- #852 narrowed the missing evidence enough: no dedicated read endpoint, no audit endpoint, no DB-backed dashboard Watchlist status DOM.
- The owner path is known and reusable.
- A direct implementation would still be too risky because exact endpoint response shape, audit behavior, dashboard DOM insertion point, and targeted tests must be fixed first.
- No further broad controller source read is needed.

The next readiness gate must verify:

- whether one minimal `GET /api/rule/push-watchlist` endpoint is allowed;
- whether audit remains in the primary status as `WATCHLIST_AUDIT_PARTIAL` instead of a new endpoint;
- whether existing DTOs / map response are sufficient without new DTO;
- exact production Java files allowed;
- exact dashboard DOM insertion point;
- exact targeted tests;
- forbidden path and forbidden semantic grep;
- no Push / MarketQuote / candidate / point / trading expansion.

Do not go directly to implementation.

## 8. Required Future Verification

Future implementation must run:

- `bash scripts/check-workflow-contract.sh`
- `./mvnw -q -DskipTests compile`
- `./mvnw -q -DskipTests test-compile`
- targeted controller / dashboard tests
- `git diff --check`
- forbidden path check
- grep confirming no Push / MarketQuote / point / trade semantics were added
- API smoke for `/api/rule/push-watchlist`
- API smoke for `/api/rule/push-watchlist/audit` only if explicitly added/reused
- dashboard visual smoke if dashboard is touched

## 9. Capability-Level Statement

Current level: `REVIEW_ONLY_RUNTIME partial`, only from the PositionSync slice.

This package raises capability level: No, plan only.

Future Watchlist minimal implementation target: `REVIEW_ONLY_RUNTIME partial` for the Watchlist slice.

It is not Production Wiring.

It is not Push.

It is not MarketQuote.

It is not candidate generation.

It is not point generation.

## 10. Freeze Rule Compliance

- 是否创建新骨架: No
- 是否复用 Cursor-era 资产: Yes
- 是否减少重复: Yes
- 是否提升 capability level: No, plan only
- 是否接 service/runtime/dashboard/API: No, plan only
- 是否符合 #830 审计建议: Yes

## 11. Final Recommendation

下一步具体做 **Minimal Review-Only Watchlist Runtime Wiring Implementation Readiness Gate**。

最小实现未来可允许一个 `GET /api/rule/push-watchlist` read-only endpoint、一个极小 dashboard Watchlist status/copy DOM、现有 RuleConfig owner path、现有 tests 的 targeted 增强。

禁止新增 DTO / Validator / Assembler，禁止改 schema/config/pom，禁止 Push、MarketQuote、candidate、Decision、Point、entry / stop / TP / RR、final direction、order、execution、auto-trading，P359/P360 继续冻结。

这不是 Push，因为它不发送消息、不生成候选、不触发外部通道。

这不是 MarketQuote，因为它不读取行情、不接 MarketQuoteClient、不读取 latest price / latest close。

这不是 P359/P360，因为它不恢复 source-owned runtime wrapper，不新增 runtime candidate assembler，也不继续骨架包。

当前还不能直接进入 implementation；必须先用 readiness gate 锁定 exact endpoint、DOM、test、forbidden path 和 no-new-DTO 边界。
