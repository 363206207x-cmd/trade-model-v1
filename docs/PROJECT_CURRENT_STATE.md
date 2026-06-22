# Trade Model V1 Current State

Contract: docs/PROJECT_DELIVERY_CONTRACT.md
Contract Version: v1.0
Current Phase: P0-0 Contract Lock + Baseline + Dead Code Candidate Report
Current Phase Status: DONE
Completion Effective State: derived by v1 state runtime
Existing Module Maturity: PARTIAL
Current Work Package: P1-2 ConfusedState + AiConflict hardening DONE candidate
Next Business Phase: P1-3 HotReset real action
Next Business Phase Allowed: NO
Production Deployment Readiness: BLOCKED

---

## Effective State Rule

P0-0 is effective only because its DONE commit is merged to `main`, local `main` is synced, and the worktree was clean when the runtime gate evaluated it.

P0-1 UserPosition is effective because its implementation is merged to clean / synced `main`.

P0-2 ExecutionPlan Source Gate is effective because its implementation is merged to clean / synced `main` and the runtime gate allowed P0-3.

P0-3 AccountRisk integrates UserPosition is effective because its implementation is merged to clean / synced `main` and the runtime gate allowed P0-4.

P0-4 PositionMonitorLog is effective because its implementation is merged to clean / synced `main` and the runtime gate allowed P0-5.

P0-5 PositionMonitorService is effective because its implementation is merged to clean / synced `main` and the runtime gate allowed P0-6.

P0-6 Review integrates UserPosition is effective because its implementation is merged to clean / synced `main` and the runtime gate allowed P1-1.

P1-1 PushRecheck semantic hardening is effective because its implementation is merged to clean / synced `main` and the runtime gate allowed P1-2.

P1-2 ConfusedState + AiConflict hardening is only a branch DONE candidate in this worktree.
It is not effective until this branch commit is reviewed, merged to `main`, local `main` is synced, the worktree is clean, and `bash scripts/v1-state.sh` confirms P1-2 effectivity.

`bash scripts/v1-state.sh` must distinguish `CURRENT_PACKAGE_PR`, `UNRELATED_OPEN_PRS`, and `BLOCK_NEXT_BUSINESS_PHASE_ONLY`. An unrelated Draft PR must not block merging the current P0-0 package PR, but it still blocks the next business phase.

---

## Current Allowed Work

Only the following work is allowed after this P1-2 branch-candidate update:

1. Checks, push, PR creation, and merge-gate handling for the P1-2 ConfusedState + AiConflict hardening B-risk package.
2. Main sync after the P1-2 PR is reviewed and merged.
3. Runtime verification that P1-2 is effective on clean / synced main before any P1-3 work starts.

P1-2 is a DONE candidate on the task branch only. It is not effective until the branch commit is merged to `main`, local `main` is synced, and the worktree is clean.

PR #1004 was an unrelated Draft dashboard PR and no code from it is merged into this package.

---

## Current Forbidden Work

The following work is blocked until P1-2 is effective on merged main:

1. HotReset real action.
2. OpportunityLog.
3. Macro / News / External Context.
4. AI Orchestrator + AiCallLog.
5. Scheduler / Idempotency / Trace.
6. Dashboard Final.
7. Auto-trading of any kind.

---

## Current Known Critical Gaps

1. P1-2 ConfusedState + AiConflict hardening is only a branch DONE candidate until merged main confirms it effective.
2. HotReset real action incomplete.
3. OpportunityLog incomplete.
4. Macro / News runtime not complete.
5. AI orchestrator and ai call log incomplete.
6. Dashboard Final must wait until business semantics are stable.

## P1-2 ConfusedState + AiConflict Hardening Branch Candidate

Branch: `p1-2-confused-state-ai-conflict-hardening`
Risk: B
Status: DONE candidate
Effective State: pending merged main

Implemented branch evidence:

1. ConfusedStatePolicy fixes enter, block, exit threshold, and required low-cycle constants.
2. `tm_asset_state.confused_low_streak` persists per-symbol low-confused streak across analysis cycles.
3. ConfusedStateService enters CONFUSED at score >= 70, blocks directional push at score >= 85, requires two consecutive scores below 55 to exit, and exits only to COOLING.
4. AiConflictResolver preserves the rule-layer base direction and limits AI disagreement effects to adjusted confidence, risk adjustment, plan mode, and confused contribution.
5. Single AI objection only reduces confidence / plan mode within a bounded result and cannot force infinite waiting or direct CONFUSED.
6. DecisionEngine uses rule-layer base direction for marketBiasHierarchy, records directionalPushBlocked, and treats GPT / Gemini / Grok roles as advisory review / challenge only.
7. PushSnapshotService explicitly skips directional snapshot writes when directionalPushBlocked=true.
8. Tests cover ConfusedState thresholds/transitions, aligned / minor / major / extreme AI conflict, DecisionEngine integration, PushSnapshot block guard, safety fields, and no direct TRIGGERED exit.
9. No external AI provider, UserPosition mutation, automatic open / close / reverse, order execution, auto-trading, Push send, external channel, Dashboard UI, HotReset real action, or P1-3 changes are part of this package.

P1-3 remains blocked until this branch is reviewed, merged to `main`, main is synced, and runtime state confirms P1-2 effective.

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

No later business phase may start until the current branch candidate is merged to main and `Next Business Phase Allowed` becomes YES through the contract gate.
