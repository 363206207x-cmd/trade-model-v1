package org.example.trademodel.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class PositionMonitorScheduler {

    private static final Logger log = LoggerFactory.getLogger(PositionMonitorScheduler.class);

    private final PositionMonitorService positionMonitorService;
    private final boolean schedulersEnabled;
    private final boolean positionMonitorSchedulerEnabled;

    public PositionMonitorScheduler(
            PositionMonitorService positionMonitorService,
            @Value("${trade-model.schedulers.enabled:true}") boolean schedulersEnabled,
            @Value("${trade-model.schedulers.position-monitor.enabled:false}") boolean positionMonitorSchedulerEnabled) {
        this.positionMonitorService = positionMonitorService;
        this.schedulersEnabled = schedulersEnabled;
        this.positionMonitorSchedulerEnabled = positionMonitorSchedulerEnabled;
    }

    @Scheduled(initialDelayString = "${trade-model.schedulers.position-monitor.initial-delay-ms:15000}",
            fixedRateString = "${trade-model.schedulers.position-monitor.fixed-rate-ms:30000}")
    public void monitorOpenUserPositionsScheduled() {
        if (!scheduledExecutionEnabled()) {
            return;
        }
        try {
            positionMonitorService.monitorClaimedOpenPositionsForSystem();
        } catch (RuntimeException ex) {
            log.warn("[position-monitor-scheduler] batch skipped: {}", ex.getMessage());
        }
    }

    boolean scheduledExecutionEnabled() {
        return schedulersEnabled && positionMonitorSchedulerEnabled;
    }
}
