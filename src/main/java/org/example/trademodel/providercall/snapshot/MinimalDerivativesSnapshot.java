package org.example.trademodel.providercall.snapshot;

import org.example.trademodel.providercall.ProviderSnapshotMetadata;

import java.math.BigDecimal;

public record MinimalDerivativesSnapshot(
        String symbol,
        BigDecimal lastFundingRate,
        BigDecimal openInterest,
        String evidenceAvailability,
        ProviderSnapshotMetadata metadata
) {
}
