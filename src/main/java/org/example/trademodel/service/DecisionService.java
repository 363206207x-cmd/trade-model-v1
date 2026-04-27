package org.example.trademodel.service;

import org.example.trademodel.vo.DecisionResultVO;
import org.example.trademodel.vo.LightSystemStatusVO;

import java.util.List;

public interface DecisionService {
    LightSystemStatusVO getLightSystemStatus();

    List<DecisionResultVO> getLatestDecisionResults(int limit);

    DecisionResultVO getLatestDecisionResultBySymbol(String symbol);

    int countOpenPositions();
}
