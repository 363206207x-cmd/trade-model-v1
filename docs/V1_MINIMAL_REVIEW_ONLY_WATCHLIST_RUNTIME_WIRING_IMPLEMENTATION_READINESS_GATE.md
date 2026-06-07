# V1 Minimal Review-Only Watchlist Runtime Wiring Implementation Readiness Gate

This package is an implementation readiness gate only.

It does not add Java, tests, dashboard changes, schema/config/pom changes, service/runtime wiring, endpoints, MarketQuote wiring, Push wiring, external channel, candidates, entry / stop / TP / RR, final direction, order execution, auto-trading, P359/P360 continuation, or any new DTO / Validator / Assembler / Orchestrator.

## 1. Executive Summary

允许进入最小 implementation，但范围必须非常窄。

未来最小 implementation 只允许新增一个 read-only `GET /api/rule/push-watchlist` endpoint，用来展示 Watchlist Pool 当前只读状态。该 endpoint 必须复用 existing RuleConfig owner path：`RuleConfigMapper` / `RuleConfigServiceImpl` / `RuleConfigWatchlistPoolReadAdapter`。

未来第一轮 implementation 不允许新增完整 `GET /api/rule/push-watchlist/audit` endpoint。#852 和 #853 已确认当前没有足够的 Watchlist audit owner path；如果没有现成 audit table / mapper / VO，不能伪造 audit。Audit 必须先作为主 status 中的 `WATCHLIST_AUDIT_PARTIAL` 或 dashboard 文案展示。

不允许新增 DTO / Validator / Assembler / Orchestrator。

不允许改 schema。

允许最小 dashboard 改动，但只能是 Watchlist status / copy / DOM，不允许 dashboard expansion。

不允许接 Push、MarketQuote、candidate、Decision、Point、final direction、order、execution 或 auto-trading。

未来最小 implementation 允许候选文件：

- existing `RuleController` / existing rule config controller；
- existing `RuleConfigServiceImpl` only if needed to expose existing owner-path state safely；
- existing `RuleConfigWatchlistPoolReadAdapter` only if needed for owner-path status reuse；
- `src/main/resources/templates/dashboard.html` only for minimal Watchlist status / copy / DOM；
- existing controller / dashboard / RuleConfig watchlist tests；
- source-of-truth docs。

当前 capability level 不提升。本包只是 readiness gate。

下一步应进入 **Minimal Review-Only Watchlist Runtime Wiring Implementation**，但不得自动合并。

## 2. Implementation Permission Matrix

| Area | Allowed? | Allowed files | Reason | Guardrail |
|---|---|---|---|---|
| `GET /api/rule/push-watchlist` | Yes, for the next minimal implementation only. | Existing `RuleController` or existing rule/config controller. | Dedicated Watchlist status endpoint is missing; a minimal read-only endpoint can expose existing RuleConfig owner-path state. | Read-only only; no config write, Push, MarketQuote, candidate generation, point generation, order, or execution. |
| `GET /api/rule/push-watchlist/audit` | No for the first minimal implementation. | None in the next implementation. | Current audit owner path is missing/partial; adding an audit endpoint now risks fake audit or schema/mapper expansion. | Represent audit as `WATCHLIST_AUDIT_PARTIAL` in primary status/dashboard copy until a real audit owner path exists. |
| `RuleConfigServiceImpl` | Limited. | Existing `RuleConfigServiceImpl` only if endpoint cannot safely read current config map through existing methods. | RuleConfig is the configuration owner. | No new runtime service, no Push, no MarketQuote, no candidate or point logic. |
| `RuleConfigWatchlistPoolReadAdapter` | Limited. | Existing `RuleConfigWatchlistPoolReadAdapter` only if needed to reuse existing fail-closed parsing/status semantics. | Existing adapter already reads `push.watchlist.symbols` and enforces Watchlist Pool boundary semantics. | Do not turn it into a new endpoint owner or new wrapper family. |
| `RuleConfigMapper` | No change expected. | Existing `RuleConfigMapper` only if readiness evidence later proves a tiny read helper is unavoidable. | Existing mapper already owns enabled RuleConfig reads. | No schema change, no audit table work, no unrelated mapper expansion. |
| `dashboard.html` | Yes, minimal only. | `src/main/resources/templates/dashboard.html`. | Dashboard needs clear DB Watchlist vs localStorage Display Slots status/copy. | No large layout change, no complex card set, no Display Slots promotion. |
| controller tests | Yes. | Existing controller tests if present; new targeted test only if it tests the existing controller endpoint. | Endpoint behavior must be locked before merge. | No broad test suite expansion or new skeleton tests. |
| dashboard static tests | Yes. | Existing dashboard/controller/static tests if present. | Labels must show Watchlist Pool vs Display Slots boundary. | No visual feature expansion. |
| source-of-truth docs | Yes. | Existing status/progress/capability docs. | Must record capability movement and freeze compliance. | Do not create a new plan chain beyond the required verification. |
| `schema.sql` | No. | None. | First minimal slice must not add DB structures. | No `tm_push_watchlist_config_audit` creation in this implementation. |
| config / pom | No. | None. | No dependency/config changes are needed. | No hidden runtime or dependency expansion. |
| DTO / Validator / Assembler | No. | None. | Freeze rule blocks new skeleton families. | No new DTO / Validator / Assembler / Orchestrator. |
| Push / MarketQuote / Candidate / Point / Trading | No. | None. | This slice is Watchlist status visibility only. | No Push send, MarketQuote read, candidate generation, point generation, final direction, order, execution, or auto-trading. |

## 3. Minimal Endpoint Readiness

