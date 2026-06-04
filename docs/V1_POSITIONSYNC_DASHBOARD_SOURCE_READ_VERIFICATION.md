# V1 PositionSync / Dashboard Source Read Verification

本文件是 `PositionSync + Dashboard review-only status` 的只读 source-read verification。

本任务只验证现有 Cursor-era 资产是否足够成为第一个最小 review-only runtime wiring 目标；不写 Java，不写 test，不改 schema，不改 config，不改 dashboard，不接 service/runtime/push/trading，不继续 P359/P360，不新增 DTO / Validator / Assembler / Orchestrator。

## 1. Executive Summary

- PositionSync + Dashboard 链路真实存在。现有代码包含 `PositionSyncScheduler` -> `PositionSyncService` -> `PositionProvider` -> `RealPositionMapper` / `tm_real_position`，并通过 `SystemController` 暴露 `/api/system/position-sync-status`。
- runtime input 已存在。默认 `position.provider.type=${POSITION_PROVIDER_TYPE:SIMULATED}`，`SimulatedPositionProvider` 会提供模拟 open position；配置为 `BINANCE` 且凭证存在时，`BinancePositionProvider` 会读取 Binance futures position risk 接口。缺凭证或 provider failure 会 fallback 到 simulated。
- service 已存在。`PositionSyncService` 是 Spring `@Service`，负责同步、upsert、关闭同步快照中消失的 open rows，并维护 `PositionSyncStatusVO`。
- mapper / schema 已存在。`RealPositionMapper` 读写 `tm_real_position`，`schema.sql` 定义了 `tm_real_position` 和 `idx_tm_real_position_symbol_status`。
- dashboard/API surface 已存在但 dashboard 可见性是 partial。`/api/system/position-sync-status` 存在；`dashboard.html` 会 fetch 该 endpoint 并解析 provider status，但未找到实际 `providerStatusValue` DOM 节点，`providerText` / `providerClassName` 也未形成稳定渲染。因此 API yes，dashboard provider/fallback status partial。
- summary/detail 已有 position fields。`DashboardSummaryResponseVO.openPositionCount` 来自 `DecisionService.countOpenPositions()`；`DecisionServiceImpl` 会把 `tm_real_position` OPEN rows 合并到 `DecisionResultVO` 的 position fields；`dashboard.html` 已有持仓中标签和持仓跟踪区域。
- 这条链路接近 REVIEW_ONLY_RUNTIME，但还没有完成 review-only status mapping。下一步需要把 provider status、freshness、fallback、failure、simulated fallback 明确映射为 review-only / incomplete / simulated-fallback UI/API 文案。
- 可以作为第一个最小落地链路。它比 P359/P360 的新 runtime candidate skeleton 更接近用户可见能力，因为它已有输入、service、scheduler、mapper/schema、API、dashboard read path。
- 本任务不需要 Java implementation。本任务只完成 source-read verification；下一步才是 `Minimal Review-Only PositionSync Runtime Wiring Design`。
- 不继续 P359，不启动 P360。
- 本任务不提升 capability level；当前仍是 `DOCS_ONLY_GATE`。它的价值是验证 source readiness，并为下一步最小 wiring design 服务。

## 2. Source Read Inventory

