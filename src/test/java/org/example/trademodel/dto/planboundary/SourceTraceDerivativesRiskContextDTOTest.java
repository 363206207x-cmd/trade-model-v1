package org.example.trademodel.dto.planboundary;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;

class SourceTraceDerivativesRiskContextDTOTest {

    @Test
    void sourceTraceShouldCarryTraceableBoundarySourcesAndFallbackState() {
        SourceTraceDTO trace = new SourceTraceDTO();
        trace.setSymbol("BTCUSDT");
        trace.setTimeframe("1h");
        trace.setEntryPriceSource(BigDecimal.valueOf(68000));
        trace.setEntrySourceType("support");
        trace.setEntrySourceTimeframe("1h");
        trace.setEntrySourceReason("support retest");
        trace.setEntrySourceRef("entry-ref");
        trace.setStopPriceSource(BigDecimal.valueOf(66800));
        trace.setStopSourceType("swing_low");
        trace.setStopSourceTimeframe("1h");
        trace.setStopSourceReason("recent swing low");
        trace.setStopSourceRef("stop-ref");
        trace.setTpPriceSources(List.of(BigDecimal.valueOf(70400), BigDecimal.valueOf(71600)));
        trace.setTpSourceType("rr_ladder");
        trace.setTpSourceTimeframe("1h");
        trace.setTpSourceReason("2R and 3R ladder");
        trace.setTpSourceRef("tp-ref");
        trace.setRrSource(BigDecimal.valueOf(2));
        trace.setRrRuleRef("min_rr_2");
        trace.setLiquiditySource("depth_snapshot");
        trace.setMultiTimeframeSource("1h_4h_alignment");
        trace.setEventSource("no_major_event_window");
        trace.setWickSource("wick_confirmed");
        trace.setFallbackStatus(SourceTraceFallbackStatusEnum.WATCH_ONLY);
        trace.setMissingFields(List.of("liquiditySource"));

        assertEquals("BTCUSDT", trace.getSymbol());
        assertEquals("1h", trace.getTimeframe());
        assertEquals(BigDecimal.valueOf(68000), trace.getEntryPriceSource());
        assertEquals("support", trace.getEntrySourceType());
        assertEquals("1h", trace.getEntrySourceTimeframe());
        assertEquals("support retest", trace.getEntrySourceReason());
        assertEquals("entry-ref", trace.getEntrySourceRef());
        assertEquals(BigDecimal.valueOf(66800), trace.getStopPriceSource());
        assertEquals("swing_low", trace.getStopSourceType());
        assertEquals("1h", trace.getStopSourceTimeframe());
        assertEquals("recent swing low", trace.getStopSourceReason());
        assertEquals("stop-ref", trace.getStopSourceRef());
        assertEquals(List.of(BigDecimal.valueOf(70400), BigDecimal.valueOf(71600)), trace.getTpPriceSources());
        assertEquals("rr_ladder", trace.getTpSourceType());
        assertEquals("1h", trace.getTpSourceTimeframe());
        assertEquals("2R and 3R ladder", trace.getTpSourceReason());
        assertEquals("tp-ref", trace.getTpSourceRef());
        assertEquals(BigDecimal.valueOf(2), trace.getRrSource());
        assertEquals("min_rr_2", trace.getRrRuleRef());
        assertEquals("depth_snapshot", trace.getLiquiditySource());
        assertEquals("1h_4h_alignment", trace.getMultiTimeframeSource());
        assertEquals("no_major_event_window", trace.getEventSource());
        assertEquals("wick_confirmed", trace.getWickSource());
        assertEquals(SourceTraceFallbackStatusEnum.WATCH_ONLY, trace.getFallbackStatus());
        assertEquals(List.of("liquiditySource"), trace.getMissingFields());
        assertFalse(trace.isComplete());
        assertTrue(trace.isManualReviewRequired());
        assertTrue(trace.isNotTradeInstruction());
        assertFalse(trace.hasRequiredBoundarySources());
    }

