package org.example.trademodel.security;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LoginAttemptServiceTest {

    @Test
    void thresholdBlocksTemporarilyAndExactExpiryRecovers() {
        MutableClock clock = new MutableClock(Instant.parse("2026-07-20T00:00:00Z"));
        LoginAttemptService service = new LoginAttemptService(
                5, Duration.ofMinutes(15), Duration.ofMinutes(15), 16, clock);

        for (int attempt = 1; attempt < 5; attempt++) {
            assertThat(service.registerFailure("operator"))
                    .isEqualTo(LoginAttemptService.FailureResult.FAILURE_RECORDED);
        }
        assertThat(service.registerFailure("operator"))
                .isEqualTo(LoginAttemptService.FailureResult.TEMPORARILY_BLOCKED);
        assertThat(service.isBlocked("operator")).isTrue();

        clock.advance(Duration.ofMinutes(15));
        assertThat(service.isBlocked("operator")).isFalse();
        assertThat(service.failureCount("operator")).isZero();
    }

    @Test
    void successfulLoginResetClearsFailures() {
        LoginAttemptService service = new LoginAttemptService(
                5, Duration.ofMinutes(15), Duration.ofMinutes(15), 16,
                Clock.fixed(Instant.parse("2026-07-20T00:00:00Z"), ZoneId.of("UTC")));
        service.registerFailure("operator");
        service.registerFailure("operator");

        service.reset("operator");

        assertThat(service.failureCount("operator")).isZero();
        assertThat(service.isBlocked("operator")).isFalse();
    }

    @Test
    void trackedUsernameStateRemainsBounded() {
        LoginAttemptService service = new LoginAttemptService(
                5, Duration.ofMinutes(15), Duration.ofMinutes(15), 2,
                Clock.fixed(Instant.parse("2026-07-20T00:00:00Z"), ZoneId.of("UTC")));

        service.registerFailure("one");
        service.registerFailure("two");
        service.registerFailure("three");

        assertThat(service.trackedUsernameCount()).isEqualTo(2);
        assertThat(service.failureCount("one")).isZero();
    }

    private static final class MutableClock extends Clock {
        private Instant instant;

        private MutableClock(Instant instant) {
            this.instant = instant;
        }

        void advance(Duration duration) {
            instant = instant.plus(duration);
        }

        @Override
        public ZoneId getZone() {
            return ZoneId.of("UTC");
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return instant;
        }
    }
}
