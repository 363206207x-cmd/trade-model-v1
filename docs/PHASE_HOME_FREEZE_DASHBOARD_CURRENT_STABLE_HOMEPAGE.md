# PHASE_HOME_FREEZE_DASHBOARD_CURRENT_STABLE_HOMEPAGE

## 1. Freeze Object

This document records the HOME-FREEZE closure baseline for the current Trade Model V1 dashboard homepage.

Stable baseline:

- `3820de9 feat(dashboard): polish HOME-P2B first screen`

Current stable homepage scope:

- HOME-P1 Dashboard Main Workbench Consolidation
- HOME-P2 Dashboard Visual Layout Alignment
- HOME-P2A Dashboard Visual Polish and Decision Sections
- HOME-P2B Dashboard First-Screen Polish

The current homepage is accepted as the stable decision-center homepage baseline.

HOME-FREEZE is documentation-only. It does not continue visual redesign, does not start HOME-P3, and does not start P19.

## 2. Completed Homepage Chain

The current stable homepage chain is:

| Phase | Status | Stable Result |
|---|---|---|
| HOME-P1 | Frozen as baseline | Main workbench consolidated selected-asset reading surface. |
| HOME-P2 | Frozen as baseline | Left sidebar, compact KPI row, structured Display Slots empty state, and main workbench empty state. |
| HOME-P2A | Frozen as baseline | KPI polish, cleaner asset tiles, main workbench scan improvements, and lower decision-section shells. |
| HOME-P2B | Frozen as current stable homepage | First-screen polish, asset-card compression, softer visual hierarchy, manual review focus strip, and unified decision shells. |

Supporting result documents:

- `docs/PHASE_HOME_P1_DASHBOARD_MAIN_WORKBENCH_CONSOLIDATION_RESULT.md`
- `docs/PHASE_HOME_P2_DASHBOARD_VISUAL_LAYOUT_ALIGNMENT_RESULT.md`
- `docs/PHASE_HOME_P2A_DASHBOARD_VISUAL_POLISH_DECISION_SECTIONS_RESULT.md`
- `docs/PHASE_HOME_P2B_DASHBOARD_FIRST_SCREEN_POLISH_RESULT.md`

## 3. Current Homepage Modules

The stable homepage contains the following first-screen and dashboard modules:

| Module | Frozen State | Notes |
|---|---|---|
| Left My Watch sidebar | Frozen | Search, Display Slots list, system status, alert center, and review center link. |
| Compact KPI row | Frozen | Shows trend, system risk, data quality, AI conflict, reviewable opportunities, Confused, and Hot Reset. |
| Display Slots asset row | Frozen | Homepage display slots only, capped to the configured display list behavior. |
| Asset cards | Frozen | Compressed first-screen cards with symbol, bias, confidence, risk, manual attention, suggestion summary, price line, and compact status tags. |
| Selected-asset main workbench | Frozen | Review-focused selected-asset reading surface with conclusion, risk, source, and boundary status summaries. |
| Manual Review Focus strip | Frozen | First-read emphasis for the next manual review concern. |
| Open position monitoring shell | Frozen | Visual shell only; shows available fields or safe placeholders. |
| Execution suggestion shell | Frozen | Review-only shell; does not generate executable plans. |
| AI role convergence shell | Frozen | Displays existing AI role fields or safe pending state. |
| Diagnostics downshift section | Frozen | Technical integration status remains below the primary reading area. |

## 4. Safety Boundaries Frozen

The stable homepage remains a display and manual review surface only.

Frozen safety boundaries:

- No external data integration.
- No Coinglass integration.
- No live execution integration.
- No order API.
- No automated trading.
- No executable order behavior.
- No real entry / stop / take-profit numeric generation.
- No fabricated prices.
- No fabricated scores.
- No fabricated opportunities.
- No fabricated charts.
- No fabricated AI outputs.
- No backend business logic change.
- No schema change.
- No dashboard rewrite.
- No Watchlist Pool semantic change.
- No Display Slots semantic change.

