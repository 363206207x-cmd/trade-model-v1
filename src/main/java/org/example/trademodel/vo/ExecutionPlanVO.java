package org.example.trademodel.vo;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class ExecutionPlanVO {
    public static final String PLAN_MODE_ADVISORY = "ADVISORY";
    public static final String PLAN_MODE_SEMI_STRUCTURED = "SEMI_STRUCTURED";
    public static final String EXECUTION_PLAN_STATUS_VALID = "VALID";
    public static final String EXECUTION_PLAN_STATUS_INCOMPLETE = "INCOMPLETE";
    public static final String EXECUTION_PLAN_STATUS_BLOCKED = "BLOCKED";
    public static final String EXECUTION_PLAN_STATUS_REVIEW_ONLY = "REVIEW_ONLY";
    public static final String EXECUTION_PLAN_STATUS_INVALID = "INVALID";
    public static final String READINESS_INCOMPLETE = "INCOMPLETE";
    public static final String READINESS_WATCH_ONLY = "WATCH_ONLY";
    public static final String READINESS_READY_REVIEW_ONLY = "READY_REVIEW_ONLY";

    private String planId;
    private String planMode;
    private String executionPlanStatus = EXECUTION_PLAN_STATUS_INCOMPLETE;
    private String readinessStatus = READINESS_INCOMPLETE;
    private String sourceGateStatus = EXECUTION_PLAN_STATUS_INCOMPLETE;
    private Boolean sourceGateComplete = false;
    private String sourceCompletenessSummary = "SOURCE_GATE_NOT_EVALUATED";
    private List<String> missingSourceReasons = new ArrayList<>();
    private List<String> sourceBlockerReasons = new ArrayList<>();
    private String sourceTraceStatus = "BACKEND_PENDING";
    private Boolean sourceTraceComplete = false;
    private String notExecutableReason = "SOURCE_TRACE_NOT_EVALUATED";
    private Boolean manualReviewRequired = true;
    private Boolean notTradeInstruction = true;
    private Boolean notExecutable = true;
    private Boolean notAutoTrading = true;
    private Boolean notOrderExecution = true;
    private Boolean notUserPositionCreation = true;
    private String riskActionGuardStatus = "BACKEND_PENDING";
    private String riskActionGuardBlockingReason;
    private Boolean riskActionGuardReady = false;
    private String recommendedAction;
    private String entryZone;
    private String stopLoss;
    private String takeProfitRules;
    private String addPositionCondition;
    private String reducePositionCondition;
    private String abandonCondition;
    private String invalidCondition;
    private String invalidationSource;
    private String invalidationReason;
    private String leverageSuggestion;
    private String positionSuggestion;
    private String accountRiskJson;
    private String executionFeasibilityStatus = "UNAVAILABLE";
    private String slippageStatus = "UNAVAILABLE";
    private String depthStatus = "UNAVAILABLE";
    private String entryDriftStatus = "UNAVAILABLE";
    private String triggerStatus = "UNAVAILABLE";
    private String executionFeasibilityReason = "EXECUTION_FEASIBILITY_SOURCE_UNAVAILABLE";
    private LocalDateTime executionFeasibilityObservedAt;
    private LocalDateTime executionFeasibilityFreshUntil;
    private String executionFeasibilitySourceRefsJson;
    private Boolean needsRevalidation;
    private String revalidationReason;
    private String derivativesStatus;
    private String derivativesFreshness;
    private List<String> derivativesReasonCodes = new ArrayList<>();
    private Instant derivativesProviderDataTime;
    private String derivativesTraceId;
    private String candidateId;
    private String opportunityId;
    private String resolverResultId;
    private String traceId;
    private String chainStatus = "LEGACY";
    private String ruleValidationStatus = "LEGACY";
    private String ruleVetoReason;
    private LocalDateTime finalizedAt;
    private Boolean finalPlan = false;
    private Long assetId;
    private String ruleVersion;
    private String ruleMarketBias;
    private String finalMarketBias;
    private String candidatePlanMode;
    private String finalPlanMode;
    private String biasAdjustmentReason;
    private String planModeAdjustmentReason;
    private String adjustmentReason;
    private String downgradeReason;
    private String opportunityType;
    private String entryLogic;
    private String entrySource;
    private String entryReason;
    private String triggerCondition;
    private String stopLogic;
    private String stopSource;
    private String stopReason;
    private String targetLogic;
    private String targetSource;
    private String targetReason;
    private String riskExplanation;
    private String leverageLimit;
    private String positionLimit;
    private BigDecimal riskLimit;
    private BigDecimal expectedRiskReward;
    private String expectedRiskRewardSource;
    private String expectedRiskRewardReason;
    private Long accountRiskSnapshotId;
    private String analysisTimeframesJson;
    private String triggerTimeframe;
    private LocalDateTime validFrom;
    private LocalDateTime validUntil;
    private String holdingHorizon;
    private String revalidationRule;
    private Integer dataQuality;
    private String sourceRefsJson;
    private String evidenceRefsJson;
    private String scoreRefsJson;
    private String validationResultId;
    private String sourceStatus;
    private String planLifecycleState = "INVALIDATED";
    private Integer planVersion = 1;
    private String supersedesPlanId;
    private String supersededByPlanId;
    private List<String> validationReasons = new ArrayList<>();

    public String getPlanId() { return planId; }
    public void setPlanId(String planId) { this.planId = planId; }
    public String getPlanMode() { return planMode; }
    public void setPlanMode(String planMode) { this.planMode = planMode; }
    public String getExecutionPlanStatus() { return executionPlanStatus; }
    public void setExecutionPlanStatus(String executionPlanStatus) { this.executionPlanStatus = executionPlanStatus; }
    public String getReadinessStatus() { return readinessStatus; }
    public void setReadinessStatus(String readinessStatus) { this.readinessStatus = readinessStatus; }
    public String getSourceGateStatus() { return sourceGateStatus; }
    public void setSourceGateStatus(String sourceGateStatus) { this.sourceGateStatus = sourceGateStatus; }
    public Boolean getSourceGateComplete() { return sourceGateComplete; }
    public void setSourceGateComplete(Boolean sourceGateComplete) { this.sourceGateComplete = sourceGateComplete; }
    public String getSourceCompletenessSummary() { return sourceCompletenessSummary; }
    public void setSourceCompletenessSummary(String sourceCompletenessSummary) { this.sourceCompletenessSummary = sourceCompletenessSummary; }
    public List<String> getMissingSourceReasons() { return missingSourceReasons; }
    public void setMissingSourceReasons(List<String> missingSourceReasons) {
        this.missingSourceReasons = missingSourceReasons == null ? new ArrayList<>() : new ArrayList<>(missingSourceReasons);
    }
    public List<String> getSourceBlockerReasons() { return sourceBlockerReasons; }
    public void setSourceBlockerReasons(List<String> sourceBlockerReasons) {
        this.sourceBlockerReasons = sourceBlockerReasons == null ? new ArrayList<>() : new ArrayList<>(sourceBlockerReasons);
    }
    public String getSourceTraceStatus() { return sourceTraceStatus; }
    public void setSourceTraceStatus(String sourceTraceStatus) { this.sourceTraceStatus = sourceTraceStatus; }
    public Boolean getSourceTraceComplete() { return sourceTraceComplete; }
    public void setSourceTraceComplete(Boolean sourceTraceComplete) { this.sourceTraceComplete = sourceTraceComplete; }
    public String getNotExecutableReason() { return notExecutableReason; }
    public void setNotExecutableReason(String notExecutableReason) { this.notExecutableReason = notExecutableReason; }
    public Boolean getManualReviewRequired() { return manualReviewRequired; }
    public void setManualReviewRequired(Boolean manualReviewRequired) { this.manualReviewRequired = manualReviewRequired; }
    public Boolean getNotTradeInstruction() { return notTradeInstruction; }
    public void setNotTradeInstruction(Boolean notTradeInstruction) { this.notTradeInstruction = notTradeInstruction; }
    public Boolean getNotExecutable() { return notExecutable; }
    public void setNotExecutable(Boolean notExecutable) { this.notExecutable = notExecutable; }
    public Boolean getNotAutoTrading() { return notAutoTrading; }
    public void setNotAutoTrading(Boolean notAutoTrading) { this.notAutoTrading = notAutoTrading; }
    public Boolean getNotOrderExecution() { return notOrderExecution; }
    public void setNotOrderExecution(Boolean notOrderExecution) { this.notOrderExecution = notOrderExecution; }
    public Boolean getNotUserPositionCreation() { return notUserPositionCreation; }
    public void setNotUserPositionCreation(Boolean notUserPositionCreation) { this.notUserPositionCreation = notUserPositionCreation; }
    public String getRiskActionGuardStatus() { return riskActionGuardStatus; }
    public void setRiskActionGuardStatus(String riskActionGuardStatus) { this.riskActionGuardStatus = riskActionGuardStatus; }
    public String getRiskActionGuardBlockingReason() { return riskActionGuardBlockingReason; }
    public void setRiskActionGuardBlockingReason(String riskActionGuardBlockingReason) { this.riskActionGuardBlockingReason = riskActionGuardBlockingReason; }
    public Boolean getRiskActionGuardReady() { return riskActionGuardReady; }
    public void setRiskActionGuardReady(Boolean riskActionGuardReady) { this.riskActionGuardReady = riskActionGuardReady; }
    public String getRecommendedAction() { return recommendedAction; }
    public void setRecommendedAction(String recommendedAction) { this.recommendedAction = recommendedAction; }
    public String getEntryZone() { return entryZone; }
    public void setEntryZone(String entryZone) { this.entryZone = entryZone; }
    public String getStopLoss() { return stopLoss; }
    public void setStopLoss(String stopLoss) { this.stopLoss = stopLoss; }
    public String getTakeProfitRules() { return takeProfitRules; }
    public void setTakeProfitRules(String takeProfitRules) { this.takeProfitRules = takeProfitRules; }
    public String getAddPositionCondition() { return addPositionCondition; }
    public void setAddPositionCondition(String addPositionCondition) { this.addPositionCondition = addPositionCondition; }
    public String getReducePositionCondition() { return reducePositionCondition; }
    public void setReducePositionCondition(String reducePositionCondition) { this.reducePositionCondition = reducePositionCondition; }
    public String getAbandonCondition() { return abandonCondition; }
    public void setAbandonCondition(String abandonCondition) { this.abandonCondition = abandonCondition; }
    public String getInvalidCondition() { return invalidCondition; }
    public void setInvalidCondition(String invalidCondition) { this.invalidCondition = invalidCondition; }
    public String getInvalidationSource() { return invalidationSource; }
    public void setInvalidationSource(String value) { this.invalidationSource = value; }
    public String getInvalidationReason() { return invalidationReason; }
    public void setInvalidationReason(String value) { this.invalidationReason = value; }
    public String getLeverageSuggestion() { return leverageSuggestion; }
    public void setLeverageSuggestion(String leverageSuggestion) { this.leverageSuggestion = leverageSuggestion; }
    public String getPositionSuggestion() { return positionSuggestion; }
    public void setPositionSuggestion(String positionSuggestion) { this.positionSuggestion = positionSuggestion; }
    public String getAccountRiskJson() { return accountRiskJson; }
    public void setAccountRiskJson(String value) { this.accountRiskJson = value; }
    public String getExecutionFeasibilityStatus() { return executionFeasibilityStatus; }
    public void setExecutionFeasibilityStatus(String value) { this.executionFeasibilityStatus = value; }
    public String getSlippageStatus() { return slippageStatus; }
    public void setSlippageStatus(String value) { this.slippageStatus = value; }
    public String getDepthStatus() { return depthStatus; }
    public void setDepthStatus(String value) { this.depthStatus = value; }
    public String getEntryDriftStatus() { return entryDriftStatus; }
    public void setEntryDriftStatus(String value) { this.entryDriftStatus = value; }
    public String getTriggerStatus() { return triggerStatus; }
    public void setTriggerStatus(String value) { this.triggerStatus = value; }
    public String getExecutionFeasibilityReason() { return executionFeasibilityReason; }
    public void setExecutionFeasibilityReason(String value) { this.executionFeasibilityReason = value; }
    public LocalDateTime getExecutionFeasibilityObservedAt() { return executionFeasibilityObservedAt; }
    public void setExecutionFeasibilityObservedAt(LocalDateTime value) { this.executionFeasibilityObservedAt = value; }
    public LocalDateTime getExecutionFeasibilityFreshUntil() { return executionFeasibilityFreshUntil; }
    public void setExecutionFeasibilityFreshUntil(LocalDateTime value) { this.executionFeasibilityFreshUntil = value; }
    public String getExecutionFeasibilitySourceRefsJson() { return executionFeasibilitySourceRefsJson; }
    public void setExecutionFeasibilitySourceRefsJson(String value) { this.executionFeasibilitySourceRefsJson = value; }
    public Boolean getNeedsRevalidation() { return needsRevalidation; }
    public void setNeedsRevalidation(Boolean needsRevalidation) { this.needsRevalidation = needsRevalidation; }
    public String getRevalidationReason() { return revalidationReason; }
    public void setRevalidationReason(String revalidationReason) { this.revalidationReason = revalidationReason; }
    public String getDerivativesStatus() { return derivativesStatus; }
    public void setDerivativesStatus(String derivativesStatus) { this.derivativesStatus = derivativesStatus; }
    public String getDerivativesFreshness() { return derivativesFreshness; }
    public void setDerivativesFreshness(String derivativesFreshness) { this.derivativesFreshness = derivativesFreshness; }
    public List<String> getDerivativesReasonCodes() { return List.copyOf(derivativesReasonCodes); }
    public void setDerivativesReasonCodes(List<String> derivativesReasonCodes) {
        this.derivativesReasonCodes = derivativesReasonCodes == null ? new ArrayList<>() : new ArrayList<>(derivativesReasonCodes);
    }
    public Instant getDerivativesProviderDataTime() { return derivativesProviderDataTime; }
    public void setDerivativesProviderDataTime(Instant derivativesProviderDataTime) { this.derivativesProviderDataTime = derivativesProviderDataTime; }
    public String getDerivativesTraceId() { return derivativesTraceId; }
    public void setDerivativesTraceId(String derivativesTraceId) { this.derivativesTraceId = derivativesTraceId; }
    public String getCandidateId() { return candidateId; }
    public void setCandidateId(String candidateId) { this.candidateId = candidateId; }
    public String getOpportunityId() { return opportunityId; }
    public void setOpportunityId(String opportunityId) { this.opportunityId = opportunityId; }
    public String getResolverResultId() { return resolverResultId; }
    public void setResolverResultId(String resolverResultId) { this.resolverResultId = resolverResultId; }
    public String getTraceId() { return traceId; }
    public void setTraceId(String traceId) { this.traceId = traceId; }
    public String getChainStatus() { return chainStatus; }
    public void setChainStatus(String chainStatus) { this.chainStatus = chainStatus; }
    public String getRuleValidationStatus() { return ruleValidationStatus; }
    public void setRuleValidationStatus(String ruleValidationStatus) { this.ruleValidationStatus = ruleValidationStatus; }
    public String getRuleVetoReason() { return ruleVetoReason; }
    public void setRuleVetoReason(String ruleVetoReason) { this.ruleVetoReason = ruleVetoReason; }
    public LocalDateTime getFinalizedAt() { return finalizedAt; }
    public void setFinalizedAt(LocalDateTime finalizedAt) { this.finalizedAt = finalizedAt; }
    public Boolean getFinalPlan() { return finalPlan; }
    public void setFinalPlan(Boolean finalPlan) { this.finalPlan = finalPlan; }
    public Long getAssetId() { return assetId; }
    public void setAssetId(Long value) { this.assetId = value; }
    public String getRuleVersion() { return ruleVersion; }
    public void setRuleVersion(String value) { this.ruleVersion = value; }
    public String getRuleMarketBias() { return ruleMarketBias; }
    public void setRuleMarketBias(String value) { this.ruleMarketBias = value; }
    public String getFinalMarketBias() { return finalMarketBias; }
    public void setFinalMarketBias(String value) { this.finalMarketBias = value; }
    public String getCandidatePlanMode() { return candidatePlanMode; }
    public void setCandidatePlanMode(String value) { this.candidatePlanMode = value; }
    public String getFinalPlanMode() { return finalPlanMode; }
    public void setFinalPlanMode(String value) { this.finalPlanMode = value; }
    public String getBiasAdjustmentReason() { return biasAdjustmentReason; }
    public void setBiasAdjustmentReason(String value) { this.biasAdjustmentReason = value; }
    public String getPlanModeAdjustmentReason() { return planModeAdjustmentReason; }
    public void setPlanModeAdjustmentReason(String value) { this.planModeAdjustmentReason = value; }
    public String getAdjustmentReason() { return adjustmentReason; }
    public void setAdjustmentReason(String value) { this.adjustmentReason = value; }
    public String getDowngradeReason() { return downgradeReason; }
    public void setDowngradeReason(String value) { this.downgradeReason = value; }
    public String getOpportunityType() { return opportunityType; }
    public void setOpportunityType(String value) { this.opportunityType = value; }
    public String getEntryLogic() { return entryLogic; }
    public void setEntryLogic(String value) { this.entryLogic = value; }
    public String getEntrySource() { return entrySource; }
    public void setEntrySource(String value) { this.entrySource = value; }
    public String getEntryReason() { return entryReason; }
    public void setEntryReason(String value) { this.entryReason = value; }
    public String getTriggerCondition() { return triggerCondition; }
    public void setTriggerCondition(String value) { this.triggerCondition = value; }
    public String getStopLogic() { return stopLogic; }
    public void setStopLogic(String value) { this.stopLogic = value; }
    public String getStopSource() { return stopSource; }
    public void setStopSource(String value) { this.stopSource = value; }
    public String getStopReason() { return stopReason; }
    public void setStopReason(String value) { this.stopReason = value; }
    public String getTargetLogic() { return targetLogic; }
    public void setTargetLogic(String value) { this.targetLogic = value; }
    public String getTargetSource() { return targetSource; }
    public void setTargetSource(String value) { this.targetSource = value; }
    public String getTargetReason() { return targetReason; }
    public void setTargetReason(String value) { this.targetReason = value; }
    public String getRiskExplanation() { return riskExplanation; }
    public void setRiskExplanation(String value) { this.riskExplanation = value; }
    public String getLeverageLimit() { return leverageLimit; }
    public void setLeverageLimit(String value) { this.leverageLimit = value; }
    public String getPositionLimit() { return positionLimit; }
    public void setPositionLimit(String value) { this.positionLimit = value; }
    public BigDecimal getRiskLimit() { return riskLimit; }
    public void setRiskLimit(BigDecimal value) { this.riskLimit = value; }
    public BigDecimal getExpectedRiskReward() { return expectedRiskReward; }
    public void setExpectedRiskReward(BigDecimal value) { this.expectedRiskReward = value; }
    public String getExpectedRiskRewardSource() { return expectedRiskRewardSource; }
    public void setExpectedRiskRewardSource(String value) { this.expectedRiskRewardSource = value; }
    public String getExpectedRiskRewardReason() { return expectedRiskRewardReason; }
    public void setExpectedRiskRewardReason(String value) { this.expectedRiskRewardReason = value; }
    public Long getAccountRiskSnapshotId() { return accountRiskSnapshotId; }
    public void setAccountRiskSnapshotId(Long value) { this.accountRiskSnapshotId = value; }
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
    public Integer getDataQuality() { return dataQuality; }
    public void setDataQuality(Integer value) { this.dataQuality = value; }
    public String getSourceRefsJson() { return sourceRefsJson; }
    public void setSourceRefsJson(String value) { this.sourceRefsJson = value; }
    public String getEvidenceRefsJson() { return evidenceRefsJson; }
    public void setEvidenceRefsJson(String value) { this.evidenceRefsJson = value; }
    public String getScoreRefsJson() { return scoreRefsJson; }
    public void setScoreRefsJson(String value) { this.scoreRefsJson = value; }
    public String getValidationResultId() { return validationResultId; }
    public void setValidationResultId(String value) { this.validationResultId = value; }
    public String getSourceStatus() { return sourceStatus; }
    public void setSourceStatus(String value) { this.sourceStatus = value; }
    public String getPlanLifecycleState() { return planLifecycleState; }
    public void setPlanLifecycleState(String value) { this.planLifecycleState = value; }
    public Integer getPlanVersion() { return planVersion; }
    public void setPlanVersion(Integer value) { this.planVersion = value; }
    public String getSupersedesPlanId() { return supersedesPlanId; }
    public void setSupersedesPlanId(String value) { this.supersedesPlanId = value; }
    public String getSupersededByPlanId() { return supersededByPlanId; }
    public void setSupersededByPlanId(String value) { this.supersededByPlanId = value; }
    public String getStopZone() { return stopLoss; }
    public String getTargetZones() { return takeProfitRules; }
    public BigDecimal getExpectedRR() { return expectedRiskReward; }
    public String getAnalysisTimeframes() { return analysisTimeframesJson; }
    public List<String> getValidationReasons() { return List.copyOf(validationReasons); }
    public void setValidationReasons(List<String> value) {
        this.validationReasons = value == null ? new ArrayList<>() : new ArrayList<>(value);
    }
}
