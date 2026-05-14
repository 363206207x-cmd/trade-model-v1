package org.example.trademodel.service.dashboard;

import org.example.trademodel.vo.DashboardDetailResponseVO;
import org.example.trademodel.vo.DecisionResultVO;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DefaultPaperObservationDisplayAdapterTest {
    private final DefaultPaperObservationDisplayAdapter adapter = new DefaultPaperObservationDisplayAdapter();

    @Test
    void shouldReturnBackendPendingAndFailClosedWhenInputsAreMissing() {
        DashboardDetailResponseVO.PaperObservationDisplayVO display = adapter.build(null, null, null, null, null);

        assertEquals("BACKEND_PENDING", display.getPaperObservationStatus());
        assertEquals("DECISION_MISSING", display.getReviewSummary());
        assertFailClosed(display);
    }

    @Test
    void shouldPreserveFallbackObjectAndForceSafetyFlags() {
        DashboardDetailResponseVO.PaperObservationDisplayVO fallback = new DashboardDetailResponseVO.PaperObservationDisplayVO();
        fallback.setPaperObservationAvailable(true);
        fallback.setManualReviewEntryAvailable(true);
        fallback.setNotRealPosition(false);
        fallback.setNotTradeInstruction(false);
        fallback.setManualReviewRequired(false);

        DashboardDetailResponseVO.PaperObservationDisplayVO display = adapter.build(null, null, null, null, fallback);

        assertSame(fallback, display);
        assertEquals("DECISION_MISSING", display.getReviewSummary());
        assertFailClosed(display);
    }

    @Test
    void shouldBlockWhenPlanBoundaryIsNotValid() {
        DecisionResultVO decision = new DecisionResultVO();
        DashboardDetailResponseVO.PlanBoundaryDisplayVO boundary = new DashboardDetailResponseVO.PlanBoundaryDisplayVO();
        boundary.setPlanBoundaryStatus("INCOMPLETE");

        DashboardDetailResponseVO.PaperObservationDisplayVO display = adapter.build(
                decision,
                boundary,
                readyReviewExecutionPlan(),
                manualReviewRiskGuard(),
                null
        );

        assertEquals("PLAN_BOUNDARY_NOT_VALID", display.getReviewSummary());
        assertFailClosed(display);
    }

    @Test
    void shouldBlockWhenExecutionPlanIsNotReadyReviewOnly() {
        DecisionResultVO decision = new DecisionResultVO();
        DashboardDetailResponseVO.ExecutionPlanDisplayVO execution = new DashboardDetailResponseVO.ExecutionPlanDisplayVO();
        execution.setExecutionPlanStatus("INCOMPLETE");

        DashboardDetailResponseVO.PaperObservationDisplayVO display = adapter.build(
                decision,
                validBoundary(),
                execution,
                manualReviewRiskGuard(),
                null
        );

        assertEquals("EXECUTION_PLAN_NOT_READY", display.getReviewSummary());
        assertFailClosed(display);
    }

    @Test
    void shouldBlockWhenRiskGuardIsBlocked() {
        DecisionResultVO decision = new DecisionResultVO();
        DashboardDetailResponseVO.RiskActionGuardDisplayVO risk = new DashboardDetailResponseVO.RiskActionGuardDisplayVO();
        risk.setRiskActionGuardStatus("MANUAL_REVIEW_REQUIRED");
        risk.setRiskActionBlockingReason("LIQUIDITY_CONTEXT_MISSING");

        DashboardDetailResponseVO.PaperObservationDisplayVO display = adapter.build(
                decision,
                validBoundary(),
                readyReviewExecutionPlan(),
                risk,
                null
        );

        assertEquals("RISK_ACTION_GUARD_BLOCKED", display.getReviewSummary());
        assertFailClosed(display);
    }

    @Test
    void shouldRequireManualReviewWhenAllUpstreamDisplaysAreReviewReady() {
        DecisionResultVO decision = new DecisionResultVO();

        DashboardDetailResponseVO.PaperObservationDisplayVO display = adapter.build(
                decision,
                validBoundary(),
                readyReviewExecutionPlan(),
                manualReviewRiskGuard(),
                null
        );

        assertEquals("MANUAL_REVIEW_REQUIRED", display.getPaperObservationStatus());
        assertEquals("AVAILABLE_REVIEW_ONLY", display.getReviewSummary());
        assertFailClosed(display);
    }

    @Test
    void shouldPreserveMissedOpportunityFlagButKeepEntryUnavailable() {
        DecisionResultVO decision = new DecisionResultVO();
        DashboardDetailResponseVO.PaperObservationDisplayVO fallback = new DashboardDetailResponseVO.PaperObservationDisplayVO();
        fallback.setMissedOpportunityFlag(true);

        DashboardDetailResponseVO.PaperObservationDisplayVO display = adapter.build(
                decision,
                validBoundary(),
                readyReviewExecutionPlan(),
                manualReviewRiskGuard(),
                fallback
        );

        assertTrue(display.getMissedOpportunityFlag());
        assertFailClosed(display);
    }

    private DashboardDetailResponseVO.PlanBoundaryDisplayVO validBoundary() {
        DashboardDetailResponseVO.PlanBoundaryDisplayVO boundary = new DashboardDetailResponseVO.PlanBoundaryDisplayVO();
        boundary.setPlanBoundaryStatus("VALID");
        return boundary;
    }

    private DashboardDetailResponseVO.ExecutionPlanDisplayVO readyReviewExecutionPlan() {
        DashboardDetailResponseVO.ExecutionPlanDisplayVO execution = new DashboardDetailResponseVO.ExecutionPlanDisplayVO();
        execution.setExecutionPlanStatus("READY_REVIEW_ONLY");
        return execution;
    }

    private DashboardDetailResponseVO.RiskActionGuardDisplayVO manualReviewRiskGuard() {
        DashboardDetailResponseVO.RiskActionGuardDisplayVO risk = new DashboardDetailResponseVO.RiskActionGuardDisplayVO();
        risk.setRiskActionGuardStatus("MANUAL_REVIEW_REQUIRED");
        risk.setRiskActionBlockingReason("MANUAL_REVIEW_REQUIRED");
        return risk;
    }

    private void assertFailClosed(DashboardDetailResponseVO.PaperObservationDisplayVO display) {
        assertFalse(display.getPaperObservationAvailable());
        assertFalse(display.getManualReviewEntryAvailable());
        assertTrue(display.getNotRealPosition());
        assertTrue(display.getNotTradeInstruction());
        assertTrue(display.getManualReviewRequired());
    }
}
