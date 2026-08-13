package org.example.trademodel.providercall.snapshot;

import org.example.trademodel.providercall.ProviderSnapshotMetadata;

import java.math.BigDecimal;
import java.time.Instant;

public record MarketPriceSnapshot(
        String symbol,
        BigDecimal lastPrice,
        BigDecimal bidPrice,
        BigDecimal bidQuantity,
        BigDecimal askPrice,
        BigDecimal askQuantity,
        BigDecimal spread,
        BigDecimal highPrice24h,
        BigDecimal lowPrice24h,
        BigDecimal priceChangePercent24h,
        String sourceProvider,
        Instant sourceFetchedAt,
        ProviderSnapshotMetadata metadata
) {
}
