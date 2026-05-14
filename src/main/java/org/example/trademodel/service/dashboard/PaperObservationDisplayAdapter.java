package org.example.trademodel.service.dashboard;

import org.example.trademodel.vo.DashboardDetailResponseVO;
import org.example.trademodel.vo.DecisionResultVO;

/**
 * Read-only adapter for building dashboard PaperObservation display state.
 * This adapter must not create real positions or trading instructions.
 */
public interface PaperObservationDisplayAdapter {

    DashboardDetailResponseVO.PaperObservationDisplayVO build(
            DecisionResultVO decision,
            DashboardDetailResponseVO.PlanBoundaryDisplayVO planBoundaryDisplay,
            DashboardDetailResponseVO.ExecutionPlanDisplayVO executionPlanDisplay,
            DashboardDetailResponseVO.RiskActionGuardDisplayVO riskActionGuardDisplay,
            DashboardDetailResponseVO.PaperObservationDisplayVO fallbackDisplay
    );
}
