package org.example.trademodel.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;

public class DecisionResultVO {

    private String decisionId;
    private String analysisId;
    private String symbol;
    private String timeframe;
    private LocalDateTime analysisTime;
    private String marketBiasHierarchy;
    private String tradeType;
    private String confidenceLevel;
    private String riskLevel;
    private String actionPriority;
    private String conclusionSummary;
    private Boolean isWorthOpening;
    private String multiTfConvergence;
    private String aiRoleResults;
    private Boolean isAdopted;
    private String validPeriod;
    private OffsetDateTime validFrom;
    private OffsetDateTime expiresAt;
    private String invalidCondition;
    private String evidenceSummary;
    private String explanationJson;
    private String reviewReasons;
    private String aiConflictLevel;
    private Integer aiConflictScore;
    private String aiPlanMode;
    private Integer confusedScore;
    private String assetStateSnapshot;
    private String executionPlanSummary;
    private String recommendedAction;
    private String planMode;
    private String validatedMarketBias;
    private String finalMarketBias;
    private String entryZone;
    private String stopLoss;
    private String takeProfitRules;
    private String leverageSuggestion;
    private String positionSuggestion;
    private Integer dataQualityScore;
    private Double opportunityScore;
    private Integer evidenceReliability;
    private Integer riskScore;
    private Integer finalConfidence;
    private Integer oneHourOpportunityQuality;
    private Integer fourHourTrendAlignment;
    private String directionDataState;
    private String normalizationVersion;
    private String scoreVersion;
    private String dataQualityVersion;
    private String providerMatrixVersion;
    private LocalDateTime createTime;
    private BigDecimal latestPrice;
    private BigDecimal priceChangePct;
    private Long priceUpdateTimeMs;
    private Boolean hasOpenPosition;
    private String positionSide;
    private BigDecimal avgOpenPrice;
    private LocalDateTime positionOpenTime;
    private BigDecimal positionQuantity;
    private BigDecimal unrealizedPnlPct;
    private String positionStatus;
    private BigDecimal markPrice;
    private BigDecimal breakEvenPrice;
    private BigDecimal liquidationPrice;

    /**
     * Read-model completeness boundary for Phase 10 Step 1.
     * FULL means required persisted fields are present; PARTIAL means one or more are absent.
     */
    private String readModelTruthStatus;

    /**
     * When any persisted read-model field is absent, lists LEGACY_MISSING:field,... so clients
     * can use technical placeholders instead of inferring business outcomes.
     */
    private String readModelFallbackReason;

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

    public String getTimeframe() {
        return timeframe;
    }

    public void setTimeframe(String timeframe) {
        this.timeframe = timeframe;
    }

    public LocalDateTime getAnalysisTime() {
        return analysisTime;
    }

