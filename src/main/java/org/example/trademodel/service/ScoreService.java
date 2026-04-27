package org.example.trademodel.service;

import org.example.trademodel.vo.AssetAnalysisVO;
import org.example.trademodel.vo.MarketEnvironmentVO;
import org.example.trademodel.vo.ScoreBriefVO;
import org.example.trademodel.vo.ScoreItemVO;
import java.util.List;

public interface ScoreService {
    List<ScoreItemVO> buildScoreList(AssetAnalysisVO assetAnalysis, MarketEnvironmentVO marketEnv);
    List<ScoreItemVO> buildScoreListFromEnvironment(MarketEnvironmentVO env);
    List<ScoreBriefVO> listTopScoreBriefByAnalysisId(String analysisId);
}
