package org.example.trademodel.providercall.snapshot;

import org.example.trademodel.providercall.ProviderSnapshotMetadata;

import java.math.BigDecimal;
import java.util.List;

public record DerivativesRiskSnapshot(
        String symbol,
        String provider,
        BigDecimal openInterestUsd,
        BigDecimal openInterestChange1m,
        BigDecimal openInterestChange5m,
        BigDecimal openInterestChange15m,
        BigDecimal openInterestChange1h,
        BigDecimal weightedFundingRate,
        BigDecimal fundingExtremityScore,
        BigDecimal longShortRatio,
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
        String evidenceAvailability,
        List<String> reasonCodes,
        ProviderSnapshotMetadata metadata
) {
    public DerivativesRiskSnapshot {
        reasonCodes = reasonCodes == null ? List.of() : List.copyOf(reasonCodes);
    }
}
