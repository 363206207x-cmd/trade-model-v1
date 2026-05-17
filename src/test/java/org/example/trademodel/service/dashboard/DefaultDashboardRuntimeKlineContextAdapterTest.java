package org.example.trademodel.service.dashboard;

import org.example.trademodel.dto.ohlcv.PersistedOhlcvReadinessResult;
import org.example.trademodel.dto.ohlcv.PersistedOhlcvReadinessStatus;
import org.example.trademodel.dto.ohlcv.PersistedOhlcvStaleReasonCode;
import org.example.trademodel.dto.planboundary.RuntimeKlineContextDTO;
import org.example.trademodel.dto.planboundary.SourceTraceFallbackStatusEnum;
import org.example.trademodel.service.PersistedOhlcvQueryService;
import org.example.trademodel.vo.DecisionResultVO;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DefaultDashboardRuntimeKlineContextAdapterTest {

    private final DefaultDashboardRuntimeKlineContextAdapter adapter =
            new DefaultDashboardRuntimeKlineContextAdapter();

    @Test
    void shouldReturnUnavailableIncompleteRuntimeKlineBoundary() {
        DecisionResultVO decision = new DecisionResultVO();
        decision.setSymbol("BTCUSDT");
        decision.setTimeframe("1h");
        decision.setLatestPrice(BigDecimal.valueOf(68100));
        decision.setDataQualityScore(88);

        RuntimeKlineContextDTO context = adapter.buildUnavailableContext("BTCUSDT", decision);

        assertEquals("BTCUSDT", context.getSymbol());
        assertEquals("1h", context.getTimeframe());
        assertEquals(SourceTraceFallbackStatusEnum.INCOMPLETE, context.getFallbackStatus());
        assertTrue(context.getMissingFields().contains("persistedOhlcvWindow"));
        assertTrue(context.getMissingFields().contains("klineItems"));
        assertTrue(context.getMissingFields().contains("klineFreshness"));
        assertTrue(context.getMissingFields().contains("staleStatus"));
        assertTrue(context.getMissingFields().contains("runtimeLatestPriceSource"));
        assertTrue(context.getMissingFields().contains("dataQualityScoreSource"));
        assertNull(context.getLatestPrice());
        assertNull(context.getDataQualityScore());
        assertNull(context.getEntryPriceSource());
        assertNull(context.getStopPriceSource());
        assertTrue(context.getTpPriceSources().isEmpty());
        assertNull(context.getRrSource());
        assertNull(context.getLiquiditySource());
        assertNull(context.getEventSource());
        assertNull(context.getWickSource());
        assertFalse(context.isComplete());
        assertTrue(context.isManualReviewRequired());
        assertTrue(context.isNotTradeInstruction());
    }

    @Test
    void shouldKeepMissingDecisionAndTimeframeExplicit() {
        RuntimeKlineContextDTO context = adapter.buildUnavailableContext("ETHUSDT", null);

        assertEquals("ETHUSDT", context.getSymbol());
        assertNull(context.getTimeframe());
        assertEquals(SourceTraceFallbackStatusEnum.INCOMPLETE, context.getFallbackStatus());
        assertTrue(context.getMissingFields().contains("decision"));
        assertTrue(context.getMissingFields().contains("timeframe"));
        assertTrue(context.getMissingFields().contains("persistedOhlcvWindow"));
        assertTrue(context.getMissingFields().contains("staleStatus"));
        assertEquals("UNKNOWN", context.getPersistedOhlcvReadinessStatus());
        assertEquals("POLICY_NOT_CONFIGURED", context.getPersistedOhlcvStaleReasonCode());
        assertTrue(context.getPersistedOhlcvMissingFields().contains("persistedOhlcvReadinessService"));
        assertTrue(context.isManualReviewRequired());
        assertTrue(context.isNotTradeInstruction());
        assertFalse(context.isComplete());
    }

    @Test
    void shouldExposeMissingPersistedOhlcvReadinessMetadataWithoutCompletingRuntimeKline() {
        DefaultDashboardRuntimeKlineContextAdapter adapterWithReadiness =
                new DefaultDashboardRuntimeKlineContextAdapter(readinessService(
                        readiness(
                                PersistedOhlcvReadinessStatus.MISSING,
                                PersistedOhlcvStaleReasonCode.NO_BARS_FOR_SYMBOL_TIMEFRAME,
                                "No closed persisted OHLCV bars exist for symbol/timeframe.",
                                List.of("persistedOhlcvWindow", "klineItems")
                        )
                ));
        DecisionResultVO decision = new DecisionResultVO();
        decision.setSymbol("BTCUSDT");
        decision.setTimeframe("1m");

        RuntimeKlineContextDTO context = adapterWithReadiness.buildUnavailableContext("BTCUSDT", decision);

        assertEquals("MISSING", context.getPersistedOhlcvReadinessStatus());
        assertEquals("NO_BARS_FOR_SYMBOL_TIMEFRAME", context.getPersistedOhlcvStaleReasonCode());
        assertEquals("No closed persisted OHLCV bars exist for symbol/timeframe.",
                context.getPersistedOhlcvStaleReasonText());
        assertEquals(List.of("persistedOhlcvWindow", "klineItems"), context.getPersistedOhlcvMissingFields());
        assertEquals(SourceTraceFallbackStatusEnum.INCOMPLETE, context.getFallbackStatus());
        assertTrue(context.getMissingFields().contains("runtimeLatestPriceSource"));
        assertNull(context.getLatestPrice());
        assertNull(context.getDataQualityScore());
        assertFalse(context.isComplete());
        assertTrue(context.isManualReviewRequired());
        assertTrue(context.isNotTradeInstruction());
    }

    @Test
    void shouldKeepFreshPersistedOhlcvReadinessAsMetadataOnly() {
        DefaultDashboardRuntimeKlineContextAdapter adapterWithReadiness =
                new DefaultDashboardRuntimeKlineContextAdapter(readinessService(
                        readiness(
                                PersistedOhlcvReadinessStatus.FRESH,
                                PersistedOhlcvStaleReasonCode.NONE,
                                "Persisted OHLCV window is fresh.",
                                List.of()
                        )
                ));
        DecisionResultVO decision = new DecisionResultVO();
        decision.setSymbol("BTCUSDT");
        decision.setTimeframe("1m");

        RuntimeKlineContextDTO context = adapterWithReadiness.buildUnavailableContext("BTCUSDT", decision);

        assertEquals("FRESH", context.getPersistedOhlcvReadinessStatus());
        assertEquals("NONE", context.getPersistedOhlcvStaleReasonCode());
        assertTrue(context.getPersistedOhlcvMissingFields().isEmpty());
        assertEquals(SourceTraceFallbackStatusEnum.INCOMPLETE, context.getFallbackStatus());
        assertTrue(context.getMissingFields().contains("runtimeLatestPriceSource"));
        assertTrue(context.getMissingFields().contains("entryPriceSource"));
        assertNull(context.getLatestPrice());
        assertNull(context.getEntryPriceSource());
        assertNull(context.getStopPriceSource());
        assertTrue(context.getTpPriceSources().isEmpty());
        assertNull(context.getRrSource());
        assertFalse(context.isComplete());
        assertTrue(context.isManualReviewRequired());
        assertTrue(context.isNotTradeInstruction());
    }

    private PersistedOhlcvQueryService readinessService(PersistedOhlcvReadinessResult result) {
        return (symbol, timeframe, requiredWindowSize, maxReadLagMs) -> result;
    }

    private PersistedOhlcvReadinessResult readiness(
            PersistedOhlcvReadinessStatus status,
            PersistedOhlcvStaleReasonCode reasonCode,
            String reasonText,
            List<String> missingFields
    ) {
        PersistedOhlcvReadinessResult result = new PersistedOhlcvReadinessResult();
        result.setStatus(status);
        result.setStaleReasonCode(reasonCode);
        result.setStaleReasonText(reasonText);
        result.setMissingFields(missingFields);
        result.setManualReviewRequired(true);
        result.setNotTradeInstruction(true);
        return result;
    }
}
