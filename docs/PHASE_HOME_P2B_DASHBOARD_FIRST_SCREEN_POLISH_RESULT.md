# PHASE_HOME_P2B_DASHBOARD_FIRST_SCREEN_POLISH_RESULT

## 1. Result Object

This document records the HOME-P2B Dashboard First-Screen Polish Pack implementation.

Target PR:

- PR #76
- Branch: codex/home-p2b-dashboard-first-screen-polish

Authoritative task source:

- Issue #75: HOME-P2B Dashboard First-Screen Polish Pack

HOME-P2B stays limited to first-screen visual polish. It does not start HOME-P3, P19, Coinglass, external API, live execution, order API, or automated trading work.

## 2. Changed Files

Final package files:

- src/main/resources/templates/dashboard.html
- docs/PHASE_HOME_P2B_DASHBOARD_FIRST_SCREEN_POLISH_RESULT.md

Removed from the final PR package:

- docs/PHASE_HOME_P2B_CLOUD_TRIGGER.md

The removed file was only a temporary Codex Cloud trigger artifact and should not be merged into main.

## 3. First-Screen Polish Summary

Implemented first-screen polish on top of the HOME-P2A dashboard.

The package focuses on readability and visual compression only:

- cleaner asset cards
- softer bias borders for repeated bearish / low-confidence states
- stronger KPI primary / secondary hierarchy
- less technical main workbench first read
- more prominent manual review focus
- visually unified lower decision-section shells
- diagnostics kept downshifted

## 4. Asset Card Visual Compression

Asset tiles were compressed without changing data semantics.

Updated presentation:

- reduced card minimum height
- tightened tile padding and row spacing
- softened bullish / bearish border intensity
- softened the top bias strip
- kept compact rows for risk, manual attention, and suggestion summary
- kept compact status tags
- retained price line placeholders

The tile does not reintroduce long conclusion text.

The tile still uses only existing fields:

- symbol
- bias
- confidence
- risk
- manual attention
- suggestion summary
- price line
- compact status tags

## 5. KPI Product Polish

KPI cards were tuned for stronger scan hierarchy.

Updated presentation:

- lower card height
- stronger primary value
- clearer secondary note
- subtle left color rail
- lightweight marker
- no chart or fake trend line

Missing values still render as safe placeholders:

- —
- 待后端字段
- 暂无

No market data, scores, opportunities, charts, or AI results are fabricated.

## 6. Main Workbench Readability

The selected-asset workbench now presents the manual review focus first.

Updated presentation:

- a Manual Review Focus strip is shown at the top of the first workbench panel
- the first panel is framed as 人工复核重点 rather than a technical report
- PlanBoundary / SourceTrace / ExecutionPlan remain visible in a lighter read-only state summary
- RiskActionGuard remains visible but stays review-only and non-trading
- long technical fields are still present, but no longer dominate the first read

The workbench continues to state that:

- BoundaryCandidate VALID remains a review candidate
- ExecutionPlan readiness does not auto-execute
- missing SourceTrace / risk context must fail closed

## 7. Lower Decision Section Shells

The lower decision-section shells remain:

- 已开仓监控 / Open position monitoring
- 执行建议 / Execution suggestion
- AI 三方裁决 / AI role convergence

HOME-P2B visually unified these shells through:

- consistent card radius and spacing
- consistent status badge placement
- quieter card surface
- consistent label / value row density
- explicit review-only notes

The shells remain presentation-only.

They do not generate:

- entry price
- stop price
- take-profit price
- executable ExecutionPlan
- order action
- close position action
- reverse position action
- auto-trading behavior

## 8. Safety Boundaries Preserved

Confirmed HOME-P2B keeps all hard boundaries:

- no HOME-P3
- no P19
- no Coinglass integration
- no external API integration
- no HTTP client or API keys
- no live execution integration
- no order API
- no automated trading
- no executable order behavior
- no real entry / stop / take-profit numeric values
- no fabricated live prices, scores, opportunities, charts, or AI results
- no schema changes
- no backend business logic changes
- no dashboard rewrite
- no Watchlist Pool semantic changes
- no Display Slots business semantic changes
- Display Slots remain homepage display slots only
- Watchlist Pool remains the push candidate boundary
- VALID remains manual-review / not-trade-instruction
- PlanBoundary remains review-only / non-trading in this UI context
- ExecutionPlan remains review-only / advisory
- RiskActionGuard remains fail-closed / review-only

Risk Action Guard principles remain explicit:

- high risk does not directly imply close, reverse, or new open
- wick / pin-bar does not mean confirmed trend reversal
- stampede / liquidity stress keeps the UI conservative and review-only

## 9. Verification Commands

Executed for this package:

```bash
node --check /private/tmp/dashboard-inline-home-p2b.js
./mvnw -q -DskipTests compile
./mvnw -q -DskipTests test-compile
./mvnw -q -Dtest=DashboardControllerTest test
```

Result:

- dashboard inline script syntax check PASS
- compile PASS
- test-compile PASS
- DashboardControllerTest PASS

## 10. Current Conclusion

HOME-P2B is a first-screen presentation polish package only.

It improves asset-card density, KPI readability, main-workbench scan order, and decision-shell visual consistency while keeping all backend, source, execution, and trading boundaries unchanged.

HOME-P2B does not introduce backend logic, external data, chart fabrication, execution readiness, order routing, or automated trading behavior.
