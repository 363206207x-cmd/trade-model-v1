package org.example.trademodel.entity;
import java.time.LocalDateTime;

public class ExecutionPlanDO {
    private String planId;
    private String analysisId;
    private String planMode;
    private String executionPlanStatus;
    private String sourceGateStatus;
    private Boolean sourceGateComplete;
    private String sourceMissingReasons;
    private String sourceBlockerReasons;
    private String sourceCompletenessSummary;
    private String recommendedAction;
    private String entryZone;
    private String stopLoss;
    private String takeProfitRules;
    private String leverageSuggestion;
    private String positionSuggestion;
    private String accountRiskJson;
    private String invalidCondition;
    private Boolean manualReviewRequired;
    private Boolean notTradeInstruction;
    private Boolean notExecutable;
    private Boolean notAutoTrading;
    private Boolean notOrderExecution;
    private Boolean notUserPositionCreation;
    private LocalDateTime createTime;

    public String getPlanId() { return planId; }
    public void setPlanId(String planId) { this.planId = planId; }
    public String getAnalysisId() { return analysisId; }
    public void setAnalysisId(String analysisId) { this.analysisId = analysisId; }
    public String getPlanMode() { return planMode; }
    public void setPlanMode(String planMode) { this.planMode = planMode; }
    public String getExecutionPlanStatus() { return executionPlanStatus; }
    public void setExecutionPlanStatus(String executionPlanStatus) { this.executionPlanStatus = executionPlanStatus; }
    public String getSourceGateStatus() { return sourceGateStatus; }
    public void setSourceGateStatus(String sourceGateStatus) { this.sourceGateStatus = sourceGateStatus; }
    public Boolean getSourceGateComplete() { return sourceGateComplete; }
    public void setSourceGateComplete(Boolean sourceGateComplete) { this.sourceGateComplete = sourceGateComplete; }
    public String getSourceMissingReasons() { return sourceMissingReasons; }
    public void setSourceMissingReasons(String sourceMissingReasons) { this.sourceMissingReasons = sourceMissingReasons; }
    public String getSourceBlockerReasons() { return sourceBlockerReasons; }
    public void setSourceBlockerReasons(String sourceBlockerReasons) { this.sourceBlockerReasons = sourceBlockerReasons; }
    public String getSourceCompletenessSummary() { return sourceCompletenessSummary; }
    public void setSourceCompletenessSummary(String sourceCompletenessSummary) { this.sourceCompletenessSummary = sourceCompletenessSummary; }
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
    public String getAccountRiskJson() { return accountRiskJson; }
    public void setAccountRiskJson(String accountRiskJson) { this.accountRiskJson = accountRiskJson; }
    public String getInvalidCondition() { return invalidCondition; }
    public void setInvalidCondition(String invalidCondition) { this.invalidCondition = invalidCondition; }
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
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
}
