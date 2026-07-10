package org.example.trademodel.providercall.coinglass;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

public record CoinGlassOpenInterestSnapshot(
        String symbol,
        BigDecimal openInterestUsd,
        BigDecimal openInterestChange1m,
        BigDecimal openInterestChange5m,
        BigDecimal openInterestChange15m,
        BigDecimal openInterestChange1h,
        BigDecimal exchangeConcentrationScore,
        List<String> exchangeCoverage,
        Instant providerDataTime,
        Map<String, String> fieldSources
) {
    public CoinGlassOpenInterestSnapshot {
        exchangeCoverage = exchangeCoverage == null ? List.of() : List.copyOf(exchangeCoverage);
        fieldSources = fieldSources == null ? Map.of() : Map.copyOf(fieldSources);
    }
}
