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
        String timeframe,
        String provider,
        String providerMarketType,
        String sourceVersion
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
                reasonCode, attemptedAt, providerDataTime, traceId, "GLOBAL", null, null, null);
    }

    public ProviderRefreshObservation(
            CanonicalInstrumentId canonicalInstrumentId,
            String providerSymbol,
            ProviderDatasetType datasetType,
            UnifiedSourceStatus sourceStatus,
            SnapshotFreshnessStatus freshnessStatus,
            String reasonCode,
            Instant attemptedAt,
            Instant providerDataTime,
            String traceId,
            String timeframe) {
        this(canonicalInstrumentId, providerSymbol, datasetType, sourceStatus, freshnessStatus,
                reasonCode, attemptedAt, providerDataTime, traceId, timeframe, null, null, null);
    }

    public ProviderRefreshObservation {
        timeframe = timeframe == null || timeframe.isBlank() ? "GLOBAL" : timeframe;
        provider = provider == null || provider.isBlank()
                ? canonicalInstrumentId == null ? "UNVERIFIED" : canonicalInstrumentId.venue()
                : provider;
        providerMarketType = providerMarketType == null || providerMarketType.isBlank()
                ? defaultProviderMarketType(canonicalInstrumentId) : providerMarketType;
        sourceVersion = sourceVersion == null || sourceVersion.isBlank() ? "UNVERIFIED" : sourceVersion;
    }

    public String symbol() {
        return providerSymbol;
    }

    private static String defaultProviderMarketType(CanonicalInstrumentId instrument) {
        if (instrument == null || instrument.marketType() == null) return "UNVERIFIED";
        return switch (instrument.marketType()) {
            case SPOT -> "SPOT";
            case PERPETUAL -> "USDT_PERP";
        };
    }
}