| Area | Files/classes found | Existing behavior | Evidence | Gap |
|---|---|---|---|---|
| PositionSyncService | `src/main/java/org/example/trademodel/service/PositionSyncService.java` | Spring service 调用 provider，读取 open positions，写入 `tm_real_position`，维护 `PositionSyncStatusVO`。 | `syncPositions()` 调用 `positionProvider.fetchOpenPositions()`；调用 `updateOpenPositionBySymbol` / `insertOpenPosition` / `closeMissingOpenPositions`；`getPositionSyncStatus()` 返回 freshness、provider、fallback、sync counters。 | status 还不是明确的 review-only runtime status；failure / stale / fallback 需要更硬的 UI/API mapping。 |
| PositionSyncScheduler | `src/main/java/org/example/trademodel/service/PositionSyncScheduler.java` | Spring scheduler 每 30 秒调用 `positionSyncService.syncPositions()`。 | `@Scheduled(initialDelay = 15000, fixedRate = 30000)`。 | 需要下一步确认配置关闭/异常时 dashboard 是否显示 INCOMPLETE / SIMULATED_FALLBACK，而不是静默。 |
| PositionProvider | `src/main/java/org/example/trademodel/position/PositionProvider.java` | provider interface，提供 `fetchOpenPositions()`。 | `PositionProviderResult fetchOpenPositions()`。 | 需要把 provider result 安全语义标准化为 review-only display。 |
| SwitchablePositionProvider | `src/main/java/org/example/trademodel/position/SwitchablePositionProvider.java` | 根据 `position.provider.type` 在 Binance / simulated 间切换；Binance 缺凭证或失败时 fallback simulated。 | `provider.type=BINANCE but credentials missing` -> fallback；unknown type -> fallback。 | fallback 是好资产，但 dashboard 必须明确提示“当前不是实时 Binance 持仓”。 |
| BinancePositionProvider | `src/main/java/org/example/trademodel/position/BinancePositionProvider.java` | 读取 Binance futures `/fapi/v2/positionRisk`，只转换 open position snapshot。 | 使用 `HttpClient` GET，解析 `positionAmt`、`entryPrice`、`markPrice`、`liquidationPrice`。 | 已有真实 provider，但本任务不接新逻辑；下一步只能设计 review-only status，不扩大 Binance 逻辑。 |
| SimulatedPositionProvider | `src/main/java/org/example/trademodel/position/SimulatedPositionProvider.java` | 默认模拟 provider，返回 BTCUSDT LONG 与 ETHUSDT SHORT 的模拟持仓。 | `return new PositionProviderResult("SIMULATED", "simulated-provider-v1", positions)`。 | 模拟持仓必须在 dashboard/API 明显展示，不能被误认为真实持仓。 |
| PositionProviderResult / PositionSnapshot | `src/main/java/org/example/trademodel/position/PositionProviderResult.java`, `src/main/java/org/example/trademodel/position/PositionSnapshot.java` | carrier 已包含 sourceType、sourceName、configuredProviderType、fallbackOccurred、fallbackReason、openPositions，以及持仓快照字段。 | `PositionProviderResult` 有 fallback fields；`PositionSnapshot` 有 symbol、side、price、quantity、pnl、mark、break-even、liquidation。 | list 未防御性复制不是本次问题；下一步不新增 DTO，只复用现有 VO/result。 |
| RealPositionMapper | `src/main/java/org/example/trademodel/mapper/RealPositionMapper.java` | MyBatis mapper 读 open positions、count、upsert、insert、把缺失 open rows 标记 CLOSED。 | `findOpenPositions()`, `countOpenPositions()`, `updateOpenPositionBySymbol()`, `insertOpenPosition()`, `closeMissingOpenPositions()`。 | `CLOSED` 是同步读模型状态，不等于执行平仓；dashboard copy 必须避免误导。 |
| tm_real_position | `src/main/resources/schema.sql` | 已有真实持仓读模型表。 | `CREATE TABLE IF NOT EXISTS tm_real_position`，含 `source_type`, `source_name`, `position_side`, `position_quantity`, `position_status`, `mark_price`, `liquidation_price`。 | 表名含 real，但默认数据可能来自 simulated provider；UI/API 必须显示 source/fallback。 |
| DashboardController | `src/main/java/org/example/trademodel/controller/DashboardController.java` | 提供 dashboard page、summary、detail；summary 带 openPositionCount；detail 带 decision position fields。 | `/api/dashboard/summary` 设置 `openPositionCount`; `/api/dashboard/detail` 设置 latest decision and display adapters。 | DashboardController 不直接返回 PositionSyncStatus；provider status 由 SystemController endpoint 单独 fetch。 |
| SystemController / position-sync-status endpoint | `src/main/java/org/example/trademodel/controller/SystemController.java` | `/api/system/position-sync-status` 返回 `PositionSyncStatusVO`。 | `@GetMapping("/position-sync-status")` -> `ApiResponse.success(positionSyncService.getPositionSyncStatus())`。 | API 已可用，但需要未来设计 status contract 文案。 |
| DashboardSummaryResponseVO / DashboardDetailResponseVO | `src/main/java/org/example/trademodel/vo/DashboardSummaryResponseVO.java`, `src/main/java/org/example/trademodel/vo/DashboardDetailResponseVO.java`, `src/main/java/org/example/trademodel/vo/DecisionResultVO.java` | summary 有 openPositionCount；detail 包含 decision，position fields 在 `DecisionResultVO`。 | `openPositionCount`; `DecisionResultVO.hasOpenPosition`, `positionSide`, `avgOpenPrice`, `positionQuantity`, `positionStatus`, `markPrice`。 | detail 不包含 explicit provider/fallback status，需要设计是否只用 `/api/system/position-sync-status` 或在 summary/detail 轻量复用。 |
| dashboard.html position display | `src/main/resources/templates/dashboard.html` | 已有持仓中 tile、已开仓监控 shell、持仓跟踪模块、openPositionCount 展示、provider status fetch。 | `fetch("/api/system/position-sync-status")`; `hasRealPositionDetail`; `positionModule`; sidebar “真实持仓”。 | `providerStatusValue` DOM 节点未找到，provider/fallback status 展示 partial；“真实持仓”与 simulated fallback 容易产生歧义。 |

