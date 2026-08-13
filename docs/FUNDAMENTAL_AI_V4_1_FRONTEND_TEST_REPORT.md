# Fundamental AI v4.1 Frontend Test Report

## Automated Results

| Validation | Result |
|---|---|
| Latest UI focused contract suite (`FundamentalAiV41FrontendRuntimeAlignmentContractTest`, `DashboardControllerTest`) | 169 tests, 0 failures, 0 errors, 0 skipped |
| Migrated FE-04 / no-trade compatibility guards | 22 tests, 0 failures, 0 errors, 0 skipped |
| Maven full suite | 4514 tests, 0 failures, 0 errors, 14 skipped |
| Product Source Gate | PASS |
| Workflow Contract | PASS (`WORKFLOW_CONTRACT_OK`) |
| Dashboard inline JavaScript syntax | PASS |
| `frontend-contract.js` syntax | PASS |
| Python visual fixture compile | PASS |
| Shell syntax for `scripts/v1-state.sh` | PASS |
| `git diff --check` | PASS |

The 14 skipped tests are environment-gated tests already represented by Surefire, including unavailable Docker/external-provider paths. They are not newly suppressed by this change.

## Regression Resolution

The first full run exposed five static tests still anchored to the removed P1-KB DOM and copy. Their safety intent was retained and rebound to the latest production structure:

- asset-context failure now asserts System Status, alerts/events and positions remain unchanged;
- Position/Execution guards target the latest `70:30` section and P1-KD content;
- AI Consistency guards target the single latest workspace and adjacent compact summary;
- no-trade guards assert the Final-only `notTradeInstruction` message and passive manual-position boundary.

The final full run is green.

## Latest UI Contract Coverage

The focused suite verifies:

- frozen Desktop module order and latest production root;
- exact Figma node provenance and rejection of old node `519:3`;
- latest Asset Card and authoritative Dynamic Top6 with no local ranking/fill;
- real search input and existing Asset Pool actions;
- Position/Execution `70:30` and P1-KD Position semantics;
- validated Final-only rendering and Candidate exclusion;
- one Three-AI workspace, one visible role and structured anti-hallucination semantics;
- compact AI Consistency without vote/percentage/chart/fourth role;
- asset context switch isolation;
- all 20 visual evidence files, first viewport dimensions and browser metrics;
- no old Home renderer in the latest production path.

## Browser Runtime Matrix

Browser checks used current templates/scripts and a deterministic local acceptance transport at an exact `1440 x 900` viewport.

| Area | Evidence | Result |
|---|---|---|
| Desktop first viewport and full page | `01`, `02` | PASS |
| Light and dark | `01`, `03` | PASS |
| Dynamic Top6 six/fewer-than-six | `04`, `05` | PASS |
| Search and Asset Pool | `06`, `07` | PASS |
| No Position / Open Top3 | `08`, `09` | PASS |
| Final / Blocked | `10`, `11` | PASS |
| GPT / Gemini / Grok | `12`, `13`, `14` | PASS |
| AI Consistency | `15` | PASS |
| AI partial failure / empty evidence | `16`, `17` | PASS |
| Asset switch without stale/global mutation | `18` | PASS |
| Figma comparison / before-after | `19`, `20` | PASS |

Complete paths are indexed by `docs/evidence/v4_1_latest_ui/README.md`.

## Browser Quality Gates

```text
HORIZONTAL_OVERFLOW_COUNT=0
TEXT_OVERFLOW_COUNT=0
TOP_LEVEL_OVERLAP_COUNT=0
CONSOLE_ERROR_COUNT=0
CONSOLE_WARNING_COUNT=0
UNHANDLED_REJECTION_COUNT=0
DETACHED_VISUAL_STATE_COUNT=0
FAKE_RUNTIME_VALUE_COUNT=0
VISIBLE_AI_ROLE_COUNT=1
POSITION_EXECUTION_WIDTH_RATIO=2.3333
CANDIDATE_VISIBLE_AS_FINAL=false
```

## Safety And Data Truth

- Fixture mode is `CONTROLLED_VISUAL_FIXTURE` and runtime writes are rejected.
- No screenshot or fixture value is claimed as live-provider evidence.
- Candidate, stale, pending, invalid, missing source and AI failure states fail closed.
- No open, close, add, reduce, reverse, order or exchange execution capability was added.

## Commands

```text
./mvnw -q -Dtest=FundamentalAiV41FrontendRuntimeAlignmentContractTest,DashboardControllerTest test
./mvnw -q -Dtest=Fe04ShellHomeDashboardContractTest,FrontendImplementationFoundationContractTest,StaticNoTradeInstructionGuardTest test
./mvnw test -q
bash scripts/product-source-gate.sh
bash scripts/check-workflow-contract.sh
<bundled-node> --check src/main/resources/static/js/frontend-contract.js
<bundled-node> -e '<extract and compile dashboard inline script>'
env PYTHONPYCACHEPREFIX=/tmp/codex-pycache python3 -m py_compile scripts/dashboard-visual-acceptance-fixture.py
bash -n scripts/v1-state.sh
git diff --check
```
