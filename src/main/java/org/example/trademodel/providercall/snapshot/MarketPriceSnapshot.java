package org.example.trademodel.providercall.snapshot;

import org.example.trademodel.providercall.ProviderSnapshotMetadata;

import java.math.BigDecimal;

public record MarketPriceSnapshot(
        String symbol,
        BigDecimal lastPrice,
        BigDecimal bidPrice,
        BigDecimal askPrice,
        BigDecimal spread,
        ProviderSnapshotMetadata metadata
) {
}
