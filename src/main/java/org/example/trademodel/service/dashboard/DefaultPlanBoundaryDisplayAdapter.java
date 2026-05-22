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
    private static final String BLOCKED = "BLOCKED";
    private static final String WATCH_ONLY = "WATCH_ONLY";
    private static final String REVIEW_ONLY = "REVIEW_ONLY";
    private static final String REVIEW_ONLY_CANDIDATE = "REVIEW_ONLY_CANDIDATE";
    private static final String VALID = "VALID";
    private static final String PARTIAL = "PARTIAL";
    private static final String MISSING = "MISSING";

    private static final String LABEL_BACKEND_PENDING = "后端未接入";
    private static final String LABEL_INCOMPLETE = "信息不完整";
    private static final String LABEL_BLOCKED = "禁止推进";
    private static final String LABEL_WATCH_ONLY = "仅观察";
    private static final String LABEL_REVIEW_ONLY = "只允许复核";

    private static final String DECISION_MISSING = "DECISION_MISSING";
    private static final String ANALYSIS_ID_MISSING = "ANALYSIS_ID_MISSING";
    private static final String READ_MODEL_PARTIAL = "READ_MODEL_PARTIAL";
    private static final String SOURCE_TRACE_PENDING = "SOURCE_TRACE_PENDING";
    private static final String REVIEW_MODE_REVIEW_ONLY = "REVIEW_MODE:REVIEW_ONLY";
    private static final String NOT_TRADE_INSTRUCTION = "NOT_TRADE_INSTRUCTION";
    private static final String BOUNDARY_CANDIDATE_STATUS_PREFIX = "BOUNDARY_CANDIDATE_STATUS:";
    private static final String UNSAFE_CANDIDATE_STATUS_PREFIX = "BOUNDARY_CANDIDATE_STATUS_UNSAFE:";

    private final PlanBoundarySourceTraceAdapter sourceTraceAdapter;

    public DefaultPlanBoundaryDisplayAdapter(PlanBoundarySourceTraceAdapter sourceTraceAdapter) {
        this.sourceTraceAdapter = sourceTraceAdapter;
    }

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
        ensureReasonLists(display);

        if (decision == null) {
            resetToBackendPending(display);
            addUnique(display.getBlockingReasons(), DECISION_MISSING);
            return invokeSourceTraceAdapter(symbol, decision, display);
        }

        if (isBlank(decision.getAnalysisId())) {
            markIncomplete(display, ANALYSIS_ID_MISSING);
            return invokeSourceTraceAdapter(symbol, decision, display);
        }

        if (PARTIAL.equalsIgnoreCase(decision.getReadModelTruthStatus())) {
            markIncomplete(display, READ_MODEL_PARTIAL);
            if (!isBlank(decision.getReadModelFallbackReason())) {
                addUnique(display.getBlockingReasons(), decision.getReadModelFallbackReason());
            }
            return invokeSourceTraceAdapter(symbol, decision, display);
        }

        if (hasTextOnlyPlanNarrative(decision)) {
            markIncomplete(display, SOURCE_TRACE_PENDING);
            return invokeSourceTraceAdapter(symbol, decision, display);
        }

        resetToBackendPending(display);
        return invokeSourceTraceAdapter(symbol, decision, display);
    }

    private DashboardDetailResponseVO.PlanBoundaryDisplayVO invokeSourceTraceAdapter(
            String symbol,
            DecisionResultVO decision,
            DashboardDetailResponseVO.PlanBoundaryDisplayVO display
    ) {
        DashboardDetailResponseVO.PlanBoundaryDisplayVO sourceTraceDisplay = sourceTraceAdapter.build(symbol, decision, display);
        DashboardDetailResponseVO.PlanBoundaryDisplayVO result = sourceTraceDisplay != null ? sourceTraceDisplay : display;
        ensureReasonLists(result);
        enforceSafetyFlags(result);
        applyReadOnlyCandidateDisplay(result);
        return result;
    }

    private void applyReadOnlyCandidateDisplay(DashboardDetailResponseVO.PlanBoundaryDisplayVO display) {
        String status = normalizedStatus(display.getPlanBoundaryStatus());
        if (isBlank(status) || BACKEND_PENDING.equals(status)) {
            display.setPlanBoundaryStatus(BACKEND_PENDING);
            display.setPlanBoundaryStatusLabel(LABEL_BACKEND_PENDING);
            return;
        }
        if (VALID.equals(status)) {
            markIncomplete(display, SOURCE_TRACE_PENDING);
            addUnique(display.getBlockingReasons(), UNSAFE_CANDIDATE_STATUS_PREFIX + VALID);
            annotateReadOnlyCandidate(display, INCOMPLETE);
            return;
        }
        if (REVIEW_ONLY_CANDIDATE.equals(status) || REVIEW_ONLY.equals(status)) {
            markReadOnlyCandidate(display, REVIEW_ONLY, LABEL_REVIEW_ONLY);
            return;
        }
        if (INCOMPLETE.equals(status)) {
            display.setPlanBoundaryStatus(INCOMPLETE);
            display.setPlanBoundaryStatusLabel(LABEL_INCOMPLETE);
            display.setBackendConnectionStatus(PARTIAL);
            addUnique(display.getIncompleteReasons(), BOUNDARY_CANDIDATE_STATUS_PREFIX + INCOMPLETE);
            annotateReadOnlyCandidate(display, INCOMPLETE);
            return;
        }
        if (BLOCKED.equals(status)) {
            markReadOnlyCandidate(display, BLOCKED, LABEL_BLOCKED);
            return;
        }
        if (WATCH_ONLY.equals(status)) {
            markReadOnlyCandidate(display, WATCH_ONLY, LABEL_WATCH_ONLY);
            return;
        }
        markIncomplete(display, UNSAFE_CANDIDATE_STATUS_PREFIX + status);
        annotateReadOnlyCandidate(display, INCOMPLETE);
    }

    private void markReadOnlyCandidate(
            DashboardDetailResponseVO.PlanBoundaryDisplayVO display,
            String status,
            String label
    ) {
        display.setPlanBoundaryStatus(status);
        display.setPlanBoundaryStatusLabel(label);
        display.setBackendConnectionStatus(PARTIAL);
        annotateReadOnlyCandidate(display, status);
    }

    private void annotateReadOnlyCandidate(DashboardDetailResponseVO.PlanBoundaryDisplayVO display, String status) {
        ensureReasonLists(display);
        enforceSafetyFlags(display);
        if (INCOMPLETE.equals(status)) {
            addUnique(display.getIncompleteReasons(), BOUNDARY_CANDIDATE_STATUS_PREFIX + INCOMPLETE);
        } else {
            addUnique(display.getBlockingReasons(), BOUNDARY_CANDIDATE_STATUS_PREFIX + status);
        }
        addUnique(display.getBlockingReasons(), REVIEW_MODE_REVIEW_ONLY);
        addUnique(display.getBlockingReasons(), NOT_TRADE_INSTRUCTION);
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

    private boolean hasTextOnlyPlanNarrative(DecisionResultVO decision) {
        return !isBlank(decision.getEntryZone())
                || !isBlank(decision.getStopLoss())
                || !isBlank(decision.getTakeProfitRules())
                || !isBlank(decision.getExecutionPlanSummary());
    }

    private String normalizedStatus(String value) {
        return value == null ? null : value.trim().toUpperCase(java.util.Locale.ROOT);
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
