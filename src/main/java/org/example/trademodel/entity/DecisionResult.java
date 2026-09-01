package org.example.trademodel.entity;

import jakarta.persistence.Column;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;

public class DecisionResult {

    @Column(name = "decision_id")
    private String decisionId;

    @Column(name = "analysis_id")
    private String analysisId;

    @Column(name = "symbol")
    private String symbol;

    @Column(name = "market_bias_hierarchy")
    private String marketBiasHierarchy;

    @Column(name = "trade_type")
    private String tradeType;

    @Column(name = "confidence_level")
    private String confidenceLevel;

    @Column(name = "risk_level")
    private String riskLevel;

    @Column(name = "action_priority")
    private String actionPriority;

    @Column(name = "conclusion_summary")
    private String conclusionSummary;

    @Column(name = "is_worth_opening")
    private Boolean isWorthOpening;

    @Column(name = "multi_tf_convergence")
    private String multiTfConvergence;

    @Column(name = "ai_role_results")
    private String aiRoleResults;

    @Column(name = "is_adopted")
    private Boolean isAdopted;

    @Column(name = "valid_period")
    private String validPeriod;

    @Column(name = "valid_from")
    private OffsetDateTime validFrom;

    @Column(name = "expires_at")
    private OffsetDateTime expiresAt;

    @Column(name = "invalid_condition")
    private String invalidCondition;

    @Column(name = "evidence_summary")
    private String evidenceSummary;

    @Column(name = "explanation_json")
    private String explanationJson;

    @Column(name = "review_reasons")
    private String reviewReasons;

    @Column(name = "ai_conflict_level", length = 64)
    private String aiConflictLevel;

    @Column(name = "ai_conflict_score")
    private Integer aiConflictScore;

    @Column(name = "ai_plan_mode")
    private String aiPlanMode;

    private String ruleMarketBias;
    @Column(name = "validated_market_bias")
    private String validatedMarketBias;
    private String finalMarketBias;
    @Column(name = "direction_data_state")
    private String directionDataState;
    @Column(name = "data_quality_score")
    private Integer dataQualityScore;
    @Column(name = "evidence_reliability")
    private Integer evidenceReliability;
    @Column(name = "opportunity_score")
    private Integer opportunityScore;
    @Column(name = "risk_score")
    private Integer riskScore;
    @Column(name = "final_confidence")
    private Integer finalConfidence;
    @Column(name = "one_hour_opportunity_quality")
    private Integer oneHourOpportunityQuality;
    @Column(name = "four_hour_trend_alignment")
    private Integer fourHourTrendAlignment;
    @Column(name = "normalization_version")
    private String normalizationVersion;
    @Column(name = "score_version")
    private String scoreVersion;
    @Column(name = "data_quality_version")
    private String dataQualityVersion;
    @Column(name = "provider_matrix_version")
    private String providerMatrixVersion;
    private String ruleConfidence;
    private String ruleRisk;
    private String rulePlanMode;
    private Boolean ruleCanExecute;
    private String candidatePlanMode;
    private String finalPlanMode;
    private String biasAdjustmentReason;
    private String planModeAdjustmentReason;

    @Column(name = "confused_score")
    private Integer confusedScore;

    @Column(name = "asset_state_snapshot", length = 512)
    private String assetStateSnapshot;

    @Column(name = "hot_reset_invalidated")
    private Boolean hotResetInvalidated;

    @Column(name = "hot_reset_event_id")
    private String hotResetEventId;

    @Column(name = "hot_reset_invalidated_at")
    private LocalDateTime hotResetInvalidatedAt;

    @Column(name = "hot_reset_reason_code")
    private String hotResetReasonCode;

    @Column(name = "create_time")
    private LocalDateTime createTime;

    public String getDecisionId() {
        return decisionId;
    }

    public void setDecisionId(String decisionId) {
        this.decisionId = decisionId;
    }

    public String getAnalysisId() {
        return analysisId;
    }

    public void setAnalysisId(String analysisId) {
        this.analysisId = analysisId;
    }

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public String getMarketBiasHierarchy() {
        return marketBiasHierarchy;
    }

