# V1 Dashboard PositionSync Visual Verification

## 1. Executive Summary

#839 / #840 后的 dashboard visual smoke 通过。`/dashboard` 可以打开，PositionSync 只读状态区域在 dashboard 左侧系统状态区域可见；浏览器滚动到该区域后，当前来源、配置来源、fallback、fallback reason、最近同步成功、freshness、current open position count、最近同步时间、安全文案均真实显示。

本次验证确认当前能力仍是 `REVIEW_ONLY_RUNTIME partial`。它不是 Production Wiring，不是完整 Position Monitor，不是点位、Push、AI，也不是交易能力。

视觉检查未发现 PositionSync 区域内明显重叠、遮挡或挤压。状态文本 `SIMULATED_FALLBACK / 模拟 / fallback 来源` 在侧栏内换行显示，但仍可读。页面明确显示 `只读状态，不是交易建议` 和 `模拟来源不等于真实 Binance 持仓`，没有把 simulated provider 误导成真实 Binance 持仓。

本次验证未发现新增交易动作语义。dashboard 中已有的 `entry / stop / TP / RR`、`auto-trading` 等词只出现在既有禁止/边界文案里，不是操作入口、不是可执行建议。

下一步建议进入 `Source-Owned Runtime vs Existing Point Proposal Merge Map`，继续减少 P1-P359 骨架与既有 Cursor-era runtime/dashboard 资产的重复，而不是恢复 P359/P360。

## 2. Smoke Results

| Check | Result | Evidence |
|---|---|---|
| compile | Passed | `./mvnw -q -DskipTests compile` completed successfully. |
| test-compile | Passed | `./mvnw -q -DskipTests test-compile` completed successfully. |
| DashboardControllerTest | Passed | `./mvnw -q -Dtest=DashboardControllerTest test` completed successfully. |
| workflow contract | Passed | `bash scripts/check-workflow-contract.sh` returned `WORKFLOW_CONTRACT_OK`. |
| `/dashboard` HTTP 200 | Passed | `GET http://localhost:8081/dashboard` returned HTTP `200`. |
| `/api/system/position-sync-status` HTTP 200 | Passed | `GET http://localhost:8081/api/system/position-sync-status` returned HTTP `200` with `configuredProviderType=SIMULATED`, `activeProviderType=SIMULATED`, `freshnessStatus=FRESH`, `lastSyncSuccess=true`, and `currentOpenPositionCount=2`. |
| dashboard visual visible | Passed | Browser opened `http://localhost:8081/dashboard`; PositionSync area was visible after scrolling within the dashboard left system-status area. |
| PositionSync status visible | Passed | Browser DOM check found `providerStatusValue`, `providerActiveValue`, `providerConfiguredValue`, `providerFallbackValue`, `providerFallbackReasonValue`, `providerLastSyncSuccessValue`, `providerFreshnessValue`, `providerOpenCountValue`, and `providerLastSyncTimeValue` visible. |
| safety copy visible | Passed | Browser DOM check found `只读状态，不是交易建议` visible. |
| simulated fallback warning visible | Passed | Browser DOM check found `模拟来源不等于真实 Binance 持仓` visible. |
| no PositionSync element overlap | Passed | Browser bounding-box check for PositionSync fields returned no overlapping element pairs. |

## 3. Visual Findings

The PositionSync display sits in the dashboard left system-status/sidebar area. It is visible after scrolling to the provider status line, and the section starts with:

- `PositionSync`
- `SIMULATED_FALLBACK / 模拟 / fallback 来源`

The visible field values during smoke were:

- 当前来源: `SIMULATED`
- 配置来源: `SIMULATED`
- 是否 fallback: `否`
- fallback reason: `—`
- 最近同步成功: `是`
- freshness: `FRESH`
- current open position count: `2`
- 最近同步时间: `2026-06-04 23:59:49`
- safety copy: `只读状态，不是交易建议`
- simulated warning: `模拟来源不等于真实 Binance 持仓`

The section is clear enough for the current minimal slice. The status label wraps in the narrow sidebar, but it remains readable and does not overlap with following fields. The provider and warning copy make the simulated source explicit, so the UI does not imply that real Binance holdings are connected.

No small layout or copy fix is required before the next planning step.

## 4. Safety Verification

Confirmed for this visual slice:

- No order action.
- No execution action.
- No auto-trading action.
- No close / reverse / open action.
- No entry / stop / TP / RR action.
- No final direction.
- No Push.
- No external channel.
- No new DTO / Validator / Assembler.
- No service / controller / provider / scheduler / mapper / schema / config / pom change.

The grep check found existing negative guardrail text containing `entry / stop / TP / RR` and `auto-trading`; those are prohibitive boundary labels, not action controls and not generated instructions.

## 5. Next Step Decision

Decision: **A. Visual verification passed，进入 Source-Owned Runtime vs Existing Point Proposal Merge Map**.

Reason: the first stop-loss runtime slice is now visible and verified as `REVIEW_ONLY_RUNTIME partial`. Continuing to P359/P360 would reopen the duplicate skeleton path. The safer next move is to reconcile the source-owned runtime / point-proposal skeleton families against existing Cursor-era dashboard/runtime owners, so future work reduces duplication before any new implementation.

Do not continue:

- P359.
- P360.
- new DTO.
- new Validator.
- new Assembler.
- new Orchestrator.
- Three AI.
- Position Monitor expansion.
- Push.
- point generation.
- order / execution / auto-trading.

## 6. Freeze Rule Compliance

- 是否创建新骨架: No
- 是否复用 Cursor-era 资产: Yes
- 是否减少重复: Yes
- 是否提升 capability level: Verification only, confirms `REVIEW_ONLY_RUNTIME partial`
- 是否接 service/runtime/dashboard/API: No new wiring; visual verifies existing dashboard/API slice
- 是否符合 #830 审计建议: Yes
