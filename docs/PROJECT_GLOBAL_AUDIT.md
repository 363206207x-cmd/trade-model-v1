# Project Global Audit

Task: P0-0 Global Repository Audit Evidence + Contract Reconciliation Draft
Baseline: `origin/main`
Actual merged-main HEAD: `c2f1bbb72d4c284bd1de19b47a98fdc872b73e02` (`c2f1bbb`)
Target branch: `p0-0-global-audit-contract-reconciliation`
Target worktree: `/Users/xuchao/Documents/trade-model-v1-p0-0-audit`
Source worktree: `/Users/xuchao/Documents/trade-model-v1`
Audit status: DRAFT / NOT MERGED
P0-0 status at closure-readiness: DONE candidate / PENDING_MERGED_MAIN

---

## 1. Scope Guard

This audit draft does not modify Java, tests, schema, application config, pom, Dashboard, PR #1004, or the source Dashboard Draft worktree.
No files are staged, committed, pushed, deleted, or merged by this task.

Open PR #1004 is treated only as current blocking evidence supplied by the task. This task does not inspect, update, close, or merge it.

---

## 2. Baseline Evidence

- `git rev-parse HEAD`: `c2f1bbb72d4c284bd1de19b47a98fdc872b73e02`
- `git rev-parse origin/main`: `c2f1bbb72d4c284bd1de19b47a98fdc872b73e02`
- Baseline Maven: `./mvnw test -q` passed before writing this draft.
- Source P0-0 draft files existed before this task:
  - `docs/PROJECT_DELIVERY_CONTRACT.md`
  - `docs/PROJECT_CURRENT_STATE.md`
  - `docs/DELIVERY_PROGRESS_MATRIX.md`
  - `docs/CODEX_TASK_TEMPLATE.md`
  - `docs/CONTRACT_CHANGE_LOG.md`
  - `docs/DEAD_CODE_CANDIDATES.md`
  - `AGENTS.md`

---

## 3. New / Old Source-of-Truth Conflict

Current repository automation and legacy workflow files still read historical V1 facts:

- `AGENTS.md` requires old files such as `docs/SESSION_BOOTSTRAP.md`, `docs/ACTIVE_MAINLINE_STATUS.yml`, `docs/V1_PROGRESS_SOURCE_OF_TRUTH.md`, and workflow docs.
- `docs/ACTIVE_MAINLINE_STATUS.yml` still references a Recheck verification mainline and stale current head context.
- `docs/CODEX_NEXT_TASK.yml` still references the old next task flow.
- `scripts/v1-state.sh` prints `ACTIVE_MAINLINE_STATUS.yml`, `CODEX_NEXT_TASK.yml`, `V1_CAPABILITY_MATRIX.md`, and `V1_PROGRESS_SOURCE_OF_TRUTH.md`.
- `scripts/v1-auto.sh` invokes `scripts/v1-state.sh` and old workflow guards.
- `scripts/codex-next-task.sh` reads `docs/CODEX_NEXT_TASK.yml` and `docs/ACTIVE_MAINLINE_STATUS.yml`.
- `scripts/check-workflow-contract.sh` validates legacy workflow files, not the new delivery contract files.
- `.github/workflows/workflow-contract.yml` runs `bash scripts/check-workflow-contract.sh`.

Reconciliation rule in this draft:

`PROJECT_DELIVERY_CONTRACT.md -> DELIVERY_PROGRESS_MATRIX.md -> PROJECT_CURRENT_STATE.md -> machine-readable compatibility files -> historical V1 docs`.

`ACTIVE_MAINLINE_STATUS.yml` and `CODEX_NEXT_TASK.yml` are compatibility files only until migrated.

---

## 4. Automation Currently Reads

Observed automation inputs:

- `scripts/v1-state.sh`: `docs/ACTIVE_MAINLINE_STATUS.yml`, `docs/CODEX_NEXT_TASK.yml`, `docs/V1_CAPABILITY_MATRIX.md`, `docs/V1_PROGRESS_SOURCE_OF_TRUTH.md`, `docs/WORKFLOW_COMMAND_AUTOMATION.md`.
- `scripts/v1-auto.sh`: `scripts/v1-state.sh`, `scripts/check-workflow-contract.sh`, GitHub/gh helper flow.
- `scripts/codex-next-task.sh`: `docs/CODEX_NEXT_TASK.yml`, `docs/ACTIVE_MAINLINE_STATUS.yml`.
- `scripts/check-workflow-contract.sh`: old workflow files and script contracts.
- GitHub workflow `workflow-contract.yml`: runs `scripts/check-workflow-contract.sh`.

