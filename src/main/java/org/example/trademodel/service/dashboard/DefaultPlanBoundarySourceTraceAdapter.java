package org.example.trademodel.service.dashboard;

import org.example.trademodel.vo.DashboardDetailResponseVO;
import org.example.trademodel.vo.DecisionResultVO;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Fail-closed source trace readiness adapter for PlanBoundary display.
 * This phase does not depend on BoundaryCandidateDTO or RuntimeKlineContextDTO because they are not present on main.
 */
@Component
public class DefaultPlanBoundarySourceTraceAdapter implements PlanBoundarySourceTraceAdapter {
    private static final String BACKEND_PENDING = "BACKEND_PENDING";
    private static final String INCOMPLETE = "INCOMPLETE";
    private static final String MISSING = "MISSING";
    private static final String PARTIAL = "PARTIAL";

    private static final String LABEL_BACKEND_PENDING = "后端未接入";
    private static final String LABEL_INCOMPLETE = "信息不完整";

    private static final String DECISION_MISSING = "DECISION_MISSING";
    private static final String SOURCE_TRACE_INPUT_NOT_AVAILABLE = "SOURCE_TRACE_INPUT_NOT_AVAILABLE";
    private static final String BOUNDARY_CANDIDATE_DTO_MISSING = "BOUNDARY_CANDIDATE_DTO_MISSING";
    private static final String RUNTIME_KLINE_CONTEXT_DTO_MISSING = "RUNTIME_KLINE_CONTEXT_DTO_MISSING";
    private static final String READ_MODEL_PARTIAL = "READ_MODEL_PARTIAL";

    @Override
    public DashboardDetailResponseVO.PlanBoundaryDisplayVO build(
            String symbol,
            DecisionResultVO decision,
            DashboardDetailResponseVO.PlanBoundaryDisplayVO fallbackDisplay
    ) {
        DashboardDetailResponseVO.PlanBoundaryDisplayVO display = fallbackDisplay != null
                ? fallbackDisplay
                : new DashboardDetailResponseVO.PlanBoundaryDisplayVO();
        enforceSafety(display);
        ensureReasonLists(display);

        if (decision == null) {
            markBackendPending(display, DECISION_MISSING);
            return display;
        }

        markSourceTraceIncomplete(display);
        if ("PARTIAL".equalsIgnoreCase(decision.getReadModelTruthStatus())) {
            addUnique(display.getIncompleteReasons(), READ_MODEL_PARTIAL);
            addUniqueIfNotBlank(display.getBlockingReasons(), decision.getReadModelFallbackReason());
        }
        return display;
    }

    private void markBackendPending(DashboardDetailResponseVO.PlanBoundaryDisplayVO display, String reason) {
        display.setPlanBoundaryStatus(BACKEND_PENDING);
        display.setPlanBoundaryStatusLabel(LABEL_BACKEND_PENDING);
        display.setSourceTraceStatus(BACKEND_PENDING);
        display.setBackendConnectionStatus(BACKEND_PENDING);
        addUnique(display.getBlockingReasons(), reason);
        enforceSafety(display);
    }

    private void markSourceTraceIncomplete(DashboardDetailResponseVO.PlanBoundaryDisplayVO display) {
        display.setPlanBoundaryStatus(INCOMPLETE);
        display.setPlanBoundaryStatusLabel(LABEL_INCOMPLETE);
        display.setSourceTraceStatus(MISSING);
        display.setBackendConnectionStatus(PARTIAL);
        addUnique(display.getIncompleteReasons(), SOURCE_TRACE_INPUT_NOT_AVAILABLE);
        addUnique(display.getBlockingReasons(), BOUNDARY_CANDIDATE_DTO_MISSING);
        addUnique(display.getBlockingReasons(), RUNTIME_KLINE_CONTEXT_DTO_MISSING);
        enforceSafety(display);
    }

    private void ensureReasonLists(DashboardDetailResponseVO.PlanBoundaryDisplayVO display) {
        if (display.getIncompleteReasons() == null) {
            display.setIncompleteReasons(new ArrayList<>());
        }
        if (display.getBlockingReasons() == null) {
            display.setBlockingReasons(new ArrayList<>());
        }
    }

    private void enforceSafety(DashboardDetailResponseVO.PlanBoundaryDisplayVO display) {
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

    private void addUniqueIfNotBlank(List<String> reasons, String reason) {
        addUnique(reasons, reason);
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
