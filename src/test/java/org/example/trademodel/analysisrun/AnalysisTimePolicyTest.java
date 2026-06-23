package org.example.trademodel.analysisrun;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class AnalysisTimePolicyTest {
    @Test
    void normalizeAcceptsIsoAndLegacySecondPrecision() {
        assertThat(AnalysisTimePolicy.normalize("2026-06-23T10:11:12.987", LocalDateTime.MIN))
                .isEqualTo(LocalDateTime.of(2026, 6, 23, 10, 11, 12));
        assertThat(AnalysisTimePolicy.normalize("2026-06-23 10:11:12", LocalDateTime.MIN))
                .isEqualTo(LocalDateTime.of(2026, 6, 23, 10, 11, 12));
    }

    @Test
    void invalidInputFallsBackAndBucketIsMinuteScoped() {
        LocalDateTime fallback = LocalDateTime.of(2026, 6, 23, 10, 11, 12, 900);
        LocalDateTime normalized = AnalysisTimePolicy.normalize("bad-time", fallback);
        assertThat(normalized).isEqualTo(LocalDateTime.of(2026, 6, 23, 10, 11, 12));
        assertThat(AnalysisTimePolicy.idempotencyBucket(normalized))
                .isEqualTo(LocalDateTime.of(2026, 6, 23, 10, 11));
    }
}
