package org.example.trademodel.providercall.coinglass;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

public record CoinGlassLongShortSnapshot(
        String symbol,
        BigDecimal longShortRatio,
        String longShortRatioSource,
        Instant providerDataTime,
        Map<String, String> fieldSources
) {
    public CoinGlassLongShortSnapshot {
        fieldSources = fieldSources == null ? Map.of() : Map.copyOf(fieldSources);
    }
}
