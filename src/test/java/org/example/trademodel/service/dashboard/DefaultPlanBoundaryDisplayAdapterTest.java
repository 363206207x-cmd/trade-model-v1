package org.example.trademodel.service.dashboard;

import org.example.trademodel.vo.DashboardDetailResponseVO;
import org.example.trademodel.vo.DecisionResultVO;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DefaultPlanBoundaryDisplayAdapterTest {
    private final PlanBoundarySourceTraceAdapter passthroughSourceTraceAdapter = (symbol, decision, fallbackDisplay) -> fallbackDisplay;
    private final DefaultPlanBoundaryDisplayAdapter adapter = new DefaultPlanBoundaryDisplayAdapter(passthroughSourceTraceAdapter);

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
        assertTrue(display.getIncompleteReasons().contains("BOUNDARY_CANDIDATE_STATUS:INCOMPLETE"));
        assertTrue(display.getBlockingReasons().contains("REVIEW_MODE:REVIEW_ONLY"));
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

    @Test
    void shouldInvokeSourceTraceAdapterWhenDecisionExists() {
        PlanBoundarySourceTraceAdapter sourceTraceAdapter = (symbol, decision, fallbackDisplay) -> {
            fallbackDisplay.setPlanBoundaryStatus("INCOMPLETE");
            fallbackDisplay.setSourceTraceStatus("MISSING");
            fallbackDisplay.getIncompleteReasons().add("SOURCE_TRACE_INPUT_NOT_AVAILABLE");
            return fallbackDisplay;
        };
        DefaultPlanBoundaryDisplayAdapter localAdapter = new DefaultPlanBoundaryDisplayAdapter(sourceTraceAdapter);
        DecisionResultVO decision = new DecisionResultVO();
        decision.setAnalysisId("ana-1");

        DashboardDetailResponseVO.PlanBoundaryDisplayVO display = localAdapter.build("BTCUSDT", decision, null);

        assertEquals("INCOMPLETE", display.getPlanBoundaryStatus());
        assertEquals("MISSING", display.getSourceTraceStatus());
        assertTrue(display.getIncompleteReasons().contains("SOURCE_TRACE_INPUT_NOT_AVAILABLE"));
        assertTrue(display.getManualReviewRequired());
        assertTrue(display.getNotTradeInstruction());
    }

    @Test
    void shouldExposeReviewOnlyCandidateStatusWithoutTradingInstruction() {
        PlanBoundarySourceTraceAdapter sourceTraceAdapter = (symbol, decision, fallbackDisplay) -> {
            fallbackDisplay.setPlanBoundaryStatus("REVIEW_ONLY_CANDIDATE");
            fallbackDisplay.setSourceTraceStatus("READY");
            fallbackDisplay.getIncompleteReasons().add("MISSING_FIELD:sourceOwner");
            return fallbackDisplay;
        };
        DefaultPlanBoundaryDisplayAdapter localAdapter = new DefaultPlanBoundaryDisplayAdapter(sourceTraceAdapter);
        DecisionResultVO decision = new DecisionResultVO();
        decision.setAnalysisId("ana-1");

        DashboardDetailResponseVO.PlanBoundaryDisplayVO display = localAdapter.build("BTCUSDT", decision, null);

        assertEquals("REVIEW_ONLY", display.getPlanBoundaryStatus());
        assertEquals("只允许复核", display.getPlanBoundaryStatusLabel());
        assertEquals("PARTIAL", display.getBackendConnectionStatus());
        assertTrue(display.getIncompleteReasons().contains("MISSING_FIELD:sourceOwner"));
        assertTrue(display.getBlockingReasons().contains("BOUNDARY_CANDIDATE_STATUS:REVIEW_ONLY"));
        assertTrue(display.getBlockingReasons().contains("REVIEW_MODE:REVIEW_ONLY"));
        assertTrue(display.getBlockingReasons().contains("NOT_TRADE_INSTRUCTION"));
        assertTrue(display.getManualReviewRequired());
        assertTrue(display.getNotTradeInstruction());
    }

    @Test
    void shouldKeepBlockedCandidateFailClosed() {
        PlanBoundarySourceTraceAdapter sourceTraceAdapter = (symbol, decision, fallbackDisplay) -> {
            fallbackDisplay.setPlanBoundaryStatus("BLOCKED");
            fallbackDisplay.getBlockingReasons().add("source_conflict");
            return fallbackDisplay;
        };
        DefaultPlanBoundaryDisplayAdapter localAdapter = new DefaultPlanBoundaryDisplayAdapter(sourceTraceAdapter);
        DecisionResultVO decision = new DecisionResultVO();
        decision.setAnalysisId("ana-1");

        DashboardDetailResponseVO.PlanBoundaryDisplayVO display = localAdapter.build("BTCUSDT", decision, null);

        assertEquals("BLOCKED", display.getPlanBoundaryStatus());
        assertEquals("禁止推进", display.getPlanBoundaryStatusLabel());
        assertTrue(display.getBlockingReasons().contains("source_conflict"));
        assertTrue(display.getBlockingReasons().contains("BOUNDARY_CANDIDATE_STATUS:BLOCKED"));
        assertTrue(display.getBlockingReasons().contains("REVIEW_MODE:REVIEW_ONLY"));
        assertTrue(display.getManualReviewRequired());
        assertTrue(display.getNotTradeInstruction());
    }

    @Test
    void shouldKeepWatchOnlyCandidateFailClosed() {
        PlanBoundarySourceTraceAdapter sourceTraceAdapter = (symbol, decision, fallbackDisplay) -> {
            fallbackDisplay.setPlanBoundaryStatus("WATCH_ONLY");
            fallbackDisplay.getBlockingReasons().add("risk_action_guard_blocked");
            return fallbackDisplay;
        };
        DefaultPlanBoundaryDisplayAdapter localAdapter = new DefaultPlanBoundaryDisplayAdapter(sourceTraceAdapter);
        DecisionResultVO decision = new DecisionResultVO();
        decision.setAnalysisId("ana-1");

        DashboardDetailResponseVO.PlanBoundaryDisplayVO display = localAdapter.build("BTCUSDT", decision, null);

        assertEquals("WATCH_ONLY", display.getPlanBoundaryStatus());
        assertEquals("仅观察", display.getPlanBoundaryStatusLabel());
        assertTrue(display.getBlockingReasons().contains("risk_action_guard_blocked"));
        assertTrue(display.getBlockingReasons().contains("BOUNDARY_CANDIDATE_STATUS:WATCH_ONLY"));
        assertTrue(display.getManualReviewRequired());
        assertTrue(display.getNotTradeInstruction());
    }

    @Test
    void shouldPreventSourceTraceAdapterFromReturningValidInCurrentPhase() {
        PlanBoundarySourceTraceAdapter sourceTraceAdapter = (symbol, decision, fallbackDisplay) -> {
            fallbackDisplay.setPlanBoundaryStatus("VALID");
            fallbackDisplay.setSourceTraceStatus("READY");
            return fallbackDisplay;
        };
        DefaultPlanBoundaryDisplayAdapter localAdapter = new DefaultPlanBoundaryDisplayAdapter(sourceTraceAdapter);
        DecisionResultVO decision = new DecisionResultVO();
        decision.setAnalysisId("ana-1");

        DashboardDetailResponseVO.PlanBoundaryDisplayVO display = localAdapter.build("BTCUSDT", decision, null);

        assertEquals("INCOMPLETE", display.getPlanBoundaryStatus());
        assertFalse("VALID".equals(display.getPlanBoundaryStatus()));
        assertTrue(display.getIncompleteReasons().contains("SOURCE_TRACE_PENDING"));
        assertTrue(display.getIncompleteReasons().contains("BOUNDARY_CANDIDATE_STATUS:INCOMPLETE"));
        assertTrue(display.getBlockingReasons().contains("BOUNDARY_CANDIDATE_STATUS_UNSAFE:VALID"));
        assertTrue(display.getManualReviewRequired());
        assertTrue(display.getNotTradeInstruction());
    }

    @Test
    void shouldNotExposeProductionCandidatePointReadinessOrTradingSurfaces() throws Exception {
        String source = Files.readString(Path.of(
                "src/main/java/org/example/trademodel/service/dashboard/DefaultPlanBoundaryDisplayAdapter.java"
        ));
        String normalizedSource = source.toLowerCase(Locale.ROOT);

        assertFalse(source.contains("BoundaryCandidateDTO.valid("));
        assertFalse(source.contains("generatedEntry"));
        assertFalse(source.contains("generatedStop"));
        assertFalse(source.contains("generatedTakeProfit"));
        assertFalse(source.contains("generatedRiskReward"));
        assertFalse(normalizedSource.contains("readiness"));
        assertFalse(declaredMemberNames().stream().anyMatch(this::containsTradingSurfaceTerm));
        assertFalse(declaredFieldTypes().contains(BigDecimal.class));
    }

    private List<String> declaredMemberNames() {
        List<String> methodNames = Arrays.stream(DefaultPlanBoundaryDisplayAdapter.class.getDeclaredMethods())
                .map(Method::getName)
                .toList();
        List<String> fieldNames = Arrays.stream(DefaultPlanBoundaryDisplayAdapter.class.getDeclaredFields())
                .map(Field::getName)
                .toList();
        return java.util.stream.Stream.concat(methodNames.stream(), fieldNames.stream()).toList();
    }

    private List<Class<?>> declaredFieldTypes() {
        return Arrays.stream(DefaultPlanBoundaryDisplayAdapter.class.getDeclaredFields())
                .map(Field::getType)
                .toList();
    }

    private boolean containsTradingSurfaceTerm(String value) {
        String normalized = value.toLowerCase(Locale.ROOT);
        return normalized.contains("order")
                || normalized.contains("execution")
                || normalized.contains("automation")
                || normalized.contains("autotrading")
                || normalized.contains("autotrade");
    }
}
