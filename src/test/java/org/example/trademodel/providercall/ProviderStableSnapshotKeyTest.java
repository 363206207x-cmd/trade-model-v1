package org.example.trademodel.providercall;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProviderStableSnapshotKeyTest {
    private static final Instant NOW = Instant.parse("2026-07-19T11:00:00Z");

    @Test
    void position5sAndDashboard30sShareOneSnapshot() {
        try (Harness harness = harness()) {
            AtomicInteger calls = new AtomicInteger();
            ProviderCallResult<String> position = harness.coordinator.execute(request("position-bucket", 5,
                    calls, () -> ProviderAdapterResponse.ready("65000", harness.clock.instant())));
            ProviderCallResult<String> dashboard = harness.coordinator.execute(request("dashboard-bucket", 30,
                    calls, () -> ProviderAdapterResponse.ready("duplicate", harness.clock.instant())));

            assertThat(position.payload()).isEqualTo("65000");
            assertThat(dashboard.payload()).isEqualTo("65000");
            assertThat(dashboard.metadata().cacheHit()).isTrue();
            assertThat(calls).hasValue(1);
            assertThat(harness.cache.entryCount()).isEqualTo(1);
        }
    }

    @Test
    void differentConsumerTtlsShareStableSnapshotKey() {
        ProviderRequestKeyFactory factory = new ProviderRequestKeyFactory(
                ProviderCallTestFixtures.binanceRegistry("BTCUSDT"));
        ProviderRequestKey fiveSeconds = factory.create("BINANCE", ProviderDatasetType.PRICE,
                ProviderCallTestFixtures.spot("BTCUSDT"), "GLOBAL", Duration.ofSeconds(5), NOW);
        ProviderRequestKey thirtySeconds = factory.create("BINANCE", ProviderDatasetType.PRICE,
                ProviderCallTestFixtures.spot("BTCUSDT"), "GLOBAL", Duration.ofSeconds(30), NOW);

        assertThat(fiveSeconds.timeBucket()).isNotEqualTo(thirtySeconds.timeBucket());
        assertThat(fiveSeconds.snapshotKey()).isEqualTo(thirtySeconds.snapshotKey());
    }

    @Test
    void crossTimeBucketProviderFailureReturnsPreviousStaleSnapshot() {
        try (Harness harness = harness()) {
            AtomicInteger calls = new AtomicInteger();
            harness.coordinator.execute(request("bucket-one", 5, calls,
                    () -> ProviderAdapterResponse.ready("65000", harness.clock.instant())));
            harness.clock.advance(Duration.ofSeconds(6));

            ProviderCallResult<String> fallback = harness.coordinator.execute(request("bucket-two", 5, calls,
                    () -> ProviderAdapterResponse.failed(UnifiedSourceStatus.ERROR, 503,
                            "PROVIDER_UNAVAILABLE", null)));

            assertThat(fallback.payload()).isEqualTo("65000");
            assertThat(fallback.metadata().freshnessStatus()).isEqualTo(SnapshotFreshnessStatus.STALE_READABLE);
            assertThat(fallback.metadata().fallbackUsed()).isTrue();
            assertThat(calls).hasValue(2);
            assertThat(harness.cache.entryCount()).isEqualTo(1);
        }
    }

    @Test
    void crossTimeBucketDoesNotCreateUnboundedEntries() {
        try (Harness harness = harness()) {
            AtomicInteger calls = new AtomicInteger();
            for (int i = 0; i < 20; i++) {
                harness.coordinator.execute(request("bucket-" + i, 1, calls,
                        () -> ProviderAdapterResponse.ready("price-" + calls.get(), harness.clock.instant())));
                harness.clock.advance(Duration.ofSeconds(2));
            }
            assertThat(harness.cache.entryCount()).isEqualTo(1);
        }
    }

    @Test
    void stricterConsumerCanRefreshSharedSnapshot() {
        try (Harness harness = harness()) {
            AtomicInteger calls = new AtomicInteger();
            harness.coordinator.execute(request("dashboard", 30, calls,
                    () -> ProviderAdapterResponse.ready("65000", harness.clock.instant())));
            harness.clock.advance(Duration.ofSeconds(6));

            ProviderCallResult<String> position = harness.coordinator.execute(request("position", 5, calls,
                    () -> ProviderAdapterResponse.ready("65001", harness.clock.instant())));

            assertThat(position.payload()).isEqualTo("65001");
            assertThat(calls).hasValue(2);
            assertThat(harness.cache.entryCount()).isEqualTo(1);
        }
    }

    @Test
    void dashboardQueryCanReadPositionRefreshedSnapshot() {
        try (Harness harness = harness()) {
            AtomicInteger calls = new AtomicInteger();
            ProviderCallRequest<String> position = request("position-refresh", 5, calls,
                    () -> ProviderAdapterResponse.ready("65000", harness.clock.instant()));
            harness.coordinator.execute(position);
            ProviderRequestKey dashboardKey = key("dashboard-query");

            ProviderCallResult<String> dashboard = harness.coordinator.peek(dashboardKey,
                    AssetPriority.P1_WATCHLIST, Duration.ofSeconds(30), "dashboard-trace");

            assertThat(dashboard.payload()).isEqualTo("65000");
            assertThat(dashboard.metadata().cacheHit()).isTrue();
            assertThat(calls).hasValue(1);
        }
    }

    @Test
    void singleFlightWorksAcrossDifferentConsumerTtls() throws Exception {
        try (Harness harness = harness()) {
            AtomicInteger calls = new AtomicInteger();
            CountDownLatch started = new CountDownLatch(1);
            CountDownLatch release = new CountDownLatch(1);
            ExecutorService callers = Executors.newFixedThreadPool(2);
            try {
                Future<ProviderCallResult<String>> position = callers.submit(() -> harness.coordinator.execute(
                        request("position-flight", 5, calls, () -> {
                            started.countDown();
                            await(release);
                            return ProviderAdapterResponse.ready("shared", harness.clock.instant());
                        })));
                assertThat(started.await(1, TimeUnit.SECONDS)).isTrue();
                Future<ProviderCallResult<String>> dashboard = callers.submit(() -> harness.coordinator.execute(
                        request("dashboard-flight", 30, calls,
                                () -> ProviderAdapterResponse.ready("duplicate", harness.clock.instant()))));
                release.countDown();

                assertThat(position.get(2, TimeUnit.SECONDS).payload()).isEqualTo("shared");
                assertThat(dashboard.get(2, TimeUnit.SECONDS).payload()).isEqualTo("shared");
                assertThat(calls).hasValue(1);
            } finally {
                release.countDown();
                callers.shutdownNow();
            }
        }
    }

    @Test
    void expiredStableSnapshotIsEventuallyRemoved() {
        try (Harness harness = harness()) {
            AtomicInteger calls = new AtomicInteger();
            ProviderCallRequest<String> request = request("expiry", 5, calls,
                    () -> ProviderAdapterResponse.ready("65000", harness.clock.instant()));
            harness.coordinator.execute(request);
            harness.clock.advance(Duration.ofSeconds(121));

            SnapshotCacheService.SnapshotLookup<String> lookup = harness.cache.lookup(
                    request.key().snapshotKey(), harness.clock.instant(), Duration.ofSeconds(5));

            assertThat(lookup.fresh()).isFalse();
            assertThat(lookup.staleReadable()).isFalse();
            assertThat(harness.cache.entryCount()).isZero();
        }
    }

    @Test
    void longRequestedTtlCannotOutliveDatasetRetention() {
        SnapshotCacheService cache = cacheWithSnapshot("retention-hard-bound", "old", NOW,
                NOW.plusSeconds(5), Duration.ofSeconds(120), UnifiedSourceStatus.READY);

        SnapshotCacheService.SnapshotLookup<String> lookup = cache.lookup(
                key("retention-hard-bound").snapshotKey(), NOW.plusSeconds(121), Duration.ofSeconds(900));

        assertThat(lookup.fresh()).isFalse();
        assertThat(lookup.staleReadable()).isFalse();
        assertThat(cache.entryCount()).isZero();
    }

    @Test
    void requestedTtlIsCappedAtRetentionBoundary() {
        ProviderSnapshotKey snapshotKey = key("retention-boundary").snapshotKey();
        SnapshotCacheService cache = cacheWithSnapshot("retention-boundary", "price", NOW,
                NOW.plusSeconds(5), Duration.ofSeconds(120), UnifiedSourceStatus.READY);

        SnapshotCacheService.SnapshotLookup<String> beforeBoundary = cache.lookup(
                snapshotKey, NOW.plusSeconds(119), Duration.ofSeconds(900));
        SnapshotCacheService.SnapshotLookup<String> atBoundary = cache.lookup(
                snapshotKey, NOW.plusSeconds(120), Duration.ofSeconds(900));

        assertThat(beforeBoundary.fresh()).isTrue();
        assertThat(atBoundary.fresh()).isFalse();
        assertThat(atBoundary.staleReadable()).isFalse();
        assertThat(cache.entryCount()).isZero();
    }

    @Test
    void shortRequestedTtlBecomesStaleBeforeRetention() {
        SnapshotCacheService cache = cacheWithSnapshot("short-consumer", "price", NOW,
                NOW.plusSeconds(30), Duration.ofSeconds(120), UnifiedSourceStatus.READY);

        SnapshotCacheService.SnapshotLookup<String> lookup = cache.lookup(
                key("short-consumer").snapshotKey(), NOW.plusSeconds(6), Duration.ofSeconds(5));

        assertThat(lookup.fresh()).isFalse();
        assertThat(lookup.staleReadable()).isTrue();
        assertThat(cache.entryCount()).isEqualTo(1);
    }

    @Test
    void longerConsumerCanReuseSnapshotWithinRetention() {
        try (Harness harness = harness()) {
            AtomicInteger calls = new AtomicInteger();
            harness.coordinator.execute(request("position-five-seconds", 5, calls,
                    () -> ProviderAdapterResponse.ready("65000", harness.clock.instant())));
            harness.clock.advance(Duration.ofSeconds(10));

            ProviderCallResult<String> dashboard = harness.coordinator.execute(request(
                    "dashboard-thirty-seconds", 30, calls,
                    () -> ProviderAdapterResponse.ready("must-not-run", harness.clock.instant())));

            assertThat(dashboard.payload()).isEqualTo("65000");
            assertThat(dashboard.metadata().freshnessStatus()).isEqualTo(SnapshotFreshnessStatus.FRESH);
            assertThat(dashboard.metadata().cacheHit()).isTrue();
            assertThat(calls).hasValue(1);
            assertThat(harness.cache.entryCount()).isEqualTo(1);
        }
    }

    @Test
    void metadataExpiryCannotOutliveRetention() {
        ProviderSnapshotKey snapshotKey = key("metadata-expiry").snapshotKey();
        SnapshotCacheService cache = cacheWithSnapshot("metadata-expiry", "price", NOW,
                NOW.plusSeconds(900), Duration.ofSeconds(120), UnifiedSourceStatus.READY);

        assertThat(cache.lookup(snapshotKey, NOW.plusSeconds(119)).fresh()).isTrue();
        SnapshotCacheService.SnapshotLookup<String> atBoundary = cache.lookup(
                snapshotKey, NOW.plusSeconds(120));

        assertThat(atBoundary.fresh()).isFalse();
        assertThat(atBoundary.staleReadable()).isFalse();
        assertThat(cache.entryCount()).isZero();
    }

    @Test
    void lookupAndPurgeExpiredUseSameBoundary() {
        for (long offsetNanos : List.of(-1L, 0L, 1L)) {
            Instant asOf = NOW.plusSeconds(120).plusNanos(offsetNanos);
            SnapshotCacheService lookupCache = cacheWithSnapshot("lookup-" + offsetNanos, "price", NOW,
                    NOW.plusSeconds(900), Duration.ofSeconds(120), UnifiedSourceStatus.READY);
            SnapshotCacheService purgeCache = cacheWithSnapshot("purge-" + offsetNanos, "price", NOW,
                    NOW.plusSeconds(900), Duration.ofSeconds(120), UnifiedSourceStatus.READY);

            SnapshotCacheService.SnapshotLookup<String> lookup = lookupCache.lookup(
                    key("lookup-" + offsetNanos).snapshotKey(), asOf, Duration.ofSeconds(900));
            int purged = purgeCache.purgeExpired(asOf);

            if (offsetNanos < 0) {
                assertThat(lookup.fresh()).isTrue();
                assertThat(lookupCache.entryCount()).isEqualTo(1);
                assertThat(purged).isZero();
                assertThat(purgeCache.entryCount()).isEqualTo(1);
            } else {
                assertThat(lookup.fresh()).isFalse();
                assertThat(lookup.staleReadable()).isFalse();
                assertThat(lookupCache.entryCount()).isZero();
                assertThat(purged).isEqualTo(1);
                assertThat(purgeCache.entryCount()).isZero();
            }
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    void expiredLookupRemovesOnlyMatchingEntry() throws Exception {
        ProviderSnapshotKey snapshotKey = key("atomic-removal").snapshotKey();
        SnapshotCacheService cache = new SnapshotCacheService();
        var entriesField = SnapshotCacheService.class.getDeclaredField("entries");
        entriesField.setAccessible(true);
        BlockingGetMap<ProviderSnapshotKey, Object> entries = new BlockingGetMap<>();
        entriesField.set(cache, entries);
        cache.put(snapshotKey, "old", metadata(NOW, NOW.plusSeconds(5), UnifiedSourceStatus.READY),
                Duration.ofSeconds(120));
        entries.blockNextGet();

        ExecutorService caller = Executors.newSingleThreadExecutor();
        try {
            Future<SnapshotCacheService.SnapshotLookup<String>> expiredLookup = caller.submit(() ->
                    cache.lookup(snapshotKey, NOW.plusSeconds(120), Duration.ofSeconds(900)));
            assertThat(entries.getCaptured.await(1, TimeUnit.SECONDS)).isTrue();
            cache.put(snapshotKey, "replacement", metadata(NOW.plusSeconds(121), NOW.plusSeconds(151),
                    UnifiedSourceStatus.READY), Duration.ofSeconds(120));
            entries.continueGet.countDown();

            assertThat(expiredLookup.get(1, TimeUnit.SECONDS).fresh()).isFalse();
            assertThat(cache.lookup(snapshotKey, NOW.plusSeconds(122), Duration.ofSeconds(30)).payload())
                    .isEqualTo("replacement");
            assertThat(cache.entryCount()).isEqualTo(1);
        } finally {
            entries.continueGet.countDown();
            caller.shutdownNow();
        }
    }

    @Test
    void emptyConfirmedSnapshotStillHonorsRetention() {
        ProviderSnapshotKey snapshotKey = key("empty-retention").snapshotKey();
        SnapshotCacheService cache = cacheWithSnapshot("empty-retention", null, NOW,
                NOW.plusSeconds(900), Duration.ofSeconds(120), UnifiedSourceStatus.EMPTY_CONFIRMED);

        assertThat(cache.lookup(snapshotKey, NOW.plusSeconds(119), Duration.ofSeconds(900)).fresh()).isTrue();
        SnapshotCacheService.SnapshotLookup<String> atBoundary = cache.lookup(
                snapshotKey, NOW.plusSeconds(120), Duration.ofSeconds(900));

        assertThat(atBoundary.fresh()).isFalse();
        assertThat(atBoundary.staleReadable()).isFalse();
        assertThat(cache.entryCount()).isZero();
    }

    @Test
    void invalidCacheInputsFailClosed() {
        SnapshotCacheService cache = new SnapshotCacheService();
        ProviderSnapshotKey snapshotKey = key("invalid-input").snapshotKey();
        ProviderSnapshotMetadata validMetadata = metadata(NOW, NOW.plusSeconds(5), UnifiedSourceStatus.READY);
        ProviderSnapshotMetadata missingFetchTime = metadata(null, NOW.plusSeconds(5), UnifiedSourceStatus.READY);

        assertThatThrownBy(() -> cache.lookup(null, NOW, Duration.ofSeconds(5)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> cache.lookup(snapshotKey, null, Duration.ofSeconds(5)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> cache.put(snapshotKey, "price", null, Duration.ofSeconds(120)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> cache.put(snapshotKey, "price", missingFetchTime, Duration.ofSeconds(120)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> cache.put(snapshotKey, "price", validMetadata, Duration.ZERO))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(cache.entryCount()).isZero();

        cache.put(snapshotKey, "price", validMetadata, Duration.ofSeconds(120));
        assertThat(cache.lookup(snapshotKey, NOW.minusSeconds(1), Duration.ZERO).staleReadable()).isTrue();
        assertThat(cache.lookup(snapshotKey, NOW.minusSeconds(1), Duration.ofSeconds(-1)).staleReadable()).isTrue();
        assertThat(cache.entryCount()).isEqualTo(1);
    }

    private static SnapshotCacheService cacheWithSnapshot(
            String bucket,
            String payload,
            Instant fetchTime,
            Instant expiresAt,
            Duration retention,
            UnifiedSourceStatus sourceStatus) {
        SnapshotCacheService cache = new SnapshotCacheService();
        cache.put(key(bucket).snapshotKey(), payload, metadata(fetchTime, expiresAt, sourceStatus), retention);
        return cache;
    }

    private static ProviderSnapshotMetadata metadata(
            Instant fetchTime,
            Instant expiresAt,
            UnifiedSourceStatus sourceStatus) {
        return new ProviderSnapshotMetadata("TEST", ProviderDatasetType.PRICE, "BTCUSDT", "GLOBAL",
                fetchTime, fetchTime, expiresAt, sourceStatus, SnapshotFreshnessStatus.FRESH,
                "trace-retention", "request-retention", false, false, null, List.of());
    }

    private static ProviderCallRequest<String> request(
            String bucket,
            int freshSeconds,
            AtomicInteger calls,
            java.util.function.Supplier<ProviderAdapterResponse<String>> adapter) {
        return new ProviderCallRequest<>(key(bucket), AssetPriority.P0_POSITION, UserScanProfile.AUTO,
                RuntimeScanProfile.STANDARD, List.of("TEST"), "FM-TEST", Duration.ofSeconds(freshSeconds),
                Duration.ofMinutes(2), Duration.ofSeconds(1), "trace-" + bucket, 0, 0, () -> {
                    calls.incrementAndGet();
                    return adapter.get();
                });
    }

    private static ProviderRequestKey key(String bucket) {
        return new ProviderRequestKey("TEST", ProviderDatasetType.PRICE,
                ProviderCallTestFixtures.spot("BTCUSDT"), "BTCUSDT", "GLOBAL", bucket, "TEST_V1");
    }

    private static Harness harness() {
        ProviderCallProperties properties = new ProviderCallProperties();
        properties.setEnabled(true);
        properties.setExternalCallsEnabled(true);
        properties.setMaxConcurrentProviderCalls(2);
        properties.setMaxQueuedCalls(4);
        properties.setReservedPrioritySlots(1);
        MutableClock clock = new MutableClock(NOW);
        SnapshotCacheService cache = new SnapshotCacheService();
        ProviderSingleFlightGuard singleFlight = new ProviderSingleFlightGuard();
        ProviderRateBudgetManager budget = new ProviderRateBudgetManager(properties, clock);
        budget.register("TEST", 1000);
        ProviderCallExecutor executor = new ProviderCallExecutor(2, 4, 1);
        ProviderCallCoordinator coordinator = new ProviderCallCoordinator(properties, cache, singleFlight,
                budget, new ProviderCircuitBreaker(10, 1, clock), new ProviderCallAuditLog(),
                new ProviderConcurrencyGuard(properties), new ProviderHealthRegistry(clock), executor,
                new ProviderSnapshotRetentionPolicy(), clock);
        return new Harness(coordinator, cache, clock, executor);
    }

    private static void await(CountDownLatch latch) {
        try {
            latch.await();
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(interrupted);
        }
    }

    private record Harness(
            ProviderCallCoordinator coordinator,
            SnapshotCacheService cache,
            MutableClock clock,
            ProviderCallExecutor executor
    ) implements AutoCloseable {
        @Override
        public void close() {
            executor.close();
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

        @Override public ZoneId getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(ZoneId zone) { return this; }
        @Override public Instant instant() { return current; }
    }

    private static final class BlockingGetMap<K, V> extends ConcurrentHashMap<K, V> {
        private final CountDownLatch getCaptured = new CountDownLatch(1);
        private final CountDownLatch continueGet = new CountDownLatch(1);
        private volatile boolean blockNextGet;

        private void blockNextGet() {
            blockNextGet = true;
        }

        @Override
        public V get(Object key) {
            V value = super.get(key);
            if (blockNextGet) {
                blockNextGet = false;
                getCaptured.countDown();
                await(continueGet);
            }
            return value;
        }
    }
}
