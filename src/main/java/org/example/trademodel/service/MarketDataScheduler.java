package org.example.trademodel.service;

import org.example.trademodel.analysisrun.AnalysisRunProperties;
import org.example.trademodel.analysisrun.AnalysisRunResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Component
public class MarketDataScheduler {

    private static final Logger log = LoggerFactory.getLogger(MarketDataScheduler.class);

    private final AnalysisSchedulerService analysisSchedulerService;
    private final AnalysisRunProperties properties;
    private final boolean schedulersEnabled;
    private final boolean marketDataSchedulerEnabled;
    private final Clock clock;
    private volatile CycleSnapshot cycleSnapshot = CycleSnapshot.empty();

    @Autowired
    public MarketDataScheduler(AnalysisSchedulerService analysisSchedulerService,
                               AnalysisRunProperties properties,
                               @Value("${trade-model.schedulers.enabled:false}") boolean schedulersEnabled,
                               @Value("${trade-model.schedulers.market-data.enabled:false}") boolean marketDataSchedulerEnabled) {
        this(analysisSchedulerService, properties, schedulersEnabled, marketDataSchedulerEnabled, Clock.systemUTC());
    }

    MarketDataScheduler(AnalysisSchedulerService analysisSchedulerService,
                        AnalysisRunProperties properties,
                        boolean schedulersEnabled,
                        boolean marketDataSchedulerEnabled,
                        Clock clock) {
        this.analysisSchedulerService = analysisSchedulerService;
        this.properties = properties;
        this.schedulersEnabled = schedulersEnabled;
        this.marketDataSchedulerEnabled = marketDataSchedulerEnabled;
        this.clock = clock == null ? Clock.systemUTC() : clock;
    }

    @Scheduled(
            initialDelayString = "${trade-model.analysis.scheduler.initial-delay-ms:60000}",
            fixedDelayString = "${trade-model.analysis.scheduler.fixed-delay-ms:60000}")
    public void fetchRealMarketDataScheduled() {
        if (!scheduledExecutionEnabled()) {
            return;
        }
        if (!properties.getScheduler().isEnabled()) {
            log.debug("analysis scheduler disabled by default; skipping scheduled analysis cycle");
            return;
        }
        Instant startedAt = clock.instant();
        CycleSnapshot before = cycleSnapshot;
        cycleSnapshot = before.started(startedAt);
        try {
            List<AnalysisRunResult> results = analysisSchedulerService.runScheduledCycle();
            int completedResultCount = results == null ? 0 : results.size();
            Instant completedAt = clock.instant();
            cycleSnapshot = cycleSnapshot.completed(
                    completedAt, "SUCCESS", null, completedResultCount, fixedDelay());
            log.info("analysis scheduler cycle completed count={}", completedResultCount);
        } catch (RuntimeException failure) {
            Instant completedAt = clock.instant();
            String failureReason = safeFailureReason(failure);
            cycleSnapshot = cycleSnapshot.completed(
                    completedAt, "FAILED", failureReason, 0, fixedDelay());
            log.warn("analysis scheduler cycle failed reason={}", failureReason);
            throw failure;
        }
    }

    private boolean scheduledExecutionEnabled() {
        return schedulersEnabled
                && marketDataSchedulerEnabled
                && properties.getScheduler().isEnabled();
    }

    public RuntimeStatus runtimeStatus() {
        return runtimeStatus(clock.instant());
    }

    public RuntimeStatus runtimeStatus(Instant now) {
        Instant effectiveNow = now == null ? clock.instant() : now;
        CycleSnapshot snapshot = cycleSnapshot;
        Duration timeout = heartbeatTimeout();
        boolean enabled = scheduledExecutionEnabled();
        boolean heartbeatFresh = snapshot.heartbeatAt() != null
                && !effectiveNow.isAfter(snapshot.heartbeatAt().plus(timeout));
        return new RuntimeStatus(
                enabled,
                snapshot.running(),
                heartbeatFresh,
                snapshot.heartbeatAt(),
                snapshot.startedAt(),
                snapshot.completedAt(),
                snapshot.nextScheduledAt(),
                snapshot.result(),
                snapshot.failureReason(),
                snapshot.completedResultCount(),
                timeout);
    }

    private Duration fixedDelay() {
        return Duration.ofMillis(properties.getScheduler().getFixedDelayMs());
    }

    private Duration heartbeatTimeout() {
        return fixedDelay().multipliedBy(3);
    }

    private String safeFailureReason(RuntimeException failure) {
        String type = failure == null ? null : failure.getClass().getSimpleName();
        return type == null || type.isBlank()
                ? "SCHEDULED_SCAN_FAILED"
                : "SCHEDULED_SCAN_FAILED:" + type;
    }

    public record RuntimeStatus(
            boolean enabled,
            boolean running,
            boolean heartbeatFresh,
            Instant heartbeatAt,
            Instant startedAt,
            Instant completedAt,
            Instant nextScheduledAt,
            String result,
            String failureReason,
            int completedResultCount,
            Duration heartbeatTimeout) {
    }

    private record CycleSnapshot(
            boolean running,
            Instant heartbeatAt,
            Instant startedAt,
            Instant completedAt,
            Instant nextScheduledAt,
            String result,
            String failureReason,
            int completedResultCount) {

        private static CycleSnapshot empty() {
            return new CycleSnapshot(false, null, null, null, null, null, null, 0);
        }

        private CycleSnapshot started(Instant now) {
            return new CycleSnapshot(true, now, now, completedAt, null,
                    result, failureReason, completedResultCount);
        }

        private CycleSnapshot completed(Instant now,
                                        String completionResult,
                                        String completionFailureReason,
                                        int resultCount,
                                        Duration delay) {
            return new CycleSnapshot(false, now, startedAt, now, now.plus(delay),
                    completionResult, completionFailureReason, resultCount);
        }
    }
}
