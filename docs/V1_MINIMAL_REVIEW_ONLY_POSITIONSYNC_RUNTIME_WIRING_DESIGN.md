# Minimal Review-Only PositionSync Runtime Wiring Design

This document is a wiring design only. It does not implement Java, tests, dashboard changes, runtime wiring, provider changes, push, external channels, point generation, final direction, order execution, or auto-trading.

## 1. Executive Summary

- 本任务只设计，不实现。
- Selected target 是 `PositionSync + Dashboard review-only status`。
- 不新增 DTO / Validator / Assembler。
- 不新增 service。
- 不新增 endpoint，未来最小实现优先复用 `/api/system/position-sync-status`。
- 不改 provider 逻辑，不接新的 Binance / OKX / Bybit 逻辑。
- 不接交易，不生成 close / reverse / open / order / execution 语义。
- 未来最小实现目标是复用现有 `PositionSyncService`、`PositionSyncStatusVO`、`SystemController.positionSyncStatus`、`DashboardController` / `dashboard.html` 的已有位置，只把 provider / fallback / simulated / stale / failure 状态显示成明确的只读状态。
- 当前 capability level 不提升，仍是 `DOCS_ONLY_GATE`。

这一步的价值不是“又开一个包”，而是把下一个可做的最小 Java / dashboard 改动边界钉死，避免回到 P359 / P360 或新骨架循环。

## 2. Existing Owner Path

Fixed owner path:

```text
PositionProvider / SwitchablePositionProvider / BinancePositionProvider / SimulatedPositionProvider
  -> PositionSyncService
  -> PositionSyncStatusVO
  -> SystemController /api/system/position-sync-status
  -> dashboard.html provider/fallback/sync display
```

Future implementation must not bypass this owner path.

- Do not add a parallel DTO.
- Do not add a parallel status endpoint.
- Do not add a parallel service.
- Do not route position sync status into point, candidate, push, AI, final direction, or trading paths.
- Do not treat `tm_real_position` as proof of live Binance state unless provider / fallback state is explicitly visible.

Existing owner evidence:

- `PositionSyncService.getPositionSyncStatus()` returns a `PositionSyncStatusVO`.
- `PositionSyncStatusVO` already carries `configuredProviderType`, `activeProviderType`, `activeProviderName`, `fallbackOccurred`, `fallbackReason`, `lastSyncStartTime`, `lastSyncEndTime`, `lastSyncSuccess`, `lastSyncMessage`, `lastFetchedOpenCount`, `lastUpsertedCount`, `lastClosedCount`, `currentOpenPositionCount`, `freshnessStatus`, `freshnessDetail`, and `staleThresholdMinutes`.
- `SystemController.positionSyncStatus()` already exposes `/api/system/position-sync-status`.
- `dashboard.html` already fetches `/api/system/position-sync-status`, computes `providerText` / `providerClassName`, and attempts to update `providerStatusValue`.
- Source-read verification found provider / fallback dashboard visibility is partial because the computed status is not yet clearly rendered in the existing page.

## 3. Minimal Future Status Mapping

Allowed review-only statuses:

- `REVIEW_ONLY_POSITION_SYNC_READY`
- `SIMULATED_FALLBACK`
- `INCOMPLETE`
- `BLOCKED_FAIL_CLOSED`

The mapping should use existing fields first:

- `configuredProviderType`
- `activeProviderType`
- `fallbackOccurred`
- `fallbackReason`
- `lastSyncSuccess`
- `freshnessStatus`
- `currentOpenPositionCount`
- provider availability / credential state only if already represented by existing provider / fallback state
- `lastSyncEndTime` as the effective last sync timestamp

Minimal mapping rules:

| Rule | Review-only status | Meaning |
|---|---|---|
| `activeProviderType=BINANCE`, `lastSyncSuccess=true`, `freshnessStatus=FRESH`, and `fallbackOccurred` is not true | `REVIEW_ONLY_POSITION_SYNC_READY` | Binance provider status is fresh enough for read-only position status display only. |
| `fallbackOccurred=true` or `activeProviderType=SIMULATED` | `SIMULATED_FALLBACK` | Dashboard must state that simulated fallback / simulated source is not real Binance holdings. |
| `lastSyncSuccess=false`, `lastSyncSuccess` missing, `freshnessStatus=STALE`, or `freshnessStatus=UNKNOWN` | `INCOMPLETE` | Status cannot be silently treated as usable runtime state. |
| Provider state is contradictory, misleading, unsafe, or claims real provider readiness while fallback/source fields disagree | `BLOCKED_FAIL_CLOSED` | Fail closed rather than presenting ambiguous position state. |

Do not add fields unless the next implementation readiness gate proves the existing fields are insufficient.

