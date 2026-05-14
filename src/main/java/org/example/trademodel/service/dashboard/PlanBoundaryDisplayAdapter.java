package org.example.trademodel.service.dashboard;

import org.example.trademodel.vo.DashboardDetailResponseVO;
import org.example.trademodel.vo.DecisionResultVO;

/**
 * Read-only adapter for building dashboard PlanBoundary display state.
 * This adapter must not generate trading instructions or price boundaries.
 */
public interface PlanBoundaryDisplayAdapter {

    DashboardDetailResponseVO.PlanBoundaryDisplayVO build(
            String symbol,
            DecisionResultVO decision,
            DashboardDetailResponseVO.PlanBoundaryDisplayVO fallbackDisplay
    );
}
