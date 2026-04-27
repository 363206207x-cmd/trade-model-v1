package org.example.trademodel.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
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
