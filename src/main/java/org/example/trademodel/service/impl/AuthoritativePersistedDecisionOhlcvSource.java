package org.example.trademodel.service.impl;

import org.example.trademodel.dto.ohlcv.PersistedOhlcvReadinessResult;
import org.example.trademodel.dto.planboundary.RuntimeKlineContextDTO;
import org.example.trademodel.dto.planboundary.RuntimeKlineItemDTO;
import org.example.trademodel.config.FundamentalAiV41Properties;
import org.example.trademodel.service.DecisionOhlcvSnapshotSource;
import org.example.trademodel.service.PersistedOhlcvQueryService;
import org.example.trademodel.service.RuntimeKlineContextAssemblyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;

@Service
public class AuthoritativePersistedDecisionOhlcvSource implements DecisionOhlcvSnapshotSource {
    private final PersistedOhlcvQueryService queryService;
    private final RuntimeKlineContextAssemblyService assemblyService;
    private final FundamentalAiV41Properties properties;

    public AuthoritativePersistedDecisionOhlcvSource(PersistedOhlcvQueryService queryService,
                                                      RuntimeKlineContextAssemblyService assemblyService) {
        this(queryService, assemblyService, FundamentalAiV41Properties.contractFixture());
    }

    @Autowired
    public AuthoritativePersistedDecisionOhlcvSource(PersistedOhlcvQueryService queryService,
                                                      RuntimeKlineContextAssemblyService assemblyService,
                                                      FundamentalAiV41Properties properties) {
        this.queryService = queryService;
        this.assemblyService = assemblyService;
        this.properties = properties;
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
        return context.getKlineItems().stream()
                .sorted(Comparator.comparing(
                        RuntimeKlineItemDTO::getOpenTimeMs,
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .map(AuthoritativePersistedDecisionOhlcvSource::legacyBar)
                .toList();
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

    private long maxReadLagMs(String timeframe) {
        return switch (timeframe) {
            case "5m" -> secondsToMillis(properties.getProviderMatrix().getFiveMinuteTtlSeconds());
            case "15m" -> secondsToMillis(properties.getProviderMatrix().getFifteenMinuteTtlSeconds());
            case "1h" -> secondsToMillis(properties.getProviderMatrix().getOneHourTtlSeconds());
            case "4h" -> secondsToMillis(properties.getProviderMatrix().getFourHourTtlSeconds());
            default -> throw new IllegalArgumentException("unsupported decision timeframe: " + timeframe);
        };
    }

    private static long secondsToMillis(Integer seconds) {
        if (seconds == null || seconds <= 0) {
            throw new IllegalStateException("provider-matrix TTL must be configured above zero");
        }
        return Math.multiplyExact(seconds.longValue(), 1_000L);
    }
}
