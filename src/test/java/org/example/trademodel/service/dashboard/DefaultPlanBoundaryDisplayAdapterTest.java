package org.example.trademodel.service.dashboard;

import org.example.trademodel.vo.DashboardDetailResponseVO;
import org.example.trademodel.vo.DecisionResultVO;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DefaultPlanBoundaryDisplayAdapterTest {
    private final DefaultPlanBoundaryDisplayAdapter adapter = new DefaultPlanBoundaryDisplayAdapter();

    @Test
    void shouldReturnBackendPendingWhenInputsAreMissing() {
        DashboardDetailResponseVO.PlanBoundaryDisplayVO display = adapter.build(null, null, null);

        assertEquals("BACKEND_PENDING", display.getPlanBoundaryStatus());
        assertEquals("BACKEND_PENDING", display.getSourceTraceStatus());
        assertEquals("BACKEND_PENDING", display.getBackendConnectionStatus());
        assertTrue(display.getManualReviewRequired());
        assertTrue(display.getNotTradeInstruction());
        assertTrue(display.getBlockingReasons().contains("DECISION_MISSING"));
    }

    @Test
    void shouldPreserveFallbackObjectAndSafetyFlags() {
        DashboardDetailResponseVO.PlanBoundaryDisplayVO fallback = new DashboardDetailResponseVO.PlanBoundaryDisplayVO();
        fallback.setManualReviewRequired(false);
        fallback.setNotTradeInstruction(false);

        DashboardDetailResponseVO.PlanBoundaryDisplayVO display = adapter.build("BTCUSDT", null, fallback);

        assertSame(fallback, display);
        assertTrue(display.getManualReviewRequired());
        assertTrue(display.getNotTradeInstruction());
        assertEquals("BACKEND_PENDING", display.getPlanBoundaryStatus());
    }

    @Test
    void shouldMarkIncompleteWhenAnalysisIdIsMissing() {
        DecisionResultVO decision = new DecisionResultVO();
        decision.setAnalysisId(" ");

        DashboardDetailResponseVO.PlanBoundaryDisplayVO display = adapter.build("BTCUSDT", decision, null);

        assertEquals("INCOMPLETE", display.getPlanBoundaryStatus());
        assertEquals("MISSING", display.getSourceTraceStatus());
        assertEquals("PARTIAL", display.getBackendConnectionStatus());
        assertTrue(display.getIncompleteReasons().contains("ANALYSIS_ID_MISSING"));
        assertTrue(display.getManualReviewRequired());
        assertTrue(display.getNotTradeInstruction());
    }

    @Test
    void shouldMarkIncompleteWhenReadModelIsPartial() {
        DecisionResultVO decision = new DecisionResultVO();
        decision.setAnalysisId("ana-1");
        decision.setReadModelTruthStatus("PARTIAL");
        decision.setReadModelFallbackReason("LEGACY_MISSING:invalid_condition");

        DashboardDetailResponseVO.PlanBoundaryDisplayVO display = adapter.build("BTCUSDT", decision, null);

        assertEquals("INCOMPLETE", display.getPlanBoundaryStatus());
        assertTrue(display.getIncompleteReasons().contains("READ_MODEL_PARTIAL"));
        assertTrue(display.getBlockingReasons().contains("LEGACY_MISSING:invalid_condition"));
        assertTrue(display.getManualReviewRequired());
        assertTrue(display.getNotTradeInstruction());
    }

    @Test
    void shouldNotTreatTextExecutionPlanFieldsAsValidBoundary() {
        DecisionResultVO decision = new DecisionResultVO();
        decision.setAnalysisId("ana-1");
        decision.setEntryZone("暂无");
        decision.setStopLoss("暂无");
        decision.setTakeProfitRules("暂无");
        decision.setExecutionPlanSummary("观察摘要");

        DashboardDetailResponseVO.PlanBoundaryDisplayVO display = adapter.build("BTCUSDT", decision, null);

        assertEquals("INCOMPLETE", display.getPlanBoundaryStatus());
        assertFalse("VALID".equals(display.getPlanBoundaryStatus()));
        assertTrue(display.getIncompleteReasons().contains("SOURCE_TRACE_PENDING"));
        assertTrue(display.getManualReviewRequired());
        assertTrue(display.getNotTradeInstruction());
    }

    @Test
    void shouldReturnBackendPendingWhenDecisionHasNoBoundarySignal() {
        DecisionResultVO decision = new DecisionResultVO();
        decision.setAnalysisId("ana-1");

        DashboardDetailResponseVO.PlanBoundaryDisplayVO display = adapter.build("BTCUSDT", decision, null);

        assertEquals("BACKEND_PENDING", display.getPlanBoundaryStatus());
        assertNotNull(display.getIncompleteReasons());
        assertNotNull(display.getBlockingReasons());
        assertTrue(display.getManualReviewRequired());
        assertTrue(display.getNotTradeInstruction());
    }
}