    public void setAnalysisTime(LocalDateTime analysisTime) {
        this.analysisTime = analysisTime;
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

    public String getExecutionPlanSummary() {
        return executionPlanSummary;
    }

    public void setExecutionPlanSummary(String executionPlanSummary) {
        this.executionPlanSummary = executionPlanSummary;
    }

    public String getRecommendedAction() {
        return recommendedAction;
    }

    public void setRecommendedAction(String recommendedAction) {
        this.recommendedAction = recommendedAction;
    }

    public String getPlanMode() {
        return planMode;
    }

    public void setPlanMode(String planMode) {
        this.planMode = planMode;
    }

    public String getFinalMarketBias() {
        return finalMarketBias;
    }

    public void setFinalMarketBias(String finalMarketBias) {
        this.finalMarketBias = finalMarketBias;
    }

    public String getValidatedMarketBias() { return validatedMarketBias; }
    public void setValidatedMarketBias(String value) { this.validatedMarketBias = value; }
    public Integer getEvidenceReliability() { return evidenceReliability; }
    public void setEvidenceReliability(Integer value) { this.evidenceReliability = value; }
    public Integer getRiskScore() { return riskScore; }
    public void setRiskScore(Integer value) { this.riskScore = value; }
    public Integer getFinalConfidence() { return finalConfidence; }
    public void setFinalConfidence(Integer value) { this.finalConfidence = value; }
    public Integer getOneHourOpportunityQuality() { return oneHourOpportunityQuality; }
    public void setOneHourOpportunityQuality(Integer value) { this.oneHourOpportunityQuality = value; }
    public Integer getFourHourTrendAlignment() { return fourHourTrendAlignment; }
    public void setFourHourTrendAlignment(Integer value) { this.fourHourTrendAlignment = value; }
    public String getDirectionDataState() { return directionDataState; }
    public void setDirectionDataState(String value) { this.directionDataState = value; }
    public String getNormalizationVersion() { return normalizationVersion; }
    public void setNormalizationVersion(String value) { this.normalizationVersion = value; }
    public String getScoreVersion() { return scoreVersion; }
    public void setScoreVersion(String value) { this.scoreVersion = value; }
    public String getDataQualityVersion() { return dataQualityVersion; }
    public void setDataQualityVersion(String value) { this.dataQualityVersion = value; }
    public String getProviderMatrixVersion() { return providerMatrixVersion; }
    public void setProviderMatrixVersion(String value) { this.providerMatrixVersion = value; }

    public String getEntryZone() {
        return entryZone;
    }

    public void setEntryZone(String entryZone) {
        this.entryZone = entryZone;
    }

    public String getStopLoss() {
        return stopLoss;
    }

    public void setStopLoss(String stopLoss) {
        this.stopLoss = stopLoss;
    }

    public String getTakeProfitRules() {
        return takeProfitRules;
    }

    public void setTakeProfitRules(String takeProfitRules) {
        this.takeProfitRules = takeProfitRules;
    }

    public String getLeverageSuggestion() {
        return leverageSuggestion;
    }

    public void setLeverageSuggestion(String leverageSuggestion) {
        this.leverageSuggestion = leverageSuggestion;
    }

    public String getPositionSuggestion() {
        return positionSuggestion;
    }

    public void setPositionSuggestion(String positionSuggestion) {
        this.positionSuggestion = positionSuggestion;
    }

    public Integer getDataQualityScore() {
        return dataQualityScore;
    }

    public void setDataQualityScore(Integer dataQualityScore) {
        this.dataQualityScore = dataQualityScore;
    }

    public Double getOpportunityScore() {
        return opportunityScore;
    }

    public void setOpportunityScore(Double opportunityScore) {
        this.opportunityScore = opportunityScore;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    public BigDecimal getLatestPrice() {
        return latestPrice;
    }

    public void setLatestPrice(BigDecimal latestPrice) {
        this.latestPrice = latestPrice;
    }

    public BigDecimal getPriceChangePct() {
        return priceChangePct;
    }

    public void setPriceChangePct(BigDecimal priceChangePct) {
        this.priceChangePct = priceChangePct;
    }

    public Long getPriceUpdateTimeMs() {
        return priceUpdateTimeMs;
    }

    public void setPriceUpdateTimeMs(Long priceUpdateTimeMs) {
        this.priceUpdateTimeMs = priceUpdateTimeMs;
    }

    public Boolean getHasOpenPosition() {
        return hasOpenPosition;
    }

    public void setHasOpenPosition(Boolean hasOpenPosition) {
        this.hasOpenPosition = hasOpenPosition;
    }

    public String getPositionSide() {
        return positionSide;
    }

    public void setPositionSide(String positionSide) {
        this.positionSide = positionSide;
    }

    public BigDecimal getAvgOpenPrice() {
        return avgOpenPrice;
    }

    public void setAvgOpenPrice(BigDecimal avgOpenPrice) {
        this.avgOpenPrice = avgOpenPrice;
    }

    public LocalDateTime getPositionOpenTime() {
        return positionOpenTime;
    }

    public void setPositionOpenTime(LocalDateTime positionOpenTime) {
        this.positionOpenTime = positionOpenTime;
    }

    public BigDecimal getPositionQuantity() {
        return positionQuantity;
    }

    public void setPositionQuantity(BigDecimal positionQuantity) {
        this.positionQuantity = positionQuantity;
    }

    public BigDecimal getUnrealizedPnlPct() {
        return unrealizedPnlPct;
    }

    public void setUnrealizedPnlPct(BigDecimal unrealizedPnlPct) {
        this.unrealizedPnlPct = unrealizedPnlPct;
    }

    public String getPositionStatus() {
        return positionStatus;
    }

    public void setPositionStatus(String positionStatus) {
        this.positionStatus = positionStatus;
    }

    public BigDecimal getMarkPrice() {
        return markPrice;
    }

    public void setMarkPrice(BigDecimal markPrice) {
        this.markPrice = markPrice;
    }

    public BigDecimal getBreakEvenPrice() {
        return breakEvenPrice;
    }

    public void setBreakEvenPrice(BigDecimal breakEvenPrice) {
        this.breakEvenPrice = breakEvenPrice;
    }

    public BigDecimal getLiquidationPrice() {
        return liquidationPrice;
    }

    public void setLiquidationPrice(BigDecimal liquidationPrice) {
        this.liquidationPrice = liquidationPrice;
    }

    public String getReadModelTruthStatus() {
        return readModelTruthStatus;
    }

    public void setReadModelTruthStatus(String readModelTruthStatus) {
        this.readModelTruthStatus = readModelTruthStatus;
    }

    public String getReadModelFallbackReason() {
        return readModelFallbackReason;
    }

    public void setReadModelFallbackReason(String readModelFallbackReason) {
        this.readModelFallbackReason = readModelFallbackReason;
    }
}