## 4. Dashboard/API Minimal Surface

Future implementation should reuse:

- `/api/system/position-sync-status`
- the existing dashboard provider/status area
- the existing position module

Minimal display copy should include:

- 当前来源：`BINANCE` / `SIMULATED`
- 配置来源：`BINANCE` / `SIMULATED`
- 是否 fallback：是 / 否
- fallback reason
- 最近同步是否成功
- freshness status
- open position count
- 明确标签：只读状态，不是交易建议
- 明确标签：模拟来源不等于真实 Binance 持仓

Dashboard boundary:

- If `providerStatusValue` DOM is missing, the future minimum may add or repair only that display slot.
- Do not do a broad dashboard expansion.
- Do not add complex new cards.
- Do not trigger push.
- Do not create a new endpoint.
- Do not display any close / reverse / open / leverage / order suggestion.

## 5. Safety Boundary

The PositionSync status display must stay read-only.

- 不允许 `closePosition`。
- 不允许 `reversePosition`。
- 不允许 `openPosition`。
- 不允许 `placeOrder` / `createOrder` / `submitOrder`。
- 不允许 leverage advice。
- 不允许 position sync status 变成交易建议。
- 不允许 simulated fallback 被当作真实持仓。
- 不允许 Binance provider presence 被当作交易授权。
- `RealPositionMapper.closeMissingOpenPositions` 只是 read-model cleanup，用于把同步快照中消失的 `OPEN` row 标为 `CLOSED`，不是交易平仓。

## 6. Minimal Future Implementation Boundary

If future work enters Java / dashboard implementation, it should be limited to:

- reuse `PositionSyncStatusVO`;
- reuse `SystemController.positionSyncStatus`;
- reuse `PositionSyncService.getPositionSyncStatus`;
- reuse the existing dashboard fetch of `/api/system/position-sync-status`;
- only supplement provider / fallback / freshness / status review-only copy and display;
- no new DTO / Validator / Assembler;
- no new service;
- no new endpoint;
- no provider change;
- no scheduler change;
- no mapper change;
- no schema change;
- no trading-related change.

The future implementation must not claim `PRODUCTION_WIRING`. At most, it can move the selected slice toward partial `REVIEW_ONLY_RUNTIME` when dashboard/API visibly shows the safe status.

## 7. Implementation Readiness Checklist

The next readiness gate must answer these before any implementation:

- Is `PositionSyncStatusVO` sufficient for the display without adding fields?
- Does `/api/system/position-sync-status` return every required field in normal, stale, failure, and simulated fallback states?
- Does `dashboard.html` have a real DOM slot for provider/fallback/freshness display?
- If the DOM slot is missing, where is the smallest existing provider/status area to repair?
- Should the implementation be only copy/status mapping, or does it require a minimal dashboard DOM fix?
- What targeted tests are needed: controller status response, dashboard static token/display check, or service status mapping?
- Which forbidden tokens must remain absent from new display/copy: close, reverse, open, order, execution, leverage advice, auto-trading.
- Smoke verification commands should include `bash scripts/check-workflow-contract.sh`, targeted Java tests if touched, `git diff --check`, and a dashboard/API smoke check if the future package changes dashboard/API.
- No service test should be added unless service code changes are necessary.
- No dashboard test should be added unless dashboard copy / DOM changes are made.

## 8. Capability-Level Movement

- Current level: `DOCS_ONLY_GATE`.
- This package raises capability level: No.
- Next implementation readiness gate raises capability level: No, prepares.
- Future minimal implementation, if it only repairs dashboard/status mapping over existing runtime owner path, can target partial `REVIEW_ONLY_RUNTIME`.
- It must not be described as `PRODUCTION_WIRING`.

## 9. Freeze Rule Compliance

- 是否创建新骨架: No
- 是否复用 Cursor-era 资产: Yes
- 是否减少重复: Yes
- 是否提升 capability level: No
- 是否接 service/runtime/dashboard/API: No, design only
- 是否符合 #830 审计建议: Yes

## 10. Final Recommendation

下一步应该进入 `Minimal Review-Only PositionSync Runtime Wiring Implementation Readiness Gate`，先证明现有 `PositionSyncStatusVO`、`/api/system/position-sync-status`、dashboard DOM slot 和测试边界足够，再决定是否做最小实现。

继续冻结 P359 / P360、新 DTO、新 Validator、新 Assembler、新 Orchestrator、Three AI、Position Monitor expansion、Push、point generation、external channel、order / execution / auto-trading。

这不是 P359/P360，因为它不再制造 source-owned runtime candidate skeleton；它只围绕已经存在的 Cursor-era PositionSync owner path 设计一个最小只读状态展示接线。
