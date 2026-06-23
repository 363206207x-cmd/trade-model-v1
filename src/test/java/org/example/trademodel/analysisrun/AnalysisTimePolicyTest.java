package org.example.trademodel.analysisrun;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AnalysisTimePolicyTest {
    private static final Clock FIXED = Clock.fixed(Instant.parse("2026-06-23T10:11:12Z"), ZoneOffset.UTC);

    @Test
    void normalizeAcceptsIsoOffsetAndLegacySecondPrecision() {
        assertThat(AnalysisTimePolicy.normalize("2026-06-23T10:11:12.987Z", "1m", FIXED))
                .isEqualTo(LocalDateTime.of(2026, 6, 23, 10, 11, 12));
        assertThat(AnalysisTimePolicy.normalize("2026-06-23 10:11:12", "1m", FIXED))
                .isEqualTo(LocalDateTime.of(2026, 6, 23, 10, 11, 12));
        assertThat(AnalysisTimePolicy.normalize("2026-06-23T23:30:00-02:00", "1d", FIXED))
                .isEqualTo(LocalDateTime.of(2026, 6, 24, 1, 30));
    }

    @Test
    void blankAnalysisTimeUsesInjectedClockNotSystemNow() {
        assertThat(AnalysisTimePolicy.normalize(" ", "5m", FIXED))
                .isEqualTo(LocalDateTime.of(2026, 6, 23, 10, 11, 12));
    }

    @Test
    void bucketsFloorToSupportedTimeframe() {
        LocalDateTime t = LocalDateTime.of(2026, 6, 23, 10, 14, 59);

        assertThat(AnalysisTimePolicy.canonicalBucket(t, "1m"))
                .isEqualTo(LocalDateTime.of(2026, 6, 23, 10, 14));
        assertThat(AnalysisTimePolicy.canonicalBucket(t, "3m"))
                .isEqualTo(LocalDateTime.of(2026, 6, 23, 10, 12));
        assertThat(AnalysisTimePolicy.canonicalBucket(t, "15m"))
                .isEqualTo(LocalDateTime.of(2026, 6, 23, 10, 0));
        assertThat(AnalysisTimePolicy.canonicalBucket(t, "4h"))
                .isEqualTo(LocalDateTime.of(2026, 6, 23, 8, 0));
        assertThat(AnalysisTimePolicy.canonicalBucket(t, "1d"))
                .isEqualTo(LocalDateTime.of(2026, 6, 23, 0, 0));
    }

    @Test
    void supportedTimeframesAreExplicitAndFailClosed() {
        assertThat(AnalysisTimePolicy.supportedTimeframes())
                .containsExactlyInAnyOrder("1m", "3m", "5m", "15m", "30m", "1h", "2h", "4h", "6h", "8h", "12h", "1d");
        assertThatThrownBy(() -> AnalysisTimePolicy.requireSupportedTimeframe("7m"))
                .isInstanceOf(AnalysisRunInputException.class)
                .hasMessageContaining("unsupported timeframe");
        assertThatThrownBy(() -> AnalysisTimePolicy.normalize("bad-time", "1m", FIXED))
                .isInstanceOf(AnalysisRunInputException.class)
                .hasMessageContaining("analysisTime is invalid");
    }
}
