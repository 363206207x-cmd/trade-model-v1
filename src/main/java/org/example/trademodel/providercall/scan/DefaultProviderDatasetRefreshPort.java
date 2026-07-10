package org.example.trademodel.providercall.scan;

import org.example.trademodel.dto.ohlcv.OhlcvIngestionResult;
import org.example.trademodel.entity.PersistedOhlcvBarDO;
import org.example.trademodel.mapper.PersistedOhlcvBarMapper;
import org.example.trademodel.providercall.ProviderCallProperties;
import org.example.trademodel.providercall.ProviderCallResult;
import org.example.trademodel.providercall.ProviderDatasetType;
import org.example.trademodel.providercall.SnapshotFreshnessStatus;
import org.example.trademodel.providercall.UnifiedSourceStatus;
import org.example.trademodel.providercall.snapshot.CoordinatedOhlcvSnapshotService;
import org.example.trademodel.providercall.snapshot.MarketPriceSnapshot;
import org.example.trademodel.providercall.snapshot.MarketPriceSnapshotService;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class DefaultProviderDatasetRefreshPort implements ProviderDatasetRefreshPort {
    private static final List<String> PRIMARY_TIMEFRAMES = List.of("5m", "15m", "1h", "4h");
    private final MarketPriceSnapshotService priceService;
    private final CoordinatedOhlcvSnapshotService ohlcvService;
    private final PersistedOhlcvBarMapper ohlcvBarMapper;
    private final ProviderCallProperties properties;
    private final ProviderRefreshStateRegistry registry;

    public DefaultProviderDatasetRefreshPort(MarketPriceSnapshotService priceService,
                                             CoordinatedOhlcvSnapshotService ohlcvService,
                                             PersistedOhlcvBarMapper ohlcvBarMapper,
                                             ProviderCallProperties properties,
                                             ProviderRefreshStateRegistry registry) {
        this.priceService = priceService;
        this.ohlcvService = ohlcvService;
        this.ohlcvBarMapper = ohlcvBarMapper;
        this.properties = properties;
        this.registry = registry;
    }

    @Override
    public void refresh(ScanPlanItem item, ProviderDatasetType datasetType) {
        String traceId = "provider-scan-" + UUID.randomUUID();
        Instant attemptedAt = Instant.now();
        switch (datasetType) {
            case PRICE -> refreshPrice(item, traceId, attemptedAt);
            case OHLCV -> refreshOhlcv(item, traceId, attemptedAt);
            case DERIVATIVES -> unavailable(item, datasetType, UnifiedSourceStatus.NOT_CONFIGURED,
                    "COINGLASS_NOT_CONFIGURED", traceId, attemptedAt);
            case EXTERNAL_CONTEXT -> unavailable(item, datasetType, UnifiedSourceStatus.NOT_CONFIGURED,
                    "EXTERNAL_CONTEXT_PROVIDER_NOT_CONFIGURED", traceId, attemptedAt);
            case AI_REVIEW -> unavailable(item, datasetType, UnifiedSourceStatus.DISABLED,
                    "AI_ROUTINE_SCAN_DISABLED", traceId, attemptedAt);
        }
    }

    private void refreshPrice(ScanPlanItem item, String traceId, Instant attemptedAt) {
        int seconds = properties.intervalSeconds(item.effectiveProfile(), item.effectivePriority(), ProviderDatasetType.PRICE);
        ProviderCallResult<MarketPriceSnapshot> result = priceService.get(item.symbol(), item.effectivePriority(),
                Duration.ofSeconds(Math.max(1, seconds)), traceId);
        record(item.symbol(), ProviderDatasetType.PRICE, result, attemptedAt, traceId);
    }

    private void refreshOhlcv(ScanPlanItem item, String traceId, Instant attemptedAt) {
        ProviderCallResult<OhlcvIngestionResult> last = null;
        boolean due = false;
        Instant latestDataTime = null;
        for (String timeframe : PRIMARY_TIMEFRAMES) {
            OhlcvDueState dueState = dueState(item.symbol(), timeframe, attemptedAt);
            if (dueState.latestCloseTime() != null
                    && (latestDataTime == null || dueState.latestCloseTime().isAfter(latestDataTime))) {
                latestDataTime = dueState.latestCloseTime();
            }
            if (!dueState.due()) continue;
            due = true;
            last = ohlcvService.refresh(item.symbol(), timeframe, 100, item.effectivePriority(), traceId);
            if (last == null || last.metadata() == null || last.metadata().sourceStatus() != UnifiedSourceStatus.READY) {
                break;
            }
        }
        if (!due) {
            registry.record(new ProviderRefreshObservation(item.symbol(), ProviderDatasetType.OHLCV,
                    UnifiedSourceStatus.READY, SnapshotFreshnessStatus.FRESH, "NO_NEW_CLOSED_BAR_DUE",
                    attemptedAt, latestDataTime, traceId));
            return;
        }
        record(item.symbol(), ProviderDatasetType.OHLCV, last, attemptedAt, traceId);
    }

    private OhlcvDueState dueState(String symbol, String timeframe, Instant now) {
        try {
            List<PersistedOhlcvBarDO> rows = ohlcvBarMapper.selectLatestClosedWindow(symbol, timeframe, 1);
            if (rows == null || rows.isEmpty() || rows.get(0).getCloseTimeMs() == null) {
                return new OhlcvDueState(true, null);
            }
            Instant latestClose = Instant.ofEpochMilli(rows.get(0).getCloseTimeMs());
            return new OhlcvDueState(!now.isBefore(latestClose.plusSeconds(timeframeSeconds(timeframe))), latestClose);
        } catch (RuntimeException ignored) {
            // A failed authoritative-read check must not suppress refresh recovery.
            return new OhlcvDueState(true, null);
        }
    }

    private static long timeframeSeconds(String timeframe) {
        return switch (timeframe) {
            case "5m" -> 300L;
            case "15m" -> 900L;
            case "1h" -> 3600L;
            case "4h" -> 14400L;
            default -> throw new IllegalArgumentException("unsupported timeframe: " + timeframe);
        };
    }

    private void unavailable(ScanPlanItem item, ProviderDatasetType datasetType, UnifiedSourceStatus status,
                             String reason, String traceId, Instant attemptedAt) {
        registry.record(new ProviderRefreshObservation(item.symbol(), datasetType, status,
                SnapshotFreshnessStatus.UNAVAILABLE, reason, attemptedAt, null, traceId));
    }

    private void record(String symbol, ProviderDatasetType datasetType, ProviderCallResult<?> result,
                        Instant attemptedAt, String traceId) {
        if (result == null || result.metadata() == null) {
            registry.record(new ProviderRefreshObservation(symbol, datasetType, UnifiedSourceStatus.ERROR,
                    SnapshotFreshnessStatus.ERROR, "PROVIDER_RESULT_MISSING", attemptedAt, null, traceId));
            return;
        }
        registry.record(new ProviderRefreshObservation(symbol, datasetType, result.metadata().sourceStatus(),
                result.metadata().freshnessStatus(), result.metadata().errorCode(), attemptedAt,
                result.metadata().providerDataTime(), result.metadata().traceId()));
    }

    private record OhlcvDueState(boolean due, Instant latestCloseTime) {
    }
}
