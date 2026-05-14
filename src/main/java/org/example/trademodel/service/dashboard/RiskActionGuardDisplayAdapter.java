package org.example.trademodel.service.dashboard;

import org.example.trademodel.vo.DashboardDetailResponseVO;
import org.example.trademodel.vo.DecisionResultVO;

/**
 * Read-only adapter for building dashboard Risk Action Guard display state.
 * This adapter must stay fail-closed and must not trigger trading actions.
 */
public interface RiskActionGuardDisplayAdapter {

    DashboardDetailResponseVO.RiskActionGuardDisplayVO build(
            DecisionResultVO decision,
            DashboardDetailResponseVO.PlanBoundaryDisplayVO planBoundaryDisplay,
            DashboardDetailResponseVO.ExecutionPlanDisplayVO executionPlanDisplay,
            DashboardDetailResponseVO.RiskActionGuardDisplayVO fallbackDisplay
    );
}
