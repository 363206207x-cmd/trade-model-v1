# PHASE_P11A_PUSH_RECHECK_NAMING_VERIFICATION

## 1. Verification Purpose

This document records the P11A review for Push / Recheck naming semantics in Trade Model V1.

Review focus:

- `VALID_EXECUTABLE`
- `RECHECK_VALID_EXECUTABLE`
- `valid=true`
- `PASS`
- `successCount`
- `executionStatus`

The goal is to identify naming that may imply executable trading permission, while preserving the current business logic unchanged.

This review does not modify source code, configuration, schema, dashboard, DTO behavior, SourceTrace behavior, BoundaryCandidateService behavior, ExecutionPlan readiness behavior, or RiskActionGuard behavior.

This review does not connect external APIs and does not generate any execution plan or automated trading code.

## 2. Repository Context

Review baseline:

- HEAD: `eb6a3c1 docs(plan): add full project closure summary`
- Scope: Push / Recheck / Watchlist naming, status mapping, scheduler status handling, controller response fields, entity / VO field names, and related tests / docs.

Core boundary:

- BoundaryCandidate `VALID` remains a review candidate.
- ExecutionPlan must not become executable from display-only, DTO-only, missing-source, or Push / Recheck naming state.
- Push / Recheck positive status must not bypass SourceTrace completeness, DerivativesRiskContext, BoundaryCandidateService fallback, ExecutionPlan readiness, RiskActionGuard, or manual review gates.

## 3. Reviewed Files

Code and test files reviewed:

- `src/main/java/org/example/trademodel/enums/RecheckStatusEnum.java`
- `src/main/java/org/example/trademodel/service/PushRecheckStatusContract.java`
- `src/main/java/org/example/trademodel/service/RecheckResult.java`
- `src/main/java/org/example/trademodel/service/RecheckExecutionCommand.java`
- `src/main/java/org/example/trademodel/service/PushRecheckService.java`
- `src/main/java/org/example/trademodel/service/impl/PushRecheckServiceImpl.java`
- `src/main/java/org/example/trademodel/controller/PushRecheckController.java`
- `src/main/java/org/example/trademodel/service/PushRecheckScheduler.java`
- `src/main/java/org/example/trademodel/service/PushSnapshotService.java`
- `src/main/java/org/example/trademodel/entity/TmPushSnapshotDO.java`
- `src/main/java/org/example/trademodel/entity/TmPushRecheckLogDO.java`
- `src/main/java/org/example/trademodel/vo/PushRecheckLogItemVO.java`
- `src/main/java/org/example/trademodel/vo/PushRecheckReplaySummaryVO.java`
- `src/main/java/org/example/trademodel/vo/PushRecheckOpsOverviewVO.java`
- `src/main/java/org/example/trademodel/mapper/PushSnapshotMapper.java`
- `src/main/java/org/example/trademodel/mapper/PushRecheckLogMapper.java`
- `src/test/java/org/example/trademodel/service/impl/PushRecheckServiceImplTest.java`

Related closure documents reviewed by reference:

- `docs/PHASE_FULL_PROJECT_AUDIT.md`
- `docs/PHASE_FULL_PROJECT_CLOSURE_SUMMARY.md`
- `docs/PHASE_P10_SOURCE_ASSEMBLER_SMALL_SCALE_PRODUCTION_VERIFICATION.md`

## 4. Naming Inventory And Risk Review

