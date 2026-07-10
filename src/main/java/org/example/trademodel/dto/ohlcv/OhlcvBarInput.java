package org.example.trademodel.dto.ohlcv;

import java.math.BigDecimal;

public record OhlcvBarInput(
        String symbol,
        String timeframe,
        long openTimeMs,
        long closeTimeMs,
        BigDecimal open,
        BigDecimal high,
        BigDecimal low,
        BigDecimal close,
        BigDecimal volume,
        BigDecimal quoteVolume,
        Long tradeCount,
        BigDecimal takerBuyBaseVolume,
        BigDecimal takerBuyQuoteVolume,
        boolean closed
) {
}
