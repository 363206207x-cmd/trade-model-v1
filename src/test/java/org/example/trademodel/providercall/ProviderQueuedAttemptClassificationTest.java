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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;

class ProviderQueuedAttemptClassificationTest {
    private static final Instant NOW = Instant.parse("2026-07-20T12:00:00Z");
    private static final Duration SHORT_TIMEOUT = Duration.ofMillis(100);

    @Test
    void queuedAttemptTimeoutIsLocalAdmission() throws Exception {
        try (Harness harness = new Harness(new ProviderCallAuditLog())) {
            try (WorkerBlock worker = harness.occupyWorker()) {
                AtomicInteger adapterCalls = new AtomicInteger();
                ProviderCallRequest<String> ownerRequest = request(
                        "BTCUSDT", "queue-classification", SHORT_TIMEOUT, 1, () -> {
                        adapterCalls.incrementAndGet();
                        return ProviderAdapterResponse.ready("must-not-run", NOW);
                    });

                ProviderCallResult<String> result = executeWithLongWaiter(harness, ownerRequest);

                assertThat(result.metadata().errorCode()).isEqualTo("PROVIDER_EXECUTOR_QUEUE_TIMEOUT");
                assertThat(result.metadata().sourceStatus()).isEqualTo(UnifiedSourceStatus.DEGRADED);
                assertThat(ProviderFailureClassifier.classify(ProviderAdapterResponse.failed(
                        UnifiedSourceStatus.DEGRADED, 0, result.metadata().errorCode(), null)))
                        .isEqualTo(ProviderFailureOrigin.LOCAL_ADMISSION);
                assertThat(adapterCalls).hasValue(0);
                worker.releaseAndAwait();
                Thread.sleep(120L);
                assertThat(adapterCalls).hasValue(0);
            }
        }
    }

    @Test
    void queuedAttemptTimeoutDoesNotAffectProviderHealth() throws Exception {
        try (Harness harness = new Harness(new ProviderCallAuditLog())) {
            try (WorkerBlock worker = harness.occupyWorker()) {
                ProviderCallRequest<String> ownerRequest = request(
                        "ETHUSDT", "queue-health", SHORT_TIMEOUT, 1,
                        () -> ProviderAdapterResponse.ready("must-not-run", NOW));
                ProviderCallResult<String> result = executeWithLongWaiter(harness, ownerRequest);

                ProviderHealthRegistry.ProviderHealthSnapshot health = harness.health.get(
                        "TEST", harness.circuit.state("TEST"));
                assertThat(result.metadata().errorCode()).isEqualTo("PROVIDER_EXECUTOR_QUEUE_TIMEOUT");
                assertThat(health.sourceStatus()).isEqualTo(UnifiedSourceStatus.WAITING_SYNC);
                assertThat(health.lastFailureAt()).isNull();
                assertThat(health.lastReasonCode()).isNull();
                assertThat(harness.circuit.state("TEST")).isEqualTo(ProviderCircuitState.CLOSED);
            }
        }
    }

    @Test
    void queuedAttemptTimeoutDoesNotConsumeAttemptBudget() throws Exception {
        try (Harness harness = new Harness(new ProviderCallAuditLog())) {
            try (WorkerBlock worker = harness.occupyWorker()) {
                ProviderCallRequest<String> ownerRequest = request(
                        "SOLUSDT", "queue-budget", SHORT_TIMEOUT, 1,
                        () -> ProviderAdapterResponse.ready("must-not-run", NOW));
                ProviderCallResult<String> result = executeWithLongWaiter(harness, ownerRequest);

                assertThat(result.metadata().errorCode()).isEqualTo("PROVIDER_EXECUTOR_QUEUE_TIMEOUT");
                assertThat(harness.budget.attemptReservations).hasValue(0);
                assertThat(harness.auditEvents()).noneMatch(event ->
                        event.phase() == ProviderCallAuditPhase.PHYSICAL_ATTEMPT_STARTED);
            }
        }
    }