These must be migrated in a later P0-0 task. This task only records the migration requirement.

---

## 5. P0-P3 Phase Evidence Matrix

| Phase | Module | Phase Status | Existing Module Maturity | Evidence Summary |
|---|---|---|---|---|
| P0-0 | Contract Lock + Baseline + Dead Code Candidate Report | IN_PROGRESS | PARTIAL | Draft governance docs now exist on this branch; legacy workflow files still need migration. |
| P0-1 | UserPosition | NOT_STARTED | NONE | No UserPosition/tm_user_position/CreateUserPositionReq/CloseUserPositionReq evidence. |
| P0-2 | ExecutionPlan Source Gate | NOT_STARTED | PARTIAL | ExecutionPlan/BoundaryCandidate/SourceTrace assets exist; exact SourceGate classes not found. |
| P0-3 | AccountRisk integrates UserPosition | NOT_STARTED | PARTIAL | AccountRisk snapshot exists; UserPosition integration missing. |
| P0-4 | PositionMonitorLog | NOT_STARTED | NONE | No tm_position_monitor_log / PositionMonitorLog evidence. |
| P0-5 | PositionMonitorService | NOT_STARTED | PARTIAL | PositionSync/tm_real_position exist; no UserPosition monitor service closure. |
| P0-6 | Review integrates UserPosition | NOT_STARTED | PARTIAL | Review aggregate exists; no UserPosition execution-deviation closure. |
| P1-1 | PushRecheck semantic hardening | NOT_STARTED | PARTIAL | PushRecheck chain exists with POST/replay/config write risk nearby. |
| P1-2 | ConfusedState + AiConflict hardening | NOT_STARTED | PARTIAL | Heuristic services and fields exist; hardening rules not complete. |
| P1-3 | HotReset real action | NOT_STARTED | PARTIAL | HotReset event/write assets exist; true invalidation/rebuild action incomplete. |
| P1-4 | OpportunityLog | NOT_STARTED | PARTIAL | MissedOpportunity exists; full OpportunityLog table/service/outcomes missing. |
| P2-1 | Macro / News / External Context | NOT_STARTED | NONE | Exact macro/news/external context code/schema evidence not found. |
| P2-2 | AI Orchestrator + AiCallLog | NOT_STARTED | PARTIAL | AiCallLogDO and AI role strings exist; no provider orchestration/table found. |
| P2-3 | Scheduler / Idempotency / Trace | NOT_STARTED | PARTIAL | Schedulers/requestId/traceId exist; full idempotency/orchestrator missing. |
| P3-1 | Dashboard Final | NOT_STARTED | PARTIAL | Dashboard exists; final semantic alignment depends on P0/P1 business closures. |
| P3-2 | Full E2E Acceptance | NOT_STARTED | NONE | No full business E2E evidence; production readiness blocked. |
| P3-3 | Final Delivery Docs | NOT_STARTED | PARTIAL | Historical docs exist; final docs cannot be complete before business chain. |
| P3-2A | Production Deployment Readiness | BLOCKED | NONE | H2 memory DB, empty DB password, H2 console, no prod profile/auth/migration/rollback/smoke evidence. |

---

## 6. Required Semantic Separation

The following meanings are mandatory and must not be collapsed:

- `execution_plan` = system suggestion / advisory plan. It is not proof that a user opened a position.
- `triggered` = event/state-machine state. It is not an opened position.
- `tm_real_position` = provider observation/read model. It may be populated by simulated provider and is not manual user truth.
- `UserPosition` = manual user truth. It does not currently exist and must not be faked from `execution_plan`, `triggered`, or `tm_real_position`.

---

## 7. Existing Assets and Gaps by Phase

### P0-1 UserPosition

Existing assets: none found by exact scan for `UserPosition`, `tm_user_position`, `CreateUserPositionReq`, `CloseUserPositionReq`.
Missing assets: schema, DO, Mapper, DTO, Service, Controller, Req/VO/Enums, tests.
Duplicate risk: high if future code reuses `tm_real_position` as manual user truth.
Downstream dependencies: AccountRisk, PositionMonitorLog, PositionMonitorService, Review.
Safety risk: execution_plan / triggered / tm_real_position may be misread as opened positions.
Evidence files: scan output over `src/main/java`, `src/test/java`, `src/main/resources`, `docs`.

### P0-2 ExecutionPlan Source Gate

