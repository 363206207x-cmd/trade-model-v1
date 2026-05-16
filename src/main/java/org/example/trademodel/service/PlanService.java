package org.example.trademodel.service;

import org.example.trademodel.dto.planboundary.SourceTraceDTO;
import org.example.trademodel.vo.AssetAnalysisVO;
import org.example.trademodel.vo.DecisionBundleVO;
import org.example.trademodel.vo.ExecutionPlanVO;
import org.example.trademodel.vo.MarketEnvironmentVO;
import org.example.trademodel.vo.ScoreItemVO;
import java.util.List;

public interface PlanService {

    ExecutionPlanVO buildExecutionPlanFromEnvironment(MarketEnvironmentVO env);

    ExecutionPlanVO generateExecutionPlan(
            DecisionBundleVO decision,
            List<ScoreItemVO> scoreList,
            MarketEnvironmentVO marketEnvironment,
            AssetAnalysisVO analysisContext);

    ExecutionPlanVO generateExecutionPlan(
            DecisionBundleVO decision,
            List<ScoreItemVO> scoreList,
            MarketEnvironmentVO marketEnvironment,
            AssetAnalysisVO analysisContext,
            SourceTraceDTO sourceTrace);

    ExecutionPlanVO generateExecutionPlan(
            DecisionBundleVO decision,
            List<ScoreItemVO> scoreList,
            MarketEnvironmentVO marketEnvironment,
            AssetAnalysisVO analysisContext,
            SourceTraceDTO sourceTrace,
            org.example.trademodel.vo.DashboardDetailResponseVO.RiskActionGuardDisplayVO riskActionGuardDisplay);
}
