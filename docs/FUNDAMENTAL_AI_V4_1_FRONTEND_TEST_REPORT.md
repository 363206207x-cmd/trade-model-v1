# Fundamental AI v4.1 Frontend Test Report

## Scope

This report covers PR `#1179` productized Desktop UI remediation only. The production files under test are:

- `src/main/resources/templates/dashboard.html`
- `src/main/resources/templates/login.html`
- `src/main/resources/templates/analysis-detail.html`
- `src/main/resources/static/css/dashboard-latest.css`
- `src/main/resources/static/js/frontend-contract.js`
- `scripts/dashboard-visual-acceptance-fixture.py`

No Backend business model, API contract, Schema, Mobile, Figma, or automatic-trading capability changed.

## Automated Results

| Validation | Result |
|---|---|
| Productized UI + Home compatibility (`DashboardControllerTest` and two v4.1 suites) | `174` tests, `0` failures, `0` errors, `0` skipped |
| FE-04 / foundation / no-trade compatibility | `22` tests, `0` failures, `0` errors, `0` skipped |
| Maven full suite | `4519` tests, `0` failures, `0` errors, `14` skipped |
| Product Source Gate | PASS |
| Workflow Contract | PASS (`WORKFLOW_CONTRACT_OK`) |
| `frontend-contract.js` syntax with bundled Node.js | PASS |
| Python visual fixture compile | PASS |
| `git diff --check` | PASS after final documentation update |

The 14 skipped tests are existing environment-gated paths, including unavailable Docker or external-provider fixtures. This revision did not add exclusions or suppressions.

## Defects Found During Validation

The first full run found three categories of issue, all resolved before the final run:

1. Nineteen `DashboardControllerTest` assertions referenced removed P1 copy or DOM. Their fail-closed, field-isolation, single-workspace, Dynamic Top6, and no-trade intent now targets the current product contract.
2. Four FE-04/foundation guards referenced old consistency, Final, and no-position copy. Their safety assertions were retained with current user-facing semantics.
3. A nested JavaScript action array inside the Thymeleaf template was interpreted as an inline expression. `homeAssetEmptyStateMarkup` now uses object entries, preserving the rendered UI while allowing authenticated server-side template processing.

Authentication/session regression tests confirm the corrected template renders successfully.

## Contract Coverage

`FundamentalAiV41ProductizedDesktopUiContractTest` and `FundamentalAiV41FrontendRuntimeAlignmentContractTest` verify these selectors and behaviors:

| Surface | Production selector / function | Covered behavior |
|---|---|---|
| Product root | `[data-latest-approved-home]` | brand, hierarchy, current production path |
| System status | `.latest-system-status` | six semantic statuses and tone mapping |
| Alerts/events | `.latest-signal-grid` | compact rows and exact empty states |
| Dynamic Top6 | `#tilesRow`, `authoritativeHomeAssetList` | backend order, limit six, no fixed-symbol fill |
| Asset search | `#symbolSearch`, `renderAssetSearchSuggestions` | select-first gating for Add/Analyze |
| Position | `#homePositionCard`, `renderHomePositionsFromPayload` | no-position, waiting, trusted Top3, no semantic fallback |
| Final | `#homeExecutionCard`, `renderHomeExecutionFromPayload` | validated Final only; Candidate never exposed as Final |
| Three AI | `#homeAiPanel`, `renderHomeAiRoleTab` | one workspace, one visible role, role-specific structure |
| Adjustment | `#homeConsistencyContent`, `renderHomeConsistencyCard` | dependent conflict/final-adjustment summary, no vote/chart |
| Asset switch | `renderAssetContextLoading`, `fetchDashboardHome` | stale response guard and global-state isolation |

## Controlled Browser Matrix

The read-only fixture served the real current templates and assets at `1440 x 900`. It rejected writes and performed zero external calls.

```text
SCREENSHOT_COUNT=21
HORIZONTAL_OVERFLOW_COUNT=0
TEXT_OVERFLOW_COUNT=0
TOP_LEVEL_OVERLAP_COUNT=0
CONSOLE_ERROR_COUNT=0
CONSOLE_WARNING_COUNT=0
UNHANDLED_REJECTION_COUNT=0
DETACHED_VISUAL_STATE_COUNT=0
RAW_ENUM_PRIMARY_DISPLAY_COUNT=0
DUPLICATE_CONSISTENCY_COUNT=0
VISIBLE_AI_ROLE_COUNT=1
FAKE_RUNTIME_VALUE_COUNT=0
CANDIDATE_VISIBLE_AS_FINAL=false
```

The complete scenario-to-file index is in `docs/evidence/v4_1_productized_ui/README.md`; machine-readable measurements and source hashes are in `browser-qa.json`.

## Actual Spring Runtime

The current branch started successfully on `127.0.0.1` with authentication enabled and a throwaway local user. Schedulers, external providers, push, and automatic trading were disabled.

| Check | Result |
|---|---|
| login page / authenticated login | HTTP `200` / `302` |
| authenticated `/dashboard` | HTTP `200`; complete 711,507-byte response; current productized markers present |
| `/actuator/health` | `UP` |
| served CSS and semantic mapper | exact worktree SHA-256 match |
| authenticated `/api/dashboard/home` | HTTP `200`; current Dashboard contract returned |
| actual Spring page in in-app browser | BLOCKED by browser URL policy |
| authenticated real-provider browser scenario | NOT EXECUTED |

The controlled browser matrix and authenticated local HTTP/template rendering are PASS. Actual Spring browser inspection and authenticated real-provider acceptance remain BLOCKED and are not claimed as complete.

## Commands

```text
./mvnw -q -Dtest=DashboardControllerTest,FundamentalAiV41ProductizedDesktopUiContractTest,FundamentalAiV41FrontendRuntimeAlignmentContractTest test
./mvnw -q -Dtest=Fe04ShellHomeDashboardContractTest,FrontendImplementationFoundationContractTest,StaticNoTradeInstructionGuardTest test
./mvnw test -q
bash scripts/product-source-gate.sh
bash scripts/check-workflow-contract.sh
<bundled-node> --check src/main/resources/static/js/frontend-contract.js
PYTHONPYCACHEPREFIX=/tmp/codex-pyc python3 -m py_compile scripts/dashboard-visual-acceptance-fixture.py
git diff --check
```
