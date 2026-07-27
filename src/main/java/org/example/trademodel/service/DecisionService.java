package org.example.trademodel.service;

import org.example.trademodel.vo.DecisionResultVO;
import org.example.trademodel.vo.LightSystemStatusVO;

import java.util.List;

public interface DecisionService {
    LightSystemStatusVO getLightSystemStatus();

    List<DecisionResultVO> getLatestDecisionResults(int limit);

    List<DecisionResultVO> getLatestDecisionResultsForUser(Long userId, int limit);

    DecisionResultVO getLatestDecisionResultBySymbol(String symbol);

    DecisionResultVO getLatestDecisionResultBySymbolForUser(Long userId, String symbol);

    int countOpenPositions();

    int countOpenPositionsForUser(Long userId);
}