    @Test
    void sourceTraceShouldReportRequiredBoundarySourcesCompleteWhenNoFallbackOrMissingFields() {
        SourceTraceDTO trace = completeSourceTrace();

        assertTrue(trace.isComplete());
        assertTrue(trace.hasRequiredBoundarySources());
    }

    @Test
    void hasRequiredBoundarySourcesShouldStayFalseUnlessEveryOwnershipFieldExists() {
        List<SourceTraceFieldMutation> mutations = List.of(
                new SourceTraceFieldMutation("missingFields", trace -> trace.setMissingFields(List.of("entryPriceSource"))),
                new SourceTraceFieldMutation("fallbackStatus", trace -> trace.setFallbackStatus(SourceTraceFallbackStatusEnum.INCOMPLETE)),
                new SourceTraceFieldMutation("entryPriceSource", trace -> trace.setEntryPriceSource(null)),
                new SourceTraceFieldMutation("entrySourceType", trace -> trace.setEntrySourceType(null)),
                new SourceTraceFieldMutation("entrySourceTimeframe", trace -> trace.setEntrySourceTimeframe(null)),
                new SourceTraceFieldMutation("entrySourceReason", trace -> trace.setEntrySourceReason(null)),
                new SourceTraceFieldMutation("entrySourceRef", trace -> trace.setEntrySourceRef(null)),
                new SourceTraceFieldMutation("stopPriceSource", trace -> trace.setStopPriceSource(null)),
                new SourceTraceFieldMutation("stopSourceType", trace -> trace.setStopSourceType(null)),
                new SourceTraceFieldMutation("stopSourceTimeframe", trace -> trace.setStopSourceTimeframe(null)),
                new SourceTraceFieldMutation("stopSourceReason", trace -> trace.setStopSourceReason(null)),
                new SourceTraceFieldMutation("stopSourceRef", trace -> trace.setStopSourceRef(null)),
                new SourceTraceFieldMutation("tpPriceSources", trace -> trace.setTpPriceSources(List.of())),
                new SourceTraceFieldMutation("tpSourceType", trace -> trace.setTpSourceType(null)),
                new SourceTraceFieldMutation("tpSourceTimeframe", trace -> trace.setTpSourceTimeframe(null)),
                new SourceTraceFieldMutation("tpSourceReason", trace -> trace.setTpSourceReason(null)),
                new SourceTraceFieldMutation("tpSourceRef", trace -> trace.setTpSourceRef(null)),
                new SourceTraceFieldMutation("rrSource", trace -> trace.setRrSource(null)),
                new SourceTraceFieldMutation("rrRuleRef", trace -> trace.setRrRuleRef(null)),
                new SourceTraceFieldMutation("liquiditySource", trace -> trace.setLiquiditySource(null)),
                new SourceTraceFieldMutation("multiTimeframeSource", trace -> trace.setMultiTimeframeSource(null)),
                new SourceTraceFieldMutation("eventSource", trace -> trace.setEventSource(null)),
                new SourceTraceFieldMutation("wickSource", trace -> trace.setWickSource(null))
        );

        for (SourceTraceFieldMutation mutation : mutations) {
            SourceTraceDTO trace = completeSourceTrace();
            mutation.apply(trace);

            assertFalse(trace.hasRequiredBoundarySources(), mutation.fieldName());
            assertTrue(trace.isManualReviewRequired(), mutation.fieldName());
            assertTrue(trace.isNotTradeInstruction(), mutation.fieldName());
        }
    }