## 3. Existing Runtime Flow

```text
Provider
  -> PositionSyncService
  -> RealPositionMapper / tm_real_position
  -> DecisionServiceImpl read path
  -> DashboardController summary/detail and SystemController status endpoint
  -> dashboard/API position status
```

| Segment | Exists / partial / missing | Real runtime | Simulated fallback | Dashboard/API visible | Review-only safe |
|---|---|---|---|---|---|
| Provider | exists | partial: Binance provider exists behind config and credentials | yes, default SIMULATED | indirect | partial: provider source exists, but UI clarity needs work |
| PositionSyncService | exists | yes, scheduled Spring service | yes, receives fallback result | API via status endpoint | partial: status is read-only, but not yet mapped to explicit REVIEW_ONLY_POSITION_SYNC_READY / INCOMPLETE / BLOCKED_FAIL_CLOSED / SIMULATED_FALLBACK |
| RealPositionMapper / tm_real_position | exists | yes, persistence read model | yes, simulated rows can be persisted | indirect through dashboard decision read model | partial: `CLOSED` row update is read-model sync only and must not be read as execution |
| DecisionServiceImpl read path | exists | yes, merges open rows into `DecisionResultVO` | yes, if simulated rows are persisted | summary/detail | partial: position fields are visible, but provider source/fallback is separate |
| DashboardController / SystemController | exists | yes | yes | `/api/dashboard/summary`, `/api/dashboard/detail`, `/api/system/position-sync-status` | partial: API exists, dashboard provider status display incomplete |
| dashboard.html | partial | yes for fetched API data | yes | partial | partial: many no-trade labels exist, but provider/fallback status needs explicit DOM/copy |

## 4. Safety Boundary

The current selected path must remain read-only:

- It must not call or expose `closePosition`.
- It must not call or expose `reversePosition`.
- It must not call or expose `openPosition`.
- It must not call or expose `placeOrder` / `createOrder` / `submitOrder`.
- It must not provide leverage advice.
- It must not treat position status as a trade suggestion.
- It must not treat Binance provider presence as trading authorization.
- It must show simulated fallback clearly.
- Missing credentials, provider failure, no completed sync, stale sync, or unknown provider status must become `INCOMPLETE` or `SIMULATED_FALLBACK` in future review-only mapping, not silent real-position confidence.
- `RealPositionMapper.closeMissingOpenPositions(...)` is a persistence read-model cleanup for rows missing from the latest snapshot. It is not an exchange close action and must never be described as one.

The source-read grep for service/controller/dashboard found no direct trading action implementation in this selected path. Existing dashboard copy already includes no-order / no-execution / no-auto-trading warnings, but provider/fallback visibility still needs a tighter mapping.

## 5. Dashboard/API Visibility

