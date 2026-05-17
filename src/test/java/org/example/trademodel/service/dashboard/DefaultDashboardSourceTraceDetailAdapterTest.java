package org.example.trademodel.service.dashboard;

import org.example.trademodel.dto.planboundary.DerivativesRiskContextDTO;
import org.example.trademodel.dto.planboundary.SourceTraceDTO;
import org.example.trademodel.dto.planboundary.SourceTraceFallbackStatusEnum;
import org.example.trademodel.vo.DecisionResultVO;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

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
        DerivativesRiskContextDTO derivativesRiskContext = context.getDerivativesRiskContext();

        assertNotNull(sourceTrace);
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
}