    @Test
    void entrySourceOwnershipResultShouldCarryFailClosedDefaults() {
        SourceTraceEntrySourceOwnershipResult result =
                SourceTraceEntrySourceOwnershipResult.missingSource("BTCUSDT", "1m");

        assertEquals("BTCUSDT", result.getSymbol());
        assertEquals("1m", result.getTimeframe());
        assertEquals(SourceTraceEntrySourceOwnershipStatusEnum.INCOMPLETE, result.getOwnershipStatus());
        assertEquals(SourceTraceEntrySourceMissingReasonEnum.MISSING_SOURCE, result.getMissingReason());
        assertEquals(SourceTraceEntrySourceReviewModeEnum.REVIEW_ONLY, result.getReviewMode());
        assertNull(result.getEntryPriceSource());
        assertNull(result.getEntrySourceType());
        assertNull(result.getEntrySourceTimeframe());
        assertNull(result.getEntrySourceReason());
        assertNull(result.getEntrySourceRef());
        assertEquals(List.of(
                "entryPriceSource",
                "entrySourceType",
                "entrySourceTimeframe",
                "entrySourceReason",
                "entrySourceRef"
        ), result.getMissingFields());
        assertTrue(result.isManualReviewRequired());
        assertTrue(result.isNotTradeInstruction());
    }

    @Test
    void stopSourceOwnershipResultShouldCarryFailClosedDefaults() {
        SourceTraceStopSourceOwnershipResult result =
                SourceTraceStopSourceOwnershipResult.missingSource("BTCUSDT", "1m");

        assertEquals("BTCUSDT", result.getSymbol());
        assertEquals("1m", result.getTimeframe());
        assertEquals(SourceTraceStopSourceOwnershipStatusEnum.INCOMPLETE, result.getOwnershipStatus());
        assertEquals(SourceTraceStopSourceMissingReasonEnum.MISSING_SOURCE, result.getMissingReason());
        assertEquals(SourceTraceStopSourceReviewModeEnum.REVIEW_ONLY, result.getReviewMode());
        assertNull(result.getStopPriceSource());
        assertNull(result.getStopSourceType());
        assertNull(result.getStopSourceTimeframe());
        assertNull(result.getStopSourceReason());
        assertNull(result.getStopSourceRef());
        assertEquals(List.of(
                "stopPriceSource",
                "stopSourceType",
                "stopSourceTimeframe",
                "stopSourceReason",
                "stopSourceRef"
        ), result.getMissingFields());
        assertTrue(result.isManualReviewRequired());
        assertTrue(result.isNotTradeInstruction());
    }

    @Test
    void takeProfitSourceOwnershipResultShouldCarryFailClosedDefaults() {
        SourceTraceTakeProfitSourceOwnershipResult result =
                SourceTraceTakeProfitSourceOwnershipResult.missingSource("BTCUSDT", "1m");

        assertEquals("BTCUSDT", result.getSymbol());
        assertEquals("1m", result.getTimeframe());
        assertEquals(SourceTraceTakeProfitSourceOwnershipStatusEnum.INCOMPLETE, result.getOwnershipStatus());
        assertEquals(SourceTraceTakeProfitSourceMissingReasonEnum.MISSING_SOURCE, result.getMissingReason());
        assertEquals(SourceTraceTakeProfitSourceReviewModeEnum.REVIEW_ONLY, result.getReviewMode());
        assertTrue(result.getTpPriceSources().isEmpty());
        assertNull(result.getTpSourceType());
        assertNull(result.getTpSourceTimeframe());
        assertNull(result.getTpSourceReason());
        assertNull(result.getTpSourceRef());
        assertEquals(List.of(
                "tpPriceSources",
                "tpSourceType",
                "tpSourceTimeframe",
                "tpSourceReason",
                "tpSourceRef"
        ), result.getMissingFields());
        assertTrue(result.isManualReviewRequired());
        assertTrue(result.isNotTradeInstruction());
    }

    @Test
    void riskRewardSourceOwnershipResultShouldCarryFailClosedDefaults() {
        SourceTraceRiskRewardSourceOwnershipResult result =
                SourceTraceRiskRewardSourceOwnershipResult.missingSource("BTCUSDT", "1m");

        assertEquals("BTCUSDT", result.getSymbol());
        assertEquals("1m", result.getTimeframe());
        assertEquals(SourceTraceRiskRewardSourceOwnershipStatusEnum.INCOMPLETE, result.getOwnershipStatus());
        assertEquals(SourceTraceRiskRewardSourceMissingReasonEnum.MISSING_SOURCE, result.getMissingReason());
        assertEquals(SourceTraceRiskRewardSourceReviewModeEnum.REVIEW_ONLY, result.getReviewMode());
        assertNull(result.getRrSource());
        assertNull(result.getRrRuleRef());
        assertEquals(List.of("rrSource", "rrRuleRef"), result.getMissingFields());
        assertTrue(result.isManualReviewRequired());
        assertTrue(result.isNotTradeInstruction());
    }

