# V1 Runtime Readiness / System Guardrail Status Source Read

## 1. Current Merged Main

- Current merged main: `b1f9e66 docs(runtime): select runtime readiness guardrail next slice`
- Current module: `Runtime readiness / system guardrail status`
- Current phase: `Source Read`
- Current capability level: `REVIEW_ONLY_RUNTIME partial`
- Completed review-only runtime partial slices: `15`
- Risk: `A`
- Scope: source-read docs and source-of-truth updates only.

This package is source read only. It does not implement runtime readiness / system guardrail status, does not add or change endpoints, does not change dashboard behavior, and does not touch Java business code, tests, schema/config/pom, DTO, Validator, Assembler, Orchestrator, service/domain/mapper/repository ownership, scheduler, collector, API client refresh, recovery, repair, restart, auto-fix, Candidate, Decision generation, Point, final direction, entry/stop/TP/RR, order/execution, auto-trading, Push, external channel, replay, recheck, P359, or P360.

## 2. Files Read

Required docs and source-of-truth files:

- `AGENTS.md`
- `docs/SESSION_BOOTSTRAP.md`
- `docs/ACTIVE_MAINLINE_STATUS.yml`
- `docs/CODEX_NEXT_TASK.yml`
- `docs/V1_PROGRESS_SOURCE_OF_TRUTH.md`
- `docs/V1_CURRENT_STATE.md`
- `docs/PROJECT_PROGRESS_INDEX.md`
- `docs/V1_CAPABILITY_MATRIX.md`
- `docs/V1_MVP_REALITY_ROADMAP.md`
- `docs/V1_DUPLICATE_SKELETON_FREEZE_RULE.md`
- `docs/ANSWER_FORMAT_CONTRACT.md`
- `docs/WORKFLOW_GITHUB_AUTH_AND_HANDOFF_RULE.md`
- `docs/WORKFLOW_COMMAND_AUTOMATION.md`
- `docs/V1_NEXT_MINIMAL_RUNTIME_SLICE_SELECTION_AFTER_REVIEW_ARCHIVE_ANALYTICS_MISSED_OPPORTUNITY_AGGREGATE.md`

Source and test assets read:

- `src/main/java/org/example/trademodel/controller/SystemController.java`
- `src/main/java/org/example/trademodel/service/SystemHealthService.java`
- `src/main/java/org/example/trademodel/service/impl/SystemHealthServiceImpl.java`
- `src/main/java/org/example/trademodel/service/RunBaselineService.java`
- `src/main/java/org/example/trademodel/service/impl/RunBaselineServiceImpl.java`
- `src/main/java/org/example/trademodel/service/RuntimeMetricService.java`
- `src/main/java/org/example/trademodel/service/PositionSyncService.java`
- `src/main/java/org/example/trademodel/vo/RunBaselineVO.java`
- `src/main/java/org/example/trademodel/vo/DashboardSummaryResponseVO.java`
- `src/main/java/org/example/trademodel/vo/LightSystemStatusVO.java`
- `src/main/java/org/example/trademodel/controller/DashboardController.java`
- `src/main/resources/templates/dashboard.html`
- `src/main/java/org/example/trademodel/mapper/MonitorAlertMapper.java`
- `src/main/java/org/example/trademodel/mapper/AnalysisRunMapper.java`
- `src/main/java/org/example/trademodel/mapper/PushRecheckLogMapper.java`
- `src/main/java/org/example/trademodel/mapper/HotResetEventMapper.java`
- `src/test/java/org/example/trademodel/controller/DashboardControllerTest.java`

## 3. Existing Assets Found

