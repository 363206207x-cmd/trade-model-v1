package org.example.trademodel.service.dashboard;

import org.example.trademodel.vo.DashboardDetailResponseVO;
import org.example.trademodel.vo.DecisionResultVO;

/**
 * Read-only readiness adapter for PlanBoundary source trace state.
 * Current phase must remain fail-closed and must not generate numeric trading boundaries.
 */
public interface PlanBoundarySourceTraceAdapter {

    DashboardDetailResponseVO.PlanBoundaryDisplayVO build(
            String symbol,
            DecisionResultVO decision,
            DashboardDetailResponseVO.PlanBoundaryDisplayVO fallbackDisplay
    );
}
