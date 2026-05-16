package org.example.trademodel.service.dashboard;

import org.example.trademodel.dto.planboundary.SourceTraceDTO;
import org.example.trademodel.dto.planboundary.SourceTraceFallbackStatusEnum;
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
    private static final String REASON_SOURCE_TRACE_MISSING = "SOURCE_TRACE_MISSING";
    private static final String REASON_SOURCE_TRACE_INCOMPLETE = "SOURCE_TRACE_INCOMPLETE";
    private static final String REASON_SOURCE_TRACE_WATCH_ONLY = "SOURCE_TRACE_WATCH_ONLY";
    private static final String REASON_SOURCE_TRACE_SAFE_FAIL_CLOSED = "SOURCE_TRACE_SAFE_FAIL_CLOSED_ONLY";
    private static final String REASON_RISK_ACTION_GUARD_MISSING = "RISK_ACTION_GUARD_MISSING";
    private static final String REASON_RISK_ACTION_GUARD_BACKEND_PENDING = "RISK_ACTION_GUARD_BACKEND_PENDING";
    private static final String REASON_HIGH_RISK_REVIEW_ONLY = "HIGH_RISK_REVIEW_ONLY";
    private static final String REASON_LIQUIDITY_CONTEXT_MISSING = "LIQUIDITY_CONTEXT_MISSING";
    private static final String REASON_STAMPEDE_REVIEW_ONLY = "STAMPEDE_RISK_REVIEW_ONLY";
    private static final String REASON_WICK_ONLY_REVIEW_ONLY = "WICK_ONLY_RISK_REVIEW_ONLY";
    private static final String REASON_RISK_ACTION_GUARD_BLOCKED = "RISK_ACTION_GUARD_BLOCKED";

    @Override
    public DashboardDetailResponseVO.ExecutionPlanDisplayVO build(
            DecisionResultVO decision,
            DashboardDetailResponseVO.PlanBoundaryDisplayVO planBoundaryDisplay,
            DashboardDetailResponseVO.ExecutionPlanDisplayVO fallbackDisplay
    ) {
        return buildInternal(decision, planBoundaryDisplay, fallbackDisplay, null, false);
    }

    @Override
    public DashboardDetailResponseVO.ExecutionPlanDisplayVO build(
            DecisionResultVO decision,
            DashboardDetailResponseVO.PlanBoundaryDisplayVO planBoundaryDisplay,
            DashboardDetailResponseVO.ExecutionPlanDisplayVO fallbackDisplay,
            SourceTraceDTO sourceTrace
    ) {
        return buildInternal(decision, planBoundaryDisplay, fallbackDisplay, sourceTrace, true);
    }

    private DashboardDetailResponseVO.ExecutionPlanDisplayVO buildInternal(
            DecisionResultVO decision,
            DashboardDetailResponseVO.PlanBoundaryDisplayVO planBoundaryDisplay,
            DashboardDetailResponseVO.ExecutionPlanDisplayVO fallbackDisplay,
            SourceTraceDTO sourceTrace,
            boolean sourceTraceRequired
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
            if (sourceTraceRequired && (sourceTrace == null || !sourceTrace.hasRequiredBoundarySources())) {
                markSourceTraceNotReady(display, sourceTrace);
                return display;
            }
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

    @Override
    public DashboardDetailResponseVO.ExecutionPlanDisplayVO build(
            DecisionResultVO decision,
            DashboardDetailResponseVO.PlanBoundaryDisplayVO planBoundaryDisplay,
            DashboardDetailResponseVO.ExecutionPlanDisplayVO fallbackDisplay,
            SourceTraceDTO sourceTrace,
            DashboardDetailResponseVO.RiskActionGuardDisplayVO riskActionGuardDisplay
    ) {
        DashboardDetailResponseVO.ExecutionPlanDisplayVO display = buildInternal(
                decision,
                planBoundaryDisplay,
                fallbackDisplay,
                sourceTrace,
                true
        );
        if (!READY_REVIEW_ONLY.equalsIgnoreCase(display.getExecutionPlanStatus())) {
            return display;
        }
        if (isRiskActionGuardReady(decision, riskActionGuardDisplay)) {
            return display;
        }
        markRiskActionGuardNotReady(display, decision, riskActionGuardDisplay);
        return display;
    }

    private void markSourceTraceNotReady(
            DashboardDetailResponseVO.ExecutionPlanDisplayVO display,
            SourceTraceDTO sourceTrace
    ) {
        String status = resolveSourceTraceExecutionStatus(sourceTrace);
        String label = WATCH_ONLY.equals(status) ? LABEL_WATCH_ONLY : LABEL_INCOMPLETE;
        String reason = resolveSourceTraceReason(sourceTrace);
        markNotAligned(display, VALID, status, label, reason);
        if (sourceTrace != null && sourceTrace.getMissingFields() != null) {
            for (String missingField : sourceTrace.getMissingFields()) {
                addUnique(display.getIncompleteReasons(), "SOURCE_TRACE_MISSING_FIELD:" + missingField);
            }
        }
    }

    private String resolveSourceTraceExecutionStatus(SourceTraceDTO sourceTrace) {
        if (sourceTrace == null) {
            return INCOMPLETE;
        }
        SourceTraceFallbackStatusEnum fallbackStatus = sourceTrace.getFallbackStatus();
        if (fallbackStatus == SourceTraceFallbackStatusEnum.WATCH_ONLY
                || fallbackStatus == SourceTraceFallbackStatusEnum.SAFE_FAIL_CLOSED_ONLY) {
            return WATCH_ONLY;
        }
        return INCOMPLETE;
    }

    private String resolveSourceTraceReason(SourceTraceDTO sourceTrace) {
        if (sourceTrace == null) {
            return REASON_SOURCE_TRACE_MISSING;
        }
        SourceTraceFallbackStatusEnum fallbackStatus = sourceTrace.getFallbackStatus();
        if (fallbackStatus == SourceTraceFallbackStatusEnum.WATCH_ONLY) {
            return REASON_SOURCE_TRACE_WATCH_ONLY;
        }
        if (fallbackStatus == SourceTraceFallbackStatusEnum.SAFE_FAIL_CLOSED_ONLY) {
            return REASON_SOURCE_TRACE_SAFE_FAIL_CLOSED;
        }
        return REASON_SOURCE_TRACE_INCOMPLETE;
    }

    private boolean isRiskActionGuardReady(
            DecisionResultVO decision,
            DashboardDetailResponseVO.RiskActionGuardDisplayVO riskActionGuardDisplay
    ) {
        return resolveRiskActionGuardReason(decision, riskActionGuardDisplay) == null;
    }

    private void markRiskActionGuardNotReady(
            DashboardDetailResponseVO.ExecutionPlanDisplayVO display,
            DecisionResultVO decision,
            DashboardDetailResponseVO.RiskActionGuardDisplayVO riskActionGuardDisplay
    ) {
        String reason = resolveRiskActionGuardReason(decision, riskActionGuardDisplay);
        String status = REASON_RISK_ACTION_GUARD_MISSING.equals(reason)
                || REASON_RISK_ACTION_GUARD_BACKEND_PENDING.equals(reason)
                ? INCOMPLETE
                : WATCH_ONLY;
        String label = WATCH_ONLY.equals(status) ? LABEL_WATCH_ONLY : LABEL_INCOMPLETE;
        markNotAligned(display, VALID, status, label, reason);
    }

    private String resolveRiskActionGuardReason(
            DecisionResultVO decision,
            DashboardDetailResponseVO.RiskActionGuardDisplayVO riskActionGuardDisplay
    ) {
        if (riskActionGuardDisplay == null) {
            return REASON_RISK_ACTION_GUARD_MISSING;
        }
        if (isBlank(riskActionGuardDisplay.getRiskActionGuardStatus())
                || BACKEND_PENDING.equalsIgnoreCase(riskActionGuardDisplay.getRiskActionGuardStatus())) {
            return REASON_RISK_ACTION_GUARD_BACKEND_PENDING;
        }
        if (isHighRisk(decision)) {
            return REASON_HIGH_RISK_REVIEW_ONLY;
        }
        if (isBlank(riskActionGuardDisplay.getLiquidityState())
                || BACKEND_PENDING.equalsIgnoreCase(riskActionGuardDisplay.getLiquidityState())) {
            return REASON_LIQUIDITY_CONTEXT_MISSING;
        }
        if (Boolean.TRUE.equals(riskActionGuardDisplay.getStampedeDetected())) {
            return REASON_STAMPEDE_REVIEW_ONLY;
        }
        if (Boolean.TRUE.equals(riskActionGuardDisplay.getWickOnlyRisk())) {
            return REASON_WICK_ONLY_REVIEW_ONLY;
        }
        if (Boolean.TRUE.equals(riskActionGuardDisplay.getOpportunityPushAllowed())
                || Boolean.TRUE.equals(riskActionGuardDisplay.getReverseTradeAllowed())
                || Boolean.TRUE.equals(riskActionGuardDisplay.getNewPositionAllowed())
                || Boolean.TRUE.equals(riskActionGuardDisplay.getMarketOrderExitAllowed())) {
            return REASON_RISK_ACTION_GUARD_BLOCKED;
        }
        String blockingReason = riskActionGuardDisplay.getRiskActionBlockingReason();
        if (!isBlank(blockingReason) && !REASON_MANUAL_REVIEW_REQUIRED.equalsIgnoreCase(blockingReason)) {
            return blockingReason;
        }
        return null;
    }

    private boolean isHighRisk(DecisionResultVO decision) {
        return decision != null
                && ("HIGH".equalsIgnoreCase(decision.getRiskLevel())
                || "EXTREME".equalsIgnoreCase(decision.getRiskLevel()));
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
