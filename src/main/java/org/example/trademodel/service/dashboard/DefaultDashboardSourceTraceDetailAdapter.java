package org.example.trademodel.service.dashboard;

import org.example.trademodel.dto.planboundary.DerivativesRiskContextDTO;
import org.example.trademodel.dto.planboundary.SourceTraceDTO;
import org.example.trademodel.dto.planboundary.SourceTraceFallbackStatusEnum;
import org.example.trademodel.vo.DecisionResultVO;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * BACKEND-P2/BACKEND-P3 read-only dashboard detail adapter.
 * It exposes explicit missing SourceTrace / derivatives-risk context instead of fabricating values.
 */
@Component
public class DefaultDashboardSourceTraceDetailAdapter implements DashboardSourceTraceDetailAdapter {
    private static final String TIMEFRAME = "timeframe";
    private static final String DASHBOARD_REQUEST_SYMBOL_SOURCE = "dashboardDetail.requestSymbol";
    private static final String RUNTIME_KLINE_UNAVAILABLE = "UNAVAILABLE";
    private static final String RUNTIME_KLINE_UNAVAILABLE_SOURCE = "dashboardDetail.noRuntimeKlineContext";
    private static final String DECISION_ID_SOURCE = "DecisionResultVO.decisionId";
    private static final String DECISION_ANALYSIS_ID_SOURCE = "DecisionResultVO.analysisId";
    private static final String DECISION_SYMBOL_SOURCE = "DecisionResultVO.symbol";
    private static final String DECISION_CREATE_TIME_SOURCE = "DecisionResultVO.createTime";
    private static final String DECISION_TIMEFRAME_SOURCE = "DecisionResultVO.timeframe";
    private static final String DECISION_LATEST_PRICE_SOURCE = "DecisionResultVO.latestPrice";
    private static final String DECISION_PRICE_UPDATE_TIME_SOURCE = "DecisionResultVO.priceUpdateTimeMs";
    private static final String QUOTE_UPDATE_TIME_ONLY = "QUOTE_UPDATE_TIME_ONLY";
    private static final String DECISION_DATA_QUALITY_SOURCE = "DecisionResultVO.dataQualityScore";
    private static final String DECISION_MULTI_TIMEFRAME_SOURCE = "DecisionResultVO.multiTfConvergence";

    @Override
    public DashboardSourceTraceDetailContext build(String symbol, DecisionResultVO decision) {
        SourceTraceDTO sourceTrace = buildSourceTrace(symbol, decision);
        DerivativesRiskContextDTO derivativesRiskContext = buildDerivativesRiskContext(symbol, decision);
        return new DashboardSourceTraceDetailContext(sourceTrace, derivativesRiskContext);
    }

    private SourceTraceDTO buildSourceTrace(String symbol, DecisionResultVO decision) {
        SourceTraceDTO sourceTrace = new SourceTraceDTO();
        sourceTrace.setSymbol(symbol);
        if (hasText(symbol)) {
            sourceTrace.setSymbolSource(DASHBOARD_REQUEST_SYMBOL_SOURCE);
        }
        wireProductionBackedSourceTraceFields(decision, sourceTrace);
        sourceTrace.setFallbackStatus(SourceTraceFallbackStatusEnum.INCOMPLETE);
        sourceTrace.setMissingFields(sourceTraceMissingFields(decision));
        sourceTrace.setManualReviewRequired(true);
        sourceTrace.setNotTradeInstruction(true);
        return sourceTrace;
    }

    private DerivativesRiskContextDTO buildDerivativesRiskContext(String symbol, DecisionResultVO decision) {
        DerivativesRiskContextDTO context = new DerivativesRiskContextDTO();
        context.setSymbol(symbol);
        wireProductionBackedDerivativesFields(decision, context);
        context.setFallbackStatus(SourceTraceFallbackStatusEnum.SAFE_FAIL_CLOSED_ONLY);
        context.setMissingFields(derivativesRiskMissingFields(decision));
        context.setManualReviewRequired(true);
        context.setNotTradeInstruction(true);
        return context;
    }

