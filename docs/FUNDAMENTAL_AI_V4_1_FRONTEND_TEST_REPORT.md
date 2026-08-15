# Fundamental AI v4.1 Frontend Test Report

## Scope

This report covers PR `#1179` Desktop Home Execution Plan semantic and interaction remediation. Production changes are limited to:

- `src/main/resources/templates/dashboard.html`
- `src/main/resources/static/css/dashboard-latest.css`
- `src/main/resources/static/js/frontend-contract.js`
- `scripts/dashboard-visual-acceptance-fixture.py`

No Backend business logic, API contract, Schema, Figma, Mobile, or automatic-trading capability changed.

## Automated Results

| Validation | Result |
|---|---|
| Execution Plan / productized Home focused suites | `202` tests, `0` failures, `0` errors, `0` skipped |
| Maven full suite | `4525` tests, `0` failures, `0` errors, `14` skipped |
| Product Source Gate | PASS |
| Workflow Contract | PASS (`WORKFLOW_CONTRACT_OK`) |
| `frontend-contract.js` syntax | PASS |
| visual fixture compile | PASS |
| `git diff --check` | PASS |

The 14 skips are existing environment-gated tests, including unavailable Docker or external-provider paths. This remediation adds no exclusion or suppression.

## Contract Coverage

`FundamentalAiV41ExecutionPlanSemanticAlignmentContractTest` and the updated Home suites verify:

| Contract | Evidence |
|---|---|
| five formal Final modes | centralized `PLAN_MODE_VIEWS` and mode-specific renderer profiles |
| Plan Mode / data-state separation | independent `PLAN_MODE_VIEWS` and `PLAN_DATA_STATE_VIEWS` |
| PREPARATION is Final | Final mode renderer; no no-plan fallback |
| OBSERVATION / BLOCKED are Final | formal Final rendering without entry/stop/target sections |
| structured plan sections | 入场与触发, 失效与止损, 目标与趋势跟踪, 风险限制, 时间有效性 |
| Candidate / Final isolation | GPT `data-result-layer="candidate"`; Execution Plan `data-plan-source="final"` |
| Final source gate | Final, Rule Validation, chain, source, and `notTradeInstruction` checks |
| user-facing semantics | no binary worth-opening field, raw enum, or default-visible disclaimer copy |
| optional value safety | missing values are omitted; no zero or placeholder synthesis |
| interaction isolation | asset switch clears stale decision context and ignores stale responses |

## Defects Closed During Validation

1. Missing optional Final fields exposed `暂无 AI 原始输出`; these fields are now omitted.
2. Final-with-AI-unavailable used legacy copy; the workspace now shows `AI解释暂不可用` without borrowing Final fields.
3. Two legacy tests used removed disclaimer copy as a safety proof; they now assert the actual Final gate and Candidate/Final boundary.
4. One asset-card test scoped a diagnostics-copy assertion to the entire dashboard; it now inspects the asset-card renderer only.

## Browser Matrix

```text
REQUIRED_SCENARIO_GROUPS=13/13
HORIZONTAL_OVERFLOW_COUNT=0
TEXT_OVERFLOW_COUNT=0
TOP_LEVEL_OVERLAP_COUNT=0
CONSOLE_ERROR_COUNT=0
CONSOLE_WARNING_COUNT=0
UNHANDLED_REJECTION_COUNT=0
VISIBLE_AI_ROLE_COUNT=1
VISIBLE_DISCLAIMER_COPY_COUNT=0
RAW_ENUM_PRIMARY_DISPLAY_COUNT=0
STALE_ASSET_CONTENT_COUNT=0
CANDIDATE_VISIBLE_AS_FINAL=false
```

The complete index and hashes are under `docs/evidence/v4_1_execution_plan_semantics/`.

## Actual Spring Runtime

Authenticated login, `/dashboard`, `/api/dashboard/home`, and health all passed. The exact worktree CSS and JavaScript were served, and the real application page passed browser inspection at `1440 x 900` with no overflow, console errors, raw binary worth-opening field, or visible disclaimer copy.

## Commands

```text
./mvnw -q -Dtest=DashboardControllerTest,FundamentalAiV41ProductizedDesktopUiContractTest,FundamentalAiV41FrontendRuntimeAlignmentContractTest,FundamentalAiV41ExecutionPlanSemanticAlignmentContractTest,Fe04ShellHomeDashboardContractTest,FrontendImplementationFoundationContractTest,StaticNoTradeInstructionGuardTest test
./mvnw -q -Dtest=DashboardLocalRealBindingContractTest test
./mvnw test -q
bash scripts/product-source-gate.sh
bash scripts/check-workflow-contract.sh
<bundled-node> --check src/main/resources/static/js/frontend-contract.js
PYTHONPYCACHEPREFIX=/tmp/codex-pycache <bundled-python> -m py_compile scripts/dashboard-visual-acceptance-fixture.py
git diff --check
```
