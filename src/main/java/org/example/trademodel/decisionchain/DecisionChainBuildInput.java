package org.example.trademodel.decisionchain;

import org.example.trademodel.analysisrun.AnalysisRunTriggerType;
import org.example.trademodel.derivatives.DerivativesBusinessAssessment;
import org.example.trademodel.providercall.snapshot.DerivativesRiskSnapshot;
import org.example.trademodel.vo.DecisionBundleVO;
import org.example.trademodel.vo.EvidenceItemVO;
import org.example.trademodel.vo.ExecutionPlanVO;
import org.example.trademodel.vo.ScoreItemVO;
import org.example.trademodel.entity.TmAccountRiskSnapshotDO;

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
        AnalysisRunTriggerType triggerType,
        String ownerType,
        Long ownerId,
        Long assetId,
        String ruleVersion,
        boolean preview,
        String requestId,
        TmAccountRiskSnapshotDO accountRiskSnapshot,
        DerivativesRiskSnapshot derivativesSnapshot,
        DerivativesBusinessAssessment derivativesAssessment) {

    public DecisionChainBuildInput(String analysisId,
                                   String traceId,
                                   String symbol,
                                   String timeframe,
                                   Integer dataQualityScore,
                                   DecisionBundleVO decision,
                                   ExecutionPlanVO rulePlan,
                                   List<EvidenceItemVO> evidence,
                                   List<ScoreItemVO> scores,
                                   AnalysisRunTriggerType triggerType,
                                   String ownerType,
                                   Long ownerId,
                                   Long assetId,
                                   String ruleVersion,
                                   boolean preview,
                                   String requestId,
                                   TmAccountRiskSnapshotDO accountRiskSnapshot) {
        this(analysisId, traceId, symbol, timeframe, dataQualityScore, decision, rulePlan,
                evidence, scores, triggerType, ownerType, ownerId, assetId, ruleVersion,
                preview, requestId, accountRiskSnapshot, null, null);
    }

    public DecisionChainBuildInput(String analysisId,
                                   String traceId,
                                   String symbol,
                                   String timeframe,
                                   Integer dataQualityScore,
                                   DecisionBundleVO decision,
                                   ExecutionPlanVO rulePlan,
                                   List<EvidenceItemVO> evidence,
                                   List<ScoreItemVO> scores,
                                   AnalysisRunTriggerType triggerType,
                                   String ownerType,
                                   Long ownerId,
                                   Long assetId,
                                   String ruleVersion,
                                   boolean preview,
                                   String requestId) {
        this(analysisId, traceId, symbol, timeframe, dataQualityScore, decision, rulePlan,
                evidence, scores, triggerType, ownerType, ownerId, assetId, ruleVersion,
                preview, requestId, null);
    }

    public DecisionChainBuildInput(String analysisId,
                                   String traceId,
                                   String symbol,
                                   String timeframe,
                                   Integer dataQualityScore,
                                   DecisionBundleVO decision,
                                   ExecutionPlanVO rulePlan,
                                   List<EvidenceItemVO> evidence,
                                   List<ScoreItemVO> scores,
                                   AnalysisRunTriggerType triggerType,
                                   String ownerType,
                                   Long ownerId,
                                   Long assetId,
                                   String ruleVersion,
                                   boolean preview) {
        this(analysisId, traceId, symbol, timeframe, dataQualityScore, decision, rulePlan,
                evidence, scores, triggerType, ownerType, ownerId, assetId, ruleVersion,
                preview, null, null);
    }

    public DecisionChainBuildInput(String analysisId,
                                   String traceId,
                                   String symbol,
                                   String timeframe,
                                   Integer dataQualityScore,
                                   DecisionBundleVO decision,
                                   ExecutionPlanVO rulePlan,
                                   List<EvidenceItemVO> evidence,
                                   List<ScoreItemVO> scores,
                                   AnalysisRunTriggerType triggerType) {
        this(analysisId, traceId, symbol, timeframe, dataQualityScore, decision, rulePlan,
                evidence, scores, triggerType, "SYSTEM", 0L, null,
                "FUNDAMENTAL_AI_V4_1", false, null, null);
    }
}