| Asset | Evidence | Reuse potential | Boundary note |
|---|---|---|---|
| `SystemController` | Exposes `GET /api/system/health`, `GET /api/system/position-sync-status`, and `GET /api/system/run-baseline`. | Strong existing owner path for system/runtime status source read and future design. | `/health` currently returns a static liveness string; `/run-baseline` is richer but broader. |
| `SystemHealthService` / `SystemHealthServiceImpl` | Builds a `Map<String,Object>` with CPU, memory, database status/detail, scheduler status/detail. | Strong read-only system health source. | Scheduler status is observed from PositionSync timestamps; it must not trigger scheduler behavior. |
| Database probe | `probeDatabase()` uses `dataSource.getConnection()` and `connection.isValid(...)`, returning `UP`, `DOWN`, or `ERROR`. | Useful readiness/guardrail source. | Probe failure can map to fail-closed; it is not repair/recovery. |
| Scheduler probe | `probeScheduler()` derives `NO_RECENT_ACTIVITY`, `STALE`, or `RUNNING` from PositionSync last start/end timestamps. | Useful operational guardrail source. | `RUNNING` must mean observed recent activity only, not authorization to run scheduler. |
| `RunBaselineService` / `RunBaselineServiceImpl` | Builds a `RunBaselineVO` with system health, PositionSync availability, performance metrics, alert summary, data quality summary, recheck counts, and hot reset summary. | Strong read-only aggregate owner path. | Recheck and hot reset sections are sensitive context only; future design must prevent execution/recovery semantics. |
| `RuntimeMetricService` | `snapshot()` exposes in-process metric snapshots; `recordDuration(...)` mutates in-memory counters. | `snapshot()` is useful read-only metadata. | Future implementation must use snapshot/read semantics only and must not create collector/performance automation. |
| `DashboardController` summary path | `/api/dashboard/summary` includes `systemStatus`, `systemHealth`, alerts, decisions, open position count, and records dashboard summary duration. | Existing dashboard API source for system surfaces. | It records runtime metric duration today; source read does not change this behavior. |
| `DashboardSummaryResponseVO` | Carries `LightSystemStatusVO systemStatus` and `Map<String,Object> systemHealth`. | Existing response carrier for system status/health. | Not a dedicated readiness contract. |
| `LightSystemStatusVO` | Contains status, monitored coins, latest decision time, missed opportunity count, confused count, pending count, reverse signal count, and hot reset fields. | Useful operational context. | Hot reset fields are context-only and must not become recovery/restart/auto-fix actions. |
| `dashboard.html` system surfaces | Top runtime pill, sidebar system status card, KPI cards, `Runtime readiness` workbench metric, `apiStatusLine`, `mapSchedulerStatusDisplay`, `renderLayer1`, and `renderSidebarPanel`. | Existing dashboard surface for future minimal status copy/DOM design. | No dedicated runtime readiness / guardrail status panel was found. |
| Existing tests | `DashboardControllerTest` covers summary system status fields, refresh legacy contract, `systemHealth` map exposure, and dashboard template safety patterns. | Future targeted tests can extend existing owner-path tests. | No dedicated `SystemController` or run-baseline source-read test was found in current inventory. |

## 4. Runtime Readiness / Guardrail State Inventory

Current states already visible in existing code:

- Database health: `UP`, `DOWN`, `ERROR`.
- Scheduler observation: `RUNNING`, `STALE`, `NO_RECENT_ACTIVITY`.
- PositionSync availability: `FRESH`, `STALE`, `UNKNOWN`.
- Runtime metric sample boundary: `hasSamples=true/false`, `totalSampleCount`, `sampleBoundaryDetail`.
- Alert aggregate context: open count, suppressed count, suppression ratios.
- Data-quality aggregate context: analysis count, low-quality count, low-quality ratio, low-quality threshold.
- Recheck aggregate context: counts by `RecheckStatusEnum`, read from `PushRecheckLogMapper`.
- Hot reset aggregate context: event counts, trigger-type counts, and latest hot reset fields.

These states can support a future read-only readiness / guardrail status, but the source read found no single dedicated status contract that already distinguishes operational readiness from executable readiness.

## 5. Dashboard / API Surface Inventory

Existing API surfaces:

- `GET /api/system/health`: static liveness response.
- `GET /api/system/position-sync-status`: existing PositionSync read-only status endpoint.
- `GET /api/system/run-baseline`: existing runtime baseline aggregate endpoint.
- `GET /api/dashboard/summary`: existing dashboard summary endpoint with `systemStatus` and `systemHealth`.

Existing dashboard surfaces:

- Top runtime pill: `runtimeDot` / `runtimeText`.
- Sidebar system card: `sidebarSystemStatus`, with PositionSync provider status DOM ids.
- KPI cards: `cardRisk`, `cardDataQuality`, `cardHotReset`, and related `kpi-*` ids.
- Workbench metric label: `Runtime readiness`.
- Existing helpers: `apiStatusLine(health)` and `mapSchedulerStatusDisplay(health)`.

Dedicated runtime readiness / system guardrail status panel:

- Not found.

## 6. Existing Safety Semantics

The codebase already uses related safety language and behavior:

- Dashboard copy repeatedly marks status surfaces as review-only / not executable in neighboring slices.
- `SystemHealthServiceImpl` returns error/down/stale/no-activity states rather than attempting repair.
- `RunBaselineVO.PerformanceSummary.sampleBoundaryDetail` explicitly states runtime metrics are in-process snapshots, not full observability.
- Existing dashboard workbench copy says `ExecutionPlan readiness is not automatic execution`.
- Existing PositionSync status copy says status is read-only and not trading advice.

This is enough to proceed to design, but the future status mapping must add explicit safety fields rather than relying on nearby copy.

## 7. Refresh / Scheduler / Collector Boundary

