package org.example.trademodel.stress.replay;

import java.math.BigDecimal;
import java.time.Instant;

record HistoricalCandle(Instant timestampUtc,
                        String symbol,
                        String timeframe,
                        BigDecimal open,
                        BigDecimal high,
                        BigDecimal low,
                        BigDecimal close,
                        BigDecimal volume) {

    SeriesKey seriesKey() {
        return new SeriesKey(symbol, timeframe);
    }

    record SeriesKey(String symbol, String timeframe) {
    }
}
