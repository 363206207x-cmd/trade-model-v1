# PHASE_HOME_P1_DASHBOARD_MAIN_WORKBENCH_CONSOLIDATION_RESULT

## 1. Result Object

This document records the HOME-P1 Dashboard Main Workbench Consolidation Pack implementation.

Target PR:

- PR #70
- Branch: codex/home-p1-dashboard-main-workbench

HOME-P1 stays limited to dashboard homepage consolidation. It does not start HOME-P2 or P19.

## 2. Changed Files

Final package files:

- src/main/resources/templates/dashboard.html
- docs/PHASE_HOME_P1_DASHBOARD_MAIN_WORKBENCH_CONSOLIDATION_RESULT.md

Removed from the final PR package:

- docs/PHASE_HOME_P1_CLOUD_TRIGGER.md

The removed file was only a temporary Codex Cloud trigger artifact and should not be merged into main.

## 3. Dashboard Consolidation Summary

Implemented a selected-asset main workbench near the top of the dashboard detail area.

The workbench uses existing dashboard summary and detail API fields only:

- current conclusion summary
- symbol and price
- market bias
- manual attention decision
- recommendation copy
- confidence level
- risk level
- market environment mini summary
- PlanBoundary display state
- SourceTrace status
- backend connection state
- ExecutionPlan display state
- not-executable reason
- RiskActionGuard display state
- liquidity state
- manual review and not-trade-instruction flags
- next manual review focus

## 4. Display Slots / Watchlist Boundary

The dashboard now labels the top asset strip as:

- Display Slots / 首页展示位

The copy explicitly distinguishes:

- Display Slots: homepage reading priority
- Watchlist Pool: push candidate boundary

HOME-P1 does not change Watchlist Pool semantics.

## 5. Diagnostics Downshift

Technical module/status cards were moved below the main detail area into a diagnostics section.

Downshifted diagnostics include:

- module access status
- PlanBoundary display status
- RiskActionGuard display status

This keeps the top of the dashboard focused on selected-asset review rather than technical placeholders.

## 6. Safety Boundaries Preserved

Confirmed HOME-P1 keeps the existing safety boundaries:

- no external API integration
- no Coinglass integration
- no order API
- no automated trading
- no executable order behavior
- no real entry / stop / TP numeric values
- no schema changes
- no backend business logic changes
- no dashboard API changes
- no Watchlist Pool semantic changes
- VALID remains manual-review / not-trade-instruction
- missing SourceTrace / derivatives-risk context remains fail-closed

BoundaryCandidate VALID remains a review candidate.

ExecutionPlan readiness remains review-only / advisory and does not become executable.

## 7. Verification Commands

Executed for this package:

```bash
node --check /private/tmp/dashboard-inline-home-p1.js
./mvnw -q -DskipTests compile
./mvnw -q -DskipTests test-compile
./mvnw -q -Dtest=DashboardControllerTest test
```

Result:

- dashboard inline script syntax check PASS
- compile PASS
- test-compile PASS
- DashboardControllerTest PASS

## 8. Current Conclusion

HOME-P1 is a dashboard presentation consolidation only.

The final PR package removes the temporary cloud trigger artifact and keeps the branch focused on dashboard main workbench consolidation.
