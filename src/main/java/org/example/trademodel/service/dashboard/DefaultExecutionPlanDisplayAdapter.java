package org.example.trademodel.service.dashboard;

import org.example.trademodel.vo.DashboardDetailResponseVO;
import org.example.trademodel.vo.DecisionResultVO;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Fail-closed dashboard display adapter for ExecutionPlan status.
 * The first phase maps only status/reason fields from PlanBoundary display and never produces entry/stop/take-profit values.
 */
@Component
public class DefaultExecutionPlanDisplayAdapter implements ExecutionPlanDisplayAdapter {
    private static final String BACKEND_PENDING = "BACKEND_PENDING";
    private static final String BOUNDARY_PENDING = "BOUNDARY_PENDING";
    private static final String INCOMPLETE = "INCOMPLETE";
    private static final String WATCH_ONLY = "WATCH_ONLY";
    private static final String INVALID = "INVALID";
    private static final String VALID = "VALID";
    private static final String READY_REVIEW_ONLY = "READY_REVIEW_ONLY";

    private static final String LABEL_BOUNDARY_PENDING = "等待边界接入";
    private static final String LABEL_INCOMPLETE = "执行计划不完整";
    private static final String LABEL_WATCH_ONLY = "仅观察";
    private static final String LABEL_INVALID = "计划已失效";
    private static final String LABEL_READY_REVIEW_ONLY = "可复核摘要";

    private static final String REASON_BOUNDARY_PENDING = "PLAN_BOUNDARY_BACKEND_PENDING";
    private static final String REASON_BOUNDARY_INCOMPLETE = "PLAN_BOUNDARY_INCOMPLETE";
    private static final String REASON_BOUNDARY_WATCH_ONLY = "PLAN_BOUNDARY_WATCH_ONLY";
    private static final String REASON_BOUNDARY_INVALID = "PLAN_BOUNDARY_INVALID";
    private static final String REASON_MANUAL_REVIEW_REQUIRED = "MANUAL_REVIEW_REQUIRED";

    @Override
    public DashboardDetailResponseVO.ExecutionPlanDisplayVO build(
            DecisionResultVO decision,
            DashboardDetailResponseVO.PlanBoundaryDisplayVO planBoundaryDisplay,
            DashboardDetailResponseVO.ExecutionPlanDisplayVO fallbackDisplay
    ) {
        DashboardDetailResponseVO.ExecutionPlanDisplayVO display = fallbackDisplay != null
                ? fallbackDisplay
                : new DashboardDetailResponseVO.ExecutionPlanDisplayVO();
        enforceSafetyFlags(display);
        ensureReasonList(display);
        if (decision != null) {
            display.setExecutionPlanSummary(decision.getExecutionPlanSummary());
        }

        if (planBoundaryDisplay == null || isBlank(planBoundaryDisplay.getPlanBoundaryStatus())) {
            markNotAligned(display, BACKEND_PENDING, BOUNDARY_PENDING, LABEL_BOUNDARY_PENDING, REASON_BOUNDARY_PENDING);
            return display;
        }

        String boundaryStatus = planBoundaryDisplay.getPlanBoundaryStatus().trim().toUpperCase();
        if (VALID.equals(boundaryStatus)) {
            display.setExecutionPlanStatus(READY_REVIEW_ONLY);
            display.setExecutionPlanStatusLabel(LABEL_READY_REVIEW_ONLY);
            display.setExecutionPlanBoundaryAligned(true);
            display.setPlanBoundaryStatus(VALID);
            display.setNotExecutableReason(REASON_MANUAL_REVIEW_REQUIRED);
            addUnique(display.getIncompleteReasons(), REASON_MANUAL_REVIEW_REQUIRED);
            enforceSafetyFlags(display);
            return display;
        }

        if (INCOMPLETE.equals(boundaryStatus)) {
            markNotAligned(display, INCOMPLETE, INCOMPLETE, LABEL_INCOMPLETE, REASON_BOUNDARY_INCOMPLETE);
            inheritBoundaryReasons(display, planBoundaryDisplay);
            return display;
        }

        if (WATCH_ONLY.equals(boundaryStatus)) {
            markNotAligned(display, WATCH_ONLY, WATCH_ONLY, LABEL_WATCH_ONLY, REASON_BOUNDARY_WATCH_ONLY);
            return display;
        }

        if (INVALID.equals(boundaryStatus)) {
            markNotAligned(display, INVALID, INVALID, LABEL_INVALID, REASON_BOUNDARY_INVALID);
            return display;
        }

        markNotAligned(display, boundaryStatus, BOUNDARY_PENDING, LABEL_BOUNDARY_PENDING, REASON_BOUNDARY_PENDING);
        return display;
    }

    private void markNotAligned(
            DashboardDetailResponseVO.ExecutionPlanDisplayVO display,
            String planBoundaryStatus,
            String executionPlanStatus,
            String executionPlanStatusLabel,
            String reason
    ) {
        display.setPlanBoundaryStatus(planBoundaryStatus);
        display.setExecutionPlanStatus(executionPlanStatus);
        display.setExecutionPlanStatusLabel(executionPlanStatusLabel);
        display.setExecutionPlanBoundaryAligned(false);
        display.setNotExecutableReason(reason);
        ensureReasonList(display);
        addUnique(display.getIncompleteReasons(), reason);
        enforceSafetyFlags(display);
    }

    private void inheritBoundaryReasons(
            DashboardDetailResponseVO.ExecutionPlanDisplayVO display,
            DashboardDetailResponseVO.PlanBoundaryDisplayVO planBoundaryDisplay
    ) {
        if (planBoundaryDisplay.getIncompleteReasons() == null) {
            return;
        }
        for (String reason : planBoundaryDisplay.getIncompleteReasons()) {
            addUnique(display.getIncompleteReasons(), reason);
        }
    }

    private void ensureReasonList(DashboardDetailResponseVO.ExecutionPlanDisplayVO display) {
        if (display.getIncompleteReasons() == null) {
            display.setIncompleteReasons(new ArrayList<>());
        }
    }

    private void enforceSafetyFlags(DashboardDetailResponseVO.ExecutionPlanDisplayVO display) {
        display.setManualReviewRequired(true);
        display.setNotTradeInstruction(true);
    }

    private void addUnique(List<String> reasons, String reason) {
        if (reasons == null || isBlank(reason)) {
            return;
        }
        if (!reasons.contains(reason)) {
            reasons.add(reason);
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
