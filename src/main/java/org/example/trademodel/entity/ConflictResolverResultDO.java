package org.example.trademodel.entity;

import java.time.LocalDateTime;

public class ConflictResolverResultDO {
    private String resolverResultId;
    private String candidateId;
    private String analysisId;
    private String traceId;
    private String ruleDirection;
    private String ruleConfidence;
    private String ruleRisk;
    private String rulePlanMode;
    private Boolean ruleCanExecute;
    private Integer dataQualityScore;
    private Integer confusedScore;
    private String accountRiskState;
    private String geminiReviewJson;
    private String grokChallengeJson;
    private String conflictLevel;
    private Integer conflictScore;
    private String planModeBefore;
    private String planModeAfter;
    private String confidenceBefore;
    private String confidenceAfter;
    private String riskBefore;
    private String riskAfter;
    private String biasBefore;
    private String biasAfter;
    private String adjustmentReason;
    private String downgradeReason;
    private String recoveryCondition;
    private Boolean confusedDecision;
    private String ruleVetoReason;
    private Boolean ruleDirectionPreserved = true;
    private LocalDateTime createdAt;

    public String getResolverResultId() { return resolverResultId; }
    public void setResolverResultId(String value) { this.resolverResultId = value; }
    public String getCandidateId() { return candidateId; }
    public void setCandidateId(String value) { this.candidateId = value; }
    public String getAnalysisId() { return analysisId; }
    public void setAnalysisId(String value) { this.analysisId = value; }
    public String getTraceId() { return traceId; }
    public void setTraceId(String value) { this.traceId = value; }
    public String getRuleDirection() { return ruleDirection; }
    public void setRuleDirection(String value) { this.ruleDirection = value; }
    public String getRuleConfidence() { return ruleConfidence; }
    public void setRuleConfidence(String value) { this.ruleConfidence = value; }
    public String getRuleRisk() { return ruleRisk; }
    public void setRuleRisk(String value) { this.ruleRisk = value; }
    public String getRulePlanMode() { return rulePlanMode; }
    public void setRulePlanMode(String value) { this.rulePlanMode = value; }
    public Boolean getRuleCanExecute() { return ruleCanExecute; }
    public void setRuleCanExecute(Boolean value) { this.ruleCanExecute = value; }
    public Integer getDataQualityScore() { return dataQualityScore; }
    public void setDataQualityScore(Integer value) { this.dataQualityScore = value; }
    public Integer getConfusedScore() { return confusedScore; }
    public void setConfusedScore(Integer value) { this.confusedScore = value; }
    public String getAccountRiskState() { return accountRiskState; }
    public void setAccountRiskState(String value) { this.accountRiskState = value; }
    public String getGeminiReviewJson() { return geminiReviewJson; }
    public void setGeminiReviewJson(String value) { this.geminiReviewJson = value; }
    public String getGrokChallengeJson() { return grokChallengeJson; }
    public void setGrokChallengeJson(String value) { this.grokChallengeJson = value; }
    public String getConflictLevel() { return conflictLevel; }
    public void setConflictLevel(String value) { this.conflictLevel = value; }
    public Integer getConflictScore() { return conflictScore; }
    public void setConflictScore(Integer value) { this.conflictScore = value; }
    public String getPlanModeBefore() { return planModeBefore; }
    public void setPlanModeBefore(String value) { this.planModeBefore = value; }
    public String getPlanModeAfter() { return planModeAfter; }
    public void setPlanModeAfter(String value) { this.planModeAfter = value; }
    public String getConfidenceBefore() { return confidenceBefore; }
    public void setConfidenceBefore(String value) { this.confidenceBefore = value; }
    public String getConfidenceAfter() { return confidenceAfter; }
    public void setConfidenceAfter(String value) { this.confidenceAfter = value; }
    public String getRiskBefore() { return riskBefore; }
    public void setRiskBefore(String value) { this.riskBefore = value; }
    public String getRiskAfter() { return riskAfter; }
    public void setRiskAfter(String value) { this.riskAfter = value; }
    public String getBiasBefore() { return biasBefore; }
    public void setBiasBefore(String value) { this.biasBefore = value; }
    public String getBiasAfter() { return biasAfter; }
    public void setBiasAfter(String value) { this.biasAfter = value; }
    public String getAdjustmentReason() { return adjustmentReason; }
    public void setAdjustmentReason(String value) { this.adjustmentReason = value; }
    public String getDowngradeReason() { return downgradeReason; }
    public void setDowngradeReason(String value) { this.downgradeReason = value; }
    public String getRecoveryCondition() { return recoveryCondition; }
    public void setRecoveryCondition(String value) { this.recoveryCondition = value; }
    public Boolean getConfusedDecision() { return confusedDecision; }
    public void setConfusedDecision(Boolean value) { this.confusedDecision = value; }
    public String getRuleVetoReason() { return ruleVetoReason; }
    public void setRuleVetoReason(String value) { this.ruleVetoReason = value; }
    public Boolean getRuleDirectionPreserved() { return ruleDirectionPreserved; }
    public void setRuleDirectionPreserved(Boolean value) { this.ruleDirectionPreserved = value; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime value) { this.createdAt = value; }
}
