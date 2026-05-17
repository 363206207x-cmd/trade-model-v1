package org.example.trademodel.service.dashboard;

import org.example.trademodel.dto.planboundary.RuntimeKlineContextDTO;
import org.example.trademodel.dto.planboundary.SourceTraceFallbackStatusEnum;
import org.example.trademodel.vo.DecisionResultVO;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

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
        assertTrue(context.isManualReviewRequired());
        assertTrue(context.isNotTradeInstruction());
        assertFalse(context.isComplete());
    }
}
