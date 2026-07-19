package org.example.trademodel.providercall;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;

class ProviderHalfOpenProbeLifecycleTest {
    private static final Instant NOW = Instant.parse("2026-07-19T12:00:00Z");

    @Test
    void halfOpenExecutorRejectionReleasesProbe() {
        try (Harness harness = Harness.switchable(1, 1)) {
            openCircuit(harness);
            harness.clock.advance(Duration.ofSeconds(6));
            harness.executor.close();

            ProviderCallResult<String> result = harness.coordinator.execute(request("executor-rejected",
                    AssetPriority.P3_DISCOVERY, Duration.ofSeconds(1),
                    () -> ProviderAdapterResponse.ready("must-not-run", harness.clock.instant())));

            assertThat(result.metadata().errorCode()).isEqualTo("PROVIDER_EXECUTOR_REJECTED");
            assertReleasedHalfOpenProbe(harness);
        }
    }

    @Test
    void halfOpenConcurrencyRejectionReleasesProbe() {
        try (Harness harness = Harness.switchable(1, 1)) {
            openCircuit(harness);
            harness.clock.advance(Duration.ofSeconds(6));
            try (ProviderConcurrencyGuard.Lease occupied = harness.concurrency.tryAcquire(
                    ProviderDatasetType.PRICE, AssetPriority.P0_POSITION)) {

                ProviderCallResult<String> result = harness.coordinator.execute(request("concurrency-rejected",
                        AssetPriority.P3_DISCOVERY, Duration.ofSeconds(1),
                        () -> ProviderAdapterResponse.ready("must-not-run", harness.clock.instant())));

                assertThat(occupied).isNotNull();
                assertThat(result.metadata().errorCode()).isEqualTo("PROVIDER_CONCURRENCY_REJECTED");
                assertReleasedHalfOpenProbe(harness);
            }
        }
    }

    @Test
    void halfOpenBudgetRejectionReleasesProbe() {
        try (Harness harness = Harness.switchable(2, 2)) {
            openCircuit(harness);
            harness.clock.advance(Duration.ofSeconds(6));
            harness.switchableBudget.rejectAll.set(true);

            ProviderCallResult<String> result = harness.coordinator.execute(request("budget-rejected",
                    AssetPriority.P3_DISCOVERY, Duration.ofSeconds(1),
                    () -> ProviderAdapterResponse.ready("must-not-run", harness.clock.instant())));

            assertThat(result.metadata().errorCode()).isEqualTo("PROVIDER_BUDGET_REJECTED");
            assertReleasedHalfOpenProbe(harness);
        }
    }

    @Test
    void halfOpenPerSymbolGapReleasesProbe() {
        try (Harness harness = Harness.realBudget(60)) {
            openCircuit(harness);
            harness.clock.advance(Duration.ofSeconds(6));

            ProviderCallResult<String> result = harness.coordinator.execute(request("minimum-gap",
                    AssetPriority.P3_DISCOVERY, Duration.ofSeconds(1),
                    () -> ProviderAdapterResponse.ready("must-not-run", harness.clock.instant())));

            assertThat(result.metadata().errorCode()).isEqualTo("PROVIDER_BUDGET_REJECTED");
            assertThat(result.budgetState().lastRejectionReason()).isEqualTo("PER_SYMBOL_MINIMUM_GAP");
            assertReleasedHalfOpenProbe(harness);
        }
    }

    @Test
    void halfOpenLocalConfigurationFailureReleasesProbe() {
        try (Harness harness = Harness.switchable(2, 2)) {
            openCircuit(harness);
            harness.clock.advance(Duration.ofSeconds(6));

            ProviderCallResult<String> result = harness.coordinator.execute(request("not-configured",
                    AssetPriority.P3_DISCOVERY, Duration.ofSeconds(1),
                    () -> ProviderAdapterResponse.failed(UnifiedSourceStatus.NOT_CONFIGURED, 0,
                            "PROVIDER_NOT_CONFIGURED", null)));

            assertThat(result.metadata().sourceStatus()).isEqualTo(UnifiedSourceStatus.NOT_CONFIGURED);
            assertReleasedHalfOpenProbe(harness);
        }
    }

