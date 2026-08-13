# Fundamental AI v4.1 Execution Plan Semantic Evidence

## Evidence Boundary

- Branch: `codex/v4-1-frontend-runtime-alignment`
- PR: `#1179` (Draft, unmerged)
- Starting Head: `3c3f7e96a8f384bbac1f1aa6f2534ef8d76b0efd`
- Base main: `edc3615c03c9b71763c32574f1d811c1d9a8954d`
- Browser source: current `dashboard.html`, `dashboard-latest.css`, and `frontend-contract.js`
- Primary viewport: `1440 x 900`
- Controlled transport: read-only fixture, zero external calls, writes rejected
- Actual runtime: authenticated Spring application with schedulers and external providers disabled

The controlled fixture proves deterministic rendering and interaction states. The authenticated Spring run proves that the current template and static assets load through the real application. Neither source claims live-provider truth.

## Approved Design Provenance

The implementation continues to use the approved Desktop sources `28:154`, `31:23`, `520:212`, `523:748`, `35:97`, `35:4`, `35:35`, and `35:66` in Figma file `rdMYmsAvZYkXHJX8hdl7UN`. Node `519:3` is the rejected old P1-KB baseline. No Figma node changed in this task.

## Required Runtime Evidence

| # | File | Scenario | Result |
|---:|---|---|---|
| 01 | `runtime/01-unselected.png` | 未选择资产 | PASS |
| 02 | `runtime/02-waiting-analysis.png` | 等待分析 | PASS |
| 03 | `runtime/03-plan-preparation.png` | PREPARATION Final | PASS |
| 04 | `runtime/04-plan-observation.png` | OBSERVATION Final | PASS |
| 05 | `runtime/05-plan-blocked.png` | BLOCKED Final | PASS |
| 06 | `runtime/06-plan-confirmation.png` | CONFIRMATION Final | PASS |
| 07 | `runtime/07-plan-reduced.png` | REDUCED Final | PASS |
| 08 | `runtime/08-candidate-no-final.png` | Candidate 有、Final 无 | PASS |
| 09 | `runtime/09-final-ai-unavailable.png` | Final 有、AI 不可用 | PASS |
| 10 | `runtime/10-gpt-candidate.png` | GPT Candidate | PASS |
| 11 | `runtime/11-before-execution-plan.png` and `runtime/11-after-execution-plan.png` | Before / After 执行计划区域 | PASS |
| 12 | `runtime/12-desktop-first-viewport-1440x900.png` | 1440 x 900 first viewport | PASS |
| 13 | `runtime/13-desktop-full-page.png` | Full page | PASS |

Supplemental actual-application evidence: `runtime/14-actual-spring-runtime.png`.

## Browser Gates

```text
VISIBLE_DISCLAIMER_COPY_COUNT=0
RAW_ENUM_PRIMARY_DISPLAY_COUNT=0
HORIZONTAL_OVERFLOW_COUNT=0
TEXT_OVERFLOW_COUNT=0
TOP_LEVEL_OVERLAP_COUNT=0
CONSOLE_ERROR_COUNT=0
CONSOLE_WARNING_COUNT=0
VISIBLE_AI_ROLE_COUNT=1
STALE_ASSET_CONTENT_COUNT=0
CANDIDATE_VISIBLE_AS_FINAL=false
```

`browser-qa.json` records source hashes, screenshot hashes, scenario outcomes, asset-switch isolation, and authenticated Spring runtime checks.

## Actual Spring Runtime

- Login page: HTTP `200`
- Authenticated login: HTTP `302`
- Authenticated `/dashboard`: HTTP `200`, `716941` bytes
- Authenticated `/api/dashboard/home`: HTTP `200`
- `/actuator/health`: `UP`
- Served CSS and semantic mapper: exact SHA-256 match with worktree
- In-app browser: PASS at `1440 x 900`
- Browser console errors/warnings: `0 / 0`
- Authenticated live-provider evidence: not executed

## Evidence Interpretation

PREPARATION, OBSERVATION, and BLOCKED are visible formal Final Plan Modes. Candidate-only remains in GPT and never appears as Final. A Final remains visible when AI explanation is unavailable. Optional Final fields with no real value are omitted rather than replaced by `--`, zeroes, or synthetic copy.
