package org.example.trademodel.service;

import org.example.trademodel.positionmonitor.PositionMonitorBatchResultDTO;
import org.example.trademodel.positionmonitor.PositionMonitorResultDTO;

public interface PositionMonitorService {
    PositionMonitorResultDTO monitorUserPositionForUser(Long positionId, Long userId);

    PositionMonitorBatchResultDTO monitorClaimedOpenPositionsForSystem();
}