    @Test
    void halfOpen429AppliesRetryAfterAndSettlesProbe() {
        AtomicInteger physicalCalls = new AtomicInteger();
        try (Harness harness = Harness.realBudget(1)) {
            openCircuit(harness);
            harness.clock.advance(Duration.ofSeconds(6));

            ProviderCallResult<String> rateLimited = harness.coordinator.execute(request("rate-limited",
                    AssetPriority.P3_DISCOVERY, Duration.ofSeconds(1), () -> {
                        physicalCalls.incrementAndGet();
                        return ProviderAdapterResponse.failed(UnifiedSourceStatus.DEGRADED, 429,
                                "RATE_LIMITED", 30L);
                    }));

            assertThat(rateLimited.metadata().errorCode()).isEqualTo("RATE_LIMITED");
            assertThat(rateLimited.budgetState().retryAfter()).isEqualTo(harness.clock.instant().plusSeconds(30));
            assertReleasedHalfOpenProbe(harness);

            ProviderCallResult<String> suspended = harness.coordinator.execute(request("retry-after-active",
                    AssetPriority.P0_POSITION, Duration.ofSeconds(1), () -> {
                        physicalCalls.incrementAndGet();
                        return ProviderAdapterResponse.ready("must-not-run", harness.clock.instant());
                    }));
            assertThat(suspended.metadata().errorCode()).isEqualTo("PROVIDER_BUDGET_REJECTED");
            assertThat(physicalCalls).hasValue(1);
            assertReleasedHalfOpenProbe(harness);

            harness.clock.advance(Duration.ofSeconds(31));
            ProviderCallResult<String> recovered = harness.coordinator.execute(request("retry-after-expired",
                    AssetPriority.P0_POSITION, Duration.ofSeconds(1), () -> {
                        physicalCalls.incrementAndGet();
                        return ProviderAdapterResponse.ready("recovered", harness.clock.instant());
                    }));
            assertThat(recovered.payload()).isEqualTo("recovered");
            assertThat(physicalCalls).hasValue(2);
            assertThat(harness.circuit.state("TEST")).isEqualTo(ProviderCircuitState.CLOSED);
        }
    }

    @Test
    void halfOpenAuthFailureSettlesAvailabilityProbe() {
        try (Harness harness = Harness.switchable(2, 2)) {
            openCircuit(harness);
            harness.clock.advance(Duration.ofSeconds(6));

            ProviderCallResult<String> auth = harness.coordinator.execute(request("auth-failure",
                    AssetPriority.P3_DISCOVERY, Duration.ofSeconds(1),
                    () -> ProviderAdapterResponse.failed(UnifiedSourceStatus.ERROR, 403,
                            "AUTHENTICATION_FAILED", null)));

            assertThat(auth.metadata().errorCode()).isEqualTo("AUTHENTICATION_FAILED");
            assertThat(harness.health.get("TEST", harness.circuit.state("TEST")).lastReasonCode())
                    .isEqualTo("AUTHENTICATION_FAILED");
            assertReleasedHalfOpenProbe(harness);
        }
    }

    @Test
    void halfOpenRemote5xxReopensCircuit() {
        AtomicInteger calls = new AtomicInteger();
        try (Harness harness = Harness.switchable(2, 2)) {
            openCircuit(harness);
            harness.clock.advance(Duration.ofSeconds(6));

            harness.coordinator.execute(request("remote-5xx", AssetPriority.P0_POSITION,
                    Duration.ofSeconds(1), 2, 0, () -> {
                        calls.incrementAndGet();
                        return ProviderAdapterResponse.failed(UnifiedSourceStatus.DEGRADED, 503,
                                "PROVIDER_5XX", null);
                    }));

            assertThat(calls).hasValue(1);
            assertThat(harness.circuit.state("TEST")).isEqualTo(ProviderCircuitState.OPEN);
            assertThat(harness.circuit.halfOpenProbeClaimed("TEST")).isFalse();
        }
    }

