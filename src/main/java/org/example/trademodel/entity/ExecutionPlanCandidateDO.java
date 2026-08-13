package org.example.trademodel.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class ExecutionPlanCandidateDO {
    private String candidateId;
    private String opportunityId;
    private String analysisId;
    private String traceId;
    private String ruleDirection;
    private String ruleConfidence;
    private String ruleRisk;
    private String rulePlanMode;
    private Boolean ruleCanExecute;
    private String candidateDirection;
    private String biasAdjustmentReason;
    private String planMode;
    private String confidenceLevel;
    private String riskLevel;
    private Boolean worthOpening;
    private String recommendedAction;
    private Long assetId;
    private String ruleVersion;
    private String opportunityType;
    private String entryLogic;
    private String entryZone;
    private String entrySource;
    private String entryReason;
    private String triggerCondition;
    private String stopLogic;
    private String stopLoss;
    private String stopSource;
    private String stopReason;
    private String targetLogic;
    private String takeProfitRules;
    private String targetSource;
    private String targetReason;
    private String addPositionCondition;
    private String reducePositionCondition;
    private String abandonCondition;
    private String leverageSuggestion;
    private String positionSuggestion;
    private String riskExplanation;
    private String invalidCondition;
    private String invalidationSource;
    private String invalidationReason;
    private BigDecimal expectedRiskReward;
    private String expectedRiskRewardSource;
    private String expectedRiskRewardReason;
    private String validity;
    private String analysisTimeframesJson;
    private String triggerTimeframe;
    private LocalDateTime validFrom;
    private LocalDateTime validUntil;
    private String holdingHorizon;
    private String revalidationRule;
    private String sourceRefsJson;
    private String evidenceRefsJson;
    private String scoreRefsJson;
    private Integer dataQuality;
    private Integer confusedScore;
    private Long accountRiskSnapshotId;
    private Integer version = 1;
    private String summary;
    private String candidateSource;
    private String candidateStatus;
    private String fallbackReason;
    private String payloadJson;
    private Boolean notFinalPlan = true;
    private Boolean notStateMachineMutation = true;
    private Boolean notUserPositionCreation = true;
    private LocalDateTime createdAt;

    public String getCandidateId() { return candidateId; }
    public void setCandidateId(String candidateId) { this.candidateId = candidateId; }
    public String getOpportunityId() { return opportunityId; }
    public void setOpportunityId(String opportunityId) { this.opportunityId = opportunityId; }
    public String getAnalysisId() { return analysisId; }
    public void setAnalysisId(String analysisId) { this.analysisId = analysisId; }
    public String getTraceId() { return traceId; }
    public void setTraceId(String traceId) { this.traceId = traceId; }
    public String getRuleDirection() { return ruleDirection; }
    public void setRuleDirection(String ruleDirection) { this.ruleDirection = ruleDirection; }
    public String getRuleConfidence() { return ruleConfidence; }
    public void setRuleConfidence(String ruleConfidence) { this.ruleConfidence = ruleConfidence; }
    public String getRuleRisk() { return ruleRisk; }
    public void setRuleRisk(String ruleRisk) { this.ruleRisk = ruleRisk; }
    public String getRulePlanMode() { return rulePlanMode; }
    public void setRulePlanMode(String value) { this.rulePlanMode = value; }
    public Boolean getRuleCanExecute() { return ruleCanExecute; }
    public void setRuleCanExecute(Boolean value) { this.ruleCanExecute = value; }
    public String getCandidateDirection() { return candidateDirection; }
    public void setCandidateDirection(String candidateDirection) { this.candidateDirection = candidateDirection; }
    public String getBiasAdjustmentReason() { return biasAdjustmentReason; }
    public void setBiasAdjustmentReason(String value) { this.biasAdjustmentReason = value; }
    public String getPlanMode() { return planMode; }
    public void setPlanMode(String planMode) { this.planMode = planMode; }
    public String getConfidenceLevel() { return confidenceLevel; }
    public void setConfidenceLevel(String confidenceLevel) { this.confidenceLevel = confidenceLevel; }
    public String getRiskLevel() { return riskLevel; }
    public void setRiskLevel(String riskLevel) { this.riskLevel = riskLevel; }
    public Boolean getWorthOpening() { return worthOpening; }
    public void setWorthOpening(Boolean worthOpening) { this.worthOpening = worthOpening; }
    public String getRecommendedAction() { return recommendedAction; }
    public void setRecommendedAction(String recommendedAction) { this.recommendedAction = recommendedAction; }
    public Long getAssetId() { return assetId; }
    public void setAssetId(Long value) { this.assetId = value; }
    public String getRuleVersion() { return ruleVersion; }
    public void setRuleVersion(String value) { this.ruleVersion = value; }
    public String getOpportunityType() { return opportunityType; }
    public void setOpportunityType(String value) { this.opportunityType = value; }
    public String getEntryLogic() { return entryLogic; }
    public void setEntryLogic(String value) { this.entryLogic = value; }
    public String getEntryZone() { return entryZone; }
    public void setEntryZone(String entryZone) { this.entryZone = entryZone; }
    public String getEntrySource() { return entrySource; }
    public void setEntrySource(String value) { this.entrySource = value; }
    public String getEntryReason() { return entryReason; }
    public void setEntryReason(String value) { this.entryReason = value; }
    public String getTriggerCondition() { return triggerCondition; }
    public void setTriggerCondition(String value) { this.triggerCondition = value; }
    public String getStopLogic() { return stopLogic; }
    public void setStopLogic(String value) { this.stopLogic = value; }
    public String getStopLoss() { return stopLoss; }
    public void setStopLoss(String stopLoss) { this.stopLoss = stopLoss; }
    public String getStopSource() { return stopSource; }
    public void setStopSource(String value) { this.stopSource = value; }
    public String getStopReason() { return stopReason; }
    public void setStopReason(String value) { this.stopReason = value; }
    public String getTargetLogic() { return targetLogic; }
    public void setTargetLogic(String value) { this.targetLogic = value; }
    public String getTakeProfitRules() { return takeProfitRules; }
    public void setTakeProfitRules(String takeProfitRules) { this.takeProfitRules = takeProfitRules; }
    public String getTargetSource() { return targetSource; }
    public void setTargetSource(String value) { this.targetSource = value; }
    public String getTargetReason() { return targetReason; }
    public void setTargetReason(String value) { this.targetReason = value; }
    public String getAddPositionCondition() { return addPositionCondition; }
    public void setAddPositionCondition(String value) { this.addPositionCondition = value; }
    public String getReducePositionCondition() { return reducePositionCondition; }
    public void setReducePositionCondition(String value) { this.reducePositionCondition = value; }
    public String getAbandonCondition() { return abandonCondition; }
    public void setAbandonCondition(String value) { this.abandonCondition = value; }
    public String getLeverageSuggestion() { return leverageSuggestion; }
    public void setLeverageSuggestion(String leverageSuggestion) { this.leverageSuggestion = leverageSuggestion; }
    public String getPositionSuggestion() { return positionSuggestion; }
    public void setPositionSuggestion(String positionSuggestion) { this.positionSuggestion = positionSuggestion; }
    public String getRiskExplanation() { return riskExplanation; }
    public void setRiskExplanation(String value) { this.riskExplanation = value; }
    public String getInvalidCondition() { return invalidCondition; }
    public void setInvalidCondition(String invalidCondition) { this.invalidCondition = invalidCondition; }
    public String getInvalidationSource() { return invalidationSource; }
    public void setInvalidationSource(String value) { this.invalidationSource = value; }
    public String getInvalidationReason() { return invalidationReason; }
    public void setInvalidationReason(String value) { this.invalidationReason = value; }
    public BigDecimal getExpectedRiskReward() { return expectedRiskReward; }
    public void setExpectedRiskReward(BigDecimal value) { this.expectedRiskReward = value; }
    public String getExpectedRiskRewardSource() { return expectedRiskRewardSource; }
    public void setExpectedRiskRewardSource(String value) { this.expectedRiskRewardSource = value; }
    public String getExpectedRiskRewardReason() { return expectedRiskRewardReason; }
    public void setExpectedRiskRewardReason(String value) { this.expectedRiskRewardReason = value; }
    public String getValidity() { return validity; }
    public void setValidity(String validity) { this.validity = validity; }
    public String getAnalysisTimeframesJson() { return analysisTimeframesJson; }
    public void setAnalysisTimeframesJson(String value) { this.analysisTimeframesJson = value; }
    public String getTriggerTimeframe() { return triggerTimeframe; }
    public void setTriggerTimeframe(String value) { this.triggerTimeframe = value; }
    public LocalDateTime getValidFrom() { return validFrom; }
    public void setValidFrom(LocalDateTime value) { this.validFrom = value; }
    public LocalDateTime getValidUntil() { return validUntil; }
    public void setValidUntil(LocalDateTime value) { this.validUntil = value; }
    public String getHoldingHorizon() { return holdingHorizon; }
    public void setHoldingHorizon(String value) { this.holdingHorizon = value; }
    public String getRevalidationRule() { return revalidationRule; }
    public void setRevalidationRule(String value) { this.revalidationRule = value; }
    public String getSourceRefsJson() { return sourceRefsJson; }
    public void setSourceRefsJson(String value) { this.sourceRefsJson = value; }
    public String getEvidenceRefsJson() { return evidenceRefsJson; }
    public void setEvidenceRefsJson(String value) { this.evidenceRefsJson = value; }
    public String getScoreRefsJson() { return scoreRefsJson; }
    public void setScoreRefsJson(String value) { this.scoreRefsJson = value; }
    public Integer getDataQuality() { return dataQuality; }
    public void setDataQuality(Integer value) { this.dataQuality = value; }
    public Integer getConfusedScore() { return confusedScore; }
    public void setConfusedScore(Integer value) { this.confusedScore = value; }
    public Long getAccountRiskSnapshotId() { return accountRiskSnapshotId; }
    public void setAccountRiskSnapshotId(Long value) { this.accountRiskSnapshotId = value; }
    public Integer getVersion() { return version; }
    public void setVersion(Integer value) { this.version = value; }
    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }
    public String getCandidateSource() { return candidateSource; }
    public void setCandidateSource(String candidateSource) { this.candidateSource = candidateSource; }
    public String getCandidateStatus() { return candidateStatus; }
    public void setCandidateStatus(String candidateStatus) { this.candidateStatus = candidateStatus; }
    public String getFallbackReason() { return fallbackReason; }
    public void setFallbackReason(String fallbackReason) { this.fallbackReason = fallbackReason; }
    public String getPayloadJson() { return payloadJson; }
    public void setPayloadJson(String payloadJson) { this.payloadJson = payloadJson; }
    public Boolean getNotFinalPlan() { return notFinalPlan; }
    public void setNotFinalPlan(Boolean notFinalPlan) { this.notFinalPlan = notFinalPlan; }
    public Boolean getNotStateMachineMutation() { return notStateMachineMutation; }
    public void setNotStateMachineMutation(Boolean value) { this.notStateMachineMutation = value; }
    public Boolean getNotUserPositionCreation() { return notUserPositionCreation; }
    public void setNotUserPositionCreation(Boolean value) { this.notUserPositionCreation = value; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
