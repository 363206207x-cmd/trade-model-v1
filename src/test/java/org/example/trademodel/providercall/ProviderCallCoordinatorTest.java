package org.example.trademodel.providercall;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class ProviderCallCoordinatorTest {

    @Test
    void sameProviderRequestKeyUsesSingleFlight() throws Exception {
        ProviderSingleFlightGuard guard = new ProviderSingleFlightGuard();
        ProviderRequestKey key = key(ProviderDatasetType.PRICE);
        CountDownLatch ownerStarted = new CountDownLatch(1);
        CountDownLatch releaseOwner = new CountDownLatch(1);
        AtomicInteger calls = new AtomicInteger();
        ExecutorService pool = Executors.newFixedThreadPool(3);
        try {
            Future<String> first = pool.submit(() -> guard.execute(key, () -> {
                calls.incrementAndGet();
                ownerStarted.countDown();
                await(releaseOwner);
                return "snapshot";
            }));
            ownerStarted.await();
            Future<String> second = pool.submit(() -> guard.execute(key, () -> {
                calls.incrementAndGet();
                return "duplicate";
            }));
            Future<?> releaser = pool.submit(() -> {
                while (guard.waitingCallerCount() == 0) Thread.onSpinWait();
                releaseOwner.countDown();
            });
            assertThat(first.get()).isEqualTo("snapshot");
            assertThat(second.get()).isEqualTo("snapshot");
            releaser.get();
            assertThat(calls).hasValue(1);
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    void derivativesSnapshotIsNotRefetchedInsideFreshnessWindow() {
        TestContext context = context(Instant.parse("2026-07-10T10:00:00Z"), 10);
        AtomicInteger calls = new AtomicInteger();
        ProviderCallRequest<String> request = request(key(ProviderDatasetType.DERIVATIVES), AssetPriority.P1_CORE,
                () -> ProviderAdapterResponse.ready("oi", context.clock.instant()), calls);

        assertThat(context.coordinator.execute(request).payload()).isEqualTo("oi");
        assertThat(context.coordinator.execute(request).metadata().cacheHit()).isTrue();
        assertThat(calls).hasValue(1);
    }

    @Test
    void decisionDashboardMonitorShareOneSnapshot() {
        TestContext context = context(Instant.parse("2026-07-10T10:00:00Z"), 10);
        AtomicInteger calls = new AtomicInteger();
        ProviderCallRequest<String> request = request(key(ProviderDatasetType.PRICE), AssetPriority.P0_POSITION,
                () -> ProviderAdapterResponse.ready("65000", context.clock.instant()), calls);

        String decision = context.coordinator.execute(request).payload();
        String dashboard = context.coordinator.execute(request).payload();
        String monitor = context.coordinator.execute(request).payload();

        assertThat(decision).isEqualTo(dashboard).isEqualTo(monitor);
        assertThat(calls).hasValue(1);
    }

    @Test
    void rateBudgetPreservesPositionPriority() {
        ProviderCallProperties properties = enabledProperties();
        properties.setInternalBudgetRatio(0.8);
        ProviderRateBudgetManager manager = new ProviderRateBudgetManager(properties,
                Clock.fixed(Instant.parse("2026-07-10T10:00:00Z"), ZoneOffset.UTC));
        manager.register("TEST", 10);
        for (int i = 0; i < 8; i++) assertThat(manager.reserve("TEST", AssetPriority.P1_CORE)).isTrue();
        assertThat(manager.reserve("TEST", AssetPriority.P1_CORE)).isFalse();
        assertThat(manager.reserve("TEST", AssetPriority.P0_POSITION)).isTrue();
    }

    @Test
    void rateBudgetDropsPoolFirst() {
        ProviderCallProperties properties = enabledProperties();
        ProviderRateBudgetManager manager = new ProviderRateBudgetManager(properties,
                Clock.fixed(Instant.parse("2026-07-10T10:00:00Z"), ZoneOffset.UTC));
        manager.register("TEST", 10);
        for (int i = 0; i < 4; i++) assertThat(manager.reserve("TEST", AssetPriority.P3_POOL)).isTrue();
        assertThat(manager.reserve("TEST", AssetPriority.P3_POOL)).isFalse();
        assertThat(manager.reserve("TEST", AssetPriority.P2_CANDIDATE)).isTrue();
        assertThat(manager.reserve("TEST", AssetPriority.P0_POSITION)).isTrue();
    }

    @Test
    void providerBudgetFailureDoesNotStopPositionPriceMonitoring() {
        ProviderCallProperties properties = enabledProperties();
        ProviderRateBudgetManager manager = new ProviderRateBudgetManager(properties,
                Clock.fixed(Instant.parse("2026-07-10T10:00:00Z"), ZoneOffset.UTC));
        manager.register("COINGLASS", 5);
        while (manager.reserve("COINGLASS", AssetPriority.P1_CORE)) { }
        manager.register("BINANCE_PUBLIC", 5);
        assertThat(manager.reserve("BINANCE_PUBLIC", AssetPriority.P0_POSITION)).isTrue();
    }

    @Test
    void provider429RespectsRetryAfterWithoutBusyLoop() {
        TestContext context = context(Instant.parse("2026-07-10T10:00:00Z"), 10);
        AtomicInteger calls = new AtomicInteger();
        ProviderCallRequest<String> request = request(key(ProviderDatasetType.EXTERNAL_CONTEXT), AssetPriority.P2_CANDIDATE,
                () -> ProviderAdapterResponse.failed(UnifiedSourceStatus.DEGRADED, 429, "RATE_LIMITED", 30L), calls);

        ProviderCallResult<String> first = context.coordinator.execute(request);
        ProviderCallResult<String> second = context.coordinator.execute(request);

        assertThat(first.metadata().errorCode()).isEqualTo("RATE_LIMITED");
        assertThat(second.metadata().errorCode()).isEqualTo("PROVIDER_BUDGET_REJECTED");
        assertThat(calls).hasValue(1);
        assertThat(second.budgetState().retryAfter()).isAfter(context.clock.instant());
    }

    @Test
    void staleSnapshotIsExplicitlyStale() {
        TestContext context = context(Instant.parse("2026-07-10T10:00:00Z"), 10);
        AtomicInteger calls = new AtomicInteger();
        ProviderCallRequest<String> ready = request(key(ProviderDatasetType.PRICE), AssetPriority.P0_POSITION,
                () -> ProviderAdapterResponse.ready("65000", context.clock.instant()), calls);
        context.coordinator.execute(ready);
        context.clock.advance(Duration.ofSeconds(11));
        context.properties.setExternalCallsEnabled(false);

        ProviderCallResult<String> stale = context.coordinator.execute(ready);

        assertThat(stale.payload()).isEqualTo("65000");
        assertThat(stale.metadata().sourceStatus()).isEqualTo(UnifiedSourceStatus.STALE);
        assertThat(stale.metadata().freshnessStatus()).isEqualTo(SnapshotFreshnessStatus.STALE);
        assertThat(stale.metadata().fallbackUsed()).isTrue();
    }

    @Test
    void missingProviderNeverBecomesHealthyOrLowRisk() {
        TestContext context = context(Instant.parse("2026-07-10T10:00:00Z"), 10);
        context.properties.setExternalCallsEnabled(false);
        ProviderCallResult<String> result = context.coordinator.execute(request(key(ProviderDatasetType.DERIVATIVES),
                AssetPriority.P1_CORE, () -> ProviderAdapterResponse.ready("not-called", context.clock.instant()),
                new AtomicInteger()));

        assertThat(result.payload()).isNull();
        assertThat(result.metadata().sourceStatus()).isEqualTo(UnifiedSourceStatus.DISABLED);
        assertThat(result.metadata().sourceStatus()).isNotEqualTo(UnifiedSourceStatus.READY);
    }

    @Test
    void providerErrorReturnsFailClosedMetadata() {
        TestContext context = context(Instant.parse("2026-07-10T10:00:00Z"), 10);
        ProviderCallResult<String> result = context.coordinator.execute(request(key(ProviderDatasetType.PRICE),
                AssetPriority.P0_POSITION,
                () -> ProviderAdapterResponse.failed(UnifiedSourceStatus.ERROR, 403, "PROVIDER_FORBIDDEN", null),
                new AtomicInteger()));

        assertThat(result.payload()).isNull();
        assertThat(result.metadata().freshnessStatus()).isEqualTo(SnapshotFreshnessStatus.ERROR);
        assertThat(result.metadata().errorCode()).isEqualTo("PROVIDER_FORBIDDEN");
    }

    @Test
    void eventRefreshUsesCacheInsideMinimumGap() {
        TestContext context = context(Instant.parse("2026-07-10T10:00:00Z"), 40);
        AtomicInteger calls = new AtomicInteger();
        ProviderCallRequest<String> request = request(key(ProviderDatasetType.EXTERNAL_CONTEXT), AssetPriority.P1_CORE,
                () -> ProviderAdapterResponse.ready("event", context.clock.instant()), calls, Duration.ofSeconds(40));
        context.coordinator.execute(request);
        context.clock.advance(Duration.ofSeconds(39));
        assertThat(context.coordinator.execute(request).metadata().cacheHit()).isTrue();
        assertThat(calls).hasValue(1);
    }

    private static ProviderCallRequest<String> request(ProviderRequestKey key, AssetPriority priority,
                                                        java.util.function.Supplier<ProviderAdapterResponse<String>> call,
                                                        AtomicInteger counter) {
        return request(key, priority, call, counter, Duration.ofSeconds(10));
    }

    private static ProviderCallRequest<String> request(ProviderRequestKey key, AssetPriority priority,
                                                        java.util.function.Supplier<ProviderAdapterResponse<String>> call,
                                                        AtomicInteger counter, Duration ttl) {
        return new ProviderCallRequest<>(key, priority, ttl, Duration.ofMinutes(2), Duration.ofSeconds(1), "trace-1", () -> {
            counter.incrementAndGet();
            return call.get();
        });
    }

    private static ProviderRequestKey key(ProviderDatasetType type) {
        return new ProviderRequestKey("TEST", type, "BTCUSDT", "GLOBAL", "202607101000");
    }

    private static TestContext context(Instant now, int advertisedRpm) {
        ProviderCallProperties properties = enabledProperties();
        MutableClock clock = new MutableClock(now);
        SnapshotCacheService cache = new SnapshotCacheService();
        ProviderSingleFlightGuard guard = new ProviderSingleFlightGuard();
        ProviderRateBudgetManager budget = new ProviderRateBudgetManager(properties, clock);
        budget.register("TEST", advertisedRpm);
        ProviderCircuitBreaker circuit = new ProviderCircuitBreaker(3, 60, clock);
        ProviderCallAuditLog audit = new ProviderCallAuditLog();
        return new TestContext(properties, clock,
                new ProviderCallCoordinator(properties, cache, guard, budget, circuit, audit, clock));
    }

    private static ProviderCallProperties enabledProperties() {
        ProviderCallProperties properties = new ProviderCallProperties();
        properties.setEnabled(true);
        properties.setExternalCallsEnabled(true);
        return properties;
    }

    private static void await(CountDownLatch latch) {
        try { latch.await(); }
        catch (InterruptedException ex) { Thread.currentThread().interrupt(); throw new IllegalStateException(ex); }
    }

    private record TestContext(ProviderCallProperties properties, MutableClock clock,
                               ProviderCallCoordinator coordinator) {}

    private static final class MutableClock extends Clock {
        private Instant instant;
        private MutableClock(Instant instant) { this.instant = instant; }
        void advance(Duration duration) { instant = instant.plus(duration); }
        @Override public ZoneId getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(ZoneId zone) { return this; }
        @Override public Instant instant() { return instant; }
    }
}
