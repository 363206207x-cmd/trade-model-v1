# Fundamental AI v4.1 Frontend Audit Handoff

## Exact Audit Target

- Package: `FUNDAMENTAL_AI_V4_1_FRONTEND_RUNTIME_ALIGNMENT`
- Issue: `#1178`
- PR: `#1179` (Draft, unmerged)
- Branch: `codex/v4-1-frontend-runtime-alignment`
- Base: `edc3615c03c9b71763c32574f1d811c1d9a8954d`
- Starting implementation head: `490919d6f8c763ffaac634cfbffd02ad8eaf66c4`
- Candidate state: `LATEST_UI_IMPLEMENTATION_COMPLETE_PENDING_INDEPENDENT_FRONTEND_AUDIT`

The auditor must record and use the pushed PR Head as an immutable target. The audit is read-only and must not fix findings.

## Required Reading

1. `docs/FUNDAMENTAL_AI_V4_1_LATEST_UI_FIGMA_MAPPING.md`
2. `docs/FUNDAMENTAL_AI_V4_1_LATEST_UI_IMPLEMENTATION_REPORT.md`
3. `docs/FUNDAMENTAL_AI_V4_1_LATEST_UI_VISUAL_TEST_REPORT.md`
4. `docs/FUNDAMENTAL_AI_V4_1_FRONTEND_CONTRACT_MAPPING.md`
5. `docs/FUNDAMENTAL_AI_V4_1_SCENARIO_VALIDATION_REPORT.md`
6. `docs/FUNDAMENTAL_AI_V4_1_FRONTEND_TEST_REPORT.md`
7. `docs/FUNDAMENTAL_AI_V4_1_FRONTEND_REMAINING_GAPS.md`
8. `docs/evidence/v4_1_latest_ui/README.md`
9. `docs/evidence/v4_1_latest_ui/browser-qa.json`

## Source Priority

1. Final frozen product contract for semantics and fail-closed behavior.
2. Latest approved Figma file `rdMYmsAvZYkXHJX8hdl7UN` for visual structure.
3. Merged v4.1 APIs for runtime data ownership.
4. Existing code only as an implementation asset.

Required Figma nodes: `28:154`, `31:23`, `520:212`, `523:748`, `35:97`, `35:4`, `35:35`, `35:66`. Node `519:3` is explicitly rejected as the target.

## Independent Audit Checklist

### Scope And Production Path

- Confirm no schema/migration, Backend algorithm, API contract, Mobile, or Figma file changed in the latest UI replacement.
- Confirm `data-latest-approved-home` is the sole active Desktop Home production subtree.
- Confirm legacy layer/tiles/position-execution/three-card DOM and renderers are absent from that subtree.
- Search for automatic open/close/add/reduce/reverse/order capability; expected count is zero.

### Visual Contract

- Compare the current-code browser captures to the latest Figma component sources, not old node `519:3`.
- Confirm module order, restrained Desktop workspace expression, light/dark tokens and no fake chart.
- Confirm Position/Execution width ratio is approximately `70:30`.
- Confirm horizontal/text overflow, top-level overlap, console error/warning, unhandled rejection and detached visual state are all zero.

### Dynamic Top6 And Asset Context

- Verify all management actions use existing Asset Pool APIs.
- Verify Home retains backend ranking order, limits to six, and never ranks/fills/fixes symbols in JavaScript.
- Verify the real search input accepts text and Pool controls are operative.
- Verify asset switching updates only Final Plan, Three AI, Consistency and selected-asset context.
- Verify System Status, alerts/events, Top6 and User Positions do not change during selection.

### Position And Final Plan

- Verify Position Monitoring uses P1-KD judgment/facts/basis semantics and Top3.
- Verify No Position and untrusted-monitor states contain no fake row, risk, PnL, conclusion or action.
- Verify the Final body opens only for validated Final/source/chain/not-trade gates.
- Verify Candidate cannot be exposed as Final and Execution Plan remains separate from User Position.

### Three AI And Consistency

- Verify one workspace, three tabs and one visible role.
- Verify GPT/Gemini/Grok structured fields remain role-specific.
- Verify role state and collection state remain independent.
- Verify no cross-role fallback, generated evidence, vote, percentage, chart, or fourth role.

### Validation

Re-run:

```text
./mvnw test -q
bash scripts/product-source-gate.sh
bash scripts/check-workflow-contract.sh
python3 -m py_compile scripts/dashboard-visual-acceptance-fixture.py
git diff --check
```

Review all 20 current-code images under `docs/evidence/v4_1_latest_ui/runtime/` and verify `browser-qa.json` against the production DOM.

## Decision Boundary

Allowed independent-audit result:

- `APPROVE`
- `REQUEST_CHANGES`

Approval must not claim live-provider or merged-main acceptance. `TARGET_RUNTIME_EVIDENCE_PENDING` remains explicit.

No merge, Mobile work, Figma change, next product package, or automatic-trading expansion is allowed inside this handoff.
