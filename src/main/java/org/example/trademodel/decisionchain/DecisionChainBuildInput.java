package org.example.trademodel.decisionchain;

import org.example.trademodel.analysisrun.AnalysisRunTriggerType;
import org.example.trademodel.vo.DecisionBundleVO;
import org.example.trademodel.vo.EvidenceItemVO;
import org.example.trademodel.vo.ExecutionPlanVO;
import org.example.trademodel.vo.ScoreItemVO;

import java.util.List;

public record DecisionChainBuildInput(
        String analysisId,
        String traceId,
        String symbol,
        String timeframe,
        Integer dataQualityScore,
        DecisionBundleVO decision,
        ExecutionPlanVO rulePlan,
        List<EvidenceItemVO> evidence,
        List<ScoreItemVO> scores,
        AnalysisRunTriggerType triggerType) {
}
