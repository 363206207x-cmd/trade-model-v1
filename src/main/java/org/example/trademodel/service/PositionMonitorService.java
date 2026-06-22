package org.example.trademodel.service;

import org.example.trademodel.positionmonitor.PositionMonitorBatchResultDTO;
import org.example.trademodel.positionmonitor.PositionMonitorResultDTO;

public interface PositionMonitorService {
    PositionMonitorResultDTO monitorUserPosition(Long positionId);

    PositionMonitorBatchResultDTO monitorOpenUserPositions();
}
