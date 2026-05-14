package org.example.trademodel.service.dashboard;

import org.example.trademodel.vo.DashboardDetailResponseVO;
import org.example.trademodel.vo.DecisionResultVO;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DefaultExecutionPlanDisplayAdapterTest {
    private final DefaultExecutionPlanDisplayAdapter adapter = new DefaultExecutionPlanDisplayAdapter();

    @Test
    void shouldReturnBoundaryPendingWhenInputsAreMissing() {
        DashboardDetailResponseVO.ExecutionPlanDisplayVO display = adapter.build(null, null, null);

        assertEquals("BOUNDARY_PENDING", display.getExecutionPlanStatus());
        assertEquals("BACKEND_PENDING", display.getPlanBoundaryStatus());
        assertFalse(display.getExecutionPlanBoundaryAligned());
        assertEquals("PLAN_BOUNDARY_BACKEND_PENDING", display.getNotExecutableReason());
        assertTrue(display.getIncompleteReasons().contains("PLAN_BOUNDARY_BACKEND_PENDING"));
        assertTrue(display.getManualReviewRequired());
        assertTrue(display.getNotTradeInstruction());
    }

    @Test
    void shouldPreserveFallbackObjectAndSafetyFlags() {
        DashboardDetailResponseVO.ExecutionPlanDisplayVO fallback = new DashboardDetailResponseVO.ExecutionPlanDisplayVO();
        fallback.setManualReviewRequired(false);
        fallback.setNotTradeInstruction(false);
        DashboardDetailResponseVO.PlanBoundaryDisplayVO boundary = new DashboardDetailResponseVO.PlanBoundaryDisplayVO();

        DashboardDetailResponseVO.ExecutionPlanDisplayVO display = adapter.build(null, boundary, fallback);

        assertSame(fallback, display);
        assertTrue(display.getManualReviewRequired());
        assertTrue(display.getNotTradeInstruction());
        assertEquals("BOUNDARY_PENDING", display.getExecutionPlanStatus());
    }

    @Test
    void shouldMapBackendPendingBoundaryToBoundaryPendingExecutionPlan() {
        DashboardDetailResponseVO.PlanBoundaryDisplayVO boundary = new DashboardDetailResponseVO.PlanBoundaryDisplayVO();
        boundary.setPlanBoundaryStatus("BACKEND_PENDING");

        DashboardDetailResponseVO.ExecutionPlanDisplayVO display = adapter.build(null, boundary, null);

        assertEquals("BOUNDARY_PENDING", display.getExecutionPlanStatus());
        assertEquals("BACKEND_PENDING", display.getPlanBoundaryStatus());
        assertFalse(display.getExecutionPlanBoundaryAligned());
        assertEquals("PLAN_BOUNDARY_BACKEND_PENDING", display.getNotExecutableReason());
    }

    @Test
    void shouldMapIncompleteBoundaryToIncompleteExecutionPlanAndInheritReasons() {
        DashboardDetailResponseVO.PlanBoundaryDisplayVO boundary = new DashboardDetailResponseVO.PlanBoundaryDisplayVO();
        boundary.setPlanBoundaryStatus("INCOMPLETE");
        boundary.setIncompleteReasons(List.of("READ_MODEL_PARTIAL"));

        DashboardDetailResponseVO.ExecutionPlanDisplayVO display = adapter.build(null, boundary, null);

        assertEquals("INCOMPLETE", display.getExecutionPlanStatus());
        assertEquals("INCOMPLETE", display.getPlanBoundaryStatus());
        assertFalse(display.getExecutionPlanBoundaryAligned());
        assertEquals("PLAN_BOUNDARY_INCOMPLETE", display.getNotExecutableReason());
        assertTrue(display.getIncompleteReasons().contains("PLAN_BOUNDARY_INCOMPLETE"));
        assertTrue(display.getIncompleteReasons().contains("READ_MODEL_PARTIAL"));
    }

    @Test
    void shouldMapWatchOnlyBoundaryToWatchOnlyExecutionPlan() {
        DashboardDetailResponseVO.PlanBoundaryDisplayVO boundary = new DashboardDetailResponseVO.PlanBoundaryDisplayVO();
        boundary.setPlanBoundaryStatus("WATCH_ONLY");

        DashboardDetailResponseVO.ExecutionPlanDisplayVO display = adapter.build(null, boundary, null);

        assertEquals("WATCH_ONLY", display.getExecutionPlanStatus());
        assertEquals("WATCH_ONLY", display.getPlanBoundaryStatus());
        assertFalse(display.getExecutionPlanBoundaryAligned());
        assertEquals("PLAN_BOUNDARY_WATCH_ONLY", display.getNotExecutableReason());
    }

    @Test
    void shouldMapInvalidBoundaryToInvalidExecutionPlan() {
        DashboardDetailResponseVO.PlanBoundaryDisplayVO boundary = new DashboardDetailResponseVO.PlanBoundaryDisplayVO();
        boundary.setPlanBoundaryStatus("INVALID");

        DashboardDetailResponseVO.ExecutionPlanDisplayVO display = adapter.build(null, boundary, null);

        assertEquals("INVALID", display.getExecutionPlanStatus());
        assertEquals("INVALID", display.getPlanBoundaryStatus());
        assertFalse(display.getExecutionPlanBoundaryAligned());
        assertEquals("PLAN_BOUNDARY_INVALID", display.getNotExecutableReason());
    }

    @Test
    void shouldMapValidBoundaryOnlyToReadyReviewOnly() {
        DashboardDetailResponseVO.PlanBoundaryDisplayVO boundary = new DashboardDetailResponseVO.PlanBoundaryDisplayVO();
        boundary.setPlanBoundaryStatus("VALID");

        DashboardDetailResponseVO.ExecutionPlanDisplayVO display = adapter.build(null, boundary, null);

        assertEquals("READY_REVIEW_ONLY", display.getExecutionPlanStatus());
        assertEquals("VALID", display.getPlanBoundaryStatus());
        assertTrue(display.getExecutionPlanBoundaryAligned());
        assertEquals("MANUAL_REVIEW_REQUIRED", display.getNotExecutableReason());
        assertTrue(display.getManualReviewRequired());
        assertTrue(display.getNotTradeInstruction());
    }

    @Test
    void textExecutionSummaryShouldNotMakeBoundaryAlignedWhenBoundaryIsPending() {
        DecisionResultVO decision = new DecisionResultVO();
        decision.setExecutionPlanSummary("观察摘要");
        DashboardDetailResponseVO.PlanBoundaryDisplayVO boundary = new DashboardDetailResponseVO.PlanBoundaryDisplayVO();
        boundary.setPlanBoundaryStatus("BACKEND_PENDING");

        DashboardDetailResponseVO.ExecutionPlanDisplayVO display = adapter.build(decision, boundary, null);

        assertEquals("观察摘要", display.getExecutionPlanSummary());
        assertFalse(display.getExecutionPlanBoundaryAligned());
        assertEquals("BOUNDARY_PENDING", display.getExecutionPlanStatus());
    }

    @Test
    void shouldKeepSafetyFlagsForEveryMapping() {
        DashboardDetailResponseVO.PlanBoundaryDisplayVO boundary = new DashboardDetailResponseVO.PlanBoundaryDisplayVO();
        boundary.setPlanBoundaryStatus("INCOMPLETE");

        DashboardDetailResponseVO.ExecutionPlanDisplayVO display = adapter.build(null, boundary, null);

        assertNotNull(display.getIncompleteReasons());
        assertTrue(display.getManualReviewRequired());
        assertTrue(display.getNotTradeInstruction());
    }
}
