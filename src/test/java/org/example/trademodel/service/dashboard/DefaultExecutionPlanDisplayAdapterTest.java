package org.example.trademodel.service.dashboard;

import org.example.trademodel.dto.planboundary.SourceTraceDTO;
import org.example.trademodel.dto.planboundary.SourceTraceFallbackStatusEnum;
import org.example.trademodel.vo.DashboardDetailResponseVO;
import org.example.trademodel.vo.DecisionResultVO;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
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
        assertReviewOnlyGuardrails(display);
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
        assertReviewOnlyGuardrails(display);
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
        assertEquals("只允许复核摘要", display.getExecutionPlanStatusLabel());
        assertEquals("VALID", display.getPlanBoundaryStatus());
        assertTrue(display.getExecutionPlanBoundaryAligned());
        assertEquals("MANUAL_REVIEW_REQUIRED", display.getNotExecutableReason());
        assertTrue(display.getManualReviewRequired());
        assertTrue(display.getNotTradeInstruction());
        assertReviewOnlyGuardrails(display);
    }

    @Test
    void shouldKeepExecutionPlanIncompleteWhenSourceTraceIsMissingForValidBoundary() {
        DashboardDetailResponseVO.PlanBoundaryDisplayVO boundary = new DashboardDetailResponseVO.PlanBoundaryDisplayVO();
        boundary.setPlanBoundaryStatus("VALID");

        DashboardDetailResponseVO.ExecutionPlanDisplayVO display = adapter.build(null, boundary, null, null);

        assertEquals("INCOMPLETE", display.getExecutionPlanStatus());
        assertEquals("VALID", display.getPlanBoundaryStatus());
        assertFalse(display.getExecutionPlanBoundaryAligned());
        assertEquals("SOURCE_TRACE_MISSING", display.getNotExecutableReason());
        assertTrue(display.getIncompleteReasons().contains("SOURCE_TRACE_MISSING"));
        assertTrue(display.getManualReviewRequired());
        assertTrue(display.getNotTradeInstruction());
        assertReviewOnlyGuardrails(display);
    }

    @Test
    void shouldKeepExecutionPlanWatchOnlyWhenSourceTraceRequestsWatchOnly() {
        DashboardDetailResponseVO.PlanBoundaryDisplayVO boundary = new DashboardDetailResponseVO.PlanBoundaryDisplayVO();
        boundary.setPlanBoundaryStatus("VALID");
        SourceTraceDTO sourceTrace = validSourceTrace();
        sourceTrace.setEventSource(null);
        sourceTrace.setFallbackStatus(SourceTraceFallbackStatusEnum.WATCH_ONLY);
        sourceTrace.setMissingFields(List.of("eventSource"));

        DashboardDetailResponseVO.ExecutionPlanDisplayVO display = adapter.build(null, boundary, null, sourceTrace);

        assertEquals("WATCH_ONLY", display.getExecutionPlanStatus());
        assertEquals("VALID", display.getPlanBoundaryStatus());
        assertFalse(display.getExecutionPlanBoundaryAligned());
        assertEquals("SOURCE_TRACE_WATCH_ONLY", display.getNotExecutableReason());
        assertTrue(display.getIncompleteReasons().contains("SOURCE_TRACE_MISSING_FIELD:eventSource"));
        assertTrue(display.getManualReviewRequired());
        assertTrue(display.getNotTradeInstruction());
        assertReviewOnlyGuardrails(display);
    }

    @Test
    void shouldKeepExecutionPlanWatchOnlyWhenSourceTraceSafeFailsClosed() {
        DashboardDetailResponseVO.PlanBoundaryDisplayVO boundary = new DashboardDetailResponseVO.PlanBoundaryDisplayVO();
        boundary.setPlanBoundaryStatus("VALID");
        SourceTraceDTO sourceTrace = validSourceTrace();
        sourceTrace.setLiquiditySource(null);
        sourceTrace.setFallbackStatus(SourceTraceFallbackStatusEnum.SAFE_FAIL_CLOSED_ONLY);
        sourceTrace.setMissingFields(List.of("liquiditySource"));

        DashboardDetailResponseVO.ExecutionPlanDisplayVO display = adapter.build(null, boundary, null, sourceTrace);

        assertEquals("WATCH_ONLY", display.getExecutionPlanStatus());
        assertEquals("VALID", display.getPlanBoundaryStatus());
        assertFalse(display.getExecutionPlanBoundaryAligned());
        assertEquals("SOURCE_TRACE_SAFE_FAIL_CLOSED_ONLY", display.getNotExecutableReason());
        assertTrue(display.getIncompleteReasons().contains("SOURCE_TRACE_MISSING_FIELD:liquiditySource"));
        assertTrue(display.getManualReviewRequired());
        assertTrue(display.getNotTradeInstruction());
        assertReviewOnlyGuardrails(display);
    }

    @Test
    void shouldMapValidBoundaryAndCompleteSourceTraceOnlyToReadyReviewOnly() {
        DashboardDetailResponseVO.PlanBoundaryDisplayVO boundary = new DashboardDetailResponseVO.PlanBoundaryDisplayVO();
        boundary.setPlanBoundaryStatus("VALID");

        DashboardDetailResponseVO.ExecutionPlanDisplayVO display = adapter.build(null, boundary, null, validSourceTrace());

        assertEquals("READY_REVIEW_ONLY", display.getExecutionPlanStatus());
        assertEquals("只允许复核摘要", display.getExecutionPlanStatusLabel());
        assertEquals("VALID", display.getPlanBoundaryStatus());
        assertTrue(display.getExecutionPlanBoundaryAligned());
        assertEquals("MANUAL_REVIEW_REQUIRED", display.getNotExecutableReason());
        assertTrue(display.getManualReviewRequired());
        assertTrue(display.getNotTradeInstruction());
        assertReviewOnlyGuardrails(display);
    }

    @Test
    void runtimeKlineOnlySourceTraceShouldNotUpgradeExecutionPlanReadiness() {
        DashboardDetailResponseVO.PlanBoundaryDisplayVO boundary = new DashboardDetailResponseVO.PlanBoundaryDisplayVO();
        boundary.setPlanBoundaryStatus("VALID");
        SourceTraceDTO sourceTrace = new SourceTraceDTO();
        sourceTrace.setSymbol("BTCUSDT");
        sourceTrace.setTimeframe("1m");
        sourceTrace.setFallbackStatus(SourceTraceFallbackStatusEnum.INCOMPLETE);
        sourceTrace.setMissingFields(List.of(
                "runtimeKlineContext",
                "entryPriceSource",
                "stopPriceSource",
                "tpPriceSources",
                "rrSource",
                "liquiditySource",
                "eventSource",
                "wickSource"
        ));

        DashboardDetailResponseVO.ExecutionPlanDisplayVO display = adapter.build(null, boundary, null, sourceTrace);

        assertEquals("INCOMPLETE", display.getExecutionPlanStatus());
        assertEquals("VALID", display.getPlanBoundaryStatus());
        assertFalse(display.getExecutionPlanBoundaryAligned());
        assertEquals("SOURCE_TRACE_INCOMPLETE", display.getNotExecutableReason());
        assertTrue(display.getIncompleteReasons().contains("SOURCE_TRACE_MISSING_FIELD:entryPriceSource"));
        assertTrue(display.getIncompleteReasons().contains("SOURCE_TRACE_MISSING_FIELD:stopPriceSource"));
        assertTrue(display.getIncompleteReasons().contains("SOURCE_TRACE_MISSING_FIELD:tpPriceSources"));
        assertTrue(display.getIncompleteReasons().contains("SOURCE_TRACE_MISSING_FIELD:rrSource"));
        assertTrue(display.getManualReviewRequired());
        assertTrue(display.getNotTradeInstruction());
        assertReviewOnlyGuardrails(display);
    }

    @Test
    void shouldFallbackToWatchOnlyWhenRiskActionGuardDetectsHighRisk() {
        DecisionResultVO decision = new DecisionResultVO();
        decision.setRiskLevel("HIGH");
        DashboardDetailResponseVO.PlanBoundaryDisplayVO boundary = new DashboardDetailResponseVO.PlanBoundaryDisplayVO();
        boundary.setPlanBoundaryStatus("VALID");

        DashboardDetailResponseVO.ExecutionPlanDisplayVO display = adapter.build(
                decision,
                boundary,
                null,
                validSourceTrace(),
                readyRiskActionGuard()
        );

        assertEquals("WATCH_ONLY", display.getExecutionPlanStatus());
        assertFalse(display.getExecutionPlanBoundaryAligned());
        assertEquals("HIGH_RISK_REVIEW_ONLY", display.getNotExecutableReason());
        assertTrue(display.getManualReviewRequired());
        assertTrue(display.getNotTradeInstruction());
        assertReviewOnlyGuardrails(display);
    }

    @Test
    void shouldFallbackToWatchOnlyWhenRiskActionGuardDetectsStampede() {
        DecisionResultVO decision = new DecisionResultVO();
        decision.setRiskLevel("LOW");
        DashboardDetailResponseVO.PlanBoundaryDisplayVO boundary = new DashboardDetailResponseVO.PlanBoundaryDisplayVO();
        boundary.setPlanBoundaryStatus("VALID");
        DashboardDetailResponseVO.RiskActionGuardDisplayVO risk = readyRiskActionGuard();
        risk.setStampedeDetected(true);

        DashboardDetailResponseVO.ExecutionPlanDisplayVO display = adapter.build(
                decision,
                boundary,
                null,
                validSourceTrace(),
                risk
        );

        assertEquals("WATCH_ONLY", display.getExecutionPlanStatus());
        assertFalse(display.getExecutionPlanBoundaryAligned());
        assertEquals("STAMPEDE_RISK_REVIEW_ONLY", display.getNotExecutableReason());
    }

    @Test
    void shouldFallbackToWatchOnlyWhenRiskActionGuardLiquidityContextIsMissing() {
        DecisionResultVO decision = new DecisionResultVO();
        decision.setRiskLevel("LOW");
        DashboardDetailResponseVO.PlanBoundaryDisplayVO boundary = new DashboardDetailResponseVO.PlanBoundaryDisplayVO();
        boundary.setPlanBoundaryStatus("VALID");
        DashboardDetailResponseVO.RiskActionGuardDisplayVO risk = readyRiskActionGuard();
        risk.setLiquidityState("BACKEND_PENDING");

        DashboardDetailResponseVO.ExecutionPlanDisplayVO display = adapter.build(
                decision,
                boundary,
                null,
                validSourceTrace(),
                risk
        );

        assertEquals("WATCH_ONLY", display.getExecutionPlanStatus());
        assertFalse(display.getExecutionPlanBoundaryAligned());
        assertEquals("LIQUIDITY_CONTEXT_MISSING", display.getNotExecutableReason());
        assertTrue(display.getManualReviewRequired());
        assertTrue(display.getNotTradeInstruction());
    }

    @Test
    void shouldFallbackToWatchOnlyWhenRiskActionGuardDetectsWickOnlyRisk() {
        DecisionResultVO decision = new DecisionResultVO();
        decision.setRiskLevel("LOW");
        DashboardDetailResponseVO.PlanBoundaryDisplayVO boundary = new DashboardDetailResponseVO.PlanBoundaryDisplayVO();
        boundary.setPlanBoundaryStatus("VALID");
        DashboardDetailResponseVO.RiskActionGuardDisplayVO risk = readyRiskActionGuard();
        risk.setWickOnlyRisk(true);

        DashboardDetailResponseVO.ExecutionPlanDisplayVO display = adapter.build(
                decision,
                boundary,
                null,
                validSourceTrace(),
                risk
        );

        assertEquals("WATCH_ONLY", display.getExecutionPlanStatus());
        assertFalse(display.getExecutionPlanBoundaryAligned());
        assertEquals("WICK_ONLY_RISK_REVIEW_ONLY", display.getNotExecutableReason());
        assertTrue(display.getManualReviewRequired());
        assertTrue(display.getNotTradeInstruction());
    }

    @Test
    void shouldStayReadyReviewOnlyWhenSourceTraceAndRiskActionGuardAreReady() {
        DecisionResultVO decision = new DecisionResultVO();
        decision.setRiskLevel("LOW");
        DashboardDetailResponseVO.PlanBoundaryDisplayVO boundary = new DashboardDetailResponseVO.PlanBoundaryDisplayVO();
        boundary.setPlanBoundaryStatus("VALID");

        DashboardDetailResponseVO.ExecutionPlanDisplayVO display = adapter.build(
                decision,
                boundary,
                null,
                validSourceTrace(),
                readyRiskActionGuard()
        );

        assertEquals("READY_REVIEW_ONLY", display.getExecutionPlanStatus());
        assertEquals("只允许复核摘要", display.getExecutionPlanStatusLabel());
        assertTrue(display.getExecutionPlanBoundaryAligned());
        assertEquals("MANUAL_REVIEW_REQUIRED", display.getNotExecutableReason());
        assertTrue(display.getManualReviewRequired());
        assertTrue(display.getNotTradeInstruction());
        assertReviewOnlyGuardrails(display);
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
    void readyReviewOnlyShouldRemainReviewOnlyAndNotExecutable() {
        DecisionResultVO decision = new DecisionResultVO();
        decision.setRiskLevel("LOW");
        decision.setExecutionPlanSummary("只读计划摘要");
        DashboardDetailResponseVO.PlanBoundaryDisplayVO boundary = new DashboardDetailResponseVO.PlanBoundaryDisplayVO();
        boundary.setPlanBoundaryStatus("VALID");

        DashboardDetailResponseVO.ExecutionPlanDisplayVO display = adapter.build(
                decision,
                boundary,
                null,
                validSourceTrace(),
                readyRiskActionGuard()
        );

        assertEquals("READY_REVIEW_ONLY", display.getExecutionPlanStatus());
        assertEquals("只允许复核摘要", display.getExecutionPlanStatusLabel());
        assertEquals("只读计划摘要", display.getExecutionPlanSummary());
        assertEquals("MANUAL_REVIEW_REQUIRED", display.getNotExecutableReason());
        assertReviewOnlyGuardrails(display);
    }

    @Test
    void adapterShouldNotExposeTradingActionAutomationOrValidFactorySurface() {
        for (java.lang.reflect.Method method : DefaultExecutionPlanDisplayAdapter.class.getDeclaredMethods()) {
            String methodName = method.getName().toLowerCase();
            assertFalse(methodName.contains("order"));
            assertFalse(methodName.contains("scheduler"));
            assertFalse(methodName.contains("automation"));
            assertFalse(methodName.contains("autotrading"));
            assertFalse(methodName.contains("boundarycandidate"));
            assertFalse("valid".equals(methodName));
        }
        for (java.lang.reflect.Field field : DefaultExecutionPlanDisplayAdapter.class.getDeclaredFields()) {
            String fieldName = field.getName().toLowerCase();
            assertFalse(fieldName.contains("order"));
            assertFalse(fieldName.contains("scheduler"));
            assertFalse(fieldName.contains("automation"));
            assertFalse(fieldName.contains("autotrading"));
            assertFalse(fieldName.contains("boundarycandidate"));
        }
    }

    @Test
    void shouldKeepSafetyFlagsForEveryMapping() {
        DashboardDetailResponseVO.PlanBoundaryDisplayVO boundary = new DashboardDetailResponseVO.PlanBoundaryDisplayVO();
        boundary.setPlanBoundaryStatus("INCOMPLETE");

        DashboardDetailResponseVO.ExecutionPlanDisplayVO display = adapter.build(null, boundary, null);

        assertNotNull(display.getIncompleteReasons());
        assertTrue(display.getManualReviewRequired());
        assertTrue(display.getNotTradeInstruction());
        assertReviewOnlyGuardrails(display);
    }

    private void assertReviewOnlyGuardrails(DashboardDetailResponseVO.ExecutionPlanDisplayVO display) {
        assertNotNull(display.getIncompleteReasons());
        assertTrue(display.getManualReviewRequired());
        assertTrue(display.getNotTradeInstruction());
        assertTrue(display.getIncompleteReasons().contains("EXECUTION_PLAN_REVIEW_ONLY_DISPLAY"));
        assertTrue(display.getIncompleteReasons().contains("EXECUTION_PLAN_NOT_EXECUTABLE"));
        assertTrue(display.getIncompleteReasons().contains("NOT_TRADE_INSTRUCTION"));
        assertTrue(display.getIncompleteReasons().contains("ENTRY_STOP_TP_RR_NOT_GENERATED"));
    }

    private SourceTraceDTO validSourceTrace() {
        SourceTraceDTO sourceTrace = new SourceTraceDTO();
        sourceTrace.setSymbol("BTCUSDT");
        sourceTrace.setTimeframe("1h");
        sourceTrace.setEntryPriceSource(BigDecimal.valueOf(68000));
        sourceTrace.setEntrySourceType("support");
        sourceTrace.setEntrySourceTimeframe("1h");
        sourceTrace.setEntrySourceReason("support retest");
        sourceTrace.setEntrySourceRef("entry-1");
        sourceTrace.setStopPriceSource(BigDecimal.valueOf(66800));
        sourceTrace.setStopSourceType("swing_low");
        sourceTrace.setStopSourceTimeframe("1h");
        sourceTrace.setStopSourceReason("recent swing low");
        sourceTrace.setStopSourceRef("stop-1");
        sourceTrace.setTpPriceSources(List.of(BigDecimal.valueOf(70400)));
        sourceTrace.setTpSourceType("rr_ladder");
        sourceTrace.setTpSourceTimeframe("1h");
        sourceTrace.setTpSourceReason("2R target");
        sourceTrace.setTpSourceRef("tp-1");
        sourceTrace.setRrSource(BigDecimal.valueOf(2));
        sourceTrace.setRrRuleRef("min_rr_2");
        sourceTrace.setLiquiditySource("liquidity-ok");
        sourceTrace.setMultiTimeframeSource("multi-timeframe-aligned");
        sourceTrace.setEventSource("no-event-window");
        sourceTrace.setWickSource("wick-confirmed");
        return sourceTrace;
    }

    private DashboardDetailResponseVO.RiskActionGuardDisplayVO readyRiskActionGuard() {
        DashboardDetailResponseVO.RiskActionGuardDisplayVO risk = new DashboardDetailResponseVO.RiskActionGuardDisplayVO();
        risk.setRiskActionGuardStatus("MANUAL_REVIEW_REQUIRED");
        risk.setRiskActionBlockingReason("MANUAL_REVIEW_REQUIRED");
        risk.setLiquidityState("NORMAL");
        risk.setStampedeDetected(false);
        risk.setWickOnlyRisk(false);
        risk.setOpportunityPushAllowed(false);
        risk.setReverseTradeAllowed(false);
        risk.setNewPositionAllowed(false);
        risk.setMarketOrderExitAllowed(false);
        risk.setManualRiskReviewRequired(true);
        risk.setNotTradeInstruction(true);
        return risk;
    }
}
