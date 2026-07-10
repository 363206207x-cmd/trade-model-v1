package org.example.trademodel.providercall.snapshot;

import org.example.trademodel.providercall.ProviderSnapshotMetadata;

import java.math.BigDecimal;
import java.time.Instant;

public record MarketPriceSnapshot(
        String symbol,
        BigDecimal lastPrice,
        BigDecimal bidPrice,
        BigDecimal askPrice,
        BigDecimal spread,
        BigDecimal highPrice24h,
        BigDecimal lowPrice24h,
        BigDecimal priceChangePercent24h,
        String sourceProvider,
        Instant sourceFetchedAt,
        ProviderSnapshotMetadata metadata
) {
}
