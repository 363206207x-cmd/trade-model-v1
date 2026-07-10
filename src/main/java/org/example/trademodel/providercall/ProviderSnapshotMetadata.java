package org.example.trademodel.providercall;

import java.time.Instant;
import java.util.List;

public record ProviderSnapshotMetadata(
        String provider,
        ProviderDatasetType datasetType,
        String symbol,
        String timeframe,
        Instant providerDataTime,
        Instant fetchTime,
        Instant expiresAt,
        UnifiedSourceStatus sourceStatus,
        SnapshotFreshnessStatus freshnessStatus,
        String traceId,
        String requestKey,
        boolean cacheHit,
        boolean fallbackUsed,
        String errorCode,
        List<String> reasonCodes
) {
    public ProviderSnapshotMetadata {
        reasonCodes = reasonCodes == null ? List.of() : List.copyOf(reasonCodes);
    }

    public ProviderSnapshotMetadata asCacheHit(SnapshotFreshnessStatus freshness, boolean fallback) {
        return asCacheHit(freshness, fallback, expiresAt);
    }

    public ProviderSnapshotMetadata asCacheHit(
            SnapshotFreshnessStatus freshness,
            boolean fallback,
            Instant requestedExpiresAt) {
        return new ProviderSnapshotMetadata(provider, datasetType, symbol, timeframe, providerDataTime,
                fetchTime, requestedExpiresAt, fallback ? UnifiedSourceStatus.STALE : sourceStatus, freshness,
                traceId, requestKey, true, fallback, errorCode, reasonCodes);
    }
}
