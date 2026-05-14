package org.example.trademodel.service.dashboard;

import org.example.trademodel.vo.DashboardDetailResponseVO;
import org.example.trademodel.vo.DecisionResultVO;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DefaultPlanBoundarySourceTraceAdapterTest {
    private final DefaultPlanBoundarySourceTraceAdapter adapter = new DefaultPlanBoundarySourceTraceAdapter();

    @Test
    void shouldReturnBackendPendingWhenInputsAreMissing() {
        DashboardDetailResponseVO.PlanBoundaryDisplayVO display = adapter.build(null, null, null);

        assertEquals("BACKEND_PENDING", display.getPlanBoundaryStatus());
        assertEquals("BACKEND_PENDING", display.getSourceTraceStatus());
        assertEquals("BACKEND_PENDING", display.getBackendConnectionStatus());
        assertTrue(display.getBlockingReasons().contains("DECISION_MISSING"));
        assertTrue(display.getManualReviewRequired());
        assertTrue(display.getNotTradeInstruction());
    }

    @Test
    void shouldPreserveFallbackObjectAndForceSafetyFlags() {
        DashboardDetailResponseVO.PlanBoundaryDisplayVO fallback = new DashboardDetailResponseVO.PlanBoundaryDisplayVO();
        fallback.setManualReviewRequired(false);
        fallback.setNotTradeInstruction(false);

        DashboardDetailResponseVO.PlanBoundaryDisplayVO display = adapter.build("BTCUSDT", null, fallback);

        assertSame(fallback, display);
        assertTrue(display.getBlockingReasons().contains("DECISION_MISSING"));
        assertTrue(display.getManualReviewRequired());
        assertTrue(display.getNotTradeInstruction());
    }

    @Test
    void shouldReturnIncompleteWhenDecisionExistsButSourceTraceInputIsUnavailable() {
        DecisionResultVO decision = new DecisionResultVO();
        decision.setSymbol("BTCUSDT");

        DashboardDetailResponseVO.PlanBoundaryDisplayVO display = adapter.build("BTCUSDT", decision, null);

        assertEquals("INCOMPLETE", display.getPlanBoundaryStatus());
        assertEquals("MISSING", display.getSourceTraceStatus());
        assertEquals("PARTIAL", display.getBackendConnectionStatus());
        assertTrue(display.getIncompleteReasons().contains("SOURCE_TRACE_INPUT_NOT_AVAILABLE"));
        assertTrue(display.getBlockingReasons().contains("BOUNDARY_CANDIDATE_DTO_MISSING"));
        assertTrue(display.getBlockingReasons().contains("RUNTIME_KLINE_CONTEXT_DTO_MISSING"));
        assertTrue(display.getManualReviewRequired());
        assertTrue(display.getNotTradeInstruction());
    }

    @Test
    void shouldKeepReadModelPartialReasonsWhenDecisionIsPartial() {
        DecisionResultVO decision = new DecisionResultVO();
        decision.setReadModelTruthStatus("PARTIAL");
        decision.setReadModelFallbackReason("LEGACY_MISSING:invalid_condition");

        DashboardDetailResponseVO.PlanBoundaryDisplayVO display = adapter.build("BTCUSDT", decision, null);

        assertEquals("INCOMPLETE", display.getPlanBoundaryStatus());
        assertTrue(display.getIncompleteReasons().contains("READ_MODEL_PARTIAL"));
        assertTrue(display.getBlockingReasons().contains("LEGACY_MISSING:invalid_condition"));
    }

    @Test
    void textFieldsMustNotProduceValidBoundary() {
        DecisionResultVO decision = new DecisionResultVO();
        decision.setEntryZone("100-101");
        decision.setStopLoss("99");
        decision.setTakeProfitRules("TP1 105");
        decision.setExecutionPlanSummary("text only");

        DashboardDetailResponseVO.PlanBoundaryDisplayVO display = adapter.build("BTCUSDT", decision, null);

        assertFalse("VALID".equals(display.getPlanBoundaryStatus()));
        assertEquals("INCOMPLETE", display.getPlanBoundaryStatus());
        assertTrue(display.getIncompleteReasons().contains("SOURCE_TRACE_INPUT_NOT_AVAILABLE"));
    }

    @Test
    void shouldInitializeMissingReasonLists() {
        DecisionResultVO decision = new DecisionResultVO();
        DashboardDetailResponseVO.PlanBoundaryDisplayVO fallback = new DashboardDetailResponseVO.PlanBoundaryDisplayVO();
        fallback.setIncompleteReasons(null);
        fallback.setBlockingReasons(null);

        DashboardDetailResponseVO.PlanBoundaryDisplayVO display = adapter.build("BTCUSDT", decision, fallback);

        assertTrue(display.getIncompleteReasons().contains("SOURCE_TRACE_INPUT_NOT_AVAILABLE"));
        assertTrue(display.getBlockingReasons().contains("BOUNDARY_CANDIDATE_DTO_MISSING"));
        assertTrue(display.getBlockingReasons().contains("RUNTIME_KLINE_CONTEXT_DTO_MISSING"));
    }
}
