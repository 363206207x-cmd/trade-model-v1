package org.example.trademodel.providercall;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class ProviderSnapshotQueryRefreshContractTest {

    @Test
    void queryNeverCallsProviderAndRefreshPopulatesSharedSnapshot() {
        Fixture fixture = fixture();
        ProviderRequestKey key = ProviderCallTestFixtures.key("TEST", ProviderDatasetType.PRICE,
                "BTCUSDT", "GLOBAL", "bucket-1");
        AtomicInteger calls = new AtomicInteger();

        ProviderCallResult<String> missing = fixture.query.query(key, AssetPriority.P1_WATCHLIST,
                Duration.ofSeconds(5), "dashboard-query");
        assertThat(missing.payload()).isNull();
        assertThat(missing.metadata().freshnessStatus()).isEqualTo(SnapshotFreshnessStatus.UNAVAILABLE);
        assertThat(calls).hasValue(0);

        ProviderCallResult<String> refreshed = fixture.refresh.refresh(request(key, fixture.clock, calls, "65000"));
        ProviderCallResult<String> dashboard = fixture.query.query(key, AssetPriority.P1_WATCHLIST,
                Duration.ofSeconds(5), "dashboard-query-2");
        assertThat(refreshed.payload()).isEqualTo("65000");
        assertThat(dashboard.payload()).isEqualTo("65000");
        assertThat(dashboard.metadata().cacheHit()).isTrue();
        assertThat(calls).hasValue(1);
    }

    @Test
    void staleReadableIsReturnedWithoutImplicitRefreshAndCanBeRefreshedExplicitly() {
        Fixture fixture = fixture();
        ProviderRequestKey key = ProviderCallTestFixtures.key("TEST", ProviderDatasetType.PRICE,
                "BTCUSDT", "GLOBAL", "bucket-2");
        AtomicInteger calls = new AtomicInteger();
        fixture.refresh.refresh(request(key, fixture.clock, calls, "65000"));
        fixture.clock.advance(Duration.ofSeconds(6));

        ProviderCallResult<String> stale = fixture.query.query(key, AssetPriority.P0_POSITION,
                Duration.ofSeconds(5), "dashboard-stale-query");
        assertThat(stale.payload()).isEqualTo("65000");
        assertThat(stale.metadata().freshnessStatus()).isEqualTo(SnapshotFreshnessStatus.STALE_READABLE);
        assertThat(stale.metadata().sourceStatus()).isEqualTo(UnifiedSourceStatus.STALE);
        assertThat(calls).hasValue(1);

        ProviderCallResult<String> refreshed = fixture.refresh.refresh(request(key, fixture.clock, calls, "65001"));
        assertThat(refreshed.payload()).isEqualTo("65001");
        assertThat(calls).hasValue(2);
    }

    @Test
    void dashboardAndPositionMonitorCanShareOneFrozenSnapshot() {
        Fixture fixture = fixture();
        ProviderRequestKey key = ProviderCallTestFixtures.key("TEST", ProviderDatasetType.PRICE,
                "ETHUSDT", "GLOBAL", "bucket-3");
        AtomicInteger calls = new AtomicInteger();
        fixture.refresh.refresh(request(key, fixture.clock, calls, "3500"));

        ProviderCallResult<String> dashboard = fixture.query.query(key, AssetPriority.P1_WATCHLIST,
                Duration.ofSeconds(5), "dashboard");
        ProviderCallResult<String> monitor = fixture.query.query(key, AssetPriority.P0_POSITION,
                Duration.ofSeconds(5), "position-monitor");
        assertThat(dashboard.payload()).isEqualTo(monitor.payload()).isEqualTo("3500");
        assertThat(calls).hasValue(1);
    }

    private static ProviderCallRequest<String> request(ProviderRequestKey key,
                                                       MutableClock clock,
                                                       AtomicInteger calls,
                                                       String payload) {
        return new ProviderCallRequest<>(key, AssetPriority.P0_POSITION, Duration.ofSeconds(5),
                Duration.ofMinutes(2), Duration.ofSeconds(1), "refresh-trace", () -> {
            calls.incrementAndGet();
            return ProviderAdapterResponse.ready(payload, clock.instant());
        });
    }

    private static Fixture fixture() {
        ProviderCallProperties properties = new ProviderCallProperties();
        properties.setEnabled(true);
        properties.setExternalCallsEnabled(true);
        MutableClock clock = new MutableClock(Instant.parse("2026-07-19T10:00:00Z"));
        SnapshotCacheService cache = new SnapshotCacheService();
        ProviderSingleFlightGuard singleFlight = new ProviderSingleFlightGuard();
        ProviderRateBudgetManager budget = new ProviderRateBudgetManager(properties, clock);
        budget.register("TEST", 100);
        ProviderCallCoordinator coordinator = new ProviderCallCoordinator(properties, cache, singleFlight,
                budget, new ProviderCircuitBreaker(3, 60, clock), new ProviderCallAuditLog(), clock);
        return new Fixture(clock, new CoordinatedProviderSnapshotQueryService(coordinator),
                new CoordinatedProviderSnapshotRefreshService(coordinator));
    }

    private record Fixture(MutableClock clock, ProviderSnapshotQueryService query,
                           ProviderSnapshotRefreshService refresh) {
    }

    private static final class MutableClock extends Clock {
        private Instant current;
        private MutableClock(Instant current) { this.current = current; }
        void advance(Duration duration) { current = current.plus(duration); }
        @Override public ZoneId getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(ZoneId zone) { return this; }
        @Override public Instant instant() { return current; }
    }
}