Display semantics remain:

- Display Slots are homepage display slots only.
- Watchlist Pool remains the push candidate boundary.
- BoundaryCandidate `VALID` remains manual-review / not-trade-instruction.
- PlanBoundary remains review-only / non-trading in the homepage context.
- ExecutionPlan remains review-only / advisory.
- RiskActionGuard remains fail-closed / review-only.

Risk Action Guard principles remain:

- High risk does not directly imply close, reverse, or new open.
- Wick / pin-bar does not mean confirmed trend reversal.
- Stampede / liquidity stress keeps the UI conservative and review-only.

## 5. Known Remaining Gaps

The frozen homepage is stable, but the following gaps remain outside HOME-FREEZE:

| Gap | Current Frozen Behavior | Future Direction |
|---|---|---|
| Real PlanBoundary fields | Displayed as backend-dependent status / placeholder when missing. | Continue backend completeness work before new homepage redesign. |
| SourceTrace completeness | Missing or incomplete source trace remains fail-closed / placeholder-only. | Improve backend SourceTrace and derivatives-risk context first. |
| ExecutionPlan readiness | Homepage shows review-only / advisory state. | Keep non-executable until backend readiness, RiskActionGuard, and review gates are complete. |
| RiskActionGuard details | Homepage displays fail-closed / review-only safety state. | Continue backend risk-source completeness and guard verification. |
| Real open-position monitor data | Shell remains placeholder when real position data is missing. | Future integration may improve read-only position tracking only. |
| AI role results structure | Existing fields are displayed when present; otherwise pending state remains explicit. | Future structured rendering can happen after data contract stabilization. |
| Coinglass / external API data | Deferred. | Do not add external API until fallback, source trace, and risk boundaries are verified. |

## 6. Freeze Acceptance Criteria

HOME-FREEZE is accepted when:

- The freeze document exists.
- The document records stable baseline `3820de9`.
- The document records HOME-P1, HOME-P2, HOME-P2A, and HOME-P2B as complete.
- The document lists current homepage modules.
- The document preserves all safety boundaries.
- The document lists known remaining gaps.
- The document recommends backend/data-completeness work before further homepage visual changes.
- No source code, template, schema, config, backend logic, API, or dashboard behavior is changed.

## 7. Recommended Next Work After Freeze

Recommended next work should prioritize data and backend completeness over additional homepage redesign:

1. Confirm PlanBoundary / SourceTrace field completeness.
2. Confirm ExecutionPlan readiness remains review-only until all gates are complete.
3. Improve RiskActionGuard source visibility and fail-closed evidence.
4. Improve real open-position monitoring data only as read-only display.
5. Improve structured AI role rendering only from real backend fields.
6. Keep Coinglass / external API work deferred until fallback and source-trace testing are complete.

Do not start HOME-P3 unless there is a specific new homepage requirement.

Do not start P19 from this freeze package.

## 8. Verification

This package is documentation-only.

Tests were not run because no Java, template, schema, config, dashboard behavior, or backend logic files were changed.

Scope verification:

- `src/main/java` unchanged.
- `src/test/java` unchanged.
- `src/main/resources/templates/dashboard.html` unchanged.
- schema unchanged.
- config unchanged.
- backend business logic unchanged.
- no external API integration added.
- no order API added.
- no automated trading added.

## 9. Current Conclusion

The current homepage is frozen at stable baseline `3820de9`.

HOME-P1, HOME-P2, HOME-P2A, and HOME-P2B are complete for the current dashboard homepage baseline.

Further homepage visual redesign should pause until backend data completeness, SourceTrace, PlanBoundary, ExecutionPlan readiness, and RiskActionGuard visibility are improved and verified.

HOME-FREEZE closes the current stable homepage package without changing runtime behavior.
