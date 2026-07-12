package org.example.trademodel.market;

import org.example.trademodel.dto.planboundary.RuntimeKlineContextDTO;
import org.example.trademodel.vo.MarketEnvironmentVO;

import java.util.List;
import java.util.Map;

public record PersistedRealMarketEnvironmentAssessment(
        boolean ready,
        String reasonCode,
        String provider,
        String sourceType,
        MarketEnvironmentVO environment,
        Map<String, RuntimeKlineContextDTO> timeframeContexts,
        int closedBarCount,
        Long latestClosedBarTimeMs,
        List<String> sourceTraceRefs
) {
    public static PersistedRealMarketEnvironmentAssessment failed(
            String reasonCode,
            Map<String, RuntimeKlineContextDTO> timeframeContexts) {
        return new PersistedRealMarketEnvironmentAssessment(false, reasonCode, null, null, null,
                timeframeContexts == null ? Map.of() : Map.copyOf(timeframeContexts), 0, null, List.of());
    }
}