    private void wireProductionBackedSourceTraceFields(DecisionResultVO decision, SourceTraceDTO sourceTrace) {
        if (decision == null || sourceTrace == null) {
            return;
        }
        wireAnalysisAnchorFields(decision, sourceTrace);
        sourceTrace.setRuntimeKlineContextStatus(RUNTIME_KLINE_UNAVAILABLE);
        sourceTrace.setRuntimeKlineContextSource(RUNTIME_KLINE_UNAVAILABLE_SOURCE);
        if (hasText(decision.getTimeframe())) {
            sourceTrace.setTimeframe(decision.getTimeframe());
            sourceTrace.setTimeframeSource(DECISION_TIMEFRAME_SOURCE);
        }
        if (decision.getLatestPrice() != null) {
            sourceTrace.setQuoteLatestPrice(decision.getLatestPrice());
            sourceTrace.setQuoteLatestPriceSource(DECISION_LATEST_PRICE_SOURCE);
        }
        if (decision.getPriceUpdateTimeMs() != null) {
            sourceTrace.setQuotePriceUpdateTimeMs(decision.getPriceUpdateTimeMs());
            sourceTrace.setQuotePriceUpdateTimeSource(DECISION_PRICE_UPDATE_TIME_SOURCE);
            sourceTrace.setQuoteFreshnessStatus(QUOTE_UPDATE_TIME_ONLY);
        }
        if (decision.getDataQualityScore() != null) {
            sourceTrace.setDataQualityScore(BigDecimal.valueOf(decision.getDataQualityScore()));
            sourceTrace.setDataQualityScoreSource(DECISION_DATA_QUALITY_SOURCE);
        }
        if (hasText(decision.getMultiTfConvergence())) {
            sourceTrace.setMultiTimeframeSource(DECISION_MULTI_TIMEFRAME_SOURCE);
        }
    }

    private void wireAnalysisAnchorFields(DecisionResultVO decision, SourceTraceDTO sourceTrace) {
        if (hasText(decision.getDecisionId())) {
            sourceTrace.setDecisionId(decision.getDecisionId());
            sourceTrace.setDecisionIdSource(DECISION_ID_SOURCE);
        }
        if (hasText(decision.getAnalysisId())) {
            sourceTrace.setAnalysisId(decision.getAnalysisId());
            sourceTrace.setAnalysisIdSource(DECISION_ANALYSIS_ID_SOURCE);
        }
        if (hasText(decision.getSymbol())) {
            sourceTrace.setSymbol(decision.getSymbol());
            sourceTrace.setSymbolSource(DECISION_SYMBOL_SOURCE);
        }
        if (decision.getCreateTime() != null) {
            sourceTrace.setDecisionCreateTime(decision.getCreateTime());
            sourceTrace.setDecisionCreateTimeSource(DECISION_CREATE_TIME_SOURCE);
        }
    }

    private void wireProductionBackedDerivativesFields(
            DecisionResultVO decision,
            DerivativesRiskContextDTO context
    ) {
        if (decision == null || context == null) {
            return;
        }
        if (hasText(decision.getTimeframe())) {
            context.setTimeframe(decision.getTimeframe());
            context.setTimeframeSource(DECISION_TIMEFRAME_SOURCE);
        }
        if (decision.getDataQualityScore() != null) {
            context.setDataQualityScore(BigDecimal.valueOf(decision.getDataQualityScore()));
            context.setDataQualityScoreSource(DECISION_DATA_QUALITY_SOURCE);
        }
    }

    private List<String> sourceTraceMissingFields(DecisionResultVO decision) {
        List<String> fields = new ArrayList<>();
        if (decision == null) {
            fields.add("decision");
        }
        fields.add("runtimeKlineContext");
        if (decision == null || !hasText(decision.getTimeframe())) {
            fields.add(TIMEFRAME);
        }
        fields.add("latestPrice");
        fields.add("dataQualityScore");
        fields.add("entryPriceSource");
        fields.add("entrySourceType");
        fields.add("entrySourceTimeframe");
        fields.add("entrySourceReason");
        fields.add("entrySourceRef");
        fields.add("stopPriceSource");
        fields.add("stopSourceType");
        fields.add("stopSourceTimeframe");
        fields.add("stopSourceReason");
        fields.add("stopSourceRef");
        fields.add("tpPriceSources");
        fields.add("tpSourceType");
        fields.add("tpSourceTimeframe");
        fields.add("tpSourceReason");
        fields.add("tpSourceRef");
        fields.add("rrSource");
        fields.add("rrRuleRef");
        fields.add("liquiditySource");
        if (decision == null || !hasText(decision.getMultiTfConvergence())) {
            fields.add("multiTimeframeSource");
        }
        fields.add("eventSource");
        fields.add("wickSource");
        fields.add("derivativesRiskContext");
        return fields;
    }

    private List<String> derivativesRiskMissingFields(DecisionResultVO decision) {
        List<String> fields = new ArrayList<>();
        if (decision == null) {
            fields.add("decision");
        }
        if (decision == null || !hasText(decision.getTimeframe())) {
            fields.add(TIMEFRAME);
        }
        fields.add("contextTime");
        fields.add("openInterestHistory");
        fields.add("fundingHistory");
        fields.add("liquidationCluster");
        fields.add("leverageDistribution");
        fields.add("longShortRatio");
        fields.add("liquidityStress");
        fields.add("liquidityStressReason");
        fields.add("eventWindowBlockers");
        fields.add("wickConfirmationSources");
        if (decision == null || decision.getDataQualityScore() == null) {
            fields.add("dataQualityScore");
        }
        return fields;
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
