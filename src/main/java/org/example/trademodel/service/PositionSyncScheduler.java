package org.example.trademodel.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class PositionSyncScheduler {

    private final PositionSyncService positionSyncService;
    private final boolean schedulersEnabled;
    private final boolean positionSyncSchedulerEnabled;

    public PositionSyncScheduler(PositionSyncService positionSyncService,
                                 @Value("${trade-model.schedulers.enabled:true}") boolean schedulersEnabled,
                                 @Value("${trade-model.schedulers.position-sync.enabled:true}") boolean positionSyncSchedulerEnabled) {
        this.positionSyncService = positionSyncService;
        this.schedulersEnabled = schedulersEnabled;
        this.positionSyncSchedulerEnabled = positionSyncSchedulerEnabled;
    }

    @Scheduled(initialDelay = 15000, fixedRate = 30000)
    public void syncPositionsScheduled() {
        if (!scheduledExecutionEnabled()) {
            return;
        }
        positionSyncService.syncPositions();
    }

    private boolean scheduledExecutionEnabled() {
        return schedulersEnabled && positionSyncSchedulerEnabled;
    }
}
