package org.example.trademodel.service;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(
        name = {"trade-model.schedulers.enabled", "trade-model.schedulers.position-sync.enabled"},
        havingValue = "true",
        matchIfMissing = true)
public class PositionSyncScheduler {

    private final PositionSyncService positionSyncService;

    public PositionSyncScheduler(PositionSyncService positionSyncService) {
        this.positionSyncService = positionSyncService;
    }

    @Scheduled(initialDelay = 15000, fixedRate = 30000)
    public void syncPositionsScheduled() {
        positionSyncService.syncPositions();
    }
}