Existing assets: `ExecutionPlanDO`, `ExecutionPlanMapper`, `PlanService`, `BoundaryCandidateService`, SourceTrace ownership services and validators.
Missing assets: exact `ExecutionPlanSourceGate`, `BoundaryCandidateSourceGate`, `NumericBoundarySourceValidator`; source gate tests for every numeric boundary.
Duplicate risk: Candidate/Point wrappers overlap with BoundaryCandidate/ExecutionPlan.
Downstream dependencies: Dashboard Final, PushRecheck, Review.
Safety risk: source-less VALID or DTO.valid bypass.
Evidence files: `src/main/java/org/example/trademodel/service/BoundaryCandidateService.java`, `src/main/java/org/example/trademodel/mapper/ExecutionPlanMapper.java`, `src/main/resources/schema.sql`, SourceTrace services/tests.

### P0-3 AccountRisk integrates UserPosition

Existing assets: `tm_account_risk_snapshot`, `TmAccountRiskSnapshotDO`, `AccountRiskSnapshotMapper`, `PushSnapshotService.ensureAccountRiskSnapshot`, dashboard account risk status.
Missing assets: UserPositionRiskAdapter and calculations over manual open/closed positions.
Duplicate risk: account snapshot read-only status mistaken for trading authorization or position sizing.
Downstream dependencies: PositionMonitor, PushRecheck, Dashboard Final.
Safety risk: risk result must not auto-reduce/close/reverse.
Evidence files: `src/main/resources/schema.sql`, `src/main/java/org/example/trademodel/mapper/AccountRiskSnapshotMapper.java`, `src/main/java/org/example/trademodel/service/PushSnapshotService.java`, `src/test/java/...AccountRiskJsonTest.java`.

### P0-4 PositionMonitorLog

Existing assets: none found for `tm_position_monitor_log` / `PositionMonitorLog`.
Missing assets: all phase-required files.
Duplicate risk: monitor evidence could be incorrectly stored only in review/archive displays.
Downstream dependencies: PositionMonitorService, Review.
Safety risk: without logs, review cannot prove warnings or execution deviation.
Evidence files: exact scan produced no code evidence.

### P0-5 PositionMonitorService

Existing assets: PositionSync provider/read model, `tm_real_position`, `RealPositionMapper`, `/api/system/position-sync-status`.
Missing assets: UserPosition-based PositionMonitorService, monitor log write, no auto-close/reverse/order tests.
Duplicate risk: PositionSync may be mistaken as PositionMonitor.
Downstream dependencies: Review integrates UserPosition.
Safety risk: simulated provider rows can look like real positions if labels are weak.
Evidence files: `src/main/java/org/example/trademodel/service/PositionSyncService.java`, `src/main/java/org/example/trademodel/mapper/RealPositionMapper.java`, `src/main/resources/schema.sql`, `src/main/resources/application.properties`.

### P0-6 Review integrates UserPosition

Existing assets: ReviewController, ReviewAggregateService, ReviewResultMapper, review page, missed/recheck archive displays.
Missing assets: UserPositionReviewAdapter and manual execution-deviation review.
Duplicate risk: review-only archive may be mistaken for full user-position review.
Downstream dependencies: Full E2E and Dashboard Final.
Safety risk: no proof whether system was wrong or user execution deviated.
Evidence files: `src/main/java/org/example/trademodel/controller/ReviewController.java`, `src/main/java/org/example/trademodel/service/ReviewAggregateService.java`, `src/main/resources/static/js/review-page.js`.

### P1-1 PushRecheck semantic hardening

Existing assets: PushRecheck controller/service/scheduler/dispatch config, `tm_push_snapshot`, `tm_push_recheck_log`, `PushRecheckStatusContract`, tests.
Missing assets: hardening that guarantees no status implies trading authorization and cannot create UserPosition.
Duplicate risk: Recheck preview/status can be mistaken as authorization.
Downstream dependencies: AccountRisk, UserPosition, Dashboard Final.
Safety risk: POST `/api/push/recheck/:pushId` and `/api/push/recheck/replay` are real action paths; status projections must not call them.
Evidence files: `src/main/java/org/example/trademodel/controller/PushRecheckController.java`, `src/main/java/org/example/trademodel/service/PushRecheckService.java`, `src/main/java/org/example/trademodel/mapper/PushRecheckLogMapper.java`, tests.

### P1-2 ConfusedState + AiConflict hardening

Existing assets: `AiConflictResolverService`, `ConfusedStateService`, `DecisionEngineService`, decision fields `ai_conflict_*`, `confused_score`.
Missing assets: contract hardening thresholds and proof that AI cannot override rules or force infinite wait.
Duplicate risk: AI status projection duplicate with DecisionResult status.
Downstream dependencies: PushRecheck, Dashboard Final, AI Orchestrator.
Safety risk: AI role strings can be misread as actual provider arbitration.
Evidence files: `src/main/java/org/example/trademodel/service/DecisionEngineService.java`, `src/main/java/org/example/trademodel/service/AiConflictResolverService.java`, `src/main/resources/schema.sql`.

