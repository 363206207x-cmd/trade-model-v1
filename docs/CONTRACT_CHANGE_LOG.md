# Contract Change Log

## v1.0

Initial active project delivery contract.

Rules:

1. PROJECT_DELIVERY_CONTRACT.md cannot be silently changed.
2. Any contract change must be recorded here.
3. Any change that modifies phase order requires explicit human confirmation.
4. Any change that modifies Done Criteria requires explicit human confirmation.
5. Any change that weakens safety boundaries requires explicit human confirmation.

---

## Change Template

Date:
Changed by:
Reason:
Before:
After:
Does this change phase order:
Does this change done criteria:
Does this weaken safety boundaries:
Human confirmation required:


---

## v1.0-p0-0-reconciliation-draft

Date: 2026-06-20
Changed by: Codex
Reason: P0-0 Global Repository Audit Evidence + Contract Reconciliation Draft.
Before: Local P0-0 draft did not explicitly separate Phase Status from Existing Module Maturity and did not mark machine-readable legacy files as compatibility-only.
After: Draft adds P0-0 governance exception, Phase Status vs Existing Module Maturity, merged-main effectivity, fact-source priority, controlled emergency exception, production deployment readiness blockers, and automation migration list.
Does this change phase order: No.
Does this change done criteria: Clarifies only; business Done Criteria remain strict.
Does this weaken safety boundaries: No.
Human confirmation required: Yes before merging to main.


---

## v1.0-p0-0-workflow-migration-draft

Date: 2026-06-20
Changed by: Codex
Reason: P0-0 Contract Workflow Migration draft.
Before: Workflow scripts and compatibility files could still use legacy V1 review-only runtime facts as the current-task driver.
After: Workflow scripts and compatibility files are migrated to Project Delivery Contract / Delivery Progress Matrix / Project Current State priority, with ACTIVE_MAINLINE_STATUS and CODEX_NEXT_TASK marked DERIVED_ONLY.
Does this change phase order: No.
Does this change done criteria: No.
Does this weaken safety boundaries: No.
Human confirmation required: Yes before P0-0 closure.


---

## v1.0-p0-0-closure-readiness-candidate

Date: 2026-06-21
Changed by: Codex
Reason: P0-0 Contract Delivery Closure Readiness.
Before: P0-0 workflow migration draft remained IN_PROGRESS.
After: P0-0 is marked DONE candidate with Completion Effective State = PENDING_MERGED_MAIN; P0-1 remains blocked until the package is separately staged, committed, pushed, reviewed, merged to main, main is synced, and the worktree is clean.
Does this change phase order: No.
Does this change done criteria: Strengthens P0-0 governance criteria by explicitly requiring PROJECT_GLOBAL_AUDIT.md and migrated workflow automation.
Does this weaken safety boundaries: No.
Human confirmation required: Yes before staging / committing / pushing / PR creation.

---

## v1.0-p0-1-user-position-done-candidate

Date: 2026-06-22
Changed by: Codex
Reason: P0-1 UserPosition Manual Workflow B-risk implementation package.
Before: P0-1 UserPosition was NOT_STARTED with missing schema, DO, Mapper, DTO, Service, Controller, Req/VO/Enums, and tests.
After: P0-1 is a branch DONE candidate with manual-only UserPosition persistence, manual open / manual close APIs, open-position query excluding CLOSED rows, fixed safety fields, fail-closed validation, and tests. It is effective only after reviewed merge to clean / synced main.
Does this change phase order: No.
Does this change done criteria: No.
Does this weaken safety boundaries: No.
Human confirmation required: Yes before B-risk PR merge.

---

## v1.0-p0-2-execution-plan-source-gate-done-candidate

Date: 2026-06-22
Changed by: Codex
Reason: P0-2 ExecutionPlan Source Gate B-risk implementation package.
Before: P0-2 ExecutionPlan Source Gate was NOT_STARTED/PARTIAL with existing SourceTrace and BoundaryCandidate assets but no exact ExecutionPlanSourceGate, BoundaryCandidateSourceGate, or NumericBoundarySourceValidator.
After: P0-2 is a branch DONE candidate with ExecutionPlan Source Gate validation, BoundaryCandidate DTO.valid gate enforcement, numeric boundary source validation, fail-closed handling for missing evidence/fallback/incomplete/review-only/AI-only/numeric-without-source outputs, safety fields, schema fields, and tests. It is effective only after reviewed merge to clean / synced main.
Does this change phase order: No.
Does this change done criteria: No.
Does this weaken safety boundaries: No.
Human confirmation required: Yes before B-risk PR merge.

---

## v1.0-gh-open-pr-gate-fix

Date: 2026-06-22
Changed by: Codex
Reason: Resolve GH_NOT_AVAILABLE gate after P0-2 merge.
Before: Runtime state could keep `GH_NOT_AVAILABLE` or stale local-main sync blockers even when `gh CLI` was available, PR #1008 was merged, HEAD matched `origin/main`, and open PR count was zero.
After: Runtime state reports open PR check source/count/status, treats `gh CLI` open PR count `0` as `OPEN_PR_STATUS=NONE`, keeps fail-closed behavior when `gh` is unavailable or open PRs exist, and accepts clean HEAD==origin/main verification as clean/synced main without weakening merged-main effectivity.
Does this change phase order: No.
Does this change done criteria: No.
Does this weaken safety boundaries: No.
Human confirmation required: No; A-risk workflow gate repair only.

---

## v1.0-p0-3-account-risk-user-position-done-candidate

Date: 2026-06-22
Changed by: Codex
Reason: P0-3 AccountRisk integrates UserPosition B-risk implementation package.
Before: P0-3 AccountRisk had snapshot/read assets, but did not consume UserPosition as the manual position fact source and did not provide open / partially closed / closed UserPosition risk semantics.
After: P0-3 is a branch DONE candidate with UserPositionRiskAdapter, read-only AccountRisk API, OPEN / PARTIALLY_CLOSED inclusion, CLOSED exclusion, leverage / position size / concentration / conservative directional correlation / drawdown-or-VaR proxy risk, high-risk blocking, PushRecheck read-only consumption, safety fields, and tests. It is effective only after reviewed merge to clean / synced main.
Does this change phase order: No.
Does this change done criteria: No.
Does this weaken safety boundaries: No.
Human confirmation required: Yes before B-risk PR merge.

---

## v1.0-p0-4-position-monitor-log-done-candidate

Date: 2026-06-22
Changed by: Codex
Reason: P0-4 PositionMonitorLog B-risk implementation package.
Before: P0-4 PositionMonitorLog was NOT_STARTED with no `tm_position_monitor_log`, DO, DTO, Mapper, Service, Review query path, or monitor-log tests.
After: P0-4 is a branch DONE candidate with monitor-log persistence, one-log-per-recordMonitorRun service behavior, immutable read queries, Review read-only monitor-log query path, safety fields, forbidden action guardrails, and tests for normal, weakened, invalidated, and high-risk scenarios. It is effective only after reviewed merge to clean / synced main.
Does this change phase order: No.
Does this change done criteria: No.
Does this weaken safety boundaries: No.
Human confirmation required: Yes before B-risk PR merge.