    public void setMarketBiasHierarchy(String marketBiasHierarchy) {
        this.marketBiasHierarchy = marketBiasHierarchy;
    }

    public String getTradeType() {
        return tradeType;
    }

    public void setTradeType(String tradeType) {
        this.tradeType = tradeType;
    }

    public String getConfidenceLevel() {
        return confidenceLevel;
    }

    public void setConfidenceLevel(String confidenceLevel) {
        this.confidenceLevel = confidenceLevel;
    }

    public String getRiskLevel() {
        return riskLevel;
    }

    public void setRiskLevel(String riskLevel) {
        this.riskLevel = riskLevel;
    }

    public String getActionPriority() {
        return actionPriority;
    }

    public void setActionPriority(String actionPriority) {
        this.actionPriority = actionPriority;
    }

    public String getConclusionSummary() {
        return conclusionSummary;
    }

    public void setConclusionSummary(String conclusionSummary) {
        this.conclusionSummary = conclusionSummary;
    }

    public Boolean getIsWorthOpening() {
        return isWorthOpening;
    }

    public void setIsWorthOpening(Boolean isWorthOpening) {
        this.isWorthOpening = isWorthOpening;
    }

    public String getMultiTfConvergence() {
        return multiTfConvergence;
    }

    public void setMultiTfConvergence(String multiTfConvergence) {
        this.multiTfConvergence = multiTfConvergence;
    }

    public String getAiRoleResults() {
        return aiRoleResults;
    }

    public void setAiRoleResults(String aiRoleResults) {
        this.aiRoleResults = aiRoleResults;
    }

    public Boolean getIsAdopted() {
        return isAdopted;
    }

    public void setIsAdopted(Boolean isAdopted) {
        this.isAdopted = isAdopted;
    }

    public String getValidPeriod() {
        return validPeriod;
    }

    public void setValidPeriod(String validPeriod) {
        this.validPeriod = validPeriod;
    }

    public OffsetDateTime getValidFrom() {
        return validFrom;
    }

    public void setValidFrom(OffsetDateTime validFrom) {
        this.validFrom = validFrom;
    }

    public OffsetDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(OffsetDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }

    public String getInvalidCondition() {
        return invalidCondition;
    }

    public void setInvalidCondition(String invalidCondition) {
        this.invalidCondition = invalidCondition;
    }

    public String getEvidenceSummary() {
        return evidenceSummary;
    }

    public void setEvidenceSummary(String evidenceSummary) {
        this.evidenceSummary = evidenceSummary;
    }

    public String getExplanationJson() {
        return explanationJson;
    }

    public void setExplanationJson(String explanationJson) {
        this.explanationJson = explanationJson;
    }

    public String getReviewReasons() {
        return reviewReasons;
    }

    public void setReviewReasons(String reviewReasons) {
        this.reviewReasons = reviewReasons;
    }

    public String getAiConflictLevel() {
        return aiConflictLevel;
    }

    public void setAiConflictLevel(String aiConflictLevel) {
        this.aiConflictLevel = aiConflictLevel;
    }

    public Integer getAiConflictScore() {
        return aiConflictScore;
    }

    public void setAiConflictScore(Integer aiConflictScore) {
        this.aiConflictScore = aiConflictScore;
    }

    public String getAiPlanMode() {
        return aiPlanMode;
    }

    public void setAiPlanMode(String aiPlanMode) {
        this.aiPlanMode = aiPlanMode;
    }