可以新增最小 `GET /api/rule/push-watchlist`。

该 endpoint 必须：

- be read-only；
- read only existing `push.watchlist.symbols` owner-path state；
- return review-only status；
- expose fail-closed states for missing / empty / unsafe config；
- clearly identify Display Slots as not the Watchlist Pool；
- use existing response conventions where possible；
- avoid new DTO if a map / existing object / existing response wrapper is sufficient。

不可以新增完整 `GET /api/rule/push-watchlist/audit` in the first minimal implementation。

Audit readiness:

- if no existing Watchlist audit owner path exists, return or display `WATCHLIST_AUDIT_PARTIAL`；
- do not fake latest operator / reason / time；
- do not reuse unrelated Push audit as Watchlist audit；
- do not add schema / mapper / VO just for audit in this slice。

Endpoint must not:

- write config；
- send Push；
- read MarketQuote；
- generate candidates；
- generate points；
- generate final direction；
- call order/execution；
- expose entry / stop / TP / RR；
- produce a trading signal。

## 4. Minimal Dashboard Readiness

允许最小 dashboard status / copy / DOM。

Dashboard may show only:

- Watchlist Pool current assets；
- source: DB / missing / unknown；
- fail-closed state；
- audit summary as `WATCHLIST_AUDIT_PARTIAL` unless a real audit endpoint exists；
- Display Slots label；
- localStorage Display Slots current assets if already available；
- review-only label；
- not Push label。

Dashboard must not:

- perform a large layout refactor；
- add a complex new card set；
- write Display Slots into Watchlist；
- treat the default six assets as candidate pool；
- present Display Slots as Watchlist Pool；
- connect Push；
- connect MarketQuote；
- connect candidate / Decision / Point；
- present order/execution/trading semantics。

## 5. Required Test Scope For Implementation

Future minimal implementation must add or strengthen targeted tests for:

- controller endpoint test for Watchlist status；
- controller endpoint test for audit only if an audit endpoint is explicitly allowed later；
- dashboard static test for Watchlist Pool vs Display Slots labels；
- dashboard static test for review-only / not Push copy；
- forbidden semantics grep；
- no DTO / Validator / Assembler check；
- no Push / MarketQuote / candidate / point / trade semantics check；
- no Display Slots promotion to Watchlist Pool；
- missing / empty Watchlist fail-closed status。

The implementation must also run:

- `bash scripts/check-workflow-contract.sh`
- `./mvnw -q -DskipTests compile`
- `./mvnw -q -DskipTests test-compile`
- targeted controller / dashboard tests
- `git diff --check`
- forbidden path check
- API smoke for `/api/rule/push-watchlist`
- dashboard smoke / visual check if dashboard is touched

## 6. Go / No-Go Decision

Decision: **A. GO: Minimal Review-Only Watchlist Runtime Wiring Implementation**.

Maximum allowed file types for the next implementation:

- existing rule/config controller for one read-only `GET /api/rule/push-watchlist` endpoint；
- existing RuleConfig owner-path class only if the endpoint cannot safely reuse current methods；
- existing `RuleConfigWatchlistPoolReadAdapter` only if needed to reuse fail-closed Watchlist parsing/status semantics；
- `src/main/resources/templates/dashboard.html` for minimal status/copy/DOM；
- existing targeted controller/dashboard/watchlist tests；
- source-of-truth docs。

Explicitly forbidden files / areas:

- new DTO / Validator / Assembler / Orchestrator；
- new runtime candidate wrapper；
- `schema.sql`；
- config / pom；
- Push external channel；
- MarketQuote；
- Candidate / Decision / Point；
- order / execution / auto-trading；
- P359 / P360。

The next implementation may proceed, but it must not auto-merge. It must pass targeted checks and remain review-only.

## 7. Capability-Level Statement

Current level: `REVIEW_ONLY_RUNTIME partial`, only from the PositionSync slice。

This package raises capability level: No, readiness gate only。

Future Watchlist minimal implementation target: `REVIEW_ONLY_RUNTIME partial` for the Watchlist slice。

It is not Production Wiring。

It is not Push。

It is not MarketQuote。

It is not candidate generation。

It is not point generation。

## 8. Freeze Rule Compliance

- 是否创建新骨架: No
- 是否复用 Cursor-era 资产: Yes
- 是否减少重复: Yes
- 是否提升 capability level: No, readiness gate only
- 是否接 service/runtime/dashboard/API: No, readiness only
- 是否符合 #830 审计建议: Yes

## 9. Final Recommendation

可以进入 **Minimal Review-Only Watchlist Runtime Wiring Implementation**。

允许改一个最小 read-only Watchlist status endpoint、极小 dashboard Watchlist status / copy / DOM、existing owner-path helper only if needed、targeted tests、source-of-truth docs。

禁止新增 DTO / Validator / Assembler / Orchestrator，禁止改 schema/config/pom，禁止新增完整 audit endpoint，禁止 Push、MarketQuote、candidate、Decision、Point、entry / stop / TP / RR、final direction、order、execution、auto-trading，P359/P360 继续冻结。

这不是 Push，因为它不发送消息、不触发外部通道、不生成候选。

这不是 MarketQuote，因为它不读取行情、不接 MarketQuoteClient、不读取 latest price / latest close。

这不是 P359/P360，因为它不恢复 source-owned runtime wrapper、不新增 runtime candidate assembler、不扩张骨架包。

不需要新 DTO / Validator / Assembler，因为未来 endpoint 只暴露 existing RuleConfig owner-path 的 review-only status；新增 skeleton 只会扩大重复面。
