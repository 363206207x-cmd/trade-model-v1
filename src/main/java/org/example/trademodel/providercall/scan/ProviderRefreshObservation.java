package org.example.trademodel.providercall.scan;

import org.example.trademodel.providercall.ProviderDatasetType;
import org.example.trademodel.providercall.SnapshotFreshnessStatus;
import org.example.trademodel.providercall.UnifiedSourceStatus;
import org.example.trademodel.providercall.instrument.CanonicalInstrumentId;

import java.time.Instant;

public record ProviderRefreshObservation(
        CanonicalInstrumentId canonicalInstrumentId,
        String providerSymbol,
        ProviderDatasetType datasetType,
        UnifiedSourceStatus sourceStatus,
        SnapshotFreshnessStatus freshnessStatus,
        String reasonCode,
        Instant attemptedAt,
        Instant providerDataTime,
        String traceId,
        String timeframe
) {
    public ProviderRefreshObservation(
            CanonicalInstrumentId canonicalInstrumentId,
            String providerSymbol,
            ProviderDatasetType datasetType,
            UnifiedSourceStatus sourceStatus,
            SnapshotFreshnessStatus freshnessStatus,
            String reasonCode,
            Instant attemptedAt,
            Instant providerDataTime,
            String traceId) {
        this(canonicalInstrumentId, providerSymbol, datasetType, sourceStatus, freshnessStatus,
                reasonCode, attemptedAt, providerDataTime, traceId, "GLOBAL");
    }

    public ProviderRefreshObservation {
        timeframe = timeframe == null || timeframe.isBlank() ? "GLOBAL" : timeframe;
    }

    public String symbol() {
        return providerSymbol;
    }
}