    public String getRuleMarketBias() { return ruleMarketBias; }
    public void setRuleMarketBias(String value) { this.ruleMarketBias = value; }
    public String getValidatedMarketBias() { return validatedMarketBias; }
    public void setValidatedMarketBias(String value) { this.validatedMarketBias = value; }
    public String getFinalMarketBias() { return finalMarketBias; }
    public void setFinalMarketBias(String value) { this.finalMarketBias = value; }
    public String getDirectionDataState() { return directionDataState; }
    public void setDirectionDataState(String value) { this.directionDataState = value; }
    public Integer getDataQualityScore() { return dataQualityScore; }
    public void setDataQualityScore(Integer value) { this.dataQualityScore = value; }
    public Integer getEvidenceReliability() { return evidenceReliability; }
    public void setEvidenceReliability(Integer value) { this.evidenceReliability = value; }
    public Integer getOpportunityScore() { return opportunityScore; }
    public void setOpportunityScore(Integer value) { this.opportunityScore = value; }
    public Integer getRiskScore() { return riskScore; }
    public void setRiskScore(Integer value) { this.riskScore = value; }
    public Integer getFinalConfidence() { return finalConfidence; }
    public void setFinalConfidence(Integer value) { this.finalConfidence = value; }
    public Integer getOneHourOpportunityQuality() { return oneHourOpportunityQuality; }
    public void setOneHourOpportunityQuality(Integer value) { this.oneHourOpportunityQuality = value; }
    public Integer getFourHourTrendAlignment() { return fourHourTrendAlignment; }
    public void setFourHourTrendAlignment(Integer value) { this.fourHourTrendAlignment = value; }
    public String getNormalizationVersion() { return normalizationVersion; }
    public void setNormalizationVersion(String value) { this.normalizationVersion = value; }
    public String getScoreVersion() { return scoreVersion; }
    public void setScoreVersion(String value) { this.scoreVersion = value; }
    public String getDataQualityVersion() { return dataQualityVersion; }
    public void setDataQualityVersion(String value) { this.dataQualityVersion = value; }
    public String getProviderMatrixVersion() { return providerMatrixVersion; }
    public void setProviderMatrixVersion(String value) { this.providerMatrixVersion = value; }
    public String getRuleConfidence() { return ruleConfidence; }
    public void setRuleConfidence(String value) { this.ruleConfidence = value; }
    public String getRuleRisk() { return ruleRisk; }
    public void setRuleRisk(String value) { this.ruleRisk = value; }
    public String getRulePlanMode() { return rulePlanMode; }
    public void setRulePlanMode(String value) { this.rulePlanMode = value; }
    public Boolean getRuleCanExecute() { return ruleCanExecute; }
    public void setRuleCanExecute(Boolean value) { this.ruleCanExecute = value; }
    public String getCandidatePlanMode() { return candidatePlanMode; }
    public void setCandidatePlanMode(String value) { this.candidatePlanMode = value; }
    public String getFinalPlanMode() { return finalPlanMode; }
    public void setFinalPlanMode(String value) { this.finalPlanMode = value; }
    public String getBiasAdjustmentReason() { return biasAdjustmentReason; }
    public void setBiasAdjustmentReason(String value) { this.biasAdjustmentReason = value; }
    public String getPlanModeAdjustmentReason() { return planModeAdjustmentReason; }
    public void setPlanModeAdjustmentReason(String value) { this.planModeAdjustmentReason = value; }

    public Integer getConfusedScore() {
        return confusedScore;
    }

    public void setConfusedScore(Integer confusedScore) {
        this.confusedScore = confusedScore;
    }

    public String getAssetStateSnapshot() {
        return assetStateSnapshot;
    }

    public void setAssetStateSnapshot(String assetStateSnapshot) {
        this.assetStateSnapshot = assetStateSnapshot;
    }

    public Boolean getHotResetInvalidated() {
        return hotResetInvalidated;
    }

    public void setHotResetInvalidated(Boolean hotResetInvalidated) {
        this.hotResetInvalidated = hotResetInvalidated;
    }

    public String getHotResetEventId() {
        return hotResetEventId;
    }

    public void setHotResetEventId(String hotResetEventId) {
        this.hotResetEventId = hotResetEventId;
    }

    public LocalDateTime getHotResetInvalidatedAt() {
        return hotResetInvalidatedAt;
    }

    public void setHotResetInvalidatedAt(LocalDateTime hotResetInvalidatedAt) {
        this.hotResetInvalidatedAt = hotResetInvalidatedAt;
    }

    public String getHotResetReasonCode() {
        return hotResetReasonCode;
    }

    public void setHotResetReasonCode(String hotResetReasonCode) {
        this.hotResetReasonCode = hotResetReasonCode;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }
}