    @Test
    void liquiditySourceOwnershipResultShouldCarryFailClosedDefaults() {
        SourceTraceLiquiditySourceOwnershipResult result =
                SourceTraceLiquiditySourceOwnershipResult.missingSource("BTCUSDT", "1m");

        assertEquals("BTCUSDT", result.getSymbol());
        assertEquals("1m", result.getTimeframe());
        assertEquals(SourceTraceLiquiditySourceOwnershipStatusEnum.INCOMPLETE, result.getOwnershipStatus());
        assertEquals(SourceTraceLiquiditySourceMissingReasonEnum.MISSING_SOURCE, result.getMissingReason());
        assertEquals(SourceTraceLiquiditySourceReviewModeEnum.REVIEW_ONLY, result.getReviewMode());
        assertNull(result.getLiquiditySource());
        assertEquals(List.of("liquiditySource"), result.getMissingFields());
        assertTrue(result.isManualReviewRequired());
        assertTrue(result.isNotTradeInstruction());
    }

    @Test
    void multiTimeframeSourceOwnershipResultShouldCarryFailClosedDefaults() {
        SourceTraceMultiTimeframeSourceOwnershipResult result =
                SourceTraceMultiTimeframeSourceOwnershipResult.missingSource("BTCUSDT", "1m");

        assertEquals("BTCUSDT", result.getSymbol());
        assertEquals("1m", result.getTimeframe());
        assertEquals(SourceTraceMultiTimeframeSourceOwnershipStatusEnum.INCOMPLETE, result.getOwnershipStatus());
        assertEquals(SourceTraceMultiTimeframeSourceMissingReasonEnum.MISSING_SOURCE, result.getMissingReason());
        assertEquals(SourceTraceMultiTimeframeSourceReviewModeEnum.REVIEW_ONLY, result.getReviewMode());
        assertNull(result.getMultiTimeframeSource());
        assertEquals(List.of("multiTimeframeSource"), result.getMissingFields());
        assertTrue(result.isManualReviewRequired());
        assertTrue(result.isNotTradeInstruction());
    }

    @Test
    void eventSourceOwnershipResultShouldCarryFailClosedDefaults() {
        SourceTraceEventSourceOwnershipResult result =
                SourceTraceEventSourceOwnershipResult.missingSource("BTCUSDT", "1m");

        assertEquals("BTCUSDT", result.getSymbol());
        assertEquals("1m", result.getTimeframe());
        assertEquals(SourceTraceEventSourceOwnershipStatusEnum.INCOMPLETE, result.getOwnershipStatus());
        assertEquals(SourceTraceEventSourceMissingReasonEnum.MISSING_SOURCE, result.getMissingReason());
        assertEquals(SourceTraceEventSourceReviewModeEnum.REVIEW_ONLY, result.getReviewMode());
        assertNull(result.getEventSource());
        assertEquals(List.of("eventSource"), result.getMissingFields());
        assertTrue(result.isManualReviewRequired());
        assertTrue(result.isNotTradeInstruction());
    }

    @Test
    void wickSourceOwnershipResultShouldCarryFailClosedDefaults() {
        SourceTraceWickSourceOwnershipResult result =
                SourceTraceWickSourceOwnershipResult.missingSource("BTCUSDT", "1m");

        assertEquals("BTCUSDT", result.getSymbol());
        assertEquals("1m", result.getTimeframe());
        assertEquals(SourceTraceWickSourceOwnershipStatusEnum.INCOMPLETE, result.getOwnershipStatus());
        assertEquals(SourceTraceWickSourceMissingReasonEnum.MISSING_SOURCE, result.getMissingReason());
        assertEquals(SourceTraceWickSourceReviewModeEnum.REVIEW_ONLY, result.getReviewMode());
        assertNull(result.getWickSource());
        assertEquals(List.of("wickSource"), result.getMissingFields());
        assertTrue(result.isManualReviewRequired());
        assertTrue(result.isNotTradeInstruction());
    }

