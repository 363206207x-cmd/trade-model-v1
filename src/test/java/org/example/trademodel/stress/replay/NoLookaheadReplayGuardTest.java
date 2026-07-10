package org.example.trademodel.stress.replay;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class NoLookaheadReplayGuardTest {

    @Test
    void replayFrameExposesOnlyCandlesAtOrBeforeCurrentTime() {
        List<HistoricalCandle> contractOnlyCandles = List.of(
                candle("2026-01-01T00:00:00Z", "100"),
                candle("2026-01-01T00:05:00Z", "101"),
                candle("2026-01-01T00:10:00Z", "102"));
        Map<Instant, String> assertionLabels = Map.of(
                Instant.parse("2026-01-01T00:10:00Z"), "ASSERTION_METADATA_ONLY");
        V1DirectHistoricalReplayAdapter adapter = new V1DirectHistoricalReplayAdapter(contractOnlyCandles);

        V1DirectHistoricalReplayAdapter.ReplayFrame first = adapter.advanceTo(Instant.parse("2026-01-01T00:00:00Z"));
        assertThat(first.candles("TESTUSDT", "5m")).hasSize(1);
        assertThat(first.candles("TESTUSDT", "5m")).allMatch(candle -> !candle.timestampUtc().isAfter(first.replayTime()));

        V1DirectHistoricalReplayAdapter.ReplayFrame second = adapter.advanceTo(Instant.parse("2026-01-01T00:05:00Z"));
        assertThat(second.candles("TESTUSDT", "5m")).hasSize(2);
        assertThat(second.candles("TESTUSDT", "5m")).allMatch(candle -> !candle.timestampUtc().isAfter(second.replayTime()));
        assertThat(assertionLabels).isNotEmpty();
        assertThat(V1DirectHistoricalReplayAdapter.class.getDeclaredFields())
                .extracting(field -> field.getName())
                .doesNotContain("labels", "expectedOutcomes", "eventTags");
    }

    @Test
    void replayClockCannotMoveBackward() {
        V1DirectHistoricalReplayAdapter adapter = new V1DirectHistoricalReplayAdapter(List.of(
                candle("2026-01-01T00:00:00Z", "100"),
                candle("2026-01-01T00:05:00Z", "101")));
        adapter.advanceTo(Instant.parse("2026-01-01T00:05:00Z"));

        assertThatThrownBy(() -> adapter.advanceTo(Instant.parse("2026-01-01T00:00:00Z")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("REPLAY_CLOCK_CANNOT_MOVE_BACKWARD");
    }

    private static HistoricalCandle candle(String timestamp, String close) {
        BigDecimal closeValue = new BigDecimal(close);
        return new HistoricalCandle(Instant.parse(timestamp), "TESTUSDT", "5m", closeValue,
                closeValue.add(BigDecimal.ONE), closeValue.subtract(BigDecimal.ONE), closeValue, BigDecimal.TEN);
    }
}