    @Test
    void halfOpenRemoteTimeoutReopensCircuit() {
        try (Harness harness = Harness.switchable(2, 2)) {
            openCircuit(harness);
            harness.clock.advance(Duration.ofSeconds(6));
            AtomicBoolean interrupted = new AtomicBoolean();
            AtomicInteger calls = new AtomicInteger();

            ProviderCallResult<String> result = harness.coordinator.execute(request("physical-timeout",
                    AssetPriority.P0_POSITION, Duration.ofMillis(80), 0, 1, () -> {
                        calls.incrementAndGet();
                        try {
                            new CountDownLatch(1).await();
                        } catch (InterruptedException expected) {
                            interrupted.set(true);
                            Thread.currentThread().interrupt();
                        }
                        return ProviderAdapterResponse.ready("late", harness.clock.instant());
                    }));

            assertThat(result.metadata().errorCode()).isEqualTo("PROVIDER_TIMEOUT");
            awaitState(interrupted::get);
            assertThat(calls).hasValue(1);
            assertThat(harness.circuit.state("TEST")).isEqualTo(ProviderCircuitState.OPEN);
            assertThat(harness.circuit.halfOpenProbeClaimed("TEST")).isFalse();
        }
    }

    @Test
    void halfOpenRemoteTransportFailureReopensCircuit() {
        assertHalfOpenRemoteFailureReopens(() -> {
            throw new IllegalStateException("fixture transport failure");
        });
    }

    @Test
    void halfOpenMalformedPayloadReopensCircuit() {
        assertHalfOpenRemoteFailureReopens(() -> null);
    }

    @Test
    void halfOpenSuccessClosesCircuit() {
        try (Harness harness = Harness.switchable(2, 2)) {
            openCircuit(harness);
            harness.clock.advance(Duration.ofSeconds(6));

            ProviderCallResult<String> result = harness.coordinator.execute(request("success",
                    AssetPriority.P0_POSITION, Duration.ofSeconds(1),
                    () -> ProviderAdapterResponse.ready("ready", harness.clock.instant())));

            assertThat(result.payload()).isEqualTo("ready");
            assertThat(harness.circuit.state("TEST")).isEqualTo(ProviderCircuitState.CLOSED);
            assertThat(harness.circuit.halfOpenProbeClaimed("TEST")).isFalse();
        }
    }

    @Test
    void halfOpenEmptyConfirmedClosesCircuit() {
        try (Harness harness = Harness.switchable(2, 2)) {
            openCircuit(harness);
            harness.clock.advance(Duration.ofSeconds(6));

            ProviderCallResult<String> result = harness.coordinator.execute(request("empty-confirmed",
                    AssetPriority.P0_POSITION, Duration.ofSeconds(1),
                    () -> new ProviderAdapterResponse<>(null, UnifiedSourceStatus.EMPTY_CONFIRMED,
                            harness.clock.instant(), 200, null, "EMPTY_CONFIRMED")));

            assertThat(result.metadata().sourceStatus()).isEqualTo(UnifiedSourceStatus.EMPTY_CONFIRMED);
            assertThat(harness.circuit.state("TEST")).isEqualTo(ProviderCircuitState.CLOSED);
            assertThat(harness.circuit.halfOpenProbeClaimed("TEST")).isFalse();
        }
    }

    @Test
    void waiterTimeoutCannotReleaseOwnerCircuitPermit() throws Exception {
        try (Harness harness = Harness.switchable(2, 2)) {
            openCircuit(harness);
            harness.clock.advance(Duration.ofSeconds(6));
            CountDownLatch entered = new CountDownLatch(1);
            CountDownLatch release = new CountDownLatch(1);
            CompletableFuture<ProviderCallResult<String>> owner = CompletableFuture.supplyAsync(() ->
                    harness.coordinator.execute(request("half-open-owner", AssetPriority.P0_POSITION,
                            Duration.ofSeconds(2), () -> {
                                entered.countDown();
                                await(release);
                                return ProviderAdapterResponse.ready("owner-ready", harness.clock.instant());
                            })));
            assertThat(entered.await(1, TimeUnit.SECONDS)).isTrue();
            assertThat(harness.circuit.halfOpenProbeClaimed("TEST")).isTrue();

            ProviderCallResult<String> waiter = harness.coordinator.execute(request("short-waiter",
                    AssetPriority.P3_DISCOVERY, Duration.ofMillis(40),
                    () -> ProviderAdapterResponse.ready("must-not-run", harness.clock.instant())));

            assertThat(waiter.metadata().errorCode()).isEqualTo("PROVIDER_TIMEOUT");
            assertThat(harness.circuit.halfOpenProbeClaimed("TEST")).isTrue();
            assertThat(harness.circuit.tryAcquire("TEST").acquired()).isFalse();
            release.countDown();
            assertThat(owner.get(1, TimeUnit.SECONDS).payload()).isEqualTo("owner-ready");
            assertThat(harness.circuit.state("TEST")).isEqualTo(ProviderCircuitState.CLOSED);
        }
    }

