# Trade Model V1 Current State

Contract: docs/PROJECT_DELIVERY_CONTRACT.md
Contract Version: v1.0
Current Phase: P0-0 Contract Lock + Baseline + Dead Code Candidate Report
Current Phase Status: DONE
Completion Effective State: derived by v1 state runtime
Existing Module Maturity: PARTIAL
Current Work Package: P2-2 AI Orchestrator + AiCallLog DONE candidate
Next Business Phase: P2-3 Scheduler / Idempotency / Trace
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

P1-2 ConfusedState + AiConflict hardening is effective because its implementation is merged to clean / synced `main` and the runtime gate allowed P1-3.

P1-3 HotReset real action is effective because its implementation is merged to clean / synced `main` and the runtime gate allowed P1-4.

P1-4 OpportunityLog is effective because its implementation is merged to clean / synced `main` by PR #1017 and the runtime gate allowed P2-1.

P2-1 Macro / News / External Context is effective because its implementation is merged to clean / synced `main` by PR #1018 commit `d7fef874b39aabbd07f6b05fd97f4725e89e79b5` and the runtime gate allowed P2-2.

P2-2 AI Orchestrator + AiCallLog is only a branch DONE candidate in this worktree.
It is not effective until this branch commit is reviewed, merged to `main`, local `main` is synced, the worktree is clean, and `bash scripts/v1-state.sh` confirms P2-2 effectivity.

`bash scripts/v1-state.sh` must distinguish `CURRENT_PACKAGE_PR`, `UNRELATED_OPEN_PRS`, and `BLOCK_NEXT_BUSINESS_PHASE_ONLY`. An unrelated Draft PR must not block merging the current P0-0 package PR, but it still blocks the next business phase.

---

## Current Allowed Work

Only the following work is allowed after this P2-2 branch-candidate update:

1. Checks, push, PR creation, and merge-gate handling for the P2-2 AI Orchestrator + AiCallLog B-risk package.
2. Main sync after the P2-2 PR is reviewed and merged.
3. Runtime verification that P2-2 is effective on clean / synced main before any P2-3 work starts.

P2-2 is a DONE candidate on the task branch only. It is not effective until the branch commit is merged to `main`, local `main` is synced, and the worktree is clean.

PR #1004 was an unrelated Draft dashboard PR and no code from it is merged into this package.

---

## Current Forbidden Work

The following work is blocked until P2-2 is effective on merged main:

1. Scheduler / Idempotency / Trace.
2. Dashboard Final.
3. Auto-trading of any kind.

---

## Current Known Critical Gaps

1. P2-2 AI Orchestrator + AiCallLog is only a branch DONE candidate until merged main confirms it effective.
2. Scheduler / Idempotency / Trace remains incomplete and blocked.
3. Dashboard Final must wait until business semantics are stable.

## P2-2 AI Orchestrator + AiCallLog Branch Candidate

Branch: `p2-2-ai-orchestrator-ai-call-log-full-implementation`
Risk: B
Status: DONE candidate
Effective State: pending merged main

Implemented branch evidence:

1. `AiDecisionOrchestratorService` runs provider review after rule-layer facts exist.
2. OpenAI / GPT, Gemini, and xAI / Grok adapters are safe, env-configured, default disabled, and tested with fake transport only.
3. AI outputs are strict JSON review-only results: SUPPORT / CHALLENGE / ABSTAIN plus conflict level and reason codes.
4. Provider responses with direction overrides, executable fields, prompt-injection text, malformed JSON, timeout, 429, or failure fail closed to fallback.
5. AI cannot overwrite rule-layer base direction, cannot create ExecutionPlan, cannot write state-machine transitions, cannot create or mutate UserPosition, and cannot issue order / Push / external-channel actions.
6. `tm_ai_call_log`, mapper, and service record STARTED before provider call and completion/fallback token, cost, latency, provider, traceId, rate-limit, budget, and error evidence without raw keys or raw provider payload.
7. `AiUsageGuard` blocks disabled, unconfigured, unknown-cost, exhausted-budget, and rate-limited providers before network calls.
8. `/api/ai/orchestrator/status` and `/api/ai/call-logs` are read-only status/log surfaces and do not expose API keys.
9. DecisionEngine uses orchestration summary only as review evidence; AI challenge contributes bounded conflict downgrade through `AiConflictResolverService`.
10. Tests cover provider request mapping, no provider-network tests, parser injection/forbidden-field guards, budget/rate limit guards, orchestrator fallback modes, AiCallLog lifecycle, controller safety, and DecisionEngine preservation of rule-layer direction.

P2-3 remains blocked until this branch is reviewed, merged to `main`, main is synced, and runtime state confirms P2-2 effective.

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