| Module | Checked Method / Field / Status | Current Semantics | Risk Explanation | Correction Suggestion |
|---|---|---|---|---|
| `RecheckStatusEnum` | `VALID_EXECUTABLE` | Positive recheck status after price drift, account-risk snapshot, confusion score, and feasibility checks pass. | The word `EXECUTABLE` can be misread as permission to create a trade or executable plan. It does not currently prove SourceTrace completeness or RiskActionGuard safe state. | Rename in a future migration to `VALID_REVIEW_READY`, `RECHECK_REVIEW_READY`, or `VALID_ADVISORY_READY`. Keep any backward-compatible mapping explicit during migration. |
| `PushRecheckStatusContract` | `toPushStatus(VALID_EXECUTABLE)` -> `RECHECK_VALID_EXECUTABLE` | Converts recheck enum into push status. | Push status carries the same executable implication and can leak into UI, ops, scheduler, or downstream logic. | Replace future outward-facing status with `RECHECK_VALID_REVIEW_READY` or `RECHECK_REVIEW_READY`. Document legacy value as non-executable until fully migrated. |
| `PushRecheckStatusContract` | `toReviewTag(VALID_EXECUTABLE)` -> `ReviewTag.PASS` | Maps positive recheck to a PASS review tag. | `PASS` may be interpreted as approval to execute rather than review readiness. | Prefer `REVIEW_READY`, `PASS_REVIEW_ONLY`, or a display label that includes manual-review semantics. |
| `PushRecheckStatusContract` | `toReviewTagByPushStatus("RECHECK_VALID_EXECUTABLE")` | Converts persisted push status to review tag `PASS`. | Persisted state can keep executable wording even after source / risk gates become stricter. | Add a documented compatibility mapping: legacy executable wording means review-ready only, not trading-ready. |
| `PushRecheckStatusContract` | `isPendingPushStatusForScheduler(...)` | Scheduler treats only `CAPTURED` and `RECHECK_VALID_WAITING` as pending. | This is safer than auto-looping `RECHECK_VALID_EXECUTABLE`, but the terminal name can still imply readiness. | Keep terminal scheduling behavior, but rename terminal state to review-ready wording. |
| `RecheckResult` | `boolean valid` | Set to `true` when status is `VALID_EXECUTABLE`. | API consumers can read `valid=true` as trade-valid or execution-valid. This bypasses the more precise review-only semantics. | Add or migrate to `reviewReady`, `candidateStillValid`, or `nonExecutableReviewReady`. Avoid `valid=true` as execution signal. |
| `PushRecheckServiceImpl` | `boolean executable = status == VALID_EXECUTABLE` | Internal boolean controls `RecheckResult.valid`. | The variable name `executable` is stronger than current safety contract. It does not check SourceTrace or RiskActionGuard. | Rename future local concept to `reviewReady` or `positiveRecheck`. |
| `PushRecheckServiceImpl` | `result.setValid(executable)` | Exposes positive recheck through `valid=true`. | This is the highest external semantic conflict because controller responses expose it directly. | Keep current behavior unchanged for now, but future API should add an explicit `notTradeInstruction=true` or replace the field with review-only wording. |
| `PushRecheckServiceImpl` | `statusMessage(VALID_EXECUTABLE)` -> Chinese message containing "可执行" | Human-readable positive recheck message. | "可执行" can be understood as execution permission. | Replace future message with "二次确认通过，进入人工复核" or "候选仍有效，非交易指令". |
| `PushRecheckServiceImpl` | no trigger price branch message containing "可执行" | Positive recheck when trigger price is not configured. | Missing trigger price should not produce execution-style wording. | Use review-only wording and explicitly state trigger validation was skipped. |
| `PushRecheckServiceImpl` | `classifyPostPriceChecks(...)` | Returns `VALID_EXECUTABLE` when account-risk snapshot allows, confusion score is below thresholds, and feasibility is high enough. | The method does not read SourceTrace, DerivativesRiskContext, BoundaryCandidateService output, ExecutionPlan readiness, or RiskActionGuard. | Future positive status must require SourceTrace completeness and RiskActionGuard pass, or remain `VALID_WAITING` / `WATCH_ONLY`. |
| `PushRecheckServiceImpl` | `riskNullDoesNotBlock` behavior | Missing account-risk snapshot may still allow positive recheck. | Missing risk context should not support executable wording under P11 safety semantics. | Reclassify missing risk data as `VALID_WAITING`, `RISK_UNKNOWN_WAIT`, `WATCH_ONLY`, or fail-closed review state. |
| `PushRecheckServiceImpl` | `summarizeReplayByDispatch(...).successCount` | Counts `VALID_EXECUTABLE` as success. | `successCount` can be mistaken as trade success or execution success. | Rename future metric to `reviewReadyCount`, `positiveRecheckCount`, or `candidateStillValidCount`. |
| `PushRecheckController` | `GET /api/push/recheck/{pushId}` returns `RecheckResult` | Exposes `recheckStatus`, `valid`, and message to clients. | API response can propagate execution-sounding state into UI or clients. | Document `valid` as review-only immediately; future response should expose `reviewReady` and `notTradeInstruction`. |
| `PushRecheckScheduler` | scheduler only rechecks pending statuses | Does not repeatedly process terminal positive status. | No direct auto-execution path was found here, but source / risk completeness is not part of scheduling criteria. | Keep terminal positive state out of automatic execution. Future scheduler should never treat positive recheck as order permission. |
| `PushSnapshotService` | `decision.isWorthOpening()` gates snapshot creation | Push snapshot may be created from decision and plan state. | The wording `worthOpening` can imply an opportunity when SourceTrace and RiskActionGuard are incomplete. | Future push snapshot creation should include SourceTrace / RiskActionGuard review-only status and avoid opening language in outward display. |
| `PushSnapshotService` | `ensureAccountRiskSnapshot(...)` with unknown exposure fallback | Unknown exposure can become `riskAllowed=true` for snapshot metadata. | Missing exposure should not combine with executable naming. | Unknown exposure should force review-only / waiting status before any positive recheck label is emitted. |
| `PushSnapshotService` | comments mentioning minimal executable schema | Documentation / comment wording around execution schema. | "Executable" wording can be copied into docs or UI and imply trading action. | Rename future comments / docs to "minimal validation schema" or "review schema". |
| `TmPushSnapshotDO` | `pushStatus` | Persists push lifecycle state. | Can persist `RECHECK_VALID_EXECUTABLE`, making old semantic risk durable. | Add migration plan or compatibility note before any future rename. |
| `TmPushRecheckLogDO` | `recheckStatus` | Persists recheck enum status. | Persisted `VALID_EXECUTABLE` has the same ambiguity. | Same migration plan as push status. |
| `TmPushRecheckLogDO` | `executionStatus` | Job execution status for the recheck task. | Field can be confused with trade execution status. | Rename future outward-facing label to `jobStatus`, `taskStatus`, or `recheckJobStatus`. |
| `PushRecheckLogItemVO` | `executionStatus` | Exposes recheck task status. | UI / ops can confuse it with trading execution. | Display as "recheck job status"; avoid pairing it with `VALID_EXECUTABLE` without review-only wording. |
| `PushRecheckReplaySummaryVO` | `successCount` | Replay summary count of positive results. | "Success" may be read as trading success. | Rename display metric to `positiveRecheckCount` or `reviewReadyCount`. |
| `PushRecheckOpsOverviewVO` | recent log `executionStatus` | Operations overview of recheck job status. | Same job-vs-trading execution ambiguity. | Add UI label / field alias clarifying this is task execution status only. |
| `PushSnapshotMapper` | pending status filter | Selects `CAPTURED` and `RECHECK_VALID_WAITING` for scheduler. | Safer because `RECHECK_VALID_EXECUTABLE` is not scheduled as pending. | Keep this behavior; do not add terminal positive status to pending queue. |
| `PushRecheckServiceImplTest` | `validExecutable()` | Expects `VALID_EXECUTABLE`, `valid=true`, and persisted push status. | Tests lock in old executable semantics. | Future rename / semantic migration must update tests to review-only naming and SourceTrace / RiskActionGuard gates. |
| `PushRecheckServiceImplTest` | `riskNullDoesNotBlock()` | Expects missing risk snapshot to allow positive status. | Conflicts with missing-source fail-closed and review-only safety posture. | Future tests should assert missing risk context returns waiting, watch-only, or safe fail-closed state. |
| Closure docs | P0-P10 / closure summary | State that Push / Recheck / Watchlist must remain non-trading and review-only. | Docs are safer than current naming, creating a documentation-code semantic mismatch. | P11 follow-up should align code naming, tests, and API labels with closure docs. |