- `/api/system/position-sync-status` exists: yes. `SystemController.positionSyncStatus()` returns `PositionSyncStatusVO`.
- Dashboard fetches position-sync status: yes. `dashboard.html` calls `fetch("/api/system/position-sync-status")` and maps configured/active/fallback provider states.
- Dashboard visibly displays provider/fallback status: partial. `dashboard.html` references `providerStatusValue`, but source read did not find a corresponding DOM element. `providerText` and `providerClassName` are assigned in `renderLayer1` but are not clearly rendered.
- Dashboard summary has position fields: yes. `DashboardSummaryResponseVO.openPositionCount` is set from `DecisionService.countOpenPositions()`.
- Dashboard detail has position fields: yes. `DecisionServiceImpl` merges `RealPositionVO` into `DecisionResultVO`, and `dashboard.html` uses `hasRealPositionDetail(...)` plus `positionModule(...)`.
- Current display clarity: partial. It displays open-position facts, but it does not yet make provider freshness, fallback, sync success, stale status, or simulated source unmissable.
- Misleading risk: yes. Because default provider is simulated and `tm_real_position` table name / sidebar copy say “真实持仓”, users could think Binance real positions are connected when they are seeing simulated provider rows.
- Next step should be design first, not code: `Minimal Review-Only PositionSync Runtime Wiring Design`. It should decide exact status mapping/copy/API surface before any implementation.

## 6. Review-only Runtime Readiness

| Requirement | Status | Evidence | Gap |
|---|---|---|---|
| real/simulated input source exists | yes | `SimulatedPositionProvider`, `BinancePositionProvider`, `SwitchablePositionProvider`, `application.properties` default SIMULATED | Must make simulated/fallback unambiguous. |
| service exists | yes | `PositionSyncService` with `syncPositions()` and `getPositionSyncStatus()` | Need review-only status labels. |
| persistence exists | yes | `RealPositionMapper`, `tm_real_position` | Must prevent `CLOSED` read-model cleanup from being interpreted as execution. |
| dashboard/API surface exists | partial | `/api/system/position-sync-status`, `/api/dashboard/summary`, `/api/dashboard/detail`, dashboard position modules | API exists; dashboard provider status rendering is incomplete/unclear. |
| fail-closed / fallback visible | partial | `PositionProviderResult.fallbackOccurred`, `fallbackReason`, `freshnessStatus`, `lastSyncSuccess` | Needs explicit mapping to INCOMPLETE / SIMULATED_FALLBACK / BLOCKED_FAIL_CLOSED style status. |
| no trade action | yes for selected path | No order/position mutation endpoint is part of PositionSync/Dashboard source path; dashboard has no-order/no-execution warnings. | Keep this as a hard guard in future design. |
| no new skeleton needed | yes | Existing VO/result/service/controller/dashboard path can be reused. | Future work should not create DTO / Validator / Assembler. |

## 7. Next Step Decision

Decision: **A. 可以进入 Minimal Review-Only PositionSync Runtime Wiring Design**.

Reason:

- Source path is real enough: provider, scheduled service, mapper/schema, API endpoint, dashboard summary/detail read path all exist.
- The remaining problem is not “find more source code”; it is “define the minimal safe mapping and UI/API wording so simulated/fallback/stale states cannot be mistaken for real Binance readiness”.
- This aligns with #830 and the freeze rule because it reuses Cursor-era assets and avoids another Codex skeleton family.

Do not recommend:

- P359;
- P360;
- new DTO;
- new Validator;
- new Assembler;
- Three AI;
- Position Monitor expansion;
- Push;
- point generation;
- order / execution / auto-trading.

## 8. Freeze Rule Compliance

- 是否创建新骨架: No
- 是否复用 Cursor-era 资产: Yes
- 是否减少重复: Yes
- 是否提升 capability level: No, verifies source readiness
- 是否接 service/runtime/dashboard/API: No, source-read only
- 是否符合 #830 审计建议: Yes

## 9. Final Recommendation

PositionSync + Dashboard 是可行的首个 review-only runtime wiring target：它已有输入、service、scheduler、mapper/schema、API 和 dashboard 读路径，但 dashboard provider/fallback 可见性仍是 partial；下一步应做 `Minimal Review-Only PositionSync Runtime Wiring Design`，继续冻结 P359/P360、新骨架、Three AI、Position Monitor expansion、Push、点位生成和 order / execution / auto-trading。