    @Test
    void permitSettlementIsIdempotent() {
        MutableClock clock = new MutableClock(NOW);
        ProviderCircuitBreaker circuit = new ProviderCircuitBreaker(1, 5, clock);
        ProviderCircuitPermit opening = circuit.tryAcquire("TEST");
        assertThat(opening.recordRemoteFailure()).isTrue();
        clock.advance(Duration.ofSeconds(6));
        ProviderCircuitPermit probe = circuit.tryAcquire("TEST");

        assertThat(probe.recordRemoteFailure()).isTrue();
        assertThat(probe.recordSuccess()).isFalse();
        assertThat(probe.recordRemoteReachable()).isFalse();
        assertThat(probe.releaseWithoutRemoteAttempt()).isFalse();
        assertThat(probe.settled()).isTrue();
        assertThat(circuit.state("TEST")).isEqualTo(ProviderCircuitState.OPEN);
        assertThat(circuit.halfOpenProbeClaimed("TEST")).isFalse();
    }

    @Test
    void completedPathNeverLeavesHalfOpenProbeClaimed() {
        assertSettlementClearsClaim(ProviderCircuitPermit::recordSuccess, ProviderCircuitState.CLOSED);
        assertSettlementClearsClaim(ProviderCircuitPermit::recordRemoteFailure, ProviderCircuitState.OPEN);
        assertSettlementClearsClaim(ProviderCircuitPermit::recordRemoteReachable, ProviderCircuitState.HALF_OPEN);
        assertSettlementClearsClaim(ProviderCircuitPermit::releaseWithoutRemoteAttempt,
                ProviderCircuitState.HALF_OPEN);
    }

    @Test
    void localPressureRecoveryAllowsLaterP0PositionProbe() {
        try (Harness harness = Harness.switchable(2, 2)) {
            openCircuit(harness);
            harness.clock.advance(Duration.ofSeconds(6));
            harness.switchableBudget.rejectDiscovery.set(true);

            ProviderCallResult<String> discovery = harness.coordinator.execute(request("discovery-pressure",
                    AssetPriority.P3_DISCOVERY, Duration.ofSeconds(1),
                    () -> ProviderAdapterResponse.ready("must-not-run", harness.clock.instant())));
            assertThat(discovery.metadata().errorCode()).isEqualTo("PROVIDER_BUDGET_REJECTED");
            assertReleasedHalfOpenProbe(harness);

            ProviderCallResult<String> position = harness.coordinator.execute(request("position-recovery",
                    AssetPriority.P0_POSITION, Duration.ofSeconds(1),
                    () -> ProviderAdapterResponse.ready("position-ready", harness.clock.instant())));
            assertThat(position.payload()).isEqualTo("position-ready");
            assertThat(harness.circuit.state("TEST")).isEqualTo(ProviderCircuitState.CLOSED);
        }
    }

    @Test
    void shutdownCancelsUnstartedHalfOpenProbe() throws Exception {
        try (Harness harness = Harness.switchable(1, 2)) {
            openCircuit(harness);
            harness.clock.advance(Duration.ofSeconds(6));
            CountDownLatch workerOccupied = new CountDownLatch(1);
            CountDownLatch releaseWorker = new CountDownLatch(1);
            harness.executor.submit(AssetPriority.P0_POSITION, () -> {
                workerOccupied.countDown();
                await(releaseWorker);
                return null;
            });
            assertThat(workerOccupied.await(1, TimeUnit.SECONDS)).isTrue();
            AtomicInteger physicalCalls = new AtomicInteger();

            CompletableFuture<ProviderCallResult<String>> queuedProbe = CompletableFuture.supplyAsync(() ->
                    harness.coordinator.execute(request("shutdown-queued-probe",
                            AssetPriority.P0_POSITION, Duration.ofSeconds(5), () -> {
                                physicalCalls.incrementAndGet();
                                return ProviderAdapterResponse.ready("must-not-run", harness.clock.instant());
                            })));
            awaitState(() -> harness.circuit.halfOpenProbeClaimed("TEST")
                    && harness.executor.state().queuedCalls() == 1);

            assertThat(harness.executor.shutdownCleanly(Duration.ZERO)).isFalse();
            ProviderCallResult<String> result = queuedProbe.get(1, TimeUnit.SECONDS);

            assertThat(result.metadata().errorCode()).isEqualTo("PROVIDER_CALL_CANCELLED");
            assertThat(physicalCalls).hasValue(0);
            assertReleasedHalfOpenProbe(harness);
            releaseWorker.countDown();
        }
    }

