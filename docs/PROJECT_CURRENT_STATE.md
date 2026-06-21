# Trade Model V1 Current State

Contract: docs/PROJECT_DELIVERY_CONTRACT.md
Contract Version: v1.0
Current Phase: P0-0 Contract Lock + Baseline + Dead Code Candidate Report
Current Phase Status: DONE
Completion Effective State: derived by v1 state runtime
Existing Module Maturity: PARTIAL
Current Work Package: P0-0 Contract Delivery Package and Merge
Next Business Phase: P0-1 UserPosition
Next Business Phase Allowed: NO
Production Deployment Readiness: BLOCKED

---

## Effective State Rule

P0-0 is only a local branch DONE candidate in this worktree.
It is not effective project completion until the commit containing this state is merged to `main`, local `main` is synced, and the worktree is clean.

P0-1 UserPosition remains blocked until `bash scripts/v1-state.sh` reports `COMPLETION_EFFECTIVE_STATE: EFFECTIVE_MERGED_MAIN` and the next-business-phase gate allows it.

---

## Current Allowed Work

Only the following work is allowed after this closure-readiness update:

1. Separately authorized stage / commit / push for the P0-0 contract delivery package.
2. Separately authorized A-risk PR creation for the P0-0 contract delivery package.
3. Review, checks, and merge of that P0-0 PR.
4. Main sync after merge.

No business module implementation is allowed in this phase.

---

## Current Forbidden Work

The following work is blocked until P0-0 is effective on merged main:

1. UserPosition implementation.
2. ExecutionPlan Source Gate implementation.
3. AccountRisk UserPosition integration.
4. PositionMonitorLog implementation.
5. PositionMonitorService implementation.
6. Review UserPosition integration.
7. PushRecheck semantic hardening.
8. ConfusedState + AiConflict hardening.
9. HotReset real action.
10. OpportunityLog.
11. Macro / News / External Context.
12. AI Orchestrator + AiCallLog.
13. Scheduler / Idempotency / Trace.
14. Dashboard Final.
15. Auto-trading of any kind.

---

## Current Known Critical Gaps

1. tm_user_position missing.
2. UserPositionService missing.
3. UserPositionController missing.
4. PositionMonitorService missing.
5. tm_position_monitor_log missing.
6. Review does not fully integrate real user position.
7. ExecutionPlan Source Gate not hardened.
8. HotReset real action incomplete.
9. OpportunityLog incomplete.
10. Macro / News runtime not complete.
11. AI orchestrator and ai call log incomplete.
12. Dashboard Final must wait until business semantics are stable.

---

## Current Deployment Readiness

Production deployment remains BLOCKED.

Blocking evidence:

- `src/main/resources/application.yml` uses `jdbc:h2:mem:trade_model_v1`.
- `src/main/resources/application.yml` has empty datasource password.
- `src/main/resources/application.yml` and `src/main/resources/application.properties` enable H2 console.
- `src/main/resources/application.properties` defaults `position.provider.type` to `SIMULATED`.
- No production profile, migration/rollback pipeline, auth/authz evidence, or deployment smoke/rollback evidence was found in the P0-0 audit pass.

---

## Derived / Compatibility Sources

`docs/ACTIVE_MAINLINE_STATUS.yml` and `docs/CODEX_NEXT_TASK.yml` are derived compatibility files only.
They do not override the Delivery Contract, Delivery Progress Matrix, or this Current State file.

Legacy V1 documents remain historical audit and asset evidence only.
Review-only slice count is no longer a delivery completion standard.

---

## Rule

No later business phase may start until P0-0 DONE is merged to main and `Next Business Phase Allowed` becomes YES through the contract gate.
