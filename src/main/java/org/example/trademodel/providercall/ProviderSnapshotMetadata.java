package org.example.trademodel.providercall;

import org.example.trademodel.providercall.instrument.CanonicalInstrumentId;
import org.example.trademodel.providercall.instrument.ContractType;
import org.example.trademodel.providercall.instrument.MarketType;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

public record ProviderSnapshotMetadata(
        String provider,
        ProviderDatasetType datasetType,
        CanonicalInstrumentId canonicalInstrumentId,
        String providerSymbol,
        String timeframe,
        Instant providerDataTime,
        Instant fetchTime,
        Instant expiresAt,
        long snapshotAgeSeconds,
        UnifiedSourceStatus sourceStatus,
        SnapshotFreshnessStatus freshnessStatus,
        String traceId,
        String requestKey,
        String sourceVersion,
        boolean cacheHit,
        boolean fallbackUsed,
        String errorCode,
        List<String> reasonCodes
) {
    public ProviderSnapshotMetadata {
        snapshotAgeSeconds = Math.max(0, snapshotAgeSeconds);
        reasonCodes = reasonCodes == null ? List.of() : List.copyOf(reasonCodes);
    }

    public ProviderSnapshotMetadata(
            String provider,
            ProviderDatasetType datasetType,
            String providerSymbol,
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
            List<String> reasonCodes) {
        this(provider, datasetType,
                new CanonicalInstrumentId("UNKNOWN", "UNKNOWN", MarketType.SPOT, "UNVERIFIED", ContractType.NONE),
                providerSymbol, timeframe, providerDataTime, fetchTime, expiresAt, 0L, sourceStatus,
                freshnessStatus, traceId, requestKey, "UNVERIFIED", cacheHit, fallbackUsed, errorCode, reasonCodes);
    }

    public String symbol() {
        return providerSymbol;
    }

    public ProviderSnapshotMetadata asCacheHit(SnapshotFreshnessStatus freshness, boolean fallback) {
        return asCacheHit(freshness, fallback, expiresAt, fetchTime);
    }

    public ProviderSnapshotMetadata asCacheHit(
            SnapshotFreshnessStatus freshness,
            boolean fallback,
            Instant requestedExpiresAt,
            Instant asOf) {
        Instant ageBasis = providerDataTime == null ? fetchTime : providerDataTime;
        long ageSeconds = ageBasis == null || asOf == null || asOf.isBefore(ageBasis)
                ? 0L : Duration.between(ageBasis, asOf).toSeconds();
        return new ProviderSnapshotMetadata(provider, datasetType, canonicalInstrumentId, providerSymbol,
                timeframe, providerDataTime, fetchTime, requestedExpiresAt, ageSeconds,
                fallback ? UnifiedSourceStatus.STALE : sourceStatus, freshness, traceId, requestKey,
                sourceVersion, true, fallback, errorCode, reasonCodes);
    }
}