## 5. Bypass Assessment

No direct order placement, position close, position reverse, or external derivative API connection was found in the reviewed Push / Recheck code path.

However, the current Push / Recheck positive path can emit:

- `VALID_EXECUTABLE`
- `RECHECK_VALID_EXECUTABLE`
- `valid=true`
- `ReviewTag.PASS`
- success-style replay counters
- "可执行" user-facing messages

These outputs are produced without checking all of the following:

- SourceTrace completeness
- DerivativesRiskContext completeness
- BoundaryCandidateService source completeness
- ExecutionPlan readiness
- RiskActionGuard fail-closed state
- manual review gate completion

Therefore, the current issue is primarily a semantic and integration-safety risk rather than an actual trading-action execution path.

Risk level:

| Area | Risk Level | Reason |
|---|---|---|
| Public API response `valid=true` | High | External clients may treat it as execution permission. |
| Enum / persisted status `VALID_EXECUTABLE` | High | Durable status value carries execution semantics. |
| User-facing message "可执行" | High | Strongly implies actionable execution. |
| Scheduler behavior | Medium | Scheduler does not auto-process terminal positive status, but source / risk completeness is still absent. |
| Replay summary `successCount` | Medium | Can be confused with trade success. |
| Entity / VO `executionStatus` | Medium | Represents job execution but can be confused with trade execution. |
| Existing tests | High | Tests enforce the old semantic contract. |

