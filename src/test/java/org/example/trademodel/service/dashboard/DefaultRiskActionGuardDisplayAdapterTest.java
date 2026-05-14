package org.example.trademodel.service.dashboard;

import org.example.trademodel.vo.DashboardDetailResponseVO;
import org.example.trademodel.vo.DecisionResultVO;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DefaultRiskActionGuardDisplayAdapterTest {
    private final DefaultRiskActionGuardDisplayAdapter adapter = new DefaultRiskActionGuardDisplayAdapter();

    @Test
    void shouldReturnBackendPendingAndFailClosedWhenInputsAreMissing() {
        DashboardDetailResponseVO.RiskActionGuardDisplayVO display = adapter.build(null, null, null, null);

        assertEquals("BACKEND_PENDING", display.getRiskActionGuardStatus());
        assertEquals("BACKEND_PENDING", display.getLiquidityState());
        assertEquals("DECISION_MISSING", display.getRiskActionBlockingReason());
        assertFailClosed(display);
    }

    @Test
    void shouldPreserveFallbackObjectAndForceSafetyFlags() {
        DashboardDetailResponseVO.RiskActionGuardDisplayVO fallback = new DashboardDetailResponseVO.RiskActionGuardDisplayVO();
        fallback.setOpportunityPushAllowed(true);
        fallback.setReverseTradeAllowed(true);
        fallback.setNewPositionAllowed(true);
        fallback.setMarketOrderExitAllowed(true);
        fallback.setManualRiskReviewRequired(false);
        fallback.setNotTradeInstruction(false);

        DashboardDetailResponseVO.RiskActionGuardDisplayVO display = adapter.build(null, null, null, fallback);

        assertSame(fallback, display);
        assertEquals("DECISION_MISSING", display.getRiskActionBlockingReason());
        assertFailClosed(display);
    }

    @Test
    void shouldBlockWhenPlanBoundaryIsNotValid() {
        DecisionResultVO decision = new DecisionResultVO();
        decision.setRiskLevel("LOW");
        DashboardDetailResponseVO.PlanBoundaryDisplayVO boundary = new DashboardDetailResponseVO.PlanBoundaryDisplayVO();
        boundary.setPlanBoundaryStatus("INCOMPLETE");
        DashboardDetailResponseVO.ExecutionPlanDisplayVO execution = readyReviewExecutionPlan();

        DashboardDetailResponseVO.RiskActionGuardDisplayVO display = adapter.build(decision, boundary, execution, null);

        assertEquals("PLAN_BOUNDARY_NOT_VALID", display.getRiskActionBlockingReason());
        assertFailClosed(display);
    }

    @Test
    void shouldBlockWhenExecutionPlanIsNotReadyReviewOnly() {
        DecisionResultVO decision = new DecisionResultVO();
        decision.setRiskLevel("LOW");
        DashboardDetailResponseVO.PlanBoundaryDisplayVO boundary = validBoundary();
        DashboardDetailResponseVO.ExecutionPlanDisplayVO execution = new DashboardDetailResponseVO.ExecutionPlanDisplayVO();
        execution.setExecutionPlanStatus("BOUNDARY_PENDING");

        DashboardDetailResponseVO.RiskActionGuardDisplayVO display = adapter.build(decision, boundary, execution, null);

        assertEquals("EXECUTION_PLAN_NOT_READY", display.getRiskActionBlockingReason());
        assertFailClosed(display);
    }

    @Test
    void shouldRequireManualReviewWhenBoundaryAndExecutionPlanAreReady() {
        DecisionResultVO decision = new DecisionResultVO();
        decision.setRiskLevel("LOW");

        DashboardDetailResponseVO.RiskActionGuardDisplayVO display = adapter.build(
                decision,
                validBoundary(),
                readyReviewExecutionPlan(),
                null
        );

        assertEquals("MANUAL_REVIEW_REQUIRED", display.getRiskActionGuardStatus());
        assertEquals("MANUAL_REVIEW_REQUIRED", display.getRiskActionBlockingReason());
        assertFailClosed(display);
    }

    @Test
    void shouldNotTurnHighRiskIntoActionWithoutLiquidityContext() {
        DecisionResultVO decision = new DecisionResultVO();
        decision.setRiskLevel("HIGH");
        DashboardDetailResponseVO.RiskActionGuardDisplayVO fallback = new DashboardDetailResponseVO.RiskActionGuardDisplayVO();
        fallback.setLiquidityState(null);

        DashboardDetailResponseVO.RiskActionGuardDisplayVO display = adapter.build(
                decision,
                validBoundary(),
                readyReviewExecutionPlan(),
                fallback
        );

        assertEquals("MANUAL_REVIEW_REQUIRED", display.getRiskActionGuardStatus());
        assertEquals("LIQUIDITY_CONTEXT_MISSING", display.getRiskActionBlockingReason());
        assertFailClosed(display);
    }

    @Test
    void shouldPreserveDetectedStampedeAndWickFlagsButKeepActionsBlocked() {
        DecisionResultVO decision = new DecisionResultVO();
        decision.setRiskLevel("HIGH");
        DashboardDetailResponseVO.RiskActionGuardDisplayVO fallback = new DashboardDetailResponseVO.RiskActionGuardDisplayVO();
        fallback.setStampedeDetected(true);
        fallback.setWickOnlyRisk(true);

        DashboardDetailResponseVO.RiskActionGuardDisplayVO display = adapter.build(
                decision,
                validBoundary(),
                readyReviewExecutionPlan(),
                fallback
        );

        assertTrue(display.getStampedeDetected());
        assertTrue(display.getWickOnlyRisk());
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

    private void assertFailClosed(DashboardDetailResponseVO.RiskActionGuardDisplayVO display) {
        assertFalse(display.getOpportunityPushAllowed());
        assertFalse(display.getReverseTradeAllowed());
        assertFalse(display.getNewPositionAllowed());
        assertFalse(display.getMarketOrderExitAllowed());
        assertTrue(display.getManualRiskReviewRequired());
        assertTrue(display.getNotTradeInstruction());
    }
}
