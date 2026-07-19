package org.example.trademodel.providercall;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class ProviderSharedFlightCallerRewrapTest {
    private static final Instant NOW = Instant.parse("2026-07-20T08:00:00Z");
    private static final Duration PRICE_RETENTION = Duration.ofMinutes(2);

    @Test
    void waiterReceivesOwnTraceAndProfileMetadata() throws Exception {
        try (Harness harness = harness()) {
            CountDownLatch started = new CountDownLatch(1);
            CountDownLatch release = new CountDownLatch(1);
            AtomicInteger physicalCalls = new AtomicInteger();
            ExecutorService callers = Executors.newFixedThreadPool(2);
            try {
                ProviderCallRequest<String> ownerRequest = request("owner", "dashboard-owner",
                        AssetPriority.P1_WATCHLIST, UserScanProfile.STANDARD, RuntimeScanProfile.STANDARD,
                        List.of("DASHBOARD"), "FM-DASHBOARD", Duration.ofSeconds(30), Duration.ofSeconds(2),
                        () -> {
                            physicalCalls.incrementAndGet();
                            started.countDown();
                            await(release);
                            return ProviderAdapterResponse.ready("shared", NOW);
                        });
                Future<ProviderCallResult<String>> owner = callers.submit(
                        () -> harness.coordinator.execute(ownerRequest));
                assertThat(started.await(1, TimeUnit.SECONDS)).isTrue();

                ProviderCallRequest<String> waiterRequest = request("waiter", "position-waiter",
                        AssetPriority.P0_POSITION, UserScanProfile.HIGH, RuntimeScanProfile.HIGH,
                        List.of("POSITION"), "FM-POSITION", Duration.ofSeconds(5), Duration.ofSeconds(2),
                        mustNotRun());
                Future<ProviderCallResult<String>> waiter = callers.submit(
                        () -> harness.coordinator.execute(waiterRequest));
                awaitState(() -> harness.singleFlight.waitingCallerCount() == 1);
                release.countDown();

                ProviderCallResult<String> ownerResult = owner.get(2, TimeUnit.SECONDS);
                ProviderCallResult<String> waiterResult = waiter.get(2, TimeUnit.SECONDS);
                assertThat(physicalCalls).hasValue(1);
                assertThat(ownerResult.metadata().traceId()).isEqualTo("dashboard-owner");
                assertThat(ownerResult.metadata().expiresAt()).isEqualTo(NOW.plusSeconds(30));
                assertThat(waiterResult.metadata().traceId()).isEqualTo("position-waiter");
                assertThat(waiterResult.metadata().expiresAt()).isEqualTo(NOW.plusSeconds(5));
                assertThat(waiterResult.metadata().cacheHit()).isTrue();
                assertThat(waiterResult.metadata().fallbackUsed()).isFalse();

                Map<String, ProviderCallAuditEvent> callerAudits = callerAuditsByTrace(harness);
                assertThat(callerAudits.get("dashboard-owner").priority()).isEqualTo(AssetPriority.P1_WATCHLIST);
                assertThat(callerAudits.get("dashboard-owner").effectiveProfile())
                        .isEqualTo(RuntimeScanProfile.STANDARD);
                assertThat(callerAudits.get("dashboard-owner").frequencyMatrixVersion())
                        .isEqualTo("FM-DASHBOARD");
                assertThat(callerAudits.get("position-waiter").priority()).isEqualTo(AssetPriority.P0_POSITION);
                assertThat(callerAudits.get("position-waiter").effectiveProfile())
                        .isEqualTo(RuntimeScanProfile.HIGH);
                assertThat(callerAudits.get("position-waiter").frequencyMatrixVersion())
                        .isEqualTo("FM-POSITION");
            } finally {
                release.countDown();
                callers.shutdownNow();
            }
        }
    }

    @Test
    void multipleWaitersReceiveIndependentMetadata() throws Exception {
        try (Harness harness = harness()) {
            CountDownLatch started = new CountDownLatch(1);
            CountDownLatch release = new CountDownLatch(1);
            AtomicInteger physicalCalls = new AtomicInteger();
            ExecutorService callers = Executors.newFixedThreadPool(4);
            try {
                Future<ProviderCallResult<String>> owner = callers.submit(() -> harness.coordinator.execute(
                        request("owner", "trace-owner", AssetPriority.P1_WATCHLIST, UserScanProfile.STANDARD,
                                RuntimeScanProfile.STANDARD, List.of("OWNER"), "FM-OWNER",
                                Duration.ofSeconds(10), Duration.ofSeconds(2), () -> {
                                    physicalCalls.incrementAndGet();
                                    started.countDown();
                                    await(release);
                                    return ProviderAdapterResponse.ready("shared", NOW);
                                })));
                assertThat(started.await(1, TimeUnit.SECONDS)).isTrue();

                List<ProviderCallRequest<String>> waiterRequests = List.of(
                        request("waiter-1", "trace-waiter-1", AssetPriority.P0_POSITION, UserScanProfile.HIGH,
                                RuntimeScanProfile.HIGH, List.of("W1"), "FM-W1", Duration.ofSeconds(5),
                                Duration.ofSeconds(2), mustNotRun()),
                        request("waiter-2", "trace-waiter-2", AssetPriority.P2_CANDIDATE, UserScanProfile.AUTO,
                                RuntimeScanProfile.LOW, List.of("W2"), "FM-W2", Duration.ofSeconds(20),
                                Duration.ofSeconds(2), mustNotRun()),
                        request("waiter-3", "trace-waiter-3", AssetPriority.P3_DISCOVERY, UserScanProfile.LOW,
                                RuntimeScanProfile.EMERGENCY, List.of("W3"), "FM-W3", Duration.ofSeconds(40),
                                Duration.ofSeconds(2), mustNotRun()));
                List<Future<ProviderCallResult<String>>> waiters = waiterRequests.stream()
                        .map(waiterRequest -> callers.submit(() -> harness.coordinator.execute(waiterRequest)))
                        .toList();
                awaitState(() -> harness.singleFlight.waitingCallerCount() == 3);
                release.countDown();

                assertThat(owner.get(2, TimeUnit.SECONDS).metadata().traceId()).isEqualTo("trace-owner");
                List<ProviderCallResult<String>> waiterResults = waiters.stream().map(future -> get(future, 2)).toList();
                assertThat(waiterResults).extracting(result -> result.metadata().traceId())
                        .containsExactlyInAnyOrder("trace-waiter-1", "trace-waiter-2", "trace-waiter-3");
                assertThat(waiterResults).extracting(result -> result.metadata().expiresAt())
                        .containsExactlyInAnyOrder(NOW.plusSeconds(5), NOW.plusSeconds(20), NOW.plusSeconds(40));
                assertThat(waiterResults).allMatch(result -> result.metadata().cacheHit());
                assertThat(physicalCalls).hasValue(1);

                List<ProviderCallAuditEvent> callerAudits = callerAudits(harness);
                assertThat(callerAudits).hasSize(4);
                assertThat(callerAudits).extracting(ProviderCallAuditEvent::traceId).doesNotHaveDuplicates();
                assertThat(callerAudits).extracting(ProviderCallAuditEvent::frequencyMatrixVersion)
                        .containsExactlyInAnyOrder("FM-OWNER", "FM-W1", "FM-W2", "FM-W3");
                assertThat(physicalAudits(harness, ProviderCallAuditPhase.PHYSICAL_ATTEMPT_STARTED)).hasSize(1);
                assertThat(physicalAudits(harness, ProviderCallAuditPhase.PHYSICAL_ATTEMPT_COMPLETED)).hasSize(1);
                assertThat(harness.budget.state("TEST", ProviderCircuitState.CLOSED).currentWindowUsage())
                        .isEqualTo(1);
                assertThat(harness.health.successes).hasValue(1);
                assertThat(harness.circuit.settlements).hasValue(1);
            } finally {
                release.countDown();
                callers.shutdownNow();
            }
        }
    }

    @Test
    void waiterSuccessIsRewrappedAsCacheHit() throws Exception {
        SharedResults<String> results = successfulSharedFlight(Duration.ofSeconds(30),
                Duration.ofSeconds(5), ProviderAdapterResponse.ready("ready", NOW));

        assertThat(results.owner.metadata().cacheHit()).isFalse();
        assertThat(results.waiter.metadata().cacheHit()).isTrue();
        assertThat(results.waiter.metadata().fallbackUsed()).isFalse();
        assertThat(results.waiter.metadata().sourceStatus()).isEqualTo(UnifiedSourceStatus.READY);
        assertThat(results.physicalCalls).isEqualTo(1);
    }

    @Test
    void waiterEmptyConfirmedUsesOwnMetadata() throws Exception {
        SharedResults<String> results = successfulSharedFlight(Duration.ofSeconds(30),
                Duration.ofSeconds(5), emptyConfirmed());

        assertThat(results.owner.payload()).isNull();
        assertThat(results.waiter.payload()).isNull();
        assertThat(results.owner.metadata().sourceStatus()).isEqualTo(UnifiedSourceStatus.EMPTY_CONFIRMED);
        assertThat(results.waiter.metadata().sourceStatus()).isEqualTo(UnifiedSourceStatus.EMPTY_CONFIRMED);
        assertThat(results.waiter.metadata().traceId()).isEqualTo("waiter-trace");
        assertThat(results.waiter.metadata().cacheHit()).isTrue();
        assertThat(results.waiter.metadata().expiresAt()).isEqualTo(NOW.plusSeconds(5));
    }

    @Test
    void waiterFailureUsesOwnFallbackAndAudit() throws Exception {
        try (Harness harness = harness()) {
            CountDownLatch started = new CountDownLatch(1);
            CountDownLatch release = new CountDownLatch(1);
            ExecutorService callers = Executors.newFixedThreadPool(2);
            try {
                ProviderCallRequest<String> ownerRequest = simpleRequest("owner", "owner-trace",
                        Duration.ofSeconds(5), Duration.ofSeconds(2), () -> {
                            started.countDown();
                            await(release);
                            return ProviderAdapterResponse.failed(UnifiedSourceStatus.ERROR, 503,
                                    "PROVIDER_UNAVAILABLE", null);
                        });
                Future<ProviderCallResult<String>> owner = callers.submit(
                        () -> harness.coordinator.execute(ownerRequest));
                assertThat(started.await(1, TimeUnit.SECONDS)).isTrue();
                putSnapshot(harness, ownerRequest.key().snapshotKey(), "waiter-stale",
                        NOW.minusSeconds(10), Duration.ofSeconds(5), UnifiedSourceStatus.READY);

                ProviderCallRequest<String> waiterRequest = request("waiter", "waiter-trace",
                        AssetPriority.P0_POSITION, UserScanProfile.HIGH, RuntimeScanProfile.HIGH,
                        List.of("WAITER"), "FM-WAITER", Duration.ofSeconds(5), Duration.ofSeconds(2),
                        mustNotRun());
                Future<ProviderCallResult<String>> waiter = callers.submit(
                        () -> harness.coordinator.execute(waiterRequest));
                awaitState(() -> harness.singleFlight.waitingCallerCount() == 1);
                release.countDown();

                assertThat(owner.get(2, TimeUnit.SECONDS).payload()).isNull();
                ProviderCallResult<String> waiterResult = waiter.get(2, TimeUnit.SECONDS);
                assertThat(waiterResult.payload()).isEqualTo("waiter-stale");
                assertThat(waiterResult.metadata().traceId()).isEqualTo("waiter-trace");
                assertThat(waiterResult.metadata().freshnessStatus())
                        .isEqualTo(SnapshotFreshnessStatus.STALE_READABLE);
                assertThat(waiterResult.metadata().fallbackUsed()).isTrue();
                assertThat(callerAuditsByTrace(harness).get("waiter-trace").effectiveProfile())
                        .isEqualTo(RuntimeScanProfile.HIGH);
            } finally {
                release.countDown();
                callers.shutdownNow();
            }
        }
    }

    @Test
    void waiterFailureWithoutFallbackUsesOwnTrace() throws Exception {
        try (Harness harness = harness()) {
            CountDownLatch started = new CountDownLatch(1);
            CountDownLatch release = new CountDownLatch(1);
            AtomicInteger physicalCalls = new AtomicInteger();
            ExecutorService callers = Executors.newFixedThreadPool(2);
            try {
                Future<ProviderCallResult<String>> owner = callers.submit(() -> harness.coordinator.execute(
                        simpleRequest("owner", "owner-trace", Duration.ofSeconds(5), Duration.ofSeconds(2), () -> {
                            physicalCalls.incrementAndGet();
                            started.countDown();
                            await(release);
                            return ProviderAdapterResponse.failed(UnifiedSourceStatus.ERROR, 503,
                                    "PROVIDER_UNAVAILABLE", null);
                        })));
                assertThat(started.await(1, TimeUnit.SECONDS)).isTrue();
                Future<ProviderCallResult<String>> waiter = callers.submit(() -> harness.coordinator.execute(
                        request("waiter", "waiter-trace", AssetPriority.P3_DISCOVERY, UserScanProfile.LOW,
                                RuntimeScanProfile.LOW, List.of("WAITER"), "FM-WAITER",
                                Duration.ofSeconds(5), Duration.ofSeconds(2), mustNotRun())));
                awaitState(() -> harness.singleFlight.waitingCallerCount() == 1);
                release.countDown();

                ProviderCallResult<String> ownerResult = owner.get(2, TimeUnit.SECONDS);
                ProviderCallResult<String> waiterResult = waiter.get(2, TimeUnit.SECONDS);
                assertThat(ownerResult.metadata().traceId()).isEqualTo("owner-trace");
                assertThat(waiterResult.metadata().traceId()).isEqualTo("waiter-trace");
                assertThat(waiterResult.metadata().errorCode()).isEqualTo("PROVIDER_UNAVAILABLE");
                assertThat(physicalCalls).hasValue(1);
                assertThat(harness.health.failures).hasValue(1);
                assertThat(harness.circuit.settlements).hasValue(1);
                assertThat(harness.budget.state("TEST", ProviderCircuitState.CLOSED).currentWindowUsage())
                        .isEqualTo(1);
                assertThat(callerAuditsByTrace(harness)).containsKeys("owner-trace", "waiter-trace");
            } finally {
                release.countDown();
                callers.shutdownNow();
            }
        }
    }

    @Test
    void waiterTimeoutStillDoesNotCancelOwner() throws Exception {
        try (Harness harness = harness()) {
            CountDownLatch started = new CountDownLatch(1);
            CountDownLatch release = new CountDownLatch(1);
            AtomicInteger physicalCalls = new AtomicInteger();
            ExecutorService callers = Executors.newSingleThreadExecutor();
            try {
                Future<ProviderCallResult<String>> owner = callers.submit(() -> harness.coordinator.execute(
                        simpleRequest("owner", "owner-trace", Duration.ofSeconds(5), Duration.ofSeconds(2), () -> {
                            physicalCalls.incrementAndGet();
                            started.countDown();
                            await(release);
                            return ProviderAdapterResponse.ready("ready", NOW);
                        })));
                assertThat(started.await(1, TimeUnit.SECONDS)).isTrue();

                ProviderCallResult<String> waiter = harness.coordinator.execute(request("waiter", "waiter-trace",
                        AssetPriority.P3_DISCOVERY, UserScanProfile.LOW, RuntimeScanProfile.LOW,
                        List.of("WAITER_TIMEOUT"), "FM-WAITER", Duration.ofSeconds(5),
                        Duration.ofMillis(30), mustNotRun()));
                assertThat(waiter.metadata().traceId()).isEqualTo("waiter-trace");
                assertThat(waiter.metadata().errorCode()).isEqualTo("PROVIDER_TIMEOUT");
                assertThat(harness.singleFlight.activeFlightCount()).isEqualTo(1);
                assertThat(harness.health.failures).hasValue(0);
                release.countDown();

                assertThat(owner.get(2, TimeUnit.SECONDS).payload()).isEqualTo("ready");
                assertThat(physicalCalls).hasValue(1);
                assertThat(callerAuditsByTrace(harness)).containsKeys("owner-trace", "waiter-trace");
                assertThat(harness.circuit.settlements).hasValue(1);
            } finally {
                release.countDown();
                callers.shutdownNow();
            }
        }
    }

    @Test
    void readyLongTtlExpiryIsCappedAtRetention() {
        try (Harness harness = harness()) {
            ProviderCallResult<String> result = harness.coordinator.execute(simpleRequest(
                    "ready-long", "ready-long-trace", Duration.ofSeconds(900), Duration.ofSeconds(2),
                    () -> ProviderAdapterResponse.ready("ready", NOW)));

            assertThat(result.metadata().expiresAt()).isEqualTo(NOW.plus(PRICE_RETENTION));
        }
    }

    @Test
    void readyShortTtlExpiryRemainsShorter() {
        try (Harness harness = harness()) {
            ProviderCallResult<String> result = harness.coordinator.execute(simpleRequest(
                    "ready-short", "ready-short-trace", Duration.ofSeconds(30), Duration.ofSeconds(2),
                    () -> ProviderAdapterResponse.ready("ready", NOW)));

            assertThat(result.metadata().expiresAt()).isEqualTo(NOW.plusSeconds(30));
        }
    }

    @Test
    void exactRetentionBoundary() {
        try (Harness harness = harness()) {
            ProviderCallResult<String> result = harness.coordinator.execute(simpleRequest(
                    "exact-boundary", "exact-boundary-trace", PRICE_RETENTION, Duration.ofSeconds(2),
                    () -> ProviderAdapterResponse.ready("ready", NOW)));

            assertThat(result.metadata().expiresAt()).isEqualTo(NOW.plus(PRICE_RETENTION));
        }
    }

    @Test
    void emptyConfirmedLongTtlIsCapped() {
        try (Harness harness = harness()) {
            ProviderCallResult<String> result = harness.coordinator.execute(simpleRequest(
                    "empty-long", "empty-long-trace", Duration.ofSeconds(900), Duration.ofSeconds(2),
                    ProviderSharedFlightCallerRewrapTest::emptyConfirmed));

            assertThat(result.metadata().sourceStatus()).isEqualTo(UnifiedSourceStatus.EMPTY_CONFIRMED);
            assertThat(result.metadata().expiresAt()).isEqualTo(NOW.plus(PRICE_RETENTION));
        }
    }

    @Test
    void cacheHitLongCallerTtlIsCapped() {
        try (Harness harness = harness()) {
            AtomicInteger physicalCalls = new AtomicInteger();
            harness.coordinator.execute(simpleRequest("short-owner", "short-owner-trace",
                    Duration.ofSeconds(5), Duration.ofSeconds(2), () -> {
                        physicalCalls.incrementAndGet();
                        return ProviderAdapterResponse.ready("cached", NOW);
                    }));

            ProviderCallResult<String> longCaller = harness.coordinator.execute(simpleRequest(
                    "long-caller", "long-caller-trace", Duration.ofSeconds(900), Duration.ofSeconds(2),
                    mustNotRun()));

            assertThat(longCaller.payload()).isEqualTo("cached");
            assertThat(longCaller.metadata().cacheHit()).isTrue();
            assertThat(longCaller.metadata().expiresAt()).isEqualTo(NOW.plus(PRICE_RETENTION));
            assertThat(physicalCalls).hasValue(1);
        }
    }

    @Test
    void peekLongCallerTtlIsCapped() {
        try (Harness harness = harness()) {
            ProviderCallRequest<String> storedRequest = simpleRequest("peek-stored", "stored-trace",
                    Duration.ofSeconds(5), Duration.ofSeconds(2), mustNotRun());
            putSnapshot(harness, storedRequest.key().snapshotKey(), "cached", NOW,
                    Duration.ofSeconds(5), UnifiedSourceStatus.READY);

            ProviderCallResult<String> result = harness.coordinator.peek(storedRequest.key(),
                    AssetPriority.P1_WATCHLIST, Duration.ofSeconds(900), "peek-caller-trace");

            assertThat(result.metadata().traceId()).isEqualTo("peek-caller-trace");
            assertThat(result.metadata().expiresAt()).isEqualTo(NOW.plus(PRICE_RETENTION));
        }
    }

    @Test
    void sharedFlightWaiterLongTtlIsCapped() throws Exception {
        SharedResults<String> results = successfulSharedFlight(Duration.ofSeconds(30),
                Duration.ofSeconds(900), ProviderAdapterResponse.ready("ready", NOW));

        assertThat(results.waiter.metadata().expiresAt()).isEqualTo(NOW.plus(PRICE_RETENTION));
        assertThat(results.physicalCalls).isEqualTo(1);
    }

    @Test
    void shortOwnerDoesNotPermanentlyCapLongWaiterBeforeRetention() throws Exception {
        SharedResults<String> results = successfulSharedFlight(Duration.ofSeconds(5),
                Duration.ofSeconds(30), ProviderAdapterResponse.ready("ready", NOW));

        assertThat(results.owner.metadata().expiresAt()).isEqualTo(NOW.plusSeconds(5));
        assertThat(results.waiter.metadata().expiresAt()).isEqualTo(NOW.plusSeconds(30));
    }

    @Test
    void expiryOverflowFallsBackToRetention() {
        Instant nearMaximum = Instant.MAX.minusSeconds(60);
        Duration retention = Duration.ofSeconds(30);
        try (Harness harness = harness(nearMaximum, retention)) {
            ProviderCallRequest<String> request = simpleRequest("overflow", "overflow-trace",
                    Duration.ofSeconds(Long.MAX_VALUE), Duration.ofSeconds(2), mustNotRun());
            putSnapshot(harness, request.key().snapshotKey(), "cached", nearMaximum,
                    Duration.ofSeconds(1), UnifiedSourceStatus.READY);

            ProviderCallResult<String> result = harness.coordinator.execute(request);

            assertThat(result.payload()).isEqualTo("cached");
            assertThat(result.metadata().expiresAt()).isEqualTo(nearMaximum.plus(retention));
        }
    }

    @Test
    void invalidRetentionFailsClosed() {
        try (Harness harness = harness(NOW, Duration.ZERO)) {
            ProviderCallResult<String> result = harness.coordinator.execute(simpleRequest(
                    "invalid-retention", "invalid-retention-trace", Duration.ofSeconds(30),
                    Duration.ofSeconds(2), () -> ProviderAdapterResponse.ready("must-not-cache", NOW)));

            assertThat(result.payload()).isNull();
            assertThat(result.metadata().sourceStatus()).isEqualTo(UnifiedSourceStatus.ERROR);
            assertThat(result.metadata().errorCode()).isEqualTo("PROVIDER_SNAPSHOT_RETENTION_INVALID");
            assertThat(harness.cache.entryCount()).isZero();
            assertThat(harness.health.successes).hasValue(0);
        }
    }

    @Test
    void retentionBoundaryOverflowFailsClosed() {
        Instant nearMaximum = Instant.MAX.minusSeconds(10);
        try (Harness harness = harness(nearMaximum, Duration.ofSeconds(30))) {
            ProviderCallResult<String> result = harness.coordinator.execute(simpleRequest(
                    "retention-overflow", "retention-overflow-trace", Duration.ofSeconds(5),
                    Duration.ofSeconds(2), () -> ProviderAdapterResponse.ready("must-not-cache", nearMaximum)));

            assertThat(result.payload()).isNull();
            assertThat(result.metadata().errorCode()).isEqualTo("PROVIDER_SNAPSHOT_RETENTION_INVALID");
            assertThat(harness.cache.entryCount()).isZero();
            assertThat(harness.health.successes).hasValue(0);
        }
    }

    private static SharedResults<String> successfulSharedFlight(
            Duration ownerTtl,
            Duration waiterTtl,
            ProviderAdapterResponse<String> response) throws Exception {
        try (Harness harness = harness()) {
            CountDownLatch started = new CountDownLatch(1);
            CountDownLatch release = new CountDownLatch(1);
            AtomicInteger physicalCalls = new AtomicInteger();
            ExecutorService callers = Executors.newFixedThreadPool(2);
            try {
                Future<ProviderCallResult<String>> owner = callers.submit(() -> harness.coordinator.execute(
                        simpleRequest("owner", "owner-trace", ownerTtl, Duration.ofSeconds(2), () -> {
                            physicalCalls.incrementAndGet();
                            started.countDown();
                            await(release);
                            return response;
                        })));
                assertThat(started.await(1, TimeUnit.SECONDS)).isTrue();
                Future<ProviderCallResult<String>> waiter = callers.submit(() -> harness.coordinator.execute(
                        simpleRequest("waiter", "waiter-trace", waiterTtl, Duration.ofSeconds(2), mustNotRun())));
                awaitState(() -> harness.singleFlight.waitingCallerCount() == 1);
                release.countDown();
                return new SharedResults<>(owner.get(2, TimeUnit.SECONDS), waiter.get(2, TimeUnit.SECONDS),
                        physicalCalls.get());
            } finally {
                release.countDown();
                callers.shutdownNow();
            }
        }
    }

    private static ProviderAdapterResponse<String> emptyConfirmed() {
        return new ProviderAdapterResponse<>(null, UnifiedSourceStatus.EMPTY_CONFIRMED, NOW,
                200, null, "NO_DATA");
    }

    private static ProviderCallRequest<String> simpleRequest(
            String bucket,
            String traceId,
            Duration freshTtl,
            Duration timeout,
            Supplier<ProviderAdapterResponse<String>> adapter) {
        return request(bucket, traceId, AssetPriority.P0_POSITION, UserScanProfile.AUTO,
                RuntimeScanProfile.STANDARD, List.of("TEST"), "FM-TEST", freshTtl, timeout, adapter);
    }

    private static ProviderCallRequest<String> request(
            String bucket,
            String traceId,
            AssetPriority priority,
            UserScanProfile baseProfile,
            RuntimeScanProfile effectiveProfile,
            List<String> reasonCodes,
            String frequencyVersion,
            Duration freshTtl,
            Duration timeout,
            Supplier<ProviderAdapterResponse<String>> adapter) {
        ProviderRequestKey key = new ProviderRequestKey("TEST", ProviderDatasetType.PRICE,
                ProviderCallTestFixtures.spot("BTCUSDT"), "BTCUSDT", "GLOBAL", bucket, "TEST_V1");
        return new ProviderCallRequest<>(key, priority, baseProfile, effectiveProfile, reasonCodes,
                frequencyVersion, freshTtl, PRICE_RETENTION, timeout, traceId, 0, 0, adapter);
    }

    private static Supplier<ProviderAdapterResponse<String>> mustNotRun() {
        return () -> {
            throw new AssertionError("waiter/cache-hit adapter must not run");
        };
    }

    private static void putSnapshot(
            Harness harness,
            ProviderSnapshotKey key,
            String payload,
            Instant fetchTime,
            Duration storedFreshTtl,
            UnifiedSourceStatus status) {
        ProviderSnapshotMetadata metadata = new ProviderSnapshotMetadata("TEST", ProviderDatasetType.PRICE,
                "BTCUSDT", "GLOBAL", fetchTime, fetchTime, fetchTime.plus(storedFreshTtl), status,
                SnapshotFreshnessStatus.FRESH, "stored-trace", "stored-key", false, false, null, List.of());
        harness.cache.put(key, payload, metadata, harness.retention);
    }

    private static List<ProviderCallAuditEvent> callerAudits(Harness harness) {
        return harness.audit.snapshot().stream()
                .filter(event -> event.phase() == ProviderCallAuditPhase.REQUEST_RESULT)
                .toList();
    }

    private static Map<String, ProviderCallAuditEvent> callerAuditsByTrace(Harness harness) {
        return callerAudits(harness).stream().collect(Collectors.toMap(
                ProviderCallAuditEvent::traceId, event -> event));
    }

    private static List<ProviderCallAuditEvent> physicalAudits(
            Harness harness,
            ProviderCallAuditPhase phase) {
        return harness.audit.snapshot().stream().filter(event -> event.phase() == phase).toList();
    }

    private static ProviderCallResult<String> get(Future<ProviderCallResult<String>> future, int seconds) {
        try {
            return future.get(seconds, TimeUnit.SECONDS);
        } catch (Exception failure) {
            throw new AssertionError(failure);
        }
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
            Thread.onSpinWait();
        }
        assertThat(condition.getAsBoolean()).isTrue();
    }

    private static Harness harness() {
        return harness(NOW, PRICE_RETENTION);
    }

    private static Harness harness(Instant now, Duration retention) {
        ProviderCallProperties properties = new ProviderCallProperties();
        properties.setEnabled(true);
        properties.setExternalCallsEnabled(true);
        properties.setMaxConcurrentProviderCalls(2);
        properties.setMaxQueuedCalls(4);
        properties.setReservedPrioritySlots(1);
        MutableClock clock = new MutableClock(now);
        SnapshotCacheService cache = new SnapshotCacheService();
        ProviderSingleFlightGuard singleFlight = new ProviderSingleFlightGuard();
        ProviderRateBudgetManager budget = new ProviderRateBudgetManager(properties, clock);
        budget.register("TEST", 1000);
        ProviderCallAuditLog audit = new ProviderCallAuditLog();
        CountingCircuitBreaker circuit = new CountingCircuitBreaker(clock);
        CountingHealthRegistry health = new CountingHealthRegistry(clock);
        ProviderCallExecutor executor = new ProviderCallExecutor(2, 4, 1);
        ProviderSnapshotRetentionPolicy retentionPolicy = new ProviderSnapshotRetentionPolicy() {
            @Override
            public Duration staleRetention(ProviderDatasetType datasetType) {
                return retention;
            }
        };
        ProviderCallCoordinator coordinator = new ProviderCallCoordinator(properties, cache, singleFlight,
                budget, circuit, audit, new ProviderConcurrencyGuard(properties), health, executor,
                retentionPolicy, clock);
        return new Harness(coordinator, cache, singleFlight, budget, audit, circuit, health, executor, retention);
    }

    private record SharedResults<T>(
            ProviderCallResult<T> owner,
            ProviderCallResult<T> waiter,
            int physicalCalls) {
    }

    private record Harness(
            ProviderCallCoordinator coordinator,
            SnapshotCacheService cache,
            ProviderSingleFlightGuard singleFlight,
            ProviderRateBudgetManager budget,
            ProviderCallAuditLog audit,
            CountingCircuitBreaker circuit,
            CountingHealthRegistry health,
            ProviderCallExecutor executor,
            Duration retention) implements AutoCloseable {
        @Override
        public void close() {
            executor.close();
        }
    }

    private static final class CountingCircuitBreaker extends ProviderCircuitBreaker {
        private final AtomicInteger settlements = new AtomicInteger();

        private CountingCircuitBreaker(Clock clock) {
            super(10, 1, clock);
        }

        @Override
        synchronized boolean settle(
                String provider,
                boolean halfOpenProbe,
                long probeId,
                ProviderCircuitPermit.Settlement settlement) {
            settlements.incrementAndGet();
            return super.settle(provider, halfOpenProbe, probeId, settlement);
        }
    }

    private static final class CountingHealthRegistry extends ProviderHealthRegistry {
        private final AtomicInteger successes = new AtomicInteger();
        private final AtomicInteger failures = new AtomicInteger();

        private CountingHealthRegistry(Clock clock) {
            super(clock);
        }

        @Override
        public void recordSuccess(ProviderSnapshotKey key, UnifiedSourceStatus sourceStatus) {
            successes.incrementAndGet();
            super.recordSuccess(key, sourceStatus);
        }

        @Override
        public void recordFailure(
                ProviderSnapshotKey key,
                UnifiedSourceStatus sourceStatus,
                String reasonCode) {
            failures.incrementAndGet();
            super.recordFailure(key, sourceStatus, reasonCode);
        }
    }

    private static final class MutableClock extends Clock {
        private final Instant current;

        private MutableClock(Instant current) {
            this.current = current;
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
