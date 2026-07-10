package org.example.trademodel.providercall.coinglass;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

public record CoinGlassFundingSnapshot(
        String symbol,
        BigDecimal weightedFundingRate,
        Instant providerDataTime,
        Map<String, String> fieldSources
) {
    public CoinGlassFundingSnapshot {
        fieldSources = fieldSources == null ? Map.of() : Map.copyOf(fieldSources);
    }
}
