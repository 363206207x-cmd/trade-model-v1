package org.example.trademodel.providercall.coinglass;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

public record CoinGlassLiquidationSnapshot(
        String symbol,
        BigDecimal longLiquidationUsd1m,
        BigDecimal longLiquidationUsd5m,
        BigDecimal longLiquidationUsd15m,
        BigDecimal longLiquidationUsd1h,
        BigDecimal shortLiquidationUsd1m,
        BigDecimal shortLiquidationUsd5m,
        BigDecimal shortLiquidationUsd15m,
        BigDecimal shortLiquidationUsd1h,
        Instant providerDataTime,
        Map<String, String> fieldSources
) {
    public CoinGlassLiquidationSnapshot {
        fieldSources = fieldSources == null ? Map.of() : Map.copyOf(fieldSources);
    }
}
