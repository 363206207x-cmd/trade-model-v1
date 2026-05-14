package org.example.trademodel.service.dashboard;

import org.example.trademodel.vo.DashboardDetailResponseVO;
import org.example.trademodel.vo.DecisionResultVO;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Fail-closed dashboard display adapter for PlanBoundary status.
 * The first phase only maps safe status/reason fields and never produces entry/stop/take-profit values.
 */
@Component
public class DefaultPlanBoundaryDisplayAdapter implements PlanBoundaryDisplayAdapter {
    private static final String BACKEND_PENDING = "BACKEND_PENDING";
    private static final String INCOMPLETE = "INCOMPLETE";
    private static final String PARTIAL = "PARTIAL";
    private static final String MISSING = "MISSING";

    private static final String LABEL_BACKEND_PENDING = "后端未接入";
    private static final String LABEL_INCOMPLETE = "信息不完整";

    private static final String DECISION_MISSING = "DECISION_MISSING";
    private static final String ANALYSIS_ID_MISSING = "ANALYSIS_ID_MISSING";
    private static final String READ_MODEL_PARTIAL = "READ_MODEL_PARTIAL";
    private static final String SOURCE_TRACE_PENDING = "SOURCE_TRACE_PENDING";

    @Override
    public DashboardDetailResponseVO.PlanBoundaryDisplayVO build(
            String symbol,
            DecisionResultVO decision,
            DashboardDetailResponseVO.PlanBoundaryDisplayVO fallbackDisplay
    ) {
        DashboardDetailResponseVO.PlanBoundaryDisplayVO display = fallbackDisplay != null
                ? fallbackDisplay
                : new DashboardDetailResponseVO.PlanBoundaryDisplayVO();
        enforceSafetyFlags(display);

        if (decision == null) {
            resetToBackendPending(display);
            addUnique(display.getBlockingReasons(), DECISION_MISSING);
            return display;
        }

        if (isBlank(decision.getAnalysisId())) {
            markIncomplete(display, ANALYSIS_ID_MISSING);
            return display;
        }

        if (PARTIAL.equalsIgnoreCase(decision.getReadModelTruthStatus())) {
            markIncomplete(display, READ_MODEL_PARTIAL);
            if (!isBlank(decision.getReadModelFallbackReason())) {
                addUnique(display.getBlockingReasons(), decision.getReadModelFallbackReason());
            }
            return display;
        }

        if (hasTextOnlyExecutionPlan(decision)) {
            markIncomplete(display, SOURCE_TRACE_PENDING);
            return display;
        }

        resetToBackendPending(display);
        return display;
    }

    private void resetToBackendPending(DashboardDetailResponseVO.PlanBoundaryDisplayVO display) {
        display.setPlanBoundaryStatus(BACKEND_PENDING);
        display.setPlanBoundaryStatusLabel(LABEL_BACKEND_PENDING);
        display.setSourceTraceStatus(BACKEND_PENDING);
        display.setBackendConnectionStatus(BACKEND_PENDING);
        ensureReasonLists(display);
        enforceSafetyFlags(display);
    }

    private void markIncomplete(DashboardDetailResponseVO.PlanBoundaryDisplayVO display, String reason) {
        display.setPlanBoundaryStatus(INCOMPLETE);
        display.setPlanBoundaryStatusLabel(LABEL_INCOMPLETE);
        display.setSourceTraceStatus(MISSING);
        display.setBackendConnectionStatus(PARTIAL);
        ensureReasonLists(display);
        addUnique(display.getIncompleteReasons(), reason);
        enforceSafetyFlags(display);
    }

    private void ensureReasonLists(DashboardDetailResponseVO.PlanBoundaryDisplayVO display) {
        if (display.getIncompleteReasons() == null) {
            display.setIncompleteReasons(new ArrayList<>());
        }
        if (display.getBlockingReasons() == null) {
            display.setBlockingReasons(new ArrayList<>());
        }
    }

    private void enforceSafetyFlags(DashboardDetailResponseVO.PlanBoundaryDisplayVO display) {
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

    private boolean hasTextOnlyExecutionPlan(DecisionResultVO decision) {
        return !isBlank(decision.getEntryZone())
                || !isBlank(decision.getStopLoss())
                || !isBlank(decision.getTakeProfitRules())
                || !isBlank(decision.getExecutionPlanSummary());
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