    private static void assertHalfOpenRemoteFailureReopens(
            java.util.function.Supplier<ProviderAdapterResponse<String>> response) {
        try (Harness harness = Harness.switchable(2, 2)) {
            openCircuit(harness);
            harness.clock.advance(Duration.ofSeconds(6));

            harness.coordinator.execute(request("remote-failure", AssetPriority.P0_POSITION,
                    Duration.ofSeconds(1), response));

            assertThat(harness.circuit.state("TEST")).isEqualTo(ProviderCircuitState.OPEN);
            assertThat(harness.circuit.halfOpenProbeClaimed("TEST")).isFalse();
        }
    }

    private static void assertSettlementClearsClaim(Consumer<ProviderCircuitPermit> settlement,
                                                    ProviderCircuitState expectedState) {
        MutableClock clock = new MutableClock(NOW);
        ProviderCircuitBreaker circuit = new ProviderCircuitBreaker(1, 5, clock);
        assertThat(circuit.tryAcquire("TEST").recordRemoteFailure()).isTrue();
        clock.advance(Duration.ofSeconds(6));
        ProviderCircuitPermit probe = circuit.tryAcquire("TEST");
        assertThat(probe.halfOpenProbe()).isTrue();

        settlement.accept(probe);

        assertThat(probe.settled()).isTrue();
        assertThat(circuit.state("TEST")).isEqualTo(expectedState);
        assertThat(circuit.halfOpenProbeClaimed("TEST")).isFalse();
    }

    private static void assertReleasedHalfOpenProbe(Harness harness) {
        assertThat(harness.circuit.state("TEST")).isEqualTo(ProviderCircuitState.HALF_OPEN);
        assertThat(harness.circuit.halfOpenProbeClaimed("TEST")).isFalse();
        ProviderCircuitPermit next = harness.circuit.tryAcquire("TEST");
        assertThat(next.acquired()).isTrue();
        assertThat(next.halfOpenProbe()).isTrue();
        assertThat(next.releaseWithoutRemoteAttempt()).isTrue();
    }

    private static void openCircuit(Harness harness) {
        ProviderCallResult<String> openingFailure = harness.coordinator.execute(request("open-circuit",
                AssetPriority.P0_POSITION, Duration.ofSeconds(1),
                () -> ProviderAdapterResponse.failed(UnifiedSourceStatus.DEGRADED, 503,
                        "PROVIDER_5XX", null)));
        assertThat(openingFailure.metadata().errorCode()).isEqualTo("PROVIDER_5XX");
        assertThat(harness.circuit.state("TEST")).isEqualTo(ProviderCircuitState.OPEN);
    }

    private static ProviderCallRequest<String> request(
            String trace,
            AssetPriority priority,
            Duration timeout,
            java.util.function.Supplier<ProviderAdapterResponse<String>> adapter) {
        return request(trace, priority, timeout, 0, 0, adapter);
    }

    private static ProviderCallRequest<String> request(
            String trace,
            AssetPriority priority,
            Duration timeout,
            int maxRetry5xx,
            int maxRetryTimeout,
            java.util.function.Supplier<ProviderAdapterResponse<String>> adapter) {
        ProviderRequestKey key = new ProviderRequestKey("TEST", ProviderDatasetType.PRICE,
                ProviderCallTestFixtures.spot("BTCUSDT"), "BTCUSDT", "GLOBAL", "BUCKET", "TEST_V1");
        return new ProviderCallRequest<>(key, priority, UserScanProfile.AUTO, RuntimeScanProfile.STANDARD,
                List.of("TEST"), "FM-TEST", Duration.ofSeconds(5), Duration.ofMinutes(2), timeout,
                "trace-" + trace, maxRetry5xx, maxRetryTimeout, adapter);
    }

