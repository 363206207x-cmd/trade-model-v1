package org.example.trademodel.vo;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DashboardDetailResponseVOTest {

    @Test
    void shouldExposeDisplayModelFieldsThroughDashboardDetailResponse() {
        DashboardDetailResponseVO response = new DashboardDetailResponseVO();
        DashboardDetailResponseVO.PlanBoundaryDisplayVO planBoundary = new DashboardDetailResponseVO.PlanBoundaryDisplayVO();
        DashboardDetailResponseVO.ExecutionPlanDisplayVO executionPlan = new DashboardDetailResponseVO.ExecutionPlanDisplayVO();
        DashboardDetailResponseVO.RiskActionGuardDisplayVO riskActionGuard = new DashboardDetailResponseVO.RiskActionGuardDisplayVO();
        DashboardDetailResponseVO.PaperObservationDisplayVO paperObservation = new DashboardDetailResponseVO.PaperObservationDisplayVO();

        response.setPlanBoundaryDisplay(planBoundary);
        response.setExecutionPlanDisplay(executionPlan);
        response.setRiskActionGuardDisplay(riskActionGuard);
        response.setPaperObservationDisplay(paperObservation);

        assertSame(planBoundary, response.getPlanBoundaryDisplay());
        assertSame(executionPlan, response.getExecutionPlanDisplay());
        assertSame(riskActionGuard, response.getRiskActionGuardDisplay());
        assertSame(paperObservation, response.getPaperObservationDisplay());
    }

    @Test
    void planBoundaryDisplayShouldUseSafeBackendPendingDefaults() {
        DashboardDetailResponseVO.PlanBoundaryDisplayVO display = new DashboardDetailResponseVO.PlanBoundaryDisplayVO();

        assertEquals("BACKEND_PENDING", display.getPlanBoundaryStatus());
        assertEquals("后端未接入", display.getPlanBoundaryStatusLabel());
        assertEquals("BACKEND_PENDING", display.getSourceTraceStatus());
        assertEquals("BACKEND_PENDING", display.getBackendConnectionStatus());
        assertTrue(display.getManualReviewRequired());
        assertTrue(display.getNotTradeInstruction());
        assertNotNull(display.getIncompleteReasons());
        assertNotNull(display.getBlockingReasons());
        assertNull(display.getUpdatedAt());
    }

    @Test
    void executionPlanDisplayShouldUseBoundaryPendingDefaults() {
        DashboardDetailResponseVO.ExecutionPlanDisplayVO display = new DashboardDetailResponseVO.ExecutionPlanDisplayVO();

        assertEquals("BOUNDARY_PENDING", display.getExecutionPlanStatus());
        assertEquals("等待边界接入", display.getExecutionPlanStatusLabel());
        assertFalse(display.getExecutionPlanBoundaryAligned());
        assertEquals("BACKEND_PENDING", display.getPlanBoundaryStatus());
        assertEquals("PLAN_BOUNDARY_BACKEND_PENDING", display.getNotExecutableReason());
        assertTrue(display.getManualReviewRequired());
        assertTrue(display.getNotTradeInstruction());
        assertNotNull(display.getIncompleteReasons());
        assertNull(display.getExecutionPlanSummary());
        assertNull(display.getUpdatedAt());
    }

    @Test
    void riskActionGuardDisplayShouldFailClosedByDefault() {
        DashboardDetailResponseVO.RiskActionGuardDisplayVO display = new DashboardDetailResponseVO.RiskActionGuardDisplayVO();

        assertEquals("BACKEND_PENDING", display.getRiskActionGuardStatus());
        assertEquals("后端未接入", display.getRiskActionGuardStatusLabel());
        assertEquals("BACKEND_PENDING", display.getLiquidityState());
        assertFalse(display.getStampedeDetected());
        assertFalse(display.getWickOnlyRisk());
        assertFalse(display.getOpportunityPushAllowed());
        assertFalse(display.getReverseTradeAllowed());
        assertFalse(display.getNewPositionAllowed());
        assertFalse(display.getMarketOrderExitAllowed());
        assertTrue(display.getManualRiskReviewRequired());
        assertTrue(display.getNotTradeInstruction());
        assertNull(display.getRiskActionAdvice());
        assertNull(display.getRiskActionBlockingReason());
        assertNull(display.getUpdatedAt());
    }

    @Test
    void paperObservationDisplayShouldNotRepresentRealPositionByDefault() {
        DashboardDetailResponseVO.PaperObservationDisplayVO display = new DashboardDetailResponseVO.PaperObservationDisplayVO();

        assertEquals("BACKEND_PENDING", display.getPaperObservationStatus());
        assertEquals("后端未接入", display.getPaperObservationStatusLabel());
        assertFalse(display.getPaperObservationAvailable());
        assertFalse(display.getManualReviewEntryAvailable());
        assertEquals(0, display.getLinkedPaperObservationCount());
        assertEquals(0, display.getLinkedReviewCount());
        assertFalse(display.getMissedOpportunityFlag());
        assertTrue(display.getNotRealPosition());
        assertTrue(display.getNotTradeInstruction());
        assertTrue(display.getManualReviewRequired());
        assertEquals("BACKEND_PENDING", display.getBackendConnectionStatus());
        assertNull(display.getReviewSummary());
        assertNull(display.getUpdatedAt());
    }

    @Test
    void displayModelsShouldSupportSetterUpdatesWithoutBusinessSideEffects() {
        DashboardDetailResponseVO.PlanBoundaryDisplayVO planBoundary = new DashboardDetailResponseVO.PlanBoundaryDisplayVO();
        planBoundary.setPlanBoundaryStatus("INCOMPLETE");
        planBoundary.setIncompleteReasons(List.of("ENTRY_SOURCE_MISSING"));

        DashboardDetailResponseVO.ExecutionPlanDisplayVO executionPlan = new DashboardDetailResponseVO.ExecutionPlanDisplayVO();
        executionPlan.setExecutionPlanSummary("观察摘要");
        executionPlan.setIncompleteReasons(List.of("PLAN_BOUNDARY_NOT_VALID"));

        assertEquals("INCOMPLETE", planBoundary.getPlanBoundaryStatus());
        assertEquals(List.of("ENTRY_SOURCE_MISSING"), planBoundary.getIncompleteReasons());
        assertEquals("观察摘要", executionPlan.getExecutionPlanSummary());
        assertEquals(List.of("PLAN_BOUNDARY_NOT_VALID"), executionPlan.getIncompleteReasons());
    }
}
