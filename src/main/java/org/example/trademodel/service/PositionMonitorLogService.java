package org.example.trademodel.service;

import org.example.trademodel.positionmonitorlog.PositionMonitorLogDTO;
import org.example.trademodel.positionmonitorlog.RecordPositionMonitorLogCommand;

import java.util.List;

public interface PositionMonitorLogService {
    PositionMonitorLogDTO recordMonitorRun(RecordPositionMonitorLogCommand command);

    PositionMonitorLogDTO findById(Long logId);

    List<PositionMonitorLogDTO> listByPositionId(Long positionId, Integer limit);

    List<PositionMonitorLogDTO> listAllByPositionIdForReview(Long positionId);

    List<PositionMonitorLogDTO> listByAnalysisId(String analysisId, Integer limit);
}