    private static void await(CountDownLatch latch) {
        try {
            latch.await();
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw new AssertionError(interrupted);
        }
    }

    private static void awaitState(java.util.function.BooleanSupplier condition) {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(2);
        while (!condition.getAsBoolean() && System.nanoTime() < deadline) {
            try {
                Thread.sleep(10L);
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
                throw new AssertionError(interrupted);
            }
        }
        assertThat(condition.getAsBoolean()).isTrue();
    }

    private static final class Harness implements AutoCloseable {
        private final MutableClock clock;
        private final SwitchableBudget switchableBudget;
        private final ProviderRateBudget budget;
        private final ProviderCircuitBreaker circuit;
        private final ProviderConcurrencyGuard concurrency;
        private final ProviderCallExecutor executor;
        private final ProviderHealthRegistry health;
        private final ProviderCallCoordinator coordinator;

        private Harness(int workers, int concurrencyLimit, int minimumGapSeconds, boolean realBudget) {
            this.clock = new MutableClock(NOW);
            ProviderCallProperties properties = new ProviderCallProperties();
            properties.setEnabled(true);
            properties.setExternalCallsEnabled(true);
            properties.setMaxConcurrentProviderCalls(concurrencyLimit);
            properties.setMaxQueuedCalls(8);
            properties.setReservedPrioritySlots(0);
            properties.setPerSymbolMinimumGapSeconds(minimumGapSeconds);
            if (realBudget) {
                ProviderRateBudgetManager manager = new ProviderRateBudgetManager(properties, clock);
                manager.register("TEST", 1000);
                this.budget = manager;
                this.switchableBudget = null;
            } else {
                this.switchableBudget = new SwitchableBudget(clock);
                this.budget = switchableBudget;
            }
            this.circuit = new ProviderCircuitBreaker(1, 5, clock);
            this.concurrency = new ProviderConcurrencyGuard(properties);
            this.executor = new ProviderCallExecutor(workers, 8, 1);
            this.health = new ProviderHealthRegistry(clock);
            this.coordinator = new ProviderCallCoordinator(properties, new SnapshotCacheService(),
                    new ProviderSingleFlightGuard(), budget, circuit, new ProviderCallAuditLog(), concurrency,
                    health, executor, new ProviderSnapshotRetentionPolicy(), clock);
        }

        private static Harness switchable(int workers, int concurrencyLimit) {
            return new Harness(workers, concurrencyLimit, 1, false);
        }

        private static Harness realBudget(int minimumGapSeconds) {
            return new Harness(2, 2, minimumGapSeconds, true);
        }

        @Override
        public void close() {
            executor.close();
        }
    }

    private static final class SwitchableBudget implements ProviderRateBudget {
        private final Clock clock;
        private final AtomicBoolean rejectAll = new AtomicBoolean();
        private final AtomicBoolean rejectDiscovery = new AtomicBoolean();
        private volatile Instant retryAfter;
        private volatile AssetPriority lastRejectedPriority;

        private SwitchableBudget(Clock clock) {
            this.clock = clock;
        }

        @Override
        public boolean reserve(ProviderRequestKey key, AssetPriority priority, RuntimeScanProfile profile) {
            boolean accepted = !rejectAll.get()
                    && !(rejectDiscovery.get() && priority == AssetPriority.P3_DISCOVERY);
            if (!accepted) lastRejectedPriority = priority;
            return accepted;
        }

        @Override
        public void applyRetryAfter(String provider, long seconds) {
            retryAfter = clock.instant().plusSeconds(seconds);
        }

        @Override
        public ProviderBudgetState state(String provider, ProviderCircuitState circuitState) {
            return new ProviderBudgetState(provider, 1000, 800, 0.8d, 0.2d,
                    0, 800, retryAfter, circuitState, lastRejectedPriority,
                    0, 0, 0, lastRejectedPriority == null ? null : "PROVIDER_BUDGET_REJECTED");
        }
    }

    private static final class MutableClock extends Clock {
        private Instant current;

        private MutableClock(Instant current) {
            this.current = current;
        }

        private void advance(Duration duration) {
            current = current.plus(duration);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return current;
        }
    }
}
