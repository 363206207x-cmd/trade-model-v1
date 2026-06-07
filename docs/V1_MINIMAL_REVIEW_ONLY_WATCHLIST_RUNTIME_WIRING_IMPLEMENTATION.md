# V1 Minimal Review-Only Watchlist Runtime Wiring Implementation

This package implements the smallest Watchlist review-only runtime status slice.

It adds one read-only Watchlist status API, minimal dashboard status/copy/DOM, targeted tests, and source-of-truth updates.

It does not add DTO, Validator, Assembler, Orchestrator, schema, config, pom, Push, external channel, MarketQuote, Candidate, Decision, Point, entry / stop / TP / RR, final direction, order execution, auto-trading, P359, or P360.

## 1. Executive Summary

本任务实现了最小 Watchlist review-only runtime wiring。

新增的可见能力是：

- `GET /api/rule/push-watchlist` 可返回 Watchlist Pool 当前只读状态；
- dashboard 显示 Watchlist Pool 当前资产 / empty / fail-closed；
- dashboard 明确 Display Slots 只是首页展示位；
- dashboard 明确 Display Slots 不是候选池；
- dashboard 明确默认六个币不是候选池；
- dashboard 明确不在 Watchlist Pool 不进入候选 / 推送 / 扫描 / 点位；
- dashboard 明确只读状态，不发送 Push。

当前能力仍是 `REVIEW_ONLY_RUNTIME partial`。

本包不等于 Production Wiring。

本包不等于 Push。

本包不等于 MarketQuote。

本包不等于 candidate generation。

本包不等于 point generation。

## 2. Implemented API

Endpoint:

```text
GET /api/rule/push-watchlist
```

Owner path:

```text
tm_rule_config / push.watchlist.symbols
  -> RuleConfigMapper
  -> RuleConfigServiceImpl
  -> existing RuleController read-only endpoint
```

Response uses `ApiResponse<Map<String, Object>>` and does not create a new DTO.

Returned review-only fields:

- `status`
- `configKey`
- `symbols`
- `source`
- `empty`
- `failClosed`
- `reviewOnly`
- `displaySlotsAreCandidatePool`
- `reason`
- `message`

Supported statuses:

- `WATCHLIST_REVIEW_ONLY_READY`
- `WATCHLIST_EMPTY_FAIL_CLOSED`
- `WATCHLIST_CONFIG_MISSING`
- `BLOCKED_FAIL_CLOSED`

`DISPLAY_SLOTS_ONLY_NOT_CANDIDATE_POOL` remains a dashboard/display boundary copy rather than a separate endpoint state in this first slice because Display Slots are browser-local state.

## 3. Implemented Dashboard Surface

Dashboard file:

```text
src/main/resources/templates/dashboard.html
```

Minimal DOM added:

- `watchlistStatusPanel`
- `watchlistRuntimeStatusValue`
- `watchlistSymbolsValue`
- `watchlistSourceValue`
- `watchlistFailClosedValue`
- `watchlistDisplaySlotsBoundaryValue`
- `watchlistReviewOnlyValue`
- `watchlistReasonValue`

Dashboard fetches:

```text
/api/rule/push-watchlist
```

Dashboard copy confirms:

- Display Slots are homepage display only.
- Display Slots are not the candidate pool.
- The default six assets are not the candidate pool.
- Assets outside Watchlist Pool do not enter candidate / Push / scan / point flows.
- The status is review-only and does not send Push.

## 4. Safety Boundary

This implementation does not:

- write Watchlist config;
- add an audit endpoint;
- add DTO / Validator / Assembler;
- modify schema/config/pom;
- connect Push;
- connect external channel;
- connect MarketQuote;
- generate candidates;
- generate point values;
- generate final direction;
- create order / execution / auto-trading behavior;
- continue P359 or P360.

Missing config and empty config remain fail-closed.

Display Slots never become Watchlist Pool.

## 5. Tests Added / Strengthened

Added:

```text
src/test/java/org/example/trademodel/controller/RuleControllerTest.java
```

Strengthened:

```text
src/test/java/org/example/trademodel/controller/DashboardControllerTest.java
```

Covered:

- endpoint returns `reviewOnly=true`;
- endpoint returns `displaySlotsAreCandidatePool=false`;
- missing Watchlist config fails closed;
- empty Watchlist config fails closed;
- endpoint does not expose executable/external runtime fields;
- dashboard contains Watchlist Pool vs Display Slots labels;
- dashboard contains default-six-not-candidate-pool copy;
- dashboard contains review-only / no-Push copy.

## 6. Capability-Level Statement

Current level: `REVIEW_ONLY_RUNTIME partial`.

This package moves the Watchlist slice to `REVIEW_ONLY_RUNTIME partial` if verification confirms compile, targeted tests, API smoke, and dashboard smoke.

It remains partial because:

- no audit endpoint is implemented;
- no Push is connected;
- no MarketQuote is connected;
- no candidate generation is connected;
- no point generation is connected.

## 7. Freeze Rule Compliance

- 是否创建新骨架: No
- 是否复用 Cursor-era 资产: Yes
- 是否减少重复: Yes
- 是否提升 capability level: Yes, prepares/implements Watchlist `REVIEW_ONLY_RUNTIME partial` pending verification
- 是否接 service/runtime/dashboard/API: Yes, minimal existing RuleConfig owner-path API + dashboard display only
- 是否符合 #830 审计建议: Yes

## 8. Next Required Action

Next required action:

```text
Minimal Review-Only Watchlist Runtime Wiring Verification
```

Verification must confirm:

- workflow contract;
- compile;
- test-compile;
- targeted controller test;
- targeted dashboard test;
- API smoke for `/api/rule/push-watchlist`;
- dashboard smoke;
- forbidden path check;
- no DTO / Validator / Assembler;
- no Push / MarketQuote / candidate / point / trading semantics.