    @Test
    void queuedAttemptTimeoutReleasesHalfOpenProbe() throws Exception {
        try (Harness harness = new Harness(new ProviderCallAuditLog())) {
            ProviderCallResult<String> openingFailure = harness.coordinator.execute(request(
                    "BTCUSDT", "open-circuit", Duration.ofSeconds(1), 0,
                    () -> ProviderAdapterResponse.failed(UnifiedSourceStatus.DEGRADED, 503,
                            "PROVIDER_5XX", null)));
            assertThat(openingFailure.metadata().errorCode()).isEqualTo("PROVIDER_5XX");
            assertThat(harness.circuit.state("TEST")).isEqualTo(ProviderCircuitState.OPEN);
            harness.clock.advance(Duration.ofSeconds(6));

            ProviderHealthRegistry.ProviderHealthSnapshot before = harness.health.get(
                    "TEST", harness.circuit.state("TEST"));
            AtomicInteger queuedAdapterCalls = new AtomicInteger();
            try (WorkerBlock worker = harness.occupyWorker()) {
                ProviderCallRequest<String> ownerRequest = request(
                        "ETHUSDT", "half-open-queue", SHORT_TIMEOUT, 1, () -> {
                        queuedAdapterCalls.incrementAndGet();
                        return ProviderAdapterResponse.ready("must-not-run", NOW);
                    });
                ProviderCallResult<String> queued = executeWithLongWaiter(harness, ownerRequest);

                assertThat(queued.metadata().errorCode()).isEqualTo("PROVIDER_EXECUTOR_QUEUE_TIMEOUT");
                assertThat(queuedAdapterCalls).hasValue(0);
                assertThat(harness.circuit.state("TEST")).isEqualTo(ProviderCircuitState.HALF_OPEN);
                assertThat(harness.circuit.halfOpenProbeClaimed("TEST")).isFalse();
                ProviderHealthRegistry.ProviderHealthSnapshot after = harness.health.get(
                        "TEST", harness.circuit.state("TEST"));
                assertThat(after.lastFailureAt()).isEqualTo(before.lastFailureAt());
                assertThat(after.lastReasonCode()).isEqualTo(before.lastReasonCode());

                worker.releaseAndAwait();
            }
            ProviderCallResult<String> recovery = harness.coordinator.execute(request(
                    "SOLUSDT", "half-open-recovery", Duration.ofSeconds(1), 0,
                    () -> ProviderAdapterResponse.ready("recovered", NOW)));
            assertThat(recovery.payload()).isEqualTo("recovered");
            assertThat(harness.circuit.state("TEST")).isEqualTo(ProviderCircuitState.CLOSED);
        }
    }

    @Test
    void queuedAttemptTimeoutCompletesSingleFlight() throws Exception {
        try (Harness harness = new Harness(new ProviderCallAuditLog())) {
            AtomicInteger adapterCalls = new AtomicInteger();
            try (WorkerBlock worker = harness.occupyWorker()) {
                ProviderCallRequest<String> first = request(
                        "BNBUSDT", "queue-flight", SHORT_TIMEOUT, 1, () -> {
                            adapterCalls.incrementAndGet();
                            return ProviderAdapterResponse.ready("must-not-run", NOW);
                        });

                ProviderCallResult<String> timedOut = executeWithLongWaiter(harness, first);
                assertThat(timedOut.metadata().errorCode()).isEqualTo("PROVIDER_EXECUTOR_QUEUE_TIMEOUT");
                assertThat(harness.singleFlight.activeFlightCount()).isZero();
                worker.releaseAndAwait();
            }

            ProviderCallResult<String> retry = harness.coordinator.execute(request(
                    "BNBUSDT", "queue-flight", Duration.ofSeconds(1), 0, () -> {
                        adapterCalls.incrementAndGet();
                        return ProviderAdapterResponse.ready("later-success", NOW);
                    }));
            assertThat(retry.payload()).isEqualTo("later-success");
            assertThat(adapterCalls).hasValue(1);
            assertThat(harness.singleFlight.activeFlightCount()).isZero();
        }
    }

    @Test
    void preRemoteTimeoutCannotCallAdapter() {
        BlockingAttemptStartAudit audit = new BlockingAttemptStartAudit();
        try (Harness harness = new Harness(audit)) {
            AtomicInteger adapterCalls = new AtomicInteger();
            ProviderCallRequest<String> ownerRequest = request(
                    "XRPUSDT", "pre-remote", SHORT_TIMEOUT, 1, () -> {
                        adapterCalls.incrementAndGet();
                        return ProviderAdapterResponse.ready("must-not-run", NOW);
                    });
            CompletableFuture<ProviderCallResult<String>> owner = CompletableFuture.supplyAsync(() ->
                    harness.coordinator.execute(ownerRequest));
            assertThat(audit.awaitAttemptStart()).isTrue();
            ProviderCallResult<String> result = harness.coordinator.execute(withTimeout(
                    ownerRequest, Duration.ofSeconds(1)));
            owner.join();

            assertThat(result.metadata().errorCode()).isEqualTo("PROVIDER_PRE_REMOTE_TIMEOUT");
            assertThat(result.metadata().sourceStatus()).isEqualTo(UnifiedSourceStatus.DEGRADED);
            assertThat(adapterCalls).hasValue(0);
            assertThat(harness.budget.attemptReservations).hasValue(1);
            assertThat(harness.health.get("TEST", ProviderCircuitState.CLOSED).lastFailureAt()).isNull();
            assertThat(harness.circuit.state("TEST")).isEqualTo(ProviderCircuitState.CLOSED);
            assertThat(audit.snapshot()).filteredOn(event ->
                    event.phase() == ProviderCallAuditPhase.PHYSICAL_ATTEMPT_STARTED).hasSize(1);
            assertThat(audit.snapshot()).filteredOn(event ->
                    event.phase() == ProviderCallAuditPhase.PHYSICAL_ATTEMPT_COMPLETED
                            && "PROVIDER_PRE_REMOTE_TIMEOUT".equals(event.reasonCode())).hasSize(1);
        }
    }