Current observed sources are read or in-memory observation paths:

- `SystemHealthServiceImpl` observes DB connectivity and PositionSync timestamps.
- `RunBaselineServiceImpl` reads mapper counts and in-memory metric snapshots.
- `RuntimeMetricService.snapshot()` reads the current metric map.

Sensitive boundaries:

- `PositionSyncService.syncPositions()` exists and writes real position state, but this source read did not call it and future runtime readiness status must not trigger it.
- Existing schedulers such as Push/Recheck and market data schedulers exist elsewhere in the codebase; this slice must not start or invoke them.
- Existing mapper insert/update methods exist for alerts, recheck logs, hot reset events, and real positions; they are not part of the future read-only status path.

Future design must require `notSchedulerTrigger=true`, `notCollectorTrigger=true`, `notApiClientRefresh=true`, and `notExternalRefresh=true`.

## 8. Recovery / Repair / Restart / Auto-Fix Boundary

No safe recovery / repair / restart / auto-fix owner path was found for this slice.

Hot reset appears in `LightSystemStatusVO` and `RunBaselineVO.HotResetSummary` as status/history context. It must stay context-only and must not become:

- recovery action;
- repair action;
- restart action;
- auto-fix action;
- executable readiness;
- trading authorization.

Future design must require `notRecoveryAction=true`, `notRepairAction=true`, `notRestartAction=true`, and `notAutoFix=true`.

## 9. Candidate / Decision / Point / Trading Boundary

The runtime readiness / guardrail status can read operational health and aggregate status only. It must not produce or imply:

- Candidate generation or ranking;
- Decision generation;
- Point generation;
- final direction;
- entry / stop / TP / RR;
- trading signal;
- executable readiness;
- trading authorization;
- order / execution / auto-trading;
- Push send or external channel.

Future design must treat "ready" as "ready for manual review of system status" only, not ready to trade.

## 10. Gaps

- No dedicated runtime readiness / system guardrail status endpoint was found.
- No dedicated runtime readiness / system guardrail dashboard panel was found.
- Existing `GET /api/system/health` is a static liveness string, not the richer `SystemHealthService` map.
- Existing `GET /api/system/run-baseline` is broad and includes recheck/hot reset context that needs strict boundary copy.
- Existing dashboard "Runtime readiness" is tied to per-symbol runtime kline context, not system-level readiness.
- No dedicated `SystemController` / run-baseline controller test was found in current source inventory.
- Existing safety semantics are distributed across nearby slices and need a future minimal explicit status contract.

## 11. Design Risk Notes

- "Readiness" is a high-risk word. Future design must rename or qualify it as review-only operational readiness / system guardrail status.
- `RUNNING` scheduler status is an observation of recent activity. It must not authorize scheduler triggering.
- Hot reset and recovery-like language must remain historical/status context only.
- Recheck summary counts must remain counts only, not recheck execution.
- PositionSync status is useful evidence but must not execute sync or real position monitoring.
- Runtime metrics are in-process observations only, not collector or performance automation.
- Any future endpoint should either reuse `/api/system/run-baseline` / `/api/dashboard/summary` or be at most one minimal read-only Map endpoint if the readiness gate approves.
- Future implementation must not add DTO / Validator / Assembler / Orchestrator, schema/config/pom, or a new service/domain/mapper/repository ownership family unless a later approved readiness gate changes scope.

## 12. Source Read Result

Result: `GO to design only`.

Reason:

- Existing owner paths are sufficient for design: `SystemController`, `SystemHealthService`, `RunBaselineService`, `RunBaselineVO`, `RuntimeMetricService`, `DashboardSummaryResponseVO`, and dashboard system surfaces.
- Missing assets are designable as status contract and dashboard/API handoff gaps.
- No implementation is allowed or performed in this package.

## 13. Next Allowed Action

Next allowed action:

`Minimal Review-Only Runtime Readiness / System Guardrail Status Runtime Wiring Design`

Next branch:

`minimal-review-only-runtime-readiness-system-guardrail-status-runtime-wiring-design`

Next risk:

`A`

Next package must remain design docs and source-of-truth updates only.

## 14. Freeze-Rule Compliance

- 是否创建新骨架: No
- 是否复用 Cursor-era / V1 资产: Yes, existing `SystemController`, `SystemHealthService`, `RunBaselineService`, `RuntimeMetricService`, dashboard summary/system surfaces, and related tests are inventoried.
- 是否减少重复: Yes, by selecting existing system/runtime owner paths instead of creating a new readiness owner family.
- 是否提升 capability level: No
- 是否接 service/runtime/dashboard/API: No, this package is source read only; it inventories existing service/runtime/dashboard/API paths.
- 是否符合 #830 审计建议: Yes
