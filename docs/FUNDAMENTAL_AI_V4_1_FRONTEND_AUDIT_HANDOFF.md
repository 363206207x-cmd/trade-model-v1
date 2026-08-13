# Fundamental AI v4.1 Frontend Audit Handoff

## Exact Audit Target

- Package: `FUNDAMENTAL_AI_V4_1_FRONTEND_RUNTIME_ALIGNMENT`
- Issue: `#1178`
- PR: `#1179` (Draft, unmerged)
- Branch: `codex/v4-1-frontend-runtime-alignment`
- Base main: `edc3615c03c9b71763c32574f1d811c1d9a8954d`
- Productization starting Head: `3c485d40f9668f6835328bf8f917fde62d73ebc1`
- Candidate Head: the exact remote PR Head after this remediation push
- Candidate state: `PRODUCTIZED_UI_CANDIDATE_PENDING_INDEPENDENT_AUDIT`

The auditor must record the remote PR Head before reading evidence and audit that immutable commit. The audit is read-only and must not repair findings.

## Required Reading

1. `docs/FUNDAMENTAL_AI_V4_1_PRODUCTIZED_UI_REMEDIATION.md`
2. `docs/FUNDAMENTAL_AI_V4_1_USER_FACING_SEMANTIC_MAPPING.md`
3. `docs/FUNDAMENTAL_AI_V4_1_UI_INTERACTION_STATE_MATRIX.md`
4. `docs/FUNDAMENTAL_AI_V4_1_UI_VISUAL_SYSTEM.md`
5. `docs/FUNDAMENTAL_AI_V4_1_RUNTIME_VISUAL_VALIDATION.md`
6. `docs/FUNDAMENTAL_AI_V4_1_FRONTEND_TEST_REPORT.md`
7. `docs/FUNDAMENTAL_AI_V4_1_FRONTEND_REMAINING_GAPS.md`
8. `docs/evidence/v4_1_productized_ui/README.md`
9. `docs/evidence/v4_1_productized_ui/browser-qa.json`

## Production Path And Selectors

| Contract surface | Selector / function |
|---|---|
| Product root | `[data-latest-approved-home]` |
| System Status | `.latest-system-status` |
| Alerts / Event Calendar | `.latest-signal-grid` |
| Dynamic Top6 | `#tilesRow`, `authoritativeHomeAssetList` |
| Asset Pool / Search | `#symbolSearch`, `#assetPoolPanel`, `renderAssetSearchSuggestions` |
| Position Monitoring | `#homePositionCard`, `renderHomePositionsFromPayload` |
| Final Plan | `#homeExecutionCard`, `renderHomeExecutionFromPayload` |
| Three AI | `#homeAiPanel`, `renderHomeAiRoleTab` |
| Conflict/final adjustment | `#homeConsistencyContent`, `renderHomeConsistencyCard` |

## Independent Audit Checklist

### Scope

- Confirm no Backend business model, API contract, Schema, Mobile, or Figma changes.
- Search for automatic open/close/add/reduce/reverse/order capability; expected count is zero.
- Confirm `TradeModelFrontendContract.USER_FACING_SEMANTIC_MAPPER` remains the single primary-copy mapper.

### Productized Hierarchy

- Confirm brand is `Fundamental AI` and subtitle is `多源证据决策系统` or its approved English equivalent.
- Confirm header focuses on update/data/current asset rather than diagnostics.
- Confirm system status, compact alert/event rows, Current Opportunities, Position, Final, Three AI, and conflict adjustment preserve frozen boundaries.
- Confirm light/dark semantic colors do not encode unavailable or selected state as success.

### Dynamic Top6 And Search

- Confirm Home preserves authoritative backend ranking order, filters default slots, and limits to six without local ranking or symbol fill.
- Exercise all three states: Pool empty, Pool populated/no opportunity, ranking unavailable.
- Confirm search does not mutate the Pool and Add/Analyze remain disabled until explicit result selection.
- Confirm switching assets clears old decision content immediately and ignores stale responses while leaving status, alert/event, Top6, and positions unchanged.

### Position And Final

- Confirm No Position renders no fake row, PnL, risk, conclusion, or close action.
- Confirm untrusted monitor state keeps risk/conclusion/action/PnL closed.
- Confirm trusted Top3 fields remain independent and Execution Plan fields never enter Position rows.
- Confirm Final opens only through Final/source/validation/chain/not-trade gates and Candidate never appears as Final.

### Three AI And Adjustment

- Confirm one workspace, three tabs, and exactly one visible role.
- Confirm GPT explains candidate formation, Gemini reviews evidence/risk, and Grok challenges failure paths.
- Confirm role state and every collection state remain independent; unavailable roles do not render a detailed field grid.
- Confirm additional evidence and audit metadata use progressive disclosure.
- Confirm `冲突与最终调整` is dependent summary only, with no percentage, vote, chart, score, or fourth role.

### Evidence And Validation

- Inspect all 21 images under `docs/evidence/v4_1_productized_ui/runtime/` manually.
- Verify `browser-qa.json` source hashes against the exact PR Head.
- Re-run `./mvnw test -q`, Product Source Gate, Workflow Contract, fixture compile, and `git diff --check`.
- Treat actual-Spring browser/runtime acceptance as BLOCKED until approved target-browser evidence exists.

## Current Evidence

```text
CONTROLLED_BROWSER=PASS
SCREENSHOT_COUNT=21
HORIZONTAL_OVERFLOW_COUNT=0
TEXT_OVERFLOW_COUNT=0
TOP_LEVEL_OVERLAP_COUNT=0
CONSOLE_ERROR_COUNT=0
VISIBLE_AI_ROLE_COUNT=1
RAW_ENUM_PRIMARY_DISPLAY_COUNT=0
FAKE_RUNTIME_VALUE_COUNT=0
MAVEN=4519 tests / 0 failures / 0 errors / 14 skipped
PRODUCT_SOURCE_GATE=PASS
WORKFLOW_CONTRACT=PASS
ACTUAL_SPRING_AUTHENTICATED_HTTP=PASS
ACTUAL_SPRING_BROWSER=BLOCKED_BY_BROWSER_URL_POLICY
```

## Decision Boundary

Allowed independent-audit result: `APPROVE` or `REQUEST_CHANGES`.

Approval must not claim actual authenticated provider acceptance, merge, or merged-main effectiveness. No merge, Mobile work, Figma change, next product package, or automatic-trading expansion is allowed inside this handoff.
