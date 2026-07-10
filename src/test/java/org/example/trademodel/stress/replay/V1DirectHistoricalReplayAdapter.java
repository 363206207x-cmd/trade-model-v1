package org.example.trademodel.stress.replay;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class V1DirectHistoricalReplayAdapter {

    private final List<HistoricalCandle> allCandles;
    private Instant replayTime;

    V1DirectHistoricalReplayAdapter(List<HistoricalCandle> candles) {
        HistoricalCandleValidator.validate(candles);
        this.allCandles = List.copyOf(candles);
    }

    ReplayFrame advanceTo(Instant nextReplayTime) {
        if (nextReplayTime == null) {
            throw new IllegalArgumentException("REPLAY_TIME_REQUIRED");
        }
        if (replayTime != null && nextReplayTime.isBefore(replayTime)) {
            throw new IllegalArgumentException("REPLAY_CLOCK_CANNOT_MOVE_BACKWARD");
        }
        replayTime = nextReplayTime;
        Map<HistoricalCandle.SeriesKey, List<HistoricalCandle>> visible = new LinkedHashMap<>();
        for (HistoricalCandle candle : allCandles) {
            if (!candle.timestampUtc().isAfter(replayTime)) {
                visible.computeIfAbsent(candle.seriesKey(), ignored -> new ArrayList<>()).add(candle);
            }
        }
        Map<HistoricalCandle.SeriesKey, List<HistoricalCandle>> immutable = new LinkedHashMap<>();
        visible.forEach((key, value) -> immutable.put(key, List.copyOf(value)));
        return new ReplayFrame(replayTime, Map.copyOf(immutable));
    }

    record ReplayFrame(Instant replayTime,
                       Map<HistoricalCandle.SeriesKey, List<HistoricalCandle>> visibleCandles) {

        List<HistoricalCandle> candles(String symbol, String timeframe) {
            return visibleCandles.getOrDefault(new HistoricalCandle.SeriesKey(symbol, timeframe), List.of());
        }
    }
}