### P1-3 HotReset real action

Existing assets: `HotResetService`, `HotResetServiceImpl`, `AssetStateService.recordHotResetEvent`, `tm_hot_reset_event`, dashboard read-only status.
Missing assets: true invalidation of old candidate/waiting/triggered plan, needs_revalidation, analysis rebuild, account/confused recalculation.
Duplicate risk: Hot Reset event source status can be mistaken for real reset action.
Downstream dependencies: Decision, ExecutionPlan, Review.
Safety risk: extreme market stale plans may remain valid-looking.
Evidence files: `src/main/java/org/example/trademodel/service/HotResetService.java`, `src/main/java/org/example/trademodel/mapper/HotResetEventMapper.java`, `src/main/resources/schema.sql`.

### P1-4 OpportunityLog

Existing assets: `MissedOpportunityService`, `MissedOpportunityController`, `MissedOpportunityMapper`, `tm_missed_opportunity`.
Missing assets: `tm_opportunity_log`, OpportunityLog outcomes, MFE/MAE, target/invalidation, opportunity stats.
Duplicate risk: MissedOpportunity archive mistaken for full OpportunityLog.
Downstream dependencies: Review, Full E2E.
Safety risk: survivorship bias remains without full opportunity outcomes.
Evidence files: `src/main/java/org/example/trademodel/service/MissedOpportunityService.java`, `src/main/java/org/example/trademodel/controller/MissedOpportunityController.java`, `src/main/resources/schema.sql`.

### P2-1 Macro / News / External Context

Existing assets: none found by exact scan.
Missing assets: tables, services, controllers, external context evidence builder, decision/monitor integration, tests.
Duplicate risk: HotReset EventImpact status may be mistaken for broad macro/news context.
Downstream dependencies: Evidence, Decision, ExecutionPlan, PositionMonitor.
Safety risk: event windows cannot block plans without traceable source.
Evidence files: exact scan for `MacroEvent`, `NewsEvent`, `ExternalContext`, `tm_macro_event`, `tm_news_event` returned no matches.

### P2-2 AI Orchestrator + AiCallLog

Existing assets: `AiCallLogDO`, heuristic AI role strings in DecisionEngineService, AI conflict services.
Missing assets: `tm_ai_call_log`, AiDecisionOrchestratorService, provider clients, budget/cache/rate limit/fallback tests.
Duplicate risk: heuristic role names look like real GPT/Gemini/Grok provider orchestration.
Downstream dependencies: Decision, Review, Full E2E.
Safety risk: provider integration must not bypass rule layer.
Evidence files: `src/main/java/org/example/trademodel/entity/AiCallLogDO.java`, `src/main/java/org/example/trademodel/service/DecisionEngineService.java`.

### P2-3 Scheduler / Idempotency / Trace

Existing assets: AnalysisSchedulerService, PushRecheckScheduler, MarketDataScheduler, requestId/traceId fields.
Missing assets: AnalysisRunOrchestrator, idempotency guard, duplicate/concurrent/failure recovery tests.
Duplicate risk: low-frequency scan schedulers can be mistaken for full analysis orchestration.
Downstream dependencies: Full E2E and production readiness.
Safety risk: duplicate analysis runs and untraceable writes.
Evidence files: `src/main/java/org/example/trademodel/service/AnalysisSchedulerService.java`, `src/main/java/org/example/trademodel/service/PushRecheckScheduler.java`, DTO request fields.

### P3-1 Dashboard Final

Existing assets: Dashboard template, many review-only status panels, DashboardController endpoints/tests.
Missing assets: final semantic separation backed by UserPosition and PositionMonitor; no final UI should precede stable business semantics.
Duplicate risk: status wall / review-only panels can be mistaken for business completion.
Downstream dependencies: P0/P1/P2 phases.
Safety risk: execution_plan/triggered/real_position can be misread if dashboard finalizes too early.
Evidence files: `src/main/resources/templates/dashboard.html`, `src/main/java/org/example/trademodel/controller/DashboardController.java`, `src/test/java/org/example/trademodel/controller/DashboardControllerTest.java`.

---

## 8. Deployment Blockers

Production Deployment Readiness: BLOCKED.

Evidence:

