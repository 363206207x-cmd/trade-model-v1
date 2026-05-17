package org.example.trademodel.service.dashboard;

import org.example.trademodel.dto.planboundary.DerivativesRiskContextDTO;
import org.example.trademodel.dto.planboundary.SourceTraceDTO;
import org.example.trademodel.dto.planboundary.SourceTraceFallbackStatusEnum;
import org.example.trademodel.vo.DecisionResultVO;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * BACKEND-P2 read-only dashboard detail adapter.
 * It exposes explicit missing SourceTrace / derivatives-risk context instead of fabricating values.
 */
@Component
public class DefaultDashboardSourceTraceDetailAdapter implements DashboardSourceTraceDetailAdapter {
    private static final String TIMEFRAME = "timeframe";

    @Override
    public DashboardSourceTraceDetailContext build(String symbol, DecisionResultVO decision) {
        SourceTraceDTO sourceTrace = buildSourceTrace(symbol, decision);
        DerivativesRiskContextDTO derivativesRiskContext = buildDerivativesRiskContext(symbol, decision);
        return new DashboardSourceTraceDetailContext(sourceTrace, derivativesRiskContext);
    }

    private SourceTraceDTO buildSourceTrace(String symbol, DecisionResultVO decision) {
        SourceTraceDTO sourceTrace = new SourceTraceDTO();
        sourceTrace.setSymbol(symbol);
        sourceTrace.setFallbackStatus(SourceTraceFallbackStatusEnum.INCOMPLETE);
        sourceTrace.setMissingFields(sourceTraceMissingFields(decision));
        sourceTrace.setManualReviewRequired(true);
        sourceTrace.setNotTradeInstruction(true);
        return sourceTrace;
    }

    private DerivativesRiskContextDTO buildDerivativesRiskContext(String symbol, DecisionResultVO decision) {
        DerivativesRiskContextDTO context = new DerivativesRiskContextDTO();
        context.setSymbol(symbol);
        context.setFallbackStatus(SourceTraceFallbackStatusEnum.SAFE_FAIL_CLOSED_ONLY);
        context.setMissingFields(derivativesRiskMissingFields(decision));
        context.setManualReviewRequired(true);
        context.setNotTradeInstruction(true);
        return context;
    }

    private List<String> sourceTraceMissingFields(DecisionResultVO decision) {
        List<String> fields = new ArrayList<>();
        if (decision == null) {
            fields.add("decision");
        }
        fields.add("runtimeKlineContext");
        fields.add(TIMEFRAME);
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
        fields.add("multiTimeframeSource");
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
        fields.add(TIMEFRAME);
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
        fields.add("dataQualityScore");
        return fields;
    }
}
