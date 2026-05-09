package org.example.trademodel.service;

import org.example.trademodel.vo.PositionMonitorOpenRowVO;

import java.util.List;

public interface PositionMonitorService {
    PositionMonitorOpenRowVO run(String positionId);

    PositionMonitorOpenRowVO evaluateForPosition(String positionId, boolean forcePersist);

    void evaluateForSymbol(String symbol);

    List<PositionMonitorOpenRowVO> listOpenManualPositions();
}