- H2 memory database: `src/main/resources/application.yml` has `jdbc:h2:mem:trade_model_v1`.
- Empty database password: `src/main/resources/application.yml` has blank password for `sa`.
- H2 console enabled: `src/main/resources/application.yml` and `src/main/resources/application.properties` enable H2 console.
- No production profile evidence found in this pass.
- No authentication/authorization evidence found for operational/write APIs in this pass.
- No secret management evidence beyond environment placeholders in this pass.
- No migration/rollback evidence found in this pass.
- No deployment smoke/rollback pipeline evidence found in this pass.
- Simulated position provider default: `src/main/resources/application.properties` has `position.provider.type=${POSITION_PROVIDER_TYPE:SIMULATED}`.

---

## 9. Automation Migration Requirements

The next P0-0 subtask must update these files to read the Delivery Contract / Progress Matrix first and treat legacy files as derived compatibility files:

1. `scripts/check-workflow-contract.sh`: add contract/current-state/matrix/template/audit checks and status vocabulary validation.
2. `scripts/v1-state.sh`: print Contract, Matrix, Current State, and Global Audit before legacy V1 files.
3. `scripts/v1-auto.sh`: refuse business-package automation while matrix current phase is not DONE.
4. `scripts/codex-next-task.sh`: derive next task from Matrix/Current State first.
5. `docs/ACTIVE_MAINLINE_STATUS.yml`: convert to compatibility output derived from matrix/current state.
6. `docs/CODEX_NEXT_TASK.yml`: convert to compatibility output derived from matrix/current state and block P0-1 while P0-0 is IN_PROGRESS.
7. `docs/SESSION_BOOTSTRAP.md`: add Contract/Matrix/Current State boot order.
8. `docs/ANSWER_FORMAT_CONTRACT.md`: add Phase Status / Existing Module Maturity / merged-main effectivity fields.
9. `docs/WORKFLOW_COMMAND_AUTOMATION.md`: replace legacy-only command authority with contract-first workflow.

This task does not modify those files.

---

## 10. No Deletion Conclusion

This audit does not recommend deleting any file.
`docs/DEAD_CODE_CANDIDATES.md` remains an evidence ledger only.
Ambiguous review-only / placeholder / duplicate / no-op / preview assets are DEFER by default until a future task proves LOW risk and not needed by future contract phases.


---

## 10. P0-0 Workflow Migration Draft Evidence

This draft migrates governance automation to the following source-of-truth priority:

1. `docs/PROJECT_DELIVERY_CONTRACT.md`
2. `docs/DELIVERY_PROGRESS_MATRIX.md`
3. `docs/PROJECT_CURRENT_STATE.md`
4. `docs/ACTIVE_MAINLINE_STATUS.yml` and `docs/CODEX_NEXT_TASK.yml` as `DERIVED_ONLY` compatibility mirrors
5. Legacy V1 documents as historical audit and asset evidence only

Updated automation draft:

- `scripts/check-workflow-contract.sh` validates contract files, matrix/current-state sync, derived compatibility status, and P0-0 gate blocking P0-1.
- `scripts/v1-state.sh` emits contract-first phase, maturity, work package, next-business gate, production deployment readiness, and blockers.
- `scripts/v1-auto.sh` renders contract-first status and summary and delegates task generation to `scripts/codex-next-task.sh`.
- `scripts/codex-next-task.sh` validates P0-0 task handoff and blocks P0-1 generation while P0-0 is not DONE.

P0-0 remains IN_PROGRESS in this draft. P0-1 UserPosition remains NOT_STARTED and is not allowed.

No deletion conclusion is made in this draft.


---

## 11. P0-0 Closure Readiness Candidate Evidence

P0-0 Done Criteria were rechecked on 2026-06-21 in the target worktree.

Passed evidence:

- PROJECT_DELIVERY_CONTRACT.md exists and is ACTIVE.
- PROJECT_CURRENT_STATE.md exists and records DONE candidate / PENDING_MERGED_MAIN.
- DELIVERY_PROGRESS_MATRIX.md exists and marks P0-0 DONE candidate while keeping P0-1 NOT_STARTED / NONE.
- CODEX_TASK_TEMPLATE.md exists.
- CONTRACT_CHANGE_LOG.md exists.
- DEAD_CODE_CANDIDATES.md exists and contains no proven DELETE recommendation.
- PROJECT_GLOBAL_AUDIT.md exists.
- AGENTS.md references the contract.
- Workflow automation migrated to contract / matrix / current-state priority.
- Maven test passed in the closure-readiness task.
- No Java, tests, schema, config, pom, Dashboard, or business implementation file was changed.
- No files were deleted.

Effective-state warning:

This is not effective project completion until a later, separately authorized P0-0 package commit is merged to `main`.
P0-1 UserPosition remains blocked.
PR #1004 remains unrelated and was not touched.
