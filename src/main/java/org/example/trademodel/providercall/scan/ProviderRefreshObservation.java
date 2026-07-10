package org.example.trademodel.providercall.scan;

import org.example.trademodel.providercall.ProviderDatasetType;
import org.example.trademodel.providercall.SnapshotFreshnessStatus;
import org.example.trademodel.providercall.UnifiedSourceStatus;

import java.time.Instant;

public record ProviderRefreshObservation(
        String symbol,
        ProviderDatasetType datasetType,
        UnifiedSourceStatus sourceStatus,
        SnapshotFreshnessStatus freshnessStatus,
        String reasonCode,
        Instant attemptedAt,
        Instant providerDataTime,
        String traceId
) {
}