    private static ProviderCallResult<String> executeWithLongWaiter(
            Harness harness,
            ProviderCallRequest<String> ownerRequest) {
        CompletableFuture<ProviderCallResult<String>> owner = CompletableFuture.supplyAsync(() ->
                harness.coordinator.execute(ownerRequest));
        awaitState(() -> harness.executor.state().queuedCalls() == 1);
        ProviderCallResult<String> result = harness.coordinator.execute(withTimeout(
                ownerRequest, Duration.ofSeconds(1)));
        owner.join();
        return result;
    }

    private static ProviderCallRequest<String> withTimeout(
            ProviderCallRequest<String> request,
            Duration timeout) {
        return new ProviderCallRequest<>(request.key(), request.priority(), request.baseProfile(),
                request.effectiveProfile(), request.profileReasonCodes(), request.frequencyMatrixVersion(),
                request.freshTtl(), request.staleTtl(), timeout, request.traceId() + "-waiter",
                request.maxRetry5xx(), request.maxRetryTimeout(), request.adapterCall());
    }

    @Test
    void timeoutRaceHasExactlyOneWinner() throws Exception {
        ExecutorService racers = Executors.newFixedThreadPool(2);
        try {
            for (int iteration = 0; iteration < 100; iteration++) {
                ProviderCallCoordinator.AttemptExecutionState state =
                        new ProviderCallCoordinator.AttemptExecutionState();
                assertThat(state.beginLocalAdmission()).isTrue();
                CountDownLatch start = new CountDownLatch(1);
                CountDownLatch timeoutSettled = new CountDownLatch(1);
                AtomicInteger adapterCalls = new AtomicInteger();

                Future<Boolean> adapterStarter = racers.submit(() -> {
                    await(start);
                    boolean remoteWon = state.beginRemoteAttempt();
                    if (remoteWon) {
                        adapterCalls.incrementAndGet();
                        await(timeoutSettled);
                    }
                    return remoteWon;
                });
                Future<ProviderCallCoordinator.AttemptExecutionPhase> timeout = racers.submit(() -> {
                    await(start);
                    ProviderCallCoordinator.AttemptExecutionPhase phase = state.requestTimeout();
                    timeoutSettled.countDown();
                    return phase;
                });

                start.countDown();
                boolean remoteWon = adapterStarter.get(1, TimeUnit.SECONDS);
                ProviderCallCoordinator.AttemptExecutionPhase timeoutPhase = timeout.get(1, TimeUnit.SECONDS);
                if (remoteWon) {
                    assertThat(timeoutPhase)
                            .isEqualTo(ProviderCallCoordinator.AttemptExecutionPhase.REMOTE_TIMEOUT_REQUESTED);
                    assertThat(state.remoteTimeoutRequested()).isTrue();
                    assertThat(adapterCalls).hasValue(1);
                } else {
                    assertThat(timeoutPhase)
                            .isEqualTo(ProviderCallCoordinator.AttemptExecutionPhase.CANCELLED_BEFORE_REMOTE);
                    assertThat(state.cancelledBeforeRemote()).isTrue();
                    assertThat(state.localTimeoutReason()).isEqualTo("PROVIDER_PRE_REMOTE_TIMEOUT");
                    assertThat(adapterCalls).hasValue(0);
                }
                assertThat(state.requestTimeout()).isNull();
            }
        } finally {
            racers.shutdownNow();
        }
    }

    private static ProviderCallRequest<String> request(
            String symbol,
            String bucket,
            Duration timeout,
            int maxRetryTimeout,
            Supplier<ProviderAdapterResponse<String>> adapter) {
        ProviderRequestKey key = new ProviderRequestKey("TEST", ProviderDatasetType.PRICE,
                ProviderCallTestFixtures.spot(symbol), symbol, "GLOBAL", bucket, "TEST_V1");
        return new ProviderCallRequest<>(key, AssetPriority.P0_POSITION, UserScanProfile.AUTO,
                RuntimeScanProfile.STANDARD, List.of("TEST"), "FM-TEST", Duration.ofSeconds(5),
                Duration.ofMinutes(2), timeout, "trace-" + bucket, 0, maxRetryTimeout, adapter);
    }

    private static void await(CountDownLatch latch) {
        try {
            latch.await();
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
        }
    }

