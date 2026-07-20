package org.example.trademodel.security;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LoginAttemptServiceTest {

    @Test
    void thresholdBlocksTemporarilyAndExactExpiryRecovers() {
        MutableClock clock = new MutableClock(Instant.parse("2026-07-20T00:00:00Z"));
        LoginAttemptService service = service(5, 16, clock);

        for (int attempt = 1; attempt < 5; attempt++) {
            assertThat(service.registerKnownUserFailure("operator"))
                    .isEqualTo(LoginAttemptService.FailureResult.FAILURE_RECORDED);
        }
        assertThat(service.registerKnownUserFailure("operator"))
                .isEqualTo(LoginAttemptService.FailureResult.TEMPORARILY_BLOCKED);
        assertThat(service.isKnownUserBlocked("operator")).isTrue();

        clock.advance(Duration.ofMinutes(15));
        assertThat(service.isKnownUserBlocked("operator")).isFalse();
        assertThat(service.knownUserFailureCount("operator")).isZero();
    }

    @Test
    void successfulLoginResetClearsOnlyKnownUserFailures() {
        LoginAttemptService service = service(5, 16, fixedClock());
        service.registerKnownUserFailure("operator");
        service.registerKnownUserFailure("operator");
        service.registerUnknownUsernameFailure("unknown-user");

        service.resetKnownUser("operator");

        assertThat(service.knownUserFailureCount("operator")).isZero();
        assertThat(service.isKnownUserBlocked("operator")).isFalse();
        assertThat(service.unknownUsernameStateCount()).isEqualTo(1);
    }

    @Test
    void knownUserLockSurvivesUnknownUsernameSpray() {
        LoginAttemptService service = service(3, 2, fixedClock());
        service.registerKnownUserFailure("operator");
        service.registerKnownUserFailure("operator");
        service.registerKnownUserFailure("operator");

        sprayUnknownUsernames(service, 100);

        assertThat(service.isKnownUserBlocked("operator")).isTrue();
        assertThat(service.knownUserFailureCount("operator")).isEqualTo(3);
        assertThat(service.knownUserStateCount()).isEqualTo(1);
        assertThat(service.unknownUsernameStateCount()).isEqualTo(2);
    }

    @Test
    void knownUserFailureCountSurvivesUnknownUsernameSpray() {
        LoginAttemptService service = service(5, 2, fixedClock());
        service.registerKnownUserFailure("operator");
        service.registerKnownUserFailure("operator");

        sprayUnknownUsernames(service, 100);

        assertThat(service.knownUserFailureCount("operator")).isEqualTo(2);
        assertThat(service.unknownUsernameStateCount()).isEqualTo(2);
    }

    @Test
    void unknownUsernameStateRemainsBounded() {
        LoginAttemptService service = service(5, 2, fixedClock());

        sprayUnknownUsernames(service, 100);

        assertThat(service.unknownUsernameStateCount()).isEqualTo(2);
        assertThat(service.knownUserStateCount()).isZero();
    }

    @Test
    void unknownUsernameCannotEvictActiveKnownState() {
        LoginAttemptService service = service(5, 1, fixedClock());
        service.registerKnownUserFailure("operator");

        service.registerUnknownUsernameFailure("unknown-one");
        service.registerUnknownUsernameFailure("unknown-two");

        assertThat(service.knownUserFailureCount("operator")).isEqualTo(1);
        assertThat(service.knownUserStateCount()).isEqualTo(1);
        assertThat(service.unknownUsernameStateCount()).isEqualTo(1);
    }

    @Test
    void expiredInactiveKnownStateCanBeCleaned() {
        MutableClock clock = new MutableClock(Instant.parse("2026-07-20T00:00:00Z"));
        LoginAttemptService service = service(5, 2, clock);
        service.registerKnownUserFailure("operator");

        clock.advance(Duration.ofMinutes(15));

        assertThat(service.knownUserStateCount()).isZero();
        assertThat(service.knownUserFailureCount("operator")).isZero();
    }

    @Test
    void saturatedKnownUserStateFailsClosedWithoutEvictingActiveProtection() {
        LoginAttemptService service = service(5, 1, fixedClock());
        service.registerKnownUserFailure("operator");

        assertThat(service.registerKnownUserFailure("second-known-user"))
                .isEqualTo(LoginAttemptService.FailureResult.TEMPORARILY_BLOCKED);

        assertThat(service.knownUserFailureCount("operator")).isEqualTo(1);
        assertThat(service.knownUserStateCount()).isEqualTo(1);
        assertThat(service.isKnownUserBlocked("operator")).isTrue();
        assertThat(service.isKnownUserBlocked("second-known-user")).isTrue();
    }

    @Test
    void concurrentKnownAndUnknownFailuresRemainSafe() throws Exception {
        LoginAttemptService service = service(1_000, 16, fixedClock());
        int workers = 8;
        int failuresPerWorker = 20;
        ExecutorService executor = Executors.newFixedThreadPool(workers);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<?>> futures = new ArrayList<>();
        try {
            for (int worker = 0; worker < workers; worker++) {
                int workerId = worker;
                futures.add(executor.submit(() -> {
                    start.await();
                    for (int failure = 0; failure < failuresPerWorker; failure++) {
                        service.registerKnownUserFailure("operator");
                        service.registerUnknownUsernameFailure("unknown-" + workerId + "-" + failure);
                    }
                    return null;
                }));
            }
            start.countDown();
            for (Future<?> future : futures) {
                future.get();
            }
        } finally {
            executor.shutdownNow();
        }

        assertThat(service.knownUserFailureCount("operator"))
                .isEqualTo(workers * failuresPerWorker);
        assertThat(service.knownUserStateCount()).isEqualTo(1);
        assertThat(service.unknownUsernameStateCount()).isEqualTo(16);
    }

    private static LoginAttemptService service(int threshold, int capacity, Clock clock) {
        return new LoginAttemptService(
                threshold, Duration.ofMinutes(15), Duration.ofMinutes(15), capacity, clock);
    }

    private static Clock fixedClock() {
        return Clock.fixed(Instant.parse("2026-07-20T00:00:00Z"), ZoneId.of("UTC"));
    }

    private static void sprayUnknownUsernames(LoginAttemptService service, int count) {
        for (int index = 0; index < count; index++) {
            service.registerUnknownUsernameFailure("unknown-" + index);
        }
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
