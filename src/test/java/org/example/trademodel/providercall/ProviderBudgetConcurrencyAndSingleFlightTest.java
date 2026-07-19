package org.example.trademodel.providercall;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProviderBudgetConcurrencyAndSingleFlightTest {
    private static final Instant NOW = Instant.parse("2026-07-19T10:00:00Z");

    @Test
    void globalProviderBudgetCapsAggregateProviders() {
        ProviderCallProperties properties = new ProviderCallProperties();
        properties.setGlobalAdvertisedRequestsPerMinute(5);
        properties.setPerSymbolMinimumGapSeconds(1);
        ProviderRateBudgetManager budget = new ProviderRateBudgetManager(properties, Clock.fixed(NOW, ZoneOffset.UTC));
        budget.register("ONE", 100);
        budget.register("TWO", 100);

        assertThat(budget.reserve(key("ONE", "BTCUSDT", "1"), AssetPriority.P0_POSITION,
                RuntimeScanProfile.STANDARD)).isTrue();
        assertThat(budget.reserve(key("TWO", "ETHUSDT", "2"), AssetPriority.P0_POSITION,
                RuntimeScanProfile.STANDARD)).isTrue();
        assertThat(budget.reserve(key("ONE", "SOLUSDT", "3"), AssetPriority.P0_POSITION,
                RuntimeScanProfile.STANDARD)).isTrue();
        assertThat(budget.reserve(key("TWO", "BNBUSDT", "4"), AssetPriority.P0_POSITION,
                RuntimeScanProfile.STANDARD)).isTrue();
        assertThat(budget.reserve(key("ONE", "XRPUSDT", "5"), AssetPriority.P0_POSITION,
                RuntimeScanProfile.STANDARD)).isFalse();
    }

    @Test
    void perSymbolMinimumGapCannotBeBypassedByDifferentConsumers() {
        ProviderCallProperties properties = new ProviderCallProperties();
        properties.setPerSymbolMinimumGapSeconds(5);
        MutableClock clock = new MutableClock(NOW);
        ProviderRateBudgetManager budget = new ProviderRateBudgetManager(properties, clock);
        budget.register("TEST", 100);
        ProviderRequestKey key = key("TEST", "BTCUSDT", "same-window");

        assertThat(budget.reserve(key, AssetPriority.P1_WATCHLIST, RuntimeScanProfile.STANDARD)).isTrue();
        assertThat(budget.reserve(key, AssetPriority.P0_POSITION, RuntimeScanProfile.STANDARD)).isFalse();
        clock.advance(Duration.ofSeconds(5));
        assertThat(budget.reserve(key, AssetPriority.P0_POSITION, RuntimeScanProfile.STANDARD)).isTrue();
    }

    @Test
    void allFourOhlcvTimeframesCanRefreshInOneScan() {
        ProviderCallProperties properties = new ProviderCallProperties();
        properties.setPerSymbolMinimumGapSeconds(5);
        ProviderRateBudgetManager budget = new ProviderRateBudgetManager(properties,
                Clock.fixed(NOW, ZoneOffset.UTC));
        budget.register("TEST", 100);

        for (String timeframe : List.of("5m", "15m", "1h", "4h")) {
            assertThat(budget.reserve(ohlcvKey(ProviderCallTestFixtures.perpetual("BTCUSDT"), timeframe),
                    AssetPriority.P0_POSITION, RuntimeScanProfile.STANDARD)).isTrue();
        }
        assertThat(budget.state("TEST", ProviderCircuitState.CLOSED).regularBudgetUsage()).isEqualTo(4);
    }

    @Test
    void sameTimeframeDuplicateIsBlockedWithinMinimumGap() {
        ProviderCallProperties properties = new ProviderCallProperties();
        properties.setPerSymbolMinimumGapSeconds(5);
        ProviderRateBudgetManager budget = new ProviderRateBudgetManager(properties,
                Clock.fixed(NOW, ZoneOffset.UTC));
        budget.register("TEST", 100);
        ProviderRequestKey first = ohlcvKey(ProviderCallTestFixtures.perpetual("BTCUSDT"), "5m");
        ProviderRequestKey anotherBucket = new ProviderRequestKey("TEST", ProviderDatasetType.OHLCV,
                first.canonicalInstrumentId(), "BTCUSDT", "5m", "ANOTHER", "TEST_V1");

        assertThat(budget.reserve(first, AssetPriority.P0_POSITION, RuntimeScanProfile.STANDARD)).isTrue();
        assertThat(budget.reserve(anotherBucket, AssetPriority.P0_POSITION, RuntimeScanProfile.STANDARD)).isFalse();
    }

    @Test
    void differentTimeframesDoNotShareGapKey() {
        ProviderCallProperties properties = new ProviderCallProperties();
        properties.setPerSymbolMinimumGapSeconds(5);
        ProviderRateBudgetManager budget = new ProviderRateBudgetManager(properties,
                Clock.fixed(NOW, ZoneOffset.UTC));
        budget.register("TEST", 100);

        assertThat(budget.reserve(ohlcvKey(ProviderCallTestFixtures.perpetual("BTCUSDT"), "5m"),
                AssetPriority.P0_POSITION, RuntimeScanProfile.STANDARD)).isTrue();
        assertThat(budget.reserve(ohlcvKey(ProviderCallTestFixtures.perpetual("BTCUSDT"), "15m"),
                AssetPriority.P0_POSITION, RuntimeScanProfile.STANDARD)).isTrue();
    }

    @Test
    void spotAndPerpetualDoNotShareGapKey() {
        ProviderCallProperties properties = new ProviderCallProperties();
        properties.setPerSymbolMinimumGapSeconds(5);
        ProviderRateBudgetManager budget = new ProviderRateBudgetManager(properties,
                Clock.fixed(NOW, ZoneOffset.UTC));
        budget.register("TEST", 100);

        assertThat(budget.reserve(ohlcvKey(ProviderCallTestFixtures.spot("BTCUSDT"), "5m"),
                AssetPriority.P0_POSITION, RuntimeScanProfile.STANDARD)).isTrue();
        assertThat(budget.reserve(ohlcvKey(ProviderCallTestFixtures.perpetual("BTCUSDT"), "5m"),
                AssetPriority.P0_POSITION, RuntimeScanProfile.STANDARD)).isTrue();
    }

    @Test
    void emergencyReserveCannotBeConsumedByDiscovery() {
        ProviderCallProperties properties = new ProviderCallProperties();
        ProviderRateBudgetManager budget = new ProviderRateBudgetManager(properties, Clock.fixed(NOW, ZoneOffset.UTC));
        budget.register("TEST", 10);

        for (String symbol : List.of("BTCUSDT", "ETHUSDT", "SOLUSDT", "BNBUSDT")) {
            assertThat(budget.reserve(key("TEST", symbol, symbol), AssetPriority.P3_DISCOVERY,
                    RuntimeScanProfile.STANDARD)).isTrue();
        }
        assertThat(budget.reserve(key("TEST", "XRPUSDT", "regular-full"), AssetPriority.P3_DISCOVERY,
                RuntimeScanProfile.STANDARD)).isFalse();
        assertThat(budget.state("TEST", ProviderCircuitState.CLOSED).emergencyBudgetUsage()).isZero();
        assertThat(budget.reserve(key("TEST", "DOGEUSDT", "emergency"), AssetPriority.P0_POSITION,
                RuntimeScanProfile.EMERGENCY)).isTrue();
        assertThat(budget.state("TEST", ProviderCircuitState.CLOSED).emergencyBudgetUsage()).isEqualTo(1);
    }

    @Test
    void candidateAndPositionRetainCapacityAfterDiscoveryAndWatchlistDegrade() {
        ProviderCallProperties properties = new ProviderCallProperties();
        ProviderRateBudgetManager budget = new ProviderRateBudgetManager(properties, Clock.fixed(NOW, ZoneOffset.UTC));
        budget.register("TEST", 10);
        for (String symbol : List.of("BTCUSDT", "ETHUSDT", "SOLUSDT", "BNBUSDT")) {
            assertThat(budget.reserve(key("TEST", symbol, symbol), AssetPriority.P3_DISCOVERY,
                    RuntimeScanProfile.STANDARD)).isTrue();
        }
        assertThat(budget.reserve(key("TEST", "XRPUSDT", "p3"), AssetPriority.P3_DISCOVERY,
                RuntimeScanProfile.STANDARD)).isFalse();
        assertThat(budget.reserve(key("TEST", "DOGEUSDT", "p2"), AssetPriority.P2_CANDIDATE,
                RuntimeScanProfile.STANDARD)).isTrue();
        assertThat(budget.reserve(key("TEST", "ADAUSDT", "p0"), AssetPriority.P0_POSITION,
                RuntimeScanProfile.STANDARD)).isTrue();
    }

    @Test
    void concurrencyReservesCapacityForHigherPriorityAndSeparatesAiLimit() {
        ProviderConcurrencyGuard guard = new ProviderConcurrencyGuard(3, 1, 2, 1);
        ProviderConcurrencyGuard.Lease discovery = guard.tryAcquire(
                ProviderDatasetType.PRICE, AssetPriority.P3_DISCOVERY);
        ProviderConcurrencyGuard.Lease watchlist = guard.tryAcquire(
                ProviderDatasetType.PRICE, AssetPriority.P1_WATCHLIST);
        assertThat(discovery).isNotNull();
        assertThat(watchlist).isNotNull();
        assertThat(guard.tryAcquire(ProviderDatasetType.PRICE, AssetPriority.P3_DISCOVERY)).isNull();
        ProviderConcurrencyGuard.Lease position = guard.tryAcquire(
                ProviderDatasetType.PRICE, AssetPriority.P0_POSITION);
        assertThat(position).isNotNull();
        position.close();
        watchlist.close();
        discovery.close();

        ProviderConcurrencyGuard.Lease firstAi = guard.tryAcquire(
                ProviderDatasetType.AI_REVIEW, AssetPriority.P2_CANDIDATE);
        assertThat(firstAi).isNotNull();
        assertThat(guard.tryAcquire(ProviderDatasetType.AI_REVIEW, AssetPriority.P0_POSITION)).isNull();
        assertThat(guard.state().activeAiCalls()).isEqualTo(1);
        firstAi.close();
    }

    @Test
    void fiftyConcurrentRequestsForSameKeyInvokeOwnerOnce() throws Exception {
        ProviderSingleFlightGuard guard = new ProviderSingleFlightGuard();
        ProviderRequestKey key = key("TEST", "BTCUSDT", "single-flight");
        AtomicInteger calls = new AtomicInteger();
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(50);
        List<Future<String>> futures = new ArrayList<>();
        try {
            for (int i = 0; i < 50; i++) {
                futures.add(pool.submit(() -> {
                    await(start);
                    return guard.execute(key, () -> {
                        calls.incrementAndGet();
                        sleep(100);
                        return "shared";
                    });
                }));
            }
            start.countDown();
            for (Future<String> future : futures) assertThat(future.get()).isEqualTo("shared");
            assertThat(calls).hasValue(1);
            assertThat(guard.activeFlightCount()).isZero();
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    void differentInstrumentAndTimeframeUseIndependentFlights() {
        ProviderSingleFlightGuard guard = new ProviderSingleFlightGuard();
        AtomicInteger calls = new AtomicInteger();
        guard.execute(key("TEST", "BTCUSDT", "5m"), () -> calls.incrementAndGet());
        guard.execute(key("TEST", "ETHUSDT", "5m"), () -> calls.incrementAndGet());
        ProviderRequestKey fifteen = new ProviderRequestKey("TEST", ProviderDatasetType.PRICE,
                ProviderCallTestFixtures.spot("BTCUSDT"), "BTCUSDT", "15m", "bucket", "TEST_V1");
        guard.execute(fifteen, () -> calls.incrementAndGet());
        assertThat(calls).hasValue(3);
    }

    @Test
    void failedFlightIsRemovedAndCanBeRetried() {
        ProviderSingleFlightGuard guard = new ProviderSingleFlightGuard();
        ProviderRequestKey key = key("TEST", "BTCUSDT", "failure");
        assertThatThrownBy(() -> guard.execute(key, () -> {
            throw new IllegalStateException("fixture failure");
        })).isInstanceOf(IllegalStateException.class);

        assertThat(guard.execute(key, () -> "retry-success")).isEqualTo("retry-success");
        assertThat(guard.activeFlightCount()).isZero();
    }

    private static ProviderRequestKey key(String provider, String symbol, String bucket) {
        return new ProviderRequestKey(provider, ProviderDatasetType.PRICE,
                ProviderCallTestFixtures.spot(symbol), symbol, "GLOBAL", bucket, provider + "_V1");
    }

    private static ProviderRequestKey ohlcvKey(
            org.example.trademodel.providercall.instrument.CanonicalInstrumentId instrument,
            String timeframe) {
        return new ProviderRequestKey("TEST", ProviderDatasetType.OHLCV, instrument,
                "BTCUSDT", timeframe, "SCAN", "TEST_V1");
    }

    private static void await(CountDownLatch latch) {
        try {
            latch.await();
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(interrupted);
        }
    }

    private static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(interrupted);
        }
    }

    private static final class MutableClock extends Clock {
        private Instant current;

        private MutableClock(Instant current) {
            this.current = current;
        }

        void advance(Duration duration) {
            current = current.plus(duration);
        }

        @Override public ZoneId getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(ZoneId zone) { return this; }
        @Override public Instant instant() { return current; }
    }
}