    private static void awaitIgnoringInterrupt(CountDownLatch latch) {
        boolean waiting = true;
        while (waiting) {
            try {
                latch.await();
                waiting = false;
            } catch (InterruptedException ignored) {
                // The worker fixture deliberately remains occupied until the test releases it.
            }
        }
    }

    private static void awaitState(java.util.function.BooleanSupplier condition) {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(2);
        while (!condition.getAsBoolean() && System.nanoTime() < deadline) {
            try {
                Thread.sleep(5L);
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
                throw new AssertionError(interrupted);
            }
        }
        assertThat(condition.getAsBoolean()).isTrue();
    }

    private static final class Harness implements AutoCloseable {
        private final MutableClock clock = new MutableClock(NOW);
        private final CountingBudget budget = new CountingBudget();
        private final ProviderCircuitBreaker circuit = new ProviderCircuitBreaker(1, 5, clock);
        private final ProviderSingleFlightGuard singleFlight = new ProviderSingleFlightGuard();
        private final ProviderHealthRegistry health = new ProviderHealthRegistry(clock);
        private final ProviderCallExecutor executor = new ProviderCallExecutor(1, 8, 1);
        private final ProviderCallAuditService audit;
        private final ProviderCallCoordinator coordinator;

        private Harness(ProviderCallAuditService audit) {
            this.audit = audit;
            ProviderCallProperties properties = new ProviderCallProperties();
            properties.setEnabled(true);
            properties.setExternalCallsEnabled(true);
            properties.setMaxConcurrentProviderCalls(1);
            properties.setMaxQueuedCalls(8);
            properties.setReservedPrioritySlots(0);
            coordinator = new ProviderCallCoordinator(properties, new SnapshotCacheService(), singleFlight,
                    budget, circuit, audit, new ProviderConcurrencyGuard(properties), health, executor,
                    new ProviderSnapshotRetentionPolicy(), clock);
        }

        private WorkerBlock occupyWorker() throws Exception {
            CountDownLatch entered = new CountDownLatch(1);
            CountDownLatch release = new CountDownLatch(1);
            ProviderCallExecutor.TaskHandle<Void> handle = executor.submit(AssetPriority.P0_POSITION, () -> {
                entered.countDown();
                awaitIgnoringInterrupt(release);
                return null;
            });
            assertThat(entered.await(1, TimeUnit.SECONDS)).isTrue();
            return new WorkerBlock(release, handle.completion());
        }

        private List<ProviderCallAuditEvent> auditEvents() {
            return audit.snapshot();
        }

        @Override
        public void close() {
            executor.close();
        }
    }

    private record WorkerBlock(CountDownLatch release, CompletableFuture<Void> completion)
            implements AutoCloseable {
        private void releaseAndAwait() throws Exception {
            release.countDown();
            completion.get(1, TimeUnit.SECONDS);
        }

        @Override
        public void close() {
            release.countDown();
            try {
                completion.get(1, TimeUnit.SECONDS);
            } catch (Exception ignored) {
                // Best-effort fixture cleanup; assertions report the primary failure.
            }
        }
    }

    private static final class CountingBudget implements ProviderRateBudget {
        private final AtomicInteger attemptReservations = new AtomicInteger();

        @Override
        public boolean reserve(ProviderRequestKey key, AssetPriority priority, RuntimeScanProfile profile) {
            attemptReservations.incrementAndGet();
            return true;
        }

        @Override
        public void applyRetryAfter(String provider, long seconds) {
            // No-op fixture.
        }

        @Override
        public ProviderBudgetState state(String provider, ProviderCircuitState circuitState) {
            int usage = attemptReservations.get();
            return new ProviderBudgetState(provider, 1000, 800, 0.8d, 0.2d,
                    usage, Math.max(0, 800 - usage), null, circuitState, null,
                    0, 0, 0, null);
        }
    }

    private static final class BlockingAttemptStartAudit implements ProviderCallAuditService {
        private final ProviderCallAuditLog delegate = new ProviderCallAuditLog();
        private final AtomicBoolean blocked = new AtomicBoolean();
        private final CountDownLatch attemptStarted = new CountDownLatch(1);

        @Override
        public void record(ProviderCallAuditEvent event) {
            delegate.record(event);
            if (event.phase() == ProviderCallAuditPhase.PHYSICAL_ATTEMPT_STARTED
                    && blocked.compareAndSet(false, true)) {
                attemptStarted.countDown();
                CountDownLatch neverReleased = new CountDownLatch(1);
                await(neverReleased);
            }
        }

        private boolean awaitAttemptStart() {
            try {
                return attemptStarted.await(1, TimeUnit.SECONDS);
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
                return false;
            }
        }

        @Override
        public List<ProviderCallAuditEvent> snapshot() {
            return delegate.snapshot();
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
