package org.example.trademodel.providercall.snapshot;

import org.example.trademodel.providercall.ProviderSnapshotMetadata;
import org.example.trademodel.providercall.SnapshotFreshnessStatus;
import org.example.trademodel.providercall.UnifiedSourceStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

public record DerivativesRiskSnapshot(
        String symbol,
        String provider,
        Instant providerDataTime,
        Instant fetchTime,
        Instant expiresAt,
        BigDecimal openInterestUsd,
        BigDecimal openInterestChange1m,
        BigDecimal openInterestChange5m,
        BigDecimal openInterestChange15m,
        BigDecimal openInterestChange1h,
        BigDecimal weightedFundingRate,
        BigDecimal fundingExtremityScore,
        BigDecimal longShortRatio,
        String longShortRatioSource,
        BigDecimal longLiquidationUsd1m,
        BigDecimal longLiquidationUsd5m,
        BigDecimal longLiquidationUsd15m,
        BigDecimal longLiquidationUsd1h,
        BigDecimal shortLiquidationUsd1m,
        BigDecimal shortLiquidationUsd5m,
        BigDecimal shortLiquidationUsd15m,
        BigDecimal shortLiquidationUsd1h,
        BigDecimal liquidationSpikeScore,
        BigDecimal exchangeConcentrationScore,
        List<String> availableDatasets,
        List<String> missingDatasets,
        List<String> degradedDatasets,
        UnifiedSourceStatus sourceStatus,
        SnapshotFreshnessStatus freshnessStatus,
        String evidenceAvailability,
        List<String> reasonCodes,
        String traceId,
        Map<String, String> fieldSources,
        ProviderSnapshotMetadata metadata
) {
    public DerivativesRiskSnapshot {
        availableDatasets = availableDatasets == null ? List.of() : List.copyOf(availableDatasets);
        missingDatasets = missingDatasets == null ? List.of() : List.copyOf(missingDatasets);
        degradedDatasets = degradedDatasets == null ? List.of() : List.copyOf(degradedDatasets);
        reasonCodes = reasonCodes == null ? List.of() : List.copyOf(reasonCodes);
        fieldSources = fieldSources == null ? Map.of() : Map.copyOf(fieldSources);
    }
}