    @Test
    void derivativesRiskContextShouldCarryRiskEvidenceAndFallbackState() {
        DerivativesRiskContextDTO context = new DerivativesRiskContextDTO();
        LocalDateTime contextTime = LocalDateTime.of(2026, 5, 16, 10, 0);
        Map<String, BigDecimal> leverageDistribution = new LinkedHashMap<>();
        leverageDistribution.put("1-5x", BigDecimal.valueOf(0.55));
        leverageDistribution.put("20x+", BigDecimal.valueOf(0.12));

        context.setSymbol("BTCUSDT");
        context.setTimeframe("1h");
        context.setContextTime(contextTime);
        context.setOpenInterestHistory(List.of(BigDecimal.valueOf(100), BigDecimal.valueOf(120)));
        context.setOpenInterestDelta(BigDecimal.valueOf(20));
        context.setLastFundingRate(new BigDecimal("0.0001"));
        context.setFundingHistory(List.of(new BigDecimal("0.00008"), new BigDecimal("0.0001")));
        context.setLiquidationCluster(List.of(BigDecimal.valueOf(66000), BigDecimal.valueOf(72000)));
        context.setLeverageDistribution(leverageDistribution);
        context.setLongShortRatio(BigDecimal.valueOf(1.2));
        context.setLiquidityStress("MEDIUM");
        context.setLiquidityStressReason("spread elevated");
        context.setEventWindowBlockers(List.of("macro_event"));
        context.setWickConfirmationSources(List.of("wick_source_ref"));
        context.setDataQualityScore(BigDecimal.valueOf(85));
        context.setFallbackStatus(SourceTraceFallbackStatusEnum.SAFE_FAIL_CLOSED_ONLY);
        context.setMissingFields(List.of("liquidationCluster"));

        assertEquals("BTCUSDT", context.getSymbol());
        assertEquals("1h", context.getTimeframe());
        assertEquals(contextTime, context.getContextTime());
        assertEquals(List.of(BigDecimal.valueOf(100), BigDecimal.valueOf(120)), context.getOpenInterestHistory());
        assertEquals(BigDecimal.valueOf(20), context.getOpenInterestDelta());
        assertEquals(new BigDecimal("0.0001"), context.getLastFundingRate());
        assertEquals(List.of(new BigDecimal("0.00008"), new BigDecimal("0.0001")), context.getFundingHistory());
        assertEquals(List.of(BigDecimal.valueOf(66000), BigDecimal.valueOf(72000)), context.getLiquidationCluster());
        assertEquals(leverageDistribution, context.getLeverageDistribution());
        assertEquals(BigDecimal.valueOf(1.2), context.getLongShortRatio());
        assertEquals("MEDIUM", context.getLiquidityStress());
        assertEquals("spread elevated", context.getLiquidityStressReason());
        assertEquals(List.of("macro_event"), context.getEventWindowBlockers());
        assertEquals(List.of("wick_source_ref"), context.getWickConfirmationSources());
        assertEquals(BigDecimal.valueOf(85), context.getDataQualityScore());
        assertEquals(SourceTraceFallbackStatusEnum.SAFE_FAIL_CLOSED_ONLY, context.getFallbackStatus());
        assertEquals(List.of("liquidationCluster"), context.getMissingFields());
        assertFalse(context.isComplete());
        assertTrue(context.isManualReviewRequired());
        assertTrue(context.isNotTradeInstruction());
    }

