package org.example.trademodel.service.dashboard;

import org.example.trademodel.dto.planboundary.RuntimeKlineContextDTO;
import org.example.trademodel.dto.planboundary.SourceTraceFallbackStatusEnum;
import org.example.trademodel.vo.DecisionResultVO;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Explicit dashboard RuntimeKline boundary for BACKEND-P8.
 * It describes unavailable runtime kline context without fabricating OHLCV or boundary sources.
 */
@Component
public class DefaultDashboardRuntimeKlineContextAdapter implements DashboardRuntimeKlineContextAdapter {

    @Override
    public RuntimeKlineContextDTO buildUnavailableContext(String symbol, DecisionResultVO decision) {
        RuntimeKlineContextDTO context = new RuntimeKlineContextDTO();
        String decisionSymbol = decision == null ? null : decision.getSymbol();
        String decisionTimeframe = decision == null ? null : decision.getTimeframe();

        context.setSymbol(hasText(decisionSymbol) ? decisionSymbol : symbol);
        if (hasText(decisionTimeframe)) {
            context.setTimeframe(decisionTimeframe);
        }
        context.setFallbackStatus(SourceTraceFallbackStatusEnum.INCOMPLETE);
        context.setMissingFields(missingFields(context.getSymbol(), context.getTimeframe(), decision));
        context.setManualReviewRequired(true);
        context.setNotTradeInstruction(true);
        return context;
    }

    private List<String> missingFields(String symbol, String timeframe, DecisionResultVO decision) {
        List<String> fields = new ArrayList<>();
        if (decision == null) {
            fields.add("decision");
        }
        if (!hasText(symbol)) {
            fields.add("symbol");
        }
        if (!hasText(timeframe)) {
            fields.add("timeframe");
        }
        fields.add("persistedOhlcvWindow");
        fields.add("klineItems");
        fields.add("klineWindow");
        fields.add("klineFreshness");
        fields.add("staleStatus");
        fields.add("runtimeLatestPriceSource");
        fields.add("dataQualityScoreSource");
        fields.add("entryPriceSource");
        fields.add("stopPriceSource");
        fields.add("tpPriceSources");
        fields.add("rrSource");
        fields.add("liquiditySource");
        fields.add("multiTimeframeSource");
        fields.add("eventSource");
        fields.add("wickSource");
        return fields;
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
