package org.example.trademodel.vo;

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
    private String leverageSuggestion;
    private String positionSuggestion;
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
    public String getLeverageSuggestion() { return leverageSuggestion; }
    public void setLeverageSuggestion(String leverageSuggestion) { this.leverageSuggestion = leverageSuggestion; }
    public String getPositionSuggestion() { return positionSuggestion; }
    public void setPositionSuggestion(String positionSuggestion) { this.positionSuggestion = positionSuggestion; }
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
}
