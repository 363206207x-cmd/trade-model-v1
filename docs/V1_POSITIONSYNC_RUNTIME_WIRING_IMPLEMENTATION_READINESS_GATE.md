# V1 PositionSync Runtime Wiring Implementation Readiness Gate

## 1. Executive Summary

本任务只做 implementation readiness gate，不做实现。

结论：GO，可以进入 `Minimal Review-Only PositionSync Runtime Wiring Implementation`，但下一步实现必须限制为最小 dashboard 状态展示 / copy mapping，不允许改 service / provider / scheduler / mapper / schema，不允许新增 endpoint，不允许新增 DTO / Validator / Assembler。

现有 `PositionSyncStatusVO` 字段足够支撑最小 review-only PositionSync 状态展示：它已经包含 configured provider、active provider、fallback、fallback reason、last sync success、freshness、open position count、last sync time 等字段。`/api/system/position-sync-status` 也已经由 `SystemController` 复用 `PositionSyncService.getPositionSyncStatus()` 返回该 VO，endpoint 足够，不需要新增 endpoint。

Dashboard 目前是 partial：`dashboard.html` 已经 fetch `/api/system/position-sync-status`，也有 provider status mapping 逻辑，但 `providerStatusValue` 只是被 JS 查询，实际 DOM slot 未被确认存在；`providerText` / `providerClassName` 已计算，但展示不够清晰。下一步最小改动应只补一个现有 dashboard provider / position 区域里的状态行和文案映射。

本任务不提升 capability level，仍是 `DOCS_ONLY_GATE`。它的价值是确认现有 Cursor-era PositionSync / Dashboard owner path 足够进入最小实现，并阻止继续 P359 / P360 或新增骨架。

## 2. Field Sufficiency Check

| Required Display | Existing source field | Enough? | Notes |
|---|---|---|---|
| 当前来源 | `activeProviderType`, `activeProviderName` | Yes | 可显示当前实际使用来源，例如 BINANCE / SIMULATED。 |
| 配置来源 | `configuredProviderType` | Yes | 可显示用户配置的 provider，与 active provider 区分。 |
| 是否 fallback | `fallbackOccurred` | Yes | 可直接映射为“是否 fallback”。 |
| fallback reason | `fallbackReason` | Yes | 可直接展示 fallback 原因。 |
| 最近同步是否成功 | `lastSyncSuccess`, `lastSyncMessage` | Yes | 可显示最近同步状态和说明。 |
| freshness status | `freshnessStatus`, `freshnessDetail`, `staleThresholdMinutes` | Yes | 可区分 fresh / stale / unknown 类状态。 |
| open position count | `currentOpenPositionCount` | Yes | Dashboard 已有 open position count 展示路径。 |
| 最近同步时间 | `lastSyncEndTime`, `lastSyncStartTime` | Yes | 最小展示优先用 `lastSyncEndTime`。 |
| 是否 simulated | `activeProviderType`, `configuredProviderType`, `fallbackOccurred` | Yes | `activeProviderType=SIMULATED` 或 fallback 到 SIMULATED 时必须明确展示。 |
| 是否 Binance | `activeProviderType`, `configuredProviderType` | Yes | 只能表示来源状态，不表示交易授权。 |
| 只读标签 | Static dashboard copy | Yes | 不需要新增字段；dashboard 文案即可固定“只读状态”。 |
| 不是交易建议标签 | Static dashboard copy | Yes | 不需要新增字段；dashboard 文案即可固定“不是交易建议”。 |

字段结论：现有字段足够。下一步不得为了这条链路新增 DTO、status object、Validator、Assembler 或 endpoint。

## 3. Endpoint Sufficiency Check

`/api/system/position-sync-status` 已经存在，并通过 `SystemController` 返回 `PositionSyncService.getPositionSyncStatus()` 的结果。

结论：

- endpoint 已经返回最小实现需要的字段。
- 不需要新增 endpoint。
- 不需要修改 `SystemController`。
- 不需要修改 `PositionSyncService`。
- 不需要修改 `PositionSyncStatusVO`。
- 可以只复用现有 endpoint。

下一步实现如果发现 endpoint 字段不够并必须新增 DTO 或 endpoint，应立即 NO-GO，退回更小 source read，而不是扩大实现范围。

## 4. Dashboard DOM / Copy Check

Dashboard 已有以下基础：

- `dashboard.html` 已 fetch `/api/system/position-sync-status`。
- `resolvePositionProviderStatus(payload)` 已读取 `configuredProviderType`、`activeProviderType`、`fallbackOccurred`、`fallbackReason`。
- `updateProviderStatusDisplay()` 已尝试查找 `providerStatusValue`。
- `providerText` / `providerClassName` 已在 dashboard 渲染逻辑中被计算。
- `openPositionCount` 和 position module 已存在。

缺口：

