package org.example.trademodel.service.dashboard;

import org.example.trademodel.dto.ohlcv.PersistedOhlcvReadinessResult;
import org.example.trademodel.dto.ohlcv.PersistedOhlcvReadinessStatus;
import org.example.trademodel.dto.ohlcv.PersistedOhlcvStaleReasonCode;
import org.example.trademodel.dto.planboundary.DerivativesRiskContextDTO;
import org.example.trademodel.dto.planboundary.RuntimeKlineContextDTO;
import org.example.trademodel.dto.planboundary.SourceTraceDTO;
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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DefaultDashboardSourceTraceDetailAdapterTest {
    private final DefaultDashboardSourceTraceDetailAdapter adapter = new DefaultDashboardSourceTraceDetailAdapter();

    @Test
    void shouldBuildFailClosedContextWhenProductionSourcesAreMissing() {
        DecisionResultVO decision = new DecisionResultVO();
        decision.setAnalysisId("ana-btc");

        DashboardSourceTraceDetailAdapter.DashboardSourceTraceDetailContext context =
                adapter.build("BTCUSDT", decision);

        SourceTraceDTO sourceTrace = context.getSourceTrace();
        RuntimeKlineContextDTO runtimeKlineContext = context.getRuntimeKlineContext();
        DerivativesRiskContextDTO derivativesRiskContext = context.getDerivativesRiskContext();

        assertNotNull(sourceTrace);
        assertNotNull(runtimeKlineContext);
        assertNotNull(derivativesRiskContext);
        assertEquals("BTCUSDT", sourceTrace.getSymbol());
        assertEquals("BTCUSDT", derivativesRiskContext.getSymbol());
        assertEquals(SourceTraceFallbackStatusEnum.INCOMPLETE, sourceTrace.getFallbackStatus());
        assertEquals(SourceTraceFallbackStatusEnum.SAFE_FAIL_CLOSED_ONLY, derivativesRiskContext.getFallbackStatus());
        assertTrue(sourceTrace.getMissingFields().contains("runtimeKlineContext"));
        assertTrue(sourceTrace.getMissingFields().contains("entryPriceSource"));
        assertTrue(sourceTrace.getMissingFields().contains("stopPriceSource"));
        assertTrue(sourceTrace.getMissingFields().contains("tpPriceSources"));
        assertTrue(sourceTrace.getMissingFields().contains("rrSource"));
        assertTrue(sourceTrace.getMissingFields().contains("liquiditySource"));
        assertTrue(sourceTrace.getMissingFields().contains("multiTimeframeSource"));
        assertTrue(sourceTrace.getMissingFields().contains("eventSource"));
        assertTrue(sourceTrace.getMissingFields().contains("wickSource"));
        assertTrue(derivativesRiskContext.getMissingFields().contains("openInterestHistory"));
        assertTrue(derivativesRiskContext.getMissingFields().contains("fundingHistory"));
        assertTrue(derivativesRiskContext.getMissingFields().contains("liquidationCluster"));
        assertTrue(derivativesRiskContext.getMissingFields().contains("leverageDistribution"));
        assertTrue(derivativesRiskContext.getMissingFields().contains("longShortRatio"));
        assertTrue(derivativesRiskContext.getMissingFields().contains("liquidityStress"));
        assertTrue(derivativesRiskContext.getMissingFields().contains("eventWindowBlockers"));
        assertTrue(derivativesRiskContext.getMissingFields().contains("wickConfirmationSources"));
        assertTrue(sourceTrace.isManualReviewRequired());
        assertTrue(sourceTrace.isNotTradeInstruction());
        assertTrue(derivativesRiskContext.isManualReviewRequired());
        assertTrue(derivativesRiskContext.isNotTradeInstruction());
        assertEquals(SourceTraceFallbackStatusEnum.INCOMPLETE, runtimeKlineContext.getFallbackStatus());
        assertTrue(runtimeKlineContext.isManualReviewRequired());
        assertTrue(runtimeKlineContext.isNotTradeInstruction());
        assertFalse(sourceTrace.hasRequiredBoundarySources());
    }

    @Test
    void shouldRecordMissingDecisionWithoutChangingSafetyDefaults() {
        DashboardSourceTraceDetailAdapter.DashboardSourceTraceDetailContext context =
                adapter.build("ETHUSDT", null);

        assertTrue(context.getSourceTrace().getMissingFields().contains("decision"));
        assertTrue(context.getDerivativesRiskContext().getMissingFields().contains("decision"));
        assertEquals("UNAVAILABLE", context.getSourceTrace().getRuntimeKlineContextStatus());
        assertEquals("dashboardDetail.noRuntimeKlineContext", context.getSourceTrace().getRuntimeKlineContextSource());
        assertTrue(context.getSourceTrace().isManualReviewRequired());
        assertTrue(context.getSourceTrace().isNotTradeInstruction());
        assertTrue(context.getDerivativesRiskContext().isManualReviewRequired());
        assertTrue(context.getDerivativesRiskContext().isNotTradeInstruction());
    }

    @Test
    void shouldWireOnlyProductionBackedDecisionFieldsAndKeepBoundarySourcesMissing() {
        DecisionResultVO decision = new DecisionResultVO();
        decision.setDecisionId("dec-btc-production-backed");
        decision.setAnalysisId("ana-btc-production-backed");
        decision.setSymbol("BTCUSDT");
        decision.setCreateTime(LocalDateTime.of(2026, 5, 17, 10, 30));
        decision.setTimeframe("1h");
        decision.setMultiTfConvergence("STRONG");
        decision.setDataQualityScore(88);
        decision.setLatestPrice(BigDecimal.valueOf(68100));
        decision.setPriceUpdateTimeMs(1710000000000L);
        decision.setEntryZone("68000-68200");
        decision.setStopLoss("67400");
        decision.setTakeProfitRules("TP1 69000");
        decision.setLiquidationPrice(BigDecimal.valueOf(62000));

        DashboardSourceTraceDetailAdapter.DashboardSourceTraceDetailContext context =
                adapter.build("BTCUSDT", decision);

        SourceTraceDTO sourceTrace = context.getSourceTrace();
        DerivativesRiskContextDTO derivativesRiskContext = context.getDerivativesRiskContext();

        assertEquals("dec-btc-production-backed", sourceTrace.getDecisionId());
        assertEquals("DecisionResultVO.decisionId", sourceTrace.getDecisionIdSource());
        assertEquals("ana-btc-production-backed", sourceTrace.getAnalysisId());
        assertEquals("DecisionResultVO.analysisId", sourceTrace.getAnalysisIdSource());
        assertEquals("BTCUSDT", sourceTrace.getSymbol());
        assertEquals("DecisionResultVO.symbol", sourceTrace.getSymbolSource());
        assertEquals(LocalDateTime.of(2026, 5, 17, 10, 30), sourceTrace.getDecisionCreateTime());
        assertEquals("DecisionResultVO.createTime", sourceTrace.getDecisionCreateTimeSource());
        assertEquals("1h", sourceTrace.getTimeframe());
        assertEquals("DecisionResultVO.timeframe", sourceTrace.getTimeframeSource());
        assertEquals("UNAVAILABLE", sourceTrace.getRuntimeKlineContextStatus());
        assertEquals("dashboardDetail.noRuntimeKlineContext", sourceTrace.getRuntimeKlineContextSource());
        assertEquals(BigDecimal.valueOf(68100), sourceTrace.getQuoteLatestPrice());
        assertEquals("DecisionResultVO.latestPrice", sourceTrace.getQuoteLatestPriceSource());
        assertEquals(1710000000000L, sourceTrace.getQuotePriceUpdateTimeMs());
        assertEquals("DecisionResultVO.priceUpdateTimeMs", sourceTrace.getQuotePriceUpdateTimeSource());
        assertEquals("QUOTE_UPDATE_TIME_ONLY", sourceTrace.getQuoteFreshnessStatus());
        assertEquals(BigDecimal.valueOf(88), sourceTrace.getDataQualityScore());
        assertEquals("DecisionResultVO.dataQualityScore", sourceTrace.getDataQualityScoreSource());
        assertEquals("DecisionResultVO.multiTfConvergence", sourceTrace.getMultiTimeframeSource());
        assertFalse(sourceTrace.getMissingFields().contains("timeframe"));
        assertFalse(sourceTrace.getMissingFields().contains("multiTimeframeSource"));
        assertEquals("1h", derivativesRiskContext.getTimeframe());
        assertEquals("DecisionResultVO.timeframe", derivativesRiskContext.getTimeframeSource());
        assertEquals(BigDecimal.valueOf(88), derivativesRiskContext.getDataQualityScore());
        assertEquals("DecisionResultVO.dataQualityScore", derivativesRiskContext.getDataQualityScoreSource());
        assertFalse(derivativesRiskContext.getMissingFields().contains("timeframe"));
        assertFalse(derivativesRiskContext.getMissingFields().contains("dataQualityScore"));

        assertNull(sourceTrace.getEntryPriceSource());
        assertNull(sourceTrace.getStopPriceSource());
        assertTrue(sourceTrace.getTpPriceSources().isEmpty());
        assertNull(sourceTrace.getRrSource());
        assertTrue(sourceTrace.getMissingFields().contains("latestPrice"));
        assertTrue(sourceTrace.getMissingFields().contains("entryPriceSource"));
        assertTrue(sourceTrace.getMissingFields().contains("stopPriceSource"));
        assertTrue(sourceTrace.getMissingFields().contains("tpPriceSources"));
        assertTrue(sourceTrace.getMissingFields().contains("rrSource"));
        assertTrue(sourceTrace.getMissingFields().contains("liquiditySource"));
        assertTrue(sourceTrace.getMissingFields().contains("eventSource"));
        assertTrue(sourceTrace.getMissingFields().contains("wickSource"));
        assertTrue(derivativesRiskContext.getLiquidationCluster().isEmpty());
        assertTrue(derivativesRiskContext.getMissingFields().contains("liquidationCluster"));
        assertEquals(SourceTraceFallbackStatusEnum.INCOMPLETE, sourceTrace.getFallbackStatus());
        assertEquals(SourceTraceFallbackStatusEnum.SAFE_FAIL_CLOSED_ONLY, derivativesRiskContext.getFallbackStatus());
        assertFalse(sourceTrace.hasRequiredBoundarySources());
    }

    @Test
    void shouldKeepQuoteAndDataQualityMetadataFromCompletingSourceTrace() {
        DecisionResultVO decision = new DecisionResultVO();
        decision.setDecisionId("dec-anchor-safe");
        decision.setAnalysisId("ana-anchor-safe");
        decision.setSymbol("BTCUSDT");
        decision.setCreateTime(LocalDateTime.of(2026, 5, 17, 11, 0));
        decision.setTimeframe("1h");
        decision.setLatestPrice(BigDecimal.valueOf(68100));
        decision.setPriceUpdateTimeMs(1710000000000L);
        decision.setDataQualityScore(88);
        decision.setEntryZone("68000-68200");
        decision.setStopLoss("67400");
        decision.setTakeProfitRules("TP1 69000");
        decision.setExecutionPlanSummary("Entry 68000, stop 67400, TP1 69000");
        decision.setRecommendedAction("OPEN_LONG");

        DashboardSourceTraceDetailAdapter.DashboardSourceTraceDetailContext context =
                adapter.build("BTCUSDT", decision);

        SourceTraceDTO sourceTrace = context.getSourceTrace();

        assertEquals("dec-anchor-safe", sourceTrace.getDecisionId());
        assertEquals("DecisionResultVO.decisionId", sourceTrace.getDecisionIdSource());
        assertEquals("ana-anchor-safe", sourceTrace.getAnalysisId());
        assertEquals("DecisionResultVO.analysisId", sourceTrace.getAnalysisIdSource());
        assertEquals("DecisionResultVO.symbol", sourceTrace.getSymbolSource());
        assertEquals(LocalDateTime.of(2026, 5, 17, 11, 0), sourceTrace.getDecisionCreateTime());
        assertEquals("DecisionResultVO.createTime", sourceTrace.getDecisionCreateTimeSource());
        assertEquals("1h", sourceTrace.getTimeframe());
        assertEquals("DecisionResultVO.timeframe", sourceTrace.getTimeframeSource());
        assertEquals(BigDecimal.valueOf(68100), sourceTrace.getQuoteLatestPrice());
        assertEquals("DecisionResultVO.latestPrice", sourceTrace.getQuoteLatestPriceSource());
        assertEquals("QUOTE_UPDATE_TIME_ONLY", sourceTrace.getQuoteFreshnessStatus());
        assertEquals(BigDecimal.valueOf(88), sourceTrace.getDataQualityScore());
        assertNull(sourceTrace.getEntryPriceSource());
        assertNull(sourceTrace.getStopPriceSource());
        assertTrue(sourceTrace.getTpPriceSources().isEmpty());
        assertNull(sourceTrace.getRrSource());
        assertEquals(SourceTraceFallbackStatusEnum.INCOMPLETE, sourceTrace.getFallbackStatus());
        assertTrue(sourceTrace.getMissingFields().contains("runtimeKlineContext"));
        assertTrue(sourceTrace.getMissingFields().contains("latestPrice"));
        assertTrue(sourceTrace.getMissingFields().contains("entryPriceSource"));
        assertTrue(sourceTrace.getMissingFields().contains("stopPriceSource"));
        assertTrue(sourceTrace.getMissingFields().contains("tpPriceSources"));
        assertTrue(sourceTrace.getMissingFields().contains("rrSource"));
        assertFalse(sourceTrace.hasRequiredBoundarySources());
        assertTrue(sourceTrace.isManualReviewRequired());
        assertTrue(sourceTrace.isNotTradeInstruction());
    }

    @Test
    void shouldKeepTimeframeMissingWhenDecisionDoesNotOwnTimeframeSource() {
        DecisionResultVO decision = new DecisionResultVO();
        decision.setLatestPrice(BigDecimal.valueOf(68100));
        decision.setPriceUpdateTimeMs(1710000000000L);

        DashboardSourceTraceDetailAdapter.DashboardSourceTraceDetailContext context =
                adapter.build("BTCUSDT", decision);

        SourceTraceDTO sourceTrace = context.getSourceTrace();
        DerivativesRiskContextDTO derivativesRiskContext = context.getDerivativesRiskContext();

        assertNull(sourceTrace.getTimeframe());
        assertNull(sourceTrace.getTimeframeSource());
        assertTrue(sourceTrace.getMissingFields().contains("timeframe"));
        assertTrue(derivativesRiskContext.getMissingFields().contains("timeframe"));
        assertEquals("UNAVAILABLE", sourceTrace.getRuntimeKlineContextStatus());
        assertEquals("dashboardDetail.noRuntimeKlineContext", sourceTrace.getRuntimeKlineContextSource());
        assertEquals("QUOTE_UPDATE_TIME_ONLY", sourceTrace.getQuoteFreshnessStatus());
        assertEquals(SourceTraceFallbackStatusEnum.INCOMPLETE, sourceTrace.getFallbackStatus());
        assertFalse(sourceTrace.hasRequiredBoundarySources());
    }

    @Test
    void shouldKeepAnalysisAnchorMetadataOptionalAndFailClosedWhenUnavailable() {
        DecisionResultVO decision = new DecisionResultVO();

        DashboardSourceTraceDetailAdapter.DashboardSourceTraceDetailContext context =
                adapter.build("BTCUSDT", decision);

        SourceTraceDTO sourceTrace = context.getSourceTrace();

        assertEquals("BTCUSDT", sourceTrace.getSymbol());
        assertEquals("dashboardDetail.requestSymbol", sourceTrace.getSymbolSource());
        assertNull(sourceTrace.getDecisionId());
        assertNull(sourceTrace.getDecisionIdSource());
        assertNull(sourceTrace.getAnalysisId());
        assertNull(sourceTrace.getAnalysisIdSource());
        assertNull(sourceTrace.getDecisionCreateTime());
        assertNull(sourceTrace.getDecisionCreateTimeSource());
        assertEquals(SourceTraceFallbackStatusEnum.INCOMPLETE, sourceTrace.getFallbackStatus());
        assertTrue(sourceTrace.getMissingFields().contains("runtimeKlineContext"));
        assertTrue(sourceTrace.getMissingFields().contains("entryPriceSource"));
        assertTrue(sourceTrace.getMissingFields().contains("stopPriceSource"));
        assertTrue(sourceTrace.getMissingFields().contains("tpPriceSources"));
        assertFalse(sourceTrace.hasRequiredBoundarySources());
        assertTrue(sourceTrace.isManualReviewRequired());
        assertTrue(sourceTrace.isNotTradeInstruction());
    }

    @Test
    void shouldExposePersistedOhlcvReadinessMetadataWithoutCompletingSourceTrace() {
        DefaultDashboardSourceTraceDetailAdapter adapterWithReadiness =
                new DefaultDashboardSourceTraceDetailAdapter(
                        new DefaultDashboardRuntimeKlineContextAdapter(readinessService(
                                readiness(
                                        PersistedOhlcvReadinessStatus.FRESH,
                                        PersistedOhlcvStaleReasonCode.NONE,
                                        "Persisted OHLCV window is fresh.",
                                        List.of()
                                )
                        ))
                );
        DecisionResultVO decision = new DecisionResultVO();
        decision.setSymbol("BTCUSDT");
        decision.setTimeframe("1m");

        DashboardSourceTraceDetailAdapter.DashboardSourceTraceDetailContext context =
                adapterWithReadiness.build("BTCUSDT", decision);

        SourceTraceDTO sourceTrace = context.getSourceTrace();

        assertEquals("UNAVAILABLE", sourceTrace.getRuntimeKlineContextStatus());
        assertEquals("dashboardDetail.noRuntimeKlineContext", sourceTrace.getRuntimeKlineContextSource());
        assertEquals("FRESH", sourceTrace.getRuntimeKlineReadinessStatus());
        assertEquals("NONE", sourceTrace.getRuntimeKlineStaleReasonCode());
        assertEquals("Persisted OHLCV window is fresh.", sourceTrace.getRuntimeKlineStaleReasonText());
        assertTrue(sourceTrace.getRuntimeKlineReadinessMissingFields().isEmpty());
        assertEquals(SourceTraceFallbackStatusEnum.INCOMPLETE, sourceTrace.getFallbackStatus());
        assertTrue(sourceTrace.getMissingFields().contains("runtimeKlineContext"));
        assertTrue(sourceTrace.getMissingFields().contains("entryPriceSource"));
        assertTrue(sourceTrace.getMissingFields().contains("stopPriceSource"));
        assertTrue(sourceTrace.getMissingFields().contains("tpPriceSources"));
        assertTrue(sourceTrace.getMissingFields().contains("rrSource"));
        assertNull(sourceTrace.getEntryPriceSource());
        assertNull(sourceTrace.getStopPriceSource());
        assertTrue(sourceTrace.getTpPriceSources().isEmpty());
        assertNull(sourceTrace.getRrSource());
        assertFalse(sourceTrace.hasRequiredBoundarySources());
        assertTrue(sourceTrace.isManualReviewRequired());
        assertTrue(sourceTrace.isNotTradeInstruction());
    }

    @Test
    void shouldKeepSourceTraceIncompleteWhenRuntimeKlineAssemblyIsAvailable() {
        DefaultDashboardSourceTraceDetailAdapter adapterWithReadiness =
                new DefaultDashboardSourceTraceDetailAdapter(
                        new DefaultDashboardRuntimeKlineContextAdapter(
                                readinessService(freshReadiness(List.of(
                                        bar(60_000L, 119_999L, "101.10", "130.00", "100.50", "102.30"),
                                        bar(0L, 59_999L, "100.00", "105.00", "98.00", "101.10")
                                ))),
                                new RuntimeKlineContextAssemblyServiceImpl()
                        )
                );
        DecisionResultVO decision = new DecisionResultVO();
        decision.setSymbol("BTCUSDT");
        decision.setTimeframe("1m");

        DashboardSourceTraceDetailAdapter.DashboardSourceTraceDetailContext context =
                adapterWithReadiness.build("BTCUSDT", decision);

        SourceTraceDTO sourceTrace = context.getSourceTrace();
        RuntimeKlineContextDTO runtimeKlineContext = context.getRuntimeKlineContext();

        assertEquals("UNAVAILABLE", sourceTrace.getRuntimeKlineContextStatus());
        assertEquals("dashboardDetail.noRuntimeKlineContext", sourceTrace.getRuntimeKlineContextSource());
        assertEquals("FRESH", sourceTrace.getRuntimeKlineReadinessStatus());
        assertEquals("NONE", sourceTrace.getRuntimeKlineStaleReasonCode());
        assertTrue(sourceTrace.getRuntimeKlineReadinessMissingFields().isEmpty());
        assertNotNull(runtimeKlineContext);
        assertNull(runtimeKlineContext.getFallbackStatus());
        assertTrue(runtimeKlineContext.getMissingFields().isEmpty());
        assertEquals(0, runtimeKlineContext.getLatestPrice().compareTo(new BigDecimal("102.30")));
        assertEquals(2, runtimeKlineContext.getKlineItems().size());
        assertEquals(SourceTraceFallbackStatusEnum.INCOMPLETE, sourceTrace.getFallbackStatus());
        assertTrue(sourceTrace.getMissingFields().contains("runtimeKlineContext"));
        assertTrue(sourceTrace.getMissingFields().contains("latestPrice"));
        assertTrue(sourceTrace.getMissingFields().contains("entryPriceSource"));
        assertTrue(sourceTrace.getMissingFields().contains("entrySourceType"));
        assertTrue(sourceTrace.getMissingFields().contains("entrySourceTimeframe"));
        assertTrue(sourceTrace.getMissingFields().contains("entrySourceReason"));
        assertTrue(sourceTrace.getMissingFields().contains("entrySourceRef"));
        assertTrue(sourceTrace.getMissingFields().contains("stopPriceSource"));
        assertTrue(sourceTrace.getMissingFields().contains("stopSourceType"));
        assertTrue(sourceTrace.getMissingFields().contains("stopSourceTimeframe"));
        assertTrue(sourceTrace.getMissingFields().contains("stopSourceReason"));
        assertTrue(sourceTrace.getMissingFields().contains("stopSourceRef"));
        assertTrue(sourceTrace.getMissingFields().contains("tpPriceSources"));
        assertTrue(sourceTrace.getMissingFields().contains("tpSourceType"));
        assertTrue(sourceTrace.getMissingFields().contains("tpSourceTimeframe"));
        assertTrue(sourceTrace.getMissingFields().contains("tpSourceReason"));
        assertTrue(sourceTrace.getMissingFields().contains("tpSourceRef"));
        assertTrue(sourceTrace.getMissingFields().contains("rrSource"));
        assertTrue(sourceTrace.getMissingFields().contains("rrRuleRef"));
        assertTrue(sourceTrace.getMissingFields().contains("liquiditySource"));
        assertTrue(sourceTrace.getMissingFields().contains("eventSource"));
        assertTrue(sourceTrace.getMissingFields().contains("wickSource"));
        assertNull(sourceTrace.getQuoteLatestPrice());
        assertNull(sourceTrace.getEntryPriceSource());
        assertNull(sourceTrace.getStopPriceSource());
        assertTrue(sourceTrace.getTpPriceSources().isEmpty());
        assertNull(sourceTrace.getRrSource());
        assertNull(sourceTrace.getLiquiditySource());
        assertNull(sourceTrace.getEventSource());
        assertNull(sourceTrace.getWickSource());
        assertFalse(sourceTrace.hasRequiredBoundarySources());
        assertTrue(runtimeKlineContext.isManualReviewRequired());
        assertTrue(runtimeKlineContext.isNotTradeInstruction());
        assertTrue(sourceTrace.isManualReviewRequired());
        assertTrue(sourceTrace.isNotTradeInstruction());
    }

    @Test
    void shouldExposeNonFreshReadinessAsFailClosedMetadata() {
        DefaultDashboardSourceTraceDetailAdapter adapterWithReadiness =
                new DefaultDashboardSourceTraceDetailAdapter(
                        new DefaultDashboardRuntimeKlineContextAdapter(readinessService(
                                readiness(
                                        PersistedOhlcvReadinessStatus.PARTIAL,
                                        PersistedOhlcvStaleReasonCode.WINDOW_TOO_SHORT,
                                        "Persisted OHLCV window is shorter than required.",
                                        List.of("persistedOhlcvWindow", "requiredClosedBars")
                                )
                        ))
                );
        DecisionResultVO decision = new DecisionResultVO();
        decision.setSymbol("BTCUSDT");
        decision.setTimeframe("1m");

        SourceTraceDTO sourceTrace = adapterWithReadiness.build("BTCUSDT", decision).getSourceTrace();

        assertEquals("PARTIAL", sourceTrace.getRuntimeKlineReadinessStatus());
        assertEquals("WINDOW_TOO_SHORT", sourceTrace.getRuntimeKlineStaleReasonCode());
        assertEquals(List.of("persistedOhlcvWindow", "requiredClosedBars"),
                sourceTrace.getRuntimeKlineReadinessMissingFields());
        assertEquals(SourceTraceFallbackStatusEnum.INCOMPLETE, sourceTrace.getFallbackStatus());
        assertTrue(sourceTrace.getMissingFields().contains("runtimeKlineContext"));
        assertFalse(sourceTrace.hasRequiredBoundarySources());
        assertTrue(sourceTrace.isManualReviewRequired());
        assertTrue(sourceTrace.isNotTradeInstruction());
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
