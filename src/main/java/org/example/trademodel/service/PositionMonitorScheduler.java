package org.example.trademodel.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.example.trademodel.positionmonitor.PositionMonitorBatchResultDTO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;
import org.example.trademodel.v41.DashboardLiveEvent;
import org.example.trademodel.v41.DashboardLiveEventService;

import java.time.Instant;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

@Component
public class PositionMonitorScheduler {

    private static final Logger log = LoggerFactory.getLogger(PositionMonitorScheduler.class);

    private final PositionMonitorService positionMonitorService;
    private final boolean schedulersEnabled;
    private final boolean positionMonitorSchedulerEnabled;
    private final ConcurrentMap<Long, Long> pendingInitialMonitors = new ConcurrentHashMap<>();
    private final AtomicBoolean immediateMonitorRequested = new AtomicBoolean();
    private DashboardLiveEventService dashboardLiveEventService;

    public PositionMonitorScheduler(
            PositionMonitorService positionMonitorService,
            @Value("${trade-model.schedulers.enabled:false}") boolean schedulersEnabled,
            @Value("${trade-model.schedulers.position-monitor.enabled:false}") boolean positionMonitorSchedulerEnabled) {
        this.positionMonitorService = positionMonitorService;
        this.schedulersEnabled = schedulersEnabled;
        this.positionMonitorSchedulerEnabled = positionMonitorSchedulerEnabled;
    }

    @Autowired
    void setDashboardLiveEventService(DashboardLiveEventService value) {
        this.dashboardLiveEventService = value;
    }

    @Scheduled(initialDelayString = "${trade-model.schedulers.position-monitor.initial-delay-ms:15000}",
            fixedRateString = "${trade-model.schedulers.position-monitor.fixed-rate-ms:30000}")
    public void monitorOpenUserPositionsScheduled() {
        monitorOpenPositions("PERIODIC_30S");
    }

    @Scheduled(initialDelayString = "${trade-model.schedulers.position-monitor.immediate-delay-ms:1000}",
            fixedDelayString = "${trade-model.schedulers.position-monitor.immediate-rate-ms:1000}")
    public void monitorImmediateRiskRequestsScheduled() {
        if (immediateMonitorRequested.compareAndSet(true, false)) {
            monitorOpenPositions("REALTIME_SHOCK");
        }
    }

    public void requestImmediateRiskMonitor(String reasonCode) {
        if (!scheduledExecutionEnabled()) return;
        immediateMonitorRequested.set(true);
        log.info("[position-monitor-scheduler] immediate read-only monitor requested reason={}",
                sanitizedReason(reasonCode));
    }

    private synchronized void monitorOpenPositions(String trigger) {
        if (!scheduledExecutionEnabled()) {
            return;
        }
        try {
            PositionMonitorBatchResultDTO batch = positionMonitorService.monitorClaimedOpenPositionsForSystem();
            if (batch == null) {
                log.warn("[position-monitor-scheduler] batch completed without a result summary");
                return;
            }
            log.info("[position-monitor-scheduler] batch completed trigger={} total={} success={} failure={} blocked={}",
                    trigger,
                    batch.getTotalCount(), batch.getSuccessCount(),
                    batch.getFailureCount(), batch.getBlockedCount());
            publishRefreshEvent(batch);
            if (batch.getFailureCount() > 0) {
                log.warn("[position-monitor-scheduler] failure summary={}", failureReasonSummary(batch));
            }
        } catch (RuntimeException ex) {
            log.warn("[position-monitor-scheduler] batch skipped: {}", ex.getMessage());
        }
    }

    private void publishRefreshEvent(PositionMonitorBatchResultDTO batch) {
        if (dashboardLiveEventService == null) return;
        long version = System.currentTimeMillis();
        dashboardLiveEventService.publish(new DashboardLiveEvent(
                "position-monitor-" + version, "POSITION_MONITOR_UPDATED", "SYSTEM", version,
                Instant.now(), Instant.now(), Map.of("refreshRequired", true,
                "successCount", batch.getSuccessCount(), "failureCount", batch.getFailureCount(),
                "version", "V41-POSITION-RISK-VECTOR-1")));
    }

    public void requestInitialMonitor(Long positionId, Long userId) {
        if (positionId == null || positionId <= 0) {
            throw new IllegalArgumentException("positionId is required");
        }
        if (userId == null || userId <= 0) {
            throw new IllegalArgumentException("userId is required");
        }
        pendingInitialMonitors.put(positionId, userId);
    }

    @Scheduled(initialDelayString = "${trade-model.schedulers.position-monitor.initial-request-delay-ms:1000}",
            fixedDelayString = "${trade-model.schedulers.position-monitor.initial-request-rate-ms:1000}")
    public void monitorInitialRequestsScheduled() {
        if (!scheduledExecutionEnabled() || pendingInitialMonitors.isEmpty()) {
            return;
        }
        var claimed = java.util.Map.copyOf(pendingInitialMonitors);
        for (var request : claimed.entrySet()) {
            try {
                positionMonitorService.monitorUserPositionForUser(request.getKey(), request.getValue());
                pendingInitialMonitors.remove(request.getKey(), request.getValue());
            } catch (RuntimeException ex) {
                log.warn("[position-monitor-scheduler] initial position retained for retry positionId={}",
                        request.getKey());
            }
        }
    }

    int pendingInitialMonitorCount() {
        return pendingInitialMonitors.size();
    }

    boolean scheduledExecutionEnabled() {
        return schedulersEnabled && positionMonitorSchedulerEnabled;
    }

    static String failureReasonSummary(PositionMonitorBatchResultDTO batch) {
        if (batch == null || batch.getFailures().isEmpty()) {
            return "NONE";
        }
        Map<String, Long> counts = batch.getFailures().stream()
                .collect(Collectors.groupingBy(
                        failure -> sanitizedReason(failure == null ? null : failure.getReason()),
                        TreeMap::new,
                        Collectors.counting()));
        return counts.entrySet().stream()
                .map(entry -> entry.getKey() + "=" + entry.getValue())
                .collect(Collectors.joining(","));
    }

    private static String sanitizedReason(String reason) {
        if (reason == null || reason.isBlank()) {
            return "UNKNOWN";
        }
        String token = reason.trim().toUpperCase(java.util.Locale.ROOT)
                .replaceAll("[^A-Z0-9:_-]", "_");
        return token.length() <= 120 ? token : token.substring(0, 120);
    }
}
