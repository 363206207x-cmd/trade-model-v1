package org.example.trademodel.service.dashboard;

import org.example.trademodel.dto.planboundary.DerivativesRiskContextDTO;
import org.example.trademodel.dto.planboundary.SourceTraceDTO;
import org.example.trademodel.dto.planboundary.SourceTraceFallbackStatusEnum;
import org.example.trademodel.vo.DecisionResultVO;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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
        assertTrue(context.getSourceTrace().isManualReviewRequired());
        assertTrue(context.getSourceTrace().isNotTradeInstruction());
        assertTrue(context.getDerivativesRiskContext().isManualReviewRequired());
        assertTrue(context.getDerivativesRiskContext().isNotTradeInstruction());
    }
}
