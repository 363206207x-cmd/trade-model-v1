package org.example.trademodel.providercall;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class ProviderFailureIsolationTest {
    private static final Instant NOW = Instant.parse("2026-07-19T12:00:00Z");

    @Test
    void repeatedDiscoveryBudgetRejectionDoesNotOpenProviderCircuit() {
        SwitchableBudget budget = new SwitchableBudget();
        budget.rejectDiscovery.set(true);
        try (Harness harness = new Harness(budget, 2, 2, 2)) {
            for (String symbol : List.of("BTCUSDT", "ETHUSDT", "SOLUSDT")) {
                ProviderCallResult<String> result = harness.coordinator.execute(request(symbol,
                        AssetPriority.P3_DISCOVERY, () -> ProviderAdapterResponse.ready("must-not-run", NOW)));
                assertThat(result.metadata().errorCode()).isEqualTo("PROVIDER_BUDGET_REJECTED");
            }

            assertThat(harness.circuit.state("TEST")).isEqualTo(ProviderCircuitState.CLOSED);
            assertThat(harness.audit.snapshot()).filteredOn(event ->
                    "PROVIDER_BUDGET_REJECTED".equals(event.reasonCode())).hasSize(3);
        }
    }

    @Test
    void repeatedExecutorRejectionDoesNotOpenProviderCircuit() {
        try (Harness harness = new Harness(new SwitchableBudget(), 2, 1, 1)) {
            harness.executor.close();

            for (String symbol : List.of("BTCUSDT", "ETHUSDT", "SOLUSDT")) {
                ProviderCallResult<String> result = harness.coordinator.execute(request(symbol,
                        AssetPriority.P3_DISCOVERY, () -> ProviderAdapterResponse.ready("must-not-run", NOW)));
                assertThat(result.metadata().errorCode()).isEqualTo("PROVIDER_EXECUTOR_REJECTED");
            }

            assertThat(harness.circuit.state("TEST")).isEqualTo(ProviderCircuitState.CLOSED);
        }
    }

    @Test
    void repeatedConcurrencyRejectionDoesNotOpenProviderCircuit() throws Exception {
        try (Harness harness = new Harness(new SwitchableBudget(), 2, 2, 1)) {
            CountDownLatch entered = new CountDownLatch(1);
            CountDownLatch release = new CountDownLatch(1);
            CompletableFuture<ProviderCallResult<String>> owner = CompletableFuture.supplyAsync(() ->
                    harness.coordinator.execute(request("BTCUSDT", AssetPriority.P0_POSITION, () -> {
                        entered.countDown();
                        await(release);
                        return ProviderAdapterResponse.ready("owner-ready", NOW);
                    }, Duration.ofSeconds(2))));
            assertThat(entered.await(1, TimeUnit.SECONDS)).isTrue();

            for (String symbol : List.of("ETHUSDT", "SOLUSDT", "DOGEUSDT")) {
                ProviderCallResult<String> result = harness.coordinator.execute(request(symbol,
                        AssetPriority.P3_DISCOVERY, () -> ProviderAdapterResponse.ready("must-not-run", NOW)));
                assertThat(result.metadata().errorCode()).isEqualTo("PROVIDER_CONCURRENCY_REJECTED");
            }

            assertThat(harness.circuit.state("TEST")).isEqualTo(ProviderCircuitState.CLOSED);
            release.countDown();
            assertThat(owner.get(1, TimeUnit.SECONDS).payload()).isEqualTo("owner-ready");
        }
    }

    @Test
    void localAdmissionFailureDoesNotMarkRemoteProviderDown() {
        try (Harness harness = new Harness(new SwitchableBudget(), 2, 1, 1)) {
            harness.executor.close();

            harness.coordinator.execute(request("BTCUSDT", AssetPriority.P3_DISCOVERY,
                    () -> ProviderAdapterResponse.ready("must-not-run", NOW)));

            ProviderHealthRegistry.ProviderHealthSnapshot health = harness.health.get("TEST",
                    harness.circuit.state("TEST"));
            assertThat(health.sourceStatus()).isEqualTo(UnifiedSourceStatus.WAITING_SYNC);
            assertThat(health.lastFailureAt()).isNull();
            assertThat(health.lastReasonCode()).isNull();
        }
    }

    @Test
    void rateLimit429AppliesRetryAfterWithoutCircuitFailure() {
        SwitchableBudget budget = new SwitchableBudget();
        try (Harness harness = new Harness(budget, 1, 1, 1)) {
            ProviderCallResult<String> result = harness.coordinator.execute(request("BTCUSDT",
                    AssetPriority.P3_DISCOVERY, () -> ProviderAdapterResponse.failed(
                            UnifiedSourceStatus.DEGRADED, 429, "RATE_LIMITED", 37L)));

            assertThat(result.metadata().errorCode()).isEqualTo("RATE_LIMITED");
            assertThat(budget.retryAfter).isEqualTo(NOW.plusSeconds(37));
            assertThat(harness.circuit.state("TEST")).isEqualTo(ProviderCircuitState.CLOSED);
            assertThat(harness.health.get("TEST", ProviderCircuitState.CLOSED).sourceStatus())
                    .isEqualTo(UnifiedSourceStatus.DEGRADED);
        }
    }

    @Test
    void remote5xxStillOpensCircuitAtThreshold() {
        try (Harness harness = new Harness(new SwitchableBudget(), 2, 2, 2)) {
            harness.coordinator.execute(request("BTCUSDT", AssetPriority.P3_DISCOVERY,
                    () -> ProviderAdapterResponse.failed(UnifiedSourceStatus.DEGRADED, 503,
                            "PROVIDER_5XX", null)));
            harness.coordinator.execute(request("ETHUSDT", AssetPriority.P3_DISCOVERY,
                    () -> ProviderAdapterResponse.failed(UnifiedSourceStatus.DEGRADED, 503,
                            "PROVIDER_5XX", null)));

            assertThat(harness.circuit.state("TEST")).isEqualTo(ProviderCircuitState.OPEN);
        }
    }

    @Test
    void remoteTransportFailureStillOpensCircuitAtThreshold() {
        try (Harness harness = new Harness(new SwitchableBudget(), 2, 2, 2)) {
            harness.coordinator.execute(request("BTCUSDT", AssetPriority.P3_DISCOVERY,
                    () -> { throw new IllegalStateException("fixture transport failure"); }));
            harness.coordinator.execute(request("ETHUSDT", AssetPriority.P3_DISCOVERY,
                    () -> { throw new IllegalStateException("fixture transport failure"); }));

            assertThat(harness.circuit.state("TEST")).isEqualTo(ProviderCircuitState.OPEN);
        }
    }

    @Test
    void p0PositionAllowedAfterP3LocalRejections() {
        SwitchableBudget budget = new SwitchableBudget();
        budget.rejectDiscovery.set(true);
        try (Harness harness = new Harness(budget, 2, 2, 2)) {
            for (String symbol : List.of("BTCUSDT", "ETHUSDT", "SOLUSDT")) {
                harness.coordinator.execute(request(symbol, AssetPriority.P3_DISCOVERY,
                        () -> ProviderAdapterResponse.ready("must-not-run", NOW)));
            }

            ProviderCallResult<String> position = harness.coordinator.execute(request("DOGEUSDT",
                    AssetPriority.P0_POSITION, () -> ProviderAdapterResponse.ready("position-ready", NOW)));

            assertThat(position.payload()).isEqualTo("position-ready");
            assertThat(harness.circuit.state("TEST")).isEqualTo(ProviderCircuitState.CLOSED);
        }
    }

    @Test
    void p0PositionBlockedAfterRealProviderCircuitOpens() {
        AtomicInteger physicalCalls = new AtomicInteger();
        try (Harness harness = new Harness(new SwitchableBudget(), 2, 2, 2)) {
            for (String symbol : List.of("BTCUSDT", "ETHUSDT")) {
                harness.coordinator.execute(request(symbol, AssetPriority.P3_DISCOVERY, () -> {
                    physicalCalls.incrementAndGet();
                    return ProviderAdapterResponse.failed(UnifiedSourceStatus.DEGRADED, 503,
                            "PROVIDER_5XX", null);
                }));
            }

            ProviderCallResult<String> position = harness.coordinator.execute(request("SOLUSDT",
                    AssetPriority.P0_POSITION, () -> {
                        physicalCalls.incrementAndGet();
                        return ProviderAdapterResponse.ready("must-not-run", NOW);
                    }));

            assertThat(position.metadata().errorCode()).isEqualTo("PROVIDER_CIRCUIT_OPEN");
            assertThat(physicalCalls).hasValue(2);
        }
    }

    @Test
    void failureClassifierSeparatesLocalAndRemoteOrigins() {
        assertThat(ProviderFailureClassifier.classify(ProviderAdapterResponse.failed(
                UnifiedSourceStatus.DEGRADED, 0, "PROVIDER_EXECUTOR_REJECTED", null)))
                .isEqualTo(ProviderFailureOrigin.LOCAL_ADMISSION);
        assertThat(ProviderFailureClassifier.classify(ProviderAdapterResponse.failed(
                UnifiedSourceStatus.DEGRADED, 0, "PROVIDER_EXECUTOR_QUEUE_TIMEOUT", null)))
                .isEqualTo(ProviderFailureOrigin.LOCAL_ADMISSION);
        assertThat(ProviderFailureClassifier.classify(ProviderAdapterResponse.failed(
                UnifiedSourceStatus.DEGRADED, 0, "PROVIDER_PRE_REMOTE_TIMEOUT", null)))
                .isEqualTo(ProviderFailureOrigin.LOCAL_ADMISSION);
        assertThat(ProviderFailureClassifier.classify(ProviderAdapterResponse.failed(
                UnifiedSourceStatus.DEGRADED, 0, "PROVIDER_BUDGET_REJECTED", null)))
                .isEqualTo(ProviderFailureOrigin.LOCAL_BUDGET);
        assertThat(ProviderFailureClassifier.classify(ProviderAdapterResponse.failed(
                UnifiedSourceStatus.DEGRADED, 0, "PROVIDER_CONCURRENCY_REJECTED", null)))
                .isEqualTo(ProviderFailureOrigin.LOCAL_CONCURRENCY);
        assertThat(ProviderFailureClassifier.classify(ProviderAdapterResponse.failed(
                UnifiedSourceStatus.ERROR, 503, "PROVIDER_5XX", null)))
                .isEqualTo(ProviderFailureOrigin.REMOTE_SERVER);
        assertThat(ProviderFailureClassifier.classify(ProviderAdapterResponse.failed(
                UnifiedSourceStatus.ERROR, 0, "PROVIDER_TIMEOUT", null)))
                .isEqualTo(ProviderFailureOrigin.REMOTE_TRANSPORT);
        assertThat(ProviderFailureClassifier.classify(null))
                .isEqualTo(ProviderFailureOrigin.REMOTE_PAYLOAD);
    }

    private static ProviderCallRequest<String> request(
            String symbol,
            AssetPriority priority,
            java.util.function.Supplier<ProviderAdapterResponse<String>> adapter) {
        return request(symbol, priority, adapter, Duration.ofSeconds(1));
    }

    private static ProviderCallRequest<String> request(
            String symbol,
            AssetPriority priority,
            java.util.function.Supplier<ProviderAdapterResponse<String>> adapter,
            Duration timeout) {
        ProviderRequestKey key = new ProviderRequestKey("TEST", ProviderDatasetType.PRICE,
                ProviderCallTestFixtures.spot(symbol), symbol, "GLOBAL", "BUCKET-" + symbol, "TEST_V1");
        return new ProviderCallRequest<>(key, priority, UserScanProfile.AUTO, RuntimeScanProfile.STANDARD,
                List.of("TEST"), "FM-TEST", Duration.ofSeconds(5), Duration.ofMinutes(2), timeout,
                "trace-" + symbol + "-" + priority, 0, 0, adapter);
    }

    private static void await(CountDownLatch latch) {
        try {
            latch.await();
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw new AssertionError(interrupted);
        }
    }

    private static final class SwitchableBudget implements ProviderRateBudget {
        private final AtomicBoolean rejectDiscovery = new AtomicBoolean();
        private volatile Instant retryAfter;
        private volatile AssetPriority lastRejectedPriority;

        @Override
        public boolean reserve(ProviderRequestKey key, AssetPriority priority, RuntimeScanProfile profile) {
            boolean accepted = !(rejectDiscovery.get() && priority == AssetPriority.P3_DISCOVERY);
            if (!accepted) lastRejectedPriority = priority;
            return accepted;
        }

        @Override
        public void applyRetryAfter(String provider, long seconds) {
            retryAfter = NOW.plusSeconds(seconds);
        }

        @Override
        public ProviderBudgetState state(String provider, ProviderCircuitState circuitState) {
            return new ProviderBudgetState(provider, 1000, 800, 0.8d, 0.2d,
                    0, 800, retryAfter, circuitState, lastRejectedPriority,
                    0, 0, 0, lastRejectedPriority == null ? null : "PROVIDER_BUDGET_REJECTED");
        }
    }

    private static final class Harness implements AutoCloseable {
        private final ProviderCallCoordinator coordinator;
        private final ProviderCircuitBreaker circuit;
        private final ProviderHealthRegistry health;
        private final ProviderCallAuditLog audit;
        private final ProviderCallExecutor executor;

        private Harness(ProviderRateBudget budget,
                        int circuitThreshold,
                        int executorWorkers,
                        int concurrencyLimit) {
            ProviderCallProperties properties = new ProviderCallProperties();
            properties.setEnabled(true);
            properties.setExternalCallsEnabled(true);
            properties.setMaxConcurrentProviderCalls(concurrencyLimit);
            properties.setMaxQueuedCalls(8);
            properties.setReservedPrioritySlots(0);
            Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
            ProviderConcurrencyGuard concurrency = new ProviderConcurrencyGuard(properties);
            this.executor = new ProviderCallExecutor(executorWorkers, 8, 1);
            this.circuit = new ProviderCircuitBreaker(circuitThreshold, 60, clock);
            this.health = new ProviderHealthRegistry(clock);
            this.audit = new ProviderCallAuditLog();
            this.coordinator = new ProviderCallCoordinator(properties, new SnapshotCacheService(),
                    new ProviderSingleFlightGuard(), budget, circuit, audit, concurrency,
                    health, executor, new ProviderSnapshotRetentionPolicy(), clock);
        }

        @Override
        public void close() {
            executor.close();
        }
    }
}
