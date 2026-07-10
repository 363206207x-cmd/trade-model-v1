package org.example.trademodel.service.impl;

import org.example.trademodel.dto.ohlcv.PersistedOhlcvReadinessResult;
import org.example.trademodel.dto.planboundary.RuntimeKlineContextDTO;
import org.example.trademodel.dto.planboundary.RuntimeKlineItemDTO;
import org.example.trademodel.service.DecisionOhlcvSnapshotSource;
import org.example.trademodel.service.PersistedOhlcvQueryService;
import org.example.trademodel.service.RuntimeKlineContextAssemblyService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AuthoritativePersistedDecisionOhlcvSource implements DecisionOhlcvSnapshotSource {
    private final PersistedOhlcvQueryService queryService;
    private final RuntimeKlineContextAssemblyService assemblyService;

    public AuthoritativePersistedDecisionOhlcvSource(PersistedOhlcvQueryService queryService,
                                                      RuntimeKlineContextAssemblyService assemblyService) {
        this.queryService = queryService;
        this.assemblyService = assemblyService;
    }

    @Override
    public List<String[]> readClosedBars(String symbol, String timeframe, int limit, String traceId) {
        long maxLagMs = maxReadLagMs(timeframe);
        PersistedOhlcvReadinessResult readiness = queryService.evaluateReadiness(symbol, timeframe, limit, maxLagMs);
        RuntimeKlineContextDTO context = assemblyService.assemble(readiness);
        if (context == null || context.getFallbackStatus() != null || !context.getMissingFields().isEmpty()
                || context.getKlineItems() == null || context.getKlineItems().size() < limit) {
            throw new IllegalStateException("AUTHORITATIVE_OHLCV_UNAVAILABLE:" + timeframe);
        }
        return context.getKlineItems().stream().map(AuthoritativePersistedDecisionOhlcvSource::legacyBar).toList();
    }

    private static String[] legacyBar(RuntimeKlineItemDTO item) {
        if (item == null || item.getOpenPrice() == null || item.getHighPrice() == null
                || item.getLowPrice() == null || item.getClosePrice() == null) {
            throw new IllegalStateException("AUTHORITATIVE_OHLCV_FIELDS_INCOMPLETE");
        }
        return new String[]{String.valueOf(item.getOpenTimeMs()), item.getOpenPrice().toPlainString(),
                item.getHighPrice().toPlainString(), item.getLowPrice().toPlainString(),
                item.getClosePrice().toPlainString(),
                item.getVolume() == null ? "0" : item.getVolume().toPlainString()};
    }

    private static long maxReadLagMs(String timeframe) {
        return switch (timeframe) {
            case "5m" -> 10L * 60_000L;
            case "15m" -> 30L * 60_000L;
            case "1h" -> 2L * 60L * 60_000L;
            case "4h" -> 8L * 60L * 60_000L;
            default -> throw new IllegalArgumentException("unsupported decision timeframe: " + timeframe);
        };
    }
}
