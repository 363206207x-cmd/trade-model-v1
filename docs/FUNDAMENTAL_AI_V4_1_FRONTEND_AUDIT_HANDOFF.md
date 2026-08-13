# Fundamental AI v4.1 Frontend Audit Handoff

## Exact Audit Target

- Package: `FUNDAMENTAL_AI_V4_1_FRONTEND_RUNTIME_ALIGNMENT`
- Issue: `#1178`
- PR: `#1179` (Draft, unmerged)
- Branch: `codex/v4-1-frontend-runtime-alignment`
- Base main: `edc3615c03c9b71763c32574f1d811c1d9a8954d`
- Remediation starting Head: `3c3f7e96a8f384bbac1f1aa6f2534ef8d76b0efd`
- Candidate Head: exact remote PR Head after this remediation push
- Candidate state: `EXECUTION_PLAN_SEMANTIC_REMEDIATION_COMPLETE_PENDING_INDEPENDENT_FRONTEND_AUDIT`

The auditor must record the remote PR Head first and audit that immutable commit. The audit is read-only and must not repair findings.

## Required Reading

1. `docs/FUNDAMENTAL_AI_V4_1_EXECUTION_PLAN_SEMANTIC_REMEDIATION.md`
2. `docs/FUNDAMENTAL_AI_V4_1_PLAN_MODE_UI_MATRIX.md`
3. `docs/FUNDAMENTAL_AI_V4_1_PLAN_DATA_STATE_MAPPING.md`
4. `docs/FUNDAMENTAL_AI_V4_1_RESULT_EXPLANATION_SEPARATION.md`
5. `docs/FUNDAMENTAL_AI_V4_1_RUNTIME_VISUAL_VALIDATION.md`
6. `docs/FUNDAMENTAL_AI_V4_1_FRONTEND_TEST_REPORT.md`
7. `docs/FUNDAMENTAL_AI_V4_1_FRONTEND_REMAINING_GAPS.md`
8. `docs/evidence/v4_1_execution_plan_semantics/README.md`
9. `docs/evidence/v4_1_execution_plan_semantics/browser-qa.json`

## Production Path

| Surface | Selector / function |
|---|---|
| Execution Plan | `#homeExecutionCard`, `renderHomeExecutionFromPayload` |
| Final result layer | `[data-plan-source="final"]` |
| Plan Mode mapper | `TradeModelFrontendContract.USER_FACING_SEMANTIC_MAPPER.planMode` |
| missing-Final mapper | `TradeModelFrontendContract.USER_FACING_SEMANTIC_MAPPER.planDataState` |
| Three AI workspace | `#homeAiPanel`, `renderHomeAiRoleTab` |
| GPT Candidate layer | `[data-result-layer="candidate"]` |
| asset switch | `renderAssetContextLoading`, `fetchDashboardHome` |

## Independent Audit Checklist

### Scope

- Confirm no Backend business logic, API contract, Schema, Figma, or Mobile change.
- Search for automatic open/close/add/reduce/reverse/order capability; expected count is zero.
- Confirm the existing semantic mapper remains the only user-facing mapping owner.

### Execution Plan

- Confirm the primary title is `执行计划`.
- Confirm CONFIRMATION, PREPARATION, REDUCED, OBSERVATION, and BLOCKED are formal Final modes.
- Confirm Plan Mode and missing-Final state are independent and cannot fall back to each other.
- Confirm PREPARATION is not shown as no plan.
- Confirm OBSERVATION and BLOCKED do not render entry, stop, target, default leverage, or default position values.
- Confirm CONFIRMATION and REDUCED use the required structured sections.
- Confirm empty optional Final values are hidden rather than filled with `--`, zeroes, or synthetic copy.
- Confirm `是否值得开仓` and default-visible disclaimer copy are absent from the primary Execution Plan and GPT surfaces.

### Candidate / Final / AI

- Confirm GPT owns Candidate semantics only and labels `候选计划模式`.
- Confirm the Execution Plan opens only through the Final/source/validation/chain/not-trade gates.
- Confirm Candidate-only leaves Execution Plan waiting for Rule Validation.
- Confirm a valid Final remains visible when AI explanation is unavailable.
- Confirm one AI workspace and one visible role.

### Interaction And Evidence

- Switch BTC to ETH and confirm stale result count remains zero.
- Confirm System Status, Alerts/Event, and Position do not change during asset-context switching.
- Inspect all 13 numbered evidence groups plus the supplemental actual-Spring screenshot.
- Verify `browser-qa.json` source hashes against the exact PR Head.
- Re-run focused tests, full Maven, Product Source Gate, Workflow Contract, syntax/fixture checks, and `git diff --check`.

## Current Candidate Evidence

```text
FOCUSED_MAVEN=202 tests / 0 failures / 0 errors / 0 skipped
FULL_MAVEN=4525 tests / 0 failures / 0 errors / 14 skipped
CONTROLLED_BROWSER=PASS
ACTUAL_SPRING_BROWSER=PASS
REQUIRED_SCENARIO_GROUPS=13/13
HORIZONTAL_OVERFLOW_COUNT=0
TEXT_OVERFLOW_COUNT=0
TOP_LEVEL_OVERLAP_COUNT=0
CONSOLE_ERROR_COUNT=0
CONSOLE_WARNING_COUNT=0
VISIBLE_AI_ROLE_COUNT=1
VISIBLE_DISCLAIMER_COPY_COUNT=0
RAW_ENUM_PRIMARY_DISPLAY_COUNT=0
STALE_ASSET_CONTENT_COUNT=0
CANDIDATE_VISIBLE_AS_FINAL=false
PRODUCT_SOURCE_GATE=PASS
WORKFLOW_CONTRACT=PASS
```

## Decision Boundary

Allowed independent-audit result: `APPROVE` or `REQUEST_CHANGES`.

Approval must not claim live-provider acceptance, merge, or merged-main effectiveness. No merge, Mobile work, Figma change, next product package, or automatic-trading expansion is allowed inside this handoff.
