package org.example.trademodel.service.dashboard;

import org.example.trademodel.dto.planboundary.SourceTraceDTO;
import org.example.trademodel.vo.DashboardDetailResponseVO;
import org.example.trademodel.vo.DecisionResultVO;

/**
 * Read-only adapter for building dashboard ExecutionPlan display state.
 * This adapter must stay gated by PlanBoundary display status and must not generate trading instructions.
 */
public interface ExecutionPlanDisplayAdapter {

    DashboardDetailResponseVO.ExecutionPlanDisplayVO build(
            DecisionResultVO decision,
            DashboardDetailResponseVO.PlanBoundaryDisplayVO planBoundaryDisplay,
            DashboardDetailResponseVO.ExecutionPlanDisplayVO fallbackDisplay
    );

    default DashboardDetailResponseVO.ExecutionPlanDisplayVO build(
            DecisionResultVO decision,
            DashboardDetailResponseVO.PlanBoundaryDisplayVO planBoundaryDisplay,
            DashboardDetailResponseVO.ExecutionPlanDisplayVO fallbackDisplay,
            SourceTraceDTO sourceTrace
    ) {
        return build(decision, planBoundaryDisplay, fallbackDisplay);
    }
}