- `providerStatusValue` 的实际 DOM slot 未被确认存在。
- `providerText` / `providerClassName` 的实际可见渲染不够清晰。
- 当前 position 文案仍可能让用户误以为“真实持仓”已接通，尤其在 simulated fallback 时有误导风险。

最小改动点：

- 在现有 position / provider 区域补一个小状态行或修复 `providerStatusValue` slot。
- 显示当前来源、配置来源、fallback、fallback reason、lastSyncSuccess、freshnessStatus、currentOpenPositionCount。
- 增加固定标签：“只读状态，不是交易建议”。
- 增加固定标签：“模拟来源不等于真实 Binance 持仓”。

不需要大改 dashboard，不增加复杂卡片，不做 dashboard expansion，不触发 push。

## 5. Minimal Implementation Scope If Allowed

如果下一步进入 GO implementation，允许范围只能是：

- 修改 `src/main/resources/templates/dashboard.html`。
- 可选修改已有 dashboard static test，例如 `src/test/java/org/example/trademodel/controller/DashboardControllerTest.java`，前提是实现确实改了 dashboard 文案或 DOM slot。
- 可选最小同步 `docs/ACTIVE_MAINLINE_STATUS.yml` / source-of-truth 状态文档。

下一步 implementation 不允许：

- 修改 Java service / controller / provider / scheduler / mapper / schema。
- 修改 `PositionSyncStatusVO`。
- 新增 DTO / Validator / Assembler。
- 新增 endpoint。
- 新增 service。
- 新增 provider。
- 接 Binance 新逻辑。
- 接 push / external channel。
- 生成 point / candidate / final direction。
- 生成或暗示任何交易动作。

未来最小实现目标：

- 显示当前来源。
- 显示配置来源。
- 显示 fallback。
- 显示 fallback reason。
- 显示 lastSyncSuccess。
- 显示 freshnessStatus。
- 显示 currentOpenPositionCount。
- 显示“只读状态，不是交易建议”。
- 显示“模拟来源不等于真实 Binance 持仓”。
- 避免“真实持仓”误导，尤其是 simulated fallback 场景。

## 6. Required Tests / Checks For Future Implementation

下一步 implementation 至少需要运行：

- `bash scripts/check-workflow-contract.sh`
- `./mvnw -q -DskipTests compile`
- `./mvnw -q -DskipTests test-compile`
- 如果存在 `DashboardControllerTest` 或 dashboard static test，运行对应 targeted test。
- grep forbidden tokens，确认 no order / no execution / no auto-trading。
- dashboard smoke：打开 `/dashboard`。
- API smoke：GET `/api/system/position-sync-status`。
- 确认 no P359 / no P360。
- 确认 no new DTO / Validator / Assembler。

未来 smoke 必须确认 dashboard 不是在暗示真实 Binance 持仓、交易建议、平仓、反手、开仓或杠杆建议。

## 7. No-Go Conditions

出现以下任一情况，下一步不能进入 implementation，必须退回 source read 或更小 dashboard DOM audit：

- endpoint 字段不够且必须新增 DTO。
- dashboard 没有可安全插入位置。
- 需要改 service / provider / scheduler / mapper / schema。
- 需要新增 status object。
- 需要新增 endpoint。
- 需要接 Binance 新逻辑。
- 需要解释持仓建议。
- 需要生成任何交易动作。
- 会造成 simulated fallback 被误认为真实持仓。
- 会扩大 dashboard 重构范围。

## 8. Go / No-Go Decision

GO: `Minimal Review-Only PositionSync Runtime Wiring Implementation`

理由：

- 现有 `PositionSyncStatusVO` 字段足够。
- 现有 `/api/system/position-sync-status` endpoint 足够。
- 现有 PositionSync service/provider/mapper/schema path 足够，不需要改动。
- 缺口集中在 dashboard DOM / copy 可见性，属于最小 dashboard status mapping 范围。
- 下一步可以不新增 DTO / Validator / Assembler，不继续 P359 / P360，不引入 candidate / point / push / trading。

GO 不等于 Production Wiring。它只允许把现有 runtime 状态以 review-only、manual-review-required、not-trade-instruction 的方式更清楚地显示出来。

## 9. Freeze Rule Compliance

- 是否创建新骨架: No
- 是否复用 Cursor-era 资产: Yes
- 是否减少重复: Yes
- 是否提升 capability level: No, readiness gate only
- 是否接 service/runtime/dashboard/API: No, readiness only
- 是否符合 #830 审计建议: Yes

## 10. Final Recommendation

允许进入最小 implementation，但只允许修 `dashboard.html` 的 PositionSync provider/fallback/freshness/status 展示与必要的已有 dashboard static test；禁止改 service / controller / provider / scheduler / mapper / schema，禁止新增 DTO / Validator / Assembler，P359 / P360、Three AI、Position Monitor expansion、Push、point generation、order / execution / auto-trading 继续冻结。这不是 P359/P360，因为它不新增 source-owned runtime candidate 骨架，而是盘活已有 Cursor-era PositionSync + Dashboard owner path。
