package org.example.trademodel.service.dashboard;

import org.example.trademodel.vo.DashboardDetailResponseVO;
import org.example.trademodel.vo.DecisionResultVO;
import org.springframework.stereotype.Component;

/**
 * Fail-closed dashboard display adapter for paper observation status.
 * The first phase only maps read-only safety state and never creates real positions.
 */
@Component
public class DefaultPaperObservationDisplayAdapter implements PaperObservationDisplayAdapter {
    private static final String BACKEND_PENDING = "BACKEND_PENDING";
    private static final String VALID = "VALID";
    private static final String READY_REVIEW_ONLY = "READY_REVIEW_ONLY";
    private static final String MANUAL_REVIEW_REQUIRED = "MANUAL_REVIEW_REQUIRED";
    private static final String AVAILABLE_REVIEW_ONLY = "AVAILABLE_REVIEW_ONLY";

    private static final String LABEL_BACKEND_PENDING = "后端未接入";
    private static final String LABEL_MANUAL_REVIEW_REQUIRED = "需要人工复核";
    private static final String LABEL_AVAILABLE_REVIEW_ONLY = "可纸面观察";

    private static final String DECISION_MISSING = "DECISION_MISSING";
    private static final String PLAN_BOUNDARY_NOT_VALID = "PLAN_BOUNDARY_NOT_VALID";
    private static final String EXECUTION_PLAN_NOT_READY = "EXECUTION_PLAN_NOT_READY";
    private static final String RISK_ACTION_GUARD_BLOCKED = "RISK_ACTION_GUARD_BLOCKED";

    @Override
    public DashboardDetailResponseVO.PaperObservationDisplayVO build(
            DecisionResultVO decision,
            DashboardDetailResponseVO.PlanBoundaryDisplayVO planBoundaryDisplay,
            DashboardDetailResponseVO.ExecutionPlanDisplayVO executionPlanDisplay,
            DashboardDetailResponseVO.RiskActionGuardDisplayVO riskActionGuardDisplay,
            DashboardDetailResponseVO.PaperObservationDisplayVO fallbackDisplay
    ) {
        DashboardDetailResponseVO.PaperObservationDisplayVO display = fallbackDisplay != null
                ? fallbackDisplay
                : new DashboardDetailResponseVO.PaperObservationDisplayVO();
        enforceFailClosed(display);

        if (decision == null) {
            display.setReviewSummary(DECISION_MISSING);
            return display;
        }

        if (!isPlanBoundaryValid(planBoundaryDisplay)) {
            display.setReviewSummary(PLAN_BOUNDARY_NOT_VALID);
            return display;
        }

        if (!isExecutionPlanReviewOnly(executionPlanDisplay)) {
            display.setReviewSummary(EXECUTION_PLAN_NOT_READY);
            return display;
        }

        if (isRiskActionGuardBlocked(riskActionGuardDisplay)) {
            display.setReviewSummary(RISK_ACTION_GUARD_BLOCKED);
            return display;
        }

        markManualReview(display);
        return display;
    }

    private boolean isPlanBoundaryValid(DashboardDetailResponseVO.PlanBoundaryDisplayVO planBoundaryDisplay) {
        return planBoundaryDisplay != null && VALID.equalsIgnoreCase(planBoundaryDisplay.getPlanBoundaryStatus());
    }

    private boolean isExecutionPlanReviewOnly(DashboardDetailResponseVO.ExecutionPlanDisplayVO executionPlanDisplay) {
        return executionPlanDisplay != null
                && READY_REVIEW_ONLY.equalsIgnoreCase(executionPlanDisplay.getExecutionPlanStatus());
    }

    private boolean isRiskActionGuardBlocked(DashboardDetailResponseVO.RiskActionGuardDisplayVO riskActionGuardDisplay) {
        if (riskActionGuardDisplay == null) {
            return true;
        }
        if (!MANUAL_REVIEW_REQUIRED.equalsIgnoreCase(riskActionGuardDisplay.getRiskActionGuardStatus())) {
            return true;
        }
        return !isBlank(riskActionGuardDisplay.getRiskActionBlockingReason())
                && !MANUAL_REVIEW_REQUIRED.equalsIgnoreCase(riskActionGuardDisplay.getRiskActionBlockingReason());
    }

    private void markManualReview(DashboardDetailResponseVO.PaperObservationDisplayVO display) {
        display.setPaperObservationStatus(MANUAL_REVIEW_REQUIRED);
        display.setPaperObservationStatusLabel(LABEL_MANUAL_REVIEW_REQUIRED);
        display.setReviewSummary(AVAILABLE_REVIEW_ONLY);
        enforceFailClosed(display);
    }

    private void enforceFailClosed(DashboardDetailResponseVO.PaperObservationDisplayVO display) {
        if (isBlank(display.getPaperObservationStatus())) {
            display.setPaperObservationStatus(BACKEND_PENDING);
        }
        if (isBlank(display.getPaperObservationStatusLabel())) {
            display.setPaperObservationStatusLabel(LABEL_BACKEND_PENDING);
        }
        if (display.getLinkedPaperObservationCount() == null) {
            display.setLinkedPaperObservationCount(0);
        }
        if (display.getLinkedReviewCount() == null) {
            display.setLinkedReviewCount(0);
        }
        display.setPaperObservationAvailable(false);
        display.setManualReviewEntryAvailable(false);
        display.setMissedOpportunityFlag(Boolean.TRUE.equals(display.getMissedOpportunityFlag()));
        display.setNotRealPosition(true);
        display.setNotTradeInstruction(true);
        display.setManualReviewRequired(true);
        if (isBlank(display.getBackendConnectionStatus())) {
            display.setBackendConnectionStatus(BACKEND_PENDING);
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
