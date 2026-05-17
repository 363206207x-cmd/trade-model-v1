package org.example.trademodel.service.dashboard;

import org.example.trademodel.dto.ohlcv.PersistedOhlcvReadinessResult;
import org.example.trademodel.dto.ohlcv.PersistedOhlcvReadinessStatus;
import org.example.trademodel.dto.ohlcv.PersistedOhlcvStaleReasonCode;
import org.example.trademodel.dto.planboundary.RuntimeKlineContextDTO;
import org.example.trademodel.dto.planboundary.SourceTraceFallbackStatusEnum;
import org.example.trademodel.entity.PersistedOhlcvBarDO;
import org.example.trademodel.service.PersistedOhlcvQueryService;
import org.example.trademodel.service.impl.RuntimeKlineContextAssemblyServiceImpl;
import org.example.trademodel.vo.DecisionResultVO;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
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
        assertTrue(context.getMissingFields().contains("klineItems"));
        assertTrue(context.getMissingFields().contains("entryPriceSource"));
        assertNull(context.getLatestPrice());
        assertTrue(context.getKlineItems().isEmpty());
        assertNull(context.getEntryPriceSource());
        assertNull(context.getStopPriceSource());
        assertTrue(context.getTpPriceSources().isEmpty());
        assertNull(context.getRrSource());
        assertFalse(context.isComplete());
        assertTrue(context.isManualReviewRequired());
        assertTrue(context.isNotTradeInstruction());
    }

    @Test
    void shouldExposeFreshAssembledRuntimeBoundaryWhenPersistedBarsAreSafe() {
        DefaultDashboardRuntimeKlineContextAdapter adapterWithAssembly =
                new DefaultDashboardRuntimeKlineContextAdapter(
                        readinessService(freshReadiness(List.of(
                                bar(60_000L, 119_999L, "101.10", "130.00", "100.50", "102.30"),
                                bar(0L, 59_999L, "100.00", "105.00", "98.00", "101.10")
                        ))),
                        new RuntimeKlineContextAssemblyServiceImpl()
                );
        DecisionResultVO decision = new DecisionResultVO();
        decision.setSymbol("BTCUSDT");
        decision.setTimeframe("1m");
        decision.setLatestPrice(BigDecimal.valueOf(68100));

        RuntimeKlineContextDTO context = adapterWithAssembly.buildUnavailableContext("BTCUSDT", decision);

        assertEquals("BTCUSDT", context.getSymbol());
        assertEquals("1m", context.getTimeframe());
        assertEquals("FRESH", context.getPersistedOhlcvReadinessStatus());
        assertEquals("NONE", context.getPersistedOhlcvStaleReasonCode());
        assertTrue(context.getPersistedOhlcvMissingFields().isEmpty());
        assertNull(context.getFallbackStatus());
        assertTrue(context.getMissingFields().isEmpty());
        assertEquals(0, context.getLatestPrice().compareTo(new BigDecimal("102.30")));
        assertEquals(2, context.getKlineItems().size());
        assertEquals(0, context.getKlineItems().get(0).getClosePrice().compareTo(new BigDecimal("102.30")));
        assertEquals("LOCAL_FIXTURE", context.getKlineItems().get(0).getProvider());
        assertNull(context.getEntryPriceSource());
        assertNull(context.getStopPriceSource());
        assertTrue(context.getTpPriceSources().isEmpty());
        assertNull(context.getRrSource());
        assertNull(context.getLiquiditySource());
        assertNull(context.getEventSource());
        assertNull(context.getWickSource());
        assertTrue(context.isComplete());
        assertTrue(context.isManualReviewRequired());
        assertTrue(context.isNotTradeInstruction());
    }

    @Test
    void shouldFailClosedWhenFreshReadinessHasUnsafeOpenCandle() {
        PersistedOhlcvBarDO open = bar(0L, 59_999L, "100.00", "105.00", "98.00", "101.10");
        open.setClosed(false);
        PersistedOhlcvReadinessResult readiness = freshReadiness(List.of(open));
        readiness.setRequiredWindowSize(1);
        DefaultDashboardRuntimeKlineContextAdapter adapterWithAssembly =
                new DefaultDashboardRuntimeKlineContextAdapter(
                        readinessService(readiness),
                        new RuntimeKlineContextAssemblyServiceImpl()
                );
        DecisionResultVO decision = new DecisionResultVO();
        decision.setSymbol("BTCUSDT");
        decision.setTimeframe("1m");

        RuntimeKlineContextDTO context = adapterWithAssembly.buildUnavailableContext("BTCUSDT", decision);

        assertEquals("FRESH", context.getPersistedOhlcvReadinessStatus());
        assertEquals(SourceTraceFallbackStatusEnum.INCOMPLETE, context.getFallbackStatus());
        assertTrue(context.getMissingFields().contains("closed"));
        assertTrue(context.getMissingFields().contains("entryPriceSource"));
        assertNull(context.getLatestPrice());
        assertTrue(context.getKlineItems().isEmpty());
        assertNull(context.getEntryPriceSource());
        assertNull(context.getStopPriceSource());
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

    private PersistedOhlcvReadinessResult freshReadiness(List<PersistedOhlcvBarDO> bars) {
        PersistedOhlcvReadinessResult result = readiness(
                PersistedOhlcvReadinessStatus.FRESH,
                PersistedOhlcvStaleReasonCode.NONE,
                "Persisted OHLCV window is fresh.",
                List.of()
        );
        result.setSymbol("BTCUSDT");
        result.setTimeframe("1m");
        result.setRequiredWindowSize(2);
        result.setBars(bars);
        result.setLatestCloseTimeMs(bars.stream()
                .filter(bar -> bar.getCloseTimeMs() != null)
                .map(PersistedOhlcvBarDO::getCloseTimeMs)
                .max(Long::compareTo)
                .orElse(null));
        result.setLatestIngestedAt(LocalDateTime.of(2026, 5, 17, 10, 0));
        return result;
    }

    private PersistedOhlcvBarDO bar(
            Long openTimeMs,
            Long closeTimeMs,
            String openPrice,
            String highPrice,
            String lowPrice,
            String closePrice
    ) {
        PersistedOhlcvBarDO bar = new PersistedOhlcvBarDO();
        bar.setSymbol("BTCUSDT");
        bar.setTimeframe("1m");
        bar.setOpenTimeMs(openTimeMs);
        bar.setCloseTimeMs(closeTimeMs);
        bar.setOpenPrice(new BigDecimal(openPrice));
        bar.setHighPrice(new BigDecimal(highPrice));
        bar.setLowPrice(new BigDecimal(lowPrice));
        bar.setClosePrice(new BigDecimal(closePrice));
        bar.setVolume(new BigDecimal("123.45"));
        bar.setClosed(true);
        bar.setProvider("LOCAL_FIXTURE");
        bar.setProviderMarketType("USDT_PERP");
        bar.setSourceEndpoint("persisted-ohlcv-fixture");
        bar.setSourceBatchId("batch-1");
        bar.setSourceTraceId("trace-1");
        bar.setSourceVersion(1);
        bar.setIngestedAt(LocalDateTime.of(2026, 5, 17, 10, 0));
        bar.setQualityStatus("OK");
        bar.setIsDeleted(0);
        return bar;
    }
}
