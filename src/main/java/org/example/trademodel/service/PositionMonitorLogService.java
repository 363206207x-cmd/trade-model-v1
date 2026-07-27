package org.example.trademodel.service;

import org.example.trademodel.positionmonitorlog.PositionMonitorLogDTO;
import org.example.trademodel.positionmonitorlog.RecordPositionMonitorLogCommand;

import java.util.List;

public interface PositionMonitorLogService {
    PositionMonitorLogDTO recordMonitorRunForUser(Long userId, RecordPositionMonitorLogCommand command);

    PositionMonitorLogDTO recordMonitorRunForSystem(RecordPositionMonitorLogCommand command);

    PositionMonitorLogDTO findByIdForSystem(Long logId);

    List<PositionMonitorLogDTO> listByPositionIdForUser(Long userId, Long positionId, Integer limit);

    List<PositionMonitorLogDTO> listAllByPositionIdForUserReview(Long userId, Long positionId);

    List<PositionMonitorLogDTO> listByPositionIdForSystem(Long positionId, Integer limit);

    List<PositionMonitorLogDTO> listAllByPositionIdForSystemReview(Long positionId);

    List<PositionMonitorLogDTO> listByAnalysisIdForSystem(String analysisId, Integer limit);
}
