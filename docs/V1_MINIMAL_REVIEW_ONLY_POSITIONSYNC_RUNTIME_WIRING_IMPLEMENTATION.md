# V1 Minimal Review-Only PositionSync Runtime Wiring Implementation

This document records the minimal implementation that makes the existing PositionSync runtime status visible on the dashboard.

It does not add Java service code, controller code, provider code, scheduler code, mapper code, schema, config, endpoint, DTO, Validator, Assembler, Orchestrator, MarketQuoteClient wiring, push, external channel, executable point generation, final direction, order execution, or auto-trading.

## 1. Scope

Implemented scope:

- Reuses existing `/api/system/position-sync-status`.
- Reuses existing `PositionSyncStatusVO` fields.
- Reuses existing `dashboard.html` fetch path.
- Adds a minimal dashboard status display inside the existing system status sidebar.
- Adds a static dashboard test assertion so the PositionSync display slot and safety copy cannot silently disappear.

Changed implementation files:

- `src/main/resources/templates/dashboard.html`
- `src/test/java/org/example/trademodel/controller/DashboardControllerTest.java`

## 2. Dashboard Status Display

The dashboard now displays:

- `PositionSync` review-only status.
- 当前来源: `activeProviderType`.
- 配置来源: `configuredProviderType`.
- 是否 fallback: `fallbackOccurred`.
- fallback reason: `fallbackReason`.
- 最近同步成功: `lastSyncSuccess`.
- freshness: `freshnessStatus`.
- 持仓读模型数量: `currentOpenPositionCount`.
- 最近同步时间: `lastSyncEndTime` or `lastSyncStartTime`.
- Fixed safety label: `只读状态，不是交易建议`.
- Fixed simulation boundary label: `模拟来源不等于真实 Binance 持仓`.

## 3. Status Mapping

Allowed status mapping:

| Condition | Dashboard status |
|---|---|
| `activeProviderType=BINANCE`, `lastSyncSuccess=true`, `freshnessStatus=FRESH`, and `fallbackOccurred` is not true | `REVIEW_ONLY_POSITION_SYNC_READY` |
| `fallbackOccurred=true` or `activeProviderType=SIMULATED` | `SIMULATED_FALLBACK` |
| `lastSyncSuccess=false`, missing sync success, `freshnessStatus=STALE`, or `freshnessStatus=UNKNOWN` | `INCOMPLETE` |
| provider state contradiction or misleading provider state | `BLOCKED_FAIL_CLOSED` |

`REVIEW_ONLY_POSITION_SYNC_READY` only means the dashboard can show a read-only position sync status. It is not a trade suggestion, not a position-management command, not push, not point generation, and not trading authorization.

## 4. Safety Boundary

The implementation keeps these boundaries:

- No new DTO / Validator / Assembler / Orchestrator.
- No new endpoint.
- No service / controller / provider / scheduler / mapper / schema change.
- No Binance / OKX / Bybit new logic.
- No MarketQuoteClient wiring.
- No push or external channel.
- No entry / stop / TP / RR generation.
- No final direction.
- No order / execution / auto-trading.
- No P359 / P360 continuation.

## 5. Capability-Level Movement

- Previous selected slice level: `DOCS_ONLY_GATE`.
- Implementation result: `REVIEW_ONLY_RUNTIME partial`.
- Why partial: the dashboard now uses an existing runtime API path and existing runtime fields to display a non-executable, review-only PositionSync status, but this is not Production Wiring and does not change backend runtime behavior.
- It does not prove real Binance holdings are connected. Simulated / fallback states are explicitly labeled.

## 6. Next Required Action

Next allowed action:

- `Minimal Review-Only PositionSync Runtime Wiring Verification`

Verification should confirm:

- dashboard template renders the provider/fallback/freshness status slot;
- targeted dashboard test passes;
- compile and test-compile pass;
- dashboard smoke can load `/dashboard`;
- API smoke can GET `/api/system/position-sync-status`;
- no service/controller/provider/scheduler/mapper/schema/config/pom changes occurred;
- no order / execution / auto-trading semantics were introduced.
