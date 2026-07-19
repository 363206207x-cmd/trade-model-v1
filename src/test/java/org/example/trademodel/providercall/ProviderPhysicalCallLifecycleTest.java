package org.example.trademodel.providercall;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProviderPhysicalCallLifecycleTest {
    private static final Instant NOW = Instant.parse("2026-07-19T10:00:00Z");

    @Test
    void uninterruptibleAdapterCannotReleaseConcurrencySlotEarly() throws Exception {
        try (Harness harness = harness(1, 2)) {
            CountDownLatch entered = new CountDownLatch(1);
            CountDownLatch release = new CountDownLatch(1);
            AtomicInteger physicalCalls = new AtomicInteger();
            ProviderCallRequest<String> request = request("bucket-a", Duration.ofMillis(80), 0, 0, () -> {
                physicalCalls.incrementAndGet();
                entered.countDown();
                awaitIgnoringInterrupt(release);
                return ProviderAdapterResponse.ready("late", NOW);
            });

            ProviderCallResult<String> timedOut = harness.coordinator.execute(request);

            assertThat(entered.await(1, TimeUnit.SECONDS)).isTrue();
            assertThat(timedOut.metadata().errorCode()).isEqualTo("PROVIDER_TIMEOUT");
            assertThat(harness.concurrency.state().activeProviderCalls()).isEqualTo(1);
            assertThat(harness.singleFlight.activeFlightCount()).isEqualTo(1);
            assertThat(physicalCalls).hasValue(1);

            release.countDown();
            awaitState(() -> harness.concurrency.state().activeProviderCalls() == 0);
            awaitState(() -> harness.singleFlight.activeFlightCount() == 0);
        }
    }

    @Test
    void timedOutPhysicalCallBlocksDuplicatePhysicalAttempt() throws Exception {
        try (Harness harness = harness(1, 2)) {
            CountDownLatch release = new CountDownLatch(1);
            AtomicInteger physicalCalls = new AtomicInteger();
            java.util.function.Supplier<ProviderAdapterResponse<String>> adapter = () -> {
                physicalCalls.incrementAndGet();
                awaitIgnoringInterrupt(release);
                return ProviderAdapterResponse.ready("late", NOW);
            };

            harness.coordinator.execute(request("consumer-5s", Duration.ofMillis(60), 0, 0, adapter));
            ProviderCallResult<String> duplicate = harness.coordinator.execute(
                    request("consumer-30s", Duration.ofMillis(60), 0, 0, adapter));

            assertThat(duplicate.metadata().errorCode()).isEqualTo("PROVIDER_TIMEOUT");
            assertThat(physicalCalls).hasValue(1);
            assertThat(harness.singleFlight.activeFlightCount()).isEqualTo(1);
            release.countDown();
            awaitState(() -> harness.singleFlight.activeFlightCount() == 0);
        }
    }

    @Test
    void timeoutDoesNotRemoveSingleFlightWhilePhysicalCallStillRuns() {
        try (Harness harness = harness(1, 2)) {
            CountDownLatch release = new CountDownLatch(1);
            ProviderCallResult<String> result = harness.coordinator.execute(
                    request("timeout-window", Duration.ofMillis(50), 0, 0, () -> {
                        awaitIgnoringInterrupt(release);
                        return ProviderAdapterResponse.ready("late", NOW);
                    }));

            assertThat(result.metadata().errorCode()).isEqualTo("PROVIDER_TIMEOUT");
            assertThat(harness.singleFlight.activeFlightCount()).isEqualTo(1);
            release.countDown();
            awaitState(() -> harness.singleFlight.activeFlightCount() == 0);
        }
    }

    @Test
    void each5xxRetryConsumesRateBudget() {
        try (Harness harness = harness(2, 4)) {
            AtomicInteger attempts = new AtomicInteger();
            ProviderCallResult<String> result = harness.coordinator.execute(
                    request("retry-5xx", Duration.ofSeconds(2), 2, 0, () -> {
                        int attempt = attempts.incrementAndGet();
                        return attempt < 3
                                ? ProviderAdapterResponse.failed(UnifiedSourceStatus.DEGRADED, 503,
                                "PROVIDER_5XX", null)
                                : ProviderAdapterResponse.ready("ready", NOW);
                    }));

            assertThat(result.payload()).isEqualTo("ready");
            assertThat(attempts).hasValue(3);
            assertThat(harness.budget.state("TEST", ProviderCircuitState.CLOSED).regularBudgetUsage())
                    .isEqualTo(3);
            assertThat(harness.audit.snapshot().stream()
                    .filter(event -> event.phase() == ProviderCallAuditPhase.PHYSICAL_ATTEMPT_STARTED))
                    .hasSize(3)
                    .allMatch(event -> event.attemptId() != null && !event.attemptId().isBlank());
        }
    }

    @Test
    void eachTimeoutRetryConsumesRateBudget() {
        try (Harness harness = harness(2, 4)) {
            AtomicInteger attempts = new AtomicInteger();
            ProviderCallResult<String> result = harness.coordinator.execute(
                    request("retry-timeout", Duration.ofSeconds(2), 0, 1, () ->
                            attempts.incrementAndGet() == 1
                                    ? ProviderAdapterResponse.failed(UnifiedSourceStatus.ERROR, 0,
                                    "PROVIDER_TIMEOUT", null)
                                    : ProviderAdapterResponse.ready("ready", NOW)));

            assertThat(result.payload()).isEqualTo("ready");
            assertThat(attempts).hasValue(2);
            assertThat(harness.budget.state("TEST", ProviderCircuitState.CLOSED).regularBudgetUsage())
                    .isEqualTo(2);
        }
    }

    @Test
    void boundedExecutorRejectsDiscoveryBeforePosition() throws Exception {
        ProviderCallExecutor executor = new ProviderCallExecutor(1, 2, 1);
        CountDownLatch started = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        try {
            ProviderCallExecutor.TaskHandle<String> running = executor.submit(AssetPriority.P3_DISCOVERY, () -> {
                started.countDown();
                release.await();
                return "running";
            });
            assertThat(started.await(1, TimeUnit.SECONDS)).isTrue();
            ProviderCallExecutor.TaskHandle<String> queuedDiscovery =
                    executor.submit(AssetPriority.P3_DISCOVERY, () -> "queued-discovery");

            assertThatThrownBy(() -> executor.submit(AssetPriority.P3_DISCOVERY, () -> "rejected"))
                    .isInstanceOf(RejectedExecutionException.class);
            ProviderCallExecutor.TaskHandle<String> queuedPosition =
                    executor.submit(AssetPriority.P0_POSITION, () -> "queued-position");
            assertThat(executor.state().queuedCalls()).isEqualTo(2);

            release.countDown();
            assertThat(running.completion().get(1, TimeUnit.SECONDS)).isEqualTo("running");
            assertThat(queuedDiscovery.completion().get(1, TimeUnit.SECONDS)).isEqualTo("queued-discovery");
            assertThat(queuedPosition.completion().get(1, TimeUnit.SECONDS)).isEqualTo("queued-position");
        } finally {
            release.countDown();
            executor.close();
        }
    }

    @Test
    void executorQueueCannotGrowWithoutBound() throws Exception {
        ProviderCallExecutor executor = new ProviderCallExecutor(1, 1, 1);
        CountDownLatch started = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        try {
            executor.submit(AssetPriority.P0_POSITION, () -> {
                started.countDown();
                release.await();
                return "running";
            });
            assertThat(started.await(1, TimeUnit.SECONDS)).isTrue();
            executor.submit(AssetPriority.P0_POSITION, () -> "queued");
            assertThatThrownBy(() -> executor.submit(AssetPriority.P0_POSITION, () -> "overflow"))
                    .isInstanceOf(RejectedExecutionException.class);
            assertThat(executor.state().queuedCalls()).isEqualTo(1);
        } finally {
            release.countDown();
            executor.close();
        }
    }

    @Test
    void providerExecutorShutsDownCleanly() {
        ProviderCallExecutor executor = new ProviderCallExecutor(1, 1, 1);
        assertThat(executor.shutdownCleanly(Duration.ofSeconds(1))).isTrue();
        assertThat(executor.state().shutdown()).isTrue();
        assertThat(executor.state().terminated()).isTrue();
    }

    @Test
    void noCommonPoolProviderExecution() {
        try (Harness harness = harness(1, 2)) {
            AtomicReference<String> threadName = new AtomicReference<>();
            ProviderCallResult<String> result = harness.coordinator.execute(
                    request("thread-name", Duration.ofSeconds(1), 0, 0, () -> {
                        threadName.set(Thread.currentThread().getName());
                        return ProviderAdapterResponse.ready("ready", NOW);
                    }));

            assertThat(result.payload()).isEqualTo("ready");
            assertThat(threadName.get()).startsWith("provider-call-");
            assertThat(threadName.get()).doesNotContain("ForkJoinPool");
        }
    }

    private static ProviderCallRequest<String> request(
            String bucket,
            Duration timeout,
            int retry5xx,
            int retryTimeout,
            java.util.function.Supplier<ProviderAdapterResponse<String>> adapter) {
        ProviderRequestKey key = new ProviderRequestKey("TEST", ProviderDatasetType.PRICE,
                ProviderCallTestFixtures.spot("BTCUSDT"), "BTCUSDT", "GLOBAL", bucket, "TEST_V1");
        return new ProviderCallRequest<>(key, AssetPriority.P0_POSITION, UserScanProfile.AUTO,
                RuntimeScanProfile.STANDARD, List.of("TEST"), "FM-TEST", Duration.ofSeconds(5),
                Duration.ofMinutes(2), timeout, "trace-" + bucket, retry5xx, retryTimeout, adapter);
    }

    private static Harness harness(int workers, int queue) {
        ProviderCallProperties properties = new ProviderCallProperties();
        properties.setEnabled(true);
        properties.setExternalCallsEnabled(true);
        properties.setMaxConcurrentProviderCalls(workers);
        properties.setMaxQueuedCalls(queue);
        properties.setReservedPrioritySlots(workers > 1 ? 1 : 0);
        Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
        SnapshotCacheService cache = new SnapshotCacheService();
        ProviderSingleFlightGuard singleFlight = new ProviderSingleFlightGuard();
        ProviderRateBudgetManager budget = new ProviderRateBudgetManager(properties, clock);
        budget.register("TEST", 1000);
        ProviderCallAuditLog audit = new ProviderCallAuditLog();
        ProviderConcurrencyGuard concurrency = new ProviderConcurrencyGuard(properties);
        ProviderCallExecutor executor = new ProviderCallExecutor(workers, queue,
                properties.getReservedPrioritySlots());
        ProviderCallCoordinator coordinator = new ProviderCallCoordinator(properties, cache, singleFlight,
                budget, new ProviderCircuitBreaker(10, 1, clock), audit, concurrency,
                new ProviderHealthRegistry(clock), executor, new ProviderSnapshotRetentionPolicy(), clock);
        return new Harness(coordinator, singleFlight, budget, audit, concurrency, executor);
    }

    private static void awaitIgnoringInterrupt(CountDownLatch latch) {
        boolean waiting = true;
        while (waiting) {
            try {
                latch.await();
                waiting = false;
            } catch (InterruptedException ignored) {
                // Fixture deliberately models a client that does not respond to interruption.
            }
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

    private record Harness(
            ProviderCallCoordinator coordinator,
            ProviderSingleFlightGuard singleFlight,
            ProviderRateBudgetManager budget,
            ProviderCallAuditLog audit,
            ProviderConcurrencyGuard concurrency,
            ProviderCallExecutor executor
    ) implements AutoCloseable {
        @Override
        public void close() {
            executor.close();
        }
    }
}
