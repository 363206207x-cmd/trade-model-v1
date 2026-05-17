# PHASE_HOME_P2A_DASHBOARD_VISUAL_POLISH_DECISION_SECTIONS_RESULT

## 1. Result Object

This document records the HOME-P2A Dashboard Visual Polish and Decision Sections Pack implementation.

Target PR:

- PR #74
- Branch: codex/home-p2a-dashboard-visual-polish

Authoritative task source:

- Issue #73: HOME-P2A Dashboard Visual Polish and Decision Sections Pack

HOME-P2A stays limited to dashboard visual polish and decision-section presentation shells. It does not start HOME-P3, P19, Coinglass, external API, order API, live execution, or automated trading work.

## 2. Changed Files

Final package files:

- src/main/resources/templates/dashboard.html
- docs/PHASE_HOME_P2A_DASHBOARD_VISUAL_POLISH_DECISION_SECTIONS_RESULT.md

Removed from the final PR package:

- docs/PHASE_HOME_P2A_CLOUD_TRIGGER.md

The removed file was only a temporary Codex Cloud trigger artifact and should not be merged into main.

## 3. HOME-P2A Visual Polish Summary

Implemented dashboard visual polish on top of the HOME-P2 layout while preserving existing backend contracts.

The dashboard now includes:

- stronger KPI primary value / secondary note hierarchy
- lightweight KPI visual anchors using CSS and text only
- shorter, more scannable asset tiles
- asset tile rows for risk, manual attention, and suggestion summary
- selected-asset main workbench with decision-section shells
- lower visual shells for open-position monitoring, execution suggestion, and AI role convergence
- explicit placeholder wording when data is unavailable
- diagnostics kept below the main reading area

## 4. Asset Tile Readability

Asset tiles were tightened so long summary text no longer dominates the tile.

Each real-data tile keeps the key scan fields:

- symbol
- directional bias
- confidence badge
- risk level
- manual attention state
- suggestion summary
- price line
- compact status tags

Long conclusion summaries remain available in the main workbench and detail sections, not inside the tile body.

No live price, score, opportunity, or AI result is fabricated when fields are missing.

## 5. KPI Hierarchy Polish

The KPI cards now emphasize:

- primary value
- secondary note
- visual anchor

The KPI row remains based on existing dashboard summary fields only:

- market trend
- system risk
- data quality
- AI conflict
- reviewable opportunities
- Confused
- Hot Reset

Missing backend fields still render safe placeholders such as:

- —
- 待后端字段
- 暂无

## 6. Main Workbench Decision Panel

The selected-asset main workbench remains the primary decision-reading panel.

It continues to show:

- conclusion and manual attention
- PlanBoundary status
- SourceTrace status
- backend connection state
- ExecutionPlan review-only status
- RiskActionGuard state
- liquidity state
- manual review requirement
- not-trade-instruction semantics
- next manual review focus

The panel now adds three decision-section shells under the main workbench grid.

## 7. Decision Section Shells

HOME-P2A adds or improves visual shells for:

- 已开仓监控 / Open position monitoring
- 执行建议 / Execution suggestion
- AI 三方裁决 / AI role convergence

These shells use existing fields only.

When data is missing, they show structured placeholders:

- 暂无真实持仓
- 等待后端 ExecutionPlan
- AI role results pending or unavailable
- SourceTrace / PlanBoundary 未齐套
- 非交易指令
- manual review required

The shells are visual reading surfaces only. They do not generate entry, stop, take-profit, execution, order, close, reverse, or open-position instructions.

## 8. Safety Boundaries Preserved

Confirmed HOME-P2A keeps the hard boundaries:

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
- no fabricated live prices, scores, opportunities, or AI results
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
node --check /private/tmp/dashboard-inline-home-p2a.js
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

HOME-P2A is a dashboard presentation polish package only.

It improves scan density, KPI hierarchy, asset tile readability, and lower decision-section shells while keeping all source, risk, execution, and trading boundaries fail-closed and review-only.

HOME-P2A does not introduce backend logic, external data, execution readiness, order routing, or automated trading behavior.
