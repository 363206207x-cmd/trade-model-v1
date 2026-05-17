# PHASE_HOME_P2_DASHBOARD_VISUAL_LAYOUT_ALIGNMENT_RESULT

## 1. Result Object

This document records the HOME-P2 Dashboard Visual Layout Alignment Pack implementation.

Target PR:

- PR #72
- Branch: codex/home-p2-dashboard-visual-layout

HOME-P2 stays limited to dashboard visual layout alignment. It does not start HOME-P3, P19, Coinglass, order API, or automated trading work.

## 2. Changed Files

Final package files:

- src/main/resources/templates/dashboard.html
- docs/PHASE_HOME_P2_DASHBOARD_VISUAL_LAYOUT_ALIGNMENT_RESULT.md

Removed from the final PR package:

- docs/PHASE_HOME_P2_CLOUD_TRIGGER.md

The removed file was only a temporary Codex Cloud trigger artifact and should not be merged into main.

## 3. Visual Layout Alignment Summary

Implemented a decision-center style homepage hierarchy while preserving existing backend contracts.

The dashboard now includes:

- left-side My Watch / 我的关注 sidebar on desktop
- sidebar search synced with the existing top search field
- Display Slots list in the sidebar
- sidebar system status summary
- sidebar alert center
- review center link
- compact top KPI row
- structured Display Slots placeholders when no decision data exists
- structured main workbench empty state
- diagnostics kept below the main reading area

## 4. Compact KPI Row

The top KPI row was changed from broad status cards to compact decision-center KPIs:

- 市场趋势
- 系统风险等级
- 数据质量分
- AI 冲突等级
- 可复核机会
- Confused
- Hot Reset

Missing backend fields show safe placeholders such as:

- —
- 待后端字段
- 暂无

HOME-P2 does not infer or fabricate live prices, scores, opportunities, or AI results.

## 5. Display Slots Empty State

When no decision data is available, Display Slots render structured placeholder cards for:

- BTCUSDT
- ETHUSDT
- SOLUSDT
- BNBUSDT
- XRPUSDT
- DOGEUSDT

Each placeholder explicitly states:

- 等待首轮分析
- 暂无真实决策
- 非交易指令
- 不伪造价格 / 评分 / 机会

These placeholders are display-only and do not create Watchlist Pool candidates.

## 6. Main Workbench Empty State

The HOME-P1 selected-asset main workbench now has a structured no-data state.

The empty state shows:

- current default display slot
- waiting for first analysis
- PlanBoundary = BACKEND_PENDING
- ExecutionPlan = BOUNDARY_PENDING
- RiskActionGuard = READ_ONLY_PLACEHOLDER
- SourceTrace = INCOMPLETE
- trading semantics = not-trade-instruction
- next manual review focus

BoundaryCandidate VALID remains a review candidate only.

ExecutionPlan readiness remains review-only / advisory and does not become executable.

## 7. Safety Boundaries Preserved

Confirmed HOME-P2 keeps the existing hard boundaries:

- no HOME-P3
- no P19
- no Coinglass API integration
- no external API / HTTP client / API keys
- no order API
- no automated trading
- no executable order behavior
- no real entry / stop / take-profit numeric values
- no fabricated live prices, scores, opportunities, or AI results
- no schema changes
- no backend business logic changes
- no Watchlist Pool semantic changes
- Display Slots remain homepage display slots only
- Watchlist Pool remains the push candidate boundary
- VALID remains manual-review / not-trade-instruction
- high risk does not directly mean stop-loss, reverse, or new open
- wick / pin-bar does not mean confirmed trend reversal
- stampede / liquidity stress continues to block new entry, reverse, and opportunity-push behavior

## 8. Verification Commands

Executed for this package:

```bash
node --check /private/tmp/dashboard-inline-home-p2.js
./mvnw -q -DskipTests compile
./mvnw -q -DskipTests test-compile
./mvnw -q -Dtest=DashboardControllerTest test
```

Result:

- dashboard inline script syntax check PASS
- compile PASS
- test-compile PASS
- DashboardControllerTest PASS

## 9. Current Conclusion

HOME-P2 is a frontend presentation and empty-state alignment package only.

It improves the homepage hierarchy and no-data experience while keeping all risk, execution, and trading boundaries fail-closed and review-only.
