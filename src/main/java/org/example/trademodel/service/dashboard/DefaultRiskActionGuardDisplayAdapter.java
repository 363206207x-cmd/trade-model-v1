package org.example.trademodel.service.dashboard;

import org.example.trademodel.vo.DashboardDetailResponseVO;
import org.example.trademodel.vo.DecisionResultVO;
import org.springframework.stereotype.Component;

/**
 * Fail-closed dashboard display adapter for Risk Action Guard status.
 * The first phase only maps read-only safety state and never triggers trading actions.
 */
@Component
public class DefaultRiskActionGuardDisplayAdapter implements RiskActionGuardDisplayAdapter {
    private static final String BACKEND_PENDING = "BACKEND_PENDING";
    private static final String VALID = "VALID";
    private static final String READY_REVIEW_ONLY = "READY_REVIEW_ONLY";
    private static final String MANUAL_REVIEW_REQUIRED = "MANUAL_REVIEW_REQUIRED";

    private static final String LABEL_BACKEND_PENDING = "后端未接入";
    private static final String LABEL_MANUAL_REVIEW_REQUIRED = "需要人工复核";

    private static final String DECISION_MISSING = "DECISION_MISSING";
    private static final String LIQUIDITY_CONTEXT_MISSING = "LIQUIDITY_CONTEXT_MISSING";
    private static final String PLAN_BOUNDARY_NOT_VALID = "PLAN_BOUNDARY_NOT_VALID";
    private static final String EXECUTION_PLAN_NOT_READY = "EXECUTION_PLAN_NOT_READY";

    @Override
    public DashboardDetailResponseVO.RiskActionGuardDisplayVO build(
            DecisionResultVO decision,
            DashboardDetailResponseVO.PlanBoundaryDisplayVO planBoundaryDisplay,
            DashboardDetailResponseVO.ExecutionPlanDisplayVO executionPlanDisplay,
            DashboardDetailResponseVO.RiskActionGuardDisplayVO fallbackDisplay
    ) {
        DashboardDetailResponseVO.RiskActionGuardDisplayVO display = fallbackDisplay != null
                ? fallbackDisplay
                : new DashboardDetailResponseVO.RiskActionGuardDisplayVO();
        enforceFailClosed(display);

        if (decision == null) {
            display.setRiskActionBlockingReason(DECISION_MISSING);
            return display;
        }

        if (!isPlanBoundaryValid(planBoundaryDisplay)) {
            display.setRiskActionBlockingReason(PLAN_BOUNDARY_NOT_VALID);
            return display;
        }

        if (!isExecutionPlanReviewOnly(executionPlanDisplay)) {
            display.setRiskActionBlockingReason(EXECUTION_PLAN_NOT_READY);
            return display;
        }

        if (isHighRisk(decision) && isLiquidityContextMissing(display)) {
            markManualReview(display, LIQUIDITY_CONTEXT_MISSING);
            return display;
        }

        markManualReview(display, MANUAL_REVIEW_REQUIRED);
        return display;
    }

    private boolean isPlanBoundaryValid(DashboardDetailResponseVO.PlanBoundaryDisplayVO planBoundaryDisplay) {
        return planBoundaryDisplay != null && VALID.equalsIgnoreCase(planBoundaryDisplay.getPlanBoundaryStatus());
    }

    private boolean isExecutionPlanReviewOnly(DashboardDetailResponseVO.ExecutionPlanDisplayVO executionPlanDisplay) {
        return executionPlanDisplay != null
                && READY_REVIEW_ONLY.equalsIgnoreCase(executionPlanDisplay.getExecutionPlanStatus());
    }

    private boolean isHighRisk(DecisionResultVO decision) {
        return "HIGH".equalsIgnoreCase(decision.getRiskLevel())
                || "EXTREME".equalsIgnoreCase(decision.getRiskLevel());
    }

    private boolean isLiquidityContextMissing(DashboardDetailResponseVO.RiskActionGuardDisplayVO display) {
        return display == null
                || isBlank(display.getLiquidityState())
                || BACKEND_PENDING.equalsIgnoreCase(display.getLiquidityState());
    }

    private void markManualReview(DashboardDetailResponseVO.RiskActionGuardDisplayVO display, String reason) {
        display.setRiskActionGuardStatus(MANUAL_REVIEW_REQUIRED);
        display.setRiskActionGuardStatusLabel(LABEL_MANUAL_REVIEW_REQUIRED);
        display.setRiskActionBlockingReason(reason);
        enforceFailClosed(display);
    }

    private void enforceFailClosed(DashboardDetailResponseVO.RiskActionGuardDisplayVO display) {
        if (isBlank(display.getRiskActionGuardStatus())) {
            display.setRiskActionGuardStatus(BACKEND_PENDING);
        }
        if (isBlank(display.getRiskActionGuardStatusLabel())) {
            display.setRiskActionGuardStatusLabel(LABEL_BACKEND_PENDING);
        }
        if (isBlank(display.getLiquidityState())) {
            display.setLiquidityState(BACKEND_PENDING);
        }
        display.setStampedeDetected(Boolean.TRUE.equals(display.getStampedeDetected()));
        display.setWickOnlyRisk(Boolean.TRUE.equals(display.getWickOnlyRisk()));
        display.setOpportunityPushAllowed(false);
        display.setReverseTradeAllowed(false);
        display.setNewPositionAllowed(false);
        display.setMarketOrderExitAllowed(false);
        display.setManualRiskReviewRequired(true);
        display.setNotTradeInstruction(true);
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
