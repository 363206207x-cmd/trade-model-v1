package org.example.trademodel.service;

import org.example.trademodel.analysisrun.AnalysisRunProperties;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MarketDataSchedulerRuntimeStatusTest {

    @Test
    void successfulCyclePublishesFreshHeartbeatAndConfiguredNextScan() {
        MutableClock clock = new MutableClock(Instant.parse("2026-09-03T01:00:00Z"));
        AnalysisRunProperties properties = enabledProperties(60_000L);
        AnalysisSchedulerService analysis = mock(AnalysisSchedulerService.class);
        when(analysis.runScheduledCycle()).thenAnswer(invocation -> {
            clock.advance(Duration.ofSeconds(5));
            return List.of();
        });
        MarketDataScheduler scheduler = new MarketDataScheduler(analysis, properties, true, true, clock);

        scheduler.fetchRealMarketDataScheduled();

        MarketDataScheduler.RuntimeStatus status = scheduler.runtimeStatus();
        assertThat(status.enabled()).isTrue();
        assertThat(status.running()).isFalse();
        assertThat(status.heartbeatFresh()).isTrue();
        assertThat(status.startedAt()).isEqualTo(Instant.parse("2026-09-03T01:00:00Z"));
        assertThat(status.completedAt()).isEqualTo(Instant.parse("2026-09-03T01:00:05Z"));
        assertThat(status.nextScheduledAt()).isEqualTo(Instant.parse("2026-09-03T01:01:05Z"));
        assertThat(status.result()).isEqualTo("SUCCESS");
        assertThat(status.failureReason()).isNull();
        assertThat(status.heartbeatTimeout()).isEqualTo(Duration.ofMinutes(3));
    }

    @Test
    void runningCycleIsVisibleBeforeCompletion() {
        MutableClock clock = new MutableClock(Instant.parse("2026-09-03T02:00:00Z"));
        AnalysisRunProperties properties = enabledProperties(60_000L);
        AnalysisSchedulerService analysis = mock(AnalysisSchedulerService.class);
        MarketDataScheduler scheduler = new MarketDataScheduler(analysis, properties, true, true, clock);
        when(analysis.runScheduledCycle()).thenAnswer(invocation -> {
            MarketDataScheduler.RuntimeStatus status = scheduler.runtimeStatus();
            assertThat(status.running()).isTrue();
            assertThat(status.heartbeatFresh()).isTrue();
            assertThat(status.startedAt()).isEqualTo(clock.instant());
            return List.of();
        });

        scheduler.fetchRealMarketDataScheduled();

        assertThat(scheduler.runtimeStatus().running()).isFalse();
    }

    @Test
    void failedCyclePublishesSafeFailureWithoutExceptionMessage() {
        MutableClock clock = new MutableClock(Instant.parse("2026-09-03T03:00:00Z"));
        AnalysisRunProperties properties = enabledProperties(60_000L);
        AnalysisSchedulerService analysis = mock(AnalysisSchedulerService.class);
        when(analysis.runScheduledCycle()).thenThrow(new IllegalStateException("sensitive provider detail"));
        MarketDataScheduler scheduler = new MarketDataScheduler(analysis, properties, true, true, clock);

        assertThatThrownBy(scheduler::fetchRealMarketDataScheduled)
                .isInstanceOf(IllegalStateException.class);

        MarketDataScheduler.RuntimeStatus status = scheduler.runtimeStatus();
        assertThat(status.result()).isEqualTo("FAILED");
        assertThat(status.failureReason()).isEqualTo("SCHEDULED_SCAN_FAILED:IllegalStateException")
                .doesNotContain("sensitive provider detail");
        assertThat(status.heartbeatFresh()).isTrue();
    }

    @Test
    void heartbeatFreshnessUsesThreeTimesConfiguredFixedDelay() {
        MutableClock clock = new MutableClock(Instant.parse("2026-09-03T04:00:00Z"));
        AnalysisRunProperties properties = enabledProperties(20_000L);
        AnalysisSchedulerService analysis = mock(AnalysisSchedulerService.class);
        when(analysis.runScheduledCycle()).thenReturn(List.of());
        MarketDataScheduler scheduler = new MarketDataScheduler(analysis, properties, true, true, clock);
        scheduler.fetchRealMarketDataScheduled();

        assertThat(scheduler.runtimeStatus(clock.instant().plusSeconds(60)).heartbeatFresh()).isTrue();
        assertThat(scheduler.runtimeStatus(clock.instant().plusSeconds(61)).heartbeatFresh()).isFalse();
        assertThat(scheduler.runtimeStatus().heartbeatTimeout()).isEqualTo(Duration.ofSeconds(60));
    }

    @Test
    void disabledSchedulerHasNoFabricatedHeartbeat() {
        MutableClock clock = new MutableClock(Instant.parse("2026-09-03T05:00:00Z"));
        AnalysisRunProperties properties = enabledProperties(60_000L);
        AnalysisSchedulerService analysis = mock(AnalysisSchedulerService.class);
        MarketDataScheduler scheduler = new MarketDataScheduler(analysis, properties, false, true, clock);

        scheduler.fetchRealMarketDataScheduled();

        MarketDataScheduler.RuntimeStatus status = scheduler.runtimeStatus();
        assertThat(status.enabled()).isFalse();
        assertThat(status.heartbeatAt()).isNull();
        assertThat(status.completedAt()).isNull();
        assertThat(status.result()).isNull();
    }

    @Test
    void disabledAnalysisCadenceDoesNotPublishANoopScanHeartbeat() {
        MutableClock clock = new MutableClock(Instant.parse("2026-09-03T06:00:00Z"));
        AnalysisRunProperties properties = enabledProperties(60_000L);
        properties.getScheduler().setEnabled(false);
        AnalysisSchedulerService analysis = mock(AnalysisSchedulerService.class);
        MarketDataScheduler scheduler = new MarketDataScheduler(analysis, properties, true, true, clock);

        scheduler.fetchRealMarketDataScheduled();

        MarketDataScheduler.RuntimeStatus status = scheduler.runtimeStatus();
        assertThat(status.enabled()).isFalse();
        assertThat(status.heartbeatAt()).isNull();
        assertThat(status.completedAt()).isNull();
        assertThat(status.result()).isNull();
    }

    private static AnalysisRunProperties enabledProperties(long fixedDelayMs) {
        AnalysisRunProperties properties = new AnalysisRunProperties();
        properties.getScheduler().setEnabled(true);
        properties.getScheduler().setFixedDelayMs(fixedDelayMs);
        return properties;
    }

    private static final class MutableClock extends Clock {
        private Instant now;

        private MutableClock(Instant now) {
            this.now = now;
        }

        private void advance(Duration duration) {
            now = now.plus(duration);
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
            return now;
        }
    }
}
