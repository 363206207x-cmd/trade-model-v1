package org.example.trademodel.stress.replay;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

final class HistoricalCandleValidator {

    private static final Map<String, Duration> TIMEFRAMES = Map.of(
            "1m", Duration.ofMinutes(1),
            "5m", Duration.ofMinutes(5),
            "15m", Duration.ofMinutes(15),
            "1h", Duration.ofHours(1),
            "4h", Duration.ofHours(4));

    private HistoricalCandleValidator() {
    }

    static HistoricalFixtureValidation validate(List<HistoricalCandle> candles) {
        if (candles == null || candles.isEmpty()) {
            throw new IllegalArgumentException("REAL_HISTORICAL_FIXTURE_EMPTY");
        }
        Set<String> uniqueRows = new HashSet<>();
        Set<String> symbols = new LinkedHashSet<>();
        Set<String> timeframes = new LinkedHashSet<>();
        Map<HistoricalCandle.SeriesKey, List<HistoricalCandle>> grouped = new LinkedHashMap<>();
        Instant previousGlobal = null;

        for (HistoricalCandle candle : candles) {
            validateCandle(candle);
            if (previousGlobal != null && candle.timestampUtc().isBefore(previousGlobal)) {
                throw new IllegalArgumentException("FIXTURE_NOT_CHRONOLOGICAL");
            }
            previousGlobal = candle.timestampUtc();
            String rowKey = candle.symbol() + "|" + candle.timeframe() + "|" + candle.timestampUtc();
            if (!uniqueRows.add(rowKey)) {
                throw new IllegalArgumentException("DUPLICATE_SYMBOL_TIMEFRAME_TIMESTAMP:" + rowKey);
            }
            symbols.add(candle.symbol());
            timeframes.add(candle.timeframe());
            grouped.computeIfAbsent(candle.seriesKey(), ignored -> new ArrayList<>()).add(candle);
        }

        List<String> gaps = new ArrayList<>();
        for (Map.Entry<HistoricalCandle.SeriesKey, List<HistoricalCandle>> entry : grouped.entrySet()) {
            Duration expected = TIMEFRAMES.get(entry.getKey().timeframe());
            List<HistoricalCandle> series = entry.getValue();
            for (int i = 1; i < series.size(); i++) {
                Duration actual = Duration.between(series.get(i - 1).timestampUtc(), series.get(i).timestampUtc());
                if (actual.compareTo(expected) < 0) {
                    throw new IllegalArgumentException("TIMEFRAME_OVERLAP:" + entry.getKey());
                }
                if (!actual.equals(expected)) {
                    gaps.add(entry.getKey() + ":" + series.get(i - 1).timestampUtc() + "->"
                            + series.get(i).timestampUtc() + ":expected=" + expected + ":actual=" + actual);
                }
            }
        }

        return new HistoricalFixtureValidation(candles.size(), candles.get(0).timestampUtc(),
                candles.get(candles.size() - 1).timestampUtc(), Set.copyOf(symbols), Set.copyOf(timeframes),
                List.copyOf(gaps));
    }

    static Duration timeframeDuration(String timeframe) {
        Duration duration = TIMEFRAMES.get(timeframe);
        if (duration == null) {
            throw new IllegalArgumentException("UNSUPPORTED_TIMEFRAME:" + timeframe);
        }
        return duration;
    }

    private static void validateCandle(HistoricalCandle candle) {
        if (candle == null || candle.timestampUtc() == null || blank(candle.symbol()) || blank(candle.timeframe())) {
            throw new IllegalArgumentException("REQUIRED_CANDLE_FIELD_MISSING");
        }
        timeframeDuration(candle.timeframe());
        requirePositive(candle.open(), "open");
        requirePositive(candle.high(), "high");
        requirePositive(candle.low(), "low");
        requirePositive(candle.close(), "close");
        if (candle.volume() == null || candle.volume().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("INVALID_NON_NEGATIVE_VOLUME");
        }
        BigDecimal maxBody = candle.open().max(candle.close()).max(candle.low());
        BigDecimal minBody = candle.open().min(candle.close()).min(candle.high());
        if (candle.high().compareTo(maxBody) < 0) {
            throw new IllegalArgumentException("INVALID_HIGH_BOUNDARY");
        }
        if (candle.low().compareTo(minBody) > 0) {
            throw new IllegalArgumentException("INVALID_LOW_BOUNDARY");
        }
    }

    private static void requirePositive(BigDecimal value, String field) {
        if (value == null || value.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("INVALID_POSITIVE_PRICE:" + field);
        }
    }

    private static boolean blank(String value) {
        return value == null || value.isBlank();
    }
}