## 6. Required Future Correction Themes

These are recommendations only. No source changes are made by this document.

### 6.1 Rename Positive Recheck Semantics

Future naming should avoid executable language.

Recommended replacements:

- `VALID_EXECUTABLE` -> `VALID_REVIEW_READY`
- `RECHECK_VALID_EXECUTABLE` -> `RECHECK_VALID_REVIEW_READY`
- `valid=true` -> `reviewReady=true` or `candidateStillValid=true`
- `successCount` -> `positiveRecheckCount` or `reviewReadyCount`
- `executionStatus` for jobs -> `recheckJobStatus` or `taskStatus`

### 6.2 Preserve Backward Compatibility During Migration

Because existing persisted status values may contain executable wording, future migration should:

- Continue reading legacy values.
- Map legacy values to review-only semantics.
- Avoid making legacy values trigger any execution gate.
- Add explicit documentation that legacy executable wording is non-trading.

### 6.3 Require SourceTrace And RiskActionGuard Before Any Positive Review State

Future Push / Recheck positive classification should require:

- entry source complete
- stop source complete
- TP source complete
- RR source complete
- liquidity source complete or safe fallback
- multi-timeframe source complete or watch-only fallback
- event window blocker available or safe fallback
- wick confirmation source available or safe fallback
- RiskActionGuard not fail-closed

If any required source is missing, Push / Recheck should return one of:

- `INCOMPLETE`
- `WATCH_ONLY`
- `SAFE_FAIL_CLOSED_ONLY`
- `VALID_WAITING`
- a future explicit `SOURCE_TRACE_INCOMPLETE` status

### 6.4 Rework Missing Risk Snapshot Behavior

Missing account-risk snapshot must not support executable wording.

Future expected behavior:

- Missing risk source -> review-only waiting state
- Unknown exposure -> safe fallback, not positive executable language
- Liquidity / leverage / long-short / event / wick risk missing -> safe fallback or watch-only

### 6.5 Update Tests After Semantic Migration

Existing tests that should be revisited:

- `validExecutable()`
- `validExecutable_shouldUpdatePushStatus()`
- `riskNullDoesNotBlock()`
- replay summary success-count assertions

Future tests should assert:

- missing SourceTrace -> fallback
- missing RiskActionGuard input -> fallback
- complete SourceTrace + safe RiskActionGuard -> review-ready only
- no API field implies trading permission

## 7. Explicit Safety Boundaries

Push / Recheck must continue to respect:

- BoundaryCandidate `VALID` is a review candidate only.
- ExecutionPlan cannot become executable from Push / Recheck naming.
- ExecutionPlan cannot become executable from display-only state.
- ExecutionPlan cannot become executable from DTO-only state.
- ExecutionPlan cannot become executable from missing source.
- High risk does not mean direct stop.
- High risk does not mean direct reverse.
- Wick / spike does not mean trend reversal.
- Stampede state blocks new-open, reverse, and opportunity push semantics.
- Funding, OI, liquidation, leverage, and long-short ratio must not directly imply trading action.

## 8. Non-Goals For This Verification

This document does not:

- Modify Push / Recheck code.
- Modify Push / Recheck tests.
- Modify DTOs.
- Modify SourceTrace.
- Modify DerivativesRiskContext.
- Modify BoundaryCandidateService.
- Modify ExecutionPlan readiness.
- Modify RiskActionGuard.
- Modify schema, dashboard, or config.
- Connect external APIs.
- Generate execution plans.
- Generate automated trading code.
- Stage or commit changes.

## 9. Current Conclusion

Push / Recheck currently has no observed direct trading execution path in the reviewed code.

The main issue is naming and API semantics:

- `VALID_EXECUTABLE`
- `RECHECK_VALID_EXECUTABLE`
- `valid=true`
- `PASS`
- `successCount`
- `executionStatus`
- "可执行" messages

These names can be misunderstood as executable trading permission, and they are not currently gated by full SourceTrace completeness or RiskActionGuard state.

The next recommended step is to create a P11B semantic correction plan before changing code. That plan should define a compatibility-safe rename and migration path from executable wording to review-only wording, then update tests to enforce SourceTrace / RiskActionGuard fallback behavior.