    @Test
    void listAndMapFieldsShouldBeDefensivelyCopied() {
        SourceTraceDTO trace = new SourceTraceDTO();
        List<BigDecimal> tpSources = new java.util.ArrayList<>();
        tpSources.add(BigDecimal.ONE);
        trace.setTpPriceSources(tpSources);
        tpSources.add(BigDecimal.TEN);

        DerivativesRiskContextDTO context = new DerivativesRiskContextDTO();
        List<BigDecimal> oiHistory = new java.util.ArrayList<>();
        oiHistory.add(BigDecimal.ONE);
        Map<String, BigDecimal> leverageDistribution = new LinkedHashMap<>();
        leverageDistribution.put("1-5x", BigDecimal.ONE);
        context.setOpenInterestHistory(oiHistory);
        context.setLeverageDistribution(leverageDistribution);
        oiHistory.add(BigDecimal.TEN);
        leverageDistribution.put("20x+", BigDecimal.TEN);

        assertEquals(List.of(BigDecimal.ONE), trace.getTpPriceSources());
        assertEquals(List.of(BigDecimal.ONE), context.getOpenInterestHistory());
        assertEquals(Map.of("1-5x", BigDecimal.ONE), context.getLeverageDistribution());
    }

    @Test
    void contractObjectsShouldNotExposeTradingExecutionMethods() {
        List<Class<?>> types = List.of(
                SourceTraceDTO.class,
                DerivativesRiskContextDTO.class,
                SourceTraceEntrySourceOwnershipResult.class,
                SourceTraceStopSourceOwnershipResult.class,
                SourceTraceTakeProfitSourceOwnershipResult.class,
                SourceTraceRiskRewardSourceOwnershipResult.class,
                SourceTraceLiquiditySourceOwnershipResult.class,
                SourceTraceMultiTimeframeSourceOwnershipResult.class,
                SourceTraceEventSourceOwnershipResult.class,
                SourceTraceWickSourceOwnershipResult.class,
                SourceCompletenessContract.class
        );
        List<String> forbiddenFragments = Arrays.asList(
                "executeorder",
                "placeorder",
                "closeposition",
                "reverseposition",
                "autotrade",
                "orderid",
                "orderside",
                "executionid"
        );

        for (Class<?> type : types) {
            for (Method method : type.getMethods()) {
                String name = method.getName().toLowerCase(Locale.ROOT);
                for (String forbidden : forbiddenFragments) {
                    assertFalse(name.contains(forbidden), type.getSimpleName() + "." + method.getName());
                }
            }
        }
    }

    private SourceTraceDTO completeSourceTrace() {
        SourceTraceDTO trace = new SourceTraceDTO();
        trace.setSymbol("BTCUSDT");
        trace.setTimeframe("1h");
        trace.setEntryPriceSource(BigDecimal.valueOf(68000));
        trace.setEntrySourceType("support");
        trace.setEntrySourceTimeframe("1h");
        trace.setEntrySourceReason("support retest");
        trace.setEntrySourceRef("entry-ref");
        trace.setStopPriceSource(BigDecimal.valueOf(66800));
        trace.setStopSourceType("swing_low");
        trace.setStopSourceTimeframe("1h");
        trace.setStopSourceReason("recent swing low");
        trace.setStopSourceRef("stop-ref");
        trace.setTpPriceSources(List.of(BigDecimal.valueOf(70400)));
        trace.setTpSourceType("rr_ladder");
        trace.setTpSourceTimeframe("1h");
        trace.setTpSourceReason("2R ladder");
        trace.setTpSourceRef("tp-ref");
        trace.setRrSource(BigDecimal.valueOf(2));
        trace.setRrRuleRef("min_rr_2");
        trace.setLiquiditySource("depth_snapshot");
        trace.setMultiTimeframeSource("1h_4h_alignment");
        trace.setEventSource("no_major_event_window");
        trace.setWickSource("wick_confirmed");
        return trace;
    }

    private record SourceTraceFieldMutation(String fieldName, Consumer<SourceTraceDTO> mutation) {
        private void apply(SourceTraceDTO trace) {
            mutation.accept(trace);
        }
    }
}
