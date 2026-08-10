package org.example.trademodel.entity;

import java.time.LocalDateTime;

public class ExecutionPlanCandidateDO {
    private String candidateId;
    private String opportunityId;
    private String analysisId;
    private String traceId;
    private String ruleDirection;
    private String ruleConfidence;
    private String ruleRisk;
    private String candidateDirection;
    private String planMode;
    private String confidenceLevel;
    private String riskLevel;
    private Boolean worthOpening;
    private String recommendedAction;
    private String entryZone;
    private String stopLoss;
    private String takeProfitRules;
    private String leverageSuggestion;
    private String positionSuggestion;
    private String invalidCondition;
    private String validity;
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
    public String getCandidateDirection() { return candidateDirection; }
    public void setCandidateDirection(String candidateDirection) { this.candidateDirection = candidateDirection; }
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
    public String getEntryZone() { return entryZone; }
    public void setEntryZone(String entryZone) { this.entryZone = entryZone; }
    public String getStopLoss() { return stopLoss; }
    public void setStopLoss(String stopLoss) { this.stopLoss = stopLoss; }
    public String getTakeProfitRules() { return takeProfitRules; }
    public void setTakeProfitRules(String takeProfitRules) { this.takeProfitRules = takeProfitRules; }
    public String getLeverageSuggestion() { return leverageSuggestion; }
    public void setLeverageSuggestion(String leverageSuggestion) { this.leverageSuggestion = leverageSuggestion; }
    public String getPositionSuggestion() { return positionSuggestion; }
    public void setPositionSuggestion(String positionSuggestion) { this.positionSuggestion = positionSuggestion; }
    public String getInvalidCondition() { return invalidCondition; }
    public void setInvalidCondition(String invalidCondition) { this.invalidCondition = invalidCondition; }
    public String getValidity() { return validity; }
    public void setValidity(String validity) { this.validity = validity; }
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
