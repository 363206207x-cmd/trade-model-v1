# Fundamental AI v4.1 Latest Approved UI Visual Test Report

## Evidence Boundary

The browser tests load current `dashboard.html`, `dashboard-latest.css`, and frontend contract code through a deterministic local, read-only acceptance transport. They validate visual structure, binding, interactions and fail-closed states. They do not claim live-provider, live-market or production-AI accuracy.

- Primary viewport: `1440 x 900`
- Full page: `1440 x 2453`
- Runtime writes: rejected
- External calls: `0`
- Evidence index: `docs/evidence/v4_1_latest_ui/README.md`
- Machine-readable metrics: `docs/evidence/v4_1_latest_ui/browser-qa.json`

## Required Visual Matrix

| # | Scenario | Evidence | Result |
|---|---|---|---|
| 1 | Desktop first viewport | `runtime/01-desktop-1440x900-light.png` | PASS |
| 2 | Desktop full page | `runtime/02-desktop-full-page-light.png` | PASS |
| 3 | Dark mode | `runtime/03-desktop-dark.png` | PASS |
| 4 | Dynamic Top6, six assets | `runtime/04-dynamic-top6-six.png` | PASS |
| 5 | Dynamic Top6, fewer than six | `runtime/05-dynamic-top6-less-than-six.png` | PASS |
| 6 | Search input | `runtime/06-search-input.png` | PASS |
| 7 | Asset Pool open | `runtime/07-asset-pool-open.png` | PASS |
| 8 | No Position | `runtime/08-position-no-position.png` | PASS |
| 9 | Open Position Top3 | `runtime/09-position-open-top3.png` | PASS |
| 10 | Validated Final Plan | `runtime/10-execution-final.png` | PASS |
| 11 | Blocked/non-Final | `runtime/11-execution-blocked.png` | PASS |
| 12 | GPT_FINAL tab | `runtime/12-gpt-tab.png` | PASS |
| 13 | GEMINI_REVIEW tab | `runtime/13-gemini-tab.png` | PASS |
| 14 | GROK_CHALLENGE tab | `runtime/14-grok-tab.png` | PASS |
| 15 | AI Consistency | `runtime/15-ai-consistency.png` | PASS |
| 16 | AI partial/failure | `runtime/16-ai-partial-failure.png` | PASS |
| 17 | Empty evidence | `runtime/17-empty-evidence.png` | PASS |
| 18 | Asset switch without stale/global mutation | `runtime/18-asset-switch-no-stale.png` | PASS |
| 19 | Figma versus implementation | `runtime/19-figma-vs-implementation.png` | PASS |
| 20 | Previous candidate versus latest UI | `runtime/20-before-after.png` | PASS |

The full-page image is assembled from three overlapping, unscaled current-browser viewport captures because the browser capture surface caps single-image height. No page content was generated or rescaled during composition.

## Browser Measurements

```text
HORIZONTAL_OVERFLOW_COUNT=0
TEXT_OVERFLOW_COUNT=0
TOP_LEVEL_OVERLAP_COUNT=0
CONSOLE_ERROR_COUNT=0
CONSOLE_WARNING_COUNT=0
UNHANDLED_REJECTION_COUNT=0
DETACHED_VISUAL_STATE_COUNT=0
VISIBLE_AI_ROLE_COUNT=1
POSITION_EXECUTION_WIDTH_RATIO=2.3333
CANDIDATE_VISIBLE_AS_FINAL=false
```

Additional observed state:

- six-asset scenario: `6` cards;
- fewer-than-six scenario: `5` cards and `0` default fills;
- Asset Pool: `10` managed assets;
- search: native input with `3` controlled suggestions;
- No Position: `0` rows, `0` close buttons, `0` placeholder rows;
- Open Position: `3` rows;
- AI Consistency: `0` percentages and `0` charts.

## Figma Provenance

The comparison package uses actual captured sources for nodes `28:154`, `31:23`, `520:212`, `523:748`, `35:97`, `35:4`, `35:35`, and `35:66`. Node `519:3` is labelled as the rejected old P1-KB baseline and is not the implementation target.

## Result

```text
BROWSER_VISUAL_ALIGNMENT=PASS
LIGHT_MODE=PASS
DARK_MODE=PASS
NO_OLD_UI_PRODUCTION_PATH=PASS
NO_FAKE_RUNTIME_VALUE=PASS
TARGET_RUNTIME_LIVE_PROVIDER_EVIDENCE=TARGET_RUNTIME_EVIDENCE_PENDING
```
